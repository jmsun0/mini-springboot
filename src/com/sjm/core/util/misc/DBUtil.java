package com.sjm.core.util.misc;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sjm.core.util.core.Converter;
import com.sjm.core.util.core.IOUtil;
import com.sjm.core.util.core.MyStringBuilder;
import com.sjm.core.util.core.Reflection;
import com.sjm.core.util.core.Reflection.Setter;
import com.sjm.core.util.core.Strings;

public class DBUtil {
    public static void main(String[] args) throws Exception {
        Connection conn = getMySQLConn("192.168.210.141", 3306, "hotdb_cloud_management_config",
                null, "hotdb_cloud", "hotdb_cloud@hotpu.cn");
        Shell.run(getShell(conn), System.in, System.out, System.err, null);
    }

    public static void loadDriver(String driver) throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
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

    public static boolean execute(Connection conn, String sql, Object... params)
            throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(sql);) {
            setPreparedParams(pst, params);
            return pst.execute();
        }
    }

    public static int update(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(sql);) {
            setPreparedParams(pst, params);
            return pst.executeUpdate();
        }
    }

    public static ResultCollector query(Connection conn, String sql, Object... params)
            throws SQLException {
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            setPreparedParams(pst, params);
            return new ResultCollector(pst.executeQuery());
        } catch (Throwable e) {
            pst.close();
            throw e;
        }
    }

    public static void setPreparedParams(PreparedStatement pst, Object... params)
            throws SQLException {
        for (int i = 0, len = params.length; i < len; i++)
            pst.setObject(i + 1, params[i]);
    }

    public interface RowMapper<T, MD> {
        public T mapRow(ResultSet rs, MD meta, int rowNum) throws SQLException;

        public MD prepareMetaData(ResultSet rs) throws SQLException;
    }

    public static class RowMappers {
        public static abstract class ResultSetMetaDataRowMapper<T>
                implements RowMapper<T, ResultSetMetaData> {
            @Override
            public ResultSetMetaData prepareMetaData(ResultSet rs) throws SQLException {
                return rs.getMetaData();
            }
        }
        public static abstract class VoidRowMapper<T> implements RowMapper<T, Void> {
            @Override
            public Void prepareMetaData(ResultSet rs) throws SQLException {
                return null;
            }
        }

        public static <T> RowMapper<Object[], ?> toArray(Class<?>[] types) {
            return new VoidRowMapper<Object[]>() {
                @Override
                public Object[] mapRow(ResultSet rs, Void meta, int rowNum) throws SQLException {
                    Object[] arr = new Object[types.length];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = rs.getObject(i + 1, types[i]);
                    }
                    return arr;
                }
            };
        }

        public static <T> RowMapper<Object[], ?> toArray() {
            return new ResultSetMetaDataRowMapper<Object[]>() {
                @Override
                public Object[] mapRow(ResultSet rs, ResultSetMetaData meta, int rowNum)
                        throws SQLException {
                    Object[] arr = new Object[meta.getColumnCount()];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = rs.getObject(i + 1);
                    }
                    return arr;
                }
            };
        }

        public static <T> RowMapper<Map<String, Object>, ?> toMap(
                Supplier<Map<String, Object>> supplier) {
            return new ResultSetMetaDataRowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, ResultSetMetaData meta, int rowNum)
                        throws SQLException {
                    Map<String, Object> map = supplier.get();
                    for (int i = 0, len = meta.getColumnCount(); i < len; i++) {
                        map.put(meta.getColumnLabel(i + 1), rs.getObject(i + 1));
                    }
                    return map;
                }
            };
        }

        public static class ToObjectMetaData<T> {
            public Function<Object, T> baseConverter;
            public Supplier<T> supplier;
            public ToObjectColumnMetaData<T>[] columns;

            public ToObjectMetaData(Function<Object, T> baseConverter, Supplier<T> supplier,
                    ToObjectColumnMetaData<T>[] columns) {
                this.baseConverter = baseConverter;
                this.supplier = supplier;
                this.columns = columns;
            }
        }
        public static class ToObjectColumnMetaData<T> {
            public int index;
            public Reflection.Setter setter;
            public Function<Object, T> converter;

            public ToObjectColumnMetaData(int index, Setter setter, Function<Object, T> converter) {
                this.index = index;
                this.setter = setter;
                this.converter = converter;
            }
        }

        public static <T> RowMapper<T, ?> toObject(Class<T> clazz) {
            Map<String, Reflection.SetterInfo> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            map.putAll(Reflection.INSTANCE.getSettersMap(clazz));
            Function<Object, T> baseConverter = Converter.INSTANCE.getBaseConverter(clazz);
            Supplier<T> supplier = baseConverter != null ? null : () -> {
                try {
                    return clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            return new RowMapper<T, ToObjectMetaData<T>>() {
                @SuppressWarnings("unchecked")
                @Override
                public ToObjectMetaData<T> prepareMetaData(ResultSet rs) throws SQLException {
                    ToObjectColumnMetaData<T>[] columns = null;
                    if (baseConverter == null) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        List<ToObjectColumnMetaData<T>> columnList = new ArrayList<>();
                        for (int i = 0, len = rsmd.getColumnCount(); i < len; i++) {
                            Reflection.SetterInfo setter = map.get(rsmd.getColumnLabel(i + 1));
                            if (setter != null) {
                                columnList.add(new ToObjectColumnMetaData<T>(i + 1, setter.setter,
                                        Converter.INSTANCE.getConverter(setter.type)));
                            }
                        }
                        columns = columnList.toArray(new ToObjectColumnMetaData[columnList.size()]);
                    }
                    return new ToObjectMetaData<T>(baseConverter, supplier, columns);
                }

                @Override
                public T mapRow(ResultSet rs, ToObjectMetaData<T> meta, int rowNum)
                        throws SQLException {
                    try {
                        if (meta.baseConverter != null) {
                            return meta.baseConverter.apply(rs.getObject(1));
                        } else {
                            T obj = meta.supplier.get();
                            for (ToObjectColumnMetaData<T> column : meta.columns) {
                                column.setter.set(obj,
                                        column.converter.apply(rs.getObject(column.index)));
                            }
                            return obj;
                        }
                    } catch (SQLException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SQLException(e);
                    }
                }
            };
        }
    }

    public static class ResultCollector implements AutoCloseable {
        private ResultSet rs;

        public ResultCollector(ResultSet rs) throws SQLException {
            this.rs = rs;
        }

        @Override
        public void close() throws SQLException {
            rs.close();
        }

        public <T, C extends Collection<T>, MD> C selectCollection(RowMapper<T, MD> rowMapper,
                C col) throws SQLException {
            try (ResultSet rs = this.rs) {
                int rowNum = 0;
                MD meta = rowMapper.prepareMetaData(rs);
                while (rs.next())
                    col.add(rowMapper.mapRow(rs, meta, rowNum++));
                return col;
            }
        }

        public <T, MD> ArrayList<T> selectArrayList(RowMapper<T, MD> rowMapper)
                throws SQLException {
            return selectCollection(rowMapper, new ArrayList<>());
        }

        public <T, MD> T selectOne(RowMapper<T, MD> rowMapper) throws SQLException {
            try (ResultSet rs = this.rs) {
                MD meta = rowMapper.prepareMetaData(rs);
                return rs.next() ? rowMapper.mapRow(rs, meta, 0) : null;
            }
        }

        public <K, V, M extends Map<K, V>, MD> M selectMap(RowMapper<V, MD> rowMapper,
                Function<V, K> keyMapper, M map) throws SQLException {
            try (ResultSet rs = this.rs) {
                int rowNum = 0;
                MD meta = rowMapper.prepareMetaData(rs);
                while (rs.next()) {
                    V value = rowMapper.mapRow(rs, meta, rowNum++);
                    map.put(keyMapper.apply(value), value);
                }
                return map;
            }
        }

        public <K, V, MD> HashMap<K, V> selectHashMap(RowMapper<V, MD> rowMapper,
                Function<V, K> keyMapper) throws SQLException {
            return selectMap(rowMapper, keyMapper, new HashMap<>());
        }

        public int selectCount() throws SQLException {
            try (ResultSet rs = this.rs) {
                int count = 0;
                while (rs.next())
                    count++;
                return count;
            }
        }

        public <T, MD> void foreach(RowMapper<T, MD> rowMapper, Consumer<T> consumer)
                throws SQLException {
            try (ResultSet rs = this.rs) {
                int rowNum = 0;
                MD meta = rowMapper.prepareMetaData(rs);
                while (rs.next())
                    consumer.accept(rowMapper.mapRow(rs, meta, rowNum++));
            }
        }

        public void print(PrintStream ps) throws SQLException {
            try (ResultSet rs = this.rs) {
                DBUtil.print(rs, ps);
            }
        }

        public void print() throws SQLException {
            print(System.out);
        }
    }

    public static void print(ResultSet rs, PrintStream ps) throws SQLException {
        List<String[]> list = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int col = rsmd.getColumnCount();
        String[] cols = new String[col];
        for (int i = 0; i < col; i++)
            cols[i] = rsmd.getColumnName(i + 1);
        list.add(cols);
        while (rs.next()) {
            String[] strs = new String[col];
            for (int i = 0; i < col; i++) {
                String str = rs.getString(i + 1);
                if (str == null)
                    str = "NULL";
                strs[i] = str;
            }
            list.add(strs);
        }
        if (list.size() > 1) {
            int[] lens = new int[list.get(0).length];
            MyStringBuilder sb = new MyStringBuilder();
            for (String[] arr : list) {
                for (int i = 0; i < arr.length; i++) {
                    lens[i] = Math.max(arr[i].length(), lens[i]);
                }
            }
            printSeparator(sb, lens);
            printLine(sb, list.get(0), lens);
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

    private static void printSeparator(MyStringBuilder sb, int[] lens) {
        for (int i = 0; i < lens.length; i++)
            sb.append('+').append('-', lens[i] + 2);
        sb.append('+').append('\n');
    }

    private static void printLine(MyStringBuilder sb, String[] cols, int[] lens) {
        for (int i = 0; i < lens.length; i++) {
            String col = cols[i];
            sb.append('|').append(' ').append(col).append(' ', lens[i] - col.length() + 1);
        }
        sb.append('|').append('\n');
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
                    int j = Strings.indexOfIgnoreQuotation(script, ';', i, -1);
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
                                        print(rs, out);
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
