package forge.adventure.data;

import forge.StaticData;
import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.Aggregates;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * An optional rule a player can turn on for a whole run when starting a new game. A modifier can add an effect to
 * every duel, like the plane's global effect, and can limit the cards the player's deck may use in duels.
 */
public class ModifierData {
    /** A rule implemented in code. The limits have a setting the player picks. */
    public enum Type {
        RarityLimit, //The highest rarity the deck's cards may have.
        ColorLimit,  //The colors the deck may use.
        CardLoss     //Losing a fight costs cards from the deck, set by lostCards and loseCommander.
    }

    public String name;
    public String description;
    public String icon;            //Name of an item icon shown beside the modifier, such as "BlackRune".
    public Type type;              //Optional rule implemented in code.
    public int lostCards;          //CardLoss: random nonbasic cards lost from the main deck.
    public boolean loseCommander;  //CardLoss: the deck's commanders are lost too.
    public EffectData effect;      //Added to every duel, written for the enemies like the plane's global effect.

    /** The rarities a player can pick as the highest, lowest first. Basic lands are always allowed. */
    public static final CardRarity[] RARITIES = {CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare};

    /** The value saved for the modifier when the player turns it on without changing its settings. */
    public String defaultValue(ColorSet startingColors) {
        if (type == Type.RarityLimit)
            return RARITIES[0].name();
        if (type == Type.ColorLimit)
            return startingColors.toString();
        return "";
    }

    /** @return a typed setting in its saved form, or null when it isn't valid for this modifier */
    public String parseValue(String input) {
        if (type == Type.RarityLimit) {
            for (CardRarity rarity : RARITIES) {
                if (rarity.name().equalsIgnoreCase(input))
                    return rarity.name();
            }
            return null;
        }
        if (type == Type.ColorLimit)
            return input.matches("(?i)[WUBRG]+") ? ColorSet.fromNames(input.toUpperCase().toCharArray()).toString() : null;
        return null;
    }

    /** @param value the saved setting: the highest rarity allowed, or the allowed colors as letters such as "WG" */
    public String describeValue(String value) {
        if (type == Type.ColorLimit) {
            List<String> names = new ArrayList<>();
            for (MagicColor.Color color : ColorSet.fromNames(value.toCharArray()).getOrderedColors())
                names.add(color.getTranslatedName());
            return String.join(", ", names);
        }
        return value;
    }

    /** @return the cards a duel deck may keep under this modifier's limit with the saved setting */
    public Predicate<PaperCard> allowedCards(String value) {
        if (type == Type.RarityLimit) {
            List<CardRarity> rarities = new ArrayList<>(List.of(CardRarity.BasicLand));
            for (CardRarity rarity : RARITIES) {
                rarities.add(rarity);
                if (rarity.name().equals(value))
                    break;
            }
            //A card is allowed when any of its printings was at an allowed rarity.
            return card -> rarities.stream().anyMatch(rarity -> StaticData.instance().getCommonCards().wasPrintedAtRarity(rarity).test(card));
        }
        if (type == Type.ColorLimit) {
            ColorSet colors = ColorSet.fromNames(value.toCharArray());
            return card -> card.getRules().getColorIdentity().hasNoColorsExcept(colors);
        }
        return card -> true;
    }

    /** @return the cards the player loses from the deck by losing a fight under this modifier, picked at random */
    public List<PaperCard> lostCards(Deck deck) {
        List<PaperCard> lost = new ArrayList<>();
        if (type != Type.CardLoss)
            return lost;
        if (loseCommander)
            lost.addAll(deck.getCommanders());
        List<PaperCard> nonbasic = deck.getMain().toFlatList();
        nonbasic.removeIf(card -> card.getRules().getType().isBasicLand());
        lost.addAll(Aggregates.random(nonbasic, lostCards));
        return lost;
    }
}
