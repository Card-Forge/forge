package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.*;
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
        parameter.generateMipMaps=true;
        parameter.textureMinFilter = Texture.TextureFilter.Nearest;
        parameter.textureMagFilter = Texture.TextureFilter.Nearest;
        final Array<FileHandle> textureFiles = getDependencyFileHandles(tmxFile);
        for (FileHandle textureFile : textureFiles) {
            Texture texture = new Texture(textureFile, parameter.generateMipMaps);
            texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter);
            Forge.getAssets().tmxMap().put(textureFile.path(), texture);
        }

        TiledMap map = loadTiledMap(tmxFile, parameter, new ImageResolver.DirectImageResolver(Forge.getAssets().tmxMap()));
        map.setOwnedResources(Forge.getAssets().tmxMap().values().toArray());
        return map;
    }

    @Override
    protected Array<FileHandle> getDependencyFileHandles(FileHandle tmxFile) {
        //return super.getDependencyFileHandles(tmxFile);
        Array<FileHandle> fileHandles = new Array<FileHandle>();

        // TileSet descriptors
        for (XmlReader.Element tileset : root.getChildrenByNameRecursively("tileset")) {
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
        for (XmlReader.Element imageLayer : root.getChildrenByNameRecursively("imagelayer")) {
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
    protected void loadObject(TiledMap map, MapObjects objects, XmlReader.Element element, float heightInPixels) {
        if (element.getName().equals("object")) {

            if( element.hasAttribute("class")&& !element.hasAttribute("type"))
                element.setAttribute("type",element.getAttribute("class"));//set type to class value for Tiled 1.9 compatibility
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
