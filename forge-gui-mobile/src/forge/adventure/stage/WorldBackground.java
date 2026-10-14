package forge.adventure.stage;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.world.WorldSave;

/**
 * Background for the over world, will get biome information and create chunks based on the terrain.
 */
public class WorldBackground extends Actor {


    int chunkSize;
    int tileSize;
    int playerX;
    int playerY;

    Texture[][] chunks;
    Texture loadingTexture, t;
    Array<Actor>[][] chunksSprites;
    Array<Actor>[][] chunksSpritesBackground;
    int currentChunkX;
    int currentChunkY;

    GameStage stage;

    private final GridPoint2 playerChunkPos = new GridPoint2();
    private final Array<GridPoint2> chunkPointsList = new Array<>(18);
    private final GridPoint2[] gridPointPool = new GridPoint2[18];

    {
        for (int i = 0; i < gridPointPool.length; i++) {
            gridPointPool[i] = new GridPoint2();
        }
    }

    public WorldBackground(GameStage gameStage) {
        stage = gameStage;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (chunks == null) {
            initialize();
        }
        GridPoint2 pos = translateFromWorldToChunk(playerX, playerY);
        int px = pos.x;
        int py = pos.y;

        if (currentChunkX != px || currentChunkY != py) {
            chunkPointsList.clear();
            int poolIndex = 0;

            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    GridPoint2 pt = gridPointPool[poolIndex++];
                    pt.set(px + x, py + y);
                    chunkPointsList.add(pt);
                }
            }

            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    int oldX = currentChunkX + x;
                    int oldY = currentChunkY + y;

                    boolean remainsVisible = false;
                    for (int i = 0; i < chunkPointsList.size; i++) {
                        GridPoint2 pt = chunkPointsList.get(i);
                        if (pt.x == oldX && pt.y == oldY) {
                            remainsVisible = true;
                            // remove
                            chunkPointsList.removeIndex(i);
                            break;
                        }
                    }

                    if (!remainsVisible) {
                        if (oldY >= 0 && oldX >= 0 && oldY < chunks[0].length && oldX < chunks.length) {
                            unLoadChunk(oldX, oldY);
                        }
                    }
                }
            }

            for (int i = 0; i < chunkPointsList.size; i++) {
                GridPoint2 point = chunkPointsList.get(i);
                if (point.y < 0 || point.x < 0 || point.y >= chunks[0].length || point.x >= chunks.length)
                    continue;
                loadChunk(point.x, point.y);
            }

            currentChunkX = px;
            currentChunkY = py;
        }

        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                int targetX = px + x;
                int targetY = py + y;
                if (targetY < 0 || targetX < 0 || targetY >= chunks[0].length || targetX >= chunks.length)
                    continue;

                batch.draw(getChunkTexture(targetX, targetY), transChunkToWorld(targetX), transChunkToWorld(targetY));
            }
        }
    }

    public void loadChunk(int x, int y) {
        if (chunksSprites[x][y] == null)
            chunksSprites[x][y] = MapSprite.getMapSprites(x, y, MapSprite.SpriteLayer);

        for (Actor sprite : chunksSprites[x][y]) {
            stage.getSpriteGroup().addActor(sprite);
        }
        if (chunksSpritesBackground[x][y] == null)
            chunksSpritesBackground[x][y] = MapSprite.getMapSprites(x, y, MapSprite.BackgroundLayer);
        for (Actor sprite : chunksSpritesBackground[x][y]) {
                stage.getBackgroundSprites().addActor(sprite);
        }
    }

    private void unLoadChunk(int x, int y) {
        Array<Actor> sprites = chunksSprites[x][y];
        if (sprites != null) {
            for (Actor sprite : sprites) {
                stage.getSpriteGroup().removeActor(sprite);
            }
        }
        sprites = chunksSpritesBackground[x][y];
        if (sprites != null) {
            for (Actor sprite : sprites) {
                stage.getBackgroundSprites().removeActor(sprite);
            }
        }
    }

    public Texture getChunkTexture(int x, int y) {
        Texture tex = chunks[x][y];
        if (tex == null) {
            Texture newChunk = new Texture(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGBA8888);
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
        if (chunks != null) {
            stage.getSpriteGroup().clear();
            for (Texture[] chunk : chunks)
                for (Texture texture : chunk)
                    if (texture != null)
                        texture.dispose();
        }
        chunks = new Texture[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        Array[][] createChunks = new Array[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSprites = createChunks;
        Array[][] createSprites = new Array[WorldSave.getCurrentSave().getWorld().getWidthInTiles()][WorldSave.getCurrentSave().getWorld().getHeightInTiles()];
        chunksSpritesBackground = createSprites;


        if (loadingTexture == null) {
            Pixmap loadPix = new Pixmap(chunkSize * tileSize, chunkSize * tileSize, Pixmap.Format.RGBA8888);
            loadPix.setColor(0.5f, 0.5f, 0.5f, 1);
            loadPix.fill();
            loadingTexture = new Texture(loadPix);
        }


        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                GridPoint2 point = new GridPoint2(currentChunkX + x, currentChunkY + y);
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

    GridPoint2 translateFromWorldToChunk(float x, float y) {
        float worldWidthTiles = x / tileSize;
        float worldHeightTiles = y / tileSize;
        playerChunkPos.set((int) worldWidthTiles / chunkSize, (int) worldHeightTiles / chunkSize);
        return playerChunkPos;
    }

    public void setPlayerPos(float x, float y) {

        playerX = (int) x;
        playerY = (int) y;
    }

    public void dispose() {
        if (chunks != null) {
            for (int x = 0; x < chunks.length; x++) {
                for (int y = 0; y < chunks[x].length; y++) {
                    if (chunks[x][y] != null) {
                        chunks[x][y].dispose();
                        chunks[x][y] = null;
                    }
                }
            }
        }
        if (loadingTexture != null) {
            loadingTexture.dispose();
            loadingTexture = null;
        }
        if (t != null) {
            t.dispose();
            t = null;
        }
        chunkPointsList.clear();
    }
}
