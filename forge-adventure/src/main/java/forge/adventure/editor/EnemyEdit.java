package forge.adventure.editor;

import forge.adventure.data.EnemyData;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EnemyEdit extends FormPanel {
    EnemyData currentData;
    JTextField nameField=new JTextField();
    JTextField nameOverride=new JTextField();
    JTextField colorField=new JTextField();
    JTextField ai=new JTextField();
    JCheckBox flying=new JCheckBox();
    JCheckBox boss=new JCheckBox();
    JCheckBox ignoreDungeonEffect=new JCheckBox();
    FloatSpinner lifeFiled= new FloatSpinner(0, 1000, 1);
    FloatSpinner spawnRate= new FloatSpinner(  0.f, 1, 0.1f);
    FloatSpinner scale= new FloatSpinner(  0.f, 8, 0.1f);
    FloatSpinner difficulty= new FloatSpinner(  0.f, 1, 0.1f);
    FloatSpinner speed= new FloatSpinner( 0.f, 100.f, 1.0f);
    FilePicker deck=new FilePicker(new String[]{"dck","json"});
    FilePicker atlas=new FilePicker(new String[]{"atlas"});
    JTextField equipment=new JTextField();
    RewardsEditor rewards=new RewardsEditor();
    SwingAtlasPreview preview=new SwingAtlasPreview();
    JTextField manualEntry = new JTextField(20);
    private boolean updating=false;

    DefaultListModel<String> existingModel = new DefaultListModel<>();
    DefaultListModel<String> enemyModel = new DefaultListModel<>();
    JList<String> existingTags;
    JList<String> enemyTags;

    public EnemyEdit()
    {
        setLayout(new BorderLayout());
        FormPanel top=new FormPanel() {  };
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
        FormPanel basicInfo=new FormPanel() {  };
        tabs.addTab("Basic Info", basicInfo);

        top.add("Name:",nameField);
        top.add("Display Name:", nameOverride);

        basicInfo.add("Life:",lifeFiled);
        basicInfo.add("Spawn rate:",spawnRate);
        basicInfo.add("Scale:",scale);
        basicInfo.add("Difficulty:",difficulty);
        basicInfo.add("Speed:",speed);
        basicInfo.add("Deck:",deck);
        basicInfo.add("Equipment:",equipment);
        basicInfo.add("Colors:",colorField);

        basicInfo.add("ai:",ai);
        basicInfo.add("flying:",flying);
        basicInfo.add("boss:",boss);

        JPanel visual = new JPanel();

        visual.add("Sprite:",atlas);
        visual.add(preview);

        JPanel tags = new JPanel();
        existingTags = new JList<>();


        existingTags.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"), "addSelected");
        existingTags.getActionMap().put("addSelected", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addSelected();

                int index = existingTags.getSelectedIndex();

                String selectedItem = existingTags.getSelectedValue();

                if (selectedItem != null) {
                    enemyModel.addElement(selectedItem);
                    existingTags.grabFocus();
                    existingTags.setSelectedIndex(index<existingModel.size()?index:index-1);

                }
            }
        });



        existingModel = QuestController.getInstance().getEnemyTags();
        existingTags.setModel(existingModel);




        enemyTags = new JList<>();
        enemyModel = new DefaultListModel<>();
        enemyTags.setModel(enemyModel);

        enemyTags.getModel().addListDataListener(new ListDataListener() {
                                           @Override
                                           public void intervalAdded(ListDataEvent e) {
                                               doUpdate();
                                           }

                                           @Override
                                           public void intervalRemoved(ListDataEvent e) {
                                               doUpdate();
                                           }

                                           @Override
                                           public void contentsChanged(ListDataEvent e) {
                                               doUpdate();
                                           }
                                       });

        JButton select = new JButton("Select");
        select.addActionListener(q -> addSelected());
        JButton add = new JButton("Manual Add");
        add.addActionListener(q -> manualAdd(enemyModel));
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
        JScrollPane listScroller2 = new JScrollPane(enemyTags);
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
                    manualAdd(enemyModel);
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
        tabs.addTab("Sprite", visual);
        tabs.addTab("Rewards", rewards);
        tabs.addTab("Quest Tags", tags);


        equipment.getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        atlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        colorField.getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        ai.getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        flying.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        boss.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        ignoreDungeonEffect.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        nameField.getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        nameOverride.getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        deck.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(EnemyEdit.this::updateEnemy));
        lifeFiled.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        speed.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        scale.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        difficulty.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        spawnRate.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        rewards.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        lifeFiled.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        enemyModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                EnemyEdit.this.updateEnemy();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                EnemyEdit.this.updateEnemy();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        refresh();
    }


    private void doUpdate(){
        EnemyEdit.this.updateEnemy();
    }

    private void addSelected(){
        if (existingTags.getSelectedIndex()>-1)
            enemyModel.addElement(existingTags.getModel().getElementAt(existingTags.getSelectedIndex()));
        doUpdate();
    }

    private void removeSelected(){
        if (enemyTags.getSelectedIndex()>-1)
            enemyModel.remove(enemyTags.getSelectedIndex());
        doUpdate();
    }

    private void filterExisting(DefaultListModel<String> filter){
        DefaultListModel<String> toReturn = new DefaultListModel<>();
        for (Enumeration<String> e = QuestController.getInstance().getEnemyTags().elements(); e.hasMoreElements();){
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
        doUpdate();
    }

    public void updateEnemy() {
        if(currentData==null||updating)
            return;
        currentData.name=nameField.getText();
        currentData.colors=colorField.getText();
        currentData.ai=ai.getText();
        currentData.flying=flying.isSelected();
        currentData.boss=boss.isSelected();
        currentData.life= (int) lifeFiled.getValue();
        currentData.sprite= atlas.getEdit().getText();
        if(equipment.getText().isEmpty())
            currentData.equipment=null;
        else
            currentData.equipment=equipment.getText().split(",");
        currentData.speed=  speed.floatValue();
        currentData.scale=  scale.floatValue();
        currentData.spawnRate=spawnRate.floatValue();
        currentData.difficulty=difficulty.floatValue();
        currentData.deck= deck.getEdit().getText().split(",");
        currentData.rewards= rewards.getRewards();
        preview.setSpritePath(currentData.sprite);

        ArrayList<String> tags = new ArrayList<>();
        for (Enumeration<String> e = enemyModel.elements(); e.hasMoreElements();){
            tags.add(e.nextElement());
        }

        tags.removeIf(q -> q.isEmpty());
        currentData.questTags = tags.toArray(currentData.questTags);
        QuestController.getInstance().refresh();
        filterExisting(enemyModel);
    }

    public void setCurrentEnemy(EnemyData data)
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
        nameField.setText(currentData.name);
        colorField.setText(currentData.colors);
        ai.setText(currentData.ai);
        boss.setSelected(currentData.boss);
        flying.setSelected(currentData.flying);
        lifeFiled.setValue(currentData.life);
        atlas.getEdit().setText(currentData.sprite);
        if(currentData.equipment!=null)
            equipment.setText(String.join(",",currentData.equipment));
        else
            equipment.setText("");
        deck.getEdit().setText(String.join(",",currentData.deck));
        speed.setValue(currentData.speed);
        scale.setValue(currentData.scale);
        spawnRate.setValue(currentData.spawnRate);
        difficulty.setValue(currentData.difficulty);
        rewards.setRewards(currentData.rewards);
        preview.setSpritePath(currentData.sprite);
        enemyModel.clear();
        for(String val : currentData.questTags) {
            if (val != null)
                enemyModel.addElement(val);
        }
        filterExisting(enemyModel);
        updating=false;
    }
}