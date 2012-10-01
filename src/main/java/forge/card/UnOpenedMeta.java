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

import forge.Singletons;
import forge.gui.GuiChoose;
import forge.item.CardPrinted;
import forge.util.MyRandom;

/** 
 * This type extends UnOpenedProduct to support booster choice or random boosters
 * in sealed deck games. See MetaSet.java for further information.
 *
 */
public class UnOpenedMeta extends UnOpenedProduct {

    private final ArrayList<MetaSet> metaSets;
    private final boolean choice;
    private List<String> partiality;
    private final int partialityPreference = 80;
    private final Random generator = MyRandom.getRandom();

    /**
     * Constructor for UnOpenedMeta.
     * @param creationString
     *      String, is parsed for MetaSet info.
     * @param choose
     *        sets the random/choice status.
     */
    public UnOpenedMeta(final String creationString, final boolean choose) {
        // NOTE: we need to call the super constructor with something non-null,
        // but it doesn't matter with what exactly, since we are overriding it
        // in open() anyway. I'm using Portal because that makes it easier to
        // spot if the code is misbehaving in certain ways. --BBU
        super(Singletons.getModel().getBoosters().get("POR"));
        metaSets = new ArrayList<MetaSet>();
        choice = choose;
        final String[] metas = creationString.split(";");
        partiality = null;
        for (int i = 0; i < metas.length; i++) {
            final MetaSet addMeta = new MetaSet(metas[i]);
            metaSets.add(addMeta);
        }
    }



    /**
     * Adds to partiality info.
     * @param addString
     *   String, add partiality for this String.
     */
    private void addPartiality(final String addString) {
        if (!hasPartiality(addString)) {
            partiality.add(addString);
        }
    }

    /**
     * Checks if the AI has a partiality for this set.
     * @param partialString
     *   String, check partiality for this.
     */
    private boolean hasPartiality(final String partialString) {

        if (partiality.isEmpty()) {
            return false;
        }

        for (final String cmp : partiality) {
            if (partialString.equals(cmp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Open the booster pack, return contents.
     * @return List, list of cards.
     */
    @Override
    public List<CardPrinted> open() {
        return this.open(true, null);
    }

    /**
     * Like open, can define whether is human or not.
     * @param isHuman
     *      boolean, is human player?
     * @param partialities
     *      known partialities for the AI.
     * @return List, list of cards.
     */
    @Override
    public List<CardPrinted> open(final boolean isHuman, List<String> partialities) {

        if (metaSets.size() < 1) {
            throw new RuntimeException("Empty UnOpenedMetaset, cannot generate booster.");
        }

        if (choice) {

            if (isHuman) {
                final List<String> choices = new ArrayList<String>();

                for (MetaSet meta : metaSets) {
                    choices.add(meta.getCode());
                }
                final Object o = GuiChoose.one("Choose booster:", choices);

                for (int i = 0; i < metaSets.size(); i++) {
                    if (o.toString().equals(metaSets.get(i).getCode())) {
                        final UnOpenedProduct newBooster = metaSets.get(i).getBooster();
                        return newBooster.open();
                    }
                }

                throw new RuntimeException("Could not find MetaSet " + o.toString());
            }
            else {
                partiality = partialities;
                int selected = -1;

                if (partiality == null || partiality.isEmpty()) {
                    // System.out.println("No partiality yet");
                    selected = generator.nextInt(metaSets.size());
                    // System.out.println("AI randomly chose " + metaSets.get(selected).getCode());
                    if (partiality != null) {
                        addPartiality(metaSets.get(selected).getCode());
                    }
                 }
                else {
                    for (int i = 0; i < metaSets.size(); i++) {
                        if (hasPartiality(metaSets.get(i).getCode()) && MyRandom.percentTrue(partialityPreference)) {
                            // System.out.println("AI chose " + metaSets.get(i).getCode() + " because of partiality.");
                            selected = i;
                            break;
                        }
                    }
                }

                if (selected == -1) {
                selected = generator.nextInt(metaSets.size());
                if (partiality != null) {
                    addPartiality(metaSets.get(selected).getCode());
                    }
                // System.out.println("AI chose " + metaSets.get(selected).getCode() + " because partiality not established or failed percentage test.");
                }
                final UnOpenedProduct newBooster = metaSets.get(selected).getBooster();
                return newBooster.open();
            }
        }
        else {
            int selected = generator.nextInt(metaSets.size());
            // System.out.println("RANDOMLY got " + metaSets.get(selected).getCode());

            // It may actually seem slightly unfair to allow the computer change its partialities based
            // on the random sets it gets since the player can't do the same...but, OTOH, this could also
            // work against the computer, if this results in a bad partiality choice. --BBU
            if (!isHuman && partiality != null && MyRandom.percentTrue(partialityPreference)) {
                addPartiality(metaSets.get(selected).getCode());
                // System.out.println("AI decided to add " + metaSets.get(selected).getCode() + " to partialities.");
            }
            final UnOpenedProduct newBooster = metaSets.get(selected).getBooster();
            return newBooster.open();
        }
    }
}
