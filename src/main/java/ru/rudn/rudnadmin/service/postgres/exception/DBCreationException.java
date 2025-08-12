package ru.rudn.rudnadmin.service.postgres.exception;

public class DBCreationException extends RuntimeException {

    public DBCreationException(String message) {
        super(message);
    }
}
