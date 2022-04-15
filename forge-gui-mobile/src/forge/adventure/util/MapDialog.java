package forge.adventure.util;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.stage.MapStage;

public class MapDialog {
    private final MapStage stage;
    private Array<DialogData> data;
    private final int parentID;

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

    private void loadDialog(DialogData dialog) {
        setEffects(dialog.effect);
        stage.getDialog().getContentTable().clear();
        stage.getDialog().getButtonTable().clear();
        String text;
        if(dialog.loctext != null && !dialog.loctext.isEmpty()){ //Check for localized string, otherwise print text.
            text = Forge.getLocalizer().getMessage(dialog.loctext);
        } else {
            text = dialog.text;
        }

        int charCount = 0;

        stage.getDialog().text(text);
        if(dialog.options != null) {
            for(DialogData option:dialog.options) {
                if( isConditionOk(option.condition) ) {
                    charCount += option.name.length();
                    if(charCount > 35){ //Gross hack.
                        stage.getDialog().getButtonTable().row();
                        charCount = 0;
                    }
                    stage.getDialog().getButtonTable().add(Controls.newTextButton(option.name,() -> loadDialog(option)));

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
            if (E.removeItem != null){
                Current.player().removeItem(E.removeItem);
            }
            if (E.addItem != null){
                Current.player().addItem(E.addItem);
            }
            if (E.deleteMapObject != 0){
                if(E.deleteMapObject < 0) stage.deleteObject(parentID);
                else stage.deleteObject(E.deleteMapObject);
            }
            if (E.battleWithActorID != 0){
                if(E.battleWithActorID < 0) stage.beginDuel(stage.getEnemyByID(parentID));
                else stage.beginDuel(stage.getEnemyByID(E.battleWithActorID));
            }



        }
    }

    boolean isConditionOk(DialogData.ConditionData[] data) {
        if(data==null) return true;
        for(DialogData.ConditionData condition:data) {
            if(condition.item != null && !condition.item.isEmpty()) { //Check for item.
                if(Current.player().hasItem(condition.item)) {
                    return ((condition.not) ? false : true);
                }
            }
            if(condition.actorID != 0) { //Check for actor ID.
                boolean result = stage.lookForID(condition.actorID);
                if(condition.not) result = !result;
                return result;
            }
        }
        return true;
    }
}
