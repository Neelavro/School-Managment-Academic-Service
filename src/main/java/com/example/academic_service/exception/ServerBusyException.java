package com.example.academic_service.exception;

/** The server is shedding load; mapped to HTTP 429 + Retry-After so clients retry. */
public class ServerBusyException extends RuntimeException {
    public ServerBusyException(String message) {
        super(message);
    }
}
