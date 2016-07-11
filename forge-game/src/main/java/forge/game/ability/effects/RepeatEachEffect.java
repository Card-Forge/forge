package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.collect.FCollection;

public class RepeatEachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();

        // setup subability to repeat
        final SpellAbility repeat = AbilityFactory.getAbility(sa.getSVar(sa.getParam("RepeatSubAbility")), source);

        if (sa.isIntrinsic()) {
            repeat.setIntrinsic(true);
            repeat.changeText();
        }

        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);

        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();

        boolean useImprinted = sa.hasParam("UseImprinted");
        boolean loopOverCards = false;
        boolean recordChoice = sa.hasParam("RecordChoice");
        CardCollectionView repeatCards = null;

        if (sa.hasParam("RepeatCards")) {
            List<ZoneType> zone = new ArrayList<ZoneType>();
            if (sa.hasParam("Zone")) {
                zone = ZoneType.listValueOf(sa.getParam("Zone"));
            } else {
                zone.add(ZoneType.Battlefield);
            }
            repeatCards = CardLists.getValidCards(game.getCardsIn(zone),
                    sa.getParam("RepeatCards"), source.getController(), source);
            loopOverCards = !recordChoice;
        }
        else if (sa.hasParam("DefinedCards")) {
            repeatCards = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
            if (sa.hasParam("AdditionalRestriction")) { // lki cards might not be in game
                repeatCards = CardLists.getValidCards(repeatCards,
                        sa.getParam("AdditionalRestriction"), source.getController(), source);
            }
            if (!repeatCards.isEmpty()) {
                loopOverCards = true;
            }
        }
        // Removing this throw since it doesn't account for Repeating by players or counters e.g. Tempting Wurm
        // Feel free to re-add it if you account for every card that's scripted with RepeatEach
        /*
        else {
            throw new IllegalAbilityException(sa, this);
        }*/


        if (sa.hasParam("ClearRemembered")) {
            source.clearRemembered();
        }

        if (loopOverCards) {
            // TODO (ArsenalNut 22 Dec 2012) Add logic to order cards for AI
            if (sa.hasParam("ChooseOrder") && repeatCards.size() >= 2) {
                repeatCards = player.getController().orderMoveToZoneList(repeatCards, ZoneType.Stack);
            }

            for (Card card : repeatCards) {
                if (useImprinted) {
                    source.addImprintedCard(card);
                } else {
                    source.addRemembered(card);
                }

                AbilityUtils.resolve(repeat);
                if (useImprinted) {
                    source.removeImprintedCard(card);
                } else {
                    source.removeRemembered(card);
                }
            }
        }

        if (sa.hasParam("RepeatPlayers")) {
            final FCollection<Player> repeatPlayers = AbilityUtils.getDefinedPlayers(source, sa.getParam("RepeatPlayers"), sa);
            if (sa.hasParam("ClearRememberedBeforeLoop")) {
                source.clearRemembered();
            }
            boolean optional = sa.hasParam("RepeatOptionalForEachPlayer");
            if (sa.hasParam("StartingWithActivator")) {
                int size = repeatPlayers.size();
                Player activator = sa.getActivatingPlayer();
                while (!activator.equals(repeatPlayers.getFirst())) {
                    repeatPlayers.add(size - 1, repeatPlayers.remove(0));
                }
            }
            for (Player p : repeatPlayers) {
                if (optional && !p.getController().confirmAction(repeat, null, sa.getParam("RepeatOptionalMessage"))) {
                    continue;
                }
                source.addRemembered(p);
                AbilityUtils.resolve(repeat);
                source.removeRemembered(p);
            }
        }

        if (sa.hasParam("RepeatCounters")) {
            Card target = sa.getTargetCard();
            if (target == null) {
                target = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            }
            Set<CounterType> types = new HashSet<CounterType>(target.getCounters().keySet());
            for (CounterType type : types) {
                StringBuilder sb = new StringBuilder();
                sb.append("Number$").append(target.getCounters(type));
                source.setSVar("RepeatSVarCounter", type.getName().toUpperCase());
                source.setSVar("RepeatCounterAmount", sb.toString());
                AbilityUtils.resolve(repeat);
            }
        }
        if (recordChoice) {
            boolean random = sa.hasParam("Random");
            Map<Player, List<Card>> recordMap = new HashMap<Player, List<Card>>();
            if (sa.hasParam("ChoosePlayer")) {
                for (Card card : repeatCards) {
                    Player p;
                    if (random) {
                        p = Aggregates.random(game.getPlayers());
                    } else {
                        p = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(game.getPlayers(), sa, "Choose a player");
                    }
                    if (recordMap.containsKey(p)) {
                        recordMap.get(p).add(0, card);
                    } else {
                        recordMap.put(p, Lists.newArrayList(card));
                    }
                }
            }
            else if (sa.hasParam("ChooseCard")) {
                List<Card> list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                        sa.getParam("ChooseCard"), source.getController(), source);
                String filterController = sa.getParam("FilterControlledBy");
                // default: Starting with you and proceeding in the chosen direction
                Player p = sa.getActivatingPlayer();
                do {
                    CardCollection valid = new CardCollection(list);
                    if ("NextPlayerInChosenDirection".equals(filterController)) {
                        valid = CardLists.filterControlledBy(valid,
                                game.getNextPlayerAfter(p, source.getChosenDirection()));
                    }
                    Card card = p.getController().chooseSingleEntityForEffect(valid, sa, "Choose a card");
                    if (recordMap.containsKey(p)) {
                        recordMap.get(p).add(0, card);
                    } else {
                        recordMap.put(p, Lists.newArrayList(card));
                    }
                    if (source.getChosenDirection() != null) {
                        p = game.getNextPlayerAfter(p, source.getChosenDirection());
                    } else {
                        p = game.getNextPlayerAfter(p);
                    }
                } while (!p.equals(sa.getActivatingPlayer()));
            }

            for (Entry<Player, List<Card>> entry : recordMap.entrySet()) {
                // Remember the player and imprint the cards
                source.addRemembered(entry.getKey());
                source.addImprintedCards(entry.getValue());
                AbilityUtils.resolve(repeat);
                source.removeRemembered(entry.getKey());
                source.removeImprintedCards(entry.getValue());
            }
        }
    }
}
