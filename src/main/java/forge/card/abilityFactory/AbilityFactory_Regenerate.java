package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CombatUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

/**
 * <p>
 * AbilityFactory_Regenerate class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Regenerate {

    // Ex: A:SP$Regenerate | Cost$W | Tgt$TgtC | SpellDescription$Regenerate
    // target creature.
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#Regenerate

    // **************************************************************
    // ********************* Regenerate ****************************
    // **************************************************************

    /**
     * <p>
     * getAbilityRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getAbilityRegenerate(final AbilityFactory af) {

        final SpellAbility abRegenerate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -6386981911243700037L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Regenerate.regenerateCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Regenerate.doTriggerAI(af, this, mandatory);
            }

        }; // Ability_Activated

        return abRegenerate;
    }

    /**
     * <p>
     * getSpellRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getSpellRegenerate(final AbilityFactory af) {

        final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3899905398102316582L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Regenerate.regenerateCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateStackDescription(af, this);
            }

        }; // Spell

        return spRegenerate;
    }

    /**
     * <p>
     * createDrawbackRegenerate.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRegenerate(final AbilityFactory af) {
        final SpellAbility dbRegen = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -2295483806708528744L;

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Regenerate.doTriggerAI(af, this, mandatory);
            }

        };
        return dbRegen;
    }

    /**
     * <p>
     * regenerateStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String regenerateStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtCards.size() > 0) {
            if (sa instanceof Ability_Sub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
            }

            sb.append("Regenerate ");
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(".");

        final Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * regenerateCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean regenerateCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;
        final Cost abCost = af.getAbCost();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, hostCard, 4)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(abCost, hostCard)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);

            if (AllZone.getStack().size() > 0) {
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(af);

                for (final Card c : list) {
                    if (objects.contains(c)) {
                        chance = true;
                    }
                }
            } else {
                if (AllZone.getPhase().is(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    boolean flag = false;

                    for (final Card c : list) {
                        if (c.getShield() == 0) {
                            flag |= CombatUtil.combatantWouldBeDestroyed(c);
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to regen
                    return false;
                }
            }
        } else {
            tgt.resetTargets();
            // filter AIs battlefield by what I can target
            CardList targetables = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
            targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);

            if (targetables.size() == 0) {
                return false;
            }

            if (AllZone.getStack().size() > 0) {
                // check stack for something on the stack will kill anything i
                // control
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(af);

                final CardList threatenedTargets = new CardList();

                for (final Card c : targetables) {
                    if (objects.contains(c) && (c.getShield() == 0)) {
                        threatenedTargets.add(c);
                    }
                }

                if (!threatenedTargets.isEmpty()) {
                    // Choose "best" of the remaining to regenerate
                    tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                    chance = true;
                }
            } else {
                if (AllZone.getPhase().is(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    final CardList combatants = targetables.getType("Creature");
                    CardListUtil.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                            tgt.addTarget(c);
                            chance = true;
                            break;
                        }
                    }
                }
            }
        }

        final Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // regenerateCanPlayAI

    /**
     * <p>
     * doTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean doTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        boolean chance = false;

        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = AbilityFactory_Regenerate.regenMandatoryTarget(af, sa, mandatory);
        }

        final Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * regenMandatoryTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean regenMandatoryTarget(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final Card hostCard = af.getHostCard();
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        CardList targetables = AllZoneUtil.getCardsIn(Zone.Battlefield);
        targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), hostCard);
        final CardList compTargetables = targetables.getController(AllZone.getComputerPlayer());

        if (targetables.size() == 0) {
            return false;
        }

        if (!mandatory && (compTargetables.size() == 0)) {
            return false;
        }

        if (compTargetables.size() > 0) {
            final CardList combatants = compTargetables.getType("Creature");
            CardListUtil.sortByEvaluateCreature(combatants);
            if (AllZone.getPhase().is(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                for (final Card c : combatants) {
                    if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i
            // can target

            // choose my best X without regen
            if (compTargetables.getNotType("Creature").size() == 0) {
                for (final Card c : combatants) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(combatants.get(0));
                return true;
            } else {
                CardListUtil.sortByMostExpensive(compTargetables);
                for (final Card c : compTargetables) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(compTargetables.get(0));
                return true;
            }
        }

        tgt.addTarget(CardFactoryUtil.getCheapestPermanentAI(targetables, hostCard, true));
        return true;
    }

    /**
     * <p>
     * regenerateResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void regenerateResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Card> tgtCards;
        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 1922050611313909200L;

                @Override
                public void execute() {
                    tgtC.resetShield();
                }
            };

            if (AllZoneUtil.isCardInPlay(tgtC) && ((tgt == null) || CardFactoryUtil.canTarget(hostCard, tgtC))) {
                tgtC.addShield();
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateResolve

    // **************************************************************
    // ********************* RegenerateAll *************************
    // **************************************************************

    /**
     * <p>
     * getAbilityRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getAbilityRegenerateAll(final AbilityFactory af) {

        final SpellAbility abRegenerateAll = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3001272997209059394L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Regenerate.regenerateAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateAllStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Regenerate.regenerateAllDoTriggerAI(af, this, mandatory);
            }

        }; // Ability_Activated

        return abRegenerateAll;
    }

    /**
     * <p>
     * getSpellRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility getSpellRegenerateAll(final AbilityFactory af) {

        final SpellAbility spRegenerateAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4185454527676705881L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactory_Regenerate.regenerateAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateAllStackDescription(af, this);
            }

        }; // Spell

        return spRegenerateAll;
    }

    /**
     * <p>
     * createDrawbackRegenerateAll.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRegenerateAll(final AbilityFactory af) {
        final SpellAbility dbRegenAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 4777861790603705572L;

            @Override
            public String getStackDescription() {
                return AbilityFactory_Regenerate.regenerateAllStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactory_Regenerate.regenerateAllResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactory_Regenerate.regenerateAllDoTriggerAI(af, this, mandatory);
            }

        };
        return dbRegenAll;
    }

    /**
     * <p>
     * regenerateAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String regenerateAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final Card host = af.getHostCard();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else {
            desc = "Regenerate all valid cards.";
        }

        sb.append(desc);

        final Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * regenerateAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean regenerateAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card hostCard = af.getHostCard();
        boolean chance = false;
        final Cost abCost = af.getAbCost();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, hostCard, 4)) {
                return false;
            }
        }

        // filter AIs battlefield by what I can target
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

        if (list.size() == 0) {
            return false;
        }

        int numSaved = 0;
        if (AllZone.getStack().size() > 0) {
            // TODO - check stack for something on the stack will kill anything
            // i control
        } else {

            if (AllZone.getPhase().is(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                final CardList combatants = list.getType("Creature");

                for (final Card c : combatants) {
                    if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                        numSaved++;
                    }
                }
            }
        }

        if (numSaved > 1) {
            chance = true;
        }

        final Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * regenerateAllDoTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean regenerateAllDoTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        boolean chance = true;

        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * regenerateAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void regenerateAllResolve(final AbilityFactory af, final SpellAbility sa) {
        final Card hostCard = af.getHostCard();
        final HashMap<String, String> params = af.getMapParams();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

        for (final Card c : list) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 259368227093961103L;

                @Override
                public void execute() {
                    c.resetShield();
                }
            };

            if (AllZoneUtil.isCardInPlay(c)) {
                c.addShield();
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

} // end class AbilityFactory_Regenerate
