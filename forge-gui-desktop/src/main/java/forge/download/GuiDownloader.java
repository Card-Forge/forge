/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.download;

import java.net.Proxy;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.gui.SOverlayUtils;
import forge.toolbox.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class GuiDownloader extends DefaultBoundedRangeModel {
    public static final Proxy.Type[] TYPES = Proxy.Type.values();

    // Swing components
    private final FPanel pnlDialog = new FPanel(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
    private final FProgressBar progressBar = new FProgressBar();
    private final FButton btnStart = new FButton("Start");
    private final FTextField txtAddress = new FTextField.Builder().ghostText("Proxy Address").build();
    private final FTextField txtPort = new FTextField.Builder().ghostText("Proxy Port").build();

    private final UiCommand cmdClose = new UiCommand() {
        @Override
        public void run() {
            service.setCancel(true);

            // Kill overlay
            SOverlayUtils.hideOverlay();
        }
    };

    private final FLabel btnClose = new FLabel.Builder().text("X")
            .hoverable(true).fontAlign(SwingConstants.CENTER).cmdClick(cmdClose).build();

    private final FRadioButton radProxyNone = new FRadioButton("No Proxy");
    private final FRadioButton radProxySocks = new FRadioButton("SOCKS Proxy");
    private final FRadioButton radProxyHTTP = new FRadioButton("HTTP Proxy");

    private final GuiDownloadService service;

    public GuiDownloader(GuiDownloadService service0) {
        service = service0;

        String radConstraints = "w 100%!, h 30px!, gap 2% 0 0 10px";
        JXButtonPanel grpPanel = new JXButtonPanel();
        grpPanel.add(radProxyNone, radConstraints);
        grpPanel.add(radProxyHTTP, radConstraints);
        grpPanel.add(radProxySocks, radConstraints);

        radProxyNone.addChangeListener(new ProxyHandler(0));
        radProxyHTTP.addChangeListener(new ProxyHandler(1));
        radProxySocks.addChangeListener(new ProxyHandler(2));
        radProxyNone.setSelected(true);

        btnClose.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_TEXT)));
        btnStart.setFont(FSkin.getFont(18));
        btnStart.setEnabled(false);

        progressBar.reset();
        progressBar.setString("Scanning for existing items...");
        pnlDialog.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

        // Layout
        pnlDialog.add(grpPanel, "w 50%!");
        pnlDialog.add(txtAddress, "w 95%!, h 30px!, gap 2% 0 0 10px");
        pnlDialog.add(txtPort, "w 95%!, h 30px!, gap 2% 0 0 10px");
        pnlDialog.add(progressBar, "w 95%!, h 40px!, gap 2% 0 20px 0");
        pnlDialog.add(btnStart, "w 200px!, h 40px!, gap 0 0 20px 0, ax center");
        pnlDialog.add(btnClose, "w 20px!, h 20px!, pos 370px 10px");

        final JPanel pnl = FOverlay.SINGLETON_INSTANCE.getPanel();
        pnl.removeAll();
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        pnl.add(pnlDialog, "w 400px!, h 350px!, ax center, ay center");
        SOverlayUtils.showOverlay();

        service.initialize(txtAddress, txtPort, progressBar, btnStart, cmdClose, new Runnable() {
            @Override
            public void run() {
                fireStateChanged();
            }
        });
    }

    private class ProxyHandler implements ChangeListener {
        private final int type;

        public ProxyHandler(final int type) {
            this.type = type;
        }

        @Override
        public final void stateChanged(final ChangeEvent e) {
            if (((AbstractButton) e.getSource()).isSelected()) {
                service.setType(this.type);
                txtAddress.setEnabled(this.type != 0);
                txtPort.setEnabled(this.type != 0);
            }
        }
    }
}
