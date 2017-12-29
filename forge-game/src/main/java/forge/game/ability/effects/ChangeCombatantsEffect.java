package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.util.collect.FCollectionView;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ChangeCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);
        // should update when adding effects for defined blocker
        sb.append("Reselect the defender of ");
        sb.append(StringUtils.join(tgtCards, ", "));

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        boolean isCombatChanged = false;
        final Game game = sa.getActivatingPlayer().getGame();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        // TODO: may expand this effect for defined blocker (False Orders, General Jarkeld, Sorrow's Path, Ydwen Efreet)
        for (final Card c : getTargetCards(sa)) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                final Combat combat = game.getCombat();
                final GameEntity originalDefender = combat.getDefenderByAttacker(c);
                final FCollectionView<GameEntity> defs = combat.getDefenders();
                final GameEntity defender = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(defs, sa,
                        "Choose which defender to attack with " + c, false);
                if (originalDefender != null && !originalDefender.equals(defender)) {
                    AttackingBand ab = combat.getBandOfAttacker(c);
                    if (ab != null) {
                        combat.unregisterAttacker(c, ab);
                        ab.removeAttacker(c);
                    }
                    combat.addAttacker(c, defender);
                    // retarget triggers to the new defender (e.g. Ulamog, Ceaseless Hunger + Portal Mage)
                    for (SpellAbilityStackInstance si : game.getStack()) {
                        if (si.isTrigger() && c.equals(si.getSourceCard())
                                && si.getTriggeringObject("Attacker") != null) {
                            si.addTriggeringObject("OriginalDefender", originalDefender);
                            if (defender instanceof Player) {
                                si.updateTriggeringObject("DefendingPlayer", defender);
                            } else if (defender instanceof Card) {
                                si.updateTriggeringObject("DefendingPlayer", ((Card)defender).getController());
                            }
                        }
                    }
                    isCombatChanged = true;
                }
            }
        }

        if (isCombatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
