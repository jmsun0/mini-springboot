package com.sjm.core.util.core;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class DateFormats {
    private static final Map<String, DateFormats> caches = new ConcurrentHashMap<>();

    public static DateFormats valueOf(String pattern) {
        DateFormats format = caches.get(pattern);
        if (format == null) {
            synchronized (DateFormats.class) {
                if (format == null)
                    format = new DateFormats(pattern);
            }
        }
        return format;
    }

    public static final DateFormats HHmm = new DateFormats("HH:mm"); //
    public static final DateFormats HHmmss = new DateFormats("HH:mm:ss"); //
    public static final DateFormats yyyyMMddHHmmss = new DateFormats("yyyy/MM/dd HH:mm:ss"); //
    public static final DateFormats yyyy_MM_ddHHmmss = new DateFormats("yyyy-MM-dd HH:mm:ss"); // 常用
    public static final DateFormats fyyyy_MM_dd_HH_mm_ss = new DateFormats("yyyy_MM_dd_HH_mm_ss"); //
    public static final DateFormats yyyy_MM_dd = new DateFormats("yyyy-MM-dd"); //
    public static final DateFormats yyyyMMdd = new DateFormats("yyyyMMdd"); //
    public static final DateFormats yyyyMMddC = new DateFormats("yyyy年MM月dd日"); //
    public static final DateFormats yyyy_MM_ddHHmmssSSS =
            new DateFormats("yyyy-MM-dd HH:mm:ss.SSS"); //
    public static final DateFormats yyyy_MM_ddTHHmmssSSSXXX =
            new DateFormats("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); // ElasticSearch
    public static final DateFormats yyyy_MM_ddTHHmmssSSSSSSSSXXX =
            new DateFormats("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"); // MySQL error.log

    private final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<>();
    private final String pattern;

    private DateFormats(String pattern) {
        this.pattern = pattern;
        caches.put(pattern, this);
    }

    public String getPattern() {
        return pattern;
    }

    public SimpleDateFormat get() {
        SimpleDateFormat fmt = sdf.get();
        if (fmt == null)
            sdf.set(fmt = new SimpleDateFormat(pattern));
        return fmt;
    }

    public String format(Date time, TimeZone zone) {
        if (time == null)
            return "";
        try {
            SimpleDateFormat format = get();
            if (zone != null) {
                TimeZone oldZone = format.getTimeZone();
                format.setTimeZone(zone);
                String result = format.format(time);
                format.setTimeZone(oldZone);
                return result;
            } else
                return format.format(time);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String format(Date time) {
        return format(time, null);
    }

    public Date parse(String time, TimeZone zone, ParsePosition pos) {
        if (time == null || time.isEmpty())
            return null;
        try {
            SimpleDateFormat format = get();
            if (zone != null) {
                TimeZone oldZone = format.getTimeZone();
                format.setTimeZone(zone);
                Date result = format.parse(time, pos);
                format.setTimeZone(oldZone);
                return result;
            } else
                return format.parse(time, pos);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date parse(String time, TimeZone zone) {
        return parse(time, zone, new ParsePosition(0));
    }

    public Date parse(String time) {
        return parse(time, null);
    }
}
