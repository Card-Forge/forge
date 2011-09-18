package forge.card.abilityFactory;

import forge.*;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>AbilityFactory_PreventDamage class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_PreventDamage {

    // Ex: A:SP$ PreventDamage | Cost$ W | Tgt$ TgtC | Amount$ 3 | SpellDescription$ Prevent the next 3 damage that would be dealt to ...
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#PreventDamage

    /**
     * <p>getAbilityPreventDamage.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getAbilityPreventDamage(final AbilityFactory af) {

        final SpellAbility abRegenerate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -6581723619801399347L;

            @Override
            public boolean canPlayAI() {
                return preventDamageCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                preventDamageResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return preventDamageStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return doPreventDamageTriggerAI(af, this, mandatory);
            }

        };//Ability_Activated

        return abRegenerate;
    }

    /**
     * <p>getSpellPreventDamage.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getSpellPreventDamage(final AbilityFactory af) {

        final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3899905398102316582L;

            @Override
            public boolean canPlayAI() {
                return preventDamageCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                preventDamageResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return preventDamageStackDescription(af, this);
            }

        }; // Spell

        return spRegenerate;
    }

    /**
     * <p>createDrawbackPreventDamage.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackPreventDamage(final AbilityFactory af) {
        final SpellAbility dbRegen = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -2295483806708528744L;

            @Override
            public String getStackDescription() {
                return preventDamageStackDescription(af, this);
            }

            @Override
            public void resolve() {
                preventDamageResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return doPreventDamageTriggerAI(af, this, mandatory);
            }

        };
        return dbRegen;
    }

    /**
     * <p>preventDamageStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String preventDamageStackDescription(AbilityFactory af, SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();
        Card host = af.getHostCard();

        ArrayList<Object> tgts;
        if (sa.getTarget() == null)
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        else
            tgts = sa.getTarget().getTargets();

        if (sa instanceof Ability_Sub)
            sb.append(" ");
        else
            sb.append(host).append(" - ");

        sb.append("Prevent the next ");
        sb.append(params.get("Amount"));
        sb.append(" that would be dealt to ");
        for (int i = 0; i < tgts.size(); i++) {
            if (i != 0)
                sb.append(" ");

            Object o = tgts.get(i);
            if (o instanceof Card) {
                Card tgtC = (Card) o;
                if (tgtC.isFaceDown()) sb.append("Morph");
                else sb.append(tgtC);
            } else sb.append(o.toString());
        }
        sb.append(" this turn.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>preventDamageCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean preventDamageCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;

        Cost cost = sa.getPayCosts();
        
        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(cost, hostCard, 4))
            return false;

        if (!CostUtil.checkDiscardCost(cost, hostCard))
            return false;
            
        if (!CostUtil.checkSacrificeCost(cost, hostCard))
            return false;
            
        if (!CostUtil.checkRemoveCounterCost(cost, hostCard))
            return false;

        Target tgt = af.getAbTgt();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of them
            ArrayList<Object> objects = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);

            // react to threats on the stack
            if (AllZone.getStack().size() > 0) {
            	ArrayList<Object> threatenedObjects = AbilityFactory.predictThreatenedObjects(af);
                for (Object o : objects) {
                    if (threatenedObjects.contains(o))
                        chance = true;
                }
            } else {
                if (AllZone.getPhase().is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
                    boolean flag = false;
                    for (Object o : objects) {
                        if (o instanceof Card) {
                            Card c = (Card) o;
                            flag |= CombatUtil.combatantWouldBeDestroyed(c);
                        } else if (o instanceof Player) {
                            Player p = (Player) o;
                            flag |= (p.isComputer() && 
                                    ((CombatUtil.wouldLoseLife(AllZone.getCombat()) && sa.isAbility()) ||
                                    CombatUtil.lifeInDanger(AllZone.getCombat()))) ;
                        }
                    }

                    chance = flag;
                } else {    // if nothing on the stack, and it's not declare blockers. no need to regen
                    return false;
                }
            }
        } //targeted
        
        // react to threats on the stack
        else if (AllZone.getStack().size() > 0) {
            tgt.resetTargets();
            // check stack for something on the stack will kill anything i control
            ArrayList<Object> objects = new ArrayList<Object>();//AbilityFactory.predictThreatenedObjects(af);
            
            if (objects.contains(AllZone.getComputerPlayer()))
            	tgt.addTarget(AllZone.getComputerPlayer());

            CardList threatenedTargets = new CardList();
            // filter AIs battlefield by what I can target
            CardList targetables = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
            targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);

            for (Card c : targetables) {
                if (objects.contains(c))
                    threatenedTargets.add(c);
            }

            if(!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                tgt.addTarget(CardFactoryUtil.AI_getBestCreature(threatenedTargets));
                chance = true;
            }
                
        } // Protect combatants
        else if (AllZone.getPhase().is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
            if (tgt.canTgtPlayer() && CombatUtil.wouldLoseLife(AllZone.getCombat())
                    && (CombatUtil.lifeInDanger(AllZone.getCombat()) || sa.isAbility())) {
                tgt.addTarget(AllZone.getComputerPlayer());
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                CardList targetables = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);

                if (targetables.size() == 0)
                    return false;
                CardList combatants = targetables.getType("Creature");
                CardListUtil.sortByEvaluateCreature(combatants);

                for (Card c : combatants) {
                    if (CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        chance = true;
                        break;
                    }
                }
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
            chance &= subAb.chkAI_Drawback();

        return chance;
    }

    /**
     * <p>doPreventDamageTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean doPreventDamageTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
        boolean chance = false;

        if (!ComputerUtil.canPayCost(sa))
            return false;

        Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = preventDamageMandatoryTarget(af, sa, mandatory);
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
            chance &= subAb.doTrigger(mandatory);

        return chance;
    }

    /**
     * <p>preventDamageMandatoryTarget.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean preventDamageMandatoryTarget(AbilityFactory af, SpellAbility sa, boolean mandatory) {
        final Card hostCard = af.getHostCard();
        Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        CardList targetables = AllZoneUtil.getCardsIn(Zone.Battlefield);
        targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);
        CardList compTargetables = targetables.getController(AllZone.getComputerPlayer());

        if (targetables.size() == 0)
            return false;

        if (!mandatory && compTargetables.size() == 0)
            return false;

        if (compTargetables.size() > 0) {
            CardList combatants = compTargetables.getType("Creature");
            CardListUtil.sortByEvaluateCreature(combatants);
            if (AllZone.getPhase().is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
                for (Card c : combatants) {
                    if (CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i can target

            tgt.addTarget(combatants.get(0));
            return true;
        }

        tgt.addTarget(CardFactoryUtil.AI_getCheapestPermanent(targetables, hostCard, true));
        return true;
    }

    /**
     * <p>preventDamageResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void preventDamageResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        int numDam = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);

        ArrayList<Object> tgts;
        if (sa.getTarget() == null)
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        else
            tgts = sa.getTarget().getTargets();

        boolean targeted = (af.getAbTgt() != null);

        for (Object o : tgts) {
            if (o instanceof Card) {
                Card c = (Card) o;
                if (AllZoneUtil.isCardInPlay(c) && (!targeted || CardFactoryUtil.canTarget(af.getHostCard(), c))) {
                    c.addPreventNextDamage(numDam);
                }

            } else if (o instanceof Player) {
                Player p = (Player) o;
                if (!targeted || p.canTarget(sa)) {
                    p.addPreventNextDamage(numDam);
                }
            }
        }
    }//doResolve
}
