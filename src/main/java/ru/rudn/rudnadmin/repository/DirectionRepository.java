package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.Direction;


public interface DirectionRepository extends JpaRepository<Direction, Long> {

}
