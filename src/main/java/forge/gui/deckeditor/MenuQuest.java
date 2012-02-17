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
package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import forge.Command;
import forge.Constant;
import forge.deck.Deck;
import forge.deck.io.DeckIOCore;
import forge.deck.io.DeckSerializer;
import forge.error.ErrorViewer;
import forge.gui.ListChooser;
import forge.item.CardDb;
import forge.item.CardPrinted;

//presumes AllZone.getQuestData() is not null
/**
 * <p>
 * Gui_Quest_DeckEditor_Menu class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MenuQuest extends MenuBase<Deck> {
    /** Constant <code>serialVersionUID=-4052319220021158574L</code>. */
    private static final long serialVersionUID = -4052319220021158574L;


    // used for import and export, try to made the gui user friendly
    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;
    /**
     * <p>
     * Constructor for Gui_Quest_DeckEditor_Menu.
     * </p>
     * 
     * @param q
     *            the q
     * @param d
     *            a {@link forge.gui.deckeditor.IDeckDisplay} object.
     * @param exit
     *            a {@link forge.Command} object.
     */
    public MenuQuest(final IDeckManager<Deck> d, final Command exit) {

        super(d, exit);

        this.setupMenu();
    }


    /**
     * <p>
     * importDeck.
     * </p>
     */
    private final void importDeck() {
        final File file = this.getImportFilename();

        if (file != null && file.getName().endsWith(".dck")) {
            try {
                final Deck newDeck = DeckIOCore.readDeck(file);
                getController().importDeck(newDeck);

            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
            }
        }

    } // importDeck()

    /**
     * <p>
     * getImportFilename.
     * </p>
     * 
     * @return a {@link java.io.File} object.
     */
    private final File getImportFilename() {
        final JFileChooser chooser = new JFileChooser(MenuQuest.previousDirectory);

        chooser.addChoosableFileFilter(DeckIOCore.DCK_FILTER);
        final int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            MenuQuest.previousDirectory = file.getParentFile();
            return file;
        }

        return null;
    } // openFileDialog()

    private final ActionListener addCardActionListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent a) {

            // Provide a model here: all unique cards to be displayed by only
            // name (unlike default toString)
            final Iterable<CardPrinted> uniqueCards = CardDb.instance().getAllUniqueCards();
            final List<String> cards = new ArrayList<String>();
            for (final CardPrinted c : uniqueCards) {
                cards.add(c.getName());
            }
            Collections.sort(cards);

            // use standard forge's list selection dialog
            final ListChooser<String> c = new ListChooser<String>("Cheat - Add Card to Your Cardpool", 0, 1, cards);
            if (c.show()) {
                ((DeckEditorQuest)getController().getView()).addCheatCard(CardDb.instance().getCard(c.getSelectedValue()));
            }
        }
    };

    
    protected JMenu getDefaultFileMenu() {
        final JMenu deckMenu = super.getDefaultFileMenu();

        final JMenuItem addCard = new JMenuItem("Cheat - Add Card");

        addCard.addActionListener(this.addCardActionListener);


        if (Constant.Runtime.DEV_MODE[0]) {
            deckMenu.addSeparator();
            deckMenu.add(addCard);
        } 
        
        deckMenu.addSeparator();
        this.addImportExport(deckMenu, true);

        appendCloseMenuItemTo(deckMenu);
       return deckMenu;

    }

    /**
     * <p>
     * addImportExport.
     * </p>
     * 
     * @param menu
     *            a {@link javax.swing.JMenu} object.
     * @param isHumanMenu
     *            a boolean.
     */
    private final void addImportExport(final JMenu menu, final boolean isHumanMenu) {
        final JMenuItem import2 = new JMenuItem("Import");
        final JMenuItem export = new JMenuItem("Export");

        import2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent a) {
                MenuQuest.this.importDeck(); // importDeck(isHumanMenu);
            }
        }); // import

        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent a) {
                MenuQuest.this.exportDeck();
            }
        }); // export

        menu.add(import2);
        menu.add(export);

    } // addImportExport()

    private final void exportDeck() {
        final File filename = this.getExportFilename();
        if (filename == null) {
            return;
        }

        try {
            DeckSerializer.writeDeck(getController().getModel(), filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error, " + ex);
        }
    }

    private final File getExportFilename() {
        final JFileChooser save = new JFileChooser(previousDirectory);
        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckSerializer.DCK_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            return check.endsWith(".dck") ? file : new File(check + ".dck");
        }
        return null;
    }    

}
