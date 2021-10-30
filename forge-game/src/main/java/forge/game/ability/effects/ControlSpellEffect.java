package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

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

        for (SpellAbility spell : getTargetSpells(sa)) {
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
        boolean remember = sa.hasParam("Remember");
        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "NewController");

        final Player newController = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);
        final Game game = newController.getGame();

        List<SpellAbility> tgtSpells = getTargetSpells(sa);

        // If an Exchange needs to happen, make sure both parties are still in the right zones

        for (SpellAbility spell : tgtSpells) {
            Card tgtC = spell.getHostCard();
            SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spell);
            long tStamp = game.getNextTimestamp();
            if (exchange) {
                // Currently the only Exchange Control for Spells is a Permanent Trigger
                // Expand this area as it becomes needed
                // Use "DefinedExchange" to Reference Object that is Exchanging the other direction
                GameObject obj = Iterables.getFirst(getDefinedOrTargeted(sa, "DefinedExchange"), null);
                if (obj instanceof Card) {
                    Card c = (Card)obj;
                    if (!(c.isInZone(ZoneType.Battlefield)) || si == null) {
                        // Exchanging object isn't available, continue
                        continue;
                    }

                    if (!c.canBeControlledBy(si.getActivatingPlayer())) {
                        continue;
                    }

                    if (c.getController().equals(si.getActivatingPlayer())) {
                        // Controllers are already the same, no exchange needed
                        continue;
                    }

                    if (remember) {
                        source.addRemembered(c);
                    }
                    c.addTempController(si.getActivatingPlayer(), tStamp);
                }
            }

            if (!tgtC.equals(sa.getHostCard()) && !sa.getHostCard().getGainControlTargets().contains(tgtC)) {
                sa.getHostCard().addGainControlTarget(tgtC);
            }

            if (remember) {
                source.addRemembered(tgtC);
            }
            tgtC.addTempController(newController, tStamp);
            si.setActivatingPlayer(newController);
        }
    }
}
