package forge;
class CardFactory_Auras {
	public static Card getCard(final Card card, String cardName, String owner)
	{
	
		//*************** START *********** START **************************
		if(cardName.equals("Epic Proportions"))
		{
		  final SpellAbility spell = new Spell(card)
		  {
			private static final long serialVersionUID = 358340213887424783L;
			
			public boolean canPlay()
			{
				return true; // for flash, which is not working through the keyword for some reason
			}
			
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      setTargetCard(list.get(0));
		      return true;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    
			private static final long serialVersionUID = 5133552158526053493L;

			public void execute()
		      {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(5);
						crd.addSemiPermanentDefenseBoost(5);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			    private static final long serialVersionUID = -2404250578944336031L;
			
				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(-5);					
						crd.addSemiPermanentDefenseBoost(-5);
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
			private static final long serialVersionUID = -6076263565995301138L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Mythic Proportions"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 4227124619270545652L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      setTargetCard(list.get(0));
		      return true;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    
			private static final long serialVersionUID = -6642467768059387172L;

			public void execute()
		      {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(8);
						crd.addSemiPermanentDefenseBoost(8);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			
			  	private static final long serialVersionUID = 3550678810412528973L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(-8);					
						crd.addSemiPermanentDefenseBoost(-8);
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
			private static final long serialVersionUID = -8590925715809196436L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Nimbus Wings"))
		{
		  final SpellAbility spell = new Spell(card)
		  {


			private static final long serialVersionUID = -4759884801420518565L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

		   /**
			 * 
			 */
			private static final long serialVersionUID = 4941909585318384005L;

		public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		      crd.addSemiPermanentAttackBoost(1);
		            crd.addSemiPermanentDefenseBoost(2);
		           
		      crd.addExtrinsicKeyword("Flying");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -728144711022713882L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		        crd.addSemiPermanentAttackBoost(-1);
		            crd.addSemiPermanentDefenseBoost(-2);
		           
		       crd.removeExtrinsicKeyword("Flying");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {


			private static final long serialVersionUID = -8808281961367126149L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************


		//*************** START *********** START **************************
		else if(cardName.equals("Hero's Resolve"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -4759884801420518565L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {
			  
			private static final long serialVersionUID = 4941909585318384005L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(1);
		            crd.addSemiPermanentDefenseBoost(5);
		           
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   


			private static final long serialVersionUID = -728144711022713882L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(-1);
		            crd.addSemiPermanentDefenseBoost(-5);
		           
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -8808281961367126149L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************


		//*************** START *********** START **************************
		else if(cardName.equals("Holy Strength"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 7142921886192227052L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = 29524607366962807L;

			public void execute()
		      {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(1);
						crd.addSemiPermanentDefenseBoost(2);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			
			private static final long serialVersionUID = -3236907619285510709L;

			public void execute()
			{
				if (card.isEnchanting())
				{
					Card crd = card.getEnchanting().get(0);
					crd.addSemiPermanentAttackBoost(-1);					
					crd.addSemiPermanentDefenseBoost(-2);
				}
		    
			}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -4543302260602460839L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Unholy Strength"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 249022827643646119L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = 3550678810412528973L;

			public void execute()
		      {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(2);
						crd.addSemiPermanentDefenseBoost(1);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			
			private static final long serialVersionUID = -8590925715809196436L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(-2);					
						crd.addSemiPermanentDefenseBoost(-1);
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = 3368827667218463197L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Weakness"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 3959966663907905001L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = -2365466450520529652L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(-2);
						crd.addSemiPermanentDefenseBoost(-1);
					}  
		    }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    

			  	private static final long serialVersionUID = 8144460293841806556L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(2);					
						crd.addSemiPermanentDefenseBoost(1);
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			   private static final long serialVersionUID = -8235558710156197207L;

			   public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Guilty Conscience"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 1169151960692309514L;
			public boolean canPlayAI()
		    {
				
		      CardList stuffy = new CardList(AllZone.Computer_Play.getCards());
		      stuffy = stuffy.getName("Stuffy Doll");
		      
		      if (stuffy.size() > 0)
		      {
		    	  setTargetCard(stuffy.get(0));
		    	  return true;
		      }
		      else 
		      {
			      CardList list = new CardList(AllZone.Human_Play.getCards());
			      list = list.getType("Creature");
			
			      if(list.isEmpty())
			    	  return false;
			
			      //else
			      CardListUtil.sortAttack(list);
			      CardListUtil.sortFlying(list);
			
			      for (int i=0;i<list.size();i++) {
				      if (CardFactoryUtil.canTarget(card, list.get(i)) && (list.get(i).getNetAttack() >= list.get(i).getNetDefense()) && list.get(i).getNetAttack() >= 3)
				      {
					      setTargetCard(list.get(i));
					      return true;
				      }
			      }
		      }
		      return false;
		      
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		    	  card.enchantCard(c);
		      
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Cessation"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 3681531440398159146L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)) && !list.get(i).getKeyword().contains("Defender"))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = -5544484800605477434L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("This creature can't attack");
					}  
		    }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			  	private static final long serialVersionUID = 3621591534173743090L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("This creature can't attack");
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -6043933114268403555L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Pacifism") || cardName.equals("Bound in Silence"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -1357026258424339999L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)) && !list.get(i).getKeyword().contains("Defender"))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = -897162953903978929L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("This creature can't attack or block");
					}  
		    }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			    private static final long serialVersionUID = 3461412526408858199L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("This creature can't attack or block");
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -4922257746317147308L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Pillory of the Sleepless"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 4504925036782582195L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)) && !list.get(i).getKeyword().contains("Defender"))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		    	  card.enchantCard(c);
		    	  //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = -6104532173397759007L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("This creature can't attack or block");
					}  
		    }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    

			private static final long serialVersionUID = -2563098134722661731L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("This creature can't attack or block");
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -1621250313053538491L;

			public void execute()
			   {
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Brilliant Halo"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 3959966663907905001L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		    	  card.enchantCard(c);

		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = -2365466450520529652L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(1);
						crd.addSemiPermanentDefenseBoost(2);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			

			  	private static final long serialVersionUID = 8144460293841806556L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(-1);					
						crd.addSemiPermanentDefenseBoost(-2);
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -8235558710156197207L;

			public void execute()
			{
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			 }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Cloak of Mists"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 1349252919350703923L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

		   
			private static final long serialVersionUID = -5477386150275605685L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Unblockable");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   


			private static final long serialVersionUID = -1458403756319532488L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Unblockable");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = 2970703965180562317L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Indomitable Will"))
		{
		  final SpellAbility spell = new Spell(card)
		  {
		      
		  
			private static final long serialVersionUID = -8730555134087764706L;
			public boolean canPlay()
		    {
		      return true; // for flash, which is not working through the keyword for some reason
		    }
		   
		   public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   
		   	private static final long serialVersionUID = -9017595501743099736L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(1);
		            crd.addSemiPermanentDefenseBoost(2);
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -301746096442890239L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            
		            crd.addSemiPermanentAttackBoost(-1);               
		            crd.addSemiPermanentDefenseBoost(-2);
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   
			private static final long serialVersionUID = 4866639414492912349L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Uncontrollable Anger"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -817670112637191266L;
			
			//for flash, keyword somehow doesn't work
			public boolean canPlay()
			{
				return true;
			}
			
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 3621591534173743090L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(2);
		            crd.addSemiPermanentDefenseBoost(2);
		       crd.addExtrinsicKeyword("This card attacks each turn if able.");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -6043933114268403555L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(-2);               
		            crd.addSemiPermanentDefenseBoost(-2);
		      crd.removeExtrinsicKeyword("This card attacks each turn if able.");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -1854544543762078840L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Fear"))
		{
		  final SpellAbility spell = new Spell(card)
		  {
			private static final long serialVersionUID = 1538560397393051959L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 3461412526408858199L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Fear");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -4922257746317147308L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Fear");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
			private static final long serialVersionUID = 8802824404322172485L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Battle Mastery"))
		{
		  final SpellAbility spell = new Spell(card)
		  {


			private static final long serialVersionUID = 1538560397393051959L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 3461412526408858199L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Double Strike");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -4922257746317147308L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Double Strike");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = 8802824404322172485L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Protective Bubble"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -5744948616351896881L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = -7504975809719164916L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Shroud");
		            crd.addExtrinsicKeyword("Unblockable");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -5732248224429180503L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Shroud");
		            crd.removeExtrinsicKeyword("Unblockable");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -5677600048500997627L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Zephid's Embrace"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -375053523089273410L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 7818182838059001941L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(2);
		            crd.addSemiPermanentDefenseBoost(2);
		            crd.addExtrinsicKeyword("Shroud");
		            crd.addExtrinsicKeyword("Flying");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   


			private static final long serialVersionUID = 8038970743056290484L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(-2);
		            crd.addSemiPermanentDefenseBoost(-2);
		            crd.removeExtrinsicKeyword("Shroud");
		            crd.removeExtrinsicKeyword("Flying");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
			private static final long serialVersionUID = 104098395022764345L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Goblin War Paint"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -959968424187950430L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 9074991509563203771L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(2);
		            crd.addSemiPermanentDefenseBoost(2);
		           
		            crd.addExtrinsicKeyword("Haste");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = 720578490253659248L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		        crd.addSemiPermanentAttackBoost(-2);
		            crd.addSemiPermanentDefenseBoost(-2);
		           
		       crd.removeExtrinsicKeyword("Haste");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -8089111708519929350L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Paralyzing Grasp"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -2685360795445503449L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = -380913483412563006L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		         crd.addExtrinsicKeyword("This card does not untap during your untap phase");
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = 4534224467226579803L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		                   
		            crd.removeExtrinsicKeyword("This card does not untap during your untap phase");
		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -2513967225177113996L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************




		//*************** START *********** START **************************
		else if(cardName.equals("Undying Rage"))
		{
			final SpellAbility spell = new Spell(card)
			{
			
				private static final long serialVersionUID = 7688777028599839669L;
				public boolean canPlayAI()
				{
					CardList list = new CardList(AllZone.Computer_Play.getCards());
					list = list.getType("Creature");
					
					if(list.isEmpty())
					return false;
					
					//else
					CardListUtil.sortAttack(list);
					CardListUtil.sortFlying(list);
					
					for (int i=0;i<list.size();i++) {
					      if (CardFactoryUtil.canTarget(card, list.get(i)))
					      {
						      setTargetCard(list.get(i));
						      return true;
					      }
				      }
				      return false;		
				 }//canPlayAI()
				public void resolve()
				{
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
					play.add(card);
					
					Card c = getTargetCard();
					
					if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
					card.enchantCard(c);
					
				}//resolve()
			};//SpellAbility
			card.clearSpellAbility();
			card.addSpellAbility(spell);
			
			Command onEnchant = new Command()
			{

				private static final long serialVersionUID = 5090552789458764964L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addSemiPermanentAttackBoost(+2);
						crd.addSemiPermanentDefenseBoost(+2);
						crd.addExtrinsicKeyword("This creature cannot block");
					}
				}//execute()
			};//Command
			
			
			Command onUnEnchant = new Command()
			{

				private static final long serialVersionUID = -82584999448000826L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("This creature cannot block");
						crd.addSemiPermanentAttackBoost(-2);
						crd.addSemiPermanentDefenseBoost(-2);
					}
					
				}//execute()
			};//Command
			
			Command onLeavesPlay = new Command()
			{
			
				private static final long serialVersionUID = -4577373116316893192L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						card.unEnchantCard(crd);
					}
				}
			};
			
			card.addEnchantCommand(onEnchant);
			card.addUnEnchantCommand(onUnEnchant);
			card.addLeavesPlayCommand(onLeavesPlay);
			
			spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************



		//*************** START *********** START **************************
		else if(cardName.equals("Rancor"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 7142921886192227052L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		      
		      Card c = getTargetCard();
		      
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		    	  card.enchantCard(c);

		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		  
		  Command onEnchant = new Command()
		  {    

			private static final long serialVersionUID = 29524607366962807L;

			public void execute()
		    {
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.addExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(2);
					}  
		      }//execute()
		  };//Command
		  

		  Command onUnEnchant = new Command()
		  {    
			
			private static final long serialVersionUID = -3236907619285510709L;

				public void execute()
				{
					if (card.isEnchanting())
					{
						Card crd = card.getEnchanting().get(0);
						crd.removeExtrinsicKeyword("Trample");
						crd.addSemiPermanentAttackBoost(-2);					
					}
			    
				}//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -4543302260602460839L;

			public void execute()
			{
				   if (card.isEnchanting())
				   {
					   Card crd = card.getEnchanting().get(0);
					   card.unEnchantCard(crd);
				   }
			   }
		   };
		  
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		  
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Armadillo Cloak"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 7414947327367193959L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = -8808281961367126149L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Trample");
		            crd.addExtrinsicKeyword("Lifelink");
		            crd.addSemiPermanentAttackBoost(2);
		            crd.addSemiPermanentDefenseBoost(2);
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
		   
			private static final long serialVersionUID = -3634342116735279715L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Trample");

		            crd.removeExtrinsicKeyword("Lifelink");
		   
		            crd.addSemiPermanentAttackBoost(-2);               
		            crd.addSemiPermanentDefenseBoost(-2);
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   

			private static final long serialVersionUID = 648546709124047998L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Serra's Embrace"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 4710595790920367640L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 8209539652143050311L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Flying");
		            crd.addExtrinsicKeyword("Vigilance");
		            crd.addSemiPermanentAttackBoost(2);
		            crd.addSemiPermanentDefenseBoost(2);
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
			private static final long serialVersionUID = 8569667869488235349L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Flying");
		            crd.removeExtrinsicKeyword("Vigilance");
		            crd.addSemiPermanentAttackBoost(-2);               
		            crd.addSemiPermanentDefenseBoost(-2);
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -515072286191538396L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Wings of Hope"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -8219002552125665610L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 615799578787695739L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Flying");
		            crd.addSemiPermanentAttackBoost(1);
		            crd.addSemiPermanentDefenseBoost(3);
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
		   
			private static final long serialVersionUID = -3945180659632792182L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Flying");
		            crd.addSemiPermanentAttackBoost(-1);               
		            crd.addSemiPermanentDefenseBoost(-3);
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   
			private static final long serialVersionUID = -5544484800605477434L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("AEther Web"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -5744948616351896881L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = -7504975809719164916L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Reach");
		            crd.addExtrinsicKeyword("This creature can block creatures with shadow as though they didn't have shadow.");
		            crd.addSemiPermanentAttackBoost(1);
		            crd.addSemiPermanentDefenseBoost(1);
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -5732248224429180503L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Reach");
		            crd.removeExtrinsicKeyword("This creature can block creatures with shadow as though they didn't have shadow.");
		            crd.addSemiPermanentAttackBoost(-1);               
		            crd.addSemiPermanentDefenseBoost(-1);
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -5677600048500997627L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************


		//*************** START *********** START **************************
		else if(cardName.equals("Launch") || cardName.equals("Flight"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 4710595790920367640L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 8209539652143050311L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Flying");
		                     } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = 8569667869488235349L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Flying");

		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -515072286191538396L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Aspect of Mongoose") || cardName.equals("Robe of Mirrors") || cardName.equals("Diplomatic Immunity"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -8219002552125665610L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   
			private static final long serialVersionUID = 615799578787695739L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Shroud");
		                     } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
		   
			private static final long serialVersionUID = -3945180659632792182L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Shroud");

		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   
			private static final long serialVersionUID = -5544484800605477434L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Sleeper's Guile"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -8219002552125665610L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 615799578787695739L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("Fear");
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
		   
			private static final long serialVersionUID = -3945180659632792182L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("Fear");

		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   
			private static final long serialVersionUID = -5544484800605477434L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		//*************** START *********** START **************************
		else if(cardName.equals("Sluggishness"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 5184457180419402397L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
			      if (CardFactoryUtil.canTarget(card, list.get(i)))
			      {
				      setTargetCard(list.get(i));
				      return true;
			      }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   


			private static final long serialVersionUID = -6043933114268403555L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addExtrinsicKeyword("This creature can't block");
		         } 
		      }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   
		   
			private static final long serialVersionUID = -1854544543762078840L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.removeExtrinsicKeyword("This creature can't block");

		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {
		   
			private static final long serialVersionUID = -897162953903978929L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************
		
		 //*************** START *********** START **************************
	    else if(cardName.equals("Control Magic"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
			private static final long serialVersionUID = 7359753772138233155L;
			public boolean canPlayAI()
	        {
	          CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
	          CardListUtil.sortAttack(c);
	          CardListUtil.sortFlying(c);

	          if(c.isEmpty())
	            return false;

	          if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying"))
	          {
	            setTargetCard(c.get(0));
	            return true;
	          }

	          CardListUtil.sortAttack(c);
	          if(4 <= c.get(0).getNetAttack())
	          {
	            setTargetCard(c.get(0));
	            return true;
	          }

	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	        	
	        	
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	            
	          Card c = getTargetCard();
	            
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	        	  card.enchantCard(c);

	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	      
	      Command onEnchant = new Command()
	      {    

			private static final long serialVersionUID = -3423649303706656587L;

			public void execute()
	        {
	    			if (card.isEnchanting())
	    			{
	    				Card crd = card.getEnchanting().get(0);
	    				//set summoning sickness
	    	            if(crd.getKeyword().contains("Haste")){
	    	              crd.setSickness(false);
	    	            }
	    	            else{
	    	              crd.setSickness(true);
	    	            }
	    	            
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

	    	            PlayerZone from = AllZone.getZone(crd);
	    	            from.remove(crd);
	    	           
	    	            crd.setController(card.getController());

	    	            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
	    	            to.add(crd);

	    	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	    	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	    			}  
	          }//execute()
	      };//Command
	      
	      Command onUnEnchant = new Command()
	      {    

			private static final long serialVersionUID = -2365466450520529652L;

				public void execute()
	    		{
	    			if (card.isEnchanting())
	    			{
	    				Card crd = card.getEnchanting().get(0);
	    				if (AllZone.GameAction.isCardInPlay(crd))
	    				{
		    				if(crd.getKeyword().contains("Haste")){
		      	              crd.setSickness(false);
		      	            }
		      	            else{
		      	              crd.setSickness(true);
		      	            }
		      	            
		      	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		      	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
		
		      	            PlayerZone from = AllZone.getZone(crd);
		      	            from.remove(crd);
		      	           
		      	            String opp = AllZone.GameAction.getOpponent(crd.getController());
		      	            crd.setController(opp);
		
		      	            PlayerZone to = AllZone.getZone(Constant.Zone.Play, opp);
		      	            to.add(crd);
		
		      	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
		      	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
	    				}
	    			}
	    	    
	    		}//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {


			private static final long serialVersionUID = 8144460293841806556L;

			public void execute()
	    	{
	    		   if (card.isEnchanting())
	    		   {
	    			   Card crd = card.getEnchanting().get(0);
	    			   card.unEnchantCard(crd);
	    		   }
	    	   }
	       };
	      
	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);
	      

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************


	    //Enchant Lands:
	    
	    //*************** START *********** START **************************
	    else if(cardName.equals("Caribou Range"))
	    {
	      
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 5394181222737344498L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Land");

	          if(list.isEmpty())
	           return false;

	          setTargetCard(list.get(0));
	          return true;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	          
	          Card c = getTargetCard();
	          
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	        	  card.enchantCard(c);

	        }//resolve()
	      };//SpellAbility
	      spell.setType("Extrinsic");
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	      
	      final SpellAbility produceCaribou = new Ability_Tap(spell.getTargetCard(), "W W")
	      {

			private static final long serialVersionUID = 1358032097310954750L;
			public void resolve()
	        {	
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();
	          Card crd = spell.getTargetCard();
	          
	          c.setName("Caribou");
	          c.setImageName("W 0 1 Caribou");

	          c.setOwner(crd.getController());
	          c.setController(crd.getController());

	          c.setManaCost("W");
	          c.setToken(true);             
	          
	          c.addType("Creature");
	          c.addType("Caribou");
	          c.setBaseAttack(0);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      
	      produceCaribou.setDescription("W W, Tap: Put a 0/1 white Caribou creature token onto the battlefield.");
	      produceCaribou.setStackDescription("Put a 0/1 white Caribou creature token onto the battlefield.");
	      
	      Command onEnchant = new Command()
	      {    

			private static final long serialVersionUID = -4394447413814077965L;

			public void execute()
	        {
	    			if (card.isEnchanting())
	    			{
	    				Card crd = card.getEnchanting().get(0);
	    				//crd.clearSpellAbility();
	    				crd.addSpellAbility(produceCaribou);
	    				
	    			}  
	          }//execute()
	      };//Command
	      

	      Command onUnEnchant = new Command()
	      {    

				private static final long serialVersionUID = -1492886212745680573L;
		
				public void execute()
				{
					if (card.isEnchanting())
					{
						
						Card crd = card.getEnchanting().get(0);
						crd.removeSpellAbility(produceCaribou);
					}
				}//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 8067313131085909766L;

			public void execute()
	    	{
	    		   if (card.isEnchanting())
	    		   {
	    			   Card crd = card.getEnchanting().get(0);
	    			   card.unEnchantCard(crd);
	    		   }
	    	   }
	       };
	       
	       final SpellAbility a2 = new Ability(card, "0")
		   {
	 	    	public void chooseTargetAI()
	 	    	{
	 	    		PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		        	CardList saps = new CardList(play.getCards());
		        	saps = saps.filter(new CardListFilter()
		        	{

						public boolean addCard(Card c) {
							if((c.getType().contains("Caribou") || c.getKeyword().contains("Changeling")) &&
								AllZone.GameAction.isCardInPlay(c) && c.isToken() )
								return true;
							return false;
						}
		        	
		        	});
		        	
		        	if (saps.size() != 0)
		        		setTargetCard(saps.getCard(0));
	 	    	}
		        public void resolve()
		        {
		          //get all saprolings:
		          Card c = getTargetCard();
		          if(c == null )
		        	  return;
		          
		          if (!AllZone.GameAction.isCardInPlay(c))
		        	  return;
		          
		          if(AllZone.GameAction.isCardInPlay(c))
		          {
		            //AllZone.getZone(c).remove(c);
		        	AllZone.GameAction.sacrifice(c);
		        	  
		        	PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
		        	life.addLife(1);
		          }
		        }//resolve
		        public boolean canPlayAI()
		        {
		        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		        	CardList cars = new CardList(play.getCards());
		        	cars = cars.filter(new CardListFilter()
		        	{

						public boolean addCard(Card c) {
							if(c.getType().contains("Caribou") || c.getKeyword().contains("Changeling") &&
								AllZone.GameAction.isCardInPlay(c))
								return true;
							return false;
						}
		        	
		        	});
		        	if(AllZone.Computer_Life.getLife() < 4 && cars.size() > 0)
		        		return true;
		        	else
		        		return false;
		        }
		      };//SpellAbility
		      
		      Input runtime = new Input()
		      {

				private static final long serialVersionUID = 1408675899387720506L;

				public void showMessage()
		        {
		          CardList cars = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
		          cars = cars.getType("Caribou");
		          
		          stopSetNext(CardFactoryUtil.input_targetSpecific(a2, cars, "Select a Caribou to sacrifice.", false));
		        }
		      };  
		      
		  card.addSpellAbility(a2);
		  a2.setDescription("Sacrifice a Caribou: You gain 1 life.");
		  a2.setStackDescription(card.getController() + " gains 1 life.");
		  a2.setBeforePayMana(runtime);
	       
	      
	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);
	      
	      Input runtime2 = new Input()
	      {

			private static final long serialVersionUID = -6674543815905055287L;

			public void showMessage()
	        {
	    	  PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	          CardList land = new CardList();
	          land.addAll(hum.getCards());
	          land = land.filter(new CardListFilter()
	          {
	              public boolean addCard(Card c)
	              {
	            	  return c.isLand();
	              }
	          });
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land you control", true));
	        }
	      };
	      spell.setBeforePayMana(runtime2);
	    }//*************** END ************ END **************************  
	    
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Leafdrake Roost"))
	    {
	      
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -4695012002471107694L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Land");

	          if(list.isEmpty())
	           return false;

	          setTargetCard(list.get(0));
	          return true;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	          
	          Card c = getTargetCard();
	          
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	        	  card.enchantCard(c);

	        }//resolve()
	      };//SpellAbility
	      spell.setType("Extrinsic");
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	      
	      final SpellAbility produceDrakes = new Ability_Tap(spell.getTargetCard(), "G U")
	      {

			private static final long serialVersionUID = -3849765771560556442L;
			public void resolve()
	        {	
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();
	          Card crd = spell.getTargetCard();
	          
	          c.setName("Drake");
	          c.setImageName("G U 2 2 Drake");

	          c.setOwner(crd.getController());
	          c.setController(crd.getController());

	          c.setManaCost("G U");
	          c.setToken(true);             
	          
	          c.addType("Creature");
	          c.addType("Drake");
	          c.setBaseAttack(2);
	          c.setBaseDefense(2);
	          c.addIntrinsicKeyword("Flying");

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      
	      produceDrakes.setDescription("G U, Tap: Put a 2/2 green and blue Drake creature token with flying onto the battlefield.");
	      produceDrakes.setStackDescription("Put a 2/2 green and blue Drake creature token with flying onto the battlefield.");
	      
	      Command onEnchant = new Command()
	      {    

			private static final long serialVersionUID = -5501311059855861341L;

			public void execute()
	        {
	    			if (card.isEnchanting())
	    			{
	    				Card crd = card.getEnchanting().get(0);
	    				//crd.clearSpellAbility();
	    				crd.addSpellAbility(produceDrakes);
	    				
	    			}  
	          }//execute()
	      };//Command
	      

	      Command onUnEnchant = new Command()
	      {    

	    	  private static final long serialVersionUID = 3589766088284055294L;

				public void execute()
	    		{
	    			if (card.isEnchanting())
	    			{
	    				
	    				Card crd = card.getEnchanting().get(0);
	    				crd.removeSpellAbility(produceDrakes);
	    				
	    			}
	    	    
	    		}//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = -3747590484749776557L;

			public void execute()
	    	{
	    		   if (card.isEnchanting())
	    		   {
	    			   Card crd = card.getEnchanting().get(0);
	    			   card.unEnchantCard(crd);
	    		   }
	    	   }
	       };
	      
	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);
	      
	      Input runtime = new Input()
	      {

	    	private static final long serialVersionUID = 967525396666242309L;

	    	public void showMessage()
	        {
	    	  PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	    	  PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	          CardList land = new CardList();
	          land.addAll(comp.getCards());
	          land.addAll(hum.getCards());
	          land = land.filter(new CardListFilter()
	          {
	              public boolean addCard(Card c)
	              {
	            	  return c.isLand();
	              }
	          });
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true));
	        }
	      };
	      spell.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Squirrel Nest"))
	    {
	      
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 6115713202262504968L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Land");

	          if(list.isEmpty())
	           return false;

	          setTargetCard(list.get(0));
	          return true;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	          
	          Card c = getTargetCard();
	          
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	        	  card.enchantCard(c);

	        }//resolve()
	      };//SpellAbility
	      spell.setType("Extrinsic");
	      
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	      
	      final SpellAbility produceSquirrels = new Ability_Tap(spell.getTargetCard())
	      {
	    	private static final long serialVersionUID = -4800170026789001271L;
	    	public void resolve()
	        {	
	          makeToken();
	        }
	        void makeToken()
	        {
	          Card c = new Card();
	          Card crd = spell.getTargetCard();
	                    
	          c.setName("Squirrel");
	          c.setImageName("G 1 1 Squirrel");

	          c.setOwner(crd.getController());
	          c.setController(crd.getController());

	          c.setManaCost("G");
	          c.setToken(true);             
	          
	          c.addType("Creature");
	          c.addType("Squirrel");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);

	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(c);
	        }//makeToken()
	      };//SpellAbility
	      
	      produceSquirrels.setDescription("Tap: Put a 1/1 green Squirrel creature token into play.");
	      produceSquirrels.setStackDescription("Put a 1/1 green Squirrel creature token into play.");
	      
	      Command onEnchant = new Command()
	      {    

	    	
	    	private static final long serialVersionUID = 3528675502863241126L;

	    	public void execute()
	        {
	    			if (card.isEnchanting())
	    			{
	    				Card crd = card.getEnchanting().get(0);
	    				//crd.clearSpellAbility();
	    				crd.addSpellAbility(produceSquirrels);
	    				
	    			}  
	          }//execute()
	      };//Command
	      

	      Command onUnEnchant = new Command()
	      {    
	    	    private static final long serialVersionUID = -2021446345291180334L;

	    		public void execute()
	    		{
	    			if (card.isEnchanting())
	    			{
	    				
	    				Card crd = card.getEnchanting().get(0);
	    				crd.removeSpellAbility(produceSquirrels);
	    				
	    			}
	    	    
	    		}//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

	    	private static final long serialVersionUID = -4543302260602460839L;

	    	public void execute()
	    	{
	    		   if (card.isEnchanting())
	    		   {
	    			   Card crd = card.getEnchanting().get(0);
	    			   card.unEnchantCard(crd);
	    		   }
	    	   }
	       };
	      
	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);
	      
	      Input runtime = new Input()
	      {

	    	private static final long serialVersionUID = 967525396666242309L;

	    	public void showMessage()
	        {
	    	  PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	    	  PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	          CardList land = new CardList();
	          land.addAll(comp.getCards());
	          land.addAll(hum.getCards());
	          land = land.filter(new CardListFilter()
	          {
	              public boolean addCard(Card c)
	              {
	            	  return c.isLand();
	              }
	          });
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true));
	        }
	      };
	      spell.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************		
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Cursed Land"))
	    {
	      
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 5394181222737344498L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Human_Play.getCards());
	          list = list.getType("Land");

	          if(list.isEmpty())
	           return false;

	          setTargetCard(list.get(0));
	          return true;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	          
	          Card c = getTargetCard();
	          
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	        	  card.enchantCard(c);

	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	      
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 1395122135234314967L;

			public void execute()
	    	{
	    		   if (card.isEnchanting())
	    		   {
	    			   Card crd = card.getEnchanting().get(0);
	    			   card.unEnchantCard(crd);
	    		   }
	    	   }
	       };
	      card.addLeavesPlayCommand(onLeavesPlay);
	      
	      Input runtime = new Input()
	      {

			private static final long serialVersionUID = -6237279587146079880L;

			public void showMessage()
	        {
	    	  PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	    	  PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	          CardList land = new CardList();
	          land.addAll(comp.getCards());
	          land.addAll(hum.getCards());
	          land = land.filter(new CardListFilter()
	          {
	              public boolean addCard(Card c)
	              {
	            	  return c.isLand();
	              }
	          });
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true));
	        }
	      };
	      spell.setBeforePayMana(runtime);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Reflexes"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -3805000291281670685L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   
			private static final long serialVersionUID = -5165641997063553049L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addExtrinsicKeyword("First Strike");
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -4110050458709776949L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.removeExtrinsicKeyword("First Strike");
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {


			private static final long serialVersionUID = -1698042819463252165L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Shield of Duty and Reason"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 2716434587294106164L;
			public boolean canPlayAI()
			{
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -4915608896430636011L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addExtrinsicKeyword("Protection from green");
	          crd.addExtrinsicKeyword("Protection from blue");
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -1816533053473285131L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.removeExtrinsicKeyword("Protection from green");
	          crd.removeExtrinsicKeyword("Protection from blue");
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 8310935051473082927L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************

	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Scavenged Weaponry"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -3173197801263912271L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -5829857523192736013L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	          crd.addSemiPermanentAttackBoost(1);
	               crd.addSemiPermanentDefenseBoost(1);
	               
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			  private static final long serialVersionUID = -6257002385098720928L;

			  public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	            crd.addSemiPermanentAttackBoost(-1);
	               crd.addSemiPermanentDefenseBoost(-1);
	               
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {
	    	   private static final long serialVersionUID = 4748755323331184782L;

	    	   public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Scavenged Weaponry"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -7363807413296118236L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -5865085954495627875L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	          crd.addSemiPermanentAttackBoost(1);
	               crd.addSemiPermanentDefenseBoost(1);
	               
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -3194216573725644954L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	            crd.addSemiPermanentAttackBoost(-1);
	               crd.addSemiPermanentDefenseBoost(-1);
	               
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 785437865495532613L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************

	  //*************** START *********** START **************************
	    else if(cardName.equals("Maniacal Rage"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -9202976073213396613L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -1545171097775488566L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(2);
	                crd.addSemiPermanentDefenseBoost(2);
	                crd.addExtrinsicKeyword("This creature cannot block");
	               
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = 4583526261231627981L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(-2);
	                crd.addSemiPermanentDefenseBoost(-2);
	                crd.removeExtrinsicKeyword("This creature cannot block");          

	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 3537725814278679099L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Magefire Wings"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -6751823877894870506L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = 1193197133727867564L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	          crd.addSemiPermanentAttackBoost(2);
	          crd.addExtrinsicKeyword("Flying");
	               
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -7574396808454297325L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	            crd.addSemiPermanentAttackBoost(-2);
	          crd.removeExtrinsicKeyword("Flying");          

	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {
			private static final long serialVersionUID = 7822454560546809225L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Vigilance"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 3659751920022901998L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -2060758415927004190L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	           crd.addExtrinsicKeyword("Vigilance");
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -5220074511756932255L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	               
	          crd.removeExtrinsicKeyword("Vigilance");
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = -549155960320946886L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Mageta's Boon"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -5675308042827100871L;
		//for flash, keyword somehow doesn't work
	       public boolean canPlay()
	       {
	          return true;
	       }
	       
	       public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -3673424989463560506L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(1);
	                crd.addSemiPermanentDefenseBoost(2);
	           
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = 8554950830729979925L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(-1);               
	                crd.addSemiPermanentDefenseBoost(-2);
	         
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {


			private static final long serialVersionUID = 1217551126264991261L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Tiger Claws"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

		   private static final long serialVersionUID = 5858467189563974257L;
		   //for flash, keyword somehow doesn't work
	       public boolean canPlay()
	       {
	          return true;
	       }
	       
	       public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -8581037450169915700L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(1);
	                crd.addSemiPermanentDefenseBoost(1);
	           crd.addExtrinsicKeyword("Trample");
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

	       

			private static final long serialVersionUID = 403947772085423264L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addSemiPermanentAttackBoost(-1);               
	                crd.addSemiPermanentDefenseBoost(-1);
	          crd.removeExtrinsicKeyword("Trample");
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = 7781833007245249369L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************



	    //*************** START *********** START **************************
	    else if(cardName.equals("Feast of the Unicorn"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = -3236961825069248917L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -933565270738037125L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	          crd.addSemiPermanentAttackBoost(4);
	                
	               
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -8104738799489609077L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	            crd.addSemiPermanentAttackBoost(-4);
	              
	               
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = -2089422362133536230L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Buoyancy"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {


			private static final long serialVersionUID = 142389375702113977L;
			//for flash, keyword somehow doesn't work
	       public boolean canPlay()
	       {
	          return true;
	       }
	       
	       public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   

			private static final long serialVersionUID = -5302506578307993978L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.addExtrinsicKeyword("Flying");
	               
	           
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -6908757692588823391L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                crd.removeExtrinsicKeyword("Flying");               
	               
	         
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = -7833240882415702940L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************


	    //*************** START *********** START **************************
	    else if(cardName.equals("Mask of Law and Grace"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {

			private static final long serialVersionUID = 1020985216772332281L;
			public boolean canPlayAI()
	        {
	          CardList list = new CardList(AllZone.Computer_Play.getCards());
	          list = list.getType("Creature");

	          if(list.isEmpty())
	           return false;

	          //else
	          CardListUtil.sortAttack(list);
	          CardListUtil.sortFlying(list);

	          for (int i=0;i<list.size();i++) {
	             if (CardFactoryUtil.canTarget(card, list.get(i)))
	             {
	                setTargetCard(list.get(i));
	                return true;
	             }
	          }
	          return false;
	        }//canPlayAI()
	        public void resolve()
	        {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          play.add(card);
	         
	          Card c = getTargetCard();
	         
	          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
	          {
	             card.enchantCard(c);
	             System.out.println("Enchanted: " +getTargetCard());
	          }
	        }//resolve()
	      };//SpellAbility
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      Command onEnchant = new Command()
	      {   


			private static final long serialVersionUID = 6690012730339118087L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	           crd.addExtrinsicKeyword("Protection from black");
	          crd.addExtrinsicKeyword("Protection from red");
	             }
	          }//execute()
	      };//Command


	      Command onUnEnchant = new Command()
	      {   

			private static final long serialVersionUID = -7610577137374784417L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	               
	          crd.removeExtrinsicKeyword("Protection from black");
	          crd.removeExtrinsicKeyword("Protection from red");
	             }
	         
	          }//execute()
	       };//Command
	       
	       Command onLeavesPlay = new Command()
	       {

			private static final long serialVersionUID = -7150108398313031735L;

			public void execute()
	          {
	             if (card.isEnchanting())
	             {
	                Card crd = card.getEnchanting().get(0);
	                card.unEnchantCard(crd);
	             }
	          }
	       };

	      card.addEnchantCommand(onEnchant);
	      card.addUnEnchantCommand(onUnEnchant);
	      card.addLeavesPlayCommand(onLeavesPlay);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	    }//*************** END ************ END **************************
		
		//*************** START *********** START **************************
	    else if(cardName.equals("Lightning Talons"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = -5377796694870681717L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Computer_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);

		  Command onEnchant = new Command()
		  {   

			private static final long serialVersionUID = 4365727560058046700L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		      crd.addSemiPermanentAttackBoost(3);
		      crd.addExtrinsicKeyword("First Strike");
		           
		         }
		      }//execute()
		  };//Command


		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = 6407641511899731357L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		        crd.addSemiPermanentAttackBoost(-3);
		      crd.removeExtrinsicKeyword("First Strike");          

		         }
		     
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = -1674039264513052930L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };

		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);

		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************
		
		//*************** START *********** START **************************
		if(cardName.equals("Despondency"))
		{
		  final SpellAbility spell = new Spell(card)
		  {

			private static final long serialVersionUID = 1125616183900458458L;
			public boolean canPlayAI()
		    {
		      CardList list = new CardList(AllZone.Human_Play.getCards());
		      list = list.getType("Creature");

		      if(list.isEmpty())
		       return false;

		      //else
		      CardListUtil.sortAttack(list);
		      CardListUtil.sortFlying(list);

		      for (int i=0;i<list.size();i++) {
		         if (CardFactoryUtil.canTarget(card, list.get(i)))
		         {
		            setTargetCard(list.get(i));
		            return true;
		         }
		      }
		      return false;
		    }//canPlayAI()
		    public void resolve()
		    {
		      PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
		      play.add(card);
		     
		      Card c = getTargetCard();
		     
		      if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
		      {
		         card.enchantCard(c);
		         //System.out.println("Enchanted: " +getTargetCard());
		      }
		    }//resolve()
		  };//SpellAbility
		  card.clearSpellAbility();
		  card.addSpellAbility(spell);
		 
		  Command onEnchant = new Command()
		  {   
			private static final long serialVersionUID = -8589566780713349434L;

			public void execute()
		    {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(-2);
		            
		         } 
		    }//execute()
		  };//Command
		 

		  Command onUnEnchant = new Command()
		  {   

			private static final long serialVersionUID = -5769889616562358735L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            crd.addSemiPermanentAttackBoost(2);               
		            
		         }
		      
		      }//execute()
		   };//Command
		   
		   Command onLeavesPlay = new Command()
		   {

			private static final long serialVersionUID = 9095725091375284510L;

			public void execute()
		      {
		         if (card.isEnchanting())
		         {
		            Card crd = card.getEnchanting().get(0);
		            card.unEnchantCard(crd);
		         }
		      }
		   };
		 
		  card.addEnchantCommand(onEnchant);
		  card.addUnEnchantCommand(onUnEnchant);
		  card.addLeavesPlayCommand(onLeavesPlay);
		 
		  spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
		}//*************** END ************ END **************************

		
	    return card;
	}
}