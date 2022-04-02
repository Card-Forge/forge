package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.DialogData;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

/**
 * Map actor that will open the Shop on collision
 */
public class DialogActor extends MapActor{
    private final MapStage stage;
    private final String dialogPath;
    private final TextureRegion textureRegion;


    public DialogActor(MapStage stage, int id, String dialog, TextureRegion textureRegion)
    {
        super(id);
        this.stage = stage;
        this.dialogPath = dialog;
        this.textureRegion = textureRegion;
    }


    @Override
    public void  onPlayerCollide()
    {
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(dialogPath);
        if (handle.exists()) {
            Array<DialogData> data = json.fromJson(Array.class, DialogData.class, handle);
            stage.resetPosition();
            stage.showDialog();

            for(DialogData dialog:data)
            {
                if(isConditionOk(dialog.condition))
                {
                    loadDialog(dialog);
                }
            }

        }
    }

    private void loadDialog(DialogData dialog) {

        setEffects(dialog.effect);
        stage.getDialog().getContentTable().clear();
        stage.getDialog().getButtonTable().clear();
        stage.getDialog().text((dialog.text));
        if(dialog.options!=null)
        {
            for(DialogData option:dialog.options)
            {
                if( isConditionOk(option.condition) )
                {
                    stage.getDialog().getButtonTable().add(Controls.newTextButton(option.name,() -> {
                        loadDialog(option);
                    }));
                }
            }
            stage.showDialog();
        }
        else
        {
            stage.hideDialog();
        }
    }
    void setEffects(DialogData.EffectData[] data)
    {
        if(data==null)
            return  ;
        for(DialogData.EffectData effectData:data)
        {
            Current.player().removeItem(effectData.removeItem);
            if(effectData.deleteMapObject<0)
                stage.deleteObject(getObjectId());
            else if(effectData.deleteMapObject>0)
                stage.deleteObject(effectData.deleteMapObject);

        }
    }

    boolean isConditionOk(DialogData.ConditionData[] data)
    {
        if(data==null)
            return true;
        for(DialogData.ConditionData condition:data)
        {
            if(condition.item!=null && !condition.item.equals(""))
            {
                if(!Current.player().hasItem(condition.item))
                {
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
