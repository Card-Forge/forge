package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;

import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.util.Aggregates;

public class ConquestAwardPool {
    private final BoosterPool commons, uncommons, rares, mythics;
    private final int commonValue, uncommonValue, rareValue, mythicValue;

    public ConquestAwardPool(Iterable<PaperCard> cards) {
        ConquestPreferences prefs = FModel.getConquestPreferences();

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

        //calculate odds of each rarity
        float commonOdds = commons.getOdds(prefs.getPrefInt(CQPref.BOOSTER_COMMONS));
        float uncommonOdds = uncommons.getOdds(prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS));
        int raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        float rareOdds = rares.getOdds(raresPerBooster);
        float mythicOdds = mythics.getOdds((float)raresPerBooster / (float)prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC));

        //determine value of each rarity based on the base value of a common
        commonValue = prefs.getPrefInt(CQPref.AETHER_BASE_VALUE);
        uncommonValue = Math.round(commonValue / (uncommonOdds / commonOdds));
        rareValue = Math.round(commonValue / (rareOdds / commonOdds));
        mythicValue = mythics.isEmpty() ? 0 : Math.round(commonValue / (mythicOdds / commonOdds));
    }

    public int getShardValue(PaperCard card) {
        switch (card.getRarity()) {
        case Common:
            return commonValue;
        case Uncommon:
            return uncommonValue;
        case Rare:
        case Special:
            return rareValue;
        case MythicRare:
            return mythicValue;
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

        private float getOdds(float perBoosterCount) {
            int count = cards.size();
            if (count == 0) { return 0; }
            return (float)perBoosterCount / (float)count;
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