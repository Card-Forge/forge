package forge.adventure.editor;

import forge.adventure.data.PointOfInterestData;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;

public class PointOfInterestEdit extends JComponent {

    PointOfInterestData currentData;


    JTextField  name        = new JTextField();
    JTextField  type        = new JTextField();
    JSpinner    count       = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    FilePicker  spriteAtlas = new FilePicker(new String[]{"atlas"});
    JTextField  sprite      = new JTextField();
    FilePicker  map         = new FilePicker(new String[]{"tmx"});
    JSpinner    radiusFactor= new JSpinner(new SpinnerNumberModel(0.0f, 0.0f, 2.0f, 0.1f));
    SwingAtlasPreview preview=new SwingAtlasPreview(256,2000);

    JTextField manualEntry = new JTextField(20);
    DefaultListModel<String> existingModel = new DefaultListModel<>();
    DefaultListModel<String> POIModel = new DefaultListModel<>();
    JList<String> existingTags;
    JList<String> POITags;

    private boolean updating=false;

    public PointOfInterestEdit()
    {
        JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        FormPanel parameters=new FormPanel();
        //parameters.setLayout(new BoxLayout(parameters, BoxLayout.Y_AXIS));
        parameters.setBorder(BorderFactory.createTitledBorder("Parameter"));

        JPanel tags = new JPanel();

        tabs.addTab("Basic Info", parameters);
        tabs.addTab("Quest Tags", tags);

        parameters.add("Name:",name);
        parameters.add("Type:",type);
        parameters.add("Count:",count);
        parameters.add("Sprite atlas:",spriteAtlas);
        parameters.add("Sprite:",sprite);
        parameters.add("Map:",map);
        parameters.add("Radius factor:",radiusFactor);
        parameters.add(preview);

        name.getDocument().addDocumentListener(new DocumentChangeListener(PointOfInterestEdit.this::updateItem));
        type.getDocument().addDocumentListener(new DocumentChangeListener(PointOfInterestEdit.this::updateItem));
        count.addChangeListener(e -> PointOfInterestEdit.this.updateItem());
        spriteAtlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(PointOfInterestEdit.this::updateItem));
        sprite.getDocument().addDocumentListener(new DocumentChangeListener(PointOfInterestEdit.this::updateItem));
        map.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(PointOfInterestEdit.this::updateItem));
        radiusFactor.addChangeListener(e -> PointOfInterestEdit.this.updateItem());



        existingTags = new JList<>();


        existingTags.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"), "addSelected");
        existingTags.getActionMap().put("addSelected", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = existingTags.getSelectedIndex();

                String selectedItem = existingTags.getSelectedValue();

                if (selectedItem != null) {
                    POIModel.addElement(selectedItem);
                    existingTags.grabFocus();
                    existingTags.setSelectedIndex(index<existingModel.size()?index:index-1);

                }
            }
        });



        existingModel = QuestController.getInstance().getPOITags();
        existingTags.setModel(existingModel);




        POITags = new JList<>();
        POIModel = new DefaultListModel<>();
        POITags.setModel(POIModel);

        POITags.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                updateItem();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                updateItem();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                updateItem();
            }
        });

        JButton select = new JButton("Select");
        select.addActionListener(q -> addSelected());
        JButton add = new JButton("Manual Add");
        add.addActionListener(q -> manualAdd(POIModel));
        JButton remove = new JButton("Remove Item");
        remove.addActionListener(q -> removeSelected());

        tags.setLayout(new BorderLayout());

        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());
        left.add(new JLabel("Tags already in use"), BorderLayout.NORTH);
        JScrollPane listScroller = new JScrollPane(existingTags);
        listScroller.setMinimumSize(new Dimension(400, 800));
        left.add(listScroller, BorderLayout.CENTER);
        tags.add(left, BorderLayout.WEST);

        FormPanel tagEdit = new FormPanel();
        tagEdit.setLayout(new BorderLayout());

        FormPanel mappedTags = new FormPanel();
        mappedTags.setLayout(new BorderLayout());
        mappedTags.add(new JLabel("Tags Mapped to this object"), BorderLayout.NORTH);
        JScrollPane listScroller2 = new JScrollPane(POITags);
        listScroller2.setMinimumSize(new Dimension(400, 800));
        mappedTags.add(listScroller2, BorderLayout.CENTER);
        tagEdit.add(mappedTags,BorderLayout.EAST);

        JPanel controlPanel = new JPanel();

        controlPanel.add(select);
        controlPanel.add(add);
        controlPanel.add(manualEntry);

        manualEntry.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"), "addTyped");
        manualEntry.getActionMap().put("addTyped", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (!manualEntry.getText().trim().isEmpty()) {
                    manualAdd(POIModel);
                    manualEntry.grabFocus();
                }
            }
        });


        controlPanel.add(remove);
        tagEdit.add(controlPanel, BorderLayout.CENTER);
        tags.add(tagEdit,BorderLayout.CENTER);

        JTextArea right1 = new JTextArea("This is really just to pad some space\n" +
                "but also to explain the use of tags.\n" +
                "Rather than adding 100's of object names\n" +
                "to every quest definition, instead we will\n"+
                "categorize enemies and points of interest with\n"+
                "tags and reference those categories in quests");
        right1.setEnabled(false);
        tags.add(right1, BorderLayout.EAST);









        refresh();
    }

    private void updateItem() {
        if(currentData==null||updating)
            return;
        currentData.name=name.getText();
        currentData.type=  type.getText();
        currentData.count= (Integer) count.getValue();
        currentData.spriteAtlas=spriteAtlas.getEdit().getText();
        currentData.sprite=sprite.getText();
        currentData.map=map.getEdit().getText();
        currentData.radiusFactor= (Float) radiusFactor.getValue();

        ArrayList<String> tags = new ArrayList<>();
        for (Enumeration<String> e = POIModel.elements(); e.hasMoreElements();){
            tags.add(e.nextElement());
        }

        currentData.questTags = tags.toArray(currentData.questTags);
        QuestController.getInstance().refresh();
        filterExisting(POIModel);
    }

    public void setCurrent(PointOfInterestData data)
    {
        currentData=data;
        refresh();
    }

    private void addSelected(){
        if (existingTags.getSelectedIndex()>-1)
            POIModel.addElement(existingTags.getModel().getElementAt(existingTags.getSelectedIndex()));
        updateItem();
    }

    private void removeSelected(){
        if (POITags.getSelectedIndex()>-1)
            POIModel.remove(POITags.getSelectedIndex());
        updateItem();
    }

    private void filterExisting(DefaultListModel<String> filter){
        DefaultListModel<String> toReturn = new DefaultListModel<>();
        for (Enumeration<String> e = QuestController.getInstance().getPOITags().elements(); e.hasMoreElements();){
            String toTest = e.nextElement();
            if (toTest != null & !filter.contains(toTest)){
                toReturn.addElement(toTest);
            }
        }
        existingTags.setModel(toReturn);
    }

    private void manualAdd(DefaultListModel<String> model){
        if (!manualEntry.getText().trim().isEmpty())
            model.addElement(manualEntry.getText().trim());
        manualEntry.setText("");
        updateItem();
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

        preview.setSpritePath(currentData.spriteAtlas,currentData.sprite);
        POIModel.clear();
        for(String val : currentData.questTags) {
            if (val != null)
                POIModel.addElement(val);
        }
        filterExisting(POIModel);
        updating=false;
    }
}
