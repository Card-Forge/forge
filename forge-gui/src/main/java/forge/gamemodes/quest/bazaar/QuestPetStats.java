package forge.gamemodes.quest.bazaar;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@XStreamAlias(value = "level")
public class QuestPetStats {

    @XStreamAsAttribute()
    @XStreamAlias(value = "value")
    private int levelValue;

    @XStreamAsAttribute()
    @XStreamAlias(value = "pic")
    private String picture;

    @XStreamAsAttribute()
    private String stats;

    @XStreamAsAttribute()
    private String cardFile;

    @XStreamAsAttribute()
    private int cost;

    @XStreamAsAttribute()
    private String nextLevel;

    private transient PaperToken petCard = null;

    private QuestPetStats() { }

    public final int getLevelValue() {
        return levelValue;
    }

    public final String getPicture() {
        return picture;
    }

    public final String getStats() {
        return stats;
    }

    public final PaperToken getCard() {
        if (null == petCard) {
            List<String> cardLines = FileUtil.readFile(new File(ForgeConstants.BAZAAR_DIR, cardFile));
            CardRules rules = CardRules.fromScript(cardLines);
            petCard = new PaperToken(rules, CardEdition.UNKNOWN, picture);
        }
        return petCard;
    }

    public final int getCost() {
        return cost;
    }

    public final String getNextLevel() {
        return nextLevel;
    }
}
