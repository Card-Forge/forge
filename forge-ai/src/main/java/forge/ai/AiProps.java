/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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
package forge.ai;

/** 
 * AI personality profile settings identifiers, and their default values.
 * When this class is instantiated, these enum values are used
 * in a map that is populated with the current AI profile settings
 * from the text file.
 */
public enum AiProps { /** */
    DEFAULT_MAX_PLANAR_DIE_ROLLS_PER_TURN ("1"), /** */
    DEFAULT_MIN_TURN_TO_ROLL_PLANAR_DIE ("3"), /** */
    DEFAULT_PLANAR_DIE_ROLL_CHANCE ("50"), /** */
    MULLIGAN_THRESHOLD ("5"), /** */
    PLANAR_DIE_ROLL_HESITATION_CHANCE ("10"),
    HOLD_LAND_DROP_FOR_MAIN2_IF_UNUSED ("0"), /** */
    HOLD_LAND_DROP_ONLY_IF_HAVE_OTHER_PERMS ("true"), /** */
    CHEAT_WITH_MANA_ON_SHUFFLE ("false"),
    MOVE_EQUIPMENT_TO_BETTER_CREATURES ("from_useless_only"),
    MOVE_EQUIPMENT_CREATURE_EVAL_THRESHOLD ("60"),
    PRIORITIZE_MOVE_EQUIPMENT_IF_USELESS ("true"),
    SAC_TO_REATTACH_TARGET_EVAL_THRESHOLD ("400"),
    PREDICT_SPELLS_FOR_MAIN2 ("true"), /** */
    RESERVE_MANA_FOR_MAIN2_CHANCE ("0"), /** */
    PLAY_AGGRO ("false"),
    CHANCE_TO_ATTACK_INTO_TRADE ("40"), /** */
    RANDOMLY_ATKTRADE_ONLY_ON_LOWER_LIFE_PRESSURE ("true"), /** */
    ATTACK_INTO_TRADE_WHEN_TAPPED_OUT ("false"), /** */
    CHANCE_TO_ATKTRADE_WHEN_OPP_HAS_MANA ("0"), /** */
    TRY_TO_AVOID_ATTACKING_INTO_CERTAIN_BLOCK ("true"), /** */
    TRY_TO_HOLD_COMBAT_TRICKS_UNTIL_BLOCK ("false"), /** */
    CHANCE_TO_HOLD_COMBAT_TRICKS_UNTIL_BLOCK ("30"), /** */
    ENABLE_RANDOM_FAVORABLE_TRADES_ON_BLOCK ("true"), /** */
    RANDOMLY_TRADE_EVEN_WHEN_HAVE_LESS_CREATS ("false"), /** */
    MAX_DIFF_IN_CREATURE_COUNT_TO_TRADE ("1"), /** */
    ALSO_TRADE_WHEN_HAVE_A_REPLACEMENT_CREAT ("true"), /** */
    MAX_DIFF_IN_CREATURE_COUNT_TO_TRADE_WITH_REPL ("1"), /** */
    MIN_CHANCE_TO_RANDOMLY_TRADE_ON_BLOCK ("30"), /** */
    MAX_CHANCE_TO_RANDOMLY_TRADE_ON_BLOCK ("70"), /** */
    CHANCE_DECREASE_TO_TRADE_VS_EMBALM ("30"), /** */
    CHANCE_TO_TRADE_TO_SAVE_PLANESWALKER ("70"), /** */
    CHANCE_TO_TRADE_DOWN_TO_SAVE_PLANESWALKER ("0"), /** */
    THRESHOLD_TOKEN_CHUMP_TO_SAVE_PLANESWALKER ("135"), /** */
    THRESHOLD_NONTOKEN_CHUMP_TO_SAVE_PLANESWALKER ("110"), /** */
    CHUMP_TO_SAVE_PLANESWALKER_ONLY_ON_LETHAL ("true"), /** */
    TRY_TO_PRESERVE_BUYBACK_SPELLS ("true"), /** */
    MIN_SPELL_CMC_TO_COUNTER ("0"), /** */
    CHANCE_TO_COUNTER_CMC_1 ("50"), /** */
    CHANCE_TO_COUNTER_CMC_2 ("75"), /** */
    CHANCE_TO_COUNTER_CMC_3 ("100"), /** */
    ALWAYS_COUNTER_OTHER_COUNTERSPELLS ("true"), /** */
    ALWAYS_COUNTER_DAMAGE_SPELLS ("true"), /** */
    ALWAYS_COUNTER_CMC_0_MANA_MAKING_PERMS ("true"), /** */
    ALWAYS_COUNTER_REMOVAL_SPELLS ("true"), /** */
    ALWAYS_COUNTER_PUMP_SPELLS ("true"), /** */
    ALWAYS_COUNTER_AURAS ("true"), /** */
    ALWAYS_COUNTER_SPELLS_FROM_NAMED_CARDS (""), /** */
    CHANCE_TO_COPY_OWN_SPELL_WHILE_ON_STACK ("30"), /** */
    ALWAYS_COPY_SPELL_IF_CMC_DIFF ("2"), /** */
    ACTIVELY_DESTROY_ARTS_AND_NONAURA_ENCHS ("true"), /** */
    ACTIVELY_DESTROY_IMMEDIATELY_UNBLOCKABLE ("false"), /** */
    ACTIVELY_PROTECT_VS_CURSE_AURAS("false"), /** */
    DESTROY_IMMEDIATELY_UNBLOCKABLE_THRESHOLD ("2"), /** */
    DESTROY_IMMEDIATELY_UNBLOCKABLE_ONLY_IN_DNGR ("true"), /** */
    DESTROY_IMMEDIATELY_UNBLOCKABLE_LIFE_IN_DNGR ("5"), /** */
    AVOID_TARGETING_CREATS_THAT_WILL_DIE ("true"), /** */
    DONT_EVAL_KILLSPELLS_ON_STACK_WITH_PERMISSION ("true"), /** */
    CHANCE_TO_CHAIN_TWO_DAMAGE_SPELLS("50"), /** */
    HOLD_X_DAMAGE_SPELLS_FOR_MORE_DAMAGE_CHANCE("100"),
    HOLD_X_DAMAGE_SPELLS_THRESHOLD("5"), /** */
    PRIORITY_REDUCTION_FOR_STORM_SPELLS ("0"), /** */
    USE_BERSERK_AGGRESSIVELY ("false"), /** */
    MIN_COUNT_FOR_STORM_SPELLS ("0"), /** */
    STRIPMINE_MIN_LANDS_IN_HAND_TO_ACTIVATE ("1"), /** */
    STRIPMINE_MIN_LANDS_FOR_NO_TIMING_CHECK ("3"), /** */
    STRIPMINE_MIN_LANDS_OTB_FOR_NO_TEMPO_CHECK ("6"), /** */
    STRIPMINE_MAX_LANDS_TO_ATTEMPT_MANALOCKING ("3"), /** */
    STRIPMINE_HIGH_PRIORITY_ON_SKIPPED_LANDDROP ("false"),
    TOKEN_GENERATION_ABILITY_CHANCE ("80"), /** */
    TOKEN_GENERATION_ALWAYS_IF_FROM_PLANESWALKER ("true"), /** */
    TOKEN_GENERATION_ALWAYS_IF_OPP_ATTACKS ("true"), /** */
    SCRY_NUM_LANDS_TO_STILL_NEED_MORE ("4"), /** */
    SCRY_NUM_LANDS_TO_NOT_NEED_MORE ("7"), /** */
    SCRY_NUM_CREATURES_TO_NOT_NEED_SUBPAR_ONES ("4"), /** */
    SCRY_EVALTHR_CREATCOUNT_TO_SCRY_AWAY_LOWCMC ("3"), /** */
    SCRY_EVALTHR_TO_SCRY_AWAY_LOWCMC_CREATURE ("160"), /** */
    SCRY_EVALTHR_CMC_THRESHOLD ("3"), /** */
    SCRY_IMMEDIATELY_UNCASTABLE_TO_BOTTOM ("false"), /** */
    SCRY_IMMEDIATELY_UNCASTABLE_CMC_DIFF ("1"), /** */
    SURVEIL_NUM_CARDS_IN_LIBRARY_TO_BAIL ("10"), /** */
    SURVEIL_LIFEPERC_AFTER_PAYING_LIFE ("75"), /** */
    COMBAT_ASSAULT_ATTACK_EVASION_PREDICTION ("true"), /** */
    COMBAT_ATTRITION_ATTACK_EVASION_PREDICTION ("true"), /** */
    CONSERVATIVE_ENERGY_PAYMENT_ONLY_IN_COMBAT ("true"), /** */
    CONSERVATIVE_ENERGY_PAYMENT_ONLY_DEFENSIVELY ("true"), /** */
    BOUNCE_ALL_TO_HAND_CREAT_EVAL_DIFF ("200"), /** */
    BOUNCE_ALL_ELSEWHERE_CREAT_EVAL_DIFF ("200"), /** */
    BOUNCE_ALL_TO_HAND_NONCREAT_EVAL_DIFF ("3"), /** */
    BOUNCE_ALL_ELSEWHERE_NONCREAT_EVAL_DIFF ("3"), /** */
    INTUITION_ALTERNATIVE_LOGIC ("false"), /** */
    EXPLORE_MAX_CMC_DIFF_TO_PUT_IN_GRAVEYARD ("2"),
    EXPLORE_NUM_LANDS_TO_STILL_NEED_MORE("2"), /** */
    MOMIR_BASIC_LAND_STRATEGY("default"), /** */
    MOJHOSTO_NUM_LANDS_TO_ACTIVATE_JHOIRA("5"), /** */
    MOJHOSTO_CHANCE_TO_PREFER_JHOIRA_OVER_MOMIR ("50"), /** */
    MOJHOSTO_CHANCE_TO_USE_JHOIRA_COPY_INSTANT ("20"), /** */
    AI_IN_DANGER_THRESHOLD("4"), /** */
    AI_IN_DANGER_MAX_THRESHOLD("4"), /** */
    FLASH_ENABLE_ADVANCED_LOGIC("true"), /** */
    FLASH_CHANCE_TO_OBEY_AMBUSHAI("100"), /** */
    FLASH_CHANCE_TO_CAST_DUE_TO_ETB_EFFECTS("100"), /** */
    FLASH_CHANCE_TO_CAST_FOR_ETB_BEFORE_MAIN1("10"), /** */
    FLASH_CHANCE_TO_RESPOND_TO_STACK_WITH_ETB("0"), /** */
    FLASH_CHANCE_TO_CAST_AS_VALUABLE_BLOCKER("100"),
    FLASH_USE_BUFF_AURAS_AS_COMBAT_TRICKS("true"),
    FLASH_BUFF_AURA_CHANCE_TO_CAST_EARLY("1"),
    FLASH_BUFF_AURA_CHANCE_CAST_AT_EOT("5"),
    FLASH_BUFF_AURA_CHANCE_TO_RESPOND_TO_STACK("100"),
    BLINK_RELOAD_PLANESWALKER_CHANCE("30"), /** */
    BLINK_RELOAD_PLANESWALKER_MAX_LOYALTY("2"), /** */
    BLINK_RELOAD_PLANESWALKER_LOYALTY_DIFF("2"),
    SACRIFICE_DEFAULT_PREF_ENABLE("true"),
    SACRIFICE_DEFAULT_PREF_MIN_CMC("0"),
    SACRIFICE_DEFAULT_PREF_MAX_CMC("2"),
    SACRIFICE_DEFAULT_PREF_ALLOW_TOKENS("true"),
    SACRIFICE_DEFAULT_PREF_MAX_CREATURE_EVAL("135"),
    SIDEBOARDING_ENABLE("true"),
    SIDEBOARDING_CHANCE_PER_CARD("50"),
    SIDEBOARDING_CHANCE_ON_WIN("0"),
    SIDEBOARDING_IN_LIMITED_FORMATS("false"),
    SIDEBOARDING_SHARED_TYPE_ONLY("false"),
    SIDEBOARDING_PLANESWALKER_EQ_CREATURE("false");
    // Experimental features, must be promoted or removed after extensive testing and, ideally, defaulting
    // <-- There are no experimental options here -->


    private final String strDefaultVal;

    /** @param s0 &emsp; {@link java.lang.String} */
    AiProps(final String s0) {
        this.strDefaultVal = s0;
    }

    /** @return {@link java.lang.String} */
    public String getDefault() {
        return strDefaultVal;
    }
}

