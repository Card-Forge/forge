package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.util.Localizer;

public class CamouflageEffect extends SpellAbilityEffect {

    private void randomizeBlockers(SpellAbility sa, Combat combat, Player declarer, Player defender, List<Card> attackers, List<CardCollection> blockerPiles) {
        CardLists.shuffle(attackers);
        for (int i = 0; i < attackers.size(); i++) {
            final Card attacker = attackers.get(i);
            CardCollection blockers = blockerPiles.get(i);

            // Remove all illegal blockers first
            for (int j = blockers.size() - 1; j >= 0; j--) {
                final Card blocker = blockers.get(j);
                if (!CombatUtil.canBlock(attacker, blocker, combat)) {
                    blockers.remove(j);
                }
            }

            if (blockers.size() < CombatUtil.getMinNumBlockersForAttacker(attacker, defender)) {
                // If not enough remaining creatures to block, don't add them as blocker
                continue;
            }

            if (StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker, defender).getRight() < blockers.size()) {
                // If no more than one creature can block, order the player to choose one to block
                Card chosen = declarer.getController().chooseCardsForEffect(blockers, sa,
                    Localizer.getInstance().getMessage("lblChooseBlockerForAttacker", attacker.toString()), 1, 1, false, null).get(0);
                combat.addBlocker(attacker, chosen);
                continue;
            }

            // Add all remaning blockers normally
            for (final Card blocker : blockers) {
                combat.addBlocker(attacker, blocker);
            }
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        Card hostCard = sa.getHostCard();
        Player declarer = getDefinedPlayersOrTargeted(sa).get(0);
        Player defender = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("Defender"), sa).get(0);
        Combat combat = hostCard.getGame().getCombat();
        List<Card> attackers = combat.getAttackers();
        List<CardCollection> blockerPiles = new ArrayList<>();

        if (declarer.isAI()) {
            // For AI player, just let it declare blockers normally, then randomize it later.
            declarer.getController().declareBlockers(defender, combat);
            // Remove all blockers first
            for (final Card attacker : attackers) {
                CardCollection blockers = combat.getBlockers(attacker);
                blockerPiles.add(blockers);
                for (final Card blocker : blockers) {
                    combat.removeFromCombat(blocker);
                }
            }
        } else { // Human player
            CardCollection pool = new CardCollection(defender.getCreaturesInPlay());
            // remove all blockers that can't block
            for (final Card blocker : pool) {
                if (!CombatUtil.canBlock(blocker)) {
                    pool.remove(blocker);
                }
            }
            List<Integer> blockedSoFar = new ArrayList<>(Collections.nCopies(pool.size(), 0));

            for (int i = 0; i < attackers.size(); i++) {
                int size = pool.size();
                CardCollection blockers = new CardCollection(declarer.getController().chooseCardsForEffect(
                    pool, sa, Localizer.getInstance().getMessage("lblChooseBlockersForPile", String.valueOf(i + 1)), 0, size, false, null));
                blockerPiles.add(blockers);
                // Remove chosen creatures, unless it can block additional attackers
                for (final Card blocker : blockers) {
                    int index = pool.indexOf(blocker);
                    Integer blockedCount = blockedSoFar.get(index) + 1;
                    if (!blocker.canBlockAny() && blocker.canBlockAdditional() < blockedCount) {
                        pool.remove(index);
                        blockedSoFar.remove(index);
                    } else {
                        blockedSoFar.set(index, blockedCount);
                    }
                }
            }
        }

        randomizeBlockers(sa, combat, declarer, defender, attackers, blockerPiles);
    }

}
