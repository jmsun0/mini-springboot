package com.sjm.core.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sjm.core.util.Analyzer;
import com.sjm.core.util.CharFilter;
import com.sjm.core.util.DateFormats;
import com.sjm.core.util.IOUtil;
import com.sjm.core.util.Lists;
import com.sjm.core.util.Maps;
import com.sjm.core.util.Misc;
import com.sjm.core.util.MyStringBuilder;
import com.sjm.core.util.Numbers;
import com.sjm.core.util.Size;
import com.sjm.core.util.Source;
import com.sjm.core.util.Strings;

public class LoggerFactory {
    static {
        try {
            loadLoggerProperties();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void loadLoggerProperties() {
        ResourceBundle res = ResourceBundle.getBundle("logger");
        Enumeration<String> keys = res.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String value = System.getProperty(key);
            if (Misc.isEmpty(value))
                System.setProperty(key, res.getString(key));
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        String[] nameArray = clazz.getName().split("\\.");
        MyStringBuilder sb = new MyStringBuilder();
        String levelStr = null;
        String pattern = null;
        Set<LogAppender> appenderSet = new HashSet<>();
        for (int i = nameArray.length - 1; i >= 0; i--) {
            sb.clear();
            sb.append("logger.loggers.");
            int fromIndex = sb.length();
            for (int j = 0; j <= i; j++) {
                sb.append(nameArray[j]).append('.');
            }
            String loggerName =
                    new String(sb.getLocalChars(), fromIndex, sb.length() - 1 - fromIndex);
            List<LogAppender> appenders = LogAppenders.appendersMap.get(loggerName);
            if (appenders != null) {
                appenderSet.addAll(appenders);
            }
            if (Misc.isEmpty(levelStr)) {
                levelStr = Util.getProperty(sb, "level", null, String.class);
            }
            if (Misc.isEmpty(pattern)) {
                pattern = Util.getProperty(sb, "pattern", null, String.class);
            }
        }
        appenderSet.addAll(LogAppenders.defaultAppenders);
        LogAppender[] appenderArr = (LogAppender[]) Lists.toArray(appenderSet, LogAppender.class);
        int level;
        if (Misc.isEmpty(levelStr))
            level = Util.getDefaultLevel();
        else
            level = Util.getLevel(levelStr);
        if (Misc.isEmpty(pattern))
            pattern = Util.getDefaultPattern();
        return new Logger(clazz, level, LogFormatParser.parseOrGetCached(pattern), appenderArr);
    }

    private static ThreadLocal<LogContext> logContextThreadLocal = new ThreadLocal<>();

    static LogContext getLogContext() {
        LogContext ctx = logContextThreadLocal.get();
        if (ctx == null)
            logContextThreadLocal.set(ctx = new LogContext());
        return ctx;
    }

    static class Util {
        private static String[] levelStrArray =
                {"ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "NONE"};

        public static String getLevelStr(int level) {
            return levelStrArray[level];
        }

        private static TreeMap<String, Integer> strLevelMap;
        static {
            strLevelMap = new TreeMap<>(String::compareToIgnoreCase);
            for (int i = 0; i < levelStrArray.length; i++)
                strLevelMap.put(levelStrArray[i], i);
        }

        public static int getLevel(String levelStr) {
            return strLevelMap.get(levelStr);
        }

        public static <T> T getProperty(MyStringBuilder prefix, String suffix, T defaultValue,
                Class<T> clazz) {
            String key = prefix.append(suffix).toString();
            prefix.deleteEnd(suffix.length());
            return Misc.getProperty(key, defaultValue, clazz);
        }

        public static String getDefaultPath() {
            return Misc.getProperty("logger.default.appenders.path", "logs", String.class);
        }

        public static int getDefaultLevel() {
            return getLevel(Misc.getProperty("logger.default.level", "info", String.class));
        }

        public static String getDefaultPattern() {
            return Misc.getProperty("logger.default.loggers.pattern",
                    "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p --- [%t] %-25.25logger{1.1}.%M[Line:%L] : %m%n",
                    String.class);
        }

        public static long getDefaultMaxSize() {
            String str = Misc.getProperty("logger.default.appenders.max-size", "", String.class);
            return str.isEmpty() ? -1 : Size.parseSize(str);
        }

        public static long getDefaultTotalSize() {
            String str = Misc.getProperty("logger.default.appenders.total-size", "", String.class);
            return str.isEmpty() ? -1 : Size.parseSize(str);
        }

        public static int getDefaultMaxHistory() {
            return Misc.getProperty("logger.default.appenders.max-history", -1, int.class);
        }
    }

    static class LogContext {
        public Class<?> clazz;
        public MyStringBuilder sb = new MyStringBuilder();
        public Thread thread;
        public int level;
        public long time;
        public StackTraceElement stack;
        public String format;
        public Object[] args = new Object[20];
        public int argsCount;
        public Throwable throwable;
        public LogFormat logFormat;
        public Map<String, String> attributes = new HashMap<>();
    }

    static class LogFormat {
        public LogPattern[] patterns;
        public boolean needTime;
        public boolean needStack;
    }

    interface LogPattern {
        public void append(LogContext ctx);
    }

    static class LogPatterns {
        // %m %msg %message
        public static LogPattern message(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> appendMessage(ctx));
        }

        // %d %date {DATE_FORMAT} {TIME_ZONE}
        public static LogPattern date(boolean left, int min, int max, String... args) {
            DateFormats fmt;
            if (args.length >= 1)
                fmt = DateFormats.valueOf(args[0]);
            else
                fmt = DateFormats.yyyy_MM_ddHHmmss;
            TimeZone zone;
            if (args.length >= 2)
                zone = TimeZone.getTimeZone(args[1]);
            else
                zone = null;
            return make(left, min, max, ctx -> ctx.sb.append(fmt.format(new Date(ctx.time), zone)));
        }

        // %c %logger {EXPRESSION}
        public static LogPattern logger(boolean left, int min, int max, String... args) {
            ClassNameFormatter formatter = new ClassNameFormatter(args.length >= 1 ? args[0] : "");
            return make(left, min, max, ctx -> formatter.formatTo(ctx.clazz.getName(), ctx.sb));
        }

        // %C %class {EXPRESSION}
        public static LogPattern clazz(boolean left, int min, int max, String... args) {
            ClassNameFormatter formatter = new ClassNameFormatter(args.length >= 1 ? args[0] : "");
            return make(left, min, max,
                    ctx -> formatter.formatTo(ctx.stack.getClassName(), ctx.sb));
        }

        // %F %file
        public static LogPattern file(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(ctx.stack.getFileName()));
        }

        // %l %location
        public static LogPattern location(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(ctx.stack.toString()));
        }

        // %L %line
        public static LogPattern line(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(ctx.stack.getLineNumber()));
        }

        // %M %method
        public static LogPattern method(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(ctx.stack.getMethodName()));
        }

        // %n
        public static LogPattern lineSeparator(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(IOUtil.LINE_SEPARATOR));
        }

        // %p %level
        public static LogPattern level(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(Util.getLevelStr(ctx.level)));
        }

        // %r %relative
        public static LogPattern relative(boolean left, int min, int max, String... args) {
            long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
            return make(left, min, max, ctx -> ctx.sb.append(ctx.time - startTime));
        }

        // TODO %replace

        // TODO %marker

        // %sn %sequenceNumber
        private static int sn = 1;

        public static LogPattern sequenceNumber(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(sn++));
        }

        // %t %thread
        public static LogPattern thread(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(ctx.thread.getName()));
        }

        // %u %uuid
        public static LogPattern uuid(boolean left, int min, int max, String... args) {
            return make(left, min, max, ctx -> ctx.sb.append(UUID.randomUUID().toString()));
        }

        // %X %attr $ {KEY[:DEFAULT_VALUE]}
        public static LogPattern attr(boolean left, int min, int max, String... args) {
            if (args.length < 1)
                throw new IllegalArgumentException();
            String[] arr = args[0].split(":");
            String key = arr[0];
            String defaultValue = arr.length > 1 ? arr[1] : null;
            return make(left, min, max,
                    ctx -> ctx.sb.append(Maps.getOrDefault(ctx.attributes, key, defaultValue)));
        }

        public static LogPattern simple(String text) {
            return ctx -> ctx.sb.append(text);
        }

        private static LogPattern make(boolean left, int min, int max, LogPattern realMessage) {
            if (min == -1 && max == -1)
                return realMessage;
            else
                return ctx -> appendFix(ctx, left, min, max, realMessage);
        }

        private static void appendFix(LogContext ctx, boolean left, int min, int max,
                LogPattern realMessage) {
            int begin = ctx.sb.length();
            realMessage.append(ctx);
            int end = ctx.sb.length();
            int fromLength = end - begin;
            int toLength = max == -1 ? Math.max(fromLength, min) : Math.min(fromLength, max);
            int moveLength = Math.min(fromLength, toLength);
            int from = Math.max(fromLength - toLength, 0);
            int to = left ? 0 : Math.max(toLength - fromLength, 0);
            if (from != to) {
                if (fromLength < toLength)
                    ctx.sb.resize(toLength - fromLength);
                char[] chars = ctx.sb.getLocalChars();
                System.arraycopy(chars, begin + from, chars, begin + to, moveLength);
                ctx.sb.setLength(begin + toLength);
            }
            if (fromLength < toLength) {
                int start = begin + (left ? fromLength : 0);
                char[] chars = ctx.sb.getLocalChars();
                for (int i = 0, n = toLength - fromLength; i < n; i++) {
                    chars[start + i] = ' ';
                }
            }
        }

        private static void appendMessage(LogContext ctx) {
            if (ctx.format == null)
                ctx.sb.appendNull();
            else {
                if (ctx.argsCount == 0)
                    ctx.sb.append(ctx.format);
                else {
                    int from = 0;
                    for (int i = 0; i < ctx.argsCount; i++) {
                        int index = ctx.format.indexOf("{}", from);
                        if (index == -1)
                            break;
                        ctx.sb.append(ctx.format, from, index - from);
                        ctx.sb.append(ctx.args[i]);
                        from = index + 2;
                    }
                    ctx.sb.append(ctx.format, from, ctx.format.length() - from);
                }
            }
        }

        static class ClassNameFormatter {
            private int n;
            private ReplaceHandler[] handlers;

            public ClassNameFormatter(String exp) {
                if (Misc.isNotEmpty(exp)) {
                    String[] arr = exp.split("\\.");
                    if (arr.length == 1) {
                        n = Integer.parseInt(arr[0]);
                    } else {
                        int len = arr[arr.length - 1].isEmpty() ? arr.length - 1 : arr.length;
                        handlers = new ReplaceHandler[len];
                        for (int i = 0; i < len; i++) {
                            String str = arr[i];
                            int numTmp = -1;
                            if (!str.isEmpty() && Strings.isNumber(str.charAt(0))) {
                                try {
                                    numTmp = Integer.parseInt(str);
                                } catch (Exception e) {
                                }
                            }
                            if (numTmp != -1) {
                                int num = numTmp;
                                handlers[i] = (className, from, to, sb) -> replaceForInt(className,
                                        from, to, sb, num);
                            } else {
                                handlers[i] = (className, from, to,
                                        sb) -> replaceForString(className, from, to, sb, str);
                            }
                        }
                    }
                } else {
                    n = -1;
                }
            }

            public void formatTo(String className, MyStringBuilder sb) {
                if (handlers != null) {
                    int end = className.lastIndexOf('.') + 1;
                    for (int i = 0, from = 0; from < end;) {
                        int index = className.indexOf('.', from);
                        handlers[i].replace(className, from, index - 1, sb);
                        sb.append('.');
                        if (i < handlers.length - 1)
                            i++;
                        from = index + 1;
                    }
                    sb.append(className, end, className.length() - end);
                } else {
                    if (n <= 0) {
                        sb.append(className);
                    } else {
                        int index = className.length() - 1;
                        for (int i = 0; i < n; i++) {
                            index = className.lastIndexOf('.', index) - 1;
                            if (index < 0)
                                break;
                        }
                        index += 2;
                        sb.append(className, index, className.length() - index);
                    }
                }
            }

            private static void replaceForInt(String className, int from, int to,
                    MyStringBuilder sb, int num) {
                sb.append(className, from, Math.min(num, to - from));
            }

            private static void replaceForString(String className, int from, int to,
                    MyStringBuilder sb, String str) {
                sb.append(str);
            }

            interface ReplaceHandler {
                public void replace(String className, int from, int to, MyStringBuilder sb);
            }
        }
    }

    interface PatternMaker<P> {
        public P make(boolean left, int min, int max, String... args);
    }

    static class LogFormatParser extends PatternParser<LogPattern> {
        private static ThreadLocal<LogFormat> currentLogFormat = new ThreadLocal<>();

        public static Map<String, PatternMaker<LogPattern>> makers = new HashMap<>();
        public static Set<PatternMaker<LogPattern>> needTimeMakes = new HashSet<>();
        public static Set<PatternMaker<LogPattern>> needStackMakes = new HashSet<>();

        private static void addMakers(PatternMaker<LogPattern> maker, String... names) {
            for (String name : names)
                makers.put(name, maker);
        }

        private static void addExistMakers(Set<PatternMaker<LogPattern>> set, String... names) {
            for (String name : names)
                set.add(makers.get(name));
        }

        static {
            addMakers(LogPatterns::message, "m", "msg", "message");
            addMakers(LogPatterns::date, "d", "date");
            addMakers(LogPatterns::logger, "c", "logger");
            addMakers(LogPatterns::clazz, "C", "class");
            addMakers(LogPatterns::file, "F", "file");
            addMakers(LogPatterns::location, "l", "location");
            addMakers(LogPatterns::line, "L", "line");
            addMakers(LogPatterns::method, "M", "method");
            addMakers(LogPatterns::lineSeparator, "n");
            addMakers(LogPatterns::level, "p", "level");
            addMakers(LogPatterns::relative, "r", "relative");
            addMakers(LogPatterns::sequenceNumber, "sn", "sequenceNumber");
            addMakers(LogPatterns::thread, "t", "thread");
            addMakers(LogPatterns::uuid, "u", "uuid");
            addMakers(LogPatterns::attr, "X", "attr");

            addExistMakers(needTimeMakes, "date", "relative");

            addExistMakers(needStackMakes, "clazz", "file", "location", "line", "method");
        }

        private static LogFormatParser INSTANCE = new LogFormatParser();

        public static LogFormat parse(String str) {
            LogFormat logFormat = new LogFormat();
            currentLogFormat.set(logFormat);
            try {
                List<LogPattern> patternList = INSTANCE.parseList(str);
                logFormat.patterns = Lists.toTArray(patternList, LogPattern.class);
                return logFormat;
            } finally {
                currentLogFormat.remove();
            }
        }

        private static Map<String, LogFormat> cacheMap = new HashMap<>();

        public static LogFormat parseOrGetCached(String str) {
            LogFormat fmt = cacheMap.get(str);
            if (fmt == null)
                cacheMap.put(str, fmt = parse(str));
            return fmt;
        }

        @Override
        protected LogPattern simple(String value) {
            return LogPatterns.simple(value);
        }

        @Override
        protected LogPattern attr(String value) {
            return LogPatterns.attr(false, -1, -1, value);
        }

        @Override
        protected Iterable<String> itemKeys() {
            return makers.keySet();
        }

        @Override
        protected LogPattern item(String key, boolean left, int min, int max, String... args) {
            PatternMaker<LogPattern> maker = makers.get(key);
            if (maker == null)
                throw new IllegalArgumentException();
            if (needTimeMakes.contains(maker))
                currentLogFormat.get().needTime = true;
            if (needStackMakes.contains(maker))
                currentLogFormat.get().needStack = true;
            return maker.make(left, min, max, args);
        }
    }

    static class FileNameFormatParser extends PatternParser<FileNameFormat> {
        public static Map<String, PatternMaker<FileNameFormat>> makers = new HashMap<>();

        private static void addMakers(PatternMaker<FileNameFormat> maker, String... names) {
            for (String name : names)
                makers.put(name, maker);
        }

        static {
            addMakers(FileNameFormats::date, "d", "date");
            addMakers(FileNameFormats::index, "i", "index");
        }

        private static FileNameFormatParser INSTANCE = new FileNameFormatParser();

        public static FileNameFormat parse(String str) {
            return FileNameFormats
                    .link(Lists.toTArray(INSTANCE.parseList(str), FileNameFormat.class));
        }

        @Override
        protected FileNameFormat simple(String value) {
            return FileNameFormats.simple(value);
        }

        @Override
        protected FileNameFormat attr(String value) {
            throw new IllegalArgumentException();
        }

        @Override
        protected Iterable<String> itemKeys() {
            return makers.keySet();
        }

        @Override
        protected FileNameFormat item(String key, boolean left, int min, int max, String... args) {
            PatternMaker<FileNameFormat> maker = makers.get(key);
            if (maker == null)
                throw new IllegalArgumentException();
            return maker.make(left, min, max, args);
        }
    }

    static abstract class PatternParser<P> {
        protected abstract P simple(String value);

        protected abstract P attr(String value);

        protected abstract Iterable<String> itemKeys();

        protected abstract P item(String key, boolean left, int min, int max, String... args);

        static enum PatternKey {
            EOF, TEXT, MESSAGE, PERCENT, VARIABLE
        }

        Analyzer<PatternKey> patternAnalyzer;
        {
            patternAnalyzer = new Analyzer<>();
            patternAnalyzer.setEOF(PatternKey.EOF);

            Analyzer.Model model = new Analyzer.Model();
            model.addLine(0, '%', 1);
            model.addLine(1, '-', 2);
            model.addLine(1, null, 2);
            model.addLine(1, null, 5);
            model.addLine(2, CharFilter.DecimalNumber, 3);
            model.addLine(2, '.', 4);
            model.addLine(3, CharFilter.DecimalNumber, 3);
            model.addLine(3, '.', 4);
            model.addLine(3, null, 5);
            model.addLine(4, CharFilter.DecimalNumber, 4);
            model.addLine(4, null, 5);
            for (String key : itemKeys()) {
                model.addLine(5, key.charAt(0), key + "_1");
                for (int i = 1; i < key.length(); i++) {
                    model.addLine(key + "_" + i, key.charAt(i), key + "_" + (i + 1));
                }
                model.addLine(key + "_" + key.length(), null, 6);
            }
            model.addLine(6, '{', 7);
            model.addLine(6, null, 8);
            model.addLine(7, '}', 6);
            model.addLine(7, CharFilter.Any, 7);
            model.addAction(8, Analyzer.Action.finish(PatternKey.MESSAGE));
            patternAnalyzer.setModel(model);

            patternAnalyzer.setSymbol("%%", PatternKey.PERCENT);

            patternAnalyzer.setPattern("\\$\\{[^\\}]*\\}", PatternKey.VARIABLE);

            model = new Analyzer.Model();
            model.addLine(0, CharFilter.Any, 1);
            model.addLine(1, CharFilter.or(CharFilter.equal(-1), CharFilter.equal('%'),
                    CharFilter.equal('$')), 2);
            model.addLine(1, CharFilter.Any, 1);
            model.addAction(2, Analyzer.Action.Back);
            model.addAction(2, Analyzer.Action.finish(PatternKey.TEXT));
            patternAnalyzer.setModel(model);
        }

        public List<P> parseList(String str) {
            List<P> list = new ArrayList<>();
            Source<PatternKey> src = patternAnalyzer.analyze(str);
            L0: while (true) {
                PatternKey key = src.next();
                switch (key) {
                    case TEXT:
                        list.add(simple(src.getValue().toString()));
                        break;
                    case MESSAGE:
                        list.add(parseItem(src.getValue()));
                        break;
                    case EOF:
                        break L0;
                    case PERCENT:
                        list.add(simple("%"));
                        break;
                    case VARIABLE:
                        CharSequence value = src.getValue();
                        list.add(attr(value.subSequence(2, value.length() - 1).toString()));
                        break;
                }
            }
            return list;
        }

        static enum ItemKey {
            EOF, PERCENT, SUB, INTEGER, DOT, TEXT, PARAM
        }

        static Analyzer<ItemKey> messageAnalyzer;
        static {
            messageAnalyzer = new Analyzer<>();
            messageAnalyzer.setEOF(ItemKey.EOF);
            messageAnalyzer.setText(ItemKey.TEXT);
            messageAnalyzer.setSymbol("%", ItemKey.PERCENT);
            messageAnalyzer.setSymbol("-", ItemKey.SUB);
            messageAnalyzer.setPattern("[0-9]+", ItemKey.INTEGER);
            messageAnalyzer.setSymbol(".", ItemKey.DOT);
            messageAnalyzer.setPattern("\\{[^\\}]*\\}", ItemKey.PARAM);
        }

        private P parseItem(CharSequence str) {
            Source<ItemKey> src = messageAnalyzer.analyze(str);
            ItemKey key = src.next();
            if (key == ItemKey.PERCENT) {
                key = src.next();
            } else
                throw new IllegalArgumentException();
            boolean left = false;
            if (key == ItemKey.SUB) {
                left = true;
                key = src.next();
            }
            int min = -1, max = -1;
            if (key == ItemKey.INTEGER) {
                min = Numbers.parseIntWithoutSign(src.getValue(), null, 10, -1, -1);
                key = src.next();
            }
            if (key == ItemKey.DOT) {
                key = src.next();
            }
            if (key == ItemKey.INTEGER) {
                max = Numbers.parseIntWithoutSign(src.getValue(), null, 10, -1, -1);
                key = src.next();
            }
            String name;
            if (key == ItemKey.TEXT) {
                name = src.getValue().toString();
                key = src.next();
            } else
                throw new IllegalArgumentException();
            List<String> params = new ArrayList<>();
            while (key == ItemKey.PARAM) {
                CharSequence value = src.getValue();
                params.add(value.subSequence(1, value.length() - 1).toString());
                key = src.next();
            }
            return item(name, left, min, max, Lists.toTArray(params, String.class));
        }
    }

    static class FileAppenderContext {
        public File path;
        public File file;
        public FileNameFormat format;
        public boolean[] levelSet;
        public long maxSize;
        public long totalSize;
        public int maxHistoryCount;
        public long checkInterval;

        public PrintWriter out;
        public long nextCheckTime;
        public long currentSize;
        public long currentHistorySize;
        public int currentHistoryCount;
    }

    interface FileNameFormat {
        public void formatTo(MyStringBuilder sb, FileNameFormatContext fctx);

        public int match(String str, int from, FileNameFormatContext fctx);
    }

    static class FileNameFormatContext implements Comparable<FileNameFormatContext> {
        public Date date;
        public int index;
        public String file;

        @Override
        public int compareTo(FileNameFormatContext o) {
            int c = (int) (getTime(date) - getTime(o.date) / 1000 / 60);
            if (c == 0) {
                c = index - o.index;
            }
            return c;
        }

        static long getTime(Date date) {
            return date == null ? 0 : date.getTime();
        }
    }

    static class FileNameFormats {
        // %d %date {DATE_FORMAT} {TIME_ZONE}
        public static FileNameFormat date(boolean left, int min, int max, String... args) {
            DateFormats fmt;
            if (args.length >= 1)
                fmt = DateFormats.valueOf(args[0]);
            else
                fmt = DateFormats.yyyy_MM_ddHHmmss;
            TimeZone zone;
            if (args.length >= 2)
                zone = TimeZone.getTimeZone(args[1]);
            else
                zone = null;
            return new FileNameFormat() {
                @Override
                public void formatTo(MyStringBuilder sb, FileNameFormatContext fctx) {
                    sb.append(fmt.format(fctx.date, zone));
                }

                @Override
                public int match(String str, int from, FileNameFormatContext fctx) {
                    try {
                        ParsePosition pos = new ParsePosition(from);
                        fctx.date = fmt.parse(str, zone, pos);
                        return pos.getIndex();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return -1;
                    }
                }
            };
        }

        // %i %index
        public static FileNameFormat index(boolean left, int min, int max, String... args) {
            return new FileNameFormat() {
                @Override
                public void formatTo(MyStringBuilder sb, FileNameFormatContext fctx) {
                    sb.append(fctx.index);
                }

                @Override
                public int match(String str, int from, FileNameFormatContext fctx) {
                    try {
                        int index = Strings.indexOf(str, null,
                                CharFilter.not(CharFilter.DecimalNumber), from, -1);
                        if (index == -1)
                            index = str.length();
                        fctx.index = Numbers.parseIntWithoutSign(str, null, 10, from, index);
                        return index;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return -1;
                    }
                }
            };
        }

        public static FileNameFormat simple(String key) {
            return new FileNameFormat() {
                @Override
                public void formatTo(MyStringBuilder sb, FileNameFormatContext fctx) {
                    sb.append(key);
                }

                @Override
                public int match(String str, int from, FileNameFormatContext fctx) {
                    if (!str.startsWith(key, from))
                        return -1;
                    return from + key.length();
                }
            };
        }

        public static FileNameFormat link(FileNameFormat... fmts) {
            return new FileNameFormat() {
                @Override
                public void formatTo(MyStringBuilder sb, FileNameFormatContext fctx) {
                    for (FileNameFormat fmt : fmts)
                        fmt.formatTo(sb, fctx);
                }

                @Override
                public int match(String str, int from, FileNameFormatContext fctx) {
                    for (FileNameFormat fmt : fmts) {
                        from = fmt.match(str, from, fctx);
                        if (from == -1)
                            return -1;
                    }
                    return from;
                }
            };
        }
    }

    interface LogAppender {
        public void append(int level, MyStringBuilder sb, Throwable throwable);
    }

    static class LogAppenders {
        public static List<LogAppender> defaultAppenders = new ArrayList<>();
        public static Map<String, List<LogAppender>> appendersMap = new HashMap<>();

        static {
            loadLogAppenders();
        }

        private static void loadLogAppenders() {
            String appenderNames = Misc.getProperty("logger.appenders", "", String.class);
            if (!appenderNames.isEmpty()) {
                List<String> appenderNameList = Strings.split(appenderNames, ",", String.class);
                MyStringBuilder sb = new MyStringBuilder();
                for (String appenderName : appenderNameList) {
                    sb.clear();
                    sb.append("logger.appenders.").append(appenderName).append('.');
                    LogAppender appender;
                    String type = Util.getProperty(sb, "type", "file", String.class);
                    String levels = Util.getProperty(sb, "levels", "", String.class);
                    boolean[] levelSet = parseLevelSet(levels);
                    switch (type) {
                        case "stdout":
                            appender = getPrintStreamAppender(System.out, levelSet);
                            break;
                        case "stderr":
                            appender = getPrintStreamAppender(System.err, levelSet);
                            break;
                        case "file":
                            String path = Util.getProperty(sb, "path", "", String.class);
                            if (path.isEmpty())
                                path = Util.getDefaultPath();
                            String fileName = Util.getProperty(sb, "file-name", "", String.class);
                            if (fileName.isEmpty())
                                throw new IllegalArgumentException();
                            String filePattern =
                                    Util.getProperty(sb, "file-pattern", "", String.class);
                            if (filePattern.isEmpty())
                                throw new IllegalArgumentException();
                            long maxSize = Util.getProperty(sb, "max-size", -1l, long.class);
                            if (maxSize < 0)
                                maxSize = Util.getDefaultMaxSize();
                            long totalSize = Util.getProperty(sb, "total-size", -1l, long.class);
                            if (totalSize < 0)
                                totalSize = Util.getDefaultTotalSize();
                            int maxHistory = Util.getProperty(sb, "max-history", -1, int.class);
                            if (maxHistory < 0)
                                maxHistory = Util.getDefaultMaxHistory();
                            appender = getFileAppender(path, fileName, filePattern, levelSet,
                                    maxSize, totalSize, maxHistory);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    String loggers = Util.getProperty(sb, "loggers", "", String.class);
                    if (loggers.isEmpty()) {
                        if (appender != null)
                            defaultAppenders.add(appender);
                    } else {
                        List<String> loggerList = Strings.split(loggers, ",", String.class);
                        for (String logger : loggerList) {
                            List<LogAppender> appenderList = appendersMap.get(logger);
                            if (appenderList == null)
                                appendersMap.put(logger, appenderList = new ArrayList<>());
                            appenderList.add(appender);
                        }
                    }
                }
            } else {
                defaultAppenders.add(getPrintStreamAppender(System.out, parseLevelSet("")));
            }
        }

        private static LogAppender getPrintStreamAppender(PrintStream ps, boolean[] levelSet) {
            return (level, sb, throwable) -> appendToPrintStream(level, sb, throwable, ps,
                    levelSet);
        }

        private static LogAppender getFileAppender(String path, String fileName, String filePattern,
                boolean[] levelSet, long maxSize, long totalSize, int maxHistory) {
            FileAppenderContext ctx = new FileAppenderContext();
            ctx.path = new File(path);
            ctx.path.mkdirs();
            ctx.file = new File(ctx.path, fileName);
            ctx.format = FileNameFormatParser.parse(filePattern);
            ctx.levelSet = levelSet;
            ctx.maxSize = maxSize;
            ctx.totalSize = totalSize;
            ctx.maxHistoryCount = maxHistory;
            ctx.currentSize = ctx.file.length();
            setNextCheckTimeAndInterval(ctx);
            resetCurrentHistoryInfo(ctx);
            return (level, sb, throwable) -> appendToFile(level, sb, throwable, ctx);
        }

        private static void appendToPrintStream(int level, MyStringBuilder sb, Throwable throwable,
                PrintStream ps, boolean[] levelSet) {
            if (levelSet[level]) {
                ps.print(sb.toString());
                if (throwable != null)
                    throwable.printStackTrace(ps);
            }
        }

        private static void appendToFile(int level, MyStringBuilder sb, Throwable throwable,
                FileAppenderContext ctx) {
            if (ctx.levelSet[level]) {
                long time = System.currentTimeMillis();
                if (ctx.out == null) {
                    ctx.out = openPrintWriterSave(ctx.file);
                }
                int maybeLength = sb.length() * 3;
                ctx.currentSize += maybeLength;
                boolean hasRefreshSize = false;
                String nextFile = null;
                if (ctx.currentSize > ctx.maxSize) {
                    ctx.currentSize = ctx.file.length() + maybeLength;
                    hasRefreshSize = true;
                    if (ctx.currentSize > ctx.maxSize) {
                        nextFile = getNextFile(ctx.path, ctx.format, new Date(time), false);
                    }
                }
                if (time > ctx.nextCheckTime && nextFile == null) {
                    nextFile = getNextFile(ctx.path, ctx.format, new Date(time), true);
                    ctx.nextCheckTime += ctx.checkInterval;
                }
                if (nextFile != null) {
                    ctx.out.close();
                    File newFile = new File(ctx.path, nextFile);
                    ctx.file.renameTo(newFile);
                    ctx.out = openPrintWriterSave(ctx.file);
                    ctx.currentHistoryCount++;
                    ctx.currentHistorySize += newFile.length();
                }
                ctx.out.write(sb.getLocalChars(), 0, sb.length());
                ctx.out.flush();
                if (throwable != null)
                    throwable.printStackTrace(ctx.out);
                boolean needClean = false;
                if (ctx.currentHistorySize + ctx.currentSize > ctx.totalSize && !hasRefreshSize) {
                    ctx.currentSize = ctx.file.length() + maybeLength;
                    if (ctx.currentHistorySize + ctx.currentSize > ctx.totalSize) {
                        needClean = true;
                    }
                }
                if (ctx.currentHistoryCount > ctx.maxHistoryCount) {
                    needClean = true;
                }
                if (needClean) {
                    cleanLogFiles(ctx.path, ctx.format, ctx.maxHistoryCount,
                            ctx.totalSize - ctx.file.length());
                    resetCurrentHistoryInfo(ctx);
                }
            }
        }

        private static PrintWriter openPrintWriterSave(File file) {
            try {
                return new PrintWriter(new FileOutputStream(file, true));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private static void setNextCheckTimeAndInterval(FileAppenderContext ctx) {
            MyStringBuilder sb = new MyStringBuilder();
            FileNameFormatContext fctx = new FileNameFormatContext();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            long[] intervals = {TimeUnit.MINUTES.toMillis(1), TimeUnit.HOURS.toMillis(1),
                    TimeUnit.DAYS.toMillis(1)};
            int[] zeroFields = {Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY};
            Date nextTime = null;
            long interval = 0;
            for (int i = 0; i < intervals.length; i++) {
                cal.set(zeroFields[i], 0);
                long time = cal.getTimeInMillis();
                interval = intervals[i];
                nextTime = new Date(time + interval);
                Date date = alignDate(ctx.format, fctx, sb, nextTime);
                if (date != null && date.getTime() != time) {
                    break;
                }
            }
            ctx.nextCheckTime = nextTime.getTime();
            ctx.checkInterval = interval;
        }

        private static void resetCurrentHistoryInfo(FileAppenderContext ctx) {
            ctx.currentHistoryCount = 0;
            ctx.currentHistorySize = 0;
            FileNameFormatContext fctx = new FileNameFormatContext();
            for (String file : ctx.path.list()) {
                int result = ctx.format.match(file, 0, fctx);
                if (result == file.length()) {
                    ctx.currentHistoryCount++;
                    ctx.currentHistorySize = new File(ctx.path, file).length();
                }
            }
        }

        private static Date alignDate(FileNameFormat format, FileNameFormatContext fctx,
                MyStringBuilder sb, Date date) {
            sb.clear();
            fctx.date = date;
            format.formatTo(sb, fctx);
            fctx.date = null;
            int result = format.match(sb.toString(), 0, fctx);
            if (result != sb.length())
                throw new IllegalArgumentException();
            return fctx.date;
        }

        private static String getNextFile(File path, FileNameFormat format, Date date,
                boolean isFirst) {
            FileNameFormatContext fctx = new FileNameFormatContext();
            MyStringBuilder nameBuf = new MyStringBuilder();
            date = alignDate(format, fctx, nameBuf, date);
            int maxIndex = -1;
            for (String file : path.list()) {
                int result = format.match(file, 0, fctx);
                if (result == file.length() && Objects.equals(date, fctx.date)
                        && fctx.index > maxIndex) {
                    maxIndex = fctx.index;
                }
            }
            nameBuf.clear();
            if (isFirst && maxIndex != -1) {
                return null;
            }
            if (maxIndex == -1)
                fctx.index = 1;
            else
                fctx.index = maxIndex + 1;
            fctx.date = date;
            format.formatTo(nameBuf, fctx);
            return nameBuf.toString();
        }

        private static void cleanLogFiles(File path, FileNameFormat format, int maxHistoryCount,
                long totalSize) {
            List<FileNameFormatContext> files = new ArrayList<>();
            FileNameFormatContext fctx = new FileNameFormatContext();
            for (String file : path.list()) {
                int result = format.match(file, 0, fctx);
                if (result == file.length()) {
                    fctx.file = file;
                    files.add(fctx);
                    fctx = new FileNameFormatContext();
                }
            }
            if (files.size() > maxHistoryCount) {
                Lists.quickSort(files, null, null, -1, -1);
                int index = files.size() - 1;
                long size = 0;
                for (int toIndex = index - maxHistoryCount + 1; index >= toIndex; index--) {
                    File file = new File(files.get(index).file);
                    size += file.length();
                    if (size > totalSize) {
                        break;
                    }
                }
                for (int i = 0; i <= index; i++) {
                    File file = new File(files.get(i).file);
                    file.delete();
                }
            }
        }

        private static boolean[] parseLevelSet(String levels) {
            boolean[] levelSet = new boolean[Logger.LEVEL_NONE + 1];
            if (Misc.isEmpty(levels)) {
                for (int i = Util.getDefaultLevel(); i < levelSet.length; i++) {
                    levelSet[i] = true;
                }
            } else {
                List<Integer> levelList = Strings.split(levels, ",", Util::getLevel);
                for (int level : levelList) {
                    levelSet[level] = true;
                }
            }
            return levelSet;
        }
    }
}
