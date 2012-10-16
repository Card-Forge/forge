package forge.gui.home;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum CHomeUI implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private LblMenuItem lblSelected = new LblMenuItem(VSubmenuConstructed.SINGLETON_INSTANCE);

    /** Programatically selects a menu item.
     *  @param id0 {@link forge.gui.framework.EDocID} */
    public void itemClick(EDocID id0) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        if (lblSelected != null) {
            lblSelected.setSelected(false);
            lblSelected.repaintSelf();
        }

        id0.getDoc().populate();
        id0.getDoc().getLayoutControl().update();
        lblSelected = VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(id0);
        lblSelected.setSelected(true);

        prefs.setPref(FPref.SUBMENU_CURRENTMENU, id0.toString());
        Singletons.getModel().getPreferences().save();
    }

    /** @param lbl0 {@link forge.gui.home.LblMenuItem} */
    public void setLblSelected(final LblMenuItem lbl0) {
        this.lblSelected = lbl0;
    }

    /** @return {@link javax.swing.JLabel} */
    public LblMenuItem getLblSelected() {
        return lblSelected;
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#intialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        selectPrevious();
        VHomeUI.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorConstructed());
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_CONSTRUCTED);
            }
        });

        VHomeUI.SINGLETON_INSTANCE.getLblExit().setCommand(new Command() {
            @Override
            public void execute() {
                System.exit(0);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#update()
     */
    @Override
    public void update() {
    }

    /* (non-Javadoc)
     * @see forge.view.home.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /**
     * Pulls previous menu selection from preferences
     * and clicks it programatically.
     */
    private void selectPrevious() {
        EDocID selected = null;
        try {
            selected = EDocID.valueOf(Singletons.getModel()
                .getPreferences().getPref(FPref.SUBMENU_CURRENTMENU));
        } catch (final Exception e) { }

        if (selected != null && VHomeUI.SINGLETON_INSTANCE.getAllSubmenuLabels().get(selected) != null) {
            itemClick(selected);
        }
        else {
            itemClick(EDocID.HOME_CONSTRUCTED);
        }
    }
}
