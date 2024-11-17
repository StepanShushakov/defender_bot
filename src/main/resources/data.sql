-- Вставляем данные в таблицу question, если они отсутствуют
INSERT INTO question (id, text)
SELECT 1, 'Напишите название города, в котором находится наш комплекс'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE id = 1);

INSERT INTO question (id, text)
SELECT 2, 'Напишите фамилию человека, в честь которого названа улица, на которой расположен комплекс "Времена года". (Фамилия совпадает с названием улицы)'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE id = 2);

-- Вставляем данные в таблицу answer, если они отсутствуют
INSERT INTO answer (id, text, question_id)
SELECT 1, 'Kemerovo', 1
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 1);

INSERT INTO answer (id, text, question_id)
SELECT 2, 'Кемерово', 1
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 2);

INSERT INTO answer (id, text, question_id)
SELECT 3, 'Sarigina', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 3);

INSERT INTO answer (id, text, question_id)
SELECT 4, 'Sarigin', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 4);

INSERT INTO answer (id, text, question_id)
SELECT 5, 'Сарыгина', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 5);

INSERT INTO answer (id, text, question_id)
SELECT 6, 'Сарыгин', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 6);

INSERT INTO answer (id, text, question_id)
SELECT 7, 'Sarygina', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 7);

INSERT INTO answer (id, text, question_id)
SELECT 8, 'Sarygin', 2
WHERE NOT EXISTS (SELECT 1 FROM answer WHERE id = 8);