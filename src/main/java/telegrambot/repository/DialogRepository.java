package telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telegrambot.entity.Dialog;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, Long> {
    // Additional query methods if needed
}
