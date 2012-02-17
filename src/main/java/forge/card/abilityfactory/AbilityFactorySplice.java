/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.ComputerUtil;
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
import forge.util.MyRandom;

//Examples:
//A:SP$ Splice | Cost$ 1 G | TargetType$ Arcane | SpellDescription$ Counter target activated ability.

/**
 * <p>
 * AbilityFactorySplice class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactorySplice {

    private AbilityFactory abilityFactory = null;
    private HashMap<String, String> params = null;
    private String unlessCost = null;

    /**
     * <p>
     * Constructor for AbilityFactorySplice.
     * </p>
     * 
     * @param newAbilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactorySplice(final AbilityFactory newAbilityFactory) {
        this.abilityFactory = newAbilityFactory;
        this.params = this.abilityFactory.getMapParams();

        if (this.params.containsKey("UnlessCost")) {
            this.unlessCost = this.params.get("UnlessCost").trim();
        }

    }

    /**
     * <p>
     * getAbilitySplice.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilitySplice(final AbilityFactory abilityFactory) {
        final SpellAbility abilitySplice = new AbilityActivated(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -3895990436431818899L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactorySplice.this
                        .spliceStackDescription(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySplice.this.spliceCanPlayAI(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySplice.this.spliceResolve(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySplice.this.spliceCanPlayAI(AbilityFactorySplice.this.abilityFactory, this);
            }

        };
        return abilitySplice;
    }

    /**
     * <p>
     * getSpellSplice.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellSplice(final AbilityFactory abilityFactory) {
        final SpellAbility spellAbilitySplice = new Spell(abilityFactory.getHostCard(), abilityFactory.getAbCost(),
                abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return AbilityFactorySplice.this
                        .spliceStackDescription(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySplice.this.spliceCanPlayAI(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySplice.this.spliceResolve(AbilityFactorySplice.this.abilityFactory, this);
            }

        };
        return spellAbilitySplice;
    }

    /**
     * <p>
     * getDrawbackSplice.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackSplice(final AbilityFactory abilityFactory) {
        final SpellAbility drawbackSplice = new AbilitySub(abilityFactory.getHostCard(), abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4272851734871573693L;

            @Override
            public String getStackDescription() {
                return AbilityFactorySplice.this
                        .spliceStackDescription(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactorySplice.this.spliceCanPlayAI(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactorySplice.this.spliceResolve(AbilityFactorySplice.this.abilityFactory, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactorySplice.this.spliceDoTriggerAI(AbilityFactorySplice.this.abilityFactory, this,
                        true);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactorySplice.this.spliceDoTriggerAI(AbilityFactorySplice.this.abilityFactory, this,
                        mandatory);
            }

        };
        return drawbackSplice;
    }

    /**
     * <p>
     * spliceCanPlayAI.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean spliceCanPlayAI(final AbilityFactory abilityFactory, final SpellAbility spellAbility) {
        boolean toReturn = true;
        final Cost abCost = abilityFactory.getAbCost();
        final Card source = spellAbility.getSourceCard();
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

        final Target tgt = spellAbility.getTarget();
        if (tgt != null) {

            final SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(spellAbility, topSA, tgt)) {
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
                toPay = ComputerUtil.determineLeftoverMana(spellAbility);
            } else {
                toPay = AbilityFactory.calculateAmount(source, this.unlessCost, spellAbility);
            }

            if (toPay == 0) {
                return false;
            }

            if (toPay <= usableManaSources) {
                // If this is a reusable Resource, feel free to play it most of
                // the time
                if (!spellAbility.getPayCosts().isReusuableResource() || (MyRandom.getRandom().nextFloat() < .4)) {
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

        final AbilitySub subAb = spellAbility.getSubAbility();
        if (subAb != null) {
            toReturn &= subAb.chkAIDrawback();
        }

        return toReturn;
    }

    /**
     * <p>
     * spliceDoTriggerAI.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean spliceDoTriggerAI(final AbilityFactory abilityFactory, final SpellAbility spellAbility, final boolean mandatory) {
        boolean toReturn = true;
        if (AllZone.getStack().size() < 1) {
            return false;
        }

        final Target tgt = spellAbility.getTarget();
        if (tgt != null) {
            final SpellAbility topSA = AllZone.getStack().peekAbility();
            if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer()) {
                return false;
            }

            tgt.resetTargets();
            if (TargetSelection.matchSpellAbility(spellAbility, topSA, tgt)) {
                tgt.addTarget(topSA);
            } else {
                return false;
            }

            final Card source = spellAbility.getSourceCard();
            if (this.unlessCost != null) {
                // Is this Usable Mana Sources? Or Total Available Mana?
                final int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());
                int toPay = 0;
                boolean setPayX = false;
                if (this.unlessCost.equals("X") && source.getSVar(this.unlessCost).equals("Count$xPaid")) {
                    setPayX = true;
                    toPay = ComputerUtil.determineLeftoverMana(spellAbility);
                } else {
                    toPay = AbilityFactory.calculateAmount(source, this.unlessCost, spellAbility);
                }

                if (toPay == 0) {
                    return false;
                }

                if (toPay <= usableManaSources) {
                    // If this is a reusable Resource, feel free to play it most
                    // of the time
                    if (!spellAbility.getPayCosts().isReusuableResource() || (MyRandom.getRandom().nextFloat() < .4)) {
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

        final AbilitySub subAb = spellAbility.getSubAbility();
        if (subAb != null) {
            toReturn &= subAb.chkAIDrawback();
        }

        return toReturn;
    }

    /**
     * <p>
     * spliceResolve.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void spliceResolve(final AbilityFactory abilityFactory, final SpellAbility spellAbility) {

        // TODO Before this resolves we should see if any of our targets are
        // still on the stack
        ArrayList<SpellAbility> sas;

        final Target tgt = abilityFactory.getAbTgt();
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(spellAbility.getSourceCard(), this.params.get("Defined"), spellAbility);
        }

        if (this.params.containsKey("ForgetOtherTargets")) {
            if (this.params.get("ForgetOtherTargets").equals("True")) {
                abilityFactory.getHostCard().clearRemembered();
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

            this.removeFromStack(tgtSA, spellAbility, si);

            // Destroy Permanent may be able to be turned into a SubAbility
            if (tgtSA.isAbility() && this.params.containsKey("DestroyPermanent")) {
                AllZone.getGameAction().destroy(tgtSACard);
            }

            if (this.params.containsKey("RememberTargets")) {
                if (this.params.get("RememberTargets").equals("True")) {
                    abilityFactory.getHostCard().addRemembered(tgtSACard);
                }
            }
        }
    } // end spliceResolve

    /**
     * <p>
     * spliceStackDescription.
     * </p>
     * 
     * @param abilityFactory
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param spellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String spliceStackDescription(final AbilityFactory abilityFactory, final SpellAbility spellAbility) {

        final StringBuilder sb = new StringBuilder();

        if (!(spellAbility instanceof AbilitySub)) {
            sb.append(spellAbility.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<SpellAbility> sas;

        final Target tgt = abilityFactory.getAbTgt();
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(spellAbility.getSourceCard(), this.params.get("Defined"), spellAbility);
        }

        sb.append("splicing");

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

        final AbilitySub abSub = spellAbility.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // end spliceStackDescription

    /**
     * <p>
     * removeFromStack.
     * </p>
     * 
     * @param targetSpellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param sourceSpellAbility
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param spellAbilityStackInstance
     *            a {@link forge.card.spellability.SpellAbilityStackInstance}
     *            object.
     */
    private void removeFromStack(final SpellAbility targetSpellAbility, final SpellAbility sourceSpellAbility, final SpellAbilityStackInstance spellAbilityStackInstance) {
        AllZone.getStack().remove(spellAbilityStackInstance);
    }

} // end class AbilityFactorySplice
