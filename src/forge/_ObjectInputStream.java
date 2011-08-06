/**
 * _ObjectInputStream.java
 * 
 * Created on 29.10.2009
 */

package forge;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;


/**
 * The class _ObjectInputStream.
 * 
 * @version V0.0 29.10.2009
 * @author Clemens Koza
 */
public class _ObjectInputStream extends ObjectInputStream {
    public _ObjectInputStream() throws IOException {
        super();
        enableResolveObject(true);
    }
    
    public _ObjectInputStream(InputStream is) throws IOException {
        super(is);
        enableResolveObject(true);
    }
    
    @Override
    protected Object resolveObject(Object obj) throws IOException {
        Class<?> c = obj.getClass().getComponentType();
        if(c != null && "Deck".equals(c.getName())) {
            return new Deck[((Object[]) obj).length];
        } else return obj;
    }
}
