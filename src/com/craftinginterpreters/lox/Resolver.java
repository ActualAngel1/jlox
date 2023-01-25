package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Var>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private WhileType currentWhile = WhileType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD,
    }
    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private ClassType currentClass = ClassType.NONE;
    private enum WhileType {
        NONE,
        WHILE
    }
    private static class Var {
        final Token name;
        VarState state;

        private Var(Token name, VarState state) {
            this.name = name;
            this.state = state;
        }
    }

    private enum VarState {
        DECLARED,
        DEFINED,
        USED
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
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null &&
                stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            Lox.error(stmt.superclass.name,
                    "A class can't inherit from itself.");
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        if (stmt.superclass != null) {
            beginScope();
            scopes.peek().put("super", new Var(stmt.superclass.name, VarState.USED));
        }

        beginScope();
        scopes.peek().put("this", new Var(stmt.name, VarState.USED));

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }

        endScope();

        if (stmt.superclass != null) endScope();

        currentClass = enclosingClass;
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
        resolveFunction(stmt, FunctionType.FUNCTION);
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
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword,
                        "Can't return a value from an initializer.");
            }

            resolve(stmt.value);
        }

        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        WhileType tempWhileState = currentWhile;
        currentWhile = WhileType.WHILE;
        resolve(stmt.condition);
        resolve(stmt.body);
        currentWhile = tempWhileState;
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
    public Void visitBreakStmt(Stmt.Break stmt){
        if (currentWhile == WhileType.NONE) {
            Lox.error(stmt.name, "Can't return from top-level code.");
        }
        return null;
    }
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
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
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
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
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }
    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                    "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword,
                    "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }
    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                    "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }
    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitTernaryExpr(Expr.Ternary expr){
        resolve(expr.condition);
        resolve(expr.ifFalsePart);
        resolve(expr.ifTruePart);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        int scopeDistance = scopes.size()-1;
        if(scopes.isEmpty()){
            return null;
        }
        if(scopes.peek().get(expr.name.lexeme)==null && checkIfHasValue(expr)==-1){
            resolveLocal(expr, expr.name);
            return null;
        }
        else if(checkIfHasValue(expr)!=-1){
            scopeDistance = checkIfHasValue(expr);
        }

        VarState variableState = scopes.get(scopeDistance).get(expr.name.lexeme).state;
        if (!scopes.isEmpty() &&
                variableState == VarState.DECLARED) {
            Lox.error(expr.name,
                    "Can't read local variable in its own initializer.");

        } else if (!scopes.isEmpty() &&
                (variableState == VarState.DEFINED || variableState == VarState.USED)) {
            setUsedInAllLowerScopes(expr);
            scopes.peek().put(expr.name.lexeme, new Var(expr.name, VarState.USED));
        }
        // this line is the problem
        resolveLocal(expr, expr.name);
        return null;
    }
    private int checkIfHasValue(Expr.Variable expr) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).get(expr.name.lexeme)!=null) {
                return i;
            }
        }
        return -1;
    }

    private void setUsedInAllLowerScopes(Expr.Variable expr){
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if(scopes.get(i).containsKey(expr.name.lexeme)){
                scopes.get(i).put(expr.name.lexeme, new Var(expr.name, VarState.USED));
            }
        }
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }
    private void resolveFunction(
          Stmt.Function function, FunctionType type) {
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

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    private void beginScope() {
        scopes.push(new HashMap<String, Var>());
    }
    private void endScope() {
        for (String i : scopes.peek().keySet()) {
                Var var = scopes.peek().get(i);
                if(var.state != VarState.USED){
                    Lox.error(var.name,
                            "The variable was defined or declared but not used.");
                }
        }

        scopes.pop();
    }
    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Var> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                    "Already a variable with this name in this scope.");
        }
        scope.put(name.lexeme, new Var(name, VarState.DECLARED));
    }
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, new Var(name, VarState.DEFINED));
    }
    private void resolveLocal(Expr expr, Token name) {
        for (int i = 0; i <= scopes.size(); i++) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }


}