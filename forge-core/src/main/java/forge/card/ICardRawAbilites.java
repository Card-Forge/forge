package forge.card;

import java.util.Map.Entry;

public interface ICardRawAbilites
{
    Iterable<String> getKeywords();
    Iterable<String> getReplacements();
    Iterable<String> getTriggers();
    Iterable<String> getStaticAbilities();
    Iterable<String> getAbilities();
    
    String getNonAbilityText();
    
    Iterable<Entry<String, String>> getVariables();
}
