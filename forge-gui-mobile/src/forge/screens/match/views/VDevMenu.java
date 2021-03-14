package forge.screens.match.views;

import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.match.MatchController;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;
import forge.util.Localizer;

public class VDevMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblGenerateMana"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke all these in game thread since they may require synchronous user input
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().generateMana();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblTutor"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().tutorForCard();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblRollbackPhase"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke all these in game thread since they may require synchronous user input
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().rollbackPhase();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCastSpellOrPlayLand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().castASpell();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCardToHand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToHand();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCardToBattlefield"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToBattlefield();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCardToLibrary"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToLibrary();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCardToGraveyard"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToGraveyard();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblCardToExile"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToExile();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblRepeatAddCard"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().repeatLastAddition();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblExileFromHand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().exileCardsFromHand();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblExileFromPlay"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().exileCardsFromBattlefield();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblRemoveFromGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().removeCardsFromGame();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblSetLife"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().setPlayerLife();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblWinGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().winGame();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblSetupGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().setupGameState();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblDumpGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().dumpGameState();
                    }
                });
            }
        }));

        final boolean unlimitedLands = MatchController.instance.getGameController().canPlayUnlimitedLands();
        addItem(new FCheckBoxMenuItem(Localizer.getInstance().getMessage("lblUnlimitedLands"), unlimitedLands,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.getGameController().cheat().setCanPlayUnlimitedLands(!unlimitedLands);
            }
        }));
        final boolean viewAll = MatchController.instance.getGameController().mayLookAtAllCards();
        addItem(new FCheckBoxMenuItem(Localizer.getInstance().getMessage("lblViewAll"), viewAll,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.getGameController().cheat().setViewAllCards(!viewAll);
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblAddCounterPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCountersToPermanent();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblSubCounterPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().removeCountersFromPermanent();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblTapPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().tapPermanents();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblUntapPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().untapPermanents();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblRiggedRoll"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().riggedPlanarRoll();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Localizer.getInstance().getMessage("lblWalkTo"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().planeswalkTo();
                    }
                });
            }
        }));
    }
}
