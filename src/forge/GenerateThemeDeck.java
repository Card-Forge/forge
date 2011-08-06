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
		
		ArrayList<String> BLands = new ArrayList<String>();
		ArrayList<String> SLands = new ArrayList<String>();
		ArrayList<String> Core = new ArrayList<String>();
		ArrayList<String> Suprt = new ArrayList<String>();
		
		Map<String,Integer> CardCounts = new HashMap<String,Integer>();
		
		// calculate percentages of deck for each type of card
		int numLands = (int)((float) Size * 0.4);
		int numBLands = (int)((float) numLands * 0.67);
		int numSLands = numLands - numBLands;
		
		int numCore = (int)((float)Size * 0.35);
		int numSuprt = Size - numLands - numCore;
		
		String s = "";
		
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
        	if (s.equals("[BasicLand]"))
        	{
        		s = readLine();
        		while (!s.equals("EndBasicLand"))
        		{
        			BLands.add(s);
        			
        			s = readLine();
        		}
        	}
        	
        	if (s.equals("[SpecialLand]"))
        	{
        		s = readLine();
        		while (!s.equals("EndSpecialLand"))
        		{
        			SLands.add(s);
        			CardCounts.put(s, 0);
        			
        			s = readLine();
        		}        		
        	}
        	
        	if (s.equals("[Core]"))
        	{
        		s = readLine();
        		while (!s.equals("EndCore"))
        		{
        			Core.add(s);
        			CardCounts.put(s, 0);
        			
        			s = readLine();
        		}        		
        	}

        	if (s.equals("[Support]"))
        	{
        		s = readLine();
        		while (!s.equals("EndSupport"))
        		{
        			Suprt.add(s);
        			CardCounts.put(s, 0);
        			
        			s = readLine();
        		}        		
        	}

        	s = readLine();
        }
        
        try {
			in.close();
		} catch (IOException ex) {
            ErrorViewer.showError(ex, "File \"%s\" exception", tFile.getAbsolutePath());
            throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- file exception -- filename is " + tFile.getPath());
		}

		// begin assigning cards to the deck
		Random r = new Random();
		
		int n = 0;
		while (n < numBLands)
		{
			for (int i=0; i<BLands.size(); i++) // assures even distribution of basic lands
			{
				if (n < numBLands)
					tDeck.add(AllZone.CardFactory.getCard(BLands.get(i), Constant.Player.Computer));
				n++;
			}
		}
		
		for (int i=0; i<numSLands; i++)
		{
			s = SLands.get(r.nextInt(SLands.size()));
			
			int lc = 0;
			while (CardCounts.get(s) > 3 || lc > Size) // don't keep looping forever
			{
				s = SLands.get(r.nextInt(SLands.size()));
				lc++;
			}
			if (lc > Size)
				throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- SLands looped too much -- filename is " + tFile.getAbsolutePath());
			
			n = CardCounts.get(s);
			tDeck.add(AllZone.CardFactory.getCard(s, Constant.Player.Computer));
			CardCounts.put(s, n + 1);
		}
		
		for (int i=0; i<numCore; i++)
		{
			s = Core.get(r.nextInt(Core.size()));
			
			int lc = 0;
			while (CardCounts.get(s) > 3 || lc > Size)
			{
				s = Core.get(r.nextInt(Core.size()));
				lc++;
			}
			if (lc > Size)
				throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- Core looped too much -- filename is " + tFile.getAbsolutePath());
			
			n = CardCounts.get(s);
			tDeck.add(AllZone.CardFactory.getCard(s, Constant.Player.Computer));
			CardCounts.put(s, n + 1);
		}

		for (int i=0; i<numSuprt; i++)
		{
			s = Suprt.get(r.nextInt(Suprt.size()));
			
			int lc = 0;
			while (CardCounts.get(s) > 3 || lc > Size)
			{
				s = Suprt.get(r.nextInt(Suprt.size()));
				lc++;
			}
			if (lc > Size)
				throw new RuntimeException("GenerateThemeDeck : getThemeDeck -- Suprt looped too much -- filename is " + tFile.getAbsolutePath());
			
			n = CardCounts.get(s);
			tDeck.add(AllZone.CardFactory.getCard(s, Constant.Player.Computer));
			CardCounts.put(s, n + 1);
		}		
        
		String tmpDeck = "";
		tmpDeck += "numLands: " + numLands + "\n";
		tmpDeck += "numBLands: " + numBLands + "\n";
		tmpDeck += "numSLands: " + numSLands + "\n";
		tmpDeck += "numCore: " + numCore + "\n";
		tmpDeck += "numSuprt: " + numSuprt + "\n";
		tmpDeck += "tDeck.size: " + tDeck.size() + "\n";
		for (int i=0; i<tDeck.size(); i++)
			tmpDeck += tDeck.get(i).getName() + "\n";
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

}
