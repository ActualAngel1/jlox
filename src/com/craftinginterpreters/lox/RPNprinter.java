package com.craftinginterpreters.lox;
class RPNprinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme,
                expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return parenthesize("?", expr.condition, expr.ifTruePart, expr.ifFalsePart);
    }
    @Override
    public String visitVariableExpr(Expr.Variable expr){
        if (expr.name == null) return "nil";
        return expr.name.toString();
    }
    @Override
    public String visitAssignExpr(Expr.Assign expr){
        if (expr.name == null) return "nil";
        return expr.name + " = " + expr.value.toString();
    }
    @Override
    public String visitCallExpr(Expr.Call expr){
        if (expr.callee == null) return "nil";
        return " calls " + expr.callee + " with args " + expr.arguments.toString();
    }
    @Override
    public String visitGetExpr(Expr.Get expr){
        if (expr.name == null) return "nil";
        return expr.name.toString();
    }
    @Override
    public String visitSetExpr(Expr.Set expr){
        if (expr.name == null) return "nil";
        return expr.name.toString();
    }
    @Override
    public String visitSuperExpr(Expr.Super expr){
        return "super";
    }
    @Override
    public String visitThisExpr(Expr.This expr){
        return "this";
    }
    @Override
    public String visitLogicalExpr(Expr.Logical expr){
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr : exprs) {
            builder.append(expr.accept(this));
            builder.append(" ");
        }
        builder.append(name);

        return builder.toString();
    }
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Binary(new Expr.Literal(1),
                        new Token(TokenType.PLUS, "+", null, 1),
                        new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(new Expr.Literal(4),
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(3)));

        System.out.println(new RPNprinter().print(expression));
    }
}