package forge.error;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import forge.Singletons;
import forge.model.BuildInfo;
import forge.properties.ForgePreferences;

import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import java.awt.Dimension;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;
import org.mantisbt.connect.MCException;
import org.mantisbt.connect.axis.MCSession;
import org.mantisbt.connect.model.CustomFieldValue;
import org.mantisbt.connect.model.ICustomFieldValue;
import org.mantisbt.connect.model.IIssue;
import org.mantisbt.connect.model.IMCAttribute;
import org.mantisbt.connect.model.IProjectVersion;
import org.mantisbt.connect.model.MCAttribute;
import org.mantisbt.connect.ui.DefaultSubmitter;
import org.mantisbt.connect.Enumeration;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.awt.event.ActionListener;

public class BugzReporter extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4068301054750322783L;

	private ForgePreferences prefs= null;
	
	private final JPanel contentPanel = new JPanel();
	private JTextField txtUserName;
	private JPasswordField txtPassword = new JPasswordField();
	private JComboBox cboCategory = new JComboBox();
	private JTextField txtSummary;
	private JTextArea txtDescription = new JTextArea();
	private JTextArea txtErrorDump = new JTextArea();
	private JComboBox cboVersion = new JComboBox();
	private JComboBox cboSeverity = new JComboBox();
	final JCheckBox chkReportAnonymously = new JCheckBox("Report Anonymously");
	private JTextField txtSVN;
	private JLabel lblAddInfo = new JLabel();
	private JTextArea txtSteps = new JTextArea();
	
	private static BugzReporter dialog = null;
	
	private IMCAttribute Severities[];

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		//try {
			dialog.setVisible(true);
		//} catch (Exception e) {
			//System.out.println("Exception - main - " + e.getMessage());
		//}
	}

	public void setDumpText(String dump) {
		txtErrorDump.setText(dump);
		lblAddInfo.setText("Crash Report");
		cboCategory.setSelectedItem("New Crash Report");
	}
	
	/**
	 * Create the dialog.
	 */
	public BugzReporter() {
	    dialog = this;
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		
        // Init Logger for Axis, which is used by Mantis Library
		org.apache.log4j.ConsoleAppender appCON = new org.apache.log4j.ConsoleAppender(new org.apache.log4j.SimpleLayout(), "System.out");

        org.apache.log4j.Logger logAxis= Logger.getLogger("org.apache.axis");
        logAxis.addAppender(appCON);
        logAxis.setLevel(org.apache.log4j.Level.ERROR);
        // Init Logger
		
        //System.out.println(System.getProperties().toString().replace(", ", "\n"));
        
    	MCSession mCS = null;
    	
    	try {
			 mCS = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"), "ForgeGUI", "vi2ccTbfBUu^");
		} catch (MalformedURLException e1) {
			System.out.println("MalFormedURLException");
		} catch (MCException e1) {
			System.out.println("MCException - new MCSession");
		}

    	String cats[] = {};
    	try {
			cats = mCS.getCategories(1);
		} catch (MCException e1) {
			System.out.println("MCException - getCategories - " + e1.getMessage());
		}
    	
    	try {
			Severities = mCS.getEnum(Enumeration.SEVERITIES);
		} catch (MCException e1) {
			System.out.println("MCException - getEnum - " + e1.getMessage());
		}
    	
    	IProjectVersion[] vers = {};
    	try {
			vers = mCS.getVersions(1);
		} catch (MCException e1) {
			System.out.println("MCException - getVersions - " + e1.getMessage());
		}
    	
    	BuildInfo bi = Singletons.getModel().getBuildInfo();
    	
		setTitle("Report Issue");
		setBounds(100, 100, 442, 575);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblMantisUsername = new JLabel("Username");
			lblMantisUsername.setHorizontalAlignment(SwingConstants.RIGHT);
			lblMantisUsername.setBounds(10, 16, 75, 14);
			contentPanel.add(lblMantisUsername);
		}
		{
			txtUserName = new JTextField("ForgeGUI");
			txtUserName.setBounds(90, 13, 185, 21);
			txtUserName.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtUserName);
			txtUserName.setColumns(4);
			
			try {
				prefs = new ForgePreferences("forge.preferences");
				if (!prefs.BugzName.equals("")) {
					txtUserName.setText(prefs.BugzName);
					txtPassword.setText(prefs.BugzPwd);
					chkReportAnonymously.setSelected(false);
				}
				else
					chkReportAnonymously.setSelected(true);
			} catch (Exception e) {
				
			}
		}
		{
			chkReportAnonymously.setBounds(284, 11, 139, 25);
			chkReportAnonymously.setFont(new Font("Dialog", Font.PLAIN, 12));
			chkReportAnonymously.setHorizontalAlignment(SwingConstants.CENTER);

			chkReportAnonymously.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                if (chkReportAnonymously.isSelected()) {
	                	txtUserName.setText("ForgeGUI");
	                	txtPassword.setText("vi2ccTbfBUu^");
	                }
	                else {
	    				if (!prefs.BugzName.equals("")) {
	    					txtUserName.setText(prefs.BugzName);
	    					txtPassword.setText(prefs.BugzPwd);
	    				}
	                }
	            }
	        });

			contentPanel.add(chkReportAnonymously);			
		}
		{
			JLabel lblMantisPassword = new JLabel("Password");
			lblMantisPassword.setHorizontalAlignment(SwingConstants.RIGHT);
			lblMantisPassword.setBounds(10, 45, 75, 14);
			contentPanel.add(lblMantisPassword);
		}
		{
			txtPassword.setBounds(90, 42, 185, 21);
			txtPassword.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtPassword);
		}
		{
			JSeparator separator = new JSeparator();
			separator.setBounds(10, 69, 417, 2);
			contentPanel.add(separator);
		}
		{
			JLabel lblCategory = new JLabel("Category");
			lblCategory.setBounds(10, 81, 75, 14);
			lblCategory.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblCategory.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblCategory);
		}
		{
			cboCategory.setBounds(90, 77, 223, 22);
			cboCategory.setFont(new Font("Dialog", Font.BOLD, 10));
			
	    	if (cats.length > 0) {
	    		for (int i=0; i<cats.length; i++)
	    			cboCategory.addItem(cats[i]);
	    	}
	    	
	    	cboCategory.setSelectedItem("General Bug Report");
			
			contentPanel.add(cboCategory);
		}
		{
			JLabel lblSummary = new JLabel("Summary");
			lblSummary.setBounds(10, 108, 75, 14);
			lblSummary.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblSummary.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblSummary);
		}
		{
			txtSummary = new JTextField();
			txtSummary.setBounds(90, 105, 337, 21);
			txtSummary.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtSummary);
			txtSummary.setColumns(10);
		}
		{
			JLabel lblDescription = new JLabel("Description");
			lblDescription.setBounds(10, 182, 75, 21);
			lblDescription.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblDescription.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblDescription);
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setBounds(90, 132, 337, 120);
			contentPanel.add(scrollPane);
			{
				txtDescription.setFont(new Font("Dialog", Font.PLAIN, 10));
				scrollPane.setViewportView(txtDescription);
				txtDescription.setBorder(null);
				txtDescription.setWrapStyleWord(true);
				txtDescription.setLineWrap(true);
				txtDescription.setRows(8);
			}
		}
		{
			lblAddInfo.setText("<html><p align=\"right\">Additional<br>Information</p></html>");
			lblAddInfo.setBounds(10, 294, 75, 40);
			lblAddInfo.setFont(new Font("Dialog", Font.PLAIN, 12));
			lblAddInfo.setHorizontalAlignment(SwingConstants.RIGHT);
			
			contentPanel.add(lblAddInfo);
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setBounds(90, 254, 337, 120);
			contentPanel.add(scrollPane);
			{
				txtErrorDump.setFont(new Font("Monospaced", Font.PLAIN, 10));
				scrollPane.setViewportView(txtErrorDump);
				txtErrorDump.setAutoscrolls(false);
				txtErrorDump.setMaximumSize(new Dimension(2147483647, 300));
				txtErrorDump.setBorder(null);
				txtErrorDump.setLineWrap(true);
				txtErrorDump.setWrapStyleWord(true);
				txtErrorDump.setRows(8);
			}
		}
		{
			JLabel lblVersion = new JLabel("Version");
			lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
			lblVersion.setBounds(20, 468, 65, 16);
			lblVersion.setFont(new Font("Dialog", Font.PLAIN, 12));
			contentPanel.add(lblVersion);
		}
		{
			cboVersion.setBounds(90, 465, 160, 22);
			cboVersion.setFont(new Font("Dialog", Font.BOLD, 10));
			
			cboVersion.addItem("");
			if (vers.length > 0) {
				for (int i=0; i<vers.length; i++) { 
					cboVersion.addItem(vers[i].getName());
					//System.out.println(vers[i].getName());
				}
			}
			cboVersion.setSelectedIndex(0);
			
			String curVer = bi.getVersion();
			String ss[] = curVer.split("-");
            String rx = "^" + ss[0].replaceAll("\\.", "\\\\.") + ".*";
            System.out.println(ss[0] + " -> " + rx);
            
			if (curVer.equals("SVN")) {
			    cboVersion.setSelectedItem("SVN");
			} else {
			    for (int i=0; i<vers.length; i++) {
			        System.out.println(vers[i].getName());
			        if (vers[i].getName().matches(rx)) {
			            System.out.println("match");
			            cboVersion.setSelectedItem(vers[i].getName());
			        }
			    }
			}
			
			contentPanel.add(cboVersion);			
		}
		{
			JLabel lblRev = new JLabel("SVN rev.");
			lblRev.setBounds(247, 468, 66, 16);
			lblRev.setHorizontalAlignment(SwingConstants.RIGHT);
			lblRev.setFont(new Font("Dialog", Font.PLAIN, 12));
			contentPanel.add(lblRev);
		}
		{
			txtSVN = new JTextField();
			
			String curRev = bi.getBuildID();
			if (curRev != null) {
			    if (!curRev.equals("null"))
			        txtSVN.setText(curRev);
			}
			txtSVN.setBounds(318, 465, 109, 21);
			txtSVN.setFont(new Font("Dialog", Font.PLAIN, 11));
		    txtSVN.setColumns(10);
			contentPanel.add(txtSVN);
		}
		{
			JLabel lblSeverity = new JLabel("Severity");
			lblSeverity.setBounds(10, 496, 75, 16);
			lblSeverity.setFont(new Font("Dialog", Font.PLAIN, 12));
			lblSeverity.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblSeverity);
		}
		{
		cboSeverity.setBounds(90, 493, 160, 22);
		cboSeverity.setFont(new Font("Dialog", Font.BOLD, 10));
		cboSeverity.addItem("");
		
		if (Severities.length > 0) {
			for (int i=0; i<Severities.length; i++)
				cboSeverity.addItem(Severities[i].getName());
		}
		
		contentPanel.add(cboSeverity);
		}
		{
		    JScrollPane scrollPane = new JScrollPane();
		    scrollPane.setBounds(90, 380, 337, 80);
		    contentPanel.add(scrollPane);
		    {
		        txtSteps.setWrapStyleWord(true);
		        txtSteps.setRows(5);
		        txtSteps.setMaximumSize(new Dimension(2147483647, 300));
		        txtSteps.setLineWrap(true);
		        txtSteps.setFont(new Font("Monospaced", Font.PLAIN, 10));
		        txtSteps.setAutoscrolls(false);
		        scrollPane.setViewportView(txtSteps);
		    }
		}
		{
		    JLabel lblSteps = new JLabel();
		    lblSteps.setText("<html><p align=\"right\">Steps to<br>Reproduce</p></html>");
		    lblSteps.setHorizontalAlignment(SwingConstants.RIGHT);
		    lblSteps.setFont(new Font("Dialog", Font.PLAIN, 12));
		    lblSteps.setBounds(10, 400, 75, 40);
		    contentPanel.add(lblSteps);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setOpaque(false);
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cmdReport = new JButton("Report");
				cmdReport.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
					    doReport();
					}
				});
				
				buttonPane.add(cmdReport);
			}
			{
				JButton cmdCancel = new JButton("Cancel");
				
				cmdCancel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dialog.dispose();
					}
				});
				
				buttonPane.add(cmdCancel);
			}
		}
	}
	
	private void doReport() {
	    Report: {
        
	    if (txtSummary.getText().length() < 4) {
	        JOptionPane.showMessageDialog(null, "Summary field must be provided", "Bug Report", JOptionPane.ERROR_MESSAGE);
	        break Report;
	    }
	    
	    if (txtDescription.getText().length() < 10) {
	        JOptionPane.showMessageDialog(null, "Description field must be provided", "Bug Report", JOptionPane.ERROR_MESSAGE);
	        break Report;
	    }
	    
	    MCSession rep = null;
        if (!chkReportAnonymously.isSelected()) {            
            try {
                 rep = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"), txtUserName.getText(), String.valueOf(txtPassword.getPassword()));
            } catch (MalformedURLException e) {
                System.out.println("MalFormedURLException");
            } catch (MCException e) {
                System.out.println("MCException - new MCSession - " + e.getMessage());
                JOptionPane.showMessageDialog(null, "MCException - new MCSession - " + e.getMessage(), "Bug Report", JOptionPane.INFORMATION_MESSAGE);
                break Report;
            }
        } else {
            try {
                rep = new MCSession(new URL("http://cardforge.org/bugz/api/soap/mantisconnect.php"), "ForgeGUI", "vi2ccTbfBUu^");
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException");
            } catch (MCException e) {
                System.out.println("MCException - new MCSession - " + e.getMessage());
                JOptionPane.showMessageDialog(null,  "MCException - new MCSession - " + e.getMessage(), "Bug Report", JOptionPane.INFORMATION_MESSAGE);
                break Report;
            }
        }
        
        IIssue iBug = null;
        try {
            iBug = rep.newIssue(1);
        } catch (MCException e) {
            System.out.println("MCException - newIssue - " + e.getMessage());
            JOptionPane.showMessageDialog(null, "MCException - newIssue - " + e.getMessage(), "Bug Report", JOptionPane.INFORMATION_MESSAGE);
            break Report;
        }
        
        iBug.setCategory(cboCategory.getSelectedItem().toString());
        iBug.setSummary(txtSummary.getText());
        iBug.setDescription(txtDescription.getText());
        iBug.setAdditionalInformation(txtErrorDump.getText());
        iBug.setVersion(cboVersion.getSelectedItem().toString());
        
        for (int i=0; i<Severities.length; i++) {
            if (cboSeverity.getSelectedItem().toString().equals(Severities[i].getName()))
                iBug.setSeverity(Severities[i]);
        }
        
        iBug.setStepsToReproduce(txtSteps.getText());
        
        ICustomFieldValue icfv[] = {
                new CustomFieldValue(new MCAttribute(1, "Detected at SVN Rev"), txtSVN.getText())
                };
        iBug.setCustomFields(icfv);
        
        DefaultSubmitter ds = new DefaultSubmitter(false);
        try {
            ds.submitIssue(rep, iBug);
        } catch (MCException e1) {
            System.out.println("MCException - submit Issue - " + e1.getMessage());
            JOptionPane.showMessageDialog(null, "MCException - submit Issue - " + e1.getMessage(), "Bug Report", JOptionPane.INFORMATION_MESSAGE);
            break Report;
        }
        
        prefs.BugzName = txtUserName.getText();
        prefs.BugzPwd = String.valueOf(txtPassword.getPassword());
        try {
            prefs.save();
        } catch (Exception e) {
            System.out.println("Exception - save preferences - " + e.getMessage());
        }
        
        JOptionPane.showMessageDialog(null, "This Issue Has Been Reported, Thank You.", "Bug Report", JOptionPane.INFORMATION_MESSAGE);
        dialog.dispose();
}// Report:             

	}
}
