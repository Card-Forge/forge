package forge.quest.bazaar;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.AllZone;
import forge.Card;
import forge.CardReader;
import forge.card.cardfactory.CardFactoryUtil;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@XStreamAlias(value="level")
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
        List<String> cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.Quest.BAZAAR_DIR), cardFile));
        Card petCard = CardReader.readCard(cardLines);
        petCard.setImageFilename(picture.replace('_', ' '));
        petCard.setToken(true);
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());
        petCard.setSickness(true);
        return petCard;
    }

    public final int getCost() {
        return cost;
    }

    public final String getNextLevel() {
        return nextLevel;
    }
}
