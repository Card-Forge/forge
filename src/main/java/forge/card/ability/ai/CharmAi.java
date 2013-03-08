package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.math.RandomUtils;

import forge.card.ability.SpellAbilityAi;
import forge.card.ability.effects.CharmEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.util.MyRandom;

public class CharmAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();

        final int num = Integer.parseInt(sa.hasParam("CharmNum") ? sa.getParam("CharmNum") : "1");
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;
        boolean timingRight = sa.isTrigger(); //is there a reason to play the charm now?

        List<AbilitySub> chooseFrom = CharmEffect.makePossibleOptions(sa);
        List<AbilitySub> chosenList = chooseOptionsAi(ai, timingRight, chooseFrom, num, min, false);

        if (chosenList == null || chosenList.isEmpty()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        return r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
    }

    public static List<AbilitySub> chooseOptionsAi(final AIPlayer ai, boolean playNow, List<AbilitySub> choices, int num, int min, boolean opponentChoser) {
        List<AbilitySub> chosenList = new ArrayList<AbilitySub>();

        if (opponentChoser) {
            // This branch is for "An Opponent chooses" Charm spells from Alliances
            // Current just choose the first available spell, which seem generally less disastrous for the AI.
            //return choices.subList(0, 1);
            return choices.subList(1, choices.size());
        }
        
        
        for (int i = 0; i < num; i++) {
            AbilitySub thisPick = null;
            for (SpellAbility sub : choices) {
                sub.setActivatingPlayer(ai);
                if (!playNow && sub.canPlayAI()) {
                    thisPick = (AbilitySub) sub;
                    choices.remove(sub);
                    playNow = true;
                    break;
                }
                if ((playNow || i < num - 1) && sub.doTrigger(false, ai)) {
                    thisPick = (AbilitySub) sub;
                    choices.remove(sub);
                    break;
                }
            }
            if (thisPick != null) {
                chosenList.add(thisPick);
            }
        }
        return chosenList.size() >= min ? chosenList : null;
    }
    
    public static Player determineOpponentChooser(AIPlayer ai, SpellAbility sa, List<Player> opponents) {
        return opponents.get(RandomUtils.nextInt(opponents.size()));
    }
    
}
