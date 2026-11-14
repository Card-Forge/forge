package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Adventure;
import forge.Forge;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestLogScene extends UIScene {
    private static final String INK = "[#1f1b16]";
    private static final String INK_SOFT = "[#5d5244]";
    private static final String TRACKED = "[#12808f]";

    private final Table root, detailRoot;
    private final Table listTable = new Table();
    private final Table detailTable = new Table();
    private final ScrollPane listScroll, detailScroll;
    private final TextraButton trackButton, abandonButton;
    private final Button backLink, completedLink;
    private final TextraLabel completedLabel = Controls.newTextraLabel("");
    private final Chevron completedChevron = new Chevron(new Color(0.36f, 0.32f, 0.27f, 1));
    private final Button.ButtonStyle rowStyle = new Button.ButtonStyle(), selectedRowStyle = new Button.ButtonStyle();
    private final List<Button> rows = new ArrayList<>();
    private final boolean split;
    private AdventureQuestData selected;
    private boolean showCompleted;
    Scene lastGameScene;

    private QuestLogScene() {
        super(Forge.isLandscapeMode() ? "ui/quests.json" : "ui/quests_portrait.json");

        ui.findActor("scrollWindow").setTouchable(Touchable.disabled);
        root = ui.findActor("questList");
        detailRoot = ui.findActor("questDetails");
        // overlapping tables show the list and the details one at a time
        split = root.getX() + root.getWidth() <= detailRoot.getX();
        ui.onButtonPress("return", QuestLogScene.this::back);
        ui.onButtonPress("status", QuestLogScene.this::status);
        rowStyle.focused = shade(0.08f);
        selectedRowStyle.up = shade(0.14f);
        selectedRowStyle.focused = shade(0.22f);

        listScroll = scrollPane(listTable);
        root.add(listScroll).expand().fill().top();
        root.row();
        root.add(new MoreArrow(listScroll)).height(11).expandX();

        trackButton = Controls.newTextButton("", () -> toggleTracked(selected));
        abandonButton = Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbandonQuest"), () -> {
            AdventureQuestData quest = selected;
            showDialog(createGenericDialog("", Forge.getLocalizer().getMessage("lblAbandonQuestConfirm"),
                    Forge.getLocalizer().getMessage("lblYes"), Forge.getLocalizer().getMessage("lblNo"), () -> {
                        removeDialog();
                        abandonQuest(quest);
                    }, () -> {
                        removeDialog();
                        updateNavigation();
                    }));
        });
        abandonButton.setColor(Color.RED);
        backLink = link(Controls.newTextraLabel(INK_SOFT + "< " + Forge.getLocalizer().getMessage("lblQuestList")), () -> {
            selected = null;
            buildList();
        });

        detailScroll = scrollPane(detailTable);
        completedLink = link(completedLabel, () -> {
            showCompleted = !showCompleted;
            float scrollY = detailScroll.getScrollY();
            fillDetails(selected);
            detailScroll.setScrollY(scrollY);
            updateNavigation();
        });
        completedLink.add(completedChevron).padRight(2);
        if (!split) {
            detailRoot.add(backLink).left().padBottom(4);
            detailRoot.row();
        }
        detailRoot.add(detailScroll).expand().fill().top();
        detailRoot.row();
        detailRoot.add(new MoreArrow(detailScroll)).height(11).expandX();
        detailRoot.row();
        Table actions = new Table();
        actions.add(trackButton).left().padRight(12);
        actions.add(abandonButton).left();
        detailRoot.add(actions).left().padTop(2);

        if (split) {
            Image divider = new Image(Forge.getAssets().getWhiteTexture());
            divider.setColor(ink(0.35f));
            divider.setBounds((root.getX() + root.getWidth() + detailRoot.getX()) / 2, detailRoot.getY(), 1, detailRoot.getHeight());
            divider.setTouchable(Touchable.disabled);
            ui.addActor(divider);
        }
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
    public void dispose() { }

    @Override
    public void enter() {
        super.enter();
        buildList();
    }

    public void buildList() {
        List<AdventureQuestData> story = questsOfType(true);
        List<AdventureQuestData> side = questsOfType(false);
        if (selected == null || !Current.player().getQuests().contains(selected))
            selected = split ? defaultSelection(story, side) : null;

        rows.clear();
        listTable.clear();
        listTable.top();
        if (story.isEmpty() && side.isEmpty())
            listTable.add(label(INK_SOFT + Forge.getLocalizer().getMessage("lblNoActiveQuests"), root.getWidth())).width(root.getWidth());
        addGroup(story, "lblStoryQuests");
        addGroup(side, "lblSideQuests");

        root.setVisible(split || selected == null);
        detailRoot.setVisible(selected != null);
        if (selected != null)
            fillDetails(selected);
        updateNavigation();
    }

    private List<AdventureQuestData> questsOfType(boolean story) {
        return Current.player().getQuests().stream().filter(q -> q.storyQuest == story).collect(Collectors.toList());
    }

    private AdventureQuestData defaultSelection(List<AdventureQuestData> story, List<AdventureQuestData> side) {
        List<AdventureQuestData> shown = new ArrayList<>(story);
        shown.addAll(side);
        return shown.stream().filter(q -> q.isTracked).findFirst().orElse(shown.isEmpty() ? null : shown.get(0));
    }

    private void addGroup(List<AdventureQuestData> group, String header) {
        if (group.isEmpty())
            return;
        listTable.add(label("[%80]" + INK_SOFT + Forge.getLocalizer().getMessage(header), root.getWidth() - 3))
                .width(root.getWidth() - 3).padTop(listTable.getRows() == 0 ? 0 : 6).padLeft(3);
        listTable.row();
        for (AdventureQuestData quest : group) {
            listTable.add(questRow(quest)).expandX().fillX();
            listTable.row();
        }
    }

    private Button questRow(AdventureQuestData quest) {
        Button row = new Button(split && quest == selected ? selectedRowStyle : rowStyle);
        row.setUserObject(quest);
        int stages = quest.stages.length;
        float progressWidth = 30;
        row.add(label(TRACKED + (quest.isTracked ? "•" : ""), 8)).width(8).top().padLeft(3);
        row.add(label(INK + quest.getName(), root.getWidth() - progressWidth - 20)).width(root.getWidth() - progressWidth - 20).left().padLeft(2);
        row.add(Controls.newTextraLabel("[%80]" + INK_SOFT + (stages == 0 ? "" : quest.getCompletedStages().size() + "/" + stages)))
                .width(progressWidth).top().right().padRight(3);
        row.pad(3, 0, 3, 0);
        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(quest);
            }
        });
        rows.add(row);
        return row;
    }

    private void select(AdventureQuestData quest) {
        selected = quest;
        showCompleted = false;
        if (split) {
            for (Button row : rows)
                row.setStyle(row.getUserObject() == quest ? selectedRowStyle : rowStyle);
        } else {
            root.setVisible(false);
            detailRoot.setVisible(true);
        }
        fillDetails(quest);
        updateNavigation();
    }

    private void updateNavigation() {
        // scroll keys act on the scroll focus, and the details have nothing to select, so they take it whenever shown
        stage.setScrollFocus(detailRoot.isVisible() ? detailScroll : listScroll);
        clearSelectable();
        if (root.isVisible())
            rows.forEach(this::addToSelectable);
        if (!detailRoot.isVisible())
            return;
        if (!split)
            addToSelectable(backLink);
        if (!selected.getCompletedStages().isEmpty())
            addToSelectable(completedLink);
        addToSelectable(trackButton);
        if (!selected.storyQuest)
            addToSelectable(abandonButton);
    }

    private void fillDetails(AdventureQuestData quest) {
        float width = detailRoot.getWidth() - 4;
        detailTable.clear();
        detailTable.top().left();
        detailTable.defaults().left().padBottom(3);
        String tracked = quest.isTracked ? "   " + TRACKED + "• " + Forge.getLocalizer().getMessage("lblTracked") : "";
        detailTable.add(label("[%80]" + INK_SOFT + Forge.getLocalizer().getMessage(quest.storyQuest ? "lblStoryQuest" : "lblSideQuest") + tracked, width)).width(width);
        detailTable.row();
        detailTable.add(label("[%125]" + INK + quest.getName(), width)).width(width);
        detailTable.row();
        if (!quest.getDescription().isEmpty()) {
            detailTable.add(label("[%90]" + INK_SOFT + quest.getDescription(), width)).width(width);
            detailTable.row();
        }
        if (quest.stages.length > 0) {
            Image rule = new Image(Forge.getAssets().getWhiteTexture());
            rule.setColor(ink(0.35f));
            detailTable.add(rule).width(width).height(1).padTop(2).padBottom(5);
            detailTable.row();
        }
        for (AdventureQuestStage stage : quest.getActiveStages()) {
            detailTable.add(label(INK + "☐ " + stage.name, width)).width(width);
            detailTable.row();
            if (!stage.description.isEmpty()) {
                detailTable.add(label("[%90]" + INK_SOFT + stage.description, width - 12)).width(width - 12).padLeft(12);
                detailTable.row();
            }
        }
        List<AdventureQuestStage> completed = quest.getCompletedStages();
        if (!completed.isEmpty()) {
            completedLabel.setText(INK_SOFT + "☑ " + Forge.getLocalizer().getMessage("lblCompletedStages", completed.size()));
            completedChevron.up = showCompleted;
            completedLink.invalidateHierarchy();
            detailTable.add(completedLink).padLeft(-2);
            detailTable.row();
        }
        if (showCompleted) {
            for (AdventureQuestStage stage : completed) {
                detailTable.add(label(INK_SOFT + "☑ " + stage.name, width)).width(width);
                detailTable.row();
            }
        }
        detailScroll.setScrollY(0);
        detailScroll.layout();
        updateTrackButton(quest.isTracked);
        abandonButton.setVisible(!quest.storyQuest);
    }

    private TextraLabel label(String text, float width) {
        TextraLabel label = Controls.newTextraLabel(text);
        label.setWrap(true);
        label.setWidth(width);
        return label;
    }

    private Button link(TextraLabel label, Runnable action) {
        Button link = new Button(rowStyle);
        link.add(label).pad(1, 2, 1, 2);
        link.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return link;
    }

    private ScrollPane scrollPane(Table content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setScrollingDisabled(true, false);
        // a ScrollPane only takes scroll focus on touch-down, so hovering hands it the mouse wheel
        pane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1)
                    stage.setScrollFocus(pane);
            }
        });
        return pane;
    }

    private static Color ink(float alpha) {
        return new Color(0.12f, 0.11f, 0.09f, alpha);
    }

    private static Drawable shade(float alpha) {
        return new TextureRegionDrawable(Forge.getAssets().getWhiteTexture()).tint(ink(alpha));
    }

    private void toggleTracked(AdventureQuestData quest) {
        quest.isTracked = !quest.isTracked;
        if (quest.isTracked) {
            AdventureQuestController.trackQuest(quest);
        }
        float scrollY = detailScroll.getScrollY();
        buildList();
        detailScroll.setScrollY(scrollY);
    }

    private void updateTrackButton(boolean isTracked) {
        if (isTracked) {
            trackButton.setText(Forge.getLocalizer().getMessage("lblUntrackQuest"));
        } else {
            trackButton.setText(Forge.getLocalizer().getMessage("lblTrackQuest"));
        }
        trackButton.invalidateHierarchy();
    }

    private void status() {
        Forge.switchScene(PlayerStatisticScene.instance(lastGameScene),true);
    }

    @Override
    public boolean back(){
        Adventure.getInstance().renderTransitionScreen = true;
        //Needed so long as quest log and stats are separate scenes that link to each other
        Forge.switchScene(lastGameScene==null ? GameScene.instance() : lastGameScene);
        return true;
    }

    private void abandonQuest(AdventureQuestData quest) {
        AdventureQuestController.instance().abandon(quest);
        AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
        selected = null;
        buildList();
    }

    private static class Chevron extends Actor {
        static final float PX = 1.5f;
        private final Color color;
        boolean up;

        Chevron(Color color) {
            this.color = color;
            setSize(7 * PX, 4 * PX);
            setTouchable(Touchable.disabled);
        }

        float bob() {
            return 0;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float packed = batch.getPackedColor();
            batch.setColor(color.r, color.g, color.b, parentAlpha);
            for (int row = 0; row < 4; row++)
                batch.draw(Forge.getAssets().getWhiteTexture(), getX() + row * PX, getY() + bob() + (up ? row : 3 - row) * PX, (7 - row * 2) * PX, PX);
            batch.setPackedColor(packed);
        }
    }

    private static class MoreArrow extends Chevron {
        private final ScrollPane pane;
        private float elapsed;

        MoreArrow(ScrollPane pane) {
            super(new Color(0.12f, 0.11f, 0.09f, 1));
            this.pane = pane;
            setSize(7 * PX, 6 * PX);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            elapsed += delta;
        }

        @Override
        float bob() {
            return PX * (1 - MathUtils.cos(elapsed * MathUtils.PI2 / 1.2f));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (pane.getMaxY() - pane.getScrollY() > 1)
                super.draw(batch, parentAlpha);
        }
    }
}
