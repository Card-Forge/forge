
package forge;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

import com.esotericsoftware.minlog.Log;

public class Card extends MyObservable {
    private static int                   nextUniqueNumber;
    private int                          uniqueNumber                      = nextUniqueNumber++;
    
    private long						 value;
    
    //private Collection keyword   = new TreeSet();
    //private ArrayList<String> keyword = new ArrayList<String>();
    private ArrayList<String>			 intrinsicAbility				   = new ArrayList<String>();
    private ArrayList<String>            intrinsicKeyword                  = new ArrayList<String>();
    private ArrayList<String>            extrinsicKeyword                  = new ArrayList<String>();
    private ArrayList<String>			 otherExtrinsicKeyword			   = new ArrayList<String>();
    private ArrayList<String>			 HiddenExtrinsicKeyword			   = new ArrayList<String>();		//Hidden keywords won't be displayed on the card
    private ArrayList<String>            prevIntrinsicKeyword              = new ArrayList<String>();
    private ArrayList<Card>              attached                          = new ArrayList<Card>();
    private ArrayList<Card>              equippedBy                        = new ArrayList<Card>();             //which equipment cards are equipping this card?
    //equipping size will always be 0 or 1
    private ArrayList<Card>              equipping                         = new ArrayList<Card>();             //if this card is of the type equipment, what card is it currently equipping?
    private ArrayList<Card>              enchantedBy                       = new ArrayList<Card>();             //which auras enchanted this card?
    //enchanting size will always be 0 or 1
    private ArrayList<Card>              enchanting                        = new ArrayList<Card>();             //if this card is an Aura, what card is it enchanting?
    private ArrayList<String>            type                              = new ArrayList<String>();
    private ArrayList<String>            prevType                          = new ArrayList<String>();
    private ArrayList<String>            ChoicesMade                 	   = new ArrayList<String>();
    private ArrayList<String>            Targets_for_Choices               = new ArrayList<String>();
    private ArrayList<SpellAbility>      spellAbility                      = new ArrayList<SpellAbility>();
    private ArrayList<Ability_Mana>      manaAbility                       = new ArrayList<Ability_Mana>();
    private ArrayList<Card_Color>		 cardColor						   = new ArrayList<Card_Color>();
    
    private HashMap<Card, Integer>       receivedDamageFromThisTurn        = new HashMap<Card, Integer>();
    private HashMap<Card, Integer>       assignedDamageHashMap             = new HashMap<Card, Integer>();
    
    private boolean                      unCastable;
    private boolean						 drawnThisTurn						= false;
    private boolean                      tapped;
    private boolean                      sickness                          = true;                              //summoning sickness
    private boolean                      token                             = false;
    private boolean 					 copiedToken					   = false;
    private boolean 					 copiedSpell					   = false; 
    private boolean 					 SpellwithChoices				   = false; 
    private boolean 					 SpellCopyingCard				   = false; 
    private boolean						creatureAttackedThisTurn			= false;
    private boolean                      creatureAttackedThisCombat        = false;
    private boolean                      creatureBlockedThisCombat         = false;
    private boolean                      creatureGotBlockedThisCombat      = false;
    private boolean                      dealtCombatDmgToOppThisTurn       = false;
    private boolean                      dealtDmgToOppThisTurn             = false;
    private boolean						 sirenAttackOrDestroy			   = false;
    private boolean                      exaltedBonus                      = false;
    private boolean                      faceDown                          = false;
    private boolean                      sacrificeAtEOT                    = false;
    private boolean                      kicked                            = false;
    private boolean                      reflectedLand                     = false;
    private boolean						 levelUp						   = false;
    private boolean						 bounceAtUntap					   = false;
    private boolean						 finishedEnteringBF				   = false;
    
    private boolean                      firstStrike                       = false;
    private boolean                      doubleStrike                      = false;
    
    private boolean                      flashback                         = false;
    private boolean 					 unearth						   = false;
    private boolean 					 unearthed;
    
    private boolean						 madness						   = false;
    private boolean						 suspendCast					   = false;
    private boolean						 suspend						   = false;
    
    //for Vanguard / Manapool / Emblems etc.
    private boolean 					 isImmutable					   = false;
    
    private int                          exaltedMagnitude                  = 0;
    
    private int                          baseAttack;
    private int                          baseDefense;
    
    private int                          damage;
    
    private int                          nShield;
    private int 					     preventNextDamage				   = 0;
    
    private int                          turnInZone;
    
    private int                          tempAttackBoost                   = 0;
    private int                          tempDefenseBoost                  = 0;
    
    private int                          semiPermanentAttackBoost          = 0;
    private int                          semiPermanentDefenseBoost         = 0;
    
    private int                          otherAttackBoost                  = 0;
    private int                          otherDefenseBoost                 = 0;
    
    private int                          randomPicture                     = 0;
    
    private int                          upkeepDamage                      = 0;
    
    private int                          X                                 = 0;
    
    private int							 xManaCostPaid 					   = 0;
    
    private int							xLifePaid							= 0;
    
    private int 					     multiKickerMagnitude			   = 0;
    
    private Player                       owner                             = null;
    private Player                       controller                        = null;
    private String                       name                              = "";
    private String                       imageName                         = "";
    private String                       rarity                            = "";
    private String                       text                              = "";
    private String                       manaCost                          = "";
    private String                       upkeepCost                        = "";
    private String                       tabernacleUpkeepCost              = "";
    private String                       magusTabernacleUpkeepCost         = "";
    private String                       echoCost                          = "";
    private String						 madnessCost					   = "";
    private String                       chosenType                        = "";
    private String                       chosenColor                       = "";
    private String                       namedCard                         = "";
    private String						 topCardName					   = "";
    private String						 reflectableMana				   = "";
    private ArrayList<Card>				gainControlTargets					= new ArrayList<Card>();	
    private ArrayList<Command>			gainControlReleaseCommands			= new ArrayList<Command>();;
    

    private ArrayList<Ability_Triggered>  zcTriggers                        = new ArrayList<Ability_Triggered>();
    /*private ArrayList<Command> comesIntoPlayCommandList = new ArrayList<Command>();
    private ArrayList<Command> destroyCommandList       = new ArrayList<Command>();
    private ArrayList<Command> leavesPlayCommandList	  = new ArrayList<Command>();*/
    private ArrayList<Command>           turnFaceUpCommandList             = new ArrayList<Command>();
    private ArrayList<Command>           equipCommandList                  = new ArrayList<Command>();
    private ArrayList<Command>           unEquipCommandList                = new ArrayList<Command>();
    private ArrayList<Command>           enchantCommandList                = new ArrayList<Command>();
    private ArrayList<Command>           unEnchantCommandList              = new ArrayList<Command>();
    private ArrayList<Command>			untapCommandList					= new ArrayList<Command>();
    private ArrayList<Command>			changeControllerCommandList			= new ArrayList<Command>();
    private ArrayList<Command>           replaceMoveToGraveyardCommandList = new ArrayList<Command>();
    private ArrayList<Command>           cycleCommandList                  = new ArrayList<Command>();
    
    private Hashtable<Counters, Integer> counters                          = new Hashtable<Counters, Integer>();
    private Hashtable<String, String>    SVars                             = new Hashtable<String, String>();
    
    //hacky code below, used to limit the number of times an ability
    //can be used per turn like Vampire Bats
    //should be put in SpellAbility, but it is put here for convienance
    //this is make public just to make things easy
    //this code presumes that each card only has one ability that can be
    //used a limited number of times per turn
    //CardFactory.SSP_canPlay(Card) uses these variables
    
    private int                          abilityTurnUsed;                                                       //What turn did this card last use this ability?
    private int                          abilityUsed;                                                           //How many times has this ability been used?
                                                                                                                 
    public void setAbilityTurnUsed(int i) {
        abilityTurnUsed = i;
    }
    
    public int getAbilityTurnUsed() {
        return abilityTurnUsed;
    }
    
    public void setAbilityUsed(int i) {
        abilityUsed = i;
    }
    
    public int getAbilityUsed() {
        return abilityUsed;
    }
    
    //****************TOhaveDOne:Use somehow
    public void setX(int i) {
        X = i;
    }
    
    public int getX() {
        return X;
    }
    
    //***************/
    
    public void addXManaCostPaid(int n)
    {
    	xManaCostPaid += n;
    }
    
    public void setXManaCostPaid(int n)
    {
    	xManaCostPaid = n;
    }
    
    public int getXManaCostPaid()
    {
    	return xManaCostPaid;
    }
    
    public void setXLifePaid(int n)
    {
    	xLifePaid = n;
    }
    
    public int getXLifePaid()
    {
    	return xLifePaid;
    }
    
    //used to see if an attacking creature with a triggering attack ability triggered this phase:
    public void setCreatureAttackedThisCombat(boolean b) {
        creatureAttackedThisCombat = b;
        if(true == b) {
        	setCreatureAttackedThisTurn(true);
        }
    }
    
    public boolean getCreatureAttackedThisCombat() {
        return creatureAttackedThisCombat;
    }
    
    public void setCreatureAttackedThisTurn(boolean b) {
        creatureAttackedThisTurn = b;
    }
    
    public boolean getCreatureAttackedThisTurn() {
        return creatureAttackedThisTurn;
    }
    
    public void setCreatureBlockedThisCombat(boolean b) {
        creatureBlockedThisCombat = b;
    }
    
    public boolean getCreatureBlockedThisCombat() {
        return creatureBlockedThisCombat;
    }
    
    public void setCreatureGotBlockedThisCombat(boolean b) {
        creatureGotBlockedThisCombat = b;
    }
    
    public boolean getCreatureGotBlockedThisCombat() {
        return creatureGotBlockedThisCombat;
    }
    
    public void setDealtCombatDmgToOppThisTurn(boolean b) {
        dealtCombatDmgToOppThisTurn = b;
    }
    
    public boolean getDealtCombatDmgToOppThisTurn() {
        return dealtCombatDmgToOppThisTurn;
    }
    
    public boolean canAnyPlayerActivate() {
    	for(SpellAbility s : spellAbility)
    	{
    		if (s.getRestrictions().getAnyPlayer())
    			return true;
    	}
    	return false;
    }
    
    public void setDealtDmgToOppThisTurn(boolean b) {
        dealtDmgToOppThisTurn = b;
    }
    
    public boolean getDealtDmgToOppThisTurn() {
        return dealtDmgToOppThisTurn;
    }
    
    public void setSirenAttackOrDestroy(boolean b) {
    	sirenAttackOrDestroy = b;
    }
    
    public boolean getSirenAttackOrDestroy() {
    	return sirenAttackOrDestroy;
    }    
    
    public boolean getSacrificeAtEOT() {
        return sacrificeAtEOT || getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME.");
    }
    
    public void setSacrificeAtEOT(boolean sacrificeAtEOT) {
        this.sacrificeAtEOT = sacrificeAtEOT;
    }
    
    public boolean getBounceAtUntap() {
        return bounceAtUntap;
    }
    
    public void setBounceAtUntap(boolean bounce) {
        this.bounceAtUntap = bounce;
    }
    
    public boolean getFinishedEnteringBF() {
        return finishedEnteringBF;
    }
    
    public void setFinishedEnteringBF(boolean b) {
        this.finishedEnteringBF = b;
    }
    
    public boolean hasFirstStrike() {
        return firstStrike || getKeyword().contains("First Strike");
    }
    
    public void setFirstStrike(boolean firstStrike) {
        this.firstStrike = firstStrike;
    }
    
    public void setDoubleStrike(boolean doubleStrike) {
        this.doubleStrike = doubleStrike;
    }
    
    public boolean hasDoubleStrike() {
        return doubleStrike || getKeyword().contains("Double Strike");
    }
    
    public boolean hasSecondStrike() {
        return !hasFirstStrike() || (hasFirstStrike() && hasDoubleStrike());
    };
    
    //for Planeswalker abilities and Combat Damage (like Wither), Doubling Season gets ignored.
    public void addCounterFromNonEffect(Counters counterName, int n) {
        if(counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) + n;
            counters.put(counterName, aux);
        } else {
            counters.put(counterName, Integer.valueOf(n));
        }
        this.updateObservers();
    }
    
    public void addCounter(Counters counterName, int n) {
    	int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(this.getController());
        if(counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) + (multiplier * n);
            counters.put(counterName, aux);
        } else {
            counters.put(counterName, Integer.valueOf(multiplier * n));
        }
        
        if (counterName.equals(Counters.P1P1) || counterName.equals(Counters.M1M1)){
        	// +1/+1 counters should erase -1/-1 counters
        	int plusOneCounters = 0;
        	int minusOneCounters = 0;
        	
        	Counters p1Counter = Counters.P1P1;
        	Counters m1Counter = Counters.M1M1;
        	if (counters.containsKey(p1Counter))
        		plusOneCounters = counters.get(p1Counter);
        	if (counters.containsKey(m1Counter))
        		minusOneCounters = counters.get(m1Counter);

        	if (plusOneCounters == minusOneCounters){
        		counters.remove(m1Counter);
        		counters.remove(p1Counter);
        	}
        	if (plusOneCounters > minusOneCounters){
        		counters.remove(m1Counter);
        		counters.put(p1Counter, (Integer)(plusOneCounters - minusOneCounters));	
        	}
        	else{
        		counters.put(m1Counter, (Integer)(minusOneCounters - plusOneCounters));
        		counters.remove(p1Counter);
        	}
        }
        
        this.updateObservers();
    }
    
    public void subtractCounter(Counters counterName, int n) {
        if(counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) - n;
            if (aux < 0)
            	aux = 0;
            counters.put(counterName, aux);
			if(counterName.equals(Counters.TIME) && aux == 0)
			{
				boolean hasVanish = CardFactory.hasKeyword(this, "Vanishing") != -1;

				if(hasVanish && AllZone.GameAction.isCardInPlay(this))
					AllZone.GameAction.sacrifice(this);
				if(hasSuspend() && AllZone.GameAction.isCardExiled(this))
				{
					final Card c = this;
					
					c.setSuspendCast(true);

					// TODO(sol): haste should wear off when player loses control. need to figure out where to add that.
					Command intoPlay = new Command() {
						private static final long serialVersionUID = -4514610171270596654L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(c) && c.isCreature()) 
								c.addExtrinsicKeyword("Haste");
						}//execute()
					};

					c.addComesIntoPlayCommand(intoPlay);
					AllZone.GameAction.playCardNoCost(c);
					if (AllZone.getZone(c) != null)
						AllZone.getZone(c).remove(c);
				}
			}
            this.updateObservers();
        }
    }
    
    public int getCounters(Counters counterName) {
        if(counters.containsKey(counterName)) {
            return counters.get(counterName);
        } else return 0;
    }
    
    public boolean hasCounters()
    {
    	return counters.size() > 0;
    }
    
    public void setCounter(Counters counterName, int n, boolean bSetValue) {
    	if (bSetValue)	// sometimes you just need to set the value without being affected by DoublingSeason
    		counters.put(counterName, Integer.valueOf(n));
    	else{
    		int num = getCounters(counterName);
    		if(num < n)	// if counters on card is less than the setting value, addCounters
	        	addCounter(counterName, n - num);
	        else
	        	subtractCounter(counterName, num - n);
	    }
        this.updateObservers();
    }
    
    /**
     * hasLevelUp() - checks to see if a creature has the "Level up" ability introduced in Rise of the Eldrazi
     * @return true if this creature can "Level up", false otherwise
     */
    public boolean hasLevelUp() {
    	return levelUp;
    }
    
    public void setLevelUp(boolean b)
    {
    	levelUp = b;
    }
    
    public String getSVar(String Var) {
        if(SVars.containsKey(Var)) return SVars.get(Var);
        else return "";
    }
    
    public void setSVar(String Var, String str) {
        if(SVars.containsKey(Var)) SVars.remove(Var);
        
        SVars.put(Var, str);
    }
    
    public Hashtable<String, String> getSVars()
    {
    	return SVars;
    }
    
    public void setSVars(Hashtable<String, String> newSVars)
    {
    	SVars = newSVars;
    }
    
    public int sumAllCounters() {
        Object[] values = counters.values().toArray();
        int count = 0;
        int num = 0;
        for(int i = 0; i < values.length; i++) {
            num = (Integer) values[i];
            count += num;
        }
        return count;
    }
    
    public int getNetPTCounters() {
        return getCounters(Counters.P1P1) - getCounters(Counters.M1M1);
    }
    
    public int getTurnInZone() {
        return turnInZone;
    }
    
    public void setTurnInZone(int turn) {
        turnInZone = turn;
    }
    
    public void setEchoCost(String s) {
        echoCost = s;
    }
    
    public String getEchoCost() {
        return echoCost;
    }
    
    public void setManaCost(String s) {
        manaCost = s;
    }
    
    public String getManaCost() {
        return manaCost;
    }
    
    public void addColor(String s){
    	if (s.equals(""))
    		s = "0";
    	cardColor.add(new Card_Color(new ManaCost(s), this, false, true));
    }
    
    public long addColor(String s, Card c, boolean addToColors, boolean bIncrease){
    	if (bIncrease)
    		Card_Color.increaseTimestamp();
    	cardColor.add(new Card_Color(new ManaCost(s), c, addToColors, false));
    	return Card_Color.getTimestamp();
    }
    
    public void removeColor(String s, Card c, boolean addTo, long timestamp){
    	Card_Color removeCol = null;
    	for(Card_Color cc : cardColor)
    		if (cc.equals(s, c, addTo, timestamp))
    			removeCol = cc;
    	
    	if (removeCol != null)
    		cardColor.remove(removeCol);
    }
    
    public Card_Color getColor(){
    	if (this.isImmutable()){
    		return new Card_Color(this);
    	}
    	Card_Color colors = null;
    	ArrayList<Card_Color> globalChanges = AllZone.GameInfo.getColorChanges();
    	colors = determineColor(globalChanges);
    	colors.fixColorless();
    	return colors;
    }
    
    
    public void setColor(ArrayList<Card_Color> colors){
    	cardColor = colors;
    }
    
    
    Card_Color determineColor(ArrayList<Card_Color> globalChanges){
    	Card_Color colors = new Card_Color(this);
    	int i = cardColor.size() - 1;
    	int j = globalChanges.size() - 1;
    	// if both have changes, see which one is most recent
    	while(i >= 0 && j >= 0){
    		Card_Color cc = null;
    		if (cardColor.get(i).getStamp() > globalChanges.get(j).getStamp()){
    			// Card has a more recent color stamp
    			cc = cardColor.get(i);
    			i--;
    		}
    		else{
    			// Global effect has a more recent color stamp
    			cc = globalChanges.get(j);
    			j--;
    		}

			for (String s : cc.toStringArray())
				colors.addToCardColor(s);
    		if (!cc.getAdditional())
    			return colors;
    	}
    	while(i >= 0){
    		Card_Color cc = cardColor.get(i);
    		i--;
			for(String s : cc.toStringArray())
				colors.addToCardColor(s);
    		if (!cc.getAdditional())
    			return colors;
    	}
    	while(j >= 0){
    		Card_Color cc = globalChanges.get(j);
    		j--;
			for(String s : cc.toStringArray())
				colors.addToCardColor(s);
    		if (!cc.getAdditional())
    			return colors;
    	}
    	
    	return colors;
    }
    
    public int getCMC()
    {
    	return CardUtil.getConvertedManaCost(manaCost);
    }
    
    public void setUpkeepCost(String s) {
        upkeepCost = s;
    }
    
    public String getUpkeepCost() {
        return upkeepCost;
    }
    
    public boolean hasUpkeepCost() {
        return upkeepCost.length() > 0 && !upkeepCost.equals("0");
    }
    
    public void setTabernacleUpkeepCost(String s) {
        tabernacleUpkeepCost = s;
    }
    
    public String getTabernacleUpkeepCost() {
        return tabernacleUpkeepCost;
    }
    
    public void setMagusTabernacleUpkeepCost(String s) {
        magusTabernacleUpkeepCost = s;
    }
    
    public String getMagusTabernacleUpkeepCost() {
        return magusTabernacleUpkeepCost;
    }
    
    //used for cards like Belbe's Portal, Conspiracy, Cover of Darkness, etc.
    public String getChosenType() {
        return chosenType;
    }
    
    public void setChosenType(String s) {
        chosenType = s;
    }
    
    public String getChosenColor() {
        return chosenColor;
    }
    
    public void setChosenColor(String s) {
        chosenColor = s;
    }
    
    //used for cards like Meddling Mage...
    public String getNamedCard() {
        return namedCard;
    }
    
    public void setNamedCard(String s) {
        namedCard = s;
    }
    
    public String getTopCardName() {
        return topCardName;
    }
    
    public void setTopCardName(String s) {
        topCardName = s;
    }
    
    public void setDrawnThisTurn(boolean b) {
    	drawnThisTurn = b;
    }
    
    public boolean getDrawnThisTurn() {
    	return drawnThisTurn;
    }
    
    public String getReflectableMana() {
        return reflectableMana;
    }
    
    public void setReflectableMana(String s) {
    	reflectableMana = s;
    }
    
    /**
     * get a list of Cards this card has gained control of
     * 
     * used primarily with AbilityFactory_GainControl
     * 
     * @return a list of cards this card has gained control of
     */
    public ArrayList<Card> getGainControlTargets() {
    	return gainControlTargets;
    }
    
    /**
     * add a Card to the list of Cards this card has gained control of
     * 
     * used primarily with AbilityFactory_GainControl
     */
    public void addGainControlTarget(Card c) {
    	gainControlTargets.add(c);
    }
    
    /**
     * clear the list of Cards this card has gained control of
     * 
     * used primarily with AbilityFactory_GainControl
     */
    public void clearGainControlTargets() {
    	gainControlTargets.clear();
    }
    
    /**
     * get the commands to be executed to lose control of Cards this
     * card has gained control of
     * 
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea specifically)
     */
    public ArrayList<Command> getGainControlReleaseCommands() {
    	return gainControlReleaseCommands;
    }
    
    /**
     * set a command to be executed to lose control of Cards this
     * card has gained control of
     * 
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea specifically)
     * 
     * @param c the Command to be executed
     */
    public void addGainControlReleaseCommand(Command c) {
    	gainControlReleaseCommands.add(c);
    }
    
    public void clearGainControlReleaseCommands() {
    	gainControlReleaseCommands.clear();
    }
    
    public String getSpellText() {
        return text;
    }
    
    public void setText(String t) {
        text = t;
    }
    
    // get the text that should be displayed
    public String getText() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(this.getAbilityText());
    	String NonAbilityText = getNonAbilityText();
    	if (NonAbilityText.length() > 0) {
    		sb.append("\r\n \r\nNon ability features: \r\n");
    		sb.append(NonAbilityText);
    	}
    	
    	return sb.toString();
    }
    
    // get the text that does not belong to a cards abilities (and is not really there ruleswise)
    public String getNonAbilityText() {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keyword = getHiddenExtrinsicKeyword();
        
        sb.append(keywordsToText(keyword));
        
        return sb.toString();
    }
    
    // convert a keyword list to the String that should be displayed ingame
    public String keywordsToText(ArrayList<String> keyword) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbLong = new StringBuilder();
        StringBuilder sbMana = new StringBuilder();
    	
    	for (int i = 0; i < keyword.size(); i++) {
            if (!keyword.get(i).toString().contains("CostChange") 
            		&& 
            		!keyword.get(i).toString().contains("Whenever CARDNAME blocks a creature, destroy that creature at end of combat")
            		&& 
            		!keyword.get(i).toString().contains("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat")
            		&& 
            		!keyword.get(i).toString().contains("Permanents don't untap during their controllers' untap steps")
            		&& 
            		!keyword.get(i).toString().contains("PreventAllDamageBy"))
            	{
                if (keyword.get(i).toString().contains("WheneverKeyword")) {
                    String k[] = keyword.get(i).split(":");
                    sbLong.append(k[9]).append("\r\n");
                } else if (keyword.get(i).toString().contains("StaticEffect")) {
                    String k[] = keyword.get(i).split(":");
                    sbLong.append(k[5]).append("\r\n");
                } else if (keyword.get(i).toString().contains("stPump")) {
                    String k[] = keyword.get(i).split(":");
                    if (!k[4].contains("no text")) sbLong.append(k[4]).append("\r\n");
                } else if (keyword.get(i).toString().contains("Protection:")) {
                    String k[] = keyword.get(i).split(":");
                    sbLong.append(k[2]).append("\r\n");
                } else if (keyword.get(i).endsWith(".")) {
                    sbLong.append(keyword.get(i).toString()).append("\r\n");
                } else if (keyword.get(i).contains("At the beginning of your upkeep, ") 
                        && keyword.get(i).contains(" unless you pay:")) {
                    sbLong.append(keyword.get(i).toString()).append("\r\n");
                } else if (keyword.get(i).toString().contains("tap: add ")) {
                    sbMana.append(keyword.get(i).toString()).append("\r\n");
                } else {
                    if (i != 0 && sb.length() != 0) sb.append(", ");
                    sb.append(keyword.get(i).toString());
                }
            }
        }
        if (sb.length() > 0) sb.append("\r\n\r\n");
        if (sbLong.length() > 0) sbLong.append("\r\n");
        sb.append(sbLong);
        sb.append(sbMana);
    	return sb.toString();
    }
    
    //get the text of the abilities of a card
    public String getAbilityText() {
        if(isInstant() || isSorcery()) {
            String s = getSpellText();
            StringBuilder sb = new StringBuilder();
            
            // Give spellText line breaks for easier reading
            sb.append(s.replaceAll("\\\\r\\\\n", "\r\n"));
            
            // NOTE:
            if (sb.toString().contains(" (NOTE: ")) {
                sb.insert(sb.indexOf("(NOTE: "), "\r\n");
            }
            if (sb.toString().contains("(NOTE: ") && sb.toString().endsWith(".)") && !sb.toString().endsWith("\r\n")) {
                sb.append("\r\n");
            }
            
            
            // Add SpellAbilities
            SpellAbility[] sa = getSpellAbility();
            for (int i = 0; i < sa.length; i++) {
                sb.append(sa[i].toString() + "\r\n");
            }
            
            // Add Keywords
            ArrayList<String> kw = getKeyword();
            
            // Ripple + Dredge + Madness + CARDNAME is {color}.
            for (int i = 0; i < kw.size(); i++) {
                if ((kw.get(i).startsWith("Ripple") && !sb.toString().contains("Ripple")) 
                        || (kw.get(i).startsWith("Dredge") && !sb.toString().contains("Dredge")) 
                        || (kw.get(i).startsWith("Madness") && !sb.toString().contains("Madness")) 
                        || (kw.get(i).startsWith("CARDNAME is ") && !sb.toString().contains("CARDNAME is "))) {
                    sb.append(kw.get(i).replace(":", " ")).append("\r\n");
                }
            }
            
            // Draw a card. + Changeling + CARDNAME can't be countered. + Cascade
            for (int i = 0; i < kw.size(); i++) {
                if ((kw.get(i).contains("Draw a card.") && !sb.toString().contains("Draw a card."))
                		|| (kw.get(i).contains("Draw a card at the beginning of the next turn's upkeep.") && !sb.toString().contains("Draw a card at the beginning of the next turn's upkeep."))
                        || (kw.get(i).contains("Changeling") && !sb.toString().contains("Changeling")) 
                        || (kw.get(i).contains("CARDNAME can't be countered.") && !sb.toString().contains("CARDNAME can't be countered.")) 
                        || (kw.get(i).contains("Cascade") && !sb.toString().contains("Cascade"))) {
                    sb.append(kw.get(i)).append("\r\n");
                }
            }
            
            // Storm
            if (getKeyword().contains("Storm") && !sb.toString().contains("Storm (When you ")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n")+3);
                }
                sb.append("Storm (When you cast this spell, copy it for each spell cast before it this turn.");
                if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                    sb.append(" You may choose new targets for the copies.");
                }
                sb.append(")\r\n");
            }
            
            // Scry
            if(!sb.toString().contains("Scry")) for(int i = 0; i < getKeyword().size(); i++) {
                String k = getKeyword().get(i);
                if(k.startsWith("Scry")) {
                    String kk[] = k.split(" ");
                    //sb.append("Scry " + kk[1] + " (To scry X, look at the top X cards of your library, then put any number of them on the bottom of your library and the rest on top in any order.)\r\n");
                    sb.append("Scry ");
                    sb.append(kk[1]);
                    sb.append(" (To scry X, look at the top X cards of your library, then put any number of them on the bottom of your library and the rest on top in any order.)\r\n");
                }
            }
            
            while (sb.toString().endsWith("\r\n")) {
                sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n")+3);
            }
            
            return sb.toString().replaceAll("CARDNAME", getName());
        }
        
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keyword = getUnhiddenKeyword();
        
        sb.append(keywordsToText(keyword));
        
/*
        for(int i = 0; i < keyword.size(); i++) {
            if(!keyword.get(i).toString().contains("CostChange")) {
                if(i != 0) sb.append(", ");
                if(!keyword.get(i).toString().contains("WheneverKeyword") 
                        && !keyword.get(i).toString().contains("StaticEffect")) sb.append(keyword.get(i).toString()); 
                else if(keyword.get(i).toString().contains("WheneverKeyword")) {                
                     String k[] = keyword.get(i).split(":");
                     sb.append(k[9]); 
                } 
                else if(keyword.get(i).toString().contains("StaticEffect")) {                
                    String k[] = keyword.get(i).split(":");
                    sb.append(k[5]); 
                }
            }
        }
*/
        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        SpellAbility[] sa = getSpellAbility();
        for(int i = 0; i < sa.length; i++) {
            //presumes the first SpellAbility added to this card, is the "main" spell
            //skip the first SpellAbility for creatures, since it says "Summon this creature"
            //looks bad on the Gui card detail
            if(isPermanent() && (isLand() || i != 0)
                    && !(manaAbility.contains(sa[i]) && ((Ability_Mana) sa[i]).isBasic()))//prevent mana ability duplication
            {
                sb.append(sa[i].toString());
                sb.append("\r\n");
            }
        }
        
        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().contains(".) ")) {
            sb.insert(sb.indexOf(".) ")+3, "\r\n");
        }
        
        // replace tripple line feeds with double line feeds
        int start;
        String s = "\r\n\r\n\r\n";
        while (sb.toString().contains(s)) {
            start = sb.lastIndexOf(s);
            if (start < 0 || start >= sb.length())
                break;
            sb.replace(start, start+4, "\r\n");
        }
        
        return sb.toString().replaceAll("CARDNAME", getName()).trim();
    }//getText()
    
    /* private ArrayList<Ability_Mana> addLandAbilities ()
     {
      ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>(manaAbility);
      if (!getType().contains("Land")) return res;
      ArrayList<String> types = getType();
      for(int i = 0; i < basics.length; i++)
    	  if(types.contains(basics[i]) && !res.contains("tap: add "+ ManaPool.colors.charAt(i)))
    		  res.add(new Ability_Mana(this, "tap: add "+ ManaPool.colors.charAt(i)){});
      return res;
     }*/
    /*ArrayList<Ability_Mana> addExtrinsicAbilities(ArrayList<Ability_Mana> have)
    {
      try{
      if (AllZone.getZone(this).is(Constant.Zone.Play))
      {
    	  for (Card c : AllZone.getZone(Constant.Zone.Play, getController()).getCards())
    			  if (c.getName().equals("Joiner Adept") && getType().contains("Land") || c.getName().equals("Gemhide Sliver") && getType().contains("Sliver"))
    				  for (char ch : ManaPool.colors.toCharArray())
    					  have.add(new Ability_Mana(this, "tap: add " + ch){});
      }}
      catch(NullPointerException ex){}//TOhaveDOne: fix this to something more memory-efficient than catching 2000 NullPointer Exceptions every time you open deck editor
      return have;
    }*/
    public ArrayList<Ability_Mana> getManaAbility() {
        return new ArrayList<Ability_Mana>(manaAbility);
    }
    
    // Returns basic mana abilities plus "reflected mana" abilities
    public ArrayList<Ability_Mana> getAIPlayableMana() {
        ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();
    	for(Ability_Mana am:getManaAbility())
    		if(am.isBasic() && !res.contains(am)) {
    			res.add(am);
    		} else if (am.isReflectedMana() && !res.contains(am)) {
    			res.add(am);
    		}	
        
        return res;
        
    }
    
    public ArrayList<Ability_Mana> getBasicMana() {
    	ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();
    	for(Ability_Mana am:getManaAbility())
    		if(am.isBasic() && !res.contains(am)) res.add(am);
        return res;
    }
    
    public void clearFirstSpellAbility(){
    	spellAbility.remove(0);
    }
    
    public void clearSpellAbility() {
        spellAbility.clear();
        manaAbility.clear();
    }
    
    public void clearSpellKeepManaAbility() {
        spellAbility.clear();
    }
    
    public void clearManaAbility() {
        manaAbility.clear();
    }
    
    
    public void addFirstSpellAbility(SpellAbility a){
    	a.setSourceCard(this);
        if(a instanceof Ability_Mana) manaAbility.add(0, (Ability_Mana) a);
        else spellAbility.add(0, a);
    }
    
    public void addSpellAbility(SpellAbility a) {
        a.setSourceCard(this);
        if(a instanceof Ability_Mana) manaAbility.add((Ability_Mana) a);
        else spellAbility.add(a);
    }
    
    public void removeSpellAbility(SpellAbility a) {
        if(a instanceof Ability_Mana)
        //if (a.isExtrinsic()) //never remove intrinsic mana abilities, is this the way to go??
        manaAbility.remove(a);
        else spellAbility.remove(a);
    }
    
    
    public void removeAllExtrinsicManaAbilities() {
        //temp ArrayList, otherwise ConcurrentModificationExceptions occur:
        ArrayList<SpellAbility> saList = new ArrayList<SpellAbility>();
        
        for(SpellAbility var:manaAbility) {
            if(var.isExtrinsic()) saList.add(var);
        }
        for(SpellAbility sa:saList) {
            removeSpellAbility(sa);
        }
    }
    
    public ArrayList<String> getIntrinsicManaAbilitiesDescriptions() {
        ArrayList<String> list = new ArrayList<String>();
        for(SpellAbility var:manaAbility) {
            if(var.isIntrinsic()) list.add(var.toString());
        }
        return list;
    }
    
    public SpellAbility[] getSpellAbility() {
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(spellAbility);
        res.addAll(getManaAbility());
        SpellAbility[] s = new SpellAbility[res.size()];
        res.toArray(s);
        return s;
    }
    
    public ArrayList<SpellAbility> getSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();
        
        for(SpellAbility sa:s) {
            if(sa.isSpell()) res.add(sa);
        }
        return res;
    }
    
    public ArrayList<SpellAbility> getBasicSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();
        
        for(SpellAbility sa:s) {
            if(sa.isSpell() && !sa.isFlashBackAbility() && !sa.isBuyBackAbility()) res.add(sa);
        }
        return res;
    }
    
    public ArrayList<SpellAbility> getAdditionalCostSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();
        
        for(SpellAbility sa:s) {
            if(sa.isSpell() && !sa.getAdditionalManaCost().equals("")) res.add(sa);
        }
        return res;
    }
    
    
    //shield = regeneration
    public void setShield(int n) {
        nShield = n;
    }
    
    public int getShield() {
        return nShield;
    }
    
    public void addShield() {
        nShield++;
    }
    
    public void subtractShield() {
        nShield--;
    }
    
    public void resetShield() {
        nShield = 0;
    }
    
    //is this "Card" supposed to be a token?
    public void setToken(boolean b) {
        token = b;
    }
    
    public boolean isToken() {
        return token;
    }
    
    public void setCopiedToken(boolean b)
    {
    	copiedToken = b;
    }
    
    public boolean isCopiedToken() {
    	return copiedToken;
    }
    public void setCopiedSpell(boolean b)
    {
    	copiedSpell = b;
    }
    
    public boolean isCopiedSpell() {
    	return copiedSpell;
    }
    public void addSpellChoice(String string)
    {
    	ChoicesMade.add(string);
    }
    
    public ArrayList<String> getChoices() {
    	return ChoicesMade;
    }
    public String getChoice(int i) {
    	return ChoicesMade.get(i);
    }
    
    public void setSpellChoiceTarget(String string)
    {
    	Targets_for_Choices.add(string);
    }
    
    public ArrayList<String> getChoiceTargets() {
    	return Targets_for_Choices;
    }
    public String getChoiceTarget(int i) {
    	return Targets_for_Choices.get(i);
    }
    
    public void setSpellWithChoices(boolean b)
    {
    	SpellwithChoices = b;
    }
    
    public boolean hasChoices() {
    	return SpellwithChoices;
    }
    public void setCopiesSpells(boolean b)
    {
    	SpellCopyingCard = b;
    }
    public boolean copiesSpells() {
    	return SpellCopyingCard;
    }
    public void setExaltedBonus(boolean b) {
        exaltedBonus = b;
    }
    
    public boolean hasExaltedBonus() {
        return exaltedBonus;
    }
    
    public void setExaltedMagnitude(int i) {
        exaltedMagnitude = i;
    }
    
    public int getExaltedMagnitude() {
        return exaltedMagnitude;
    }
    
    public void setIsFaceDown(boolean b) {
        faceDown = b;
    }
    
    public boolean isFaceDown() {
        return faceDown;
    }
    
    public void addTrigger(Command c, ZCTrigger type) {
        zcTriggers.add(new Ability_Triggered(this, c, type));
    }
    
    public void removeTrigger(Command c, ZCTrigger type) {
        zcTriggers.remove(new Ability_Triggered(this, c, type));
    }
    
    public void executeTrigger(ZCTrigger type) {
        for(Ability_Triggered t:zcTriggers)
            if(t.trigger.equals(type) && t.isBasic()) AllZone.Stack.add(t);
    }
    
    public void addComesIntoPlayCommand(Command c) {
        addTrigger(c, ZCTrigger.ENTERFIELD);
    }
    
    public void removeComesIntoPlayCommand(Command c) {
        removeTrigger(c, ZCTrigger.ENTERFIELD);
    }
    
    public void comesIntoPlay() {
        executeTrigger(ZCTrigger.ENTERFIELD);
    }
    
    public void addTurnFaceUpCommand(Command c) {
        turnFaceUpCommandList.add(c);
    }
    
    public void removeTurnFaceUpCommand(Command c) {
        turnFaceUpCommandList.remove(c);
    }
    
    public void turnFaceUp() {
        for(Command var:turnFaceUpCommandList)
            var.execute();
    }
    
    public void addDestroyCommand(Command c) {
        addTrigger(c, ZCTrigger.DESTROY);
    }
    
    public void removeDestroyCommand(Command c) {
        removeTrigger(c, ZCTrigger.DESTROY);
    }
    
    public void destroy() {
        executeTrigger(ZCTrigger.DESTROY);
    }
    
    public void addLeavesPlayCommand(Command c) {
        addTrigger(c, ZCTrigger.LEAVEFIELD);
    }
    
    public void removeLeavesPlayCommand(Command c) {
        removeTrigger(c, ZCTrigger.LEAVEFIELD);
    }
    
    public void leavesPlay() {
        executeTrigger(ZCTrigger.LEAVEFIELD);
    }
    
    public void addEquipCommand(Command c) {
        equipCommandList.add(c);
    }
    
    public void removeEquipCommand(Command c) {
        equipCommandList.remove(c);
    }
    
    public void equip() {
        for(Command var:equipCommandList)
            var.execute();
    }
    
    public void addUnEquipCommand(Command c) {
        unEquipCommandList.add(c);
    }
    
    public void removeUnEquipCommand(Command c) {
        unEquipCommandList.remove(c);
    }
    
    public void unEquip() {
        for(Command var:unEquipCommandList)
            var.execute();
    }
    
    public void addEnchantCommand(Command c) {
        enchantCommandList.add(c);
    }
    
    public void removeEnchantCommand(Command c) {
        enchantCommandList.add(c);
    }
    
    public void enchant() {
        for(Command var:enchantCommandList)
            var.execute();
    }
    
    public void addUnEnchantCommand(Command c) {
        unEnchantCommandList.add(c);
    }
    
    public void unEnchant() {
        for(Command var:unEnchantCommandList)
            var.execute();
    }
    
    public void addUntapCommand(Command c) {
    	untapCommandList.add(c);
    }
    
    public void addChangeControllerCommand(Command c) {
    	changeControllerCommandList.add(c);
    }
    
    public ArrayList<Command> getReplaceMoveToGraveyard() {
        return replaceMoveToGraveyardCommandList;
    }
    
    public void addReplaceMoveToGraveyardCommand(Command c) {
        replaceMoveToGraveyardCommandList.add(c);
    }
    
    public void clearReplaceMoveToGraveyardCommandList() {
        replaceMoveToGraveyardCommandList.clear();
    }
    
    public void replaceMoveToGraveyard() {
        for(Command var:replaceMoveToGraveyardCommandList)
            var.execute();
    }
    
    public void addCycleCommand(Command c) {
        cycleCommandList.add(c);
    }
    
    public void cycle() {
        for(Command var:cycleCommandList)
            var.execute();
    }
    
    public void setSickness(boolean b) {
        sickness = b;
    }
    
    public boolean hasSickness() {
        if(getKeyword().contains("Haste")) return false;
        
        return sickness;
    }
    
    public boolean isSick() {
        if(getKeyword().contains("Haste")) return false;
        
        return sickness && isCreature();
    }
    
    public void setRarity(String s) {
        rarity = s;
    }
    
    public String getRarity() {
        return rarity;
    }
    
    
    
    
    public void setImageName(String s) {
        imageName = s;
    }
    
    public String getImageName() {
        if(!imageName.equals("")) return imageName;
        return name;
    }
    
    public String getName() {
        return name;
    }
    
    public Player getOwner() {
        return owner;
    }
    
    public Player getController() {
        return controller;
    }
    
    public void setName(String s) {
        name = s;
        this.updateObservers();
    }
    
    public void setOwner(Player player) {
        owner = player;
        this.updateObservers();
    }
    
    public void setController(Player player) {
    	if( null != controller && !controller.isPlayer(player)) {
    		for(Command var:changeControllerCommandList)
                var.execute();
    	}
        controller = player;
        this.updateObservers();
    }
    
    public ArrayList<Card> getEquippedBy() {
        return equippedBy;
    }
    
    public void setEquippedBy(ArrayList<Card> list) {
        equippedBy = list;
    }
    
    public ArrayList<Card> getEquipping() {
        return equipping;
    }
    
    public Card getEquippingCard() {
        if (equipping.size() == 0)
        	return null;
        return equipping.get(0);
    }
    
    public void setEquipping(ArrayList<Card> list) {
        equipping = list;
    }
    
    public boolean isEquipped() {
        return equippedBy.size() != 0;
    }
    
    public boolean isEquipping() {
        return equipping.size() != 0;
    }
    
    public void addEquippedBy(Card c) {
        equippedBy.add(c);
        this.updateObservers();
    }
    
    public void removeEquippedBy(Card c) {
        equippedBy.remove(c);
        this.updateObservers();
    }
    
    public void addEquipping(Card c) {
        equipping.add(c);
        this.updateObservers();
    }
    
    public void removeEquipping(Card c) {
        equipping.remove(c);
        this.updateObservers();
    }
    
    public void equipCard(Card c) //equipment.equipCard(cardToBeEquipped);
    {
        equipping.add(c);
        c.addEquippedBy(this);
        this.equip();
    }
    
    public void unEquipCard(Card c) //equipment.unEquipCard(equippedCard);
    {
        this.unEquip();
        equipping.remove(c);
        c.removeEquippedBy(this);
    }
    
    public void unEquipAllCards() {
    	while(equippedBy.size() > 0){	// while there exists equipment, unequip the first one 
            equippedBy.get(0).unEquipCard(this);
        }
    }
    
    //
    
    public ArrayList<Card> getEnchantedBy() {
        return enchantedBy;
    }
    
    public void setEnchantedBy(ArrayList<Card> list) {
        enchantedBy = list;
    }
    
    public ArrayList<Card> getEnchanting() {
        return enchanting;
    }
    
    public Card getEnchantingCard() {
        if (enchanting.size() == 0)
        	return null;
        return enchanting.get(0);
    }
    
    public void setEnchanting(ArrayList<Card> list) {
        enchanting = list;
    }
    
    public boolean isEnchanted() {
        return enchantedBy.size() != 0;
    }
    
    public boolean isEnchanting() {;
        return enchanting.size() != 0;
    }
    
    public void addEnchantedBy(Card c) {
        enchantedBy.add(c);
        this.updateObservers();
    }
    
    public void removeEnchantedBy(Card c) {
        enchantedBy.remove(c);
        this.updateObservers();
    }
    
    /**
     * checks to see if this card is enchanted by an aura with a given name
     * 
     * @param cardName the name of the aura
     * @return true if this card is enchanted by an aura with the given name, false otherwise
     */
    public boolean isEnchantedBy(String cardName) {
    	ArrayList<Card> allAuras = this.getEnchantedBy();
    	for(Card aura:allAuras) {
    		if(aura.getName().equals(cardName)) return true;
    	}
    	return false;
    }
    
    public void addEnchanting(Card c) {
        enchanting.add(c);
        this.updateObservers();
    }
    
    public void removeEnchanting(Card c) {
        enchanting.remove(c);
        this.updateObservers();
    }
    
    public void enchantCard(Card c) {
        enchanting.add(c);
        c.addEnchantedBy(this);
        this.enchant();
    }
    
    public void unEnchantCard(Card c) {
        this.unEnchant();
        enchanting.remove(c);
        c.removeEnchantedBy(this);
    }
    
    public void unEnchantAllCards() {
        for(int i = 0; i < equippedBy.size(); i++) {
            enchantedBy.get(i).unEnchantCard(this);
        }
    }
    
    //array size might equal 0, will NEVER be null
    public Card[] getAttachedCards() {
        Card c[] = new Card[attached.size()];
        attached.toArray(c);
        return c;
    }
    
    public boolean hasAttachedCards() {
        return getAttachedCards().length != 0;
    }
    
    public void attachCard(Card c) {
        attached.add(c);
        this.updateObservers();
    }
    
    public void unattachCard(Card c) {
        attached.remove(c);
        this.updateObservers();
    }
    
    public void setType(ArrayList<String> a) {
        type = new ArrayList<String>(a);
    }
    
    public void addType(String a) {
        type.add(a);
        this.updateObservers();
    }
    
    public void removeType(String a) {
        type.remove(a);
        this.updateObservers();
    }
    
    public ArrayList<String> getType() {
        return new ArrayList<String>(type);
    }

    public void setPrevType(ArrayList<String> a) {
        prevType = new ArrayList<String>(a);
    }
    
    public void addPrevType(String a) {
        prevType.add(a);
    }
    
    public void removePrevType(String a) {
        prevType.remove(a);
    }
    
    public ArrayList<String> getPrevType() {
        return new ArrayList<String>(prevType);
    }
    
    //values that are printed on card
    public int getBaseAttack() {
        return baseAttack;
    }
    
    public int getBaseDefense() {
        return baseDefense;
    }
    
    //values that are printed on card
    public void setBaseAttack(int n) {
        baseAttack = n;
        this.updateObservers();
    }
    
    public void setBaseDefense(int n) {
        baseDefense = n;
        this.updateObservers();
    }
    
    public int getNetAttack() {
        int total = getBaseAttack();
        total += getTempAttackBoost() + getSemiPermanentAttackBoost() + getOtherAttackBoost()
                + getCounters(Counters.P1P1) + getCounters(Counters.P1P2) 
                + getCounters(Counters.P1P0) - getCounters(Counters.M1M1)
                + (2*getCounters(Counters.P2P2));
        return total;
    }
    
    public int getNetDefense() {
        int total = getBaseDefense();
        total += getTempDefenseBoost() + getSemiPermanentDefenseBoost() + getOtherDefenseBoost()
                + getCounters(Counters.P1P1) + (2*getCounters(Counters.P1P2)) 
                - getCounters(Counters.M1M1) + getCounters(Counters.P0P1 ) 
                - getCounters(Counters.P0M1) - (2*getCounters(Counters.P0M2))
                + (2*getCounters(Counters.P2P2));
        return total;
    }
    
    public void setRandomPicture(int n) {
        randomPicture = n;
    }
    
    public int getRandomPicture() {
        return randomPicture;
    }
    
    public void setUpkeepDamage(int n) {
        upkeepDamage = n;
    }
    
    public int getUpkeepDamage() {
        return upkeepDamage;
    }
    
    public void addMultiKickerMagnitude(int n)
    {
    	multiKickerMagnitude += n;
    }
    
    public void setMultiKickerMagnitude(int n) 
    {
    	multiKickerMagnitude = n;
    }
    
    public int getMultiKickerMagnitude()
    {
    	return multiKickerMagnitude;
    }
    
    //public int getAttack(){return attack;}
    
    //for cards like Giant Growth, etc.
    public int getTempAttackBoost() {
        return tempAttackBoost;
    }
    
    public int getTempDefenseBoost() {
        return tempDefenseBoost;
    }
    
    public void addTempAttackBoost(int n) {
        tempAttackBoost += n;
        this.updateObservers();
    }
    
    public void addTempDefenseBoost(int n) {
        tempDefenseBoost += n;
        this.updateObservers();
    }
    
    public void setTempAttackBoost(int n) {
        tempAttackBoost = n;
        this.updateObservers();
    }
    
    public void setTempDefenseBoost(int n) {
        tempDefenseBoost = n;
        this.updateObservers();
    }
    
    //for cards like Glorious Anthem, etc.
    public int getSemiPermanentAttackBoost() {
        return semiPermanentAttackBoost;
    }
    
    public int getSemiPermanentDefenseBoost() {
        return semiPermanentDefenseBoost;
    }
    
    public void addSemiPermanentAttackBoost(int n) {
        semiPermanentAttackBoost += n;
    }
    
    public void addSemiPermanentDefenseBoost(int n) {
        semiPermanentDefenseBoost += n;
    }
    
    public void setSemiPermanentAttackBoost(int n) {
        semiPermanentAttackBoost = n;
    }
    
    public void setSemiPermanentDefenseBoost(int n) {
        semiPermanentDefenseBoost = n;
    }
    
    //for cards like Relentless Rats, Master of Etherium, etc.
    public int getOtherAttackBoost() {
        return otherAttackBoost;
    }
    
    public int getOtherDefenseBoost() {
        return otherDefenseBoost;
    }
    
    public void addOtherAttackBoost(int n) {
        otherAttackBoost += n;
    }
    
    public void addOtherDefenseBoost(int n) {
        otherDefenseBoost += n;
    }
    
    public void setOtherAttackBoost(int n) {
        otherAttackBoost = n;
    }
    
    public void setOtherDefenseBoost(int n) {
        otherDefenseBoost = n;
    }
    
    //public void setAttack(int n)    {attack  = n; this.updateObservers();}
    //public void setDefense(int n)  {defense = n; this.updateObservers();}
    
    public boolean isUntapped() {
        return !tapped;
    }
    
    public boolean isTapped() {
        return tapped;
    }
    
    public void setTapped(boolean b) {
        tapped = b;
        updateObservers();
    }
    
    public void tap() {
    	if (isUntapped())
    		GameActionUtil.executeTapSideEffects(this);
    	setTapped(true);
    }
    
    public void untap() {
    	if( isTapped() ) {
    		GameActionUtil.executeUntapSideEffects(this);
    	}
    	if (isTapped() && isReflectedLand()) {
    		Ability_Reflected_Mana am = (Ability_Reflected_Mana) getManaAbility().get(0);
    		am.reset();
    	}
    	for(Command var:untapCommandList) {
            var.execute();
    	}
        setTapped(false);
    }
    
    public boolean isUnCastable() {
        return unCastable;
    }
    
    public void setUnCastable(boolean b) {
        unCastable = b;
        updateObservers();
    }
    
    //keywords are like flying, fear, first strike, etc...
    public ArrayList<String> getKeyword() {
        ArrayList<String> a1 = new ArrayList<String>(getIntrinsicKeyword());
        ArrayList<String> a2 = new ArrayList<String>(getExtrinsicKeyword());
        ArrayList<String> a3 = new ArrayList<String>(getOtherExtrinsicKeyword());
        ArrayList<String> a4 = new ArrayList<String>(getHiddenExtrinsicKeyword());
        a1.addAll(a2);
        a1.addAll(a3);
        a1.addAll(a4);
        
        for(Ability_Mana sa:getManaAbility())
            if(sa.isBasic()) a1.add((sa).orig);
        
        return a1;
    }
    
    //keywords are like flying, fear, first strike, etc...
    // Hidden keywords will be left out
    public ArrayList<String> getUnhiddenKeyword() {
        ArrayList<String> a1 = new ArrayList<String>(getIntrinsicKeyword());
        ArrayList<String> a2 = new ArrayList<String>(getExtrinsicKeyword());
        ArrayList<String> a3 = new ArrayList<String>(getOtherExtrinsicKeyword());
        a1.addAll(a2);
        a1.addAll(a3);
        
        for(Ability_Mana sa:getManaAbility())
            if(sa.isBasic()) a1.add((sa).orig);
        
        return a1;
    }
    
    public ArrayList<String> getIntrinsicAbilities()
    {
    	return intrinsicAbility;
    }
    
    //public void setKeyword(ArrayList a) {keyword = new ArrayList(a); this.updateObservers();}
    //public void addKeyword(String s)     {keyword.add(s);                    this.updateObservers();}
    //public void removeKeyword(String s) {keyword.remove(s);              this.updateObservers();}
    //public int getKeywordSize() 	{return keyword.size();}
    
    //public String[] basics = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
    
    public ArrayList<String> getIntrinsicKeyword() {
        return new ArrayList<String>(intrinsicKeyword);
    }
    
    public void setIntrinsicKeyword(ArrayList<String> a) {
        intrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }
    
    public void setIntrinsicAbilities(ArrayList<String> a)
    {
    	intrinsicAbility = new ArrayList<String>(a);
    }
    
    public void addIntrinsicKeyword(String s) {/*if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s){}); else*/
        if (s.trim().length()!=0)
        	intrinsicKeyword.add((getName().trim().length()== 0 ? s :s.replaceAll(getName(), "CARDNAME")));
    }
    
    public void addIntrinsicAbility(String s)
    {
    	if (s.trim().length() != 0)
    		intrinsicAbility.add(s);
    }
    
    public void addNonStackingIntrinsicKeyword(String s) {/*if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s){}); else*/
    	if(!getIntrinsicKeyword().contains(s)){
	    	if (s.trim().length()!=0)
	        	intrinsicKeyword.add((getName().trim().length()== 0 ? s :s.replaceAll(getName(), "CARDNAME")));
    	}
    }
    
    public void removeIntrinsicKeyword(String s) {
        intrinsicKeyword.remove(s);
        this.updateObservers();
    }
    
    public int getIntrinsicKeywordSize() {
        return intrinsicKeyword.size();
    }
    
    public ArrayList<String> getExtrinsicKeyword() {
        return new ArrayList<String>(extrinsicKeyword);
    }
    
    public void setExtrinsicKeyword(ArrayList<String> a) {
        extrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }
    
    public void addExtrinsicKeyword(String s) {
        //if(!getKeyword().contains(s)){
    	if(s.startsWith("HIDDEN")) addHiddenExtrinsicKeyword(s);
    	else if(s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s) {
            private static final long serialVersionUID = 221124403788942412L;
        });
        else 
        	extrinsicKeyword.add((getName().trim().length()==0 ? s :s.replaceAll(getName(), "CARDNAME")));
        //}
    }
    
    public void addStackingExtrinsicKeyword(String s) {
    	if(s.startsWith("HIDDEN")) addHiddenExtrinsicKeyword(s);
    	else if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s)
    	{
    		private static final long serialVersionUID = 2443750124751086033L;  
    	});
    	else extrinsicKeyword.add(s);
    }
    
    public void removeExtrinsicKeyword(String s) {
    	if(s.startsWith("HIDDEN")) removeHiddenExtrinsicKeyword(s);
    	else extrinsicKeyword.remove(s);
        this.updateObservers();
    }
    
    public int getExtrinsicKeywordSize() {
        return extrinsicKeyword.size();
    }
    
    public ArrayList<String> getOtherExtrinsicKeyword() {
        return new ArrayList<String>(otherExtrinsicKeyword);
    }
    
    public void setOtherExtrinsicKeyword(ArrayList<String> a) {
        otherExtrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }
    
    public void addOtherExtrinsicKeyword(String s) {
        //if(!getKeyword().contains(s)){
        if(s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s) {

			private static final long serialVersionUID = -3032496855034700637L;
        });
        else 
        otherExtrinsicKeyword.add((getName().trim().length()==0 ? s :s.replaceAll(getName(), "CARDNAME")));
        //}
    }
    
    public void addStackingOtherExtrinsicKeyword(String s) {
    	if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s)
    	{
			private static final long serialVersionUID = 7004485151675361747L;
    	});
    	else extrinsicKeyword.add(s);
    }
    
    public void removeOtherExtrinsicKeyword(String s) {
        otherExtrinsicKeyword.remove(s);
        this.updateObservers();
    }
    
    public int getOtherExtrinsicKeywordSize() {
        return otherExtrinsicKeyword.size();
    }
    
    public ArrayList<String> getPrevIntrinsicKeyword() {
        return new ArrayList<String>(prevIntrinsicKeyword);
    }
    
    public void setPrevIntrinsicKeyword(ArrayList<String> a) {
        prevIntrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }
    
    public void addPrevIntrinsicKeyword(String s) {
        prevIntrinsicKeyword.add(s);
    }
    
    public void removePrevIntrinsicKeyword(String s) {
        prevIntrinsicKeyword.remove(s);
        this.updateObservers();
    }
    
    public int getPrevIntrinsicKeywordSize() {
        return prevIntrinsicKeyword.size();
    }
    
    // Hidden Keywords will be returned without the indicator HIDDEN
    public ArrayList<String> getHiddenExtrinsicKeyword() {
    	ArrayList<String> Keyword = new ArrayList<String>();
    	for (int i = 0; i < HiddenExtrinsicKeyword.size(); i++) {
    		String keyword = HiddenExtrinsicKeyword.get(i);
    		Keyword.add(keyword.replace("HIDDEN ", ""));
    	}
        return Keyword;
    }
    
    public void addHiddenExtrinsicKeyword(String s) {
    	HiddenExtrinsicKeyword.add(s);
    }
    
    public void removeHiddenExtrinsicKeyword(String s) {
    	HiddenExtrinsicKeyword.remove(s);
        //this.updateObservers();
    }
    
    public boolean isPermanent() {
        return !(isInstant() || isSorcery() || isImmutable());
    }
    
    public boolean isSpell() {
        return (isInstant() || isSorcery());
    }
    
    public boolean isCreature() {
        return type.contains("Creature");
    }
    
    public boolean isWall() {
    	return type.contains("Wall");
    }
    
    public boolean isBasicLand() {
        return type.contains("Basic");
    }
    
    public boolean isLand() {
        return type.contains("Land");
    }
    
    public boolean isSorcery() {
        return type.contains("Sorcery");
    }
    
    public boolean isInstant() {
        return type.contains("Instant") /*|| getKeyword().contains("Flash")*/;
    }
    
    public boolean isArtifact() {
        return type.contains("Artifact");
    }
    
    public boolean isEquipment() {
        return type.contains("Equipment");
    }
    
    public boolean isPlaneswalker() {
        return type.contains("Planeswalker");
    }
    
    public boolean isEmblem() {
    	return type.contains("Emblem");
    }
    
    public boolean isTribal() {
        return type.contains("Tribal");
    }
    
    public boolean isSnow() {
        return type.contains("Snow");
    }
    
    //global and local enchantments
    public boolean isEnchantment() {
        return typeContains("Enchantment");
    }
    
    public boolean isLocalEnchantment() {
        return typeContains("Aura");
    }
    
    public boolean isAura() {
        return typeContains("Aura");
    }
    
    public boolean isGlobalEnchantment() {
        return typeContains("Enchantment") && (!isLocalEnchantment());
    }
    
    private boolean typeContains(String s) {
        Iterator<?> it = this.getType().iterator();
        while(it.hasNext())
            if(it.next().toString().startsWith(s)) return true;
        
        return false;
    }
    
    public void setUniqueNumber(int n) {
        uniqueNumber = n;
        this.updateObservers();
    }
    
    public int getUniqueNumber() {
        return uniqueNumber;
    }
    
    public void setValue(long n)
    {
    	value = n;
    }
    
    public long getValue()
    {
    	return value;
    }
    @Override
    public boolean equals(Object o) {
        if(o instanceof Card) {
            Card c = (Card) o;
            int a = getUniqueNumber();
            int b = c.getUniqueNumber();
            return (a == b);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return getUniqueNumber();
    }
    
    @Override
    public String toString() {
        return this.getName() + " (" + this.getUniqueNumber() + ")";
    }
    
    public boolean hasFlashback() {
        return flashback;
    }
    
    public void setFlashback(boolean b) {
        flashback = b;
    }
    
    public boolean hasUnearth() {
        return unearth;
    }
    
    public void setUnearth(boolean b) {
        unearth = b;
    }
    
    public boolean isUnearthed()
    {
    	return unearthed;
    }
    
    public void setUnearthed(boolean b)
    {
    	unearthed = b;
    }
    
    public boolean hasMadness() {
        return madness;
    }
    
    public void setMadness(boolean b) {
    	madness = b;
    }
    
    public String getMadnessCost() {
        return madnessCost;
    }
    
    public void setMadnessCost(String cost) {
    	madnessCost = cost;
    }
    
    public boolean hasSuspend() {
        return suspend;
    }
    
    public void setSuspend(boolean b) {
    	suspend = b;
    }
    
    public boolean wasSuspendCast() {
        return suspendCast;
    }
    
    public void setSuspendCast(boolean b) {
    	suspendCast = b;
    }
    
    public void setKicked(boolean b) {
        kicked = b;
    }
    
    public boolean isKicked() {
        return kicked;
    }

    public void setReflectedLand(boolean b) {
    	reflectedLand = b;
    }
    
	public boolean isReflectedLand() {
		return reflectedLand;
	}
	
	public boolean hasKeyword(String keyword)
	{
		return getKeyword().contains(keyword);
	}
	
	public boolean hasStartOfKeyword(String keyword)
	{
        ArrayList<String> a = getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(keyword)) return true;
        return false;
    }
	
	public int getKeywordPosition(String k) {
        ArrayList<String> a = getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        return -1;
    }
	
	public boolean keywordsContain(String keyword) {
        ArrayList<String> a = getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().contains(keyword)) return true;
        return false;
    }

	
	public boolean hasAnyKeyword(String keywords[])
	{
		for (int i=0; i<keywords.length; i++)
			if (hasKeyword(keywords[i]))
				return true;
		
		return false;
	}
	
	public boolean hasAnyKeyword(ArrayList<String> keywords)
	{
		for (int i=0; i<keywords.size(); i++)
			if (hasKeyword(keywords.get(i)))
				return true;
		
		return false;
	}
	
	//This counts the number of instances of a keyword a card has
	public int getAmountOfKeyword(String k) {
		int count = 0;
        ArrayList<String> keywords = getKeyword();
        for(int j = 0; j < keywords.size(); j++) {
            if(keywords.get(j).equals(k)) count++;
        }

        return count;
    }
	
	// This is for keywords with a number like Bushido, Annihilator and Rampage. It returns the total.
	public int getKeywordMagnitude(String k) {
		int count = 0;
        ArrayList<String> keywords = getKeyword();
        for(String kw:keywords) {
            if(kw.startsWith(k)) {
                String[] parse = kw.split(" ");
                String s = parse[1];
                count += Integer.parseInt(s);
            }
        }
        return count;
    }
	
    private String toMixedCase(String s)
    {
    	StringBuilder sb = new StringBuilder();
    	// to handle hyphenated Types
    	String[] types = s.split("-");
    	for(int i = 0; i < types.length; i++){
    		if (i != 0)
    			sb.append("-");
        	sb.append(types[i].substring(0,1).toUpperCase());
        	sb.append(types[i].substring(1).toLowerCase());
    	}
    	
    	return sb.toString();
    }	
	
    //usable to check for changelings
    public boolean isType(String cardType) {
    	cardType = toMixedCase(cardType);
    	
    	if (type.contains(cardType)
                    || ( (isCreature() || isTribal())
                    		&& CardUtil.isACreatureType(cardType) && getKeyword().contains("Changeling"))) return true;
        return false;
    }
    
    
    // Takes an array of arguments like Permanent.Blue+withFlying, only one of them has to be true
    public boolean isValidCard(final String Restrictions[], final Player sourceController, final Card source) {
    	
        if (getName().equals("Mana Pool") || isImmutable()) return false;

        for(int i = 0; i < Restrictions.length; i++) {
        	if(isValid(Restrictions[i],sourceController,source)) return true;
        }
        return false;
        
    }//isValidCard
    
    
    // Takes one argument like Permanent.Blue+withFlying
    public boolean isValid(final String Restriction, final Player sourceController, final Card source) {
    	
        if (getName().equals("Mana Pool") || isImmutable()) return false;
        if (Restriction.equals("False")) return false;

            String incR[] = Restriction.split("\\."); // Inclusive restrictions are Card types
            
            if (incR[0].equals("Spell") && isType("Land"))
            	return false;
            if (incR[0].equals("Permanent") && (isType("Instant") || isType("Sorcery")))
            	return false;
            if(!incR[0].equals("Card") && !incR[0].equals("Spell") && !incR[0].equals("Permanent") && !(isType(incR[0])))
            	return false; //Check for wrong type
            
            if(incR.length > 1) {
                final String excR = incR[1];
                String exR[] = excR.split("\\+"); // Exclusive Restrictions are ...
                for(int j = 0; j < exR.length; j++)
                    if(hasProperty(exR[j],sourceController,source) == false) return false;
            }
            return true;
    }//isValidCard(String Restriction)

    // Takes arguments like Blue or withFlying
	public boolean hasProperty(String Property, final Player sourceController, final Card source) {
		if (Property.contains("White") || // ... Card colors
                Property.contains("Blue") ||
                Property.contains("Black") ||
                Property.contains("Red") ||
                Property.contains("Green") ||
                Property.contains("Colorless")) 
 			{
					if(Property.startsWith("non"))
					{	
						if (CardUtil.getColors(this).contains(Property.substring(3).toLowerCase())) return false;
					}	
					else
						if (!CardUtil.getColors(this).contains(Property.toLowerCase())) return false;
 			}
		else if (Property.contains("MultiColor")) // ... Card is multicolored
        {
			if (Property.startsWith("non") && (CardUtil.getColors(this).size() > 1)) return false;
			if (!Property.startsWith("non") && (CardUtil.getColors(this).size() <= 1)) return false;
        }
 			
        else if (Property.contains("MonoColor")) // ... Card is monocolored
        {
        	if (Property.startsWith("non") && (CardUtil.getColors(this).size() == 1 && !isColorless())) return false;
        	if (!Property.startsWith("non") && (CardUtil.getColors(this).size() > 1 || isColorless())) return false;
        }
             
        else if (Property.startsWith("YouCtrl")) { if (!getController().isPlayer(sourceController)) return false; }
        else if (Property.startsWith("YouDontCtrl")) { if (getController().isPlayer(sourceController)) return false; }
        else if (Property.startsWith("YouOwn")) { if (!getOwner().isPlayer(sourceController)) return false; }
        else if (Property.startsWith("YouDontOwn")) { if (getOwner().isPlayer(sourceController)) return false; }
		
        else if (Property.startsWith("ControllerControls")) { 
        	String type = Property.substring(18);
        	CardList list = AllZoneUtil.getPlayerCardsInPlay(getController());
        	if (list.getType(type).isEmpty()) return false; 
        }
		
        else if (Property.startsWith("Other")) { if(this.equals(source)) return false; }
        else if (Property.startsWith("Self")) { if(!this.equals(source)) return false; }
		
        else if (Property.startsWith("Attached")) {
        	if (!equipping.contains(source) && !enchanting.contains(source)) return false; }
        else if (Property.startsWith("SharesColorWith")) { if(!sharesColorWith(source)) return false; }
				
             else if (Property.startsWith("with")) // ... Card keywords
             {
              	if (Property.startsWith("without") && getKeyword().contains(Property.substring(7))) return false;
             	if (!Property.startsWith("without") && !getKeyword().contains(Property.substring(4))) return false;
             }
             
             else if (Property.startsWith("tapped"))
             	{ if(!isTapped()) return false;}
             else if (Property.startsWith("untapped"))
             	{ if(!isUntapped()) return false;}
             else if (Property.startsWith("faceDown"))
             	{ if(!isFaceDown()) return false;}
             else if (Property.startsWith("enteredBattlefieldThisTurn"))
             	{ if(!(getTurnInZone() == AllZone.Phase.getTurn())) return false;}
             
             else if (Property.startsWith("enchanted"))
             	{ if(!isEnchanted()) return false;}
             else if (Property.startsWith("unenchanted"))
             	{ if(isEnchanted()) return false;}
             else if (Property.startsWith("enchanting"))
             	{ if(!isEnchanting()) return false;}
 			
             else if (Property.startsWith("equipped"))
         		{ if(!isEquipped()) return false;}
             else if (Property.startsWith("unequipped"))
         		{ if(isEquipped()) return false;}
             else if (Property.startsWith("equipping"))
         		{ if(!isEquipping()) return false;}
             
             else if (Property.startsWith("token"))
         		{ if(!isToken()) return false;}
             else if (Property.startsWith("nonToken"))
         		{ if(isToken()) return false;}
             
             else if (Property.startsWith("power") || 	// 8/10
             		 Property.startsWith("toughness") ||
             		 Property.startsWith("cmc"))
             {
             	int x = 0;
             	int y = 0;
             	int z = 0;
             	
             	if (Property.startsWith("power") )
                 {
                 	z = 7;
                 	y = getNetAttack();
                 }
                 else if (Property.startsWith("toughness"))
                 {
                 	z = 11;
                 	y = getNetDefense();
                 }
                 else if (Property.startsWith("cmc"))
                 {
                 	z = 5;
                 	y = getCMC();
                 }
             	
             	if (Property.substring(z).equals("X")) {
             		x = CardFactoryUtil.xCount(source, source.getSVar("X"));
             	}
             	else
             		x = Integer.parseInt(Property.substring(z));
             	
             	if (!compare(y, Property, x))
             		return false;
             }
			
             else if (Property.startsWith("counters")) // syntax example: countersGE9 P1P1 or countersLT12TIME (greater number than 99 not supported)
             {
            	int number = 0;
              	if (Property.substring(10,11).equals("X"))
              		number = CardFactoryUtil.xCount(source, getSVar("X"));
             	else
             		number = Integer.parseInt(Property.substring(10,11));
              	
              	String type = Property.substring(11);
              	String comparator = Property.substring(8,10); // comparator = EQ, LE, GE etc.
             	int actualnumber = getCounters(Counters.getType(type));
             	
             	if (!compare(actualnumber, comparator, number))
             		return false;
             }
 			
             else if (Property.startsWith("attacking")) { if(!isAttacking())  return false;}
 			
             else if (Property.startsWith("notattacking")) { if(isAttacking())  return false;}
 			
             else if (Property.startsWith("blocking")) { if(!isBlocking())  return false;}
		
             else if (Property.startsWith("notblocking")) { if(isBlocking())  return false;}
		
             else if (Property.startsWith("blocked")) { if(!AllZone.Combat.isBlocked(this))  return false;}
 			
             else if(Property.startsWith("named")) //by name
             	{ if(!getName().equals(Property.substring(5))) return false;}
             else if(Property.startsWith("non")) // ... Other Card types
             	{ if(isType(Property.substring(3))) return false;}
             else {
            	 if(Property.equals("ChosenType")) {
            		 if(!isType(source.getChosenType())) return false;
            	 }
            	 else {
            		 if(!isType(Property)) return false;
            	 }
             }
       return true;
	}//hasProperty

	public static boolean compare(int leftSide, String comp, int rightSide){
		// should this function be somewhere else?
		// leftSide COMPARED to rightSide:
		if (comp.contains("LT")) return leftSide < rightSide;
		
		else if (comp.contains("LE")) return leftSide <= rightSide;
		
		else if (comp.contains("EQ")) return leftSide == rightSide;
		
		else if (comp.contains("GE")) return leftSide >= rightSide;
		
		else if (comp.contains("GT")) return leftSide > rightSide;
		
		else if (comp.contains("NE")) return leftSide != rightSide; // not equals
		
		return false;
	}
	
	public void setImmutable(boolean isImmutable) {
		this.isImmutable = isImmutable;
	}

	public boolean isImmutable() {
		return isImmutable;
	}
	
	/*
	 * there are easy checkers for Color.  The CardUtil functions should
	 * be made part of the Card class, so calling out is not necessary
	 */
	
	public boolean isBlack() {
		return CardUtil.getColors(this).contains(Constant.Color.Black);
	}
	
	public boolean isBlue() {
		return CardUtil.getColors(this).contains(Constant.Color.Blue);
	}
	
	public boolean isRed() {
		return CardUtil.getColors(this).contains(Constant.Color.Red);
	}
	
	public boolean isGreen() {
		return CardUtil.getColors(this).contains(Constant.Color.Green);
	}
	
	public boolean isWhite() {
		return CardUtil.getColors(this).contains(Constant.Color.White);
	}
	
	public boolean isColorless() {
		return CardUtil.getColors(this).contains(Constant.Color.Colorless);
	}
	
	public boolean sharesColorWith(final Card c1) {
		boolean shares = false;
		shares = shares || (isBlack() && c1.isBlack());
		shares = shares || (isBlue() && c1.isBlue());
		shares = shares || (isGreen() && c1.isGreen());
		shares = shares || (isRed() && c1.isRed());
		shares = shares || (isWhite() && c1.isWhite());
		return shares;
	}
	
	public boolean isAttacking() {
		CardList attackers = new CardList(AllZone.Combat.getAttackers());
        attackers.addAll(AllZone.pwCombat.getAttackers());
        return attackers.contains(this);
	}
	
	public boolean isBlocking() {
		CardList blockers = AllZone.Combat.getAllBlockers();
        blockers.add(AllZone.pwCombat.getAllBlockers());
     	return blockers.contains(this);
	}
	
	///////////////////////////
	//
	// Damage code
	//
	//////////////////////////
	
	//all damage to cards is now handled in Card.java, no longer AllZone.GameAction...
	public void addReceivedDamageFromThisTurn(Card c, int damage) {
        receivedDamageFromThisTurn.put(c, damage);
    }
    
    public void setReceivedDamageFromThisTurn(HashMap<Card, Integer> receivedDamageFromThisTurn) {
        this.receivedDamageFromThisTurn = receivedDamageFromThisTurn;
    }
    
    public HashMap<Card, Integer> getReceivedDamageFromThisTurn() {
        return receivedDamageFromThisTurn;
    }
    
    public void resetReceivedDamageFromThisTurn() {
        receivedDamageFromThisTurn.clear();
    }
    
    //the amount of damage needed to kill the creature
    public int getKillDamage() {
        return getNetDefense() + preventNextDamage - getDamage();
    }
    
    public void setDamage(int n) {
        //if(this.getKeyword().contains("Prevent all damage that would be dealt to CARDNAME.")) n = 0;
        damage = n;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public void addAssignedDamage(int damage, Card sourceCard) {    	
        if(damage < 0) damage = 0;
        
        int assignedDamage = damage;
        addReceivedDamageFromThisTurn(sourceCard, damage);
        
        if(!CardFactoryUtil.canDamage(sourceCard, this)) assignedDamage = 0;

        Log.debug(this + " - was assigned " + assignedDamage + " damage, by " + sourceCard);
        if(!assignedDamageHashMap.containsKey(sourceCard)) assignedDamageHashMap.put(sourceCard, assignedDamage);
        else {
            assignedDamageHashMap.put(sourceCard, assignedDamageHashMap.get(sourceCard) + assignedDamage);
        }
        
        Log.debug("***");
        /*
        if(sourceCards.size() > 1)
          System.out.println("(MULTIPLE blockers):");
        System.out.println("Assigned " + damage + " damage to " + card);
        for (int i=0;i<sourceCards.size();i++){
          System.out.println(sourceCards.get(i).getName() + " assigned damage to " + card.getName());
        }
        System.out.println("***");
        */
    }
    
    public void clearAssignedDamage() {
        assignedDamageHashMap.clear();
    }
    
    public int getTotalAssignedDamage() {
        int total = 0;
        
        Collection<Integer> c = assignedDamageHashMap.values();
        
        Iterator<Integer> itr = c.iterator();
        while(itr.hasNext())
            total += itr.next();
        
        return total;
    }
    
    public HashMap<Card, Integer> getAssignedDamageHashMap() {
        return assignedDamageHashMap;
    }
    
    public void addCombatDamage(HashMap<Card, Integer> map) {
        CardList list = new CardList();
        
        for(Entry<Card, Integer> entry : map.entrySet()){
            Card source = entry.getKey();
            list.add(source);
            int damageToAdd = entry.getValue();
            
            damageToAdd = preventDamage(damageToAdd, source, true);
            
            if (damageToAdd > 0) {
	            if(isCreature() && source.getName().equals("Mirri the Cursed") ) {
	                final Card thisCard = source;
	                Ability ability2 = new Ability(thisCard, "0") {
	                    @Override
	                    public void resolve() {
	                        thisCard.addCounter(Counters.P1P1, 1);
	                    }
	                }; // ability2
	                
	                StringBuilder sb2 = new StringBuilder();
	                sb2.append(thisCard.getName()).append(" - gets a +1/+1 counter");
	                ability2.setStackDescription(sb2.toString());
	                
	                AllZone.Stack.add(ability2);
	            }
	            if(source.getKeyword().contains("Deathtouch") && isCreature()) {
	                AllZone.GameAction.destroy(this);
	            }
            }
	        map.put(source, damageToAdd);
        }
        
        if(AllZoneUtil.isCardInPlay(this)) {
        	addDamage(map);
        }
        
        for(Entry<Card, Integer> entry : map.entrySet()){
        	Card source = entry.getKey();
        	CombatUtil.executeCombatDamageEffects(source);
        }
    }
    
	//This should be also usable by the AI to forecast an effect (so it must not change the game state) 
	public int staticDamagePrevention(final int damage, final Card source, final boolean isCombat) {
		
    	if(AllZoneUtil.isCardInPlay("Leyline of Punishment")) return damage;
		
		int restDamage = damage;
		Player player = source.getController();
		
    	if(isCombat) {
    		if(getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME."))return 0;
    		if(getKeyword().contains("Prevent all combat damage that would be dealt to CARDNAME."))return 0;
    		if(source.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME."))return 0;
    		if(source.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME."))return 0;
    	}
    	if(getKeyword().contains("Prevent all damage that would be dealt to CARDNAME."))return 0;
    	if(getKeyword().contains("Prevent all damage that would be dealt to and dealt by CARDNAME."))return 0;
    	if(source.getKeyword().contains("Prevent all damage that would be dealt to and dealt by CARDNAME."))return 0;
    	if(source.getKeyword().contains("Prevent all damage that would be dealt by CARDNAME."))return 0;
    	
    	if(hasStartOfKeyword("Absorb")) {
    		int absorbed = this.getKeywordMagnitude("Absorb");
    		if (restDamage > absorbed) restDamage = restDamage - absorbed;
    		else return 0;
    	}
    	
    	if(hasStartOfKeyword("PreventAllDamageBy")) {
    		String valid = getKeyword().get(getKeywordPosition("PreventAllDamageBy"));
    		valid = valid.split(" ", 2)[1]; 
    		if (source.isValid(valid,this.getController(),this))
    			return 0;
    	}
    	
		/* Should use the PreventAllDamageBy
    	if((getKeyword().contains("Prevent all damage that would be dealt to CARDNAME by artifact creatures.") 
				&& source.isCreature() && source.isArtifact()))return 0;
    	if((getKeyword().contains("Prevent all damage that would be dealt to CARDNAME by artifacts.") 
				&& source.isArtifact()))return 0;
    	if((getKeyword().contains("Prevent all damage that would be dealt to CARDNAME by creatures.")
				&& source.isCreature()))return 0;
		*/
		
		// specific Cards
    	if(isCreature()) { //and not a planeswalker
    		if((source.isCreature() && AllZoneUtil.isCardInPlay("Well-Laid Plans") && source.sharesColorWith(this)))return 0;
    	
    		if((!isCombat && AllZoneUtil.isCardInPlay("Mark of Asylum", player)))return 0;
    	
    		if((AllZoneUtil.isCardInPlay("Light of Sanction", player) && source.getController().isPlayer(player)))
    			return 0;
    	
    		if (AllZoneUtil.isCardInPlay("Plated Pegasus") && source.isSpell() 
    			&& restDamage > 0) restDamage = restDamage - 1;
    	} //Creature end
    	
		if (AllZoneUtil.isCardInPlay("Energy Storm") && source.isSpell()) return 0;
    	
		return restDamage;
    }
    
    public int preventDamage(final int damage, Card source, boolean isCombat) {
    	
    	if(AllZoneUtil.isCardInPlay("Leyline of Punishment")) return damage;
    	
    	int restDamage = damage;
    	
    	restDamage = staticDamagePrevention(restDamage, source, isCombat);
    	
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
    
    public void addDamage(HashMap<Card, Integer> sourcesMap) {
        for(Entry<Card, Integer> entry : sourcesMap.entrySet()) {
        	addDamageWithoutPrevention(entry.getValue(), entry.getKey()); // damage prevention is already checked!
        }
    }
    
    public void addDamage(final int damageIn, final Card source) {
        int damageToAdd = damageIn;
        damageToAdd = preventDamage(damageToAdd, source, false);
        
        addDamageWithoutPrevention(damageToAdd,source);
    }
        
    public void addDamageWithoutPrevention(final int damageIn, final Card source) {
    	int damageToAdd = damageIn;
    	
        if( damageToAdd == 0 ) return;  //Rule 119.8
        
        if(this.isPlaneswalker()) {
        	this.subtractCounter(Counters.LOYALTY, damageToAdd);
        }
        
        if((source.getKeyword().contains("Wither") || source.getKeyword().contains("Infect")) && this.isCreature()) {
        	this.addCounterFromNonEffect(Counters.M1M1, damageToAdd);
        	damageToAdd = 0;
        }
        
        if(source.getName().equals("Spiritmonger")) {
        	Ability ability2 = new Ability(source, "0") {
        		@Override
        		public void resolve() {
        			source.addCounter(Counters.P1P1, 1);
        		}
        	}; // ability2
        	
        	StringBuilder sb2 = new StringBuilder();
        	sb2.append(source.getName()).append(" - gets a +1/+1 counter");
        	ability2.setStackDescription(sb2.toString());
        	
        	AllZone.Stack.add(ability2);
        }
        
        if(this.getName().equals("Fungusaur")) {
        	Ability ability2 = new Ability(this, "0") {
        		@Override
        		public void resolve() {
        			addCounter(Counters.P1P1, 1);
        		}
        	}; // ability2
        	
        	StringBuilder sb2 = new StringBuilder();
        	sb2.append(this.getName()).append(" - gets a +1/+1 counter");
        	ability2.setStackDescription(sb2.toString());
        	
        	AllZone.Stack.add(ability2);
        }
        
        if(source.getKeyword().contains("Deathtouch") && this.isCreature()) {
            AllZone.GameAction.destroy(this);
            //AllZone.Combat.removeFromCombat(card);
        }
        
        System.out.println("Adding " + damageToAdd + " damage to " + getName());
        Log.debug("Adding " + damageToAdd + " damage to " + getName());
        if(AllZoneUtil.isCardInPlay(this) && CardFactoryUtil.canDamage(source, this)) {
        	damage += damageToAdd;
        }
        
        if(this.getName().equals("Stuffy Doll")) {
        	final Player opponent = this.getOwner().getOpponent();
        	final int stuffyDamage = damageToAdd;
        	SpellAbility ability = new Ability(this, "0") {
        		@Override
        		public void resolve() {
        			opponent.addDamage(stuffyDamage, Card.this);
        		}
        	};
        	StringBuilder sb = new StringBuilder();
            sb.append(this.getName()+" - Deals ").append(stuffyDamage).append(" damage to ").append(opponent);
            ability.setStackDescription(sb.toString());
            
            AllZone.Stack.add(ability);
        }
        
        if(this.getName().equals("Jackal Pup") || this.getName().equals("Shinka Gatekeeper")) {
        	final Player player = this.getController();
        	final int selfDamage = damageToAdd;
        	SpellAbility ability = new Ability(this, "0") {
        		@Override
        		public void resolve() {
        			player.addDamage(selfDamage, Card.this);
        		}
        	};
        	StringBuilder sb = new StringBuilder();
            sb.append(this.getName()+" - Deals ").append(selfDamage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());
            
            AllZone.Stack.add(ability);
        }
        
        if(this.getName().equals("Filthy Cur")) {
        	final Player player = this.getController();
        	final int life = damageToAdd;
        	SpellAbility ability = new Ability(this, "0") {
        		@Override
        		public void resolve() {
        			player.loseLife(life, Card.this);
        		}
        	};
        	StringBuilder sb = new StringBuilder();
            sb.append(this.getName()+" - ").append(player).append(" loses ").append(life).append("life");
            ability.setStackDescription(sb.toString());
            
            AllZone.Stack.add(ability);
        }
        
        if(source.getKeyword().contains("Lifelink") && CardFactoryUtil.canDamage(source, this)) GameActionUtil.executeLifeLinkEffects(source, damageToAdd);
        
        if(isEnchantedBy("Mortal Wound")) {
        	AllZone.GameAction.destroy(this);
        }
        
        CardList cl = CardFactoryUtil.getAurasEnchanting(source, "Guilty Conscience");
        for(Card c:cl) {
            GameActionUtil.executeGuiltyConscienceEffects(source, c, damageToAdd);
        }
    }
    private ArrayList<SetInfo> Sets = new ArrayList<SetInfo>();
    private String curSetCode = "";
    
    public void addSet(SetInfo sInfo)
    {
    	Sets.add(sInfo);
    }
    
    public ArrayList<SetInfo> getSets()
    {
    	return Sets;
    }
    
    public void setSets(ArrayList<SetInfo> siList)
    {
    	Sets = siList;
    }
    
    public void setCurSetCode(String setCode) {
    	curSetCode = setCode;
    }
    
    public String getCurSetCode() {
    	return curSetCode;
    }
    
    public void setRandomSetCode() {
    	if (Sets.size() < 1)
    		return;
    	
    	Random r = new Random();
    	SetInfo si = Sets.get(r.nextInt(Sets.size()));
    	
    	curSetCode = si.Code;
    }
    
    public String getSetImageName(String setCode) {
    	return "/" + setCode + "/" + getImageName();
    }
    
    public String getCurSetImage() {
    	return getSetImageName(curSetCode); 
    }
    
    public String getCurSetRarity() {
    	for (int i=0; i<Sets.size(); i++)
    		if (Sets.get(i).Code.equals(curSetCode))
    			return Sets.get(i).Rarity;
    	
    	return "";
    }
    
    public String getMostRecentSet()
    {
    	return SetInfoUtil.getMostRecentSet(Sets);
    }
    
    private String ImageFilename = "";
    
    public void setImageFilename(String iFN)
    {
    	ImageFilename = iFN;	
    }
    
    public String getImageFilename()
    {
    	return ImageFilename;
    }
    
}//end Card class
