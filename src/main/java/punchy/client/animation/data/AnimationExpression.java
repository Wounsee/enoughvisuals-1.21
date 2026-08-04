package punchy.client.animation.data;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression.class */
public final class AnimationExpression {
    private final Node root;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$Node.class */
    private interface Node {
        double eval(double d);
    }

    private AnimationExpression(Node root) {
        this.root = root;
    }

    public static AnimationExpression compile(String source) {
        if (source == null) {
            return null;
        }
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            Parser parser = new Parser(trimmed);
            Node node = parser.parse();
            if (node == null) {
                return null;
            }
            return new AnimationExpression(node);
        } catch (Exception e) {
            return null;
        }
    }

    public double evaluate(double animTimeSeconds) {
        return this.root.eval(animTimeSeconds);
    }

    public static AnimationExpression negated(AnimationExpression base) {
        if (base == null) {
            return null;
        }
        return new AnimationExpression(animTimeSeconds -> {
            return -base.evaluate(animTimeSeconds);
        });
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$ConstantNode.class */
    private static final class ConstantNode extends Record implements Node {
        private final double value;

        private ConstantNode(double value) {
            this.value = value;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ConstantNode.class), ConstantNode.class, "value", "FIELD:Lpunchy/client/animation/data/AnimationExpression$ConstantNode;->value:D").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ConstantNode.class), ConstantNode.class, "value", "FIELD:Lpunchy/client/animation/data/AnimationExpression$ConstantNode;->value:D").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ConstantNode.class, Object.class), ConstantNode.class, "value", "FIELD:Lpunchy/client/animation/data/AnimationExpression$ConstantNode;->value:D").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public double value() {
            return this.value;
        }

        @Override // punchy.client.animation.data.AnimationExpression.Node
        public double eval(double animTimeSeconds) {
            return this.value;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$VariableNode.class */
    private enum VariableNode implements Node {
        ANIM_TIME;

        @Override // punchy.client.animation.data.AnimationExpression.Node
        public double eval(double animTimeSeconds) {
            return animTimeSeconds;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$UnaryNode.class */
    private static final class UnaryNode extends Record implements Node {
        private final char op;
        private final Node operand;

        private UnaryNode(char op, Node operand) {
            this.op = op;
            this.operand = operand;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, UnaryNode.class), UnaryNode.class, "op;operand", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->operand:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, UnaryNode.class), UnaryNode.class, "op;operand", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->operand:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, UnaryNode.class, Object.class), UnaryNode.class, "op;operand", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$UnaryNode;->operand:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public char op() {
            return this.op;
        }

        public Node operand() {
            return this.operand;
        }

        @Override // punchy.client.animation.data.AnimationExpression.Node
        public double eval(double animTimeSeconds) {
            double v = this.operand.eval(animTimeSeconds);
            return this.op == '-' ? -v : v;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$BinaryNode.class */
    private static final class BinaryNode extends Record implements Node {
        private final char op;
        private final Node left;
        private final Node right;

        private BinaryNode(char op, Node left, Node right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, BinaryNode.class), BinaryNode.class, "op;left;right", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->left:Lpunchy/client/animation/data/AnimationExpression$Node;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->right:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, BinaryNode.class), BinaryNode.class, "op;left;right", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->left:Lpunchy/client/animation/data/AnimationExpression$Node;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->right:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, BinaryNode.class, Object.class), BinaryNode.class, "op;left;right", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->op:C", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->left:Lpunchy/client/animation/data/AnimationExpression$Node;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$BinaryNode;->right:Lpunchy/client/animation/data/AnimationExpression$Node;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public char op() {
            return this.op;
        }

        public Node left() {
            return this.left;
        }

        public Node right() {
            return this.right;
        }

        @Override // punchy.client.animation.data.AnimationExpression.Node
        public double eval(double animTimeSeconds) {
            double a = this.left.eval(animTimeSeconds);
            double b = this.right.eval(animTimeSeconds);
            switch (this.op) {
                case '*':
                    return a * b;
                case '+':
                    return a + b;
                case ',':
                case '.':
                default:
                    return 0.0d;
                case '-':
                    return a - b;
                case '/':
                    if (b == 0.0d) {
                        return 0.0d;
                    }
                    return a / b;
            }
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$FunctionNode.class */
    private static final class FunctionNode extends Record implements Node {
        private final String name;
        private final List<Node> args;

        private FunctionNode(String name, List<Node> args) {
            this.name = name;
            this.args = args;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FunctionNode.class), FunctionNode.class, "name;args", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->name:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->args:Ljava/util/List;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FunctionNode.class), FunctionNode.class, "name;args", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->name:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->args:Ljava/util/List;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FunctionNode.class, Object.class), FunctionNode.class, "name;args", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->name:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/AnimationExpression$FunctionNode;->args:Ljava/util/List;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String name() {
            return this.name;
        }

        public List<Node> args() {
            return this.args;
        }

        @Override // punchy.client.animation.data.AnimationExpression.Node
        public double eval(double animTimeSeconds) {
            List<Double> values;
            values = new ArrayList<>(this.args.size());
            for (Node arg : this.args) {
                values.add(Double.valueOf(arg.eval(animTimeSeconds)));
            }
            switch (this.name) {
                case "sin":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.sin(Math.toRadians(values.get(0).doubleValue()));
                case "cos":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.cos(Math.toRadians(values.get(0).doubleValue()));
                case "tan":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.tan(Math.toRadians(values.get(0).doubleValue()));
                case "abs":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.abs(values.get(0).doubleValue());
                case "sqrt":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.sqrt(Math.max(0.0d, values.get(0).doubleValue()));
                case "floor":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.floor(values.get(0).doubleValue());
                case "ceil":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.ceil(values.get(0).doubleValue());
                case "round":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return Math.rint(values.get(0).doubleValue());
                case "min":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    double out = values.get(0).doubleValue();
                    for (int i = 1; i < values.size(); i++) {
                        out = Math.min(out, values.get(i).doubleValue());
                    }
                    return out;
                case "max":
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    double out2 = values.get(0).doubleValue();
                    for (int i2 = 1; i2 < values.size(); i2++) {
                        out2 = Math.max(out2, values.get(i2).doubleValue());
                    }
                    return out2;
                case "clamp":
                    if (values.size() >= 3) {
                        return Math.max(values.get(1).doubleValue(), Math.min(values.get(2).doubleValue(), values.get(0).doubleValue()));
                    }
                    if (values.isEmpty()) {
                        return 0.0d;
                    }
                    return values.get(0).doubleValue();
                default:
                    return 0.0d;
            }
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationExpression$Parser.class */
    private static final class Parser {
        private final String src;
        private int pos;

        private Parser(String src) {
            this.src = src;
        }

        private Node parse() {
            Node n = parseExpression();
            skipWs();
            if (this.pos == this.src.length()) {
                return n;
            }
            return null;
        }

        private Node parseExpression() {
            Node term = parseTerm();
            while (true) {
                Node left = term;
                skipWs();
                if (match('+')) {
                    term = new BinaryNode('+', left, parseTerm());
                } else if (match('-')) {
                    term = new BinaryNode('-', left, parseTerm());
                } else {
                    return left;
                }
            }
        }

        private Node parseTerm() {
            Node unary = parseUnary();
            while (true) {
                Node left = unary;
                skipWs();
                if (match('*')) {
                    unary = new BinaryNode('*', left, parseUnary());
                } else if (match('/')) {
                    unary = new BinaryNode('/', left, parseUnary());
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            skipWs();
            if (match('+')) {
                return new UnaryNode('+', parseUnary());
            }
            if (match('-')) {
                return new UnaryNode('-', parseUnary());
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            skipWs();
            if (match('(')) {
                Node inner = parseExpression();
                expect(')');
                return inner == null ? new ConstantNode(0.0d) : inner;
            }
            if (peekNumberStart()) {
                return new ConstantNode(parseNumber());
            }
            String id = parseIdentifier();
            if (id == null || id.isEmpty()) {
                return new ConstantNode(0.0d);
            }
            String normalized = normalizeIdentifier(id);
            skipWs();
            if (match('(')) {
                List<Node> args = new ArrayList<>();
                skipWs();
                if (!peek(')')) {
                    do {
                        args.add(parseExpression());
                        skipWs();
                    } while (match(','));
                }
                expect(')');
                return new FunctionNode(normalized, args);
            }
            if ("query.anim_time".equals(normalized) || "query.animtime".equals(normalized)) {
                return VariableNode.ANIM_TIME;
            }
            if ("pi".equals(normalized) || "math.pi".equals(normalized)) {
                return new ConstantNode(3.141592653589793d);
            }
            if ("e".equals(normalized) || "math.e".equals(normalized)) {
                return new ConstantNode(2.718281828459045d);
            }
            return new ConstantNode(0.0d);
        }

        private String normalizeIdentifier(String id) {
            String out = id.trim().toLowerCase(Locale.ROOT);
            if (out.startsWith("math.")) {
                String fn = out.substring("math.".length());
                if (!fn.isBlank()) {
                    return fn;
                }
            }
            return out;
        }

        private boolean peekNumberStart() {
            if (this.pos >= this.src.length()) {
                return false;
            }
            char c = this.src.charAt(this.pos);
            return Character.isDigit(c) || c == '.';
        }

        private double parseNumber() {
            int start = this.pos;
            while (this.pos < this.src.length()) {
                char c = this.src.charAt(this.pos);
                if (Character.isDigit(c) || c == '.') {
                    this.pos++;
                } else if ((c == 'e' || c == 'E') && this.pos + 1 < this.src.length()) {
                    this.pos++;
                    if (this.src.charAt(this.pos) == '+' || this.src.charAt(this.pos) == '-') {
                        this.pos++;
                    }
                }
            }
            try {
                return Double.parseDouble(this.src.substring(start, this.pos));
            } catch (Exception e) {
                return 0.0d;
            }
        }

        private String parseIdentifier() {
            skipWs();
            if (this.pos >= this.src.length()) {
                return null;
            }
            int start = this.pos;
            char first = this.src.charAt(this.pos);
            if (!Character.isLetter(first) && first != '_') {
                return null;
            }
            this.pos++;
            while (this.pos < this.src.length()) {
                char c = this.src.charAt(this.pos);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                    break;
                }
                this.pos++;
            }
            return this.src.substring(start, this.pos);
        }

        private void skipWs() {
            while (this.pos < this.src.length() && Character.isWhitespace(this.src.charAt(this.pos))) {
                this.pos++;
            }
        }

        private boolean match(char c) {
            if (peek(c)) {
                this.pos++;
                return true;
            }
            return false;
        }

        private boolean peek(char c) {
            skipWs();
            return this.pos < this.src.length() && this.src.charAt(this.pos) == c;
        }

        private void expect(char c) {
            skipWs();
            if (this.pos < this.src.length() && this.src.charAt(this.pos) == c) {
                this.pos++;
            }
        }
    }
}
