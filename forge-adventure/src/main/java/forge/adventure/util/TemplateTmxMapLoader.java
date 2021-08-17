package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.XmlReader;

public class TemplateTmxMapLoader extends TmxMapLoader {

    FileHandle tmxFile;

    @Override
    protected TiledMap loadTiledMap(FileHandle tmxFile, TmxMapLoader.Parameters parameter, ImageResolver imageResolver) {
        this.tmxFile = tmxFile;
        return super.loadTiledMap(tmxFile, parameter, imageResolver);
    }

    @Override
    protected void loadObject(TiledMap map, MapObjects objects, XmlReader.Element element, float heightInPixels) {
        if (element.getName().equals("object")) {

            if (!element.hasAttribute("template")) {
                super.loadObject(map, objects, element, heightInPixels);
                return;
            }
            FileHandle template = getRelativeFileHandle(tmxFile, element.getAttribute("template"));
            XmlReader.Element el = xml.parse(template);
            for (XmlReader.Element obj : el.getChildrenByName("object")) {
                obj.setAttribute("x", element.getAttribute("x"));
                obj.setAttribute("y", element.getAttribute("y"));
                obj.setAttribute("id", element.getAttribute("id"));
                super.loadObject(map, objects, obj, heightInPixels);
                return;
            }
        }
    }
}
