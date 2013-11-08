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
import java.util.List;
import java.util.Random;

import forge.gui.GuiChoose;
import forge.item.PaperCard;
import forge.util.MyRandom;
import forge.util.TextUtil;

/** 
 * This type extends UnOpenedProduct to support booster choice or random boosters
 * in sealed deck games. See MetaSet.java for further information.
 */

public class UnOpenedMeta implements IUnOpenedProduct {

    private enum JoinOperation {
        RandomOne,
        ChooseOne,
        SelectAll,
    }
    
    private final ArrayList<MetaSet> metaSets;
    private final JoinOperation operation;
    private final Random generator = MyRandom.getRandom();

    /**
     * Constructor for UnOpenedMeta.
     * @param creationString
     *      String, is parsed for MetaSet info.
     * @param choose
     *        sets the random/choice status.
     */
    private UnOpenedMeta(final String creationString, final JoinOperation op) {
        metaSets = new ArrayList<MetaSet>();
        operation = op;

        for(String m : TextUtil.splitWithParenthesis(creationString, ';')) {
            metaSets.add(new MetaSet(m, true));
        }
    }


    /**
     * Open the booster pack, return contents.
     * @return List, list of cards.
     */
    @Override
    public List<PaperCard> get() {
        return this.open(true);
    }

    /**
     * Like open, can define whether is human or not.
     * @param isHuman
     *      boolean, is human player?
     * @param partialities
     *      known partialities for the AI.
     * @return List, list of cards.
     */
    public List<PaperCard> open(final boolean isHuman) {

        if (metaSets.isEmpty()) {
            throw new RuntimeException("Empty UnOpenedMetaset, cannot generate booster.");
        }

        switch(operation) {
            case ChooseOne:
                if (isHuman) {
                    final MetaSet ms = GuiChoose.one("Choose booster:", metaSets);
                    return ms.getBooster().get();
                }
                
            case RandomOne: // AI should fall though here from the case above
                int selected = generator.nextInt(metaSets.size());
                final IUnOpenedProduct newBooster = metaSets.get(selected).getBooster();
                return newBooster.get();
            
            case SelectAll:
                List<PaperCard> allCards = new ArrayList<PaperCard>();
                for (MetaSet ms : metaSets) {
                    allCards.addAll(ms.getBooster().get());
                }
                return allCards;
        }
        throw new IllegalStateException("Got wrong operation type in unopenedMeta - execution should never reach this point");
    }
    
    public static UnOpenedMeta choose(String desc) {
        return new UnOpenedMeta(desc, JoinOperation.ChooseOne);
    }
    public static UnOpenedMeta random(String desc) {
        return new UnOpenedMeta(desc, JoinOperation.RandomOne);
    }
    public static UnOpenedMeta selectAll(String desc) {
        return new UnOpenedMeta(desc, JoinOperation.SelectAll);
    }
    
}
