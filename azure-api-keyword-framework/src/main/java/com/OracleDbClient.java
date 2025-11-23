package com;

import com.config.FrameworkConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OracleDbClient implements AutoCloseable {

    private Connection connection;

    public OracleDbClient(FrameworkConfig config) {
        try {
            // Explicit driver load (optional for modern JDBC, but safe)
            Class.forName("oracle.jdbc.OracleDriver");
            this.connection = DriverManager.getConnection(
                    config.getDbUrl(),
                    config.getDbUser(),
                    config.getDbPassword()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Oracle DB", e);
        }
    }

    public List<Map<String, Object>> queryForList(String sql) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<Map<String, Object>> list = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String colName = md.getColumnLabel(i);
                    row.put(colName, rs.getObject(i));
                }
                list.add(row);
            }
            return list;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignore) {
        }
    }
}
