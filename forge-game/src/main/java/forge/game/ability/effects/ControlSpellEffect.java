package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

import java.util.List;

public class ControlSpellEffect extends SpellAbilityEffect {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Player> newController = getTargetPlayers(sa, "NewController");
        if (newController.isEmpty()) {
            newController.add(sa.getActivatingPlayer());
        }

        sb.append(newController).append(" gains control of ");

        for(SpellAbility spell : getTargetSpells(sa)) {
            Card c = spell.getHostCard();
            sb.append(" ");
            if (c.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(c);
            }
        }
        sb.append(".");

        return sb.toString();
    }


    @Override
    public void resolve(SpellAbility sa) {
        // Gaining Control of Spells is a permanent effect
        Card source = sa.getHostCard();

        boolean exchange = sa.getParam("Mode").equals("Exchange");
        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "NewController");

        final Player newController = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);
        final Game game = newController.getGame();

        List<SpellAbility> tgtSpells = getTargetSpells(sa);

        // If an Exchange needs to happen, make sure both parties are still in the right zones

        for(SpellAbility spell : tgtSpells) {
            if (exchange) {
                // Currently the only Exchange Control for Spells, is a Permanent Trigger
                // Use "DefinedExchange" to Reference Object that is Exchanging the other direction
            }

            Card tgtC = spell.getHostCard();
            if (!tgtC.equals(sa.getHostCard()) && !sa.getHostCard().getGainControlTargets().contains(tgtC)) {
                sa.getHostCard().addGainControlTarget(tgtC);
            }

            long tStamp = game.getNextTimestamp();
            tgtC.setController(newController, tStamp);
            SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spell);
            si.setActivator(newController);
        }
    }
}
