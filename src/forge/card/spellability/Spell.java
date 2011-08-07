package forge.card.spellability;
import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Phase;
import forge.error.ErrorViewer;


abstract public class Spell extends SpellAbility implements java.io.Serializable, Cloneable {
    
    private static final long serialVersionUID = -7930920571482203460L;
    
    public Spell(Card sourceCard) {
        super(SpellAbility.Spell, sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        setStackDescription(sourceCard.getSpellText());
        getRestrictions().setZone(Constant.Zone.Hand);
    }
    
    public Spell(Card sourceCard, Cost abCost, Target abTgt) {
        super(SpellAbility.Spell, sourceCard);
        
        setManaCost(sourceCard.getManaCost());

        setPayCosts(abCost);
        setTarget(abTgt);
        setStackDescription(sourceCard.getSpellText());
        getRestrictions().setZone(Constant.Zone.Hand);
    }
    
    @Override
    public boolean canPlay() {
    	if(AllZone.Stack.isSplitSecondOnStack()) return false;
    	
    	Card card = getSourceCard();
        
        if (card.isUnCastable())
        	return false;
        
        if (payCosts != null)
        	if  (!Cost_Payment.canPayAdditionalCosts(payCosts, this))
        		return false;
        
    	if (!this.getRestrictions().canPlay(card, this))
    		return false;
        
        return (card.isInstant() || card.hasKeyword("Flash") || Phase.canCastSorcery(card.getController()));
    }//canPlay()
    
    @Override
    public boolean canPlayAI() {
    	Card card = getSourceCard();
    	if (card.getSVar("NeedsToPlay").length() > 0) {
    		String needsToPlay = card.getSVar("NeedsToPlay");
    		CardList list = AllZoneUtil.getCardsInPlay();
	
    		list = list.getValidCards(needsToPlay.split(","), card.getController(), card);
    		if (list.isEmpty()) return false;
    	}     

    	return super.canPlayAI();
    }
    
    @Override
    public String getStackDescription() {
        return super.getStackDescription();
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Spell : clone() error, " + ex);
        }
    }
}
