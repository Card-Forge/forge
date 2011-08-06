package forge;
import java.util.*;

public class GenerateConstructedDeck
{
  private String color1;
  private String color2;

  private Map<String,String> map = new HashMap<String,String>();

  public GenerateConstructedDeck() {setupMap();}

  private void setupMap()
  {
    map.put(Constant.Color.Black , "Swamp");
    map.put(Constant.Color.Blue  , "Island");
    map.put(Constant.Color.Green , "Forest");
    map.put(Constant.Color.Red   , "Mountain");
    map.put(Constant.Color.White , "Plains");
  }

  public CardList generateDeck()
  {
    CardList deck;

    int check;

    do{
      deck = get2ColorDeck();
      check = deck.getType("Creature").size();

    }while(check < 16 || 24 < check);

    addLand(deck);

    if(deck.size() != 60)
      throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " +deck.size());

    return deck;
  }
  //25 lands
  private void addLand(CardList list)
  {
    Card land;
    for(int i = 0; i < 13; i++)
    {
      land = AllZone.CardFactory.getCard(map.get(color1).toString(), Constant.Player.Computer);
      list.add(land);

      land = AllZone.CardFactory.getCard(map.get(color2).toString(), Constant.Player.Computer);
      list.add(land);
    }
  }//addLand()
  private CardList getCards()
  {
    return filterBadCards(AllZone.CardFactory.getAllCards());
  }//getCards()
  private CardList get2ColorDeck()
  {
    CardList deck = get2Colors(getCards());

    CardList out = new CardList();
    deck.shuffle();

    //trim deck size down to 34 cards, presumes 26 land, for a total of 60 cards
    for(int i = 0; i < 34 && i < deck.size(); i++)
      out.add(deck.get(i));

    return out;
  }
  private CardList get2Colors(CardList in)
  {
    int a;
    int b;

    do{
      a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
      b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
    }while(a == b);//do not want to get the same color twice

    color1 = Constant.Color.onlyColors[a];
    color2 = Constant.Color.onlyColors[b];

    CardList out = new CardList();
    out.addAll(CardListUtil.getColor(in, color1).toArray());
    out.addAll(CardListUtil.getColor(in, color2).toArray());
    out.shuffle();

    CardList artifact = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {         
        //is this really a colorless artifact and not something
        //wierd like Sarcomite Myr which is a colored artifact
        return c.isArtifact() &&
         CardUtil.getColors(c).contains(Constant.Color.Colorless) &&
         !Gui_NewGame.removeArtifacts.isSelected();
      }
    });
    out.addAll(artifact.toArray());
   
    out = out.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
         if(c.isCreature() &&
            c.getNetAttack() <= 1 &&
            Gui_NewGame.removeSmallCreatures.isSelected())
         {
           return false;
         }
            
         return true;
      }
    });
   
    out = filterBadCards(out);
    return out;
  }

  private CardList filterBadCards(CardList list)
  {
    //remove "bad" and multi-colored cards
    final ArrayList<String> remove = new ArrayList<String>();
    remove.add("Sarcomite Myr");
    remove.add("Force of Savagery");
    remove.add("Darksteel Colossus");
    remove.add("Jokulhaups");
    remove.add("Steel Wall");
    remove.add("Ornithopter");
    remove.add("Amnesia");
    remove.add("Battle of Wits");
    remove.add("Ashes to Ashes");
    remove.add("Haunted Angel");
    remove.add("Sky Swallower");
    remove.add("Magus of the Library");
    remove.add("The Unspeakable");
    remove.add("Wall of Kelp");

    remove.add("Incendiary Command");
    remove.add("Memnarch");
    remove.add("Plague Wind");
    remove.add("Klaas, Elf Friend");
    remove.add("Delirium Skeins");

    remove.add("Undying Beast");
    remove.add("Wit's End");

    remove.add("Blinding Light");
    remove.add("Hymn to Tourach");
    
    //cards that slow the computer down
    
    remove.add("Anger");
    remove.add("Brawn");
    remove.add("Valor");
    remove.add("Wonder");
    
    //not fully implemented:
    
    remove.add("Aether Membrane");
    remove.add("Arashi, the Sky Asunder");
    remove.add("Hand of Cruelty");
    remove.add("Hand of Honor");
    
    //useless, or combo cards:
    
    remove.add("Aluren");
    remove.add("Conspiracy");
    remove.add("Crucible of Fire");
    remove.add("Verduran Enchantress");
    remove.add("Enchantress's Presence");
    remove.add("Mesa Enchantress");
    remove.add("Moat");
    remove.add("Magus of the Moat");
    remove.add("Relentless Rats");
    remove.add("Vedalken Archmage");
    remove.add("Hatching Plans");
    remove.add("Sensation Gorger");
    
    //semi useless
    
    remove.add("Wren's Run Packmaster");
    remove.add("Nova Chaser");
    remove.add("Supreme Exemplar");
    remove.add("Goblin Ringleader");
    remove.add("Sylvan Messenger");
    remove.add("Tromp the Domains");
    remove.add("Legacy Weapon");
      
    //cards the AI cannot play (effectively):
    
    remove.add("Necropotence");
    remove.add("Yawgmoth's Bargain");
    remove.add("Sensei's Divining Top");
    remove.add("Standstill");
    //remove.add("Counterspell");
    //remove.add("Exclude");
    //remove.add("False Summoning");
    //remove.add("Essence Scatter");
    //remove.add("Preemptive Strike");
    //remove.add("Punish Ignorance");
    //remove.add("Remand");
    //remove.add("Mystic Snake");
    //remove.add("Absorb");
    //remove.add("Undermine");
    //remove.add("Overwhelming Intellect");
    remove.add("AEther Vial");
    remove.add("Covetous Dragon");
    remove.add("Terramorphic Expanse");
    remove.add("Earthcraft");
    remove.add("Burst of Speed");
    remove.add("Magnify");
    remove.add("Nature's Cloak");
    remove.add("Resuscitate");
    remove.add("Shield Wall");
    remove.add("Solidarity");
    remove.add("Steadfastness");
    remove.add("Tortoise Formation");
    
    //manapool stuff:
    remove.add("Dark Ritual");
    remove.add("Seething Song");
    remove.add("Sol Ring");
    remove.add("Gaea's Cradle");
    remove.add("Priest of Titania");
    remove.add("Tolarian Academy");
    remove.add("Serra's Sanctum");
    remove.add("Basalt Monolith");
    remove.add("Grim Monolith");
    remove.add("Black Lotus");
    remove.add("Composite Golem");
    remove.add("Thran Dynamo");
    remove.add("Elvish Archdruid");
    remove.add("Sunglasses of Urza");
    remove.add("Ceta Discple");
    remove.add("Agent of Stromgald");
    remove.add("Apprentice Wizard");
    remove.add("Azorius Signet");
    remove.add("Bog Initiate");
    remove.add("Boros Signet");
    remove.add("Celestial Prism");
    remove.add("Dimir Signet");
    remove.add("Fire Sprites");
    remove.add("Fyndhorn Elder");
    remove.add("Gilded Lotus");
    remove.add("Golgari Signet");
    remove.add("Greenweaver Druid");
    remove.add("Gruul Signet");
    remove.add("Helionaut");
    remove.add("Izzet Signet");
    remove.add("Knotvine Mystic");
    remove.add("Mana Cylix");
    remove.add("Mana Prism");
    remove.add("Nantuko Elder");
    remove.add("Nomadic Elf");
    remove.add("Orochi Leafcaller");
    remove.add("Orzhov Signet");
    remove.add("Prismatic Lens");
    remove.add("Rakdos Signet");
    remove.add("Sea Scryer");
    remove.add("Selesnya Signet");
    remove.add("Simic Signet");
    remove.add("Sisay's Ring");
    remove.add("Skyshroud Elf");
    remove.add("Ur-Golem's Eye");
    remove.add("Viridian Acolyte");
    remove.add("Worn Powerstone");
   
    
    final ArrayList<Card> goodLand = new ArrayList<Card>();
    //goodLand.add("Faerie Conclave");
    //goodLand.add("Forbidding Watchtower");
    //goodLand.add("Treetop Village");

    CardList out = list.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
    	ArrayList<String> list = CardUtil.getColors(c);
    	if (list.size() == 2)
      	{	
      		if (!(list.contains(color1) && list.contains(color2)))
      		   return false;
      	}
        return CardUtil.getColors(c).size() <= 2 && //only dual colored gold cards
               !c.isLand()                       && //no land
               !remove.contains(c.getName())     || //OR very important
               goodLand.contains(c.getName());
      }
    });

    return out;
  }//filterBadCards()
  public static void main(String[] args)
  {
    GenerateConstructedDeck g = new GenerateConstructedDeck();

    for(int i = 0; i < 50; i++)
    {
      CardList c = g.generateDeck();
      System.out.println(c.getType("Creature").size() +" - " +c.size());
    }
    System.exit(1);

  }//main
}