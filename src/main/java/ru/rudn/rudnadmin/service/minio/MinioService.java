package ru.rudn.rudnadmin.service.minio;

import ru.rudn.rudnadmin.service.minio.exception.MinioLoadException;

import java.io.IOException;

public interface MinioService {

    String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    String OVPN_CONTENT_TYPE = "application/x-openvpn-profile";

    void load(String fileName, String filePath) throws IOException, MinioLoadException;

    void load(String fileName, String contentType, String filePath) throws IOException, MinioLoadException;

    String getLink(String fileName);

    void delete(String fileName);
}
