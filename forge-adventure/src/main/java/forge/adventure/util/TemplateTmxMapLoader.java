package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.ObjectMap;
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
                for(ObjectMap.Entry<String, String> attr: element.getAttributes())
                {
                    obj.setAttribute(attr.key, attr.value);
                }
                XmlReader.Element properties = element.getChildByName("properties");
                XmlReader.Element templateProperties = obj.getChildByName("properties");
                if (properties != null&&templateProperties!=null)
                {
                    for( XmlReader.Element propertyElements : properties.getChildrenByName("property"))
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
