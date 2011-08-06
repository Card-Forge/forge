package forge;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.math.BigDecimal;
import com.cloudgarden.layout.AnchorConstraint;
import com.cloudgarden.layout.AnchorLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class GUI_DeckAnalysis extends javax.swing.JDialog {

	private static final long serialVersionUID = -8475271235196182185L;
	private JPanel jPanel1;
	private JLabel jLabelColorless;
	private JLabel jLabelMultiColor;
	private JLabel jLabelWhite;
	private JLabel jLabelSixMana;
	private JLabel jLabelFiveMana;
	private JLabel jLabelFourMana;
	private JLabel jLabelThreeMana;
	private JButton jButtonRegenerate;
	private JLabel jLabel4;
	private JSeparator jSeparator4;
	private JPanel jPanel4;
	private JList jListFirstHand;
	private JLabel jLabelTwoMana;
	private JLabel jLabelOneMana;
	private JLabel jLabelManaCost;
	private JSeparator jSeparator3;
	private JLabel jLabelZeroMana;
	private JPanel jPanel3;
	private JLabel jLabelSorcery;
	private JLabel jLabelPlaneswalker;
	private JLabel jLabelRed;
	private JLabel jLabelGreen;
	private JLabel jLabelBlue;
	private JLabel jLabelBlack;
	private JLabel jLabelEnchant;
	private JLabel jLabelLandType;
	private JLabel jLabelInstant;
	private JLabel jLabelCreature;
	private JLabel jLabel3;
	private JSeparator jSeparator2;
	private JLabel jLabelArtifact;
	private JPanel jPanel2;
	private JLabel jLabelTotal;
	private JLabel jLabelLand;
	private JSeparator jSeparator1;
	private JLabel jLabel2;
	private JButton jButtonOk;
	private JFrame jF;
	//private ButtonGroup buttonGroup1;
	
	 public CardList filterCardList;
	 public TableModel tModel;

	
	public GUI_DeckAnalysis(JFrame g, TableModel  tb) {
		super(g);		
		tModel = tb;
		jF=g;
		initGUI();		
	}
	
	private void initGUI() {
		try {
			
			AnchorLayout thisLayout = new AnchorLayout();
		getContentPane().setLayout(thisLayout);
			setVisible(true);
			int wWidth = 600;
			int wHeight = 300;
			this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));
		
			Dimension screen = getToolkit().getScreenSize();
			int x = (screen.width - wWidth) / 2;
	        int y = (screen.height - wHeight) / 2;
	        this.setBounds(x, y, wWidth, wHeight);
			this.setResizable(false);
			this.setTitle("Deck Analysis");
			pack();
			this.setIconImage(null);
			this.addWindowListener(new WListener());
			getContentPane().add(getJButton1(), new AnchorConstraint(875, 992, 953, 758, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJLabel1xx(), new AnchorConstraint(27, 783, 80, 9, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJButtonOk(), new AnchorConstraint(875, 642, 983, 354, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJPanel1(), new AnchorConstraint(102, 243, 852, 10, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJPanel2(), new AnchorConstraint(102, 490, 852, 258, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJPanel3(), new AnchorConstraint(102, 741, 852, 507, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			getContentPane().add(getJPanel4(), new AnchorConstraint(102, 992, 852, 758, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	
	
	/*
	private ButtonGroup getButtonGroup1() {
		if(buttonGroup1 == null) {
			buttonGroup1 = new ButtonGroup();
		}
		return buttonGroup1;
	}
	*/
	
	private JPanel getJPanel1() {
		if(jPanel1 == null) {
			jPanel1 = new JPanel();
			AnchorLayout jPanel1Layout = new AnchorLayout();
			jPanel1.setPreferredSize(new java.awt.Dimension(138, 201));
			jPanel1.setLayout(jPanel1Layout);
			jPanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanel1.setBackground(new java.awt.Color(192,192,192));
			jPanel1.add(getJLabel1(), new AnchorConstraint(155, 986, 223, 73, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJSeparator1(), new AnchorConstraint(107, 987, 139, 12, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel2(), new AnchorConstraint(-20, 990, 123, 16, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel3(), new AnchorConstraint(258, 988, 328, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel4(), new AnchorConstraint(366, 988, 437, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel5(), new AnchorConstraint(475, 988, 546, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel6(), new AnchorConstraint(578, 988, 649, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel7(), new AnchorConstraint(687, 988, 752, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel8(), new AnchorConstraint(796, 996, 855, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel1.add(getJLabel1x(), new AnchorConstraint(904, 988, 964, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		}
		return jPanel1;
	}
	
	private JLabel getJLabel2() {
		if(jLabel2 == null) {
			jLabel2 = new JLabel();
			jLabel2.setText("Color");
			jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel2.setFont(new java.awt.Font("Segoe UI",0,14));
			jLabel2.setPreferredSize(new java.awt.Dimension(152, 39));
			jLabel2.setLayout(null);
		}
		return jLabel2;
	}
	
	private JSeparator getJSeparator1() {
		if(jSeparator1 == null) {
			jSeparator1 = new JSeparator();
			//AnchorLayout jSeparator1Layout = new AnchorLayout();
			jSeparator1.setPreferredSize(new java.awt.Dimension(117, 6));
			jSeparator1.setLayout(null);
		}
		return jSeparator1;
	}

	private JButton getJButtonOk() {
		if(jButtonOk == null) {
			jButtonOk = new JButton();
			AnchorLayout jButtonOkLayout = new AnchorLayout();
			//AnchorLayout jButtonOkLayout = new AnchorLayout();
			jButtonOk.setLayout(jButtonOkLayout);
			jButtonOk.setText("OK");
			jButtonOk.setPreferredSize(new java.awt.Dimension(171, 29));
			jButtonOk.addMouseListener(new CustomListener());
		}
		return jButtonOk;
	}
	
	private JLabel getJLabel1() {
		if(jLabelBlack == null) {
			jLabelBlack = new JLabel();
	//		AnchorLayout jLabel1Layout = new AnchorLayout();
			jLabelBlack.setText("Black:");
			jLabelBlack.setPreferredSize(new java.awt.Dimension(105, 12));
			jLabelBlack.setLayout(null);
		}
		return jLabelBlack;
	}
	
	private JLabel getJLabel3() {
		if(jLabelBlue == null) {
			jLabelBlue = new JLabel();
	//		AnchorLayout jLabel3Layout = new AnchorLayout();
			jLabelBlue.setText("Blue:");
			jLabelBlue.setPreferredSize(new java.awt.Dimension(117, 13));
			jLabelBlue.setLayout(null);
		}
		return jLabelBlue;
	}
	
	private JLabel getJLabel4() {
		if(jLabelGreen == null) {
			jLabelGreen = new JLabel();
	//		AnchorLayout jLabel4Layout = new AnchorLayout();
			jLabelGreen.setText("Green:");
			jLabelGreen.setPreferredSize(new java.awt.Dimension(117, 13));
			jLabelGreen.setLayout(null);
		}
		return jLabelGreen;
	}
	
	private JLabel getJLabel5() {
		if(jLabelRed == null) {
			jLabelRed = new JLabel();
	//		AnchorLayout jLabel5Layout = new AnchorLayout();
			jLabelRed.setText("Red:");
			jLabelRed.setPreferredSize(new java.awt.Dimension(117, 13));
			jLabelRed.setLayout(null);
		}
		return jLabelRed;
	}
	
	private JLabel getJLabel6() {
		if(jLabelWhite == null) {
			jLabelWhite = new JLabel();
	//		AnchorLayout jLabel6Layout = new AnchorLayout();
			jLabelWhite.setText("White:");
			jLabelWhite.setPreferredSize(new java.awt.Dimension(117, 13));
			jLabelWhite.setLayout(null);
		}
		return jLabelWhite;
	}
	
	private JLabel getJLabel7() {
		if(jLabelMultiColor == null) {
			jLabelMultiColor = new JLabel();
	//		AnchorLayout jLabel7Layout = new AnchorLayout();
			jLabelMultiColor.setText("Multicolor:");
			jLabelMultiColor.setPreferredSize(new java.awt.Dimension(117, 12));
			jLabelMultiColor.setLayout(null);
		}
		return jLabelMultiColor;
	}
	
	private JLabel getJLabel8() {
		if(jLabelColorless == null) {
			jLabelColorless = new JLabel();
	//		AnchorLayout jLabel8Layout = new AnchorLayout();
			jLabelColorless.setText("Colorless:");
			jLabelColorless.setPreferredSize(new java.awt.Dimension(118, 11));
			jLabelColorless.setLayout(null);
		}
		return jLabelColorless;
	}
	
	private JLabel getJLabel1x() {
		if(jLabelLand == null) {
			jLabelLand = new JLabel();
			jLabelLand.setText("Land: ");
			jLabelLand.setPreferredSize(new java.awt.Dimension(117, 11));
			jLabelLand.setLayout(null);
		}
		return jLabelLand;
	}
	
	private JLabel getJLabel1xx() {
		if(jLabelTotal == null) {
			jLabelTotal = new JLabel();
	//		AnchorLayout jLabel1Layout = new AnchorLayout();
			jLabelTotal.setText("Information about deck:");
			jLabelTotal.setPreferredSize(new java.awt.Dimension(460, 14));
			jLabelTotal.setLayout(null);
		}
		return jLabelTotal;
	}
	
	private JPanel getJPanel2() {
		if(jPanel2 == null) {
			jPanel2 = new JPanel();
			AnchorLayout jPanel2Layout = new AnchorLayout();
			jPanel2.setBackground(new java.awt.Color(192,192,192));
			jPanel2.setPreferredSize(new java.awt.Dimension(138, 201));
			jPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanel2.setLayout(jPanel2Layout);
			jPanel2.add(getJLabel1xxx(), new AnchorConstraint(155, 986, 223, 73, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJSeparator2(), new AnchorConstraint(107, 987, 139, 12, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel3x(), new AnchorConstraint(-20, 990, 123, 16, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel4x(), new AnchorConstraint(279, 987, 350, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel5x(), new AnchorConstraint(404, 987, 475, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel6x(), new AnchorConstraint(529, 987, 600, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel7x(), new AnchorConstraint(654, 987, 725, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel8x(), new AnchorConstraint(779, 987, 845, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel2.add(getJLabel10(), new AnchorConstraint(904, 988, 964, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		}
		return jPanel2;
	}
	
	private JLabel getJLabel1xxx() {
		if(jLabelArtifact == null) {
			jLabelArtifact = new JLabel();
			jLabelArtifact.setText("Artifact:");
			jLabelArtifact.setPreferredSize(new java.awt.Dimension(105,12));
			jLabelArtifact.setLayout(null);
		}
		return jLabelArtifact;
	}
	
	private JSeparator getJSeparator2() {
		if(jSeparator2 == null) {
			jSeparator2 = new JSeparator();
			jSeparator2.setPreferredSize(new java.awt.Dimension(117,6));
			jSeparator2.setLayout(null);
		}
		return jSeparator2;
	}
	
	private JLabel getJLabel3x() {
		if(jLabel3 == null) {
			jLabel3 = new JLabel();
			jLabel3.setText("Type");
			jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel3.setFont(new java.awt.Font("Segoe UI",0,14));
			jLabel3.setPreferredSize(new java.awt.Dimension(152,39));
			jLabel3.setLayout(null);
		}
		return jLabel3;
	}
	
	private JLabel getJLabel4x() {
		if(jLabelCreature == null) {
			jLabelCreature = new JLabel();
			jLabelCreature.setText("Creature:");
			jLabelCreature.setPreferredSize(new java.awt.Dimension(111, 13));
			jLabelCreature.setLayout(null);
		}
		return jLabelCreature;
	}
	
	private JLabel getJLabel5x() {
		if(jLabelEnchant == null) {
			jLabelEnchant = new JLabel();
			jLabelEnchant.setText("Enchant:");
			jLabelEnchant.setPreferredSize(new java.awt.Dimension(111, 13));
			jLabelEnchant.setLayout(null);
		}
		return jLabelEnchant;
	}
	
	private JLabel getJLabel6x() {
		if(jLabelInstant == null) {
			jLabelInstant = new JLabel();
			jLabelInstant.setText("Instant:");
			jLabelInstant.setPreferredSize(new java.awt.Dimension(111, 13));
			jLabelInstant.setLayout(null);
		}
		return jLabelInstant;
	}
	
	private JLabel getJLabel7x() {
		if(jLabelLandType == null) {
			jLabelLandType = new JLabel();
			jLabelLandType.setText("Land:");
			jLabelLandType.setPreferredSize(new java.awt.Dimension(111, 13));
			jLabelLandType.setLayout(null);
		}
		return jLabelLandType;
	}
	
	private JLabel getJLabel8x() {
		if(jLabelPlaneswalker == null) {
			jLabelPlaneswalker = new JLabel();
			jLabelPlaneswalker.setText("Planeswalker:");
			jLabelPlaneswalker.setPreferredSize(new java.awt.Dimension(111, 12));
			jLabelPlaneswalker.setLayout(null);
		}
		return jLabelPlaneswalker;
	}

	private JLabel getJLabel10() {
		if(jLabelSorcery == null) {
			jLabelSorcery = new JLabel();
			jLabelSorcery.setText("Sorcery:");
			jLabelSorcery.setPreferredSize(new java.awt.Dimension(117,11));
			jLabelSorcery.setLayout(null);
		}
		return jLabelSorcery;
	}
	
	private JPanel getJPanel3() {
		if(jPanel3 == null) {
			jPanel3 = new JPanel();
			AnchorLayout jPanel3Layout = new AnchorLayout();
			jPanel3.setBackground(new java.awt.Color(192,192,192));
			jPanel3.setPreferredSize(new java.awt.Dimension(139, 201));
			jPanel3.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanel3.setLayout(jPanel3Layout);
			jPanel3.add(getJLabel1xxxx(), new AnchorConstraint(155, 986, 223, 73, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJSeparator3(), new AnchorConstraint(107, 987, 139, 12, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel4xx(), new AnchorConstraint(-20, 990, 123, 16, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel5xx(), new AnchorConstraint(279, 987, 350, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel6xx(), new AnchorConstraint(404, 987, 475, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel7xx(), new AnchorConstraint(529, 987, 600, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel8xx(), new AnchorConstraint(654, 987, 725, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel9(), new AnchorConstraint(779, 987, 845, 77, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel3.add(getJLabel10x(), new AnchorConstraint(904, 988, 964, 74, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		}
		return jPanel3;
	}
	
	private JLabel getJLabel1xxxx() {
		if(jLabelZeroMana == null) {
			jLabelZeroMana = new JLabel();
			jLabelZeroMana.setText("Zero mana:");
			jLabelZeroMana.setPreferredSize(new java.awt.Dimension(105,12));
			jLabelZeroMana.setLayout(null);
		}
		return jLabelZeroMana;
	}
	
	private JSeparator getJSeparator3() {
		if(jSeparator3 == null) {
			jSeparator3 = new JSeparator();
			jSeparator3.setPreferredSize(new java.awt.Dimension(117,6));
			jSeparator3.setLayout(null);
		}
		return jSeparator3;
	}
	
	private JLabel getJLabel4xx() {
		if(jLabelManaCost == null) {
			jLabelManaCost = new JLabel();
			jLabelManaCost.setText("Mana cost");
			jLabelManaCost.setHorizontalAlignment(SwingConstants.CENTER);
			jLabelManaCost.setFont(new java.awt.Font("Segoe UI",0,14));
			jLabelManaCost.setPreferredSize(new java.awt.Dimension(152,39));
			jLabelManaCost.setLayout(null);
		}
		return jLabelManaCost;
	}
	
	private JLabel getJLabel5xx() {
		if(jLabelOneMana == null) {
			jLabelOneMana = new JLabel();
			jLabelOneMana.setText("One mana:");
			jLabelOneMana.setPreferredSize(new java.awt.Dimension(111,13));
			jLabelOneMana.setLayout(null);
		}
		return jLabelOneMana;
	}
	
	private JLabel getJLabel6xx() {
		if(jLabelTwoMana == null) {
			jLabelTwoMana = new JLabel();
			jLabelTwoMana.setText("Two mana:");
			jLabelTwoMana.setPreferredSize(new java.awt.Dimension(111,13));
			jLabelTwoMana.setLayout(null);
		}
		return jLabelTwoMana;
	}
	
	private JLabel getJLabel7xx() {
		if(jLabelThreeMana == null) {
			jLabelThreeMana = new JLabel();
			jLabelThreeMana.setText("Three mana:");
			jLabelThreeMana.setPreferredSize(new java.awt.Dimension(111,13));
			jLabelThreeMana.setLayout(null);
		}
		return jLabelThreeMana;
	}
	
	private JLabel getJLabel8xx() {
		if(jLabelFourMana == null) {
			jLabelFourMana = new JLabel();
			jLabelFourMana.setText("Four mana:");
			jLabelFourMana.setPreferredSize(new java.awt.Dimension(111,13));
			jLabelFourMana.setLayout(null);
		}
		return jLabelFourMana;
	}
	
	private JLabel getJLabel9() {
		if(jLabelFiveMana == null) {
			jLabelFiveMana = new JLabel();
			jLabelFiveMana.setText("Five mana:");
			jLabelFiveMana.setPreferredSize(new java.awt.Dimension(111,12));
			jLabelFiveMana.setLayout(null);
		}
		return jLabelFiveMana;
	}
	
	private JLabel getJLabel10x() {
		if(jLabelSixMana == null) {
			jLabelSixMana = new JLabel();
			jLabelSixMana.setText("Six and more:");
			jLabelSixMana.setPreferredSize(new java.awt.Dimension(117,11));
			jLabelSixMana.setLayout(null);
		}
		return jLabelSixMana;
	}
	
	private JList getJList1() {
		CardList rList = new CardList();
		rList=tModel.getCards();
    	rList.shuffle();    	
    	ListModel jList1Model;
		if (jListFirstHand==null){
					if(rList.size()>=40){
					 jList1Model = 
						new DefaultComboBoxModel(
								new String[] { rList.getCard(0).getName(),
										       rList.getCard(1).getName(),
										       rList.getCard(2).getName(),
										       rList.getCard(3).getName(),
										       rList.getCard(4).getName(),
										       rList.getCard(5).getName(),
										       rList.getCard(6).getName()});
					jListFirstHand = new JList();
					}else{
						 jList1Model = 
							new DefaultComboBoxModel(
									new String[] {"Few cards."});
						jListFirstHand = new JList();
					}
			}else{
				if(rList.size()>=40){
					 jList1Model = 
						new DefaultComboBoxModel(
								new String[] { rList.getCard(0).getName(),
										       rList.getCard(1).getName(),
										       rList.getCard(2).getName(),
										       rList.getCard(3).getName(),
										       rList.getCard(4).getName(),
										       rList.getCard(5).getName(),
										       rList.getCard(6).getName()});
			
					}else{
						 jList1Model = 
							new DefaultComboBoxModel(
									new String[] {"Few cards."});
						
					}
			}
				
	
			//		AnchorLayout jList1Layout = new AnchorLayout();
			jListFirstHand.setModel(jList1Model);
			jListFirstHand.setPreferredSize(new java.awt.Dimension(135, 173));
			jListFirstHand.setLayout(null);
			jListFirstHand.setBackground(new java.awt.Color(192,192,192));
			jListFirstHand.setSelectionBackground(new java.awt.Color(192,192,192));
			jListFirstHand.setSelectionForeground(new java.awt.Color(0,0,0));
			jListFirstHand.setFixedCellHeight(25);
		
		return jListFirstHand;
	}
	
	private JPanel getJPanel4() {
		if(jPanel4 == null) {
			jPanel4 = new JPanel();
			AnchorLayout jPanel4Layout = new AnchorLayout();
			jPanel4.setBackground(new java.awt.Color(192,192,192));
			jPanel4.setPreferredSize(new java.awt.Dimension(139, 201));
			jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanel4.setLayout(jPanel4Layout);
			jPanel4.add(getJSeparator4(), new AnchorConstraint(104, 989, 140, 3, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel4.add(getJLabel4xxx(), new AnchorConstraint(2, 989, 108, 17, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel4.add(getJList1(), new AnchorConstraint(116, 989, 977, 17, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		}else{
			jPanel4.removeAll();
			AnchorLayout jPanel4Layout = new AnchorLayout();
			jPanel4.setBackground(new java.awt.Color(192,192,192));
			jPanel4.setPreferredSize(new java.awt.Dimension(139, 201));
			jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanel4.setLayout(jPanel4Layout);
			jPanel4.add(getJSeparator4(), new AnchorConstraint(104, 989, 140, 3, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel4.add(getJLabel4xxx(), new AnchorConstraint(2, 989, 108, 17, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			jPanel4.add(getJList1(), new AnchorConstraint(116, 989, 977, 17, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		}	
		return jPanel4;
		
	}

	private JSeparator getJSeparator4() {
		if(jSeparator4 == null) {
			jSeparator4 = new JSeparator();
			jSeparator4.setPreferredSize(new java.awt.Dimension(138, 8));
			jSeparator4.setLayout(null);
		}
		return jSeparator4;
	}
	
	private JLabel getJLabel4xxx() {
		if(jLabel4 == null) {
			jLabel4 = new JLabel();
			jLabel4.setText("Random start hand");
			jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel4.setFont(new java.awt.Font("Segoe UI",0,14));
			jLabel4.setPreferredSize(new java.awt.Dimension(136, 24));
			jLabel4.setLayout(null);
		}
		return jLabel4;
	}
	
	private JButton getJButton1() {
		CardList rList = new CardList();
		rList=tModel.getCards();
		if(jButtonRegenerate == null) {
			if(rList.size()>=40){
			jButtonRegenerate = new JButton();
	//		AnchorLayout jButton1Layout = new AnchorLayout();
			jButtonRegenerate.setLayout(null);
			jButtonRegenerate.setText("Regenerate hand");
			jButtonRegenerate.setPreferredSize(new java.awt.Dimension(139, 21));
			jButtonRegenerate.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	jButtonRegenerate_actionPerformed(e);
	            }
	        });
			}else{
			jButtonRegenerate = new JButton();
			jButtonRegenerate.setVisible(false);
			}
		}
		return jButtonRegenerate;
	}

	public class CustomListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
        	
        	
            jF.setEnabled(true);  	        		
        	dispose();
        }

        public void mouseEntered(MouseEvent e) {             
             
        }

        public void mouseExited(MouseEvent e) {             
            
        }

        public void mousePressed(MouseEvent e) {             
             
        }

        public void mouseReleased(MouseEvent e) {           
             
        }
   }
	
	public class WListener implements WindowListener {

		public void windowActivated(WindowEvent arg0) {
			
		}

		public void windowClosed(WindowEvent arg0) {
			
		}

		public void windowClosing(WindowEvent arg0) {
			
			jF.setEnabled(true);
			
		}

		public void windowDeactivated(WindowEvent arg0) {
			
		}

		public void windowDeiconified(WindowEvent arg0) {
			
		}

		public void windowIconified(WindowEvent arg0) {
			
		}

		public void windowOpened(WindowEvent arg0) {
			
			int cBlack, cBlue, cGreen, cRed, cWhite, cMulticolor, cColorless, cLand;
			int cArtifact, cCreature, cEnchant, cInstant, cLandType, cPlaneswalker, cSorcery;
			int mZero, mOne, mTwo, mThree, mFour, mFive, mSixMore;
			float tManaCost;			
        	Card c;
	        	cBlack=0;
	        	cBlue=0;
	        	cGreen=0;
	        	cRed=0;
	        	cWhite=0;
	        	cMulticolor=0;
	        	cColorless=0;
	        	cLand=0;
	        	cArtifact = 0;
	        	cCreature = 0;
	        	cEnchant = 0;
	        	cInstant = 0;
	        	cLandType = 0;
	        	cPlaneswalker = 0;
	        	cSorcery = 0;
	        	mZero =0;
	        	mOne = 0;
	        	mTwo = 0;
	        	mThree = 0;
	        	mFour = 0;
	        	mFive = 0;
	        	mSixMore = 0;
	        	tManaCost = 0;
        	CardList cList = new CardList();
        	cList=tModel.getCards();
        	for(int i=0; i<cList.size();i++){
        		c = cList.getCard(i);
        		if(CardUtil.getColors(c).size()>1){
        			cMulticolor = cMulticolor+1;
        		}else
        		{
        			if(CardUtil.getColors(c).contains(Constant.Color.Black)){
        				cBlack = cBlack+1;
        			}
        			if(CardUtil.getColors(c).contains(Constant.Color.Blue)){
        				cBlue = cBlue+1;
        			}
        			if(CardUtil.getColors(c).contains(Constant.Color.Green)){
        				cGreen = cGreen+1;
        			}
        			if(CardUtil.getColors(c).contains(Constant.Color.Red)){
        				cRed = cRed+1;
        			}
        			if(CardUtil.getColors(c).contains(Constant.Color.White)){
        				cWhite = cWhite+1;
        			}
        			if(CardUtil.getColors(c).contains(Constant.Color.Colorless)){
        				if(c.isLand()){
        					cLand = cLand+1;	
        				}else{
        					cColorless = cColorless+1;
        				}
        			}
        		}
        		
        	}
        	
        	for(int i=0; i<cList.size();i++){
        		c = cList.getCard(i);
        		if(c.isArtifact()){cArtifact = cArtifact+1;}
        		if(c.isCreature()){cCreature = cCreature+1;}
        		if(c.isEnchantment()){cEnchant = cEnchant+1;}
        		if(c.isInstant()){cInstant = cInstant+1;}
        		if(c.isLand()){cLandType = cLandType+1;}
        		if(c.isPlaneswalker()){cPlaneswalker = cPlaneswalker+1;}
        		if(c.isSorcery()){cSorcery = cSorcery+1;}
        	}
        	
        	for(int i=0; i<cList.size();i++){
        		c = cList.getCard(i);
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==0){mZero = mZero+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==1){mOne = mOne+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==2){mTwo = mTwo+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==3){mThree = mThree+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==4){mFour = mFour+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())==5){mFive = mFive+1;}
        		if(CardUtil.getConvertedManaCost(c.getManaCost())>=6){mSixMore = mSixMore+1;}
        	}
        	
        	for(int i=0; i<cList.size();i++){
        		c = cList.getCard(i);
        		tManaCost = tManaCost + CardUtil.getConvertedManaCost(c.getManaCost());
        	}
        	BigDecimal aManaCost = new BigDecimal(tManaCost/cList.size());
        	aManaCost = aManaCost.setScale(2, BigDecimal.ROUND_HALF_UP);  
        	jLabelTotal.setText("Information about deck (total cards: "+cList.size()+"):");
        	jLabelManaCost.setText("Mana cost (ACC:"+aManaCost+")");
        	Color cr = new Color(100,100,100);
        	if(cBlack==0){jLabelBlack.setForeground(cr);}
        	jLabelBlack.setText("Black: "+cBlack + " ("+cBlack*100/cList.size()+"%)");
        	if(cBlue==0){jLabelBlue.setForeground(cr);}
        	jLabelBlue.setText("Blue: " + cBlue+ " ("+cBlue*100/cList.size()+"%)");
        	if(cGreen==0){jLabelGreen.setForeground(cr);}
        	jLabelGreen.setText("Green: "+cGreen+ " ("+cGreen*100/cList.size()+"%)");
        	if(cRed==0){jLabelRed.setForeground(cr);}
        	jLabelRed.setText("Red: "+cRed+ " ("+cRed*100/cList.size()+"%)");
        	if(cWhite==0){jLabelWhite.setForeground(cr);}
        	jLabelWhite.setText("White: "+cWhite+ " ("+cWhite*100/cList.size()+"%)");
        	if(cMulticolor==0){jLabelMultiColor.setForeground(cr);}
        	jLabelMultiColor.setText("Multicolor: "+cMulticolor + " ("+cMulticolor*100/cList.size()+"%)");
        	if(cColorless==0){jLabelColorless.setForeground(cr);}
        	jLabelColorless.setText("Colorless: " + cColorless+ " ("+cColorless*100/cList.size()+"%)");
        	if(cLand==0){jLabelLand.setForeground(cr);}
        	jLabelLand.setText("Land: "+ cLand+ " ("+cLand*100/cList.size()+"%)");
        	if(cArtifact==0){jLabelArtifact.setForeground(cr);}
        	jLabelArtifact.setText("Artifact: " + cArtifact+ " ("+cArtifact*100/cList.size()+"%)");
        	if(cCreature==0){jLabelCreature.setForeground(cr);}
        	jLabelCreature.setText("Creature: "+ cCreature+ " ("+cCreature*100/cList.size()+"%)");
        	if(cEnchant==0){jLabelEnchant.setForeground(cr);}
        	jLabelEnchant.setText("Enchant: "+ cEnchant+ " ("+cEnchant*100/cList.size()+"%)");
        	if(cInstant==0){jLabelInstant.setForeground(cr);}
        	jLabelInstant.setText("Instant: "+ cInstant+ " ("+cInstant*100/cList.size()+"%)");
        	if(cLandType==0){jLabelLandType.setForeground(cr);}
        	jLabelLandType.setText("Land: "+ cLandType+ " ("+cLandType*100/cList.size()+"%)");
        	if(cPlaneswalker==0){jLabelPlaneswalker.setForeground(cr);}
        	jLabelPlaneswalker.setText("Planeswalker: " + cPlaneswalker+ " ("+cPlaneswalker*100/cList.size()+"%)");
        	if(cSorcery==0){jLabelSorcery.setForeground(cr);}
        	jLabelSorcery.setText("Sorcery: "+ cSorcery+ " ("+cSorcery*100/cList.size()+"%)");
        	if(mZero==0){jLabelZeroMana.setForeground(cr);}
        	jLabelZeroMana.setText("Zero mana: "+ mZero+ " ("+mZero*100/cList.size()+"%)");
        	if(mOne==0){jLabelOneMana.setForeground(cr);}
        	jLabelOneMana.setText("One mana: "+ mOne+ " ("+mOne*100/cList.size()+"%)");
        	if(mTwo==0){jLabelTwoMana.setForeground(cr);}
        	jLabelTwoMana.setText("Two mana: "+ mTwo+ " ("+mTwo*100/cList.size()+"%)");
        	if(mThree==0){jLabelThreeMana.setForeground(cr);}
        	jLabelThreeMana.setText("Three mana :"+ mThree+ " ("+mThree*100/cList.size()+"%)");
        	if(mFour==0){jLabelFourMana.setForeground(cr);}
        	jLabelFourMana.setText("Four mana: "+ mFour+ " ("+mFour*100/cList.size()+"%)");
        	if(mFive==0){jLabelFiveMana.setForeground(cr);}
        	jLabelFiveMana.setText("Five mana: "+ mFive+ " ("+mFive*100/cList.size()+"%)");
        	if(mSixMore==0){jLabelSixMana.setForeground(cr);}
        	jLabelSixMana.setText("Six and more: "+ mSixMore+ " ("+mSixMore*100/cList.size()+"%)");
		}
	
	}

	void jButtonRegenerate_actionPerformed(ActionEvent e) {
		getContentPane().removeAll();
		getContentPane().add(getJButton1(), new AnchorConstraint(875, 992, 953, 758, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJLabel1xx(), new AnchorConstraint(27, 783, 80, 9, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJButtonOk(), new AnchorConstraint(875, 642, 983, 354, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJPanel1(), new AnchorConstraint(102, 243, 852, 10, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJPanel2(), new AnchorConstraint(102, 490, 852, 258, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJPanel3(), new AnchorConstraint(102, 741, 852, 507, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().add(getJPanel4(), new AnchorConstraint(102, 992, 852, 758, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
		getContentPane().repaint();
		
	}
	
}
