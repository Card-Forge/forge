package forge.quest.data.bazaar;

public class QuestStallDefinition {
    public String name;
    public String displayName;
    public String iconName;
    public String fluff;

    public QuestStallDefinition(String name, String displayName, String fluff, String iconName) {
        this.name = name;
        this.displayName = displayName;
        this.fluff = fluff;
        this.iconName = iconName;
    }
}
