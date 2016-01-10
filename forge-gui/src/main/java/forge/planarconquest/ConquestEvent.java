package forge.planarconquest;

import java.util.Set;

import forge.deck.Deck;
import forge.game.GameType;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public abstract class ConquestEvent {
    private final ConquestLocation location;
    private final int tier;
    private Deck opponentDeck;

    public ConquestEvent(ConquestLocation location0, int tier0) {
        location = location0;
        tier = tier0;
    }

    public ConquestLocation getLocation() {
        return location;
    }

    public int getTier() {
        return tier;
    }

    public Deck getOpponentDeck() {
        if (opponentDeck == null) {
            opponentDeck = buildOpponentDeck();
        }
        return opponentDeck;
    }

    protected abstract Deck buildOpponentDeck();
    public abstract void addVariants(Set<GameType> variants);
    public abstract String getEventName();
    public abstract String getOpponentName();
    public abstract String getAvatarImageKey();

    public static class ConquestEventRecord implements IXmlWritable {
        private final ConquestRecord[] tiers = new ConquestRecord[4];

        public ConquestEventRecord() {
        }
        public ConquestEventRecord(XmlReader xml) {
            xml.read("tiers", tiers, ConquestRecord.class);
        }
        @Override
        public void saveToXml(XmlWriter xml) {
            xml.write("tiers", tiers);
        }

        public boolean hasConquered() {
            //it's enough to check first tier, as second tier wouldn't unlock without beating it at least once
            ConquestRecord record = tiers[0];
            return record != null && record.getWins() > 0;
        }

        public int getTotalWins() {
            int wins = 0;
            for (int i = 0; i < tiers.length; i++) {
                ConquestRecord record = tiers[i];
                if (record != null) {
                    wins += record.getWins();
                }
            }
            return wins;
        }
        public int getTotalLosses() {
            int losses = 0;
            for (int i = 0; i < tiers.length; i++) {
                ConquestRecord record = tiers[i];
                if (record != null) {
                    losses += record.getLosses();
                }
            }
            return losses;
        }

        public int getWins(int tier) {
            ConquestRecord record = tiers[tier];
            return record != null ? record.getWins() : 0;
        }
        public int getLosses(int tier) {
            ConquestRecord record = tiers[tier];
            return record != null ? record.getLosses() : 0;
        }

        private ConquestRecord getOrCreateRecord(int tier) {
            ConquestRecord record = tiers[tier];
            if (record == null) {
                record = new ConquestRecord();
                tiers[tier] = record;
            }
            return record;
        }

        public void addWin(int tier) {
            getOrCreateRecord(tier).addWin();
        }
        public void addLoss(int tier) {
            getOrCreateRecord(tier).addLoss();
        }

        public int getHighestConqueredTier() {
            for (int i = tiers.length - 1; i >= 0; i--) {
                ConquestRecord record = tiers[i];
                if (record != null && record.getWins() > 0) {
                    return i;
                }
            }
            return -1;
        }
    }
}
