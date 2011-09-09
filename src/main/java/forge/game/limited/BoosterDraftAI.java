package forge.game.limited;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.MyRandom;
import forge.card.spellability.Ability_Mana;
import forge.deck.Deck;
import forge.game.GameType;

import java.util.*;

/**
 * <p>BoosterDraftAI class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class BoosterDraftAI {
    public BoosterDraft bd = null;
    //once a deck has this number of creatures the computer randomly
    //picks a card, so the final computer deck has 12-20 creatures
    //minimum of creatures per deck
    //private static final int nCreatures = 16;
    /**
     * Constant <code>nDecks=7</code>
     */
    private static final int nDecks = 7;

    //holds all the cards for each of the computer's decks
    private CardList[] deck = new CardList[nDecks];
    private String[][] deckColor = new String[nDecks][];

    /**
     * Constant <code>colorToLand</code>
     */
    private static Map<String, String> colorToLand = new TreeMap<String, String>();

    //picks one Card from in_choose, removes that card, and returns the list
    //returns the cards not picked

    /**
     * <p>choose.</p>
     *
     * @param chooseFrom a {@link forge.CardList} object.
     * @param player    a int.
     * @return a {@link forge.CardList} object.
     */
    public Card choose(final CardList chooseFrom, int player) {
        //in_choose should ONLY be on the RIGHT side of any equal sign
        //only 1 card should be removed from in_choose

        if (Constant.Runtime.DevMode[0])
            System.out.println("Player[" + player + "] pack: " + chooseFrom.toString());

        CardList wouldPick = new CardList();
        boolean hasPicked = false;
        Card pickedCard = new Card();

        CardList AIPlayables = chooseFrom.filter(new CardListFilter() {
            public boolean addCard(Card c) {
            	if (c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True"))
            		return false;
                return true;
            }
        });

        if (playerColors.get(player).Color1.equals("none") && playerColors.get(player).Color2.equals("none")) {
            //
            CardList creatures = AIPlayables.getType("Creature").getColored();
            creatures.sort(bestCreature);
            //for (int i=0; i<creatures.size(); i++)
            //System.out.println("creature[" + i + "]: " + creatures.get(i).getName());

            if (creatures.size() > 0) {
                pickedCard = creatures.get(creatures.size() - 1);
                playerColors.get(player).Color1 = pickedCard.getColor().get(0).toStringArray().get(0);
                if (Constant.Runtime.DevMode[0])
                    System.out.println("Player[" + player + "] Color1: " + playerColors.get(player).Color1);

                playerColors.get(player).Mana1 = playerColors.get(player).ColorToMana(playerColors.get(player).Color1);
                
                //if the first pick has more than one color add the second as second color to draft
                if (pickedCard.getColor().get(0).toStringArray().size() > 1) {
                    playerColors.get(player).Color2 = pickedCard.getColor().get(0).toStringArray().get(1);
                    if (Constant.Runtime.DevMode[0])
                        System.out.println("Player[" + player + "] Color2: " + playerColors.get(player).Color2);

                    playerColors.get(player).Mana2 = playerColors.get(player).ColorToMana(playerColors.get(player).Color2);
                }	
                	
                hasPicked = true;
            }
        } else if (!playerColors.get(player).Color1.equals("none") && playerColors.get(player).Color2.equals("none")) {
            CardList creatures = AIPlayables.getType("Creature").getColored();
            creatures.sort(bestCreature);
            //for (int i=0; i<creatures.size(); i++)
            //System.out.println("creature[" + i + "]: " + creatures.get(i).getName());

            if (creatures.size() > 0) {
                pickedCard = creatures.get(creatures.size() - 1);
                playerColors.get(player).Color2 = pickedCard.getColor().get(0).toStringArray().get(0);
                if (Constant.Runtime.DevMode[0])
                    System.out.println("Player[" + player + "] Color2: " + playerColors.get(player).Color2);

                playerColors.get(player).Mana2 = playerColors.get(player).ColorToMana(playerColors.get(player).Color2);
                hasPicked = true;
            }
        } else {
            CardList typeList;
            CardList colorList;

            colorList = AIPlayables.getOnly2Colors(playerColors.get(player).Color1, playerColors.get(player).Color2);

            if (colorList.size() > 0) {
                typeList = colorList.getType("Creature");
                if (typeList.size() > 0) {
                    typeList.sort(bestCreature);
                    typeList.reverse();
                    wouldPick.add(typeList.get(0));
                    if (typeList.size() > 1)
                        wouldPick.add(typeList.get(1));
                }

                typeList = colorList.getType("Instant");
                typeList.addAll(colorList.getType("Sorcery"));
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(typeList.size() / 2));
                }

                typeList = colorList.getType("Enchantment");
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(0));
                }

                typeList = colorList.getType("Planeswalker");
                if (typeList.size() > 0)
                    wouldPick.add(typeList.get(0));
                
                typeList = colorList.getType("Artifact");
                if (typeList.size() > 0) {
                    CardListUtil.sortCMC(typeList);
                    wouldPick.add(typeList.get(0));
                }

            } else {
/*    		if (!playerColors.get(player).Splash.equals("none")) {
    			// pick randomly from splash color
    			colorList = AIPlayables.getColor(playerColors.get(player).Splash);
    			if (colorList.size() > 0) {
    				Random r = new Random();
    				list.add(colorList.get(r.nextInt(colorList.size())));
    			}
    		}
    		else {
    			// pick splash color
    			ArrayList<String> otherColors = new ArrayList<String>();
    			for (int i=0; i<5; i++)
    				otherColors.add(Constant.Color.onlyColors[i]);
    			otherColors.remove(playerColors.get(player).Color1);
    			otherColors.remove(playerColors.get(player).Color2);
    			
    			colorList = new CardList();
    			for (int i=0; i<otherColors.size(); i++)
    				colorList.add(in_choose.getColor(otherColors.get(i)));
    			
    			if (colorList.size() > 0) {
    				Random r = new Random();
    				pickedCard = colorList.get(r.nextInt(colorList.size()));
    				playerColors.get(player).Splash = pickedCard.getColor().get(0).toStringArray().get(0);
    				System.out.println("Player["+player+"] Splash: "+playerColors.get(player).Splash);
    				playerColors.get(player).ManaS = playerColors.get(player).ColorToMana(playerColors.get(player).Splash);
    				hasPicked = true;
    			}
    		}
*/
                typeList = AIPlayables.getType("Land");
                if (typeList.size() > 0) {
                    for (int i = 0; i < typeList.size(); i++) {
                        ArrayList<Ability_Mana> maList = typeList.get(i).getManaAbility();
                        for (int j = 0; j < maList.size(); j++) {
                            if (maList.get(j).canProduce(playerColors.get(player).Mana1) || maList.get(j).canProduce(playerColors.get(player).Mana2)) //|| maList.get(j).canProduce(playerColors.get(player).ManaS))
                                wouldPick.add(typeList.get(i));
                        }
                    }
                }
            }


        }
        if (!hasPicked) {
            Random r = new Random();

            if (wouldPick.size() > 0) {
                wouldPick.shuffle();
                pickedCard = wouldPick.get(r.nextInt(wouldPick.size()));
            } else {
                chooseFrom.shuffle();
                pickedCard = chooseFrom.get(r.nextInt(chooseFrom.size()));
            }
            
            hasPicked = true;
        }

        if (hasPicked) {
            chooseFrom.remove(pickedCard);
            deck[player].add(pickedCard);

            if (Constant.Runtime.DevMode[0])
                System.out.println("Player[" + player + "] picked " + pickedCard.getName() + " (" + pickedCard.getManaCost() + ") " + pickedCard.getType().toString() + "\n");
        }

        return pickedCard;
    }

/*
  I get some wierd error when I have this method, I don't know whats wrong

  private void checkDeckList(CardList[] deck)
  {
    if(deck.length != nDecks)
      throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck list size is not 7 - " +deck.length);

    for(int i = 0; i < nDecks; i++)
    {
      if(deck[i].size() != 22)
      {
        throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck list size is not 22 - " +deck[i].size() +" - " +deck.toString());
      }
      if(countCreatures(deck[i]) < nCreatures)
        throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck needs more creatures - " +countCreatures(deck[i]));

      for(int inner = 0; inner < 22; inner++)
        if(! CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][0]) &&
           ! CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][1]))
          throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck has different card colors");
    }//for
  }//checkDeckList()
*/

    //private int countCreatures(CardList list) {return list.getType("Creature").size();}

    /**
     * <p>testColors.</p>
     *
     * @param n an array of int.
     */
    private void testColors(int[] n) {
        if (n.length != nDecks)
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers array length does not equal 7");

        Set<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < nDecks; i++)
            set.add(Integer.valueOf(n[i]));

        if (set.size() != nDecks)
            throw new RuntimeException("BoosterDraftAI : testColors error, numbers not unique");

        for (int i = 0; i < nDecks; i++)
            if (n[i] < 0 || deckColorChoices.length <= n[i])
                throw new RuntimeException("BoosterDraftAI : testColors error, index out of range - " + n[i]);
    }//testColors()

    /**
     * <p>getDecks.</p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDecks() {
        //check CardList[] deck for errors
        //checkDeckList(deck);

        Deck[] out = new Deck[deck.length];

        for (int i = 0; i < deck.length; i++) {
            //addLand(deck[i], deckColor[i]);
            //out[i] = getDeck(deck[i]);
            if (Constant.Runtime.DevMode[0])
                System.out.println("Deck[" + i + "]");

            out[i] = buildDeck(deck[i], playerColors.get(i));
        }
        return out;
    }//getDecks()

    /**
     * <p>buildDeck.</p>
     *
     * @param dList a {@link forge.CardList} object.
     * @param pClrs a {@link forge.game.limited.DeckColors} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck buildDeck(CardList dList, DeckColors pClrs) {
        Deck out = new Deck(GameType.Draft);
        CardList outList = new CardList();
        int cardsNeeded = 22;
        int landsNeeded = 18;

        CardList AIPlayables = dList.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !(c.getSVar("RemAIDeck").equals("True"));
            }
        });
        for (int i = 0; i < AIPlayables.size(); i++)
            dList.remove(AIPlayables.get(i));

        CardList creatures = AIPlayables.getType("Creature").getOnly2Colors(pClrs.Color1, pClrs.Color2);

        int nCreatures = 15;

        creatures.sort(bestCreature);
        creatures.reverse();

        int i = 0;
        while (nCreatures > 0 && i < creatures.size()) {
            Card c = creatures.get(i);

            outList.add(c);
            cardsNeeded--;
            nCreatures--;
            AIPlayables.remove(c);

            if (Constant.Runtime.DevMode[0])
                System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");

            i++;
        }

        CardList otherCreatures = AIPlayables.getType("Creature");
        while (nCreatures > 1 && otherCreatures.size() > 1) {
            Card c = otherCreatures.get(MyRandom.random.nextInt(otherCreatures.size() - 1));
            outList.add(c);
            cardsNeeded--;
            nCreatures--;
            AIPlayables.remove(c);

            otherCreatures = AIPlayables.getType("Creature");

            if (Constant.Runtime.DevMode[0])
                System.out.println("AddCreature: " + c.getName() + " (" + c.getManaCost() + ")");
        }

        CardList others = AIPlayables.getNotType("Creature").getNotType("Land").getOnly2Colors(pClrs.Color1, pClrs.Color2);

        int ii = 0;
        while (cardsNeeded > 0 && others.size() > 1) {
            Card c = others.get(MyRandom.random.nextInt(others.size() - 1));

            //out.addMain(c.getName());
            outList.add(c);
            cardsNeeded--;
            AIPlayables.remove(c);

            others = AIPlayables.getNotType("Creature").getNotType("Land").getOnly2Colors(pClrs.Color1, pClrs.Color2);

            if (Constant.Runtime.DevMode[0])
                System.out.println("Others[" + ii++ + "]:" + c.getName() + " (" + c.getManaCost() + ")");
        }

        ii = 0;
        CardList z = AIPlayables.getNotType("Land");
        while (cardsNeeded > 0 && z.size() > 1) {

            //if (z.size() < 1)
            //	throw new RuntimeException("BoosterDraftAI : buildDeck() error, deck does not have enough non-lands");
            Card c = z.get(MyRandom.random.nextInt(z.size() - 1));

            //out.addMain(c.getName());
            outList.add(c);
            cardsNeeded--;
            AIPlayables.remove(c);

            z = AIPlayables.getNotType("Land");

            if (Constant.Runtime.DevMode[0])
                System.out.println("NonLands[" + ii++ + "]:" + c.getName() + "(" + c.getManaCost() + ")");
        }

        CardList lands = AIPlayables.getType("Land");
        while (landsNeeded > 0 && lands.size() > 0) {
            Card c = lands.get(0);

            outList.add(c);
            landsNeeded--;
            AIPlayables.remove(c);

            lands = AIPlayables.getType("Land");

            if (Constant.Runtime.DevMode[0])
                System.out.println("Land:" + c.getName());
        }

        if (landsNeeded > 0)    // attempt to optimize basic land counts according to color representation
        {
            CCnt ClrCnts[] = {new CCnt("Plains", 0),
                    new CCnt("Island", 0),
                    new CCnt("Swamp", 0),
                    new CCnt("Mountain", 0),
                    new CCnt("Forest", 0)};

            // count each card color using mana costs
            // TODO: count hybrid mana differently?
            for (i = 0; i < outList.size(); i++) {
                String mc = outList.get(i).getManaCost();

                // count each mana symbol in the mana cost
                for (int j = 0; j < mc.length(); j++) {
                    char c = mc.charAt(j);

                    if (c == 'W')
                        ClrCnts[0].Count++;
                    else if (c == 'U')
                        ClrCnts[1].Count++;
                    else if (c == 'B')
                        ClrCnts[2].Count++;
                    else if (c == 'R')
                        ClrCnts[3].Count++;
                    else if (c == 'G')
                        ClrCnts[4].Count++;
                }
            }

            // total of all ClrCnts
            int totalColor = 0;
            for (i = 0; i < 5; i++) {
                totalColor += ClrCnts[i].Count;
                //tmpDeck += ClrCnts[i].Color + ":" + ClrCnts[i].Count + "\n";
            }

            //tmpDeck += "totalColor:" + totalColor + "\n";

            for (i = 0; i < 5; i++) {
                if (ClrCnts[i].Count > 0) {    // calculate number of lands for each color
                    float p = (float) ClrCnts[i].Count / (float) totalColor;
                    int nLand = (int) ((float) landsNeeded * p) + 1;
                    //tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand + "\n";
                    if (Constant.Runtime.DevMode[0])
                        System.out.println("Basics[" + ClrCnts[i].Color + "]:" + nLand);

                    // just to prevent a null exception by the deck size fixing code
                    //CardCounts.put(ClrCnts[i].Color, nLand);

                    for (int j = 0; j <= nLand; j++) {
                        Card c = AllZone.getCardFactory().getCard(ClrCnts[i].Color, AllZone.getComputerPlayer());
                        c.setCurSetCode(BoosterDraft.LandSetCode[0]);
                        outList.add(c);
                        landsNeeded--;
                    }
                }
            }
            int n = 0;
            while (landsNeeded > 0) {
                if (ClrCnts[n].Count > 0) {
                    Card c = AllZone.getCardFactory().getCard(ClrCnts[n].Color, AllZone.getComputerPlayer());
                    c.setCurSetCode(BoosterDraft.LandSetCode[0]);
                    outList.add(c);
                    landsNeeded--;

                    if (Constant.Runtime.DevMode[0])
                        System.out.println("AddBasics: " + c.getName());
                }
                if (++n > 4)
                    n = 0;
            }
        }

        while (outList.size() > 40) {
            Card c = outList.get(MyRandom.random.nextInt(outList.size() - 1));
            outList.remove(c);
            AIPlayables.add(c);
        }

        while (outList.size() < 40) {
            Card c = AIPlayables.get(MyRandom.random.nextInt(AIPlayables.size() - 1));
            outList.add(c);
            AIPlayables.remove(c);
        }
        if (outList.size() == 40) {
            for (i = 0; i < outList.size(); i++)
                out.addMain(outList.get(i).getName() + "|" + outList.get(i).getCurSetCode());

            for (i = 0; i < AIPlayables.size(); i++)
                out.addSideboard(AIPlayables.get(i).getName() + "|" + AIPlayables.get(i).getCurSetCode());

            for (i = 0; i < dList.size(); i++)
                out.addSideboard(dList.get(i).getName() + "|" + dList.get(i).getCurSetCode());
        } else
            throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");

        return out;
    }

/*  private Deck getDeck(CardList list)
  {
    Deck out = new Deck(GameType.Draft);
    for(int i = 0; i < list.size(); i++)
      out.addMain(list.get(i).getName());

    return out;
  }//getDeck()

  //add Land to list argument
  private void addLand(CardList list, String[] color)
  {
    Card land;
    for(int i = 0; i < 9; i++)
    {
      land = AllZone.getCardFactory().getCard(colorToLand.get(color[0]).toString(), AllZone.getComputerPlayer());
      
      land.setCurSetCode(land.getMostRecentSet());
      land.setImageFilename(CardUtil.buildFilename(land));
      
      list.add(land);

      land = AllZone.getCardFactory().getCard(colorToLand.get(color[1]).toString(), AllZone.getComputerPlayer());
      
      land.setCurSetCode(land.getMostRecentSet());
      land.setImageFilename(CardUtil.buildFilename(land));
      
      list.add(land);
    }

    //if(list.getType("Land").size() != 18)
      //throw new RuntimeException("BoosterDraftAI : addLand() error, deck does not have 18 lands - " +list.getType("Land").size());

    //if(list.size() != 40)
      //throw new RuntimeException("BoosterDraftAI : addLand() error, deck is not 40 cards - " +list.size());
  }//addLand()
*/

    //returns 7 different ints, within the range of 0-9

    /**
     * <p>getDeckColors.</p>
     *
     * @return an array of int.
     */
    private int[] getDeckColors() {
        int[] out = new int[nDecks];
        int start = MyRandom.random.nextInt(10);

        for (int i = 0; i < out.length; i++) {
            //% to get an index between 0 and deckColorChoices.length
            out[i] = start % deckColorChoices.length;
            start++;
        }
        testColors(out);

        return out;
    }//getDeckColors()

    /**
     * <p>Constructor for BoosterDraftAI.</p>
     */
    public BoosterDraftAI() {
        //choose colors for decks
        int[] n = getDeckColors();
        for (int i = 0; i < n.length; i++) {
            deckColor[i] = deckColorChoices[n[i]];
        }

        //initilize color map
        colorToLand.put(Constant.Color.Black, "Swamp");
        colorToLand.put(Constant.Color.Blue, "Island");
        colorToLand.put(Constant.Color.Green, "Forest");
        colorToLand.put(Constant.Color.Red, "Mountain");
        colorToLand.put(Constant.Color.White, "Plains");

        //initilize deck array and playerColors list
        for (int i = 0; i < deck.length; i++) {
            deck[i] = new CardList();
            playerColors.add(new DeckColors());
        }

    }//BoosterDraftAI()


    private ArrayList<DeckColors> playerColors = new ArrayList<DeckColors>();

    //all 10 two color combinations
    private String[][] deckColorChoices = {
        {Constant.Color.Black, Constant.Color.Blue}, {Constant.Color.Black, Constant.Color.Green},
        {Constant.Color.Black, Constant.Color.Red}, {Constant.Color.Black, Constant.Color.White},

        {Constant.Color.Blue, Constant.Color.Green}, {Constant.Color.Blue, Constant.Color.Red},
        {Constant.Color.Blue, Constant.Color.White},

        {Constant.Color.Green, Constant.Color.Red}, {Constant.Color.Green, Constant.Color.White},

        {Constant.Color.Red, Constant.Color.White}
    };

    private Comparator<Card> bestCreature = new Comparator<Card>() {
        public int compare(Card a, Card b) {
            int cmcA = a.getCMC();
            if (cmcA == 0)
                cmcA = 1;
            cmcA *= 10;

            int cmcB = b.getCMC();
            if (cmcB == 0)
                cmcB = 1;
            cmcB *= 10;

            int attA = a.getBaseAttack() * 10;
            int attB = b.getBaseAttack() * 10;

            int defA = a.getBaseDefense() * 10;
            int defB = b.getBaseDefense() * 10;

            int keyA = a.getKeyword().size() * 10;
            int keyB = b.getKeyword().size() * 10;

            int abA = a.getSpellAbility().length * 10;
            int abB = b.getSpellAbility().length * 10;

            int trgA = a.getTriggers().size() * 10;
            int trgB = b.getTriggers().size() * 10;
            
            int rarA = 0;
            int rarB = 0;
            
            if (a.getCurSetRarity().equals("Common"))
            	rarA = 1;
            else if (a.getCurSetRarity().equals("Uncommon"))
            	rarA = 2;
            else if (a.getCurSetRarity().equals("Rare"))
            	rarA = 4;
            else if (a.getCurSetRarity().equals("Mythic"))
            	rarA = 8;
            
            if (b.getCurSetRarity().equals("Common"))
            	rarB = 1;
            else if (b.getCurSetRarity().equals("Uncommon"))
            	rarB = 2;
            else if (b.getCurSetRarity().equals("Rare"))
            	rarB = 4;
            else if (b.getCurSetRarity().equals("Mythic"))
            	rarB = 8;

            int scoreA = ((attA + defA) / cmcA) + keyA + abA + trgA + rarA;
            int scoreB = ((attB + defB) / cmcB) + keyB + abB + trgB + rarB;

            return scoreA - scoreB;
        }
    };
}//BoosterDraftAI()




