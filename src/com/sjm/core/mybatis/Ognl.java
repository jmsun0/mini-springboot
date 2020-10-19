package com.sjm.core.mybatis;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.Environment;

import com.sjm.core.mybatis.Ognl.NumberExpression;
import com.sjm.core.util.core.Converter;
import com.sjm.core.util.core.Lex;
import com.sjm.core.util.core.MyStringBuilder;
import com.sjm.core.util.core.Strings;


public class Ognl {
    public static void main(String[] args) throws Exception {
        Object result = OperatorHandler.handleBinary(1.1, OgnlKey.GT, true);
        System.out.println(result);
        System.out.println(result.getClass());
    }

    public static OgnlExpression parseExpression(String exp) {
        return null;
    }

    public static boolean toBoolean(Object obj) {
        return Converter.INSTANCE.convert(obj, boolean.class);
    }

    public static class OgnlContext {
        private Map<String, Object> map = new HashMap<>();

        public Object get(String key) {
            return map.get(key);
        }

        public Object put(String key, Object value) {
            return map.put(key, value);
        }
    }

    public static class OgnlException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public OgnlException(String message, Throwable cause) {
            super(message, cause);
        }

        public OgnlException(Throwable cause) {
            super(cause);
        }

        public OgnlException(String message) {
            super(message);
        }

        public OgnlException() {
            super();
        }
    }

    public interface OgnlExpression {
        public Object apply(OgnlContext ctx);
    }

    interface UnaryOperator {// 一元运算符
        public Object calc(OgnlContext ctx, OgnlExpression value);
    }

    interface BinaryOperator {// 二元运算符
        public Object calc(OgnlContext ctx, OgnlExpression left, OgnlExpression right);
    }

    interface TernaryOperator {// 三元运算符
        public Object calc(OgnlContext ctx, OgnlExpression v1, OgnlExpression v2,
                OgnlExpression v3);
    }

    interface CallOperator {// 函数运算符
        public Object call(OgnlContext ctx, OgnlExpression[] args);
    }

    static class UnaryLeftExpression implements OgnlExpression {// 一元左表达式
        public OgnlExpression value;
        public UnaryOperator op;

        public UnaryLeftExpression(OgnlExpression value, UnaryOperator op) {
            this.value = value;
            this.op = op;
        }

        @Override
        public Object apply(OgnlContext ctx) {
            return op.calc(ctx, value);
        }
    }

    static class BinaryExpression implements OgnlExpression {// 二元表达式
        public Expression left, right;
        public Key key;
        public BinaryOperator op;

        public BinaryExpression(Expression left, Expression right, Key key) {
            this.left = left;
            this.right = right;
            this.key = key;
            this.op = OperatorFactory.getBinaryOperator(key);
        }

        @Override
        public double apply(Environment env) {
            return op.calc(env, left, right);
        }

        @Override
        public String toString() {
            return "(" + left + key.getSymbol() + right + ")";
        }
    }

    static class TernaryExpression implements OgnlExpression {// 三元表达式
        public Expression expr1, expr2, expr3;
        public Key key1, key2;
        public TernaryOperator op;

        public TernaryExpression(Expression expr1, Expression expr2, Expression expr3, Key key1,
                Key key2) {
            this.expr1 = expr1;
            this.expr2 = expr2;
            this.expr3 = expr3;
            this.key1 = key1;
            this.key2 = key2;
            this.op = OperatorFactory.getTernaryOperator(key1, key2);
        }

        @Override
        public double apply(Environment env) {
            return op.calc(env, expr1, expr2, expr3);
        }

        @Override
        public String toString() {
            return "(" + expr1 + key1.getSymbol() + expr2 + key2.getSymbol() + expr3 + ")";
        }
    }

    static class CallExpression implements OgnlExpression {// 函数表达式
        public List<OgnlExpression> args;
        public String name;
        public CallOperator call;

        public CallExpression(List<OgnlExpression> args, String name) {
            this.args = args;
            this.name = name;
            this.call = CallFactory.getCallOperator(name);
        }

        @Override
        public double apply(Environment env) {
            int len = args.size();
            double[] params = new double[len];
            for (int i = 0; i < len; i++)
                params[i] = args.get(i).apply(env);
            return call.call(params);
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append(name).append("(").appends(args, ",").append(")")
                    .toString();
        }
    }

    static class ConstExpression implements OgnlExpression {// 数字表达式
        public Object value;

        public ConstExpression(Object value) {
            this.value = value;
        }

        @Override
        public Object apply(OgnlContext ctx) {
            return value;
        }

        @Override
        public String toString() {
            if (value instanceof String)
                return new MyStringBuilder().append("'").appendEscape((String) value, -1, -1)
                        .append("'").toString();
            else
                return String.valueOf(value);
        }
    }

    static class VariableExpression implements OgnlExpression {// 变量表达式
        public String name;
        public Object[] extra;

        public VariableExpression(String name, Object[] extra) {
            this.name = name;
            this.extra = extra;
        }

        @Override
        public double apply(Environment env) {
            return Converters.convert(env.get(name, extra), Double.class);
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append("${").append(name)
                    .appends(extra, (sb, v) -> sb.append(",").append(v), "").append("}").toString();
        }
    }

    static class OperatorHandler {

        public static Object handleBinary(Object o1, OgnlKey op, Object o2) {
            DataType t1 = getDataType(o1);
            DataType t2 = getDataType(o2);
            BinaryOperatorHandler handler =
                    binaryHandlers[op.ordinal()][t1.ordinal()][t2.ordinal()];
            if (handler == null)
                throw new UnsupportedOperationException(
                        "" + o1 + "(" + t1 + ") " + op.symbol + " " + o2 + "(" + t2 + ")");
            return handler.handle(o1, o2);
        }

        public static Object handleUnaryLeft(OgnlKey op, Object o) {

        }

        interface UnaryOperatorHandler {
            public Object handle(Object o1);
        }

        interface BinaryOperatorHandler {
            public Object handle(Object o1, Object o2);
        }

        static enum DataType {
            INT, LONG, BYTE, SHORT, CHAR, FLOAT, DOUBLE, BOOLEAN, STRING, OBJECT;

            public static final DataType[] VALUES = DataType.values();
        }

        static Map<Class<?>, DataType> dataTypeMap = new IdentityHashMap<>();
        static {
            dataTypeMap.put(Integer.class, DataType.INT);
            dataTypeMap.put(Long.class, DataType.LONG);
            dataTypeMap.put(Byte.class, DataType.BYTE);
            dataTypeMap.put(Short.class, DataType.SHORT);
            dataTypeMap.put(Character.class, DataType.CHAR);
            dataTypeMap.put(Float.class, DataType.FLOAT);
            dataTypeMap.put(Double.class, DataType.DOUBLE);
            dataTypeMap.put(Boolean.class, DataType.BOOLEAN);
            dataTypeMap.put(String.class, DataType.STRING);
        }

        static DataType getDataType(Object o) {
            return o == null ? DataType.OBJECT
                    : dataTypeMap.getOrDefault(o.getClass(), DataType.OBJECT);
        }

        static UnaryOperatorHandler[][] casts;

        static {
            int len = DataType.VALUES.length;
            casts = new UnaryOperatorHandler[len][];
            for (int i = 0; i < casts.length; i++)
                casts[i] = new UnaryOperatorHandler[len];
            DataType[] toTypes = {DataType.INT, DataType.LONG, DataType.FLOAT, DataType.DOUBLE};
            UnaryOperatorHandler[] toCasts =
                    {o -> ((Number) o).intValue(), o -> ((Number) o).longValue(),
                            o -> ((Number) o).floatValue(), o -> ((Number) o).doubleValue()};
            for (int i = 0; i < toTypes.length; i++) {
                DataType to = toTypes[i];
                UnaryOperatorHandler cast = toCasts[i];
                for (DataType from : new DataType[] {DataType.INT, DataType.LONG, DataType.BYTE,
                        DataType.SHORT, DataType.FLOAT, DataType.DOUBLE})
                    casts[from.ordinal()][to.ordinal()] = cast;
                casts[DataType.CHAR.ordinal()][to.ordinal()] = o -> cast.handle((int) (char) o);
            }
            UnaryOperatorHandler castToString = o -> String.valueOf(o);
            for (DataType from : DataType.VALUES)
                casts[from.ordinal()][DataType.STRING.ordinal()] = castToString;
        }

        static BinaryOperatorHandler[][] binaryOps;
        static {
            binaryOps = new BinaryOperatorHandler[OgnlKey.VALUES.length][];
            int len = DataType.VALUES.length;
            for (int i = 0; i < binaryOps.length; i++)
                binaryOps[i] = new BinaryOperatorHandler[len];
            addBinaryOp(OgnlKey.ADD, DataType.INT, (o1, o2) -> (int) o1 + (int) o2);
            addBinaryOp(OgnlKey.ADD, DataType.LONG, (o1, o2) -> (long) o1 + (long) o2);
            addBinaryOp(OgnlKey.ADD, DataType.FLOAT, (o1, o2) -> (float) o1 + (float) o2);
            addBinaryOp(OgnlKey.ADD, DataType.DOUBLE, (o1, o2) -> (double) o1 + (double) o2);
            addBinaryOp(OgnlKey.ADD, DataType.STRING, (o1, o2) -> (String) o1 + (String) o2);

            addBinaryOp(OgnlKey.SUB, DataType.INT, (o1, o2) -> (int) o1 - (int) o2);
            addBinaryOp(OgnlKey.SUB, DataType.LONG, (o1, o2) -> (long) o1 - (long) o2);
            addBinaryOp(OgnlKey.SUB, DataType.FLOAT, (o1, o2) -> (float) o1 - (float) o2);
            addBinaryOp(OgnlKey.SUB, DataType.DOUBLE, (o1, o2) -> (double) o1 - (double) o2);

            addBinaryOp(OgnlKey.MUL, DataType.INT, (o1, o2) -> (int) o1 * (int) o2);
            addBinaryOp(OgnlKey.MUL, DataType.LONG, (o1, o2) -> (long) o1 * (long) o2);
            addBinaryOp(OgnlKey.MUL, DataType.FLOAT, (o1, o2) -> (float) o1 * (float) o2);
            addBinaryOp(OgnlKey.MUL, DataType.DOUBLE, (o1, o2) -> (double) o1 * (double) o2);

            addBinaryOp(OgnlKey.DIV, DataType.INT, (o1, o2) -> (int) o1 / (int) o2);
            addBinaryOp(OgnlKey.DIV, DataType.LONG, (o1, o2) -> (long) o1 / (long) o2);
            addBinaryOp(OgnlKey.DIV, DataType.FLOAT, (o1, o2) -> (float) o1 / (float) o2);
            addBinaryOp(OgnlKey.DIV, DataType.DOUBLE, (o1, o2) -> (double) o1 / (double) o2);

            addBinaryOp(OgnlKey.MOD, DataType.INT, (o1, o2) -> (int) o1 % (int) o2);
            addBinaryOp(OgnlKey.MOD, DataType.LONG, (o1, o2) -> (long) o1 % (long) o2);
            addBinaryOp(OgnlKey.MOD, DataType.FLOAT, (o1, o2) -> (float) o1 % (float) o2);
            addBinaryOp(OgnlKey.MOD, DataType.DOUBLE, (o1, o2) -> (double) o1 % (double) o2);

            addBinaryOp(OgnlKey.GTE, DataType.INT, (o1, o2) -> (int) o1 >= (int) o2);
            addBinaryOp(OgnlKey.GTE, DataType.LONG, (o1, o2) -> (long) o1 >= (long) o2);
            addBinaryOp(OgnlKey.GTE, DataType.FLOAT, (o1, o2) -> (float) o1 >= (float) o2);
            addBinaryOp(OgnlKey.GTE, DataType.DOUBLE, (o1, o2) -> (double) o1 >= (double) o2);

            addBinaryOp(OgnlKey.LTE, DataType.INT, (o1, o2) -> (int) o1 <= (int) o2);
            addBinaryOp(OgnlKey.LTE, DataType.LONG, (o1, o2) -> (long) o1 <= (long) o2);
            addBinaryOp(OgnlKey.LTE, DataType.FLOAT, (o1, o2) -> (float) o1 <= (float) o2);
            addBinaryOp(OgnlKey.LTE, DataType.DOUBLE, (o1, o2) -> (double) o1 <= (double) o2);

            addBinaryOp(OgnlKey.GT, DataType.INT, (o1, o2) -> (int) o1 > (int) o2);
            addBinaryOp(OgnlKey.GT, DataType.LONG, (o1, o2) -> (long) o1 > (long) o2);
            addBinaryOp(OgnlKey.GT, DataType.FLOAT, (o1, o2) -> (float) o1 > (float) o2);
            addBinaryOp(OgnlKey.GT, DataType.DOUBLE, (o1, o2) -> (double) o1 > (double) o2);

            addBinaryOp(OgnlKey.LT, DataType.INT, (o1, o2) -> (int) o1 < (int) o2);
            addBinaryOp(OgnlKey.LT, DataType.LONG, (o1, o2) -> (long) o1 < (long) o2);
            addBinaryOp(OgnlKey.LT, DataType.FLOAT, (o1, o2) -> (float) o1 < (float) o2);
            addBinaryOp(OgnlKey.LT, DataType.DOUBLE, (o1, o2) -> (double) o1 < (double) o2);

            addBinaryOp(OgnlKey.EQ, DataType.INT, (o1, o2) -> (int) o1 == (int) o2);
            addBinaryOp(OgnlKey.EQ, DataType.LONG, (o1, o2) -> (long) o1 == (long) o2);
            addBinaryOp(OgnlKey.EQ, DataType.FLOAT, (o1, o2) -> (float) o1 == (float) o2);
            addBinaryOp(OgnlKey.EQ, DataType.DOUBLE, (o1, o2) -> (double) o1 == (double) o2);
            addBinaryOp(OgnlKey.EQ, DataType.BOOLEAN, (o1, o2) -> (boolean) o1 == (boolean) o2);
            addBinaryOp(OgnlKey.EQ, DataType.STRING, (o1, o2) -> o1.equals(o2));
            addBinaryOp(OgnlKey.EQ, DataType.OBJECT, (o1, o2) -> o1 == o2);

            addBinaryOp(OgnlKey.NEQ, DataType.INT, (o1, o2) -> (int) o1 != (int) o2);
            addBinaryOp(OgnlKey.NEQ, DataType.LONG, (o1, o2) -> (long) o1 != (long) o2);
            addBinaryOp(OgnlKey.NEQ, DataType.FLOAT, (o1, o2) -> (float) o1 != (float) o2);
            addBinaryOp(OgnlKey.NEQ, DataType.DOUBLE, (o1, o2) -> (double) o1 != (double) o2);
            addBinaryOp(OgnlKey.NEQ, DataType.BOOLEAN, (o1, o2) -> (boolean) o1 != (boolean) o2);
            addBinaryOp(OgnlKey.NEQ, DataType.STRING, (o1, o2) -> !o1.equals(o2));
            addBinaryOp(OgnlKey.NEQ, DataType.OBJECT, (o1, o2) -> o1 != o2);

            for (OgnlKey op : new OgnlKey[] {OgnlKey.AND, OgnlKey.SAND}) {
                addBinaryOp(op, DataType.BOOLEAN, (o1, o2) -> (boolean) o1 && (boolean) o2);
            }

            for (OgnlKey op : new OgnlKey[] {OgnlKey.OR, OgnlKey.SOR}) {
                addBinaryOp(op, DataType.BOOLEAN, (o1, o2) -> (boolean) o1 || (boolean) o2);
            }
        }

        private static void addBinaryOp(OgnlKey op, DataType type, BinaryOperatorHandler handler) {
            binaryOps[op.ordinal()][type.ordinal()] = handler;
        }

        static DataType[][] targetTypes;

        static {
            int len = DataType.VALUES.length;
            targetTypes = new DataType[len][];
            for (int i = 0; i < targetTypes.length; i++)
                targetTypes[i] = new DataType[len];
            DataType[] numberTypes = {DataType.INT, DataType.LONG, DataType.BYTE, DataType.SHORT,
                    DataType.FLOAT, DataType.DOUBLE, DataType.CHAR};
            for (DataType left : numberTypes) {
                for (DataType right : numberTypes) {
                    if (left == DataType.DOUBLE || right == DataType.DOUBLE)
                        targetTypes[left.ordinal()][right.ordinal()] = DataType.DOUBLE;
                    else if (left == DataType.FLOAT || right == DataType.FLOAT)
                        targetTypes[left.ordinal()][right.ordinal()] = DataType.FLOAT;
                    else if (left == DataType.LONG || right == DataType.LONG)
                        targetTypes[left.ordinal()][right.ordinal()] = DataType.LONG;
                    else
                        targetTypes[left.ordinal()][right.ordinal()] = DataType.INT;
                }
            }
            for (DataType left : DataType.VALUES) {
                for (DataType right : DataType.VALUES) {
                    if (left == DataType.STRING || right == DataType.STRING)
                        targetTypes[left.ordinal()][right.ordinal()] = DataType.STRING;
                }
            }
            targetTypes[DataType.BOOLEAN.ordinal()][DataType.BOOLEAN.ordinal()] = DataType.BOOLEAN;
        }

        static BinaryOperatorHandler[][][] binaryHandlers;

        static {
            binaryHandlers = new BinaryOperatorHandler[OgnlKey.VALUES.length][][];
            int len = DataType.VALUES.length;
            for (int i = 0; i < binaryHandlers.length; i++) {
                BinaryOperatorHandler[][] arr =
                        binaryHandlers[i] = new BinaryOperatorHandler[len][];
                for (int j = 0; j < arr.length; j++)
                    arr[j] = new BinaryOperatorHandler[len];
            }
            for (OgnlKey op : new OgnlKey[] {OgnlKey.ADD, OgnlKey.SUB, OgnlKey.MUL, OgnlKey.DIV,
                    OgnlKey.MOD, OgnlKey.GTE, OgnlKey.LTE, OgnlKey.GT, OgnlKey.LT, OgnlKey.EQ,
                    OgnlKey.NEQ, OgnlKey.AND, OgnlKey.SAND, OgnlKey.OR, OgnlKey.SOR}) {
                for (DataType left : DataType.VALUES) {
                    for (DataType right : DataType.VALUES) {
                        DataType targetType = targetTypes[left.ordinal()][right.ordinal()];
                        if (targetType == null)
                            continue;
                        BinaryOperatorHandler handler =
                                binaryOps[op.ordinal()][targetType.ordinal()];
                        if (handler == null)
                            continue;
                        binaryHandlers[op.ordinal()][left.ordinal()][right.ordinal()] =
                                getCastBinaryHandler(left, right, targetType, handler);
                    }
                }
            }
        }

        private static BinaryOperatorHandler getCastBinaryHandler(DataType left, DataType right,
                DataType targetType, BinaryOperatorHandler handler) {
            if (targetType == left) {
                if (targetType == right) {
                    return handler;
                } else {
                    UnaryOperatorHandler castRight = casts[right.ordinal()][targetType.ordinal()];
                    return (o1, o2) -> handler.handle(o1, castRight.handle(o2));
                }
            } else {
                UnaryOperatorHandler castLeft = casts[left.ordinal()][targetType.ordinal()];
                if (targetType == right) {
                    return (o1, o2) -> handler.handle(castLeft.handle(o1), o2);
                } else {
                    UnaryOperatorHandler castRight = casts[right.ordinal()][targetType.ordinal()];
                    return (o1, o2) -> handler.handle(castLeft.handle(o1), castRight.handle(o2));
                }
            }
        }

        static UnaryOperatorHandler[][] unaryOps;
        static {
            unaryHandlers = new UnaryOperatorHandler[OgnlKey.VALUES.length][];
            int len = DataType.VALUES.length;
            for (int i = 0; i < unaryOps.length; i++)
                unaryOps[i] = new UnaryOperatorHandler[len];

            addUnaryOp(OgnlKey.ADD, DataType.INT, o -> (int) o);
            addUnaryOp(OgnlKey.ADD, DataType.LONG, o -> (long) o);
            addUnaryOp(OgnlKey.ADD, DataType.FLOAT, o -> (float) o);
            addUnaryOp(OgnlKey.ADD, DataType.DOUBLE, o -> (double) o);

            addUnaryOp(OgnlKey.SUB, DataType.INT, o -> -(int) o);
            addUnaryOp(OgnlKey.SUB, DataType.LONG, o -> -(long) o);
            addUnaryOp(OgnlKey.SUB, DataType.FLOAT, o -> -(float) o);
            addUnaryOp(OgnlKey.SUB, DataType.DOUBLE, o -> -(double) o);

            addUnaryOp(OgnlKey.NOT, DataType.BOOLEAN, o -> !(boolean) o);
        }

        private static void addUnaryOp(OgnlKey op, DataType type, UnaryOperatorHandler handler) {
            unaryOps[op.ordinal()][type.ordinal()] = handler;
        }

        static UnaryOperatorHandler[][] unaryHandlers;
    }

    static class OperatorFactory {// 运算符工厂
        private static Map<OgnlKey, UnaryOperator> unaryMap = new HashMap<>();
        private static Map<OgnlKey, BinaryOperator> binaryMap = new HashMap<>();
        private static Map<OgnlKey, Map<OgnlKey, TernaryOperator>> ternaryMap = new HashMap<>();

        static {
            binaryMap.put(OgnlKey.ADD, (env, a, b) -> a.apply(env) + b.apply(env));
            binaryMap.put(OgnlKey.SUB, (env, a, b) -> a.apply(env) - b.apply(env));
            binaryMap.put(OgnlKey.MUL, (env, a, b) -> a.apply(env) * b.apply(env));
            binaryMap.put(OgnlKey.DIV, (env, a, b) -> a.apply(env) / b.apply(env));
            binaryMap.put(OgnlKey.MOD, (env, a, b) -> a.apply(env) % b.apply(env));
            binaryMap.put(OgnlKey.GTE, (env, a, b) -> a.apply(env) >= b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.LTE, (env, a, b) -> a.apply(env) <= b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.EQ, (env, a, b) -> a.apply(env) == b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.NEQ, (env, a, b) -> a.apply(env) != b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.GT, (env, a, b) -> a.apply(env) > b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.LT, (env, a, b) -> a.apply(env) < b.apply(env) ? 1 : 0);
            binaryMap.put(OgnlKey.AND,
                    (env, a, b) -> a.apply(env) != 0 && b.apply(env) != 0 ? 1 : 0);
            binaryMap.put(OgnlKey.OR,
                    (env, a, b) -> a.apply(env) != 0 || b.apply(env) != 0 ? 1 : 0);

            unaryMap.put(OgnlKey.NOT, (env, a) -> a.apply(env) == 0 ? 1 : 0);
            unaryMap.put(OgnlKey.ADD, (env, a) -> a.apply(env));
            unaryMap.put(OgnlKey.SUB, (env, a) -> -a.apply(env));

            addTernary(OgnlKey.QS, OgnlKey.COLON,
                    (env, a, b, c) -> a.apply(env) != 0 ? b.apply(env) : c.apply(env));
        }

        public static BinaryOperator getBinaryOperator(Key key) {
            return binaryMap.get(key);
        }

        public static UnaryOperator getUnaryOperator(OgnlKey key) {
            return unaryMap.get(key);
        }

        static void addTernary(OgnlKey key1, OgnlKey key2, TernaryOperator op) {
            Map<OgnlKey, TernaryOperator> map = ternaryMap.get(key1);
            if (map == null)
                ternaryMap.put(key1, map = new HashMap<>());
            map.put(key2, op);
        }

        public static TernaryOperator getTernaryOperator(OgnlKey key1, OgnlKey key2) {
            Map<OgnlKey, TernaryOperator> map = ternaryMap.get(key1);
            if (map == null)
                return null;
            return map.get(key2);
        }
    }

    public static enum OgnlKey {
        EOF, STRING, LITERAL, NUMBER, //
        LSB("("), RSB(")"), LMB("["), RMB("]"), LBB("{"), RBB("}"), //
        GTE(">="), LTE("<="), EQ("=="), NEQ("!="), GT(">"), LT("<"), //
        ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"), //
        AND("&&"), OR("||"), NOT("!"), //
        SAND("and"), SOR("or"), INSTANCEOF("instanceof"), // //
        ASSIGN("="), DOT("."), COMMA(","), WELL("#"), AT("@"), QS("?"), COLON(":"), //

        ;
        public final String symbol;

        private OgnlKey(String symbol) {
            this.symbol = symbol;
        }

        private OgnlKey() {
            this(null);
        }

        public static final OgnlKey[] VALUES = OgnlKey.values();
    }

    static class OgnlLex extends Lex.StringLex<OgnlKey> {
        static final DFAState START = new OgnlLexBuilder().build();

        public OgnlLex() {
            super(START);
        }

        static class OgnlLexBuilder extends Lex.Builder<OgnlLex> {
            public DFAState build() {
                initNFA();

                defineActionTemplate("finish", (lex, a) -> lex.finish((OgnlKey) a[0]));
                defineActionTemplate("rollback", (lex, a) -> lex.rollback());

                OgnlKey[] keys = OgnlKey.values();
                for (OgnlKey key : keys)
                    defineVariable(key.name(), key);

                definePattern("BLANK", "[\\r\\n\\t\\b\\f ]+");
                definePattern("STRING", "[a-zA-Z_][a-zA-Z_0-9]*");
                definePattern("ESCAPE", "\\\\(u[0-9a-fA-F]{4})|([^u])");
                definePattern("LITERAL_1", "\'(${ESCAPE}|[^(\\\\\\\')])*\'");
                definePattern("LITERAL_2", "\"(${ESCAPE}|[^(\\\\\\\")])*\"");
                definePattern("LITERAL", "${LITERAL_1}|${LITERAL_2}");
                definePattern("NUMBER", "[0-9]+([.][0-9]+)?");

                addPattern("START", "${BLANK}#{finish(null)}");
                addPattern("START", "[$]#{finish(EOF)}");
                addPattern("START", "${STRING}#{finish(STRING)}");
                addPattern("START", "${LITERAL}#{finish(LITERAL)}");
                addPattern("START", "${NUMBER}#{finish(NUMBER)}");

                for (OgnlKey key : keys) {
                    if (key.symbol != null) {
                        String pattern;
                        if (Strings.isLetterUnderline(key.symbol.charAt(0))) {
                            pattern = key.symbol + "[^(a-zA-Z_0-9)]#{finish(" + key.name()
                                    + ")}#{rollback()}";
                        } else {
                            pattern = new MyStringBuilder().appendUnicode(key.symbol, -1, -1)
                                    .append("#{finish(").append(key.name()).append(")}").toString();
                        }
                        addPattern("START", pattern);
                    }
                }
                return buildDFA("START");
            }
        }

        @Override
        public RuntimeException newError(String message) {
            return new OgnlException(message);
        }
    }

    static class OgnlParser {
        static class OperatorSet {// 运算符集合
            public int yuan;// 元
            public OgnlKey[] ops;// 运算符列表

            public OperatorSet(int yuan, OgnlKey... ops) {
                this.yuan = yuan;
                this.ops = ops;
            }
        }

        // 运算符优先级排序，从上到下增长
        static OperatorSet[] OPS = {//
                new OperatorSet(2, OgnlKey.ASSIGN), // 赋值 运算符
                new OperatorSet(3, OgnlKey.QS, OgnlKey.COLON), // 问号 运算符
                new OperatorSet(2, OgnlKey.OR, OgnlKey.SOR), // 或 运算符
                new OperatorSet(2, OgnlKey.AND, OgnlKey.SAND), // 与 运算符
                new OperatorSet(2, OgnlKey.GT, OgnlKey.LT, OgnlKey.GTE, OgnlKey.LTE, OgnlKey.EQ,
                        OgnlKey.NEQ, OgnlKey.INSTANCEOF), // 比较运算符
                new OperatorSet(2, OgnlKey.ADD, OgnlKey.SUB), // 加减运算符
                new OperatorSet(2, OgnlKey.MUL, OgnlKey.DIV, OgnlKey.MOD), // 乘 除 取余 运算符
                new OperatorSet(1, OgnlKey.NOT, OgnlKey.ADD, OgnlKey.SUB),// 非 正 负 运算符
        };

        static <T> boolean contains(T[] keys, T key) {
            for (Object k : keys)
                if (k == key)
                    return true;
            return false;
        }

        static <T> T containsTernary(T[] keys, T key) {
            for (int i = 0; i < keys.length; i += 2)
                if (keys[i] == key)
                    return keys[i + 1];
            return null;
        }

        static OgnlExpression expr(OgnlLex lex, int level) {// 匹配表达式
            if (level == OPS.length)
                return factor(lex);
            OperatorSet ops = OPS[level];
            if (ops.yuan == 1) {
                OgnlKey key = lex.getKey();
                if (contains(ops.ops, key)) {
                    lex.next();
                    return new UnaryLeftExpression(expr(lex, level), key);
                } else
                    return expr(lex, level + 1);
            } else if (ops.yuan == 2)
                return expr_(lex, expr(lex, level + 1), level);
            else {
                OgnlExpression expr = expr(lex, level + 1);
                OgnlKey key1 = lex.getKey();
                OgnlKey key2 = containsTernary(OPS[level].ops, key1);
                if (key2 != null) {
                    lex.next();
                    OgnlExpression expr2 = expr(lex, level);
                    matchKeyAndNext(lex, key2);
                    OgnlExpression expr3 = expr(lex, level);
                    expr = new TernaryExpression(expr, expr2, expr3, key1, key2);
                }
                return expr;
            }
        }

        static OgnlExpression expr_(OgnlLex lex, OgnlExpression left, int level) {
            OgnlKey key = lex.getKey();
            if (contains(OPS[level].ops, key)) {
                lex.next();
                return expr_(lex, new BinaryExpression(left, expr(lex, level + 1), key), level);
            } else
                return left;
        }

        static OgnlExpression factor(OgnlLex lex) {// 匹配表达式最基本单元
            switch (lex.getKey()) {
                case LSB:
                    matchKeyAndNext(lex, OgnlKey.LSB);
                    OgnlExpression expr = expr(lex, 0);
                    matchKeyAndNext(lex, OgnlKey.RSB);
                    return expr;
                case NUMBER:
                    return new NumberExpression(number(lex).doubleValue());
                case STRING:
                    return call(lex);
                case AT:
                    return call(lex);
                case WELL:
                    return call(lex);
                case LBB:
                    return call(lex);
                default:
                    throw lex.newError();
            }
        }

        private static void matchKeyAndNext(OgnlLex lex, OgnlKey key) {
            if (lex.getKey() != key)
                throw lex.newError();
            lex.next();
        }
    }
}
