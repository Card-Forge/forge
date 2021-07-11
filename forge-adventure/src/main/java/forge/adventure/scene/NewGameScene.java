package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.adventure.world.WorldSave;

public class NewGameScene extends Scene {
    Texture Background;

    public NewGameScene( ) {
        super();
    }

    @Override
    public void dispose() {
        Stage.dispose();
        Background.dispose();
    }
    Texture image;
    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1,0,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().disableBlending();
        Stage.getBatch().draw(Background,0,0,IntendedWidth,IntendedHeight);
        Stage.getBatch().enableBlending();
        if(image!=null)
        Stage.getBatch().draw(image,0,0);
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        //Batch.end();
    }

    public boolean Start()
    {
        FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC,false);
        //AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene);
        Pixmap img=WorldSave.GenerateNewWorld();
        image = new Texture(img);
        return true;
    }
    public boolean Back()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.StartScene.instance);
        return true;
    }
    @Override
    public void create() {
       // FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC,false);
        Stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));

        VerticalGroup nameGroup =new VerticalGroup();


        nameGroup.addActor(Controls.newTextButton("Start",()->Start()));
        nameGroup.addActor(Controls.newTextButton("Back",()->Back()));

        nameGroup.addActor(Controls.newTextButton("Start",()->Start()));
        nameGroup.addActor(Controls.newTextButton("Back",()->Back()));


        nameGroup.setPosition(900,500);
        Stage.addActor(nameGroup);
    }
}
