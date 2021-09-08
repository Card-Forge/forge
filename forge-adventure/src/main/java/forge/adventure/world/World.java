package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.*;
import forge.adventure.scene.Scene;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.Serializer;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;


public class World implements  Disposable, SaveFileContent {


    private WorldData data;
    private Pixmap biomImage;
    private long[][] biomMap;
    private int[][] terrainMap;
    private int width;
    private int height;
    private SpritesDataMap mapObjectIds;
    private PointOfIntrestMap mapPoiIds;
    private BiomTexture[] biomTexture;
    private long seed;
    private final Random random = new Random();

    public Random getRandom()
    {
        return random;
    }
    static public int highestBiom(long biom) {
        return (int) (Math.log(Long.highestOneBit(biom)) / Math.log(2));
    }

    @Override
    public void writeToSaveFile(java.io.ObjectOutputStream out) throws IOException {


        Serializer.WritePixmap(out, biomImage);
        out.writeObject(biomMap);
        out.writeObject(terrainMap);
        out.writeInt(width);
        out.writeInt(height);
        out.writeObject(mapObjectIds);
        mapPoiIds.writeToSaveFile(out);
        out.writeLong(seed);
    }

    @Override
    public void readFromSaveFile(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        FileHandle handle = Config.instance().getFile(Paths.World);
        String rawJson = handle.readString();
        data = (new Json()).fromJson(WorldData.class, rawJson);

        if (biomImage != null) biomImage.dispose();
        biomImage = Serializer.ReadPixmap(in);
        biomMap = (long[][]) in.readObject();
        terrainMap = (int[][]) in.readObject();
        width = in.readInt();
        height = in.readInt();
        mapObjectIds = (SpritesDataMap) in.readObject();
        if(mapPoiIds==null)mapPoiIds=new PointOfIntrestMap(1,1,1,1);
        mapPoiIds.readFromSaveFile(in);
        seed = in.readLong();

        biomTexture = new BiomTexture[data.GetBioms().size()+1];
        for(int i=0;i<data.GetBioms().size();i++)
        {
            biomTexture[i] = new BiomTexture(data.GetBioms().get(i), data.tileSize);
        }
        biomTexture[data.GetBioms().size()] = new BiomTexture(data.roadTileset, data.tileSize);



    }

    public BiomSpriteData getObject(int id) {
        return mapObjectIds.get(id);
    }
    private class DrawingInformation {

        private int neighbors;
        private final BiomTexture regions;
        private final int terrain;

        public DrawingInformation(int neighbors, BiomTexture regions, int terrain) {

            this.neighbors = neighbors;
            this.regions = regions;
            this.terrain = terrain;
        }

        public void draw(Pixmap drawingPixmap) {
            regions.drawPixmapOn(terrain,neighbors,drawingPixmap);
        }
    }
    public Pixmap getBiomSprite(int x, int y) {
        if (x < 0 || y <= 0 || x >= width || y > height)
            return new Pixmap(data.tileSize, data.tileSize, Pixmap.Format.RGB888);

        long biomIndex = getBiom(x, y);
        int terrain = getTerrainIndex(x, y);
        Pixmap drawingPixmap = new Pixmap(data.tileSize, data.tileSize, Pixmap.Format.RGBA8888);
        Array<DrawingInformation> information=new Array<>();
        for (int i = 0; i < biomTexture.length; i++) {
            if ((biomIndex & 1 << i) == 0) {
                continue;
            }
            BiomTexture regions = biomTexture[i];
            if (x <= 0 || y <= 1 || x >= width - 1 || y >= height)//edge
            {
                return regions.getPixmap(terrain);
            }
            int biomTerrain=Math.min(regions.images.size-1,terrain);


            int neighbors = 0b000_000_000;

            int bitIndex = 8;
            for (int ny = 1; ny > -2; ny--) {
                for (int nx = -1; nx < 2; nx++) {
                    long otherBiom = getBiom(x + nx, y + ny);
                    int otherTerrain = getTerrainIndex(x + nx, y + ny);


                    if ((otherBiom & 1 << i) != 0 && biomTerrain <= otherTerrain)
                        neighbors |= (1 << bitIndex);

                    bitIndex--;
                }
            }
            if(biomTerrain!=0&&neighbors!=0b111_111_111)
            {
                 bitIndex = 8;
                int baseneighbors=0;
                for (int ny = 1; ny > -2; ny--) {
                    for (int nx = -1; nx < 2; nx++) {
                        if ((getBiom(x + nx, y + ny) & (1 << i)) != 0 )
                            baseneighbors |= (1 << bitIndex);
                        bitIndex--;
                    }
                }
                information.add(new DrawingInformation(baseneighbors,regions,0) );
            }
            information.add(new DrawingInformation(neighbors,regions,biomTerrain) );

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
        if(lastFullNeighbour<0&&information.size!=0)
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

    public long getBiom(int x, int y) {
        return biomMap[x][height - y];
    }

    public WorldData getData() {
        return data;
    }

    public World generateNew(long seed) {

        FileHandle handle = Config.instance().getFile(Paths.World);
        String rawJson = handle.readString();
        data = (new Json()).fromJson(WorldData.class, rawJson);
        if(seed==0)
        {
            seed=random.nextLong();
        }
        this.seed=seed;
        random.setSeed(seed);
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);

        double noiseZoom = data.noiseZoomBiom;
        width = data.width;
        height = data.height;
        //save at all data
        biomMap = new long[width][height];
        terrainMap= new int[width][height];
        Pixmap pix = new Pixmap(width, height, Pixmap.Format.RGB888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                biomMap[x][y] = 0;
                terrainMap[x][y] = 0;
            }
        }

        pix.setColor(1, 0, 0, 1);
        pix.fill();

        int biomIndex = -1;
        biomTexture = new BiomTexture[data.GetBioms().size() + 1];
        for (BiomData biom : data.GetBioms()) {

            biomIndex++;
            biomTexture[biomIndex] = new BiomTexture(biom, data.tileSize);
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
                    double noiseValue = (noise.eval(x / (double) width * noiseZoom, y / (double) height * noiseZoom) + 1) / 2;
                    noiseValue *= biom.noiseWeight;
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
                        int terrainCounter=1;
                        if(biom.terrain==null)
                            continue;
                        for(BiomTerrainData terrain:biom.terrain)
                        {
                            double terrainNoise = (noise.eval(x / (double) width * (noiseZoom*terrain.resolution), y / (double) height * (noiseZoom*terrain.resolution)) + 1) / 2;
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

        mapPoiIds = new PointOfIntrestMap(GetChunkSize(), data.tileSize, data.width / GetChunkSize(),data.height / GetChunkSize());
        List<PointOfInterest> towns = new ArrayList<>();
        List<Rectangle> otherPoints = new ArrayList<>();
        otherPoints.add(new Rectangle(((float)data.width*data.playerStartPosX*(float)data.tileSize)-data.tileSize*5,((float)data.height*data.playerStartPosY*data.tileSize)-data.tileSize*5,data.tileSize*10,data.tileSize*10));
        int biomIndex2=-1;
        for (BiomData biom : data.GetBioms()) {
            biomIndex2++;
            for (PointOfInterestData poi : biom.getPointsOfIntrest()) {
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
                        x *= (biom.width * width / 2);
                        x += (biom.startPointX * width);
                        float y = (float) (radius * Math.sin(theta));
                        y *= (biom.height * height / 2);
                        y += (height - (biom.startPointY * height));

                        /*
                        float x = biom.startPointX+(float)(random.nextFloat() *(biom.width*poi.radiusFactor) )-((biom.width*poi.radiusFactor)/2f);
                        float y = biom.startPointY+(float)(random.nextFloat() *(biom.height*poi.radiusFactor) )-((biom.height*poi.radiusFactor)/2f);
                        x*=width;
                        y*=height;
                        y=height-y;*/
                        if((int)x<0||(int)y<=0||(int)y>=height||(int)x>=width|| biomIndex2!=highestBiom(getBiom((int)x,(int)y)))
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

                        /*
                        Color color = biom.GetColor();
                        pix.setColor(color.r, 0.1f, 0.1f, 1);
                        pix.drawRectangle((int) x / data.tileSize - 5, height - (int) y / data.tileSize - 5, 10, 10);
                        */

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
            allSortedTowns.add(new Pair<>(current, towns.get(smallestIndex)));
        }

        biomIndex++;
        pix.setColor(1, 1, 1, 1);
        biomTexture[biomIndex] = new BiomTexture(data.roadTileset, data.tileSize);
        for (Pair<PointOfInterest, PointOfInterest> townPair : allSortedTowns) {

            Vector2 currentPoint = townPair.getKey().getTilePosition(data.tileSize);
            Vector2 endPoint = townPair.getValue().getTilePosition(data.tileSize);
            for (int x = (int) currentPoint.x - 1; x < currentPoint.x + 2; x++) {
                for (int y = (int) currentPoint.y - 1; y < currentPoint.y + 2; y++) {
                    if(x<0||y<=0||x>=width||y>height)continue;
                    biomMap[x][height - y] |= (1 << biomIndex);
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
                biomMap[(int) currentPoint.x][height - (int) currentPoint.y] |= (1 << biomIndex);
                pix.drawPixel((int) currentPoint.x, height - (int) currentPoint.y);
            }

        }

        mapObjectIds = new SpritesDataMap(GetChunkSize(), data.tileSize, data.width / GetChunkSize());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int invertedHeight = height - y - 1;
                int currentBiom = highestBiom(biomMap[x][invertedHeight]);
                if (currentBiom >= data.GetBioms().size())
                    continue;
                BiomData biom = data.GetBioms().get(currentBiom);
                for (String name : biom.spriteNames) {
                    BiomSpriteData sprite = data.GetBiomSprites().GetSpriteData(name);
                    double spriteNoise = (noise.eval(x / (double) width * noiseZoom*sprite.resolution, y / (double) invertedHeight * noiseZoom*sprite.resolution) + 1) / 2;
                    if (spriteNoise >= sprite.startArea && spriteNoise <= sprite.endArea) {
                        if (random.nextFloat() <= sprite.density) {
                            String spriteKey = sprite.key();
                            int key = -1;
                            if (!mapObjectIds.containsKey(spriteKey)) {

                                key = mapObjectIds.put(sprite.key(), sprite, data.GetBiomSprites());
                            } else {
                                key = mapObjectIds.intKey(spriteKey);
                            }
                            mapObjectIds.putPosition(key, new Vector2((float) x * data.tileSize + (random.nextFloat() * data.tileSize), (float) y * data.tileSize + (random.nextFloat() * data.tileSize)));
                            continue;
                        }
                    }
                }
            }
        }
        biomImage = pix;

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
        return width / GetChunkSize();
    }

    public int getHeightInChunks() {
        return height / GetChunkSize();
    }

    public int getTileSize() {
        return data.tileSize;
    }

    public Pixmap getBiomImage() {
        return biomImage;
    }


    public List<Pair<Vector2, Integer>> GetMapObjects(int chunkx, int chunky) {
        return mapObjectIds.positions(chunkx, chunky);
    }

    public List<PointOfInterest> getPointsOfIntrest(Actor player) {
        return mapPoiIds.pointsOfIntrest((int) player.getX() / data.tileSize / GetChunkSize(), (int) player.getY() / data.tileSize / GetChunkSize());
    }

    public List<PointOfInterest> getPointsOfIntrest(int chunkx, int chunky) {
        return mapPoiIds.pointsOfIntrest(chunkx, chunky);
    }

    public int GetChunkSize() {
        return Scene.GetIntendedWidth() / data.tileSize;
    }

    public void dispose() {

        if (biomImage != null) biomImage.dispose();
    }

    public void setSeed(long seedOffset) {
        random.setSeed(seedOffset+seed);
    }


}
