package forge.adventure.data;

public class DialogData {
    public EffectData[] effect;
    public ConditionData[] condition;
    public String name;
    public String text;
    public String loctext; //References a localized string.
    public DialogData[] options;

    static public class EffectData {
        public String removeItem;         //Remove item name from inventory.
        public String addItem;            //Add item name to inventory.
        public int deleteMapObject = 0;   //Remove ID from the map. -1 for self.
        public int battleWithActorID = 0; //Start a battle with enemy ID. -1 for self if possible.
    }

    static public class ConditionData {
        public String item;
        public int flag = 0;
        public int actorID = 0;
        public boolean not = false;
    }
}
