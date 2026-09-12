package forge.game.keyword;

public class PoisonTolerance extends KeywordWithAmount {

    @Override
    public String getTitle() {
        return getKeyword() + " +" + getAmountString();
    }
}
