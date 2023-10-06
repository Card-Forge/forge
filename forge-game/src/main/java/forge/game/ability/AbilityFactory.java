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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.CardTraitBase;
import forge.game.IHasSVars;
import forge.game.ability.effects.CharmEffect;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.cost.Cost;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

/**
 * <p>
 * AbilityFactory class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory {

    public static final List<String> additionalAbilityKeys = Lists.newArrayList(
            "WinSubAbility", "OtherwiseSubAbility", // Clash
            "BidSubAbility", // BidLifeEffect
            "ChooseNumberSubAbility", "Lowest", "Highest", "NotLowest", // ChooseNumber
            "HeadsSubAbility", "TailsSubAbility", "LoseSubAbility", // FlipCoin
            "TrueSubAbility", "FalseSubAbility", // Branch
            "ChosenPile", "UnchosenPile", // MultiplePiles & TwoPiles
            "RepeatSubAbility", // Repeat & RepeatEach
            "Execute", // DelayedTrigger
            "FallbackAbility", // Complex Unless costs which can be unpayable
            "ChooseSubAbility", // Can choose a player via ChoosePlayer
            "CantChooseSubAbility", // Can't choose a player via ChoosePlayer
            "AnimateSubAbility", // For ChangeZone Effects to Animate before ETB
            "RegenerationAbility", // for Regeneration Effect
            "ReturnAbility" // for Delayed Trigger on Magpie
        );

    public enum AbilityRecordType {
        Ability("AB"),
        Spell("SP"),
        StaticAbility("ST"),
        SubAbility("DB");

        private final String prefix;
        AbilityRecordType(String prefix) {
            this.prefix = prefix;
        }
        public String getPrefix() {
            return prefix;
        }

        public SpellAbility buildSpellAbility(ApiType api, Card hostCard, Cost abCost, TargetRestrictions abTgt, Map<String, String> mapParams) {
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

    public static final SpellAbility getAbility(final String abString, final Card card) {
        return getAbility(abString, card.getCurrentState());
    }
    public static final SpellAbility getAbility(final String abString, final Card card, final IHasSVars sVarHolder) {
        return getAbility(abString, card.getCurrentState(), sVarHolder);
    }
    /**
     * <p>
     * getAbility.
     * </p>
     *
     * @param abString
     *            a {@link java.lang.String} object.
     * @param state
     *            a {@link forge.game.card.CardState} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final SpellAbility getAbility(final String abString, final CardState state) {
        return getAbility(abString, state, state);
    }

    private static final SpellAbility getAbility(final String abString, final CardState state, final IHasSVars sVarHolder) {
        Map<String, String> mapParams;
        try {
            mapParams = AbilityFactory.getMapParams(abString);
        }
        catch (RuntimeException ex) {
            throw new RuntimeException(state.getName() + ": " + ex.getMessage());
        }
        // parse universal parameters
        AbilityRecordType type = AbilityRecordType.getRecordType(mapParams);
        if (null == type) {
            String source = state.getName().isEmpty() ? abString : state.getName();
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + source + ": " + abString);
        }
        try {
            return getAbility(mapParams, type, state, sVarHolder);
        } catch (Error | Exception ex) {
            String msg = "AbilityFactory:getAbility: crash when trying to create ability ";
            
            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", state.getName());
            bread.setData("Ability", abString);
            
            Sentry.addBreadcrumb(bread);
            throw new RuntimeException(msg + " of card: " + state.getName(), ex);
        }
    }

    public static final SpellAbility getAbility(final Card hostCard, final String svar) {
        return getAbility(hostCard, svar, hostCard.getCurrentState());
    }

    public static final SpellAbility getAbility(final Card hostCard, final String svar, final IHasSVars sVarHolder) {
        return getAbility(hostCard.getCurrentState(), svar, sVarHolder);
    }

    public static final SpellAbility getAbility(final CardState state, final String svar, final IHasSVars sVarHolder) {
        if (!sVarHolder.hasSVar(svar)) {
            String source = state.getCard().getName();
            throw new RuntimeException("AbilityFactory : getAbility -- " + source +  " has no SVar: " + svar);
        } else {
            return getAbility(sVarHolder.getSVar(svar), state, sVarHolder);
        }
    }

    public static final SpellAbility getAbility(final Map<String, String> mapParams, AbilityRecordType type, final CardState state, final IHasSVars sVarHolder) {
        return getAbility(type, type.getApiTypeOf(mapParams), mapParams, null, state, sVarHolder);
    }

    public static Cost parseAbilityCost(final CardState state, Map<String, String> mapParams, AbilityRecordType type) {
        Cost abCost = null;
        if (type != AbilityRecordType.SubAbility) {
            String cost = mapParams.get("Cost");
            if (cost == null) {
                if (type == AbilityRecordType.Spell) {
                    SpellAbility firstAbility = state.getFirstAbility();
                    if (firstAbility != null && firstAbility.isSpell()) {
                        // TODO might remove when Enchant Keyword is refactored
                        System.err.println(state.getName() + " already has Spell using mana cost");
                    }
                    // for a Spell if no Cost is used, use the card states ManaCost
                    abCost = new Cost(state.getManaCost(), false);
                } else {
                    throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + state.getName());
                }
            } else {
                abCost = new Cost(cost, type == AbilityRecordType.Ability);
            }
        }
        return abCost;
    }

    public static final SpellAbility getAbility(AbilityRecordType type, ApiType api, Map<String, String> mapParams,
            Cost abCost, final CardState state, final IHasSVars sVarHolder) {
        final Card hostCard = state.getCard();
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
            // Clear out the auto created SpellPermanent spell
            if (type == AbilityRecordType.Spell
                    && !mapParams.containsKey("SubAbility") && !mapParams.containsKey("NonBasicSpell")) {
                hostCard.clearFirstSpell();
            }
        }

        if (abCost == null) {
            abCost = parseAbilityCost(state, mapParams, type);
        }
        SpellAbility spellAbility = type.buildSpellAbility(api, hostCard, abCost, abTgt, mapParams);

        if (spellAbility == null) {
            final StringBuilder msg = new StringBuilder();
            msg.append("AbilityFactory : SpellAbility was not created for ");
            msg.append(state.toString());
            msg.append(". Looking for API: ").append(api);
            throw new RuntimeException(msg.toString());
        }

        if (sVarHolder instanceof CardState) {
            spellAbility.setCardState((CardState)sVarHolder);
        } else if (sVarHolder instanceof CardTraitBase) {
            spellAbility.setCardState(((CardTraitBase)sVarHolder).getCardState());
        } else {
            spellAbility.setCardState(state);
        }

        if (mapParams.containsKey("Forecast")) {
            spellAbility.putParam("ActivationZone", "Hand");
            spellAbility.putParam("ActivationLimit", "1");
            spellAbility.putParam("ActivationPhases", "Upkeep");
            spellAbility.putParam("PlayerTurn", "True");
            spellAbility.putParam("PrecostDesc", "Forecast — ");
        }
        if (mapParams.containsKey("Boast")) {
            spellAbility.putParam("PresentDefined", "Self");
            spellAbility.putParam("IsPresent", "Card.attackedThisTurn");
            spellAbility.putParam("PrecostDesc", "Boast — ");
        }

        // *********************************************
        // set universal properties of the SpellAbility

        if ((api == ApiType.DelayedTrigger || api == ApiType.ImmediateTrigger) && mapParams.containsKey("Execute")) {
            spellAbility.setSVar(mapParams.get("Execute"), sVarHolder.getSVar(mapParams.get("Execute")));
        }

        if (mapParams.containsKey("PreventionSubAbility")) {
            spellAbility.setSVar(mapParams.get("PreventionSubAbility"), sVarHolder.getSVar(mapParams.get("PreventionSubAbility")));
        }

        if (mapParams.containsKey("SubAbility")) {
            final String name = mapParams.get("SubAbility");
            spellAbility.setSubAbility(getSubAbility(state, name, sVarHolder));
        }

        for (final String key : additionalAbilityKeys) {
            if (mapParams.containsKey(key) && spellAbility.getAdditionalAbility(key) == null) {
                spellAbility.setAdditionalAbility(key, getAbility(state, mapParams.get(key), sVarHolder));
            }
        }

        if (api == ApiType.Charm || api == ApiType.GenericChoice || api == ApiType.AssignGroup) {
            final String key = "Choices";
            if (mapParams.containsKey(key)) {
                List<String> names = Lists.newArrayList(mapParams.get(key).split(","));
                spellAbility.setAdditionalAbilityList(key, Lists.transform(names, new Function<String, AbilitySub>() {
                    @Override
                    public AbilitySub apply(String input) {
                        return getSubAbility(state, input, sVarHolder);
                    }
                }));
            }
        }

        if (api == ApiType.RollDice) {
            final String key = "ResultSubAbilities";
            if (mapParams.containsKey(key)) {
                String [] diceAbilities = mapParams.get(key).split(",");
                for (String ab : diceAbilities) {
                    String [] kv = ab.split(":");
                    spellAbility.setAdditionalAbility(kv[0], getSubAbility(state, kv[1], sVarHolder));
                }
            }
        }

        if (spellAbility instanceof SpellApiBased && hostCard.isPermanent()) {
            String desc = mapParams.getOrDefault("SpellDescription", spellAbility.getHostCard().getName());
            spellAbility.setDescription(desc);
        } else if (mapParams.containsKey("SpellDescription")) {
            spellAbility.rebuiltDescription();
        } else if (api == ApiType.Charm) {
            spellAbility.setDescription(CharmEffect.makeFormatedDescription(spellAbility));
        } else {
            spellAbility.setDescription("");
        }

        if (api == ApiType.RollDice) {
            spellAbility.setDescription(spellAbility.getDescription() + RollDiceEffect.makeFormatedDescription(spellAbility));
        } else if (api == ApiType.Repeat) {
            spellAbility.setDescription(spellAbility.getDescription() + spellAbility.getAdditionalAbility("RepeatSubAbility").getDescription());
        }

        initializeParams(spellAbility);
        makeRestrictions(spellAbility);
        makeConditions(spellAbility);

        return spellAbility;
    }

    private static final TargetRestrictions readTarget(Map<String, String> mapParams) {
        final String min = mapParams.getOrDefault("TargetMin", "1");
        final String max = mapParams.getOrDefault("TargetMax", "1");

        // TgtPrompt should only be needed for more complicated ValidTgts
        String tgtWhat = mapParams.get("ValidTgts");
        final String prompt;
        if (mapParams.containsKey("TgtPrompt")) {
            prompt = mapParams.get("TgtPrompt");
        } else if (tgtWhat.equals("Any")) {
            prompt = "Select any target";
        } else {
            final String[] commonStuff = new String[] {
                    //list of common one word non-core type ValidTgts that should be lowercase in the target prompt
                    "Player", "Opponent", "Card", "Spell", "Permanent"
            };
            if (Arrays.asList(commonStuff).contains(tgtWhat) || CardType.CoreType.isValidEnum(tgtWhat)) {
                tgtWhat = tgtWhat.toLowerCase();
            }
            prompt = "Select target " + tgtWhat;
        }

        TargetRestrictions abTgt = new TargetRestrictions(prompt, mapParams.get("ValidTgts").split(","), min, max);

        if (mapParams.containsKey("TgtZone")) {
            // if Targeting something not in play, this Key should be set
            abTgt.setZone(ZoneType.listValueOf(mapParams.get("TgtZone")));
        }

        if (mapParams.containsKey("MaxTotalTargetCMC")) {
            // only target cards up to a certain total max CMC
            abTgt.setMaxTotalCMC(mapParams.get("MaxTotalTargetCMC"));
        }

        if (mapParams.containsKey("MaxTotalTargetPower")) {
            // only target cards up to a certain total max power
            abTgt.setMaxTotalPower(mapParams.get("MaxTotalTargetPower"));
        }

        // TargetValidTargeting most for Counter: e.g. target spell that targets X.
        if (mapParams.containsKey("TargetValidTargeting")) {
            abTgt.setSAValidTargeting(mapParams.get("TargetValidTargeting"));
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
        if (mapParams.containsKey("TargetsWithSameCreatureType")) {
            abTgt.setWithSameCreatureType(true);
        }
        if (mapParams.containsKey("TargetsWithSameCardType")) {
            abTgt.setWithSameCardType(true);
        }
        if (mapParams.containsKey("TargetsWithSameController")) {
            abTgt.setSameController(true);
        }
        if (mapParams.containsKey("TargetsWithDifferentControllers")) {
            abTgt.setDifferentControllers(true);
        }
        if (mapParams.containsKey("TargetsWithDifferentCMC")) {
            abTgt.setDifferentCMC(true);
        }
        if (mapParams.containsKey("TargetsAtRandom")) {
            abTgt.setRandomTarget(true);
        }
        if (mapParams.containsKey("RandomNumTargets")) {
            abTgt.setRandomNumTargets(true);
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
     */
    private static final void initializeParams(final SpellAbility sa) {
        if (sa.hasParam("NonBasicSpell")) {
            sa.setBasicSpell(false);
        }
    }

    /**
     * <p>
     * makeRestrictions.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private static final void makeRestrictions(final SpellAbility sa) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityRestriction restrict = sa.getRestrictions();
        if (restrict != null) {
            restrict.setRestrictions(sa.getMapParams());
        }
    }

    /**
     * <p>
     * makeConditions.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private static final void makeConditions(final SpellAbility sa) {
        // SpellAbilityConditions should be added in here
        final SpellAbilityCondition condition = sa.getConditions();
        condition.setConditions(sa.getMapParams());
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
    private static final AbilitySub getSubAbility(CardState state, String sSub, final IHasSVars sVarHolder) {
        if (sVarHolder.hasSVar(sSub)) {
            return (AbilitySub) AbilityFactory.getAbility(state, sSub, sVarHolder);
        }
        System.out.println("SubAbility '"+ sSub +"' not found for: " + state.getName());

        return null;
    }

    public static final Map<String, String> getMapParams(final String abString) {
        return FileSection.parseToMap(abString, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    public static final void adjustChangeZoneTarget(final Map<String, String> params, final SpellAbility sa) {
        if (params.containsKey("Origin")) {
            List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));

            final TargetRestrictions tgt = sa.getTargetRestrictions();

            // Don't set the zone if it targets a player
            if (tgt != null && !tgt.canTgtPlayer()) {
                tgt.setZone(origin);
            }
        }
    }

    public static final SpellAbility buildFusedAbility(final Card card) {
        if (!card.isSplitCard())
            throw new IllegalStateException("Fuse ability may be built only on split cards");

        CardState leftState = card.getState(CardStateName.LeftSplit);
        SpellAbility leftAbility = leftState.getFirstAbility();
        Map<String, String> leftMap = Maps.newHashMap(leftAbility.getMapParams());
        AbilityRecordType leftType = AbilityRecordType.getRecordType(leftMap);
        ApiType leftApi = leftType.getApiTypeOf(leftMap);
        leftMap.put("StackDescription", leftMap.get("SpellDescription"));
        leftMap.put("SpellDescription", "Fuse (you may cast both halves of this card from your hand).");
        leftMap.put("ActivationZone", "Hand");

        CardState rightState = card.getState(CardStateName.RightSplit);
        SpellAbility rightAbility = rightState.getFirstAbility();
        Map<String, String> rightMap = Maps.newHashMap(rightAbility.getMapParams());

        AbilityRecordType rightType = AbilityRecordType.getRecordType(rightMap);
        ApiType rightApi = leftType.getApiTypeOf(rightMap);
        rightMap.put("StackDescription", rightMap.get("SpellDescription"));
        rightMap.put("SpellDescription", "");

        Cost totalCost = parseAbilityCost(leftState, leftMap, leftType);
        totalCost.add(parseAbilityCost(rightState, rightMap, rightType));

        final SpellAbility left = getAbility(leftType, leftApi, leftMap, totalCost, leftState, leftState);
        left.setCardState(card.getState(CardStateName.Original));
        final AbilitySub right = (AbilitySub) getAbility(AbilityRecordType.SubAbility, rightApi, rightMap, null, rightState, rightState);
        left.appendSubAbility(right);
        return left;
    }
}
