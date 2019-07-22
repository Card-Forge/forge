package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.collect.FCollection;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RepeatEachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @SuppressWarnings("serial")
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();

        final AbilitySub repeat = sa.getAdditionalAbility("RepeatSubAbility");

        if (repeat != null && !repeat.getHostCard().equalsWithTimestamp(source)) {
            // TODO: for some reason, the host card of the original additional SA is set to the cloned card when
            // the ability is copied (e.g. Clone Legion + Swarm Intelligence). Couldn't figure out why this happens,
            // so this hack is necessary for now to work around this issue.
            System.out.println("Warning: RepeatSubAbility had the wrong host set (potentially after cloning the root SA or changing zones), attempting to correct...");
            repeat.setHostCard(source);
        }

        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();

        boolean useImprinted = sa.hasParam("UseImprinted");
        boolean loopOverCards = false;
        boolean recordChoice = sa.hasParam("RecordChoice");
        CardCollectionView repeatCards = null;

        if (sa.hasParam("RepeatCards")) {
            List<ZoneType> zone = Lists.newArrayList();
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
        
        if (sa.hasParam("DamageMap")) {
            sa.setDamageMap(new CardDamageMap());
            sa.setPreventMap(new CardDamageMap());
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(new CardZoneTable());
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
            boolean nextTurn = sa.hasParam("NextTurnForEachPlayer");
            if (sa.hasParam("StartingWithActivator")) {
                int size = repeatPlayers.size();
                Player activator = sa.getActivatingPlayer();
                while (!activator.equals(repeatPlayers.getFirst())) {
                    repeatPlayers.add(size - 1, repeatPlayers.remove(0));
                }
            }
            for (final Player p : repeatPlayers) {
                if (optional && !p.getController().confirmAction(repeat, null, sa.getParam("RepeatOptionalMessage"))) {
                    continue;
                }
                if (nextTurn) {
                    game.getUntap().addUntil(p, new GameCommand() {
                        @Override
                        public void run() {
                            source.addRemembered(p);
                            AbilityUtils.resolve(repeat);
                            source.removeRemembered(p);
                        }
                    });
                } else {
                    source.addRemembered(p);
                    AbilityUtils.resolve(repeat);
                    source.removeRemembered(p);
                }
            }
        }

        if (sa.hasParam("RepeatCounters")) {
            Card target = sa.getTargetCard();
            if (target == null) {
                target = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            }
            for (CounterType type : target.getCounters().keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Number$").append(target.getCounters(type));
                source.setSVar("RepeatSVarCounter", type.getName().toUpperCase());
                source.setSVar("RepeatCounterAmount", sb.toString());
                AbilityUtils.resolve(repeat);
            }
        }
        if (recordChoice) {
            boolean random = sa.hasParam("Random");
            Map<Player, List<Card>> recordMap = Maps.newHashMap();
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
        
        if(sa.hasParam("DamageMap")) {
            sa.getPreventMap().triggerPreventDamage(false);
            sa.setPreventMap(null);
            // non combat damage cause lifegain there
            sa.getDamageMap().triggerDamageDoneOnce(false, sa);
            sa.setDamageMap(null);
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.getChangeZoneTable().triggerChangesZoneAll(game);
            sa.setChangeZoneTable(null);
        }
    }
}
