package forge;
public class PlayerLife extends MyObservable implements java.io.Serializable
{
	private static final long serialVersionUID = -1614695903669967202L;
	
	private String player;
	private Card playerCard;
	private int life;
    private int assignedDamage;//from combat
    
    public void setAssignedDamage(int n)   {assignedDamage = n;}
    public int  getAssignedDamage()        {return assignedDamage;}
    
    public PlayerLife(String pl)
    {
    	player = pl;
    	if (player.equals(Constant.Player.Human))
    		playerCard = AllZone.CardFactory.HumanNullCard;
    	else
    		playerCard = AllZone.CardFactory.ComputerNullCard;
    }
    
    public int getLife()
    {
    	return life;
    }
    public void setLife(int life2)
    {
    	life = life2;
    	this.updateObservers();
    }
    
    public void addLife(int life2)
    {
    	life += life2;
    	Object[] Life_Whenever_Parameters = new Object[1];
    	Life_Whenever_Parameters[0] = life2;
    	AllZone.GameAction.CheckWheneverKeyword(playerCard, "GainLife", Life_Whenever_Parameters);
    	this.updateObservers();
    }
    public void subtractLife(int life2, Card SourceCard)
    {
    	life -= life2;
    	//unnecessary (I think?), since the addDamage methods in GameAction already check this:
    	/*
    	Object[] DealsDamage_Whenever_Parameters = new Object[3];
    	DealsDamage_Whenever_Parameters[0] = player;
    	DealsDamage_Whenever_Parameters[2] = SourceCard;
    	AllZone.GameAction.CheckWheneverKeyword(playerCard, "DealsDamage", DealsDamage_Whenever_Parameters);
    	*/
    	this.updateObservers();
    }

    public void payLife(int life2)
    {
    	life -= life2;
    	this.updateObservers();
    }
}