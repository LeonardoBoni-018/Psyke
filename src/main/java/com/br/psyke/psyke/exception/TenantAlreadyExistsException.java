package com.br.psyke.psyke.exception;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String message) { super(message); }
}
