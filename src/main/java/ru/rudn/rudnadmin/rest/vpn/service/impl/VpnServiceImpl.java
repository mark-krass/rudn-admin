package ru.rudn.rudnadmin.rest.vpn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnRepository;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.rest.global.exception.EntityNotFoundException;
import ru.rudn.rudnadmin.rest.student.service.StudentService;
import ru.rudn.rudnadmin.rest.vpn.service.VpnService;

import java.util.*;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.*;

@Service
@RequiredArgsConstructor
public class VpnServiceImpl implements VpnService {

    private final VpnRepository vpnRepository;
    private final VpnTaskRepository taskRepository;

    private final StudentService service;

    @Override
    @Transactional
    public void createForGroup(Long groupId) {
        final List<Student> students = service.findStudentsByGroup_Id(groupId);
        if (students.isEmpty()) throw new EntityNotFoundException("No students by this group id found!");

        createAndSaveVpnWithTask(students);
    }

    @Override
    @Transactional
    public void createForStudent(Long studentId) {
        final Optional<Student> student = service.findStudentById(studentId);
        if (student.isEmpty()) throw new EntityNotFoundException("No student with such id found!");

        createAndSaveVpnWithTask(Collections.singletonList(student.get()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vpn> findAllVpnStudentsByGroupId(Long groupId) {
        return vpnRepository.findAllByUser_Student_Group_Id(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vpn> findVpnByStudentId(Long studentId) {
        return vpnRepository.findByUser_Student_Id(studentId);
    }

    @Override
    @Transactional
    public void updateLinkByGroupId(Long groupId) {
        final List<Vpn> studentsByGroup = findAllVpnStudentsByGroupId(groupId);
        if (studentsByGroup.isEmpty()) throw new EntityNotFoundException("No vpn by this student group id found!");

        createVpnTask(studentsByGroup, RENEW_LINK);
    }

    @Override
    @Transactional
    public void updateLinkByStudentId(Long studentId) {
        final Optional<Vpn> byStudentId = findVpnByStudentId(studentId);
        if (byStudentId.isEmpty()) throw new EntityNotFoundException("No vpn by this student id found!");

        createVpnTask(Collections.singletonList(byStudentId.get()), RENEW_LINK);
    }

    @Override
    @Transactional
    public void deleteByGroupId(Long groupId) {
        final List<Vpn> studentsByGroup = findAllVpnStudentsByGroupId(groupId);
        if (studentsByGroup.isEmpty()) throw new EntityNotFoundException("No vpn by this student group id found!");

        createVpnTask(studentsByGroup, DELETE_MINIO);
    }

    @Override
    @Transactional
    public void deleteByStudentId(Long studentId) {
        final Optional<Vpn> byStudentId = findVpnByStudentId(studentId);
        if (byStudentId.isEmpty()) throw new EntityNotFoundException("No vpn by this student id found!");

        createVpnTask(Collections.singletonList(byStudentId.get()), DELETE_MINIO);
    }

    /**
     * Метод создает {@link Vpn} для каждого студента. <p>
     * Далее, для каждого {@link Vpn} создается {@link VpnTask}. <p>
     * См. {@link ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler}
     * @param students студенты, для которых требуется создать VPN.
     */
    private void createAndSaveVpnWithTask(final List<Student> students) {
        final List<Vpn> vpns = new ArrayList<>();
        final List<VpnTask> vpnTasks = new ArrayList<>();
        students.forEach(s -> {
            final Vpn vpn = Vpn.builder().user(s.getUser()).build();
            vpns.add(vpn);
            final VpnTask vpnTask = VpnTask.builder().vpn(vpn).type(CREATE_OVPN).build();
            vpnTasks.add(vpnTask);
        });

        vpnRepository.saveAll(vpns);
        taskRepository.saveAll(vpnTasks);
    }

    /**
     * Для каждого {@link Vpn} создается активный {@link VpnTask}  <p>
     * См. {@link ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler}
     * @param vpns список впн для которых создаются таски
     * @param type тип таски
     */
    private void createVpnTask(final List<Vpn> vpns, final TaskType type) {
        final List<VpnTask> vpnTasks = new ArrayList<>();
        vpns.forEach(s -> {
            final VpnTask vpnTask = VpnTask.builder().vpn(s).type(type).build();
            vpnTasks.add(vpnTask);
        });

        taskRepository.saveAll(vpnTasks);
    }
}


