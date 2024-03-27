package forge.game.keyword;

import java.util.Collection;

public class Landwalk extends KeywordWithType {

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInstance#redundant(java.util.Collection)
     */
    @Override
    public boolean redundant(Collection<KeywordInterface> list) {
        for (KeywordInterface i : list) {
            if (i.getOriginal().equals(getOriginal())) {
                return true;
            }
        }
        return false;
    }
}
