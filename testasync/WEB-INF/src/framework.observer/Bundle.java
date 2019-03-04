package framework.observer;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class Bundle {

    protected HashMap<String, Object> map;

    public Bundle() {
        this.map = new HashMap<>();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public boolean containsValue(String value) {
        return map.containsValue(value);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public boolean isEmpty() {
        return (null == map || map.size() == 0);
    }

    public HashMap<String, Object> prototype() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public void putBundle(String key, Bundle value) { map.put(key, value); }

    public void putByte(String key, byte value) {
        map.put(key, value);
    }

    public void putByteArray(String key, byte[] value) {
        map.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        map.put(key, value);
    }

    public void putInt(String key, int value) {
        map.put(key, value);
    }

    public void putLong(String key, long value) {
        map.put(key, value);
    }

    public void putFloat(String key, float value) {
        map.put(key, value);
    }

    public void putDouble(String key, double value) {
        map.put(key, value);
    }

    public void putChar(String key, char value) {
        map.put(key, value);
    }

    public void putString(String key, String value) {
        map.put(key, value);
    }

    public void putFile(String key, File value) { map.put(key, value); }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    public Bundle getBundler(String key) {
        if(!map.containsValue(key)) return null;
        try {
            return (Bundle) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public byte getByte(String key, byte defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (byte) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public byte[] getByteArray(String key) {
        if(!map.containsKey(key)) return null;
        try {
            return (byte[]) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (boolean) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (int) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (long) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    public float getFloat(String key, float defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (float) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (double) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    public char getChar(String key, char defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (char) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (String) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public File getFile(String key) {
        return getFile(key, null);
    }

    public File getFile(String key, File defaultValue) {
        if(!map.containsKey(key)) return defaultValue;
        try {
            return (File) map.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public Object get(String key) {
        return map.get(key);
    }

}
