package forge.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
import forge.item.IPaperCard;

// An input box for handling the order of choices.
// Left box has the original choices
// Right box has the final order
// Top string will be like Top of the Stack or Top of the Library
// Bottom string will be like Bottom of the Stack or Bottom of the Library
// Single Arrows in between left box and right box for ordering
// Multi Arrows for moving everything in order
// Up/down arrows on the right of the right box for swapping
// Single ok button, disabled until left box has specified number of items remaining

@SuppressWarnings("serial")
public class DualListBox<T> extends FPanel {
    private final FList sourceList;
    private final UnsortedListModel<T> sourceListModel;

    private final FList destList;
    private final UnsortedListModel<T> destListModel;

    private final FButton addButton;
    private final FButton addAllButton;
    private final FButton removeButton;
    private final FButton removeAllButton;
    private final FButton okButton;
    private final FButton autoButton;

    private final FLabel orderedLabel;
    private final FLabel selectOrder;

    private final int targetRemainingSources;

    private boolean sideboardingMode = false;
    private boolean showCard = true;

    public DualListBox(int remainingSources, List<T> sourceElements, List<T> destElements ) {
        targetRemainingSources = remainingSources;
        sourceListModel = new UnsortedListModel<T>();
        sourceList = new FList(sourceListModel);
        destListModel = new UnsortedListModel<T>();
        destList = new FList(destListModel);
        
        setPreferredSize(new Dimension(650, 300));
        setLayout(new GridLayout(0, 3));
        setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        final Runnable onAdd = new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                List<T> selected = new ArrayList<T>();
                for (Object item : sourceList.getSelectedValues()) {
                    selected.add((T)item);
                }
                addDestinationElements(selected);
                clearSourceSelected();
                sourceList.validate();
                _setButtonState();
            }
        };

        final Runnable onRemove = new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                List<T> selected = new ArrayList<T>();
                for (Object item : destList.getSelectedValues()) {
                    selected.add((T)item);
                }
                clearDestinationSelected();
                addSourceElements(selected);
                _setButtonState();
            }
        };

        sourceList.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                _handleListKey(e, onAdd, destList);
            }
        });
        sourceList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) { onAdd.run(); }
            }
        });
        
        destList.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                _handleListKey(e, onRemove, sourceList);
            }
        });
        destList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) { onRemove.run(); }
            }
        });
        
        // Dual List control buttons
        addButton = new FButton(">");
        addButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) { onAdd.run(); } });
        addAllButton = new FButton(">>");
        addAllButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) { _addAll(); } });
        removeButton = new FButton("<");
        removeButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { onRemove.run(); } });
        removeAllButton = new FButton("<<");
        removeAllButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) { _removeAll(); } });

        // Dual List Complete Buttons
        okButton = new FButton("OK");
        okButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) { _finish(); } });
        autoButton = new FButton("Auto");
        autoButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) { _addAll(); _finish(); } });

        FPanel leftPanel = new FPanel(new BorderLayout());
        selectOrder = new FLabel.Builder().text("Select Order:").build();
        leftPanel.setSize(300, 300);
        leftPanel.add(selectOrder, BorderLayout.NORTH);
        leftPanel.add(new FScrollPane(sourceList), BorderLayout.CENTER);
        leftPanel.add(okButton, BorderLayout.SOUTH);

        FPanel centerPanel = new FPanel(new GridLayout(6, 1));
        centerPanel.setSize(50, this.getHeight());
        centerPanel.add(new FPanel()); // empty panel to take up the first slot
        centerPanel.add(addButton);
        centerPanel.add(addAllButton);
        centerPanel.add(removeButton);
        centerPanel.add(removeAllButton);

        orderedLabel = new FLabel.Builder().build();

        FPanel rightPanel = new FPanel(new BorderLayout());
        rightPanel.setSize(300, 300);
        rightPanel.add(orderedLabel, BorderLayout.NORTH);
        rightPanel.add(new FScrollPane(destList), BorderLayout.CENTER);
        rightPanel.add(autoButton, BorderLayout.SOUTH);

        add(leftPanel);
        add(centerPanel);
        add(rightPanel);

        _addListListeners(sourceList);
        _addListListeners(destList);
        
        if (destElements != null && !destElements.isEmpty()) {
            addDestinationElements(destElements);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    destList.setSelectedIndex(0);
                }
            });
        }
        
        if (sourceElements != null && !sourceElements.isEmpty()) {
            addSourceElements(sourceElements);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sourceList.setSelectedIndex(0);
                }
            });
        }
        
        _setButtonState();
    }
    
    public void setSecondColumnLabelText(String label) {
        orderedLabel.setText(label);
    }
    
    public void setSideboardMode( boolean isSideboardMode) {
        sideboardingMode = isSideboardMode;
        if (sideboardingMode) {
            addAllButton.setVisible(false);
            removeAllButton.setVisible(false);
            autoButton.setEnabled(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceListModel.getSize()));
            orderedLabel.setText(String.format("Main Deck (%d):", destListModel.getSize()));
        }
    }

    private void _handleListKey (KeyEvent e, Runnable onSpace, FList arrowFocusTarget) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_SPACE:
            onSpace.run();
            break;
            
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
            arrowFocusTarget.requestFocusInWindow();
            break;
            
        case KeyEvent.VK_ENTER:
            if (okButton.isEnabled()) {
                okButton.doClick();
            } else if (autoButton.isEnabled()) {
                autoButton.doClick();
            }
            break;
            
        default:
            break;
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

    private void fillListModel(UnsortedListModel<T> model, ListModel newValues) {
        model.addAll(newValues);
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

    private void showSelectedCard(Object obj) {
        if (!showCard || null == obj) {
            return;
        }
        Card card = null;
        if (obj instanceof Card) {
            card = (Card) obj;
        } else if (obj instanceof SpellAbility) {
            card = ((SpellAbility) obj).getSourceCard();
        } else if (obj instanceof CardPrinted) {
            card = ((IPaperCard) obj).getMatchingForgeCard();
        }

        GuiUtils.clearPanelSelections();
        if (card != null) {
            CMatchUI.SINGLETON_INSTANCE.setCard(card);
            GuiUtils.setPanelSelection(card);
        }
    }

    private void _addListListeners(final FList list) {
        list.getModel().addListDataListener(new ListDataListener() {
            int callCount = 0;
            @Override
            public void intervalRemoved(final ListDataEvent e) {
                final int callNum = ++callCount;
                // invoke this later since the list is out of sync with the model
                // at this moment.
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (callNum != callCount) {
                            // don't run stale callbacks
                            return;
                        }
                        
                        ListModel model = list.getModel();
                        if (0 == model.getSize()) {
                            // nothing left to show
                            return;
                        }
                        
                        int cardIdx = e.getIndex0();
                        if (model.getSize() <= cardIdx) {
                            // the last element got removed, get the one above it
                            cardIdx = model.getSize() - 1;
                        }
                        showCard = false;
                        list.setSelectedIndex(cardIdx);
                        showCard = true;
                        showSelectedCard(model.getElementAt(cardIdx));
                        list.requestFocusInWindow();
                    }
                });
            }
            
            @Override
            public void intervalAdded(final ListDataEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // select just-added items so user can undo the add with a single click
                        int startIdx = Math.min(e.getIndex0(), e.getIndex1());
                        int endIdx = Math.max(e.getIndex0(), e.getIndex1());
                        int[] addedIndices = new int[endIdx - startIdx + 1];
                        for (int idx = startIdx; idx <= endIdx; ++idx) {
                            addedIndices[idx - startIdx] = idx;
                        }
                        // attempt to scroll to just-added item (setSelectedIndices does not scroll)
                        // this will scroll to the wrong item if there are other identical items previously in the list
                        showCard = false;
                        list.setSelectedValue(list.getModel().getElementAt(
                                Math.min(endIdx, startIdx + list.getVisibleRowCount())), true);
                        list.setSelectedIndices(addedIndices);
                        showCard = true;
                    }
                });
            }
            
            @Override
            public void contentsChanged(ListDataEvent e) {
            }
        });
        
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent ev) {
                showSelectedCard(list.getSelectedValue());
            }
        });
        
        list.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                showSelectedCard(list.getSelectedValue());
            }
        });
    }

    private void _addAll() {
        addDestinationElements(sourceListModel);
        clearSourceListModel();
        _setButtonState();
    }
    
    private void _removeAll() {
        addSourceElements(destListModel);
        clearDestinationListModel();
        _setButtonState();
    }

    private void _setButtonState() {
        if (sideboardingMode) {
            removeAllButton.setVisible(false);
            addAllButton.setVisible(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceListModel.getSize()));
            orderedLabel.setText(String.format("Main Deck (%d):", destListModel.getSize()));
        }
        
        if (targetRemainingSources != -1) {
            okButton.setEnabled(sourceListModel.getSize() == targetRemainingSources);
        }
        if (targetRemainingSources < 1 && !sideboardingMode) {
            autoButton.setEnabled(sourceListModel.getSize() != targetRemainingSources);
        } else {
            autoButton.setEnabled(false);
        }

        removeButton.setEnabled(destListModel.getSize() != 0);
        removeAllButton.setEnabled(destListModel.getSize() != 0);
        addButton.setEnabled(sourceListModel.getSize() != 0);
        addAllButton.setEnabled(sourceListModel.getSize() != 0);
    }

    private void _finish() {
        this.setVisible(false);

        Container grandpa = this.getParent().getParent();
        JDialog dialog = (JDialog) grandpa.getParent();
        dialog.dispose();
    }
}
