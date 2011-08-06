
package forge;


import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.*;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCostUtil;
import forge.gui.input.Input_PayManaCost_Ability;


public class GameActionUtil {
	public static void executeUpkeepEffects() {
		AllZone.Stack.freezeStack();
		upkeep_removeDealtDamageToOppThisTurn();
		upkeep_Braid_Of_Fire();
		
		upkeep_Slowtrips();  // for "Draw a card at the beginning of the next turn's upkeep."
		upkeep_UpkeepCost(); //sacrifice unless upkeep cost is paid
		upkeep_DestroyUpkeepCost(); //destroy unless upkeep cost is paid
		upkeep_DamageUpkeepCost(); //deal damage unless upkeep cost is paid
		upkeep_CumulativeUpkeepCost(); //sacrifice unless cumulative upkeep cost is paid
		upkeep_Echo();
		upkeep_TabernacleUpkeepCost();
		upkeep_MagusTabernacleUpkeepCost();
		// upkeep_CheckEmptyDeck_Lose(); //still a little buggy
		
		AllZone.GameAction.checkWheneverKeyword(AllZone.CardFactory.HumanNullCard, "BeginningOfUpkeep", null);
		
		upkeep_The_Abyss();
		upkeep_All_Hallows_Eve();
		upkeep_Mana_Vortex();
		upkeep_Yawgmoth_Demon();
		upkeep_Lord_of_the_Pit();
		upkeep_Drop_of_Honey();
		upkeep_Demonic_Hordes();
		upkeep_Fallen_Empires_Storage_Lands();
		upkeep_Carnophage();
		upkeep_Sangrophage();
		upkeep_Dega_Sanctuary();
		upkeep_Tangle_Wire();
		upkeep_Dance_of_the_Dead();
		upkeep_Mana_Crypt();
		upkeep_Farmstead();
		
		upkeep_Greener_Pastures();
		upkeep_Heartmender();
		upkeep_Shapeshifter();
		upkeep_Vesuvan_Doppelganger_Keyword();
		
		//Kinship cards
		upkeep_Ink_Dissolver();
		upkeep_Kithkin_Zephyrnaut();
		upkeep_Leaf_Crowned_Elder();
		upkeep_Mudbutton_Clanger();
		upkeep_Nightshade_Schemers();
		upkeep_Pyroclast_Consul();
		upkeep_Sensation_Gorger();
		upkeep_Squeaking_Pie_Grubfellows();
		upkeep_Wandering_Graybeard();
		upkeep_Waterspout_Weavers();
		upkeep_Winnower_Patrol();
		upkeep_Wolf_Skull_Shaman();
		
		upkeep_Oversold_Cemetery();
		//upkeep_Nether_Spirit();
		upkeep_Vampire_Lacerator();
		upkeep_Sleeper_Agent();
		upkeep_Pillory_of_the_Sleepless();
		upkeep_Mirror_Sigil_Sergeant();
		upkeep_Dragon_Broodmother(); //put this before bitterblossom and mycoloth, so that they will resolve FIRST

		//Win / Lose
		// Checks for can't win or can't lose happen in Player.altWinConditionMet()
		upkeep_Battle_of_Wits();
		upkeep_Mortal_Combat();
		upkeep_Near_Death_Experience();
		upkeep_Test_of_Endurance();
		upkeep_Helix_Pinnacle();
		upkeep_Barren_Glory();
		upkeep_Felidar_Sovereign();

		// upkeep_Convalescence();
		// upkeep_Convalescent_Care();
		upkeep_Ancient_Runes();
		upkeep_Karma();
		upkeep_Oath_of_Druids();
		upkeep_Oath_of_Ghouls();
		upkeep_Suspend();
		upkeep_Vanishing();
		upkeep_Fading();
		upkeep_Masticore();
		upkeep_Eldrazi_Monument();
		upkeep_Blaze_Counters();
		upkeep_Dark_Confidant(); // keep this one semi-last
		upkeep_Power_Surge();	
		upkeep_AI_Aluren(); 
		// experimental, AI abuse aluren

		AllZone.Stack.unfreezeStack();
	}

	public static void executeDrawStepEffects() {
		AllZone.Stack.freezeStack();
		final Player player = AllZone.Phase.getPlayerTurn();
		draw_Teferi_Puzzle_Box(player);
		draw_Sylvan_Library(player);

    	AllZone.Stack.unfreezeStack();
	}

	public static void executeTapSideEffects(Card c) {
		
		AllZone.GameAction.checkWheneverKeyword(c,"BecomesTapped",null);
		
		//Blight can't be converted to triggers until AF_Destroy properly handles "Defined"
		//which in turn requires lastKnownInfo to get Enchanted right, otherwise Parallax Dementia doesn't work
		/*
		 * Blight - When enchanted land becomes tapped, destroy it.
		 */
		if(c.isEnchantedBy("Blight")) {
			ArrayList<Card> blights = c.getEnchantedBy();
			final Card target = c;
			for(int i = 0; i < blights.size(); i++) {
				Card blight = blights.get(i);
				if(blight.getName().equals("Blight")) {
					Ability ability = new Ability(blight, "0") {
						@Override
						public void resolve() {
							AllZone.GameAction.destroy(target);
						}
					};//Ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(blight.getName()).append(" - Destroy enchanted land.");
					ability.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability);

				}
			}
		}//end Blight

	}//end executeTapSideEffects()

	public static void executePlayCardEffects(SpellAbility sa) {
		// experimental:
		// this method check for cards that have triggered abilities whenever a
		// card gets played
		// (called in MagicStack.java)
		Card c = sa.getSourceCard();
		
		playCard_Cascade(c);
		playCard_Ripple(c);
        playCard_Storm(sa);

		playCard_Dovescape(c); //keep this one top
		playCard_Chalice_of_the_Void(c);
		playCard_Vengevine(c);
		playCard_Demigod_of_Revenge(c);
		playCard_Infernal_Kirin(c);
		playCard_Forced_Fruition(c);
		playCard_Standstill(c);
		playCard_Sigil_of_the_Empty_Throne(c);
		playCard_Curse_of_Wizardry(c);
		playCard_Venser_Emblem(c);
		playCard_Presence_of_the_Master(c);
		
		AllZone.GameAction.checkWheneverKeyword(c,"CastSpell",null);

	}
	
	public static void playCard_Cascade(final Card c) {
		Command Cascade = new Command() {
			private static final long serialVersionUID = -845154812215847505L;
			public void execute() {
				final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield,
						AllZone.HumanPlayer);
				final PlayerZone comp = AllZone.getZone(Constant.Zone.Battlefield,
						AllZone.ComputerPlayer);

				CardList Human_Nexus = new CardList();
				CardList Computer_Nexus = new CardList();
				Human_Nexus.addAll(play.getCards());
				Computer_Nexus.addAll(comp.getCards());

				Human_Nexus = Human_Nexus.getName("Maelstrom Nexus");
				Computer_Nexus = Computer_Nexus.getName("Maelstrom Nexus");
				if (Human_Nexus.size() > 0){
					if (Phase.PlayerSpellCount == 1 && !c.isCopiedSpell())
					{
						for (int i=0;i<Human_Nexus.size();i++)
						{
							DoCascade(c);
						}
					}
						}
				if (Computer_Nexus.size() > 0){
					if (Phase.ComputerSpellCount == 1 && !c.isCopiedSpell())
					{
						for (int i=0;i<Computer_Nexus.size();i++)
						{ 
							DoCascade(c);
						}
					}
						}
				if(c.getKeyword().contains("Cascade") || c.getName().equals("Bituminous Blast")) //keyword gets cleared for Bitumonous Blast
				{
				DoCascade(c);	
				}
			}// execute()

            void DoCascade(Card c) {
                final Player controller = c.getController();
                final PlayerZone lib = AllZone.getZone(Constant.Zone.Library, controller);
                final Card cascCard = c;

                final Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList topOfLibrary = new CardList(lib.getCards());
                        CardList revealed = new CardList();

                        if (topOfLibrary.size() == 0) return;

                        Card cascadedCard = null;
                        Card crd;
                        int count = 0;
                        while (cascadedCard == null) {
                            crd = topOfLibrary.get(count++);
                            revealed.add(crd);
                            if ((!crd.isLand() && CardUtil.getConvertedManaCost(crd.getManaCost()) < CardUtil.getConvertedManaCost(cascCard.getManaCost()))) cascadedCard = crd;

                            if (count == topOfLibrary.size()) break;

                        }//while
                        GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());

                        if (cascadedCard != null && !cascadedCard.isUnCastable()) {

                            if (cascadedCard.getController().equals(AllZone.HumanPlayer)) {
                                StringBuilder title = new StringBuilder();
                                title.append(cascCard.getName()).append(" - Cascade Ability");
                                StringBuilder question = new StringBuilder();
                                question.append("Cast ").append(cascadedCard.getName()).append(" without paying its mana cost?");
                                
                                int answer = JOptionPane.showConfirmDialog(null, question.toString(), title.toString(), JOptionPane.YES_NO_OPTION);
                                
                                if (answer == JOptionPane.YES_OPTION) {
                                    AllZone.GameAction.playCardNoCost(cascadedCard);
                                    revealed.remove(cascadedCard);
                                }
                            } else //computer
                            {
                                ArrayList<SpellAbility> choices = cascadedCard.getBasicSpells();

                                for (SpellAbility sa:choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        revealed.remove(cascadedCard);
                                        break;
                                    }
                                }
                            }
                        }
                        revealed.shuffle();
                        for (Card bottom:revealed) {
                        	AllZone.GameAction.moveToBottomOfLibrary(bottom);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(c).append(" - Cascade.");
                ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

            }
        };
        Cascade.execute();
    }
	
	public static void playCard_Ripple(final Card c) {
		Command Ripple = new Command() {
			private static final long serialVersionUID = -845154812215847505L;
			public void execute() {
				final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield,
						AllZone.HumanPlayer);
				final PlayerZone comp = AllZone.getZone(Constant.Zone.Battlefield,
						AllZone.ComputerPlayer);

				CardList Human_ThrummingStone = new CardList();
				CardList Computer_ThrummingStone = new CardList();
				Human_ThrummingStone.addAll(play.getCards());
				Computer_ThrummingStone.addAll(comp.getCards());

				Human_ThrummingStone = Human_ThrummingStone.getName("Thrumming Stone");
				Computer_ThrummingStone = Computer_ThrummingStone.getName("Thrumming Stone");
						for (int i=0;i<Human_ThrummingStone.size();i++)
						{
							if(c.getController().equals(AllZone.HumanPlayer)) c.addExtrinsicKeyword("Ripple:4");
						}
						for (int i=0;i<Computer_ThrummingStone.size();i++)
						{ 
							if(c.getController().equals(AllZone.ComputerPlayer)) c.addExtrinsicKeyword("Ripple:4");
						}
		 		        ArrayList<String> a = c.getKeyword();
		 		        for(int x = 0; x < a.size(); x++)
		 		            if(a.get(x).toString().startsWith("Ripple")) {
		 		                String parse = c.getKeyword().get(x).toString();                
		 		                 String k[] = parse.split(":");
		 		            	DoRipple(c,Integer.valueOf(k[1]));
		 		            }
			}// execute()

			void DoRipple(Card c, final int RippleCount) {
				final Player controller = c.getController();
				final PlayerZone lib = AllZone.getZone(Constant.Zone.Library, controller);
				final Card RippleCard = c;
				boolean Activate_Ripple = false;
	        	if(controller == AllZone.HumanPlayer){
		        	Object[] possibleValues = {"Yes", "No"};
                    AllZone.Display.showMessage("Activate Ripple? ");
		        	Object q = JOptionPane.showOptionDialog(null, "Activate Ripple for " + c, "Ripple", 
		        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
		        			null, possibleValues, possibleValues[0]);
                      if(q.equals(0)) Activate_Ripple = true;
	        	} else Activate_Ripple = true;
	        	if(Activate_Ripple == true) {
				final Ability ability = new Ability(c, "0") {
					@Override
					public void resolve() {
						CardList topOfLibrary = new CardList(lib.getCards());
						CardList revealed = new CardList();
						int RippleNumber = RippleCount;
						if(topOfLibrary.size() == 0) return;
						int RippleMax = 10; // Shouldn't Have more than Ripple 10, seeing as no cards exist with a ripple greater than 4
						Card[] RippledCards = new Card[RippleMax]; 
						Card crd;
						if(topOfLibrary.size() < RippleNumber) RippleNumber = topOfLibrary.size();

						for(int i = 0; i < RippleNumber; i++){
							crd = topOfLibrary.get(i);
							revealed.add(crd);
							if(crd.getName().equals(RippleCard.getName())) RippledCards[i] = crd;
						}//For
							GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());
							for(int i = 0; i < RippleMax; i++) {
						if(RippledCards[i] != null && !RippledCards[i].isUnCastable()) {

							if(RippledCards[i].getController().equals(AllZone.HumanPlayer)) {
					        	Object[] possibleValues = {"Yes", "No"};
					        	Object q = JOptionPane.showOptionDialog(null, "Cast " + RippledCards[i].getName() + "?", "Ripple", 
					        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
					        			null, possibleValues, possibleValues[0]);
			                      if(q.equals(0)) {
										AllZone.GameAction.playCardNoCost(RippledCards[i]);
										revealed.remove(RippledCards[i]);
									}
							} else //computer
							{
								ArrayList<SpellAbility> choices = RippledCards[i].getBasicSpells();

								for(SpellAbility sa:choices) {
									if(sa.canPlayAI() && !sa.getSourceCard().getType().contains("Legendary")) {
										ComputerUtil.playStackFree(sa);
										revealed.remove(RippledCards[i]);
										break;
									}
								}
							}
						}
					}
							revealed.shuffle();
							for(Card bottom:revealed) {
								AllZone.GameAction.moveToBottomOfLibrary(bottom);
							}
					}
				};
				StringBuilder sb = new StringBuilder();
				sb.append(c).append(" - Ripple.");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
			}
		};
		Ripple.execute();
	}//playCard_Ripple()
	
	public static void playCard_Storm(SpellAbility sa) {
		Card source = sa.getSourceCard();
		if(!source.isCopiedSpell() && source.getKeyword().contains("Storm"))
		{		
			int StormNumber  = Phase.getStormCount() - 1;		
			for(int i = 0; i < StormNumber; i++)
				AllZone.CardFactory.copySpellontoStack(source, source, sa, true);   	
		}
	}//playCard_Storm()
	
	public static void playCard_Vengevine(Card c) {
		if (c.isCreature() == true && (Phase.PlayerCreatureSpellCount == 2 || Phase.ComputerCreatureSpellCount == 2))
		{
		final Player controller = c.getController();
		final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, controller);
		final PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
		CardList list = new CardList();
		list.addAll(grave.getCards());
		list = list.getName("Vengevine");
		if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					Ability ability = new Ability(card, "0") {
						@Override
						public void resolve() {
				        	if(controller == AllZone.HumanPlayer){
					        	Object[] possibleValues = {"Yes", "No"};
					        	Object q = JOptionPane.showOptionDialog(null, "Return Vengevine from the graveyard?", "Vengevine Ability", 
					        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
					        			null, possibleValues, possibleValues[0]);
			                      if(q.equals(0)) {
				                    if(AllZone.GameAction.isCardInZone(card, grave)) {
				                        AllZone.GameAction.moveTo(play, card);
				                    }
					    		}
				        	} else {
			                    if(AllZone.GameAction.isCardInZone(card, grave)) {
			                        AllZone.GameAction.moveTo(play, card);
			                    }
				        	}
						}
					}; // ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append("Whenever you cast a spell, if it's the second creature ");
					sb.append("spell you cast this turn, you may return Vengevine from your graveyard to the battlefield.");
					ability.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability);

				}//if
			}
		}
	}//playCard_Vengevine()
	
	public static void playCard_Presence_of_the_Master(Card c) {
		//if(sp instanceof Spell_Permanent && sp.getSourceCard().isEnchantment()) {
		if(AllZoneUtil.isCardInPlay("Presence of the Master") && c.isEnchantment()) {
			final Card source = AllZoneUtil.getCardsInPlay("Presence of the Master").get(0);
			SpellAbility counter = new Ability(source, "") {
				@Override
				public void resolve() {
					AllZone.Stack.pop();
				}
			};
			counter.setStackDescription(source.getName()+" - counter enchantment spell.");

            AllZone.Stack.addSimultaneousStackEntry(counter);

		}
	}
	
	public static void playCard_Venser_Emblem(Card c)
	{
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.filter(new CardListFilter(){
			public boolean addCard(Card crd)
			{
				return crd.getKeyword().contains("Whenever you cast a spell, exile target permanent.");
			}
		});
		
		for (int i=0;i<list.size();i++)
		{
			final Card card = list.get(i);
			final SpellAbility ability = new Ability(card, "0")
			{
				public void resolve()
				{
					Card target = getTargetCard();
					if (CardFactoryUtil.canTarget(card, target) && AllZone.GameAction.isCardInPlay(target))
						AllZone.GameAction.exile(target);
				}
				
				public void chooseTargetAI()
				{
					CardList humanList = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
					CardList compList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
					
					CardListFilter filter = new CardListFilter(){
						public boolean addCard(Card c)
						{
							return CardFactoryUtil.canTarget(card, c);
						}
					};
					
					humanList = humanList.filter(filter);
					compList = compList.filter(filter);
					
					if (humanList.size() > 0)
					{
						CardListUtil.sortCMC(humanList);
						setTargetCard(humanList.get(0));
					}
					else if (compList.size() > 0)
					{
						CardListUtil.sortCMC(compList);
						compList.reverse();
						setTargetCard(compList.get(0));
					}
								
				}
			};
			
			Input runtime = new Input() {
				private static final long serialVersionUID = -7620283169787412409L;

				@Override
                public void showMessage() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Battlefield.getCards());
                    list.addAll(AllZone.Computer_Battlefield.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isPermanent() && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, list,
                            "Select target permanent to Exile", true, false));
                }//showMessage()
            };//Input

			ability.setBeforePayMana(runtime);
			if (controller.equals(AllZone.HumanPlayer))
				AllZone.GameAction.playSpellAbility(ability);
			else {
				ability.chooseTargetAI();

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}
	
	public static void playCard_Chalice_of_the_Void(Card c) {
		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getName("Chalice of the Void");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final SpellAbility sa = AllZone.Stack.peek();
				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						AllZone.Stack.pop();
						AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a spell with same amount of charge counters on Chalice of the Void. The spell is countered");
				ability2.setStackDescription(sb.toString());
                
				int convertedManaSpell = CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost());								
				if(sa.isSpell() == true && card.getCounters(Counters.CHARGE) == convertedManaSpell)
                    AllZone.Stack.addSimultaneousStackEntry(ability2);

			}					
		}//if
	} // Chalice_of_the_Void 

	public static void playCard_Demigod_of_Revenge(final Card c) {
		// not enough boom stick references in this block of code
		if(c.getName().equals("Demigod of Revenge")) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					CardList evildead = AllZoneUtil.getPlayerGraveyard(c.getController(), "Demigod of Revenge");

					for(Card c : evildead){
						AllZone.GameAction.moveToPlay(c);
					}
				}
			}; // ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(c.getController());
			sb.append(" casts Demigod of Revenge, returns all cards named Demigod ");
			sb.append("of Revenge from your graveyard to the battlefield.");
			ability2.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability2);


		}//if					
	}// Demigod of Revenge

	public static void playCard_Infernal_Kirin(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Infernal Kirin");

		if(list.size() > 0) {
			if(c.isType("Spirit") || c.getType().contains("Arcane")) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final int converted = CardUtil.getConvertedManaCost(c.getManaCost());
					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							final Player target;
							if(card.getController().isHuman()) {
								String[] choices = {"Opponent", "Yourself"};
								Object choice = GuiUtils.getChoice("Choose target player", choices);
								if(choice.equals("Opponent")) {
									target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here
								}// if choice yes
								else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							} else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target);
							CardList fullHand = new CardList(hand.getCards());
							if(fullHand.size() > 0 && target.equals(AllZone.ComputerPlayer)) GuiUtils.getChoice(
									"Revealing hand", fullHand.toArray());
							CardList discard = new CardList(hand.getCards());
							discard = discard.filter(new CardListFilter() {
								public boolean addCard(Card c) {
									return CardUtil.getConvertedManaCost(c.getManaCost()) == converted;
								}
							});
							for(int j = 0; j < discard.size(); j++) {
								Card choice = discard.get(j);
								choice.getController().discard(choice, this);
							}
						} //resolve
					}; //ability
					ability2.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
					ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a Spirit or Arcane spell, target player reveals his or her hand ");
					sb.append("and discards all cards with converted mana cost ").append(converted).append(".");
					ability2.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability2);

				}
			}//if
		}

	}//Infernal Kirin

	public static void playCard_Dovescape(Card c) {
		final Card crd1 = c;
		PlayerZone hplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
		PlayerZone cplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);

		CardList list = new CardList();
		list.addAll(hplay.getCards());
		list.addAll(cplay.getCards());
		final int cmc = CardUtil.getConvertedManaCost(c.getManaCost());
		list = list.getName("Dovescape");
		final CardList cl = list;
		if(!c.getType().contains("Creature") && list.size() > 0) {


			final Card card = list.get(0);

			Ability ability2 = new Ability(card, "0") {
				@Override
				public void resolve() {

					SpellAbility sa = AllZone.Stack.peek();

					if(sa.getSourceCard().equals(crd1)) {
						sa = AllZone.Stack.pop();

						AllZone.GameAction.moveToGraveyard(sa.getSourceCard());

						for(int j = 0; j < cl.size() * cmc; j++) {
							CardFactoryUtil.makeToken("Bird", "WU 1 1 Bird", sa.getSourceCard().getController(), "W U", new String[] {
								"Creature", "Bird"}, 1, 1, new String[] {"Flying"});
						}

						/*
                        SpellAbility sa = AllZone.Stack.peek
                        if (!sa.getSourceCard().isCreature() && sa.isSpell())
                        {

                        }
						 */
					} else //TODO 
					{
						;
					}


				}
			}; // ability2

			ability2.setStackDescription("Dovescape Ability");

            AllZone.Stack.addSimultaneousStackEntry(ability2);


		}
	} // Dovescape

	public static void playCard_Forced_Fruition(Card c) {
		CardList list = AllZoneUtil.getCardsInPlay("Forced Fruition");

		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			final Player drawer = card.getController().getOpponent();


			Ability ability2 = new Ability(card, "0") {
				@Override
				public void resolve() {
					drawer.drawCards(7);
				}
			}; // ability2
			if(!(card.getController().equals(c.getController()))) {
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a spell, ").append(drawer).append(" draws seven cards.");
				ability2.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability2);

			}
		}

	}

	public static void playCard_Standstill(Card c) {
		CardList list = AllZoneUtil.getCardsInPlay("Standstill");

		for(int i = 0; i < list.size(); i++) {
			final Player drawer = c.getController().getOpponent();
			final Card card = list.get(i);

			Ability ability2 = new Ability(card, "0") {
				@Override
				public void resolve() {
					// sac standstill
					AllZone.GameAction.sacrifice(card);
					// player who didn't play spell, draws 3 cards
					drawer.drawCards(3);
				}
			}; // ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(card.getName()).append(" - ").append(c.getController());
			sb.append(" played a spell, ").append(drawer).append(" draws three cards.");
			ability2.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability2);


		}

	}

	public static void playCard_Sigil_of_the_Empty_Throne(Card c) {

		final PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Sigil of the Empty Throne");
		if(c.isEnchantment()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(0);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						CardFactoryUtil.makeToken("Angel", "W 4 4 Angel", card.getController(), "W", new String[] {
								"Creature", "Angel"}, 4, 4, new String[] {"Flying"});
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" puts a 4/4 White Angel token with flying onto the battlefield.");
				ability2.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability2);


			} // for
		}// if isEnchantment()
	}
	
	public static void playCard_Curse_of_Wizardry(final Card c) {
		CardList list = AllZoneUtil.getCardsInPlay("Curse of Wizardry");

		if(list.size() > 0){
			ArrayList<String> cl=CardUtil.getColors(c);
			
				for (int i=0;i<list.size();i++) {
					final Card card = list.get(i);
					if(cl.contains(card.getChosenColor())) {
					Ability ability = new Ability(card, "0") {
						public void resolve() {
							c.getController().loseLife(1, card);
						} //resolve
					};//ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a ").append(card.getChosenColor()).append(" spell, ");
					sb.append(c.getController()).append(" loses 1 life.");
					ability.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability);

				}
			}//if
		}//if
	}//Curse of Wizardry

	//UPKEEP CARDS:

	public static void payManaDuringAbilityResolve(String message, String manaCost, Command paid, Command unpaid){
		// temporarily disable the Resolve flag, so the user can payMana for the resolving Ability
		boolean bResolving = AllZone.Stack.getResolving();
		AllZone.Stack.setResolving(false);
		AllZone.InputControl.setInput(new Input_PayManaCost_Ability(message, manaCost, paid, unpaid));
		AllZone.Stack.setResolving(bResolving);
	}
	
	private static void upkeep_removeDealtDamageToOppThisTurn() {
		// TODO: this should happen in the cleanup phase
		// resets the status of attacked/blocked this turn
		Player player = AllZone.Phase.getPlayerTurn();
		Player opp = player.getOpponent();
		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, opp);

		CardList list = new CardList();
		list.addAll(play.getCards());
		list = list.getType("Creature");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			//c.setDealtCombatDmgToOppThisTurn(false);
			c.setDealtDmgToHumanThisTurn(false);
			c.setDealtDmgToComputerThisTurn(false);
		}
	}

	private static void upkeep_Braid_Of_Fire(){
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList braids = AllZoneUtil.getPlayerCardsInPlay(player, "Braid of Fire");
		
		for(int i = 0; i < braids.size(); i++) {
			final Card c = braids.get(i);

			final StringBuilder sb = new StringBuilder();
			sb.append("Cumulative Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getUpkeepCost()) {
				@Override
				public void resolve() {
					c.addCounter(Counters.AGE, 1);
					if (player.isComputer()){
						// AI can't handle this yet without manapool
						AllZone.GameAction.sacrifice(c);
					}
					else if (GameActionUtil.showYesNoDialog(c, sb.toString())){
						int ageCounters = c.getCounters(Counters.AGE);
						Ability_Mana abMana = new Ability_Mana(c, "0", "R", ageCounters) {
							private static final long serialVersionUID = -2182129023960978132L;
						};
						abMana.produceMana();
					}
					else{
						AllZone.GameAction.sacrifice(c);
					}
						
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

		}
	} //upkeep_Braid_of_Fire
	
	public static void upkeep_TabernacleUpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());

		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith(
							"At the beginning of your upkeep, destroy this creature unless you pay")) {
						String k[] = a.get(i).toString().split("pay ");
						k[1] = k[1].substring(0, k[1].length() - 1);
						c.setTabernacleUpkeepCost(k[1]);
						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command paidCommand = Command.Blank;
			
			final Command unpaidCommand = new Command() {

				private static final long serialVersionUID = -8737736216222268696L;

				public void execute() {
					AllZone.GameAction.destroy(c);
				}
			};
			
			final Ability aiPaid = upkeepAIPayment(c, c.getTabernacleUpkeepCost());
			
			// TODO convert to triggered ability when they are no longer Commands
			final StringBuilder sb = new StringBuilder();
			sb.append("Tabernacle Upkeep for ").append(c).append("\n");
			final Ability destroyAbility = new Ability(c, c.getTabernacleUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getTabernacleUpkeepCost(), paidCommand, unpaidCommand);
					}
					else
						if(ComputerUtil.canPayCost(aiPaid)) 
							ComputerUtil.playNoStack(aiPaid);
						else 
							AllZone.GameAction.destroy(c);
				}
			};
			destroyAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(destroyAbility);

		}
	}//TabernacleUpkeepCost

	private static void upkeep_MagusTabernacleUpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith(
							"At the beginning of your upkeep, sacrifice this creature unless you pay")) {
						String k[] = a.get(i).toString().split("pay ");
						k[1] = k[1].substring(0, k[1].length() - 1);
						c.setMagusTabernacleUpkeepCost(k[1]);
						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command unpaidCommand = new Command() {
				private static final long serialVersionUID = 660060621665783254L;

				public void execute() {
					AllZone.GameAction.sacrifice(c);
				}
			};

			final Command paidCommand = Command.Blank;
			
			final Ability aiPaid = upkeepAIPayment(c, c.getMagusTabernacleUpkeepCost());
			
			final StringBuilder sb = new StringBuilder();
			sb.append("Magus of the Tabernacle Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getMagusTabernacleUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getMagusTabernacleUpkeepCost(), paidCommand, unpaidCommand);
					}
					else
						if(ComputerUtil.canPayCost(aiPaid)) 
							ComputerUtil.playNoStack(aiPaid);
						else 
							AllZone.GameAction.sacrifice(c);
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

			AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

			
		}
	}//MagusTabernacleUpkeepCost

	public static void upkeep_CumulativeUpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith("Cumulative upkeep")) {
						String k[] = a.get(i).toString().split(":");
						c.addCounter(Counters.AGE, 1);
						String upkeepCost = CardFactoryUtil.multiplyManaCost(k[1], c.getCounters(Counters.AGE));
						c.setUpkeepCost(upkeepCost);
						System.out.println("Multiplied cost: " + upkeepCost);
						//c.setUpkeepCost(k[1]);
						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command unpaidCommand = new Command() {

				private static final long serialVersionUID = -8737736216222268696L;

				public void execute() {
					AllZone.GameAction.sacrifice(c);
				}
			};

			final Command paidCommand = Command.Blank;
			
			final Ability aiPaid = upkeepAIPayment(c, c.getUpkeepCost());
			
			final StringBuilder sb = new StringBuilder();
			sb.append("Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getUpkeepCost(), paidCommand, unpaidCommand);
					}
					else
						if(ComputerUtil.canPayCost(aiPaid)) 
							ComputerUtil.playNoStack(aiPaid);
						else 
							AllZone.GameAction.sacrifice(c);
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

		}
	}//upkeepCost

	private static void upkeep_Echo() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.getKeyword().contains("(Echo unpaid)");
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);
			if(c.getIntrinsicKeyword().contains("(Echo unpaid)")) {

				final Command paidCommand = Command.Blank;
				
				final Command unpaidCommand = new Command() {
					private static final long serialVersionUID = -7354791599039157375L;

					public void execute() {
						AllZone.GameAction.sacrifice(c);
					}
				};
				
				final Ability aiPaid = upkeepAIPayment(c, c.getEchoCost());
				
				final StringBuilder sb = new StringBuilder();
				sb.append("Echo for ").append(c).append("\n");
				
				final Ability sacAbility = new Ability(c, "0") {
					@Override
					public void resolve() {
						if(c.getController().equals(AllZone.HumanPlayer)) {
							payManaDuringAbilityResolve(sb.toString(), c.getEchoCost(), paidCommand, unpaidCommand);
						}
						else //computer
						{
							if(ComputerUtil.canPayCost(aiPaid)) 
								ComputerUtil.playNoStack(aiPaid);
							else 
								AllZone.GameAction.sacrifice(c);
						}
					}
				};
				sacAbility.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(sacAbility);


				c.removeIntrinsicKeyword("(Echo unpaid)");
			}
		}
	}//echo

	private static void upkeep_Slowtrips() {  // Draw a card at the beginning of the next turn's upkeep.
		final Player player = AllZone.Phase.getPlayerTurn();
		
		CardList list = player.getSlowtripList();
		
		for (int i = 0; i < list.size(); i++) {
			Card card = list.get(i);
			card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep."); //otherwise another slowtrip gets added
			
			final Ability slowtrip = new Ability(card, "0") {
				@Override
				public void resolve() {
					player.drawCard();
				}
			};
			slowtrip.setStackDescription(card.getName() + " - Draw a card");

			AllZone.Stack.addSimultaneousStackEntry(slowtrip);

			
		}
		player.clearSlowtripList();
		
		//Do the same for the opponent
		final Player opponent = player.getOpponent();
		
		list = opponent.getSlowtripList();
		
		for (int i = 0; i < list.size(); i++) {
			Card card = list.get(i);
			card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep."); //otherwise another slowtrip gets added
			
			final Ability slowtrip = new Ability(card, "0") {
				@Override
				public void resolve() {
					opponent.drawCard();
				}
			};
			slowtrip.setStackDescription(card.getName() + " - Draw a card");

            AllZone.Stack.addSimultaneousStackEntry(slowtrip);

		}
		opponent.clearSlowtripList();
	}
	
	
	private static void upkeep_UpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith("At the beginning of your upkeep, sacrifice " + c.getName()) ||
							a.get(i).toString().startsWith("At the beginning of your upkeep, sacrifice CARDNAME")) {
						String k[] = a.get(i).toString().split(" pay ");
						c.setUpkeepCost(k[1]);
						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command unpaidCommand = new Command() {

				private static final long serialVersionUID = -6483405139208343935L;

				public void execute() {
					AllZone.GameAction.sacrifice(c);
				}
			};

			final Command paidCommand = Command.Blank;
			
			final Ability aiPaid = upkeepAIPayment(c, c.getUpkeepCost());
			
			final StringBuilder sb = new StringBuilder();
			sb.append("Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getUpkeepCost(), paidCommand, unpaidCommand);
					} 
					else //computer
					{
						if(ComputerUtil.canPayCost(aiPaid)) 
							ComputerUtil.playNoStack(aiPaid);
						else 
							AllZone.GameAction.sacrifice(c);
					}
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

		}
	}//upkeepCost

	private static void upkeep_DestroyUpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
						String k[] = a.get(i).toString().split(" pay ");
						c.setUpkeepCost(k[1]);
						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command unpaidCommand = new Command() {
				private static final long serialVersionUID = 8942537892273123542L;

				public void execute() {
					if(c.getName().equals("Cosmic Horror")) {
						Player player = c.getController();
						player.addDamage(7, c);
					}
					AllZone.GameAction.destroy(c);
				}
			};

			final Command paidCommand = Command.Blank;
			
			final Ability aiPaid = upkeepAIPayment(c, c.getUpkeepCost());
			
			final StringBuilder sb = new StringBuilder();
			sb.append("Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getUpkeepCost(), paidCommand, unpaidCommand);
					} 
					else //computer
					{
						if(ComputerUtil.canPayCost(aiPaid)) 
							ComputerUtil.playNoStack(aiPaid);
						else AllZone.GameAction.destroy(c);
					}		
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

		}
	}//upkeepCost


	private static void upkeep_DamageUpkeepCost() {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				ArrayList<String> a = c.getKeyword();
				for(int i = 0; i < a.size(); i++) {
					if(a.get(i).toString().startsWith(
							"At the beginning of your upkeep, CARDNAME deals ")) {
						String k[] = a.get(i).toString().split("deals ");
						String s1 = k[1].substring(0, 2);
						s1 = s1.trim();
						c.setUpkeepDamage(Integer.parseInt(s1));
						System.out.println(k[1]);
						String l[] = k[1].split(" pay ");
						System.out.println(l[1]);
						c.setUpkeepCost(l[1]);

						return true;
					}
				}
				return false;
			}
		});

		for(int i = 0; i < list.size(); i++) {
			final Card c = list.get(i);

			final Command unpaidCommand = new Command() {
				private static final long serialVersionUID = 8942537892273123542L;

				public void execute() {
					Player player = c.getController();
					player.addDamage(c.getUpkeepDamage(), c);
				}
			};

			final Command paidCommand = Command.Blank;
			
			final Ability aiPaid = upkeepAIPayment(c, c.getUpkeepCost());
			
			final StringBuilder sb = new StringBuilder();
			sb.append("Upkeep for ").append(c).append("\n");
			final Ability upkeepAbility = new Ability(c, c.getUpkeepCost()) {
				@Override
				public void resolve() {
					if(c.getController().equals(AllZone.HumanPlayer)) {
						payManaDuringAbilityResolve(sb.toString(), c.getUpkeepCost(), paidCommand, unpaidCommand);

					} 
					else //computer
					{
						if(ComputerUtil.canPayCost(aiPaid)) ComputerUtil.playNoStack(aiPaid);
						else AllZone.GameAction.sacrifice(c);
					}
				}
			};
			upkeepAbility.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(upkeepAbility);

		}
	}//damageUpkeepCost
	
	private static Ability upkeepAIPayment(Card c, String cost){
		return new Ability_Static(c, cost) {
			@Override
			public void resolve() {
				
			}
		};
	}
	
	private static void upkeep_The_Abyss() {
		/*
		 * At the beginning of each player's upkeep, destroy target
		 * nonartifact creature that player controls of his or her
		 * choice. It can't be regenerated.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList the = AllZoneUtil.getCardsInPlay("The Abyss");
		final CardList magus = AllZoneUtil.getCardsInPlay("Magus of the Abyss");
		
		CardList cards = new CardList();
		cards.add(the);
		cards.add(magus);
		
		for(Card c:cards) {
			final Card abyss = c;
			
			final Ability sacrificeCreature = new Ability(abyss, "") {
				@Override
				public void resolve() {
					if(player.equals(AllZone.HumanPlayer)) {
						if(abyss_getTargets(player, abyss).size() > 0) {
							AllZone.InputControl.setInput( new Input() {
								private static final long serialVersionUID = 4820011040853968644L;
								public void showMessage() {
									AllZone.Display.showMessage(abyss.getName()+" - Select one nonartifact creature to destroy");
									ButtonUtil.disableAll();
								}
								public void selectCard(Card selected, PlayerZone zone) {
									//probably need to restrict by controller also
									if(selected.isCreature() && !selected.isArtifact() && CardFactoryUtil.canTarget(abyss, selected)
											&& zone.is(Constant.Zone.Battlefield) && zone.getPlayer().equals(AllZone.HumanPlayer)) {
										AllZone.GameAction.destroyNoRegeneration(selected);
										stop();
									}
								}//selectCard()
							});//Input
						}
					}
					else { //computer
						CardList targets = abyss_getTargets(player,abyss);
						CardList indestruct = targets.getKeyword("Indestructible");
						if(indestruct.size() > 0) {
							AllZone.GameAction.destroyNoRegeneration(indestruct.get(0));
						}
						else {
							Card target = CardFactoryUtil.AI_getWorstCreature(targets);
							if(null == target) {
								//must be nothing valid to destroy
							}
							else AllZone.GameAction.destroyNoRegeneration(target);
						}
					}
				}//resolve
			};//sacrificeCreature
			
			StringBuilder sb = new StringBuilder();
			sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
			sacrificeCreature.setStackDescription(sb.toString());
			
			if(abyss_getTargets(player,abyss).size() > 0)
                AllZone.Stack.addSimultaneousStackEntry(sacrificeCreature);

		}//end for
	}//The Abyss
	
	private static CardList abyss_getTargets(final Player player, Card card) {
		CardList creats = AllZoneUtil.getCreaturesInPlay(player);
		creats = creats.filter(AllZoneUtil.nonartifacts);
		creats = creats.getTargetableCards(card);
		return creats;
	}
	
	private static void upkeep_Mana_Vortex() {
		/*
		 * At the beginning of each player's upkeep, that player
		 * sacrifices a land.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList vortices = AllZoneUtil.getCardsInPlay("Mana Vortex");
		
		for(Card c:vortices) {
			final Card vortex = c;
			
			final Ability sacrificeLand = new Ability(vortex, "") {
				@Override
				public void resolve() {
					CardList choices = AllZoneUtil.getPlayerLandsInPlay(player);
					player.sacrificePermanent(vortex.getName()+" - select a land to sacrifice.", choices);
					
					//if no lands in play, sacrifice all "Mana Vortex"s
					if(AllZoneUtil.getLandsInPlay().size() == 0) {
						for(Card d:vortices) {
							AllZone.GameAction.sacrifice(d);
						}
						return;
					}
				}//resolve
			};//sacrificeCreature
			
			StringBuilder sb = new StringBuilder();
			sb.append(vortex.getName()).append(" - "+player+" sacrifices a land.");
			sacrificeLand.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(sacrificeLand);

		}//end for
	}//Mana_Vortex
	
	private static void upkeep_All_Hallows_Eve() {
		/*
		 * At the beginning of your upkeep, if All Hallow's Eve is exiled
		 * with a scream counter on it, remove a scream counter from it.
		 * If there are no more scream counters on it, put it into your
		 * graveyard and each player returns all creature cards from his
		 * or her graveyard to the battlefield.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList eves = AllZoneUtil.getPlayerCardsInExile(player, "All Hallow's Eve");
		
		for(Card c:eves) {
			final Card eve = c;
			eve.clearSpellAbility();
			
			final Ability hallow = new Ability(eve, "") {
				@Override
				public void resolve() {
					
					if(AllZone.GameAction.isCardExiled(eve) && eve.getCounters(Counters.SCREAM) > 0) {
						eve.subtractCounter(Counters.SCREAM, 1);

						if(eve.getCounters(Counters.SCREAM) == 0) {
							eve.clearReplaceMoveToGraveyardCommandList();
							AllZone.GameAction.moveToGraveyard(eve);

							CardList compGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
							compGrave = compGrave.filter(AllZoneUtil.creatures);
							CardList humanGrave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
							humanGrave = humanGrave.filter(AllZoneUtil.creatures);

							for(Card cc:compGrave) AllZone.GameAction.moveToPlay(cc);
							for(Card hc:humanGrave) AllZone.GameAction.moveToPlay(hc);
						}
					}
				}//resolve
			};//sacrificeCreature
			
			StringBuilder sb = new StringBuilder();
			sb.append(eve.getName()).append(" - remove a scream counter and return creatures to the battlefield.");
			hallow.setStackDescription(sb.toString());
			if(AllZone.GameAction.isCardExiled(eve)) {
				AllZone.Stack.addSimultaneousStackEntry(hallow);

			}
		}//end for
	}//All_Hallows_Eve
	
	private static void upkeep_Yawgmoth_Demon() {
		/*
		 * At the beginning of your upkeep, you may sacrifice an artifact. If
		 * you don't, tap Yawgmoth Demon and it deals 2 damage to you.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList cards = AllZoneUtil.getPlayerCardsInPlay(player, "Yawgmoth Demon");

		for(int i = 0; i < cards.size(); i++) {
			final Card c = cards.get(i);

			final Ability sacrificeArtifact = new Ability(c, "") {
				@Override
				public void resolve() {
					CardList artifacts = AllZoneUtil.getPlayerCardsInPlay(player);
					artifacts = artifacts.filter(AllZoneUtil.artifacts);
					
					if(player.equals(AllZone.HumanPlayer)) {
						AllZone.InputControl.setInput( new Input() {
							private static final long serialVersionUID = -1698502376924356936L;
							public void showMessage() {
								AllZone.Display.showMessage("Yawgmoth Demon - Select one artifact to sacrifice or be dealt 2 damage");
								ButtonUtil.enableOnlyCancel();
							}
							public void selectButtonCancel() {
								tapAndDamage(player);
								stop();
							}
							public void selectCard(Card artifact, PlayerZone zone) {
								//probably need to restrict by controller also
								if(artifact.isArtifact() && zone.is(Constant.Zone.Battlefield)
										&& zone.getPlayer().equals(AllZone.HumanPlayer)) {
									AllZone.GameAction.sacrifice(artifact);
									stop();
								}
							}//selectCard()
						});//Input
					}
					else { //computer
						Card target = CardFactoryUtil.AI_getCheapestPermanent(artifacts, c, false);
						if(null == target) {
							tapAndDamage(player);
						}
						else AllZone.GameAction.sacrifice(target);
					}
				}//resolve

				private void tapAndDamage(Player player) {
					c.tap();
					player.addDamage(2, c);
				}
			};
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - sacrifice an artifact or ");
			sb.append(c.getName()).append(" becomes tapped and deals 2 damage to you.");
			sacrificeArtifact.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(sacrificeArtifact);

		}//end for
	}
	
	private static void upkeep_Lord_of_the_Pit() {
		/*
		 * At the beginning of your upkeep, sacrifice a creature other than
		 * Lord of the Pit. If you can't, Lord of the Pit deals 7 damage to you.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList lords = AllZoneUtil.getPlayerCardsInPlay(player, "Lord of the Pit");
		lords.add(AllZoneUtil.getPlayerCardsInPlay(player, "Liege of the Pit"));
		final CardList cards = lords;
		
		for(int i = 0; i < cards.size(); i++) {
			final Card c = cards.get(i);
			if (c.isFaceDown()) continue;

			final Ability sacrificeCreature = new Ability(c, "") {
				@Override
				public void resolve() {
					//TODO: this should handle the case where you sacrifice 2 LOTPs to each other
					CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
					creatures.remove(c);
					if(player.equals(AllZone.HumanPlayer)) {
        				AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanent(creatures, c.getName()+" - Select a creature to sacrifice."));
        			}
					else { //computer
						Card target = CardFactoryUtil.AI_getWorstCreature(creatures);
						AllZone.GameAction.sacrifice(target);
					}
				}//resolve
			};
			
			final Ability sevenDamage = new Ability(c, "") {
				@Override
				public void resolve() {
					player.addDamage(7, c);
				}
			};
			
			CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
			creatures.remove(c);
			if(creatures.size() == 0) {
				//there are no creatures to sacrifice, so we must do the 7 damage
				
				StringBuilder sb = new StringBuilder();
				sb.append(c.getName()).append(" - deals 7 damage to controller");
				sevenDamage.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(sevenDamage);

			}
			else {
				
				StringBuilder sb = new StringBuilder();
				sb.append(c.getName()).append(" - sacrifice a creature.");
				sacrificeCreature.setStackDescription(sb.toString());
				
				AllZone.Stack.addSimultaneousStackEntry(sacrificeCreature);

			}
		}//end for
	}// upkeep_Lord_of_the_Pit()
	
	private static void upkeep_Drop_of_Honey() {
		/*
		 * At the beginning of your upkeep, destroy the creature with the
		 * least power. It can't be regenerated. If two or more creatures
		 * are tied for least power, you choose one of them.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList drops = AllZoneUtil.getPlayerCardsInPlay(player, "Drop of Honey");
		drops.add(AllZoneUtil.getPlayerCardsInPlay(player, "Porphyry Nodes"));
		final CardList cards = drops;
		
		for(int i = 0; i < cards.size(); i++) {
			final Card c = cards.get(i);
			
			final Ability ability = new Ability(c, "") {
				@Override
				public void resolve() {
					CardList creatures = AllZoneUtil.getCreaturesInPlay();
					if(creatures.size() > 0) {
						CardListUtil.sortAttackLowFirst(creatures);
						int power = creatures.get(0).getNetAttack();
						if(player.equals(AllZone.HumanPlayer)) {
							AllZone.InputControl.setInput(CardFactoryUtil.input_destroyNoRegeneration(getLowestPowerList(creatures), "Select creature with power: "+power+" to sacrifice."));
						}
						else { //computer
							Card compyTarget = getCompyCardToDestroy(creatures);
							AllZone.GameAction.destroyNoRegeneration(compyTarget);
						}
					}
				}//resolve
				
				private CardList getLowestPowerList(CardList original) {
					CardList lowestPower = new CardList();
					int power = original.get(0).getNetAttack();
					int i = 0;
					while(i < original.size() && original.get(i).getNetAttack() == power) {
						lowestPower.add(original.get(i));
						i++;
					}
					return lowestPower;
				}
				
				private Card getCompyCardToDestroy(CardList original) {
					CardList options = getLowestPowerList(original);
					CardList humanCreatures = options.filter(new CardListFilter() {
						public boolean addCard(Card c) {
							return c.getController().equals(AllZone.HumanPlayer);
						}
					});
					if(humanCreatures.isEmpty()) {
						options.shuffle();
						return options.get(0);
					}
					else {
						humanCreatures.shuffle();
						return humanCreatures.get(0);
					}
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//end for
	}// upkeep_Drop_of_Honey()
	
	private static void upkeep_Demonic_Hordes() {
		
		/*
		 * At the beginning of your upkeep, unless you pay BBB, 
		 * tap Demonic Hordes and sacrifice a land of an opponent's choice.
		 */
		
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList cards = AllZoneUtil.getPlayerCardsInPlay(player, "Demonic Hordes");
		
		for(int i = 0; i < cards.size(); i++) {
			
			final Card c = cards.get(i);
			
			final Ability noPay = new Ability(c, "B B B") {
				private static final long serialVersionUID = 4820011390853920644L;
				@Override
					public void resolve() {
						CardList playerLand = AllZoneUtil.getPlayerLandsInPlay(player);
						
						c.tap();
						if (c.getController().isComputer()){
							if (playerLand.size() > 0)
								AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanent(playerLand, c.getName()+" - Select a land to sacrifice."));
						}
						else {
								Card target = CardFactoryUtil.AI_getBestLand(playerLand);
								
								AllZone.GameAction.sacrifice(target);
						}
					} //end resolve()
			}; //end noPay ability
			
            if (c.getController().equals(AllZone.HumanPlayer)) {
                String question = "Pay Demonic Hordes upkeep cost?";
                if (GameActionUtil.showYesNoDialog(c, question)) {
                    final Ability pay = new Ability(c, "0") {
                        private static final long serialVersionUID = 4820011440853920644L;
                        public void resolve() {
                            if (AllZone.getZone(c).is(Constant.Zone.Battlefield)) {
                                StringBuilder cost = new StringBuilder();
                                cost.append("Pay cost for ").append(c).append("\r\n");
                                GameActionUtil.payManaDuringAbilityResolve(cost.toString(), noPay.getManaCost(), Command.Blank, Command.Blank);
                            }
                        } //end resolve()
                    }; //end pay ability
                    pay.setStackDescription("Demonic Hordes - Upkeep Cost");

                    AllZone.Stack.addSimultaneousStackEntry(pay);

                } //end choice
                else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
                    noPay.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(noPay);

                }
            } //end human
			else { //computer
				if((c.getController().equals(AllZone.ComputerPlayer) && (ComputerUtil.canPayCost(noPay)))) {
					final Ability computerPay = new Ability(c, "0") {
						private static final long serialVersionUID = 4820011440852868644L;
						public void resolve() {
							ComputerUtil.payManaCost(noPay);
						}
					};
					computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    AllZone.Stack.addSimultaneousStackEntry(computerPay);

				} 
				else {
                    AllZone.Stack.addSimultaneousStackEntry(noPay);

				}
			} //end computer
			
		} //end for loop
			
	} //upkeep_Demonic_Hordes

	//END UPKEEP CARDS

	//START ENDOFTURN CARDS

    public static void endOfTurn_Wall_Of_Reverence()
    {
        final Player player = AllZone.Phase.getPlayerTurn();
        final PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
        CardList list = new CardList(playZone.getCards());
        list = list.getName("Wall of Reverence");

        Ability ability;
        for (int i = 0; i < list.size(); i++)
        {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0")
            {
                public void resolve()
                {
                    CardList creats = new CardList(playZone.getCards());
                    CardList validTargets = new CardList();
                    creats = creats.getType("Creature");
                    for (int i = 0; i < creats.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, creats.get(i))) {
                            validTargets.add(creats.get(i));
                        }
                    }
                    if (validTargets.size() == 0)
                        return;

                    if (player.equals(AllZone.HumanPlayer))
                    {
                        Object o = GuiUtils.getChoiceOptional("Select creature for Wall of Reverence life gain", validTargets.toArray());
                        if (o != null) {
                            Card c = (Card) o;
                            int power=c.getNetAttack();
                            player.gainLife(power, card);
                        }
                    }
                    else//computer
                    {
                        CardListUtil.sortAttack(validTargets);
                        Card c = validTargets.get(0);
                        // Card c = creats.get(0);
                        if (c != null) {
                            int power = c.getNetAttack();
                            player.gainLife(power, card);
                        }
                    }
                } // resolve
            }; // ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Wall of Reverence - ").append(player).append(" gains life equal to target creature's power.");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }
    }//endOfTurn_Wall_Of_Reverence()
	
	public static void endOfTurn_Predatory_Advantage()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player.getOpponent());
		CardList list = new CardList(playZone.getCards());
		list = list.getName("Predatory Advantage");
		for (int i = 0; i < list.size(); i++) {
            final Player controller = list.get(i).getController();
		    if((player == AllZone.HumanPlayer && Phase.PlayerCreatureSpellCount == 0) || (player == AllZone.ComputerPlayer && Phase.ComputerCreatureSpellCount == 0))
            {
                Ability abTrig = new Ability(list.get(i),"0") {
                    public void resolve()
                    {
                        CardFactoryUtil.makeToken("Lizard", "G 2 2 Lizard", controller, "G", new String[] {"Creature", "Lizard"}, 2, 2, new String[] {""});
                    }
                };
                abTrig.setTrigger(true);
                abTrig.setStackDescription("At the beginning of each opponent's end step, if that player didn't cast a creature spell this turn, put a 2/2 green Lizard creature token onto the battlefield.");

                AllZone.GameAction.playSpellAbility(abTrig);
            }
		}
	}
	
	public static void endOfTurn_Lighthouse_Chronologist() 
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, opponent);
		CardList list = new CardList(playZone.getCards());
		
		list = list.filter(new CardListFilter()
		{
			public boolean addCard(Card c)
			{
				return c.getName().equals("Lighthouse Chronologist") && c.getCounters(Counters.LEVEL) >= 7;
			}
		});
		
		Ability ability;
		for (int i = 0; i < list.size(); i++)
		{
			final Card card = list.get(i);
			ability = new Ability(list.get(i), "0")
			{
				public void resolve()
				{
					 AllZone.Phase.addExtraTurn(card.getController());
				}
			};
			
			StringBuilder sb = new StringBuilder();
			sb.append(card).append(" - ").append(card.getController()).append(" takes an extra turn.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}
	}

	public static void endOfTurn_Krovikan_Horror()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		horrorReturn(player);
		horrorReturn(opponent);
	}

    public static void horrorReturn(Player player)
    {
        // Find each Horror, peek at the card above it, if it's a creature return to hand
        CardList grave = new CardList(AllZone.getZone(Constant.Zone.Graveyard, player).getCards());
        if (grave.getName("Krovikan Horror").size() == 0) return;
        int i = 0;
        
        while (i+1 < grave.size()){
            Card c = grave.get(i);
            ArrayList<String> types = grave.get(i+1).getType();
            if (c.getName().equals("Krovikan Horror") && types.contains("Creature")) {
                
                if (player.isHuman()) {
                    String question = "Return Krovikan Horror to your hand?";
                    if (GameActionUtil.showYesNoDialog(c, question)) {
                        AllZone.GameAction.moveToHand(c);
                        grave.remove(c);
                    }
                    // increment counter to next occurance of Krovikan Horror
                    // if human decides not to return Krovikan Horror to hand
                    else
                        i++;
                }
                // player is computer
                else {
                    AllZone.GameAction.moveToHand(c);
                    grave.remove(c);
                }
            }
            else
                i++;
        }
    }
	//END ENDOFTURN CARDS

	public static void removeAttackedBlockedThisTurn() {
		// resets the status of attacked/blocked this turn
		Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList();
		list.addAll(play.getCards());
		list = list.getType("Creature");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(c.getCreatureAttackedThisCombat()) c.setCreatureAttackedThisCombat(false);
			if(c.getCreatureBlockedThisCombat()) c.setCreatureBlockedThisCombat(false);
			//do not reset setCreatureAttackedThisTurn(), this appears to be combat specific

			if(c.getCreatureGotBlockedThisCombat()) c.setCreatureGotBlockedThisCombat(false);
		}
		
		AllZone.GameInfo.setAssignedFirstStrikeDamageThisCombat(false);
		AllZone.GameInfo.setResolvedFirstStrikeDamageThisCombat(false);
	}
	
    public static boolean showYesNoDialog(Card c, String question) {
        AllZone.Display.setCard(c);
        StringBuilder title = new StringBuilder();
        title.append(c.getName()).append(" - Ability");
        
        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }
        
        int answer = JOptionPane.showConfirmDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION);
        
        if (answer == JOptionPane.YES_OPTION) return true;
        else return false;
    }
    
    public static void showInfoDialg(String message) {
    	JOptionPane.showMessageDialog(null, message);
    }
    
    public static boolean flipACoin(Player caller, Card source) {
    	String choice = "";
    	String choices[] = {"heads","tails"};
    	boolean flip = MyRandom.percentTrue(50);
    	if(caller.isHuman()) {
    		choice = (String) GuiUtils.getChoice(source.getName()+" - Call coin flip", choices);
    	}
    	else {
    		choice = choices[MyRandom.random.nextInt(2)];
    	}

    	if( (flip == true && choice.equals("heads")) || (flip == false && choice.equals("tails"))) {
    		JOptionPane.showMessageDialog(null, source.getName()+" - "+source.getController()+" wins flip.", source.getName(), JOptionPane.PLAIN_MESSAGE);
    		return true;
    	}
    	else{
    		JOptionPane.showMessageDialog(null, source.getName()+" - "+source.getController()+" loses flip.", source.getName(), JOptionPane.PLAIN_MESSAGE);
    		return false;
    	}
    }

	public static void executeLandfallEffects(Card c) {
		if(c.getName().equals("Lotus Cobra")) landfall_Lotus_Cobra(c);


	}
	
	private static boolean checkValakutCondition(Card valakutCard, Card mtn) {
		// Get a list of all mountains
		CardList mountainList = AllZoneUtil.getPlayerTypeInPlay(valakutCard.getController(),
				"Mountain");
		// Don't count the one that just entered the battlefield
		if (mountainList.contains(mtn))
			mountainList.remove(mtn);
		
		// Do not activate if at least 5 other mountains are not present.
		if (mountainList.size() < 5)
			return false;  
		else
			return true;
		
	}
	// Returns true if the routine found enough mountains to activate the effect
	// Returns false otherwise
	// This lets the calling routine break if a player has multiple Valakut in play
	public static boolean executeValakutEffect(final Card valakutCard, final Card mtn) {
		
		if (!checkValakutCondition(valakutCard, mtn))
			return false; // Tell the calling routine there aren't enough mountains, don't call again
		
        SpellAbility DamageTgt = new Spell(valakutCard) {

        	private static final long serialVersionUID = -7360567876931046530L;

			public boolean canPlayAI() {
                return getCreature().size() != 0 || AllZone.HumanPlayer.getLife() < 10;
            }
            
            public boolean canPlay() {
            	return true;
            }
            
            CardList getCreature() {
                //toughness of 3
                CardList list = CardFactoryUtil.AI_getHumanCreature(3, valakutCard, true);
                list = list.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        //only get 1/1 flyers or 2/1 or bigger creatures
                        return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
                    }
                });
                return list;
            }//getCreature()
 
            @Override
            public void chooseTargetAI() {
            	boolean targetHuman; 
            	// Get a list of all creatures Valakut could destroy
            	CardList list = getCreature();
            	
                CardList listValakut = list.filter(new CardListFilter() {
                	public boolean addCard(Card c) {
                		return c.getName().contains("Valakut, the Molten Pinnacle");
                	}
                });
            	
                int lifeThreshold = Math.max( 3 * listValakut.size(), 6); 
                if ( (AllZone.HumanPlayer.getLife() < lifeThreshold) || list.isEmpty()) { 
                	targetHuman = true;
                } else {
            		// Remove any creatures that have been targeted by other Valakuts
            		for (int ix = 0; ix < AllZone.Stack.size(); ix++) {
            			SpellAbility sa = AllZone.Stack.peek(ix);
            			if (sa.getSourceCard().getName().contains("Valakut, the Molten Pinnacle")) {
            				Card target = sa.getTargetCard();
            				if ((target != null) && list.contains(target)) {
            					list.remove(target);
            				}
            			}
            		}
            		if (list.isEmpty()) {
            			targetHuman = true;
            		} else {
            			targetHuman = false;
            		}
            	}
            	
            	
                if(targetHuman) setTargetPlayer(AllZone.HumanPlayer);
                else {
                    list.shuffle();
                    setTargetCard(list.get(0));
                }
            }//chooseTargetAI()
            
            @Override
            public void resolve() {
            	if (!checkValakutCondition(valakutCard, mtn))
            		return;
                if(getTargetCard() != null) {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(valakutCard, getTargetCard())) getTargetCard().addDamage(3,
                            valakutCard);
                } else {
                	getTargetPlayer().addDamage(3, valakutCard);
                }
            }//resolve()

        };
        DamageTgt.setManaCost("0");
        DamageTgt.setStackDescription("Valakut, the Molten Pinnacle deals 3 damage to target creature or player.");
        if (valakutCard.getController() == AllZone.HumanPlayer) {
        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreaturePlayer(DamageTgt, true, true));
        } else {
        	DamageTgt.chooseTargetAI();
            AllZone.Stack.addSimultaneousStackEntry(DamageTgt);

        }
        return true; // Tell the calling routine it's okay to call again if there are other Valakuts in play
	}
	
	private static boolean showLandfallDialog(Card c) {
		AllZone.Display.setCard(c);
		String[] choices = {"Yes", "No"};

		Object q = null;

		q = GuiUtils.getChoiceOptional("Use " + c.getName() + " Landfall?", choices);

		if(q == null || q.equals("No")) return false;
		else return true;
	}

	private static void landfall_Lotus_Cobra(final Card c) {
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				String color = "";

				Object o = GuiUtils.getChoice("Choose mana color", Constant.Color.onlyColors);
				color = Input_PayManaCostUtil.getShortColorString((String) o);

				Ability_Mana abMana = new Ability_Mana(c, "0", color) {
					private static final long serialVersionUID = -2182129023960978132L;
				};
				abMana.produceMana();
			}
		};
		

		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - add one mana of any color to your mana pool.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.addSimultaneousStackEntry(ability);
		}
		else{
			// todo: once AI has a mana pool he should choose add Ability and choose a mana as appropriate
		}
	}
	
	//not restricted to combat damage, not restricted to dealing damage to creatures/players
	public static void executeDamageDealingEffects(final Card source, int damage) {
		
		if (damage <= 0) return;
		
        if(source.getKeyword().contains("Lifelink")) source.getController().gainLife(damage, source);
        
	}
	
	//restricted to combat damage and dealing damage to creatures
	public static void executeCombatDamageToCreatureEffects(final Card source, final Card affected, int damage) {
		
		if (damage <= 0) return;
        
    	if(source.getName().equals("Mirri the Cursed") ) {
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

            AllZone.Stack.addSimultaneousStackEntry(ability2);

    	}


	}
	
	//not restricted to combat damage, restricted to dealing damage to creatures
	public static void executeDamageToCreatureEffects(final Card source, final Card affected, int damage) {
		
		if (damage <= 0) return;
		
		final Player player = affected.getController();
		
        if(affected.getName().equals("Stuffy Doll")) {
        	final Player opponent = affected.getOwner().getOpponent();
        	final int stuffyDamage = damage;
        	SpellAbility ability = new Ability(affected, "0") {
        		@Override
        		public void resolve() {
        			opponent.addDamage(stuffyDamage, affected);
        		}
        	};
        	StringBuilder sb = new StringBuilder();
            sb.append(affected.getName()+" - Deals ").append(stuffyDamage).append(" damage to ").append(opponent);
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }
        
        if(affected.getName().equals("Jackal Pup") || affected.getName().equals("Shinka Gatekeeper")) {
        	final int selfDamage = damage;
        	Ability ability = new Ability(affected, "0") {
        		@Override
        		public void resolve() {
        			player.addDamage(selfDamage, affected);
        		}
        	};
        	StringBuilder sb = new StringBuilder();
            sb.append(affected.getName()+" - Deals ").append(selfDamage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

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

            AllZone.Stack.addSimultaneousStackEntry(ability2);

        }
        
        if(affected.getKeyword().contains("Whenever CARDNAME is dealt damage, put a +1/+1 counter on it.")) {
        	Ability ability2 = new Ability(affected, "0") {
        		@Override
        		public void resolve() {
        			affected.addCounter(Counters.P1P1, 1);
        		}
        	}; // ability2
        	
        	StringBuilder sb2 = new StringBuilder();
        	sb2.append(affected.getName()).append(" - gets a +1/+1 counter");
        	ability2.setStackDescription(sb2.toString());
        	int amount = affected.getAmountOfKeyword("Whenever CARDNAME is dealt damage, put a +1/+1 counter on it.");
            
            for(int i=0 ; i < amount ; i++)
                AllZone.Stack.addSimultaneousStackEntry(ability2);

        }
        
        if(affected.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
	        final Ability ability = new Ability(source, "0") {
	        	@Override
	        	public void resolve() { AllZone.GameAction.destroy(affected); }
	        };
	        
	        final Ability ability2 = new Ability(source, "0") {
	        	@Override
	        	public void resolve() { AllZone.GameAction.destroyNoRegeneration(affected); }
	        };
	    
	        StringBuilder sb = new StringBuilder();
	    	sb.append(affected).append(" - destroy");
	    	ability.setStackDescription(sb.toString());
	    	ability2.setStackDescription(sb.toString());
	    	
	    	if(affected.getKeyword().contains("When CARDNAME is dealt damage, destroy it. It can't be regenerated.")) {
	        	int amount = affected.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it. It can't be regenerated.");
	            
	            for(int i=0 ; i < amount ; i++)
                    AllZone.Stack.addSimultaneousStackEntry(ability2);

	    	}
	    	int amount = affected.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it.");
            
            for(int i=1 ; i < amount ; i++)
                AllZone.Stack.addSimultaneousStackEntry(ability); AllZone.Stack.addSimultaneousStackEntry(ability);
        }
        
        if(source.getKeyword().contains("Deathtouch") && affected.isCreature()) AllZone.GameAction.destroy(affected);


	}
	
	public static void executeSwordOfLightAndShadowEffects(final Card source) {
		final Card src = source;
		final Ability ability = new Ability(src, "0") {
			@Override
			public void resolve() {
				Card target = getTargetCard();
				if(target != null){
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, src.getController());
                    if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, src.getController());
                        AllZone.GameAction.moveTo(hand, getTargetCard());
                    }
				}
				
				src.getController().gainLife(3, source);
			}
		}; // ability

		Command res = new Command() {
             private static final long serialVersionUID = -7433708170033536384L;
             
             public void execute() {
                 PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, src.getController());
                 CardList list = new CardList(grave.getCards());
                 
                 list = list.filter(new CardListFilter() {
                     public boolean addCard(Card crd) {
                         return crd.isCreature();
                     }
                 });
                 // list = list.getType("Creature");
                 
                 if(list.isEmpty()) {
                     AllZone.Stack.addSimultaneousStackEntry(ability);

                	 return;
                 }
                 
                 if(src.getController().equals(AllZone.HumanPlayer)) {
                     Object o = GuiUtils.getChoiceOptional("Select target card", list.toArray());
                     if(o != null) {
                         ability.setTargetCard((Card) o);
                         AllZone.Stack.addSimultaneousStackEntry(ability);

                     }
                 }//if
                 else//computer
                 {
                     Card best = CardFactoryUtil.AI_getBestCreature(list);
                     ability.setTargetCard(best);
                     AllZone.Stack.addSimultaneousStackEntry(ability);

                 }
             }//execute()
        };//Command
        
        StringBuilder sb = new StringBuilder();
        sb.append("Sword of Light and Shadow - You gain 3 life and you may return ");
        sb.append("up to one target creature card from your graveyard to your hand");
        ability.setStackDescription(sb.toString());
		
		res.execute();
	}
	
    //this is for cards like Sengir Vampire
    public static void executeVampiricEffects(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++) {
            if(AllZone.GameAction.isCardInPlay(c)
                    && a.get(i).toString().startsWith(
                            "Whenever a creature dealt damage by CARDNAME this turn is put into a graveyard, put")) {
                final Card thisCard = c;
                final String kw = a.get(i).toString();
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                    	Counters counter = Counters.P1P1;
                    	if(kw.contains("+2/+2")) counter = Counters.P2P2;
                        if(AllZone.GameAction.isCardInPlay(thisCard)) thisCard.addCounter(counter, 1);
                    }
                }; // ability2
                
                StringBuilder sb = new StringBuilder();
                sb.append(c.getName());
                if(kw.contains("+2/+2")) {
                	sb.append(" - gets a +2/+2 counter");
                }
                else {
                	sb.append(" - gets a +1/+1 counter");
                }
                ability2.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability2);

            }
        }
    }

    //not restricted to just combat damage, restricted to players
    public static void executeDamageToPlayerEffects(final Player player, final Card c, final int damage)
    {
    	if (damage <= 0) return;
    	
    	Object[] DealsDamage_Whenever_Parameters = new Object[3];
    	DealsDamage_Whenever_Parameters[2] = c;
    	DealsDamage_Whenever_Parameters[0] = player;
    	AllZone.GameAction.checkWheneverKeyword(c, "DealsDamage/Player", DealsDamage_Whenever_Parameters);
    	if(!c.getController().equals(player))
    		AllZone.GameAction.checkWheneverKeyword(c, "DealsDamage/Opponent", DealsDamage_Whenever_Parameters);
    	
		CardList playerPerms = AllZoneUtil.getPlayerCardsInPlay(player);
		
		/*
		 * Backfire - Whenever enchanted creature deals damage to you, Backfire
		 * deals that much damage to that creature's controller.
		 */
		if(c.isEnchanted()) {
			final String auraName = "Backfire";
			
	        CardList auras = new CardList(c.getEnchantedBy().toArray());
			auras = auras.getName(auraName);
	        
			if(auras.size() > 0) {
				for(Card aura:auras) {
					final Card source = aura;
					Ability ability = new Ability(source, "0") {
						@Override
						public void resolve() {
							c.getController().addDamage(damage, source);
						}
					};
					
					StringBuilder sb = new StringBuilder();
					sb.append(source.getName()).append(" - deals ").append(damage);
					sb.append(" damage to ").append(c.getController());
					ability.setStackDescription(sb.toString());
					
					//Backfire triggers only if its controller is damaged
					if(aura.getController().isPlayer(player))
                        AllZone.Stack.addSimultaneousStackEntry(ability);

				}
			}//auras > 0
		}//end c.isEnchanted()
		

		if (playerPerms.getName("Farsight Mask").size() > 0)    		
		{
			final Card c1 = c;
			CardList l = playerPerms.filter(new CardListFilter()
			{
				public boolean addCard(Card crd)
				{
					return crd.getName().equals("Farsight Mask") && crd.isUntapped() && !c1.getController().equals(crd.getController());
				}
			});
			for (Card crd:l)
				playerDamage_Farsight_Mask(player, c, crd);
		}
		
    	if(AllZoneUtil.isCardInPlay("Lich", player)) {
    		CardList lichs = playerPerms.getName("Lich");
			for(Card crd:lichs) {
				final Card lich = crd;
				SpellAbility ability = new Ability(lich, "0") {
					public void resolve() {
						for(int i = 0; i < damage; i++) {
			    			CardList nonTokens = AllZoneUtil.getPlayerCardsInPlay(player);
			    			nonTokens = nonTokens.filter(AllZoneUtil.nonToken);
			    			if(nonTokens.size() == 0) {
			    				player.altLoseConditionMet("Lich");
			    			}
			    			else player.sacrificePermanent("Select a permanent to sacrifice", nonTokens);
			    		}
					}
				};
				
				StringBuilder sb = new StringBuilder();
				sb.append(lich.getName()).append(" - ").append(lich.getController());
				sb.append(" sacrifices ").append(damage).append(" nontoken Permanents.");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
    	}
		
    	if(c.getKeyword().contains("Whenever this creature deals damage to a player, that player gets a poison counter."))
			playerCombatDamage_PoisonCounter(c, 1);
    
    	if(c.getName().equals("Whirling Dervish") || c.getName().equals("Dunerider Outlaw")) 
			playerCombatDamage_Whirling_Dervish(c);
    	
    	if (player.isPlayer(AllZone.HumanPlayer)) c.setDealtDmgToHumanThisTurn(true);
    	if (player.isPlayer(AllZone.ComputerPlayer)) c.setDealtDmgToComputerThisTurn(true);
    }
    
    
    //restricted to combat damage, restricted to players
	public static void executeCombatDamageToPlayerEffects(final Player player, final Card c, final int damage) {
		
		if (damage <= 0) return;
		
		// Whenever Keyword
    	Object[] DealsDamage_Whenever_Parameters = new Object[3];
    	DealsDamage_Whenever_Parameters[0] = c.getController().getOpponent();
    	DealsDamage_Whenever_Parameters[2] = c;
    	AllZone.GameAction.checkWheneverKeyword(c, "DealsCombatDamage/Opponent", DealsDamage_Whenever_Parameters);
    	
    	if(c.isCreature() && AllZoneUtil.isCardInPlay("Contested War Zone", player)) {
    		CardList zones = AllZoneUtil.getPlayerCardsInPlay(player, "Contested War Zone");
    		for(final Card zone:zones) {
    			Ability ability = new Ability(zone, "0") {
    				@Override
    				public void resolve() {
    					if(AllZone.GameAction.isCardInPlay(zone)) {
        					AllZone.GameAction.changeController(new CardList(zone), zone.getController(), c.getController());
    					}
    				}
    			};
    			ability.setStackDescription(zone+" - "+c.getController()+" gains control of "+zone);

                AllZone.Stack.addSimultaneousStackEntry(ability);

    		}
    	}

		if (c.hasStartOfKeyword("Poisonous"))
		{
        	int KeywordPosition = c.getKeywordPosition("Poisonous");
        	String parse = c.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(" ");
    		final int poison = Integer.parseInt(k[1]);
			final Card crd = c;
			
			Ability ability = new Ability(c, "0")
			{
				public void resolve()
				{
					final Player player = crd.getController();
					final Player opponent = player.getOpponent();

					if(opponent.equals(AllZone.HumanPlayer)) 
						AllZone.HumanPlayer.addPoisonCounters(poison);
					else
						AllZone.ComputerPlayer.addPoisonCounters(poison);
				}
			};

			StringBuilder sb = new StringBuilder();
			sb.append(c);
			sb.append(" - Poisonous: ");
			sb.append(c.getController().getOpponent());
			sb.append(" gets ");
			sb.append(poison);
			sb.append(" poison counters.");

			ability.setStackDescription(sb.toString());
			ArrayList<String> keywords = c.getKeyword();

			for (int i=0;i<keywords.size();i++)
			{
				if (keywords.get(i).startsWith("Poisonous"))
					AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
		
		if(CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike") > 0 && c.getNetAttack() > 0) {
			for(int k = 0; k < CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike"); k++) {
				playerCombatDamage_lose_halflife_up(c);
			}
		}
		
        if(c.isEquipped()) {
        	ArrayList<Card> equips = c.getEquippedBy();
        	for(Card equip:equips) {
        		if(equip.getName().equals("Sword of Light and Shadow")) {
        			GameActionUtil.executeSwordOfLightAndShadowEffects(equip);
        		}
        	}
        }//isEquipped

		
		if(c.getName().equals("Scalpelexis")) playerCombatDamage_Scalpelexis(c);
		else if(c.getName().equals("Augury Adept")) playerCombatDamage_Augury_Adept(c);
		else if(c.getName().equals("Spawnwrithe")) playerCombatDamage_Spawnwrithe(c);
		else if(c.getName().equals("Rootwater Thief")) playerCombatDamage_Rootwater_Thief(c);
		else if(c.getName().equals("Treva, the Renewer")) playerCombatDamage_Treva(c);
		else if(c.getName().equals("Rith, the Awakener")) playerCombatDamage_Rith(c);
		
		else if(c.isEnchantedBy("Celestial Mantle")) execute_Celestial_Mantle( c);

		//Unused variable
		//c.setDealtCombatDmgToOppThisTurn(true); 

	}//executeCombatDamageToPlayerEffects
	
	private static void execute_Celestial_Mantle(final Card enchanted) {
		ArrayList<Card> auras = enchanted.getEnchantedBy();
		for(final Card aura:auras) {
			if(aura.getName().equals("Celestial Mantle")) {
				Ability doubleLife = new Ability(aura, "0") {
					public void resolve() {
						int life = enchanted.getController().getLife();
						enchanted.getController().setLife(life * 2, aura);
					}
				};
				doubleLife.setStackDescription(aura.getName()+" - "+enchanted.getController()+" doubles his or her life total.");

                AllZone.Stack.addSimultaneousStackEntry(doubleLife);

			}
		}
	}

	private static void playerCombatDamage_PoisonCounter(Card c, int n) {
		final Player opponent = c.getController().getOpponent();
		opponent.addPoisonCounters(n);
	}
	
	private static void playerDamage_Farsight_Mask(final Player player, final Card c, final Card crd)
	{
		Ability ability = new Ability(crd,"0")
		{
			public void resolve()
			{
				if (crd.isUntapped())
				{
					player.mayDrawCard();
				}
			}
		};
		ability.setStackDescription("Farsight Mask - You may draw a card.");

		AllZone.Stack.addSimultaneousStackEntry(ability);

	}


	private static void playerCombatDamage_Rootwater_Thief(Card c) {
		SpellAbility[] sa = c.getSpellAbility();
		if(c.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[2]); //because sa[1] is the kpump u: flying
		else ComputerUtil.playNoStack(sa[2]);


	}

	private static void playerCombatDamage_Treva(Card c) {
		SpellAbility[] sa = c.getSpellAbility();
		if(c.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[1]);
		else ComputerUtil.playNoStack(sa[1]);

	}

	private static void playerCombatDamage_Rith(Card c) {
		SpellAbility[] sa = c.getSpellAbility();
		if(c.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[1]);
		else ComputerUtil.playNoStack(sa[1]);
	}
	
	private static void playerCombatDamage_Whirling_Dervish(Card c) {
		final int power = c.getNetAttack();
		final Card card = c;

		if(power > 0) {
			final Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					card.addCounter(Counters.P1P1, 1);
				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - gets a +1/+1 counter.");
			ability2.setStackDescription(sb.toString());

			Command dealtDmg = new Command() {
				private static final long serialVersionUID = 2200679209414069339L;

				public void execute() {
					AllZone.Stack.addSimultaneousStackEntry(ability2);

				}
			};
			AllZone.EndOfTurn.addAt(dealtDmg);

		} // if
	}

	private static void playerCombatDamage_lose_halflife_up(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();
		final Card F_card = c;
		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					int x = 0;
					int y = 0;
					if(player == AllZone.HumanPlayer) {
						y = (AllZone.ComputerPlayer.getLife() % 2);
						if(!(y == 0)) y = 1;
						else y = 0;

						x = (AllZone.ComputerPlayer.getLife() / 2) + y;
					} else {
						y = (AllZone.HumanPlayer.getLife() % 2);
						if(!(y == 0)) y = 1;
						else y = 0;

						x = (AllZone.HumanPlayer.getLife() / 2) + y;
					}
					opponent.loseLife(x, F_card);

				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(opponent);
			sb.append(" loses half his or her life, rounded up.");
			ability2.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability2);

		}
	}

	private static void playerCombatDamage_Scalpelexis(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();

		if(c.getNetAttack() > 0) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {

					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opponent);

					CardList libList = new CardList(lib.getCards());
					int count = 0;
					int broken = 0;
					for(int i = 0; i < libList.size(); i = i + 4) {
						Card c1 = null;
						Card c2 = null;
						Card c3 = null;
						Card c4 = null;
						if(i < libList.size()) c1 = libList.get(i);
						else broken = 1;
						if(i + 1 < libList.size()) c2 = libList.get(i + 1);
						else broken = 1;
						if(i + 2 < libList.size()) c3 = libList.get(i + 2);
						else broken = 1;
						if(i + 3 < libList.size()) c4 = libList.get(i + 3);
						else broken = 1;
						if(broken == 0) {
							if((c1.getName().contains(c2.getName()) || c1.getName().contains(c3.getName())
									|| c1.getName().contains(c4.getName()) || c2.getName().contains(c3.getName())
									|| c2.getName().contains(c4.getName()) || c3.getName().contains(c4.getName()))) {
								count = count + 1;
							} else {
								broken = 1;
							}
						}

					}
					count = (count * 4) + 4;
					int max = count;
					if(libList.size() < count) max = libList.size();

					for(int j = 0; j < max; j++) {
						Card c = libList.get(j);
						AllZone.GameAction.exile(c);
					}
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Scalpelexis - ").append(opponent);
			sb.append(" exiles the top four cards of his or her library. ");
			sb.append("If two or more of those cards have the same name, repeat this process.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}
	}

	private static void playerCombatDamage_Spawnwrithe(Card c) {
		final Player player = c.getController();
		final Card crd = c;

		Ability ability2 = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardList cl = CardFactoryUtil.makeToken("Spawnwrithe", "", crd.getController(), "2 G", new String[] {
						"Creature", "Elemental"}, 2, 2, new String[] {"Trample"});

				for(Card c:cl) {
					c.setText("Whenever Spawnwrithe deals combat damage to a player, put a token that's a copy of Spawnwrithe onto the battlefield.");
					c.setCopiedToken(true);
				}
			}
		};// ability2
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - ").append(player).append(" puts copy onto the battlefield.");
		ability2.setStackDescription(sb.toString());

        AllZone.Stack.addSimultaneousStackEntry(ability2);

	}
	
	private static void playerCombatDamage_Augury_Adept(Card c) {
		final Player[] player = new Player[1];
		final Card crd = c;

		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(crd, "0") {
				@Override
				public void resolve() {
					player[0] = crd.getController();
					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player[0]);

					if(lib.size() > 0) {
						CardList cl = new CardList();
						cl.add(lib.get(0));
						GuiUtils.getChoiceOptional("Top card", cl.toArray());
					};
                    if(lib.size() == 0)
                        return;
					Card top = lib.get(0);
					player[0].gainLife(CardUtil.getConvertedManaCost(top.getManaCost()), crd);
					AllZone.GameAction.moveToHand(top);
				}
			};// ability2

			player[0] = c.getController();
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player[0]);
			sb.append(" reveals the top card of his library and put that card into his hand. ");
			sb.append("He gain life equal to its converted mana cost.");
			ability2.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability2);

		}
	}

	private static void upkeep_AI_Aluren() {
		CardList alurens = AllZoneUtil.getCardsInPlay("Aluren");
		if (alurens.size() == 0)
			return;
		
		CardList inHand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
		inHand = inHand.getType("Creature");
		CardList playable = new CardList();

		for(Card c : inHand)
			if(CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) 
				playable.add(c);

		for(Card c : playable)
			AllZone.GameAction.playSpellAbilityForFree(c.getSpellPermanent());
	}

		
	private static void upkeep_Dance_of_the_Dead() {
		final Player player = AllZone.Phase.getPlayerTurn();
		
		CardList dances = AllZoneUtil.getPlayerCardsInPlay(player, "Dance of the Dead");
		for(Card dance:dances) {
			final Card source = dance;
			final ArrayList<Card> list = source.getEnchanting();
			final Card creature = list.get(0);
			if(creature.isTapped()) {
				Ability vaultChoice = new Ability(source, "0"){
				
					@Override
					public void resolve(){
						if(GameActionUtil.showYesNoDialog(source, "Untap "+creature.getName()+"?")) {
							//prompt for pay mana cost, then untap
							final SpellAbility untap = new Ability(source, "1 B") {
								@Override
								public void resolve() {
									creature.untap();
								}
							};//Ability
							
							StringBuilder sb = new StringBuilder();
							sb.append("Untap ").append(creature);
							untap.setStackDescription(sb.toString());
							
							AllZone.GameAction.playSpellAbility(untap);
						}
					}
				};
				vaultChoice.setStackDescription(source.getName()+" - Untap creature during Upkeep?");

                AllZone.Stack.addSimultaneousStackEntry(vaultChoice);

			}
		}
	}
	
	private static void upkeep_Mana_Crypt() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList crypts = AllZoneUtil.getPlayerCardsInPlay(player, "Mana Crypt");
		for(final Card crypt:crypts) {
			SpellAbility ab = new Ability(crypt, "0"){

				@Override
				public void resolve(){
					if(flipACoin(crypt.getController().getOpponent(), crypt)) {
						//do nothing
					}
					else {
						crypt.getController().addDamage(3, crypt);
					}
				}
			};
			ab.setStackDescription(crypt.getName()+" - Flip a coin.");

            AllZone.Stack.addSimultaneousStackEntry(ab);

		}
	}//upkeep_Mana_Crypt
	
	private static void upkeep_Farmstead() {
		final String auraName = "Farmstead";
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.isEnchantedBy(auraName);
			}
		});

		for(final Card land:list) {
			CardList auras = new CardList(land.getEnchantedBy().toArray());
			auras = auras.getName(auraName);
			for(int i = 0; i < auras.size(); i++) {
				Ability ability = new Ability(land, "0") {
					@Override
					public void resolve() {
						if(GameActionUtil.showYesNoDialog(land, "Pay W W, and gain 1 life?")) {
							//prompt for pay mana cost
							final SpellAbility gain = new Ability(land, "W W") {
								@Override
								public void resolve() {
									land.getController().gainLife(1, land);
								}
							};//Ability

							StringBuilder sb = new StringBuilder();
							sb.append(land).append(" - gain 1 life.");
							gain.setStackDescription(sb.toString());

							AllZone.GameAction.playSpellAbility(gain);
						}
					}
				};

				StringBuilder sb = new StringBuilder();
				sb.append(land.getName()).append(" -  activate Life gain ability?");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}//upkeep_Farmstead()
	
	private static void upkeep_Heartmender() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Heartmender");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {

				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

						CardList creats = new CardList(play.getCards());
						creats = creats.filter(new CardListFilter() {

							public boolean addCard(Card c) {
								return c.getCounters(Counters.M1M1) > 0;
							}

						});

						for(int j = 0; j < creats.size(); j++) {
							Card c = creats.get(j);
							if(c.getCounters(Counters.M1M1) > 0) c.subtractCounter(Counters.M1M1,1);
						}

					}

				};// Ability
				ability.setStackDescription("Heartmender - Remove a -1/-1 counter from each creature you control.");

                AllZone.Stack.addSimultaneousStackEntry(ability);

			} // for
		} // if creatures > 0
	}//upkeep_Heartmender
	
    /////////////////////////
    // Start of Kinship cards
    /////////////////////////
    
    
    private static void upkeep_Ink_Dissolver() {
        final Player player = AllZone.Phase.getPlayerTurn();
        final Player opponent = player.getOpponent();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Ink Dissolver");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Merfolk", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantToMillOpponent = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent puts the top 3 ");
                            question.append("cards of his library into his graveyard?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantToMillOpponent = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToMillOpponent = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantToMillOpponent)
                        opponent.mill(3);
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Ink Dissolver - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Ink_Dissolver()
    
    
    private static void upkeep_Kithkin_Zephyrnaut() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Kithkin Zephyrnaut");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Kithkin", "Soldier" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantKithkinBuff = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card, Kithkin Zephyrnaut gets +2/+2 and ");
                            question.append("gains flying and vigilance until end of turn?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantKithkinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantKithkinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantKithkinBuff) {
                        k.addTempAttackBoost(2);
                        k.addTempDefenseBoost(2);
                        k.addExtrinsicKeyword("Flying");
                        k.addExtrinsicKeyword("Vigilance");
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 213717084767008154L;

                            public void execute() {
                                k.addTempAttackBoost(-2);
                                k.addTempDefenseBoost(-2);
                                k.removeExtrinsicKeyword("Flying");
                                k.removeExtrinsicKeyword("Vigilance");
                            }
                        };
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Kithkin Zephyrnaut - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Kithkin_Zephyrnaut()
    
    
    private static void upkeep_Leaf_Crowned_Elder() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Leaf-Crowned Elder");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Treefolk", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantToPlayCard = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal and play this card without paying its mana cost?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantToPlayCard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToPlayCard = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantToPlayCard) {
                        if (player.isHuman()) {
                            Card c = library.get(0);
                            AllZone.GameAction.playCardNoCost(c);
                        }
                        // player isComputer()
                        else {
                            Card c = library.get(0);
                            ArrayList<SpellAbility> choices = c.getBasicSpells();
                            
                            for (SpellAbility sa:choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa);
                                    break;
                                }
                            }
                        }
                    }// wantToPlayCard
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Leaf-Crowned Elder - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Leaf_Crowned_Elder()
    
    
    private static void upkeep_Mudbutton_Clanger() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Mudbutton Clanger");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Goblin", "Warrior" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantGoblinBuff = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Mudbutton Clanger gets +1/+1 until end of turn?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantGoblinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantGoblinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantGoblinBuff) {
                        k.addTempAttackBoost(1);
                        k.addTempDefenseBoost(1);
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -103560515951630426L;

                            public void execute() {
                                k.addTempAttackBoost(-1);
                                k.addTempDefenseBoost(-1);
                            }
                        };
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Mudbutton Clanger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Mudbutton_Clanger()
    
    
    private static void upkeep_Nightshade_Schemers() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Nightshade Schemers");
        final Player opponent = player.getOpponent();
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Faerie", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantOpponentLoseLife = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent loses 2 life?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantOpponentLoseLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantOpponentLoseLife = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantOpponentLoseLife)
                        opponent.loseLife(2, k);
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Nightshade Schemers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Nightshade_Schemers()
    
    
    private static void upkeep_Pyroclast_Consul() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Pyroclast Consul");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Elemental", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantDamageCreatures = false;
                    String[] smallCreatures = { "Creature.toughnessLE2" };
                    
                    CardList humanCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                    humanCreatures = humanCreatures.getValidCards(smallCreatures,k.getController(),k);
                    humanCreatures = humanCreatures.getNotKeyword("Indestructible");
                    
                    CardList computerCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
                    computerCreatures = computerCreatures.getValidCards(smallCreatures,k.getController(),k);
                    computerCreatures = computerCreatures.getNotKeyword("Indestructible");
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Pyroclast Consul deals 2 damage to each creature?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantDamageCreatures = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (humanCreatures.size() > computerCreatures.size()) {
                                String title = "Computer reveals";
                                revealTopCard(title);
                                wantDamageCreatures = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantDamageCreatures) {
                        CardList allCreatures = AllZoneUtil.getCreaturesInPlay();
                        for (final Card crd : allCreatures) {
                                crd.addDamage(2, k);
                        }
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Pyroclast Consul - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Pyroclast_Consul()
    
    
    private static void upkeep_Sensation_Gorger() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Sensation Gorger");
        final Player opponent = player.getOpponent();
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Goblin", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantDiscardThenDraw = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have both players discard their hand and draw 4 cards?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantDiscardThenDraw = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (library.size() > 4 && hand.size() < 2) {
                                String title = "Computer reveals";
                                revealTopCard(title);
                                wantDiscardThenDraw = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantDiscardThenDraw) {
                        player.discardHand(this);
                        opponent.discardHand(this);
                        
                        player.drawCards(4);
                        opponent.drawCards(4);
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Sensation Gorger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Sensation_Gorger()
    
    
    private static void upkeep_Squeaking_Pie_Grubfellows() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Squeaking Pie Grubfellows");
        final Player opponent = player.getOpponent();
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Goblin", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantOpponentDiscard = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have opponent discard a card?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantOpponentDiscard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantOpponentDiscard = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantOpponentDiscard) {
                        opponent.discard(this);
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Squeaking Pie Grubfellows - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Squeaking_Pie_Grubfellows()
    
    
    private static void upkeep_Wandering_Graybeard() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Wandering Graybeard");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Giant", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantGainLife = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and gain 4 life?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantGainLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantGainLife = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantGainLife)
                        player.gainLife(4, k);
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Wandering Graybeard - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Wandering_Graybeard()
    
    
    private static void upkeep_Waterspout_Weavers() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Waterspout Weavers");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.
        
        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Merfolk", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantMerfolkBuff = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and each creature you ");
                            question.append("control gains flying until end of turn?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantMerfolkBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantMerfolkBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    
                    if (wantMerfolkBuff) {
                        CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                        for (int i = 0; i < creatures.size(); i ++) {
                            if (!creatures.get(i).getKeyword().contains("Flying")) {
                                creatures.get(i).addExtrinsicKeyword("Flying");
                            }
                        }
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1978446996943583910L;

                            public void execute() {
                                CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                                for (int i = 0; i < creatures.size(); i ++) {
                                    if (creatures.get(i).getKeyword().contains("Flying")) {
                                        creatures.get(i).removeExtrinsicKeyword("Flying");
                                    }
                                }
                            }
                        };
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Waterspout Weavers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Waterspout_Weavers()
    
    
    private static void upkeep_Winnower_Patrol() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Winnower Patrol");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Elf", "Warrior" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };
        
        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantCounter = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a +1/+1 counter on Winnower Patrol?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantCounter = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantCounter = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantCounter)
                        k.addCounter(Counters.P1P1, 1);
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Winnower Patrol - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Winnower_Patrol()
    
    
    private static void upkeep_Wolf_Skull_Shaman() {
        final Player player = AllZone.Phase.getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Wolf-Skull Shaman");
        
        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;
        
        final String[] shareTypes = { "Elf", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card peek[] = { null };

        for (final Card k : kinship){
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;
                    
                    peek[0] = library.get(0);
                    boolean wantToken = false;
                    
                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!
                    
                    if (peek[0].isValidCard(shareTypes,k.getController(),k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a 2/2 green Wolf creature token onto the battlefield?");
                            if (showYesNoDialog(k, question.toString())) {
                                wantToken = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToken = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantToken)
                        CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", k.getController(), "G", 
                            new String[] {"Creature", "Wolf"}, 2, 2, new String[] {""});
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Wolf-Skull Shaman - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Wolf_Skull_Shaman()
    
    
    ///////////////////////
    // End of Kinship cards
    ///////////////////////


	private static void upkeep_Dark_Confidant() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Dark Confidant");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			if(library.size() <= 0) {
				return;
			}
			// System.out.println("top of deck: " + library.get(i).getName());
			final int convertedManaCost = CardUtil.getConvertedManaCost(library.get(i).getManaCost());
			String cardName = library.get(i).getName();
			final Card F_card = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					// todo: this is bad, the card that is revealed could be different than the one we get 
					PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
					player.loseLife(convertedManaCost, F_card);

					AllZone.GameAction.moveToHand(library.get(0));
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Dark Confidant - ").append(player).append(" loses ").append(convertedManaCost);
			sb.append(" life and draws top card (").append(cardName).append(").");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// for
	}// upkeep_Dark_Confidant()

	private static void upkeep_Oversold_Cemetery() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList cemeteryList = AllZoneUtil.getPlayerCardsInPlay(player, "Oversold Cemetery");
		if (cemeteryList.isEmpty())
			return;
		
		CardList graveyardCreatures = AllZoneUtil.getPlayerTypeInGraveyard(player, "Creature");

		if(graveyardCreatures.size() >= 4) {
			for(int i = 0; i < cemeteryList.size(); i++) {
				Ability ability = new Ability(cemeteryList.get(0), "0") {
					@Override
					public void resolve() {
						CardList graveyardCreatures = AllZoneUtil.getPlayerTypeInGraveyard(player, "Creature");

						if(graveyardCreatures.size() >= 4) {
							if(player.equals(AllZone.HumanPlayer)) {
								Object o = GuiUtils.getChoiceOptional("Pick a creature to return to hand",
										graveyardCreatures.toArray());
								if(o != null) {
									Card card = (Card) o;

									AllZone.GameAction.moveToHand(card);
								}
							} 
							else if(player.equals(AllZone.ComputerPlayer)) {
								Card card = graveyardCreatures.get(0);
								AllZone.GameAction.moveToHand(card);
							}
						}
					}
				};// Ability
				ability.setStackDescription("Oversold Cemetary returns creature from the graveyard to its owner's hand.");

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}//Oversold Cemetery

	/*
    private static void upkeep_Nether_Spirit() {
        final Player player = AllZone.Phase.getPlayerTurn();
        final PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

        CardList all = new CardList(graveyard.getCards());
        all = all.getType("Creature");

        CardList list = new CardList(graveyard.getCards());
        list = list.getName("Nether Spirit");

        if (all.size() == 1 && list.size() == 1) {
            final Card nether = list.get(0);
            Ability ability = new Ability(list.get(0), "0") {
                @Override
                public void resolve() {
                    AllZone.GameAction.moveToPlay(nether);
                }
            };

            boolean returnNether = false;

            if (player.equals(AllZone.HumanPlayer)) {
                String question = "Return Nether Spirit to the battlefield?";
                if (GameActionUtil.showYesNoDialog(nether, question)) {
                    returnNether = true;
                }
            }

            if (player.equals(AllZone.ComputerPlayer) || returnNether) {
                StringBuilder sb = new StringBuilder();
                sb.append("Nether Spirit - ").append(player).append(" returns Nether Spirit to the battlefield");
                ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

            }
        } //if
    }//nether spirit
    */

	public static void upkeep_Suspend() {
		Player player = AllZone.Phase.getPlayerTurn();

		PlayerZone exile = AllZone.getZone(Constant.Zone.Exile, player);
		CardList list = new CardList();
		list.addAll(exile.getCards());
		//list = list.getType("Creature");
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.hasSuspend();
			}
		});

		if (list.size() == 0) return;
		
		for(final Card c : list){
			int counters = c.getCounters(Counters.TIME);
			if (counters > 0) c.subtractCounter(Counters.TIME, 1);
		}
	}//suspend	
	
	private static void upkeep_Vanishing() {

		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		CardList list = new CardList(playZone.getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CardFactory.hasKeyword(c, "Vanishing") != -1;
			}
		});
		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						card.subtractCounter(Counters.TIME, 1);
					}
				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - Vanishing - remove a time counter from it. ");
				sb.append("When the last is removed, sacrifice it.)");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}
	
	private static void upkeep_Fading() {

		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		CardList list = new CardList(playZone.getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CardFactory.hasKeyword(c, "Fading") != -1;
			}
		});
		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						int fadeCounters = card.getCounters(Counters.FADE);
						if (fadeCounters <= 0)
							AllZone.GameAction.sacrifice(card);
						else
							card.subtractCounter(Counters.FADE, 1);
					}
				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - Fading - remove a fade counter from it. ");
				sb.append("If you can't, sacrifice it.)");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}
	
    private static void upkeep_Oath_of_Druids() {
        CardList oathList = AllZoneUtil.getCardsInPlay("Oath of Druids");
        if (oathList.isEmpty())
            return;
        
        final Player player = AllZone.Phase.getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                Ability ability = new Ability(oath, "0") {
                   @Override
                   public void resolve() {
                       CardList libraryList = AllZoneUtil.getPlayerCardsInLibrary(player);
                       PlayerZone battlefield = AllZone.getZone(Constant.Zone.Battlefield, player);
                       boolean oathFlag = true;

                       if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
                           if (player == AllZone.HumanPlayer){
                               StringBuilder question = new StringBuilder();
                               question.append("Reveal cards from the top of your library and place ");
                               question.append("the first creature revealed onto the battlefield?");
                               if (!GameActionUtil.showYesNoDialog(oath, question.toString())) {
                                   oathFlag = false;
                               }
                           }
                           else {    // if player == Computer
                               CardList creaturesInLibrary = AllZoneUtil.getPlayerTypeInLibrary(player, "Creature");
                               CardList creaturesInBattlefield = AllZoneUtil.getPlayerTypeInPlay(player, "Creature");

                               // if there are at least 3 creatures in library, or none in play with one in library, oath
                               if (creaturesInLibrary.size() > 2 
                                       || (creaturesInBattlefield.size() == 0 && creaturesInLibrary.size() > 0) )
                                   oathFlag = true;
                               else
                                   oathFlag = false;
                           }
                           
                           if (oathFlag) {
                               CardList cardsToReveal = new CardList();
                               int max = libraryList.size();
                               for (int i = 0; i < max; i++) {
                                   Card c = libraryList.get(i);
                                   cardsToReveal.add(c);
                                   if (c.getType().contains("Creature")) {
                                       AllZone.GameAction.moveTo(battlefield, c);
                                       break;   
                                   } 
                                   else {
                                       AllZone.GameAction.moveToGraveyard(c);
                                   }
                               }// for loop
                               if (cardsToReveal.size() > 0)
                                   GuiUtils.getChoice("Revealed cards", cardsToReveal.toArray());
                           }
                       }
                   }
                };// Ability
                
                StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, that player chooses target player ");
                sb.append("who controls more creatures than he or she does and is his or her opponent. The ");
                sb.append("first player may reveal cards from the top of his or her library until he or she ");
                sb.append("reveals a creature card. If he or she does, that player puts that card onto the ");
                sb.append("battlefield and all other cards revealed this way into his or her graveyard.");
                ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

            }
        }
    }// upkeep_Oath of Druids()
	
	private static void upkeep_Oath_of_Ghouls() {	
		CardList oathList = AllZoneUtil.getCardsInPlay("Oath of Ghouls");
		if (oathList.isEmpty())
			return;
		
		final Player player = AllZone.Phase.getPlayerTurn();	
		
		if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0)
		{
			for(int i = 0; i < oathList.size(); i++)
			{
				Ability ability = new Ability(oathList.get(0), "0") {
					@Override
					public void resolve() {
						CardList graveyardCreatures = AllZoneUtil.getPlayerTypeInGraveyard(player, "Creature");

						if(AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
							if(player.equals(AllZone.HumanPlayer)) {
								Object o = GuiUtils.getChoiceOptional("Pick a creature to return to hand",
										graveyardCreatures.toArray());
								if(o != null) {
									Card card = (Card) o;

									AllZone.GameAction.moveToHand(card);
								}
							} 
							else if(player.equals(AllZone.ComputerPlayer)) {
								Card card = graveyardCreatures.get(0);

								AllZone.GameAction.moveToHand(card);
							}
						}
					}
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
				sb.append("from their graveyard to owner's hand if they have more than an opponent.");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}
	}//Oath of Ghouls
	
	private static void upkeep_Ancient_Runes() {
		final Player player = AllZone.Phase.getPlayerTurn();
		
		CardList ancient_runes = AllZoneUtil.getCardsInPlay("Ancient Runes");
		
		// determine how much damage to deal the current player
		final int damage = AllZoneUtil.getPlayerTypeInPlay(player, "Artifact").size();
		
		// if there are 1 or more Ancient Runes on the 
		// battlefield have each of them deal damage.
		if(0 < ancient_runes.size()) {
			for(Card rune:ancient_runes) {
				final Card src = rune;
				Ability ability = new Ability(src, "0") {
					@Override
					public void resolve() {
						if(damage>0){
							player.addDamage(damage,src);
						}
					}
				};// Ability
				if(damage>0){
					
					StringBuilder sb = new StringBuilder();
					sb.append("Ancient Runes deals ").append(damage).append(" damage to ").append(player);
					ability.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability);

				}
			}
		}// if
	}// upkeep_Ancient_Runes()
	
	private static void upkeep_Karma() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList karmas = AllZoneUtil.getCardsInPlay("Karma");
		CardList swamps = AllZoneUtil.getPlayerTypeInPlay(player, "Swamp");
		
		// determine how much damage to deal the current player
		final int damage = swamps.size();
		
		// if there are 1 or more Karmas on the  
		// battlefield have each of them deal damage.
		if(0 < karmas.size()) {
			for(Card karma:karmas) {
				final Card src = karma;
				Ability ability = new Ability(src, "0") {
					@Override
					public void resolve() {
						if(damage>0){
							player.addDamage(damage, src);
						}
					}
				};// Ability
				if(damage>0){
					
					StringBuilder sb = new StringBuilder();
					sb.append("Karma deals ").append(damage).append(" damage to ").append(player);
					ability.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(ability);

				}
			}
		}// if
	}// upkeep_Karma()
	
	/* Converted to trigger and AF
	private static void upkeep_Convalescence() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Convalescence");

		for(int i = 0; i < list.size(); i++) {
			final Card source = list.get(i);
			Ability ability = new Ability(source, "0") {

				@Override
				public void resolve() {
					if (player.getLife() <= 10)
						player.gainLife(1, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Convalescence - ").append(player).append(" gain 1 life");
			ability.setStackDescription(sb.toString());

			if(player.getLife() <= 10) {
                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}// for
	}// upkeep_Convalescence()
	*/

	/* Converted to trigger and AF
	private static void upkeep_Convalescent_Care() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Convalescent Care");

		for(int i = 0; i < list.size(); i++) {
			final Card source = list.get(i);
            Ability ability = new Ability(source, "0")
            {

            	public void resolve()
            	{
            		if (player.getLife() <= 5){
	            		player.gainLife(3, source);
            			player.drawCard();
            		}
            	}
            };// Ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Convalescent Care - ").append(player).append(" gains 3 life and draws a card");
            ability.setStackDescription(sb.toString());

            if (player.getLife() <= 5){
                AllZone.Stack.addSimultaneousStackEntry(ability);

            }

		}// for
	}// upkeep_Convalescence_Care()
	*/
	
	private static void upkeep_Dega_Sanctuary() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Dega Sanctuary");

		for(Card sanc:list) {
			final Card source = sanc;
			final Ability ability = new Ability(source, "0") {
				public void resolve() {
					int gain = 0;
					CardList play = AllZoneUtil.getPlayerCardsInPlay(player);
					CardList black = play.filter(AllZoneUtil.black);
					CardList red = play.filter(AllZoneUtil.red);
					if(black.size() > 0 && red.size() > 0) gain = 4;
					else if(black.size() > 0 || red.size() > 0) gain = 2;
					player.gainLife(gain, source);
				}
			};//Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source.getName()).append(" - ");
			sb.append("if you control a black or red permanent, you gain 2 life. If you control a black permanent and a red permanent, you gain 4 life instead.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//for
	}//upkeep_Dega_Sanctuary()
		
	private static void upkeep_Power_Surge() {
		/*
		 * At the beginning of each player's upkeep, Power Surge deals X
		 * damage to that player, where X is the number of untapped
		 * lands he or she controlled at the beginning of this turn.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getCardsInPlay("Power Surge");
		final int damage = player.getNumPowerSurgeLands();

		for(Card surge:list) {
			final Card source = surge;
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					player.addDamage(damage, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - deals ").append(damage).append(" damage to ").append(player);
			ability.setStackDescription(sb.toString());
			
			if(damage > 0) {
                AllZone.Stack.addSimultaneousStackEntry(ability);

			}
		}// for
	}// upkeep_Power_Surge()

	private static void upkeep_Felidar_Sovereign() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Felidar Sovereign");

		if(0 < list.size() && player.getLife() >= 40) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					if (player.getLife() >= 40)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Felidar Sovereign - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Felidar_Sovereign

	private static void upkeep_Battle_of_Wits() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Battle of Wits");

		if(0 < list.size() && 200 <= libraryZone.size()) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, player);
					if (libraryZone.size() >= 200)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Battle of Wits - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Battle_of_Wits
	
	private static void upkeep_Mortal_Combat() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Mortal Combat");
		CardList grave = AllZoneUtil.getPlayerGraveyard(player);
		grave = grave.filter(AllZoneUtil.creatures);

		if(0 < list.size() && 20 <= grave.size()) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					CardList grave = AllZoneUtil.getPlayerGraveyard(player);
					grave = grave.filter(AllZoneUtil.creatures);
					if (grave.size() >= 20)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Mortal Combat - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Mortal Combat

	private static void upkeep_Helix_Pinnacle() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Helix Pinnacle");

		for(final Card c : list) {
			if (c.getCounters(Counters.TOWER) < 100) continue;

			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					if (c.getCounters(Counters.TOWER) >= 100)
						player.altWinConditionMet(c.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Helix Pinnacle - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Helix_Pinnacle

	private static void upkeep_Near_Death_Experience() {
		/*
		 * At the beginning of your upkeep, if you have exactly 1 life, you win the game.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Near-Death Experience");

		if(0 < list.size() && player.getLife() == 1) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					if (player.getLife() == 1)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Near-Death Experience - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Near_Death_Experience
	
	private static void upkeep_Test_of_Endurance() {
		/*
		 * At the beginning of your upkeep, if you have 50 or more life, you win the game.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Test of Endurance");
		
		if(0 < list.size() && player.getLife() >= 50) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					if (player.getLife() >= 50)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(list.get(0)).append(" - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Test_of_Endurance


	private static void upkeep_Barren_Glory() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		PlayerZone handZone = AllZone.getZone(Constant.Zone.Hand, player);

		CardList list = new CardList(playZone.getCards());
		CardList playList = new CardList(playZone.getCards());
		playList = playList.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return !c.getName().equals("Mana Pool");
			}
		});

		list = list.getName("Barren Glory");

		if(playList.size() == 1 && list.size() == 1 && handZone.size() == 0) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					CardList handList = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, player);
					CardList playList = AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, player);
					playList = playList.getValidCards("Permanents".split(","),source.getController(),source);
					playList.remove(source);
					
					if (playList.size() == 0 && handList.size() == 0)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Barren Glory - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// if
	}// upkeep_Barren_Glory

	private static void upkeep_Sleeper_Agent() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Sleeper Agent");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card F_card = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(2, F_card);
				}
			};

			ability.setStackDescription("Sleeper Agent deals 2 damage to its controller.");

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}
	}//upkeep_Sleeper_Agent
	
	private static void upkeep_Shapeshifter() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Shapeshifter");
		list = list.filter(AllZoneUtil.nonToken);

		for(final Card c:list) {
			SpellAbility ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					int num = 0;
					if(player.isHuman()) {
						String[] choices = new String[7];
						for(int j = 0; j < 7; j++) {
							choices[j] = ""+j;
						}
						String answer = (String)(GuiUtils.getChoiceOptional(c.getName()+" - Choose a number", choices));
						num = Integer.parseInt(answer);
					}
					else {
						num = 3;
					}
					c.setBaseAttack(num);
					c.setBaseDefense(7-num);
				}
			};
			ability.setStackDescription(c.getName()+" - choose a new number");

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//foreach(Card)
	}//upkeep_Shapeshifter
	
	private static void upkeep_Vesuvan_Doppelganger_Keyword() {
		// todo: what about enchantments? i dont know how great this solution is
		final Player player = AllZone.Phase.getPlayerTurn();
		final String keyword = "At the beginning of your upkeep, you may have this creature become a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.";
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
		list = list.filter(AllZoneUtil.getKeywordFilter(keyword));

		for(final Card c:list) {
			final SpellAbility ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					final Card[] newTarget = new Card[1];
					newTarget[0] = null;
					
					final Ability switchTargets = new Ability(c, "0") {
						public void resolve() {
							if(newTarget[0] != null) {
								/*
								 * 1. need to select new card - DONE
								 * 1a. need to create the newly copied card with pic and setinfo
								 * 2. need to add the leaves play command
								 * 3. need to transfer the keyword
								 * 4. need to update the clone origin of new card and old card
								 * 5. remove clone leaves play commands from old
								 * 5a. remove old from play
								 * 6. add new to play
								 */
								
								Card newCopy = AllZone.CardFactory.getCard(newTarget[0].getName(), player);
								newCopy.setCurSetCode(newTarget[0].getCurSetCode());
								newCopy.setImageFilename(newTarget[0].getImageFilename());

								//need to add the leaves play command (2)
								newCopy.addLeavesPlayCommand(c.getCloneLeavesPlayCommand());
								c.removeTrigger(c.getCloneLeavesPlayCommand(), ZCTrigger.LEAVEFIELD);
								newCopy.setCloneLeavesPlayCommand(c.getCloneLeavesPlayCommand());

								newCopy.addExtrinsicKeyword(keyword);
								newCopy.addColor("U", newCopy, false, true);
								newCopy.setCloneOrigin(c.getCloneOrigin());
								newCopy.getCloneOrigin().setCurrentlyCloningCard(newCopy);
								c.setCloneOrigin(null);

								//5
								PlayerZone play = AllZone.getZone(c);
								play.remove(c);

								play.add(newCopy);
							}
						}
					};
					
					AllZone.InputControl.setInput(new Input() {
						private static final long serialVersionUID = 5662272658873063221L;

						@Override
						public void showMessage() {
							AllZone.Display.showMessage(c.getName()+" - Select new target creature.  (Click Cancel to remain as is.)");
							ButtonUtil.enableOnlyCancel();
						}
						
						@Override
						public void selectButtonCancel() { stop(); }
						
						@Override
						public void selectCard(Card selectedCard, PlayerZone z) {
							if(z.is(Constant.Zone.Battlefield) && selectedCard.isCreature()
									&& CardFactoryUtil.canTarget(c, selectedCard)) {
								newTarget[0] = selectedCard;
								StringBuilder sb = new StringBuilder();
			                    sb.append(c.getCloneOrigin()).append(" - switching to copy "+selectedCard.getName()+".");
			                    switchTargets.setStackDescription(sb.toString());
								AllZone.Stack.add(switchTargets);
								stop();
							}
						}
					});
				}
			};
			ability.setStackDescription(c.getName()+" - you may have this creature become a copy of target creature.");

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//foreach(Card)
	}//upkeep_Vesuvan_Doppelganger_Keyword
	
	private static void upkeep_Tangle_Wire() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList wires = AllZoneUtil.getCardsInPlay("Tangle Wire");

		for(final Card source:wires) {
			SpellAbility ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					final int num = source.getCounters(Counters.FADE);
					final CardList list = AllZoneUtil.getPlayerCardsInPlay(player).filter(new CardListFilter() {
						public boolean addCard(Card c) {
							return (c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped();
						}
					});

					for(int i = 0; i < num; i++) {
						if(player.isComputer()) {
							Card toTap = CardFactoryUtil.AI_getWorstPermanent(list, false, false, false, false);
							if(null != toTap) {
								toTap.tap();
								list.remove(toTap);
							}
						}
						else {
							AllZone.InputControl.setInput(new Input() {
								private static final long serialVersionUID = 5313424586016061612L;
								public void showMessage() {
									if(list.size() == 0)
                                    {
                                        stop();
                                        return;
                                    }
						             AllZone.Display.showMessage(source.getName()+" - Select "+num+" untapped artifact(s), creature(s), or land(s) you control");
						             ButtonUtil.disableAll();
						          }
								public void selectCard(Card card, PlayerZone zone) {
						        	  if(zone.is(Constant.Zone.Battlefield, AllZone.HumanPlayer) && list.contains(card)) {
						                card.tap();
						                list.remove(card);
						                stop();
						             }
						          }
							});
						}
					}
				}
			};
			ability.setStackDescription(source.getName()+" - "+player+" taps X artifacts, creatures or lands he or she controls.");

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//foreach(wire)
	}//upkeep_Tangle_Wire()

	private static void upkeep_Pillory_of_the_Sleepless() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.filter(new CardListFilter() {

			public boolean addCard(Card c) {
				return c.isCreature() && c.isEnchanted();
			}
		});

		if(list.size() > 0) {
			ArrayList<Card> enchants;
			Ability ability;
			for(int i = 0; i < list.size(); i++) {
				final Card F_card = list.get(i);
				enchants = list.get(i).getEnchantedBy();
				for(Card enchant:enchants) {
					if(enchant.getName().equals("Pillory of the Sleepless")) {
						//final Card c = enchant;
						ability = new Ability(enchant, "0") {
							@Override
							public void resolve() {
								player.loseLife(1, F_card);
							}
						};
						ability.setStackDescription("Pillory of the Sleepless  - enchanted creature's controller loses 1 life.");

                        AllZone.Stack.addSimultaneousStackEntry(ability);

					}
				}
			}

		}//list > 0
	}//cursed land


	private static void upkeep_Greener_Pastures() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);
		PlayerZone oppPlayZone = AllZone.getZone(Constant.Zone.Battlefield, player.getOpponent());

		CardList self = new CardList(playZone.getCards());
		CardList opp = new CardList(oppPlayZone.getCards());

		self = self.getType("Land");
		opp = opp.getType("Land");

		if((self.size() == opp.size()) || opp.size() > self.size()) return;
		else //active player has more lands
		{
			Player mostLandsPlayer = null;
			if(self.size() > opp.size()) mostLandsPlayer = player;

			final Player mostLands = mostLandsPlayer;

			CardList list = AllZoneUtil.getCardsInPlay("Greener Pastures");

			Ability ability;

			for(int i = 0; i < list.size(); i++) {
				//final Card crd = list.get(i);
				ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						CardFactoryUtil.makeTokenSaproling(mostLands);
					}// resolve()
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("Greener Pastures - ").append(mostLands).append(" puts a 1/1 green Saproling token onto the battlefield.");
				ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

			}// for

		}//else
	}// upkeep_Greener_Pastures()

	private static void upkeep_Masticore() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Masticore");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);

			final Input discard = new Input() {
				private static final long serialVersionUID = 2252076866782738069L;

				@Override
				public void showMessage() {
					AllZone.Display.showMessage(crd + " - Discard a card from your hand");
					ButtonUtil.enableOnlyCancel();
				}

				@Override
				public void selectCard(Card c, PlayerZone zone) {
					if(zone.is(Constant.Zone.Hand)) {
						c.getController().discard(c, null);
						stop();
					}
				}

				@Override
				public void selectButtonCancel() {
					AllZone.GameAction.sacrifice(crd);
					stop();
				}
			};//Input

			ability = new Ability(crd, "0") {
				@Override
				public void resolve() {
					if(crd.getController().equals(AllZone.HumanPlayer)) {
						if(AllZone.Human_Hand.getCards().length == 0) AllZone.GameAction.sacrifice(crd);
						else AllZone.InputControl.setInput(discard);
					} else //comp
					{
						CardList list = new CardList(AllZone.Computer_Hand.getCards());

						if(list.size() != 0) list.get(0).getController().discard(list.get(0),this);
						else AllZone.GameAction.sacrifice(crd);
					}//else
				}//resolve()
			};//Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(crd).append(" - sacrifice Masticore unless you discard a card.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// for
	}//upkeep_Masticore


	private static void upkeep_Eldrazi_Monument() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Eldrazi Monument");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					CardList creats = new CardList(playZone.getCards());
					creats = creats.getType("Creature");

					if(creats.size() < 1) {
						AllZone.GameAction.sacrifice(card);
						return;
					}

					if(player.equals(AllZone.HumanPlayer)) {
						Object o = GuiUtils.getChoiceOptional("Select creature to sacrifice",
								creats.toArray());
						Card sac = (Card) o;
						if(sac == null) {
							creats.shuffle();
							sac = creats.get(0);
						}
						AllZone.GameAction.sacrifice(sac);
					} else//computer
					{
						CardListUtil.sortAttackLowFirst(creats);
						AllZone.GameAction.sacrifice(creats.get(0));
					}
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Eldrazi Monument - ").append(player).append(" sacrifices a creature.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}

	}//upkeep_Eldrazi_Monument

	private static void upkeep_Blaze_Counters() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList blaze = new CardList(playZone.getCards());
		blaze = blaze.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.isLand() && c.getCounters(Counters.BLAZE) > 0;
			}
		});

			for(int i = 0; i < blaze.size(); i++) {
			final Card Source = blaze.get(i);
			Ability ability = new Ability(blaze.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(1, Source);
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
			sb.append(player).append(".");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}
	}
	
	private static void upkeep_Dragon_Broodmother() {
		CardList list = AllZoneUtil.getCardsInPlay("Dragon Broodmother");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			ability = new Ability(card, "0") {
				@Override
				public void resolve() {
					int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(card.getController());
					for(int i = 0; i < multiplier; i++)
						makeToken();

				}// resolve()

				public void makeToken() {
					//CardList cl = CardFactoryUtil.makeToken("Dragon", "RG 1 1 Dragon", card, "RG", new String[] {"Creature", "Dragon"}, 1, 1, new String[] {"Flying"} );


					final Card c = new Card();

					c.setOwner(card.getController());
					c.setController(card.getController());

					c.setName("Dragon");
					c.setImageName("RG 1 1 Dragon");
					//c.setManaCost("RG");
					c.addColor("RG");
					c.setToken(true);

					c.addType("Creature");
					c.addType("Dragon");

					c.addIntrinsicKeyword("Flying");

					c.setBaseAttack(1);
					c.setBaseDefense(1);

					//final String player = card.getController();
					final int[] numCreatures = new int[1];

					final SpellAbility devour = new Spell(card) {

						private static final long serialVersionUID = 4158780345303896275L;

						@Override
						public void resolve() {
							int totalCounters = numCreatures[0] * 2;
							c.addCounter(Counters.P1P1, totalCounters);

						}

						@Override
						public boolean canPlay() {
							return AllZone.Phase.getPlayerTurn().equals(card.getController())
							&& card.isFaceDown() && !AllZone.Phase.getPhase().equals("End of Turn")
							&& AllZone.GameAction.isCardInPlay(card);
						}

					};//devour

					Command intoPlay = new Command() {

						private static final long serialVersionUID = -9220268793346809216L;

						public void execute() {

							PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
							CardList creatsToSac = new CardList();
							CardList creats = new CardList(play.getCards());
							creats = creats.filter(new CardListFilter() {
								public boolean addCard(Card crd) {
									return crd.isCreature() && !crd.equals(c);
								}
							});

							//System.out.println("Creats size: " + creats.size());

							if(card.getController().equals(AllZone.HumanPlayer)) {
								Object o = null;
								int creatsSize = creats.size();

								for(int k = 0; k < creatsSize; k++) {
									o = GuiUtils.getChoiceOptional("Select creature to sacrifice",
											creats.toArray());

									if(o == null) break;

									Card crd = (Card) o;
									creatsToSac.add(crd);
									creats.remove(crd);
								}

								numCreatures[0] = creatsToSac.size();
								for(int m = 0; m < creatsToSac.size(); m++) {
									AllZone.GameAction.sacrifice(creatsToSac.get(m));
								}

							}//human
							else {
								int count = 0;
								for(int i = 0; i < creats.size(); i++) {
									Card crd = creats.get(i);
									if(crd.getNetAttack() <= 1 && crd.getNetDefense() <= 2) {
										AllZone.GameAction.sacrifice(crd);
										count++;
									}
								}
								numCreatures[0] = count;
							}
                            AllZone.Stack.addSimultaneousStackEntry(devour);

						}
					};
					
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName()).append(" - gets 2 +1/+1 counter(s) per devoured creature.");
					devour.setStackDescription(sb.toString());
					
					devour.setDescription("Devour 2");
					c.addSpellAbility(devour);
					c.addComesIntoPlayCommand(intoPlay);

					AllZone.GameAction.moveToPlay(c);
				}
			};// Ability
			ability.setStackDescription("Dragon Broodmother - put a 1/1 red and green Dragon token onto the battlefield.");

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}// for
	}// upkeep_Dragon_Broodmother()
	
	private static void draw_Sylvan_Library(final Player player) {
		/*
		 * At the beginning of your draw step, you may draw two additional
		 * cards. If you do, choose two cards in your hand drawn this turn.
		 * For each of those cards, pay 4 life or put the card on top of
		 * your library.
		 */
		final CardList cards = AllZoneUtil.getPlayerCardsInPlay(player, "Sylvan Library");
		
		for(final Card source:cards) {
			final Ability ability = new Ability(source, "") {
				@Override
				public void resolve() {
					final Player player = source.getController();
					if (player.isHuman()) {
						String question = "Draw 2 additional cards?";
						final String cardQuestion = "Pay 4 life and keep in hand?";
						if (GameActionUtil.showYesNoDialog(source, question)) {
							player.drawCards(2);
							for(int i = 0; i < 2; i++) {
								final String prompt = source.getName()+" - Select a card drawn this turn: "+(2-i)+" of 2";
								AllZone.InputControl.setInput(new Input() {
									private static final long serialVersionUID = -3389565833121544797L;

									@Override
						            public void showMessage() {
						            	if (AllZone.Human_Hand.getCards().length == 0) stop();
						                AllZone.Display.showMessage(prompt);
						                ButtonUtil.disableAll();
						            }
						            
						            @Override
						            public void selectCard(Card card, PlayerZone zone) {
						                if(zone.is(Constant.Zone.Hand) && true == card.getDrawnThisTurn()) {
						                    /////////////////////////////////////////
						                	if (player.canPayLife(4) && GameActionUtil.showYesNoDialog(source, cardQuestion)) {
						                		player.payLife(4, source);
						                		//card stays in hand
						                	}
						                	else {
						                		AllZone.GameAction.moveToLibrary(card);
						                	}
						                	stop();
						                	////////////////////////////////////
						                }
						            }
								});//end Input
							}
						}
					}
					else {
						//Computer, but he's too stupid to play this
					}
				}//resolve
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("At the beginning of your draw step, you may draw two additional cards. If you do, choose two cards in your hand drawn this turn. For each of those cards, pay 4 life or put the card on top of your library.");
			ability.setStackDescription(sb.toString());

            AllZone.Stack.addSimultaneousStackEntry(ability);

		}//end for
	}
	
	private static void draw_Teferi_Puzzle_Box(Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Teferi's Puzzle Box");
        PlayerZone Playerhand = AllZone.getZone(Constant.Zone.Hand, player);
        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player); 
       
		CardList hand = new CardList();
		Card[] handlist = null;
		if(list.size() > 0) {
            AllZone.Display.showMessage("Shuffle cards back into your library: ");
            ButtonUtil.enableOnlyCancel();
            hand.addAll(Playerhand.getCards());            
			int Count = hand.size();
            for(int i = 0; i < list.size(); i++) {           	
            	if(AllZone.HumanPlayer.equals(player)) {            
                    for(int e = 0; e < Count; e++) {
	                    if(hand.size() == 0) hand.addAll(Playerhand.getCards());
	                    handlist = hand.toArray();
	                    Object check = GuiUtils.getChoice("Select card to put on bottom of library", handlist);
	                    if(check != null) {
		                     Card target = ((Card) check);
		                     hand.remove(target);
		                     AllZone.GameAction.moveTo(lib, target);    
	                    }
                    }
            	}else {
                    for(int x = 0; x < hand.size(); x++) hand.remove(hand.get(x));
                    hand.addAll(Playerhand.getCards());
                    for(int e = 0; e < hand.size(); e++) {
                    	AllZone.GameAction.moveTo(lib, hand.get(e)); 
                    }
            	}
            			
				player.drawCards(Count);
            }
		}

	}// Teferi_Puzzle_Box

		private static void upkeep_Carnophage() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Carnophage");
		if(player == AllZone.HumanPlayer) {
			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				String[] choices = {"Yes", "No"};
				Object choice = GuiUtils.getChoice("Pay Carnophage's upkeep?", choices);
				if(choice.equals("Yes")) player.loseLife(1, c);
				else c.tap();
			}
		}
		if(player == AllZone.ComputerPlayer) for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(AllZone.ComputerPlayer.getLife() > 1) player.loseLife(1, c);
			else c.tap();
		}
	}// upkeep_Carnophage

	private static void upkeep_Sangrophage() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Sangrophage");
		if(player == AllZone.HumanPlayer) {
			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				String[] choices = {"Yes", "No"};
				Object choice = GuiUtils.getChoice("Pay Sangrophage's upkeep?", choices);
				if(choice.equals("Yes")) player.loseLife(2,c);
				else c.tap();
			}
		}
		if(player == AllZone.ComputerPlayer) for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(AllZone.ComputerPlayer.getLife() > 2) player.loseLife(2,c);
			else c.tap();
		}
	}// upkeep_Carnophage
	
	private static void upkeep_Fallen_Empires_Storage_Lands() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList all = AllZoneUtil.getPlayerCardsInPlay(player, "Bottomless Vault");
		all.add(AllZoneUtil.getPlayerCardsInPlay(player, "Dwarven Hold"));
		all.add(AllZoneUtil.getPlayerCardsInPlay(player, "Hollow Trees"));
		all.add(AllZoneUtil.getPlayerCardsInPlay(player, "Icatian Store"));
		all.add(AllZoneUtil.getPlayerCardsInPlay(player, "Sand Silos"));
		
		for(Card land:all) {
			if(land.isTapped()) land.addCounter(Counters.STORAGE, 1);
		}
	} //upkeep_Fallen_Empires_Storage_Lands

	private static void upkeep_Vampire_Lacerator() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Vampire Lacerator");

		for(int i = 0; i < list.size(); i++) {
			final Card F_card = list.get(i);
			if(player.isHuman() && AllZone.ComputerPlayer.getLife() > 10) {
				player.loseLife(1, F_card);
			} else {
				if(player.isComputer() && AllZone.HumanPlayer.getLife() > 10) {
					player.loseLife(1, F_card);
				}
			}
		}
	}// upkeep_Vampire_Lacerator
	
	private static void upkeep_Mirror_Sigil_Sergeant()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player);

		list = list.getName("Mirror-Sigil Sergeant");

		Ability ability;
		for (int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			ability = new Ability(card, "0") {
				public void resolve() {
					// CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
					// CardList blueList = list.getColor(Constant.Color.Blue);
					CardList blueList = AllZoneUtil.getPlayerColorInPlay(player, Constant.Color.Blue);
					if (!blueList.isEmpty()) {
						CardFactoryUtil.makeToken("Mirror-Sigil Sergeant","W 4 4 Mirror Sigil Sergeant", card.getController(), "5 W",
								new String[]{"Creature","Rhino","Soldier"}, 4, 4, new String[]{"Trample",
								"At the beginning of your upkeep, if you control a blue permanent, you may put a token that's a copy of Mirror-Sigil Sergeant onto the battlefield."});
					}
				};

			}; // ability

			ability.setStackDescription("Mirror-Sigil Sergeant - put a token onto the battlefield that's a copy of Mirror-Sigil Sergeant.");

			AllZone.Stack.addSimultaneousStackEntry(ability);

		} // for
	} //upkeep_Mirror_Sigil_Sergeant

	public static void executeCardStateEffects() {
		Wonder.execute();
		Anger.execute();
		Valor.execute();
		Brawn.execute();
		Filth.execute();
		Dauntless_Escort.execute();

		Sacrifice_NoIslands.execute();
		Sacrifice_NoForests.execute();
		Sacrifice_NoSwamps.execute();
		Sacrifice_NoArtifacts.execute();
		Sacrifice_NoEnchantments.execute();
		Sacrifice_NoLands.execute();
		Sacrifice_NoCreatures.execute();
		Sacrifice_NoOtherCreatures.execute();
		
		topCardReveal_Update.execute();
	}// executeCardStateEffects()

	public static Command Conspiracy                  = new Command() {
		private static final long serialVersionUID   = -752798545956593342L;

		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			//String keyword = "Defender";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				//System.out.println("prev type: " +c.getPrevType());
				c.setType(c.getPrevType());
			}

			list.clear();

			PlayerZone[] zone = new PlayerZone[4];

			CardList cl = AllZoneUtil.getCardsInPlay("Conspiracy");

			for(int i = 0; i < cl.size(); i++) {
				Card card = cl.get(i);
				Player player = card.getController();
				zone[0] = AllZone.getZone(Constant.Zone.Hand,
						player);
				zone[1] = AllZone.getZone(Constant.Zone.Library,
						player);
				zone[2] = AllZone.getZone(
						Constant.Zone.Graveyard, player);
				zone[3] = AllZone.getZone(Constant.Zone.Battlefield,
						player);

				for(int outer = 0; outer < zone.length; outer++) {
					CardList creature = new CardList(
							zone[outer].getCards());
					creature = creature.getType("Creature");

					//System.out.println("zone[" + outer + "] = " + creature.size());

					for(int j = 0; j < creature.size(); j++) {
						boolean art = false;
						boolean ench = false;

						c = creature.get(j);

						if(c.isArtifact()) art = true;
						if(c.isEnchantment()) ench = true;

						if(c.getPrevType().size() == 0) c.setPrevType(c.getType());
						c.setType(new ArrayList<String>());
						c.addType("Creature");
						if(art) c.addType("Artifact");
						if(ench) c.addType("Enchantment");
						c.addType(card.getChosenType());

						gloriousAnthemList.add(c);
					}
				}
			}// for inner
		}// execute()
	}; //Conspiracy

	public static Command Mul_Daya_Channelers          = new Command() {
		private static final long serialVersionUID   = -2543659953307485051L;

		CardList                  landList = new CardList();
		CardList				  creatList = new CardList();

		String[]                  keyword            = { "B", "W", "G", "U", "R" };

		final void addMana(Card c) {
			for(int i = 0; i < keyword.length; i++) {
				//don't add an extrinsic mana ability if the land can already has the same intrinsic mana ability
				//eg. "tap: add G"
				if(!c.getIntrinsicManaAbilitiesDescriptions().contains(
						keyword[i])) {
					//c.addExtrinsicKeyword(keyword[i]);
					SpellAbility mana = new Ability_Mana(c, "T", keyword[i], 2) {
						private static final long serialVersionUID = 2384540533244132975L;
					};
					StringBuilder sb = new StringBuilder();
					sb.append("T: Add ").append(keyword[i]).append(" ").append(keyword[i]).append(" to your mana pool.");
					
					mana.setType("Extrinsic");
					mana.setDescription(sb.toString());
					c.addSpellAbility(mana);
				}
			}
		}
		final void removeMana(Card c) {
			c.removeAllExtrinsicManaAbilities();
		}

		public void execute() {
			CardList list1 = landList;
			CardList list2 = creatList;
			
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list1.size(); i++) {
				c = list1.get(i);
				removeMana(c);
			}
			
			for(int i = 0; i < list2.size(); i++) {
				c = list2.get(i);
				c.addSemiPermanentAttackBoost(-3);
				c.addSemiPermanentDefenseBoost(-3);
			}


			list1.clear();
			list2.clear();
			CardList cl = AllZoneUtil.getCardsInPlay();
			cl = cl.getName("Mul Daya Channelers");

			for (Card crd:cl)
			{
				if (CardFactoryUtil.getTopCard(crd)!= null)
				{
					Card topCard = CardFactoryUtil.getTopCard(crd);
					if (topCard.isLand()) {
						addMana(crd);
						landList.add(crd);
					}
					else if(topCard.isCreature())
					{
						crd.addSemiPermanentAttackBoost(3);
						crd.addSemiPermanentDefenseBoost(3);
						creatList.add(crd);
					}
						

				}
			}// for outer
		}// execute()
	}; // Mul Daya

	
	public static Command Elspeth_Emblem 			  = new Command() {

		private static final long serialVersionUID = 7414127991531889390L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "Indestructible";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword(keyword);
			}

			list.clear();
			
			CardList emblem = AllZoneUtil.getCardsInPlay();
			emblem = emblem.filter(new CardListFilter()
			{
				public boolean addCard(Card c)
				{
					return c.isEmblem() && c.getKeyword().contains("Artifacts, creatures, enchantments, and lands you control are indestructible.");
				}
			});
			
			for (int i = 0; i < emblem.size(); i++)
			{
				CardList perms = AllZoneUtil.getPlayerCardsInPlay(emblem.get(i).getController());
				
				for(int j = 0; j < perms.size(); j++) {
					c = perms.get(j);
					if(!c.getKeyword().contains(keyword)) {
						c.addExtrinsicKeyword(keyword);
						gloriousAnthemList.add(c);
					}
				}
			}
		}// execute()
	};
	
	public static Command Koth_Emblem = new Command() {

		private static final long serialVersionUID = -3233715310427996429L;
		CardList gloriousAnthemList = new CardList();
		
		public void execute()
		{
			CardList list = gloriousAnthemList;
			Card crd;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				crd = list.get(i);
				SpellAbility[] sas = crd.getSpellAbility();
				for (int j=0;j<sas.length;j++)
				{
					if (sas[j].isKothThirdAbility())
						crd.removeSpellAbility(sas[j]);
				}
			}
			
			CardList emblem = AllZoneUtil.getCardsInPlay();
			emblem = emblem.filter(new CardListFilter()
			{
				public boolean addCard(Card c)
				{
					return c.isEmblem() && c.getKeyword().contains("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'");
				}
			});
			
			for (int i = 0; i < emblem.size(); i++)
			{
				CardList mountains = AllZoneUtil.getPlayerCardsInPlay(emblem.get(i).getController());
				mountains = mountains.filter(new CardListFilter()
				{
					public boolean addCard(Card crd)
					{
						return crd.getType().contains("Mountain");
					}
				});
				
				for(int j = 0; j < mountains.size(); j++) {
					final Card c = mountains.get(j);
					boolean hasAbility = false;
					SpellAbility[] sas = c.getSpellAbility();
					for (SpellAbility sa:sas)
					{
						if (sa.isKothThirdAbility())
							hasAbility = true;
					}
					
					if(!hasAbility) {
						Cost abCost = new Cost("T", c.getName(), true);
						Target target = new Target(c,"TgtCP");
						final Ability_Activated ability = new Ability_Activated(c, abCost, target)
					    {
					        private static final long serialVersionUID = -7560349014757367722L;
							public void chooseTargetAI()
					        {
					          CardList list = CardFactoryUtil.AI_getHumanCreature(1, c, true);
					          list.shuffle();

					          if(list.isEmpty() || AllZone.HumanPlayer.getLife() < 5)
					            setTargetPlayer(AllZone.HumanPlayer);
					          else
					            setTargetCard(list.get(0));
					        }
					        public void resolve()
					        {
					          if(getTargetCard() != null)
					          {
					            if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(c, getTargetCard()) )
					            	getTargetCard().addDamage(1, c);
					          }
					          else {
					        	  getTargetPlayer().addDamage(1, c);
					          }
					        }//resolve()
					    };//SpellAbility
					    ability.setKothThirdAbility(true);
					    ability.setDescription(abCost+"This land deals 1 damage to target creature or player.");
					    
					    c.addSpellAbility(ability);
					    
						gloriousAnthemList.add(c);
					}
				}
			}
			
		}
	};
	
	public static Command stPump  		= new Command() {
		/** StaticEffectKeyword
		 * Syntax:[ k[0] stPump[All][Self][Other] : k[1] Which Cards the Bonus Affects : 
		 * 			k[2] What Bonus does the Card have : k[3] Special Conditions : k[4] Description
		 */
		
		private static final long serialVersionUID = -7853346190458174501L;
		private ArrayList<StaticEffect> storage = new ArrayList<StaticEffect>();
			// storage stores the source card and the cards it gave its bonus to, to know what to remove
		
		public void execute() {
			
			// remove all static effects
			for (int i = 0; i < storage.size(); i++) {
	    		removeStaticEffect(storage.get(i));
	    	}
			
			//clear the list
			storage = new ArrayList<StaticEffect>();
			
			//Gather Cards on the Battlefield with the stPump Keyword
			PlayerZone Hplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
			PlayerZone Cplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			CardList cards_WithKeyword = new CardList();
			
			cards_WithKeyword.add(new CardList(Hplay.getCards()));
			cards_WithKeyword.add(new CardList(Cplay.getCards()));
			cards_WithKeyword.getKeywordsContain("stPump");
			
			// check each card
			for (int i = 0; i < cards_WithKeyword.size(); i++) {
	    		Card cardWithKeyword = cards_WithKeyword.get(i);
	            ArrayList<String> keywords = cardWithKeyword.getKeyword();
	            
	            // check each keyword of the card
	            for (int j = 0; j < keywords.size(); j++) {
	            	String keyword = keywords.get(j);
	            	
	            	if(keyword.startsWith("stPump")) {
	            		StaticEffect se = new StaticEffect(); 	//create a new StaticEffect
	            		se.setSource(cardWithKeyword);
	            		se.setKeywordNumber(j);

	            		
	            		//get the affected cards
						String k[] = keyword.split(":",5);    
						
						if(SpecialConditionsMet(cardWithKeyword, k[3])) { //special Conditions are Threshold, etc.
						
							final String affected = k[1];			
							final String specific[] = affected.split(",");
							CardList affectedCards = AffectedCards(cardWithKeyword, k); // options are All, Self, Enchanted etc.
							affectedCards = affectedCards.getValidCards(specific, cardWithKeyword.getController(), cardWithKeyword);
							se.setAffectedCards(affectedCards);
							
							String[] pt = k[2].split("/");
							
							int x = 0;
		            		if (pt[0].contains("X") || pt[1].contains("X")) 
		                 		x = CardFactoryUtil.xCount(cardWithKeyword, cardWithKeyword.getSVar("X").split("\\$")[1]);
		                 	se.setXValue(x);
		                 	
		                 	int y = 0;
		            		if (pt[1].contains("Y")) 
		                 		y = CardFactoryUtil.xCount(cardWithKeyword, cardWithKeyword.getSVar("Y").split("\\$")[1]);
		                 	se.setYValue(y);
		            		
							addStaticEffects(cardWithKeyword,affectedCards,k[2],x, y); //give the boni to the affected cards

							storage.add(se); // store the information
						}
	            	}
	            }
	    	}
		}// execute()
		
		void addStaticEffects(Card source, CardList affectedCards, String Keyword_Details, int xValue, int yValue) {
			
			int powerbonus = 0;
			int toughnessbonus = 0;
			String[] Keyword = Keyword_Details.split("/",3);
			
			Keyword[0] = Keyword[0].replace("+","");
			Keyword[1] = Keyword[1].replace("+","");
			
			if(!Keyword[0].contains("X")) powerbonus = Integer.valueOf(Keyword[0]);
			else powerbonus = xValue; 		// the xCount takes places before
			
			if(Keyword[1].contains("X")) toughnessbonus = xValue;
			else if(Keyword[1].contains("Y")) toughnessbonus = yValue;
			else toughnessbonus = Integer.valueOf(Keyword[1]);
			
			for(int i = 0; i < affectedCards.size(); i++) {
				Card affectedCard = affectedCards.get(i);
				affectedCard.addSemiPermanentAttackBoost(powerbonus);
				affectedCard.addSemiPermanentDefenseBoost(toughnessbonus);
				if (Keyword.length > 2) {
					String Keywords[] = Keyword[2].split(" & ");
					for(int j = 0; j < Keywords.length; j++) {
						String keyword = Keywords[j];
						if(keyword.startsWith("SVar=")) {
							String sVar = source.getSVar(keyword.split("SVar=")[1]);
							if (sVar.startsWith("AB")) { // grant the ability
								AbilityFactory AF = new AbilityFactory();
								SpellAbility sa = AF.getAbility(sVar, affectedCard);
								sa.setType("Temporary");
			        		
								affectedCard.addSpellAbility(sa);
							}/*
							else if (sVar.startsWith("Mode")){ // grant a Trigger
								affectedCard.addTrigger(TriggerHandler.parseTrigger(sVar, affectedCard));							
							}*/
							else { // Copy this SVar
								affectedCard.setSVar(keyword.split("SVar=")[1], sVar);								
							}
						}
						else if(keyword.startsWith("Types=")) {
							String[] tmptypes = keyword.split("=");
							String[] types = tmptypes[1].split(",");
							if(types[0].equals("ChosenType")) {
								types[0] = source.getChosenType();
							}
							for(String type : types) affectedCard.addType(type);
						}
						else affectedCard.addExtrinsicKeyword(keyword);
					}
				}
			}
		}
		
		void removeStaticEffect(StaticEffect se) {
			Card source = se.getSource();
			CardList affected = se.getAffectedCards();
			int KeywordNumber = se.getKeywordNumber();
			int xValue = se.getXValue(); 		// the old xValue has to be removed, not the actual one!
			int yValue = se.getYValue(); 		// the old xValue has to be removed, not the actual one!
            String parse = source.getKeyword().get(KeywordNumber).toString();                
            String k[] = parse.split(":");
			for(int i = 0; i < affected.size(); i++) {
				removeStaticEffect(source, affected.get(i),k,xValue, yValue);
			}	
		}
		
		void removeStaticEffect(Card source, Card affectedCard, String[] Keyword_Details, int xValue, int yValue) {
			
			int powerbonus = 0;
			int toughnessbonus = 0;
			String[] Keyword = Keyword_Details[2].split("/",3);
			
			Keyword[0] = Keyword[0].replace("+","");
			Keyword[1] = Keyword[1].replace("+","");
			
			if(!Keyword[0].contains("X")) powerbonus = Integer.valueOf(Keyword[0]);
			else powerbonus = xValue; 		
			
			if(Keyword[1].contains("X")) toughnessbonus = xValue;
			else if(Keyword[1].contains("Y")) toughnessbonus = yValue;
			else toughnessbonus = Integer.valueOf(Keyword[1]);
			
			affectedCard.addSemiPermanentAttackBoost(powerbonus * -1);
			affectedCard.addSemiPermanentDefenseBoost(toughnessbonus * -1);
			if (Keyword.length > 2) {
				String Keywords[] = Keyword[2].split(" & ");
				for(int j = 0; j < Keywords.length; j++) {
					String keyword = Keywords[j];
					if(keyword.startsWith("SVar=")) {
						String sVar = source.getSVar(keyword.split("SVar=")[1]);
						if (sVar.startsWith("AB")) { // remove granted abilities
							SpellAbility[] spellAbility = affectedCard.getSpellAbility();
							for(SpellAbility s : spellAbility)
							{
								if (s.getType().equals("Temporary"))
									affectedCard.removeSpellAbility(s);
							}
						}
					}
					else if(keyword.startsWith("Types=")) {
						String[] tmptypes = keyword.split("=");
						String[] types = tmptypes[1].split(",");
						if(types[0].equals("ChosenType")) {
							types[0] = source.getChosenType();
						}
						for(String type : types) affectedCard.removeType(type);
					}
					affectedCard.removeExtrinsicKeyword(keyword);
				}
			}
		}
		
    	// Special Conditions
		boolean SpecialConditionsMet(Card SourceCard, String SpecialConditions) {
  	      	
  	      	if(SpecialConditions.contains("CardsInHandMore")) {
  	      		CardList SpecialConditionsCardList = new CardList();
	      		SpecialConditionsCardList.clear();
	      		String Condition = SpecialConditions.split("/")[1];
	      		SpecialConditionsCardList.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController()).getCards());
	      		if(SpecialConditionsCardList.size() < Integer.valueOf(Condition)) return false;
  	      	}
  	      	if(SpecialConditions.contains("OppHandEmpty")) {
	      		CardList oppHand = AllZoneUtil.getPlayerHand(SourceCard.getController().getOpponent());
	      		if(!(oppHand.size() == 0)) return false;
	      	}
  	      	if(SpecialConditions.contains("TopCardOfLibraryIsBlack")) {
	      		PlayerZone lib = AllZone.getZone(Constant.Zone.Library, SourceCard.getController());
	      		if(!(lib.get(0).isBlack())) return false;
	      	}
  	      	if(SpecialConditions.contains("LibraryLE")) {
  	      		CardList Library = new CardList();
  	      		Library.addAll(AllZone.getZone(Constant.Zone.Library, SourceCard.getController()).getCards());
  	      		String maxnumber = SpecialConditions.split("/")[1];
	      		if (Library.size() > Integer.valueOf(maxnumber)) return false;
	      	}
  	      	if(SpecialConditions.contains("LifeGE")) {
  	      		int life = SourceCard.getController().getLife();
  	      		String maxnumber = SpecialConditions.split("/")[1];
  	      		if (!(life >= Integer.valueOf(maxnumber))) return false;
  	      	}
  	      	if(SpecialConditions.contains("OppLifeLE")) {
	      		int life = SourceCard.getController().getOpponent().getLife();
	      		String maxnumber = SpecialConditions.split("/")[1];
	      		if (!(life <= Integer.valueOf(maxnumber))) return false;
	      	}
  	      	if(SpecialConditions.contains("Threshold")) {
  	      		if (!SourceCard.getController().hasThreshold()) return false;
  	      	}
  	      	if(SpecialConditions.contains("Hellbent")) {
  	      		CardList Handcards = new CardList();
  	      		Handcards.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController()).getCards());
	      		if (Handcards.size() > 0) return false;
	      	}
  	      	if(SpecialConditions.contains("Metalcraft")) {
  	      		PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, SourceCard.getController());
  	      		CardList CardsinPlay = new CardList(play.getCards());
      			CardsinPlay = CardsinPlay.getType("Artifact");
	      		if (CardsinPlay.size() < 3) return false;
	      	}
  	      	if(SpecialConditions.contains("isPresent")) { // is a card of a certain type/color present?
  	    	  	String Requirements = SpecialConditions.replaceAll("isPresent ", "");
    			CardList CardsinPlay = new CardList();
      			CardsinPlay.addAll(AllZone.Human_Battlefield.getCards());
      			CardsinPlay.addAll(AllZone.Computer_Battlefield.getCards());
      			String Conditions[] = Requirements.split(",");
      			CardsinPlay = CardsinPlay.getValidCards(Conditions, SourceCard.getController(), SourceCard);
	      		if (CardsinPlay.isEmpty()) return false;
  	      	}
  	      	if(SpecialConditions.contains("isInGraveyard")) { // is a card of a certain type/color present in yard?
  	      		String Requirements = SpecialConditions.replaceAll("isInGraveyard ", "");
  	      		CardList CardsinYards = AllZoneUtil.getCardsInGraveyard();
  	      		String Conditions[] = Requirements.split(",");
  	      		CardsinYards = CardsinYards.getValidCards(Conditions, SourceCard.getController(), SourceCard);
  	      		if (CardsinYards.isEmpty()) return false;
  	      	}
  	      	if(SpecialConditions.contains("isNotPresent")) { // is no card of a certain type/color present?
  	      		String Requirements = SpecialConditions.replaceAll("isNotPresent ", "");
    			CardList CardsinPlay = new CardList();
      			CardsinPlay.addAll(AllZone.Human_Battlefield.getCards());
      			CardsinPlay.addAll(AllZone.Computer_Battlefield.getCards());
      			String Conditions[] = Requirements.split(",");
      			CardsinPlay = CardsinPlay.getValidCards(Conditions, SourceCard.getController(), SourceCard);
	      		if (!CardsinPlay.isEmpty()) return false;
  	      	}
  	      	if(SpecialConditions.contains("isEquipped")) {
	      		if (!SourceCard.isEquipped()) return false;
	      	}
  	      	if(SpecialConditions.contains("isEnchanted")) {
	      		if (!SourceCard.isEnchanted()) return false;
	      	}
  	      	if(SpecialConditions.contains("isUntapped")) {
	      		if (!SourceCard.isUntapped()) return false;
	      	}
  	      	if(SpecialConditions.contains("isValid")) { // does this card meet the valid description?
  	      		String Requirements = SpecialConditions.replaceAll("isValid ", "");
  	      		if (!SourceCard.isValid(Requirements, SourceCard.getController(), SourceCard)) return false;
  	      	}
  	      	if(SpecialConditions.contains("isYourTurn")) {
  	      		if( !AllZone.Phase.isPlayerTurn(SourceCard.getController()) ) return false;
  	      	}
  	      	if(SpecialConditions.contains("notYourTurn")) {
  	      		if( !AllZone.Phase.isPlayerTurn(SourceCard.getController().getOpponent()) ) return false;
  	      	}
  	      	return true;
			
		}
		
		CardList AffectedCards (Card SourceCard, String[] Keyword_Details) {
			// [Self], [All], [Other]
			CardList Cards_inZone = new CardList();
			String Range = Keyword_Details[0].replaceFirst("stPump", "");
			
			if(Range.equals("Self")) {
				Cards_inZone.add(SourceCard);
			}
      		if(Range.equals("All")) {
      			Cards_inZone.addAll(AllZone.Human_Battlefield.getCards());
      			Cards_inZone.addAll(AllZone.Computer_Battlefield.getCards());
      			//this is a hack for Quick Sliver
      			if (Keyword_Details.length >= 2 
      					&& (Keyword_Details[2].contains("Flash") 
      					|| Keyword_Details[2].contains("CARDNAME can't be countered."))) {
      				Cards_inZone.addAll(AllZone.Human_Hand.getCards());
      				Cards_inZone.addAll(AllZone.Computer_Hand.getCards());
      				Cards_inZone.addAll(AllZone.Human_Graveyard.getCards());
      				Cards_inZone.addAll(AllZone.Computer_Graveyard.getCards());
      			}
      			//hack for Molten Disaster
      			/*
      			 // TODO for future use
      			if(Keyword_Details.length >= 2 && Keyword_Details[2].contains("Split second")) {
      				Cards_inZone.add(AllZone.Stack.getSpellCardsOnStack());
      			}
      			*/
      		}
      		if(Range.equals("Enchanted")) {
      			if (SourceCard.getEnchanting().size() > 0)
      				Cards_inZone.addAll(SourceCard.getEnchanting().toArray());
	      	}
      		
      		if(Range.equals("Equipped")) {
      			if (SourceCard.getEquipping().size() > 0)
      				Cards_inZone.addAll(SourceCard.getEquipping().toArray());
		    }
      		
			return Cards_inZone;
		}
	};
	
	public static Command stLandManaAbilities = new Command() {
		private static final long serialVersionUID = 8005448956536998277L;

		public void execute() {
			CardList lands = AllZoneUtil.getCardsInGame();
			lands = lands.filter(AllZoneUtil.lands);
			
			//remove all abilities granted by this Command
			for(Card land : lands) {
				ArrayList<Ability_Mana> sas = land.getManaAbility();
				for(SpellAbility sa : sas) {
					if(sa.getType().equals("BasicLandTypeMana")) {
						land.removeSpellAbility(sa);
					}
				}
			}
			
			//add all appropriate mana abilities based on current types
			for(Card land : lands) {
				if(land.isType("Swamp")) {
					AbilityFactory AF = new AbilityFactory();
					SpellAbility sa = AF.getAbility("AB$ Mana | Cost$ T | Produced$ B | SpellDescription$ Add B to your mana pool.", land);
					sa.setType("BasicLandTypeMana");
					land.addSpellAbility(sa);
				}
				if(land.isType("Forest")) {
					AbilityFactory AF = new AbilityFactory();
					SpellAbility sa = AF.getAbility("AB$ Mana | Cost$ T | Produced$ G | SpellDescription$ Add G to your mana pool.", land);
					sa.setType("BasicLandTypeMana");
					land.addSpellAbility(sa);
				}
				if(land.isType("Island")) {
					AbilityFactory AF = new AbilityFactory();
					SpellAbility sa = AF.getAbility("AB$ Mana | Cost$ T | Produced$ U | SpellDescription$ Add U to your mana pool.", land);
					sa.setType("BasicLandTypeMana");
					land.addSpellAbility(sa);
				}
				if(land.isType("Mountain")) {
					AbilityFactory AF = new AbilityFactory();
					SpellAbility sa = AF.getAbility("AB$ Mana | Cost$ T | Produced$ R | SpellDescription$ Add R to your mana pool.", land);
					sa.setType("BasicLandTypeMana");
					land.addSpellAbility(sa);
				}
				if(land.isType("Plains")) {
					AbilityFactory AF = new AbilityFactory();
					SpellAbility sa = AF.getAbility("AB$ Mana | Cost$ T | Produced$ W | SpellDescription$ Add W to your mana pool.", land);
					sa.setType("BasicLandTypeMana");
					land.addSpellAbility(sa);
				}
			}
		}// execute()

	};//stLandManaAbilities

	public static Command stSetPT = new Command() {
		/*
		 * Syntax: K:stSetPT:power:toughness:Description
		 * or (for Angry Mob/Gaea's Liege)
		 * K:stSetPT:power:toughness:condition:altPower:altToughness:Description
		 * or (for Levels)
		 * K:stSetPT:power:toughness:condition:altPower:altToughness:condition2:altPower2:altToughness2:Description
		 */
		private static final long serialVersionUID = -8019071015309088017L;

		public void execute() {
			//gather cards in all zones based on rule 112.6a
			CardList Cards_WithKeyword = AllZoneUtil.getCardsInGame();
			Cards_WithKeyword = Cards_WithKeyword.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					if(c.getKeyword().toString().contains("stSetPT")) return true;
					return false;
				}
			});

			// For each card found, find the keywords which are the stSetPT Keywords
			for(int i = 0; i < Cards_WithKeyword.size() ; i++) {
				Card card = Cards_WithKeyword.get(i);
				ArrayList<String> a = card.getKeyword();
				for(int x = 0; x < a.size(); x++) {
					if(a.get(x).toString().startsWith("stSetPT")) {
						String parse = card.getKeyword().get(x).toString();
						String k[] = parse.split(":");
						/*for(int z = 0; z < k.length; z++) {
							System.out.println("k["+z+"]: "+k[z]);
						}*/
						if(k.length < 2) {
							System.out.println("Error in stSetPT for: "+card.getName());
						}
						else {
							//TODO - add some checking here...?
							int power = 0;
							int toughness = 0;
							int altPower = 0;
							int altToughness = 0;
							boolean altCondition = false;
							
							int altPower2 = 0;
							int altToughness2 = 0;
							boolean altCondition2 = false;
							
							//double condition (for level creatures)
							if(k.length > 6) {
								String condition2 = k[6];
								if(condition2.startsWith("LevelGE")) {
									condition2 = condition2.replace("LevelGE", "");
									int levelReq = Integer.parseInt(condition2);
									//System.out.println("condition2, got level: "+levelReq);
									if(card.getCounters(Counters.LEVEL) >= levelReq) {
										altCondition2 = true;
									}
								}
							}
							//single condition (for Gaea's Liege/Angry Mob)
							if(k.length > 3) {
								String condition = k[3];
								if(condition.equals("isAttacking")
										&& card.isAttacking()) {
									altCondition = true;
								}
								else if(condition.equals("isYourTurn")
										&& AllZone.Phase.isPlayerTurn(card.getController())) {
									altCondition = true;
								}
								else if(condition.startsWith("LevelGE")) {
									condition = condition.replace("LevelGE", "");
									int levelReq = Integer.parseInt(condition);
									//System.out.println("condition, got level: "+levelReq);
									if(card.getCounters(Counters.LEVEL) >= levelReq) {
										altCondition = true;
									}
								}
							}
							
							if(altCondition2) {
								if(k.length > 6) {
									altPower2 = k[7].matches("[0-9][0-9]?") ? Integer.parseInt(k[7]) : CardFactoryUtil.xCount(card,k[7]);
								}
								if(k.length > 7) {
									altToughness2 = k[8].matches("[0-9][0-9]?") ? Integer.parseInt(k[8]) : CardFactoryUtil.xCount(card,k[8]);
								}
								card.setBaseAttack(altPower2);
								card.setBaseDefense(altToughness2);
							}
							else if(altCondition) {
								//System.out.println("In alt condition");
								//System.out.println("Setting power for ("+card.getName()+") to: "+altPower);
								//System.out.println("Setting toughness for ("+card.getName()+") to: "+altToughness);
								if(k.length > 4) {
									altPower = k[4].matches("[0-9][0-9]?") ? Integer.parseInt(k[4]) : CardFactoryUtil.xCount(card,k[4]);
								}
								if(k.length > 5) {
									altToughness = k[5].matches("[0-9][0-9]?") ? Integer.parseInt(k[5]) : CardFactoryUtil.xCount(card,k[5]);
								}
								card.setBaseAttack(altPower);
								card.setBaseDefense(altToughness);
							}
							else {
								//use the base power/toughness to calculate
								//System.out.println("Setting power for ("+card.getName()+") to: "+power);
								//System.out.println("Setting toughness for ("+card.getName()+") to: "+toughness);
								power = k[1].matches("[0-9][0-9]?") ? Integer.parseInt(k[1]) : CardFactoryUtil.xCount(card,k[1]);
								toughness = k[2].matches("[0-9][0-9]?") ? Integer.parseInt(k[2]) : CardFactoryUtil.xCount(card,k[2]);
								card.setBaseAttack(power);
								card.setBaseDefense(toughness);
							}
						}
						
					}
				}
			}
		}// execute()

	};//stSetPT
	
	public static Command Coat_of_Arms                 = new Command() {
		private static final long serialVersionUID   = 583505612126735693L;

		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			// reset all cards in list - aka "old" cards
			for(int i2 = 0; i2 < list.size(); i2++) {
				list.get(i2).addSemiPermanentAttackBoost(-1);
				list.get(i2).addSemiPermanentDefenseBoost(-1);
			}
			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Coat of Arms");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList();
				creature.addAll(AllZone.Human_Battlefield.getCards());
				creature.addAll(AllZone.Computer_Battlefield.getCards());

				for(int i = 0; i < creature.size(); i++) {
					final Card crd = creature.get(i);
					CardList Type = new CardList();
					Type.addAll(AllZone.Human_Battlefield.getCards());
					Type.addAll(AllZone.Computer_Battlefield.getCards());
					Type = Type.filter(new CardListFilter() {
						public boolean addCard(Card card) {
							return !card.equals(crd) && card.isCreature() && !crd.getName().equals("Mana Pool");
						}
					});
					CardList Already_Added = new CardList();
					for(int x = 0; x < Type.size(); x++) {
						Already_Added.clear();
						for(int x2 = 0; x2 < Type.get(x).getType().size(); x2++) {
							if(!Already_Added.contains(Type.get(x))) {
								if(!Type.get(x).getType().get(x2).equals("Creature") && !Type.get(x).getType().get(x2).equals("Legendary") 
										&& !Type.get(x).getType().get(x2).equals("Artifact") ) {	
									if(crd.getType().contains(Type.get(x).getType().get(x2)) || crd.getKeyword().contains("Changeling")
											|| Type.get(x).getKeyword().contains("Changeling")) {					
										Already_Added.add(Type.get(x));
										crd.addSemiPermanentAttackBoost(1);
										crd.addSemiPermanentDefenseBoost(1);
										gloriousAnthemList.add(crd);
									}
								}
							}
						}
					}
				}// for inner
			}// for outer
		}// execute
	}; // Coat of Arms
	
	
	public static Command Leyline_of_Singularity                = new Command() {

		private static final long serialVersionUID   = -67071835355151830L;
		CardList                  Leyline_of_Singularity_Tokens = new CardList();

		public void execute() {
			String Type = "Legendary";

			if(AllZoneUtil.isCardInPlay("Leyline of Singularity")) {
				CardList NonLand = new CardList();
				NonLand.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
				NonLand.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
				
				NonLand = NonLand.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return !c.isLand() && c.isPermanent() && !c.getName().equals("Mana Pool");
                    }
                });

				for(int i = 0; i < NonLand.size(); i++) {
					Card c = NonLand.get(i);
					ArrayList<String> Card_Types = c.getType();
					if(!c.getType().contains(Type)) {					
						ArrayList<String> NewCard_Type = new ArrayList<String>(Card_Types.size() + 1);
						NewCard_Type.add(0,Type);
						for(int x = 0; x < Card_Types.size(); x++) NewCard_Type.add(x + 1 , Card_Types.get(x));
						if(c.isToken()) Leyline_of_Singularity_Tokens.add(c);
						c.setType(NewCard_Type);
					}
				}
				} else {
    				CardList NonLand = new CardList();
    				NonLand.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
    				NonLand.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
    				
    				NonLand = NonLand.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isLand() && c.isPermanent() && !c.getName().equals("Mana Pool");
                        }
                    });
    				
    				for(int r = 0; r < NonLand.size(); r++) {
        				Card crd = NonLand.get(r);
        				if(!crd.isToken()) {
                        	Card c = AllZone.CardFactory.copyCard(crd); 
                        	if(!c.getType().contains("Legendary")) crd.removeType("Legendary");

        				} else if(Leyline_of_Singularity_Tokens.contains(crd) && AllZoneUtil.isCardInPlay(crd.getName())){
        					crd.removeType("Legendary");
        				}
    				
    				}
    				Leyline_of_Singularity_Tokens.clear();
				}
		}// execute()
	};
	
	/**
	 * stores the Command
	 */
	public static Command Umbra_Stalker = new Command() {
		private static final long serialVersionUID = -3500747003228938898L;

		public void execute() {
			// get all creatures
			CardList cards = AllZoneUtil.getCardsInPlay("Umbra Stalker");
			for(Card c:cards) {
				Player player = c.getController();
				CardList grave = AllZoneUtil.getPlayerGraveyard(player);
				int pt = CardFactoryUtil.getNumberOfManaSymbolsByColor("B", grave);
				c.setBaseAttack(pt);
				c.setBaseDefense(pt);
			}
		}// execute()
	};
	
	public static Command The_Tabernacle_at_Pendrell_Vale                 = new Command() {
		private static final long serialVersionUID   = -3233715310427996429L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "At the beginning of your upkeep, destroy this creature unless you pay";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				ArrayList<String> a = c.getKeyword();
				for(String s:a) {
					if(s.startsWith(keyword)) c.removeExtrinsicKeyword(s);
				}
			}

			list.clear();
			
			CardList clist = AllZoneUtil.getCardsInPlay("The Tabernacle at Pendrell Vale");

			int number = clist.size();
			//System.out.println("Tabernacle Number:" + number);
			if(number > 0) {
				CardList creature = AllZoneUtil.getCreaturesInPlay();

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword(keyword + " " + number
							+ ".");
					gloriousAnthemList.add(c);
				}// for inner
			}
		}// execute()
	};

	public static Command Magus_of_the_Tabernacle     = new Command() {
		private static final long serialVersionUID   = -249708982895077034L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "At the beginning of your upkeep, sacrifice this creature unless you pay";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				ArrayList<String> a = c.getKeyword();
				for(String s:a) {
					if(s.startsWith(keyword)) c.removeExtrinsicKeyword(s);
				}
			}

			list.clear();
			CardList clist = AllZoneUtil.getCardsInPlay("Magus of the Tabernacle");

			int number = clist.size();
			//System.out.println("Tabernacle Number:" + number);
			if(number > 0) {
				CardList creature = AllZoneUtil.getCreaturesInPlay();

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword(keyword + " " + number
							+ ".");
					gloriousAnthemList.add(c);
				}// for inner
			}
		}// execute()
	};

	public static Command Ajani_Avatar_Token          = new Command() {
		private static final long serialVersionUID = 3027329837165436727L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getName().equals("Avatar")
						&& c.getImageName().equals("W N N Avatar");
				}
			});
			for(int i = 0; i < list.size(); i++) {
				Card card = list.get(i);
				int n = card.getController().getLife();
				card.setBaseAttack(n);
				card.setBaseDefense(n);
			}// for
		}// execute
	}; // Ajani Avatar
	
	public static Command Old_Man_of_the_Sea = new Command() {
		private static final long serialVersionUID = 8076177362922156784L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Old Man of the Sea");
			for(Card oldman:list) {
				if(!oldman.getGainControlTargets().isEmpty()) {
					if(oldman.getNetAttack() < oldman.getGainControlTargets().get(0).getNetAttack()) {
						ArrayList<Command> coms = oldman.getGainControlReleaseCommands();
						for(int i = 0; i < coms.size(); i++) {
							coms.get(i).execute();
						}
					}
				}
			}
		}
	};//Old Man of the Sea
	
	public static Command Homarid = new Command() {
		private static final long serialVersionUID = 7156319758035295773L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Homarid");

			for(Card homarid:list) {
				int tide = homarid.getCounters(Counters.TIDE);
				if(tide == 4) {
					homarid.setCounter(Counters.TIDE, 0, true);
				}
			}
		}// execute()
	};
	
	public static Command Dauntless_Escort = new Command() {
		private static final long serialVersionUID = -2201201455269804L;
		CardList  old = new CardList();	
		public void execute() {

			// Human Activates Dauntless Escort
			PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Battlefield,AllZone.HumanPlayer);
			CardList PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
			PlayerCreatureList = PlayerCreatureList.getType("Creature");
			PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Battlefield,AllZone.ComputerPlayer);
			CardList opponentCreatureList = new CardList(opponentPlayZone.getCards());
			opponentCreatureList = opponentCreatureList.getType("Creature");
			if(Phase.isSacDauntlessEscort() == true) {
				if(PlayerCreatureList.size() != 0) {
					for(int i = 0; i < PlayerCreatureList.size(); i++) {
						Card c = PlayerCreatureList.get(i);
						c.removeExtrinsicKeyword("Indestructible");	
						c.addExtrinsicKeyword("Indestructible");
					}
				}
				if(opponentCreatureList.size() != 0) {
					for(int i = 0; i < opponentCreatureList.size(); i++) {
						Card c = opponentCreatureList.get(i);
						if(c.getOwner() == AllZone.HumanPlayer) {
							if(old.size() == 0) {
								c.removeExtrinsicKeyword("Indestructible");	 
								old.add(c);              			
							}
							for(int x = 0; x < old.size(); x++) {
								if(old.get(x) == c) break;
								c.removeExtrinsicKeyword("Indestructible");	 
								old.add(c);
							}
						}
					}
				}
			}            

			// Computer Activates Dauntless Escort
			PlayerPlayZone = AllZone.getZone(Constant.Zone.Battlefield,AllZone.ComputerPlayer);
			PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
			PlayerCreatureList = PlayerCreatureList.getType("Creature");
			opponentPlayZone = AllZone.getZone(Constant.Zone.Battlefield,AllZone.HumanPlayer);
			opponentCreatureList = new CardList(opponentPlayZone.getCards());
			opponentCreatureList = opponentCreatureList.getType("Creature");
			if(Phase.isSacDauntlessEscortAI() == true) {
				if(PlayerCreatureList.size() != 0) {
					for(int i = 0; i < PlayerCreatureList.size(); i++) {
						Card c = PlayerCreatureList.get(i);
						c.removeExtrinsicKeyword("Indestructible");	
						c.addExtrinsicKeyword("Indestructible");
					}
				}
				if(opponentCreatureList.size() != 0) {
					for(int i = 0; i < opponentCreatureList.size(); i++) {
						Card c = opponentCreatureList.get(i);
						if(c.getOwner() == AllZone.ComputerPlayer) {
							if(old.size() == 0) {
								c.removeExtrinsicKeyword("Indestructible");	 
								old.add(c);              			
							}
							for(int x = 0; x < old.size(); x++) {
								if(old.get(x) == c) break;
								c.removeExtrinsicKeyword("Indestructible");	 
								old.add(c);
							}
						}
					}
				}            
			} 			
		}// execute()

	};

	public static Command Liu_Bei                     = new Command() {

		private static final long serialVersionUID = 4235093010715735727L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Liu Bei, Lord of Shu");

			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {

					Card c = list.get(i);
					if(getsBonus(c)) {
						c.setBaseAttack(4);
						c.setBaseDefense(6);
					} else {
						c.setBaseAttack(2);
						c.setBaseDefense(4);
					}

				}
			}
		}// execute()

		private boolean getsBonus(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, c.getController());

			CardList list = new CardList();
			list.addAll(play.getCards());
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getName().equals(
							"Guan Yu, Sainted Warrior")
							|| c.getName().equals(
									"Zhang Fei, Fierce Warrior");
				}

			});

			return list.size() > 0;
		}

	}; //Liu_Bei

	public static Command Plague_Rats = new Command() {
		private static final long serialVersionUID = 2333292591304646698L;

		public void execute() {

			CardList rats = AllZoneUtil.getCardsInPlay("Plague Rats");
			for(Card rat:rats) {
				rat.setBaseAttack(rats.size());
				rat.setBaseDefense(rats.size());
			}
		}// execute()

	};//Plague_Rats
	
	public static Command Phylactery_Lich             = new Command() {

		private static final long serialVersionUID = -1606115081917467754L;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay("Phylactery Lich");
			int size = creature.size();
			
			for(int i = 0; i < size; i++) {
				Card c = creature.get(i);
				if(!phylacteryExists(c) && c.getFinishedEnteringBF()) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()

		private boolean phylacteryExists(Card c) {
			CardList play = AllZoneUtil.getPlayerCardsInPlay(c.getController());
			play = play.filter(new CardListFilter()
			{
				public boolean addCard(Card crd)
				{
					return crd.getCounters(Counters.PHYLACTERY) > 0;
				}
			});
			return play.size() > 0;
		}
	};//Phylactery_Lich
	
	public static Command topCardReveal_Update      = new Command() {

		private static final long serialVersionUID = 8669404698350637963L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"Play with the top card of your library revealed.");
				}
			});

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if (CardFactoryUtil.getTopCard(c)!= null)
					c.setTopCardName(CardFactoryUtil.getTopCard(c).getName());
			}

		}//execute()
	};//topCardReveal_Update

	public static Command Sacrifice_NoIslands         = new Command() {

		private static final long serialVersionUID = 8064452222949253952L;
		int                       islands          = 0;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay();

			creature = creature.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"When you control no Islands, sacrifice CARDNAME.");
				}
			});

			for(int i = 0; i < creature.size(); i++) {
				Card c = creature.get(i);
				islands = countIslands(c);
				if(islands == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()

		private int countIslands(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, c.getController());
			CardList islands = new CardList(play.getCards());
			islands = islands.getType("Island");
			return islands.size();
		}

	};//Sacrifice_NoIslands
	
	public static Command Sacrifice_NoForests = new Command() {
		private static final long serialVersionUID = -5310856079162962126L;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay();

			creature = creature.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"When you control no Forests, sacrifice CARDNAME.");
				}
			});

			for(Card c:creature) {
				if(AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Forest").size() == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoForests
	
	public static Command Sacrifice_NoSwamps = new Command() {
		private static final long serialVersionUID = 1961985826678794078L;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay();

			creature = creature.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"When you control no Swamps, sacrifice CARDNAME.");
				}
			});

			for(Card c:creature) {
				if(AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Swamp").size() == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoForests
	
	public static Command Sacrifice_NoArtifacts = new Command() {
		private static final long serialVersionUID = -2546650213674544590L;
		int artifacts = 0;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains("When you control no artifacts, sacrifice CARDNAME.");
				}
			});

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				artifacts = AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Artifact").size();
				if(artifacts == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoArtifacts
	
	private static Command Sacrifice_NoEnchantments = new Command() {
		private static final long serialVersionUID = -8280843743243927861L;
		int enchs = 0;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains("When you control no enchantments, sacrifice CARDNAME.");
				}
			});

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				enchs = AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Enchantment").size();
				if(enchs == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoEnchantments
	
	public static Command Sacrifice_NoLands = new Command() {
		private static final long serialVersionUID = 2768929064034728027L;

		public void execute() {
			CardList cards = AllZoneUtil.getCardsInPlay();

			cards = cards.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"When there are no lands on the battlefield, sacrifice CARDNAME.");
				}
			});

			for(Card c:cards) {
				if(AllZoneUtil.getLandsInPlay().size() == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoLands
	
	public static Command Sacrifice_NoCreatures = new Command() {
		private static final long serialVersionUID = -177976088524215734L;

		public void execute() {
			CardList cards = AllZoneUtil.getCardsInPlay();

			cards = cards.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains(
							"When there are no creatures on the battlefield, sacrifice CARDNAME.");
				}
			});

			for(Card c:cards) {
				if(AllZoneUtil.getCreaturesInPlay().size() == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	};//Sacrifice_NoCreatures
	
	private static Command Sacrifice_NoOtherCreatures = new Command() {
		private static final long serialVersionUID = 6941452572773927921L;

		public void execute() {
			CardList cards = AllZoneUtil.getCardsInPlay();

			cards = cards.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getKeyword().contains("When you control no other creatures, sacrifice CARDNAME.");
				}
			});

			for(Card c:cards) {
				if(AllZoneUtil.getCreaturesInPlay(c.getController()).size() == 1) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()
	}; //Sacrifice_NoOtherCreatures

	public static Command Sound_the_Call_Wolf      = new Command() {
		private static final long serialVersionUID = 4614281706799537283L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();
			list = list.filter(new CardListFilter(){
				public boolean addCard(Card c)
				{
					return c.getName().equals("Wolf") && c.getKeyword().contains("This creature gets +1/+1 for each card named Sound the Call in each graveyard.");
				}
			});

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(1 + countSoundTheCalls());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countSoundTheCalls() {
			CardList list = AllZoneUtil.getCardsInGraveyard();
			list = list.getName("Sound the Call");
			return list.size();
		}

	}; //Sound_the_Call_Wolf

	public static Command Tarmogoyf                   = new Command() {
		private static final long serialVersionUID = 5895665460018262987L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Tarmogoyf");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countDiffTypes());
				c.setBaseDefense(c.getBaseAttack() + 1);
			}

		}// execute()

		private int countDiffTypes() {
			CardList list = AllZoneUtil.getCardsInGraveyard();

			int count = 0;
			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isCreature()) {
					count++;
					break;
				}
			}
			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isSorcery()) {
					count++;
					break;
				}
			}
			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isInstant()) {
					count++;
					break;
				}
			}
			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isArtifact()) {
					count++;
					break;
				}
			}

			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isEnchantment()) {
					count++;
					break;
				}
			}

			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isLand()) {
					count++;
					break;
				}
			}

			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isPlaneswalker()) {
					count++;
					break;
				}
			}

			for(int q = 0; q < list.size(); q++) {
				if(list.get(q).isTribal()) {
					count++;
					break;
				}
			}
			return count;
		}
	};


	public static Command Filth                       = new Command() {
		private static final long serialVersionUID = -8423145847505L;

		CardList                  old              = new CardList();
		CardList                  next             = new CardList();

		public void execute() {
			if(Phase.getGameBegins() == 1) {
			// reset creatures
			removeSwampwalk(old);

			if(isInGrave(AllZone.ComputerPlayer)) addSwampwalk(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addSwampwalk(AllZone.HumanPlayer);
			}
		}// execute()

		void addSwampwalk(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
			CardList playlist = new CardList(play.getCards());
			playlist = playlist.getType("Creature");
			for(int i = 0; i < playlist.size(); i++) {
				if(!old.contains(playlist.get(i))) next.add(playlist.get(i));
			}
			// add creatures to "old" or previous list of creatures
			
			
			addSwampwalk(next);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Swamp");


			if(!list.containsName("Filth") || lands.size() == 0) return false;
			else return true;
		}

		void removeSwampwalk(CardList list) {
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(!isInGrave(c.getController()) && old.contains(c)) {
					List_Copy.get(i).removeExtrinsicKeyword("Swampwalk");
					old.remove(c);
				}
		}
		}

		void addSwampwalk(CardList list) {
			int Count = list.size();
			for(int i = 0; i < Count; i++) {
				list.get(i).addExtrinsicKeyword("Swampwalk");
				old.add(list.get(i));
			}
		}
	}; // Flith
	
	public static Command Valor                       = new Command() {
		private static final long serialVersionUID = -846781470342847505L;

		CardList                  old              = new CardList();
		CardList                  next             = new CardList();

		public void execute() {
			if(Phase.getGameBegins() == 1) {
			// reset creatures
			removeFirstStrike(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFirstStrike(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFirstStrike(AllZone.HumanPlayer);
			}
		}// execute()

		void addFirstStrike(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
			CardList playlist = new CardList(play.getCards());
			playlist = playlist.getType("Creature");
			for(int i = 0; i < playlist.size(); i++) {
				if(!old.contains(playlist.get(i))) next.add(playlist.get(i));
			}
			// add creatures to "old" or previous list of creatures
			
			
			addFirstStrike(next);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Plains");


			if(!list.containsName("Valor") || lands.size() == 0) return false;
			else return true;
		}

		void removeFirstStrike(CardList list) {
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(!isInGrave(c.getController()) && old.contains(c)) {
					List_Copy.get(i).removeExtrinsicKeyword("First Strike");
					old.remove(c);
				}
		}
		}

		void addFirstStrike(CardList list) {
			int Count = list.size();
			for(int i = 0; i < Count; i++) {
				list.get(i).addExtrinsicKeyword("First Strike");
				old.add(list.get(i));
			}
		}
	}; // Valor

	public static Command Anger                       = new Command() {
		private static final long serialVersionUID = -8463420545847505L;

		CardList                  old              = new CardList();
		CardList                  next             = new CardList();

		public void execute() {
			if(Phase.getGameBegins() == 1) {
			// reset creatures
			removeHaste(old);

			if(isInGrave(AllZone.ComputerPlayer)) addHaste(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addHaste(AllZone.HumanPlayer);
			}
		}// execute()

		void addHaste(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
			CardList playlist = new CardList(play.getCards());
			playlist = playlist.getType("Creature");
			for(int i = 0; i < playlist.size(); i++) {
				if(!old.contains(playlist.get(i))) next.add(playlist.get(i));
			}
			// add creatures to "old" or previous list of creatures
			
			
			addHaste(next);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Mountain");


			if(!list.containsName("Anger") || lands.size() == 0) return false;
			else return true;
		}

		void removeHaste(CardList list) {
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(!isInGrave(c.getController()) && old.contains(c)) {
					List_Copy.get(i).removeExtrinsicKeyword("Haste");
					old.remove(c);
				}
			}
		}

		void addHaste(CardList list) {
			int Count = list.size();
			for(int i = 0; i < Count; i++) {
				list.get(i).addExtrinsicKeyword("Haste");
				old.add(list.get(i));
			}
		}
	}; // Anger

	public static Command Wonder                      = new Command() {
		private static final long serialVersionUID = -846723300545847505L;

		CardList                  old              = new CardList();
		CardList                  next             = new CardList();

		public void execute() {
			if(Phase.getGameBegins() == 1) {
			// reset creatures
			removeFlying(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFlying(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFlying(AllZone.HumanPlayer);
			}
		}// execute()

		void addFlying(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
			CardList playlist = new CardList(play.getCards());
			playlist = playlist.getType("Creature");
			for(int i = 0; i < playlist.size(); i++) {
				if(!old.contains(playlist.get(i))) next.add(playlist.get(i));
			}
			// add creatures to "old" or previous list of creatures
			
			
			addFlying(next);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Island");


			if(!list.containsName("Wonder") || lands.size() == 0) return false;
			else return true;
		}

		void removeFlying(CardList list) {
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(!isInGrave(c.getController()) && old.contains(c)) {
					List_Copy.get(i).removeExtrinsicKeyword("Flying");
					old.remove(c);
				}
		}
		}

		void addFlying(CardList list) {
			int Count = list.size();
			for(int i = 0; i < Count; i++) {
				list.get(i).addExtrinsicKeyword("Flying");
				old.add(list.get(i));
			}
		}
	}; // Wonder

	public static Command Brawn                       = new Command() {
		private static final long serialVersionUID = -8467814700545847505L;

		CardList                  old              = new CardList();
		CardList                  next             = new CardList();

		public void execute() {
			if(Phase.getGameBegins() == 1) {
			// reset creatures
			removeTrample(old);

			if(isInGrave(AllZone.ComputerPlayer)) addTrample(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addTrample(AllZone.HumanPlayer);
			}
		}// execute()

		void addTrample(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
			CardList playlist = new CardList(play.getCards());
			playlist = playlist.getType("Creature");
			for(int i = 0; i < playlist.size(); i++) {
				if(!old.contains(playlist.get(i))) next.add(playlist.get(i));
			}
			// add creatures to "old" or previous list of creatures
			
			
			addTrample(next);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Battlefield, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Forest");


			if(!list.containsName("Brawn") || lands.size() == 0) return false;
			else return true;
		}

		void removeTrample(CardList list) {
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(!isInGrave(c.getController()) && old.contains(c)) {
					List_Copy.get(i).removeExtrinsicKeyword("Trample");
					old.remove(c);
				}
		}
		}

		void addTrample(CardList list) {
			int Count = list.size();
			for(int i = 0; i < Count; i++) {
				list.get(i).addExtrinsicKeyword("Trample");
				old.add(list.get(i));
			}
		}
	}; // Brawn

	public static Command Muraganda_Petroglyphs       = new Command() {
		private static final long serialVersionUID   = -6715848091817213517L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.addSemiPermanentAttackBoost(-2);
				c.addSemiPermanentDefenseBoost(-2);
			}

			// add +2/+2 to vanilla cards
			list.clear();
			PlayerZone[] zone = getZone("Muraganda Petroglyphs");

			// for each zone found add +2/+2 to each vanilla card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = AllZoneUtil.getCreaturesInPlay();

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					if((( c.getAbilityText().trim().equals("") || c.isFaceDown()) && c.getUnhiddenKeyword().size() == 0)) {
						c.addSemiPermanentAttackBoost(2);
						c.addSemiPermanentDefenseBoost(2);

						gloriousAnthemList.add(c);
					}

				}// for inner
			}// for outer
		}// execute()
	}; // Muraganda_Petroglyphs
	
	public static Command Meddling_Mage               = new Command() {
		private static final long serialVersionUID   = 738264163993370439L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				//c.removeIntrinsicKeyword("This card can't be cast");
				c.setUnCastable(false);
			}

			list.clear();

			CardList cl = AllZoneUtil.getCardsInPlay("Meddling Mage");

			for(int i = 0; i < cl.size(); i++) {
				final Card crd = cl.get(i);

				CardList spells = new CardList();
				spells.addAll(AllZone.Human_Graveyard.getCards());
				spells.addAll(AllZone.Human_Hand.getCards());
				spells.addAll(AllZone.Computer_Hand.getCards());
				spells.addAll(AllZone.Computer_Graveyard.getCards());
				spells = spells.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return !c.isLand()
						&& c.getName().equals(
								crd.getNamedCard());
					}
				});

				for(int j = 0; j < spells.size(); j++) {
					c = spells.get(j);
					if(!c.isLand()) {
						//c.addIntrinsicKeyword("This card can't be cast");
						c.setUnCastable(true);
						gloriousAnthemList.add(c);
					}
				}// for inner
			}// for outer
		}// execute()
	}; // Meddling_Mage

	public static Command Gaddock_Teeg                = new Command() {
		private static final long serialVersionUID   = -479252814191086571L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				//c.removeIntrinsicKeyword("This card can't be cast");
				c.setUnCastable(false);
			}

			list.clear();
			
			CardList cl = AllZoneUtil.getCardsInPlay("Gaddock Teeg");

			for(int i = 0; i < cl.size(); i++) {
				CardList spells = new CardList();
				spells.addAll(AllZone.Human_Graveyard.getCards());
				spells.addAll(AllZone.Human_Hand.getCards());
				spells.addAll(AllZone.Computer_Hand.getCards());
				spells.addAll(AllZone.Computer_Graveyard.getCards());


				spells = spells.filter(new CardListFilter() {
					public boolean addCard(Card c) {

						boolean isXNonCreature = false;
						if (c.getSpellAbility().length > 0)
						{
							if (c.getSpellAbility()[0].isXCost())
								isXNonCreature = true;
						}

						return !c.isLand()
						&& !c.isCreature()
						&& (CardUtil.getConvertedManaCost(c.getManaCost()) >= 4 || isXNonCreature);
					}
				});

				for(int j = 0; j < spells.size(); j++) {
					c = spells.get(j);
					if(!c.isLand()) {
						c.setUnCastable(true);
						gloriousAnthemList.add(c);
					}
				}// for inner
			}// for outer
		}// execute()
	}; //

	public static Command Iona_Shield_of_Emeria       = new Command() {
		private static final long serialVersionUID   = 7349652597673216545L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				//c.removeIntrinsicKeyword("This card can't be cast");
				c.setUnCastable(false);
			}

			list.clear();
			
			CardList cl = AllZoneUtil.getCardsInPlay("Iona, Shield of Emeria");

			for(int i = 0; i < cl.size(); i++) {
				final Card crd = cl.get(i);
				Player controller = cl.get(i).getController();
				Player opp = controller.getOpponent();

				CardList spells = new CardList();
				PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, opp);
				PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opp);

				spells.addAll(grave.getCards());
				spells.addAll(hand.getCards());

				spells = spells.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return !c.isLand()
						&& CardUtil.getColors(c).contains(
								crd.getChosenColor());
					}
				});

				for(int j = 0; j < spells.size(); j++) {
					c = spells.get(j);
					if(!c.isLand()) {
						c.setUnCastable(true);
						gloriousAnthemList.add(c);
					}
				}// for inner
			}// for outer
		}// execute()
	}; //end Iona, Shield of Emeria
	
	// returns all PlayerZones that has at least 1 Glorious Anthem
	// if Computer has 2 Glorious Anthems, AllZone.Computer_Play will be
	// returned twice
	private static PlayerZone[] getZone(String cardName) {
		CardList all = AllZoneUtil.getCardsInPlay();

		ArrayList<PlayerZone> zone = new ArrayList<PlayerZone>();
		for(int i = 0; i < all.size(); i++) {
			Card c = all.get(i);
			if(c.getName().equals(cardName) && !c.isFaceDown()) zone.add(AllZone.getZone(c));
		}

		PlayerZone[] z = new PlayerZone[zone.size()];
		zone.toArray(z);
		return z;
	}

	public static HashMap<String, Command> commands = new HashMap<String, Command>();
	static {
		//Please add cards in alphabetical order so they are easier to find
		
		commands.put("Ajani_Avatar_Token", Ajani_Avatar_Token);
		commands.put("Coat_of_Arms", Coat_of_Arms);
		commands.put("Conspiracy", Conspiracy);
		commands.put("Elspeth_Emblem", Elspeth_Emblem);
		
		commands.put("Gaddock_Teeg", Gaddock_Teeg);
		//commands.put("Gemhide_Sliver", Gemhide_Sliver);
		commands.put("Homarid", Homarid);
		commands.put("Iona_Shield_of_Emeria", Iona_Shield_of_Emeria);
		
		// Card converted to script
		// commands.put("Joiner_Adept", Joiner_Adept);
		
		commands.put("Koth_Emblem", Koth_Emblem);
		commands.put("Leyline_of_Singularity", Leyline_of_Singularity);
		commands.put("Liu_Bei", Liu_Bei);
		
		commands.put("Magus_of_the_Tabernacle", Magus_of_the_Tabernacle);
		commands.put("Meddling_Mage", Meddling_Mage);
		commands.put("Mul_Daya_Channelers", Mul_Daya_Channelers);
		commands.put("Muraganda_Petroglyphs", Muraganda_Petroglyphs);
		
		commands.put("Old_Man_of_the_Sea", Old_Man_of_the_Sea);
		commands.put("Phylactery_Lich", Phylactery_Lich);
		commands.put("Plague_Rats", Plague_Rats);
		
		commands.put("Sound_the_Call_Wolf", Sound_the_Call_Wolf);
		commands.put("The_Tabernacle_at_Pendrell_Vale", The_Tabernacle_at_Pendrell_Vale);
		commands.put("Tarmogoyf", Tarmogoyf);
		
		commands.put("Umbra_Stalker", Umbra_Stalker);
		
		///The commands above are in alphabetical order by cardname.
	}

}
