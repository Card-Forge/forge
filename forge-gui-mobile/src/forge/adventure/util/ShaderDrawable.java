package forge.adventure.util;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShaderDrawable implements Drawable {
    private ShaderProgram shader;
    private TextureRegion region;
    private Consumer<ShaderProgram> uniformSetter;
    private Supplier<Boolean> condition;

    public ShaderDrawable(ShaderProgram shader) {
        this.shader = shader;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    // Register a condition that decides whether to use the shader
    public void setCondition(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    // Register a callback for uniforms
    public void setUniformSetter(Consumer<ShaderProgram> setter) {
        this.uniformSetter = setter;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        if (region == null) return;

        // Default: true if no condition set
        boolean useShader = (condition == null) || condition.get();

        ShaderProgram oldShader = batch.getShader();
        if (useShader) {
            batch.setShader(shader);
            if (uniformSetter != null) {
                uniformSetter.accept(shader);
            }
        }

        batch.draw(region, x, y, width, height);

        if (useShader) {
            batch.setShader(oldShader);
        }
    }

    // Implement the other Drawable methods (same as before)
    @Override public float getLeftWidth() { return 0; }
    @Override public void setLeftWidth(float leftWidth) {}
    @Override public float getRightWidth() { return 0; }
    @Override public void setRightWidth(float rightWidth) {}
    @Override public float getTopHeight() { return 0; }
    @Override public void setTopHeight(float topHeight) {}
    @Override public float getBottomHeight() { return 0; }
    @Override public void setBottomHeight(float bottomHeight) {}
    @Override public float getMinWidth() { return region != null ? region.getRegionWidth() : 0; }
    @Override
    public void setMinWidth(float minWidth) {}
    @Override public float getMinHeight() { return region != null ? region.getRegionHeight() : 0; }
    @Override
    public void setMinHeight(float minHeight) {}
}
