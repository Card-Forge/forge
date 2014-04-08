package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FTextField;
import forge.toolbox.LayoutHelper;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class TextSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected FTextField txtSearch;

    public TextSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<T> createCopy() {
        TextSearchFilter<T> copy = new TextSearchFilter<T>(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }

    @Override
    public boolean isEmpty() {
        return txtSearch.isEmpty();
    }

    @Override
    public void reset() {
        txtSearch.setText("");
    }

    @Override
    public Component getMainComponent() {
        return txtSearch;
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(ItemFilter<?> filter) {
        return false;
    }

    @Override
    protected void buildWidget(JPanel widget) {
        txtSearch = new FTextField.Builder().ghostText("Search").build();
        widget.add(txtSearch);

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_PAGE_UP:
                    case KeyEvent.VK_PAGE_DOWN:
                        //set focus to item manager when certain keys pressed
                        if (changeTimer.isRunning()) {
                            applyChange(); //apply change now if currently delayed
                        }
                        itemManager.focus();
                        break;
                    case KeyEvent.VK_ENTER:
                        if (e.getModifiers() == 0) {
                            if (changeTimer.isRunning()) {
                                applyChange(); //apply change now if currently delayed
                            }
                        }
                        break;
                }
            }
        });

        txtSearch.addChangeListener(new FTextField.ChangeListener() {
            @Override
            public void textChanged() {
                changeTimer.restart();
            }
        });
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(txtSearch, FTextField.HEIGHT);
    }

    @Override
    protected void applyChange() {
        changeTimer.stop(); //ensure change timer stopped before applying change
        super.applyChange();
    }

    private Timer changeTimer = new Timer(200, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            applyChange();
        }
    });

    @Override
    protected Predicate<T> buildPredicate() {
        String text = txtSearch.getText();
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }
        return SFilterUtil.buildItemTextFilter(text);
    }
}
