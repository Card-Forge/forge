package forge.adventure.data;

/**
 * Dialog Data JSON loader class.
 * Carries all text, branches and effects of dialogs.
 */
public class DialogData {
    public ActionData[] effect;       //List of effects to cause when the dialog shows.
    public ConditionData[] condition; //List of conditions for the action to show.
    public String name;               //Text to display when action is listed as a button.
    public String locname;            //References a localized string for the button labels.
    public String text;               //The text body.
    public String loctext;            //References a localized string for the text body.
    public DialogData[] options;      //



    static public class ActionData {
        static public class QuestFlag{
            public String key;
            public int val;
        }
        public String removeItem;         //Remove item name from inventory.
        public String addItem;            //Add item name to inventory.
        public int addLife = 0;           //Gives the player X health. Negative to take.
        public int addGold = 0;           //Gives the player X gold. Negative to take.
        public int deleteMapObject = 0;   //Remove ID from the map. -1 for self.
        public int battleWithActorID = 0; //Start a battle with enemy ID. -1 for self if possible.
        public EffectData giveBlessing;   //Give a blessing to the player.
        public String setColorIdentity;   //Change player's color identity.
        public String advanceQuestFlag;//Increase given quest flag by 1.
        public QuestFlag setQuestFlag;    //Set quest flag {flag ID, value}
    }

    static public class ConditionData {
        static public class QueryQuestFlag{
            public String key;
            public String op;
            public int val;
        }
        public String item;
        public int flag = 0;                 //Check for a local dungeon flag.
        public int actorID = 0;              //Check for an actor ID.
        public String hasBlessing = null;    //Check for specific blessing, if named.
        public int hasGold = 0;              //Check for player gold. True if gold is equal or higher than X.
        public int hasLife = 0;              //Check for player life. True if life is equal or higher than X.
        public String colorIdentity = null;  //Check for player's current color identity.
        public String checkQuestFlag = null; //Check if a quest flag is not 0. False if equals 0 (not started, not set).
        public QueryQuestFlag getQuestFlag = null; //Check for value of a flag { <flagID>, <comparison>, <value> }
        public boolean not = false;          //Reverse the result of a condition ("actorID":"XX" + "not":true => true if XX is not in the map.)
    }
}
