package com.sjm.core.util.core;

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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

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
        public static abstract class SimpleRowMapper<T> implements RowMapper<T, ResultSetMetaData> {
            @Override
            public ResultSetMetaData prepareMetaData(ResultSet rs) throws SQLException {
                return rs.getMetaData();
            }
        }

        public static <T> RowMapper<T[], ResultSetMetaData> toArray(IntFunction<T[]> allocator,
                Function<Object, T> func) {
            return new SimpleRowMapper<T[]>() {
                @Override
                public T[] mapRow(ResultSet rs, ResultSetMetaData meta, int rowNum)
                        throws SQLException {
                    // TODO Auto-generated method stub
                    return null;
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
                                        new ResultCollector(rs).print(out);
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
