abstract public class Spell extends SpellAbility implements java.io.Serializable, Cloneable {
    
    private static final long serialVersionUID = -7930920571482203460L;
    
    public Spell(Card sourceCard) {
        super(SpellAbility.Spell, sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        setStackDescription(sourceCard.getSpellText());
        
    }
    
    @Override
    public boolean canPlay() {
        return canPlay(getSourceCard());
    }//canPlay()
    
    @Override
    public String getStackDescription() {
        return super.getStackDescription();
        
//      return getSourceCard().getName() +" - " + super.getStackDescription();
    }
    
    public static boolean canPlay(Card card) {
        String controller = card.getController();
        
        String phase = AllZone.Phase.getPhase();
        String activePlayer = AllZone.Phase.getActivePlayer();
        PlayerZone zone = AllZone.getZone(card);
        
        if(card.isInstant()) return true;
        else if((phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2))
                && controller.equals(activePlayer) && AllZone.Stack.size() == 0 && zone.is(Constant.Zone.Hand)) {
            return true;
        }
        return false;
    }//canPlay(Card)
    
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
