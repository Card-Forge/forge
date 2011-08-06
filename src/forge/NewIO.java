
package forge;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.util.HashMap;

import forge.error.ErrorViewer;


public class NewIO {
    private HashMap<String, Serializable> map = new HashMap<String, Serializable>();
    private final File                    file;
    
    public NewIO(String filename) {
        file = new File(filename);
        if(!file.exists()) createBlankFile();
    }
    
    //would like one class to do all of the IO
    //internally just a Map is used, and written to the hard drive
    //WARNING - different parts of the program have to use different, unique keys
    //the data is immediately written, nothing is cached or buffered
    public void write(String key, Serializable data) {
        readData();
        map.put(key, data);
        
        ObjectOutputStream out = getWriter();
        
        try {
            out.writeObject(map);
            
            out.flush();
            out.close();
            out = null;
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : write() error - " + ex);
        }
    }
    
    public Object read(String key) {
        readData();
        return map.get(key);
    }
    
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }
    
    private ObjectOutputStream getWriter() {
        try {
            return new ObjectOutputStream(new FileOutputStream(file));
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : getWriter() - error - " + ex);
        }
    }
    
    private ObjectInputStream getReader() {
        try {
            return new ObjectInputStream(new FileInputStream(file));
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : getReader() - error - " + ex);
        }
    }//getReader()
    
    @SuppressWarnings("unchecked")
    // HashMap needs <type>
    private void readData() {
        ObjectInputStream in = getReader();
        Object o;
        try {
            o = in.readObject();
            in.close();
            in = null;
        } catch(WriteAbortedException ex) {
            //deletes current file
            //a new file will be constructed next time by the constructor
            try {
                in.close();
                in = null;
            } catch(IOException ex2) {}
            file.delete();
            ErrorViewer.showError(ex, "All old decks are lost, sorry - please restart");
            throw new RuntimeException(
                    "IO : readData() - WriterAbortedException - all old decks are lost, sorry - please restart");
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : readData() - error - " + ex);
        }
        
        if(o instanceof HashMap) map = (HashMap<String, Serializable>) o;
    }
    
    private void createBlankFile() {
        try {
            if(file.exists()) file.delete();
            else file.createNewFile();
            
            ObjectOutputStream out = getWriter();
            out.writeObject(map);
            
            out.flush();
            out.close();
            out = null;
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("IO : createBlankFile() - error - " + ex);
        }
    }//createBlankFile()
}
