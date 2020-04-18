package com.sjm.core.logger;

public class Logger {
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_TRACE = 1;
    public static final int LEVEL_DEBUG = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_WARN = 4;
    public static final int LEVEL_ERROR = 5;
    public static final int LEVEL_NONE = 6;

    private Class<?> clazz;
    private int minlevel;
    private LoggerFactory.LogFormat logFormat;
    private LoggerFactory.LogAppender[] appenders;

    Logger(Class<?> clazz, int minlevel, LoggerFactory.LogFormat logFormat,
            LoggerFactory.LogAppender[] appenders) {
        this.clazz = clazz;
        this.minlevel = minlevel;
        this.logFormat = logFormat;
        this.appenders = appenders;
    }

    public String getName() {
        return clazz.getName();
    }

    public boolean isTraceEnabled() {
        return minlevel <= LEVEL_TRACE;
    }

    public void trace(String msg) {
        log(LEVEL_TRACE, msg);
    }

    public void trace(String format, Object arg) {
        log(LEVEL_TRACE, format, arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
        log(LEVEL_TRACE, format, arg1, arg2);
    }

    public void trace(String format, Object... arguments) {
        log(LEVEL_TRACE, format, arguments);
    }

    public void trace(String msg, Throwable t) {
        log(LEVEL_TRACE, msg, t);
    }

    public boolean isDebugEnabled() {
        return minlevel <= LEVEL_DEBUG;
    }

    public void debug(String msg) {
        log(LEVEL_DEBUG, msg);
    }

    public void debug(String format, Object arg) {
        log(LEVEL_DEBUG, format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        log(LEVEL_DEBUG, format, arg1, arg2);
    }

    public void debug(String format, Object... arguments) {
        log(LEVEL_DEBUG, format, arguments);
    }

    public void debug(String msg, Throwable t) {
        log(LEVEL_DEBUG, msg, t);
    }

    public boolean isInfoEnabled() {
        return minlevel <= LEVEL_INFO;
    }

    public void info(String msg) {
        log(LEVEL_INFO, msg);
    }

    public void info(String format, Object arg) {
        log(LEVEL_INFO, format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        log(LEVEL_INFO, format, arg1, arg2);
    }

    public void info(String format, Object... arguments) {
        log(LEVEL_INFO, format, arguments);
    }

    public void info(String msg, Throwable t) {
        log(LEVEL_INFO, msg, t);
    }

    public boolean isWarnEnabled() {
        return minlevel <= LEVEL_WARN;
    }

    public void warn(String msg) {
        log(LEVEL_WARN, msg);
    }

    public void warn(String format, Object arg) {
        log(LEVEL_WARN, format, arg);
    }

    public void warn(String format, Object arg1, Object arg2) {
        log(LEVEL_WARN, format, arg1, arg2);
    }

    public void warn(String format, Object... arguments) {
        log(LEVEL_WARN, format, arguments);
    }

    public void warn(String msg, Throwable t) {
        log(LEVEL_WARN, msg, t);
    }

    public boolean isErrorEnabled() {
        return minlevel <= LEVEL_ERROR;
    }

    public void error(String msg) {
        log(LEVEL_ERROR, msg);
    }

    public void error(String format, Object arg) {
        log(LEVEL_ERROR, format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        log(LEVEL_ERROR, format, arg1, arg2);
    }

    public void error(String format, Object... arguments) {
        log(LEVEL_ERROR, format, arguments);
    }

    public void error(String msg, Throwable t) {
        log(LEVEL_ERROR, msg, t);
    }

    private void log(int level, String msg) {
        if (level >= minlevel) {
            LoggerFactory.LogContext ctx = LoggerFactory.getLogContext();
            ctx.level = level;
            ctx.format = msg;
            ctx.argsCount = 0;
            ctx.throwable = null;
            log(ctx);
        }
    }

    private void log(int level, String format, Object arg) {
        if (level >= minlevel) {
            LoggerFactory.LogContext ctx = LoggerFactory.getLogContext();
            ctx.level = level;
            ctx.format = format;
            ctx.argsCount = 1;
            ctx.args[0] = arg;
            ctx.throwable = null;
            log(ctx);
        }
    }

    private void log(int level, String format, Object arg1, Object arg2) {
        if (level >= minlevel) {
            LoggerFactory.LogContext ctx = LoggerFactory.getLogContext();
            ctx.level = level;
            ctx.format = format;
            ctx.argsCount = 2;
            ctx.args[0] = arg1;
            ctx.args[1] = arg2;
            ctx.throwable = null;
            log(ctx);
        }
    }

    private void log(int level, String format, Object... arguments) {
        if (level >= minlevel) {
            LoggerFactory.LogContext ctx = LoggerFactory.getLogContext();
            ctx.level = level;
            ctx.format = format;
            ctx.argsCount = arguments.length;
            for (int i = 0; i < arguments.length; i++)
                ctx.args[i] = arguments[i];
            ctx.throwable = null;
            log(ctx);
        }
    }

    private void log(int level, String msg, Throwable t) {
        if (level >= minlevel) {
            LoggerFactory.LogContext ctx = LoggerFactory.getLogContext();
            ctx.level = level;
            ctx.format = msg;
            ctx.argsCount = 0;
            ctx.throwable = t;
            log(ctx);
        }
    }

    private void log(LoggerFactory.LogContext ctx) {
        ctx.clazz = clazz;
        ctx.logFormat = logFormat;
        ctx.thread = Thread.currentThread();
        if (logFormat.needTime)
            ctx.time = System.currentTimeMillis();
        if (logFormat.needStack)
            ctx.stack = new Exception().getStackTrace()[3];
        ctx.sb.clear();
        for (LoggerFactory.LogPattern message : ctx.logFormat.patterns)
            message.append(ctx);
        for (LoggerFactory.LogAppender appender : appenders)
            appender.append(ctx.level, ctx.sb, ctx.throwable);
        ctx.thread = null;
        ctx.stack = null;
        ctx.throwable = null;
        for (int i = 0; i < ctx.argsCount; i++)
            ctx.args[i] = null;
    }
}
