package forge.adventure.data;

import forge.card.CardRarity;
import forge.deck.Deck;
import forge.game.GameFormat;
import forge.game.GameFormat.FormatSubType;
import forge.game.GameFormat.FormatType;

import java.util.ArrayList;
import java.util.List;

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

    /** The rarities a player can pick as the highest, lowest first. Basic lands are always allowed. */
    public static final CardRarity[] RARITIES = {CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare};

    /** @param value the saved setting: the highest rarity allowed */
    public String describeValue(String value) {
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
        return rules;
    }
}
