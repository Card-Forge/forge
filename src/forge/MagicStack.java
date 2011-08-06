package forge;

import java.util.*;

import com.esotericsoftware.minlog.Log;

public class MagicStack extends MyObservable {
	private ArrayList<SpellAbility> stack = new ArrayList<SpellAbility>();
	private ArrayList<SpellAbility> frozenStack = new ArrayList<SpellAbility>();
	private boolean frozen = false;

	private Object StormCount;
	private Object PlayerSpellCount;
	private Object PlayerCreatureSpellCount;
	private Object ComputerSpellCount;
	private Object ComputerCreatureSpellCount;

	public void reset() {
		stack.clear();
		frozen = false;
		frozenStack.clear();
		this.updateObservers();
	}

	public void freezeStack() {
		frozen = true;
	}

	public void addAndUnfreeze(SpellAbility sp) {
		frozen = false;
		this.add(sp);
		unfreezeStack();
	}

	public void unfreezeStack() {
		frozen = false;
		while (!frozenStack.isEmpty()) {
			this.add(frozenStack.get(0));
			frozenStack.remove(0);
		}
	}

	public void clearFrozen() {
		// todo: frozen triggered abilities and undoable costs have nasty
		// consequences
		frozen = false;
		frozenStack.clear();
	}

	public void add(SpellAbility sp, boolean useX) {
		if (!useX)
			this.add(sp);
		else {
			// TODO make working triggered abilities!
			if (sp instanceof Ability_Mana || sp instanceof Ability_Triggered)
				sp.resolve();
			else {
				push(sp);
				if (sp.getTargetCard() != null)
					CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());
			}
		}
	}

	public ManaCost GetMultiKickerSpellCostChange(SpellAbility sa) {
		int Max = 25;
		String[] Numbers = new String[Max];
		for (int no = 0; no < Max; no++)
			Numbers[no] = String.valueOf(no);
		
		ManaCost manaCost = new ManaCost(sa.getManaCost());
		String Mana = manaCost.toString();
		
		int MultiKickerPaid = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid;
		
		String Number_ManaCost = " ";
		
		if (Mana.toString().length() == 1)
			Number_ManaCost = Mana.toString().substring(0, 1);
		
		else if (Mana.toString().length() == 0)
			Number_ManaCost = "0"; // Should Never Occur
		
		else
			Number_ManaCost = Mana.toString().substring(0, 2);
		Number_ManaCost = Number_ManaCost.trim();

		for (int check = 0; check < Max; check++) {
			if (Number_ManaCost.equals(Numbers[check])) {

				if (check - MultiKickerPaid < 0) {
					MultiKickerPaid = MultiKickerPaid - check;
					AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
					Mana = Mana.replaceFirst(String.valueOf(check), "0");
				} 
				else {
					Mana = Mana.replaceFirst(String.valueOf(check), String.valueOf(check - MultiKickerPaid));
					MultiKickerPaid = 0;
					AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
				}
			}
			Mana = Mana.trim();
			if (Mana.equals(""))
				Mana = "0";
			manaCost = new ManaCost(Mana);
		}
		String Color_cut = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored;

		for (int Colored_Cut = 0; Colored_Cut < Color_cut.length(); Colored_Cut++) {
			if ("WUGRB".contains(Color_cut.substring(Colored_Cut, Colored_Cut + 1))) {
				
				if (!Mana.equals(Mana.replaceFirst((Color_cut.substring(Colored_Cut, Colored_Cut + 1)), ""))) {
					Mana = Mana.replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
					AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored = AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored
							.replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
					Mana = Mana.trim();
					if (Mana.equals(""))
						Mana = "0";
					manaCost = new ManaCost(Mana);
				}
			}
		}

		return manaCost;
	}

	public void add(SpellAbility sp) {
		if (sp instanceof Ability_Mana) { // Mana Abilities go straight through
			sp.resolve();
			return;
		}

		if (frozen) {
			frozenStack.add(sp);
			return;
		}

		// if activating player slips through the cracks, assign activating
		// Player to the controller here
		if (null == sp.getActivatingPlayer()) {
			sp.setActivatingPlayer(sp.getSourceCard().getController());
			System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
		}
		// WheneverKeyword Test
		boolean ActualEffectTriggered = false;
		if (sp.getSourceCard().getKeyword().toString().contains("WheneverKeyword")) {
			ArrayList<String> a = sp.getSourceCard().getKeyword();
			int WheneverKeywords = 0;
			int WheneverKeyword_Number[] = new int[a.size()];
			
			for (int x = 0; x < a.size(); x++)
				if (a.get(x).toString().startsWith("WheneverKeyword")) {
					WheneverKeyword_Number[WheneverKeywords] = x;
					WheneverKeywords = WheneverKeywords + 1;
				}

			for (int CKeywords = 0; CKeywords < WheneverKeywords; CKeywords++) {
				String parse = sp.getSourceCard().getKeyword().get(
						WheneverKeyword_Number[CKeywords]).toString();
				String k[] = parse.split(":");
				if (k[1].equals("ActualSpell")
						&& ActualEffectTriggered == false) {
					AllZone.GameAction.CheckWheneverKeyword(sp.getSourceCard(),
							"ActualSpell", null);
					sp.getSourceCard().removeIntrinsicKeyword(parse);
					ActualEffectTriggered = true;
				}
			}

		}
		if (!ActualEffectTriggered) {
			// // WheneverKeyword Test: Added one } at end
			if (sp instanceof Ability_Triggered || sp instanceof Ability_Static)
				// TODO make working triggered ability
				sp.resolve();
			else {
				if (sp.isKickerAbility()) {
					sp.getSourceCard().setKicked(true);
					SpellAbility[] sa = sp.getSourceCard().getSpellAbility();
					int AbilityNumber = 0;
					
					for (int i = 0; i < sa.length; i++)
						if (sa[i] == sp)
							AbilityNumber = i;
					
					sp.getSourceCard().setAbilityUsed(AbilityNumber);
				}
				if (sp.getSourceCard().isCopiedSpell())
					push(sp);
				
				if (!sp.isMultiKicker() && !sp.isXCost() && !sp.getSourceCard().isCopiedSpell()) {
					push(sp);
				} 
				
				else if (sp.isXCost() && !sp.getSourceCard().isCopiedSpell()) {
					final SpellAbility sa = sp;
					final Ability ability = new Ability(sp.getSourceCard(), sa.getXManaCost()) {
						public void resolve() {
							Card crd = this.getSourceCard();
							crd.addXManaCostPaid(1);
						}
					};

					final Command unpaidCommand = new Command() {
						private static final long serialVersionUID = -3342222770086269767L;

						public void execute() {
							push(sa);
						}
					};

					final Command paidCommand = new Command() {
						private static final long serialVersionUID = -2224875229611007788L;

						public void execute() {
							ability.resolve();
							Card crd = sa.getSourceCard();
							AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Pay X cost for " + crd.getName()
											 + " (X=" + crd.getXManaCostPaid() + ")\r\n", 
													ability.getManaCost(), this, unpaidCommand, true));
						}
					};
					
					Card crd = sa.getSourceCard();
					if (sp.getSourceCard().getController().equals(AllZone.HumanPlayer)) {
						AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Pay X cost for " +
										sp.getSourceCard().getName() + " (X=" + crd.getXManaCostPaid() + ")\r\n", 
												ability.getManaCost(), paidCommand, unpaidCommand, true));
					} 
					
					else // computer
					{
						int neededDamage = CardFactoryUtil.getNeededXDamage(sa);

						while (ComputerUtil.canPayCost(ability) && neededDamage != sa.getSourceCard().getXManaCostPaid()) {
							ComputerUtil.playNoStack(ability);
						}
						push(sa);
					}
				} 
				
				else if (sp.isMultiKicker() && !sp.getSourceCard().isCopiedSpell()){
					// both X and multi is not supported yet
				
					final SpellAbility sa = sp;
					final Ability ability = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost()) {
						public void resolve() {
							this.getSourceCard().addMultiKickerMagnitude(1);
						}
					};

					final Command unpaidCommand = new Command() {
						private static final long serialVersionUID = -3342222770086269767L;

						public void execute() {
							push(sa);
						}
					};

					final Command paidCommand = new Command() {
						private static final long serialVersionUID = -6037161763374971106L;

						public void execute() {
							ability.resolve();
							ManaCost manaCost = GetMultiKickerSpellCostChange(ability);
							if (manaCost.isPaid()) {
								this.execute();
							} else {
								if (AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid == 0
										&& AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
									
									AllZone.InputControl.setInput(new Input_PayManaCost_Ability(
											"Multikicker for "+ sa.getSourceCard() + "\r\n"
											+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n", 
											manaCost.toString(), this, unpaidCommand));
								} 
								
								else {
									AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for "
											+ sa.getSourceCard() + "\r\n" + "Mana in Reserve: "
											+ ((AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid != 0) ? 
											AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid : "")
											+ AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored + "\r\n"
											+ "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n", 
									manaCost.toString(), this, unpaidCommand));
								}
							}
						}
					};

					if (sp.getSourceCard().getController().equals(
							AllZone.HumanPlayer)) {
						ManaCost manaCost = GetMultiKickerSpellCostChange(ability);

						if (manaCost.isPaid()) {
							paidCommand.execute();
						} else {
							if (AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid == 0
									&& AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
								AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for "
									+ sa.getSourceCard() + "\r\n" + "Times Kicked: " 
									+ sa.getSourceCard().getMultiKickerMagnitude() + "\r\n", 
								manaCost.toString(), paidCommand, unpaidCommand));
							} else {
								AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Multikicker for "
									+ sa.getSourceCard() + "\r\n" + "Mana in Reserve: " + 
									((AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid != 0) ? 
											AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid: "")
									+ AllZone.GameAction.CostCutting_GetMultiMickerManaCostPaid_Colored
									+ "\r\n" + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n", 
									manaCost.toString(), paidCommand, unpaidCommand));
							}
						}
					} 
					
					else // computer
					{
						while (ComputerUtil.canPayCost(ability))
							ComputerUtil.playNoStack(ability);
						push(sa);
					}
				}

			}
		}
		if (sp.getTargetCard() != null)
			CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());
	}

	public int size() {
		return stack.size();
	}

	public void push(SpellAbility sp) {
		if (null == sp.getActivatingPlayer()) {
			sp.setActivatingPlayer(sp.getSourceCard().getController());
			System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
		}
		
		stack.add(0, sp);

		this.updateObservers();
		
		if (sp.isSpell() && !sp.getSourceCard().isCopiedSpell()) {
			Phase.StormCount = Phase.StormCount + 1;
			if (sp.getSourceCard().getController() == AllZone.HumanPlayer) {
				Phase.PlayerSpellCount = Phase.PlayerSpellCount + 1;
				if (sp.getSourceCard().isCreature()) {
					Phase.PlayerCreatureSpellCount = Phase.PlayerCreatureSpellCount + 1;
				}
			} 
			
			else {
				Phase.ComputerSpellCount = Phase.ComputerSpellCount + 1;
				if (sp.getSourceCard().isCreature()) {
					Phase.ComputerCreatureSpellCount = Phase.ComputerCreatureSpellCount + 1;
				}
			}
			// attempt to counter human spell (todo(sol) fix this to go with new ai stuff)
			if (sp.getSourceCard().getController().equals(AllZone.HumanPlayer) && CardFactoryUtil.isCounterable(sp.getSourceCard()))
				ComputerAI_counterSpells2.counter_Spell(sp);

			GameActionUtil.executePlayCardEffects(sp);

		}
	}

	public void resolveStack() {
		// Resolving the Stack
		GuiDisplayUtil.updateGUI();
		this.freezeStack();	// freeze the stack while we're in the middle of resolving
		AllZone.InputControl.setResolving(true);
		SpellAbility sa = AllZone.Stack.pop();
		AllZone.Phase.setPriorityPlayer(AllZone.Phase.getPlayerTurn());
		Card c = sa.getSourceCard();
		boolean fizzle = false;

		if (sa.getTargetCard() != null) {
			// Fizzling will only work for Abilities that use the Target class,
			// since the info isn't available otherwise
			fizzle = !CardFactoryUtil.isTargetStillValid(sa, sa.getTargetCard());
		} 
		else if (sa.getTargetPlayer() != null) {
			fizzle = !CardFactoryUtil.canTarget(c, sa.getTargetPlayer());
		}

		if (!fizzle) {
			final Card crd = c;
			if (sa.isBuyBackAbility()) {
				c.addReplaceMoveToGraveyardCommand(new Command() {
					private static final long serialVersionUID = -2559488318473330418L;

					public void execute() {
						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, crd.getOwner());
						AllZone.GameAction.moveTo(hand, crd);
					}
				});
			}

			// To stop Copied Spells from going into the graveyard.
			if (sa.getSourceCard().isCopiedSpell()) {
				c.addReplaceMoveToGraveyardCommand(new Command() {
					private static final long serialVersionUID = -2559488318473330418L;

					public void execute() {
					}
				});
			}
			sa.resolve();

			if (sa.getSourceCard().getKeyword().contains("Draw a card.")
					&& !(sa.getSourceCard().getKeyword().contains("Ripple:4") && sa.isAbility()))
				sa.getSourceCard().getController().drawCard();

			if (sa.getSourceCard().getKeyword().contains("Proliferate"))
				AllZone.GameAction.getProliferateAbility(sa.getSourceCard(), "0").resolve();

			for (int i = 0; i < sa.getSourceCard().getKeyword().size(); i++) {
				String k = sa.getSourceCard().getKeyword().get(i);
				if (k.startsWith("Scry")) {
					String kk[] = k.split(" ");
					sa.getSourceCard().getController().scry(Integer.parseInt(kk[1]));
				}
			}
		} else {
			// Spell fizzles, alert player?
			Log.debug(c.getName() + " ability fizzles.");
		}

		// Handle cards that need to be moved differently
		if (sa.isFlashBackAbility())
			c.replaceMoveToGraveyard();

		else if (fizzle)
			AllZone.GameAction.moveToGraveyard(c);

		else if ((c.isInstant() || c.isSorcery()) && (!c.getName().startsWith("Beacon of"))
				&& (!c.getName().startsWith("Pulse of the")))												
		{
			if (c.getReplaceMoveToGraveyard().size() == 0)
				AllZone.GameAction.moveToGraveyard(c);
			else
				c.replaceMoveToGraveyard();
		}

		AllZone.InputControl.setResolving(false);
		this.unfreezeStack(); // unfreeze the stack once we're done resolving
		AllZone.GameAction.checkStateEffects();
		sa.resetSacrificedCost();
		
		AllZone.Phase.setNeedToNextPhase(false);

		if (AllZone.Phase.inCombat()) 
			CombatUtil.showCombat();

		GuiDisplayUtil.updateGUI();
	}

	public SpellAbility pop() {
		SpellAbility sp = (SpellAbility) stack.remove(0);
		this.updateObservers();
		return sp;
	}

	// index = 0 is the top, index = 1 is the next to top, etc...
	public SpellAbility peek(int index) {
		return (SpellAbility) stack.get(index);
	}

	public SpellAbility peek() {
		return peek(0);
	}

	public ArrayList<Card> getSourceCards() {
		ArrayList<Card> a = new ArrayList<Card>();
		Iterator<SpellAbility> it = stack.iterator();
		while (it.hasNext())
			a.add(((SpellAbility) it.next()).getSourceCard());

		return a;
	}

	public void setStormCount(Object stormCount) {
		StormCount = stormCount;
	}

	public Object getStormCount() {
		return StormCount;
	}

	public void setPlayerCreatureSpellCount(Object playerCreatureSpellCount) {
		PlayerCreatureSpellCount = playerCreatureSpellCount;
	}

	public Object getPlayerCreatureSpellCount() {
		return PlayerCreatureSpellCount;
	}

	public void setPlayerSpellCount(Object playerSpellCount) {
		PlayerSpellCount = playerSpellCount;
	}

	public Object getPlayerSpellCount() {
		return PlayerSpellCount;
	}

	public void setComputerSpellCount(Object computerSpellCount) {
		ComputerSpellCount = computerSpellCount;
	}

	public Object getComputerSpellCount() {
		return ComputerSpellCount;
	}

	public void setComputerCreatureSpellCount(Object computerCreatureSpellCount) {
		ComputerCreatureSpellCount = computerCreatureSpellCount;
	}

	public Object getComputerCreatureSpellCount() {
		return ComputerCreatureSpellCount;
	}

}
