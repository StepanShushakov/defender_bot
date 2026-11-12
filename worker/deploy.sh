#!/usr/bin/env bash
# Выкладка бота в Cloudflare Workers. Повторный запуск безопасен: база и данные не задваиваются.
# Перед первым запуском один раз:  npm ci  и  npx wrangler login
# Токен бота скрипт спросит сам, если его ещё нет в Cloudflare.
# Сменить токен:  UPDATE_TOKEN=1 ./deploy.sh
set -euo pipefail
cd "$(dirname "$0")"

DB_NAME=defender-bot
W=./node_modules/.bin/wrangler
[ -x "$W" ] || npm ci --no-fund --no-audit

if "$W" whoami 2>&1 | grep -qi 'not authenticated'; then
	echo 'Нет входа в Cloudflare: выполните  npx wrangler login  и запустите скрипт снова.' >&2
	exit 1
fi

# Вырезает JSON-массив или объект из вывода wrangler и выполняет над ним выражение на JavaScript.
json() {
	node -e '
		let s = "";
		process.stdin.on("data", (d) => (s += d)).on("end", () => {
			const start = s.search(/[\[{]/);
			const data = start < 0 ? null : JSON.parse(s.slice(start));
			const out = new Function("data", "arg", "return " + process.argv[1])(data, process.argv[2]);
			process.stdout.write(out === undefined || out === null ? "" : String(out));
		});' "$@"
}

db_id() {
	"$W" d1 list --json </dev/null | json '(data || []).filter((x) => x.name === arg).map((x) => x.uuid || x.database_id)[0]' "$DB_NAME"
}

echo '== 1/5 База D1'
ID=$(db_id)
if [ -z "$ID" ]; then
	"$W" d1 create "$DB_NAME" --location weur </dev/null
	ID=$(db_id)
fi
[ -n "$ID" ] || { echo "Не удалось получить id базы $DB_NAME" >&2; exit 1; }
node -e '
	const fs = require("fs");
	const f = "wrangler.toml";
	fs.writeFileSync(f, fs.readFileSync(f, "utf8").replace(/^database_id = .*$/m, `database_id = "${process.argv[1]}"`));' "$ID"
echo "База $DB_NAME: $ID"

echo '== 2/5 Таблицы и вопросы'
"$W" d1 execute "$DB_NAME" --remote --file=schema.sql -y </dev/null
"$W" d1 execute "$DB_NAME" --remote --file=../src/main/resources/data.sql -y </dev/null

echo '== 3/5 Выкладка'
OUT=$("$W" deploy </dev/null 2>&1) || { echo "$OUT"; exit 1; }
echo "$OUT"
URL=$(printf '%s\n' "$OUT" | grep -oE 'https://[a-zA-Z0-9.-]+\.workers\.dev' | head -1)
[ -n "$URL" ] || { echo 'Не нашёл адрес *.workers.dev в выводе deploy' >&2; exit 1; }

echo '== 4/5 Секреты'
SECRETS=$("$W" secret list --format json </dev/null 2>/dev/null || true)
if [ "${UPDATE_TOKEN:-}" = 1 ] || [[ "$SECRETS" != *'"BOT_TOKEN"'* ]]; then
	if [ -n "${BOT_TOKEN:-}" ]; then
		printf '%s' "$BOT_TOKEN" | "$W" secret put BOT_TOKEN
	else
		echo 'Вставьте токен бота от @BotFather и нажмите Enter (ввод не отображается):'
		"$W" secret put BOT_TOKEN
	fi
fi
mkdir -p .secrets
[ -s .secrets/webhook_secret ] || node -e 'process.stdout.write(require("crypto").randomBytes(32).toString("hex"))' > .secrets/webhook_secret
SECRET=$(cat .secrets/webhook_secret)
printf '%s' "$SECRET" | "$W" secret put WEBHOOK_SECRET

echo '== 5/5 Подключение к Telegram'
RES=''
for _ in 1 2 3 4 5 6 7 8; do
	RES=$(curl -s -m 30 -X POST "$URL/setup" -H "X-Admin-Secret: $SECRET" || true)
	case "$RES" in *'"setWebhook":{"ok":true'*) break ;; esac
	sleep 5
done
case "$RES" in
	*'"setWebhook":{"ok":true'*) ;;
	*) echo "Не удалось подключить webhook. Ответ: ${RES:-нет ответа от $URL}" >&2; exit 1 ;;
esac

printf '%s' "$RES" | json '[
	"Бот: @" + (data.me.result || {}).username,
	"Webhook: " + data.webhookInfo.result.url,
	"Ожидают обработки: " + data.webhookInfo.result.pending_update_count,
	"Бот в группе: " + (data.botInGroup && data.botInGroup.ok
		? data.botInGroup.result.status + ", право приглашать: " + (data.botInGroup.result.status === "creator" || data.botInGroup.result.can_invite_users ? "есть" : "НЕТ -- выдайте его в настройках администраторов группы")
		: "не найден (" + (data.botInGroup || {}).description + ") -- добавьте бота в группу администратором"),
].join("\n")'
echo
echo 'Готово. Журнал работы бота:  npx wrangler tail'
