package com.br.psyke.psyke.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) { super(message); }
}
