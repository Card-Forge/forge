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

    @Override
    public void buildSpellAbility(SpellAbility sa) {
        super.buildSpellAbility(sa);
        if (sa.usesTargeting()) {
            sa.getTargetRestrictions().setZone(ZoneType.Stack);
        }
    }

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

        Player newController = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);
        final Game game = newController.getGame();

        for (SpellAbility spell : getTargetSpells(sa)) {
            Card tgtC = spell.getHostCard();
            long tStamp = game.getNextTimestamp();
            SpellAbilityStackInstance si = game.getStack().getInstanceMatchingSpellAbilityID(spell);
            if (exchange) {
                // Currently the only Exchange Control for Spells is a Permanent Trigger
                // Use "DefinedExchange" to Reference Object that is Exchanging the other direction
                GameObject obj = Iterables.getFirst(getDefinedOrTargeted(sa, "DefinedExchange"), null);
                if (obj instanceof Card c) {
                    if (!c.isInPlay() || c.isPhasedOut() || si == null) {
                        // Exchanging object isn't available, continue
                        continue;
                    }

                    if (!c.canBeControlledBy(si.getActivatingPlayer())) {
                        continue;
                    }

                    if (remember) {
                        source.addRemembered(c);
                    }
                    newController = c.getController();
                    c.addTempController(si.getActivatingPlayer(), tStamp);
                    c.runChangeControllerCommands();
                }
            }

            if (remember) {
                source.addRemembered(tgtC);
            }
            if (tgtC.getController() != newController) {
                tgtC.runChangeControllerCommands();
            }
            tgtC.addTempController(newController, tStamp);
            si.setActivatingPlayer(newController);
        }
    }
}
