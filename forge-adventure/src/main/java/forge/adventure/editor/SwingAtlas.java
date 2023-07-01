package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.awt.Image.SCALE_FAST;


/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class SwingAtlas {

    int imageSize=32;
    HashMap<String, ArrayList<ImageIcon>> images=new  HashMap<>();
    public HashMap<String, ArrayList<ImageIcon>> getImages()
    {
        return images;
    }
    public  SwingAtlas(FileHandle path,int imageSize)
    {
        this.imageSize=imageSize;
        if(!path.exists()||!path.toString().endsWith(".atlas"))
            return;
        TextureAtlas.TextureAtlasData data=new TextureAtlas.TextureAtlasData(path,path.parent(),false);
        for(TextureAtlas.TextureAtlasData.Region region: new Array.ArrayIterator<>(data.getRegions()))
        {
             String name=region.name;
             if(!images.containsKey(name))
             {
                 images.put(name,new ArrayList<>());
             }
            ArrayList<ImageIcon> imageList=images.get(name);
            try
            {
                imageList.add(spriteToImage(region));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    public  SwingAtlas(FileHandle path)
    {
        this(path,32);
    }

    private ImageIcon spriteToImage(TextureAtlas.TextureAtlasData.Region sprite) throws IOException {
        try
        {
            BufferedImage img = ImageIO.read(sprite.page.textureFile.file());
            if(sprite.width== sprite.height)
                return new ImageIcon(img.getSubimage(sprite.left,sprite.top, sprite.width, sprite.height).getScaledInstance(imageSize,imageSize,SCALE_FAST));
            if(sprite.width>sprite.height)
                return new ImageIcon(img.getSubimage(sprite.left,sprite.top, sprite.width, sprite.height).getScaledInstance(imageSize, (int) (imageSize*(sprite.height/(float)sprite.width)),SCALE_FAST));
            return new ImageIcon(img.getSubimage(sprite.left,sprite.top, sprite.width, sprite.height).getScaledInstance((int) (imageSize*(sprite.width/(float)sprite.height)),imageSize,SCALE_FAST));

        }
        catch (IOException e)
        {
            return null;
        }
}

    public ImageIcon get(String name) {
        if(images.get(name).size()==0)
            return null;
        return images.get(name).get(0);
    }

    public boolean has(String name) {
        return images.containsKey(name);
    }

    public ImageIcon getAny() {
        if(images.isEmpty())
            return null;
        ArrayList<ImageIcon> imageList= images.get(images.keySet().iterator().next());
        if(imageList.isEmpty())
            return null;
        return imageList.get(0);
    }
}
