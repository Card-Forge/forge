package forge.screens.match.controllers;

import forge.game.GameView;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SRearrangingUtil;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.FloatingStack;
import forge.screens.match.views.VStack;
import forge.view.FView;

/**
 * Controls the combat panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public class CStack implements ICDoc {

    private final CMatchUI matchUI;
    private final VStack view;

    private FloatingStack floatingStack;

    public CStack(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VStack(this);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }
    public final VStack getView() {
        return view;
    }

    /** Lazily creates the floating stack window and moves the cascade into it. */
    private FloatingStack getFloatingStack() {
        if (floatingStack == null) {
            floatingStack = new FloatingStack();
            floatingStack.getTitleBar().addRightControl(view.getTextToggle());
            view.populateInto(floatingStack.getContentPanel());
        }
        return floatingStack;
    }

    /**
     * Called when the stack view itself changes shape — opening or closing the
     * text list. Widens the window by the same amount, or re-packs it if the
     * user hasn't taken over its size.
     */
    public void stackLayoutChanged(final int widthDelta) {
        if (floatingStack == null) { return; }
        if (floatingStack.isUserSized()) {
            floatingStack.grow(widthDelta);
        } else {
            floatingStack.sizeToContent();
        }
    }

    /**
     * Pulls the stack out of whatever dock cell the saved layout put it in, so
     * it exists only as the floating window. Collapses the cell if the stack
     * was the only thing in it, mirroring {@link CPrompt#undockPrompt()}.
     */
    public void undockStack() {
        final DragCell cell = view.getParentCell();
        if (cell != null) {
            cell.removeDoc(view);
            view.setParentCell(null);
            if (cell.getDocs().isEmpty()) {
                SRearrangingUtil.fillGap(cell);
                FView.SINGLETON_INSTANCE.removeDragCell(cell);
            }
        }
        // Always re-populate: docking calls VStack.populate(), which reparents
        // the cascade into the cell body.
        view.populateInto(getFloatingStack().getContentPanel());
    }

    /** Tears the floating stack down at the end of a match. */
    public void closeFloatingStack() {
        if (floatingStack != null) {
            floatingStack.setVisible(false);
            floatingStack.dispose();
            floatingStack = null;
        }
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    @Override
    public void update() {
        SDisplayUtil.showTab(EDocID.REPORT_STACK.getDoc()); //no-op once undocked

        final GameView game = matchUI.getGameView();
        if (game == null || game.getStack().isEmpty()) {
            view.updateStack();
            if (floatingStack != null) {
                floatingStack.setVisible(false);
            }
            return;
        }
        final FloatingStack window = getFloatingStack();
        view.setFitToContainer(window.isUserSized());
        view.updateStack();
        window.sizeToContent();
        window.setVisible(true);
        window.toFront();
    }
}
