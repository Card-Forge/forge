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
package forge.itemmanager.views;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JTable;

import forge.deck.Deck;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.screens.deckeditor.CDeckEditorUI;

/**
 * Cell renderer for displaying and toggling key card status in deck editor.
 */
public class DeckKeyCardRenderer extends ItemCellRenderer {
    private static final String KEY_CARD_INDICATOR = "\uD83D\uDD11";

    @Override
    public boolean alwaysShowTooltip() {
        return true;
    }

    @Override
    public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1) {
            if (value instanceof PaperCard card) {
                final Deck currentDeck = (Deck) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().getModel();

                if (currentDeck != null) {
                    toggleKeyCard(currentDeck, card);
                    // Repaint the current table
                    listView.getTable().repaint();
                    // Also repaint the other manager's table to update all cards with this name
                    refreshOtherManagerTable();
                    e.consume();
                }
            }
        }
    }

    private void refreshOtherManagerTable() {
        try {
            final CDeckEditorUI editor = CDeckEditorUI.SINGLETON_INSTANCE;
            final forge.itemmanager.ItemManager<?> deckManager = editor.getCurrentEditorController().getDeckManager();
            final forge.itemmanager.ItemManager<?> catalogManager = editor.getCurrentEditorController().getCatalogManager();

            // Refresh both managers to update all cards with this name
            if (deckManager != null) {
                deckManager.refresh();
            }
            if (catalogManager != null) {
                catalogManager.refresh();
            }
        } catch (Exception ex) {
            // Silently ignore if unable to refresh other table
        }
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final StringBuilder label = new StringBuilder();
        final StringBuilder tooltip = new StringBuilder();

        if (value instanceof PaperCard card) {
            final Deck currentDeck = (Deck) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().getModel();

            if (currentDeck != null) {
                // Check if this card is in the key cards list
                boolean isKeyCard = false;
                for (String keyCardName : currentDeck.getKeyCards()) {
                    if (keyCardName.equalsIgnoreCase(card.getName())) {
                        isKeyCard = true;
                        break;
                    }
                }

                if (isKeyCard) {
                    label.append(KEY_CARD_INDICATOR);
                    tooltip.append("Click to unmark as key card");
                } else {
                    tooltip.append("Click to mark as key card");
                }
            }
        }

        this.setToolTipText(!tooltip.isEmpty() ? tooltip.toString() : null);
        return super.getTableCellRendererComponent(table, label.toString(), isSelected, hasFocus, row, column);
    }

    private void toggleKeyCard(final Deck deck, final PaperCard card) {
        String cardName = card.getName();

        if (deck.isKeyCard(cardName)) {
            // Remove from key cards
            deck.removeKeyCard(cardName);
        } else {
            // Add to key cards
            deck.addKeyCard(cardName);
        }

        // Mark deck as modified so it will be saved
        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().notifyModelChanged();
    }
}


