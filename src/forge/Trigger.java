package forge;

import java.util.HashMap;

public class Trigger {
	
	private HashMap<String,String> mapParams = new HashMap<String,String>();
	public HashMap<String,String> getMapParams()
	{
		return mapParams;
	}
	
	private SpellAbility overridingAbility = null;
	public SpellAbility getOverridingAbility()
	{
		return overridingAbility;
	}
	public void setOverridingAbility(SpellAbility sa)
	{
		overridingAbility = sa;
	}
	
	private Card hostCard;
	public Card getHostCard()
	{
		return hostCard;
	}
	public void setHostCard(Card c)
	{
		hostCard = c;
	}
	
	public Trigger(HashMap<String,String> params, Card host)
	{
		mapParams = params;
		hostCard = host;
	}
}
