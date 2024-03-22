# Lox
This is the exact implementation of the language "Lox" from the Crafting Interpreters book.


#### TODO:
1. Implement the ```AstPrinter.java``` class using the functional approach as defined below
   ```java
   public class AstPrinter {
        public static String print(Expr expr) {
            return switch (expr) {
                case Expr.Binary binary -> parenthesize(binary.operator.lexeme, binary.left, binary.right);
                case Expr.Grouping grouping -> parenthesize("group", grouping.expression);
                case Expr.Literal literal -> literal.value == null ? "nil" : literal.value.toString();
                case Expr.Unary unary -> parenthesize(unary.operator.lexeme, unary.right);
            };
        }
        
        private static String parenthesize(String name, Expr... exprs) {
            StringBuilder builder = new StringBuilder();
            builder.append("(").append(name);
            for (var expr : exprs) {
                builder.append(" ").append(print(expr));
            }
            builder.append(")");
            return builder.toString();
        }
    }

   ```