package forge.adventure.util;

import com.badlogic.gdx.utils.Array;
import forge.Forge;

public enum AdventureModes {
    Standard(Forge.getLocalizer().getMessage("lblStandard")),
    Constructed(Forge.getLocalizer().getMessage("lblConstructed")),
    Chaos("[GOLD]"+Forge.getLocalizer().getMessage("lblChaos")),
    Pile(Forge.getLocalizer().getMessage("lblPile")),
    Custom(Forge.getLocalizer().getMessage("lblCustom")),
    Commander(Forge.getLocalizer().getMessage("lblCommander")),
    Precon(Forge.getLocalizer().getMessageorUseDefault("lblPrecon", "Precon")),
    CommanderPrecon(Forge.getLocalizer().getMessageorUseDefault("lblCommanderPrecon", "Commander Precon"));

    private final String name;
    private String selectionName;
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

    public Array<String> getModes() {
        return modes;
    }

    public boolean isCommanderLike() {
        return this == Commander || this == CommanderPrecon;
    }

    public boolean usesFolderDeckPicker() {
        return this == Precon || this == CommanderPrecon;
    }

    public boolean usesStarterEditionSelector() {
        return this == Standard || usesFolderDeckPicker();
    }
}
