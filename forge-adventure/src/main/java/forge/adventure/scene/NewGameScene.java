package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.NameGenerator;


public class NewGameScene extends Scene {
    Texture Background;

    public NewGameScene( ) {
        super();
    }

    Stage stage;
    @Override
    public void dispose() {
        stage.dispose();
        Background.dispose();
    }
    Texture image;
    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1,0,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().disableBlending();
        stage.getBatch().draw(Background,0,0,IntendedWidth,IntendedHeight);
        stage.getBatch().enableBlending();
        if(image!=null)
            stage.getBatch().draw(image,0,0);
        stage.getBatch().end();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        //Batch.end();
    }

    public boolean Start()
    {
        FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC,false);
        WorldSave.GenerateNewWorld(selectedName,starterDeck,selectedDiff);
        GamePlayerUtil.getGuiPlayer().setName(selectedName);
        GamePlayerUtil.getGuiPlayer().setAvatarIndex(0);
        //image = new Texture(img);

        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene.instance);
        return true;
    }
    public boolean Back()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.StartScene.instance);
        return true;
    }
    String selectedName;
    WorldSave.Difficulty selectedDiff= WorldSave.Difficulty.Medium;
    Deck starterDeck;
    @Override
    public void ResLoaded()
    {
        // FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC,false);
        stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));

        Table table =new Table(Controls.GetSkin());

        table.add("Name:").align(Align.left);
        table.add(Controls.newTextField(NameGenerator.getRandomName("Any", "Any", ""), s -> {selectedName=s;return null;})).fillX().expandX().align(Align.right);
        table.row();
        table.add("Difficulty:").align(Align.left);
        table.add(Controls.newComboBox(WorldSave.Difficulty.values(),WorldSave.Difficulty.Medium, s -> {selectedDiff=(WorldSave.Difficulty)s;return null;})).fillX().expandX().align(Align.right);
        table.row();
        table.add("StartingDeck:").align(Align.left).fillX().expandX().align(Align.right);
        Deck[] decks=World.StarterDecks();
        table.add(Controls.newComboBox(decks,decks[0], s -> {starterDeck=(Deck)s;return null;})).fillX().expandX().align(Align.right);
        table.row();

        table.add(Controls.newTextButton("Back",()->Back())).fillX().expandX().align(Align.right);

        table.add(Controls.newTextButton("Start",()->Start())).fillX().expandX().align(Align.right);


        table.setPosition(900,500);
        stage.addActor(table);
    }
    @Override
    public void create() {

    }
    @Override
    public void Enter()
    {
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }
}
