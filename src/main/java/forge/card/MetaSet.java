package forge.card;

import java.io.File;
import java.util.List;

import forge.Singletons;
import forge.game.limited.CustomLimited;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.util.FileUtil;
import forge.util.closures.Lambda1;

/** 
 * The class MetaSet. This class is used to define 'special'
 * sets within a (fantasy) block, like custom sets (cubes),
 * combination sets (sub-blocks), and full cardpool.
 * 
 * NOTE: The format of a MetaSet definition is as follows
 * (in a blocks definition file):
 * 
 *      "metaX A/B/C"
 * 
 * where "X" is an integer from 0...8 (just like for sets)
 * 
 * "A" is either "cube", "meta", or "full".
 * 
 * "full" uses all available cards for this booster, just like
 * the full cardpool option. The values of "B" and "C" are not
 * relevant ("B" is not used at all, and the displayed name is
 * always "*FULL").
 * 
 *  "meta" uses a cardpool that is combined from several
 *  editions. The parameter "B" is a list of 3-letter
 *  edition codes, separated with commas, e.g.
 *  "2ED,ARN,ATQ". The parameter "C" is the name
 *  that is displayed for this meta-booster in the
 *  set selection menu.
 * 
 *  "cube" uses a previously defined custom sealed deck
 *  deck cube as the cardpool for this booster. The cube
 *  definition file must be in res/sealed/ and have a
 *  ".sealed" extension. The related .dck file must be
 *  in the res/decks/cube directory.
 *  "B" is the name of the cube definition file without
 *  the ".sealed" extension, e.g. "juzamjedi".
 *  "C" is the name that is displayed for this meta-booster
 *  in the set selection menu.
 *
 */
public class MetaSet {

    private final String type;
    private final String data;
    private final String code;
    // private BoosterGenerator boosterGen;

    /**
     * The constructor. A new MetaSet is currently only instantiated in CardBlock.java
     * when CardBlock information is read.
     * 
     * @param creationString
     *       a {@link java.lang.String} object.
     */
    public MetaSet(final String creationString) {

        final String[] kv = creationString.split("/", 3);

        type = kv[0];
        data = kv[1];
        // boosterGen = null;


        if ("cube".equalsIgnoreCase(type)) {
            code = "*C:" + kv[2];
            //System.out.println("Created a CUBE, code '" + code + "'");
        }
        else if ("full".equalsIgnoreCase(type)) {
            code = "*FULL";
            //System.out.println("Selecting from FULL cardpool'" + code + "'");
        }
        else if ("meta".equalsIgnoreCase(type)) {
            code = "*B:" + kv[2];
            //System.out.println("Created a META set of " + kv[1] + ", code '" + code + "'");
        }
        else {
            code = null;
            throw new RuntimeException("Invalid MetaSet type: " + type);
        }

    }

    /**
     * Return the code.
     * 
     * @return
     *  String, code
     */
    public final String getCode() {
        return code;
    }

    /**
     * 
     * Attempt to get a booster.
     * 
     * @return UnOpenedProduct, the generated booster.
     */
    public UnOpenedProduct getBooster() {

        //System.out.println("MetaSet.booster called...");

        if ("cube".equalsIgnoreCase(type)) {

            final File dFolder = new File("res/sealed/");

            if (!dFolder.exists()) {
                throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                        + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
            }

            List<String> dfData = FileUtil.readFile("res/sealed/" + data + ".sealed");
            final CustomLimited myCube = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());
            final BoosterGenerator bpCustom = new BoosterGenerator(myCube.getCardPool());
            final Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
              @Override
              public List<CardPrinted> apply(final BoosterGenerator pack) {
                   if (myCube.getIgnoreRarity()) {
                       if (!myCube.getSingleton()) {
                           return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, myCube.getNumCards(), 0);
                       } else {
                           return pack.getSingletonBoosterPack(myCube.getNumCards());
                       }
                   }
                   return pack.getBoosterPack(myCube.getNumbersByRarity(), 0, 0, 0);
               }

            };

            return new UnOpenedProduct(fnPick, bpCustom);
        }
        else if ("full".equalsIgnoreCase(type)) {
            // System.out.println("Initializing boosters for FULL cardpool");
            final BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            return new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpFull);
        }
        else if ("meta".equalsIgnoreCase(type)) {
            // System.out.println("Initializing boosters for " + data);

            // NOTE: The following code is far from ideal in a number of ways. If someone can
            // think of a way to improve it, please do so. --BBU
            ItemPool<CardPrinted> cardPool = new ItemPool<CardPrinted>(CardPrinted.class);
            for (CardPrinted aCard : CardDb.instance().getAllCards()) {
                if (data.indexOf(aCard.getEdition()) > -1) {
                    cardPool.add(aCard);
                    // System.out.println("Added card" + aCard.getName());
                    }
            }

                final BoosterGenerator bpSets = new BoosterGenerator(cardPool);
                return new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpSets);
        }
        else {
            throw new RuntimeException("Cannot initialize boosters for: " + type);
        }
    }
}
