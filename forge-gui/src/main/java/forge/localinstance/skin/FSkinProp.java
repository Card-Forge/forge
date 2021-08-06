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
package forge.localinstance.skin;

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
    CLR_PWATTK_TARGETING_ARROW  (new int[] {70, 310}, PropType.COLOR),

    //zone images
    IMG_ZONE_HAND        (new int[] {280, 40, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_HAND      (new int[] {2, 136, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_LIBRARY     (new int[] {280, 0, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_LIBRARY   (new int[] {132, 136, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_EXILE       (new int[] {320, 40, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_EXILE     (new int[] {262, 136, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_FLASHBACK   (new int[] {280, 80, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_FLASHBACK (new int[] {262, 6, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_GRAVEYARD   (new int[] {320, 0, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_GRAVEYARD (new int[] {132, 6, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_ANTE        (new int[] {360, 0, 40, 40}, PropType.IMAGE),

    IMG_ZONE_SIDEBOARD   (new int[] {360, 40, 40, 40}, PropType.IMAGE),
    IMG_HDZONE_SIDEBOARD (new int[] {132, 1792, 128, 128}, PropType.BUTTONS),

    IMG_HDZONE_MANAPOOL  (new int[] {2, 6, 128, 128}, PropType.BUTTONS),

    IMG_ZONE_POISON      (new int[] {320, 80, 40, 40}, PropType.IMAGE),

    //mana images
    IMG_MANA_B         (new int[] {166, 2, 80, 80}, PropType.MANAICONS),
    IMG_MANA_R         (new int[] {330, 2, 80, 80}, PropType.MANAICONS),
    IMG_MANA_COLORLESS (new int[] {248, 2, 80, 80}, PropType.MANAICONS),
    IMG_MANA_U         (new int[] {330, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_G         (new int[] {166, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_W         (new int[] {412, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2B        (new int[] {166, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2G        (new int[] {248, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2R        (new int[] {330, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2U        (new int[] {412, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2W        (new int[] {166, 576, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_BG (new int[] {166, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_BR (new int[] {248, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_GU (new int[] {166, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_GW (new int[] {248, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_RG (new int[] {166, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_RW (new int[] {248, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_UB (new int[] {330, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_UR (new int[] {412, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_WB (new int[] {330, 412, 80, 80}, PropType.MANAICONS),
    IMG_MANA_HYBRID_WU (new int[] {412, 412, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX     (new int[] {166, 822, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX_U   (new int[] {330, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX_W   (new int[] {412, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX_R   (new int[] {412, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX_G   (new int[] {330, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_PHRYX_B   (new int[] {248, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_SNOW      (new int[] {412, 2, 80, 80}, PropType.MANAICONS),

    //generic mana images
    IMG_MANA_0   (new int[] {2, 2, 80, 80}, PropType.MANAICONS),
    IMG_MANA_1   (new int[] {84, 2, 80, 80}, PropType.MANAICONS),
    IMG_MANA_2   (new int[] {2, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_3   (new int[] {84, 84, 80, 80}, PropType.MANAICONS),
    IMG_MANA_4   (new int[] {2, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_5   (new int[] {84, 166, 80, 80}, PropType.MANAICONS),
    IMG_MANA_6   (new int[] {2, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_7   (new int[] {84, 248, 80, 80}, PropType.MANAICONS),
    IMG_MANA_8   (new int[] {2, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_9   (new int[] {84, 330, 80, 80}, PropType.MANAICONS),
    IMG_MANA_10  (new int[] {2, 412, 80, 80}, PropType.MANAICONS),
    IMG_MANA_11  (new int[] {84, 412, 80, 80}, PropType.MANAICONS),
    IMG_MANA_12  (new int[] {2, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_13  (new int[] {84, 494, 80, 80}, PropType.MANAICONS),
    IMG_MANA_14  (new int[] {2, 576, 80, 80}, PropType.MANAICONS),
    IMG_MANA_15  (new int[] {84, 576, 80, 80}, PropType.MANAICONS),
    IMG_MANA_16  (new int[] {2, 658, 80, 80}, PropType.MANAICONS),
    IMG_MANA_17  (new int[] {84, 658, 80, 80}, PropType.MANAICONS),
    IMG_MANA_18  (new int[] {166, 658, 80, 80}, PropType.MANAICONS),
    IMG_MANA_19  (new int[] {248, 658, 80, 80}, PropType.MANAICONS),
    IMG_MANA_20  (new int[] {330, 658, 80, 80}, PropType.MANAICONS),
    IMG_MANA_X   (new int[] {248, 576, 80, 80}, PropType.MANAICONS),
    IMG_MANA_Y   (new int[] {330, 576, 80, 80}, PropType.MANAICONS),
    IMG_MANA_Z   (new int[] {412, 576, 80, 80}, PropType.MANAICONS),

    //combination images for CMC ranges
    IMG_CMC_LOW      (new int[] {2, 2, 160, 160}, PropType.MANAICONS),
    IMG_CMC_LOW_MID  (new int[] {2, 84, 160, 160}, PropType.MANAICONS),
    IMG_CMC_MID_HIGH (new int[] {2, 166, 160, 160}, PropType.MANAICONS),
    IMG_CMC_HIGH     (new int[] {2, 248, 160, 160}, PropType.MANAICONS),

    //gameplay images
    IMG_TAP             (new int[] {166, 412, 80, 80}, PropType.MANAICONS),
    IMG_UNTAP           (new int[] {248, 412, 80, 80}, PropType.MANAICONS),
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
    IMG_ENERGY          (new int[] {320, 120, 40, 40}, PropType.IMAGE),
    IMG_EXPERIENCE      (new int[] {280, 120, 40, 30}, PropType.IMAGE),

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

    //choice-search-misc
    ICO_HDCHOICE     (new int[] {2, 1792, 128, 128}, PropType.BUTTONS),
    ICO_HDSIDEBOARD  (new int[] {132, 1792, 128, 128}, PropType.BUTTONS),
    ICO_HDPREFERENCE (new int[] {262, 1792, 128, 128}, PropType.BUTTONS),
    ICO_HDIMPORT     (new int[] {2, 1922, 128, 128}, PropType.BUTTONS),
    ICO_HDEXPORT     (new int[] {132, 1922, 128, 128}, PropType.BUTTONS),
    ICO_HDYIELD      (new int[] {262, 1922, 128, 128}, PropType.BUTTONS),
    ICO_BLANK        (new int[] {2, 2, 2, 2}, PropType.ICON), //safe coords, lower than 2 will cause crash on desktop
    IMG_LANDLOGO     (new int[] {84, 822, 80, 80}, PropType.MANAICONS),

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
    ICO_QUEST_BIG_ELIXIR  (new int[] {0, 880, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_BREW    (new int[] {160, 880, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_BM      (new int[] {320, 880, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_STAKES  (new int[] {480, 880, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_HOUSE   (new int[] {0, 1040, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_COIN    (new int[] {160, 1040, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_BOOK    (new int[] {320, 1040, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_MAP     (new int[] {480, 1040, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_ZEP     (new int[] {0, 1200, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_CHARM   (new int[] {160, 1200, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_BOOTS   (new int[] {320, 1200, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_SHIELD  (new int[] {480, 1200, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_ARMOR   (new int[] {0, 1360, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_AXE     (new int[] {160, 1360, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_SWORD   (new int[] {320, 1360, 160, 160}, PropType.ICON),
    ICO_QUEST_BIG_BAG     (new int[] {480, 1360, 160, 160}, PropType.ICON),

    //menu icon
    ICO_MENU_GALAXY       (new int[] {0, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_STATS        (new int[] {80, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_PUZZLE       (new int[] {160, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_GAUNTLET     (new int[] {240, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_SEALED       (new int[] {320, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_DRAFT        (new int[] {400, 1520, 80, 80}, PropType.ICON),
    ICO_MENU_CONSTRUCTED  (new int[] {480, 1520, 80, 80}, PropType.ICON),

    //interface icons
    ICO_QUESTION        (new int[] {560, 800, 32, 32}, PropType.ICON),
    ICO_INFORMATION     (new int[] {592, 800, 32, 32}, PropType.ICON),
    ICO_WARNING         (new int[] {560, 832, 32, 32}, PropType.ICON),
    ICO_ERROR           (new int[] {592, 832, 32, 32}, PropType.ICON),

    ICO_DELETE          (new int[] {640, 480, 20, 20}, PropType.ICON),
    ICO_HDDELETE        (new int[] {392, 134, 64, 64}, PropType.BUTTONS),

    ICO_DELETE_OVER     (new int[] {660, 480, 20, 20}, PropType.ICON),

    ICO_EDIT            (new int[] {640, 500, 20, 20}, PropType.ICON),
    ICO_HDEDIT          (new int[] {392, 200, 64, 64}, PropType.BUTTONS),

    ICO_EDIT_OVER       (new int[] {660, 500, 20, 20}, PropType.ICON),

    ICO_OPEN            (new int[] {660, 520, 20, 20}, PropType.ICON),
    ICO_HDOPEN          (new int[] {392, 68, 64, 64}, PropType.BUTTONS),

    ICO_MINUS           (new int[] {660, 620, 20, 20}, PropType.ICON),
    ICO_HDMINUS         (new int[] {391, 1506, 64, 64}, PropType.BUTTONS),

    ICO_NEW             (new int[] {660, 540, 20, 20}, PropType.ICON),

    ICO_PLUS            (new int[] {660, 600, 20, 20}, PropType.ICON),
    ICO_HDPLUS          (new int[] {391, 1572, 64, 64}, PropType.BUTTONS),

    ICO_PRINT           (new int[] {660, 640, 20, 20}, PropType.ICON),

    ICO_SAVE            (new int[] {660, 560, 20, 20}, PropType.ICON),
    ICO_HDSAVE          (new int[] {391, 1704, 64, 64}, PropType.BUTTONS),
    ICO_SAVEAS          (new int[] {660, 580, 20, 20}, PropType.ICON),
    ICO_HDSAVEAS          (new int[] {391, 1638, 64, 64}, PropType.BUTTONS),

    ICO_CLOSE           (new int[] {640, 640, 20, 20}, PropType.ICON),
    ICO_LIST            (new int[] {640, 660, 20, 20}, PropType.ICON),
    ICO_CARD_IMAGE      (new int[] {660, 660, 20, 20}, PropType.ICON),

    ICO_FOLDER          (new int[] {640, 680, 20, 20}, PropType.ICON),
    ICO_HDFOLDER        (new int[] {392, 2, 64, 64}, PropType.BUTTONS),

    ICO_SEARCH          (new int[] {660, 680, 20, 20}, PropType.ICON),
    ICO_HDSEARCH        (new int[] {391, 1374, 64, 64}, PropType.BUTTONS),

    ICO_UNKNOWN         (new int[] {0, 720, 80, 80}, PropType.ICON),
    ICO_LOGO            (new int[] {480, 0, 200, 200}, PropType.ICON),

    ICO_FLIPCARD        (new int[] {400, 0, 80, 120}, PropType.ICON),
    ICO_HDFLIPCARD      (new int[] {2, 1268, 387, 500}, PropType.BUTTONS),

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
    IMG_STAR_OUTLINE    (new int[] {640, 460, 20, 20}, PropType.IMAGE),
    IMG_HDSTAR_OUTLINE  (new int[] {391, 1308, 64, 64}, PropType.BUTTONS),
    IMG_STAR_FILLED     (new int[] {660, 460, 20, 20}, PropType.IMAGE),
    IMG_HDSTAR_FILLED   (new int[] {391, 1440, 64, 64}, PropType.BUTTONS),

    IMG_ARTIFACT        (new int[] {412, 658, 80, 80}, PropType.MANAICONS),
    IMG_CREATURE        (new int[] {2, 740, 80, 80}, PropType.MANAICONS),
    IMG_ENCHANTMENT     (new int[] {84, 740, 80, 80}, PropType.MANAICONS),
    IMG_INSTANT         (new int[] {166, 740, 80, 80}, PropType.MANAICONS),
    IMG_LAND            (new int[] {248, 740, 80, 80}, PropType.MANAICONS),
    IMG_MULTI           (new int[] {80, 720, 40, 40}, PropType.IMAGE),
    IMG_HDMULTI         (new int[] {2, 822, 80, 80}, PropType.MANAICONS),
    IMG_PLANESWALKER    (new int[] {330, 740, 80, 80}, PropType.MANAICONS),
    IMG_PACK            (new int[] {80, 760, 40, 40}, PropType.IMAGE),
    IMG_SORCERY         (new int[] {412, 740, 80, 80}, PropType.MANAICONS),
    IMG_COMMANDER       (new int[] {120, 760, 40, 40}, PropType.IMAGE),

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
    IMG_PLANE_MONITOR     (new int[] {2, 466, 900, 650}, PropType.PLANAR_CONQUEST),
    IMG_AETHER_SHARD      (new int[] {244, 224, 240, 240}, PropType.PLANAR_CONQUEST),
    IMG_MULTIVERSE        (new int[] {486, 244, 220, 220}, PropType.PLANAR_CONQUEST),
    IMG_SPELLBOOK         (new int[] {2, 224, 240, 240}, PropType.PLANAR_CONQUEST),
    IMG_PW_BADGE_COMMON   (new int[] {224, 2, 220, 220}, PropType.PLANAR_CONQUEST),
    IMG_PW_BADGE_UNCOMMON (new int[] {668, 22, 220, 220}, PropType.PLANAR_CONQUEST),
    IMG_PW_BADGE_RARE     (new int[] {2, 2, 220, 220}, PropType.PLANAR_CONQUEST),
    IMG_PW_BADGE_MYTHIC   (new int[] {446, 2, 220, 220}, PropType.PLANAR_CONQUEST),

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
    //hd buttons
    IMG_HDBTN_START_UP        (new int[] {2, 2, 588, 312}, PropType.BTNSTART),
    IMG_HDBTN_START_OVER      (new int[] {1183, 2, 588, 312}, PropType.BTNSTART),
    IMG_HDBTN_START_DOWN      (new int[] {593, 2, 588, 312}, PropType.BTNSTART),
    IMG_HDBTN_UP_LEFT         (new int[] {2, 266, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_UP_CENTER       (new int[] {162, 266, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_UP_RIGHT        (new int[] {322, 266, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_OVER_LEFT       (new int[] {2, 433, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_OVER_CENTER     (new int[] {162, 433, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_OVER_RIGHT      (new int[] {322, 433, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_DOWN_LEFT       (new int[] {2, 600, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_DOWN_CENTER     (new int[] {162, 600, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_DOWN_RIGHT      (new int[] {322, 600, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_FOCUS_LEFT      (new int[] {2, 767, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_FOCUS_CENTER    (new int[] {162, 767, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_FOCUS_RIGHT     (new int[] {322, 767, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_TOGGLE_LEFT     (new int[] {2, 934, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_TOGGLE_CENTER   (new int[] {162, 934, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_TOGGLE_RIGHT    (new int[] {322, 934, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_DISABLED_LEFT   (new int[] {2, 1101, 160, 165}, PropType.BUTTONS),
    IMG_HDBTN_DISABLED_CENTER (new int[] {162, 1101, 1, 165}, PropType.BUTTONS),
    IMG_HDBTN_DISABLED_RIGHT  (new int[] {322, 1101, 160, 165}, PropType.BUTTONS),

    //FOR DECKBOX
    IMG_DECK_GOLD_BG          (new int[] {2, 2, 488, 680}, PropType.DECKBOX),
    IMG_DECK_CARD_ART         (new int[] {492, 2, 488, 680}, PropType.DECKBOX),
    IMG_DECK_GENERIC          (new int[] {982, 2, 488, 680}, PropType.DECKBOX),

    IMG_FAV1    (new int[] {0, 0, 100, 100}, PropType.FAVICON),
    IMG_FAV2    (new int[] {100, 0, 100, 100}, PropType.FAVICON),
    IMG_FAV3    (new int[] {200, 0, 100, 100}, PropType.FAVICON),
    IMG_FAV4    (new int[] {300, 0, 100, 100}, PropType.FAVICON),
    IMG_FAV5    (new int[] {400, 0, 100, 100}, PropType.FAVICON),
    IMG_FAVNONE (new int[] {500, 0, 100, 100}, PropType.FAVICON),

    IMG_QUEST_DRAFT_DECK (new int[] {0, 0, 680, 475}, PropType.IMAGE),
    //COMMANDER
    IMG_ABILITY_COMMANDER      (new int[] {330, 576, 80, 80}, PropType.ABILITY),
    //Ability Icons
    IMG_ABILITY_DEATHTOUCH     (new int[] {2, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_DEFENDER       (new int[] {84, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_DOUBLE_STRIKE  (new int[] {166, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_FIRST_STRIKE   (new int[] {248, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_FEAR           (new int[] {84, 412, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_FLASH          (new int[] {166, 576, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_FLYING         (new int[] {330, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HASTE          (new int[] {412, 494, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF       (new int[] {412, 2, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HORSEMANSHIP   (new int[] {2, 576, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_INDESTRUCTIBLE (new int[] {2, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_INTIMIDATE     (new int[] {166, 412, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_LANDWALK       (new int[] {248, 576, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_LIFELINK       (new int[] {84, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_MENACE         (new int[] {166, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_REACH          (new int[] {248, 330, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_SHADOW         (new int[] {84, 576, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_SHROUD         (new int[] {330, 330, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_TRAMPLE        (new int[] {412, 330, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_VIGILANCE      (new int[] {2, 412, 80, 80}, PropType.ABILITY),
    //Hexproof From
    IMG_ABILITY_HEXPROOF_R       (new int[] {2, 494, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_G       (new int[] {412, 412, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_B       (new int[] {248, 412, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_U       (new int[] {84, 494, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_W       (new int[] {248, 494, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_C       (new int[] {330, 412, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_HEXPROOF_UB      (new int[] {166, 494, 80, 80}, PropType.ABILITY),
    //token icon
    IMG_ABILITY_TOKEN            (new int[] {330, 494, 80, 80}, PropType.ABILITY),
    //border
    IMG_BORDER_BLACK             (new int[] {2, 2, 672, 936}, PropType.BORDERS),
    IMG_BORDER_WHITE             (new int[] {676, 2, 672, 936}, PropType.BORDERS),
    //Protection From
    IMG_ABILITY_PROTECT_ALL           (new int[] {248, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_B             (new int[] {330, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_BU            (new int[] {412, 84, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_BW            (new int[] {2, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_COLOREDSPELLS (new int[] {84, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_G             (new int[] {166, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_GB            (new int[] {248, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_GU            (new int[] {330, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_GW            (new int[] {412, 166, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_GENERIC       (new int[] {2, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_R             (new int[] {84, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_RB            (new int[] {166, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_RG            (new int[] {248, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_RU            (new int[] {330, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_RW            (new int[] {412, 248, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_U             (new int[] {2, 330, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_UW            (new int[] {84, 330, 80, 80}, PropType.ABILITY),
    IMG_ABILITY_PROTECT_W             (new int[] {166, 330, 80, 80}, PropType.ABILITY);

    private int[] coords;
    private PropType type;

    FSkinProp(final int[] coords0, final PropType type0) {
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
        ABILITY,
        BORDERS,
        BUTTONS,
        BTNSTART,
        MANAICONS,
        PLANAR_CONQUEST,
        DECKBOX,
        FAVICON
    }
}
