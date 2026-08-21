package com.example.wasaas.tenant.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class TenantDataSourcePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof TenantAwareDataSource)) {
            return new TenantAwareDataSource((DataSource) bean);
        }
        return bean;
    }

    private static class TenantAwareDataSource extends DelegatingDataSource {
        TenantAwareDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            return wrapConnection(connection);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            return wrapConnection(connection);
        }

        private Connection wrapConnection(Connection connection) {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    new ConnectionInvocationHandler(connection)
            );
        }
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection target;
        private UUID lastSetTenantId = null;
        private boolean isLocal = true;

        ConnectionInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("close")) {
                if (lastSetTenantId != null && !isLocal) {
                    try (PreparedStatement ps = target.prepareStatement("RESET app.tenant_id")) {
                        ps.execute();
                    } catch (SQLException ignored) {
                    }
                }
                return method.invoke(target, args);
            }
            if (isExecuteMethod(method)) {
                UUID currentTenant = TenantContext.get();
                if (currentTenant != null && !currentTenant.equals(lastSetTenantId)) {
                    isLocal = !target.getAutoCommit();
                    String sql = isLocal
                            ? "SELECT set_config('app.tenant_id', ?, true)"
                            : "SELECT set_config('app.tenant_id', ?, false)";
                    try (PreparedStatement ps = target.prepareStatement(sql)) {
                        ps.setString(1, currentTenant.toString());
                        ps.execute();
                    }
                    lastSetTenantId = currentTenant;
                }
            }
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            return method.invoke(target, args);
        }

        private boolean isExecuteMethod(Method method) {
            String name = method.getName();
            return name.equals("prepareStatement") || name.equals("createStatement") || name.equals("prepareCall");
        }
    }
}
