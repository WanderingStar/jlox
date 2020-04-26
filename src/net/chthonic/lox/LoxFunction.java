package net.chthonic.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    public final FunctionType type;

    public enum FunctionType {
        FUNCTION,
        INITIALIZER,
        GETTER,
        METHOD,
        CLASS_METHOD
    }

    LoxFunction(Stmt.Function declaration, Environment closure, FunctionType type) {
        this.closure = closure;
        this.declaration = declaration;
        this.type = type;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, type);
    }

    LoxFunction bind(LoxClass klass) {
        Environment environment = new Environment(closure);
        environment.define("cls", klass);
        return new LoxFunction(declaration, environment, type);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                    arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (type == FunctionType.INITIALIZER) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (type == FunctionType.INITIALIZER) return closure.getAt(0, "this");
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}