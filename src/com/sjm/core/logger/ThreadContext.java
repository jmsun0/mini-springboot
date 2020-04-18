package com.sjm.core.logger;

public class ThreadContext {
    public static void put(String key, String value) {
        LoggerFactory.getLogContext().attributes.put(key, value);
    }

    public static void clearMap() {
        LoggerFactory.getLogContext().attributes.clear();
    }
}
