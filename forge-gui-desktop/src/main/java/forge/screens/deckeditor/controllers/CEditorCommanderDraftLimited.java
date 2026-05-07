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

import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGeneratorBase;
import forge.gamemodes.limited.CommanderDraftUtil;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.storage.IStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Deck editor for post-draft Commander Draft deck building.
 *
 * <p>Extends the standard limited editor ({@link CEditorLimited}) with:</p>
 * <ul>
 *   <li>A {@link DeckSection#Commander} section prepended to the section list so
 *       the player is routed there first to pick their commander(s).</li>
 *   <li>A catalog filtered to color-identity-legal cards when the
 *       {@link DeckSection#Main} section is active.</li>
 *   <li>Commander-section add/remove logic that enforces the 1-or-2-commander
 *       limit and the Commander Draft partner rules.</li>
 * </ul>
 */
public final class CEditorCommanderDraftLimited extends CEditorLimited {

    private final String freeCommanderName;
    private final Localizer localizer = Localizer.getInstance();

    /**
     * @param deckMap0          storage backing this draft's deck group
     * @param screen0           the FScreen this editor lives on
     * @param cDetailPicture0   detail-picture controller
     * @param freeCommanderName edition's free commander name (may be {@code null})
     */
    public CEditorCommanderDraftLimited(final IStorage<DeckGroup> deckMap0,
            final FScreen screen0, final CDetailPicture cDetailPicture0,
            final String freeCommanderName) {
        super(deckMap0, screen0, cDetailPicture0);
        this.freeCommanderName = freeCommanderName;

        // Insert the Commander section at position 0 so it appears first
        allSections.add(0, DeckSection.Commander);

        // Rebuild the section combo box to include Commander
        this.getCbxSection().removeAllItems();
        for (DeckSection section : allSections) {
            this.getCbxSection().addItem(section);
        }
    }

    // -------------------------------------------------------------------------
    // Section mode setup
    // -------------------------------------------------------------------------

    @Override
    public void setEditorMode(final DeckSection sectionMode) {
        switch (sectionMode) {
            case Commander:
                setupCommanderSection();
                break;
            case Main:
                setupMainSection();
                break;
            default:
                super.setEditorMode(sectionMode);
                return;
        }
        this.sectionMode = sectionMode;
        this.getDeckController().updateCaptions();
    }

    /** Catalog = legal commanders from pool + free options; deck manager = Commander section. */
    private void setupCommanderSection() {
        this.getCatalogManager().setup(ItemManagerConfig.DRAFT_POOL);
        this.getCatalogManager().setPool(buildCommanderCatalog(), false);
        this.getDeckManager().setPool(getHumanDeck().getOrCreate(DeckSection.Commander));
    }

    /** Catalog = color-identity-filtered sideboard; deck manager = Main. */
    private void setupMainSection() {
        this.getCatalogManager().setup(ItemManagerConfig.DRAFT_POOL);
        this.getDeckManager().setPool(getHumanDeck().getOrCreate(DeckSection.Main));

        final CardPool cmdSection = getHumanDeck().get(DeckSection.Commander);
        if (cmdSection != null && !cmdSection.isEmpty()) {
            final ColorSet identity =
                    CommanderDraftUtil.getCommanderColorIdentity(cmdSection.toFlatList());
            final ItemPool<PaperCard> filtered = new ItemPool<>(PaperCard.class);
            for (final Map.Entry<PaperCard, Integer> entry
                    : getHumanDeck().getOrCreate(DeckSection.Sideboard)) {
                final PaperCard card = entry.getKey();
                if (card.getRules().getColorIdentity().hasNoColorsExcept(identity)
                        || DeckGeneratorBase.COLORLESS_CARDS.test(card.getRules())) {
                    filtered.add(card, entry.getValue());
                }
            }
            this.getCatalogManager().setPool(filtered, false);
        } else {
            this.getCatalogManager().setPool(
                    getHumanDeck().getOrCreate(DeckSection.Sideboard), false);
        }
    }

    // -------------------------------------------------------------------------
    // Add / remove items
    // -------------------------------------------------------------------------

    @Override
    protected void onAddItems(final Iterable<Map.Entry<PaperCard, Integer>> items,
            final boolean toAlternate) {
        if (toAlternate) { return; }
        if (sectionMode == DeckSection.Commander) {
            handleCommanderAdd(items.iterator().next().getKey());
        } else {
            super.onAddItems(items, toAlternate);
        }
    }

    @Override
    protected void onRemoveItems(final Iterable<Map.Entry<PaperCard, Integer>> items,
            final boolean toAlternate) {
        if (toAlternate) { return; }
        if (sectionMode == DeckSection.Commander) {
            for (final Map.Entry<PaperCard, Integer> entry : items) {
                this.getDeckManager().removeItem(entry.getKey(), entry.getValue());
                if (!isFreeOption(entry.getKey())) {
                    this.getCatalogManager().addItem(entry.getKey(), entry.getValue());
                }
            }
            this.getDeckController().notifyModelChanged();
        } else {
            super.onRemoveItems(items, toAlternate);
        }
    }

    // -------------------------------------------------------------------------
    // Reset / context menus
    // -------------------------------------------------------------------------

    @Override
    public void resetTables() {
        // Always start on Commander section so players pick their commander(s) first
        setEditorMode(DeckSection.Commander);
    }

    @Override
    protected void buildAddContextMenu(final EditorContextMenuBuilder cmb) {
        if (sectionMode == DeckSection.Commander) {
            cmb.addMoveItems(localizer.getMessage("lblAddcard"), null);
        } else {
            super.buildAddContextMenu(cmb);
        }
    }

    @Override
    protected void buildRemoveContextMenu(final EditorContextMenuBuilder cmb) {
        if (sectionMode == DeckSection.Commander) {
            cmb.addMoveItems("Move", "to pool");
        } else {
            super.buildRemoveContextMenu(cmb);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Handle adding a card to the Commander section.
     * Enforces ≤2 commanders, respecting partner rules.
     */
    private void handleCommanderAdd(final PaperCard newCard) {
        final CardPool cmdPool = getHumanDeck().getOrCreate(DeckSection.Commander);
        final List<PaperCard> current = new ArrayList<>(cmdPool.toFlatList());

        boolean isValidPartnerAdd = false;
        if (current.size() == 1) {
            final PaperCard first = current.get(0);
            isValidPartnerAdd = newCard.getRules().canBePartnerCommanders(first.getRules())
                    || (CommanderDraftUtil.isPartnerEligible(first)
                            && CommanderDraftUtil.isPartnerEligible(newCard));
        }

        if (!isValidPartnerAdd) {
            // Replace all existing commanders
            returnCommandersToCatalog(current);
            this.getDeckManager().removeAllItems();
        } else if (current.size() >= 2) {
            // Already have 2 — replace second partner
            final PaperCard second = current.get(1);
            returnCommandersToCatalog(Collections.singletonList(second));
            this.getDeckManager().removeItem(second, 1);
        }

        this.getDeckManager().addItem(newCard, 1);
        if (!isFreeOption(newCard)) {
            this.getCatalogManager().removeItem(newCard, 1);
        }
        this.getDeckController().notifyModelChanged();
    }

    private void returnCommandersToCatalog(final List<PaperCard> commanders) {
        for (final PaperCard cmd : commanders) {
            if (!isFreeOption(cmd)) {
                this.getCatalogManager().addItem(cmd, 1);
            }
        }
    }

    /** Build catalog for Commander section: legal commanders from pool + free options. */
    private ItemPool<PaperCard> buildCommanderCatalog() {
        final CardPool sideboard = getHumanDeck().getOrCreate(DeckSection.Sideboard);
        final ItemPool<PaperCard> catalog = new ItemPool<>(PaperCard.class);

        for (final PaperCard card : sideboard.toFlatList()) {
            if (card.getRules().canBeCommander()) {
                catalog.add(card, 1);
            }
        }
        addFreeOption(catalog, sideboard, freeCommanderName);
        for (final String fallback : CommanderDraftUtil.FREE_COMMANDER_FALLBACKS) {
            addFreeOption(catalog, sideboard, fallback);
        }
        return catalog;
    }

    private static void addFreeOption(final ItemPool<PaperCard> catalog,
            final CardPool sideboard, final String name) {
        if (name == null || name.isEmpty()) { return; }
        final boolean inPool = sideboard.toFlatList().stream()
                .anyMatch(c -> c.getName().equals(name));
        final boolean inCatalog = catalog.toFlatList().stream()
                .anyMatch(c -> c.getName().equals(name));
        if (!inPool && !inCatalog) {
            final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
            if (card != null) {
                catalog.add(card, 1);
            }
        }
    }

    /** Returns true when a card is a free-option commander (not from the draft pool). */
    private boolean isFreeOption(final PaperCard card) {
        final String name = card.getName();
        if (name.equals(freeCommanderName)) { return true; }
        for (final String fallback : CommanderDraftUtil.FREE_COMMANDER_FALLBACKS) {
            if (name.equals(fallback)) { return true; }
        }
        return false;
    }
}

