package forge.quest.data;

import forge.CardList;
import forge.Constant;
import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// The BoosterPack generates cards for the Card Pool in Quest Mode
public class QuestBoosterPack implements NewConstants {
     ArrayList<String> choices;

    public QuestBoosterPack() {
    	choices = new ArrayList<String>();
    	choices.add("Multicolor");
    	
    	for(String s : Constant.Color.Colors){
    		choices.add(s);
	    	choices.add(s);
	    }
    }
    
    public ArrayList<String> getQuestStarterDeck(CardList cards, int numCommon, int numUncommon, int numRare, boolean standardPool){
    	ArrayList<String> names = new ArrayList<String>();
    	
    	// Each color should have around the same amount of monocolored cards
    	// There should be 3 Colorless cards for every 4 cards in a single color
    	// There should be 1 Multicolor card for every 4 cards in a single color
    	
    	ArrayList<String> started = new ArrayList<String>();
    	started.add("Multicolor");
    	for(int i = 0; i < 4; i++){
    		if (i != 2)
    			started.add(Constant.Color.Colorless);

            started.addAll(Arrays.asList(Constant.Color.onlyColors));
    	}
    		
    	if (standardPool){
    		// filter Cards for cards appearing in Standard Sets
	    	ArrayList<String> sets = new ArrayList<String>();
	    	//TODO: It would be handy if the list of any sets can be chosen
	    	sets.add("NPH");
	    	sets.add("MBS");
	    	sets.add("SOM");
	    	sets.add("M11");
	    	sets.add("ROE");
	    	sets.add("WWK");
	    	sets.add("ZEN");
	
	    	cards = cards.getSets(sets);
    	}
    	
    	names.addAll(generateCards(cards, numCommon, Constant.Rarity.Common, null, started));
    	names.addAll(generateCards(cards, numUncommon, Constant.Rarity.Uncommon, null, started));
    	names.addAll(generateCards(cards, numRare, Constant.Rarity.Rare, null, started));

    	return names;
    }
    
    public ArrayList<String> generateCards(CardList cards, int num, String rarity, String color, ArrayList<String> colorOrder){
    	// If color is null, use colorOrder progression to grab cards
    	ArrayList<String> names = new ArrayList<String>();

    	int size = colorOrder.size();
    	Collections.shuffle(colorOrder);

    	cards = cards.getRarity(rarity);
    	int count = 0, i = 0;
    	while(count < num){
    		String name;

    		if (color == null)
    			name = getCardName(cards, colorOrder.get(i % size));
    		else
    			name = getCardName(cards);

    		if (name != null && !names.contains(name)){
    			names.add(name);
    			count++;	
    		}
    		i++;
    	}

    	return names;	
    }

    public ArrayList<String> generateCards(CardList cards, int num, String rarity, String color){
    	return generateCards(cards, num, rarity, color, choices);
    }

    public String getCardName(CardList cards, String color){
        return getCardName(cards.getColor(color));
    }

    public String getCardName(CardList cards){
    	if (cards.isEmpty())	// Only should happen if something is programmed wrong
    		return null;
        cards.shuffle();

        return cards.get(0).getName();
    }
}