package com.craftinginterpreters.lox;

class RpnPrinter implements Expr.Visitor<String> {
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return "";
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return expr.left.accept(this) + ' '
                + expr.right.accept(this) + ' '
                + expr.operator.lexeme;
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return "";
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return "";
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return expr.accept(this);
    }

    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {
        return "";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return "";
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return "";
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return "";
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return "";
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "";
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return "";
    }

    public static void main(String[] args) {
        Expr expr = new Expr.Binary(
                new Expr.Binary(
                        new Expr.Literal(1),
                        new Token(TokenType.MINUS, "+", null, 1),
                        new Expr.Literal(2)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                        new Expr.Literal(4),
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(3)
                )
        );
        System.out.println(expr.accept(new RpnPrinter()));
    }
}
