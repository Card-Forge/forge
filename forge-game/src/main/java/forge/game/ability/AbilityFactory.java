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
package forge.game.ability;

import forge.card.CardStateName;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.util.FileSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        StaticAbility("ST"),
        SubAbility("DB");
        
        private final String prefix;
        private AbilityRecordType(String prefix) {
            this.prefix = prefix;
        }
        public String getPrefix() {
            return prefix;
        }
        
        public SpellAbility buildSpellAbility(ApiType api, Card hostCard, Cost abCost, TargetRestrictions abTgt, Map<String, String> mapParams ) {
            switch(this) {
                case Ability: return new AbilityApiBased(api, hostCard, abCost, abTgt, mapParams);
                case Spell: return new SpellApiBased(api, hostCard, abCost, abTgt, mapParams);
                case StaticAbility: return new StaticAbilityApiBased(api, hostCard, abCost, abTgt, mapParams);
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
            } else if (abParams.containsKey(AbilityRecordType.StaticAbility.getPrefix())) {
                return AbilityRecordType.StaticAbility;
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
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
        if (null == type) {
            String source = hostCard.getName().isEmpty() ? abString : hostCard.getName();
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + source + ": " + abString);
        }
        return getAbility(mapParams, type, hostCard);
    }
    
    public static final SpellAbility getAbility(final Card hostCard, final String svar) {
        if (!hostCard.hasSVar(svar)) {
            String source = hostCard.getName();
            throw new RuntimeException("AbilityFactory : getAbility -- " + source +  " has no SVar: " + svar);
        } else {
            return getAbility(hostCard.getSVar(svar), hostCard);
        }
    }
    
    public static final SpellAbility getAbility(final Map<String, String> mapParams, AbilityRecordType type, final Card hostCard) {
        return getAbility(type, type.getApiTypeOf(mapParams), mapParams, parseAbilityCost(hostCard, mapParams, type), hostCard);
    }


    public static Cost parseAbilityCost(final Card hostCard, Map<String, String> mapParams, AbilityRecordType type) {
        Cost abCost = null;
        if (type != AbilityRecordType.SubAbility) {
            String cost = mapParams.get("Cost");
            if (cost == null) {
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
            }
            abCost = new Cost(cost, type == AbilityRecordType.Ability);
        }
        return abCost;
    }

    public static final SpellAbility getAbility(AbilityRecordType type, ApiType api, Map<String, String> mapParams, Cost abCost, Card hostCard) {
        TargetRestrictions abTgt = mapParams.containsKey("ValidTgts") ? readTarget(mapParams) : null;

        if (api == ApiType.CopySpellAbility || api == ApiType.Counter || api == ApiType.ChangeTargets || api == ApiType.ControlSpell) {
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
            if (type == AbilityRecordType.Spell && !mapParams.containsKey("SubAbility")) {
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

        if (api == ApiType.DelayedTrigger && mapParams.containsKey("Execute")) {
            spellAbility.setSVar(mapParams.get("Execute"), hostCard.getSVar(mapParams.get("Execute")));
        }

        if (api == ApiType.RepeatEach) {
            spellAbility.setSVar(mapParams.get("RepeatSubAbility"), hostCard.getSVar(mapParams.get("RepeatSubAbility")));
        }

        if (mapParams.containsKey("PreventionSubAbility")) {
            spellAbility.setSVar(mapParams.get("PreventionSubAbility"), hostCard.getSVar(mapParams.get("PreventionSubAbility")));
        }

        if (mapParams.containsKey("SubAbility")) {
            spellAbility.setSubAbility(getSubAbility(hostCard, mapParams.get("SubAbility")));
        }

        if (spellAbility instanceof SpellApiBased && hostCard.isPermanent()) {
            spellAbility.setDescription(spellAbility.getHostCard().getName());
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
        } else if (api == ApiType.Charm) {
        	spellAbility.setDescription(CharmEffect.makeSpellDescription(spellAbility));
        } else {
            spellAbility.setDescription("");
        }
        
        initializeParams(spellAbility, mapParams);
        makeRestrictions(spellAbility, mapParams);
        makeConditions(spellAbility, mapParams);

        return spellAbility;
    }

    private static final TargetRestrictions readTarget(Map<String, String> mapParams) {
        final String min = mapParams.containsKey("TargetMin") ? mapParams.get("TargetMin") : "1";
        final String max = mapParams.containsKey("TargetMax") ? mapParams.get("TargetMax") : "1";


        // TgtPrompt now optional
        final String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");

        TargetRestrictions abTgt = new TargetRestrictions(prompt, mapParams.get("ValidTgts").split(","), min, max);

        if (mapParams.containsKey("TgtZone")) { // if Targeting
                                                     // something
            // not in play, this Key
            // should be set
            abTgt.setZone(ZoneType.listValueOf(mapParams.get("TgtZone")));
        }

        if (mapParams.containsKey("MaxTotalTargetCMC")) {
            // only target cards up to a certain total max CMC
            abTgt.setMaxTotalCMC(mapParams.get("MaxTotalTargetCMC"));
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
        if (mapParams.containsKey("TargetsWithoutSameCreatureType")) {
            abTgt.setWithoutSameCreatureType(true);
        }
        if (mapParams.containsKey("TargetsWithSameController")) {
            abTgt.setSameController(true);
        }
        if (mapParams.containsKey("TargetsWithDifferentControllers")) {
            abTgt.setDifferentControllers(true);
        }
        if (mapParams.containsKey("DividedAsYouChoose")) {
            abTgt.calculateStillToDivide(mapParams.get("DividedAsYouChoose"), null, null);
            abTgt.setDividedAsYouChoose(true);
        }
        if (mapParams.containsKey("TargetsAtRandom")) {
            abTgt.setRandomTarget(true);
        }
        if (mapParams.containsKey("TargetingPlayer")) {
            abTgt.setMandatory(true);
        }
        return abTgt;
    }

    /**
     * <p>
     * initializeParams.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void initializeParams(final SpellAbility sa, Map<String, String> mapParams) {
        if (mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }

        if (mapParams.containsKey("NonBasicSpell")) {
            sa.setBasicSpell(false);
        }

        if (mapParams.containsKey("Outlast")) {
            sa.setOutlast(true);
        }
    }

    /**
     * <p>
     * makeRestrictions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void makeRestrictions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityRestriction restrict = sa.getRestrictions();
        restrict.setRestrictions(mapParams);
    }

    /**
     * <p>
     * makeConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void makeConditions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityCondition condition = sa.getConditions();
        condition.setConditions(mapParams);
    }

    // Easy creation of SubAbilities
    /**
     * <p>
     * getSubAbility.
     * </p>
     * @param sSub
     * 
     * @return a {@link forge.game.spellability.AbilitySub} object.
     */
    private static final AbilitySub getSubAbility(Card hostCard, String sSub) {

        if (hostCard.hasSVar(sSub)) {
            return (AbilitySub) AbilityFactory.getAbility(hostCard, sSub);
        }
        System.out.println("SubAbility '"+ sSub +"' not found for: " + hostCard);

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
    
        final TargetRestrictions tgt = sa.getTargetRestrictions();
    
        // Don't set the zone if it targets a player
        if ((tgt != null) && !tgt.canTgtPlayer()) {
            sa.getTargetRestrictions().setZone(origin);
        }
    }

    public static final SpellAbility buildFusedAbility(final Card card) {
        if(!card.isSplitCard()) 
            throw new IllegalStateException("Fuse ability may be built only on split cards");
        
        final String strLeftAbility = card.getState(CardStateName.LeftSplit).getFirstUnparsedAbility();
        Map<String, String> leftMap = getMapParams(strLeftAbility);
        AbilityRecordType leftType = AbilityRecordType.getRecordType(leftMap);
        ApiType leftApi = leftType.getApiTypeOf(leftMap);
        leftMap.put("StackDecription", leftMap.get("SpellDescription"));
        leftMap.put("SpellDescription", "Fuse (you may cast both halves of this card from your hand).");
        leftMap.put("ActivationZone", "Hand");
    
        final String strRightAbility = card.getState(CardStateName.RightSplit).getFirstUnparsedAbility();
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

    public static final SpellAbility buildEntwineAbility(final SpellAbility sa) {
    	final Card source = sa.getHostCard();
        final String[] saChoices = sa.getParam("Choices").split(",");
        if (sa.getApi() != ApiType.Charm || saChoices.length != 2) 
            throw new IllegalStateException("Entwine ability may be built only on charm cards");
        final String ab = source.getSVar(saChoices[0]);
    	Map<String, String> firstMap = getMapParams(ab);
        AbilityRecordType firstType = AbilityRecordType.getRecordType(firstMap);
        ApiType firstApi = firstType.getApiTypeOf(firstMap);
        firstMap.put("StackDecription", firstMap.get("SpellDescription"));
        firstMap.put("SpellDescription", sa.getDescription() + " Entwine (Choose both if you pay the entwine cost.)");
        SpellAbility entwineSA = getAbility(AbilityRecordType.Spell, firstApi, firstMap, new Cost(sa.getPayCosts().toSimpleString(), false), source);

        final String ab2 = source.getSVar(saChoices[1]);
    	Map<String, String> secondMap = getMapParams(ab2);
        ApiType secondApi = firstType.getApiTypeOf(secondMap);
        secondMap.put("StackDecription", secondMap.get("SpellDescription"));
        secondMap.put("SpellDescription", "");
        AbilitySub sub =  (AbilitySub) getAbility(AbilityRecordType.SubAbility, secondApi, secondMap, null, source);
        entwineSA.appendSubAbility(sub);

        entwineSA.setBasicSpell(false);
        entwineSA.setActivatingPlayer(sa.getActivatingPlayer());
        entwineSA.setRestrictions(sa.getRestrictions());
		return entwineSA;
    }

} // end class AbilityFactory
