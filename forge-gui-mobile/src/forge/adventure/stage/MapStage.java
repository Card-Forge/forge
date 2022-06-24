package forge.adventure.stage;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import forge.Forge;
import forge.adventure.character.*;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.gui.FThreads;
import forge.screens.TransitionScreen;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;


import java.util.Map;

import static forge.adventure.util.Paths.MANA_ATLAS;

/**
 * Stage to handle tiled maps for points of interests
 */
public class MapStage extends GameStage {
    public static MapStage instance;
    Array<MapActor> actors = new Array<>();

    TiledMap map;
    Array<Rectangle>[][] collision;
    private float tileHeight;
    private float tileWidth;
    private float width;
    private float height;
    private boolean isInMap = false;
    MapLayer spriteLayer;
    private PointOfInterestChanges changes;
    private EnemySprite currentMob;
    private final Vector2 oldPosition = new Vector2();//todo
    private final Vector2 oldPosition2 = new Vector2();
    private final Vector2 oldPosition3 = new Vector2();
    private final Vector2 oldPosition4 = new Vector2();
    private boolean isLoadingMatch = false;
    private ObjectMap<String, Byte> mapFlags = new ObjectMap<>(); //Stores local map flags. These aren't available outside this map.

    private Dialog dialog;
    private Stage dialogStage;
    private boolean dialogOnlyInput;

    //Map properties.
    //These maps are defined as embedded properties within the Tiled maps.
    private EffectData effect;             //"Dungeon Effect": Character Effect applied to all adversaries within the map.
    private boolean preventEscape = false; //Prevents player from escaping the dungeon by any means that aren't an exit.


    public boolean getDialogOnlyInput() {
        return dialogOnlyInput;
    }
    public Dialog getDialog() {
        return dialog;
    }

    public boolean canEscape() { return (preventEscape ? true : false); } //Check if escape is possible.

    public void clearIsInMap() {
        isInMap = false;
        effect = null; //Reset effect so battles outside the dungeon don't use the last visited dungeon's effects.
        preventEscape = false;
        GameHUD.getInstance().showHideMap(true);
    }
    public void draw (Batch batch) {
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

    public static MapStage getInstance() {
        return instance == null ? instance = new MapStage() : instance;
    }
    public void resLoaded()
    {
        dialog = Controls.newDialog("");
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
        for (Rectangle collision : currentCollidingRectangles) {
            if (collision.overlaps(adjustedBoundingRect)) {
                return true;
            }
        }
        return false;
    }

    final Array<Rectangle> currentCollidingRectangles = new Array<>();

    @Override
    public void prepareCollision(Vector2 pos, Vector2 direction, Rectangle boundingRect) {
        currentCollidingRectangles.clear();
        int x1 = (int) (Math.min(boundingRect.x, boundingRect.x + direction.x) / tileWidth);
        int y1 = (int) (Math.min(boundingRect.y, boundingRect.y + direction.y) / tileHeight);
        int x2 = (int) (Math.min(boundingRect.x + boundingRect.width, boundingRect.x + boundingRect.width + direction.x) / tileWidth);
        int y2 = (int) (Math.min(boundingRect.y + boundingRect.height, boundingRect.y + boundingRect.height + direction.y) / tileHeight);

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    continue;
                }
                currentCollidingRectangles.addAll(collision[x][y]);
            }
        }
    }


    Group collisionGroup;

    @Override
    protected void debugCollision(boolean b) {

        if (collisionGroup == null) {
            collisionGroup = new Group();

            for (int x = 0; x < collision.length; x++) {
                for (int y = 0; y < collision[x].length; y++) {
                    for (Rectangle rectangle : collision[x][y]) {
                        MapActor collisionActor = new MapActor(0);
                        collisionActor.setBoundDebug(true);
                        collisionActor.setWidth(rectangle.width);
                        collisionActor.setHeight(rectangle.height);
                        collisionActor.setX(rectangle.x);
                        collisionActor.setY(rectangle.y);
                        collisionGroup.addActor(collisionActor);
                    }
                }
            }

        }
        if (b) {
            addActor(collisionGroup);
        } else {
            collisionGroup.remove();
        }

    }

    private void effectDialog(EffectData E){
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        String text = "Strange magical energies flow within this place...\nAll opponents get:\n";
        text += E.getDescription();
        Label L = Controls.newLabel(text);
        L.setWrap(true);
        dialog.getContentTable().add(L).width(250f);
        dialog.getButtonTable().add(Controls.newTextButton("OK", this::hideDialog)).width(250f);
        dialog.setKeepWithinStage(true);
        showDialog();
    }
    public void showImageDialog(String message, Texture texture) {
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        if (texture != null) {
            TextureRegion tr = new TextureRegion(texture);
            tr.flip(true, true);
            Image image = new Image(tr);
            image.setScaling(Scaling.fit);
            dialog.getContentTable().add(image).height(100);
            dialog.getContentTable().add().row();
        }
        Label L = Controls.newLabel(message);
        L.setWrap(true);
        dialog.getContentTable().add(L).width(250f);
        dialog.getButtonTable().add(Controls.newTextButton("OK", this::hideDialog)).width(250f);
        dialog.setKeepWithinStage(true);
        setDialogStage(GameHUD.getInstance());
        showDialog();
    }
    public void showDeckAwardDialog(String message, Deck deck) {
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        if (deck != null) {
            TextureAtlas atlas = Config.instance().getAtlas(MANA_ATLAS);
            ColorSet deckColor = DeckProxy.getColorIdentity(deck);
            if (deckColor.isColorless()) {
                Image pixC = new Image(atlas.createSprite("pixC"));
                pixC.setScaling(Scaling.fit);
                dialog.getContentTable().add(pixC).height(20).width(20);
                dialog.getContentTable().add().row();
            } else if (deckColor.isMonoColor()) {
                Image pix = new Image(atlas.createSprite("pixC"));
                if (deckColor.hasWhite())
                    pix = new Image(atlas.createSprite("pixW"));
                else if (deckColor.hasBlue())
                    pix = new Image(atlas.createSprite("pixU"));
                else if (deckColor.hasBlack())
                    pix = new Image(atlas.createSprite("pixB"));
                else if (deckColor.hasRed())
                    pix = new Image(atlas.createSprite("pixR"));
                else if (deckColor.hasGreen())
                    pix = new Image(atlas.createSprite("pixG"));
                pix.setScaling(Scaling.fit);
                dialog.getContentTable().add(pix).height(20).width(20);
                dialog.getContentTable().add().row();
            } else if (deckColor.isMulticolor()) {
                Group group = new Group();
                int mul = 0;
                if (deckColor.hasWhite()) {
                    Image pix = new Image(atlas.createSprite("pixW"));
                    pix.setScaling(Scaling.fit);
                    pix.setSize(20,20);
                    pix.setPosition(0, 0);
                    group.addActor(pix);
                    mul++;
                }
                if (deckColor.hasBlue()) {
                    Image pix = new Image(atlas.createSprite("pixU"));
                    pix.setScaling(Scaling.fit);
                    pix.setSize(20,20);
                    pix.setPosition(20*mul, 0);
                    mul++;
                    group.addActor(pix);
                }
                if (deckColor.hasBlack()) {
                    Image pix = new Image(atlas.createSprite("pixB"));
                    pix.setScaling(Scaling.fit);
                    pix.setSize(20,20);
                    pix.setPosition(20*mul, 0);
                    mul++;
                    group.addActor(pix);
                }
                if (deckColor.hasRed()) {
                    Image pix = new Image(atlas.createSprite("pixR"));
                    pix.setScaling(Scaling.fit);
                    pix.setSize(20,20);
                    pix.setPosition(20*mul, 0);
                    mul++;
                    group.addActor(pix);
                }
                if (deckColor.hasGreen()) {
                    Image pix = new Image(atlas.createSprite("pixG"));
                    pix.setScaling(Scaling.fit);
                    pix.setSize(20,20);
                    pix.setPosition(20*mul, 0);
                    mul++;
                    group.addActor(pix);
                }
                group.setHeight(20);
                group.setWidth(20*mul);
                dialog.getContentTable().add(group).align(Align.center);
                dialog.getContentTable().add().row();
            }
        }
        Label L = Controls.newLabel(message);
        L.setWrap(true);
        dialog.getContentTable().add(L).width(240);
        dialog.getButtonTable().add(Controls.newTextButton("OK", this::hideDialog)).width(240);
        dialog.setKeepWithinStage(true);
        setDialogStage(GameHUD.getInstance());
        showDialog();
    }

    public void loadMap(TiledMap map, String sourceMap) {
        isLoadingMatch = false;
        isInMap = true;
        GameHUD.getInstance().showHideMap(false);
        this.map = map;
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            actor.remove();
            foregroundSprites.removeActor(actor);
        }

        actors = new Array<>();
        width = Float.parseFloat(map.getProperties().get("width").toString());
        height = Float.parseFloat(map.getProperties().get("height").toString());
        tileHeight = Float.parseFloat(map.getProperties().get("tileheight").toString());
        tileWidth = Float.parseFloat(map.getProperties().get("tilewidth").toString());
        setBounds(width * tileWidth, height * tileHeight);
        collision = new Array[(int) width][(int) height];

        //Load dungeon effects.
        MapProperties MP = map.getProperties();

        if( MP.get("dungeonEffect") != null && !MP.get("dungeonEffect").toString().isEmpty()){
            effect = JSONStringLoader.parse(EffectData.class, map.getProperties().get("dungeonEffect").toString(), "");
            effectDialog(effect);
        }
        if (MP.get("preventEscape") != null) preventEscape = (boolean)MP.get("preventEscape");

        if (MP.get("music") != null && !MP.get("music").toString().isEmpty()){
            //TODO: Add a way to play a music file directly without using a playlist.
        }

        GetPlayer().stop();
        spriteLayer = null;
        for (MapLayer layer : map.getLayers()) {
            if (layer.getProperties().containsKey("spriteLayer") && layer.getProperties().get("spriteLayer", boolean.class)) {
                spriteLayer = layer;
            }
            if (layer instanceof TiledMapTileLayer) {
                loadCollision((TiledMapTileLayer) layer);
            } else {
                loadObjects(layer, sourceMap);
            }
        }
        if(spriteLayer == null) System.err.print("Warning: No spriteLayer present in map.\n");

    }

    private void loadCollision(TiledMapTileLayer layer) {
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                if (collision[x][y] == null)
                    collision[x][y] = new Array<>();
                Array<Rectangle> map = collision[x][y];
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null)
                    continue;
                for (MapObject collision : cell.getTile().getObjects()) {
                    if (collision instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) collision).getRectangle();
                        map.add(new Rectangle((Math.round(layer.getTileWidth() * x) + r.x), (Math.round(layer.getTileHeight() * y) + r.y), Math.round(r.width), Math.round(r.height)));
                    }
                }
            }
        }
    }

    private boolean canSpawn(MapProperties prop) {
        DifficultyData DF = Current.player().getDifficulty();
        boolean spawnEasy = prop.get("spawn.Easy", Boolean.class);
        boolean spawnNorm = prop.get("spawn.Normal", Boolean.class);
        boolean spawnHard = prop.get("spawn.Hard", Boolean.class);
        if(DF.spawnRank == 2 && !spawnHard) return false;
        if(DF.spawnRank == 1 && !spawnNorm) return false;
        if(DF.spawnRank == 0 && !spawnEasy) return false;
        return true;
    }

    private void loadObjects(MapLayer layer, String sourceMap) {
        player.setMoveModifier(2);
        for (MapObject obj : layer.getObjects()) {
            MapProperties prop = obj.getProperties();
            if (prop.containsKey("type")) {
                String type = prop.get("type", String.class);
                int id = prop.get("id", int.class);
                if (changes.isObjectDeleted(id))
                    continue;
                boolean hidden = !obj.isVisible(); //Check if the object is invisible.

                switch (type) {
                    case "entry":
                        float x = Float.parseFloat(prop.get("x").toString());
                        float y = Float.parseFloat(prop.get("y").toString());
                        float w = Float.parseFloat(prop.get("width").toString());
                        float h = Float.parseFloat(prop.get("height").toString());
                        EntryActor entry = new EntryActor(this, sourceMap, id, prop.get("teleport").toString(), x, y, w, h, prop.get("direction").toString());
                        addMapActor(obj, entry);
                        break;
                    case "reward":
                        if(!canSpawn(prop)) break;
                        Object R = prop.get("reward");
                        if(R != null && !R.toString().isEmpty()) {
                            Object S = prop.get("sprite");
                            String Sp; Sp = "sprites/treasure.atlas";
                            if(S != null && !S.toString().isEmpty()) Sp = S.toString();
                            else System.err.printf("No sprite defined for reward (ID:%s), defaulting to \"sprites/treasure.atlas\"", id);
                            RewardSprite RW = new RewardSprite(id, R.toString(), Sp);
                            RW.hidden = hidden;
                            addMapActor(obj, RW);
                        }
                        break;
                    case "enemy":
                        if(!canSpawn(prop)) break;
                        Object E = prop.get("enemy");
                        if(E != null && !E.toString().isEmpty()) {
                            EnemyData EN = WorldData.getEnemy(E.toString());
                            if(EN == null){
                                System.err.printf("Enemy \"%s\" not found.", E.toString());
                                break;
                            }
                            EnemySprite mob = new EnemySprite(id, EN);
                            Object D = prop.get("dialog"); //Check if the enemy has a dialogue attached to it.
                            if(D != null && !D.toString().isEmpty()) {
                                mob.dialog = new MapDialog(D.toString(), this, mob.getId());
                            }
                            D = prop.get("defeatDialog"); //Check if the enemy has a defeat dialogue attached to it.
                            if(D != null && !D.toString().isEmpty()) {
                                mob.defeatDialog = new MapDialog(D.toString(), this, mob.getId());
                            }
                            D = prop.get("name"); //Check for name override.
                            if(D != null && !D.toString().isEmpty()) {
                                mob.nameOverride = D.toString();
                            }
                            D = prop.get("effect"); //Check for special effects.
                            if(D != null && !D.toString().isEmpty()) {
                                mob.effect = JSONStringLoader.parse(EffectData.class, D.toString(), "");
                            }
                            D = prop.get("reward"); //Check for additional rewards.
                            if(D != null && !D.toString().isEmpty()) {
                                mob.rewards = JSONStringLoader.parse(RewardData[].class, D.toString(), "[]");
                            }
                            mob.hidden = hidden; //Evil.
                            addMapActor(obj, mob);
                        }
                        break;
                    case "dummy": //Does nothing. Mostly obstacles to be removed by ID by switches or such.
                        TiledMapTileMapObject obj2 = (TiledMapTileMapObject) obj;
                        DummySprite D = new DummySprite(id, obj2.getTextureRegion(), this);
                        addMapActor(obj, D);
                        //TODO: Ability to toggle their solid state.
                        //TODO: Ability to move them (using a sequence such as "UULU" for up, up, left, up).
                        break;
                    case "inn":
                        addMapActor(obj, new OnCollide(new Runnable() {
                            @Override
                            public void run() {
                                Forge.switchScene(SceneType.InnScene.instance);
                            }
                        }));
                        break;
                    case "spellsmith":
                        addMapActor(obj, new OnCollide(new Runnable() {
                            @Override
                            public void run() { Forge.switchScene(SceneType.SpellSmithScene.instance); }
                        }));
                        break;
                    case "exit":
                        addMapActor(obj, new OnCollide(new Runnable() {
                            @Override
                            public void run() {
                                MapStage.this.exit();
                            }
                        }));
                        break;
                    case "dialog":
                        if(obj instanceof TiledMapTileMapObject) {
                            TiledMapTileMapObject tiledObj = (TiledMapTileMapObject) obj;
                            DialogActor dialog = new DialogActor(this, id, prop.get("dialog").toString(), tiledObj.getTextureRegion());
                            addMapActor(obj, dialog);
                        }
                        break;
                    case "shop":
                        String shopList = prop.get("shopList").toString();
                        shopList=shopList.replaceAll("\\s","");
                        Array<String> possibleShops = new Array<>(shopList.split(","));
                        Array<ShopData> shops;
                        if (possibleShops.size == 0 || shopList.equals(""))
                            shops = WorldData.getShopList();
                        else {
                            shops = new Array<>();
                            for (ShopData data : new Array.ArrayIterator<>(WorldData.getShopList())) {
                                if (possibleShops.contains(data.name, false)) {
                                    shops.add(data);
                                }
                            }
                        }
                        if(shops.size == 0) continue;

                        ShopData data = shops.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(shops.size));
                        Array<Reward> ret = new Array<>();
                        for (RewardData rdata : new Array.ArrayIterator<>(data.rewards)) {
                            ret.addAll(rdata.generate(false));
                        }
                        ShopActor actor = new ShopActor(this, id, ret, data.unlimited);
                        addMapActor(obj, actor);
                        if (prop.containsKey("signYOffset") && prop.containsKey("signXOffset")) {
                            try {
                                TextureSprite sprite = new TextureSprite(Config.instance().getAtlas(data.spriteAtlas).createSprite(data.sprite));
                                sprite.setX(actor.getX() + Float.parseFloat(prop.get("signXOffset").toString()));
                                sprite.setY(actor.getY() + Float.parseFloat(prop.get("signYOffset").toString()));
                                addMapActor(sprite);
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

    public boolean exit() {
        isLoadingMatch = false;
        effect = null; //Reset dungeon effects.
        clearIsInMap();
        Forge.switchScene(SceneType.GameScene.instance);
        return true;
    }


    @Override
    public void setWinner(boolean playerWins) {
        isLoadingMatch = false;
        if (playerWins) {
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
            startPause(0.3f, new Runnable() {
                @Override
                public void run() {
                    MapStage.this.getReward();
                }
            });
        } else {
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(0.3f, new Runnable() {
                @Override
                public void run() {
                    player.setAnimation(CharacterSprite.AnimationTypes.Idle);
                    currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                    player.setPosition(oldPosition4);
                    Current.player().defeated();
                    MapStage.this.stop();
                    currentMob = null;
                }
            });
        }

    }

    public boolean deleteObject(int id) {
        changes.deleteObject(id);
        for (int i=0;i< actors.size;i++) {
            if (actors.get(i).getObjectId() == id && id > 0) {
                actors.get(i).remove();
                actors.removeIndex(i);
                return true;
            }
        }
        return false;
    }

    public boolean lookForID(int id){ //Search actor by ID.

        for(MapActor A : new Array.ArrayIterator<>(actors)){
            if(A.getId() == id)
                return true;
        }
        return false;
    }

    public EnemySprite getEnemyByID(int id) { //Search actor by ID, enemies only.
        for(MapActor A : new Array.ArrayIterator<>(actors)){
            if(A instanceof EnemySprite && A.getId() == id)
                return ((EnemySprite) A);
        }
        return null;
    }

    protected void getReward() {
        isLoadingMatch = false;
        ((RewardScene) SceneType.RewardScene.instance).loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
        Forge.switchScene(SceneType.RewardScene.instance);
        if(currentMob.defeatDialog == null) {
            currentMob.remove();
            actors.removeValue(currentMob, true);
            changes.deleteObject(currentMob.getId());
        } else {
            currentMob.defeatDialog.activate();
            player.setAnimation(CharacterSprite.AnimationTypes.Idle);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
        }
        currentMob = null;
    }
    public void removeAllEnemies() {
        Array<Integer> idsToRemove=new Array<>();
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor instanceof EnemySprite) {
                idsToRemove.add(actor.getObjectId());
            }
        }
        for(Integer i:idsToRemove) deleteObject(i);
    }

    @Override
    protected void onActing(float delta) {
        oldPosition4.set(oldPosition3);
        oldPosition3.set(oldPosition2);
        oldPosition2.set(oldPosition);
        oldPosition.set(player.pos());
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor.collideWithPlayer(player)) {
                if (actor instanceof EnemySprite) {
                    EnemySprite mob = (EnemySprite) actor;
                    currentMob = mob;
                    resetPosition();
                    if(mob.dialog != null && mob.dialog.canShow()){ //This enemy has something to say. Display a dialog like if it was a DialogActor but only if dialogue is possible.
                        mob.dialog.activate();
                    } else { //Duel the enemy.
                        beginDuel(mob);
                    }
                    break;
                } else if (actor instanceof RewardSprite) {
                    Gdx.input.vibrate(50);
                    startPause(0.1f, new Runnable() {
                        @Override
                        public void run() { //Switch to item pickup scene.
                            RewardSprite RS = (RewardSprite) actor;
                            ((RewardScene) SceneType.RewardScene.instance).loadRewards(RS.getRewards(), RewardScene.Type.Loot, null);
                            RS.remove();
                            actors.removeValue(RS, true);
                            changes.deleteObject(RS.getId());
                            Forge.switchScene(SceneType.RewardScene.instance);
                        }
                    });
                    break;
                }
            }
        }
    }

    public void beginDuel(EnemySprite mob){
        if(mob == null) return;
        currentMob = mob;
        player.setAnimation(CharacterSprite.AnimationTypes.Attack);
        mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
        SoundSystem.instance.play(SoundEffectType.Block, false);
        Gdx.input.vibrate(50);
        startPause(0.8f, () -> {
            Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
            SoundSystem.instance.play(SoundEffectType.ManaBurn, false);
            DuelScene duelScene = ((DuelScene) SceneType.DuelScene.instance);
            FThreads.invokeInEdtNowOrLater(() -> {
                if (!isLoadingMatch) {
                    isLoadingMatch = true;
                    Forge.setTransitionScreen(new TransitionScreen(() -> {
                        duelScene.initDuels(player, mob);
                        Forge.clearTransitionScreen();
                        startPause(0.3f, () -> {
                            if(isInMap && effect != null) duelScene.setDungeonEffect(effect);
                            Forge.switchScene(SceneType.DuelScene.instance);
                        });
                    }, Forge.takeScreenshot(), true, false));
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

    public void showDialog() {
        dialog.show(dialogStage, Actions.show());
        dialog.setPosition((dialogStage.getWidth() - dialog.getWidth()) / 2, (dialogStage.getHeight() - dialog.getHeight()) / 2);
        dialogOnlyInput=true;
    }
    public void hideDialog() {
        dialog.hide(Actions.sequence(Actions.sizeTo(dialog.getOriginX(), dialog.getOriginY(), 0.3f), Actions.hide()));
        dialogOnlyInput=false;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage=dialogStage;
    }

    public void resetPosition() {
        player.setPosition(oldPosition4);
        stop();
    }

    public void setQuestFlag(String key, int value){ changes.getMapFlags().put(key, (byte) value); }
    public void advanceQuestFlag(String key){
        Map<String, Byte> C = changes.getMapFlags();
        if(C.get(key) != null){
            C.put(key, (byte) (C.get(key) + 1));
        } else {
            C.put(key, (byte) 1);
        }
    }
    public boolean checkQuestFlag(String key){
        return changes.getMapFlags().get(key) != null;
    }
    public int getQuestFlag(String key){
        return (int) changes.getMapFlags().getOrDefault(key, (byte) 0);
    }
    public void resetQuestFlags(){
        changes.getMapFlags().clear();
    }
}
