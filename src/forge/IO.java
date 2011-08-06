
package forge;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.error.ErrorViewer;


public class IO {
    private final File                file;
    
    private Map<String, Serializable> map = new HashMap<String, Serializable>();
    
    public IO(String filename) {
        file = new File(filename);
        try {
            if(!file.exists()) {
                file.createNewFile();
                writeMap();
            }
        } catch(Exception ex) {
            ErrorViewer.showError(ex, "IO : constructor error, bad filename - %s%n%s", filename, ex.getMessage());
            throw new RuntimeException(String.format("IO : constructor error, bad filename - %s%n%s", filename,
                    ex.getMessage()), ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    // Map has unchecked cast
    private void readMap() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            map = (Map<String, Serializable>) in.readObject();
            in.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : readMap() error, " + ex.getMessage());
        }
    }
    
    private void writeMap() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(map);
            
            out.flush();
            out.close();
            
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : writeMap error, " + ex.getMessage());
        }
    }
    
    public void writeObject(String key, Serializable ser) {
        readMap();
        map.put(key, ser);
        writeMap();
    }
    
    //may return null;
    public Object readObject(String key) {
        readMap();
        return map.get(key);
    }
    
    public void deleteObject(String key) {
        readMap();
        map.remove(key);
        writeMap();
    }
    
    public ArrayList<String> getKeyList() {
        readMap();
        
        ArrayList<String> list = new ArrayList<String>();
        Iterator<String> it = map.keySet().iterator();
        while(it.hasNext())
            list.add(it.next()); // guessing that this is going to be a string.
        return list;
    }
    
    public String[] getKeyString() {
        ArrayList<String> list = getKeyList();
        String[] s = new String[list.size()];
        list.toArray(s);
        return s;
    }
}
