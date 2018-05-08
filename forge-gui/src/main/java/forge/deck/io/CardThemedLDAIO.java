package forge.deck.io;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.properties.ForgeConstants;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by maustin on 11/05/2017.
 */
public class CardThemedLDAIO {

    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".lda.dat";

    public static void saveLDA(String format, Map<String,List<List<String>>> map){
        File file = getLDAFile(format);
        ObjectOutputStream s = null;
        try {
            FileOutputStream f = new FileOutputStream(file);
            s = new ObjectOutputStream(f);
            s.writeObject(map);
            s.close();
        } catch (IOException e) {
            System.out.println("Error writing matrix data: " + e);
        } finally {
            if(s!=null) {
                try {
                    s.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String,List<List<String>>> loadLDA(String format){
        try {
            FileInputStream fin = new FileInputStream(getLDAFile(format));
            ObjectInputStream s = new ObjectInputStream(fin);
            Map<String, List<List<String>>> matrix = (Map<String, List<List<String>>>) s.readObject();
            s.close();
            return matrix;
        }catch (Exception e){
            System.out.println("Error reading LDA data: " + e);
            return null;
        }

    }

    public static File getLDAFile(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name + SUFFIX_DATA);
    }

    public static File getMatrixFolder(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name);
    }

    public static File getLDAFile(final GameFormat gf) {
        return getLDAFile(gf.getName());
    }
}
