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

    public static final CardFactory              CardFactory        = new CardFactory(ForgeProps.getFile(CARDSFOLDER));
    
    public static final Phase                    Phase              = new Phase();
    public static final MagicStack               Stack              = new MagicStack();
    public static final InputControl             InputControl       = new InputControl();
    public static final GameAction               GameAction         = new GameAction();
    public static final StaticEffects        	 StaticEffects      = new StaticEffects();
    public static final GameInfo				 GameInfo 			= new GameInfo();
    

    //initialized at Runtime since it has to be the last object constructed

    public static ComputerAI_Input Computer;

    //shared between Input_Attack, Input_Block, Input_CombatDamage , InputState_Computer
    
    public static Combat Combat   = new Combat();
    public static Combat pwCombat = new Combat();//for Planeswalker combat

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    public final static PlayerZone Human_Battlefield	= new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield, AllZone.HumanPlayer);
    public final static PlayerZone Human_Hand      		= new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.HumanPlayer);
    public final static PlayerZone Human_Graveyard 		= new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.HumanPlayer);
    public final static PlayerZone Human_Library   		= new DefaultPlayerZone(Constant.Zone.Library   , AllZone.HumanPlayer);
    public final static PlayerZone Human_Exile   		= new DefaultPlayerZone(Constant.Zone.Exile, AllZone.HumanPlayer);
    public final static PlayerZone Human_Command   		= new DefaultPlayerZone(Constant.Zone.Command, AllZone.HumanPlayer);

    public final static PlayerZone Computer_Battlefield	= new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield      , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Hand      	= new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Graveyard 	= new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Library   	= new DefaultPlayerZone(Constant.Zone.Library   , AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Exile   	= new DefaultPlayerZone(Constant.Zone.Exile, AllZone.ComputerPlayer);
    public final static PlayerZone Computer_Command   	= new DefaultPlayerZone(Constant.Zone.Command, AllZone.ComputerPlayer);
    
    public final static PlayerZone Stack_Zone   = new DefaultPlayerZone(Constant.Zone.Stack, null);
    
    public static final ManaPool ManaPool = new ManaPool(AllZone.HumanPlayer);
    
    public static Display Display = new GuiDisplay2();

    private final static Map<String,PlayerZone> map = new HashMap<String,PlayerZone>();

    static
    {
		map.put(Constant.Zone.Graveyard         + AllZone.HumanPlayer, Human_Graveyard);
		map.put(Constant.Zone.Hand              + AllZone.HumanPlayer, Human_Hand);
		map.put(Constant.Zone.Library           + AllZone.HumanPlayer, Human_Library);
		map.put(Constant.Zone.Battlefield		+ AllZone.HumanPlayer, Human_Battlefield);
		map.put(Constant.Zone.Exile 			+ AllZone.HumanPlayer, Human_Exile);
		map.put(Constant.Zone.Command 			+ AllZone.HumanPlayer, Human_Command);
	
		map.put(Constant.Zone.Graveyard         + AllZone.ComputerPlayer, Computer_Graveyard);
		map.put(Constant.Zone.Hand              + AllZone.ComputerPlayer, Computer_Hand);
		map.put(Constant.Zone.Library           + AllZone.ComputerPlayer, Computer_Library);
		map.put(Constant.Zone.Battlefield       + AllZone.ComputerPlayer, Computer_Battlefield);
		map.put(Constant.Zone.Exile 			+ AllZone.ComputerPlayer, Computer_Exile);
		map.put(Constant.Zone.Command 			+ AllZone.ComputerPlayer, Computer_Command);
		
		map.put(Constant.Zone.Stack				+ null					, Stack_Zone);
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
    }
    
    public static PlayerZone getZone(String zone, Player player)
    {
		Object o = map.get(zone + player);
		if(o == null)
		    throw new RuntimeException("AllZone : getZone() invalid parameters " +zone +" " +player);
	
		return (PlayerZone)o;
    }
}//AllZone