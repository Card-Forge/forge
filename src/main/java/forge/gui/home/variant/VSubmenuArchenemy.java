package forge.gui.home.variant;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import forge.game.player.PlayerType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FDeckChooser;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;

/** 
 * Assembles Swing components of deck editor submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuArchenemy implements IVSubmenu<CSubmenuArchenemy> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Multiplayer Test");
    private final JPanel pnlFields;

    /** */
    private final List<JRadioButton> fieldRadios = new ArrayList<JRadioButton>();
    private final StartButton btnStart = new StartButton();
    private final ButtonGroup grpFields = new ButtonGroup();

    private final FDeckChooser dcHuman = new FDeckChooser("Choose your deck", PlayerType.HUMAN);

    private VSubmenuArchenemy() {
        FRadioButton temp;

        for (int i = 2; i < 8; i++) {
            temp = new FRadioButton();
            temp.setText(String.valueOf(i));
            fieldRadios.add(temp);
            grpFields.add(temp);
        }



        pnlFields = new FPanel();
        pnlFields.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlFields.add(new FLabel.Builder().text("How many AI opponents are you ready to handle?").build(), "h 30px!");

        for (JRadioButton rad : fieldRadios) {
            pnlFields.add(rad, "w 100px!, h 30px!, gap 30px 0 0 0");
        }

        fieldRadios.get(0).setSelected(true);
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return btnStart;
    }

    /** @return {@link java.util.List} */
    public List<JRadioButton> getFieldRadios() {
        return fieldRadios;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlFields, "w 41%!, gap 6% 6% 50px 0, growy, pushy");
        dcHuman.populate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(dcHuman, "w 41%!, gap 0 0 50px 0, growy, pushy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(new FLabel.Builder()
            .text("Starts a new game with preconstructed 2 color decks for each field.")
            .build(), "gap 0 0 50px 5px, ax center, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStart, "gap 0 0 50px 50px, ax center, span 2");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.VARIANT;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Archenemy";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_MULTITEST;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_MULTITEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuArchenemy getLayoutControl() {
        return CSubmenuArchenemy.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    public final FDeckChooser getDcHuman() {
        return dcHuman;
    }
}
