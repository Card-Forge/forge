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
package forge.screens.deckeditor.controllers;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.limited.IBoosterDraft;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.toolbox.FComboBox;

import java.util.Map.Entry;

/** Shows and edits the human player's draft deck in the drafted-cards pane. */
final class DraftPicksSections {
    private final CardManager cardManager;
    private final FComboBox<DeckSection> sectionSelector;
    private IBoosterDraft draft;

    @SuppressWarnings("unchecked")
    DraftPicksSections(CardManager cardManager) {
        this.cardManager = cardManager;
        sectionSelector = cardManager.getCbxSection();
        sectionSelector.removeAllItems();
        sectionSelector.addItem(DeckSection.Main);
        sectionSelector.addItem(DeckSection.Sideboard);
        sectionSelector.setSelectedItem(DeckSection.Main);
        sectionSelector.addActionListener(e -> {
            refresh();
            CStatistics.SINGLETON_INSTANCE.update();
            CProbabilities.SINGLETON_INSTANCE.update();
        });
    }

    void setDraft(IBoosterDraft draft) {
        this.draft = draft;
        refresh();
    }

    DeckSection getDestination() {
        return sectionSelector.getSelectedItem() == DeckSection.Main
                ? DeckSection.Sideboard : DeckSection.Main;
    }

    static DeckSection getPickSection(boolean toAlternate) {
        return toAlternate ? DeckSection.Sideboard : DeckSection.Main;
    }

    void refresh() {
        if (draft == null) {
            return;
        }
        DeckSection section = sectionSelector.getSelectedItem();
        CardPool cards = draft.getHumanPlayer().getDeck().getOrCreate(section);
        cardManager.setPool(cards);
        cardManager.setCaption(section.getLocalizedName() + " (" + cards.countAll() + ")");
    }

    void move(Iterable<Entry<PaperCard, Integer>> cards) {
        move(draft.getHumanPlayer().getDeck(), sectionSelector.getSelectedItem(), cards);
        refresh();
    }

    static void move(Deck deck, DeckSection sourceSection, Iterable<Entry<PaperCard, Integer>> cards) {
        DeckSection destinationSection = sourceSection == DeckSection.Main
                ? DeckSection.Sideboard : DeckSection.Main;
        CardPool source = deck.getOrCreate(sourceSection);
        CardPool destination = deck.getOrCreate(destinationSection);
        for (Entry<PaperCard, Integer> entry : cards) {
            int count = Math.min(source.count(entry.getKey()), entry.getValue());
            if (count > 0) {
                source.remove(entry.getKey(), count);
                destination.add(entry.getKey(), count);
            }
        }
    }
}
