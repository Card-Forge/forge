package forge.adventure.stage;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.world.WorldSave;

import java.awt.*;


public class WorldBackground extends Actor {


    int chunkSize;
    int tileSize;
    int playerX;
    int playerY;

    Texture[][] chunks;
    Texture loadingTextrue;
    Array<Actor>[][] chunksSprites;
    Array<Actor>[][] chunksSpritesBackground;
    int currentChunkX;
    int currentChunkY;

    GameStage stage;

    public WorldBackground(GameStage gameStage) {
        stage = gameStage;
    }

    public void draw(Batch batch, float parentAlpha) {
        if (chunks == null) {
            initialize();
        }
        Point pos = translateFromWorldToChunk(playerX, playerY);
        if (currentChunkX != pos.x || currentChunkY != pos.y) {
            int xDiff = currentChunkX - pos.x;
            int yDiff = currentChunkY - pos.y;
            Array<Point> points = new Array<Point>();
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    points.add(new Point(pos.x + x, pos.y + y));
                }
            }
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    Point point = new Point(currentChunkX + x, currentChunkY + y);
                    if (points.contains(point, false))// old Point is part of new points
                    {
                        points.removeValue(point, false);
                    } else {
                        if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                            continue;
                        UnLoadChunk(point.x, point.y);
                    }
                }
            }
            for (Point point : points) {
                if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                    continue;
                LoadChunk(point.x, point.y);
            }
            currentChunkX = pos.x;
            currentChunkY = pos.y;
        }
        batch.disableBlending();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (pos.y + y < 0 || pos.x + x < 0 || pos.y >= chunks[0].length || pos.x >= chunks.length)
                    continue;


                batch.draw(GetChunkTexture(pos.x + x, pos.y + y), transChunkToWorld(pos.x + x), transChunkToWorld(pos.y + y));
            }
        }
        batch.enableBlending();

    }

    private void LoadChunk(int x, int y) {
        Array<Actor> sprites = chunksSprites[x][y];

        if (sprites == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Array<Actor> sprites = MapSprite.GetMapSprites(x, y);
                    chunksSprites[x][y] = sprites;
                    LoadChunk(x, y);
                }
            }).run();
        } else {

            for (Actor sprite : sprites) {
                stage.GetSpriteGroup().addActor(sprite);
            }
        }
        sprites = chunksSpritesBackground[x][y];

        if (sprites == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Array<Actor> sprites = MapSprite.GetMapSpritesBackground(x, y);
                    chunksSpritesBackground[x][y] = sprites;
                    LoadChunk(x, y);
                }
            }).run();
        } else {

            for (Actor sprite : sprites) {
                stage.GetBackgroundSprites().addActor(sprite);
            }
        }
    }

    private void UnLoadChunk(int x, int y) {
        Array<Actor> sprites = chunksSprites[x][y];
        if (sprites != null) {
            for (Actor sprite : sprites) {
                stage.GetSpriteGroup().removeActor(sprite);
            }
        }
        sprites = chunksSpritesBackground[x][y];
        if (sprites != null) {
            for (Actor sprite : sprites) {
                stage.GetSpriteGroup().removeActor(sprite);
            }
        }
    }

    public Texture GetChunkTexture(int x, int y) {
        Texture tex = chunks[x][y];
        if (tex == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Texture newChunk = new Texture(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGB888);
                    for (int cx = 0; cx < chunkSize; cx++) {
                        for (int cy = 0; cy < chunkSize; cy++) {
                            newChunk.draw(WorldSave.getCurrentSave().getWorld().getBiomSprite(cx + chunkSize * x, cy + chunkSize * y), cx * tileSize, (chunkSize * tileSize) - (cy + 1) * tileSize);
                        }
                    }
                    chunks[x][y] = newChunk;
                }
            }).run();
            /*Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Texture newChunk=new Texture(chunkSize *tileSize, chunkSize *tileSize, Pixmap.Format.RGB888);
                    for(int cx = 0; cx< chunkSize; cx++)
                    {
                        for(int cy = 0; cy< chunkSize; cy++)
                        {
                            newChunk.draw(WorldSave.getCurrentSave().world.GetBiomSprite(cx+chunkSize*x,cy+chunkSize*y),cx*tileSize,(chunkSize*tileSize)-(cy+1)*tileSize);

                            if(cy==0&&cx==0||cy==0&&cx==1||cy==1&&cx==0)
                            {
                                Pixmap pic=new Pixmap(32,32, Pixmap.Format.RGB888);
                                pic.setColor(1.0f,0.0f,0.0f,1.0f);
                                pic.fill();
                                newChunk.draw(pic,cx*tileSize,cy*tileSize);
                            }
                        }
                    }
                    chunks[x][y]= newChunk;
                }

            });*/
            return loadingTextrue;
        }
        return chunks[x][y];
    }

    public void initialize() {
        tileSize = WorldSave.getCurrentSave().getWorld().getTileSize();
        chunkSize = WorldSave.getCurrentSave().getWorld().GetChunkSize();
        chunks = new Texture[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSprites = new Array[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSpritesBackground = new Array[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        Pixmap loadPix = new Pixmap(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGB565);
        loadPix.setColor(0.5f, 0.5f, 0.5f, 1);
        loadPix.fill();
        loadingTextrue = new Texture(loadPix);
    }

    int transChunkToWorld(int xy) {
        return xy * tileSize * chunkSize;
    }

    Point translateFromWorldToChunk(float x, float y) {
        float worldWidthTiles = x / tileSize;
        float worldHeightTiles = y / tileSize;
        return new Point((int) worldWidthTiles / chunkSize, (int) worldHeightTiles / chunkSize);
    }

    public void setPlayerPos(float x, float y) {

        playerX = (int) x;
        playerY = (int) y;
    }
}
