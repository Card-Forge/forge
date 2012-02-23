package forge.util;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FileSectionManual extends FileSection {
    
    public void put(String key, String value) {
        getLines().put(key, value);
    }

}
