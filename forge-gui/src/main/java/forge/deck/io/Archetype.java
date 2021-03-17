package forge.deck.io;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class Archetype implements Serializable {

    static final long serialVersionUID = 1733769383530140352L;

    private List<Pair<String, Double>> cardProbabilities;
    private String name;
    private Integer deckCount;

    public Archetype(List<Pair<String, Double>> cardProbabilities, String name, Integer deckCount){
        this.cardProbabilities = cardProbabilities;
        this.name = name;
        this.deckCount = deckCount;
    }

    public List<Pair<String, Double>> getCardProbabilities() {
        return cardProbabilities;
    }

    public void setCardProbabilities(List<Pair<String, Double>> cardProbabilities) {
        this.cardProbabilities = cardProbabilities;
    }

    public String getName() {
        return titleize(name);
        /*//Debug:
        return getDeckCount() + "-" + getCardProbabilities().get(0).getRight().toString().substring(0,4)
                + "-" + titleize(name) + "-" + getCardProbabilities().get(0).getLeft();*/
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDeckCount() {
        return deckCount;
    }

    public void setDeckCount(Integer deckCount) {
        this.deckCount = deckCount;
    }

    public static String titleize(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        boolean lastCharacterWasWhitespace = true;

        for (final char currentCharacter : input.toCharArray()) {
            if (lastCharacterWasWhitespace) {
                output.append(Character.toTitleCase(currentCharacter));
            } else {
                output.append(currentCharacter);
            }
            lastCharacterWasWhitespace = Character.isWhitespace(currentCharacter);
        }
        return output.toString();
    }
}
