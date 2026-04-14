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
package forge.adventure.util;

import forge.card.CardEdition;
import forge.item.SealedTemplate;
import forge.item.generation.UnOpenedProduct;
import forge.model.CardBlock;
import forge.model.FModel;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plane-scoped overrides for block and booster data. Parsed from the
 * adventure's {@code blockdata/blocks.txt} and {@code editions/*.txt}
 * files; never mutates upstream static data. Adventure consumers query
 * this registry and fall through to {@link FModel} when no override is
 * present; classic-mode code paths keep reading the upstream data.
 */
public final class AdventureOverrides {
    private static final AdventureOverrides INSTANCE = new AdventureOverrides();

    public static AdventureOverrides instance() {
        return INSTANCE;
    }

    private final Map<String, CardBlock> blocks = new LinkedHashMap<>();
    private final Map<String, SealedTemplate> boosters = new HashMap<>();

    private AdventureOverrides() {}

    /**
     * Reload overrides for the current plane. Safe to call repeatedly;
     * absent overlay files clear the registry back to empty.
     */
    public void load(String prefix, CardEdition.Collection editions) {
        blocks.clear();
        boosters.clear();

        File editionsDir = new File(prefix + "editions");
        if (editionsDir.isDirectory()) {
            try {
                CardEdition.Reader reader = new CardEdition.Reader(editionsDir);
                for (CardEdition patch : reader.readAll().values()) {
                    SealedTemplate tpl = patch.getBoosterTemplate();
                    if (tpl != null) {
                        boosters.put(patch.getCode(), tpl);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load adventure edition overrides from " + editionsDir + ": " + e);
            }
        }

        File blocksOverlay = new File(prefix + "blockdata/blocks.txt");
        if (blocksOverlay.isFile()) {
            try {
                CardBlock.Reader reader = new CardBlock.Reader(blocksOverlay.getAbsolutePath(), editions);
                for (Map.Entry<String, CardBlock> entry : reader.readAll().entrySet()) {
                    CardBlock block = entry.getValue();
                    block.setBoosterResolver(code -> {
                        SealedTemplate tpl = boosters.get(code);
                        return tpl == null ? null : new UnOpenedProduct(tpl);
                    });
                    blocks.put(entry.getKey(), block);
                }
            } catch (Exception e) {
                System.err.println("Failed to load adventure block overrides from " + blocksOverlay + ": " + e);
            }
        }
    }

    /** @return the override block for the given name, or the upstream block if none. */
    public CardBlock getBlock(String name) {
        CardBlock override = blocks.get(name);
        return override != null ? override : FModel.getBlocks().get(name);
    }

    /**
     * Iterate all blocks, substituting any name-matched overrides for
     * upstream entries. Purely additive overrides (names not in
     * upstream) are appended at the end.
     */
    public Iterable<CardBlock> allBlocks() {
        if (blocks.isEmpty()) {
            return FModel.getBlocks();
        }
        Map<String, CardBlock> merged = new LinkedHashMap<>();
        for (CardBlock b : FModel.getBlocks()) {
            CardBlock override = blocks.get(b.getName());
            merged.put(b.getName(), override != null ? override : b);
        }
        for (Map.Entry<String, CardBlock> entry : blocks.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableCollection(merged.values());
    }

    /** @return the override booster template for the given set code, or {@code null} if none. */
    public SealedTemplate getBoosterTemplate(String setCode) {
        return boosters.get(setCode);
    }
}
