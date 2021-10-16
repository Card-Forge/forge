package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.util.Config;
import forge.adventure.util.UIActor;

/**
 * Base class for an GUI scene where the elements are loaded from a json file
 */
public class UIScene extends Scene{
    protected UIActor ui;
    Stage stage;

    String uiFile;
    public UIScene(String uiFilePath) {

        uiFile=uiFilePath;
    }

    @Override
    public void dispose() {
        if(stage!=null)
            stage.dispose();
    }
    @Override
    public void act(float delta) {

        stage.act(delta);
    }
    @Override
    public void render() {

        Gdx.gl.glClearColor(0, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    public UIActor getUI()
    {
        return ui;
    }
    @Override
    public void resLoaded() {
        stage = new Stage(new StretchViewport(GetIntendedWidth(), GetIntendedHeight()));
        ui = new UIActor(Config.instance().getFile(uiFile));
        screenImage=ui.findActor("lastScreen");
        stage.addActor(ui);

    }
    Image screenImage;
    TextureRegion backgroundTexture;
    @Override
    public void enter() {
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui


    if(screenImage!=null)
    {
        if (backgroundTexture != null)
            backgroundTexture.getTexture().dispose();

        final Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        final Pixmap potPixmap = new Pixmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Pixmap.Format.RGBA8888);
        potPixmap.setBlending(Pixmap.Blending.SourceOver);
        potPixmap.drawPixmap(pixmap, 0, 0);
        potPixmap.setColor(0,0,0,0.75f);
        potPixmap.fillRectangle(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        backgroundTexture = new TextureRegion(new Texture(potPixmap), 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());
        screenImage.setDrawable(new TextureRegionDrawable(backgroundTexture));
    }

        super.enter();
    }


}
