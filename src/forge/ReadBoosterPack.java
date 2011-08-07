package forge;


//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
import java.util.ArrayList;

import forge.properties.NewConstants;



public class ReadBoosterPack implements NewConstants {
    
//    final private static String comment = "//";
    
    private CardList     commonCreatureList = new CardList();
    private CardList     commonNonCreatureList = new CardList();
    
    private CardList     commonList = new CardList();
    private CardList     uncommonList = new CardList();
    private CardList     rareList = new CardList();
    
    public static void main(String[] args) {
        //testing
        ReadBoosterPack r = new ReadBoosterPack();
        

        for(int i = 0; i < 1000; i++) {
            r.getBoosterPack5();
        }
        
        System.exit(0);
    }//main()
    
    /*
    //average creature versus noncreature

    ReadBoosterPack r = new ReadBoosterPack();
    double n = 0; //total
    int nCreature = 0;
    int nSpell = 0;

    for(int i = 0; i < 1000; i++)
    {
      CardList list = r.getBoosterPack();

      int c = list.getType("Creature").size();
      nCreature += c;
      nSpell += (15 -c);

      n += 15;
    }
    System.out.println(nCreature / n +" - " +nSpell / n);

    System.exit(0);
    */

    public ReadBoosterPack() {
        setup();
    }
    
    //returns "common", "uncommon", or "rare"
    public String getRarity(String cardName) {
        if(commonList.containsName(cardName)) return "Common";
        if(uncommonList.containsName(cardName)) return "Uncommon";
        if(rareList.containsName(cardName)) return "Rare";
        
        ArrayList<String> land = new ArrayList<String>();
        land.add("Forest");
        land.add("Plains");
        land.add("Swamp");
        land.add("Mountain");
        land.add("Island");
        land.add("Terramorphic Expanse");
        land.add("Snow-Covered Forest");
        land.add("Snow-Covered Plains");
        land.add("Snow-Covered Swamp");
        land.add("Snow-Covered Mountain");
        land.add("Snow-Covered Island");
        if(land.contains(cardName)) return "Land";
        
        return "error";
    }
    
    public CardList getBoosterPack5() {
        CardList list = new CardList();
        for(int i = 0; i < 5; i++)
            list.addAll(getBoosterPack());
        
        for(int i = 0; i < 20; i++) {
            list.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Forest", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Island", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Plains", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Mountain", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Swamp", AllZone.HumanPlayer));
        }
        
        for(int i = 0; i < 4; i++)
            list.add(AllZone.CardFactory.getCard("Terramorphic Expanse", AllZone.HumanPlayer));
        
        return list;
    }//getBoosterPack5()
    
    public CardList getBoosterPack() {
        CardList pack = new CardList();
        
        pack.add(getRandomCard(rareList));
        
        for(int i = 0; i < 3; i++)
            pack.add(getRandomCard(uncommonList));
        
        //11 commons, 7 creature 4 noncreature
        CardList variety;
        for(int i = 0; i < 7; i++) {
            variety = getVariety(commonCreatureList);
            pack.add(getRandomCard(variety));
        }
        
        for(int i = 0; i < 4; i++) {
            variety = getVariety(commonNonCreatureList);
            pack.add(getRandomCard(variety));
        }
        
        if(pack.size() != 15)
            throw new RuntimeException("ReadBoosterPack : getBoosterPack() error, pack is not 15 card - "
                    + pack.size());
        
        return pack;
    }
    
    public CardList getShopCards(int numberWins)
    {
    	CardList list = new CardList();
    	
    	int numberRares = 1 + numberWins / 15;
    	if (numberRares > 10 )
    		numberRares = 10;
    	
    	for (int i=0;i<numberRares;i++)
    		list.add(getRandomCard(rareList));
    	
    	int numberUncommons = 3 + numberWins/10;
    	if (numberUncommons > 20)
    		numberUncommons = 20;
    	
    	for(int i = 0; i < numberUncommons; i++)
            list.add(getRandomCard(uncommonList));
    	
    	int numberCommons = 5 + numberWins/5;
    	if (numberCommons > 35)
    		numberCommons = 35;
    	
    	for(int i = 0; i < numberCommons; i++)
            list.add(getRandomCard(commonList));
    	
    	for (int i = 0;i<10;i++)
    	{
	    	list.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
	        list.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
	        list.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
	        list.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
	        list.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
    	}
    	
    	for (int i = 0;i<5;i++)
    	{
    		list.add(AllZone.CardFactory.getCard("Snow-Covered Forest", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Island", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Plains", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Mountain", AllZone.HumanPlayer));
            list.add(AllZone.CardFactory.getCard("Snow-Covered Swamp", AllZone.HumanPlayer));
    	}
    	
    	return list;
    }
    
    //return CardList of 5 or 6 cards, one for each color and maybe an artifact
    private CardList getVariety(CardList in) {
        CardList out = new CardList();
        
        String color[] = Constant.Color.Colors;
        Card check;
        in.shuffle();
        
        for(int i = 0; i < color.length; i++) {
            check = findColor(in, color[i]);
            if(check != null) out.add(check);
        }
        
        return out;
    }//getVariety()
    
    private Card findColor(CardList in, String color) {
        for(int i = 0; i < in.size(); i++)
            if(CardUtil.getColors(in.get(i)).contains(color)) return in.get(i);
        
        return null;
    }
    
    
    private Card getRandomCard(CardList list) {
        for(int i = 0; i < 10; i++)
            list.shuffle();
        
        int index = MyRandom.random.nextInt(list.size());
        
        Card c = AllZone.CardFactory.copyCard(list.get(index));
        c.setRarity("rare");
        return c;
    }//getRandomCard()
    
    private void setup() {
        //commonList = readFile(ForgeProps.getFile(REGULAR.COMMON));
        //uncommonList = readFile(ForgeProps.getFile(REGULAR.UNCOMMON));
        //rareList = readFile(ForgeProps.getFile(REGULAR.RARE));
        
        //commonCreatureList = commonList.getType("Creature");
        //commonNonCreatureList = commonList.filter(new CardListFilter() {
        //    public boolean addCard(Card c) {
        //        return !c.isCreature();
        //    }
        //});
    	
        CardList AllCards = new CardList(AllZone.CardFactory.getAllCards().toArray());
        
        for (int i=0; i<AllCards.size(); i++)
        {
        	Card aCard = AllCards.get(i);
        	String rr = aCard.getSVar("Rarity");
        	
        	if (rr.equals("Common"))
        	{
        		commonList.add(aCard);
        		if (aCard.isCreature())
        			commonCreatureList.add(aCard);
        		else
        			commonNonCreatureList.add(aCard);
        	}
        	else if (rr.equals("Uncommon"))
        	{
        		uncommonList.add(aCard);
        	}
        	else if (rr.equals("Rare"))
        	{
        		rareList.add(aCard);
        	}
        	else if (rr.equals("Mythic"))
        	{
        		rareList.add(aCard);
        	}
        		
        }

    }//setup()
    

/*    private CardList readFile(File file) {
        CardList cardList = new CardList();
        
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            
            //stop reading if end of file or blank line is read
            while(line != null && (line.trim().length() != 0)) {
                Card c;
                if(!line.startsWith(comment)) {
                    c = AllZone.CardFactory.getCard(line.trim(), AllZone.HumanPlayer);
                    cardList.add(c);
                }
                
                line = in.readLine();
            }//if
            
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadBoosterPack : readFile error, " + ex);
        }
        
        return cardList;
    }//readFile()
*/
}

/*
import java.util.*;
import java.io.*;

public class ReadBoosterPack
{
//  final private String commonFilename   = Constant.IO.baseDir +"data/common.txt";
//  final private String uncommonFilename = Constant.IO.baseDir +"data/uncommon.txt";
//  final private String rareFilename     = Constant.IO.baseDir +"data/rare.txt";

  final private String commonFilename   = "common.txt";
  final private String uncommonFilename = "uncommon.txt";
  final private String rareFilename     = "rare.txt";

  final private String comment = "//";

  private ArrayList commonList;
  private ArrayList uncommonList;
  private ArrayList rareList;


  public static void main(String[] args)
  {
    ReadBoosterPack r = new ReadBoosterPack();
    CardList list = r.getBoosterPack();
    double n = 0; //total
    int nCreature = 0;
    int nSpell = 0;

    for(int i = 0; i < 2; i++)
    {
      int c = list.getType("Creature").size();
      nCreature += c;
      nSpell += (15 -c);

      n += 15;
    }
    System.out.println(nCreature / n +" - " +nSpell / n);

    System.exit(0);
  }//main()

  public ReadBoosterPack() {setup();}

  //returns "common", "uncommon", or "rare"
  public String getRarity(String cardName)
  {
    if(commonList.contains(cardName))
      return "Common";
    if(uncommonList.contains(cardName))
      return "Uncommon";
    if(rareList.contains(cardName))
      return "Rare";

    ArrayList land = new ArrayList();
    land.add("Forest");
    land.add("Plains");
    land.add("Swamp");
    land.add("Mountain");
    land.add("Island");
    land.add("Terramorphic Expanse");
    if(land.contains(cardName))
      return "Land";

    return "error";
  }

  public CardList getBoosterPack5()
  {
    CardList list = new CardList();
    for(int i = 0; i < 5; i++)
      list.addAll(getBoosterPack());

    for(int i = 0; i < 40; i++)
    {
      list.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
      list.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
      list.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
      list.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
      list.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
    }

    for(int i = 0; i < 4; i++)
      list.add(AllZone.CardFactory.getCard("Terramorphic Expanse", AllZone.HumanPlayer));

    return list;
  }//getBoosterPack5()

  public CardList getBoosterPack()
  {
    CardList pack = new CardList();

    pack.add(getRandomCard(rareList));

    for(int i = 0; i < 3; i++)
      pack.add(getRandomCard(uncommonList));

    for(int i = 0; i < 11; i++)
      pack.add(getRandomCard(commonList));

    return pack;
  }
  private Card getRandomCard(ArrayList list)
  {
    for(int i = 0; i < 10; i++)
      Collections.shuffle(list, MyRandom.random);

    int index = MyRandom.random.nextInt(list.size());
    String name = list.get(index).toString();

    Card c =  AllZone.CardFactory.getCard(name, AllZone.HumanPlayer);
    c.setRarity("rare");
    return c;
  }//getRandomCard()

  private void setup()
  {
    commonList   = readFile(commonFilename);
    uncommonList = readFile(uncommonFilename);
    rareList     = readFile(rareFilename);

    checkName(commonList);
    checkName(uncommonList);
    checkName(rareList);
  }
  private void checkName(ArrayList name)
  {
    for(int i = 0; i < name.size(); i++)
      AllZone.CardFactory.getCard(name.get(i).toString(), AllZone.HumanPlayer);
  }

  //returns an ArrayList of Strings, the names of the cards read
  private ArrayList readFile(String filename)
  {
    ArrayList cardName = new ArrayList();

    BufferedReader in;
    try{
      in = new BufferedReader(new FileReader(filename));
      String line = in.readLine();

      //stop reading if end of file or blank line is read
      while(line != null && (line.trim().length() != 0))
      {
        if(! line.startsWith(comment))
          cardName.add(line.trim());

        line = in.readLine();
      }//if

    }catch(Exception ex){throw new RuntimeException("ReadBoosterPack : readFile error, " + ex);}

    return cardName;
  }//readFile()
}
*/
