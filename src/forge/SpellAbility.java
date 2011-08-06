package forge;

//only SpellAbility can go on the stack
//override any methods as needed
public abstract class SpellAbility
{
  public Object[] choices_made;//open ended Casting choice storage
  //choices for constructor isPermanent argument
  public static final int Spell = 0;
  public static final int Ability = 1;
  public static final int Ability_Tap = 2;

  private String description        = "";
  private String targetPlayer       = "";
  private String stackDescription = "";
  private String manaCost          = "";
  private String additionalManaCost= "";
  
  private String type = "Intrinsic"; //set to Intrinsic by default

  private Card targetCard;
  private Card sourceCard;

  private boolean spell;
  private boolean tapAbility;
  private boolean buyBackAbility = false; //false by default
  private boolean flashBackAbility = false;

  private Input beforePayMana;
  private Input afterResolve;
  private Input afterPayMana;
  
  private Command cancelCommand = Command.Blank;
  private Command beforePayManaAI = Command.Blank;

  private CommandArgs randomTarget = new CommandArgs() {
  
  private static final long serialVersionUID = 1795025064923737374L;

public void execute(Object o) {}};

  public SpellAbility(int spellOrAbility, Card i_sourceCard)
  {
    if(spellOrAbility == Spell)
      spell = true;
    else if(spellOrAbility == Ability)
      spell = false;
    else if(spellOrAbility == Ability_Tap)
    {
      spell        = false;
      tapAbility = true;
    }
    
    else
      throw new RuntimeException("SpellAbility : constructor error, invalid spellOrAbility argument = " +spellOrAbility);
    
    

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
  public boolean canPlayAI() {return true;}

  public void        chooseTargetAI()                 {randomTarget.execute(this);}
  public void        setChooseTargetAI(CommandArgs c) {randomTarget = c;}
  public CommandArgs getChooseTargetAI()              {return randomTarget;}

  public String getManaCost()                {return manaCost;}
  public void   setManaCost(String cost) {manaCost = cost;}
  
  public String getAdditionalManaCost()  {return additionalManaCost;}
  public void   setAdditionalManaCost(String cost) { additionalManaCost = cost; }

  public boolean isSpell()        {return spell; }
  public boolean isAbility()     {return ! isSpell(); }
  public boolean isTapAbility() {return tapAbility;}
  
  public void setIsBuyBackAbility(boolean b) { buyBackAbility = b;}
  public boolean isBuyBackAbility() 		 {return buyBackAbility; }

  public void setSourceCard(Card c) {sourceCard=c;}
  public Card getSourceCard()  {return sourceCard;}
  
  public Command getBeforePayManaAI() 		 { return beforePayManaAI; }
  public void setBeforePayManaAI(Command c)  { beforePayManaAI = c; }
  
  //begin - Input methods
  public Input getBeforePayMana()            {return beforePayMana;}
  public void  setBeforePayMana(Input in) {beforePayMana = in; }
  
  public Input getAfterPayMana()            {return afterPayMana;}
  public void  setAfterPayMana(Input in) {afterPayMana = in;}

  public Input getAfterResolve()             {return afterResolve;}
  public void setAfterResolve (Input in)  {afterResolve = in;}

  public void setStackDescription(String s) {stackDescription = s;}
  public String getStackDescription()
  {
    if(stackDescription.equals(getSourceCard().getText().trim()))
      return getSourceCard().getName() +" - " +getSourceCard().getText();

    return stackDescription;
  }
  
  public boolean isIntrinsic()
  {
  	return type.equals("Intrinsic");
  }
  public boolean isExtrinsic()
  {
  	return type.equals("Extrinsic");
  }
  
  public void setType(String s) //Extrinsic or Intrinsic:
  {
	  type = s;
  }

  //setDescription() includes mana cost and everything like
  //"G, tap: put target creature from your hand into play"
  public void setDescription(String s)  {description = s;}
  public String toString()                     {return description;}

  public Card getTargetCard()
  {
    if(targetCard == null)
      return null;

    return targetCard;
  }
  public void setTargetCard(Card card)
  {
    targetPlayer = null;//reset setTargetPlayer()

    targetCard = card;
    String desc = "";
    if (!card.isFaceDown())
    	desc = getSourceCard().getName() + " - targeting " +card;
    else
    	desc = getSourceCard().getName() + " - targeting Morph(" + card.getUniqueNumber() + ")";
    setStackDescription(desc);
  }
  public void setTargetPlayer(String p)
  {
    targetCard = null;//reset setTargetCard()

    if(p == null || (!(p.equals(Constant.Player.Human) || p.equals(Constant.Player.Computer))))
      throw new RuntimeException("SpellAbility : setTargetPlayer() error, argument is " +p  +" source card is " +getSourceCard());
    targetPlayer = p;
    setStackDescription(getSourceCard().getName() +" - targeting " +p);
  }
  public String getTargetPlayer()            {return targetPlayer;}
  
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
}