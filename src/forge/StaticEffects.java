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
	{	//value has to be an array, since certain cards have multiple commands associated with them
		cardToEffectsList.put("Conspiracy", new String[] {"Conspiracy"});
		cardToEffectsList.put("Serra Avatar", new String[] {"Serra_Avatar"});
		cardToEffectsList.put("Avatar",  new String[] {"Ajani_Avatar_Token"});
		cardToEffectsList.put("Windwright Mage", new String[] {"Windwright_Mage"});
		cardToEffectsList.put("Uril, the Miststalker", new String[] {"Uril"});
		cardToEffectsList.put("Rabid Wombat", new String[] {"Rabid_Wombat"});
		cardToEffectsList.put("Kithkin Rabble", new String[] {"Kithkin_Rabble"});
		cardToEffectsList.put("Death's Shadow", new String[] {"Deaths_Shadow"});
		cardToEffectsList.put("Nightmare", new String[] {"Nightmare"});
		cardToEffectsList.put("Aven Trailblazer", new String[] {"Aven_Trailblazer"});
		cardToEffectsList.put("Matca Rioters", new String[] {"Matca_Rioters"});
		cardToEffectsList.put("Rakdos Pit Dragon", new String[] {"Rakdos_Pit_Dragon"});
		cardToEffectsList.put("Nyxathid", new String[] {"Nyxathid"});
		cardToEffectsList.put("Lord of Extinction", new String[] {"Lord_of_Extinction"});
		cardToEffectsList.put("Cantivore", new String[] {"Cantivore"});
		cardToEffectsList.put("Cognivore", new String[] {"Cognivore"});
		cardToEffectsList.put("Mortivore", new String[] {"Mortivore"});
		cardToEffectsList.put("Terravore", new String[] {"Terravore"});
		cardToEffectsList.put("Magnivore", new String[] {"Magnivore"});
		cardToEffectsList.put("Tarmogoyf", new String[] {"Tarmogoyf"});
		cardToEffectsList.put("Lhurgoyf", new String[] {"Lhurgoyf"});
		cardToEffectsList.put("Emperor Crocodile", new String[] {"Emperor_Crocodile"});		
		cardToEffectsList.put("Wolf", new String[] {"Sound_the_Call_Wolf"});
		cardToEffectsList.put("Drove of Elves", new String[] {"Drove_of_Elves"});		
		cardToEffectsList.put("Crowd of Cinders", new String[] {"Crowd_of_Cinders"});
		cardToEffectsList.put("Faerie Swarm", new String[] {"Faerie_Swarm"});
		cardToEffectsList.put("Svogthos, the Restless Tomb", new String[] {"Svogthos_the_Restless_Tomb"});
		cardToEffectsList.put("Multani, Maro-Sorcerer", new String[] {"Multani_Maro_Sorcerer"});
		cardToEffectsList.put("Molimo, Maro-Sorcerer", new String[] {"Molimo_Maro_Sorcerer"});
		cardToEffectsList.put("Maro", new String[] {"Maro"});
		cardToEffectsList.put("Masumaro, First to Live", new String[] {"Masumaro_First_to_Live"});
		cardToEffectsList.put("Adamaro, First to Desire", new String[] {"Adamaro_First_to_Desire"});
		cardToEffectsList.put("Overbeing of Myth", new String[] {"Overbeing_of_Myth"});
		cardToEffectsList.put("Guul Draz Specter", new String[] {"Guul_Draz_Specter"});
		cardToEffectsList.put("Dakkon Blackblade", new String[] {"Dakkon"});
		cardToEffectsList.put("Korlash, Heir to Blackblade", new String[] {"Korlash_Heir_to_Blackblade"});
		cardToEffectsList.put("Student of Warfare", new String[]{"Student_of_Warfare"});
		cardToEffectsList.put("Kargan Dragonlord", new String[]{"Kargan_Dragonlord"});
		cardToEffectsList.put("Transcendent Master", new String[]{"Transcendent_Master"});
		cardToEffectsList.put("Lighthouse Chronologist", new String[] {"Lighthouse_Chronologist"});
		cardToEffectsList.put("Skywatcher Adept", new String[] {"Skywatcher_Adept"});
		cardToEffectsList.put("Caravan Escort", new String[] {"Caravan_Escort"});
		cardToEffectsList.put("Ikiral Outrider", new String[] {"Ikiral_Outrider"});
		cardToEffectsList.put("Knight of Cliffhaven", new String[] {"Knight_of_Cliffhaven"});
		cardToEffectsList.put("Beastbreaker of Bala Ged", new String[] {"Beastbreaker_of_Bala_Ged"});
		cardToEffectsList.put("Hada Spy Patrol", new String[] {"Hada_Spy_Patrol"});
		cardToEffectsList.put("Halimar Wavewatch", new String[] {"Halimar_Wavewatch"});
		cardToEffectsList.put("Nirkana Cutthroat", new String[] {"Nirkana_Cutthroat"});
		cardToEffectsList.put("Zulaport Enforcer", new String[] {"Zulaport_Enforcer"});
		
		cardToEffectsList.put("Champion's Drake", new String[] {"Champions_Drake"});
		cardToEffectsList.put("Soulsurge Elemental", new String[] {"Soulsurge_Elemental"});
		cardToEffectsList.put("Vampire Nocturnus", new String[]{"Vampire_Nocturnus"});
		cardToEffectsList.put("Dauntless Dourbark", new String[] {"Dauntless_Dourbark"});
		cardToEffectsList.put("Gaea's Avenger", new String[] {"Gaeas_Avenger"});
		cardToEffectsList.put("People of the Woods", new String[] {"People_of_the_Woods"});
		cardToEffectsList.put("Old Man of the Sea", new String[] {"Old_Man_of_the_Sea"});
		cardToEffectsList.put("Serpent of the Endless Sea", new String[] {"Serpent_of_the_Endless_Sea"});
		cardToEffectsList.put("Vexing Beetle", new String[] {"Vexing_Beetle"});
		cardToEffectsList.put("Wild Nacatl", new String[] {"Wild_Nacatl"});
		cardToEffectsList.put("Liu Bei, Lord of Shu", new String[] {"Liu_Bei"});
		cardToEffectsList.put("Guul Draz Vampire", new String[] {"Guul_Draz_Vampire"});
		cardToEffectsList.put("Ruthless Cullblade", new String[] {"Ruthless_Cullblade"});
		cardToEffectsList.put("Bloodghast", new String[] {"Bloodghast"});
		cardToEffectsList.put("Bant Sureblade", new String[] {"Bant_Sureblade"});
		cardToEffectsList.put("Esper Stormblade", new String[] {"Esper_Stormblade"});
		cardToEffectsList.put("Grixis Grimblade", new String[] {"Grixis_Grimblade"});
		cardToEffectsList.put("Jund Hackblade", new String[] {"Jund_Hackblade"});
		cardToEffectsList.put("Naya Hushblade", new String[] {"Naya_Hushblade"});
		cardToEffectsList.put("Mystic Enforcer", new String[] {"Mystic_Enforcer"});
		cardToEffectsList.put("Werebear", new String[] {"Werebear"});
		cardToEffectsList.put("Divinity of Pride", new String[] {"Divinity_of_Pride"});
		cardToEffectsList.put("Serra Ascendant", new String[] {"Serra_Ascendant"});
		cardToEffectsList.put("Yavimaya Enchantress", new String[] {"Yavimaya_Enchantress"});
		cardToEffectsList.put("Aura Gnarlid", new String[] {"Aura_Gnarlid"} );
		cardToEffectsList.put("Kor Spiritdancer", new String[] {"Kor_Spiritdancer"});
		cardToEffectsList.put("Knight of the Reliquary", new String[] {"Knight_of_the_Reliquary"});
		cardToEffectsList.put("Zuberi, Golden Feather", new String[] {"Zuberi"});
		cardToEffectsList.put("Loxodon Punisher", new String[] {"Loxodon_Punisher"});
		cardToEffectsList.put("Goblin Gaveleer", new String[] {"Goblin_Gaveleer"});
		cardToEffectsList.put("Master of Etherium", new String[] {"Master_of_Etherium", "Master_of_Etherium_Pump", "Master_of_Etherium_Other"});
		cardToEffectsList.put("Broodstar", new String[] {"Master_of_Etherium"});
		cardToEffectsList.put("Daru Warchief", new String[] {"Daru_Warchief"});
		cardToEffectsList.put("Squirrel Mob", new String[] {"Squirrel_Mob_Other"});
		cardToEffectsList.put("Relentless Rats", new String[] {"Relentless_Rats_Other"});
		cardToEffectsList.put("Privileged Position", new String[] {"Privileged_Position", "Privileged_Position_Other"});
		cardToEffectsList.put("Broodwarden", new String[] {"Broodwarden"});
		cardToEffectsList.put("Elvish Archdruid", new String[] {"Elvish_Archdruid_Pump", "Elvish_Archdruid_Other"});
		cardToEffectsList.put("Knight Exemplar", new String[] {"Knight_Exemplar_Pump", "Knight_Exemplar_Other"});
		cardToEffectsList.put("Wizened Cenn", new String[] {"Wizened_Cenn_Pump", "Wizened_Cenn_Other"});
		cardToEffectsList.put("Lord of the Undead", new String[] {"Lord_of_the_Undead_Pump", "Lord_of_the_Undead_Other"});
		cardToEffectsList.put("Cemetery Reaper", new String[] {"Cemetery_Reaper_Pump", "Cemetery_Reaper_Other"});
		cardToEffectsList.put("Captain of the Watch", new String[] {"Captain_of_the_Watch_Pump", "Captain_of_the_Watch_Other"});
		cardToEffectsList.put("Veteran Swordsmith", new String[] {"Veteran_Swordsmith_Pump", "Veteran_Swordsmith_Other"});
		cardToEffectsList.put("Elvish Champion", new String[] {"Elvish_Champion_Pump","Elvish_Champion_Other"});
		cardToEffectsList.put("Death Baron", new String[] {"Death_Baron_Pump","Death_Baron_Other"});
		cardToEffectsList.put("Lovisa Coldeyes", new String[] {"Lovisa_Coldeyes_Pump"});
		cardToEffectsList.put("Aven Brigadier", new String[] {"Aven_Brigadier_Soldier_Pump", "Aven_Brigadier_Bird_Pump", "Aven_Brigadier_Other"});
		cardToEffectsList.put("Scion of Oona", new String[] {"Scion_of_Oona_Pump", "Scion_of_Oona_Other"});
		cardToEffectsList.put("Covetous Dragon", new String[] {"Covetous_Dragon"});
		cardToEffectsList.put("Phylactery Lich", new String[]{"Phylactery_Lich"});
		cardToEffectsList.put("Tethered Griffin", new String[] {"Tethered_Griffin"});
		cardToEffectsList.put("Shared Triumph", new String[] {"Shared_Triumph"});
		cardToEffectsList.put("Crucible of Fire", new String[] {"Crucible_of_Fire"});
		cardToEffectsList.put("Time of Heroes", new String[] {"Time_of_Heroes"});
		cardToEffectsList.put("Glorious Anthem", new String[] {"Glorious_Anthem"});
		cardToEffectsList.put("Gaea's Anthem", new String[] {"Gaeas_Anthem"});
		cardToEffectsList.put("Honor of the Pure", new String[] {"Honor_of_the_Pure"});
		cardToEffectsList.put("Beastmaster Ascension", new String[] {"Beastmaster_Ascension"});
		cardToEffectsList.put("Spidersilk Armor", new String[] {"Spidersilk_Armor"});
		cardToEffectsList.put("Chainer, Dementia Master", new String[] {"Chainer"});
		cardToEffectsList.put("Eldrazi Monument", new String[] {"Eldrazi_Monument"});
		cardToEffectsList.put("Muraganda Petroglyphs", new String[] {"Muraganda_Petroglyphs"});
		cardToEffectsList.put("Nut Collector", new String[] {"Nut_Collector"});
		cardToEffectsList.put("Engineered Plague", new String[] {"Engineered_Plague"});
		cardToEffectsList.put("Thelonite Hermit", new String[] {"Thelonite_Hermit"});
		cardToEffectsList.put("Deranged Hermit", new String[] {"Deranged_Hermit"});
		cardToEffectsList.put("Jacques le Vert", new String[] {"Jacques"});
		cardToEffectsList.put("Kaysa", new String[] {"Kaysa"});
		cardToEffectsList.put("Meng Huo, Barbarian King", new String[] {"Meng_Huo"});
		cardToEffectsList.put("Eladamri, Lord of Leaves", new String[] {"Eladamri"});
		cardToEffectsList.put("Tolsimir Wolfblood", new String[] {"Tolsimir"});
		cardToEffectsList.put("Imperious Perfect", new String[] {"Imperious_Perfect"});
		cardToEffectsList.put("Castle", new String[] {"Castle"});
		cardToEffectsList.put("Giant Tortoise", new String[] {"Giant_Tortoise"});
		cardToEffectsList.put("Castle Raptors", new String[] {"Castle_Raptors"});
		cardToEffectsList.put("Darksteel Forge", new String[] {"Darksteel_Forge"} );
		cardToEffectsList.put("Akroma's Memorial", new String[] {"Akromas_Memorial"});
		cardToEffectsList.put("Leyline of Singularity", new String[] {"Leyline_of_Singularity"});
		cardToEffectsList.put("Goblin Warchief", new String[] {"Goblin_Warchief"});
		cardToEffectsList.put("Undead Warchief", new String[] {"Undead_Warchief"});
		cardToEffectsList.put("Coat of Arms", new String[] {"Coat_of_Arms"});
		cardToEffectsList.put("Levitation", new String[] {"Levitation"});
		cardToEffectsList.put("Knighthood", new String[] {"Knighthood"});
		cardToEffectsList.put("Absolute Law", new String[] {"Absolute_Law"});
		cardToEffectsList.put("Absolute Grace", new String[] {"Absolute_Grace"});
		cardToEffectsList.put("The Tabernacle at Pendrell Vale", new String[] {"Tabernacle"});
		cardToEffectsList.put("Magus of the Tabernacle", new String[] {"Magus_of_the_Tabernacle"});
		cardToEffectsList.put("Goblin Assault", new String[] {"Goblin_Assault"});
		cardToEffectsList.put("Serra's Blessing", new String[] {"Serras_Blessing"});
		cardToEffectsList.put("Cover of Darkness", new String[] {"Cover_of_Darkness"});
		cardToEffectsList.put("Steely Resolve", new String[] {"Steely_Resolve"});
		cardToEffectsList.put("Concordant Crossroads", new String[] {"Concordant_Crossroads"});
		cardToEffectsList.put("That Which Was Taken", new String[] {"That_Which_Was_Taken"});
		cardToEffectsList.put("Mass Hysteria", new String[] {"Mass_Hysteria"});
		cardToEffectsList.put("Fervor", new String[] {"Fervor"});
		cardToEffectsList.put("Madrush Cyclops", new String[] {"Madrush_Cyclops"});
		cardToEffectsList.put("Rolling Stones", new String[] {"Rolling_Stones"});
		cardToEffectsList.put("Sun Quan, Lord of Wu", new String[] {"Sun_Quan"});
		cardToEffectsList.put("Kinsbaile Cavalier", new String[] {"Kinsbaile_Cavalier"});
		cardToEffectsList.put("Wren's Run Packmaster", new String[] {"Wrens_Run_Packmaster"});
		
		cardToEffectsList.put("Sliver Legion", new String[] {"Sliver_Legion"});
		cardToEffectsList.put("Sidewinder Sliver", new String[] {"Sidewinder_Sliver"});
		cardToEffectsList.put("Essence Sliver", new String[] {"Essence_Sliver"});
		cardToEffectsList.put("Sinew Sliver", new String[] {"Sinew_Sliver"});
		cardToEffectsList.put("Horned Sliver", new String[] {"Horned_Sliver"});
		cardToEffectsList.put("Heart Sliver", new String[] {"Heart_Sliver"});
		cardToEffectsList.put("Reflex Sliver", new String[] {"Reflex_Sliver"});
		cardToEffectsList.put("Gemhide Sliver", new String[] {"Gemhide_Sliver"});
		cardToEffectsList.put("Blade Sliver", new String[] {"Blade_Sliver"});
		
		cardToEffectsList.put("Marrow-Gnawer", new String[] {"Marrow_Gnawer"});
		cardToEffectsList.put("Mul Daya Channelers", new String[] {"Mul_Daya_Channelers"});
		cardToEffectsList.put("Joiner Adept", new String[] {"Joiner_Adept"});
		cardToEffectsList.put("Meddling Mage", new String[] {"Meddling_Mage"});
		cardToEffectsList.put("Gaddock Teeg", new String[] {"Gaddock_Teeg"});
		cardToEffectsList.put("Iona, Shield of Emeria", new String[] {"Iona_Shield_of_Emeria"});
		cardToEffectsList.put("Kor Duelist", new String[] {"Kor_Duelist"});
		cardToEffectsList.put("Keldon Warlord", new String[] {"Keldon_Warlord"});
		cardToEffectsList.put("Heedless One", new String[] {"Heedless_One"});
		cardToEffectsList.put("Omnath, Locus of Mana", new String[] {"Omnath"});
		cardToEffectsList.put("Angry Mob", new String[] {"Angry_Mob"});
		cardToEffectsList.put("Maraxus of Keld", new String[] {"Maraxus_of_Keld"});
		cardToEffectsList.put("Umbra Stalker", new String[] {"Umbra_Stalker"});
		cardToEffectsList.put("Primalcrux",new String[] {"Primalcrux"});
		cardToEffectsList.put("Homarid", new String[] {"Homarid"});
		cardToEffectsList.put("Plague Rats", new String[] {"Plague_Rats"});
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
