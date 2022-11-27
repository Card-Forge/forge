package forge.adventure.editor;

import com.badlogic.gdx.graphics.Color;
import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeStructureData;
import forge.adventure.util.Config;
import forge.adventure.world.BiomeStructure;
import forge.adventure.world.ColorMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class StructureEditor extends JComponent{
    DefaultListModel<BiomeStructureData> model = new DefaultListModel<>();
    JList<BiomeStructureData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    BiomeStructureEdit edit=new BiomeStructureEdit();

    BiomeData currentData;

    public class StructureDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof BiomeStructureData))
                return label;
            BiomeStructureData structureData=(BiomeStructureData) value;
            label.setText("Structure");
            label.setIcon(new ImageIcon(Config.instance().getFilePath(structureData.sourcePath)));
            return label;
        }
    }
    public void addButton(String name, ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }

    public StructureEditor()
    {

        list.setCellRenderer(new StructureDataRenderer());
        list.addListSelectionListener(e -> StructureEditor.this.updateEdit());
        addButton("add", e -> StructureEditor.this.addStructure());
        addButton("remove", e -> StructureEditor.this.remove());
        addButton("copy", e -> StructureEditor.this.copy());
        addButton("test", e -> StructureEditor.this.test());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(list, BorderLayout.WEST);
        add(toolBar, BorderLayout.NORTH);
        add(edit,BorderLayout.CENTER);


        edit.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                emitChanged();
            }
        });
    }
    protected void emitChanged() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }
    }
    private void test() {
        if (list.isSelectionEmpty())
            return;
        long start = System.currentTimeMillis();
        BiomeStructureData data = model.get(list.getSelectedIndex());

        try
        {

        BiomeStructure struct = new BiomeStructure(data, System.currentTimeMillis(),
                (int) (currentData.width * EditorMainWindow.worldEditor.width.intValue() ),
                (int) (currentData.width * EditorMainWindow.worldEditor.height.intValue()));

            BufferedImage sourceImage= null;
            try {
                sourceImage = ImageIO.read(new File(struct.sourceImagePath()));
            ColorMap sourceColorMap=new ColorMap(sourceImage.getWidth(),sourceImage.getHeight());
            for(int y=0;y<sourceColorMap.getHeight();y++)
                for(int x=0;x<sourceColorMap.getWidth();x++)
                {
                    Color c =new Color();
                    Color.argb8888ToColor(c,sourceImage.getRGB(x,y));
                    sourceColorMap.setColor(x,y,c);
                }


            BufferedImage  maskImage=   ImageIO.read(new File(struct.maskImagePath()));
            ColorMap maskColorMap=new ColorMap(maskImage.getWidth(),maskImage.getHeight());
            for(int y=0;y<maskColorMap.getHeight();y++)
                for(int x=0;x<maskColorMap.getWidth();x++)
                {
                    Color c =new Color();
                    Color.argb8888ToColor(c,maskImage.getRGB(x,y));
                    maskColorMap.setColor(x,y,c);
                }

            struct.initialize(sourceColorMap,maskColorMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        float calcTime=(System.currentTimeMillis() - start)/1000f;

        JLabel label = new JLabel();
        ColorMap colorMap=struct.image;
        BufferedImage image = new BufferedImage(colorMap.getWidth(),colorMap.getHeight(),BufferedImage.TYPE_INT_ARGB);

            for(int y=0;y<colorMap.getHeight();y++)
                for(int x=0;x<colorMap.getWidth();x++)
                    image.setRGB(x,y,Color.argb8888(colorMap.getColor(x,y)));

        if (image.getWidth() < 640 | image.getHeight() < 640) {
            if (image.getHeight() > image.getWidth()) {
                BufferedImage nimage = new BufferedImage(640, 640 * (image.getWidth() / image.getHeight()), BufferedImage.TYPE_INT_ARGB);
                AffineTransform at = new AffineTransform();
                at.scale(640 / image.getHeight(), 640 / image.getHeight());
                AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                image = scaleOp.filter(image, nimage);
            } else {
                BufferedImage nimage = new BufferedImage(640 * (image.getHeight() / image.getWidth()), 640, BufferedImage.TYPE_INT_ARGB);
                AffineTransform at = new AffineTransform();
                at.scale(640 / image.getWidth(), 640 / image.getWidth());
                AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                image = scaleOp.filter(image, nimage);
            }

        }
        label.setIcon(new ImageIcon(image));
        label.setSize(640, 640);



        JOptionPane.showMessageDialog(this, label,"Calculating took "+ calcTime+" seconds",JOptionPane.PLAIN_MESSAGE);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "WaveFunctionCollapse was not successful","can not calculate function "+e.getMessage(),JOptionPane.ERROR_MESSAGE);
        }

    }
    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        BiomeStructureData data=new BiomeStructureData(model.get(selected));
        model.add(model.size(),data);
    }

    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentStructure(model.get(selected),currentData);
    }

    void addStructure()
    {
        BiomeStructureData data=new BiomeStructureData();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }
    public void setStructures(BiomeData data) {

        currentData=data;
        model.clear();
        if(data==null||data.structures==null)
        {
            edit.setCurrentStructure(null,null);
            return;
        }
        for (int i=0;i<data.structures.length;i++) {
            model.add(i,data.structures[i]);
        }
        list.setSelectedIndex(0);
    }

    public BiomeStructureData[] getBiomeStructureData() {

        BiomeStructureData[] rewards= new BiomeStructureData[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            rewards[i]=model.get(i);
        }
        return rewards;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
}
