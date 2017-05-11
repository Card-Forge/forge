package forge.deck.io;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import forge.deck.CardPool;
import forge.game.GameFormat;
import forge.gauntlet.GauntletData;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.IgnoringXStream;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by maustin on 11/05/2017.
 */
public class CardThemedMatrixIO {

    /** suffix for all gauntlet data files */
    public static final String SUFFIX_DATA = ".dat";

    public static void saveMatrix(GameFormat format, HashMap<String,List<PaperCard>> map){
        File file = getMatrixFile(format);
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

    public static HashMap<String,List<PaperCard>> loadMatrix(GameFormat format){
        try {
            FileInputStream fin = new FileInputStream(getMatrixFile(format));
            ObjectInputStream s = new ObjectInputStream(fin);
            HashMap<String, List<PaperCard>> matrix = (HashMap<String, List<PaperCard>>) s.readObject();
            s.close();
            return matrix;
        }catch (Exception e){
            System.out.println("Error reading matrix data: " + e);
            return null;
        }

    }

    public static File getMatrixFile(final String name) {
        return new File(ForgeConstants.DECK_GEN_DIR, name + SUFFIX_DATA);
    }

    public static File getMatrixFile(final GameFormat gf) {
        return getMatrixFile(gf.getName());
    }
}
