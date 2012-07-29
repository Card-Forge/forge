package forge.gui.match.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import forge.Command;
import forge.Singletons;
import forge.gui.GuiDisplayUtil;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VDev;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CDev implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========= Start mouse listener inits
    private final MouseListener madMilling = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            VDev.SINGLETON_INSTANCE.getLblMilling().toggleEnabled();
            Singletons.getModel().savePrefs();
    } };

    private final MouseListener madUnlimited = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().toggleEnabled();
            System.out.println(VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().getEnabled());
            Singletons.getModel().getPreferences().setPref(FPref.DEV_UNLIMITED_LAND,
                    String.valueOf(VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().getEnabled()));
            Singletons.getModel().getPreferences().save();
    } };

    private final MouseListener madMana = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeGenerateMana();
            Singletons.getModel().getPreferences().save(); } };

    private final MouseListener madSetup = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devSetupGameState(); } };

    private final MouseListener madTutor = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeTutor(); } };

    private final MouseListener madAddAnyCard = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeAddAnyCard(); } };

    private final MouseListener madGiveAnyCard = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeGiveAnyCard(); } };

    private final MouseListener madCounter = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeAddCounter(); } };

    private final MouseListener madTap = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeTapPerm(); } };

    private final MouseListener madUntap = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeUntapPerm(); } };

    private final MouseListener madLife = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeSetLife(); } };

    private final MouseListener madBreakpoint = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            GuiDisplayUtil.devModeBreakpoint(); } };

    //========== End mouse listener inits

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        VDev.SINGLETON_INSTANCE.getLblMilling().addMouseListener(madMilling);
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().addMouseListener(madUnlimited);
        VDev.SINGLETON_INSTANCE.getLblGenerateMana().addMouseListener(madMana);
        VDev.SINGLETON_INSTANCE.getLblSetupGame().addMouseListener(madSetup);
        VDev.SINGLETON_INSTANCE.getLblTutor().addMouseListener(madTutor);
        VDev.SINGLETON_INSTANCE.getAnyCard().addMouseListener(madAddAnyCard);
        VDev.SINGLETON_INSTANCE.getLblGiveCard().addMouseListener(madGiveAnyCard);
        VDev.SINGLETON_INSTANCE.getLblCounterPermanent().addMouseListener(madCounter);
        VDev.SINGLETON_INSTANCE.getLblTapPermanent().addMouseListener(madTap);
        VDev.SINGLETON_INSTANCE.getLblUntapPermanent().addMouseListener(madUntap);
        VDev.SINGLETON_INSTANCE.getLblSetLife().addMouseListener(madLife);
        VDev.SINGLETON_INSTANCE.getLblBreakpoint().addMouseListener(madBreakpoint);

        ForgePreferences prefs = Singletons.getModel().getPreferences();
        
        VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(prefs.getPrefBoolean(FPref.DEV_MILLING_LOSS));
        //VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(Constant.Runtime.MILL[0]);
        VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setEnabled(prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND));
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MILLING_LOSS)) {
            VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(true);
        } else {
            VDev.SINGLETON_INSTANCE.getLblMilling().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)) {
            VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setEnabled(true);
        } else {
            VDev.SINGLETON_INSTANCE.getLblUnlimitedLands().setEnabled(false);
        }
    }

}
