package ru.rudn.rudnadmin.service.minio.exception;

public class MinioException extends RuntimeException {

    public MinioException(String message) {
        super(message);
    }
}
