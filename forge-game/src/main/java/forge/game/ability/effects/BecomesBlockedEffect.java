package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCombatChanged;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

public class BecomesBlockedEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(" becomes blocked.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getActivatingPlayer().getGame();
        List<Card> blocked = Lists.newArrayList();
        for (final Card c : getTargetCards(sa)) {
            game.getCombat().setBlocked(c, true);
            if (!c.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                blocked.add(c);
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Attacker, c);
                runParams.put(AbilityKey.Blockers, Lists.<Card>newArrayList());
                runParams.put(AbilityKey.Defender, game.getCombat().getDefenderByAttacker(c));
                runParams.put(AbilityKey.DefendingPlayer, game.getCombat().getDefenderPlayerByAttacker(c));
                game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);
            }
        }

        if (!blocked.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Attackers, blocked);
            game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedOnce, runParams, false);
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
