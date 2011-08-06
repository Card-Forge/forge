
package forge.properties;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;


/**
 * A collection of name/value pairs with sorted keys and utility methods.
 */
public class Preferences {
    protected Properties props;
    
    public Preferences() {
        props = new Properties();
    }
    
    public Preferences(Preferences prefs) {
        props = prefs.props;
    }
    
    public synchronized Enumeration<String> keys() {
    	@SuppressWarnings({"unchecked", "rawtypes"})
    	Set<String> keysEnum = (Set) props.keySet();
        Vector<String> keyList = new Vector<String>();
        keyList.addAll(keysEnum);
        Collections.sort(keyList);
        return keyList.elements();
    }
    
    public int getInt(String name, int defaultValue) {
        String value = props.getProperty(name);
        if(value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException ex) {
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = props.getProperty(name);
        if(value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    public long getLong(String name, long defaultValue) {
        String value = props.getProperty(name);
        if(value == null) return defaultValue;
        return Long.parseLong(value);
    }
    
    public void set(String key, Object value) {
        props.setProperty(key, String.valueOf(value));
    }
    
    public String get(String key, Object value) {
        String string = null;
        if(value != null) string = String.valueOf(value);
        return props.getProperty(key, string);
    }
    
    public void load(FileInputStream stream) throws IOException {
        props.load(stream);
    }
    
    public void store(FileOutputStream stream, String comments) throws IOException {
        props.store(stream, comments);
    }
}
