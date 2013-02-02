package forge.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.gui.match.CMatchUI;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.item.CardPrinted;

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
public class DualListBox<T> extends FPanel {
    private FList sourceList;

    private UnsortedListModel<T> sourceListModel;

    private FList destList;

    private UnsortedListModel<T> destListModel;

    private FButton addButton;
    private FButton addAllButton;

    private FButton removeButton;
    private FButton removeAllButton;

    private FButton okButton;
    private FButton autoButton;

    private FLabel orderedLabel;
    private FLabel selectOrder;

    private int remainingObjects = 0;

    private boolean sideboardingMode = false;

    public DualListBox(int remainingObjects, String label, List<T> sourceElements, List<T> destElements,
            Card referenceCard) {
        this.remainingObjects = remainingObjects;
        initScreen();
        orderedLabel.setText(label);
        if (sourceElements != null && !sourceElements.isEmpty()) {
            addSourceElements(sourceElements);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sourceList.setSelectedIndex(0);
                }
            });
        }
        if (destElements != null) {
            addDestinationElements(destElements);
        }
        this.setButtonState();
    }

    public DualListBox(int remainingObjects, String label, List<T> sourceElements, List<T> destElements, Card referenceCard, boolean isSideboardDialog) {
        this(remainingObjects, label, sourceElements, destElements, referenceCard);

        this.sideboardingMode = isSideboardDialog;
        if (sideboardingMode) {
            addAllButton.setVisible(false);
            removeAllButton.setVisible(false);
            autoButton.setEnabled(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceListModel.getSize()));
            orderedLabel.setText(String.format("Main Deck (%d):", destListModel.getSize()));
        }
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

    public void addDestinationElements(List<T> newValue) {
        fillListModel(destListModel, newValue);
    }

    public void addDestinationElements(ListModel newValue) {
        fillListModel(destListModel, newValue);
    }

    @SuppressWarnings("unchecked") // Java 7 has type parameterized ListModel
    private void fillListModel(UnsortedListModel<T> model, ListModel newValues) {
        int size = newValues.getSize();
        for (int i = 0; i < size; i++) {
            model.add((T) newValues.getElementAt(i));
        }
    }

    public void addSourceElements(List<T> newValue) {
        fillListModel(sourceListModel, newValue);
    }

    public void setSourceElements(List<T> newValue) {
        clearSourceListModel();
        addSourceElements(newValue);
    }

    private void fillListModel(UnsortedListModel<T> model, List<T> newValues) {
        model.addAll(newValues);
    }

    private void clearSourceSelected() {
        int[] selected = sourceList.getSelectedIndices();
        for (int i = selected.length - 1; i >= 0; --i) {
            sourceListModel.removeElement(selected[i]);
        }
    }

    private void clearDestinationSelected() {
        int[] selected = destList.getSelectedIndices();
        for (int i = selected.length - 1; i >= 0; --i) {
            destListModel.removeElement(selected[i]);
        }
    }

    public List<T> getOrderedList() {
        this.setVisible(false);
        return destListModel.model;
    }

    public List<T> getRemainingSourceList() {
        return sourceListModel.model;
    }

    private static void showSelectedCard(Object obj) {
        Card card = null;
        if (obj instanceof Card) {
            card = (Card) obj;
        } else if (obj instanceof SpellAbility) {
            card = ((SpellAbility) obj).getSourceCard();
        } else if (obj instanceof CardPrinted) {
            card = ((CardPrinted) obj).getMatchingForgeCard();
        }

        GuiUtils.clearPanelSelections();
        if (card != null) {
            CMatchUI.SINGLETON_INSTANCE.setCard(card);
            GuiUtils.setPanelSelection(card);
        }
    }

    private static void addCardViewListener(final FList list) {
        list.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalRemoved(ListDataEvent e) {
                ListModel model = list.getModel();
                if (0 == model.getSize()) {
                    // nothing left to show
                    return;
                }
                
                int cardIdxPre = e.getIndex0();
                if (model.getSize() <= cardIdxPre) {
                    // the last element got removed, get the one above it
                    --cardIdxPre;
                }
                final int cardIdx = cardIdxPre;
                showSelectedCard(model.getElementAt(cardIdx));
                
                // invoke this later since the list is out of sync with the model
                // at this moment.
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        list.setSelectedIndex(cardIdx);
                    }
                });
            }
            
            @Override
            public void intervalAdded(ListDataEvent e) {
            }
            
            @Override
            public void contentsChanged(ListDataEvent e) {
            }
        });
        
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent ev) {
                showSelectedCard(list.getSelectedValue());
            }
        });
    }

    private void initScreen() {
        setPreferredSize(new Dimension(650, 300));
        setLayout(new GridLayout(0, 3));
        setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        sourceListModel = new UnsortedListModel<T>();
        sourceList = new FList(sourceListModel);

        // Dual List control buttons
        addButton = new FButton(">");
        addButton.addActionListener(new AddListener());
        addAllButton = new FButton(">>");
        addAllButton.addActionListener(new AddAllListener());
        removeButton = new FButton("<");
        removeButton.addActionListener(new RemoveListener());
        removeAllButton = new FButton("<<");
        removeAllButton.addActionListener(new RemoveAllListener());

        // Dual List Complete Buttons
        okButton = new FButton("OK");
        okButton.addActionListener(new OkListener());

        autoButton = new FButton("Auto");
        autoButton.addActionListener(new AutoListener());

        destListModel = new UnsortedListModel<T>();
        destList = new FList(destListModel);

        FPanel leftPanel = new FPanel(new BorderLayout());
        selectOrder = new FLabel.Builder().build();
        selectOrder.setText("Select Order:");
        leftPanel.setSize(300, 300);
        leftPanel.add(selectOrder, BorderLayout.NORTH);
        leftPanel.add(new FScrollPane(sourceList), BorderLayout.CENTER);
        leftPanel.add(okButton, BorderLayout.SOUTH);

        FPanel centerPanel = new FPanel(new GridLayout(5, 1));
        centerPanel.setSize(50, this.getHeight());
        centerPanel.add(addButton);
        centerPanel.add(addAllButton);
        centerPanel.add(removeButton);
        centerPanel.add(removeAllButton);

        orderedLabel = new FLabel.Builder().build();
        orderedLabel.setText("Selected Elements:");

        FPanel rightPanel = new FPanel(new BorderLayout());
        rightPanel.setSize(300, 300);
        rightPanel.add(orderedLabel, BorderLayout.NORTH);
        rightPanel.add(new FScrollPane(destList), BorderLayout.CENTER);
        rightPanel.add(autoButton, BorderLayout.SOUTH);

        add(leftPanel);
        add(centerPanel);
        add(rightPanel);

        addCardViewListener(sourceList);
        addCardViewListener(destList);
    }

    private class AddListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            @SuppressWarnings("unchecked")
            List<T> selected = (List<T>) Arrays.asList(sourceList.getSelectedValues());
            addDestinationElements(selected);
            clearSourceSelected();
            sourceList.validate();
            setButtonState();
        }
    }

    private void addAll() {
        addDestinationElements(sourceListModel);
        clearSourceListModel();
        setButtonState();
    }

    private class AddAllListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            addAll();
            setButtonState();
        }
    }

    private class RemoveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            @SuppressWarnings("unchecked")
            List<T> selected = (List<T>) Arrays.asList(destList.getSelectedValues());
            addSourceElements(selected);
            clearDestinationSelected();
            setButtonState();
        }
    }

    private class RemoveAllListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            addSourceElements(destListModel);
            clearDestinationListModel();
            setButtonState();
        }
    }

    public void setButtonState() {
        if (sideboardingMode) {
            removeAllButton.setVisible(false);
            addAllButton.setVisible(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceListModel.getSize()));
            orderedLabel.setText(String.format("Main Deck (%d):", destListModel.getSize()));
        }
        
        if (remainingObjects != -1) {
            okButton.setEnabled(sourceListModel.getSize() == remainingObjects);
        }
        if (remainingObjects < 1) {
            if (!sideboardingMode) {
                autoButton.setEnabled(sourceListModel.getSize() != remainingObjects);
            } else {
                autoButton.setEnabled(false);
            }
        } else {
            autoButton.setEnabled(false);
        }

        removeButton.setEnabled(destListModel.getSize() != 0);
        removeAllButton.setEnabled(destListModel.getSize() != 0);
        addButton.setEnabled(sourceListModel.getSize() != 0);
        addAllButton.setEnabled(sourceListModel.getSize() != 0);
    }

    private void finishOrdering() {
        System.out.println("Attempting to finish.");
        this.setVisible(false);

        Container grandpa = this.getParent().getParent();
        JDialog dialog = (JDialog) grandpa.getParent();
        dialog.dispose();
    }

    private class OkListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            finishOrdering();
        }
    }

    private class AutoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            addAll();
            finishOrdering();
        }
    }
}
