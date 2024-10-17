package telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telegrambot.entity.Question;

/*INSERT INTO question (id, text) VALUES
        (1, 'Напишите название города, в котором находится наш комплекс'),
        (2, 'Напишите фамилию человека, в честь которого названа улица, на которой расположен комплекс "Времена года". (Фамилия совпадает с названием улицы)');*/

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Additional query methods if needed
}
