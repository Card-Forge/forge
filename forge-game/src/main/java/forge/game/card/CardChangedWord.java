package forge.game.card;

public class CardChangedWord {

    private final String originalWord,
        newWord;

    public CardChangedWord(final String originalWord, final String newWord) {
        this.originalWord = originalWord;
        this.newWord = newWord;
    }

    public String getOriginalWord() {
        return originalWord;
    }

    public String getNewWord() {
        return newWord;
    }

}
