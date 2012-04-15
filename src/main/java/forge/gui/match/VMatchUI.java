package forge.gui.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import forge.AllZone;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SIOUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.views.VDetail;
import forge.gui.match.views.VDev;
import forge.gui.match.views.VMessage;
import forge.gui.match.views.VPicture;
import forge.view.FView;

/** 
 * Top level view class for match UI drag layout.<br>
 * Has access methods for all draggable documents.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VMatchUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    // Instantiate non-singleton tab instances
    private final IVDoc field0 = new VField(EDocID.FIELD_0, AllZone.getComputerPlayer());
    private final IVDoc field1 = new VField(EDocID.FIELD_1, AllZone.getHumanPlayer());
    private final IVDoc field2 = new VField(EDocID.FIELD_2, AllZone.getComputerPlayer());
    private final IVDoc field3 = new VField(EDocID.FIELD_3, AllZone.getComputerPlayer());

    private final IVDoc hand0 = new VHand(EDocID.HAND_0, AllZone.getComputerPlayer());
    private final IVDoc hand1 = new VHand(EDocID.HAND_1, AllZone.getHumanPlayer());
    private final IVDoc hand2 = new VHand(EDocID.HAND_2, AllZone.getComputerPlayer());
    private final IVDoc hand3 = new VHand(EDocID.HAND_3, AllZone.getComputerPlayer());

    // Instantiate singleton tab instances
    private final IVDoc stack = EDocID.REPORT_STACK.getDoc();
    private final IVDoc combat = EDocID.REPORT_COMBAT.getDoc();
    private final IVDoc log = EDocID.REPORT_LOG.getDoc();
    private final IVDoc players = EDocID.REPORT_PLAYERS.getDoc();
    private final IVDoc message = EDocID.REPORT_MESSAGE.getDoc();

    private final IVDoc dock = EDocID.BUTTON_DOCK.getDoc();
    private final IVDoc detail = EDocID.CARD_DETAIL.getDoc();
    private final IVDoc picture = EDocID.CARD_PICTURE.getDoc();
    private final IVDoc antes = EDocID.CARD_ANTES.getDoc();
    private final IVDoc devmode = EDocID.DEV_MODE.getDoc();

    // Other instantiations
    private final CMatchUI control = null;
    private boolean isPopulated = false;

    /** */
    public void instantiate() {

    }

    /** */
    public void populate() {
        if (isPopulated) { return; }
        else { isPopulated = true; }

        SIOUtil.loadLayout(null);
    }

    /** */
    public void defaultLayout() {
        final DragCell cell0 = new DragCell();
        final DragCell cell1 = new DragCell();
        final DragCell cell2 = new DragCell();
        final DragCell cell3 = new DragCell();
        final DragCell cell4 = new DragCell();
        final DragCell cell5 = new DragCell();
        final DragCell cell6 = new DragCell();

        cell0.addDoc(stack);
        cell0.addDoc(combat);
        cell0.addDoc(log);
        cell0.addDoc(players);

        cell1.addDoc(message);
        cell1.addDoc(devmode);

        cell2.addDoc(field0);
        cell6.addDoc(field1);
        if (AllZone.getPlayersInGame().size() > 2) { cell2.addDoc(field2); }
        if (AllZone.getPlayersInGame().size() > 3) { cell2.addDoc(field3); }

        if (AllZone.getPlayersInGame().size() > 1000) { cell3.addDoc(hand0); }
        cell3.addDoc(hand1);
        if (AllZone.getPlayersInGame().size() > 2) { cell2.addDoc(hand2); }
        if (AllZone.getPlayersInGame().size() > 3) { cell2.addDoc(hand3); }

        cell4.addDoc(dock);
        cell5.addDoc(detail);
        cell5.addDoc(picture);
        cell5.addDoc(antes);

        FView.SINGLETON_INSTANCE.addDragCell(cell0);
        FView.SINGLETON_INSTANCE.addDragCell(cell1);
        FView.SINGLETON_INSTANCE.addDragCell(cell2);
        FView.SINGLETON_INSTANCE.addDragCell(cell3);
        FView.SINGLETON_INSTANCE.addDragCell(cell4);
        FView.SINGLETON_INSTANCE.addDragCell(cell5);
        FView.SINGLETON_INSTANCE.addDragCell(cell6);

        cell0.setRoughBounds(0, 0, 0.2, 0.7);
        cell1.setRoughBounds(0, 0.7, 0.2, 0.3);
        cell2.setRoughBounds(0.2, 0, 0.6, 0.33);
        cell3.setRoughBounds(0.2, 0.66, 0.6, 0.34);
        cell4.setRoughBounds(0.8, 0, 0.2, 0.25);
        cell5.setRoughBounds(0.8, 0.25, 0.2, 0.75);
        cell6.setRoughBounds(0.2, 0.33, 0.6, 0.33);

        SResizingUtil.resizeWindow();
    }

    //========== Retrieval methods

    /** @return {@link forge.gui.match.CMatchUI} */
    public CMatchUI getControl() {
        return this.control;
    }

    /** @return {@link forge.gui.match.views.VDetail} */
    public VDetail getViewDetail() {
        return ((VDetail) this.detail);
    }

    /** @return {@link forge.gui.match.views.VPicture} */
    public VPicture getViewPicture() {
        return ((VPicture) this.picture);
    }

    /** @return {@link forge.gui.match.views.VDev} */
    public VDev getViewDevMode() {
        return ((VDev) this.devmode);
    }

    /** @return {@link java.util.List}<{@link forge.gui.match.nonsigleton.VField}> */
    public List<VField> getFieldViews() {
        final List<VField> lst = new ArrayList<VField>();
        lst.add((VField) field0);
        lst.add((VField) field1);
        //lst.add((VField) field2);
        //lst.add((VField) field3);
        return lst;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnCancel() {
        return ((VMessage) this.message).getBtnCancel();
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnOK() {
        return ((VMessage) this.message).getBtnOK();
    }
}
