package org.lang.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class Lox {

    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {

        args = setupPrg(args, true);

        try {
            if (args.length > 1) {
                System.out.println("Usage: jlox [script]");
            } else if (args.length == 1) {
                runFile(args[0]);
            } else {
                runPrompt();
            }
        } catch (RuntimeError e) {
            // swallow
        }
    }

    private static String[] setupPrg(String[] args, boolean init) {

        if (!init) return new String[]{};

        var basePath = "/Users/ssj256x/Code/Java/jlox/src/main/java/org/lang/test/";
        var printPrg = "print.lox";
        var varPrg = "variables.lox";
        var scopesPrg = "scopes.lox";
        var logicalPrg = "logical.lox";
        var whilePrg = "while.lox";
        var forPrg = "for.lox";
        var functionPrg = "functions.lox";
        var fibPrg = "fib.lox";
        var closurePrg = "closure.lox";

        var currentPrg = STR."\{basePath}\{functionPrg}";

        return new String[]{currentPrg};
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
    }

    private static void run(String source) {

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError) return;

        interpreter.interpret(statements);
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message);
        } else {
            report(token.line, STR."at '\{token.lexeme}'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(STR."\{error.getMessage()} [line \{error.token.line}]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println(STR."[line: \{line}] Error \{where} : \{message}");
        hadError = true;
    }
}