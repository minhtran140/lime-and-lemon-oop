package com.clothingstore.exception;

public final class EntityNotFoundException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) { super(message); }
}
