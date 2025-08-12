package ru.rudn.rudnadmin.rest.student.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.repository.StudentRepository;
import ru.rudn.rudnadmin.rest.student.service.StudentService;

import java.util.List;
import java.util.Optional;

import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityParamException;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public void saveAllStudents(List<Student> students) {
        studentRepository.saveAll(students);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> findStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public void deleteStudentById(Long studentId) {
        final Optional<Student> studentOptional = findStudentById(studentId);
        studentOptional.orElseThrow(getEntityException(Student.class));

        final Student student = studentOptional.get();
        final Vpn vpn = student.getUser().getVpn();
        if (vpn != null) throw getEntityParamException("Cannot delete student, while he has vpn. Student id: " + student.getId()).get();
        studentRepository.delete(student);
    }

    @Override
    @Transactional
    public void deleteStudentsByGroupId(Long groupId) {
        final List<Student> studentsByGroupId = findStudentsByGroup_Id(groupId);

        final List<Long> studentsWithVpn = studentsByGroupId.stream()
                .filter(s -> s.getUser().getVpn() != null)
                .map(Student::getId)
                .toList();
        if (CollectionUtils.isNotEmpty(studentsWithVpn)) throw getEntityParamException("Cannot delete students, while at least one has vpn. Students id with vpn: " + studentsWithVpn).get();
        studentRepository.deleteAll(studentsByGroupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findStudentsByGroup_Name(String name) {
        return studentRepository.findStudentsByGroup_Name(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findStudentsByGroup_Id(Long groupId) {
        return studentRepository.findStudentsByGroup_Id(groupId);
    }
}


