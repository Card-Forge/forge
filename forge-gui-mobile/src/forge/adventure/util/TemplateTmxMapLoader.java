package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.XmlReader;
import forge.Forge;

import java.io.File;

/**
 * Rewritten the loadObject method of TmxMapLoader to support templates in tiled map.
 *
 */
public class TemplateTmxMapLoader extends TmxMapLoader {

    FileHandle tmxFile;

    @Override
    public TiledMap load(String fileName) {
        TmxMapLoader.Parameters parameter = new TmxMapLoader.Parameters();
        File f = new File(fileName);
        FileHandle tmxFile = new FileHandle(f);

        this.root = xml.parse(tmxFile);

        final Array<FileHandle> textureFiles = getDependencyFileHandles(tmxFile);
        for (FileHandle textureFile : textureFiles) {
            Texture texture = new Texture(textureFile, parameter.generateMipMaps);
            texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter);
            Forge.getAssets().tmxMap.put(textureFile.path(), texture);
        }

        TiledMap map = loadTiledMap(tmxFile, parameter, new ImageResolver.DirectImageResolver(Forge.getAssets().tmxMap));
        map.setOwnedResources(Forge.getAssets().tmxMap.values().toArray());
        return map;
    }

    @Override
    protected Array<FileHandle> getDependencyFileHandles(FileHandle tmxFile) {
        //return super.getDependencyFileHandles(tmxFile);
        Array<FileHandle> fileHandles = new Array<FileHandle>();

        // TileSet descriptors
        for (XmlReader.Element tileset : root.getChildrenByName("tileset")) {
            String source = tileset.getAttribute("source", null);
            if (source != null) {
                FileHandle tsxFile = getRelativeFileHandle(tmxFile, source);
                tileset = xml.parse(tsxFile);
                XmlReader.Element imageElement = tileset.getChildByName("image");
                if (imageElement != null) {
                    String imageSource = tileset.getChildByName("image").getAttribute("source");
                    FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
                    fileHandles.add(image);
                } else {
                    for (XmlReader.Element tile : tileset.getChildrenByName("tile")) {
                        String imageSource = tile.getChildByName("image").getAttribute("source");
                        FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
                        fileHandles.add(image);
                    }
                }
            } else {
                XmlReader.Element imageElement = tileset.getChildByName("image");
                if (imageElement != null) {
                    String imageSource = tileset.getChildByName("image").getAttribute("source");
                    FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
                    fileHandles.add(image);
                } else {
                    for (XmlReader.Element tile : tileset.getChildrenByName("tile")) {
                        String imageSource = tile.getChildByName("image").getAttribute("source");
                        FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
                        fileHandles.add(image);
                    }
                }
            }
        }

        // ImageLayer descriptors
        for (XmlReader.Element imageLayer : root.getChildrenByName("imagelayer")) {
            XmlReader.Element image = imageLayer.getChildByName("image");
            String source = image.getAttribute("source", null);

            if (source != null) {
                FileHandle handle = getRelativeFileHandle(tmxFile, source);
                fileHandles.add(handle);
            }
        }

        return fileHandles;
    }

    @Override
    protected TiledMap loadTiledMap(FileHandle tmxFile, Parameters parameter, ImageResolver imageResolver) {
        this.tmxFile = tmxFile;
        return super.loadTiledMap(tmxFile, parameter, imageResolver);
    }

    @Override
    protected void loadTileSet(XmlReader.Element element, FileHandle tmxFile, ImageResolver imageResolver) {
        //super.loadTileSet(element, tmxFile, imageResolver);
        if (element.getName().equals("tileset")) {
            int firstgid = element.getIntAttribute("firstgid", 1);
            String imageSource = "";
            int imageWidth = 0;
            int imageHeight = 0;
            FileHandle image = null;

            String source = element.getAttribute("source", null);
            if (source != null) {
                FileHandle tsx = getRelativeFileHandle(tmxFile, source);

                try {
                    element = xml.parse(tsx);
                    XmlReader.Element imageElement = element.getChildByName("image");
                    if (imageElement != null) {
                        imageSource = imageElement.getAttribute("source");
                        imageWidth = imageElement.getIntAttribute("width", 0);
                        imageHeight = imageElement.getIntAttribute("height", 0);
                        image = getRelativeFileHandle(tsx, imageSource);
                    }
                } catch (SerializationException e) {
                    throw new GdxRuntimeException("Error parsing external tileset.");
                }
            } else {
                XmlReader.Element imageElement = element.getChildByName("image");
                if (imageElement != null) {
                    imageSource = imageElement.getAttribute("source");
                    imageWidth = imageElement.getIntAttribute("width", 0);
                    imageHeight = imageElement.getIntAttribute("height", 0);
                    image = getRelativeFileHandle(tmxFile, imageSource);
                }
            }
            String name = element.get("name", null);
            int tilewidth = element.getIntAttribute("tilewidth", 0);
            int tileheight = element.getIntAttribute("tileheight", 0);
            int spacing = element.getIntAttribute("spacing", 0);
            int margin = element.getIntAttribute("margin", 0);

            XmlReader.Element offset = element.getChildByName("tileoffset");
            int offsetX = 0;
            int offsetY = 0;
            if (offset != null) {
                offsetX = offset.getIntAttribute("x", 0);
                offsetY = offset.getIntAttribute("y", 0);
            }
            TiledMapTileSet tileSet = new TiledMapTileSet();

            // TileSet
            tileSet.setName(name);
            final MapProperties tileSetProperties = tileSet.getProperties();
            XmlReader.Element properties = element.getChildByName("properties");
            if (properties != null) {
                loadProperties(tileSetProperties, properties);
            }
            tileSetProperties.put("firstgid", firstgid);

            // Tiles
            Array<XmlReader.Element> tileElements = element.getChildrenByName("tile");

            addStaticTiles(tmxFile, imageResolver, tileSet, element, tileElements, name, firstgid, tilewidth, tileheight, spacing,
                    margin, source, offsetX, offsetY, imageSource, imageWidth, imageHeight, image);

            Array<AnimatedTiledMapTile> animatedTiles = new Array<AnimatedTiledMapTile>();

            for (XmlReader.Element tileElement : tileElements) {
                int localtid = tileElement.getIntAttribute("id", 0);
                TiledMapTile tile = tileSet.getTile(firstgid + localtid);
                if (tile != null) {
                    AnimatedTiledMapTile animatedTile = createAnimatedTile(tileSet, tile, tileElement, firstgid);
                    if (animatedTile != null) {
                        animatedTiles.add(animatedTile);
                        tile = animatedTile;
                    }
                    addTileProperties(tile, tileElement);
                    addTileObjectGroup(tile, tileElement);
                }
            }

            // replace original static tiles by animated tiles
            for (AnimatedTiledMapTile animatedTile : animatedTiles) {
                tileSet.putTile(animatedTile.getId(), animatedTile);
            }

            map.getTileSets().addTileSet(tileSet);
        }
    }

    @Override
    protected void loadObject(TiledMap map, MapObjects objects, XmlReader.Element element, float heightInPixels) {
        if (element.getName().equals("object")) {

            if (!element.hasAttribute("template")) {
                super.loadObject(map, objects, element, heightInPixels);
                return;
            }
            String source = element.getAttribute("template");
            FileHandle template = getRelativeFileHandle(tmxFile, source);

            XmlReader.Element el = xml.parse(template);
            for (XmlReader.Element obj : new Array.ArrayIterator<>(el.getChildrenByName("object"))) {
                for(ObjectMap.Entry<String, String> attr: new ObjectMap.Entries<>(element.getAttributes()))
                {
                    obj.setAttribute(attr.key, attr.value);
                }
                XmlReader.Element properties = element.getChildByName("properties");
                XmlReader.Element templateProperties = obj.getChildByName("properties");
                if (properties != null&&templateProperties!=null)
                {
                    for( XmlReader.Element propertyElements : new Array.ArrayIterator<>(properties.getChildrenByName("property")))
                    {
                        templateProperties.addChild(propertyElements);
                    }
                }
                super.loadObject(map, objects, obj, heightInPixels);
                return;
            }
        }
    }
}
