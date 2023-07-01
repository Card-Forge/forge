package forge.game;

import java.util.Map;

public interface IHasSVars {

    public String getSVar(final String name);

    public boolean hasSVar(final String name);
    //public Integer getSVarInt(final String name);

    public void setSVar(final String name, final String value);
    public void setSVars(final Map<String, String> newSVars);

    //public Set<String> getSVars();

    public Map<String, String> getSVars();
    public Map<String, String> getDirectSVars();

    public void removeSVar(final String var);
}
