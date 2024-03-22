package org.lang.lox;

public class RuntimeError extends RuntimeException {

    public final Token token;

    public RuntimeError(Token token, String s) {
        super(s);
        this.token = token;
    }
}
