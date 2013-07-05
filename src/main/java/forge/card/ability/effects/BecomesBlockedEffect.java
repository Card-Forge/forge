package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.card.trigger.TriggerType;
import forge.game.Game;

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

        final Game game = sa.getActivatingPlayer().getGame();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        for (final Card c : getTargetCards(sa)) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                game.getCombat().setBlocked(c, true);
                if (!c.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Attacker", c);
                    runParams.put("Blockers", new ArrayList<Card>());
                    runParams.put("NumBlockers", 0);
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);

                    // Bushido
                    for (final SpellAbility ab : CardFactoryUtil.getBushidoEffects(c)) {
                        game.getStack().add(ab);
                    }
                }
            }
        }

    }
}
