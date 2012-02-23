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
package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import forge.util.FileUtil;

/**
 * <p>
 * SetInfoUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class SetUtils {

    private final List<CardEdition> allSets;

    public final List<CardEdition> getAllSets() {
        return allSets;
    }

    public SetUtils() {
        allSets = loadSetData(loadBoosterData());
        allBlocks = loadBlockData();
    }

    /** Constant <code>setData</code>. */
    private final Map<String, CardEdition> setsByCode = new TreeMap<String, CardEdition>(String.CASE_INSENSITIVE_ORDER);
    private final List<CardBlock> allBlocks;

    /**
     * Gets the blocks.
     * 
     * @return the blocks
     */
    public List<CardBlock> getBlocks() {
        return allBlocks;
    }

    /**
     * Gets the sets the by code.
     * 
     * @param code
     *            the code
     * @return the sets the by code
     */
    public CardEdition getEditionByCode(final String code) {
        return setsByCode.get(code);
    }

    /**
     * Gets the sets the by code or throw.
     * 
     * @param code
     *            the code
     * @return the sets the by code or throw
     */
    public CardEdition getEditionByCodeOrThrow(final String code) {
        final CardEdition set = setsByCode.get(code);
        if (null == set) {
            throw new RuntimeException(String.format("Edition with code '%s' not found", code));
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
    public String getCode2ByCode(final String code) {
        final CardEdition set = setsByCode.get(code);
        return set == null ? "" : set.getCode2();
    }

    private Map<String, CardEdition.BoosterData> loadBoosterData() {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/boosters.txt");
        final Map<String, CardEdition.BoosterData> result = new HashMap<String, CardEdition.BoosterData>();

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
            result.put(code, new CardEdition.BoosterData(nC, nU, nR, nS, nDF));
        }
        return result;
    }

    // parser code - quite boring.
    private List<CardEdition> loadSetData(final Map<String, CardEdition.BoosterData> boosters) {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/setdata.txt");

        final List<CardEdition> allSets = new ArrayList<CardEdition>();
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
            final CardEdition set = new CardEdition(index, name, code, code2, boosters.get(code));
            boosters.remove(code);
            setsByCode.put(code, set);
            if (alias != null) {
                setsByCode.put(alias, set);
            }
            allSets.add(set);
        }
        assert boosters.isEmpty();
        return allSets;
    }

    private List<CardBlock> loadBlockData() {
        final ArrayList<String> fData = FileUtil.readFile("res/blockdata/blocks.txt");
        final List<CardBlock> theBlocks = new ArrayList<CardBlock>();

        for (final String s : fData) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            final String[] sParts = s.trim().split("\\|");

            String name = null;
            int index = -1;
            final List<CardEdition> sets = new ArrayList<CardEdition>(4);
            CardEdition landSet = null;
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
                    sets.add(getEditionByCodeOrThrow(kv[1]));
                } else if ("landsetcode".equals(key)) {
                    landSet = getEditionByCodeOrThrow(kv[1]);
                } else if ("draftpacks".equals(key)) {
                    draftBoosters = Integer.parseInt(kv[1]);
                } else if ("sealedpacks".equals(key)) {
                    sealedBoosters = Integer.parseInt(kv[1]);
                }

            }
            theBlocks.add(new CardBlock(index, name, sets, landSet, draftBoosters, sealedBoosters));
        }
        Collections.reverse(theBlocks);
        return Collections.unmodifiableList(theBlocks);
    }

}
