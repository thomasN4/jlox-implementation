package com.craftinginterpreters.lox;

class FlowException extends RuntimeError {
    TokenType type;

    FlowException(Token token) {
        super(token, "Flow statement outside of loop.");
        this.type = token.type;
    }
}
