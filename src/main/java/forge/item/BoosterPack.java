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
package forge.item;

import java.util.Arrays;
import java.util.List;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.lambda.Lambda1;
import forge.Singletons;
import forge.card.BoosterData;
import forge.card.BoosterGenerator;
import forge.card.CardRules;
import forge.card.CardEdition;
import forge.util.Predicate;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class BoosterPack extends OpenablePack {

    /** The Constant fnFromSet. */
    public static final Lambda1<BoosterPack, CardEdition> FN_FROM_SET = new Lambda1<BoosterPack, CardEdition>() {
        @Override
        public BoosterPack apply(final CardEdition arg1) {
            BoosterData d = Singletons.getModel().getBoosters().get(arg1.getCode());
            return new BoosterPack(arg1.getName(), d);
        }
    };

    /**
     * Instantiates a new booster pack.
     * 
     * @param set
     *            the set
     */
    public BoosterPack(final String name0, final BoosterData boosterData) {
        super(name0, boosterData);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    /**
     * Gets the image filename.
     * 
     * @return String
     */
    @Override
    public final String getImageFilename() {
        return "booster/" + this.contents.getEdition() + ".png";
    }

    private CardPrinted getRandomBasicLand(final CardEdition set) {
        return Predicate.and(CardPrinted.Predicates.printedInSets(set.getCode()),
                CardRules.Predicates.Presets.IS_BASIC_LAND, CardPrinted.FN_GET_RULES).random(
                CardDb.instance().getAllCards());
    }

    private CardPrinted getLandFromNearestSet() {
        final CardEdition[] editions = UtilFunctions.iteratorToArray(Singletons.getModel().getEditions().iterator(), new CardEdition[]{});
        final int iThisSet = Arrays.binarySearch(editions, this.contents);
        for (int iSet = iThisSet; iSet < editions.length; iSet++) {
            final CardPrinted land = this.getRandomBasicLand(editions[iSet]);
            if (null != land) {
                return land;
            }
        }
        // if not found (though that's impossible)
        return this.getRandomBasicLand(Singletons.getModel().getEditions().get("M12"));
    }

    protected List<CardPrinted> generate() {
        final BoosterGenerator gen = new BoosterGenerator(this.contents.getEditionFilter());
        List<CardPrinted> myCards = gen.getBoosterPack(this.contents);

        final int cntLands = this.contents.getLand();
        if (cntLands > 0) {
            myCards.add(this.getLandFromNearestSet());
        }
        return myCards;
    }


 

    @Override
    public final String getType() {
        return "Booster Pack";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    /**
     * Clone.
     * 
     * @return Object
     */
    @Override
    public final Object clone() {
        return new BoosterPack(name, contents); 
    }


}
