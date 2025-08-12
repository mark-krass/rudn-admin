package ru.rudn.rudnadmin.rest.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import ru.rudn.rudnadmin.rest.global.exception.EntityNotFoundException;
import ru.rudn.rudnadmin.rest.global.exception.EntityParameterFoundException;
import ru.rudn.rudnadmin.rest.global.model.ErrorResponse;

/**
 * Глобальный обработчик исключений для REST API
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Некорректные аргументы запроса → 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Ошибки валидации DTO (@Valid) → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        // Возвращаем краткое сообщение. Детализацию можно расширить при необходимости
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Validation failed"));
    }

    /**
     * Ошибки валидации метода (например, коллекции DTO) → 400
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(final HandlerMethodValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Validation failed"));
    }

    /**
     * Ошибки некорректно переданных параметров сущности для поиска/работы в БД → 400
     */
    @ExceptionHandler(EntityParameterFoundException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(final EntityParameterFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

     /**
     * Ошибки некорректно переданного ключевого параметра сущности (id) → 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(final EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Нарушение ограничений БД (например, уникальность email) → 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(final DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Data integrity violation"));
    }

    /**
     * Прочие исключения → 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(final Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ex.getMessage()));
    }
}
