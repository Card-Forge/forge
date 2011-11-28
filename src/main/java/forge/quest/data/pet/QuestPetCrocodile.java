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
package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;

/**
 * <p>
 * QuestPetCrocodile class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetCrocodile extends QuestPetAbstract {
    /** {@inheritDoc} */
    @Override
    public final Card getPetCard() {
        final Card petCard = new Card();
        petCard.setName("Crocodile Pet");
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("B");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Crocodile");
        petCard.addType("Pet");

        if (this.getLevel() == 1) {
            petCard.setImageName("B 1 1 Crocodile Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 2) {
            petCard.setImageName("B 2 1 Crocodile Pet");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 3) {
            petCard.setImageName("B 3 1 Crocodile Pet");
            petCard.setBaseAttack(3);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 4) {
            petCard.setImageName("B 3 1 Crocodile Pet Swampwalk");
            petCard.setBaseAttack(3);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("Swampwalk");
        }

        return petCard;
    }

    /**
     * <p>
     * Constructor for QuestPetCrocodile.
     * </p>
     */
    public QuestPetCrocodile() {
        super("Crocodile", "With its razor sharp teeth, this swamp-dwelling monster is extremely dangerous.", 4);
    }

    /** {@inheritDoc} */
    @Override
    public final int[] getAllUpgradePrices() {
        return new int[] { 250, 300, 450, 600 };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllUpgradeDescriptions() {
        return new String[] { "Purchase Crocodile", "Improve the attack power of your crocodile.",
                "Improve the attack power of your crocodile.", "Give Swampwalking to your crocodile.",
                "You cannot train your crocodile any further" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllStats() {
        return new String[] { "You do not own a crocodile", "1/1, B", "2/1, B", "3/1, B", "3/1, B, Swampwalking" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllImageNames() {
        return new String[] { "", "b_1_1_crocodile_pet_small.jpg", "b_2_1_crocodile_pet_small.jpg",
                "b_3_1_crocodile_pet_small.jpg", "b_3_1_crocodile_pet_swampwalk_small.jpg" };
    }
}
