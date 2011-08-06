package forge;
import java.util.*;

import com.esotericsoftware.minlog.Log;


public class StaticEffects
{
	//this is used to keep track of all state-based effects in play:
	private HashMap<String, Integer> stateBasedMap = new HashMap<String, Integer>();
	
	//this is used to define all cards that are state-based effects, and map the corresponding commands to their cardnames
	private static HashMap<String, String[]> cardToEffectsList = new HashMap<String, String[]>();
	
	public StaticEffects() 
	{
		initStateBasedEffectsList();
	}
	
	public void initStateBasedEffectsList()
	{	
		//value has to be an array, since certain cards have multiple commands associated with them
		
		//cardToEffectsList.put("Adamaro, First to Desire", new String[] {"Adamaro_First_to_Desire"});
		cardToEffectsList.put("Akroma's Memorial", new String[] {"Akromas_Memorial"});
		//cardToEffectsList.put("Angry Mob", new String[] {"Angry_Mob"});
		cardToEffectsList.put("Avatar",  new String[] {"Ajani_Avatar_Token"});
		//cardToEffectsList.put("Aven Trailblazer", new String[] {"Aven_Trailblazer"});
		
		cardToEffectsList.put("Beastbreaker of Bala Ged", new String[] {"Beastbreaker_of_Bala_Ged"});
		//cardToEffectsList.put("Bloodghast", new String[] {"Bloodghast"});
		//cardToEffectsList.put("Broodstar", new String[] {"Master_of_Etherium"});
		cardToEffectsList.put("Broodwarden", new String[] {"Broodwarden"});
		
		//cardToEffectsList.put("Cantivore", new String[] {"Cantivore"});
		cardToEffectsList.put("Caravan Escort", new String[] {"Caravan_Escort"});
		cardToEffectsList.put("Champion's Drake", new String[] {"Champions_Drake"});
		cardToEffectsList.put("Coat of Arms", new String[] {"Coat_of_Arms"});
		//cardToEffectsList.put("Cognivore", new String[] {"Cognivore"});
		cardToEffectsList.put("Conspiracy", new String[] {"Conspiracy"});
		cardToEffectsList.put("Covetous Dragon", new String[] {"Covetous_Dragon"});
		//cardToEffectsList.put("Crowd of Cinders", new String[] {"Crowd_of_Cinders"});
		
		//cardToEffectsList.put("Dakkon Blackblade", new String[] {"Dakkon_Blackblade"});
		//cardToEffectsList.put("Dauntless Dourbark", new String[] {"Dauntless_Dourbark"});
		//cardToEffectsList.put("Drove of Elves", new String[] {"Drove_of_Elves"});
		
		//cardToEffectsList.put("Eldrazi Monument", new String[] {"Eldrazi_Monument"});
		cardToEffectsList.put("Emperor Crocodile", new String[] {"Emperor_Crocodile"});
		
		//cardToEffectsList.put("Faerie Swarm", new String[] {"Faerie_Swarm"});
		
		cardToEffectsList.put("Gaddock Teeg", new String[] {"Gaddock_Teeg"});
		//cardToEffectsList.put("Gaea's Avenger", new String[] {"Gaeas_Avenger"});
		cardToEffectsList.put("Gemhide Sliver", new String[] {"Gemhide_Sliver"});
		//cardToEffectsList.put("Goblin Assault", new String[] {"Goblin_Assault"});
		//cardToEffectsList.put("Goblin Gaveleer", new String[] {"Goblin_Gaveleer"});
		//cardToEffectsList.put("Guul Draz Specter", new String[] {"Guul_Draz_Specter"});
		//cardToEffectsList.put("Guul Draz Vampire", new String[] {"Guul_Draz_Vampire"});
		
		cardToEffectsList.put("Hada Spy Patrol", new String[] {"Hada_Spy_Patrol"});
		cardToEffectsList.put("Halimar Wavewatch", new String[] {"Halimar_Wavewatch"});
		cardToEffectsList.put("Heedless One", new String[] {"Heedless_One"});
		cardToEffectsList.put("Homarid", new String[] {"Homarid"});
		
		cardToEffectsList.put("Ikiral Outrider", new String[] {"Ikiral_Outrider"});
		cardToEffectsList.put("Iona, Shield of Emeria", new String[] {"Iona_Shield_of_Emeria"});
		
		cardToEffectsList.put("Joiner Adept", new String[] {"Joiner_Adept"});
		
		cardToEffectsList.put("Kargan Dragonlord", new String[]{"Kargan_Dragonlord"});
		//cardToEffectsList.put("Keldon Warlord", new String[] {"Keldon_Warlord"});
		cardToEffectsList.put("Kithkin Rabble", new String[] {"Kithkin_Rabble"});
		cardToEffectsList.put("Knight of Cliffhaven", new String[] {"Knight_of_Cliffhaven"});
		cardToEffectsList.put("Korlash, Heir to Blackblade", new String[] {"Korlash_Heir_to_Blackblade"});
		
		cardToEffectsList.put("Leyline of Singularity", new String[] {"Leyline_of_Singularity"});
		cardToEffectsList.put("Lhurgoyf", new String[] {"Lhurgoyf"});
		cardToEffectsList.put("Lighthouse Chronologist", new String[] {"Lighthouse_Chronologist"});
		cardToEffectsList.put("Liu Bei, Lord of Shu", new String[] {"Liu_Bei"});
		cardToEffectsList.put("Lord of Extinction", new String[] {"Lord_of_Extinction"});
		cardToEffectsList.put("Loxodon Punisher", new String[] {"Loxodon_Punisher"});
		
		cardToEffectsList.put("Magnivore", new String[] {"Magnivore"});
		cardToEffectsList.put("Magus of the Tabernacle", new String[] {"Magus_of_the_Tabernacle"});
		cardToEffectsList.put("Maraxus of Keld", new String[] {"Maraxus_of_Keld"});
		//cardToEffectsList.put("Maro", new String[] {"Maro"});
		//cardToEffectsList.put("Master of Etherium", new String[] {"Master_of_Etherium"});
		//cardToEffectsList.put("Masumaro, First to Live", new String[] {"Masumaro_First_to_Live"});
		cardToEffectsList.put("Matca Rioters", new String[] {"Matca_Rioters"});
		cardToEffectsList.put("Meddling Mage", new String[] {"Meddling_Mage"});
		//cardToEffectsList.put("Molimo, Maro-Sorcerer", new String[] {"Molimo_Maro_Sorcerer"});
		cardToEffectsList.put("Mortivore", new String[] {"Mortivore"});
		cardToEffectsList.put("Mul Daya Channelers", new String[] {"Mul_Daya_Channelers"});
		//cardToEffectsList.put("Multani, Maro-Sorcerer", new String[] {"Multani_Maro_Sorcerer"});
		cardToEffectsList.put("Muraganda Petroglyphs", new String[] {"Muraganda_Petroglyphs"});
		
		//cardToEffectsList.put("Nightmare", new String[] {"Nightmare"});
		cardToEffectsList.put("Nirkana Cutthroat", new String[] {"Nirkana_Cutthroat"});
		//cardToEffectsList.put("Nut Collector", new String[] {"Nut_Collector"});
		cardToEffectsList.put("Nyxathid", new String[] {"Nyxathid"});
		
		cardToEffectsList.put("Old Man of the Sea", new String[] {"Old_Man_of_the_Sea"});
		cardToEffectsList.put("Omnath, Locus of Mana", new String[] {"Omnath"});
		//cardToEffectsList.put("Overbeing of Myth", new String[] {"Overbeing_of_Myth"});
		
		//cardToEffectsList.put("People of the Woods", new String[] {"People_of_the_Woods"});
		cardToEffectsList.put("Phylactery Lich", new String[]{"Phylactery_Lich"});
		cardToEffectsList.put("Plague Rats", new String[] {"Plague_Rats"});
		cardToEffectsList.put("Primalcrux",new String[] {"Primalcrux"});
		
		//cardToEffectsList.put("Ruthless Cullblade", new String[] {"Ruthless_Cullblade"});
		
		//cardToEffectsList.put("Serpent of the Endless Sea", new String[] {"Serpent_of_the_Endless_Sea"});
		//cardToEffectsList.put("Serra Avatar", new String[] {"Serra_Avatar"});
		cardToEffectsList.put("Skywatcher Adept", new String[] {"Skywatcher_Adept"});
		//cardToEffectsList.put("Soulsurge Elemental", new String[] {"Soulsurge_Elemental"});
		cardToEffectsList.put("Student of Warfare", new String[]{"Student_of_Warfare"});
		cardToEffectsList.put("Svogthos, the Restless Tomb", new String[] {"Svogthos_the_Restless_Tomb"});
		
		cardToEffectsList.put("Tarmogoyf", new String[] {"Tarmogoyf"});
		cardToEffectsList.put("Terravore", new String[] {"Terravore"});
		cardToEffectsList.put("Tethered Griffin", new String[] {"Tethered_Griffin"});
		cardToEffectsList.put("That Which Was Taken", new String[] {"That_Which_Was_Taken"});
		cardToEffectsList.put("The Tabernacle at Pendrell Vale", new String[] {"Tabernacle"});
		cardToEffectsList.put("Time of Heroes", new String[] {"Time_of_Heroes"});
		cardToEffectsList.put("Transcendent Master", new String[]{"Transcendent_Master"});
		
		cardToEffectsList.put("Umbra Stalker", new String[] {"Umbra_Stalker"});
		//cardToEffectsList.put("Uril, the Miststalker", new String[] {"Uril"});
		
		cardToEffectsList.put("Vampire Nocturnus", new String[]{"Vampire_Nocturnus"});
		cardToEffectsList.put("Vexing Beetle", new String[] {"Vexing_Beetle"});
		
		cardToEffectsList.put("Windwright Mage", new String[] {"Windwright_Mage"});
		cardToEffectsList.put("Wolf", new String[] {"Sound_the_Call_Wolf"});
		cardToEffectsList.put("Wren's Run Packmaster", new String[] {"Wrens_Run_Packmaster"});
		
		cardToEffectsList.put("Zulaport Enforcer", new String[] {"Zulaport_Enforcer"});
	}
	
	public HashMap<String, String[]> getCardToEffectsList()
	{
		return cardToEffectsList;
	}
	
	public void addStateBasedEffect(String s)
	{
		if (stateBasedMap.containsKey(s))
			stateBasedMap.put(s, stateBasedMap.get(s)+1);
		else 
			stateBasedMap.put(s, 1);
	}
	
	public void removeStateBasedEffect(String s)
	{
		if (stateBasedMap.containsKey(s)) {
			stateBasedMap.put(s, stateBasedMap.get(s)-1);
			if(stateBasedMap.get(s) == 0)
				stateBasedMap.remove(s);
		}
	}
	
	public HashMap<String, Integer> getStateBasedMap()
	{
		return stateBasedMap;
	}
	
	public void reset()
	{
		stateBasedMap.clear();
	}
	
	public void rePopulateStateBasedList()
	{
		reset();
		PlayerZone playerZone = AllZone.getZone(Constant.Zone.Play,
				AllZone.HumanPlayer);
		PlayerZone computerZone = AllZone.getZone(Constant.Zone.Play,
				AllZone.ComputerPlayer);

		CardList cards = new CardList();
		cards.addAll(playerZone.getCards());
		cards.addAll(computerZone.getCards());
		
		
		Log.debug("== Start add state effects ==");
		for (int i=0;i<cards.size();i++)
		{
			Card c = cards.get(i);
			if (cardToEffectsList.containsKey(c.getName()) )
			{
				String[] effects = getCardToEffectsList().get(c.getName());
				for (String effect : effects) {
					addStateBasedEffect(effect);
					Log.debug("Added " + effect);
				}
			}
			if (c.isEmblem() && !CardFactoryUtil.checkEmblemKeyword(c).equals(""))
			{
				String s = CardFactoryUtil.checkEmblemKeyword(c);
				addStateBasedEffect(s);
				Log.debug("Added " + s);
			}
		}
		Log.debug("== End add state effects ==");
		
	}
}
