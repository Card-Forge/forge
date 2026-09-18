package forge.card;

import java.util.Map;

/**
 * One tokenized {@code DeckRule:<ruleClass>:<params>} line: the rule class name and its
 * {@code Key$ Value} params, split but not yet built into a typed rule object. See
 * {@code forge.deck.DeckRule}, which does that last step (it needs per-printing/per-instance
 * data - like a commander's marked colors - that a card face alone doesn't have).
 */
public final class DeckRuleLine {
    private final String ruleClass;
    private final Map<String, String> params;

    DeckRuleLine(final String ruleClass, final Map<String, String> params) {
        this.ruleClass = ruleClass;
        this.params = params;
    }

    public String getRuleClass() {
        return ruleClass;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
