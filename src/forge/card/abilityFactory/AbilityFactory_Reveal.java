package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.Player;
import forge.PlayerZone;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

public class AbilityFactory_Reveal {
	
	// *************************************************************************
	// ************************* Dig *******************************************
	// *************************************************************************

	public static SpellAbility createAbilityDig(final AbilityFactory af) {

		final SpellAbility abDig = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 4239474096624403497L;

			@Override
			public String getStackDescription() {
				return digStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return digCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				digResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return digTriggerAI(af, this, mandatory);
			}

		};
		return abDig;
	}

	public static SpellAbility createSpellDig(final AbilityFactory af) {
		final SpellAbility spDig = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 3389143507816474146L;

			@Override
			public String getStackDescription() {
				return digStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return digCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				digResolve(af, this);
			}

		};
		return spDig;
	}

	public static SpellAbility createDrawbackDig(final AbilityFactory af) {
		final SpellAbility dbDig = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -3372788479421357024L;

			@Override
			public String getStackDescription(){
				return digStackDescription(af, this);
			}

			@Override
			public void resolve() {
				digResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return digTriggerAI(af, this, mandatory);
			}

		};
		return dbDig;
	}

	private static String digStackDescription(AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();
		StringBuilder sb = new StringBuilder();
		int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard()).append(" - ");
		else
			sb.append(" ");

		/*
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		 */

		//TODO - need to update if human targets computer for looking

		sb.append(host.getController()).append(" looks at the top ").append(numToDig);
		sb.append(" card");
		if(numToDig != 1) sb.append("s");
		sb.append(" of his or her library.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean digCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		return false;
		/*
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();

		if (sa.getTarget() != null){
			tgt.resetTargets();
			sa.getTarget().addTarget(AllZone.ComputerPlayer);
		}
		else{
			ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			for (Player p : tgtPlayers)
				if (p.isHuman())
					return false;
			// not sure if the AI should be playing with cards that give the Human more turns.
		}
		return true;
		 */
	}

	private static boolean digTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();

		if (sa.getTarget() != null){
			tgt.resetTargets();
			sa.getTarget().addTarget(AllZone.ComputerPlayer);
		}
		else{
			ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			for (Player p : tgtPlayers)
				if (p.isHuman() && !mandatory)
					return false;
			// not sure if the AI should be playing with cards that give the Human more turns.
		}
		return true;
	}

	private static void digResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();
		int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);
		String destZone1 = params.containsKey("DestinationZone") ? params.get("DestinationZone") : "Hand";
		int destZone1ChangeNum = params.containsKey("ChangeNum") ? Integer.parseInt(params.get("ChangeNum")) : 1;
		String changeValid = params.containsKey("ChangeValid") ? params.get("ChangeValid") : "";
		boolean anyNumber = params.containsKey("AnyNumber");
		String destZone2 = params.containsKey("DestinationZone2") ? params.get("DestinationZone2") : "Library";
		int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : -1;
		boolean optional = params.containsKey("Optional");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(Player p : tgtPlayers) {
			if (tgt == null || p.canTarget(af.getHostCard())) {

				CardList top = new CardList();
				CardList valid = new CardList();
				CardList rest = new CardList();
				PlayerZone library = AllZone.getZone(Constant.Zone.Library, p);

				numToDig = Math.min(numToDig, library.size());
				for(int i = 0; i < numToDig; i++) {
					top.add(library.get(i));
				}

				if(top.size() > 0) {
					//show the user the revealed cards
					GuiUtils.getChoice("Looking at cards from library", top.toArray());

					if(!changeValid.equals("")) {
						valid = top.getValidCards(changeValid.split(","), host.getController(), host);
						for(Card c:top) {
							if(!valid.contains(c)) rest.add(c);
						}
					}
					else {
						valid = top;
					}

					int j = 0;
					while(j < destZone1ChangeNum || (anyNumber && j < numToDig)) {
						//let user get choice
						Card chosen = null;
						if(anyNumber || optional) {
							chosen = GuiUtils.getChoiceOptional("Choose a card to put into "+destZone1, valid.toArray());
						}
						else {
							chosen = GuiUtils.getChoice("Choose a card to put into "+destZone1, valid.toArray());
						}
						if(chosen == null) break;
						valid.remove(chosen);
						PlayerZone zone = AllZone.getZone(destZone1, chosen.getOwner());
						AllZone.GameAction.moveTo(zone, chosen);
						//AllZone.GameAction.revealToComputer() - for when this exists
						j++;
					}

					//dump anything not selected from valid back into the rest
					rest.addAll(valid.toArray());
					
					//now, move the rest to destZone2
					if(destZone2.equals("Library")) {
						//put them in any order
						while(rest.size() > 0) {
							Card chosen = GuiUtils.getChoice("Put the rest in your library in any order", rest.toArray());
							AllZone.GameAction.moveToLibrary(chosen, libraryPosition);
							rest.remove(chosen);
						}
					}
					else {
						//just move them randomly
						for(int i = 0; i < rest.size(); i++) {
							Card c = rest.get(i);
							PlayerZone toZone = AllZone.getZone(destZone2, c.getOwner());
							AllZone.GameAction.moveTo(toZone, c);
						}
					}
				}//end if canTarget
			}//end foreach player

			if (af.hasSubAbility()){
				Ability_Sub abSub = sa.getSubAbility();
				if (abSub != null) {
					abSub.resolve();
				}
			}
		}
	}//end resolve

}//end class AbilityFactory_Reveal
