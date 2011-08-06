package forge;

import java.util.ArrayList;
import java.util.HashMap;

public class AbilityFactory_Sacrifice {
	//**************************************************************
	// ****************************** FOG **************************
	//**************************************************************
	
	public static SpellAbility createAbilitySacrifice(final AbilityFactory AF){
		final SpellAbility abSacrifice = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlayAI()
			{
				return sacrificeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				sacrificeResolve(af, this);
			}
			
			public String getStackDescription(){
				return sacrificeDescription(af, this);
			}
		};
		return abSacrifice;
	}
	
	public static SpellAbility createSpellSacrifice(final AbilityFactory AF){
		final SpellAbility spSacrifice = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlayAI()
			{
				return sacrificeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				sacrificeResolve(af, this);
			}
			
			public String getStackDescription(){
				return sacrificeDescription(af, this);
			}
		};
		return spSacrifice;
	}
	
	public static SpellAbility createDrawbackSacrifice(final AbilityFactory AF){
		final SpellAbility dbSacrifice = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
			@Override
			public void resolve() {
				sacrificeResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return sacrificePlayDrawbackAI(af, this);
			}
			
			public String getStackDescription(){
				return sacrificeDescription(af, this);
			}
		};
		return dbSacrifice;
	}
	
	public static String sacrificeDescription(final AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		
		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard().getName()).append(" - ");
		
		Target tgt = af.getAbTgt();
		if (tgt == null){
			return sa.toString();
		}
		String valid = af.getMapParams().get("SacValid");
		String num = af.getMapParams().get("Amount");
		num = (num == null) ? "1" : num;
		int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);
		

		for(Player p : tgt.getTargetPlayers())
			sb.append(p.getName()).append(" ");
		
		String msg = af.getMapParams().get("SacMessage");
		if (msg == null)
			msg = valid;
		
		sb.append("Sacrifices ").append(amount).append(" ").append(msg);
		
		return sb.toString();
	}
	
	public static boolean sacrificeCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's Declare Blockers phase
		boolean chance = sacrificeTgtAI(af, sa);

		// Some additional checks based on what is being sacrificed, and who is sacrificing
		Target tgt = af.getAbTgt();
		if (tgt != null){
			String valid = af.getMapParams().get("SacValid");
			String num = af.getMapParams().get("Amount");
			num = (num == null) ? "1" : num;
			int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);
			
			CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
			list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
			
			if (list.size() == 0)
				return false;
			
			int half = amount / 2 + amount % 2;	// Half of amount rounded up
			
			// If the Human has at least half rounded up of the amount to be sacrificed, cast the spell
			if (list.size() < half)
				return false;
			
			return true;
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	public static boolean sacrificePlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's turn
		boolean chance = sacrificeTgtAI(af, sa);

		// todo: restrict the subAbility a bit
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	public static boolean sacrificeTgtAI(AbilityFactory af, SpellAbility sa){
		Target tgt = af.getAbTgt();
		if (tgt != null){
			tgt.resetTargets();
			if (AllZone.HumanPlayer.canTarget(sa.getSourceCard()))
				tgt.addTarget(AllZone.HumanPlayer);
			else
				return false;
		}
		else{
			String defined = af.getMapParams().get("Defined");
			if (defined == null){
				// Self Sacrifice. 
			}
			else if (defined.equals("Each")){
			// If Sacrifice hits both players:
			// Don't cast it if Human doesn't have any of valid
			// Definitely cast it if AI doesn't have any of Valid
			// Cast if the type is favorable: my "worst" valid is worse than his "worst" valid
			}
		}
		
		return true;
	}
	
	public static void sacrificeResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		String DrawBack = params.get("SubAbility");
		
		// Expand Fog keyword here depending on what we need out of it.
		String num = params.containsKey("Amount") ? params.get("Amount") : "1";
		int amount = AbilityFactory.calculateAmount(card, num, sa);
		
		Target tgt = af.getAbTgt();
		ArrayList<Object> tgts;
		if (tgt != null){
			tgts = tgt.getTargets();
		}
		else{
			tgts = new ArrayList<Object>();
			String defined = params.get("Defined");
			if (defined == null)	// Self
				tgts.add(sa.getActivatingPlayer());
			else{
				if (defined.equals("Each")){
					tgts.add(sa.getActivatingPlayer());
					tgts.add(sa.getActivatingPlayer().getOpponent());
				}
				else if (defined.equals("Targeted")){
					SpellAbility parent;
					do{
						parent = ((Ability_Sub)sa).getParent();
					}while(parent.getTarget() == null && parent.getTarget().getTargetPlayers().size() == 0);
					
					tgts.addAll(parent.getTarget().getTargetPlayers());
				}
			}
		}
		
		String valid = params.get("SacValid");
		
		String msg = params.get("SacMessage");
		if (msg == null)
			msg = valid;
		
		msg = "Sacrifice a " + msg;
		
		for(Object o : tgts){
			Player p = (Player)o;
			if (p.isComputer())
				sacrificeAI(p, amount, valid, sa);
			else
				sacrificeHuman(p, amount, valid, sa, msg);
		}
		
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}
	
	
	private static void sacrificeAI(Player p, int amount, String valid, SpellAbility sa){
		CardList list = AllZoneUtil.getPlayerCardsInPlay(p);
		list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
		
		ComputerUtil.sacrificePermanents(amount, list);
	}
	
	private static void sacrificeHuman(Player p, int amount, String valid, SpellAbility sa, String message){
		CardList list = AllZoneUtil.getPlayerCardsInPlay(p);
		list = list.getValidCards(valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
		
		AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanentsFromList(amount, list, message));
	}
	
}