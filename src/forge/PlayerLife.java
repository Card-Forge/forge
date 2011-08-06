/*package forge;
public class PlayerLife extends MyObservable implements java.io.Serializable
{
	private static final long serialVersionUID = -1614695903669967202L;
	
	private String player;
	private Card playerCard;
	private int life;
    private int assignedDamage;//from combat
    
    public void setPlayerCard(Card playerCard)	{	this.playerCard = playerCard; }
	public Card getPlayerCard() 				{	return playerCard; 	}
	public void setAssignedDamage(int n)   		{	assignedDamage = n; }
    public int  getAssignedDamage()        		{	return assignedDamage; }
    
    public PlayerLife(String pl)
    {
    	player = pl;
    	if (player.isHuman())
    		setPlayerCard(AllZone.CardFactory.HumanNullCard);
    	else
    		setPlayerCard(AllZone.CardFactory.ComputerNullCard);
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
    	this.updateObservers();
    }
    public void subtractLife(int life2, Card SourceCard)
    {
    	life -= life2;
    	this.updateObservers();
    }

    public void payLife(int life2)	// change this to subtractLife
    {
    	subtractLife(life2, null);
    }
} */