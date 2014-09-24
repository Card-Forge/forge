package forge.screens.match.views;

import forge.LobbyPlayer;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.match.MatchUtil;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.player.GamePlayerUtil;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class VGameMenu extends FDropDownMenu {
    public VGameMenu() {
    }

    @Override
    protected void buildMenu() {
        final Game game = MatchUtil.getGame();
        addItem(new FMenuItem("Concede", FSkinImage.CONCEDE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchUtil.concede();
                    }
                });
            }
        }));
        /*addItem(new FMenuItem("Save Game", FSkinImage.SAVE, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GameStateSerializer.saveGameState(MatchUtil.getGame(), ForgeConstants.USER_GAMES_DIR + "GameSave.txt");
                FOptionPane.showMessageDialog("Game saved successfully.", "Save Game", FOptionPane.INFORMATION_ICON);
            }
        }));
        addItem(new FMenuItem("Load Game", FSkinImage.OPEN, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GameStateDeserializer.loadGameState(MatchUtil.getGame(), ForgeConstants.USER_GAMES_DIR + "GameSave.txt");
            }
        }));*/
        addItem(new FMenuItem("Deck List", FSkinImage.DECKLIST, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final LobbyPlayer guiPlayer = GamePlayerUtil.getGuiPlayer();
                final Deck deck = MatchUtil.getGameView().getDeck(guiPlayer);
                if (deck != null) {
                    FDeckViewer.show(deck);
                }
            }
        }));
        addItem(new FMenuItem("Auto-Yields", FSkinImage.WARNING, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final Player localPlayer = MatchUtil.getCurrentPlayer();
                if (localPlayer == null) { return; }

                final boolean autoYieldsDisabled = game.getDisableAutoYields();
                VAutoYields autoYields = new VAutoYields(game, localPlayer) {
                    @Override
                    public void setVisible(boolean b0) {
                        super.setVisible(b0);
                        if (!b0) {
                            if (autoYieldsDisabled && !game.getDisableAutoYields()) {
                                //if re-enabling auto-yields, auto-yield to current ability on stack if applicable
                                SpellAbility ability = game.getStack().peekAbility();
                                if (ability != null && ability.isAbility() && localPlayer.getController().shouldAutoYield(ability.toUnsuppressedString())) {
                                    MatchUtil.getGameView().passPriority();
                                }
                            }
                        }
                    }
                };
                autoYields.show();
            }
        }));
        addItem(new FMenuItem("Settings", FSkinImage.SETTINGS, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                SettingsScreen.show();
            }
        }));
    }
}
