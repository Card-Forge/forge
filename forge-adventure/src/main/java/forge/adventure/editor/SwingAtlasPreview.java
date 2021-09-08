package forge.adventure.editor;

import forge.adventure.util.Config;
import javafx.util.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        SwingAtlas atlas=new SwingAtlas(Config.instance().getFile(sprite));
        for(Map.Entry<String, ArrayList<ImageIcon>> element:atlas.getImages().entrySet())
        {
            JLabel image=new JLabel(element.getValue().get(0));
            add(new JLabel(element.getKey()));
            add(image);
            labels.add(new Pair<>(image, element.getValue()));
        }
        timer.restart();
        repaint();
    }
}
