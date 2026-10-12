package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.stage.MapStage;
import forge.adventure.util.MapDialog;

/**
 * Map actor that will show a text message with optional choices
 */
public class DialogActor extends CharacterSprite {
    protected final MapStage stage;
    private final TextureRegion textureRegion;
    protected MapDialog dialog;
    public AdventureQuestData questData;

    private String lazyDialogText = null;
    private String lazySourceMapFile = null;
    public DialogActor(MapStage stage, int id, String S, TextureRegion textureRegion) {
        this(stage, id, S, textureRegion, null);
    }

    public DialogActor(MapStage stage, int id, String S, TextureRegion textureRegion, String sourceMapFile) {
        super(id, "");
        this.stage = stage;
        this.textureRegion = textureRegion;
        this.lazyDialogText = S;
        this.lazySourceMapFile = sourceMapFile;
    }

    public DialogActor(MapStage stage, int id, String S, String sprite) {
        this(stage, id, S, sprite, null);
    }

    public DialogActor(MapStage stage, int id, String S, String sprite, String sourceMapFile) {
        super(id, sprite);
        this.stage = stage;
        this.textureRegion = null;
        this.lazyDialogText = S;
        this.lazySourceMapFile = sourceMapFile;
    }

    public DialogActor(AdventureQuestData data, MapStage stage, int id) {
        super(id, "");
        this.stage = stage;
        this.textureRegion = null;
        this.questData = data;
    }

    public void removeFromMap() {
        dialog = null;
    }

    @Override
    public void onPlayerCollide() {
        if (dialog == null && lazyDialogText != null) {
            // move here so we only create dialog if we collide and need to interact with it. With this
            // if we walk past behind the actor, we don't waste initiating the MapDialog since its heavy
            // and you save parsing JSON which needs some attention since it populates the heap on the jfr
            dialog = new MapDialog(lazyDialogText, stage, objectId, lazySourceMapFile);
        }

        if (dialog != null) {
            if (dialog.activate()) {
                stage.resetPosition();
                stage.showDialog();
            }
        }
    }

    @Override
    public void draw(Batch batch, float alpha) {
        if (textureRegion != null) {
            batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        } else {
            super.draw(batch, alpha);
        }
    }
}
