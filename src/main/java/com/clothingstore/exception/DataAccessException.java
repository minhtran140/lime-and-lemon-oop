package com.clothingstore.exception;

import java.util.Objects;

/**
 * Bọc lỗi lưu trữ ở tầng thấp hơn và giữ nguyên nguyên nhân gốc.
 */
public final class DataAccessException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, Objects.requireNonNull(cause, "cause"));
    }
}
