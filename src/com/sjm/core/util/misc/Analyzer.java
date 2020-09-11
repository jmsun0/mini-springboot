package com.sjm.core.util.misc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sjm.core.util.core.JSON;
import com.sjm.core.util.core.Lex;
import com.sjm.core.util.core.MyStringBuilder;
import com.sjm.core.util.core.JSON.Serializers;
import com.sjm.core.util.core.Lex.Builder;
import com.sjm.core.util.core.Lex.DFAState;
import com.sjm.core.util.core.Lex.StringLex;

public class Analyzer {

    public static void main1(String[] args) {
        // Builder<StringLex<String>> builder = new Builder<>();
        // builder.initNFA();
        // // builder.setAsciiMapOptimizeThreshold(-1);
        // // builder.setArrayMapOptimizeThreshold(-1);
        //
        // builder.defineActionTemplate("finish", (lex, a) -> lex.finish((String) a[0]));
        // builder.defineActionTemplate("rollback", (lex, a) -> lex.rollback());
        // builder.definePattern("NUMBER", "-?[0-9]+");
        //
        // builder.addPattern("START", "${NUMBER}#{finish('NUMBER')}");
        // builder.addPattern("START", "ab[cd]#{finish('world')}");
        // builder.addPattern("START", "ab[bc]#{finish('hello')}");
        //
        // System.out.println(builder.declaration);
        // DFAState start = builder.buildDFA("START");
        //
        // System.out.println(builder.declaration);
        // System.out.println(builder.stateIdGenerator.id);
        //
        // System.out.println(start.transfer);
        //
        // StringLex<String> lex = new StringLex<String>(start);
        //
        // lex.reset("abb-1abb");
        //
        // System.out.println(lex.next());
        // System.out.println(lex.next());
        // System.out.println(lex.next());

        // JSONLex lex = new JSONLex();
        // lex.reset("1 nul[\"\"");
        //
        // System.out.println(lex.next());
        // System.out.println(lex.next());

        // LL1Pattern num = LL1Pattern.term("num");
        // LL1Pattern id = LL1Pattern.term("id");
        // LL1Pattern add = LL1Pattern.term("+");
        // LL1Pattern sub = LL1Pattern.term("-");
        // LL1Pattern mul = LL1Pattern.term("*");
        // LL1Pattern lsb = LL1Pattern.term("(");
        // LL1Pattern rsb = LL1Pattern.term(")");

        // LL1Pattern a = LL1Pattern.term("a");
        // LL1Pattern b = LL1Pattern.term("b");
        // LL1Pattern c = LL1Pattern.term("c");
        //
        // LL1Pattern S = LL1Pattern.nonterm("S");
        // LL1Pattern Q = LL1Pattern.nonterm("Q");
        // LL1Pattern R = LL1Pattern.nonterm("R");
        //
        // S.patterns = newList(newList(Q, c), newList(c));
        // Q.patterns = newList(newList(R, b), newList(b));
        // R.patterns = newList(newList(S, a), newList(a));
        //
        // List<LL1Pattern> list = newList(R, Q, S);


        // LL1Pattern P = LL1Pattern.nonterm("P");
        //
        // P.patterns = newList(newList(P, add, P), newList(num));
        //
        // List<LL1Pattern> list = newList(P);

        // LL1Pattern E = LL1Pattern.nonterm("E");
        // LL1Pattern E1 = LL1Pattern.nonterm("E1");
        // LL1Pattern T = LL1Pattern.nonterm("T");
        // LL1Pattern T1 = LL1Pattern.nonterm("T1");
        // LL1Pattern F = LL1Pattern.nonterm("F");
        //
        // E.patterns = newList(newList(T, E1));
        // E1.patterns = newList(newList(add, T, E1), LL1Converter.EPSILON_LIST);
        // T.patterns = newList(newList(F, T1));
        // T1.patterns = newList(newList(mul, F, T1), LL1Converter.EPSILON_LIST);
        // F.patterns = newList(newList(lsb, E, rsb), newList(id));
        //
        // List<LL1Pattern> list = newList(E, E1, T, T1, F);
        //
        // list.forEach(System.out::println);
        // System.out.println();

        // LL1Converter.removeLeftRecursion(list);
        // list.forEach(System.out::println);
        // System.out.println();

        // LL1Converter.calculateFirst(list);
        // list.forEach(System.out::println);
        // System.out.println();
        //
        // LL1Converter.calculateFollow(list);
        // list.forEach(System.out::println);
        // System.out.println();
    }

    @SafeVarargs
    private static <T> List<T> newList(T... values) {
        List<T> list = new ArrayList<>();
        for (T value : values)
            list.add(value);
        return list;
    }

    static class LL1Converter {
        private static int tmpOrdinal = 0;
        private static boolean hasAction = true;

        public static LL1Pattern[] process(List<LL1Pattern> nontermList,
                List<LL1Pattern> termList) {
            removeLeftRecursion(nontermList);
            extractCommonLeft(nontermList);
            LL1Pattern[] nonterms = nontermList.toArray(new LL1Pattern[nontermList.size()]);
            calculateFirst(nonterms);
            calculateFollow(nonterms);
            calculateAnalyzeTable(nonterms, termList.size());
            return nonterms;
        }

        private static void removeLeftRecursion(List<LL1Pattern> nonterms) {
            for (int i = 0, len = nonterms.size(); i < len; i++) {
                LL1Pattern pattern = nonterms.get(i);
                for (int j = 0; j < i; j++)
                    replaceFirst(pattern, nonterms.get(j));
                LL1Pattern tmp = removeDirectLeftRecursion(pattern);
                if (tmp != null)
                    nonterms.add(tmp);
            }
        }

        private static LL1Pattern removeDirectLeftRecursion(LL1Pattern nonterms) {
            List<LL1Prod> list = nonterms.prods;
            int index;
            for (index = 0; index < list.size(); index++)
                if (list.get(index).pwas.get(0).pattern == nonterms)
                    break;
            if (index == list.size())
                return null;
            List<LL1Prod> newList = newList();
            LL1Pattern tmp = LL1Pattern.nonterm("TMP_" + tmpOrdinal++, newList);
            for (int i = list.size() - 1; i >= 0; i--) {
                List<PatternWithAction> pwas = list.get(i).pwas;
                PatternWithAction newTmp = new PatternWithAction(tmp, null);
                if (pwas.get(0).pattern == nonterms) {
                    LL1Prod prod = list.remove(i);
                    List<PatternWithAction> tmpPwas = prod.pwas;
                    if (hasAction) {
                        LL1Action action = tmpPwas.get(tmpPwas.size() - 1).action;
                        action.expr.move(-1);
                        action.expr.replace(-1, -1, "i");
                        action.dst = new LL1Variable(tmpPwas.size() - 1, "i");
                        newTmp.action = new LL1Action(new LL1Variable(-1, "s"),
                                new LL1Variable(tmpPwas.size() - 1, "s"));
                    }
                    tmpPwas.remove(0);
                    tmpPwas.add(newTmp);
                    newList.add(prod);
                } else {
                    if (hasAction) {
                        LL1Action action = pwas.get(pwas.size() - 1).action;
                        LL1Variable dst = action.dst;
                        action.dst = new LL1Variable(pwas.size(), "i");
                        newTmp.action = new LL1Action(dst, new LL1Variable(pwas.size(), "s"));
                    }
                    pwas.add(newTmp);
                }
            }
            LL1Action action =
                    hasAction ? new LL1Action(new LL1Variable(-1, "s"), new LL1Variable(-1, "i"))
                            : null;
            newList.add(new LL1Prod(newList(new PatternWithAction(LL1Pattern.EPSILON, action))));
            return tmp;
        }

        private static void replaceFirst(LL1Pattern pattern, LL1Pattern replacement) {
            List<LL1Prod> prods = pattern.prods;
            List<LL1Prod> replaceProds = replacement.prods;
            for (int i = 0; i < prods.size(); i++) {
                LL1Prod prod = prods.get(i);
                List<PatternWithAction> pwas = prod.pwas;
                if (pwas.get(0).pattern == replacement) {
                    List<PatternWithAction> subPwas = replaceProds.get(0).pwas;
                    if (hasAction) {
                        PatternWithAction last = pwas.get(pwas.size() - 1);
                        LL1Action action = last.action;
                        PatternWithAction subLast = subPwas.get(subPwas.size() - 1);
                        LL1Action subAction = subLast.action;
                        action.expr.move(subPwas.size() - 1);
                        action.replace(subPwas.size() - 1, subAction.expr);
                        subLast.action = pwas.size() == 1 ? action : null;
                    }
                    pwas.remove(0);
                    pwas.addAll(0, subPwas);
                    for (int j = 1; j < replaceProds.size(); j++)
                        prods.add(mergeProd(replaceProds.get(j), prod));
                }
            }
        }

        private static LL1Prod mergeProd(LL1Prod prod1, LL1Prod prod2) {
            List<PatternWithAction> newList = newList();
            for (PatternWithAction pwa : prod1.pwas)
                newList.add((PatternWithAction) pwa.clone());
            for (PatternWithAction pwa : prod2.pwas)
                newList.add((PatternWithAction) pwa.clone());
            if (hasAction) {

            }
            newList.addAll(prod1.pwas);
            newList.addAll(prod2.pwas);
            return new LL1Prod(newList);
        }

        private static void extractCommonLeft(List<LL1Pattern> nonterms) {

        }

        private static void calculateFirst(LL1Pattern[] nonterms) {
            ChangedStatus status = new ChangedStatus();
            do {
                status.changed = false;
                for (LL1Pattern pattern : nonterms) {
                    FirstTable patternFirst = pattern.first;
                    List<LL1Prod> prods = pattern.prods;
                    for (int i = 0; i < prods.size(); i++) {
                        LL1Prod prod = prods.get(i);
                        FirstTable prodFirst = prod.first;
                        List<PatternWithAction> pwas = prod.pwas;
                        PatternWithAction pwa = pwas.get(0);
                        LL1Pattern first = pwa.pattern;
                        switch (first.type) {
                            case TERM:
                                add(status, patternFirst.first, first);
                                add(status, prodFirst.first, first);
                                break;
                            case EPSILON:
                                addFirstEpslion(status, patternFirst);
                                addFirstEpslion(status, prodFirst);
                                break;
                            case NONTERM:
                                boolean allHasEpsilon = true;
                                for (int j = 0; j < pwas.size(); j++) {
                                    LL1Pattern ptn = pwas.get(j).pattern;
                                    addAll(status, patternFirst.first, ptn.first.first);
                                    addAll(status, prodFirst.first, ptn.first.first);
                                    if (!hasEpsilon(ptn)) {
                                        allHasEpsilon = false;
                                        break;
                                    }
                                }
                                if (allHasEpsilon) {
                                    addFirstEpslion(status, patternFirst);
                                    addFirstEpslion(status, prodFirst);
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                    }
                }
            } while (status.changed);
        }

        private static void calculateFollow(LL1Pattern[] nonterms) {
            ChangedStatus status = new ChangedStatus();
            nonterms[0].follow.followHasDoller = true;
            do {
                status.changed = false;
                for (LL1Pattern pattern : nonterms) {
                    List<LL1Prod> prods = pattern.prods;
                    for (int i = 0; i < prods.size(); i++) {
                        LL1Prod prod = prods.get(i);
                        List<PatternWithAction> pwas = prod.pwas;
                        for (int j = 0; j < pwas.size(); j++) {
                            PatternWithAction pwa = pwas.get(j);
                            LL1Pattern ptn = pwa.pattern;
                            FollowTable ptnFollow = ptn.follow;
                            if (ptn.type == LL1PatternType.NONTERM) {
                                boolean followHasEpsilon = true;
                                if (j != pwas.size() - 1) {
                                    LL1Pattern followPtn = pwas.get(j + 1).pattern;
                                    if (followPtn.type == LL1PatternType.NONTERM) {
                                        addAll(status, ptnFollow.follow, followPtn.first.first);
                                    } else {
                                        add(status, ptnFollow.follow, followPtn);
                                    }
                                    followHasEpsilon = hasEpsilon(followPtn);
                                }
                                if (followHasEpsilon) {
                                    addAll(status, ptnFollow.follow, pattern.follow.follow);
                                    if (pattern.follow.followHasDoller)
                                        addFollowDoller(status, ptnFollow);
                                }
                            }
                        }
                    }
                }
            } while (status.changed);
        }

        private static void calculateAnalyzeTable(LL1Pattern[] nonterms, int termSize) {
            for (LL1Pattern nonterm : nonterms) {
                LL1Prod[] table = nonterm.analyzeTable = new LL1Prod[termSize + 1];
                for (LL1Prod prod : nonterm.prods) {
                    for (LL1Pattern first : prod.first.first)
                        table[first.ordinal] = prod;
                    if (prod.first.firstHasEpsilon) {
                        for (LL1Pattern follow : nonterm.follow.follow) {
                            if (table[follow.ordinal] != null)
                                throw new IllegalArgumentException("二义文法");
                            table[follow.ordinal] = prod;
                        }
                        if (nonterm.follow.followHasDoller)
                            table[table.length - 1] = prod;
                    }
                }
            }
        }

        private static boolean hasEpsilon(LL1Pattern pattern) {
            switch (pattern.type) {
                case TERM:
                    return false;
                case NONTERM:
                    List<LL1Prod> prods = pattern.prods;
                    for (int i = 0; i < prods.size(); i++)
                        if (prods.get(i).pwas.get(0).pattern.type == LL1PatternType.EPSILON)
                            return true;
                    return false;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        static class ChangedStatus {
            public boolean changed;
        }

        private static void add(ChangedStatus status, Set<LL1Pattern> set, LL1Pattern element) {
            if (set.add(element))
                status.changed = true;
        }

        private static void addFirstEpslion(ChangedStatus status, FirstTable first) {
            if (!first.firstHasEpsilon) {
                first.firstHasEpsilon = true;
                status.changed = true;
            }
        }

        private static void addAll(ChangedStatus status, Set<LL1Pattern> set,
                Collection<LL1Pattern> col) {
            if (set.addAll(col))
                status.changed = true;
        }

        private static void addFollowDoller(ChangedStatus status, FollowTable follow) {
            if (!follow.followHasDoller) {
                follow.followHasDoller = true;
                status.changed = true;
            }
        }
    }

    abstract static class LL1Expr extends BaseObject {
        public void move(int len) {}

        public void replace(int index, int newIndex, String newField) {}

        public void replace(int index, LL1Function func, int argsIndex, LL1Expr replacement) {}

        public abstract Object execute(Namespace ns);
    }
    static class Namespace {
        public LL1Pattern nonterm;
        public LL1Prod prod;
        public Map<LL1Variable, Object> values = new HashMap<>();
        public Map<String, Function<Object[], Object>> functions = new HashMap<>();

        public Namespace(LL1Pattern nonterm, LL1Prod prod) {
            this.nonterm = nonterm;
            this.prod = prod;
        }
    }
    static class LL1Variable extends LL1Expr {
        public int index;
        public String field;

        public LL1Variable(int index, String field) {
            this.index = index;
            this.field = field;
        }

        @Override
        public Object execute(Namespace ns) {
            return ns.values.get(this);
        }

        @Override
        public void move(int len) {
            index += len;
        }

        @Override
        public void replace(int index, int newIndex, String newField) {
            if (this.index == index) {
                this.index = newIndex;
                this.field = newField;
            }
        }

        @Override
        public void replace(int index, LL1Function func, int argsIndex, LL1Expr replacement) {
            if (this.index == index)
                func.args[argsIndex] = replacement;
        }

        @Override
        public int hashCode() {
            return index ^ field.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            LL1Variable that = (LL1Variable) obj;
            return index == that.index && field.equals(that.field);
        }
    }

    static class LL1Const extends LL1Expr {
        public Object value;

        public LL1Const(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(Namespace ns) {
            return value;
        }
    }

    static class LL1Function extends LL1Expr {
        public String name;
        public LL1Expr[] args;

        public LL1Function(String name, LL1Expr[] args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public Object execute(Namespace ns) {
            Object[] a = new Object[args.length];
            for (int i = 0; i < args.length; i++)
                a[i] = args[i].execute(ns);
            return ns.functions.get(name).apply(a);
        }

        @Override
        public void move(int len) {
            for (LL1Expr expr : args)
                expr.move(len);
        }

        @Override
        public void replace(int index, int newIndex, String newField) {
            for (LL1Expr expr : args)
                expr.replace(index, newIndex, newField);
        }

        @Override
        public void replace(int index, LL1Function func, int argsIndex, LL1Expr replacement) {
            for (int i = 0; i < args.length; i++) {
                args[i].replace(index, this, i, replacement);
            }
        }

        @Override
        public Object clone() {
            LL1Function obj = (LL1Function) super.clone();
            LL1Expr[] newArgs = obj.args = new LL1Expr[args.length];
            for (int i = 0; i < args.length; i++)
                newArgs[i] = (LL1Expr) args[i].clone();
            return obj;
        }
    }

    static class LL1Action extends BaseObject {
        public LL1Variable dst;
        public LL1Expr expr;

        public LL1Action(LL1Variable dst, LL1Expr expr) {
            this.dst = dst;
            this.expr = expr;
        }

        public void replace(int index, LL1Expr replacement) {
            if (expr instanceof LL1Variable) {
                LL1Variable v = (LL1Variable) expr;
                if (v.index == index)
                    expr = replacement;
            } else if (expr instanceof LL1Function) {
                expr.replace(index, null, 0, replacement);
            }
        }
    }

    static class PatternWithAction extends BaseObject {
        public LL1Pattern pattern;
        public LL1Action action;
        public LL1Variable[] dsts;
        public Function<MyLex, Object>[] getters;// for TERM
        public LL1Variable[] srcs;// for NONTERM

        public PatternWithAction(LL1Pattern pattern, LL1Action action) {
            this.pattern = pattern;
            this.action = action;
        }

        @Override
        public Object clone() {
            PatternWithAction obj = (PatternWithAction) super.clone();
            obj.action = (LL1Action) action.clone();
            return obj;
        }
    }

    static class FirstTable extends BaseObject {
        public Set<LL1Pattern> first = new HashSet<>();
        public boolean firstHasEpsilon;
    }

    static class FollowTable extends BaseObject {
        public Set<LL1Pattern> follow = new HashSet<>();
        public boolean followHasDoller;
    }

    static enum LL1PatternType {
        TERM, NONTERM, EPSILON
    }

    static class LL1Prod extends BaseObject {
        public List<PatternWithAction> pwas;
        public FirstTable first;

        public LL1Prod(List<PatternWithAction> pwas) {
            this.pwas = pwas;
            this.first = new FirstTable();
        }
    }

    static class LL1Pattern extends BaseObject {
        public LL1PatternType type;
        public String name;// for ALL
        public int ordinal;// for TERM
        public String friendlyName;// for TERM
        public String regex;// for TERM
        public List<LL1Prod> prods;// for NONTERM
        public FirstTable first;// for NONTERM
        public FollowTable follow;// for NONTERM
        public LL1Prod[] analyzeTable;// for NONTERM

        public LL1Pattern(LL1PatternType type, String name, int ordinal, String friendlyName,
                String regex, List<LL1Prod> prods, FirstTable first, FollowTable follow) {
            this.type = type;
            this.name = name;
            this.ordinal = ordinal;
            this.friendlyName = friendlyName;
            this.regex = regex;
            this.prods = prods;
            this.first = first;
            this.follow = follow;
        }

        public static LL1Pattern term(String name, int ordinal, String friendlyName, String regex) {
            return new LL1Pattern(LL1PatternType.TERM, name, ordinal, friendlyName, regex, null,
                    null, null);
        }

        public static final LL1Pattern EPSILON =
                new LL1Pattern(LL1PatternType.EPSILON, "ε", 0, null, null, null, null, null);

        public static LL1Pattern nonterm(String name, List<LL1Prod> prods) {
            return new LL1Pattern(LL1PatternType.NONTERM, name, 0, null, null, prods,
                    new FirstTable(), new FollowTable());
        }
    }

    abstract static class BaseObject implements Cloneable {
        @Override
        public String toString() {
            return ToStringSupport.toString(this);
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }
    }

    static class ToStringSupport {
        private static Map<Class<?>, Method> appenders = new HashMap<>();
        static {
            try {
                Method[] methods = ToStringSupport.class.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("appendTo")) {
                        Class<?>[] types = method.getParameterTypes();
                        if (types.length == 2 && types[1] == MyStringBuilder.class)
                            appenders.put(types[0], method);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static MyStringBuilder appendAny(Object obj, MyStringBuilder sb) {
            Method method = appenders.get(obj.getClass());
            if (method != null)
                try {
                    return (MyStringBuilder) method.invoke(null, obj, sb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            JSON.Serializers.forAny.serialize(obj, sb);
            return sb;
        }

        public static String toString(Object obj) {
            return appendAny(obj, new MyStringBuilder()).toString();
        }

        public static MyStringBuilder appendTo(LL1Variable v, MyStringBuilder sb,
                LL1Pattern nonterm, LL1Prod prod) {
            if (v.index == -1)
                sb.append(nonterm == null ? "NONTERM" : nonterm.name);
            else
                sb.append(prod == null ? "PROD" : prod.pwas.get(v.index).pattern.name).append('[')
                        .append(v.index).append(']');
            return sb.append('.').append(v.field);
        }

        public static MyStringBuilder appendTo(LL1Variable v, MyStringBuilder sb) {
            return appendTo(v, sb, null, null);
        }

        public static MyStringBuilder appendTo(LL1Const v, MyStringBuilder sb) {
            return sb.append(v.value);
        }

        public static MyStringBuilder appendTo(LL1Function v, MyStringBuilder sb,
                LL1Pattern nonterm, LL1Prod prod) {
            return sb.append(v.name).append("(")
                    .<LL1Expr>appends(v.args, (a, s) -> appendExpr(a, s, nonterm, prod), ",")
                    .append(")");
        }

        public static MyStringBuilder appendTo(LL1Function v, MyStringBuilder sb) {
            return appendTo(v, sb, null, null);
        }

        public static MyStringBuilder appendExpr(LL1Expr v, MyStringBuilder sb, LL1Pattern nonterm,
                LL1Prod prod) {
            if (v instanceof LL1Variable)
                return appendTo((LL1Variable) v, sb, nonterm, prod);
            else if (v instanceof LL1Const)
                return appendTo((LL1Const) v, sb);
            else if (v instanceof LL1Function)
                return appendTo((LL1Function) v, sb, nonterm, prod);
            else
                throw new UnsupportedOperationException();
        }

        public static MyStringBuilder appendTo(LL1Action v, MyStringBuilder sb, LL1Pattern nonterm,
                LL1Prod prod) {
            appendTo(v.dst, sb, nonterm, prod).append("=");
            return appendExpr(v.expr, sb, nonterm, prod);
        }

        public static MyStringBuilder appendTo(LL1Action v, MyStringBuilder sb) {
            return appendTo(v, sb, null, null);
        }

        public static MyStringBuilder appendTo(PatternWithAction v, MyStringBuilder sb,
                LL1Pattern nonterm, LL1Prod prod) {
            sb.append(v.pattern.name);
            if (v.action != null)
                sb.append("  {").append(v.action, (a, s) -> appendTo(a, s, nonterm, prod))
                        .append("}");
            return sb;
        }

        public static MyStringBuilder appendTo(PatternWithAction v, MyStringBuilder sb) {
            return appendTo(v, sb, null, null);
        }

        public static MyStringBuilder appendTo(FirstTable v, MyStringBuilder sb) {
            sb.append("first=[").appends(v.first, (p, s) -> s.append(p.name), ",");
            if (v.firstHasEpsilon) {
                if (!v.first.isEmpty())
                    sb.append(",");
                sb.append(LL1Pattern.EPSILON);
            }
            return sb.append("]");
        }

        public static MyStringBuilder appendTo(FollowTable v, MyStringBuilder sb) {
            sb.append("follow=[").appends(v.follow, (p, s) -> s.append(p.name), ",");
            if (v.followHasDoller) {
                if (!v.follow.isEmpty())
                    sb.append(",");
                sb.append("$");
            }
            return sb.append("]");
        }

        public static MyStringBuilder appendTo(LL1Pattern v, MyStringBuilder sb) {
            switch (v.type) {
                case TERM:
                case EPSILON:
                    return sb.append(v.name);
                case NONTERM:
                    for (int i = 0; i < v.prods.size(); i++) {
                        if (i == 0)
                            sb.append(v.name).append(" --> ");
                        else
                            sb.append(' ', v.name.length()).append("     ");
                        LL1Prod prod = v.prods.get(i);
                        sb.appends(prod.pwas, (p, s) -> appendTo(p, s, v, prod), " ").append("    ")
                                .append(prod.first, ToStringSupport::appendTo).append('\n');
                    }
                    return sb.append(' ', v.name.length()).append("     ")
                            .append(v.first, ToStringSupport::appendTo).append(",")
                            .append(v.follow, ToStringSupport::appendTo);
                default:
                    throw new UnsupportedOperationException();
            }
        }

        public static MyStringBuilder appendTo(LL1Prod v, MyStringBuilder sb) {
            return sb.appends(v.pwas, (p, s) -> s.append(p.pattern.name), " ");
        }
    }

    static class LL1PatternBuildHelper {
        private int termOrdinal;
        private Map<String, LL1Pattern> patterns = new LinkedHashMap<>();
        private Map<String, LL1Pattern> nonterms = new LinkedHashMap<>();

        public LL1Variable V(int index, String field) {
            return new LL1Variable(index, field);
        }

        public LL1Function F(String name, LL1Expr... args) {
            return new LL1Function(name, args);
        }

        public LL1Const C(Object value) {
            return new LL1Const(value);
        }

        public LL1Action A(LL1Variable dst, LL1Expr expr) {
            return new LL1Action(dst, expr);
        }

        public void term(String name, String friendlyName, String regex) {
            patterns.put(name, LL1Pattern.term(name, termOrdinal++, friendlyName, regex));
        }

        private LL1Pattern getPattern(String name) {
            LL1Pattern pattern = patterns.get(name);
            if (pattern == null) {
                nonterms.put(name, pattern = LL1Pattern.nonterm(name, new ArrayList<>()));
                patterns.put(name, pattern);
            }
            return pattern;
        }

        public void nonterm(String name, Object... exprs) {
            LL1Pattern pattern = getPattern(name);
            if (pattern.type != LL1PatternType.NONTERM)
                throw new IllegalArgumentException();
            List<PatternWithAction> pwas = new ArrayList<>();
            for (int i = 0; i < exprs.length;) {
                Object expr = exprs[i++];
                LL1Pattern ptn =
                        expr.equals("EPSILON") ? LL1Pattern.EPSILON : getPattern((String) expr);
                LL1Action action = null;
                if (i < exprs.length && exprs[i] instanceof LL1Action)
                    action = (LL1Action) exprs[i++];
                pwas.add(new PatternWithAction(ptn, action));
            }
            pattern.prods.add(new LL1Prod(pwas));
        }

        public List<LL1Pattern> build1() {
            term("+", "ADD", "\\+#{finish(ADD)}");
            term("-", "SUB", "\\-#{finish(SUB)}");
            term("(", "LSB", "\\(#{finish(LSB)}");
            term(")", "RSB", "\\)#{finish(RSB)}");
            term("id", "ID", "[a-zA-Z_][a-zA-Z0-9_]*#{finish(ID)}");
            term("num", "NUM", "-?[0-9]+(\\.[0-9]+)?#{finish(NUM)}");

            nonterm("E", "E", "+", "T",
                    A(V(-1, "nptr"), F("mknode", C('+'), V(0, "nptr"), V(2, "nptr"))));
            nonterm("E", "E", "-", "T",
                    A(V(-1, "nptr"), F("mknode", C('-'), V(0, "nptr"), V(2, "nptr"))));
            nonterm("E", "T", A(V(-1, "nptr"), V(0, "nptr")));
            nonterm("T", "(", "E", ")", A(V(-1, "nptr"), V(1, "nptr")));
            nonterm("T", "id", A(V(-1, "nptr"), F("mkleaf", C("id"), V(0, "entry"))));
            nonterm("T", "num", A(V(-1, "nptr"), F("mkleaf", C("num"), V(0, "val"))));
            return new ArrayList<>(nonterms.values());
        }

        public List<LL1Pattern> build2() {
            term("+", "ADD", "\\+#{finish(ADD)}");
            term("(", "LSB", "\\(#{finish(LSB)}");
            term(")", "RSB", "\\)#{finish(RSB)}");
            term("id", "ID", "[a-zA-Z_][a-zA-Z0-9_]*#{finish(ID)}");
            term("num", "NUM", "-?[0-9]+(\\.[0-9]+)?#{finish(NUM)}");

            nonterm("E", "E", "+", "num",
                    A(V(-1, "nptr"), F("mknode", C('+'), V(0, "nptr"), V(2, "nptr"))));
            nonterm("E", "num", A(V(-1, "nptr"), F("mkleaf", C("num"), V(0, "val"))));
            nonterm("E", "T", A(V(-1, "nptr"), F("mknode", C("simple"), V(0, "nptr"))));
            nonterm("T", "(", "id", ")", A(V(-1, "nptr"), F("mkleaf", C("id"), V(1, "val"))));
            return new ArrayList<>(nonterms.values());
        }

        public List<LL1Pattern> build3() {
            term("+", "ADD", "\\+#{finish(ADD)}");
            term("*", "MUL", "\\*#{finish(MUL)}");
            term("(", "LSB", "\\(#{finish(LSB)}");
            term(")", "RSB", "\\)#{finish(RSB)}");
            term("id", "ID", "[a-zA-Z_][a-zA-Z0-9_]*#{finish(ID)}");

            nonterm("E", "T", "E1");
            nonterm("E1", "+", "T", "E1");
            nonterm("E1", "EPSILON");
            nonterm("T", "F", "T1");
            nonterm("T1", "*", "F", "T1");
            nonterm("T1", "EPSILON");
            nonterm("F", "(", "E", ")");
            nonterm("F", "id");
            return new ArrayList<>(nonterms.values());
        }

        public List<LL1Pattern> build4() {
            term("a", "A", "\\a#{finish(A)}");
            term("b", "B", "\\b#{finish(B)}");
            term("e", "E", "\\e#{finish(E)}");
            term("i", "I", "\\i#{finish(I)}");
            term("t", "T", "\\t#{finish(T)}");

            nonterm("S", "i", "e", "t", "S", "S1");
            nonterm("S", "a");
            nonterm("S1", "e", "S");
            nonterm("S1", "EPSILON");
            nonterm("E", "b");
            return new ArrayList<>(nonterms.values());
        }

        public List<LL1Pattern> build() {
            term("+", "ADD", "\\+#{finish(ADD)}");
            term("(", "LSB", "\\(#{finish(LSB)}");
            term(")", "RSB", "\\)#{finish(RSB)}");
            term("id", "ID", "[a-zA-Z_][a-zA-Z0-9_]*#{finish(ID)}");
            term("num", "NUM", "-?[0-9]+(\\.[0-9]+)?#{finish(NUM)}");

            nonterm("E", "num", A(V(-1, "nptr"), F("mkleaf", C("num"), V(0, "val"))));
            return new ArrayList<>(nonterms.values());
        }
    }
    static class MyLex extends Lex.StringLex<LL1Pattern> {
        public MyLex(DFAState start) {
            super(start);
        }

        static class MyBuilder extends Lex.Builder<MyLex> {
            public DFAState build(LL1Pattern[] terms, Map<String, String> patternMap,
                    List<String> extraPatterns) {
                initNFA();

                defineActionTemplate("finish", (lex, a) -> lex.finish((LL1Pattern) a[0]));
                defineActionTemplate("rollback", (lex, a) -> lex.rollback());

                for (LL1Pattern term : terms) {
                    defineVariable(term.friendlyName, term);
                }

                for (Map.Entry<String, String> e : patternMap.entrySet()) {
                    definePattern(e.getKey(), e.getValue());
                }

                for (LL1Pattern term : terms) {
                    addPattern("START", term.regex);
                }

                for (String extraPattern : extraPatterns) {
                    addPattern("START", extraPattern);
                }

                return buildDFA("START");
            }
        }
    }
    static class DebugUtils {
        public static void printAnalyzeTable(LL1Pattern[] nonterms, LL1Pattern[] terms) {
            LL1Prod[][] table = new LL1Prod[nonterms.length][];
            for (int i = 0; i < table.length; i++)
                table[i] = nonterms[i].analyzeTable;
            String[] rows = Arrays.stream(nonterms).map(p -> p.name).toArray(String[]::new);
            String[] columns = Arrays.stream(terms).map(p -> p.name).toArray(String[]::new);
            printTable(table, rows, columns, o -> o == null ? "" : o.toString());
        }

        public static <T> void printTable(T[][] table, String[] rows, String[] columns,
                Function<T, String> toStringFunc) {
            String[][] arr = new String[rows.length + 1][];
            for (int i = 0; i < arr.length; i++)
                arr[i] = new String[columns.length + 1];
            arr[0][0] = "";
            for (int i = 0; i < rows.length; i++)
                arr[i + 1][0] = rows[i];
            for (int j = 0; j < columns.length; j++)
                arr[0][j + 1] = columns[j];
            for (int i = 0; i < rows.length; i++)
                for (int j = 0; j < columns.length; j++)
                    arr[i + 1][j + 1] = toStringFunc.apply(table[i][j]);
            int[] maxLengths = new int[columns.length + 1];
            for (int i = 0; i <= rows.length; i++)
                for (int j = 0; j <= columns.length; j++)
                    maxLengths[j] = Math.max(maxLengths[j], arr[i][j].length());
            StringBuilder formatBuffer = new StringBuilder();
            for (int i = 0; i < maxLengths.length; i++)
                formatBuffer.append("%").append(maxLengths[i] + 2).append("s|");
            String format = formatBuffer.toString();
            StringBuilder output = new StringBuilder();
            for (int i = 0; i <= rows.length; i++)
                output.append(String.format(format, (Object[]) arr[i])).append("\n");
            System.out.println(output);
        }

        public static void match(LL1Pattern start, MyLex lex) {
            Stack<LL1Pattern> stack = new Stack<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                LL1Pattern peek = stack.peek();
                if (peek.type == LL1PatternType.TERM) {
                    if (lex.key != peek)
                        throw lex.newError();
                    stack.pop();
                    lex.next();
                } else {
                    LL1Prod prod = peek.analyzeTable[lex.key.ordinal];
                    if (prod == null)
                        throw lex.newError();
                    stack.pop();
                    if (!prod.first.firstHasEpsilon) {
                        List<PatternWithAction> pwas = prod.pwas;
                        for (int i = pwas.size() - 1; i >= 0; i--) {
                            stack.push(pwas.get(i).pattern);
                        }
                    }
                    System.out.println(prod);
                }
            }
        }

        public static void matchTest() {
            LL1PatternBuildHelper builder = new LL1PatternBuildHelper();
            List<LL1Pattern> nontermList = builder.build3();
            List<LL1Pattern> termList = builder.patterns.values().stream()
                    .filter(p -> p.type == LL1PatternType.TERM).collect(Collectors.toList());

            LL1Pattern[] nonterms = LL1Converter.process(nontermList, termList);
            Arrays.stream(nonterms).forEach(System.out::println);
            System.out.println();

            termList.add(LL1Pattern.term("$", termList.size(), "EOF", "[$]#{finish(EOF)}"));
            LL1Pattern[] terms = termList.toArray(new LL1Pattern[termList.size()]);

            DebugUtils.printAnalyzeTable(nonterms, terms);

            Map<String, String> patternMap = new HashMap<String, String>();
            List<String> extraPatterns = new ArrayList<>();

            extraPatterns.add("[\r\n\t\b\f ]+#{finish(null)}");

            Lex.DFAState start = new MyLex.MyBuilder().build(terms, patternMap, extraPatterns);
            MyLex lex = new MyLex(start);

            lex.resetAndNext("a+a* b(b+r)+a");

            match(nonterms[0], lex);
        }

        public static void eval(LL1Pattern start, MyLex lex) {
            Stack<Object> stack = new Stack<>();
            Stack<Namespace> nsStack = new Stack<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                Object pattenrOrAction = stack.peek();
                if (pattenrOrAction == null) {
                    stack.pop();
                    // Namespace ns1 = nsStack.pop();
                    // Namespace ns2 = nsStack.peek();
                    // ns2.values.put(key, value);
                    // System.out.println(nsStack.pop());
                } else if (pattenrOrAction instanceof LL1Pattern) {
                    LL1Pattern peekPattern = (LL1Pattern) pattenrOrAction;
                    if (peekPattern.type == LL1PatternType.TERM) {
                        if (lex.key != peekPattern)
                            throw lex.newError();
                        stack.pop();
                        lex.next();
                    } else if (peekPattern.type == LL1PatternType.NONTERM) {
                        LL1Prod prod = peekPattern.analyzeTable[lex.key.ordinal];
                        if (prod == null)
                            throw lex.newError();
                        stack.pop();
                        stack.push(null);
                        nsStack.push(new Namespace(peekPattern, prod));
                        List<PatternWithAction> pwas = prod.pwas;
                        for (int i = pwas.size() - 1; i >= 0; i--) {
                            PatternWithAction pwa = pwas.get(i);
                            if (pwa.action != null)
                                stack.push(pwa.action);
                            stack.push(pwa.pattern);
                        }
                    } else {
                        stack.pop();
                    }
                } else {
                    LL1Action action = (LL1Action) pattenrOrAction;
                    stack.pop();
                    Namespace ns = nsStack.peek();
                    System.out.println(ns.prod);
                    System.out.println(ToStringSupport.appendTo(action, new MyStringBuilder(),
                            ns.nonterm, ns.prod));
                    System.out.println();
                }
            }
        }
    }

    public static void main(String[] args) {
        // LL1Converter.removeLeftRecursion(nontermList);
        // nontermList.forEach(System.out::println);
        // System.out.println();

        // LL1Converter.replaceFirst(nontermList.get(0), nontermList.get(1));
        // nontermList.forEach(System.out::println);
        // System.out.println();


        LL1PatternBuildHelper builder = new LL1PatternBuildHelper();
        List<LL1Pattern> nontermList = builder.build1();
        nontermList.forEach(System.out::println);
        System.out.println();
        List<LL1Pattern> termList = builder.patterns.values().stream()
                .filter(p -> p.type == LL1PatternType.TERM).collect(Collectors.toList());

        LL1Pattern[] nonterms = LL1Converter.process(nontermList, termList);
        Arrays.stream(nonterms).forEach(System.out::println);
        System.out.println();

        termList.add(LL1Pattern.term("$", termList.size(), "EOF", "[$]#{finish(EOF)}"));
        LL1Pattern[] terms = termList.toArray(new LL1Pattern[termList.size()]);

        DebugUtils.printAnalyzeTable(nonterms, terms);

        Map<String, String> patternMap = new HashMap<String, String>();
        List<String> extraPatterns = new ArrayList<>();

        extraPatterns.add("[\r\n\t\b\f ]+#{finish(null)}");

        Lex.DFAState start = new MyLex.MyBuilder().build(terms, patternMap, extraPatterns);
        MyLex lex = new MyLex(start);

        lex.resetAndNext("a+b");

        DebugUtils.eval(nonterms[0], lex);
    }
}
