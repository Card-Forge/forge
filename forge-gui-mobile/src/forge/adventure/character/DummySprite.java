package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.adventure.stage.MapStage;

public class DummySprite extends MapActor {
    private final TextureRegion textureRegion;
    private final MapStage stage;
    public DummySprite(int id, TextureRegion textureRegion, MapStage stage) {
        super(id);
        this.textureRegion = textureRegion;
        this.stage = stage;
    }

    @Override
    public void onPlayerCollide() { stage.resetPosition(); }

    @Override
    public void draw(Batch batch, float alpha) {
        batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        super.draw(batch, alpha);
    }
}
