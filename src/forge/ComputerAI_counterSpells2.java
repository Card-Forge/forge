package forge;

import java.util.ArrayList;

public class ComputerAI_counterSpells2 {
	
	public static ArrayList<String> KeywordedCounterspells = new ArrayList<String>();
	
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
				"Mystic Snake"/*, "Force of Will",  "Thwart" */
		};
		
		final String[] creature = {
			"Overwhelming Intellect"
		};
		
		final String[] nonCreatureUnlessPay2 = {
			"Spell Pierce"
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
		
		final int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.HumanPlayer);
		
		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
		CardList list = new CardList(hand.getCards());
		list = list.filter(new CardListFilter()
		{
			public boolean addCard(Card c)
			{
				if(KeywordedCounterspells.contains(c.getName()))
				{
					System.out.println("Counterspell is keyworded, casting is a-go.");
					return c.getSpells().get(0).canPlayAI();
				}
				
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
				
				else if (!sa.getSourceCard().isCreature() && usableManaSources < 2) {
					if (checkArray(c, nonCreatureUnlessPay2))
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
		if (c.getSpellAbility().length == 0)
			return false;
		for(String s : type)
		{
			SpellAbility sa = c.getSpellAbility()[0];

			if (s.equals(c.getName()) && ComputerUtil.canPayCost(sa) && !c.isUnCastable())
				return true;
		}
		return false;
	}
}
