package forge.deck;

import forge.card.CardType;
import forge.item.PaperCard;

import java.util.function.Function;

public enum DeckSection {
    Avatar(1, Validators.AVATAR_VALIDATOR),
    Commander(1, Validators.COMMANDER_VALIDATOR),
    Main(60, Validators.DECK_AND_SIDE_VALIDATOR),
    Sideboard(15, Validators.DECK_AND_SIDE_VALIDATOR),
    Planes(10, Validators.PLANES_VALIDATOR),
    Schemes(20, Validators.SCHEME_VALIDATOR),
    Conspiracy(0, Validators.CONSPIRACY_VALIDATOR),
    Dungeon(0, Validators.DUNGEON_VALIDATOR);

    private final int typicalSize; // Rules enforcement is done in DeckFormat class, this is for reference only
    private Function<PaperCard, Boolean> fnValidator;

    DeckSection(int commonSize, Function<PaperCard, Boolean> validator){
        this.typicalSize = commonSize;
        fnValidator = validator;
    }
    
    public boolean isSingleCard() { return typicalSize == 1; }

    public boolean validate(PaperCard card){
        if (fnValidator == null) return true;
        return fnValidator.apply(card);
    }

    // Returns the matching section for "special"/supplementary core types.
    public static DeckSection matchingSection(PaperCard card){
       if (DeckSection.Conspiracy.validate(card))
            return Conspiracy;
        if (DeckSection.Schemes.validate(card))
            return Schemes;
        if (DeckSection.Avatar.validate(card))
            return Avatar;
        if (DeckSection.Planes.validate(card))
            return Planes;
        if (DeckSection.Commander.validate(card))
            return Commander;
        if (DeckSection.Dungeon.validate(card))
            return Dungeon;
        return Main;  // default
    }

    public static DeckSection smartValueOf(String value) {
        if (value == null)
            return null;
        final String valToCompare = value.trim();
        for (final DeckSection v : DeckSection.values()) {
            if (v.name().compareToIgnoreCase(valToCompare) == 0) {
                return v;
            }
        }
        return null;
    }

    private static class Validators {
        static final Function<PaperCard, Boolean> DECK_AND_SIDE_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                // NOTE: Same rules applies to both Deck and Side, despite "Conspiracy cards" are allowed
                // in the SideBoard (see Rule 313.2)
                // Those will be matched later, in case (see `Deck::validateDeferredSections`)
                return (!t.isConspiracy() && !t.isDungeon() && !t.isPhenomenon() && !t.isPlane() && !t.isScheme() &&
                        !t.isVanguard());
            }
        };

        static final Function<PaperCard, Boolean> COMMANDER_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return (t.isPlaneswalker() || (t.isCreature() && t.isLegendary()));
            }
        };

        static final Function<PaperCard, Boolean> PLANES_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return (t.isPlane() || t.isPhenomenon());
            }
        };

        static final Function<PaperCard, Boolean> DUNGEON_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return t.isDungeon();
            }
        };

        static final Function<PaperCard, Boolean> SCHEME_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return (t.isScheme());
            }
        };

        static final Function<PaperCard, Boolean> CONSPIRACY_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return (t.isConspiracy());
            }
        };

        static final Function<PaperCard, Boolean> AVATAR_VALIDATOR = new Function<PaperCard, Boolean>() {
            @Override
            public Boolean apply(PaperCard card) {
                CardType t = card.getRules().getType();
                return ((t.isCreature() && t.hasSubtype("Avatar")) || t.isVanguard());
            }
        };

    }
}
