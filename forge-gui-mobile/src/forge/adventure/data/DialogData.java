package forge.adventure.data;

import forge.util.Callback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog Data JSON loader class.
 * Carries all text, branches and effects of dialogs.
 */
public class DialogData implements Serializable {
    //private static final long SerialVersionUID = 1; // TODO: set to current value

    public ActionData[] action = new ActionData[0];       //List of effects to cause when the dialog shows.
    public ConditionData[] condition = new ConditionData[0]; //List of conditions for the action to show.
    public String name = "";               //Text to display when action is listed as a button.
    public String locname = "";            //References a localized string for the button labels.
    public String text = "";               //The text body.
    public String loctext= "";            //References a localized string for the text body.
    public DialogData[] options = new DialogData[0];      //List of sub-dialogs. Show up as options in the current one.

    public transient Callback callback;

    public DialogData(){}
    public DialogData(DialogData other){
        if (other == null)
            return;

        this.action = other.action.clone();
        this.condition = other.condition.clone();
        this.name = other.name;
        this.locname = other.locname.isEmpty()?"":("Copy of " + other.locname);
        this.text = other.text;
        this.loctext = other.loctext;
        List<DialogData> clonedOptions = new ArrayList<>();
        for (DialogData option: other.options){
            clonedOptions.add(new DialogData(option));
        }
        this.options = clonedOptions.toArray(new DialogData[0]);
        this.voiceFile = other.voiceFile;
    }

    @Override
    public String toString(){
        return this.name;
    }

    public String voiceFile;

    static public class ActionData implements Serializable {
        public static final long serialVersionUID = 2848523275822677205L;
        static public class QuestFlag implements Serializable{
            public String key;
            public int val;
        }
        public String removeItem;         //Remove item name from inventory.
        public String addItem;            //Add item name to inventory.
        public int addLife = 0;           //Gives the player X health. Negative to take.
        public int addGold = 0;           //Gives the player X gold. Negative to take.
        public int addShards = 0;           //Gives the player X shards. Negative to take.

        public int deleteMapObject = 0;   //Remove ID from the map. -1 for self.
        public int activateMapObject = 0; //Remove inactive state from ID.
        public int battleWithActorID = 0; //Start a battle with enemy ID. -1 for self if possible.
        public EffectData giveBlessing;   //Give a blessing to the player.
        public String setColorIdentity;   //Change player's color identity.
        public String advanceCharacterFlag;   //Increase given quest flag by 1.
        public String advanceQuestFlag;   //Increase given quest flag by 1.
        public String advanceMapFlag;     //Increase given map flag by 1.
        public EffectData setEffect;      //Set or replace current effects on current actor.
        public QuestFlag setCharacterFlag;    //Set quest flag.
        public QuestFlag setQuestFlag;    //Set quest flag.
        public QuestFlag setMapFlag;      //Set map flag.

        public RewardData[] grantRewards = new RewardData[0];   //launch a RewardScene with the provided data.
        public String issueQuest; //Add quest with this ID to the player's questlog.

        public int addMapReputation = 0;  //Gives the player X reputation points in this POI. Negative to take.
        public String POIReference; //used with addMapReputation when a quest step affects reputation in another location

        public ActionData(){}

        public ActionData(ActionData other){
            removeItem = other.removeItem;
            addItem = other.removeItem;
            addLife = other.addLife;
            addGold = other.addGold;
            addShards = other.addShards;
            deleteMapObject = other.deleteMapObject;
            activateMapObject = other.activateMapObject;
            battleWithActorID = other.battleWithActorID;
            giveBlessing = other.giveBlessing;
            setColorIdentity = other.setColorIdentity;
            advanceQuestFlag = other.advanceQuestFlag;
            advanceMapFlag = other.advanceMapFlag;
            setEffect = other.setEffect;
            setQuestFlag = new QuestFlag();
            if (other.setQuestFlag != null) {
                setQuestFlag.key = other.setQuestFlag.key;
                setQuestFlag.val = other.setQuestFlag.val;
            }
            setMapFlag = new QuestFlag();
            if (other.setMapFlag != null) {
                setMapFlag.key = other.setMapFlag.key;
                setMapFlag.val = other.setMapFlag.val;
            }
            grantRewards = other.grantRewards.clone();
            issueQuest = other.issueQuest;
            addMapReputation = other.addMapReputation;
            POIReference = other.POIReference;
        }
    }

    static public class ConditionData implements Serializable {
        private static final long SerialVersionUID = 1L;
        static public class QueryQuestFlag{
            public String key;
            public String op;
            public int val;
        }
        public String item;
        public int actorID = 0;                    //Check for an actor ID.
        public String hasBlessing = null;          //Check for specific blessing, if named.
        public int hasGold = 0;                    //Check for player gold. True if gold is equal or higher than X.
        public int hasShards = 0;                  //Check player's mana shards. True if equal or higher than X.
        public int hasMapReputation = Integer.MIN_VALUE; //Check for player reputation in this POI. True if reputation is equal or higher than X.
        public int hasLife = 0;                    //Check for player life. True if life is equal or higher than X.
        public String colorIdentity = null;        //Check for player's current color identity.
        public String checkCharacterFlag = null;       //Check if a character flag is not 0. False if equals 0 (not started, not set).
        public String checkQuestFlag = null;       //Check if a quest flag is not 0. False if equals 0 (not started, not set).
        public String checkMapFlag = null;         //Check if a map flag is not 0. False if equals 0 (not started, not set).
        public QueryQuestFlag getCharacterFlag = null; //Check for value of a flag { <flagID>, <comparison>, <value> }
        public QueryQuestFlag getQuestFlag = null; //Check for value of a flag { <flagID>, <comparison>, <value> }
        public QueryQuestFlag getMapFlag = null;   //Check for a local dungeon flag ("map flag").
        public boolean not = false;                //Reverse the result of a condition ("actorID":"XX" + "not":true => true if XX is not in the map.)
    }
}
