package forge.card.abilityFactory;

import forge.*;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.*;

import java.util.ArrayList;
import java.util.HashMap;

//Destination - send countered spell to: (only applies to Spells; ignored for Abilities)
//		-Graveyard (Default)
//		-Exile
//		-TopOfLibrary
//		-Hand
//		-BottomOfLibrary
//		-ShuffleIntoLibrary
//PowerSink - true if the drawback type part of Power Sink should be used
//ExtraActions - this has been removed.  All SubAbilitys should now use the standard SubAbility system

//Examples:
//A:SP$Counter | Cost$ 1 G | TargetType$ Activated | SpellDescription$ Counter target activated ability.
//A:AB$Counter | Cost$ G G | TargetType$ Spell | Destination$ Exile | ValidTgts$ Color.Black | SpellDescription$ xxxxx

/**
 * <p>AbilityFactory_CounterMagic class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_CounterMagic {

    private AbilityFactory af = null;
    private HashMap<String, String> params = null;
    private String destination = null;
    private String unlessCost = null;

    /**
     * <p>Constructor for AbilityFactory_CounterMagic.</p>
     *
     * @param newAF a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory_CounterMagic(AbilityFactory newAF) {
        af = newAF;
        params = af.getMapParams();

        destination = params.containsKey("Destination") ? params.get("Destination") : "Graveyard";

        if (params.containsKey("UnlessCost"))
            unlessCost = params.get("UnlessCost").trim();

    }

    /**
     * <p>getAbilityCounter.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbilityCounter(final AbilityFactory AF) {
        final SpellAbility abCounter = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -3895990436431818899L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return counterStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return counterCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                counterResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return counterCanPlayAI(af, this);
            }

        };
        return abCounter;
    }

    /**
     * <p>getSpellCounter.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getSpellCounter(final AbilityFactory AF) {
        final SpellAbility spCounter = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return counterStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return counterCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                counterResolve(af, this);
            }

        };
        return spCounter;
    }

    // Add Counter Drawback
    /**
     * <p>getDrawbackCounter.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getDrawbackCounter(final AbilityFactory AF) {
        final SpellAbility dbCounter = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return counterStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return counterCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                counterResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return counterDoTriggerAI(af, this, true);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return counterDoTriggerAI(af, this, mandatory);
            }

        };
        return dbCounter;
    }

    /**
     * <p>counterCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean counterCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        boolean toReturn = true;
        Cost abCost = af.getAbCost();
        final Card source = sa.getSourceCard();
        if (AllZone.getStack().size() < 1) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (abCost.getSacCost() && !abCost.getSacThis()) {
                //only sacrifice something that's supposed to be sacrificed
                String type = abCost.getSacType();
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                typeList = typeList.getValidCards(type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
                    return false;
            }
            if (abCost.getLifeCost()) {
                if (AllZone.getComputerPlayer().getLife() - abCost.getLifeAmount() < 4)
                    return false;
            }
        }

        Target tgt = sa.getTarget();
        if (tgt != null) {

            SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
                return false;

            tgt.resetTargets();
            if (Target_Selection.matchSpellAbility(sa, topSA, tgt))
                tgt.addTarget(topSA);
            else
                return false;
        }


        if (unlessCost != null) {
            // Is this Usable Mana Sources? Or Total Available Mana?
            int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());
            int toPay = 0;
            boolean setPayX = false;
            if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                setPayX = true;
                toPay = ComputerUtil.determineLeftoverMana(sa);
            } else
                toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);

            if (toPay == 0)
                return false;

            if (toPay <= usableManaSources) {
                // If this is a reusable Resource, feel free to play it most of the time
                if (!sa.getPayCosts().isReusuableResource() || MyRandom.random.nextFloat() < .4)
                    return false;
            }

            if (setPayX)
                source.setSVar("PayX", Integer.toString(toPay));
        }

        // TODO: Improve AI

        // Will return true if this spell can counter (or is Reusable and can force the Human into making decisions)

        // But really it should be more picky about how it counters things

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
            toReturn &= subAb.chkAI_Drawback();

        return toReturn;
    }

    /**
     * <p>counterDoTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    public boolean counterDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory) {
        boolean toReturn = true;
        if (AllZone.getStack().size() < 1) {
            return false;
        }

        Target tgt = sa.getTarget();
        if (tgt != null) {
            SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
                return false;

            tgt.resetTargets();
            if (Target_Selection.matchSpellAbility(sa, topSA, tgt))
                tgt.addTarget(topSA);
            else
                return false;

            Card source = sa.getSourceCard();
            if (unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());
                int toPay = 0;
                boolean setPayX = false;
                if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtil.determineLeftoverMana(sa);
                } else
                    toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);

                if (toPay == 0)
                    return false;

                if (toPay <= usableManaSources) {
                    // If this is a reusable Resource, feel free to play it most of the time
                    if (!sa.getPayCosts().isReusuableResource() || MyRandom.random.nextFloat() < .4)
                        return false;
                }

                if (setPayX)
                    source.setSVar("PayX", Integer.toString(toPay));
            }
        }

        // TODO: Improve AI

        // Will return true if this spell can counter (or is Reusable and can force the Human into making decisions)

        // But really it should be more picky about how it counters things

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
            toReturn &= subAb.chkAI_Drawback();

        return toReturn;
    }

    /**
     * <p>counterResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private void counterResolve(final AbilityFactory af, final SpellAbility sa) {

        // TODO: Before this resolves we should see if any of our targets are still on the stack
        ArrayList<SpellAbility> sas;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            sas = tgt.getTargetSAs();
        else
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);

        if (params.containsKey("ForgetOtherTargets")) {
            if (params.get("ForgetOtherTargets").equals("True")) {
                af.getHostCard().clearRemembered();
            }
        }

        for (final SpellAbility tgtSA : sas) {
            Card tgtSACard = tgtSA.getSourceCard();

            if (tgtSA.isSpell() && tgtSACard.keywordsContain("CARDNAME can't be countered."))
                continue;

            SpellAbility_StackInstance si = AllZone.getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null)
                continue;

            removeFromStack(tgtSA, sa, si);

            // Destroy Permanent may be able to be turned into a SubAbility
            if (tgtSA.isAbility() && params.containsKey("DestroyPermanent")) {
                AllZone.getGameAction().destroy(tgtSACard);
            }

            if (params.containsKey("RememberTargets")) {
                if (params.get("RememberTargets").equals("True")) {
                    af.getHostCard().addRemembered(tgtSACard);
                }
            }
        }
    }//end counterResolve

    /**
     * <p>counterStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String counterStackDescription(AbilityFactory af, SpellAbility sa) {

        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard().getName()).append(" - ");
        else
            sb.append(" ");

        ArrayList<SpellAbility> sas;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            sas = tgt.getTargetSAs();
        else
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);

        sb.append("countering");

        boolean isAbility = false;
        for (final SpellAbility tgtSA : sas) {
            sb.append(" ");
            sb.append(tgtSA.getSourceCard());
            isAbility = tgtSA.isAbility();
            if (isAbility) sb.append("'s ability");
        }

        if (isAbility && params.containsKey("DestroyPermanent")) {
            sb.append(" and destroy it");
        }

        sb.append(".");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }//end counterStackDescription

    /**
     * <p>removeFromStack.</p>
     *
     * @param tgtSA a {@link forge.card.spellability.SpellAbility} object.
     * @param srcSA a {@link forge.card.spellability.SpellAbility} object.
     * @param si a {@link forge.card.spellability.SpellAbility_StackInstance} object.
     */
    private void removeFromStack(SpellAbility tgtSA, SpellAbility srcSA, SpellAbility_StackInstance si) {
        AllZone.getStack().remove(si);

        if (tgtSA.isAbility()) {
            //For Ability-targeted counterspells - do not move it anywhere, even if Destination$ is specified.
        } else if (destination.equals("Graveyard")) {
            AllZone.getGameAction().moveToGraveyard(tgtSA.getSourceCard());
        } else if (destination.equals("Exile")) {
            AllZone.getGameAction().exile(tgtSA.getSourceCard());
        } else if (destination.equals("TopOfLibrary")) {
            AllZone.getGameAction().moveToLibrary(tgtSA.getSourceCard());
        } else if (destination.equals("Hand")) {
            AllZone.getGameAction().moveToHand(tgtSA.getSourceCard());
        } else if (destination.equals("BottomOfLibrary")) {
            AllZone.getGameAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
        } else if (destination.equals("ShuffleIntoLibrary")) {
            AllZone.getGameAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
            tgtSA.getSourceCard().getController().shuffle();
        } else {
            throw new IllegalArgumentException("AbilityFactory_CounterMagic: Invalid Destination argument for card " + srcSA.getSourceCard().getName());
        }

        if (!tgtSA.isAbility())
            System.out.println("Send countered spell to " + destination);
    }

}//end class AbilityFactory_CounterMagic
