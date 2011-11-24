package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.ComputerUtil;
import forge.MyRandom;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;

//Destination - send countered spell to: (only applies to Spells; ignored for Abilities)
// -Graveyard (Default)
// -Exile
// -TopOfLibrary
// -Hand
// -BottomOfLibrary
// -ShuffleIntoLibrary
//PowerSink - true if the drawback type part of Power Sink should be used
//ExtraActions - this has been removed.  All SubAbilitys should now use the standard SubAbility system

//Examples:
//A:SP$Counter | Cost$ 1 G | TargetType$ Activated | SpellDescription$ Counter target activated ability.
//A:AB$Counter | Cost$ G G | TargetType$ Spell | Destination$ Exile | ValidTgts$ Color.Black | SpellDescription$ xxxxx

/**
 * <p>
 * AbilityFactory_CounterMagic class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryCounterMagic {

    private AbilityFactory af = null;
    private HashMap<String, String> params = null;
    private String destination = null;
    private String unlessCost = null;

    /**
     * <p>
     * Constructor for AbilityFactory_CounterMagic.
     * </p>
     * 
     * @param newAF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactoryCounterMagic(final AbilityFactory newAF) {
        this.af = newAF;
        this.params = this.af.getMapParams();

        this.destination = this.params.containsKey("Destination") ? this.params.get("Destination") : "Graveyard";

        if (this.params.containsKey("UnlessCost")) {
            this.unlessCost = this.params.get("UnlessCost").trim();
        }

    }

    /**
     * <p>
     * getAbilityCounter.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityCounter(final AbilityFactory abilityFactory) {
        final SpellAbility abCounter = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -3895990436431818899L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryCounterMagic.this.counterStackDescription(AbilityFactoryCounterMagic.this.af,
                        this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounterMagic.this.counterCanPlayAI(AbilityFactoryCounterMagic.this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounterMagic.this.counterResolve(AbilityFactoryCounterMagic.this.af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounterMagic.this.counterCanPlayAI(AbilityFactoryCounterMagic.this.af, this);
            }

        };
        return abCounter;
    }

    /**
     * <p>
     * getSpellCounter.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellCounter(final AbilityFactory abilityFactory) {
        final SpellAbility spCounter = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounterMagic.this.counterStackDescription(AbilityFactoryCounterMagic.this.af,
                        this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounterMagic.this.counterCanPlayAI(AbilityFactoryCounterMagic.this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounterMagic.this.counterResolve(AbilityFactoryCounterMagic.this.af, this);
            }

        };
        return spCounter;
    }

    // Add Counter Drawback
    /**
     * <p>
     * getDrawbackCounter.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackCounter(final AbilityFactory abilityFactory) {
        final SpellAbility dbCounter = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCounterMagic.this.counterStackDescription(AbilityFactoryCounterMagic.this.af,
                        this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCounterMagic.this.counterCanPlayAI(AbilityFactoryCounterMagic.this.af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCounterMagic.this.counterResolve(AbilityFactoryCounterMagic.this.af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCounterMagic.this.counterDoTriggerAI(AbilityFactoryCounterMagic.this.af, this,
                        true);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCounterMagic.this.counterDoTriggerAI(AbilityFactoryCounterMagic.this.af, this,
                        mandatory);
            }

        };
        return dbCounter;
    }

    /**
     * <p>
     * counterCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean counterCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        boolean toReturn = true;
        final Cost abCost = af.getAbCost();
        final Card source = sa.getSourceCard();
        if (AllZone.getStack().size() < 1) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {

            final SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(sa, topSA, tgt)) {
                tgt.addTarget(topSA);
            } else {
                return false;
            }
        }

        if (this.unlessCost != null) {
            // Is this Usable Mana Sources? Or Total Available Mana?
            final int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());
            int toPay = 0;
            boolean setPayX = false;
            if (this.unlessCost.equals("X") && source.getSVar(this.unlessCost).equals("Count$xPaid")) {
                setPayX = true;
                toPay = ComputerUtil.determineLeftoverMana(sa);
            } else {
                toPay = AbilityFactory.calculateAmount(source, this.unlessCost, sa);
            }

            if (toPay == 0) {
                return false;
            }

            if (toPay <= usableManaSources) {
                // If this is a reusable Resource, feel free to play it most of
                // the time
                if (!sa.getPayCosts().isReusuableResource() || (MyRandom.getRandom().nextFloat() < .4)) {
                    return false;
                }
            }

            if (setPayX) {
                source.setSVar("PayX", Integer.toString(toPay));
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            toReturn &= subAb.chkAIDrawback();
        }

        return toReturn;
    }

    /**
     * <p>
     * counterDoTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean counterDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        boolean toReturn = true;
        if (AllZone.getStack().size() < 1) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(sa, topSA, tgt)) {
                tgt.addTarget(topSA);
            } else {
                return false;
            }

            final Card source = sa.getSourceCard();
            if (this.unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                final int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());
                int toPay = 0;
                boolean setPayX = false;
                if (this.unlessCost.equals("X") && source.getSVar(this.unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtil.determineLeftoverMana(sa);
                } else {
                    toPay = AbilityFactory.calculateAmount(source, this.unlessCost, sa);
                }

                if (toPay == 0) {
                    return false;
                }

                if (toPay <= usableManaSources) {
                    // If this is a reusable Resource, feel free to play it most
                    // of the time
                    if (!sa.getPayCosts().isReusuableResource() || (MyRandom.getRandom().nextFloat() < .4)) {
                        return false;
                    }
                }

                if (setPayX) {
                    source.setSVar("PayX", Integer.toString(toPay));
                }
            }
        }

        // TODO Improve AI

        // Will return true if this spell can counter (or is Reusable and can
        // force the Human into making decisions)

        // But really it should be more picky about how it counters things

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            toReturn &= subAb.chkAIDrawback();
        }

        return toReturn;
    }

    /**
     * <p>
     * counterResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void counterResolve(final AbilityFactory af, final SpellAbility sa) {

        // TODO Before this resolves we should see if any of our targets are
        // still on the stack
        ArrayList<SpellAbility> sas;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        if (this.params.containsKey("ForgetOtherTargets")) {
            if (this.params.get("ForgetOtherTargets").equals("True")) {
                af.getHostCard().clearRemembered();
            }
        }

        for (final SpellAbility tgtSA : sas) {
            final Card tgtSACard = tgtSA.getSourceCard();

            if (tgtSA.isSpell() && !CardFactoryUtil.isCounterable(tgtSACard)) {
                continue;
            }

            final SpellAbilityStackInstance si = AllZone.getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                continue;
            }

            this.removeFromStack(tgtSA, sa, si);

            // Destroy Permanent may be able to be turned into a SubAbility
            if (tgtSA.isAbility() && this.params.containsKey("DestroyPermanent")) {
                AllZone.getGameAction().destroy(tgtSACard);
            }

            if (this.params.containsKey("RememberTargets")) {
                if (this.params.get("RememberTargets").equals("True")) {
                    af.getHostCard().addRemembered(tgtSACard);
                }
            }
        }
    } // end counterResolve

    /**
     * <p>
     * counterStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String counterStackDescription(final AbilityFactory af, final SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<SpellAbility> sas;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        sb.append("countering");

        boolean isAbility = false;
        for (final SpellAbility tgtSA : sas) {
            sb.append(" ");
            sb.append(tgtSA.getSourceCard());
            isAbility = tgtSA.isAbility();
            if (isAbility) {
                sb.append("'s ability");
            }
        }

        if (isAbility && this.params.containsKey("DestroyPermanent")) {
            sb.append(" and destroy it");
        }

        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // end counterStackDescription

    /**
     * <p>
     * removeFromStack.
     * </p>
     * 
     * @param tgtSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.card.spellability.SpellAbilityStackInstance}
     *            object.
     */
    private void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si) {
        AllZone.getStack().remove(si);

        if (tgtSA.isAbility()) {
            // For Ability-targeted counterspells - do not move it anywhere,
            // even if Destination$ is specified.
        } else if (this.destination.equals("Graveyard")) {
            AllZone.getGameAction().moveToGraveyard(tgtSA.getSourceCard());
        } else if (this.destination.equals("Exile")) {
            AllZone.getGameAction().exile(tgtSA.getSourceCard());
        } else if (this.destination.equals("TopOfLibrary")) {
            AllZone.getGameAction().moveToLibrary(tgtSA.getSourceCard());
        } else if (this.destination.equals("Hand")) {
            AllZone.getGameAction().moveToHand(tgtSA.getSourceCard());
        } else if (this.destination.equals("BottomOfLibrary")) {
            AllZone.getGameAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
        } else if (this.destination.equals("ShuffleIntoLibrary")) {
            AllZone.getGameAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
            tgtSA.getSourceCard().getController().shuffle();
        } else {
            throw new IllegalArgumentException("AbilityFactory_CounterMagic: Invalid Destination argument for card "
                    + srcSA.getSourceCard().getName());
        }

        if (!tgtSA.isAbility()) {
            System.out.println("Send countered spell to " + this.destination);
        }
    }

} // end class AbilityFactory_CounterMagic
