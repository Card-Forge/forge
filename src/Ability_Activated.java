abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Ability_Activated(Card sourceCard)
    { 
	this(sourceCard, "");
    }
    public Ability_Activated(Card sourceCard, String manaCost)
    {
	super(SpellAbility.Ability, sourceCard);
	setManaCost(manaCost);
    }    
    public boolean canPlay()
    {
	Card c = getSourceCard();
	
	return AllZone.GameAction.isCardInPlay(c) && !c.isFaceDown();
	    //TODO: make sure you can't play the Computer's activated abilities
    }
}    
