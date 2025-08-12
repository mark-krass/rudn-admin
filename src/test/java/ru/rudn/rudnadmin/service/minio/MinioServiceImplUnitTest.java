package ru.rudn.rudnadmin.service.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.rudn.rudnadmin.service.minio.exception.MinioDeleteException;
import ru.rudn.rudnadmin.service.minio.exception.MinioGetLinkException;
import ru.rudn.rudnadmin.service.minio.exception.MinioLoadException;
import ru.rudn.rudnadmin.service.minio.impl.MinioServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class MinioServiceImplUnitTest {

    private MinioService newService(MinioClient client) {
        final MinioServiceImpl service = new MinioServiceImpl(client);
        ReflectionTestUtils.setField(service, "vpnBucket", "test-bucket");
        return service;
    }

    @Test
    @DisplayName("MinIO: load — отсутствующий файл даёт MalformedURLException")
    void load_missingFile_throwsMalformedUrl() {
        final MinioClient client = Mockito.mock(MinioClient.class);
        final MinioService service = newService(client);

        assertThrows(MalformedURLException.class, () ->
                service.load("f.ovpn", "D:/definitely/not/exist/some-file.ovpn"));
    }

    @Test
    @DisplayName("MinIO: load — ошибка клиента заворачивается в MinioLoadException")
    void load_minioError_wrapsToMinioLoadException() throws IOException {
        final MinioClient client = Mockito.mock(MinioClient.class);
        final MinioService service = newService(client);

        final Path tmp = Files.createTempFile("minio-test-", ".ovpn");
        // эмуляция сбоя в minio.putObject
        try {
            doThrow(new RuntimeException("put-error")).when(client).putObject(any(PutObjectArgs.class));
        } catch (Exception ignored) {}

        assertThrows(MinioLoadException.class, () ->
                service.load("f.ovpn", tmp.toAbsolutePath().toString()));
    }

    @Test
    @DisplayName("MinIO: getLink — ошибка клиента заворачивается в MinioGetLinkException")
    void getLink_minioError_wrapsToMinioGetLinkException() {
        final MinioClient client = Mockito.mock(MinioClient.class);
        final MinioService service = newService(client);

        try {
            when(client.getPresignedObjectUrl(any())).thenThrow(new RuntimeException("presign-error"));
        } catch (Exception ignored) {}

        assertThrows(MinioGetLinkException.class, () -> service.getLink("f.ovpn"));
    }

    @Test
    @DisplayName("MinIO: delete — ошибка клиента заворачивается в MinioDeleteException")
    void delete_minioError_wrapsToMinioDeleteException() {
        final MinioClient client = Mockito.mock(MinioClient.class);
        final MinioService service = newService(client);

        try {
            doThrow(new RuntimeException("remove-error")).when(client).removeObject(any(RemoveObjectArgs.class));
        } catch (Exception ignored) {}

        assertThrows(MinioDeleteException.class, () -> service.delete("f.ovpn"));
    }
}


