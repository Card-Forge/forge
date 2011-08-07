
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandArgs;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.mana.Mana;
import forge.gui.input.Input;


//only SpellAbility can go on the stack
//override any methods as needed
public abstract class SpellAbility {
    public Object[]         choices_made;                       //open ended Casting choice storage
    //choices for constructor isPermanent argument
    public static final int Spell              = 0;
    public static final int Ability            = 1;
    
    private String          description        = "";
    private Player          targetPlayer       = null;
    private String          stackDescription   = "";
    private String          manaCost           = "";
    private String          additionalManaCost = "";
    private String			multiKickerManaCost= "";
    private String			replicateManaCost 	= "";
    private String 			xManaCost		   = "";
    private Player			activatingPlayer   = null;
    
    private String          type               = "Intrinsic";  //set to Intrinsic by default
                                                                
    private Card            targetCard;
    private Card            sourceCard;
    
    private CardList        targetList;
    // targetList doesn't appear to be used anymore
    
    private boolean         spell;
    private boolean			trigger 		   = false;
    private boolean         mandatory          = false;
    
    private boolean         tapAbility;
    private boolean         untapAbility;
    private boolean         buyBackAbility     = false;        //false by default
    private boolean         flashBackAbility   = false;
    private boolean			multiKicker 	   = false;
    private boolean			replicate			= false;
    private boolean 		xCost			   = false;
    private boolean         kickerAbility      = false;
    private boolean 		kothThirdAbility   = false;
    private boolean			cycling 		   = false;
    
    private Input           beforePayMana;
    private Input           afterResolve;
    private Input           afterPayMana;
    
    protected Cost			payCosts		   = null;
    protected Target		chosenTarget	   = null;
    
    private SpellAbility_Restriction restrictions = new SpellAbility_Restriction();
    private SpellAbility_Condition	conditions = new SpellAbility_Condition();
    private Ability_Sub 	subAbility 			= null;
    
    private AbilityFactory  abilityFactory 	   = null;
    
    private ArrayList<Mana> payingMana = new ArrayList<Mana>();
    private ArrayList<Ability_Mana> paidAbilities = new ArrayList<Ability_Mana>();
    
    private HashMap<String, CardList> paidLists = new HashMap<String, CardList>();
    
    private HashMap<String,Object> triggeringObjects = new HashMap<String,Object>();

	private Command         cancelCommand      = Command.Blank;
    private Command         beforePayManaAI    = Command.Blank;
    
    private CommandArgs     randomTarget       = new CommandArgs() {
                                                   
                                                   private static final long serialVersionUID = 1795025064923737374L;
                                                   
                                                   public void execute(Object o) {}
                                               };
    
    public SpellAbility(int spellOrAbility, Card i_sourceCard) {
        if(spellOrAbility == Spell) spell = true;
        else if(spellOrAbility == Ability) spell = false;

        else throw new RuntimeException("SpellAbility : constructor error, invalid spellOrAbility argument = "
                + spellOrAbility);
        

        sourceCard = i_sourceCard;
    }
    
    //Spell, and Ability, and other Ability objects override this method
    abstract public boolean canPlay();
    
    //all Spell's and Abilities must override this method
    abstract public void resolve();
    
    public boolean canPlayAI() {
        return true;
    }

	// This should be overridden by ALL AFs
    public boolean doTrigger(boolean mandatory){
    	return false;
    }
    
    public void chooseTargetAI() {
        randomTarget.execute(this);
    }
    
    public void setChooseTargetAI(CommandArgs c) {
        randomTarget = c;
    }
    
    public CommandArgs getChooseTargetAI() {
        return randomTarget;
    }
    
    public String getManaCost() {
        return manaCost;
    }
    
    public void setManaCost(String cost) {
        manaCost = cost;
    }

    public String getAdditionalManaCost() {
        return additionalManaCost;
    }
    
    public void setAdditionalManaCost(String cost) {
        additionalManaCost = cost;
    }
    
    public String getMultiKickerManaCost() {
        return multiKickerManaCost;
    }
    
    public void setMultiKickerManaCost(String cost) {
    	multiKickerManaCost = cost;
    }
    
    public String getReplicateManaCost() {
        return replicateManaCost;
    }
    
    public void setReplicateManaCost(String cost) {
    	replicateManaCost = cost;
    }
    
    public String getXManaCost()
    {
    	return xManaCost;
    }
    
    public void setXManaCost(String cost)
    {
    	xManaCost = cost;
    }
    
    public Player getActivatingPlayer(){
    	return activatingPlayer;
    }
    
    public void setActivatingPlayer(Player player){
    	// trickle down activating player
    	activatingPlayer = player;
    	if (subAbility != null)
    		subAbility.setActivatingPlayer(player);
    }
    
    public boolean isSpell() {
        return spell;
    }
    
    public boolean isAbility() {
        return !isSpell();
    }
    
    public boolean isTapAbility() {
        return tapAbility;
    }
    
    public boolean isUntapAbility() {
        return untapAbility;
    }
    
    public void makeUntapAbility()
    {
    	untapAbility = true;
    	tapAbility = false;
    }
    
    public void setIsBuyBackAbility(boolean b) {
        buyBackAbility = b;
    }
    
    public boolean isBuyBackAbility() {
        return buyBackAbility;
    }
    
    public void setIsMultiKicker(boolean b){
    	multiKicker = b;
    }
    
    public boolean isMultiKicker() {
    	return multiKicker;
    }
    
    public void setIsReplicate(boolean b){
    	replicate = b;
    }
    
    public boolean isReplicate() {
    	return replicate;
    }
    
    public void setIsXCost(boolean b) {
    	xCost = b;
    }
    
    public boolean isXCost(){
    	return xCost;
    }
    
    public void setIsCycling(boolean b) {
    	cycling = b;
    }
    
    public boolean isCycling(){
    	return cycling;
    }
    
    public void setSourceCard(Card c) {
        sourceCard = c;
    }
    
    public Card getSourceCard() {
        return sourceCard;
    }
    
    public Command getBeforePayManaAI() {
        return beforePayManaAI;
    }
    
    public void setBeforePayManaAI(Command c) {
        beforePayManaAI = c;
    }
    
    //begin - Input methods
    public Input getBeforePayMana() {
    	return beforePayMana;
    }
    
    public void setBeforePayMana(Input in) {
        beforePayMana = in;
    }
    
    public Input getAfterPayMana() {
        return afterPayMana;
    }
    
    public void setAfterPayMana(Input in) {
        afterPayMana = in;
    }
    
    public Cost getPayCosts() {
    	return payCosts;
    }
    
    public void setPayCosts(Cost abCost) {
    	payCosts = abCost;
    }
    
    public Target getTarget() {
    	return chosenTarget;
    }
    
    public void setTarget(Target tgt) {
    	chosenTarget = tgt;
    }
    
    public void setRestrictions(SpellAbility_Restriction restrict){
    	restrictions = restrict;
    }
    
    public SpellAbility_Restriction getRestrictions(){
    	return restrictions;
    }
    
    public void setConditions(SpellAbility_Condition condition) {
    	conditions = condition;
    }
    
    public SpellAbility_Condition getConditions() {
    	return conditions;
    }
    
    public void setAbilityFactory(AbilityFactory af){
    	abilityFactory = af;
    }
    
    public AbilityFactory getAbilityFactory(){
    	return abilityFactory;
    }
    
    public ArrayList<Mana> getPayingMana(){
    	return payingMana;
    }
    
    public ArrayList<Ability_Mana> getPayingManaAbilities(){
    	return paidAbilities;
    }
    
    // Combined PaidLists
    public void setPaidHash(HashMap<String, CardList> hash){
    	paidLists = hash;
    }
    
    public HashMap<String, CardList> getPaidHash(){
    	return paidLists;
    }
    
    // Paid List are for things ca
    public void setPaidList(CardList list, String str){
    	paidLists.put(str, list);
    }
    
    public CardList getPaidList(String str){
    	return paidLists.get(str);
    }
    
    public void addCostToHashList(Card c, String str){
    	if (!paidLists.containsKey(str))
    		paidLists.put(str, new CardList());
    		
    	paidLists.get(str).add(c);
    }
    
    public void resetPaidHash(){
    	paidLists = new HashMap<String, CardList>();
    }

    public HashMap<String, Object> getTriggeringObjects() {
		return triggeringObjects;
	}

	public void setAllTriggeringObjects(HashMap<String, Object> triggeredObjects) {
		this.triggeringObjects = triggeredObjects;
	}
	
	public void setTriggeringObject(String type,Object o) {
		this.triggeringObjects.put(type, o);
	}
	
    public Object getTriggeringObject(String type)
    {
        return triggeringObjects.get(type);
    }

    public boolean hasTriggeringObject(String type)
    {
        return triggeringObjects.containsKey(type);
    }
    
    public void resetTriggeringObjects(){
    	triggeringObjects = new HashMap<String, Object>();
    }
    
    public void resetOnceResolved(){
    	resetPaidHash();

    	if (chosenTarget != null)
    		chosenTarget.resetTargets();
    	
    	resetTriggeringObjects();
    }
    
    public Input getAfterResolve() {
        return afterResolve;
    }
    
    public void setAfterResolve(Input in) {
        afterResolve = in;
    }
    
    public void setStackDescription(String s) {
        stackDescription = s;
        if(description == "" && sourceCard.getText().equals(""))
            description = s;
    }
    
    public String getStackDescription() {
        if(stackDescription.equals(getSourceCard().getText().trim())) return getSourceCard().getName() + " - "
                + getSourceCard().getText();
        
        return stackDescription.replaceAll("CARDNAME", this.getSourceCard().getName());
    }
    
    public boolean isIntrinsic() {
        return type.equals("Intrinsic");
    }
    
    public boolean isExtrinsic() {
        return type.equals("Extrinsic");
    }
    
    public void setType(String s) //Extrinsic or Intrinsic:
    {
        type = s;
    }
    
    public String getType() //Extrinsic or Intrinsic:
    {
        return type;
    }
    
    //setDescription() includes mana cost and everything like
    //"G, tap: put target creature from your hand onto the battlefield"
    public void setDescription(String s) {
        description = s;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        SpellAbility node = this;

		while(node != null){
			if (node != this)
				sb.append(" ");

			sb.append(node.getDescription().replace("CARDNAME", node.getSourceCard().getName()));
			node = node.getSubAbility();

		}
		return sb.toString();
 }
    
	public void setSubAbility(Ability_Sub subAbility) {
		this.subAbility = subAbility;
		if (subAbility != null)
			subAbility.setParent(this);
	}
	
	public Ability_Sub getSubAbility() {
		return this.subAbility;		
	}
    
    public Card getTargetCard() {
        if(targetCard == null){
        	Target tgt = this.getTarget();
        	if (tgt != null){
	        	ArrayList<Card> list = tgt.getTargetCards();
	        	
	        	if (!list.isEmpty())
	        		return list.get(0);
        	}
        	return null;
        }
        
        return targetCard;
    }
    
    public void setTargetCard(Card card) {
    	if (card == null){
    		System.out.println(getSourceCard()+" - SpellAbility.setTargetCard() called with null for target card.");
    		return;
    	}
    	
    	Target tgt = this.getTarget();
    	if (tgt != null){
    		tgt.addTarget(card);
    	}
    	else{
	    	targetPlayer = null;//reset setTargetPlayer()
	    	targetCard = card;
    	}
    	String desc = "";
    	if(null != card) {
    		if(!card.isFaceDown()) desc = getSourceCard().getName() + " - targeting " + card;
    		else desc = getSourceCard().getName() + " - targeting Morph(" + card.getUniqueNumber() + ")";
    		setStackDescription(desc);
    	}
    }
    
    public CardList getTargetList() {
        return targetList;
    }
    
    public void setTargetList(CardList list) {
        // The line below started to create a null error at forge.CardFactoryUtil.canTarget(CardFactoryUtil.java:3329)
        // after ForgeSVN r2699. I hope that commenting out the line below will not result in other bugs.  :)
        // targetPlayer = null;//reset setTargetPlayer()
        
        targetList = list;
        StringBuilder sb = new StringBuilder();
        sb.append(getSourceCard().getName()).append(" - targeting ");
        for (int i = 0; i < targetList.size(); i++) {
            
            if (!targetList.get(i).isFaceDown()) sb.append(targetList.get(i));
            else sb.append("Morph(").append(targetList.get(i).getUniqueNumber()).append(")");
            
            if (i < targetList.size() - 1) sb.append(", ");
        }
        setStackDescription(sb.toString());
    }
    
    public void setTargetPlayer(Player p) {
        if(p == null || (!(p.isHuman() || p.isComputer()))) throw new RuntimeException(
                "SpellAbility : setTargetPlayer() error, argument is " + p + " source card is " + getSourceCard());
        
    	Target tgt = this.getTarget();
    	if (tgt != null){
    		tgt.addTarget(p);
    	}
    	else{
	        targetCard = null;//reset setTargetCard()
	        targetPlayer = p;
    	}
        setStackDescription(getSourceCard().getName() + " - targeting " + p);
    }
    
    public Player getTargetPlayer() {
        if(targetPlayer == null){
        	Target tgt = this.getTarget();
        	if (tgt != null){
	        	ArrayList<Player> list = tgt.getTargetPlayers();
	        	
	        	if (!list.isEmpty())
	        		return list.get(0);
        	}
        	return null;
        }
        return targetPlayer;
    }
    
    public Command getCancelCommand() {
        return cancelCommand;
    }
    
    public void setCancelCommand(Command cancelCommand) {
        this.cancelCommand = cancelCommand;
    }
    
    public void setFlashBackAbility(boolean flashBackAbility) {
        this.flashBackAbility = flashBackAbility;
    }
    
    public boolean isFlashBackAbility() {
        return flashBackAbility;
    }
    public void setKickerAbility(boolean kab) {
    	this.kickerAbility=kab;
    }
    public boolean isKickerAbility() {
    	return kickerAbility;
    }
    // Only used by Ability_Reflected_Mana, because the user has an option to cancel the input.
    // Most spell abilities and even most mana abilities do not need to use this.
    public boolean wasCancelled() {
    	return false;
    }
    public SpellAbility copy()
    {
       SpellAbility clone = null;
        try {
           clone = (SpellAbility)this.clone();
        } catch (CloneNotSupportedException e) {
           System.err.println(e);
        }
        return clone;
    }

	public void setKothThirdAbility(boolean kothThirdAbility) {
		this.kothThirdAbility = kothThirdAbility;
	}

	public boolean isKothThirdAbility() {
		return kothThirdAbility;
	}

	public void setTrigger(boolean trigger) {
		this.trigger = trigger;
	}

	public boolean isTrigger() {
		return trigger;
	}

    public void setMandatory(boolean mand) {
        this.mandatory = mand;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
