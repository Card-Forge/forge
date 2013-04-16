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
package forge.card.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardCharacteristicName;
import forge.card.cost.Cost;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityCondition;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.zone.ZoneType;
import forge.util.FileSection;

/**
 * <p>
 * AbilityFactory class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory {

    public enum AbilityRecordType {
        Ability("AB"),
        Spell("SP"),
        SubAbility("DB");
        
        private final String prefix;
        private AbilityRecordType(String prefix) {
            this.prefix = prefix;
        }
        public String getPrefix() {
            return prefix;
        }
        
        public SpellAbility buildSpellAbility(ApiType api, Card hostCard, Cost abCost, Target abTgt, Map<String, String> mapParams ) {
            switch(this) {
                case Ability: return new AbilityApiBased(api, hostCard, abCost, abTgt, mapParams);
                case Spell: return new SpellApiBased(api, hostCard, abCost, abTgt, mapParams);
                case SubAbility: return new AbilitySub(api, hostCard, abTgt, mapParams);
            }
            return null; // exception here would be fine!
        }
        
        public ApiType getApiTypeOf(Map<String, String> abParams) {
            return ApiType.smartValueOf(abParams.get(this.getPrefix()));
        }
        
        public static AbilityRecordType getRecordType(Map<String, String> abParams) {
            if (abParams.containsKey(AbilityRecordType.Ability.getPrefix())) {
                return AbilityRecordType.Ability;
            } else if (abParams.containsKey(AbilityRecordType.Spell.getPrefix())) {
                return AbilityRecordType.Spell;
            } else if (abParams.containsKey(AbilityRecordType.SubAbility.getPrefix())) {
                return AbilityRecordType.SubAbility;
            } else {
                return null;
            }
        }
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
    public static final SpellAbility getAbility(final String abString, final Card hostCard) {
        

        Map<String, String> mapParams;
        try {
            mapParams = AbilityFactory.getMapParams(abString);
        }
        catch (RuntimeException ex) {
            throw new RuntimeException(hostCard.getName() + ": " + ex.getMessage());
        }

        // parse universal parameters
        AbilityRecordType type = AbilityRecordType.getRecordType(mapParams);
        if( null == type )
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());
        
        return getAbility(type, type.getApiTypeOf(mapParams), mapParams, parseAbilityCost(hostCard, mapParams, type), hostCard);
    }

    public static Cost parseAbilityCost(final Card hostCard, Map<String, String> mapParams, AbilityRecordType type) {
        Cost abCost = null;
        if (type != AbilityRecordType.SubAbility) {
            if (!mapParams.containsKey("Cost")) {
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
            }
            abCost = new Cost(mapParams.get("Cost"), type == AbilityRecordType.Ability);
        }
        return abCost;
    }

    private static final SpellAbility getAbility(AbilityRecordType type, ApiType api, Map<String, String> mapParams, Cost abCost, Card hostCard) {
        
        
        Target abTgt = mapParams.containsKey("ValidTgts") ? readTarget(hostCard, mapParams) : null;

        if (api == ApiType.CopySpellAbility || api == ApiType.Counter || api == ApiType.ChangeTargets) {
            // Since all "CopySpell" ABs copy things on the Stack no need for it to be everywhere
            // Since all "Counter" or "ChangeTargets" abilities only target the Stack Zone
            // No need to have each of those scripts have that info
            if (abTgt != null) {
                abTgt.setZone(ZoneType.Stack);
            }
        }

        else if (api == ApiType.PermanentCreature || api == ApiType.PermanentNoncreature) {
            // If API is a permanent type, and creating AF Spell
            // Clear out the auto created SpellPemanent spell
            if (type == AbilityRecordType.Spell) {
                hostCard.clearFirstSpell();
            }
        }


        SpellAbility spellAbility = type.buildSpellAbility(api, hostCard, abCost, abTgt, mapParams);


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
                spellAbility.setSVar(svar, hostCard.getSVar(svar));
            }
        }

        if (mapParams.containsKey("SubAbility")) {
            spellAbility.setSubAbility(getSubAbility(hostCard, hostCard.getSVar(mapParams.get("SubAbility"))));
        }

        if (spellAbility instanceof SpellApiBased && hostCard.isPermanent()) {
            spellAbility.setDescription(spellAbility.getSourceCard().getName());
        } else if (mapParams.containsKey("SpellDescription")) {
            final StringBuilder sb = new StringBuilder();

            if (type != AbilityRecordType.SubAbility) { // SubAbilities don't have Costs or Cost
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

    private static final Target readTarget(Card hostC, Map<String, String> mapParams) {
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

        if (mapParams.containsKey("TargetsSingleTarget")) {
            abTgt.setSingleTarget(true);
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
        if (mapParams.containsKey("TargetsWithSameController")) {
            abTgt.setSameController(true);
        }
        if (mapParams.containsKey("TargetsWithDifferentControllers")) {
            abTgt.setDifferentControllers(true);
        }
        if (mapParams.containsKey("DividedAsYouChoose")) {
            abTgt.calculateStillToDivide(mapParams.get("DividedAsYouChoose"), hostC, null);
            abTgt.setDividedAsYouChoose(true);
        }
        if (mapParams.containsKey("TargetsAtRandom")) {
            abTgt.setRandomTarget(true);
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
    private static final void makeRestrictions(final SpellAbility sa, Map<String, String> mapParams) {
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
    private static final void makeConditions(final SpellAbility sa, Map<String, String> mapParams) {
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
    private static final AbilitySub getSubAbility(Card hostCard, String sSub) {

        if (!sSub.equals("")) {
            return (AbilitySub) AbilityFactory.getAbility(sSub, hostCard);
        }
        System.out.println("SubAbility not found for: " + hostCard);

        return null;
    }

    public static final Map<String, String> getMapParams(final String abString) {
        return FileSection.parseToMap(abString, "$", "|");
    }

    public static final void adjustChangeZoneTarget(final Map<String, String> params, final SpellAbility sa) {
        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }
    
        final Target tgt = sa.getTarget();
    
        // Don't set the zone if it targets a player
        if ((tgt != null) && !tgt.canTgtPlayer()) {
            sa.getTarget().setZone(origin);
        }
    }

    public static final SpellAbility buildFusedAbility(final Card card) {
        if(!card.isSplitCard()) 
            throw new IllegalStateException("Fuse ability may be built only on split cards");
        
        final String strLeftAbility = card.getState(CardCharacteristicName.LeftSplit).getIntrinsicAbility().get(0);
        Map<String, String> leftMap = getMapParams(strLeftAbility);
        AbilityRecordType leftType = AbilityRecordType.getRecordType(leftMap);
        ApiType leftApi = leftType.getApiTypeOf(leftMap);
        leftMap.put("StackDecription", leftMap.get("SpellDescription"));
        leftMap.put("SpellDescription", "Fuse (you may cast both halves of this card from your hand).");
        leftMap.put("ActivationZone", "Hand");
    
        final String strRightAbility = card.getState(CardCharacteristicName.RightSplit).getIntrinsicAbility().get(0);
        Map<String, String> rightMap = getMapParams(strRightAbility);
        AbilityRecordType rightType = AbilityRecordType.getRecordType(leftMap);
        ApiType rightApi = leftType.getApiTypeOf(rightMap);
        rightMap.put("StackDecription", rightMap.get("SpellDescription"));
        rightMap.put("SpellDescription", "");

        Cost totalCost = parseAbilityCost(card, leftMap, leftType);
        totalCost.add(parseAbilityCost(card, rightMap, rightType));

        final SpellAbility left = getAbility(leftType, leftApi, leftMap, totalCost, card);
        final AbilitySub right = (AbilitySub) getAbility(AbilityRecordType.SubAbility, rightApi, rightMap, null, card);
        left.appendSubAbility(right);
        return left;
    }

} // end class AbilityFactory
