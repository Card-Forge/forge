package forge;
public class PlayerLife extends MyObservable implements java.io.Serializable
{
	private static final long serialVersionUID = -1614695903669967202L;
	
	private int life;
    private int assignedDamage;//from combat
    
    public void setAssignedDamage(int n)   {assignedDamage = n;}
    public int  getAssignedDamage()        {return assignedDamage;}
    
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
    	Card WhoGainedLife = new Card();
    	if(AllZone.Human_Life.getLife() != AllZone.Computer_Life.getLife()) {
    	if(AllZone.Human_Life.getLife() == life) WhoGainedLife = AllZone.CardFactory.HumanNullCard;
    	else WhoGainedLife = AllZone.CardFactory.ComputerNullCard;
    	}
    	life += life2;
    	if(WhoGainedLife != AllZone.CardFactory.HumanNullCard && WhoGainedLife != AllZone.CardFactory.ComputerNullCard) {
        if(AllZone.Human_Life.getLife() == life) WhoGainedLife = AllZone.CardFactory.HumanNullCard;
        else WhoGainedLife = AllZone.CardFactory.ComputerNullCard;	
    	}
    	Object[] Life_Whenever_Parameters = new Object[1];
    	Life_Whenever_Parameters[0] = life2;
    	AllZone.GameAction.CheckWheneverKeyword(WhoGainedLife, "GainLife", Life_Whenever_Parameters);
    	this.updateObservers();
    }
    public void subtractLife(int life2)
    {
    	life -= life2;
    	this.updateObservers();
    }
}