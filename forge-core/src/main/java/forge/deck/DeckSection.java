package forge.deck;

import forge.card.CardRulesPredicates;
import forge.card.CardType;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.Localizer;

import java.util.function.Predicate;

public enum DeckSection {
    Main("lblMainDeck", Validators.DECK_AND_SIDE_VALIDATOR),
    Sideboard("lblSideboard", Validators.DECK_AND_SIDE_VALIDATOR),
    Commander("lblCommander", Validators.COMMANDER_VALIDATOR),
    Avatar("lblAvatar", PaperCardPredicates.fromRules(CardRulesPredicates.IS_VANGUARD)),
    Planes("lblPlanarDeck", PaperCardPredicates.fromRules(CardRulesPredicates.IS_PLANE_OR_PHENOMENON)),
    Schemes("lblSchemeDeck", PaperCardPredicates.fromRules(CardRulesPredicates.IS_SCHEME)),
    Conspiracy("lblConspiracies", PaperCardPredicates.fromRules(CardRulesPredicates.IS_CONSPIRACY)),
    Dungeon("lblDungeons", PaperCardPredicates.fromRules(CardRulesPredicates.IS_DUNGEON)),
    Attractions("lblAttractions", PaperCardPredicates.fromRules(CardRulesPredicates.IS_ATTRACTION)),
    Contraptions("lblContraptions", PaperCardPredicates.fromRules(CardRulesPredicates.IS_CONTRAPTION));

    /**
     * Array of DeckSections that contain nontraditional cards.
     */
    public static final DeckSection[] NONTRADITIONAL_SECTIONS = new DeckSection[]{Avatar, Planes, Schemes, Conspiracy, Dungeon, Attractions, Contraptions};

    private final String nameLbl;
    private final Predicate<PaperCard> fnValidator;

    DeckSection(String nameLbl, Predicate<PaperCard> validator) {
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
        return fnValidator.test(card);
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
        static final Predicate<PaperCard> DECK_AND_SIDE_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            // NOTE: Same rules applies to both Deck and Side, despite "Conspiracy cards" are allowed
            // in the SideBoard (see Rule 313.2)
            // Those will be matched later, in case (see `Deck::normalizeDeferredSections`)
            return !t.isConspiracy() && !t.isDungeon() && !t.isPhenomenon() && !t.isPlane() && !t.isScheme() && !t.isVanguard();
        };

        static final Predicate<PaperCard> COMMANDER_VALIDATOR = card -> {
            CardType t = card.getRules().getType();
            return card.getRules().canBeCommander() || t.isPlaneswalker() || card.getRules().canBeOathbreaker() || card.getRules().canBeSignatureSpell();
        };

    }
}
