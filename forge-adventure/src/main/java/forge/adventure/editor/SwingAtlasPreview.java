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
    private String sprite="";
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
    int counter=0;
    List<Pair<JLabel,ArrayList<ImageIcon>>> labels=new ArrayList<>();
    public void setSpritePath(String sprite) {

        removeAll();
        counter=0;
        labels.clear();
        if(this.sprite.equals(sprite))
            return;
        this.sprite=sprite;
        SwingAtlas atlas=new SwingAtlas(Config.instance().getFile(sprite));
        for(Map.Entry<String, ArrayList<ImageIcon>> element:atlas.getImages().entrySet())
        {
            JLabel image=new JLabel(element.getValue().get(0));
            add(new JLabel(element.getKey()));
            add(image);
            labels.add(Pair.of(image, element.getValue()));
        }
        timer.restart();
        repaint();
    }
}
