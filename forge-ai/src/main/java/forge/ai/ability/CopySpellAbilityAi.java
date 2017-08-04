package forge.ai.ability;

import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class CopySpellAbilityAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // the AI should not miss mandatory activations (e.g. Precursor Golem trigger)
        return sa.isMandatory() || "Always".equals(sa.getParam("AILogic"));
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        // the AI should not miss mandatory activations (e.g. Precursor Golem trigger)
        return mandatory || "Always".equals(sa.getParam("AILogic"));
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        // NOTE: Other SAs that use CopySpellAbilityAi (e.g. Chain Lightning) are currently routed through
        // generic method SpellAbilityAi#chkDrawbackWithSubs and are handled there.
        if ("ChainOfSmog".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfSmog.consider(aiPlayer, sa);
        } else if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(aiPlayer, sa);
        }

        return super.chkAIDrawback(sa, aiPlayer);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells) {
        return spells.get(0);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // Chain of Acid requires special attention here since otherwise the AI will confirm the copy and then
        // run into the necessity of confirming a mandatory Destroy, thus destroying all of its own permanents.
        if ("ChainOfAcid".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.ChainOfAcid.consider(player, sa);
        }

        return true;
    }

}

