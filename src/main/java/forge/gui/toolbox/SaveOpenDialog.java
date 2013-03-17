/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2012 Forge Team
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

package forge.gui.toolbox;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 
 * A class for showing open or save dialogs in Forge.
 * 
 *
 *
 */
@SuppressWarnings("serial")
public class SaveOpenDialog extends JPanel {

    private JFileChooser fc;

    /**
     *  Enum to contain information for filetype filtering in the open/save dialog.
     *  Add more entries to enum as needed.
     * 
     * 
     */
    public enum Filetypes {
        LAYOUT  ("Layout File", "xml"),
        DECK    ("Deck File",   "dck");

        private final String TypeName;
        private final String TypeExtension;
        Filetypes(String Name, String Extension) {
            this.TypeName = Name;
            this.TypeExtension = Extension;
        }
    }

    /**
     * 
     * constructor for a save or open dialog.
     */
    public SaveOpenDialog() {
        fc = new JFileChooser();
    }

    /**
     * Shows the open dialog for files. If no file selected, returns null. Pass null
     * to Type to allow all files to be viewed/opened.
     * @param defFileName pass the default file to use, also determines directory
     * @param type label to tell what type of file to filter
     * 
     *
     */
    public File OpenDialog(final File defFileName, final Filetypes type) {
        fc.setCurrentDirectory(defFileName);

        if (type != null) {
            fc.setAcceptAllFileFilterUsed(false);
            final FileFilter filter = new FileNameExtensionFilter(type.TypeName, type.TypeExtension);
            fc.addChoosableFileFilter(filter);
        }

        int RetValue = fc.showOpenDialog(getParent());
        if (RetValue == JFileChooser.APPROVE_OPTION) {
            final File RetFile = fc.getSelectedFile();
            return RetFile;
        }
        return null;
    }

    /**
     * Shows the save dialog.
     * 
     * @param defFileName default file name/directory to show when save dialog is opened
     * @param type file types to show in dialog
     * 
     * 
     */
    public File SaveDialog(final File defFileName, final Filetypes type) {
        File RetFile = defFileName;
        fc.setCurrentDirectory(defFileName);

        /* set the file filter if desired */
        if (type != null) {
            fc.setAcceptAllFileFilterUsed(false);
            final FileFilter filter = new FileNameExtensionFilter(type.TypeName, type.TypeExtension);
            fc.addChoosableFileFilter(filter);
        }

        int RetValue = fc.showSaveDialog(getParent());

        /* user picked save */
        if (RetValue == JFileChooser.APPROVE_OPTION) {
            RetFile = fc.getSelectedFile();

            /* Adds extension if it is known and not given */
            if (type != null & !(RetFile.getAbsolutePath().endsWith(type.TypeExtension))) {
                RetFile = new File(RetFile.getAbsolutePath() + "." + type.TypeExtension);
            }

            return RetFile;
        }

        /* user picked cancel */
        return null;
    }

}
