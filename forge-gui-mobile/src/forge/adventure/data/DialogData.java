package forge.adventure.data;

/**
 * Dialog Data JSON loader class.
 * Carries all text, branches and effects of dialogs.
 */
public class DialogData {
    public EffectData[] effect;       //List of effects to cause when the dialog shows.
    public ConditionData[] condition; //List of conditions for the action to show.
    public String name;               //Text to display when action is listed as a button.
    public String text;               //The text body.
    public String loctext;            //References a localized string.
    public DialogData[] options;      //

    static public class EffectData {
        public String removeItem;         //Remove item name from inventory.
        public String addItem;            //Add item name to inventory.
        public int deleteMapObject = 0;   //Remove ID from the map. -1 for self.
        public int battleWithActorID = 0; //Start a battle with enemy ID. -1 for self if possible.
    }

    static public class ConditionData {
        public String item;
        public int flag = 0;           //Check for a local dungeon flag.
        public int actorID = 0;        //Check for an actor ID.
        public boolean not = false;    //Reverse the result of a condition ("actorID":"XX" + "not":true => true if XX is not in the map.)
    }
}
