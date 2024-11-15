## О проекте

Программный код бота, предназначенного для фильтрации пользователей, вступающих в группу.
При добавлении пользователя в группу, бот задает ему случайны вопрос и, если пользователь отвечает
правильно, бот принимает заявку на вступление. В противном случае заявка отклоняется.

### Как запустить
Для того, что бы бот заработал в группе, нужно
1. через `@BotFather` добавить нового бота или использовать уже имеющегося
2. получить токен бота и довить имя и токен в файл настроек (описано ниже)
3. добавить бота в группу, в которой он будет работать и дать ему права администратора
4. в файле настроек изменить настройки имени, ссылки и chatId групп, которую он будет защищать,
 для того, что бы бот отправлял пользователям корректные сообщения
5. запустить проект

### Spring проект

В данном проекте используется Spring, maven используется в качестве системы сборки.
Изначально в проекте для хранения списка вопросов была использована база данных
postgreSql, но для экономии места и облегчения бота, СУБД была изменена на H2.

В `application.properties` находятся конфигурируемые параметры приложения
в частности, там указываются:
- токен бота `telegram.bot.token`
- имя бота `telegram.bot.username`
- имя группы `telegram.bot.groupName`
- ссылка на группу `telegram.bot.group.link`
- chatId группы `telegram.bot.group.chatId`

### База данных
Для удобства работы с базой данных используется in memory база данных H2,
которая заполняется данными вопросов и ответов при запуске проекта.
Бот задает случайный вопрос из списка, новому участнику, который добавляется в группу,
находящуюся под защитой бота.
Список вопросов и ответов хранится в файле `data.sql`,
который при необходимости можно дополнить новыми вопросами и ответами.

H2 консоль, становится доступной после старта проекта по адресу `http://localhost:8182/h2`
При необходимости можно изменить порт сервера `server.port`
и эндпоинт консоли `spring.h2.console.path` в файле свойств `application.properties`

### Пример использования
На момент размещения проекта бот защищает группу `https://t.me/vremena_goda_kemerovo`
с более чем тремястами пользователями.