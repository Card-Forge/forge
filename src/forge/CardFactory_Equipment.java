package forge;
class CardFactory_Equipment {
	public static Card getCard(final Card card, String cardName, String owner)
	{

		//*************** START *********** START **************************
	    if (cardName.equals("Lightning Greaves"))
	    {
	    	final Ability equip = new Ability(card, "0")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}	
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}
	    		
	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                         (! c.getKeyword().contains("Haste")) && (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    
				private static final long serialVersionUID = 277714373478367657L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addExtrinsicKeyword("Haste");
	    				crd.addExtrinsicKeyword("Shroud");
	    			}  
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    

				private static final long serialVersionUID = -3427116314295067303L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.removeExtrinsicKeyword("Haste");
	    				crd.removeExtrinsicKeyword("Shroud");
	    					
	    			}
		          
		        }//execute()
		    };//Command
		    
	    	
	    	Input runtime = new Input()
	        {

				private static final long serialVersionUID = 3195056500461797420L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 2");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("Loxodon Warhammer"))
	    {
	    	final Ability equip = new Ability(card, "3")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}	
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}
	    		
	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
				
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    

				private static final long serialVersionUID = 8130682765214560887L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addExtrinsicKeyword("Trample");
	    				crd.addExtrinsicKeyword("Lifelink");
	    				crd.addSemiPermanentAttackBoost(3);
	    			}  
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    

				private static final long serialVersionUID = 5783423127748320501L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.removeExtrinsicKeyword("Trample");
	    				crd.removeExtrinsicKeyword("Lifelink");
	    				crd.addSemiPermanentAttackBoost(-3);
	    					
	    			}
		          
		        }//execute()
		    };//Command
		    
	    	
	    	Input runtime = new Input()
	    	{
				private static final long serialVersionUID = -6785656229070523470L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 3");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Behemoth Sledge"))
	    {
	    	final Ability equip = new Ability(card, "3")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}	
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}
	    		
	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                  (! c.getKeyword().contains("Defender"));
	                }
	              });
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    

				private static final long serialVersionUID = 8130682765214560887L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addExtrinsicKeyword("Lifelink");
	    				crd.addExtrinsicKeyword("Trample");
	    				crd.addSemiPermanentAttackBoost(2);
	    				crd.addSemiPermanentDefenseBoost(2);
	    			}  
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    

				private static final long serialVersionUID = 5783423127748320501L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.removeExtrinsicKeyword("Lifelink");
	    				crd.removeExtrinsicKeyword("Trample");
	    				crd.addSemiPermanentAttackBoost(-2);
	    				crd.addSemiPermanentDefenseBoost(-2);
	    					
	    			}
		          
		        }//execute()
		    };//Command
		    
	    	
	    	Input runtime = new Input()
	    	{
				private static final long serialVersionUID = -6785656229070523470L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 3");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Fireshrieker"))
	    {
	    	final Ability equip = new Ability(card, "2")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}	
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}
	    		
	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                  (!c.getKeyword().contains("Double Strike") )&& (! c.getKeyword().contains("Defender"));
	                }
	              });
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    

				private static final long serialVersionUID = 277714373478367657L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addExtrinsicKeyword("Double Strike");
	    			}  
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    

				private static final long serialVersionUID = -3427116314295067303L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.removeExtrinsicKeyword("Double Strike");
	    					
	    			}
		          
		        }//execute()
		    };//Command
		    
	    	
	    	Input runtime = new Input()
	        {
				private static final long serialVersionUID = 3195056500461797420L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 2");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Skullclamp"))
	    {
	    	final Ability equip = new Ability(card, "1")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}	
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}

	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                  (! c.getKeyword().contains("Defender"));
	                }
	              });
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    
				private static final long serialVersionUID = 277714373478367657L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);

	    				crd.addSemiPermanentAttackBoost(1);
	    				crd.addSemiPermanentDefenseBoost(-1);
	    			}  
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    

				private static final long serialVersionUID = 6496501799243208207L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				
	    				crd.addSemiPermanentAttackBoost(-1);
	    				crd.addSemiPermanentDefenseBoost(1);
	    			}
		          
		        }//execute()
		    };//Command
		    
	    	
	    	Input runtime = new Input()
	        {
				private static final long serialVersionUID = -5844375382897176476L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 1");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Bonesplitter"))
	    {
	    	final Ability equip = new Ability(card, "1")
	    	{
	    		public void resolve()
	    		{
	    			if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	    			{
		    			if (card.isEquipping())
		    			{
		    				Card crd = card.getEquipping().get(0);
		    				if (crd.equals(getTargetCard()) )
		    					return;
		    				
		    				card.unEquipCard(crd);
		    			}
	    			
		    			card.equipCard(getTargetCard());
	    			}
	    		}
	    		
	    		public boolean canPlay()
	    		{
	    			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	                	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	    		}

	    		public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	    		
	    		public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) && 
	                  (! c.getKeyword().contains("Defender"));
	                }
	              });
	              return list;
	            }//getCreature()
	    		
	    	};//equip ability
	    	

	    	Command onEquip = new Command()
		    {    
				private static final long serialVersionUID = -6930553087037330743L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addSemiPermanentAttackBoost(2);
	    			}
		          
		        }//execute()
		    };//Command
		    

	    	Command onUnEquip = new Command()
		    {    
				private static final long serialVersionUID = -3427116314295067303L;

				public void execute()
		        {
					if (card.isEquipping())
	    			{
	    				Card crd = card.getEquipping().get(0);
	    				crd.addSemiPermanentAttackBoost(-2);
	    			}
		          
		        }//execute()
		    };//Command
	    	
		    Input runtime = new Input()
	        {
				private static final long serialVersionUID = 5184756493874218024L;

				public void showMessage()
		          {
		            //get all creatures you control
		            CardList list = new CardList();
		            list.addAll(AllZone.Human_Play.getCards());
		            list = list.getType("Creature");
		            
		            stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		          }
	        };//Input
		    
	    	equip.setBeforePayMana(runtime);
	    	
	    	equip.setDescription("Equip: 1");
	    	card.addSpellAbility(equip);
	    	
	    	card.addEquipCommand(onEquip);
	    	card.addUnEquipCommand(onUnEquip);
	    } //*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("Trailblazer's Boots"))
	    {
	       final Ability equip = new Ability(card, "2")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	  		  {
	  			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	              	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	              	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	  		  }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -8783427230086868847L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addExtrinsicKeyword("Nonbasic landwalk");
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = 732383503612045113L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeExtrinsicKeyword("Nonbasic landwalk");
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
	    	   
			private static final long serialVersionUID = -696882688005519805L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 2");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Blight Sickle"))
	    {
	       final Ability equip = new Ability(card, "2")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	  		  {
	  			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	              	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	              	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	  		  }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

	         private static final long serialVersionUID = 8130682765214560887L;

	         public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addExtrinsicKeyword("Wither");
	                crd.addSemiPermanentAttackBoost(1);
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

	         private static final long serialVersionUID = 5783423127748320501L;

	         public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeExtrinsicKeyword("Wither");
	                crd.addSemiPermanentAttackBoost(-1);
	                   
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
	         
			private static final long serialVersionUID = -8564484340029497370L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 2");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Spidersilk Net"))
	    {
	       final Ability equip = new Ability(card, "2")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	          {
	             return AllZone.getZone(card).is(Constant.Zone.Play) &&           
	                      AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	          }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Flying") || !c.getKeyword().contains("Reach"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -5830699867070741036L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addExtrinsicKeyword("Reach");
	                crd.addSemiPermanentDefenseBoost(2);
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = -4098923908462881875L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeExtrinsicKeyword("Reach");
	                crd.addSemiPermanentDefenseBoost(-2);
	                   
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {

				private static final long serialVersionUID = 5068745895084312024L;
		
				public void showMessage()
		        {
		               //get all creatures you control
		               CardList list = new CardList();
		               list.addAll(AllZone.Human_Play.getCards());
		               list = list.getType("Creature");
		              
		               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
		        }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 2");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************

	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("Whispersilk Cloak"))
	    {
	       final Ability equip = new Ability(card, "2")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	          {
	             return AllZone.getZone(card).is(Constant.Zone.Play) &&           
	                      AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	          }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -1829389094046225543L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addExtrinsicKeyword("Unblockable");
	                crd.addExtrinsicKeyword("Shroud");
	                
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = 110426811459225458L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeExtrinsicKeyword("Unblockable");
	                crd.removeExtrinsicKeyword("Shroud");
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
	         
			private static final long serialVersionUID = 2399248271613089612L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 2");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("Trusty Machete"))
	    {
	       final Ability equip = new Ability(card, "2")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	  		  {
	  			return AllZone.getZone(card).is(Constant.Zone.Play) &&            
	              	   AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	              	   (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	  		  }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -6434466688054628650L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addSemiPermanentAttackBoost(2);
	                crd.addSemiPermanentDefenseBoost(1);
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = -5297369538913528146L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addSemiPermanentAttackBoost(-2);
	                crd.addSemiPermanentDefenseBoost(-1);
	                   
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
			private static final long serialVersionUID = -1425693231661483469L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 2");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************
	    
	    //*************** START *********** START **************************
	    else if (cardName.equals("Umbral Mantle"))
	    {
	       final Ability equip = new Ability(card, "0")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }
	             
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	          {
	             return AllZone.getZone(card).is(Constant.Zone.Play) &&           
	                      AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                      !AllZone.Phase.getPhase().equals("End of Turn") &&
	                   !AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility);
	          }
	          

	          public boolean canPlayAI()
	            {
	              return false;
	            }
	       };//equip ability

	       equip.setType("Extrinsic");
	       
	       final Ability untapboost = new Ability (card, "3")
	      {
	          public void resolve(){
	            getSourceCard().addTempAttackBoost(2);
	            getSourceCard().addTempDefenseBoost(2);
	            getSourceCard().untap();
	         }
	         public boolean canPlay(){return (getSourceCard().isTapped() && !getSourceCard().hasSickness() && super.canPlay());}
	      };//equiped creature's ability
	      
	       Command onEquip = new Command()
	       {   
	         private static final long serialVersionUID = 1L;

	         public void execute()
	           {
	            if (card.isEquipping())
	             {
	               Card crd=card.getEquipping().get(0);
	               untapboost.setDescription("3, Untap:"+crd+" gets +2/+2 until end of turn");
	                untapboost.setStackDescription(crd+ " - +2/+2 until EOT");
	               
	               crd.addSpellAbility(untapboost);
	             }
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   
	         private static final long serialVersionUID = -3427116314295067303L;

	         public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeSpellAbility(untapboost);
	             }
	            
	           }//execute()
	       };//Command
	       
	       equip.setBeforePayMana(CardFactoryUtil.input_targetCreature(equip));
	       
	       equip.setDescription("Equip: 1");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);
	    } //*************** END ************ END **************************  
	    
	  //*************** START *********** START **************************
	    else if (cardName.equals("No-Dachi"))
	    {
	       final Ability equip = new Ability(card, "3")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	          {
	             return AllZone.getZone(card).is(Constant.Zone.Play) &&           
	                      AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	          }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -3581510347221716639L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.addExtrinsicKeyword("First Strike");
	                crd.addSemiPermanentAttackBoost(2);
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = 7782372477768948526L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                crd.removeExtrinsicKeyword("First Strike");
	                crd.addSemiPermanentAttackBoost(-2);
	                   
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
			private static final long serialVersionUID = 8252169208912917353L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 3");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************



	//*************** START *********** START **************************
	    else if (cardName.equals("Shuko"))
	    {
	       final Ability equip = new Ability(card, "0")
	       {
	          public void resolve()
	          {
	             if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
	             {
	                if (card.isEquipping())
	                {
	                   Card crd = card.getEquipping().get(0);
	                   if (crd.equals(getTargetCard()) )
	                      return;
	                   
	                   card.unEquipCard(crd);
	                }   
	                card.equipCard(getTargetCard());
	             }
	          }
	          
	          public boolean canPlay()
	          {
	             return AllZone.getZone(card).is(Constant.Zone.Play) &&           
	                      AllZone.Phase.getActivePlayer().equals(card.getController()) &&
	                      (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2") );
	          }
	          
	          public boolean canPlayAI()
	            {
	              return getCreature().size() != 0 && !card.isEquipping();
	            }
	         
	          
	          public void chooseTargetAI()
	            {
	              Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
	              setTargetCard(target);
	            }
	            CardList getCreature()
	            {
	              CardList list = new CardList(AllZone.Computer_Play.getCards());
	              list = list.filter(new CardListFilter()
	              {
	                public boolean addCard(Card c)
	                {
	                  return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
	                         (! c.getKeyword().contains("Defender"));
	                }
	              });
	              // list.remove(card);      // if mana-only cost, allow self-target
	              return list;
	            }//getCreature()
	          
	       };//equip ability
	       

	       Command onEquip = new Command()
	       {   

			private static final long serialVersionUID = -5615942134074972356L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                
	                crd.addSemiPermanentAttackBoost(1);
	             } 
	           }//execute()
	       };//Command
	      

	       Command onUnEquip = new Command()
	       {   

			private static final long serialVersionUID = 8169940790698709406L;

			public void execute()
	           {
	            if (card.isEquipping())
	             {
	                Card crd = card.getEquipping().get(0);
	                
	                crd.addSemiPermanentAttackBoost(-1);
	                   
	             }
	            
	           }//execute()
	       };//Command
	      
	       
	       Input runtime = new Input()
	       {
			private static final long serialVersionUID = -5319605106507450668L;

			public void showMessage()
	             {
	               //get all creatures you control
	               CardList list = new CardList();
	               list.addAll(AllZone.Human_Play.getCards());
	               list = list.getType("Creature");
	              
	               stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list, "Select target creature to equip", true));
	             }
	        };//Input
	      
	       equip.setBeforePayMana(runtime);
	       
	       equip.setDescription("Equip: 0");
	       card.addSpellAbility(equip);
	       
	       card.addEquipCommand(onEquip);
	       card.addUnEquipCommand(onUnEquip);

	    } //*************** END ************ END **************************

		
		return card;
	}
}