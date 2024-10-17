package telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telegrambot.entity.Answer;
import telegrambot.entity.Question;

import java.util.List;

/*INSERT INTO ANSWER (id, text, question_id) VALUES
        (1, 'Kemerovo', 1),
        (2, 'Кемерово', 1),
        (3, 'Sarigina', 2),
        (4, 'Sarigin',  2),
        (5, 'Сарыгина', 2),
        (6, 'Сарыгин',  2),
        (7, 'Sarygina', 2),
        (8, 'Sarygin',  2);*/

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestion(Question question);
}
