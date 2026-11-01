package com.clothingstore.exception;

/** Lớp gốc cho các lỗi dự kiến của ứng dụng; thông báo là văn bản tiếng Việt mà khách hoặc admin đọc được. */
public abstract class ApplicationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected ApplicationException(String message) { super(message); }
    protected ApplicationException(String message, Throwable cause) { super(message, cause); }
}
