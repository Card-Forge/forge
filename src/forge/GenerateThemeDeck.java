package forge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import forge.error.ErrorViewer;

public class GenerateThemeDeck
{
	private BufferedReader in = null;
	
	public GenerateThemeDeck()
	{
		
	}
	
	public ArrayList<String> getThemeNames()
	{
		ArrayList<String> ltNames = new ArrayList<String>();
		
		File file = new File("res/quest/themes/");
		
        if(!file.exists())
            throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- file not found -- filename is "
                    + file.getAbsolutePath());
        
        if (!file.isDirectory())
        	throw new RuntimeException("GenerateThemeDeck : getThemeNames error -- not a direcotry -- "
        			+ file.getAbsolutePath());
        
        String[] fileList = file.list();
        for (int i=0; i<fileList.length; i++)
        {
        	if (fileList[i].endsWith(".thm"))
        		ltNames.add(fileList[i].substring(0, fileList[i].indexOf(".thm")));
        }
        
		return ltNames;
	}
	
	public CardList getThemeDeck(String ThemeName, int Size)
	{
		CardList tDeck = new CardList();
				
		ArrayList<Grp> Groups = new ArrayList<Grp>();
		
		Map<String,Integer> CardCounts = new HashMap<String,Integer>();
		
		String s = "";
		int BLandPercentage = 0;
		boolean Testing = false;
		
		// read theme file
		String tFileName = "res/quest/themes/" + ThemeName + ".thm";
		File tFile = new File(tFileName);
		if(!tFile.exists())
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file not found -- filename is " + tFile.getAbsolutePath());
		
		try {
            in = new BufferedReader(new FileReader(tFile));
        } catch(Exception ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is " + tFile.getPath());
        }
        
        s = readLine();
        while (!s.equals("End"))
        {
        	if (s.startsWith("[Group"))
        	{
        		Grp G = new Grp();
        		
        		String ss[] = s.replaceAll("[\\[\\]]", "").split(" ");
        		for (int i=0; i<ss.length; i++)
        		{
        			if (ss[i].startsWith("Percentage"))
        			{
        				String p = ss[i].substring("Percentage".length() + 1);
        				G.Percentage = Integer.parseInt(p);
        			}
        			if (ss[i].startsWith("MaxCnt"))
        			{
        				String m = ss[i].substring("MaxCnt".length() + 1);
        				G.MaxCnt = Integer.parseInt(m);
        			}
        		}
        		
        		s = readLine();
        		while (!s.equals("[/Group]"))
        		{
        			G.Cardnames.add(s);
        			CardCounts.put(s, 0);
        			
        			s = readLine();
        		}
        		
        		Groups.add(G);
        	}
        	
        	if (s.startsWith("BasicLandPercentage"))
        		BLandPercentage = Integer.parseInt(s.substring("BasicLandPercentage".length() + 1));
        	
        	if (s.equals("Testing"))
        		Testing = true;
        	
        	s = readLine();
        }
        
        try {
			in.close();
		} catch (IOException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is " + tFile.getPath());
		}
		
		String tmpDeck = "";

		// begin assigning cards to the deck
		Random r = new Random();
		
		for (int i=0; i<Groups.size(); i++)
		{
			Grp G = Groups.get(i);
			float p = (float) ((float)G.Percentage * .01);
			int GrpCnt = (int)(p * (float)Size);
			int cnSize = G.Cardnames.size();
			tmpDeck += "Group" + i + ":" + GrpCnt + "\n";
			
			for (int j=0; j<GrpCnt; j++)
			{
				s = G.Cardnames.get(r.nextInt(cnSize));
				
				int lc = 0;
				while (CardCounts.get(s) >= G.MaxCnt || lc > Size) // don't keep looping forever
				{
					s = G.Cardnames.get(r.nextInt(cnSize));
					lc++;
				}
				if (lc > Size)
					throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- looped too much -- filename is " + tFile.getAbsolutePath());
				
				int n = CardCounts.get(s);
				tDeck.add(AllZone.CardFactory.getCard(s, Constant.Player.Computer));
				CardCounts.put(s, n + 1);
				tmpDeck += s + "\n";

			}
		}
		
		int numBLands = 0;
		if (BLandPercentage > 0)	// if theme explicitly defines this
		{
			float p = (float)((float)BLandPercentage * .01);
			numBLands = (int)(p * (float)Size);
		}
		else 	// otherwise, just fill in the rest of the deck with basic lands
			numBLands = Size - tDeck.size();
		
		tmpDeck += "numBLands:" + numBLands + "\n";
		
		if (numBLands > 0)	// attempt to optimize basic land counts according to color representation
		{
			CCnt ClrCnts[] = {new CCnt("Plains", 0),
							  new CCnt("Island", 0),
							  new CCnt("Swamp", 0),
							  new CCnt("Mountain", 0),
							  new CCnt("Forest", 0)};
					
			// count each instance of a color in mana costs
			// TODO: count hybrid mana differently?
			for (int i=0;i<tDeck.size(); i++)
			{
				String mc = tDeck.get(i).getManaCost();
				
				for (int j=0; j<mc.length(); j++)
				{
					char c = mc.charAt(j);
					
					if (c == 'W')
						ClrCnts[0].Count++;
					else if (c == 'U')
						ClrCnts[1].Count++;
					else if (c == 'B')
						ClrCnts[2].Count++;
					else if (c == 'R')
						ClrCnts[3].Count++;
					else if (c == 'G')
						ClrCnts[4].Count++;
				}
			}
	
			int totalColor = 0;
			for (int i=0;i<5; i++)
			{
				totalColor += ClrCnts[i].Count;
				tmpDeck += ClrCnts[i].Color + ":" + ClrCnts[i].Count + "\n";
			}
			
			tmpDeck += "totalColor:" + totalColor + "\n";
			
			for (int i=0; i<5; i++)
			{
				if (ClrCnts[i].Count > 0)
				{	// calculate number of lands for each color
					float p = (float)ClrCnts[i].Count / (float)totalColor;
					int nLand = (int)((float)numBLands * p);
					tmpDeck += "numLand-" + ClrCnts[i].Color + ":" + nLand + "\n";
				
					CardCounts.put(ClrCnts[i].Color, 2);
					for (int j=0; j<nLand; j++)
						tDeck.add(AllZone.CardFactory.getCard(ClrCnts[i].Color, Constant.Player.Computer));
				}
			}
		}
		tmpDeck += "DeckSize:" + tDeck.size() + "\n";
		
		if (tDeck.size() < Size)
		{
			int diff = Size - tDeck.size();
			
			for (int i=0; i<diff; i++)
			{
				s = tDeck.get(r.nextInt(tDeck.size())).getName();
				
				while (CardCounts.get(s) >= 4)
					s = tDeck.get(r.nextInt(tDeck.size())).getName();
				
				int n = CardCounts.get(s);
				tDeck.add(AllZone.CardFactory.getCard(s, Constant.Player.Computer));
				CardCounts.put(s, n + 1);
				tmpDeck += "Added:" + s + "\n";
			}
		}
		else if (tDeck.size() > Size)
		{
			int diff = tDeck.size() - Size;
			
			for (int i=0; i<diff; i++)
			{
				Card c = tDeck.get(r.nextInt(tDeck.size()));
				
				while (c.getType().contains("Basic"))
					c = tDeck.get(r.nextInt(tDeck.size()));
				
				tDeck.remove(c);
				tmpDeck += "Removed:" + s + "\n";
			}
		}
		
		tmpDeck += "DeckSize:" + tDeck.size() + "\n";
		if (Testing)
			ErrorViewer.showError(tmpDeck);

		return tDeck;
	}
	
    private String readLine() {
        //makes the checked exception, into an unchecked runtime exception
        try {
            String s = in.readLine();
            if(s != null) s = s.trim();
            return s;
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("GenerateThemeDeck : readLine error");
        }
    }//readLine(Card)

    class CCnt
    {
    	public String Color;
    	public int Count;
    	
    	public CCnt(String clr, int cnt)
    	{
    		Color = clr;
    		Count = cnt;
    	}
    }

    class Grp
    {
    	public ArrayList<String> Cardnames = new ArrayList<String>();
    	public int MaxCnt;
    	public int Percentage;
    }
}

