package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.Group;


public interface GroupRepository extends JpaRepository<Group, Long> {


}
