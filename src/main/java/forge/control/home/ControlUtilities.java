/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.Command;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.Singletons;
import forge.error.BugzReporter;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorBase;
import forge.gui.deckeditor.DeckEditorConstructed;
import forge.gui.deckeditor.DeckEditorLimited;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang;
import forge.view.home.ViewUtilities;
import forge.view.toolbox.FSkin;

/**
 * Controls logic and listeners for Utilities panel of the home screen.
 * 
 */
public class ControlUtilities {
    private final ViewUtilities view;
    private final MouseListener madLicensing;
    private final Command cmdDeckEditor, cmdPicDownload, cmdSetDownload, cmdQuestImages, cmdReportBug,
            cmdImportPictures, cmdHowToPlay, cmdDownloadPrices;

    /**
     * 
     * Controls logic and listeners for Utilities panel of the home screen.
     * 
     * @param v0
     *            &emsp; ViewUtilities
     */
    @SuppressWarnings("serial")
    public ControlUtilities(final ViewUtilities v0) {
        this.view = v0;

        this.madLicensing = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                ControlUtilities.this.view.showLicensing();
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                ControlUtilities.this.view.getLblLicensing().setForeground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                ControlUtilities.this.view.getLblLicensing().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        };

        this.cmdDeckEditor = new Command() {
            @Override
            public void execute() {
                ControlUtilities.this.showDeckEditor(GameType.Constructed, null);
            }
        };

        this.cmdPicDownload = new Command() {
            @Override
            public void execute() {
                ControlUtilities.this.doDownloadPics();
            }
        };

        this.cmdSetDownload = new Command() {
            @Override
            public void execute() {
                ControlUtilities.this.doDownloadSetPics();
            }
        };

        this.cmdQuestImages = new Command() {
            @Override
            public void execute() {
                ControlUtilities.this.doDownloadQuestImages();
            }
        };

        this.cmdReportBug = new Command() {
            @Override
            public void execute() {
                final BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        };

        this.cmdImportPictures = new Command() {
            @Override
            public void execute() {
                final GuiImportPicture ip = new GuiImportPicture(null);
                ip.setVisible(true);
            }
        };

        this.cmdHowToPlay = new Command() {
            @Override
            public void execute() {
                final String text = ForgeProps.getLocalized(Lang.HowTo.MESSAGE);

                final JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);
                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(Lang.HowTo.TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        this.cmdDownloadPrices = new Command() {
            @Override
            public void execute() {
                final GuiDownloadPrices gdp = new GuiDownloadPrices();
                gdp.setVisible(true);
            }
        };

        this.addListeners();
    }

    /**
     * Gets the view.
     *
     * @return ViewUtilities
     */
    public ViewUtilities getView() {
        return this.view;
    }

    /**
     * Adds the listeners.
     */
    public void addListeners() {
        this.view.getBtnDownloadPics().setCommand(this.cmdPicDownload);
        this.view.getBtnDownloadSetPics().setCommand(this.cmdSetDownload);
        this.view.getBtnDownloadQuestImages().setCommand(this.cmdQuestImages);
        this.view.getBtnReportBug().setCommand(this.cmdReportBug);
        this.view.getBtnImportPictures().setCommand(this.cmdImportPictures);
        this.view.getBtnHowToPlay().setCommand(this.cmdHowToPlay);
        this.view.getBtnDownloadPrices().setCommand(this.cmdDownloadPrices);
        this.view.getBtnDeckEditor().setCommand(this.cmdDeckEditor);

        this.view.getLblLicensing().removeMouseListener(this.madLicensing);
        this.view.getLblLicensing().addMouseListener(this.madLicensing);
    }

    private void doDownloadPics() {
        new GuiDownloadPicturesLQ(null);
    }

    private void doDownloadSetPics() {
        new GuiDownloadSetPicturesLQ(null);
    }

    private void doDownloadQuestImages() {
        new GuiDownloadQuestImages(null);
    }

    /**
     * Show deck editor.
     *
     * @param <T> the generic type
     * @param gt0 &emsp; GameType
     * @param d0 &emsp; Deck
     */
    @SuppressWarnings("unchecked")
    public <T> void showDeckEditor(final GameType gt0, final T d0) {

        DeckEditorBase<?, T> editor = null;
        if (gt0 == GameType.Constructed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorConstructed();
        } else if (gt0 == GameType.Draft) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(Singletons.getModel().getDecks().getDraft());
        } else if (gt0 == GameType.Sealed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(Singletons.getModel().getDecks().getSealed());
        }

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                Singletons.getControl().getControlHome().getControlConstructed().updateDeckLists();
                // view.getParentView().getControlSealed().updateDeckLists();
            }
        };

        editor.show(exit);

        if (d0 != null) {
            editor.getController().setModel(d0);
        }

        editor.setVisible(true);
    }
}
