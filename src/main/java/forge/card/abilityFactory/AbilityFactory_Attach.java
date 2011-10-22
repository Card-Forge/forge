package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.CombatUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameEntity;
import forge.MyRandom;
import forge.Player;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;
import forge.card.spellability.Target;
import forge.card.staticAbility.StaticAbility;
import forge.gui.GuiUtils;

public class AbilityFactory_Attach {
	public static SpellAbility createSpellAttach(final AbilityFactory AF){
		// There are two types of Spell Attachments: Auras and Instants/Sorceries
		// Auras generally target what that card will attach to
		// I/S generally target the Attacher and the Attachee
		SpellAbility spAttach = null;
		if (AF.getHostCard().isAura()){
			// The 4th parameter is to resolve an issue with SetDescription in default Spell_Permanent constructor
			spAttach = new Spell_Permanent(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt(), false){
				private static final long serialVersionUID = 6631124959690157874L;
				
				final AbilityFactory af = AF;
			
				@Override
				public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
					return attachStackDescription(af, this);
				}
				
				public boolean canPlayAI(){
					return attachCanPlayAI(af, this);
				}
				
				@Override
				public void resolve() {
					// The Spell_Permanent (Auras) version of this AF needs to move the card into play before Attaching
					Card c = AllZone.getGameAction().moveToPlay(getSourceCard());
					this.setSourceCard(c);
					attachResolve(af, this);
				}
			};
		}
		else{
			// This is here to be complete, however there's only a few cards that use it
			// And the Targeting system can't really handle them at this time (11/7/1)
			spAttach = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
				private static final long serialVersionUID = 6631124959690157874L;
				
				final AbilityFactory af = AF;
			
				@Override
				public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
					return attachStackDescription(af, this);
				}
				
				public boolean canPlayAI(){
					return attachCanPlayAI(af, this);
				}
				
				@Override
				public void resolve() {
					attachResolve(af, this);
				}
			};
		}
		return spAttach;
	}

	// Attach Ability
	public static SpellAbility createAbilityAttach(final AbilityFactory AF){
		// placeholder for Equip and other similar cards
		return null;
	}
	
	// Attach Drawback
	public static SpellAbility createDrawbackAttach(final AbilityFactory AF){
		// placeholder for DBs that might attach
		return null;
	}
	
	public static String attachStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		String conditionDesc = af.getMapParams().get("ConditionDescription");
		if (conditionDesc != null)
			sb.append(conditionDesc).append(" ");

		sb.append(" Attach to ");
		
		ArrayList<Object> targets;

		// Should never allow more than one Attachment per card
		Target tgt = af.getAbTgt();
		if (tgt != null)
			targets = tgt.getTargets();
		else
			targets = AbilityFactory.getDefinedObjects(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Object o : targets)
			sb.append(o).append(" ");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null)
			sb.append(abSub.getStackDescription());

		return sb.toString();
	}
	
	public static boolean attachPreference(AbilityFactory af, SpellAbility sa, Map<String,String> params, Target tgt, boolean mandatory){
		Object o;
		if (tgt.canTgtPlayer())
			o = attachToPlayerAIPreferences(af, sa, mandatory);

		else
			o = attachToCardAIPreferences(af, sa, params, mandatory);

		if (o == null)
			return false;
		
		tgt.addTarget(o);
		return true;
	}
	
	public static Card attachToCardAIPreferences(AbilityFactory af, final SpellAbility sa, Map<String,String> params, boolean mandatory){
		Target tgt = sa.getTarget();
		Card attachSource = sa.getSourceCard();
		// TODO AttachSource is currently set for the Source of the Spell, but at some point can support attaching a different card
		
		CardList list = AllZoneUtil.getCardsIn(tgt.getZone());
		list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), attachSource);
		
		// TODO: If Attaching without casting, don't need to actually target. 
		// I believe this is the only case where mandatory will be true, so just check that when starting that work
		// But we shouldn't attach to things with Protection
		if (tgt.getZone().contains(Zone.Battlefield) && !mandatory){
			list = list.getTargetableCards(attachSource);
		}
		else{
			list = list.getUnprotectedCards(attachSource);
		}
		
		if (list.size() == 0)
			return null;
		
		Card c = attachGeneralAI(sa, list, mandatory, attachSource, params.get("AILogic"));
		
		if (c == null && mandatory){
			list.shuffle();
			c = list.get(0);
		}
		
		return c;
	}
	
	public static Card attachGeneralAI(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource, String logic){
		Player prefPlayer = "Pump".equals(logic) ? AllZone.getComputerPlayer() : AllZone.getHumanPlayer();
		// Some ChangeType cards are beneficial, and PrefPlayer should be changed to represent that
		CardList prefList = list.getController(prefPlayer);
		
		// If there are no preferred cards, and not mandatory bail out
		if (prefList.size() == 0)
			return chooseUnpreferred(mandatory, list);

		// Preferred list has at least one card in it to make to the actual Logic
		Card c = null;
		if ("GainControl".equals(logic))
			c = attachAIControlPreference(sa, prefList, mandatory, attachSource);
		else if ("Curse".equals(logic))
			c = attachAICursePreference(sa, prefList, mandatory, attachSource);
		else if ("Pump".equals(logic))
			c = attachAIPumpPreference(sa, prefList, mandatory, attachSource);
		else if ("ChangeType".equals(logic))	// Evil Presence, Spreading Seas
			c = attachAIChangeTypePreference(sa, prefList, mandatory, attachSource);
		else if ("KeepTapped".equals(logic)){
		    c = attachAIKeepTappedPreference(sa, prefList, mandatory, attachSource);
		}
		
		return c;
	}
	
	public static Card chooseUnpreferred(boolean mandatory, CardList list){
		if (!mandatory)
			return null;
		
		return CardFactoryUtil.AI_getWorstPermanent(list, true, true, true, false);
	}
	
	public static Card chooseLessPreferred(boolean mandatory, CardList list){
		if (!mandatory)
			return null;
		
		return CardFactoryUtil.AI_getBest(list);
	}
	
	public static Card acceptableChoice(Card c, boolean mandatory){
        if (mandatory)
        	return c;
        
        // TODO: If Not Mandatory, make sure the card is "good enough"
        if (c.isCreature()){
	        int eval = CardFactoryUtil.evaluateCreature(c);
	        if (eval < 160 && (eval < 130 || AllZone.getComputerPlayer().getLife() > 5))
	        	return null;
        }

		return c;
	}
	
	// Should generalize this code a bit since they all have similar structures
	public static Card attachAIControlPreference(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource){
		// AI For choosing a Card to Gain Control of. 

		if (sa.getTarget().canTgtPermanent()){
			// If can target all Permanents, and Life isn't in eminent danger, grab Planeswalker first, then Creature
			// if Life < 5 grab Creature first, then Planeswalker. Lands, Enchantments and Artifacts are probably "not good enough"
			
		}
		
		Card c = CardFactoryUtil.AI_getBest(list);
        
		// If Mandatory (brought directly into play without casting) gotta choose something
        if (c == null)
        	return chooseLessPreferred(mandatory, list);
        
        return acceptableChoice(c, mandatory);
	}
	

	public static Card attachAIPumpPreference(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource){
		// AI For choosing a Card to Pump 
		Card c = null;
		CardList magnetList = null;
		String stCheck = null;
		if (attachSource.isAura()){
			stCheck = "EnchantedBy";
			magnetList = list.getEnchantMagnets();
		}
		else if (attachSource.isEquipment()){
			stCheck = "EquippedBy";
			magnetList = list.getEquipMagnets();
		}
		
		if (magnetList != null && !magnetList.isEmpty()){
			// Always choose something from the Magnet List.
			// Probably want to "weight" the list by amount of Enchantments and choose the "lightest"

			magnetList = magnetList.filter(new CardListFilter() {
				@Override
				public boolean addCard(Card c) {
					return CombatUtil.canAttack(c);
				}
			});
			
			return CardFactoryUtil.AI_getBest(magnetList);
		}
		
		int totToughness = 0;
		int totPower = 0;
		ArrayList<String> keywords = new ArrayList<String>();
		boolean grantingAbilities = false;
		
		for (StaticAbility stAbility : attachSource.getStaticAbilities()){
			Map<String,String> params = stAbility.getMapParams();
			
			if (!params.get("Mode").equals("Continuous"))
				continue;
			
			String affected = params.get("Affected");
			
			if (affected == null)
				continue;
			if ((affected.contains(stCheck) || affected.contains("AttachedBy")) ){
				totToughness += CardFactoryUtil.parseSVar(attachSource, params.get("AddToughness"));
				totPower += CardFactoryUtil.parseSVar(attachSource, params.get("AddPower"));
				
				grantingAbilities |= params.containsKey("AddAbility");
				
				String kws = params.get("AddKeyword");
				if (kws != null){
					for(String kw : kws.split(" & "))
					keywords.add(kw);
				}
			}
		}
		
		CardList prefList = new CardList(list);
		if (totToughness < 0){
			// Don't kill my own stuff with Negative toughness Auras
			final int tgh = totToughness;
			prefList = prefList.filter(new CardListFilter() {
				@Override
				public boolean addCard(Card c) {
					return c.getLethalDamage() > Math.abs(tgh);
				}
			});
		}
		
		else if (totToughness == 0 && totPower == 0){
			// Just granting Keywords don't assign stacking Keywords
			Iterator<String> it = keywords.iterator();
			while(it.hasNext()){
				String key = it.next();
				if (CardUtil.isStackingKeyword(key))
					it.remove();
			}
			if (!keywords.isEmpty()){
				final ArrayList<String> finalKWs = keywords;
				prefList = prefList.filter(new CardListFilter() {
					//If Aura grants only Keywords, don't Stack unstackable keywords
					@Override
					public boolean addCard(Card c) {
						for(String kw : finalKWs)
							if (c.hasKeyword(kw))
								return false;
						
						return true;
					}
				});
			}
		}

		if (attachSource.isAura()){
			// TODO: For Auras like Rancor, that aren't as likely to lead to card disadvantage, this check should be skipped
			prefList = prefList.filter(new CardListFilter() {
				
				@Override
				public boolean addCard(Card c) {
					return !c.isEnchanted();
				}
			});
		}
		
		if (!grantingAbilities){
		// Probably prefer to Enchant Creatures that Can Attack
		// Filter out creatures that can't Attack or have Defender
			prefList = prefList.filter(new CardListFilter() {
				@Override
				public boolean addCard(Card c) {
					return !c.isCreature() || CombatUtil.canAttack(c);
				}
			});
			c = CardFactoryUtil.AI_getBest(prefList);
		}
		else // If we grant abilities, we may want to put it on something Weak? Possibly more defensive?
			c = CardFactoryUtil.AI_getWorstPermanent(prefList, false, false, false, false);

		
        if (c == null)
        	return chooseLessPreferred(mandatory, list);
        
        return acceptableChoice(c, mandatory);
	}
	
	public static Card attachAICursePreference(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource){
		// AI For choosing a Card to Curse of. 	

		// TODO Figure out some way to combine The "gathering of data" from statics used in both Pump and Curse
		String stCheck = null;
		if (attachSource.isAura()){
			stCheck = "EnchantedBy";
		}
		else if (attachSource.isEquipment()){
			stCheck = "EquippedBy";
		}
		
		int totToughness = 0;
		//int totPower = 0;
		ArrayList<String> keywords = new ArrayList<String>();
		//boolean grantingAbilities = false;
		
		for (StaticAbility stAbility : attachSource.getStaticAbilities()){
			Map<String,String> params = stAbility.getMapParams();
			
			if (!params.get("Mode").equals("Continuous"))
				continue;
			
			String affected = params.get("Affected");
			
			if (affected == null)
				continue;
			if ((affected.contains(stCheck) || affected.contains("AttachedBy")) ){
				totToughness += CardFactoryUtil.parseSVar(attachSource, params.get("AddToughness"));
				//totPower += CardFactoryUtil.parseSVar(attachSource, params.get("AddPower"));
				
				//grantingAbilities |= params.containsKey("AddAbility");
				
				String kws = params.get("AddKeyword");
				if (kws != null){
					for(String kw : kws.split(" & "))
						keywords.add(kw);
				}
			}
		}
		
		CardList prefList = null;
		if (totToughness < 0){
			// Kill a creature if we can
			final int tgh = totToughness;
			prefList = list.filter(new CardListFilter() {
				@Override
				public boolean addCard(Card c) {
					if (!c.hasKeyword("Indestructible") && c.getLethalDamage() <= Math.abs(tgh))
						return true;
					
					return c.getNetDefense() <= Math.abs(tgh);
				}
			});
		}
		Card c = null;
		if (prefList == null || prefList.size() == 0)
			prefList = new CardList(list);
		else {
			c = CardFactoryUtil.AI_getBest(prefList);
			if (c != null)
				return c;
		}
		
		if (!keywords.isEmpty()){
			// Don't give Can't Attack or Defender to cards that can't do these things to begin with
			if (keywords.contains("CARDNAME can't attack") || keywords.contains("Defender") || 
					keywords.contains("CARDNAME attacks each turn if able.")){
				prefList = prefList.filter(new CardListFilter() {
					@Override
					public boolean addCard(Card c) {
						return !(c.hasKeyword("CARDNAME can't attack") || c.hasKeyword("Defender"));
					}
				});
			}
		}
		
		c = CardFactoryUtil.AI_getBest(prefList);
		
        if (c == null)
        	return chooseLessPreferred(mandatory, list);
		
        return acceptableChoice(c, mandatory);
	}
	
   public static Card attachAIChangeTypePreference(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource){
        // AI For Cards like Evil Presence or Spreading Seas
       
        String type = "";
       
        for (StaticAbility stAb : attachSource.getStaticAbilities()) {
            HashMap<String, String> params = stAb.getMapParams();
            if (params.get("Mode").equals("Continuous") && params.containsKey("AddType"))
                type = params.get("AddType");
        }
        
        list.getNotType(type);// Filter out Basic Lands that have the same type as the changing type
        
        Card c = CardFactoryUtil.AI_getBest(list);
                
        // TODO: Port over some of the existing code, but rewrite most of it.
        // Ultimately, these spells need to be used to reduce mana base of a color. So it might be better to choose a Basic over a Nonbasic
        
        if (c == null)
            return chooseLessPreferred(mandatory, list);
        
        return acceptableChoice(c, mandatory);
    }
	
	public static Card attachAIKeepTappedPreference(final SpellAbility sa, CardList list, boolean mandatory, Card attachSource){
		// AI For Cards like Paralyzing Grasp and Glimmerdust Nap 
	    CardList prefList = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(Card c) {
				// Don't do Untapped Vigilance cards
                if (c.isCreature() && c.hasKeyword("Vigilance") && c.isUntapped())
                    return false;
                
                if (!c.isEnchanted())
                    return true;

                ArrayList<Card> auras = c.getEnchantedBy();
                Iterator<Card> itr = auras.iterator();
                while(itr.hasNext()){
                    Card aura = (Card)itr.next();
                    AbilityFactory af = aura.getSpellPermanent().getAbilityFactory();
                    if (af != null && af.getAPI().equals("Attach")){
                        Map<String,String> params = af.getMapParams();
                        if ("KeepTapped".equals(params.get("AILogic"))){
                            // Don't attach multiple KeepTapped Auras to one card
                            return false;
                        }
                    }
                }
                
                
                
                return true;
            }
        });
	    
	    
		Card c = CardFactoryUtil.AI_getBest(prefList);
        
        if (c == null)
			return chooseLessPreferred(mandatory, list);
		
        return acceptableChoice(c, mandatory);
	}
	
	public static Player attachToPlayerAIPreferences(AbilityFactory af, final SpellAbility sa, boolean mandatory){
		Target tgt = sa.getTarget();
		Player p;
		if (tgt.canOnlyTgtOpponent()){
			// If can Only Target Opponent, do so.
			p = AllZone.getHumanPlayer();
			if (p.canTarget(sa))
				return p;
			else
				return null;
		}
		
		if ("Curse".equals(af.getMapParams().get("AILogic")))
			p = AllZone.getHumanPlayer();
		else
			p = AllZone.getComputerPlayer();

		if (p.canTarget(sa))
			return p;
		
		if (!mandatory)
			return null;
		
		p = p.getOpponent();
		if (p.canTarget(sa))
			return p;
		
		return null;
	}
	
	public static boolean attachCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		Random r = MyRandom.random;
		Map<String,String> params = af.getMapParams();
		Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();

		if (abCost != null){
			// No Aura spells have Additional Costs 
		    
		}

		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= .6667;
		
		// Attach spells always have a target
		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (!attachPreference(af, sa, params, tgt, false))
				return false;
		}
		
		if (abCost.getTotalMana().contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value. (Endless Scream and Venarian Gold)
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			
			if (xPay == 0)
				return false;
			
			source.setSVar("PayX", Integer.toString(xPay));
		}
		
		if (AbilityFactory.isSorcerySpeed(sa)){
			if (AllZone.getPhase().is(Constant.Phase.Main1))
				chance = r.nextFloat() <= .75;
			else // Don't Attach Sorcery Speed stuff after Main1
				return false;
		}
		else
			chance &= r.nextFloat() <= .75;
		
		return chance;
	}
	
	public static boolean attachDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))	// If there is a cost payment it's usually not mandatory
			return false;

		// Check if there are any valid targets
		
		// Now are Valid Targets better than my targets?

		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null)
			return abSub.doTrigger(mandatory);

		return true;
	}
	
	public static void attachResolve(final AbilityFactory af, final SpellAbility sa){
		Map<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Object> targets;

		Target tgt = af.getAbTgt();
		if (tgt != null){
			targets = tgt.getTargets();
			// TODO Remove invalid targets (although more likely this will just fizzle earlier)
		}
		else
			targets = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
		
		// If Cast Targets will be checked on the Stack
		for(Object o : targets){
			handleAttachment(card, o, af);
		}
	}
	
	public static void handleAttachment(Card card, Object o, AbilityFactory af){

		if (o instanceof Card){
			Card c = (Card)o;
			if (card.isAura()){
				// Most Auras can enchant permanents, a few can Enchant cards in graveyards
				// Spellweaver Volute, Dance of the Dead, Animate Dead
				// Although honestly, I'm not sure if the three of those could handle being scripted
				boolean gainControl = "GainControl".equals(af.getMapParams().get("AILogic")); 
				handleAura(card, c, gainControl);
			}
			else if (card.isEquipment())
				card.equipCard(c);
			//else if (card.isFortification())
			//	card.fortifyCard(c);
		}
		else if (o instanceof Player){
			// Currently, a few cards can enchant players
			// Psychic Possession, Paradox Haze, Wheel of Sun and Moon, New Curse cards
			Player p = (Player)o;
			if (card.isAura()){
				handleAura(card, p, false);
			}
		}
	}
	
	public static void handleAura(final Card card, final GameEntity tgt, boolean gainControl){
		if (card.isEnchanting()){
			// If this Card is already Enchanting something
			// Need to unenchant it, then clear out the commands
			GameEntity oldEnchanted = card.getEnchanting();
			card.removeEnchanting(oldEnchanted);
			card.clearEnchantCommand();
			card.clearUnEnchantCommand();
			card.clearTriggers();	// not sure if cleartriggers is needed?
		}
		
		if (gainControl){
			// Handle GainControl Auras
			final Player[] pl = new Player[1];
			
			if (tgt instanceof Card){
			    pl[0] = ((Card)tgt).getController();
			}
			else{
			    pl[0] = (Player)tgt;
			}
			
	        Command onEnchant = new Command() {
				private static final long serialVersionUID = -2519887209491512000L;
	
				public void execute() {
                    Card crd = card.getEnchantingCard();
                    if (crd == null){ 
                        return; 
                    }
                        
                    pl[0] = crd.getController();

                    crd.addController(card);

	            }//execute()
	        };//Command
	        
	        Command onUnEnchant = new Command() {
				private static final long serialVersionUID = 3426441132121179288L;
	
				public void execute() {
                    Card crd = card.getEnchantingCard();
                    if (crd == null){ 
                        return; 
                    }

                    if(AllZoneUtil.isCardInPlay(crd)) {
                        crd.removeController(card);
                    }
	                
	            }//execute()
	        };//Command

            Command onChangesControl = new Command() {
                /** automatically generated serialVersionUID. */
				private static final long serialVersionUID = -65903786170234039L;

				public void execute() {
                    Card crd = card.getEnchantingCard();
                    if (crd == null){ 
                        return; 
                    }
                    crd.removeController(card); //This looks odd, but will simply refresh controller
                    crd.addController(card);
                }//execute()
            };//Command
			
	        // Add Enchant Commands for Control changers
	        card.addEnchantCommand(onEnchant);
	        card.addUnEnchantCommand(onUnEnchant);
            card.addChangeControllerCommand(onChangesControl);
		}
		
        Command onLeavesPlay = new Command() {
			private static final long serialVersionUID = -639204333673364477L;

			public void execute() {
                GameEntity entity = card.getEnchanting();
                if (entity == null){ 
                    return; 
                }

                card.unEnchantEntity(entity);
            }
        };//Command
		
        card.addLeavesPlayCommand(onLeavesPlay);
		card.enchantEntity(tgt);
	}
	
	public static SpellAbility getAttachSpellAbility(Card source){
		SpellAbility aura = null;
		AbilityFactory af = null;
		for (SpellAbility sa : source.getSpells()){
			af = sa.getAbilityFactory();
			if (af != null && af.getAPI().equals("Attach")){
				aura = sa;
				break;
			}
		}
		return aura;
	}
	
	public static boolean attachAuraOnIndirectEnterBattlefield(Card source){
		// When an Aura ETB without being cast you can choose a valid card to attach it to
		SpellAbility aura = getAttachSpellAbility(source);

		if (aura == null)
			return false;
		
		AbilityFactory af = aura.getAbilityFactory();
		Target tgt = aura.getTarget();
				
		if (source.getController().isHuman()){
		    if (tgt.canTgtPlayer()){
		        ArrayList<Player> players = new ArrayList<Player>();

		        // TODO Once Player's are gaining Protection we need to add a check here
		        
		        players.add(AllZone.getComputerPlayer());
		        if (!tgt.canOnlyTgtOpponent()){
		            players.add(AllZone.getHumanPlayer());
		        }
		        
                Object o = GuiUtils.getChoice(source + " - Select a player to attach to.", players.toArray());
                if (o instanceof Player) {
                    source.enchantEntity((Player) o);
                    return true;
                }
		    }
		    else{
                CardList list = AllZoneUtil.getCardsIn(tgt.getZone());
                list = list.getValidCards(tgt.getValidTgts(), aura.getActivatingPlayer(), source);

                Object o = GuiUtils.getChoice(source + " - Select a card to attach to.", list.toArray());
                if (o instanceof Card) {
                    source.enchantEntity((Card) o);
                    return true;
                }
		    }
		}
		
		else if (AbilityFactory_Attach.attachPreference(af, aura, af.getMapParams(), tgt, true)){
			Object o = aura.getTarget().getTargets().get(0);
			if (o instanceof Card){
				source.enchantEntity((Card)o);
				return true;
			}
			else if (o instanceof Player){
			    source.enchantEntity((Player)o);
			    return true;
			}
		}

		return false;
	}
}
