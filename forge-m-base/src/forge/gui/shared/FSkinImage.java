package forge.gui.shared;

/** Properties of various components that make up the skin.
 * This interface allows all enums to be under the same roof.
 * It also enforces a getter for coordinate locations in sprites. */
public enum FSkinImage {
    //Zones
    HAND        (280, 40, 40, 40),
    LIBRARY     (280, 0, 40, 40),
    EXILE       (320, 40, 40, 40),
    FLASHBACK   (280, 80, 40, 40),
    GRAVEYARD   (320, 0, 40, 40),
    POISON      (320, 80, 40, 40),

    //Mana symbols
    MANA_COLORLESS (440, 160, 40, 40),
    MANA_B         (360, 160, 40, 40),
    MANA_R         (400, 160, 40, 40),
    MANA_U         (360, 200, 40, 40),
    MANA_G         (400, 200, 40, 40),
    MANA_W         (440, 200, 40, 40),
    MANA_2B        (360, 400, 40, 40),
    MANA_2G        (400, 400, 40, 40),
    MANA_2R        (440, 400, 40, 40),
    MANA_2U        (440, 360, 40, 40),
    MANA_2W        (400, 360, 40, 40),
    MANA_HYBRID_BG (360, 240, 40, 40),
    MANA_HYBRID_BR (400, 240, 40, 40),
    MANA_HYBRID_GU (360, 280, 40, 40),
    MANA_HYBRID_GW (440, 280, 40, 40),
    MANA_HYBRID_RG (360, 320, 40, 40),
    MANA_HYBRID_RW (400, 320, 40, 40),
    MANA_HYBRID_UB (440, 240, 40, 40),
    MANA_HYBRID_UR (440, 320, 40, 40),
    MANA_HYBRID_WB (400, 280, 40, 40),
    MANA_HYBRID_WU (360, 360, 40, 40),
    MANA_PHRYX_U   (320, 200, 40, 40),
    MANA_PHRYX_W   (320, 240, 40, 40),
    MANA_PHRYX_R   (320, 280, 40, 40),
    MANA_PHRYX_G   (320, 320, 40, 40),
    MANA_PHRYX_B   (320, 360, 40, 40),
    MANA_SNOW      (320, 160, 40, 40),
    MANA_0         (640, 200, 20, 20),
    MANA_1         (660, 200, 20, 20),
    MANA_2         (640, 220, 20, 20),
    MANA_3         (660, 220, 20, 20),
    MANA_4         (640, 240, 20, 20),
    MANA_5         (660, 240, 20, 20),
    MANA_6         (640, 260, 20, 20),
    MANA_7         (660, 260, 20, 20),
    MANA_8         (640, 280, 20, 20),
    MANA_9         (660, 280, 20, 20),
    MANA_10        (640, 300, 20, 20),
    MANA_11        (660, 300, 20, 20),
    MANA_12        (640, 320, 20, 20),
    MANA_13        (660, 320, 20, 20),
    MANA_14        (640, 340, 20, 20),
    MANA_15        (660, 340, 20, 20),
    MANA_16        (640, 360, 20, 20),
    MANA_17        (660, 360, 20, 20),
    MANA_18        (640, 380, 20, 20),
    MANA_19        (660, 380, 20, 20),
    MANA_20        (640, 400, 20, 20),
    MANA_X         (660, 400, 20, 20),
    MANA_Y         (640, 420, 20, 20),
    MANA_Z         (660, 420, 20, 20),

    //Gameplay
    TAP             (640, 440, 20, 20),
    UNTAP           (660, 440, 20, 20),
    CHAOS           (320, 400, 40, 40),
    SLASH           (660, 400, 10, 13),
    ATTACK          (160, 320, 80, 80),
    DEFEND          (160, 400, 80, 80),
    SUMMONSICK      (240, 400, 80, 80),
    PHASING         (240, 320, 80, 80),
    COSTRESERVED    (240, 240, 80, 80),
    COUNTERS1       (0, 320, 80, 80),
    COUNTERS2       (0, 400, 80, 80),
    COUNTERS3       (80, 320, 80, 80),
    COUNTERS_MULTI  (80, 400, 80, 80),

    //Foils
    FOIL_01     (0, 0, 400, 570),
    FOIL_02     (400, 0, 400, 570),
    FOIL_03     (0, 570, 400, 570),
    FOIL_04     (400, 570, 400, 570),
    FOIL_05     (0, 1140, 400, 570),
    FOIL_06     (400, 1140, 400, 570),
    FOIL_07     (0, 1710, 400, 570),
    FOIL_08     (400, 1710, 400, 570),
    FOIL_09     (0, 2280, 400, 570),
    FOIL_10     (400, 2280, 400, 570),

    //Old Foils
    FOIL_11     (0, 0, 400, 570),
    FOIL_12     (400, 0, 400, 570),
    FOIL_13     (0, 570, 400, 570),
    FOIL_14     (400, 570, 400, 570),
    FOIL_15     (0, 1140, 400, 570),
    FOIL_16     (400, 1140, 400, 570),
    FOIL_17     (0, 1710, 400, 570),
    FOIL_18     (400, 1710, 400, 570),
    FOIL_19     (0, 2280, 400, 570),
    FOIL_20     (400, 2280, 400, 570),

    //Dock Icons
    SHORTCUTS    (160, 640, 80, 80),
    SETTINGS     (80, 640, 80, 80),
    ENDTURN      (320, 640, 80, 80),
    CONCEDE      (240, 640, 80, 80),
    REVERTLAYOUT (400, 720, 80, 80),
    OPENLAYOUT   (0, 800, 80, 80),
    SAVELAYOUT   (80, 800, 80, 80),
    DECKLIST     (400, 640, 80, 80),
    ALPHASTRIKE  (160, 800, 80, 80),
    ARCSOFF      (240, 800, 80, 80),
    ARCSON       (320, 800, 80, 80),
    ARCSHOVER    (400, 800, 80, 80),

    //Quest Icons
    QUEST_ZEP         (0, 480, 80, 80),
    QUEST_GEAR        (80, 480, 80, 80),
    QUEST_GOLD        (160, 480, 80, 80),
    QUEST_ELIXIR      (240, 480, 80, 80),
    QUEST_BOOK        (320, 480, 80, 80),
    QUEST_BOTTLES     (400, 480, 80, 80),
    QUEST_BOX         (480, 480, 80, 80),
    QUEST_COIN        (560, 480, 80, 80),
    QUEST_CHARM       (480, 800, 80, 80),

    QUEST_FOX         (0, 560, 80, 80),
    QUEST_LEAF        (80, 560, 80, 80),
    QUEST_LIFE        (160, 560, 80, 80),
    QUEST_COINSTACK   (240, 560, 80, 80),
    QUEST_MAP         (320, 560, 80, 80),
    QUEST_NOTES       (400, 560, 80, 80),
    QUEST_HEART       (480, 560, 80, 80),
    QUEST_BREW        (560, 560, 80, 80),
    QUEST_STAKES      (400, 560, 80, 80),

    QUEST_MINUS       (560, 640, 80, 80),
    QUEST_PLUS        (480, 640, 80, 80),
    QUEST_PLUSPLUS    (480, 720, 80, 80),

    //Interface icons
    QUESTION        (560, 800, 32, 32),
    INFORMATION     (592, 800, 32, 32),
    WARNING         (560, 832, 32, 32),
    ERROR           (592, 832, 32, 32),
    DELETE          (640, 480, 20, 20),
    DELETE_OVER     (660, 480, 20, 20),
    EDIT            (640, 500, 20, 20),
    EDIT_OVER       (660, 500, 20, 20),
    OPEN            (660, 520, 20, 20),
    MINUS           (660, 620, 20, 20),
    NEW             (660, 540, 20, 20),
    PLUS            (660, 600, 20, 20),
    PRINT           (660, 640, 20, 20),
    SAVE            (660, 560, 20, 20),
    SAVEAS          (660, 580, 20, 20),
    CLOSE           (640, 640, 20, 20),
    LIST            (640, 660, 20, 20),
    CARD_IMAGE      (660, 660, 20, 20),
    UNKNOWN         (0, 720, 80, 80),
    LOGO            (480, 0, 200, 200),
    FLIPCARD        (400, 0, 80, 120),
    FAVICON         (0, 640, 80, 80),

    //Layout images
    HANDLE  (320, 450, 80, 20),
    CUR_L   (564, 724, 32, 32),
    CUR_R   (564, 764, 32, 32),
    CUR_T   (604, 724, 32, 32),
    CUR_B   (604, 764, 32, 32),
    CUR_TAB (644, 764, 32, 32),

    //Editor images
    STAR_OUTINE     (640, 460, 20, 20),
    STAR_FILLED     (660, 460, 20, 20),
    ARTIFACT        (280, 720, 40, 40),
    CREATURE        (240, 720, 40, 40),
    ENCHANTMENT     (320, 720, 40, 40),
    INSTANT         (360, 720, 40, 40),
    LAND            (120, 720, 40, 40),
    MULTI           (80, 720, 40, 40),
    PLANESWALKER    (200, 720, 40, 40),
    PACK            (80, 760, 40, 40),
    SORCERY         (160, 720, 40, 40),

    //Buttons
    BTN_START_UP        (480, 200, 160, 80),
    BTN_START_OVER      (480, 280, 160, 80),
    BTN_START_DOWN      (480, 360, 160, 80),

    BTN_UP_LEFT         (80, 0, 40, 40),
    BTN_UP_CENTER       (120, 0, 1, 40),
    BTN_UP_RIGHT        (160, 0, 40, 40),

    BTN_OVER_LEFT       (80, 40, 40, 40),
    BTN_OVER_CENTER     (120, 40, 1, 40),
    BTN_OVER_RIGHT      (160, 40, 40, 40),

    BTN_DOWN_LEFT       (80, 80, 40, 40),
    BTN_DOWN_CENTER     (120, 80, 1, 40),
    BTN_DOWN_RIGHT      (160, 80, 40, 40),

    BTN_FOCUS_LEFT      (80, 120, 40, 40),
    BTN_FOCUS_CENTER    (120, 120, 1, 40),
    BTN_FOCUS_RIGHT     (160, 120, 40, 40),

    BTN_TOGGLE_LEFT     (80, 160, 40, 40),
    BTN_TOGGLE_CENTER   (120, 160, 1, 40),
    BTN_TOGGLE_RIGHT    (160, 160, 40, 40),

    BTN_DISABLED_LEFT   (80, 200, 40, 40),
    BTN_DISABLED_CENTER (120, 200, 1, 40),
    BTN_DISABLED_RIGHT  (160, 200, 40, 40);

    private int x, y, w, h;

    FSkinImage(int x0, int y0, int w0, int h0) {
        x = x0;
        y = y0;
        w = w0;
        h = h0;
    }
}