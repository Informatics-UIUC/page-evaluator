package edu.illinois.i3.emop.apps.pageevaluator;

import java.util.Properties;

public class KeyValueStore {
    private final Properties _store;

    public KeyValueStore() {
        _store = new Properties();
    }

    public void put(String key, Object value) {
        _store.setProperty(key, value.toString());
    }

    public String getString(String key) {
        return _store.getProperty(key);
    }

    public Integer getInt(String key) {
        String value = getString(key);
        return (value != null) ? Integer.parseInt(value) : null;
    }

    public Float getFloat(String key) {
        String value = getString(key);
        return (value != null) ? Float.parseFloat(value) : null;
    }

    public Double getDouble(String key) {
        String value = getString(key);
        return (value != null) ? Double.parseDouble(value) : null;
    }

    public Boolean getBoolean(String key) {
        String value = getString(key);
        return (value != null) ? Boolean.parseBoolean(value) : null;
    }
}
