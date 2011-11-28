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
import forge.Constant;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>
 * QuestPetPlant class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetPlant extends QuestPetAbstract {
    /**
     * QuestPetPlant.
     * 
     * @return Card
     */
    @Override
    public final Card getPetCard() {
        final Card petCard = new Card();

        petCard.setName("Plant Wall");

        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("G");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Plant");
        petCard.addType("Wall");

        petCard.addIntrinsicKeyword("Defender");

        if (this.getLevel() == 1) {
            petCard.setImageName("G 0 1 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 2) {
            petCard.setImageName("G 0 2 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(2);
        } else if (this.getLevel() == 3) {
            petCard.setImageName("G 0 3 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(3);
        } else if (this.getLevel() == 4) {
            petCard.setImageName("G 1 3 Plant Wall");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(3);
            // petCard.addIntrinsicKeyword("First Strike");
        } else if (this.getLevel() == 5) {
            petCard.setImageName("G 1 3 Plant Wall Deathtouch");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(3);
            petCard.addIntrinsicKeyword("Deathtouch");
        } else if (this.getLevel() == 6) {
            petCard.setImageName("G 1 4 Plant Wall");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(4);
            petCard.addIntrinsicKeyword("Deathtouch");

            final Cost abCost = new Cost("T", petCard.getName(), true);
            final SpellAbility ability = new AbilityActivated(petCard, abCost, null) {
                private static final long serialVersionUID = 7546242087593613719L;

                @Override
                public boolean canPlayAI() {
                    return AllZone.getPhase().getPhase().equals(Constant.Phase.MAIN2);
                }

                @Override
                public void resolve() {
                    petCard.getController().gainLife(1, petCard);
                }
            };
            petCard.addSpellAbility(ability);
            ability.setDescription("tap: You gain 1 life.");

            final StringBuilder sb = new StringBuilder();
            sb.append("Plant Wall - ").append(petCard.getController()).append(" gains 1 life.");
            ability.setStackDescription(sb.toString());

            petCard.setText("tap: You gain 1 life.");
        }

        return petCard;
    }

    /**
     * <p>
     * Constructor for QuestPetPlant.
     * </p>
     */
    public QuestPetPlant() {
        super(
                "Plant",
                "Start each of your battles with this lush, verdant plant on your side. Excellent at blocking the nastiest of critters!",
                6);
    }

    /** {@inheritDoc} */
    @Override
    public final int[] getAllUpgradePrices() {
        return new int[] { 100, 150, 200, 300, 750, 1000 };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllUpgradeDescriptions() {
        return new String[] { "Purchase Plant", "Improve the defense power of your plant.",
                "Improve the defense power of your plant.", "Improve the defense power of your plant.",
                "Grow venomous thorns on your plant.",
                "Improve the defense power of your plant and your plant will have healing properties",
                "You cannot train your plant any further" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllStats() {
        return new String[] { "You do not own a plant", "0/1, G, Defender", "0/2, G, Defender", "0/3, G, Defender",
                "1/3, G, Defender", "1/3, G, Defender, Deathtouch", "1/4, G, Defender, Deathtouch, T: Gain 1 life" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllImageNames() {
        return new String[] { "", "g_0_1_plant_wall_small.jpg", "g_0_2_plant_wall_small.jpg",
                "g_0_3_plant_wall_small.jpg", "g_1_3_plant_wall_small.jpg", "g_1_3_plant_wall_deathtouch_small",
                "g_1_4_plant_wall_small.jpg" };
    }

    /** {@inheritDoc} */
    @Override
    public final String getStallName() {
        return QuestStallManager.NURSERY;
    }

    /** {@inheritDoc} */
    @Override
    public final void onPurchase() {
        AllZone.getQuestData().getPetManager().addPlantLevel();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isAvailableForPurchase() {
        final QuestPetPlant plant = (QuestPetPlant) AllZone.getQuestData().getPetManager().getPlant();

        return (plant == null) || (plant.getLevel() < plant.getMaxLevel());
    }
}
