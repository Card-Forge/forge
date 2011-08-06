package forge;
import forge.error.ErrorViewer;


abstract public class Spell extends SpellAbility implements java.io.Serializable, Cloneable {
    
    private static final long serialVersionUID = -7930920571482203460L;
    
    public Spell(Card sourceCard) {
        super(SpellAbility.Spell, sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        setStackDescription(sourceCard.getSpellText());
    }
    
    public Spell(Card sourceCard, Ability_Cost abCost, Target abTgt) {
        super(SpellAbility.Spell, sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        //abCost.setMana(getManaCost());
        setPayCosts(abCost);
        setTarget(abTgt);
        setStackDescription(sourceCard.getSpellText());
    }
    
    @Override
    public boolean canPlay() {
    	Card card = getSourceCard();
        PlayerZone zone = AllZone.getZone(card);
        
        if (card.isUnCastable())
        	return false;
        
        if (payCosts != null)
        	if  (!Cost_Payment.canPayAdditionalCosts(payCosts, this))
        		return false;
        
        return ((card.isInstant() || Phase.canCastSorcery(card.getController())) && zone.is(Constant.Zone.Hand));
    }//canPlay()
    
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
