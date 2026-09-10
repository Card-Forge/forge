package forge.card;

import java.util.List;
import java.util.Map.Entry;

public interface ICardRawAbilites
{
    Iterable<String> getKeywords();
    Iterable<String> getDeckRules();
    List<DeckRuleLine> getTokenizedDeckRules();
    Iterable<String> getReplacements();
    Iterable<String> getTriggers();
    Iterable<String> getDraftActions();
    Iterable<String> getStaticAbilities();
    Iterable<String> getAbilities();
    
    String getNonAbilityText();
    
    Iterable<Entry<String, String>> getVariables();
}
