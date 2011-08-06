package forge;

import java.util.ArrayList;

public class SetInfoUtil {
	
	private static String[][] sets = {
		{"A", "LEA", "Limited Edition Alpha"},
		{"B", "LEB", "Limited Edition Beta"},
		{"U", "2ED", "Unlimited"},
		{"AN", "ARN", "Arabian Nights"},
		{"AQ", "ATQ", "Antiquities"},
		{"R", "3ED", "Revised Edition"},
		{"LG", "LEG", "Legends"},
		{"FE", "FEM", "Fallen Empires"},
		{"DK", "DRK", "The Dark"},
		{"4E", "4ED", "Fourth Edition"},
		{"IA", "ICE", "Ice Age"},
		{"CH", "CHR", "Chronicles"},
		{"HL", "HML", "Homelands"},
		{"AL", "ALL", "Alliances"},
		{"MI", "MIR", "Mirage"},
		{"VI", "VIS", "Visions"},
		{"5E", "5ED", "Fifth Edition"},
		{"PT", "POR", "Portal"},
		{"WL", "WTH", "Weatherlight"},
		{"TE", "TMP", "Tempest"},
		{"SH", "STH", "Stronghold"},
		{"EX", "EXO", "Exodus"},
		{"P2", "P02", "Portal Second Age"},
		{"US", "USG", "Urza's Saga"},
		{"UL", "ULG", "Urza's Legacy"},
		{"6E", "6ED", "Classic (Sixth Edition)"},
		{"UD", "UDS", "Urza's Destiny"},
		{"P3", "PTK", "Portal Three Kingdoms"},
		{"ST", "S99", "Starter 1999"},
		{"MM", "MMQ", "Mercadian Masques"},
		{"NE", "NMS", "Nemesis"},
		{"S2K", "S00", "Starter 2000"},
		{"PY", "PCY", "Prophecy"},
		{"IN", "INV", "Invasion"},
		{"PS", "PLS", "Planeshift"},
		{"7E", "7ED", "Seventh Edition"},
		{"AP", "APC", "Apocalypse"},
		{"OD", "ODY", "Odyssey"},
		{"TO", "TOR", "Torment"},
		{"JU", "JUD", "Judgment"},
		{"ON", "ONS", "Onslaught"},
		{"LE", "LGN", "Legions"},
		{"SC", "SCG", "Scourge"},
		{"8E", "8ED", "Core Set - Eighth Edition"},
		{"MR", "MRD", "Mirrodin"},
		{"DS", "DST", "Darksteel"},
		{"FD", "5DN", "Fifth Dawn"},
		{"CHK", "CHK", "Champions of Kamigawa"},
		{"BOK", "BOK", "Betrayers of Kamigawa"},
		{"SOK", "SOK", "Saviors of Kamigawa"},
		{"9E", "9ED", "Core Set - Ninth Edition"},
		{"RAV", "RAV", "Ravnica: City of Guilds"},
		{"GP", "GPT", "Guildpact"},
		{"DIS", "DIS", "Dissension"},
		{"CS", "CSP", "Coldsnap"},
		{"TSP", "TSP", "Time Spiral"},
		{"TSB", "TSB", "Time Spiral Timeshifted"},
		{"PLC", "PLC", "Planar Chaos"},
		{"FUT", "FUT", "Future Sight"},
		{"10E", "10E", "Core Set - Tenth Edition"},
		{"LRW", "LRW", "Lorwyn"},
		{"MOR", "MOR", "Morningtide"},
		{"SHM", "SHM", "Shadowmoor"},
		{"EVE", "EVE", "Eventide"},
		{"ALA", "ALA", "Shards of Alara"},
		{"CFX", "CFX", "Conflux"},
		{"ARB", "ARB", "Alara Reborn"},
		{"M10", "M10", "Magic The Gathering 2010"},
		{"ZEN", "ZEN", "Zendikar"},
		{"WWK", "WWK", "Worldwake"},
		{"ROE", "ROE", "Rise of the Eldrazi"},
		{"M11", "M11", "Magic The Gathering 2011"},
		{"SOM", "SOM", "Scars of Mirrodin"}
	};
	
	public static ArrayList<String> getSetCode2List()
	{
		ArrayList<String> scl = new ArrayList<String>();
		
		for (int i=0; i<sets.length; i++)
			scl.add(sets[i][0]);
		
		return scl;
	}
	
	public static ArrayList<String> getSetCode3List()
	{
		ArrayList<String> scl = new ArrayList<String>();
		
		for (int i=0; i<sets.length; i++)
			scl.add(sets[i][1]);
	
		return scl;
	}
	
	public static ArrayList<String> getSetNameList()
	{
		ArrayList<String> snl = new ArrayList<String>();
		
		for (int i=0; i<sets.length; i++)
			snl.add(sets[i][2]);
		
		return snl;
	}
	
	public static String getSetCode2_SetName(String SetName)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][2].equals(SetName))
				return sets[i][0];
		
		return "";
	}

	public static String getSetCode3_SetName(String SetName)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][2].equals(SetName))
				return sets[i][1];
		
		return "";
	}

	public static String getSetCode2_SetCode3(String SetCode3)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][1].equals(SetCode3))
				return sets[i][0];
		
		return "";
	}
	
	public static String getSetCode3_SetCode2(String SetCode2)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][0].equals(SetCode2))
				return sets[i][1];
		
		return "";
	}

	public static String getSetName_SetCode2(String SetCode2)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][0].equals(SetCode2))
				return sets[i][2];
		
		return "";
	}

	public static String getSetName_SetCode3(String SetCode3)
	{
		for (int i=0; i<sets.length; i++)
			if (sets[i][1].equals(SetCode3))
				return sets[i][2];
		
		return "";
	}
	
	public static String getMostRecentSet(ArrayList<SetInfo> alSI)
	{
		int mostRecent = -1;
		
		for (int i=0; i<alSI.size(); i++)
		{
			SetInfo s = alSI.get(i);
			
			for (int j=0; j<sets.length; j++)
			{
				if (sets[j][1].equals(s.Code))
				{
					if (j > mostRecent)
					{
						mostRecent = j;
						break;
					}
				}
			}
			
		}
		
		if (mostRecent > -1)
			return sets[mostRecent][1];
		
		return "";
	}

}
