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
package forge.localinstance.properties;

import forge.gui.GuiBase;
import forge.util.FileUtil;
import forge.util.Localizer;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public final class ForgeConstants {
    private static final Localizer localizer = Localizer.getInstance();
    public static final String GITHUB_FORGE_URL                 = "https://github.com/Card-Forge/forge/";
    public static final String GITHUB_RELEASES_ATOM             = GITHUB_FORGE_URL + "releases.atom";
    public static final String GITHUB_COMMITS_ATOM              = GITHUB_FORGE_URL + "commits/master.atom";
    public static final String GITHUB_SNAPSHOT_URL              = GITHUB_FORGE_URL + "releases/download/daily-snapshots/";
    public static final String RELEASE_URL                      = "https://releases.cardforge.org/";
    public static final String PATH_SEPARATOR                   = File.separator;
    public static final String ASSETS_DIR                       = GuiBase.getInterface().getAssetsDir();
    public static final String PROFILE_FILE                     = ASSETS_DIR + "forge.profile.properties";
    public static final String PROFILE_TEMPLATE_FILE            = PROFILE_FILE + ".example";
    public static final Integer DEFAULT_SERVER_CONNECTION_PORT  = 36743;

    public static final String RES_DIR                      = ASSETS_DIR + "res" + PATH_SEPARATOR;
    public static final String ADVENTURE_DIR                = RES_DIR + "adventure" + PATH_SEPARATOR;
    public static final String ADVENTURE_COMMON_DIR         = ADVENTURE_DIR + "common" + PATH_SEPARATOR;
    public static final String LISTS_DIR                    = RES_DIR + "lists" + PATH_SEPARATOR;
    public static final String SETLOOKUP_DIR                = RES_DIR + "setlookup" + PATH_SEPARATOR;
    public static final String KEYWORD_LIST_FILE                     = LISTS_DIR + "NonStackingKWList.txt";
    public static final String TYPE_LIST_FILE                        = LISTS_DIR + "TypeLists.txt";
    public static final String SPECIAL_CARD_ACHIEVEMENT_LIST_FILE    = LISTS_DIR + "special-card-achievements.txt";
    public static final String PLANESWALKER_ACHIEVEMENT_LIST_FILE    = LISTS_DIR + "planeswalker-achievements.txt";
    public static final String ALTWIN_ACHIEVEMENT_LIST_FILE          = LISTS_DIR + "altwin-achievements.txt";
    public static final String IMAGE_LIST_TOKENS_FILE                = LISTS_DIR + "token-images.txt";
    public static final String IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE  = LISTS_DIR + "quest-opponent-icons.txt";
    public static final String IMAGE_LIST_QUEST_TOKENS_FILE          = LISTS_DIR + "quest-pet-token-images.txt";
    public static final String IMAGE_LIST_QUEST_BOOSTERS_FILE        = LISTS_DIR + "booster-images.txt";
    public static final String IMAGE_LIST_QUEST_FATPACKS_FILE        = LISTS_DIR + "fatpack-images.txt";
    public static final String IMAGE_LIST_QUEST_BOOSTERBOXES_FILE    = LISTS_DIR + "boosterbox-images.txt";
    public static final String IMAGE_LIST_QUEST_PRECONS_FILE         = LISTS_DIR + "precon-images.txt";
    public static final String IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE = LISTS_DIR + "tournamentpack-images.txt";
    public static final String IMAGE_LIST_ACHIEVEMENTS_FILE          = LISTS_DIR + "achievement-images.txt";
    public static final String IMAGE_LIST_PLANES_IMAGES_FILE         = LISTS_DIR + "planes-images.txt";
    public static final String NET_DECKS_LIST_FILE                   = LISTS_DIR + "net-decks.txt";
    public static final String NET_DECKS_COMMANDER_LIST_FILE         = LISTS_DIR + "net-decks-commander.txt";
    public static final String NET_DECKS_BRAWL_LIST_FILE             = LISTS_DIR + "net-decks-brawl.txt";
    public static final String NET_DECKS_OATHBREAKER_LIST_FILE       = LISTS_DIR + "net-decks-oathbreaker.txt";
    public static final String NET_DECKS_TINYLEADERS_LIST_FILE       = LISTS_DIR + "net-decks-tinyleaders.txt";
    public static final String CLASSIC_MODULE_CARD_TO_CROP_FILE      = LISTS_DIR + "classicModuleCardtoCrop.txt";
    public static final String SKINS_LIST_FILE                       = LISTS_DIR + "skinsList.txt";
    public static final String CJK_FONTS_LIST_FILE                   = LISTS_DIR + "font-list.txt";
    public static final String NET_ARCHIVE_STANDARD_DECKS_LIST_FILE  = LISTS_DIR + "net-decks-archive-standard.txt";
    public static final String NET_ARCHIVE_PIONEER_DECKS_LIST_FILE   = LISTS_DIR + "net-decks-archive-pioneer.txt";
    public static final String NET_ARCHIVE_MODERN_DECKS_LIST_FILE    = LISTS_DIR + "net-decks-archive-modern.txt";
    public static final String NET_ARCHIVE_PAUPER_DECKS_LIST_FILE    = LISTS_DIR + "net-decks-archive-pauper.txt";
    public static final String NET_ARCHIVE_LEGACY_DECKS_LIST_FILE    = LISTS_DIR + "net-decks-archive-legacy.txt";
    public static final String NET_ARCHIVE_VINTAGE_DECKS_LIST_FILE   = LISTS_DIR + "net-decks-archive-vintage.txt";
    public static final String NET_ARCHIVE_BLOCK_DECKS_LIST_FILE     = LISTS_DIR + "net-decks-archive-block.txt";


    public static final String CHANGES_FILE                 = ASSETS_DIR + "README.txt";
    public static final String CHANGES_FILE_NO_RELEASE      = ASSETS_DIR + "CHANGES.txt";
    public static final String LICENSE_FILE                 = ASSETS_DIR + "LICENSE.txt";
    public static final String HOWTO_FILE                   = RES_DIR + "howto.txt";

    public static final String DRAFT_DIR                    = RES_DIR + "draft" + PATH_SEPARATOR;
    public static final String DRAFT_RANKINGS_FILE          = DRAFT_DIR + "rankings.txt";
    public static final String DRAFT_RANKINGS_FOLDER          = DRAFT_DIR + "rankings/";
    public static final String SEALED_DIR                   = RES_DIR + "sealed" + PATH_SEPARATOR;
    public static final String CARD_DATA_DIR                = RES_DIR + "cardsfolder" + PATH_SEPARATOR;
    public static final String TOKEN_DATA_DIR               = RES_DIR + "tokenscripts" + PATH_SEPARATOR;
    public static final String EDITIONS_DIR                 = RES_DIR + "editions" + PATH_SEPARATOR;
    public static final String BLOCK_DATA_DIR               = RES_DIR + "blockdata" + PATH_SEPARATOR;
    public static final String FORMATS_DATA_DIR             = RES_DIR + "formats" + PATH_SEPARATOR;
    public static final String DECK_CUBE_DIR                = RES_DIR + "cube" + PATH_SEPARATOR;
    public static final String AI_PROFILE_DIR               = RES_DIR + "ai" + PATH_SEPARATOR;
    public static final String SOUND_DIR                    = "sound" + PATH_SEPARATOR;
    public static final String MUSIC_DIR                    = "music" + PATH_SEPARATOR;
    public static final String SKIN_DIR                     = "skin" + PATH_SEPARATOR;
    public static final String ADVENTURE_COMMON_MUSIC_DIR   = ADVENTURE_COMMON_DIR + "music" + PATH_SEPARATOR;
    public static final String LANG_DIR                     = RES_DIR + "languages" + PATH_SEPARATOR;
    public static final String EFFECTS_DIR                  = RES_DIR + "effects" + PATH_SEPARATOR;
    public static final String PUZZLE_DIR                   = RES_DIR + "puzzle" + PATH_SEPARATOR;
    public static final String TUTORIAL_DIR                 = RES_DIR + "tutorial" + PATH_SEPARATOR;
    public static final String DECK_GEN_DIR                 = RES_DIR + "deckgendecks" + PATH_SEPARATOR;
    public static final String GENETIC_AI_DECK_DIR          = RES_DIR + "geneticaidecks" + PATH_SEPARATOR;

    private static final String QUEST_DIR                   = RES_DIR + "quest" + PATH_SEPARATOR;
    public static final String QUEST_WORLD_DIR              = QUEST_DIR + "world" + PATH_SEPARATOR;
    public static final String QUEST_PRECON_DIR             = QUEST_DIR + "precons" + PATH_SEPARATOR;
    public static final String PRICES_BOOSTER_FILE          = QUEST_DIR + "booster-prices.txt";
    public static final String BAZAAR_DIR                   = QUEST_DIR + "bazaar" + PATH_SEPARATOR;
    public static final String BAZAAR_INDEX_FILE            = BAZAAR_DIR + "index.xml";
    public static final String DEFAULT_DUELS_DIR            = QUEST_DIR + "duels";
    public static final String DEFAULT_CHALLENGES_DIR       = QUEST_DIR + "challenges";
    public static final String THEMES_DIR                   = QUEST_DIR + "themes";

    private static final String CONQUEST_DIR                = RES_DIR + "conquest" + PATH_SEPARATOR;
    public static final String CONQUEST_PLANES_DIR          = CONQUEST_DIR + "planes" + PATH_SEPARATOR;
    public static final String BASE_SKINS_DIR               = RES_DIR + "skins" + PATH_SEPARATOR;
    public static final String COMMON_FONTS_DIR             = RES_DIR + "fonts" + PATH_SEPARATOR;
    public static final String DEFAULT_SKINS_DIR            = BASE_SKINS_DIR + "default" + PATH_SEPARATOR;
    //don't associate these skin files with a directory since skin directory will be determined later
    public static final String SPRITE_ICONS_FILE             = "sprite_icons.png";
    public static final String SPRITE_FOILS_FILE             = "sprite_foils.png";
    public static final String SPRITE_OLD_FOILS_FILE         = "sprite_old_foils.png";
    public static final String SPRITE_TROPHIES_FILE          = "sprite_trophies.png";
    public static final String SPRITE_ABILITY_FILE           = "sprite_ability.png";
    public static final String SPRITE_BORDER_FILE            = "sprite_border.png";
    public static final String SPRITE_ADV_BUTTONS_FILE       = "sprite_adv_buttons.png";
    public static final String SPRITE_BUTTONS_FILE           = "sprite_buttons.png";
    public static final String SPRITE_DECKBOX_FILE           = "sprite_deckbox.png";
    public static final String SPRITE_START_FILE             = "sprite_start.png";
    public static final String SPRITE_MANAICONS_FILE         = "sprite_manaicons.png";
    public static final String SPRITE_PHYREXIAN_FILE         = "sprite_phyrexian.png";
    public static final String SPRITE_COLORLESS_HYBRID_FILE  = "sprite_hybrid_colorless.png";
    public static final String SPRITE_ATTRACTION_LIGHTS_FILE = "sprite_attraction_lights.png";
    public static final String SPRITE_CURSOR_FILE            = "sprite_cursor.png";
    public static final String SPRITE_AVATARS_FILE           = "sprite_avatars.png";
    public static final String SPRITE_CRACKS_FILE            = "sprite_cracks.png";
    public static final String SPRITE_SLEEVES_FILE           = "sprite_sleeves.png";
    public static final String SPRITE_SLEEVES2_FILE          = "sprite_sleeves2.png";
    public static final String SPRITE_FAVICONS_FILE          = "sprite_favicons.png";
    public static final String SPRITE_PLANAR_CONQUEST_FILE   = "sprite_planar_conquest.png";
    public static final String SPRITE_ADVENTURE_FILE         = "sprite_adventure.png";
    public static final String SPRITE_SETLOGO_FILE           = "sprite_setlogo.png";
    public static final String SPRITE_WATERMARK_FILE         = "sprite_watermark.png";
    public static final String SPRITE_ZONE_FILE              = "sprite_zone.png";
    public static final String SPRITE_DRAFTRANKS_FILE        = "sprite_draftranks.png";

    public static final String FONT_FILE                    = "font1.ttf";
    public static final String SPLASH_BG_FILE               = "bg_splash.png";
    public static final String MATCH_BG_FILE                = "bg_match.jpg";
    public static final String MATCH_BG_DAY_FILE            = "bg_day.jpg";
    public static final String MATCH_BG_NIGHT_FILE          = "bg_night.jpg";
    public static final String ADV_MATCH_BG_FILE            = "adv_bg_match.jpg";
    public static final String ADV_MATCH_BG_DAY_FILE        = "adv_bg_match_day.jpg";
    public static final String ADV_MATCH_BG_NIGHT_FILE      = "adv_bg_match_nigh.jpg";
    public static final String ADV_TEXTURE_BG_FILE          = "adv_bg_texture.jpg";
    public static final String TEXTURE_BG_FILE              = "bg_texture.jpg";
    public static final String SPACE_BG_FILE                = "bg_space.png";
    public static final String CHAOS_WHEEL_IMG_FILE         = "bg_chaos_wheel.png";
    public static final String DRAFT_DECK_IMG_FILE          = "bg_draft_deck.png";
    //Adventure locations
    public static final String ADV_BG_SWAMP_FILE            = "adv_bg_swamp.jpg";
    public static final String ADV_BG_FOREST_FILE           = "adv_bg_forest.jpg";
    public static final String ADV_BG_MOUNTAIN_FILE         = "adv_bg_mountain.jpg";
    public static final String ADV_BG_ISLAND_FILE           = "adv_bg_island.jpg";
    public static final String ADV_BG_PLAINS_FILE           = "adv_bg_plains.jpg";
    public static final String ADV_BG_WASTE_FILE            = "adv_bg_waste.jpg";
    public static final String ADV_BG_COMMON_FILE           = "adv_bg_common.jpg";
    public static final String ADV_BG_CAVE_FILE             = "adv_bg_cave.jpg";
    public static final String ADV_BG_DUNGEON_FILE          = "adv_bg_dungeon.jpg";
    public static final String ADV_BG_CASTLE_FILE           = "adv_bg_castle.jpg";

    //CARD BG
    public static final String IMG_CARDBG_A           = "IMG_CARDBG_A.png";
    public static final String IMG_CARDBG_B           = "IMG_CARDBG_B.png";
    public static final String IMG_CARDBG_BG          = "IMG_CARDBG_BG.png";
    public static final String IMG_CARDBG_BR          = "IMG_CARDBG_BR.png";
    public static final String IMG_CARDBG_C           = "IMG_CARDBG_C.png";
    public static final String IMG_CARDBG_G           = "IMG_CARDBG_G.png";
    public static final String IMG_CARDBG_L           = "IMG_CARDBG_L.png";
    public static final String IMG_CARDBG_M           = "IMG_CARDBG_M.png";
    public static final String IMG_CARDBG_R           = "IMG_CARDBG_R.png";
    public static final String IMG_CARDBG_RG          = "IMG_CARDBG_RG.png";
    public static final String IMG_CARDBG_U           = "IMG_CARDBG_U.png";
    public static final String IMG_CARDBG_UB          = "IMG_CARDBG_UB.png";
    public static final String IMG_CARDBG_UG          = "IMG_CARDBG_UG.png";
    public static final String IMG_CARDBG_UR          = "IMG_CARDBG_UR.png";
    public static final String IMG_CARDBG_V           = "IMG_CARDBG_V.png";
    public static final String IMG_CARDBG_W           = "IMG_CARDBG_W.png";
    public static final String IMG_CARDBG_WB          = "IMG_CARDBG_WB.png";
    public static final String IMG_CARDBG_WG          = "IMG_CARDBG_WG.png";
    public static final String IMG_CARDBG_WR          = "IMG_CARDBG_WR.png";
    public static final String IMG_CARDBG_WU          = "IMG_CARDBG_WU.png";
    public static final String IMG_PWBG_B             = "IMG_PWBG_B.png";
    public static final String IMG_PWBG_BG            = "IMG_PWBG_BG.png";
    public static final String IMG_PWBG_BR            = "IMG_PWBG_BR.png";
    public static final String IMG_PWBG_C             = "IMG_PWBG_C.png";
    public static final String IMG_PWBG_G             = "IMG_PWBG_G.png";
    public static final String IMG_PWBG_M             = "IMG_PWBG_M.png";
    public static final String IMG_PWBG_R             = "IMG_PWBG_R.png";
    public static final String IMG_PWBG_RG            = "IMG_PWBG_RG.png";
    public static final String IMG_PWBG_U             = "IMG_PWBG_U.png";
    public static final String IMG_PWBG_UB            = "IMG_PWBG_UB.png";
    public static final String IMG_PWBG_UG            = "IMG_PWBG_UG.png";
    public static final String IMG_PWBG_UR            = "IMG_PWBG_UR.png";
    public static final String IMG_PWBG_W             = "IMG_PWBG_W.png";
    public static final String IMG_PWBG_WB            = "IMG_PWBG_WB.png";
    public static final String IMG_PWBG_WG            = "IMG_PWBG_WG.png";
    public static final String IMG_PWBG_WR            = "IMG_PWBG_WR.png";
    public static final String IMG_PWBG_WU            = "IMG_PWBG_WU.png";
    public static final String IMG_NYX_B              = "IMG_NYX_B.png";
    public static final String IMG_NYX_G              = "IMG_NYX_G.png";
    public static final String IMG_NYX_M              = "IMG_NYX_M.png";
    public static final String IMG_NYX_R              = "IMG_NYX_R.png";
    public static final String IMG_NYX_U              = "IMG_NYX_U.png";
    public static final String IMG_NYX_W              = "IMG_NYX_W.png";
    public static final String IMG_NYX_C              = "IMG_NYX_C.png";

    // data tree roots
    public static final String USER_DIR;
    public static final String CACHE_DIR;
    public static final String CACHE_CARD_PICS_DIR;
    public static final Map<String, String> CACHE_CARD_PICS_SUBDIR;
    public static final String DECK_BASE_DIR;
    public static final String DECK_CONSTRUCTED_DIR;
    static {
        ForgeProfileProperties.load(GuiBase.isUsingAppDirectory());
        USER_DIR               = ForgeProfileProperties.getUserDir();
        CACHE_DIR              = ForgeProfileProperties.getCacheDir();
        CACHE_CARD_PICS_DIR    = ForgeProfileProperties.getCardPicsDir();
        CACHE_CARD_PICS_SUBDIR = Collections.unmodifiableMap(ForgeProfileProperties.getCardPicsSubDirs());
        DECK_BASE_DIR          = ForgeProfileProperties.getDecksDir();
        DECK_CONSTRUCTED_DIR   = ForgeProfileProperties.getDecksConstructedDir();
    }

    // data that is only in the profile dirs
    public static final String USER_QUEST_DIR             = USER_DIR + "quest" + PATH_SEPARATOR;
    public static final String USER_QUEST_WORLD_DIR       = USER_QUEST_DIR + "world" + PATH_SEPARATOR;
    public static final String USER_CONQUEST_DIR          = USER_DIR + "conquest" + PATH_SEPARATOR;
    public static final String USER_PREFS_DIR             = USER_DIR + "preferences" + PATH_SEPARATOR;
    public static final String USER_GAMES_DIR             = USER_DIR + "games" + PATH_SEPARATOR;
    public static final String USER_PUZZLE_DIR            = USER_DIR + "puzzle" + PATH_SEPARATOR;
    public static final String LOG_FILE                   = USER_DIR + "forge.log";
    public static final String NETWORK_LOGS_DIR           = USER_DIR + "networklogs" + PATH_SEPARATOR;
    public static final String ACHIEVEMENTS_DIR           = USER_DIR + "achievements" + PATH_SEPARATOR;
    public static final String USER_CUSTOM_DIR            = USER_DIR + "custom" + PATH_SEPARATOR;
    public static final String USER_CUSTOM_EDITIONS_DIR   = USER_CUSTOM_DIR + "editions" + PATH_SEPARATOR;
    public static final String USER_CUSTOM_CARDS_DIR      = USER_CUSTOM_DIR + "cards" + PATH_SEPARATOR;
    public static final String USER_CUSTOM_TOKENS_DIR     = USER_CUSTOM_DIR + "tokens" + PATH_SEPARATOR;
    public static final String USER_FORMATS_DIR           = USER_CUSTOM_DIR + "formats" + PATH_SEPARATOR;
    public static final String CUSTOM_STARTER_DECK_DIR    = USER_CUSTOM_DIR + "starterdecks" + PATH_SEPARATOR;
    public static final String USER_ADVENTURE_DIR         = USER_DIR + "adventure" + PATH_SEPARATOR;
    public static final String DECK_DRAFT_DIR             = DECK_BASE_DIR + "draft" + PATH_SEPARATOR;
    public static final String DECK_WINSTON_DIR           = DECK_BASE_DIR + "winston" + PATH_SEPARATOR;
    public static final String DECK_SEALED_DIR            = DECK_BASE_DIR + "sealed" + PATH_SEPARATOR;
    public static final String DECK_SCHEME_DIR            = DECK_BASE_DIR + "scheme" + PATH_SEPARATOR;
    public static final String DECK_PLANE_DIR             = DECK_BASE_DIR + "planar" + PATH_SEPARATOR;
    public static final String DECK_COMMANDER_DIR         = DECK_BASE_DIR + "commander" + PATH_SEPARATOR;
    public static final String COMMANDER_PRECON_DIR       = QUEST_DIR + "commanderprecons" + PATH_SEPARATOR;
    public static final String DECK_OATHBREAKER_DIR       = DECK_BASE_DIR + "oathbreaker" + PATH_SEPARATOR;
    public static final String DECK_NET_DIR               = DECK_BASE_DIR + "net" + PATH_SEPARATOR;
    public static final String DECK_NET_ARCHIVE_DIR       = DECK_BASE_DIR + "archive" + PATH_SEPARATOR;
    public static final String QUEST_SAVE_DIR             = USER_QUEST_DIR + "saves" + PATH_SEPARATOR;
    public static final String CONQUEST_SAVE_DIR          = USER_CONQUEST_DIR + "saves" + PATH_SEPARATOR;
    public static final String DECK_TINY_LEADERS_DIR      = DECK_BASE_DIR + "tiny_leaders" + PATH_SEPARATOR;
    public static final String DECK_BRAWL_DIR             = DECK_BASE_DIR + "brawl" + PATH_SEPARATOR;
    public static final String MAIN_PREFS_FILE            = USER_PREFS_DIR + "forge.preferences";
    public static final String SERVER_PREFS_FILE          = USER_PREFS_DIR + "server.preferences";
    public static final String CARD_PREFS_FILE            = USER_PREFS_DIR + "card.preferences";
    public static final String DECK_PREFS_FILE            = USER_PREFS_DIR + "deck.preferences";
    public static final String QUEST_PREFS_FILE           = USER_PREFS_DIR + "quest.preferences";
    public static final String CONQUEST_PREFS_FILE        = USER_PREFS_DIR + "conquest.preferences";
    public static final String ITEM_VIEW_PREFS_FILE       = USER_PREFS_DIR + "item_view.preferences";
    public static final String CLOSE_CONN_COMMAND         = "<<_EM_ESOLC_<<";
    public static final String INVALID_HOST_COMMAND       = "<<_TSOH_DILAVNI_<<";
    public static final String CONN_ERROR_PREFIX          = "<<_CONN_ERROR_>>:";

    // data that has defaults in the program dir but overrides/additions in the user dir
    private static final String _DEFAULTS_DIR = RES_DIR + "defaults" + PATH_SEPARATOR;
    public static final String NO_CARD_FILE   = _DEFAULTS_DIR + "no_card.jpg";
    public static final String STARS_FILE     = _DEFAULTS_DIR + "stars.png";
    public static final FileLocation WINDOW_LAYOUT_FILE      = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "window.xml");
    public static final FileLocation MATCH_LAYOUT_FILE       = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "match.xml");
    public static final FileLocation WORKSHOP_LAYOUT_FILE    = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "workshop.xml");
    public static final FileLocation EDITOR_LAYOUT_FILE      = new FileLocation(_DEFAULTS_DIR, USER_PREFS_DIR, "editor.xml");
    public static final FileLocation GAUNTLET_DIR            = new FileLocation(_DEFAULTS_DIR, USER_DIR,       "gauntlet" + PATH_SEPARATOR);
    public static final FileLocation TOURNAMENT_DIR          = new FileLocation(_DEFAULTS_DIR, USER_DIR,       "tournament" + PATH_SEPARATOR);

    // data that is only in the cached dir
    private static final String PICS_DIR                     = CACHE_DIR + "pics" + PATH_SEPARATOR;
    public static final String DB_DIR                        = CACHE_DIR + "db" + PATH_SEPARATOR;
    public static final String FONTS_DIR                     = CACHE_DIR + "fonts" + PATH_SEPARATOR;
    public static final String CACHE_SKINS_DIR               = CACHE_DIR + "skins" + PATH_SEPARATOR;
    public static final String CACHE_TOKEN_PICS_DIR          = PICS_DIR + "tokens" + PATH_SEPARATOR;
    public static final String CACHE_ICON_PICS_DIR           = PICS_DIR + "icons" + PATH_SEPARATOR;
    public static final String CACHE_SYMBOLS_DIR             = PICS_DIR + "symbols" + PATH_SEPARATOR;
    public static final String CACHE_BOOSTER_PICS_DIR        = PICS_DIR + "boosters" + PATH_SEPARATOR;
    public static final String CACHE_FATPACK_PICS_DIR        = PICS_DIR + "fatpacks" + PATH_SEPARATOR;
    public static final String CACHE_BOOSTERBOX_PICS_DIR     = PICS_DIR + "boosterboxes" + PATH_SEPARATOR;
    public static final String CACHE_PRECON_PICS_DIR         = PICS_DIR + "precons" + PATH_SEPARATOR;
    public static final String CACHE_TOURNAMENTPACK_PICS_DIR = PICS_DIR + "tournamentpacks" + PATH_SEPARATOR;
    public static final String CACHE_PLANECHASE_PICS_DIR     = PICS_DIR + "planechase" + PATH_SEPARATOR;
    public static final String CACHE_ACHIEVEMENTS_DIR        = PICS_DIR + "achievements" + PATH_SEPARATOR;
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
            DECK_OATHBREAKER_DIR,
            DECK_NET_DIR,
            QUEST_SAVE_DIR,
            CACHE_TOKEN_PICS_DIR,
            CACHE_ICON_PICS_DIR,
            CACHE_BOOSTER_PICS_DIR,
            CACHE_FATPACK_PICS_DIR,
            CACHE_BOOSTERBOX_PICS_DIR,
            CACHE_PRECON_PICS_DIR,
            CACHE_TOURNAMENTPACK_PICS_DIR,
            CACHE_PLANECHASE_PICS_DIR };

    // URLs
    public static final String URL_CARDFORGE = "https://downloads.cardforge.org";
    private static final String GITHUB_ASSETS_BASE = "https://raw.githubusercontent.com/Card-Forge/forge-extras/refs/heads/main/";

    public static final String URL_PIC_DOWNLOAD = URL_CARDFORGE + "/images/cards/";
    public static final String URL_TOKEN_DOWNLOAD = URL_CARDFORGE + "/images/tokens/";
    public static final String URL_PRICE_DOWNLOAD = GITHUB_ASSETS_BASE + "all-prices.txt";
    private static final String URL_SCRYFALL = "https://api.scryfall.com";
    public static final String URL_PIC_SCRYFALL_DOWNLOAD = URL_SCRYFALL + "/cards/";

    // Constants for Display Card Identity game setting
    public static final String DISP_CURRENT_COLORS_ALWAYS = "Always";
    public static final String DISP_CURRENT_COLORS_CHANGED = "Changed";
    public static final String DISP_CURRENT_COLORS_MULTICOLOR = "Multicolor";
    public static final String DISP_CURRENT_COLORS_MULTI_OR_CHANGED = "Multi+Changed";
    public static final String DISP_CURRENT_COLORS_NEVER = "Never";

    // Constants for Auto-Yield Mode
    public static final String AUTO_YIELD_PER_CARD = "Per Card (Each Game)";
    public static final String AUTO_YIELD_PER_ABILITY = "Per Ability (Each Match)";

    // Constants for Graveyard Ordering
    public static final String GRAVEYARD_ORDERING_NEVER = "Never";
    public static final String GRAVEYARD_ORDERING_OWN_CARDS = "With Relevant Cards";
    public static final String GRAVEYARD_ORDERING_ALWAYS = "Always";

    // Constants for Stack effect addition notification policy
    public static final String STACK_EFFECT_NOTIFICATION_NEVER = "Never";
    public static final String STACK_EFFECT_NOTIFICATION_ALWAYS = "Always";
    public static final String STACK_EFFECT_NOTIFICATION_AI_AND_TRIGGERED = "AI cast/activated, or triggered by any player";

    // Constants for Land played notification policy
    public static final String LAND_PLAYED_NOTIFICATION_NEVER = "Never";
    public static final String LAND_PLAYED_NOTIFICATION_ALWAYS = "Always";
    public static final String LAND_PLAYED_NOTIFICATION_ALWAYS_FOR_NONBASIC_LANDS = "Always, but only for nonbasic lands";
    public static final String LAND_PLAYED_NOTIFICATION_AI = "Lands entering a battlefield because of an action of a AI player";
    public static final String LAND_PLAYED_NOTIFICATION_AI_FOR_NONBASIC_LANDS = "Nonbasic lands entering a battlefield because of an action of a AI player";

    public static final String SWITCH_CARDSTATES_DECK_NEVER = "Never";
    public static final String SWITCH_CARDSTATES_DECK_ALWAYS = "Always";
    public static final String SWITCH_CARDSTATES_DECK_HOVER = "Switch back on hover";

    // Set boolean constant for landscape mode for gdx port
    public static final boolean isGdxPortLandscape = FileUtil.doesFileExist(ASSETS_DIR + "switch_orientation.ini");

    // Supported video mode names and dimensions (currently used in Adventure Mode)
    public static final String[] VIDEO_MODES = {"720p", "768p", "900p", "1080p", "1440p", "2160p"};

    public static Map<String, String> getUPnPPreferenceMapping() {
        return Map.of(
                localizer.getMessage("lblAsk"), "ASK",
                localizer.getMessage("lblAlways"), "ALWAYS",
                localizer.getMessage("lblNever"), "NEVER"
        );
    }

    public enum CounterDisplayLocation {

        TOP("Top of Card"), BOTTOM("Bottom of Card");

        private final String name;

        CounterDisplayLocation(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static CounterDisplayLocation from(final String name) {
            for (CounterDisplayLocation counterDisplayLocation : values()) {
                if (counterDisplayLocation.name.equals(name)) {
                    return counterDisplayLocation;
                }
            }
            throw new IllegalArgumentException("Counter display location '" + name + "' not found.");
        }

    }

    public enum CounterDisplayType {

        /** Use only the new tab-like counter display */
        TEXT("Text-based"),

        /** Use only the old image-based counter display */
        IMAGE("Image-based"),

        /** Use both counter displays at the same time */
        HYBRID("Hybrid"),

        /** Use the new tab-like counter display unless the card panel is very small, then use the old image-based counter display */
        OLD_WHEN_SMALL("Images when cards are small; text otherwise");

        private final String name;

        CounterDisplayType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static CounterDisplayType from(final String name) {
            for (CounterDisplayType counterDisplayType : values()) {
                if (counterDisplayType.name.equals(name)) {
                    return counterDisplayType;
                }
            }
            throw new IllegalArgumentException("Counter display type '" + name + "' not found.");
        }

    }

}
