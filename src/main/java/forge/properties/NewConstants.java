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

import java.util.List;

import com.google.common.collect.Lists;


public final class NewConstants {
    public static final String PROFILE_FILE          = "forge.profile.properties";
    public static final String PROFILE_TEMPLATE_FILE = PROFILE_FILE + ".example";

    // data tree roots
    private static final String _USER_DIR;
    private static final String _CACHE_DIR;
    static {
        ForgeProfileProperties profileProps = new ForgeProfileProperties(PROFILE_FILE);
        _USER_DIR  = profileProps.userDir;
        _CACHE_DIR = profileProps.cacheDir;
    }
    private static final String _RES_ROOT       = "res/";
    private static final String _DEFAULTS_DIR   = _RES_ROOT + "defaults/";
    private static final String _QUEST_DIR      = _RES_ROOT + "quest/";
    private static final String _USER_PREFS_DIR = _USER_DIR + "preferences/";
    private static final String _USER_QUEST_DIR = _USER_DIR + "quest/";
    private static final String _DB_DIR         = _CACHE_DIR + "db/";

    // data that is only in the program dir
    public static final String IMAGE_LIST_TOKENS_FILE                = _RES_ROOT + "token-images.txt";
    public static final String IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE  = _QUEST_DIR + "quest-opponent-icons.txt";
    public static final String IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE  = _QUEST_DIR + "quest-pet-shop-icons.txt";
    public static final String IMAGE_LIST_QUEST_TOKENS_FILE          = _QUEST_DIR + "quest-pet-token-images.txt";
    public static final String IMAGE_LIST_QUEST_BOOSTERS_FILE        = _QUEST_DIR + "booster-images.txt";
    public static final String IMAGE_LIST_QUEST_FATPACKS_FILE        = _QUEST_DIR + "fatpack-images.txt";
    public static final String IMAGE_LIST_QUEST_PRECONS_FILE         = _QUEST_DIR + "precon-images.txt";
    public static final String IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE = _QUEST_DIR + "tournamentpack-images.txt";
    
    public static final String TEXT_HOWTO_FILE     = _RES_ROOT + "howto.txt";
    public static final String DRAFT_RANKINGS_FILE = _RES_ROOT + "draft/rankings.txt";
    public static final String PRICES_BOOSTER_FILE = _QUEST_DIR + "booster-prices.txt";
    public static final String BAZAAR_FILE         = _QUEST_DIR + "bazaar/index.xml";
    
    public static final String CARD_DATA_PETS_DIR     = _QUEST_DIR + "bazaar/";
    public static final String DEFAULT_DUELS_DIR      = _QUEST_DIR + "duels";
    public static final String DEFAULT_CHALLENGES_DIR = _QUEST_DIR + "challenges";
    
    // data that has defaults in the program dir but overrides/additions in the user dir
    public static final FileLocation MAIN_PREFERENCES_FILE   = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "forge.preferences");
    public static final FileLocation EDITOR_PREFERENCES_FILE = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "editor.preferences");
    public static final FileLocation QUEST_PREFERENCES_FILE  = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "quest.preferences");
    
    public static final FileLocation HOME_LAYOUT_FILE   = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "home.xml");
    public static final FileLocation MATCH_LAYOUT_FILE  = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "match.xml");
    public static final FileLocation EDITOR_LAYOUT_FILE = new FileLocation(_DEFAULTS_DIR, _USER_PREFS_DIR, "editor.xml");
    
    public static final FileLocation QUEST_CARD_PRICE_FILE = new FileLocation(_DEFAULTS_DIR, _DB_DIR, "all-prices.txt");
    
    public static final FileLocation CARD_DATA_DIR    = new FileLocation(_RES_ROOT, _USER_DIR, "cardsfolder/");
    public static final FileLocation DECK_CUBE_DIR    = new FileLocation(_DEFAULTS_DIR, _USER_DIR, "cube");
    public static final FileLocation QUEST_WORLD_DIR  = new FileLocation(_QUEST_DIR, _USER_QUEST_DIR, "worlds/");
    public static final FileLocation QUEST_PRECON_DIR = new FileLocation(_QUEST_DIR, _USER_QUEST_DIR, "precons/");

    // data that is only in the user dir
    public static final String DECK_CONSTRUCTED_DIR = _USER_DIR + "constructed/";
    public static final String DECK_DRAFT_DIR       = _USER_DIR + "draft/";
    public static final String DECK_SEALED_DIR      = _USER_DIR + "sealed/";
    public static final String DECK_SCHEME_DIR      = _USER_DIR + "scheme/";
    public static final String DECK_PLANE_DIR       = _USER_DIR + "plane/";
    public static final String QUEST_SAVE_DIR       = _USER_DIR + "quest/saves";

    // data that is only in the cached dir
    public static final String CACHE_CARD_PICS_DIR           = _CACHE_DIR + "pics/cards/";
    public static final String CACHE_TOKEN_PICS_DIR          = _CACHE_DIR + "pics/tokens/";
    public static final String CACHE_ICON_PICS_DIR           = _CACHE_DIR + "pics/icons/";
    public static final String CACHE_BOOSTER_PICS_DIR        = _CACHE_DIR + "pics/boosters/";
    public static final String CACHE_FATPACK_PICS_DIR        = _CACHE_DIR + "pics/fatpacks/";
    public static final String CACHE_PRECON_PICS_DIR         = _CACHE_DIR + "pics/precons/";
    public static final String CACHE_TOURNAMENTPACK_PICS_DIR = _CACHE_DIR + "pics/tournamentpacks/";
    public static final String CACHE_MORPH_IMAGE_FILE        = CACHE_TOKEN_PICS_DIR + "morph.jpg";
    
    public static final List<String> PROFILE_DIRS = Lists.newArrayList(
            _USER_PREFS_DIR,
            _DB_DIR,
            CARD_DATA_DIR.userPrefLoc,
            DECK_CUBE_DIR.userPrefLoc,
            QUEST_WORLD_DIR.userPrefLoc,
            QUEST_PRECON_DIR.userPrefLoc,
            DECK_CONSTRUCTED_DIR,
            DECK_DRAFT_DIR,
            DECK_SEALED_DIR,
            DECK_SCHEME_DIR,
            DECK_PLANE_DIR,
            QUEST_SAVE_DIR,
            CACHE_CARD_PICS_DIR,
            CACHE_TOKEN_PICS_DIR,
            CACHE_ICON_PICS_DIR,
            CACHE_BOOSTER_PICS_DIR,
            CACHE_FATPACK_PICS_DIR,
            CACHE_PRECON_PICS_DIR,
            CACHE_TOURNAMENTPACK_PICS_DIR);
    
    // URLs
    private static final String _URL_CARDFORGE = "http://cardforge.org";
    public static final String URL_DRAFT_UPLOAD   = _URL_CARDFORGE + "/draftAI/submitDraftData.php";
    public static final String URL_PIC_DOWNLOAD   = _URL_CARDFORGE + "/fpics/";
    public static final String URL_PRICE_DOWNLOAD = _URL_CARDFORGE + "/MagicInfo/pricegen.php";
}
