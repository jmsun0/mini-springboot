package com.sjm.core.util.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public abstract class Lex {
    public abstract int read();

    public interface Action<L extends Lex> {
        public void process(L lex);
    }

    interface DFATransfer {
        public DFAState transfer(int input, Lex lex);
    }

    public static class DFAState {
        protected DFATransfer transfer;

        public DFAState next(Lex lex) {
            return transfer == null ? null : transfer.transfer(lex.read(), lex);
        }
    }

    static enum ConditionType {
        TRUE, EQ, RANGE, NOT, OR
    }

    static class Condition {
        public ConditionType type;
        public int ch;// for EQ
        public int min, max;// for RANGE
        public Condition sub;// for NOT
        public Condition[] subs;// for OR

        private Condition(ConditionType type, int ch, int min, int max, Condition sub,
                Condition[] subs) {
            this.type = type;
            this.ch = ch;
            this.min = min;
            this.max = max;
            this.sub = sub;
            this.subs = subs;
        }

        public static final Condition TRUE = new Condition(ConditionType.TRUE, 0, 0, 0, null, null);

        public static Condition eq(int ch) {
            return new Condition(ConditionType.EQ, ch, 0, 0, null, null);
        }

        public static Condition range(int min, int max) {
            return new Condition(ConditionType.RANGE, 0, min, max, null, null);
        }

        public static Condition not(Condition sub) {
            return new Condition(ConditionType.NOT, 0, 0, 0, sub, null);
        }

        public static Condition or(Condition[] subs) {
            return new Condition(ConditionType.OR, 0, 0, 0, null, subs);
        }
    }

    static class OriginDFATransfer {
        public Condition condition;
        public Object to;
        public Action<Lex> action;

        public OriginDFATransfer(Condition condition, Object to, Action<Lex> action) {
            this.condition = condition;
            this.to = to;
            this.action = action;
        }
    }

    public static class DFABuilder<L extends Lex> {
        private Object start;
        private Map<Object, List<OriginDFATransfer>> transfers = new HashMap<>();
        private int optimizeThreshold = 3;

        public DFABuilder<L> setStart(Object start) {
            this.start = start;
            return this;
        }

        @SuppressWarnings("unchecked")
        public DFABuilder<L> addTransfer(Object from, Object input, Object to, Action<L> action) {
            List<OriginDFATransfer> list = transfers.get(from);
            if (list == null)
                transfers.put(from, list = new ArrayList<>());
            list.add(new OriginDFATransfer(parseCondition(input), to, (Action<Lex>) action));
            return this;
        }

        public DFABuilder<L> addTransfer(Object from, Object input, Object to) {
            return addTransfer(from, input, to, null);
        }

        public DFABuilder<L> setOptimizeThreshold(int optimizeThreshold) {
            this.optimizeThreshold = optimizeThreshold;
            return this;
        }

        public void enableToStringSupport() {
            stateFactory = ToStringSupport.DFAStateForToString::new;
            transferFactory = new ToStringSupport.DFATransferFactoryForToString();
        }

        private Function<Object, DFAState> stateFactory = id -> new DFAState();
        private DFATransferFactory transferFactory = new DFATransferFactory();
        private Map<Object, DFAState> states = new HashMap<>();
        private DFATransferOptimizer optimizer = new DFATransferOptimizer();

        public DFAState build() {
            for (Map.Entry<Object, List<OriginDFATransfer>> e : transfers.entrySet()) {
                if (!states.containsKey(e.getKey()))
                    states.put(e.getKey(), stateFactory.apply(e.getKey()));
                for (OriginDFATransfer transfer : e.getValue()) {
                    if (!states.containsKey(transfer.to))
                        states.put(transfer.to, stateFactory.apply(transfer.to));
                }
            }
            for (Map.Entry<Object, List<OriginDFATransfer>> e : transfers.entrySet()) {
                optimizer.optimize(e.getValue(), optimizeThreshold);
                List<DFATransfer> list = new ArrayList<>();
                OriginDFATransfer[] map = optimizer.mapResult;
                if (map != null) {
                    Map<OriginDFATransfer, DFATransfer> transferCache = new HashMap<>();
                    DFATransfer[] mapTransfers = new DFATransfer[map.length];
                    for (int i = 0; i < mapTransfers.length; i++) {
                        OriginDFATransfer transfer = map[i];
                        if (transfer != null) {
                            DFATransfer tran = transferCache.get(transfer);
                            if (tran == null) {
                                tran = transferFactory.withAction(
                                        transferFactory._true(states.get(transfer.to)),
                                        transfer.action);
                                transferCache.put(transfer, tran);
                            }
                            mapTransfers[i] = tran;
                        }
                    }
                    list.add(transferFactory.map(mapTransfers));
                }
                for (OriginDFATransfer transfer : optimizer.listResult) {
                    list.add(transferFactory.withAction(
                            toDFATransfer(transfer.condition, transfer.to), transfer.action));
                }
                states.get(e.getKey()).transfer = list.size() == 1 ? list.get(0)
                        : transferFactory.or(list.toArray(new DFATransfer[list.size()]));
            }
            return states.get(start);
        }

        private DFATransfer toDFATransfer(Condition condition, Object to) {
            switch (condition.type) {
                case TRUE:
                    return transferFactory._true(states.get(to));
                case EQ:
                    return transferFactory.equals(states.get(to), condition.ch);
                case RANGE:
                    return transferFactory.range(states.get(to), condition.min, condition.max);
                case NOT:
                    return transferFactory.not(states.get(to), toDFATransfer(condition.sub, to));
                case OR:
                    Condition[] subs = condition.subs;
                    DFATransfer[] tranfers = new DFATransfer[subs.length];
                    for (int i = 0; i < tranfers.length; i++)
                        tranfers[i] = toDFATransfer(subs[i], to);
                    return transferFactory.or(tranfers);
                default:
                    throw new RuntimeException();
            }
        }

        private static Condition parseCondition(Object input) {
            if (input == null) {
                return Condition.TRUE;
            } else if (input instanceof Condition) {
                return (Condition) input;
            } else if (input instanceof Character || input instanceof Integer) {
                return Condition.eq(input instanceof Character ? (int) (char) input : (int) input);
            } else if (input instanceof String) {
                return ConditionParser.parse((String) input);
            } else
                throw new UnsupportedOperationException();
        }
    }

    public static class DFAGenerator<L extends Lex> {
        private DFABuilder<L> builder = new DFABuilder<>();

        public void setEOF(Action<L> action) {

        }

        public void setText(Action<L> action) {

        }

        public void setString(Action<L> action) {

        }
    }

    static class DFATransferFactory {
        public DFATransfer _true(DFAState toState) {
            return (input, lex) -> toState;
        }

        public DFATransfer equals(DFAState toState, int ch) {
            return (input, lex) -> input == ch ? toState : null;
        }

        public DFATransfer range(DFAState toState, int min, int max) {
            return (input, lex) -> input >= min && input <= max ? toState : null;
        }

        public DFATransfer not(DFAState toState, DFATransfer transfer) {
            return (input, lex) -> transfer.transfer(input, lex) == null ? toState : null;
        }

        public DFATransfer map(DFATransfer[] map) {
            return (input, lex) -> input >= 0 && input < map.length && map[input] != null
                    ? map[input].transfer(input, lex)
                    : null;
        }

        public DFATransfer or(DFATransfer[] transfers) {
            return (input, lex) -> {
                for (DFATransfer transfer : transfers) {
                    DFAState next = transfer.transfer(input, lex);
                    if (next != null)
                        return next;
                }
                return null;
            };
        }

        public DFATransfer withAction(DFATransfer transfer, Action<Lex> action) {
            if (action == null)
                return transfer;
            return (input, lex) -> {
                DFAState next = transfer.transfer(input, lex);
                if (next != null)
                    action.process(lex);
                return next;
            };
        }
    }

    static class ConditionParser {
        private String exp;
        private int index;
        private int current;

        public static Condition parse(String exp) {
            ConditionParser parser = new ConditionParser();
            parser.exp = exp;
            parser.next();
            return parser.matchGroup(false);
        }

        private void next() {
            current = index < exp.length() ? exp.charAt(index++) : -1;
        }

        private Condition matchCondition() {
            switch (current) {
                case '^':
                    next();
                    return Condition.not(matchCondition());
                case '(':
                    next();
                    return matchGroup(true);
                default:
                    return matchSingle();
            }
        }

        private Condition matchGroup(boolean isInternal) {
            Condition condition = null;
            List<Condition> conditions = null;
            while (true) {
                Condition tmp = matchCondition();
                if (condition == null)
                    condition = tmp;
                else {
                    if (conditions == null) {
                        conditions = new ArrayList<>();
                        conditions.add(condition);
                    }
                    conditions.add(tmp);
                }
                if (isInternal) {
                    if (current == ')') {
                        next();
                        break;
                    }
                    if (current == -1)
                        throw new RuntimeException();
                } else {
                    if (current == -1)
                        break;
                }
            }
            if (conditions != null) {
                return Condition.or(conditions.toArray(new Condition[conditions.size()]));
            } else {
                return condition;
            }
        }

        private void nextUnescapeChar() {
            if (current == '\\')
                next();
            if (current == -1)
                throw new RuntimeException();
        }

        private Condition matchSingle() {
            if (current == '$') {
                next();
                return Condition.eq(-1);
            }
            int min, max;
            nextUnescapeChar();
            min = current;
            next();
            if (current == '-') {
                next();
                nextUnescapeChar();
                max = current;
                next();
                return Condition.range(min, max);
            } else {
                return Condition.eq(min);
            }
        }
    }

    static class BufferPool<T> {
        private Queue<T> buffers = new LinkedList<>();
        private Supplier<T> allocator;
        private Consumer<T> cleaner;

        public BufferPool(Supplier<T> allocator, Consumer<T> cleaner) {
            this.allocator = allocator;
            this.cleaner = cleaner;
        }

        public T apply() {
            T buffer = buffers.poll();
            if (buffer == null)
                buffer = allocator.get();
            return buffer;
        }

        public void recycle(T buffer) {
            cleaner.accept(buffer);
            buffers.offer(buffer);
        }
    }

    static class DFATransferOptimizer {
        public OriginDFATransfer[] mapResult;
        public List<OriginDFATransfer> listResult;

        private BufferPool<boolean[]> booleanArrayPool =
                new BufferPool<boolean[]>(() -> new boolean[128], b -> Arrays.fill(b, false));
        private BufferPool<List<Condition>> conditionListPool =
                new BufferPool<List<Condition>>(() -> new ArrayList<>(), list -> list.clear());

        public void optimize(List<OriginDFATransfer> source, int optimizeThreshold) {
            if (optimizeThreshold <= 0) {
                listResult = source;
                mapResult = null;
            } else {
                int compareCount = 0;
                for (OriginDFATransfer transfer : source) {
                    compareCount += getSmallCompareCount(transfer.condition);
                }
                if (compareCount <= optimizeThreshold) {
                    listResult = source;
                    mapResult = null;
                } else {
                    OriginDFATransfer[] mapResult = this.mapResult = new OriginDFATransfer[128];
                    List<OriginDFATransfer> listResult = this.listResult = new ArrayList<>();
                    Map<OriginDFATransfer, OriginDFATransfer> transferCacheMap = new HashMap<>();
                    for (OriginDFATransfer transfer : source) {
                        boolean[] tmpMap = booleanArrayPool.apply();
                        List<Condition> tmpList = conditionListPool.apply();
                        optimizeCondition(transfer.condition, tmpMap, tmpList);
                        for (int i = 0; i < tmpMap.length; i++) {
                            if (mapResult[i] == null && tmpMap[i]) {
                                OriginDFATransfer newTransfer = transferCacheMap.get(transfer);
                                if (newTransfer == null)
                                    transferCacheMap.put(transfer,
                                            newTransfer = new OriginDFATransfer(null, transfer.to,
                                                    transfer.action));
                                mapResult[i] = newTransfer;
                            }
                        }
                        for (Condition condition : tmpList) {
                            listResult.add(
                                    new OriginDFATransfer(condition, transfer.to, transfer.action));
                        }
                        booleanArrayPool.recycle(tmpMap);
                        conditionListPool.recycle(tmpList);
                    }
                }
            }
        }

        private void optimizeCondition(Condition condition, boolean[] map, List<Condition> list) {
            switch (condition.type) {
                case TRUE:
                    Arrays.fill(map, true);
                    list.add(condition);
                    break;
                case EQ:
                    if (isSmallChar(condition.ch)) {
                        map[condition.ch] = true;
                    } else {
                        list.add(condition);
                    }
                    break;
                case RANGE:
                    if (isSmallChar(condition.min)) {
                        if (isSmallChar(condition.max)) {
                            for (int i = condition.min; i <= condition.max; i++)
                                map[i] = true;
                        } else {
                            for (int i = condition.min; i <= map.length; i++)
                                map[i] = true;
                            list.add(Condition.range(map.length, condition.max));
                        }
                    } else {
                        list.add(condition);
                    }
                    break;
                case NOT: {
                    boolean[] tmpMap = booleanArrayPool.apply();
                    List<Condition> tmpList = conditionListPool.apply();
                    optimizeCondition(condition.sub, tmpMap, tmpList);
                    for (int i = 0; i < map.length; i++) {
                        map[i] = !tmpMap[i];
                    }
                    if (tmpList.size() == 1) {
                        Condition first = tmpList.get(0);
                        if (first.type == ConditionType.NOT) {
                            list.add(first.sub);
                        } else {
                            list.add(Condition.not(first));
                        }
                    } else {
                        list.add(Condition
                                .not(Condition.or(tmpList.toArray(new Condition[tmpList.size()]))));
                    }
                    booleanArrayPool.recycle(tmpMap);
                    conditionListPool.recycle(tmpList);
                    break;
                }
                case OR: {
                    for (Condition sub : condition.subs) {
                        boolean[] tmpMap = booleanArrayPool.apply();
                        List<Condition> tmpList = conditionListPool.apply();
                        optimizeCondition(sub, tmpMap, tmpList);
                        for (int i = 0; i < map.length; i++) {
                            map[i] = map[i] || tmpMap[i];
                        }
                        list.addAll(tmpList);
                        booleanArrayPool.recycle(tmpMap);
                        conditionListPool.recycle(tmpList);
                    }
                    break;
                }
            }
        }

        private static boolean isSmallChar(int ch) {
            return ch >= 0 && ch < 128;
        }

        private static int getSmallCompareCount(Condition condition) {
            int compareCount = 0;
            switch (condition.type) {
                case TRUE:
                    break;
                case EQ:
                    compareCount += isSmallChar(condition.ch) ? 1 : 0;
                    break;
                case RANGE:
                    compareCount += isSmallChar(condition.min) ? 1 : 0;
                    compareCount += isSmallChar(condition.max) ? 1 : 0;
                    break;
                case NOT:
                    compareCount += getSmallCompareCount(condition.sub);
                    break;
                case OR:
                    for (Condition sub : condition.subs)
                        compareCount += getSmallCompareCount(sub);
                    break;
            }
            return compareCount;
        }
    }

    static class ToStringSupport {
        interface ToString {
            public MyStringBuilder appendTo(MyStringBuilder sb, int blankCount);
        }

        static class DFAStateForToString extends DFAState {
            private Object id;

            public DFAStateForToString(Object id) {
                this.id = id;
            }

            @Override
            public String toString() {
                MyStringBuilder sb = new MyStringBuilder();
                sb.append(id).append("  -->  ").append('\n');
                if (transfer != null)
                    ((DFATransferForToString) transfer).toString.appendTo(sb, 4);
                return sb.toString();
            }
        }
        static class DFATransferForToString implements DFATransfer {
            private DFATransfer originTransfer;
            protected DFAState toState;
            private ToString toString;

            public DFATransferForToString(DFATransfer originTransfer, DFAState toState,
                    ToString toString) {
                this.originTransfer = originTransfer;
                this.toState = toState;
                this.toString = toString;
            }

            @Override
            public DFAState transfer(int input, Lex lex) {
                return originTransfer.transfer(input, lex);
            }

            @Override
            public String toString() {
                MyStringBuilder sb = new MyStringBuilder();
                toString.appendTo(sb, 0);
                return sb.toString();
            }

            public boolean hasAction() {
                return false;
            }
        }
        static class DFATransferFactoryForToString extends DFATransferFactory {
            @Override
            public DFATransfer _true(DFAState toState) {
                return new DFATransferForToString(super._true(toState), toState,
                        (sb, blankCount) -> appendBlank(sb, blankCount).append("true()  -->  ")
                                .append(((DFAStateForToString) toState).id));
            }

            @Override
            public DFATransfer equals(DFAState toState, int ch) {
                return new DFATransferForToString(super.equals(toState, ch), toState,
                        (sb, blankCount) -> appendBlank(sb, blankCount).append("equals(")
                                .appendEscape(ch < 0 ? '$' : (char) ch).append(")  -->  ")
                                .append(((DFAStateForToString) toState).id));
            }

            @Override
            public DFATransfer range(DFAState toState, int min, int max) {
                return new DFATransferForToString(super.range(toState, min, max), toState,
                        (sb, blankCount) -> appendBlank(sb, blankCount).append("range(")
                                .appendEscape((char) min).append(",").appendEscape((char) max)
                                .append(")  -->  ").append(((DFAStateForToString) toState).id));
            }

            @Override
            public DFATransfer not(DFAState toState, DFATransfer transfer) {
                return new DFATransferForToString(super.not(toState, transfer), toState,
                        (sb, blankCount) -> appendChilds(sb, blankCount, "not", transfer));
            }

            @Override
            public DFATransfer map(DFATransfer[] map) {
                return new DFATransferForToString(super.map(map), null, (sb, blankCount) -> {
                    Map<DFATransfer, MyStringBuilder> transferMap = new HashMap<>();
                    for (int i = 0; i < map.length; i++) {
                        DFATransfer transfer = map[i];
                        if (transfer == null)
                            continue;
                        MyStringBuilder chars = transferMap.get(transfer);
                        if (chars == null)
                            transferMap.put(transfer, chars = new MyStringBuilder());
                        chars.appendEscape((char) i);
                    }
                    appendBlank(sb, blankCount).append("map").append("(");
                    for (Map.Entry<DFATransfer, MyStringBuilder> e : transferMap.entrySet()) {
                        DFATransferForToString state = (DFATransferForToString) e.getKey();
                        appendBlank(sb.append('\n'), blankCount + 4).append(e.getValue())
                                .append("  -->  ").append(((DFAStateForToString) state.toState).id);
                        if (state.hasAction())
                            sb.append(" (action)");
                    }
                    return appendBlank(sb.append('\n'), blankCount).append(")");
                });
            }

            @Override
            public DFATransfer or(DFATransfer[] transfers) {
                return new DFATransferForToString(super.or(transfers), null,
                        (sb, blankCount) -> appendChilds(sb, blankCount, "or", transfers));
            }

            @Override
            public DFATransfer withAction(DFATransfer transfer, Action<Lex> action) {
                if (action == null)
                    return transfer;
                return new DFATransferForToString(super.withAction(transfer, action),
                        ((DFATransferForToString) transfer).toState,
                        (sb, blankCount) -> ((DFATransferForToString) transfer).toString
                                .appendTo(sb, blankCount).append(" (action)")) {
                    @Override
                    public boolean hasAction() {
                        return true;
                    }
                };
            }
        }

        private static MyStringBuilder appendBlank(MyStringBuilder sb, int count) {
            for (int i = 0; i < count; i++)
                sb.append(' ');
            return sb;
        }

        private static MyStringBuilder appendChilds(MyStringBuilder sb, int blankCount, String name,
                ToString... childs) {
            appendBlank(sb, blankCount).append(name).append("(");
            for (ToString child : childs) {
                sb.append('\n');
                child.appendTo(sb, blankCount + 4);
            }
            appendBlank(sb.append('\n'), blankCount).append(")");
            return sb;
        }

        private static MyStringBuilder appendChilds(MyStringBuilder sb, int blankCount, String name,
                DFATransfer... transfers) {
            ToString[] childs = new ToString[transfers.length];
            for (int i = 0; i < childs.length; i++)
                childs[i] = ((DFATransferForToString) transfers[i]).toString;
            return appendChilds(sb, blankCount, name, childs);
        }
    }

    static class ExampleLex extends Lex {
        private String str;
        private int index;
        private boolean running;

        public ExampleLex(String str) {
            this.str = str;
        }

        @Override
        public int read() {
            int ch = index < str.length() ? str.charAt(index) : -1;
            index++;
            return ch;
        }

        public void throwError() {
            String left = str.substring(Math.max(0, index - 10), index);
            String right = str.substring(index, Math.min(str.length(), index + 10));
            throw new RuntimeException(left + "<<<" + right);
        }

        public void next() {
            DFAState state = START;
            running = true;
            while (running) {
                state = state.next(this);
                if (state == null)
                    throwError();
            }
        }

        private void finish() {
            running = false;
        }

        private static final DFAState START;
        static {
            DFABuilder<ExampleLex> builder = new DFABuilder<>();
            builder.setStart(0);
            builder.addTransfer(0, "a-zA-Z", 1, ExampleLex::finish);
            builder.addTransfer(0, "0-9", 2);

            builder.enableToStringSupport();
            START = builder.build();
            System.out.println(START);
        }
    }

    public static void main(String[] args) {
        ExampleLex lex = new ExampleLex("hello");
        lex.next();
        System.out.println(lex.index);
    }
}
