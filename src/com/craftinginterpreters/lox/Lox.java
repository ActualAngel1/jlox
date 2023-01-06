package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {

        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64); // [64]
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
        private static void runPrompt() throws IOException {
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);

            for (;;) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break;
                run(line, true);
                hadError = false;
            }
        }
        private static void run(String source, boolean isREPL) {
            /* Clean code note: I know it is considered bad practice to pass booleans into functions,
             but it makes the resulting code more understandable and clean this way
             because the resulting code of separating the function into two will need to declare the entire process
             of scanning, making lists and parsing, thus creating repetitions in the code
             */
            Scanner scanner = new Scanner(source);
            List<Token> tokens = scanner.scanTokens();
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();
            // Stop if there was a syntax error.
            if (hadError) return;

            // I want the REPL session to print expressions
            if (isREPL) runREPL(statements);
            else interpreter.interpret(statements);
        }
        private static void runREPL(List<Stmt> statements){
            for (Stmt statement : statements) {
                List<Stmt> printExpr = new ArrayList<>();
                if (statement instanceof Stmt.Expression) {
                    printExpr.add(new Stmt.Print(((Stmt.Expression) statement).expression));
                    interpreter.interpret(printExpr);
                }
                else{
                    printExpr.add(statement);
                    interpreter.interpret(printExpr);
                }
            }
        }
        static void error(int line, String message) {
            report(line, "", message);
        }

        private static void report(int line, String where,
                String message) {
            System.err.println(
                    "[line " + line + "] Error" + where + ": " + message);
            hadError = true;
        }
        static void error(Token token, String message) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message);
            } else {
                report(token.line, " at '" + token.lexeme + "'", message);
            }
        }
        static void runtimeError(RuntimeError error) {
            System.err.println(error.getMessage() +
                    "\n[line " + error.token.line + "]");
            hadRuntimeError = true;
        }
        static void BreakException(BreakException error) {
            System.err.println(error.getMessage() +
                    "\n[line " + error.token.line + "]");
            hadRuntimeError = true;
        }
    }