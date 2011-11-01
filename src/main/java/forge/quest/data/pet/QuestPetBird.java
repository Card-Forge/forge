package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;

/**
 * <p>
 * QuestPetBird class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetBird extends QuestPetAbstract {
    /** {@inheritDoc} */
    @Override
    public final Card getPetCard() {
        final Card petCard = new Card();

        petCard.setName("Bird Pet");
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("W");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Bird");
        petCard.addType("Pet");

        petCard.addIntrinsicKeyword("Flying");

        if (this.getLevel() == 1) {
            petCard.setImageName("W 0 1 Bird Pet");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 2) {
            petCard.setImageName("W 1 1 Bird Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 3) {
            petCard.setImageName("W 2 1 Bird Pet");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 4) {
            petCard.setImageName("W 2 1 Bird Pet First Strike");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("First Strike");
        }

        return petCard;
    }

    /**
     * <p>
     * Constructor for QuestPetBird.
     * </p>
     */
    public QuestPetBird() {
        super("Bird", "Unmatched in speed, agility and awareness, this trained hawk makes a fantastic hunter.", 4);
    }

    /** {@inheritDoc} */
    @Override
    public final int[] getAllUpgradePrices() {
        return new int[] { 200, 300, 450, 400 };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllUpgradeDescriptions() {
        return new String[] { "Purchase Bird", "Improve the attack power of your bird.",
                "Improve the attack power of your bird.", "Give First Strike to your bird.",
                "You cannot train your bird any further" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllStats() {
        return new String[] { "You do not own a bird", "0/1, W, Flying", "1/1, W, Flying", "2/1, W, Flying",
                "2/1, W, Flying, First Strike" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllImageNames() {
        return new String[] { "", "w_0_1_bird_pet_small.jpg", "w_1_1_bird_pet_small.jpg", "w_2_1_bird_pet_small.jpg",
                "w_2_1_bird_pet_first_strike_small.jpg" };
    }
}
