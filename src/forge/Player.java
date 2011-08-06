
package forge;


public abstract class Player extends MyObservable{
	protected String name;
	protected int poisonCounters;
	protected int life;
	protected int assignedDamage;
	
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
	
	public abstract boolean isHuman();
	public abstract boolean isComputer();
	public abstract boolean isPlayer(Player p1);
	
	public abstract Player getOpponent();
	
	//////////////////////////
	//
	// methods for manipulating life
	//
	//////////////////////////
	
	public boolean setLife(final int newLife) {
		boolean change = false;
		//rule 118.5
		if(life > newLife) {
			change = loseLife(life - newLife);
		}
		else if(newLife > life) {
			change = gainLife(newLife - life);
		}
		else {
			//life == newLife
			change = false;
		}
		this.updateObservers();
		return change;
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
		if(!canGainLife()) return false;
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
	
	public boolean canGainLife() {
		return true;
	}
	
	public boolean loseLife(final int toLose) {
		boolean newLifeSet = false;
		if(!canLoseLife()) return false;
		if(toLose > 0) {
			life -= toLose;
			newLifeSet = true;
			this.updateObservers();
		}
		else System.out.println("Player - trying to lose positive or 0 life");
		return newLifeSet;
	}
	
	public boolean canLoseLife() {
		return true;
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
	
	public void addDamage(final int damage, final Card source) {
		int damageToDo = damage;
		
		if( reducePlayerDamageToZero(source, false) )
        	damageToDo = 0;
		
		if( source.getKeyword().contains("Infect") ) {
        	addPoisonCounters(damage);
        }
        else {
        	if(PlayerUtil.worshipFlag(this) && life <= damageToDo) {
        		damageToDo = Math.min(damageToDo, life - 1);
        	}
        	subtractLife(damageToDo, source);
        }
        	
        if(source.getKeyword().contains("Lifelink")) GameActionUtil.executeLifeLinkEffects(source, damage);
        
        CardList cl = CardFactoryUtil.getAurasEnchanting(source, "Guilty Conscience");
        for(Card c:cl) {
            GameActionUtil.executeGuiltyConscienceEffects(source, c, damageToDo);
        }
        
        GameActionUtil.executePlayerDamageEffects(this, source, damageToDo, false);
	}
	
	private boolean reducePlayerDamageToZero(final Card source, final boolean isCombat) {
		boolean reduce = false;
    	if(isCombat) {
    		//for future use
    	}
    	reduce = reduce || source.getKeyword().contains("Prevent all damage that would be dealt to and dealt by CARDNAME.");
		reduce = reduce || source.getKeyword().contains("Prevent all damage that would be dealt by CARDNAME.");
		
		//Spirit of Resistance
		if(AllZoneUtil.isCardInPlay("Spirit of Resistance", this)) {
			if( AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Black).size() > 0
					&& AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Blue).size() > 0
					&& AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Green).size() > 0
					&& AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Red).size() > 0
					&& AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.White).size() > 0) {
				reduce = true;
			}
		}
		return reduce;
	}
	
	public void setAssignedDamage(int n)   		{	assignedDamage = n; }
    public int  getAssignedDamage()        		{	return assignedDamage; }
    
    public void addCombatDamage(final int damage, final Card source) {
    	int damageToDo = damage;
    	if (source.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
    			|| source.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME."))
        	damageToDo = 0;
        addDamage(damageToDo, source);
    	
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
	
	
	
	////////////////////////////////
	//
	// generic Object overrides
	//
	/////////////////////////////////
	
	@Override
	public boolean equals(Object o) {
		Player p1 = (Player)o;
		return p1.getName().equals(name);
	}
	
	@Override
	public String toString() {
		return name;
	}
}