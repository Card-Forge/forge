package forge.deck.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import forge.game.GameFormat;
import forge.localinstance.properties.ForgeConstants;

/**
 * Created by maustin on 11/05/2017.
 */
public class CardThemedLDAIO {

    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".lda.dat";
    public static final String RAW_SUFFIX_DATA = ".raw.dat";

    public static void saveRawLDA(String format, List<Archetype> lda){
        File file = getRAWLDAFile(format);
        try (FileOutputStream f = new FileOutputStream(file);
             ObjectOutputStream s = new ObjectOutputStream(f)){
            s.writeObject(lda);
            s.close();
        } catch (IOException e) {
            System.out.println("Error writing matrix data: " + e);
        }
    }

    public static List<Archetype> loadRawLDA(String format){
        try (FileInputStream fin = new FileInputStream(getRAWLDAFile(format));
             ObjectInputStream s = new ObjectInputStream(fin)) {
            List<Archetype> matrix = (List<Archetype>) s.readObject();
            return matrix;
        } catch (Exception e){
            System.out.println("Error reading LDA data: " + e);
            return null;
        }

    }

    public static void saveLDA(String format, Map<String,List<List<Pair<String, Double>>>> map){
        File file = getLDAFile(format);

        try (FileOutputStream f = new FileOutputStream(file);
             ObjectOutputStream s = new ObjectOutputStream(f)){
            s.writeObject(map);
        } catch (IOException e) {
            System.out.println("Error writing matrix data: " + e);
        }
    }

    public static Map<String,List<List<Pair<String, Double>>>> loadLDA(String format){
        try (FileInputStream fin = new FileInputStream(getLDAFile(format));
             ObjectInputStream s = new ObjectInputStream(fin)) {
            Map<String,List<List<Pair<String, Double>>>> matrix = (Map<String,List<List<Pair<String, Double>>>>) s.readObject();
            return matrix;
        } catch (Exception e){
            System.out.println("Error reading LDA data: " + e);
            return null;
        }
    }

    public static File getLDAFile(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name + SUFFIX_DATA);
    }

    public static File getRAWLDAFile(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name + RAW_SUFFIX_DATA);
    }

    public static File getMatrixFolder(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name);
    }

    public static File getLDAFile(final GameFormat gf) {
        return getLDAFile(gf.getName());
    }
}
