package forge.screens.home.quest;

import forge.ImageCache;
import forge.quest.QuestEvent;
import forge.quest.QuestUtil;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.image.BufferedImage;

/** 
 * Panels for displaying duels and challenges.<br>
 * Handles radio button selection, event storage, and repainting.<br>
 * Package private!
 */
@SuppressWarnings("serial")
class PnlEvent extends JPanel {
    private final QuestEvent event;
    private final FRadioButton rad;
    private final SkinImage img;

    private final int wImg = 100;
    private final int hImg = 100;
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
        img = ImageCache.getIcon(e0.getIconImageKey());

        // Title and description
        this.rad = new FRadioButton(event.getTitle() + " (" + event.getDifficulty().getTitle() + ")");
        this.rad.setFont(FSkin.getBoldFont(16));

        final FTextArea tarDesc = new FTextArea();
        tarDesc.setText(event.getDescription());
        tarDesc.setFont(FSkin.getItalicFont(12));

        // Change listener for radio button
        this.rad.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (rad.isSelected()) {
                    QuestUtil.setEvent(event);
                }
            }
        });

        // Final layout
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.add(rad, "gap " + (wImg + 15) + "px 0 10px 0");
        this.add(tarDesc, "w 100% - " + (wImg + 15) + "px!, gap " + (wImg + 15) + "px 0 5px 0");
   }

    /** @return {@link forge.quest.QuestEvent} */
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
        
        Dimension srcSize = img.getSizeForPaint(g2d);
        int wSrc = srcSize.width;
        int hSrc = srcSize.height;

        FSkin.drawImage(g2d, img,
                0, 0, wImg, hImg, // Destination
                0, 0, wSrc, hSrc); // Source

        // Gap between image and reflection set here
        g2d.translate(0, hImg + 2);

        // Reflection drawn onto temporary graphics
        BufferedImage refl = new BufferedImage(wImg, hImg, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gRefl = refl.createGraphics();

        FSkin.drawImage(gRefl, img,
                0, hRfl, wImg, 0, // Destination
                0, hSrc - hRfl * hSrc / hImg, wSrc, hSrc); // Source

        gRefl.setPaint(new GradientPaint(0, 0, clr1, 0, hRfl, clr2));
        gRefl.setComposite(AlphaComposite.DstIn);
        gRefl.fillRect(0, 0, wImg, hImg);

        // Reflection drawn onto panel graphics, cleanup
        g2d.drawImage(refl, 0, 0, null);
        gRefl.dispose();
        g2d.dispose();
    }
}
