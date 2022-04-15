package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.adventure.stage.MapStage;
import forge.adventure.util.MapDialog;

/**
 * Map actor that will show a text message with optional choices
 */
public class DialogActor extends MapActor {
    private final MapStage stage;
    private final TextureRegion textureRegion;
    private final MapDialog dialog;

    public DialogActor(MapStage stage, int id, String S, TextureRegion textureRegion) {
        super(id);
        this.stage = stage;
        dialog = new MapDialog(S, stage, id);
        this.textureRegion = textureRegion;
    }

    @Override
    public void onPlayerCollide() {
        stage.resetPosition();
        stage.showDialog();
        dialog.activate();
    }

    @Override
    public void draw(Batch batch, float alpha) {
        batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        super.draw(batch, alpha);
    }

}
