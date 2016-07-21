package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.event.GameEventCombatChanged;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.TriggerType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BecomesBlockedEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(" becomes blocked.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        boolean isCombatChanged = false;
        final Game game = sa.getActivatingPlayer().getGame();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        for (final Card c : getTargetCards(sa)) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                game.getCombat().setBlocked(c, true);
                if (!c.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                    isCombatChanged = true;
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Attacker", c);
                    runParams.put("Blockers", new ArrayList<Card>());
                    runParams.put("NumBlockers", 0);
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);
                }
            }
        }

        if (isCombatChanged) {
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
