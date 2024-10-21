package forge.adventure.editor;

import javax.swing.*;
import java.awt.*;

public class QuestTagSelector extends JComponent  {
    DefaultListModel<String> allItems = new DefaultListModel<>();
    DefaultListModel<String> selectedItems = new DefaultListModel<>();
    JList<String> unselectedList;
    JList<String> selectedList;

    boolean useEnemyTags = false;
    boolean usePOITags = false;

    public QuestTagSelector(String title, boolean useEnemyTags, boolean usePOITags)
    {
        if (useEnemyTags){
            this.useEnemyTags = true;
        } else if (usePOITags) {
            this.usePOITags = true;
        }
        else{
            return;
        }

        unselectedList = new JList<>(allItems);
        selectedList = new JList<>(selectedItems);
//        unselectedList.setCellRenderer(new PointOfInterestEditor.PointOfInterestRenderer()); // Replace with use count of tag?
//        selectedList.setCellRenderer(new PointOfInterestEditor.PointOfInterestRenderer());

        JButton addButton=new JButton("add");
        JButton removeButton=new JButton("remove");
        addButton.addActionListener( e -> QuestTagSelector.this.addTag());
        removeButton.addActionListener( e -> QuestTagSelector.this.removeTag());

        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        if (title.length() > 0)
            add(new JLabel(title),BorderLayout.PAGE_START);
        add(new JScrollPane(unselectedList), BorderLayout.LINE_START);
        add(new JScrollPane(selectedList), BorderLayout.LINE_END);

        JPanel buttonPanel = new JPanel();
        GridLayout buttonLayout = new GridLayout(2,0);
        buttonPanel.setLayout(buttonLayout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        add(buttonPanel);
    }

    public void addTag(){
        if (unselectedList.isSelectionEmpty()){
            return;
        }
        for (String toAdd : unselectedList.getSelectedValuesList())
        {
            if (selectedItems.contains(toAdd)) continue;
            selectedItems.addElement(toAdd);
        }
        refresh();
    }

    public void removeTag(){
        if (selectedList.isSelectionEmpty()){
            return;
        }
        for (String toRemove : selectedList.getSelectedValuesList())
        {
            selectedItems.removeElement(toRemove);
        }
        refresh();
    }

    public void load(DefaultListModel<String> selectedNames)
    {
        allItems.clear();
        selectedItems.clear();

        if (useEnemyTags){
            allItems =  QuestController.getInstance().getEnemyTags();
        }
        else if (usePOITags) {
            allItems = QuestController.getInstance().getPOITags();
        }
        unselectedList.setModel(allItems);
        for (int i=0;i<allItems.size();i++){
            if (selectedNames.contains(allItems.get(i))){
                selectedItems.addElement(allItems.get(i));
            }
        }
    }

    private boolean updating=false;

    private void refresh() {
        setEnabled(allItems!=null);
        if(allItems==null)
        {
            return;
        }
        updating=true;

        //unselectedList = new JList<>(allItems);
        //selectedList = new JList<>(selectedItems);
        updating=false;
    }
}
