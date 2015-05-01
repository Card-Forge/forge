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
package forge.screens.workshop;

import java.util.List;

import javax.swing.JMenu;

import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.screens.match.controllers.CDetailPicture;
import forge.screens.workshop.menus.CWorkshopUIMenus;
import forge.screens.workshop.views.VWorkshopCatalog;

/**
 * Constructs instance of workshop UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CWorkshopUI implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    private CWorkshopUI() {
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        return new CWorkshopUIMenus().getMenus();
    }

    @Override
    public void register() {
        final CDetailPicture cDetailPicture = VWorkshopCatalog.SINGLETON_INSTANCE.getCDetailPicture();
        EDocID.CARD_PICTURE.setDoc(cDetailPicture.getCPicture().getView());
        EDocID.CARD_DETAIL.setDoc(cDetailPicture.getCDetail().getView());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        Singletons.getControl().getForgeMenu().setProvider(this);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }
}

