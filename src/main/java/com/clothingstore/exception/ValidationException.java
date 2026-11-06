package com.clothingstore.exception;

public final class ValidationException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    /** Tên ô nhập của form mà thông báo nói đến, hoặc null khi thông báo không gắn với ô nhập nào. */
    private final String field;

    // Ép (String) null để chọn đúng constructor (message, field), tránh nhầm với (message, cause).
    public ValidationException(String message) { this(message, (String) null); }
    public ValidationException(String message, String field) {
        super(message);
        this.field = field;
    }
    public ValidationException(String message, String field, Throwable cause) {
        super(message, cause);
        this.field = field;
    }

    public String getField() { return field; }
}
