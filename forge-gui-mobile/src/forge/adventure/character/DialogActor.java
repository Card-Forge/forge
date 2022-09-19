package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.adventure.stage.MapStage;
import forge.adventure.util.MapDialog;

/**
 * Map actor that will show a text message with optional choices
 */
public class DialogActor extends CharacterSprite {
    private final MapStage stage;
    private final TextureRegion textureRegion;
    private final MapDialog dialog;

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

    @Override
    public void onPlayerCollide() {
        stage.resetPosition();
        stage.showDialog();
        dialog.activate();
    }

    @Override
    public void draw(Batch batch, float alpha) {
        if(textureRegion!=null)
            batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        else
            super.draw(batch, alpha);
    }

}
