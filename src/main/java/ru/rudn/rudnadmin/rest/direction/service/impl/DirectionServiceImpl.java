package ru.rudn.rudnadmin.rest.direction.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.repository.DirectionRepository;
import ru.rudn.rudnadmin.rest.direction.service.DirectionService;

import java.util.List;
import java.util.Optional;

import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;

@Service
@RequiredArgsConstructor
public class DirectionServiceImpl implements DirectionService {

    private final DirectionRepository directionRepository;

    @Override
    public void saveAll(List<Direction> directions) {
        directionRepository.saveAll(directions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Direction> findAll() {
        return directionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Direction> findById(Long id) {
        return directionRepository.findById(id);
    }


    @Override
    public void save(Direction direction) {
        directionRepository.save(direction);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        final Direction direction = findById(id).orElseThrow(getEntityException(Direction.class));

        directionRepository.delete(direction);
    }
}


