package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import utils.SimulationTables.Format;

public class Table {
    private final String title;
    private final TreeMap<String, Map<String, String>> rows;

    public Table() {
        this(null);
    }

    public Table(String title) {
        this.title = title;
        this.rows = new TreeMap<>();
    }

    public String getTitle() {
        return title;
    }

    public Map<String, String> addRow(int id) {
        return addRow(String.format("%09d", id));
    }

    public Map<String, String> getRow(int id) {
        return getRow(String.format("%09d", id));
    }

    public Map<String, String> addRow(String key) {
        HashMap<String, String> map = new HashMap<>();
        rows.put(key, map);
        return map;
    }

    public Map<String, String> getRow(String key) {
        Map<String, String> row = rows.get(key);
        return row != null ? row : addRow(key);
    }

    public int numRows() {
        return rows.size();
    }

    // Merge the given table's rows into this one. This does NOT clone the rows
    public void merge(Table table) {
        for (String key : table.rows.keySet()) {
            getRow(key).putAll(table.rows.get(key));
        }
    }

    // Add the given (key,value) pair to every row in this table
    public void setAll(String column, String value) {
        for (Map<String, String> map : rows.values()) map.put(column, value);
    }

    // Build a CSV
    public String csv(String[] keys) {
        StringBuilder builder = new StringBuilder();
        String[] elems = new String[keys.length];

        builder.append(String.join(",", keys)).append('\n');

        for (Map<String, String> map : rows.values()) {
            for (int i = 0; i < keys.length; ++i) {
                elems[i] = map.getOrDefault(keys[i], "");
            }
            builder.append(String.join(",", elems)).append('\n');
        }

        return builder.toString();
    }

    // Build a pretty table
    public String table(String[] keys, String columnDelimiter) {
        StringBuilder builder = new StringBuilder();
        int[] width = new int[keys.length];
        String[] elems = new String[keys.length];

        for (int i = 0; i < keys.length; ++i) width[i] = keys[i].length();
        for (Map<String, String> map : rows.values()) {
            for (int i = 0; i < keys.length; ++i) {
                String value = map.getOrDefault(keys[i], "");
                if (width[i] < value.length()) {
                    width[i] = value.length();
                }
            }
        }

        for (int i = 0; i < keys.length; ++i) {
            elems[i] = " ".repeat(width[i] - keys[i].length()) + keys[i];
        }
        builder.append(String.join(columnDelimiter, elems)).append('\n');

        for (Map<String, String> map : rows.values()) {
            for (int i = 0; i < keys.length; ++i) {
                String value = map.getOrDefault(keys[i], "");
                elems[i] = " ".repeat(width[i] - value.length()) + value;
            }
            builder.append(String.join(columnDelimiter, elems)).append('\n');
        }

        return builder.toString();
    }

    public String table(String[] keys) {
        return table(keys, "  ");
    }

    public String output(Format format, String[] keys) {
        switch (format) {
        case CSV:
            return csv(keys);
        case TABLE:
        default:
            return table(keys);
        }
    }
}