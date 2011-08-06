
package forge;


//only SpellAbility can go on the stack
//override any methods as needed
public abstract class SpellAbility {
    public Object[]         choices_made;                       //open ended Casting choice storage
    //choices for constructor isPermanent argument
    public static final int Spell              = 0;
    public static final int Ability            = 1;
    public static final int Ability_Tap        = 2;
    
    private String          description        = "";
    private Player          targetPlayer       = null;
    private String          stackDescription   = "";
    private String          manaCost           = "";
    private String          additionalManaCost = "";
    private String			multiKickerManaCost= "";
    private String 			xManaCost		   = "";
    private Player			activatingPlayer   = null;
    
    private String          type               = "Intrinsic";  //set to Intrinsic by default
                                                                
    private Card            targetCard;
    private Card            sourceCard;
    
    private CardList        targetList;
    
    private boolean         spell;
    private boolean         tapAbility;
    private boolean         untapAbility;
    private boolean         buyBackAbility     = false;        //false by default
    private boolean         flashBackAbility   = false;
    private boolean			multiKicker 	   = false;
    private boolean 		xCost			   = false;
    private boolean         kickerAbility      = false;
    private boolean 		kothThirdAbility   = false;
    
    private Input           beforePayMana;
    private Input           afterResolve;
    private Input           afterPayMana;
    
    protected Ability_Cost	payCosts		   = null;
    protected Target		chosenTarget	   = null;
    
    private SpellAbility_Restriction restrictions = new SpellAbility_Restriction();
    
    private CardList 		sacrificedCards	   = null;
    
    private Command         cancelCommand      = Command.Blank;
    private Command         beforePayManaAI    = Command.Blank;
    
    private CommandArgs     randomTarget       = new CommandArgs() {
                                                   
                                                   private static final long serialVersionUID = 1795025064923737374L;
                                                   
                                                   public void execute(Object o) {}
                                               };
    
    public SpellAbility(int spellOrAbility, Card i_sourceCard) {
        if(spellOrAbility == Spell) spell = true;
        else if(spellOrAbility == Ability) spell = false;
        else if(spellOrAbility == Ability_Tap) {
            spell = false;
            tapAbility = true;
        }

        else throw new RuntimeException("SpellAbility : constructor error, invalid spellOrAbility argument = "
                + spellOrAbility);
        

        sourceCard = i_sourceCard;
    }
    
    //Spell, and Ability, and other Ability objects override this method
    abstract public boolean canPlay();
    
    //all Spell's and Abilities must override this method
    abstract public void resolve();
    
    /*
      public boolean canPlayAI()
      {
        return true;
      }
      public void chooseTargetAI()
      {

      }
    */
    public boolean canPlayAI() {
        return true;
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
    
    public String getXManaCost()
    {
    	return xManaCost;
    }
    
    public void setXManaCost(String cost)
    {
    	xManaCost = cost;
    }
    
    public Player getActivatingPlayer()
    {
    	return activatingPlayer;
    }
    
    public void setActivatingPlayer(Player player)
    {
    	activatingPlayer = player;
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
    
    public void setIsXCost(boolean b) {
    	xCost = b;
    }
    
    public boolean isXCost(){
    	return xCost;
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
    
    public Ability_Cost getPayCosts() {
    	return payCosts;
    }
    
    public void setPayCosts(Ability_Cost abCost) {
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
    
    public void addSacrificedCost(Card c){
    	if (sacrificedCards == null)
    		sacrificedCards = new CardList();
    	sacrificedCards.add(c);
    }
    
    public CardList getSacrificedCost(){
    	return sacrificedCards;
    }
    
    public void resetSacrificedCost(){
    	sacrificedCards = null;
    }
    
    public Input getAfterResolve() {
        return afterResolve;
    }
    
    public void setAfterResolve(Input in) {
        afterResolve = in;
    }
    
    public void setStackDescription(String s) {
        stackDescription = s;
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
    
    //setDescription() includes mana cost and everything like
    //"G, tap: put target creature from your hand into play"
    public void setDescription(String s) {
        description = s;
    }
    
    @Override
    public String toString() {
    	if (description.contains("CARDNAME"))
    		description = description.replace("CARDNAME", this.getSourceCard().getName());
        return description;
    }
    
    public Card getTargetCard() {
        if(targetCard == null) return null;
        
        return targetCard;
    }
    
    public void setTargetCard(Card card) {
    	targetPlayer = null;//reset setTargetPlayer()

    	targetCard = card;
    	String desc = "";
    	if(null != card) {
    		if(!card.isFaceDown()) desc = getSourceCard().getName() + " - targeting " + card;
    		else desc = getSourceCard().getName() + " - targeting Morph(" + card.getUniqueNumber() + ")";
    		setStackDescription(desc);
    	}
    	else {
    		System.out.println(getSourceCard()+" - SpellAbility.setTargetCard() called with null for target card.");
    	}
        //System.out.println(card + " has become target of a spell or ability (" +this.getSourceCard() + ")");
    }
    
    public CardList getTargetList() {
        if (targetList == null) return null;
        
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
        targetCard = null;//reset setTargetCard()
        
        if(p == null || (!(p.isHuman() || p.isComputer()))) throw new RuntimeException(
                "SpellAbility : setTargetPlayer() error, argument is " + p + " source card is " + getSourceCard());
        targetPlayer = p;
        setStackDescription(getSourceCard().getName() + " - targeting " + p);
    }
    
    public Player getTargetPlayer() {
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
}
