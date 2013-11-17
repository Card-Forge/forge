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
package forge.gui.workshop;

import java.util.List;

import javax.swing.JMenu;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.ICDoc;
import forge.gui.menus.IMenuProvider;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.SItemManagerIO.EditorPreference;
import forge.gui.toolbox.itemmanager.table.ItemTable;
import forge.gui.workshop.controllers.CWorkshopCatalog;
import forge.gui.workshop.views.VWorkshopCatalog;
import forge.item.PaperCard;

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
        return null; //TODO: Create Workshop menus
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        Singletons.getControl().getForgeMenu().setProvider(this);
    	final CardManager cardManager = VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager();
        final ItemTable<PaperCard> cardTable = cardManager.getTable();

        boolean wantElastic = SItemManagerIO.getPref(EditorPreference.elastic_columns);
        boolean wantUnique = SItemManagerIO.getPref(EditorPreference.display_unique_only);
        cardTable.setWantElasticColumns(wantElastic);
        cardManager.setWantUnique(wantUnique);
        CWorkshopCatalog.SINGLETON_INSTANCE.applyCurrentFilter();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }
}

