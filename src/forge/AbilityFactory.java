package forge;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AbilityFactory {

	public SpellAbility getAbility(String abString, final Card hostCard){
		final SpellAbility SA[] = {null};
		
		final HashMap<String,String> mapParams = new HashMap<String,String>();
		
		if (!(abString.length() > 0))
			throw new RuntimeException("AbilityFactory : getAbility -- abString too short in " + hostCard.getName());
		
		String a[] = abString.split("\\|");
		
		if (!(a.length > 1))
			throw new RuntimeException("AbilityFactory : getAbility -- a[] too short in " + hostCard.getName());
			
		for (int i=0; i<a.length; i++)
		{
			String aa[] = a[i].split("\\$");
			
			if (!(aa.length == 2))
				throw new RuntimeException("AbilityFactory : getAbility -- aa.length not 2 in " + hostCard.getName());
			
			mapParams.put(aa[0], aa[1]);
		}
		
		
		final boolean isAb[] = {false};
		final boolean isSp[] = {false};
		String abAPI = "";
		String spAPI = "";
		// additional ability types here
		if (mapParams.containsKey("AB"))
		{
			isAb[0] = true;
			abAPI = mapParams.get("AB");
		}
		else if (mapParams.containsKey("SP"))
		{
			isSp[0] = true;
			spAPI = mapParams.get("SP");
		}
		else
			throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());

		
		if (!mapParams.containsKey("Cost"))
			throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
		final Ability_Cost abCost = new Ability_Cost(mapParams.get("Cost"), hostCard.getName(), isAb[0]);
		
		
		final boolean isTargeted[] = {false};
		final boolean hasValid[] = {false};
		final Target abTgt[] = {null};
		if (mapParams.containsKey("ValidTgts"))
		{
			hasValid[0] = true;
			isTargeted[0] = true;
			abTgt[0] = new Target("V");
			abTgt[0].setValidTgts(mapParams.get("ValidTgts").split(","));
			abTgt[0].setVTSelection(mapParams.get("TgtPrompt"));
		}
		
		if (mapParams.containsKey("ValidCards"))
			hasValid[0] = true;
		
		if (mapParams.containsKey("Tgt"))
		{
			isTargeted[0] = true;
			abTgt[0] = new Target(mapParams.get("Tgt"));
		}
		
		
		//final String SubAbility[] = {"none"};
		final boolean hasSubAb[] = {false};
		if (mapParams.containsKey("SubAbility"))
			hasSubAb[0] = true;
			//SubAbility[0] = mapParams;
		
		final String spDescription[] = {"none"};
		final boolean hasSpDesc[] = {false};
		//String tmpSpDesc = mapParams.get("SpellDescription");
		if (mapParams.containsKey("SpellDescription"))
		{
			hasSpDesc[0] = true;
			spDescription[0] = abCost.toString() + mapParams.get("SpellDescription");
		}
		
		
		if (abAPI.equals("DealDamage"))
		{
            final int NumDmg[] = {-1};
            final String NumDmgX[] = {"none"};
            String tmpND = mapParams.get("NumDmg");
            if (tmpND.length() > 0)
            {
            	if (tmpND.matches("X"))
            		NumDmgX[0] = hostCard.getSVar(tmpND.substring(1));
            	
            	else if (tmpND.matches("[0-9][0-9]?"))
            		NumDmg[0] = Integer.parseInt(tmpND);
            }
 
            final SpellAbility abDamage = new Ability_Activated(hostCard, abCost, abTgt[0])
            {
            	private static final long serialVersionUID = -7560349014757367722L;
            	
            	int damage = 0;
            	
                public int getNumDamage() {
                    if(NumDmg[0] != -1) return NumDmg[0];
                    
                    if(!NumDmgX[0].equals("none")) return CardFactoryUtil.xCount(hostCard, NumDmgX[0]);
                    
                    return 0;
                }
                
                boolean shouldTgtP() {
                    PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
                    CardList hand = new CardList(compHand.getCards());
                    
                    if(hand.size() > 7) // anti-discard-at-EOT
                    	return true;
                    
                    if(AllZone.Human_Life.getLife() - damage < 10) // if damage from this spell would drop the human to less than 10 life
                    	return true;
                    
                    return false;
                }
                
                Card chooseTgtC() {
                    // Combo alert!!
                    PlayerZone compy = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList cPlay = new CardList(compy.getCards());
                    if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                        if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
                    
                    PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList hPlay = new CardList(human.getCards());
                    hPlay = hPlay.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            // will include creatures already dealt damage
                            return c.isCreature() && ((c.getNetDefense() + c.getDamage()) <= damage)
                                    && CardFactoryUtil.canTarget(hostCard, c);
                        }
                    });
                    
                    if(hPlay.size() > 0) {
                        Card best = hPlay.get(0);
                        
                        if(hPlay.size() > 1) {
                            for(int i = 1; i < hPlay.size(); i++) {
                                Card b = hPlay.get(i);
                                // choose best overall creature?
                                if(b.getSpellAbility().length > best.getSpellAbility().length
                                        || b.getKeyword().size() > best.getKeyword().size()
                                        || b.getNetAttack() > best.getNetAttack()) best = b;
                            }
                        }
                        return best;
                    }
                    return null;
                }
                
                @Override
                public boolean canPlay(){
                    return (Cost_Payment.canPayAdditionalCosts(abCost, this) && CardFactoryUtil.canUseAbility(hostCard) && super.canPlay());
                }
                
                @Override
                public boolean canPlayAI() {
                	// temporarily disabled until better AI
                	if (abCost.getSacCost())	 return false;
                	if (abCost.getSubCounter())  return false;
                	if (abCost.getLifeCost())	 return false;
                	
                	if (!ComputerUtil.canPayCost(this))
                		return false;

                    damage = getNumDamage();
                    
                    Random r = new Random(); // prevent run-away activations 
                    boolean rr = false;
                    if(r.nextFloat() <= Math.pow(.6667, hostCard.getAbilityUsed())) 
                    	rr = true;
                    
                    if(abTgt[0].canTgtCreaturePlayer()) {
                        if(shouldTgtP()) {
                            setTargetPlayer(Constant.Player.Human);
                            return rr;
                        }
                        
                        Card c = chooseTgtC();
                        if(c != null) {
                            setTargetCard(c);
                            return rr;
                        }
                    }
                    
                    if(abTgt[0].canTgtPlayer()/* || TgtOpp[0] == true */) {
                        setTargetPlayer(Constant.Player.Human);
                        return rr;
                    }
                    
                    if(abTgt[0].canTgtCreature()) {
                        Card c = chooseTgtC();
                        if(c != null) {
                            setTargetCard(c);
                            return rr;
                        }
                    }
                    return false;
                }
                
                @Override
                public void resolve() {
                    int damage = getNumDamage();
                    String tgtP = "";
                    
                    //if(TgtOpp[0] == true) {
                    //    tgtP = AllZone.GameAction.getOpponent(card.getController());
                    //    setTargetPlayer(tgtP);
                    //}
                    Card c = getTargetCard();
                    if(c != null) {
                        if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(hostCard, c)) {
                            AllZone.GameAction.addDamage(c, hostCard, damage);
                            tgtP = c.getController();
                            
                            if(hasSubAb[0])
                            	CardFactoryUtil.doDrawBack(mapParams.get("SubAbility"), damage,
                                    hostCard.getController(), AllZone.GameAction.getOpponent(hostCard.getController()),
                                    tgtP, hostCard, c, this);

                        }
                    } else {
                        tgtP = getTargetPlayer();
                        AllZone.GameAction.addDamage(tgtP, hostCard, damage);
                        
                        if(hasSubAb[0]) 
                        	CardFactoryUtil.doDrawBack(mapParams.get("SubAbility"), damage,
                                hostCard.getController(), AllZone.GameAction.getOpponent(hostCard.getController()),
                                tgtP, hostCard, null, this);

                    }
                    
                }//resolve()
            };//Ability_Activated

            if (isTargeted[0])
            	abDamage.setTarget(abTgt[0]);
            
            abDamage.setPayCosts(abCost);
            
            if (hasSpDesc[0])
            	abDamage.setDescription(spDescription[0]);
            
            SA[0] = abDamage;            	
		}
		
		// additional keywords here
		
		return SA[0];
	}
	
	
}

