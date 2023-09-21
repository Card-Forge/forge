package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingAdapter;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.character.*;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.*;
import forge.adventure.util.*;
import forge.adventure.util.pathfinding.NavigationMap;
import forge.adventure.util.pathfinding.NavigationVertex;
import forge.adventure.util.pathfinding.ProgressableGraphPath;
import forge.adventure.world.WorldSave;
import forge.assets.FBufferedImage;
import forge.assets.FImageComplex;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gui.FThreads;
import forge.screens.TransitionScreen;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

import java.time.LocalDate;
import java.util.*;
import java.util.Queue;


/**
 * Stage to handle tiled maps for points of interests
 */
public class MapStage extends GameStage {
    public static MapStage instance;
    final Array<MapActor> actors = new Array<>();
    public com.badlogic.gdx.physics.box2d.World gdxWorld;
    public TiledMap tiledMap;
    public Array<Rectangle> collisionRect = new Array<>();
    public Map<Float, NavigationMap> navMaps = new HashMap<>();
    private boolean isInMap = false;
    MapLayer spriteLayer;
    private PointOfInterestChanges changes;
    private EnemySprite currentMob;
    Queue<Vector2> positions = new LinkedList<>();
    private boolean isLoadingMatch = false;
    //private HashMap<String, Byte> mapFlags = new HashMap<>(); //Stores local map flags. These aren't available outside this map.

    private final Dialog dialog;
    private Stage dialogStage;
    private boolean dialogOnlyInput;

    //Map properties.
    //These maps are defined as embedded properties within the Tiled maps.
    private EffectData effect;             //"Dungeon Effect": Character Effect applied to all adversaries within the map.
    private boolean preventEscape = false; //Prevents player from escaping the dungeon by any means that aren't an exit.
    private final Array<TextraButton> dialogButtonMap = new Array<>();

    public InputEvent eventTouchDown, eventTouchUp;
    TextraButton selectedKey;
    private boolean respawnEnemies;
    private boolean canFailDungeon = false;
    protected ArrayList<EnemySprite> enemies = new ArrayList<>();
    public Map<Integer, Vector2> waypoints = new HashMap<>();

    //todo: add additional graphs for other sprite sizes if desired. Current implementation
    // allows for mobs of any size to fit into 16x16 tiles for navigation purposes
    float collisionWidthMod = 0.4f;
    float defaultSpriteSize = 16f;
    float navMapSize =  defaultSpriteSize * collisionWidthMod;

    public boolean getDialogOnlyInput() {
        return dialogOnlyInput;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public boolean canEscape() {
        return !preventEscape;
    } //Check if escape is possible.

    public void clearIsInMap() {
        isInMap = false;
        effect = null; //Reset effect so battles outside the dungeon don't use the last visited dungeon's effects.
        preventEscape = false;
        GameHUD.getInstance().showHideMap(true);
    }

    public void draw(Batch batch) {
        //Camera camera = getCamera() ;
        //camera.update();
        //update camera after all layers got drawn
        if (!getRoot().isVisible()) return;
        getRoot().draw(batch, 1);
    }

    public MapLayer getSpriteLayer() {
        return spriteLayer;
    }

    public PointOfInterestChanges getChanges() {
        return changes;
    }
    private boolean freezeAllEnemyBehaviors = false;

    protected MapStage() {
        disposeWorld();
        gdxWorld = new World(new Vector2(0, 0),false);
        dialog = Controls.newDialog("");
        eventTouchDown = new InputEvent();
        eventTouchDown.setPointer(-1);
        eventTouchDown.setType(InputEvent.Type.touchDown);
        eventTouchUp = new InputEvent();
        eventTouchUp.setPointer(-1);
        eventTouchUp.setType(InputEvent.Type.touchUp);
    }

    public static MapStage getInstance() {
        return instance == null ? instance = new MapStage() : instance;
    }

    public void disposeWorld() {
        if (gdxWorld != null) {
            try {
                gdxWorld.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addMapActor(MapObject obj, MapActor newActor) {
        newActor.setWidth(Float.parseFloat(obj.getProperties().get("width").toString()));
        newActor.setHeight(Float.parseFloat(obj.getProperties().get("height").toString()));
        newActor.setX(Float.parseFloat(obj.getProperties().get("x").toString()));
        newActor.setY(Float.parseFloat(obj.getProperties().get("y").toString()));
        actors.add(newActor);
        foregroundSprites.addActor(newActor);
    }

    public void addMapActor(MapActor newActor) {
        actors.add(newActor);
        foregroundSprites.addActor(newActor);
    }

    @Override
    public boolean isColliding(Rectangle adjustedBoundingRect) {
        for (Rectangle collision : collisionRect) {
            if (collision.overlaps(adjustedBoundingRect)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepareCollision(Vector2 pos, Vector2 direction, Rectangle boundingRect) {

    }


    Group collisionGroup;

    @Override
    public void debugCollision(boolean b) {

        if (collisionGroup == null) {
            collisionGroup = new Group();

            for (Rectangle rectangle : collisionRect) {
                MapActor collisionActor = new MapActor(0);
                collisionActor.setBoundDebug(true);
                collisionActor.setWidth(rectangle.width);
                collisionActor.setHeight(rectangle.height);
                collisionActor.setX(rectangle.x);
                collisionActor.setY(rectangle.y);
                collisionGroup.addActor(collisionActor);
            }

        }
        if (b) {
            addActor(collisionGroup);
        } else {
            collisionGroup.remove();
        }
        super.debugCollision(b);
    }

    private void effectDialog(EffectData effectData) {
        dialog.getButtonTable().clear();
        dialog.getContentTable().clear();
        dialog.clearListeners();
        TextraButton ok = Controls.newTextButton("OK", this::hideDialog);
        ok.setVisible(false);
        TypingLabel L = Controls.newTypingLabel("{GRADIENT=CYAN;WHITE;1;1}Strange magical energies flow within this place...{ENDGRADIENT}\nAll opponents get:\n" + effectData.getDescription());
        L.setWrap(true);
        L.setTypingListener(new TypingAdapter() {
            @Override
            public void end() {
                ok.setVisible(true);
            }
        });
        dialog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                L.skipToTheEnd();
                super.clicked(event, x, y);
            }
        });
        dialog.getButtonTable().add(ok).width(240f);
        dialog.getContentTable().add(L).width(250f);
        dialog.setKeepWithinStage(true);
        showDialog();
    }

    public void showImageDialog(String message, FBufferedImage fb) {
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        dialog.clearListeners();

        if (fb.getTexture() != null) {
            TextureRegion tr = new TextureRegion(fb.getTexture());
            tr.flip(true, true);
            Image image = new Image(tr);
            image.setScaling(Scaling.fit);
            dialog.getContentTable().add(image).height(100);
            dialog.getContentTable().add().row();
        }
        TypingLabel L = Controls.newTypingLabel(message);
        L.setWrap(true);
        L.skipToTheEnd();
        dialog.getContentTable().add(L).width(250f);
        dialog.getButtonTable().add(Controls.newTextButton("OK", () -> {
            hideDialog();
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    fb.dispose();
                }
            }, 0.5f);
        })).width(240f);
        dialog.setKeepWithinStage(true);
        setDialogStage(GameHUD.getInstance());
        showDialog();
    }

    public void showDeckAwardDialog(String message, Deck deck) {
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        dialog.clearListeners();
        DeckProxy dp = new DeckProxy(deck, "Constructed", GameType.Constructed, null);
        FImageComplex cardArt = CardRenderer.getCardArt(dp.getHighestCMCCard());
        if (cardArt != null) {
            Image art = new Image(cardArt.getTextureRegion());
            art.setWidth(58);
            art.setHeight(46);
            art.setPosition(25, 43);
            Image image = new Image(FSkinImage.ADV_DECKBOX.getTextureRegion());
            image.setWidth(60);
            image.setHeight(80);
            image.setPosition(24, 10);
            ColorSet colorSet = DeckProxy.getColorIdentity(deck);
            TypingLabel deckColors = Controls.newTypingLabel(Controls.colorIdToTypingString(colorSet, true).toUpperCase());
            deckColors.skipToTheEnd();
            deckColors.setAlignment(Align.center);
            deckColors.setPosition(14, 44);
            TextraLabel deckname = Controls.newTextraLabel(deck.getName());
            deckname.setAlignment(Align.center);
            deckname.setWrap(true);
            deckname.setWidth(80);
            deckname.setPosition(14, 28);
            Group group = new Group();
            group.addActor(art);
            group.addActor(image);
            group.addActor(deckColors);
            group.addActor(deckname);
            dialog.getContentTable().add(group).height(100).width(100).center();
            dialog.getContentTable().add().row();
        } else {
            TypingLabel label = Controls.newTypingLabel("[%120]" + Controls.colorIdToTypingString(DeckProxy.getColorIdentity(deck)).toUpperCase() + "\n[%]" + deck.getName());
            label.skipToTheEnd();
            label.setAlignment(Align.center);
            dialog.getContentTable().add(label).align(Align.center);
            dialog.getContentTable().add().row();
        }

        TypingLabel L = Controls.newTypingLabel(message);
        L.setWrap(true);
        L.skipToTheEnd();

        dialog.getContentTable().add(L).width(250);
        dialog.getButtonTable().add(Controls.newTextButton("OK", this::hideDialog)).width(240);
        dialog.setKeepWithinStage(true);
        setDialogStage(GameHUD.getInstance());
        showDialog();
    }

    Array<EntryActor> otherEntries = new Array<>();
    Array<EntryActor> spawnClassified = new Array<>();
    Array<EntryActor> sourceMapMatch = new Array<>();

    public void loadMap(TiledMap map, String sourceMap, String targetMap) {
        loadMap(map, sourceMap, targetMap, 0);
    }

    public void loadMap(TiledMap map, String sourceMap, String targetMap, int spawnTargetId) {
        disposeWorld();
        gdxWorld = new World(new Vector2(0, 0),false);
        isLoadingMatch = false;
        isInMap = true;
        GameHUD.getInstance().showHideMap(false);
        this.tiledMap = map;
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            actor.remove();
            foregroundSprites.removeActor(actor);
        }
        positions.clear();
        actors.clear();
        collisionRect.clear();
        waypoints.clear();

        if (collisionGroup != null)
            collisionGroup.remove();
        collisionGroup = null;

        float width = Float.parseFloat(map.getProperties().get("width").toString());
        float height = Float.parseFloat(map.getProperties().get("height").toString());
        float tileHeight = Float.parseFloat(map.getProperties().get("tileheight").toString());
        float tileWidth = Float.parseFloat(map.getProperties().get("tilewidth").toString());
        setBounds(width * tileWidth, height * tileHeight);
        //collision = new Array[(int) width][(int) height];

        //Load dungeon effects.
        MapProperties MP = map.getProperties();

        if (MP.get("dungeonEffect") != null && !MP.get("dungeonEffect").toString().isEmpty()) {
            effect = JSONStringLoader.parse(EffectData.class, map.getProperties().get("dungeonEffect").toString(), "");
            effectDialog(effect);
        }
        if (MP.get("respawnEnemies") != null && MP.get("respawnEnemies") instanceof Boolean && (Boolean) MP.get("respawnEnemies")) {
            respawnEnemies = true;
        } else {
            respawnEnemies = false;
        }
        if (MP.get("canFailDungeon") != null && MP.get("canFailDungeon") instanceof Boolean && (Boolean) MP.get("canFailDungeon")) {
            canFailDungeon = true;
        } else {
            canFailDungeon = false;
        }
        if (MP.get("preventEscape") != null) preventEscape = (boolean) MP.get("preventEscape");

        if (MP.get("music") != null && !MP.get("music").toString().isEmpty()) {
            //TODO: Add a way to play a music file directly without using a playlist.
        }

        getPlayerSprite().stop();
        spriteLayer = null;
        otherEntries.clear();
        spawnClassified.clear();
        sourceMapMatch.clear();
        enemies.clear();
        for (MapLayer layer : map.getLayers()) {
            if (layer.getProperties().containsKey("spriteLayer") && layer.getProperties().get("spriteLayer", boolean.class)) {
                spriteLayer = layer;
            }
            if (layer instanceof TiledMapTileLayer) {
                loadCollision((TiledMapTileLayer) layer);
            } else {
                loadObjects(layer, sourceMap, targetMap);
            }
        }
        spawn(spawnTargetId);

        //reduce geometry in collision rectangles
        int oldSize;
        do {
            oldSize = collisionRect.size;
            for (int i = 0; i < collisionRect.size; i++) {
                Rectangle r1 = collisionRect.get(i);
                for (int j = i + 1; j < collisionRect.size; j++) {
                    Rectangle r2 = collisionRect.get(j);
                    if ((Math.abs(r1.x - (r2.x + r2.width)) < 1 && Math.abs(r1.y - r2.y) < 1 && Math.abs(r1.height - r2.height) < 1)//left edge is the same as right edge

                            || (Math.abs((r1.x + r1.width) - r2.x) < 1 && Math.abs(r1.y - r2.y) < 1 && Math.abs(r1.height - r2.height) < 1)//right edge is the same as left edge

                            || (Math.abs(r1.x - r2.x) < 1 && Math.abs((r1.y + r1.height) - r2.y) < 1 && Math.abs(r1.width - r2.width) < 1)//top edge is the same as bottom edge

                            || (Math.abs(r1.x - r2.x) < 1 && Math.abs(r1.y - (r2.y + r2.height)) < 1 && Math.abs(r1.width - r2.width) < 1)//bottom edge is the same as left edge

                            || containsOrEquals(r1, r2) || containsOrEquals(r2, r1)
                    ) {
                        r1.merge(r2);
                        collisionRect.removeIndex(j);
                        i--;
                        break;
                    }
                }
            }
        } while (oldSize != collisionRect.size);
        if (spriteLayer == null) System.err.print("Warning: No spriteLayer present in map.\n");

        navMaps.clear();
        navMaps.put(navMapSize, new NavigationMap(navMapSize));
        navMaps.get(navMapSize).initializeGeometryGraph();
        getPlayerSprite().stop();
    }

    public void spawn(int targetId){
        stop(); //Prevent player from unintentionally going back through entrance again when holding input
        boolean hasSpawned = false;
        if (targetId > 0){
            for (int i = 0; i < actors.size; i++) {
                if (actors.get(i).getObjectId() == targetId) {
                    if (actors.get(i) instanceof EntryActor) {
                        ((EntryActor)(actors.get(i))).spawn();
                        hasSpawned = true;
                    }
                }
            }
        }
        if (!hasSpawned){
            if (!spawnClassified.isEmpty())
                spawnClassified.first().spawn();
            else if (!sourceMapMatch.isEmpty())
                sourceMapMatch.first().spawn();
            else if (!otherEntries.isEmpty())
                otherEntries.first().spawn();
        }
    }

    static public boolean containsOrEquals(Rectangle r1, Rectangle r2) {
        float xmi = r2.x;
        float xma = xmi + r2.width;
        float ymi = r2.y;
        float yma = ymi + r2.height;
        return xmi >= r1.x && xmi <= r1.x + r1.width && xma >= r1.x && xma <= r1.x + r1.width && ymi >= r1.y && ymi <= r1.y + r1.height && yma >= r1.y && yma <= r1.y + r1.height;
    }

    private void loadCollision(TiledMapTileLayer layer) {
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null)
                    continue;
                for (MapObject collision : cell.getTile().getObjects()) {
                    if (collision instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) collision).getRectangle();
                        collisionRect.add(new Rectangle(((layer.getTileWidth() * x) + r.x), ((layer.getTileHeight() * y) + r.y), Math.round(r.width), Math.round(r.height)));
                    }
                }
            }
        }
    }

    private boolean canSpawn(MapProperties prop) {
        DifficultyData difficultyData = Current.player().getDifficulty();
        boolean spawnEasy = prop.get("spawn.Easy", Boolean.class);
        boolean spawnNorm = prop.get("spawn.Normal", Boolean.class);
        boolean spawnHard = prop.get("spawn.Hard", Boolean.class);
        if (difficultyData.spawnRank == 2 && !spawnHard) return false;
        if (difficultyData.spawnRank == 1 && !spawnNorm) return false;
        if (difficultyData.spawnRank == 0 && !spawnEasy) return false;

        if (prop.containsKey("spawnCondition") && !prop.get("spawnCondition").toString().isEmpty()){

        }

        return true;
    }

    private void loadObjects(MapLayer layer, String sourceMap, String currentMap) {
        player.setMoveModifier(2);
        Array<String> shopsAlreadyPresent = new Array<>();
        for (MapObject obj : layer.getObjects()) {
            MapProperties prop = obj.getProperties();
            String type = prop.get("type", String.class);
            if (type != null) {
                int id = prop.get("id", int.class);
                if (changes.isObjectDeleted(id))
                    continue;

                boolean hidden = !obj.isVisible(); //Check if the object is invisible.

                String rotatingShop = "";

                switch (type) {
                    case "collision":
                        float cX = Float.parseFloat(prop.get("x").toString());
                        float cY = Float.parseFloat(prop.get("y").toString());
                        float cW = Float.parseFloat(prop.get("width").toString());
                        float cH = Float.parseFloat(prop.get("height").toString());
                        collisionRect.add(new Rectangle(cX, cY, cW, cH));
                        break;
                    case "waypoint":
                        waypoints.put(id, new Vector2(Float.parseFloat(prop.get("x").toString()), Float.parseFloat(prop.get("y").toString())));
                        break;
                    case "entry":
                        float x = Float.parseFloat(prop.get("x").toString());
                        float y = Float.parseFloat(prop.get("y").toString());
                        float w = Float.parseFloat(prop.get("width").toString());
                        float h = Float.parseFloat(prop.get("height").toString());

                        String targetMap = prop.containsKey("teleport")?prop.get("teleport").toString():"";
                        String direction = prop.containsKey("direction")?prop.get("direction").toString():"";
                        boolean canStillSpawnPlayerThere = (targetMap == null || targetMap.isEmpty() && sourceMap.isEmpty()) ||//if target is null and "from world"
                                !sourceMap.isEmpty() && targetMap.equals(sourceMap);

                        int entryTargetId = (!prop.containsKey("teleportObjectId") || prop.get("teleportObjectId") ==null || prop.get("teleportObjectId").toString().isEmpty())? 0: Integer.parseInt(prop.get("teleportObjectId").toString());

                        EntryActor entry = new EntryActor(this, id, targetMap, x, y, w, h, direction, currentMap, entryTargetId);
                        if (prop.containsKey("spawn") && prop.get("spawn").toString().equals("true")) {
                            spawnClassified.add(entry);
                        } else if (canStillSpawnPlayerThere) {
                            sourceMapMatch.add(entry);
                        } else {
                            otherEntries.add(entry);
                        }
                        if (!prop.containsKey("noExit") || prop.get("noExit").toString().equals("false"))
                            addMapActor(obj, entry);
                        break;
                    case "portal":
                        float px = Float.parseFloat(prop.get("x").toString());
                        float py = Float.parseFloat(prop.get("y").toString());
                        float pw = Float.parseFloat(prop.get("width").toString());
                        float ph = Float.parseFloat(prop.get("height").toString());

                        Object portalSpriteProvided = prop.get("sprite");
                        String portalSpriteToUse;
                        portalSpriteToUse = "sprites/portal.atlas";
                        if (portalSpriteProvided != null && !portalSpriteProvided.toString().isEmpty()) portalSpriteToUse = portalSpriteProvided.toString();
                        else
                            System.err.printf("No sprite defined for portal (ID:%s), defaulting to \"sprites/portal.atlas\"", id);

                        String portalTargetMap = prop.get("teleport").toString();
                        boolean validSpawnPoint = (portalTargetMap == null || portalTargetMap.isEmpty() && sourceMap.isEmpty()) ||//if target is null and "from world"
                                !sourceMap.isEmpty() && portalTargetMap.equals(sourceMap);

                        int portalTargetId = (!prop.containsKey("teleportObjectId") || prop.get("teleportObjectId") ==null || prop.get("teleportObjectId").toString().isEmpty())? 0: Integer.parseInt(prop.get("teleportObjectId").toString());

                        PortalActor portal = new PortalActor(this, id, prop.get("teleport").toString(), px, py, pw, ph, prop.get("direction").toString(), currentMap, portalTargetId, portalSpriteToUse);

                        if (prop.containsKey("activeQuestFlag") && Current.player().checkQuestFlag(prop.get("activeQuestFlag").toString())){
                            portal.setAnimation("active");
                        }
                        else if (prop.containsKey("inactiveQuestFlag") && Current.player().checkQuestFlag(prop.get("inactiveQuestFlag").toString())){
                            portal.setAnimation("inactive");
                        }
                        else if (prop.containsKey("closedQuestFlag") && Current.player().checkQuestFlag(prop.get("closedQuestFlag").toString())){
                            portal.setAnimation("closed");
                        }
                        else if (prop.containsKey("portalState")) {
                            portal.setAnimation(prop.get("portalState").toString());
                        }
                        if (prop.containsKey("spawn") && prop.get("spawn").toString().equals("true")) {
                            spawnClassified.add(portal);
                        } else if (validSpawnPoint) {
                            sourceMapMatch.add(portal);
                        } else {
                            otherEntries.add(portal);
                        }
                        addMapActor(obj, portal);
                        break;
                    case "reward":
                        if (!canSpawn(prop)) break;
                        Object R = prop.get("reward");
                        if (R != null && !R.toString().isEmpty()) {
                            Object S = prop.get("sprite");
                            String Sp;
                            Sp = "sprites/treasure.atlas";
                            if (S != null && !S.toString().isEmpty()) Sp = S.toString();
                            else
                                System.err.printf("No sprite defined for reward (ID:%s), defaulting to \"sprites/treasure.atlas\"", id);
                            RewardSprite RW = new RewardSprite(id, R.toString(), Sp);
                            RW.hidden = hidden;
                            addMapActor(obj, RW);
                        }
                        break;
                    case "enemy":
                        if (!canSpawn(prop)) break;
                        Object enemy = prop.get("enemy");
                        if (enemy != null && !enemy.toString().isEmpty()) {
                            EnemyData EN = WorldData.getEnemy(enemy.toString());
                            if (EN == null) {
                                System.err.printf("Enemy \"%s\" not found.", enemy);
                                break;
                            }
                            EnemySprite mob = new EnemySprite(id, EN);
                            Object dialogObject = prop.get("dialog"); //Check if the enemy has a dialogue attached to it.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.dialog = new MapDialog(dialogObject.toString(), this, mob.getId());
                            }
                            dialogObject = prop.get("defeatDialog"); //Check if the enemy has a defeat dialogue attached to it.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.defeatDialog = new MapDialog(dialogObject.toString(), this, mob.getId());
                            }
                            dialogObject = prop.get("displayNameOverride"); //Check for name override.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.nameOverride = dialogObject.toString();
                            }
                            dialogObject = prop.get("effect"); //Check for special effects.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.effect = JSONStringLoader.parse(EffectData.class, dialogObject.toString(), "");
                            }
                            dialogObject = prop.get("ignoreDungeonEffect"); //Check for special effects.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.ignoreDungeonEffect = Boolean.parseBoolean(dialogObject.toString());
                            }
                            dialogObject = prop.get("reward"); //Check for additional rewards.
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.rewards = JSONStringLoader.parse(RewardData[].class, dialogObject.toString(), "[]");
                            }
                            if (prop.containsKey("threatRange")) //Check for threat range.
                            {
                                mob.threatRange = Float.parseFloat(prop.get("threatRange").toString());
                            }
                            if (prop.containsKey("threatRange")) //Check for threat range.
                            {
                                mob.pursueRange = Float.parseFloat(prop.get("pursueRange").toString());
                            }
                            if (prop.containsKey("fleeRange")) //Check for flee range.
                            {
                                mob.fleeRange = Float.parseFloat(prop.get("fleeRange").toString());
                            }
                            if (prop.containsKey("speed")) //Check for flee range.
                            {
                                mob.getData().speed = Float.parseFloat(prop.get("speed").toString());
                            }
                            if (prop.containsKey("flying"))
                            {
                                mob.getData().flying = Boolean.parseBoolean(prop.get("flying").toString());
                            }
                            if (prop.containsKey("hidden"))
                            {
                                hidden = Boolean.parseBoolean(prop.get("hidden").toString());
                            }
                            if (prop.containsKey("inactive"))
                            {
                                mob.inactive = Boolean.parseBoolean(prop.get("inactive").toString());
                                if (mob.inactive) mob.clearCollisionHeight();
                            }
                            dialogObject = prop.get("deckOverride");
                            if (dialogObject != null && !dialogObject.toString().isEmpty())
                            {
                                mob.overrideDeck(dialogObject.toString());
                            }
                            if (hidden){
                                mob.hidden = hidden; //Evil.
                                mob.setAnimation(CharacterSprite.AnimationTypes.Hidden);
                            }
                            dialogObject = prop.get("waypoints");
                            if (dialogObject != null && !dialogObject.toString().isEmpty()) {
                                mob.parseWaypoints(dialogObject.toString());
                            }
                            if (prop.containsKey("speedModifier")) //Increase or decrease default speed for this mob
                            {
                                mob.speedModifier = Float.parseFloat(prop.get("speedModifier").toString());
                            }

                            enemies.add(mob);
                            addMapActor(obj, mob);
                        }
                        break;
                    case "dummy": //Does nothing. Mostly obstacles to be removed by ID by switches or such.
                        TiledMapTileMapObject obj2 = (TiledMapTileMapObject) obj;
                        DummySprite D = new DummySprite(id, obj2.getTextureRegion(), this);
                        if (prop.containsKey("hidden")){
                            D.setVisible(!Boolean.parseBoolean(prop.get("hidden").toString()));
                        }
                        addMapActor(obj, D);
                        //TODO: Ability to toggle their solid state.
                        //TODO: Ability to move them (using a sequence such as "UULU" for up, up, left, up).
                        break;
                    case "inn":
                        addMapActor(obj, new OnCollide(() -> Forge.switchScene(InnScene.instance(TileMapScene.instance(), TileMapScene.instance().rootPoint.getID(), changes, id))));
                        break;
                    case "spellsmith":
                        addMapActor(obj, new OnCollide(() -> Forge.switchScene(SpellSmithScene.instance())));
                        break;
                    case "shardtrader":
                        MapActor shardTraderActor = new OnCollide(() -> Forge.switchScene(ShardTraderScene.instance()));
                        addMapActor(obj, shardTraderActor);
                        if (prop.containsKey("hasSign") && Boolean.parseBoolean(prop.get("hasSign").toString()) && prop.containsKey("signYOffset") && prop.containsKey("signXOffset")) {
                            try {
                                TextureSprite sprite = new TextureSprite(Config.instance().getAtlasSprite(ShardTraderScene.spriteAtlas, ShardTraderScene.sprite));
                                sprite.setX(shardTraderActor.getX() + Float.parseFloat(prop.get("signXOffset").toString()));
                                sprite.setY(shardTraderActor.getY() + Float.parseFloat(prop.get("signYOffset").toString()));
                                addMapActor(sprite);

                            } catch (Exception e) {
                                System.err.print("Can not create Texture for Shard Trader");
                            }
                        }
                        break;
                    case "arena":
                        addMapActor(obj, new OnCollide(() -> {
                            ArenaData arenaData = JSONStringLoader.parse(ArenaData.class, prop.get("arena").toString(), "");
                            ArenaScene.instance().loadArenaData(arenaData, WorldSave.getCurrentSave().getWorld().getRandom().nextLong());
                            Forge.switchScene(ArenaScene.instance());
                        }));
                        break;
                    case "exit":
                        addMapActor(obj, new OnCollide(MapStage.this::exitDungeon));
                        break;
                    case "dialog":
                        if (obj instanceof TiledMapTileMapObject) {
                            TiledMapTileMapObject tiledObj = (TiledMapTileMapObject) obj;
                            DialogActor dialog;
                            if (prop.containsKey("sprite"))
                                dialog = new DialogActor(this, id, prop.get("dialog").toString(), prop.get("sprite").toString());
                            else {
                                dialog = new DialogActor(this, id, prop.get("dialog").toString(), tiledObj.getTextureRegion());
                            }
                            if (prop.containsKey("hidden") && Boolean.parseBoolean(prop.get("hidden").toString()))
                            {
                                dialog.setVisible(false);
                            }
                            addMapActor(obj, dialog);
                        }
                        break;
                    case "quest":

                        if (prop.containsKey("questtype")) {
                            TiledMapTileMapObject tiledObj = (TiledMapTileMapObject) obj;
                            String questOrigin = prop.containsKey("questtype") ? prop.get("questtype").toString() : "";
                            AdventureQuestData questInfo = AdventureQuestController.instance().getQuestNPCResponse(TileMapScene.instance().rootPoint.getID(), changes,questOrigin);

                            if (questInfo != null) {
                                DialogActor questActor = new DialogActor(questInfo, this, id);
                                questActor.setVisible(false);
                                addMapActor(obj, questActor);
                            }
                        }
                        break;

                    case "Rotating":
                        String rotation = "";
                        if (prop.containsKey("rotation")) {
                            rotation = prop.get("rotation").toString();
                        }

                        Array<String> possibleShops = new Array<>(rotation.split(","));

                        if (possibleShops.size > 0) {
                            long rotatingRandomSeed = WorldSave.getCurrentSave().getWorld().getRandom().nextLong() + LocalDate.now().toEpochDay();
                            Random rotatingShopRandom = new Random(rotatingRandomSeed);
                            rotatingShop = possibleShops.get(rotatingShopRandom.nextInt(possibleShops.size));
                            changes.setRotatingShopSeed(id, rotatingRandomSeed);
                        }

                        //Intentionally not breaking here.
                        //Flow continues into "shop" case with above data overriding base logic.

                    case "shop":

                        int restockPrice = 0;
                        String shopList = "";

                        boolean isRotatingShop = !rotatingShop.isEmpty();

                        if (isRotatingShop) {
                            shopList = rotatingShop;
                            restockPrice = 7;
                        } else {
                            int rarity = WorldSave.getCurrentSave().getWorld().getRandom().nextInt(100);
                            if (rarity > 95 & prop.containsKey("mythicShopList")) {
                                shopList = prop.get("mythicShopList").toString();
                                restockPrice = 5;
                            }
                            if (shopList.isEmpty() && (rarity > 85 & prop.containsKey("rareShopList"))) {
                                shopList = prop.get("rareShopList").toString();
                                restockPrice = 4;
                            }
                            if (shopList.isEmpty() && (rarity > 55 & prop.containsKey("uncommonShopList"))) {
                                shopList = prop.get("uncommonShopList").toString();
                                restockPrice = 3;
                            }
                            if (shopList.isEmpty() && prop.containsKey("commonShopList")) {
                                shopList = prop.get("commonShopList").toString();
                                restockPrice = 2;
                            }
                            if (shopList.trim().isEmpty() && prop.containsKey("shopList")) {
                                shopList = prop.get("shopList").toString(); //removed but included to not break existing custom planes
                                restockPrice = 0; //Tied to restock button
                            }
                            shopList = shopList.replaceAll("\\s", "");

                        }

                        if (prop.containsKey("noRestock") && (boolean) prop.get("noRestock")) {
                            restockPrice = 0;
                        }

                        possibleShops = new Array<String>(shopList.split(","));
                        Array<String> filteredPossibleShops = new Array<>();
                        if (!isRotatingShop) {
                            for (String candidate : possibleShops) {
                                if (!shopsAlreadyPresent.contains(candidate, false))
                                    filteredPossibleShops.add(candidate);
                            }
                        }
                        if (filteredPossibleShops.isEmpty()) {
                            filteredPossibleShops = possibleShops;
                        }
                        Array<ShopData> shops;
                        if (filteredPossibleShops.size == 0 || shopList.equals(""))
                            shops = WorldData.getShopList();
                        else {
                            shops = new Array<>();
                            for (ShopData data : new Array.ArrayIterator<>(WorldData.getShopList())) {
                                if (filteredPossibleShops.contains(data.name, false)) {
                                    data.restockPrice = restockPrice;
                                    shops.add(data);
                                }
                            }
                        }
                        if (shops.size == 0) continue;

                        ShopData data = shops.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(shops.size));
                        shopsAlreadyPresent.add(data.name);
                        Array<Reward> ret = new Array<>();
                        WorldSave.getCurrentSave().getWorld().getRandom().setSeed(changes.getShopSeed(id));
                        for (RewardData rdata : new Array.ArrayIterator<>(data.rewards)) {
                            ret.addAll(rdata.generate(false, false));
                        }
                        ShopActor actor = new ShopActor(this, id, ret, data);
                        addMapActor(obj, actor);
                        if (prop.containsKey("hasSign") && (boolean) prop.get("hasSign") && prop.containsKey("signYOffset") && prop.containsKey("signXOffset")) {
                            try {
                                TextureSprite sprite = new TextureSprite(Config.instance().getAtlasSprite(data.spriteAtlas, data.sprite));
                                sprite.setX(actor.getX() + Float.parseFloat(prop.get("signXOffset").toString()));
                                sprite.setY(actor.getY() + Float.parseFloat(prop.get("signYOffset").toString()));
                                addMapActor(sprite);

                                if (!(data.overlaySprite == null || data.overlaySprite.isEmpty())) {
                                    TextureSprite overlay = new TextureSprite(Config.instance().getAtlasSprite(data.spriteAtlas, data.overlaySprite));
                                    overlay.setX(actor.getX() + Float.parseFloat(prop.get("signXOffset").toString()));
                                    overlay.setY(actor.getY() + Float.parseFloat(prop.get("signYOffset").toString()));
                                    addMapActor(overlay);
                                }
                            } catch (Exception e) {
                                System.err.print("Can not create Texture for " + data.sprite + " Obj:" + data);
                            }
                        }
                        break;
                    default:
                        System.err.println("Unexpected value: " + type);
                }
            }
        }
    }

    public boolean exitDungeon() {
        WorldSave.getCurrentSave().autoSave();
        AdventureQuestController.instance().updateQuestsLeave();
        AdventureQuestController.instance().showQuestDialogs(this);
        isLoadingMatch = false;
        effect = null; //Reset dungeon effects.
        clearIsInMap();
        Forge.switchScene(GameScene.instance());
        return true;
    }


    @Override
    public void setWinner(boolean playerWins) {
        isLoadingMatch = false;
        freezeAllEnemyBehaviors = true;
        if (playerWins) {
            currentMob.clearCollisionHeight();
            Current.player().win();
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.playEffect(Paths.EFFECT_BLOOD, 0.5f);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
                    currentMob.resetCollisionHeight();
                    startPause(0.3f, MapStage.this::getReward);
                    AdventureQuestController.instance().updateQuestsWin(currentMob,enemies);
                    AdventureQuestController.instance().showQuestDialogs(MapStage.this);
                    player.setAnimation(CharacterSprite.AnimationTypes.Idle);
                }
            }, 1f);
        } else {
            currentMob.clearCollisionHeight();
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(0.3f, () -> {
                player.setAnimation(CharacterSprite.AnimationTypes.Idle);
                currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                currentMob.resetCollisionHeight();
                if (positions.peek() != null) {
                    player.setPosition(positions.peek());
                }
                currentMob.freezeMovement();
                AdventureQuestController.instance().updateQuestsLose(currentMob);
                AdventureQuestController.instance().showQuestDialogs(MapStage.this);
                boolean defeated = Current.player().defeated();
                if (canFailDungeon && defeated) {
                    //If hardcore mode is added, check and redirect to game over screen here
                    dungeonFailedDialog();
                    exitDungeon();
                }
                MapStage.this.stop();
                currentMob = null;
            });
        }
    }

    private void dungeonFailedDialog() {
        dialog.getButtonTable().clear();
        dialog.getContentTable().clear();
        dialog.clearListeners();
        TextraButton ok = Controls.newTextButton("OK", this::hideDialog);
        ok.setVisible(false);
        TypingLabel L = Controls.newTypingLabel("{GRADIENT=RED;WHITE;1;1}Defeated and unable to continue, you use the last of your power to escape the area.");
        L.setWrap(true);
        L.setTypingListener(new TypingAdapter() {
            @Override
            public void end() {
                ok.setVisible(true);
            }
        });
        dialog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                L.skipToTheEnd();
                super.clicked(event, x, y);
                //exitDungeon();
            }
        });
        dialog.getButtonTable().add(ok).width(240f);
        dialog.getContentTable().add(L).width(250f);
        dialog.setKeepWithinStage(true);
        showDialog();
    }

    public boolean deleteObject(int id) {
        changes.deleteObject(id);
        for (int i = 0; i < actors.size; i++) {
            if (actors.get(i).getObjectId() == id && id > 0) {
                if (actors.get(i).getClass().equals(EnemySprite.class)) {
                    enemies.remove((EnemySprite) actors.get(i));
                }
                actors.get(i).remove();
                actors.removeIndex(i);
                return true;
            }
        }
        return false;
    }

    public boolean activateMapObject(int id){
        if (changes.isObjectDeleted(id)){
            return false;
        }
        for (int i = 0; i < actors.size; i++) {
            if (actors.get(i).getObjectId() == id && id > 0) {
                if (actors.get(i) instanceof EnemySprite) {
                    ((EnemySprite)(actors.get(i))).inactive = false;
                    (actors.get(i)).resetCollisionHeight();
                    return true;
                }
                else if (actors.get(i) instanceof PortalActor) {
                    PortalActor thisPortal = (PortalActor)(actors.get(i));

                    if (thisPortal.getAnimation().equals("active"))
                        thisPortal.setAnimation("closed");
                    else
                        thisPortal.setAnimation("active");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean lookForID(int id) { //Search actor by ID.

        for (MapActor A : new Array.ArrayIterator<>(actors)) {
            if (A.getId() == id)
                return true;
        }
        return false;
    }

    public EnemySprite getEnemyByID(int id) { //Search actor by ID, enemies only.
        for (MapActor A : new Array.ArrayIterator<>(actors)) {
            if (A instanceof EnemySprite && A.getId() == id)
                return ((EnemySprite) A);
        }
        return null;
    }

    public Actor getByID(int id) { //Search actor by ID.
        for (MapActor A : new Array.ArrayIterator<>(actors)) {
            if (A.getId() == id)
                return A;
        }
        return null;
    }

    protected void getReward() {
        isLoadingMatch = false;
        RewardScene.instance().loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
        Forge.switchScene(RewardScene.instance());
        if (currentMob.defeatDialog == null) {
            currentMob.remove();
            actors.removeValue(currentMob, true);
            if (!respawnEnemies || currentMob.getData().boss)
                changes.deleteObject(currentMob.getId());
                enemies.remove(currentMob);
        } else {
            currentMob.defeatDialog.activate();
            player.setAnimation(CharacterSprite.AnimationTypes.Idle);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
        }
        currentMob = null;
    }

    public void removeAllEnemies() {
        Array<Integer> idsToRemove = new Array<>();
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor instanceof EnemySprite) {
                idsToRemove.add(actor.getObjectId());
            }
        }
        for (Integer i : idsToRemove) deleteObject(i);
    }

    @Override
    protected void onActing(float delta) {
        if (isPaused() || isDialogOnlyInput())
            return;
        Iterator<EnemySprite> it = enemies.iterator();

        if (freezeAllEnemyBehaviors) {
            if (!positions.contains(player.pos())) {
                freezeAllEnemyBehaviors = false;
            }
            else return;
        }
        float mobSize = navMapSize; //todo: replace with actual size if multiple nav maps implemented
        ArrayList<NavigationVertex> verticesNearPlayer = new ArrayList<>(navMaps.get(mobSize).navGraph.getNodes());
        verticesNearPlayer.sort(Comparator.comparingInt(o -> Math.round((o.pos.x - player.pos().x) * (o.pos.x - player.pos().x) + (o.pos.y - player.pos().y) * (o.pos.y - player.pos().y))));

        if (!freezeAllEnemyBehaviors) {
            while (it.hasNext()) {
                EnemySprite mob = it.next();
                if (mob.inactive){
                    continue;
                }
                mob.updatePositon();

                ProgressableGraphPath<NavigationVertex> navPath = new ProgressableGraphPath<>(0);
                if (mob.getData().flying) {
                    navPath.add(new NavigationVertex(mob.getTargetVector(player, null,delta)));
                } else {
                    Vector2 destination = mob.getTargetVector(player, verticesNearPlayer, delta);

                    if (destination.epsilonEquals(mob.pos()) && !mob.aggro) {
                        mob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                        continue;
                    }
                    if (destination.equals(mob.targetVector) && mob.getNavPath() != null)
                        navPath = mob.getNavPath();

                    if (navPath.nodes.size == 0 || !destination.equals(mob.targetVector)) {
                        mob.targetVector = destination;
                        navPath = navMaps.get(mobSize).findShortestPath(mobSize, mob.pos(), mob.targetVector);
                    }

                    if (mob.aggro) {
                        navPath.add(new NavigationVertex(player.pos()));
                    }
                }

                if (navPath == null || navPath.getCount() == 0 || navPath.get(0) == null) {
                        mob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                        continue;
                }
                Vector2 currentVector = null;

                while (navPath.getCount() > 0 && navPath.get(0) != null && (navPath.get(0).pos == null || navPath.get(0).pos.dst(mob.pos()) < 0.5f)) {

                    navPath.remove(0);

                }
                if (navPath.getCount() != 0) {
                    currentVector = new Vector2(navPath.get(0).pos).sub(mob.pos());
                }
                mob.setNavPath(navPath);
                mob.clearActions();
                if (currentVector == null || (currentVector.x == 0.0f && currentVector.y == 0.0f)) {
                    mob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                    continue;
                }
                mob.steer(currentVector);
                mob.update(delta);
            }
        }

        float sprintingMod = currentModifications.containsKey(PlayerModification.Sprint) ? 2 : 1;
        player.setMoveModifier(2 * sprintingMod);

        positions.add(player.pos());
        if (positions.size() > 4)
            positions.remove();

        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor.collideWithPlayer(player)) {
                if (actor instanceof EnemySprite) {
                    EnemySprite mob = (EnemySprite) actor;
                    currentMob = mob;
                    resetPosition();
                    if (mob.dialog != null && mob.dialog.canShow()) { //This enemy has something to say. Display a dialog like if it was a DialogActor but only if dialogue is possible.
                        mob.dialog.activate();
                    } else { //Duel the enemy.
                        beginDuel(mob);
                    }
                    break;
                } else if (actor instanceof RewardSprite) {
                    freezeAllEnemyBehaviors = true;
                    Gdx.input.vibrate(50);
                    if (Controllers.getCurrent() != null && Controllers.getCurrent().canVibrate())
                        Controllers.getCurrent().startVibration(100, 1);
                    startPause(0.1f, () -> { //Switch to item pickup scene.
                        RewardSprite RS = (RewardSprite) actor;
                        RewardScene.instance().loadRewards(RS.getRewards(), RewardScene.Type.Loot, null);
                        RS.remove();
                        actors.removeValue(RS, true);
                        changes.deleteObject(RS.getId());
                        Forge.switchScene(RewardScene.instance());
                    });
                    break;
                }
            }
        }
    }

    public void beginDuel(EnemySprite mob) {
        if (mob == null) return;
        mob.clearCollisionHeight();
        currentMob = mob;
        player.setAnimation(CharacterSprite.AnimationTypes.Attack);
        player.playEffect(Paths.EFFECT_SPARKS, 0.5f);
        mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
        SoundSystem.instance.play(SoundEffectType.Block, false);
        Gdx.input.vibrate(50);
        int duration = mob.getData().boss ? 400 : 200;
        if (Controllers.getCurrent() != null && Controllers.getCurrent().canVibrate())
            Controllers.getCurrent().startVibration(duration, 1);
        Forge.restrictAdvMenus = true;
        player.clearCollisionHeight();
        startPause(0.8f, () -> {
            Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
            SoundSystem.instance.play(SoundEffectType.ManaBurn, false);
            DuelScene duelScene = DuelScene.instance();
            FThreads.invokeInEdtNowOrLater(() -> {
                if (!isLoadingMatch) {
                    isLoadingMatch = true;
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        duelScene.initDuels(player, mob);
                        if (isInMap && effect != null && !mob.ignoreDungeonEffect)
                            duelScene.setDungeonEffect(effect);
                        Forge.switchScene(duelScene);
                    }, Forge.takeScreenshot(), true, false, false, false, "", Current.player().avatar(), mob.getAtlasPath(), Current.player().getName(), mob.getName()));
                }
            });
        });
    }

    public void setPointOfInterest(PointOfInterestChanges change) {
        changes = change;
    }

    public boolean isInMap() {
        return isInMap;
    }

    public boolean isDialogOnlyInput() {
        return dialogOnlyInput;
    }

    public void showDialog() {
        if (dialogStage == null){
            setDialogStage(GameHUD.getInstance());
        }
        GameHUD.getInstance().playerIdle();
        dialogButtonMap.clear();
        for (int i = 0; i < dialog.getButtonTable().getCells().size; i++) {
            dialogButtonMap.add((TextraButton) dialog.getButtonTable().getCells().get(i).getActor());
        }
        freezeAllEnemyBehaviors = true;
        dialog.show(dialogStage, Actions.show());
        dialog.setPosition((dialogStage.getWidth() - dialog.getWidth()) / 2, (dialogStage.getHeight() - dialog.getHeight()) / 2);
        dialogOnlyInput = true;
        if (Forge.hasGamepad() && !dialogButtonMap.isEmpty())
            dialogStage.setKeyboardFocus(dialogButtonMap.first());
    }

    public void hideDialog() {
        dialog.hide(Actions.sequence(Actions.sizeTo(dialog.getOriginX(), dialog.getOriginY(), 0.3f), Actions.hide()));
        dialogOnlyInput = false;
        selectedKey = null;
        dialog.clearListeners();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void resetPosition() {
        if (positions.peek() != null){
            player.setPosition(positions.peek());
        }
        stop();
    }

    public void setQuestFlag(String key, int value) {
        changes.getMapFlags().put(key, (byte) value);

        AdventureQuestController.instance().updateQuestsMapFlag(key,value);
        AdventureQuestController.instance().showQuestDialogs(this);
    }

    public void advanceQuestFlag(String key) {
        Map<String, Byte> C = changes.getMapFlags();
        if (C.get(key) != null) {
            C.put(key, (byte) (C.get(key) + 1));
        } else {
            C.put(key, (byte) 1);
        }

        AdventureQuestController.instance().updateQuestsMapFlag(key,changes.getMapFlags().get(key));
        AdventureQuestController.instance().showQuestDialogs(this);
    }

    public boolean checkQuestFlag(String key) {
        return changes.getMapFlags().get(key) != null;
    }

    public int getQuestFlag(String key) {
        return (int) changes.getMapFlags().getOrDefault(key, (byte) 0);
    }

    public void resetQuestFlags() {
        changes.getMapFlags().clear();
    }

    public boolean dialogInput(int keycode) {
        if (dialogOnlyInput) {
            if (KeyBinding.Up.isPressed(keycode)) {
                selectPreviousDialogButton();
            }
            if (KeyBinding.Down.isPressed(keycode)) {
                selectNextDialogButton();
            }
            if (KeyBinding.Use.isPressed(keycode)) {
                performTouch(dialogStage.getKeyboardFocus());
            }
        }
        return true;
    }

    public void performTouch(Actor actor) {
        if (actor == null)
            return;
        actor.fire(eventTouchDown);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                actor.fire(eventTouchUp);
            }
        }, 0.10f);
    }

    private void selectNextDialogButton() {
        if (dialogButtonMap.size < 2)
            return;
        if (!(dialogStage.getKeyboardFocus() instanceof Button)) {
            dialogStage.setKeyboardFocus(dialogButtonMap.first());
            return;
        }
        for (int i = 0; i < dialogButtonMap.size; i++) {
            if (dialogStage.getKeyboardFocus() == dialogButtonMap.get(i)) {
                i += 1;
                i %= dialogButtonMap.size;
                dialogStage.setKeyboardFocus(dialogButtonMap.get(i));
                return;
            }
        }
    }

    private void selectPreviousDialogButton() {
        if (dialogButtonMap.size < 2)
            return;
        if (!(dialogStage.getKeyboardFocus() instanceof Button)) {
            dialogStage.setKeyboardFocus(dialogButtonMap.first());
            return;
        }
        for (int i = 0; i < dialogButtonMap.size; i++) {
            if (dialogStage.getKeyboardFocus() == dialogButtonMap.get(i)) {
                i -= 1;
                if (i < 0)
                    i = dialogButtonMap.size - 1;
                dialogStage.setKeyboardFocus(dialogButtonMap.get(i));
                return;
            }
        }
    }
}
