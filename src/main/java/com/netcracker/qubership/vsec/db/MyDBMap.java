package com.netcracker.qubership.vsec.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MyDBMap {
    private static final Logger log = LoggerFactory.getLogger(MyDBMap.class);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS my_db_map (
                 key_name VARCHAR(256) PRIMARY KEY,
                 key_value VARCHAR(4096)
              )
            """;

    private static final String LOAD_ALL_SQL = "SELECT key_name, key_value FROM my_db_map";

    private static final String SAVE_ALL_SQL = "MERGE INTO my_db_map (key_name, key_value) VALUES (?, ?)";

    private final Connection conn;
    private Map<String, String> data;

    public MyDBMap(Connection conn) {
        this.conn = conn;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException sqlEx) {
            log.error("Error while executing DDL to create table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        loadAllFromDB();
    }

    // todo: thread unsafe method - check later for concurrency
    public void saveAllToDB() {
        try (PreparedStatement pstmt = conn.prepareStatement(SAVE_ALL_SQL)) {
            for (HashMap.Entry<String, String> entry : data.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setString(2, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException sqlEx) {
            log.error("Error while executing DML to save data into table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }
    }

    public void loadAllFromDB() {
        HashMap<String, String> map = new HashMap<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(LOAD_ALL_SQL)) {

            while (rs.next()) {
                String key = rs.getString("key_name");
                String value = rs.getString("key_value");
                map.put(key, value);
            }
        } catch (SQLException sqlEx) {
            log.error("Error while executing DML to load data from table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        this.data = map;
    }

    public void setValue(String key, String value) {
        data.put(key, value);
    }

    public String getValue(String key) {
        return data.get(key);
    }

    public String getValue(String key, String defaultValue) {
        String actualValue = data.get(key);
        if (actualValue == null) setValue(key, defaultValue);
        return getValue(key);
    }
}
