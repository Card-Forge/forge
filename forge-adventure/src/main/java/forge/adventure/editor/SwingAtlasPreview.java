package forge.adventure.editor;

import forge.adventure.util.Config;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class SwingAtlasPreview extends Box {
    int imageSize=32;
    private String sprite="";
    private String spriteName="";
    Timer timer;
    public SwingAtlasPreview() {
        super(BoxLayout.Y_AXIS);
         timer = new Timer(200, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                counter++;
                for (Pair<JLabel, ArrayList<ImageIcon>> element : labels) {
                    element.getKey().setIcon(element.getValue().get(counter % element.getValue().size()));
                }
            }
        });
    }
    public SwingAtlasPreview(int size) {
        this();
        imageSize=size;
    }
    int counter=0;
    List<Pair<JLabel,ArrayList<ImageIcon>>> labels=new ArrayList<>();
    public void setSpritePath(String sprite) {

        setSpritePath(sprite,null);
    }
    public void setSpritePath(String sprite,String name) {
        if(this.sprite==null||sprite==null||(this.sprite.equals(sprite)&&(spriteName!=null&&name!=null&&spriteName.equals(name))))
            return;
        removeAll();
        counter=0;
        labels.clear();
        this.sprite=sprite;
        this.spriteName=name;
        SwingAtlas atlas=new SwingAtlas(Config.instance().getFile(sprite),imageSize);
        int maxCount=0;
        for(Map.Entry<String, ArrayList<ImageIcon>> element:atlas.getImages().entrySet())
        {
            if(name==null||element.getKey().equals(name))
            {
                JLabel image=new JLabel(element.getValue().get(0));
                if(maxCount<element.getValue().size())
                    maxCount=element.getValue().size();
                add(new JLabel(element.getKey()));
                add(image);
                labels.add(Pair.of(image, element.getValue()));
            }
        }
        if(maxCount<=1)
        {
            timer.stop();
        }
        else
        {
            timer.restart();
        }
        doLayout();
        revalidate();
        update(getGraphics());
        repaint();

    }
}
