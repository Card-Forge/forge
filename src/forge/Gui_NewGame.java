package forge;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.esotericsoftware.minlog.Log;

import arcane.ui.util.ManaSymbols;
import arcane.util.MultiplexOutputStream;

import net.miginfocom.swing.MigLayout;

import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.gui.ListChooser;
import forge.properties.ForgePreferences;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.StackOffsetType;
import forge.properties.NewConstants.LANG.Gui_NewGame.MENU_BAR.MENU;
import forge.properties.NewConstants.LANG.Gui_NewGame.MENU_BAR.OPTIONS;


public class Gui_NewGame extends JFrame implements NewConstants, NewConstants.LANG.Gui_NewGame {
    private static final long       serialVersionUID     = -2437047615019135648L;
    
//    private final DeckIO            deckIO               = new OldDeckIO(ForgeProps.getFile(DECKS));
//    private final DeckIO            boosterDeckIO        = new OldDeckIO(ForgeProps.getFile(BOOSTER_DECKS));
    private final DeckIO            deckIO               = new NewDeckIO(ForgeProps.getFile(NEW_DECKS));
    //with the new IO, there's no reason to use different instances
    private final DeckIO            boosterDeckIO        = deckIO;
    private ArrayList<Deck>         allDecks;
    private static Gui_DeckEditor   editor;
    
    private JLabel                  titleLabel           = new JLabel();
    private JLabel                  jLabel2              = new JLabel();
    private JLabel                  jLabel3              = new JLabel();
    private JComboBox               humanComboBox        = new JComboBox();
    private JComboBox               computerComboBox     = new JComboBox();
    private JButton                 deckEditorButton     = new JButton();
    private JButton                 startButton          = new JButton();
    private ButtonGroup             buttonGroup1         = new ButtonGroup();
    private JRadioButton            sealedRadioButton    = new JRadioButton();
    private JRadioButton            singleRadioButton    = new JRadioButton();
    private JPanel                  jPanel2              = new JPanel();
    private Border                  border1;
    private TitledBorder            titledBorder1;
    private JRadioButton            draftRadioButton     = new JRadioButton();
    private JPanel                  jPanel1              = new JPanel();
    private Border                  border2;
    private JPanel                  jPanel3              = new JPanel();
    private Border                  border3;
    private TitledBorder            titledBorder3;
    // @SuppressWarnings("unused")
    // titledBorder2
    private TitledBorder            titledBorder2;
    private static JCheckBox        newGuiCheckBox       = new JCheckBox("", true);
    private static JCheckBox        smoothLandCheckBox   = new JCheckBox("", false);
    private static JCheckBox        millLoseCheckBox     = new JCheckBox("", true);
    
    // GenerateConstructedDeck.get2Colors() and GenerateSealedDeck.get2Colors()
    // use these two variables
    public static JCheckBoxMenuItem removeSmallCreatures = new JCheckBoxMenuItem(
                                                                 ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.REMOVE_SMALL));
    public static JCheckBoxMenuItem removeArtifacts      = new JCheckBoxMenuItem(
                                                                 ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.REMOVE_ARTIFACTS));
    public static JCheckBoxMenuItem useLAFFonts          = new JCheckBoxMenuItem(
                                                                 ForgeProps.getLocalized(MENU_BAR.OPTIONS.FONT));
    public static JCheckBoxMenuItem cardOverlay          = new JCheckBoxMenuItem(
            													 ForgeProps.getLocalized(MENU_BAR.OPTIONS.CARD_OVERLAY));
    public static JCheckBoxMenuItem cardScale          = new JCheckBoxMenuItem(
			                                                     ForgeProps.getLocalized(MENU_BAR.OPTIONS.CARD_SCALE));
    private JButton                 questButton          = new JButton();
    
    private Action                  LOOK_AND_FEEL_ACTION = new LookAndFeelAction(this);
    private Action                  DOWNLOAD_ACTION      = new DownloadAction();
    private Action                  DOWNLOAD_ACTION_LQ   = new DownloadActionLQ();
    private Action                  IMPORT_PICTURE       = new ImportPictureAction();
    private Action                  CARD_SIZES_ACTION    = new CardSizesAction();
    private Action					CARD_STACK_ACTION    = new CardStackAction();
    private Action					CARD_STACK_OFFSET_ACTION = new CardStackOffsetAction();
    private Action                  ABOUT_ACTION         = new AboutAction();
    private Action					DNLD_PRICES_ACTION	 = new DownloadPriceAction(); 
    
    static public ForgePreferences preferences;
    public static void main(String[] args) {
        ExceptionHandler.registerErrorHandling();
        File logFile = new File("forge.log");
		logFile.delete();
		try {
			OutputStream logFileStream = new BufferedOutputStream(new FileOutputStream(logFile));
			System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
			System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));
		} catch (FileNotFoundException ex) {
			ErrorViewer.showError(ex);
		}
        try {
        	preferences = new ForgePreferences("forge.preferences");
        	useLAFFonts.setSelected(preferences.lafFonts);
        	newGuiCheckBox.setSelected(preferences.newGui);
        	smoothLandCheckBox.setSelected(preferences.stackAiLand);
        	millLoseCheckBox.setSelected(preferences.millingLossCondition);
        	cardOverlay.setSelected(preferences.cardOverlay);
        	ImageCache.scaleLargerThanOriginal = preferences.scaleLargerThanOriginal;
        	cardScale.setSelected(preferences.scaleLargerThanOriginal);
        	CardStackOffsetAction.set(preferences.stackOffset);
        	CardStackAction.setVal(preferences.maxStackSize);
        	CardSizesAction.set(preferences.cardSize);
		} catch (Exception e) {
			Log.error("Error loading preferences");
		}
		
		SwingUtilities.invokeLater(new Runnable() {		
			public void run() {
		        try {
		        	if(preferences.laf.equals(""))
		        		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		        	else
		        		UIManager.setLookAndFeel(preferences.laf);
		        } catch(Exception ex) {
		            ErrorViewer.showError(ex);
		        }
			}
		});
		
        try {
            //deck migration - this is a little hard to read, because i can't just plainly reference a class in the
            //default package
            Class<?> deckConverterClass = Class.forName("DeckConverter");
            //invoke public static void main(String[] args) of DeckConverter
            deckConverterClass.getDeclaredMethod("main", String[].class).invoke(null, (Object) null);
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        try {
            Constant.Runtime.GameType[0] = Constant.GameType.Constructed;
            SwingUtilities.invokeLater(new Runnable() {   			
    			public void run() {
    				AllZone.Computer = new ComputerAI_Input(new ComputerAI_General());
    				new Gui_NewGame();
    			}
    		});
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    public Gui_NewGame() {

        AllZone.QuestData = null;
        allDecks = getDecks();
        Constant.Runtime.WinLose.reset();
        
        if(Constant.Runtime.width[0] == 0) Constant.Runtime.width[0] = 70;
        
        if(Constant.Runtime.height[0] == 0) Constant.Runtime.height[0] = 98;
        
        if(Constant.Runtime.stackSize[0] == 0) Constant.Runtime.stackSize[0] = 4;
        
        if(Constant.Runtime.stackOffset[0] == 0) Constant.Runtime.stackOffset[0] = 10;

        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        if(Constant.Runtime.GameType[0].equals(Constant.GameType.Constructed)) {
            singleRadioButton.setSelected(true);
            updateDeckComboBoxes();
        }
        if(Constant.Runtime.GameType[0].equals(Constant.GameType.Sealed)) {
            sealedRadioButton.setSelected(true);
            updateDeckComboBoxes();
        }
        if(Constant.Runtime.GameType[0].equals(Constant.GameType.Draft)) {
            draftRadioButton.setSelected(true);
            draftRadioButton_actionPerformed(null);
        }
        
        addListeners();
        
        Dimension screen = getToolkit().getScreenSize();
        Rectangle bounds = getBounds();
        bounds.width = 550;
        bounds.height = 553;
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        setBounds(bounds);
        
        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        setupMenu();
        setVisible(true);
        
        ManaSymbols.loadImages();
        Log.WARN(); //set logging level to warn
    	SwingUtilities.updateComponentTreeUI(this);
    }// init()
    
    private void setupMenu() {
        Action[] actions = {
                LOOK_AND_FEEL_ACTION, DNLD_PRICES_ACTION, DOWNLOAD_ACTION, DOWNLOAD_ACTION_LQ, IMPORT_PICTURE, CARD_SIZES_ACTION,
                CARD_STACK_ACTION, CARD_STACK_OFFSET_ACTION, ErrorViewer.ALL_THREADS_ACTION, ABOUT_ACTION};
        JMenu menu = new JMenu(ForgeProps.getLocalized(MENU.TITLE));
        for (Action a:actions) {
            menu.add(a);
            if (a.equals(LOOK_AND_FEEL_ACTION)
                    || a.equals(IMPORT_PICTURE)
                    || a.equals(CARD_STACK_OFFSET_ACTION)
                    || a.equals(ErrorViewer.ALL_THREADS_ACTION)) {
                menu.addSeparator();
            }
        }
        
        //useLAFFonts.setSelected(false);
        
        // new stuff
        JMenu generatedDeck = new JMenu(ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.TITLE));
        generatedDeck.add(removeSmallCreatures);
        generatedDeck.add(removeArtifacts);
        JMenu optionsMenu = new JMenu(ForgeProps.getLocalized(OPTIONS.TITLE));
        optionsMenu.add(generatedDeck);
        
        optionsMenu.add(useLAFFonts);
        optionsMenu.addSeparator();
        optionsMenu.add(cardOverlay);
        optionsMenu.add(cardScale);
        
        cardScale.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent arg0) {
				ImageCache.scaleLargerThanOriginal = cardScale.isSelected();			
			}
		});
        
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        bar.add(optionsMenu);
        bar.add(new MenuItem_HowToPlay());
        
        setJMenuBar(bar);
    }
    
    
    // returns, ArrayList of Deck objects
    private ArrayList<Deck> getDecks() {
        ArrayList<Deck> list = new ArrayList<Deck>();
        Deck[] deck = deckIO.getDecks();
        for(int i = 0; i < deck.length; i++)
            list.add(deck[i]);
        
        Collections.sort(list, new DeckSort());
        return list;
    }
    
    private void addListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                System.exit(0);
            }
        });
        
        questButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // close this windows
                // can't use this.dispose() because "this" object is an
                // ActionListener
                Gui_NewGame.this.dispose();
                
                new Gui_QuestOptions();
            }
        });
    }// addListeners()
    
    @SuppressWarnings("unused")
    // setupSealed
    private void setupSealed() {
        Deck deck = new Deck(Constant.GameType.Sealed);
        
        ReadBoosterPack booster = new ReadBoosterPack();
        CardList pack = booster.getBoosterPack5();
        
        for(int i = 0; i < pack.size(); i++)
            deck.addSideboard(pack.get(i).getName());
        
        Constant.Runtime.HumanDeck[0] = deck;
    }
    
    private void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(border1, ForgeProps.getLocalized(NEW_GAME_TEXT.GAMETYPE));
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, ForgeProps.getLocalized(NEW_GAME_TEXT.LIBRARY));
        border3 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder3 = new TitledBorder(border3, ForgeProps.getLocalized(NEW_GAME_TEXT.SETTINGS));
        titleLabel.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.NEW_GAME));
        titleLabel.setFont(new java.awt.Font("Dialog", 0, 26));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.getContentPane().setLayout(new MigLayout("fill"));

        /*
         *  Game Type Panel
         */
        
        jPanel2.setBorder(titledBorder1);
        jPanel2.setLayout(new MigLayout("align center"));
        
        singleRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.CONSTRUCTED_TEXT));
        singleRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                singleRadioButton_actionPerformed(e);
            }
        });
        
        sealedRadioButton.setToolTipText("");
        sealedRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.SEALED_TEXT));
        sealedRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sealedRadioButton_actionPerformed(e);
            }
        });
        
        draftRadioButton.setToolTipText("");
        draftRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.BOOSTER_TEXT));
        draftRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                draftRadioButton_actionPerformed(e);
            }
        });
        
        /*
         *  Library Panel
         */
        
        jPanel1.setBorder(titledBorder2);
        jPanel1.setLayout(new MigLayout("align center"));
        
        jLabel2.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.YOURDECK));
        jLabel3.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.OPPONENT));
        

        humanComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                humanComboBox_actionPerformed(e);
            }
        });

        /*
         *  Settings Panel
         */
        
        jPanel3.setBorder(titledBorder3);
        jPanel3.setLayout(new MigLayout("align center"));
        
        newGuiCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.NEW_GUI));
        smoothLandCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.AI_LAND));
        millLoseCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.MILLING));
        
        /*
         *  Buttons
         */
        
        deckEditorButton.setToolTipText("");
        deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 15));
        deckEditorButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.DECK_EDITOR));
        deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deckEditorButton_actionPerformed(e);
            }
        });
        
        startButton.setFont(new java.awt.Font("Dialog", 0, 18));
        startButton.setHorizontalTextPosition(SwingConstants.LEADING);
        startButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.START_GAME));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton_actionPerformed(e);
            }
        });

        questButton.setFont(new java.awt.Font("Dialog", 0, 18));
        questButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.QUEST_MODE));
        
        this.getContentPane().add(titleLabel, "align center, span 3, grow, wrap");
        
        this.getContentPane().add(jPanel2, "span 3, grow, wrap");
        jPanel2.add(singleRadioButton, "span 3, wrap");
        jPanel2.add(sealedRadioButton, "span 3, wrap");
        jPanel2.add(draftRadioButton, "span 3, wrap");
        
        this.getContentPane().add(jPanel1, "span 2, grow");
        jPanel1.add(jLabel2);
        jPanel1.add(humanComboBox, "sg combobox, wrap");
        jPanel1.add(jLabel3);
        jPanel1.add(computerComboBox, "sg combobox"); 
        this.getContentPane().add(deckEditorButton, "sg buttons, align 50% 50%, wrap");
        
        this.getContentPane().add(jPanel3, "span 2, grow");
        
        jPanel3.add(newGuiCheckBox, "wrap");
        jPanel3.add(smoothLandCheckBox, "wrap");
        jPanel3.add(millLoseCheckBox, "wrap");
        
        this.getContentPane().add(startButton, "sg buttons, align 50% 50%, split 2, flowy");
        this.getContentPane().add(questButton, "sg buttons, align 50% 50%"); 
        
        buttonGroup1.add(singleRadioButton);
        buttonGroup1.add(sealedRadioButton);
        buttonGroup1.add(draftRadioButton);
    }
    
    void deckEditorButton_actionPerformed(ActionEvent e) {
        if(editor == null) {
            editor = new Gui_DeckEditor();
            
            {
                {
                    Command exit = new Command() {
                        private static final long serialVersionUID = -9133358399503226853L;
                        
                        public void execute() {
                            new Gui_NewGame();
                        }
                    };
                    editor.show(exit);
                    editor.setVisible(true);
                }//run()
            }
        }//if
        
        //refresh decks:
        allDecks = getDecks();
        
        editor.setVisible(true);
        dispose();
    }
    
    Deck getRandomDeck(Deck[] d) {
        //get a random number between 0 and d.length
        int i = (int) (Math.random() * d.length);
        
        return d[i];
    }
    
    void startButton_actionPerformed(ActionEvent e) {
        if(humanComboBox.getSelectedItem() == null || computerComboBox.getSelectedItem() == null) return;
        
        String human = humanComboBox.getSelectedItem().toString();
        
        String computer = null;
        if(computerComboBox.getSelectedItem() != null) computer = computerComboBox.getSelectedItem().toString();
        
        if(draftRadioButton.isSelected()) {
            if(human.equals("New Draft")) {
                dispose();
                Gui_BoosterDraft draft = new Gui_BoosterDraft();
                draft.showGui(new BoosterDraft_1());
                return;
            } else//load old draft
            {
                Deck[] deck = boosterDeckIO.readBoosterDeck(human);
                int index = Integer.parseInt(computer);
                
                Constant.Runtime.HumanDeck[0] = deck[0];
                Constant.Runtime.ComputerDeck[0] = deck[index];
                
                if(Constant.Runtime.ComputerDeck[0] == null) throw new RuntimeException(
                        "Gui_NewGame : startButton() error - computer deck is null");
            }// else - load old draft
        }// if
        else {
            // non-draft decks
            String format = Constant.Runtime.GameType[0];
            boolean sealed = Constant.GameType.Sealed.equals(format);
            boolean constructed = Constant.GameType.Constructed.equals(format);
            
            boolean humanGenerate = human.equals("Generate Deck");
/*            boolean humanGenerateMulti3 = human.equals("Generate 3-Color Deck");
            boolean humanGenerateMulti5 = human.equals("Generate 5-Color Gold Deck");
            boolean humanGenerateTheme = human.equals("Generate Theme Deck");
            boolean humanGenerate2Color = human.equals("Generate 2 Color Deck");*/
            boolean humanRandom = human.equals("Random");
            if(humanGenerate) {
                if(constructed) genDecks("H"); //Constant.Runtime.HumanDeck[0] = generateConstructedDeck();
                else if(sealed) Constant.Runtime.HumanDeck[0] = generateSealedDeck();
            } /*else if(humanGenerateMulti3) {
                if(constructed) Constant.Runtime.HumanDeck[0] = generateConstructed3ColorDeck();
            } else if(humanGenerateMulti5) {
                if(constructed) Constant.Runtime.HumanDeck[0] = generateConstructed5ColorDeck();
            } else if (humanGenerateTheme) {
            	if (constructed) Constant.Runtime.HumanDeck[0] = generateConstructedThemeDeck();
            } else if (humanGenerate2Color) {
            	if (constructed) Constant.Runtime.HumanDeck[0] = generate2ColorDeck("H");
            }*/ else if(humanRandom) {
                Constant.Runtime.HumanDeck[0] = getRandomDeck(getDecks(format));
                JOptionPane.showMessageDialog(null, String.format("You are using deck: %s",
                        Constant.Runtime.HumanDeck[0].getName()), "Deck Name", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.HumanDeck[0] = deckIO.readDeck(human);
            }
            

            boolean computerGenerate = computer.equals("Generate Deck");
/*            boolean computerGenerateMulti3 = computer.equals("Generate 3-Color Deck");
            boolean computerGenerateMulti5 = computer.equals("Generate 5-Color Gold Deck");
            boolean computerGenerateTheme = computer.equals("Generate Theme Deck");
            boolean computerGenerate2Color = computer.equals("Generate 2 Color Deck");
*/            
            boolean computerRandom = computer.equals("Random");
            if(computerGenerate) {
                if(constructed) genDecks("C"); //Constant.Runtime.ComputerDeck[0] = generateConstructedDeck();
                else if(sealed) Constant.Runtime.ComputerDeck[0] = generateSealedDeck();
            } /*else if(computerGenerateMulti3) {
                if(constructed) Constant.Runtime.ComputerDeck[0] = generateConstructed3ColorDeck();
            } else if(computerGenerateMulti5) {
                if(constructed) Constant.Runtime.ComputerDeck[0] = generateConstructed5ColorDeck();
            } else if (computerGenerateTheme) {
            	if (constructed) Constant.Runtime.ComputerDeck[0] = generateConstructedThemeDeck();
            } else if (computerGenerate2Color) {
            	if (constructed) Constant.Runtime.ComputerDeck[0] = generate2ColorDeck("C");
            }*/ else if(computerRandom) {
                Constant.Runtime.ComputerDeck[0] = getRandomDeck(getDecks(format));
                JOptionPane.showMessageDialog(null, String.format("The computer is using deck: %s",
                        Constant.Runtime.ComputerDeck[0].getName()), "Deck Name", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.ComputerDeck[0] = deckIO.readDeck(computer);
            }
        }// else
        
        //DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards are added
        
        if(newGuiCheckBox.isSelected()) AllZone.Display = new GuiDisplay4();
        else AllZone.Display = new GuiDisplay3();
        
        if(smoothLandCheckBox.isSelected()) Constant.Runtime.Smooth[0] = true;
        else Constant.Runtime.Smooth[0] = false;
        
        if(millLoseCheckBox.isSelected()) Constant.Runtime.Mill[0] = true;
        else Constant.Runtime.Mill[0] = false;
        

        AllZone.GameAction.newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0]);
        AllZone.Display.setVisible(true);
        
        dispose();
    }//startButton_actionPerformed()
    
    private Deck generateSealedDeck() {
        GenerateSealedDeck gen = new GenerateSealedDeck();
        CardList name = gen.generateDeck();
        Deck deck = new Deck(Constant.GameType.Sealed);
        
        for(int i = 0; i < 40; i++)
            deck.addMain(name.get(i).getName());
        return deck;
    }
    
    private void genDecks(String p)
    {
    	Deck d = null;
    	
        ArrayList<String> decks = new ArrayList<String>();
        decks.add("2-Color Deck (original)");
        decks.add("3-Color Deck (original)");
        decks.add("5-Color Deck (original)");
        decks.add("Semi-Random Theme Deck");
        decks.add("2-Color Deck (new)");
        decks.add("3-Color Deck (new)");
        
        String prompt = "Generate ";
        if (p.equals("H"))
        	prompt += "Human ";
        else
        	prompt += "Computer ";
        prompt += "Deck";
        Object o = AllZone.Display.getChoice(prompt, decks.toArray());
    	
        if (o.toString().equals(decks.get(0)))
        	d = generateConstructedDeck();
        else if (o.toString().equals(decks.get(1)))
        	d = generateConstructed3ColorDeck();
        else if (o.toString().equals(decks.get(2)))
        	d = generateConstructed5ColorDeck();
        else if (o.toString().equals(decks.get(3)))
        	d = generateConstructedThemeDeck();
        else if (o.toString().equals(decks.get(4)))
        	d = generate2ColorDeck(p);
        else if (o.toString().equals(decks.get(5)))
        	d = generate3ColorDeck(p);
        
        if (p.equals("H"))
        	Constant.Runtime.HumanDeck[0] = d;
        else if (p.equals("C"))
        	Constant.Runtime.ComputerDeck[0] = d;
    }
    
    private Deck generateConstructedDeck() {
        GenerateConstructedDeck gen = new GenerateConstructedDeck();
        CardList name = gen.generateDeck();
        Deck deck = new Deck(Constant.GameType.Constructed);
        
        for(int i = 0; i < 60; i++)
            deck.addMain(name.get(i).getName());
        return deck;
    }
    
    private Deck generateConstructed3ColorDeck() {
        GenerateConstructedMultiColorDeck gen = new GenerateConstructedMultiColorDeck();
        CardList name = gen.generate3ColorDeck();
        Deck deck = new Deck(Constant.GameType.Constructed);
        
        for(int i = 0; i < 60; i++)
            deck.addMain(name.get(i).getName());
        return deck;
    }
    
    private Deck generateConstructed5ColorDeck() {
        GenerateConstructedMultiColorDeck gen = new GenerateConstructedMultiColorDeck();
        CardList name = gen.generate5ColorDeck();
        Deck deck = new Deck(Constant.GameType.Constructed);
        
        for(int i = 0; i < 60; i++)
            deck.addMain(name.get(i).getName());
        return deck;
    }
    
    private Deck generateConstructedThemeDeck()
    {
    	GenerateThemeDeck gen = new GenerateThemeDeck();
    	ArrayList<String> tNames = gen.getThemeNames();
    	Object o = AllZone.Display.getChoice("Select a theme.", tNames.toArray());
    	
    	CardList td = gen.getThemeDeck(o.toString(), 60);
    	Deck deck = new Deck(Constant.GameType.Constructed);
    	
    	for (int i=0; i<td.size(); i++)
    		deck.addMain(td.get(i).getName());
    	
    	return deck;
    }
    
    private Deck generate2ColorDeck(String p)
    {
    	Random r = new Random();
    	
    	ArrayList<String> colors = new ArrayList<String>();
    	colors.add("Random");
    	colors.add("White");
    	colors.add("Blue");
    	colors.add("Black");
    	colors.add("Red");
    	colors.add("Green");
    	
    	Object c1 = null, c2 = null;
    	if (p.equals("H"))
    	{
    		c1 = AllZone.Display.getChoice("Select first color.", colors.toArray());
    	
    		if (c1.toString().equals("Random"))
    			c1 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		
    		colors.remove(c1);
    	
    		c2 = AllZone.Display.getChoice("Select second color.", colors.toArray());
    		
    		if (c2.toString().equals("Random"))
    			c2 = colors.get(r.nextInt(colors.size() - 1) + 1);
    	}
    	else if (p.equals("C"))
    	{
    		c1 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		colors.remove(c1);
    		c2 = colors.get(r.nextInt(colors.size() - 1) + 1);
    	}
    	Generate2ColorDeck gen = new Generate2ColorDeck(c1.toString(), c2.toString());
    	CardList d = gen.get2ColorDeck(60);
    	
    	Deck deck = new Deck(Constant.GameType.Constructed);
    	
    	for (int i=0; i<d.size(); i++)
    		deck.addMain(d.get(i).getName());
    	
    	return deck;

    }

    private Deck generate3ColorDeck(String p)
    {
    	Random r = new Random();
    	
    	ArrayList<String> colors = new ArrayList<String>();
    	colors.add("Random");
    	colors.add("White");
    	colors.add("Blue");
    	colors.add("Black");
    	colors.add("Red");
    	colors.add("Green");
    	
    	Object c1 = null, c2 = null, c3 = null;
    	if (p.equals("H"))
    	{
    		c1 = AllZone.Display.getChoice("Select first color.", colors.toArray());
    	
    		if (c1.toString().equals("Random"))
    			c1 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		
    		colors.remove(c1);
    	
    		c2 = AllZone.Display.getChoice("Select second color.", colors.toArray());
    		
    		if (c2.toString().equals("Random"))
    			c2 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		
    		colors.remove(c2);
    		
    		c3 = AllZone.Display.getChoice("Select third color.", colors.toArray());
    		if (c3.toString().equals("Random"))
    			c3 = colors.get(r.nextInt(colors.size() - 1) + 1);

    	}
    	else if (p.equals("C"))
    	{
    		c1 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		colors.remove(c1);
    		c2 = colors.get(r.nextInt(colors.size() - 1) + 1);
    		colors.remove(c2);
    		c3 = colors.get(r.nextInt(colors.size() - 1) + 1);
    	}
    	Generate3ColorDeck gen = new Generate3ColorDeck(c1.toString(), c2.toString(), c3.toString());
    	CardList d = gen.get3ColorDeck(60);
    	
    	Deck deck = new Deck(Constant.GameType.Constructed);
    	
    	for (int i=0; i<d.size(); i++)
    		deck.addMain(d.get(i).getName());
    	
    	return deck;

    }

    void singleRadioButton_actionPerformed(ActionEvent e) {
        Constant.Runtime.GameType[0] = Constant.GameType.Constructed;
        updateDeckComboBoxes();
    }
    
    void sealedRadioButton_actionPerformed(ActionEvent e) {
        Constant.Runtime.GameType[0] = Constant.GameType.Sealed;
        updateDeckComboBoxes();
    }
    
    private void updateDeckComboBoxes() {
        humanComboBox.removeAllItems();
        computerComboBox.removeAllItems();
        
        if(Constant.GameType.Sealed.equals(Constant.Runtime.GameType[0])
                || Constant.GameType.Constructed.equals(Constant.Runtime.GameType[0])) {
            humanComboBox.addItem("Generate Deck");
            computerComboBox.addItem("Generate Deck");
                        
            humanComboBox.addItem("Random");
            computerComboBox.addItem("Random");
        }
        
        Deck d;
        for(int i = 0; i < allDecks.size(); i++) {
            d = allDecks.get(i);
            if(d.getDeckType().equals(Constant.Runtime.GameType[0])) {
                humanComboBox.addItem(d.getName());
                computerComboBox.addItem(d.getName());
            }
        }//for
        
        //not sure if the code below is useful or not
        //this will select the deck that you previously used
        
        //if(Constant.Runtime.HumanDeck[0] != null)
        //  humanComboBox.setSelectedItem(Constant.Runtime.HumanDeck[0].getName());
        
    }/*updateComboBoxes()*/
    
    Deck[] getDecks(String gameType) {
        ArrayList<Deck> list = new ArrayList<Deck>();
        
        Deck d;
        for(int i = 0; i < allDecks.size(); i++) {
            d = allDecks.get(i);
            if(d.getDeckType().equals(gameType)) list.add(d);
        }//for
        
        //convert ArrayList to Deck[]
        Deck[] out = new Deck[list.size()];
        list.toArray(out);
        
        return out;
    }//getDecks()
    
    void draftRadioButton_actionPerformed(ActionEvent e) {
        Constant.Runtime.GameType[0] = Constant.GameType.Draft;
        humanComboBox.removeAllItems();
        computerComboBox.removeAllItems();
        
        humanComboBox.addItem("New Draft");
        Object[] key = boosterDeckIO.getBoosterDecks().keySet().toArray();
        Arrays.sort(key);
        for(int i = 0; i < key.length; i++)
            humanComboBox.addItem(key[i]);
        
        for(int i = 0; i < 7; i++)
            computerComboBox.addItem("" + (i + 1));
    }
    
    void humanComboBox_actionPerformed(ActionEvent e) {

    }/* draftRadioButton_actionPerformed() */
    
    public static class LookAndFeelAction extends AbstractAction {
        
        private static final long serialVersionUID = -4447498333866711215L;
        private Component         c;
        
        public LookAndFeelAction(Component c) {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.LF));
            this.c = c;
        }
        
        public void actionPerformed(ActionEvent e) {
            LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
            HashMap<String, String> LAFMap = new HashMap<String, String>();
            for(int i = 0; i < info.length; i++)
                LAFMap.put(info[i].getName(), info[i].getClassName());
            
            //add Substance LAFs:
            LAFMap.put("Autumn", "org.jvnet.substance.skin.SubstanceAutumnLookAndFeel");
            LAFMap.put("Business", "org.jvnet.substance.skin.SubstanceBusinessLookAndFeel");
            LAFMap.put("Business Black Steel", "org.jvnet.substance.skin.SubstanceBusinessBlackSteelLookAndFeel");
            LAFMap.put("Business Blue Steel", "org.jvnet.substance.skin.SubstanceBusinessBlueSteelLookAndFeel");
            LAFMap.put("Challenger Deep", "org.jvnet.substance.skin.SubstanceChallengerDeepLookAndFeel");
            LAFMap.put("Creme", "org.jvnet.substance.skin.SubstanceCremeLookAndFeel");
            LAFMap.put("Creme Coffee", "org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel");
            LAFMap.put("Dust", "org.jvnet.substance.skin.SubstanceDustLookAndFeel");
            LAFMap.put("Dust Coffee", "org.jvnet.substance.skin.SubstanceDustCoffeeLookAndFeel");
            LAFMap.put("Emerald Dusk", "org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel");
            LAFMap.put("Gemini", "org.jvnet.substance.api.skin.SubstanceGeminiLookAndFeel");
            LAFMap.put("Graphite Aqua", "org.jvnet.substance.api.skin.SubstanceGraphiteAquaLookAndFeel");
            LAFMap.put("Moderate", "org.jvnet.substance.skin.SubstanceModerateLookAndFeel");
            LAFMap.put("Magellan", "org.jvnet.substance.api.skin.SubstanceMagellanLookAndFeel");
            LAFMap.put("Magma", "org.jvnet.substance.skin.SubstanceMagmaLookAndFeel");
            LAFMap.put("Mist Aqua", "org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel");
            LAFMap.put("Mist Silver", "org.jvnet.substance.skin.SubstanceMistSilverLookAndFeel");
            LAFMap.put("Napkin", "net.sourceforge.napkinlaf.NapkinLookAndFeel");
            LAFMap.put("Nebula", "org.jvnet.substance.skin.SubstanceNebulaLookAndFeel");
            LAFMap.put("Nebula Brick Wall", "org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel");
            LAFMap.put("NimROD", "com.nilo.plaf.nimrod.NimRODLookAndFeel");
            LAFMap.put("Office Blue 2007", "org.jvnet.substance.skin.SubstanceOfficeBlue2007LookAndFeel");
            LAFMap.put("Office Silver 2007", "org.jvnet.substance.skin.SubstanceOfficeSilver2007LookAndFeel");
            LAFMap.put("Raven", "org.jvnet.substance.skin.SubstanceRavenLookAndFeel");
            LAFMap.put("Raven Graphite", "org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel");
            LAFMap.put("Raven Graphite Glass", "org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel");
            LAFMap.put("Sahara", "org.jvnet.substance.skin.SubstanceSaharaLookAndFeel");
            LAFMap.put("Twilight", "org.jvnet.substance.skin.SubstanceTwilightLookAndFeel");
            
            String[] keys = new String[LAFMap.size()];
            int count = 0;
            Iterator<String> iter = LAFMap.keySet().iterator();
            while(iter.hasNext()) {
                String s = iter.next();
                keys[count++] = s;
            }
            Arrays.sort(keys);
            
            ListChooser<String> ch = new ListChooser<String>("Choose one", 0, 1, keys);
            if(ch.show()) try {
                String name = ch.getSelectedValue();
                int index = ch.getSelectedIndex();
                if(index == -1) return;
                //UIManager.setLookAndFeel(info[index].getClassName());
                preferences.laf = LAFMap.get(name);
                UIManager.setLookAndFeel(LAFMap.get(name));
                
                SwingUtilities.updateComponentTreeUI(c);
            } catch(Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
    }
    
    public static class DownloadPriceAction extends AbstractAction {
    	private static final long serialVersionUID = 929877827872974298L;
    	
    	public DownloadPriceAction() {
    		super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADPRICE));
    	}
    	
    	public void actionPerformed(ActionEvent e) {
    		Gui_DownloadPrices gdp = new Gui_DownloadPrices();
    		gdp.setVisible(true);
    	}
    }
    
    public static class DownloadAction extends AbstractAction {
        
        private static final long serialVersionUID = 6564425021778307101L;
        
        public DownloadAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOAD));
        }
        
        public void actionPerformed(ActionEvent e) {
            
            Gui_DownloadPictures.startDownload(null);
        }
    }
    
    public static class DownloadActionLQ extends AbstractAction {
        
        private static final long serialVersionUID = -6234380664413874813L;
        
        public DownloadActionLQ() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADLQ));
        }
        
        public void actionPerformed(ActionEvent e) {
            
            Gui_DownloadPictures_LQ.startDownload(null);
        }
    }
    
    public static class ImportPictureAction extends AbstractAction {
        
        private static final long serialVersionUID = 6893292814498031508L;
        
        public ImportPictureAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.IMPORTPICTURE));
        }
        
        public void actionPerformed(ActionEvent e) {
            GUI_ImportPicture ip = new GUI_ImportPicture(null);
            ip.setVisible(true);
        }
    }
    
    public static class CardSizesAction extends AbstractAction {
        
        private static final long serialVersionUID = -2900235618450319571L;
        private static String[]          keys             = {"Tiny", "Smaller", "Small", "Medium", "Large", "Huge"};
        private static int[]             widths           = {36, 42, 63, 70, 93,  120};
        private static int[]             heights          = {50, 59, 88, 98, 130, 168};
        
        public CardSizesAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_SIZES));
        }
        
        public void actionPerformed(ActionEvent e) {
            ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a new card size", 0, 1, keys);
            if(ch.show()) try {
                int index = ch.getSelectedIndex();
                if(index == -1) return;
                set(index);
            } catch(Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
        
        public static void set(int index) {
        	preferences.cardSize = CardSizeType.valueOf(keys[index].toLowerCase());
        	Constant.Runtime.width[0] = widths[index];
            Constant.Runtime.height[0] = heights[index];
        }
        
        public static void set(CardSizeType s) {
        	preferences.cardSize = s;
        	int index = 0;
        	for(String str : keys){
        		if(str.toLowerCase().equals(s.toString()))
        			break;
        		index++;
        	}
        	Constant.Runtime.width[0] = widths[index];
            Constant.Runtime.height[0] = heights[index];
        }
    }
    
    public static class CardStackAction extends AbstractAction {

		private static final long serialVersionUID = -3770527681359311455L;
		private static String[]          keys             = {"3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        private static int[]             values           = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        
        public CardStackAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_STACK));
        }
        
        public void actionPerformed(ActionEvent e) {
            ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose the max size of a stack", 0, 1, keys);
            if(ch.show()) try {
                int index = ch.getSelectedIndex();
                if(index == -1) return;
                set(index);

            } catch(Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
        
        public static void set(int index) {
        	preferences.maxStackSize = values[index];
        	Constant.Runtime.stackSize[0] = values[index];
        }
        
        public static void setVal(int val) {
        	preferences.maxStackSize = val;
        	Constant.Runtime.stackSize[0] = val;
        }
    }
    
    public static class CardStackOffsetAction extends AbstractAction {
        
		private static final long serialVersionUID = 5021304777748833975L;
		private static String[]          keys             = {"Tiny", "Small", "Medium", "Large"};
        private static int[]             offsets          = {5, 7, 10, 15};
        
        public CardStackOffsetAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_STACK_OFFSET));
        }
        
        public void actionPerformed(ActionEvent e) {
            ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a stack offset value", 0, 1, keys);
            if(ch.show()) try {
                int index = ch.getSelectedIndex();
                if(index == -1) return;
                set(index);

            } catch(Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
        
        public static void set(int index) {
        	preferences.stackOffset = StackOffsetType.valueOf(keys[index].toLowerCase());
        	Constant.Runtime.stackOffset[0] = offsets[index];
        }
        
        public static void set(StackOffsetType s) {
        	preferences.stackOffset = s;
        	int index = 0;
        	for(String str : keys){
        		if(str.toLowerCase().equals(s.toString()))
        			break;
        		index++;
        	}
        	Constant.Runtime.stackOffset[0] = offsets[index];
        }
    }
    
    public static class AboutAction extends AbstractAction {
        
        private static final long serialVersionUID = 5492173304463396871L;
        
        public AboutAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.ABOUT));
        }
        
        public void actionPerformed(ActionEvent e) {
            JTextArea area = new JTextArea(12, 25);
            
            if(useLAFFonts.isSelected()) {
                Font f = new Font(area.getFont().getName(), Font.PLAIN, 13);
                area.setFont(f);
            }
            
            area.setText("I enjoyed programming this project.  I'm glad other people also enjoy my program.  MTG Forge has turned out to be very successful.\n\nmtgrares@yahoo.com\nhttp://mtgrares.blogspot.com\n\nWritten by: Forge\n\n(Quest icons used created by Teekatas, from his Legendora set:\n http://raindropmemory.deviantart.com)");
            
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);
            
            JPanel p = new JPanel();
            area.setBackground(p.getBackground());
            
            JOptionPane.showMessageDialog(null, area, "About", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
	public boolean exit () {
		try {
			preferences.laf = UIManager.getLookAndFeel().getClass().getName();
			preferences.lafFonts = useLAFFonts.isSelected();
			preferences.newGui = newGuiCheckBox.isSelected();
			preferences.stackAiLand = smoothLandCheckBox.isSelected();
			preferences.millingLossCondition = millLoseCheckBox.isSelected();
			preferences.cardOverlay = cardOverlay.isSelected();
			preferences.scaleLargerThanOriginal = ImageCache.scaleLargerThanOriginal;
			preferences.save();
		} catch (Exception ex) {
			int result = JOptionPane.showConfirmDialog(this,
				"Preferences could not be saved. Continue to close without saving ?", "Confirm Exit",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return false;
		}

		setVisible(false);
		dispose();
		return true;
	}

	protected void processWindowEvent (WindowEvent event) {
		if (event.getID() == WindowEvent.WINDOW_CLOSING) {
			if (!exit()) return;
		}
		super.processWindowEvent(event);
	}
}
