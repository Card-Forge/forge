package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class RepeatEachEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final AbilityFactory afRepeat = new AbilityFactory();
        Card source = sa.getSourceCard();

        // setup subability to repeat
        final SpellAbility repeat = afRepeat.getAbility(sa.getSourceCard().getSVar(sa.getParam("RepeatSubAbility")), source);
        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);

        GameState game = Singletons.getModel().getGame();

        boolean useImprinted = sa.hasParam("UseImprinted");
        boolean loopOverCards = false;
        List<Card> repeatCards = null;

        if (sa.hasParam("RepeatCards")) {
            ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;

            repeatCards = CardLists.getValidCards(game.getCardsIn(zone),
                    sa.getParam("RepeatCards"), source.getController(), source);
            loopOverCards = true;
        }
        else if (sa.hasParam("DefinedCards")) {
            repeatCards = AbilityFactory.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
            if (!repeatCards.isEmpty()) {
                loopOverCards = true;
            }
        }

        if (loopOverCards) {
            for (Card card : repeatCards) {
                if (useImprinted) {
                    source.addImprinted(card);
                } else {
                    source.addRemembered(card);
                }

                AbilityFactory.resolve(repeat, false);
                if (useImprinted) {
                    source.removeImprinted(card);
                } else {
                    source.removeRemembered(card);
                }
            }
        }

        if (sa.hasParam("RepeatPlayers")) {
            final List<Player> repeatPlayers = AbilityFactory.getDefinedPlayers(source, sa.getParam("RepeatPlayers"), sa);

            for (Player player : repeatPlayers) {
                source.addRemembered(player);
                AbilityFactory.resolve(repeat, false);
                source.removeRemembered(player);
            }
        }
    }
}
