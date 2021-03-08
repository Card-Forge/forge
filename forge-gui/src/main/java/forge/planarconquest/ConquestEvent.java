package forge.planarconquest;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;
import forge.util.collect.FCollectionReader;

public class ConquestEvent {
    private final ConquestRegion region;
    private final String name;
    private final String description;
    private final String deckPath;
    private final Set<GameType> variants;
    private final String avatar;
    private final String tempUnlock;
    private PaperCard avatarCard;
    private Deck deck;

    public ConquestEvent(ConquestRegion region0, String name0, String description0, String deckPath0, Set<GameType> variants0, String avatar0, String tempUnlock0) {
        region = region0;
        name = name0;
        description = description0;
        deckPath = deckPath0;
        variants = variants0;
        avatar = avatar0;
        tempUnlock = tempUnlock0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Deck getDeck() {
        if (deck == null) {
            if (deckPath != null) {
                File deckFile = new File(deckPath);
                if (deckFile.exists()) {
                    deck = DeckSerializer.fromFile(deckFile);
                }
            }
            if (deck == null || deck.isEmpty()) {
                //if deck can't be loaded, generate it randomly
                PaperCard commander = getAvatarCard();
                if (commander == null) {
                    commander = Aggregates.random(region.getCommanders());
                }
                deck = ConquestUtil.generateDeck(commander, region.getCardPool(), true);
            }
        }
        return deck;
    }

    public Set<GameType> getVariants() {
        return variants;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getTemporaryUnlock() { return tempUnlock; }

    public PaperCard getAvatarCard() {
        if (avatarCard == null && avatar != null) {
            //attempt to load card from plane's card pool
            avatarCard = region.getPlane().getCardPool().getCard(avatar);
            if (avatarCard == null) {
                //if not in plane's card pool, load from all cards instead
                avatarCard = FModel.getMagicDb().getCommonCards().getCard(avatar);
                if (avatarCard == null) {
                    //if not in common cards, check variant cards
                    avatarCard = FModel.getMagicDb().getVariantCards().getCard(avatar);
                }
            }
        }
        return avatarCard;
    }

    public String getOpponentName() {
        if (avatar == null) {
            return name;
        }
        String name = avatar;
        int idx = name.indexOf(',');
        if (idx != -1) { //trim everything after the comma
            name = name.substring(0, idx);
        }
        return name;
    }

    public ConquestBattle createBattle(ConquestLocation location0, int tier0) {
        return new ConquestEventBattle(location0, tier0);
    }

    public static class Reader extends FCollectionReader<ConquestEvent> {
        private final ConquestRegion region;

        public Reader(ConquestRegion region0) {
            super(region0.getPlane().getDirectory() + region0.getName() + ForgeConstants.PATH_SEPARATOR + "_events.txt");
            region = region0;
        }

        @Override
        protected ConquestEvent read(String line) {
            String name = null;
            String deck = null;
            Set<GameType> variants = EnumSet.noneOf(GameType.class);
            String avatar = null;
            String description = null;
            String tempUnlock = null;

            String key, value;
            String[] pieces = line.split("\\|");
            for (String piece : pieces) {
                int idx = piece.indexOf(':');
                if (idx != -1) {
                    key = piece.substring(0, idx).trim().toLowerCase();
                    value = piece.substring(idx + 1).trim();
                }
                else {
                    alertInvalidLine(line, "Invalid event definition.");
                    key = piece.trim().toLowerCase();
                    value = "";
                }
                switch(key) {
                case "name":
                    name = value;
                    break;
                case "deck":
                    deck = value;
                    break;
                case "variant":
                    if (!value.equalsIgnoreCase("none")) {
                        for (String variantName : value.split(",")) {
                            try {
                                variants.add(GameType.valueOf(variantName));
                            }
                            catch (Exception ex) {
                                System.out.println(variantName + " is not a valid variant");
                            }
                        }
                    }
                    break;
                case "avatar":
                    avatar = value;
                    break;
                case "desc":
                    description = value;
                    break;
                case "temporaryunlock":
                    tempUnlock = value;
                    break;
                default:
                    alertInvalidLine(line, "Invalid event definition.");
                    break;
                }
            }
            if (deck == null) {
                deck = name + ".dck"; //assume deck file has same name if not specified
            }
            deck = file.getParent() + ForgeConstants.PATH_SEPARATOR + deck;
            return new ConquestEvent(region, name, description, deck, variants, avatar, tempUnlock);
        }
    }

    public class ConquestEventBattle extends ConquestBattle {
        private ConquestEventBattle(ConquestLocation location0, int tier0) {
            super(location0, tier0);
        }

        @Override
        protected Deck buildOpponentDeck() {
            return ConquestEvent.this.getDeck();
        }

        @Override
        public String getEventName() {
            return ConquestEvent.this.getName();
        }

        @Override
        public String getOpponentName() {
            return ConquestEvent.this.getOpponentName();
        }

        @Override
        public PaperCard getPlaneswalker() {
            PaperCard avatarCard = ConquestEvent.this.getAvatarCard();
            if (avatarCard != null && avatarCard.getRules().getType().isPlaneswalker()) {
                return avatarCard;
            }
            return null;
        }

        @Override
        public void setOpponentAvatar(LobbyPlayer aiPlayer, IGuiGame gui) {
            PaperCard avatarCard = ConquestEvent.this.getAvatarCard();
            if (avatarCard != null) {
                aiPlayer.setAvatarCardImageKey(avatarCard.getImageKey(false));
            }
        }

        @Override
        public Set<GameType> getVariants() {
            return ConquestEvent.this.getVariants();
        }

        @Override
        public int gamesPerMatch() {
            return 1; //event battles are single game
        }
    }

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

    public enum ChaosWheelOutcome {
        BOOSTER,
        DOUBLE_BOOSTER,
        SHARDS,
        DOUBLE_SHARDS,
        PLANESWALK,
        CHAOS;

        private static final ChaosWheelOutcome[] wheelSpots = new ChaosWheelOutcome[] {
            CHAOS, BOOSTER, SHARDS, DOUBLE_BOOSTER, PLANESWALK, BOOSTER, DOUBLE_SHARDS, BOOSTER
        };
        private static final float ANGLE_PER_SPOT = 360f / wheelSpots.length;

        public static ChaosWheelOutcome getWheelOutcome(float wheelRotation) {
            if (wheelRotation < 0) {
                wheelRotation += 360f;
            }
            int spot = (int)(wheelRotation / ANGLE_PER_SPOT);
            return wheelSpots[spot];
        }
    }
}
