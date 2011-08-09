package forge.error;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

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
	private JTextField txtGit;
	
	private static BugzReporter dialog = new BugzReporter();

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
	}
	
	/**
	 * Create the dialog.
	 */
	public BugzReporter() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		
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
    	
    	IMCAttribute sevs[] = {};
    	try {
			sevs = mCS.getEnum(Enumeration.SEVERITIES);
		} catch (MCException e1) {
			System.out.println("MCException - getEnum - " + e1.getMessage());
		}
    	final IMCAttribute imCA[] = sevs;
    	
    	IProjectVersion[] vers = {};
    	try {
			vers = mCS.getVersions(1);
		} catch (MCException e1) {
			System.out.println("MCException - getVersions - " + e1.getMessage());
		}
    	
		setTitle("Bug Report");
		setBounds(100, 100, 500, 542);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("35dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu:grow"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu:grow"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("16dlu"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		{
			JLabel lblMantisUsername = new JLabel("Username");
			contentPanel.add(lblMantisUsername, "2, 2, right, default");
		}
		{
			txtUserName = new JTextField("ForgeGUI");
			txtUserName.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtUserName, "4, 2, 9, 1, fill, default");
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

			contentPanel.add(chkReportAnonymously, "14, 2, 7, 1, center, default");			
		}
		{
			JLabel lblMantisPassword = new JLabel("Password");
			contentPanel.add(lblMantisPassword, "2, 4, right, default");
		}
		{
			txtPassword.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtPassword, "4, 4, 9, 1, fill, default");
		}
		{
			JSeparator separator = new JSeparator();
			contentPanel.add(separator, "2, 6, 19, 1");
		}
		{
			JLabel lblCategory = new JLabel("Category");
			lblCategory.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblCategory.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblCategory, "2, 8");
		}
		{
			cboCategory.setFont(new Font("Dialog", Font.BOLD, 10));
			
	    	if (cats.length > 0) {
	    		for (int i=0; i<cats.length; i++)
	    			cboCategory.addItem(cats[i]);
	    	}
			
			contentPanel.add(cboCategory, "4, 8, 11, 1, fill, default");
		}
		{
			JLabel lblSummary = new JLabel("Summary");
			lblSummary.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblSummary.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblSummary, "2, 10");
		}
		{
			txtSummary = new JTextField();
			txtSummary.setFont(new Font("Dialog", Font.PLAIN, 11));
			contentPanel.add(txtSummary, "4, 10, 17, 1, fill, default");
			txtSummary.setColumns(10);
		}
		{
			JLabel lblDescription = new JLabel("Description");
			lblDescription.setFont(new Font("Tahoma", Font.BOLD, 11));
			lblDescription.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblDescription, "2, 12");
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, "4, 12, 17, 1, fill, fill");
			{
				txtDescription.setFont(new Font("Dialog", Font.PLAIN, 10));
				scrollPane.setViewportView(txtDescription);
				txtDescription.setBorder(null);
				txtDescription.setWrapStyleWord(true);
				txtDescription.setLineWrap(true);
				txtDescription.setRows(10);
			}
		}
		{
			JLabel lblNewLabel = new JLabel("Error Dump");
			lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblNewLabel, "2, 14");
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, "4, 14, 17, 1, fill, fill");
			{
				txtErrorDump.setFont(new Font("Monospaced", Font.PLAIN, 10));
				scrollPane.setViewportView(txtErrorDump);
				txtErrorDump.setAutoscrolls(false);
				txtErrorDump.setMaximumSize(new Dimension(2147483647, 300));
				txtErrorDump.setBorder(null);
				txtErrorDump.setLineWrap(true);
				txtErrorDump.setWrapStyleWord(true);
				txtErrorDump.setRows(10);
			}
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Version");
			lblNewLabel_1.setFont(new Font("Dialog", Font.PLAIN, 12));
			contentPanel.add(lblNewLabel_1, "2, 16, right, default");
		}
		{
			cboVersion.setFont(new Font("Dialog", Font.BOLD, 10));
			
			if (vers.length > 0) {
				for (int i=0; i<vers.length; i++) 
					cboVersion.addItem(vers[i].getName());
			}
			
			contentPanel.add(cboVersion, "4, 16, 9, 1, fill, default");
		}
		{
			JLabel lblGit = new JLabel("Git");
			lblGit.setHorizontalAlignment(SwingConstants.RIGHT);
			lblGit.setFont(new Font("Dialog", Font.PLAIN, 12));
			contentPanel.add(lblGit, "14, 16, right, default");
		}
		{
			txtGit = new JTextField();
			txtGit.setFont(new Font("Dialog", Font.PLAIN, 11));
		    txtGit.setColumns(10);
			contentPanel.add(txtGit, "16, 16, 5, 1, fill, default");
		}
		{
			JLabel lblSeverity = new JLabel("Severity");
			lblSeverity.setFont(new Font("Dialog", Font.PLAIN, 12));
			lblSeverity.setHorizontalAlignment(SwingConstants.RIGHT);
			contentPanel.add(lblSeverity, "2, 18");
		}
		{
		
		cboSeverity.setFont(new Font("Dialog", Font.BOLD, 10));
		
		if (imCA.length > 0) {
			for (int i=0; i<imCA.length; i++)
				cboSeverity.addItem(imCA[i].getName());
		}
		
		contentPanel.add(cboSeverity, "4, 18, 9, 1, fill, default");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cmdReport = new JButton("Report");
				cmdReport.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
Report: {						
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
						
						for (int i=0; i<imCA.length; i++) {
							if (cboSeverity.getSelectedItem().toString().equals(imCA[i].getName()))
								iBug.setSeverity(imCA[i]);
						}
						
						ICustomFieldValue icfv[] = {new CustomFieldValue(new MCAttribute(1, "Detected at Git Rev (hex7)"), txtGit.getText())};
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

}
