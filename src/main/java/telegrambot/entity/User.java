package telegrambot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "app_user")
public class User {
    @Id
    private Long id; // Telegram user ID
    private String username;

    @ManyToOne
    @JoinColumn(name = "current_question_id")
    private Question currentQuestion;
}
