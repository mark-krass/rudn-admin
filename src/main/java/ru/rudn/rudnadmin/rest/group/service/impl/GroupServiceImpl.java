package ru.rudn.rudnadmin.rest.group.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.repository.GroupRepository;
import ru.rudn.rudnadmin.rest.group.service.GroupService;

import java.util.List;
import java.util.Optional;

import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;

    @Override
    public void saveAll(List<Group> groups) {
        groupRepository.saveAll(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    @Override
    public Group save(Group group) {
        return groupRepository.save(group);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        final Group group = findById(id).orElseThrow(getEntityException(Group.class));

        groupRepository.delete(group);
    }
}


