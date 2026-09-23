package forge.game;

import java.util.Map;

public interface IHasSVars {

    String getSVar(final String name);

    boolean hasSVar(final String name);
    //public Integer getSVarInt(final String name);

    void setSVar(final String name, final String value);
    void setSVars(final Map<String, String> newSVars);

    //public Set<String> getSVars();

    Map<String, String> getSVars();

    void removeSVar(final String var);
}
