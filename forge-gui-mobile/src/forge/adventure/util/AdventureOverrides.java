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

import forge.StaticData;
import forge.adventure.data.ConfigData;
import forge.card.CardEdition;
import forge.item.SealedTemplate;
import forge.item.generation.UnOpenedProduct;
import forge.model.CardBlock;
import forge.model.FModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plane-scoped overrides for block and booster data. Parsed from the
 * adventure's {@code blockdata/blocks.txt} and {@code editions/*.txt}
 * files; never mutates upstream static data. Overlay blocks are added
 * on top of the upstream set, and the plane's {@code restrictedBlocks}
 * config entry hides upstream blocks from Adventure event rolls.
 * Classic-mode code paths keep reading the unmodified upstream data.
 */
public final class AdventureOverrides {
    private static final AdventureOverrides INSTANCE = new AdventureOverrides();

    public static AdventureOverrides instance() {
        return INSTANCE;
    }

    private final Map<String, CardBlock> blocks = new LinkedHashMap<>();
    private final Map<String, SealedTemplate> boosters = new HashMap<>();
    private final Set<String> restrictedBlocks = new HashSet<>();

    private AdventureOverrides() {}

    /**
     * Reload overrides for the current plane. Safe to call repeatedly;
     * absent overlay files and config entries clear the registry back
     * to empty.
     */
    public void load(String prefix, CardEdition.Collection editions, ConfigData configData) {
        blocks.clear();
        boosters.clear();
        restrictedBlocks.clear();

        if (configData != null && configData.restrictedBlocks != null) {
            Collections.addAll(restrictedBlocks, configData.restrictedBlocks);
        }

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
                    String name = entry.getKey();
                    if (FModel.getBlocks().contains(name)) {
                        System.err.println("Adventure block overlay '" + name
                                + "' collides with an upstream block; pick a distinct name and list the upstream block in restrictedBlocks.");
                        continue;
                    }
                    CardBlock block = entry.getValue();
                    block.setBoosterResolver(code -> {
                        SealedTemplate tpl = boosters.get(code);
                        return tpl == null ? null : new UnOpenedProduct(tpl);
                    });
                    blocks.put(name, block);
                }
            } catch (Exception e) {
                System.err.println("Failed to load adventure block overrides from " + blocksOverlay + ": " + e);
            }
        }
    }

    /**
     * @return the overlay block for the given name, or the upstream
     * block if it is not restricted, or {@code null} if the name maps
     * to a restricted upstream block with no overlay.
     */
    public CardBlock getBlock(String name) {
        CardBlock overlay = blocks.get(name);
        if (overlay != null) return overlay;
        if (restrictedBlocks.contains(name)) return null;
        return FModel.getBlocks().get(name);
    }

    /**
     * Iterate upstream blocks (minus any listed in {@code restrictedBlocks})
     * followed by overlay blocks. Overlay entries are purely additive.
     */
    public Iterable<CardBlock> allBlocks() {
        if (blocks.isEmpty() && restrictedBlocks.isEmpty()) {
            return FModel.getBlocks();
        }
        List<CardBlock> merged = new ArrayList<>();
        for (CardBlock b : FModel.getBlocks()) {
            if (restrictedBlocks.contains(b.getName())) continue;
            merged.add(b);
        }
        merged.addAll(blocks.values());
        return Collections.unmodifiableList(merged);
    }

    /**
     * @return the overlay booster template for the given set code, or the
     * upstream template if no overlay exists, or {@code null} if neither has one.
     */
    public SealedTemplate getBoosterTemplate(String setCode) {
        SealedTemplate overlay = boosters.get(setCode);
        if (overlay != null) return overlay;
        return StaticData.instance().getBoosters().get(setCode);
    }
}
