package com.github.database.rider.core.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConfig.ConfigProperty;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

import com.github.database.rider.core.api.connection.ConnectionHolder;
import com.github.database.rider.core.configuration.DBUnitConfig;
import com.github.database.rider.core.util.DriverUtils;

/**
 * @author artemy-osipov
 */
public class RiderDataSource {

    public enum DBType {
        HSQLDB, H2, MYSQL, ORACLE, POSTGRESQL, UNKNOWN
    }

    private final ConnectionHolder connectionHolder;
    private final DBUnitConfig dbUnitConfig;
    private Connection connection;
    private DatabaseConnection dbUnitConnection;
    private DBType dbType;

    public RiderDataSource(ConnectionHolder connectionHolder, DBUnitConfig dbUnitConfig) throws SQLException {
        this.connectionHolder = connectionHolder;
        this.dbUnitConfig = dbUnitConfig;
        init();
    }

    public Connection getConnection() throws SQLException {
        if (!dbUnitConfig.isCacheConnection() || connection == null || connection.isClosed()) {
            connection = connectionHolder.getConnection();
        }

        return connection;
    }

    public DatabaseConnection getDBUnitConnection() throws SQLException {
        if (!dbUnitConfig.isCacheConnection()) {
            initDBUnitConnection();
        }

        return dbUnitConnection;
    }

    public DBType getDBType() {
        return dbType;
    }

    private void init() throws SQLException {
        Connection conn = getConnection();
        if (conn != null) {
            dbType = resolveDBType(DriverUtils.getDriverName(conn));
            initDBUnitConnection();
        }
    }

    private void initDBUnitConnection() throws SQLException {
        try {
            dbUnitConnection = new DatabaseConnection(getConnection());
            configDatabaseProperties();
        } catch (DatabaseUnitException e) {
            throw new SQLException(e);
        }
    }

    private void configDatabaseProperties() {
        DatabaseConfig config = dbUnitConnection.getConfig();
        for (Map.Entry<String, Object> p : dbUnitConfig.getProperties().entrySet()) {
            ConfigProperty byShortName = DatabaseConfig.findByShortName(p.getKey());
            if (byShortName != null) {
                config.setProperty(byShortName.getProperty(), p.getValue());
            }
        }

        if (!dbUnitConfig.getProperties().containsKey("datatypeFactory")) {
            IDataTypeFactory dataTypeFactory = getDataTypeFactory(dbType);
            if (dataTypeFactory != null) {
                config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory);
            }
        }
    }

    private IDataTypeFactory getDataTypeFactory(DBType dbType) {
        switch (dbType) {
            case HSQLDB:
                return new HsqldbDataTypeFactory();
            case H2:
                return new H2DataTypeFactory();
            case MYSQL:
                return new MySqlDataTypeFactory();
            case POSTGRESQL:
                return new PostgresqlDataTypeFactory();
            case ORACLE:
                return new Oracle10DataTypeFactory();
            default:
                return null;
        }
    }

    private DBType resolveDBType(String driverName) {
        if (DriverUtils.isHsql(driverName)) {
            return DBType.HSQLDB;
        } else if (DriverUtils.isH2(driverName)) {
            return DBType.H2;
        } else if (DriverUtils.isMysql(driverName)) {
            return DBType.MYSQL;
        } else if (DriverUtils.isPostgre(driverName)) {
            return DBType.POSTGRESQL;
        } else if (DriverUtils.isOracle(driverName)) {
            return DBType.ORACLE;
        } else {
            return DBType.UNKNOWN;
        }
    }
}
