package forge.screens.home.quest;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import forge.ImageCache;
import forge.gamemodes.quest.QuestEvent;
import forge.gamemodes.quest.QuestUtil;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FTextArea;
import net.miginfocom.swing.MigLayout;

/**
 * Panels for displaying duels and challenges.<br>
 * Handles radio button selection, event storage, and repainting.<br>
 * Package private!
 */
@SuppressWarnings("serial")
class PnlEvent extends JPanel {
    private final QuestEvent event;
    private final FRadioButton rad;
    private final SkinImage image;

    private final int wImg = 85;
    private final int hImg = 85;
    private final int hRfl = 20;

    private final Color clr1 = new Color(255, 0, 255, 100);
    private final Color clr2 = new Color(255, 255, 0, 0);
    private final SkinColor clr3 = FSkin.getColor(FSkin.Colors.CLR_THEME2).alphaColor(200);

    /**
     * Panels for displaying duels and challenges.<br>
     * Handles radio button selection, event storage, and repainting.<br>
     * Package private!
     *
     * @param e0 &emsp; QuestEvent
     */
    public PnlEvent(final QuestEvent e0) {
        super();
        this.event = e0;

        if (event.getFullTitle().startsWith("Random Opponent")) {
            image = FSkin.getIcon(FSkinProp.ICO_UNKNOWN);
        } else {
            image = event.hasImage() ? ImageCache.getIcon(e0.getIconImageKey()) : null;
        }

        // Title and description
        this.rad = new FRadioButton(event.getFullTitle());
        this.rad.setFont(FSkin.getRelativeBoldFont(16));

        final FTextArea tarDesc = new FTextArea();
        tarDesc.setText(event.getDescription());
        tarDesc.setFont(FSkin.getItalicFont());

        tarDesc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                mousePressed(e);
            }
            @Override
            public void mousePressed(final MouseEvent e) {
                e.getComponent().getParent().dispatchEvent(e);
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                e.getComponent().getParent().dispatchEvent(e);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                e.getComponent().getParent().dispatchEvent(e);
            }
        });

        // Change listener for radio button
        this.rad.addChangeListener(arg0 -> {
            if (rad.isSelected()) {
                QuestUtil.setEvent(event);
            }
        });

        // Final layout
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.add(rad, "gap " + (wImg + 15) + "px 0 10px 0");
        this.add(tarDesc, "w 100% - " + (wImg + 15) + "px!, gap " + (wImg + 15) + "px 0 5px 0");
   }

    /** @return {@link forge.gamemodes.quest.QuestEvent} */
    public QuestEvent getEvent() {
        return event;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRad() {
        return rad;
    }

    @Override
    public void paintComponent(final Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();
        FSkin.setGraphicsGradientPaint(g2d, 0, 0, clr3, getWidth(), 0, clr2);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Padding here
        g2d.translate(5, 5);

        Dimension srcSize = image.getSizeForPaint(g2d);
        int wSrc = srcSize.width;
        int hSrc = srcSize.height;

        FSkin.drawImage(g2d, image,
                0, 0, wImg, hImg, // Destination
                0, 0, wSrc, hSrc); // Source

        /*// Gap between image and reflection set here
        g2d.translate(0, hImg + 2);

        // Reflection drawn onto temporary graphics
        BufferedImage refl = new BufferedImage(wImg, hImg, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gRefl = refl.createGraphics();

        FSkin.drawImage(gRefl, image,
                0, hRfl, wImg, 0, // Destination
                0, hSrc - hRfl * hSrc / hImg, wSrc, hSrc); // Source

        gRefl.setPaint(new GradientPaint(0, 0, clr1, 0, hRfl, clr2));
        gRefl.setComposite(AlphaComposite.DstIn);
        gRefl.fillRect(0, 0, wImg, hImg);

        // Reflection drawn onto panel graphics, cleanup
        g2d.drawImage(refl, 0, 0, null);
        gRefl.dispose();*/
        g2d.dispose();

    }

}
