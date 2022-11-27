package forge.adventure.util;

import com.badlogic.gdx.utils.Array;

public enum AdventureModes {
    Standard("Standard"),
    Constructed("Constructed"),
    Chaos("[RED]Chaos"),
    Pile("Pile"),
    Custom("Custom");

    private final String name;
    private  String selectionName;
    private Array<String> modes;
    AdventureModes(String name)
    {
        this.name=name;
    }
    public String getName()
    {
        return name.isEmpty()?toString():name;
    }
    public void setSelectionName(String selectionName)
    {
        this.selectionName=selectionName;
    }
    public void setModes( Array<String> modes)
    {
        this.modes=modes;
    }

    public String getSelectionName() {
        return selectionName;
    }

    public  Array<String> getModes() {
        return modes;
    }
}
