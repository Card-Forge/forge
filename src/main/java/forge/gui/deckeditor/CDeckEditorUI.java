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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import forge.Card;
import forge.deck.DeckBase;
import forge.gui.CardContainer;
import forge.gui.deckeditor.SEditorIO.EditorPreference;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CCardCatalog;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.item.InventoryItem;

/**
 * Constructs instance of deck editor UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDeckEditorUI implements CardContainer {
    /** */
    SINGLETON_INSTANCE;

    private ACEditorBase<?, ?> childController;

    private CDeckEditorUI() {
    }

    //========== Overridden from CardContainer

    @Override
    public void setCard(final Card c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }

    @Override
    public Card getCard() {
        return CDetail.SINGLETON_INSTANCE.getCurrentCard();
    }

    /**
     * Set Pack, for when Packs can be shown in the CardPicturePanel.
     * @param item
     */
    public void setCard(final InventoryItem item) {
        CDetail.SINGLETON_INSTANCE.showCard(item);
        CPicture.SINGLETON_INSTANCE.showCard(item);
    }

    //========= Accessor/mutator methods
    /**
     * @return ACEditorBase<?, ?>
     */
    public ACEditorBase<? extends InventoryItem, ? extends DeckBase> getCurrentEditorController() {
        return childController;
    }

    /**
     * Set controller for current configuration of editor.
     * @param editor0 &emsp; {@link forge.gui.deckeditor.controllers.ACEditorBase}<?, ?>
     */
    public void setCurrentEditorController(ACEditorBase<?, ?> editor0) {
        this.childController = editor0;
        updateController();
        if (childController != null) {
            boolean wantElastic = SEditorIO.getPref(EditorPreference.elastic_columns);
            boolean wantUnique = SEditorIO.getPref(EditorPreference.display_unique_only);
            childController.getTableCatalog().setWantElasticColumns(wantElastic);
            childController.getTableDeck().setWantElasticColumns(wantElastic);
            childController.getTableCatalog().setWantUnique(wantUnique);
            childController.getTableDeck().setWantUnique(wantUnique);
        }
        CCardCatalog.SINGLETON_INSTANCE.applyCurrentFilter();
    }

    //========== Other methods
    /**
     * Updates listeners for current controller.
     */
    private void updateController() {
        childController.getTableCatalog().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') { childController.addCard(); }
            }
        });

        childController.getTableDeck().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') { childController.removeCard(); }
            }
        });

        childController.getTableCatalog().getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) { childController.addCard(); }
            }
        });

        childController.getTableDeck().getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) { childController.removeCard(); }
            }
        });

        childController.init();
    }
}
