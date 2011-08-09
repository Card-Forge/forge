package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;

/**
 * <p>QuestPetCrocodile class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestPetCrocodile extends QuestPetAbstract {
    /** {@inheritDoc} */
    @Override
    public Card getPetCard() {
        Card petCard = new Card();
        petCard.setName("Crocodile Pet");
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("B");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Crocodile");
        petCard.addType("Pet");

        if (level == 1) {
            petCard.setImageName("B 1 1 Crocodile Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
        } else if (level == 2) {
            petCard.setImageName("B 2 1 Crocodile Pet");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
        } else if (level == 3) {
            petCard.setImageName("B 3 1 Crocodile Pet");
            petCard.setBaseAttack(3);
            petCard.setBaseDefense(1);
        } else if (level == 4) {
            petCard.setImageName("B 3 1 Crocodile Pet Swampwalk");
            petCard.setBaseAttack(3);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("Swampwalk");
        }


        return petCard;
    }

    /**
     * <p>Constructor for QuestPetCrocodile.</p>
     */
    public QuestPetCrocodile() {
        super("Crocodile",
                "With its razor sharp teeth, this swamp-dwelling monster is extremely dangerous.",
                4);
    }

    /** {@inheritDoc} */
    @Override
    public int[] getAllUpgradePrices() {
        return new int[]{250, 300, 450, 600};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllUpgradeDescriptions() {
        return new String[]{
                "Purchase Crocodile",
                "Improve the attack power of your crocodile.",
                "Improve the attack power of your crocodile.",
                "Give Swampwalking to your crocodile.",
                "You cannot train your crocodile any further"};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllStats() {
        return new String[]{"You do not own a crocodile",
                "1/1, B",
                "2/1, B",
                "3/1, B",
                "3/1, B, Swampwalking"};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllImageNames() {
        return new String[]{
                "",
                "b_1_1_crocodile_pet_small.jpg",
                "b_2_1_crocodile_pet_small.jpg",
                "b_3_1_crocodile_pet_small.jpg",
                "b_3_1_crocodile_pet_swampwalk_small.jpg"
        };
    }
}
