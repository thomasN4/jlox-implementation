package com.craftinginterpreters.lox;

import java.util.List;
import com.craftinginterpreters.utils.*;

abstract class Expr {
	interface Visitor<R> {
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitCallExpr(Call expr);
		R visitGetExpr(Get expr);
		R visitGroupingExpr(Grouping expr);
		R visitLambdaExpr(Lambda expr);
		R visitLiteralExpr(Literal expr);
		R visitLogicalExpr(Logical expr);
		R visitSetExpr(Set expr);
		R visitSuperExpr(Super expr);
		R visitTernaryExpr(Ternary expr);
		R visitThisExpr(This expr);
		R visitUnaryExpr(Unary expr);
		R visitVariableExpr(Variable expr);
	}
	static class Assign extends Expr {
		Assign(Token name, Expr value) {
			this.name = name;
			this.value = value;
		}

		final Token name;
		final Expr value;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}
	}
	static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		final Expr left;
		final Token operator;
		final Expr right;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}
	}
	static class Call extends Expr {
		Call(Expr callee, Token paren, List<Expr> arguments) {
			this.callee = callee;
			this.paren = paren;
			this.arguments = arguments;
		}

		final Expr callee;
		final Token paren;
		final List<Expr> arguments;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}
	}
	static class Get extends Expr {
		Get(Expr object, Token name) {
			this.object = object;
			this.name = name;
		}

		final Expr object;
		final Token name;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGetExpr(this);
		}
	}
	static class Grouping extends Expr {
		Grouping(Expr expression) {
			this.expression = expression;
		}

		final Expr expression;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}
	}
	static class Lambda extends Expr {
		Lambda(List<Token> params, List<Stmt> body) {
			this.params = params;
			this.body = body;
		}

		final List<Token> params;
		final List<Stmt> body;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLambdaExpr(this);
		}
	}
	static class Literal extends Expr {
		Literal(Object value) {
			this.value = value;
		}

		final Object value;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}
	}
	static class Logical extends Expr {
		Logical(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		final Expr left;
		final Token operator;
		final Expr right;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
		}
	}
	static class Set extends Expr {
		Set(Expr object, Token name, Expr value) {
			this.object = object;
			this.name = name;
			this.value = value;
		}

		final Expr object;
		final Token name;
		final Expr value;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetExpr(this);
		}
	}
	static class Super extends Expr {
		Super(Token keyword, Token method) {
			this.keyword = keyword;
			this.method = method;
		}

		final Token keyword;
		final Token method;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSuperExpr(this);
		}
	}
	static class Ternary extends Expr {
		Ternary(Expr condition, Expr trueBranch, Expr falseBranch) {
			this.condition = condition;
			this.trueBranch = trueBranch;
			this.falseBranch = falseBranch;
		}

		final Expr condition;
		final Expr trueBranch;
		final Expr falseBranch;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitTernaryExpr(this);
		}
	}
	static class This extends Expr {
		This(Token keyword) {
			this.keyword = keyword;
		}

		final Token keyword;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitThisExpr(this);
		}
	}
	static class Unary extends Expr {
		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		final Token operator;
		final Expr right;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}
	}
	static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
		}

		final Token name;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}
	}

	abstract <R> R accept(Visitor<R> visitor);
}
