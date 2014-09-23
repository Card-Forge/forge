/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.controllers;

import java.io.File;

import forge.FThreads;
import forge.GuiBase;
import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deckchooser.FDeckViewer;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SLayoutIO;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.FileLocation;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VDock;
import forge.toolbox.FSkin;
import forge.toolbox.SaveOpenDialog;
import forge.toolbox.SaveOpenDialog.Filetypes;
import forge.view.FView;

/**
 * Controls the dock panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDock implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private int arcState;

    /**
     * End turn.
     */
    public void endTurn() {
        CPrompt.SINGLETON_INSTANCE.passPriorityUntilEndOfTurn();
    }

    public void revertLayout() {
        SOverlayUtils.genericOverlay();
        FView.SINGLETON_INSTANCE.getPnlContent().removeAll();

        FThreads.invokeInEdtLater(GuiBase.getInterface(), new Runnable(){
            @Override public void run() {
                SLayoutIO.loadLayout(null);
                SOverlayUtils.hideOverlay();
            }
        });
    }

    public void saveLayout() {
        final SaveOpenDialog dlgSave = new SaveOpenDialog();
        final FileLocation layoutFile = Singletons.getControl().getCurrentScreen().getLayoutFile();
        final File defFile = layoutFile != null ? new File(layoutFile.userPrefLoc) : null;
        final File saveFile = dlgSave.SaveDialog(defFile, Filetypes.LAYOUT);
        if (saveFile != null) {
            SLayoutIO.saveLayout(saveFile);
        }
    }

    public void openLayout() {
        SOverlayUtils.genericOverlay();

        final SaveOpenDialog dlgOpen = new SaveOpenDialog();
        final FileLocation layoutFile = Singletons.getControl().getCurrentScreen().getLayoutFile();
        final File defFile = layoutFile != null ? new File(layoutFile.userPrefLoc) : null;
        final File loadFile = dlgOpen.OpenDialog(defFile, Filetypes.LAYOUT);

        if (loadFile != null) {
            FView.SINGLETON_INSTANCE.getPnlContent().removeAll();
            // let it redraw everything first

            FThreads.invokeInEdtLater(GuiBase.getInterface(), new Runnable() {
                @Override
                public void run() {
                    if (loadFile != null) {
                        SLayoutIO.loadLayout(loadFile);
                        SLayoutIO.saveLayout(null);
                    }
                    SOverlayUtils.hideOverlay();
                }
            });
        }
    }

    /**
     * View deck list.
     */
    public void viewDeckList() {
        final Deck deck = FControl.instance.getGameView().getDeck(GamePlayerUtil.getGuiPlayer());
        if (deck != null) {
            FDeckViewer.show(deck);
        }
    }

    /**
     * @return int State of targeting arc preference:<br>
     * 0 = don't draw<br>
     * 1 = draw on card mouseover<br>
     * 2 = always draw
     */
    public int getArcState() {
        return arcState;
    }

    /** @param state0 int */
    private void refreshArcStateDisplay() {
        switch (arcState) {
        case 0:
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setToolTipText("Targeting arcs: Off");
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSOFF));
            VDock.SINGLETON_INSTANCE.getBtnTargeting().repaintSelf();
            break;
        case 1:
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setToolTipText("Targeting arcs: Card mouseover");
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSHOVER));
            VDock.SINGLETON_INSTANCE.getBtnTargeting().repaintSelf();
            break;
        default:
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSON));
            VDock.SINGLETON_INSTANCE.getBtnTargeting().setToolTipText("Targeting arcs: Always on");
            VDock.SINGLETON_INSTANCE.getBtnTargeting().repaintSelf();
            break;
        }

        FModel.getPreferences().setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState));
        //FModel.SINGLETON_INSTANCE.getPreferences().save();
    }

    /** Toggle targeting overlay painting. */
    public void toggleTargeting() {
        arcState++;

        if (arcState == 3) { arcState = 0; }

        refreshArcStateDisplay();
        FView.SINGLETON_INSTANCE.getFrame().repaint(); // repaint the match UI
    }

    public void setArcState(int state) {
        arcState = state;
    }

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
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final String temp = FModel.getPreferences()
                .getPref(FPref.UI_TARGETING_OVERLAY);

        // Old preference used boolean; new preference needs 0-1-2
        // (none, mouseover, solid).  Can remove this conditional
        // statement after a while...Doublestrike 17-10-12
        if (temp.equals("0") || temp.equals("1")) {
            arcState = Integer.valueOf(temp);
        }
        else {
            arcState = 2;
        }

        refreshArcStateDisplay();

        VDock.SINGLETON_INSTANCE.getBtnConcede().setCommand(new UiCommand() {
            @Override
            public void run() {
                CMatchUI.SINGLETON_INSTANCE.concede();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnSettings().setCommand(new UiCommand() {
            @Override
            public void run() {
                SOverlayUtils.showOverlay();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnEndTurn().setCommand(new UiCommand() {
            @Override
            public void run() {
                endTurn();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnViewDeckList().setCommand(new UiCommand() {
            @Override
            public void run() {
                viewDeckList();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnRevertLayout().setCommand(new UiCommand() {
            @Override
            public void run() {
                revertLayout();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnOpenLayout().setCommand(new UiCommand() {
            @Override
            public void run() {
                openLayout();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnSaveLayout().setCommand(new UiCommand() {
            @Override
            public void run() {
                saveLayout();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnAlphaStrike().setCommand(new UiCommand() {
            @Override
            public void run() {
                Singletons.getControl().getGameView().alphaStrike();
            }
        });
        VDock.SINGLETON_INSTANCE.getBtnTargeting().setCommand(new UiCommand() {
            @Override
            public void run() {
                toggleTargeting();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

}
