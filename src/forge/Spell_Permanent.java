
package forge;


public class Spell_Permanent extends Spell {
    

    private static final long serialVersionUID = 2413495058630644447L;
    
    public Spell_Permanent(Card sourceCard) {
        super(sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        
        if(sourceCard.isCreature()) setStackDescription(sourceCard.getName() + " - Creature "
                + sourceCard.getNetAttack() + " / " + sourceCard.getNetDefense());
        else setStackDescription(sourceCard.getName());
        
        setDescription(getStackDescription());
    }//Spell_Permanent()
    
    @Override
    public boolean canPlay() {
        return super.canPlay()
                || (getSourceCard().getKeyword().contains("Flash") && !AllZone.GameAction.isCardInPlay(getSourceCard()));
    }
    
    @Override
    public boolean canPlayAI() {
        //check on legendary crap
        if(getSourceCard().getType().contains("Legendary")) {
        	CardList list = new CardList(AllZone.Computer_Play.getCards());
            if (list.containsName(getSourceCard().getName()) /*&&
            	!getSourceCard().getName().equals("Flagstones of Trokair")*/)
            	return false;
        }
        if(getSourceCard().getType().contains("Planeswalker")) {
        	CardList list = new CardList(AllZone.Computer_Play.getCards());
        	list = list.getType("Planeswalker");
        	
        	Card c = getSourceCard();
        	for (int i=0;i<list.size();i++)
        	{
        		String subtype = c.getType().get(c.getType().size() - 1);
        		CardList cl = AllZone.GameAction.getPlaneswalkerSubtype(list, subtype, c);
        		
        		 if(cl.size() > 0) {
                     return false;
                 }
        	}
        }
        
        return true;
    }//canPlayAI()
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
        play.add(c);
    }
}
