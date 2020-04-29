package net.chthonic.lox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    private final List<LoxClass> mro;

    public static final LoxClass Class = new LoxClass("Class");

    private LoxClass(String name) {
        super(Class);
        this.name = name;
        this.methods = Collections.emptyMap();
        this.mro = Collections.singletonList(this);
    }

    LoxClass(String name,
             List<LoxClass> superclasses,
             Map<String, LoxFunction> instanceMethods,
             Map<String, LoxFunction> classMethods) throws InheritanceError {
        super(Class);
        this.name = name;
        if (superclasses.isEmpty()) {
            this.mro = Collections.singletonList(this);
        } else {
            this.mro = findMethodResolutionOrder(superclasses);
        }

        this.methods = instanceMethods;
        for (Map.Entry<String, LoxFunction> entry : classMethods.entrySet()) {
            LoxFunction boundToClass = entry.getValue().bind(this);
            set(entry.getKey(), boundToClass);
        }
    }

    private List<LoxClass> findMethodResolutionOrder(List<LoxClass> superclasses) throws InheritanceError {
        ArrayList<ArrayList<LoxClass>> toMerge = new ArrayList<>();
        for (LoxClass superclass : superclasses) {

            toMerge.add(new ArrayList<>(superclass.mro));
        }
        toMerge.add(new ArrayList<>(superclasses));

        ArrayList<LoxClass> order = new ArrayList<>();
        order.add(this);
        // C3 linearization algorithm
        while (!toMerge.stream().allMatch(ArrayList::isEmpty)) {
            LoxClass goodCandidate = null;
            outer:
            for (ArrayList<LoxClass> sublist : toMerge) {
                if (sublist.isEmpty()) continue;
                LoxClass candidate = sublist.get(0);
                // is the candidate present in any of the other lists, as any element after 0?
                for (ArrayList<LoxClass> other : toMerge) {
                    if (other.indexOf(candidate) > 0) {
                        continue outer; // reject the candidate
                    }
                }
                goodCandidate = candidate;
                break;
            }
            if (goodCandidate == null) {
                throw new InheritanceError();
            }
            order.add(goodCandidate);
            for (ArrayList<LoxClass> sublist : toMerge) {
                sublist.remove(goodCandidate);
            }
        }
        return order;
    }

    LoxFunction findMethod(String name) {
        for (LoxClass ancestor : mro) {
            if (ancestor.methods.containsKey(name)) {
                return ancestor.methods.get(name);
            }
        }

        return null;
    }

    LoxFunction findSuperMethod(String name) {
        for (int i=1; i<mro.size(); i++) {
            LoxClass ancestor = mro.get(i);
            if (ancestor.methods.containsKey(name)) {
                return ancestor.methods.get(name);
            }
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
