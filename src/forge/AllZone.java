package forge;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class AllZone implements NewConstants {
    //only for testing, should read decks from local directory
//  public static final IO IO = new IO("all-decks");
	
	public static final Player					HumanPlayer				= new HumanPlayer("Human");
    public static final Player					ComputerPlayer			= new AIPlayer("Computer");
    
    public static QuestData                      QuestData          = null;
    public static Quest_Assignment 			     QuestAssignment    = null;
    public static final NameChanger              NameChanger        = new NameChanger();
    
    public static EndOfTurn                      EndOfTurn          = new EndOfTurn();
    public static EndOfCombat					 EndOfCombat		= new EndOfCombat();
    //public static final CardFactory              CardFactory        = new CardFactory(ForgeProps.getFile(CARDS));
    public static final CardFactory              CardFactory        = new CardFactory(ForgeProps.getFile(CARDSFOLDER));
    
    public static final Phase                    Phase              = new Phase();
    public static final MagicStack               Stack              = new MagicStack();
    public static final InputControl             InputControl       = new InputControl();
    public static final GameAction               GameAction         = new GameAction();
    public static final StaticEffects        	 StaticEffects      = new StaticEffects();
    public static final GameInfo				 GameInfo 			= new GameInfo();
    
    
    
    //initialized at Runtime since it has to be the last object constructed

    public static ComputerAI_Input Computer;

    //shared between Input_Attack              , Input_Block,
    //                       Input_CombatDamage , InputState_Computer
    public static Combat Combat   = new Combat();
    public static Combat pwCombat = new Combat();//for Planeswalker combat

    //public static PlayerLife Human_Life    = new PlayerLife(AllZone.HumanPlayer);
    //public static PlayerLife ComputerPlayer = new PlayerLife(AllZone.ComputerPlayer);
    
    //public static PlayerPoisonCounter Human_PoisonCounter = new PlayerPoisonCounter();
    //public static PlayerPoisonCounter ComputerPlayer = new PlayerPoisonCounter();

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    public final static PlayerZone Human_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play, AllZone.HumanPlayer);
    public final static PlayerZone Human_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.HumanPlayer);
    public final static PlayerZone Human_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.HumanPlayer);
    public final static PlayerZone Human_Library   = new DefaultPlayerZone(Constant.Zone.Library   , AllZone.HumanPlayer);
    public final static PlayerZone Human_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, AllZone.HumanPlayer);

    public final static PlayerZone Computer_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play      , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Library   = new DefaultPlayerZone(Constant.Zone.Library   , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, AllZone.ComputerPlayer);
    
    public static final ManaPool ManaPool = new ManaPool(AllZone.HumanPlayer);
    
    public static Display Display = new GuiDisplay2();


    private final static Map<String,PlayerZone> map = new HashMap<String,PlayerZone>();

    static
    {
	map.put(Constant.Zone.Graveyard         + AllZone.HumanPlayer, Human_Graveyard);
	map.put(Constant.Zone.Hand              + AllZone.HumanPlayer, Human_Hand);
	map.put(Constant.Zone.Library           + AllZone.HumanPlayer, Human_Library);
	map.put(Constant.Zone.Play              + AllZone.HumanPlayer, Human_Play);
	map.put(Constant.Zone.Removed_From_Play + AllZone.HumanPlayer, Human_Removed);

	map.put(Constant.Zone.Graveyard         + AllZone.ComputerPlayer, Computer_Graveyard);
	map.put(Constant.Zone.Hand              + AllZone.ComputerPlayer, Computer_Hand);
	map.put(Constant.Zone.Library           + AllZone.ComputerPlayer, Computer_Library);
	map.put(Constant.Zone.Play              + AllZone.ComputerPlayer, Computer_Play);
	map.put(Constant.Zone.Removed_From_Play + AllZone.ComputerPlayer, Computer_Removed);
    }
    public static PlayerZone getZone(Card c)
    {
	Iterator<PlayerZone> it = map.values().iterator();
	PlayerZone p;
	while(it.hasNext())
	{
	    p = (PlayerZone)it.next();

	    if(GameAction.isCardInZone(c, p))
		return p;
	}
	return null;
//	throw new RuntimeException("AllZone : getZone() error, card is not found, card is " +c);
    }
    public static PlayerZone getZone(String zone, Player player)
    {
    if (null == player) { //this is a really bad hack, to allow raging goblin to attack on turn 1
    	player = AllZone.HumanPlayer; 
    	//TODO - someday, maybe this needs to be fixed so it's *not* an evil hack
    	//System.out.println("Evil hack");
    }

	Object o = map.get(zone + player);
	if(o == null)
	    throw new RuntimeException("AllZone : getZone() invalid parameters " +zone +" " +player);

	return (PlayerZone)o;
    }
}//AllZone