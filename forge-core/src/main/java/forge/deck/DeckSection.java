package forge.deck;

import forge.card.CardType;
import forge.item.PaperCard;
import forge.util.Localizer;

import java.util.function.Function;

public enum DeckSection {
    Main("lblMainDeck", Validators.DECK_AND_SIDE_VALIDATOR),
    Sideboard("lblSideboard", Validators.DECK_AND_SIDE_VALIDATOR),
    Commander("lblCommander", Validators.COMMANDER_VALIDATOR),
    Avatar("lblAvatar", Validators.AVATAR_VALIDATOR),
    Planes("lblPlanarDeck", Validators.PLANES_VALIDATOR),
    Schemes("lblSchemeDeck", Validators.SCHEME_VALIDATOR),
    Conspiracy("lblConspiracies", Validators.CONSPIRACY_VALIDATOR),
    Dungeon("lblDungeons", Validators.DUNGEON_VALIDATOR),
    Attractions("lblAttractions", Validators.ATTRACTION_VALIDATOR),
    Contraptions("lblContraptions", Validators.CONTRAPTION_VALIDATOR);

    /**
     * Array of DeckSections that contain nontraditional cards.
     */
    public static final DeckSection[] NONTRADITIONAL_SECTIONS = new DeckSection[]{Avatar, Planes, Schemes, Conspiracy, Dungeon, Attractions, Contraptions};

    private final String nameLbl;
    private final Function<PaperCard, Boolean> fnValidator;

    DeckSection(String nameLbl, Function<PaperCard, Boolean> validator) {
        this.nameLbl = nameLbl;
        fnValidator = validator;
    }

    public String getLocalizedName() {
        return Localizer.getInstance().getMessage(this.nameLbl);
    }

    public String getLocalizedShortName() {
        String shortNameLabel;
        switch(this) {
            case Main: shortNameLabel = "lblMain"; break;
            case Sideboard: shortNameLabel = "lblSide"; break;
            case Planes: shortNameLabel = "lblPlanes"; break;
            case Schemes: shortNameLabel = "lblSchemes"; break;
            default: return getLocalizedName();
        }
        return Localizer.getInstance().getMessage(shortNameLabel);
    }

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
        if (DeckSection.Dungeon.validate(card))
            return Dungeon;
        if (DeckSection.Attractions.validate(card))
            return Attractions;
        if (DeckSection.Contraptions.validate(card))
            return Contraptions;
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
        static final Function<PaperCard, Boolean> DECK_AND_SIDE_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            // NOTE: Same rules applies to both Deck and Side, despite "Conspiracy cards" are allowed
            // in the SideBoard (see Rule 313.2)
            // Those will be matched later, in case (see `Deck::validateDeferredSections`)
            return !t.isConspiracy() && !t.isDungeon() && !t.isPhenomenon() && !t.isPlane() && !t.isScheme() && !t.isVanguard();
        };

        static final Function<PaperCard, Boolean> COMMANDER_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return card.getRules().canBeCommander() || t.isPlaneswalker() || card.getRules().canBeOathbreaker() || card.getRules().canBeSignatureSpell();
        };

        static final Function<PaperCard, Boolean> PLANES_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isPlane() || t.isPhenomenon();
        };

        static final Function<PaperCard, Boolean> DUNGEON_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isDungeon();
        };

        static final Function<PaperCard, Boolean> SCHEME_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isScheme();
        };

        static final Function<PaperCard, Boolean> CONSPIRACY_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isConspiracy();
        };

        static final Function<PaperCard, Boolean> AVATAR_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isVanguard();
        };

        static final Function<PaperCard, Boolean> ATTRACTION_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isAttraction();
        };

        static final Function<PaperCard, Boolean> CONTRAPTION_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return t.isContraption();
        };

    }
}
