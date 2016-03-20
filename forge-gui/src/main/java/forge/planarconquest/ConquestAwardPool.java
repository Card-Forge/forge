package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;
import forge.card.CardRarity;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.util.Aggregates;

public class ConquestAwardPool {
    private final BoosterPool commons, uncommons, rares, mythics;

    public ConquestAwardPool(Iterable<PaperCard> cards) {
        commons = new BoosterPool();
        uncommons = new BoosterPool();
        rares = new BoosterPool();
        mythics = new BoosterPool();

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

    public int getShardValue(CardRarity rarity, int baseValue) {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        switch (rarity) {
        case Common:
            return baseValue;
        case Uncommon:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_UNCOMMON_MULTIPLIER));
        case Rare:
        case Special:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_RARE_MULTIPLIER));
        case MythicRare:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_MYTHIC_MULTIPLIER));
        default:
            return 0;
        }
    }

    public BoosterPool getCommons() {
        return commons;
    }
    public BoosterPool getUncommons() {
        return uncommons;
    }
    public BoosterPool getRares() {
        return rares;
    }
    public BoosterPool getMythics() {
        return mythics;
    }

    public class BoosterPool {
        private final List<PaperCard> cards = new ArrayList<PaperCard>();

        private BoosterPool() {
        }

        public boolean isEmpty() {
            return cards.isEmpty();
        }

        private void add(PaperCard c) {
            cards.add(c);
        }

        public void rewardCard(List<PaperCard> rewards) {
            int index = Aggregates.randomInt(0, cards.size() - 1);
            PaperCard c = cards.get(index);
            cards.remove(index);
            rewards.add(c);
        }
    }
}