package net.chthonic.lox;

import java.util.*;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VariableUsage>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private Stack<Stmt.While> loops = new Stack<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        LAMBDA
    }

    private enum VariableState {
        DECLARED,
        DEFINED,
        ASSIGNED,
        USED
    }

    private class VariableUsage {
        public Token token;  // for nice error messages
        public VariableState state;
        public Set<Stmt.While> usedInLoops = new HashSet<>();

        public VariableUsage(Token token, VariableState state) {
            this.token = token;
            this.state = state;
        }
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        for (VariableUsage usage : scopes.peek().values()) {
            switch (usage.state){
                case DECLARED:
                    // should be impossible
                    Lox.error(usage.token, "Variable declared but not defined");
                case DEFINED:
                    if (!usage.token.lexeme.equals("_"))
                        Lox.error(usage.token, "Variable defined but not used");
                case ASSIGNED:
                    if (!usage.token.lexeme.equals("_"))
                        Lox.error(usage.token, "Variable assignment not used");
            }
        }
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, VariableUsage> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            if (!name.lexeme.equals("_"))
                Lox.error(name,
                    "Variable with this name already declared in this scope.");
        }

        scope.put(name.lexeme, new VariableUsage(name, VariableState.DECLARED));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
    }

    private VariableUsage resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return scopes.get(i).get(name.lexeme);
            }
        }

        // Not found. Assume it is global.
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void resolveLambda(Expr.Lambda lambda) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = FunctionType.LAMBDA;

        beginScope();
        for (Token param : lambda.params) {
            declare(param);
            define(param);
        }
        resolve(lambda.body);
        endScope();
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);

        VariableUsage usage = resolveLocal(expr, expr.name);
        if (usage != null) {
            usage.token = expr.name;
            if (loops.isEmpty() || usage.state != VariableState.USED) {
                usage.state = VariableState.ASSIGNED;
            } else {
                // if this variable is reassigned in a loop and used in the same loop, it still counts as used
                Set<Stmt.While> intersection = new HashSet<>(loops);
                intersection.retainAll(usage.usedInLoops);
                // it it's NOT used in the same loop, treat it as assigned, but not used
                if (intersection.isEmpty()) {
                    usage.state = VariableState.ASSIGNED;
                }
            }
        }
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.left);
        resolve(expr.middle);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        VariableUsage usage = resolveLocal(expr, expr.name);
        if (usage != null) {
            switch (usage.state) {
                case DECLARED:
                    Lox.error(expr.name,
                            "Cannot read local variable in its own initializer.");
                case DEFINED:
                case ASSIGNED:
                    if (usage.token.lexeme.equals("_"))
                        Lox.error(expr.name, "_ variable used");
                    usage.state = VariableState.USED;
                    usage.usedInLoops.addAll(loops);
            }
        }
        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        // Don't allow break to jump out of a function
        Stack<Stmt.While> enclosingLoops = loops;
        loops = new Stack<>();
        resolveLambda(expr);
        loops = enclosingLoops;
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        // Don't allow break to jump out of a function
        Stack<Stmt.While> enclosingLoops = loops;
        loops = new Stack<>();
        resolveFunction(stmt, FunctionType.FUNCTION);
        loops = enclosingLoops;
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintAstStmt(Stmt.PrintAst stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        loops.push(stmt);
        resolve(stmt.condition);
        resolve(stmt.body);
        loops.pop();
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (loops.isEmpty()) {
            Lox.error(stmt.keyword, "No enclosing loop");
        }
        if (stmt.label == null) {
            return null;
        }
        for (Stmt.While loop : loops) {
            if (stmt.label.lexeme.equals(loop.label.lexeme)) {
                return null;
            }
        }
        Lox.error(stmt.label, "No enclosing loop labeled " + stmt.label.lexeme);
        return null;
    }
}
