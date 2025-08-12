package ru.rudn.rudnadmin.rest.user.service;

import ru.rudn.rudnadmin.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    void saveAll(List<User> users);

    List<User> findAll();

    Optional<User> findById(Long id);

    boolean exists(Long id);

    User save(User user);

    void deleteById(Long id);

    List<User> findAllByStudentIsNull();
}


