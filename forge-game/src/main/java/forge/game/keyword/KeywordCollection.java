package forge.game.keyword;

import java.util.EnumMap;
import java.util.List;

public class KeywordCollection {
    private final EnumMap<Keyword, List<KeywordInstance<?>>> keywords = new EnumMap<Keyword, List<KeywordInstance<?>>>(Keyword.class);

    public boolean contains(Keyword keyword) {
        return keywords.containsKey(keyword);
    }

    public int getAmount(Keyword keyword) {
        int amount = 0;
        List<KeywordInstance<?>> instances = keywords.get(keyword);
        if (instances != null) {
            for (KeywordInstance<?> inst : instances) {
                amount += inst.getAmount();
            }
        }
        return amount;
    }
}
