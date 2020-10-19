package com.sjm.core.mybatis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Mybatis {

    public enum StatementType {
        STATEMENT, PREPARED, CALLABLE
    }

    public interface ResultMapper {
        public Object mapResult(ResultSet resultSet);
    }

    public static class BoundSql {
        public String sql;
        public List<Object> args;
    }

    public interface SqlSource {
        public BoundSql getBoundSql(Object parameter);
    }

    public interface KeyGenerator {
        public void processAfter(Statement stmt, Object parameter) throws SQLException;
    }

    public interface Transaction extends AutoCloseable {
        public Connection getConnection() throws SQLException;

        public void commit() throws SQLException;

        public void rollback() throws SQLException;

        public void close() throws SQLException;

        public int getTimeout() throws SQLException;
    }

    public static class MappedStatement {
        public String id;
        public int timeout;
        public int fetchSize;
        public StatementType statementType;
        public SqlSource sqlSource;
        public List<ResultMapper> resultMappers;
        public KeyGenerator keyGenerator;
        public String[] keyColumns;
    }

    public static Object execute(Transaction transaction, MappedStatement statement,
            Object parameter) throws SQLException {
        BoundSql boundSql = statement.sqlSource.getBoundSql(parameter);
        Connection conn = transaction.getConnection();
        try (Statement stmt = createStatement(conn, boundSql, statement);) {
            int timeout = getMinTimeout(statement.timeout, transaction.getTimeout());
            if (timeout > 0)
                stmt.setQueryTimeout(timeout);
            int fetchSize = statement.fetchSize;
            if (fetchSize > 0)
                stmt.setFetchSize(fetchSize);
            List<Object> args = boundSql.args;
            boolean hasResultSet;
            if (stmt instanceof PreparedStatement) {
                @SuppressWarnings("resource")
                PreparedStatement pst = (PreparedStatement) stmt;
                if (args != null && args.size() > 0) {
                    for (int i = 0; i < args.size(); i++)
                        pst.setObject(i + 1, args.get(i));
                }
                hasResultSet = pst.execute();
            } else {
                hasResultSet = stmt.execute(boundSql.sql);
            }
            Object result;
            List<ResultMapper> resultMappers = statement.resultMappers;
            if (resultMappers != null) {
                if (hasResultSet) {
                    try (ResultSet resultSet = stmt.getResultSet();) {
                        result = resultMappers.get(0).mapResult(resultSet);
                    }
                } else {
                    result = stmt.getUpdateCount();
                }
                if (resultMappers.size() > 1) {
                    List<Object> resultList = new ArrayList<>();
                    resultList.add(result);
                    for (int index = 1; index < resultMappers.size();) {
                        if (stmt.getMoreResults()) {
                            try (ResultSet resultSet = stmt.getResultSet();) {
                                resultList.add(resultMappers.get(index++).mapResult(resultSet));
                            }
                        } else {
                            if (stmt.getUpdateCount() == -1)
                                break;
                            resultList.add(stmt.getUpdateCount());
                        }
                    }
                    result = resultList;
                }
            } else {
                result = stmt.getUpdateCount();
            }
            if (statement.keyGenerator != null)
                statement.keyGenerator.processAfter(stmt, parameter);
            return result;
        }
    }

    private static Statement createStatement(Connection conn, BoundSql boundSql,
            MappedStatement statement) throws SQLException {
        switch (statement.statementType) {
            case CALLABLE:
                return conn.prepareCall(boundSql.sql);
            case PREPARED:
                if (statement.keyGenerator != null) {
                    if (statement.keyColumns != null) {
                        return conn.prepareStatement(boundSql.sql, statement.keyColumns);
                    } else {
                        return conn.prepareStatement(boundSql.sql,
                                PreparedStatement.RETURN_GENERATED_KEYS);
                    }
                } else {
                    return conn.prepareStatement(boundSql.sql);
                }
            case STATEMENT:
                return conn.createStatement();
            default:
                throw new SQLException();
        }
    }

    private static int getMinTimeout(int timeout1, int timeout2) {
        if (timeout1 > 0) {
            if (timeout2 > 0) {
                return Math.min(timeout1, timeout2);
            } else {
                return timeout1;
            }
        } else {
            return timeout2;
        }
    }
}
