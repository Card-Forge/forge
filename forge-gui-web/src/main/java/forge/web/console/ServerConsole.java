package forge.web.console;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.FThreads;
import forge.gui.download.CdnUuidCache;
import forge.gui.interfaces.IProgressBar;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.LogExporter;
import forge.web.WebGuiBase;
import forge.web.WebMain;
import forge.web.ServerControl;
import forge.web.images.ImageIndex;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import static forge.web.console.ConsoleWidgets.WARN;
import static forge.web.console.ConsoleWidgets.WARN_TEXT;
import static forge.web.console.ConsoleWidgets.asLink;
import static forge.web.console.ConsoleWidgets.button;
import static forge.web.console.ConsoleWidgets.copyButton;
import static forge.web.console.ConsoleWidgets.field;
import static forge.web.console.ConsoleWidgets.heading;
import static forge.web.console.ConsoleWidgets.link;
import static forge.web.console.ConsoleWidgets.muted;
import static forge.web.console.ConsoleWidgets.reword;
import static forge.web.console.ConsoleWidgets.row;
import static forge.web.console.ConsoleWidgets.section;
import static forge.web.console.ConsoleWidgets.stack;
import static forge.web.console.ConsoleWidgets.warning;
import static forge.web.console.ConsoleWidgets.wrapped;

/**
 * The desktop window that starts and stops the server. Without it, a player who closes their tab has no sign that Forge is still running.
 * As a progress bar it shows the card reader's progress.
 */
public final class ServerConsole implements IProgressBar, IHasForgeLog {
    private static final Localizer TEXT = Localizer.getInstance();
    /** How wide a line of text can be before it wraps. This sets the width of the window. */
    private static final int TEXT_WIDTH = 480;
    private static final int HELP_WIDTH = TEXT_WIDTH - 26;
    private static final String GUIDE = "https://github.com/Card-Forge/forge/wiki/Network-Play#network-configuration";

    /** Running shows a filled lamp and stopped shows an empty ring. The shape shows the state, so the colour does not have to. */
    private static final Color LIT = new Color(0x3f, 0xb9, 0x50);
    private static final Color DARK = new Color(0xd0, 0x57, 0x4e);

    private final WebGuiBase ui;
    private final Runnable onQuit;
    private ServerControl service;
    /** Whether the server is up and the console is not starting or stopping it. */
    private boolean up;
    private Map<String, String> homes = Map.of();
    /** The adapter and address that the network link uses, or null when this machine has none. */
    private Map.Entry<String, String> home;
    private String internet;
    private final Light light = new Light();
    private final JLabel state = new JLabel(TEXT.getMessage("lblWebConsoleStartingForge"));
    private final JLabel keepOpen = muted(TEXT.getMessage("lblWebConsoleKeepOpen"));
    private final JProgressBar progress = new JProgressBar();
    private final JTextArea startFailure = wrapped("", WARN_TEXT, HELP_WIDTH);
    private final JLabel homeAddress = muted("—");
    private final JLabel netAddress = muted("—");
    private final JButton copyHome = copyButton(TEXT.getMessage("lblWebConsoleCopyLink"), () -> service.inviteUrl(home.getValue()));
    private final JButton moreHome = new BasicArrowButton(SwingConstants.SOUTH);
    private final JButton copyNet = copyButton(TEXT.getMessage("lblWebConsoleCopyLink"), () -> service.inviteUrl(internet));
    private final Light indexLight = new Light(WARN);
    private final JLabel indexState = new JLabel();
    private final JLabel indexUpdated = muted(" ");
    private final JProgressBar indexBar = new JProgressBar(0, 100);
    private final JButton indexButton = new JButton();
    private final ImageIndex index = new ImageIndex(now -> SwingUtilities.invokeLater(() -> showIndex(now)));
    private final JCheckBox quitWhenEmpty = new JCheckBox(TEXT.getMessage("lblWebConsoleQuitWhenEmpty"), !ServerControl.KEEP_OPEN);
    private final JCheckBox forwardPort = new JCheckBox(TEXT.getMessage("lblWebConsoleForwardPort"));
    private final Light forwardLight = new Light();
    private final JTextArea forwardState = wrapped("", UIManager.getColor("Label.foreground"), TEXT_WIDTH - 250);
    private final JLabel refused = new JLabel();
    private final JLabel players = muted(" ");
    private final JButton imagesButton = new JButton(TEXT.getMessage("lblWebImagesButton"));
    private final CardImageDownloads images = new CardImageDownloads(imagesButton::setText);
    /** One thread, so two quick clicks on the port option reach the router in the order they were made. */
    private final ExecutorService router = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ForgePortForward").setDaemon(true).build());
    private JFrame frame;
    private JButton browse;
    private JButton startStop;
    private JButton retryNet;
    private JPanel startHelp;
    private JPanel forwardStateRow;
    private JPanel forwardHelp;
    private JPanel netField;
    private boolean downloadingIndex;
    private int widest;
    private JPanel main;
    private volatile boolean quitting;

    private ServerConsole(final WebGuiBase ui, final Runnable onQuit) {
        this.ui = ui;
        this.onQuit = onQuit;
    }

    /** Opens the console. Returns null when there is no display or the flag turns the console off. */
    public static ServerConsole open(final WebGuiBase ui, final Runnable onQuit) {
        if (Boolean.getBoolean("forge.web.noConsole") || GraphicsEnvironment.isHeadless()) {
            return null;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            netLog.warn(e, "Could not use the system's look for the console");
        }
        final ServerConsole console = new ServerConsole(ui, onQuit);
        try {
            SwingUtilities.invokeAndWait(console::build);
        } catch (final Exception e) {
            netLog.warn(e, "Could not open the console");
            return null;
        }
        return console;
    }

    public void starting(final String what) {
        SwingUtilities.invokeLater(() -> busy(what));
    }

    /** Hands the console the service after the first start attempt, and the buttons control it from now on. failure is the reason it could not start, or null. */
    public void attach(final ServerControl driven, final Exception failure) {
        service = driven;
        SwingUtilities.invokeLater(() -> {
            quitWhenEmpty.setEnabled(true);
            forwardPort.setEnabled(true);
            imagesButton.setEnabled(true);
            indexButton.setEnabled(true);
            showIndex(new ImageIndex.State(false, -1, false));
        });
        driven.onForwarding(this::forwarding);
        if (driven.running()) {
            running();
        } else {
            stopped(failure);
        }
    }

    private void busy(final String what) {
        up = false;
        // Forge's keys end with three dots and the console's own with an ellipsis
        state.setText(what.endsWith("...") ? what.substring(0, what.length() - 3) + "\u2026" : what);
        keepOpen.setVisible(false);
        startStop.setVisible(false);
        startHelp.setVisible(false);
        progress.setIndeterminate(true);
        progress.setString("");
        progress.setVisible(true);
        browse.setEnabled(false);
        copyHome.setEnabled(false);
        copyNet.setEnabled(false);
    }

    /** The port is bound. The lamp is lit and the links can be copied. */
    private void running() {
        SwingUtilities.invokeLater(() -> {
            up = true;
            light.lit(true);
            state.setText(TEXT.getMessage("lblWebConsoleRunning"));
            keepOpen.setVisible(true);
            startStop.setText(TEXT.getMessage("lblWebConsoleStopServer"));
            startStop.setVisible(true);
            progress.setVisible(false);
            browse.setEnabled(true);
            frame.getRootPane().setDefaultButton(null);
            showInternet(null, TEXT.getMessage("lblWebMatchBarWorkingOutAddress"), false);
            refit();
        });
        FThreads.invokeInBackgroundThread(() -> {
            final Map<String, String> local = FServerManager.getAllLocalAddresses();
            local.values().removeIf("localhost"::equals);
            SwingUtilities.invokeLater(() -> {
                if (up) {
                    showHome(local);
                }
            });
            findInternet();
        });
    }

    /** Shows the state of the request to the router, in the words a router's own settings use. */
    private void forwarding(final ServerControl.Forwarding now) {
        SwingUtilities.invokeLater(() -> {
            final boolean wanted = service.forwardPort();
            forwardPort.setSelected(wanted);
            forwardLight.lit(now == ServerControl.Forwarding.FORWARDED);
            // A refusal only means UPnP did not forward the port, which may still be forwarded by hand, so the link stays usable
            refused.setText(TEXT.getMessage("lblWebConsoleRefused", service.port()));
            forwardHelp.setVisible(now == ServerControl.Forwarding.REFUSED);
            final String words = switch (now) {
                case OFF -> wanted ? "" : TEXT.getMessage("lblWebConsoleUpnpOff", service.port());
                case ASKING -> TEXT.getMessage("lblWebConsoleAskingRouter");
                case FORWARDED -> TEXT.getMessage("lblWebConsoleForwarded", service.port());
                case REFUSED -> "";
            };
            // Forwarding is off until the host turns it on, so the line that says so is highlighted
            final boolean off = now == ServerControl.Forwarding.OFF;
            forwardState.setForeground(off ? WARN : UIManager.getColor("Label.foreground"));
            forwardState.setFont(UIManager.getFont("Label.font").deriveFont(off ? Font.BOLD : Font.PLAIN));
            reword(forwardState, words, TEXT_WIDTH - 250);
            forwardLight.setVisible(!words.isEmpty());
            refit();
        });
    }

    /** Shows where card pictures come from. They come from Scryfall's image servers once its card data has been downloaded. */
    private void showIndex(final ImageIndex.State now) {
        final long updated = ImageIndex.updated();
        indexBar.setVisible(now.downloading());
        indexBar.setIndeterminate(now.percent() < 0);
        indexBar.setValue(Math.max(0, now.percent()));
        indexLight.lit(updated > 0);
        final String words;
        final String action;
        if (now.downloading()) {
            words = now.percent() < 0 ? TEXT.getMessage("lblWebConsolePicturesStarting") : TEXT.getMessage("lblWebConsolePicturesDownloading", now.percent());
            action = TEXT.getMessage("lblCancel");
        } else if (now.failed()) {
            words = TEXT.getMessage("lblWebConsolePicturesFailed");
            action = TEXT.getMessage("lblTryAgain");
        } else if (updated > 0) {
            words = TEXT.getMessage("lblWebConsolePicturesFast");
            action = TEXT.getMessage("lblWebImagesIndexUpdate");
        } else {
            words = TEXT.getMessage("lblWebConsolePicturesSlow");
            action = TEXT.getMessage("lblWebConsolePicturesSpeedUp");
        }
        indexButton.setText(action);
        // The text takes the space the button leaves, so the row is as wide as the others whatever the button says
        indexState.setText("<html>" + words + "</html>");
        // A label wraps HTML only when it is given a width, and a width set in the HTML itself is not in screen pixels
        final View laidOut = (View) indexState.getClientProperty(BasicHTML.propertyKey);
        final int room = TEXT_WIDTH - 60 - indexButton.getPreferredSize().width;
        laidOut.setSize(room, 0);
        final Dimension size = new Dimension(room, (int) Math.ceil(laidOut.getPreferredSpan(View.Y_AXIS)));
        indexState.setPreferredSize(size);
        indexState.setMaximumSize(size);
        downloadingIndex = now.downloading();
        indexUpdated.setText(updated > 0 && !now.downloading()
                ? TEXT.getMessage("lblWebConsolePicturesUpdated", DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(updated))) + "  " : "");
        refit();
    }

    /** The port is closed, so there is nothing to link to until the server starts again. failure is the reason a start failed, or null. */
    private void stopped(final Exception failure) {
        SwingUtilities.invokeLater(() -> {
            up = false;
            light.lit(false);
            state.setText(TEXT.getMessage(failure == null ? "lblWebConsoleStopped" : "lblWebConsoleCouldNotStart"));
            keepOpen.setVisible(false);
            startStop.setText(TEXT.getMessage("lblWebConsoleStartServer"));
            startStop.setVisible(true);
            progress.setVisible(false);
            browse.setEnabled(false);
            showHome(Map.of());
            showInternet(null, "—", false);
            netField.setVisible(true);
            startHelp.setVisible(failure != null);
            if (failure != null) {
                // A failed bind only says that the port is taken, most likely by another Forge, because desktop Forge hosts on the same port
                reword(startFailure, failure instanceof BindException ? TEXT.getMessage("lblWebConsolePortInUse", service.port())
                        : TEXT.getMessage("lblWebConsoleStartFailed"), HELP_WIDTH);
            }
            frame.getRootPane().setDefaultButton(startStop);
            refit();
        });
    }

    /** Starts or stops the server off the event thread, because binding a port and closing one both take time. */
    private void toggle() {
        final boolean stopping = up;
        if (stopping && service.playersHere() > 0 && !confirmed("lblWebConsoleStopAsk", "lblWebConsoleStopServer")) {
            return;
        }
        busy(TEXT.getMessage(stopping ? "lblWebConsoleStoppingServer" : "lblStartingServer"));
        FThreads.invokeInBackgroundThread(() -> {
            Exception failure = null;
            try {
                if (stopping) {
                    service.stop();
                } else {
                    service.start();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                // Netty rethrows a failed bind's checked exception without declaring it
                netLog.error(e, "Could not {} the server", stopping ? "stop" : "start");
                failure = stopping ? null : e;
            }
            if (service.running()) {
                running();
            } else {
                stopped(failure);
            }
        });
    }

    /** Zips Forge's logs into the Downloads folder, as desktop's Help menu does. */
    private void exportLogs() {
        FThreads.invokeInBackgroundThread(() -> {
            String outcome;
            try {
                File downloads = new File(System.getProperty("user.home"), "Downloads");
                if (!downloads.isDirectory()) {
                    downloads = new File(System.getProperty("user.home"));
                }
                final File zip = LogExporter.exportLogs(downloads);
                outcome = zip == null ? TEXT.getMessage("lblNoLogFilesFound") : TEXT.getMessage("lblSuccess") + "\n" + zip.getAbsolutePath();
            } catch (final IOException e) {
                outcome = e.toString();
            }
            final String say = outcome;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, say, TEXT.getMessage("lblExportLogs"), JOptionPane.INFORMATION_MESSAGE));
        });
    }

    /** Asks before an action that drops connected players. */
    private boolean confirmed(final String question, final String yes) {
        final String[] answers = {TEXT.getMessage(yes), TEXT.getMessage("lblCancel")};
        return JOptionPane.showOptionDialog(frame, TEXT.getMessage(question), frame.getTitle(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, answers, answers[1]) == 0;
    }

    private void showHome(final Map<String, String> found) {
        homes = found;
        moreHome.setVisible(found.size() > 1);
        home = found.isEmpty() ? null : found.entrySet().iterator().next();
        showHomeChoice();
    }

    private void showHomeChoice() {
        homeAddress.setText(home == null ? "—" : home.getKey() + "  " + home.getValue());
        copyHome.setEnabled(home != null);
        copyHome.setToolTipText(home == null ? null : service.inviteUrl(home.getValue()));
    }

    /** address is null while it is being found, when it could not be found, and while stopped. shown is the text to show in its place. */
    private void showInternet(final String address, final String shown, final boolean mayRetry) {
        internet = address;
        netAddress.setText(address == null ? shown : address);
        copyNet.setEnabled(address != null);
        copyNet.setToolTipText(address == null ? null : service.inviteUrl(address));
        retryNet.setVisible(mayRetry);
    }

    /** Call it off the event thread, because finding the address the internet sees needs its own web request. */
    private void findInternet() {
        final String found = FServerManager.getExternalAddress();
        SwingUtilities.invokeLater(() -> {
            if (up) {
                showInternet(found, TEXT.getMessage("lblWebConsoleNoAddress"), found == null);
            }
        });
    }

    private void build() {
        state.setFont(state.getFont().deriveFont(Font.BOLD, state.getFont().getSize2D() + 2f));
        keepOpen.setVisible(false);
        progress.setStringPainted(true);
        progress.setString("");
        progress.setIndeterminate(true);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        startStop = button(TEXT.getMessage("lblWebConsoleStopServer"), this::toggle);
        startStop.setVisible(false);
        startHelp = warning(startFailure, row(button(TEXT.getMessage("lblExportLogs"), this::exportLogs)));
        startHelp.setVisible(false);

        browse = button(TEXT.getMessage("lblWebConsoleOpenBrowser"), () -> WebMain.openBrowser(service.url(), ui));
        browse.setEnabled(false);

        copyHome.setEnabled(false);
        copyNet.setEnabled(false);
        moreHome.setVisible(false);
        moreHome.setToolTipText(TEXT.getMessage("lblWebConsoleOtherAddresses"));
        moreHome.getAccessibleContext().setAccessibleName(TEXT.getMessage("lblWebConsoleOtherAddresses"));
        moreHome.addActionListener(e -> {
            final JPopupMenu menu = new JPopupMenu();
            for (final Map.Entry<String, String> address : homes.entrySet()) {
                menu.add(address.getKey() + "  " + address.getValue()).addActionListener(picked -> {
                    home = address;
                    showHomeChoice();
                });
            }
            menu.show(moreHome, 0, moreHome.getHeight());
        });
        retryNet = button(TEXT.getMessage("lblTryAgain"), () -> {
            showInternet(null, TEXT.getMessage("lblWebMatchBarWorkingOutAddress"), false);
            FThreads.invokeInBackgroundThread(this::findInternet);
        });
        retryNet.setVisible(false);

        forwardPort.setEnabled(false);
        forwardPort.addActionListener(e -> {
            final boolean on = forwardPort.isSelected();
            if (!on && service.playersHere() > 1 && !confirmed("lblWebConsoleUnforwardAsk", "lblWebConsoleStopForwarding")) {
                forwardPort.setSelected(true);
                return;
            }
            router.execute(() -> service.forwardPort(on));
        });
        forwardStateRow = row(forwardLight, Box.createHorizontalStrut(6), forwardState, Box.createHorizontalGlue(), forwardPort);
        forwardLight.setVisible(false);
        refused.setForeground(WARN);
        refused.setFont(refused.getFont().deriveFont(Font.BOLD));
        forwardHelp = warning(refused, wrapped(TEXT.getMessage("lblWebConsoleRefusedAbout"), WARN_TEXT, HELP_WIDTH),
                row(button(TEXT.getMessage("lblTryAgain"), () -> router.execute(() -> service.forwardPort(true))), Box.createHorizontalStrut(8),
                        button(TEXT.getMessage("lblWebConsoleForwardedMyself"), () -> router.execute(() -> service.forwardPort(false))),
                        Box.createHorizontalStrut(12), Box.createHorizontalGlue(), link(TEXT.getMessage("lblWebConsoleSetupGuide"), () -> open(GUIDE))));
        forwardHelp.setVisible(false);

        indexBar.setVisible(false);
        indexBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        indexButton.setEnabled(false);
        indexButton.addActionListener(e -> {
            if (downloadingIndex) {
                index.cancel();
            } else {
                // Desktop and mobile then stop offering the same download
                CdnUuidCache.markBulkSyncPromptAnswered();
                index.download();
            }
        });

        quitWhenEmpty.setEnabled(false);
        quitWhenEmpty.addActionListener(e -> service.quitWhenEmpty(quitWhenEmpty.isSelected()));
        imagesButton.addActionListener(e -> images.startOrCancel());
        imagesButton.setEnabled(false);
        asLink(imagesButton);

        main = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                final Dimension size = super.getPreferredSize();
                size.width = Math.max(size.width, widest);
                return size;
            }
        };
        main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(14, 26, 12, 26));
        final Color quiet = UIManager.getColor("Label.disabledForeground");
        section(main, row(light, Box.createHorizontalStrut(10), stack(row(state, Box.createHorizontalStrut(10), players), keepOpen),
                        Box.createHorizontalGlue(), browse, Box.createHorizontalStrut(8), startStop),
                progress, startHelp);
        netField = field(netAddress, Box.createHorizontalGlue(), retryNet, Box.createHorizontalStrut(8), copyNet);
        section(main, stack(title(TEXT.getMessage("lblWebMatchBarInvite")), wrapped(TEXT.getMessage("lblWebConsoleInviteAbout"), quiet, TEXT_WIDTH)),
                muted(TEXT.getMessage("lblWebConsoleOnNetwork")),
                field(homeAddress, Box.createHorizontalGlue(), copyHome, moreHome),
                muted(TEXT.getMessage("lblWebConsoleOnInternet")),
                netField, forwardStateRow, forwardHelp);
        section(main, title(TEXT.getMessage("lblSettings")),
                stack(row(new JLabel(TEXT.getMessage("lblWebConsoleHostPassword")), Box.createHorizontalStrut(12), passwordBox(), Box.createHorizontalGlue()),
                        wrapped(TEXT.getMessage("lblWebConsoleHostPasswordAbout"), quiet, TEXT_WIDTH)),
                quitWhenEmpty);
        section(main, row(indexLight, Box.createHorizontalStrut(10),
                stack(indexState, stack(indexBar, row(indexUpdated, imagesButton))),
                Box.createHorizontalGlue(), indexButton));
        final JPanel foot = row(Box.createHorizontalGlue(), link(TEXT.getMessage("lblExportLogs"), this::exportLogs));
        // A row left centred would push every left-aligned row in by half its width
        foot.setAlignmentX(0f);
        main.add(foot);

        frame = new JFrame(TEXT.getMessage("lblWebConsoleTitle"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if (up && service.playersHere() > 0 && !confirmed("lblWebConsoleCloseAsk", "lblWebConsoleCloseForge")) {
                    return;
                }
                quit();
            }
        });
        // The rows sit at the top of a scrolling pane. They keep their own heights, and a screen that is too short can still scroll to them all
        final JPanel top = new JPanel(new BorderLayout());
        top.add(main, BorderLayout.NORTH);
        final JScrollPane scroll = new JScrollPane(top, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.setLocationByPlatform(true);
        refit();
        frame.setVisible(true);

        new Timer(1000, e -> {
            final int here = service == null ? 0 : service.playersHere();
            players.setText(up ? TEXT.getMessage("lblWebConsolePlayersConnected", here) : " ");
        }).start();
    }

    /** Sizes the window to fit what it shows. That changes as the help is shown and hidden. */
    private void refit() {
        fit();
        // The gap under a part is shown by an event that runs after the part's own event, so the window is fitted again after it
        SwingUtilities.invokeLater(this::fit);
    }

    private void fit() {
        // Use the width of the widest part, shown or not, so the window keeps one width as help is shown and hidden
        for (final Component part : main.getComponents()) {
            widest = Math.max(widest, part.getPreferredSize().width + main.getInsets().left + main.getInsets().right);
        }
        frame.pack();
        frame.setSize(frame.getWidth(), Math.min(frame.getHeight(), GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height));
    }

    /** A group's heading, a size above the labels inside the group. */
    private static JLabel title(final String words) {
        final JLabel label = heading(words);
        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() + 1f));
        return label;
    }

    /** The password box is only as wide as a password needs, not the width of the window. */
    private static JPanel passwordBox() {
        final JPanel box = field(hostPasswordField());
        box.setMaximumSize(box.getPreferredSize());
        return box;
    }

    /** Kept when the player presses Enter or leaves the field, so a half-typed password is never saved. */
    private static JPasswordField hostPasswordField() {
        final JPasswordField field = new JPasswordField(ServerControl.hostPassword(), 18);
        field.setBorder(null);
        field.setOpaque(false);
        field.getAccessibleContext().setAccessibleName(TEXT.getMessage("lblWebConsoleHostPassword"));
        final Runnable keep = () -> ServerControl.hostPassword(new String(field.getPassword()));
        field.addActionListener(e -> keep.run());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                keep.run();
            }
        });
        return field;
    }

    private void open(final String address) {
        try {
            ui.browseToUrl(address);
        } catch (final IOException | URISyntaxException e) {
            netLog.warn(e, "Open {} in a browser", address);
        }
    }

    private void quit() {
        if (quitting) {
            return;
        }
        quitting = true;
        onQuit.run();
    }

    @Override
    public void setDescription(final String s0) {
        SwingUtilities.invokeLater(() -> progress.setString(s0));
    }

    @Override
    public void setValue(final int value) {
        SwingUtilities.invokeLater(() -> progress.setValue(value));
    }

    @Override
    public void reset() {
        SwingUtilities.invokeLater(() -> progress.setValue(0));
    }

    @Override
    public void setShowETA(final boolean b0) {
    }

    @Override
    public void setShowCount(final boolean b0) {
    }

    @Override
    public void setPercentMode(final boolean percentMode0) {
    }

    @Override
    public int getMaximum() {
        return progress.getMaximum();
    }

    @Override
    public void setMaximum(final int maximum) {
        SwingUtilities.invokeLater(() -> {
            progress.setIndeterminate(maximum <= 0);
            progress.setMaximum(Math.max(1, maximum));
        });
    }

    public void close() {
        quitting = true;
        SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    /** A lamp, filled while lit and an empty ring while not. */
    private static final class Light extends JComponent {
        private static final int SIZE = 11;
        private final Color off;
        private boolean on;

        Light() {
            this(DARK);
        }

        /** off is the colour of the empty ring. Use it for a lamp whose unlit state is not an error. */
        Light(final Color off) {
            this.off = off;
            final Dimension size = new Dimension(SIZE, SIZE);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        void lit(final boolean value) {
            on = value;
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(on ? LIT : off);
            if (on) {
                g2.fillOval(0, 0, SIZE - 1, SIZE - 1);
            } else {
                g2.drawOval(0, 0, SIZE - 1, SIZE - 1);
            }
            g2.dispose();
        }
    }
}
