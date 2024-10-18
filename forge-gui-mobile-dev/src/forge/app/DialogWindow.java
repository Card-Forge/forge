package forge.app;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;

public class DialogWindow {
    public DialogWindow(String title, String message) {
        List<Object> options = new ArrayList<>();
        JButton ok = new JButton("OK");
        options.add(ok);
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options.toArray());
        JDialog dlg = pane.createDialog(JOptionPane.getRootFrame(), title);
        ok.addActionListener(e -> {
            dlg.setVisible(false);
            System.exit(0);
        });
        dlg.setResizable(false);
        dlg.setVisible(true);
    }
}
