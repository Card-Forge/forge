package forge.adventure.editor;

import forge.adventure.data.PointOfInterestData;

import javax.swing.*;
import java.awt.*;

public class PointOfInterestEdit extends JComponent {

    PointOfInterestData currentData;


    JTextField  name        = new JTextField();
    JTextField  type        = new JTextField();
    JSpinner    count       = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    FilePicker  spriteAtlas = new FilePicker(new String[]{"atlas"});
    JTextField  sprite      = new JTextField();
    FilePicker  map         = new FilePicker(new String[]{"tmx"});
    JSpinner    radiusFactor= new JSpinner(new SpinnerNumberModel(0.0f, 0.0f, 2.0f, 0.1f));


    private boolean updating=false;

    public PointOfInterestEdit()
    {

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        JPanel parameters=new JPanel();
        parameters.setBorder(BorderFactory.createTitledBorder("Parameter"));
        parameters.setLayout(new GridLayout(7,2)) ;

        parameters.add(new JLabel("Name:")); parameters.add(name);
        parameters.add(new JLabel("Type:")); parameters.add(type);
        parameters.add(new JLabel("Count:")); parameters.add(count);
        parameters.add(new JLabel("Sprite atlas:")); parameters.add(spriteAtlas);
        parameters.add(new JLabel("Sprite:")); parameters.add(sprite);
        parameters.add(new JLabel("Map:")); parameters.add(map);
        parameters.add(new JLabel("Radius factor:")); parameters.add(radiusFactor);

        add(parameters);
        add(new Box.Filler(new Dimension(0,0),new Dimension(0,Integer.MAX_VALUE),new Dimension(0,Integer.MAX_VALUE)));

        name.getDocument().addDocumentListener(new DocumentChangeListener(() -> PointOfInterestEdit.this.updateItem()));
        type.getDocument().addDocumentListener(new DocumentChangeListener(() -> PointOfInterestEdit.this.updateItem()));
        count.addChangeListener(e -> PointOfInterestEdit.this.updateItem());
        spriteAtlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> PointOfInterestEdit.this.updateItem()));
        sprite.getDocument().addDocumentListener(new DocumentChangeListener(() -> PointOfInterestEdit.this.updateItem()));
        map.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> PointOfInterestEdit.this.updateItem()));
        radiusFactor.addChangeListener(e -> PointOfInterestEdit.this.updateItem());
        refresh();
    }

    private void updateItem() {
        if(currentData==null||updating)
            return;
        currentData.name=name.getText();
        currentData.type=  type.getText();
        currentData.count= ((Integer)  count.getValue()).intValue();
        currentData.spriteAtlas=spriteAtlas.getEdit().getText();
        currentData.sprite=sprite.getText();
        currentData.map=map.getEdit().getText();
        currentData.radiusFactor=((Float)  radiusFactor.getValue()).floatValue();
    }

    public void setCurrent(PointOfInterestData data)
    {
        currentData=data;
        refresh();
    }

    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        name.setText(currentData.name);
        type.setText(currentData.type);
        count.setValue(currentData.count);
        spriteAtlas.getEdit().setText(currentData.spriteAtlas);
        sprite.setText(currentData.sprite);
        map.getEdit().setText(currentData.map);
        radiusFactor.setValue(currentData.radiusFactor);

        updating=false;
    }
}
