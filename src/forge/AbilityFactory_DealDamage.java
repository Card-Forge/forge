
    package forge;

    import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

    public class AbilityFactory_DealDamage {
        private AbilityFactory AF = null;
        
        private String damage;

        private boolean TgtOpp = false;
        
        private Ability_Sub subAbAF = null;
        private boolean hasSubAbAF = false;
        private String subAbStr = "none";
        private boolean hasSubAbStr = false;
    	
    	public Ability_Sub getSubAbility() { return subAbAF; }
        
    	public AbilityFactory_DealDamage(AbilityFactory newAF)
    	{
    		AF = newAF;

    		damage = AF.getMapParams().get("NumDmg");

    		if(AF.getMapParams().containsKey("Tgt"))
    			if (AF.getMapParams().get("Tgt").equals("TgtOpp"))
    				TgtOpp = true;

    		if(AF.hasSubAbility())
    		{
    			String sSub = AF.getMapParams().get("SubAbility");

    			if (sSub.startsWith("SVar="))
    				sSub = AF.getHostCard().getSVar(sSub.split("=")[1]);

    			if (sSub.startsWith("DB$"))
    			{
    				AbilityFactory afDB = new AbilityFactory();
    				subAbAF = (Ability_Sub)afDB.getAbility(sSub, AF.getHostCard());
    				hasSubAbAF = true;
    			}
    			else
    			{
    				subAbStr = sSub;
    				hasSubAbStr = true;
    			}
    		}

    	}
       
       public SpellAbility getAbility()
       {
            final SpellAbility abDamage = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt())
            {
               private static final long serialVersionUID = -7560349014757367722L;
               
                @Override
                public boolean canPlay(){
                    return super.canPlay();
                }
               
                @Override
                public boolean canPlayAI() {
                   return doCanPlayAI(this);
                   
                }
               
             @Override
             public String getStackDescription(){
                return damageStackDescription(AF, this);
             }
               
                @Override
                public void resolve() {
                   doResolve(this);
                   AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);
                   
                }
            };//Ability_Activated
          
          return abDamage;
       }
       
       public SpellAbility getSpell()
       {
          final SpellAbility spDealDamage = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
                private static final long serialVersionUID = 7239608350643325111L;

                @Override
                public boolean canPlay(){
                    return super.canPlay();
                }
               
                @Override
                public boolean canPlayAI() {
                   return doCanPlayAI(this);
                   
                }
               
             @Override
             public String getStackDescription(){
                return damageStackDescription(AF, this);
             }
               
                @Override
                public void resolve() {
                   doResolve(this);
                   
                }

           
            }; // Spell
           
          return spDealDamage;
       }

       public SpellAbility getDrawback()
       {
		final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean chkAI_Drawback() {
				// Make sure there is a valid target
				return damageTargetAI(this);
			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

		}; // Spell

		return dbDealDamage;
	}

       
        private int getNumDamage(SpellAbility saMe) {
            return AbilityFactory.calculateAmount(saMe.getSourceCard(), damage, saMe);
        }
       
        private boolean shouldTgtP(int d, final boolean noPrevention) {
        	int restDamage = d;
        	
        	if (!noPrevention)
        		restDamage = AllZone.HumanPlayer.staticDamagePrevention(restDamage,AF.getHostCard(),false);
        	
        	if (restDamage == 0) return false;
        	
            PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
            CardList hand = new CardList(compHand.getCards());
           
            if(AF.isSpell() && hand.size() > 7) // anti-discard-at-EOT
               return true;
           
            if(AllZone.HumanPlayer.getLife() - restDamage < 10) // if damage from this spell would drop the human to less than 10 life
               return true;
           
            return false;
        }
       
        private Card chooseTgtC(final int d, final boolean noPrevention) {
           
            PlayerZone human = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
            CardList hPlay = new CardList(human.getCards());
            hPlay = hPlay.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                	int restDamage = d;
                	if (!noPrevention)
                		restDamage = c.staticDamagePrevention(d,AF.getHostCard(),false);
                    // will include creatures already dealt damage
                    return c.isCreature() && (c.getKillDamage() <= restDamage)
                            && CardFactoryUtil.canTarget(AF.getHostCard(), c)
                            && !c.getKeyword().contains("Indestructible")
                            && !(c.getSVar("SacMe").length() > 0);
                }
            });
           
            if(hPlay.size() > 0) {
                Card best = CardFactoryUtil.AI_getBestCreature(hPlay);
                return best;
            }
            
            // Combo alert!!
            PlayerZone compy = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
            CardList cPlay = new CardList(compy.getCards());
            if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
            
            return null;
        }

        private boolean doCanPlayAI(SpellAbility saMe)
        {           
        	int dmg = getNumDamage(saMe);
           boolean rr = AF.isSpell();
           
           // temporarily disabled until better AI
           if (AF.getAbCost().getSacCost())    {
               if(AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
        	   return false;
           }
           if (AF.getAbCost().getSubCounter())  {
        	   // +1/+1 counters only if damage from this ability would kill the human, otherwise ok
        	   if(AllZone.HumanPlayer.getLife() - dmg > 0 && AF.getAbCost().getCounterType().equals(Counters.P1P1))
        	   return false;
           }
           if (AF.getAbCost().getLifeCost())    {
               if(AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
        	   return false;
           }
           
           if (!ComputerUtil.canPayCost(saMe))
              return false;

          // TODO handle proper calculation of X values based on Cost
           
           if(AF.getHostCard().equals("Stuffy Doll")) {
        	   return true;
           }
           
            if (AF.isAbility())
            {
               Random r = new Random(); // prevent run-away activations
               if(r.nextFloat() <= Math.pow(.6667, AF.getHostCard().getAbilityUsed()))
                  rr = true;
            }
           
            boolean bFlag = damageTargetAI(saMe);
            if (!bFlag)
            	return false;
           
          Ability_Sub subAb = saMe.getSubAbility();
          if (subAb != null)
        	  rr &= subAb.chkAI_Drawback();
            return rr;
           
        }
        
	private boolean damageTargetAI(SpellAbility saMe) {
		int dmg = getNumDamage(saMe);
		Target tgt = AF.getAbTgt();
        HashMap<String,String> params = AF.getMapParams();
		
		boolean noPrevention = params.containsKey("NoPrevention");
		
		// AI handle multi-targeting?
		if (tgt == null){
			if (AF.getMapParams().containsKey("Affected")){
        	   String affected = AF.getMapParams().get("Affected");
        	   if (affected.equals("You"))
        		   // todo: when should AI not use an SA like Psionic Blast?
        		   ;
        	   else if (affected.equals("Self"))
        		   // todo: when should AI not use an SA like Orcish Artillery?
        		   ;
           }
		   return true;
		}
		
		
		tgt.resetTargets();

		// target loop
		while (tgt.getNumTargeted() < tgt.getMaxTargets(saMe.getSourceCard(), saMe)) {
			// TODO: Consider targeting the planeswalker
			if (tgt.canTgtCreatureAndPlayer()) {

				if (shouldTgtP(dmg,noPrevention)) {
					tgt.addTarget(AllZone.HumanPlayer);
					continue;
				}

				Card c = chooseTgtC(dmg,noPrevention);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}

			if (tgt.canTgtPlayer() || TgtOpp) {
				tgt.addTarget(AllZone.HumanPlayer);
				continue;
			}

			if (tgt.canTgtCreature()) {
				Card c = chooseTgtC(dmg,noPrevention);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}
			// fell through all the choices, no targets left?
			if (tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)
					|| tgt.getNumTargeted() == 0) {
				tgt.resetTargets();
				return false;
			} else {
				// todo is this good enough? for up to amounts?
				break;
			}
		}
		return true;
	}
       
        private String damageStackDescription(AbilityFactory af, SpellAbility sa){
          // when damageStackDescription is called, just build exactly what is happening
           StringBuilder sb = new StringBuilder();
           String name = af.getHostCard().getName();
           int dmg = getNumDamage(sa);

           ArrayList<Object> tgts = findTargets(sa);
           
           if (!(sa instanceof Ability_Sub))
        	   sb.append(name).append(" - ");
           
           sb.append("Deals ").append(dmg).append(" damage to ");
           if(tgts == null || tgts.size() == 0) {
        	   sb.append("itself");
           }
           else {
        	   for(int i = 0; i < tgts.size(); i++){
        		   if (i != 0)
        			   sb.append(" ");

        		   Object o = tgts.get(0);
        		   if (o instanceof Player){
        			   sb.append(((Player)o).getName());
        		   }
        		   else{
        			   sb.append(((Card)o).getName());
        		   }

        	   }
           }
           sb.append(". ");
           
           if (hasSubAbAF){
        	   subAbAF.setParent(sa);
        	   sb.append(subAbAF.getStackDescription());
           }

           return sb.toString();
        }
       
        private  ArrayList<Object> findTargets(SpellAbility saMe){
            Target tgt = AF.getAbTgt();
            ArrayList<Object> tgts;
            if (tgt != null)
               tgts = tgt.getTargets();
            else{
               tgts = new ArrayList<Object>();
               if (TgtOpp){
               		tgts.add(saMe.getActivatingPlayer().getOpponent());
               }
               else if (AF.getMapParams().containsKey("Affected")){
            	   String affected = AF.getMapParams().get("Affected");
            	   if (affected.equals("You"))
            		   tgts.add(saMe.getActivatingPlayer());
            	   else if (affected.equals("Self"))
            		   tgts.add(saMe.getSourceCard());
               }
            }
            return tgts;
        }
        
        private void doResolve(SpellAbility saMe)
        {
            int dmg = getNumDamage(saMe);
            HashMap<String,String> params = AF.getMapParams();
    		
    		boolean noPrevention = params.containsKey("NoPrevention");

            ArrayList<Object> tgts = findTargets(saMe);
            boolean targeted = (AF.getAbTgt() != null) || TgtOpp;

            if (tgts == null || tgts.size() == 0){
            	System.out.println("AF_DealDamage ("+AF.getHostCard()+") - No targets?  Ok.  Just making sure.");
            	//if no targets, damage goes to self (Card; i.e. Stuffy Doll)
            	Card c = saMe.getSourceCard();
            	if(AllZone.GameAction.isCardInPlay(c)) {
    				if (noPrevention)
    					c.addDamageWithoutPrevention(dmg, AF.getHostCard());
    				else
    					c.addDamage(dmg, AF.getHostCard());
    			}
            }
            else {
            	for(Object o : tgts){
            		if (o instanceof Card){
            			Card c = (Card)o;
            			if(AllZone.GameAction.isCardInPlay(c) && (!targeted || CardFactoryUtil.canTarget(AF.getHostCard(), c))) {
            				if (noPrevention)
            					c.addDamageWithoutPrevention(dmg, AF.getHostCard());
            				else
            					c.addDamage(dmg, AF.getHostCard());
            			}

            		}
            		else if (o instanceof Player){
            			Player p = (Player) o;
            			if (!targeted || p.canTarget(AF.getHostCard())) {
            				if (noPrevention)
            					p.addDamageWithoutPrevention(dmg, AF.getHostCard());
            				else
            					p.addDamage(dmg, AF.getHostCard());
            			}
            		}
            	}
            }
   
           if (hasSubAbAF) {
        	   if (subAbAF.getParent() == null)
        		   subAbAF.setParent(saMe);
			   subAbAF.resolve();
           }
           
           else if (hasSubAbStr){
               Object obj = tgts.get(0);
               
               Player pl = null;
               Card c = null;
              
               if (obj instanceof Card){
                  c = (Card)obj;
                  pl = c.getController();
               }
               else{
                  pl = (Player)obj;
               }
              CardFactoryUtil.doDrawBack(subAbStr, dmg, AF.getHostCard().getController(),
                 AF.getHostCard().getController().getOpponent(),   pl, AF.getHostCard(), c, saMe);
              
           }

        }
    }
