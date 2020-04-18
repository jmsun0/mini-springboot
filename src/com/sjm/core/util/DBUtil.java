package com.sjm.core.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sjm.core.json.JSONArray;
import com.sjm.core.json.JSONObject;

public class DBUtil {
    public static void main(String[] args) throws Exception {
        // Connection conn = getMySQLConn("127.0.0.1", 3306, "test", null, "root", "root");
        Connection conn = getMySQLConn("192.168.200.89", 3306, "hotdb_cloud_management_config",
                null, "hotdb_cloud", "hotdb_cloud@hotpu.cn");
        Shell.run(getShell(conn), System.in, System.out, System.err, null);
    }

    public static void loadDriver(String driver) throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException(driver + "驱动加载失败");
        }
    }

    public static Connection getConn(String driver, String url, String name, String psw)
            throws SQLException {
        loadDriver(driver);
        return DriverManager.getConnection(url, name, psw);
    }

    public static Connection getConn(String driver, String url) throws SQLException {
        loadDriver(driver);
        return DriverManager.getConnection(url);
    }

    public static Connection getMySQLConn(String host, int port, String db, String args,
            String name, String psw) throws SQLException {
        String url = "jdbc:mysql://%s:%d/%s?%s";
        final String defaultArgs =
                "useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        url = String.format(url, host, port, db, args == null ? defaultArgs : args);
        return getConn("com.mysql.cj.jdbc.Driver", url, name, psw);
    }

    public static Connection getSQLiteConn(String path) throws SQLException {
        return getConn("org.sqlite.JDBC", "jdbc:sqlite:" + path);
    }

    public static int execute(PreparedStatement pst, Object... params) throws SQLException {
        setParam(pst, params);
        return pst.executeUpdate();
    }

    public static int execute(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            return execute(pst, params);
        } finally {
            Misc.close(pst);
        }
    }

    public static ResultCollector query(PreparedStatement pst, Object... params)
            throws SQLException {
        setParam(pst, params);
        return createResultCollector(pst.executeQuery());
    }

    public static ResultCollector query(Connection conn, String sql, Object... params)
            throws SQLException {
        PreparedStatement pst = conn.prepareStatement(sql);
        setParam(pst, params);
        return createResultCollector(pst.executeQuery(), pst);
    }

    public static String getDatabase(Connection conn) throws SQLException {
        return query(conn, "select database()").toObject(ToString);
    }

    public static Integer getLastInsertId(Connection conn) throws SQLException {
        return query(conn, "select last_insert_id()").toObject(ToInteger);
    }

    public static Long getMaxAllowedPacket(Connection conn) throws SQLException {
        return query(conn, "show variables like 'max_allowed_packet'")
                .toObject(ToObject(2, Long.class));
    }

    public static List<String> getDatabases(Connection conn) throws SQLException {
        return createResultCollector(conn.getMetaData().getCatalogs()).toArrayList(ToString);
    }

    private static void appendHeader(MyStringBuilder sb, String table, String[] columns) {
        sb.append("insert into `").append(table).append("`(");
        for (String column : columns)
            sb.append('`').append(column).append('`').append(',');
        sb.deleteEnd().append(") values ");
    }

    private static void appendData(MyStringBuilder sb, Object[] data) {
        sb.append('(');
        for (Object obj : data) {
            if (obj instanceof Number)
                sb.append(obj).append(',');
            else
                sb.append('"').appendEscape(obj.toString(), null, -1, -1).append('"').append(',');
        }
        sb.deleteEnd().append(')').append(',');
    }

    private static void appendTail(MyStringBuilder sb, String[] columns) {
        sb.append(" on duplicate key update");
        for (String column : columns)
            sb.append("`").append(column).append("` = values(`").append(column).append("`),");
        sb.deleteEnd();
    }

    public static long bulkInsert(Connection conn, String table, String[] columns,
            Iterator<Object[]> datas) throws SQLException {
        Long maxPacket = getMaxAllowedPacket(conn);
        long max = maxPacket / 4;
        Statement stm = conn.createStatement();
        long count = 0;
        try {
            MyStringBuilder sb = new MyStringBuilder();
            appendHeader(sb, table, columns);
            while (datas.hasNext()) {
                Object[] data = datas.next();
                int len1 = sb.length();
                appendData(sb, data);
                count++;
                int len2 = sb.length();
                if (len2 > max) {
                    sb.deleteEnd(len2 - len1);
                    sb.deleteEnd();
                    appendTail(sb, columns);
                    stm.execute(sb.toString());
                    sb.clear();
                    appendHeader(sb, table, columns);
                    appendData(sb, data);
                }
            }
            sb.deleteEnd();
            appendTail(sb, columns);
            stm.execute(sb.toString());
        } finally {
            Misc.close(stm);
        }
        return count;
    }

    public static void setParam(PreparedStatement pst, Object... params) throws SQLException {
        for (int i = 0, len = params.length; i < len; i++)
            pst.setObject(i + 1, params[i]);
    }

    public interface ResultExtractor<T> {
        public T extract(ResultSet rs, ResultSetMetaData rsmd) throws SQLException;
    }

    public static abstract class AbstractResultExtractor<T> implements ResultExtractor<T> {
        public abstract T create(int column);

        public abstract void set(int index, String name, T result, Object value);

        @Override
        public T extract(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
            int column = rsmd.getColumnCount();
            T result = create(column);
            for (int i = 0; i < column; i++)
                set(i, rsmd.getColumnName(i + 1), result, rs.getObject(i + 1));
            return result;
        }
    }

    public static ResultExtractor<JSONObject> ToJSONObject =
            new AbstractResultExtractor<JSONObject>() {
                @Override
                public JSONObject create(int column) {
                    return new JSONObject();
                }

                @Override
                public void set(int index, String name, JSONObject result, Object value) {
                    result.put(name, value);
                }
            };
    public static ResultExtractor<Object[]> ToObjectArray =
            new AbstractResultExtractor<Object[]>() {
                @Override
                public Object[] create(int column) {
                    return new Object[column];
                }

                @Override
                public void set(int index, String name, Object[] result, Object value) {
                    result[index] = value;
                }
            };
    public static final ResultExtractor<String[]> ToStringArray =
            new AbstractResultExtractor<String[]>() {
                @Override
                public String[] create(int column) {
                    return new String[column];
                }

                @Override
                public void set(int index, String name, String[] result, Object value) {
                    result[index] = String.valueOf(value);
                }
            };

    private static HashMap<Class<?>, ResultExtractor<?>> beanExtractorMap = new HashMap<>();

    public static <T> ResultExtractor<T> ToBean(final Class<T> clazz) {
        @SuppressWarnings("unchecked")
        ResultExtractor<T> re = (ResultExtractor<T>) beanExtractorMap.get(clazz);
        if (re == null) {
            final Reflection.IClass cls = Reflection.forClass(clazz);
            Collection<Reflection.Setter> setters = cls.getSetterMap().values();
            Map<String, Reflection.Setter> map = new TreeMap<>(Misc.STRING_NOCASE_COMPARATOR);
            Maps.putAll(map, setters, Reflection.Setter::getName);
            final Reflection.Creator creator = cls.getCreator();
            re = new AbstractResultExtractor<T>() {
                @SuppressWarnings("unchecked")
                @Override
                public T create(int column) {
                    return (T) creator.newInstance();
                }

                @Override
                public void set(int index, String name, T result, Object value) {
                    Reflection.Setter setter = map.get(name);
                    if (value != null && setter != null)
                        setter.set(result, Converters.convert(value, setter.getIType().getClazz()));
                }
            };
            beanExtractorMap.put(clazz, re);
        }
        return re;
    }

    private static HashMap<Class<?>, ResultExtractor<?>> arrayExtractorMap = new HashMap<>();

    public static <T> ResultExtractor<T> ToArray(final Class<T> arrayClazz) {
        @SuppressWarnings("unchecked")
        ResultExtractor<T> re = (ResultExtractor<T>) arrayExtractorMap.get(arrayClazz);
        if (re == null) {
            if (!arrayClazz.isArray())
                throw new IllegalArgumentException();
            Class<?> cType = arrayClazz.getComponentType();
            final ArrayController<Object, T> ctr = ArrayController.valueOf(cType);
            final Converter<?, Object> conv = Converters.valueOf(cType);
            re = new AbstractResultExtractor<T>() {
                @Override
                public T create(int column) {
                    return ctr.newInstance(column);
                }

                @Override
                public void set(int index, String name, T result, Object value) {
                    ctr.set(result, index, conv.convert(value));
                }
            };
            arrayExtractorMap.put(arrayClazz, re);
        }
        return re;
    }

    public static ResultExtractor<Object> ToObject = new ResultExtractor<Object>() {
        @Override
        public Object extract(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
            return rs.getObject(1);
        }
    };

    public static <T> ResultExtractor<T> ToObject(final int index,
            final Converter<? extends T, Object> conv) {
        return new ResultExtractor<T>() {
            @Override
            public T extract(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
                return conv.convert(rs.getObject(index));
            }
        };
    }

    public static <T> ResultExtractor<T> ToObject(int index, Class<T> clazz) {
        return ToObject(index, Converters.valueOf(clazz));
    }

    public static <T> ResultExtractor<T> ToObject(Class<T> clazz) {
        return ToObject(1, clazz);
    }

    public static ResultExtractor<Integer> ToInteger = ToObject(Integer.class);
    public static ResultExtractor<Long> ToLong = ToObject(Long.class);
    public static ResultExtractor<String> ToString = ToObject(String.class);

    public static class ResultCollector implements AutoCloseable {
        private ResultSet rs;
        private ResultSetMetaData rsmd;
        private AutoCloseable pst;

        ResultCollector(ResultSet rs, AutoCloseable pst) throws SQLException {
            this.rs = rs;
            this.rsmd = rs.getMetaData();
            this.pst = pst;
        }

        public boolean next() throws SQLException {
            return rs.next();
        }

        public void close() throws SQLException {
            Misc.close(rs);
            Misc.close(pst);
        }

        public <T> T get(ResultExtractor<T> re) throws SQLException {
            return re.extract(rs, rsmd);
        }

        public <T> void addAll(Collection<? super T> col, ResultExtractor<T> re)
                throws SQLException {
            try {
                while (next())
                    col.add(get(re));
            } finally {
                close();
            }
        }

        public <K, T> void putAll(Map<K, ? super T> map, ResultExtractor<T> re,
                Converter<? extends K, ? super T> conv) throws SQLException {
            try {
                while (next()) {
                    T result = get(re);
                    map.put(conv.convert(result), result);
                }
            } finally {
                close();
            }
        }

        public int size() throws SQLException {
            int size = 0;
            try {
                while (next())
                    size++;
            } finally {
                close();
            }
            return size;
        }

        public <T> T toObject(ResultExtractor<T> re) throws SQLException {
            try {
                return next() ? get(re) : null;
            } finally {
                close();
            }
        }

        public <T> ArrayList<T> toArrayList(ResultExtractor<T> re) throws SQLException {
            ArrayList<T> list = new ArrayList<>();
            addAll(list, re);
            return list;
        }

        public <T> LinkedList<T> toLinkedList(ResultExtractor<T> re) throws SQLException {
            LinkedList<T> list = new LinkedList<>();
            addAll(list, re);
            return list;
        }

        public JSONArray toJSONArray() throws SQLException {
            JSONArray array = new JSONArray();
            addAll(array, ToJSONObject);
            return array;
        }

        public <T> List<T> toBeanList(Class<T> clazz) throws SQLException {
            return toArrayList(ToBean(clazz));
        }

        public List<Object[]> toObjectArrayList() throws SQLException {
            return toArrayList(ToObjectArray);
        }

        public List<Object> toObjectList() throws SQLException {
            return toArrayList(ToObject);
        }

        public <T> List<T> toArrayList(Class<T> arrayClazz) throws SQLException {
            return toArrayList(ToArray(arrayClazz));
        }

        public <K, T> HashMap<K, T> toHashMap(ResultExtractor<T> re,
                Converter<? extends K, ? super T> conv) throws SQLException {
            HashMap<K, T> map = new HashMap<>();
            putAll(map, re, conv);
            return map;
        }

        private void printSeparator(MyStringBuilder sb, int[] lens) {
            for (int i = 0; i < lens.length; i++)
                sb.append('+').append('-', lens[i] + 2);
            sb.append('+').append('\n');
        }

        private void printLine(MyStringBuilder sb, String[] cols, int[] lens) {
            for (int i = 0; i < lens.length; i++) {
                String col = cols[i];
                sb.append('|').append(' ').append(col).append(' ', lens[i] - col.length() + 1);
            }
            sb.append('|').append('\n');
        }

        private ResultExtractor<String[]> ToStringArray = new AbstractResultExtractor<String[]>() {
            @Override
            public String[] create(int column) {
                return new String[column];
            }

            @Override
            public void set(int index, String name, String[] result, Object value) {
                if (value instanceof Date)
                    result[index] = DateFormats.yyyy_MM_ddTHHmmssSSSXXX.format((Date) value);
                else
                    result[index] = String.valueOf(value);
            }
        };

        public void print(PrintStream ps) throws SQLException {
            MyStringBuilder sb = new MyStringBuilder();
            int col = rsmd.getColumnCount();
            int[] lens = new int[col];
            List<String[]> list = new ArrayList<>();
            String[] cols = new String[col];
            for (int i = 0; i < col; i++)
                cols[i] = rsmd.getColumnName(i + 1);
            list.add(cols);
            addAll(list, ToStringArray);
            if (list.size() > 1) {
                for (String[] arr : list) {
                    for (int i = 0; i < col; i++) {
                        lens[i] = Math.max(arr[i].length(), lens[i]);
                    }
                }
                printSeparator(sb, lens);
                printLine(sb, cols, lens);
                printSeparator(sb, lens);
                for (int i = 1; i < list.size(); i++)
                    printLine(sb, list.get(i), lens);
                printSeparator(sb, lens);
                sb.append((list.size() - 1) + " rows in set\n\n");
                ps.print(sb.toString());
            } else {
                ps.print("Empty set\n\n");
            }
        }

        public void print() throws SQLException {
            print(System.out);
        }
    }

    public static ResultCollector createResultCollector(ResultSet rs, AutoCloseable pst)
            throws SQLException {
        return new ResultCollector(rs, pst);
    }

    public static ResultCollector createResultCollector(ResultSet rs) throws SQLException {
        return createResultCollector(rs, null);
    }

    public static Shell getShell(final Connection conn) {
        return new Shell() {
            private PrintWriter err;
            private PrintStream out;
            private MyStringBuilder sb = new MyStringBuilder();

            @Override
            public void start() throws IOException {
                out.print("mysql> ");
                out.flush();
            }

            @Override
            public void setWriter(Writer out) {
                this.out = new PrintStream(IOUtil.toOutputStream(out, null));
            }

            @Override
            public void setErrorWriter(Writer err) {
                this.err = new PrintWriter(err);
            }

            @Override
            public void execute(String script) {
                if (script.isEmpty()) {
                    if (sb.isEmpty())
                        out.print("mysql> ");
                    else
                        out.print("    -> ");
                    out.flush();
                    return;
                }
                if (script.equalsIgnoreCase("exit"))
                    System.exit(0);
                for (int i = 0; i < script.length();) {
                    int j = Strings.indexOfIgnoreQuotation(script, null, ';', i, -1);
                    if (j == -1) {
                        sb.append(script);
                        out.print("    -> ");
                        out.flush();
                        return;
                    }
                    sb.append(script, i, j - i);
                    if (sb.isEmpty()) {
                        out.println("1065 - Query was empty");
                        out.flush();
                    } else {
                        String sql = sb.toString().trim();
                        sb.clear();
                        try {
                            try (Statement stmt = conn.createStatement()) {
                                if (stmt.execute(sql)) {
                                    do {
                                        ResultSet rs = stmt.getResultSet();
                                        DBUtil.createResultCollector(rs).print(out);
                                    } while (stmt.getMoreResults());
                                } else {
                                    int n = stmt.getUpdateCount();
                                    out.println("Query OK, " + n + " rows affected");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace(err);
                            err.flush();
                            Misc.sleep(300);
                        }
                    }
                    i = j + 1;
                }
                out.print("mysql> ");
                out.flush();
            }
        };
    }
}
