package forge.screens.match.views;

import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.match.FControl;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
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
                        FControl.getGameView().cheat().generateMana();
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
                        FControl.getGameView().cheat().tutorForCard();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Add Card to Hand", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().addCardToHand();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Add Card to Play", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().addCardToBattlefield();
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
                        FControl.getGameView().cheat().setPlayerLife();
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
                        FControl.getGameView().cheat().winGame();
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
                        FControl.getGameView().cheat().setupGameState();
                    }
                });
            }
        }));

        final boolean unlimitedLands = FControl.getGameView().canPlayUnlimitedLands();
        addItem(new FCheckBoxMenuItem("Play Unlimited Lands", unlimitedLands,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.getGameView().cheat().setCanPlayUnlimitedLands(!unlimitedLands);
            }
        }));
        final boolean viewAll = FControl.getGameView().canViewAllCards();
        addItem(new FCheckBoxMenuItem("View All Cards", viewAll,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FControl.getGameView().cheat().setViewAllCards(!viewAll);
            }
        }));
        addItem(new FMenuItem("Add Counters to Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().addCountersToPermanent();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Tap Permanents", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().tapPermanents();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Untap Permanents", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().untapPermanents();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Rigged Planar Roll", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        FControl.getGameView().cheat().riggedPlanarRoll();
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
                        FControl.getGameView().cheat().planeswalkTo();
                    }
                });
            }
        }));
    }
}
