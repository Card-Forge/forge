package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.card.CardType;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;

public class SacrificeEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Card card = sa.getHostCard();

        if (sa.hasParam("Echo")) {
            boolean isPaid;
            if (activator.hasKeyword("You may pay 0 rather than pay the echo cost for permanents you control.")
                    && activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantPayEcho") + " {0}?", null)) {
                isPaid = true;
            } else {
                isPaid = activator.getController().payManaOptional(card, new Cost(sa.getParam("Echo"), true),
                    sa, Localizer.getInstance().getMessage("lblPayEcho"), ManaPaymentPurpose.Echo);
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(card);
            runParams.put(AbilityKey.EchoPaid, isPaid);
            game.getTriggerHandler().runTrigger(TriggerType.PayEcho, runParams, false);
            if (isPaid || !card.getController().equals(activator)) {
                return;
            }
        } else if (sa.hasParam("CumulativeUpkeep")) {
            GameEntityCounterTable table = new GameEntityCounterTable();
            card.addCounter(CounterEnumType.AGE, 1, activator, table);

            table.replaceCounterEffect(game, sa, true);

            Cost payCost = new Cost(ManaCost.ZERO, true);
            int n = card.getCounters(CounterEnumType.AGE);
            if (n > 0) {
                Cost cumCost = new Cost(sa.getParam("CumulativeUpkeep"), true);
                payCost.mergeTo(cumCost, n, sa);
            }

            game.updateLastStateForCard(card);

            StringBuilder sb = new StringBuilder();
            sb.append("Cumulative upkeep for ").append(card);

            boolean isPaid = activator.getController().payManaOptional(card, payCost, sa, sb.toString(), ManaPaymentPurpose.CumulativeUpkeep);
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(card);
            runParams.put(AbilityKey.CumulativeUpkeepPaid, isPaid);
            runParams.put(AbilityKey.PayingMana, StringUtils.join(sa.getPayingMana(), ""));
            game.getTriggerHandler().runTrigger(TriggerType.PayCumulativeUpkeep, runParams, false);
            if (isPaid || !card.getController().equals(activator)) {
                return;
            }
        }

        // Expand Sacrifice keyword here depending on what we need out of it.
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Amount", "1"), sa);
        final boolean devour = sa.hasParam("Devour");
        final boolean exploit = sa.hasParam("Exploit");
        final boolean sacEachValid = sa.hasParam("SacEachValid");

        String valid = sa.getParamOrDefault("SacValid", "Self");
        String msg = sa.getParamOrDefault("SacMessage", valid);

        final boolean destroy = sa.hasParam("Destroy");
        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        final boolean optional = sa.hasParam("Optional");
        CardZoneTable table = new CardZoneTable();
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());

        if (valid.equals("Self") && game.getZoneOf(card) != null) {
            if (game.getZoneOf(card).is(ZoneType.Battlefield)) {
                if (!optional || activator.getController().confirmAction(sa, null,
                        Localizer.getInstance().getMessage("lblDoYouWantSacrificeThis", card.getName()), null)) {
                    if (game.getAction().sacrifice(card, sa, true, table, params) != null) {
                        if (remSacrificed) {
                            card.addRemembered(card);
                        }
                    }
                }
            }
        } else {
            CardCollectionView choosenToSacrifice = null;
            for (final Player p : getTargetPlayers(sa)) {
                CardCollectionView battlefield = p.getCardsIn(ZoneType.Battlefield);
                if (sacEachValid) { // Sacrifice maximum permanents in any combination of types specified by SacValid
                    String [] validArray = valid.split(" & ");
                    String [] msgArray = msg.split(" & ");
                    List<CardCollection> validTargetsList = new ArrayList<>(validArray.length);
                    for (String subValid : validArray) {
                        CardCollection validTargets = CardLists.filter(AbilityUtils.filterListByType(battlefield, subValid, sa), CardPredicates.canBeSacrificedBy(sa, true));
                        validTargetsList.add(validTargets);
                    }
                    CardCollection chosenCards = new CardCollection();
                    for (int i = 0; i < validArray.length; ++i) {
                        CardCollection validTargets = validTargetsList.get(i);
                        if (validTargets.isEmpty()) continue;
                        if (validTargets.size() > 1 && i < validArray.length - 1) {
                            removeCandidates(validTargets, validTargetsList, new HashSet<>(), i + 1, 0, amount);
                        }
                        choosenToSacrifice = p.getController().choosePermanentsToSacrifice(sa, amount, amount, validTargets, msgArray[i]);
                        for (int j = i + 1; j < validArray.length; ++j) {
                            validTargetsList.get(j).removeAll(choosenToSacrifice);
                        }
                        chosenCards.addAll(choosenToSacrifice);
                    }
                    choosenToSacrifice = chosenCards;
                } else {
                    CardCollectionView validTargets = AbilityUtils.filterListByType(battlefield, valid, sa);
                    if (!destroy) {
                        validTargets = CardLists.filter(validTargets, CardPredicates.canBeSacrificedBy(sa, true));
                    }

                    boolean isStrict = sa.hasParam("StrictAmount");
                    int minTargets = optional && !isStrict ? 0 : amount;
                    boolean notEnoughTargets = isStrict && validTargets.size() < minTargets;

                    if (sa.hasParam("Random")) {
                        choosenToSacrifice = new CardCollection(Aggregates.random(validTargets, Math.min(amount, validTargets.size())));
                    } else if (notEnoughTargets || (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantSacrifice"), null))) {
                        choosenToSacrifice = CardCollection.EMPTY;
                    } else {
                        choosenToSacrifice = destroy ?
                                p.getController().choosePermanentsToDestroy(sa, minTargets, amount, validTargets, msg) :
                                    p.getController().choosePermanentsToSacrifice(sa, minTargets, amount, validTargets, msg);
                    }
                }

                choosenToSacrifice = GameActionUtil.orderCardsByTheirOwners(game, choosenToSacrifice, ZoneType.Graveyard, sa);

                Map<Integer, Card> cachedMap = Maps.newHashMap();
                for (Card sac : choosenToSacrifice) {
                    Card lKICopy = null;
                    if (devour || exploit || remSacrificed) {
                        lKICopy = CardUtil.getLKICopy(sac, cachedMap);
                    }
                    boolean wasSacrificed = !destroy && game.getAction().sacrifice(sac, sa, true, table, params) != null;
                    boolean wasDestroyed = destroy && game.getAction().destroy(sac, sa, true, table, params);
                    // Run Devour Trigger
                    if (devour) {
                        card.addDevoured(lKICopy);
                        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                        runParams.put(AbilityKey.Devoured, lKICopy);
                        game.getTriggerHandler().runTrigger(TriggerType.Devoured, runParams, false);
                    }
                    if (exploit) {
                        card.addExploited(lKICopy);
                        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(card);
                        runParams.put(AbilityKey.Exploited, lKICopy);
                        game.getTriggerHandler().runTrigger(TriggerType.Exploited, runParams, false);
                    }
                    if ((wasDestroyed || wasSacrificed) && remSacrificed) {
                        card.addRemembered(lKICopy);
                    }
                }
            }
        }

        table.triggerChangesZoneAll(game, sa);
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgts = getTargetPlayers(sa);

        String valid = sa.getParamOrDefault("SacValid", "Self");
        String num = sa.getParamOrDefault("Amount", "1");

        if (sa.hasParam("Optional")) { // TODO make boolean and handle verb reconjugation throughout
            sb.append("(OPTIONAL) ");
        }

        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), num, sa);

        if (valid.equals("Self")) {
            sb.append("Sacrifices ").append(sa.getHostCard());
        } else if (valid.equals("Card.AttachedBy")) {
            final Card toSac = sa.getHostCard().getEnchantingCard();
            sb.append(toSac.getController()).append(" sacrifices ").append(toSac).append(".");
        } else {
            sb.append(Lang.joinHomogenous(tgts)).append(" ");
            boolean oneTgtP = tgts.size() == 1;

            String msg = sa.getParamOrDefault("SacMessage", valid);
            msg = CardType.CoreType.isValidEnum(msg) ? msg.toLowerCase() : msg;

            if (sa.hasParam("Destroy")) {
                sb.append(oneTgtP ? "destroys " : " destroy ");
            } else {
                sb.append(oneTgtP ? "sacrifices " : "sacrifice ");
            }
            sb.append(Lang.nounWithNumeralExceptOne(amount, msg)).append(".");
        }

        return sb.toString();
    }

    private void removeCandidates(CardCollection validTargets, List<CardCollection> validTargetsList, Set<Card> union, int index, int included, int amount) {
        if (index >= validTargetsList.size()) {
            if (union.size() <= included * amount) {
                validTargets.removeAll(union);
            }
            return;
        }

        removeCandidates(validTargets, validTargetsList, union, index + 1, included, amount);

        CardCollection candidate = validTargetsList.get(index);
        if (candidate.isEmpty()) {
            return;
        }

        if (union.isEmpty()) {
            if (candidate.size() <= amount) {
                validTargets.removeAll(candidate.asSet());
            } else {
                removeCandidates(validTargets, validTargetsList, candidate.asSet(), index + 1, included + 1, amount);
            }
        } else {
            Set<Card> unionClone = new HashSet<>(union);
            unionClone.addAll(candidate.asSet());
            removeCandidates(validTargets, validTargetsList, unionClone, index + 1, included + 1, amount);
        }
    }
}
