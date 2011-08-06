
package forge;


public class Player extends MyObservable{
	private String name;
	private int poisonCounters;
	private int life;
	private int assignedDamage;
	
	public Player(String myName) {
		this(myName, 20, 0);
	}
	
	public Player(String myName, int myLife, int myPoisonCounters) {
		name = myName;
		life = myLife;
		poisonCounters = myPoisonCounters;
		assignedDamage = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isHuman() {
		return name.equals("Human");
	}
	
	public boolean isComputer() {
		return name.equals("Computer");
	}
	
	public boolean isPlayer(Player p1) {
		return p1.getName().equals(this.name);
	}
	
	public Player getOpponent() {
		if(isPlayer(AllZone.HumanPlayer)) {
			return AllZone.ComputerPlayer;
		}
		else return AllZone.HumanPlayer;
	}
	
	//////////////////////////
	//
	// methods for manipulating life
	//
	//////////////////////////
	
	public boolean setLife(final int newLife) {
		life = newLife;
		this.updateObservers();
		return true;
	}
	
	public int getLife() {
		return life;
	}
	
	public void addLife(final int toAdd) {
		life += toAdd;
		this.updateObservers();
	}
	
	public boolean gainLife(final int toGain) {
		boolean newLifeSet = false;
		if(toGain > 0) {
			addLife(toGain);
			newLifeSet = true;
			this.updateObservers();
		}
		else System.out.println("Player - trying to gain negative or 0 life");
		
		/*
		Object[] Life_Whenever_Parameters = new Object[1];
    	Life_Whenever_Parameters[0] = toGain;
    	AllZone.GameAction.CheckWheneverKeyword(p.getPlayerCard(), "GainLife", Life_Whenever_Parameters);
		*/
		return newLifeSet;
	}
	
	public boolean loseLife(final int toLose) {
		boolean newLifeSet = false;
		if(toLose > 0) {
			life -= toLose;
			newLifeSet = true;
			this.updateObservers();
		}
		else System.out.println("Player - trying to lose positive or 0 life");
		return newLifeSet;
	}
	
	public void subtractLife(final int toSub, final Card c) {
		life -= toSub;
		this.updateObservers();
	}
	
	public void payLife(final int cost) {
		life -= cost;
		this.updateObservers();
	}
	
	public boolean payLife(int lifePayment, Card source) {
    	
    	if (lifePayment <= life){
    		subtractLife(lifePayment, source);
    		return true;
    	}
    	return false;
	}
	
	//////////////////////////
	//
	// methods for handling damage
	//
	//////////////////////////
	
	public void addDamage(int damage, Card source) {
		if (source.getKeyword().contains("Infect")) {
        	addPoisonCounters(damage);
        }
        else {
        	int damageToDo = damage;
        	if(PlayerUtil.worshipFlag(this) && life <= damageToDo) {
        		damageToDo = Math.min(damageToDo, life - 1);
        	}
        	subtractLife(damageToDo,source);
        }
        	
        if(source.getKeyword().contains("Lifelink")) GameActionUtil.executeLifeLinkEffects(source, damage);
        
        CardList cl = CardFactoryUtil.getAurasEnchanting(source, "Guilty Conscience");
        for(Card c:cl) {
            GameActionUtil.executeGuiltyConscienceEffects(source, c, damage);
        }
        
        GameActionUtil.executePlayerDamageEffects(this, source, damage, false);
	}
	
	public void setAssignedDamage(int n)   		{	assignedDamage = n; }
    public int  getAssignedDamage()        		{	return assignedDamage; }
    
    
    
    
    
    public void addCombatDamage(int damage, final Card source) {
    	if (source.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
    			|| source.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME."))
        	damage = 0;
    	if (source.getKeyword().contains("Infect")) {
    		//addPoison(player, damage);
    	}
        else {
        	addDamage(damage, source);
        }
    	
    	//GameActionUtil.executePlayerDamageEffects(player, source, damage, true);
    	GameActionUtil.executePlayerCombatDamageEffects(source);
    	CombatUtil.executeCombatDamageEffects(source);
    }
	
	//////////////////////////
	//
	// methods for handling Poison counters
	//
	//////////////////////////
	
	public void addPoisonCounters(int num) {
		poisonCounters += num;
		this.updateObservers();
	}
	
	public void setPoisonCounters(int num) {
		poisonCounters = num;
		this.updateObservers();
	}
	
	public int getPoisonCounters() {
		return poisonCounters;
	}
	
	public void subtractPoisonCounters(int num) {
		poisonCounters -= num;
		this.updateObservers();
	}
	
	public boolean hasShroud() {
		return false;
	}
	
	
	public boolean canPlaySpells() {
		return true;
	}
	
	public boolean canPlayAbilities() {
		return true;
	}
	
	public CardList getCards(PlayerZone zone) {
		//TODO
		return new CardList();
	}
	
	public String toString() {
		return name;
	}
}