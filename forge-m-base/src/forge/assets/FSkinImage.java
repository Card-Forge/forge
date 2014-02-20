package forge.assets;

/** Properties of various components that make up the skin.
 * This interface allows all enums to be under the same roof.
 * It also enforces a getter for coordinate locations in sprites. */
public enum FSkinImage {
    //Backgrounds
    BG_SPLASH (0, 0, 0, -100, SourceFile.SPLASH), //treat 0 and negative as offset from full width/height
    BG_TEXTURE (0, 0, 0, 0, SourceFile.TEXTURE),
    BG_MATCH (0, 0, 0, 0, SourceFile.MATCH),

    //Zones
    HAND        (280, 40, 40, 40, SourceFile.ICONS),
    LIBRARY     (280, 0, 40, 40, SourceFile.ICONS),
    EXILE       (320, 40, 40, 40, SourceFile.ICONS),
    FLASHBACK   (280, 80, 40, 40, SourceFile.ICONS),
    GRAVEYARD   (320, 0, 40, 40, SourceFile.ICONS),
    POISON      (320, 80, 40, 40, SourceFile.ICONS),

    //Mana symbols
    MANA_COLORLESS (440, 160, 40, 40, SourceFile.ICONS),
    MANA_B         (360, 160, 40, 40, SourceFile.ICONS),
    MANA_R         (400, 160, 40, 40, SourceFile.ICONS),
    MANA_U         (360, 200, 40, 40, SourceFile.ICONS),
    MANA_G         (400, 200, 40, 40, SourceFile.ICONS),
    MANA_W         (440, 200, 40, 40, SourceFile.ICONS),
    MANA_2B        (360, 400, 40, 40, SourceFile.ICONS),
    MANA_2G        (400, 400, 40, 40, SourceFile.ICONS),
    MANA_2R        (440, 400, 40, 40, SourceFile.ICONS),
    MANA_2U        (440, 360, 40, 40, SourceFile.ICONS),
    MANA_2W        (400, 360, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_BG (360, 240, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_BR (400, 240, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_GU (360, 280, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_GW (440, 280, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_RG (360, 320, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_RW (400, 320, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_UB (440, 240, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_UR (440, 320, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_WB (400, 280, 40, 40, SourceFile.ICONS),
    MANA_HYBRID_WU (360, 360, 40, 40, SourceFile.ICONS),
    MANA_PHRYX_U   (320, 200, 40, 40, SourceFile.ICONS),
    MANA_PHRYX_W   (320, 240, 40, 40, SourceFile.ICONS),
    MANA_PHRYX_R   (320, 280, 40, 40, SourceFile.ICONS),
    MANA_PHRYX_G   (320, 320, 40, 40, SourceFile.ICONS),
    MANA_PHRYX_B   (320, 360, 40, 40, SourceFile.ICONS),
    MANA_SNOW      (320, 160, 40, 40, SourceFile.ICONS),
    MANA_0         (640, 200, 20, 20, SourceFile.ICONS),
    MANA_1         (660, 200, 20, 20, SourceFile.ICONS),
    MANA_2         (640, 220, 20, 20, SourceFile.ICONS),
    MANA_3         (660, 220, 20, 20, SourceFile.ICONS),
    MANA_4         (640, 240, 20, 20, SourceFile.ICONS),
    MANA_5         (660, 240, 20, 20, SourceFile.ICONS),
    MANA_6         (640, 260, 20, 20, SourceFile.ICONS),
    MANA_7         (660, 260, 20, 20, SourceFile.ICONS),
    MANA_8         (640, 280, 20, 20, SourceFile.ICONS),
    MANA_9         (660, 280, 20, 20, SourceFile.ICONS),
    MANA_10        (640, 300, 20, 20, SourceFile.ICONS),
    MANA_11        (660, 300, 20, 20, SourceFile.ICONS),
    MANA_12        (640, 320, 20, 20, SourceFile.ICONS),
    MANA_13        (660, 320, 20, 20, SourceFile.ICONS),
    MANA_14        (640, 340, 20, 20, SourceFile.ICONS),
    MANA_15        (660, 340, 20, 20, SourceFile.ICONS),
    MANA_16        (640, 360, 20, 20, SourceFile.ICONS),
    MANA_17        (660, 360, 20, 20, SourceFile.ICONS),
    MANA_18        (640, 380, 20, 20, SourceFile.ICONS),
    MANA_19        (660, 380, 20, 20, SourceFile.ICONS),
    MANA_20        (640, 400, 20, 20, SourceFile.ICONS),
    MANA_X         (660, 400, 20, 20, SourceFile.ICONS),
    MANA_Y         (640, 420, 20, 20, SourceFile.ICONS),
    MANA_Z         (660, 420, 20, 20, SourceFile.ICONS),

    //Gameplay
    TAP             (640, 440, 20, 20, SourceFile.ICONS),
    UNTAP           (660, 440, 20, 20, SourceFile.ICONS),
    CHAOS           (320, 400, 40, 40, SourceFile.ICONS),
    SLASH           (660, 400, 10, 13, SourceFile.ICONS),
    ATTACK          (160, 320, 80, 80, SourceFile.ICONS),
    DEFEND          (160, 400, 80, 80, SourceFile.ICONS),
    SUMMONSICK      (240, 400, 80, 80, SourceFile.ICONS),
    PHASING         (240, 320, 80, 80, SourceFile.ICONS),
    COSTRESERVED    (240, 240, 80, 80, SourceFile.ICONS),
    COUNTERS1       (0, 320, 80, 80, SourceFile.ICONS),
    COUNTERS2       (0, 400, 80, 80, SourceFile.ICONS),
    COUNTERS3       (80, 320, 80, 80, SourceFile.ICONS),
    COUNTERS_MULTI  (80, 400, 80, 80, SourceFile.ICONS),

    //Dock Icons
    SHORTCUTS    (160, 640, 80, 80, SourceFile.ICONS),
    SETTINGS     (80, 640, 80, 80, SourceFile.ICONS),
    ENDTURN      (320, 640, 80, 80, SourceFile.ICONS),
    CONCEDE      (240, 640, 80, 80, SourceFile.ICONS),
    REVERTLAYOUT (400, 720, 80, 80, SourceFile.ICONS),
    OPENLAYOUT   (0, 800, 80, 80, SourceFile.ICONS),
    SAVELAYOUT   (80, 800, 80, 80, SourceFile.ICONS),
    DECKLIST     (400, 640, 80, 80, SourceFile.ICONS),
    ALPHASTRIKE  (160, 800, 80, 80, SourceFile.ICONS),
    ARCSOFF      (240, 800, 80, 80, SourceFile.ICONS),
    ARCSON       (320, 800, 80, 80, SourceFile.ICONS),
    ARCSHOVER    (400, 800, 80, 80, SourceFile.ICONS),

    //Quest Icons
    QUEST_ZEP         (0, 480, 80, 80, SourceFile.ICONS),
    QUEST_GEAR        (80, 480, 80, 80, SourceFile.ICONS),
    QUEST_GOLD        (160, 480, 80, 80, SourceFile.ICONS),
    QUEST_ELIXIR      (240, 480, 80, 80, SourceFile.ICONS),
    QUEST_BOOK        (320, 480, 80, 80, SourceFile.ICONS),
    QUEST_BOTTLES     (400, 480, 80, 80, SourceFile.ICONS),
    QUEST_BOX         (480, 480, 80, 80, SourceFile.ICONS),
    QUEST_COIN        (560, 480, 80, 80, SourceFile.ICONS),
    QUEST_CHARM       (480, 800, 80, 80, SourceFile.ICONS),

    QUEST_FOX         (0, 560, 80, 80, SourceFile.ICONS),
    QUEST_LEAF        (80, 560, 80, 80, SourceFile.ICONS),
    QUEST_LIFE        (160, 560, 80, 80, SourceFile.ICONS),
    QUEST_COINSTACK   (240, 560, 80, 80, SourceFile.ICONS),
    QUEST_MAP         (320, 560, 80, 80, SourceFile.ICONS),
    QUEST_NOTES       (400, 560, 80, 80, SourceFile.ICONS),
    QUEST_HEART       (480, 560, 80, 80, SourceFile.ICONS),
    QUEST_BREW        (560, 560, 80, 80, SourceFile.ICONS),
    QUEST_STAKES      (400, 560, 80, 80, SourceFile.ICONS),

    QUEST_MINUS       (560, 640, 80, 80, SourceFile.ICONS),
    QUEST_PLUS        (480, 640, 80, 80, SourceFile.ICONS),
    QUEST_PLUSPLUS    (480, 720, 80, 80, SourceFile.ICONS),

    //Interface icons
    QUESTION        (560, 800, 32, 32, SourceFile.ICONS),
    INFORMATION     (592, 800, 32, 32, SourceFile.ICONS),
    WARNING         (560, 832, 32, 32, SourceFile.ICONS),
    ERROR           (592, 832, 32, 32, SourceFile.ICONS),
    DELETE          (640, 480, 20, 20, SourceFile.ICONS),
    DELETE_OVER     (660, 480, 20, 20, SourceFile.ICONS),
    EDIT            (640, 500, 20, 20, SourceFile.ICONS),
    EDIT_OVER       (660, 500, 20, 20, SourceFile.ICONS),
    OPEN            (660, 520, 20, 20, SourceFile.ICONS),
    MINUS           (660, 620, 20, 20, SourceFile.ICONS),
    NEW             (660, 540, 20, 20, SourceFile.ICONS),
    PLUS            (660, 600, 20, 20, SourceFile.ICONS),
    PRINT           (660, 640, 20, 20, SourceFile.ICONS),
    SAVE            (660, 560, 20, 20, SourceFile.ICONS),
    SAVEAS          (660, 580, 20, 20, SourceFile.ICONS),
    CLOSE           (640, 640, 20, 20, SourceFile.ICONS),
    LIST            (640, 660, 20, 20, SourceFile.ICONS),
    CARD_IMAGE      (660, 660, 20, 20, SourceFile.ICONS),
    UNKNOWN         (0, 720, 80, 80, SourceFile.ICONS),
    LOGO            (480, 0, 200, 200, SourceFile.ICONS),
    FLIPCARD        (400, 0, 80, 120, SourceFile.ICONS),
    FAVICON         (0, 640, 80, 80, SourceFile.ICONS),

    //Layout images
    HANDLE  (320, 450, 80, 20, SourceFile.ICONS),
    CUR_L   (564, 724, 32, 32, SourceFile.ICONS),
    CUR_R   (564, 764, 32, 32, SourceFile.ICONS),
    CUR_T   (604, 724, 32, 32, SourceFile.ICONS),
    CUR_B   (604, 764, 32, 32, SourceFile.ICONS),
    CUR_TAB (644, 764, 32, 32, SourceFile.ICONS),

    //Editor images
    STAR_OUTINE     (640, 460, 20, 20, SourceFile.ICONS),
    STAR_FILLED     (660, 460, 20, 20, SourceFile.ICONS),
    ARTIFACT        (280, 720, 40, 40, SourceFile.ICONS),
    CREATURE        (240, 720, 40, 40, SourceFile.ICONS),
    ENCHANTMENT     (320, 720, 40, 40, SourceFile.ICONS),
    INSTANT         (360, 720, 40, 40, SourceFile.ICONS),
    LAND            (120, 720, 40, 40, SourceFile.ICONS),
    MULTI           (80, 720, 40, 40, SourceFile.ICONS),
    PLANESWALKER    (200, 720, 40, 40, SourceFile.ICONS),
    PACK            (80, 760, 40, 40, SourceFile.ICONS),
    SORCERY         (160, 720, 40, 40, SourceFile.ICONS),

    //Buttons
    BTN_START_UP        (480, 200, 160, 80, SourceFile.ICONS),
    BTN_START_OVER      (480, 280, 160, 80, SourceFile.ICONS),
    BTN_START_DOWN      (480, 360, 160, 80, SourceFile.ICONS),

    BTN_UP_LEFT         (80, 0, 40, 40, SourceFile.ICONS),
    BTN_UP_CENTER       (120, 0, 1, 40, SourceFile.ICONS),
    BTN_UP_RIGHT        (160, 0, 40, 40, SourceFile.ICONS),

    BTN_OVER_LEFT       (80, 40, 40, 40, SourceFile.ICONS),
    BTN_OVER_CENTER     (120, 40, 1, 40, SourceFile.ICONS),
    BTN_OVER_RIGHT      (160, 40, 40, 40, SourceFile.ICONS),

    BTN_DOWN_LEFT       (80, 80, 40, 40, SourceFile.ICONS),
    BTN_DOWN_CENTER     (120, 80, 1, 40, SourceFile.ICONS),
    BTN_DOWN_RIGHT      (160, 80, 40, 40, SourceFile.ICONS),

    BTN_FOCUS_LEFT      (80, 120, 40, 40, SourceFile.ICONS),
    BTN_FOCUS_CENTER    (120, 120, 1, 40, SourceFile.ICONS),
    BTN_FOCUS_RIGHT     (160, 120, 40, 40, SourceFile.ICONS),

    BTN_TOGGLE_LEFT     (80, 160, 40, 40, SourceFile.ICONS),
    BTN_TOGGLE_CENTER   (120, 160, 1, 40, SourceFile.ICONS),
    BTN_TOGGLE_RIGHT    (160, 160, 40, 40, SourceFile.ICONS),

    BTN_DISABLED_LEFT   (80, 200, 40, 40, SourceFile.ICONS),
    BTN_DISABLED_CENTER (120, 200, 1, 40, SourceFile.ICONS),
    BTN_DISABLED_RIGHT  (160, 200, 40, 40, SourceFile.ICONS),

    //Foils
    FOIL_01     (0, 0, 400, 570, SourceFile.FOILS),
    FOIL_02     (400, 0, 400, 570, SourceFile.FOILS),
    FOIL_03     (0, 570, 400, 570, SourceFile.FOILS),
    FOIL_04     (400, 570, 400, 570, SourceFile.FOILS),
    FOIL_05     (0, 1140, 400, 570, SourceFile.FOILS),
    FOIL_06     (400, 1140, 400, 570, SourceFile.FOILS),
    FOIL_07     (0, 1710, 400, 570, SourceFile.FOILS),
    FOIL_08     (400, 1710, 400, 570, SourceFile.FOILS),
    FOIL_09     (0, 2280, 400, 570, SourceFile.FOILS),
    FOIL_10     (400, 2280, 400, 570, SourceFile.FOILS),

    //Old Foils
    FOIL_11     (0, 0, 400, 570, SourceFile.OLD_FOILS),
    FOIL_12     (400, 0, 400, 570, SourceFile.OLD_FOILS),
    FOIL_13     (0, 570, 400, 570, SourceFile.OLD_FOILS),
    FOIL_14     (400, 570, 400, 570, SourceFile.OLD_FOILS),
    FOIL_15     (0, 1140, 400, 570, SourceFile.OLD_FOILS),
    FOIL_16     (400, 1140, 400, 570, SourceFile.OLD_FOILS),
    FOIL_17     (0, 1710, 400, 570, SourceFile.OLD_FOILS),
    FOIL_18     (400, 1710, 400, 570, SourceFile.OLD_FOILS),
    FOIL_19     (0, 2280, 400, 570, SourceFile.OLD_FOILS),
    FOIL_20     (400, 2280, 400, 570, SourceFile.OLD_FOILS);

    public enum SourceFile {
        ICONS("sprite_icons.png"),
        FOILS("sprite_foils.png"),
        OLD_FOILS("sprite_old_foils.png"),
        SPLASH("bg_splash.png"),
        MATCH("bg_match.jpg"),
        TEXTURE("bg_texture.jpg");

        private final String filename;

        SourceFile(String filename0) {
            filename = filename0;
        }

        public String getFilename() {
            return filename;
        }
    }

    private final int x, y, w, h;
    private final SourceFile sourceFile;

    FSkinImage(int x0, int y0, int w0, int h0, SourceFile sourceFile0) {
        x = x0;
        y = y0;
        w = w0;
        h = h0;
        sourceFile = sourceFile0;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth(int fullWidth) {
        if (w > 0) {
            return w;
        }
        return fullWidth + w;
    }

    public int getHeight(int fullHeight) {
        if (h > 0) {
            return h;
        }
        return fullHeight + h;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }
}