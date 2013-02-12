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

import java.util.HashMap;
import java.util.Map;


import forge.Card;

import forge.card.cost.Cost;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityCondition;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
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

    /**
     * <p>
     * Constructor for AbilityFactory.
     * </p>
     */
    public AbilityFactory() {
    }

    // *******************************************************

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

        boolean isAb = false;
        boolean isSp = false;
        boolean isDb = false;

        ApiType api = null;

        Card hostC = hostCard;
        Map<String, String> mapParams = new HashMap<String, String>();


        try {
            mapParams = AbilityUtils.getMapParams(abString);
        }
        catch (RuntimeException ex) {

            throw new RuntimeException(hostCard.getName() + ": " + ex.getMessage());
        }

        // parse universal parameters

        if (mapParams.containsKey("AB")) {
            isAb = true;
            api = ApiType.smartValueOf(mapParams.get("AB"));
        } else if (mapParams.containsKey("SP")) {
            isSp = true;
            api = ApiType.smartValueOf(mapParams.get("SP"));
        } else if (mapParams.containsKey("DB")) {
            isDb = true;
            api = ApiType.smartValueOf(mapParams.get("DB"));
        } else {
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());
        }

        Cost abCost = null;
        if (!isDb) {
            if (!mapParams.containsKey("Cost")) {
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
            }
            abCost = new Cost(hostCard, mapParams.get("Cost"), isAb);

        }

        Target abTgt = mapParams.containsKey("ValidTgts") ? readTarget(hostC, mapParams) : null;

        // ***********************************
        // Match API keywords. These are listed in alphabetical order.


        if (api == ApiType.CopySpellAbility) {
            if (abTgt != null) {
                // Since all "CopySpell" ABs copy things on the Stack no need for it to be everywhere
                abTgt.setZone(ZoneType.Stack);
            }

            hostCard.setCopiesSpells(true);
        }

        else if (api == ApiType.Counter) {
            // Since all "Counter" ABs Counter things on the Stack no need for
            // it to be everywhere
            if (abTgt != null) {
                abTgt.setZone(ZoneType.Stack);
            }
        }

        else if (api == ApiType.PermanentCreature || api == ApiType.PermanentNoncreature) {
            // If API is a permanent type, and creating AF Spell
            // Clear out the auto created SpellPemanent spell
            if (isSp) {
                hostCard.clearFirstSpell();
            }
        }

        SpellAiLogic ai = api.getAi();
        SpellEffect se = api.getSpellEffect();

        if (isAb) {
            spellAbility = new CommonAbility(api, hostCard, abCost, abTgt, mapParams, se, ai);
        } else if (isSp) {
            spellAbility = new CommonSpell(api, hostCard, abCost, abTgt, mapParams, se, ai);
        } else if (isDb) {
            spellAbility = new CommonDrawback(api, hostCard, abTgt, mapParams, se, ai);
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
            msg.append(". Looking for API: ").append(api);
            throw new RuntimeException(msg.toString());
        }

        // *********************************************
        // set universal properties of the SpellAbility



        if (mapParams.containsKey("References")) {
            for (String svar : mapParams.get("References").split(",")) {
                spellAbility.setSVar(svar, hostC.getSVar(svar));
            }
        }

        if (mapParams.containsKey("SubAbility")) {
            spellAbility.setSubAbility(getSubAbility(hostCard, hostCard.getSVar(mapParams.get("SubAbility"))));
        }

        if (spellAbility instanceof CommonSpell && hostCard.isPermanent()) {
            spellAbility.setDescription(spellAbility.getSourceCard().getName());
        } else if (mapParams.containsKey("SpellDescription")) {
            final StringBuilder sb = new StringBuilder();

            if (!isDb) { // SubAbilities don't have Costs or Cost
                              // descriptors
                if (mapParams.containsKey("PrecostDesc")) {
                    sb.append(mapParams.get("PrecostDesc")).append(" ");
                }
                if (mapParams.containsKey("CostDesc")) {
                    sb.append(mapParams.get("CostDesc")).append(" ");
                } else {
                    sb.append(abCost.toString());
                }
            }

            sb.append(mapParams.get("SpellDescription"));

            spellAbility.setDescription(sb.toString());
        } else {
            spellAbility.setDescription("");
        }

        if (mapParams.containsKey("NonBasicSpell")) {
            spellAbility.setBasicSpell(false);
        }

        makeRestrictions(spellAbility, mapParams);
        makeConditions(spellAbility, mapParams);

        return spellAbility;
    }

    private Target readTarget(Card hostC, Map<String, String> mapParams) {
        final String min = mapParams.containsKey("TargetMin") ? mapParams.get("TargetMin") : "1";
        final String max = mapParams.containsKey("TargetMax") ? mapParams.get("TargetMax") : "1";


        // TgtPrompt now optional
        final StringBuilder sb = new StringBuilder();
        if (hostC != null) {
            sb.append(hostC + " - ");
        }
        final String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");
        sb.append(prompt);
        
        Target abTgt = new Target(hostC, sb.toString(), mapParams.get("ValidTgts").split(","), min, max);
        
        if (mapParams.containsKey("TgtZone")) { // if Targeting
                                                     // something
            // not in play, this Key
            // should be set
            abTgt.setZone(ZoneType.listValueOf(mapParams.get("TgtZone")));
        }

        // Target Type mostly for Counter: Spell,Activated,Triggered,Ability
        // (or any combination of)
        // Ability = both activated and triggered abilities
        if (mapParams.containsKey("TargetType")) {
            abTgt.setTargetSpellAbilityType(mapParams.get("TargetType"));
        }

        // TargetValidTargeting most for Counter: e.g. target spell that
        // targets X.
        if (mapParams.containsKey("TargetValidTargeting")) {
            abTgt.setSAValidTargeting(mapParams.get("TargetValidTargeting"));
        }

        if (mapParams.containsKey("TargetUnique")) {
            abTgt.setUniqueTargets(true);
        }
        if (mapParams.containsKey("TargetsFromSingleZone")) {
            abTgt.setSingleZone(true);
        }
        if (mapParams.containsKey("TargetsFromDifferentZone")) {
            abTgt.setDifferentZone(true);
        }
        if (mapParams.containsKey("TargetsWithoutSameCreatureType")) {
            abTgt.setWithoutSameCreatureType(true);
        }
        if (mapParams.containsKey("TargetsWithDefinedController")) {
            abTgt.setDefinedController(mapParams.get("TargetsWithDefinedController"));
        }
        if (mapParams.containsKey("TargetsWithDifferentControllers")) {
            abTgt.setDifferentControllers(true);
        }
        return abTgt;
    }

    /**
     * <p>
     * makeRestrictions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mapParams
     */
    private void makeRestrictions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityRestriction restrict = sa.getRestrictions();
        if (mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        restrict.setRestrictions(mapParams);
    }

    /**
     * <p>
     * makeConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mapParams
     */
    private void makeConditions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityCondition condition = sa.getConditions();
        if (mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        condition.setConditions(mapParams);
    }

    // Easy creation of SubAbilities
    /**
     * <p>
     * getSubAbility.
     * </p>
     * @param sSub
     * 
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    private final AbilitySub getSubAbility(Card hostCard, String sSub) {

        if (!sSub.equals("")) {
            final AbilityFactory afDB = new AbilityFactory();
            return (AbilitySub) afDB.getAbility(sSub, hostCard);
        }
        System.out.println("SubAbility not found for: " + hostCard);

        return null;
    }

} // end class AbilityFactory
