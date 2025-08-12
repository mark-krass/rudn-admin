package ru.rudn.rudnadmin.service.vpn.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import java.io.IOException;
import java.nio.file.Paths;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.RENEW_LINK;
import static ru.rudn.rudnadmin.entity.vpn.TaskType.UPLOAD_MINIO;
import static ru.rudn.rudnadmin.service.minio.MinioService.OVPN_CONTENT_TYPE;

@Component
@RequiredArgsConstructor
public class VpnTaskFlowUploadMinio implements VpnTaskFlow {

    @Value("${vpn.output.dir}")
    private String outputDirPath;

    private final MinioService minioService;
    private final VpnTaskRepository taskRepository;

    @Override
    public void makeLogic(VpnTask task) throws IOException {
        final Long connectId = getConnectId(task);
        final String fileName = getVpnName(connectId);
        final String localPathToFile = Paths.get(outputDirPath).resolve(fileName).toString();
        minioService.load(fileName, OVPN_CONTENT_TYPE, localPathToFile);
    }

    @Override
    public void changeState(VpnTask task) {
        task.setType(RENEW_LINK);
        taskRepository.save(task);
    }

    @Override
    public TaskType getType() {
        return UPLOAD_MINIO;
    }
}
