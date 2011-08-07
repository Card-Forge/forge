package forge.quest.data;

import forge.*;

import java.util.ArrayList;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

/**
 * <p>QuestUtil class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestUtil {

    /**
     * <p>getComputerCreatures.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getComputerCreatures(QuestData qd) {
        return new CardList();
    }

    /**
     * <p>getComputerCreatures.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qa a {@link forge.Quest_Assignment} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getComputerCreatures(QuestData qd, Quest_Assignment qa) {
        CardList list = new CardList();
        if (qa != null) {
            ArrayList<String> compCards = qa.getCompy();

            for (String s : compCards) {
                Card c = AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                list.add(c);
            }
        }
        return list;
    }

    /**
     * <p>getHumanPlantAndPet.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanPlantAndPet(QuestData qd) {
        CardList list = new CardList();

        if (qd.getPetManager().shouldPetBeUsed()) {
            list.add(qd.getPetManager().getSelectedPet().getPetCard());
        }

        if (qd.getPetManager().shouldPlantBeUsed()) {
            list.add(qd.getPetManager().getPlant().getPetCard());
        }

        return list;
    }

    /**
     * <p>getHumanPlantAndPet.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qa a {@link forge.Quest_Assignment} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanPlantAndPet(QuestData qd, Quest_Assignment qa) {
        CardList list = getHumanPlantAndPet(qd);

        if (qa != null)
            list.addAll(qa.getHuman());

        return list;
    }


    /**
     * <p>setupQuest.</p>
     *
     * @param qa a {@link forge.Quest_Assignment} object.
     */
    public static void setupQuest(Quest_Assignment qa) {
        QuestBoosterPack pack = new QuestBoosterPack();
        qa.clearCompy();

        int id = qa.getId();

        Generator<Card> cards = YieldUtils.toGenerator(AllZone.getCardFactory());

        if (id == 1) //White Dungeon
        {
            qa.addCompy("Divine Presence");

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.White));
        } else if (id == 2) //Blue Dungeon
        {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Quest for Ancient Secrets", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);
            
            qa.addCompy("Forced Fruition");

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Blue));
        } else if (id == 3) //Black Dungeon
        {
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Black));
        } else if (id == 4) //Red Dungeon
        {
            for (int i = 0; i < 3; i++)
                qa.addCompy("Mons's Goblin Raiders");

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Red));
        } else if (id == 5) //Green Dungeon
        {
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Green));
        } else if (id == 6) //Colorless Dungeon
        {
            for (int i = 0; i < 3; i++)
                qa.addCompy("Eon Hub");
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Colorless));
        } else if (id == 7) //Gold Dungeon
        {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Trailblazer's Boots", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, "Multicolor"));
        } else if (id == 8) {
            CardList humanList = new CardList();
            for (int i = 0; i < 3; i++) {
                //CANNOT use makeToken because of WheneverKeyword
                Card c = new Card();
                c.setName("Sheep");
                c.setImageName("G 0 1 Sheep");

                c.setController(AllZone.getHumanPlayer());
                c.setOwner(AllZone.getHumanPlayer());

                //c.setManaCost("G");
                c.addColor("G");
                c.setToken(true);

                c.addType("Creature");
                c.addType("Sheep");

                c.setBaseAttack(0);
                c.setBaseDefense(1);

                humanList.add(c);
            }
            qa.setHuman(humanList);
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, null));
        } else if (id == 9) {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Trusty Machete", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);

            for (int i = 0; i < 3; i++)
                qa.addCompy("Wall of Wood");

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, Constant.Color.Green));
        } else if (id == 10) {
            CardList humanList = new CardList();

            Card crd = AllZone.getCardFactory().getCard("Wall of Spears", AllZone.getHumanPlayer());

            crd.setCurSetCode(crd.getMostRecentSet());
            crd.setImageFilename(CardUtil.buildFilename(crd));

            humanList.add(crd);

            for (int i = 0; i < 3; i++) {
                Card c = new Card();
                c.setName("Citizen");
                c.setImageName("W 1 1 Citizen");

                c.setController(AllZone.getHumanPlayer());
                c.setOwner(AllZone.getHumanPlayer());

                c.setManaCost("W");
                c.addColor("W");
                c.setToken(true);

                c.addType("Creature");
                c.addType("Citizen");

                c.setBaseAttack(1);
                c.setBaseDefense(1);

                humanList.add(c);
            }

            qa.setHuman(humanList);

            for (int i = 0; i < 3; i++)
                qa.addCompy("Scathe Zombies");
            qa.addCompy("Mass of Ghouls");

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, Constant.Color.Black));
        } else if (id == 11)  // The King's Contest
        {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Seal of Cleansing", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);

            qa.addCompy("Loyal Retainers");

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, null));
        } else if (id == 12)  // Barroom Brawl
        {
            CardList humanList = new CardList();
            for (int i = 0; i < 3; i++) {
                Card c = new Card();
                c.setName("Soldier Ally");
                c.setImageName("W 1 1 Soldier Ally");

                c.setController(AllZone.getHumanPlayer());
                c.setOwner(AllZone.getHumanPlayer());

                c.setManaCost("W");
                c.addColor("W");
                c.setToken(true);

                c.addType("Creature");
                c.addType("Soldier");
                c.addType("Ally");

                c.setBaseAttack(1);
                c.setBaseDefense(1);


                humanList.add(c);
            }
            qa.setHuman(humanList);


            qa.addCompy("Lowland Giant");

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, null));
        } else if (id == 13)  // The Court Jester
        {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Sensei's Divining Top", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);

            qa.addCompy("Teferi's Puzzle Box");

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, "Multicolor"));
        } else if (id == 14)  // Ancient Battlefield
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Glasses of Urza", "Blight Sickle"};

            for (int i = 0; i < 2; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Bad Moon", "Wall of Brambles"};

            for (int i = 0; i < 2; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, null));
        } else if (id == 15)  // Don't Play With Matches
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Mudbutton Torchrunner", "Scuzzback Scrapper"};

            for (int i = 0; i < 2; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Heedless One", "Norwood Archers", "Wildslayer Elves"};

            for (int i = 0; i < 3; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, Constant.Color.Red));
        } else if (id == 16)  // Mines of Kazum Durl
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Dwarven Demolition Team", "Dwarven Pony", "Dwarven Trader"};

            for (int i = 0; i < 3; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] =
                    {"Wall of Earth", "Wall of Air", "Wall of Ice", "Wall of Light", "Carrion Wall", "Steel Wall"};

            for (int i = 0; i < 6; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, Constant.Color.Green));
        } else if (id == 17)  // House Party
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Hopping Automaton", "Honden of Life's Web", "Forbidden Orchard"};

            for (int i = 0; i < 3; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Honden of Infinite Rage", "Mikokoro, Center of the Sea", "Tidehollow Strix"};

            for (int i = 0; i < 3; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 4, Constant.Rarity.Rare, Constant.Color.Colorless));
        } else if (id == 18)  // Crows in the Field
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Straw Soldiers", "Femeref Archers", "Moonglove Extract"};

            for (int i = 0; i < 3; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Defiant Falcon", "Soulcatcher", "Storm Crow", "Hypnotic Specter"};

            for (int i = 0; i < 4; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 5, Constant.Rarity.Rare, null));
        } else if (id == 19)  // The Desert Caravan
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Spidersilk Net", "Dromad Purebred"};

            for (int i = 0; i < 2; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Ambush Party", "Ambush Party", "Gnat Alley Creeper", "Ambush Party", "Ambush Party"};

            for (int i = 0; i < 5; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 5, Constant.Rarity.Rare, null));
        } else if (id == 20)  // Blood Oath
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Counterbalance", "Hatching Plans", "Ley Druid"};

            for (int i = 0; i < 3; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Ior Ruin Expedition", "Oversold Cemetery", "Trapjaw Kelpie"};

            for (int i = 0; i < 3; i++) {
                qa.addCompy(compySetupCards[i]);
            }

            qa.setCardRewardList(pack.generateCards(cards, 5, Constant.Rarity.Rare, Constant.Color.Colorless));
        } else if (id == 21) // Private Domain
        {
            CardList humanList = new CardList();

            Card c = AllZone.getCardFactory().getCard("Strip Mine", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);

            String compySetupCards[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};

            for (int i = 0; i < 5; i++)
                qa.addCompy(compySetupCards[i]);

            qa.setCardRewardList(pack.generateCards(cards, 6, Constant.Rarity.Rare, null));
        } else if (id == 22) // Pied Piper
        {
            CardList humanList = new CardList();
            String humanSetupCards[] = {"Volunteer Militia", "Land Tax", "Elvish Farmer", "An-Havva Township"};

            for (int i = 0; i < 4; i++) {
                Card c = AllZone.getCardFactory().getCard(humanSetupCards[i], AllZone.getHumanPlayer());

                c.setCurSetCode(c.getMostRecentSet());
                c.setImageFilename(CardUtil.buildFilename(c));

                humanList.add(c);
            }
            qa.setHuman(humanList);

            String compySetupCards[] = {"Darksteel Citadel", "Relentless Rats"};

            for (int i = 0; i < 2; i++)
                qa.addCompy(compySetupCards[i]);

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, null));
        }

    }

}//QuestUtil
