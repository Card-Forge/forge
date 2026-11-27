package forge.game.ability.effects;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.WrappedAbility;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class RollPlanarDiceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (game.getActivePlanes() == null) { // not a planechase game, nothing happens
            return;
        }
        if (sa.hasParam("SpecialAction")) {
            game.getPhaseHandler().incPlanarDiceSpecialActionThisTurn();
        }
        PlanarDice.roll(activator, null);

        if (sa.hasParam("QueueAdditionalRolls")) {
            queueAdditionalRoll(sa, activator);
        }
    }

    private static void queueAdditionalRoll(final SpellAbility sa, final Player activator) {
        final int remaining = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("QueueAdditionalRolls"), sa);
        if (remaining < 1) {
            return;
        }

        final SpellAbility roll = sa.copy(sa.getHostCard(), activator, false, true);
        roll.removeParam("SpecialAction");
        if (remaining > 1) {
            roll.putParam("QueueAdditionalRolls", String.valueOf(remaining - 1));
        } else {
            roll.removeParam("QueueAdditionalRolls");
        }
        roll.setPayCosts(Cost.Zero);
        roll.setDescription("Roll the planar die.");
        roll.setStackDescription(roll.getHostCard() + " - Roll the planar die.");

        // Put the roll on the stack before the first roll's waiting triggers are placed above it.
        final Trigger trigger = TriggerHandler.parseTrigger(
                "Mode$ Immediate | TriggerDescription$ Roll the planar die.", roll.getHostCard(), roll.isIntrinsic());
        trigger.setOverridingAbility(roll);
        final WrappedAbility wrapper = new WrappedAbility(trigger, roll, null);
        wrapper.setActivatingPlayer(activator);
        activator.getGame().getStack().add(wrapper);
    }
}
