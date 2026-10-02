package forge.adventure.stage;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;

/**
 * Background for the over world, will get biome information and create chunks based on the terrain.
 * <p>
 * Streams chunks based on what the camera can actually see (zoom included):
 * <ul>
 *   <li>chunk textures are only built for visible chunks, plus a small prefetch ring so they are ready before they scroll in</li>
 *   <li>texture building is time-budgeted per frame, a gray placeholder is drawn for a chunk that is not ready yet</li>
 *   <li>a chunk is only built with a single GPU upload (CPU pixmap first), not one upload per tile</li>
 *   <li>chunk sprites and textures are freed once a chunk is well outside the view (hysteresis avoids rebuild flicker)</li>
 * </ul>
 */
public class WorldBackground extends Actor {

    /** Chunks beyond the visible area whose sprites (actors) get loaded. */
    private static final int LOAD_MARGIN = 1;
    /** Chunks beyond the visible area whose textures get built ahead of time. Must be <= LOAD_MARGIN. */
    private static final int PREFETCH_MARGIN = 1;
    /** Chunks beyond the visible area that are NOT freed yet. Must be > PREFETCH_MARGIN (hysteresis). */
    private static final int KEEP_MARGIN = 2;
    /** Time per frame we may spend building chunk textures (the first build of a frame is always allowed). */
    private static final long BUILD_BUDGET_NANOS = 4_000_000L;

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

    private boolean[][] chunkLoaded;
    // last kept chunk area (starts empty)
    private int keepMinX = Integer.MAX_VALUE, keepMaxX = Integer.MIN_VALUE;
    private int keepMinY = Integer.MAX_VALUE, keepMaxY = Integer.MIN_VALUE;

    /** CPU-side scratch image reused for every chunk build (avoids a native allocation per chunk). */
    private Pixmap scratch;
    private long frameStart;
    private int buildsThisFrame;
    /** After initialize()/dispose() build every visible chunk right away, so no gray frame shows on entering the map. */
    private boolean buildAll = true;

    private final GridPoint2 playerChunkPos = new GridPoint2();

    public WorldBackground(GameStage gameStage) {
        stage = gameStage;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (chunks == null) {
            initialize();
        }
        final float chunkPx = chunkSize * tileSize;
        if (chunkPx <= 0f) {
            return;
        }

        // Real visible world area from the camera (includes zoom).
        Camera cam = stage.getCamera();
        Vector3[] p = cam.frustum.planePoints; // [0] = bottom-left, [2] = top-right for an orthographic camera
        final int viewMinX = (int) Math.floor(p[0].x / chunkPx);
        final int viewMaxX = (int) Math.floor(p[2].x / chunkPx);
        final int viewMinY = (int) Math.floor(p[0].y / chunkPx);
        final int viewMaxY = (int) Math.floor(p[2].y / chunkPx);

        GridPoint2 playerChunk = translateFromWorldToChunk(playerX, playerY);
        currentChunkX = playerChunk.x;
        currentChunkY = playerChunk.y;

        updateChunks(viewMinX, viewMaxX, viewMinY, viewMaxY);

        frameStart = System.nanoTime();
        buildsThisFrame = 0;

        final int maxX = chunks.length - 1;
        final int maxY = chunks[0].length - 1;
        final int minVisX = Math.max(0, viewMinX), maxVisX = Math.min(maxX, viewMaxX);
        final int minVisY = Math.max(0, viewMinY), maxVisY = Math.min(maxY, viewMaxY);

        // 1) visible chunks: build if there is budget, otherwise draw a placeholder this frame
        for (int x = minVisX; x <= maxVisX; x++) {
            for (int y = minVisY; y <= maxVisY; y++) {
                Texture tex = chunks[x][y];
                if (tex == null && canBuild(buildAll)) {
                    tex = buildChunkTexture(x, y);
                }
                if (tex != null) {
                    batch.draw(tex, transChunkToWorld(x), transChunkToWorld(y));
                } else {
                    batch.draw(getPlaceholder(), transChunkToWorld(x), transChunkToWorld(y), chunkPx, chunkPx);
                }
            }
        }
        buildAll = false;

        // 2) prefetch ring just outside the view, only with the remaining budget
        final int preMinX = Math.max(0, viewMinX - PREFETCH_MARGIN), preMaxX = Math.min(maxX, viewMaxX + PREFETCH_MARGIN);
        final int preMinY = Math.max(0, viewMinY - PREFETCH_MARGIN), preMaxY = Math.min(maxY, viewMaxY + PREFETCH_MARGIN);
        for (int x = preMinX; x <= preMaxX; x++) {
            for (int y = preMinY; y <= preMaxY; y++) {
                if (x >= minVisX && x <= maxVisX && y >= minVisY && y <= maxVisY)
                    continue; // already handled above
                if (chunks[x][y] != null)
                    continue;
                if (!canBuild(false))
                    return;
                buildChunkTexture(x, y);
            }
        }
    }

    private boolean canBuild(boolean force) {
        return force || buildsThisFrame == 0 || (System.nanoTime() - frameStart) < BUILD_BUDGET_NANOS;
    }

    private void updateChunks(int vMinX, int vMaxX, int vMinY, int vMaxY) {
        final int maxX = chunks.length - 1;
        final int maxY = chunks[0].length - 1;

        final int nKeepMinX = Math.max(0, vMinX - KEEP_MARGIN), nKeepMaxX = Math.min(maxX, vMaxX + KEEP_MARGIN);
        final int nKeepMinY = Math.max(0, vMinY - KEEP_MARGIN), nKeepMaxY = Math.min(maxY, vMaxY + KEEP_MARGIN);

        // Free chunks that left the keep area (sprites + texture).
        for (int x = keepMinX; x <= keepMaxX; x++) {
            for (int y = keepMinY; y <= keepMaxY; y++) {
                if (x < nKeepMinX || x > nKeepMaxX || y < nKeepMinY || y > nKeepMaxY)
                    unLoadChunk(x, y);
            }
        }

        // Load chunks entering the (smaller) load area.
        final int loadMaxX = Math.min(maxX, vMaxX + LOAD_MARGIN);
        final int loadMaxY = Math.min(maxY, vMaxY + LOAD_MARGIN);
        for (int x = Math.max(0, vMinX - LOAD_MARGIN); x <= loadMaxX; x++) {
            for (int y = Math.max(0, vMinY - LOAD_MARGIN); y <= loadMaxY; y++) {
                if (!chunkLoaded[x][y])
                    loadChunk(x, y);
            }
        }

        keepMinX = nKeepMinX;
        keepMaxX = nKeepMaxX;
        keepMinY = nKeepMinY;
        keepMaxY = nKeepMaxY;
    }

    public void loadChunk(int x, int y) {
        if (chunks == null || x < 0 || y < 0 || x >= chunks.length || y >= chunks[0].length)
            return;

        chunkLoaded[x][y] = true;
        // track the chunk even if it was loaded from outside (e.g. WorldStage.enter)
        keepMinX = Math.min(keepMinX, x);
        keepMaxX = Math.max(keepMaxX, x);
        keepMinY = Math.min(keepMinY, y);
        keepMaxY = Math.max(keepMaxY, y);

        if (chunksSprites[x][y] == null)
            chunksSprites[x][y] = MapSprite.getMapSprites(x, y, MapSprite.SpriteLayer);
        Array<Actor> sprites = chunksSprites[x][y];
        for (int i = 0; i < sprites.size; i++) {
            stage.getSpriteGroup().addActor(sprites.get(i));
        }

        if (chunksSpritesBackground[x][y] == null)
            chunksSpritesBackground[x][y] = MapSprite.getMapSprites(x, y, MapSprite.BackgroundLayer);
        sprites = chunksSpritesBackground[x][y];
        for (int i = 0; i < sprites.size; i++) {
            stage.getBackgroundSprites().addActor(sprites.get(i));
        }
    }

    private void unLoadChunk(int x, int y) {
        if (chunkLoaded[x][y]) {
            chunkLoaded[x][y] = false;

            Array<Actor> sprites = chunksSprites[x][y];
            if (sprites != null) {
                for (int i = 0; i < sprites.size; i++) {
                    stage.getSpriteGroup().removeActor(sprites.get(i));
                }
            }
            sprites = chunksSpritesBackground[x][y];
            if (sprites != null) {
                for (int i = 0; i < sprites.size; i++) {
                    stage.getBackgroundSprites().removeActor(sprites.get(i));
                }
            }
        }
        // the actual leak fix: give the texture back
        if (chunks[x][y] != null) {
            chunks[x][y].dispose();
            chunks[x][y] = null;
        }
    }

    /** Returns the chunk texture, building it first if needed. Kept public for existing callers. */
    public Texture getChunkTexture(int x, int y) {
        Texture tex = chunks[x][y];
        if (tex == null) {
            tex = buildChunkTexture(x, y);
        }
        return tex;
    }

    /**
     * Builds one chunk texture. The tiles are composed on the CPU in a reusable pixmap and uploaded to the GPU once.
     */
    private Texture buildChunkTexture(int x, int y) {
        final int size = chunkSize * tileSize;
        if (scratch == null || scratch.getWidth() != size) {
            if (scratch != null)
                scratch.dispose();
            scratch = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            scratch.setBlending(Pixmap.Blending.None); // overwrite pixels, exactly like Texture.draw() did
        }
        World world = WorldSave.getCurrentSave().getWorld();
        for (int cx = 0; cx < chunkSize; cx++) {
            for (int cy = 0; cy < chunkSize; cy++) {
                scratch.drawPixmap(world.getBiomeSprite(cx + chunkSize * x, cy + chunkSize * y), cx * tileSize, size - (cy + 1) * tileSize);
            }
        }
        Texture newChunk = new Texture(scratch); // does not take ownership of the pixmap
        chunks[x][y] = newChunk;
        buildsThisFrame++;
        return newChunk;
    }

    /** 1x1 gray texture, stretched when drawn. */
    private Texture getPlaceholder() {
        if (loadingTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
            pixmap.fill();
            loadingTexture = new Texture(pixmap);
            pixmap.dispose(); // the old code never disposed its pixmap
        }
        return loadingTexture;
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        // sizes first, so they are valid even if something below fails
        World world = WorldSave.getCurrentSave().getWorld();
        tileSize = world.getTileSize();
        chunkSize = world.getChunkSize();

        if (chunks != null) {
            stage.getSpriteGroup().clear();
            stage.getBackgroundSprites().clear();
            disposeChunkTextures();
        }
        final int width = world.getWidthInTiles();
        final int height = world.getHeightInTiles();
        chunks = new Texture[width][height];
        chunksSprites = new Array[width][height];
        chunksSpritesBackground = new Array[width][height];
        chunkLoaded = new boolean[width][height];

        keepMinX = Integer.MAX_VALUE;
        keepMaxX = Integer.MIN_VALUE;
        keepMinY = Integer.MAX_VALUE;
        keepMaxY = Integer.MIN_VALUE;
        buildAll = true;

        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                loadChunk(currentChunkX + x, currentChunkY + y); // ignores out of range chunks
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
        if (chunkSize <= 0 || tileSize <= 0) {
            World world = WorldSave.getCurrentSave().getWorld();
            tileSize = world.getTileSize();
            chunkSize = world.getChunkSize();
            if (chunkSize <= 0 || tileSize <= 0) {
                return playerChunkPos.set(0, 0);
            }
        }
        float worldWidthTiles = x / tileSize;
        float worldHeightTiles = y / tileSize;
        playerChunkPos.set((int) worldWidthTiles / chunkSize, (int) worldHeightTiles / chunkSize);
        return playerChunkPos;
    }

    public void setPlayerPos(float x, float y) {

        playerX = (int) x;
        playerY = (int) y;
    }

    private void disposeChunkTextures() {
        if (chunks == null)
            return;
        for (int x = 0; x < chunks.length; x++) {
            for (int y = 0; y < chunks[x].length; y++) {
                if (chunks[x][y] != null) {
                    chunks[x][y].dispose();
                    chunks[x][y] = null;
                }
            }
        }
    }

    public void dispose() {
        disposeChunkTextures();
        if (scratch != null) {
            scratch.dispose();
            scratch = null;
        }
        if (loadingTexture != null) {
            loadingTexture.dispose();
            loadingTexture = null;
        }
        if (t != null) {
            t.dispose();
            t = null;
        }
        buildAll = true; // everything visible gets rebuilt on the next draw
    }
}
