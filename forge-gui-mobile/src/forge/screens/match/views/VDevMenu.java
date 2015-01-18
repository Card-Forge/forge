package forge.screens.match.views;

import forge.match.MatchUtil;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
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
                        MatchUtil.getHumanController().cheat().generateMana();
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
                        MatchUtil.getHumanController().cheat().tutorForCard();
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
                        MatchUtil.getHumanController().cheat().addCardToHand();
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
                        MatchUtil.getHumanController().cheat().addCardToBattlefield();
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
                        MatchUtil.getHumanController().cheat().setPlayerLife();
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
                        MatchUtil.getHumanController().cheat().winGame();
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
                        MatchUtil.getHumanController().cheat().setupGameState();
                    }
                });
            }
        }));
        addItem(new FMenuItem("Dump Game State", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchUtil.getHumanController().cheat().dumpGameState();
                    }
                });
            }
        }));

        final boolean unlimitedLands = MatchUtil.getHumanController().canPlayUnlimitedLands();
        addItem(new FCheckBoxMenuItem("Play Unlimited Lands", unlimitedLands,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchUtil.getHumanController().cheat().setCanPlayUnlimitedLands(!unlimitedLands);
            }
        }));
        final boolean viewAll = MatchUtil.getHumanController().mayLookAtAllCards();
        addItem(new FCheckBoxMenuItem("View All Cards", viewAll,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchUtil.getHumanController().cheat().setViewAllCards(!viewAll);
            }
        }));
        addItem(new FMenuItem("Add Counters to Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchUtil.getHumanController().cheat().addCountersToPermanent();
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
                        MatchUtil.getHumanController().cheat().tapPermanents();
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
                        MatchUtil.getHumanController().cheat().untapPermanents();
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
                        MatchUtil.getHumanController().cheat().riggedPlanarRoll();
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
                        MatchUtil.getHumanController().cheat().planeswalkTo();
                    }
                });
            }
        }));
    }
}
