package ru.rudn.rudnadmin.config;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.repository.DirectionRepository;
import ru.rudn.rudnadmin.repository.GroupRepository;
import ru.rudn.rudnadmin.repository.StudentRepository;
import ru.rudn.rudnadmin.repository.UserRepository;
import ru.rudn.rudnadmin.repository.VpnRepository;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@SpringBootTest
public abstract class TestContainersBase extends TestContainers {

    @Autowired
    protected StudentRepository studentRepository;
    @Autowired
    protected GroupRepository groupRepository;
    @Autowired
    protected DirectionRepository directionRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected VpnRepository vpnRepository;
    @Autowired
    protected VpnTaskRepository vpnTaskRepository;
    @Autowired
    protected MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String vpnBucket;


    @AfterEach
    @SneakyThrows
    void cleanup() {
        vpnRepository.deleteAll();
        studentRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
        directionRepository.deleteAll();
        cleanBucket(vpnBucket);
    }

    @SneakyThrows
    protected void cleanBucket(final String bucket) {
        // удаляем все объекты из бакета, чтобы избежать ошибки BucketNotEmpty
        final Iterable<Result<Item>> listObjects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
        for (Result<Item> result : listObjects) {
            final Item item = result.get();
            final Optional<String> optional = ofNullable(item).map(Item::objectName);
            if (optional.isPresent()) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(optional.get())
                                .build()
                );
            }
        }
    }
}
