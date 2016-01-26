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
package forge.properties;

import forge.ai.AiProfileUtil;
import forge.game.GameLogEntryType;

public class ForgePreferences extends PreferencesStore<ForgePreferences.FPref> {
    /**
     * Preference identifiers, and their default values.
     */
    public static enum FPref {
        PLAYER_NAME (""),
        CONSTRUCTED_P1_DECK_STATE(""),
        CONSTRUCTED_P2_DECK_STATE(""),
        CONSTRUCTED_P3_DECK_STATE(""),
        CONSTRUCTED_P4_DECK_STATE(""),
        CONSTRUCTED_P5_DECK_STATE(""),
        CONSTRUCTED_P6_DECK_STATE(""),
        CONSTRUCTED_P7_DECK_STATE(""),
        CONSTRUCTED_P8_DECK_STATE(""),
        UI_LANDSCAPE_MODE ("false"),
        UI_COMPACT_MAIN_MENU ("false"),
        UI_USE_OLD ("false"),
        UI_RANDOM_FOIL ("false"),
        UI_ENABLE_AI_CHEATS ("false"),
        UI_AVATARS ("0,1"),
        UI_SHOW_CARD_OVERLAYS ("true"),
        UI_OVERLAY_CARD_NAME ("true"),
        UI_OVERLAY_CARD_POWER ("true"),
        UI_OVERLAY_CARD_MANA_COST ("true"),
        UI_OVERLAY_CARD_ID ("true"),
        UI_ENABLE_ONLINE_IMAGE_FETCHER ("false"),
        UI_OVERLAY_FOIL_EFFECT ("true"),
        UI_HIDE_REMINDER_TEXT ("false"),
        UI_OPEN_PACKS_INDIV ("false"),
        UI_STACK_CREATURES ("false"),
        UI_UPLOAD_DRAFT ("false"),
        UI_SCALE_LARGER ("true"),
        UI_LARGE_CARD_VIEWERS ("false"),
        UI_RANDOM_ART_IN_POOLS ("true"),
        UI_COMPACT_PROMPT ("false"),
        UI_COMPACT_TABS ("false"),
        UI_COMPACT_LIST_ITEMS ("false"),
        UI_CARD_SIZE ("small"),
        UI_SINGLE_CARD_ZOOM("false"),
        UI_BUGZ_NAME (""),
        UI_BUGZ_PWD (""),
        UI_ANTE ("false"),
        UI_ANTE_MATCH_RARITY ("false"),
        UI_MANABURN("false"),
        UI_SKIN ("Default"),
        UI_PREFERRED_AVATARS_ONLY ("false"),
        UI_TARGETING_OVERLAY ("0"),
        UI_ENABLE_SOUNDS ("true"),
        UI_ENABLE_MUSIC ("true"),
        UI_ALT_SOUND_SYSTEM ("false"),
        UI_CURRENT_AI_PROFILE (AiProfileUtil.AI_PROFILE_RANDOM_MATCH),
        UI_CLONE_MODE_SOURCE ("false"),
        UI_MATCH_IMAGE_VISIBLE ("true"),
        UI_THEMED_COMBOBOX ("true"), // Now applies to all theme settings, not just Combo.
        UI_LOCK_TITLE_BAR ("false"),
        UI_HIDE_GAME_TABS ("false"), // Visibility of tabs in match screen.
        UI_CLOSE_ACTION ("NONE"),
        UI_MANA_LOST_PROMPT ("false"), // Prompt on losing mana when passing priority
        UI_PAUSE_WHILE_MINIMIZED("false"),
        UI_TOKENS_IN_SEPARATE_ROW("false"), // Display tokens in their own battlefield row.
        UI_DISPLAY_CURRENT_COLORS("Never"),
        UI_FILTER_LANDS_BY_COLOR_IDENTITY("true"),

        UI_FOR_TOUCHSCREN("false"),

        UI_VIBRATE_ON_LIFE_LOSS("true"),
        UI_VIBRATE_ON_LONG_PRESS("true"),

        UI_LANGUAGE("en-US"),

        MATCH_HOT_SEAT_MODE("false"), //this only applies to mobile game
        MATCHPREF_PROMPT_FREE_BLOCKS("false"),

        NEW_GAME_SCREEN("Constructed"),
        LOAD_GAME_SCREEN("BoosterDraft"),
        PLAY_ONLINE_SCREEN("Lobby"),

        SUBMENU_CURRENTMENU ("CONSTRUCTED"),
        SUBMENU_SANCTIONED ("true"),
        SUBMENU_ONLINE ("false"),
        SUBMENU_GAUNTLET ("false"),
        SUBMENU_QUEST ("false"),
        SUBMENU_SETTINGS ("false"),
        SUBMENU_UTILITIES ("false"),

        ENFORCE_DECK_LEGALITY ("true"),

        DEV_MODE_ENABLED ("false"),
        DEV_WORKSHOP_SYNTAX ("false"),
        DEV_LOG_ENTRY_TYPE (GameLogEntryType.DAMAGE.toString()),

        DECK_DEFAULT_CARD_LIMIT ("4"),
        DECKGEN_SINGLETONS ("false"),
        DECKGEN_ARTIFACTS ("false"),
        DECKGEN_NOSMALL ("false"),

        PHASE_AI_UPKEEP ("false"),
        PHASE_AI_DRAW ("false"),
        PHASE_AI_MAIN1 ("false"),
        PHASE_AI_BEGINCOMBAT ("true"),
        PHASE_AI_DECLAREATTACKERS ("false"),
        PHASE_AI_DECLAREBLOCKERS ("true"),
        PHASE_AI_FIRSTSTRIKE ("false"),
        PHASE_AI_COMBATDAMAGE ("false"),
        PHASE_AI_ENDCOMBAT ("false"),
        PHASE_AI_MAIN2 ("false"),
        PHASE_AI_EOT ("true"),
        PHASE_AI_CLEANUP ("false"),

        PHASE_HUMAN_UPKEEP ("false"),
        PHASE_HUMAN_DRAW ("false"),
        PHASE_HUMAN_MAIN1 ("true"),
        PHASE_HUMAN_BEGINCOMBAT ("false"),
        PHASE_HUMAN_DECLAREATTACKERS ("false"),
        PHASE_HUMAN_DECLAREBLOCKERS ("true"),
        PHASE_HUMAN_FIRSTSTRIKE ("false"),
        PHASE_HUMAN_COMBATDAMAGE ("false"),
        PHASE_HUMAN_ENDCOMBAT ("false"),
        PHASE_HUMAN_MAIN2 ("true"),
        PHASE_HUMAN_EOT ("false"),
        PHASE_HUMAN_CLEANUP ("false"),

        ZONE_LOC_HUMAN_HAND(""),
        ZONE_LOC_HUMAN_LIBRARY(""),
        ZONE_LOC_HUMAN_GRAVEYARD(""),
        ZONE_LOC_HUMAN_EXILE(""),
        ZONE_LOC_HUMAN_FLASHBACK(""),

        ZONE_LOC_AI_HAND(""),
        ZONE_LOC_AI_LIBRARY(""),
        ZONE_LOC_AI_GRAVEYARD(""),
        ZONE_LOC_AI_EXILE(""),
        ZONE_LOC_AI_FLASHBACK(""),

        CHAT_WINDOW_LOC(""),

        SHORTCUT_SHOWSTACK ("83"),
        SHORTCUT_SHOWCOMBAT ("67"),
        SHORTCUT_SHOWCONSOLE ("76"),
        SHORTCUT_SHOWDEV ("68"),
        SHORTCUT_CONCEDE ("17"),
        SHORTCUT_ENDTURN ("69"),
        SHORTCUT_ALPHASTRIKE ("65"),
        SHORTCUT_SHOWTARGETING ("84"),
        SHORTCUT_AUTOYIELD_ALWAYS_YES ("89"),
        SHORTCUT_AUTOYIELD_ALWAYS_NO ("78");

        private final String strDefaultVal;

        private FPref(final String s0) {
            this.strDefaultVal = s0;
        }

        public String getDefault() {
            return strDefaultVal;
        }

        public static FPref[] CONSTRUCTED_DECK_STATES = {
            CONSTRUCTED_P1_DECK_STATE, CONSTRUCTED_P2_DECK_STATE,
            CONSTRUCTED_P3_DECK_STATE, CONSTRUCTED_P4_DECK_STATE,
            CONSTRUCTED_P5_DECK_STATE, CONSTRUCTED_P6_DECK_STATE,
            CONSTRUCTED_P7_DECK_STATE, CONSTRUCTED_P8_DECK_STATE };
    }

    /** Instantiates a ForgePreferences object. */
    public ForgePreferences() {
        super(ForgeConstants.MAIN_PREFS_FILE, FPref.class);
    }

    @Override
    protected FPref[] getEnumValues() {
        return FPref.values();
    }

    @Override
    protected FPref valueOf(final String name) {
        try {
            return FPref.valueOf(name);
        }
        catch (final Exception e) {
            return null;
        }
    }

    @Override
    protected String getPrefDefault(final FPref key) {
        return key.getDefault();
    }

    // was not used anywhere else
    public static boolean NET_CONN = false;

    /** The Constant DevMode. */
    // one for normal mode, one for quest mode
    public static boolean DEV_MODE;
    /** The Constant UpldDrft. */
    public static boolean UPLOAD_DRAFT;
}
