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

import forge.GuiBase;

import java.util.Collections;
import java.util.Map;

public final class ForgeConstants {
    public static final String ASSETS_DIR            = GuiBase.getInterface().getAssetsDir();
    public static final String PROFILE_FILE          = ASSETS_DIR + "forge.profile.properties";
    public static final String PROFILE_TEMPLATE_FILE = PROFILE_FILE + ".example";

    public static final String RES_DIR = ASSETS_DIR + "res/";
    public static final String LISTS_DIR = RES_DIR + "lists/";
    public static final String KEYWORD_LIST_FILE                     = LISTS_DIR + "NonStackingKWList.txt";
    public static final String TYPE_LIST_FILE                        = LISTS_DIR + "TypeLists.txt";
    public static final String IMAGE_LIST_TOKENS_FILE                = LISTS_DIR + "token-images.txt";
    public static final String IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE  = LISTS_DIR + "quest-opponent-icons.txt";
    public static final String IMAGE_LIST_QUEST_TOKENS_FILE          = LISTS_DIR + "quest-pet-token-images.txt";
    public static final String IMAGE_LIST_QUEST_BOOSTERS_FILE        = LISTS_DIR + "booster-images.txt";
    public static final String IMAGE_LIST_QUEST_FATPACKS_FILE        = LISTS_DIR + "fatpack-images.txt";
    public static final String IMAGE_LIST_QUEST_BOOSTERBOXES_FILE    = LISTS_DIR + "boosterbox-images.txt";
    public static final String IMAGE_LIST_QUEST_PRECONS_FILE         = LISTS_DIR + "precon-images.txt";
    public static final String IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE = LISTS_DIR + "tournamentpack-images.txt";
    public static final String NET_DECKS_LIST_FILE                   = LISTS_DIR + "net-decks.txt";
    public static final String NET_DECKS_COMMANDER_LIST_FILE         = LISTS_DIR + "net-decks-commander.txt";

    public static final String CHANGES_FILE = ASSETS_DIR + "CHANGES.txt";
    public static final String LICENSE_FILE = ASSETS_DIR + "LICENSE.txt";
    public static final String README_FILE  = ASSETS_DIR + "README.txt";
    public static final String HOWTO_FILE   = RES_DIR + "howto.txt";

    public static final String DRAFT_DIR           = RES_DIR + "draft/";
    public static final String DRAFT_RANKINGS_FILE = DRAFT_DIR + "rankings.txt";
    public static final String SEALED_DIR          = RES_DIR + "sealed/";
    public static final String CARD_DATA_DIR       = RES_DIR + "cardsfolder/";
    public static final String EDITIONS_DIR        = RES_DIR + "editions/";
    public static final String BLOCK_DATA_DIR      = RES_DIR + "blockdata/";
    public static final String DECK_CUBE_DIR       = RES_DIR + "cube/";
    public static final String AI_PROFILE_DIR      = RES_DIR + "ai/";
    public static final String SOUND_DIR           = RES_DIR + "sound/";
    public static final String MUSIC_DIR           = RES_DIR + "music/";
	public static final String LANG_DIR            = RES_DIR + "languages/";
    public static final String EFFECTS_DIR         = RES_DIR + "effects/";

    private static final String QUEST_DIR             = RES_DIR + "quest/";
    public static final String QUEST_WORLD_DIR        = QUEST_DIR + "world/";
    public static final String QUEST_PRECON_DIR       = QUEST_DIR + "precons/";
    public static final String PRICES_BOOSTER_FILE    = QUEST_DIR + "booster-prices.txt";
    public static final String BAZAAR_DIR             = QUEST_DIR + "bazaar/";
    public static final String BAZAAR_INDEX_FILE      = BAZAAR_DIR + "index.xml";
    public static final String DEFAULT_DUELS_DIR      = QUEST_DIR + "duels";
    public static final String DEFAULT_CHALLENGES_DIR = QUEST_DIR + "challenges";
    public static final String THEMES_DIR             = QUEST_DIR + "themes";

    public static final String SKINS_DIR         = RES_DIR + "skins/";
    public static final String DEFAULT_SKINS_DIR = SKINS_DIR + "default/";
    //don't associate these skin files with a directory since skin directory will be determined later
    public static final String SPRITE_ICONS_FILE      = "sprite_icons.png";
    public static final String SPRITE_FOILS_FILE      = "sprite_foils.png";
    public static final String SPRITE_OLD_FOILS_FILE  = "sprite_old_foils.png";
    public static final String SPRITE_TROPHIES_FILE   = "sprite_trophies.png";
    public static final String SPRITE_AVATARS_FILE    = "sprite_avatars.png";
    public static final String SPRITE_PLANAR_CONQUEST_FILE = "sprite_planar_conquest.png";
    public static final String FONT_FILE              = "font1.ttf";
    public static final String SPLASH_BG_FILE         = "bg_splash.png";
    public static final String MATCH_BG_FILE          = "bg_match.jpg";
    public static final String TEXTURE_BG_FILE        = "bg_texture.jpg";
    public static final String DRAFT_DECK_IMG_FILE    = "bg_draft_deck.png";

    // data tree roots
    public static final String USER_DIR;
    public static final String CACHE_DIR;
    public static final String CACHE_CARD_PICS_DIR;
    public static final Map<String, String> CACHE_CARD_PICS_SUBDIR;
    public static final int SERVER_PORT_NUMBER;
    static {
        ForgeProfileProperties.load();
        USER_DIR               = ForgeProfileProperties.getUserDir();
        CACHE_DIR              = ForgeProfileProperties.getCacheDir();
        CACHE_CARD_PICS_DIR    = ForgeProfileProperties.getCardPicsDir();
        CACHE_CARD_PICS_SUBDIR = Collections.unmodifiableMap(ForgeProfileProperties.getCardPicsSubDirs());
        SERVER_PORT_NUMBER     = ForgeProfileProperties.getServerPort();
    }

    // data that is only in the profile dirs
    public static final String USER_QUEST_DIR       = USER_DIR + "quest/";
    public static final String USER_CONQUEST_DIR    = USER_DIR + "conquest/";
    public static final String USER_PREFS_DIR       = USER_DIR + "preferences/";
    public static final String USER_GAMES_DIR       = USER_DIR + "games/";
    public static final String LOG_FILE             = USER_DIR + "forge.log";
    public static final String DECK_BASE_DIR        = USER_DIR + "decks/";
    public static final String ACHIEVEMENTS_DIR     = USER_DIR + "achievements/";
    public static final String DECK_CONSTRUCTED_DIR = DECK_BASE_DIR + "constructed/";
    public static final String DECK_DRAFT_DIR       = DECK_BASE_DIR + "draft/";
    public static final String DECK_WINSTON_DIR     = DECK_BASE_DIR + "winston/";
    public static final String DECK_SEALED_DIR      = DECK_BASE_DIR + "sealed/";
    public static final String DECK_SCHEME_DIR      = DECK_BASE_DIR + "scheme/";
    public static final String DECK_PLANE_DIR       = DECK_BASE_DIR + "planar/";
    public static final String DECK_COMMANDER_DIR   = DECK_BASE_DIR + "commander/";
    public static final String DECK_TINY_LEADERS_DIR = DECK_BASE_DIR + "tiny_leaders/";
    public static final String DECK_NET_DIR         = DECK_BASE_DIR + "net/";
    public static final String QUEST_SAVE_DIR       = USER_QUEST_DIR + "saves/";
    public static final String CONQUEST_SAVE_DIR    = USER_CONQUEST_DIR + "saves/";
    public static final String MAIN_PREFS_FILE      = USER_PREFS_DIR + "forge.preferences";
    public static final String CARD_PREFS_FILE      = USER_PREFS_DIR + "card.preferences";
    public static final String DECK_PREFS_FILE      = USER_PREFS_DIR + "deck.preferences";
    public static final String QUEST_PREFS_FILE     = USER_PREFS_DIR + "quest.preferences";
    public static final String CONQUEST_PREFS_FILE  = USER_PREFS_DIR + "conquest.preferences";
    public static final String ITEM_VIEW_PREFS_FILE = USER_PREFS_DIR + "item_view.preferences";

    // data that has defaults in the program dir but overrides/additions in the user dir
    private static final String _DEFAULTS_DIR = RES_DIR + "defaults/";
    public static final String NO_CARD_FILE   = _DEFAULTS_DIR + "no_card.jpg";
    public static final FileLocation WINDOW_LAYOUT_FILE      = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "window.xml");
    public static final FileLocation MATCH_LAYOUT_FILE       = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "match.xml");
    public static final FileLocation WORKSHOP_LAYOUT_FILE    = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "workshop.xml");
    public static final FileLocation EDITOR_LAYOUT_FILE      = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "editor.xml");
    public static final FileLocation GAUNTLET_DIR            = new FileLocation(_DEFAULTS_DIR, USER_DIR,       "gauntlet/");

    // data that is only in the cached dir
    private static final String PICS_DIR                     = CACHE_DIR + "pics/";
    public static final String DB_DIR                        = CACHE_DIR + "db/";
    public static final String FONTS_DIR                     = CACHE_DIR + "fonts/";
    public static final String CACHE_TOKEN_PICS_DIR          = PICS_DIR + "tokens/";
    public static final String CACHE_ICON_PICS_DIR           = PICS_DIR + "icons/";
    public static final String CACHE_SYMBOLS_DIR             = PICS_DIR + "symbols/";
    public static final String CACHE_BOOSTER_PICS_DIR        = PICS_DIR + "boosters/";
    public static final String CACHE_FATPACK_PICS_DIR        = PICS_DIR + "fatpacks/";
    public static final String CACHE_BOOSTERBOX_PICS_DIR     = PICS_DIR + "boosterboxes/";
    public static final String CACHE_PRECON_PICS_DIR         = PICS_DIR + "precons/";
    public static final String CACHE_TOURNAMENTPACK_PICS_DIR = PICS_DIR + "tournamentpacks/";
    public static final String CACHE_ACHIEVEMENTS_DIR        = PICS_DIR + "achievements/";
    public static final String QUEST_CARD_PRICE_FILE         = DB_DIR + "all-prices.txt";

    public static final String[] PROFILE_DIRS = {
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
            DECK_COMMANDER_DIR,
            DECK_NET_DIR,
            QUEST_SAVE_DIR,
            CACHE_TOKEN_PICS_DIR,
            CACHE_ICON_PICS_DIR,
            CACHE_BOOSTER_PICS_DIR,
            CACHE_FATPACK_PICS_DIR,
            CACHE_BOOSTERBOX_PICS_DIR,
            CACHE_PRECON_PICS_DIR,
            CACHE_TOURNAMENTPACK_PICS_DIR };

    // URLs
    private static final String URL_CARDFORGE = "http://downloads.cardforge.link";
    public static final String URL_PIC_DOWNLOAD = URL_CARDFORGE + "/images/cards/";
    public static final String URL_PRICE_DOWNLOAD = "http://www.cardforge.link/MagicInfo/pricegen.php";

    // Constants for Display Card Identity game setting
    public static final String DISP_CURRENT_COLORS_ALWAYS = "Always";
    public static final String DISP_CURRENT_COLORS_CHANGED = "Changed";
    public static final String DISP_CURRENT_COLORS_MULTICOLOR = "Multicolor";
    public static final String DISP_CURRENT_COLORS_MULTI_OR_CHANGED = "Multi+Changed";
    public static final String DISP_CURRENT_COLORS_NEVER = "Never";

}
