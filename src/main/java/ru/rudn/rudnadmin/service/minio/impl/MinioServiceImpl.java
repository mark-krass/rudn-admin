package ru.rudn.rudnadmin.service.minio.impl;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.minio.exception.MinioDeleteException;
import ru.rudn.rudnadmin.service.minio.exception.MinioGetLinkException;
import ru.rudn.rudnadmin.service.minio.exception.MinioLoadException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String vpnBucket;

    @Override
    public void load(final String fileName, final String filePath) throws IOException, MinioLoadException {
        load(fileName, DEFAULT_CONTENT_TYPE, filePath);
    }

    @Override
    public void load(final String fileName, final String contentType, final String filePath) throws IOException, MinioLoadException {
        final Path path = Paths.get(filePath);
        if (Files.notExists(path)) throw new MalformedURLException(String.format("Cannot find file with name %s for %s", fileName, filePath));

        final UrlResource urlResource = new UrlResource(path.toUri());
        final long fileSize = Files.size(path);
        try (final InputStream inputStream = urlResource.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(vpnBucket)
                            .object(fileName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("Smthg went wrong while loading vpn {} with file path {} to minio", fileName, filePath, e);
            throw new MinioLoadException(String.format("Smthg went wrong while loading vpn %s with file path %s to minio: %s", fileName, filePath, e.getMessage()));
        }
    }

    @Override
    public String getLink(String fileName) {
        try {
            return  minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(vpnBucket)
                            .object(fileName)
    //                        .expiry() default 7 days
                            .build()
            );
        } catch (Exception e) {
            log.error("Smthg went wrong while get link for vpn {}", fileName, e);
            throw new MinioGetLinkException(String.format("Smthg went wrong while get link for vpn %s: %s", fileName, e.getMessage()));
        }
    }


    @Override
    public void delete(final String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(vpnBucket).object(fileName).build()
            );
        } catch (Exception e) {
            log.error("Smthg went wrong while delete vpn {} from minio", fileName, e);
            throw new MinioDeleteException(String.format("Smthg went wrong while delete vpn %s from minio: %s", fileName, e.getMessage()));
        }
    }
}
