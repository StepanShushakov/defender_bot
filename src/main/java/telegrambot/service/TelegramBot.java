package telegrambot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ApproveChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.groupadministration.DeclineChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import telegrambot.entity.Question;
import telegrambot.entity.User;
import telegrambot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final BotService botService;

    private final UserRepository userRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.group.name}")
    private String groupName;

    @Value("${telegram.bot.group.link}")
    private String groupLink;

    @Value("${telegram.bot.group.chatId}")
    private Long groupChatId;

    public TelegramBot(BotService botService, UserRepository userRepository) {
        this.botService = botService;
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername; // замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.info("Received update: " + update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        } else if (update.hasChatJoinRequest()) {
            handleChatJoinRequest(update.getChatJoinRequest());
        }
    }

    private void handleMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        if (Objects.equals(chatId, groupChatId)) {
            return;
        }
        String messageText = update.getMessage().getText();
        org.telegram.telegrambots.meta.api.objects.User tgUser = update.getMessage().getFrom();

        User user = userRepository.findById(chatId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(chatId);
            newUser.setUsername(tgUser.getUserName());
            return newUser;
        });
            Question currentQuestion = user.getCurrentQuestion();
            if (currentQuestion == null) {
                sendMessage(chatId, "Для того, что бы я смог проверить ваш ответ, нужно подать заявку на вступление в группу " + groupLink);
            }
            else if (botService.checkAnswer(currentQuestion, messageText)) {
                sendMessage(chatId, "Правильно! Добро пожаловать в группу.");
                approveJoinRequest(chatId);
                saveDialogAndClearCurrentQuestion(user, messageText);
            } else {
                sendMessage(chatId, "Неверный ответ. Ваш запрос на вступление отклонен. Отправьте заявку на вступление снова.");
                declineJoinRequest(chatId);
                saveDialogAndClearCurrentQuestion(user, messageText);
            }
    }

    private void saveDialogAndClearCurrentQuestion(User user, String messageText) {
        botService.saveDialog(user, messageText);
        user.setCurrentQuestion(null);
        userRepository.save(user);
    }

    private void handleChatJoinRequest(ChatJoinRequest chatJoinRequest) {
        Long userId = chatJoinRequest.getUser().getId();
        String username = chatJoinRequest.getUser().getUserName();

        User user = userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setUsername(username);
            return newUser;
        });

        Question question = botService.getRandomQuestion();
        sendMessage(userId, "Здравствуйте, перед присоединением к группе <b><u>"
                + groupName + "</u></b>, пожалуйста ответьте на вопрос: " + question.getText());
        user.setCurrentQuestion(question);
        userRepository.save(user);
    }

    private void approveJoinRequest(Long userId) {
        try {
            ApproveChatJoinRequest approveRequest = new ApproveChatJoinRequest();
            approveRequest.setChatId(getChatId());
            approveRequest.setUserId(userId);
            execute(approveRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void declineJoinRequest(Long userId) {
        try {
            DeclineChatJoinRequest declineRequest = new DeclineChatJoinRequest();
            declineRequest.setChatId(getChatId());
            declineRequest.setUserId(userId);
            execute(declineRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getChatId() {
        return groupChatId.toString();
    }
}
