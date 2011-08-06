//TODO: make this class a type of Ability
abstract public class Ability_Hand extends SpellAbility implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Ability_Hand(Card sourceCard)
    {
	this(sourceCard, "");
    }
    public Ability_Hand(Card sourceCard, String manaCost)
    {
	super(SpellAbility.Ability, sourceCard);
	setManaCost(manaCost);
    }
    public boolean canPlay()
    {
	PlayerZone zone = AllZone.getZone(getSourceCard());
	return zone.is(Constant.Zone.Hand, getSourceCard().getController());
    }
}