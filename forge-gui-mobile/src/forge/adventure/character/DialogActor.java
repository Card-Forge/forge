package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.data.RewardData;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

/**
 * Map actor that will show a text message with optional choices
 */
public class DialogActor extends MapActor{
    private final MapStage stage;
    private final String dialogJSON;
    private final TextureRegion textureRegion;
    private final String defaultJSON = "[\n" +
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


    public DialogActor(MapStage stage, int id, String dialog, TextureRegion textureRegion) {
        super(id);
        this.stage = stage;
        if (!dialog.isEmpty()){
            System.err.printf("Dialog error. Dialog property is empty.\n");
            this.dialogJSON = defaultJSON;
        }
        else this.dialogJSON = dialog;
        this.textureRegion = textureRegion;
    }

    @Override
    public void onPlayerCollide() {
        Json json = new Json();
        Array<DialogData> data;
        stage.resetPosition();
        stage.showDialog();

        try { data = json.fromJson(Array.class, DialogData.class, dialogJSON); }
        catch(SerializationException E){
            //JSON parsing could fail. Since this an user written part, assume failure is possible (it happens).
            System.err.printf("[%s] while loading JSON file for dialog actor. JSON:\n%s\nUsing a default dialog.", E.getMessage(), dialogJSON);
            data = json.fromJson(Array.class, DialogData.class, defaultJSON);
        }

        for(DialogData dialog:data) {
            if(isConditionOk(dialog.condition)) {
                loadDialog(dialog);
            }
        }
    }

    private void loadDialog(DialogData dialog) {
        setEffects(dialog.effect);
        stage.getDialog().getContentTable().clear();
        stage.getDialog().getButtonTable().clear();
        String text = "";
        if(dialog.loctext != null && !dialog.loctext.isEmpty()){ //Check for localized string, otherwise print text.
            text = Forge.getLocalizer().getMessage(dialog.loctext);
        } else {
            text = dialog.text;
        }
        stage.getDialog().text(text);
        if(dialog.options != null) {
            for(DialogData option:dialog.options) {
                if( isConditionOk(option.condition) ) {
                    stage.getDialog().getButtonTable().add(Controls.newTextButton(option.name,() -> {
                        loadDialog(option);
                    }));
                }
            }
            stage.showDialog();
        }
        else {
            stage.hideDialog();
        }
    }
    void setEffects(DialogData.EffectData[] data) {
        if(data==null) return;
        for(DialogData.EffectData effectData:data) {
            Current.player().removeItem(effectData.removeItem);
            if(effectData.deleteMapObject<0)
                stage.deleteObject(getObjectId());
            else if(effectData.deleteMapObject>0)
                stage.deleteObject(effectData.deleteMapObject);
        }
    }

    boolean isConditionOk(DialogData.ConditionData[] data) {
        if(data==null) return true;
        for(DialogData.ConditionData condition:data) {
            if(condition.item!=null && !condition.item.equals("")) {
                if(!Current.player().hasItem(condition.item)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void draw(Batch batch, float alpha) {
        batch.draw(textureRegion,getX(),getY(),getWidth(),getHeight());
        super.draw(batch,alpha);
    }

}
