package forge.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

@SuppressWarnings("serial")
class UnsortedListModel extends AbstractListModel {
    List<Object> model;

    public UnsortedListModel() {
        model = new LinkedList<Object>();
    }

    public int getSize() {
        return model.size();
    }

    public Object getElementAt(int index) {
        return model.toArray()[index];
    }

    public void add(Object element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void addAll(Object elements[]) {
        Collection<Object> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(Object element) {
        return model.contains(element);
    }

    public Iterator iterator() {
        return model.iterator();
    }

    public boolean removeElement(Object element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
}

// An input box for handling the order of choices. 
// Left box has the original choices
// Right box has the final order
// Top string will be like Top of the Stack or Top of the Library
// Bottom string will be like Bottom of the Stack or Bottom of the Library
// Single Arrows in between left box and right box for ordering
// Multi Arrows for moving everything in order 
// Up/down arrows on the right of the right box for swapping
// Single ok button, disabled until left box is empty

@SuppressWarnings("serial")
public class DualListBox extends JPanel {
    private static Object lock = new Object();
    
    private JList sourceList;

    private UnsortedListModel sourceListModel;

    private JList destList;

    private UnsortedListModel destListModel;

    private JButton addButton;
    private JButton addAllButton;

    private JButton removeButton;
    private JButton removeAllButton;
    
    private JButton okButton;
    private JButton autoButton;
    
    private JLabel orderedLabel;

    private boolean selectAll = true;
    
    public DualListBox(boolean selectAll, String label) {
        initScreen();
        orderedLabel.setText(label);
        this.selectAll = selectAll;
    }

    public void clearSourceListModel() {
        sourceListModel.clear();
    }

    public void clearDestinationListModel() {
        destListModel.clear();
    }

    public void addSourceElements(ListModel newValue) {
        fillListModel(sourceListModel, newValue);
    }

    public void setSourceElements(ListModel newValue) {
        clearSourceListModel();
        addSourceElements(newValue);
    }

    public void addDestinationElements(ListModel newValue) {
        fillListModel(destListModel, newValue);
    }

    private void fillListModel(UnsortedListModel model, ListModel newValues) {
        int size = newValues.getSize();
        for (int i = 0; i < size; i++) {
            model.add(newValues.getElementAt(i));
        }
    }

    public void addSourceElements(Object[] newValue) {
        fillListModel(sourceListModel, newValue);
    }

    public void setSourceElements(Object[] newValue) {
        clearSourceListModel();
        addSourceElements(newValue);
    }

    public void addDestinationElements(Object[] newValue) {
        fillListModel(destListModel, newValue);
    }

    private void fillListModel(UnsortedListModel model, Object[] newValues) {
        model.addAll(newValues);
    }

    private void clearSourceSelected() {
        Object[] selected = sourceList.getSelectedValues();
        for (int i = selected.length - 1; i >= 0; --i) {
            sourceListModel.removeElement(selected[i]);
        }
        sourceList.getSelectionModel().clearSelection();
    }

    private void clearDestinationSelected() {
        Object[] selected = destList.getSelectedValues();
        for (int i = selected.length - 1; i >= 0; --i) {
            destListModel.removeElement(selected[i]);
        }
        destList.getSelectionModel().clearSelection();
    }
    
    public List<Object> getOrderedList() {
        this.setVisible(false);
        return destListModel.model;
    }
    
    private void initScreen() {
        setPreferredSize(new Dimension(650, 300));
        setLayout(new GridLayout(0, 3));
        sourceListModel = new UnsortedListModel();
        sourceList = new JList(sourceListModel);

        // Dual List control buttons
        addButton = new JButton(">");
        addButton.addActionListener(new AddListener());
        addAllButton = new JButton(">>");
        addAllButton.addActionListener(new AddAllListener());
        removeButton = new JButton("<");
        removeButton.addActionListener(new RemoveListener());
        removeAllButton = new JButton("<<");
        removeAllButton.addActionListener(new RemoveAllListener());
        
        // Dual List Complete Buttons
        okButton = new JButton("OK");
        okButton.addActionListener(new OkListener());
        okButton.setEnabled(!selectAll);
        autoButton = new JButton("Auto");
        autoButton.addActionListener(new AutoListener());

        destListModel = new UnsortedListModel();
        destList = new JList(destListModel);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setSize(300, 300);
        leftPanel.add(new JLabel("Select Order:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(sourceList), BorderLayout.CENTER);
        leftPanel.add(okButton, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new GridLayout(5, 1));
        centerPanel.setSize(50, this.getHeight());
        centerPanel.add(addButton);
        centerPanel.add(addAllButton);
        centerPanel.add(removeButton);
        centerPanel.add(removeAllButton);
        
        orderedLabel = new JLabel("Selected Elements:");
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setSize(300, 300);
        rightPanel.add(orderedLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(destList), BorderLayout.CENTER);
        rightPanel.add(autoButton, BorderLayout.SOUTH);
        
        add(leftPanel);
        add(centerPanel);
        add(rightPanel);
    }

    private class AddListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object[] selected = sourceList.getSelectedValues();
            addDestinationElements(selected);
            clearSourceSelected();
            sourceList.validate();
            setButtonState();
        }
    }
    
    private void addAll() {
        Iterator itr = sourceListModel.iterator();
        
        ArrayList<Object> objects = new ArrayList<Object>();
        
        while (itr.hasNext()) {
            Object obj = itr.next();
            objects.add(obj);
        }
        
        addDestinationElements(objects.toArray());
        clearSourceListModel();
        setButtonState();
    }
    
    private class AddAllListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            addAll();
            setButtonState();
        }
    }

    private class RemoveListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object[] selected = destList.getSelectedValues();
            addSourceElements(selected);
            clearDestinationSelected();
            setButtonState();
        }
    }
    
    private class RemoveAllListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {   
            Iterator itr = destListModel.iterator();
            
            ArrayList<Object> objects = new ArrayList<Object>();
            
            while (itr.hasNext()) {
                Object obj = itr.next();
                objects.add(obj);
            }
            
            addSourceElements(objects.toArray());
            clearDestinationListModel();
            setButtonState();
        }
    }
    
    public void setButtonState() {
        if (this.selectAll) {
            okButton.setEnabled(sourceListModel.getSize() == 0);
        }
        removeButton.setEnabled(destListModel.getSize() != 0);
        removeAllButton.setEnabled(destListModel.getSize() != 0);
        addButton.setEnabled(sourceListModel.getSize() != 0);
        addAllButton.setEnabled(sourceListModel.getSize() != 0);
        autoButton.setEnabled(sourceListModel.getSize() != 0);
    }
    
    private void finishOrdering() {
        System.out.println("Attempting to finish.");
        this.setVisible(false);
        
        Container grandpa = this.getParent().getParent();
        JDialog dialog = (JDialog)grandpa.getParent();
        dialog.dispose();
    }
    
    private class OkListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            finishOrdering();
        }
    }
    
    private class AutoListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            addAll();
            finishOrdering();
        }
    }
}
