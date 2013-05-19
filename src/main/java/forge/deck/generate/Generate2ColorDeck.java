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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import com.google.common.collect.Lists;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.generate.GenerateDeckUtil.FilterCMC;
import forge.game.player.PlayerType;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;

/**
 * <p>
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate2ColorDeck extends GenerateColoredDeckBase {
    @Override protected final float getLandsPercentage() { return 0.39f; }
    @Override protected final float getCreatPercentage() { return 0.36f; }
    @Override protected final float getSpellPercentage() { return 0.25f; }

    @SuppressWarnings("unchecked")
    final List<ImmutablePair<FilterCMC, Integer>> cmcRelativeWeights = Lists.newArrayList(
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(0, 2), 6),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(3, 4), 4),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(5, 6), 2),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(7, 20), 1)
    );

    // mana curve of the card pool
    // 20x 0 - 2
    // 16x 3 - 4
    // 12x 5 - 6
    // 4x 7 - 20
    // = 52x - card pool (before further random filtering)

    /**
     * <p>
     * Constructor for Generate2ColorDeck.
     * </p>
     * 
     * @param clr1
     *            a {@link java.lang.String} object.
     * @param clr2
     *            a {@link java.lang.String} object.
     */
    public Generate2ColorDeck(final String clr1, final String clr2) {
        int c1 = MagicColor.fromName(clr1);
        int c2 = MagicColor.fromName(clr2);
        
        if( c1 == 0 && c2 == 0) {
            int color1 = r.nextInt(5);
            int color2 = (color1 + 1 + r.nextInt(4)) % 5;
            colors = ColorSet.fromMask(MagicColor.WHITE << color1 | MagicColor.WHITE << color2);
        } else if ( c1 == 0 || c2 == 0 ) {
            byte knownColor = (byte) (c1 | c2);
            int color1 = Arrays.binarySearch(MagicColor.WUBRG, knownColor);
            int color2 = (color1 + 1 + r.nextInt(4)) % 5;
            colors = ColorSet.fromMask(MagicColor.WHITE << color1 | MagicColor.WHITE << color2);
        } else {
            colors = ColorSet.fromMask(c1 | c2);
        }
    }


    public final ItemPoolView<CardPrinted> getDeck(final int size, final PlayerType pt) {
        addCreaturesAndSpells(size, cmcRelativeWeights, pt);

        // Add lands
        int numLands = Math.round(size * getLandsPercentage());
        adjustDeckSize(size - numLands);
        tmpDeck.append(String.format("Adjusted deck size to: %d, should add %d land(s)%n", size - numLands, numLands));

        // Add dual lands
        List<String> duals = GenerateDeckUtil.getDualLandList(colors);
        for (String s : duals) {
            this.cardCounts.put(s, 0);
        }

        int dblsAdded = addSomeStr((numLands / 6), duals);
        numLands -= dblsAdded;

        addBasicLand(numLands);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");
        
        //System.out.println(tmpDeck.toString());
        
        return tDeck;
    }
}
