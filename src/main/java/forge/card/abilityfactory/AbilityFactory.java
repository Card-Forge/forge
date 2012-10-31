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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;

import forge.CardLists;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.ai.*;
import forge.card.abilityfactory.effects.*;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityCondition;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory {

    private Card hostC = null;

    /**
     * <p>
     * Constructor for AbilityFactory.
     * </p>
     */
    public AbilityFactory() {
    }

    /**
     * <p>
     * Constructor for AbilityFactory.
     * </p>
     * 
     * @param af
     *            a AbilityFactory object.
     */
    public AbilityFactory(final AbilityFactory af) {
        this.abCost = af.getAbCost();
        this.abTgt = af.getAbTgt();
        this.api = af.getAPI();
        this.hasSpDesc = af.hasSpDescription();
        this.hasValid = af.hasValid();
        this.hostC = af.getHostCard();
        this.isAb = af.isAbility();
        this.isDb = af.isDrawback();
        this.isSp = af.isSpell();
        this.isTargeted = af.isTargeted();
        this.mapParams = af.getMapParams();
    }

    /**
     * <p>
     * getHostCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostC;
    }

    /**
     * <p>
     * setHostCard.
     * </p>
     * 
     * @param host
     *            a Card object.
     * 
     */
    public final void setHostCard(final Card host) {
        this.hostC = host;
    }

    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams() {
        return this.mapParams;
    }

    private boolean isAb = false;
    private boolean isSp = false;
    private boolean isDb = false;

    /**
     * <p>
     * isAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isAbility() {
        return this.isAb;
    }

    /**
     * <p>
     * isSpell.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSpell() {
        return this.isSp;
    }

    /**
     * <p>
     * isDrawback.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isDrawback() {
        return this.isDb;
    }

    private Cost abCost = null;

    /**
     * <p>
     * Getter for the field <code>abCost</code>.
     * </p>
     * 
     * @return a {@link forge.card.cost.Cost} object.
     */
    public final Cost getAbCost() {
        return this.abCost;
    }

    private boolean isTargeted = false;
    private boolean hasValid = false;
    private Target abTgt = null;

    /**
     * <p>
     * isTargeted.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isTargeted() {
        return this.isTargeted;
    }

    /**
     * <p>
     * hasValid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasValid() {
        return this.hasValid;
    }

    /**
     * <p>
     * Getter for the field <code>abTgt</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.Target} object.
     */
    public final Target getAbTgt() {
        return this.abTgt;
    }


    /**
     * <p>
     * isCurse.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCurse() {
        return this.mapParams.containsKey("IsCurse");
    }

    private boolean hasSpDesc = false;

    /**
     * <p>
     * hasSpDescription.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSpDescription() {
        return this.hasSpDesc;
    }

    private String api = "";

    /**
     * <p>
     * getAPI.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getAPI() {
        return this.api;
    }

    // *******************************************************


    public static final HashMap<String, String> getMapParams(final String abString, final Card hostCard) {
        final HashMap<String, String> mapParameters = new HashMap<String, String>();

        if (!(abString.length() > 0)) {
            throw new RuntimeException("AbilityFactory : getAbility -- abString too short in " + hostCard.getName()
                    + ": [" + abString + "]");
        }

        final String[] a = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++) {
            a[aCnt] = a[aCnt].trim();
        }

        if (!(a.length > 0)) {
            throw new RuntimeException("AbilityFactory : getAbility -- a[] too short in " + hostCard.getName());
        }

        for (final String element : a) {
            final String[] aa = element.split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("AbilityFactory Parsing Error in getAbility() : Split length of ");
                sb.append(element).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }

    /**
     * <p>
     * getAbility.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbility(final String abString, final Card hostCard) {

        SpellAbility spellAbility = null;

        this.hostC = hostCard;

        this.mapParams = AbilityFactory.getMapParams(abString, hostCard);

        // parse universal parameters

        if (this.mapParams.containsKey("AB")) {
            this.isAb = true;
            this.api = this.mapParams.get("AB");
        } else if (this.mapParams.containsKey("SP")) {
            this.isSp = true;
            this.api = this.mapParams.get("SP");
        } else if (this.mapParams.containsKey("DB")) {
            this.isDb = true;
            this.api = this.mapParams.get("DB");
        } else {
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());
        }

        if (!this.isDb) {
            if (!this.mapParams.containsKey("Cost")) {
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
            }
            this.abCost = new Cost(hostCard, this.mapParams.get("Cost"), this.isAb);

        }

        if (this.mapParams.containsKey("ValidTgts")) {
            this.hasValid = true;
            this.isTargeted = true;
        }

        if (this.mapParams.containsKey("Tgt")) {
            this.isTargeted = true;
        }

        if (this.isTargeted) {
            final String min = this.mapParams.containsKey("TargetMin") ? this.mapParams.get("TargetMin") : "1";
            final String max = this.mapParams.containsKey("TargetMax") ? this.mapParams.get("TargetMax") : "1";

            if (this.hasValid) {
                // TgtPrompt now optional
                final StringBuilder sb = new StringBuilder();
                if (this.hostC != null) {
                    sb.append(this.hostC + " - ");
                }
                final String prompt = this.mapParams.containsKey("TgtPrompt") ? this.mapParams.get("TgtPrompt")
                        : "Select target " + this.mapParams.get("ValidTgts");
                sb.append(prompt);
                this.abTgt = new Target(this.hostC, sb.toString(), this.mapParams.get("ValidTgts").split(","), min, max);
            } else {
                this.abTgt = new Target(this.hostC, this.mapParams.get("Tgt"), min, max);
            }

            if (this.mapParams.containsKey("TgtZone")) { // if Targeting
                                                         // something
                // not in play, this Key
                // should be set
                this.abTgt.setZone(ZoneType.listValueOf(this.mapParams.get("TgtZone")));
            }

            // Target Type mostly for Counter: Spell,Activated,Triggered,Ability
            // (or any combination of)
            // Ability = both activated and triggered abilities
            if (this.mapParams.containsKey("TargetType")) {
                this.abTgt.setTargetSpellAbilityType(this.mapParams.get("TargetType"));
            }

            // TargetValidTargeting most for Counter: e.g. target spell that
            // targets X.
            if (this.mapParams.containsKey("TargetValidTargeting")) {
                this.abTgt.setSAValidTargeting(this.mapParams.get("TargetValidTargeting"));
            }

            if (this.mapParams.containsKey("TargetUnique")) {
                this.abTgt.setUniqueTargets(true);
            }
            if (this.mapParams.containsKey("TargetsFromSingleZone")) {
                this.abTgt.setSingleZone(true);
            }
            if (this.mapParams.containsKey("TargetsFromDifferentZone")) {
                this.abTgt.setDifferentZone(true);
            }
            if (this.mapParams.containsKey("TargetsWithoutSameCreatureType")) {
                this.abTgt.setWithoutSameCreatureType(true);
            }
            if (this.mapParams.containsKey("TargetsWithDefinedController")) {
                this.abTgt.setDefinedController(this.mapParams.get("TargetsWithDefinedController"));
            }
        }
        this.hasSpDesc = this.mapParams.containsKey("SpellDescription");

        // ***********************************
        // Match API keywords. These are listed in alphabetical order.

        SpellAiLogic ai = null;
        SpellEffect se = null;
        
        if (this.api.equals("AddTurn")) {
            ai = new AddTurnAi();
            se = new AddTurnEffect();
        }

        else if (this.api.equals("Animate")) {
            ai = new AnimateAi();
            se = new AnimateEffect();
        }

        else if (this.api.equals("AnimateAll")) {
            ai = new AnimateAllAi();
            se = new AnimateAllEffect();
        }

        else if (this.api.equals("Attach")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAttach.createAbilityAttach(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAttach.createSpellAttach(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAttach.createDrawbackAttach(this);
            }
        }

        else if (this.api.equals("Bond")) {
            se = new BondEffect();
            ai = new BondAi();
        }

        else if (this.api.equals("ChangeZone")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChangeZone.createAbilityChangeZone(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChangeZone.createSpellChangeZone(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChangeZone.createDrawbackChangeZone(this);
            }
        }

        else if (this.api.equals("ChangeZoneAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChangeZone.createAbilityChangeZoneAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChangeZone.createSpellChangeZoneAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChangeZone.createDrawbackChangeZoneAll(this);
            }
        }

        else if (this.api.equals("Charm")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCharm.createAbilityCharm(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCharm.createSpellCharm(this);
            }
        }

        else if (this.api.equals("ChooseCard")) {
            ai = new ChooseCardAi();
            se = new ChooseCardEffect();
        }

        else if (this.api.equals("ChooseColor")) {
            ai = new ChooseColorAi();
            se = new ChooseColorEffect();
        }

        else if (this.api.equals("ChooseNumber")) {
            ai = new CannotPlayAi();
            se = new ChooseNumberEffect();
        }

        else if (this.api.equals("ChoosePlayer")) {
            ai = new CannotPlayAi();
            se = new ChoosePlayerEffect();
        }

        else if (this.api.equals("ChooseType")) {
            ai = new ChooseTypeAi();
            se = new ChooseTypeEffect();
        }

        else if (this.api.equals("Clash")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryClash.createAbilityClash(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryClash.createSpellClash(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryClash.createDrawbackClash(this);
            }
        }

        else if (this.api.equals("Cleanup")) {
            if (this.isDb) {
                spellAbility = AbilityFactoryCleanup.getDrawback(this);
            }
        }

        else if (this.api.equals("Clone")) {
            ai = new CloneAi();
            se = new CloneEffect();
        }

        else if (this.api.equals("CopyPermanent")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCopy.createAbilityCopyPermanent(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCopy.createSpellCopyPermanent(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCopy.createDrawbackCopyPermanent(this);
            }
        }

        else if (this.api.equals("CopySpell")) {
            if (this.isTargeted) { // Since all "CopySpell" ABs copy things on
                                   // the
                // Stack no need for it to be everywhere
                this.abTgt.setZone(ZoneType.Stack);
            }

            if (this.isAb) {
                spellAbility = AbilityFactoryCopy.createAbilityCopySpell(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCopy.createSpellCopySpell(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCopy.createDrawbackCopySpell(this);
            }

            hostCard.setCopiesSpells(true);
        }

        else if (this.api.equals("Counter")) {
            
            ai = new CounterAi();
            se = new CounterEffect();

            // Since all "Counter" ABs Counter things on the Stack no need for
            // it to be everywhere
            if (this.isTargeted) {
                this.abTgt.setZone(ZoneType.Stack);
            }
        }

        else if (this.api.equals("DamageAll")) {
            final AbilityFactoryDealDamage dd = new AbilityFactoryDealDamage(this);
            if (this.isAb) {
                spellAbility = dd.getAbilityDamageAll();
            } else if (this.isSp) {
                spellAbility = dd.getSpellDamageAll();
            } else if (this.isDb) {
                spellAbility = dd.getDrawbackDamageAll();
            }
        }

        else if (this.api.equals("DealDamage")) {
            final AbilityFactoryDealDamage dd = new AbilityFactoryDealDamage(this);

            if (this.isAb) {
                spellAbility = dd.getAbilityDealDamage();
            } else if (this.isSp) {
                spellAbility = dd.getSpellDealDamage();
            } else if (this.isDb) {
                spellAbility = dd.getDrawbackDealDamage();
            }
        }

        else if (this.api.equals("Debuff")) {
            ai = new DebuffAi();
            se = new DebuffEffect();
        }

        else if (this.api.equals("DebuffAll")) {
            ai = new DebuffAllAi();
            se = new DebuffAllEffect();
        }

        else if (this.api.equals("DelayedTrigger")) {
            ai = new DelayedTriggerAi();
            se = new DelayedTriggerEffect();
        }

        else if (this.api.equals("Destroy")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryDestroy.createAbilityDestroy(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryDestroy.createSpellDestroy(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryDestroy.createDrawbackDestroy(this);
            }
        }

        else if (this.api.equals("DestroyAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryDestroy.createAbilityDestroyAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryDestroy.createSpellDestroyAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryDestroy.createDrawbackDestroyAll(this);
            }
        }

        else if (this.api.equals("Dig")) {
            ai = new DigAi();
            se = new DigEffect();
        }

        else if (this.api.equals("DigUntil")) {
            ai = new DigUntilAi();
            se = new DigUntilEffect();
        }

        else if (this.api.equals("Discard")) {
            ai = new DiscardAi();
            se = new DiscardEffect();
        }

        else if (this.api.equals("DrainMana")) {
            ai = new DrainManaAi();
            se = new DrainManaEffect();
        }

        else if (this.api.equals("Draw")) {
            ai = new DrawAi();
            se = new DrawEffect();
        }

        else if (this.api.equals("EachDamage")) {
            final AbilityFactoryDealDamage dd = new AbilityFactoryDealDamage(this);
            if (this.isAb) {
                spellAbility = dd.getAbilityEachDamage();
            } else if (this.isSp) {
                spellAbility = dd.getSpellEachDamage();
            } else if (this.isDb) {
                spellAbility = dd.getDrawbackEachDamage();
            }
        }

        else if (this.api.equals("Effect")) {
            ai = new EffectAi();
            se = new EffectEffect();
        }

        else if (this.api.equals("EndTurn")) {
            ai = new EndTurnAi();
            se = new EndTurnEffect();
        }

        else if (this.api.equals("ExchangeLife")) {
            ai = new LifeExchangeAi();
            se = new LifeExchangeEffect();
        }

        else if (this.api.equals("ExchangeControl")) {
            final AbilityFactoryGainControl afControl = new AbilityFactoryGainControl(this);
            if (this.isAb) {
                spellAbility = afControl.getAbilityExchangeControl();
            } else if (this.isSp) {
                spellAbility = afControl.getSpellExchangeControl();
            } else if (this.isDb) {
                spellAbility = afControl.getDrawbackExchangeControl();
            }
        }

        else if (this.api.equals("Fight")) {
            final AbilityFactoryDealDamage dd = new AbilityFactoryDealDamage(this);
            if (this.isAb) {
                spellAbility = dd.getAbilityFight();
            } else if (this.isSp) {
                spellAbility = dd.getSpellFight();
            } else if (this.isDb) {
                spellAbility = dd.getDrawbackFight();
            }
        }

        else if (this.api.equals("FlipACoin")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryClash.createAbilityFlip(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryClash.createSpellFlip(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryClash.createDrawbackFlip(this);
            }
        }

        else if (this.api.equals("Fog")) {
            ai = new FogAi();
            se = new FogEffect();
        }

        else if (this.api.equals("GainControl")) {
            final AbilityFactoryGainControl afControl = new AbilityFactoryGainControl(this);

            if (this.isAb) {
                spellAbility = afControl.getAbilityGainControl();
            } else if (this.isSp) {
                spellAbility = afControl.getSpellGainControl();
            } else if (this.isDb) {
                spellAbility = afControl.getDrawbackGainControl();
            }
        }

        else if (this.api.equals("GainLife")) {
            ai = new LifeGainAi();
            se = new LifeGainEffect();
        }

        else if (this.api.equals("GenericChoice")) {
            ai = new CannotPlayAi();
            se = new ChooseGenericEffect();
        }

        else if (this.api.equals("LoseLife")) {
            ai = new LifeLoseAi();
            se = new LifeLoseEffect();
        }

        else if (this.api.equals("LosesGame")) {
            ai = new GameLossAi();
            se = new GameLossEffect();
        }

        else if (this.api.equals("Mana")) {
            final String produced = this.mapParams.get("Produced");
            if (this.isAb) {
                spellAbility = AbilityFactoryMana.createAbilityMana(this, produced);
            }
            if (this.isSp) {
                spellAbility = AbilityFactoryMana.createSpellMana(this, produced);
            }
            if (this.isDb) {
                spellAbility = AbilityFactoryMana.createDrawbackMana(this, produced);
            }
        }

        else if (this.api.equals("ManaReflected")) {
            // Reflected mana will have a filler for produced of "1"
            if (this.isAb) {
                spellAbility = AbilityFactoryMana.createAbilityManaReflected(this, "1");
            }
            if (this.isSp) { // shouldn't really happen i think?
                spellAbility = AbilityFactoryMana.createSpellManaReflected(this, "1");
            }
        }

        else if (this.api.equals("Mill")) {
            ai = new MillAi();
            se = new MillEffect();
        }

        else if (this.api.equals("MoveCounter")) {
            ai = new CountersMoveAi();
            se = new CountersMoveEffect();
        }

        else if (this.api.equals("MustAttack")) {
            ai = new MustAttackAi();
            se = new MustAttackEffect();
        }

        else if (this.api.equals("MustBlock")) {
            ai = new MustBlockAi();
            se = new MustBlockEffect();
        }

        else if (this.api.equals("NameCard")) {
            ai = new ChooseCardNameAi();
            se = new ChooseCardNameEffect();
        }

        else if (this.api.equals("Phases")) {
            ai = new PhasesAi();
            se = new PhasesEffect();
        }

        else if (this.api.equals("Play")) {
            ai = new PlayAi();
            se = new PlayEffect();
        }

        else if (this.api.equals("Poison")) {
            ai = new PoisonAi();
            se = new PoisonEffect();
        }

        else if (this.api.equals("PreventDamage")) {
            ai = new DamagePreventAi();
            se = new DamagePreventEffect();
        }

        else if (this.api.equals("PreventDamageAll")) {
            ai = new DamagePreventAllAi();
            se = new DamagePreventAllEffect();
        }

        else if (this.api.equals("Proliferate")) {
            ai = new CountersProliferateAi();
            se = new CountersProliferateEffect();
        }

        else if (this.api.equals("Protection")) {
            ai = new ProtectAi();
            se = new ProtectEffect();
        }

        else if (this.api.equals("ProtectionAll")) {
            ai = new ProtectAllAi();
            se = new ProtectAllEffect();
        }

        else if (this.api.equals("Pump")) {
            final AbilityFactoryPump afPump = new AbilityFactoryPump(this);

            if (this.isAb) {
                spellAbility = afPump.getAbilityPump();
            } else if (this.isSp) {
                spellAbility = afPump.getSpellPump();
            } else if (this.isDb) {
                spellAbility = afPump.getDrawbackPump();
            }
        }

        else if (this.api.equals("PumpAll")) {
            final AbilityFactoryPump afPump = new AbilityFactoryPump(this);

            if (this.isAb) {
                spellAbility = afPump.getAbilityPumpAll();
            } else if (this.isSp) {
                spellAbility = afPump.getSpellPumpAll();
            } else if (this.isDb) {
                spellAbility = afPump.getDrawbackPumpAll();
            }
        }

        else if (this.api.equals("PutCounter")) {
            ai = new CountersPutAi();
            se = new CountersPutEffect();
        }

        else if (this.api.equals("PutCounterAll")) {
            ai = new CountersPutAllAi();
            se = new CountersPutAllEffect();
        }

        else if (this.api.equals("RearrangeTopOfLibrary")) {
            ai = new RearrangeTopOfLibraryAi();
            se = new RearrangeTopOfLibraryEffect();
        }

        else if (this.api.equals("Regenerate")) {
            ai = new RegenerateAi();
            se = new RegenerateEffect();
        }

        else if (this.api.equals("RegenerateAll")) {
            ai = new RegenerateAllAi();
            se = new RegenerateAllEffect();
        }

        else if (this.api.equals("RemoveCounter")) {
            ai = new CountersRemoveAi();
            se = new CountersRemoveEffect();
        }

        else if (this.api.equals("RemoveCounterAll")) {
            ai = new CannotPlayAi();
            se = new CountersRemoveAllEffect();
        }

        else if (this.api.equals("RemoveFromCombat")) {
            ai = new RemoveFromCombatAi();
            se = new RemoveFromCombatEffect();
        }

        else if (this.api.equals("Repeat")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryRepeat.createAbilityRepeat(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryRepeat.createSpellRepeat(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryRepeat.createDrawbackRepeat(this);
            }
        }

        else if (this.api.equals("Reveal")) {
            ai = new RevealAi();
            se = new RevealEffect();
        }

        else if (this.api.equals("RevealHand")) {
            ai = new RevealHandAi();
            se = new RevealHandEffect();
        }

        else if (this.api.equals("Sacrifice")) {
            ai = new SacrificeAi();
            se = new SacrificeEffect();
        }

        else if (this.api.equals("SacrificeAll")) {
            ai = new SacrificeAllAi();
            se = new SacrificeAllEffect();
        }

        else if (this.api.equals("Scry")) {
            ai = new ScryAi();
            se = new ScryEffect();
        }

        else if (this.api.equals("SetLife")) {
            ai = new LifeSetAi();
            se = new LifeSetEffect();
        }

        else if (this.api.equals("SetState")) {
            ai = new SetStateAi();
            se = new SetStateEffect(); 
        }

        else if (this.api.equals("SetStateAll")) {
            ai = new SetStateAllAi();
            se = new SetStateAllEffect(); 
        }

        else if (this.api.equals("Shuffle")) {
            ai = new ShuffleAi();
            se = new ShuffleEffect();
        }

        else if (this.api.equals("StoreSVar")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryStoreSVar.createAbilityStoreSVar(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryStoreSVar.createSpellStoreSVar(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryStoreSVar.createDrawbackStoreSVar(this);
            }
        }

        else if (this.api.equals("Tap")) {
            ai = new TapAi();
            se = new TapEffect();
        }

        else if (this.api.equals("TapAll")) {
            ai = new TapAllAi();
            se = new TapAllEffect();
        }

        else if (this.api.equals("TapOrUntap")) {
            ai = new TapOrUntapAi();
            se = new TapOrUntapEffect();
        }

        else if (this.api.equals("Token")) {
            ai = new TokenAi();
            se = new TokenEffect();
        }

        else if (this.api.equals("TwoPiles")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryClash.createAbilityTwoPiles(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryClash.createSpellTwoPiles(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryClash.createDrawbackTwoPiles(this);
            }
        }

        else if (this.api.equals("UnattachAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAttach.createAbilityUnattachAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAttach.createSpellUnattachAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAttach.createDrawbackUnattachAll(this);
            }
        }

        else if (this.api.equals("Untap")) {
            ai = new UntapAi();
            se = new UntapEffect();
        }

        else if (this.api.equals("UntapAll")) {
            ai = new UntapAllAi();
            se = new UntapAllEffect();
        }

        else if (this.api.equals("WinsGame")) {
            ai = new GameWinAi();
            se = new GameWinEffect();
        }
        
        // build code is here once for the refactored APIs
        if ( se != null && ai != null && spellAbility == null)
        {
            if (this.isAb) {
                spellAbility = new CommonAbility(this.getHostCard(), this.getAbCost(), this.getAbTgt(), this.getMapParams(), se, ai);
            } else if (this.isSp) {
                spellAbility = new CommonSpell(this.getHostCard(), this.getAbCost(), this.getAbTgt(), this.getMapParams(), se, ai);
            } else if (this.isDb) {
                spellAbility = new CommonDrawback(this.getHostCard(), this.getAbTgt(), this.getMapParams(), se, ai);
            }
        }
        
        // //////////////////////
        //
        // End API matching. The above APIs are listed in alphabetical order.
        //
        // //////////////////////

        if (spellAbility == null) {
            final StringBuilder msg = new StringBuilder();
            msg.append("AbilityFactory : SpellAbility was not created for ");
            msg.append(hostCard.getName());
            msg.append(". Looking for API: ").append(this.api);
            throw new RuntimeException(msg.toString());
        }

        // *********************************************
        // set universal properties of the SpellAbility

        spellAbility.setAbilityFactory(this);

        if (this.mapParams.containsKey("References")) {
            for (String svar : this.mapParams.get("References").split(",")) {
                spellAbility.setSVar(svar, this.hostC.getSVar(svar));
            }
        }

        if (getMapParams().containsKey("SubAbility"))
            spellAbility.setSubAbility(this.getSubAbility());

        if (spellAbility instanceof SpellPermanent) {
            spellAbility.setDescription(spellAbility.getSourceCard().getName());
        } else if (this.hasSpDesc) {
            final StringBuilder sb = new StringBuilder();

            if (!this.isDb) { // SubAbilities don't have Costs or Cost
                              // descriptors
                if (this.mapParams.containsKey("PrecostDesc")) {
                    sb.append(this.mapParams.get("PrecostDesc")).append(" ");
                }
                if (this.mapParams.containsKey("CostDesc")) {
                    sb.append(this.mapParams.get("CostDesc")).append(" ");
                } else {
                    sb.append(this.abCost.toString());
                }
            }

            sb.append(this.mapParams.get("SpellDescription"));

            spellAbility.setDescription(sb.toString());
        } else {
            spellAbility.setDescription("");
        }

        if (this.mapParams.containsKey("NonBasicSpell")) {
            spellAbility.setBasicSpell(false);
        }

        this.makeRestrictions(spellAbility);
        this.makeConditions(spellAbility);

        return spellAbility;
    }

    /**
     * <p>
     * makeRestrictions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void makeRestrictions(final SpellAbility sa) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityRestriction restrict = sa.getRestrictions();
        if (this.mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        restrict.setRestrictions(this.mapParams);
    }

    /**
     * <p>
     * makeConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void makeConditions(final SpellAbility sa) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityCondition condition = sa.getConditions();
        if (this.mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        condition.setConditions(this.mapParams);
    }

    /**
     * <p>
     * checkConditional.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean checkConditional(final SpellAbility sa) {
        return sa.getConditions().checkConditions(sa);
    }

    // Easy creation of SubAbilities
    /**
     * <p>
     * getSubAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    private final AbilitySub getSubAbility() {
        AbilitySub abSub = null;

        String sSub = this.getMapParams().get("SubAbility");

        sSub = this.getHostCard().getSVar(sSub);

        if (!sSub.equals("")) {
            final AbilityFactory afDB = new AbilityFactory();
            abSub = (AbilitySub) afDB.getAbility(sSub, this.getHostCard());
        } else {
            System.out.println("SubAbility not found for: " + this.getHostCard());
        }

        return abSub;
    }

    public static ArrayList<String> getProtectionList(final Map<String, String> params) {
        final ArrayList<String> gains = new ArrayList<String>();
    
        final String gainStr = params.get("Gains");
        if (gainStr.equals("Choice")) {
            String choices = params.get("Choices");
    
            // Replace AnyColor with the 5 colors
            if (choices.contains("AnyColor")) {
                gains.addAll(Arrays.asList(Constant.Color.ONLY_COLORS));
                choices = choices.replaceAll("AnyColor,?", "");
            }
            // Add any remaining choices
            if (choices.length() > 0) {
                gains.addAll(Arrays.asList(choices.split(",")));
            }
        } else {
            gains.addAll(Arrays.asList(gainStr.split(",")));
        }
        return gains;
    }

    /**
     * <p>
     * playReusable.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean playReusable(final Player ai, final SpellAbility sa) {
        // TODO probably also consider if winter orb or similar are out

        if (sa.getPayCosts() == null) {
            return true; // This is only true for Drawbacks and triggers
        }

        if (!sa.getPayCosts().isReusuableResource()) {
            return false;
        }

        if (sa.getRestrictions().getPlaneswalker() && Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return true;
        }

        return Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.END_OF_TURN) 
             && Singletons.getModel().getGame().getPhaseHandler().getNextTurn().equals(ai);
    }

    /**
     * <p>
     * isSorcerySpeed.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean isSorcerySpeed(final SpellAbility sa) {
        if (sa.isSpell()) {
            return sa.getSourceCard().isSorcery();
        } else if (sa.isAbility()) {
            return sa.getRestrictions().isSorcerySpeed();
        }

        return false;
    }

    /**
     * <p>
     * isInstantSpeed. To be used for mana abilities like Lion's Eye Diamond
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean isInstantSpeed(final SpellAbility sa) {
        if (sa.isAbility()) {
            return sa.getRestrictions().isInstantSpeed();
        }

        return false;
    }

    // Utility functions used by the AFs
    /**
     * <p>
     * calculateAmount.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param amount
     *            a {@link java.lang.String} object.
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int calculateAmount(final Card card, String amount, final SpellAbility ability) {
        // amount can be anything, not just 'X' as long as sVar exists

        if (amount == null) {
            return 0;
        }

        // If Amount is -X, strip the minus sign before looking for an SVar of
        // that kind
        int multiplier = 1;
        if (amount.startsWith("-")) {
            multiplier = -1;
            amount = amount.substring(1);
        } else if (amount.startsWith("+")) {
            amount = amount.substring(1);
        }

        String svarval;
        if (ability != null) {

            svarval = ability.getSVar(amount);
            if (svarval.equals("")) {
                try {
                    Integer.parseInt(amount);
                }
                catch (NumberFormatException ignored) {
                    //If this is reached, amount wasn't an integer
                    //Print a warning to console to help debug if an ability is not stolen properly.
                    StringBuilder sb = new StringBuilder("WARNING:SVar fallback to Card (");
                    sb.append(card.getName()).append(") and Ability(").append(ability.toString()).append(")");
                    System.out.println(sb.toString());
                    svarval = card.getSVar(amount);
                }
            }
        } else {
            svarval = card.getSVar(amount);
        }

        if (!svarval.equals("")) {
            final String[] calcX = svarval.split("\\$");
            if ((calcX.length == 1) || calcX[1].equals("none")) {
                return 0;
            }

            if (calcX[0].startsWith("Count")) {
                return CardFactoryUtil.xCount(card, calcX[1], ability) * multiplier;
            }

            if (calcX[0].startsWith("Number")) {
                return CardFactoryUtil.xCount(card, svarval) * multiplier;
            } else if (calcX[0].startsWith("SVar")) {
                final String[] l = calcX[1].split("/");
                final String[] m = CardFactoryUtil.parseMath(l);
                return CardFactoryUtil.doXMath(AbilityFactory.calculateAmount(card, l[0], ability), m, card)
                        * multiplier;
            } else if (calcX[0].startsWith("Remembered")) {
                // Add whole Remembered list to handlePaid
                final List<Card> list = new ArrayList<Card>();
                if (card.getRemembered().isEmpty()) {
                    final Card newCard = Singletons.getModel().getGame().getCardState(card);
                    for (final Object o : newCard.getRemembered()) {
                        if (o instanceof Card) {
                            list.add(Singletons.getModel().getGame().getCardState((Card) o));
                        }
                    }
                }

                if (calcX[0].endsWith("LKI")) { // last known information
                    for (final Object o : card.getRemembered()) {
                        if (o instanceof Card) {
                            list.add((Card) o);
                        }
                    }
                } else {
                    for (final Object o : card.getRemembered()) {
                        if (o instanceof Card) {
                            list.add(Singletons.getModel().getGame().getCardState((Card) o));
                        }
                    }
                }

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
            } else if (calcX[0].startsWith("Imprinted")) {
                // Add whole Imprinted list to handlePaid
                final List<Card> list = new ArrayList<Card>();
                for (final Card c : card.getImprinted()) {
                    list.add(Singletons.getModel().getGame().getCardState(c));
                }

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
            } else if (calcX[0].matches("Enchanted")) {
                // Add whole Enchanted list to handlePaid
                final List<Card> list = new ArrayList<Card>();
                if (card.isEnchanting()) {
                    Object o = card.getEnchanting();
                    if (o instanceof Card) {
                        list.add(Singletons.getModel().getGame().getCardState((Card) o));
                    }
                }
                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
            } else if (ability != null) {
                // Player attribute counting
                if (calcX[0].startsWith("TargetedPlayer")) {
                    final ArrayList<Player> players = new ArrayList<Player>();
                    final SpellAbility saTargeting = (ability.getTarget() == null) ? AbilityFactory
                            .findParentsTargetedPlayer(ability) : ability;
                    if (saTargeting.getTarget() != null) {
                        players.addAll(saTargeting.getTarget().getTargetPlayers());
                    } else {
                        players.addAll(AbilityFactory.getDefinedPlayers(card, saTargeting.getAbilityFactory()
                                .getMapParams().get("Defined"), saTargeting));
                    }
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }
                if (calcX[0].startsWith("TargetedObjects")) {
                    final ArrayList<Object> objects = new ArrayList<Object>();
                    final SpellAbility saTargeting = (ability.getTarget() == null) ? AbilityFactory
                            .findParentsTargetedPlayer(ability) : ability;
                    if (saTargeting.getTarget() != null) {
                        objects.addAll(saTargeting.getTarget().getTargets());
                    } else {
                        objects.addAll(AbilityFactory.getDefinedObjects(card, saTargeting.getAbilityFactory()
                                .getMapParams().get("Defined"), saTargeting));
                    }
                    return CardFactoryUtil.objectXCount(objects, calcX[1], card) * multiplier;
                }
                if (calcX[0].startsWith("TargetedController")) {
                    final ArrayList<Player> players = new ArrayList<Player>();
                    final ArrayList<Card> list = AbilityFactory.getDefinedCards(card, "Targeted", ability);
                    final ArrayList<SpellAbility> sas = AbilityFactory.getDefinedSpellAbilities(card, "Targeted",
                            ability);

                    for (final Card c : list) {
                        final Player p = c.getController();
                        if (!players.contains(p)) {
                            players.add(p);
                        }
                    }
                    for (final SpellAbility s : sas) {
                        final Player p = s.getSourceCard().getController();
                        if (!players.contains(p)) {
                            players.add(p);
                        }
                    }
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }
                if (calcX[0].startsWith("TriggeredPlayer")) {
                    final SpellAbility root = ability.getRootSpellAbility();
                    Object o = root.getTriggeringObject("Player");
                    final ArrayList<Player> players = new ArrayList<Player>();
                    if (o instanceof Player) {
                        players.add((Player) o);
                    }
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }
                // Added on 9/30/12 (ArsenalNut) - Ended up not using but might be useful in future
                /*
                if (calcX[0].startsWith("EnchantedController")) {
                    final ArrayList<Player> players = new ArrayList<Player>();
                    players.addAll(AbilityFactory.getDefinedPlayers(card, "EnchantedController", ability));
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }
                */

                List<Card> list = new ArrayList<Card>();
                if (calcX[0].startsWith("Sacrificed")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Sacrificed");
                } else if (calcX[0].startsWith("Discarded")) {
                    final SpellAbility root = AbilityFactory.findRootAbility(ability);
                    list = root.getPaidList("Discarded");
                    if ((null == list) && root.isTrigger()) {
                        list = root.getSourceCard().getSpellPermanent().getPaidList("Discarded");
                    }
                } else if (calcX[0].startsWith("Exiled")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Exiled");
                } else if (calcX[0].startsWith("Tapped")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Tapped");
                } else if (calcX[0].startsWith("Revealed")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Revealed");
                } else if (calcX[0].startsWith("Targeted")) {
                    final Target t = ability.getTarget();
                    if (null != t) {
                        final ArrayList<Object> all = t.getTargets();
                        if (!all.isEmpty() && (all.get(0) instanceof SpellAbility)) {
                            final SpellAbility saTargeting = AbilityFactory.findParentsTargetedSpellAbility(ability);
                            list = new ArrayList<Card>();
                            final ArrayList<SpellAbility> sas = saTargeting.getTarget().getTargetSAs();
                            for (final SpellAbility sa : sas) {
                                list.add(sa.getSourceCard());
                            }
                        } else {
                            final SpellAbility saTargeting = AbilityFactory.findParentsTargetedCard(ability);
                            list = new ArrayList<Card>(saTargeting.getTarget().getTargetCards());
                        }
                    } else {
                        final SpellAbility parent = AbilityFactory.findParentsTargetedCard(ability);

                        if (parent != null) {
                            final ArrayList<Object> all = parent.getTarget().getTargets();
                            if (!all.isEmpty() && (all.get(0) instanceof SpellAbility)) {
                                list = new ArrayList<Card>();
                                final ArrayList<SpellAbility> sas = parent.getTarget().getTargetSAs();
                                for (final SpellAbility sa : sas) {
                                    list.add(sa.getSourceCard());
                                }
                            } else {
                                final SpellAbility saTargeting = AbilityFactory.findParentsTargetedCard(ability);
                                list = new ArrayList<Card>(saTargeting.getTarget().getTargetCards());
                            }
                        }
                    }
                } else if (calcX[0].startsWith("Triggered")) {
                    final SpellAbility root = ability.getRootSpellAbility();
                    list = new ArrayList<Card>();
                    list.add((Card) root.getTriggeringObject(calcX[0].substring(9)));
                } else if (calcX[0].startsWith("TriggerCount")) {
                    // TriggerCount is similar to a regular Count, but just
                    // pulls Integer Values from Trigger objects
                    final SpellAbility root = ability.getRootSpellAbility();
                    final String[] l = calcX[1].split("/");
                    final String[] m = CardFactoryUtil.parseMath(l);
                    final int count = (Integer) root.getTriggeringObject(l[0]);

                    return CardFactoryUtil.doXMath(count, m, card) * multiplier;
                } else if (calcX[0].startsWith("Replaced")) {
                    final SpellAbility root = ability.getRootSpellAbility();
                    list = new ArrayList<Card>();
                    list.add((Card) root.getReplacingObject(calcX[0].substring(8)));
                } else if (calcX[0].startsWith("ReplaceCount")) {
                    // ReplaceCount is similar to a regular Count, but just
                    // pulls Integer Values from Replacement objects
                    final SpellAbility root = ability.getRootSpellAbility();
                    final String[] l = calcX[1].split("/");
                    final String[] m = CardFactoryUtil.parseMath(l);
                    final int count = (Integer) root.getReplacingObject(l[0]);

                    return CardFactoryUtil.doXMath(count, m, card) * multiplier;
                } else {

                    return 0;
                }

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;

            } else {
                return 0;
            }
        }
        if (amount.equals("ChosenX") || amount.equals("ChosenY")) {
            // isn't made yet
            return 0;
        }
        // cost hasn't been paid yet
        if (amount.startsWith("Cost")) {
            return 0;
        }

        return Integer.parseInt(amount) * multiplier;
    }

    private static Card findEffectRoot(Card startCard) {

        Card cc = startCard.getEffectSource();
        if (cc != null) {

            if (cc.isType("Effect")) {
                return findEffectRoot(cc);
            }
            return cc;
        }

        return null; //If this happens there is a card in the game that is not in any zone
    }

    // should the three getDefined functions be merged into one? Or better to
    // have separate?
    // If we only have one, each function needs to Cast the Object to the
    // appropriate type when using
    // But then we only need update one function at a time once the casting is
    // everywhere.
    // Probably will move to One function solution sometime in the future
    /**
     * <p>
     * getDefinedCards.
     * </p>
     * 
     * @param hostCard
     *            a {@link forge.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Card> getDefinedCards(final Card hostCard, final String def, final SpellAbility sa) {
        final ArrayList<Card> cards = new ArrayList<Card>();
        final String defined = (def == null) ? "Self" : def; // default to Self

        Card c = null;

        if (defined.equals("Self")) {
            c = hostCard;
        }

        else if (defined.equals("OriginalHost")) {
            c = sa.getOriginalHost();
        }

        else if (defined.equals("EffectSource")) {
            if (hostCard.isType("Effect")) {
                c = findEffectRoot(hostCard);
            }
        }

        else if (defined.equals("Equipped")) {
            c = hostCard.getEquippingCard();
        }

        else if (defined.equals("Enchanted")) {
            c = hostCard.getEnchantingCard();
            if ((c == null) && (AbilityFactory.findRootAbility(sa) != null)
                    && (AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed") != null)
                    && !AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed").isEmpty()) {
                c = AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed").get(0).getEnchantingCard();
            }
        }

        else if (defined.equals("TopOfLibrary")) {
            final List<Card> lib = hostCard.getController().getCardsIn(ZoneType.Library);
            if (lib.size() > 0) {
                c = lib.get(0);
            } else {
                // we don't want this to fall through and return the "Self"
                return new ArrayList<Card>();
            }
        }

        else if (defined.equals("Targeted")) {
            final SpellAbility parent = AbilityFactory.findParentsTargetedCard(sa);
            if (parent != null) {
                if (parent.getTarget() != null && parent.getTarget().getTargetCards() != null) {
                    cards.addAll(parent.getTarget().getTargetCards());
                }
            }
        } else if (defined.startsWith("Triggered") && (sa != null)) {
            final SpellAbility root = sa.getRootSpellAbility();
            if (defined.contains("LKICopy")) { //TriggeredCardLKICopy
                final Object crd = root.getTriggeringObject(defined.substring(9, 13));
                if (crd instanceof Card) {
                    c = (Card) crd;
                }
            }
            else {
                final Object crd = root.getTriggeringObject(defined.substring(9));
                if (crd instanceof Card) {
                    c = Singletons.getModel().getGame().getCardState((Card) crd);
                    c = (Card) crd;
                } else if (crd instanceof List<?>) {
                    for (final Card cardItem : (List<Card>) crd) {
                        cards.add(cardItem);
                    }
                }
            }
        } else if (defined.startsWith("Replaced") && (sa != null)) {
            final SpellAbility root = sa.getRootSpellAbility();
            final Object crd = root.getReplacingObject(defined.substring(8));
            if (crd instanceof Card) {
                c = Singletons.getModel().getGame().getCardState((Card) crd);
            } else if (crd instanceof List<?>) {
                for (final Card cardItem : (List<Card>) crd) {
                    cards.add(cardItem);
                }
            }
        } else if (defined.equals("Remembered")) {
            if (hostCard.getRemembered().isEmpty()) {
                final Card newCard = Singletons.getModel().getGame().getCardState(hostCard);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        cards.add(Singletons.getModel().getGame().getCardState((Card) o));
                    }
                }
            }

            for (final Object o : hostCard.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(Singletons.getModel().getGame().getCardState((Card) o));
                }
            }
        } else if (defined.equals("Clones")) {
            for (final Card clone : hostCard.getClones()) {
                cards.add(Singletons.getModel().getGame().getCardState(clone));
            }
        } else if (defined.equals("Imprinted")) {
            for (final Card imprint : hostCard.getImprinted()) {
                cards.add(Singletons.getModel().getGame().getCardState(imprint));
            }
        } else if (defined.startsWith("ThisTurnEntered")) {
            final String[] workingCopy = defined.split("_");
            ZoneType destination, origin;
            String validFilter;

            destination = ZoneType.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = ZoneType.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }
            for (final Card cl : CardUtil.getThisTurnEntered(destination, origin, validFilter, hostCard)) {
                cards.add(cl);
            }
        } else {
            List<Card> list = null;
            if (defined.startsWith("Sacrificed")) {
                list = AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed");
            }

            else if (defined.startsWith("Discarded")) {
                list = AbilityFactory.findRootAbility(sa).getPaidList("Discarded");
            }

            else if (defined.startsWith("Exiled")) {
                list = AbilityFactory.findRootAbility(sa).getPaidList("Exiled");
            }

            else if (defined.startsWith("Tapped")) {
                list = AbilityFactory.findRootAbility(sa).getPaidList("Tapped");
            }

            else {
                return cards;
            }

            for (final Card cl : list) {
                cards.add(cl);
            }
        }

        if (c != null) {
            cards.add(c);
        }

        return cards;
    }

    /**
     * <p>
     * getDefinedPlayers.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Player> getDefinedPlayers(final Card card, final String def, final SpellAbility sa) {
        final ArrayList<Player> players = new ArrayList<Player>();
        final String defined = (def == null) ? "You" : def;

        if (defined.equals("Targeted")) {
            final SpellAbility parent = AbilityFactory.findParentsTargetedPlayer(sa);
            if (parent != null) {
                if (parent.getTarget() != null) {
                    players.addAll(parent.getTarget().getTargetPlayers());
                }
            }
        } else if (defined.equals("TargetedController")) {
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(card, "Targeted", sa);
            final ArrayList<SpellAbility> sas = AbilityFactory.getDefinedSpellAbilities(card, "Targeted", sa);

            for (final Card c : list) {
                final Player p = c.getController();
                if (!players.contains(p)) {
                    players.add(p);
                }
            }
            for (final SpellAbility s : sas) {
                final Player p = s.getActivatingPlayer();
                if (!players.contains(p)) {
                    players.add(p);
                }
            }
        } else if (defined.equals("TargetedOwner")) {
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(card, "Targeted", sa);

            for (final Card c : list) {
                final Player p = c.getOwner();
                if (!players.contains(p)) {
                    players.add(p);
                }
            }
        } else if (defined.equals("Remembered")) {
            for (final Object rem : card.getRemembered()) {
                if (rem instanceof Player) {
                    players.add((Player) rem);
                }
            }
        } else if (defined.equals("RememberedOpponent")) {
            for (final Object rem : card.getRemembered()) {
                if (rem instanceof Player) {
                    players.add(((Player) rem).getOpponent());
                }
            }
        } else if (defined.startsWith("Triggered")) {
            final SpellAbility root = sa.getRootSpellAbility();
            Object o = null;
            if (defined.endsWith("Controller")) {
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 10);
                final Object c = root.getTriggeringObject(triggeringType);
                if (c instanceof Card) {
                    o = ((Card) c).getController();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getSourceCard().getController();
                }
            } else if (defined.endsWith("Opponent")) {
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 8);
                final Object c = root.getTriggeringObject(triggeringType);
                if (c instanceof Card) {
                    o = ((Card) c).getController().getOpponent();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getSourceCard().getController().getOpponent();
                }
            } else if (defined.endsWith("Owner")) {
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 5);
                final Object c = root.getTriggeringObject(triggeringType);
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
            } else {
                final String triggeringType = defined.substring(9);
                o = root.getTriggeringObject(triggeringType);
            }
            if (o != null) {
                if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (!players.contains(p)) {
                        players.add(p);
                    }
                }
            }
        } else if (defined.startsWith("Replaced")) {
            final SpellAbility root = sa.getRootSpellAbility();
            Object o = null;
            if (defined.endsWith("Controller")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 10);
                final Object c = root.getReplacingObject(replacingType);
                if (c instanceof Card) {
                    o = ((Card) c).getController();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getSourceCard().getController();
                }
            } else if (defined.endsWith("Opponent")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 8);
                final Object c = root.getReplacingObject(replacingType);
                if (c instanceof Card) {
                    o = ((Card) c).getController().getOpponent();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getSourceCard().getController().getOpponent();
                }
            } else if (defined.endsWith("Owner")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 5);
                final Object c = root.getReplacingObject(replacingType);
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
            } else {
                final String replacingType = defined.substring(8);
                o = root.getReplacingObject(replacingType);
            }
            if (o != null) {
                if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (!players.contains(p)) {
                        players.add(p);
                    }
                }
            }
        } else if (defined.equals("EnchantedController")) {
            if (card.getEnchantingCard() == null) {
                return players;
            }
            final Player p = card.getEnchantingCard().getController();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("EnchantedOwner")) {
            if (card.getEnchantingCard() == null) {
                return players;
            }
            final Player p = card.getEnchantingCard().getOwner();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("EnchantedPlayer")) {
            final Object o = sa.getSourceCard().getEnchanting();
            if (o instanceof Player) {
                if (!players.contains(o)) {
                    players.add((Player) o);
                }
            }
        } else if (defined.equals("AttackingPlayer")) {
            final Player p = Singletons.getModel().getGame().getCombat().getAttackingPlayer();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("DefendingPlayer")) {
            final Player p = Singletons.getModel().getGame().getCombat().getDefendingPlayerRelatedTo(card);
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("ChosenPlayer")) {
            final Player p = card.getChosenPlayer();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("You") || defined.equals("Opponent") || defined.equals("Each")) {
            if (defined.equals("You") || defined.equals("Each")) {
                players.add(sa.getActivatingPlayer());
            }

            if (defined.equals("Opponent") || defined.equals("Each")) {
                players.add(sa.getActivatingPlayer().getOpponent());
            }
        } else {
            for (Player p : Singletons.getModel().getGame().getPlayers()) {
                if (p.isValid(defined, sa.getActivatingPlayer(), sa.getSourceCard())) {
                    players.add(p);
                }
            }
        }
        return players;
    }

    /**
     * <p>
     * getDefinedSpellAbilities.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<SpellAbility> getDefinedSpellAbilities(final Card card, final String def,
            final SpellAbility sa) {
        final ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
        final String defined = (def == null) ? "Self" : def; // default to Self

        SpellAbility s = null;

        // TODO - this probably needs to be fleshed out a bit, but the basics
        // work
        if (defined.equals("Self")) {
            s = sa;
        } else if (defined.equals("Targeted")) {
            final SpellAbility parent = AbilityFactory.findParentsTargetedSpellAbility(sa);
            if (parent != null) {
                if (parent.getTarget() != null) {
                    sas.addAll(parent.getTarget().getTargetSAs());
                }
            }
        } else if (defined.startsWith("Triggered")) {
            final SpellAbility root = sa.getRootSpellAbility();

            final String triggeringType = defined.substring(9);
            final Object o = root.getTriggeringObject(triggeringType);
            if (o instanceof SpellAbility) {
                s = (SpellAbility) o;
            }
        } else if (defined.equals("Remembered")) {
            for (final Object o : card.getRemembered()) {
                if (o instanceof Card) {
                    final Card rem = (Card) o;
                    sas.addAll(Singletons.getModel().getGame().getCardState(rem).getSpellAbilities());
                }
            }
        } else if (defined.equals("Imprinted")) {
            for (final Card imp : card.getImprinted()) {
                sas.addAll(imp.getSpellAbilities());
            }
        } else if (defined.equals("EffectSource")) {
            if (card.getEffectSource() != null) {
                sas.addAll(card.getEffectSource().getSpellAbilities());
            }
        } else if (defined.equals("Imprinted.doesNotShareNameWith+TriggeredCard+Exiled")) {
            //get Imprinted list
            ArrayList<SpellAbility> imprintedCards = new ArrayList<SpellAbility>();
            for (final Card imp : card.getImprinted()) {
                imprintedCards.addAll(imp.getSpellAbilities());
            } //get Triggered card
            Card triggeredCard = null;
            final SpellAbility root = sa.getRootSpellAbility();
            final Object crd = root.getTriggeringObject("Card");
            if (crd instanceof Card) {
                triggeredCard = Singletons.getModel().getGame().getCardState((Card) crd);
            } //find the imprinted card that does not share a name with the triggered card
            for (final SpellAbility spell : imprintedCards) {
                if (!spell.getSourceCard().getName().equals(triggeredCard.getName())) {
                    sas.add(spell);
                }
            } //is it exiled?
            if (!sas.get(0).getSourceCard().isInZone(ZoneType.Exile)) {
                sas.clear();
            }
        }

        if (s != null) {
            sas.add(s);
        }

        return sas;
    }

    /**
     * <p>
     * getDefinedObjects.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Object> getDefinedObjects(final Card card, final String def, final SpellAbility sa) {
        final ArrayList<Object> objects = new ArrayList<Object>();
        final String defined = (def == null) ? "Self" : def;

        objects.addAll(AbilityFactory.getDefinedPlayers(card, defined, sa));
        objects.addAll(AbilityFactory.getDefinedCards(card, defined, sa));
        objects.addAll(AbilityFactory.getDefinedSpellAbilities(card, defined, sa));
        return objects;
    }

    /**
     * <p>
     * findRootAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findRootAbility(final SpellAbility sa) {
        SpellAbility parent = sa;
        while (parent instanceof AbilitySub) {
            parent = ((AbilitySub) parent).getParent();
        }

        return parent;
    }

    /**
     * <p>
     * findParentsTargetedCard.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findParentsTargetedCard(final SpellAbility sa) {
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof AbilitySub) || ((AbilitySub) parent).getParent() == null) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while (parent.getTarget() == null
                || parent.getTarget().getTargetCards() == null
                || parent.getTarget().getTargetCards().size() == 0);

        return parent;
    }

    /**
     * <p>
     * findParentsTargetedSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    private static SpellAbility findParentsTargetedSpellAbility(final SpellAbility sa) {
        if (sa.getTarget() != null && !sa.getTarget().getTargetSAs().isEmpty()) {
            return sa;
        }
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof AbilitySub) || ((AbilitySub) parent).getParent() == null) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while (parent.getTarget() == null || parent.getTarget().getTargetSAs().isEmpty());

        return parent;
    }

    /**
     * <p>
     * findParentsTargetedPlayer.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findParentsTargetedPlayer(final SpellAbility sa) {
        if (sa.getTarget() != null && !sa.getTarget().getTargetPlayers().isEmpty()) {
            return sa;
        }
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof AbilitySub) || ((AbilitySub) parent).getParent() == null) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while (parent.getTarget() == null || parent.getTarget().getTargetPlayers().isEmpty());

        return parent;
    }

    /**
     * <p>
     * predictThreatenedObjects.
     * </p>
     * 
     * @param saviourAf
     *            a AbilityFactory object
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(final Player aiPlayer, final AbilityFactory saviourAf) {
        final ArrayList<Object> objects = new ArrayList<Object>();
        if (Singletons.getModel().getGame().getStack().size() == 0) {
            return objects;
        }

        // check stack for something that will kill this
        final SpellAbility topStack = Singletons.getModel().getGame().getStack().peekAbility();
        objects.addAll(AbilityFactory.predictThreatenedObjects(aiPlayer, saviourAf, topStack));

        return objects;
    }

    /**
     * <p>
     * predictThreatenedObjects.
     * </p>
     * 
     * @param saviourAf
     *            a AbilityFactory object
     * @param topStack
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(final Player aiPlayer, final AbilityFactory saviourAf, final SpellAbility topStack) {
        ArrayList<Object> objects = new ArrayList<Object>();
        final ArrayList<Object> threatened = new ArrayList<Object>();
        String saviourApi = "";
        HashMap<String, String> saviourParams = null;
        if (saviourAf != null) {
            saviourApi = saviourAf.getAPI();
            saviourParams = saviourAf.getMapParams();
        }

        if (topStack == null) {
            return objects;
        }

        final Card source = topStack.getSourceCard();
        final AbilityFactory topAf = topStack.getAbilityFactory();

        // Can only Predict things from AFs
        if (topAf != null) {
            final Target tgt = topStack.getTarget();
            final HashMap<String, String> threatParams = topAf.getMapParams();

            if (tgt == null) {
                if (threatParams.containsKey("Defined")) {
                    objects = AbilityFactory.getDefinedObjects(source, threatParams.get("Defined"), topStack);
                } else if (threatParams.containsKey("ValidCards")) {
                    List<Card> battleField = aiPlayer.getCardsIn(ZoneType.Battlefield);
                    List<Card> cards = CardLists.getValidCards(battleField, threatParams.get("ValidCards").split(","), source.getController(), source);
                    for (Card card : cards) {
                        objects.add(card);
                    }
                }
            } else {
                objects = tgt.getTargets();
            }

            // Determine if Defined Objects are "threatened" will be destroyed
            // due to this SA

            final String threatApi = topAf.getAPI();

            // Lethal Damage => prevent damage/regeneration/bounce/shroud
            if (threatApi.equals("DealDamage") || threatApi.equals("DamageAll")) {
                // If PredictDamage is >= Lethal Damage
                final int dmg = AbilityFactory.calculateAmount(topStack.getSourceCard(),
                        topAf.getMapParams().get("NumDmg"), topStack);
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;

                        // indestructible
                        if (c.hasKeyword("Indestructible")) {
                            continue;
                        }

                        // already regenerated
                        if (c.getShield() > 0) {
                            continue;
                        }

                        // don't use it on creatures that can't be regenerated
                        if (saviourApi.startsWith("Regenerate") && !c.canBeShielded()) {
                            continue;
                        }

                        // give Shroud to targeted creatures
                        if (saviourApi.equals("Pump") && tgt == null && saviourParams.containsKey("KW")
                                && (saviourParams.get("KW").endsWith("Shroud")
                                        || saviourParams.get("KW").endsWith("Hexproof"))) {
                            continue;
                        }

                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken())) {
                            continue;
                        }

                        if (c.predictDamage(dmg, source, false) >= c.getKillDamage()) {
                            threatened.add(c);
                        }
                    } else if (o instanceof Player) {
                        final Player p = (Player) o;

                        if (source.hasKeyword("Infect")) {
                            if (p.predictDamage(dmg, source, false) >= p.getPoisonCounters()) {
                                threatened.add(p);
                            }
                        } else if (p.predictDamage(dmg, source, false) >= p.getLife()) {
                            threatened.add(p);
                        }
                    }
                }
            }
            // Destroy => regeneration/bounce/shroud
            else if ((threatApi.equals("Destroy") || threatApi.equals("DestroyAll"))
                    && ((saviourApi.startsWith("Regenerate") && !threatParams.containsKey("NoRegen")) || saviourApi
                            .equals("ChangeZone") || saviourApi.equals("Pump"))) {
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
                        // indestructible
                        if (c.hasKeyword("Indestructible")) {
                            continue;
                        }

                        // already regenerated
                        if (c.getShield() > 0) {
                            continue;
                        }

                        // give Shroud to targeted creatures
                        if (saviourApi.equals("Pump") && tgt == null && saviourParams.containsKey("KW")
                                && (saviourParams.get("KW").endsWith("Shroud")
                                        || saviourParams.get("KW").endsWith("Hexproof"))) {
                            continue;
                        }

                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken())) {
                            continue;
                        }

                        // don't use it on creatures that can't be regenerated
                        if (saviourApi.equals("Regenerate") && !c.canBeShielded()) {
                            continue;
                        }
                        threatened.add(c);
                    }
                }
            }
            // Exiling => bounce/shroud
            else if ((threatApi.equals("ChangeZone") || threatApi.equals("ChangeZoneAll"))
                    && (saviourApi.equals("ChangeZone") || saviourApi.equals("Pump"))
                    && threatParams.containsKey("Destination")
                    && threatParams.get("Destination").equals("Exile")) {
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
                        // give Shroud to targeted creatures
                        if (saviourApi.equals("Pump") && tgt == null && saviourParams.containsKey("KW")
                                && (saviourParams.get("KW").endsWith("Shroud")
                                        || saviourParams.get("KW").endsWith("Hexproof"))) {
                            continue;
                        }

                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken())) {
                            continue;
                        }

                        threatened.add(c);
                    }
                }
            }
        }

        threatened.addAll(AbilityFactory.predictThreatenedObjects(aiPlayer, saviourAf, topStack.getSubAbility()));
        return threatened;
    }

    /**
     * <p>
     * handleRemembering.
     * </p>
     * 
     * @param sa
     *            a SpellAbility object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public static void handleRemembering(final SpellAbility sa, final AbilityFactory af) {
        final HashMap<String, String> params = af.getMapParams();
        Card host;

        if (!params.containsKey("RememberTargets") && !params.containsKey("RememberToughness")
                && !params.containsKey("RememberCostCards")) {
            return;
        }

        host = sa.getSourceCard();

        if (params.containsKey("ForgetOtherTargets")) {
            host.clearRemembered();
        }

        final Target tgt = sa.getTarget();

        if (params.containsKey("RememberTargets")) {
            final ArrayList<Object> tgts = (tgt == null) ? new ArrayList<Object>() : tgt.getTargets();
            for (final Object o : tgts) {
                host.addRemembered(o);
            }
        }

        if (params.containsKey("RememberCostCards")) {
            if (params.get("Cost").contains("Exile")) {
                final List<Card> paidListExiled = sa.getPaidList("Exiled");
                for (final Card exiledAsCost : paidListExiled) {
                    host.addRemembered(exiledAsCost);
                }
            }
        }
    }

    /**
     * Filter list by type.
     * 
     * @param list
     *            a CardList
     * @param type
     *            a card type
     * @param sa
     *            a SpellAbility
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> filterListByType(final List<Card> list, String type, final SpellAbility sa) {
        if (type == null) {
            return list;
        }

        // Filter List Can send a different Source card in for things like
        // Mishra and Lobotomy

        Card source = sa.getSourceCard();
        final Object o;
        if (type.startsWith("Triggered")) {
            if (type.contains("Card")) {
                o = sa.getTriggeringObject("Card");
            } else if (type.contains("Attacker")) {
                o = sa.getTriggeringObject("Attacker");
            } else if (type.contains("Blocker")) {
                o = sa.getTriggeringObject("Blocker");
            } else {
                o = sa.getTriggeringObject("Card");
            }

            if (!(o instanceof Card)) {
                return new ArrayList<Card>();
            }

            if (type.equals("Triggered") || (type.equals("TriggeredCard")) || (type.equals("TriggeredAttacker")) || (type.equals("TriggeredBlocker"))) {
                type = "Card.Self";
            }

            source = (Card) (o);
            if (type.contains("TriggeredCard")) {
                type = type.replace("TriggeredCard", "Card");
            } else if (type.contains("TriggeredAttacker")) {
                type = type.replace("TriggeredAttacker", "Card");
            } else if (type.contains("TriggeredBlocker")) {
                type = type.replace("TriggeredBlocker", "Card");
            } else {
                type = type.replace("Triggered", "Card");
            }

        } else if (type.startsWith("Targeted")) {
            source = null;
            final SpellAbility parent = AbilityFactory.findParentsTargetedCard(sa);
            if (parent != null) {
                if (parent.getTarget() != null) {
                    if (!parent.getTarget().getTargetCards().isEmpty()) {
                        source = parent.getTarget().getTargetCards().get(0);
                    } else if (!parent.getTarget().getTargetSAs().isEmpty()) {
                        source = parent.getTarget().getTargetSAs().get(0).getSourceCard();
                    }
                }
            }
            if (source == null) {
                return new ArrayList<Card>();
            }

            if (type.startsWith("TargetedCard")) {
                type = type.replace("TargetedCard", "Card");
            } else {
                type = type.replace("Targeted", "Card");
            }

        } else if (type.startsWith("Remembered")) {
            boolean hasRememberedCard = false;
            for (final Object object : source.getRemembered()) {
                if (object instanceof Card) {
                    hasRememberedCard = true;
                    source = (Card) object;
                    type = type.replace("Remembered", "Card");
                    break;
                }
            }

            if (!hasRememberedCard) {
                return new ArrayList<Card>();
            }
        } else if (type.equals("Card.AttachedBy")) {
            source = source.getEnchantingCard();
            type = type.replace("Card.AttachedBy", "Card.Self");
        }

        String valid = type;
        if (valid.contains("EQX")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(source, "X", sa)));
        }
        return CardLists.getValidCards(list, valid.split(","), sa.getActivatingPlayer(), source);
      }

    /**
     * <p>
     * passUnlessCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param usedStack
     *            a boolean.
     */
    public static void passUnlessCost(final SpellAbility sa, final boolean usedStack) {
        final Card source = sa.getSourceCard();
        final AbilityFactory af = sa.getAbilityFactory();
        if (af == null) {
            sa.resolve();
            AbilityFactory.resolveSubAbilities(sa, usedStack);
            return;
        }
        final HashMap<String, String> params = af.getMapParams();

        // Nothing to do
        if (params.get("UnlessCost") == null) {
            sa.resolve();
            AbilityFactory.resolveSubAbilities(sa, usedStack);
            return;
        }

        // The player who has the chance to cancel the ability
        final String pays = params.containsKey("UnlessPayer") ? params.get("UnlessPayer") : "TargetedController";
        final ArrayList<Player> payers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), pays, sa);

        // The cost
        String unlessCost = params.get("UnlessCost").trim();

        try {
            String unlessVar = Integer.toString(AbilityFactory.calculateAmount(source, params.get("UnlessCost").replace(" ", ""), sa));
            unlessCost = unlessVar;
        } catch (final NumberFormatException n) {
        } //This try/catch method enables UnlessCost to parse any svar name
          //instead of just X for cards like Draco. If there's a better way
          //feel free to change it. Old code follows:

        /*if (unlessCost.equals("X")) {
            unlessCost = Integer.toString(AbilityFactory.calculateAmount(source, params.get("UnlessCost"), sa));
        }*/
        final Cost cost = new Cost(source, unlessCost, true);

        final Ability ability = new AbilityStatic(source, cost, null) {

            @Override
            public void resolve() {
                // nothing to do here
            }
        };

        final Command paidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            @Override
            public void execute() {
                AbilityFactory.resolveSubAbilities(sa, usedStack);
            }
        };

        Command unpaidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            @Override
            public void execute() {
                sa.resolve();
                if (params.containsKey("PowerSink")) {
                    GameActionUtil.doPowerSink(sa.getActivatingPlayer());
                }
                AbilityFactory.resolveSubAbilities(sa, usedStack);
            }
        };
        boolean paid = false;
        for (Player payer : payers) {
            if (payer.isComputer()) {
                // AI will only pay when it's not already payed and only opponents abilities
                if (paid || (payers.size() > 1 && sa.getActivatingPlayer().equals(payer))) {
                    continue;
                }
                ability.setActivatingPlayer(payer);
                if (ComputerUtil.canPayCost(ability, payer) && CostUtil.checkLifeCost(payer, cost, source, 4, sa)
                        && CostUtil.checkDamageCost(payer, cost, source, 4)
                        && (!source.getName().equals("Tyrannize") || payer.getCardsIn(ZoneType.Hand).size() > 2)
                        && (!params.containsKey("UnlessAI") || !params.get("UnlessAI").equals("Never"))
                        && (!source.getName().equals("Breaking Point") || payer.getCreaturesInPlay().size() > 1)) {
                    // AI was crashing because the blank ability used to pay costs
                    // Didn't have any of the data on the original SA to pay dependant costs
                    ability.setTarget(sa.getTarget());
                    ComputerUtil.playNoStack(payer, ability); // Unless cost was payed - no resolve
                    paid = true;
                }
            }
        }
        boolean waitForInput = false;
        for (Player payer : payers) {
            if (payer.isHuman()) {
                // if it's paid by the AI already the human can pay, but it won't change anything
                if (paid) {
                    unpaidCommand = paidCommand;
                }
                GameActionUtil.payCostDuringAbilityResolve(ability, cost, paidCommand, unpaidCommand, sa);
                waitForInput = true; // wait for the human input
                break; // multiple human players are not supported
            }
        }
        if (!waitForInput) {
            if (paid) {
                AbilityFactory.resolveSubAbilities(sa, usedStack);
            } else {
                sa.resolve();
                if (params.containsKey("PowerSink")) {
                    GameActionUtil.doPowerSink(payers.get(0));
                }
                AbilityFactory.resolveSubAbilities(sa, usedStack);
            }
        }
        
    }

    /**
     * <p>
     * resolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param usedStack
     *            a boolean.
     */
    public static void resolve(final SpellAbility sa, final boolean usedStack) {
        if (sa == null) {
            return;
        }
        final AbilityFactory af = sa.getAbilityFactory();
        if (af == null) {
            sa.resolve();
            if (sa.getSubAbility() != null) {
                resolve(sa.getSubAbility(), usedStack);
            }
            return;
        }

        // check conditions
        if (AbilityFactory.checkConditional(sa)) {
            if (sa.isWrapper()) {
                sa.resolve();
                AbilityFactory.resolveSubAbilities(sa, usedStack);
            } else {
                AbilityFactory.passUnlessCost(sa, usedStack);
            }
        } else {
            AbilityFactory.resolveSubAbilities(sa, usedStack);
        }
    }

    /**
     * <p>
     * resolveSubAbilities.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static void resolveSubAbilities(final SpellAbility sa, boolean usedStack) {
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub == null || sa.isWrapper()) {
            // every resolving spellAbility will end here
            if (usedStack) {
                SpellAbility root = sa.getRootSpellAbility();
                Singletons.getModel().getGame().getStack().finishResolving(root, false);
            }
            return;
        }
        // check conditions
        if (AbilityFactory.checkConditional(abSub)) {
            AbilityFactory.passUnlessCost(abSub, usedStack);
        } else {
            AbilityFactory.resolveSubAbilities(abSub, usedStack);
        }
    }

} // end class AbilityFactory
