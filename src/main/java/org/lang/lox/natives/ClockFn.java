package org.lang.lox.natives;

import org.lang.lox.Interpreter;
import org.lang.lox.LoxCallable;

import java.util.List;

public class ClockFn implements LoxCallable {

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
    }

    @Override
    public String toString() {
        return "<native-fn : clock>";
    }
}
