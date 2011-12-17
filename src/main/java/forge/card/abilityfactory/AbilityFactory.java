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
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityCondition;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;

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
     * getHostCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostC;
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

    private boolean hasSubAb = false;

    /**
     * <p>
     * hasSubAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSubAbility() {
        return this.hasSubAb;
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

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams(final String abString, final Card hostCard) {
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

        this.mapParams = this.getMapParams(abString, hostCard);

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
            this.abCost = new Cost(this.mapParams.get("Cost"), hostCard.getName(), this.isAb);
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
                this.abTgt.setZone(Zone.listValueOf(this.mapParams.get("TgtZone")));
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
        }

        this.hasSubAb = this.mapParams.containsKey("SubAbility");

        this.hasSpDesc = this.mapParams.containsKey("SpellDescription");

        // ***********************************
        // Match API keywords. These are listed in alphabetical order.

        if (this.api.equals("AddTurn")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryTurns.createAbilityAddTurn(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryTurns.createSpellAddTurn(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryTurns.createDrawbackAddTurn(this);
            }
        }

        else if (this.api.equals("Animate")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAnimate.createAbilityAnimate(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAnimate.createSpellAnimate(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAnimate.createDrawbackAnimate(this);
            }
        }

        else if (this.api.equals("AnimateAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAnimate.createAbilityAnimateAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAnimate.createSpellAnimateAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAnimate.createDrawbackAnimateAll(this);
            }
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
            spellAbility.setIsCharm(true);
            final int num = Integer.parseInt(this.mapParams.containsKey("CharmNum") ? this.mapParams.get("CharmNum")
                    : "1");
            spellAbility.setCharmNumber(num);
            final int min = this.mapParams.containsKey("MinCharmNum") ? Integer.parseInt(this.mapParams
                    .get("MinCharmNum")) : num;
            spellAbility.setMinCharmNumber(min);

            final String[] saChoices = this.mapParams.get("Choices").split(",");
            for (final String saChoice : saChoices) {
                final String ab = this.hostC.getSVar(saChoice);
                final AbilityFactory charmAF = new AbilityFactory();
                spellAbility.addCharmChoice(charmAF.getAbility(ab, this.hostC));
            }
        }

        else if (this.api.equals("ChooseCard")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityChooseCard(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellChooseCard(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackChooseCard(this);
            }
        }

        else if (this.api.equals("ChooseColor")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityChooseColor(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellChooseColor(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackChooseColor(this);
            }
        }

        else if (this.api.equals("ChooseNumber")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityChooseNumber(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellChooseNumber(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackChooseNumber(this);
            }
        }

        else if (this.api.equals("ChoosePlayer")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityChoosePlayer(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellChoosePlayer(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackChoosePlayer(this);
            }
        }

        else if (this.api.equals("ChooseType")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityChooseType(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellChooseType(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackChooseType(this);
            }
        }

        else if (this.api.equals("Clash")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryClash.getAbilityClash(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryClash.getSpellClash(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryClash.getDrawbackClash(this);
            }
        }

        else if (this.api.equals("Cleanup")) {
            if (this.isDb) {
                spellAbility = AbilityFactoryCleanup.getDrawback(this);
            }
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
                this.abTgt.setZone(Zone.Stack);
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
            final AbilityFactoryCounterMagic c = new AbilityFactoryCounterMagic(this);

            // Since all "Counter" ABs Counter things on the Stack no need for
            // it to be everywhere
            if (this.isTargeted) {
                this.abTgt.setZone(Zone.Stack);
            }

            if (this.isAb) {
                spellAbility = c.getAbilityCounter(this);
            } else if (this.isSp) {
                spellAbility = c.getSpellCounter(this);
            } else if (this.isDb) {
                spellAbility = c.getDrawbackCounter(this);
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
            if (this.isAb) {
                spellAbility = AbilityFactoryDebuff.createAbilityDebuff(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryDebuff.createSpellDebuff(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryDebuff.createDrawbackDebuff(this);
            }
        }

        else if (this.api.equals("DebuffAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryDebuff.createAbilityDebuffAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryDebuff.createSpellDebuffAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryDebuff.createDrawbackDebuffAll(this);
            }
        }

        else if (this.api.equals("DelayedTrigger")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryDelayedTrigger.getAbility(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryDelayedTrigger.getSpell(this);
            }
            if (this.isDb) {
                spellAbility = AbilityFactoryDelayedTrigger.getDrawback(this);
            }
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
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createAbilityDig(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createSpellDig(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createDrawbackDig(this);
            }
        }

        else if (this.api.equals("DigUntil")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createAbilityDigUntil(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createSpellDigUntil(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createDrawbackDigUntil(this);
            }
        }

        else if (this.api.equals("Discard")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryZoneAffecting.createAbilityDiscard(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryZoneAffecting.createSpellDiscard(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryZoneAffecting.createDrawbackDiscard(this);
            }
        }

        else if (this.api.equals("DrainMana")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryMana.createAbilityDrainMana(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryMana.createSpellDrainMana(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryMana.createDrawbackDrainMana(this);
            }
        }

        else if (this.api.equals("Draw")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryZoneAffecting.createAbilityDraw(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryZoneAffecting.createSpellDraw(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryZoneAffecting.createDrawbackDraw(this);
            }
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
            if (this.isAb) {
                spellAbility = AbilityFactoryEffect.createAbilityEffect(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryEffect.createSpellEffect(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryEffect.createDrawbackEffect(this);
            }
        }

        else if (this.api.equals("ExchangeLife")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAlterLife.createAbilityExchangeLife(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAlterLife.createSpellExchangeLife(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAlterLife.createDrawbackExchangeLife(this);
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
            if (this.isAb) {
                spellAbility = AbilityFactoryCombat.createAbilityFog(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCombat.createSpellFog(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCombat.createDrawbackFog(this);
            }
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
            if (this.isAb) {
                spellAbility = AbilityFactoryAlterLife.createAbilityGainLife(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAlterLife.createSpellGainLife(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAlterLife.createDrawbackGainLife(this);
            }
        }

        else if (this.api.equals("LoseLife")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAlterLife.createAbilityLoseLife(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAlterLife.createSpellLoseLife(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAlterLife.createDrawbackLoseLife(this);
            }
        }

        else if (this.api.equals("LosesGame")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryEndGameCondition.createAbilityLosesGame(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryEndGameCondition.createSpellLosesGame(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryEndGameCondition.createDrawbackLosesGame(this);
            }
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
            if (this.isAb) {
                spellAbility = AbilityFactoryZoneAffecting.createAbilityMill(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryZoneAffecting.createSpellMill(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryZoneAffecting.createDrawbackMill(this);
            }
        }

        else if (this.api.equals("MoveCounter")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityMoveCounters(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellMoveCounters(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackMoveCounters(this);
            }
        }

        else if (this.api.equals("MustAttack")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCombat.createAbilityMustAttack(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCombat.createSpellMustAttack(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCombat.createDrawbackMustAttack(this);
            }
        }

        else if (this.api.equals("MustBlock")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCombat.createAbilityMustBlock(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCombat.createSpellMustBlock(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCombat.createDrawbackMustBlock(this);
            }
        }

        else if (this.api.equals("NameCard")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryChoose.createAbilityNameCard(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryChoose.createSpellNameCard(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryChoose.createDrawbackNameCard(this);
            }
        }

        else if (this.api.equals("Phases")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityPhases(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellPhases(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackPhases(this);
            }
        }

        else if (this.api.equals("Poison")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAlterLife.createAbilityPoison(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAlterLife.createSpellPoison(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAlterLife.createDrawbackPoison(this);
            }
        }

        else if (this.api.equals("PreventDamage")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPreventDamage.createAbilityPreventDamage(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPreventDamage.createSpellPreventDamage(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPreventDamage.createDrawbackPreventDamage(this);
            }
        }

        else if (this.api.equals("PreventDamageAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPreventDamage.createAbilityPreventDamageAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPreventDamage.createSpellPreventDamageAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPreventDamage.createDrawbackPreventDamageAll(this);
            }
        }

        else if (this.api.equals("Proliferate")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityProliferate(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellProliferate(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackProliferate(this);
            }
        }

        else if (this.api.equals("Protection")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryProtection.createAbilityProtection(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryProtection.createSpellProtection(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryProtection.createDrawbackProtection(this);
            }
        }

        else if (this.api.equals("ProtectionAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryProtection.createAbilityProtectionAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryProtection.createSpellProtectionAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryProtection.createDrawbackProtectionAll(this);
            }
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

            if (this.isAb || this.isSp) {
                hostCard.setSVar("PlayMain1", "TRUE");
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

            if (this.isAb || this.isSp) {
                hostCard.setSVar("PlayMain1", "TRUE");
            }
        }

        else if (this.api.equals("PutCounter")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityPutCounters(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellPutCounters(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackPutCounters(this);
            }
        }

        else if (this.api.equals("PutCounterAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityPutCounterAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellPutCounterAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackPutCounterAll(this);
            }
        }

        else if (this.api.equals("RearrangeTopOfLibrary")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createRearrangeTopOfLibraryAbility(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createRearrangeTopOfLibrarySpell(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createRearrangeTopOfLibraryDrawback(this);
            }
        }

        else if (this.api.equals("Regenerate")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryRegenerate.getAbilityRegenerate(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryRegenerate.getSpellRegenerate(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryRegenerate.createDrawbackRegenerate(this);
            }
        }

        else if (this.api.equals("RegenerateAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryRegenerate.getAbilityRegenerateAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryRegenerate.getSpellRegenerateAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryRegenerate.createDrawbackRegenerateAll(this);
            }
        }

        else if (this.api.equals("RemoveCounter")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityRemoveCounters(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellRemoveCounters(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackRemoveCounters(this);
            }
        }

        else if (this.api.equals("RemoveCounterAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCounters.createAbilityRemoveCounterAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCounters.createSpellRemoveCounterAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCounters.createDrawbackRemoveCounterAll(this);
            }
        }

        else if (this.api.equals("RemoveFromCombat")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryCombat.createAbilityRemoveFromCombat(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryCombat.createSpellRemoveFromCombat(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryCombat.createDrawbackRemoveFromCombat(this);
            }
        }

        else if (this.api.equals("Reveal")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createAbilityReveal(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createSpellReveal(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createDrawbackReveal(this);
            }
        }

        else if (this.api.equals("RevealHand")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createAbilityRevealHand(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createSpellRevealHand(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createDrawbackRevealHand(this);
            }
        }

        else if (this.api.equals("Sacrifice")) {
            if (this.isAb) {
                spellAbility = AbilityFactorySacrifice.createAbilitySacrifice(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactorySacrifice.createSpellSacrifice(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactorySacrifice.createDrawbackSacrifice(this);
            }
        }

        else if (this.api.equals("SacrificeAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactorySacrifice.createAbilitySacrificeAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactorySacrifice.createSpellSacrificeAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactorySacrifice.createDrawbackSacrificeAll(this);
            }
        }

        else if (this.api.equals("Scry")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryReveal.createAbilityScry(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryReveal.createSpellScry(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryReveal.createDrawbackScry(this);
            }
        }

        else if (this.api.equals("SetLife")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryAlterLife.createAbilitySetLife(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryAlterLife.createSpellSetLife(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryAlterLife.createDrawbackSetLife(this);
            }
        }

        else if (this.api.equals("SetState")) {
            if (this.isAb) {
                spellAbility = AbilityFactorySetState.getSetStateAbility(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactorySetState.getSetStateSpell(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactorySetState.getSetStateDrawback(this);
            }
        }

        else if (this.api.equals("SetStateAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactorySetState.getSetStateAllAbility(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactorySetState.getSetStateAllSpell(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactorySetState.getSetStateAllDrawback(this);
            }
        }

        else if (this.api.equals("Shuffle")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryZoneAffecting.createAbilityShuffle(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryZoneAffecting.createSpellShuffle(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryZoneAffecting.createDrawbackShuffle(this);
            }
        }

        else if (this.api.equals("Tap")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityTap(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellTap(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackTap(this);
            }
        }

        else if (this.api.equals("TapAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityTapAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellTapAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackTapAll(this);
            }
        }

        else if (this.api.equals("TapOrUntap")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityTapOrUntap(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellTapOrUntap(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackTapOrUntap(this);
            }
        }

        else if (this.api.equals("Token")) {
            final AbilityFactoryToken aft = new AbilityFactoryToken(this);

            if (this.isAb) {
                spellAbility = aft.getAbility();
            } else if (this.isSp) {
                spellAbility = aft.getSpell();
            } else if (this.isDb) {
                spellAbility = aft.getDrawback();
            }
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

        else if (this.api.equals("Untap")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityUntap(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellUntap(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackUntap(this);
            }
        }

        else if (this.api.equals("UntapAll")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryPermanentState.createAbilityUntapAll(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryPermanentState.createSpellUntapAll(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryPermanentState.createDrawbackUntapAll(this);
            }
        }

        else if (this.api.equals("WinsGame")) {
            if (this.isAb) {
                spellAbility = AbilityFactoryEndGameCondition.createAbilityWinsGame(this);
            } else if (this.isSp) {
                spellAbility = AbilityFactoryEndGameCondition.createSpellWinsGame(this);
            } else if (this.isDb) {
                spellAbility = AbilityFactoryEndGameCondition.createDrawbackWinsGame(this);
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

        if (this.hasSubAbility()) {
            spellAbility.setSubAbility(this.getSubAbility());
        }

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

        // StackDescriptions are overwritten by the AF type instead of through
        // this
        // if (!isTargeted)
        // SA.setStackDescription(hostCard.getName());

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
    public final AbilitySub getSubAbility() {
        AbilitySub abSub = null;

        String sSub = this.getMapParams().get("SubAbility");

        if (sSub.startsWith("SVar=")) {
            sSub = sSub.replace("SVar=", "");
        }

        sSub = this.getHostCard().getSVar(sSub);

        if (!sSub.equals("")) {
            // Older style Drawback no longer supported
            final AbilityFactory afDB = new AbilityFactory();
            abSub = (AbilitySub) afDB.getAbility(sSub, this.getHostCard());
        } else {
            System.out.println("SubAbility not found for: " + this.getHostCard());
        }

        return abSub;
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
    public static boolean playReusable(final SpellAbility sa) {
        // TODO probably also consider if winter orb or similar are out

        if (sa.getPayCosts() == null) {
            return true; // This is only true for Drawbacks and triggers
        }

        if (!sa.getPayCosts().isReusuableResource()) {
            return false;
        }

        if (sa.getRestrictions().getPlaneswalker() && AllZone.getPhase().is(Constant.Phase.MAIN2)) {
            return true;
        }

        return (AllZone.getPhase().is(Constant.Phase.END_OF_TURN) && AllZone.getPhase().isNextTurn(
                AllZone.getComputerPlayer()));
    }

    // returns true if it's better to wait until blockers are declared
    /**
     * <p>
     * waitForBlocking.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean waitForBlocking(final SpellAbility sa) {

        return (sa.getSourceCard().isCreature() && sa.getPayCosts().getTap() && (AllZone.getPhase().isBefore(
                Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY) || AllZone.getPhase().isNextTurn(
                AllZone.getHumanPlayer())));
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
        }

        if (!card.getSVar(amount).equals("")) {
            final String[] calcX = card.getSVar(amount).split("\\$");
            if ((calcX.length == 1) || calcX[1].equals("none")) {
                return 0;
            }

            if (calcX[0].startsWith("Count")) {
                return CardFactoryUtil.xCount(card, calcX[1]) * multiplier;
            }

            if (calcX[0].startsWith("Number")) {
                return CardFactoryUtil.xCount(card, card.getSVar(amount)) * multiplier;
            } else if (calcX[0].startsWith("SVar")) {
                final String[] l = calcX[1].split("/");
                final String[] m = CardFactoryUtil.parseMath(l);
                return CardFactoryUtil.doXMath(AbilityFactory.calculateAmount(card, l[0], ability), m, card)
                        * multiplier;
            } else if (calcX[0].startsWith("Remembered")) {
                // Add whole Remembered list to handlePaid
                final CardList list = new CardList();
                if (card.getRemembered().isEmpty()) {
                    Card newCard = AllZoneUtil.getCardState(card);
                    for (final Object o : newCard.getRemembered()) {
                        if (o instanceof Card) {
                            list.add(AllZoneUtil.getCardState((Card) o));
                        }
                    }
                }

                for (final Object o : card.getRemembered()) {
                    if (o instanceof Card) {
                        list.add(AllZoneUtil.getCardState((Card) o));
                    }
                }

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
            } else if (calcX[0].startsWith("Imprinted")) {
                // Add whole Imprinted list to handlePaid
                final CardList list = new CardList();
                for (final Card c : card.getImprinted()) {
                    list.add(AllZoneUtil.getCardState(c));
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

                CardList list = new CardList();
                if (calcX[0].startsWith("Sacrificed")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Sacrificed");
                } else if (calcX[0].startsWith("Discarded")) {
                    list = AbilityFactory.findRootAbility(ability).getPaidList("Discarded");
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
                            list = new CardList();
                            final ArrayList<SpellAbility> sas = saTargeting.getTarget().getTargetSAs();
                            for (final SpellAbility sa : sas) {
                                list.add(sa.getSourceCard());
                            }
                        } else {
                            final SpellAbility saTargeting = AbilityFactory.findParentsTargetedCard(ability);
                            list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
                        }
                    } else {
                        final SpellAbility parent = AbilityFactory.findParentsTargetedCard(ability);

                        final ArrayList<Object> all = parent.getTarget().getTargets();
                        if (!all.isEmpty() && (all.get(0) instanceof SpellAbility)) {
                            list = new CardList();
                            final ArrayList<SpellAbility> sas = parent.getTarget().getTargetSAs();
                            for (final SpellAbility sa : sas) {
                                list.add(sa.getSourceCard());
                            }
                        } else {
                            final SpellAbility saTargeting = AbilityFactory.findParentsTargetedCard(ability);
                            list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
                        }
                    }
                } else if (calcX[0].startsWith("Triggered")) {
                    final SpellAbility root = ability.getRootSpellAbility();
                    list = new CardList();
                    list.add((Card) root.getTriggeringObject(calcX[0].substring(9)));
                } else if (calcX[0].startsWith("TriggerCount")) {
                    // TriggerCount is similar to a regular Count, but just
                    // pulls Integer Values from Trigger objects
                    final SpellAbility root = ability.getRootSpellAbility();
                    final String[] l = calcX[1].split("/");
                    final String[] m = CardFactoryUtil.parseMath(l);
                    final int count = (Integer) root.getTriggeringObject(l[0]);

                    return CardFactoryUtil.doXMath(count, m, card) * multiplier;
                } else {

                    return 0;
                }

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;

            } else {
                return 0;
            }
        }
        if (amount.equals("ChosenX")) {
            // isn't made yet
            return 0;
        }

        return Integer.parseInt(amount) * multiplier;
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
    public static ArrayList<Card> getDefinedCards(final Card hostCard, final String def, final SpellAbility sa) {
        final ArrayList<Card> cards = new ArrayList<Card>();
        final String defined = (def == null) ? "Self" : def; // default to Self

        Card c = null;

        if (defined.equals("Self")) {
            c = hostCard;
        }

        else if (defined.equals("Equipped")) {
            c = hostCard.getEquippingCard();
        }

        else if (defined.equals("Enchanted")) {
            c = hostCard.getEnchantingCard();
            if (c == null
                && AbilityFactory.findRootAbility(sa) != null
                && AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed") != null
                && !AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed").isEmpty()) {
                c = AbilityFactory.findRootAbility(sa).getPaidList("Sacrificed").get(0).getEnchantingCard();
            }
        }

        else if (defined.equals("TopOfLibrary")) {
            final CardList lib = hostCard.getController().getCardsIn(Constant.Zone.Library);
            if (lib.size() > 0) {
                c = lib.get(0);
            } else {
                // we don't want this to fall through and return the "Self"
                return new ArrayList<Card>();
            }
        }

        else if (defined.equals("Targeted")) {
            final SpellAbility parent = AbilityFactory.findParentsTargetedCard(sa);
            cards.addAll(parent.getTarget().getTargetCards());
        } else if (defined.startsWith("Triggered") && (sa != null)) {
            final SpellAbility root = sa.getRootSpellAbility();
            final Object crd = root.getTriggeringObject(defined.substring(9));
            if (crd instanceof Card) {
                c = AllZoneUtil.getCardState((Card) crd);
            } else if (crd instanceof CardList) {
                for (final Card cardItem : (CardList) crd) {
                    cards.add(cardItem);
                }
            }
        } else if (defined.equals("Remembered")) {
            if (hostCard.getRemembered().isEmpty()) {
                Card newCard = AllZoneUtil.getCardState(hostCard);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        cards.add(AllZoneUtil.getCardState((Card) o));
                    }
                }
            }

            for (final Object o : hostCard.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(AllZoneUtil.getCardState((Card) o));
                }
            }
        } else if (defined.equals("Clones")) {
            for (final Card clone : hostCard.getClones()) {
                cards.add(AllZoneUtil.getCardState(clone));
            }
        } else if (defined.equals("Imprinted")) {
            for (final Card imprint : hostCard.getImprinted()) {
                cards.add(AllZoneUtil.getCardState(imprint));
            }
        } else if (defined.startsWith("ThisTurnEntered")) {
            final String[] workingCopy = defined.split("_");
            Zone destination, origin;
            String validFilter;

            destination = Zone.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = Zone.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }
            for (final Card cl : CardUtil.getThisTurnEntered(destination, origin, validFilter, hostCard)) {
                cards.add(cl);
            }
        } else {
            CardList list = null;
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
            players.addAll(parent.getTarget().getTargetPlayers());
            /*
             * Target tgt = sa.getTarget(); SpellAbility parent = sa;
             * 
             * do {
             * 
             * // did not find any targets if (!(parent instanceof AbilitySub))
             * { return players; } parent = ((AbilitySub) parent).getParent();
             * tgt = parent.getTarget(); } while ((tgt == null) ||
             * (tgt.getTargetPlayers().size() == 0));
             * 
             * players.addAll(tgt.getTargetPlayers());
             */
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
                final Player p = s.getSourceCard().getController();
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
        } else if (defined.equals("EnchantedController")) {
            final Player p = card.getEnchantingCard().getController();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("EnchantedOwner")) {
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
            final Player p = AllZone.getCombat().getAttackingPlayer();
            if (!players.contains(p)) {
                players.add(p);
            }
        } else if (defined.equals("DefendingPlayer")) {
            final Player p = AllZone.getCombat().getDefendingPlayer();
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
            if (AllZone.getHumanPlayer().isValid(defined, sa.getActivatingPlayer(), sa.getSourceCard())) {
                players.add(AllZone.getHumanPlayer());
            }
            if (AllZone.getComputerPlayer().isValid(defined, sa.getActivatingPlayer(), sa.getSourceCard())) {
                players.add(AllZone.getComputerPlayer());
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
            sas.addAll(parent.getTarget().getTargetSAs());
        } else if (defined.startsWith("Triggered")) {
            final SpellAbility root = sa.getRootSpellAbility();

            final String triggeringType = defined.substring(9);
            final Object o = root.getTriggeringObject(triggeringType);
            if (o instanceof SpellAbility) {
                s = (SpellAbility) o;
            }
        } else if (defined.startsWith("Imprinted")) {
            for (final Card imp : card.getImprinted()) {
                sas.addAll(imp.getSpellAbilities());
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
            if (!(parent instanceof AbilitySub)) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while ((parent.getTarget() == null) || (parent.getTarget().getTargetCards().size() == 0));

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
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof AbilitySub)) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while ((parent.getTarget() == null) || (parent.getTarget().getTargetSAs().size() == 0));

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
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof AbilitySub)) {
                return parent;
            }
            parent = ((AbilitySub) parent).getParent();
        } while ((parent.getTarget() == null) || (parent.getTarget().getTargetPlayers().size() == 0));

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
    public static ArrayList<Object> predictThreatenedObjects(final AbilityFactory saviourAf) {
        final ArrayList<Object> objects = new ArrayList<Object>();
        if (AllZone.getStack().size() == 0) {
            return objects;
        }

        // check stack for something that will kill this
        final SpellAbility topStack = AllZone.getStack().peekAbility();
        objects.addAll(AbilityFactory.predictThreatenedObjects(saviourAf, topStack));

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
    public static ArrayList<Object> predictThreatenedObjects(final AbilityFactory saviourAf, final SpellAbility topStack) {
        ArrayList<Object> objects = new ArrayList<Object>();
        final ArrayList<Object> threatened = new ArrayList<Object>();
        String saviourApi = "";
        if (saviourAf != null) {
            saviourApi = saviourAf.getAPI();
        }

        if (topStack == null) {
            return objects;
        }

        final Card source = topStack.getSourceCard();
        final AbilityFactory topAf = topStack.getAbilityFactory();

        // Can only Predict things from AFs
        if (topAf != null) {
            final Target tgt = topStack.getTarget();

            if (tgt == null) {
                objects = AbilityFactory.getDefinedObjects(source, topAf.getMapParams().get("Defined"), topStack);
            } else {
                objects = tgt.getTargets();
            }

            // Determine if Defined Objects are "threatened" will be destroyed
            // due to this SA

            final String threatApi = topAf.getAPI();
            final HashMap<String, String> threatParams = topAf.getMapParams();

            // Lethal Damage => prevent damage/regeneration/bounce
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
                        if (saviourApi.equals("Regenerate") && !c.canBeShielded()) {
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
            // Destroy => regeneration/bounce
            else if ((threatApi.equals("Destroy") || threatApi.equals("DestroyAll"))
                    && ((saviourApi.equals("Regenerate") && !threatParams.containsKey("NoRegen")) || saviourApi
                            .equals("ChangeZone"))) {
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
            // Exiling => bounce
            else if ((threatApi.equals("ChangeZone") || threatApi.equals("ChangeZoneAll"))
                    && saviourApi.equals("ChangeZone") && threatParams.containsKey("Destination")
                    && threatParams.get("Destination").equals("Exile")) {
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
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

        threatened.addAll(AbilityFactory.predictThreatenedObjects(saviourAf, topStack.getSubAbility()));
        return threatened;
    }

    /**
     * <p>
     * handleRemembering.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public static void handleRemembering(final AbilityFactory af) {
        final HashMap<String, String> params = af.getMapParams();
        Card host;

        if (!params.containsKey("RememberTargets")) {
            return;
        }

        host = af.getHostCard();

        if (params.containsKey("ForgetOtherTargets")) {
            host.clearRemembered();
        }

        final Target tgt = af.getAbTgt();

        if (params.containsKey("RememberTargets")) {
            final ArrayList<Object> tgts = (tgt == null) ? new ArrayList<Object>() : tgt.getTargets();
            for (final Object o : tgts) {
                host.addRemembered(o);
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
    public static CardList filterListByType(final CardList list, String type, final SpellAbility sa) {
        if (type == null) {
            return list;
        }

        // Filter List Can send a different Source card in for things like
        // Mishra and Lobotomy

        Card source = sa.getSourceCard();
        if (type.contains("Triggered")) {
            final Object o = sa.getTriggeringObject("Card");

            // I won't the card attached to the Triggering object
            if (!(o instanceof Card)) {
                return new CardList();
            }

            source = (Card) (o);
            type = type.replace("Triggered", "Card");
        } else if (type.startsWith("Remembered")) {
            boolean hasRememberedCard = false;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    hasRememberedCard = true;
                    source = (Card) o;
                    type = type.replace("Remembered", "Card");
                    break;
                }
            }

            if (!hasRememberedCard) {
                return new CardList();
            }
        }

        return list.getValidCards(type.split(","), sa.getActivatingPlayer(), source);
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
        final HashMap<String, String> params = af.getMapParams();

        // Nothing to do
        if (params.get("UnlessCost") == null) {
            sa.resolve();
            return;
        }

        // The player who has the chance to cancel the ability
        final String pays = params.containsKey("UnlessPayer") ? params.get("UnlessPayer") : "TargetedController";
        final Player payer = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), pays, sa).get(0);

        // The cost
        String unlessCost = params.get("UnlessCost").trim();
        if (unlessCost.equals("X")) {
            unlessCost = Integer.toString(AbilityFactory.calculateAmount(source, params.get("UnlessCost"), sa));
        }

        final Ability ability = new Ability(source, unlessCost) {
            @Override
            public void resolve() {
                // nothing to do
            }
        };

        final Command paidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            @Override
            public void execute() {
                AbilityFactory.resolveSubAbilities(sa);
                if (usedStack) {
                    AllZone.getStack().finishResolving(sa, false);
                }
            }
        };

        final Command unpaidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            @Override
            public void execute() {
                sa.resolve();
                if (params.containsKey("PowerSink")) {
                    GameActionUtil.doPowerSink(AllZone.getHumanPlayer());
                }
                AbilityFactory.resolveSubAbilities(sa);
                if (usedStack) {
                    AllZone.getStack().finishResolving(sa, false);
                }
            }
        };

        if (payer.isHuman()) {
            GameActionUtil.payManaDuringAbilityResolve(source + "\r\n", ability.getManaCost(), paidCommand,
                    unpaidCommand);
        } else {
            if (ComputerUtil.canPayCost(ability)) {
                ComputerUtil.playNoStack(ability); // Unless cost was payed - no
                                                   // resolve
                AbilityFactory.resolveSubAbilities(sa);
                if (usedStack) {
                    AllZone.getStack().finishResolving(sa, false);
                }
            } else {
                sa.resolve();
                if (params.containsKey("PowerSink")) {
                    GameActionUtil.doPowerSink(AllZone.getComputerPlayer());
                }
                AbilityFactory.resolveSubAbilities(sa);
                if (usedStack) {
                    AllZone.getStack().finishResolving(sa, false);
                }
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
            return;
        }
        final HashMap<String, String> params = af.getMapParams();

        // check conditions
        if (AbilityFactory.checkConditional(sa)) {
            if ((params.get("UnlessCost") == null) || sa.isWrapper()) {
                sa.resolve();

                // try to resolve subabilities (see null check above)
                AbilityFactory.resolveSubAbilities(sa);
                if (usedStack) {
                    AllZone.getStack().finishResolving(sa, false);
                }
            } else {
                AbilityFactory.passUnlessCost(sa, usedStack);
            }
        } else {
            AbilityFactory.resolveSubAbilities(sa);
            if (usedStack) {
                AllZone.getStack().finishResolving(sa, false);
            }
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
    public static void resolveSubAbilities(final SpellAbility sa) {
        final AbilitySub abSub = sa.getSubAbility();
        if ((abSub == null) || sa.isWrapper()) {
            return;
        }
        // check conditions
        if (AbilityFactory.checkConditional(abSub)) {
            abSub.resolve();
        }
        AbilityFactory.resolveSubAbilities(abSub);
    }

} // end class AbilityFactory
