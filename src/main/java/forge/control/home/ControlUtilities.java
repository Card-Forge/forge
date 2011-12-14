package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.Command;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.deck.Deck;
import forge.error.BugzReporter;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorCommon;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang;
import forge.view.home.ViewUtilities;

/** 
 * Controls logic and listeners for Utilities panel of the home screen.
 *
 */
public class ControlUtilities {
    private ViewUtilities view;
    private boolean licensingExpanded = false;
    /**
     * 
     * Controls logic and listeners for Utilities panel of the home screen.
     * 
     * @param v0 &emsp; ViewUtilities
     */
    public ControlUtilities(ViewUtilities v0) {
        this.view = v0;
        addListeners();
    }

    /** @return ViewUtilities */
    public ViewUtilities getView() {
        return view;
    }

    /** */
    public void addListeners() {
        this.view.getBtnDownloadPics().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadPics();
            }
        });

        this.view.getBtnDownloadSetPics().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadSetPics();
            }
        });

        this.view.getBtnDownloadQuestImages().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadQuestImages();
            }
        });

        this.view.getBtnReportBug().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        });
        this.view.getBtnImportPictures().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final GuiImportPicture ip = new GuiImportPicture(null);
                ip.setVisible(true);
            }
        });
        this.view.getBtnHowToPlay().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final String text = ForgeProps.getLocalized(Lang.HowTo.MESSAGE);

                final JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);
                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(Lang.HowTo.TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        this.view.getBtnDownloadPrices().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final GuiDownloadPrices gdp = new GuiDownloadPrices();
                gdp.setVisible(true);
            }
        });

        this.view.getBtnDeckEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                showDeckEditor(null, null);
            }
        });

        this.view.getTarLicensing().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (licensingExpanded) {
                    hideLicenseInfo();
                    licensingExpanded = false;
                }
                else {
                    showLicenseInfo();
                    licensingExpanded = true;
                }
            }
        });
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

    private void showLicenseInfo() {
        view.getTarLicensing().setText(
                "This program is free software : you can redistribute it and/or modify "
                + "it under the terms of the GNU General Public License as published by "
                + "the Free Software Foundation, either version 3 of the License, or "
                + "(at your option) any later version."
                + "\r\n"
                + "This program is distributed in the hope that it will be useful, "
                + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
                + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
                + "GNU General Public License for more details."
                + "\r\n"
                + "You should have received a copy of the GNU General Public License "
                + "along with this program.  If not, see <http://www.gnu.org/licenses/>."
       );
    }

    private void hideLicenseInfo() {
        view.getTarLicensing().setText("Click here for license information.");
    }

    /**
     * @param gt0 &emsp; GameType
     * @param d0 &emsp; Deck
     */
    public void showDeckEditor(GameType gt0, Deck d0) {
        if (gt0 == null) {
            gt0 = GameType.Constructed;
        }

        DeckEditorCommon editor = new DeckEditorCommon(gt0);

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                view.getParentView().getConstructedController().updateDeckNames();
                view.getParentView().getSealedController().updateDeckLists();
            }
        };

        editor.show(exit);

        if (d0 != null) {
            editor.getCustomMenu().showDeck(d0, gt0);
        }

        editor.setVisible(true);
    }
}
