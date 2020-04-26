package net.chthonic.lox;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;

    public static final LoxClass Class = new LoxClass("Class", Collections.emptyMap(), Collections.emptyMap());

    LoxClass(String name, Map<String, LoxFunction> instanceMethods, Map<String, LoxFunction> classMethods) {
        super(Class);
        this.name = name;
        this.methods = instanceMethods;
        for (Map.Entry<String, LoxFunction> entry : classMethods.entrySet()) {
            LoxFunction boundToClass = entry.getValue().bind(this);
            set(entry.getKey(), boundToClass);
        }
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }
}
