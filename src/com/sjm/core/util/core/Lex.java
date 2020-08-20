package com.sjm.core.util.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Supplier;


public abstract class Lex {
    protected abstract int read();

    public interface Action<L extends Lex> {
        public void process(L lex);
    }

    public static class State implements Comparable<State> {
        public final int id;
        public Action<Lex>[] actions;

        public State(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }

        @Override
        public int compareTo(State o) {
            return id - o.id;
        }

        @Override
        public boolean equals(Object obj) {
            State that = (State) obj;
            return this == that || id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        public void runAction(Lex lex) {
            if (actions != null)
                for (Action<Lex> action : actions)
                    action.process(lex);
        }
    }

    public static class NFAState extends State {
        public IntFunction<NFAState[]> transfer;
        public NFAState[] epsilon;

        public NFAState(int id) {
            super(id);
        }
    }

    public static class DFAState extends State {
        public IntFunction<DFAState> transfer;

        public DFAState(int id) {
            super(id);
        }
    }

    static abstract class FADeclaration {
        protected abstract void addTransfer(int from, int conditionId, int to);

        public abstract void merge();

        public ConditionCache conditionCache;

        protected FADeclaration(ConditionCache conditionCache) {
            this.conditionCache = conditionCache;
        }

        public void addTransfer(int from, Condition condition, int to) {
            int conditionId = condition == null ? -1 : conditionCache.getIdByCondition(condition);
            addTransfer(from, conditionId, to);
        }
    }

    static class DFADeclaration extends FADeclaration {
        public IntArrayMap<IntArrayMap<Integer>> transfers;

        public DFADeclaration(ConditionCache conditionCache,
                IntArrayMap<IntArrayMap<Integer>> transfers) {
            super(conditionCache);
            this.transfers = transfers;
        }

        public DFADeclaration() {
            this(new ConditionCache(), new IntArrayMap<>());
        }

        @Override
        public void addTransfer(int from, int conditionId, int to) {
            IntArrayMap<Integer> map = transfers.get(from);
            if (map == null)
                transfers.put(from, map = new IntArrayMap<>());
            map.put(conditionId, to);
        }

        @Override
        public void merge() {
            ConditionMerger merger = new ConditionMerger();
            for (int i = 0; i < transfers.size(); i++)
                merger.merge(transfers.getValue(i), conditionCache,
                        (current, toMerge) -> mergeTarget(current, toMerge));
        }

        private static Integer mergeTarget(Integer current, Integer toMerge) {
            if (current == null) {
                return toMerge;
            } else {
                if (toMerge != null)
                    throw new IllegalArgumentException(
                            "one condition can only have one target for DFA");
                return current;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < transfers.size(); i++) {
                int from = transfers.getKey(i);
                IntArrayMap<Integer> map = transfers.getValue(i);
                for (int j = 0; j < map.size(); j++)
                    sb.append(from).append("-->")
                            .append(conditionCache.getConditionById(map.getKey(j))).append("-->")
                            .append(map.getValue(j)).append("\n");
            }
            return sb.toString();
        }
    }

    static class NFADeclaration extends FADeclaration {
        public IntArrayMap<IntArrayMap<IntArraySet>> transfers;
        public IntArrayMap<IntArraySet> epsilons;

        public NFADeclaration(ConditionCache conditionCache,
                IntArrayMap<IntArrayMap<IntArraySet>> transfers,
                IntArrayMap<IntArraySet> epsilons) {
            super(conditionCache);
            this.transfers = transfers;
            this.epsilons = epsilons;
        }

        public NFADeclaration() {
            this(new ConditionCache(), new IntArrayMap<>(), new IntArrayMap<>());
        }

        @Override
        public void addTransfer(int from, int conditionId, int to) {
            if (conditionId == -1) {
                IntArraySet set = epsilons.get(from);
                if (set == null)
                    epsilons.put(from, set = new IntArraySet());
                set.add(to);
            } else {
                IntArrayMap<IntArraySet> map = transfers.get(from);
                if (map == null)
                    transfers.put(from, map = new IntArrayMap<>());
                IntArraySet set = map.get(conditionId);
                if (set == null)
                    map.put(conditionId, set = new IntArraySet());
                set.add(to);
            }
        }

        @Override
        public void merge() {
            ConditionMerger merger = new ConditionMerger();
            Map<IntArraySet, IntArraySet> statesIntCache = new HashMap<>();
            IntArraySet tmpState = new IntArraySet();
            for (int i = 0; i < transfers.size(); i++)
                merger.merge(transfers.getValue(i), conditionCache, (current,
                        toMerge) -> mergeTarget(current, toMerge, statesIntCache, tmpState));
        }

        private static IntArraySet mergeTarget(IntArraySet current, IntArraySet toMerge,
                Map<IntArraySet, IntArraySet> statesIntCache, IntArraySet tmpState) {
            tmpState.clear();
            if (current != null)
                tmpState.addAll(current);
            if (toMerge != null)
                tmpState.addAll(toMerge);
            IntArraySet cachedStates = statesIntCache.get(tmpState);
            if (cachedStates == null) {
                cachedStates = new IntArraySet(tmpState);
                statesIntCache.put(cachedStates, cachedStates);
            }
            return cachedStates;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < transfers.size(); i++) {
                int from = transfers.getKey(i);
                IntArrayMap<IntArraySet> map = transfers.getValue(i);
                for (int j = 0; j < map.size(); j++)
                    sb.append(from).append("-->")
                            .append(conditionCache.getConditionById(map.getKey(j))).append("-->")
                            .append(map.getValue(j)).append("\n");
            }
            for (int i = 0; i < epsilons.size(); i++) {
                sb.append(epsilons.getKey(i)).append("-->null-->").append(epsilons.getValue(i))
                        .append("\n");
            }
            return sb.toString();
        }
    }

    // IT: int target ,T: real target,S: real state
    static abstract class FAStateBuilder<IT, T, S extends State, L extends Lex> {
        private final IntFunction<S> stateFactory;
        private final FADeclaration declaration;
        private final int asciiMapOptimizeThreshold;
        private final int arrayMapOptimizeThreshold;
        private final Map<String, Action<L>> actions;
        private final IntArrayMap<List<String>> actionBinding;
        private final IntArrayMap<S> states = new IntArrayMap<>();
        private final IntArrayMap<S> unfilledStates = new IntArrayMap<>();
        private final BufferPool<IntArrayMap<?>> mapPool =
                new BufferPool<>(IntArrayMap::new, IntArrayMap::clear);
        private final BufferPool<List<?>> listPool = new BufferPool<>(ArrayList::new, List::clear);

        protected FAStateBuilder(IntFunction<S> stateFactory, FADeclaration declaration,
                int asciiMapOptimizeThreshold, int arrayMapOptimizeThreshold,
                Map<String, Action<L>> actions, IntArrayMap<List<String>> actionBinding) {
            this.stateFactory = stateFactory;
            this.declaration = declaration;
            this.asciiMapOptimizeThreshold = asciiMapOptimizeThreshold;
            this.arrayMapOptimizeThreshold = arrayMapOptimizeThreshold;
            this.actions = actions;
            this.actionBinding = actionBinding;
        }

        protected abstract T getRealTarget(IT intTarget);

        protected abstract T[] newRealTargetArray(int len);

        protected abstract void fillState(S state);

        @SuppressWarnings("unchecked")
        protected IntFunction<T> toTransfer(IntArrayMap<IT> source) {
            if (source == null)
                return Transfers._false();
            List<Transfer<T>> transfers = listPool.apply();
            ConditionCache conditionCache = declaration.conditionCache;
            boolean isNewSource = false;
            if (asciiMapOptimizeThreshold > 0) {
                int asciiConditionCount = 0;
                for (int i = 0; i < source.size(); i++) {
                    Condition condition = conditionCache.getConditionById(source.getKey(i));
                    switch (condition.type) {
                        case EQ:
                            if (condition.min >= 0 && condition.min < 128)
                                asciiConditionCount++;
                            break;
                        case RANGE:
                            if (condition.min >= 0 && condition.min < 128
                                    || condition.max >= 0 && condition.max < 128)
                                asciiConditionCount++;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                if (asciiConditionCount >= asciiMapOptimizeThreshold) {
                    IntArrayMap<IT> newSource = mapPool.apply();
                    T[] asciiMap = newRealTargetArray(128);
                    for (int i = 0; i < source.size(); i++) {
                        int conditionId = source.getKey(i);
                        IT targetId = source.getValue(i);
                        Condition condition = conditionCache.getConditionById(conditionId);
                        T target = getRealTarget(targetId);
                        switch (condition.type) {
                            case EQ:
                                if (condition.min >= 0 && condition.min < 128)
                                    asciiMap[condition.min] = target;
                                else
                                    newSource.put(conditionId, targetId);
                                break;
                            case RANGE:
                                int min = condition.min, max = condition.max;
                                if (min >= 128 || max < 0) {
                                    newSource.put(conditionId, targetId);
                                } else {
                                    int from = Math.max(0, min), to = Math.min(128, max + 1);
                                    for (int j = from; j < to; j++)
                                        asciiMap[j] = target;
                                    if (min < 0 && max >= 0)
                                        newSource.put(conditionCache.rangeId(min, 0), targetId);
                                    if (max >= 128 && min < 128)
                                        newSource.put(conditionCache.rangeId(128, max), targetId);
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                    }
                    source = newSource;
                    isNewSource = true;
                    transfers.add(Transfers.asciiMap(asciiMap));
                }
            }
            if (arrayMapOptimizeThreshold > 0 && source.size() > arrayMapOptimizeThreshold) {
                IntArrayMap<MaxAndTarget<T>> arrayMap = new IntArrayMap<>();
                for (int i = 0; i < source.size(); i++) {
                    Condition condition = conditionCache.getConditionById(source.getKey(i));
                    T target = getRealTarget(source.getValue(i));
                    int from, to;
                    switch (condition.type) {
                        case EQ:
                            from = condition.min;
                            to = from + 1;
                            break;
                        case RANGE:
                            from = condition.min;
                            to = condition.max + 1;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    arrayMap.put(from, new MaxAndTarget<>(to, target));
                }
                transfers.add(Transfers.arrayMap(arrayMap));
            } else {
                for (int i = 0; i < source.size(); i++) {
                    Condition condition = conditionCache.getConditionById(source.getKey(i));
                    T target = getRealTarget(source.getValue(i));
                    transfers.add(Transfers.condition(target, condition));
                }
            }
            if (isNewSource)
                mapPool.recycle(source);
            if (transfers.isEmpty())
                return Transfers._false();
            if (transfers.size() == 1)
                return transfers.get(0);
            Transfer<T>[] arr = transfers.toArray(new Transfer[transfers.size()]);
            listPool.recycle(transfers);
            return Transfers.merge(arr);
        }

        protected S getState(int id) {
            S state = states.get(id);
            if (state == null) {
                state = unfilledStates.get(id);
                if (state == null)
                    unfilledStates.put(id, state = stateFactory.apply(id));
            }
            return state;
        }

        public S getOrBuildState(int id) {
            S result = getState(id);
            while (true) {
                int size = unfilledStates.size();
                if (size == 0)
                    break;
                int last = size - 1;
                S state = unfilledStates.getValue(last);
                unfilledStates.remove(last);
                states.put(state.id, state);
                state.actions = getActions(state.id);
                fillState(state);
            }
            return result;
        }

        private Map<List<String>, Action<Lex>[]> actionsCache = new HashMap<>();

        private Action<Lex>[] getActions(int state) {
            List<String> acNames = actionBinding.get(state);
            if (acNames == null)
                return null;
            Action<Lex>[] acs = actionsCache.get(acNames);
            if (acs == null)
                actionsCache.put(acNames, acs = getActions(acNames));
            return acs;
        }

        @SuppressWarnings("unchecked")
        private Action<Lex>[] getActions(List<String> acNames) {
            Action<Lex>[] acs = new Action[acNames.size()];
            for (int i = 0; i < acs.length; i++)
                acs[i] = (Action<Lex>) actions.get(acNames.get(i));
            return acs;
        }
    }

    static class NFAStateBuilder<L extends Lex>
            extends FAStateBuilder<IntArraySet, NFAState[], NFAState, L> {
        private NFADeclaration declaration;

        public NFAStateBuilder(NFADeclaration declaration, int asciiMapOptimizeThreshold,
                int arrayMapOptimizeThreshold, Map<String, Action<L>> actions,
                IntArrayMap<List<String>> actionBinding) {
            super(NFAState::new, declaration, asciiMapOptimizeThreshold, arrayMapOptimizeThreshold,
                    actions, actionBinding);
            this.declaration = declaration;
        }

        private Map<IntArraySet, NFAState[]> statesCache = new HashMap<>();

        @Override
        protected NFAState[] getRealTarget(IntArraySet set) {
            if (set == null || set.size() == 0)
                return null;
            NFAState[] states = statesCache.get(set);
            if (states == null) {
                states = new NFAState[set.size()];
                for (int i = 0; i < set.size(); i++)
                    states[i] = getState(set.get(i));
                statesCache.put(set, states);
            }
            return states;
        }

        @Override
        protected NFAState[][] newRealTargetArray(int len) {
            return new NFAState[len][];
        }

        @Override
        protected void fillState(NFAState state) {
            state.transfer = toTransfer(declaration.transfers.get(state.id));
            state.epsilon = getRealTarget(declaration.epsilons.get(state.id));
        }
    }

    static class DFAStateBuilder<L extends Lex>
            extends FAStateBuilder<Integer, DFAState, DFAState, L> {
        private DFADeclaration declaration;

        public DFAStateBuilder(DFADeclaration declaration, int asciiMapOptimizeThreshold,
                int arrayMapOptimizeThreshold, Map<String, Action<L>> actions,
                IntArrayMap<List<String>> actionBinding) {
            super(DFAState::new, declaration, asciiMapOptimizeThreshold, arrayMapOptimizeThreshold,
                    actions, actionBinding);
            this.declaration = declaration;
        }

        @Override
        protected DFAState getRealTarget(Integer id) {
            return getState(id);
        }

        @Override
        protected DFAState[] newRealTargetArray(int len) {
            return new DFAState[len];
        }

        @Override
        protected void fillState(DFAState state) {
            state.transfer = toTransfer(declaration.transfers.get(state.id));
        }
    }

    static class BasicBuilder<L extends Lex> {
        protected FADeclaration declaration;

        protected NFADeclaration nfaDeclaration;
        protected DFADeclaration dfaDeclaration;

        protected int asciiMapOptimizeThreshold = 3;
        protected int arrayMapOptimizeThreshold = 3;
        protected Map<String, Action<L>> actions = new HashMap<>();
        protected IntArrayMap<List<String>> actionBinding = new IntArrayMap<>();

        public void initNFA() {
            declaration = nfaDeclaration = new NFADeclaration();
        }

        public void initDFA() {
            declaration = dfaDeclaration = new DFADeclaration();
        }

        public void setAsciiMapOptimizeThreshold(int asciiMapOptimizeThreshold) {
            this.asciiMapOptimizeThreshold = asciiMapOptimizeThreshold;
        }

        public void setArrayMapOptimizeThreshold(int arrayMapOptimizeThreshold) {
            this.arrayMapOptimizeThreshold = arrayMapOptimizeThreshold;
        }

        public void setAction(String name, Action<L> action) {
            actions.put(name, action);
        }

        protected void addTransferInternal(int from, Condition condition, int to) {
            declaration.addTransfer(from, condition, to);
        }

        protected void bindActionInternal(int state, String name) {
            List<String> acNames = actionBinding.get(state);
            if (acNames == null)
                actionBinding.put(state, acNames = new ArrayList<>());
            acNames.add(name);
        }

        protected NFAState[] buildNFAInternal(int... starts) {
            if (declaration != nfaDeclaration)
                throw new IllegalArgumentException();
            nfaDeclaration.merge();
            NFAStateBuilder<L> nfaBuilder = new NFAStateBuilder<>(nfaDeclaration,
                    asciiMapOptimizeThreshold, arrayMapOptimizeThreshold, actions, actionBinding);
            return getStates(nfaBuilder, starts, new NFAState[starts.length]);
        }

        protected DFAState[] buildDFAInternal(int[] starts) {
            DFADeclaration dfaDeclaration = this.dfaDeclaration;
            IntArrayMap<List<String>> actionBinding = this.actionBinding;
            if (declaration == nfaDeclaration) {
                nfaDeclaration.merge();
                NFAToDFA converter = new NFAToDFA();
                FAIntContext<NFADeclaration> nfa =
                        new FAIntContext<>(nfaDeclaration, actionBinding, starts);
                FAIntContext<DFADeclaration> dfa = converter.convert(nfa);
                starts = dfa.starts;
                actionBinding = dfa.actionBinding;
                dfaDeclaration = dfa.declaration;
            } else
                dfaDeclaration.merge();
            DFAStateBuilder<L> dfaBuilder = new DFAStateBuilder<>(dfaDeclaration,
                    asciiMapOptimizeThreshold, arrayMapOptimizeThreshold, actions, actionBinding);
            return getStates(dfaBuilder, starts, new DFAState[starts.length]);
        }

        private <S extends State> S[] getStates(FAStateBuilder<?, ?, S, L> builder, int[] starts,
                S[] startStates) {
            for (int i = 0; i < starts.length; i++)
                startStates[i] = builder.getOrBuildState(starts[i]);
            return startStates;
        }
    }

    static class IDGenerator {
        public int id;

        public int get() {
            return id++;
        }
    }

    static class IDRegister<T> {
        public IDGenerator generator = new IDGenerator();
        public Map<T, Integer> idMap = new HashMap<>();
        public IntArrayMap<T> dataMap = new IntArrayMap<>();

        public int getOrRegisterId(T data) {
            Integer id = idMap.get(data);
            if (id == null) {
                idMap.put(data, id = generator.get());
                dataMap.put(id, data);
            }
            return id;
        }

        public T getData(int id) {
            return dataMap.get(id);
        }
    }

    public static class Builder<L extends Lex> extends BasicBuilder<L> {
        private IDRegister<Object> stateNameRegister = new IDRegister<>();
        private IDGenerator stateIdGenerator = stateNameRegister.generator;
        private Map<String, ActionTemplate<L>> templateMap = new HashMap<>();
        private TemplatedActionFactory<L> actionFactory =
                new TemplatedActionFactory<>(templateMap, actions);
        private RegexParser parser;

        public void addTransfer(Object from, Condition condition, Object to) {
            addTransferInternal(toId(from), condition, toId(to));
        }

        public void addPattern(Object from, Pattern pattern) {
            addPatternInternal(toId(from), pattern, stateIdGenerator.get());
        }

        public void bindAction(Object state, String name) {
            bindActionInternal(toId(state), name);
        }

        public NFAState[] buildNFA(Object... starts) {
            return buildNFAInternal(toIds(starts));
        }

        public NFAState buildNFA(Object start) {
            return buildNFA(new Object[] {start})[0];
        }

        public DFAState[] buildDFA(Object... starts) {
            return buildDFAInternal(toIds(starts));
        }

        public DFAState buildDFA(Object start) {
            return buildDFA(new Object[] {start})[0];
        }

        public void defineActionTemplate(String name, ActionTemplate<L> am) {
            templateMap.put(name, am);
        }

        public void defineVariable(String key, Object value) {
            getRegexParser().setVariable(key, value);
        }

        public void definePattern(String name, String regex) {
            getRegexParser().parseAndAddPattern(name, regex);
        }

        public void addPattern(Object from, String regex) {
            addPattern(from, getRegexParser().parsePattern(regex));
        }

        public void addTransfer(Object from, String conditionStr, Object to) {
            addTransfer(from, getRegexParser().parseCondition(conditionStr), to);
        }

        private RegexParser getRegexParser() {
            if (parser == null)
                parser = new RegexParser(actionFactory, declaration.conditionCache);
            return parser;
        }

        private int toId(Object state) {
            return stateNameRegister.getOrRegisterId(state);
        }

        private int[] toIds(Object[] states) {
            int[] ids = new int[states.length];
            for (int i = 0; i < ids.length; i++)
                ids[i] = toId(states[i]);
            return ids;
        }

        private void addPatternInternal(int from, Pattern pattern, int to) {
            switch (pattern.type) {
                case CONDITION:
                    addTransferInternal(from, pattern.condition, to);
                    break;
                case LINK: {
                    Pattern[] patterns = pattern.patterns;
                    if (patterns.length == 0) {
                        addTransferInternal(from, null, to);
                    } else {
                        int currentFrom = from, currentTo = stateIdGenerator.get();
                        for (int i = 0; i < patterns.length; i++) {
                            if (i != 0) {
                                currentFrom = stateIdGenerator.get();
                                addTransferInternal(currentTo, null, currentFrom);
                                if (i == patterns.length - 1)
                                    currentTo = to;
                                else
                                    currentTo = stateIdGenerator.get();
                            }
                            addPatternInternal(currentFrom, patterns[i], currentTo);
                        }
                    }
                    break;
                }
                case OR: {
                    Pattern[] patterns = pattern.patterns;
                    for (int i = 0; i < patterns.length; i++) {
                        int currentFrom = stateIdGenerator.get(),
                                currentTo = stateIdGenerator.get();
                        addPatternInternal(currentFrom, patterns[i], currentTo);
                        addTransferInternal(from, null, currentFrom);
                        addTransferInternal(currentTo, null, to);
                    }
                    break;
                }
                case REPEAT: {
                    int currentFrom = stateIdGenerator.get(), currentTo = stateIdGenerator.get();
                    addPatternInternal(currentFrom, pattern.pattern, currentTo);
                    addTransferInternal(from, null, currentFrom);
                    addTransferInternal(from, null, to);
                    addTransferInternal(currentTo, null, currentFrom);
                    addTransferInternal(currentTo, null, to);
                    break;
                }
                case ACTION:
                    addPatternInternal(from, pattern.pattern, to);
                    for (String action : pattern.actions)
                        bindActionInternal(to, action);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    static class FAIntContext<D extends FADeclaration> {
        public D declaration;
        public IntArrayMap<List<String>> actionBinding;
        public int[] starts;

        public FAIntContext(D declaration, IntArrayMap<List<String>> actionBinding, int[] starts) {
            this.declaration = declaration;
            this.actionBinding = actionBinding;
            this.starts = starts;
        }
    }

    static class NFAToDFA {
        private IDGenerator generator = new IDGenerator();
        private Map<IntArraySet, Integer> stateIds = new HashMap<>();
        private IntArrayMap<IntArraySet> unmarkedStates = new IntArrayMap<>();
        private IntArrayMap<IntArrayMap<Integer>> resultTransfers = new IntArrayMap<>();
        private IntArraySet conditions = new IntArraySet();
        private IntArraySet out = new IntArraySet();

        public FAIntContext<DFADeclaration> convert(FAIntContext<NFADeclaration> nfa) {
            convert(nfa.declaration.transfers, nfa.declaration.epsilons, nfa.starts);
            IntArrayMap<List<String>> newActionBinding = new IntArrayMap<>();
            IntArrayMap<List<String>> actionBinding = nfa.actionBinding;
            for (Map.Entry<IntArraySet, Integer> e : stateIds.entrySet()) {
                IntArraySet set = e.getKey();
                int id = e.getValue();
                for (int i = 0; i < set.size(); i++) {
                    List<String> acs = actionBinding.get(set.get(i));
                    if (acs != null) {
                        List<String> newAcs = newActionBinding.get(id);
                        if (newAcs == null)
                            newActionBinding.put(id, newAcs = new ArrayList<>());
                        newAcs.addAll(acs);
                    }
                }
            }
            int[] starts = new int[nfa.starts.length];
            for (int i = 0; i < starts.length; i++)
                starts[i] = i;
            return new FAIntContext<>(
                    new DFADeclaration(nfa.declaration.conditionCache, resultTransfers),
                    newActionBinding, starts);
        }

        private void reset() {
            generator.id = 0;
            stateIds.clear();;
            unmarkedStates.clear();
            resultTransfers.clear();
        }

        private void convert(IntArrayMap<IntArrayMap<IntArraySet>> transfers,
                IntArrayMap<IntArraySet> epsilons, int[] starts) {
            reset();
            convertInternal(transfers, epsilons, starts, generator, stateIds, unmarkedStates,
                    resultTransfers, conditions, out);
        }

        private static void convertInternal(IntArrayMap<IntArrayMap<IntArraySet>> transfers,
                IntArrayMap<IntArraySet> epsilons, int[] starts, IDGenerator generator,
                Map<IntArraySet, Integer> stateIds, IntArrayMap<IntArraySet> unmarkedStates,
                IntArrayMap<IntArrayMap<Integer>> resultTransfers, IntArraySet conditions,
                IntArraySet out) {
            for (int start : starts) {
                IntArraySet startSet = new IntArraySet();
                startSet.add(start);
                doEpsilon(epsilons, startSet);
                Integer startId = generator.get();
                stateIds.put(startSet, startId);
                unmarkedStates.put(startId, startSet);
            }
            while (true) {
                int size = unmarkedStates.size();
                if (size == 0)
                    break;
                int last = size - 1;
                int fromId = unmarkedStates.getKey(last);
                IntArraySet fromStates = unmarkedStates.getValue(last);
                unmarkedStates.remove(last);
                getConditions(transfers, fromStates, conditions);
                for (int i = 0, len = conditions.size(); i < len; i++) {
                    int condition = conditions.get(i);
                    doTransfer(transfers, fromStates, out, condition);
                    doEpsilon(epsilons, out);
                    Integer toId = stateIds.get(out);
                    if (toId == null) {
                        IntArraySet set = new IntArraySet(out);
                        stateIds.put(set, toId = generator.get());
                        unmarkedStates.put(toId, set);
                    }
                    IntArrayMap<Integer> map = resultTransfers.get(fromId);
                    if (map == null)
                        resultTransfers.put(fromId, map = new IntArrayMap<>());
                    map.put(condition, toId);
                }
            }
        }

        private static void getConditions(IntArrayMap<IntArrayMap<IntArraySet>> transfers,
                IntArraySet in, IntArraySet out) {
            out.clear();
            for (int i = 0, len = in.size(); i < len; i++) {
                IntArrayMap<IntArraySet> map = transfers.get(in.get(i));
                if (map != null)
                    for (int j = 0, mlen = map.size(); j < mlen; j++)
                        out.add(map.getKey(j));
            }
        }

        private static void doTransfer(IntArrayMap<IntArrayMap<IntArraySet>> transfers,
                IntArraySet in, IntArraySet out, int condition) {
            out.clear();
            for (int i = 0, len = in.size(); i < len; i++) {
                IntArrayMap<IntArraySet> map = transfers.get(in.get(i));
                if (map != null) {
                    IntArraySet states = map.get(condition);
                    if (states != null)
                        out.addAll(states);
                }
            }
        }

        private static void doEpsilon(IntArrayMap<IntArraySet> epsilons, IntArraySet out,
                int state) {
            IntArraySet nexts = epsilons.get(state);
            if (nexts != null)
                for (int i = 0, len = nexts.size(); i < len; i++) {
                    int next = nexts.get(i);
                    if (out.add(next))
                        doEpsilon(epsilons, out, next);
                }
        }

        private static void doEpsilon(IntArrayMap<IntArraySet> epsilons, IntArraySet inout) {
            for (int i = 0; i < inout.size(); i++)
                doEpsilon(epsilons, inout, inout.get(i));
        }
    }

    public interface FARunner<S> {
        public void run(Lex lex, S start);
    }

    public static class NFARunner implements FARunner<NFAState> {
        public static NFARunner getInstance() {
            return new NFARunner();
        }

        private ArraySet<NFAState> set1 = new ArraySet<>();
        private ArraySet<NFAState> set2 = new ArraySet<>();

        @Override
        public void run(Lex lex, NFAState start) {
            runNFA(lex, start, set1, set2);
        }

        private static void runNFA(Lex lex, NFAState start, ArraySet<NFAState> set1,
                ArraySet<NFAState> set2) {
            set1.clear();
            set1.add(start);
            doEpsilon(lex, set1);
            while (true) {
                doTransfer(lex, set1, set2);
                if (set2.size() == 0)
                    break;
                doTransfer(lex, set2, set1);
                if (set1.size() == 0)
                    break;
            }
        }

        private static void doTransfer(Lex lex, ArraySet<NFAState> in, ArraySet<NFAState> out) {
            out.clear();
            int input = lex.read();
            for (int i = 0, len = in.size(); i < len; i++)
                out.addAll(in.get(i).transfer.apply(input));
            if (out.size() != 0)
                doEpsilon(lex, out);
        }

        private static void doEpsilon(Lex lex, ArraySet<NFAState> inout) {
            for (int i = 0; i < inout.size(); i++)
                doEpsilon(lex, inout, inout.get(i));
            for (int i = 0, len = inout.size(); i < len; i++)
                inout.get(i).runAction(lex);
        }

        private static void doEpsilon(Lex lex, ArraySet<NFAState> set, NFAState state) {
            NFAState[] nexts = state.epsilon;
            if (nexts != null)
                for (NFAState next : nexts)
                    if (set.add(next))
                        doEpsilon(lex, set, next);
        }
    }

    public static class DFARunner implements FARunner<DFAState> {
        private static final DFARunner INSTANCE = new DFARunner();

        public static DFARunner getInstance() {
            return INSTANCE;
        }

        @Override
        public void run(Lex lex, DFAState start) {
            while (start != null) {
                start.runAction(lex);
                start = start.transfer.apply(lex.read());
            }
        }
    }

    static enum ConditionType {
        EQ, RANGE, NOT, OR
    }

    static class Condition implements IntPredicate {
        public ConditionType type;
        public int min;// for EQ | RANGE
        public int max;// for RANGE
        public Condition sub;// for NOT
        public Condition[] subs;// for OR

        private Condition(ConditionType type, int min, int max, Condition sub, Condition[] subs) {
            set(type, min, max, sub, subs);
        }

        public void set(ConditionType type, int min, int max, Condition sub, Condition[] subs) {
            this.type = type;
            this.min = min;
            this.max = max;
            this.sub = sub;
            this.subs = subs;
        }

        @Override
        public boolean test(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return appendTo(new MyStringBuilder()).toString();
        }

        public MyStringBuilder appendTo(MyStringBuilder sb) {
            switch (type) {
                case EQ:
                    return min == -1 ? sb.append('$') : sb.appendEscape((char) min);
                case RANGE:
                    return sb.appendEscape((char) min).append('-').appendEscape((char) max);
                case NOT:
                    return sb.append('^').append(sub, Condition::appendTo);
                case OR:
                    return sb.append('[').appends(subs, Condition::appendTo, "").append(']');
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean equals(Object obj) {
            Condition that = (Condition) obj;
            return this == that
                    || Objects.equals(type, that.type) && min == that.min && max == that.max
                            && Objects.equals(sub, that.sub) && Arrays.equals(subs, that.subs);
        }

        @Override
        public int hashCode() {
            return type.hashCode() ^ min ^ max ^ Objects.hashCode(sub) ^ Arrays.hashCode(subs);
        }

        public static Condition valueOf(ConditionType type, int min, int max, Condition sub,
                Condition[] subs) {
            switch (type) {
                case EQ:
                    return eq(min);
                case RANGE:
                    return range(min, max);
                case NOT:
                    return not(sub);
                case OR:
                    return or(subs);
                default:
                    return new Condition(type, min, max, sub, subs);
            }
        }

        public static Condition eq(int ch) {
            return new Condition(ConditionType.EQ, ch, 0, null, null) {
                @Override
                public boolean test(int value) {
                    return value == min;
                }
            };
        }

        public static Condition range(int min, int max) {
            return new Condition(ConditionType.RANGE, min, max, null, null) {
                @Override
                public boolean test(int value) {
                    return value >= min && value <= max;
                }
            };
        }

        public static Condition not(Condition sub) {
            return new Condition(ConditionType.NOT, 0, 0, sub, null) {
                @Override
                public boolean test(int value) {
                    return !sub.test(value);
                }
            };
        }

        public static Condition or(Condition... subs) {
            return new Condition(ConditionType.OR, 0, 0, null, subs) {
                @Override
                public boolean test(int value) {
                    for (Condition sub : subs)
                        if (sub.test(value))
                            return true;
                    return false;
                }
            };
        }
    }

    static class ConditionCache {
        private final Map<Condition, Integer> conditionIdMap = new HashMap<>();
        private final IntArrayMap<Condition> idConditionMap = new IntArrayMap<>();
        private final IDGenerator generator = new IDGenerator();
        private final Condition tmp = new Condition(null, 0, 0, null, null);

        public int getIdByCondition(ConditionType type, int min, int max, Condition sub,
                Condition[] subs) {
            tmp.set(type, min, max, sub, subs);
            Integer conditionId = conditionIdMap.get(tmp);
            if (conditionId == null) {
                Condition condition = Condition.valueOf(type, min, max, sub, subs);
                conditionId = generator.get();
                conditionIdMap.put(condition, conditionId);
                idConditionMap.put(conditionId, condition);
            }
            return conditionId;
        }

        public int getIdByCondition(Condition condition) {
            Integer conditionId = conditionIdMap.get(condition);
            if (conditionId == null) {
                conditionId = generator.get();
                conditionIdMap.put(condition, conditionId);
                idConditionMap.put(conditionId, condition);
            }
            return conditionId;
        }

        public Condition getConditionById(int conditionId) {
            return idConditionMap.get(conditionId);
        }

        public int eqId(int ch) {
            return getIdByCondition(ConditionType.EQ, ch, 0, null, null);
        }

        public int rangeId(int min, int max) {
            if (min == max)
                return eqId(min);
            return getIdByCondition(ConditionType.RANGE, min, max, null, null);
        }

        public int notId(Condition sub) {
            if (sub.type == ConditionType.NOT)
                return getIdByCondition(sub.sub);
            return getIdByCondition(ConditionType.NOT, 0, 0, sub, null);
        }

        public int orId(Condition... subs) {
            if (subs.length == 1)
                return getIdByCondition(subs[0]);
            return getIdByCondition(ConditionType.OR, 0, 0, null, subs);
        }

        public Condition eq(int ch) {
            return getConditionById(eqId(ch));
        }

        public Condition range(int min, int max) {
            return getConditionById(rangeId(min, max));
        }

        public Condition not(Condition sub) {
            return getConditionById(notId(sub));
        }

        public Condition or(Condition... subs) {
            return getConditionById(orId(subs));
        }

        public Condition remain(String chars) {
            Condition[] subs = new Condition[chars.length() + 1];
            for (int i = 0; i < chars.length(); i++)
                subs[i] = eq(chars.charAt(i));
            subs[chars.length()] = eq(-1);
            return not(or(subs));
        }

        public Condition or(String chars) {
            if (chars.length() == 1)
                return eq(chars.charAt(0));
            Condition[] subs = new Condition[chars.length()];
            for (int i = 0; i < subs.length; i++)
                subs[i] = eq(chars.charAt(i));
            return or(subs);
        }

        @Override
        public String toString() {
            return conditionIdMap.toString();
        }
    }

    static class MaxAndTarget<T> {
        public int max;
        public T target;

        public MaxAndTarget(int max, T target) {
            this.max = max;
            this.target = target;
        }
    }

    static enum TransferType {
        TRUE, CONDITION, ASCII_MAP, ARRAY_MAP, MERGE
    }

    static abstract class Transfer<T> implements IntFunction<T> {
        public final TransferType type;
        public final T dst;// for CONDITION
        public final Condition condition;// for CONDITION
        public final T[] asciiMap;// for ASCII_MAP
        public final IntArrayMap<MaxAndTarget<T>> arrayMap;// for ARRAY_MAP
        public final Transfer<T>[] transfers;// for MERGE

        public Transfer(TransferType type, T dst, Condition condition, T[] asciiMap,
                IntArrayMap<MaxAndTarget<T>> arrayMap, Transfer<T>[] transfers) {
            this.type = type;
            this.dst = dst;
            this.condition = condition;
            this.asciiMap = asciiMap;
            this.arrayMap = arrayMap;
            this.transfers = transfers;
        }

        private static MyStringBuilder appendBlank(MyStringBuilder sb, int count) {
            for (int i = 0; i < count; i++)
                sb.append(' ');
            return sb;
        }

        private static MyStringBuilder appendStates(MyStringBuilder sb, Object states) {
            if (states == null)
                return sb.appendNull();
            else if (states.getClass().isArray())
                return sb.append("[").appends(states, ",").append("]");
            else
                return sb.append(states);
        }

        public MyStringBuilder appendTo(MyStringBuilder sb, int blankCount) {
            appendBlank(sb, blankCount);
            switch (type) {
                case TRUE:
                    return appendStates(sb.append("true()  -->  "), dst);
                case CONDITION:
                    return appendStates(
                            sb.append("condition(").append(condition).append(")  -->  "), dst);
                case ASCII_MAP:
                    Map<T, MyStringBuilder> transferMap = new HashMap<>();
                    for (int i = 0; i < asciiMap.length; i++) {
                        T dst = asciiMap[i];
                        if (dst != null) {
                            MyStringBuilder chars = transferMap.get(dst);
                            if (chars == null)
                                transferMap.put(dst, chars = new MyStringBuilder());
                            chars.append((char) i);
                        }
                    }
                    for (MyStringBuilder chars : transferMap.values()) {
                        if (chars.length() == 1) {
                            replaceCharDetail(chars, 0);
                        } else {
                            for (int begin, end = 0; end < chars.length();) {
                                begin = end;
                                for (int i = begin; i < chars.length(); i++) {
                                    char ch = chars.charAt(i);
                                    if (!((ch >= 'a' && ch < 'z' || ch >= 'A' && ch < 'Z'
                                            || ch >= '0' && ch < '9' || ch >= 0 && ch < 31)
                                            && ch + 1 == chars.charAt(i + 1))) {
                                        end = i;
                                        break;
                                    }
                                }
                                int beforeLen = chars.length();
                                if (end - begin > 0) {
                                    chars.replace(begin + 1, end - 1, "-", -1, -1);
                                    replaceCharDetail(chars, begin + 2);
                                }
                                replaceCharDetail(chars, begin);
                                end += chars.length() - beforeLen + 1;
                            }
                        }
                    }
                    sb.append("ascii_map").append("(")
                            .appends(transferMap.entrySet(), (e, sbb) -> appendStates(
                                    appendBlank(sbb.append('\n'), blankCount + 4)
                                            .appendEscape(e.getValue(), -1, -1).append("  -->  "),
                                    e.getKey()), "")
                            .append('\n');
                    return appendBlank(sb, blankCount).append(")");
                case ARRAY_MAP:
                    sb.append("array_map").append("(");
                    for (int i = 0; i < arrayMap.size(); i++) {
                        appendBlank(sb.append('\n'), blankCount + 4);
                        int min = arrayMap.getKey(i);
                        appendCharDetail(sb, min);
                        MaxAndTarget<T> mat = arrayMap.getValue(i);
                        if (min + 1 != mat.max) {
                            sb.append('-');
                            appendCharDetail(sb, mat.max - 1);
                        }
                        appendStates(sb.append("  -->  "), mat.target);
                    }
                    return appendBlank(sb.append('\n'), blankCount).append(")");
                case MERGE:
                    sb.append("merge").append("(").<Transfer<T>>appends(transfers,
                            (transfer, sbb) -> transfer.appendTo(sbb.append('\n'), blankCount + 4),
                            "").append('\n');
                    return appendBlank(sb, blankCount).append(")");
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private void replaceCharDetail(MyStringBuilder chars, int index) {
            char ch = chars.charAt(index);
            chars.replace(index, index, "(" + (int) ch + ":" + ch + ")", -1, -1);
        }

        private void appendCharDetail(MyStringBuilder chars, int ch) {
            chars.append('(').append(ch).append(':').appendEscape((char) ch).append(')');
        }

        @Override
        public String toString() {
            return appendTo(new MyStringBuilder(), 0).toString();
        }
    }

    @SuppressWarnings("unchecked")
    static class Transfers {
        public static <T> Transfer<T> _true(T dst) {
            return new Transfer<T>(TransferType.TRUE, dst, null, null, null, null) {
                @Override
                public T apply(int value) {
                    return dst;
                }
            };
        }

        private static final Transfer<?> FALSE = _true(null);

        public static <T> Transfer<T> _false() {
            return (Transfer<T>) FALSE;
        }

        public static <T> Transfer<T> condition(T dst, Condition condition) {
            return new Transfer<T>(TransferType.CONDITION, dst, condition, null, null, null) {
                @Override
                public T apply(int value) {
                    return condition.test(value) ? dst : null;
                }
            };
        }

        public static <T> Transfer<T> asciiMap(T[] asciiMap) {
            return new Transfer<T>(TransferType.ASCII_MAP, null, null, asciiMap, null, null) {
                @Override
                public T apply(int value) {
                    return value >= 0 && value < asciiMap.length ? asciiMap[value] : null;
                }
            };
        }

        public static <T> Transfer<T> arrayMap(IntArrayMap<MaxAndTarget<T>> arrayMap) {
            return new Transfer<T>(TransferType.ARRAY_MAP, null, null, null, arrayMap, null) {
                @Override
                public T apply(int value) {
                    int index = arrayMap.getIndex(value);
                    if (index < 0) {
                        index = -index - 2;
                        if (index == -1)
                            return null;
                    }
                    MaxAndTarget<T> mat = arrayMap.getValue(index);
                    return value < mat.max ? mat.target : null;
                }
            };
        }

        public static <T> Transfer<T> merge(Transfer<T>[] transfers) {
            return new Transfer<T>(TransferType.MERGE, null, null, null, null, transfers) {
                @Override
                public T apply(int value) {
                    for (Transfer<T> transfer : transfers) {
                        T next = transfer.apply(value);
                        if (next != null)
                            return next;
                    }
                    return null;
                }
            };
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

        private T privateApply() {
            T buffer = buffers.poll();
            if (buffer == null)
                buffer = allocator.get();
            return buffer;
        }

        private void privateRecycle(T buffer) {
            cleaner.accept(buffer);
            buffers.offer(buffer);
        }

        @SuppressWarnings("unchecked")
        public <V> V apply() {
            return (V) privateApply();
        }

        @SuppressWarnings("unchecked")
        public <V> void recycle(V buffer) {
            privateRecycle((T) buffer);
        }
    }

    static class ConditionMerger {
        private final BufferPool<IntArrayMap<?>> mapPool =
                new BufferPool<>(IntArrayMap::new, IntArrayMap::clear);

        static final int MIN_INT = -1, MAX_INT = Character.MAX_VALUE;
        static final Integer MIN_INTEGER = MIN_INT, MAX_INTEGER = MAX_INT;

        public <T> void merge(IntArrayMap<T> source, ConditionCache conditionCache,
                BiFunction<T, T, T> targetMerger) {
            IntArrayMap<T> arrayTargets = mapPool.apply();
            IntArrayMap<Integer> arrayMap = mapPool.apply();
            arrayMap.put(MIN_INT, MAX_INTEGER);
            IntArrayMap<Integer> tmp = mapPool.apply();
            for (int i = 0; i < source.size(); i++) {
                int conditionId = source.getKey(i);
                T target = source.getValue(i);
                Condition condition = conditionCache.getConditionById(conditionId);
                tmp.clear();
                getConditionRange(condition, tmp);
                for (int j = 0; j < tmp.size(); j++) {
                    int from = tmp.getKey(j);
                    Integer to = tmp.getValue(j);
                    int fromIndex = splitByPoint(arrayMap, arrayTargets, from);
                    int toIndex = splitByPoint(arrayMap, arrayTargets, to);
                    for (int k = fromIndex; k < toIndex; k++) {
                        int key = arrayMap.getKey(k);
                        arrayTargets.put(key, targetMerger.apply(arrayTargets.get(key), target));
                    }
                }
            }
            source.clear();
            for (int i = 0; i < arrayMap.size(); i++) {
                int from = arrayMap.getKey(i);
                T target = arrayTargets.get(from);
                if (target != null) {
                    int conditionId = conditionCache.rangeId(from, arrayMap.getValue(i) - 1);
                    source.put(conditionId, target);
                }
            }
            mapPool.recycle(tmp);
            mapPool.recycle(arrayMap);
            mapPool.recycle(arrayTargets);
        }

        private static <T> int splitByPoint(IntArrayMap<Integer> map, IntArrayMap<T> result,
                int point) {
            int index = map.getIndex(point);
            if (index < 0) {
                index = -index - 1;
                int i = index - 1;
                map.put(point, map.getValue(i));
                map.setValue(i, point);
                result.put(point, result.get(map.getKey(i)));
            }
            return index;
        }

        private void getConditionRange(Condition condition, IntArrayMap<Integer> output) {
            switch (condition.type) {
                case EQ:
                    output.put(condition.min, condition.min + 1);
                    break;
                case RANGE:
                    output.put(condition.min, condition.max + 1);
                    break;
                case NOT: {
                    IntArrayMap<Integer> tmp = mapPool.apply();
                    getConditionRange(condition.sub, tmp);
                    if (tmp.getKey(0) != MIN_INT)
                        output.put(MIN_INT, tmp.getKey(0));
                    int last = tmp.size() - 1;
                    for (int i = 0; i < last; i++) {
                        int from = tmp.getValue(i), to = tmp.getKey(i + 1);
                        if (from < to)
                            output.put(from, to);
                    }
                    int end = tmp.getValue(last);
                    if (end != MAX_INT)
                        output.put(end, MAX_INTEGER);
                    mapPool.recycle(tmp);
                    break;
                }
                case OR: {
                    IntArrayMap<Integer> tmp = mapPool.apply();
                    for (Condition sub : condition.subs) {
                        tmp.clear();
                        getConditionRange(sub, tmp);
                        for (int i = 0; i < tmp.size(); i++) {
                            int from = tmp.getKey(i);
                            Integer to = tmp.getValue(i);
                            int fromIndex = inRange(output, from);
                            int toIndex = inRange(output, to);
                            int newFrom;
                            Integer newTo;
                            if (fromIndex < 0) {
                                fromIndex = -fromIndex - 1;
                                newFrom = from;
                            } else
                                newFrom = output.getKey(fromIndex);
                            if (toIndex < 0) {
                                toIndex = -toIndex - 2;
                                newTo = to;
                            } else
                                newTo = output.getValue(toIndex);
                            if (!(fromIndex == toIndex && output.getKey(fromIndex) == newFrom
                                    && output.getValue(fromIndex) == (int) newTo)) {
                                for (int j = toIndex; j >= fromIndex; j--)
                                    output.remove(j);
                                output.put(newFrom, newTo);
                            }
                        }
                    }
                    mapPool.recycle(tmp);
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private static int inRange(IntArrayMap<Integer> map, int ch) {
            int index = map.getIndex(ch);
            if (index < 0) {
                int i = -index - 2;
                if (i >= 0 && map.getValue(i) >= ch)
                    index = i;
            }
            return index;
        }
    }

    static enum PatternType {
        CONDITION, LINK, OR, REPEAT, ACTION
    }

    static class Pattern {
        public final PatternType type;
        public final Condition condition;// for CONDITION
        public final Pattern[] patterns;// for LINK|OR
        public final Pattern pattern;// for REPEAT
        public final String[] actions;// for ACTION

        private Pattern(PatternType type, Condition condition, Pattern[] patterns, Pattern pattern,
                String[] actions) {
            this.type = type;
            this.condition = condition;
            this.patterns = patterns;
            this.pattern = pattern;
            this.actions = actions;
        }

        @Override
        public String toString() {
            return appendTo(new MyStringBuilder()).toString();
        }

        public MyStringBuilder appendTo(MyStringBuilder sb) {
            switch (type) {
                case CONDITION:
                    return sb.append(condition);
                case LINK:
                    return sb.appends(patterns, Pattern::appendTo, "");
                case OR:
                    return sb.append('(').appends(patterns, Pattern::appendTo, "|").append(')');
                case REPEAT:
                    return sb.append(pattern, Pattern::appendTo).append('*');
                case ACTION:
                    return sb.append(pattern, Pattern::appendTo).append('$').append('{')
                            .appends(actions, ",").append('}');
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean equals(Object obj) {
            Pattern that = (Pattern) obj;
            return this == that || type == that.type && Objects.equals(condition, that.condition)
                    && Arrays.equals(patterns, that.patterns)
                    && Objects.equals(pattern, that.pattern)
                    && Arrays.equals(actions, that.actions);
        }

        @Override
        public int hashCode() {
            return type.hashCode() ^ Objects.hashCode(condition) ^ Arrays.hashCode(patterns)
                    ^ Objects.hashCode(pattern) ^ Arrays.hashCode(actions);
        }
    }

    static class Patterns {
        private static final Pattern[] EMPTY_ARR = {};
        public static final Pattern TRUE =
                new Pattern(PatternType.LINK, null, EMPTY_ARR, null, null);
        public static final Pattern FALSE =
                new Pattern(PatternType.OR, null, EMPTY_ARR, null, null);

        public static Pattern condition(Condition condition) {
            return new Pattern(PatternType.CONDITION, condition, null, null, null);
        }

        public static Pattern link(Pattern... patterns) {
            if (patterns.length == 0)
                return TRUE;
            if (patterns.length == 1)
                return patterns[0];
            return new Pattern(PatternType.LINK, null, patterns, null, null);
        }

        public static Pattern link(List<Pattern> patterns) {
            if (patterns.size() == 0)
                return TRUE;
            if (patterns.size() == 1)
                return patterns.get(0);
            return link(patterns.toArray(new Pattern[patterns.size()]));
        }

        public static Pattern or(Pattern... patterns) {
            if (patterns.length == 0)
                return FALSE;
            if (patterns.length == 1)
                return patterns[0];
            return new Pattern(PatternType.OR, null, patterns, null, null);
        }

        public static Pattern or(List<Pattern> patterns) {
            if (patterns.size() == 0)
                return FALSE;
            if (patterns.size() == 1)
                return patterns.get(0);
            return or(patterns.toArray(new Pattern[patterns.size()]));
        }

        public static Pattern repeat(Pattern pattern) {
            return new Pattern(PatternType.REPEAT, null, null, pattern, null);
        }

        public static Pattern repeat(Pattern pattern, int times) {
            if (times <= 0)
                return TRUE;
            if (times == 1)
                return pattern;
            Pattern[] patterns = new Pattern[times];
            Arrays.fill(patterns, pattern);
            return link(patterns);
        }

        public static Pattern repeat(Pattern pattern, int min, int max) {
            if (min < 0)
                min = 0;
            Pattern result;
            if (max < 0) {
                result = repeat(pattern);
            } else {
                if (max == min)
                    return repeat(pattern, min);
                if (max < min)
                    throw new IllegalArgumentException();
                int times = max - min;
                Pattern[] patterns = new Pattern[times + 1];
                for (int i = 0; i < patterns.length; i++)
                    patterns[i] = repeat(pattern, times - i);
                result = or(patterns);
            }
            if (min != 0) {
                Pattern[] patterns = new Pattern[min + 1];
                Arrays.fill(patterns, 0, min, pattern);
                patterns[min] = result;
                result = link(patterns);
            }
            return result;
        }

        public static <L extends Lex> Pattern action(Pattern pattern, String... actions) {
            if (actions.length == 0)
                return pattern;
            return new Pattern(PatternType.ACTION, null, null, pattern, actions);
        }

        public static <L extends Lex> Pattern action(Pattern pattern, List<String> actions) {
            if (actions.size() == 0)
                return pattern;
            return action(pattern, actions.toArray(new String[actions.size()]));
        }
    }

    public static class StringLex<K> extends Lex {
        public State start;
        public FARunner<? extends State> runner;
        public String str;
        public int index;
        public int begin, end;
        public boolean running;
        public K key;

        public <S extends State> StringLex(S start, FARunner<S> runner) {
            this.start = start;
            this.runner = runner;
        }

        public StringLex(NFAState start) {
            this(start, NFARunner.getInstance());
        }

        public StringLex(DFAState start) {
            this(start, DFARunner.getInstance());
        }

        @Override
        protected int read() {
            int ch = index < str.length() ? str.charAt(index) : -1;
            index++;
            return ch;
        }

        @SuppressWarnings("unchecked")
        public K next() {
            key = null;
            while (true) {
                running = true;
                begin = end = index;
                ((FARunner<State>) runner).run(this, start);
                if (running)
                    throw newError();
                index = end;
                if (key != null)
                    return key;
            }
        }

        public RuntimeException newError(String message) {
            return new RuntimeException(message);
        }

        public RuntimeException newError() {
            int i = Math.max(Math.min(index, str.length()), 0);
            String left = str.substring(Math.max(0, i - 10), i);
            String right = str.substring(i, Math.min(str.length(), i + 10));
            return newError(left + "<<<<<<" + right);
        }

        public void reset(String str) {
            this.str = str;
            this.index = 0;
        }

        public void resetAndNext(String str) {
            reset(str);
            next();
        }

        public void finish(K result) {
            running = false;
            key = result;
            end = index;
        }

        public void rollback() {
            index--;
        }
    }

    static enum RegexKey {
        CHAR, NUM, STR, LITERAL, EOF(-1), //
        LSB('('), RSB(')'), LMB('['), RMB(']'), //
        LBB('{'), RBB('}'), ADD('+'), SUB('-'), COMMA(','), //
        OR('|'), XOR('^'), QUE('?'), DOT('.'), ASTERISK('*'), DOLLER('$'), WELL('#');

        public final int ch;

        private RegexKey(int ch) {
            this.ch = ch;
        }

        private RegexKey() {
            this(0);
        }
    }

    static class RegexLex extends StringLex<RegexKey> {
        private static final NFAState[] START_GROUP = new RegexPatternBuildHelper().build();

        public RegexLex() {
            super(START_GROUP[0]);
        }

        public void switchStart(int index) {
            start = START_GROUP[index];
        }

        public char getChar() {
            char first = str.charAt(begin);
            if (first != '\\')
                return first;
            char second = str.charAt(begin + 1);
            switch (second) {
                case 'r':
                    return '\r';
                case 'n':
                    return '\n';
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'u':
                    return (char) Numbers.parseInt(str, 16, begin + 2, begin + 6);
                default:
                    return second;
            }
        }

        public int getInt() {
            return Numbers.parseInt(str, 10, begin, end);
        }

        public String getString() {
            return str.substring(begin, end);
        }

        private MyStringBuilder buffer = new MyStringBuilder();

        public String getUnescapeString() {
            return buffer.clear().appendUnEscape(str, begin + 1, end - 1).toString();
        }
    }

    static class PatternBuildHelper<L extends Lex> extends Builder<L> {
        private Map<String, Pattern> patternMap = new HashMap<>();
        protected ConditionCache c = new ConditionCache();

        public Pattern get(String regex) {
            Pattern pattern = patternMap.get(regex);
            if (pattern == null)
                throw new IllegalArgumentException();
            return pattern;
        }

        protected void put(String regex, Pattern pattern) {
            patternMap.put(regex, pattern);
        }

        protected void addPattern(Object from, String name, String... actions) {
            addPattern(from, Patterns.action(get(name), actions));
        }

        protected void condition(String regex, Condition condition) {
            put(regex, Patterns.condition(condition));
        }

        protected void link(String regex, String... names) {
            put(regex, Patterns.link(getPatterns(names)));
        }

        protected void or(String regex, String... names) {
            put(regex, Patterns.or(getPatterns(names)));
        }

        protected void repeat(String regex, String name, int min, int max) {
            put(regex, Patterns.repeat(get(name), min, max));
        }

        protected void repeat(String regex, String name, int times) {
            repeat(regex, name, times, times);
        }

        protected void repeat(String regex, String name) {
            repeat(regex, name, -1);
        }

        protected void action(String regex, String name, String... actions) {
            put(regex, Patterns.action(get(name), actions));
        }

        private Pattern[] getPatterns(String[] names) {
            Pattern[] patterns = new Pattern[names.length];
            for (int i = 0; i < names.length; i++)
                patterns[i] = get(names[i]);
            return patterns;
        }
    }

    static class RegexPatternBuildHelper extends PatternBuildHelper<RegexLex> {
        public NFAState[] build() {
            initNFA();
            // setAsciiMapOptimizeThreshold(-1);

            RegexKey[] keys = RegexKey.values();

            for (RegexKey key : keys)
                setAction(key.name(), lex -> lex.finish(key));
            setAction("BLANK", lex -> lex.finish(null));
            setAction("ROLLBACK", lex -> lex.rollback());
            setAction("SWITCH_0", lex -> lex.switchStart(0));
            setAction("SWITCH_1", lex -> lex.switchStart(1));
            setAction("SWITCH_2", lex -> lex.switchStart(2));

            Condition u = c.eq('u');
            Condition number = c.range('0', '9');
            Condition lowcase = c.range('a', 'z');
            Condition upcase = c.range('A', 'Z');
            Condition underline = c.eq('_');
            Condition named_char = c.or(lowcase, upcase, number, underline);

            condition("u", u);
            condition("[^u]", c.not(u));
            condition("\\\\", c.eq('\\'));
            condition("[0-9a-fA-F]", c.or(number, c.range('a', 'f'), c.range('A', 'F')));
            repeat("[0-9a-fA-F]{4}", "[0-9a-fA-F]", 4);
            link("u[0-9a-fA-F]{4}", "u", "[0-9a-fA-F]{4}");
            or("([^u])|(u[0-9a-fA-F]{4})", "[^u]", "u[0-9a-fA-F]{4}");
            link("ESCAPE", "\\\\", "([^u])|(u[0-9a-fA-F]{4})");
            condition("REMAIN", c.remain("()+|?.*$[{\\#"));
            or("CHAR", "ESCAPE", "REMAIN");

            condition("\\\'", c.eq('\''));
            condition("\\\"", c.eq('\"'));
            condition("REMAIN_L1", c.remain("\\\'"));
            condition("REMAIN_L2", c.remain("\\\""));
            or("CHAR_L1", "ESCAPE", "REMAIN_L1");
            or("CHAR_L2", "ESCAPE", "REMAIN_L2");
            repeat("${CHAR_L1}*", "CHAR_L1");
            repeat("${CHAR_L2}*", "CHAR_L2");
            link("LITERAL_L1", "\\\'", "${CHAR_L1}*", "\\\'");
            link("LITERAL_L2", "\\\"", "${CHAR_L2}*", "\\\"");
            or("LITERAL", "LITERAL_L1", "LITERAL_L2");

            condition("-", c.eq('-'));
            repeat("-?", "-", 0, 1);
            condition("[0-9]", number);
            repeat("[0-9]+", "[0-9]", 1, -1);
            link("NUM", "-?", "[0-9]+");

            condition("[a-zA-Z_]", c.or(lowcase, upcase, underline));
            condition("[a-zA-Z0-9_]", named_char);
            repeat("[a-zA-Z0-9_]*", "[a-zA-Z0-9_]");
            link("STR", "[a-zA-Z_]", "[a-zA-Z0-9_]*");

            condition("REMAIN_LMB", c.remain("()$\\^-]"));
            or("CHAR_LMB", "ESCAPE", "REMAIN_LMB");

            condition("BLANK", c.or("\r\n\t\b\f "));
            repeat("BLANK", "BLANK", 1, -1);

            for (RegexKey key : keys)
                if (key.ch != 0)
                    condition(key.name(), c.eq(key.ch));

            action("LBB", "LBB", "SWITCH_1");
            action("LMB", "LMB", "SWITCH_2");
            action("RBB", "RBB", "SWITCH_0");
            action("RMB", "RMB", "SWITCH_0");

            for (RegexKey key : new RegexKey[] {RegexKey.EOF, RegexKey.LSB, RegexKey.RSB,
                    RegexKey.ADD, RegexKey.OR, RegexKey.QUE, RegexKey.DOT, RegexKey.ASTERISK,
                    RegexKey.DOLLER, RegexKey.WELL, RegexKey.CHAR, RegexKey.LBB, RegexKey.LMB})
                addPattern("START", key.name(), key.name());

            for (RegexKey key : new RegexKey[] {RegexKey.COMMA, RegexKey.LSB, RegexKey.RSB,
                    RegexKey.NUM, RegexKey.STR, RegexKey.LITERAL, RegexKey.RBB})
                addPattern("START_LBB", key.name(), key.name());
            addPattern("START_LBB", "BLANK", "BLANK");

            for (RegexKey key : new RegexKey[] {RegexKey.SUB, RegexKey.XOR, RegexKey.DOLLER,
                    RegexKey.RMB, RegexKey.LSB, RegexKey.RSB})
                addPattern("START_LMB", key.name(), key.name());
            addPattern("START_LMB", "CHAR_LMB", RegexKey.CHAR.name());

            NFAState[] states = buildNFA("START", "START_LBB", "START_LMB");
            return states;
        }
    }

    public interface ActionTemplate<L extends Lex> {
        public void invoke(L lex, Object[] args);
    }

    static class TemplatedActionFactory<L extends Lex> {
        private Map<String, ActionTemplate<L>> templateMap;
        private Map<String, Action<L>> actionMap;

        public TemplatedActionFactory(Map<String, ActionTemplate<L>> templateMap,
                Map<String, Action<L>> actionMap) {
            this.templateMap = templateMap;
            this.actionMap = actionMap;
        }

        public String defineAction(String name, Object... args) {
            ActionTemplate<L> template = templateMap.get(name);
            if (template == null)
                throw new IllegalArgumentException();
            String desc = new MyStringBuilder().append(name).append("(").appends(args, ",")
                    .append(")").toString();
            Action<L> action = actionMap.get(desc);
            if (action == null)
                actionMap.put(desc, action = new TemplatedAction<>(desc, template, args));
            return desc;
        }

        static class TemplatedAction<L extends Lex> implements Action<L> {
            private String desc;
            private ActionTemplate<L> template;
            private Object[] args;

            public TemplatedAction(String desc, ActionTemplate<L> template, Object[] args) {
                this.desc = desc;
                this.template = template;
                this.args = args;
            }

            @Override
            public void process(L lex) {
                template.invoke(lex, args);
            }

            @Override
            public String toString() {
                return desc;
            }
        }
    }

    static class RegexParser {
        private TemplatedActionFactory<?> actionFactory;
        private ConditionCache conditionCache;
        private Map<String, Pattern> patternMap = new HashMap<>();
        private Map<String, Object> variablesMap = new HashMap<>();
        private BufferPool<List<?>> listPool = new BufferPool<>(ArrayList::new, List::clear);
        private RegexLex lex = new RegexLex();

        public RegexParser(TemplatedActionFactory<?> actionFactory, ConditionCache conditionCache) {
            this.actionFactory = actionFactory;
            this.conditionCache = conditionCache;

            variablesMap.put("true", Boolean.TRUE);
            variablesMap.put("false", Boolean.FALSE);
            variablesMap.put("null", null);
        }

        public Pattern parseAndAddPattern(String name, String regex) {
            Pattern pattern = parsePattern(regex);
            if (name != null)
                patternMap.put(name, pattern);
            return pattern;
        }

        public Pattern parsePattern(String regex) {
            lex.resetAndNext(regex);
            Pattern pattern = matchLinkPatterns(lex, RegexKey.EOF);
            return pattern;
        }

        public Condition parseCondition(String conditionStr) {
            lex.resetAndNext(conditionStr);
            Condition condition = matchCondition(lex);
            if (lex.key != RegexKey.EOF)
                throw lex.newError();
            return condition;
        }

        public void setVariable(String key, Object value) {
            variablesMap.put(key, value);
        }

        private Pattern matchLinkPatterns(RegexLex lex, RegexKey stopKey) {
            List<Pattern> list = listPool.apply();
            try {
                while (lex.key != stopKey)
                    list.add(matchOrPattern(lex));
                return Patterns.link(list);
            } finally {
                listPool.recycle(list);
            }
        }

        private Pattern matchOrPattern(RegexLex lex) {
            List<Pattern> list = listPool.apply();
            try {
                while (true) {
                    list.add(matchBasePattern(lex));
                    if (lex.key != RegexKey.OR)
                        break;
                    lex.next();
                }
                Pattern pattern = Patterns.or(list);
                List<String> actions = listPool.apply();
                try {
                    matchActions(lex, actions);
                    return Patterns.action(pattern, actions);
                } finally {
                    listPool.recycle(actions);
                }
            } finally {
                listPool.recycle(list);
            }
        }

        private void matchActions(RegexLex lex, List<String> actions) {
            while (lex.key == RegexKey.WELL) {
                if (lex.next() != RegexKey.LBB)
                    throw lex.newError();
                if (lex.next() != RegexKey.STR)
                    throw lex.newError();
                String name = lex.getString();
                if (lex.next() != RegexKey.LSB)
                    throw lex.newError();
                List<Object> list = listPool.apply();
                try {
                    matchArgs(lex, list);
                    Object[] args = list.toArray();
                    actions.add(actionFactory.defineAction(name, args));
                } finally {
                    listPool.recycle(list);
                }
                if (lex.next() != RegexKey.RBB)
                    throw lex.newError();
                lex.next();
            }
        }

        private void matchArgs(RegexLex lex, List<Object> list) {
            L0: while (true) {
                switch (lex.next()) {
                    case NUM:
                        list.add(lex.getInt());
                        break;
                    case STR:
                        list.add(variablesMap.get(lex.getString()));
                        break;
                    case LITERAL:
                        list.add(lex.getUnescapeString());
                        break;
                    case COMMA:
                        break;
                    case RSB:
                        break L0;
                    default:
                        throw lex.newError();
                }
            }
        }

        private Pattern matchBasePattern(RegexLex lex) {
            Pattern pattern;
            if (lex.key == RegexKey.LSB) {
                lex.next();
                pattern = matchLinkPatterns(lex, RegexKey.RSB);
                lex.next();
            } else if (lex.key == RegexKey.DOLLER) {
                if (lex.next() != RegexKey.LBB)
                    throw lex.newError();
                if (lex.next() != RegexKey.STR)
                    throw lex.newError();
                String name = lex.getString();
                pattern = patternMap.get(name);
                if (pattern == null)
                    throw lex.newError("pattern '" + name + "' not found");
                if (lex.next() != RegexKey.RBB)
                    throw lex.newError();
                lex.next();
            } else {
                pattern = Patterns.condition(matchCondition(lex));
            }
            switch (lex.key) {
                case LBB:
                    int min = -1, max = -1;
                    if (lex.next() == RegexKey.NUM) {
                        min = lex.getInt();
                        lex.next();
                    }
                    if (lex.key == RegexKey.COMMA)
                        lex.next();
                    else
                        max = min;
                    if (lex.key == RegexKey.NUM) {
                        max = lex.getInt();
                        lex.next();
                    }
                    if (lex.key != RegexKey.RBB)
                        throw lex.newError();
                    lex.next();
                    pattern = Patterns.repeat(pattern, min, max);
                    break;
                case ADD:
                    lex.next();
                    pattern = Patterns.repeat(pattern, 1, -1);
                    break;
                case QUE:
                    lex.next();
                    pattern = Patterns.repeat(pattern, 0, 1);
                    break;
                case ASTERISK:
                    lex.next();
                    pattern = Patterns.repeat(pattern, 0, -1);
                    break;
                default:
                    break;
            }
            return pattern;
        }

        private Condition matchCondition(RegexLex lex) {
            switch (lex.key) {
                case CHAR:
                    char ch = lex.getChar();
                    lex.next();
                    return conditionCache.eq(ch);
                case LMB:
                    lex.next();
                    return matchOrConditionInLMB(lex, RegexKey.RMB);
                case DOT:
                    lex.next();
                    return conditionCache.not(conditionCache.eq(-1));
                default:
                    throw lex.newError();
            }
        }

        private Condition matchOrConditionInLMB(RegexLex lex, RegexKey stopKey) {
            List<Condition> list = listPool.apply();
            try {
                while (lex.key != stopKey)
                    list.add(matchConditionInLMB(lex));
                lex.next();
                return list.size() == 1 ? list.get(0)
                        : conditionCache.or(list.toArray(new Condition[list.size()]));
            } finally {
                listPool.recycle(list);
            }
        }

        private Condition matchConditionInLMB(RegexLex lex) {
            switch (lex.key) {
                case CHAR:
                    char left = lex.getChar();
                    lex.next();
                    if (lex.key != RegexKey.SUB)
                        return conditionCache.eq(left);
                    if (lex.next() != RegexKey.CHAR)
                        throw lex.newError();
                    char right = lex.getChar();
                    lex.next();
                    return conditionCache.range(left, right);
                case XOR:
                    lex.next();
                    return conditionCache.not(matchConditionInLMB(lex));
                case DOLLER:
                    lex.next();
                    return conditionCache.eq(-1);
                case LSB:
                    lex.next();
                    return matchOrConditionInLMB(lex, RegexKey.RSB);
                default:
                    throw lex.newError();
            }
        }
    }

    @SuppressWarnings("unchecked")
    static class ArraySet<T> {
        private Object[] arr;
        private int size;

        public ArraySet() {
            arr = new Object[10];
        }

        public ArraySet(ArraySet<T> that) {
            this.size = that.size;
            this.arr = Arrays.copyOfRange(that.arr, 0, that.size);
        }

        public T get(int index) {
            return (T) arr[index];
        }

        public int size() {
            return size;
        }

        public boolean add(T e) {
            int index = Arrays.binarySearch(arr, 0, size, e);
            if (index >= 0)
                return false;
            resize(1);
            int i = -index - 1;
            System.arraycopy(arr, i, arr, i + 1, size - i);
            arr[i] = e;
            size++;
            return true;
        }

        public void addAll(T[] arr) {
            if (arr != null)
                for (T e : arr)
                    add(e);
        }

        public void addAll(ArraySet<T> set) {
            if (set != null) {
                if (size != 0) {
                    for (int i = 0; i < set.size; i++)
                        add((T) set.arr[i]);
                } else {
                    resize(set.size - size);
                    System.arraycopy(set.arr, 0, arr, 0, set.size);
                    size = set.size;
                }
            }
        }

        public void clear() {
            size = 0;
        }

        private void resize(int n) {
            int need = size + n;
            if (arr.length < need)
                arr = Arrays.copyOf(arr, Math.max(arr.length * 2, need));
        }

        @Override
        public String toString() {
            return Arrays.toString(Arrays.copyOfRange(arr, 0, size));
        }

        @Override
        public int hashCode() {
            return hashCode(arr, 0, size);
        }

        private static int hashCode(Object[] a, int begin, int end) {
            int result = 1;
            for (; begin < end; begin++)
                result = 31 * result + Objects.hashCode(a[begin]);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            ArraySet<T> that = (ArraySet<T>) obj;
            return this == that || size == that.size && equals(arr, that.arr, 0, size);
        }

        private static boolean equals(Object[] a1, Object[] a2, int begin, int end) {
            for (; begin < end; begin++)
                if (!Objects.equals(a1[begin], a2[begin]))
                    return false;
            return true;
        }
    }

    static class IntArraySet {
        private int[] arr;
        private int size;

        public IntArraySet() {
            arr = new int[10];
        }

        public IntArraySet(IntArraySet that) {
            this.size = that.size;
            this.arr = Arrays.copyOfRange(that.arr, 0, that.size);
        }

        public int get(int index) {
            return arr[index];
        }

        public int size() {
            return size;
        }

        public boolean add(int e) {
            int index = Arrays.binarySearch(arr, 0, size, e);
            if (index >= 0)
                return false;
            resize(1);
            int i = -index - 1;
            System.arraycopy(arr, i, arr, i + 1, size - i);
            arr[i] = e;
            size++;
            return true;
        }

        public void addAll(IntArraySet set) {
            if (set != null) {
                if (size != 0) {
                    for (int i = set.size - 1; i >= 0; i--)
                        add(set.arr[i]);
                } else {
                    resize(set.size - size);
                    System.arraycopy(set.arr, 0, arr, 0, set.size);
                    size = set.size;
                }
            }
        }

        public int remove(int index) {
            int old = arr[index];
            if (index < --size)
                System.arraycopy(arr, index + 1, arr, index, size - index);
            return old;
        }

        public void clear() {
            size = 0;
        }

        private void resize(int n) {
            int need = size + n;
            if (arr.length < need)
                arr = Arrays.copyOf(arr, Math.max(arr.length * 2, need));
        }

        @Override
        public String toString() {
            return Arrays.toString(Arrays.copyOfRange(arr, 0, size));
        }

        @Override
        public int hashCode() {
            return hashCode(arr, 0, size);
        }

        private static int hashCode(int[] a, int begin, int end) {
            int result = 1;
            for (; begin < end; begin++)
                result = 31 * result + a[begin];
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            IntArraySet that = (IntArraySet) obj;
            return size == that.size && equals(arr, that.arr, 0, size);
        }

        private static boolean equals(int[] a1, int[] a2, int begin, int end) {
            for (; begin < end; begin++)
                if (a1[begin] != a2[begin])
                    return false;
            return true;
        }
    }

    static class IntArrayMap<T> {
        private int[] keys;
        private Object[] values;
        private int size;

        public IntArrayMap() {
            keys = new int[10];
            values = new Object[10];
        }

        public T get(int key) {
            int index = getIndex(key);
            if (index < 0)
                return null;
            return getValue(index);
        }

        public void put(int key, T value) {
            int index = getIndex(key);
            if (index < 0) {
                resize(1);
                int i = -index - 1;
                System.arraycopy(keys, i, keys, i + 1, size - i);
                System.arraycopy(values, i, values, i + 1, size - i);
                keys[i] = key;
                values[i] = value;
                size++;
            } else
                values[index] = value;
        }

        public int getIndex(int key) {
            return Arrays.binarySearch(keys, 0, size, key);
        }

        public int getKey(int index) {
            return keys[index];
        }

        @SuppressWarnings("unchecked")
        public T getValue(int index) {
            return (T) values[index];
        }

        public void setValue(int index, T value) {
            values[index] = value;
        }

        public int size() {
            return size;
        }

        public void remove(int index) {
            if (index < --size) {
                System.arraycopy(keys, index + 1, keys, index, size - index);
                System.arraycopy(values, index + 1, values, index, size - index);
            }
        }

        public void clear() {
            size = 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            for (int i = 0; i < size; i++)
                sb.append(keys[i]).append('=').append(values[i]).append(',');
            if (size != 0)
                sb.deleteCharAt(sb.length() - 1);
            sb.append('}');
            return sb.toString();
        }

        private void resize(int n) {
            int need = size + n;
            if (keys.length < need) {
                int len = Math.max(keys.length * 2, need);
                keys = Arrays.copyOf(keys, len);
                values = Arrays.copyOf(values, len);
            }
        }
    }
}
