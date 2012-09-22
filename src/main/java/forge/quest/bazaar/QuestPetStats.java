package forge.quest.bazaar;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.Card;
import forge.card.cardfactory.CardReader;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
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

    private transient Card petCard = null;

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

    public final Card getCard() {
        if (null == petCard) {

            List<String> cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.Quest.BAZAAR_DIR), cardFile));
            petCard = CardReader.readCard(cardLines);
            petCard.setImageName(picture.replace('_', ' '));
            petCard.setToken(true);
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
