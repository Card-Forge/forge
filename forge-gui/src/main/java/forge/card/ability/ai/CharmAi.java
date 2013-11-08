package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import forge.card.ability.SpellAbilityAi;
import forge.card.ability.effects.CharmEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.Aggregates;
import forge.util.MyRandom;

public class CharmAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();

        final int num = Integer.parseInt(sa.hasParam("CharmNum") ? sa.getParam("CharmNum") : "1");
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;
        boolean timingRight = sa.isTrigger(); //is there a reason to play the charm now?

        
        List<AbilitySub> chosenList = chooseOptionsAi(sa, ai, timingRight, num, min, false);

        if (chosenList.isEmpty()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        return r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
    }

    public static List<AbilitySub> chooseOptionsAi(SpellAbility sa, final Player ai, boolean playNow, int num, int min, boolean opponentChoser) {
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
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
        if (playNow && chosenList.size() < min) {
            for (int i = 0; i < min; i++) {
                AbilitySub thisPick = null;
                for (SpellAbility sub : choices) {
                    sub.setActivatingPlayer(ai);
                    if (sub.doTrigger(true, ai)) {
                        thisPick = (AbilitySub) sub;
                        choices.remove(sub);
                        break;
                    }
                }
                if (thisPick != null) {
                    chosenList.add(thisPick);
                }
            }
        }
        return chosenList;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List)
     */
    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, List<Player> opponents) {
        return Aggregates.random(opponents);
    }
    
}
