package forge.deck.io;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.List;

public class Archetype implements Serializable {

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
        return name;
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
}
