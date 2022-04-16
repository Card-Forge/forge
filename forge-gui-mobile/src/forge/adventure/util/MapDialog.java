package forge.adventure.util;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.stage.MapStage;
import forge.util.Localizer;

/**
 * MapDialog
 * Implements a dialogue/event tree for dialogs.
 */

public class MapDialog {
    private final MapStage stage;
    private Array<DialogData> data;
    private final int parentID;
    private final float WIDTH = 260f;
    static private final String defaultJSON = "[\n" +
            "  {\n" +
            "    \"effect\":[],\n" +
            "    \"name\":\"Error\",\n" +
            "    \"text\":\"This is a fallback dialog.\\nPlease check Forge logs for errors.\",\n" +
            "    \"condition\":[],\n" +
            "    \"options\":[\n" +
            "        { \"name\":\"OK\" }\n" +
            "    ]\n" +
            "  }\n" +
            "]";


    public MapDialog(String S, MapStage ST, int parentID) {
        this.stage = ST;
        this.parentID = parentID;
        Json json = new Json();
        if (S.isEmpty()){
            System.err.print("Dialog error. Dialog property is empty.\n");
            this.data = json.fromJson(Array.class, DialogData.class, defaultJSON);
            return;
        }
        try { data = json.fromJson(Array.class, DialogData.class, S); }
        catch(SerializationException E){
            //JSON parsing could fail. Since this an user written part, assume failure is possible (it happens).
            System.err.printf("[%s] while loading JSON file for dialog actor. JSON:\n%s\nUsing a default dialog.", E.getMessage(), S);
            this.data = json.fromJson(Array.class, DialogData.class, defaultJSON);
        }
    }

    private void loadDialog(DialogData dialog) { //Displays a dialog with dialogue and possible choices.
        setEffects(dialog.effect);
        Dialog D = stage.getDialog();
        Localizer L = Forge.getLocalizer();
        D.getContentTable().clear(); D.getButtonTable().clear(); //Clear tables to start fresh.
        String text; //Check for localized string (locname), otherwise print text.
        if(dialog.loctext != null && !dialog.loctext.isEmpty()) text = L.getMessage(dialog.loctext);
        else text = dialog.text;
        Label A = Controls.newLabel(text);
        A.setWrap(true);
        D.getContentTable().add(A).width(WIDTH); //Add() returns a Cell, which is what the width is being applied to.
        if(dialog.options != null) {
            for(DialogData option:dialog.options) {
                if( isConditionOk(option.condition) ) {
                    String name; //Get localized label if present.
                    if(option.locname != null && !option.locname.isEmpty()) name = L.getMessage(option.locname);
                    else name = option.name;
                    TextButton B = Controls.newTextButton(name,() -> loadDialog(option));
                    B.getLabel().setWrap(true); //We want this to wrap in case it's a wordy choice.
                    D.getButtonTable().add(B).width(WIDTH - 10); //The button table also returns a Cell when adding.
                    D.getButtonTable().row(); //Add a row. Tried to allow a few per row but it was a bit erratic.
                }
            }
            stage.showDialog();
        }
        else {
            stage.hideDialog();
        }
    }

    public void activate() {
        for(DialogData dialog:data) {
            if(isConditionOk(dialog.condition)) {
                loadDialog(dialog);
            }
        }
    }

    void setEffects(DialogData.EffectData[] data) {
        if(data==null) return;
        for(DialogData.EffectData E:data) {
            if (E.removeItem != null){ //Removes an item from the player's inventory.
                Current.player().removeItem(E.removeItem);
            }
            if (E.addItem != null){ //Gives an item to the player.
                Current.player().addItem(E.addItem);
            }
            if (E.deleteMapObject != 0){ //Removes a dummy object from the map.
                if(E.deleteMapObject < 0) stage.deleteObject(parentID);
                else stage.deleteObject(E.deleteMapObject);
            }
            if (E.battleWithActorID != 0){ //Starts a battle with the given enemy ID.
                if(E.battleWithActorID < 0) stage.beginDuel(stage.getEnemyByID(parentID));
                else stage.beginDuel(stage.getEnemyByID(E.battleWithActorID));
            }
            //Create map object.
            //Check for quest flags, local.
            //Check for quest flags, global.
        }
    }

    boolean isConditionOk(DialogData.ConditionData[] data) {
        if(data==null) return true;
        for(DialogData.ConditionData condition:data) {
            if(condition.item != null && !condition.item.isEmpty()) { //Check for item.
                if(!Current.player().hasItem(condition.item)) {
                    if(!condition.not) return false; //Only return on a false.
                } else if(condition.not) return false;
            }
            if(condition.actorID != 0) { //Check for actor ID.
                if(!stage.lookForID(condition.actorID)){
                    if(!condition.not) return false; //Same as above.
                } else if(condition.not) return false;
            }
        }
        return true;
    }
}
