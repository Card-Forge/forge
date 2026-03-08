package forge.screens.deckeditor;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.toolbox.FCheckBox;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ColorSelectionDialog extends JDialog {
    private final List<FCheckBox> colorBoxes = new ArrayList<>();
    private ColorSet selectedColors = ColorSet.fromMask(0x1F); // All colors by default
    private boolean confirmed = false;

    public ColorSelectionDialog(Window owner, ColorSet defaultSelected) {
        super(owner, Localizer.getInstance().getMessage("lblChooseColors"), ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE); // Set dialog background
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBackground(Color.WHITE); // Set panel background
        String[] colorNames = {"White", "Blue", "Black", "Red", "Green"};
        Color[] fgColors = {Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK};
        byte[] colorMasks = {forge.card.MagicColor.WHITE, forge.card.MagicColor.BLUE, forge.card.MagicColor.BLACK, forge.card.MagicColor.RED, forge.card.MagicColor.GREEN};
        for (int i = 0; i < colorNames.length; i++) {
            boolean selected = defaultSelected == null || defaultSelected.hasAnyColor(colorMasks[i]);
            FCheckBox box = new FCheckBox(colorNames[i], selected);
            box.setForeground(fgColors[i]); // Set label text color to black
            box.setBackground(Color.WHITE); // Set checkbox background
            colorBoxes.add(box);
            panel.add(box);
        }
        add(panel, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE); // Set button panel background
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        btnPanel.add(ok);
        btnPanel.add(cancel);
        add(btnPanel, BorderLayout.SOUTH);
        ok.addActionListener(e -> {
            confirmed = true;
            selectedColors = ColorSet.fromMask(
                (colorBoxes.get(0).isSelected() ? MagicColor.WHITE : 0) |
                (colorBoxes.get(1).isSelected() ? MagicColor.BLUE : 0) |
                (colorBoxes.get(2).isSelected() ? MagicColor.BLACK : 0) |
                (colorBoxes.get(3).isSelected() ? MagicColor.RED : 0) |
                (colorBoxes.get(4).isSelected() ? MagicColor.GREEN : 0)
            );
            setVisible(false);
        });
        cancel.addActionListener(e -> setVisible(false));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ColorSet getSelectedColors() {
        return selectedColors;
    }
}
