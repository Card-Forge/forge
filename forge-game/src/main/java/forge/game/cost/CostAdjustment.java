package forge.game.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardStateName;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

public class CostAdjustment {

    public static Cost adjust(final Cost cost, final SpellAbility sa) {
        final Player player = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = player.getGame();

        if (sa.isTrigger()) {
            return cost;
        }
    
        boolean isStateChangeToFaceDown = false;
        if (sa.isSpell() && ((Spell) sa).isCastFaceDown()) {
            // Turn face down to apply cost modifiers correctly
            host.setState(CardStateName.FaceDown, false);
            isStateChangeToFaceDown = true;
        } // isSpell
    
        CardCollection cardsOnBattlefield = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(host)) {
            cardsOnBattlefield.add(host);
        }
        final List<StaticAbility> raiseAbilities = new ArrayList<StaticAbility>();
    
        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                if (stAb.getMapParams().get("Mode").equals("RaiseCost")) {
                    raiseAbilities.add(stAb);
                }
            }
        }
        // Raise cost
        for (final StaticAbility stAb : raiseAbilities) {
            applyRaise(cost, sa, stAb);
        }

        // Reset card state (if changed)
        if (isStateChangeToFaceDown) {
            host.setState(CardStateName.Original, false);
        }
        return cost;
    }
    
    private static void applyRaise(final Cost cost, final SpellAbility sa, final StaticAbility st) {
        final Map<String, String> params = st.getMapParams();
        final Card hostCard = st.getHostCard();
        final Card card = sa.getHostCard();
        
        if (!checkRequirement(sa, st)) {
            return;
        }

        final String scost = params.containsKey("Cost") ? params.get("Cost") : "1";
        Cost part = new Cost(scost, sa.isAbility());
        int count = 0;

        if (params.containsKey("ForEachShard")) {
            CostPartMana mc = cost.getCostMana();
            if (mc != null) {
                byte atom = ManaAtom.fromName(params.get("ForEachShard").toLowerCase());
                for (ManaCostShard shard : mc.getManaCostFor(sa)) {
                    if ((shard.getColorMask() & atom) != 0) {
                        ++count;
                    }
                }
            }
        } else if (params.containsKey("Amount")) {
            String amount = params.get("Amount");
            if ("Escalate".equals(amount)) {
                SpellAbility sub = sa;
                while(sub != null) {
                    if (!sub.getSVar("CharmOrder").equals("")) {
                        count++;
                    }
                    sub = sub.getSubAbility();
                }
                --count;
            } else if ("Strive".equals(amount)) {
                for (TargetChoices tc : sa.getAllTargetChoices()) {
                    count += tc.getNumTargeted();
                }
                --count;
            } else {
                if (StringUtils.isNumeric(amount)) {
                    count = Integer.parseInt(amount);
                } else {
                    if (params.containsKey("AffectedAmount")) {
                        count = CardFactoryUtil.xCount(card, hostCard.getSVar(amount));
                    } else {
                        count = AbilityUtils.calculateAmount(hostCard, amount, sa);
                    }
                }
            }
        } else {
            // Amount 1 as default
            count = 1;
        }
        for(int i = 0; i < count; ++i) {
            cost.add(part);
        }
    }
    
    // If cardsToDelveOut is null, will immediately exile the delved cards and remember them on the host card.
    // Otherwise, will return them in cardsToDelveOut and the caller is responsible for doing the above.
    public static final void adjust(ManaCostBeingPaid cost, final SpellAbility sa, CardCollection cardsToDelveOut, boolean test) {
        final Game game = sa.getActivatingPlayer().getGame();
        final Card originalCard = sa.getHostCard();
    
        if (sa.isTrigger()) {
            return;
        }

        boolean isStateChangeToFaceDown = false;
        if (sa.isSpell()) {
            if (((Spell) sa).isCastFaceDown()) {
            	// Turn face down to apply cost modifiers correctly
            	originalCard.setState(CardStateName.FaceDown, false);
            	isStateChangeToFaceDown = true;
            }
        } // isSpell

        CardCollection cardsOnBattlefield = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(originalCard)) {
            cardsOnBattlefield.add(originalCard);
        }
        final List<StaticAbility> reduceAbilities = new ArrayList<StaticAbility>();
        final List<StaticAbility> setAbilities = new ArrayList<StaticAbility>();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                if (stAb.getMapParams().get("Mode").equals("ReduceCost")) {
                    reduceAbilities.add(stAb);
                }
                else if (stAb.getMapParams().get("Mode").equals("SetCost")) {
                    setAbilities.add(stAb);
                }
            }
        }

        // Reduce cost
        for (final StaticAbility stAb : reduceAbilities) {
        	applyReduceCostAbility(stAb, sa, cost);
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
            if (sa.isDelve()) {
                sa.getHostCard().clearDelved();
                final Player pc = sa.getActivatingPlayer();
                final CardCollection mutableGrave = new CardCollection(pc.getCardsIn(ZoneType.Graveyard));
                final CardCollectionView toExile = pc.getController().chooseCardsToDelve(cost.getUnpaidShards(ManaCostShard.GENERIC), mutableGrave);
                for (final Card c : toExile) {
                    cost.decreaseGenericMana(1);
                    if (cardsToDelveOut != null) {
                        cardsToDelveOut.add(c);
                    } else if (!test) {
                        sa.getHostCard().addDelved(c);
                        pc.getGame().getAction().exile(c);
                    }
                }
            }
            else if (sa.getHostCard().hasKeyword("Convoke")) {
                adjustCostByConvoke(cost, sa, test);
            }
        } // isSpell

        // Reset card state (if changed)
        if (isStateChangeToFaceDown) {
        	originalCard.setState(CardStateName.Original, false);
        }
    }
    // GetSpellCostChange

    private static void adjustCostByConvoke(ManaCostBeingPaid cost, final SpellAbility sa, boolean test) {
        CardCollectionView untappedCreats = CardLists.filter(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
        untappedCreats = CardLists.filter(untappedCreats, CardPredicates.Presets.UNTAPPED);
    
        Map<Card, ManaCostShard> convokedCards = sa.getActivatingPlayer().getController().chooseCardsForConvoke(sa, cost.toManaCost(), untappedCreats);
        
        // Convoked creats are tapped here with triggers suppressed,
        // Then again when payment is done(In InputPayManaCost.done()) with suppression cleared.
        // This is to make sure that triggers go off at the right time
        // AND that you can't use mana tapabilities of convoked creatures to pay the convoked cost.
        for (final Entry<Card, ManaCostShard> conv : convokedCards.entrySet()) {
            sa.addTappedForConvoke(conv.getKey());
            cost.decreaseShard(conv.getValue(), 1);
            if (!test) {
                conv.getKey().tap();
            }
        }
    }

    private static void adjustCostByOffering(final ManaCostBeingPaid cost, final SpellAbility sa) {
        String offeringType = "";
        for (String kw : sa.getHostCard().getKeywords()) {
            if (kw.endsWith(" offering")) {
                offeringType = kw.split(" ")[0];
                break;
            }
        }

        Card toSac = null;
        CardCollectionView canOffer = CardLists.filter(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield),
                CardPredicates.isType(offeringType), CardPredicates.canBeSacrificedBy(sa));

        final CardCollectionView toSacList = sa.getHostCard().getController().getController().choosePermanentsToSacrifice(sa, 0, 1, canOffer, offeringType);

        if (!toSacList.isEmpty()) {
            toSac = toSacList.getFirst();
        }
        else {
            return;
        }
    
        cost.subtractManaCost(toSac.getManaCost());
    
        sa.setSacrificedAsOffering(toSac);
        toSac.setUsedToPay(true); //stop it from interfering with mana input
    }

    private static void adjustCostByEmerge(final ManaCostBeingPaid cost, final SpellAbility sa) {
    
        Card toSac = null;
        CardCollectionView canEmerge = CardLists.filter(sa.getActivatingPlayer().getCreaturesInPlay(), CardPredicates.canBeSacrificedBy(sa));

        final CardCollectionView toSacList = sa.getHostCard().getController().getController().choosePermanentsToSacrifice(sa, 0, 1, canEmerge, "Creature");

        if (!toSacList.isEmpty()) {
            toSac = toSacList.getFirst();
        }
        else {
            return;
        }
    
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
        final Map<String, String> params = staticAbility.getMapParams();
        final String amount = params.get("Amount");

        if (!checkRequirement(sa, staticAbility)) {
            return;
        }

        int value = 0;
        if (StringUtils.isNumeric(amount)) {
            value = Integer.parseInt(amount);
        } else {
            if ("Min3".equals(amount)) {
                int cmc = manaCost.getConvertedManaCost();
                if (cmc < 3) {
                    value = 3 - cmc;
                }
            }
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
    private static void applyReduceCostAbility(final StaticAbility staticAbility, final SpellAbility sa, final ManaCostBeingPaid manaCost) {
        //Can't reduce zero cost
        if (manaCost.toString().equals("{0}")) {
            return;
        }
        final Map<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Card card = sa.getHostCard();
        final String amount = params.get("Amount");

        if (!checkRequirement(sa, staticAbility)) {
            return;
        }

        int value;
        if ("AffectedX".equals(amount)) {
            value = CardFactoryUtil.xCount(card, hostCard.getSVar(amount));
        } else if ("X".equals(amount)){
            value = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(amount));
        } else {
            value = AbilityUtils.calculateAmount(hostCard, amount, sa);
        }

        if (!params.containsKey("Cost") && ! params.containsKey("Color")) {
            int minMana = 0;
            if (params.containsKey("MinMana")) {
                minMana = Integer.valueOf(params.get("MinMana"));
            }

            final int maxReduction = Math.max(0, manaCost.getConvertedManaCost() - minMana);
            if (maxReduction > 0) {
                manaCost.decreaseGenericMana(Math.min(value, maxReduction));
            }
        } else {
            final String color = params.containsKey("Cost") ? params.get("Cost") : params.get("Color");
            for (final String cost : color.split(" ")) {
                if (StringUtils.isNumeric(cost)) {
                    manaCost.decreaseGenericMana(Integer.parseInt(cost) * value);
                } else {
                    manaCost.decreaseShard(ManaCostShard.parseNonGeneric(cost), value);
                }
            }
        }
    }    

    private static boolean checkRequirement(final SpellAbility sa, final StaticAbility st) {
        if (st.isSuppressed() || !st.checkConditions()) {
            return false;
        }

        final Map<String, String> params = st.getMapParams();
        final Card hostCard = st.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, sa)) {
            return false;
        }
        if (params.containsKey("Activator") && ((activator == null)
                || !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard, sa))) {
            return false;
        }

        if (params.containsKey("Type")) {
        	final String type = params.get("Type");
            if (type.equals("Spell")) {
                if (!sa.isSpell()) {
                    return false;
                }
                if (params.containsKey("OnlyFirstSpell")) {
                    if (activator == null ) {
                        return false;
                    }
                    CardCollection list = CardLists.filterControlledBy(activator.getGame().getStack().getSpellsCastThisTurn(), activator);
                    if (params.containsKey("ValidCard")) {
                        list = CardLists.getValidCards(list, params.get("ValidCard"), hostCard.getController(), hostCard);
                    }
                    if (list.size() > 0) {
                        return false;
                    }
                }
            } else if (type.equals("Ability")) {
                if (!(sa instanceof AbilityActivated) || sa.isReplacementAbility()) {
                    return false;
                }
            } else if (type.equals("NonManaAbility")) {
                if (!(sa instanceof AbilityActivated) || sa.isManaAbility() || sa.isReplacementAbility()) {
                    return false;
                }
            } else if (type.equals("Buyback")) {
                if (!sa.isBuyBackAbility()) {
                    return false;
                }
            } else if (type.equals("Cycling")) {
                if (!sa.isCycling()) {
                    return false;
                }
            } else if (type.equals("Dash")) {
                if (!sa.isDash()) {
                    return false;
                }
            } else if (type.equals("Equip")) {
                if (!(sa instanceof AbilityActivated) || !sa.hasParam("Equip")) {
                    return false;
                }
            } else if (type.equals("Flashback")) {
                if (!sa.isFlashBackAbility()) {
                    return false;
                }
            } else if (type.equals("MorphUp")) {
                if (!sa.isMorphUp()) {
                    return false;
                }
            } else if (type.equals("MorphDown")) {
                if (!sa.isSpell() || !((Spell) sa).isCastFaceDown()) {
                    return false;
                }
            } else if (type.equals("SelfMonstrosity")) {
                if (!(sa instanceof AbilityActivated) || !sa.hasParam("Monstrosity") || sa.isTemporary()) {
                    // Nemesis of Mortals
                    return false;
                }
            } else if (type.equals("SelfIntrinsicAbility")) {
                if (!(sa instanceof AbilityActivated) || sa.isReplacementAbility() || sa.isTemporary()) {
                    return false;
                }
            }
        }
        if (params.containsKey("AffectedZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(params.get("AffectedZone"));
            boolean found = false;
            for(ZoneType zt : zones) {
                if(card.isInZone(zt))
                {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        if (params.containsKey("ValidTarget")) {
            TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt == null) {
                return false;
            }
            boolean targetValid = false;
            for (GameObject target : sa.getTargets().getTargets()) {
                if (target.isValid(params.get("ValidTarget").split(","), hostCard.getController(), hostCard, sa)) {
                    targetValid = true;
                }
            }
            if (!targetValid) {
                return false;
            }
        }
        if (params.containsKey("ValidSpellTarget")) {
            TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt == null) {
                return false;
            }
            boolean targetValid = false;
            for (SpellAbility target : sa.getTargets().getTargetSpells()) {
                Card targetCard = target.getHostCard();
                if (targetCard.isValid(params.get("ValidSpellTarget").split(","), hostCard.getController(), hostCard, sa)) {
                    targetValid = true;
                    break;
                }
            }
            if (!targetValid) {
                return false;
            }
        }
        return true;
    }
}