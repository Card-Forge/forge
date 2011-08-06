public class PlayerLife extends MyObservable implements java.io.Serializable
{
	private static final long serialVersionUID = -1614695903669967202L;
	
	private int life;
    private int assignedDamage;//from combat
    
    public void setAssignedDamage(int n)
    {assignedDamage = n;}
    public int  getAssignedDamage()
    {return assignedDamage;}
    
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
    public void subtractLife(int life2)
    {
	life -= life2;
	this.updateObservers();
    }
}