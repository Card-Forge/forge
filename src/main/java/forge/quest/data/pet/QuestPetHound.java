package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * <p>
 * QuestPetHound class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetHound extends QuestPetAbstract {
    /**
     * <p>
     * Constructor for QuestPetHound.
     * </p>
     */
    public QuestPetHound() {
        super("Hound", "Dogs are said to be man's best friend. Definitely not this one.", 4);
    }

    /** {@inheritDoc} */
    @Override
    public final Card getPetCard() {
        Card petCard = new Card();

        petCard.setName("Hound Pet");
        petCard.addController(AllZone.getHumanPlayer());
        petCard.setOwner(AllZone.getHumanPlayer());

        petCard.addColor("R");
        petCard.setToken(true);

        petCard.addType("Creature");
        petCard.addType("Hound");
        petCard.addType("Pet");

        if (level == 1) {
            petCard.setImageName("R 1 1 Hound Pet");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
        } else if (level == 2) {
            petCard.setImageName("R 1 1 Hound Pet Haste");
            petCard.setBaseAttack(1);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("Haste");
        } else if (level == 3) {
            petCard.setImageName("R 2 1 Hound Pet");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("Haste");

        } else if (level == 4) {
            petCard.setImageName("R 2 1 Hound Pet Alone");
            petCard.setBaseAttack(2);
            petCard.setBaseDefense(1);
            petCard.addIntrinsicKeyword("Haste");

            final Trigger myTrigger = TriggerHandler
                    .parseTrigger(
                            "Mode$ Attacks | ValidCard$ Card.Self | Alone$ True | TriggerDescription$ Whenever CARDNAME attacks alone, it gets +2/+0 until end of turn.",
                            petCard, true);
            AbilityFactory af = new AbilityFactory();
            myTrigger.setOverridingAbility(af.getAbility("AB$Pump | Cost$ 0 | Defined$ Self | NumAtt$ 2", petCard));
            petCard.addTrigger(myTrigger);
        }

        return petCard;
    }

    /** {@inheritDoc} */
    @Override
    public final int[] getAllUpgradePrices() {
        return new int[] { 200, 350, 450, 750 };
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllUpgradeDescriptions() {
        return new String[] {"Purchase hound", "Give Haste to your hound.", "Improve the attack power of your hound.",
                "Greatly improves your hound's attack power if it attacks alone.",
                "You cannot train your hound any further"};
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllStats() {
        return new String[] {"You do not own a hound", "1/1, R", "1/1, R, Haste", "2/1, R, Haste",
                "2/1, R, Haste, Whenever this creature attacks alone, it gets +2/+0 until end of turn."};
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAllImageNames() {
        return new String[] {"", "r_1_1_hound_pet_small.jpg", "r_1_1_hound_pet_haste_small.jpg",
                "r_2_1_hound_pet_small.jpg", "r_2_1_hound_pet_alone_small.jpg"};
    }
}
