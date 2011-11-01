package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;

/**
 * <p>
 * QuestPetWolf class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetWolf extends QuestPetAbstract {
    /** {@inheritDoc} */
    @Override
    public final Card getPetCard() {
        final Card petCard = new Card();

        petCard.setName("Wolf Pet");
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("G");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Wolf");
        petCard.addType("Pet");

        if (this.getLevel() == 1) {
            petCard.setImageName("G 1 1 Wolf Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
        } else if (this.getLevel() == 2) {
            petCard.setImageName("G 1 2 Wolf Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(2);
        } else if (this.getLevel() == 3) {
            petCard.setImageName("G 2 2 Wolf Pet");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(2);
        } else if (this.getLevel() == 4) {
            petCard.setImageName("G 2 2 Wolf Pet Flanking");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(2);
            petCard.addIntrinsicKeyword("Flanking");
        }

        return petCard;
    }

    /**
     * <p>
     * Constructor for QuestPetWolf.
     * </p>
     */
    public QuestPetWolf() {
        super("Wolf", "This ferocious animal may have been raised in captivity, but it has been trained to kill.", 4);
    }

    /** {@inheritDoc} */
    @Override
    public final int[] getAllUpgradePrices() {
        return new int[] { 250, 250, 500, 550 };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllUpgradeDescriptions() {
        return new String[] { "Purchase Wolf", "Improve the attack power of your wolf.",
                "Improve the defense power of your wolf.", "Give Flanking to your wolf.",
                "You cannot train your wolf any further" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllStats() {
        return new String[] { "You do not own a wolf", "1/1, G", "1/2, G", "2/2, G", "2/2, G, Flanking" };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllImageNames() {
        return new String[] { "", "g_1_1_wolf_pet_small.jpg", "g_1_2_wolf_pet_small.jpg", "g_2_2_wolf_pet_small.jpg",
                "g_2_2_wolf_pet_flanking_small.jpg" };
    }
}
