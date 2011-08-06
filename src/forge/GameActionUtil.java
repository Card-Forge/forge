
package forge;


import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;


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
		upkeep_Defiler_of_Souls();
		upkeep_Yawgmoth_Demon();
		upkeep_Lord_of_the_Pit();
		upkeep_Drop_of_Honey();
		upkeep_Planar_Collapse();
		upkeep_Genesis();
		upkeep_Demonic_Hordes();
		upkeep_Phyrexian_Arena();
		upkeep_Fallen_Empires_Storage_Lands();
		upkeep_Master_of_the_Wild_Hunt();
		upkeep_Carnophage();
		upkeep_Sangrophage();
		upkeep_Honden_of_Cleansing_Fire();
		upkeep_Honden_of_Seeing_Winds();
		upkeep_Honden_of_Lifes_Web();
		upkeep_Honden_of_Nights_Reach();
		upkeep_Honden_of_Infinite_Rage();
		upkeep_Vensers_Journal();
		upkeep_Land_Tax();
		upkeep_Mana_Vault();
		upkeep_Feedback();
		upkeep_Unstable_Mutation();
		upkeep_Warp_Artifact();
		upkeep_Soul_Bleed();
		upkeep_Wanderlust();
		upkeep_Curse_of_Chains();
		upkeep_Festering_Wound_Counter();
		upkeep_Festering_Wound_Damage();
		upkeep_Kemba_Kha_Regent();
		upkeep_Greener_Pastures();
		upkeep_Wort();
		upkeep_Squee();
		upkeep_Sporesower_Thallid();
		upkeep_Dragonmaster_Outcast();
		upkeep_Scute_Mob();
		upkeep_Lichenthrope();
		upkeep_Heartmender();
		//upkeep_AEther_Vial();
		upkeep_Ratcatcher();
		upkeep_Nath();
		upkeep_Anowon();
		upkeep_Cunning_Lethemancer();
		
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
		
		upkeep_Debtors_Knell();
		upkeep_Reya();
		upkeep_Emeria();
		upkeep_Oversold_Cemetery();
		upkeep_Nether_Spirit();
		upkeep_Nettletooth_Djinn();
		upkeep_Fledgling_Djinn();
		upkeep_Juzam_Djinn();
		upkeep_Grinning_Demon();
		upkeep_Moroii();
		upkeep_Vampire_Lacerator();
		upkeep_Seizan_Perverter_of_Truth();
		upkeep_Serendib_Efreet();
		upkeep_Sleeper_Agent();
		upkeep_Cursed_Land();
		upkeep_Pillory_of_the_Sleepless();
		upkeep_Creakwood_Liege();
		upkeep_Bringer_of_the_Green_Dawn();
		upkeep_Bringer_of_the_Blue_Dawn();
		upkeep_Bringer_of_the_White_Dawn();
		upkeep_Murkfiend_Liege();
		upkeep_Seedborn_Muse();
		upkeep_Mirror_Sigil_Sergeant();
		//upkeep_Luminous_Angel();
		upkeep_Verdant_Force();
		upkeep_Dragon_Broodmother(); //put this before bitterblossom and mycoloth, so that they will resolve FIRST
		upkeep_Bitterblossom();
		upkeep_Goblin_Assault();
		upkeep_Awakening_Zone();
		upkeep_Nut_Collector();
		
		// Win / Lose	
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone OpplayZone = AllZone.getZone(Constant.Zone.Play, player.getOpponent());
		CardList Platinumlist = new CardList(OpplayZone.getCards());
		Platinumlist = Platinumlist.getName("Platinum Angel");
		CardList Abyssallist = new CardList(playZone.getCards());
		Abyssallist = Abyssallist.getName("Abyssal Persecutor");
		if(Platinumlist.size() == 0 && Abyssallist.size() == 0) {
			upkeep_Battle_of_Wits();
			upkeep_Mortal_Combat();
			upkeep_Epic_Struggle();
			upkeep_Near_Death_Experience();
			upkeep_Test_of_Endurance();
			upkeep_Helix_Pinnacle();
			upkeep_Barren_Glory();
			upkeep_Felidar_Sovereign();
			upkeep_Klass();
		}
		//Win / Lose
		
		upkeep_Convalescence();
		upkeep_Convalescent_Care();
		upkeep_Ancient_Runes();
		upkeep_Karma();
		upkeep_Defense_of_the_Heart();
		upkeep_Oath_of_Druids();
		upkeep_Oath_of_Ghouls();
		upkeep_Mycoloth();
		upkeep_Spore_Counters();
		upkeep_Suspend();
		upkeep_Vanishing();
		upkeep_Fading();
		upkeep_Benthic_Djinn();
		upkeep_Masticore();
		upkeep_Eldrazi_Monument();
		upkeep_Blaze_Counters();
		upkeep_Dark_Confidant(); // keep this one semi-last

		upkeep_Copper_Tablet();
		upkeep_Sulfuric_Vortex();
		upkeep_Power_Surge();
		upkeep_The_Rack();
		upkeep_Storm_World();
		upkeep_Black_Vise();
		upkeep_Ebony_Owl_Netsuke();
		upkeep_Ivory_Tower();		
		
		upkeep_AI_Aluren(); 
		// experimental, AI abuse aluren
		
		AllZone.Stack.unfreezeStack();
		
		if (AllZone.Stack.size() == 0 && !AllZone.Display.stopAtPhase(player, Constant.Phase.Upkeep)) 
	    	AllZone.Phase.setNeedToNextPhase(true);
	}

	public static void executeDrawStepEffects() {
		AllZone.Stack.freezeStack();
		final Player player = AllZone.Phase.getPlayerTurn();
		draw_Teferi_Puzzle_Box(player);
		draw_Howling_Mine(player);
		draw_Spiteful_Visions(player);
		draw_Kami_Crescent_Moon(player);
		draw_Font_of_Mythos(player);
		draw_Overbeing_of_Myth(player);
		draw_Mana_Vault(player);
		draw_Sylvan_Library(player);
		draw_Armageddon_Clock(player);
		AllZone.Stack.unfreezeStack();
		if (AllZone.Stack.size() == 0 && !AllZone.Display.stopAtPhase(player, Constant.Phase.Draw)) 
	    	AllZone.Phase.setNeedToNextPhase(true);
	}

	public static void executeTapSideEffects(Card c) {
		
		AllZone.GameAction.checkWheneverKeyword(c,"BecomesTapped",null);
		final Player activePlayer = AllZone.Phase.getPlayerTurn();

		/* cards with Tap side effects can be listed here, just like in
		 * the CardFactory classes
		 */
		if(c.getName().equals("City of Brass")) {
			final Player player = c.getController();
			final Card crd = c;
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					player.addDamage(1, crd);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("City of Brass deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}//end City of Brass
		
		if(c.getType().contains("Mountain")) {
			final Player opponent = c.getController().getOpponent();
			final CardList lifebloods = AllZoneUtil.getPlayerCardsInPlay(opponent, "Lifeblood");
			for(Card lifeblood:lifebloods) {
				final Card source = lifeblood;
				Ability ability = new Ability(source, "0") {
					@Override
					public void resolve() {
						//Lifeblood controller (opponent in this case) gains 1 life
						opponent.gainLife(1, source);
					}
				};//Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(lifeblood.getName()).append(" - Mountain was tapped, ").append(opponent).append(" gains 1 life.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}//for
		}//end Lifeblood
		
		/*
		 * Whenever a Forest an opponent controls becomes tapped, you gain 1 life.
		 */
		if(c.getType().contains("Forest")) {
			final Player opponent = c.getController().getOpponent();
			final CardList lifetaps = AllZoneUtil.getPlayerCardsInPlay(opponent, "Lifetap");
			for(Card lifetap:lifetaps) {
				final Card source = lifetap;
				Ability ability = new Ability(source, "0") {
					@Override
					public void resolve() {
						//Lifetap controller (opponent in this case) gains 1 life
						opponent.gainLife(1, source);
					}
				};//Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(lifetap.getName()).append(" - Forest was tapped, ").append(opponent).append(" gains 1 life.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}//for
		}//end Lifetap
		
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
					
					AllZone.Stack.add(ability);
				}
			}
		}//end Blight
		
		/*
		 * Relic Putrescense - When enchanted artifact becomes tapped,
		 * controller gets a poison counter.
		 */
		if(c.isEnchantedBy("Relic Putrescence")) {
			final ArrayList<Card> auras = c.getEnchantedBy();
			final Card target = c;
			for(Card aura:auras) {
				if(aura.getName().equals("Relic Putrescence")) {
					Ability ability = new Ability(aura, "0") {
						@Override
						public void resolve() {
							if (target.getController().equals(AllZone.HumanPlayer))
								AllZone.HumanPlayer.addPoisonCounters(1);
							else
								AllZone.ComputerPlayer.addPoisonCounters(1);
						}
					};//Ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(aura.getName()).append(" - ").append(target.getController()).append(" gets a poison counter.");
					ability.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability);
				}
			}
		}//end relic putrescence
		
		/*
		 * Psychic Venom - When enchanted land becomes tapped, it deals 2 damage
		 * to enchanted lands' controller
		 */
		if(c.isEnchantedBy("Psychic Venom")) {
			final ArrayList<Card> cards = c.getEnchantedBy();
			for(Card card:cards) {
				final Card source = card;
				if(card.getName().equals("Psychic Venom")) {
					Ability ability = new Ability(card, "0") {
						@Override
						public void resolve() {
							activePlayer.addDamage(2, source);
						}
					};//Ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - deals 2 damage to ").append(activePlayer);
					ability.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability);
				}
			}
		}//end Psychic Venom
		
		/*
		 * Insolence - Whenever enchanted creature becomes 
		 * tapped, Insolence deals 2 damage to that creature's controller.
		 */
		if(c.isEnchantedBy("Insolence")) {
			final ArrayList<Card> cards = c.getEnchantedBy();
			for(Card card:cards) {
				final Card source = card;
				if(card.getName().equals("Insolence")) {
					Ability ability = new Ability(card, "0") {
						@Override
						public void resolve() {
							activePlayer.addDamage(2, source);
						}
					};//Ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - deals 2 damage to ").append(activePlayer);
					ability.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability);
				}
			}
		}//end Insolence
		
		/*
		 * Whenever enchanted creature becomes tapped, put a -0/-2 counter on it.
		 */
		if(c.isEnchantedBy("Spirit Shackle")) {
			final ArrayList<Card> cards = c.getEnchantedBy();
			for(Card card:cards) {
				final Card enchantedCard = c;
				if(card.getName().equals("Spirit Shackle")) {
					Ability ability = new Ability(card, "0") {
						@Override
						public void resolve() {
							enchantedCard.addCounter(Counters.P0M2, 1);
						}
					};//Ability
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - enchanted creature gets a -0/-2 counter.");
					ability.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability);
				}
			}
		}//end Spirit Shackle
		
		/*
		 * Royal Decree - Whenever a Swamp, Mountain, black permanent, or red
		 * permanent becomes tapped, Royal Decree deals 1 damage to that
		 * permanent's controller.
		 */
		if(c.getType().contains("Mountain") || c.getType().contains("Swamp") ||
				(c.isPermanent() && (c.isRed() || c.isBlack()))) {
			final CardList decrees = AllZoneUtil.getCardsInPlay("Royal Decree");
			for(Card decree:decrees) {
				final Card crd = decree;
				final Card source = c;
				Ability ability = new Ability(decree, "0") {
					@Override
					public void resolve() {
						source.getController().addDamage(1, crd);
					}
				};//Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(decree.getName()).append(" - does 1 damage to ").append(c.getController());
				sb.append(".  (").append(c.getName()).append(" was tapped.)");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}//for
		}//end Royal Decree

	}//end executeTapSideEffects()
	
	public static void executeUntapSideEffects(Card c) {
		/*
		 * This is currently only for Hollowsage, Mesmeric Orb and Wake Thrasher
		 * I don't think WheneverKeyword is implemented for BecomesUntapped
		 */
		//AllZone.GameAction.CheckWheneverKeyword(c,"BecomesUntapped",null);
		
		/*
		 * Mesmeric Orb - Whenever a permanent becomes untapped, that permanent's
		 * controller puts the top card of his or her library into his or her graveyard.
		 */
		if(c.isPermanent()) {
			final Player controller = c.getController();
			final CardList orbs = AllZoneUtil.getCardsInPlay("Mesmeric Orb");
			for(Card orb:orbs) {
				Ability ability = new Ability(orb, "0") {
					@Override
					public void resolve() {
						CardList lib = AllZoneUtil.getPlayerCardsInLibrary(controller);
						if(lib.size() >0) {
							AllZone.GameAction.moveToGraveyard(lib.get(0));
						}
					}
				};//Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(orb.getName()).append(" - ").append(c).append(" was untapped, ");
				sb.append(controller).append(" puts top card of library into graveyard.");
				ability.setStackDescription(sb.toString());
				
				if(AllZoneUtil.getPlayerCardsInLibrary(controller).size() > 0) {
					AllZone.Stack.add(ability);
				}
			}//for
		}//end Mesmeric Orb
		
		/*
		 * Wake Thrasher - Whenever a permanent you control becomes untapped,
		 * Wake Thrasher gets +1/+1 until end of turn.
		 */
		if(c.isPermanent()) {
			final Player controller = c.getController();
			final CardList thrashers = AllZoneUtil.getPlayerCardsInPlay(controller, "Wake Thrasher");
			for(Card thrasher:thrashers) {
				final Card crd = thrasher;
				Ability ability = new Ability(crd, "0") {
					@Override
					public void resolve() {
						crd.addTempAttackBoost(1);
						crd.addTempDefenseBoost(1);
						//EOT
	                    final Command untilEOT = new Command() {
							private static final long serialVersionUID = -8593688796458658565L;

							public void execute() {
	                            crd.addTempAttackBoost(-1);
	                            crd.addTempDefenseBoost(-1);
	                        }
	                    };
	                    AllZone.EndOfTurn.addUntil(untilEOT);
					}
				};//Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(crd.getName()).append(" - gets +1/+1 until end of turn.  (");
				sb.append(c).append(" was untapped.)");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}//for
		}//end Wake Thrasher
		
	}

	public static void executePlayCardEffects(SpellAbility sa) {
		// experimental:
		// this method check for cards that have triggered abilities whenever a
		// card gets played
		// (called in MagicStack.java)
		Card c = sa.getSourceCard();
		
		if (c.getName().equals("Kozilek, Butcher of Truth"))
			playCard_Kozilek(c);
		else if (c.getName().equals("Ulamog, the Infinite Gyre"))
			playCard_Ulamog(c);
		else if (c.getName().equals("Emrakul, the Aeons Torn"))
			playCard_Emrakul(c);
		else if (c.getName().equals("Artisan of Kozilek"))
			playCard_Artisan_of_Kozilek(c);

		playCard_Cascade(c);
		playCard_Ripple(c);
        playCard_Storm(c);

		playCard_Dovescape(c); //keep this one top
		playCard_Chalice_of_the_Void(c);
		playCard_Vengevine(c);
		playCard_Demigod_of_Revenge(c);
		playCard_Halcyon_Glaze(c);
		playCard_Thief_of_Hope(c);
		playCard_Infernal_Kirin(c);
		playCard_Cloudhoof_Kirin(c);
		playCard_Bounteous_Kirin(c);
		playCard_Emberstrike_Duo(c);		
		playCard_Gravelgill_Duo(c);
		playCard_Safehold_Duo(c);
		playCard_Tattermunge_Duo(c);
		playCard_Thistledown_Duo(c);
		playCard_Battlegate_Mimic(c);
		playCard_Nightsky_Mimic(c);
		playCard_Riverfall_Mimic(c);
		playCard_Shorecrasher_Mimic(c);
		playCard_Woodlurker_Mimic(c);
		playCard_Belligerent_Hatchling(c);
		playCard_Voracious_Hatchling(c);
		playCard_Sturdy_Hatchling(c);
		playCard_Noxious_Hatchling(c);
		playCard_Witch_Maw_Nephilim(c);
		playCard_Forced_Fruition(c);
		playCard_Gelectrode(c);
		playCard_Cinder_Pyromancer(c);
		playCard_Standstill(c);
		playCard_Memory_Erosion(c);
		playCard_SolKanar(c);
		playCard_Gilt_Leaf_Archdruid(c);
		playCard_Reki(c);
		playCard_Vedalken_Archmage(c);
		playCard_Sigil_of_the_Empty_Throne(c);
		playCard_Merrow_Levitator(c);
		playCard_Primordial_Sage(c);
		playCard_Quirion_Dryad(c);
		playCard_Enchantress_Draw(c);
		playCard_Mold_Adder(c);
		playCard_Fable_of_Wolf_and_Owl(c);
		playCard_Kor_Firewalker(c);
		playCard_Curse_of_Wizardry(c);
		playCard_Hand_of_the_Praetors(c);
		playCard_Venser_Emblem(c);
		playCard_Presence_of_the_Master(c);
		
		AllZone.GameAction.checkWheneverKeyword(c,"CastSpell",null);
	}

	public static void playCard_Kozilek(Card c)
	{
		final Player controller = c.getController();
		final Ability ability = new Ability(c, "0")
		{
			public void resolve()
			{
				controller.drawCards(4);
			}
		};
		ability.setStackDescription("Kozilek - draw four cards.");
		AllZone.Stack.add(ability);
	}
	
	public static void playCard_Ulamog(Card c)
	{
		final Card ulamog = c;
		final Player controller = c.getController();
		final Ability ability = new Ability(c, "0")
		{
			public void chooseTargetAI()
			{
				CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
				list = list.filter(new CardListFilter()
				{
					public boolean addCard(Card card)
					{
						return CardFactoryUtil.canTarget(ulamog, card);
					}
				});
				
				if (list.size()>0)
				{
					CardListUtil.sortCMC(list);
					setTargetCard(list.get(0));
				}
			}
			public void resolve()
			{
				Card crd = getTargetCard();
				if (crd!=null) {
					if (CardFactoryUtil.canTarget(ulamog, crd))
						AllZone.GameAction.destroy(crd);
				}
			}
		};
		ability.setBeforePayMana(CardFactoryUtil.input_targetPermanent(ability));
		if (controller.equals(AllZone.HumanPlayer))
			AllZone.GameAction.playSpellAbility(ability);
		else {
			ability.chooseTargetAI();
			AllZone.Stack.add(ability);
		}
	}
	
	public static void playCard_Emrakul(Card c)
	{
		final Player controller = c.getController();
		final Ability ability = new Ability(c, "0")
		{
			public void resolve()
			{
				AllZone.Phase.addExtraTurn(controller);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c).append(" - When you cast Emrakul, take an extra turn after this one.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}
	
	public static void playCard_Artisan_of_Kozilek(Card c)
	{
		final Player controller = c.getController();
		final Ability ability = new Ability(c, "0")
		{
			public void chooseTargetAI()
			{	
				CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
				list = list.getType("Creature");
				
				if (list.size()>0)
				{
					CardListUtil.sortCMC(list);
					setTargetCard(list.get(0));
				}
			}
			public void resolve()
			{
				if (controller.equals(AllZone.HumanPlayer))
				{
					CardList creatures = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					creatures = creatures.getType("Creature");
					Object check = AllZone.Display.getChoiceOptional("Select creature", creatures.toArray());
					if(check != null) {
	                    this.setTargetCard((Card) check);
	                }
				}
				
				PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, controller);
				Card crd = getTargetCard();
				if (crd!=null) {
					if(AllZone.GameAction.isCardInZone(crd, grave)) {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
                        AllZone.GameAction.moveTo(play, crd);
                    }
				}
			}
		};
		StringBuilder sb = new StringBuilder();
		sb.append(c).append(" - you may return target creature card from your graveyard to the battlefield.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}
	
	
	
	public static void playCard_Cascade(final Card c) {
		Command Cascade = new Command() {
			private static final long serialVersionUID = -845154812215847505L;
			public void execute() {
				final PlayerZone play = AllZone.getZone(Constant.Zone.Play,
						AllZone.HumanPlayer);
				final PlayerZone comp = AllZone.getZone(Constant.Zone.Play,
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

						if(topOfLibrary.size() == 0) return;

						Card cascadedCard = null;
						Card crd;
						int count = 0;
						while(cascadedCard == null) {
							crd = topOfLibrary.get(count++);
							revealed.add(crd);
							lib.remove(crd);
							if((!crd.isLand() && CardUtil.getConvertedManaCost(crd.getManaCost()) < CardUtil.getConvertedManaCost(cascCard.getManaCost()))) cascadedCard = crd;

							if(count == topOfLibrary.size()) break;

						}//while
							AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());

						if(cascadedCard != null && !cascadedCard.isUnCastable()) {

							if(cascadedCard.getController().equals(AllZone.HumanPlayer)) {
								String[] choices = {"Yes", "No"};

								Object q = null;

								q = AllZone.Display.getChoiceOptional("Cast " + cascadedCard.getName() + "?", choices);
								if(q != null) {
									if(q.equals("Yes")) {
										AllZone.GameAction.playCardNoCost(cascadedCard);
										revealed.remove(cascadedCard);
									}
								}
							} else //computer
							{
								ArrayList<SpellAbility> choices = cascadedCard.getBasicSpells();

								for(SpellAbility sa:choices) {
									if(sa.canPlayAI()) {
										ComputerUtil.playStackFree(sa);
										revealed.remove(cascadedCard);
										break;
									}
								}
							}
						}
						revealed.shuffle();
						for(Card bottom:revealed) {
							lib.add(bottom);
						}
					}
				};
				StringBuilder sb = new StringBuilder();
				sb.append(c).append(" - Cascade.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		};
		Cascade.execute();
	}
	
	public static void playCard_Ripple(final Card c) {
		Command Ripple = new Command() {
			private static final long serialVersionUID = -845154812215847505L;
			public void execute() {
				final PlayerZone play = AllZone.getZone(Constant.Zone.Play,
						AllZone.HumanPlayer);
				final PlayerZone comp = AllZone.getZone(Constant.Zone.Play,
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
							lib.remove(crd);
							if(crd.getName().equals(RippleCard.getName())) RippledCards[i] = crd;
						}//For
							AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());
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
								lib.add(bottom);
							}
					}
				};
				StringBuilder sb = new StringBuilder();
				sb.append(c).append(" - Ripple.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
			}
		};
		Ripple.execute();
	}//playCard_Ripple()
	
	public static void playCard_Storm(Card c) {
		if(c.getKeyword().contains("Storm") && !c.isCopiedSpell())
		{		
			final Card StormCard = c;
			StormCard.removeIntrinsicKeyword("Storm");
			final int StormNumber  = Phase.StormCount - 1;		
			final Ability Storm = new Ability(c, "0") {	
				public void resolve() {
					for(int i = 0; i < (StormNumber); i++) {
						AllZone.CardFactory.copySpellontoStack(StormCard,StormCard,true);   	
				};	// For
				}
			};
			StringBuilder sb = new StringBuilder();
			sb.append(c).append(" - Storm.");
			Storm.setStackDescription(sb.toString());
			
			AllZone.Stack.add(Storm);			
		}
	}//playCard_Storm()
	
	public static void playCard_Vengevine(Card c) {
		if (c.isCreature() == true && (Phase.PlayerCreatureSpellCount == 2 || Phase.ComputerCreatureSpellCount == 2))
		{
		final Player controller = c.getController();
		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
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
					
					AllZone.Stack.add(ability);
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
			AllZone.Stack.add(counter);
		}
	}
	
	public static void playCard_Hand_of_the_Praetors(Card c)
	{
		if (!c.getKeyword().contains("Infect"))
			return;
		
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Hand of the Praetors");
		
		for (int i=0;i<list.size();i++)
		{
			final Card card = list.get(i);
			final Ability ability = new Ability(card, "0")
			{
				public void resolve()
				{
					if (getTargetPlayer().equals(AllZone.HumanPlayer))
						AllZone.HumanPlayer.addPoisonCounters(1);
					else
						AllZone.ComputerPlayer.addPoisonCounters(1);
				}
				
				public void chooseTargetAI()
				{
					setTargetPlayer(AllZone.HumanPlayer);
				}
			};
			
			ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
			if (controller.equals(AllZone.HumanPlayer))
				AllZone.GameAction.playSpellAbility(ability);
			else {
				ability.chooseTargetAI();
				AllZone.Stack.add(ability);
			}
		}
	}
	
	public static void playCard_Venser_Emblem(Card c)
	{
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

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
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
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
				AllZone.Stack.add(ability);
			}
		}
	}
	
	
	public static void playCard_Emberstrike_Duo(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Emberstrike Duo");

		if(list.size() > 0) {
			if(c.isBlack()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.addTempAttackBoost(-1);
								card.addTempDefenseBoost(-1);
							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.addTempAttackBoost(1);
							card.addTempDefenseBoost(1);
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a black spell, Emberstrike Duo gets +1/+1 until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isRed()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = -4569751606008597913L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(card)) {
							card.removeIntrinsicKeyword("First Strike");

						}
					}
				};

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(!card.getIntrinsicKeyword().contains("First Strike")) card.addIntrinsicKeyword("First Strike");
						AllZone.EndOfTurn.addUntil(untilEOT);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a red spell, Emberstrike Duo gains first strike until end of turn.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}//if

	}//Emberstrike Duo

	public static void playCard_Gravelgill_Duo(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Gravelgill Duo");

		if(list.size() > 0) {
			if(c.isBlue()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.addTempAttackBoost(-1);
								card.addTempDefenseBoost(-1);


							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.addTempAttackBoost(1);
							card.addTempDefenseBoost(1);
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a blue spell, Gravelgill Duo gets +1/+1 until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isBlack()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = -4569751606008597903L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(card)) {
							card.removeIntrinsicKeyword("Fear");

						}
					}
				};

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(!card.getIntrinsicKeyword().contains("Fear")) card.addIntrinsicKeyword("Fear");
						AllZone.EndOfTurn.addUntil(untilEOT);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a black spell, Emberstrike Duo gains fear until end of turn.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}//if

	}//Gravelgill Duo
	
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
				if(sa.isSpell() == true && card.getCounters(Counters.CHARGE) == convertedManaSpell) AllZone.Stack.add(ability2);	
			}					
		}//if
	} // Chalice_of_the_Void 
	
	public static void playCard_Safehold_Duo(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Safehold Duo");

		if(list.size() > 0) {
			if(c.isGreen()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.addTempAttackBoost(-1);
								card.addTempDefenseBoost(-1);
							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.addTempAttackBoost(1);
							card.addTempDefenseBoost(1);
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a green spell, Safehold Duo gets +1/+1 until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isWhite()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = -4569751606008597903L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(card)) {
							card.removeIntrinsicKeyword("Vigilance");

						}
					}
				};

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(!card.getIntrinsicKeyword().contains("Vigilance")) card.addIntrinsicKeyword("Vigilance");
						AllZone.EndOfTurn.addUntil(untilEOT);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a white spell, Safehold Duo gains vigilance until end of turn.");
				ability2.setStackDescription(sb.toString());
			    
				AllZone.Stack.add(ability2);
			}
		}//if

	}//Safehold Duo

	public static void playCard_Tattermunge_Duo(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Tattermunge Duo");

		if(list.size() > 0) {
			if(c.isRed()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.addTempAttackBoost(-1);
								card.addTempDefenseBoost(-1);
							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.addTempAttackBoost(1);
							card.addTempDefenseBoost(1);
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a red spell, Tattermunge Duo gets +1/+1 until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isGreen()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = -4569751606008597903L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(card)) {
							card.removeIntrinsicKeyword("Forestwalk");
						}
					}
				};

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(!card.getIntrinsicKeyword().contains("Forestwalk")) card.addIntrinsicKeyword("Forestwalk");
						AllZone.EndOfTurn.addUntil(untilEOT);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a green spell, Tattermunge Duo gains forestwalk until end of turn.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}//if

	}//Tattermunge Duo

	public static void playCard_Thistledown_Duo(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Thistledown Duo");

		if(list.size() > 0) {
			if(c.isWhite()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.addTempAttackBoost(-1);
								card.addTempDefenseBoost(-1);
							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.addTempAttackBoost(1);
							card.addTempDefenseBoost(1);
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a white spell, Thistledown Duo gets +1/+1 until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isBlue()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = -4569751606008597903L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(card)) {
							card.removeIntrinsicKeyword("Flying");

						}
					}
				};

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(!card.getIntrinsicKeyword().contains("Flying")) card.addIntrinsicKeyword("Flying");
						AllZone.EndOfTurn.addUntil(untilEOT);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a blue spell, Thistledown Duo gains flying until end of turn.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}//if


	}//Thistledown Duo

	public static void playCard_Demigod_of_Revenge(Card c) {

		if(c.getName().equals("Demigod of Revenge")) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					PlayerZone Grave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.Phase.getPlayerTurn());
					PlayerZone Play = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getPlayerTurn());
					CardList evildead = new CardList();
					evildead.addAll(Grave.getCards());
					evildead = evildead.filter(new CardListFilter() {
						public boolean addCard(Card card) {
							return (card.getName().contains("Demigod of Revenge"));
						}
					});
					for(int i = 0; i < evildead.size(); i++) {
						Card c = evildead.get(i);
						Grave.remove(c);
						Play.add(c);
					}
				}
			}; // ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(c.getController());
			sb.append(" casts Demigod of Revenge, returns all cards named Demigod ");
			sb.append("of Revenge from your graveyard to the battlefield.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);

		}//if					
	}// Demigod of Revenge

	public static void playCard_Halcyon_Glaze(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Halcyon Glaze");

		if(list.size() > 0) {
			if(c.getType().contains("Creature")) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(0);
								card.setBaseDefense(0);
								card.removeType("Creature");
								card.removeType("Illusion");
								card.removeIntrinsicKeyword("Flying");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(4);
							card.setBaseDefense(4);
							if(!card.getIntrinsicKeyword().contains("Flying")) card.addIntrinsicKeyword("Flying");
							if(!card.getType().contains("Creature")) card.addType("Creature");
							if(!card.getType().contains("Illusion")) card.addType("Illusion");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a creature spell Halcyon Glaze becomes a 4/4 Illusion ");
					sb.append("creature with flying until end of turn. It's still an enchantment.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Halcyon Glaze

	public static void playCard_Thief_of_Hope(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Thief of Hope");

		if(list.size() > 0) {
			if(c.isType("Spirit") || c.getType().contains("Arcane")) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							final Player target;
							if(card.getController().isHuman()) {
								String[] choices = {"Opponent", "Yourself"};
								Object choice = AllZone.Display.getChoice("Choose target player", choices);
								if(choice.equals("Opponent")) {
									target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here
								}// if choice yes
								else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							} else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							//AllZone.GameAction.getPlayerLife(target).loseLife(1,card);
							target.loseLife(1, card);
							card.getController().gainLife(1, card);

						} //resolve
					}; //ability
					ability2.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
					ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a Spirit or Arcane spell, target opponent loses 1 life and you gain 1 life.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Thief of Hope

	public static void playCard_Infernal_Kirin(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

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
								Object choice = AllZone.Display.getChoice("Choose target player", choices);
								if(choice.equals("Opponent")) {
									target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here
								}// if choice yes
								else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							} else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
							PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target);
							CardList fullHand = new CardList(hand.getCards());
							if(fullHand.size() > 0 && target.equals(AllZone.ComputerPlayer)) AllZone.Display.getChoice(
									"Revealing hand", fullHand.toArray());
							CardList discard = new CardList(hand.getCards());
							discard = discard.filter(new CardListFilter() {
								public boolean addCard(Card c) {
									return CardUtil.getConvertedManaCost(c.getManaCost()) == converted;
								}
							});
							for(int j = 0; j < discard.size(); j++) {
								Card choice = discard.get(j);
								//AllZone.GameAction.discard(choice, this);
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
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Infernal Kirin

	public static void playCard_Cloudhoof_Kirin(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Cloudhoof Kirin");

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
								String[] choices = {"Opponent", "Yourself", "None"};
								Object choice = AllZone.Display.getChoice("Choose target player", choices);
								if(choice.equals("Opponent")) {
									target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here
								}// if choice yes
								else if(!choice.equals("None")) target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
								else target = null;
							} else target = AllZone.HumanPlayer; // check for target of spell/abilities should be here						
							if(!(null == target)) {
								PlayerZone lib = AllZone.getZone(Constant.Zone.Library, target);
								PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, target);
								CardList libList = new CardList(lib.getCards());

								int max = converted;

								if(libList.size() < max) max = libList.size();

								for(int i = 0; i < max; i++) {
									Card c = libList.get(i);
									lib.remove(c);
									grave.add(c);
								}
							} //if
						} //resolve
					}; //ability
					ability2.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
					ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a Spirit or Arcane spell, target player puts the top ");
					sb.append(converted).append(" cards of his or her library into his or her graveyard.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Cloudhoof Kirin

	public static void playCard_Bounteous_Kirin(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Bounteous Kirin");

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
								String[] choices = {"Yourself", "Opponent", "None"};
								Object choice = AllZone.Display.getChoice("Choose target player", choices);
								if(choice.equals("Opponent")) {
									target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here
								}// if choice yes
								else if(!choice.equals("None")) target = AllZone.HumanPlayer; // check for target of spell/abilities should be here
								else target = null;
							} else target = AllZone.ComputerPlayer; // check for target of spell/abilities should be here						
							if(!(null == target)) {
								target.gainLife(converted, card);
							}
								
						} //resolve
					}; //ability
					ability2.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
					ability2.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability2));
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a Spirit or Arcane spell, target player may gain ");
					sb.append(converted).append(" life.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Bounteous Kirin


	public static void playCard_Shorecrasher_Mimic(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Shorecrasher Mimic");

		if(list.size() > 0) {
			if(c.isBlue() && c.isGreen()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(2);
								card.setBaseDefense(1);
								card.removeIntrinsicKeyword("Trample");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(5);
							card.setBaseDefense(3);
							if(!card.getIntrinsicKeyword().contains("Trample")) card.addIntrinsicKeyword("Trample");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a spell that's both green and blue, ");
					sb.append("it becomes 5/3 and gains trample until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Shorecrasher Mimic

	public static void playCard_Battlegate_Mimic(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Battlegate Mimic");

		if(list.size() > 0) {
			if(c.isRed() && c.isWhite()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(2);
								card.setBaseDefense(1);
								card.removeIntrinsicKeyword("First Strike");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(4);
							card.setBaseDefense(2);
							if(!card.getIntrinsicKeyword().contains("First Strike")) card.addIntrinsicKeyword("First Strike");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a spell that's both red and white, ");
					sb.append("it becomes 4/2 and gains first strike until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Battlegate Mimic

	public static void playCard_Nightsky_Mimic(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Nightsky Mimic");

		if(list.size() > 0) {
			if(c.isBlack() && c.isWhite()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(2);
								card.setBaseDefense(1);
								card.removeIntrinsicKeyword("Flying");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(4);
							card.setBaseDefense(4);
							if(!card.getIntrinsicKeyword().contains("Flying")) card.addIntrinsicKeyword("Flying");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a spell that's both black and white, ");
					sb.append("it becomes 4/4 and gains flying until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Nightsky Mimic

	public static void playCard_Riverfall_Mimic(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Riverfall Mimic");

		if(list.size() > 0) {
			if(c.isBlue() && c.isRed()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(2);
								card.setBaseDefense(1);
								card.removeIntrinsicKeyword("Unblockable");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(3);
							card.setBaseDefense(3);
							if(!card.getIntrinsicKeyword().contains("Unblockable")) card.addIntrinsicKeyword("Unblockable");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a spell that's both red and blue, ");
					sb.append("it becomes 3/3 and is unblockable until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Riverfall Mimic

	public static void playCard_Woodlurker_Mimic(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Woodlurker Mimic");

		if(list.size() > 0) {
			if(c.isBlack() && c.isGreen()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);
					final Command untilEOT = new Command() {
						private static final long serialVersionUID = -4569751606008597903L;

						public void execute() {
							if(AllZone.GameAction.isCardInPlay(card)) {
								card.setBaseAttack(2);
								card.setBaseDefense(1);
								card.removeIntrinsicKeyword("Wither");

							}
						}
					};

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							card.setBaseAttack(4);
							card.setBaseDefense(5);
							if(!card.getIntrinsicKeyword().contains("Wither")) card.addIntrinsicKeyword("Wither");
							AllZone.EndOfTurn.addUntil(untilEOT);
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a spell that's both green and black, ");
					sb.append("it becomes 4/5 and gains wither until end of turn.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

	}//Woodlurker Mimic


	public static void playCard_Dovescape(Card c) {
		final Card crd1 = c;
		PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
		PlayerZone cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);

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
			AllZone.Stack.add(ability2);


		}
	} // Dovescape


	public static void playCard_Belligerent_Hatchling(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Belligerent Hatchling");

		if(list.size() > 0) {
			if(c.isRed()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
						}

					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a red spell, remove a -1/-1 counter from Belligerent Hatchling.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isWhite()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
					}

				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a white spell, remove a -1/-1 counter from Belligerent Hatchling.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		}//if


	}// Belligerent Hatchling

	public static void playCard_Noxious_Hatchling(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Noxious Hatchling");

		if(list.size() > 0) {
			if(c.isBlack()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
						}

					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a black spell, remove a -1/-1 counter from Noxious Hatchling.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isGreen()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
					}

				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a green spell, remove a -1/-1 counter from Noxious Hatchling.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		}//if


	}// Noxious Hatchling

	public static void playCard_Sturdy_Hatchling(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Sturdy Hatchling");

		if(list.size() > 0) {
			if(c.isBlue()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
						}

					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a blue spell, remove a -1/-1 counter from Sturdy Hatchling.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isGreen()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
					}

				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a green spell, remove a -1/-1 counter from Sturdy Hatchling.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		}//if


	}// Sturdy Hatchling

	public static void playCard_Voracious_Hatchling(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Voracious Hatchling");

		if(list.size() > 0) {
			if(c.isBlack()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
						}

					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a black spell, remove a -1/-1 counter from Voracious Hatchling.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isWhite()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(card.getCounters(Counters.M1M1) > 0) card.subtractCounter(Counters.M1M1, 1);
					}

				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a white spell, remove a -1/-1 counter from Voracious Hatchling.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		}//if


	}// Voracious Hatchling

	public static void playCard_Witch_Maw_Nephilim(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Witch-Maw Nephilim");

		if(list.size() > 0) {

			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {

						if(card.getController().equals(AllZone.HumanPlayer)) {
							String[] choices = {"Yes", "No"};
							Object choice = AllZone.Display.getChoice("Put two +1/+1 on Witch-Maw Nephilim?",
									choices);
							if(choice.equals("Yes")) {
								card.addCounter(Counters.P1P1, 2);
							}
						}
						if(card.getController().equals(AllZone.ComputerPlayer)) {
							card.addCounter(Counters.P1P1, 2);
						}
					}

				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a spell, you may put two +1/+1 counters on Witch-Maw Nephilim.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}
	}// Witch-Maw Nephilim

	public static void playCard_Gelectrode(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Gelectrode");

		if(list.size() > 0 && (c.getType().contains("Instant") || c.getType().contains("Sorcery")) && !c.isCopiedSpell()) {

			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {

						if(card.getController().equals(AllZone.HumanPlayer)) {
							String[] choices = {"Yes", "No"};
							Object choice = AllZone.Display.getChoice("Untap Gelectrode?", choices);
							if(choice.equals("Yes")) {
								card.untap();
							}
						}
						if(card.getController().equals(AllZone.ComputerPlayer)) {
							card.untap();
						}
					}

				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played an instant or sorcery spell, you may untap Gelectrode.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}
	}// Gelectrode

    public static void playCard_Cinder_Pyromancer(Card c) {
        final Player controller = c.getController();

        final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

        CardList list = new CardList();
        list.addAll(play.getCards());
        list = list.getName("Cinder Pyromancer");

        if (list.size() > 0 && c.isRed()) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                
                Ability ability2 = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (controller.isHuman()) {
                            String question = "Untap your Cinder Pyromancer?";
                            if (showYesNoDialog(card, question)) {
                                card.untap();
                            }
                        }// controller isComputer()
                        else card.untap(); 
                        
                    }// resolve()
                };// ability2

                StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - ").append(c.getController());
                sb.append(" played a red spell, you may untap Cinder Pyromancer.");
                ability2.setStackDescription(sb.toString());
                AllZone.Stack.add(ability2);
            }
        }
    }// Cinder_Pyromancer

	public static void playCard_Forced_Fruition(Card c) {
		PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
		PlayerZone cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);

		CardList list = new CardList();
		list.addAll(hplay.getCards());
		list.addAll(cplay.getCards());

		list = list.getName("Forced Fruition");

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
				
				AllZone.Stack.add(ability2);
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
			
			AllZone.Stack.add(ability2);

		}

	}

	public static void playCard_Memory_Erosion(Card c) {
		CardList list = AllZoneUtil.getCardsInPlay("Memory Erosion");

		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			final Player drawer = card.getController().getOpponent();


			Ability ability2 = new Ability(card, "0") {
				@Override
				public void resolve() {
					// sac standstill
					//            AllZone.GameAction.sacrifice(card);
					// player who didn't play spell, draws 3 cards
					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, drawer);
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, drawer);
					CardList libList = new CardList(lib.getCards());

					int max = 2;
					if(libList.size() < 2) max = libList.size();

					for(int i = 0; i < max; i++) {
						Card c = libList.get(i);
						lib.remove(c);
						grave.add(c);
					}

				}
			}; // ability2
			if(!(card.getController().equals(c.getController()))) {
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController()).append(" played a spell, ");
				sb.append(drawer).append(" puts the top two cards of his or her library into his or her graveyard.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}
	}

	public static void playCard_SolKanar(Card c) {
		CardList list = AllZoneUtil.getCardsInPlay("Sol'kanar the Swamp King");

		if(list.size() > 0 && c.isBlack()) {
			final Card card = list.get(0);
			final Player controller = card.getController();

			Ability ability2 = new Ability(card, "0") {
				@Override
				public void resolve() {
					controller.gainLife(1, card);
				}
			}; // ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(card.getName()).append(" - ").append(c.getController());
			sb.append(" played a black spell, ").append(card.getController()).append(" gains 1 life.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}

	}

    public static void playCard_Enchantress_Draw(Card c) {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController());

        list = list.filter(new CardListFilter() {
            public boolean addCard(Card crd) {
                if (crd.getName().equals("Verduran Enchantress")    || crd.getName().equals("Enchantress's Presence")
                        || crd.getName().equals("Mesa Enchantress") || crd.getName().equals("Argothian Enchantress")
                        || crd.getName().equals("Kor Spiritdancer")) return true;
                else return false;
            }
        });

        if (c.isEnchantment()) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);

                Ability ability2 = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        Player controller = card.getController();
                        int computerLibrarySize = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.ComputerPlayer).size();
                        int computerHandSize = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.ComputerPlayer).size();
                        int computerMaxHandSize = AllZone.ComputerPlayer.getMaxHandSize();
                        
                        Boolean mayDrawNotMust = (card.getName().equals("Verduran Enchantress") 
                                || card.getName().equals("Mesa Enchantress")
                                || card.getName().equals("Kor Spiritdancer"));
                        
                        if (mayDrawNotMust) {
                            
                            if (controller.isHuman()) {
                                String question = "Will you draw a card?";
                                if (showYesNoDialog(card, question)) {
                                    controller.drawCard();
                                }
                            }// controller isComputer() and may draw
                            else if (computerLibrarySize >= 5 && computerHandSize < computerMaxHandSize) {
                                controller.drawCard();
                        }
                        // Must draw, not may draw
                        } else controller.drawCard();
                        
                    }// resolve()
                };// ability2
                
                StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - ").append(c.getController()).append(" plays an enchantment spell and ");
                
                if (card.getName().equals("Verduran Enchantress") 
                        || card.getName().equals("Mesa Enchantress")
                        || card.getName().equals("Kor Spiritdancer")) {
                    sb.append("may draw a card.");
                } else {
                    sb.append("draws a card.");
                }
                ability2.setStackDescription(sb.toString());
                AllZone.Stack.add(ability2);
            }// for
        }// if isEnchantment()
    }// playCard_Enchantress_Draw()

	public static void playCard_Gilt_Leaf_Archdruid(Card c) {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController(), "Gilt-Leaf Archdruid");
		
		if(c.getType().contains("Druid") || c.getKeyword().contains("Changeling")) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(0);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						// draws a card
						card.getController().drawCard();
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" plays a Druid spell and draws a card");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if druid
	}

	public static void playCard_Reki(Card c) {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController(), "Reki, the History of Kamigawa");
		
		if(c.getType().contains("Legendary")) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(0);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						card.getController().drawCard();
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" plays a Legendary spell and draws a card");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if legendary
	}

	public static void playCard_Vedalken_Archmage(Card c) {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController(), "Vedalken Archmage");
		
		if(c.getType().contains("Artifact")) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(0);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						card.getController().drawCard();
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" plays an Artifact spell and draws a card");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if artifact
	}

	public static void playCard_Sigil_of_the_Empty_Throne(Card c) {

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());

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
				sb.append(" puts a 4/4 White Angel token with flying into play.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if isEnchantment()
	}

	public static void playCard_Merrow_Levitator(Card c) {
		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());


		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Merrow Levitator");

		if(c.isBlue()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						if(card.isTapped()) card.untap();
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(" untaps");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if is blue spell
	}//merrow levitator

	public static void playCard_Primordial_Sage(Card c) {

		PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Primordial Sage");
		if(c.getType().contains("Creature")) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(0);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						// draws a card
						card.getController().drawCard();
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" plays a Creature spell and draws a card");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			} // for
		}// if Creature
	}//primordial sage

	public static void playCard_Quirion_Dryad(Card c) {
		Player controller = c.getController();

		PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Quirion Dryad");

		if(list.size() > 0 && (c.isWhite() ||c.isBlue() || c.isBlack() || c.isRed())) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						card.addCounter(Counters.P1P1, 1);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a white, blue, black or red spell, ");
				sb.append(card.getName()).append(" gets a +1/+1 counter.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);
			}
		}

	}//Quirion

	public static void playCard_Mold_Adder(Card c) {
		Player opponent = c.getController().getOpponent();

		PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Mold Adder");

		if(list.size() > 0 && (c.isBlue() || c.isBlack())) {
			for(int i = 0; i < list.size(); i++) {

				final Card card = list.get(i);

				Ability ability2 = new Ability(card, "0") {
					@Override
					public void resolve() {
						card.addCounter(Counters.P1P1, 1);
					}
				}; // ability2
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a blue or black spell, ").append(card.getName());
				sb.append(" gets a +1/+1 counter.");
				ability2.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability2);

			}
		}

	}//Quirion

	public static void playCard_Fable_of_Wolf_and_Owl(Card c) {
		final Player controller = c.getController();

		final PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Fable of Wolf and Owl");

		if(list.size() > 0) {
			if(c.isBlue()) {
				for(int i = 0; i < list.size(); i++) {
					final Card card = list.get(i);

					Ability ability2 = new Ability(card, "0") {
						@Override
						public void resolve() {
							CardFactoryUtil.makeToken("Bird", "U 1 1 Bird", card.getController(), "U", new String[] {
									"Creature", "Bird"}, 1, 1, new String[] {"Flying"});
						}
					}; // ability2
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(c.getController());
					sb.append(" played a blue spell, put a 1/1 blue Bird token with flying into play.");
					ability2.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability2);
				}
			}//if
		}

		if(c.isGreen()) {
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);

				Ability ability = new Ability(card, "0") {
					@Override
					public void resolve() {
						CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", card.getController(), "G", new String[] {
								"Creature", "Wolf"}, 2, 2, new String[] {""});
					}
				}; // ability
				
				StringBuilder sb = new StringBuilder();
				sb.append(card.getName()).append(" - ").append(c.getController());
				sb.append(" played a green spell, put a 2/2 green Wolf token into play.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}
		}//if
	}//Fable

    public static void playCard_Kor_Firewalker(Card c) {

        CardList list = AllZoneUtil.getCardsInPlay("Kor Firewalker");

        if (list.size() > 0) {
            if (c.isRed()) {
                for (int i=0;i<list.size();i++) {
                    final Card card = list.get(i);
                    final Player controller = card.getController();
                    
                    Ability ability2 = new Ability(card, "0") {
                        public void resolve() {
                            
                            if (controller.isHuman()) {
                                String question = "Will you gain 1 life?";
                                if (showYesNoDialog(card, question)) {
                                    controller.gainLife(1, card);
                                }
                            }// controller isComputer()
                            else controller.gainLife(1, card);
                            
                        }// resolve()
                    };//ability2
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(c.getController()).append(" played a Red spell, ");
                    sb.append(controller).append(" may gain 1 life.");
                    ability2.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability2);
                }//for
            }//if c.isRed
        }//if list
    }//Kor Firewalker
	
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
					
					AllZone.Stack.add(ability);
				}
			}//if
		}//if
	}//Curse of Wizardry


	public static void executeDrawCardTriggeredEffects(Player player) {
    	Object[] DrawCard_Whenever_Parameters = new Object[1];
    	DrawCard_Whenever_Parameters[0] = player;
		AllZone.GameAction.checkWheneverKeyword(AllZone.CardFactory.HumanNullCard,"DrawCard",DrawCard_Whenever_Parameters);
		drawCardTriggered_Hoofprints_of_the_Stag(player);
		drawCardTriggered_Lorescale_Coatl(player);
		drawCardTriggered_Underworld_Dreams(player);
		drawCardTriggered_Spiteful_Visions(player);
	}

	public static void drawCardTriggered_Underworld_Dreams(Player player) {
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player.getOpponent());
		CardList list = new CardList(playZone.getCards());
		final Player player_d = player;
		list = list.getName("Underworld Dreams");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			final Card F_card = c;
			final Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					player_d.addDamage(1, F_card);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(list.get(i)).append(" - Deals 1 damage to him or her");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}
	
	private static void drawCardTriggered_Spiteful_Visions(final Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Spiteful Visions");

		for(int i = 0; i < list.size(); i++) {
			final Card source = list.get(i);
			final Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					player.addDamage(1, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - deals 1 damage to ").append(player).append(".");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

	public static void drawCardTriggered_Lorescale_Coatl(Player player) {
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		CardList list = new CardList(playZone.getCards());

		list = list.getName("Lorescale Coatl");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			c.addCounter(Counters.P1P1, 1);
		}
	}

	public static void drawCardTriggered_Hoofprints_of_the_Stag(Player player) {
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		CardList list = new CardList(playZone.getCards());

		list = list.getName("Hoofprints of the Stag");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			c.addCounter(Counters.HOOFPRINT, 1);
		}
	}

	//UPKEEP CARDS:

	public static void payManaDuringAbilityResolve(String message, String manaCost, Command paid, Command unpaid){
		// temporarily disable the Resolve flag, so the user can payMana for the resolving Ability
		boolean bResolving = AllZone.InputControl.getResolving();
		AllZone.InputControl.setResolving(false);
		AllZone.InputControl.setInput(new Input_PayManaCost_Ability(message, manaCost, paid, unpaid));
		AllZone.InputControl.setResolving(bResolving);
	}
	
	private static void upkeep_removeDealtDamageToOppThisTurn() {
		// TODO: this should happen in the cleanup phase
		// resets the status of attacked/blocked this turn
		Player player = AllZone.Phase.getPlayerTurn();
		Player opp = player.getOpponent();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, opp);

		CardList list = new CardList();
		list.addAll(play.getCards());
		list = list.getType("Creature");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(c.getDealtCombatDmgToOppThisTurn()) c.setDealtCombatDmgToOppThisTurn(false);
			if(c.getDealtDmgToOppThisTurn()) c.setDealtDmgToOppThisTurn(false);
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
						for(int i = 0; i < ageCounters; i++)
							AllZone.ManaPool.addManaToFloating("R", c);
					}
					else{
						AllZone.GameAction.sacrifice(c);
					}
						
				}
			};
			upkeepAbility.setStackDescription(sb.toString());
			
			AllZone.Stack.add(upkeepAbility);
		}
	}
	
	
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

			AllZone.Stack.add(destroyAbility);
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

			AllZone.Stack.add(upkeepAbility);
			
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

			AllZone.Stack.add(upkeepAbility);
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
				
				final Ability sacAbility = new Ability(c, c.getEchoCost()) {
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

				AllZone.Stack.add(sacAbility);

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
			AllZone.Stack.add(slowtrip);
			
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
			AllZone.Stack.add(slowtrip);
			
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
			AllZone.Stack.add(upkeepAbility);
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
			AllZone.Stack.add(upkeepAbility);
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
			AllZone.Stack.add(upkeepAbility);
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
											&& zone.is(Constant.Zone.Play) && zone.getPlayer().equals(AllZone.HumanPlayer)) {
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
				AllZone.Stack.add(sacrificeCreature);
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
			
			AllZone.Stack.add(sacrificeLand);
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
		CardList eves = AllZoneUtil.getPlayerCardsRemovedFromGame(player, "All Hallow's Eve");
		
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
			sb.append(eve.getName()).append(" - remove a scream counter and return creatures to play.");
			hallow.setStackDescription(sb.toString());
			if(AllZone.GameAction.isCardExiled(eve)) {
				AllZone.Stack.add(hallow);
			}
		}//end for
	}//All_Hallows_Eve

	private static void upkeep_Defiler_of_Souls() {
		/*
		 * At the beginning of each player's upkeep, destroy target
		 * nonartifact creature that player controls of his or her
		 * choice. It can't be regenerated.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList defilers = AllZoneUtil.getCardsInPlay("Defiler of Souls");
		
		for(Card c:defilers) {
			final Card defiler = c;
			
			final Ability sacrificeCreature = new Ability(defiler, "") {
				@Override
				public void resolve() {
					if(player.equals(AllZone.HumanPlayer)) {
						AllZone.InputControl.setInput( new Input() {
							private static final long serialVersionUID = 8013298767165776609L;
							public void showMessage() {
								AllZone.Display.showMessage("Defiler of Souls - Select a monocolored creature to sacrifice");
								ButtonUtil.disableAll();
							}
							public void selectCard(Card selected, PlayerZone zone) {
								//probably need to restrict by controller also
								if(selected.isCreature() && CardUtil.getColors(selected).size() == 1 && !selected.isColorless() 
										&& zone.is(Constant.Zone.Play) && zone.getPlayer().equals(AllZone.HumanPlayer)) {
									AllZone.GameAction.sacrificeDestroy(selected);
									stop();
								}
							}//selectCard()
						});//Input
					}
					else { //computer
						CardList targets = Defiler_of_Souls_getTargets(player,defiler);
						Card target = CardFactoryUtil.AI_getWorstCreature(targets);
						if(null == target) {
							//must be nothing valid to destroy
						}
						else AllZone.GameAction.sacrificeDestroy(target);
					}
				}//resolve
			};//sacrificeCreature
			sacrificeCreature.setStackDescription("Defiler of Souls - Select a monocolored creature to sacrifice");
			if(Defiler_of_Souls_getTargets(player,defiler).size() > 0)
				AllZone.Stack.add(sacrificeCreature);
		}//end for
	}//The Abyss
	
	private static CardList Defiler_of_Souls_getTargets(final Player player, Card card) {
		CardList creats = AllZoneUtil.getCreaturesInPlay(player);
		String mono[] = {"Creature.MonoColor"};
		creats = creats.getValidCards(mono,player,card);
		return creats;
	}
	
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
								if(artifact.isArtifact() && zone.is(Constant.Zone.Play)
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
			
			AllZone.Stack.add(sacrificeArtifact);
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
				
				AllZone.Stack.add(sevenDamage);
			}
			else {
				
				StringBuilder sb = new StringBuilder();
				sb.append(c.getName()).append(" - sacrifice a creature.");
				sacrificeCreature.setStackDescription(sb.toString());
				
				AllZone.Stack.add(sacrificeCreature);
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
			
			AllZone.Stack.add(ability);
		}//end for
	}// upkeep_Drop_of_Honey()
	
	/**
	 * runs the upkeep for Genesis
	 */
	private static void upkeep_Genesis() {
		/*
		 * At the beginning of your upkeep, if Genesis is in your graveyard,
		 * you may pay 2G. If you do, return target creature card from your 
		 * graveyard to your hand.
		 */
		
		// The ordering is a bit off, it should be: trigger, add ability, resolve, pay mana, if paid, return creature.
		
		final Player player = AllZone.Phase.getPlayerTurn();
		final CardList grave = AllZoneUtil.getPlayerGraveyard(player, "Genesis");

		for(int i = 0; i < grave.size(); i++) {
			final Card c = grave.get(i);

			final Ability ability = new Ability(c, "2 G") {
				CardListFilter creatureFilter = new CardListFilter() {
					public boolean addCard(Card c) {
						return c.isCreature();
					}
				};
				@Override
				public void resolve() {
					// should check if Genesis is still there
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
					PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
					if(player.equals(AllZone.HumanPlayer) && grave.size() > 0) {
							CardList creatures = AllZoneUtil.getPlayerGraveyard(player);
							creatures = creatures.filter(creatureFilter);
							Object creatureChoice = AllZone.Display.getChoice("Creature to move to hand", creatures.toArray());
							Card creatureCard = (Card) creatureChoice;
	                        graveyard.remove(creatureCard);
	                        hand.add(creatureCard);
						//}//end choice="Yes"
					}
					else{ //computer resolve
						CardList compCreatures = AllZoneUtil.getPlayerGraveyard(player);
						compCreatures = compCreatures.filter(creatureFilter);
						Card target = CardFactoryUtil.AI_getBestCreature(compCreatures);
						graveyard.remove(target);
                        hand.add(target);
					}
				}
			};

			final Command paidCommand = new Command() {
				private static final long serialVersionUID = -5102763277280782548L;

				public void execute() {
					/*
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName()).append(" - return 1 creature from your graveyard to your hand");
					ability.setStackDescription(sb.toString());

					AllZone.Stack.add(ability);
					*/
					
					// Resolve through the Command, since it shouldn't hit the stack again. 
					// Not a great solution, but not terrible
					ability.resolve();
				}
			};

			//AllZone.Stack.add(ability);
			if(c.getController().equals(AllZone.HumanPlayer)) {
				String[] choices = {"Yes", "No"};
				Object choice = AllZone.Display.getChoice("Use Genesis?", choices);
				if(choice.equals("Yes")) {
					Ability pay = new Ability(c, "0"){
						public void resolve() {
							if (AllZone.getZone(c).is(Constant.Zone.Graveyard)){
								GameActionUtil.payManaDuringAbilityResolve("Pay cost for " + c + "\r\n", ability.getManaCost(), 
										paidCommand, Command.Blank);
							}
							else{
								System.out.println("Genesis no longer in graveyard");
							}
							
						}
					};
					pay.setStackDescription("Genesis - Upkeep Ability");
					
					AllZone.Stack.add(pay);
				}
			} else //computer
			{
				if(ComputerUtil.canPayCost(ability)) {
					ComputerUtil.payManaCost(ability);
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName()).append(" - return 1 creature from your graveyard to your hand");
					ability.setStackDescription(sb.toString());
					
					AllZone.Stack.add(ability);
				}
			}
		}
	}//upkeep_Genesis
	
	
//upkeep_Demonic_Hordes
	
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
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
						PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
						CardList playerLand = AllZoneUtil.getPlayerLandsInPlay(player);
						
						if((c.getController().equals(AllZone.ComputerPlayer)) && (playerLand.size() > 0)) {
							AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanent(playerLand, c.getName()+" - Select a land to sacrifice."));
							play.remove(playerLand);
							graveyard.add(playerLand);
							c.tap();
						}
						else {
							if((c.getController().equals(AllZone.ComputerPlayer)) && (playerLand.size() == 0)) {
								c.tap();
							}
						}
						if((c.getController().equals(AllZone.HumanPlayer)) && (playerLand.size() > 0)) {
								Card target = CardFactoryUtil.AI_getBestLand(playerLand);
								play.remove(target);
								graveyard.add(target);
								c.tap();
							}
						else {
							if((c.getController().equals(AllZone.HumanPlayer)) && (playerLand.size() == 0)) {
								c.tap();
							}
						}
					} //end resolve()
			}; //end noPay ability
			
			if(c.getController().equals(AllZone.HumanPlayer)) {
				String[] choices = {"Yes", "No"};
				Object choice = AllZone.Display.getChoice("Pay Demonic Hordes upkeep cost?", choices);
				if(choice.equals("Yes")) {
					final Ability pay = new Ability(c, "0") {
						private static final long serialVersionUID = 4820011440853920644L;
						public void resolve() {
							if (AllZone.getZone(c).is(Constant.Zone.Play)) {
								GameActionUtil.payManaDuringAbilityResolve("Pay cost for " + c + "\r\n", noPay.getManaCost(), Command.Blank, Command.Blank);
							}
						} //end resolve()
					}; //end pay ability
					pay.setStackDescription("Demonic Hordes - Upkeep Cost");
					AllZone.Stack.add(pay);
				} //end choice
				else {
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
					noPay.setStackDescription(sb.toString());
					AllZone.Stack.add(noPay);
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
					AllZone.Stack.add(computerPay);
				} 
				else {
					AllZone.Stack.add(noPay);
				}
			} //end computer
			
		} //end for loop
			
	} //upkeep_Demonic_Hordes

	//END UPKEEP CARDS

	//START ENDOFTURN CARDS

    public static void endOfTurn_Wall_Of_Reverence()
    {
        final Player player = AllZone.Phase.getPlayerTurn();
        final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
                        Object o = AllZone.Display.getChoiceOptional("Select creature for Wall of Reverence life gain", validTargets.toArray());
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
            
            AllZone.Stack.add(ability);
        }
    }//endOfTurn_Wall_Of_Reverence()
	
	public static void endOfTurn_Predatory_Advantage()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player.getOpponent());
		CardList list = new CardList(playZone.getCards());
		list = list.getName("Predatory Advantage");
		for (int i = 0; i < list.size(); i++) {
		if(player == AllZone.HumanPlayer && Phase.PlayerCreatureSpellCount == 0) 
			CardFactoryUtil.makeToken("Lizard", "G 2 2 Lizard", list.get(i).getController(), "G", new String[] {"Creature", "Lizard"}, 2, 2, new String[] {""});
			else if(player == AllZone.ComputerPlayer && Phase.ComputerCreatureSpellCount == 0) 
				CardFactoryUtil.makeToken("Lizard", "G 2 2 Lizard", list.get(i).getController(), "G", new String[] {"Creature", "Lizard"}, 2, 2, new String[] {""});
		}
	}
	
	public static void endOfTurn_Lighthouse_Chronologist() 
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, opponent);
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
			
			AllZone.Stack.add(ability);
		}
	}
	
	public static void endOfTurn_Thran_Quarry() 
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		controlNoTypeSacrifice("Thran Quarry", "Creature", player);
		controlNoTypeSacrifice("Thran Quarry", "Creature", opponent);
	}
	
	public static void endOfTurn_Glimmervoid() 
	{ 		
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		controlNoTypeSacrifice("Glimmervoid", "Artifact", player);
		controlNoTypeSacrifice("Glimmervoid", "Artifact", opponent);
	}
	
	public static void controlNoTypeSacrifice(String name, String type, Player player)
	{
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		CardList list = new CardList(playZone.getCards());

		CardList nameList = list.getName(name);
		
		if (nameList.size() == 0) return;
		
		CardList typeList = list.getType(type);
		
		if (typeList.size() == 0){
			for(Card c : nameList){
				AllZone.GameAction.sacrifice(c);
			}
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
		while(i+1 < grave.size()){
			Card c = grave.get(i);
			ArrayList<String> types = grave.get(i+1).getType();
			if (c.getName().equals("Krovikan Horror") && types.contains("Creature")){
				AllZone.GameAction.moveToHand(c);
				grave.remove(c);
			}
			else
				i++;
		}
	}
	//END ENDOFTURN CARDS

	public static void removeAttackedBlockedThisTurn() {
		// resets the status of attacked/blocked this turn
		Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());
		list = list.getType("Creature");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(c.getCreatureAttackedThisCombat()) c.setCreatureAttackedThisCombat(false);
			if(c.getCreatureBlockedThisCombat()) c.setCreatureBlockedThisCombat(false);
			//do not reset setCreatureAttackedThisTurn(), this appears to be combat specific

			if(c.getCreatureGotBlockedThisCombat()) c.setCreatureGotBlockedThisCombat(false);

			c.resetReceivedDamageFromThisTurn();
		}
		
		AllZone.GameInfo.setAssignedFirstStrikeDamageThisCombat(false);
		AllZone.GameInfo.setResolvedFirstStrikeDamageThisCombat(false);
	}

	public static void executeAllyEffects(Card c) {
		if(c.getName().equals("Kazandu Blademaster") || c.getName().equals("Makindi Shieldmate")
				|| c.getName().equals("Nimana Sell-Sword") || c.getName().equals("Oran-Rief Survivalist")
				|| c.getName().equals("Tuktuk Grunts") || c.getName().equals("Umara Raptor")
				|| c.getName().equals("Hada Freeblade") || c.getName().equals("Bojuka Brigand")
				|| c.getName().equals("Graypelt Hunter")) ally_Generic_P1P1(c);
		else if(c.getName().equals("Turntimber Ranger")) ally_Turntimber_Ranger(c);
		else if(c.getName().equals("Highland Berserker")) ally_BoostUntilEOT(c, "First Strike");
		else if(c.getName().equals("Joraga Bard")) ally_BoostUntilEOT(c, "Vigilance");
		else if(c.getName().equals("Seascape Aerialist")) ally_BoostUntilEOT(c, "Flying");
		else if(c.getName().equals("Ondu Cleric")) ally_Ondu_Cleric(c);
		else if(c.getName().equals("Kazuul Warlord")) ally_Kazuul_Warlord(c);

	}

	private static boolean showAllyDialog(Card c) {
		String[] choices = {"Yes", "No"};

		Object q = null;

		q = AllZone.Display.getChoiceOptional("Use " + c.getName() + "'s Ally ability?", choices);

		if(q == null || q.equals("No")) return false;
		else return true;
	}

	private static void ally_Generic_P1P1(Card c) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.addCounter(Counters.P1P1, 1);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Ally: gets a +1/+1 counter.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showAllyDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}

	private static void ally_Turntimber_Ranger(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", crd.getController(), "G", new String[] {"Creature", "Wolf"}, 2, 2,
						new String[] {""});
				crd.addCounter(Counters.P1P1, 1);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Ally: ").append(c.getController());
		sb.append(" puts a 2/2 green Wolf creature token onto the battlefield, and adds a +1/+1 on ");
		sb.append(c.getName()).append(".");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showAllyDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) {

			PlayerZone cPlay = AllZone.Computer_Play;
			CardList list = new CardList();
			list.addAll(cPlay.getCards());

			CardList cl = list.filter(new CardListFilter() {
				public boolean addCard(Card crd) {
					return crd.getName().equals("Conspiracy") && crd.getChosenType().equals("Ally");
				}
			});

			list = list.getName("Wolf");

			if((list.size() > 15 && cl.size() > 0)) ;
			else AllZone.Stack.add(ability);
		}
	}

	private static void ally_BoostUntilEOT(Card c, String k) {
		final Card crd = c;
		final String keyword = k;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
				CardList list = new CardList(play.getCards());
				list = list.getType("Ally");

				final CardList allies = list;

				final Command untilEOT = new Command() {

					private static final long serialVersionUID = -8434529949884582940L;

					public void execute() {
						for(Card creat:allies) {
							if(AllZone.GameAction.isCardInPlay(creat)) {
								creat.removeExtrinsicKeyword(keyword);
							}
						}
					}
				};//Command

				for(Card creat:allies) {
					if(AllZone.GameAction.isCardInPlay(creat)) {
						creat.addExtrinsicKeyword(keyword);
					}
				}
				AllZone.EndOfTurn.addUntil(untilEOT);

			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Ally: Ally creatures you control gain ");
		sb.append(keyword).append(" until end of turn.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showAllyDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}

	private static void ally_Ondu_Cleric(final Card c) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
				CardList allies = new CardList(play.getCards());
				allies = allies.getType("Ally");
				crd.getController().gainLife(allies.size(), c);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Ally: gain life equal to the number of allies you control.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showAllyDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}

	private static void ally_Kazuul_Warlord(Card c) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
				CardList list = new CardList(play.getCards());
				list = list.getType("Ally");

				for(Card ally:list) {
					ally.addCounter(Counters.P1P1, 1);
				}
			}
		};// Ability
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Ally: put a +1/+1 counter on each Ally creature you control.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showAllyDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}


	public static void executeDestroyCardEffects(Card c, Card destroyed) {
		if(destroyed.isCreature()) executeDestroyCreatureCardEffects(c, destroyed);
		if(destroyed.isLand()) executeDestroyLandCardEffects(c, destroyed);
		if(destroyed.isEnchantment()) executeDestroyEnchantmentCardEffects(c, destroyed);
	}
	
    public static boolean showYesNoDialog(Card c, String question) {
        
        StringBuilder title = new StringBuilder();
        title.append(c.getName()).append(" - Ability");
        
        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }
        
        int answer = JOptionPane.showConfirmDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION);
        
        if (answer == JOptionPane.YES_OPTION) return true;
        else return false;
    }
/*
    private static boolean showDialog(Card c) {
        String[] choices = {"Yes", "No"};

        Object q = null;

        q = AllZone.Display.getChoiceOptional("Use " + c.getName() + " effect?", choices);

        if(q == null || q.equals("No")) return false;
        else return true;
    }
*/
    //***CREATURES START HERE***

	public static void executeDestroyCreatureCardEffects(Card c, Card destroyed) {
		//if (AllZone.GameAction.isCardInPlay(c)){
		if(c.getName().equals("Goblin Sharpshooter")) destroyCreature_Goblin_Sharpshooter(c, destroyed);
		else if(c.getName().equals("Dingus Staff")) destroyCreature_Dingus_Staff(c, destroyed);
		else if(c.getName().equals("Dauthi Ghoul") && destroyed.getKeyword().contains("Shadow")) destroyCreature_Dauthi_Ghoul(
				c, destroyed);
		else if(c.getName().equals("Prowess of the Fair") && destroyed.isType("Elf")
				&& !destroyed.isToken() && !c.equals(destroyed)
				&& destroyed.getController().equals(c.getController())) destroyCreature_Prowess_of_the_Fair(c,
						destroyed);
		else if(c.getName().equals("Fecundity")) destroyCreature_Fecundity(c, destroyed);
		else if(c.getName().equals("Moonlit Wake")) destroyCreature_Moonlit_Wake(c, destroyed);
		else if(c.getName().equals("Proper Burial") && destroyed.getController().equals(c.getController())) destroyCreature_Proper_Burial(
				c, destroyed);
		else if(c.getName().equals("Sek'Kuar, Deathkeeper") && !destroyed.isToken()
				&& destroyed.getController().equals(c.getController()) && !destroyed.getName().equals(c.getName())) destroyCreature_SekKuar(
						c, destroyed);
		//}
	}

	//***

	private static void destroyCreature_Goblin_Sharpshooter(Card c, Card destroyed) {
		//not using stack for this one
		if(AllZone.GameAction.isCardInPlay(c) && c.isTapped()) c.untap();
	}

	private static void destroyCreature_Dingus_Staff(Card c, Card destroyed) {
		final Card crd = destroyed;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				Player player = crd.getController();
				player.loseLife(2,crd);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Dingus Staff - Deals 2 damage to ").append(destroyed.getController()).append(".");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}

	private static void destroyCreature_Dauthi_Ghoul(Card c, Card destroyed) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				if(AllZone.GameAction.isCardInPlay(crd)) crd.addCounter(Counters.P1P1, 1);
			}
		};
		if(AllZone.GameAction.isCardInPlay(c)) ability.setStackDescription("Dauthi Ghoul - gets a +1/+1 counter.");
		AllZone.Stack.add(ability);
	}

	private static void destroyCreature_Prowess_of_the_Fair(Card c, Card destroyed) {
        final Card crd = c;
        final Card crd2 = c;
        
        Ability ability = new Ability(c, "0") {
            @Override
            public void resolve() {
                Player player = crd.getController();
                if (player.isHuman()) {
                    String question = "Put a 1/1 green Elf Warrior creature token onto the battlefield?";
                    if (showYesNoDialog(crd2, question)) makeToken();
                } else makeToken();
            }

            public void makeToken() {
                CardFactoryUtil.makeToken("Elf Warrior", "G 1 1 Elf Warrior", crd.getController(), "G", 
                        new String[] {"Creature", "Elf", "Warrior"}, 1, 1, new String[] {""});
            }
        };
        
        StringBuilder sb = new StringBuilder();
        sb.append("Prowess of the Fair - ").append(c.getController());
        sb.append(" may put a 1/1 green Elf Warrior creature token onto the battlefield.");
        ability.setStackDescription(sb.toString());
        
        AllZone.Stack.add(ability);
    }

	private static void destroyCreature_Fecundity(Card c, Card destroyed) {
        final Card crd = destroyed;
        final Card crd2 = c;

        Ability ability = new Ability(c, "0") {
            @Override
            public void resolve() {
                Player player = crd.getController();
                if (player.isHuman()) {
                    String question = "Draw a card?";
                    if (showYesNoDialog(crd2, question)) player.drawCard();
                } else player.drawCard(); //computer
            }
        };
        
        StringBuilder sb = new StringBuilder();
        sb.append("Fecundity - ").append(destroyed.getController()).append(" may draw a card.");
        ability.setStackDescription(sb.toString());

        AllZone.Stack.add(ability);
    }

	private static void destroyCreature_Moonlit_Wake(final Card c, Card destroyed) {
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				c.getController().gainLife(1, c);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Moonlit Wake - ").append(c.getController()).append(" gains 1 life.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}

	private static void destroyCreature_Proper_Burial(final Card c, Card destroyed) {
		final Card crd = c;
		final Card crd2 = destroyed;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.getController().gainLife(crd2.getNetDefense(), c);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Proper Burial - ").append(c.getController()).append(" gains ");
		sb.append(destroyed.getNetDefense()).append(" life.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}

	private static void destroyCreature_SekKuar(Card c, Card destroyed) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardFactoryUtil.makeToken("Graveborn", "BR 3 1 Graveborn", crd.getController(), "BR", new String[] {
						"Creature", "Graveborn"}, 3, 1, new String[] {"Haste"});
			}
		};
		ability.setStackDescription("Sek'Kuar, Deathkeeper - put a 3/1 black and red Graveborn creature token with haste onto the battlefield.");
		AllZone.Stack.add(ability);
	}

	//***CREATURES END HERE***

	//***LANDS START HERE***

	public static void executeDestroyLandCardEffects(Card c, Card destroyed) {
		if(c.getName().equals("Dingus Egg")) destroyLand_Dingus_Egg(c, destroyed);
	}

	//***

	private static void destroyLand_Dingus_Egg(Card c, Card destroyed) {
		final Card crd = destroyed;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				Player player = crd.getController();
				player.addDamage(2, crd);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Dingus Egg - Deals 2 damage to ").append(destroyed.getController()).append(".");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}

	public static void executeGrvDestroyCardEffects(Card c, Card destroyed) {
		if(c.getName().contains("Bridge from Below") && destroyed.getController().equals(c.getController())
				&& !destroyed.isToken() && destroyed.isCreature()) destroyCreature_Bridge_from_Below_maketoken(c, destroyed);
		if(c.getName().contains("Bridge from Below") && !destroyed.getController().equals(c.getController()) && destroyed.isCreature()) destroyCreature_Bridge_from_Below_remove(c);
	}

	private static void destroyCreature_Bridge_from_Below_maketoken(Card c, Card destroyed) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardFactoryUtil.makeToken("Zombie", "B 2 2 Zombie", crd.getController(), "B", new String[] {"Creature", "Zombie"},
						2, 2, new String[] {""});
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Bridge from Below - ").append(c.getController());
		sb.append("puts a 2/2 black Zombie creature token onto the battlefield.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}

	private static void destroyCreature_Bridge_from_Below_remove(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone grv = AllZone.getZone(Constant.Zone.Graveyard, crd.getController());
				PlayerZone exile = AllZone.getZone(Constant.Zone.Removed_From_Play, crd.getController());
				grv.remove(crd);
				exile.add(crd);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Bridge from Below - ").append(c.getController()).append(" exile Bridge from Below.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}


	//***LANDS END HERE***

	//***ENCHANTMENTS START HERE***

	public static void executeDestroyEnchantmentCardEffects(Card c, Card destroyed) {
		if(c.getName().equals("Femeref Enchantress")) destroyEnchantment_Femeref_Enchantress(c, destroyed);
	}


	//***

	public static void destroyEnchantment_Femeref_Enchantress(Card c, Card destroyed) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.getController().drawCard();
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Femeref Enchantress - ").append(c.getController()).append(" draws a card.");
		ability.setStackDescription(sb.toString());

		AllZone.Stack.add(ability);
	}

	//***ENCHANTMENTS END HERE***

	public static void executeLandfallEffects(Card c) {
		
		ArrayList<String> kws = c.getKeyword();
		for (String kw : kws){
			if (kw.equals("Landfall - Whenever a land enters the battlefield under your control, CARDNAME gets +2/+2 until end of turn."))
			landfall_Generic_P2P2_UntilEOT(c);
		}

		if(c.getName().equals("Rampaging Baloths")) landfall_Rampaging_Baloths(c);
		else if(c.getName().equals("Emeria Angel")) landfall_Emeria_Angel(c);
		else if(c.getName().equals("Ob Nixilis, the Fallen")) landfall_Ob_Nixilis(c);
		else if(c.getName().equals("Ior Ruin Expedition")
				|| c.getName().equals("Khalni Heart Expedition")) landfall_AddQuestCounter(c);
		else if(c.getName().equals("Lotus Cobra")) landfall_Lotus_Cobra(c);
		else if(c.getName().equals("Hedron Crab")) landfall_Hedron_Crab(c);
		else if(c.getName().equals("Bloodghast")) landfall_Bloodghast(c);
		else if(c.getName().equals("Avenger of Zendikar")) landfall_Avenger_of_Zendikar(c);
		else if(c.getName().equals("Eternity Vessel")) landfall_Eternity_Vessel(c);
	}
	
	private static boolean checkValakutCondition(Card valakutCard, Card mtn) {
		// Get a list of all mountains
		CardList mountainList = AllZoneUtil.getPlayerTypeInPlay(valakutCard.getController(),
				"Mountain");
		// Don't count the one that just came into play
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
        	AllZone.Stack.add(DamageTgt);
        }
        return true; // Tell the calling routine it's okay to call again if there are other Valakuts in play
	}


	private static boolean showLandfallDialog(Card c) {
		String[] choices = {"Yes", "No"};

		Object q = null;

		q = AllZone.Display.getChoiceOptional("Use " + c.getName() + " Landfall?", choices);

		if(q == null || q.equals("No")) return false;
		else return true;
	}

	private static void landfall_Generic_P2P2_UntilEOT(Card c)
	{
		final Card crd = c;
		Ability ability = new Ability(c, "0")
		{
			@Override
			public void resolve()
			{
				final Command untilEOT = new Command() {
					private static final long serialVersionUID = 8919719388859986796L;

					public void execute() {
						if(AllZone.GameAction.isCardInPlay(crd)) {
							crd.addTempAttackBoost(-2);
							crd.addTempDefenseBoost(-2);
						}
					}
				};
				crd.addTempAttackBoost(2);
				crd.addTempDefenseBoost(2);

				AllZone.EndOfTurn.addUntil(untilEOT);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c).append(" - Landfall: gets +2/+2 until EOT.");
		ability.setStackDescription(sb.toString());

		/*if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer))*/
		AllZone.Stack.add(ability);
	}

	private static void landfall_Rampaging_Baloths(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardFactoryUtil.makeToken("Beast", "G 4 4 Beast", crd.getController(), "G", new String[] {"Creature", "Beast"}, 4,
						4, new String[] {""});
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Landfall: ").append(c.getController());
		sb.append(" puts a 4/4 green Beast creature token onto the battlefield.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);

	}

	private static void landfall_Emeria_Angel(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				CardFactoryUtil.makeToken("Bird", "W 1 1 Bird", crd.getController(), "W", new String[] {"Creature", "Bird"}, 1, 1,
						new String[] {"Flying"});
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Landfall: ").append(c.getController());
		sb.append(" puts a 1/1 white Bird creature token with flying onto the battlefield.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}//landfall_Emeria_Angel

	private static void landfall_Ob_Nixilis(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.getController().getOpponent().loseLife(3, crd);
				crd.addCounter(Counters.P1P1, 3);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Landfall: ").append(c.getController().getOpponent());
		sb.append(" loses 3 life and ").append(c.getName()).append(" gets three +1/+1 counters.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		} else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}

	private static void landfall_AddQuestCounter(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.addCounter(Counters.QUEST, 1);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - gets a Quest counter.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) AllZone.Stack.add(ability);
	}

	private static void landfall_Lotus_Cobra(Card c) {
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				String color = "";

				Object o = AllZone.Display.getChoice("Choose mana color", Constant.Color.Colors);
				color = (String) o;

				if(color.equals("white")) color = "W";
				else if(color.equals("blue")) color = "U";
				else if(color.equals("black")) color = "B";
				else if(color.equals("red")) color = "R";
				else if(color.equals("green")) color = "G";

				Card mp = AllZone.ManaPool;

				mp.addExtrinsicKeyword("ManaPool:" + color);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - add one mana of any color to your mana pool.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		}

	}

	private static void landfall_Hedron_Crab(Card c) {
		//final Card crd = c;
		final Player targetPlayer = c.getController().getOpponent();
		final Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				getTargetPlayer().mill(3);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - Landfall: ").append(targetPlayer);
		sb.append(" puts the top three cards of his or her library into his or her graveyard.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			AllZone.InputControl.setInput(CardFactoryUtil.input_targetPlayer(ability));
			//AllZone.Stack.add(ability);
		}

		else if(c.getController().equals(AllZone.ComputerPlayer)) {
			//ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
			ability.setTargetPlayer(AllZone.HumanPlayer);
			AllZone.Stack.add(ability);

		}
	}//landfall_Hedron_Crab

	private static void landfall_Bloodghast(Card c) {
		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
		if(!AllZone.GameAction.isCardInZone(c, grave)) return;

		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
				PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, crd.getController());

				if(AllZone.GameAction.isCardInZone(crd, grave)) {
					grave.remove(crd);
					play.add(crd);
				}
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c).append(" - return Bloodghast from your graveyard to the battlefield.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		} else if(c.getController().equals(AllZone.ComputerPlayer)) {
			AllZone.Stack.add(ability);
		}

		AllZone.GameAction.checkStateEffects();

	}//landfall_Bloodghast

	private static void landfall_Avenger_of_Zendikar(Card c) {
		final Card crd = c;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, crd.getController());
				CardList plants = new CardList(play.getCards());
				plants = plants.filter(new CardListFilter() {
					public boolean addCard(Card card) {
						return (card.isCreature() && card.isType("Plant"));
					}
				});

				for(Card plant:plants)
					plant.addCounter(Counters.P1P1, 1);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(c).append(" - put a +1/+1 counter on each Plant creature you control.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		} else if(c.getController().equals(AllZone.ComputerPlayer)) {
			AllZone.Stack.add(ability);
		}

	}//landfall_Avenger
	
	private static void landfall_Eternity_Vessel(final Card c) {
		final Card crd = c;
		Card biggest = null;
		if(c.getController() == AllZone.ComputerPlayer) {
			CardList vessels = new CardList();
			PlayerZone zone = AllZone.getZone(Constant.Zone.Play, c.getController());
			if(zone != null) {
				vessels.addAll(zone.getCards());
				vessels = vessels.getName("Eternity Vessel");
				biggest = vessels.get(0);
				for(int i = 0; i < vessels.size(); i++)
					if(biggest.getCounters(Counters.CHARGE) < vessels.get(i).getCounters(Counters.CHARGE)) biggest = vessels.get(i);                         
			}
		}
        final Card compVessel = biggest;
		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				Card Target = null;
				if(crd.getController() == AllZone.HumanPlayer) Target = crd;
				else Target = compVessel;
				
                        int lifeGain = Target.getCounters(Counters.CHARGE);
                        Target.getController().setLife(lifeGain, c);
			}
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append("Landfall: Whenever a land enters the battlefield under your control, you may ");
		sb.append("have your life total become the number of charge counters on Eternity Vessel.");
		ability.setStackDescription(sb.toString());

		if(c.getController().equals(AllZone.HumanPlayer)) {
			if(showLandfallDialog(c)) AllZone.Stack.add(ability);
		} else if(c.getController().equals(AllZone.ComputerPlayer)) {
			CardList Hexmages = new CardList();
			PlayerZone zone = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
			if(zone != null) {
				Hexmages.addAll(zone.getCards());
				Hexmages = Hexmages.getName("Vampire Hexmage");
				int clife = AllZone.ComputerPlayer.getLife();			
				if(compVessel.getCounters(Counters.CHARGE) > clife && (Hexmages.size() == 0)) AllZone.Stack.add(ability);
			}
		}

	}//landfall_Eternity_Vessel

	public static void executeLifeLinkEffects(final Card c, int n) {
		final Player player = c.getController();

		final int power = n;

		Ability ability2 = new Ability(c, "0") {
			@Override
			public void resolve() {
				player.gainLife(power, c);
			}
		}; // ability2
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" (Lifelink) - ").append(player);
		sb.append(" gains ").append(power).append(" life.");
		ability2.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability2);
	}

	public static void executeGuiltyConscienceEffects(Card c, Card source) {
		int pwr = c.getNetAttack();
		if(CombatUtil.isDoranInPlay()) pwr = c.getNetDefense();
		final int damage = pwr;
		final Card src = source;

		final Card crd = c;
		Ability ability2 = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.addDamage(damage, src);
			}
		}; // ability2
		
		StringBuilder sb = new StringBuilder();
		sb.append("Guilty Conscience deals ").append(damage).append(" damage to ").append(c.getName());
		ability2.setStackDescription(sb.toString());
		
		if (damage >= 0)
			AllZone.Stack.add(ability2);
	}

	public static void executeGuiltyConscienceEffects(Card c, Card source, int n) {
		final int damage = n;
		final Card crd = c;
		final Card src = source;
		Ability ability2 = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.addDamage(damage, src);
			}
		}; // ability2
		
		StringBuilder sb = new StringBuilder();
		sb.append("Guilty Conscience deals ").append(n).append(" damage to ").append(c.getName());
		ability2.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability2);
	}

	public static void executeSwordOfFireAndIceEffects(Card source) {
		final Card src = source;
		Ability ability = new Ability(src, "0") {
			@Override
			public void resolve() {
				Card target = getTargetCard();
				if(target != null)
					target.addDamage(2, src);
				else {
					getTargetPlayer().addDamage(2, src);
				}
				
				src.getController().drawCard();
			}
		}; // ability

		ability.setChooseTargetAI(CardFactoryUtil.AI_targetHumanCreatureOrPlayer());
		ability.setStackDescription("Sword of Fire and Ice - Deals 2 damage to target creature or player and you draw a card." );
		if (src.getController() == AllZone.HumanPlayer) {
	       	AllZone.InputControl.setInput(CardFactoryUtil.input_targetCreaturePlayer(ability, true, true));
	    } else {
	    	ability.chooseTargetAI();
	       	AllZone.Stack.add(ability);
	    }
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
                	 AllZone.Stack.add(ability);
                	 return;
                 }
                 
                 if(src.getController().equals(AllZone.HumanPlayer)) {
                     Object o = AllZone.Display.getChoiceOptional("Select target card", list.toArray());
                     if(o != null) {
                         ability.setTargetCard((Card) o);
                         AllZone.Stack.add(ability);
                     }
                 }//if
                 else//computer
                 {
                     Card best = CardFactoryUtil.AI_getBestCreature(list);
                     ability.setTargetCard(best);
                     AllZone.Stack.add(ability);
                 }
             }//execute()
        };//Command
        
        StringBuilder sb = new StringBuilder();
        sb.append("Sword of Light and Shadow - You gain 3 life and you may return ");
        sb.append("up to one target creature card from your graveyard to your hand");
        ability.setStackDescription(sb.toString());
		
		res.execute();
	}
	
	public static void executeSwordOfBodyAndMindEffects(Card source)
	{
		final Card src = source;
		final Ability ability = new Ability(src, "0") {
			@Override
			public void resolve() {
				Player opponent = src.getController().getOpponent();
				
				CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", src.getController(), "G",
						new String[] {"Creature", "Wolf"}, 2, 2, new String[] {""});
				
				opponent.mill(10);
			}
		}; // ability
		
		StringBuilder sb = new StringBuilder();
		sb.append("Sword of Body and Mind - put a 2/2 green Wolf creature token onto the battlefield ");
		sb.append("and opponent puts the top ten cards of his or her library into his or her graveyard.");
		ability.setStackDescription(sb.toString());

		AllZone.Stack.add(ability);
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
                
                AllZone.Stack.add(ability2);
            }
        }
    }

    //not restricted to just combat damage:
    public static void executePlayerDamageEffects(final Player player, final Card c, final int damage, boolean isCombatDamage)
    {
    	if (damage > 0)
    	{
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
    					
    					AllZone.Stack.add(ability);
    				}
    			}//auras > 0
    		}//end c.isEnchanted()
    		
    		/*
    		 * Darien, King of Kjeldor - 
    		 * Whenever you're dealt damage, you may put that many 1/1 white
    		 * Soldier creature tokens onto the battlefield.
    		 */
    		if( playerPerms.getName("Darien, King of Kjeldor").size() > 0) {
    			CardList dariens = playerPerms.getName("Darien, King of Kjeldor");
    			for(Card crd:dariens) {
    				final Card darien = crd;
    				SpellAbility ability = new Ability(darien, "0") {
    					public void resolve() {
    						for(int i = 0; i < damage; i++)
    							CardFactoryUtil.makeToken11WSoldier(darien.getController());
    					}
    				};
    				
    				StringBuilder sb = new StringBuilder();
    				sb.append(darien.getName()).append(" - ").append(darien.getController());
    				sb.append(" puts ").append(damage).append(" Soldier tokens in play.");
    				ability.setStackDescription(sb.toString());
    				
    				AllZone.Stack.add(ability);
    			}
    		}
    		if (playerPerms.getName("Dissipation Field").size() > 0)  {  
    			CardList disFields = playerPerms.getName("Dissipation Field");
    			for (int i=0;i<disFields.size();i++) {
    				Card crd = disFields.get(i);
    				playerDamage_Dissipation_Field(c, crd);
    			}
    		}
    		if (c.isCreature() && (playerPerms.getName("Dread").size() > 0 || playerPerms.getName("No Mercy").size() > 0))
    		{
    			CardList l = playerPerms.filter(new CardListFilter()
    			{
    				public boolean addCard(Card crd)
    				{
    					return crd.getName().equals("Dread") || crd.getName().equals("No Mercy");
    				}
    			});
    			for (Card crd:l)
    				playerDamage_No_Mercy(c, crd);
    		}
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
    		
	    	if(c.getKeyword().contains("Whenever this creature deals damage to a player, that player gets a poison counter."))
				playerCombatDamage_PoisonCounter(c, 1);
	    
	    	if(c.getName().equals("Marsh Viper")) playerCombatDamage_PoisonCounter(c, 2);
	    	else if(c.getName().equals("Abyssal Specter")) playerCombatDamage_Abyssal_Specter(c);
	    	else if(c.getName().equals("Silent Specter")) playerCombatDamage_Silent_Specter(c);
	    	else if(c.getName().equals("Nicol Bolas")) playerCombatDamage_Nicol_Bolas(c);
			else if(c.getName().equals("Goblin Lackey")) playerCombatDamage_Goblin_Lackey(c);
			else if(c.getName().equals("Thieving Magpie")|| c.getName().equals("Lu Xun, Scholar General")) playerCombatDamage_Shadowmage_Infiltrator(c);
			else if(c.getName().equals("Warren Instigator")) playerCombatDamage_Warren_Instigator(c);
			else if(c.getName().equals("Whirling Dervish") || c.getName().equals("Dunerider Outlaw")) 
				playerCombatDamage_Whirling_Dervish(c);
	    	
	    	if (isCombatDamage)
	    		c.setDealtCombatDmgToOppThisTurn(true);
	    		
    	}
    }
    
	public static void executePlayerCombatDamageEffects(Card c) {
		// Whenever Keyword
    	Object[] DealsDamage_Whenever_Parameters = new Object[3];
    	DealsDamage_Whenever_Parameters[0] = c.getController().getOpponent();
    	DealsDamage_Whenever_Parameters[2] = c;
    	AllZone.GameAction.checkWheneverKeyword(c, "DealsDamage/Opponent", DealsDamage_Whenever_Parameters);

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
					AllZone.Stack.add(ability);
			}
		}
		
		if (c.getKeyword().contains("Whenever CARDNAME deals combat damage to a player, proliferate."))
			AllZone.GameAction.proliferate(c, "0");
		
		if(CardFactoryUtil.hasNumberEquipments(c, "Mask of Riddles") > 0 && c.getNetAttack() > 0) {
			for(int k = 0; k < CardFactoryUtil.hasNumberEquipments(c, "Mask of Riddles"); k++) {
				playerCombatDamage_May_draw(c);
			}
		}
		if(CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike") > 0 && c.getNetAttack() > 0) {
			for(int k = 0; k < CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike"); k++) {
				playerCombatDamage_lose_halflife_up(c);
			}
		} 

		if(c.getName().equals("Hypnotic Specter")) playerCombatDamage_Hypnotic_Specter(c);
		else if(c.getName().equals("Dimir Cutpurse")) playerCombatDamage_Dimir_Cutpurse(c);
		else if(c.getName().equals("Ghastlord of Fugue")) playerCombatDamage_Ghastlord_of_Fugue(c);
		else if(c.getName().equals("Garza Zol, Plague Queen")) playerCombatDamage_May_draw(c);
		else if(c.getName().equals("Scalpelexis")) playerCombatDamage_Scalpelexis(c); 
		else if(c.getName().equals("Guul Draz Specter")
				|| c.getName().equals("Chilling Apparition") || c.getName().equals("Sedraxis Specter")) playerCombatDamage_Simple_Discard(c);
		else if((c.getName().equals("Headhunter") || c.getName().equals("Riptide Pilferer")) && !c.isFaceDown()) playerCombatDamage_Simple_Discard(c);
		else if(c.getName().equals("Shadowmage Infiltrator")) playerCombatDamage_Shadowmage_Infiltrator(c);
		else if(c.getName().equals("Augury Adept")) playerCombatDamage_Augury_Adept(c);
		else if(c.getName().equals("Spawnwrithe")) playerCombatDamage_Spawnwrithe(c);
		else if(c.getName().equals("Glint-Eye Nephilim") || c.getName().equals("Cold-Eyed Selkie")) playerCombatDamage_Glint_Eye_Nephilim(c);
		else if(c.getName().equals("Hystrodon") && !c.isFaceDown()) playerCombatDamage_Hystrodon(c);
		else if(c.getName().equals("Raven Guild Master") && !c.isFaceDown()) playerCombatDamage_Raven_Guild_Master(c);
		else if(c.getName().equals("Slith Strider") || c.getName().equals("Slith Ascendant")
				|| c.getName().equals("Slith Bloodletter") || c.getName().equals("Slith Firewalker")
				|| c.getName().equals("Slith Predator")) playerCombatDamage_Slith(c);
		else if (c.getName().equals("Arcbound Slith"))
			playerCombatDamage_Arcbound_Slith(c);
		else if(c.getName().equals("Oros, the Avenger")) playerCombatDamage_Oros(c);
		else if(c.getName().equals("Rootwater Thief")) playerCombatDamage_Rootwater_Thief(c);
		else if(c.getName().equals("Treva, the Renewer")) playerCombatDamage_Treva(c);
		else if(c.getName().equals("Rith, the Awakener")) playerCombatDamage_Rith(c);
		else if(c.getName().equals("Vorosh, the Hunter")) playerCombatDamage_Vorosh(c);

		if(c.getNetAttack() > 0) c.setDealtCombatDmgToOppThisTurn(true);

	}

	private static void playerCombatDamage_PoisonCounter(Card c, int n) {
		final Player opponent = c.getController().getOpponent();
		opponent.addPoisonCounters(n);
	}

	private static void playerCombatDamage_Oros(Card c) {
		SpellAbility[] sa = c.getSpellAbility();
		if(c.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[1]);
		else ComputerUtil.playNoStack(sa[1]);
	}

	private static void playerCombatDamage_Dimir_Cutpurse(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();

		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					opponent.discard(this);
					player.drawCard();
					
					//if(opponent.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
					//else AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this);

				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player).append(" draws a card, opponent discards a card");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
	}
	
	private static void playerDamage_Dissipation_Field(final Card c, final Card crd)
	{
		final Player owner = c.getOwner();
		
		Ability ability = new Ability(crd,"0")
		{
			public void resolve() {
				if (AllZone.GameAction.isCardInPlay(c)) {
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, owner);
					AllZone.GameAction.moveTo(hand, c);
				}
			}	
		};// Ability
		
		StringBuilder sb = new StringBuilder();
		sb.append("Dissipation Field - returns ").append(c).append(" back to owner's hand.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}
	
	private static void playerDamage_No_Mercy(final Card c, final Card crd)
	{		
		Ability ability = new Ability(crd,"0")
		{
			public void resolve() {
				if (AllZone.GameAction.isCardInPlay(c))
				{
					AllZone.GameAction.destroy(c);
				}
			}	
		};// Ability
		
		StringBuilder sb = new StringBuilder();
		sb.append(crd).append(" - destroys ").append(c).append(".");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
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
		AllZone.Stack.add(ability);
	}

	private static void playerCombatDamage_Ghastlord_of_Fugue(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();

		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					Card choice = null;

					//check for no cards in hand on resolve
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
					PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, opponent);
					Card[] handChoices = removeLand(hand.getCards());

					if(handChoices.length == 0) return;

					//human chooses
					if(opponent.equals(AllZone.ComputerPlayer)) {
						choice = AllZone.Display.getChoice("Choose", handChoices);
					} else//computer chooses
					{
						choice = CardUtil.getRandom(handChoices); // wise choice should be here
					}

					hand.remove(choice);
					exiled.add(choice);
				}//resolve()

				@Override
				public boolean canPlayAI() {
					Card[] c = removeLand(AllZone.Human_Hand.getCards());
					return 0 < c.length;
				}

				Card[] removeLand(Card[] in) {
					return in;
				}//removeLand() 
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append("opponent discards a card.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
	} //Ghastlord of Fugue


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

	private static void playerCombatDamage_Vorosh(Card c) {
		SpellAbility[] sa = c.getSpellAbility();
		if(c.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[1]);
		else ComputerUtil.playNoStack(sa[1]);
	}

	private static void playerCombatDamage_Slith(Card c) {
		final int power = c.getNetAttack();
		final Card card = c;

		if(power > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					card.addCounter(Counters.P1P1, 1);
				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - gets a +1/+1 counter.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		} // if
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
					AllZone.Stack.add(ability2);
				}
			};
			AllZone.EndOfTurn.addAt(dealtDmg);

		} // if
	}

	private static void playerCombatDamage_Arcbound_Slith(Card c) {
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

			AllZone.Stack.add(ability2);

		} // if
	}

	private static void playerCombatDamage_Raven_Guild_Master(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();

		if(c.getNetAttack() > 0) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opponent);
					PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, opponent);
					CardList libList = new CardList(lib.getCards());

					int max = 10;
					if(libList.size() < 10) max = libList.size();

					for(int i = 0; i < max; i++) {
						Card c = libList.get(i);
						lib.remove(c);
						exiled.add(c);
					}
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Raven Guild Master - ").append(opponent);
			sb.append(" removes the top ten cards of his or her library from the game");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

	private static void playerCombatDamage_May_draw(Card c) {
		final Player player = c.getController();

		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					player.mayDrawCard();
				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player).append(" may draw a card.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
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
			
			AllZone.Stack.add(ability2);
		}
	}


	private static void playerCombatDamage_Simple_Discard(Card c) {
		final Player player = c.getController();
		final Player opponent = player.getOpponent();

		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					opponent.discard(this);
					//if(opponent.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
					//else AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this); // Should be changed to wise discard  

				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append("opponent discards a card.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
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
					PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, opponent);
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
						lib.remove(c);
						exiled.add(c);
					}
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Scalpelexis - ").append(opponent);
			sb.append(" removes the top four cards of his or her library from the game. ");
			sb.append("If two or more of those cards have the same name, repeat this process.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

    private static void playerCombatDamage_Hystrodon(final Card c) {
        final Player player = c.getController();
        final int power = c.getNetAttack();

        if (power > 0) {
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    player.mayDrawCard();
                }//resolve()
            };// ability2
            
            StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - ").append(player).append(" may draw a card.");
            ability2.setStackDescription(sb.toString());
            AllZone.Stack.add(ability2);
        }// if
    }//playerCombatDamage_Hystrodon()

	private static void playerCombatDamage_Glint_Eye_Nephilim(Card c) {
		final Player player = c.getController();
		final int power = c.getNetAttack();

		if(power > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					player.drawCards(power);
				}
			};// ability2
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player);
			sb.append(" draws ").append(power).append(" card(s).");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		} // if

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
					c.setText("Whenever Spawnwrithe deals combat damage to a player, put a token into play that's a copy of Spawnwrithe.");
					c.setCopiedToken(true);
				}
			}
		};// ability2
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - ").append(player).append(" puts copy into play.");
		ability2.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability2);
	}

	private static void playerCombatDamage_Goblin_Lackey(Card c) {
		if(c.getNetAttack() > 0) {
			final Card card = c;
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

					CardList goblins = new CardList(hand.getCards());
					//goblins = goblins.getType("Goblin");
					goblins = goblins.filter(new CardListFilter() {

						public boolean addCard(Card c) {
							return (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling"))
							&& c.isPermanent();
						}

					});

					if(goblins.size() > 0) {
						if(card.getController().equals(AllZone.HumanPlayer)) {
							Object o = AllZone.Display.getChoiceOptional("Select a Goblin to put into play",
									goblins.toArray());

							if(o != null) {
								Card gob = (Card) o;
								hand.remove(gob);
								play.add(gob);
							}
						} else {
							Card gob = goblins.get(0);
							hand.remove(gob);
							play.add(gob);
						}
					}
				}
			};
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(c.getController());
			sb.append(" puts a goblin into play from his or her hand.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
	}

	private static void playerCombatDamage_Warren_Instigator(Card c) {
		if(c.getNetAttack() > 0) {
			final Card card = c;
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

					CardList goblins = new CardList(hand.getCards());
					//goblins = goblins.getType("Goblin");
					goblins = goblins.filter(new CardListFilter() {

						public boolean addCard(Card c) {
							return (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling"))
							&& c.isCreature();
						}

					});

					if(goblins.size() > 0) {
						if(card.getController().equals(AllZone.HumanPlayer)) {
							Object o = AllZone.Display.getChoiceOptional("Select a Goblin to put into play",
									goblins.toArray());

							if(o != null) {
								Card gob = (Card) o;
								hand.remove(gob);
								play.add(gob);
							}
						} else {
							Card gob = goblins.get(0);
							hand.remove(gob);
							play.add(gob);
						}
					}
				}
			};
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(c.getController());
			sb.append(" puts a goblin into play from his or her hand.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
	}//warren instigator

	private static void playerCombatDamage_Nicol_Bolas(Card c) {
		final Player[] opp = new Player[1];
		final Card crd = c;

		if(c.getNetAttack() > 0) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					opp[0] = crd.getController().getOpponent();
					//AllZone.GameAction.discardHand(opp[0], this);
					opp[0].discardHand(this);
				}
			};
			opp[0] = c.getController().getOpponent();
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(opp[0]).append(" discards his or her hand.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}//nicol bolas

	private static void playerCombatDamage_Shadowmage_Infiltrator(Card c) {
		//Player player = c.getController();
		final Player[] player = new Player[1];
		final Card crd = c;


		if(c.getNetAttack() > 0) {
			Ability ability2 = new Ability(c, "0") {
				@Override
				public void resolve() {
					player[0] = crd.getController();
					player[0].drawCard();
				}
			};// ability2

			player[0] = c.getController();
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player[0]).append(" draws a card.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
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
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player[0]);
					if(lib.size() > 0) {
						CardList cl = new CardList();
						cl.add(lib.get(0));
						AllZone.Display.getChoiceOptional("Top card", cl.toArray());
					};
					Card top = lib.get(0);
					player[0].gainLife(CardUtil.getConvertedManaCost(top.getManaCost()), crd);
					hand.add(top);
					lib.remove(top);

				}
			};// ability2

			player[0] = c.getController();
			
			StringBuilder sb = new StringBuilder();
			sb.append(c.getName()).append(" - ").append(player[0]);
			sb.append(" reveals the top card of his library and put that card into his hand. ");
			sb.append("He gain life equal to its converted mana cost.");
			ability2.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability2);
		}
	}

	private static void playerCombatDamage_Hypnotic_Specter(Card c) {
		final Player[] player = new Player[1];
		player[0] = c.getController();
		final Player[] opponent = new Player[1];

		if(c.getNetAttack() > 0) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {

					opponent[0] = player[0].getOpponent();
					opponent[0].discardRandom(this);
				}
			};// ability

			opponent[0] = player[0].getOpponent();
			
			StringBuilder sb = new StringBuilder();
			sb.append("Hypnotic Specter - ").append(opponent[0]).append(" discards a card at random");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}
	
	private static void playerCombatDamage_Silent_Specter(Card c) {
		final Player[] player = new Player[1];
		player[0] = c.getController();
		final Player[] opponent = new Player[1];

		if(c.getNetAttack() > 0 && !c.isFaceDown()) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
					
                    PlayerZone Ohand = AllZone.getZone(Constant.Zone.Hand, player[0].getOpponent());
                    Card h[] = Ohand.getCards();
                    Card[] handChoices = Ohand.getCards();
                    int Handsize = 1;
                    if(h.length <= 1) Handsize = h.length;
                    Player opponent = player[0].getOpponent();
                    Card choice = null; 

                    int j=0;
                    
                    while (j<2) {

                    for(int i = 0; i < Handsize; i++) {
                            AllZone.Display.showMessage("Select two cards to discard ");
                            ButtonUtil.enableOnlyCancel();
                        handChoices = Ohand.getCards();
                        //human chooses
                        if(opponent.equals(AllZone.HumanPlayer)) {
                            choice = AllZone.Display.getChoice("Choose", handChoices);
                        } else//computer chooses
                        {
                            choice = CardUtil.getRandom(handChoices);
                        }
                        
                        choice.getController().discard(choice, this);
                    }
                    
                    j++;
                    
                    }
                    
				}
			};// ability
			opponent[0] = player[0].getOpponent();
			
			StringBuilder sb = new StringBuilder();
			sb.append("Silent Specter - ").append(opponent[0]).append(" discards two cards");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}
	

	private static void playerCombatDamage_Abyssal_Specter(Card c) {
		final Player[] player = new Player[1];
		player[0] = c.getController();
		final Player[] opponent = new Player[1];

		if(c.getNetAttack() > 0) {
			Ability ability = new Ability(c, "0") {
				@Override
				public void resolve() {
                    PlayerZone Ohand = AllZone.getZone(Constant.Zone.Hand, player[0].getOpponent());
                    Card h[] = Ohand.getCards();
                    Card[] handChoices = Ohand.getCards();
                    int Handsize = 1;
                    if(h.length <= 1) Handsize = h.length;
                    Player opponent = player[0].getOpponent();
                    Card choice = null; 

                    for(int i = 0; i < Handsize; i++) {
                            AllZone.Display.showMessage("Select a card to discard ");
                            ButtonUtil.enableOnlyCancel();
                        handChoices = Ohand.getCards();
                        //human chooses
                        if(opponent.equals(AllZone.HumanPlayer)) {
                            choice = AllZone.Display.getChoice("Choose", handChoices);
                        } else//computer chooses
                        {
                            choice = CardUtil.getRandom(handChoices);
                        }
                        
                        choice.getController().discard(choice, this);
                    }
				}
			};// ability

			opponent[0] = player[0].getOpponent();
			
			StringBuilder sb = new StringBuilder();
			sb.append("Abyssal Specter - ").append(opponent[0]).append(" discards a card");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}
	
	@SuppressWarnings("unused")
	// upkeep_CheckEmptyDeck_Lose
	private static void upkeep_CheckEmptyDeck_Lose() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone libraryZone = AllZone.getZone(Constant.Zone.Library, player);

		System.out.println("libraryZone.size: " + libraryZone.size() + " phase: " + AllZone.Phase.getPhase()
				+ "Turn: " + AllZone.Phase.getTurn());
		if(libraryZone.size() == 0 && AllZone.Phase.getPhase().equals(Constant.Phase.Untap)
				&& AllZone.Phase.getTurn() > 1) {
			//PlayerLife life = AllZone.GameAction.getPlayerLife(player);
			player.setLife(0, null);
			// TODO display this somehow!!!
		}// if
	}// upkeep_CheckEmptyDeck_Lose

	private static void upkeep_AI_Aluren() {
		PlayerZone AIHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
		PlayerZone AIPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);

		CardList list = AllZoneUtil.getCardsInPlay("Aluren");

		CardList creatures = new CardList();

		for(int i = 0; i < AIHand.size(); i++) {
			if(AIHand.get(i).getType().contains("Creature")
					&& CardUtil.getConvertedManaCost(AIHand.get(i).getManaCost()) <= 3) creatures.add(AIHand.get(i));
		}

		if(list.size() > 0 && creatures.size() > 0) {
			for(int i = 0; i < creatures.size(); i++) {
				Card c = creatures.getCard(i);
				AIHand.remove(c);
				AIPlay.add(c);
				c.setSickness(true);

			}
		}

	}

	private static void upkeep_Land_Tax() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Land Tax");

		PlayerZone oppPlayZone = AllZone.getZone(Constant.Zone.Play, player.getOpponent());

		CardList self = new CardList(playZone.getCards());
		CardList opp = new CardList(oppPlayZone.getCards());

		self = self.getType("Land");
		opp = opp.getType("Land");

		if(self.size() < opp.size()) {

			for(int i = 0; i < list.size(); i++) {
				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

						CardList lands = new CardList(lib.getCards());
						lands = lands.getType("Basic");

						if(player.equals(AllZone.HumanPlayer) && lands.size() > 0) {
							String[] choices = {"Yes", "No"};
							Object choice = AllZone.Display.getChoice("Use Land Tax?", choices);
							if(choice.equals("Yes")) {
								Object o = AllZone.Display.getChoiceOptional(
										"Pick a basic land card to put into your hand", lands.toArray());
								if(o != null) {
									Card card = (Card) o;
									lib.remove(card);
									hand.add(card);
									lands.remove(card);

									if(lands.size() > 0) {
										o = AllZone.Display.getChoiceOptional(
												"Pick a basic land card to put into your hand", lands.toArray());
										if(o != null) {
											card = (Card) o;
											lib.remove(card);
											hand.add(card);
											lands.remove(card);
											if(lands.size() > 0) {
												o = AllZone.Display.getChoiceOptional(
														"Pick a basic land card to put into your hand",
														lands.toArray());
												if(o != null) {
													card = (Card) o;
													lib.remove(card);
													hand.add(card);
													lands.remove(card);
												}
											}
										}

									}
								}
								AllZone.HumanPlayer.shuffle();
							}// if choice yes
						} // player equals human
						else if(player.equals(AllZone.ComputerPlayer) && lands.size() > 0) {
							Card card = lands.get(0);
							lib.remove(card);
							hand.add(card);
							lands.remove(card);

							if(lands.size() > 0) {
								card = lands.get(0);
								lib.remove(card);
								hand.add(card);
								lands.remove(card);

								if(lands.size() > 0) {
									card = lands.get(0);
									lib.remove(card);
									hand.add(card);
									lands.remove(card);
								}
							}
							AllZone.ComputerPlayer.shuffle();
						}
					}

				};// Ability
				ability.setStackDescription("Land Tax - search library for up to three basic land cards and put them into your hand");
				AllZone.Stack.add(ability);

			}// for
		}// if fewer lands than opponent

	}
	
	private static void upkeep_Mana_Vault() {
		//this card is filtered out for the computer, so we will only worry about Human here
		final Player player = AllZone.Phase.getPlayerTurn();
		
		if(!player.equals(AllZone.HumanPlayer)) return; // AI doesn't try to untap
		
		CardList vaults = AllZoneUtil.getPlayerCardsInPlay(player, "Mana Vault");
		for(Card vault:vaults) {
			if(vault.isTapped()) {
				final Card thisVault = vault;
				
				Ability vaultChoice = new Ability(thisVault, "0"){
				
					@Override
					public void resolve(){
						final String[] choices = {"Yes", "No"};
						Object o = AllZone.Display.getChoice("Untap Mana Vault?", choices);
						String choice = (String) o;
						if(choice.equals("Yes")) {
							//prompt for pay mana cost, then untap
							final SpellAbility untap = new Ability(thisVault, "4") {
								@Override
								public void resolve() {
									thisVault.untap();
								}
							};//Ability
							
							StringBuilder sb = new StringBuilder();
							sb.append("Untap ").append(thisVault);
							untap.setStackDescription(sb.toString());
							
							AllZone.GameAction.playSpellAbility(untap);
						}
					}
				};
				vaultChoice.setStackDescription("Mana Vault - Untap during Upkeep?");
				AllZone.Stack.add(vaultChoice);
			}
		}
	}
	
	private static void upkeep_Feedback() {
		final String auraName = "Feedback";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isEnchantment() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability;
        	for(Card target:list) {
        		if(target.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(target.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card source = aura;
        				ability = new Ability(aura, "0") {
        					@Override
        					public void resolve() {
        						player.addDamage(1, source);
        					}
        				};
        				
        				StringBuilder sb = new StringBuilder();
        				sb.append(auraName).append(" -  deals 1 damage to ").append(player);
        				ability.setStackDescription(sb.toString());
        				
                        AllZone.Stack.add(ability);
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Feedback()
	
	private static void upkeep_Unstable_Mutation() {
		final String auraName = "Unstable Mutation";
        final Player player = AllZone.Phase.getPlayerTurn();
        
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isCreature() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability;
        	for(final Card creature:list) {
        		if(creature.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(creature.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card source = aura;
        				ability = new Ability(source, "0") {
        					@Override
        					public void resolve() {
        						creature.addCounter(Counters.M1M1, 1);
        					}
        				};
        				
        				StringBuilder sb = new StringBuilder();
        				sb.append(source.getName()).append(" -  put a -1/-1 counter on ").append(creature.getName());
        				ability.setStackDescription(sb.toString());
        				
                        AllZone.Stack.add(ability);
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Unstable_Mutation()
	
	private static void upkeep_Warp_Artifact() {
		final String auraName = "Warp Artifact";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isArtifact() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability;
        	for(Card target:list) {
        		if(target.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(target.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card source = aura;
        				ability = new Ability(aura, "0") {
        					@Override
        					public void resolve() {
        						player.addDamage(1, source);
        					}
        				};
        				
        				StringBuilder sb = new StringBuilder();
        				sb.append(auraName).append(" -  deals 1 damage to ").append(player);
        				ability.setStackDescription(sb.toString());
        				
                        AllZone.Stack.add(ability);
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Warp_Artifact()
	
	private static void upkeep_Soul_Bleed() {
		final String auraName = "Soul Bleed";
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.filter(AllZoneUtil.enchanted);

		if(list.size() > 0) {
			Ability ability;
			for(Card target:list) {
				if(target.isEnchantedBy(auraName)) {
					CardList auras = new CardList(target.getEnchantedBy().toArray());
					auras = auras.getName(auraName);
					for(Card aura:auras) {
						final Card source = aura;
						ability = new Ability(aura, "0") {
							@Override
							public void resolve() {
								player.loseLife(1, source);
							}
						};
						
						StringBuilder sb = new StringBuilder();
						sb.append(auraName).append(" - ").append(player).append(" loses 1 life.");
						ability.setStackDescription(sb.toString());
						
						AllZone.Stack.add(ability);
					} 
				}
			}
		}//list > 0
	}//upkeep_Soul_Bleed()
	
	private static void upkeep_Wanderlust() {
		final String auraName = "Wanderlust";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isCreature() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability;
        	for(Card target:list) {
        		if(target.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(target.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card source = aura;
        				ability = new Ability(aura, "0") {
        					@Override
        					public void resolve() {
        						player.addDamage(1, source);
        					}
        				};
        				
        				StringBuilder sb = new StringBuilder();
        				sb.append(auraName).append(" -  deals 1 damage to ").append(player);
        				ability.setStackDescription(sb.toString());
        				
                        AllZone.Stack.add(ability);
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Wanderlust()
	
	private static void upkeep_Curse_of_Chains() {
		final String auraName = "Curse of Chains";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isCreature() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability;
        	for(Card target:list) {
        		if(target.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(target.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card enchantedCard = target;
        				ability = new Ability(aura, "0") {
        					@Override
        					public void resolve() {
        						enchantedCard.tap();
        					}
        				};
        				if(enchantedCard.isUntapped()) {
        					StringBuilder sb = new StringBuilder();
        					sb.append(auraName).append(" -  tap enchanted creature.");
        					ability.setStackDescription(sb.toString());
        					
        					AllZone.Stack.add(ability);
        				}
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Curse_of_Chains()
	
	private static void upkeep_Festering_Wound_Counter() {
		final String auraName = "Festering Wound";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.getName(auraName);
        
        if(list.size() > 0) {
        	Ability ability2;
        	for(Card target:list) {
        		final Card source = list.get(0);
        		final Card enchantedCard = target;
        		ability2 = new Ability(source, "0") {
        			@Override
        			public void resolve() {
        				source.addCounter(Counters.INFECTION, 1);
        			}
        		};
        		
        		StringBuilder sb = new StringBuilder();
        		sb.append(auraName).append(" - add an infection counter to ");
        		sb.append(enchantedCard.getName());
        		ability2.setStackDescription(sb.toString());
        		
        		AllZone.Stack.add(ability2);
        	}
        }//list > 0
	}
	
	private static void upkeep_Festering_Wound_Damage() {
		final String auraName = "Festering Wound";
        final Player player = AllZone.Phase.getPlayerTurn();
        PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
        
        CardList list = new CardList(playZone.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isCreature() && c.isEnchanted();
            }
        });
        
        if(list.size() > 0) {
        	Ability ability1;
        	for(Card target:list) {
        		if(target.isEnchantedBy(auraName)) {
        			CardList auras = new CardList(target.getEnchantedBy().toArray());
        			auras = auras.getName(auraName);
        			for(Card aura:auras) {
        				final Card source = aura;
        				final Card enchantedCard = target;
        				ability1 = new Ability(aura, "0") {
        					@Override
        					public void resolve() {
        						int damage = source.getCounters(Counters.INFECTION);
        						enchantedCard.getController().addDamage(damage, source);
        					}
        				};
        				
        				StringBuilder sb = new StringBuilder();
        				sb.append(auraName).append(" - deals X damage to ").append(target.getController());
        				ability1.setStackDescription(sb.toString());
        				
        				AllZone.Stack.add(ability1);
        			} 
        		}
        	}
        }//list > 0
    }//upkeep_Festering_Wound_Damage()

	private static void upkeep_Squee() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

		CardList list = new CardList(graveyard.getCards());
		list = list.getName("Squee, Goblin Nabob");

		final CardList squees = list;
		final int[] index = new int[1];
		index[0] = 0;

		for(int i = 0; i < list.size(); i++) {
			Ability ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

					Card c = squees.get(index[0]);
                    if(AllZone.GameAction.isCardInZone(c, graveyard)) {
						if(player.equals(AllZone.HumanPlayer)) {
							String[] choices = {"Yes", "No"};
							Object o = AllZone.Display.getChoiceOptional(
									"Return Squee from your graveyard to your hand?", choices);
							if(!o.equals("Yes")) {
								index[0] = index[0] + 1;
								return;
							}
						}
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getController());
                        AllZone.GameAction.moveTo(hand, c);

						index[0] = index[0] + 1;
                    }
				}

			};// Ability
			ability.setStackDescription("Squee gets returned from graveyard to hand.");
			AllZone.Stack.add(ability);
		} // if creatures > 0

	}

	/*
	private static void upkeep_AEther_Vial() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("AEther Vial");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				final Card thisCard = list.get(i);
				Ability ability = new Ability(list.get(i), "") {
					@Override
					public void resolve() {
						// TODO Auto-generated method stub
						String[] choices = {"Yes", "No"};

						Object q = null;
						if(player.equals(AllZone.HumanPlayer)) {
							q = AllZone.Display.getChoiceOptional("Put a counter on AEther Vial? ("
									+ thisCard.getCounters(Counters.CHARGE) + ")", choices);
							if(q == null || q.equals("No")) return;
							if(q.equals("Yes")) {

								thisCard.addCounter(Counters.CHARGE, 1);
							}
						} else if(player.equals(AllZone.ComputerPlayer)) {

							thisCard.addCounter(Counters.CHARGE, 1);
						}

					}

				};
				ability.setStackDescription(list.get(i).getName() + " ("
						+ list.get(i).getCounters(Counters.CHARGE)
						+ " counters) - Put a charge counter on AEther Vial?");
				AllZone.Stack.add(ability);
			}//for
		}
	}
	*/

	private static void upkeep_Dragonmaster_Outcast() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Dragonmaster Outcast");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				CardList lands = new CardList(playZone.getCards());
				lands = lands.getType("Land");

				if(lands.size() >= 6) {
					final Card c = list.get(i);
					Ability ability = new Ability(list.get(i), "0") {
						@Override
						public void resolve() {
							CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", c.getController(), "R", new String[] {
									"Creature", "Dragon"}, 5, 5, new String[] {"Flying"});
						}

					};// Ability
					ability.setStackDescription("Dragonmaster Outcast - put a 5/5 red Dragon creature token with flying onto the battlefield.");
					AllZone.Stack.add(ability);
				}
			} // for
		} // if creatures > 0
	};
	
	/*
	 * At the beginning of your upkeep, put a 2/2 white Cat creature token
	 * onto the battlefield for each Equipment attached to Kemba, Kha Regent.
	 */
	private static void upkeep_Kemba_Kha_Regent() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Kemba, Kha Regent");
		
		for(Card src:list) {
			final Card regent = src;
			final int equipNum = regent.getEquippedBy().size();
			Ability ability = new Ability(regent, "0") {
				@Override
				public void resolve() {
					for(int i = 0; i < equipNum; i++) {
						CardFactoryUtil.makeToken("Cat", "W 2 2 Cat", regent.getController(), "W",
								new String[] {"Creature", "Cat"}, 2, 2, new String[] {});
					}
				}

			};// Ability
			ability.setStackDescription(regent.getName()+" - put "+equipNum+" 2/2 white Cat creature token(s) onto the battlefield.");
			if(equipNum > 0) AllZone.Stack.add(ability);
		}
	};

	private static void upkeep_Scute_Mob() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Scute Mob");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				CardList lands = new CardList(playZone.getCards());
				lands = lands.getType("Land");

				if(lands.size() >= 5) {
					final Card c = list.get(i);
					Ability ability = new Ability(list.get(i), "0") {
						@Override
						public void resolve() {
							c.addCounter(Counters.P1P1, 4);
						}

					};// Ability
					ability.setStackDescription("Scute Mob - put four +1/+1 counters on Scute Mob.");
					AllZone.Stack.add(ability);
				}
			} // for
		} // if creatures > 0
	}//Scute Mob


	private static void upkeep_Sporesower_Thallid() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Sporesower Thallid");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {

				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

						CardList fungi = new CardList(play.getCards());
						fungi = fungi.getType("Fungus");

						for(int j = 0; j < fungi.size(); j++) {
							Card c = fungi.get(j);
							c.addCounter(Counters.SPORE, 1);
						}
					}
				};// Ability
				ability.setStackDescription("Sporesower - put a spore counter on each fungus you control.");
				AllZone.Stack.add(ability);
			} // for
		} // if creatures > 0
	}

	private static void upkeep_Lichenthrope() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.filter(new CardListFilter()
		{
			public boolean addCard(Card c)
			{
				return c.getName().equals("Lichenthrope") && c.getCounters(Counters.M1M1) > 0;
			}
		});

		final CardList cl = list;

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {

				final int j = i;
				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						Card c = cl.get(j);
						c.subtractCounter(Counters.M1M1, 1);
					}

				};// Ability
				ability.setStackDescription("Lichenthrope - Remove a -1/-1 counter.");
				AllZone.Stack.add(ability);
			} // for
		} // if creatures > 0
	}//Lichenthrope


	private static void upkeep_Heartmender() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Heartmender");

		if(list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {

				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

						CardList creats = new CardList(play.getCards());
						creats = creats.filter(new CardListFilter() {

							public boolean addCard(Card c) {
								return c.getCounters(Counters.M1M1) > 0;
							}

						});

						for(int j = 0; j < creats.size(); j++) {
							Card c = creats.get(j);
							if(c.getCounters(Counters.M1M1) > 0) c.addCounter(Counters.M1M1, -1);
						}

					}

				};// Ability
				ability.setStackDescription("Heartmender - Remove a -1/-1 counter from each creature you control.");
				AllZone.Stack.add(ability);
			} // for
		} // if creatures > 0
	}//heartmender

	private static void upkeep_Ratcatcher() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);

		CardList creatures = new CardList(library.getCards());
		creatures = creatures.getType("Rat");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Ratcatcher");

		if(creatures.size() > 0 && list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {

				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

						CardList rats = new CardList(lib.getCards());
						rats = rats.getType("Rat");

						if(rats.size() > 0) {
							if(player.equals(AllZone.HumanPlayer)) {
								Object o = AllZone.Display.getChoiceOptional("Pick a Rat to put into your hand",
										rats.toArray());
								if(o != null) {
									Card card = (Card) o;
									lib.remove(card);
									hand.add(card);
								}
							} else if(player.equals(AllZone.ComputerPlayer)) {
								Card card = rats.get(0);
								lib.remove(card);
								hand.add(card);

							}
							player.shuffle();
						}
					}

				};// Ability
				ability.setStackDescription("Ratcatcher - search library for a rat and put into your hand");
				AllZone.Stack.add(ability);
			} // for
		} // if creatures > 0
	}

	private static void upkeep_Nath() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();

		PlayerZone zone1 = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(zone1.getCards());
		list = list.getName("Nath of the Gilt-Leaf");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {

			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					opponent.discardRandom(this);
				}
			}; // ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Nath of the Gilt-Leaf - ").append(opponent).append(" discards a card at random.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

	private static void upkeep_Anowon() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Anowon, the Ruin Sage");

		if(list.size() > 0) {
			Ability ability = new Ability(list.get(0), "0") {
				@Override
				public void resolve() {
					PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
					PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
					CardList choices = new CardList(hPlay.getCards());

					CardListFilter filter = new CardListFilter() {
						public boolean addCard(Card c) {
							return (c.isCreature() && !c.isType("Vampire"));
						}
					};

					choices = choices.filter(filter);
					if(choices.size() > 0) AllZone.HumanPlayer.sacrificeCreature(choices);

					CardList compCreats = new CardList(cPlay.getCards());
					compCreats = compCreats.filter(filter);

					if(compCreats.size() > 0) AllZone.ComputerPlayer.sacrificeCreature(compCreats);
				}
			};
			ability.setStackDescription("At the beginning of your upkeep, each player sacrifices a non-Vampire creature.");
			AllZone.Stack.add(ability);
		}
	}

	private static void upkeep_Cunning_Lethemancer() {
		final Player player = AllZone.Phase.getPlayerTurn();

		PlayerZone zone1 = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(zone1.getCards());
		list = list.getName("Cunning Lethemancer");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {

			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					AllZone.ComputerPlayer.discard(this);
					AllZone.HumanPlayer.discard(this);
					/*PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);

					CardList cardsInHand = new CardList(hand.getCards());

					if(cardsInHand.size() > 0) {
						Object o = AllZone.Display.getChoiceOptional("Select Card to discard",
								cardsInHand.toArray());
						Card c = (Card) o;
						c.getController().discard(c, this);
					}

					AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this);
					*/

				}
			}; // ability
			ability.setStackDescription("Cunning Lethemancer - Everyone discards a card.");
			AllZone.Stack.add(ability);
		}
	}

	private static void upkeep_Benthic_Djinn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Benthic Djinn");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {

			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
                    crd.getController().loseLife(2, crd);    
				}// resolve()
			};// Ability

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Benthic_Djinn()   
	
	
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Ink Dissolver - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Kithkin Zephyrnaut - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                            library.remove(c);
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
                            library.remove(c);
                        }
                    }// wantToPlayCard
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Leaf-Crowned Elder - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Mudbutton Clanger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Nightshade Schemers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                    humanCreatures = humanCreatures.canBeDamagedBy(k);
                    humanCreatures = humanCreatures.getNotKeyword("Indestructible");
                    
                    CardList computerCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
                    computerCreatures = computerCreatures.getValidCards(smallCreatures,k.getController(),k);
                    computerCreatures = computerCreatures.canBeDamagedBy(k);
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
                            if (CardFactoryUtil.canDamage(k, crd))
                                crd.addDamage(2, k);
                        }
                    }
                }// resolve()
                
                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Pyroclast Consul - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Sensation Gorger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Squeaking Pie Grubfellows - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Wandering Graybeard - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Waterspout Weavers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Winnower Patrol - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
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
                            question.append(". Reveal card and put a 2/2 green Wolf creature token into play?");
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
                        AllZone.Display.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Wolf-Skull Shaman - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());
            AllZone.Stack.add(ability);
        }// for
    }// upkeep_Wolf_Skull_Shaman()
    
    
    ///////////////////////
    // End of Kinship cards
    ///////////////////////


	private static void upkeep_Dark_Confidant() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
					PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
					player.loseLife(convertedManaCost, F_card);

					// AllZone.GameAction.drawCard(player);
					// !!!can't just draw card, since it won't work with jpb's
					// fix!!!
					Card c = library.get(0);
					library.remove(c);
					hand.add(c);

				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Dark Confidant - ").append(player).append(" loses ").append(convertedManaCost);
			sb.append(" life and draws top card(").append(cardName).append(").");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Dark_Confidant()

	private static void upkeep_Debtors_Knell() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
		PlayerZone oppGrave = AllZone.getZone(Constant.Zone.Graveyard, player.getOpponent());

		CardList creatures = new CardList();
		creatures.addAll(grave.getCards());
		creatures.addAll(oppGrave.getCards());
		creatures = creatures.getType("Creature");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Debtors' Knell");

		if(creatures.size() > 0 && list.size() > 0) for(int i = 0; i < list.size(); i++) {
			{
				Ability ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
						PlayerZone oppGrave = AllZone.getZone(Constant.Zone.Graveyard,
								player.getOpponent());
						PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

						CardList creatures = new CardList();
						creatures.addAll(grave.getCards());
						creatures.addAll(oppGrave.getCards());

						creatures = creatures.getType("Creature");

						if(player.equals(AllZone.HumanPlayer)) {
							Object o = AllZone.Display.getChoiceOptional("Pick a creature to put into play",
									creatures.toArray());
							if(o != null) {
								Card card = (Card) o;
								PlayerZone graveyard = AllZone.getZone(card);
								graveyard.remove(card);
								card.setController(player);
								playZone.add(card);
							}
						} else if(player.equals(AllZone.ComputerPlayer)) {
							Card card = creatures.get(0);
							PlayerZone graveyard = AllZone.getZone(card);
							graveyard.remove(card);
							card.setController(player);
							playZone.add(card);

						}
					}
				};// Ability
				ability.setStackDescription("Debtors' Knell returns creature from graveyard to play");
				AllZone.Stack.add(ability);
			}//for
		} // if creatures > 0

	}

	private static void upkeep_Emeria() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

		CardList creatures = new CardList(graveyard.getCards());
		creatures = creatures.getType("Creature");

		CardList land = new CardList(playZone.getCards());
		land = land.getType("Plains");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Emeria, the Sky Ruin");

		if(land.size() >= 7 && creatures.size() >= 1) {
			for(int i = 0; i < list.size(); i++) {
				Ability ability = new Ability(list.get(0), "0") {
					@Override
					public void resolve() {
						PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
						PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

						CardList creatures = new CardList(graveyard.getCards());
						creatures = creatures.getType("Creature");

						if(player.equals(AllZone.HumanPlayer)) {
							Object o = AllZone.Display.getChoiceOptional("Pick a creature to put into play",
									creatures.toArray());
							if(o != null) {
								Card card = (Card) o;
								graveyard.remove(card);
								playZone.add(card);
							}
						} else if(player.equals(AllZone.ComputerPlayer)) {
							Card card = creatures.get(0);
							graveyard.remove(card);
							playZone.add(card);

						}
					}

				};// Ability
				ability.setStackDescription("Emeria, the Sky Ruin returns creature from graveyard to the battlefield.");
				AllZone.Stack.add(ability);

			}
		}
	}

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
						PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

						if(graveyardCreatures.size() >= 4) {
							if(player.equals(AllZone.HumanPlayer)) {
								Object o = AllZone.Display.getChoiceOptional("Pick a creature to return to hand",
										graveyardCreatures.toArray());
								if(o != null) {
									Card card = (Card) o;
									graveyard.remove(card);
									hand.add(card);
								}
							} 
							else if(player.equals(AllZone.ComputerPlayer)) {
								Card card = graveyardCreatures.get(0);
								graveyard.remove(card);
								hand.add(card);
							}
						}
					}
				};// Ability
				ability.setStackDescription("Oversold Cemetary returns creature from the graveyard to its owner's hand.");
				AllZone.Stack.add(ability);
			}
		}
	}//Oversold Cemetery

	private static void upkeep_Reya() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

		CardList creatures = new CardList(graveyard.getCards());
		creatures = creatures.getType("Creature");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Reya Dawnbringer");

		if(creatures.size() > 0 && list.size() > 0) {
			Ability ability = new Ability(list.get(0), "0") {
				@Override
				public void resolve() {
					PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
					PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

					CardList creatures = new CardList(graveyard.getCards());
					creatures = creatures.getType("Creature");

					if(player.equals(AllZone.HumanPlayer)) {
						Object o = AllZone.Display.getChoiceOptional("Pick a creature to put into play",
								creatures.toArray());
						if(o != null) {
							Card card = (Card) o;
							graveyard.remove(card);
							playZone.add(card);
						}
					} else if(player.equals(AllZone.ComputerPlayer)) {
						Card card = creatures.get(0);
						graveyard.remove(card);
						playZone.add(card);

					}
				}

			};// Ability
			ability.setStackDescription("Reya returns creature from graveyard back to play");
			AllZone.Stack.add(ability);
		} // if creatures > 0
	} // reya

	private static void upkeep_Wort() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

		CardList creatures = new CardList(graveyard.getCards());
		creatures = creatures.getType("Goblin");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Wort, Boggart Auntie");

		if(creatures.size() > 0 && list.size() > 0) {
			Ability ability = new Ability(list.get(0), "0") {
				@Override
				public void resolve() {
					PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
					PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

					CardList creatures = new CardList(graveyard.getCards());
					creatures = creatures.getType("Goblin");

					if(player.equals(AllZone.HumanPlayer)) {
						Object o = AllZone.Display.getChoiceOptional("Pick a goblin to put into your hand",
								creatures.toArray());
						if(o != null) {
							Card card = (Card) o;
							graveyard.remove(card);
							hand.add(card);
						}
					} else if(player.equals(AllZone.ComputerPlayer)) {
						Card card = creatures.get(0);
						graveyard.remove(card);
						hand.add(card);

					}
				}

			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Wort returns creature from graveyard to ").append(player).append("'s hand");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		} // if creatures > 0
	} // Wort

	private static void upkeep_Nether_Spirit() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList all = new CardList(graveyard.getCards());
		all = all.getType("Creature");

		CardList list = new CardList(graveyard.getCards());
		list = list.getName("Nether Spirit");

		if(all.size() == 1 && list.size() == 1) {
			final Card nether = list.get(0);
			Ability ability = new Ability(list.get(0), "0") {
				@Override
				public void resolve() {
					graveyard.remove(nether);
					playZone.add(nether);
				}
			};

			boolean returnNether = false;

			if(player.equals(AllZone.HumanPlayer)) {
				String[] choices = {"Yes", "No"};

				Object q = AllZone.Display.getChoiceOptional("Return Nether Spirit to play?", choices);
				if(q != null && q.equals("Yes")) returnNether = true;
			}

			if(player.equals(AllZone.ComputerPlayer) || returnNether) {
				ability.setStackDescription("Nether Spirit returns to play.");
				AllZone.Stack.add(ability);
			}
		} //if
	}//nether spirit

	private static void upkeep_Spore_Counters() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getType("Creature");

		for(int i = 0; i < list.size(); i++) {
			Card c = list.get(i);
			if(c.getName().equals("Deathspore Thallid") || c.getName().equals("Elvish Farmer")
					|| c.getName().equals("Feral Thallid") || c.getName().equals("Mycologist")
					|| c.getName().equals("Pallid Mycoderm") || c.getName().equals("Psychotrope Thallid")
					|| c.getName().equals("Savage Thallid") || c.getName().equals("Thallid")
					|| c.getName().equals("Thallid Devourer") || c.getName().equals("Thallid Germinator")
					|| c.getName().equals("Thallid Shell-Dweller") || c.getName().equals("Thorn Thallid")
					|| c.getName().equals("Utopia Mycon") || c.getName().equals("Vitaspore Thallid")) {
				c.addCounter(Counters.SPORE, 1);
			}
		}
	}

	public static void upkeep_Suspend() {
		Player player = AllZone.Phase.getPlayerTurn();

		PlayerZone exile = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
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
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
				
				AllZone.Stack.add(ability);
			}
		}
	}
	
	private static void upkeep_Fading() {

		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
				
				AllZone.Stack.add(ability);
			}
		}
	}

	private static void upkeep_Defense_of_the_Heart() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();

		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		// check if opponent has 3 or more creatures in play
		PlayerZone opponentZone = AllZone.getZone(Constant.Zone.Play, opponent);
		CardList opponentList = new CardList(opponentZone.getCards());
		opponentList = opponentList.getType("Creature");

		/*
        for (int i = 0; i < opponentList.size(); i++)
        {
        	Card tmpCard = opponentList.get(i);
        	System.out.println("opponent has: " + tmpCard);
        }
		 */

		if(3 > opponentList.size()) return;

		// opponent has more than 3 creatures in play, so check if Defense of
		// the Heart is in play and sacrifice it for the effect.
		CardList list = new CardList(playZone.getCards());
		list = list.getName("Defense of the Heart");

		if(0 < list.size()) {
			// loop through the number of Defense of the Heart's that player
			// controls. They could control 1, 2, 3, or 4 of them.
			for(int i = 0; i < list.size(); i++) {
				final Card card = list.get(i);
				Ability ability = new Ability(list.get(0), "0") {
					@Override
					public void resolve() {
						PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

						// sacrifice Defense of the Heart
						AllZone.GameAction.sacrifice(card);
						// search library for a creature, put it into play
						Card creature1 = getCreatureFromLibrary();
						if(creature1 != null) {
							library.remove(creature1);
							play.add(creature1);
						}

						// search library for a second creature, put it into
						// play
						Card creature2 = getCreatureFromLibrary();
						if(creature2 != null) {
							// if we got this far the effect was good
							library.remove(creature2);
							play.add(creature2);
						}

						card.getController().shuffle();

					}

					public Card getCreatureFromLibrary() {
						PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

						CardList creatureList = new CardList(library.getCards());
						creatureList = creatureList.getType("Creature");

						if(AllZone.ComputerPlayer.equals(card.getController())) {
							return CardFactoryUtil.AI_getBestCreature(creatureList);
						} else {
							Object o = AllZone.Display.getChoiceOptional("Choose a creature card",
									creatureList.toArray());
							if(o != null) {
								Card creature = (Card) o;
								return creature;
							} else {
								return null;
							}
						}
					}// getCreatureFromLibrary
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("Defense of the Heart - ").append(player);
				sb.append(" sacrifices Defense of the Heart to search their library for up to two ");
				sb.append("creature cards and put those creatures into play. Then shuffle's their library.");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
				card.addSpellAbility(ability);
			}
		}// if
	}// upkeep_Defense of the Heart
	
	private static void upkeep_Oath_of_Druids() {
		CardList oathList = AllZoneUtil.getCardsInPlay("Oath of Druids");
		if (oathList.isEmpty())
			return;
		
		final Player player = AllZone.Phase.getPlayerTurn();

		if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0){
			for(int i = 0; i < oathList.size(); i++) {
				Ability ability = new Ability(oathList.get(i), "0") {
	               @Override
	               public void resolve() {
	            	   //String opponent = player.getOpponent();
	            	   CardList libraryList = AllZoneUtil.getPlayerCardsInLibrary(player);
	            	   //PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
	            	   PlayerZone battlefield = AllZone.getZone(Constant.Zone.Play, player);
	            	   boolean oathFlag = true;

	            	   if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0){
		                      if(player == AllZone.HumanPlayer){
			                      String[] choices = {"Yes", "No"};
			                      Object q = null;
			                      q = AllZone.Display.getChoiceOptional("Use Oath of Druids?", choices);
			                      if(q == null || q.equals("No"))
			                    	  oathFlag = false;
		                      }
		                      else {	// if player == Computer
		                    	  CardList creaturesInLibrary = AllZoneUtil.getPlayerTypeInLibrary(player, "Creature");
		                    	  CardList creaturesInBattlefield = AllZoneUtil.getPlayerTypeInPlay(player, "Creature");

	                        	  // if there are at least 3 creatures in library, or none in play with one in library, oath
		                          if(creaturesInLibrary.size() > 2 || (creaturesInBattlefield.size() == 0 && creaturesInLibrary.size() > 0) )
		                        	  oathFlag = true;
		                          else
		                        	  oathFlag = false;
		                      }
		                      
		                      if (oathFlag){
			                        int max = libraryList.size();
			                        for(int i = 0; i < max; i++) {
			                            Card c = libraryList.get(i);
			                            if(c.getType().contains("Creature")) {
		                                    AllZone.GameAction.moveTo(battlefield, c);
		                                    break;   
			                            } 
			                            else{
			                            	AllZone.GameAction.moveToGraveyard(c);
			                            }
			                        }
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

	            AllZone.Stack.add(ability);
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
						PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

						if(AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
							if(player.equals(AllZone.HumanPlayer)) {
								Object o = AllZone.Display.getChoiceOptional("Pick a creature to return to hand",
										graveyardCreatures.toArray());
								if(o != null) {
									Card card = (Card) o;
									graveyard.remove(card);
									hand.add(card);
								}
							} 
							else if(player.equals(AllZone.ComputerPlayer)) {
								Card card = graveyardCreatures.get(0);
								graveyard.remove(card);
								hand.add(card);
							}
						}
					}
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
				sb.append("from their graveyard to owner's hand if they have more than an opponent.");
				ability.setStackDescription(sb.toString());

				AllZone.Stack.add(ability);
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
					
					AllZone.Stack.add(ability);
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
					
					AllZone.Stack.add(ability);
				}
			}
		}// if
	}// upkeep_Karma()
	
	private static void upkeep_Convalescence() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		//final PlayerLife pLife = AllZone.GameAction.getPlayerLife(player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Convalescence");

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
				AllZone.Stack.add(ability);
			}
		}// for
	}// upkeep_Convalescence()

	private static void upkeep_Convalescent_Care() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
            	AllZone.Stack.add(ability);
            }

		}// for
	}// upkeep_Convalescence()

	private static void upkeep_Ivory_Tower() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Ivory Tower");

		for(Card tower:list) {
			final Card source = tower;
			final Ability ability = new Ability(tower, "0") {
				public void resolve() {
					int numCards = AllZoneUtil.getPlayerHand(player).size();
					if( numCards > 4 ) {
						player.gainLife(numCards - 4, source);
					}
				}
			};//Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Ivory Tower - " ).append(player).append(" gains 1 life for each card > 4");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}//for
	}//upkeep_Ivory Tower()
	
	/*
	 * At the beginning of your upkeep, if there are four or more creatures
	 * on the battlefield, sacrifice Planar Collapse and destroy all creatures.
	 * They can't be regenerated.
	 * 
	 * Ruling: This ability does not trigger at all if there are not 4 or more
	 * creatures on the battlefield. It also checks this at the start of
	 * resolution and does nothing if this is not still true. 
	 */
	private static void upkeep_Planar_Collapse() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList planars = AllZoneUtil.getPlayerCardsInPlay(player, "Planar Collapse");

		for(Card c:planars) {
			final Card source = c;
			int num = AllZoneUtil.getCreaturesInPlay().size();
			final Ability ability = new Ability(source, "0") {
				public void resolve() {
					CardList creatures = AllZoneUtil.getCreaturesInPlay();
					if( creatures.size() >= 4 ) {
						for(Card creature:creatures) {
							AllZone.GameAction.destroyNoRegeneration(creature);
						}
						AllZone.GameAction.sacrifice(source);
					}
				} 
			};//Ability

			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - ").append("destroy all creatures.  They can't be regenerated.");
			ability.setStackDescription(sb.toString());
			if(num >= 4) {
				AllZone.Stack.add(ability);
			}
		}//for
	}//upkeep_Planar_Collapse()
	
	private static void upkeep_Vensers_Journal() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Venser's Journal");

		for(Card journal:list) {
			final Card source = journal;
			final Ability ability = new Ability(source, "0") {
				public void resolve() {
					CardList hand = AllZoneUtil.getPlayerHand(player);
					player.gainLife(hand.size(), source);
				}
			};//Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source.getName()).append(" - ").append(player);
			sb.append(" gains 1 life for each card in your hand.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}//for
	}//upkeep_Vensers_Journal()

	private static void upkeep_The_Rack() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList racks = AllZoneUtil.getPlayerCardsInPlay(player.getOpponent(), "The Rack");
		racks.add(AllZoneUtil.getPlayerCardsInPlay(player.getOpponent(), "Wheel of Torture"));

		// if there are 1 or more The Racks owned by the opponent of the
		// current player have each of them deal damage.
		for(Card rack:racks) {
			final Card src = rack;
			Ability ability = new Ability(src, "0") {
				@Override
				public void resolve() {
					int playerHandSize = AllZone.getZone(Constant.Zone.Hand, player).size();
					int damage = 3 - playerHandSize;
					if (damage < 1) 
						return;
					player.addDamage(damage, src);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(src.getName());
			sb.append(" - deals X damage to ").append(player);
			sb.append(", where X is 3 minus the number of cards in his or her hand.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_The_Rack
	
	private static void upkeep_Storm_World() {
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList storms = AllZoneUtil.getCardsInPlay("Storm World");

		for(Card storm:storms) {
			final Card source = storm;
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					int playerHandSize = AllZone.getZone(Constant.Zone.Hand, player).size();
					int damage = 3 - playerHandSize;
					if (damage < 1) 
						return;
					player.addDamage(damage, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(storm).append(" - deals X damage to ").append(player);
			sb.append(", where X is 3 minus the number of cards in his or her hand.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_Storm_World

	private static void upkeep_Black_Vise() {
		// Vise should always trigger, just in case the draw cards while Ability is on the stack
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList vises = AllZoneUtil.getPlayerCardsInPlay(player.getOpponent(), "Black Vise");

		// Each vise triggers
		for(Card vise:vises) {
			final Card src = vise;
			Ability ability = new Ability(src, "0") {
				@Override
				public void resolve() {
					// determine how much damage to deal the current player
					int playerHandSize = AllZone.getZone(Constant.Zone.Hand, player).size();

					if(playerHandSize <= 4) {
						return;
					}
				
					// determine how much damage to deal the current player
					player.addDamage(playerHandSize - 4, src);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Black Vise deals X damage to ").append(player);
			sb.append(", where X is the number of cards in his or her hand minus 4.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_Black_Vise
	
	private static void upkeep_Ebony_Owl_Netsuke() {
		/*
		 * At the beginning of each opponent's upkeep, if that player has seven
		 * or more cards in hand, Ebony Owl Netsuke deals 4 damage to him or her.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();

		CardList owls = AllZoneUtil.getPlayerCardsInPlay(player.getOpponent(), "Ebony Owl Netsuke");
		final CardList hand = AllZoneUtil.getPlayerHand(player);

		// Each owl triggers
		for(final Card owl:owls) {
			Ability ability = new Ability(owl, "0") {
				@Override
				public void resolve() {
					player.addDamage(4, owl);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(owl.getName()).append(" deals 4 damage to ").append(player);
			sb.append(".");
			ability.setStackDescription(sb.toString());
			if(hand.size() >= 7) {
				AllZone.Stack.add(ability);
			}
		}
	}// upkeep_Ebony_Owl_Netsuke

	private static void upkeep_Copper_Tablet() {
		/*
		 * At the beginning of each player's upkeep, Copper Tablet deals 1
		 * damage to that player.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getCardsInPlay("Copper Tablet");

		Ability ability;
		for(Card tablet:list) {
			final Card source = tablet;
			ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					player.addDamage(1, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Copper_Tablet()
	
	private static void upkeep_Sulfuric_Vortex() {
		/*
		 * At the beginning of each player's upkeep, Sulfuric Vortex deals 2
		 * damage to that player.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getCardsInPlay("Sulfuric Vortex");

		Ability ability;
		for(Card tablet:list) {
			final Card source = tablet;
			ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					player.addDamage(2, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - deals 2 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Sulfuric_Vortex()
	
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
				AllZone.Stack.add(ability);
			}
		}// for
	}// upkeep_Power_Surge()

	private static void upkeep_Klass() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList elf = new CardList(playZone.getCards());
		elf = elf.getType("Elf");

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Klaas, Elf Friend");

		if(0 < list.size() && 10 <= elf.size()) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					player.getOpponent().setLife(0, source);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Klaas, Elf Friend - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Klass

	private static void upkeep_Felidar_Sovereign() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Felidar_Sovereign

	private static void upkeep_Battle_of_Wits() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
			
			AllZone.Stack.add(ability);
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
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Mortal Combat

	private static void upkeep_Epic_Struggle() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Epic Struggle");

		CardList creats = new CardList(playZone.getCards());
		creats = creats.getType("Creature");

		if(0 < list.size() && creats.size() >= 20) {
			final Card source = list.get(0);
			Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					CardList creats = new CardList(playZone.getCards());
					creats = creats.getType("Creature");
					
					if (creats.size() >= 20)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Epic Struggle - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Epic_Struggle

	private static void upkeep_Helix_Pinnacle() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Helix_Pinnacle

	private static void upkeep_Near_Death_Experience() {
		/*
		 * At the beginning of your upkeep, if you have exactly 1 life, you win the game.
		 */
		final Player player = AllZone.Phase.getPlayerTurn();
		//PlayerLife life = AllZone.GameAction.getPlayerLife(player);
		
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
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Near_Death_Experience
	
	private static void upkeep_Test_of_Endurance() {
		/*
		 * At the beginning of your upkeep, if you have exactly 50 or more life, you win the game.
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
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Test_of_Endurance


	private static void upkeep_Barren_Glory() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
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
					CardList playList = AllZoneUtil.getCardsInZone(Constant.Zone.Play, player);
					playList = playList.getValidCards("Permanents".split(","),source.getController(),source);
					playList.remove(source);
					
					if (playList.size() == 0 && handList.size() == 0)
						player.altWinConditionMet(source.getName());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Barren Glory - ").append(player).append(" wins the game");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}// if
	}// upkeep_Barren_Glory

	private static void upkeep_Sleeper_Agent() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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

			AllZone.Stack.add(ability);
		}
	}

	private static void upkeep_Cursed_Land() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		//list = list.getName("Cursed Land");
		list = list.filter(new CardListFilter() {

			public boolean addCard(Card c) {
				return c.isLand() && c.isEnchanted();
			}
		});

		if(list.size() > 0) {
			ArrayList<Card> enchants;
			Ability ability;
			for(int i = 0; i < list.size(); i++) {
				enchants = list.get(i).getEnchantedBy();
				final Card F_card = list.get(i);
				for(Card enchant:enchants) {
					if(enchant.getName().equals("Cursed Land")) {
						//final Card c = enchant;
						ability = new Ability(enchant, "0") {

							@Override
							public void resolve() {
								//if (c.getController().equals(player))
								player.addDamage(1, F_card);
							}
						};
						ability.setStackDescription("Cursed Land deals one damage to enchanted land's controller.");
						AllZone.Stack.add(ability);
					}
				}
			}
		}//list > 0
	}//cursed land

	private static void upkeep_Pillory_of_the_Sleepless() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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

						AllZone.Stack.add(ability);
					}
				}
			}

		}//list > 0
	}//cursed land


	private static void upkeep_Greener_Pastures() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone oppPlayZone = AllZone.getZone(Constant.Zone.Play, player.getOpponent());

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
				sb.append("Greener Pastures - ").append(mostLands).append(" puts a 1/1 green Saproling token into play.");
				ability.setStackDescription(sb.toString());

				AllZone.Stack.add(ability);
			}// for

		}//else
	}// upkeep_Greener_Pastures()

	private static void upkeep_Bitterblossom() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Bitterblossom");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.loseLife(1,crd);
					CardFactoryUtil.makeToken("Faerie Rogue", "B 1 1 Faerie Rogue", crd.getController(), "B", new String[] {
							"Creature", "Faerie", "Rogue"}, 1, 1, new String[] {"Flying"});
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Bitterblossom - ").append(player).append(" loses 1 life and puts a 1/1 token into play.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Bitterblossom()

	private static void upkeep_Goblin_Assault() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Goblin Assault");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					CardFactoryUtil.makeToken("Goblin", "R 1 1 Goblin", crd.getController(), "R", new String[] {
							"Creature", "Goblin"}, 1, 1, new String[] {"Haste"});
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Goblin Assault - ").append(player);
			sb.append(" puts a 1/1 red Goblin creature token with haste onto the battlefield.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Goblin_Assault()
	
	private static void upkeep_Awakening_Zone() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Awakening Zone");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					CardList cl = CardFactoryUtil.makeToken("Eldrazi Spawn", "C 0 1 Eldrazi Spawn", crd.getController(), "", new String[] {
							"Creature", "Eldrazi", "Spawn"}, 0, 1, new String[] {"Sacrifice CARDNAME: Add 1 to your mana pool."});
					for (Card c:cl)
						c.addSpellAbility(CardFactoryUtil.getEldraziSpawnAbility(c));
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Awakening Zone - ").append(player);
			sb.append(" puts a 0/1 colorless Eldrazi Spawn creature token onto the battlefield.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Awakening_Zone()
	
	private static void upkeep_Nut_Collector() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Nut Collector");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					CardFactoryUtil.makeToken("Squirrel", "G 1 1 Squirrel", crd.getController(), "G", new String[] {
							"Creature", "Squirrel"}, 1, 1, new String[] {""});
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Nut Collector - ").append(player);
			sb.append(" puts a 1/1 green Squirrel creature token onto the battlefield.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Nut_Collector()

	private static void upkeep_Masticore() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
			
			AllZone.Stack.add(ability);
		}// for
	}//upkeep_Masticore


	private static void upkeep_Eldrazi_Monument() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
						Object o = AllZone.Display.getChoiceOptional("Select creature to sacrifice",
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
			
			AllZone.Stack.add(ability);
		}

	}//upkeep_Eldrazi_Monument

	private static void upkeep_Blaze_Counters() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

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
			
			AllZone.Stack.add(ability);
		}
	}
	
	private static void upkeep_Mycoloth() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		final int[] number = new int[1];

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Mycoloth");


		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					number[0] = card.getNetPTCounters();

					for(int j = 0; j < number[0]; j++) {
						makeToken();
					}

				}// resolve()

				public void makeToken() {
					CardFactoryUtil.makeTokenSaproling(card.getController());
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Mycoloth - ").append(player);
			sb.append(" puts a 1/1 green Saproling into play for each +1/+1 counter on Mycoloth.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Mycoloth()
	
	private static void upkeep_Verdant_Force() {
		//final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
		PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);

		CardList list = new CardList(hPlay.getCards());
		list.addAll(cPlay.getCards());
		list = list.getName("Verdant Force");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			ability = new Ability(card, "0") {
				@Override
				public void resolve() {
					CardFactoryUtil.makeTokenSaproling(card.getController());
				}
			};// Ability
			ability.setStackDescription("Verdant Force - put a 1/1 green Saproling token into play.");

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Verdant_Force()
	
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
					c.setManaCost("RG");
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

							PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
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
									o = AllZone.Display.getChoiceOptional("Select creature to sacrifice",
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
							AllZone.Stack.add(devour);
						}
					};
					
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName()).append(" - gets 2 +1/+1 counter(s) per devoured creature.");
					devour.setStackDescription(sb.toString());
					
					devour.setDescription("Devour 2");
					c.addSpellAbility(devour);
					c.addComesIntoPlayCommand(intoPlay);

					PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
					play.add(c);

				}
			};// Ability
			ability.setStackDescription("Dragon Broodmother - put a 1/1 red and green Dragon token into play.");

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Dragon_Broodmother()

	private static void upkeep_Bringer_of_the_Green_Dawn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Bringer of the Green Dawn");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					String[] choices = {"Yes", "No"};

					Object q = null;
					if(player.equals(AllZone.HumanPlayer)) {
						q = AllZone.Display.getChoiceOptional("Use Bringer of the Green Dawn?", choices);

						if(q == null || q.equals("No")) return;
						if(q.equals("Yes")) {
							CardFactoryUtil.makeToken("Beast", "G 3 3 Beast", crd.getController(), "G", new String[] {
									"Creature", "Beast"}, 3, 3, new String[] {""});
						}
					} else if(player.equals(AllZone.ComputerPlayer)) {
						CardFactoryUtil.makeToken("Beast", "G 3 3 Beast", crd.getController(), "G", new String[] {
								"Creature", "Beast"}, 3, 3, new String[] {""});
					}
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Bringer of the Green Dawn - ").append(player);
			sb.append(" puts a 3/3 Green Beast token creature into play.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Bringer_of_the_Green_Dawn()

	private static void upkeep_Bringer_of_the_Blue_Dawn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Bringer of the Blue Dawn");

		for(int i = 0; i < list.size(); i++) {
			player.mayDrawCards(2);
		}// for
	}// upkeep_Bringer_of_the_Blue_Dawn()

	private static void upkeep_Bringer_of_the_White_Dawn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
		PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Bringer of the White Dawn");

		CardList artifacts = new CardList(graveyard.getCards());
		artifacts = artifacts.getType("Artifact");

		if(artifacts.size() > 0 && list.size() > 0) {
			Ability ability;
			for(int i = 0; i < list.size(); i++) {
				ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						String[] choices = {"Yes", "No"};

						Object q = null;
						if(player.equals(AllZone.HumanPlayer)) {
							q = AllZone.Display.getChoiceOptional("Use Bringer of the White Dawn?", choices);
							if(q == null || q.equals("No")) return;
							if(q.equals("Yes")) {
								PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
								PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

								CardList arts = new CardList(graveyard.getCards());
								arts = arts.getType("Artifact");

								Object o = AllZone.Display.getChoiceOptional("Pick an artifact to put into play",
										arts.toArray());
								if(o != null) {
									Card card = (Card) o;
									graveyard.remove(card);
									playZone.add(card);
								}

							}
						}

						else if(player.equals(AllZone.ComputerPlayer)) {
							PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, player);
							PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

							CardList arts = new CardList(graveyard.getCards());
							arts = arts.getType("Artifact");

							Card card = arts.get(0);
							graveyard.remove(card);
							playZone.add(card);
						}

					}// resolve()
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("Bringer of the White Dawn - ").append(player);
				sb.append(" returns an artifact to play.");
				ability.setStackDescription(sb.toString());

				AllZone.Stack.add(ability);
			}// for
		}//if
	}// upkeep_Bringer_of_the_White_Dawn()


	private static void upkeep_Serendib_Efreet() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Serendib Efreet");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(1, crd);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Serendib Efreet - deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Serendib_Efreet()

	private static void upkeep_Nettletooth_Djinn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Nettletooth Djinn");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(1, crd);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Nettletooth Djinn - deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Nettletooth_Djinn()
	
	private static void draw_Howling_Mine(Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Howling Mine");

		for(int i = 0; i < list.size(); i++){
			if( list.getCard(i).isUntapped() ) {
				player.drawCard();
			}
		}
	}// Howling_Mine()
	
	private static void draw_Spiteful_Visions(final Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Spiteful Visions");
		player.drawCards(list.size());
	}// Spiteful_Visions()
	
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
						                		AllZone.GameAction.moveToTopOfLibrary(card);
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
			sb.append(source.getName()).append(" - draw 2 extra cards.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}//end for
	}
	
	private static void draw_Kami_Crescent_Moon(Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Kami of the Crescent Moon");
		player.drawCards(list.size());
	}// Kami_Crescent_Moon()

	private static void draw_Font_of_Mythos(Player player) {
		CardList list = AllZoneUtil.getCardsInPlay("Font of Mythos");
		player.drawCards(2*list.size());
	}// Font_of_Mythos()
	
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
	                    Object check = AllZone.Display.getChoice("Select card to put on bottom of library", handlist);
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

	private static void draw_Overbeing_of_Myth(Player player) {
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Overbeing of Myth");
		player.drawCards(list.size());
	}// Overbeing_of_Myth()

	private static void draw_Mana_Vault(final Player player){
        /*
         * Mana Vault - At the beginning of your draw step, if Mana Vault
         * is tapped, it deals 1 damage to you.
         */
        CardList manaVaults = AllZoneUtil.getPlayerCardsInPlay(player, "Mana Vault");
        for(Card manaVault:manaVaults) {
        	final Card vault = manaVault;
        	if(vault.isTapped()) {
        		final Ability damage = new Ability(vault, "0") {
        			@Override
        			public void resolve() {
        				player.addDamage(1, vault);
        			}
        		};//Ability
        		
        		StringBuilder sb = new StringBuilder();
        		sb.append(vault).append(" - does 1 damage to ").append(player);
        		damage.setStackDescription(sb.toString());
        		
        		AllZone.Stack.add(damage);
        	}
        }
	}
	
	private static void draw_Armageddon_Clock(final Player player){
        /*
         * At the beginning of your draw step, Armageddon Clock deals
         * damage equal to the number of doom counters on it to each player.
         */
		CardList clocks = AllZoneUtil.getPlayerCardsInPlay(player, "Armageddon Clock");
		for(final Card clock:clocks) {
			//final Card source = clock;
			final Ability damage = new Ability(clock, "0") {
				@Override
				public void resolve() {
					player.getOpponent().addDamage(clock.getCounters(Counters.DOOM), clock);
					player.addDamage(clock.getCounters(Counters.DOOM), clock);
				}
			};//Ability

			StringBuilder sb = new StringBuilder();
			sb.append(clock);
			sb.append(" - does "+clock.getCounters(Counters.DOOM)+" damage to ");
			sb.append("both players.");
			damage.setStackDescription(sb.toString());

			AllZone.Stack.add(damage);
		}
	}
	
	private static void upkeep_Carnophage() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Carnophage");
		if(player == AllZone.HumanPlayer) {
			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				String[] choices = {"Yes", "No"};
				Object choice = AllZone.Display.getChoice("Pay Carnophage's upkeep?", choices);
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
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Sangrophage");
		if(player == AllZone.HumanPlayer) {
			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				String[] choices = {"Yes", "No"};
				Object choice = AllZone.Display.getChoice("Pay Sangrophage's upkeep?", choices);
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

	private static void upkeep_Phyrexian_Arena() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Phyrexian Arena");

		for(int i = 0; i < list.size(); i++) {
			final Card F_card = list.get(i);
			player.drawCard();
			player.loseLife(1, F_card);

			AllZone.GameAction.checkStateEffects();
		}
	}// upkeep_Phyrexian_Arena
	
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
	}// upkeep_Phyrexian_Arena

	private static void upkeep_Master_of_the_Wild_Hunt() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Master of the Wild Hunt");

		for(int i = 0; i < list.size(); i++) {
		    CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", list.get(i).getController(), "G", new String[] {
		            "Creature", "Wolf"}, 2, 2, new String[] {""});
		}
	}// upkeep_Master_of_the_Wild_Hunt
	
	private static void upkeep_Honden_of_Seeing_Winds() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Honden of Seeing Winds");

		for(int i = 0; i < list.size(); i++) {
			PlayerZone Play = AllZone.getZone(Constant.Zone.Play, player);
			CardList hondlist = new CardList();
			hondlist.addAll(Play.getCards());
			hondlist = hondlist.getType("Shrine");
			player.drawCards(hondlist.size());
		}

	}// upkeep_Honden_of_Seeing_Winds

	private static void upkeep_Honden_of_Cleansing_Fire() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Honden of Cleansing Fire");

		for(int i = 0; i < list.size(); i++) {
			final Card source = list.get(i);
			final Ability ability = new Ability(source, "0") {
				@Override
				public void resolve() {
					PlayerZone Play = AllZone.getZone(Constant.Zone.Play, player);
					CardList hondlist = new CardList();
					hondlist.addAll(Play.getCards());
					hondlist = hondlist.getType("Shrine");
					player.gainLife(2*hondlist.size(), source);
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(source).append(" - ").append(source.getController());
			sb.append(" gains 2 life for each Shrine he controls.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_Honden_of_Cleansing_Fire

	private static void upkeep_Honden_of_Nights_Reach() {
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opponent = player.getOpponent();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Honden of Night's Reach");

		for(int i = 0; i < list.size(); i++) {
			final Ability ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					PlayerZone Play = AllZone.getZone(Constant.Zone.Play, player);
					CardList hondlist = new CardList();
					hondlist.addAll(Play.getCards());
					hondlist = hondlist.getType("Shrine");
					
					opponent.discard(hondlist.size(), this);
					/*
					for(int j = 0; j < hondlist.size(); j++) {
						if(opponent.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
						else {
							AllZone.GameAction.discardRandom(AllZone.ComputerPlayer, this);
						}
					}
					*/
				}
			};// ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(list.get(i)).append(" - ").append(list.get(i).getController().getOpponent());
			sb.append(" discards a card for each Shrine ").append(list.get(i).getController()).append(" controls.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

	// upkeep_Honden_of_Nights_Reach()

	private static void upkeep_Honden_of_Infinite_Rage() {
		final Player controller = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Honden of Infinite Rage");
		PlayerZone Play = AllZone.getZone(Constant.Zone.Play, controller);
		CardList hondlist = new CardList();
		hondlist.addAll(Play.getCards());
		hondlist = hondlist.getType("Shrine");
		for(int i = 0; i < list.size(); i++) {

			final Card card = list.get(i);
			final Ability ability = new Ability(list.get(i), "0") {


				@Override
				public void resolve() {
					PlayerZone Play = AllZone.getZone(Constant.Zone.Play, controller);
					CardList hondlist = new CardList();
					hondlist.addAll(Play.getCards());
					hondlist = hondlist.getType("Shrine");
					if(controller.equals(AllZone.HumanPlayer)) {
						Player opp = controller.getOpponent();
						PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, opp);

						String[] choices = {"Yes", "No, target a creature instead"};

						Object q = AllZone.Display.getChoiceOptional("Select computer as target?", choices);
						if(q != null && q.equals("Yes")) {
							AllZone.ComputerPlayer.addDamage(hondlist.size(), card);
						}	
						else {
							CardList cards = new CardList(oppPlay.getCards());
							CardList oppCreatures = new CardList();
							for(int i = 0; i < cards.size(); i++) {
								if(cards.get(i).isPlaneswalker() || cards.get(i).isCreature()) {
									oppCreatures.add(cards.get(i));
								}
							}

							if(oppCreatures.size() > 0) {

								Object o = AllZone.Display.getChoiceOptional("Pick target creature",
										oppCreatures.toArray());
								Card c = (Card) o;
								c.addDamage(hondlist.size(), card);
							}
						}
					}

					else {
						Card targetc = null;
						CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
						if(AllZone.HumanPlayer.getLife() > hondlist.size() * 2) {
							for(int i = 0; i < flying.size(); i++) {
								if(flying.get(i).getNetDefense() <= hondlist.size()) {
									targetc = flying.get(i);
								}

							}
						}
						if(targetc != null) {
							if(AllZone.GameAction.isCardInPlay(targetc)) targetc.addDamage(hondlist.size(), card);
						} else {
							AllZone.HumanPlayer.addDamage(hondlist.size(), card);
						}
					}
				}//resolve()
			};//SpellAbility
			
			StringBuilder sb = new StringBuilder();
			sb.append(list.get(i)).append(" - Deals ").append(hondlist.size());
			sb.append(" damage to target creature or player");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_Honden_of_Infinite_Rage


	private static void upkeep_Honden_of_Lifes_Web() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList();
		list.addAll(play.getCards());

		list = list.getName("Honden of Life's Web");

		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			final Ability ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					PlayerZone Play = AllZone.getZone(Constant.Zone.Play, player);
					CardList hondlist = new CardList();
					hondlist.addAll(Play.getCards());
					hondlist = hondlist.getType("Shrine");
					for(int j = 0; j < hondlist.size(); j++) {
						CardFactoryUtil.makeToken("Spirit", "C 1 1 Spirit", crd.getController(), "", new String[] {
								"Creature", "Spirit"}, 1, 1, new String[] {""});
					}
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(list.get(i)).append(" - ").append(list.get(i).getController());
			sb.append(" puts a 1/1 colorless Spirit creature token into play for each Shrine he controls.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}// upkeep_Honden_of_Lifes_Web

	private static void upkeep_Seizan_Perverter_of_Truth() {
		final Player player = AllZone.Phase.getPlayerTurn();

		// get all creatures
		CardList list = AllZoneUtil.getCardsInPlay("Seizan, Perverter of Truth");

		if(list.size() == 0) return;
		final Card F_card = list.get(0);
		Ability ability = new Ability(list.get(0), "0") {
			@Override
			public void resolve() {
				player.loseLife(2, F_card);
			}
		};// Ability
		
		StringBuilder sb = new StringBuilder();
		sb.append("Seizan, Perverter of Truth - ").append(player);
		sb.append(" loses 2 life and draws 2 cards");
		ability.setStackDescription(sb.toString());

		AllZone.Stack.add(ability);

		//drawing cards doesn't seem to work during upkeep if it's in an ability
		player.drawCards(2);
	}// upkeep_Seizan_Perverter_of_Truth()

	private static void upkeep_Moroii() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Moroii");
		for(int i = 0; i < list.size(); i++) {
			final Card F_card = list.get(i);
			player.loseLife(1, F_card);
		}
	}// upkeep_Moroii

	private static void upkeep_Vampire_Lacerator() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);

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

	private static void upkeep_Grinning_Demon() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Grinning Demon");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			if(!list.get(i).isFaceDown()) {
				final Card F_card = list.get(i);
				ability = new Ability(list.get(i), "0") {
					@Override
					public void resolve() {
						player.loseLife(2,F_card);
					}
				};// Ability
				
				StringBuilder sb = new StringBuilder();
				sb.append("Grinning Demon - ").append(player).append(" loses 2 life");
				ability.setStackDescription(sb.toString());

				AllZone.Stack.add(ability);
			}
		}// for
	}// upkeep_Grinning_Demon()

	private static void upkeep_Juzam_Djinn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Juzam Djinn");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(1, crd);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Juzam Djinn - deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Juzam_Djinn()

	private static void upkeep_Fledgling_Djinn() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Fledgling Djinn");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					player.addDamage(1, crd);
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Fledgling Djinn - deals 1 damage to ").append(player);
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Fledgling_Djinn()
	
	private static void upkeep_Creakwood_Liege() {
		final Player player = AllZone.Phase.getPlayerTurn();
		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);

		CardList list = new CardList(playZone.getCards());
		list = list.getName("Creakwood Liege");

		Ability ability;
		for(int i = 0; i < list.size(); i++) {
			final Card crd = list.get(i);
			ability = new Ability(list.get(i), "0") {
				@Override
				public void resolve() {
					String[] choices = {"Yes", "No"};

					Object q = null;
					if(player.equals(AllZone.HumanPlayer)) {
						q = AllZone.Display.getChoiceOptional("Use Creakwood Liege?", choices);

						if(q == null || q.equals("No")) return;
						if(q.equals("Yes")) {
							CardFactoryUtil.makeToken("Worm", "B G 1 1 Worm", crd.getController(), "B G", new String[] {
									"Creature", "Worm"}, 1, 1, new String[] {""});
						}
					} else if(player.equals(AllZone.ComputerPlayer)) {
						CardFactoryUtil.makeToken("Worm", "B G 1 1 Worm", crd.getController(), "B G", new String[] {
								"Creature", "Worm"}, 1, 1, new String[] {""});
					}
				}// resolve()
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append("Creakwood Liege - ").append(player);
			sb.append(" puts a 1/1 Green Black Worm creature token into play.");
			ability.setStackDescription(sb.toString());

			AllZone.Stack.add(ability);
		}// for
	}// upkeep_Creakwood_Liege
	
	private static void upkeep_Murkfiend_Liege()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opp = player.getOpponent();
		
		CardList list = AllZoneUtil.getPlayerCardsInPlay(opp);
		list = list.getName("Murkfiend Liege");
		
		Ability ability;
		for (int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			
			ability = new Ability(card, "0")
			{
				public void resolve()
				{
					CardList blueGreen = AllZoneUtil.getPlayerCardsInPlay(opp);
					blueGreen = blueGreen.filter(new CardListFilter() {
						public boolean addCard(Card c)
						{
							return c.isCreature() && c.isTapped() && (c.isBlue() || c.isGreen());
						}
					});
					
					for (Card crd:blueGreen)
					{
						crd.untap();
					}
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(card);
			sb.append(" - Untap all green and/or blue creatures you control during each other player's untap step.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}
	
	private static void upkeep_Seedborn_Muse()
	{
		final Player player = AllZone.Phase.getPlayerTurn();
		final Player opp = player.getOpponent();
		
		CardList list = AllZoneUtil.getPlayerCardsInPlay(opp);
		list = list.getName("Seedborn Muse");
		
		Ability ability;
		for (int i = 0; i < list.size(); i++) {
			final Card card = list.get(i);
			
			ability = new Ability(card, "0")
			{
				public void resolve()
				{
					CardList permanents = AllZoneUtil.getPlayerCardsInPlay(opp);
					permanents = permanents.filter(new CardListFilter() {
						public boolean addCard(Card c)
						{
							return c.isTapped();
						}
					});
					
					for (Card crd:permanents)
					{
						crd.untap();
					}
				}
			};// Ability
			
			StringBuilder sb = new StringBuilder();
			sb.append(card);
			sb.append(" - Untap all permanents you control during each other player's untap step.");
			ability.setStackDescription(sb.toString());
			
			AllZone.Stack.add(ability);
		}
	}

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
					CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
					CardList blueList = list.getColor("U");
					if (!blueList.isEmpty()) {
						CardFactoryUtil.makeToken("Mirror-Sigil Sergeant","W 4 4 Mirror Sigil Sergeant", card.getController(), "5 W",
								new String[]{"Creature","Rhino","Soldier"}, 4, 4, new String[]{"Trample",
								"At the beginning of your upkeep, if you control a blue permanent, you may put a token that's a copy of Mirror-Sigil Sergeant onto the battlefield."});
					}
				};

			}; // ability

			ability.setStackDescription("Mirror-Sigil Sergeant - put a token into play that's a copy of Mirror-Sigil Sergeant.");
			AllZone.Stack.add(ability);
		} // for
	} //upkeep_Mirror_Sigil_Sergeant

	public static void executeCardStateEffects() {
		Wonder.execute();
		Anger.execute();
		Valor.execute();
		Brawn.execute();
		Filth.execute();
		Dauntless_Escort.execute();
	
		Baru.execute();
		Sosukes_Summons.execute();

		//Souls_Attendant.execute();
		Wirewood_Hivemaster.execute();

		Sacrifice_NoIslands.execute();
		Sacrifice_NoForests.execute();
		Sacrifice_NoLands.execute();
		Sacrifice_NoCreatures.execute();
		
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
				zone[3] = AllZone.getZone(Constant.Zone.Play,
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

		String[]                  keyword            = {
				"tap: add B B", "tap: add W W", "tap: add G G", "tap: add U U", "tap: add R R" };

		final void addMana(Card c) {
			for(int i = 0; i < keyword.length; i++) {
				//don't add an extrinsic mana ability if the land can already has the same intrinsic mana ability
				//eg. "tap: add G"
				if(!c.getIntrinsicManaAbilitiesDescriptions().contains(
						keyword[i])) {
					//c.addExtrinsicKeyword(keyword[i]);
					SpellAbility mana = new Ability_Mana(c,
							keyword[i]) {
						private static final long serialVersionUID = 2384540533244132975L;
					};

					mana.setType("Extrinsic");
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
	

	//moved to Card.addExtrinsicAbilities

	public static Command Joiner_Adept                = new Command() {
		private static final long serialVersionUID   = -2543659953307485051L;

		CardList                  gloriousAnthemList = new CardList();

		String[]                  keyword            = {
				"tap: add B", "tap: add W", "tap: add G", "tap: add U", "tap: add R"                       };

		final void addMana(Card c) {
			for(int i = 0; i < keyword.length; i++) {
				//don't add an extrinsic mana ability if the land can already has the same intrinsic mana ability
				//eg. "tap: add G"
				if(!c.getIntrinsicManaAbilitiesDescriptions().contains(
						keyword[i])) {
					//c.addExtrinsicKeyword(keyword[i]);
					SpellAbility mana = new Ability_Mana(c,
							keyword[i]) {
						private static final long serialVersionUID = 2384540533244132975L;
					};

					mana.setType("Extrinsic");
					c.addSpellAbility(mana);
				}
			}
		}

		final void removeMana(Card c) {

			/*
                                                              for (int i = 0; i < keyword.length; i++)
                                                              	c.removeExtrinsicKeyword(keyword[i]);
			 */
			c.removeAllExtrinsicManaAbilities();
		}

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				removeMana(c);
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Joiner Adept");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length && outer < 1; outer++) // 1
				// is
				// a
				// cheat
			{
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Land");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					addMana(c);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	}; // Muscles_Sliver
	
	public static Command Marrow_Gnawer               = new Command() {
		private static final long serialVersionUID   = -2500490393763095527L;

		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "Fear";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword(keyword);
			}

			list.clear();
			PlayerZone[] zone = getZone("Marrow-Gnawer");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList();
				creature.addAll(AllZone.Human_Play.getCards());
				creature.addAll(AllZone.Computer_Play.getCards());
				creature = creature.getType("Rat");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword(keyword);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	}; // Marrow-Gnawer

	

	public static Command Gemhide_Sliver              = new Command() {
		private static final long serialVersionUID   = -2941784982910968772L;

		CardList                  gloriousAnthemList = new CardList();

		String[]                  keyword            = {
				"tap: add B", "tap: add W", "tap: add G", "tap: add U", "tap: add R"                       };

		final void addMana(Card c) {

			for(int i = 0; i < keyword.length; i++) {
				if(!c.getIntrinsicManaAbilitiesDescriptions().contains(
						keyword[i])) {
					//c.addExtrinsicKeyword(keyword[i]);
					SpellAbility mana = new Ability_Mana(c,
							keyword[i]) {
						private static final long serialVersionUID = -8909660504657778172L;
					};
					mana.setType("Extrinsic");
					c.addSpellAbility(mana);
				}
			}
		}

		final void removeMana(Card c) {
			/*
                                                              for (int i = 0; i < keyword.length; i++)
                                                              	c.removeExtrinsicKeyword(keyword[i]);
			 */
			c.removeAllExtrinsicManaAbilities();
		}

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				removeMana(c);
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Gemhide Sliver");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length && outer < 1; outer++) {
				CardList creature = new CardList();
				creature.addAll(AllZone.Human_Play.getCards());
				creature.addAll(AllZone.Computer_Play.getCards());
				creature = creature.getType("Sliver");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					addMana(c);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	}; // Gemhide_Sliver

	
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
						
						final Ability_Tap ability = new Ability_Tap(c)
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
					    ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true, false));
					    ability.setDescription("tap: This land deals 1 damage to target creature or player.");
					    
					    c.addSpellAbility(ability);
					    
						gloriousAnthemList.add(c);
					}
				}
			}
			
		}
	};
	
    public static Command Akromas_Memorial                = new Command() {
        
        private static final long serialVersionUID   = -670715429635395830L;
        CardList              gloriousAnthemList = new CardList();
    	Player[]              Akroma_Memorial_Controller = new Player[10]; // Shouldn't have more than 2 in play since its legendary, let alone 10.
        public void execute() {
            String keyword1 = "Flying";
            String keyword2 = "First Strike";
            String keyword3 = "Vigilance";
            String keyword4 = "Trample";
            String keyword5 = "Haste";
            String keyword6 = "Protection from black";
            String keyword7 = "Protection from red";
            CardList list = gloriousAnthemList;
            Card c;
            
			CardList Akroma_List = AllZoneUtil.getCardsInPlay("Akroma's Memorial");
			CardList Akroma_Zone = new CardList();
        	for(int i = 0; i < Akroma_List.size(); i++) {
        		Akroma_Zone.clear();
				Akroma_Zone.addAll(AllZone.getZone(Constant.Zone.Play, Akroma_List.get(i).getController()).getCards());	
        	}
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
            for(int i = 0; i < List_Copy.size(); i++) {
                c = List_Copy.get(i);                 
                if(!Akroma_Zone.contains(c)) {
                	c.removeExtrinsicKeyword(keyword1);
                    c.removeExtrinsicKeyword(keyword2);
                    c.removeExtrinsicKeyword(keyword3);
                    c.removeExtrinsicKeyword(keyword4);
                    c.removeExtrinsicKeyword(keyword5);
                    c.removeExtrinsicKeyword(keyword6);
                    c.removeExtrinsicKeyword(keyword7);
                    list.remove(c);
                }
            }

            PlayerZone[] zone = getZone("Akroma's Memorial");
			if(AllZoneUtil.isCardInPlay("Akroma's Memorial")) {
			CardList ControllerCheck = AllZoneUtil.getCardsInPlay("Akroma's Memorial");
			for(int i = 0; i < ControllerCheck.size(); i++) Akroma_Memorial_Controller[i] = ControllerCheck.get(i).getController();
            for(int outer = 0; outer < zone.length; outer++) {
                CardList creature = new CardList(
                        zone[outer].getCards());
                creature = creature.getType("Creature");
            	
                for(int i = 0; i < creature.size(); i++) {
                    c = creature.get(i);
                    if(!list.contains(c)) {
                    gloriousAnthemList.add(c);
                    
                    if(!c.getIntrinsicKeyword().contains(keyword1)) {
                        c.addExtrinsicKeyword(keyword1);  
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword2)) {
                        c.addExtrinsicKeyword(keyword2);
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword3)) {
                        c.addExtrinsicKeyword(keyword3);
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword4)) {
                        c.addExtrinsicKeyword(keyword4);
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword5)) {
                        c.addExtrinsicKeyword(keyword5);
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword6)) {
                        c.addExtrinsicKeyword(keyword6);
                    }
                    if(!c.getIntrinsicKeyword().contains(keyword7)) {
                        c.addExtrinsicKeyword(keyword7);
                    }
                    }
                }// for inner
            }// for outer
			} 
        }// execute()
    };
    
	
	public static Command StaticEffectKeyword  		= new Command() {
		/** StaticEffectKeyword
		 * Syntax:[ k[0] StaticEffect : k[1] Where the Card must be : k[2] Which Cards the Bonus Affects : 
		 * 			k[3] What Bonus does the Card have : k[4] Special Conditions : k[5] Description
		 */
		
		private static final long serialVersionUID = -8467814700545847505L;
		int						  max			  		 = 100;
		CardList[]                old            		 = new CardList[max];
		CardList[]                next             	 	 = new CardList[max];
		CardList                  CardsWithKeyword 	 	 = new CardList();
		String[] 				      InfoStorage	   		 = new String[max];
		int						  KeywordsActive		 = 0;
		int						  ActivationNumber	     = 0;

		public void execute() {
			// Initialize Variables
			if(old[0] == null) {
				for(int i = 0; i < max; i++) {
					old[i] = new CardList();
					next[i] = new CardList();
					old[i].clear();
					next[i].clear();
					InfoStorage[i] = "-1";
				}
			}
			// Reset Variables at Start of Game
			if(AllZone.GameAction.StaticEffectKeywordReset) {
			AllZone.GameAction.StaticEffectKeywordReset = false;
			for(int i = 0; i < max; i++)	{
				old[i] = new CardList();
				next[i] = new CardList();
				old[i].clear();
				next[i].clear();
				InfoStorage[i] = "-1";	
			}
			}
			// Check if the Bonuses need to be removed
			if(Phase.GameBegins == 1) {
			for(int i = 0; i < max; i++)	{
				String[] InfoSplit = InfoStorage[i].split(":");
				for(int z = 0; z < CardsWithKeyword.size(); z++)
					if(Integer.valueOf(InfoSplit[0]) == CardsWithKeyword.get(z).getUniqueNumber()) 
						removeKeyword(old[i],CardsWithKeyword.get(z),i,Integer.valueOf(InfoSplit[1]),InfoSplit[2]);
			}
			// Gather Cards in Play and Graveyards with the Keyword
            PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
            PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
            PlayerZone Hgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.HumanPlayer);
            PlayerZone Cgrave = AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer);
            
     		CardList Cards_WithKeyword = new CardList();
            Cards_WithKeyword.add(new CardList(Hplay.getCards()));
            Cards_WithKeyword.add(new CardList(Cplay.getCards()));
            Cards_WithKeyword.add(new CardList(Hgrave.getCards()));
            Cards_WithKeyword.add(new CardList(Cgrave.getCards()));
     		Cards_WithKeyword = Cards_WithKeyword.filter(new CardListFilter() {
                 public boolean addCard(Card c) {
                     if(c.getKeyword().toString().contains("StaticEffect")) return true;
                     return false;
                 }
             });
     		// For each card found, find the keywords which are the StatusEffect Keywords
     		for(int i = 0; i < Cards_WithKeyword.size() ; i++) {
     			Card card = Cards_WithKeyword.get(i);
     		        ArrayList<String> a = card.getKeyword();
     		        int StaticEffectKeywords = 0;
     		        int StaticEffectKeyword_Number[] = new int[a.size()];
     		        for(int x = 0; x < a.size(); x++)
     		            if(a.get(x).toString().startsWith("StaticEffect")) {
     		            	StaticEffectKeyword_Number[StaticEffectKeywords] = x;
     		            	StaticEffectKeywords = StaticEffectKeywords + 1;
     		            }
     		       // For each keyword found, Record Data about the keyword and the source card
     		        // Record Data Start
     		        for(int CKeywords = 0; CKeywords < StaticEffectKeywords; CKeywords++) {
                        String parse = card.getKeyword().get(StaticEffectKeyword_Number[CKeywords]).toString();                
                        String k[] = parse.split(":");
                        int ANCount = 0;
     	     			KeywordsActive = 0;
     	     			while(InfoStorage[KeywordsActive] != "-1") {
     	     				KeywordsActive++;		
     	     			}
     	     			
     	   	      	// Special Conditions
     		      		boolean SpecialConditionsMet = true;
     		      		CardList SpecialConditionsCardList = new CardList();
     		      		if(k[4].contains("CardsInHandMore")) {
     		      			SpecialConditionsCardList.clear();
     		      			String Condition = k[4].split("/")[1];
     		      			SpecialConditionsCardList.addAll(AllZone.getZone(Constant.Zone.Hand, card.getController()).getCards());
     		      			if(SpecialConditionsCardList.size() < Integer.valueOf(Condition)) SpecialConditionsMet = false;
     		      		}
     		      		if(SpecialConditionsMet) {
     	     			boolean ActivatedAlready = false;
     	     			// JOptionPane.showMessageDialog(null, ANCount + " " + CKeywords, "", JOptionPane.INFORMATION_MESSAGE); 
     	     			for(int y = 0; y < max; y++) {
     	     			if(InfoStorage[y].split(":")[0].equals(String.valueOf(card.getUniqueNumber()))) {
     	     				if(ANCount == CKeywords) {
     	     				ActivatedAlready = true;
     	     				ActivationNumber = y;	
     	     				break;
     	     			} else {
     	     				ANCount++;	
     	     			}
     	     			}
     	     			}
     	     			if(!ActivatedAlready) {
     	     				InfoStorage[KeywordsActive] = String.valueOf(card.getUniqueNumber()) + ":" + StaticEffectKeyword_Number[CKeywords] + ":" +  card.getController();
     	     				ActivationNumber = KeywordsActive;
     	     			}
     	     			CardList SourceCard_in_CardsWithKeyword = CardsWithKeyword.getName(card.getName());
     	     			for(int i1 = 0; i1 < SourceCard_in_CardsWithKeyword.size() ; i1++) {
         	     			if(!SourceCard_in_CardsWithKeyword.get(i1).equals(card)) 
         	     				SourceCard_in_CardsWithKeyword.remove(SourceCard_in_CardsWithKeyword.get(i1));	
         	     			}
     	     			// JOptionPane.showMessageDialog(null, ANCount + " " + SourceCard_in_CardsWithKeyword.size(), "", JOptionPane.INFORMATION_MESSAGE);
     	     			for(int i1 = 0; i1 < ANCount - SourceCard_in_CardsWithKeyword.size() + 1; i1++) {
     	     				if(CKeywords + 1 == StaticEffectKeywords) CardsWithKeyword.add(card);
     	     			}
     	     			 // Record Data End
     	     			
     	     		 // Final Statement: For each keyword found, run addKeyword	
                     addKeyword(card,ActivationNumber,k);
     		        }
     		}
			}
			}
		}// execute()

		void addKeyword(Card SourceCard, int ANumber, String[] Keyword_Details) {
			// Initialize Variables
				next[ANumber].clear();
				final Card F_SourceCard = SourceCard;
	      		CardList Cards_inZone = new CardList();
	      		
	      		// Where does the SourceCard have to be?
	      		boolean CardInRightZone = false;
	      		if(Keyword_Details[1].equals("Play")&& AllZone.GameAction.isCardInPlay(SourceCard)) CardInRightZone = true;
	      		if(Keyword_Details[1].equals("Graveyard")&& AllZone.GameAction.isCardInGrave(SourceCard)) CardInRightZone = true;
	      	
	      		if(CardInRightZone) {
	      		// Who gets the Bonus?
	      		Cards_inZone.add(AffectedCards(SourceCard, Keyword_Details));
	      		
	      		// Special Conditions
		      		final String[] Specific = Keyword_Details[4].split("!");
		      		final int[] Restriction_Count = new int[1]; 
		      		for(int i = 0; i < Specific.length;i++) {
		      			if(Specific[i].contains("Type.") && !Specific[i].contains("NonType.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                if(c.isType(Specific[Restriction_Count[0]].replaceFirst("Type.", ""))) return true;
                                return false;
                            }
                		});
		      			}
		      			if(Specific[i].contains("NonType.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    if(!c.isType(Specific[Restriction_Count[0]].replaceFirst("NonType.", ""))) return true;
                                    return false;
                                }
                    		});
   		      			}
		      			if(Specific[i].contains("Color.") && !Specific[i].contains("NonColor.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    if(CardUtil.getColors(c).contains(Specific[Restriction_Count[0]].replaceFirst("Color.", ""))) return true;
                                    return false;
                                }
                    		});	
   		      			}
		      			if(Specific[i].contains("NonColor.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    if(!CardUtil.getColors(c).contains(Specific[Restriction_Count[0]].replaceFirst("NonColor.", ""))) return true;
                                    return false;
                                }
                    		});	
   		      			}
		      			if(Specific[i].contains("with.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
	                            public boolean addCard(Card c) {
	                                if(c.hasKeyword(Specific[Restriction_Count[0]].replaceFirst("with.", ""))) return true;
	                                return false;
	                            }
	                		});
			      		}
		      			if(Specific[i].contains("without.")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
	                            public boolean addCard(Card c) {
	                                if(!c.hasKeyword(Specific[Restriction_Count[0]].replaceFirst("without.", ""))) return true;
	                                return false;
	                            }
	                		});
			      		}
		      			if(Specific[i].contains("enchanted")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
	                            public boolean addCard(Card c) {
	                                if(c.isEnchanted()) return true;
	                                return false;
	                            }
	                		});
			      		}
		      			if(Specific[i].equals("NotSelf")) {
		      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    if(!c.equals(F_SourceCard)) return true;
                                    return false;
                                }
                    		});
   		      			}
		      		Restriction_Count[0]++;
		      		}
		      		
		    // From the cards left, determine which cards have already got the bonus 		
			for(int i = 0; i < Cards_inZone.size(); i++) {
				if(!old[ANumber].contains(Cards_inZone.get(i))) next[ANumber].add(Cards_inZone.get(i));
			}
			// Final Statement: Run addKeyword noting which cards should have the bonus but don't have it yet
			addKeyword(SourceCard,next[ANumber],Keyword_Details,ANumber);
		}
		}

		void addKeyword(Card SourceCard, CardList list, String[] Keyword_Details, int ANumber) {
			// Initialize Variables
			String[] Keyword = Keyword_Details[3].split("!");
			// For each effect .....
			for(int a =0; a < Keyword.length;a++) {
			int Count = list.size();
			// .... For each card that needs the bonus, add the bonus
				for(int i = 0; i < Count; i++) {
					if(a + 1 == Keyword.length && !Keyword[a].contains("SetPT")) old[ANumber].add(list.get(i)); // Only store the card when it has all the bonuses added
					if(Keyword[a].contains("PTBonus")) {
						list.get(i).addSemiPermanentAttackBoost(Integer.valueOf(Keyword[a].split("/")[1].replace("+","")));
						list.get(i).addSemiPermanentDefenseBoost(Integer.valueOf(Keyword[a].split("/")[2].replace("+","")));
					}
					else if(Keyword[a].contains("SetPT")) {
						//old[ANumber].remove(list.get(i)); //hack, make sure the card is not in the "old" list if there's a setPT 
						// -9001 is a failsafe number, It will be used when a card only has a SetPT bonus which only affects 
						// cards with either a Power or Toughness bonus, but not both. NOT TESTED
						int[] SetPTAmounts = SetPTBonus(SourceCard, Keyword_Details);
						if(SetPTAmounts[0] != -9001) list.get(i).setBaseAttack(SetPTAmounts[0]);
						if(SetPTAmounts[0] != -9001) list.get(i).setBaseDefense(SetPTAmounts[1]);
					}
			else if(Keyword[a].contains("Keyword")) {
				list.get(i).addExtrinsicKeyword(Keyword[a].replaceFirst("Keyword/", ""));
			 }
			}
			}
		}
		
		void removeKeyword(CardList list , Card Source,int ANumber, int AbilityNumber, String LastKnownController) {
			// Initialize Variables
        	CardList List_Copy = new CardList();
        	List_Copy.add(list);
        	String keyword = "";
            String parse = Source.getKeyword().get(AbilityNumber).toString();                
            String k[] = parse.split(":");
                     
            // Get the Effects from the Keyword
            int Effects = 1;                   
            String EffectParse = k[3];                
            String Effect[] = EffectParse.split("!");
            Effects = Effect.length;
            for(int y = 0; y < Effects; y++) { 
            boolean Done = false;
            if(Effect[y].contains("SetPT")) { // Auto reset for cards with SetPT
            	CardsWithKeyword.remove(Source);
            	InfoStorage[ANumber] = "-1";
            	old[ANumber].remove(Source);
            	Done = true;
             }
     		 if(!Done) { // Basically if its not SetPT
     		 // Is the Card in the right location	 
     		 boolean SourceCardinRightZone = true;
     		 if(k[1].equals("Play") && !AllZone.GameAction.isCardInPlay(Source)) SourceCardinRightZone = false;
     		 if(k[1].equals("Graveyard") && !AllZone.GameAction.isCardInGrave(Source)) SourceCardinRightZone = false;
     		 if(!LastKnownController.equals(Source.getController().getName())) SourceCardinRightZone = false;
     		  // Special Conditions
      	      boolean SpecialConditionsMet = true;
   	      	  CardList SpecialConditionsCardList = new CardList();
   	      	  if(k[4].contains("CardsInHandMore")) {
   	      			SpecialConditionsCardList.clear();
   	      			String Condition = k[4].split("/")[1];
   	      			SpecialConditionsCardList.addAll(AllZone.getZone(Constant.Zone.Hand, Source.getController()).getCards());
   	      			if(SpecialConditionsCardList.size() < Integer.valueOf(Condition)) SpecialConditionsMet = false;
   	      	  }
   	      // If the Source Card is not in the right Location or the Special Conditions are no longer met - Then Remove Bonus
            		if(!SourceCardinRightZone || !SpecialConditionsMet) {
            			if(Effects == y + 1) {
            			CardsWithKeyword.remove(Source);
            			InfoStorage[ANumber] = "-1";
            			}
               for(int i = 0; i < List_Copy.size(); i++) {
			   Card c = List_Copy.get(i);
			   if(old[ANumber].contains(c)) {
					if(Effects == y + 1) old[ANumber].remove(c);
                    if(Effect[y].contains("PTBonus")) {
                   	keyword = Effect[y]; 
					c.addSemiPermanentAttackBoost(Integer.valueOf(keyword.split("/")[1].replace("+","")) * -1);
					c.addSemiPermanentDefenseBoost(Integer.valueOf(keyword.split("/")[2].replace("+","")) * -1);
			   } else if(Effect[y].contains("Keyword")) {
                   	keyword = Effect[y].split("/")[1];
					List_Copy.get(i).removeExtrinsicKeyword(keyword);
					}
			   }
               }
               }
        // If a card under the influence of a source card is not in the right location - Then Remove Bonus
			for(int i = 0; i < List_Copy.size(); i++) {
				Card c = List_Copy.get(i);
				if(old[ANumber].contains(c) && !AffectedCards(Source, k).contains(c)) {
					old[ANumber].remove(c);
                    if(Effect[y].contains("PTBonus")) {
                      	 keyword = Effect[y]; 
								c.addSemiPermanentAttackBoost(Integer.valueOf(keyword.split("/")[1].replace("+","")) * -1);
								c.addSemiPermanentDefenseBoost(Integer.valueOf(keyword.split("/")[2].replace("+","")) * -1);
						} else if(Effect[y].contains("Keyword")) {
							keyword = Effect[y].split("/")[1];
							List_Copy.get(i).removeExtrinsicKeyword(keyword);
						}
						}
     		        	}
                     	}
            			}	
		}

		CardList AffectedCards (Card SourceCard, String[] Keyword_Details) {
			/** 
			 	This Function is used for 2 purposes:
				1. To determine which cards should be affected by a static effect
				2. To determine the value of SetPT bonuses.
				It works by going through all conditions and finding the cards in the zones
			**/
			CardList Cards_inZone = new CardList();
      		if(Keyword_Details[2].equals("All Permanents")) {
	      		Cards_inZone.addAll(AllZone.Human_Play.getCards());
	      		Cards_inZone.addAll(AllZone.Computer_Play.getCards());
	      		}
	      		if(Keyword_Details[2].equals("Permanents you Control")) {
		      		Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Play, SourceCard.getController()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("Permanents your Opponents Control")) {
		      		Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Play, SourceCard.getController().getOpponent()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("ControllerCardsInHand")) {
		      		Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("OpponentCardsInHand")) {
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController().getOpponent()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("ControllerCardsInGrave")) {
		      		Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Graveyard, SourceCard.getController()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("OpponentCardsInGrave")) {
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Graveyard, SourceCard.getController().getOpponent()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("ControllerAllCards") || Keyword_Details[2].equals("AllCards")) {
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Play, SourceCard.getController()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Graveyard, SourceCard.getController()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Library, SourceCard.getController()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Removed_From_Play, SourceCard.getController()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("OpponentAllCards") || Keyword_Details[2].equals("AllCards")) {
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Play, SourceCard.getController().getOpponent()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController().getOpponent()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Graveyard, SourceCard.getController().getOpponent()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Library, SourceCard.getController().getOpponent()).getCards());
	      			Cards_inZone.addAll(AllZone.getZone(Constant.Zone.Removed_From_Play, SourceCard.getController().getOpponent()).getCards());
		      		}
	      		if(Keyword_Details[2].equals("Self")) {
		      		Cards_inZone.add(SourceCard);
		      		}
	      		if(Keyword_Details[2].equals("Enchanted Permanent")) {
	      			CardList CardsinPlay = new CardList();
	      			CardsinPlay.addAll(AllZone.Human_Play.getCards());
	      			CardsinPlay.addAll(AllZone.Computer_Play.getCards());
		      		for(int i = 0; i < CardsinPlay.size(); i++)
		      		if(CardsinPlay.get(i).getEnchantedBy().contains(SourceCard)) Cards_inZone.add(CardsinPlay.get(i));
		      		}
	      		if(Keyword_Details[2].equals("Eqiupped Permanent")) {
	      			CardList CardsinPlay = new CardList();
	      			CardsinPlay.addAll(AllZone.Human_Play.getCards());
	      			CardsinPlay.addAll(AllZone.Computer_Play.getCards());
		      		for(int i = 0; i < CardsinPlay.size(); i++)
		      		if(CardsinPlay.get(i).getEquippedBy().contains(SourceCard)) Cards_inZone.add(CardsinPlay.get(i));
		      		}
			return Cards_inZone;
		}
		
		int[] SetPTBonus (Card SourceCard, String[] Keyword_Details) {
			/**
			 	This Function determines the value of SetPT Bonuses and sends it to addKeyword
			 	It calls AffectedCards, to get the cards which would determine the value of the SetPT Bonus 
			 	and then uses special conditions to determine the proper value.
			**/
			 
			int[] Bonus = new int[2];
			String[] CardsinZoneDetails = new String[3];
			CardsinZoneDetails[2] = Keyword_Details[4]; // Where to search for Cards MUST be the only special condition
			final String[] Details = Keyword_Details[3].split("/");
			for(int i =0; i < Details.length - 1; i++) {
				final int[] Count = new int[1];
				if(Details[i+1].contains("Type") && !Details[i+1].contains("NonType")) {
					CardList Cards_inZone = AffectedCards(SourceCard, CardsinZoneDetails);
      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(c.isType(Details[Count[0] + 1].replaceFirst("Type.", ""))) return true;
                            return false;
			}
      				});
      				Bonus[i] = Cards_inZone.size();
				}
				if(Details[i+1].contains("NonType")) {
					CardList Cards_inZone = AffectedCards(SourceCard, CardsinZoneDetails);
      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(!c.isType(Details[Count[0] + 1].replaceFirst("NonType.", ""))) return true;
                            return false;
			}
      				});
      				Bonus[i] = Cards_inZone.size();
				}
				if(Details[i+1].contains("Color") && !Details[i+1].contains("NonColor")) {
					CardList Cards_inZone = AffectedCards(SourceCard, CardsinZoneDetails);
      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(CardUtil.getColors(c).contains(Details[Count[0] + 1].replaceFirst("Color.", ""))) return true;
                            return false;
                        }
            		});	
      				Bonus[i] = Cards_inZone.size();
				}
				if(Details[i+1].contains("NonColor")) {
					CardList Cards_inZone = AffectedCards(SourceCard, CardsinZoneDetails);
      				Cards_inZone = Cards_inZone.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(!CardUtil.getColors(c).contains(Details[Count[0] + 1].replaceFirst("NonColor.", ""))) return true;
                            return false;
                        }
            		});	;
      				Bonus[i] = Cards_inZone.size();
				}
				if(Details[i+1].equals("NoModifier")) {
					Bonus[i] = -9001; // Failsafe
				}
				Count[0]++;
			}
			return Bonus;
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
			PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
			PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
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
						String k[] = keyword.split(":");    
						
						if(SpecialConditionsMet(cardWithKeyword, k[3])) { //special Conditions are Threshold, etc.
						
							final String affected = k[1];			
							final String specific[] = affected.split(",");
							CardList affectedCards = AffectedCards(cardWithKeyword, k); // options are All, Self, Enchanted etc.
							affectedCards = affectedCards.getValidCards(specific, cardWithKeyword.getController(), cardWithKeyword);
							se.setAffectedCards(affectedCards);
							
							int x = 0;
		            		if (k[2].contains("X")) 
		                 		x = CardFactoryUtil.xCount(cardWithKeyword, cardWithKeyword.getSVar("X").split("\\$")[1]);
		                 	se.setXValue(x);	
		            		
							addStaticEffects(affectedCards,k[2],x); //give the boni to the affected cards

							storage.add(se); // store the information
						}
	            	}
	            }
	    	}
		}// execute()
		
		void addStaticEffects(CardList affectedCards, String Keyword_Details, int xValue) {
			
			int powerbonus = 0;
			int toughnessbonus = 0;
			String[] Keyword = Keyword_Details.split("/",3);
			
			Keyword[0] = Keyword[0].replace("+","");
			Keyword[1] = Keyword[1].replace("+","");
			
			if(!Keyword[0].contains("X")) powerbonus = Integer.valueOf(Keyword[0]);
			else powerbonus = xValue; 		// the xCount takes places before
			
			if(!Keyword[1].contains("X")) toughnessbonus = Integer.valueOf(Keyword[1]);
			else toughnessbonus = xValue;
			
			for(int i = 0; i < affectedCards.size(); i++) {
				Card affectedCard = affectedCards.get(i);
				affectedCard.addSemiPermanentAttackBoost(powerbonus);
				affectedCard.addSemiPermanentDefenseBoost(toughnessbonus);
				if (Keyword.length > 2) {
					String Keywords[] = Keyword[2].split(" & ");
					for(int j = 0; j < Keywords.length; j++) {
						affectedCard.addExtrinsicKeyword(Keywords[j]);
					}
				}
			}
		}
		
		void removeStaticEffect(StaticEffect se) {
			Card Source = se.getSource();
			CardList affected = se.getAffectedCards();
			int KeywordNumber = se.getKeywordNumber();
			int xValue = se.getXValue(); 		// the old xValue has to be removed, not the actual one!
            String parse = Source.getKeyword().get(KeywordNumber).toString();                
            String k[] = parse.split(":");
			for(int i = 0; i < affected.size(); i++) {
				removeStaticEffect(affected.get(i),k,xValue);
			}	
		}
		
		void removeStaticEffect(Card affectedCard, String[] Keyword_Details, int xValue) {
			
			int powerbonus = 0;
			int toughnessbonus = 0;
			String[] Keyword = Keyword_Details[2].split("/",3);
			
			Keyword[0] = Keyword[0].replace("+","");
			Keyword[1] = Keyword[1].replace("+","");
			
			if(!Keyword[0].contains("X")) powerbonus = Integer.valueOf(Keyword[0]);
			else powerbonus = xValue; 		
			
			if(!Keyword[1].contains("X")) toughnessbonus = Integer.valueOf(Keyword[1]);
			else toughnessbonus = xValue;
			
			affectedCard.addSemiPermanentAttackBoost(powerbonus * -1);
			affectedCard.addSemiPermanentDefenseBoost(toughnessbonus * -1);
			if (Keyword.length > 2) {
				String Keywords[] = Keyword[2].split(" & ");
				for(int j = 0; j < Keywords.length; j++) {
					affectedCard.removeExtrinsicKeyword(Keywords[j]);
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
  	      	if(SpecialConditions.contains("Threshold")) {
  	      		if (!SourceCard.getController().hasThreshold()) return false;
  	      	}
  	      	if(SpecialConditions.contains("Hellbent")) {
  	      		CardList Handcards = new CardList();
  	      		Handcards.addAll(AllZone.getZone(Constant.Zone.Hand, SourceCard.getController()).getCards());
	      		if (Handcards.size() > 0) return false;
	      	}
  	      	if(SpecialConditions.contains("Metalcraft")) {
  	      		PlayerZone play = AllZone.getZone(Constant.Zone.Play, SourceCard.getController());
  	      		CardList CardsinPlay = new CardList(play.getCards());
      			CardsinPlay = CardsinPlay.getType("Artifact");
	      		if (CardsinPlay.size() < 3) return false;
	      	}
  	      	if(SpecialConditions.contains("isPresent")) { // is a card of a certain type/color present?
  	    	  	String Requirements = SpecialConditions.replaceAll("isPresent ", "");
    			CardList CardsinPlay = new CardList();
      			CardsinPlay.addAll(AllZone.Human_Play.getCards());
      			CardsinPlay.addAll(AllZone.Computer_Play.getCards());
      			String Conditions[] = Requirements.split(",");
      			CardsinPlay = CardsinPlay.getValidCards(Conditions, SourceCard.getController(), SourceCard);
	      		if (CardsinPlay.isEmpty()) return false;
  	      	}
  	      	if(SpecialConditions.contains("isNotPresent")) { // is no card of a certain type/color present?
  	      		String Requirements = SpecialConditions.replaceAll("isNotPresent ", "");
    			CardList CardsinPlay = new CardList();
      			CardsinPlay.addAll(AllZone.Human_Play.getCards());
      			CardsinPlay.addAll(AllZone.Computer_Play.getCards());
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
      			Cards_inZone.addAll(AllZone.Human_Play.getCards());
	      		Cards_inZone.addAll(AllZone.Computer_Play.getCards());
	      		}
      		
      		//no longer needed: use All with Other as restriction of ValidCard
      		if(Range.equals("Other")) {
      			Cards_inZone.addAll(AllZone.Human_Play.getCards());
	      		Cards_inZone.addAll(AllZone.Computer_Play.getCards());
	      		final Card F_SourceCard = SourceCard;
	      		Cards_inZone = Cards_inZone.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						if(!c.equals(F_SourceCard)) return true;
						return false;
					}
	      		});
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
				creature.addAll(AllZone.Human_Play.getCards());
				creature.addAll(AllZone.Computer_Play.getCards());

				for(int i = 0; i < creature.size(); i++) {
					final Card crd = creature.get(i);
					CardList Type = new CardList();
					Type.addAll(AllZone.Human_Play.getCards());
					Type.addAll(AllZone.Computer_Play.getCards());
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
				NonLand.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
				NonLand.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
				
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
    				NonLand.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
    				NonLand.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
    				
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
	
	public static Command Angry_Mob = new Command() {
		private static final long serialVersionUID = 4129359648989610278L;

		public void execute() {
			Player player = AllZone.Phase.getPlayerTurn();
			
			// get all creatures
			CardList mobs = AllZoneUtil.getCardsInPlay("Angry Mob");
			for(int i = 0; i < mobs.size(); i++) {
				Card c = mobs.get(i);
				if(player.equals(c.getController())) {
					Player opp = player.getOpponent();
					int boost = AllZoneUtil.getPlayerTypeInPlay(opp, "Swamp").size();
					c.setBaseAttack(2+boost);
					c.setBaseDefense(2+boost);
				}
				else {
				c.setBaseAttack(2);
				c.setBaseDefense(2);
				}
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
	
	/**
	 * stores the Command
	 */
	public static Command Primalcrux = new Command() {
		private static final long serialVersionUID = -5760985115546543522L;

		public void execute() {
			// get all creatures
			CardList cards = AllZoneUtil.getCardsInPlay("Primalcrux");
			for(Card c:cards) {
				Player player = c.getController();
				int pt = CardFactoryUtil.getNumberOfManaSymbolsControlledByColor("B", player);
				c.setBaseAttack(pt);
				c.setBaseDefense(pt);
			}
		}// execute()
	};

	
	public static Command Tabernacle                  = new Command() {
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

	
	public static Command Goblin_Assault                = new Command() {

		private static final long serialVersionUID = 5138624295158786103L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "CARDNAME attacks each turn if able.";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword(keyword);
			}

			list.clear();
			PlayerZone[] zone = getZone("Goblin Assault");

			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList();
				creature.addAll(AllZone.Human_Play.getCards());
				creature.addAll(AllZone.Computer_Play.getCards());
				creature = creature.getType("Goblin");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					if(!c.getKeyword().contains(keyword)) {
						c.addExtrinsicKeyword(keyword);
						gloriousAnthemList.add(c);
					}
				}// for inner
			}// for outer
		}// execute()
	}; //Goblin Assault               

	public static Command That_Which_Was_Taken        = new Command() {
		private static final long serialVersionUID   = -4142514935709694293L;

		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "HIDDEN Indestructible";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword(keyword);
			}

			list.clear();
			PlayerZone[] zone = getZone("That Which Was Taken");

			if(zone.length > 0) {
				CardList cards = new CardList();
				cards.addAll(AllZone.Human_Play.getCards());
				cards.addAll(AllZone.Computer_Play.getCards());

				for(int i = 0; i < cards.size(); i++) {
					c = cards.get(i);
					if(!c.getKeyword().contains(keyword)
							&& c.getCounters(Counters.DIVINITY) > 0) {
						c.addExtrinsicKeyword(keyword);
						gloriousAnthemList.add(c);
					}
				}// for inner
			}// for outer
		}// execute()
	};
	
	
	public static Command Emperor_Crocodile                      = new Command() {
		private static final long serialVersionUID   = -5406532269475480827L;

		@SuppressWarnings("unused")
		// gloriousAnthemList
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			// get all cards

			CardList list = AllZoneUtil.getCardsInPlay("Emperor Crocodile");
			
			for (int i=0;i<list.size();i++) {
				
				Card c = list.get(i);
				
			
			//Player only has one creature in play (the crocodile) then it dies.
			if (AllZoneUtil.getCreaturesInPlay(c.getController()).size() == 1) {
	            AllZone.GameAction.sacrifice(c);	
			}
            
			}
			

		}// execute()
	};
	
	public static Command Wrens_Run_Packmaster        = new Command() {

		private static final long serialVersionUID   = 6089293045852070662L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			String keyword = "Deathtouch";

			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword(keyword);
			}

			list.clear();
			PlayerZone[] zone = getZone("Wren's Run Packmaster");

			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Wolf");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword(keyword);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	};

	public static Command Serra_Avatar                = new Command() {
		private static final long serialVersionUID = -7560281839252561370L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Serra Avatar");

			for(int i = 0; i < list.size(); i++) {
				Card card = list.get(i);
				int n = card.getController().getLife();
				card.setBaseAttack(n);
				card.setBaseDefense(n);
			}// for
		}// execute
	}; // Serra Avatar

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
	}; // Serra Avatar

	public static Command Windwright_Mage             = new Command() {
		private static final long serialVersionUID = 7208941897570511298L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Windwright Mage");

			for(int i = 0; i < list.size(); i++) {
				Card card = list.get(i);

				Player player = card.getController();
				PlayerZone graveyard = AllZone.getZone(
						Constant.Zone.Graveyard, player);

				CardList artifacts = new CardList(
						graveyard.getCards());
				artifacts = artifacts.getType("Artifact");

				if(artifacts.size() > 0) {
					if(!card.getKeyword().contains("Flying")) {
						card.addExtrinsicKeyword("Flying");
					}
				} else {
					// this is tricky, could happen that flying is wrongfully
					// removed... not sure?
							card.removeExtrinsicKeyword("Flying");
				}

			}// for
		}// execute
	}; // Windwright Mage
	
	// Copied from Reach of Branches
	public static Command Sosukes_Summons= new Command() {
		private static final long serialVersionUID = -6316413742244380102L;
		CardList oldSnakes = new CardList();

		public void execute() {
			final Player player = AllZone.Phase.getPlayerTurn();
			final CardList nCard = AllZoneUtil.getPlayerGraveyard(player, "Sosuke's Summons");

			// get all Snakes that player has
			CardList newSnakes = AllZoneUtil.getPlayerTypeInPlay(player, "Snake");
			newSnakes = newSnakes.filter(AllZoneUtil.nonToken);

			// if "Sosuke's Summons" is in graveyard and played a Forest
			if(0 < nCard.size()	&& newSnake(oldSnakes, newSnakes)) {
				SpellAbility ability = new Ability( new Card(), "0" ) {
					@Override
					public void resolve() {
						// return all Summons' to hand
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
						PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
						for(int i = 0; i < nCard.size(); i++) {
							grave.remove(nCard.get(i));
							hand.add(nCard.get(i));
						}
					}// resolve()
				};// SpellAbility
				
				StringBuilder sb = new StringBuilder();
				sb.append("Sosuke's Summons - return card to ").append(player).append("'s hand");
				ability.setStackDescription(sb.toString());
				
				AllZone.Stack.add(ability);
			}// if

			// potential problem: if a snake is bounced to your hand - won't trigger when you play that snake
			oldSnakes.addAll(newSnakes.toArray());
		}// execute

		// check if newList has anything that oldList doesn't have
		boolean newSnake(CardList oldList, CardList newList) {
			// check if a Snake came into play under your control
			for(int i = 0; i < newList.size(); i++)
				if(!oldList.contains(newList.get(i))) return true;

			return false;
		}// newSnake()
	}; // Sosukes Summons
	
	public static Command Baru                        = new Command() {
		private static final long serialVersionUID = 7535910275326543185L;

		CardList                  old              = new CardList();

		public void execute() {
			// get all Forests
			CardList all = new CardList();
			all.addAll(AllZone.Human_Play.getCards());
			all.addAll(AllZone.Computer_Play.getCards());
			CardList current = all.getType("Forest");

			for(int outer = 0; outer < current.size(); outer++) {
				if(old.contains(current.get(outer))) continue;

				final CardList test = all.getName("Baru, Fist of Krosa");
				SpellAbility ability = new Ability(current.get(outer), "0") {
					@Override
					public void resolve() {
						Card c = test.get(0);

						CardList all = new CardList(
								AllZone.getZone(
										Constant.Zone.Play,
										c.getController()).getCards());

						all = all.filter(new CardListFilter() {
							public boolean addCard(Card c) {
								return c.isCreature() && c.isGreen();
							}
						});

						for(int i = 0; i < all.size(); i++) {
							all.get(i).addTempAttackBoost(1);
							all.get(i).addTempDefenseBoost(1);
							all.get(i).addExtrinsicKeyword(
									"Trample");

							final Card c1 = all.get(i);
							AllZone.EndOfTurn.addUntil(new Command() {
								private static final long serialVersionUID = 3659932873866606966L;

								public void execute() {
									c1.addTempAttackBoost(-1);
									c1.addTempDefenseBoost(-1);
									c1.removeExtrinsicKeyword("Trample");
								}
							});
						}// for
					}
				};
				ability.setStackDescription("Baru, Fist of Krosa - creatures get +1/+1 until end of turn.");

				if(!all.getName("Baru, Fist of Krosa").isEmpty()) AllZone.Stack.add(ability);
			}// outer for

			old = current;
		}// execute()
	}; // Baru

	public static Command Wirewood_Hivemaster         = new Command() {
		private static final long serialVersionUID = -6440532066018273862L;

		// Hold old creatures
		CardList                  old              = new CardList();       // Hold old Wirewood Hivemasters
		CardList                  wirewood         = new CardList();

		public void execute() {
			// get all creatures
			CardList current = new CardList();
			current.addAll(AllZone.Human_Play.getCards());
			current.addAll(AllZone.Computer_Play.getCards());
			current = current.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return !c.isToken()
					&& (c.getType().contains("Elf") || c.getKeyword().contains("Changeling"));
				}

			});

			// Holds Wirewood_Hivemaster's in play
			CardList hivemasterList = current.getName("Wirewood Hivemaster");

			// Holds Wirewood_Hivemaster's that are new to play
			CardList newHivemaster = new CardList();

			// Go through the list of Wirewood_Hivemaster's in play
			for(int i = 0; i < hivemasterList.size(); i++) {
				Card c = hivemasterList.get(i);

				// Check to see which Wirewood_Hivemaster's in play are new
				if(!wirewood.contains(c)) {
					newHivemaster.add(c);
					hivemasterList.remove(c);
					i -= 1; // Must do as a card was just removed
				}

				current.remove(c);
			}

			for(int outer = 0; outer < hivemasterList.size(); outer++) {

				final int[] n = new int[1];
				for(int i = 0; i < current.size(); i++) {
					if(!old.contains(current.getCard(i))) {
						n[0]++;
					}
				}

				// Gain life for new Wirewood_Hivemaster
				n[0] += newHivemaster.size();
				final Card crd = hivemasterList.get(outer);

				SpellAbility ability = new Ability(crd, "0") {

					@Override
					public void resolve() {
						for(int i = 0; i < n[0]; i++) {
							CardFactoryUtil.makeToken("Insect",
									"G 1 1 Insect", crd.getController(), "G",
									new String[] {"Creature", "Insect"}, 1, 1, new String[] {""});
						}
					}
				};// SpellAbility
				
				StringBuilder sb = new StringBuilder();
				sb.append(hivemasterList.get(outer).getName()).append(" - ").append(hivemasterList.get(outer).getController());
				sb.append(" puts ").append(n[0]).append(" insect tokens into play.");
				ability.setStackDescription(sb.toString());

				if(n[0] != 0) {
					AllZone.Stack.add(ability);
				}
			}// outer for

			wirewood = hivemasterList;
			wirewood.addAll(newHivemaster.toArray());
			old = current;
		}// execute()
	}; // Wirewood_Hivemaster
	
	public static Command Lighthouse_Chronologist  = new Command() {
		
		private static final long serialVersionUID = 2627513737024865169L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Lighthouse Chronologist");
			
			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 4)
				{
					c.setBaseAttack(1);
					c.setBaseDefense(3);
				}
				else if ( lcs >=4 && lcs < 7 )
				{
					c.setBaseAttack(2);
					c.setBaseDefense(4);
				}
				else
				{
					c.setBaseAttack(3);
					c.setBaseDefense(5);
				}
			}
		}
	};
	
	public static Command Skywatcher_Adept  = new Command() {
		private static final long serialVersionUID = -7568530551652446195L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Skywatcher Adept");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(1);
					c.setBaseDefense(1);
					c.removeIntrinsicKeyword("Flying");
				}
				else if ( lcs >=1 && lcs < 3 )
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
					c.addNonStackingIntrinsicKeyword("Flying");
				}
				else
				{
					c.setBaseAttack(4);
					c.setBaseDefense(2);
					c.addNonStackingIntrinsicKeyword("Flying");
				}
			}
		}
	};
	
	public static Command Caravan_Escort  = new Command() {
		private static final long serialVersionUID = -6996623102170747897L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Caravan Escort");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(1);
					c.setBaseDefense(1);
				}
				else if ( lcs >=1 && lcs < 5 )
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
				}
				else
				{
					c.setBaseAttack(5);
					c.setBaseDefense(5);
					c.addNonStackingIntrinsicKeyword("First Strike");
				}
			}
		}
	};
	
	public static Command Ikiral_Outrider  = new Command() {
		private static final long serialVersionUID = 7835884582225439851L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Ikiral Outrider");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(1);
					c.setBaseDefense(2);
				}
				else if ( lcs >=1 && lcs < 4 )  //levels 1-3
				{
					c.setBaseAttack(2);
					c.setBaseDefense(6);
					c.addNonStackingIntrinsicKeyword("Vigilance");
				}
				else
				{
					c.setBaseAttack(3);
					c.setBaseDefense(10);
					c.addNonStackingIntrinsicKeyword("Vigilance");
				}
			}
		}
	};
	
	public static Command Knight_of_Cliffhaven  = new Command() {
		private static final long serialVersionUID = 3624165284236103054L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Knight of Cliffhaven");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
				}
				else if ( lcs >=1 && lcs < 4 )  //levels 1-3
				{
					c.setBaseAttack(2);
					c.setBaseDefense(3);
					c.addNonStackingIntrinsicKeyword("Flying");
				}
				else
				{
					c.setBaseAttack(4);
					c.setBaseDefense(4);
					c.addNonStackingIntrinsicKeyword("Flying");
					c.addNonStackingIntrinsicKeyword("Vigilance");
				}
			}
		}
	};
	
	public static Command Beastbreaker_of_Bala_Ged  = new Command() {
		private static final long serialVersionUID = -8692202913296782937L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Beastbreaker of Bala Ged");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
				}
				else if ( lcs >=1 && lcs < 4 )  //levels 1-3
				{
					c.setBaseAttack(4);
					c.setBaseDefense(4);
				}
				else
				{
					c.setBaseAttack(6);
					c.setBaseDefense(6);
					c.addNonStackingIntrinsicKeyword("Trample");
				}
			}
		}
	};
	
	public static Command Hada_Spy_Patrol  = new Command() {
		private static final long serialVersionUID = 2343715852240209999L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Hada Spy Patrol");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(1);
					c.setBaseDefense(1);
				}
				else if ( lcs >=1 && lcs < 3 )  //levels 1-2
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
					c.addNonStackingIntrinsicKeyword("Unblockable");
				}
				else
				{
					c.setBaseAttack(3);
					c.setBaseDefense(3);
					c.addNonStackingIntrinsicKeyword("Unblockable");
					c.addNonStackingIntrinsicKeyword("Shroud");
				}
			}
		}
	};
	
	public static Command Halimar_Wavewatch  = new Command() {
		private static final long serialVersionUID = 117755207922239944L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Halimar Wavewatch");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(0);
					c.setBaseDefense(3);
				}
				else if ( lcs >=1 && lcs < 5 )  //levels 1-4
				{
					c.setBaseAttack(0);
					c.setBaseDefense(6);
				}
				else
				{
					c.setBaseAttack(6);
					c.setBaseDefense(6);
					c.addNonStackingIntrinsicKeyword("Islandwalk");
				}
			}
		}
	};
	
	/*
	 * Level up 2 B
	 * LEVEL 1-2 4/3 Deathtouch
	 * LEVEL 3+ 5/4 First strike, deathtouch
	 */
	public static Command Nirkana_Cutthroat  = new Command() {
		private static final long serialVersionUID = 3804539422363462063L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Nirkana Cutthroat");

			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1)
				{
					c.setBaseAttack(3);
					c.setBaseDefense(2);
				}
				else if ( lcs >=1 && lcs < 3 )  //levels 1-2
				{
					c.setBaseAttack(4);
					c.setBaseDefense(3);
					c.addNonStackingIntrinsicKeyword("Deathtouch");
				}
				else
				{
					c.setBaseAttack(5);
					c.setBaseDefense(4);
					c.addNonStackingIntrinsicKeyword("Deathtouch");
					c.addNonStackingIntrinsicKeyword("First Strike");
				}
			}
		}
	};
	
	/*
	 * Level up 4
	 * LEVEL 1-2 3/3
	 * LEVEL 3+ 5/5 CARDNAME can't be blocked except by black creatures.
	 */
	public static Command Zulaport_Enforcer  = new Command() {
		private static final long serialVersionUID = -679141054963080569L;

		public void execute(){
			CardList list = AllZoneUtil.getCardsInPlay("Zulaport Enforcer");

			for (Card c:list) {
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 1) {
					c.setBaseAttack(1);
					c.setBaseDefense(1);
				}
				else if ( lcs >=1 && lcs < 3 ) {  //levels 1-2
					c.setBaseAttack(3);
					c.setBaseDefense(3);
				}
				else {
					c.setBaseAttack(5);
					c.setBaseDefense(5);
					c.addNonStackingIntrinsicKeyword("CARDNAME can't be blocked except by black creatures.");
				}
			}
		}
	};
	
	public static Command Student_of_Warfare 		  = new Command() {
		private static final long serialVersionUID = 2627513737024865169L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Student of Warfare");
			
			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 2)
				{
					c.setBaseDefense(1);
					c.setBaseAttack(1);
					c.removeIntrinsicKeyword("First Strike");
					c.removeIntrinsicKeyword("Double Strike");
				}
				else if ( lcs >=2 && lcs < 7 )
				{
					c.setBaseDefense(3);
					c.setBaseAttack(3);
					c.addNonStackingIntrinsicKeyword("First Strike");
				}
				else
				{
					c.setBaseDefense(4);
					c.setBaseAttack(4);
					c.removeIntrinsicKeyword("First Strike");
					c.addNonStackingIntrinsicKeyword("Double Strike");
				}
			}
		}
	};
	
	public static Command Transcendent_Master		  = new Command() {
		private static final long serialVersionUID = -7568530551652446195L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Transcendent Master");
			
			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				if ( lcs < 6)
				{
					c.setBaseDefense(3);
					c.setBaseAttack(3);
					c.removeIntrinsicKeyword("Lifelink");
					c.removeIntrinsicKeyword("Indestructible");
				}
				else if ( lcs >=6)
				{
					c.setBaseDefense(6);
					c.setBaseAttack(6);
					c.addNonStackingIntrinsicKeyword("Lifelink");
				
					if (lcs >=12) {
						c.setBaseDefense(9);
						c.setBaseAttack(9);
						c.addNonStackingIntrinsicKeyword("Indestructible");
					}
				}
			}
		}
	};

	public static Command Kargan_Dragonlord		  = new Command() {
		private static final long serialVersionUID = -1956268937191206962L;

		public void execute()
		{
			CardList list = AllZoneUtil.getCardsInPlay("Kargan Dragonlord");
			
			for (Card c:list)
			{
				int lcs = c.getCounters(Counters.LEVEL);
				
				if ( lcs < 4)
				{
					c.setBaseAttack(2);
					c.setBaseDefense(2);
					c.removeIntrinsicKeyword("Trample");
					c.removeIntrinsicKeyword("Flying");
				}
				else if ( lcs >=4 && lcs < 8 )
				{
					c.setBaseAttack(4);
					c.setBaseDefense(4);
					c.addNonStackingIntrinsicKeyword("Flying");
					c.removeIntrinsicKeyword("Trample");
				}
				else
				{
					c.setBaseAttack(8);
					c.setBaseDefense(8);
					c.addNonStackingIntrinsicKeyword("Flying");
					c.addNonStackingIntrinsicKeyword("Trample");
				}
			}
		}
	};
	
	
	
	public static Command Dakkon_Blackblade                      = new Command() {

		private static final long serialVersionUID = 6863244333398587274L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Dakkon Blackblade");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countLands(c));
				c.setBaseDefense(c.getBaseAttack());
			}
		}// execute()

		private int countLands(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Land");
			return lands.size();
		}
	};

	public static Command Korlash_Heir_to_Blackblade = new Command() {
		private static final long serialVersionUID = 1791221644995716398L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Korlash, Heir to Blackblade");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countSwamps(c));
				c.setBaseDefense(c.getBaseAttack());
			}
		}// execute()

		private int countSwamps(Card c) {
			CardList swamps = AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Swamp");
			return swamps.size();
		}
	};
	
	public static Command Vampire_Nocturnus                       = new Command() {

		private static final long serialVersionUID = 666334034902503917L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.addSemiPermanentAttackBoost(-2);
				c.addSemiPermanentDefenseBoost(-1);
				c.removeExtrinsicKeyword("Flying");
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Vampire Nocturnus");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Vampire");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					if (CardFactoryUtil.getTopCard(c) != null)
					{
						if((CardFactoryUtil.getTopCard(c)).isBlack()) {
							c.addSemiPermanentAttackBoost(2);
							c.addSemiPermanentDefenseBoost(1);
							c.addExtrinsicKeyword("Flying");
							gloriousAnthemList.add(c);
						}
					}//topCard!=null
				}// for inner
			}// for outer
		}// execute()
	}; // Vampire Nocturnus
	

	public static Command Dauntless_Dourbark          = new Command() {

		private static final long serialVersionUID = -8843070116088984774L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Dauntless Dourbark");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);

				PlayerZone pz = AllZone.getZone(c);
				CardList cl = new CardList(pz.getCards());
				cl = cl.getName("Dauntless Dourbark");
				int dourbarksControlled = cl.size();

				if(hasTreefolk(c) || dourbarksControlled > 1) {
					//may be problematic, should be fine though
					c.setIntrinsicKeyword(new ArrayList<String>());
					c.addIntrinsicKeyword("Trample");
				} else {
					c.removeIntrinsicKeyword("Trample");
				}

				c.setBaseAttack(countTreeForests(c));
				c.setBaseDefense(c.getBaseAttack());
			}
		}// execute()

		private boolean hasTreefolk(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());

			CardList tree = new CardList();
			tree.addAll(play.getCards());

			tree = tree.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return (c.getKeyword().contains("Changeling") || c.getType().contains("Treefolk"))
							&& !c.getName().equals("Dauntless Dourbark");
				}
			});
			if(tree.size() > 0) return true;
			else return false;
		}

		private int countTreeForests(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());
			CardList list = new CardList(play.getCards());
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.getType().contains("Treefolk")
						|| c.getKeyword().contains("Changeling")
						|| c.getType().contains("Forest");
				}
			});

			return list.size();
		}
	};

	public static Command Guul_Draz_Vampire           = new Command() {
		private static final long serialVersionUID = -4252257530318024113L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Guul Draz Vampire");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(oppLess10Life(c)) {
					if(!c.getIntrinsicKeyword().contains(
							"Intimidate")) c.addIntrinsicKeyword("Intimidate");
					c.setBaseAttack(3);
					c.setBaseDefense(2);
				} else {
					c.removeIntrinsicKeyword("Haste");
					c.setBaseAttack(1);
					c.setBaseDefense(1);
				}
			}
		}// execute()

		//does opponent have 10 or less life?
		private boolean oppLess10Life(Card c) {
			Player opp = c.getController().getOpponent();
			return opp.getLife() <= 10;
		}
	};

	public static Command Ruthless_Cullblade          = new Command() {
		private static final long serialVersionUID = 2627513737024865169L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Ruthless Cullblade");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(oppLess10Life(c)) {
					c.setBaseAttack(4);
					c.setBaseDefense(2);
				} else {
					c.setBaseAttack(2);
					c.setBaseDefense(1);
				}
			}
		}// execute()

		//does opponent have 10 or less life?
		private boolean oppLess10Life(Card c) {
			Player opp = c.getController().getOpponent();
			return opp.getLife() <= 10;
		}
	};

	public static Command Bloodghast                  = new Command() {
		private static final long serialVersionUID = -4252257530318024113L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Bloodghast");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(oppLess10Life(c) == true && !c.getIntrinsicKeyword().contains("Haste")) c.addIntrinsicKeyword("Haste");
				else c.removeIntrinsicKeyword("Haste");
			}
		}// execute()

		//does opponent have 10 or less life?
		private boolean oppLess10Life(Card c) {
			Player opp = c.getController().getOpponent();
			return opp.getLife() <= 10;
		}
	};

	public static Command Gaeas_Avenger                   = new Command() {
		private static final long serialVersionUID = 1987511098173387864L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Gaea's Avenger");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countOppArtifacts(c)+1);
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()

		private int countOppArtifacts(Card c) {
			CardList artifacts = AllZoneUtil.getPlayerCardsInPlay(c.getController().getOpponent());
			artifacts = artifacts.filter(AllZoneUtil.artifacts);
			return artifacts.size();
		}
	};

	public static Command People_of_the_Woods                   = new Command() {
		private static final long serialVersionUID = 1987554325573387864L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("People of the Woods");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(1);
				c.setBaseDefense(AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Forest").size());
			}
		}// execute()
	};
	
	public static Command Maraxus_of_Keld = new Command() {
		private static final long serialVersionUID = 7128144927031872127L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Maraxus of Keld");

			for(int i = 0; i < list.size(); i++) {
				
				Card c = list.get(i);
				Player controller = c.getController();
				int pt = countUntapped(controller);
				
				c.setBaseAttack(pt);
				c.setBaseDefense(pt);
			}
		}// execute()
		
		private int countUntapped(Player player) {
			CardList num = AllZoneUtil.getPlayerCardsInPlay(player);
			num = num.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isArtifact() || c.isCreature() || c.isLand();
				}
			});
			num = num.filter(AllZoneUtil.untapped);
			return num.size();
		}
	};
	
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
	
	public static Command Serpent_of_the_Endless_Sea = new Command() {
		private static final long serialVersionUID = 8263339065128877297L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Serpent of the Endless Sea");
			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int pt = AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Island").size();
				c.setBaseAttack(pt);
				c.setBaseDefense(pt);
			}
		}// execute()
	};
	
	public static Command Heedless_One = new Command() {
		private static final long serialVersionUID = -220650457326100804L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Heedless One");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countElves(c));
				c.setBaseDefense(countElves(c));
			}
		}// execute()

		private int countElves(Card c) {
			return AllZoneUtil.getTypeInPlay("Elf").size();
		}
	}; 
	
	public static Command Homarid = new Command() {
		private static final long serialVersionUID = 7156319758035295773L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Homarid");

			for(Card homarid:list) {
				int tide = homarid.getCounters(Counters.TIDE);
				if(tide == 4) {
					homarid.setCounter(Counters.TIDE, 0, true);
				}
				if(tide == 1) {
					homarid.setBaseAttack(1);
					homarid.setBaseDefense(1);
				}
				else if (tide == 3) {
					homarid.setBaseAttack(3);
					homarid.setBaseDefense(3);
				}
				else {
					homarid.setBaseAttack(2);
					homarid.setBaseDefense(2);
				}
			}
		}// execute()
	};

	public static Command Omnath = new Command() {
		private static final long serialVersionUID = -22045167326100804L;

		public void execute() {
			// todo: when computer manapool is made, this needs to be fixed
			CardList list = AllZoneUtil.getCardsInPlay("Omnath, Locus of Mana");
			int bonus = AllZone.ManaPool.getAmountOfColor(Constant.Color.Green);
			for(Card c: list){
				c.setBaseAttack(bonus + 1);
				c.setBaseDefense(bonus + 1);
			}
		}// execute()
	};
	
	public static Command Dauntless_Escort = new Command() {
		private static final long serialVersionUID = -2201201455269804L;
		CardList  old = new CardList();	
		public void execute() {

			// Human Activates Dauntless Escort
	        PlayerZone PlayerPlayZone = AllZone.getZone(Constant.Zone.Play,AllZone.HumanPlayer);
	        CardList PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
	        PlayerCreatureList = PlayerCreatureList.getType("Creature");
			PlayerZone opponentPlayZone = AllZone.getZone(Constant.Zone.Play,AllZone.ComputerPlayer);
	        CardList opponentCreatureList = new CardList(opponentPlayZone.getCards());
	        opponentCreatureList = opponentCreatureList.getType("Creature");
			if(Phase.Sac_Dauntless_Escort == true) {
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
	        PlayerPlayZone = AllZone.getZone(Constant.Zone.Play,AllZone.ComputerPlayer);
	        PlayerCreatureList = new CardList(PlayerPlayZone.getCards());
	        PlayerCreatureList = PlayerCreatureList.getType("Creature");
			opponentPlayZone = AllZone.getZone(Constant.Zone.Play,AllZone.HumanPlayer);
	        opponentCreatureList = new CardList(opponentPlayZone.getCards());
	        opponentCreatureList = opponentCreatureList.getType("Creature");
			if(Phase.Sac_Dauntless_Escort_Comp == true) {
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

	public static Command Vexing_Beetle               = new Command() {

		private static final long serialVersionUID = 4599996155083227853L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Vexing Beetle");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(!oppHasCreature(c)) {
					c.setBaseAttack(6);
					c.setBaseDefense(6);
				} else {
					c.setBaseAttack(3);
					c.setBaseDefense(3);
				}
			}
		}// execute()

		private boolean oppHasCreature(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play,
					c.getController().getOpponent());

			CardList creats = new CardList();
			creats.addAll(play.getCards());

			creats = creats.getType("Creature");
			if(creats.size() > 0) return true;
			else return false;
		}
	};

	
	public static Command Champions_Drake = new Command() {
		private static final long serialVersionUID = 8076177362922156784L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Champion's Drake");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(hasThreeLevels(c)) {
					c.setBaseAttack(4);
					c.setBaseDefense(4);
				} else {
					c.setBaseAttack(1);
					c.setBaseDefense(1);
				}
			}
		}// execute()

		private boolean hasThreeLevels(Card c) {
			CardList levels = AllZoneUtil.getPlayerCardsInPlay(c.getController());
			levels = levels.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isCreature() && c.getCounters(Counters.LEVEL) >= 3;
				}
			});
			return levels.size() > 0;
		}
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
					Constant.Zone.Play, c.getController());

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

	
	public static Command Bant_Sureblade              = new Command() {

		private static final long serialVersionUID = 1987511205573387864L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Bant Sureblade");

			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {

					Card c = list.get(i);
					if(CardFactoryUtil.controlsAnotherMulticoloredPermanent(c)) {
						c.setBaseAttack(3);
						c.setBaseDefense(2);
						if(!c.getIntrinsicKeyword().contains(
								"First Strike")) c.addIntrinsicKeyword("First Strike");
					} else {
						c.setBaseAttack(2);
						c.setBaseDefense(1);
						if(c.getIntrinsicKeyword().contains(
								"First Strike")) c.removeIntrinsicKeyword("First Strike");
					}

				}
			}
		}// execute()

	}; //Bant_Sureblade

	public static Command Esper_Stormblade            = new Command() {

		private static final long serialVersionUID = 1799759665613654307L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Esper Stormblade");

			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {

					Card c = list.get(i);
					if(CardFactoryUtil.controlsAnotherMulticoloredPermanent(c)) {
						c.setBaseAttack(3);
						c.setBaseDefense(2);
						if(!c.getIntrinsicKeyword().contains(
								"Flying")) c.addIntrinsicKeyword("Flying");
					} else {
						c.setBaseAttack(2);
						c.setBaseDefense(1);
						if(c.getIntrinsicKeyword().contains(
								"Flying")) c.removeIntrinsicKeyword("Flying");
					}

				}
			}
		}// execute()

	}; //Esper_Stormblade

	public static Command Grixis_Grimblade            = new Command() {

		private static final long serialVersionUID = 5895665460018262987L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Grixis Grimblade");

			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {

					Card c = list.get(i);
					if(CardFactoryUtil.controlsAnotherMulticoloredPermanent(c)) {
						c.setBaseAttack(3);
						c.setBaseDefense(2);
						if(!c.getIntrinsicKeyword().contains(
								"Deathtouch")) c.addIntrinsicKeyword("Deathtouch");
					} else {
						c.setBaseAttack(2);
						c.setBaseDefense(1);
						if(c.getIntrinsicKeyword().contains(
								"Deathtouch")) c.removeIntrinsicKeyword("Deathtouch");
					}

				}
			}
		}// execute()

	}; //Grixis_Grimblade

	public static Command Jund_Hackblade              = new Command() {

		private static final long serialVersionUID = -4386978825641506610L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Jund Hackblade");

			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {

					Card c = list.get(i);
					if(CardFactoryUtil.controlsAnotherMulticoloredPermanent(c)) {
						c.setBaseAttack(3);
						c.setBaseDefense(2);
						if(!c.getIntrinsicKeyword().contains(
								"Haste")) c.addIntrinsicKeyword("Haste");
					} else {
						c.setBaseAttack(2);
						c.setBaseDefense(1);
						if(c.getIntrinsicKeyword().contains(
								"Haste")) c.removeIntrinsicKeyword("Haste");
					}

				}
			}
		}// execute()

	}; //Jund_Hackblade
	
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
	
	
	public static Command Broodwarden      = new Command() {

		private static final long serialVersionUID = -9033688979680507210L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {

			CardList cList = gloriousAnthemList;
			Card c;

			for(int i = 0; i < cList.size(); i++) {
				c = cList.get(i);
				c.addSemiPermanentAttackBoost(-2);
				c.addSemiPermanentDefenseBoost(-1);
			}
			cList.clear();
			PlayerZone[] zone = getZone("Broodwarden");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.filter(new CardListFilter()
				{
					public boolean addCard(Card c)
					{
						return (c.getType().contains("Eldrazi") && c.getType().contains("Spawn") ) || c.getKeyword().contains("Changeling");
					}
				});

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addSemiPermanentAttackBoost(2);
					c.addSemiPermanentDefenseBoost(1);
					gloriousAnthemList.add(c);

				} // for
			} // for

		}// execute()

	}; //Broodwarden

	public static Command Covetous_Dragon             = new Command() {
		private static final long serialVersionUID = -8898010588711890705L;

		int                       artifacts        = 0;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay("Covetous Dragon");

			for(int i = 0; i < creature.size(); i++) {
				Card c = creature.get(i);
				artifacts = countArtifacts(c);
				if(artifacts == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()

		private int countArtifacts(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());
			CardList artifacts = new CardList(play.getCards());
			artifacts = artifacts.getType("Artifact");
			return artifacts.size();
		}
	};
	
	public static Command Phylactery_Lich             = new Command() {

		private static final long serialVersionUID = -1606115081917467754L;
		int                       artifacts        = 0;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay("Phylactery Lich");

			for(int i = 0; i < creature.size(); i++) {
				Card c = creature.get(i);
				artifacts = countArtifacts(c);
				if(artifacts == 0 && c.getFinishedEnteringBF()) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()

		private int countArtifacts(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());
			CardList artifacts = new CardList(play.getCards());
			artifacts = artifacts.filter(new CardListFilter()
			{
				public boolean addCard(Card crd)
				{
					return crd.isArtifact() && crd.getCounters(Counters.PHYLACTERY) > 0;
				}
			});
			return artifacts.size();
		}
	};

	public static Command Tethered_Griffin            = new Command() {

		private static final long serialVersionUID = 572286202401670996L;
		int                       enchantments     = 0;

		public void execute() {
			CardList creature = AllZoneUtil.getCardsInPlay("Tethered Griffin");

			for(int i = 0; i < creature.size(); i++) {
				Card c = creature.get(i);
				enchantments = countEnchantments(c);
				if(enchantments == 0) {
					AllZone.GameAction.sacrifice(c);
				}
			}

		}//execute()

		private int countEnchantments(Card c) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, c.getController());
			CardList enchantments = new CardList(play.getCards());
			enchantments = enchantments.getType("Enchantment");
			return enchantments.size();
		}


	};
	
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
	};

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
					Constant.Zone.Play, c.getController());
			CardList islands = new CardList(play.getCards());
			islands = islands.getType("Island");
			return islands.size();
		}

	};
	
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
	};
	
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
	};
	
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
	};

	
	public static Command Master_of_Etherium          = new Command() {
		private static final long serialVersionUID   = -5406532269375480827L;
		
		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay();
			list = list.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.getName().equals("Master of Etherium") || c.getName().equals("Broodstar");
                }
            });

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				CardList arts = AllZoneUtil.getPlayerCardsInPlay(c.getController());
				arts = arts.filter(AllZoneUtil.artifacts);
				c.setBaseAttack(arts.size());
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()
	}; // Master of Etherium + Broodstar

	public static Command Loxodon_Punisher            = new Command() {

		private static final long serialVersionUID = -7746134566580289667L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Loxodon Punisher");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(2 + countEquipment(c) * 2);
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()

		private int countEquipment(Card c) {
			CardList equipment = new CardList(
					c.getEquippedBy().toArray());
			return equipment.size();
		}
	};
	
	
    public static Command Goblin_Gaveleer            = new Command() {
		
        private static final long serialVersionUID = -9039509574117844271L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Goblin Gaveleer");
			
			for (int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(1 + countEquipment(c) * 2);
			}
			
		}// execute()
		
		private int countEquipment(Card c) {
			CardList equipment = new CardList(
					c.getEquippedBy().toArray());
			return equipment.size();
		}
	};// Goblin_Gaveleer

	public static Command Uril                        = new Command() {
		private static final long serialVersionUID = 8168928048322850517L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Uril, the Miststalker");

			if(list.size() > 0) {
				Card c = list.get(0);
				c.setBaseAttack(5 + (countAuras(c) * 2));
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()

		private int countAuras(Card c) {
			CardList auras = new CardList(
					c.getEnchantedBy().toArray());
			return auras.size();
		}
	};


	public static Command Kithkin_Rabble              = new Command() {
		private static final long serialVersionUID = 6686690505949642328L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Kithkin Rabble");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				CardList white = AllZoneUtil.getPlayerCardsInPlay(c.getController());
				white = white.filter(AllZoneUtil.white);
				c.setBaseAttack(white.size());
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()
	};

	public static Command Crowd_of_Cinders            = new Command() {
		private static final long serialVersionUID = 6686690505949642328L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Crowd of Cinders");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				CardList black = AllZoneUtil.getPlayerCardsInPlay(c.getController());
				black = black.filter(AllZoneUtil.black);
				c.setBaseAttack(black.size());
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()
	}; // Crowd of Cinders

	public static Command Faerie_Swarm                = new Command() {
		private static final long serialVersionUID = 6686690505949642328L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Faerie Swarm");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countBluePermanents(c));
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()

		private int countBluePermanents(Card c) {
			CardList blue = AllZoneUtil.getPlayerCardsInPlay(c.getController());
			blue = blue.filter(AllZoneUtil.blue);
			return blue.size();
		}
	}; // Faerie Swarm

	public static Command Drove_of_Elves              = new Command() {
		private static final long serialVersionUID = 6686690505949642328L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Drove of Elves");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				CardList green = AllZoneUtil.getPlayerCardsInPlay(c.getController());
				green = green.filter(AllZoneUtil.green);
				c.setBaseAttack(green.size());
				c.setBaseDefense(green.size());
			}

		}// execute()
	}; // Drove of Elves


	public static Command Multani_Maro_Sorcerer       = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Multani, Maro-Sorcerer");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countHands());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countHands() {
			PlayerZone compHand = AllZone.getZone(
					Constant.Zone.Hand, AllZone.ComputerPlayer);
			PlayerZone humHand = AllZone.getZone(
					Constant.Zone.Hand, AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(compHand.getCards());
			list.addAll(humHand.getCards());
			return list.size();
		}

	}; //Multani, Maro-Sorcerer

	public static Command Molimo_Maro_Sorcerer        = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Molimo, Maro-Sorcerer");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(AllZoneUtil.getPlayerLandsInPlay(c.getController()).size());
				c.setBaseDefense(c.getBaseAttack());

			}
		}

	}; //Molimo, Maro-Sorcerer

	public static Command Maro                        = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Maro");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(AllZoneUtil.getPlayerHand(c.getController()).size());
				c.setBaseDefense(c.getBaseAttack());
			}
		}
	}; //Maro
	
	public static Command Masumaro_First_to_Live                       = new Command() {
		private static final long serialVersionUID = -8778922687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Masumaro, First to Live");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int k = 0;
				if(c.getController().equals(
						AllZone.HumanPlayer)) {
					k = countHand_Human();
				} else k = countHand_Computer();
				c.setBaseAttack(2*k);
				c.setBaseDefense(2*k);
			}
		}

		private int countHand_Human() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

		private int countHand_Computer() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.ComputerPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

	}; //Masumaro, first to live
	
	
	public static Command Adamaro_First_to_Desire                       = new Command() {
		private static final long serialVersionUID = -8778912687347191965L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Adamaro, First to Desire");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int k = 0;
				if(c.getController().equals(
						AllZone.HumanPlayer)) {
					k = countHand_Computer();
				} else k = countHand_Human();
				c.setBaseAttack(k);
				c.setBaseDefense(k);
			}
		}

		private int countHand_Human() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

		private int countHand_Computer() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.ComputerPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

	}; //Adamaro, first to desire

	public static Command Overbeing_of_Myth           = new Command() {
		private static final long serialVersionUID = -2250795040532050455L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Overbeing of Myth");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int k = 0;
				if(c.getController().equals(
						AllZone.HumanPlayer)) {
					k = countHand_Human();
				} else k = countHand_Computer();
				c.setBaseAttack(k);
				c.setBaseDefense(k);
			}
		}

		private int countHand_Human() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

		private int countHand_Computer() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.ComputerPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

	}; //overbeing of myth

	public static Command Guul_Draz_Specter           = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Guul Draz Specter");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int k = 0;
				if(c.getController().equals(
						AllZone.ComputerPlayer)) {
					k = countHand_Human();
				} else k = countHand_Computer();
				if(k == 0) {
					c.setBaseAttack(5);
					c.setBaseDefense(5);
				} else {
					c.setBaseAttack(2);
					c.setBaseDefense(2);
				}
			}
		}

		private int countHand_Human() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

		private int countHand_Computer() {
			PlayerZone Play = AllZone.getZone(
					Constant.Zone.Hand, AllZone.ComputerPlayer);
			CardList list = new CardList();
			list.addAll(Play.getCards());
			return list.size();
		}

	}; //Guul Draz Specter

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
	
	public static Command Mortivore                   = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Mortivore");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCreatures());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countCreatures() {
			CardList list = AllZoneUtil.getCardsInGraveyard();
			list = list.filter(AllZoneUtil.creatures);
			return list.size();
		}

	}; //Mortivore

	public static Command Cognivore                   = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Cognivore");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCreatures());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countCreatures() {
			CardList list = AllZoneUtil.getCardsInGraveyard();
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isInstant();
				}
			});
			return list.size();
		}

	}; //Cognivore

	public static Command Cantivore                   = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Cantivore");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCreatures());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countCreatures() {
			CardList list = AllZoneUtil.getCardsInGraveyard();
			list = list.filter(AllZoneUtil.enchantments);
			return list.size();
		}

	}; //Cantivore

	public static Command Lhurgoyf                    = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Lhurgoyf");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCreatures());
				c.setBaseDefense(c.getBaseAttack() + 1);
			}
		}

		private int countCreatures() {
			CardList list = AllZoneUtil.getCardsInGraveyard();
			list = list.filter(AllZoneUtil.creatures);
			return list.size();
		}

	}; //Lhurgoyf

	public static Command Svogthos_the_Restless_Tomb  = new Command() {
		private static final long serialVersionUID = -8778902687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Svogthos, the Restless Tomb");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int x = 0;
				if(c.getController() == AllZone.HumanPlayer) x = countCreatures_Hum();
				else x = countCreatures_Comp();
				if(c.isCreature()) {
					c.setBaseAttack(x);
					c.setBaseDefense(x);
				}
			}
		}

		private int countCreatures_Comp() {
			PlayerZone compGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.ComputerPlayer);
			CardList list = new CardList();
			list.addAll(compGrave.getCards());
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isCreature();
				}
			});
			return list.size();
		}

		private int countCreatures_Hum() {
			PlayerZone humGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(humGrave.getCards());
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isCreature();
				}
			});
			return list.size();
		}

	}; //Svogthos, the Restless Tomb

	public static Command Nightmare                   = new Command() {
		private static final long serialVersionUID = 1987511205573387864L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Nightmare");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countSwamps(c));
				c.setBaseDefense(c.getBaseAttack());
			}

		}// execute()

		private int countSwamps(Card c) {
			CardList swamps = AllZoneUtil.getPlayerTypeInPlay(c.getController(), "Swamp");
			return swamps.size();
		}
	};

	public static Command Aven_Trailblazer            = new Command() {
		private static final long serialVersionUID = 2731050781896531776L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Aven Trailblazer");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseDefense(CardFactoryUtil.countBasicLandTypes(c.getController()));
			}

		}// execute()
	};
	
	public static Command Matca_Rioters            = new Command() {
		private static final long serialVersionUID = 1090204321481353143L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Matca Rioters");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseDefense(CardFactoryUtil.countBasicLandTypes(c.getController()));
				c.setBaseAttack(c.getBaseDefense());
			}

		}// execute()
	};

	public static Command Rakdos_Pit_Dragon           = new Command() {
		private static final long serialVersionUID = -8778900687347191964L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Rakdos Pit Dragon");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				if(hellbent(c.getController())
						&& !c.getIntrinsicKeyword().contains(
								"Double Strike")) c.addIntrinsicKeyword("Double Strike");
				else if(!hellbent(c.getController())
						&& c.getIntrinsicKeyword().contains(
								"Double Strike")) c.removeIntrinsicKeyword("Double Strike");
			}
		}

		private boolean hellbent(Player player) {
			PlayerZone hand = AllZone.getZone(
					Constant.Zone.Hand, player);

			CardList list = new CardList();
			list.addAll(hand.getCards());

			return list.size() == 0;
		}

	}; //Rakdos Pit Dragon

	public static Command Nyxathid                    = new Command() {

		private static final long serialVersionUID = -8778900687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Nyxathid");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				int pt = 7 - countCards(c.getController().getOpponent());
				c.setBaseAttack(pt);
				c.setBaseDefense(pt);
			}
		}

		private int countCards(Player player) {
			PlayerZone hand = AllZone.getZone(
					Constant.Zone.Hand, player);

			CardList list = new CardList();
			list.addAll(hand.getCards());

			return list.size();
		}

	}; //Nyxathid


	public static Command Lord_of_Extinction          = new Command() {
		private static final long serialVersionUID = -8778900687347191964L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Lord of Extinction");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCards());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countCards() {
			PlayerZone compGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.ComputerPlayer);
			PlayerZone humGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(compGrave.getCards());
			list.addAll(humGrave.getCards());

			return list.size();
		}

	}; //Lord of Extinction


	public static Command Terravore                   = new Command() {
		private static final long serialVersionUID = -7848248012651247059L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Terravore");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countLands());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countLands() {
			PlayerZone compGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.ComputerPlayer);
			PlayerZone humGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(compGrave.getCards());
			list.addAll(humGrave.getCards());

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isLand();
				}
			});

			return list.size();
		}

	}; //terravore

	public static Command Magnivore                   = new Command() {

		private static final long serialVersionUID = 6569701555927133445L;

		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Magnivore");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countSorcs());
				c.setBaseDefense(c.getBaseAttack());
			}
		}

		private int countSorcs() {
			PlayerZone compGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.ComputerPlayer);
			PlayerZone humGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(compGrave.getCards());
			list.addAll(humGrave.getCards());

			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isSorcery();
				}
			});

			return list.size();
		}

	}; //magnivore

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
			PlayerZone compGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.ComputerPlayer);
			PlayerZone humGrave = AllZone.getZone(
					Constant.Zone.Graveyard,
					AllZone.HumanPlayer);
			CardList list = new CardList();
			list.addAll(compGrave.getCards());
			list.addAll(humGrave.getCards());

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
			if(Phase.GameBegins == 1) {
			// reset creatures
			removeSwampwalk(old);

			if(isInGrave(AllZone.ComputerPlayer)) addSwampwalk(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addSwampwalk(AllZone.HumanPlayer);
			}
		}// execute()

		void addSwampwalk(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
					Constant.Zone.Play, player);
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
			if(Phase.GameBegins == 1) {
			// reset creatures
			removeFirstStrike(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFirstStrike(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFirstStrike(AllZone.HumanPlayer);
			}
		}// execute()

		void addFirstStrike(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
					Constant.Zone.Play, player);
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
			if(Phase.GameBegins == 1) {
			// reset creatures
			removeHaste(old);

			if(isInGrave(AllZone.ComputerPlayer)) addHaste(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addHaste(AllZone.HumanPlayer);
			}
		}// execute()

		void addHaste(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
					Constant.Zone.Play, player);
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
			if(Phase.GameBegins == 1) {
			// reset creatures
			removeFlying(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFlying(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFlying(AllZone.HumanPlayer);
			}
		}// execute()

		void addFlying(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
					Constant.Zone.Play, player);
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
			if(Phase.GameBegins == 1) {
			// reset creatures
			removeTrample(old);

			if(isInGrave(AllZone.ComputerPlayer)) addTrample(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addTrample(AllZone.HumanPlayer);
			}
		}// execute()

		void addTrample(Player player) {
			next.clear();
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
					Constant.Zone.Play, player);
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
	
	/**
	public static Command Valor                       = new Command() {
		
		private static final long serialVersionUID = 1664342157638418864L;

		CardList                  old              = new CardList();

		public void execute() {
			// reset creatures
			removeFirstStrike(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFirstStrike(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFirstStrike(AllZone.HumanPlayer);
		}// execute()

		void addFirstStrike(Player player) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList list = new CardList(play.getCards());
			list = list.getType("Creature");

			// add creatures to "old" or previous list of creatures
			old.addAll(list.toArray());

			addFirstStrike(list);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Plains");


			if(!list.containsName("Valor") || lands.size() == 0) return false;
			else return true;
		}

		void removeFirstStrike(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).removeExtrinsicKeyword(
						"First Strike");
		}

		void addFirstStrike(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).addExtrinsicKeyword("First Strike");
		}
	}; // Valor

	public static Command Anger                       = new Command() {
		private static final long serialVersionUID = -8436803135298267370L;

		CardList                  old              = new CardList();

		public void execute() {
			// reset creatures
			removeHaste(old);

			if(isInGrave(AllZone.ComputerPlayer)) addHaste(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addHaste(AllZone.HumanPlayer);
		}// execute()

		void addHaste(Player player) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList list = new CardList(play.getCards());
			list = list.getType("Creature");

			// add creatures to "old" or previous list of creatures
			old.addAll(list.toArray());

			addHaste(list);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Mountain");


			if(!list.containsName("Anger") || lands.size() == 0) return false;
			else return true;
		}

		void removeHaste(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).removeExtrinsicKeyword("Haste");
		}

		void addHaste(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).addExtrinsicKeyword("Haste");
		}
	}; // Anger

	public static Command Wonder                      = new Command() {
		private static final long serialVersionUID = 8346741995447241353L;

		CardList                  old              = new CardList();

		public void execute() {
			// reset creatures
			removeFlying(old);

			if(isInGrave(AllZone.ComputerPlayer)) addFlying(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addFlying(AllZone.HumanPlayer);
		}// execute()

		void addFlying(Player player) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList list = new CardList(play.getCards());
			list = list.getType("Creature");

			// add creatures to "old" or previous list of creatures
			old.addAll(list.toArray());

			addFlying(list);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Island");


			if(!list.containsName("Wonder") || lands.size() == 0) return false;
			else return true;
		}

		void removeFlying(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).removeExtrinsicKeyword("Flying");
		}

		void addFlying(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).addExtrinsicKeyword("Flying");
		}
	}; // Wonder

	public static Command Brawn                       = new Command() {
		private static final long serialVersionUID = -8467814700545847505L;

		CardList                  old              = new CardList();

		public void execute() {
			// reset creatures
			removeTrample(old);

			if(isInGrave(AllZone.ComputerPlayer)) addTrample(AllZone.ComputerPlayer);

			if(isInGrave(AllZone.HumanPlayer)) addTrample(AllZone.HumanPlayer);
		}// execute()

		void addTrample(Player player) {
			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList list = new CardList(play.getCards());
			list = list.getType("Creature");

			// add creatures to "old" or previous list of creatures
			old.addAll(list.toArray());

			addTrample(list);
		}

		boolean isInGrave(Player player) {
			PlayerZone grave = AllZone.getZone(
					Constant.Zone.Graveyard, player);
			CardList list = new CardList(grave.getCards());

			PlayerZone play = AllZone.getZone(
					Constant.Zone.Play, player);
			CardList lands = new CardList(play.getCards());
			lands = lands.getType("Forest");


			if(!list.containsName("Brawn") || lands.size() == 0) return false;
			else return true;
		}

		void removeTrample(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).removeExtrinsicKeyword("Trample");
		}

		void addTrample(CardList list) {
			for(int i = 0; i < list.size(); i++)
				list.get(i).addExtrinsicKeyword("Trample");
		}
	}; // Brawn
**/
	
	public static Command Crucible_of_Fire            = new Command() {
		private static final long serialVersionUID   = 3620025773676030026L;

		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.addSemiPermanentAttackBoost(-3);
				c.addSemiPermanentDefenseBoost(-3);
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Crucible of Fire");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Dragon");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addSemiPermanentAttackBoost(3);
					c.addSemiPermanentDefenseBoost(3);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	}; // Crucible of Fire
	
	public static Command Time_of_Heroes        = new Command() {
		private static final long serialVersionUID = 2490232366851007733L;
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

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Time of Heroes");

			// for each zone found add +1/+1 to each card
			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.filter(new CardListFilter()
				{
					public boolean addCard(Card crd)
					{
						return crd.getCounters(Counters.LEVEL) > 0;
					}
				});

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addSemiPermanentAttackBoost(2);
					c.addSemiPermanentDefenseBoost(2);

					gloriousAnthemList.add(c);
				}// for inner
			}// for outer
		}// execute()
	}; // Time of Heroes

	public static Command Beastmaster_Ascension       = new Command() {

		private static final long serialVersionUID   = -3455855754974451348L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.addSemiPermanentAttackBoost(-5);
				c.addSemiPermanentDefenseBoost(-5);
			}

			// add +1/+1 to cards
			list.clear();
			
			CardList cl = AllZoneUtil.getCardsInPlay("Beastmaster Ascension");

			for(int i = 0; i < cl.size(); i++) {
				Player player = cl.get(i).getController();
				PlayerZone play = AllZone.getZone(
						Constant.Zone.Play, player);

				CardList creature = new CardList(play.getCards());
				creature = creature.getType("Creature");

				for(int j = 0; j < creature.size(); j++) {
					if(cl.get(i).getCounters(Counters.QUEST) >= 7) {
						c = creature.get(j);
						c.addSemiPermanentAttackBoost(5);
						c.addSemiPermanentDefenseBoost(5);
						gloriousAnthemList.add(c);
					}


				}// for inner
			}// for outer
		}// execute()
	}; // Beastmaster Ascension

	public static Command Spidersilk_Armor            = new Command() {

		private static final long serialVersionUID   = -1151510755451414602L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword("Reach");
				c.addSemiPermanentDefenseBoost(-1);
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Spidersilk Armor");

			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Creature");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword("Reach");
					c.addSemiPermanentDefenseBoost(1);
					gloriousAnthemList.add(c);


				}// for inner
			}// for outer
		}// execute()
	}; // Spidersilk Armor

	
	public static Command Eldrazi_Monument            = new Command() {

		private static final long serialVersionUID   = -3591110487441151195L;
		CardList                  gloriousAnthemList = new CardList();

		public void execute() {
			CardList list = gloriousAnthemList;
			Card c;
			// reset all cards in list - aka "old" cards
			for(int i = 0; i < list.size(); i++) {
				c = list.get(i);
				c.removeExtrinsicKeyword("Flying");
				c.removeExtrinsicKeyword("Indestructible");
				c.addSemiPermanentAttackBoost(-1);
				c.addSemiPermanentDefenseBoost(-1);
			}

			// add +1/+1 to cards
			list.clear();
			PlayerZone[] zone = getZone("Eldrazi Monument");

			for(int outer = 0; outer < zone.length; outer++) {
				CardList creature = new CardList(
						zone[outer].getCards());
				creature = creature.getType("Creature");

				for(int i = 0; i < creature.size(); i++) {
					c = creature.get(i);
					c.addExtrinsicKeyword("Flying");
					c.addExtrinsicKeyword("Indestructible");
					c.addSemiPermanentAttackBoost(1);
					c.addSemiPermanentDefenseBoost(1);
					gloriousAnthemList.add(c);


				}// for inner
			}// for outer
		}// execute()
	}; // Eldrazi_Monument

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
				// CardList creature = new CardList(zone[outer].getCards());
				CardList creature = new CardList();
				creature.addAll(AllZone.Human_Play.getCards());
				creature.addAll(AllZone.Computer_Play.getCards());
				creature = creature.getType("Creature");

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
	
	public static Command Nut_Collector = new Command()
	{
		private static final long serialVersionUID = 4873843480453112825L;
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
			PlayerZone[] zone = getZone("Nut Collector");
			
			
			// for each zone found add +2/+2 to each vanilla card
			for(int outer = 0; outer < zone.length; outer++) {
				PlayerZone z = zone[outer];
				Player player = z.getPlayer();
				
				if (player.hasThreshold())
				{
					CardList creature = AllZoneUtil.getTypeInPlay("Squirrel");
					
					for(int i = 0; i < creature.size(); i++) {
						c = creature.get(i);
						c.addSemiPermanentAttackBoost(2);
						c.addSemiPermanentDefenseBoost(2);
	
						gloriousAnthemList.add(c);
					}// for inner
				}//if Threshold
			}// for outer
			
		}//execute
	}; //nut_collector
	
	
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

			PlayerZone cplay = AllZone.Computer_Play;
			PlayerZone hplay = AllZone.Human_Play;

			CardList cl = new CardList();
			cl.addAll(cplay.getCards());
			cl.addAll(hplay.getCards());
			cl = cl.getName("Iona, Shield of Emeria");

			for(int i = 0; i < cl.size(); i++) {
				final Card crd = cl.get(i);
				Player controller = cl.get(i).getController();
				Player opp = controller.getOpponent();

				CardList spells = new CardList();
				PlayerZone grave = AllZone.getZone(
						Constant.Zone.Graveyard, opp);
				PlayerZone hand = AllZone.getZone(
						Constant.Zone.Hand, opp);

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
	}; //

	public static Command Keldon_Warlord                   = new Command() {
		private static final long serialVersionUID = 3804539422363462063L;
		
		/*
		 * Keldon Warlord's power and toughness are each equal to the number
		 * of non-Wall creatures you control.
		 */
		public void execute() {
			// get all creatures
			CardList list = AllZoneUtil.getCardsInPlay("Keldon Warlord");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				c.setBaseAttack(countCreatures(c));
				c.setBaseDefense(c.getNetAttack());
			}

		}// execute()

		private int countCreatures(Card c) {
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
			CardList creatures = new CardList(play.getCards());
			creatures = creatures.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isCreature() && !c.isWall();
				}
			});
			return creatures.size();
		}
	};
	
	public static Command Soulsurge_Elemental                   = new Command() {
		private static final long serialVersionUID = 8607200838396348507L;

		public void execute() {
			CardList list = AllZoneUtil.getCardsInPlay("Soulsurge Elemental");

			for(int i = 0; i < list.size(); i++) {
				Card c = list.get(i);
				//c.setBaseAttack(countCreatures(c));
				c.setBaseAttack(AllZoneUtil.getCreaturesInPlay(c.getController()).size());
				c.setBaseDefense(1);
			}
		}// execute()
	};
	
	public static void Elvish_Vanguard(Card c) {
		final Card crd = c;

		Ability ability = new Ability(c, "0") {
			@Override
			public void resolve() {
				crd.addCounter(Counters.P1P1, 1);
			}
		};// Ability
		
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName()).append(" - gets a +1/+1 counter.");
		ability.setStackDescription(sb.toString());
		
		AllZone.Stack.add(ability);
	}


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
		
		commands.put("Adamaro_First_to_Desire", Adamaro_First_to_Desire);
		commands.put("Ajani_Avatar_Token", Ajani_Avatar_Token);
		commands.put("Akromas_Memorial", Akromas_Memorial);
		commands.put("Angry_Mob", Angry_Mob);
		commands.put("Aven_Trailblazer", Aven_Trailblazer);
		
		commands.put("Bant_Sureblade", Bant_Sureblade);
		commands.put("Beastbreaker_of_Bala_Ged", Beastbreaker_of_Bala_Ged);
		commands.put("Beastmaster_Ascension", Beastmaster_Ascension);
		commands.put("Bloodghast", Bloodghast);
		commands.put("Broodwarden", Broodwarden);
		
		commands.put("Cantivore", Cantivore);
		commands.put("Caravan_Escort", Caravan_Escort);
		commands.put("Champions_Drake", Champions_Drake);
		commands.put("Coat_of_Arms", Coat_of_Arms);	
		commands.put("Cognivore", Cognivore);
		commands.put("Conspiracy", Conspiracy);
		commands.put("Covetous_Dragon", Covetous_Dragon);
		commands.put("Crowd_of_Cinders", Crowd_of_Cinders);
		commands.put("Crucible_of_Fire", Crucible_of_Fire);
		
		commands.put("Dakkon_Blackblade", Dakkon_Blackblade);
		commands.put("Dauntless_Dourbark", Dauntless_Dourbark);
		commands.put("Drove_of_Elves", Drove_of_Elves);
		
		commands.put("Eldrazi_Monument", Eldrazi_Monument);
		commands.put("Elspeth_Emblem", Elspeth_Emblem);
		commands.put("Emperor_Crocodile", Emperor_Crocodile);
		commands.put("Esper_Stormblade", Esper_Stormblade);
		
		commands.put("Faerie_Swarm", Faerie_Swarm);
		
		commands.put("Gaddock_Teeg", Gaddock_Teeg);
		commands.put("Gaeas_Avenger", Gaeas_Avenger);
		commands.put("Gemhide_Sliver", Gemhide_Sliver);
		commands.put("Goblin_Assault", Goblin_Assault);
		commands.put("Goblin_Gaveleer", Goblin_Gaveleer);
		commands.put("Grixis_Grimblade", Grixis_Grimblade);
		commands.put("Guul_Draz_Specter", Guul_Draz_Specter);
		commands.put("Guul_Draz_Vampire", Guul_Draz_Vampire);
		
		commands.put("Hada_Spy_Patrol", Hada_Spy_Patrol);
		commands.put("Halimar_Wavewatch", Halimar_Wavewatch);
		commands.put("Heedless_One", Heedless_One);
		commands.put("Homarid", Homarid);
		
		commands.put("Ikiral_Outrider", Ikiral_Outrider);
		commands.put("Iona_Shield_of_Emeria", Iona_Shield_of_Emeria);
		
		commands.put("Joiner_Adept", Joiner_Adept);
		commands.put("Jund_Hackblade", Jund_Hackblade);
		
		commands.put("Kargan_Dragonlord", Kargan_Dragonlord);
		commands.put("Keldon_Warlord", Keldon_Warlord);
		commands.put("Kithkin_Rabble", Kithkin_Rabble);
		commands.put("Knight_of_Cliffhaven", Knight_of_Cliffhaven);
		commands.put("Korlash_Heir_to_Blackblade", Korlash_Heir_to_Blackblade);
		commands.put("Koth_Emblem", Koth_Emblem);
		
		commands.put("Leyline_of_Singularity", Leyline_of_Singularity);
		commands.put("Lhurgoyf", Lhurgoyf);
		commands.put("Lighthouse_Chronologist", Lighthouse_Chronologist);
		commands.put("Liu_Bei", Liu_Bei);
		commands.put("Lord_of_Extinction", Lord_of_Extinction);
		commands.put("Loxodon_Punisher", Loxodon_Punisher);
		
		commands.put("Magnivore", Magnivore);
		commands.put("Magus_of_the_Tabernacle", Magus_of_the_Tabernacle);
		commands.put("Maraxus_of_Keld", Maraxus_of_Keld);
		commands.put("Maro", Maro);
		commands.put("Marrow_Gnawer", Marrow_Gnawer);
		commands.put("Master_of_Etherium", Master_of_Etherium);
		commands.put("Masumaro_First_to_Live", Masumaro_First_to_Live);
		commands.put("Matca_Rioters", Matca_Rioters);
		commands.put("Meddling_Mage", Meddling_Mage);
		commands.put("Molimo_Maro_Sorcerer", Molimo_Maro_Sorcerer);
		commands.put("Mortivore", Mortivore);
		commands.put("Mul_Daya_Channelers", Mul_Daya_Channelers);
		commands.put("Multani_Maro_Sorcerer", Multani_Maro_Sorcerer);
		commands.put("Muraganda_Petroglyphs", Muraganda_Petroglyphs);
		
		commands.put("Nightmare", Nightmare);
		commands.put("Nirkana_Cutthroat", Nirkana_Cutthroat);
		commands.put("Nut_Collector", Nut_Collector);
		commands.put("Nyxathid", Nyxathid);
		
		commands.put("Old_Man_of_the_Sea", Old_Man_of_the_Sea);
		commands.put("Omnath", Omnath);
		commands.put("Overbeing_of_Myth", Overbeing_of_Myth);
		
		commands.put("People_of_the_Woods", People_of_the_Woods);
		commands.put("Phylactery_Lich", Phylactery_Lich);
		commands.put("Plague_Rats", Plague_Rats);
		commands.put("Primalcrux", Primalcrux);
		
		commands.put("Rakdos_Pit_Dragon", Rakdos_Pit_Dragon);
		commands.put("Ruthless_Cullblade", Ruthless_Cullblade);
		
		commands.put("Serpent_of_the_Endless_Sea", Serpent_of_the_Endless_Sea);
		commands.put("Serra_Avatar", Serra_Avatar);
		commands.put("Skywatcher_Adept", Skywatcher_Adept);
		commands.put("Soulsurge_Elemental", Soulsurge_Elemental);
		commands.put("Sound_the_Call_Wolf", Sound_the_Call_Wolf);
		commands.put("Spidersilk_Armor", Spidersilk_Armor);
		commands.put("Student_of_Warfare", Student_of_Warfare);
		commands.put("Svogthos_the_Restless_Tomb", Svogthos_the_Restless_Tomb);
		
		commands.put("Tabernacle", Tabernacle);
		commands.put("Tarmogoyf", Tarmogoyf);
		commands.put("Terravore", Terravore);
		commands.put("Tethered_Griffin", Tethered_Griffin);
		commands.put("That_Which_Was_Taken", That_Which_Was_Taken);
		commands.put("Time_of_Heroes", Time_of_Heroes);
		commands.put("Transcendent_Master", Transcendent_Master);
		
		commands.put("Umbra_Stalker", Umbra_Stalker);
		commands.put("Uril", Uril);
		
		commands.put("Vampire_Nocturnus", Vampire_Nocturnus);
		commands.put("Vexing_Beetle", Vexing_Beetle);
		
		commands.put("Windwright_Mage", Windwright_Mage);
		commands.put("Wrens_Run_Packmaster", Wrens_Run_Packmaster);
		
		commands.put("Zulaport_Enforcer", Zulaport_Enforcer);
		
		///The commands above are in alphabetical order by cardname.  The cammands.put() below need to be filed above
	}

}
