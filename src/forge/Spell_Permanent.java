
package forge;


public class Spell_Permanent extends Spell {
    

    private static final long serialVersionUID = 2413495058630644447L;
    
    public Spell_Permanent(Card sourceCard) {
        super(sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        
        if(sourceCard.isCreature()) {
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetAttack());
        	sb.append(" / ").append(sourceCard.getNetDefense());
        	setStackDescription(sb.toString());
        }
        else setStackDescription(sourceCard.getName());
        
        setDescription(getStackDescription());
    }//Spell_Permanent()
    
    @Override
    public boolean canPlay() {
        return super.canPlay()
                || (getSourceCard().getKeyword().contains("Flash") && !AllZone.GameAction.isCardInPlay(getSourceCard())
                    && !getSourceCard().isUnCastable());
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
        		CardList cl = list.getType(subtype);
        		
        		 if(cl.size() > 0) {
                     return false;
                 }
        	}
        }
        if(getSourceCard().getType().contains("World")) {
        	CardList list = new CardList(AllZone.Computer_Play.getCards());
        	list = list.getType("World");
        	if(list.size() > 0) return false;
        }
        
        return super.canPlayAI();
    }//canPlayAI()
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
        play.add(c);
    }
}
