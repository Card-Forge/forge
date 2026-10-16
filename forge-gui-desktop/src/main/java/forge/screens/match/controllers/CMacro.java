package forge.screens.match.controllers;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.gui.FThreads;
import forge.gui.framework.DragCell;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SLayoutConstants;
import forge.interfaces.IMacroSystem;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VMacro;
import forge.view.FDialog;
import forge.view.FFrame;
import forge.view.FView;
import forge.Singletons;
import forge.util.Localizer;

public class CMacro implements ICDoc {
    private final CMatchUI matchUI;
    private final VMacro view;
    private final FDialog floatingWindow = new FDialog(false, true, "0") {
        @Override
        public void setLocationRelativeTo(final Component c) {
            if (floatingLocationPrepared) {
                return;
            }
            super.setLocationRelativeTo(c);
        }
    };
    private final JPanel floatingContent = new JPanel();
    private IMacroSystem observedMacroSystem;
    private DragCell dockTargetCell;
    private DragCell highlightedCell;
    private Border dockOriginalBorder;
    private boolean updateQueued;
    private boolean dockDetectionInstalled;
    private boolean floatingLocationPrepared;
    private boolean closedForMatchEnd;

    private final Runnable updateView = this::queueUpdate;

    public CMacro(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VMacro(this);
        floatingContent.setOpaque(false);
        floatingWindow.setTitle(Localizer.getInstance().getMessage("lblMacroWindow"));
        floatingWindow.add(floatingContent, "grow, push");
    }

    public CMatchUI getMatchUI() {
        return matchUI;
    }

    public VMacro getView() {
        return view;
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        closedForMatchEnd = false;
        floatingLocationPrepared = false;
        observeCurrentMacroSystem();
    }

    public void showWindow() {
        closedForMatchEnd = false;
        observeCurrentMacroSystem();
        if (view.getParentCell() != null) {
            view.getParentCell().setSelected(view);
        } else {
            showFloatingWindow();
        }
        update();
    }

    public void showFloatingWindow() {
        closedForMatchEnd = false;
        observeCurrentMacroSystem();
        undockFromCurrentCell();
        view.populateContainer(floatingContent);
        ensureDockDetectionInstalled();
        if (!floatingWindow.isVisible()) {
            setDefaultFloatingBounds();
            floatingWindow.setVisible(true);
        }
        update();
    }

    public void closeForMatchEnd() {
        closedForMatchEnd = true;
        clearDockHighlight();
        dockTargetCell = null;
        floatingLocationPrepared = false;
        floatingWindow.setVisible(false);
        undockFromCurrentCell();

        IMacroSystem macros = observedMacroSystem;
        if (macros == null && matchUI.getGameController() != null) {
            macros = matchUI.getGameController().macros();
        }
        if (macros != null) {
            macros.cancelCurrentMacro();
            macros.removeStatusListener(updateView);
        }
        observedMacroSystem = null;
        updateQueued = false;
    }

    private void setDefaultFloatingBounds() {
        final FFrame mainFrame = Singletons.getView().getFrame();
        final int width = Math.max(260, mainFrame.getWidth() / 5);
        final int height = Math.max(260, mainFrame.getHeight() / 2);
        final int x = mainFrame.getX() + 20;
        final int y = mainFrame.getY() + Math.max(40, (mainFrame.getHeight() - height) / 2);
        floatingWindow.setBounds(x, y, width, height);
        floatingLocationPrepared = true;
    }

    private void dockIntoCell(final DragCell target) {
        if (target == null) {
            return;
        }
        clearDockHighlight();
        floatingWindow.setVisible(false);
        undockFromCurrentCell();
        target.addDoc(view);
        target.setSelected(view);
        update();
    }

    private void undockFromCurrentCell() {
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (cell.getDocs().contains(view)) {
                cell.removeDoc(view);
            }
        }
        view.setParentCell(null);
    }

    private void ensureDockDetectionInstalled() {
        if (dockDetectionInstalled) {
            return;
        }
        dockDetectionInstalled = true;
        floatingWindow.getTitleBar().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (dockTargetCell != null) {
                    final DragCell target = dockTargetCell;
                    dockTargetCell = null;
                    dockIntoCell(target);
                }
            }
        });
        floatingWindow.getTitleBar().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    detectDockTarget(e);
                }
            }
        });
    }

    private void detectDockTarget(final MouseEvent e) {
        final int ex = (int) e.getLocationOnScreen().getX();
        final int ey = (int) e.getLocationOnScreen().getY();

        DragCell newTarget = null;
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            final int cx = cell.getAbsX();
            final int cy = cell.getAbsY();
            final int cw = cell.getW();
            if (ex >= cx && ey >= cy && ex <= cx + cw && ey <= cy + SLayoutConstants.HEAD_H * 3 / 2) {
                newTarget = cell;
                break;
            }
        }

        if (newTarget != dockTargetCell) {
            clearDockHighlight();
            dockTargetCell = newTarget;
            applyDockHighlight();
        }
    }

    private void applyDockHighlight() {
        if (dockTargetCell == null) {
            return;
        }
        highlightedCell = dockTargetCell;
        dockOriginalBorder = highlightedCell.getBody().getBorder();
        highlightedCell.getBody().setBorder(BorderFactory.createLineBorder(new Color(70, 130, 230), 2));
    }

    private void clearDockHighlight() {
        if (highlightedCell != null) {
            highlightedCell.getBody().setBorder(dockOriginalBorder);
            highlightedCell = null;
        }
        dockOriginalBorder = null;
    }

    private void observeCurrentMacroSystem() {
        if (closedForMatchEnd) {
            return;
        }
        if (matchUI.getGameController() == null) {
            return;
        }

        final IMacroSystem macros = matchUI.getGameController().macros();
        if (macros == observedMacroSystem) {
            return;
        }
        if (observedMacroSystem != null) {
            observedMacroSystem.removeStatusListener(updateView);
        }
        observedMacroSystem = macros;
        observedMacroSystem.addStatusListener(updateView);
    }

    private void queueUpdate() {
        if (updateQueued) {
            return;
        }
        updateQueued = true;
        FThreads.invokeInEdtLater(() -> {
            updateQueued = false;
            if (closedForMatchEnd) {
                return;
            }
            update();
        });
    }

    @Override
    public void update() {
        if (closedForMatchEnd) {
            return;
        }
        observeCurrentMacroSystem();
        view.updateMacroStatus();
    }
}
