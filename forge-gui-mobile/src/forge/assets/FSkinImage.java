package forge.assets;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.localinstance.skin.FSkinProp;

/** Properties of various components that make up the skin.
 * This interface allows all enums to be under the same roof.
 * It also enforces a getter for coordinate locations in sprites. */
public enum FSkinImage implements FSkinImageInterface {
    //Zones
    HAND        (FSkinProp.IMG_ZONE_HAND),
    HDHAND      (FSkinProp.IMG_HDZONE_HAND),

    LIBRARY     (FSkinProp.IMG_ZONE_LIBRARY),
    HDLIBRARY   (FSkinProp.IMG_HDZONE_LIBRARY),

    EXILE       (FSkinProp.IMG_ZONE_EXILE),
    HDEXILE     (FSkinProp.IMG_HDZONE_EXILE),

    FLASHBACK   (FSkinProp.IMG_ZONE_FLASHBACK),
    HDFLASHBACK (FSkinProp.IMG_HDZONE_FLASHBACK),

    GRAVEYARD   (FSkinProp.IMG_ZONE_GRAVEYARD),
    HDGRAVEYARD (FSkinProp.IMG_HDZONE_GRAVEYARD),

    SIDEBOARD   (FSkinProp.IMG_ZONE_SIDEBOARD),

    HDMANAPOOL   (FSkinProp.IMG_HDZONE_MANAPOOL),

    POISON      (FSkinProp.IMG_ZONE_POISON),

    //CMC ranges
    CMC_LOW        (FSkinProp.IMG_CMC_LOW),
    CMC_LOW_MID    (FSkinProp.IMG_CMC_LOW_MID),
    CMC_MID_HIGH   (FSkinProp.IMG_CMC_MID_HIGH),
    CMC_HIGH       (FSkinProp.IMG_CMC_HIGH),

    //Setlogo
    SET_COMMON     (FSkinProp.IMG_SETLOGO_COMMON),
    SET_UNCOMMON   (FSkinProp.IMG_SETLOGO_UNCOMMON),
    SET_RARE       (FSkinProp.IMG_SETLOGO_RARE),
    SET_MYTHIC     (FSkinProp.IMG_SETLOGO_MYTHIC),
    SET_SPECIAL    (FSkinProp.IMG_SETLOGO_SPECIAL),

    //Watermarks
    WATERMARK_G    (FSkinProp.IMG_WATERMARK_G),
    WATERMARK_R    (FSkinProp.IMG_WATERMARK_R),
    WATERMARK_B    (FSkinProp.IMG_WATERMARK_B),
    WATERMARK_U    (FSkinProp.IMG_WATERMARK_U),
    WATERMARK_W    (FSkinProp.IMG_WATERMARK_W),
    WATERMARK_C    (FSkinProp.IMG_WATERMARK_C),

    //draft ranks
    DRAFTRANK_D (FSkinProp.IMG_DRAFTRANK_D),
    DRAFTRANK_C (FSkinProp.IMG_DRAFTRANK_C),
    DRAFTRANK_B (FSkinProp.IMG_DRAFTRANK_B),
    DRAFTRANK_A (FSkinProp.IMG_DRAFTRANK_A),
    DRAFTRANK_S (FSkinProp.IMG_DRAFTRANK_S),

    //Gameplay
    CHAOS           (FSkinProp.IMG_CHAOS),
    SLASH           (FSkinProp.IMG_SLASH),
    ATTACK          (FSkinProp.IMG_ATTACK),
    DEFEND          (FSkinProp.IMG_DEFEND),
    SUMMONSICK      (FSkinProp.IMG_SUMMONSICK),
    PHASING         (FSkinProp.IMG_PHASING),
    COSTRESERVED    (FSkinProp.IMG_COSTRESERVED),
    COUNTERS1       (FSkinProp.IMG_COUNTERS1),
    COUNTERS2       (FSkinProp.IMG_COUNTERS2),
    COUNTERS3       (FSkinProp.IMG_COUNTERS3),
    COUNTERS_MULTI  (FSkinProp.IMG_COUNTERS_MULTI),
    ENERGY          (FSkinProp.IMG_ENERGY),
    TICKET          (FSkinProp.IMG_TICKET),
    RAD             (FSkinProp.IMG_RAD),

    //Dock Icons
    SHORTCUTS    (FSkinProp.ICO_SHORTCUTS),
    SETTINGS     (FSkinProp.ICO_SETTINGS),
    ENDTURN      (FSkinProp.ICO_ENDTURN),
    CONCEDE      (FSkinProp.ICO_CONCEDE),
    REVERTLAYOUT (FSkinProp.ICO_REVERTLAYOUT),
    OPENLAYOUT   (FSkinProp.ICO_OPENLAYOUT),
    SAVELAYOUT   (FSkinProp.ICO_SAVELAYOUT),
    DECKLIST     (FSkinProp.ICO_DECKLIST),
    ALPHASTRIKE  (FSkinProp.ICO_ALPHASTRIKE),
    ARCSOFF      (FSkinProp.ICO_ARCSOFF),
    ARCSON       (FSkinProp.ICO_ARCSON),
    ARCSHOVER    (FSkinProp.ICO_ARCSHOVER),

    //choice-search-misc
    HDCHOICE     (FSkinProp.ICO_HDCHOICE),
    HDSIDEBOARD  (FSkinProp.ICO_HDSIDEBOARD),
    HDPREFERENCE (FSkinProp.ICO_HDPREFERENCE),
    HDIMPORT     (FSkinProp.ICO_HDIMPORT),
    HDEXPORT     (FSkinProp.ICO_HDEXPORT),
    HDYIELD      (FSkinProp.ICO_HDYIELD),
    BLANK        (FSkinProp.ICO_BLANK),

    //Achievement Trophies
    COMMON_TROPHY    (FSkinProp.IMG_COMMON_TROPHY),
    UNCOMMON_TROPHY  (FSkinProp.IMG_UNCOMMON_TROPHY),
    RARE_TROPHY      (FSkinProp.IMG_RARE_TROPHY),
    MYTHIC_TROPHY    (FSkinProp.IMG_MYTHIC_TROPHY),
    SPECIAL_TROPHY   (FSkinProp.IMG_SPECIAL_TROPHY),
    TROPHY_PLATE     (FSkinProp.IMG_TROPHY_PLATE),
    TROPHY_CASE_TOP  (FSkinProp.IMG_TROPHY_CASE_TOP),
    TROPHY_SHELF     (FSkinProp.IMG_TROPHY_SHELF),

    //Planar Conquest Images
    PLANE_MONITOR     (FSkinProp.IMG_PLANE_MONITOR),
    AETHER_SHARD      (FSkinProp.IMG_AETHER_SHARD),
    MULTIVERSE        (FSkinProp.IMG_MULTIVERSE),
    SPELLBOOK         (FSkinProp.IMG_SPELLBOOK),
    PW_BADGE_COMMON   (FSkinProp.IMG_PW_BADGE_COMMON),
    PW_BADGE_UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON),
    PW_BADGE_RARE     (FSkinProp.IMG_PW_BADGE_RARE),
    PW_BADGE_MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC),

    //Quest Icons
    QUEST_ZEP         (FSkinProp.ICO_QUEST_ZEP),
    QUEST_GEAR        (FSkinProp.ICO_QUEST_GEAR),
    QUEST_GOLD        (FSkinProp.ICO_QUEST_GOLD),
    QUEST_ELIXIR      (FSkinProp.ICO_QUEST_ELIXIR),
    QUEST_BOOK        (FSkinProp.ICO_QUEST_BOOK),
    QUEST_BOTTLES     (FSkinProp.ICO_QUEST_BOTTLES),
    QUEST_BOX         (FSkinProp.ICO_QUEST_BOX),
    QUEST_COIN        (FSkinProp.ICO_QUEST_COIN),
    QUEST_CHARM       (FSkinProp.ICO_QUEST_CHARM),
    QUEST_FOX         (FSkinProp.ICO_QUEST_FOX),
    QUEST_LEAF        (FSkinProp.ICO_QUEST_LEAF),
    QUEST_LIFE        (FSkinProp.ICO_QUEST_LIFE),
    QUEST_COINSTACK   (FSkinProp.ICO_QUEST_COINSTACK),
    QUEST_MAP         (FSkinProp.ICO_QUEST_MAP),
    QUEST_NOTES       (FSkinProp.ICO_QUEST_NOTES),
    QUEST_HEART       (FSkinProp.ICO_QUEST_HEART),
    QUEST_BREW        (FSkinProp.ICO_QUEST_BREW),
    QUEST_STAKES      (FSkinProp.ICO_QUEST_STAKES),
    QUEST_MINUS       (FSkinProp.ICO_QUEST_MINUS),
    QUEST_PLUS        (FSkinProp.ICO_QUEST_PLUS),
    QUEST_PLUSPLUS    (FSkinProp.ICO_QUEST_PLUSPLUS),
    QUEST_BIG_ELIXIR  (FSkinProp.ICO_QUEST_BIG_ELIXIR),
    QUEST_BIG_BREW    (FSkinProp.ICO_QUEST_BIG_BREW),
    QUEST_BIG_BM      (FSkinProp.ICO_QUEST_BIG_BM),
    QUEST_BIG_STAKES  (FSkinProp.ICO_QUEST_BIG_STAKES),
    QUEST_BIG_HOUSE   (FSkinProp.ICO_QUEST_BIG_HOUSE),
    QUEST_BIG_COIN    (FSkinProp.ICO_QUEST_BIG_COIN),
    QUEST_BIG_BOOK    (FSkinProp.ICO_QUEST_BIG_BOOK),
    QUEST_BIG_MAP     (FSkinProp.ICO_QUEST_BIG_MAP),
    QUEST_BIG_ZEP     (FSkinProp.ICO_QUEST_BIG_ZEP),
    QUEST_BIG_CHARM   (FSkinProp.ICO_QUEST_BIG_CHARM),
    QUEST_BIG_BOOTS   (FSkinProp.ICO_QUEST_BIG_BOOTS),
    QUEST_BIG_SHIELD  (FSkinProp.ICO_QUEST_BIG_SHIELD),
    QUEST_BIG_ARMOR   (FSkinProp.ICO_QUEST_BIG_ARMOR),
    QUEST_BIG_AXE     (FSkinProp.ICO_QUEST_BIG_AXE),
    QUEST_BIG_SWORD   (FSkinProp.ICO_QUEST_BIG_SWORD),
    QUEST_BIG_BAG     (FSkinProp.ICO_QUEST_BIG_BAG),

    //adventure
    MANASHARD         (FSkinProp.ICO_MANASHARD),
    MENU_ADVLOGO      (FSkinProp.ICO_ADVLOGO),
    ADV_DECKBOX       (FSkinProp.ICO_ADVDECKBOX),
    ADV_FLIPICON      (FSkinProp.ICO_ADVFLIP),

    //menu icon
    MENU_GALAXY       (FSkinProp.ICO_MENU_GALAXY),
    MENU_STATS        (FSkinProp.ICO_MENU_STATS),
    MENU_PUZZLE       (FSkinProp.ICO_MENU_PUZZLE),
    MENU_GAUNTLET     (FSkinProp.ICO_MENU_GAUNTLET),
    MENU_SEALED       (FSkinProp.ICO_MENU_SEALED),
    MENU_DRAFT        (FSkinProp.ICO_MENU_DRAFT),
    MENU_CONSTRUCTED  (FSkinProp.ICO_MENU_CONSTRUCTED),

    //Interface icons
    QUESTION        (FSkinProp.ICO_QUESTION),
    INFORMATION     (FSkinProp.ICO_INFORMATION),
    WARNING         (FSkinProp.ICO_WARNING),
    ERROR           (FSkinProp.ICO_ERROR),

    DELETE          (FSkinProp.ICO_DELETE),
    HDDELETE        (FSkinProp.ICO_HDDELETE),

    DELETE_OVER     (FSkinProp.ICO_DELETE_OVER),

    EDIT            (FSkinProp.ICO_EDIT),
    HDEDIT            (FSkinProp.ICO_HDEDIT),

    EDIT_OVER       (FSkinProp.ICO_EDIT_OVER),

    OPEN            (FSkinProp.ICO_OPEN),
    HDOPEN          (FSkinProp.ICO_HDOPEN),

    MINUS           (FSkinProp.ICO_MINUS),
    HDMINUS         (FSkinProp.ICO_HDMINUS),

    NEW             (FSkinProp.ICO_NEW),

    PLUS            (FSkinProp.ICO_PLUS),
    HDPLUS          (FSkinProp.ICO_HDPLUS),

    PRINT           (FSkinProp.ICO_PRINT),

    SAVE            (FSkinProp.ICO_SAVE),
    HDSAVE          (FSkinProp.ICO_HDSAVE),
    SAVEAS          (FSkinProp.ICO_SAVEAS),
    HDSAVEAS        (FSkinProp.ICO_HDSAVEAS),

    CLOSE           (FSkinProp.ICO_CLOSE),
    LIST            (FSkinProp.ICO_LIST),
    CARD_IMAGE      (FSkinProp.ICO_CARD_IMAGE),

    FOLDER          (FSkinProp.ICO_FOLDER),
    HDFOLDER        (FSkinProp.ICO_HDFOLDER),

    SEARCH          (FSkinProp.ICO_SEARCH),
    HDSEARCH        (FSkinProp.ICO_HDSEARCH),

    UNKNOWN         (FSkinProp.ICO_UNKNOWN),
    LOGO            (FSkinProp.ICO_LOGO),
    CARDART         (FSkinProp.ICO_CARDART),
    PADLOCK         (FSkinProp.ICO_PADLOCK),

    FLIPCARD        (FSkinProp.ICO_FLIPCARD),
    HDFLIPCARD      (FSkinProp.ICO_HDFLIPCARD),

    FAVICON         (FSkinProp.ICO_FAVICON),
    LOCK            (FSkinProp.ICO_LOCK),
    //reveal icons
    SEE             (FSkinProp.ICO_SEE),
    UNSEE           (FSkinProp.ICO_UNSEE),

    //Layout images
    HANDLE  (FSkinProp.IMG_HANDLE),
    CUR_L   (FSkinProp.IMG_CUR_L),
    CUR_R   (FSkinProp.IMG_CUR_R),
    CUR_T   (FSkinProp.IMG_CUR_T),
    CUR_B   (FSkinProp.IMG_CUR_B),
    CUR_TAB (FSkinProp.IMG_CUR_TAB),

    //Editor images
    STAR_OUTLINE    (FSkinProp.IMG_STAR_OUTLINE),
    HDSTAR_OUTLINE  (FSkinProp.IMG_HDSTAR_OUTLINE),
    STAR_FILLED     (FSkinProp.IMG_STAR_FILLED),
    HDSTAR_FILLED   (FSkinProp.IMG_HDSTAR_FILLED),
    AI_ACTIVE       (FSkinProp.IMG_AI_ACTIVE),
    AI_INACTIVE     (FSkinProp.IMG_AI_INACTIVE),

    ARTIFACT        (FSkinProp.IMG_ARTIFACT),
    CREATURE        (FSkinProp.IMG_CREATURE),
    ENCHANTMENT     (FSkinProp.IMG_ENCHANTMENT),
    INSTANT         (FSkinProp.IMG_INSTANT),
    LAND            (FSkinProp.IMG_LAND),
    LANDLOGO        (FSkinProp.IMG_LANDLOGO),
    MULTI           (FSkinProp.IMG_MULTI),
    HDMULTI         (FSkinProp.IMG_HDMULTI),
    PLANESWALKER    (FSkinProp.IMG_PLANESWALKER),
    PACK            (FSkinProp.IMG_PACK),
    SORCERY         (FSkinProp.IMG_SORCERY),
    BATTLE          (FSkinProp.IMG_BATTLE),
    COMMANDER       (FSkinProp.IMG_COMMANDER),

    //Buttons
    BTN_START_UP        (FSkinProp.IMG_BTN_START_UP),
    BTN_START_OVER      (FSkinProp.IMG_BTN_START_OVER),
    BTN_START_DOWN      (FSkinProp.IMG_BTN_START_DOWN),
    BTN_UP_LEFT         (FSkinProp.IMG_BTN_UP_LEFT),
    BTN_UP_CENTER       (FSkinProp.IMG_BTN_UP_CENTER),
    BTN_UP_RIGHT        (FSkinProp.IMG_BTN_UP_RIGHT),
    BTN_OVER_LEFT       (FSkinProp.IMG_BTN_OVER_LEFT),
    BTN_OVER_CENTER     (FSkinProp.IMG_BTN_OVER_CENTER),
    BTN_OVER_RIGHT      (FSkinProp.IMG_BTN_OVER_RIGHT),
    BTN_DOWN_LEFT       (FSkinProp.IMG_BTN_DOWN_LEFT),
    BTN_DOWN_CENTER     (FSkinProp.IMG_BTN_DOWN_CENTER),
    BTN_DOWN_RIGHT      (FSkinProp.IMG_BTN_DOWN_RIGHT),
    BTN_FOCUS_LEFT      (FSkinProp.IMG_BTN_FOCUS_LEFT),
    BTN_FOCUS_CENTER    (FSkinProp.IMG_BTN_FOCUS_CENTER),
    BTN_FOCUS_RIGHT     (FSkinProp.IMG_BTN_FOCUS_RIGHT),
    BTN_TOGGLE_LEFT     (FSkinProp.IMG_BTN_TOGGLE_LEFT),
    BTN_TOGGLE_CENTER   (FSkinProp.IMG_BTN_TOGGLE_CENTER),
    BTN_TOGGLE_RIGHT    (FSkinProp.IMG_BTN_TOGGLE_RIGHT),
    BTN_DISABLED_LEFT   (FSkinProp.IMG_BTN_DISABLED_LEFT),
    BTN_DISABLED_CENTER (FSkinProp.IMG_BTN_DISABLED_CENTER),
    BTN_DISABLED_RIGHT  (FSkinProp.IMG_BTN_DISABLED_RIGHT),
    //adv_buttons
    ADV_BTN_UP_LEFT         (FSkinProp.IMG_ADV_BTN_UP_LEFT),
    ADV_BTN_UP_CENTER       (FSkinProp.IMG_ADV_BTN_UP_CENTER),
    ADV_BTN_UP_RIGHT        (FSkinProp.IMG_ADV_BTN_UP_RIGHT),
    ADV_BTN_OVER_LEFT       (FSkinProp.IMG_ADV_BTN_OVER_LEFT),
    ADV_BTN_OVER_CENTER     (FSkinProp.IMG_ADV_BTN_OVER_CENTER),
    ADV_BTN_OVER_RIGHT      (FSkinProp.IMG_ADV_BTN_OVER_RIGHT),
    ADV_BTN_DOWN_LEFT       (FSkinProp.IMG_ADV_BTN_DOWN_LEFT),
    ADV_BTN_DOWN_CENTER     (FSkinProp.IMG_ADV_BTN_DOWN_CENTER),
    ADV_BTN_DOWN_RIGHT      (FSkinProp.IMG_ADV_BTN_DOWN_RIGHT),
    ADV_BTN_FOCUS_LEFT      (FSkinProp.IMG_ADV_BTN_FOCUS_LEFT),
    ADV_BTN_FOCUS_CENTER    (FSkinProp.IMG_ADV_BTN_FOCUS_CENTER),
    ADV_BTN_FOCUS_RIGHT     (FSkinProp.IMG_ADV_BTN_FOCUS_RIGHT),
    ADV_BTN_TOGGLE_LEFT     (FSkinProp.IMG_ADV_BTN_TOGGLE_LEFT),
    ADV_BTN_TOGGLE_CENTER   (FSkinProp.IMG_ADV_BTN_TOGGLE_CENTER),
    ADV_BTN_TOGGLE_RIGHT    (FSkinProp.IMG_ADV_BTN_TOGGLE_RIGHT),
    ADV_BTN_DISABLED_LEFT   (FSkinProp.IMG_ADV_BTN_DISABLED_LEFT),
    ADV_BTN_DISABLED_CENTER (FSkinProp.IMG_ADV_BTN_DISABLED_CENTER),
    ADV_BTN_DISABLED_RIGHT  (FSkinProp.IMG_ADV_BTN_DISABLED_RIGHT),
    //Hdbuttons
    HDBTN_START_UP        (FSkinProp.IMG_HDBTN_START_UP),
    HDBTN_START_OVER      (FSkinProp.IMG_HDBTN_START_OVER),
    HDBTN_START_DOWN      (FSkinProp.IMG_HDBTN_START_DOWN),
    HDBTN_UP_LEFT         (FSkinProp.IMG_HDBTN_UP_LEFT),
    HDBTN_UP_CENTER       (FSkinProp.IMG_HDBTN_UP_CENTER),
    HDBTN_UP_RIGHT        (FSkinProp.IMG_HDBTN_UP_RIGHT),
    HDBTN_OVER_LEFT       (FSkinProp.IMG_HDBTN_OVER_LEFT),
    HDBTN_OVER_CENTER     (FSkinProp.IMG_HDBTN_OVER_CENTER),
    HDBTN_OVER_RIGHT      (FSkinProp.IMG_HDBTN_OVER_RIGHT),
    HDBTN_DOWN_LEFT       (FSkinProp.IMG_HDBTN_DOWN_LEFT),
    HDBTN_DOWN_CENTER     (FSkinProp.IMG_HDBTN_DOWN_CENTER),
    HDBTN_DOWN_RIGHT      (FSkinProp.IMG_HDBTN_DOWN_RIGHT),
    HDBTN_FOCUS_LEFT      (FSkinProp.IMG_HDBTN_FOCUS_LEFT),
    HDBTN_FOCUS_CENTER    (FSkinProp.IMG_HDBTN_FOCUS_CENTER),
    HDBTN_FOCUS_RIGHT     (FSkinProp.IMG_HDBTN_FOCUS_RIGHT),
    HDBTN_TOGGLE_LEFT     (FSkinProp.IMG_HDBTN_TOGGLE_LEFT),
    HDBTN_TOGGLE_CENTER   (FSkinProp.IMG_HDBTN_TOGGLE_CENTER),
    HDBTN_TOGGLE_RIGHT    (FSkinProp.IMG_HDBTN_TOGGLE_RIGHT),
    HDBTN_DISABLED_LEFT   (FSkinProp.IMG_HDBTN_DISABLED_LEFT),
    HDBTN_DISABLED_CENTER (FSkinProp.IMG_HDBTN_DISABLED_CENTER),
    HDBTN_DISABLED_RIGHT  (FSkinProp.IMG_HDBTN_DISABLED_RIGHT),

    //Foils
    FOIL_01     (FSkinProp.FOIL_01),
    FOIL_02     (FSkinProp.FOIL_02),
    FOIL_03     (FSkinProp.FOIL_03),
    FOIL_04     (FSkinProp.FOIL_04),
    FOIL_05     (FSkinProp.FOIL_05),
    FOIL_06     (FSkinProp.FOIL_06),
    FOIL_07     (FSkinProp.FOIL_07),
    FOIL_08     (FSkinProp.FOIL_08),
    FOIL_09     (FSkinProp.FOIL_09),
    FOIL_10     (FSkinProp.FOIL_10),

    //Old Foils
    FOIL_11     (FSkinProp.FOIL_11),
    FOIL_12     (FSkinProp.FOIL_12),
    FOIL_13     (FSkinProp.FOIL_13),
    FOIL_14     (FSkinProp.FOIL_14),
    FOIL_15     (FSkinProp.FOIL_15),
    FOIL_16     (FSkinProp.FOIL_16),
    FOIL_17     (FSkinProp.FOIL_17),
    FOIL_18     (FSkinProp.FOIL_18),
    FOIL_19     (FSkinProp.FOIL_19),
    FOIL_20     (FSkinProp.FOIL_20),

    //COMMANDER
    IMG_ABILITY_COMMANDER      (FSkinProp.IMG_ABILITY_COMMANDER),
    IMG_ABILITY_RINGBEARER     (FSkinProp.IMG_ABILITY_RINGBEARER),
    //ANNIHILATOR
    IMG_ABILITY_ANNIHILATOR    (FSkinProp.IMG_ABILITY_ANNIHILATOR),
    //TOXIC
    IMG_ABILITY_TOXIC          (FSkinProp.IMG_ABILITY_TOXIC),
    //ABILITY ICONS
    IMG_ABILITY_DEATHTOUCH     (FSkinProp.IMG_ABILITY_DEATHTOUCH),
    IMG_ABILITY_DEFENDER       (FSkinProp.IMG_ABILITY_DEFENDER),
    IMG_ABILITY_DOUBLE_STRIKE  (FSkinProp.IMG_ABILITY_DOUBLE_STRIKE),
    IMG_ABILITY_EXALTED        (FSkinProp.IMG_ABILITY_EXALTED),
    IMG_ABILITY_FIRST_STRIKE   (FSkinProp.IMG_ABILITY_FIRST_STRIKE),
    IMG_ABILITY_FEAR           (FSkinProp.IMG_ABILITY_FEAR),
    IMG_ABILITY_FLASH          (FSkinProp.IMG_ABILITY_FLASH),
    IMG_ABILITY_FLYING         (FSkinProp.IMG_ABILITY_FLYING),
    IMG_ABILITY_HASTE          (FSkinProp.IMG_ABILITY_HASTE),
    IMG_ABILITY_HEXPROOF       (FSkinProp.IMG_ABILITY_HEXPROOF),
    IMG_ABILITY_HORSEMANSHIP   (FSkinProp.IMG_ABILITY_HORSEMANSHIP),
    IMG_ABILITY_INDESTRUCTIBLE (FSkinProp.IMG_ABILITY_INDESTRUCTIBLE),
    IMG_ABILITY_INTIMIDATE     (FSkinProp.IMG_ABILITY_INTIMIDATE),
    IMG_ABILITY_LANDWALK       (FSkinProp.IMG_ABILITY_LANDWALK),
    IMG_ABILITY_LIFELINK       (FSkinProp.IMG_ABILITY_LIFELINK),
    IMG_ABILITY_MENACE         (FSkinProp.IMG_ABILITY_MENACE),
    IMG_ABILITY_REACH          (FSkinProp.IMG_ABILITY_REACH),
    IMG_ABILITY_SHADOW         (FSkinProp.IMG_ABILITY_SHADOW),
    IMG_ABILITY_SHROUD         (FSkinProp.IMG_ABILITY_SHROUD),
    IMG_ABILITY_TRAMPLE        (FSkinProp.IMG_ABILITY_TRAMPLE),
    IMG_ABILITY_WARD           (FSkinProp.IMG_ABILITY_WARD),
    IMG_ABILITY_WITHER         (FSkinProp.IMG_ABILITY_WITHER),
    IMG_ABILITY_VIGILANCE      (FSkinProp.IMG_ABILITY_VIGILANCE),
    //HEXPROOF FROM
    IMG_ABILITY_HEXPROOF_R       (FSkinProp.IMG_ABILITY_HEXPROOF_R),
    IMG_ABILITY_HEXPROOF_G       (FSkinProp.IMG_ABILITY_HEXPROOF_G),
    IMG_ABILITY_HEXPROOF_B       (FSkinProp.IMG_ABILITY_HEXPROOF_B),
    IMG_ABILITY_HEXPROOF_U       (FSkinProp.IMG_ABILITY_HEXPROOF_U),
    IMG_ABILITY_HEXPROOF_W       (FSkinProp.IMG_ABILITY_HEXPROOF_W),
    IMG_ABILITY_HEXPROOF_C       (FSkinProp.IMG_ABILITY_HEXPROOF_C),
    IMG_ABILITY_HEXPROOF_UB      (FSkinProp.IMG_ABILITY_HEXPROOF_UB),
    //token icon
    IMG_ABILITY_TOKEN            (FSkinProp.IMG_ABILITY_TOKEN),
    //border
    IMG_BORDER_BLACK            (FSkinProp.IMG_BORDER_BLACK),
    IMG_BORDER_WHITE            (FSkinProp.IMG_BORDER_WHITE),
    //PROTECT ICONS
    IMG_ABILITY_PROTECT_ALL           (FSkinProp.IMG_ABILITY_PROTECT_ALL),
    IMG_ABILITY_PROTECT_B             (FSkinProp.IMG_ABILITY_PROTECT_B),
    IMG_ABILITY_PROTECT_BU            (FSkinProp.IMG_ABILITY_PROTECT_BU),
    IMG_ABILITY_PROTECT_BW            (FSkinProp.IMG_ABILITY_PROTECT_BW),
    IMG_ABILITY_PROTECT_COLOREDSPELLS (FSkinProp.IMG_ABILITY_PROTECT_COLOREDSPELLS),
    IMG_ABILITY_PROTECT_G             (FSkinProp.IMG_ABILITY_PROTECT_G),
    IMG_ABILITY_PROTECT_GB            (FSkinProp.IMG_ABILITY_PROTECT_GB),
    IMG_ABILITY_PROTECT_GU            (FSkinProp.IMG_ABILITY_PROTECT_GU),
    IMG_ABILITY_PROTECT_GW            (FSkinProp.IMG_ABILITY_PROTECT_GW),
    IMG_ABILITY_PROTECT_GENERIC       (FSkinProp.IMG_ABILITY_PROTECT_GENERIC),
    IMG_ABILITY_PROTECT_R             (FSkinProp.IMG_ABILITY_PROTECT_R),
    IMG_ABILITY_PROTECT_RB            (FSkinProp.IMG_ABILITY_PROTECT_RB),
    IMG_ABILITY_PROTECT_RG            (FSkinProp.IMG_ABILITY_PROTECT_RG),
    IMG_ABILITY_PROTECT_RU            (FSkinProp.IMG_ABILITY_PROTECT_RU),
    IMG_ABILITY_PROTECT_RW            (FSkinProp.IMG_ABILITY_PROTECT_RW),
    IMG_ABILITY_PROTECT_U             (FSkinProp.IMG_ABILITY_PROTECT_U),
    IMG_ABILITY_PROTECT_UW            (FSkinProp.IMG_ABILITY_PROTECT_UW),
    IMG_ABILITY_PROTECT_W             (FSkinProp.IMG_ABILITY_PROTECT_W);

    private final FSkinImageImpl impl;

    FSkinImage(FSkinProp skinProp) {
        impl = new FSkinImageImpl(skinProp);
        FSkin.getImages().put(skinProp, this);
    }

    @Override
    public void load(Pixmap preferredIcons) {
        impl.load(preferredIcons);
    }

    @Override
    public float getWidth() {
        return impl.getWidth();
    }

    @Override
    public float getHeight() {
        return impl.getHeight();
    }

    @Override
    public TextureRegion getTextureRegion() {
        return impl.getTextureRegion();
    }

    @Override
    public float getNearestHQWidth(float baseWidth) {
        return impl.getNearestHQWidth(baseWidth);
    }

    @Override
    public float getNearestHQHeight(float baseHeight) {
        return impl.getNearestHQHeight(baseHeight);
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        impl.draw(g, x, y, w, h);
    }
}
