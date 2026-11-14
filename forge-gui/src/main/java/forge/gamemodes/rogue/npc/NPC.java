package forge.gamemodes.rogue.npc;

/**
 * Registry of all NPCs in Rogue Commander mode.
 * Holds identity data shared across encounters, boons, and events.
 */
public enum NPC {

    GONTI("gonti", "Gonti, Lord of Luxury", 121),
    HENZIE("henzie", "Henzie \"Toolbox\" Torre", 59),
    NARSET("narset", "Narset, Planeshard Collector", 11),
    TEFERI("teferi", "Teferi, Master of Time", 65),
    TYVAR("tyvar", "Tyvar Kell", 14);

    public final String id;
    public final String name;
    public final int avatarIndex;



    NPC(String id, String name, int avatarIndex) {
        this.id = id;
        this.name = name;
        this.avatarIndex = avatarIndex;
    }
}
