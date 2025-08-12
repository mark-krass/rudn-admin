package ru.rudn.rudnadmin.rest.direction.service;

import ru.rudn.rudnadmin.entity.Direction;

import java.util.List;
import java.util.Optional;

public interface DirectionService {

    void saveAll(List<Direction> directions);

    List<Direction> findAll();

    Optional<Direction> findById(Long id);

    void save(Direction direction);

    void deleteById(Long id);
}


