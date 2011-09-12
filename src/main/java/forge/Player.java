package forge;

import forge.game.GameLossReason;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

import javax.swing.*;
import java.util.*;


/**
 * <p>Abstract Player class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class Player extends GameEntity {
    protected int poisonCounters;
    protected int life;
    protected int assignedDamage;
    protected int numPowerSurgeLands;

    protected boolean altWin = false;
    protected String altWinSourceName;
    protected boolean altLose = false;
    protected GameLossReason lossState = GameLossReason.DidNotLoseYet;
    protected String loseConditionSpell;

    protected int nTurns = 0;
    protected boolean skipNextUntap = false;
    protected ArrayList<String> prowl = new ArrayList<String>();

    protected int maxLandsToPlay = 1;
    protected int numLandsPlayed = 0;

    protected Card lastDrawnCard;
    protected int numDrawnThisTurn = 0;
    protected CardList slowtripList = new CardList();
    
    protected ArrayList<String> keywords = new ArrayList<String>();
    
    protected ManaPool manaPool = null;
    
    protected Object mustAttackEntity = null;
    

    /**
     * <p>Constructor for Player.</p>
     *
     * @param myName a {@link java.lang.String} object.
     */
    public Player(String myName) {
        this(myName, 20, 0);
    }

    /**
     * <p>Constructor for Player.</p>
     *
     * @param myName a {@link java.lang.String} object.
     * @param myLife a int.
     * @param myPoisonCounters a int.
     */
    public Player(String myName, int myLife, int myPoisonCounters) {
        reset();
        
        setName(myName);
        life = myLife;
        poisonCounters = myPoisonCounters;
    }

    /**
     * <p>reset.</p>
     */
    public void reset() {
        life = 20;
        poisonCounters = 0;
        assignedDamage = 0;
        setPreventNextDamage(0);
        lastDrawnCard = null;
        numDrawnThisTurn = 0;
        slowtripList = new CardList();
        nTurns = 0;
        altWin = false;
        altWinSourceName = null;
        altLose = false;
        lossState = GameLossReason.DidNotLoseYet;
        loseConditionSpell = null;
        maxLandsToPlay = 1;
        numLandsPlayed = 0;
        prowl = new ArrayList<String>();
        
        handSizeOperations = new ArrayList<HandSizeOp>();
        keywords.clear();
        manaPool = new ManaPool(this);
        
        this.updateObservers();
    }

    /**
     * <p>isHuman.</p>
     *
     * @return a boolean.
     */
    public abstract boolean isHuman();

    /**
     * <p>isComputer.</p>
     *
     * @return a boolean.
     */
    public abstract boolean isComputer();

    /**
     * <p>isPlayer.</p>
     *
     * @param p1 a {@link forge.Player} object.
     * @return a boolean.
     */
    public boolean isPlayer(Player p1) {
        return p1 != null && p1.getName().equals(getName());
    }

    /**
     * <p>getOpponent.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public abstract Player getOpponent();

    //////////////////////////
    //
    // methods for manipulating life
    //
    //////////////////////////

    /**
     * <p>Setter for the field <code>life</code>.</p>
     *
     * @param newLife a int.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean setLife(final int newLife, final Card source) {
        boolean change = false;
        //rule 118.5
        if (life > newLife) {
            change = loseLife(life - newLife, source);
        } else if (newLife > life) {
            change = gainLife(newLife - life, source);
        } else {
            //life == newLife
            change = false;
        }
        this.updateObservers();
        return change;
    }

    /**
     * <p>Getter for the field <code>life</code>.</p>
     *
     * @return a int.
     */
    public int getLife() {
        return life;
    }

    /**
     * <p>addLife.</p>
     *
     * @param toAdd a int.
     */
    private void addLife(final int toAdd) {
        life += toAdd;
        this.updateObservers();
    }

    /**
     * <p>gainLife.</p>
     *
     * @param toGain a int.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean gainLife(final int toGain, final Card source) {
        boolean newLifeSet = false;
        if (!canGainLife()) return false;
        int lifeGain = toGain;

        if (AllZoneUtil.isCardInPlay("Boon Reflection", this)) {
            int amount = AllZoneUtil.getCardsInPlay("Boon Reflection").size();
            for (int i = 0; i < amount; i++)
                lifeGain += lifeGain;
        }

        if (lifeGain > 0) {
            if (AllZoneUtil.isCardInPlay("Lich", this)) {
                //draw cards instead of gain life
                drawCards(lifeGain);
                newLifeSet = false;
            } else {
                addLife(lifeGain);
                newLifeSet = true;
                this.updateObservers();

                //Run triggers
                HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Player", this);
                runParams.put("LifeAmount", lifeGain);
                AllZone.getTriggerHandler().runTrigger("LifeGained", runParams);
            }
        } else System.out.println("Player - trying to gain negative or 0 life");

        return newLifeSet;
    }

    /**
     * <p>canGainLife.</p>
     *
     * @return a boolean.
     */
    public boolean canGainLife() {
        if (AllZoneUtil.isCardInPlay("Sulfuric Vortex") || AllZoneUtil.isCardInPlay("Leyline of Punishment") ||
                AllZoneUtil.isCardInPlay("Platinum Emperion", this) || AllZoneUtil.isCardInPlay("Forsaken Wastes"))
            return false;
        return true;
    }

    /**
     * <p>loseLife.</p>
     *
     * @param toLose a int.
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean loseLife(final int toLose, final Card c) {
        boolean newLifeSet = false;
        if (!canLoseLife()) return false;
        if (toLose > 0) {
            subtractLife(toLose);
            newLifeSet = true;
            this.updateObservers();
        } else if (toLose == 0) {
            //Rule 118.4
            //this is for players being able to pay 0 life
            //nothing to do
        } else System.out.println("Player - trying to lose positive life");

        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("LifeAmount", toLose);
        AllZone.getTriggerHandler().runTrigger("LifeLost", runParams);

        return newLifeSet;
    }

    /**
     * <p>canLoseLife.</p>
     *
     * @return a boolean.
     */
    public boolean canLoseLife() {
        if (AllZoneUtil.isCardInPlay("Platinum Emperion", this)) return false;
        return true;
    }

    /**
     * <p>subtractLife.</p>
     *
     * @param toSub a int.
     */
    private void subtractLife(final int toSub) {
        life -= toSub;
        this.updateObservers();
    }

    /**
     * <p>canPayLife.</p>
     *
     * @param lifePayment a int.
     * @return a boolean.
     */
    public boolean canPayLife(int lifePayment) {
        if (life < lifePayment) return false;
        if (lifePayment > 0 && AllZoneUtil.isCardInPlay("Platinum Emperion", this)) return false;
        return true;
    }

    /**
     * <p>payLife.</p>
     *
     * @param lifePayment a int.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean payLife(int lifePayment, Card source) {
        if (!canPayLife(lifePayment)) return false;
        //rule 118.8
        if (life >= lifePayment) {
            return loseLife(lifePayment, source);
        }

        return false;
    }

    //////////////////////////
    //
    // methods for handling damage
    //
    //////////////////////////

    //This function handles damage after replacement and prevention effects are applied
    /**
     * <p>addDamageAfterPrevention.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     */
    @Override
    public void addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat) {
        int damageToDo = damage;

        if (source.hasKeyword("Infect")) {
            addPoisonCounters(damageToDo);
        } else {
            //Worship does not reduce the damage dealt but changes the effect of the damage
            if (PlayerUtil.worshipFlag(this) && life <= damageToDo) {
                loseLife(Math.min(damageToDo, life - 1), source);
            } else
                //rule 118.2. Damage dealt to a player normally causes that player to lose that much life.
                loseLife(damageToDo, source);
        }
        if (damageToDo > 0) {
            addAssignedDamage(damageToDo);
            GameActionUtil.executeDamageDealingEffects(source, damageToDo);
            GameActionUtil.executeDamageToPlayerEffects(this, source, damageToDo);
            
            if(isCombat) {
                ArrayList<String> types = source.getType();
                for (String type : types)
                    source.getController().addProwlType(type);
            }

            //Run triggers
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("DamageSource", source);
            runParams.put("DamageTarget", this);
            runParams.put("DamageAmount", damageToDo);
            runParams.put("IsCombatDamage", isCombat);
            AllZone.getTriggerHandler().runTrigger("DamageDone", runParams);
        }
    }

    /**
     * <p>predictDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int predictDamage(final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = staticReplaceDamage(restDamage, source, isCombat);
        restDamage = staticDamagePrevention(restDamage, source, isCombat);

        return restDamage;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticDamagePrevention.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public int staticDamagePrevention(final int damage, final Card source, final boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) return damage;

        int restDamage = damage;

        if (isCombat) {
            if (source.hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) return 0;
            if (source.hasKeyword("Prevent all combat damage that would be dealt by CARDNAME.")) return 0;
        }
        if (source.hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) return 0;
        if (source.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")) return 0;
        if (AllZoneUtil.isCardInPlay("Purity", this) && !isCombat) return 0;

        //stPreventDamage
        CardList allp = AllZoneUtil.getCardsInPlay();
        for (Card ca : allp) {
            if (ca.hasStartOfKeyword("stPreventDamage")) {
                //syntax stPreventDamage:[Who is protected(You/Player/ValidCards)]:[ValidSource]:[Amount/All]
                int KeywordPosition = ca.getKeywordPosition("stPreventDamage");
                String parse = ca.getKeyword().get(KeywordPosition).toString();
                String k[] = parse.split(":");

                final Card card = ca;
                if (k[1].equals("Player") || (k[1].equals("You") && card.getController().isPlayer(this))) {
                    final String restrictions[] = k[2].split(",");
                    if (source.isValidCard(restrictions, card.getController(), card)) {
                        if (k[3].equals("All")) return 0;
                        restDamage = restDamage - Integer.valueOf(k[3]);
                    }
                }
            }
        } //stPreventDamage

        //specific cards
        if (AllZoneUtil.isCardInPlay("Spirit of Resistance", this)) {
            if (AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Black).size() > 0
                    && AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Blue).size() > 0
                    && AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Green).size() > 0
                    && AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.Red).size() > 0
                    && AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.White).size() > 0) {
                return 0;
            }
        }
        if (restDamage > 0)
            return restDamage;
        else return 0;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticReplaceDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public int staticReplaceDamage(final int damage, Card source, boolean isCombat) {

        int restDamage = damage;

        if (AllZoneUtil.isCardInPlay("Sulfuric Vapors") && source.isSpell() && source.isRed()) {
            int amount = AllZoneUtil.getCardsInPlay("Sulfuric Vapors").size();
            for (int i = 0; i < amount; i++)
                restDamage += 1;
        }
        
        if (AllZoneUtil.isCardInPlay("Pyromancer's Swath", source.getController()) && (source.isInstant() || source.isSorcery())) {
            int amount = AllZoneUtil.getPlayerCardsInPlay(source.getController(), "Pyromancer's Swath").size();
            for (int i = 0; i < amount; i++)
                restDamage += 2;
        }

        if (AllZoneUtil.isCardInPlay("Furnace of Rath")) {
            int amount = AllZoneUtil.getCardsInPlay("Furnace of Rath").size();
            for (int i = 0; i < amount; i++)
                restDamage += restDamage;
        }

        if (AllZoneUtil.isCardInPlay("Gratuitous Violence", source.getController())) {
            int amount = AllZoneUtil.getPlayerCardsInPlay(source.getController(), "Gratuitous Violence").size();
            for (int i = 0; i < amount; i++)
                restDamage += restDamage;
        }

        if (AllZoneUtil.isCardInPlay("Fire Servant", source.getController()) && source.isRed()
                && (source.isInstant() || source.isSorcery())) {
            int amount = AllZoneUtil.getPlayerCardsInPlay(source.getController(), "Fire Servant").size();
            for (int i = 0; i < amount; i++)
                restDamage += restDamage;
        }

        if (AllZoneUtil.isCardInPlay("Benevolent Unicorn") && source.isSpell()) {
            int amount = AllZoneUtil.getCardsInPlay("Benevolent Unicorn").size();
            for (int i = 0; i < amount; i++)
                if (restDamage > 0)
                    restDamage -= 1;
        }

        if (AllZoneUtil.isCardInPlay("Divine Presence") && restDamage > 3) {

            restDamage = 3;
        }

        if (AllZoneUtil.isCardInPlay("Forethought Amulet", this) && (source.isInstant() || source.isSorcery()) && restDamage > 2) {

            restDamage = 2;
        }

        return restDamage;
    }

    /**
     * <p>replaceDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public int replaceDamage(final int damage, Card source, boolean isCombat) {

        int restDamage = staticReplaceDamage(damage, source, isCombat);

        if (source.getName().equals("Szadek, Lord of Secrets") && isCombat) {
            source.addCounter(Counters.P1P1, restDamage);
            for (int i = 0; i < restDamage; i++) {
                CardList lib = AllZoneUtil.getPlayerCardsInLibrary(this);
                if (lib.size() > 0) {
                    AllZone.getGameAction().moveToGraveyard(lib.get(0));
                }
            }
            return 0;
        }

        if (AllZoneUtil.isCardInPlay("Crumbling Sanctuary")) {
            for (int i = 0; i < restDamage; i++) {
                CardList lib = AllZoneUtil.getPlayerCardsInLibrary(this);
                if (lib.size() > 0) {
                    AllZone.getGameAction().exile(lib.get(0));
                }
            }
            //return so things like Lifelink, etc do not trigger.  This is a replacement effect I think.
            return 0;
        }

        return restDamage;
    }

    /**
     * <p>preventDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public int preventDamage(final int damage, Card source, boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) return damage;

        int restDamage = damage;

        // Purity has to stay here because it changes the game state
        if (AllZoneUtil.isCardInPlay("Purity", this) && !isCombat) {
            gainLife(restDamage, null);
            return 0;
        }

        restDamage = staticDamagePrevention(restDamage, source, isCombat);

        if (restDamage >= getPreventNextDamage()) {
            restDamage = restDamage - getPreventNextDamage();
            setPreventNextDamage(0);
        } else {
            setPreventNextDamage(getPreventNextDamage() - restDamage);
            restDamage = 0;
        }

        return restDamage;
    }

    /**
     * <p>Setter for the field <code>assignedDamage</code>.</p>
     *
     * @param n a int.
     */
    public void setAssignedDamage(int n) {
        assignedDamage = n;
    }

    /**
     * <p>addAssignedDamage.</p>
     *
     * @param n a int.
     */
    public void addAssignedDamage(int n) {
        assignedDamage += n;
    }

    /**
     * <p>Getter for the field <code>assignedDamage</code>.</p>
     *
     * @return a int.
     */
    public int getAssignedDamage() {
        return assignedDamage;
    }

    /**
     * <p>addCombatDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     */
    public void addCombatDamage(final int damage, final Card source) {

        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, true);
        damageToDo = preventDamage(damageToDo, source, true);

        addDamageAfterPrevention(damageToDo, source, true);   //damage prevention is already checked

        if (damageToDo > 0) {
            GameActionUtil.executeCombatDamageToPlayerEffects(this, source, damageToDo);
        }
    }

    //////////////////////////
    //
    // methods for handling Poison counters
    //
    //////////////////////////

    /**
     * <p>addPoisonCounters.</p>
     *
     * @param num a int.
     */
    public void addPoisonCounters(int num) {
        poisonCounters += num;
        this.updateObservers();
    }

    /**
     * <p>Setter for the field <code>poisonCounters</code>.</p>
     *
     * @param num a int.
     */
    public void setPoisonCounters(int num) {
        poisonCounters = num;
        this.updateObservers();
    }

    /**
     * <p>Getter for the field <code>poisonCounters</code>.</p>
     *
     * @return a int.
     */
    public int getPoisonCounters() {
        return poisonCounters;
    }

    /**
     * <p>subtractPoisonCounters.</p>
     *
     * @param num a int.
     */
    public void subtractPoisonCounters(int num) {
        poisonCounters -= num;
        this.updateObservers();
    }   
    
    public ArrayList<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(ArrayList<String> keywords) {
		this.keywords = keywords;
	}
	
	public void addKeyword(String keyword){
		this.keywords.add(keyword);
	}
	
	   public void removeKeyword(String keyword){
	        this.keywords.remove(keyword);
	    }
	
	public boolean hasKeyword(String keyword){
		return this.keywords.contains(keyword);
	}

    /**
     *
     * @param sa
     * @return  a boolean
     */
	@Override
    public boolean canTarget(SpellAbility sa) {
    	if (hasKeyword("Shroud") ||
    			(!this.isPlayer(sa.getActivatingPlayer()) && hasKeyword("Hexproof")))
    		return false;
    		
        return true;
    }
    
    /**
     * <p>canPlaySpells.</p>
     *
     * @return a boolean.
     */
    public boolean canCastSpells() {
        return !this.keywords.contains("Can't cast spells");
    }

    /**
     * <p>canPlayAbilities.</p>
     *
     * @return a boolean.
     */
    public boolean canActivateAbilities() {
    	return !this.keywords.contains("Can't activate abilities");
    }

    /**
     * <p>getCards.</p>
     *
     * @param zone a {@link forge.PlayerZone} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getCards(PlayerZone zone) {
        //TODO
        return new CardList();
    }


    ////////////////////////////////
    ///
    /// replaces AllZone.getGameAction().draw* methods
    ///
    ////////////////////////////////
    
    /**
     * <p>canDraw</p>
     * 
     * @return true if a player can draw a card, false otherwise
     */
    public boolean canDraw() {
        return !AllZoneUtil.isCardInPlay("Maralen of the Mornsong");
    }

    /**
     * <p>mayDrawCard.</p>
     * 
     * @return a CardList of cards actually drawn
     */
    public abstract CardList mayDrawCard();

    /**
     * <p>mayDrawCards.</p>
     *
     * @param numCards a int.
     * @return a CardList of cards actually drawn
     */
    public abstract CardList mayDrawCards(int numCards);

    /**
     * <p>drawCard.</p>
     * 
     * @return a CardList of cards actually drawn
     */
    public CardList drawCard() {
        return drawCards(1);
    }

    /**
     * <p>drawCards.</p>
     * 
     * @return a CardList of cards actually drawn
     */
    public CardList drawCards() {
        return drawCards(1);
    }

    /**
     * <p>dredge.</p>
     *
     * @return a boolean.
     */
    public abstract boolean dredge();
    
    /**
     * <p>drawCards.</p>
     *
     * @param n a int.
     * @return a CardList of cards actually drawn
     */
    public CardList drawCards(int n) {
    	return drawCards(n, false);
    }

    /**
     * <p>drawCards.</p>
     *
     * @param n a int.
     * @param firstFromDraw true if this is the card drawn from that player's draw step each turn
     * @return a CardList of cards actually drawn
     */
    public CardList drawCards(int n, boolean firstFromDraw) {
    	CardList drawn = new CardList();
    	
    	if(!canDraw()) {
    	    return drawn;
    	}
    	
        for (int i = 0; i < n; i++) {

            // TODO: multiple replacements need to be selected by the controller
            if (getDredge().size() != 0)
                if(dredge())
                	continue;
            
            if(!firstFromDraw && AllZoneUtil.isCardInPlay("Chains of Mephistopheles")) {
            	if(AllZoneUtil.getPlayerHand(this).size() > 0) {
            		if(isHuman()) discard_Chains_of_Mephistopheles();
            		else { //Computer
            			discard(1, null, false);
            			//true causes this code not to be run again
            			drawn.addAll(drawCards(1, true));
            		}
            	}
            	else {
            		mill(1);
            	}
            }
            else {
            	drawn.addAll(doDraw());
            }
        }
        return drawn;
    }

    /**
     * <p>doDraw.</p>
     * 
     * @return a CardList of cards actually drawn
     */
    private CardList doDraw() {
    	CardList drawn = new CardList();
    	PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
        if (library.size() != 0) {
            Card c = library.get(0);
            c = AllZone.getGameAction().moveToHand(c);

            setLastDrawnCard(c);
            c.setDrawnThisTurn(true);
            numDrawnThisTurn++;
            drawn.add(c);

            //Run triggers
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", c);
            AllZone.getTriggerHandler().runTrigger("Drawn", runParams);
        }
        //lose:
        else if (!Constant.Runtime.DevMode[0] || AllZone.getDisplay().canLoseByDecking()) {
            // if devMode is off, or canLoseByDecking is Enabled, run Lose Condition
            if (!cantLose()) {
                loseConditionMet(GameLossReason.Milled, null);
                AllZone.getGameAction().checkStateEffects();
            }
        }
        return drawn;
    }

    /**
     * <p>getDredge.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    protected CardList getDredge() {
        CardList dredge = new CardList();
        CardList cl = AllZoneUtil.getPlayerGraveyard(this);

        for (Card c : cl) {
            ArrayList<String> kw = c.getKeyword();
            for (int i = 0; i < kw.size(); i++) {
                if (kw.get(i).toString().startsWith("Dredge")) {
                    if (AllZoneUtil.getPlayerCardsInLibrary(this).size() >= getDredgeNumber(c)) dredge.add(c);
                }
            }
        }
        return dredge;
    }//hasDredge()

    /**
     * <p>getDredgeNumber.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    protected int getDredgeNumber(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("Dredge")) {
                String s = a.get(i).toString();
                return Integer.parseInt("" + s.charAt(s.length() - 1));
            }

        throw new RuntimeException("Input_Draw : getDredgeNumber() card doesn't have dredge - " + c.getName());
    }//getDredgeNumber()

    /**
     * <p>resetNumDrawnThisTurn.</p>
     */
    public void resetNumDrawnThisTurn() {
        numDrawnThisTurn = 0;
    }

    /**
     * <p>Getter for the field <code>numDrawnThisTurn</code>.</p>
     *
     * @return a int.
     */
    public int getNumDrawnThisTurn() {
        return numDrawnThisTurn;
    }

    ////////////////////////////////
    ///
    /// replaces AllZone.getGameAction().discard* methods
    ///
    ////////////////////////////////
    
    protected abstract void discard_Chains_of_Mephistopheles();

    /**
     * <p>discard.</p>
     *
     * @param num a int.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param duringResolution a boolean.
     * @return a {@link forge.CardList} object.
     */
    public abstract CardList discard(final int num, final SpellAbility sa, boolean duringResolution);

    /**
     * <p>discard.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList discard(final SpellAbility sa) {
        return discard(1, sa, false);
    }

    /**
     * <p>discard.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void discard(Card c, SpellAbility sa) {
        doDiscard(c, sa);
    }

    /**
     * <p>doDiscard.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    protected void doDiscard(final Card c, final SpellAbility sa) {
        // TODO: This line should be moved inside CostPayment somehow
        if (sa != null) {
            sa.addCostToHashList(c, "Discarded");
        }

        /*
           * When a spell or ability an opponent controls causes you
           * to discard Psychic Purge, that player loses 5 life.
           */
        if (c.getName().equals("Psychic Purge")) {
            if (null != sa && !sa.getSourceCard().getController().equals(this)) {
                SpellAbility ability = new Ability(c, "") {
                    public void resolve() {
                        sa.getSourceCard().getController().loseLife(5, c);
                    }
                };
                ability.setStackDescription(c.getName() + " - " +
                        sa.getSourceCard().getController() + " loses 5 life.");
                AllZone.getStack().add(ability);
            }
        }

        AllZone.getGameAction().discard_madness(c);

        if ((c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield instead of putting it into your graveyard.")
                || c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME, put it onto the battlefield with two +1/+1 counters on it instead of putting it into your graveyard."))
                && !c.getController().equals(sa.getSourceCard().getController())) {
            AllZone.getGameAction().discard_PutIntoPlayInstead(c);
        } else if (c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME, return it to your hand.")) {
            ;
        } else {
            AllZone.getGameAction().moveToGraveyard(c);
        }

        //Run triggers
        Card cause = null;
        if (sa != null) {
            cause = sa.getSourceCard();
        }
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("Card", c);
        runParams.put("Cause", cause);
        AllZone.getTriggerHandler().runTrigger("Discarded", runParams);

    }//end doDiscard

    /**
     * <p>discardHand.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void discardHand(SpellAbility sa) {
        CardList list = AllZoneUtil.getPlayerHand(this);
        discardRandom(list.size(), sa);
    }

    /**
     * <p>discardRandom.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void discardRandom(SpellAbility sa) {
        discardRandom(1, sa);
    }

    /**
     * <p>discardRandom.</p>
     *
     * @param num a int.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return 
     */
    public CardList discardRandom(final int num, final SpellAbility sa) {
        CardList discarded = new CardList();
        for (int i = 0; i < num; i++) {
            CardList list = AllZoneUtil.getPlayerHand(this);
            if (list.size() != 0){
                Card disc = CardUtil.getRandom(list.toArray());
                discarded.add(disc);
                doDiscard(disc, sa);
            }
        }
        return discarded;
    }

    /**
     * <p>discardUnless.</p>
     *
     * @param num a int.
     * @param uType a {@link java.lang.String} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public abstract void discardUnless(int num, String uType, SpellAbility sa);

    /**
     * <p>mill.</p>
     *
     * @param n a int.
     */
    public CardList mill(int n) {
        return mill(n, Constant.Zone.Graveyard);
    }

    /**
     * <p>mill.</p>
     *
     * @param n a int.
     * @param zone a {@link java.lang.String} object.
     */
    public CardList mill(int n, String zone) {
        CardList lib = AllZoneUtil.getPlayerCardsInLibrary(this);
        CardList milled = new CardList();

        int max = Math.min(n, lib.size());

        PlayerZone destination = AllZone.getZone(zone, this);

        for (int i = 0; i < max; i++) {
            milled.add(AllZone.getGameAction().moveTo(destination, lib.get(i)));
        }
        
        return milled;
    }

    /**
     * <p>handToLibrary.</p>
     *
     * @param numToLibrary a int.
     * @param libPos a {@link java.lang.String} object.
     */
    public abstract void handToLibrary(final int numToLibrary, String libPos);

    ////////////////////////////////
    /**
     * <p>shuffle.</p>
     */
    public void shuffle() {
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
        Card c[] = AllZoneUtil.getPlayerCardsInLibrary(this).toArray();

        if (c.length <= 1) return;

        ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(c));
        //overdone but wanted to make sure it was really random
        Random random = MyRandom.random;
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);

        Object o;
        for (int i = 0; i < list.size(); i++) {
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

        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        AllZone.getTriggerHandler().runTrigger("Shuffled", runParams);

    }//shuffle
    ////////////////////////////////

    ////////////////////////////////
    /**
     * <p>doScry.</p>
     *
     * @param topN a {@link forge.CardList} object.
     * @param N a int.
     */
    protected abstract void doScry(CardList topN, int N);

    /**
     * <p>scry.</p>
     *
     * @param numScry a int.
     */
    public void scry(int numScry) {
        CardList topN = new CardList();
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, this);
        numScry = Math.min(numScry, library.size());
        for (int i = 0; i < numScry; i++) {
            topN.add(library.get(i));
        }
        doScry(topN, topN.size());
    }
    ///////////////////////////////

    /**
     * <p>playLand.</p>
     *
     * @param land a {@link forge.Card} object.
     */
    public void playLand(Card land) {
        if (canPlayLand()) {
            AllZone.getGameAction().moveToPlay(land);
            CardFactoryUtil.playLandEffects(land);
            numLandsPlayed++;

            //check state effects for static animate (Living Lands, Conversion, etc...)
            AllZone.getGameAction().checkStateEffects();

            //Run triggers
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", land);
            AllZone.getTriggerHandler().runTrigger("LandPlayed", runParams);
        }

        AllZone.getStack().unfreezeStack();
    }

    /**
     * <p>canPlayLand.</p>
     *
     * @return a boolean.
     */
    public boolean canPlayLand() {
        return Phase.canCastSorcery(this) && (numLandsPlayed < maxLandsToPlay ||
                AllZoneUtil.getPlayerCardsInPlay(this, "Fastbond").size() > 0);
    }

    public ManaPool getManaPool() {
		return manaPool;
	}

	///////////////////////////////
    ////
    ////	properties about the player and his/her cards/game status
    ////
    ///////////////////////////////
    /**
     * <p>hasPlaneswalker.</p>
     *
     * @return a boolean.
     */
    public boolean hasPlaneswalker() {
        return null != getPlaneswalker();
    }

    /**
     * <p>getPlaneswalker.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getPlaneswalker() {
        CardList c = AllZoneUtil.getPlayerTypeInPlay(this, "Planeswalker");
        if (null != c && c.size() > 0) return c.get(0);
        else return null;
    }

    /**
     * <p>Getter for the field <code>numPowerSurgeLands</code>.</p>
     *
     * @return a int.
     */
    public int getNumPowerSurgeLands() {
        return numPowerSurgeLands;
    }

    /**
     * <p>Setter for the field <code>numPowerSurgeLands</code>.</p>
     *
     * @param n a int.
     * @return a int.
     */
    public int setNumPowerSurgeLands(int n) {
        numPowerSurgeLands = n;
        return numPowerSurgeLands;
    }

    /**
     * <p>Getter for the field <code>lastDrawnCard</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getLastDrawnCard() {
        return lastDrawnCard;
    }

    /**
     * <p>Setter for the field <code>lastDrawnCard</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public Card setLastDrawnCard(Card c) {
        lastDrawnCard = c;
        return lastDrawnCard;
    }

    /**
     * <p>resetLastDrawnCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card resetLastDrawnCard() {
        Card old = lastDrawnCard;
        lastDrawnCard = null;
        return old;
    }

    /**
     * <p>skipNextUntap.</p>
     *
     * @return a boolean.
     */
    public boolean skipNextUntap() {
        return skipNextUntap;
    }

    /**
     * <p>Setter for the field <code>skipNextUntap</code>.</p>
     *
     * @param b a boolean.
     */
    public void setSkipNextUntap(boolean b) {
        skipNextUntap = b;
    }

    /**
     * <p>Getter for the field <code>slowtripList</code>.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getSlowtripList() {
        return slowtripList;
    }

    /**
     * <p>clearSlowtripList.</p>
     */
    public void clearSlowtripList() {
        slowtripList.clear();
    }

    /**
     * <p>addSlowtripList.</p>
     *
     * @param card a {@link forge.Card} object.
     */
    public void addSlowtripList(Card card) {
        slowtripList.add(card);
    }

    /**
     * <p>getTurn.</p>
     *
     * @return a int.
     */
    public int getTurn() {
        return nTurns;
    }

    /**
     * <p>incrementTurn.</p>
     */
    public void incrementTurn() {
        nTurns++;
    }

    ////////////////////////////////
    /**
     * <p>sacrificePermanent.</p>
     *
     * @param prompt a {@link java.lang.String} object.
     * @param choices a {@link forge.CardList} object.
     */
    public abstract void sacrificePermanent(String prompt, CardList choices);

    /**
     * <p>sacrificeCreature.</p>
     */
    public void sacrificeCreature() {
        CardList choices = AllZoneUtil.getCreaturesInPlay(this);
        sacrificePermanent("Select a creature to sacrifice.", choices);
    }

    /**
     * <p>sacrificeCreature.</p>
     *
     * @param choices a {@link forge.CardList} object.
     */
    public void sacrificeCreature(CardList choices) {
        sacrificePermanent("Select a creature to sacrifice.", choices);
    }

    // Game win/loss

    /**
     * <p>Getter for the field <code>altWin</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getAltWin() {
        return altWin;
    }

    /**
     * <p>Getter for the field <code>winCondition</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWinConditionSource()
    {
        return altWinSourceName;
    }

    /**
     * <p>Getter for the field <code>loseCondition</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public GameLossReason getLossState() { return lossState; }
    public String getLossConditionSource() { return loseConditionSpell; }

    /**
     * <p>altWinConditionMet.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void altWinBySpellEffect(final String sourceName) {
        if (cantWin()) {
            System.out.println("Tried to win, but currently can't.");
            return;
        }
        altWin = true;
        altWinSourceName = sourceName;
    }

    /**
     * <p>altLoseConditionMet.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean loseConditionMet(GameLossReason state, String spellName ) {
        if (cantLose()) {
            System.out.println("Tried to lose, but currently can't.");
            return false;
        }
        lossState = state;
        loseConditionSpell = spellName;
        return true;
    }

    public void concede() { // No cantLose checks - just lose
        lossState = GameLossReason.Conceded;
        loseConditionSpell = null;
    }

    /**
     * <p>cantLose.</p>
     *
     * @return a boolean.
     */
    public boolean cantLose() {
        if (lossState == GameLossReason.Conceded) { return false; }

        CardList list = AllZoneUtil.getPlayerCardsInPlay(this);
        list = list.getKeyword("You can't lose the game.");

        if (list.size() > 0)
            return true;

        CardList oppList = AllZoneUtil.getPlayerCardsInPlay(getOpponent());
        oppList = oppList.getKeyword("Your opponents can't lose the game.");

        return oppList.size() > 0;
    }

    /**
     * <p>cantLoseForZeroOrLessLife.</p>
     *
     * @return a boolean.
     */
    public boolean cantLoseForZeroOrLessLife() {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(this);
        list = list.getKeyword("You don't lose the game for having 0 or less life.");

        return list.size() > 0;
    }

    /**
     * <p>cantWin.</p>
     *
     * @return a boolean.
     */
    public boolean cantWin() {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(getOpponent());
        list = list.getKeyword("You can't win the game.");

        if (list.size() > 0)
            return true;

        CardList oppList = AllZoneUtil.getPlayerCardsInPlay(this);
        oppList = oppList.getKeyword("Your opponents can't win the game.");

        return oppList.size() > 0;
    }

    /**
     * <p>hasLost.</p>
     *
     * @return a boolean.
     */
    public boolean hasLost() {

        if (cantLose())
            return false;

        if (lossState != GameLossReason.DidNotLoseYet) {
            return true;
        }

        if (poisonCounters >= 10) {
            loseConditionMet( GameLossReason.Poisoned, null );
            return true;
        }

        if (cantLoseForZeroOrLessLife()) {
            return false;
        }

        boolean hasNoLife = getLife() <= 0;
        if (hasNoLife) {
            loseConditionMet( GameLossReason.LifeReachedZero, null);
            return true;
        }

        return false;
    }


    /**
     * <p>hasWon.</p>
     *
     * @return a boolean.
     */
    public boolean hasWon() {
        if (cantWin())
            return false;

        return altWin;
    }

    /**
     * <p>hasMetalcraft.</p>
     *
     * @return a boolean.
     */
    public boolean hasMetalcraft() {
        CardList list = AllZoneUtil.getPlayerTypeInPlay(this, "Artifact");
        return list.size() >= 3;
    }

    /**
     * <p>hasThreshold.</p>
     *
     * @return a boolean.
     */
    public boolean hasThreshold() {
        CardList grave = AllZoneUtil.getPlayerGraveyard(this);
        return grave.size() >= 7;
    }

    /**
     * <p>hasHellbent.</p>
     *
     * @return a boolean.
     */
    public boolean hasHellbent() {
        CardList hand = AllZoneUtil.getPlayerHand(this);
        return hand.size() == 0;
    }
    
    /**
     * <p>hasLandfall.</p>
     *
     * @return a boolean.
     */
    public boolean hasLandfall() {
        CardList list = ((DefaultPlayerZone) AllZone.getZone("Battlefield", this)).getCardsAddedThisTurn("Any").getType("Land");
        return !list.isEmpty();
    }
    
    /**
     * <p>hasProwl.</p>
     *
     * @return a boolean.
     */
    public boolean hasProwl(String type) {
        return prowl.contains(type);
    }
    
    public void addProwlType(String type) {
        prowl.add(type);
    }
    
    public void resetProwl() {
        prowl = new ArrayList<String>();
    }
    
    @Override
    public boolean isValid(final String Restriction, final Player sourceController, final Card source) {
        
        String incR[] = Restriction.split("\\.");
        
        if (!incR[0].equals("Player") && 
                !(incR[0].equals("Opponent") && !this.equals(sourceController)) &&
                !(incR[0].equals("You") && this.equals(sourceController)))
            return false;
        
        if (incR.length > 1) {
            final String excR = incR[1];
            String exR[] = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++)
                if (hasProperty(exR[j], sourceController, source) == false) return false;
        }
        
        return true;
    }
    
    @Override
    public boolean hasProperty(String Property, final Player sourceController, final Card source) {
        
        if (Property.equals("You")) {
            if (!this.equals(sourceController)) {
                return false;
            }
        }        
        else if (Property.equals("Opponent")) {
            if (this.equals(sourceController)) {
                return false;
            }
        }
        
        return true;
    }

    private ArrayList<HandSizeOp> handSizeOperations;

    /**
     * <p>getMaxHandSize.</p>
     *
     * @return a int.
     */
    public int getMaxHandSize() {

        int ret = 7;
        for (int i = 0; i < handSizeOperations.size(); i++) {
            if (handSizeOperations.get(i).Mode.equals("=")) {
                ret = handSizeOperations.get(i).Amount;
            } else if (handSizeOperations.get(i).Mode.equals("+") && ret >= 0) {
                ret = ret + handSizeOperations.get(i).Amount;
            } else if (handSizeOperations.get(i).Mode.equals("-") && ret >= 0) {
                ret = ret - handSizeOperations.get(i).Amount;
                if (ret < 0) {
                    ret = 0;
                }
            }
        }
        return ret;
    }

    /**
     * <p>sortHandSizeOperations.</p>
     */
    public void sortHandSizeOperations() {
        if (handSizeOperations.size() < 2) {
            return;
        }

        int changes = 1;

        while (changes > 0) {
            changes = 0;
            for (int i = 1; i < handSizeOperations.size(); i++) {
                if (handSizeOperations.get(i).hsTimeStamp < handSizeOperations.get(i - 1).hsTimeStamp) {
                    HandSizeOp tmp = handSizeOperations.get(i);
                    handSizeOperations.set(i, handSizeOperations.get(i - 1));
                    handSizeOperations.set(i - 1, tmp);
                    changes++;
                }
            }
        }
    }

    /**
     * <p>addHandSizeOperation.</p>
     *
     * @param theNew a {@link forge.HandSizeOp} object.
     */
    public void addHandSizeOperation(HandSizeOp theNew) {
        handSizeOperations.add(theNew);
    }

    /**
     * <p>removeHandSizeOperation.</p>
     *
     * @param timestamp a int.
     */
    public void removeHandSizeOperation(int timestamp) {
        for (int i = 0; i < handSizeOperations.size(); i++) {
            if (handSizeOperations.get(i).hsTimeStamp == timestamp) {
                handSizeOperations.remove(i);
                break;
            }
        }
    }

    /**
     * <p>clearHandSizeOperations.</p>
     */
    public void clearHandSizeOperations() {
        handSizeOperations.clear();
    }

    /** Constant <code>NextHandSizeStamp=0</code> */
    private static int NextHandSizeStamp = 0;

    /**
     * <p>getHandSizeStamp.</p>
     *
     * @return a int.
     */
    public static int getHandSizeStamp() {
        return NextHandSizeStamp++;
    }

    /**
     * <p>Getter for the field <code>maxLandsToPlay</code>.</p>
     *
     * @return a int.
     */
    public int getMaxLandsToPlay() {
        return maxLandsToPlay;
    }

    /**
     * <p>Setter for the field <code>maxLandsToPlay</code>.</p>
     *
     * @param n a int.
     */
    public void setMaxLandsToPlay(int n) {
        maxLandsToPlay = n;
    }

    /**
     * <p>addMaxLandsToPlay.</p>
     *
     * @param n a int.
     */
    public void addMaxLandsToPlay(int n) {
        maxLandsToPlay += n;
    }

    /**
     * <p>Getter for the field <code>numLandsPlayed</code>.</p>
     *
     * @return a int.
     */
    public int getNumLandsPlayed() {
        return numLandsPlayed;
    }

    /**
     * <p>Setter for the field <code>numLandsPlayed</code>.</p>
     *
     * @param n a int.
     */
    public void setNumLandsPlayed(int n) {
        numLandsPlayed = n;
    }

    ////////////////////////////////
    //
    // Clash
    //
    /////////////////////////////////

    /**
     * <p>clashWithOpponent.</p>
     *
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean clashWithOpponent(Card source) {
        /*
           * Each clashing player reveals the top card of his or
           * her library, then puts that card on the top or bottom.
           * A player wins if his or her card had a higher mana cost.
           *
           * Clash you win or win you don't.  There is no tie.
           */
        Player player = source.getController();
        Player opponent = player.getOpponent();
        String lib = Constant.Zone.Library;

        PlayerZone pLib = AllZone.getZone(lib, player);
        PlayerZone oLib = AllZone.getZone(lib, opponent);

        StringBuilder reveal = new StringBuilder();

        Card pCard = null;
        Card oCard = null;

        if (pLib.size() > 0) pCard = pLib.get(0);
        if (oLib.size() > 0) oCard = oLib.get(0);

        if (pLib.size() == 0 && oLib.size() == 0) return false;
        else if (pLib.size() == 0) {
            opponent.clashMoveToTopOrBottom(oCard);
            return false;
        } else if (oLib.size() == 0) {
            player.clashMoveToTopOrBottom(pCard);
            return true;
        } else {
            int pCMC = CardUtil.getConvertedManaCost(pCard);
            int oCMC = CardUtil.getConvertedManaCost(oCard);
            reveal.append(player).append(" reveals: ").append(pCard.getName()).append(".  CMC = ").append(pCMC);
            reveal.append("\r\n");
            reveal.append(opponent).append(" reveals: ").append(oCard.getName()).append(".  CMC = ").append(oCMC);
            reveal.append("\r\n\r\n");
            if (pCMC > oCMC) reveal.append(player).append(" wins clash.");
            else reveal.append(player).append(" loses clash.");
            JOptionPane.showMessageDialog(null, reveal.toString(), source.getName(), JOptionPane.PLAIN_MESSAGE);
            player.clashMoveToTopOrBottom(pCard);
            opponent.clashMoveToTopOrBottom(oCard);
            //JOptionPane.showMessageDialog(null, reveal.toString(), source.getName(), JOptionPane.PLAIN_MESSAGE);
            return pCMC > oCMC;
        }
    }

    /**
     * <p>clashMoveToTopOrBottom.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    protected abstract void clashMoveToTopOrBottom(Card c);
    
    /**
     * a Player or Planeswalker that this Player must attack if able in an upcoming combat.
     * This is cleared at the end of each combat.
     * 
     * @param o Player or Planeswalker (Card) to attack
     * 
     * @since 1.1.01
     */
    public void setMustAttackEntity(Object o) {
    	mustAttackEntity = o;
    }
    
    /**
     * get the Player object or Card (Planeswalker) object that this Player must attack this combat
     * 
     * @return the Player or Card (Planeswalker)
     * 
     * @since 1.1.01
     */
    public Object getMustAttackEntity() {
    	return mustAttackEntity;
    }

    ////////////////////////////////
    //
    // generic Object overrides
    //
    /////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Player) {
            Player p1 = (Player) o;
            return p1.getName().equals(getName());
        } else return false;
    }
}
