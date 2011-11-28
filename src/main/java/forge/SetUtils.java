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
package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardBlock;
import forge.card.CardSet;
import forge.game.GameFormat;

/**
 * <p>
 * SetInfoUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class SetUtils {

    private SetUtils() {
        throw new AssertionError();
    }

    /** Constant <code>setData</code>. */
    private static Map<String, CardSet> setsByCode = new HashMap<String, CardSet>();
    private static List<CardSet> allSets = new ArrayList<CardSet>();
    private static List<CardBlock> allBlocks = new ArrayList<CardBlock>();

    private static List<GameFormat> formats = new ArrayList<GameFormat>();
    private static GameFormat fmtStandard = null;
    private static GameFormat fmtExtended = null;
    private static GameFormat fmtModern = null;

    /**
     * Gets the standard.
     * 
     * @return the standard
     */
    public static GameFormat getStandard() {
        return SetUtils.fmtStandard;
    }

    /**
     * Gets the extended.
     * 
     * @return the extended
     */
    public static GameFormat getExtended() {
        return SetUtils.fmtExtended;
    }

    /**
     * Gets the modern.
     * 
     * @return the modern
     */
    public static GameFormat getModern() {
        return SetUtils.fmtModern;
    }

    // list are immutable, no worries
    /**
     * Gets the formats.
     * 
     * @return the formats
     */
    public static List<GameFormat> getFormats() {
        return SetUtils.formats;
    }

    /**
     * Gets the blocks.
     * 
     * @return the blocks
     */
    public static List<CardBlock> getBlocks() {
        return SetUtils.allBlocks;
    }

    /**
     * Gets the all sets.
     * 
     * @return the all sets
     */
    public static List<CardSet> getAllSets() {
        return SetUtils.allSets;
    }

    // Perform that first of all
    static {
        SetUtils.loadSetData(SetUtils.loadBoosterData());
        SetUtils.loadBlockData();
        SetUtils.loadFormatData();
    }

    /**
     * Gets the sets the by code.
     * 
     * @param code
     *            the code
     * @return the sets the by code
     */
    public static CardSet getSetByCode(final String code) {
        return SetUtils.setsByCode.get(code);
    }

    /**
     * Gets the sets the by code or throw.
     * 
     * @param code
     *            the code
     * @return the sets the by code or throw
     */
    public static CardSet getSetByCodeOrThrow(final String code) {
        final CardSet set = SetUtils.setsByCode.get(code);
        if (null == set) {
            throw new RuntimeException(String.format("Set with code '%s' not found", code));
        }
        return set;
    }

    // used by image generating code
    /**
     * Gets the code2 by code.
     * 
     * @param code
     *            the code
     * @return the code2 by code
     */
    public static String getCode2ByCode(final String code) {
        final CardSet set = SetUtils.setsByCode.get(code);
        return set == null ? "" : set.getCode2();
    }

    private static Map<String, CardSet.BoosterData> loadBoosterData() {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/boosters.txt");
        final Map<String, CardSet.BoosterData> result = new HashMap<String, CardSet.BoosterData>();

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            final String[] sParts = s.trim().split("\\|");
            String code = null;
            int nC = 0, nU = 0, nR = 0, nS = 0, nDF = 0;
            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();

                if (key.equalsIgnoreCase("Set")) {
                    code = kv[1];
                }
                if (key.equalsIgnoreCase("Commons")) {
                    nC = Integer.parseInt(kv[1]);
                }
                if (key.equalsIgnoreCase("Uncommons")) {
                    nU = Integer.parseInt(kv[1]);
                }
                if (key.equalsIgnoreCase("Rares")) {
                    nR = Integer.parseInt(kv[1]);
                }
                if (key.equalsIgnoreCase("Special")) {
                    nS = Integer.parseInt(kv[1]);
                }
                if (key.equalsIgnoreCase("DoubleFaced")) {
                    nDF = Integer.parseInt(kv[1]);
                }
            }
            result.put(code, new CardSet.BoosterData(nC, nU, nR, nS, nDF));
        }
        return result;
    }

    // parser code - quite boring.
    private static void loadSetData(final Map<String, CardSet.BoosterData> boosters) {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/setdata.txt");

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            final String[] sParts = s.trim().split("\\|");
            String code = null, code2 = null, name = null;

            int index = -1;
            String alias = null;
            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("code3".equals(key)) {
                    code = kv[1];
                } else if ("code2".equals(key)) {
                    code2 = kv[1];
                } else if ("name".equals(key)) {
                    name = kv[1];
                } else if ("index".equals(key)) {
                    index = Integer.parseInt(kv[1]);
                } else if ("alias".equals(key)) {
                    alias = kv[1];
                }
            }
            final CardSet set = new CardSet(index, name, code, code2, boosters.get(code));
            boosters.remove(code);
            SetUtils.setsByCode.put(code, set);
            if (alias != null) {
                SetUtils.setsByCode.put(alias, set);
            }
            SetUtils.allSets.add(set);
        }
        assert boosters.isEmpty();
        Collections.sort(SetUtils.allSets);
        SetUtils.allSets = Collections.unmodifiableList(SetUtils.allSets);
    }

    private static void loadBlockData() {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/blocks.txt");

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            final String[] sParts = s.trim().split("\\|");

            String name = null;
            int index = -1;
            final List<CardSet> sets = new ArrayList<CardSet>(4);
            CardSet landSet = null;
            int draftBoosters = 3;
            int sealedBoosters = 6;

            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("index".equals(key)) {
                    index = Integer.parseInt(kv[1]);
                } else if ("set0".equals(key) || "set1".equals(key) || "set2".equals(key)) {
                    sets.add(SetUtils.getSetByCodeOrThrow(kv[1]));
                } else if ("landsetcode".equals(key)) {
                    landSet = SetUtils.getSetByCodeOrThrow(kv[1]);
                } else if ("draftpacks".equals(key)) {
                    draftBoosters = Integer.parseInt(kv[1]);
                } else if ("sealedpacks".equals(key)) {
                    sealedBoosters = Integer.parseInt(kv[1]);
                }

            }
            SetUtils.allBlocks.add(new CardBlock(index, name, sets, landSet, draftBoosters, sealedBoosters));
        }
        Collections.reverse(SetUtils.allBlocks);
        SetUtils.allBlocks = Collections.unmodifiableList(SetUtils.allBlocks);
    }

    private static void loadFormatData() {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/formats.txt");

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            String name = null;
            final List<String> sets = new ArrayList<String>(); // default: all
                                                               // sets
            // allowed
            final List<String> bannedCards = new ArrayList<String>(); // default:
            // nothing
            // banned

            final String[] sParts = s.trim().split("\\|");
            for (final String sPart : sParts) {
                final String[] kv = sPart.split(":", 2);
                final String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("sets".equals(key)) {
                    sets.addAll(Arrays.asList(kv[1].split(", ")));
                } else if ("banned".equals(key)) {
                    bannedCards.addAll(Arrays.asList(kv[1].split("; ")));
                }
            }
            if (name == null) {
                throw new RuntimeException("Format must have a name! Check formats.txt file");
            }
            final GameFormat thisFormat = new GameFormat(name, sets, bannedCards);
            if (name.equalsIgnoreCase("Standard")) {
                SetUtils.fmtStandard = thisFormat;
            }
            if (name.equalsIgnoreCase("Modern")) {
                SetUtils.fmtModern = thisFormat;
            }
            if (name.equalsIgnoreCase("Extended")) {
                SetUtils.fmtExtended = thisFormat;
            }
            SetUtils.formats.add(thisFormat);
        }
        SetUtils.formats = Collections.unmodifiableList(SetUtils.formats);
    }

}
