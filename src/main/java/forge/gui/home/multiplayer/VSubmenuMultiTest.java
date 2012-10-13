package forge.gui.home.multiplayer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;

/** 
 * Assembles Swing components of deck editor submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuMultiTest implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Multiplayer Test");
    private final JPanel pnlFields, pnlHands;

    /** */
    private final List<JRadioButton> fieldRadios = new ArrayList<JRadioButton>();
    private final List<JRadioButton> handRadios = new ArrayList<JRadioButton>();
    private final StartButton btnStart = new StartButton();
    private final ButtonGroup grpFields = new ButtonGroup();
    private final ButtonGroup grpHands = new ButtonGroup();

    private VSubmenuMultiTest() {
        FRadioButton temp;

        for (int i = 1; i < 8; i++) {
            temp = new FRadioButton();
            temp.setText(String.valueOf(i + 1));
            fieldRadios.add(temp);
            grpFields.add(temp);
        }

        for (int i = 0; i < 4; i++) {
            temp = new FRadioButton();
            temp.setText(String.valueOf(i + 1));
            handRadios.add(temp);
            grpHands.add(temp);
        }

        pnlFields = new FPanel();
        pnlFields.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlFields.add(new FLabel.Builder().text("Player panels:").build(), "w 100px!, h 30px!");

        for (JRadioButton rad : fieldRadios) {
            pnlFields.add(rad, "w 100px!, h 30px!, gap 30px 0 0 0");
        }

        pnlHands = new FPanel();
        pnlHands.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlHands.add(new FLabel.Builder().text("Hand panels:").build(), "w 100px!, h 30px!");

        for (JRadioButton rad : handRadios) {
            pnlHands.add(rad, "w 100px!, h 30px!, gap 30px 0 0 0");
        }

        handRadios.get(0).setSelected(true);
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

    /** @return {@link java.util.List} */
    public List<JRadioButton> getHandRadios() {
        return handRadios;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        parentCell.getBody().add(pnlFields, "w 41%!, gap 6% 6% 50px 0, growy, pushy");
        parentCell.getBody().add(pnlHands, "w 41%!, gap 0 0 50px 0, growy, pushy");
        parentCell.getBody().add(new FLabel.Builder()
            .text("Starts a new game with preconstructed 2 color decks for each field.")
            .build(), "gap 0 0 50px 5px, ax center, span 2");
        parentCell.getBody().add(new FLabel.Builder()
            .text("Field 0 is Human, the rest are AI [AllZone.getComputerPlayer()].")
            .build(), "gap 0 0 0 5px, ax center, span 2");
        parentCell.getBody().add(btnStart, "gap 0 0 50px 50px, ax center, span 2");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.MULTIPLAYER;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Testing (Temporary)";
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
    public ICDoc getLayoutControl() {
        return CSubmenuMultiTest.SINGLETON_INSTANCE;
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
}
