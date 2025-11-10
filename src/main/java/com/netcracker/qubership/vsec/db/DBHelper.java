package com.netcracker.qubership.vsec.db;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class DBHelper {
    static void setBind(int index, Object value, PreparedStatement pstm) throws SQLException {
        if (value instanceof String) {
            pstm.setString(index, (String) value);
            return;
        }
        if (value instanceof Integer) {
            pstm.setInt(index, (Integer) value);
            return;
        }
        if (value instanceof Double) {
            pstm.setDouble(index, (Double) value);
            return;
        }

        throw new IllegalArgumentException("Unsupported type of bind value = " +(value == null ? "null" : value.getClass()));
    }

    /**
     * Converts the database value to the appropriate Java type
     */
    static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If types already match, return as is
        if (targetType.isInstance(value)) {
            return value;
        }

        // Handle common type conversions
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } else if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } else if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse((String) value);
        } else if (targetType == String.class) {
            return value.toString();
        }

        // If no conversion is possible, return the original value
        // This might throw a ClassCastException later, which is acceptable
        return value;
    }

    /**
     * Parses a ResultSet and maps columns to object fields using @DBProperty annotation.
     * If necessary column is absent in the selected set - then it will be ignored, i.e. null value will be set.
     * @param resultSet The ResultSet to read data from
     * @param clazz The class of the object to create and populate
     * @return A new instance of the class with fields populated from ResultSet
     */
    static <T> T parse(ResultSet resultSet, Class<T> clazz) {
        try {
            // Create new instance of the class
            T instance = clazz.getDeclaredConstructor().newInstance();

            // Get all declared fields of the class
            Field[] fields = clazz.getDeclaredFields();
            Set<String> loadedColumnNames = getColumnNames(resultSet);

            for (Field field : fields) {
                // Check if field has @DBProperty annotation
                if (field.isAnnotationPresent(MyDBColumn.class)) {
                    MyDBColumn dbProperty = field.getAnnotation(MyDBColumn.class);
                    String columnName = dbProperty.value();
                    if (!loadedColumnNames.contains(columnName.toUpperCase())) continue;

                    // Make the field accessible (in case it's private)
                    field.setAccessible(true);

                    // Get value from ResultSet and set it to the field
                    Object value = resultSet.getObject(columnName);

                    // Handle null values appropriately
                    if (value != null) {
                        // Convert the value to the field's type if necessary
                        value = DBHelper.convertValue(value, field.getType());
                        field.set(instance, value);
                    }
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing ResultSet to object", e);
        }
    }

    static Set<String> getColumnNames(ResultSet rs)throws SQLException {
        Set<String> result = new HashSet<>();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            result.add(metaData.getColumnName(i).toUpperCase());
        }

        return result;
    }
}
