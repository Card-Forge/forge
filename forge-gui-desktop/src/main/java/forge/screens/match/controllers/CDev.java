package forge.screens.match.controllers;

import forge.GuiBase;
import forge.UiCommand;
import forge.Singletons;
import forge.game.player.Player;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.views.VDev;
import forge.util.GuiDisplayUtil;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CDev implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

//    //========= Start mouse listener inits
//    private final MouseListener madMilling = new MouseAdapter() { @Override
//        public void mousePressed(final MouseEvent e) {
//        toggleLossByMilling();
//    } };
//    public void toggleLossByMilling() {
//        VDev.SINGLETON_INSTANCE.getLblMilling().toggleEnabled();
//        FModel.getPreferences().writeMatchPreferences();
//        FModel.getPreferences().save();
//    }

    private final MouseListener madUnlimited = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        togglePlayManyLandsPerTurn();
    } };
    public void togglePlayManyLandsPerTurn() {
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().toggleEnabled();
        boolean newValue = VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().getEnabled();
        FModel.getPreferences().setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(newValue));
        
        for(Player p : Singletons.getControl().getObservedGame().getPlayers()) {
            if( p.getLobbyPlayer() == GuiBase.getInterface().getGuiPlayer() )
                p.canCheatPlayUnlimitedLands = newValue;
        }
        // probably will need to call a synchronized method to have the game thread see changed value of the variable 
        
        FModel.getPreferences().save();
    }

    private final MouseListener madMana = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        generateMana(); }
    };
    public void generateMana() {
        GuiDisplayUtil.devModeGenerateMana();
    }

    private final MouseListener madSetup = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        setupGameState(); }
    };
    public void setupGameState() {
        GuiDisplayUtil.devSetupGameState();
    }

    private final MouseListener madTutor = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        tutorForCard(); }
    };
    public void tutorForCard() {
        GuiDisplayUtil.devModeTutor();
    }

    private final MouseListener madCardToHand = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        addCardToHand(); }
    };
    public void addCardToHand() {
        GuiDisplayUtil.devModeCardToHand();
    }

    private final MouseListener madCounter = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        addCounterToPermanent(); }
    };
    public void addCounterToPermanent() {
        GuiDisplayUtil.devModeAddCounter();
    }

    private final MouseListener madTap = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        tapPermanent(); }
    };
    public void tapPermanent() {
        GuiDisplayUtil.devModeTapPerm();
    }

    private final MouseListener madUntap = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        untapPermanent(); }
    };
    public void untapPermanent() {
        GuiDisplayUtil.devModeUntapPerm();
    }

    private final MouseListener madLife = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        setPlayerLife(); }
    };
    public void setPlayerLife() {
        GuiDisplayUtil.devModeSetLife();
    }

    private final MouseListener madWinGame = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        winGame(); }
    };
    public void winGame() {
        GuiDisplayUtil.devModeWinGame();
    }

    private final MouseListener madCardToBattlefield = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        addCardToPlay(); }
    };
    public void addCardToPlay() {
        GuiDisplayUtil.devModeCardToBattlefield();
    }

    private final MouseListener madRiggedRoll = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        riggedPlanerRoll(); }
    };
    public void riggedPlanerRoll() {
        GuiDisplayUtil.devModeRiggedPlanarRoll();
    }

    private final MouseListener madWalkToPlane = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
        planeswalkTo(); }
    };
    public void planeswalkTo() {
        GuiDisplayUtil.devModePlaneswalkTo();
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

        ForgePreferences prefs = FModel.getPreferences();

       //  VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(prefs.getPrefBoolean(FPref.DEV_MILLING_LOSS));
        //VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(Constant.Runtime.MILL[0]);
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setEnabled(prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND));
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        // VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(FModel.getPreferences().getPrefBoolean(FPref.DEV_MILLING_LOSS));
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setEnabled(FModel.getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND));
    }

}
