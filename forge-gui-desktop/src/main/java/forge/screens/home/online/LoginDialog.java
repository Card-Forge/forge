package forge.screens.home.online;

import java.awt.Dimension;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.UiCommand;
import forge.error.BugReporter;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.server.ServerUtil;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FPasswordField;
import forge.toolbox.FTextField;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class LoginDialog extends FDialog {
    private final FLabel lblUsername = new FLabel.Builder().text("Username:").build();
    private final FTextField txtUsername = new FTextField.Builder().build();
    private final FLabel lblPassword = new FLabel.Builder().text("Password:").build();
    private final FPasswordField txtPassword = new FPasswordField();
    private final FCheckBox cbRememberMe = new FCheckBox("Remember Me");
    private final FButton btnLogin = new FButton("Login");
    private final FButton btnCancel = new FButton("Cancel");

    public static boolean login() {
        String username = FModel.getPreferences().getPref(FPref.ONLINE_USERNAME);
        String password = FModel.getPreferences().getPref(FPref.ONLINE_PASSWORD);
        if (!username.isEmpty() && !password.isEmpty()) {
            try {
                if (ServerUtil.login(username, password)) {
                    return true; //avoid showing dialog if able to login with saved username/password
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            FModel.getPreferences().setPref(FPref.ONLINE_PASSWORD, ""); //clear password if login failed
            FModel.getPreferences().save();
        }

        LoginDialog dialog = new LoginDialog(username);
        dialog.setVisible(true);
        dialog.dispose();
        return dialog.result;
    }

    private boolean result = false;

    private LoginDialog(String username) {
        setTitle("Login");

        txtUsername.setText(username);

        btnLogin.setCommand(new UiCommand() {
            @Override
            public void run() {
                String username = txtUsername.getText();
                if (username.isEmpty()) {
                    FOptionPane.showErrorDialog("You must enter a username", "Login Failed");
                    txtUsername.requestFocusInWindow();
                    return;
                }
                char[] password = txtPassword.getPassword();
                if (password == null || password.length == 0) {
                    FOptionPane.showErrorDialog("You must enter a password", "Login Failed");
                    txtPassword.requestFocusInWindow();
                    return;
                }
                try {
                    String passwordStr = String.valueOf(password);
                    if (ServerUtil.login(username, passwordStr)) {
                        FModel.getPreferences().setPref(FPref.ONLINE_USERNAME, username);
                        FModel.getPreferences().setPref(FPref.ONLINE_PASSWORD, cbRememberMe.isSelected() ? passwordStr : "");
                        FModel.getPreferences().save();
                        setVisible(false);
                        return;
                    }
                    FOptionPane.showErrorDialog("Could not login with entered username and password.", "Login Failed");
                    txtUsername.requestFocusInWindow();
                }
                catch (Exception e) {
                    BugReporter.reportException(e, "Login Failed");
                }
            }
        });
        btnCancel.setCommand(new UiCommand() {
            @Override
            public void run() {
                setVisible(false);
            }
        });

        final int width = 330;
        final int height = 180;
        setPreferredSize(new Dimension(width, height));
        setSize(width, height);

        JPanel pnlContent = new JPanel(new MigLayout("gap 6, insets 0"));
        pnlContent.setOpaque(false);
        pnlContent.add(lblUsername);
        pnlContent.add(txtUsername, "growx, pushx, wrap");
        pnlContent.add(lblPassword);
        pnlContent.add(txtPassword, "growx, pushx, wrap");

        add(pnlContent, "pushx, growx, wrap, span 2");
        add(cbRememberMe, "pushx, growx, wrap");
        add(btnLogin, "w 150px!, h 30px!, gapright 6px");
        add(btnCancel, "w 150px!, h 30px!");

        if (!username.isEmpty()) {
            setDefaultFocus(txtPassword);
        }
    }
}
