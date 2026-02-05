package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);
		R visitClassStmt(Class stmt);
		R visitExpressionStmt(Expression stmt);
		R visitFlowStmt(Flow stmt);
		R visitFunctionStmt(Function stmt);
		R visitIfStmt(If stmt);
		R visitPrintStmt(Print stmt);
		R visitReturnStmt(Return stmt);
		R visitTraitStmt(Trait stmt);
		R visitVarStmt(Var stmt);
		R visitWhileStmt(While stmt);
	}
	static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		final List<Stmt> statements;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}
	}
	static class Class extends Stmt {
		Class(Token name, Expr.Variable superclass, List<Expr.Variable> traits, Map<Stmt.Function,Boolean> methods, List<Stmt.Function> staticMethods) {
			this.name = name;
			this.superclass = superclass;
			this.traits = traits;
			this.methods = methods;
			this.staticMethods = staticMethods;
		}

		final Token name;
		final Expr.Variable superclass;
		final List<Expr.Variable> traits;
		final Map<Stmt.Function,Boolean> methods;
		final List<Stmt.Function> staticMethods;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}
	}
	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		final Expr expression;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}
	}
	static class Flow extends Stmt {
		Flow(Token type) {
			this.type = type;
		}

		final Token type;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFlowStmt(this);
		}
	}
	static class Function extends Stmt {
		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		final Token name;
		final List<Token> params;
		final List<Stmt> body;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}
	}
	static class If extends Stmt {
		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}
	}
	static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
		}

		final Expr expression;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}
	static class Return extends Stmt {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		final Token keyword;
		final Expr value;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}
	}
	static class Trait extends Stmt {
		Trait(Token name, Map<Token,Integer> methods, List<Stmt.Function> defaultImpls) {
			this.name = name;
			this.methods = methods;
			this.defaultImpls = defaultImpls;
		}

		final Token name;
		final Map<Token,Integer> methods;
		final List<Stmt.Function> defaultImpls;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitTraitStmt(this);
		}
	}
	static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		final Token name;
		final Expr initializer;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}
	}
	static class While extends Stmt {
		While(Expr condition, Stmt body, Stmt increment) {
			this.condition = condition;
			this.body = body;
			this.increment = increment;
		}

		final Expr condition;
		final Stmt body;
		final Stmt increment;

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}
	}

	abstract <R> R accept(Visitor<R> visitor);
}
