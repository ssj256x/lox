package org.lang.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lang.lox.TokenType.*;

/**
 * program        → declaration* EOF ;
 * declaration    → funDecl | varDecl | statement ;
 * funDecl        → "fun" function ;
 * function       → IDENTIFIER "(" parameters? ")" block ;
 * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
 * exprStmt       → expression ";" ;
 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )expression? ";" expression? ")" statement ;
 * ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
 * printStmt      → "print" expression ";" ;
 * returnStmt     → "return" expression? ";" ;
 * whileStmt      → "while" "(" expression ")" statement ;
 * block          → "{" declaration* "}" ;
 * expression     → assignment ;
 * assignment     → IDENTIFIER "=" assignment | equality ;
 * logic_or       → logic_and ( "or" logic_and )* ;
 * logic_and      → equality ( "and" equality )* ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary | primary ;
 * call           → primary ( "(" arguments? ")" )* ;
 * arguments      → expression ( "," expression )* ;
 * primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
 */

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * program → declaration* EOF ;
     *
     * @return parsed statements
     */
    private Stmt declaration() {
        try {
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    /**
     * function → IDENTIFIER "(" parameters? ")" block ;
     *
     * @param kind
     * @return pased statement
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, STR."Expect \{kind} name.");
        consume(LEFT_PAREN, STR."Expect '(' after \{kind} name.");

        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");

        consume(LEFT_BRACE, STR."Expect '{' before \{kind} body.");
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    /**
     * program → exprStmt | printStmt ;
     *
     * @return parsed statements
     */
    private Stmt statement() {
        //@formatter:off
        if (match(FOR))         return forStatement();
        if (match(IF))          return ifStatement();
        if (match(PRINT))       return printStatement();
        if (match(RETURN))      return returnStatement();
        if (match(WHILE))       return whileStatement();
        if (match(LEFT_BRACE))  return new Stmt.Block(block());
        return expressionStatement();
        //@formatter:on
    }

    /**
     * forStmt → "for" "(" ( varDecl | exprStmt | ";" )expression? ";" expression? ")" statement ;
     * <p>
     * NOTE: We will be converting the for statement to a while node.
     *
     * @return parsed statements
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // Initializer
        Stmt initializer;
        if (match(SEMICOLON))
            initializer = null;
        else if (match(VAR))
            initializer = varDeclaration();
        else
            initializer = expressionStatement();

        // Condition
        Expr condition = null;
        if (!check(SEMICOLON))
            condition = expression();

        consume(SEMICOLON, "Expect ';' after loop condition.");

        // Increment
        Expr increment = null;
        if (!check(RIGHT_PAREN))
            increment = expression();

        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // "De-sugaring" the 'for' statement into 'while'
        Stmt body = statement();

        if (increment != null)
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));

        if (condition == null)
            condition = new Expr.Literal(true);

        body = new Stmt.While(condition, body);

        if (initializer != null)
            body = new Stmt.Block(Arrays.asList(initializer, body));

        return body;
    }

    /**
     * ifStmt → "if" "(" expression ")" statement ( "else" statement )? ;
     *
     * @return parsed statements
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * printStmt → "print" expression ";" ;
     *
     * @return parsed statement
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * returnStmt → "return" expression? ";" ;
     *
     * @return parsed statement
     */
    private Stmt returnStatement() {
        Token keyword = previous();

        Expr value = null;
        if (!check(SEMICOLON))
            value = expression();

        consume(SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    /**
     * whileStmt → "while" "(" expression ")" statement ;
     *
     * @return parsed statement
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL))
            initializer = expression();

        consume(SEMICOLON, "Expect ; after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    /**
     * exprStmt → expression ";" ;
     *
     * @return parsed statement
     */
    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * expression → equality ;
     *
     * @return parsed expression
     */
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    /**
     * logic_or → logic_and ( "or" logic_and )* ;
     *
     * @return parsed expression
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * logic_and → equality ( "and" equality )* ;
     *
     * @return parsed expression
     */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * equality → comparison ( ( "!=" | "==" ) comparison )* ;
     *
     * @return parsed expression
     */
    private Expr equality() {
        Expr expr = comparision();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparision();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     *
     * @return parsed expression
     */
    private Expr comparision() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term → factor ( ( "-" | "+" ) factor )* ;
     *
     * @return parsed expression
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor → unary ( ( "/" | "*" ) unary )* ;
     *
     * @return parsed expression
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary → ( "!" | "-" ) unary | primary ;
     *
     * @return parsed expression
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    /**
     * call → primary ( "(" arguments? ")" )* ;
     *
     * @return parsed expression
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN))
                expr = finishCall(expr);
            else
                break;
        }
        return expr;
    }

    /**
     * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
     *
     * @return parsed expression
     */
    private Expr primary() {

        // TODO : try to convert to switch-case
        // @formatter:off
        if (match(FALSE))           return new Expr.Literal(false);
        if (match(TRUE))            return new Expr.Literal(true);
        if (match(NIL))             return new Expr.Literal(null);
        if (match(NUMBER, STRING))  return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER))      return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        // @formatter:on

        throw error(peek(), "Expect expression.");
    }

    /*
        Helper Methods Below
     */

    private Expr finishCall(Expr callee) {
        var arguments = new ArrayList<Expr>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() > 255)
                    error(peek(), "Can't have more than 255 arguments.");

                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;

    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
            }
            advance();
        }
    }
}
