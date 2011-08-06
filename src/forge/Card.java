
package forge;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;


public class Card extends MyObservable {
    private static int                   nextUniqueNumber;
    private int                          uniqueNumber                      = nextUniqueNumber++;
    
    private long						 value;
    

    //private Collection keyword   = new TreeSet();
    //private ArrayList<String> keyword = new ArrayList<String>();
    private ArrayList<String>            intrinsicKeyword                  = new ArrayList<String>();
    private ArrayList<String>            extrinsicKeyword                  = new ArrayList<String>();
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
    private ArrayList<SpellAbility>      spellAbility                      = new ArrayList<SpellAbility>();
    private ArrayList<Ability_Mana>      manaAbility                       = new ArrayList<Ability_Mana>();
    
    private HashMap<Card, Integer>       receivedDamageFromThisTurn        = new HashMap<Card, Integer>();
    private HashMap<Card, Integer>       assignedDamageHashMap             = new HashMap<Card, Integer>();
    
    private boolean                      unCastable;
    private boolean                      tapped;
    private boolean                      sickness                          = true;                              //summoning sickness
    private boolean                      token                             = false;
    private boolean 					 checkedPropagandaThisTurn		   = false;
    private boolean                      creatureAttackedThisCombat        = false;
    private boolean                      creatureBlockedThisCombat         = false;
    private boolean                      creatureGotBlockedThisCombat      = false;
    private boolean                      dealtCombatDmgToOppThisTurn       = false;
    private boolean                      dealtDmgToOppThisTurn             = false;
    private boolean                      exaltedBonus                      = false;
    private boolean                      faceDown                          = false;
    private boolean                      sacrificeAtEOT                    = false;
    private boolean                      kicked                            = false;
    
    private boolean                      firstStrike                       = false;
    private boolean                      doubleStrike                      = false;
    
    private boolean                      flashback                         = false;
    
    private int                          exaltedMagnitude                  = 0;
    
    private int                          baseAttack;
    private int                          baseDefense;
    
    private int                          damage;
    
    private int                          nShield;
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
    
    private int 					     multiKickerMagnitude			   = 0;
    
    private String                       owner                             = "";
    private String                       controller                        = "";
    private String                       name                              = "";
    private String                       imageName                         = "";
    private String                       rarity                            = "";
    private String                       text                              = "";
    private String                       manaCost                          = "";
    private String                       upkeepCost                        = "";
    private String                       tabernacleUpkeepCost              = "";
    private String                       magusTabernacleUpkeepCost         = "";
    private String                       echoCost                          = "";
    private String                       chosenType                        = "";
    private String                       chosenColor                       = "";
    private String                       namedCard                         = "";
    

    public ArrayList<Ability_Triggered>  zcTriggers                        = new ArrayList<Ability_Triggered>();
    /*private ArrayList<Command> comesIntoPlayCommandList = new ArrayList<Command>();
    private ArrayList<Command> destroyCommandList       = new ArrayList<Command>();
    private ArrayList<Command> leavesPlayCommandList	  = new ArrayList<Command>();*/
    private ArrayList<Command>           turnFaceUpCommandList             = new ArrayList<Command>();
    private ArrayList<Command>           equipCommandList                  = new ArrayList<Command>();
    private ArrayList<Command>           unEquipCommandList                = new ArrayList<Command>();
    private ArrayList<Command>           enchantCommandList                = new ArrayList<Command>();
    private ArrayList<Command>           unEnchantCommandList              = new ArrayList<Command>();
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
    
    public void setCheckedPropagandaThisTurn(boolean b)
    {
    	checkedPropagandaThisTurn = b;
    }
    
    public boolean getCheckedPropagandaThisTurn()
    {
    	return checkedPropagandaThisTurn;
    }
    
    //used to see if an attacking creature with a triggering attack ability triggered this phase:
    public void setCreatureAttackedThisCombat(boolean b) {
        creatureAttackedThisCombat = b;
    }
    
    public boolean getCreatureAttackedThisCombat() {
        return creatureAttackedThisCombat;
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
    
    public void setDealtDmgToOppThisTurn(boolean b) {
        dealtDmgToOppThisTurn = b;
    }
    
    public boolean getDealtDmgToOppThisTurn() {
        return dealtDmgToOppThisTurn;
    }
    
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
    
    
    public boolean getSacrificeAtEOT() {
        return sacrificeAtEOT || getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME.");
    }
    
    public void setSacrificeAtEOT(boolean sacrificeAtEOT) {
        this.sacrificeAtEOT = sacrificeAtEOT;
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
        int multiplier = 1;
        int doublingSeasons = CardFactoryUtil.getCards("Doubling Season", this.getController()).size();
        if(doublingSeasons > 0) multiplier = (int) Math.pow(2, doublingSeasons);
        
        if(counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) + (multiplier * n);
            counters.put(counterName, aux);
        } else {
            counters.put(counterName, Integer.valueOf(multiplier * n));
        }
        this.updateObservers();
    }
    
    public void subtractCounter(Counters counterName, int n) {
        if(counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) - n;
            counters.put(counterName, aux);
            this.updateObservers();
        }
    }
    
    public int getCounters(Counters counterName) {
        if(counters.containsKey(counterName)) {
            return counters.get(counterName);
        } else return 0;
    }
    
    public void setCounter(Counters counterName, int n) {
        counters.put(counterName, Integer.valueOf(n));
        this.updateObservers();
    }
    
    public String getSVar(String Var) {
        if(SVars.containsKey(Var)) return SVars.get(Var);
        else return "";
    }
    
    public void setSVar(String Var, String str) {
        if(SVars.containsKey(Var)) SVars.remove(Var);
        
        SVars.put(Var, str);
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
    
    //the amount of damage needed to kill the creature
    public int getKillDamage() {
        return getNetDefense() - getDamage();
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
    
    public String getSpellText() {
        return text;
    }
    
    public void setText(String t) {
        text = t;
    }
    
    public String getText() {
        if(isInstant() || isSorcery()) {
            String s = getSpellText();
            StringBuilder sb = new StringBuilder();
            sb.append(s);
            
            SpellAbility[] sa = getSpellAbility();
            for(int i = 0; i < sa.length; i++)
                sb.append(sa[i].toString() + "\r\n");
            
            // Cantrip -> Draw a card.
            if(getKeyword().contains("Draw a card.") && !sb.toString().contains("Draw a card.")) sb.append("Draw a card.\r\n");
            
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
            
            return sb.toString().replaceAll("CARDNAME", getName());
        }
        
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keyword = getKeyword();
        for(int i = 0; i < keyword.size(); i++) {
            if(i != 0) sb.append(", ");
            sb.append(keyword.get(i).toString());
        }
        sb.append("\r\n");
        sb.append(text);
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
    
    public ArrayList<Ability_Mana> getBasicMana() {
        ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();
        for(Ability_Mana am:getManaAbility())
            if(am.isBasic() && !res.contains(am)) res.add(am);
        return res;
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
    
    public void setRarity(String s) {
        rarity = s;
    }
    
    public String getRarity() {
        return rarity;
    }
    
    
    public void addDamage(HashMap<Card, Integer> sourcesMap) {
        Iterator<Entry<Card, Integer>> iter1 = sourcesMap.entrySet().iterator();
        while(iter1.hasNext()) {
            Entry<Card, Integer> entry = iter1.next();
            this.addDamage(entry.getValue(), entry.getKey());
        }
        
        /*
        Iterator<Card> iter = sourcesMap.keySet().iterator();
        while(iter.hasNext()) {
        	Card source = iter.next();	
        	int damage = sourcesMap.get(source);
        	
        	this.addDamage(damage, source);
        }
        */
        //for(Card source : sources)
        //	  this.addDamage(n, source);
        
    }
    
    public void addDamage(int n, Card source) {
        System.out.println("addDamage called on " + this + " for " + n + " damage.");
        if(this.getName().equals("Cho-Manno, Revolutionary")) n = 0;
        //setDamage(getDamage() + n);
        
        if(CardFactoryUtil.canDamage(source, this)) {
            damage += n;
        }
    }
    
    public void setDamage(int n) {
        if(this.getName().equals("Cho-Manno, Revolutionary")) n = 0;
        damage = n;
    }
    
    public int getDamage() {
        return damage;
    }
    
    /*public void setAssignedDamage(int n)
    {
      assignedDamage = n;
    }*/

    public void addAssignedDamage(int n, Card source) {
        System.out.println(this + " - was assigned " + n + " damage, by " + source);
        if(!assignedDamageHashMap.containsKey(source)) assignedDamageHashMap.put(source, n);
        else {
            assignedDamageHashMap.put(source, assignedDamageHashMap.get(source) + n);
        }
    }
    
    //public void setAssignedDamage(int n) {assignedDamage = n;}
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
    
    public String getOwner() {
        return owner;
    }
    
    public String getController() {
        return controller;
    }
    
    public void setName(String s) {
        name = s;
        this.updateObservers();
    }
    
    public void setOwner(String player) {
        owner = player;
        this.updateObservers();
    }
    
    public void setController(String player) {
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
    
    public void setEquipping(ArrayList<Card> list) {
        equipping = list;
    }
    
    public boolean isEquipped() {
        Card c[] = new Card[equippedBy.size()];
        equippedBy.toArray(c);
        return c.length != 0;
    }
    
    public boolean isEquipping() {
        Card c[] = new Card[equipping.size()];
        equippedBy.toArray();
        return c.length != 0;
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
        for(int i = 0; i < equippedBy.size(); i++) {
            equippedBy.get(i).unEquipCard(this);
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
    
    public void setEnchanting(ArrayList<Card> list) {
        enchanting = list;
    }
    
    public boolean isEnchanted() {
        Card c[] = new Card[enchantedBy.size()];
        enchantedBy.toArray(c);
        return c.length != 0;
    }
    
    public boolean isEnchanting() {
        Card c[] = new Card[enchanting.size()];
        enchantedBy.toArray();
        return c.length != 0;
    }
    
    public void addEnchantedBy(Card c) {
        enchantedBy.add(c);
        this.updateObservers();
    }
    
    public void removeEnchantedBy(Card c) {
        enchantedBy.remove(c);
        this.updateObservers();
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
                + getCounters(Counters.P1P1) - getCounters(Counters.M1M1);
        return total;
    }
    
    public int getNetDefense() {
        int total = getBaseDefense();
        total += getTempDefenseBoost() + getSemiPermanentDefenseBoost() + getOtherDefenseBoost()
                + getCounters(Counters.P1P1) - getCounters(Counters.M1M1) - getCounters(Counters.P0M1);
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
        setTapped(true);
    }
    
    public void untap() {
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
        a1.addAll(a2);
        
        for(Ability_Mana sa:getManaAbility())
            if(sa.isBasic()) a1.add((sa).orig);
        
        return a1;
    }
    
    //public void setKeyword(ArrayList a) {keyword = new ArrayList(a); this.updateObservers();}
    //public void addKeyword(String s)     {keyword.add(s);                    this.updateObservers();}
    //public void removeKeyword(String s) {keyword.remove(s);              this.updateObservers();}
    //public int getKeywordSize() 	{return keyword.size();}
    
    public String[] basics = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
    
    public ArrayList<String> getIntrinsicKeyword() {
        return new ArrayList<String>(intrinsicKeyword);
    }
    
    public void setIntrinsicKeyword(ArrayList<String> a) {
        intrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }
    
    public void addIntrinsicKeyword(String s) {/*if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s){}); else*/
        if (s.trim().length()!=0)
        	intrinsicKeyword.add((getName().trim().length()== 0 ? s :s.replaceAll(getName(), "CARDNAME")));
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
        if(s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s) {
            private static final long serialVersionUID = 221124403788942412L;
        });
        else 
        	extrinsicKeyword.add((getName().trim().length()==0 ? s :s.replaceAll(getName(), "CARDNAME")));
        //}
    }
    
    public void addStackingExtrinsicKeyword(String s) {
    	if (s.startsWith("tap: add")) manaAbility.add(new Ability_Mana(this, s)
    	{
    		private static final long serialVersionUID = 2443750124751086033L;  
    	});
    	else extrinsicKeyword.add(s);
    }
    
    public void removeExtrinsicKeyword(String s) {
        extrinsicKeyword.remove(s);
        this.updateObservers();
    }
    
    public int getExtrinsicKeywordSize() {
        return extrinsicKeyword.size();
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
    
    
    public boolean isPermanent() {
        return !(isInstant() || isSorcery());
    }
    
    public boolean isCreature() {
        return type.contains("Creature");
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
    
    public void setKicked(boolean b) {
        kicked = b;
    }
    
    public boolean isKicked() {
        return kicked;
    }
}
