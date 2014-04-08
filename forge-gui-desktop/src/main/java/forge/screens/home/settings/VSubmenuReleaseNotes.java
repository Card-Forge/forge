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

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/** 
 * Displays contents of CHANGES.txt file.
 *
 * @version $Id$
 * 
 */
public enum VSubmenuReleaseNotes implements IVSubmenu<CSubmenuReleaseNotes> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Release Notes");

    private final JPanel pnlMain = new JPanel();
    private SkinnedTextArea tar;
    private final FScrollPane scroller;

    /**
     * Constructor.
     */
    private VSubmenuReleaseNotes() {
        pnlMain.setOpaque(false);
        pnlMain.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        tar = new SkinnedTextArea();
        tar.setOpaque(true);
        tar.setLineWrap(true);
        tar.setWrapStyleWord(true);
        tar.setEditable(false);
        tar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tar.setFont(FSkin.getFixedFont(16));
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tar.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        scroller = new FScrollPane(tar, true);
        pnlMain.add(scroller, "w 100%!, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlMain, "w 98%!, h 98%!, gap 1% 0 1% 0");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Release Notes";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_RELEASE_NOTES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_RELEASE_NOTES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuReleaseNotes getLayoutControl() {
        return CSubmenuReleaseNotes.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param content
     */
    public void setReleaseNotesContent(String content) {
        tar.setText(content);
        setScrollbarToTop();
    }

    private void setScrollbarToTop() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Needs to run in here otherwise does not work.
                scroller.getVerticalScrollBar().setValue(0);
            }
        });
    }
}
