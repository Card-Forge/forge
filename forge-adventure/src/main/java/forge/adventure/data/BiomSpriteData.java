package forge.adventure.data;

import java.io.Serializable;

public class BiomSpriteData implements Serializable {
    public String name;
    public double startArea;
    public double endArea;
    public double density;
    public int layer;

    public String key() {
        return "BiomSprite&" + name;
    }
}
