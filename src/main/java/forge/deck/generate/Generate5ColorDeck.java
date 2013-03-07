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
import forge.card.ColorSet;
import forge.deck.generate.GenerateDeckUtil.FilterCMC;
import forge.error.BugReporter;
import forge.game.player.PlayerType;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.properties.ForgeProps;

/**
 * <p>
 * Generate5ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate5ColorDeck extends GenerateColoredDeckBase {
    final List<FilterCMC> cmcLevels = Arrays.asList(
            new GenerateDeckUtil.FilterCMC(0, 2),
            new GenerateDeckUtil.FilterCMC(3, 5),
            new GenerateDeckUtil.FilterCMC(6, 20));
    final int[] cmcAmounts = {15, 10, 5};

    // resulting mana curve of the card pool
    // 30x 0 - 2
    // 20x 3 - 5
    // 10x 6 - 20
    // =60x - card pool

    /**
     * Instantiates a new generate5 color deck.
     */
    public Generate5ColorDeck() {
        colors = ColorSet.fromMask(0).inverse();
    }

    /**
     * <p>
     * get3ColorDeck.
     * </p>
     * 
     * @param deckSize
     *            a int.
     * @param playerType
     *            a PlayerType
     * @return a {@link forge.CardList} object.
     */
    public final ItemPoolView<CardPrinted> getDeck(final int size, final PlayerType pt) {
        addCreaturesAndSpells(size, cmcLevels, cmcAmounts, pt);

        // Add lands
        int numLands = (int) (getLandsPercentage() * size);

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

        adjustDeckSize(size);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");
        if (ForgeProps.getProperty("showdeck/5color", "false").equals("true")) {
            BugReporter.reportBug(tmpDeck.toString());
        }

        return tDeck;
    }
}
