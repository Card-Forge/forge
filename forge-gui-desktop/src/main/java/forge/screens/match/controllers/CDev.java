package forge.screens.match.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.match.MatchUtil;
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
        MatchUtil.getHumanController().cheat().setCanPlayUnlimitedLands(newValue);
    }

    private final MouseListener madViewAll = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            toggleViewAllCards();
        }
    };
    public void toggleViewAllCards() {
        boolean newValue = !VDev.SINGLETON_INSTANCE.getLblViewAll().getToggled();
        VDev.SINGLETON_INSTANCE.getLblViewAll().setToggled(newValue);
        MatchUtil.getHumanController().cheat().setViewAllCards(newValue);
    }

    private final MouseListener madMana = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            generateMana();
        }
    };
    public void generateMana() {
        MatchUtil.getHumanController().cheat().generateMana();
    }

    private final MouseListener madSetup = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setupGameState();
        }
    };
    public void setupGameState() {
        MatchUtil.getHumanController().cheat().setupGameState();
    }
    
    private final MouseListener madDump = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            dumpGameState();
        }
    };
    public void dumpGameState() {
        MatchUtil.getHumanController().cheat().dumpGameState();
    }

    private final MouseListener madTutor = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tutorForCard();
        }
    };
    public void tutorForCard() {
        MatchUtil.getHumanController().cheat().tutorForCard();
    }

    private final MouseListener madCardToHand = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToHand();
        }
    };
    public void addCardToHand() {
        MatchUtil.getHumanController().cheat().addCardToHand();
    }

    private final MouseListener madCounter = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCounterToPermanent();
        }
    };
    public void addCounterToPermanent() {
        MatchUtil.getHumanController().cheat().addCountersToPermanent();
    }

    private final MouseListener madTap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            tapPermanent();
        }
    };
    public void tapPermanent() {
        MatchUtil.getHumanController().cheat().tapPermanents();
    }

    private final MouseListener madUntap = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            untapPermanent();
        }
    };
    public void untapPermanent() {
        MatchUtil.getHumanController().cheat().untapPermanents();
    }

    private final MouseListener madLife = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            setPlayerLife();
        }
    };
    public void setPlayerLife() {
        MatchUtil.getHumanController().cheat().setPlayerLife();
    }

    private final MouseListener madWinGame = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            winGame();
        }
    };
    public void winGame() {
        MatchUtil.getHumanController().cheat().winGame();
    }

    private final MouseListener madCardToBattlefield = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            addCardToBattlefield();
        }
    };
    public void addCardToBattlefield() {
        MatchUtil.getHumanController().cheat().addCardToBattlefield();
    }

    private final MouseListener madRiggedRoll = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            riggedPlanerRoll();
        }
    };
    public void riggedPlanerRoll() {
        MatchUtil.getHumanController().cheat().riggedPlanarRoll();
    }

    private final MouseListener madWalkToPlane = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            planeswalkTo();
        }
    };
    public void planeswalkTo() {
        MatchUtil.getHumanController().cheat().planeswalkTo();
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
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().addMouseListener(madUnlimited);
        VDev.SINGLETON_INSTANCE.getLblViewAll().addMouseListener(madViewAll);
        VDev.SINGLETON_INSTANCE.getLblGenerateMana().addMouseListener(madMana);
        VDev.SINGLETON_INSTANCE.getLblSetupGame().addMouseListener(madSetup);
        VDev.SINGLETON_INSTANCE.getLblDumpGame().addMouseListener(madDump);
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
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setToggled(MatchUtil.getHumanController().canPlayUnlimitedLands());
        VDev.SINGLETON_INSTANCE.getLblViewAll().setToggled(MatchUtil.getHumanController().mayLookAtAllCards());
    }
}
