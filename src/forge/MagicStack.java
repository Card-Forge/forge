package forge;
import java.util.*;

public class MagicStack extends MyObservable
{
  private ArrayList<SpellAbility> stack = new ArrayList<SpellAbility>();
  private ArrayList<SpellAbility> frozenStack = new ArrayList<SpellAbility>();
  private boolean frozen = false;
  public void freezeStack() { frozen = true; }
  
  private Object StormCount;
  private Object PlayerSpellCount;
  private Object PlayerCreatureSpellCount;   
  private Object ComputerSpellCount;
  private Object ComputerCreatureSpellCount;
  
  public void reset()
  {
	  stack.clear();
	  frozen = false;
	  frozenStack.clear();
	  this.updateObservers();
  }
  
  public void addAndUnfreeze(SpellAbility sp){
	  frozen = false;
	  this.add(sp);
	  while(!frozenStack.isEmpty()){
		  this.add(frozenStack.get(0));
		  frozenStack.remove(0);
	  }
  }
  
  public void clearFrozen(){
	  // todo: frozen triggered abilities and undoable costs have nasty consequences
	  frozen = false;
	  frozenStack.clear();
  }
  
  public void add(SpellAbility sp, boolean useX)
  {
	  if (!useX)
		  this.add(sp);
	  else
	  {
		  if(sp instanceof Ability_Mana || sp instanceof Ability_Triggered)//TODO make working triggered abilities!
			  sp.resolve(); 
		  else {
			  push(sp);
			  if (sp.getTargetCard()!= null)
		         	CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());
		  }
	  }
  }

  public ManaCost GetMultiKickerSpellCostChange(SpellAbility sa) { 
	  	int Max = 25;
     	String[] Numbers = new String[Max];
   	for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
     	ManaCost manaCost = new ManaCost(sa.getManaCost());
		String Mana = manaCost.toString();
		int MultiKickerPaid = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid;
       	for(int no = 0; no < Max; no ++) Numbers[no] = String.valueOf(no);
       	String Number_ManaCost = " ";
   		if(Mana.toString().length() == 1) Number_ManaCost = Mana.toString().substring(0, 1);
   		else if(Mana.toString().length() == 0) Number_ManaCost = "0";  // Should Never Occur
   		else Number_ManaCost = Mana.toString().substring(0, 2);
   		Number_ManaCost = Number_ManaCost.trim();
   		

       	for(int check = 0; check < Max; check ++) {
       		if(Number_ManaCost.equals(Numbers[check])) {
       			
       			if(check - MultiKickerPaid < 0) {    
       				MultiKickerPaid = MultiKickerPaid - check;
       				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
       				Mana = Mana.replaceFirst(String.valueOf(check),"0");
       			} else {
       				Mana = Mana.replaceFirst(String.valueOf(check),String.valueOf(check - MultiKickerPaid));
       				MultiKickerPaid = 0;
       				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
       			}
       		}
       		Mana = Mana.trim();
       		if(Mana.equals("")) Mana = "0";
       		manaCost = new ManaCost(Mana);	
       	}	
       	 String Color_cut = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored;
       	
		 for(int Colored_Cut = 0; Colored_Cut < Color_cut.length(); Colored_Cut++) {
			 if("WUGRB".contains(Color_cut.substring(Colored_Cut, Colored_Cut + 1))) {
			if(!Mana.equals(Mana.replaceFirst((Color_cut.substring(Colored_Cut, Colored_Cut + 1)), ""))) {             		 
          	Mana = Mana.replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
          	AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored.replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
          	Mana = Mana.trim();
          	if(Mana.equals("")) Mana = "0";
         	manaCost = new ManaCost(Mana);
			 }
			 }
		 }
         
	  return manaCost;
  }
  boolean ActualEffectTriggered = false; // WheneverKeyword Test
  public void add(SpellAbility sp)
  {
	  if(sp instanceof Ability_Mana){ // Mana Abilities get resolved right away
		  sp.resolve(); 
		  return;
	  }
	  
	  if (frozen){
		  frozenStack.add(sp);
		  return;
	  }
	  
	  // if activating player slips through the cracks, assign activating Player to the controller here
	  if (null == sp.getActivatingPlayer()){
			sp.setActivatingPlayer(sp.getSourceCard().getController());
			//System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
	  }
	  // WheneverKeyword Test
	  ActualEffectTriggered = false;
	  if(sp.getSourceCard().getKeyword().toString().contains("WheneverKeyword")) {
		ArrayList<String> a = sp.getSourceCard().getKeyword();
		int WheneverKeywords = 0;
		int WheneverKeyword_Number[] = new int[a.size()];
		for(int x = 0; x < a.size(); x++)
		    if(a.get(x).toString().startsWith("WheneverKeyword")) {
		    	WheneverKeyword_Number[WheneverKeywords] = x;
		    	WheneverKeywords = WheneverKeywords + 1;
		    }
		
		for(int CKeywords = 0; CKeywords < WheneverKeywords; CKeywords++) {
			 String parse = sp.getSourceCard().getKeyword().get(WheneverKeyword_Number[CKeywords]).toString();                
			 String k[] = parse.split(":");
			 if(k[1].equals("ActualSpell") && ActualEffectTriggered == false) {
				 AllZone.GameAction.CheckWheneverKeyword(sp.getSourceCard(),"ActualSpell",null);
			    	 sp.getSourceCard().removeIntrinsicKeyword(parse);
			    	 ActualEffectTriggered = true;
		     }
		}
		  
	  }
		 if(ActualEffectTriggered == false) {
		  //  // WheneverKeyword Test: Added one } at end
	  if(sp instanceof Ability_Triggered || sp instanceof Ability_Static)//TODO make working triggered abilities!
		  sp.resolve(); 
	  else {
		  if(sp.isKickerAbility()) {
			  sp.getSourceCard().setKicked(true);
			  SpellAbility[] sa = sp.getSourceCard().getSpellAbility();
			  int AbilityNumber = 0;
			  for(int i = 0; i < sa.length; i++) 
				  if(sa[i] == sp)  AbilityNumber = i;
			  sp.getSourceCard().setAbilityUsed(AbilityNumber);
		  }
		  if(sp.getSourceCard().isCopiedSpell()) push(sp);
		  if (!sp.isMultiKicker() && !sp.isXCost() && !sp.getSourceCard().isCopiedSpell())
		  {
			  push(sp);
		  }
		  else if (sp.isXCost()  && !sp.getSourceCard().isCopiedSpell())
		  {
			  final SpellAbility sa = sp;
			  final Ability ability = new Ability(sp.getSourceCard(), sa.getXManaCost())
			  {
				  public void resolve()
				  {
					  Card crd = this.getSourceCard();
					  crd.addXManaCostPaid(1);
				  }
			  };
			  
			  final Command unpaidCommand = new Command()
			  {
				private static final long serialVersionUID = -3342222770086269767L;

				public void execute()
				{
					  push(sa);
				}
			  };
			  
			  final Command paidCommand = new Command() {
				private static final long serialVersionUID = -2224875229611007788L;

				public void execute() {
                    ability.resolve();
                    Card crd = sa.getSourceCard();
                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Pay X cost for " + crd.getName() + " (X=" +crd.getXManaCostPaid()+")\r\n",
	                        ability.getManaCost(), this, unpaidCommand, true));
				}
			  };
			  Card crd = sa.getSourceCard();
			  if(sp.getSourceCard().getController().equals(AllZone.HumanPlayer)) {
				  AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Pay X cost for " + sp.getSourceCard().getName() + " (X=" +crd.getXManaCostPaid()+")\r\n",
                        ability.getManaCost(), paidCommand, unpaidCommand, true));
			  } 
			  else //computer
	          {
				  int neededDamage = CardFactoryUtil.getNeededXDamage(sa);
					  
				  while(ComputerUtil.canPayCost(ability) && neededDamage != sa.getSourceCard().getXManaCostPaid()) 
				  {
					  ComputerUtil.playNoStack(ability);
				  }
				  push(sa);
	          }
		  }
		  else if (sp.isMultiKicker()  && !sp.getSourceCard().isCopiedSpell()) //both X and multi is not supported yet
		  {   
			  final SpellAbility sa = sp;
			  final Ability ability = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost())
			  {
				  public void resolve()
				  {
					  this.getSourceCard().addMultiKickerMagnitude(1);
					  //System.out.println("MultiKicker has been paid (currently multi-kicked " + this.getSourceCard().getName() + " " + this.getSourceCard().getMultiKickerMagnitude()+ " times)");
				  }
			  };
			  
			  final Command unpaidCommand = new Command()
			  {
				private static final long serialVersionUID = -3342222770086269767L;

				public void execute()
				  {
					  push(sa);
				  }
			  };
			  
			  final Command paidCommand = new Command() {
				    private static final long serialVersionUID = -6037161763374971106L;
					public void execute() {
	                    ability.resolve();
	                    ManaCost manaCost = GetMultiKickerSpellCostChange(ability);
	  				  if(manaCost.isPaid()) {
						  this.execute();
					  } else {
						  if(AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid == 0 
								  && AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
			                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n"
			                    		+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
				                        manaCost.toString(), this, unpaidCommand));
						  } else {
	                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n" 
	                    		+ "Mana in Reserve: " + ((AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid != 0)? 
	                    				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid:"") + 
	                    				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored + "\r\n"
	                    		+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
		                        manaCost.toString(), this, unpaidCommand));
						  }
					  }
	                }
	          };
			  
			  if(sp.getSourceCard().getController().equals(AllZone.HumanPlayer)) {
				  ManaCost manaCost = GetMultiKickerSpellCostChange(ability); 
			       
				  if(manaCost.isPaid()) {
					  paidCommand.execute();
				  } else {
					  if(AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid == 0 
							  && AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
		                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n"
		                    		+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
			                        manaCost.toString(), paidCommand, unpaidCommand));
					  } else {
                AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n" 
                		+ "Mana in Reserve: " + ((AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid != 0)? 
                				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid:"") + 
                				AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored + "\r\n"
                		+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
	                        manaCost.toString(), paidCommand, unpaidCommand));
					  }
				  }
	            } 
			    else //computer
	            {
	                while(ComputerUtil.canPayCost(ability)) ComputerUtil.playNoStack(ability);
	                push(sa);
	            }
		  }
		  
	  }
    }
		 if (sp.getTargetCard()!= null)
         	CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());
  }
  public int size()
  {
    return stack.size();
  }

public void push(SpellAbility sp)
{
	stack.add(0, sp);
	
	this.updateObservers();
	if(sp.isSpell() && !sp.getSourceCard().isCopiedSpell())
	{
	    Phase.StormCount = Phase.StormCount + 1;
	    if(sp.getSourceCard().getController() == AllZone.HumanPlayer) {
		    Phase.PlayerSpellCount = Phase.PlayerSpellCount + 1; 
		    if(sp.getSourceCard().isCreature() == true) {
		   	    Phase.PlayerCreatureSpellCount = Phase.PlayerCreatureSpellCount + 1;   	    	
		    }
	    } else {
		    Phase.ComputerSpellCount = Phase.ComputerSpellCount + 1;
		    if(sp.getSourceCard().isCreature() == true) {
		    	Phase.ComputerCreatureSpellCount = Phase.ComputerCreatureSpellCount + 1;
		    }
	    }
		//attempt to counter human spell
		if (sp.getSourceCard().getController().equals(AllZone.HumanPlayer) &&
			CardFactoryUtil.isCounterable(sp.getSourceCard()) )
			ComputerAI_counterSpells2.counter_Spell(sp);	
		//put code for Standstill here
		
		GameActionUtil.executePlayCardEffects(sp);
			
	}
  }
  public SpellAbility pop()
  {
	
    SpellAbility sp = (SpellAbility) stack.remove(0);
    this.updateObservers();
    return sp;
  }
  //index = 0 is the top, index = 1 is the next to top, etc...
  public SpellAbility peek(int index)
  {
    return (SpellAbility) stack.get(index);
  }
  public SpellAbility peek()
  {
    return peek(0);
  }
  public ArrayList<Card> getSourceCards()
  {
    ArrayList<Card> a = new ArrayList<Card>();
    Iterator<SpellAbility> it = stack.iterator();
    while(it.hasNext())
      a.add(((SpellAbility)it.next()).getSourceCard());

    return a;
  }
  public void setStormCount(Object stormCount) {
	  	StormCount = stormCount;
	  }

	  public Object getStormCount() {
	  	return StormCount;
	  }
	  public void setPlayerCreatureSpellCount(Object playerCreatureSpellCount) {
			PlayerCreatureSpellCount = playerCreatureSpellCount;
		}

		public Object getPlayerCreatureSpellCount() {
			return PlayerCreatureSpellCount;
		}

		public void setPlayerSpellCount(Object playerSpellCount) {
			PlayerSpellCount = playerSpellCount;
		}

		public Object getPlayerSpellCount() {
			return PlayerSpellCount;
		}

		public void setComputerSpellCount(Object computerSpellCount) {
			ComputerSpellCount = computerSpellCount;
		}

		public Object getComputerSpellCount() {
			return ComputerSpellCount;
		}

		public void setComputerCreatureSpellCount(Object computerCreatureSpellCount) {
			ComputerCreatureSpellCount = computerCreatureSpellCount;
		}

		public Object getComputerCreatureSpellCount() {
			return ComputerCreatureSpellCount;
		}
		}

