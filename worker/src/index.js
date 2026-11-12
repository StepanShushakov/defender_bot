// Бот-привратник группы Telegram для Cloudflare Workers.
// Логика перенесена из Java-версии (src/main/java/telegrambot/service/TelegramBot.java):
// заявка на вступление -> случайный вопрос в личку -> верный ответ одобряет заявку, неверный отклоняет.
// Telegram присылает обновления на /webhook (режим webhook вместо опроса getUpdates).

export default {
	async fetch(request, env) {
		const url = new URL(request.url);

		if (request.method === 'POST' && url.pathname === '/webhook') {
			if (!env.WEBHOOK_SECRET || request.headers.get('X-Telegram-Bot-Api-Secret-Token') !== env.WEBHOOK_SECRET) {
				return new Response('forbidden', { status: 403 });
			}
			let update;
			try {
				update = await request.json();
			} catch {
				return new Response('bad request', { status: 400 });
			}
			try {
				await handleUpdate(update, env);
			} catch (e) {
				// Отвечаем 200 и при ошибке: иначе Telegram будет повторять то же обновление.
				console.error('update failed', update.update_id, e && e.stack ? e.stack : e);
			}
			return new Response('ok');
		}

		// Служебные адреса: подключить webhook и посмотреть его состояние.
		// Доступ только с заголовком X-Admin-Secret, равным WEBHOOK_SECRET.
		if (request.method === 'POST' && (url.pathname === '/setup' || url.pathname === '/status')) {
			if (!env.WEBHOOK_SECRET || request.headers.get('X-Admin-Secret') !== env.WEBHOOK_SECRET) {
				return new Response('forbidden', { status: 403 });
			}
			const result = {};
			if (url.pathname === '/setup') {
				result.setWebhook = await tg(env, 'setWebhook', {
					url: `${url.origin}/webhook`,
					secret_token: env.WEBHOOK_SECRET,
					allowed_updates: ['message', 'chat_join_request'],
				});
			}
			result.webhookInfo = await tg(env, 'getWebhookInfo', {});
			result.me = await tg(env, 'getMe', {});
			if (result.me.ok) {
				// Чтобы одобрять заявки, бот должен быть администратором группы с правом приглашать (can_invite_users).
				result.botInGroup = await tg(env, 'getChatMember', {
					chat_id: Number(env.GROUP_CHAT_ID),
					user_id: result.me.result.id,
				});
			}
			return Response.json(result);
		}

		return new Response('not found', { status: 404 });
	},
};

async function handleUpdate(update, env) {
	const groupChatId = Number(env.GROUP_CHAT_ID);

	if (update.message && typeof update.message.text === 'string') {
		// Отвечаем только в личной переписке с ботом: сообщения в группах не трогаем.
		if (update.message.chat.type !== 'private') {
			return;
		}
		await handlePrivateMessage(update.message, groupChatId, env);
	} else if (update.chat_join_request) {
		// Заявки обрабатываем только в защищаемую группу.
		if (update.chat_join_request.chat.id !== groupChatId) {
			return;
		}
		await handleJoinRequest(update.chat_join_request, env);
	}
}

async function handleJoinRequest(joinRequest, env) {
	const userId = joinRequest.from.id;
	const question = await env.DB.prepare('SELECT id, text FROM question ORDER BY RANDOM() LIMIT 1').first();
	if (!question) {
		console.error('в базе нет вопросов');
		return;
	}

	await env.DB.prepare(
		`INSERT INTO app_user (id, username, current_question_id) VALUES (?1, ?2, ?3)
		 ON CONFLICT(id) DO UPDATE SET username = excluded.username, current_question_id = excluded.current_question_id`
	)
		.bind(userId, joinRequest.from.username ?? null, question.id)
		.run();

	// user_chat_id -- личный чат с подавшим заявку; бот может писать туда, пока заявка не обработана.
	await sendMessage(
		env,
		joinRequest.user_chat_id ?? userId,
		`Здравствуйте, перед присоединением к группе <b><u>${escapeHtml(env.GROUP_NAME)}</u></b>, ` +
			`пожалуйста ответьте на вопрос: ${escapeHtml(question.text)}`
	);
}

async function handlePrivateMessage(message, groupChatId, env) {
	const userId = message.chat.id; // в личном чате совпадает с id пользователя
	const text = message.text;

	const user = await env.DB.prepare('SELECT current_question_id FROM app_user WHERE id = ?1').bind(userId).first();
	if (!user || user.current_question_id === null) {
		await sendMessage(
			env,
			userId,
			`Для того, что бы я смог проверить ваш ответ, нужно подать заявку на вступление в группу ${escapeHtml(env.GROUP_LINK)}`
		);
		return;
	}

	const { results: answers } = await env.DB.prepare('SELECT text FROM answer WHERE question_id = ?1')
		.bind(user.current_question_id)
		.all();
	const given = normalize(text);
	const isCorrect = answers.some((answer) => normalize(answer.text) === given);

	// Сначала сохраняем диалог и сбрасываем вопрос: повтор того же обновления уже не решит заявку второй раз.
	await env.DB.batch([
		env.DB.prepare('INSERT INTO dialog (user_id, message, timestamp) VALUES (?1, ?2, ?3)').bind(
			userId,
			text,
			new Date().toISOString()
		),
		env.DB.prepare('UPDATE app_user SET current_question_id = NULL, username = ?2 WHERE id = ?1').bind(
			userId,
			message.from?.username ?? null
		),
	]);

	if (isCorrect) {
		await sendMessage(env, userId, 'Правильно! Добро пожаловать в группу.');
		await tg(env, 'approveChatJoinRequest', { chat_id: groupChatId, user_id: userId });
	} else {
		await sendMessage(env, userId, 'Неверный ответ. Ваш запрос на вступление отклонен. Отправьте заявку на вступление снова.');
		await tg(env, 'declineChatJoinRequest', { chat_id: groupChatId, user_id: userId });
	}
}

// Как equalsIgnoreCase в Java-версии, плюс без пробелов по краям.
function normalize(value) {
	return String(value).trim().toLowerCase();
}

function escapeHtml(value) {
	return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function sendMessage(env, chatId, text) {
	return tg(env, 'sendMessage', { chat_id: chatId, text, parse_mode: 'HTML' });
}

// Не бросает исключений: сбой одного вызова (например, sendMessage) не должен отменять следующий (approve/decline).
async function tg(env, method, payload) {
	let response;
	try {
		response = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify(payload),
		});
	} catch (e) {
		console.error(`Telegram ${method} failed:`, e && e.message ? e.message : e);
		return { ok: false, description: String(e) };
	}
	const result = await response.json().catch(() => ({ ok: false, description: `HTTP ${response.status}` }));
	if (!result.ok) {
		console.error(`Telegram ${method} failed:`, result.description);
	}
	return result;
}
