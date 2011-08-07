package forge;

import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoosterGenerator {
	private CardList commons = new CardList();
	private CardList uncommons = new CardList();
	private CardList rares = new CardList();
	private CardList mythics = new CardList();
	private CardList specials = new CardList();

	private int iCommons = 0;
	private int iUncommons = 0;
	private int iRares = 0;
	private int iMythics = 0;
	private int iSpecials = 0;

	private int numCommons = 0;
	private int numUncommons = 0;
	private int numRares = 0;
	private int numMythics = 0;
	private int numSpecials = 0;

	private Random r  = new Random();

	public BoosterGenerator() {
		numCommons = 11;
		numUncommons = 3;
		numRares = 1;
		numMythics = 0;
		numSpecials = 0;

		CardList tList = AllZone.CardFactory.getAllCards();
		for (int i=0; i<tList.size(); i++) {
			Card c = tList.get(i);
			SetInfo si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetInfoUtil.getMostRecentSet(c.getSets()));

			addToRarity(c, si);
		}

		shuffleAll();

	}

	public BoosterGenerator(String DeckFile, int nCommons, int nUncommons, int nRares, int nMythics, int nSpecials, boolean ignoreRarity) {
		numCommons = nCommons;
		numUncommons = nUncommons;
		numRares = nRares;
		numMythics = nMythics;
		numSpecials = nSpecials;
		
		DeckManager dio = new DeckManager(ForgeProps.getFile(NewConstants.NEW_DECKS));
		Deck dPool= dio.getDeck(DeckFile);
		if (dPool == null)
			throw new RuntimeException("BoosterGenerator : deck not found - " + DeckFile);
		
		CardList cList = new CardList();
		List<String> tList = dPool.getMain();

		for (int i=0; i<tList.size(); i++) {
			String cardName = tList.get(i);
        	String setCode = "";
            if (cardName.contains("|"))
            {
            	String s[] = cardName.split("\\|",2);
            	cardName = s[0];
            	setCode = s[1];
            }

            Card c = AllZone.CardFactory.getCard(cardName, AllZone.HumanPlayer);

            if (!setCode.equals(""))
            	c.setCurSetCode(setCode);
            else if ((c.getSets().size() > 0)) // && card.getCurSetCode().equals(""))
            	c.setRandomSetCode();

            cList.add(c);
		}


		for (int i=0; i<cList.size();i++) {
			Card c = cList.get(i);
			SetInfo si = null;
			if (c.getCurSetCode().equals(""))
				si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetInfoUtil.getMostRecentSet(c.getSets()));
			else
				si = SetInfoUtil.getSetInfo_Code(c.getSets(), c.getCurSetCode());

			if (ignoreRarity)
				commons.add(c);
			else
				addToRarity(c, si);
		}

		shuffleAll();
	}

	public BoosterGenerator(final String SetCode) {
		numCommons = 0;
		numUncommons = 0;
		numRares = 0;
		numMythics = 0;
		numSpecials = 0;
		
		CardList tList = AllZone.CardFactory.getAllCards();
		for (int i=0; i<tList.size(); i++) {
			Card c = tList.get(i);
			SetInfo si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetCode);

			if (si != null) {
				c.setCurSetCode(SetCode);

		    	Random r = new Random();
		    	int n = si.PicCount;
		    	if (n > 1)
		    		c.setRandomPicture(r.nextInt(n-1) + 1);

		    	addToRarity(c, si);
			}
		}

		shuffleAll();

		ArrayList<String> bpData = FileUtil.readFile("res/boosterdata/" + SetCode + ".pack");

        for (String line : bpData) {
            if (line.startsWith("Commons:")) {
                numCommons = Integer.parseInt(line.substring(8));
            }
            else if (line.startsWith("Uncommons:")) {
                numUncommons = Integer.parseInt(line.substring(10));
            }
            else if (line.startsWith("Rares:")) {
                numRares = Integer.parseInt(line.substring(6));
            }
            else if (line.startsWith("Mythics:")) {
                numMythics = Integer.parseInt(line.substring(8));
            }
            else if (line.startsWith("Specials:")) {
                numSpecials = Integer.parseInt(line.substring(9));
            }

        }

        if (Constant.Runtime.DevMode[0]) {
			System.out.println("numCommons: " + numCommons);
			System.out.println("numUncommons: " + numUncommons);
			System.out.println("numRares: " + numRares);
			System.out.println("numMythics: " + numMythics);
			System.out.println("numSpecials: " + numSpecials);
        }

	}

	private void addToRarity(Card c, SetInfo si) {
		if (si != null) {
			if (si.Rarity.equals("Common"))
				commons.add(c);
			else if (si.Rarity.equals("Uncommon"))
				uncommons.add(c);
			else if (si.Rarity.equals("Rare"))
				rares.add(c);
			else if (si.Rarity.equals("Mythic"))
				mythics.add(c);
			else if (si.Rarity.equals("Special"))
				specials.add(c);
		}
	}

	private void shuffleAll() {

		if (commons.size() > 0)
			commons.shuffle();

		if (uncommons.size() > 0)
			uncommons.shuffle();

		if (rares.size() > 0)
			rares.shuffle();

		if (mythics.size() > 0)
			mythics.shuffle();

		if (specials.size() > 0)
			specials.shuffle();

		if (Constant.Runtime.DevMode[0]) {
			System.out.println("commons.size: " + commons.size());
			System.out.println("uncommons.size: " + uncommons.size());
			System.out.println("rares.size: " + rares.size());
			System.out.println("mythics.size: " + mythics.size());
			System.out.println("specials.size: " + specials.size());
		}
	}

	public CardList getBoosterPack() {
		CardList temp = new CardList();

		int i = 0;

		if (commons.size() > numCommons) {
			for (i=0; i<numCommons; i++) {
				if (iCommons >= commons.size())
					iCommons = 0;

				temp.add(commons.get(iCommons++));
			}
		}

		if (uncommons.size() > numUncommons) {
			for (i=0; i<numUncommons; i++) {
				if (iUncommons >= uncommons.size())
					iUncommons = 0;

				temp.add(uncommons.get(iUncommons++));
			}
		}

		for (i=0; i<numRares; i++) {
			if (numMythics > 0) {
				if (mythics.size() > numMythics) {
					if (r.nextInt(8) <= 1) {
						if (iMythics >= mythics.size())
							iMythics = 0;

						temp.add(mythics.get(iMythics++));
					}
					else {
						if (iRares >= rares.size())
							iRares = 0;

						temp.add(rares.get(iRares++));
					}
				}
			}
			else {
				if (rares.size() > numRares) {
					if (iRares >= rares.size())
						iRares = 0;

					temp.add(rares.get(iRares++));
				}
			}
		}

		if (specials.size() > numSpecials) {
			for (i=0; i<numSpecials; i ++) {
				if (iSpecials >= specials.size())
					iSpecials = 0;

				temp.add(specials.get(iSpecials++));
			}
		}

		return temp;
	}

	public int getBoosterPackSize() {
		return numCommons + numUncommons + numRares + numSpecials;
	}
}
