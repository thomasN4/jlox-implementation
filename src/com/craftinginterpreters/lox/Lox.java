package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()), false);

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException, InterruptedException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line, true);
            hadError = false;
            Thread.sleep(50);
        }
    }

    public static void run(String source, boolean fromRepl) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser;

        if (fromRepl) {
            int sc_counter = 0;
            for (int i = 0; i < source.length(); i++) {
                if (source.charAt(i) == ';') sc_counter++;
            }
            if (sc_counter == 0) {
                Token eof = tokens.remove(tokens.size()-1);
                tokens.add(new Token(TokenType.SEMICOLON, ";", null, 1));
                tokens.add(eof);
                parser = new Parser(tokens);
                Stmt expression = parser.parse().get(0);
                if (expression instanceof Stmt.Expression && !hadError) {
                    try {
                        String value = interpreter.stringify(
                            ((Stmt.Expression) expression).expression.accept(
                                interpreter));
                        String AstRepresentation =
                            new AstPrinter(interpreter).print(
                                ((Stmt.Expression) expression).expression);
                        System.out.println(value + '\n' + AstRepresentation);
                    } catch (RuntimeError e) {
                        runtimeError(e);
                    }
                    return;
                } else {
                    eof = tokens.remove(tokens.size()-1);
                    tokens.remove(tokens.size()-1);
                    tokens.add(eof);
                }
            }
        }

        parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        hadError = true;
        report(line, "", message, 0);
    }

	static void error(Token token, String message) {
        hadError = true;
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message, 0);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message, 0);
		}
	}

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                           "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message, int type) {
        String typeStr = switch (type) {
            case 0 -> "Error";
            case 1 -> "Warning";
            default -> "";
        };
        System.err.println(
                "[line " + line + "] " + typeStr + where + ": " + message);
        System.err.flush();
    }

    static void warning(int line, String message) {
        report(line, "", message, 1);
    }
}
