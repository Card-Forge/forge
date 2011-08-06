
package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


public abstract class Player extends MyObservable{
	protected String name;
	protected int poisonCounters;
	protected int life;
	protected int assignedDamage;
	protected int preventNextDamage;
	protected int numPowerSurgeLands;
	
	protected boolean altWin = false;
	protected String winCondition = "";
	protected boolean altLose = false;
	protected String loseCondition = "";
	
	protected boolean bFirstTurn;
	
	protected Card lastDrawnCard;
	protected int numDrawnThisTurn = 0;
	protected CardList slowtripList = new CardList();
	
	public Player(String myName) {
		this(myName, 20, 0);
	}
	
	public Player(String myName, int myLife, int myPoisonCounters) {
		name = myName;
		life = myLife;
		poisonCounters = myPoisonCounters;
		assignedDamage = 0;
		preventNextDamage = 0;
		lastDrawnCard = null;
		numDrawnThisTurn = 0;
		bFirstTurn = true;
		altWin = false;
		altLose = false;
		winCondition = "";
		loseCondition = "";
		
		handSizeOperations = new ArrayList<HandSizeOp>();
	}
	
	public void reset(){
		life = 20;
		poisonCounters = 0;
		assignedDamage = 0;
		preventNextDamage = 0;
		lastDrawnCard = null;
		numDrawnThisTurn = 0;
		slowtripList = new CardList();
		bFirstTurn = true;
		altWin = false;
		altLose = false;
		winCondition = "";
		loseCondition = "";
		this.updateObservers();
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
	
	public boolean setLife(final int newLife, final Card source) {
		boolean change = false;
		//rule 118.5
		if(life > newLife) {
			change = loseLife(life - newLife, source);
		}
		else if(newLife > life) {
			change = gainLife(newLife - life, source);
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
	
	private void addLife(final int toAdd) {
		life += toAdd;
		this.updateObservers();
	}
	
	public boolean gainLife(final int toGain, final Card source) {
		boolean newLifeSet = false;
		if(!canGainLife()) return false;
		if(toGain > 0) {
			addLife(toGain);
			newLifeSet = true;
			this.updateObservers();
		}
		else System.out.println("Player - trying to gain negative or 0 life");
		
		Object[] Life_Whenever_Parameters = new Object[1];
    	Life_Whenever_Parameters[0] = toGain;
    	AllZone.GameAction.checkWheneverKeyword(getPlayerCard(), "GainLife", Life_Whenever_Parameters);

		return newLifeSet;
	}
	
	protected abstract Card getPlayerCard();
	
	public boolean canGainLife() {
		if(AllZoneUtil.isCardInPlay("Sulfuric Vortex")) return false;
		if(AllZoneUtil.isCardInPlay("Platinum Emperion",this)) return false;
		return true;
	}
	
	public boolean loseLife(final int toLose, final Card c) {
		boolean newLifeSet = false;
		if(!canLoseLife()) return false;
		if(toLose > 0) {
			subtractLife(toLose);
			newLifeSet = true;
			this.updateObservers();
		}
		else if(toLose == 0) {
			//Rule 118.4
			//this is for players being able to pay 0 life
			//nothing to do
		}
		else System.out.println("Player - trying to lose positive life");
		return newLifeSet;
	}
	
	public boolean canLoseLife() {
		if(AllZoneUtil.isCardInPlay("Platinum Emperion",this)) return false;
		return true;
	}
	
	private void subtractLife(final int toSub) {
		life -= toSub;
		this.updateObservers();
	}
	
	//this shouldn't be needed
	/*
	private void payLife(final int cost) {
		life -= cost;
		this.updateObservers();
	}
	*/
	
	public boolean canPayLife(int lifePayment) {
		if(life < lifePayment) return false;
		if(lifePayment > 0 && AllZoneUtil.isCardInPlay("Platinum Emperion",this)) return false;
		return true;
	}
	
	public boolean payLife(int lifePayment, Card source) {
    	if (!canPayLife(lifePayment)) return false;
		//rule 118.8
    	if (life >= lifePayment){
    		return loseLife(lifePayment, source);
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
    	damageToDo = preventDamage(damageToDo,source,false);
		
		addDamageWithoutPrevention(damageToDo,source);
	}
	
	public void addDamageWithoutPrevention(final int damage, final Card source) {
		int damageToDo = damage;
		
		if( source.getKeyword().contains("Infect") ) {
        	addPoisonCounters(damageToDo);
        }
        else {
        	if(PlayerUtil.worshipFlag(this) && life <= damageToDo) {
        		damageToDo = Math.min(damageToDo, life - 1);
        	}
        	//rule 118.2. Damage dealt to a player normally causes that player to lose that much life.
        	loseLife(damageToDo, source);
        }
        	
        if(source.getKeyword().contains("Lifelink")) GameActionUtil.executeLifeLinkEffects(source, damageToDo);
        
        CardList cl = CardFactoryUtil.getAurasEnchanting(source, "Guilty Conscience");
        for(Card c:cl) {
            GameActionUtil.executeGuiltyConscienceEffects(source, c, damageToDo);
        }
        
        GameActionUtil.executePlayerDamageEffects(this, source, damageToDo, false);
	}
	
	public boolean preventAllDamageToPlayer(final Card source, final boolean isCombat) {
		boolean reduce = false;
		
    	if(isCombat) {
    		reduce = reduce || source.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME.");
    		reduce = reduce || source.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME.");
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
	
	public int preventDamage(final int damage, Card source, boolean isCombat) {
    	int restDamage = damage;
    	
    	if (AllZoneUtil.isCardInPlay("Purity", this) && !isCombat) {
    		gainLife(restDamage,null);
    		return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Energy Storm") && source.isSpell()) return 0;
    	
    	if( preventAllDamageToPlayer(source, isCombat)) {
    		return 0;
        }
    	
    	if (AllZoneUtil.isCardInPlay("Spirit of Resistance", this) && !source.getController().equals(this)
    			&& restDamage > 0) restDamage = restDamage - 1;
    	
    	if (AllZoneUtil.isCardInPlay("Plated Pegasus") && source.isSpell() && restDamage > 0) restDamage = restDamage - 1;
    	
    	if (AllZoneUtil.isCardInPlay("Sphere of Purity", this) && source.isArtifact() && restDamage > 0) 
    		restDamage = restDamage - 1;
    	
    	if (AllZoneUtil.isCardInPlay("Sphere of Duty", this) && source.isGreen()) {
			if (restDamage > 1) restDamage = restDamage - 2;
			else return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Sphere of Grace", this) && source.isBlack()) {
			if (restDamage > 1) restDamage = restDamage - 2;
			else return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Sphere of Law", this) && source.isRed()) {
			if (restDamage > 1) restDamage = restDamage - 2;
			else return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Sphere of Reason", this) && source.isBlue()) {
			if (restDamage > 1) restDamage = restDamage - 2;
			else return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Sphere of Truth", this) && source.isWhite()) {
			if (restDamage > 1) restDamage = restDamage - 2;
			else return 0;
    	}
    	if (AllZoneUtil.isCardInPlay("Urza's Armor", this) && restDamage > 0) restDamage = restDamage - 1;

    	
    	if(restDamage >= preventNextDamage) {
    		restDamage = restDamage - preventNextDamage;
    		preventNextDamage = 0;
    	}
    	else {
    		restDamage = 0;
    		preventNextDamage = preventNextDamage - restDamage;
    	}
    	
    	return restDamage;
    }
	
	public void setAssignedDamage(int n)   		{	assignedDamage = n; }
    public int  getAssignedDamage()        		{	return assignedDamage; }
    
    public void addCombatDamage(final int damage, final Card source) {
    	
    	int damageToDo = damage;
    	damageToDo = preventDamage(damageToDo,source,true);
    	
    	addDamageWithoutPrevention(damageToDo, source);   //damage prevention is already checked
    	
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
	
	public boolean canTarget(Card card) {
		return !hasShroud();
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
	///
	/// replaces AllZone.GameAction.draw* methods
	///
	////////////////////////////////
	
	public abstract void mayDrawCard();
	
	public abstract void mayDrawCards(int numCards);
	
	public void drawCard() {
		drawCards(1);
	}
	
	public void drawCards() {
		drawCards(1);
	}
	
	public abstract boolean dredge();
	
	public void drawCards(int n) {
		PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, this);
		for(int i = 0; i < n; i++) {
			if(getDredge().size() == 0 || !dredge()) {
				doDraw(library, hand);
			}
		}
	}
	
	private void doDraw(PlayerZone library, PlayerZone hand) {
		if(library.size() != 0) {
			Card c = library.get(0);
			library.remove(0);
			hand.add(c);
			setLastDrawnCard(c);
			c.setDrawnThisTurn(true);
			numDrawnThisTurn++;

			GameActionUtil.executeDrawCardTriggeredEffects(this);
		}
		//lose:
		else if(Constant.Runtime.Mill[0]) {
			if (!cantLose()){
				altLoseConditionMet("Milled");
				AllZone.GameAction.checkStateEffects();
			}
		}
	}
	
	protected CardList getDredge() {
        CardList dredge = new CardList();
        CardList cl = AllZoneUtil.getPlayerGraveyard(this);
        
        for(Card c:cl) {
            ArrayList<String> kw = c.getKeyword();
            for(int i = 0; i < kw.size(); i++) {
                if(kw.get(i).toString().startsWith("Dredge")) {
                    if(AllZoneUtil.getPlayerCardsInLibrary(this).size() >= getDredgeNumber(c)) dredge.add(c);
                }
            }
        }
        return dredge;
    }//hasDredge()
    
    protected int getDredgeNumber(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Dredge")) {
                String s = a.get(i).toString();
                return Integer.parseInt("" + s.charAt(s.length() - 1));
            }
        
        throw new RuntimeException("Input_Draw : getDredgeNumber() card doesn't have dredge - " + c.getName());
    }//getDredgeNumber()
    
    public void resetNumDrawnThisTurn() {
    	numDrawnThisTurn = 0;
    }
    
    public int getNumDrawnThisTurn() {
    	return numDrawnThisTurn;
    }
    
    ////////////////////////////////
	///
	/// replaces AllZone.GameAction.discard* methods
	///
	////////////////////////////////
    
    public abstract CardList discard(final int num, final SpellAbility sa);
    
    public CardList discard(final SpellAbility sa) {
    	return discard(1, sa);
    }
    
    public void discard(Card c, SpellAbility sa) {
    	doDiscard(c, sa);
    }
    
    public void doDiscard(final Card c, final SpellAbility sa) {
    	if (sa!= null){
    		;
    	}
    	
    	AllZone.GameAction.checkWheneverKeyword(c,"DiscardsCard",null);
    	
    	/*
    	 * When a spell or ability an opponent controls causes you
    	 * to discard Psychic Purge, that player loses 5 life.
    	 */
    	if(c.getName().equals("Psychic Purge")) {
    		if( null != sa && !sa.getSourceCard().getController().equals(this)) {
    			SpellAbility ability = new Ability(c, "") {
    				public void resolve() {
    					sa.getSourceCard().getController().loseLife(5, c);
    				}
    			};
    			ability.setStackDescription(c.getName()+" - "+
    					sa.getSourceCard().getController()+" loses 5 life.");
    			AllZone.Stack.add(ability);
    		}
    	} 
    	
        AllZone.GameAction.discard_nath(c);
        AllZone.GameAction.discard_megrim(c);
        
        // necro disrupts madness
        if(AllZoneUtil.getPlayerCardsInPlay(c.getOwner(), "Necropotence").size() > 0) {	
        	AllZone.GameAction.exile(c);
        	return;
        }
        
        AllZone.GameAction.discard_madness(c);
        
        if((c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield instead of putting it into your graveyard.")
        		|| c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield with two +1/+1 counters on it instead of putting it into your graveyard."))	
        		&& !c.getController().equals(sa.getSourceCard().getController())) {
        	AllZone.GameAction.discard_PutIntoPlayInstead(c);
        }
        else if (c.getKeyword().contains("If a spell or ability an opponent controls causes you to discard CARDNAME, return it to your hand.")) {
        	;
        }
        else {
        	AllZone.GameAction.moveToGraveyard(c);
        }
    }//end doDiscard
    
    public void discardHand(SpellAbility sa) {
        CardList list = AllZoneUtil.getPlayerHand(this);
        discardRandom(list.size(), sa);
    }
    
    public void discardRandom(SpellAbility sa) {
        discardRandom(1, sa);
    }
    
    public void discardRandom(final int num, final SpellAbility sa) {
    	for(int i = 0; i < num; i++) {
    		Card[] c = AllZone.getZone(Constant.Zone.Hand, this).getCards();
    		if(c.length != 0) doDiscard(CardUtil.getRandom(c), sa);
    	}
    }
    
    public abstract void discardUnless(int num, String uType, SpellAbility sa);
    
    public void mill(int n) {
    	CardList lib = AllZoneUtil.getPlayerCardsInLibrary(this);

    	int max = Math.min(n, lib.size());

    	for(int i = 0; i < max; i++) {
    		AllZone.GameAction.moveToGraveyard(lib.get(i));
    	}
    }
    
    public abstract void handToLibrary(final int numToLibrary, String libPos);
    
    ////////////////////////////////
    public void shuffle() {
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
        Card c[] = library.getCards();
        
        if(c.length <= 1) return;
        
        ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(c));
        //overdone but wanted to make sure it was really random
        Random random = new Random();
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        
        Object o;
        for(int i = 0; i < list.size(); i++) {
            o = list.remove(random.nextInt(list.size()));
            list.add(random.nextInt(list.size()), o);
        }
        
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        

        list.toArray(c);
        library.setCards(c);
    }//shuffle
    ////////////////////////////////
    
    ////////////////////////////////
    protected abstract void doScry(CardList topN, int N);
    
    public void scry(int numScry) {
        CardList topN = new CardList();
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
        numScry = Math.min(numScry, library.size());
        for(int i = 0; i < numScry; i++) {
            topN.add(library.get(0));
            library.remove(0);
        }
        doScry(topN, topN.size());
    }
    ///////////////////////////////
    
    ///////////////////////////////
    ////
    ////	properties about the player and his/her cards/game status
    ////
    ///////////////////////////////
    public boolean hasPlaneswalker() {
        return null != getPlaneswalker();
    }
    
    public Card getPlaneswalker() {
    	CardList c = AllZoneUtil.getPlayerTypeInPlay(this, "Planeswalker");
    	if(null != c && c.size() > 0) return c.get(0);
    	else return null;
    }
    
    public int getNumPowerSurgeLands() {
    	return numPowerSurgeLands;
    }
    
    public int setNumPowerSurgeLands(int n) {
    	numPowerSurgeLands = n;
    	return numPowerSurgeLands;
    }
    
    public Card getLastDrawnCard() {
    	return lastDrawnCard;
    }
    
    public Card setLastDrawnCard(Card c) {
    	lastDrawnCard = c;
    	return lastDrawnCard;
    }
    
    public Card resetLastDrawnCard() {
    	Card old = lastDrawnCard;
    	lastDrawnCard = null;
    	return old;
    }
    
    public CardList getSlowtripList() {
    	return slowtripList;
    }
    
    public void clearSlowtripList() {
    	slowtripList.clear();
    }
    
    public void addSlowtripList(Card card) {
    	slowtripList.add(card);
    }
	
    public boolean isFirstTurn() { return bFirstTurn; }
    public void setFirstTurn(boolean b) { bFirstTurn = b; }
    
    ////////////////////////////////
    public abstract void sacrificePermanent(String prompt, CardList choices);
    
    public void sacrificeCreature() {
        CardList choices = AllZoneUtil.getCreaturesInPlay(this);
    	sacrificePermanent("Select a creature to sacrifice.", choices);
    }
    
    public void sacrificeCreature(CardList choices) {
    	sacrificePermanent("Select a creature to sacrifice.", choices);
    }
    
    // Game win/loss
    
    public boolean getAltWin(){
    	return altWin;
    }
    
    public boolean getAltLose(){
    	return altLose;
    }
    
    public String getWinCondition(){
    	return winCondition;
    }
    
    public String getLoseCondition(){
    	return loseCondition;
    }
    
    public void altWinConditionMet(String s) {
    	if (cantWin()){
    		System.out.println("Tried to win, but currently can't.");
    		return;
    	}
    	altWin = true; 
    	winCondition = s;
    }
    
    public void altLoseConditionMet(String s) { 
    	if (cantLose()){
    		System.out.println("Tried to lose, but currently can't.");
    		return;
    	}
    	altLose = true; 
    	loseCondition = s;
    }
    
    public boolean cantLose(){
    	if ((AllZoneUtil.getPlayerCardsInPlay(this, "Platinum Angel").size() > 0) ||
    			(AllZoneUtil.getPlayerCardsInPlay(getOpponent(), "Abyssal Persecutor").size() > 0)){
    		return true;
    	}
    	return false;
    }
    
    public boolean cantWin(){
    	if ((AllZoneUtil.getPlayerCardsInPlay(getOpponent(), "Platinum Angel").size() > 0) ||
    			(AllZoneUtil.getPlayerCardsInPlay(this, "Abyssal Persecutor").size() > 0)){
    		return true;
    	}
    	return false;
    }
    
    public boolean hasLost(){
    	if (cantLose())
    		return false;
    	
    	if (altLose){
    		return true;
    	}
    	
    	if (poisonCounters >= 10){
    		altLoseConditionMet("Poison Counters");
    		return true;
    	}
    	
    	return getLife() <= 0;
    }
    

    public boolean hasWon(){
    	if (cantWin())
    		return false;
    	
    	return altWin;
    }
    
    public boolean hasMetalcraft() {
    	CardList list = AllZoneUtil.getPlayerTypeInPlay(this, "Artifact");
    	return list.size() >= 3;
    }
    
    private ArrayList<HandSizeOp> handSizeOperations;

    public int getMaxHandSize() {
        
        int ret = 7;
        for(int i=0;i<handSizeOperations.size();i++)
        {
           if(handSizeOperations.get(i).Mode.equals("="))
           {
              ret = handSizeOperations.get(i).Amount;
           }
           else if(handSizeOperations.get(i).Mode.equals("+") && ret >= 0)
           {
              ret = ret + handSizeOperations.get(i).Amount;
           }
           else if(handSizeOperations.get(i).Mode.equals("-") && ret >= 0)
           {
              ret = ret - handSizeOperations.get(i).Amount;
              if(ret < 0) {
                 ret = 0;
              }
           }
        }
        return ret;
     }

    public void sortHandSizeOperations() {
        if(handSizeOperations.size() < 2) {
           return;
        }
        
        int changes = 1;

        while(changes > 0) {
           changes = 0;
           for(int i=1;i<handSizeOperations.size();i++) {
        	   if(handSizeOperations.get(i).hsTimeStamp < handSizeOperations.get(i-1).hsTimeStamp) {
        		   HandSizeOp tmp = handSizeOperations.get(i);
        		   handSizeOperations.set(i, handSizeOperations.get(i-1));
        		   handSizeOperations.set(i-1, tmp);
        		   changes++;
        	   }
            }
        }
     }
    
    public void addHandSizeOperation(HandSizeOp theNew)
    {
       handSizeOperations.add(theNew);
    }
    public void removeHandSizeOperation(int timestamp)
    {
       for(int i=0;i<handSizeOperations.size();i++)
       {
          if(handSizeOperations.get(i).hsTimeStamp == timestamp)
          {
             handSizeOperations.remove(i);
             break;
          }
       }
    }
    public void clearHandSizeOperations() {
    	handSizeOperations.clear();
    }
    
    private static int NextHandSizeStamp = 0;
    
    public static int getHandSizeStamp() {
       return NextHandSizeStamp++;
    }
	////////////////////////////////
	//
	// generic Object overrides
	//
	/////////////////////////////////
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Player){
			Player p1 = (Player)o;
			return p1.getName().equals(name);
		} else return false;
	}
	
	@Override
	public String toString() {
		return name;
	}
}