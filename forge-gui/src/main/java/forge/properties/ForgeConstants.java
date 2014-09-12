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

import java.util.Collections;
import java.util.Map;

public final class ForgeConstants {

    public static void init(final String assetsDir) {
        ASSETS_DIR            = assetsDir;
        PROFILE_FILE          = ASSETS_DIR + "forge.profile.properties";
        PROFILE_TEMPLATE_FILE = PROFILE_FILE + ".example";    

        RES_DIR   = ASSETS_DIR + "res/";
        LISTS_DIR = RES_DIR + "lists/";

        KEYWORD_LIST_FILE                     = LISTS_DIR + "NonStackingKWList.txt";
        TYPE_LIST_FILE                        = LISTS_DIR + "TypeLists.txt";
        IMAGE_LIST_TOKENS_FILE                = LISTS_DIR + "token-images.txt";
        IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE  = LISTS_DIR + "quest-opponent-icons.txt";
        IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE  = LISTS_DIR + "quest-pet-shop-icons.txt";
        IMAGE_LIST_QUEST_TOKENS_FILE          = LISTS_DIR + "quest-pet-token-images.txt";
        IMAGE_LIST_QUEST_BOOSTERS_FILE        = LISTS_DIR + "booster-images.txt";
        IMAGE_LIST_QUEST_FATPACKS_FILE        = LISTS_DIR + "fatpack-images.txt";
        IMAGE_LIST_QUEST_BOOSTERBOXES_FILE    = LISTS_DIR + "boosterbox-images.txt";
        IMAGE_LIST_QUEST_PRECONS_FILE         = LISTS_DIR + "precon-images.txt";
        IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE = LISTS_DIR + "tournamentpack-images.txt";

        CHANGES_FILE = ASSETS_DIR + "CHANGES.txt";
        LICENSE_FILE = ASSETS_DIR + "LICENSE.txt";
        README_FILE  = ASSETS_DIR + "README.txt";
        HOWTO_FILE   = RES_DIR + "howto.txt";

        DRAFT_DIR           = RES_DIR + "draft/";
        DRAFT_RANKINGS_FILE = DRAFT_DIR + "rankings.txt";
        SEALED_DIR          = RES_DIR + "sealed/";
        CARD_DATA_DIR       = RES_DIR + "cardsfolder/";
        EDITIONS_DIR        = RES_DIR + "editions/";
        BLOCK_DATA_DIR      = RES_DIR + "blockdata/";
        DECK_CUBE_DIR       = RES_DIR + "cube/";
        AI_PROFILE_DIR      = RES_DIR + "ai/";
        SOUND_DIR           = RES_DIR + "sound/";
        MUSIC_DIR           = RES_DIR + "music/";
        LANG_DIR            = RES_DIR + "languages/";
        EFFECTS_DIR         = RES_DIR + "effects/";

        QUEST_DIR              = RES_DIR + "quest/";
        QUEST_WORLD_DIR        = QUEST_DIR + "world/";
        QUEST_PRECON_DIR       = QUEST_DIR + "precons/";
        PRICES_BOOSTER_FILE    = QUEST_DIR + "booster-prices.txt";
        BAZAAR_DIR             = QUEST_DIR + "bazaar/";
        BAZAAR_INDEX_FILE      = BAZAAR_DIR + "index.xml";
        DEFAULT_DUELS_DIR      = QUEST_DIR + "duels";
        DEFAULT_CHALLENGES_DIR = QUEST_DIR + "challenges";
        THEMES_DIR             = QUEST_DIR + "themes";

        SKINS_DIR         = RES_DIR + "skins/";
        DEFAULT_SKINS_DIR = SKINS_DIR + "default/";

        ForgeProfileProperties.load();
        USER_DIR               = ForgeProfileProperties.getUserDir();
        CACHE_DIR              = ForgeProfileProperties.getCacheDir();
        CACHE_CARD_PICS_DIR    = ForgeProfileProperties.getCardPicsDir();
        CACHE_CARD_PICS_SUBDIR = Collections.unmodifiableMap(ForgeProfileProperties.getCardPicsSubDirs());
        SERVER_PORT_NUMBER     = ForgeProfileProperties.getServerPort();

        USER_QUEST_DIR       = USER_DIR + "quest/";
        USER_PREFS_DIR       = USER_DIR + "preferences/";
        USER_GAMES_DIR       = USER_DIR + "games/";
        LOG_FILE             = USER_DIR + "forge.log";
        DECK_BASE_DIR        = USER_DIR + "decks/";
        DECK_CONSTRUCTED_DIR = DECK_BASE_DIR + "constructed/";
        DECK_DRAFT_DIR       = DECK_BASE_DIR + "draft/";
        DECK_WINSTON_DIR     = DECK_BASE_DIR + "winston/";
        DECK_SEALED_DIR      = DECK_BASE_DIR + "sealed/";
        DECK_SCHEME_DIR      = DECK_BASE_DIR + "scheme/";
        DECK_PLANE_DIR       = DECK_BASE_DIR + "planar/";
        DECK_COMMANDER_DIR   = DECK_BASE_DIR + "commander/";
        QUEST_SAVE_DIR       = USER_QUEST_DIR + "saves/";
        MAIN_PREFS_FILE      = USER_PREFS_DIR + "forge.preferences";
        CARD_PREFS_FILE      = USER_PREFS_DIR + "card.preferences";
        DECK_PREFS_FILE      = USER_PREFS_DIR + "deck.preferences";
        QUEST_PREFS_FILE     = USER_PREFS_DIR + "quest.preferences";
        ITEM_VIEW_PREFS_FILE = USER_PREFS_DIR + "item_view.preferences";
        ACHIEVEMENTS_FILE    = USER_PREFS_DIR + "achievements.xml";

        _DEFAULTS_DIR = RES_DIR + "defaults/";
        NO_CARD_FILE  = _DEFAULTS_DIR + "no_card.jpg";

        WINDOW_LAYOUT_FILE   = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "window.xml");
        MATCH_LAYOUT_FILE    = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "match.xml");
        WORKSHOP_LAYOUT_FILE = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "workshop.xml");
        EDITOR_LAYOUT_FILE   = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "editor.xml");
        GAUNTLET_DIR         = new FileLocation(_DEFAULTS_DIR, USER_DIR,       "gauntlet/");

        PICS_DIR                      = CACHE_DIR + "pics/";
        DB_DIR                        = CACHE_DIR + "db/";
        FONTS_DIR                     = CACHE_DIR + "fonts/";
        CACHE_TOKEN_PICS_DIR          = PICS_DIR + "tokens/";
        CACHE_ICON_PICS_DIR           = PICS_DIR + "icons/";
        CACHE_SYMBOLS_DIR             = PICS_DIR + "symbols/";
        CACHE_BOOSTER_PICS_DIR        = PICS_DIR + "boosters/";
        CACHE_FATPACK_PICS_DIR        = PICS_DIR + "fatpacks/";
        CACHE_BOOSTERBOX_PICS_DIR     = PICS_DIR + "boosterboxes/";
        CACHE_PRECON_PICS_DIR         = PICS_DIR + "precons/";
        CACHE_TOURNAMENTPACK_PICS_DIR = PICS_DIR + "tournamentpacks/";
        QUEST_CARD_PRICE_FILE         = DB_DIR + "all-prices.txt";;

        PROFILE_DIRS = new String[] {
            USER_DIR,
            CACHE_DIR,
            CACHE_CARD_PICS_DIR,
            USER_PREFS_DIR,
            GAUNTLET_DIR.userPrefLoc,
            DB_DIR,
            DECK_CONSTRUCTED_DIR,
            DECK_DRAFT_DIR,
            DECK_SEALED_DIR,
            DECK_SCHEME_DIR,
            DECK_PLANE_DIR,
            QUEST_SAVE_DIR,
            CACHE_TOKEN_PICS_DIR,
            CACHE_ICON_PICS_DIR,
            CACHE_BOOSTER_PICS_DIR,
            CACHE_FATPACK_PICS_DIR,
            CACHE_BOOSTERBOX_PICS_DIR,
            CACHE_PRECON_PICS_DIR,
            CACHE_TOURNAMENTPACK_PICS_DIR };
    }

    public static String ASSETS_DIR;
    public static String PROFILE_FILE;
    public static String PROFILE_TEMPLATE_FILE;

    public static String RES_DIR;
    public static String LISTS_DIR;
    public static String KEYWORD_LIST_FILE;
    public static String TYPE_LIST_FILE;
    public static String IMAGE_LIST_TOKENS_FILE;
    public static String IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE;
    public static String IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE;
    public static String IMAGE_LIST_QUEST_TOKENS_FILE;
    public static String IMAGE_LIST_QUEST_BOOSTERS_FILE;
    public static String IMAGE_LIST_QUEST_FATPACKS_FILE;
    public static String IMAGE_LIST_QUEST_BOOSTERBOXES_FILE;
    public static String IMAGE_LIST_QUEST_PRECONS_FILE;
    public static String IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE;

    public static String CHANGES_FILE;
    public static String LICENSE_FILE;
    public static String README_FILE;
    public static String HOWTO_FILE;

    public static String DRAFT_DIR;
    public static String DRAFT_RANKINGS_FILE;
    public static String SEALED_DIR;
    public static String CARD_DATA_DIR;
    public static String EDITIONS_DIR;
    public static String BLOCK_DATA_DIR;
    public static String DECK_CUBE_DIR;
    public static String AI_PROFILE_DIR;
    public static String SOUND_DIR;
    public static String MUSIC_DIR;
	public static String LANG_DIR;
    public static String EFFECTS_DIR;

    private static String QUEST_DIR;
    public static String QUEST_WORLD_DIR;
    public static String QUEST_PRECON_DIR;
    public static String PRICES_BOOSTER_FILE;
    public static String BAZAAR_DIR;
    public static String BAZAAR_INDEX_FILE;
    public static String DEFAULT_DUELS_DIR;
    public static String DEFAULT_CHALLENGES_DIR;
    public static String THEMES_DIR;

    public static String SKINS_DIR;
    public static String DEFAULT_SKINS_DIR;
    //don't associate these skin files with a directory since skin directory will be determined later
    public static final String SPRITE_ICONS_FILE     = "sprite_icons.png"; 
    public static final String SPRITE_FOILS_FILE     = "sprite_foils.png";
    public static final String SPRITE_OLD_FOILS_FILE = "sprite_old_foils.png";
    public static final String SPRITE_AVATARS_FILE   = "sprite_avatars.png";
    public static final String FONT_FILE             = "font1.ttf";
    public static final String SPLASH_BG_FILE        = "bg_splash.png";
    public static final String MATCH_BG_FILE         = "bg_match.jpg";
    public static final String TEXTURE_BG_FILE       = "bg_texture.jpg";
    public static final String DRAFT_DECK_IMG_FILE   = "bg_draft_deck.png";

    // data tree roots
    public static String USER_DIR;
    public static String CACHE_DIR;
    public static String CACHE_CARD_PICS_DIR;
    public static Map<String, String> CACHE_CARD_PICS_SUBDIR;
    public static int SERVER_PORT_NUMBER;

    // data that is only in the profile dirs
    public static String USER_QUEST_DIR;
    public static String USER_PREFS_DIR;
    public static String USER_GAMES_DIR;
    public static String LOG_FILE;
    public static String DECK_BASE_DIR;
    public static String DECK_CONSTRUCTED_DIR;
    public static String DECK_DRAFT_DIR;
    public static String DECK_WINSTON_DIR;
    public static String DECK_SEALED_DIR;
    public static String DECK_SCHEME_DIR;
    public static String DECK_PLANE_DIR;
    public static String DECK_COMMANDER_DIR;
    public static String QUEST_SAVE_DIR;
    public static String MAIN_PREFS_FILE;
    public static String CARD_PREFS_FILE;
    public static String DECK_PREFS_FILE;
    public static String QUEST_PREFS_FILE;
    public static String ITEM_VIEW_PREFS_FILE;
    public static String ACHIEVEMENTS_FILE;

    // data that has defaults in the program dir but overrides/additions in the user dir
    private static String _DEFAULTS_DIR;
    public static String NO_CARD_FILE;
    public static FileLocation WINDOW_LAYOUT_FILE;
    public static FileLocation MATCH_LAYOUT_FILE;
    public static FileLocation WORKSHOP_LAYOUT_FILE;
    public static FileLocation EDITOR_LAYOUT_FILE;
    public static FileLocation GAUNTLET_DIR;

    // data that is only in the cached dir
    private static String PICS_DIR;
    public static String DB_DIR;
    public static String FONTS_DIR;
    public static String CACHE_TOKEN_PICS_DIR;
    public static String CACHE_ICON_PICS_DIR;
    public static String CACHE_SYMBOLS_DIR;
    public static String CACHE_BOOSTER_PICS_DIR;
    public static String CACHE_FATPACK_PICS_DIR;
    public static String CACHE_BOOSTERBOX_PICS_DIR;
    public static String CACHE_PRECON_PICS_DIR;
    public static String CACHE_TOURNAMENTPACK_PICS_DIR;
    public static String QUEST_CARD_PRICE_FILE;

    public static String[] PROFILE_DIRS;
 
    // URLs
    private static final String URL_CARDFORGE = "http://cardforge.org";
    public static final String URL_DRAFT_UPLOAD   = URL_CARDFORGE + "/draftAI/submitDraftData.php";
    public static final String URL_PIC_DOWNLOAD   = URL_CARDFORGE + "/fpics/";
    public static final String URL_PRICE_DOWNLOAD = URL_CARDFORGE + "/MagicInfo/pricegen.php";
}
