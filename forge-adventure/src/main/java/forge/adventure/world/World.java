package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.BiomData;
import forge.adventure.data.BiomSpriteData;
import forge.adventure.data.PointOfIntrestData;
import forge.adventure.data.WorldData;
import forge.adventure.scene.Scene;
import forge.adventure.util.Serializer;
import javafx.util.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class World implements Serializable, Disposable {


    private WorldData data;
    private double[][] noiseData;
    private Pixmap biomImage;
    private Pixmap noiseImage;
    private long[][] biomMap;
    private int width;
    private int height;
    private SpritesDataMap mapObjectIds;
    private PointOfIntrestMap mapPoiIds;
    private int tileSize;
    private BiomTexture[] biomTexture;

    static public int highestBiom(long biom) {
        return (int) (Math.log(Long.highestOneBit(biom)) / Math.log(2));
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {


        out.writeObject(data);
        out.writeObject(noiseData);
        Serializer.WritePixmap(out, biomImage);
        Serializer.WritePixmap(out, noiseImage);
        out.writeObject(biomMap);
        out.writeInt(width);
        out.writeInt(height);
        out.writeObject(mapObjectIds);
        out.writeInt(tileSize);
        out.writeObject(biomTexture);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        data = (WorldData) in.readObject();
        noiseData = (double[][]) in.readObject();
        if (biomImage != null) biomImage.dispose();
        biomImage = Serializer.ReadPixmap(in);
        if (noiseImage != null) noiseImage.dispose();
        noiseImage = Serializer.ReadPixmap(in);
        biomMap = (long[][]) in.readObject();
        width = in.readInt();
        height = in.readInt();
        mapObjectIds = (SpritesDataMap) in.readObject();
        tileSize = in.readInt();
        biomTexture = (BiomTexture[]) in.readObject();


    }

    public BiomSpriteData GetObject(int id) {
        return mapObjectIds.get(id);
    }

    public Pixmap GetBiomSprite(int x, int y) {
        if (x < 0 || y <= 0 || x >= width || y > height)
            return new Pixmap(tileSize, tileSize, Pixmap.Format.RGB888);

        long biomIndex = GetBiom(x, y);
        double noise = GetNoise(x, y);
        Pixmap drawingPixmap = new Pixmap(tileSize, tileSize, Pixmap.Format.RGBA8888);
        boolean drawnFirst = false;
        BiomTexture lastFullTile = null;
        int lastFullTileSubIndex = 0;
        for (int i = 0; i < biomTexture.length; i++) {
            if ((biomIndex & 1 << i) == 0) {
                continue;
            }
            BiomTexture regions = biomTexture[i];
            int biomSubIndex = (int) (noise * (double) regions.images.size);
            if (x == 0 || y == 1 || x == width - 1 || y == height)//edge
            {
                return regions.GetPixmapFor(biomSubIndex);
            }


            int neighbors = 0b000_000_000;

            int bitIndex = 8;
            int smallestSubBiom = biomSubIndex;
            int biggestSubBiom = biomSubIndex;
            boolean only1Biom = true;
            for (int ny = 1; ny > -2; ny--) {
                for (int nx = -1; nx < 2; nx++) {
                    long otherBiom = GetBiom(x + nx, y + ny);
                    double othernoise = GetNoise(x + nx, y + ny);
                    int otherbiomSubIndex = (int) (othernoise * (double) regions.images.size);

                    if (smallestSubBiom > otherbiomSubIndex)
                        smallestSubBiom = otherbiomSubIndex;
                    if (biggestSubBiom < otherbiomSubIndex)
                        biggestSubBiom = otherbiomSubIndex;

                    if ((otherBiom & 1 << i) == 0) {
                        only1Biom = false;
                    }
                    if ((otherBiom & 1 << i) != 0 && biomSubIndex <= otherbiomSubIndex) {
                        int bit = 1;
                        bit = bit << bitIndex;
                        neighbors |= bit;

                    }
                    bitIndex--;
                }
            }
            if (neighbors == 0b111_111_111) {
                lastFullTile = regions;
                lastFullTileSubIndex = biomSubIndex;
            } else {
                if (!drawnFirst) {
                    drawnFirst = true;
                    if (lastFullTile == null) {
                        drawingPixmap.drawPixmap(regions.images.get(Math.max(biomSubIndex, 0)).get(BiomTexture.BigPictures.Center.value), 0, 0);
                    } else {
                        drawingPixmap.drawPixmap(lastFullTile.images.get(lastFullTileSubIndex).get(BiomTexture.BigPictures.Center.value), 0, 0);
                    }
                }
                if (only1Biom) {
                    //drawingPixmap.drawPixmap(regions.images.get(Math.min(biomSubIndex,0)).get(BiomTexture.BigPictures.Center.value),0,0);
                    drawingPixmap.drawPixmap(regions.images.get(Math.max(biomSubIndex - 1, 0)).get(BiomTexture.BigPictures.Center.value), 0, 0);
                }
                regions.GetPixmapFor(biomSubIndex, neighbors, drawingPixmap);
            }
        }
        if (!drawnFirst) {
            drawingPixmap.drawPixmap(lastFullTile.images.get(lastFullTileSubIndex).get(BiomTexture.BigPictures.Center.value), 0, 0);
        }
        return drawingPixmap;

    }

    public double GetNoise(int x, int y) {
        return noiseData[x][height - y];
    }

    public long GetBiom(int x, int y) {
        return biomMap[x][height - y];
    }

    public WorldData GetData() {
        return data;
    }

    public World GenerateNew() {

        FileHandle handle = forge.adventure.util.Res.CurrentRes.GetFile("world/world.json");
        String rawJson = handle.readString();
        data = (new Json()).fromJson(WorldData.class, rawJson);
        Random rand = new Random();
        int seed = rand.nextInt();
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);

        double noiceZoom = data.noiceZoomBiom;
        double noiceObjectZoom = data.noiceZoomObject;
        width = data.width;
        height = data.height;
        tileSize = data.tileSize;
        //save at all data
        noiseData = new double[width][height];
        biomMap = new long[width][height];
        Pixmap pix = new Pixmap(width, height, Pixmap.Format.RGB888);
        Pixmap noicePix = new Pixmap(width, height, Pixmap.Format.RGB888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                noiseData[x][y] = (noise.eval(x / (double) width * noiceZoom, y / (double) height * noiceZoom) + 1) / 2;
                noicePix.setColor((float) noiseData[x][y], (float) noiseData[x][y], (float) noiseData[x][y], 1);
                noicePix.drawPixel(x, y);
                biomMap[x][y] = 0;
            }
        }

        pix.setColor(1, 0, 0, 1);
        pix.fill();

        /*for(long x=0;x<sizeX;x++)
        {
            for(long y=0;y<sizeY;y++)
            {
                //value 0-1 based on noise
                ;
                float value=(float)noise.eval((double) x/(double)sizeX*noiceZoom,(double)y/(double)sizeY*noiceZoom);


            }
        }*/
        int biomIndex = -1;
        biomTexture = new BiomTexture[data.GetBioms().size() + 1];
        for (BiomData biom : data.GetBioms()) {

            biomIndex++;
            biomTexture[biomIndex] = new BiomTexture(biom.tileset, data.tileSize, biom.name);
            int biomXStart = (int) Math.round(biom.startPointX * (double) width);
            int biomYStart = (int) Math.round(biom.startPointY * (double) height);
            int biomWidth = (int) Math.round(biom.width * (double) width);
            int biomHeight = (int) Math.round(biom.height * (double) height);

            int beginx = Math.max(biomXStart - biomWidth / 2, 0);
            int beginy = Math.max(biomYStart - biomHeight / 2, 0);
            int endx = Math.min(biomXStart + biomWidth, width);
            int endy = Math.min(biomYStart + biomHeight, height);
            if (biom.width == 1.0 && biom.height == 1.0) {
                beginx = 0;
                beginy = 0;
                endx = width;
                endy = height;
            }
            for (int x = beginx; x < endx; x++) {
                for (int y = beginy; y < endy; y++) {
                    //value 0-1 based on noise
                    double noiseValue = noiseData[x][y];
                    noiseValue *= biom.noiceWeight;
                    //value 0-1 based on dist to origin
                    double distanceValue = (Math.sqrt((x - biomXStart) * (x - biomXStart) + (y - biomYStart) * (y - biomYStart))) / (Math.max(biomWidth, biomHeight) / 2);
                    distanceValue *= biom.distWeight;
                    if (noiseValue + distanceValue < 1.0 || biom.invertHeight && (1 - noiseValue) + distanceValue < 1.0) {
                        Color color = biom.GetColor();
                        float[] hsv = new float[3];
                        color.toHsv(hsv);
                        int count = (int) ((noiseValue - 0.5) * 10 / 4);
                        //hsv[2]+=(count*0.2);
                        color.fromHsv(hsv);
                        pix.setColor(color.r, color.g, color.b, 1);
                        pix.drawPixel(x, y);
                        biomMap[x][y] |= (1 << biomIndex);
                    }

                }
            }
        }

        mapPoiIds = new PointOfIntrestMap(GetChunkSize(), tileSize, data.width / GetChunkSize());
        List<PointOfIntrest> towns = new ArrayList<>();
        List<Rectangle> rectangles = new ArrayList<>();
        for (BiomData biom : data.GetBioms()) {
            for (PointOfIntrestData poi : biom.getPointsOfIntrest()) {
                for (int i = 0; i < poi.count; i++) {
                    for (int counter = 0; counter < 100; counter++)//tries 100 times to find a free point
                    {

                        float radius = (float) Math.sqrt((rand.nextDouble() * poi.radiusFactor) + poi.radiusOffset);
                        float theta = (float) (rand.nextDouble() * 2 * Math.PI);
                        float x = (float) (radius * Math.cos(theta));
                        x *= (biom.width * width / 2);
                        x += (biom.startPointX * width);
                        float y = (float) (radius * Math.sin(theta));
                        y *= (biom.height * height / 2);
                        y += (height - (biom.startPointY * height));
                        y = Math.round(y) * tileSize;
                        x = Math.round(x) * tileSize;
                        boolean breakNextLoop = false;
                        for (Rectangle rect : rectangles) {
                            if (rect.contains(x, y)) {
                                breakNextLoop = true;
                                break;
                            }
                        }
                        if (breakNextLoop)
                            continue;
                        rectangles.add(new Rectangle(x - tileSize * 10, y - tileSize * 10, tileSize * 20, tileSize * 20));
                        PointOfIntrest newPoint = new PointOfIntrest(poi, new Vector2(x, y), rand);

                        mapPoiIds.add(newPoint);

                        if (poi.type.equals("town")) {
                            Color color = biom.GetColor();
                            pix.setColor(color.r, 0.1f, 0.1f, 1);
                            pix.drawRectangle((int) x / tileSize - 5, height - (int) y / tileSize - 5, 10, 10);
                            towns.add(newPoint);
                        }
                        break;
                    }

                }
            }

        }

        //sort towns
        List<Pair<PointOfIntrest, PointOfIntrest>> allSortedTowns = new ArrayList<>();

        for (int i = 0; i < towns.size() - 1; i++) {
            PointOfIntrest current = towns.get(i);
            int smallestIndex = -1;
            float smallestDistance = Float.MAX_VALUE;
            for (int j = i + 1; j < towns.size(); j++) {
                float dist = current.position.dst(towns.get(j).position);
                if (dist < smallestDistance) {
                    smallestDistance = dist;
                    smallestIndex = j;
                }
            }
            if (smallestIndex < 0)
                continue;
            allSortedTowns.add(new Pair<>(current, towns.get(smallestIndex)));
        }

        biomIndex++;
        pix.setColor(1, 1, 1, 1);
        biomTexture[biomIndex] = new BiomTexture(data.roadTileset, data.tileSize, "Road");
        for (Pair<PointOfIntrest, PointOfIntrest> townPair : allSortedTowns) {

            Vector2 currentPoint = townPair.getKey().getTilePosition(tileSize);
            Vector2 endPoint = townPair.getValue().getTilePosition(tileSize);
            for (int x = (int) currentPoint.x - 1; x < currentPoint.x + 2; x++) {
                for (int y = (int) currentPoint.y - 1; y < currentPoint.y + 2; y++) {
                    biomMap[x][height - y] |= (1 << biomIndex);
                    pix.drawPixel(x, y);
                }
            }

            while (!currentPoint.equals(endPoint)) {
                float xDir = endPoint.x - currentPoint.x;
                float yDir = endPoint.y - currentPoint.y;

                if (xDir == 0) {
                    if (yDir > 0)
                        currentPoint.y++;
                    else
                        currentPoint.y--;
                } else if (yDir == 0) {
                    if (xDir > 0)
                        currentPoint.x++;
                    else
                        currentPoint.x--;
                } else if (Math.abs(xDir) > Math.abs(yDir)) {

                    if (xDir > 0)
                        currentPoint.x++;
                    else
                        currentPoint.x--;
                } else {
                    if (yDir > 0)
                        currentPoint.y++;
                    else
                        currentPoint.y--;
                }

                biomMap[(int) currentPoint.x][height - (int) currentPoint.y] |= (1 << biomIndex);
                pix.drawPixel((int) currentPoint.x, height - (int) currentPoint.y);
            }

        }

        mapObjectIds = new SpritesDataMap(GetChunkSize(), tileSize, data.width / GetChunkSize());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int invertedHeight = height - y - 1;
                int currentBiom = highestBiom(biomMap[x][invertedHeight]);
                double spriteNoise = (noise.eval(x / (double) width * noiceObjectZoom, y / (double) invertedHeight * noiceObjectZoom) + 1) / 2;
                if (currentBiom >= data.GetBioms().size())
                    continue;
                BiomData biom = data.GetBioms().get(currentBiom);
                for (String name : biom.spriteNames) {
                    BiomSpriteData sprite = data.GetBiomSprites().GetSpriteData(name);
                    if (spriteNoise >= sprite.startArea && spriteNoise <= sprite.endArea) {
                        if (rand.nextFloat() <= sprite.density) {
                            String spriteKey = sprite.key();
                            int key = -1;
                            if (!mapObjectIds.containsKey(spriteKey)) {

                                key = mapObjectIds.put(sprite.key(), sprite, data.GetBiomSprites());
                            } else {
                                key = mapObjectIds.intKey(spriteKey);
                            }
                            mapObjectIds.putPosition(key, new Vector2((float) x * tileSize + (rand.nextFloat() * tileSize), (float) y * tileSize + (rand.nextFloat() * tileSize)));
                        }
                    }
                }
            }
        }
        noiseImage = noicePix;
        biomImage = pix;

        return this;//new World();
    }

    public int GetWidthInTiles() {
        return width;
    }

    public int GetHeightInTiles() {
        return height;
    }

    public int GetWidthInPixels() {
        return width * tileSize;
    }

    public int GetHeightInPixels() {
        return height * tileSize;
    }

    public int GetWidthInChunks() {
        return width / GetChunkSize();
    }

    public int GetHeightInChunks() {
        return height / GetChunkSize();
    }

    public int GetTileSize() {
        return tileSize;
    }

    public Pixmap getBiomImage() {
        return biomImage;
    }

    public Pixmap getNoiseImage() {
        return noiseImage;
    }

    public List<Pair<Vector2, Integer>> GetMapObjects(int chunkx, int chunky) {
        return mapObjectIds.positions(chunkx, chunky);
    }

    public List<PointOfIntrest> getPointsOfIntrest(Actor player) {
        return mapPoiIds.pointsOfIntrest((int) player.getX() / tileSize / GetChunkSize(), (int) player.getY() / tileSize / GetChunkSize());
    }

    public List<PointOfIntrest> getPointsOfIntrest(int chunkx, int chunky) {
        return mapPoiIds.pointsOfIntrest(chunkx, chunky);
    }

    public int GetChunkSize() {
        return Scene.GetIntendedWidth() / tileSize;
    }

    public void dispose() {

        if (biomImage != null) biomImage.dispose();
        if (noiseImage != null) noiseImage.dispose();
    }

}
