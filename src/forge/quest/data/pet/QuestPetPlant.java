package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Cost;
import forge.card.spellability.SpellAbility;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>QuestPetPlant class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestPetPlant extends QuestPetAbstract {
    /** {@inheritDoc} */
    @Override
    public Card getPetCard() {
        final Card petCard = new Card();

        petCard.setName("Plant Wall");

        petCard.setController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("G");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Plant");
        petCard.addType("Wall");

        petCard.addIntrinsicKeyword("Defender");

        if (level == 1) {
            petCard.setImageName("G 0 1 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(1);
        } else if (level == 2) {
            petCard.setImageName("G 0 2 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(2);
        } else if (level == 3) {
            petCard.setImageName("G 0 3 Plant Wall");
            petCard.setBaseAttack(0);
            petCard.setBaseDefense(3);
        } else if (level == 4) {
            petCard.setImageName("G 1 3 Plant Wall");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(3);
            // petCard.addIntrinsicKeyword("First Strike");
        } else if (level == 5) {
            petCard.setImageName("G 1 3 Plant Wall Deathtouch");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(3);
            petCard.addIntrinsicKeyword("Deathtouch");
        } else if (level == 6) {
            petCard.setImageName("G 1 4 Plant Wall");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(4);
            petCard.addIntrinsicKeyword("Deathtouch");


            Cost abCost = new Cost("T", petCard.getName(), true);
            final SpellAbility ability = new Ability_Activated(petCard, abCost, null) {
                private static final long serialVersionUID = 7546242087593613719L;

                @Override
                public boolean canPlayAI() {
                    return AllZone.getPhase().getPhase().equals(Constant.Phase.Main2);
                }

                @Override
                public void resolve() {
                    petCard.getController().gainLife(1, petCard);
                }
            };
            petCard.addSpellAbility(ability);
            ability.setDescription("tap: You gain 1 life.");

            StringBuilder sb = new StringBuilder();
            sb.append("Plant Wall - ").append(petCard.getController()).append(" gains 1 life.");
            ability.setStackDescription(sb.toString());

            petCard.setText("tap: You gain 1 life.");
        }


        return petCard;
    }

    /**
     * <p>Constructor for QuestPetPlant.</p>
     */
    public QuestPetPlant() {
        super("Plant",
                "Start each of your battles with this lush, verdant plant on your side. Excellent at blocking the nastiest of critters!",
                6);
    }

    /** {@inheritDoc} */
    @Override
    public int[] getAllUpgradePrices() {
        return new int[]{100, 150, 200, 300, 750, 1000};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllUpgradeDescriptions() {
        return new String[]{
                "Purchase Plant",
                "Improve the defense power of your plant.",
                "Improve the defense power of your plant.",
                "Improve the defense power of your plant.",
                "Grow venomous thorns on your plant.",
                "Improve the defense power of your plant and your plant will have healing properties",
                "You cannot train your plant any further"};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllStats() {
        return new String[]{"You do not own a plant",
                "0/1, G, Defender",
                "0/2, G, Defender",
                "0/3, G, Defender",
                "1/3, G, Defender",
                "1/3, G, Defender, Deathtouch",
                "1/4, G, Defender, Deathtouch, T: Gain 1 life"};
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllImageNames() {
        return new String[]{
                "",
                "g_0_1_plant_wall_small.jpg",
                "g_0_2_plant_wall_small.jpg",
                "g_0_3_plant_wall_small.jpg",
                "g_1_3_plant_wall_small.jpg",
                "g_1_3_plant_wall_deathtouch_small",
                "g_1_4_plant_wall_small.jpg"
        };
    }

    /** {@inheritDoc} */
    @Override
    public String getStallName() {
        return QuestStallManager.NURSERY;
    }

    /** {@inheritDoc} */
    @Override
    public void onPurchase() {
        AllZone.getQuestData().getPetManager().addPlantLevel();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailableForPurchase() {
        QuestPetPlant plant = (QuestPetPlant) AllZone.getQuestData().getPetManager().getPlant();

        return plant == null || plant.getLevel() < plant.getMaxLevel();
    }
}
