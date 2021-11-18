package forge.adventure.stage;


import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.*;
import forge.adventure.data.RewardData;
import forge.adventure.data.ShopData;
import forge.adventure.data.WorldData;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.adventure.world.PointOfInterestChanges;
import forge.adventure.world.WorldSave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage to handle tiled maps for points of interests
 */
public class MapStage extends GameStage {

    public static MapStage instance;
    Array<MapActor> actors = new Array<>();

    TiledMap map;
    ArrayList<Rectangle>[][] collision;
    private float tileHeight;
    private float tileWidth;
    private float width;
    private float height;
    private boolean isInMap=false;
    MapLayer spriteLayer;
    private PointOfInterestChanges changes;
    private EnemySprite currentMob;
    private final Vector2 oldPosition=new Vector2();//todo
    private final Vector2 oldPosition2=new Vector2();
    private final Vector2 oldPosition3=new Vector2();
    private final Vector2 oldPosition4=new Vector2();


    public MapLayer getSpriteLayer()
    {
        return spriteLayer;
    }
    public PointOfInterestChanges getChanges()
    {
        return changes;
    }
    public static MapStage getInstance() {
        return instance == null ? instance = new MapStage() : instance;
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
    public boolean isColliding( Rectangle adjustedBoundingRect)
    {
        for(Rectangle collision:currentCollidingRectangles)
        {
            if(collision.overlaps(adjustedBoundingRect))
            {
                return true;
            }
        }
        return false;

    }
    final ArrayList<Rectangle> currentCollidingRectangles=new ArrayList<>();
    @Override
    public void prepareCollision(Vector2 pos, Vector2 direction, Rectangle boundingRect)
    {
        currentCollidingRectangles.clear();
        int x1= (int) (Math.min(boundingRect.x,boundingRect.x+direction.x)/tileWidth);
        int y1= (int) (Math.min(boundingRect.y,boundingRect.y+direction.y)/tileHeight);
        int x2= (int) (Math.min(boundingRect.x+boundingRect.width,boundingRect.x+boundingRect.width+direction.x)/tileWidth);
        int y2= (int) (Math.min(boundingRect.y+boundingRect.height,boundingRect.y+boundingRect.height+direction.y)/tileHeight);

        for(int x=x1;x<=x2;x++)
        {
            for(int y=y1;y<=y2;y++)
            {
                if(x<0||x>=width||y<0||y>=height)
                {
                    continue;
                }
                currentCollidingRectangles.addAll(collision[x][y]);
            }
        }
    }


    Group collisionGroup;
    @Override
    protected void debugCollision(boolean b) {

        if(collisionGroup==null)
        {
            collisionGroup=new Group();

            for (int x = 0; x < collision.length; x++)
            {
                for (int y = 0; y < collision[x].length; y++)
                {
                    for(Rectangle rectangle:collision[x][y])
                    {
                        MapActor collisionActor=new MapActor();
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
        if(b)
        {
            addActor(collisionGroup);
        }
        else
        {
            collisionGroup.remove();
        }

    }
    public void loadMap(TiledMap map,String sourceMap) {

        isInMap=true;
        this.map=map;
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
        collision= new ArrayList[(int) width][(int) height];

        GetPlayer().stop();

        for(MapLayer layer: map.getLayers())
        {
            if(layer.getProperties().containsKey("spriteLayer")&&layer.getProperties().get("spriteLayer",boolean.class))
            {
                spriteLayer=layer;
            }
            if(layer instanceof TiledMapTileLayer)
            {
                loadCollision((TiledMapTileLayer)layer);
            }
            else
            {
                loadObjects(layer,sourceMap);
            }
        }

    }

    private void loadCollision(TiledMapTileLayer layer) {
        for(int x=0;x<layer.getWidth();x++)
        {
            for(int y=0;y<layer.getHeight();y++)
            {
                if(collision[x][y]==null)
                    collision[x][y]=new ArrayList<>();
                ArrayList<Rectangle> map=collision[x][y];
                TiledMapTileLayer.Cell cell=layer.getCell(x,y);
                if(cell==null)
                    continue;
                for(MapObject collision:cell.getTile().getObjects())
                {
                    if(collision  instanceof RectangleMapObject)
                    {
                      Rectangle r=((RectangleMapObject)collision).getRectangle();
                      map.add(new Rectangle((Math.round(layer.getTileWidth()*x)+r.x),(Math.round(layer.getTileHeight()*y)+r.y),Math.round(r.width),Math.round(r.height)));
                    }
                }
            }
        }
    }

    private void loadObjects(MapLayer layer,String sourceMap) {
        player.setMoveModifier(2);
        for (MapObject obj : layer.getObjects()) {

            MapProperties prop=obj.getProperties();
            Object typeObject = prop.get("type");
            if (typeObject != null) {
                String type = prop.get("type",String.class);
                int id = prop.get("id",int.class);
                if(changes.isObjectDeleted(id))
                    continue;
                switch (type) {
                    case "entry":
                        float x=Float.parseFloat(prop.get("x").toString());
                        float y=Float.parseFloat(prop.get("y").toString());
                        float w=Float.parseFloat(prop.get("width").toString());
                        float h=Float.parseFloat(prop.get("height").toString());
                        EntryActor entry=new EntryActor(this,sourceMap,id,prop.get("teleport").toString(),x,y,w,h,prop.get("direction").toString());
                        addMapActor(obj, entry);
                        break;
                    case "enemy":
                        EnemySprite mob=new EnemySprite(id, WorldData.getEnemy(prop.get("enemy").toString()));
                        addMapActor(obj, mob);
                        break;
                    case "inn":
                        addMapActor(obj, new OnCollide(() -> AdventureApplicationAdapter.instance.switchScene(SceneType.InnScene.instance)));
                        break;
                    case "exit":
                        addMapActor(obj, new OnCollide(() -> exit()));
                        break;
                    case "shop":
                        String shopList=prop.get("shopList").toString();
                        List possibleShops=Arrays.asList(shopList.split(","));
                        Array<ShopData> shops;
                        if(possibleShops.size()==0||shopList.equals(""))
                            shops= WorldData.getShopList();
                        else
                        {
                            shops=new Array<>();
                            for(ShopData data:new Array.ArrayIterator<>(WorldData.getShopList()))
                            {
                                if(possibleShops.contains(data.name))
                                {
                                    shops.add(data);
                                }
                            }
                        }
                        if(shops.size==0)
                            continue;

                        ShopData data=shops.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(shops.size));
                        Array<Reward> ret=new Array<Reward>();
                        for(RewardData rdata:new Array.ArrayIterator<>(data.rewards))
                        {
                            ret.addAll(rdata.generate(false));
                        }
                        ShopActor actor=new ShopActor(this,id,ret);
                        addMapActor(obj,actor);
                        if(prop.containsKey("signYOffset")&&prop.containsKey("signXOffset"))
                        {
                            try {
                                TextureSprite sprite=new TextureSprite(Config.instance().getAtlas(data.spriteAtlas).createSprite(data.sprite));
                                sprite.setX(actor.getX()+Float.parseFloat(prop.get("signXOffset").toString()));
                                sprite.setY(actor.getY()+Float.parseFloat(prop.get("signYOffset").toString()));
                                addMapActor(sprite);
                            }
                            catch (Exception e)
                            {
                                System.err.print("Can not create Texture for "+data.sprite+" Obj:"+data);
                            }
                        }
                        break;
                }
            }
        }
    }

    public boolean exit() {

        isInMap=false;
        AdventureApplicationAdapter.instance.switchScene(SceneType.GameScene.instance);
        return true;
    }



    @Override
    public void setWinner(boolean playerWins) {

        if (playerWins) {
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
            startPause(1,()->getReward());
        } else {
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(1,()->
            {

                player.setAnimation(CharacterSprite.AnimationTypes.Idle);
                currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                player.setPosition(oldPosition4);
                Current.player().defeated();
                stop();
                currentMob=null;
            });
        }

    }


    protected void getReward()
    {
        ((RewardScene)SceneType.RewardScene.instance).loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
        currentMob.remove();
        actors.removeValue(currentMob,true);
        changes.deleteObject(currentMob.getId());
        currentMob = null;
        AdventureApplicationAdapter.instance.switchScene(SceneType.RewardScene.instance);
    }
    @Override
    protected void onActing(float delta) {

        oldPosition4.set(oldPosition3);
        oldPosition3.set(oldPosition2);
        oldPosition2.set(oldPosition);
        oldPosition.set(player.pos());
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor.collideWithPlayer(player)) {
                if(actor instanceof EnemySprite)
                {
                    EnemySprite mob=(EnemySprite) actor;
                    currentMob=mob;
                    if(mob.getData().deck==null||mob.getData().deck.isEmpty())
                    {
                        currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
                        startPause(1,()->getReward());
                    }
                    else
                    {
                        player.setAnimation(CharacterSprite.AnimationTypes.Attack);
                        mob.setAnimation(CharacterSprite.AnimationTypes.Attack);

                        startPause(1,()->
                        {
                            ((DuelScene) SceneType.DuelScene.instance).setEnemy(mob);
                            ((DuelScene) SceneType.DuelScene.instance).setPlayer(player);
                            AdventureApplicationAdapter.instance.switchScene(SceneType.DuelScene.instance);
                        });
                    }

                }

            }
        }
    }


    public void setPointOfInterest(PointOfInterestChanges change) {

        changes =change;
    }

    public boolean isInMap() {
        return isInMap;
    }

}
