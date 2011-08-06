package forge;
import java.util.*;

abstract public class Ability_Mana extends SpellAbility implements java.io.Serializable
{
	private ArrayList<Command> runcommands = new ArrayList<Command>();
	public String orig;
	private String Mana;
	private Card sourceCard;
	
	public boolean isBasic()
	{
		return(orig.length() ==10 && orig.startsWith("tap: add ") && "1WBURG".contains(""+orig.charAt(9)));
	}
	private static final long serialVersionUID = 8292723782268822439L;
	/*public Ability_Mana(Card sourceCard, String mana)
    {
   this(sourceCard, "0");
    }*/
   public boolean isSnow(){return getSourceCard().isSnow();}//override?
   public boolean isTapAbility(){return isTapAbility(orig);}
   private static boolean isTapAbility(String orig){
      String cost = orig.split(":")[0];
      cost = cost.replaceAll("tap", "T");
      return (cost.contains("T"));
   }
   
   public Ability_Mana(Card sourceCard, String orig)
   {
	   super(isTapAbility(orig) ? SpellAbility.Ability_Tap : SpellAbility.Ability, sourceCard);
	   
	   /*
	   if (orig.contains("$"))
	   {
		   String[] k = orig.split(":");
		   String mana = k[1];
		   int count = CardFactoryUtil.xCount(sourceCard, k[2]);
		   StringBuilder sb = new StringBuilder();
		   sb.append("tap: add ");
		   for (int i=0;i<count;i++)
			   sb.append(mana);
		   orig = sb.toString();
	   }
	   */
	   
	   this.sourceCard = sourceCard;
	   this.orig=orig;
	   setDescription(orig);
	  
	   /*
	   String parts[] = orig.split(":");
	   System.out.println("0:" +parts[0]);
	   System.out.println("1:" +parts[1]);
	   StringBuilder sb = new StringBuilder();
	   
	   sb.append(parts[0]);
	   sb.append(parts[1]);
	   sb.append(" to your mana pool for each ");
	   
	   setDescription(sb.toString());
	   */
	   
	   if(isBasic())//lowers memory usage drastically
	   {
		   Mana = "" + orig.charAt(9);
		   setManaCost("0");
		   return;
	   }
	   String[] parts = orig.split(":");
	   Mana=parts[1];
	   Mana = Mana.replaceAll(" add ", "");
	   setStackDescription("Add "+ Mana +" to your mana pool.");
	   Mana = Mana.replaceAll(" ", "");
	   
	   String cost=parts[0];
	   cost = cost.replaceAll("tap", "T");
	   //cost = cost.replaceAll("T, ", "");
	   setManaCost(cost.replaceAll("T", "").split(",")[0]);
	   if (getManaCost().equals("")) setManaCost("0");   
	   //pain lands
	    ArrayList<String> pain = new ArrayList<String>();
	    pain.add("Battlefield Forge");
	    pain.add("Caves of Koilos");
	    pain.add("Llanowar Wastes");
	    pain.add("Shivan Reef");
	    pain.add("Yavimaya Coast");
	    pain.add("Adarkar Wastes");
	    pain.add("Brushland");
	    pain.add("Karplusan Forest");
	    pain.add("Underground River");
	    pain.add("Sulfurous Springs");
	    if(pain.contains(sourceCard.getName()) && !Mana.equals("1"))
	    runcommands.add(new Command()
	    {
			private static final long serialVersionUID = -5904507275105961979L;
	
			public void execute(){
	
	    		AllZone.GameAction.getPlayerLife(getController()).subtractLife(1);
	    }});
		//parseCosts();
		/*for(String subcost : cost.split(","))
		{
			subcost.trim();
			if (subcost.equals("") || subcost.equals("T") ||
					subcost.equals(getManaCost())) continue;
			if (subcost.startsWith("Sacrifice a")){
				String sactype= subcost.substring(12).trim();//will remove both "Sac a " and "Sac an" :P
				Input bpM_old = getBeforePayMana();
			}
		}*/
    }
    public boolean equals(Object o)//Mana abilities with equal descriptions are considered equal, please take this into account
      {return (o instanceof Ability_Mana ? ((Ability_Mana)o).orig.equals(orig) : (o instanceof String ? o.toString().equals(orig) : false )) ;}
    public boolean equalsIgnoreMana(Ability_Mana ma)
    {
    	String noManaDesc=orig.substring(0,orig.indexOf(Mana));
    	noManaDesc+=orig.substring(orig.indexOf(Mana)+Mana.length());
    	String noManaDesc_2=ma.orig.substring(0,ma.orig.indexOf(Mana));
    	noManaDesc_2+=ma.orig.substring(ma.orig.indexOf(Mana)+Mana.length());
    	return noManaDesc.equals(noManaDesc_2);
    }
    
    
    public String toString(){return orig;}
    boolean undoable=false;
    public void undo(){getSourceCard().untap();}//override undo() and set undoable for custom undo costs
    public boolean undoable(){return isBasic() || undoable;}
    public void parseCosts(){parseCosts(orig);}
    private void parseCosts(String orig)
    {
    	setBeforePayMana(null);// reset everything
    	setAfterPayMana(null);
    	setAfterResolve(null);
    	
    	String[] split={",",":","."};//i.e. "cost,cost,cost<...>:Effect.Effect.Effect.
    	String copy=orig+"";
    	for(String s : split)
    	{
	    	copy=copy.replaceAll(" " + s, s); //trim splitter whitespace
	    	copy=copy.replaceAll(s + " ", s);
    	}
    	copy=copy.replaceAll("T"+split[0], "");//i.e. "T,"-> ""(reason for this is that tapCost is already determined)
    	copy=copy.trim();
    	if (!("123456789GWURBXY(".contains(copy.charAt(0)+"")))
    	{
    		setManaCost("0");
    		copy=split[0] + copy;
    	}//Hacky, but should work
    	else setManaCost(copy.substring(0, copy.indexOf(",")));
    	String divider = ",";
    	while(!copy.trim().equals(""))
    	{
    		if(copy.startsWith(":"))
    			if (!divider.equals(".")) divider=".";
    		while (copy.startsWith(":") || copy.startsWith(divider) ) copy=copy.substring(1);
    		String current=copy.substring(0, copy.indexOf(","));
    		/*if (current.startsWith("Sacrifice a")){
    			
    		}*/
    		copy=copy.substring(current.length());
    	}
    }
    public void resolve(){
       AllZone.ManaPool.addMana(this);
         if(!runcommands.isEmpty())
         for(Command c : runcommands) c.execute();
    }
    public int getX(){return getSourceCard().getX();}//override these when not defined by card,
    public void setX(int X){getSourceCard().setX(X);}//i.e. "T, remove X charge counters from {name}: add X+1 <color> mana to your mana pool"
    public String Mana(){
    	if(!orig.contains("for each"))
    		return Mana;
        else
        {
        	String[] manaPart = orig.split(":");
        	String m = manaPart[1];
        	m = m.replaceAll(" add ", "");
        	//TODO: make this handle "multiple-mana symbol" cases, if they are ever needed
        	m = m.substring(0, 2);
        	
        	String[] parts = orig.split(" for each ");
        	int index = parts[1].indexOf(' ');
        	String s1 = parts[1].substring(0, index);
        	String s2 = parts[1].substring(index);

        	if (s2.equals(" on the battlefield."))
        		s2 = "TypeOnBattlefield";
        	else if (s2.equals(" you control."))
        		s2 = "TypeYouCtrl";
        	
        	StringBuilder countSB = new StringBuilder();
        	countSB.append("Count$");
        	countSB.append(s2);
        	countSB.append(".");
        	countSB.append(s1);
        	
        	int count = CardFactoryUtil.xCount(sourceCard, countSB.toString());

        	StringBuilder sb = new StringBuilder();
        	for (int i=0;i<count;i++)
      		   sb.append(m);
        	return sb.toString();
          
        }
    	
    }//override for all non-X variable mana,
    public String getController(){return getSourceCard().getController();}
   
    public boolean canPlayAI(){return false;}
    public boolean canPlay()
    {
   Card card = getSourceCard();
   if(AllZone.GameAction.isCardInPlay(card) &&(!isTapAbility() || card.isUntapped()))
   {
      if(card.isFaceDown())
         return false;
      
        if(card.isArtifact() && card.isCreature())
           return !(card.hasSickness() && isTapAbility());

       if(card.isCreature() && (!card.hasSickness() || !isTapAbility()))
    	   return true;
       //Dryad Arbor, Mishra's Factory, Mutavault, ...
       else if (card.isCreature() && card.isLand() && card.hasSickness())
    	   return false;
       else if(card.isArtifact() || card.isGlobalEnchantment() || card.isLand())
    	   return true;
   }
   return false;
    }
}