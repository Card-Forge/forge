package forge.screens.match;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import forge.Singletons;
import forge.UiCommand;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class VAutoYields extends FDialog {
    private static final int PADDING = 10;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 26;

    private final FButton btnOk;
    private final FButton btnRemove;
    private final FList<String> lstAutoYields;
    private final FScrollPane listScroller;
    private final FCheckBox chkDisableAll;
    private final List<String> autoYields;

    public VAutoYields(final CMatchUI matchUI) {
        super();
        setTitle("Auto-Yields");

        autoYields = new ArrayList<String>();
        for (final String autoYield : matchUI.getAutoYields()) {
            autoYields.add(autoYield);
        }
        lstAutoYields = new FList<String>(new AutoYieldsListModel());

        int x = PADDING;
        int y = PADDING;
        int width = Singletons.getView().getFrame().getWidth() * 2 / 3;
        int w = width - 2 * PADDING;

        listScroller = new FScrollPane(lstAutoYields, true);

        chkDisableAll = new FCheckBox("Disable All Auto Yields", matchUI.getDisableAutoYields());
        chkDisableAll.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                matchUI.setDisableAutoYields(chkDisableAll.isSelected());
            }
        });

        btnOk = new FButton("OK");
        btnOk.setCommand(new UiCommand() {
            @Override
            public void run() {
                setVisible(false);
            }
        });
        btnRemove = new FButton("Remove Yield");
        btnRemove.setCommand(new UiCommand() {
            @Override
            public void run() {
                String selected = lstAutoYields.getSelectedValue();
                if (selected != null) {
                    autoYields.remove(selected);
                    btnRemove.setEnabled(autoYields.size() > 0);
                    matchUI.setShouldAutoYield(selected, false);
                    VAutoYields.this.revalidate();
                    lstAutoYields.repaint();
                }
            }
        });
        if (autoYields.size() > 0) {
            lstAutoYields.setSelectedIndex(0);
        }
        else {
            btnRemove.setEnabled(false);
        }

        Dimension checkBoxSize = chkDisableAll.getPreferredSize();
        int listHeight = lstAutoYields.getMinimumSize().height + 2 * PADDING;

        add(listScroller, x, y, w, listHeight);
        y += listHeight + PADDING;
        add(chkDisableAll, x, y, checkBoxSize.width, checkBoxSize.height);
        x = w - 2 * BUTTON_WIDTH - PADDING;
        add(btnOk, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        x += BUTTON_WIDTH + PADDING;
        add(btnRemove, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

        this.pack();
        this.setSize(width, getHeight());
    }

    private class AutoYieldsListModel extends AbstractListModel<String> {
        @Override
        public int getSize() {
            return autoYields.size();
        }

        @Override
        public String getElementAt(final int index) {
            return autoYields.get(index);
        }
    }

    public void showAutoYields() {
        if (lstAutoYields.getCount() > 0) {
            setVisible(true);
            dispose();
        } else {
            FOptionPane.showMessageDialog("There are no active auto-yields.", "No Auto-Yields", FOptionPane.INFORMATION_ICON);
        }
    }
}
