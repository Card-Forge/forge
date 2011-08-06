package forge;

public class ComputerAI_counterSpells2 {
	
	public static void counter_Spell(SpellAbility sa)
	{
		CardList counterSpells = getPlayableCounterSpells(sa);
		
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
	
	public static CardList getPlayableCounterSpells(final SpellAbility sa){
		final String[] basic = {
				"Counterspell", "Remand", "Cancel", "Mystic Snake", "Absorb", "Undermine", "Punish Ignorance",
				"Dismiss", "Last Word", "Force of Will", "Thwart"
		};
		
		final String[] creature = {
			"Exclude", "Overwhelming Intellect", "Preemptive Strike", "Remove Soul", "Essence Scatter", "False Summoning"
		};
		
		final String[] nonCreature = {
			""
		};
		
		final String[] unlessPay1 = { 
			"Force Spike", "Daze", "Runeboggle", "Spell Snip"
		};
		
		final String[] unlessPay3 = {
			"Mana Leak"
		};
		
		final String[] unlessPay4 = {
			"Convolute"
		};

		final int usableManaSources = CardFactoryUtil.getUsableManaSources(Constant.Player.Human);
		
		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
		CardList list = new CardList(hand.getCards());
		list = list.filter(new CardListFilter()
		{
			public boolean addCard(Card c)
			{
				if (CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost()) == 2)
					if (c.getName().equals("Spell Snare"))
						return true;
				
				if (usableManaSources == 0) {
					if (checkArray(c, unlessPay1))
						return true;
				}
				
				if (usableManaSources < 3) {
					if (checkArray(c, unlessPay3))
						return true;
				}
				
				if (usableManaSources < 4) {
					if (checkArray(c, unlessPay4))
						return true;
				}
				
				if (sa.getSourceCard().isCreature()) {
					if (checkArray(c, creature))
						return true;
				}

				else if (!sa.getSourceCard().isCreature()) {
					if (checkArray(c, nonCreature))
						return true;
				}
				
				if (checkArray(c, basic))
					return true;
				
				return false;
			}
		});
		return list;

	}
	
	public static boolean checkArray(Card c, String[] type)
	{
		for(String s : type)
		{
			SpellAbility sa = c.getSpellAbility()[0];
			if (s.equals(c.getName()) && ComputerUtil.canPayCost(sa))
				return true;
		}
		return false;
	}
}
