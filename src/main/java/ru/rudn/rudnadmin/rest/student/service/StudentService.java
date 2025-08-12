package ru.rudn.rudnadmin.rest.student.service;

import ru.rudn.rudnadmin.entity.Student;

import java.util.List;
import java.util.Optional;

public interface StudentService {

    void saveAllStudents(List<Student> students);

    List<Student> findAllStudents();

    Optional<Student> findStudentById(Long id);

    Student saveStudent(Student student);

    void deleteStudentById(Long id);

    void deleteStudentsByGroupId(Long groupId);

    List<Student> findStudentsByGroup_Name(String name);

    List<Student> findStudentsByGroup_Id(Long groupId);
}


