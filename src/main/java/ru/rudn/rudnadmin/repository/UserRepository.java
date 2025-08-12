package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.User;

import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {

    // Все пользователи, у которых нет связанной записи Student
    List<User> findAllByStudentIsNull();
}
