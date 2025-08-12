package ru.rudn.rudnadmin.rest.global.utils;

import ru.rudn.rudnadmin.rest.global.exception.EntityNotFoundException;
import ru.rudn.rudnadmin.rest.global.exception.EntityParameterFoundException;

import java.util.function.Supplier;

public class ExceptionHelperUtils {

    public static final String ERROR_DESCRIPTION_PREFIX = "Cannot find such ";

    public static <T> Supplier<EntityNotFoundException> getEntityException(final Class<T> aclass) {
        return () -> new EntityNotFoundException(ERROR_DESCRIPTION_PREFIX + aclass.getSimpleName());
    }

    public static <T> Supplier<EntityParameterFoundException> getEntityParamException(final Class<T> aclass) {
        return () -> new EntityParameterFoundException(ERROR_DESCRIPTION_PREFIX + aclass.getSimpleName());
    }

    public static <T> Supplier<EntityParameterFoundException> getEntityParamException(final String message) {
        return () -> new EntityParameterFoundException(message);
    }
}
