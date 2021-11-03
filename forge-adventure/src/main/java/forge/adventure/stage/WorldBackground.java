package forge.adventure.stage;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.adventure.world.WorldSave;

import java.awt.*;
import java.util.ArrayList;

/**
 * Background for the over world, will get biome information and create chunks based on the terrain.
 */
public class WorldBackground extends Actor {


    int chunkSize;
    int tileSize;
    int playerX;
    int playerY;

    Texture[][] chunks;
    Texture loadingTexture;
    ArrayList<Actor>[][] chunksSprites;
    ArrayList<Actor>[][] chunksSpritesBackground;
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
            ArrayList<Point> points = new ArrayList<Point>();
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    points.add(new Point(pos.x + x, pos.y + y));
                }
            }
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    Point point = new Point(currentChunkX + x, currentChunkY + y);
                    if (points.contains(point))// old Point is part of new points
                    {
                        points.remove(point);
                    } else {
                        if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                            continue;
                        unLoadChunk(point.x, point.y);
                    }
                }
            }
            for (Point point : points) {
                if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                    continue;
                loadChunk(point.x, point.y);
            }
            currentChunkX = pos.x;
            currentChunkY = pos.y;
        }
        batch.disableBlending();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (pos.y + y < 0 || pos.x + x < 0 || pos.y >= chunks[0].length || pos.x >= chunks.length)
                    continue;


                batch.draw(getChunkTexture(pos.x + x, pos.y + y), transChunkToWorld(pos.x + x), transChunkToWorld(pos.y + y));
            }
        }
        batch.enableBlending();

    }

    private void loadChunk(int x, int y) {
        if (chunksSprites[x][y] == null)
            chunksSprites[x][y] =  MapSprite.GetMapSprites(x, y);

        for (Actor sprite : chunksSprites[x][y]) {
            stage.GetSpriteGroup().addActor(sprite);
        }
        if (chunksSpritesBackground[x][y] == null)
            chunksSpritesBackground[x][y] =  MapSprite.GetMapSpritesBackground(x, y);
        for (Actor sprite : chunksSpritesBackground[x][y]) {
            stage.GetBackgroundSprites().addActor(sprite);
        }
    }

    private void unLoadChunk(int x, int y) {
        ArrayList<Actor> sprites = chunksSprites[x][y];
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

    public Texture getChunkTexture(int x, int y) {
        Texture tex = chunks[x][y];
        if (tex == null) {
            Texture newChunk = new Texture(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGB888);
            for (int cx = 0; cx < chunkSize; cx++) {
                for (int cy = 0; cy < chunkSize; cy++) {
                    newChunk.draw(WorldSave.getCurrentSave().getWorld().getBiomeSprite(cx + chunkSize * x, cy + chunkSize * y), cx * tileSize, (chunkSize * tileSize) - (cy + 1) * tileSize);
                }
            }
            chunks[x][y] = newChunk;
        }
        return chunks[x][y];
    }

    public void initialize() {
        tileSize = WorldSave.getCurrentSave().getWorld().getTileSize();
        chunkSize = WorldSave.getCurrentSave().getWorld().getChunkSize();
        if(chunks!=null)
        {
            stage.GetSpriteGroup().clear();
            for(int i=0;i<chunks.length;i++)
                for(int j=0;j<chunks[i].length;j++)
                    if(chunks[i][j]!=null)
                        chunks[i][j].dispose();
        }
        chunks = new Texture[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        ArrayList[][] createChunks = new ArrayList[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSprites = createChunks;
        ArrayList[][] createSprites = new ArrayList[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSpritesBackground = createSprites;


        if(loadingTexture==null)
        {
            Pixmap loadPix = new Pixmap(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGB565);
            loadPix.setColor(0.5f, 0.5f, 0.5f, 1);
            loadPix.fill();
            loadingTexture = new Texture(loadPix);
        }


        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                Point point = new Point(currentChunkX + x, currentChunkY + y);
                if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                    continue;
                loadChunk(point.x, point.y);
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        initialize();
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
