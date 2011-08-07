package forge.card.spellability;

import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Player;
import forge.card.mana.ManaPool;

abstract public class Ability_Mana extends Ability_Activated implements java.io.Serializable {
	private static final long serialVersionUID = -6816356991224950520L;

    private String		origProduced;
    private int			amount = 1;
    protected boolean	reflected = false;
    protected boolean 	undoable = true;
    protected boolean	canceled = false;
    
    public Ability_Mana(Card sourceCard, String parse, String produced) {
    	this(sourceCard, parse, produced, 1);
    }
    
    public Ability_Mana(Card sourceCard, String parse, String produced, int num) {
    	this(sourceCard, new Cost(parse, sourceCard.getName(), true), produced, num);
    }
    
    public Ability_Mana(Card sourceCard, Cost cost, String produced) {
        this(sourceCard, cost, produced, 1);
    }
    
    public Ability_Mana(Card sourceCard, Cost cost, String produced, int num) {
        super(sourceCard, cost, null);

        origProduced = produced;
        amount = num;
    }

    @Override
    public boolean canPlayAI() {
        return false;
    }    

	@Override
	public void resolve() {
		produceMana();
	}
	
	public void produceMana(){
		StringBuilder sb = new StringBuilder();
		if (amount == 0)
			sb.append("0");
		else{
			try{
				// if baseMana is an integer(colorless), just multiply amount and baseMana
				int base = Integer.parseInt(origProduced);
				sb.append(base*amount);
			}
			catch(NumberFormatException e){
				for(int i = 0; i < amount; i++){
					if (i != 0)
						sb.append(" ");
					sb.append(origProduced);
				}
			}
		}
		produceMana(sb.toString(), this.getSourceCard().getController());
	}
	
	public void produceMana(String produced, Player player){
		final Card source = this.getSourceCard();
		ManaPool manaPool;
		if (player.isHuman())
			manaPool = AllZone.ManaPool;
		else manaPool = AllZone.Computer_ManaPool;
		// change this, once ManaPool moves to the Player
		// this.getActivatingPlayer().ManaPool.addManaToFloating(origProduced, getSourceCard());
		manaPool.addManaToFloating(produced, source);

		// TODO: all of the following would be better as trigger events "tapped for mana"		
        if(source.getName().equals("Rainbow Vale")) {
        	this.undoable = false;
        	source.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
        }
        
        if (source.getName().equals("Undiscovered Paradise")) {
        	this.undoable = false;
        	// Probably best to conver this to an Extrinsic Ability
        	source.setBounceAtUntap(true);
        }
        
        //Run triggers        
        HashMap<String,Object> runParams = new HashMap<String,Object>();

        runParams.put("Card", source);
        runParams.put("Player", AllZone.HumanPlayer);
        runParams.put("Ability_Mana", this);
        runParams.put("Produced", produced);
        AllZone.TriggerHandler.runTrigger("TapsForMana", runParams);
        
	}//end produceMana(String)
	
	public String mana() { return origProduced; }
	public void setMana(String s) { origProduced = s; }
	public void setReflectedMana(boolean bReflect) { reflected = bReflect; }
	
	public boolean isSnow() { return this.getSourceCard().isSnow(); }
	public boolean isSacrifice() { return this.getPayCosts().getSacCost(); }
	public boolean isReflectedMana() { return reflected; }
	
	public boolean canProduce(String s) { return origProduced.contains(s); }
	
	public boolean isBasic(){
		if (origProduced.length() != 1)
			return false;
		
		if (amount > 1)
			return false;
		
		return true;
	}

	public boolean isUndoable() { return undoable && getPayCosts().isUndoable() && AllZoneUtil.isCardInPlay(getSourceCard()); }
	public void setUndoable(boolean bUndo) { undoable = bUndo; }
	
	public void setCanceled(boolean bCancel) { canceled = bCancel; }
	public boolean getCanceled() { return canceled; }
	
	public void undo(){
		if (isUndoable()){
			getPayCosts().refundPaidCost(getSourceCard());
		}
	}
	
    @Override
    public boolean equals(Object o)
    {
    	//Mana abilities with same Descriptions are "equal"
    	if(o == null)
    		return false;
    	return  o.toString().equals(this.toString());
    }
    
}//end class Ability_Mana

