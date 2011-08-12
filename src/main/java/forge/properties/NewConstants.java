package forge.properties;


/**
 * NewConstants.java
 *
 * Created on 22.08.2009
 */

/**
 * These are mostly property keys.
 *
 * @author Clemens Koza
 * @version V0.0 22.08.2009
 */
public interface NewConstants {
    //General properties
    /** Constant <code>HOW_TO_REPORT_BUGS_URL = "program/howToReportBugsURL"</code>. */
    String HOW_TO_REPORT_BUGS_URL = "program/howToReportBugsURL";

    /** Constant <code>SHOW2CDECK="showdeck/2color"</code>. */
    String SHOW2CDECK = "showdeck/2color";

    /** Constant <code>DECKS="decks"</code>. */
    String DECKS = "decks";
    /** Constant <code>BOOSTER_DECKS="booster-decks"</code>. */
    String BOOSTER_DECKS = "booster-decks";
    /** Constant <code>NEW_DECKS="decks-dir"</code>. */
    String NEW_DECKS = "decks-dir";

    /** Constant <code>TOKENS="tokens"</code>. */
    String TOKENS = "tokens";
    /** Constant <code>CARD_PICTURES="card-pictures"</code>. */
    String CARD_PICTURES = "card-pictures";
    /** Constant <code>CARD_PICTURES_TOKEN_LQ="card-pictures_token_lq"</code>. */
    String CARD_PICTURES_TOKEN_LQ = "card-pictures_token_lq";
    /** Constant <code>CARD_PICTURES_A="card-pictures_a"</code>. */
    String CARD_PICTURES_A = "card-pictures_a";
    /** Constant <code>CARD_PICTURES_B="card-pictures_b"</code>. */
    String CARD_PICTURES_B = "card-pictures_b";
    /** Constant <code>CARD_PICTURES_C="card-pictures_c"</code>. */
    String CARD_PICTURES_C = "card-pictures_c";
    /** Constant <code>CARD_PICTURES_D="card-pictures_d"</code>. */
    String CARD_PICTURES_D = "card-pictures_d";
    /** Constant <code>CARD_PICTURES_E="card-pictures_e"</code>. */
    String CARD_PICTURES_E = "card-pictures_e";
    /** Constant <code>CARD_PICTURES_F="card-pictures_f"</code>. */
    String CARD_PICTURES_F = "card-pictures_f";
    /** Constant <code>CARD_PICTURES_G="card-pictures_g"</code>. */
    String CARD_PICTURES_G = "card-pictures_g";
    /** Constant <code>CARD_PICTURES_H="card-pictures_h"</code>. */
    String CARD_PICTURES_H = "card-pictures_h";
    /** Constant <code>CARD_PICTURES_I="card-pictures_i"</code>. */
    String CARD_PICTURES_I = "card-pictures_i";
    /** Constant <code>CARD_PICTURES_J="card-pictures_j"</code>. */
    String CARD_PICTURES_J = "card-pictures_j";
    /** Constant <code>CARD_PICTURES_K="card-pictures_k"</code>. */
    String CARD_PICTURES_K = "card-pictures_k";
    /** Constant <code>CARD_PICTURES_L="card-pictures_l"</code>. */
    String CARD_PICTURES_L = "card-pictures_l";
    /** Constant <code>CARD_PICTURES_M="card-pictures_m"</code>. */
    String CARD_PICTURES_M = "card-pictures_m";
    /** Constant <code>CARD_PICTURES_N="card-pictures_n"</code>. */
    String CARD_PICTURES_N = "card-pictures_n";
    /** Constant <code>CARD_PICTURES_O="card-pictures_o"</code>. */
    String CARD_PICTURES_O = "card-pictures_o";
    /** Constant <code>CARD_PICTURES_P="card-pictures_p"</code>. */
    String CARD_PICTURES_P = "card-pictures_p";
    /** Constant <code>CARD_PICTURES_Q="card-pictures_q"</code>. */
    String CARD_PICTURES_Q = "card-pictures_q";
    /** Constant <code>CARD_PICTURES_R="card-pictures_r"</code>. */
    String CARD_PICTURES_R = "card-pictures_r";
    /** Constant <code>CARD_PICTURES_S="card-pictures_s"</code>. */
    String CARD_PICTURES_S = "card-pictures_s";
    /** Constant <code>CARD_PICTURES_T="card-pictures_t"</code>. */
    String CARD_PICTURES_T = "card-pictures_t";
    /** Constant <code>CARD_PICTURES_U="card-pictures_u"</code>. */
    String CARD_PICTURES_U = "card-pictures_u";
    /** Constant <code>CARD_PICTURES_V="card-pictures_v"</code>. */
    String CARD_PICTURES_V = "card-pictures_v";
    /** Constant <code>CARD_PICTURES_W="card-pictures_w"</code>. */
    String CARD_PICTURES_W = "card-pictures_w";
    /** Constant <code>CARD_PICTURES_X="card-pictures_x"</code>. */
    String CARD_PICTURES_X = "card-pictures_x";
    /** Constant <code>CARD_PICTURES_Y="card-pictures_y"</code>. */
    String CARD_PICTURES_Y = "card-pictures_y";
    /** Constant <code>CARD_PICTURES_Z="card-pictures_z"</code>. */
    String CARD_PICTURES_Z = "card-pictures_z";
    /** Constant <code>CARD_PICTURES_OTHER="card-pictures_other"</code>. */
    String CARD_PICTURES_OTHER = "card-pictures_other";
    /** Constant <code>CARD_PICTURES_TOKEN_HQ="card-pictures_token_hq"</code>. */
    String CARD_PICTURES_TOKEN_HQ = "card-pictures_token_hq";
    /** Constant <code>CARDS="cards"</code>. */
    String CARDS = "cards";
    /** Constant <code>CARDSFOLDER="cardsfolder"</code>. */
    String CARDSFOLDER = "cardsfolder";
    /** Constant <code>REMOVED="removed-cards"</code>. */
    String REMOVED = "removed-cards";
    /** Constant <code>NAME_MUTATOR="name-mutator"</code>. */
    String NAME_MUTATOR = "name-mutator";
    /** Constant <code>BOOSTERDATA="boosterdata"</code>. */
    String BOOSTERDATA = "boosterdata";

    /** Constant <code>IMAGE_BASE="image/base"</code>. */
    String IMAGE_BASE = "image/base";
    /** Constant <code>IMAGE_TOKEN="image/token"</code>. */
    String IMAGE_TOKEN = "image/token";
    /** Constant <code>IMAGE_ICON="image/icon"</code>. */
    String IMAGE_ICON = "image/icon";
    /** Constant <code>SOUND_BASE="sound/base"</code>. */
    String SOUND_BASE = "sound/base";

    /**
     * These properties are for a regular game.
     */
    public static interface REGULAR {
        /** Property path for a common card. */
        String COMMON = "regular/common";
        /** Property path for an uncommon card. */
        String UNCOMMON = "regular/uncommon";
        /** Property path for a rare card. */
        String RARE = "regular/rare";
    }

    /**
     * These properties are for a booster draft.
     */
    public static interface DRAFT {
        /** Property path for a common card. */
        String COMMON = "draft/common";
        /** Property path for an uncommon card. */
        String UNCOMMON = "draft/uncommon";
        /** Property path for a rare card. */
        String RARE = "draft/rare";
    }

    /**
     * These properties are for a quest game.
     */
    public static interface QUEST {
        /** Property path for a common card. */
        String COMMON = "quest/common";
        /** Property path for an uncommon card. */
        String UNCOMMON = "quest/uncommon";
        /** Property path for a rare card. */
        String RARE = "quest/rare";

        /** Property path for price. */
        String PRICE = "quest/price";
        /** Property path for quests. */
        String QUESTS = "quest/quests";

        /** Property path for easy quest difficulty. */
        String EASY = "quest/easy";
        /** Property path for medium quest difficulty. */
        String MEDIUM = "quest/medium";
        /** Property path for hard quest difficulty. */
        String HARD = "quest/hard";
        /** Property path for very hard quest difficulty. */
        String VERYHARD = "quest/veryhard";

        String DATA = "quest/data";
        String PREFS = "quest/prefs";

        String DECKS = "quest/decks-dir";
        String XMLDATA = "quest/data-xml";
    }

    /**
     * These are GUI-related properties.
     */
    public static interface GUI {
        public static interface GuiDisplay {
            String LAYOUT = "gui/Display";
            String LAYOUT_NEW = "gui/Display/new";
        }

        public static interface GuiDeckEditor {
            String LAYOUT = "gui/DeckEditor";

        }
    }

    /**
     * These are localization properties.
     */
    public static interface LANG {
        String PROGRAM_NAME = "%s/program/name";
        String LANGUAGE = "lang";

        public static interface HowTo {
            String TITLE = "%s/HowTo/title";
            String MESSAGE = "%s/HowTo/message";
        }

        public static interface ErrorViewer {
            String SHOW_ERROR = "%s/ErrorViewer/show";

            String TITLE = "%s/ErrorViewer/title";
            String MESSAGE = "%s/ErrorViewer/message";
            String BUTTON_SAVE = "%s/ErrorViewer/button/save";
            String BUTTON_CLOSE = "%s/ErrorViewer/button/close";
            String BUTTON_EXIT = "%s/ErrorViewer/button/exit";

            public static interface ERRORS {
                String SAVE_MESSAGE = "%s/ErrorViewer/errors/save/message";
                String SHOW_MESSAGE = "%s/ErrorViewer/errors/show/message";
            }
        }

        public static interface Gui_BoosterDraft {
            String CLOSE_MESSAGE = "%s/BoosterDraft/close/message";
            String SAVE_MESSAGE = "%s/BoosterDraft/save/message";
            String SAVE_TITLE = "%s/BoosterDraft/save/title";
            String RENAME_MESSAGE = "%s/BoosterDraft/rename/message";
            String RENAME_TITLE = "%s/BoosterDraft/rename/title";
            String SAVE_DRAFT_MESSAGE = "%s/BoosterDraft/saveDraft/message";
            String SAVE_DRAFT_TITLE = "%s/BoosterDraft/saveDraft/title";
        }

        public static interface GuiDisplay {
            public static interface MENU_BAR {
                public static interface MENU {
                    String TITLE = "%s/Display/menu/title";
                }

                public static interface PHASE {
                    String TITLE = "%s/Display/phase/title";
                }

                public static interface DEV {
                    String TITLE = "%s/Display/dev/title";
                }
            }

            String HUMAN_TITLE = "%s/Display/human/title";

            public static interface HUMAN_HAND {
                String TITLE = "%s/Display/human/hand/title";
            }

            public static interface HUMAN_LIBRARY {
                String BASE = "%s/Display/human/library";
                String TITLE = "%s/Display/human/library/title";
                String MENU = "%s/Display/human/library/menu";
                String BUTTON = "%s/Display/human/library/button";
            }

            String HUMAN_GRAVEYARD = "%s/Display/human/graveyard";

            public static interface HUMAN_GRAVEYARD {
                String TITLE = "%s/Display/human/graveyard/title";
                String BUTTON = "%s/Display/human/graveyard/button";
                String MENU = "%s/Display/human/graveyard/menu";
            }

            String HUMAN_REMOVED = "%s/Display/human/removed";

            public static interface HUMAN_REMOVED {
                String TITLE = "%s/Display/human/removed/title";
                String BUTTON = "%s/Display/human/removed/button";
                String MENU = "%s/Display/human/removed/menu";
            }

            String COMBAT = "%s/Display/combat/title";

            String HUMAN_FLASHBACK = "%s/Display/human/flashback";

            public static interface HUMAN_FLASHBACK {
                String TITLE = "%s/Display/human/flashback/title";
                String BUTTON = "%s/Display/human/flashback/button";
                String MENU = "%s/Display/human/flashback/menu";
            }

            String COMPUTER_TITLE = "%s/Display/computer/title";


            public static interface COMPUTER_HAND {
                String BASE = "%s/Display/computer/hand";
                String TITLE = "%s/Display/computer/hand/title";
                String BUTTON = "%s/Display/computer/hand/button";
                String MENU = "%s/Display/computer/hand/menu";
            }


            public static interface COMPUTER_LIBRARY {
                String BASE = "%s/Display/computer/library";
                String TITLE = "%s/Display/computer/library/title";
                String BUTTON = "%s/Display/computer/library/button";
                String MENU = "%s/Display/computer/library/menu";
            }


            String COMPUTER_GRAVEYARD = "%s/Display/computer/graveyard";

            public static interface COMPUTER_GRAVEYARD {
                String TITLE = "%s/Display/computer/graveyard/title";
                String BUTTON = "%s/Display/computer/graveyard/button";
                String MENU = "%s/Display/computer/graveyard/menu";
            }


            String COMPUTER_REMOVED = "%s/Display/computer/removed";

            public static interface COMPUTER_REMOVED {
                String TITLE = "%s/Display/computer/removed/title";
                String BUTTON = "%s/Display/computer/removed/button";
                String MENU = "%s/Display/computer/removed/menu";
            }

            String CONCEDE = "%s/Display/concede";

            public static interface CONCEDE {
                String BUTTON = "%s/Display/concede/button";
                String MENU = "%s/Display/concede/menu";
            }

            String MANAGEN = "%s/Display/managen";

            public static interface MANAGEN {
                String BUTTON = "%s/Display/managen/button";
                String MENU = "%s/Display/managen/menu";
            }

            String SETUPBATTLEFIELD = "%s/Display/setupbattlefield";

            public static interface SETUPBATTLEFIELD {
                String BUTTON = "%s/Display/setupbattlefield/button";
                String MENU = "%s/Display/setupbattlefield/menu";
            }

            String TUTOR = "%s/Display/tutor";

            public static interface TUTOR {
                String BUTTON = "%s/Display/tutor/button";
                String MENU = "%s/Display/tutor/menu";
            }

            String ADDCOUNTER = "%s/Display/addcounter";

            public static interface ADDCOUNTER {
                String BUTTON = "%s/Display/addcounter/button";
                String MENU = "%s/Display/addcounter/menu";
            }

            String TAPPERM = "%s/Display/tapperm";

            public static interface TAPPERM {
                String BUTTON = "%s/Display/tapperm/button";
                String MENU = "%s/Display/tapperm/menu";
            }

            String UNTAPPERM = "%s/Display/untapperm";

            public static interface UNTAPPERM {
                String BUTTON = "%s/Display/untapperm/button";
                String MENU = "%s/Display/untapperm/menu";
            }

            String NOLANDLIMIT = "%s/Display/nolandlimit";

            public static interface NOLANDLIMIT {
                String BUTTON = "%s/Display/nolandlimit/button";
                String MENU = "%s/Display/nolandlimit/menu";
            }
        }

        public static interface Gui_DownloadPictures {
            String TITLE = "%s/DownloadPictures/title";

            String PROXY_ADDRESS = "%s/DownloadPictures/proxy/address";
            String PROXY_PORT = "%s/DownloadPictures/proxy/port";

            String NO_PROXY = "%s/DownloadPictures/proxy/type/none";
            String HTTP_PROXY = "%s/DownloadPictures/proxy/type/http";
            String SOCKS_PROXY = "%s/DownloadPictures/proxy/type/socks";

            String NO_MORE = "%s/DownloadPictures/no-more";

            String BAR_BEFORE_START = "%s/DownloadPictures/bar/before-start";
            String BAR_WAIT = "%s/DownloadPictures/bar/wait";
            String BAR_CLOSE = "%s/DownloadPictures/bar/close";

            public static interface BUTTONS {
                String START = "%s/DownloadPictures/button/start";
                String CANCEL = "%s/DownloadPictures/button/cancel";
                String CLOSE = "%s/DownloadPictures/button/close";
            }

            public static interface ERRORS {
                String PROXY_CONNECT = "%s/DownloadPictures/errors/proxy/connect";
                String OTHER = "%s/DownloadPictures/errors/other";
            }
        }


        public static interface OldGuiNewGame {
            public static interface NEW_GAME_TEXT {
                String GAMETYPE = "%s/NewGame/gametype";
                String LIBRARY = "%s/NewGame/library";
                String SETTINGS = "%s/NewGame/settings";
                String NEW_GAME = "%s/NewGame/new_game";
                String CONSTRUCTED_TEXT = "%s/NewGame/constructed_text";
                String SEALED_TEXT = "%s/NewGame/sealed_text";
                String BOOSTER_TEXT = "%s/NewGame/booster_text";
                String YOURDECK = "%s/NewGame/yourdeck";
                String OPPONENT = "%s/NewGame/opponent";
                String DECK_EDITOR = "%s/NewGame/deckeditor";
                String NEW_GUI = "%s/NewGame/newgui";
                String AI_LAND = "%s/NewGame/ailand";
                String DEV_MODE = "%s/NewGame/devmode";
                String QUEST_MODE = "%s/NewGame/questmode";
                String START_GAME = "%s/NewGame/startgame";
                String SAVE_SEALED_MSG = "%s/NewGame/savesealed_msg";
                String SAVE_SEALED_TTL = "%s/NewGame/savesealed_ttl";

            }

            public static interface MENU_BAR {
                public static interface MENU {
                    String TITLE = "%s/NewGame/menu/title";
                    String LF = "%s/NewGame/menu/lookAndFeel";
                    String DOWNLOADPRICE = "%s/NewGame/menu/downloadPrice";
                    String DOWNLOAD = "%s/NewGame/menu/download";
                    String DOWNLOADLQ = "%s/NewGame/menu/downloadlq";
                    String DOWNLOADSETLQ = "%s/NewGame/menu/downloadsetlq";
                    String IMPORTPICTURE = "%s/NewGame/menu/importPicture";
                    String CARD_SIZES = "%s/NewGame/menu/cardSizes";
                    String CARD_STACK = "%s/NewGame/menu/cardStack";
                    String CARD_STACK_OFFSET = "%s/NewGame/menu/cardStackOffset";
                    String ABOUT = "%s/NewGame/menu/about";
                }

                public static interface OPTIONS {
                    String TITLE = "%s/NewGame/options/title";
                    String FONT = "%s/NewGame/options/font";
                    String CARD_OVERLAY = "%s/NewGame/options/cardOverlay";
                    String CARD_SCALE = "%s/NewGame/options/cardScale";

                    public static interface GENERATE {
                        String TITLE = "%s/NewGame/options/generate/title";
                        String REMOVE_SMALL = "%s/NewGame/options/generate/removeSmall";
                        String REMOVE_ARTIFACTS = "%s/NewGame/options/generate/removeArtifacts";
                    }
                }

                public static interface HELP {
                    String TITLE = "%s/NewGame/help/title";
                }


            }

            public static interface ERRORS {
            }
        }

        public static interface Gui_WinLose {
            public static interface WINLOSE_TEXT {
                String WON = "%s/WinLose/won";
                String LOST = "%s/WinLose/lost";
                String WIN = "%s/WinLose/win";
                String LOSE = "%s/WinLose/lose";
                String CONTINUE = "%s/WinLose/continue";
                String RESTART = "%s/WinLose/restart";
                String QUIT = "%s/WinLose/quit";
            }
        }

        public static interface Gui_DownloadPrices {
            public static interface DOWNLOADPRICES {
                String TITLE = "%s/DownloadPrices/title";
                String START_UPDATE = "%s/DownloadPrices/startupdate";
                String DOWNLOADING = "%s/DownloadPrices/downloading";
                String COMPILING = "%s/DownloadPrices/compiling";
            }
        }

        public static interface GameAction {
            public static interface GAMEACTION_TEXT {
                String HEADS = "%s/GameAction/heads";
                String TAILS = "%s/GameAction/tails";
                String HEADS_OR_TAILS = "%s/GameAction/heads_or_tails";
                String COIN_TOSS = "%s/GameAction/coin_toss";
                String HUMAN_WIN = "%s/GameAction/human_win";
                String COMPUTER_WIN = "%s/GameAction/computer_win";
                String COMPUTER_STARTS = "%s/GameAction/computer_starts";
                String HUMAN_STARTS = "%s/GameAction/human_starts";
                String HUMAN_MANA_COST = "%s/GameAction/human_mana_cost";
                String COMPUTER_MANA_COST = "%s/GameAction/computer_mana_cost";
                String COMPUTER_CUT = "%s/GameAction/computer_cut";
                String HUMAN_CUT = "%s/GameAction/human_cut";
                String CUT_NUMBER = "%s/GameAction/cut_number";
                String RESOLVE_STARTER = "%s/GameAction/resolve_starter";
                String EQUAL_CONVERTED_MANA = "%s/GameAction/equal_converted_mana";
                String CUTTING_AGAIN = "%s/GameAction/cutting_again";
                String YES = "%s/GameAction/yes";
                String NO = "%s/GameAction/no";
                String WANT_DREDGE = "%s/GameAction/want_dredge";
                String SELECT_DREDGE = "%s/GameAction/select_dredge";
                String CHOOSE_2ND_LAND = "%s/GameAction/choose_2nd_land";


            }
        }
    }
}

