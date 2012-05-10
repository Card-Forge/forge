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
import javax.swing.JPanel;
import javax.swing.JFileChooser;
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
    
    public SaveOpenDialog() {
        fc = new JFileChooser();
    }
    
    /**
     * Shows the open dialog for xml files (layouts). If no file selected, returns default.
     *
     *
     */
    public File OpenXMLDialog(File defFileName) {
        File RetFile = defFileName;
        fc.setCurrentDirectory(defFileName);
        fc.setAcceptAllFileFilterUsed(false);
        FileFilter filter = new FileNameExtensionFilter("Layout Files", "xml");
        fc.addChoosableFileFilter(filter);
        
         
        int RetValue = fc.showOpenDialog(getParent());
        if (RetValue == JFileChooser.APPROVE_OPTION) {
            RetFile = fc.getSelectedFile();
        }
        return RetFile;
    }
    
    /**
     * Shows the save dialog. Not implemented anywhere yet.
     * 
     * 
     */
    public File SaveDialog(File defFileName) {
        File RetFile = defFileName;
        fc.setCurrentDirectory(defFileName);
        
        int RetValue = fc.showSaveDialog(getParent());
        if (RetValue == JFileChooser.APPROVE_OPTION) {
            RetFile = fc.getSelectedFile();
        }
        return RetFile;
    }
    
}