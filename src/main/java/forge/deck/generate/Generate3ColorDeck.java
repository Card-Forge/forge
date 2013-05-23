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
package forge.deck.generate;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Lists;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.generate.GenerateDeckUtil.FilterCMC;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.MyRandom;

/**
 * <p>
 * Generate3ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate3ColorDeck extends GenerateColoredDeckBase {
    @SuppressWarnings("unchecked")
    final List<ImmutablePair<FilterCMC, Integer>> cmcLevels = Lists.newArrayList(
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(0, 2), 12),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(3, 5), 9),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(6, 20), 3)
    );

    /**
     * <p>
     * Constructor for Generate3ColorDeck.
     * </p>
     * 
     * @param clr1
     *            a {@link java.lang.String} object.
     * @param clr2
     *            a {@link java.lang.String} object.
     * @param clr3
     *            a {@link java.lang.String} object.
     */
    public Generate3ColorDeck(final String clr1, final String clr2, final String clr3) {
        int c1 = MagicColor.fromName(clr1);
        int c2 = MagicColor.fromName(clr2);
        int c3 = MagicColor.fromName(clr3);
        
        int rc = 0;
        int combo = c1 | c2 | c3;

        ColorSet param = ColorSet.fromMask(combo);
        switch(param.countColors()) {
            case 3:
                colors = param;
                return;

            case 0:
                int color1 = r.nextInt(5);
                int color2 = (color1 + 1 + r.nextInt(4)) % 5;
                colors = ColorSet.fromMask(MagicColor.WHITE << color1 | MagicColor.WHITE << color2).inverse();
                return;

            case 1:
                do {
                    rc = MagicColor.WHITE << MyRandom.getRandom().nextInt(5); 
                } while ( rc == combo );
                combo |= rc;
                // fall-through

            case 2:
                do {
                    rc = MagicColor.WHITE << MyRandom.getRandom().nextInt(5); 
                } while ( (rc & combo) != 0 );
                combo |= rc;
                break;
        }
        colors = ColorSet.fromMask(combo);
    }

    @Override
    public final ItemPoolView<CardPrinted> getDeck(final int size, final boolean forAi) {
        addCreaturesAndSpells(size, cmcLevels, forAi);

        // Add lands
        int numLands = Math.round(size * getLandsPercentage());
        adjustDeckSize(size - numLands);
        tmpDeck.append("numLands:").append(numLands).append("\n");

        // Add dual lands
        List<String> duals = GenerateDeckUtil.getDualLandList(colors);
        for (String s : duals) {
            this.cardCounts.put(s, 0);
        }

        int dblsAdded = addSomeStr((numLands / 4), duals);
        numLands -= dblsAdded;

        addBasicLand(numLands);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");
        return tDeck;
    }
}
