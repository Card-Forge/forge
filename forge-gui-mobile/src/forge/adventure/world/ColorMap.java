package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class ColorMap {
    private final int width;
    private final int height;
    private final Color[] data;

    public ColorMap(int w, int h) {
        width = w;
        height = h;
        data = new Color[w * h];
        for (int i = 0; i < w * h; i++)
            data[i] = new Color();
    }

    public ColorMap(FileHandle file, String path) {
        if (file != null && file.exists()) {
            Pixmap pdata = new Pixmap(file);
            width = pdata.getWidth();
            height = pdata.getHeight();
            data = new Color[width * height];
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    data[x + y * width] = new Color(pdata.getPixel(x, y));
                }
            pdata.dispose();
        } else {
            width = 1;
            height = 1;
            data = new Color[1];
            for (int i = 0; i < width * height; i++)
                data[i] = new Color();
            System.err.println("Cannot find file for ColorMap: " + path);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor(int x, int y) {
        return data[x + y * width];
    }

    public void setColor(int x, int y, Color c) {
        data[x + y * width].set(c);
    }
}