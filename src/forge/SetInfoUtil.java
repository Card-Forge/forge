package forge;

import java.util.ArrayList;

public class SetInfoUtil {
	
	private static String[][] sets = {
		{"MBP", "MBP", "Media Insert Promo"},
		{"A", "LEA", "Limited Edition Alpha"},
		{"B", "LEB", "Limited Edition Beta"},
		{"U", "2ED", "Unlimited"},
		{"AN", "ARN", "Arabian Nights"},
		{"AQ", "ATQ", "Antiquities"},
		{"R", "3ED", "Revised Edition"},
		{"LG", "LEG", "Legends"},
		{"DK", "DRK", "The Dark"},
		{"FE", "FEM", "Fallen Empires"},
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
		{"P2", "PO2", "Portal Second Age"},
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
		{"SOM", "SOM", "Scars of Mirrodin"},
		{"MBS", "MBS", "Mirrodin Besieged"},
        {"NPH", "NPH", "New Phyrexia"}
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
	
	public static SetInfo getSetInfo_Code(ArrayList<SetInfo> SetList, String SetCode)
	{
		SetInfo si;
		
		for (int i=0; i<SetList.size(); i++)
		{
			si = SetList.get(i);
			if (si.Code.equals(SetCode))
				return si;
		}
		
		return null;
	}
	
	public static int getSetIndex(String SetCode)
	{
		for (int i=0; i<sets.length; i++)
		{
			if (sets[i][1].equals(SetCode))
				return i;
		}
		
		return 0;
	}

	private static String[][] blocks = {
		{"A", "", "", "Alpha", "3", "A"},
		{"B", "", "", "Beta", "3", "B"},
		{"2ED", "", "", "Unlimited", "3", "2ED"},
		{"ARN", "", "", "Arabian Nights", "5", "2ED"},
		{"ATQ", "", "", "Antiquities", "5", "ATQ"},
		{"3ED", "", "", "Revised", "3", "3ED"},
		{"LEG", "", "", "Legends", "3", "3ED"},
		{"DRK", "", "", "The Dark", "5", "3ED"},
		{"FEM", "", "", "Fallen Empires", "5", "3ED"},
		{"4ED", "", "", "Fourth Edition", "3", "4ED"},
		{"ICE", "ALL", "CSP", "Ice Age", "3", "ICE"},
		//{"CHR", "", "", "Chronicles", "4", "4ED"},
		{"HML", "", "", "Homelands", "5", "4ED"},
		{"MIR", "VIS", "WTH", "Mirage", "3", "MIR"},
		{"5ED", "", "", "Fifth Edition", "3", "5ED"},
		{"POR", "", "", "Portal", "3", "POR"},
		{"TMP", "STH", "EXO", "Tempest", "3", "TMP"},
		{"PO2", "", "", "Portal Second Age", "3", "PO2"},
		{"USG", "ULG", "UDS", "Urza", "3", "USG"},
		{"6ED", "", "", "Sixth Edition", "3", "6ED"},
		{"PTK", "", "", "Portal Three Kingdoms", "5", "PTK"},
		{"MMQ", "NMS", "PCY", "Masques", "3", "MMQ"},
		{"INV", "PLS", "APC", "Invasion", "3", "INV"},
		{"7ED", "", "", "Seventh Edition", "3", "7ED"},
		{"ODY", "TOR", "JUD", "Odyssey", "3", "ODY"},
		{"ONS", "LGN", "SCG", "Onslaught", "3", "ONS"},
		{"8ED", "", "", "Eighth Edition", "3", "8ED"},
		{"MRD", "DST", "5DN", "Mirrodin", "3", "MRD"},
		{"CHK", "BOK", "SOK", "Kamigawa", "3", "CHK"},
		{"9ED", "", "", "Ninth Edition", "3", "9ED"},
		{"RAV", "GPT", "DIS", "Ravnica", "3", "RAV"},
		{"CSP", "", "", "Coldsnap", "3", "9ED"},
		{"TSP", "PLC", "FUT", "Time Spiral", "3", "TSP"},
		{"10E", "", "", "Tenth Edition", "3", "10E"},
		{"LRW", "MOR", "", "Lorwyn", "3", "LRW"},
		{"SHM", "EVE", "", "Shadowmoor", "3", "SHM"},
		{"ALA", "CFX", "ARB", "Shards of Alara", "3", "ALA"},
		{"M10", "", "", "Magic 2010", "3", "M10"},
		{"ZEN", "WWK", "", "Zendikar", "3", "ZEN"},
		{"ROE", "", "", "Rise of the Eldrazi", "3", "ROE"},
		{"M11", "", "", "Magic 2011", "3", "M11"},
		{"SOM", "MBS", "NPH", "Scars of Mirrodin", "3", "SOM"}
	};
	
	public static ArrayList<String> getBlockNameList() {
		ArrayList<String> bnl = new ArrayList<String>();
		
		for (int i=0; i<blocks.length; i++)
			bnl.add(blocks[i][3]);
		
		return bnl;
	}
	
	public static ArrayList<String> getSets_BlockName(String blockName) {
		ArrayList<String> sets = new ArrayList<String>();
		
		for (int i=0; i<blocks.length; i++) {
			if (blocks[i][3].equals(blockName)) {
				for (int j=0; j<3; j++)
					sets.add(blocks[i][j]);
			}
		}
		
		return sets;
	}
	
	public static int getPackCount(String blockName) {
		for (int i=0; i<blocks.length; i++) {
			if (blocks[i][3].equals(blockName))
				return Integer.parseInt(blocks[i][4]);
		}
		
		return 0;
	}
	
	public static String getLandCode(String blockName) {
		for (int i=0; i<blocks.length; i++) {
			if (blocks[i][3].equals(blockName))
				return blocks[i][5];
		}
		
		return "M11"; // default, should never happen IRL
	}
}
