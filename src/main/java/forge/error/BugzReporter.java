package forge.error;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.Logger;
import org.mantisbt.connect.Enumeration;
import org.mantisbt.connect.MCException;
import org.mantisbt.connect.axis.MCSession;
import org.mantisbt.connect.model.CustomFieldValue;
import org.mantisbt.connect.model.ICustomFieldValue;
import org.mantisbt.connect.model.IIssue;
import org.mantisbt.connect.model.IMCAttribute;
import org.mantisbt.connect.model.IProjectVersion;
import org.mantisbt.connect.model.MCAttribute;
import org.mantisbt.connect.ui.DefaultSubmitter;

import forge.Singletons;
import forge.model.BuildInfo;
import forge.properties.ForgePreferences;

/**
 * The Class BugzReporter.
 */
public class BugzReporter extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = -4068301054750322783L;

    private ForgePreferences prefs = null;

    private final JPanel contentPanel = new JPanel();
    private final JTextField txtUserName;
    private final JPasswordField txtPassword = new JPasswordField();
    private final JComboBox cboCategory = new JComboBox();
    private final JTextField txtSummary;
    private final JTextArea txtDescription = new JTextArea();
    private final JTextArea txtErrorDump = new JTextArea();
    private final JComboBox cboVersion = new JComboBox();
    private final JComboBox cboSeverity = new JComboBox();

    /** The chk report anonymously. */
    private final JCheckBox chkReportAnonymously = new JCheckBox("Report Anonymously");
    private final JTextField txtSVN;
    private final JLabel lblAddInfo = new JLabel();
    private final JTextArea txtSteps = new JTextArea();

    private static BugzReporter dialog = null;

    private IMCAttribute[] severities;

    /**
     * Launch the application.
     * 
     * @param args
     *            the arguments
     */
    public static void main(final String[] args) {
        // try {
        BugzReporter.dialog.setVisible(true);
        // } catch (Exception e) {
        // System.out.println("Exception - main - " + e.getMessage());
        // }
    }

    /**
     * Sets the dump text.
     * 
     * @param dump
     *            the new dump text
     */
    public final void setDumpText(final String dump) {
        this.txtErrorDump.setText(dump);
        this.lblAddInfo.setText("Crash Report");
        this.cboCategory.setSelectedItem("New Crash Report");
    }

    /**
     * Create the dialog.
     */
    public BugzReporter() {
        BugzReporter.dialog = this;
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);

        // Init Logger for Axis, which is used by Mantis Library
        final org.apache.log4j.ConsoleAppender appCON = new org.apache.log4j.ConsoleAppender(
                new org.apache.log4j.SimpleLayout(), "System.out");

        final org.apache.log4j.Logger logAxis = Logger.getLogger("org.apache.axis");
        logAxis.addAppender(appCON);
        logAxis.setLevel(org.apache.log4j.Level.ERROR);
        // Init Logger

        // System.out.println(System.getProperties().toString().replace(", ",
        // "\n"));

        MCSession mCS = null;

        try {
            mCS = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"), "ForgeGUI",
                    "vi2ccTbfBUu^");
        } catch (final MalformedURLException e1) {
            System.out.println("MalFormedURLException");
        } catch (final MCException e1) {
            System.out.println("MCException - new MCSession");
        }

        String[] cats = {};
        try {
            cats = mCS.getCategories(1);
        } catch (final MCException e1) {
            System.out.println("MCException - getCategories - " + e1.getMessage());
        }

        try {
            this.severities = mCS.getEnum(Enumeration.SEVERITIES);
        } catch (final MCException e1) {
            System.out.println("MCException - getEnum - " + e1.getMessage());
        }

        IProjectVersion[] vers = {};
        try {
            vers = mCS.getVersions(1);
        } catch (final MCException e1) {
            System.out.println("MCException - getVersions - " + e1.getMessage());
        }

        final BuildInfo bi = Singletons.getModel().getBuildInfo();

        this.setTitle("Report Issue");
        this.setBounds(100, 100, 442, 575);
        this.getContentPane().setLayout(new BorderLayout());
        this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
        this.contentPanel.setLayout(null);
        final JLabel lblMantisUsername = new JLabel("Username");
        lblMantisUsername.setHorizontalAlignment(SwingConstants.RIGHT);
        lblMantisUsername.setBounds(10, 16, 75, 14);
        this.contentPanel.add(lblMantisUsername);
        this.txtUserName = new JTextField("ForgeGUI");
        this.txtUserName.setBounds(90, 13, 185, 21);
        this.txtUserName.setFont(new Font("Dialog", Font.PLAIN, 11));
        this.contentPanel.add(this.txtUserName);
        this.txtUserName.setColumns(4);
        try {
            this.prefs = new ForgePreferences("forge.preferences");
            if (!this.prefs.BugzName.equals("")) {
                this.txtUserName.setText(this.prefs.BugzName);
                this.txtPassword.setText(this.prefs.BugzPwd);
                this.chkReportAnonymously.setSelected(false);
            } else {
                this.chkReportAnonymously.setSelected(true);
            }
        } catch (final Exception e) {

        }
        this.chkReportAnonymously.setBounds(284, 11, 139, 25);
        this.chkReportAnonymously.setFont(new Font("Dialog", Font.PLAIN, 12));
        this.chkReportAnonymously.setHorizontalAlignment(SwingConstants.CENTER);
        this.chkReportAnonymously.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (BugzReporter.this.chkReportAnonymously.isSelected()) {
                    BugzReporter.this.txtUserName.setText("ForgeGUI");
                    BugzReporter.this.txtPassword.setText("vi2ccTbfBUu^");
                } else {
                    if (!BugzReporter.this.prefs.BugzName.equals("")) {
                        BugzReporter.this.txtUserName.setText(BugzReporter.this.prefs.BugzName);
                        BugzReporter.this.txtPassword.setText(BugzReporter.this.prefs.BugzPwd);
                    }
                }
            }
        });
        this.contentPanel.add(this.chkReportAnonymously);
        final JLabel lblMantisPassword = new JLabel("Password");
        lblMantisPassword.setHorizontalAlignment(SwingConstants.RIGHT);
        lblMantisPassword.setBounds(10, 45, 75, 14);
        this.contentPanel.add(lblMantisPassword);
        this.txtPassword.setBounds(90, 42, 185, 21);
        this.txtPassword.setFont(new Font("Dialog", Font.PLAIN, 11));
        this.contentPanel.add(this.txtPassword);
        final JSeparator separator = new JSeparator();
        separator.setBounds(10, 69, 417, 2);
        this.contentPanel.add(separator);
        final JLabel lblCategory = new JLabel("Category");
        lblCategory.setBounds(10, 81, 75, 14);
        lblCategory.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblCategory.setHorizontalAlignment(SwingConstants.RIGHT);
        this.contentPanel.add(lblCategory);
        this.cboCategory.setBounds(90, 77, 223, 22);
        this.cboCategory.setFont(new Font("Dialog", Font.BOLD, 10));
        if (cats.length > 0) {
            for (final String cat : cats) {
                this.cboCategory.addItem(cat);
            }
        }
        this.cboCategory.setSelectedItem("General Bug Report");
        this.contentPanel.add(this.cboCategory);
        final JLabel lblSummary = new JLabel("Summary");
        lblSummary.setBounds(10, 108, 75, 14);
        lblSummary.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblSummary.setHorizontalAlignment(SwingConstants.RIGHT);
        this.contentPanel.add(lblSummary);
        this.txtSummary = new JTextField();
        this.txtSummary.setBounds(90, 105, 337, 21);
        this.txtSummary.setFont(new Font("Dialog", Font.PLAIN, 11));
        this.contentPanel.add(this.txtSummary);
        this.txtSummary.setColumns(10);
        final JLabel lblDescription = new JLabel("Description");
        lblDescription.setBounds(10, 182, 75, 21);
        lblDescription.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblDescription.setHorizontalAlignment(SwingConstants.RIGHT);
        this.contentPanel.add(lblDescription);
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(90, 132, 337, 120);
        this.contentPanel.add(scrollPane);
        this.txtDescription.setFont(new Font("Dialog", Font.PLAIN, 10));
        scrollPane.setViewportView(this.txtDescription);
        this.txtDescription.setBorder(null);
        this.txtDescription.setWrapStyleWord(true);
        this.txtDescription.setLineWrap(true);
        this.txtDescription.setRows(8);
        this.lblAddInfo.setText("<html><p align=\"right\">Additional<br>Information</p></html>");
        this.lblAddInfo.setBounds(10, 294, 75, 40);
        this.lblAddInfo.setFont(new Font("Dialog", Font.PLAIN, 12));
        this.lblAddInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        this.contentPanel.add(this.lblAddInfo);
        final JScrollPane scrollPane3 = new JScrollPane();
        scrollPane3.setBounds(90, 254, 337, 120);
        this.contentPanel.add(scrollPane3);
        this.txtErrorDump.setFont(new Font("Monospaced", Font.PLAIN, 10));
        scrollPane.setViewportView(this.txtErrorDump);
        this.txtErrorDump.setAutoscrolls(false);
        this.txtErrorDump.setMaximumSize(new Dimension(2147483647, 300));
        this.txtErrorDump.setBorder(null);
        this.txtErrorDump.setLineWrap(true);
        this.txtErrorDump.setWrapStyleWord(true);
        this.txtErrorDump.setRows(8);
        final JLabel lblVersion = new JLabel("Version");
        lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        lblVersion.setBounds(20, 468, 65, 16);
        lblVersion.setFont(new Font("Dialog", Font.PLAIN, 12));
        this.contentPanel.add(lblVersion);
        this.cboVersion.setBounds(90, 465, 160, 22);
        this.cboVersion.setFont(new Font("Dialog", Font.BOLD, 10));
        this.cboVersion.addItem("");
        if (vers.length > 0) {
            for (final IProjectVersion ver : vers) {
                this.cboVersion.addItem(ver.getName());
                // System.out.println(vers[i].getName());
            }
        }
        this.cboVersion.setSelectedIndex(0);
        final String curVer = bi.getVersion();
        final String[] ss = curVer.split("-");
        final String rx = "^" + ss[0].replaceAll("\\.", "\\\\.") + ".*";
        System.out.println(ss[0] + " -> " + rx);
        if (curVer.equals("SVN")) {
            this.cboVersion.setSelectedItem("SVN");
        } else {
            for (final IProjectVersion ver : vers) {
                System.out.println(ver.getName());
                if (ver.getName().matches(rx)) {
                    System.out.println("match");
                    this.cboVersion.setSelectedItem(ver.getName());
                }
            }
        }
        this.contentPanel.add(this.cboVersion);
        final JLabel lblRev = new JLabel("SVN rev.");
        lblRev.setBounds(247, 468, 66, 16);
        lblRev.setHorizontalAlignment(SwingConstants.RIGHT);
        lblRev.setFont(new Font("Dialog", Font.PLAIN, 12));
        this.contentPanel.add(lblRev);
        this.txtSVN = new JTextField();
        final String curRev = bi.getBuildID();
        if (curRev != null) {
            if (!curRev.equals("null")) {
                this.txtSVN.setText(curRev);
            }
        }
        this.txtSVN.setBounds(318, 465, 109, 21);
        this.txtSVN.setFont(new Font("Dialog", Font.PLAIN, 11));
        this.txtSVN.setColumns(10);
        this.contentPanel.add(this.txtSVN);
        final JLabel lblSeverity = new JLabel("Severity");
        lblSeverity.setBounds(10, 496, 75, 16);
        lblSeverity.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblSeverity.setHorizontalAlignment(SwingConstants.RIGHT);
        this.contentPanel.add(lblSeverity);
        this.cboSeverity.setBounds(90, 493, 160, 22);
        this.cboSeverity.setFont(new Font("Dialog", Font.BOLD, 10));
        this.cboSeverity.addItem("");
        if (this.severities.length > 0) {
            for (final IMCAttribute severitie : this.severities) {
                this.cboSeverity.addItem(severitie.getName());
            }
        }
        this.contentPanel.add(this.cboSeverity);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setBounds(90, 380, 337, 80);
        this.contentPanel.add(scrollPane2);
        this.txtSteps.setWrapStyleWord(true);
        this.txtSteps.setRows(5);
        this.txtSteps.setMaximumSize(new Dimension(2147483647, 300));
        this.txtSteps.setLineWrap(true);
        this.txtSteps.setFont(new Font("Monospaced", Font.PLAIN, 10));
        this.txtSteps.setAutoscrolls(false);
        scrollPane.setViewportView(this.txtSteps);
        final JLabel lblSteps = new JLabel();
        lblSteps.setText("<html><p align=\"right\">Steps to<br>Reproduce</p></html>");
        lblSteps.setHorizontalAlignment(SwingConstants.RIGHT);
        lblSteps.setFont(new Font("Dialog", Font.PLAIN, 12));
        lblSteps.setBounds(10, 400, 75, 40);
        this.contentPanel.add(lblSteps);
        final JPanel buttonPane = new JPanel();
        buttonPane.setOpaque(false);
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
        final JButton cmdReport = new JButton("Report");
        cmdReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                BugzReporter.this.doReport();
            }
        });
        buttonPane.add(cmdReport);
        final JButton cmdCancel = new JButton("Cancel");
        cmdCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                BugzReporter.dialog.dispose();
            }
        });
        buttonPane.add(cmdCancel);
    }

    private void doReport() {
        Report: {

            if (this.txtSummary.getText().length() < 4) {
                JOptionPane.showMessageDialog(null, "Summary field must be provided", "Bug Report",
                        JOptionPane.ERROR_MESSAGE);
                break Report;
            }

            if (this.txtDescription.getText().length() < 10) {
                JOptionPane.showMessageDialog(null, "Description field must be provided", "Bug Report",
                        JOptionPane.ERROR_MESSAGE);
                break Report;
            }

            MCSession rep = null;
            if (!this.chkReportAnonymously.isSelected()) {
                try {
                    rep = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"),
                            this.txtUserName.getText(), String.valueOf(this.txtPassword.getPassword()));
                } catch (final MalformedURLException e) {
                    System.out.println("MalFormedURLException");
                } catch (final MCException e) {
                    System.out.println("MCException - new MCSession - " + e.getMessage());
                    JOptionPane.showMessageDialog(null, "MCException - new MCSession - " + e.getMessage(),
                            "Bug Report", JOptionPane.INFORMATION_MESSAGE);
                    break Report;
                }
            } else {
                try {
                    rep = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"), "ForgeGUI",
                            "vi2ccTbfBUu^");
                } catch (final MalformedURLException e) {
                    System.out.println("MalformedURLException");
                } catch (final MCException e) {
                    System.out.println("MCException - new MCSession - " + e.getMessage());
                    JOptionPane.showMessageDialog(null, "MCException - new MCSession - " + e.getMessage(),
                            "Bug Report", JOptionPane.INFORMATION_MESSAGE);
                    break Report;
                }
            }

            IIssue iBug = null;
            try {
                iBug = rep.newIssue(1);
            } catch (final MCException e) {
                System.out.println("MCException - newIssue - " + e.getMessage());
                JOptionPane.showMessageDialog(null, "MCException - newIssue - " + e.getMessage(), "Bug Report",
                        JOptionPane.INFORMATION_MESSAGE);
                break Report;
            }

            iBug.setCategory(this.cboCategory.getSelectedItem().toString());
            iBug.setSummary(this.txtSummary.getText());
            iBug.setDescription(this.txtDescription.getText());
            iBug.setAdditionalInformation(this.txtErrorDump.getText());
            iBug.setVersion(this.cboVersion.getSelectedItem().toString());

            for (final IMCAttribute severitie : this.severities) {
                if (this.cboSeverity.getSelectedItem().toString().equals(severitie.getName())) {
                    iBug.setSeverity(severitie);
                }
            }

            iBug.setStepsToReproduce(this.txtSteps.getText());

            final ICustomFieldValue[] icfv = { new CustomFieldValue(new MCAttribute(1, "Detected at SVN Rev"),
                    this.txtSVN.getText()) };
            iBug.setCustomFields(icfv);

            final DefaultSubmitter ds = new DefaultSubmitter(false);
            try {
                ds.submitIssue(rep, iBug);
            } catch (final MCException e1) {
                System.out.println("MCException - submit Issue - " + e1.getMessage());
                JOptionPane.showMessageDialog(null, "MCException - submit Issue - " + e1.getMessage(), "Bug Report",
                        JOptionPane.INFORMATION_MESSAGE);
                break Report;
            }

            this.prefs.BugzName = this.txtUserName.getText();
            this.prefs.BugzPwd = String.valueOf(this.txtPassword.getPassword());
            try {
                this.prefs.save();
            } catch (final Exception e) {
                System.out.println("Exception - save preferences - " + e.getMessage());
            }

            JOptionPane.showMessageDialog(null, "This Issue Has Been Reported, Thank You.", "Bug Report",
                    JOptionPane.INFORMATION_MESSAGE);
            BugzReporter.dialog.dispose();
        } // Report:

    }
}
