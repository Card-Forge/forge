package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.*;
import forge.adventure.scene.Scene;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.Serializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Class that will create the world from the configuration
 */
public class World implements  Disposable, SaveFileContent {


    private WorldData data;
    private Pixmap biomeImage;
    private long[][] biomeMap;
    private int[][] terrainMap;
    private int width;
    private int height;
    private SpritesDataMap mapObjectIds;
    private PointOfInterestMap mapPoiIds;
    private BiomeTexture[] biomeTexture;
    private long seed;
    private final Random random = new Random();

    public Random getRandom()
    {
        return random;
    }
    static public int highestBiome(long biome) {
        return (int) (Math.log(Long.highestOneBit(biome)) / Math.log(2));
    }

    @Override
    public void writeToSaveFile(java.io.ObjectOutputStream out) throws IOException {


        Serializer.WritePixmap(out, biomeImage);
        out.writeObject(biomeMap);
        out.writeObject(terrainMap);
        out.writeInt(width);
        out.writeInt(height);
        out.writeObject(mapObjectIds);
        mapPoiIds.writeToSaveFile(out);
        out.writeLong(seed);
    }

    @Override
    public void readFromSaveFile(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        FileHandle handle = Config.instance().getFile(Paths.WORLD);
        String rawJson = handle.readString();
        data = (new Json()).fromJson(WorldData.class, rawJson);

        if (biomeImage != null) biomeImage.dispose();
        biomeImage = Serializer.ReadPixmap(in);
        biomeMap = (long[][]) in.readObject();
        terrainMap = (int[][]) in.readObject();
        width = in.readInt();
        height = in.readInt();
        mapObjectIds = (SpritesDataMap) in.readObject();
        if(mapPoiIds==null)mapPoiIds=new PointOfInterestMap(1,1,1,1);
        mapPoiIds.readFromSaveFile(in);
        seed = in.readLong();

        biomeTexture = new BiomeTexture[data.GetBiomes().size()+1];
        for(int i = 0; i<data.GetBiomes().size(); i++)
        {
            biomeTexture[i] = new BiomeTexture(data.GetBiomes().get(i), data.tileSize);
        }
        biomeTexture[data.GetBiomes().size()] = new BiomeTexture(data.roadTileset, data.tileSize);



    }

    public BiomeSpriteData getObject(int id) {
        return mapObjectIds.get(id);
    }
    private class DrawingInformation {

        private int neighbors;
        private final BiomeTexture regions;
        private final int terrain;

        public DrawingInformation(int neighbors, BiomeTexture regions, int terrain) {

            this.neighbors = neighbors;
            this.regions = regions;
            this.terrain = terrain;
        }

        public void draw(Pixmap drawingPixmap) {
            regions.drawPixmapOn(terrain,neighbors,drawingPixmap);
        }
    }
    public Pixmap getBiomeSprite(int x, int y) {
        if (x < 0 || y <= 0 || x >= width || y > height)
            return new Pixmap(data.tileSize, data.tileSize, Pixmap.Format.RGB888);

        long biomeIndex = getBiome(x, y);
        int terrain = getTerrainIndex(x, y);
        Pixmap drawingPixmap = new Pixmap(data.tileSize, data.tileSize, Pixmap.Format.RGBA8888);
        ArrayList<DrawingInformation> information=new ArrayList<>();
        for (int i = 0; i < biomeTexture.length; i++) {
            if ((biomeIndex & 1L << i) == 0) {
                continue;
            }
            BiomeTexture regions = biomeTexture[i];
            if (x <= 0 || y <= 1 || x >= width - 1 || y >= height)//edge
            {
                return regions.getPixmap(terrain);
            }
            int biomeTerrain=Math.min(regions.images.size()-1,terrain);


            int neighbors = 0b000_000_000;

            int bitIndex = 8;
            for (int ny = 1; ny > -2; ny--) {
                for (int nx = -1; nx < 2; nx++) {
                    long otherBiome = getBiome(x + nx, y + ny);
                    int otherTerrain = getTerrainIndex(x + nx, y + ny);


                    if ((otherBiome & 1L << i) != 0 && biomeTerrain <= otherTerrain)
                        neighbors |= (1 << bitIndex);

                    bitIndex--;
                }
            }
            if(biomeTerrain!=0&&neighbors!=0b111_111_111)
            {
                 bitIndex = 8;
                int baseNeighbors=0;
                for (int ny = 1; ny > -2; ny--) {
                    for (int nx = -1; nx < 2; nx++) {
                        if ((getBiome(x + nx, y + ny) & (1L << i)) != 0 )
                            baseNeighbors |= (1 << bitIndex);
                        bitIndex--;
                    }
                }
                information.add(new DrawingInformation(baseNeighbors,regions,0) );
            }
            information.add(new DrawingInformation(neighbors,regions,biomeTerrain) );

        }
        int lastFullNeighbour=-1;
        int counter=0;
        for(DrawingInformation info:information)
        {
            if(info.neighbors==0b111_111_111)
                lastFullNeighbour= counter;
            counter++;

        }
        counter=0;
        if(lastFullNeighbour<0&&information.size()!=0)
            information.get(0).neighbors=0b111_111_111;
        for(DrawingInformation info:information)
        {
            if(counter<lastFullNeighbour)
            {
                counter++;
                continue;
            }
            info.draw(drawingPixmap);
        }
        return drawingPixmap;

    }

    public int getTerrainIndex(int x, int y) {
        return terrainMap[x][height - y];
    }

    public long getBiome(int x, int y) {
        return biomeMap[x][height - y];
    }

    public WorldData getData() {
        return data;
    }

    public World generateNew(long seed) {

        FileHandle handle = Config.instance().getFile(Paths.WORLD);
        String rawJson = handle.readString();
        data = (new Json()).fromJson(WorldData.class, rawJson);
        if(seed==0)
        {
            seed=random.nextLong();
        }
        this.seed=seed;
        random.setSeed(seed);
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);

        float noiseZoom = data.noiseZoomBiome;
        width = data.width;
        height = data.height;
        //save at all data
        biomeMap = new long[width][height];
        terrainMap= new int[width][height];
        Pixmap pix = new Pixmap(width, height, Pixmap.Format.RGB888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                biomeMap[x][y] = 0;
                terrainMap[x][y] = 0;
            }
        }

        pix.setColor(1, 0, 0, 1);
        pix.fill();

        int biomeIndex = -1;
        biomeTexture = new BiomeTexture[data.GetBiomes().size() + 1];
        for (BiomeData biome : data.GetBiomes()) {

            biomeIndex++;
            biomeTexture[biomeIndex] = new BiomeTexture(biome, data.tileSize);
            int biomeXStart = (int) Math.round(biome.startPointX * (double) width);
            int biomeYStart = (int) Math.round(biome.startPointY * (double) height);
            int biomeWidth = (int) Math.round(biome.width * (double) width);
            int biomeHeight = (int) Math.round(biome.height * (double) height);

            int beginX = Math.max(biomeXStart - biomeWidth / 2, 0);
            int beginY = Math.max(biomeYStart - biomeHeight / 2, 0);
            int endX = Math.min(biomeXStart + biomeWidth, width);
            int endY = Math.min(biomeYStart + biomeHeight, height);
            if (biome.width == 1.0 && biome.height == 1.0) {
                beginX = 0;
                beginY = 0;
                endX = width;
                endY = height;
            }
            for (int x = beginX; x < endX; x++) {
                for (int y = beginY; y < endY; y++) {
                    //value 0-1 based on noise
                    float noiseValue = ((float)noise.eval(x / (float) width * noiseZoom, y / (float) height * noiseZoom) + 1) / 2f;
                    noiseValue *= biome.noiseWeight;
                    //value 0-1 based on dist to origin
                    float distanceValue = ((float)Math.sqrt((x - biomeXStart) * (x - biomeXStart) + (y - biomeYStart) * (y - biomeYStart))) / (Math.max(biomeWidth, biomeHeight) / 2f);
                    distanceValue *= biome.distWeight;
                    if (noiseValue + distanceValue < 1.0 || biome.invertHeight && (1 - noiseValue) + distanceValue < 1.0) {
                        Color color = biome.GetColor();
                        float[] hsv = new float[3];
                        color.toHsv(hsv);
                        int count = (int) ((noiseValue - 0.5) * 10 / 4);
                        //hsv[2]+=(count*0.2);
                        color.fromHsv(hsv);
                        pix.setColor(color.r, color.g, color.b, 1);
                        pix.drawPixel(x, y);
                        biomeMap[x][y] |= (1L << biomeIndex);
                        int terrainCounter=1;
                        if(biome.terrain==null)
                            continue;
                        for(BiomeTerrainData terrain:biome.terrain)
                        {
                            float terrainNoise = ((float)noise.eval(x / (float) width * (noiseZoom*terrain.resolution), y / (float) height * (noiseZoom*terrain.resolution)) + 1) / 2;
                            if(terrainNoise>=terrain.min&&terrainNoise<=terrain.max)
                            {
                                terrainMap[x][y]=terrainCounter;
                            }
                            terrainCounter++;
                        }
                    }

                }
            }
        }

        mapPoiIds = new PointOfInterestMap(getChunkSize(), data.tileSize, data.width / getChunkSize(),data.height / getChunkSize());
        List<PointOfInterest> towns = new ArrayList<>();
        List<Rectangle> otherPoints = new ArrayList<>();
        otherPoints.add(new Rectangle(((float)data.width*data.playerStartPosX*(float)data.tileSize)-data.tileSize*5,((float)data.height*data.playerStartPosY*data.tileSize)-data.tileSize*5,data.tileSize*10,data.tileSize*10));
        int biomeIndex2=-1;
        for (BiomeData biome : data.GetBiomes()) {
            biomeIndex2++;
            for (PointOfInterestData poi : biome.getPointsOfInterest()) {
                for (int i = 0; i < poi.count; i++) {
                    for (int counter = 0; counter < 500; counter++)//tries 100 times to find a free point
                    {
                        if(counter==499)
                        {
                            System.err.print("## Can not place POI "+poi.name+" ##");
                        }
                        float radius = (float) Math.sqrt(((random.nextDouble())/2 * poi.radiusFactor));
                        float theta = (float) (random.nextDouble() * 2 * Math.PI);
                        float x = (float) (radius * Math.cos(theta));
                        x *= (biome.width * width / 2);
                        x += (biome.startPointX * width);
                        float y = (float) (radius * Math.sin(theta));
                        y *= (biome.height * height / 2);
                        y += (height - (biome.startPointY * height));

                        if((int)x<0||(int)y<=0||(int)y>=height||(int)x>=width|| biomeIndex2!= highestBiome(getBiome((int)x,(int)y)))
                        {
                            continue;
                        }

                        x*= data.tileSize;
                        y*= data.tileSize;

                        boolean breakNextLoop = false;
                        for (Rectangle rect : otherPoints) {
                            if (rect.contains(x, y)) {
                                breakNextLoop = true;
                                break;
                            }
                        }
                        if (breakNextLoop)
                            continue;
                        otherPoints.add(new Rectangle(x - data.tileSize * 10, y - data.tileSize * 10, data.tileSize * 20, data.tileSize * 20));
                        PointOfInterest newPoint = new PointOfInterest(poi, new Vector2(x, y), random);

                        mapPoiIds.add(newPoint);


                        Color color = biome.GetColor();
                        pix.setColor(color.r, 0.1f, 0.1f, 1);
                        pix.drawRectangle((int) x / data.tileSize - 5, height - (int) y / data.tileSize - 5, 10, 10);


                        if (poi.type!=null&&poi.type.equals("town")) {
                            towns.add(newPoint);
                        }
                        break;
                    }

                }
            }

        }

        //sort towns
        List<Pair<PointOfInterest, PointOfInterest>> allSortedTowns = new ArrayList<>();//edge is first 32 bits id of first id and last 32 bits id of second

        HashSet<Long> usedEdges=new HashSet<>();
        for (int i = 0; i < towns.size() - 1; i++) {

            PointOfInterest current = towns.get(i);
            int smallestIndex = -1;
            float smallestDistance = Float.MAX_VALUE;
            for (int j = 0; j < towns.size(); j++) {

                if(i==j||usedEdges.contains((long)i|((long)j<<32)))
                    continue;
                float dist = current.position.dst(towns.get(j).position);
                if (dist < smallestDistance) {
                    smallestDistance = dist;
                    smallestIndex = j;
                }
            }
            if (smallestIndex < 0)
                continue;
            if(smallestDistance>data.maxRoadDistance)
                continue;
            usedEdges.add((long)i|((long)smallestIndex<<32));
            usedEdges.add((long)i<<32|((long)smallestIndex));
            allSortedTowns.add(Pair.of(current, towns.get(smallestIndex)));
        }

        biomeIndex++;
        pix.setColor(1, 1, 1, 1);
        biomeTexture[biomeIndex] = new BiomeTexture(data.roadTileset, data.tileSize);
        for (Pair<PointOfInterest, PointOfInterest> townPair : allSortedTowns) {

            Vector2 currentPoint = townPair.getKey().getTilePosition(data.tileSize);
            Vector2 endPoint = townPair.getValue().getTilePosition(data.tileSize);
            for (int x = (int) currentPoint.x - 1; x < currentPoint.x + 2; x++) {
                for (int y = (int) currentPoint.y - 1; y < currentPoint.y + 2; y++) {
                    if(x<0||y<=0||x>=width||y>height)continue;
                    biomeMap[x][height - y] |= (1L << biomeIndex);
                    pix.drawPixel(x, height-y);
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

                if( (int)currentPoint.x<0|| (int)currentPoint.y<=0|| (int)currentPoint.x>=width|| (int)currentPoint.y>height)continue;
                biomeMap[(int) currentPoint.x][height - (int) currentPoint.y] |= (1L << biomeIndex);
                pix.drawPixel((int) currentPoint.x, height - (int) currentPoint.y);
            }

        }

        mapObjectIds = new SpritesDataMap(getChunkSize(), data.tileSize, data.width / getChunkSize());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int invertedHeight = height - y - 1;
                int currentBiome = highestBiome(biomeMap[x][invertedHeight]);
                if (currentBiome >= data.GetBiomes().size())
                    continue;
                BiomeData biome = data.GetBiomes().get(currentBiome);
                for (String name : biome.spriteNames) {
                    BiomeSpriteData sprite = data.GetBiomeSprites().getSpriteData(name);
                    double spriteNoise = (noise.eval(x / (double) width * noiseZoom*sprite.resolution, y / (double) invertedHeight * noiseZoom*sprite.resolution) + 1) / 2;
                    if (spriteNoise >= sprite.startArea && spriteNoise <= sprite.endArea) {
                        if (random.nextFloat() <= sprite.density) {
                            String spriteKey = sprite.key();
                            int key;
                            if (!mapObjectIds.containsKey(spriteKey)) {

                                key = mapObjectIds.put(sprite.key(), sprite, data.GetBiomeSprites());
                            } else {
                                key = mapObjectIds.intKey(spriteKey);
                            }
                            mapObjectIds.putPosition(key, new Vector2((float) x * data.tileSize + (random.nextFloat() * data.tileSize), (float) y * data.tileSize + (random.nextFloat() * data.tileSize)));

                        }
                    }
                }
            }
        }
        biomeImage = pix;

        return this;//new World();
    }

    public int getWidthInTiles() {
        return width;
    }

    public int getHeightInTiles() {
        return height;
    }

    public int getWidthInPixels() {
        return width * data.tileSize;
    }

    public int getHeightInPixels() {
        return height * data.tileSize;
    }

    public int getWidthInChunks() {
        return width / getChunkSize();
    }

    public int getHeightInChunks() {
        return height / getChunkSize();
    }

    public int getTileSize() {
        return data.tileSize;
    }

    public Pixmap getBiomeImage() {
        return biomeImage;
    }


    public List<Pair<Vector2, Integer>> GetMapObjects(int chunkX, int chunkY) {
        return mapObjectIds.positions(chunkX, chunkY);
    }

    public List<PointOfInterest> getPointsOfInterest(Actor player) {
        return mapPoiIds.pointsOfInterest((int) player.getX() / data.tileSize / getChunkSize(), (int) player.getY() / data.tileSize / getChunkSize());
    }

    public List<PointOfInterest> getPointsOfInterest(int chunkX, int chunkY) {
        return mapPoiIds.pointsOfInterest(chunkX, chunkY);
    }

    public int getChunkSize() {
        return Scene.GetIntendedWidth() / data.tileSize;
    }

    public void dispose() {

        if (biomeImage != null) biomeImage.dispose();
    }

    public void setSeed(long seedOffset) {
        random.setSeed(seedOffset+seed);
    }


}
