package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.stage.MapStage;
import forge.adventure.util.MapDialog;

/**
 * Map actor that will show a text message with optional choices
 */
public class DialogActor extends CharacterSprite {
    private final MapStage stage;
    private final TextureRegion textureRegion;
    private MapDialog dialog;
    public AdventureQuestData questData;

    public DialogActor(MapStage stage, int id, String S, TextureRegion textureRegion) {
        super(id,"");
        this.stage = stage;
        dialog = new MapDialog(S, stage, id);
        this.textureRegion = textureRegion;
    }
    public DialogActor(MapStage stage, int id, String S, String sprite) {
        super(id,sprite);
        this.stage = stage;
        dialog = new MapDialog(S, stage, id);
        this.textureRegion = null;
    }

    public DialogActor(AdventureQuestData data, MapStage stage, int id){
        super(id,"");
        this.stage = stage;
        dialog = new MapDialog(data.offerDialog, stage, id);
        this.textureRegion = null;
        this.questData = data;

        ChangeListener finished = new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                removeFromMap();
            }
        };
        dialog.addDialogCompleteListener(finished);
    }

    public void removeFromMap() { dialog = null; }

    @Override
    public void onPlayerCollide() {
        if (dialog != null) {
            if (dialog.activate()){
                stage.resetPosition();
                stage.showDialog();
            }
        }
    }

    @Override
    public void draw(Batch batch, float alpha) {
        if(textureRegion!=null)
            batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        else
            super.draw(batch, alpha);
    }

}
