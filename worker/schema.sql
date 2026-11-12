-- Схема базы D1 для бота на Cloudflare Workers.
-- Вопросы и ответы заливаются отдельно из src/main/resources/data.sql (он совместим с SQLite).

CREATE TABLE IF NOT EXISTS question (
	id INTEGER PRIMARY KEY,
	text TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS answer (
	id INTEGER PRIMARY KEY,
	text TEXT NOT NULL,
	question_id INTEGER NOT NULL REFERENCES question (id)
);

CREATE INDEX IF NOT EXISTS answer_question_id ON answer (question_id);

CREATE TABLE IF NOT EXISTS app_user (
	id INTEGER PRIMARY KEY, -- Telegram user id
	username TEXT,
	current_question_id INTEGER REFERENCES question (id)
);

CREATE TABLE IF NOT EXISTS dialog (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id INTEGER NOT NULL,
	message TEXT,
	timestamp TEXT NOT NULL
);
