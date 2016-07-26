package forge.game.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardStateName;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
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
                if (stAb.getMapParams().get("Mode").equals("RaiseCost") && stAb.getMapParams().containsKey("Cost")) {
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

        Cost part = new Cost(params.get("Cost"), sa.isAbility());
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
    
    private static boolean checkRequirement(final SpellAbility sa, final StaticAbility st) {
        if (st.isSuppressed() || !st.checkConditions()) {
            return false;
        }

        final Map<String, String> params = st.getMapParams();
        final Card hostCard = st.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getHostCard();
        
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
