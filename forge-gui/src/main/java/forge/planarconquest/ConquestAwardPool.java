package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;
import forge.item.PaperCard;

public class ConquestAwardPool {
    public final List<PaperCard> commons, uncommons, rares, mythics;

    public ConquestAwardPool(Iterable<PaperCard> cards) {
        commons = new ArrayList<PaperCard>();
        uncommons = new ArrayList<PaperCard>();
        rares = new ArrayList<PaperCard>();
        mythics = new ArrayList<PaperCard>();

        for (PaperCard c : cards) {
            switch (c.getRarity()) {
            case Common:
                commons.add(c);
                break;
            case Uncommon:
                uncommons.add(c);
                break;
            case Rare:
            case Special: //lump special cards in with rares for simplicity
                rares.add(c);
                break;
            case MythicRare:
                mythics.add(c);
                break;
            default:
                break;
            }
        }
    }
}