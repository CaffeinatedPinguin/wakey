package dev.wakey.testsupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

/** Wraps a data source and counts the JDBC statements prepared through its connections. */
public final class QueryCountingDataSource extends AbstractDataSource {

    private static final Pattern FROM_TABLE = Pattern.compile("(?is)\\bfrom\\s+\"?([a-z_][a-z0-9_]*)\"?");

    private final DataSource delegate;

    private final AtomicInteger statements = new AtomicInteger();

    private final AtomicInteger selects = new AtomicInteger();

    private final Map<String, AtomicInteger> selectsByTable = new ConcurrentHashMap<>();

    public QueryCountingDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return counted(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return counted(delegate.getConnection(username, password));
    }

    public int statementCount() {
        return statements.get();
    }

    /** The number of prepared statements whose SQL reads rows with a {@code SELECT}. */
    public int selectCount() {
        return selects.get();
    }

    /** The number of prepared statements that read rows from one table. */
    public int selectCount(String table) {
        AtomicInteger count = selectsByTable.get(table);
        return count == null ? 0 : count.get();
    }

    public void reset() {
        statements.set(0);
        selects.set(0);
        selectsByTable.clear();
    }

    private Connection counted(Connection connection) {
        ClassLoader classLoader = getClass().getClassLoader();
        return (Connection) Proxy.newProxyInstance(
                classLoader, new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if (isStatementCreation(method.getName())) {
                        statements.incrementAndGet();
                        if (isSelect(method.getName(), args)) {
                            selects.incrementAndGet();
                            countTable(args);
                        }
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException exception) {
                        Throwable cause = exception.getCause();
                        throw cause == null ? exception : cause;
                    }
                });
    }

    private void countTable(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String sql) {
            Matcher matcher = FROM_TABLE.matcher(sql);
            if (matcher.find()) {
                String table = matcher.group(1);
                selectsByTable
                        .computeIfAbsent(table, ignored -> new AtomicInteger())
                        .incrementAndGet();
            }
        }
    }

    private static boolean isStatementCreation(String methodName) {
        return methodName.equals("prepareStatement")
                || methodName.equals("prepareCall")
                || methodName.equals("createStatement");
    }

    private static boolean isSelect(String methodName, Object[] args) {
        return methodName.equals("prepareStatement")
                && args != null
                && args.length > 0
                && args[0] instanceof String sql
                && sql.stripLeading().regionMatches(true, 0, "SELECT", 0, "SELECT".length());
    }
}
