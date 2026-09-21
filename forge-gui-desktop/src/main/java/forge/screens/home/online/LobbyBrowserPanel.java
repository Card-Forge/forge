package forge.screens.home.online;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import forge.gamemodes.net.LobbyClient;
import forge.gamemodes.net.LobbyRoom;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.BuildInfo;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Inline panel displaying a table of public lobbies from the lobby server.
 * Shown below the Host/Join buttons in the online lobby view when no game is active.
 */
public class LobbyBrowserPanel extends JPanel {

    private final JTable table;
    private final LobbyTableModel tableModel;
    private final FButton btnRefresh;
    private final FLabel lblStatus;
    private final Timer autoRefreshTimer;
    private volatile LobbyClient sharedLobbyClient;
    private int consecutiveFailures = 0;
    private final java.util.concurrent.atomic.AtomicBoolean refreshing = new java.util.concurrent.atomic.AtomicBoolean(false);

    public LobbyBrowserPanel() {
        super(new MigLayout("insets 10 15 10 15, gap 0, wrap 1", "[grow,fill]"));
        setOpaque(false);

        // Header row
        JPanel headerPanel = new JPanel(new MigLayout("insets 0, gap 10", "[grow,fill][]"));
        headerPanel.setOpaque(false);

        FLabel lblTitle = new FLabel.Builder()
                .text(Localizer.getInstance().getMessageorUseDefault("lblLobbyPublicLobbies", "Public Lobbies"))
                .fontSize(16)
                .fontStyle(Font.BOLD)
                .build();
        headerPanel.add(lblTitle, "growx");

        btnRefresh = new FButton(Localizer.getInstance().getMessage("lblRefresh"));
        btnRefresh.setFont(FSkin.getRelativeFixedFont(12));
        btnRefresh.addActionListener(e -> refreshRooms());
        headerPanel.add(btnRefresh, "w 80!, h 28!");

        add(headerPanel, "growx, gapbottom 8");

        // Status label (for errors / "no rooms" message)
        lblStatus = new FLabel.Builder()
                .text("")
                .fontSize(12)
                .fontAlign(SwingConstants.CENTER)
                .build();

        // Table
        tableModel = new LobbyTableModel();
        table = new JTable(tableModel);
        table.setFont(FSkin.getRelativeFixedFont(12));
        table.setRowHeight(28);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setOpaque(false);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE).getColor());
        table.setSelectionForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(FSkin.getRelativeFixedFont(12).deriveFont(Font.BOLD));
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setBackground(new Color(0, 0, 0, 0));

        // Custom renderer for version compatibility highlighting
        table.setDefaultRenderer(Object.class, new RoomCellRenderer());

        // Double-click to join
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);   // Lock icon
        table.getColumnModel().getColumn(1).setPreferredWidth(200);  // Room name
        table.getColumnModel().getColumn(2).setPreferredWidth(100);  // Format
        table.getColumnModel().getColumn(3).setPreferredWidth(50);   // Players
        table.getColumnModel().getColumn(4).setPreferredWidth(100);  // Game version

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new java.awt.Dimension(0, 200));
        scrollPane.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 250));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        add(scrollPane, "growx, gapbottom 5");

        // Join button below table
        JPanel joinPanel = new JPanel(new MigLayout("insets 0, ax center"));
        joinPanel.setOpaque(false);
        FButton btnJoin = new FButton(Localizer.getInstance().getMessage("lblJoinGame"));
        btnJoin.setFont(FSkin.getRelativeFont(14));
        btnJoin.addActionListener(e -> joinSelectedRoom());
        joinPanel.add(btnJoin, "w 150!, h 35!");
        add(joinPanel, "growx");

        // Status bar
        add(lblStatus, "growx, gaptop 5");

        // Auto-refresh every 30 seconds
        autoRefreshTimer = new Timer(30000, e -> refreshRooms());
        autoRefreshTimer.setCoalesce(true);
    }

    /**
     * Start auto-refreshing. Call when the panel becomes visible.
     */
    public void startAutoRefresh() {
        if (!autoRefreshTimer.isRunning()) {
            refreshRooms();
            autoRefreshTimer.start();
        }
    }

    /**
     * Stop auto-refreshing. Call when the panel is hidden.
     */
    public void stopAutoRefresh() {
        autoRefreshTimer.stop();
    }

    /**
     * Fetch rooms from the lobby server and update the table.
     * Uses exponential backoff on consecutive failures to avoid
     * hammering the server during outages.
     */
    public void refreshRooms() {
        // Prevent overlapping refreshes (e.g. slow server + timer + manual clicks)
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        btnRefresh.setEnabled(false);
        lblStatus.setText(Localizer.getInstance().getMessage("lblLoading"));

        final int backoffMs = consecutiveFailures > 0
                ? Math.min(1000 * (1 << consecutiveFailures), 30000)
                : 0;

        new Thread(() -> {
            // Apply backoff delay on background thread, not EDT
            if (backoffMs > 0) {
                try { Thread.sleep(backoffMs); } catch (InterruptedException e) {
                    refreshing.set(false);
                    return;
                }
            }

            String lobbyUrl = FModel.getNetPreferences().getPref(ForgeNetPreferences.FNetPref.LOBBY_SERVER_URL);
            List<LobbyRoom> rooms = new ArrayList<>();
            String error = null;

            try {
                if (lobbyUrl == null || lobbyUrl.isEmpty()) {
                    error = "Lobby server URL not configured";
                } else {
                    // Reuse a single LobbyClient instance per URL
                    if (sharedLobbyClient == null || !sharedLobbyClient.getServerUrl().equals(lobbyUrl)) {
                        sharedLobbyClient = new LobbyClient(lobbyUrl);
                    }
                    rooms = sharedLobbyClient.fetchRooms();
                }
            } catch (Exception e) {
                error = e.getMessage();
            }

            final List<LobbyRoom> finalRooms = rooms;
            final String finalError = error;

            SwingUtilities.invokeLater(() -> {
                btnRefresh.setEnabled(true);
                refreshing.set(false);

                if (finalError != null) {
                    consecutiveFailures = Math.min(consecutiveFailures + 1, 5);
                    lblStatus.setText(Localizer.getInstance().getMessageorUseDefault(
                            "lblLobbyServerUnavailable", "Could not connect to lobby server"));
                    tableModel.setRooms(new ArrayList<>());
                } else {
                    consecutiveFailures = 0;
                    if (finalRooms.isEmpty()) {
                        lblStatus.setText(Localizer.getInstance().getMessageorUseDefault(
                                "lblLobbyNoRoomsAvailable", "No active lobbies found"));
                        tableModel.setRooms(new ArrayList<>());
                    } else {
                        lblStatus.setText("");
                        tableModel.setRooms(finalRooms);
                    }
                }
            });
        }).start();
    }

    private void joinSelectedRoom() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) return;

        LobbyRoom room = tableModel.getRoomAt(row);
        if (room == null) return;

        // Check password
        if (room.isHasPassword()) {
            String password = SOptionPane.showInputDialog(
                    Localizer.getInstance().getMessageorUseDefault("lblLobbyPasswordRequired", "This room is password-protected. Enter password:"),
                    Localizer.getInstance().getMessage("lblJoinGame"));
            if (password == null) return; // cancelled

            if (sharedLobbyClient == null) {
                String lobbyUrl = FModel.getNetPreferences().getPref(ForgeNetPreferences.FNetPref.LOBBY_SERVER_URL);
                sharedLobbyClient = new LobbyClient(lobbyUrl);
            }
            if (!sharedLobbyClient.verifyPassword(room.getId(), password)) {
                SOptionPane.showErrorDialog(
                        Localizer.getInstance().getMessageorUseDefault("lblLobbyWrongPassword", "Incorrect password."),
                        Localizer.getInstance().getMessage("lblJoinGame"));
                return;
            }
        }

        // Join via relay
        String url = room.getConnectAddress();
        if (url == null) {
            SOptionPane.showErrorDialog(
                    Localizer.getInstance().getMessageorUseDefault("lblLobbyNoRelay", "This room does not have a relay configured."),
                    Localizer.getInstance().getMessage("lblJoinGame"));
            return;
        }
        CSubmenuOnlineLobby.SINGLETON_INSTANCE.joinGameWithUrl(url);
    }

    private boolean isVersionCompatible(String roomVersion) {
        try {
            String currentVersion = BuildInfo.getVersionString();
            // Compare major.minor — ignore patch
            String[] currentParts = currentVersion.split("\\.");
            String[] roomParts = roomVersion.split("\\.");
            if (currentParts.length >= 2 && roomParts.length >= 2) {
                return currentParts[0].equals(roomParts[0]) && currentParts[1].equals(roomParts[1]);
            }
            return currentVersion.equals(roomVersion);
        } catch (Exception e) {
            return true; // If we can't parse, assume compatible
        }
    }

    // --- Table Model ---

    private class LobbyTableModel extends AbstractTableModel {
        private List<LobbyRoom> rooms = new ArrayList<>();
        private final String[] COLUMNS = {"", "Room Name", "Format", "Players", "Version"};

        public void setRooms(List<LobbyRoom> rooms) {
            this.rooms = new ArrayList<>(rooms);
            fireTableDataChanged();
        }

        public LobbyRoom getRoomAt(int row) {
            return (row >= 0 && row < rooms.size()) ? rooms.get(row) : null;
        }

        @Override
        public int getRowCount() { return rooms.size(); }

        @Override
        public int getColumnCount() { return COLUMNS.length; }

        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public boolean isCellEditable(int row, int col) { return false; }

        @Override
        public Object getValueAt(int row, int col) {
            LobbyRoom room = rooms.get(row);
            if (room == null) return "";
            return switch (col) {
                case 0 -> room.isHasPassword() ? "\uD83D\uDD12" : ""; // lock emoji
                case 1 -> room.getName();
                case 2 -> room.getFormat();
                case 3 -> room.getPlayerCount() + "/" + room.getMaxPlayers();
                case 4 -> room.getGameVersion() + (isVersionCompatible(room.getGameVersion()) ? "" : " \u26A0");
                default -> "";
            };
        }
    }

    // --- Cell Renderer ---

    private class RoomCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            LobbyRoom room = tableModel.getRoomAt(row);
            if (room != null && !isVersionCompatible(room.getGameVersion())) {
                c.setForeground(new Color(150, 150, 150)); // grey for incompatible
                c.setFont(c.getFont().deriveFont(Font.ITALIC));
            } else {
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
                if (isSelected) {
                    c.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
                } else {
                    c.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
                }
            }
            if (isSelected) {
                c.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE).getColor());
                setOpaque(true);
            } else {
                setOpaque(false);
            }
            return c;
        }
    }
}
