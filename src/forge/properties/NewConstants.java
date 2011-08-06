
package forge.properties;


/**
 * NewConstants.java
 * 
 * Created on 22.08.2009
 */


/**
 * property keys
 * 
 * @version V0.0 22.08.2009
 * @author Clemens Koza
 */
public interface NewConstants {
    //General properties
    public static final String MAIL          = "program/mail";
    public static final String FORUM         = "program/forum";
    public static final String VERSION       = "program/version";
    
    public static final String SHOW2CDECK    = "showdeck/2color";
    
    public static final String DECKS         = "decks";
    public static final String BOOSTER_DECKS = "booster-decks";
    public static final String NEW_DECKS     = "decks-dir";
    
    public static final String TOKENS        = "tokens";
    public static final String CARD_PICTURES = "card-pictures";
    public static final String CARD_PICTURES_TOKEN_LQ = "card-pictures_token_lq";
    public static final String CARD_PICTURES_A = "card-pictures_a";
    public static final String CARD_PICTURES_B = "card-pictures_b";
    public static final String CARD_PICTURES_C = "card-pictures_c";
    public static final String CARD_PICTURES_D = "card-pictures_d";
    public static final String CARD_PICTURES_E = "card-pictures_e";
    public static final String CARD_PICTURES_F = "card-pictures_f";
    public static final String CARD_PICTURES_G = "card-pictures_g";
    public static final String CARD_PICTURES_H = "card-pictures_h";
    public static final String CARD_PICTURES_I = "card-pictures_i";
    public static final String CARD_PICTURES_J = "card-pictures_j";
    public static final String CARD_PICTURES_K = "card-pictures_k";
    public static final String CARD_PICTURES_L = "card-pictures_l";
    public static final String CARD_PICTURES_M = "card-pictures_m";
    public static final String CARD_PICTURES_N = "card-pictures_n";
    public static final String CARD_PICTURES_O = "card-pictures_o";
    public static final String CARD_PICTURES_P = "card-pictures_p";
    public static final String CARD_PICTURES_Q = "card-pictures_q";
    public static final String CARD_PICTURES_R = "card-pictures_r";
    public static final String CARD_PICTURES_S = "card-pictures_s";
    public static final String CARD_PICTURES_T = "card-pictures_t";
    public static final String CARD_PICTURES_U = "card-pictures_u";
    public static final String CARD_PICTURES_V = "card-pictures_v";
    public static final String CARD_PICTURES_W = "card-pictures_w";
    public static final String CARD_PICTURES_X = "card-pictures_x";
    public static final String CARD_PICTURES_Y = "card-pictures_y";
    public static final String CARD_PICTURES_Z = "card-pictures_z";
    public static final String CARD_PICTURES_OTHER = "card-pictures_other";
    public static final String CARD_PICTURES_TOKEN_HQ = "card-pictures_token_hq";
    public static final String CARDS         = "cards";
    public static final String CARDSFOLDER   = "cardsfolder";
    public static final String REMOVED       = "removed-cards";
    public static final String NAME_MUTATOR  = "name-mutator";
    
    public static final String IMAGE_BASE    = "image/base";
    public static final String IMAGE_TOKEN   = "image/token";
    public static final String IMAGE_ICON    = "image/icon";
    public static final String SOUND_BASE    = "sound/base";
    
    /**
     * properties for regular game
     */
    public static interface REGULAR {
        public static final String COMMON   = "regular/common";
        public static final String UNCOMMON = "regular/uncommon";
        public static final String RARE     = "regular/rare";
    }
    /**
     * properties for booster draft
     */
    public static interface DRAFT {
    	public static final String COMMON 	= "draft/common";
    	public static final String UNCOMMON = "draft/uncommon";
        public static final String RARE     = "draft/rare";
    }
    
    /**
     * properties for quest game
     */
    public static interface QUEST {
        public static final String COMMON   = "quest/common";
        public static final String UNCOMMON = "quest/uncommon";
        public static final String RARE     = "quest/rare";
        
        public static final String PRICE    = "quest/price";
        public static final String QUESTS   = "quest/quests";
        
        public static final String EASY     = "quest/easy";
        public static final String MEDIUM   = "quest/medium";
        public static final String HARD     = "quest/hard";
        
        public static final String DATA     = "quest/data";
        public static final String PREFS    = "quest/prefs";
        
        public static final String DECKS    = "quest/decks-dir";
    }
    
    /**
     * gui-related properties
     */
    public static interface GUI {
        public static interface GuiDisplay {
            public static final String LAYOUT = "gui/Display";
            public static final String LAYOUT_NEW = "gui/Display/new";
        }
        
        public static interface GuiDeckEditor {
            public static final String LAYOUT = "gui/DeckEditor";
        }
    }
    
    /**
     * Localization properties
     */
    public static interface LANG {
        public static final String PROGRAM_NAME = "%s/program/name";
        public static final String LANGUAGE     = "lang";
        
        public static interface HowTo {
            public static final String TITLE   = "%s/HowTo/title";
            public static final String MESSAGE = "%s/HowTo/message";
        }
        
        public static interface ErrorViewer {
            public static final String SHOW_ERROR   = "%s/ErrorViewer/show";
            
            public static final String TITLE        = "%s/ErrorViewer/title";
            public static final String MESSAGE      = "%s/ErrorViewer/message";
            public static final String BUTTON_SAVE  = "%s/ErrorViewer/button/save";
            public static final String BUTTON_CLOSE = "%s/ErrorViewer/button/close";
            public static final String BUTTON_EXIT  = "%s/ErrorViewer/button/exit";
            
            public static interface ERRORS {
                public static final String SAVE_MESSAGE = "%s/ErrorViewer/errors/save/message";
                public static final String SHOW_MESSAGE = "%s/ErrorViewer/errors/show/message";
            }
        }
        
        public static interface Gui_BoosterDraft {
            public static final String CLOSE_MESSAGE      = "%s/BoosterDraft/close/message";
            public static final String SAVE_MESSAGE       = "%s/BoosterDraft/save/message";
            public static final String SAVE_TITLE         = "%s/BoosterDraft/save/title";
            public static final String RENAME_MESSAGE     = "%s/BoosterDraft/rename/message";
            public static final String RENAME_TITLE       = "%s/BoosterDraft/rename/title";
            public static final String SAVE_DRAFT_MESSAGE = "%s/BoosterDraft/saveDraft/message";
            public static final String SAVE_DRAFT_TITLE   = "%s/BoosterDraft/saveDraft/title";
        }
        
        public static interface GuiDisplay {
            public static interface MENU_BAR {
                public static interface MENU {
                    public static final String TITLE = "%s/Display/menu/title";
                }
            }
            
            public static final String HUMAN_TITLE = "%s/Display/human/title";
            
            public static interface HUMAN_HAND {
                public static final String TITLE = "%s/Display/human/hand/title";
            }
            
            public static interface HUMAN_LIBRARY {
                public static final String TITLE = "%s/Display/human/library/title";
            }
            
            public static final String HUMAN_GRAVEYARD = "%s/Display/human/graveyard";
            
            public static interface HUMAN_GRAVEYARD {
                public static final String TITLE  = "%s/Display/human/graveyard/title";
                public static final String BUTTON = "%s/Display/human/graveyard/button";
                public static final String MENU   = "%s/Display/human/graveyard/menu";
            }
            
            public static final String HUMAN_REMOVED = "%s/Display/human/removed";
            
            public static interface HUMAN_REMOVED {
                public static final String TITLE  = "%s/Display/human/removed/title";
                public static final String BUTTON = "%s/Display/human/removed/button";
                public static final String MENU   = "%s/Display/human/removed/menu";
            }
            
            public static final String COMBAT          = "%s/Display/combat/title";
            
            public static final String HUMAN_FLASHBACK = "%s/Display/human/flashback";
            
            public static interface HUMAN_FLASHBACK {
                public static final String TITLE  = "%s/Display/human/flashback/title";
                public static final String BUTTON = "%s/Display/human/flashback/button";
                public static final String MENU   = "%s/Display/human/flashback/menu";
            }
            
            public static final String COMPUTER_TITLE = "%s/Display/computer/title";
            
            public static interface COMPUTER_HAND {
                public static final String TITLE = "%s/Display/computer/hand/title";
            }
            
            public static interface COMPUTER_LIBRARY {
                public static final String TITLE = "%s/Display/computer/library/title";
            }
            
            
            public static final String COMPUTER_GRAVEYARD = "%s/Display/computer/graveyard";
            
            public static interface COMPUTER_GRAVEYARD {
                public static final String TITLE  = "%s/Display/computer/graveyard/title";
                public static final String BUTTON = "%s/Display/computer/graveyard/button";
                public static final String MENU   = "%s/Display/computer/graveyard/menu";
            }
            
            
            public static final String COMPUTER_REMOVED = "%s/Display/computer/removed";
            
            public static interface COMPUTER_REMOVED {
                public static final String TITLE  = "%s/Display/computer/removed/title";
                public static final String BUTTON = "%s/Display/computer/removed/button";
                public static final String MENU   = "%s/Display/computer/removed/menu";
            }
            
            public static final String CONCEDE = "%s/Display/concede";
            
            public static interface CONCEDE {
                public static final String BUTTON = "%s/Display/concede/button";
                public static final String MENU   = "%s/Display/concede/menu";
            }
        }
        
        public static interface Gui_DownloadPictures {
            public static final String TITLE            = "%s/DownloadPictures/title";
            
            public static final String PROXY_ADDRESS    = "%s/DownloadPictures/proxy/address";
            public static final String PROXY_PORT       = "%s/DownloadPictures/proxy/port";
            
            public static final String NO_PROXY         = "%s/DownloadPictures/proxy/type/none";
            public static final String HTTP_PROXY       = "%s/DownloadPictures/proxy/type/http";
            public static final String SOCKS_PROXY      = "%s/DownloadPictures/proxy/type/socks";
            
            public static final String NO_MORE          = "%s/DownloadPictures/no-more";
            
            public static final String BAR_BEFORE_START = "%s/DownloadPictures/bar/before-start";
            public static final String BAR_WAIT         = "%s/DownloadPictures/bar/wait";
            public static final String BAR_CLOSE        = "%s/DownloadPictures/bar/close";
            
            public static interface BUTTONS {
                public static final String START  = "%s/DownloadPictures/button/start";
                public static final String CANCEL = "%s/DownloadPictures/button/cancel";
                public static final String CLOSE  = "%s/DownloadPictures/button/close";
            }
            
            public static interface ERRORS {
                public static final String PROXY_CONNECT = "%s/DownloadPictures/errors/proxy/connect";
                public static final String OTHER         = "%s/DownloadPictures/errors/other";
            }
        }
        

        public static interface Gui_NewGame {
        	public static interface NEW_GAME_TEXT {
        		public static final String GAMETYPE     = "%s/NewGame/gametype";
        		public static final String LIBRARY     = "%s/NewGame/library";
        		public static final String SETTINGS     = "%s/NewGame/settings";
        		public static final String NEW_GAME     = "%s/NewGame/new_game";
        		public static final String CONSTRUCTED_TEXT      = "%s/NewGame/constructed_text";
                public static final String SEALED_TEXT         = "%s/NewGame/sealed_text";
                public static final String BOOSTER_TEXT         = "%s/NewGame/booster_text";
                public static final String YOURDECK        = "%s/NewGame/yourdeck";
                public static final String OPPONENT         = "%s/NewGame/opponent";
                public static final String DECK_EDITOR         = "%s/NewGame/deckeditor";
                public static final String NEW_GUI         = "%s/NewGame/newgui";
                public static final String AI_LAND         = "%s/NewGame/ailand";
                public static final String MILLING         = "%s/NewGame/milling";
                public static final String QUEST_MODE         = "%s/NewGame/questmode";
                public static final String START_GAME         = "%s/NewGame/startgame";
                
        	}
            public static interface MENU_BAR {
                public static interface MENU {
                    public static final String TITLE      = "%s/NewGame/menu/title";
                    public static final String LF         = "%s/NewGame/menu/lookAndFeel";
                    public static final String DOWNLOADPRICE  = "%s/NewGame/menu/downloadPrice";
                    public static final String DOWNLOAD   = "%s/NewGame/menu/download";
                    public static final String DOWNLOADLQ   = "%s/NewGame/menu/downloadlq";
                    public static final String IMPORTPICTURE   = "%s/NewGame/menu/importPicture";
                    public static final String CARD_SIZES = "%s/NewGame/menu/cardSizes";
                    public static final String CARD_STACK = "%s/NewGame/menu/cardStack";
                    public static final String CARD_STACK_OFFSET = "%s/NewGame/menu/cardStackOffset";
                    public static final String ABOUT      = "%s/NewGame/menu/about";
                }
                
                public static interface OPTIONS {
                    public static final String TITLE = "%s/NewGame/options/title";
                    public static final String FONT = "%s/NewGame/options/font";
                    public static final String CARD_OVERLAY = "%s/NewGame/options/cardOverlay";
					public static final String CARD_SCALE = "%s/NewGame/options/cardScale";
                    
                    public static interface GENERATE {
                        public static final String TITLE            = "%s/NewGame/options/generate/title";
                        public static final String REMOVE_SMALL     = "%s/NewGame/options/generate/removeSmall";
                        public static final String REMOVE_ARTIFACTS = "%s/NewGame/options/generate/removeArtifacts";
                    }
                }
            }
            
            public static interface ERRORS {}
        }
        
        public static interface Gui_WinLose {
        	public static interface WINLOSE_TEXT {
        		public static final String WON     = "%s/WinLose/won";
        		public static final String LOST     = "%s/WinLose/lost";
        		public static final String WIN     = "%s/WinLose/win";
        		public static final String LOSE     = "%s/WinLose/lose";
        		public static final String CONTINUE     = "%s/WinLose/continue";
        		public static final String RESTART     = "%s/WinLose/restart";
        		public static final String QUIT     = "%s/WinLose/quit";
        	}
        }
        
        public static interface Gui_DownloadPrices {
        	public static interface DOWNLOADPRICES {
        		public static final String TITLE     = "%s/DownloadPrices/title";
        		public static final String START_UPDATE     = "%s/DownloadPrices/startupdate";
        		public static final String DOWNLOADING     = "%s/DownloadPrices/downloading";
        		public static final String COMPILING     = "%s/DownloadPrices/compiling";
        	}
        }
        
    }
}

