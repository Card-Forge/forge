package forge;
import java.util.*;

public class SpellAbilityUtil
{   
    //only works for MONO-COLORED spells
    static public UndoCommand getPayCostCommand(final Player player, final String manaCost)
    {
	return new UndoCommand()
	{
	    /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CardList tapped = new CardList();
	    public void execute()
	    {
		tapped.clear();
		
		int n = CardUtil.getConvertedManaCost(manaCost);
		CardList mana = getAvailableMana(player);
		if(mana.size() < n)
		    throw new RuntimeException("SpellAbilityUtil : payCost() error, not enough mana, trying to pay for the mana cost " +manaCost +" , player " +player);

		for(int i = 0; i < n; i++)
		{
		    mana.get(i).tap();	
		    tapped.add(mana.get(i));
		}
	    }
	    public void undo()
	    {
		for(int i = 0; i < tapped.size(); i++)
		    tapped.get(i).untap();

		tapped.clear();	    
	    }
	};//UndoCommand
    }//payCost()
    
    static public CardList getAvailableMana(Player player)
    {
	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	CardList all = new CardList(play.getCards());
	CardList mana = all.filter(new CardListFilter()
	{
	    public boolean addCard(Card c)
	    {
		if(c.isTapped())
		    return false;
		
		ArrayList<Ability_Mana> a = c.getManaAbility();
		for(Ability_Mana am : a)
			return am.isBasic();
		
		return false;
	    }//addCard()
	});//CardListFilter
	
	return mana;
    }//getUntappedMana     
}