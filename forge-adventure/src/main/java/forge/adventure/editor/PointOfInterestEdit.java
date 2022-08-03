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
        FormPanel parameters=new FormPanel();
        parameters.setBorder(BorderFactory.createTitledBorder("Parameter"));

        parameters.add("Name:",name);
        parameters.add("Type:",type);
        parameters.add("Count:",count);
        parameters.add("Sprite atlas:",spriteAtlas);
        parameters.add("Sprite:",sprite);
        parameters.add("Map:",map);
        parameters.add("Radius factor:",radiusFactor);

        add(parameters);

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
