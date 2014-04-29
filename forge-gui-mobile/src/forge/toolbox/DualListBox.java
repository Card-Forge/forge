package forge.toolbox;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Callback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

// An input box for handling the order of choices.
// Left box has the original choices
// Right box has the final order
// Top string will be like Top of the Stack or Top of the Library
// Bottom string will be like Bottom of the Stack or Bottom of the Library
// Single Arrows in between left box and right box for ordering
// Multi Arrows for moving everything in order
// Up/down arrows on the right of the right box for swapping
// Single ok button, disabled until left box has specified number of items remaining
public class DualListBox<T> extends FDialog {
    private final ChoiceList sourceList;
    private final ChoiceList destList;

    private final FButton addButton;
    private final FButton addAllButton;
    private final FButton removeButton;
    private final FButton removeAllButton;
    private final FButton okButton;
    private final FButton autoButton;

    private final FLabel orderedLabel;
    private final FLabel selectOrder;

    private final int targetRemainingSourcesMin;
    private final int targetRemainingSourcesMax;

    private boolean sideboardingMode = false;

    public DualListBox(String title, int remainingSources, List<T> sourceElements, List<T> destElements, final Callback<List<T>> callback) {
        this(title, remainingSources, remainingSources, sourceElements, destElements, callback);
    }
    
    public DualListBox(String title, int remainingSourcesMin, int remainingSourcesMax, List<T> sourceElements, List<T> destElements, final Callback<List<T>> callback) {
        super(title);
        targetRemainingSourcesMin = remainingSourcesMin;
        targetRemainingSourcesMax = remainingSourcesMax;

        final FEventHandler onAdd = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (!addButton.isEnabled()) { return; }

                List<T> selected = new ArrayList<T>();
                for (int index : sourceList.selectedIndices) {
                    selected.add(sourceList.getItemAt(index));
                }
                for (T item : selected) {
                    sourceList.removeItem(item);
                    destList.addItem(item);
                }
                setButtonState();
            }
        };

        final FEventHandler onRemove = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (!removeButton.isEnabled()) { return; }

                List<T> selected = new ArrayList<T>();
                for (int index : destList.selectedIndices) {
                    selected.add(destList.getItemAt(index));
                }
                for (T item : selected) {
                    destList.removeItem(item);
                    sourceList.addItem(item);
                }
                setButtonState();
            }
        };

        sourceList = add(new ChoiceList(sourceElements, onAdd));
        destList = add(new ChoiceList(destElements, onRemove));

        // Dual List control buttons
        addButton = add(new FButton(">", onAdd));
        addAllButton = add(new FButton(">>", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                addAll();
            }
        }));
        removeButton = add(new FButton("<", onRemove));
        removeAllButton = add(new FButton("<<", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                removeAll();
            }
        }));
        
        final FEventHandler onAccept = new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
                callback.run(destList.extractListData());
            }
        };

        // Dual List Complete Buttons
        okButton = add(new FButton("OK", onAccept));
        autoButton = add(new FButton("Auto", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                addAll();
                onAccept.handleEvent(e);
            }
        }));

        selectOrder = add(new FLabel.Builder().align(HAlignment.CENTER).text("Select Order:").build());
        orderedLabel = add(new FLabel.Builder().align(HAlignment.CENTER).build());

        setButtonState();
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float x = FOptionPane.PADDING;
        float y = FOptionPane.PADDING;
        width -= 2 * x;

        float buttonHeight = FOptionPane.BUTTON_HEIGHT;
        float labelHeight = selectOrder.getAutoSizeBounds().height;
        float listHeight = (maxHeight - 2 * labelHeight - 2 * buttonHeight - 3 * FOptionPane.PADDING - FOptionPane.GAP_BELOW_BUTTONS) / 2;
        selectOrder.setBounds(x, y, width, labelHeight);
        y += labelHeight;
        sourceList.setBounds(x, y, width, listHeight);
        y += listHeight + FOptionPane.PADDING;
        
        float gapBetweenButtons = FOptionPane.PADDING / 2;
        float buttonWidth = (width - 3 * gapBetweenButtons) / 4;
        float dx = buttonWidth + gapBetweenButtons;
        addButton.setBounds(x, y, buttonWidth, buttonHeight);
        x += dx;
        addAllButton.setBounds(x, y, buttonWidth, buttonHeight);
        x += dx;
        removeButton.setBounds(x, y, buttonWidth, buttonHeight);
        x += dx;
        removeAllButton.setBounds(x, y, buttonWidth, buttonHeight);

        x = FList.PADDING;
        y += buttonHeight + FOptionPane.PADDING;
        orderedLabel.setBounds(x, y, width, labelHeight);
        y += labelHeight;
        destList.setBounds(x, y, width, listHeight);
        y += listHeight + FOptionPane.PADDING;
        
        buttonWidth = (width - gapBetweenButtons) / 2;
        dx = buttonWidth + gapBetweenButtons;
        okButton.setBounds(x, y, buttonWidth, buttonHeight);
        x += dx;
        autoButton.setBounds(x, y, buttonWidth, buttonHeight);

        return maxHeight;
    }

    public void setSecondColumnLabelText(String label) {
        orderedLabel.setText(label);
    }

    public void setSideboardMode(boolean isSideboardMode) {
        sideboardingMode = isSideboardMode;
        if (sideboardingMode) {
            addAllButton.setVisible(false);
            removeAllButton.setVisible(false);
            autoButton.setEnabled(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceList.getCount()));
            orderedLabel.setText(String.format("Main Deck (%d):", destList.getCount()));
        }
    }

    public List<T> getRemainingSourceList() {
        return sourceList.extractListData();
    }

    private void addAll() {
        for (T item : sourceList) {
            destList.addItem(item);
        }
        sourceList.clear();
        setButtonState();
    }

    private void removeAll() {
        for (T item : destList) {
            sourceList.addItem(item);
        }
        destList.clear();
        setButtonState();
    }

    private void setButtonState() {
        if (sideboardingMode) {
            removeAllButton.setVisible(false);
            addAllButton.setVisible(false);
            selectOrder.setText(String.format("Sideboard (%d):", sourceList.getCount()));
            orderedLabel.setText(String.format("Main Deck (%d):", destList.getCount()));
        }

        boolean anySize = targetRemainingSourcesMax < 0;
        boolean canAdd = sourceList.getCount() != 0 && (anySize || targetRemainingSourcesMin <= sourceList.getCount());
        boolean canRemove = destList.getCount() != 0;
        boolean targetReached = anySize || targetRemainingSourcesMin <= sourceList.getCount() && targetRemainingSourcesMax >= sourceList.getCount();

        autoButton.setEnabled(targetRemainingSourcesMax == 0 && !targetReached && !sideboardingMode);

        addButton.setEnabled(canAdd);
        addAllButton.setEnabled(canAdd);
        removeButton.setEnabled(canRemove);
        removeAllButton.setEnabled(canRemove);
        okButton.setEnabled(targetReached);
    }

    private class ChoiceList extends FList<T> {
        private List<Integer> selectedIndices = new ArrayList<Integer>();

        private ChoiceList(Collection<T> items, final FEventHandler dblTapCommand) {
            super(items != null ? items : new ArrayList<T>()); //handle null without crashing

            setListItemRenderer(new ListItemRenderer<T>() {
                @Override
                public float getItemHeight() {
                    return ListChooser.ITEM_HEIGHT;
                }

                @Override
                public boolean tap(T value, float x, float y, int count) {
                    Integer index = ChoiceList.this.getIndexOf(value);
                    if (selectedIndices.contains(index)) {
                        selectedIndices.remove(index);
                    }
                    else {
                        selectedIndices.add(index);
                    }
                    if (count == 2) {
                        dblTapCommand.handleEvent(new FEvent(ChoiceList.this, FEventType.ACTIVATE, index));
                    }
                    return true;
                }

                @Override
                public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    g.drawText(value.toString(), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
                }
            });
            setFontSize(12);
        }

        @Override
        protected void drawBackground(Graphics g) {
            g.fillRect(ListChooser.BACK_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawRect(1.5f, ListChooser.BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (selectedIndices.contains(index)) {
                return ListChooser.SEL_COLOR;
            }
            if (index % 2 == 1) {
                return ListChooser.ALT_ITEM_COLOR;
            }
            return null;
        }

        @Override
        protected boolean drawLineSeparators() {
            return false;
        }
    }
}
