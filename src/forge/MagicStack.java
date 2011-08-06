package forge;
import java.util.*;

public class MagicStack extends MyObservable
{
  private ArrayList<SpellAbility> stack = new ArrayList<SpellAbility>();

  public void reset()
  {
	  stack.clear();
	  this.updateObservers();
  }

  public void add(SpellAbility sp)
  {
	  if(sp instanceof Ability_Mana || sp instanceof Ability_Triggered)//TODO make working triggered abilities!
		  sp.resolve(); 
	  else {
		  if (!sp.isMultiKicker())
		  {
			  push(sp);
		  }
		  else //this spell does have multikicker
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
				  public void execute()
				  {
					  push(sa);
				  }
			  };
			  
			  final Command paidCommand = new Command() {
				    private static final long serialVersionUID = -6037161763374971106L;
					public void execute() {
	                    ability.resolve();
	                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n",
		                        ability.getManaCost(), this, unpaidCommand));
	                }
	          };
			  
			  if(sp.getSourceCard().getController().equals(Constant.Player.Human)) {
	                AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for " + sp.getSourceCard() + "\r\n",
	                        ability.getManaCost(), paidCommand, unpaidCommand));
	            } 
			    else //computer
	            {
	                while(ComputerUtil.canPayCost(ability)) ComputerUtil.playNoStack(ability);
	            }
		  }
		  
	  }
  }
  public int size()
  {
    return stack.size();
  }
  public void push(SpellAbility sp)
  {
    stack.add(0, sp);
    this.updateObservers();
    if(sp.isSpell())
    {
    	
    	//attempt to counter human spell
    	if (sp.getSourceCard().getController().equals(Constant.Player.Human) &&
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
}
