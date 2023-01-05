package com.craftinginterpreters.lox;

class BreakException extends RuntimeException {
    final Token token;

    BreakException(Token token, String message) {
        super(message);
        this.token = token;
    }
}