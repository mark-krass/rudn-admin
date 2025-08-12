package ru.rudn.rudnadmin.rest.group.service;

import ru.rudn.rudnadmin.entity.Group;

import java.util.List;
import java.util.Optional;

public interface GroupService {

    void saveAll(List<Group> groups);

    List<Group> findAll();

    Optional<Group> findById(Long id);

    Group save(Group group);

    void deleteById(Long id);
}


