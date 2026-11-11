package forge.gamemodes.rogue;

/**
 * Shared hint catalog for keyword markers embedded in Rogue description text.
 */
public enum KeywordHint {
    ITEM("Item",
            "An Artifact added to your command zone as a carry card. It persists between matches unless it ends a match neither in the command zone nor on the battlefield."),
    FELLOW("Fellow",
        "A Creature added to your command zone as a carry card. It persists between matches unless it ends a match neither in the command zone nor on the battlefield."),
    SCROLL("Scroll",
        "An Instant or Sorcery added to your command zone as a carry card. It persists between matches unless it ends a match outside the command zone."),
    TRAIT("Trait",
        "A permanent effect that lasts for the rest of the Run, or until its last charge is consumed."),
    AETHERWORK("Aetherwork",
        "A building or machine reconstructed in the Aether that strengthens future Runs while active."),
    WOUND("Wound",
            "A permanent negative effect (Trait) that lasts for the rest of the Run unless treated at a Sanctum."),
    WRATHFUL("Wrathful",
            "For each instance of Wrathful, the Planebound gets a minor advantage for the match."),
    CURSED("Cursed",
            "For each instance of Cursed, the Planebound gets a powerful advantage for the match."),
    BAZAAR("Bazaar",
            "Spend your gold on cards and special offers."),
    SANCTUM("Sanctum",
            "Rest to regain life and cure all wounds or cook a random food item."),
    CHEST("Chest",
        "Contains a random reward, like gold, cards from your Reward Pool, or permanent buffs that last for the rest of the Run."),
    SIDE_NODE("Side Node",
        "A location or encounter on the Map in between matches, like a Sanctum, Event or Chest."),
    GOLD("Gold",
        "Currency to spend during the Run, like for buying cards at a Bazaar or rerolling rewards."),
    ECHOES("Echoes",
        "Persistent memories of the Aether used to build and upgrade Aetherworks."),
    REMOVAL_CREDITS("Removal Credits",
        "Earned for each card added to your deck from a Card Reward or Bazaar. Can be spent to remove cards from your Deck. Basic lands can always be removed."),
    PLANEBOUND("Planebound",
        "The opponent you will face in a match dedicated to a specific Plane Node on the Map. Neither you nor the Planebound can planeswalk away from the Plane during the match."),
    CARD_REWARD("Card Reward",
        "A reward you receive after winning a Match during the Run. A default Card Reward consists of 6 non-mythic and 1 mythic Card from your Reward Pool and you can add up to 3 Cards to your deck."),
    REWARD_POOL("Reward Pool",
        "The Commander-specific card pool where cards are drawn from for Card Rewards and Bazaars."),
    MAX_LIFE("Max. Life",
            "Your life can only exceed Max-Life during a match.");

    private final String token;
    private final String hintText;

    KeywordHint(String token, String hintText) {
        this.token = token;
        this.hintText = hintText;
    }

    public String getToken() {
        return token;
    }

    public String getTitle() {
        return token;
    }

    public String getHintText() {
        return hintText;
    }

    public static KeywordHint fromToken(String token) {
        if (token == null) {
            return null;
        }
        for (KeywordHint hint : values()) {
            if (hint.token.equals(token)) {
                return hint;
            }
        }
        return null;
    }
}
