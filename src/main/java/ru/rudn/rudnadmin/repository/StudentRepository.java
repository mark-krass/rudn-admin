package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.Student;

import java.util.List;


public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findStudentsByGroup_Name(String name);

    List<Student> findStudentsByGroup_Id(Long id);
}
