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
package forge.assets;

/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 * 
 */
public enum FSkinProp {
    //backgrounds
    BG_SPLASH (null, PropType.BACKGROUND),
    BG_TEXTURE (null, PropType.BACKGROUND),
    BG_MATCH (null, PropType.BACKGROUND),

    //colors
    CLR_THEME                   (new int[] {70, 10}, PropType.COLOR),
    CLR_BORDERS                 (new int[] {70, 30}, PropType.COLOR),
    CLR_ZEBRA                   (new int[] {70, 50}, PropType.COLOR),
    CLR_HOVER                   (new int[] {70, 70}, PropType.COLOR),
    CLR_ACTIVE                  (new int[] {70, 90}, PropType.COLOR),
    CLR_INACTIVE                (new int[] {70, 110}, PropType.COLOR),
    CLR_TEXT                    (new int[] {70, 130}, PropType.COLOR),
    CLR_PHASE_INACTIVE_ENABLED  (new int[] {70, 150}, PropType.COLOR),
    CLR_PHASE_INACTIVE_DISABLED (new int[] {70, 170}, PropType.COLOR),
    CLR_PHASE_ACTIVE_ENABLED    (new int[] {70, 190}, PropType.COLOR),
    CLR_PHASE_ACTIVE_DISABLED   (new int[] {70, 210}, PropType.COLOR),
    CLR_THEME2                  (new int[] {70, 230}, PropType.COLOR),
    CLR_OVERLAY                 (new int[] {70, 250}, PropType.COLOR),
    CLR_COMBAT_TARGETING_ARROW  (new int[] {70, 270}, PropType.COLOR),
    CLR_NORMAL_TARGETING_ARROW  (new int[] {70, 290}, PropType.COLOR),

    //zone images
    IMG_ZONE_HAND        (new int[] {280, 40, 40, 40}, PropType.IMAGE),
    IMG_ZONE_LIBRARY     (new int[] {280, 0, 40, 40}, PropType.IMAGE),
    IMG_ZONE_EXILE       (new int[] {320, 40, 40, 40}, PropType.IMAGE),
    IMG_ZONE_FLASHBACK   (new int[] {280, 80, 40, 40}, PropType.IMAGE),
    IMG_ZONE_GRAVEYARD   (new int[] {320, 0, 40, 40}, PropType.IMAGE),
    IMG_ZONE_POISON      (new int[] {320, 80, 40, 40}, PropType.IMAGE),

    //mana images
    IMG_MANA_B         (new int[] {360, 160, 40, 40}, PropType.IMAGE),
    IMG_MANA_R         (new int[] {400, 160, 40, 40}, PropType.IMAGE),
    IMG_MANA_COLORLESS (new int[] {440, 160, 40, 40}, PropType.IMAGE),
    IMG_MANA_U         (new int[] {360, 200, 40, 40}, PropType.IMAGE),
    IMG_MANA_G         (new int[] {400, 200, 40, 40}, PropType.IMAGE),
    IMG_MANA_W         (new int[] {440, 200, 40, 40}, PropType.IMAGE),
    IMG_MANA_2B        (new int[] {360, 400, 40, 40}, PropType.IMAGE),
    IMG_MANA_2G        (new int[] {400, 400, 40, 40}, PropType.IMAGE),
    IMG_MANA_2R        (new int[] {440, 400, 40, 40}, PropType.IMAGE),
    IMG_MANA_2U        (new int[] {440, 360, 40, 40}, PropType.IMAGE),
    IMG_MANA_2W        (new int[] {400, 360, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_BG (new int[] {360, 240, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_BR (new int[] {400, 240, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_GU (new int[] {360, 280, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_GW (new int[] {440, 280, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_RG (new int[] {360, 320, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_RW (new int[] {400, 320, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_UB (new int[] {440, 240, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_UR (new int[] {440, 320, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_WB (new int[] {400, 280, 40, 40}, PropType.IMAGE),
    IMG_MANA_HYBRID_WU (new int[] {360, 360, 40, 40}, PropType.IMAGE),
    IMG_MANA_PHRYX_U   (new int[] {320, 200, 40, 40}, PropType.IMAGE),
    IMG_MANA_PHRYX_W   (new int[] {320, 240, 40, 40}, PropType.IMAGE),
    IMG_MANA_PHRYX_R   (new int[] {320, 280, 40, 40}, PropType.IMAGE),
    IMG_MANA_PHRYX_G   (new int[] {320, 320, 40, 40}, PropType.IMAGE),
    IMG_MANA_PHRYX_B   (new int[] {320, 360, 40, 40}, PropType.IMAGE),
    IMG_MANA_SNOW      (new int[] {320, 160, 40, 40}, PropType.IMAGE),

    //colorless mana images
    IMG_MANA_0   (new int[] {640, 200, 20, 20}, PropType.IMAGE),
    IMG_MANA_1   (new int[] {660, 200, 20, 20}, PropType.IMAGE),
    IMG_MANA_2   (new int[] {640, 220, 20, 20}, PropType.IMAGE),
    IMG_MANA_3   (new int[] {660, 220, 20, 20}, PropType.IMAGE),
    IMG_MANA_4   (new int[] {640, 240, 20, 20}, PropType.IMAGE),
    IMG_MANA_5   (new int[] {660, 240, 20, 20}, PropType.IMAGE),
    IMG_MANA_6   (new int[] {640, 260, 20, 20}, PropType.IMAGE),
    IMG_MANA_7   (new int[] {660, 260, 20, 20}, PropType.IMAGE),
    IMG_MANA_8   (new int[] {640, 280, 20, 20}, PropType.IMAGE),
    IMG_MANA_9   (new int[] {660, 280, 20, 20}, PropType.IMAGE),
    IMG_MANA_10  (new int[] {640, 300, 20, 20}, PropType.IMAGE),
    IMG_MANA_11  (new int[] {660, 300, 20, 20}, PropType.IMAGE),
    IMG_MANA_12  (new int[] {640, 320, 20, 20}, PropType.IMAGE),
    IMG_MANA_13  (new int[] {660, 320, 20, 20}, PropType.IMAGE),
    IMG_MANA_14  (new int[] {640, 340, 20, 20}, PropType.IMAGE),
    IMG_MANA_15  (new int[] {660, 340, 20, 20}, PropType.IMAGE),
    IMG_MANA_16  (new int[] {640, 360, 20, 20}, PropType.IMAGE),
    IMG_MANA_17  (new int[] {660, 360, 20, 20}, PropType.IMAGE),
    IMG_MANA_18  (new int[] {640, 380, 20, 20}, PropType.IMAGE),
    IMG_MANA_19  (new int[] {660, 380, 20, 20}, PropType.IMAGE),
    IMG_MANA_20  (new int[] {640, 400, 20, 20}, PropType.IMAGE),
    IMG_MANA_X   (new int[] {660, 400, 20, 20}, PropType.IMAGE),
    IMG_MANA_Y   (new int[] {640, 420, 20, 20}, PropType.IMAGE),
    IMG_MANA_Z   (new int[] {660, 420, 20, 20}, PropType.IMAGE),

    //gameplay images
    IMG_TAP             (new int[] {640, 440, 20, 20}, PropType.IMAGE),
    IMG_UNTAP           (new int[] {660, 440, 20, 20}, PropType.IMAGE),
    IMG_CHAOS           (new int[] {320, 400, 40, 40}, PropType.IMAGE),
    IMG_SLASH           (new int[] {660, 400, 10, 13}, PropType.IMAGE),
    IMG_ATTACK          (new int[] {160, 320, 80, 80, 32, 32}, PropType.IMAGE),
    IMG_DEFEND          (new int[] {160, 400, 80, 80, 32, 32}, PropType.IMAGE),
    IMG_SUMMONSICK      (new int[] {240, 400, 80, 80, 32, 32}, PropType.IMAGE),
    IMG_PHASING         (new int[] {240, 320, 80, 80, 32, 32}, PropType.IMAGE),
    IMG_COSTRESERVED    (new int[] {240, 240, 80, 80, 40, 40}, PropType.IMAGE),
    IMG_COUNTERS1       (new int[] {0, 320, 80, 80}, PropType.IMAGE),
    IMG_COUNTERS2       (new int[] {0, 400, 80, 80}, PropType.IMAGE),
    IMG_COUNTERS3       (new int[] {80, 320, 80, 80}, PropType.IMAGE),
    IMG_COUNTERS_MULTI  (new int[] {80, 400, 80, 80}, PropType.IMAGE),
    
    //foils
    FOIL_01     (new int[] {0, 0, 400, 570}, PropType.FOIL),
    FOIL_02     (new int[] {400, 0, 400, 570}, PropType.FOIL),
    FOIL_03     (new int[] {0, 570, 400, 570}, PropType.FOIL),
    FOIL_04     (new int[] {400, 570, 400, 570}, PropType.FOIL),
    FOIL_05     (new int[] {0, 1140, 400, 570}, PropType.FOIL),
    FOIL_06     (new int[] {400, 1140, 400, 570}, PropType.FOIL),
    FOIL_07     (new int[] {0, 1710, 400, 570}, PropType.FOIL),
    FOIL_08     (new int[] {400, 1710, 400, 570}, PropType.FOIL),
    FOIL_09     (new int[] {0, 2280, 400, 570}, PropType.FOIL),
    FOIL_10     (new int[] {400, 2280, 400, 570}, PropType.FOIL),

    //old foils
    FOIL_11     (new int[] {0, 0, 400, 570}, PropType.OLD_FOIL),
    FOIL_12     (new int[] {400, 0, 400, 570}, PropType.OLD_FOIL),
    FOIL_13     (new int[] {0, 570, 400, 570}, PropType.OLD_FOIL),
    FOIL_14     (new int[] {400, 570, 400, 570}, PropType.OLD_FOIL),
    FOIL_15     (new int[] {0, 1140, 400, 570}, PropType.OLD_FOIL),
    FOIL_16     (new int[] {400, 1140, 400, 570}, PropType.OLD_FOIL),
    FOIL_17     (new int[] {0, 1710, 400, 570}, PropType.OLD_FOIL),
    FOIL_18     (new int[] {400, 1710, 400, 570}, PropType.OLD_FOIL),
    FOIL_19     (new int[] {0, 2280, 400, 570}, PropType.OLD_FOIL),
    FOIL_20     (new int[] {400, 2280, 400, 570}, PropType.OLD_FOIL),

    //dock icons
    ICO_SHORTCUTS    (new int[] {160, 640, 80, 80}, PropType.ICON),
    ICO_SETTINGS     (new int[] {80, 640, 80, 80}, PropType.ICON),
    ICO_ENDTURN      (new int[] {320, 640, 80, 80}, PropType.ICON),
    ICO_CONCEDE      (new int[] {240, 640, 80, 80}, PropType.ICON),
    ICO_REVERTLAYOUT (new int[] {400, 720, 80, 80}, PropType.ICON),
    ICO_OPENLAYOUT   (new int[] {0, 800, 80, 80}, PropType.ICON),
    ICO_SAVELAYOUT   (new int[] {80, 800, 80, 80}, PropType.ICON),
    ICO_DECKLIST     (new int[] {400, 640, 80, 80}, PropType.ICON),
    ICO_ALPHASTRIKE  (new int[] {160, 800, 80, 80}, PropType.ICON),
    ICO_ARCSOFF      (new int[] {240, 800, 80, 80}, PropType.ICON),
    ICO_ARCSON       (new int[] {320, 800, 80, 80}, PropType.ICON),
    ICO_ARCSHOVER    (new int[] {400, 800, 80, 80}, PropType.ICON),

    //quest icons
    ICO_QUEST_ZEP         (new int[] {0, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_GEAR        (new int[] {80, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_GOLD        (new int[] {160, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_ELIXIR      (new int[] {240, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_BOOK        (new int[] {320, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_BOTTLES     (new int[] {400, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_BOX         (new int[] {480, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_COIN        (new int[] {560, 480, 80, 80}, PropType.ICON),
    ICO_QUEST_CHARM       (new int[] {480, 800, 80, 80}, PropType.ICON),
    ICO_QUEST_FOX         (new int[] {0, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_LEAF        (new int[] {80, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_LIFE        (new int[] {160, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_COINSTACK   (new int[] {240, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_MAP         (new int[] {320, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_NOTES       (new int[] {400, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_HEART       (new int[] {480, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_BREW        (new int[] {560, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_STAKES      (new int[] {400, 560, 80, 80}, PropType.ICON),
    ICO_QUEST_MINUS       (new int[] {560, 640, 80, 80}, PropType.ICON),
    ICO_QUEST_PLUS        (new int[] {480, 640, 80, 80}, PropType.ICON),
    ICO_QUEST_PLUSPLUS    (new int[] {480, 720, 80, 80}, PropType.ICON),

    //interface icons
    ICO_QUESTION        (new int[] {560, 800, 32, 32}, PropType.ICON),
    ICO_INFORMATION     (new int[] {592, 800, 32, 32}, PropType.ICON),
    ICO_WARNING         (new int[] {560, 832, 32, 32}, PropType.ICON),
    ICO_ERROR           (new int[] {592, 832, 32, 32}, PropType.ICON),
    ICO_DELETE          (new int[] {640, 480, 20, 20}, PropType.ICON),
    ICO_DELETE_OVER     (new int[] {660, 480, 20, 20}, PropType.ICON),
    ICO_EDIT            (new int[] {640, 500, 20, 20}, PropType.ICON),
    ICO_EDIT_OVER       (new int[] {660, 500, 20, 20}, PropType.ICON),
    ICO_OPEN            (new int[] {660, 520, 20, 20}, PropType.ICON),
    ICO_MINUS           (new int[] {660, 620, 20, 20}, PropType.ICON),
    ICO_NEW             (new int[] {660, 540, 20, 20}, PropType.ICON),
    ICO_PLUS            (new int[] {660, 600, 20, 20}, PropType.ICON),
    ICO_PRINT           (new int[] {660, 640, 20, 20}, PropType.ICON),
    ICO_SAVE            (new int[] {660, 560, 20, 20}, PropType.ICON),
    ICO_SAVEAS          (new int[] {660, 580, 20, 20}, PropType.ICON),
    ICO_CLOSE           (new int[] {640, 640, 20, 20}, PropType.ICON),
    ICO_LIST            (new int[] {640, 660, 20, 20}, PropType.ICON),
    ICO_CARD_IMAGE      (new int[] {660, 660, 20, 20}, PropType.ICON),
    ICO_FOLDER          (new int[] {640, 680, 20, 20}, PropType.ICON),
    ICO_SEARCH          (new int[] {660, 680, 20, 20}, PropType.ICON),
    ICO_UNKNOWN         (new int[] {0, 720, 80, 80}, PropType.ICON),
    ICO_LOGO            (new int[] {480, 0, 200, 200}, PropType.ICON),
    ICO_FLIPCARD        (new int[] {400, 0, 80, 120}, PropType.ICON),
    ICO_FAVICON         (new int[] {0, 640, 80, 80}, PropType.ICON),
    ICO_LOCK            (new int[] {620, 800, 48, 48}, PropType.ICON),

    //layout images
    IMG_HANDLE  (new int[] {320, 450, 80, 20}, PropType.IMAGE),
    IMG_CUR_L   (new int[] {564, 724, 32, 32}, PropType.IMAGE),
    IMG_CUR_R   (new int[] {564, 764, 32, 32}, PropType.IMAGE),
    IMG_CUR_T   (new int[] {604, 724, 32, 32}, PropType.IMAGE),
    IMG_CUR_B   (new int[] {604, 764, 32, 32}, PropType.IMAGE),
    IMG_CUR_TAB (new int[] {644, 764, 32, 32}, PropType.IMAGE),

    //editor images
    IMG_STAR_OUTINE     (new int[] {640, 460, 20, 20}, PropType.IMAGE),
    IMG_STAR_FILLED     (new int[] {660, 460, 20, 20}, PropType.IMAGE),
    IMG_ARTIFACT        (new int[] {280, 720, 40, 40}, PropType.IMAGE),
    IMG_CREATURE        (new int[] {240, 720, 40, 40}, PropType.IMAGE),
    IMG_ENCHANTMENT     (new int[] {320, 720, 40, 40}, PropType.IMAGE),
    IMG_INSTANT         (new int[] {360, 720, 40, 40}, PropType.IMAGE),
    IMG_LAND            (new int[] {120, 720, 40, 40}, PropType.IMAGE),
    IMG_MULTI           (new int[] {80, 720, 40, 40}, PropType.IMAGE),
    IMG_PLANESWALKER    (new int[] {200, 720, 40, 40}, PropType.IMAGE),
    IMG_PACK            (new int[] {80, 760, 40, 40}, PropType.IMAGE),
    IMG_SORCERY         (new int[] {160, 720, 40, 40}, PropType.IMAGE),

    //achievement trophies and shelf
    IMG_COMMON_TROPHY     (new int[] {0, 0, 135, 185}, PropType.TROPHY),
    IMG_UNCOMMON_TROPHY   (new int[] {135, 0, 135, 185}, PropType.TROPHY),
    IMG_RARE_TROPHY       (new int[] {270, 0, 135, 185}, PropType.TROPHY),
    IMG_MYTHIC_TROPHY     (new int[] {405, 0, 135, 185}, PropType.TROPHY),
    IMG_SPECIAL_TROPHY    (new int[] {540, 0, 135, 185}, PropType.TROPHY),
    IMG_TROPHY_PLATE      (new int[] {675, 0, 170, 40}, PropType.TROPHY),
    IMG_TROPHY_CASE_TOP   (new int[] {0, 185, 798, 38}, PropType.TROPHY),
    IMG_TROPHY_SHELF      (new int[] {0, 223, 798, 257}, PropType.TROPHY),

    //planar conquest images
    IMG_HEXAGON_TILE      (new int[] {354, 0, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_COLORLESS_TILE    (new int[] {0, 354, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_WHITE_TILE        (new int[] {0, 0, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_BLUE_TILE         (new int[] {89, 177, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_BLACK_TILE        (new int[] {177, 0, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_RED_TILE          (new int[] {266, 177, 151, 173}, PropType.PLANAR_CONQUEST),
    IMG_GREEN_TILE        (new int[] {177, 354, 151, 173}, PropType.PLANAR_CONQUEST),

    //button images
    IMG_BTN_START_UP        (new int[] {480, 200, 160, 80}, PropType.ICON),
    IMG_BTN_START_OVER      (new int[] {480, 280, 160, 80}, PropType.ICON),
    IMG_BTN_START_DOWN      (new int[] {480, 360, 160, 80}, PropType.ICON),
    IMG_BTN_UP_LEFT         (new int[] {80, 0, 40, 40}, PropType.ICON),
    IMG_BTN_UP_CENTER       (new int[] {120, 0, 1, 40}, PropType.ICON),
    IMG_BTN_UP_RIGHT        (new int[] {160, 0, 40, 40}, PropType.ICON),
    IMG_BTN_OVER_LEFT       (new int[] {80, 40, 40, 40}, PropType.ICON),
    IMG_BTN_OVER_CENTER     (new int[] {120, 40, 1, 40}, PropType.ICON),
    IMG_BTN_OVER_RIGHT      (new int[] {160, 40, 40, 40}, PropType.ICON),
    IMG_BTN_DOWN_LEFT       (new int[] {80, 80, 40, 40}, PropType.ICON),
    IMG_BTN_DOWN_CENTER     (new int[] {120, 80, 1, 40}, PropType.ICON),
    IMG_BTN_DOWN_RIGHT      (new int[] {160, 80, 40, 40}, PropType.ICON),
    IMG_BTN_FOCUS_LEFT      (new int[] {80, 120, 40, 40}, PropType.ICON),
    IMG_BTN_FOCUS_CENTER    (new int[] {120, 120, 1, 40}, PropType.ICON),
    IMG_BTN_FOCUS_RIGHT     (new int[] {160, 120, 40, 40}, PropType.ICON),
    IMG_BTN_TOGGLE_LEFT     (new int[] {80, 160, 40, 40}, PropType.ICON),
    IMG_BTN_TOGGLE_CENTER   (new int[] {120, 160, 1, 40}, PropType.ICON),
    IMG_BTN_TOGGLE_RIGHT    (new int[] {160, 160, 40, 40}, PropType.ICON),
    IMG_BTN_DISABLED_LEFT   (new int[] {80, 200, 40, 40}, PropType.ICON),
    IMG_BTN_DISABLED_CENTER (new int[] {120, 200, 1, 40}, PropType.ICON),
    IMG_BTN_DISABLED_RIGHT  (new int[] {160, 200, 40, 40}, PropType.ICON),
    
    IMG_QUEST_DRAFT_DECK (new int[] {0, 0, 680, 475}, PropType.IMAGE);
    
    private int[] coords;
    private PropType type;

    private FSkinProp(final int[] coords0, final PropType type0) {
        coords = coords0;
        type = type0;
    }

    public int[] getCoords() {
        return coords;
    }
    public PropType getType() {
        return type;
    }

    public int getWidth() {
        return coords[2];
    }

    public int getHeight() {
        return coords[3];
    }

    public enum PropType {
        BACKGROUND,
        COLOR,
        IMAGE,
        ICON,
        FOIL,
        OLD_FOIL,
        TROPHY,
        PLANAR_CONQUEST
    }
}
