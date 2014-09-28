package forge.game.player;

import java.util.HashSet;

import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

//class for storing information during a game that is used at the end of the game to determine achievements
public class AchievementTracker {
    public final HashSet<String> activatedUltimates = new HashSet<String>();
    public final HashSet<String> challengesCompleted = new HashSet<String>();
    public int mulliganTo = 7;
    public int spellsCast = 0;
    public int maxStormCount = 0;
    public int landsPlayed = 0;

    public void onSpellAbilityPlayed(SpellAbility sa) {
        final Card card = sa.getHostCard();
        if (sa.getRestrictions().isPwAbility() && sa.hasParam("Ultimate")) {
            activatedUltimates.add(card.getName());
        }
        if (card.determineColor().getColor() == MagicColor.ALL_COLORS) {
            challengesCompleted.add("Chromatic");
        }
    }

    public void onSpellResolve(SpellAbility spell) {
        final Card card = spell.getHostCard();
        if (card.hasKeyword("Epic")) {
            challengesCompleted.add("Epic");
        }
    }
}
