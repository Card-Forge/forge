package forge.screens.home.online;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.properties.ForgeConstants;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FTextField;

public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby> {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Network Games");

    private VSubmenuOnlineLobby() {
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("fill", "[grow][grow]", "[grow]"));

        final FPanel pnlHost = new FPanel(new MigLayout("insets 5px 10% 5px 10%, wrap 2", "[grow,l]10[grow,r]", "[grow,c][grow,c]"));
        container.add(pnlHost, "west, w 50%!, h 100%!");

        final FLabel lblServerPort = new FLabel.Builder().text("Server port").build();
        pnlHost.add(lblServerPort, "w 100!, h 50!");

        final FTextField txtServerPort = new FTextField.Builder().text(String.valueOf(ForgeConstants.SERVER_PORT_NUMBER)).build();
        txtServerPort.setEditable(false);
        pnlHost.add(txtServerPort, "wrap");

        final FButton btnHost = new FButton("Host");
        btnHost.addActionListener(new ActionListener() {
            @Override public final void actionPerformed(final ActionEvent e) {
                getLayoutControl().host(Integer.parseInt(txtServerPort.getText()));
            }
        });
        pnlHost.add(btnHost, "span 2, wrap, w 200!, h 50!");

        final FPanel pnlJoin = new FPanel(new MigLayout("insets 5px 10% 5px 10%, wrap 2", "[grow,l]10[grow,r]", "[grow,c][grow,c][grow,c]"));
        container.add(pnlJoin, "east, w 50%!, h 100%!");

        final FLabel lblJoinHost = new FLabel.Builder().text("Hostname").build();
        pnlJoin.add(lblJoinHost, "w 100!, h 50!");

        final FTextField txtJoinHost = new FTextField.Builder().text("localhost").build();
        pnlJoin.add(txtJoinHost, "wrap, w 250!");

        final FLabel lblJoinPort = new FLabel.Builder().text("Host port").build();
        pnlJoin.add(lblJoinPort, "w 100!, h 50!");

        final FTextField txtJoinPort = new FTextField.Builder().text(String.valueOf(ForgeConstants.SERVER_PORT_NUMBER)).build();
        txtJoinPort.setEditable(false);
        pnlJoin.add(txtJoinPort, "wrap");

        final FButton btnJoin = new FButton("Join");
        btnJoin.addActionListener(new ActionListener() {
            @Override public final void actionPerformed(final ActionEvent e) {
                getLayoutControl().join(txtJoinHost.getText(), Integer.parseInt(txtJoinPort.getText()));
            }
        });
        pnlJoin.add(btnJoin, "span 2, w 200!, h 50!");

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ONLINE;
    }

    @Override
    public String getMenuTitle() {
        return "Lobby";
    }

    @Override
    public EDocID getItemEnum() {
        return getDocumentID();
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_NETWORK;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuOnlineLobby getLayoutControl() {
        return CSubmenuOnlineLobby.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
