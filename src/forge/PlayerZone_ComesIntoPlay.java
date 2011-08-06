package forge;
import java.util.ArrayList;

public class PlayerZone_ComesIntoPlay extends DefaultPlayerZone
{
	private static final long serialVersionUID = 5750837078903423978L;
	
	private boolean trigger = true;
	private boolean leavesTrigger = true;

	public PlayerZone_ComesIntoPlay(String zone, String player)
	{
		super(zone, player);
	}

	public void add(Object o)
	{
		if (o == null)
			throw new RuntimeException(
					"PlayerZone_ComesInto Play : add() object is null");

		super.add(o);

		Card c = (Card) o;
		
		//cannot use addComesIntoPlayCommand - trigger might be set to false;
		if (c.getName().equals("Exploration")) {
			Input_Main.canPlayNumberOfLands++;
			AllZone.Computer.getComputer().addNumberPlayLands(1);	
		}
		else if (c.getName().equals("Azusa, Lost but Seeking")) {
			Input_Main.canPlayNumberOfLands+=2;
			AllZone.Computer.getComputer().addNumberPlayLands(2);
		}
		else if( c.getName().equals("Fastbond")) {
			Input_Main.canPlayNumberOfLands+=100;
			AllZone.Computer.getComputer().addNumberPlayLands(100);
		}
		
		if (trigger)
		{
			c.setSickness(true);// summoning sickness
			c.comesIntoPlay();
			
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
			
			
			if (c.isLand())
			{
				//System.out.println("A land just came into play: " + c.getName());
				
				CardList list = new CardList(play.getCards());
				list = list.filter(new CardListFilter()
				{
					public boolean addCard(Card c) {
						return c.getKeyword().contains("Landfall");
					}
				});
				
				for (int i=0; i<list.size();i++)
				{
					GameActionUtil.executeLandfallEffects(list.get(i));
				}
				
			}//isLand()
			
			//hack to make tokens trigger ally effects:
			CardList clist = new CardList(play.getCards());
			clist = clist.filter(new CardListFilter()
			{
				public boolean addCard(Card c) {
					return c.getName().equals("Conspiracy") && c.getChosenType().equals("Ally");
				}
			});
			
			String[] allyNames = {"Umara Raptor", "Tuktuk Grunts", "Oran-Rief Survivalist", "Nimana Sell-Sword", "Makindi Shieldmate", 
								  "Kazandu Blademaster","Turntimber Ranger", "Highland Berserker", "Joraga Bard"};
			final ArrayList<String> allyNamesList = new ArrayList<String>();
			
			for (int i=0;i<allyNames.length;i++)
			{
				allyNamesList.add(allyNames[i]);
			}
			
			if (c.getType().contains("Ally") || (clist.size() > 0 && (c.getType().contains("Creature") || c.getKeyword().contains("Changeling"))) || allyNamesList.contains(c.getName()))
			{
				CardList list = new CardList(play.getCards());
				list = list.filter(new CardListFilter()
				{
					public boolean addCard(Card c) {
						return c.getType().contains("Ally") || c.getKeyword().contains("Changeling") || allyNamesList.contains(c.getName());
					}				
				});
				
				for (Card var : list)
				{
					GameActionUtil.executeAllyEffects(var);
				}
			}
			
			
		}
		if (AllZone.StateBasedEffects.getCardToEffectsList().containsKey(c.getName()))
		{
			String[] effects = AllZone.StateBasedEffects.getCardToEffectsList().get(c.getName());
			for (String effect : effects) {
				AllZone.StateBasedEffects.addStateBasedEffect(effect);
			}	
		}
		
		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
		CardList meek = new CardList(grave.getCards());
		
		if (meek.size() > 0 && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1)
		{
			for (int i=0;i<meek.size();i++)
			{
				final Card crd = meek.get(i);
				final Card creat = c;
				final PlayerZone graveZone = grave;
				final PlayerZone playZone = play;
				Ability ability = new Ability(meek.get(i), "0")
				{
					public void resolve() {
						if (crd.getController().equals(Constant.Player.Human))
						{
							String[] choices = { "Yes", "No" };
					    	  
							Object q = null;

							q = AllZone.Display.getChoiceOptional("Attach " +crd + " to " +creat + "?", choices);
							if (q == null || q.equals("No"))
								;
							else if (AllZone.GameAction.isCardInZone(crd, graveZone) && AllZone.GameAction.isCardInPlay(creat) && creat.isCreature() && 
									 creat.getNetAttack() == 1 && creat.getNetDefense() == 1){
								graveZone.remove(crd);
								playZone.add(crd);
								
								crd.equipCard(creat);
							}

						}
						else //computer
						{
							if (AllZone.GameAction.isCardInZone(crd, graveZone) && AllZone.GameAction.isCardInPlay(creat) && creat.isCreature() && 
								creat.getNetAttack() == 1 && creat.getNetDefense() == 1) {
								graveZone.remove(crd);
								playZone.add(crd);
								
								crd.equipCard(creat);
							}
						}
					}						
				};
				
				ability.setStackDescription("Sword of the Meek - Whenever a 1/1 creature enters the battlefield under your control, you may return Sword of the Meek from your graveyard to the battlefield, then attach it to that creature.");
				AllZone.Stack.add(ability);
			}
		}
		

		
		/*
		for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
			Command com = GameActionUtil.commands.get(effect);
			com.execute();
		}
		*/

		//System.out.println("Size: " + AllZone.StateBasedEffects.getStateBasedMap().size());
	}
	
	public void remove(Object o)
	{
		
		super.remove(o);
		
		Card c = (Card) o;
		
		//cannot use addLeavesPlayCommand - trigger might be set to false
		if (c.getName().equals("Exploration")) {
			Input_Main.canPlayNumberOfLands--;
			AllZone.Computer.getComputer().addNumberPlayLands(-1);
		}
		else if (c.getName().equals("Azusa, Lost but Seeking")) {
			Input_Main.canPlayNumberOfLands-=2;
			AllZone.Computer.getComputer().addNumberPlayLands(-2);
		}
		else if( c.getName().equals("Fastbond")) {
			Input_Main.canPlayNumberOfLands-=100;
			AllZone.Computer.getComputer().addNumberPlayLands(-100);
		}
		
		
		if (leavesTrigger)
			c.leavesPlay();
		
		if (AllZone.StateBasedEffects.getCardToEffectsList().containsKey(c.getName()))
		{
			String[] effects = AllZone.StateBasedEffects.getCardToEffectsList().get(c.getName());
			String tempEffect = "";
			for (String effect : effects) {
				tempEffect = effect; 
				AllZone.StateBasedEffects.removeStateBasedEffect(effect);
				Command comm = GameActionUtil.commands.get(tempEffect); //this is to make sure cards reset correctly
				comm.execute();
			}
			
		}
		for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
			Command com = GameActionUtil.commands.get(effect);
			com.execute();
		}
		
		
	}

	public void setTrigger(boolean b)
	{
		trigger = b;
	}
	
	public void setLeavesTrigger(boolean b)
	{
		leavesTrigger = b;
	}
	
	public void setTriggers(boolean b)
	{
		trigger = b;
		leavesTrigger = b;
	}
}
