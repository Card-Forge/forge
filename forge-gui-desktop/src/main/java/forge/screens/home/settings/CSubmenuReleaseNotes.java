/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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

package forge.screens.home.settings;

import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgeConstants;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.TextUtil;

import java.io.File;

/**
 * Controller for VSubmenuReleaseNotes submenu in the home UI.
 *
 * @version $Id$
 *
 */
public enum CSubmenuReleaseNotes implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private VSubmenuReleaseNotes view;
    //private ForgePreferences prefs;
    private boolean isReleaseNotesUpdated = false;

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        this.view = VSubmenuReleaseNotes.SINGLETON_INSTANCE;
        //this.prefs = FModel.getPreferences();
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        this.view = VSubmenuReleaseNotes.SINGLETON_INSTANCE;
        //this.prefs = FModel.getPreferences();
        setReleaseNotesContent();
    }

    private void setReleaseNotesContent() {
        if (!this.isReleaseNotesUpdated) {
            this.view.setReleaseNotesContent(getReleaseNotes());
            this.isReleaseNotesUpdated = true;
        }
    }

    /**
     * Returns content of README.txt (release) or CHANGES.txt (snapshot).
     */
    private static String getReleaseNotes() {
        final String filename = ForgeConstants.CHANGES_FILE_NO_RELEASE;
        final String filePath = FileUtil.pathCombine(System.getProperty("user.dir"), filename);

        //The file packaged with full releases is not the same as the one for snapshots
        final String filenameRelease = ForgeConstants.CHANGES_FILE;
        final String filePathRelease = FileUtil.pathCombine(System.getProperty("user.dir"), filenameRelease);

        String notes;

        if (FileUtil.doesFileExist(filePath)) {
            // get release notes
            notes = BuildInfo.getVersionString() +" Changelog:\n\n" + TextUtil.getFormattedChangelog(new File(filePath), ForgeConstants.CHANGES_FILE_NO_RELEASE);
        } else if (FileUtil.doesFileExist(filePathRelease)) {
            notes = filePathRelease + "\n\n" + FileUtil.readFileToString(filePathRelease);
        } else {
            notes = filePath + "\nis MISSING!";
        }
        return notes;

    }

}
