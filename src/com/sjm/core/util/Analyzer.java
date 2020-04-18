package com.sjm.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Analyzer<K> {
    public static void main(String[] args) {
        Source<Analyzers.JSONWord> src = Analyzers.JSONAnalyzer.analyze(
                "{\r\n" + "  \"code\": 0,\r\n" + "  \"message\": \"OK\",\r\n" + "  \"data\": {\r\n"
                        + "    \"webNotice\": 0,\r\n" + "    \"smsNotice\": 0,\r\n"
                        + "    \"emailNotice\": 0,\r\n" + "    \"smsServerAddr\": null,\r\n"
                        + "    \"mailServerAddr\": null\r\n" + "  }\r\n" + "}/*dddd*/// adasdas ");
        Source.print(src, Analyzers.JSONWord.EOF);
    }

    public static class Analyzers {
        public static enum JSONWord {
            EOF, BLANK, TEXT, NUMBER, QUOTATION, TRUE, FALSE, NULL, LEFT_BIG_BRACKETS, RIGHT_BIG_BRACKETS, LEFT_MEDIUM_BRACKETS, RIGHT_MEDIUM_BRACKETS, COMMA, COLON, COMMENT
        }

        public static final Analyzer<JSONWord> JSONAnalyzer;
        static {
            Analyzer<JSONWord> alz = JSONAnalyzer = new Analyzer<JSONWord>();
            alz.setText(JSONWord.TEXT);
            alz.setEOF(JSONWord.EOF);
            alz.setBlank(Strings.BlankChars, JSONWord.BLANK);
            alz.setEscape('\'', JSONWord.QUOTATION);
            alz.setEscape('\"', JSONWord.QUOTATION);
            alz.setNumber(JSONWord.NUMBER);
            alz.setString("true", JSONWord.TRUE);
            alz.setString("false", JSONWord.FALSE);
            alz.setString("null", JSONWord.NULL);
            alz.setSymbol("{", JSONWord.LEFT_BIG_BRACKETS);
            alz.setSymbol("}", JSONWord.RIGHT_BIG_BRACKETS);
            alz.setSymbol("[", JSONWord.LEFT_MEDIUM_BRACKETS);
            alz.setSymbol("]", JSONWord.RIGHT_MEDIUM_BRACKETS);
            alz.setSymbol(",", JSONWord.COMMA);
            alz.setSymbol(":", JSONWord.COLON);
            alz.setPattern("//.*?[$\\n]", JSONWord.COMMENT);
            alz.setPattern("/\\*.*?\\*/", JSONWord.COMMENT);
        }
    }

    private CharFilter textHead = Ftr.TextHead, textBody = Ftr.TextBody, textTail = Ftr.TextTail;
    private State start;

    public Analyzer() {
        start = new DefaultState();
    }

    public void setModel(Model model) {
        model.addState(0, this.start);
        model.apply();
    }

    public void setPattern(Pattern pattern, K key) {
        Model model = new Model();
        PatternContext ctx = new PatternContext(model);
        pattern.build(ctx);
        model.addAction(ctx.last, Action.finish(key));
        setModel(model);
    }

    public void setPattern(CharSequence regex, K key) {
        setPattern(Pattern.compile(regex), key);
    }

    public void setText(K key) {
        Pattern pattern = Pattern.link(Pattern.single(textHead),
                Pattern.repeat(Pattern.single(textBody), 0, -1, true));
        setPattern(pattern, key);
    }

    public void setString(CharSequence str, K key) {
        Object[] patterns = new Object[str.length() + 2];
        for (int i = 0; i < str.length(); i++)
            patterns[i] = Pattern.single(str.charAt(i));
        patterns[str.length()] = Pattern.single(textTail);
        patterns[str.length() + 1] = Action.Back;
        Pattern pattern = Pattern.link(patterns);
        setPattern(pattern, key);
    }

    public void setSymbol(CharSequence str, K key) {
        Object[] patterns = new Object[str.length()];
        for (int i = 0; i < patterns.length; i++)
            patterns[i] = Pattern.single(str.charAt(i));
        Pattern pattern = Pattern.link(patterns);
        setPattern(pattern, key);
    }

    public void setBlank(CharSequence chars, K key) {
        for (int i = 0; i < chars.length(); i++) {
            Pattern pattern = Pattern.single(chars.charAt(i));
            setPattern(pattern, key);
        }
    }

    public void setEOF(K key) {
        Pattern pattern = Pattern.single(-1);
        setPattern(pattern, key);
    }

    public void setEscape(char quotation, K key) {
        Model model = new Model();
        model.addLine(0, quotation, 1);
        model.addLine(1, '\\', 2);
        model.addLine(1, quotation, 3);
        model.addLine(1, CharFilter.Any, 1);
        model.addLine(2, CharFilter.Any, 1);

        model.addAction(3, Action.finish(key));

        setModel(model);
    }

    public void setNumber(K key) {
        // Pattern pint = Pattern.repeat(Pattern.single(CharFilter.DecimalNumber), 1, -1, true);
        // Pattern pintfloat = Pattern.link(pint,
        // Pattern.repeat(Pattern.link(Pattern.single('.'), pint), 0, 1, true));
        // Pattern pScience =
        // Pattern.link(pintfloat,
        // Pattern.repeat(Pattern.link(Pattern.single(
        // CharFilter.or(CharFilter.equal('e'), CharFilter.equal('E'))), pint),
        // 0, 1, true));
        // setPattern(pScience, key);
        CharFilter AddSub = CharFilter.or(CharFilter.equal('+'), CharFilter.equal('-'));
        CharFilter eE = CharFilter.or(CharFilter.equal('e'), CharFilter.equal('E'));
        Model model = new Model();
        model.addLine(0, AddSub, 1);
        model.addLine(0, CharFilter.DecimalNumber, 2);
        model.addLine(1, CharFilter.DecimalNumber, 2);
        model.addLine(2, CharFilter.DecimalNumber, 2);
        model.addLine(2, CharFilter.equal('.'), 4);
        model.addLine(2, eE, 6);
        model.addLine(2, null, 3);
        model.addLine(4, CharFilter.DecimalNumber, 5);
        model.addLine(5, CharFilter.DecimalNumber, 5);
        model.addLine(5, eE, 6);
        model.addLine(5, null, 3);
        model.addLine(6, AddSub, 7);
        model.addLine(6, CharFilter.DecimalNumber, 8);
        model.addLine(7, CharFilter.DecimalNumber, 8);
        model.addLine(8, CharFilter.DecimalNumber, 8);
        model.addLine(8, null, 3);

        model.addAction(3, Action.finish(key));
        setModel(model);
    }

    public Source<K> analyze(final CharSequence str) {
        final Context ctx = new DefaultContext(str);
        return new Source<K>() {
            int index;
            CharSequence value = new CharSequence() {
                @Override
                public CharSequence subSequence(int start, int end) {
                    return str.subSequence(index + start, index + end);
                }

                @Override
                public int length() {
                    return ctx.index() - index;
                }

                @Override
                public char charAt(int i) {
                    return str.charAt(index + i);
                }

                @Override
                public String toString() {
                    return new MyStringBuilder().append(str, null, index, ctx.index()).toString();
                }
            };

            @Override
            public CharSequence getValue() {
                return value;
            }

            @SuppressWarnings("unchecked")
            @Override
            public K next() {
                try {
                    index = ctx.index();
                    start.move(ctx);
                } catch (SuccessException e) {
                    return (K) ctx.getKey();
                } catch (Throwable e) {
                    throw new RuntimeException("next() occur an exception ["
                            + e.getClass().getSimpleName() + "] ,string=" + str);
                }
                throw new RuntimeException();
            }
        };
    }

    interface State {
        public void add(Object con, State[] sts);

        public void move(Context ctx);

        public void addAction(Action action);
    }

    public static class DefaultState implements State {
        static class FilterStates {
            CharFilter filter;
            State[] states;

            FilterStates(CharFilter filter, State[] states) {
                this.filter = filter;
                this.states = states;
            }
        }

        private Map<Integer, List<State>> stateMap = Collections.emptyMap();
        private List<FilterStates> stateList = Collections.emptyList();
        private List<Action> actions = Collections.emptyList();

        private void add(int ch, State... sts) {
            if (stateMap.isEmpty())
                stateMap = new HashMap<>();
            List<State> list = stateMap.get(ch);
            if (list == null)
                stateMap.put(ch, list = new ArrayList<>());
            for (State st : sts)
                list.add(st);
        }

        private void add(CharFilter cf, State... sts) {
            if (stateList.isEmpty())
                stateList = new ArrayList<>();
            stateList.add(new FilterStates(cf, sts));
        }

        @Override
        public void add(Object con, State[] sts) {
            if (con instanceof Integer)
                add((int) con, sts);
            else
                add((CharFilter) con, sts);
        }

        @Override
        public void move(Context ctx) {
            for (int i = 0, len = actions.size(); i < len; i++)
                actions.get(i).perform(this, ctx);
            int ch = ctx.read();
            List<State> sts = stateMap.get(ch);
            if (sts != null)
                for (int i = 0, len = sts.size(); i < len; i++)
                    sts.get(i).move(ctx);
            for (int i = 0, len = stateList.size(); i < len; i++) {
                FilterStates fst = stateList.get(i);
                if (fst.filter == null) {
                    ctx.back();
                    for (State states : fst.states)
                        states.move(ctx);
                    ctx.read();
                } else if (fst.filter.accept(ch)) {
                    for (State states : fst.states)
                        states.move(ctx);
                }
            }
            ctx.back();
        }

        @Override
        public void addAction(Action action) {
            if (actions.isEmpty())
                actions = new ArrayList<>();
            actions.add(action);
        }
    }
    static class SuccessException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public static final SuccessException INSTANCE = new SuccessException();
    }
    interface Context {
        public int read();

        public void back();

        public int index();

        public void setKey(Object key);

        public Object getKey();
    }
    static class DefaultContext implements Context {
        private CharSequence str;
        private int index;
        private int end;
        private Object key;

        DefaultContext(CharSequence str) {
            this.str = str;
            this.end = str.length();
        }

        @Override
        public int read() {
            int ch = index < end && index >= 0 ? str.charAt(index) : -1;
            index++;
            return ch;
        }

        @Override
        public void back() {
            index--;
        }

        @Override
        public int index() {
            return Math.min(index, end);
        }

        @Override
        public void setKey(Object key) {
            this.key = key;
        }

        @Override
        public Object getKey() {
            return key;
        }
    }

    public static class Model {
        private Map<Object, Map<Object, Deque<Object>>> lines = new LinkedHashMap<>();
        private Map<Object, State> states = new HashMap<>();
        private Map<Object, List<Action>> actions = new HashMap<>();

        public void addLine(boolean first, Object from, Object con, Object to) {
            if (con instanceof Character)
                con = (int) (char) con;
            Map<Object, Deque<Object>> map = lines.get(from);
            if (map == null)
                lines.put(from, map = new LinkedHashMap<>());
            Deque<Object> dq = map.get(con);
            if (dq == null)
                map.put(con, dq = new LinkedList<>());
            if (first)
                dq.offerFirst(to);
            else
                dq.offerLast(to);
        }

        public void addLine(Object from, Object con, Object to) {
            addLine(false, from, con, to);
        }

        void addState(Object name, State state) {
            states.put(name, state);
        }

        public void addAction(Object name, Action action) {
            List<Action> actionList = actions.get(name);
            if (actionList == null)
                actions.put(name, actionList = new ArrayList<>());
            actionList.add(action);
        }

        public void apply() {
            Set<Object> nameSet = new HashSet<>();
            Set<Object> names = new HashSet<>();

            names.addAll(lines.keySet());
            for (Map<Object, Deque<Object>> map : lines.values())
                for (Deque<Object> dq : map.values())
                    names.addAll(dq);
            for (Object name : names) {
                if (!apply(name, nameSet))
                    throw new IllegalArgumentException("该节点在有向图中不可达：" + name);
                nameSet.clear();
                List<Action> actionList = actions.get(name);
                if (actionList != null)
                    for (Action action : actionList)
                        states.get(name).addAction(action);
            }
            for (Map.Entry<Object, Map<Object, Deque<Object>>> e : lines.entrySet()) {
                Object from = e.getKey();
                for (Map.Entry<Object, Deque<Object>> ee : e.getValue().entrySet()) {
                    Object con = ee.getKey();
                    Deque<Object> dq = ee.getValue();
                    int i = 0;
                    State[] to = new State[dq.size()];
                    for (Object obj : ee.getValue())
                        to[i++] = states.get(obj);
                    states.get(from).add(con, to);
                }
            }
        }

        private boolean apply(Object name, Set<Object> nameSet) {
            State state = states.get(name);
            if (state != null)
                return true;
            for (Map.Entry<Object, Map<Object, Deque<Object>>> e : lines.entrySet()) {
                Object from = e.getKey();
                for (Map.Entry<Object, Deque<Object>> ee : e.getValue().entrySet()) {
                    for (Object to : ee.getValue()) {
                        if (to.equals(name) && !nameSet.contains(from)) {
                            nameSet.add(from);
                            if (apply(from, nameSet)) {
                                // State stateFrom = states.get(from);
                                // stateFrom.add(con, sts);
                                states.put(name, new DefaultState());
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
    interface Ftr {
        CharFilter TextHead = CharFilter.or(CharFilter.JavaNameHead, CharFilter.Chinese);
        CharFilter TextBody = CharFilter.or(CharFilter.JavaNameBody, CharFilter.Chinese);
        CharFilter TextTail = CharFilter.not(TextBody);
    }
    public static abstract class Action {
        public abstract void perform(State state, Context ctx);

        public static Action Back = new Action() {
            @Override
            public void perform(State state, Context ctx) {
                ctx.back();
            }
        };

        public static Action finish(final Object key) {
            return new Action() {
                @Override
                public void perform(State state, Context ctx) {
                    ctx.setKey(key);
                    throw SuccessException.INSTANCE;
                }
            };
        }
    }
    public static class PatternContext {
        public int state;
        public Model model;
        public int first;
        public int secondLast;
        public Object con;
        public int last;

        public PatternContext(Model model) {
            this.model = model;
        }
    }
    public static abstract class Pattern {
        public abstract void build(PatternContext ctx);

        public static Pattern compile(CharSequence regex) {
            return Regex.compile(regex);
        }

        public static Pattern single(final Object con) {
            return new Pattern() {
                @Override
                public void build(PatternContext ctx) {
                    int state = ++ctx.state;
                    ctx.model.addLine(ctx.last, con, state);

                    ctx.first = ctx.last;
                    ctx.secondLast = ctx.last;
                    ctx.con = con;
                    ctx.last = state;
                }

                @Override
                public String toString() {
                    return con.toString();
                }
            };
        }

        public static Pattern link(final Object... args) {
            return new Pattern() {
                @Override
                public void build(PatternContext ctx) {
                    int first = -1;
                    int state = ctx.last;
                    for (Object obj : args) {
                        if (obj instanceof Pattern) {
                            Pattern pattern = (Pattern) obj;
                            pattern.build(ctx);
                            state = ctx.last;
                            if (first == -1)
                                first = ctx.first;
                        } else {
                            ctx.model.addAction(state, (Action) obj);
                        }
                    }
                    ctx.first = first;
                }

                @Override
                public String toString() {
                    return Strings.combine(args, "");
                }
            };
        }

        public static Pattern repeat(final Pattern pattern, final int min, final int max,
                final boolean more) {
            if (min < 0 || max > 0 && min > max || max == 0)
                throw new IllegalArgumentException();
            return new Pattern() {
                @Override
                public void build(PatternContext ctx) {
                    int tmax = max, tmin = min;
                    int first = -1;
                    if (tmin > 1) {
                        int n = min - 1;
                        tmin -= n;
                        tmax -= n;
                        for (int i = 0; i < n; i++) {
                            pattern.build(ctx);
                            if (first == -1)
                                first = ctx.first;
                        }
                    }
                    if (tmax < 0) {
                        pattern.build(ctx);
                        int secondLast = ctx.secondLast;
                        if (first == -1)
                            first = ctx.first;
                        pattern.build(ctx);
                        ctx.model.addLine(more, ctx.secondLast, ctx.con, ctx.first);
                        ctx.model.addLine(!more, secondLast, ctx.con, ctx.last);
                    } else {
                        List<Integer> secondLasts = new ArrayList<>();
                        pattern.build(ctx);
                        secondLasts.add(ctx.secondLast);
                        if (first == -1)
                            first = ctx.first;
                        Object con = ctx.con;
                        for (int i = 1; i < tmax; i++) {
                            pattern.build(ctx);
                            secondLasts.add(ctx.secondLast);
                        }
                        int last = ctx.last;
                        for (int i = 0, len = secondLasts.size() - 1; i < len; i++) {
                            int state = secondLasts.get(i);
                            ctx.model.addLine(!more, state, con, last);
                        }
                    }
                    ctx.first = first;
                    if (tmin == 0) {
                        int state = ++ctx.state;
                        ctx.model.addLine(!more, first, null, state);
                        ctx.model.addLine(!more, state, null, ctx.last);
                        // ctx.model.addAction(state, Action.Back);
                        // ctx.model.addAction(state, Action.Back);
                    }
                }

                @Override
                public String toString() {
                    return pattern + "{" + min + "," + (max == -1 ? "" : max) + "}"
                            + (more ? "" : "?");
                }
            };
        }
    }
    public static class Range {
        public int min, max;

        public Range(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return (char) min + "-" + (char) max;
        }
    }
    public static class CharacterSet extends CharFilter {
        private MyStringBuilder chars = new MyStringBuilder();
        private List<Range> ranges = new ArrayList<>();
        public boolean[] arr;
        public Set<Integer> set;
        public List<CharFilter> cfs;

        @Override
        public boolean accept(int ch) {
            return ch > 0 && ch < 128 && arr != null && arr[ch] || set != null && set.contains(ch)
                    || cfs != null && accepts(cfs, ch);
        }

        private static boolean accepts(List<CharFilter> cfs, int ch) {
            for (int i = 0, len = cfs.size(); i < len; i++)
                if (cfs.get(i).accept(ch))
                    return true;
            return false;
        }

        private boolean[] getArr() {
            if (arr == null)
                arr = new boolean[128];
            return arr;
        }

        private Set<Integer> getSet() {
            if (set == null)
                set = new HashSet<>();
            return set;
        }

        private List<CharFilter> getCfs() {
            if (cfs == null)
                cfs = new ArrayList<>();
            return cfs;
        }

        public void put(int ch) {
            if (ch >= 0 && ch < 128) {
                getArr()[ch] = true;
            } else {
                getSet().add(ch);
            }
            chars.append((char) ch);
        }

        public void put(String chars) {
            for (int i = 0; i < chars.length(); i++)
                put(chars.charAt(i));
        }

        public void put(int min, int max) {
            if (min > max)
                throw new IllegalArgumentException();
            if (min >= 0 && max < 128) {
                for (int i = min; i < max; i++) {
                    getArr()[i] = true;
                    chars.append((char) i);
                }
            } else {
                getCfs().add(CharFilter.range(min, max));
                ranges.add(new Range(min, max));
            }
        }

        private int hashCode = -1;

        @Override
        public int hashCode() {
            if (hashCode == -1)
                hashCode = Arrays.hashCode(arr) ^ (set == null ? 0 : set.hashCode())
                        ^ (cfs == null ? 0 : cfs.hashCode());
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            CharacterSet cs;
            return this == obj || obj instanceof CharacterSet
                    && Arrays.equals(arr, (cs = (CharacterSet) obj).arr)
                    && (set == cs.set || set != null && set.equals(cs.set))
                    && (cfs == cs.cfs || cfs != null && cfs.equals(cs.cfs));
        }

        @Override
        public String toString() {
            return new MyStringBuilder().append('[').append(chars).appends(ranges, "").append(']')
                    .toString();
        }
    }

    static class Regex {
        static enum Key {
            CHAR, EOF, LSB, RSB, LMB, RMB, LBB, RBB, ADD, SUB, COMMA, OR, XOR, QUE, DOT, ASTERISK, CDATA, END, NOT
        }

        static Model CharModel = new Model();
        static {
            CharModel.addLine(0, '\\', 1);
            CharModel.addLine(0, CharFilter.Any, 2);
            CharModel.addLine(1, 'u', 3);
            CharModel.addLine(1, CharFilter.OctalNumber, 4);
            CharModel.addLine(1, CharFilter.OctalNumber, 2);
            CharModel.addLine(1, CharFilter.Any, 2);
            CharModel.addLine(3, CharFilter.HexNumber, 4);
            CharModel.addLine(4, CharFilter.HexNumber, 5);
            CharModel.addLine(5, CharFilter.HexNumber, 6);
            CharModel.addLine(6, CharFilter.HexNumber, 2);
            CharModel.addLine(4, CharFilter.OctalNumber, 7);
            CharModel.addLine(4, CharFilter.OctalNumber, 2);
            CharModel.addLine(7, CharFilter.OctalNumber, 2);

            CharModel.addAction(2, Action.finish(Key.CHAR));
        }
        static Model CDATAModel = new Model();// <![CDATA[常量字符串，无需转义]]>
        static {
            CDATAModel.addLine(0, '<', 1);
            CDATAModel.addLine(1, '!', 2);
            CDATAModel.addLine(2, '[', 3);
            CDATAModel.addLine(3, 'C', 4);
            CDATAModel.addLine(4, 'D', 5);
            CDATAModel.addLine(5, 'A', 6);
            CDATAModel.addLine(6, 'T', 7);
            CDATAModel.addLine(7, 'A', 8);
            CDATAModel.addLine(8, '[', 9);
            CDATAModel.addLine(9, ']', 10);
            CDATAModel.addLine(9, CharFilter.Any, 9);
            CDATAModel.addLine(10, ']', 11);
            CDATAModel.addLine(10, CharFilter.Any, 9);
            CDATAModel.addLine(11, '>', 12);
            CDATAModel.addLine(11, CharFilter.Any, 9);

            CDATAModel.addAction(12, Action.finish(Key.CDATA));
        }
        static Analyzer<Key> RegexAnalyzer = new Analyzer<>();

        static {
            RegexAnalyzer.setModel(CharModel);
            RegexAnalyzer.setModel(CDATAModel);
            RegexAnalyzer.setEOF(Key.EOF);
            RegexAnalyzer.setSymbol("(", Key.LSB);
            RegexAnalyzer.setSymbol(")", Key.RSB);
            RegexAnalyzer.setSymbol("[", Key.LMB);
            RegexAnalyzer.setSymbol("]", Key.RMB);
            RegexAnalyzer.setSymbol("{", Key.LBB);
            RegexAnalyzer.setSymbol("}", Key.RBB);
            RegexAnalyzer.setSymbol("+", Key.ADD);
            RegexAnalyzer.setSymbol("-", Key.SUB);
            RegexAnalyzer.setSymbol(",", Key.COMMA);
            RegexAnalyzer.setSymbol("|", Key.OR);// TODO
            RegexAnalyzer.setSymbol("^", Key.XOR);// TODO
            RegexAnalyzer.setSymbol("?", Key.QUE);
            RegexAnalyzer.setSymbol(".", Key.DOT);
            RegexAnalyzer.setSymbol("*", Key.ASTERISK);
            RegexAnalyzer.setSymbol("$", Key.END);
            RegexAnalyzer.setSymbol("!", Key.NOT);// TODO
        }

        public static Pattern compile(CharSequence regex) {
            Source<Key> src = RegexAnalyzer.analyze(regex);
            List<Object> patterns = new ArrayList<>();
            boolean lastRange = false;
            int min = -1, max = -1;
            boolean more = true;
            L0: for (Key key = src.next();;) {
                switch (key) {
                    case CHAR:
                    case EOF:
                    case LMB:
                    case DOT:
                    case CDATA:
                        if (lastRange) {
                            int index = patterns.size() - 1;
                            patterns.set(index,
                                    Pattern.repeat((Pattern) patterns.get(index), min, max, more));
                            lastRange = false;
                            min = max = -1;
                            more = true;
                        }
                        break;
                    case LBB:
                    case ADD:
                    case ASTERISK:
                        lastRange = true;
                        break;
                    default:
                        break;
                }
                switch (key) {
                    case CHAR:
                        patterns.add(Pattern.single(unEscape(src.getValue())));
                        break;
                    case EOF:
                        break L0;
                    case LMB:
                        CharacterSet set = null;
                        CharacterSet notSet = null;
                        boolean lastSub = true;
                        int last = -1;
                        boolean isNotSet = false;
                        L1: while (true) {
                            key = src.next();
                            switch (key) {
                                case RMB:
                                    break L1;
                                case XOR:
                                    if (notSet == null)
                                        notSet = new CharacterSet();
                                    isNotSet = true;
                                    break;
                                case CHAR:
                                    if (!isNotSet && set == null) {
                                        set = new CharacterSet();
                                    }
                                    char ch = unEscape(src.getValue());
                                    if (last == -1)
                                        last = ch;
                                    else {
                                        CharacterSet s = isNotSet ? notSet : set;
                                        if (lastSub) {
                                            s.put(last, ch);
                                            last = -1;
                                        } else {
                                            s.put(last);
                                            last = ch;
                                        }
                                        isNotSet = false;
                                    }
                                    lastSub = false;
                                    break;
                                case END:
                                    (isNotSet ? notSet : set).put(-1);
                                    isNotSet = false;
                                    break;
                                case SUB:
                                    if (lastSub)
                                        throw new IllegalArgumentException();
                                    lastSub = true;
                                    break;
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                        if (last != -1)
                            (isNotSet ? notSet : set).put(last);
                        CharFilter cf;
                        if (set == null) {
                            if (notSet == null) {
                                throw new IllegalArgumentException();
                            } else {
                                cf = CharFilter.not(notSet);
                            }
                        } else {
                            if (notSet == null) {
                                cf = set;
                            } else {
                                cf = CharFilter.or(set, CharFilter.not(notSet));
                            }
                        }
                        patterns.add(Pattern.single(cf));
                        break;
                    case DOT:
                        patterns.add(Pattern.single(CharFilter.Any));
                        break;
                    case END:
                        patterns.add(Pattern.single(-1));
                        break;
                    case CDATA:
                        CharSequence value = src.getValue();
                        List<Pattern> list = new ArrayList<>();
                        for (int i = "<![CDATA[".length(), len =
                                value.length() - "]]>".length(); i < len; i++)
                            list.add(Pattern.single(value.charAt(i)));
                        patterns.add(Pattern.link(list.toArray()));
                        break;
                    case LBB:
                        MyStringBuilder sb = new MyStringBuilder();
                        L2: while (true) {
                            key = src.next();
                            switch (key) {
                                case RBB:
                                    break L2;
                                case CHAR:
                                    sb.append(src.getValue());
                                    break;
                                case COMMA:
                                    min = Numbers.parseInt(sb, null, 10, -1, -1);
                                    sb.clear();
                                    break;
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                        max = Numbers.parseInt(sb, null, 10, -1, -1);
                        break;
                    case QUE:
                        if (lastRange)
                            more = false;
                        else {
                            min = 0;
                            max = 1;
                            lastRange = true;
                        }
                        break;
                    case ADD:
                        min = 1;
                        break;
                    case ASTERISK:
                        min = 0;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                key = src.next();
            }
            return Pattern.link(patterns.toArray());
        }

        private static char unEscape(CharSequence cs) {
            char c = cs.charAt(0);
            if (c == '\\') {
                c = cs.charAt(1);
                switch (c) {
                    case 'r':
                        return '\r';
                    case 'n':
                        return '\n';
                    case 'f':
                        return '\f';
                    case 't':
                        return '\t';
                    case 'b':
                        return '\b';
                    case 'u':
                        return (char) Numbers.parseInt(cs, null, 16, 1, -1);
                    default:
                        if (CharFilter.OctalNumber.accept(c))
                            return (char) Numbers.parseInt(cs, null, 8, 1, -1);
                }
            }
            return c;
        }
    }
}
