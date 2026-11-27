package dev.wakey.spaces;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

/** Wraps a data source and counts the JDBC statements prepared through its connections. */
final class QueryCountingDataSource extends AbstractDataSource {

    private final DataSource delegate;

    private final AtomicInteger statements = new AtomicInteger();

    QueryCountingDataSource(DataSource delegate) {
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

    int statementCount() {
        return statements.get();
    }

    void reset() {
        statements.set(0);
    }

    private Connection counted(Connection connection) {
        ClassLoader classLoader = getClass().getClassLoader();
        return (Connection) Proxy.newProxyInstance(
                classLoader, new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if (isStatementCreation(method.getName())) {
                        statements.incrementAndGet();
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException exception) {
                        Throwable cause = exception.getCause();
                        throw cause == null ? exception : cause;
                    }
                });
    }

    private static boolean isStatementCreation(String methodName) {
        return methodName.equals("prepareStatement")
                || methodName.equals("prepareCall")
                || methodName.equals("createStatement");
    }
}
