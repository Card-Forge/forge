package forge.adventure.data;

public class DialogData {
    public EffectData[] effect;
    public ConditionData[] condition;
    public String name;
    public String text;
    public String loctext; //References a localized string.
    public DialogData[] options;

    static public class EffectData {
        public String removeItem;
        public int deleteMapObject;
    }

    static public class ConditionData {
        public String item;
    }
}
