package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.archipelago.ArchipelagoColors;
import forge.adventure.archipelago.ArchipelagoTracker;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Paths;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Scene to show the current archipelago progress in Forge
 */
public class ArchipelagoTrackerScene extends UIScene {

    private static final float TILE_SIZE = 28;
    private static final String BUILDINGS_ATLAS = "maps/tileset/buildings.atlas";
    private static final String DONE_COLOR = ArchipelagoColors.Green;
    private static final String HEADER_COLOR = ArchipelagoColors.Salmon;
    private static final String DETAILS_HINT = "Tap an icon to see its details.";
    private static final String[][] KEYS = {{"White Key", "WhiteKey"}, {"Blue Key", "BlueKey"}, {"Black Key", "BlackKey"},
            {"Red Key", "RedKey"}, {"Green Key", "GreenKey"}, {"Strange Key", "StrangeKey"}};

    TextraButton backButton;
    private final ScrollPane contentPane;
    private final TextraLabel detailsLabel;
    private final Table content = new Table();
    private final Table setList = new Table();
    private final Map<String, TextureRegion> portraits = new HashMap<>();
    private TextureRegion checkIcon;
    private TextField setSearch;
    private List<String[]> unlockedSets = Collections.emptyList();
    // Scroll position when the tracker was last left, restored when it's opened again in this session (not saved).
    private static float lastScrollY = 0;

    private ArchipelagoTrackerScene() {
        super(Forge.isLandscapeMode() ? "ui/aptracker.json" : "ui/aptracker_portrait.json");

        backButton = ui.findActor("return");
        ui.onButtonPress("return", ArchipelagoTrackerScene.this::back);

        detailsLabel = ui.findActor("details");
        detailsLabel.setWrap(true);
        detailsLabel.setAlignment(Align.topLeft);

        content.top().left().pad(4);
        contentPane = ui.findActor("content");
        // Give the details text the same dark panel as the content so it stays readable over the background art.
        Table detailsPanel = new Table();
        detailsPanel.setBackground(contentPane.getStyle().background);
        detailsPanel.setBounds(detailsLabel.getX(), detailsLabel.getY(), detailsLabel.getWidth(), detailsLabel.getHeight());
        ui.addActorBefore(detailsLabel, detailsPanel);
        detailsLabel.setBounds(detailsLabel.getX() + 6, detailsLabel.getY() + 3, detailsLabel.getWidth() - 12, detailsLabel.getHeight() - 6);
        contentPane.setActor(content);
        contentPane.setScrollingDisabled(true, false);
        contentPane.setOverscroll(false, false);
        contentPane.setFadeScrollBars(false);
        addToSelectable(backButton);
    }

    @Override
    public void enter() {
        super.enter();
        build();
        // Lay out the rebuilt content first so the old position can be scrolled to, then jump there without animating.
        contentPane.layout();
        contentPane.setScrollY(lastScrollY);
        contentPane.updateVisualScroll();
    }

    @Override
    public boolean leave() {
        lastScrollY = contentPane.getScrollY();
        return super.leave();
    }

    public boolean back() {
        Forge.switchToLast();
        return true;
    }

    // Rebuilds the whole screen from the current save data.
    private void build() {
        content.clear();
        detailsLabel.setText(DETAILS_HINT);
        contentPane.setScrollY(0);

        if (!ArchipelagoTracker.isAvailable()) {
            addRow(label("Connect to an Archipelago server once to see your progress here."));
            return;
        }

        ArchipelagoTracker.Progress goal = ArchipelagoTracker.getGoalProgress();
        addRow(label(String.format("%sGoal:{RESET} %s %s", HEADER_COLOR, goal.label, fraction(goal.done, goal.target))));
        addRow(label(String.format("%sCastle keys:{RESET} %s", HEADER_COLOR, keyIcons())));

        for (ArchipelagoTracker.Region region : ArchipelagoTracker.getRegions()) {
            addRow(label(region.color + region.name + "{RESET}")).padTop(6);
            Table tiles = new Table();
            tiles.left();
            for (ArchipelagoTracker.Entry entry : region.entries) {
                tiles.add(tile(entry)).size(TILE_SIZE).pad(1);
            }
            ScrollPane row = new ScrollPane(tiles);
            row.setScrollingDisabled(false, true);
            row.setOverscroll(false, false);
            addRow(row).height(TILE_SIZE + 4);
        }

        addRow(label(HEADER_COLOR + "Cards collected{RESET}")).padTop(8);
        for (ArchipelagoTracker.Progress cards : ArchipelagoTracker.getCardProgress()) {
            addRow(label(String.format("%s: %s", cards.label, fraction(cards.done, cards.target))));
        }

        unlockedSets = ArchipelagoTracker.getUnlockedSets();
        addRow(label(String.format("%sUnlocked sets{RESET} %s", HEADER_COLOR,
                fraction(unlockedSets.size(), ArchipelagoTracker.getTotalSetCount())))).padTop(8);
        setSearch = Controls.newTextField("", text -> {
            refreshSetList();
            return null;
        });
        setSearch.setMessageText("Search by set name or code");
        addRow(setSearch).height(24);
        ScrollPane setPane = new ScrollPane(setList);
        setPane.setScrollingDisabled(true, false);
        setPane.setOverscroll(false, false);
        addRow(setPane).height(Forge.isLandscapeMode() ? 90 : 150).padBottom(4);
        refreshSetList();
    }

    private void refreshSetList() {
        setList.clear();
        setList.top().left();
        String filter = setSearch == null ? "" : setSearch.getText().trim().toLowerCase();
        for (String[] set : unlockedSets) {
            if (filter.isEmpty() || set[0].toLowerCase().contains(filter) || set[1].toLowerCase().contains(filter)) {
                TextraLabel row = label(String.format("%s%s{RESET}  %s", ArchipelagoColors.Cyan, set[0], set[1]));
                setList.add(row).growX().left().pad(1);
                setList.row();
            }
        }
        if (!setList.hasChildren()) {
            setList.add(label(unlockedSets.isEmpty() ? "No sets unlocked yet." : "No sets match your search.")).growX().left();
        }
    }

    // A region tile: greyed out until done, with a green check in the bottom right corner once done.
    private Actor tile(ArchipelagoTracker.Entry entry) {
        Stack stack = new Stack();
        TextureRegion region = icon(entry);
        Image image = region != null ? new Image(region) : new Image();
        image.setScaling(Scaling.fit);
        if (!entry.done) image.setColor(0.35f, 0.35f, 0.35f, 0.9f);
        stack.add(image);

        TextureRegion check = checkIcon();
        if (entry.done && check != null) {
            Image checkImage = new Image(check);
            checkImage.setScaling(Scaling.fit);
            checkImage.setColor(Color.GREEN);
            Table overlay = new Table();
            overlay.add(checkImage).size(TILE_SIZE * 0.45f).expand().bottom().right();
            stack.add(overlay);
        }
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                detailsLabel.setText(toTextraMarkup(entry.done ? DONE_COLOR + entry.description + " (done){RESET}" : entry.description));
            }
        });
        return stack;
    }

    private TextureRegion icon(ArchipelagoTracker.Entry entry) {
        switch (entry.icon) {
            case BOSS:
                return portrait(entry.enemy);
            case EQUIPMENT_SHOP: // the shop signs used on the map
                return Config.instance().getAtlas(BUILDINGS_ATLAS).findRegion("EquipmentShop");
            case ITEM_SHOP:
                return Config.instance().getAtlas(BUILDINGS_ATLAS).findRegion("ItemShop");
            case TOWN_QUESTS:
                return Config.instance().getItemSprite("LogBook");
            case INN_EVENTS:
                return Config.instance().getAtlas(Paths.MAP_MARKER).findRegion("town");
            case BATTLES:
                return Config.instance().getItemSprite("BattleStandard");
            default:
                return null;
        }
    }

    private TextureRegion portrait(String enemyName) {
        if (portraits.containsKey(enemyName)) return portraits.get(enemyName);
        TextureRegion avatar = null;
        EnemyData data = WorldData.getEnemy(enemyName);
        if (data != null) avatar = new EnemySprite(data).getAvatar();
        if (avatar == null) avatar = Config.instance().getAtlas(Paths.MAP_MARKER).findRegion("sidebossmoderate");
        portraits.put(enemyName, avatar);
        return avatar;
    }

    private TextureRegion checkIcon() {
        if (checkIcon == null && Controls.getSkin().has("check", TextureRegion.class)) {
            checkIcon = Controls.getSkin().getRegion("check");
        }
        return checkIcon;
    }

    private String keyIcons() {
        StringBuilder keys = new StringBuilder();
        for (String[] key : KEYS) {
            keys.append(AdventurePlayer.current().hasItem(key[0]) ? "[+" + key[1] + "]" : "[+Dot]").append(' ');
        }
        return keys.toString().trim();
    }

    private static String fraction(long done, long target) {
        String text = done + "/" + target;
        return done >= target && target > 0 ? DONE_COLOR + text + "{RESET}" : text;
    }

    private TextraLabel label(String text) {
        TextraLabel label = Controls.newTextraLabel(toTextraMarkup(text));
        label.setWrap(true);
        return label;
    }

    // ArchipelagoColors uses TypingLabel tokens ({COLOR=#hex} ... {RESET}); TextraLabel uses [#hex] ... [] instead.
    // Both braces are escaped: Android's regex engine rejects an unescaped '}'.
    private static final Pattern COLOR_TOKEN = Pattern.compile("\\{COLOR=(#[0-9A-Fa-f]{6})\\}");

    private static String toTextraMarkup(String text) {
        return COLOR_TOKEN.matcher(text).replaceAll("[$1]").replace("{RESET}", "[]");
    }

    private Cell<Actor> addRow(Actor actor) {
        content.row();
        return content.add(actor).growX().left();
    }

    private static ArchipelagoTrackerScene object;

    public static ArchipelagoTrackerScene instance() {
        if (object == null)
            object = new ArchipelagoTrackerScene();
        return object;
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

}
