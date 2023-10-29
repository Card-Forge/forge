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
        abandonQuestButton = ui.findActor("abandonQuest");
        trackButton = ui.findActor("trackQuest");
        backToListButton = ui.findActor("backToList");//Controls.newTextButton("Quest List");
        ui.onButtonPress("return", QuestLogScene.this::back);
        ui.onButtonPress("status", QuestLogScene.this::status);
        ui.onButtonPress("backToList", QuestLogScene.this::backToList);


        //Todo - refactor below, replace buttons in landscape

        scrollContainer = new Table(Controls.getSkin());
        scrollContainer.row();

        detailScrollContainer = new Table(Controls.getSkin());
        detailScrollContainer.row();

        detailScroller = new ScrollPane(detailScrollContainer);
        detailRoot.row();
        detailRoot.add(detailScroller).expandX().fillX();
        detailRoot.row();
        scrollWindow.setTouchable(Touchable.disabled);
        detailRoot.setVisible(false);
        scroller = new ScrollPane(scrollContainer);
        root.add(scroller).colspan(3);
        root.align(Align.right);
        root.row();
        Label column0Label = new Label("Quest Name", Controls.getSkin());
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
            Button details = Controls.newTextButton("Details");
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
        detailScrollContainer.row();
        trackButton.setText(quest.isTracked?"Untrack Quest":"Track Quest");
        trackButton.addListener( new ClickListener(){
            public void clicked(InputEvent event, float x, float y){
                toggleTracked(quest);
            }
        });

        abandonQuestButton.setColor(Color.RED);
        abandonQuestButton.addListener( new ClickListener(){
            public void clicked(InputEvent event, float x, float y){

                Dialog confirm = createGenericDialog("", "Abandon Quest?","Yes","No", () -> abandonQuest(quest), null);
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
        detailScrollContainer.add(dDescriptionLabel).align(Align.left).padLeft(25);

        for (AdventureQuestStage stage : quest.getStagesForQuestLog()){
            // Todo: Eventually needs to be multiple loops or sort stages by status
            //       because parallel objectives will make this messy
            switch (stage.getStatus()){
                case Complete:
                TypingLabel completeLabel = Controls.newTypingLabel("*  " + stage.name);
                completeLabel.skipToTheEnd();
                completeLabel.setColor(Color.GREEN);
                completeLabel.setWrap(true);
                detailScrollContainer.row();
                detailScrollContainer.add(completeLabel).align(Align.left).padLeft(25);
                break;
            case Failed:
                TypingLabel failedLabel = Controls.newTypingLabel("*  " + stage.name);
                failedLabel.skipToTheEnd();
                failedLabel.setColor(Color.RED);
                failedLabel.setText(stage.name);
                failedLabel.setWrap(true);
                detailScrollContainer.row();
                detailScrollContainer.add(failedLabel).align(Align.left).padLeft(25);
                break;
            case Active:
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
                detailScrollContainer.add(activeDescriptionLabel).padLeft(35).width(scrollWindow.getWidth() - 50).colspan(4);
                detailScrollContainer.row();
                break;
            }
        }
    }

    private void toggleTracked(AdventureQuestData quest){
        quest.isTracked = !quest.isTracked;
        if (quest.isTracked){
            for (AdventureQuestData q: Current.player().getQuests()){
                if (q.equals(quest))
                    continue;
                q.isTracked = false;
            }
        }
        trackButton.setText(quest.isTracked?"Untrack Quest":"Track Quest");
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
