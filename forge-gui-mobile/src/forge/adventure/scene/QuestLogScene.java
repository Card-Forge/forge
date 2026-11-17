package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.Line;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingAdapter;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Adventure;
import forge.Forge;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestLogScene extends UIScene {
    private static final String INK = "[#1f1b16]";
    private static final String INK_SOFT = "[#5d5244]";
    private static final String TRACKED = "[#12808f]";
    private static final Color INK_SOFT_COLOR = new Color(0.36f, 0.32f, 0.27f, 1);
    private static final String DONE = "[#3b8a33]";
    private static final Color DONE_COLOR = new Color(0.23f, 0.54f, 0.2f, 1);

    private final Table root, detailRoot;
    private final Table listTable = new Table();
    private final Table detailTable = new Table();
    private final ScrollPane listScroll, detailScroll;
    private final TextraButton trackButton, abandonButton;
    private final Button backLink, completedLink;
    private final TextraLabel completedLabel = Controls.newTextraLabel("");
    private final Chevron completedChevron = new Chevron(INK_SOFT_COLOR, false);
    private final Button.ButtonStyle rowStyle = new Button.ButtonStyle(), selectedRowStyle = new Button.ButtonStyle();
    private final List<Button> rows = new ArrayList<>();
    private final Font nameFont = new Font(Controls.getTextraFont()).adjustLineHeight(0.75f);
    private final Map<AdventureQuestData, NewTag> newTags = new HashMap<>();
    private List<AdventureQuestData> finished = new ArrayList<>();
    private boolean animate;
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

        // the landscape layout puts the quest actions in the side column beside Status and Back
        TextraButton sideTrack = ui.findActor("track");
        if (sideTrack != null) {
            trackButton = sideTrack;
            abandonButton = ui.findActor("abandon");
            ui.onButtonPress("track", () -> toggleTracked(selected));
            ui.onButtonPress("abandon", this::confirmAbandon);
        } else {
            trackButton = Controls.newTextButton("", () -> toggleTracked(selected));
            abandonButton = Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbandonQuest"), this::confirmAbandon);
        }
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
        completedLink.add(completedChevron).padLeft(2).padRight(2);
        if (!split) {
            detailRoot.add(backLink).left().padBottom(4);
            detailRoot.row();
        }
        detailRoot.add(detailScroll).expand().fill().top();
        detailRoot.row();
        detailRoot.add(new MoreArrow(detailScroll)).height(11).expandX();
        if (sideTrack == null) {
            detailRoot.row();
            Table actions = new Table();
            actions.add(trackButton).left().padRight(12);
            actions.add(abandonButton).left();
            detailRoot.add(actions).left().padTop(2);
        }

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
        List<AdventureQuestData> unseen = Current.player().getUnseenFinishedQuests();
        finished = new ArrayList<>(unseen);
        unseen.clear();
        animate = true;
        buildList();
        animate = false;
    }

    public void buildList() {
        List<AdventureQuestData> story = questsOfType(true);
        List<AdventureQuestData> side = questsOfType(false);
        if (selected == null || !Current.player().getQuests().contains(selected))
            selected = split ? defaultSelection(story, side) : null;

        rows.clear();
        newTags.clear();
        listTable.clear();
        listTable.top();
        if (story.isEmpty() && side.isEmpty())
            listTable.add(label(INK_SOFT + Forge.getLocalizer().getMessage("lblNoActiveQuests"), root.getWidth())).width(root.getWidth());
        addGroup(story, true, "lblStoryQuests");
        addGroup(side, false, "lblSideQuests");

        root.setVisible(split || selected == null);
        detailRoot.setVisible(selected != null);
        if (selected != null) {
            fillDetails(selected);
        } else {
            trackButton.setVisible(false);
            abandonButton.setVisible(false);
        }
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

    private void addGroup(List<AdventureQuestData> group, boolean story, String header) {
        List<AdventureQuestData> done = animate ? finished.stream().filter(q -> q.storyQuest == story).collect(Collectors.toList()) : new ArrayList<>();
        if (group.isEmpty() && done.isEmpty())
            return;
        TextraLabel title = label("[%80]" + INK_SOFT + Forge.getLocalizer().getMessage(header), root.getWidth() - 3);
        listTable.add(group.isEmpty() ? fold(title, false, 2.5f) : title)
                .width(root.getWidth() - 3).padTop(listTable.getRows() == 0 ? 0 : 6).padLeft(3);
        listTable.row();
        for (AdventureQuestData quest : group) {
            Button row = questRow(quest);
            listTable.add(animate && isNew(quest) ? fold(row, true, 0.8f) : row).expandX().fillX();
            listTable.row();
        }
        for (AdventureQuestData quest : done) {
            listTable.add(fold(questRow(quest), false, 2.5f)).expandX().fillX();
            listTable.row();
        }
    }

    private Button questRow(AdventureQuestData quest) {
        boolean done = finished.contains(quest);
        boolean unseenQuest = !done && isNew(quest);
        Button row = new Button(split && quest == selected ? selectedRowStyle : rowStyle);
        row.setUserObject(quest);
        int stages = quest.stages.length;
        int completed = done ? stages : quest.getCompletedStages().size();
        float progressWidth = 30;
        float nameWidth = root.getWidth() - progressWidth - (split ? 13 : 33);
        row.add(label(TRACKED + (quest.isTracked ? "•" : ""), 8)).width(8).top().padLeft(3);
        TextraLabel name = unseenQuest && animate ? typedName(quest) : Controls.newTextraLabel(INK + quest.getName(), nameFont);
        name.setWrap(true);
        name.setWidth(nameWidth);
        // restores the first line's position and the row height after the shorter line height
        float shrink = Controls.getTextraFont().cellHeight - nameFont.cellHeight;
        Actor nameCell = done ? struckOut(name, DONE + quest.getName(), nameWidth, shrink) : unseenQuest ? growable(newTagged(quest, name, shrink), nameWidth) : name;
        row.add(nameCell).width(nameWidth).left().padLeft(2).padTop(-shrink / 2).padBottom(shrink * 1.5f);
        TextraLabel progress = Controls.newTextraLabel(stages == 0 ? "" : progressText(done ? DONE : INK_SOFT, completed, stages));
        Container<TextraLabel> count = new Container<>(progress).right();
        int fresh = unseenCompleted(quest).size();
        if (animate && !done && fresh > 0) {
            progress.setText(progressText(INK_SOFT, completed - fresh, stages));
            count.setTransform(true);
            count.addAction(Actions.sequence(Actions.delay(1), Actions.run(() -> progress.setText(progressText(DONE, completed, stages))),
                    grow(count, Align.right), Actions.delay(1.1f), Actions.run(() -> progress.setText(progressText(INK_SOFT, completed, stages)))));
        }
        row.add(count).width(progressWidth).top().padTop(2);
        if (!split) {
            Chevron chevron = new Chevron(INK_SOFT_COLOR, true);
            chevron.setVisible(!done);
            row.add(chevron).top().padTop(0.5f).padLeft(7).padRight(3);
        }
        row.pad(3, 0, 3, 0);
        if (done)
            return row;
        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(quest);
            }
        });
        rows.add(row);
        return row;
    }

    private static String progressText(String color, int completed, int stages) {
        return "[%80]" + color + completed + "/" + stages;
    }

    private TypingLabel typedName(AdventureQuestData quest) {
        TypingLabel name = new TypingLabel("{WAIT=1}" + DONE + quest.getName(), Controls.getSkin(), nameFont);
        name.setTypingListener(new TypingAdapter() {
            @Override
            public void end() {
                name.setTypingListener(null);
                Actor box = name.getParent().getParent();
                box.addAction(Actions.sequence(grow(box, Align.left), Actions.run(() -> {
                    name.setText(INK + quest.getName());
                    name.skipToTheEnd();
                    NewTag tag = newTags.get(quest);
                    if (tag != null)
                        tag.setVisible(true);
                })));
            }
        });
        return name;
    }

    private Stack newTagged(AdventureQuestData quest, TextraLabel name, float drop) {
        NewTag tag = new NewTag(name, drop);
        tag.setVisible(!animate);
        newTags.put(quest, tag);
        return new Stack(name, tag);
    }

    private static boolean isNew(AdventureQuestData quest) {
        List<AdventureQuestStage> started = Arrays.stream(quest.stages)
                .filter(stage -> stage.getStatus() != AdventureQuestController.QuestStatus.INACTIVE).collect(Collectors.toList());
        return !started.isEmpty() && started.stream().allMatch(stage -> stage.unseen && stage.getStatus() != AdventureQuestController.QuestStatus.COMPLETE);
    }

    private static List<AdventureQuestStage> unseenCompleted(AdventureQuestData quest) {
        return Arrays.stream(quest.stages)
                .filter(stage -> stage.unseen && stage.getStatus() == AdventureQuestController.QuestStatus.COMPLETE).collect(Collectors.toList());
    }

    private void markSeen(AdventureQuestData quest) {
        for (AdventureQuestStage stage : quest.stages)
            stage.unseen = false;
        NewTag tag = newTags.remove(quest);
        if (tag != null)
            tag.setVisible(false);
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
        completedLink.clearActions();
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
        List<AdventureQuestStage> fresh = unseenCompleted(quest);
        for (AdventureQuestStage stage : fresh) {
            TextraLabel box = Controls.newTextraLabel(INK + "☑ ");
            box.addAction(Actions.delay(1, Actions.run(() -> box.setText(DONE + "☑ "))));
            float nameWidth = width - box.getPrefWidth();
            Table line = new Table();
            line.add(box).top();
            line.add(struckOut(label(INK + stage.name, nameWidth), DONE + stage.name, nameWidth, 0)).top();
            detailTable.add(fold(line, false, 2.5f)).width(width);
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
            int seen = completed.size() - fresh.size();
            setCompletedCount(seen > 0 ? seen : completed.size());
            completedChevron.up = showCompleted;
            completedLink.invalidateHierarchy();
            if (seen > 0 && !fresh.isEmpty())
                completedLink.addAction(Actions.delay(2.8f, Actions.run(() -> setCompletedCount(completed.size()))));
            detailTable.add(seen == 0 && !fresh.isEmpty() ? fold(completedLink, true, 2.8f) : completedLink).padLeft(-2);
            detailTable.row();
        }
        if (showCompleted) {
            for (AdventureQuestStage stage : completed) {
                detailTable.add(label(INK_SOFT + "☑ " + stage.name, width - 12)).width(width - 12).padLeft(12);
                detailTable.row();
            }
        }
        detailScroll.setScrollY(0);
        detailScroll.layout();
        updateTrackButton(quest.isTracked);
        trackButton.setVisible(true);
        abandonButton.setVisible(!quest.storyQuest);
        markSeen(quest);
    }

    private void setCompletedCount(int count) {
        completedLabel.setText(INK_SOFT + "☑ " + Forge.getLocalizer().getMessage("lblCompletedStages", count));
        // setText only measures the text roughly until the label is drawn
        completedLabel.getFont().calculateSize(completedLabel.layout);
    }

    private static Container<Actor> growable(Actor content, float width) {
        Container<Actor> box = new Container<>(content).width(width).left();
        box.setTransform(true);
        return box;
    }

    private static Action grow(Actor actor, int origin) {
        return Actions.sequence(Actions.run(() -> actor.setOrigin(origin)),
                Actions.scaleTo(1.14f, 1.14f, 0.22f, Interpolation.sineOut),
                Actions.scaleTo(1, 1, 0.28f, Interpolation.sineIn));
    }

    private static Container<Actor> struckOut(TextraLabel label, String doneText, float width, float drop) {
        Strike strike = new Strike(label, drop);
        Container<Actor> box = growable(new Stack(label, strike), width);
        box.addAction(Actions.sequence(Actions.delay(1), Actions.run(() -> {
            label.setText(doneText);
            strike.start();
        }), Actions.delay(0.7f), grow(box, Align.left)));
        return box;
    }

    private static Fold fold(Actor content, boolean opening, float delay) {
        Fold fold = new Fold(content, !opening);
        fold.addAction(Actions.delay(delay, Actions.run(() -> fold.setOpen(opening))));
        return fold;
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

    private void confirmAbandon() {
        AdventureQuestData quest = selected;
        showDialog(createGenericDialog("", Forge.getLocalizer().getMessage("lblAbandonQuestConfirm"),
                Forge.getLocalizer().getMessage("lblYes"), Forge.getLocalizer().getMessage("lblNo"), () -> {
                    removeDialog();
                    abandonQuest(quest);
                }, () -> {
                    removeDialog();
                    updateNavigation();
                }));
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
        private final boolean right;
        boolean up;

        Chevron(Color color, boolean right) {
            this.color = color;
            this.right = right;
            setSize(right ? 4 * PX : 7 * PX, right ? 7 * PX : 4 * PX);
            setTouchable(Touchable.disabled);
        }

        float bob() {
            return 0;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float packed = batch.getPackedColor();
            batch.setColor(color.r, color.g, color.b, parentAlpha);
            for (int row = 0; row < 4; row++) {
                if (right)
                    batch.draw(Forge.getAssets().getWhiteTexture(), getX() + row * PX, getY() + row * PX, PX, (7 - row * 2) * PX);
                else
                    batch.draw(Forge.getAssets().getWhiteTexture(), getX() + row * PX, getY() + bob() + (up ? row : 3 - row) * PX, (7 - row * 2) * PX, PX);
            }
            batch.setPackedColor(packed);
        }
    }

    private static class Fold extends Container<Actor> {
        private float open, target;
        private boolean opening;

        Fold(Actor content, boolean open) {
            super(content);
            fillX().top();
            setClip(true);
            this.open = target = open ? 1 : 0;
            getColor().a = this.open;
        }

        void setOpen(boolean open) {
            target = open ? 1 : 0;
            opening = open;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (open == target)
                return;
            open = MathUtils.clamp(open + Math.signum(target - open) * delta / 0.45f, 0, 1);
            getColor().a = open;
            invalidateHierarchy();
            // UIScene scrolls to a selected actor by its own position, which only works for a direct child of the table
            if (opening && open == 1 && getParent() instanceof Table)
                ((Table) getParent()).getCell(this).setActor(getActor());
        }

        @Override
        public float getPrefHeight() {
            return super.getPrefHeight() * open;
        }

        @Override
        public float getMinHeight() {
            return 0;
        }
    }

    private static class NewTag extends WidgetGroup {
        private final TextraLabel name;
        private final float drop;
        private final TypingLabel tag = Controls.newTypingLabel("{WAVE}{STYLE=SHADOW}{COLOR=LIME}[%85]" + Forge.getLocalizer().getMessage("lblNew"));

        NewTag(TextraLabel name, float drop) {
            this.name = name;
            this.drop = drop;
            tag.skipToTheEnd();
            addActor(tag);
            setTouchable(Touchable.disabled);
        }

        @Override
        public void layout() {
            tag.pack();
            float bottom = getHeight() - drop;
            float width = 0;
            for (int i = 0; i < name.layout.lines(); i++) {
                Line line = name.layout.getLine(i);
                bottom -= line.height;
                width = line.width;
            }
            tag.setPosition(width + 4, bottom + 2);
        }
    }

    private static class Strike extends Actor {
        private final TextraLabel label;
        private final float drop;
        private float progress = -1;

        Strike(TextraLabel label, float drop) {
            this.label = label;
            this.drop = drop;
            setTouchable(Touchable.disabled);
        }

        void start() {
            progress = 0;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (progress >= 0)
                progress = Math.min(1, progress + delta / 0.45f);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (progress <= 0)
                return;
            float packed = batch.getPackedColor();
            batch.setColor(DONE_COLOR.r, DONE_COLOR.g, DONE_COLOR.b, parentAlpha);
            float top = getY() + getHeight() - drop;
            for (int i = 0; i < label.layout.lines(); i++) {
                Line line = label.layout.getLine(i);
                top -= line.height;
                batch.draw(Forge.getAssets().getWhiteTexture(), getX(), top + line.height * 0.45f, line.width * progress, 1);
            }
            batch.setPackedColor(packed);
        }
    }

    private static class MoreArrow extends Chevron {
        private final ScrollPane pane;
        private float elapsed;

        MoreArrow(ScrollPane pane) {
            super(new Color(0.12f, 0.11f, 0.09f, 1), false);
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
