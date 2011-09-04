package forge.quest.data;

import java.util.List;
import java.util.Map.Entry;

import forge.*;
import forge.card.CardPoolView;
import forge.card.CardPrinted;

/**
 * <p>QuestUtil class.</p>
 * General utility class for quest tasks.
 *
 * @author Forge
 * @version $Id$
 */
public class QuestUtil {

    /**
     * <p>getComputerCreatures.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAIExtraCards(QuestData qd) {
        return new CardList();
    }
    
    /**
     * <p>getComputerCreatures.</p>
     * Assembles extra cards computer will have in play.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qa a {@link forge.Quest_Assignment} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAIExtraCards(QuestData qd, DeckSingleQuest sq) {
        CardList list = new CardList();
        if (sq != null) {
            CardPoolView compCards = sq.getDeck().getAIExtraCards();

            /*for (String s : compCards) {
                Card c = extraCardBuilder(s);
                if(c!=null) {
                    list.add(c);  
                }
            }*/
        }
        return list;
    }
    
    /**
     * <p>getHumanPlantAndPet.</p>
     * Starts up empty list of extra cards for human.
     * Adds plant and pet as appropriate.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanExtraCards(QuestData qd) {
        System.out.println("LOOKATME");
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
     * Checks for plant and pet, then adds extra cards for human from quest deck.
     * Assembles extra cards human will have in play.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qq a DeckSingleQuest object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanExtraCards(QuestData qd, DeckSingleQuest sq) {
        CardList list = getHumanExtraCards(qd);

        /*if (sq != null) {
            List<Entry<CardPrinted, Integer>> humanCards = 
                    sq.getDeck().getHumanExtraCards().getOrderedList();

            for (String s : humanCards) {
                Card c = extraCardBuilder(s);
                if(c!=null) {
                    list.add(c);  
                }
            }
        }*/
        return list;
    }
    
    /**
     * <p>extraCardBuilder.</p>
     * Assembles card objects for extra cards and tokens for AI and human.
     * 
     * @param  String card name
     * @return Card object.
     */
    public static Card extraCardBuilder(String s) {
        Card c = null;
        int i;
        
        if(s.substring(0,5).equals("TOKEN")) {
            String[] tokenProps = s.split("\\|");
            
            if(tokenProps.length < 6) {
                System.err.println("QuestUtil > extraCardBuilder() reports an " +
                        "incomplete token in the current deck.\n"+
                        "Token should follow the form:\n"+
                        "TOKEN|color|attack|defense|name|type|type...\n"+
                        "For example: TOKEN|G|0|1|sheep|Creature");
                return c;
            }
            else {
                c = new Card(); 
                
                c.setManaCost("0");
                c.addColor(tokenProps[1]);
                c.setBaseAttack(Integer.parseInt(tokenProps[2]));
                c.setBaseDefense(Integer.parseInt(tokenProps[3]));
                
                // Uppercase each word in name
                StringBuilder name = new StringBuilder(tokenProps[4]);
                i = 0;
                do {
                    name.replace(i, i + 1, name.substring(i,i + 1).toUpperCase());
                  i =  name.indexOf(" ", i) + 1;
                } while (i > 0 && i < name.length());
                c.setName(name.toString());
                
                i = 4;
                while(i<tokenProps.length) {
                    c.addType(tokenProps[i++]);
                }

                // Example image name: G 0 1 sheep
                c.setImageName(tokenProps[1]+" "+tokenProps[2]+" "+tokenProps[3]+" "+tokenProps[4]);

                c.setToken(true);
                c.addController(AllZone.getHumanPlayer());
                c.setOwner(AllZone.getHumanPlayer()); 
            }
        }        
        else if(!s.equals("")) {
            c = AllZone.getCardFactory().getCard(s, AllZone.getComputerPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));
        }
        
        return c;
    }    
    


    // THIS INFORMATION WILL BE STORED IN THE DCK FILE.
    /**
     * <p>setupQuest.</p>
     *
     * @param qa a {@link forge.Quest_Assignment} object.
     */
    
    /*
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
            qa.addCompy("Infernal Genesis");
            
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Black));
        } else if (id == 4) //Red Dungeon
        {
            qa.addCompy("Furnace of Rath");

            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Red));
        } else if (id == 5) //Green Dungeon
        {
            CardList humanList = new CardList();
            Card c = AllZone.getCardFactory().getCard("Defense of the Heart", AllZone.getHumanPlayer());

            c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            humanList.add(c);

            qa.setHuman(humanList);
            
            qa.addCompy("Eladamri's Vineyard");
            qa.addCompy("Upwelling");
            
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Green));
        } else if (id == 6) //Colorless Dungeon
        {
            for (int i = 0; i < 3; i++)
                qa.addCompy("Eon Hub");
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, Constant.Color.Colorless));
        } else if (id == 7) //Gold Dungeon
        {
            qa.addCompy("Darksteel Ingot");
            
            qa.setCardRewardList(pack.generateCards(cards, 3, Constant.Rarity.Rare, "Multicolor"));
        } else if (id == 8) {
            CardList humanList = new CardList();
            for (int i = 0; i < 3; i++) {
                //CANNOT use makeToken because of WheneverKeyword
                Card c = new Card();
                c.setName("Sheep");
                c.setImageName("G 0 1 Sheep");

                c.addController(AllZone.getHumanPlayer());
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

                c.addController(AllZone.getHumanPlayer());
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

                c.addController(AllZone.getHumanPlayer());
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
    */

}//QuestUtil
