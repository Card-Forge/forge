
package forge;


abstract public class Ability_Tap extends SpellAbility implements java.io.Serializable {
    
    private static final long serialVersionUID = 8292723782268822539L;
    
    public Ability_Tap(Card sourceCard) {
        this(sourceCard, "0");
    }
    
    public Ability_Tap(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability_Tap, sourceCard);
        setManaCost(manaCost);
    }
    
    @Override
    public boolean canPlay() {
        Card card = getSourceCard();
        
        CardList Pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		Pithing.add(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
		Pithing = Pithing.getName("Pithing Needle");
		Pithing = Pithing.filter(new CardListFilter() {
			public boolean addCard(Card c){
				return c.getSVar("PithingTarget").equals(c.getName());
			}
		});
		
		if(Pithing.size() != 0) return false;
        
        if(AllZone.GameAction.isCardInPlay(card) && card.isUntapped()) {
            if(card.isFaceDown()) return false;
            if(card.isCreature() == true) {
        		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(getSourceCard().getController().getOpponent()); 		
        		Silence = Silence.getName("Linvala, Keeper of Silence");
        		if(Silence.size() > 0) return false;
        		}
            if(card.isArtifact() && card.isCreature()) return !card.hasSickness();
            
            if(card.isCreature() && (!card.hasSickness())) return true;
            else if(card.isArtifact() || card.isGlobalEnchantment() || card.isLand()) return true;
        }
        return false;
    }
}
