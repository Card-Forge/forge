package forge.game;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.Enums;
import com.google.common.base.Function;

import forge.StaticData;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.player.RegisteredPlayer;
import forge.util.Localizer;
import forge.util.MyRandom;

public enum GameType {

    Sealed          (DeckFormat.Limited, true, true, true, "lblSealed", ""),
    Draft           (DeckFormat.Limited, true, true, true, "lblDraft", ""),
    Winston         (DeckFormat.Limited, true, true, true, "lblWinston", ""),
    Gauntlet        (DeckFormat.Constructed, false, true, true, "lblGauntlet", ""),
    Tournament      (DeckFormat.Constructed, false, true, true, "lblTournament", ""),
    Quest           (DeckFormat.QuestDeck, true, true, false, "lblQuest", ""),
    QuestDraft      (DeckFormat.Limited, true, true, true, "lblQuestDraft", ""),
    PlanarConquest  (DeckFormat.PlanarConquest, true, false, false, "lblPlanarConquest", ""),
    Puzzle          (DeckFormat.Puzzle, false, false, false, "lblPuzzle", "lblPuzzleDesc"),
    Constructed     (DeckFormat.Constructed, false, true, true, "lblConstructed", ""),
    DeckManager     (DeckFormat.Constructed, false, true, true, "lblDeckManager", ""),
    Vanguard        (DeckFormat.Vanguard, true, true, true, "lblVanguard", "lblVanguardDesc"),
    Commander       (DeckFormat.Commander, false, false, false, "lblCommander", "lblCommanderDesc"),
    Oathbreaker     (DeckFormat.Oathbreaker, false, false, false, "lblOathbreaker", "lblOathbreakerDesc"),
    TinyLeaders     (DeckFormat.TinyLeaders, false, false, false, "lblTinyLeaders", "lblTinyLeadersDesc"),
    Brawl           (DeckFormat.Brawl, false, false, false, "lblBrawl", "lblBrawlDesc"),
    Planeswalker    (DeckFormat.PlanarConquest, false, false, true, "lblPlaneswalker", "lblPlaneswalkerDesc"),
    Planechase      (DeckFormat.Planechase, false, false, true, "lblPlanechase", "lblPlanechaseDesc"),
    Archenemy       (DeckFormat.Archenemy, false, false, true, "lblArchenemy", "lblArchenemyDesc"),
    ArchenemyRumble (DeckFormat.Archenemy, false, false, true, "lblArchenemyRumble", "lblArchenemyRumbleDesc"),
    MomirBasic      (DeckFormat.Constructed, false, false, false, "lblMomirBasic", "lblMomirBasicDesc", new Function<RegisteredPlayer, Deck>() {
        @Override
        public Deck apply(RegisteredPlayer player) {
            Deck deck = new Deck();
            CardPool mainDeck = deck.getMain();
            String setcode = StaticData.instance().getBlockLands().get(MyRandom.getRandom().nextInt(StaticData.instance().getBlockLands().size()));
            mainDeck.add("Plains", setcode, 12, true);
            mainDeck.add("Island", setcode, 12, true);
            mainDeck.add("Swamp", setcode, 12, true);
            mainDeck.add("Mountain", setcode, 12, true);
            mainDeck.add("Forest", setcode, 12, true);
            deck.getOrCreate(DeckSection.Avatar).add(StaticData.instance().getVariantCards()
                    .getCard("Momir Vig, Simic Visionary Avatar"), 1);
            return deck;
        }
    }),
    MoJhoSto      (DeckFormat.Constructed, false, false, false, "lblMoJhoSto", "lblMoJhoStoDesc", new Function<RegisteredPlayer, Deck>() {
        @Override
        public Deck apply(RegisteredPlayer player) {
            Deck deck = new Deck();
            CardPool mainDeck = deck.getMain();
            String setcode = StaticData.instance().getBlockLands().get(MyRandom.getRandom().nextInt(StaticData.instance().getBlockLands().size()));
            mainDeck.add("Plains", setcode, 12, true);
            mainDeck.add("Island", setcode, 12, true);
            mainDeck.add("Swamp", setcode, 12, true);
            mainDeck.add("Mountain", setcode, 12, true);
            mainDeck.add("Forest", setcode, 12, true);
            deck.getOrCreate(DeckSection.Avatar).add(StaticData.instance().getVariantCards()
                    .getCard("Momir Vig, Simic Visionary Avatar"), 1);
            deck.getOrCreate(DeckSection.Avatar).add(StaticData.instance().getVariantCards()
                    .getCard("Jhoira of the Ghitu Avatar"), 1);
            deck.getOrCreate(DeckSection.Avatar).add(StaticData.instance().getVariantCards()
                    .getCard("Stonehewer Giant Avatar"), 1);
            return deck;
        }
    });

    private final DeckFormat deckFormat;
    private final boolean isCardPoolLimited, canSideboard, addWonCardsMidGame;
    private final String name, description;
    private final Function<RegisteredPlayer, Deck> deckAutoGenerator;

    GameType(DeckFormat deckFormat0, boolean isCardPoolLimited0, boolean canSideboard0, boolean addWonCardsMidgame0, String name0, String description0) {

        this(deckFormat0, isCardPoolLimited0, canSideboard0, addWonCardsMidgame0, name0, description0, null);
    }
    GameType(DeckFormat deckFormat0, boolean isCardPoolLimited0, boolean canSideboard0, boolean addWonCardsMidgame0, String name0, String description0, Function<RegisteredPlayer, Deck> deckAutoGenerator0) {
        final Localizer localizer = forge.util.Localizer.getInstance();
        deckFormat = deckFormat0;
        isCardPoolLimited = isCardPoolLimited0;
        canSideboard = canSideboard0;
        addWonCardsMidGame = addWonCardsMidgame0;
        name = localizer.getMessage(name0);
        if (description0.length()>0) {
            description0 = localizer.getMessage(description0);
        }
        description = description0;
        deckAutoGenerator = deckAutoGenerator0;
    }

    /**
     * @return the decksFormat
     */
    public DeckFormat getDeckFormat() {
        return deckFormat;
    }

    public boolean isAutoGenerated() {
        return deckAutoGenerator != null;
    }

    public Deck autoGenerateDeck(RegisteredPlayer player) {
        return deckAutoGenerator.apply(player);
    }

    /**
     * @return the isCardpoolLimited
     */
    public boolean isCardPoolLimited() {
        return isCardPoolLimited;
    }

    /**
     * @return the canSideboard
     */
    public boolean isSideboardingAllowed() {
        return canSideboard;
    }

    public boolean canAddWonCardsMidGame() {
        return addWonCardsMidGame;
    }

    public boolean isCommandZoneNeeded() {
    	return true; //TODO: Figure out way to move command zone into field so it can be hidden when empty
        /*switch (this) {
        case Archenemy:
        case Commander:
        case Oathbreaker:
        case TinyLeaders:
        case Planechase:
        case Vanguard:
            return true;
        default:
            return false;
        }*/
    }

    public String toString() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static GameType smartValueOf(String name) {
        return Enums.getIfPresent(GameType.class, name).orNull();
    }

    public static Set<GameType> listValueOf(final String values) {
        final Set<GameType> result = EnumSet.noneOf(GameType.class);
        for (final String s : values.split(",")) {
            GameType g = GameType.smartValueOf(s);
            if (g != null) {
                result.add(g);
            }
        }
        return result;
    }
}
