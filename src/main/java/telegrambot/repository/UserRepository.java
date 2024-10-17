package telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telegrambot.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Additional query methods if needed
}
