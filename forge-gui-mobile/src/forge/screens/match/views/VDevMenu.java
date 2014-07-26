package forge.screens.match.views;

import forge.GuiBase;
import forge.game.player.Player;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.FControl;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.GuiDisplayUtil;
import forge.util.ThreadUtil;

public class VDevMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        addItem(new FMenuItem("Generate Mana", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke all these in game thread since they may require synchronous user input
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeGenerateMana();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Tutor for Card", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeTutor();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Add card to hand", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeCardToHand();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Add card to play", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeCardToBattlefield();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Set Player Life", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeSetLife();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Win Game", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeWinGame();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Setup Game State", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devSetupGameState();
                    }
                });
            }
        }));

        final ForgePreferences prefs = FModel.getPreferences();
        addItem(new FCheckBoxMenuItem("Play Unlimited Lands",
                prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND),
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                boolean unlimitedLands = !prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND);

                for (Player p : FControl.getGame().getPlayers()) {
                    if (p.getLobbyPlayer() == GuiBase.getInterface().getGuiPlayer() ) {
                        p.canCheatPlayUnlimitedLands = unlimitedLands;
                    }
                }
                // probably will need to call a synchronized method to have the game thread see changed value of the variable

                prefs.setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(unlimitedLands));
                prefs.save();
            }
        }));
        addItem(new FMenuItem("Add Counter to Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeAddCounter();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Tap Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeTapPerm();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Untap Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeUntapPerm();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Rigged planar roll", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModeRiggedPlanarRoll();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Planeswalk to", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        GuiDisplayUtil.devModePlaneswalkTo();
                    }
                });
            }
        }));
    }
}
