package forge.adventure.data;

import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.GameFormat;
import forge.game.GameFormat.FormatSubType;
import forge.game.GameFormat.FormatType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An optional rule a player can turn on for a whole run when starting a new game. A challenge can add an effect to
 * every duel, like the plane's global effect, and can require the player's deck to meet deck rules.
 */
public class ChallengeData {
    public String name;
    public String description;
    public String icon;            //Name of an item icon shown beside the challenge, such as "BlackRune".
    public EffectData effect;      //Added to every duel, written for the enemies like the plane's global effect.
    public boolean maxRarity;      //The player picks the highest rarity the deck's cards may have.
    public boolean pickColors;     //The player picks the colors the deck may use.

    /** The rarities a player can pick as the highest, lowest first. Basic lands are always allowed. */
    public static final CardRarity[] RARITIES = {CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare};

    /** The value saved for the challenge when the player turns it on without changing its settings. */
    public String defaultValue(ColorSet startingColors) {
        if (maxRarity)
            return RARITIES[0].name();
        if (pickColors)
            return startingColors.toString();
        return "";
    }

    /** @param value the saved setting: the highest rarity allowed, or the allowed colors as letters such as "WG" */
    public String describeValue(String value) {
        if (pickColors) {
            List<String> names = new ArrayList<>();
            for (MagicColor.Color color : ColorSet.fromNames(value.toCharArray()).getOrderedColors())
                names.add(color.getTranslatedName());
            return String.join(", ", names);
        }
        return value;
    }

    /** @return the problems of the deck under this challenge, empty when the deck is allowed */
    public List<String> deckProblems(Deck deck, String value) {
        List<String> problems = new ArrayList<>();
        for (GameFormat rules : deckRules(value)) {
            String problem = rules.getDeckConformanceProblem(deck);
            if (problem != null)
                problems.add(problem);
        }
        return problems;
    }

    private List<GameFormat> deckRules(String value) {
        List<GameFormat> rules = new ArrayList<>();
        if (maxRarity) {
            List<CardRarity> rarities = new ArrayList<>(List.of(CardRarity.BasicLand));
            for (CardRarity rarity : RARITIES) {
                rarities.add(rarity);
                if (rarity.name().equals(value))
                    break;
            }
            rules.add(new GameFormat(name, null, null, null, null, false, null, rarities, 0, FormatType.CUSTOM, FormatSubType.CUSTOM));
        }
        if (pickColors) {
            GameFormat.DeckRules colors = new GameFormat.DeckRules(false, 0, 0, Map.of(), ColorSet.fromNames(value.toCharArray()));
            rules.add(new GameFormat(name, null, null, null, null, false, null, null, 0, FormatType.CUSTOM, FormatSubType.CUSTOM, colors));
        }
        return rules;
    }
}
