package org.lang.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {

        String outputDir = "/Users/ssj256x/Code/Java/jlox/src/main/java/org/lang/lox";

        var expr = List.of(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"
        );

        defineAst(outputDir, "Expr", expr);

        var stmt = Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        );

        defineAst(outputDir, "Stmt", stmt);
    }

    private static void defineAst(String outputDir,
                                  String baseName,
                                  List<String> types) throws IOException {

        String path = STR."\{outputDir}/\{baseName}.java";
        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package org.lang.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println(STR."public abstract class \{baseName} {");

            defineVisitor(writer, baseName, types);

            // The AST classes.
            for (String type : types) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            // The base accept() method.
            writer.println();
            writer.println("        public abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {

        writer.println("    public interface Visitor<R> {");
        types.forEach(type -> {
            var typeName = type.split(":")[0].trim();
            writer.println(STR."        R visit\{typeName}\{baseName}(\{typeName} \{baseName.toLowerCase()});");
        });
        writer.println("}");

    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println(STR."    public static class \{className} extends \{baseName} {");

        // Constructor.
        writer.println(STR."        public \{className}(\{fieldList}) {");

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println(STR."            this.\{name} = \{name};");
        }

        writer.println("        }");

        // Visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println(STR."          return visitor.visit\{className}\{baseName}(this);");
        writer.println("        }");

        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println(STR."      public final \{field};");
        }

        writer.println("    }");
    }
}
