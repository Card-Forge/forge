package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.MyRandom;
import forge.card.cardFactory.CardFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;

public class AbilityFactory_Copy {

	// *************************************************************************
	// ************************* CopyPermanent *********************************
	// *************************************************************************

	public static SpellAbility createAbilityCopyPermanent(final AbilityFactory af) {

		final SpellAbility abCopyPermanent = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 4557071554433108024L;

			@Override
			public String getStackDescription() {
				return copyPermanentStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return copyPermanentCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				copyPermanentResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return copyPermanentTriggerAI(af, this, mandatory);
			}

		};
		return abCopyPermanent;
	}

	public static SpellAbility createSpellCopyPermanent(final AbilityFactory af) {
		final SpellAbility spCopyPermanent = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 3313370358993251728L;

			@Override
			public String getStackDescription() {
				return copyPermanentStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return copyPermanentCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				copyPermanentResolve(af, this);
			}

		};
		return spCopyPermanent;
	}

	public static SpellAbility createDrawbackCopyPermanent(final AbilityFactory af) {
		final SpellAbility dbCopyPermanent = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -7725564505830285184L;

			@Override
			public String getStackDescription(){
				return copyPermanentStackDescription(af, this);
			}

			@Override
			public void resolve() {
				copyPermanentResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return copyPermanentTriggerAI(af, this, mandatory);
			}

		};
		return dbCopyPermanent;
	}

	private static String copyPermanentStackDescription(AbilityFactory af, SpellAbility sa) {
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard()).append(" - ");
		else
			sb.append(" ");

		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		sb.append("Copy ");
		Iterator<Card> it = tgtCards.iterator();
		while(it.hasNext()) {
			sb.append(it.next());
			if(it.hasNext()) sb.append(", ");
		}
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean copyPermanentCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		//TODO - I'm sure someone can do this AI better
		
		HashMap<String,String> params = af.getMapParams();
		if(params.containsKey("AtEOT") && !AllZone.Phase.is(Constant.Phase.Main1)) {
			return false;
		}
		else return copyPermanentTriggerAI(af, sa, false);
	}

	private static boolean copyPermanentTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory){
		//HashMap<String,String> params = af.getMapParams();
		Card source = sa.getSourceCard();
		
		if (!ComputerUtil.canPayCost(sa) && !mandatory)
			return false;
		
		double chance = .4;	// 40 percent chance with instant speed stuff
		if (AbilityFactory.isSorcerySpeed(sa))
			chance = .667;	// 66.7% chance for sorcery speed (since it will never activate EOT)
		Random r = MyRandom.random;
		boolean randomReturn = r.nextFloat() <= Math.pow(chance, source.getAbilityUsed() + 1);
		
		//////
		// Targeting
		
		Target abTgt = sa.getTarget();
		
		if (abTgt != null){
			CardList list = AllZoneUtil.getCardsInPlay();
			list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
			abTgt.resetTargets();
			// target loop
			while(abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) { 
				if(list.size() == 0) {
					if(abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0) {
						abTgt.resetTargets();
						return false;
					}
					else {
						// TODO is this good enough? for up to amounts?
						break;
					}
				}
				
				Card choice;
				if(list.filter(AllZoneUtil.creatures).size() > 0) {
					choice = CardFactoryUtil.AI_getBestCreature(list);
				}
				else {
					choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, source, true);
				}

				if(choice == null) {	// can't find anything left
					if(abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0) {
						abTgt.resetTargets();
						return false;
					}
					else {
						// TODO is this good enough? for up to amounts?
						break;
					}
				}
				list.remove(choice);
				abTgt.addTarget(choice);
			}
		}
		else {
			//if no targeting, it should always be ok
		}
		
		//end Targeting

		if (af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if(abSub != null) {
				return randomReturn && abSub.chkAI_Drawback();
			}
		}
		return randomReturn;
	}

	private static void copyPermanentResolve(final AbilityFactory af, final SpellAbility sa) {
		final HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) {
			keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		}

		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(Card c : tgtCards) {
			if (tgt == null || CardFactoryUtil.canTarget(card, c)) {

				//start copied Kiki code
				int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(card.getController());
				Card[] crds = new Card[multiplier];

				for(int i = 0; i < multiplier; i++) {
					//TODO: Use central copy methods
					Card copy;
					if(!c.isToken()) {
						//copy creature and put it onto the battlefield
						copy = AllZone.CardFactory.getCard(c.getName(), sa.getActivatingPlayer());

						//when copying something stolen:
						copy.setController(sa.getActivatingPlayer());

						copy.setToken(true);
						copy.setCopiedToken(true);
					}
					else { //isToken()
						copy = CardFactory.copyStats(c);

						copy.setName(c.getName());
						copy.setImageName(c.getImageName());

						copy.setOwner(sa.getActivatingPlayer());
						copy.setController(sa.getActivatingPlayer());

						copy.setManaCost(c.getManaCost());
						copy.setColor(c.getColor());
						copy.setToken(true);

						copy.setType(c.getType());

						copy.setBaseAttack(c.getBaseAttack());
						copy.setBaseDefense(c.getBaseDefense());
					}
					
					//add keywords from params
					for(String kw : keywords) {
						copy.addIntrinsicKeyword(kw);
					}

					//Slight hack in case we copy a creature with triggers.
					for(Trigger t : copy.getTriggers()) {
						AllZone.TriggerHandler.registerTrigger(t);
					}

					copy.setCurSetCode(c.getCurSetCode());
					copy.setImageFilename(c.getImageFilename());

					if(c.isFaceDown()) {
						copy.setIsFaceDown(true);
						copy.setManaCost("");
						copy.setBaseAttack(2);
						copy.setBaseDefense(2);
						copy.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
						copy.setType(new ArrayList<String>()); //remove all types
						copy.addType("Creature");
						copy.clearSpellAbility(); //disallow "morph_up"
						copy.setCurSetCode("");
						copy.setImageFilename("morph.jpg");
					}

					AllZone.GameAction.moveToPlay(copy);
					crds[i] = copy;
				}


				//have to do this since getTargetCard() might change
				//if Kiki-Jiki somehow gets untapped again
				final Card[] target = new Card[multiplier];
				for(int i = 0; i < multiplier; i++) {
					final int index = i;
					target[index] = crds[index];
					
					final SpellAbility sac = new Ability(target[index], "0") {
						@Override
						public void resolve() {
							//technically your opponent could steal the token
							//and the token shouldn't be sacrificed
							if(AllZoneUtil.isCardInPlay(target[index])) {
								if(params.get("AtEOT").equals("Sacrifice")) {
									AllZone.GameAction.sacrifice(target[index]); //maybe do a setSacrificeAtEOT, but probably not.
								}
								else if(params.get("AtEOT").equals("Exile")) {
									AllZone.GameAction.exile(target[index]);
								}
								
								//Slight hack in case we copy a creature with triggers
								AllZone.TriggerHandler.removeAllFromCard(target[index]);
							}
						}
					};
					
					Command atEOT = new Command() {
						private static final long serialVersionUID = -4184510100801568140L;

						public void execute() {
							sac.setStackDescription(params.get("AtEOT")+" "+target[index]+".");
							AllZone.Stack.addSimultaneousStackEntry(sac);
						}
					};//Command
					if(params.containsKey("AtEOT")) {
						AllZone.EndOfTurn.addAt(atEOT);
					}
					//end copied Kiki code

				}
			}//end canTarget
		}//end foreach Card
	}//end resolve
	
	// *************************************************************************
	// ************************* CopySpell *************************************
	// *************************************************************************

	public static SpellAbility createAbilityCopySpell(final AbilityFactory af) {

		final SpellAbility abCopySpell = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 5232548517225345052L;

			@Override
			public String getStackDescription() {
				return copySpellStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return copySpellCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				copySpellResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return copySpellTriggerAI(af, this, mandatory);
			}

		};
		return abCopySpell;
	}

	public static SpellAbility createSpellCopySpell(final AbilityFactory af) {
		final SpellAbility spCopySpell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 1878946074608916745L;

			@Override
			public String getStackDescription() {
				return copySpellStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return copySpellCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				copySpellResolve(af, this);
			}

		};
		return spCopySpell;
	}

	public static SpellAbility createDrawbackCopySpell(final AbilityFactory af) {
		final SpellAbility dbCopySpell = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = 1927508119173644632L;

			@Override
			public String getStackDescription(){
				return copySpellStackDescription(af, this);
			}

			@Override
			public void resolve() {
				copySpellResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return copySpellTriggerAI(af, this, mandatory);
			}

		};
		return dbCopySpell;
	}

	private static String copySpellStackDescription(AbilityFactory af, SpellAbility sa) {
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		ArrayList<SpellAbility> tgtSpells;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtSpells = tgt.getTargetSAs();
		else
			tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		sb.append("Copy ");
		Iterator<SpellAbility> it = tgtSpells.iterator();
		while(it.hasNext()) {
			sb.append(it.next().getSourceCard());
			if(it.hasNext()) sb.append(", ");
		}
		sb.append(".");
		//TODO probably add an optional "You may choose new targets..."

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean copySpellCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		return false;
	}

	private static boolean copySpellTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		boolean randomReturn = false;
		
		if(af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				return randomReturn && abSub.chkAI_Drawback();
			}
		}
		return randomReturn;
	}

	private static void copySpellResolve(final AbilityFactory af, final SpellAbility sa) {
		//final HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();

		ArrayList<SpellAbility> tgtSpells;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtSpells = tgt.getTargetSAs();
		else
			tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(SpellAbility tgtSA: tgtSpells) {
			if (tgt == null || CardFactoryUtil.canTarget(card, tgtSA.getSourceCard())) {

				//copied from Twincast
				AllZone.CardFactory.copySpellontoStack(card, tgtSA.getSourceCard(), true);				
				//end copied from Twincast

			}//end canTarget
		}//end foreach SpellAbility
	}//end resolve

}//end class AbilityFactory_Copy
