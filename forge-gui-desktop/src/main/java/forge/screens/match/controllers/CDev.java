package forge.screens.match.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import forge.Singletons;
import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.match.views.VDev;

/**
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CDev implements ICDoc {
    SINGLETON_INSTANCE;

    private final MouseListener madUnlimited = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            togglePlayManyLandsPerTurn();
        }
    };
    public void togglePlayManyLandsPerTurn() {
        boolean newValue = !VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().getToggled();
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setToggled(newValue);
        Singletons.getControl().getGameView().cheat().setCanPlayUnlimitedLands(newValue);
    }

    private final MouseListener madMana = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            generateMana();
        }
    };
    public void generateMana() {
        Singletons.getControl().getGameView().cheat().generateMana();
    }

    private final MouseListener madSetup = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setupGameState();
        }
    };
    public void setupGameState() {
        Singletons.getControl().getGameView().cheat().setupGameState();
    }

    private final MouseListener madTutor = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tutorForCard();
        }
    };
    public void tutorForCard() {
        Singletons.getControl().getGameView().cheat().tutorForCard();
    }

    private final MouseListener madCardToHand = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToHand();
        }
    };
    public void addCardToHand() {
        Singletons.getControl().getGameView().cheat().addCardToHand();
    }

    private final MouseListener madCounter = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCounterToPermanent();
        }
    };
    public void addCounterToPermanent() {
        Singletons.getControl().getGameView().cheat().addCountersToPermanent();
    }

    private final MouseListener madTap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tapPermanent();
        }
    };
    public void tapPermanent() {
        Singletons.getControl().getGameView().cheat().tapPermanents();
    }

    private final MouseListener madUntap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            untapPermanent();
        }
    };
    public void untapPermanent() {
        Singletons.getControl().getGameView().cheat().untapPermanents();
    }

    private final MouseListener madLife = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setPlayerLife();
        }
    };
    public void setPlayerLife() {
        Singletons.getControl().getGameView().cheat().setPlayerLife();
    }

    private final MouseListener madWinGame = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            winGame();
        }
    };
    public void winGame() {
        Singletons.getControl().getGameView().cheat().winGame();
    }

    private final MouseListener madCardToBattlefield = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToBattlefield();
        }
    };
    public void addCardToBattlefield() {
        Singletons.getControl().getGameView().cheat().addCardToBattlefield();
    }

    private final MouseListener madRiggedRoll = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            riggedPlanerRoll();
        }
    };
    public void riggedPlanerRoll() {
        Singletons.getControl().getGameView().cheat().riggedPlanarRoll();
    }

    private final MouseListener madWalkToPlane = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            planeswalkTo();
        }
    };
    public void planeswalkTo() {
        Singletons.getControl().getGameView().cheat().planeswalkTo();
    }

    //========== End mouse listener inits

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
       //  VDev.SINGLETON_INSTANCE.getLblMilling().addMouseListener(madMilling);
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().addMouseListener(madUnlimited);
        VDev.SINGLETON_INSTANCE.getLblGenerateMana().addMouseListener(madMana);
        VDev.SINGLETON_INSTANCE.getLblSetupGame().addMouseListener(madSetup);
        VDev.SINGLETON_INSTANCE.getLblTutor().addMouseListener(madTutor);
        VDev.SINGLETON_INSTANCE.getLblCardToHand().addMouseListener(madCardToHand);
        VDev.SINGLETON_INSTANCE.getLblCounterPermanent().addMouseListener(madCounter);
        VDev.SINGLETON_INSTANCE.getLblTapPermanent().addMouseListener(madTap);
        VDev.SINGLETON_INSTANCE.getLblUntapPermanent().addMouseListener(madUntap);
        VDev.SINGLETON_INSTANCE.getLblSetLife().addMouseListener(madLife);
        VDev.SINGLETON_INSTANCE.getLblWinGame().addMouseListener(madWinGame);
        VDev.SINGLETON_INSTANCE.getLblCardToBattlefield().addMouseListener(madCardToBattlefield);
        VDev.SINGLETON_INSTANCE.getLblRiggedRoll().addMouseListener(madRiggedRoll);
        VDev.SINGLETON_INSTANCE.getLblWalkTo().addMouseListener(madWalkToPlane);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setToggled(Singletons.getControl().getGameView().canPlayUnlimitedLands());
    }
}
