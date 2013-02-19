package forge.card.ability.effects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class RepeatEachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getSourceCard();

        // setup subability to repeat
        final SpellAbility repeat = AbilityFactory.getAbility(sa.getSourceCard().getSVar(sa.getParam("RepeatSubAbility")), source);
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
            repeatCards = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
            if (!repeatCards.isEmpty()) {
                loopOverCards = true;
            }
        }

        if (loopOverCards) {

            // TODO (ArsenalNut 22 Dec 2012) Add logic to order cards for AI
            if (sa.getActivatingPlayer().isHuman() && sa.hasParam("ChooseOrder") && repeatCards.size() >= 2) {
                repeatCards = GuiChoose.order("Choose order of copies to cast", "Put first", 0, repeatCards, null, null);
            }

            for (Card card : repeatCards) {
                if (useImprinted) {
                    source.addImprinted(card);
                } else {
                    source.addRemembered(card);
                }

                AbilityUtils.resolve(repeat, false);
                if (useImprinted) {
                    source.removeImprinted(card);
                } else {
                    source.removeRemembered(card);
                }
            }
        }

        if (sa.hasParam("RepeatPlayers")) {
            final List<Player> repeatPlayers = AbilityUtils.getDefinedPlayers(source, sa.getParam("RepeatPlayers"), sa);

            for (Player player : repeatPlayers) {
                source.addRemembered(player);
                AbilityUtils.resolve(repeat, false);
                source.removeRemembered(player);
            }
        }

        if (sa.hasParam("RepeatCounters")) {
            Card target = sa.getTargetCard();
            Set<CounterType> types = new HashSet<CounterType>(target.getCounters().keySet());
            for (CounterType type : types) {
                StringBuilder sb = new StringBuilder();
                sb.append("Number$").append(target.getCounters(type));
                source.setSVar("RepeatSVarCounter", type.getName().toUpperCase());
                source.setSVar("RepeatCounterAmount", sb.toString());
                AbilityUtils.resolve(repeat, false);
            }
        }
    }
}
