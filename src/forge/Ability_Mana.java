
package forge;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class Ability_Mana extends SpellAbility implements java.io.Serializable {
    private ArrayList<Command> runcommands = new ArrayList<Command>();
    public String              orig;
    private String             Mana;
    private Card               sourceCard;
    private boolean            reflectedMana = false;	   
    
    public boolean isBasic() {
        return (orig.length() == 10 && orig.startsWith("tap: add ") && "1WBURG".contains("" + orig.charAt(9)));
    }
    
    public boolean isSacrifice()
    {
    	return orig.contains("Sacrifice CARDNAME: Add ");
    }
    
    public boolean isSacrificeAny()
    {
    	return (orig.contains("Sacrifice a") && orig.contains(": Add"));
    }
    
    public boolean isUndoableMana() {
    	// isBasic, plus "2" so Sol Ring is counted
    	return (orig.length() == 10 && orig.startsWith("tap: add ") && "21WBURG".contains("" + orig.charAt(9)));
    }
    
    private static final long serialVersionUID = 8292723782268822439L;
    
    /*public Ability_Mana(Card sourceCard, String mana)
    {
    this(sourceCard, "0");
    }*/
    
    public void setReflectedMana(boolean b) {
    	this.reflectedMana = b;
    }
    public boolean isReflectedMana() {
    	return (this.reflectedMana);
    }
    public boolean isSnow() {
        return getSourceCard().isSnow();
    }//override?
    
    @Override
    public boolean isTapAbility() {
        return isTapAbility(orig);
    }
    
    private static boolean isTapAbility(String orig) {
        String cost = orig.split(":")[0];
        cost = cost.replaceAll("Tap", "tap").replaceAll("tap", "T");
        return (cost.contains("T"));
    }
    
    public Ability_Mana(Card sourceCard, String orig) {
        super(isTapAbility(orig)? SpellAbility.Ability_Tap:SpellAbility.Ability, sourceCard);
                
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
        this.orig = (sourceCard.getName().length() == 0? orig:orig.replaceAll(sourceCard.getName(), "CARDNAME"));
        setDescription(orig);
        
        /*
        if (sourceCard.getName().equals("Black Lotus"))
        	System.out.println("BLACK LOTUS!");
        */
        
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
        
        if (sourceCard.getName().equals("Forbidden Orchard"))
        {
        	final Card crd = sourceCard;
        	runcommands.add(new Command() {
				private static final long serialVersionUID = 1365329719980281985L;

				public void execute()
        		{
					//  The computer can now use this card. A version of the 
					//  line of code below was added to ComputerUtil.payManaCost()
        			AllZone.Stack.add(CardFactoryUtil.getForbiddenOrchardAbility(crd, AllZone.ComputerPlayer));
        		}
        	});
        }
        
        if (sourceCard.getName().equals("Undiscovered Paradise"))
        {
        	final Card crd = sourceCard;
        	runcommands.add(new Command() {
				private static final long serialVersionUID = 1365329719980281985L;

				public void execute()
        		{
					//  The computer can now use this card. A version of the 
					//  line of code below was added to ComputerUtil.payManaCost()
        			crd.setBounceAtUntap(true);
        		}
        	});
        }

        if(isBasic())//lowers memory usage drastically
        {
            Mana = "" + orig.charAt(9);
            setManaCost("0");
            return;
        }
        else if (isSacrifice() || isSacrificeAny())
        {
        	String regex = "[0-9]+,";
        	Pattern pattern = Pattern.compile(regex); 
        	Matcher matcher = pattern.matcher(orig); 
        	
        	if (orig.startsWith("Sacrifice ") || orig.startsWith("tap, Sacrifice "))
        		setManaCost("0");
        	else if(matcher.find()) 
        		setManaCost(matcher.group().substring(0, matcher.group().length()-1));
        		
        	return;
        }
        String[] parts = orig.split(":");
        Mana = parts[1];
        Mana = Mana.replaceAll(" add ", "");
        //Mana = Mana.replaceAll(" ", ""); // sol - removed for new manapool compatibility
        
        String cost = parts[0];
        cost = cost.replaceAll("Tap", "tap").replaceAll("tap", "T");
        //cost = cost.replaceAll("T, ", "");
        setManaCost(cost.replaceAll("T", "").split(",")[0]);
        if(getManaCost().equals("")) setManaCost("0");
        /**
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
        if(pain.contains(sourceCard.getName()) && !Mana.equals("1")) runcommands.add(new Command() {
            private static final long serialVersionUID = -5904507275105961979L;
            
            public void execute() {
                
                AllZone.GameAction.getPlayerLife(getController()).subtractLife(1);
            }
        });
        **/
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
    
    @Override
    public boolean equals(Object o)//Mana abilities with equal descriptions are considered equal, please take this into account
    {
        return (o instanceof Ability_Mana? ((Ability_Mana) o).orig.equals(orig):(o instanceof String? o.toString().equals(
                orig):false));
    }
    
    public boolean equalsIgnoreMana(Ability_Mana ma) {
        String noManaDesc = orig.substring(0, orig.indexOf(Mana));
        noManaDesc += orig.substring(orig.indexOf(Mana) + Mana.length());
        String noManaDesc_2 = ma.orig.substring(0, ma.orig.indexOf(Mana));
        noManaDesc_2 += ma.orig.substring(ma.orig.indexOf(Mana) + Mana.length());
        return noManaDesc.equals(noManaDesc_2);
    }
    
    
    @Override
    public String toString() {
        return orig;
    }
    
    boolean undoable = false;
    
    public void undo() {
        getSourceCard().untap();
    }//override undo() and set undoable for custom undo costs
    
    public boolean undoable() {
        return isUndoableMana() || undoable;
    }
    
    public void parseCosts() {
        parseCosts(orig);
    }
    
    private void parseCosts(String orig) {
        setBeforePayMana(null);// reset everything
        setAfterPayMana(null);
        setAfterResolve(null);
        
        String[] split = {",", ":", "."};//i.e. "cost,cost,cost<...>:Effect.Effect.Effect.
        String copy = orig + "";
        for(String s:split) {
            copy = copy.replaceAll(" " + s, s); //trim splitter whitespace
            copy = copy.replaceAll(s + " ", s);
        }
        copy = copy.replaceAll("T" + split[0], "");//i.e. "T,"-> ""(reason for this is that tapCost is already determined)
        copy = copy.trim();
        if(!("123456789GWURBXY(".contains(copy.charAt(0) + ""))) {
            setManaCost("0");
            copy = split[0] + copy;
        }//Hacky, but should work
        else setManaCost(copy.substring(0, copy.indexOf(",")));
        String divider = ",";
        while(!copy.trim().equals("")) {
            if(copy.startsWith(":")) if(!divider.equals(".")) divider = ".";
            while(copy.startsWith(":") || copy.startsWith(divider))
                copy = copy.substring(1);
            String current = copy.substring(0, copy.indexOf(","));
            /*if (current.startsWith("Sacrifice a")){
            	
            }*/
            copy = copy.substring(current.length());
        }
    }
    
    @Override
    public void resolve() {
    	if (isSacrifice())
    		AllZone.GameAction.sacrifice(sourceCard);
    	AllZone.ManaPool.addMana(this);
    	
    	// Nirkana Revenant Code
    	CardList Nirkana_Human = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer, "Nirkana Revenant");
        if(Nirkana_Human.size() > 0 && sourceCard.getType().contains("Swamp") && sourceCard.getController().isHuman()) {
        	for(int i = 0; i < Nirkana_Human.size(); i++) {
        		AllZone.ManaPool.addManaToFloating("B", Nirkana_Human.get(i));	
        	}
        } 

    	// High Tide Code
        if(Phase.HighTideCount > 0 && sourceCard.getType().contains("Island") && sourceCard.getController().isHuman()) {
        	for(int i = 0; i < Phase.HighTideCount; i++) {
        		AllZone.ManaPool.addManaToFloating("U", sourceCard);	
        	}
        }
        
        //Manabarbs code
        if(sourceCard.isLand() && this.isTapAbility()) {
        	CardList barbs = AllZoneUtil.getCardsInPlay("Manabarbs");
        	for(Card barb:barbs) {
        		final Card manabarb = barb;
        		SpellAbility ability = new Ability(manabarb, "") {
        			@Override
        			public void resolve() {
        				sourceCard.getController().addDamage(1, manabarb);
        			}
        		};
        		ability.setStackDescription(manabarb.getName()+" - deal 1 damage to "+sourceCard.getController());
        		AllZone.Stack.add(ability);
        	}
        }

        if(!runcommands.isEmpty()) for(Command c:runcommands)
            c.execute();
    }
    
    public int getX() {
        return getSourceCard().getX();
    }//override these when not defined by card,
    
    public void setX(int X) {
        getSourceCard().setX(X);
    }//i.e. "T, remove X charge counters from {name}: add X+1 <color> mana to your mana pool"
    
    public String mana() {
    	if (orig.contains("for each")) {
            /*String[] manaPart = orig.split(": add ");
            String m = manaPart[1];
            m = m.replaceAll(" add ", "");
            //TOhaveDOne: make this handle "multiple-mana symbol" cases, if they are ever needed
            m = m.substring(0, 2);*/
            String m = orig.split(": add ")[1].split(" to ")[0];
            
            String[] parts = orig.split(" for each ");
            int index = parts[1].indexOf(' ');
            String s1 = parts[1].substring(0, index);
            String s2 = parts[1].substring(index);
            
            if(s2.equals(" on the battlefield.")) s2 = "TypeOnBattlefield";
            else if(s2.equals(" you control.")) s2 = "TypeYouCtrl";
            
            StringBuilder countSB = new StringBuilder();
            countSB.append("Count$");
            countSB.append(s2);
            countSB.append(".");
            countSB.append(s1);
            
            int count = CardFactoryUtil.xCount(sourceCard, countSB.toString());
            
            StringBuilder sb = new StringBuilder();
            
            if(count == 0) sb.append("0");
            
            for(int i = 0; i < count; i++){
            	if (i != 0)
                    sb.append(" "); 	// added a space here to play nice with the new ManaPool
                sb.append(m);
            } 
            return sb.toString();
            
        } 
    	else if ((orig.contains("Sacrifice this creature: Add ") || orig.contains("Sacrifice CARDNAME: Add "))
    		   && orig.contains(" to your mana pool."))
    	{
    		String m = orig.split(": Add ")[1].split(" to ")[0];
    		return m;
    	}
    	else if( orig.contains("X")) {
    		int amt = CardFactoryUtil.xCount(sourceCard, sourceCard.getSVar("X"));
    		return Integer.toString(amt);
    	}
    	else {
        	return Mana;
        }
        
    }//override for all non-X variable mana,
    
    public Player getController() {
        return getSourceCard().getController();
    }
    
    @Override
    public boolean canPlayAI() {
        return false;
    }
    
    @Override
    public boolean canPlay() {
        Card card = getSourceCard();
		 if(card.isCreature() == true) {
    		 
	 			CardList Silence = AllZoneUtil.getPlayerCardsInPlay(card.getController().getOpponent()); 	
	     		Silence = Silence.getName("Linvala, Keeper of Silence");
	     		if(Silence.size() > 0) return false;
	     		}
        if(AllZone.GameAction.isCardInPlay(card)
                & !((isTapAbility() && card.isTapped()) || (isUntapAbility() && card.isUntapped()))) {
            if(card.isFaceDown()) return false;
            
            if(card.isArtifact() && card.isCreature()) return !(card.hasSickness() && (isTapAbility() || isUntapAbility()));
            
            if(card.isCreature() && !(card.hasSickness() && (isTapAbility() || isUntapAbility()))) return true;
            //Dryad Arbor, Mishra's Factory, Mutavault, ...
            else if(card.isCreature() && card.isLand() && card.hasSickness()) return false;
            else if(card.isArtifact() || card.isGlobalEnchantment() || card.isLand()) return true;
        }
        return false;
    }


}

