package telegrambot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import telegrambot.entity.Answer;
import telegrambot.entity.Dialog;
import telegrambot.entity.Question;
import telegrambot.entity.User;
import telegrambot.repository.AnswerRepository;
import telegrambot.repository.DialogRepository;
import telegrambot.repository.QuestionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class BotService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final DialogRepository dialogRepository;

    @Autowired
    public BotService(QuestionRepository questionRepository,
                      AnswerRepository answerRepository,
                      DialogRepository dialogRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.dialogRepository = dialogRepository;
    }

    public Question getRandomQuestion() {
        List<Question> questions = questionRepository.findAll();
        return questions.get(new Random().nextInt(questions.size()));
    }

    public boolean checkAnswer(Question question, String answerText) {
        List<Answer> answers = answerRepository.findByQuestion(question);
        for (Answer answer : answers) {
            if (answer.getText().equalsIgnoreCase(answerText)) {
                return true;
            }
        }
        return false;
    }

    public void saveDialog(User user, String message) {
        Dialog dialog = new Dialog();
        dialog.setUser(user);
        dialog.setMessage(message);
        dialog.setTimestamp(LocalDateTime.now());
        dialogRepository.save(dialog);
    }
}
