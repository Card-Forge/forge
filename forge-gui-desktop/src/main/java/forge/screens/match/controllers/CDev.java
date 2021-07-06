package forge.screens.match.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import com.google.common.collect.Lists;

import forge.gui.framework.ICDoc;
import forge.interfaces.IGameController;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.IDevListener;
import forge.screens.match.views.VDev;

/**
 * Controls the combat panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public final class CDev implements ICDoc {

    private final CMatchUI matchUI;
    private final VDev view;
    public CDev(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VDev(this);
        addListener(view);

        view.getLblUnlimitedLands().addMouseListener(madUnlimited);
        view.getLblViewAll().addMouseListener(madViewAll);
        view.getLblGenerateMana().addMouseListener(madMana);
        view.getLblSetupGame().addMouseListener(madSetup);
        view.getLblDumpGame().addMouseListener(madDump);
        view.getLblTutor().addMouseListener(madTutor);
        view.getLblCardToHand().addMouseListener(madCardToHand);
        view.getLblExileFromHand().addMouseListener(madExileFromHand);
        view.getLblCardToBattlefield().addMouseListener(madCardToBattlefield);
        view.getLblCardToLibrary().addMouseListener(madCardToLibrary);
        view.getLblCardToGraveyard().addMouseListener(madCardToGraveyard);
        view.getLblCardToExile().addMouseListener(madCardToExile);
        view.getLblCastSpell().addMouseListener(madCastASpell);
        view.getLblRepeatAddCard().addMouseListener(madRepeatAddCard);
        view.getLblAddCounterPermanent().addMouseListener(madAddCounter);
        view.getLblSubCounterPermanent().addMouseListener(madSubCounter);
        view.getLblTapPermanent().addMouseListener(madTap);
        view.getLblUntapPermanent().addMouseListener(madUntap);
        view.getLblSetLife().addMouseListener(madLife);
        view.getLblWinGame().addMouseListener(madWinGame);
        view.getLblExileFromPlay().addMouseListener(madExileFromPlay);
        view.getLblRemoveFromGame().addMouseListener(madRemoveFromGame);
        view.getLblRiggedRoll().addMouseListener(madRiggedRoll);
        view.getLblWalkTo().addMouseListener(madWalkToPlane);
        view.getLblAskAI().addMouseListener(madAskAI);
    }
    public IGameController getController() {
        return matchUI.getGameController();
    }
    public VDev getView() {
        return view;
    }

    private final List<IDevListener> listeners = Lists.newArrayListWithCapacity(2);
    public void addListener(final IDevListener listener) {
        listeners.add(listener);
    }

    private final MouseListener madUnlimited = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            togglePlayManyLandsPerTurn();
        }
    };
    public void togglePlayManyLandsPerTurn() {
        final boolean newValue = !view.getLblUnlimitedLands().getToggled();
        getController().cheat().setCanPlayUnlimitedLands(newValue);
        update();
    }

    private final MouseListener madViewAll = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            toggleViewAllCards();
        }
    };
    public void toggleViewAllCards() {
        final boolean newValue = !view.getLblViewAll().getToggled();
        getController().cheat().setViewAllCards(newValue);
        update();
    }

    private final MouseListener madMana = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            generateMana();
        }
    };
    public void generateMana() {
        getController().cheat().generateMana();
    }

    private final MouseListener madSetup = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setupGameState();
        }
    };
    public void setupGameState() {
        getController().cheat().setupGameState();
    }

    private final MouseListener madDump = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            dumpGameState();
        }
    };
    public void dumpGameState() {
        getController().cheat().dumpGameState();
    }

    private final MouseListener madTutor = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tutorForCard();
        }
    };
    public void tutorForCard() {
        getController().cheat().tutorForCard();
    }

    private final MouseListener madCardToHand = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToHand();
        }
    };
    public void addCardToHand() {
        getController().cheat().addCardToHand();
    }

    private final MouseListener madCardToLibrary = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToLibrary();
        }
    };
    public void addCardToLibrary() {
        getController().cheat().addCardToLibrary();
    }

    private final MouseListener madCardToGraveyard = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToGraveyard();
        }
    };
    public void addCardToGraveyard() {
        getController().cheat().addCardToGraveyard();
    }

    private final MouseListener madCardToExile = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToExile();
        }
    };
    public void addCardToExile() {
        getController().cheat().addCardToExile();
    }

    private final MouseListener madCastASpell = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            castASpell();
        }
    };
    public void castASpell() {
        getController().cheat().castASpell();
    }

    private final MouseListener madRepeatAddCard = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            repeatAddCard();
        }
    };
    public void repeatAddCard() {
        getController().cheat().repeatLastAddition();
    }

    private final MouseListener madExileFromHand = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            exileCardsFromHand();
        }
    };
    public void exileCardsFromHand() {
        getController().cheat().exileCardsFromHand();
    }

    private final MouseListener madAddCounter = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCounterToPermanent();
        }
    };
    public void addCounterToPermanent() {
        getController().cheat().addCountersToPermanent();
    }

    private final MouseListener madSubCounter = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            removeCountersFromPermanent();
        }
    };
    public void removeCountersFromPermanent() {
        getController().cheat().removeCountersFromPermanent();
    }

    private final MouseListener madTap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tapPermanent();
        }
    };
    public void tapPermanent() {
        getController().cheat().tapPermanents();
    }

    private final MouseListener madUntap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            untapPermanent();
        }
    };
    public void untapPermanent() {
        getController().cheat().untapPermanents();
    }

    private final MouseListener madLife = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setPlayerLife();
        }
    };
    public void setPlayerLife() {
        getController().cheat().setPlayerLife();
    }

    private final MouseListener madWinGame = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            winGame();
        }
    };
    public void winGame() {
        getController().cheat().winGame();
    }

    private final MouseListener madCardToBattlefield = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToBattlefield();
        }
    };
    public void addCardToBattlefield() {
        getController().cheat().addCardToBattlefield();
    }

    private final MouseListener madExileFromPlay = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            exileCardsFromPlay();
        }
    };
    public void exileCardsFromPlay() {
        getController().cheat().exileCardsFromBattlefield();
    }

    private final MouseListener madRemoveFromGame = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            removeCardsFromGame();
        }
    };
    public void removeCardsFromGame() {
        getController().cheat().removeCardsFromGame();
    }

    private final MouseListener madRiggedRoll = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            riggedPlanerRoll();
        }
    };
    public void riggedPlanerRoll() {
        getController().cheat().riggedPlanarRoll();
    }

    private final MouseListener madWalkToPlane = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            planeswalkTo();
        }
    };
    public void planeswalkTo() {
        getController().cheat().planeswalkTo();
    }

    private final MouseListener madAskAI = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            askAI();
        }
    };
    public void askAI() {
        getController().cheat().askAI();
    }

    //========== End mouse listener inits

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        final IGameController controller = getController();
        if (controller != null) {
            final boolean canPlayUnlimitedLands = controller.canPlayUnlimitedLands();
            final boolean mayLookAtAllCards = controller.mayLookAtAllCards();
            for (final IDevListener listener : listeners) {
                listener.update(canPlayUnlimitedLands, mayLookAtAllCards);
            }
        }
    }
}
