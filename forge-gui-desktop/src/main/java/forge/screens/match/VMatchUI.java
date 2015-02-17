package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.game.GameView;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.framework.VEmptyDoc;
import forge.properties.ForgePreferences;
import forge.screens.match.views.VCommand;
import forge.screens.match.views.VDev;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.view.FView;

/** 
 * Top level view class for match UI drag layout.<br>
 * Has access methods for all draggable documents.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VMatchUI implements IVTopLevelUI {
    private List<VCommand> lstCommands = new ArrayList<VCommand>();
    private List<VField> lstFields = new ArrayList<VField>();
    private List<VHand> lstHands = new ArrayList<VHand>();

    // Other instantiations
    private final CMatchUI control;

    VMatchUI(final CMatchUI control) {
        this.control = control;
    }

    @Override
    public void instantiate() {
    }

    @Override
    public void populate() {
        // Dev mode disabled? Remove from parent cell if exists.
        final VDev vDev = getControl().getCDev().getView();
        if (!ForgePreferences.DEV_MODE) {
            if (vDev.getParentCell() != null) {
                final DragCell parent = vDev.getParentCell();
                parent.removeDoc(vDev);
                vDev.setParentCell(null);

                // If dev mode was first tab, the new first tab needs re-selecting.
                if (parent.getDocs().size() > 0) {
                    parent.setSelected(parent.getDocs().get(0));
                }
            }
        } else if (vDev.getParentCell() == null) {
            // Dev mode enabled? May already by added, or put in message cell by default.
            getControl().getCPrompt().getView().getParentCell().addDoc(vDev);
        }

        //focus first enabled Prompt button if returning to match screen
        if (getBtnOK().isEnabled()) {
            getBtnOK().requestFocusInWindow();
        } else if (getBtnCancel().isEnabled()) {
            getBtnCancel().requestFocusInWindow();
        }

        // Add extra players alternatively to existing user/AI field panels.
        for (int i = 2; i < lstFields.size(); i++) {
            // If already in layout, no need to add again.
            VField vField = lstFields.get(i);
            if (vField.getParentCell() == null) {
                lstFields.get(i % 2).getParentCell().addDoc(vField);
            }
        }

        // Add extra players alternatively to existing user/AI field panels.
        for (int i = 2; i < lstCommands.size(); i++) {
            // If already in layout, no need to add again.
            VCommand cmdView = lstCommands.get(i);
            if (cmdView.getParentCell() == null) {
                lstCommands.get(i % 2).getParentCell().addDoc(cmdView);
            }
        }

        // Add extra hands to existing hand panel.
        for (int i = 1; i < lstHands.size(); i++) {
            // If already in layout, no need to add again.
            if (lstHands.get(i).getParentCell() == null) { // if i == 0, we get NPE in two lines
                DragCell cellWithHand = lstHands.get(0).getParentCell();
                cellWithHand.addDoc(lstHands.get(i));
            }
        }

        // Remove any hand panels that aren't needed anymore
        for (int i = EDocID.Hands.length - 1; i >= lstHands.size(); i--) {
            DragCell cellWithHand = EDocID.Hands[i].getDoc().getParentCell();
            if (cellWithHand != null) {
                cellWithHand.removeDoc(EDocID.Hands[i].getDoc());
                EDocID.Hands[i].setDoc(new VEmptyDoc(EDocID.Hands[i]));
            }
        }

        // Fill in gaps
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final DragCell c : FView.SINGLETON_INSTANCE.getDragCells()) {
                    if (c.getDocs().isEmpty()) {
                        SRearrangingUtil.fillGap(c);
                        FView.SINGLETON_INSTANCE.removeDragCell(c);
                    }
                }
            }
        });
    }

    public CMatchUI getControl() {
        return this.control;
    }

    public void setFieldViews(final List<VField> lst0) {
        this.lstFields = lst0;
    }

    public List<VField> getFieldViews() {
        return lstFields;
    }

    public void setHandViews(final List<VHand> lst0) {
        this.lstHands = lst0;
    }

    public FButton getBtnCancel() {
        return getControl().getCPrompt().getView().getBtnCancel();
    }

    public FButton getBtnOK() {
        return getControl().getCPrompt().getView().getBtnOK();
    }

    public List<VCommand> getCommandViews() {
        return lstCommands;
    }

    public void setCommandViews(final List<VCommand> lstCommands0) {
        this.lstCommands = lstCommands0;
    }

    public List<VHand> getHands() {
        return lstHands;
    }

    @Override
    public boolean onSwitching(final FScreen fromScreen, final FScreen toScreen) {
        return true;
    }

    @Override
    public boolean onClosing(final FScreen screen) {
        if (!Singletons.getControl().getCurrentScreen().equals(screen)) {
            Singletons.getControl().setCurrentScreen(screen);
        }

        final GameView gameView = control.getGameView();
        if (gameView != null && !gameView.isGameOver()) {
            control.concede();
            return false; //delay hiding tab even if concede successful
        }

        //switch back to menus music when closing screen
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);

        return true;
    }
}
