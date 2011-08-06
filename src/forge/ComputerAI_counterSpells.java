package forge;

public class ComputerAI_counterSpells {
	
	public static void counter_Spell(SpellAbility sa)
	{
		if (sa.getSourceCard().isCreature()) 
			counter_CreatureSpell(sa);
		
		else 
			counter_NonCreatureSpell(sa);
	}
	
	public static void counter_CreatureSpell(SpellAbility sa)
	{
		if (!hasPlayableCounterSpells() && !hasPlayableCreatureCounterSpells())
			return;
		
		CardList counterSpells;
		if (hasPlayableCreatureCounterSpells())
			counterSpells = getPlayableCreatureCounterSpells();
		else
			counterSpells = getPlayableCounterSpells();
		
		boolean countered = false;
		for (Card var:counterSpells)
		{
			if (countered)
				break;
			else if (CardUtil.getConvertedManaCost(var.getManaCost()) <= CardUtil.getConvertedManaCost(sa) ||
					(var.getName().equals("Overwhelming Intellect") && CardUtil.getConvertedManaCost(sa) >= 3 ))
			{
				SpellAbility sp = var.getSpellAbility()[0];
				//ComputerUtil.playNoStack(sp);
				ComputerUtil.playStack(sp);
				countered = true;
			}
		}
		
	}

	public static void counter_NonCreatureSpell(SpellAbility sa)
	{
		if (!hasPlayableCounterSpells())
			return;
		
		CardList counterSpells;
		counterSpells = getPlayableCounterSpells();
		
		boolean countered = false;
		for (Card var:counterSpells)
		{
			if (countered)
				break;
			else if (CardUtil.getConvertedManaCost(var.getManaCost()) <= CardUtil.getConvertedManaCost(sa))
			{
				SpellAbility sp = var.getSpellAbility()[0];
				//ComputerUtil.playNoStack(sp);
				ComputerUtil.playStack(sp);
				countered = true;
			}
		}
	}
	
	public static boolean hasPlayableCounterSpells()
	{
		return getPlayableCounterSpells().size() > 0;		
	}
	
	public static boolean hasPlayableCreatureCounterSpells()
	{
		return getPlayableCreatureCounterSpells().size() > 0;		
	}
	
	public static CardList getPlayableCounterSpells()
	{
		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
		CardList list = new CardList(hand.getCards());
		list = list.filter(new CardListFilter()
		{
			
			
			public boolean addCard(Card c) {
				if (c.getSpellAbility().length == 0)
		               return false;

				SpellAbility sa = c.getSpellAbility()[0];
				return c.getName().equals("Counterspell") || c.getName().equals("Cancel") ||
					   c.getName().equals("Remand") || c.getName().equals("Mystic Snake") ||
				 	   c.getName().equals("Absorb") || c.getName().equals("Undermine") ||
				 	   c.getName().equals("Punish Ignorance") || c.getName().equals("Dismiss") || 
				 	   c.getName().equals("Last Word") || c.getName().equals("Dissipate")
					   && ComputerUtil.canPayCost(sa);
			}	
		});
		
		return list;
	}
	public static CardList getPlayableCreatureCounterSpells()
	{
		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
		CardList list = new CardList(hand.getCards());
		list = list.filter(new CardListFilter()
		{
			public boolean addCard(Card c) {
				if (c.getSpellAbility().length == 0)
		               return false;
				SpellAbility sa = c.getSpellAbility()[0];
				return c.getName().equals("Exclude") || c.getName().equals("Overwhelming Intellect") ||
					   c.getName().equals("Preemptive Strike") || c.getName().equals("False Summoning") ||
				 	   c.getName().equals("Essence Scatter") || c.getName().equals("Remove Soul")
					   && ComputerUtil.canPayCost(sa);
			}	
		});
		
		return list;
	}
}
