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
import forge.item.CardPrinted;
import forge.util.MyRandom;

/** 
 * This type extends UnOpenedProduct to support booster choice or random boosters
 * in sealed deck games. See MetaSet.java for further information.
 */

public class UnOpenedMeta implements IUnOpenedProduct {

    private final ArrayList<MetaSet> metaSets;
    private final boolean canChoose;
    private final Random generator = MyRandom.getRandom();

    /**
     * Constructor for UnOpenedMeta.
     * @param creationString
     *      String, is parsed for MetaSet info.
     * @param choose
     *        sets the random/choice status.
     */
    public UnOpenedMeta(final String creationString, final boolean choose) {
        metaSets = new ArrayList<MetaSet>();
        canChoose = choose;

        
        for(String m : creationString.split(";")) {
            metaSets.add(new MetaSet(m, true));
        }
    }


    /**
     * Open the booster pack, return contents.
     * @return List, list of cards.
     */
    @Override
    public List<CardPrinted> get() {
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

    public List<CardPrinted> open(final boolean isHuman) {

        if (metaSets.size() < 1) {
            throw new RuntimeException("Empty UnOpenedMetaset, cannot generate booster.");
        }

        if (canChoose) {
            if (isHuman) {
                final List<String> choices = new ArrayList<String>();

                for (MetaSet meta : metaSets) {
                    choices.add(meta.getCode());
                }
                final Object o = GuiChoose.one("Choose booster:", choices);

                for (int i = 0; i < metaSets.size(); i++) {
                    if (o.toString().equals(metaSets.get(i).getCode())) {
                        final IUnOpenedProduct newBooster = metaSets.get(i).getBooster();
                        return newBooster.get();
                    }
                }

                throw new RuntimeException("Could not find MetaSet " + o.toString());
            }
            else {
                int selected = generator.nextInt(metaSets.size());
                final IUnOpenedProduct newBooster = metaSets.get(selected).getBooster();
                return newBooster.get();
            }
        }
        else {
            int selected = generator.nextInt(metaSets.size());
            final IUnOpenedProduct newBooster = metaSets.get(selected).getBooster();
            return newBooster.get();
        }
    }
}
