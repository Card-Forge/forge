package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

public class QuestLogScene extends UIScene {
    private Table scrollContainer, detailScrollContainer;
    Window scrollWindow;
    ScrollPane scroller,detailScroller;
    Table root, detailRoot;
    TextraButton trackButton, backToListButton, abandonQuestButton;
    Scene lastGameScene;

    private QuestLogScene() {
        super(Forge.isLandscapeMode() ? "ui/quests.json" : "ui/quests_portrait.json");


        scrollWindow = ui.findActor("scrollWindow");
        root = ui.findActor("questList");
        detailRoot = ui.findActor("questDetails");
        abandonQuestButton = Controls.newTextButton("Abandon Quest");
        trackButton = Controls.newTextButton("Track Quest");
        backToListButton = Controls.newTextButton("Quest List");
        ui.onButtonPress("return", QuestLogScene.this::back);
        ui.onButtonPress("status", QuestLogScene.this::status);
        ui.onButtonPress("backToList", QuestLogScene.this::backToList);


        //Todo - refactor below, replace buttons in landscape

        scrollContainer = new Table(Controls.getSkin());
        scrollContainer.row();

        detailScrollContainer = new Table(Controls.getSkin());
        detailScrollContainer.row();

        detailScroller = new ScrollPane(detailScrollContainer);
        detailScroller.setScrollingDisabled(true,false);
        if (Forge.isLandscapeMode()) {
            detailRoot.add(abandonQuestButton).fillX().top().padTop(5f);
            detailRoot.add(trackButton).fillX().top().padTop(5f);
            detailRoot.add(backToListButton).fillX().top().padTop(5f);
        } else {
            detailRoot.add(abandonQuestButton).fillX().top().padTop(5f).colspan(3);
            detailRoot.row();
            detailRoot.add(trackButton).fillX().top().padTop(5f).colspan(3);
            detailRoot.row();
            detailRoot.add(backToListButton).fillX().top().padTop(5f).colspan(3);
        }

        detailRoot.row();
        detailRoot.add(detailScroller).colspan(3).expandX().fillX().expandY();
        detailRoot.row();
        scrollWindow.setTouchable(Touchable.disabled);
        detailRoot.setVisible(false);
        scroller = new ScrollPane(scrollContainer);
        root.add(scroller).colspan(3);
        root.align(Align.right);
        root.row();
        Label column0Label = new Label(Forge.getLocalizer().getMessage("lblQuestName"), Controls.getSkin());
        column0Label.setColor(Color.BLACK);
        root.add(column0Label).align(Align.bottomLeft);
        root.row();
        ScrollPane scroller = new ScrollPane(scrollContainer);
        root.add(scroller).colspan(3).fill().expand();



    }

    private static QuestLogScene object;

    public static QuestLogScene instance(Scene lastGameScene) {
        //if (object == null)
            object = new QuestLogScene();
        if (lastGameScene != null)
            object.lastGameScene=lastGameScene;
        return object;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void enter() {
        super.enter();
        buildList();


    }

    public void buildList(){
        backToList();

        scrollContainer.clear();

        for (AdventureQuestData quest : Current.player().getQuests()) {
            TypingLabel nameLabel = Controls.newTypingLabel(quest.getName());
            nameLabel.skipToTheEnd();
            nameLabel.setWrap(true);
            nameLabel.setColor(Color.BLACK);
            scrollContainer.add(nameLabel).align(Align.left).expandX();
            Button details = Controls.newTextButton(Forge.getLocalizer().getMessage("lblDetails"));
            details.addListener( new ClickListener(){
                public void clicked(InputEvent event, float x, float y){
                    loadDetailsPane(quest);
                }
            });
            scrollContainer.add(details).align(Align.center).padRight(10);
            scrollContainer.row().padTop(5);
            addToSelectable(details);
        }
        performTouch(scrollPaneOfActor(scrollContainer)); //can use mouse wheel if available to scroll
    }

    private void backToList(){
        abandonQuestButton.setVisible(false);
        trackButton.setVisible(false);
        backToListButton.setVisible(false);

        root.setVisible(true);
        detailRoot.setVisible(false);
    }

    private void loadDetailsPane(AdventureQuestData quest){
        if (quest == null){
            return;
        }
        root.setVisible(false);
        detailRoot.setVisible(true);
        detailScrollContainer.clear();
        detailScrollContainer.row();
        trackButton.setText(quest.isTracked?Forge.getLocalizer().getMessage("lblUntrackQuest"):Forge.getLocalizer().getMessage("lblTrackQuest"));
        trackButton.addListener( new ClickListener(){
            public void clicked(InputEvent event, float x, float y){
                toggleTracked(quest);
            }
        });

        abandonQuestButton.setColor(Color.RED);
        abandonQuestButton.addListener( new ClickListener(){
            public void clicked(InputEvent event, float x, float y){

                Dialog confirm = createGenericDialog("", Forge.getLocalizer().getMessage("lblAbandonQuestConfirm"),Forge.getLocalizer().getMessage("lblYes"),Forge.getLocalizer().getMessage("lblNo"), () -> abandonQuest(quest), null);
                showDialog(confirm);
            }
        });

        TypingLabel dNameLabel = Controls.newTypingLabel(quest.getName());
        dNameLabel.skipToTheEnd();
        dNameLabel.setWrap(true);
        dNameLabel.setColor(Color.BLACK);

        detailScrollContainer.add(dNameLabel).align(Align.left).expandX().padLeft(10);

        abandonQuestButton.setVisible(!quest.storyQuest);
        trackButton.setVisible(true);
        backToListButton.setVisible(true);

        TypingLabel dDescriptionLabel = Controls.newTypingLabel(quest.getDescription());
        dDescriptionLabel.skipToTheEnd();
        dDescriptionLabel.setWrap(true);
        dDescriptionLabel.setColor(Color.DARK_GRAY);

        detailScrollContainer.row();
        detailScrollContainer.add(dDescriptionLabel).align(Align.left).padLeft(25).width(detailRoot.getWidth() -25);

        for (AdventureQuestStage stage : quest.getCompletedStages()) {
            TypingLabel completeLabel = Controls.newTypingLabel("*  " + stage.name);
            completeLabel.skipToTheEnd();
            completeLabel.setColor(Color.GREEN);
            completeLabel.setWrap(true);
            detailScrollContainer.row();
            detailScrollContainer.add(completeLabel).align(Align.left).padLeft(25);
        }

        for (AdventureQuestStage stage : quest.getActiveStages()) {
            TypingLabel activeLabel = Controls.newTypingLabel("*  " + stage.name);
            activeLabel.skipToTheEnd();
            activeLabel.setColor(Color.BLACK);
            activeLabel.setWrap(true);
            detailScrollContainer.row();
            detailScrollContainer.add(activeLabel).align(Align.left).padLeft(25);

            TypingLabel activeDescriptionLabel = Controls.newTypingLabel(stage.description);
            activeDescriptionLabel.skipToTheEnd();
            activeDescriptionLabel.setColor(Color.DARK_GRAY);
            activeDescriptionLabel.setWrap(true);

            detailScrollContainer.row();
            detailScrollContainer.add(activeDescriptionLabel).padLeft(35).width(detailRoot.getWidth() - 50);
            detailScrollContainer.row();
        }
    }

    private void toggleTracked(AdventureQuestData quest){
        if (quest.isTracked){
            quest.isTracked = false;
            trackButton.setText(Forge.getLocalizer().getMessage("lblTrackQuest"));
        } else {
            AdventureQuestController.trackQuest(quest);
            trackButton.setText(Forge.getLocalizer().getMessage("lblUntrackQuest"));
        }
    }

    private void status() {
        Forge.switchScene(PlayerStatisticScene.instance(lastGameScene),true);
    }

    @Override
    public boolean back(){
        //Needed so long as quest log and stats are separate scenes that link to each other
        Forge.switchScene(lastGameScene==null?GameScene.instance():lastGameScene);
        return true;
    }

    private void abandonQuest(AdventureQuestData quest) {
        AdventureQuestController.instance().abandon(quest);
        AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
        buildList();
    }

}
