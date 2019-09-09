package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.Game;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.*;

public class BlockEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Combat combat = game.getPhaseHandler().getCombat();

        List<Card> attackers = new ArrayList<>();
        if (sa.hasParam("DefinedAttacker")) {
            for (final Card attacker : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa)) {
                if (combat.isAttacking(attacker))
                    attackers.add(attacker);
            }
        }

        List<Card> blockers = new ArrayList<>();
        if (sa.hasParam("DefinedBlocker")) {
            for (final Card blocker : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedBlocker"), sa)) {
                if (blocker.isCreature() && blocker.isInZone(ZoneType.Battlefield))
                    blockers.add(blocker);
            }
        }

        if (attackers.size() == 0 || blockers.size() == 0) return;

        for (final Card attacker : attackers) {
            final boolean wasBlocked = combat.isBlocked(attacker);

            for (final Card blocker : blockers) {
                if (combat.isBlocking(blocker, attacker)) continue;

                // If the attacker was blocked, this covers adding the blocker to the damage assignment
                combat.addBlocker(attacker, blocker);
                combat.orderAttackersForDamageAssignment(blocker);

                blocker.addBlockedThisTurn(attacker);
                attacker.addBlockedByThisTurn(blocker);

                Map<String, Object> runParams = Maps.newHashMap();
                runParams.put("Attacker", attacker);
                runParams.put("Blocker", blocker);
                game.getTriggerHandler().runTriggerOld(TriggerType.AttackerBlockedByCreature, runParams, false);

                runParams = Maps.newHashMap();
                runParams.put("Blocker", blocker);
                runParams.put("Attackers", attacker);
                game.getTriggerHandler().runTriggerOld(TriggerType.Blocks, runParams, false);
            }

            attacker.getDamageHistory().setCreatureGotBlockedThisCombat(true);
            if (!wasBlocked) {
                final Map<String, Object> runParams = Maps.newHashMap();
                runParams.put("Attacker", attacker);
                runParams.put("Blockers", blockers);
                runParams.put("NumBlockers", blockers.size());
                runParams.put("Defender", combat.getDefenderByAttacker(attacker));
                runParams.put("DefendingPlayer", combat.getDefenderPlayerByAttacker(attacker));
                game.getTriggerHandler().runTriggerOld(TriggerType.AttackerBlocked, runParams, false);

                combat.orderBlockersForDamageAssignment(attacker, new CardCollection(blockers));
            }
        }

        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());

    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        // end standard pre-

        List<String> attackers = new ArrayList<>();
        if (sa.hasParam("DefinedAttacker")) {
            for (final Card attacker : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa)) {
                attackers.add(attacker.toString());
            }
        }

        List<String> blockers = new ArrayList<>();
        if (sa.hasParam("DefinedBlocker")) {
            for (final Card blocker : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedBlocker"), sa)) {
                blockers.add(blocker.toString());
            }
        }

        sb.append(String.join(", ", blockers)).append(" block ").append(String.join(", ", attackers));

        return sb.toString();
    }

}
