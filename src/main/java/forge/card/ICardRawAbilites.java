package forge.card;

import java.util.Map.Entry;

public interface ICardRawAbilites
{
    public abstract Iterable<String> getKeywords();
    public abstract Iterable<String> getReplacements();
    public abstract Iterable<String> getTriggers();
    public abstract Iterable<String> getStaticAbilities();
    public abstract Iterable<String> getAbilities();
    public abstract String getNonAbilityText();
    public abstract Iterable<Entry<String, String>> getVariables();

}