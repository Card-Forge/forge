package forge.game.cost;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.game.player.PlayerCollection;
import forge.game.ability.AbilityKey;
import forge.game.trigger.TriggerType;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.TargetChoices;
import forge.game.staticability.StaticAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class CostAdjustment {

    public static Cost adjust(final Cost cost, final SpellAbility sa) {
        if (sa.isTrigger() || cost == null) {
            return cost;
        }

        final Player player = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = player.getGame();
        Cost result = cost.copy();
        boolean isStateChangeToFaceDown = false;

        if (sa.isSpell()) {
            if (sa.isCastFaceDown() && !host.isFaceDown()) {
                // Turn face down to apply cost modifiers correctly
                host.turnFaceDownNoUpdate();
                isStateChangeToFaceDown = true;
            }

            // Commander Tax there
            if (host.isCommander() && host.getCastFrom() != null && ZoneType.Command.equals(host.getCastFrom().getZoneType())) {
                int n = player.getCommanderCast(host) * 2;
                if (n > 0) {
                    result.add(new Cost(ManaCost.get(n), false));
                }
            }
        }

        CardCollection cardsOnBattlefield = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(host)) {
            cardsOnBattlefield.add(host);
        }
        final List<StaticAbility> raiseAbilities = Lists.newArrayList();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                if (stAb.checkMode("RaiseCost")) {
                    raiseAbilities.add(stAb);
                }
            }
        }

        if (sa.hasParam("RaiseCost")) {
            String raise = sa.getParam("RaiseCost");
            Cost inc;
            if (sa.hasSVar(raise)) {
                inc = new Cost(ManaCost.get(AbilityUtils.calculateAmount(host, raise, sa)), false);
            } else {
                inc = new Cost(raise, false);
            }
            result.add(inc);
        }

        // Raise cost
        for (final StaticAbility stAb : raiseAbilities) {
            applyRaise(result, sa, stAb);
        }

        // Reset card state (if changed)
        if (isStateChangeToFaceDown) {
            host.setState(CardStateName.Original, false);
            host.setFaceDown(false);
        }
        return result;
    }

    private static void applyRaise(final Cost cost, final SpellAbility sa, final StaticAbility st) {
        final Card hostCard = st.getHostCard();

        if (!checkRequirement(sa, st)) {
            return;
        }

        final String scost = st.getParamOrDefault("Cost", "1");
        int count = 0;

        if (st.hasParam("ForEachShard")) {
            ManaCost mc = ManaCost.ZERO;
            if (sa.isSpell()) {
                mc = sa.getHostCard().getManaCost();
            } else if (sa.isAbility() && sa.getPayCosts().hasManaCost()) {
                // TODO check for AlternateCost$, it should always be part of the activation cost too
                mc = sa.getPayCosts().getCostMana().getMana();
            }
            byte atom = ManaAtom.fromName(st.getParam("ForEachShard").toLowerCase());
            for (ManaCostShard shard : mc) {
                if ((shard.getColorMask() & atom) != 0) {
                    ++count;
                }
            }
        } else if (st.hasParam("Amount")) {
            String amount = st.getParam("Amount");
            if ("Escalate".equals(amount)) {
                SpellAbility sub = sa;
                while (sub != null) {
                    if (sub.getDirectSVars().containsKey("CharmOrder")) {
                        count++;
                    }
                    sub = sub.getSubAbility();
                }
                --count;
            } else if ("Strive".equals(amount)) {
                for (TargetChoices tc : sa.getAllTargetChoices()) {
                    count += tc.size();
                }
                --count;
            } else if ("Spree".equals(amount)) {
                SpellAbility sub = sa;
                while (sub != null) {
                    if (sub.hasParam("SpreeCost")) {
                        Cost part = new Cost(sub.getParam("SpreeCost"), sa.isAbility(), sa.getHostCard().equals(hostCard));
                        cost.mergeTo(part, count, sa);
                    }
                    sub = sub.getSubAbility();
                }
            } else {
                if (StringUtils.isNumeric(amount)) {
                    count = Integer.parseInt(amount);
                } else {
                    if (st.hasParam("Relative")) {
                        // grab SVar here already to avoid potential collision when SA has one with same name
                        count = AbilityUtils.calculateAmount(hostCard, st.hasSVar(amount) ? st.getSVar(amount) : amount, sa);
                    } else {
                        count = AbilityUtils.calculateAmount(hostCard, amount, st);
                    }
                }
            }
        } else {
            // Amount 1 as default
            count = 1;
        }
        if (count > 0) {
            Cost part = new Cost(scost, sa.isAbility(), sa.getHostCard().equals(hostCard));
            cost.mergeTo(part, count, sa);
        }
    }

    // If cardsToDelveOut is null, will immediately exile the delved cards and remember them on the host card.
    // Otherwise, will return them in cardsToDelveOut and the caller is responsible for doing the above.
    public static boolean adjust(ManaCostBeingPaid cost, final SpellAbility sa, CardCollection cardsToDelveOut, boolean test) {
        if (sa.isTrigger() || sa.isReplacementAbility()) {
            return true;
        }

        final Game game = sa.getActivatingPlayer().getGame();
        final Card originalCard = sa.getHostCard();
        boolean isStateChangeToFaceDown = false;

        if (sa.isSpell()) {
            if (sa.isCastFaceDown() && !originalCard.isFaceDown()) {
                // Turn face down to apply cost modifiers correctly
                originalCard.turnFaceDownNoUpdate();
                isStateChangeToFaceDown = true;
            }
        }

        CardCollection cardsOnBattlefield = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(originalCard)) {
            cardsOnBattlefield.add(originalCard);
        }
        final List<StaticAbility> reduceAbilities = Lists.newArrayList();
        final List<StaticAbility> setAbilities = Lists.newArrayList();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                if (stAb.checkMode("ReduceCost") && checkRequirement(sa, stAb)) {
                    reduceAbilities.add(stAb);
                }
                else if (stAb.checkMode("SetCost")) {
                    setAbilities.add(stAb);
                }
            }
        }

        // Reduce cost
        int sumGeneric = 0;
        if (sa.hasParam("ReduceCost")) {
            String cst = sa.getParam("ReduceCost");
            String amt = sa.getParamOrDefault("ReduceAmount", cst);
            int num = AbilityUtils.calculateAmount(originalCard, amt, sa);

            if (sa.hasParam("ReduceAmount") && num > 0) {
                cost.subtractManaCost(new ManaCost(new ManaCostParser(Strings.repeat(cst + " ", num))));
            } else {
                sumGeneric += num;
            }
        }

        while (!reduceAbilities.isEmpty()) {
            StaticAbility choice = sa.getActivatingPlayer().getController().chooseSingleStaticAbility(Localizer.getInstance().getMessage("lblChooseCostReduction"), reduceAbilities);
            reduceAbilities.remove(choice);
            sumGeneric += applyReduceCostAbility(choice, sa, cost, sumGeneric);
        }
        // need to reduce generic extra because of 2 hybrid mana
        cost.decreaseGenericMana(sumGeneric);

        if (sa.isSpell()) {
            for (String pip : sa.getPipsToReduce()) {
                cost.decreaseShard(ManaCostShard.parseNonGeneric(pip), 1);
            }
        }

        if (sa.isSpell() && sa.isOffering()) { // cost reduction from offerings
            adjustCostByOffering(cost, sa);
        }
        if (sa.isSpell() && sa.isEmerge()) { // cost reduction from offerings
            adjustCostByEmerge(cost, sa);
        }
        // Set cost (only used by Trinisphere) is applied last
        for (final StaticAbility stAb : setAbilities) {
            applySetCostAbility(stAb, sa, cost);
        }

        if (sa.isSpell()) {
            if (sa.getHostCard().hasKeyword(Keyword.ASSIST) && !adjustCostByAssist(cost, sa, test)) {
                return false;
            }

            if (sa.getHostCard().hasKeyword(Keyword.DELVE)) {
                sa.getHostCard().clearDelved();

                final CardZoneTable table = new CardZoneTable();
                final Player pc = sa.getActivatingPlayer();
                final CardCollection mutableGrave = new CardCollection(pc.getCardsIn(ZoneType.Graveyard));
                final CardCollectionView toExile = pc.getController().chooseCardsToDelve(cost.getUnpaidShards(ManaCostShard.GENERIC), mutableGrave);
                for (final Card c : toExile) {
                    cost.decreaseGenericMana(1);
                    if (cardsToDelveOut != null) {
                        cardsToDelveOut.add(c);
                    } else if (!test) {
                        sa.getHostCard().addDelved(c);
                        final Card d = game.getAction().exile(c, null, null);
                        final Card host = sa.getHostCard();
                        host.addExiledCard(d);
                        d.setExiledWith(host);
                        d.setExiledBy(host.getController());
                        table.put(ZoneType.Graveyard, d.getZone().getZoneType(), d);
                    }
                }
                table.triggerChangesZoneAll(game, sa);
            }
            if (sa.getHostCard().hasKeyword(Keyword.CONVOKE)) {
                adjustCostByConvokeOrImprovise(cost, sa, false, test);
            }
            if (sa.getHostCard().hasKeyword(Keyword.IMPROVISE)) {
                adjustCostByConvokeOrImprovise(cost, sa, true, test);
            }
        } // isSpell

        // Reset card state (if changed)
        if (isStateChangeToFaceDown) {
            originalCard.setFaceDown(false);
            originalCard.setState(CardStateName.Original, false);
        }

        return true;
    }
    // GetSpellCostChange

    private static boolean adjustCostByAssist(ManaCostBeingPaid cost, final SpellAbility sa, boolean test) {
        // 702.132a Assist is a static ability that modifies the rules of paying for the spell with assist (see rules 601.2g-h).
        // If the total cost to cast a spell with assist includes a generic mana component, before you activate mana abilities while casting it, you may choose another player.
        // That player has a chance to activate mana abilities. Once that player chooses not to activate any more mana abilities, you have a chance to activate mana abilities.
        // Before you begin to pay the total cost of the spell, the player you chose may pay for any amount of the generic mana in the spell’s total cost.
        int genericLeft = cost.getUnpaidShards(ManaCostShard.GENERIC);
        if (genericLeft == 0) {
            return true;
        }

        Player activator = sa.getActivatingPlayer();
        PlayerCollection otherPlayers = activator.getAllOtherPlayers();

        Player assistant = activator.getController().choosePlayerToAssistPayment(otherPlayers, sa, "Choose a player to assist paying this spell", genericLeft);
        if (assistant == null) {
            return true;
        }
        int requestedAmount = genericLeft;
        // TODO: Nice to have. Ask the player how much mana you are hoping someone will pay.
        return assistant.getController().helpPayForAssistSpell(cost, sa, genericLeft, requestedAmount);
    }

    private static void adjustCostByConvokeOrImprovise(ManaCostBeingPaid cost, final SpellAbility sa, boolean improvise, boolean test) {
        if (!improvise) {
            sa.clearTappedForConvoke();
        }

        final Player activator = sa.getActivatingPlayer();
        CardCollectionView untappedCards = CardLists.filter(activator.getCardsIn(ZoneType.Battlefield),
                CardPredicates.Presets.CAN_TAP);
        if (improvise) {
            untappedCards = CardLists.filter(untappedCards, CardPredicates.Presets.ARTIFACTS);
        } else {
            untappedCards = CardLists.filter(untappedCards, CardPredicates.Presets.CREATURES);
        }

        Map<Card, ManaCostShard> convokedCards = activator.getController().chooseCardsForConvokeOrImprovise(sa,
                cost.toManaCost(), untappedCards, improvise);

        CardCollection tapped = new CardCollection();
        for (final Entry<Card, ManaCostShard> conv : convokedCards.entrySet()) {
            Card c = conv.getKey();
            if (!improvise) {
                sa.addTappedForConvoke(c);
            }
            cost.decreaseShard(conv.getValue(), 1);
            if (!test) {
                if (c.tap(true, sa, activator)) tapped.add(c);
            }
        }
        if (!tapped.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, tapped);
            activator.getGame().getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
        }
    }

    private static void adjustCostByOffering(final ManaCostBeingPaid cost, final SpellAbility sa) {
        String offeringType = "";
        for (KeywordInterface inst : sa.getHostCard().getKeywords(Keyword.OFFERING)) {
            final String kw = inst.getOriginal();
            offeringType = kw.split(" ")[0];
            break;
        }

        CardCollectionView canOffer = CardLists.filter(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield),
                CardPredicates.isType(offeringType), CardPredicates.canBeSacrificedBy(sa, false));

        final CardCollectionView toSacList = sa.getHostCard().getController().getController().choosePermanentsToSacrifice(sa, 0, 1, canOffer, offeringType);

        if (toSacList.isEmpty()) {
            return;
        }
        Card toSac = toSacList.getFirst();

        cost.subtractManaCost(toSac.getManaCost());

        sa.setSacrificedAsOffering(toSac);
        toSac.setUsedToPay(true); //stop it from interfering with mana input
    }

    private static void adjustCostByEmerge(final ManaCostBeingPaid cost, final SpellAbility sa) {
        String kw = sa.getKeyword().getOriginal();
        String k[] = kw.split(":");
        String validStr = k.length > 2 ? k[2] : "Creature";
        Player p = sa.getActivatingPlayer();
        CardCollectionView canEmerge = CardLists.filter(p.getCardsIn(ZoneType.Battlefield),
                CardPredicates.restriction(validStr, p, sa.getHostCard(), sa),
                CardPredicates.canBeSacrificedBy(sa, false));

        final CardCollectionView toSacList = p.getController().choosePermanentsToSacrifice(sa, 0, 1, canEmerge, validStr);

        if (toSacList.isEmpty()) {
            return;
        }
        Card toSac = toSacList.getFirst();

        cost.decreaseGenericMana(toSac.getCMC());

        sa.setSacrificedAsEmerge(toSac);
        toSac.setUsedToPay(true); //stop it from interfering with mana input
    }
    /**
     * Applies applyRaiseCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param manaCost
     *            a ManaCost
     */
    private  static void applySetCostAbility(final StaticAbility staticAbility, final SpellAbility sa, final ManaCostBeingPaid manaCost) {
        final String amount = staticAbility.getParam("Amount");

        if (!checkRequirement(sa, staticAbility)) {
            return;
        }

        int value = Integer.parseInt(amount);

        if (staticAbility.hasParam("RaiseTo")) {
            value = Math.max(value - manaCost.getConvertedManaCost(), 0);
        }

        manaCost.increaseGenericMana(value);
    }

    /**
     * Applies applyReduceCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param manaCost
     *            a ManaCost
     */
    private static int applyReduceCostAbility(final StaticAbility staticAbility, final SpellAbility sa, final ManaCostBeingPaid manaCost, int sumReduced) {
        //Can't reduce zero cost
        if (manaCost.toString().equals("{0}")) {
            return 0;
        }
        final Card hostCard = staticAbility.getHostCard();
        final Card card = sa.getHostCard();
        final String amount = staticAbility.getParam("Amount");

        int value;
        if ("AffectedX".equals(amount)) {
            value = AbilityUtils.calculateAmount(card, amount, staticAbility);
        } else if ("Undaunted".equals(amount)) {
            value = card.getController().getOpponents().size();
        } else if (staticAbility.hasParam("Relative")) {
            // TODO: update cards with "This spell costs X less to cast...if you..."
            // The caster is sa.getActivatingPlayer()
            // cards like Hostage Taker can cast spells from other players.
            value = AbilityUtils.calculateAmount(hostCard, staticAbility.hasSVar(amount) ? staticAbility.getSVar(amount) : amount, sa);
        } else {
            value = AbilityUtils.calculateAmount(hostCard, amount, staticAbility);
        }

        if (staticAbility.hasParam("UpTo")) {
            value = sa.getActivatingPlayer().getController().chooseNumberForCostReduction(sa, 0, value);
        }

        if (!staticAbility.hasParam("Cost") && !staticAbility.hasParam("Color")) {
            int minMana = 0;
            if (staticAbility.hasParam("MinMana")) {
                minMana = Integer.valueOf(staticAbility.getParam("MinMana"));
            }

            final int maxReduction = manaCost.getConvertedManaCost() - minMana - sumReduced;
            if (maxReduction > 0) {
                return Math.min(value, maxReduction);
            }
        } else {
            final String color = staticAbility.getParamOrDefault("Cost", staticAbility.getParam("Color"));
            int sumGeneric = 0;
            // might be problematic for wierd hybrid combinations
            for (final String cost : color.split(" ")) {
                if (StringUtils.isNumeric(cost)) {
                    sumGeneric += Integer.parseInt(cost) * value;
                } else {
                    if (staticAbility.hasParam("IgnoreGeneric")) {
                        manaCost.decreaseShard(ManaCostShard.parseNonGeneric(cost), value);
                    } else {
                        manaCost.subtractManaCost(new ManaCost(new ManaCostParser(Strings.repeat(cost + " ", value))));
                    }
                }
            }
            return sumGeneric;
        }
        return 0;
    }    

    private static boolean checkRequirement(final SpellAbility sa, final StaticAbility st) {
        if (!st.checkConditions()) {
            return false;
        }

        final Card hostCard = st.getHostCard();
        final Player controller = hostCard.getController();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getHostCard();
        final Game game = hostCard.getGame();

        if (!st.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (!st.matchesValidParam("ValidSpell", sa)) {
            return false;
        }
        if (!st.matchesValidParam("Activator", activator)) {
            return false;
        }

        if (st.hasParam("Type")) {
            final String type = st.getParam("Type");
            if (type.equals("Spell")) {
                if (!sa.isSpell()) {
                    return false;
                }
                if (st.hasParam("OnlyFirstSpell")) {
                    if (activator == null ) {
                        return false;
                    }
                    List<Card> list;
                    if (st.hasParam("ValidCard")) {
                        list = CardUtil.getThisTurnCast(st.getParam("ValidCard"), hostCard, st, controller);
                    } else {
                        list = game.getStack().getSpellsCastThisTurn();
                    }

                    if (st.hasParam("ValidSpell")) {
                        list = CardLists.filterAsList(list, CardPredicates.castSA(
                            SpellAbilityPredicates.isValid(st.getParam("ValidSpell").split(","), controller, hostCard, st))
                        );
                    }

                    if (CardLists.filterControlledBy(list, activator).size() > 0) {
                        return false;
                    }
                }
            } else if (type.equals("Ability")) {
                if (!sa.isActivatedAbility() || sa.isReplacementAbility()) {
                    return false;
                }
                if (st.hasParam("OnlyFirstActivation")) {
                    int times = 0;
                    for (IndividualCostPaymentInstance i : game.costPaymentStack) {
                        SpellAbility paymentSa = i.getPayment().getAbility();
                        if (paymentSa.isActivatedAbility() && st.matchesValidParam("ValidCard", paymentSa.getHostCard())) {
                            times++;
                            if (times > 1) {
                                return false;
                            }
                        }
                    }
                }
            } else if (type.equals("NonManaAbility")) {
                if (!sa.isActivatedAbility() || sa.isManaAbility() || sa.isReplacementAbility()) {
                    return false;
                }
            } else if (type.equals("MorphDown")) {
                if (!sa.isSpell() || !sa.isCastFaceDown()) {
                    return false;
                }
            } else if (type.equals("Foretell")) {
                if (!sa.isForetelling()) {
                    return false;
                }
                if (st.hasParam("FirstForetell") && activator.getNumForetoldThisTurn() > 0) {
                    return false;
                }
            }
        }
        if (st.hasParam("AffectedZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(st.getParam("AffectedZone"));
            if (sa.isSpell() && card.wasCast()) {
                if (!zones.contains(card.getCastFrom().getZoneType())) {
                    return false;
                }
            } else {
                Zone z = card.getLastKnownZone();
                if (z == null || !zones.contains(z.getZoneType())) {
                    return false;
                }
            }
        }
        if (st.hasParam("ValidTarget")) {
            SpellAbility curSa = sa;
            boolean targetValid = false;
            outer: while (curSa != null) {
                if (!curSa.usesTargeting()) {
                    curSa = curSa.getSubAbility();
                    continue;
                }
                for (GameObject target : curSa.getTargets()) {
                    if (target.isValid(st.getParam("ValidTarget").split(","), controller, hostCard, curSa)) {
                        targetValid = true;
                        break outer;
                    }
                }
                curSa = curSa.getSubAbility();
            }
            if (st.hasParam("UnlessValidTarget")) {
                if (targetValid) {
                    return false;
                }
            } else if (!targetValid) {
                return false;
            }
        }
        return true;
    }
}
