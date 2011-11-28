/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package arcane.ui.util;

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import arcane.ui.CardPanel;
import arcane.util.Util;

/**
 * <p>
 * Abstract Animation class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Animation {
    /** Constant <code>TARGET_MILLIS_PER_FRAME=30</code>. */
    private static final long TARGET_MILLIS_PER_FRAME = 30;

    /** Constant <code>timer</code>. */
    private static Timer timer = new Timer("Animation", true);

    /** Constant <code>delayedCardPanel</code>. */
    private static CardPanel delayedCardPanel;
    /** Constant <code>delayedTime=</code>. */
    private static long delayedTime;
    /** Constant <code>enlargedCardPanel</code>. */
    private static CardPanel enlargedCardPanel;
    /** Constant <code>enlargedAnimationPanel</code>. */
    private static CardPanel enlargedAnimationPanel;
    /** Constant <code>enlargeLock</code>. */
    private static Object enlargeLock = new Object();

    private TimerTask timerTask;
    private FrameTimer frameTimer;
    private long elapsed;

    /**
     * <p>
     * Constructor for Animation.
     * </p>
     * 
     * @param duration
     *            a long.
     */
    public Animation(final long duration) {
        this(duration, 0);
    }

    /**
     * <p>
     * Constructor for Animation.
     * </p>
     * 
     * @param duration
     *            a long.
     * @param delay
     *            a long.
     */
    public Animation(final long duration, final long delay) {
        timerTask = new TimerTask() {
            public void run() {
                if (frameTimer == null) {
                    start();
                    frameTimer = new FrameTimer();
                }
                elapsed += frameTimer.getTimeSinceLastFrame();
                if (elapsed >= duration) {
                    cancel();
                    elapsed = duration;
                }
                update(elapsed / (float) duration);
                if (elapsed == duration) {
                    end();
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, delay, TARGET_MILLIS_PER_FRAME);
    }

    /**
     * <p>
     * update.
     * </p>
     * 
     * @param percentage
     *            a float.
     */
    protected abstract void update(float percentage);

    /**
     * <p>
     * cancel.
     * </p>
     */
    protected final void cancel() {
        timerTask.cancel();
        end();
    }

    /**
     * <p>
     * start.
     * </p>
     */
    protected void start() {
    }

    /**
     * <p>
     * end.
     * </p>
     */
    protected void end() {
    }

    /**
     * Uses averaging of the time between the past few frames to provide smooth
     * animation.
     */
    private class FrameTimer {
        private static final int SAMPLES = 6;
        private static final long MAX_FRAME = 100; // Max time for one frame, to
                                                   // weed out spikes.

        private long[] samples = new long[SAMPLES];
        private int sampleIndex;

        public FrameTimer() {
            long currentTime = System.currentTimeMillis();
            for (int i = SAMPLES - 1; i >= 0; i--) {
                samples[i] = currentTime - (SAMPLES - i) * TARGET_MILLIS_PER_FRAME;
            }
        }

        public long getTimeSinceLastFrame() {
            long currentTime = System.currentTimeMillis();

            int id = sampleIndex - 1;
            if (id < 0) {
                id += SAMPLES;
            }

            long timeSinceLastSample = currentTime - samples[id];

            // If the slice was too big, advance all the previous times by the
            // diff.
            if (timeSinceLastSample > MAX_FRAME) {
                long diff = timeSinceLastSample - MAX_FRAME;
                for (int i = 0; i < SAMPLES; i++) {
                    samples[i] += diff;
                }
            }

            long timeSinceOldestSample = currentTime - samples[sampleIndex];
            samples[sampleIndex] = currentTime;
            sampleIndex = (sampleIndex + 1) % SAMPLES;

            return timeSinceOldestSample / SAMPLES;
        }
    }

    /**
     * <p>
     * tapCardToggle.
     * </p>
     * 
     * @param panel
     *            a {@link arcane.ui.CardPanel} object.
     */
    public static void tapCardToggle(final CardPanel panel) {
        new Animation(200) {
            protected void start() {
                panel.setTapped(!panel.isTapped());
            }

            protected void update(final float percentage) {
                panel.setTappedAngle(CardPanel.TAPPED_ANGLE * percentage);
                if (!panel.isTapped()) {
                    panel.setTappedAngle(CardPanel.TAPPED_ANGLE - panel.getTappedAngle());
                }
                panel.repaint();
            }

            protected void end() {
                panel.setTappedAngle(panel.isTapped() ? CardPanel.TAPPED_ANGLE : 0);
            }
        };
    }

    // static public void moveCardToPlay (Component source, final CardPanel
    // dest, final CardPanel animationPanel) {
    /**
     * <p>
     * moveCardToPlay.
     * </p>
     * 
     * @param startX
     *            a int.
     * @param startY
     *            a int.
     * @param startWidth
     *            a int.
     * @param endX
     *            a int.
     * @param endY
     *            a int.
     * @param endWidth
     *            a int.
     * @param animationPanel
     *            a {@link arcane.ui.CardPanel} object.
     * @param placeholder
     *            a {@link arcane.ui.CardPanel} object.
     * @param layeredPane
     *            a {@link javax.swing.JLayeredPane} object.
     * @param speed
     *            a int.
     */
    public static void moveCardToPlay(final int startX, final int startY, final int startWidth, final int endX,
            final int endY, final int endWidth, final CardPanel animationPanel, final CardPanel placeholder,
            final JLayeredPane layeredPane, final int speed) {
        UI.invokeLater(new Runnable() {
            public void run() {
                final int startHeight = Math.round(startWidth * CardPanel.ASPECT_RATIO);
                final int endHeight = Math.round(endWidth * CardPanel.ASPECT_RATIO);
                final float a = 2f;
                final float sqrta = (float) Math.sqrt(1 / a);

                animationPanel.setCardBounds(startX, startY, startWidth, startHeight);
                animationPanel.setAnimationPanel(true);
                Container parent = animationPanel.getParent();
                if (parent != layeredPane) {
                    layeredPane.add(animationPanel);
                    layeredPane.setLayer(animationPanel, JLayeredPane.MODAL_LAYER);
                }

                new Animation(700) {
                    protected void update(float percentage) {
                        if (placeholder != null && !placeholder.isShowing()) {
                            cancel();
                            return;
                        }
                        int currentX = startX + Math.round((endX - startX + endWidth / 2f) * percentage);
                        int currentY = startY + Math.round((endY - startY + endHeight / 2f) * percentage);
                        int currentWidth, currentHeight;
                        int midWidth = Math.max(200, endWidth * 2);
                        int midHeight = Math.round(midWidth * CardPanel.ASPECT_RATIO);
                        if (percentage <= 0.5f) {
                            percentage = percentage * 2;
                            float pp = sqrta * (1 - percentage);
                            percentage = 1 - a * pp * pp;
                            currentWidth = startWidth + Math.round((midWidth - startWidth) * percentage);
                            currentHeight = startHeight + Math.round((midHeight - startHeight) * percentage);
                        } else {
                            percentage = (percentage - 0.5f) * 2;
                            float pp = sqrta * percentage;
                            percentage = a * pp * pp;
                            currentWidth = midWidth + Math.round((endWidth - midWidth) * percentage);
                            currentHeight = midHeight + Math.round((endHeight - midHeight) * percentage);
                        }
                        currentX -= Math.round(currentWidth / 2.0);
                        currentY -= Math.round(currentHeight / 2.0);
                        animationPanel.setCardBounds(currentX, currentY, currentWidth, currentHeight);
                    }

                    protected void end() {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                if (placeholder != null) {
                                    placeholder.setDisplayEnabled(true);
                                    placeholder.setImage(animationPanel);
                                }
                                animationPanel.setVisible(false);
                                animationPanel.repaint();
                                layeredPane.remove(animationPanel);
                            }
                        });
                    }
                };
            }
        });
    }

    /**
     * <p>
     * moveCard.
     * </p>
     * 
     * @param startX
     *            a int.
     * @param startY
     *            a int.
     * @param startWidth
     *            a int.
     * @param endX
     *            a int.
     * @param endY
     *            a int.
     * @param endWidth
     *            a int.
     * @param animationPanel
     *            a {@link arcane.ui.CardPanel} object.
     * @param placeholder
     *            a {@link arcane.ui.CardPanel} object.
     * @param layeredPane
     *            a {@link javax.swing.JLayeredPane} object.
     * @param speed
     *            a int.
     */
    public static void moveCard(final int startX, final int startY, final int startWidth, final int endX,
            final int endY, final int endWidth, final CardPanel animationPanel, final CardPanel placeholder,
            final JLayeredPane layeredPane, final int speed) {
        UI.invokeLater(new Runnable() {
            public void run() {
                final int startHeight = Math.round(startWidth * CardPanel.ASPECT_RATIO);
                final int endHeight = Math.round(endWidth * CardPanel.ASPECT_RATIO);

                animationPanel.setCardBounds(startX, startY, startWidth, startHeight);
                animationPanel.setAnimationPanel(true);
                Container parent = animationPanel.getParent();
                if (parent != layeredPane) {
                    layeredPane.add(animationPanel);
                    layeredPane.setLayer(animationPanel, JLayeredPane.MODAL_LAYER);
                }

                new Animation(speed) {
                    protected void update(final float percentage) {
                        int currentX = startX + Math.round((endX - startX) * percentage);
                        int currentY = startY + Math.round((endY - startY) * percentage);
                        int currentWidth = startWidth + Math.round((endWidth - startWidth) * percentage);
                        int currentHeight = startHeight + Math.round((endHeight - startHeight) * percentage);
                        animationPanel.setCardBounds(currentX, currentY, currentWidth, currentHeight);
                    }

                    protected void end() {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                if (placeholder != null) {
                                    placeholder.setDisplayEnabled(true);
                                    // placeholder.setImage(animationPanel);
                                    placeholder.setCard(placeholder.getGameCard());
                                }
                                animationPanel.setVisible(false);
                                animationPanel.repaint();
                                layeredPane.remove(animationPanel);
                            }
                        });
                    }
                };
            }
        });
    }

    /**
     * <p>
     * moveCard.
     * </p>
     * 
     * @param placeholder
     *            a {@link arcane.ui.CardPanel} object.
     */
    public static void moveCard(final CardPanel placeholder) {
        UI.invokeLater(new Runnable() {
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (placeholder != null) {
                            placeholder.setDisplayEnabled(true);
                            // placeholder.setImage(imagePanel);
                            placeholder.setCard(placeholder.getGameCard());
                        }
                    }
                });
            }
        });
    }

    /**
     * <p>
     * shrinkCard.
     * </p>
     */
    public static void shrinkCard() {
        CardPanel enlargedCardPanel, enlargedAnimationPanel;
        synchronized (enlargeLock) {
            delayedCardPanel = null;
            delayedTime = 0;
            enlargedCardPanel = Animation.enlargedCardPanel;
            enlargedAnimationPanel = Animation.enlargedAnimationPanel;
            if (enlargedAnimationPanel == null) {
                return;
            }
            Animation.enlargedCardPanel = null;
            Animation.enlargedAnimationPanel = null;
        }

        final CardPanel overPanel = enlargedCardPanel, animationPanel = enlargedAnimationPanel;

        animationPanel.setAnimationPanel(true);
        final JLayeredPane layeredPane = SwingUtilities.getRootPane(overPanel).getLayeredPane();
        layeredPane.setLayer(animationPanel, JLayeredPane.MODAL_LAYER);

        final int startWidth = animationPanel.getCardWidth();
        final int startHeight = Math.round(startWidth * CardPanel.ASPECT_RATIO);
        final int endWidth = overPanel.getCardWidth();
        final int endHeight = Math.round(endWidth * CardPanel.ASPECT_RATIO);

        new Animation(200) {
            protected void update(final float percentage) {
                int currentWidth = startWidth + Math.round((endWidth - startWidth) * percentage);
                int currentHeight = startHeight + Math.round((endHeight - startHeight) * percentage);
                Point startPos = SwingUtilities.convertPoint(overPanel.getParent(), overPanel.getCardLocation(),
                        layeredPane);
                int centerX = startPos.x + Math.round(endWidth / 2f);
                int centerY = startPos.y + Math.round(endHeight / 2f);
                int currentX = Math.max(0, centerX - Math.round(currentWidth / 2f));
                currentX = Math.min(currentX, layeredPane.getWidth() - currentWidth);
                int currentY = Math.max(0, centerY - Math.round(currentHeight / 2f));
                currentY = Math.min(currentY, layeredPane.getHeight() - currentHeight);
                animationPanel.setTappedAngle(overPanel.getTappedAngle() * percentage);
                animationPanel.setCardBounds(currentX, currentY, currentWidth, currentHeight);
            }

            protected void end() {
                animationPanel.setVisible(false);
                animationPanel.repaint();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        layeredPane.remove(animationPanel);
                    }
                });
            }
        };
    }

    /**
     * <p>
     * enlargeCard.
     * </p>
     * 
     * @param overPanel
     *            a {@link arcane.ui.CardPanel} object.
     * @param clientFrame
     *            a {@link java.awt.Frame} object.
     * @param delay
     *            a long.
     */
    public static void enlargeCard(final CardPanel overPanel, final Frame clientFrame, final long delay) {
        if (SwingUtilities.getRootPane(overPanel) == null) {
            synchronized (enlargeLock) {
                delayedCardPanel = null;
                delayedTime = 0;
                if (Animation.enlargedCardPanel != null) {
                    Animation.enlargedCardPanel = null;
                }
                if (enlargedAnimationPanel == null) {
                    return;
                }

                Animation.enlargedAnimationPanel.setVisible(false);
                Animation.enlargedAnimationPanel.repaint();
                Animation.enlargedAnimationPanel = null;
                return;
            }
        }
        final JLayeredPane layeredPane = SwingUtilities.getRootPane(overPanel).getLayeredPane();

        final int startWidth = overPanel.getCardWidth();
        final int startHeight = Math.round(startWidth * CardPanel.ASPECT_RATIO);
        final int endWidth = 300;
        final int endHeight = Math.round(endWidth * CardPanel.ASPECT_RATIO);
        if (startWidth >= endWidth) {
            return;
        }

        long delayedTime;
        synchronized (enlargeLock) {
            if (enlargedCardPanel == overPanel) {
                return; // Already showing this card enlarged.
            }
            if (delay > 0 && delayedCardPanel == overPanel) {
                return; // Already delayed this card.
            }
            delayedCardPanel = overPanel;
            Animation.delayedTime = System.currentTimeMillis();
            delayedTime = Animation.delayedTime;
        }
        final long thisDelayedTime = delayedTime;

        final CardPanel animationPanel = new CardPanel(overPanel.getGameCard());
        animationPanel.setImage(overPanel);

        new Animation(200, delay) {
            protected void start() {
                synchronized (enlargeLock) {
                    // Cancel if the panel is no longer delayed or already
                    // shown.
                    if (delayedCardPanel != overPanel || thisDelayedTime != Animation.delayedTime
                            || enlargedCardPanel == overPanel) {
                        cancel();
                        return;
                    }
                    shrinkCard();
                    enlargedAnimationPanel = animationPanel;
                    enlargedCardPanel = overPanel;
                    Point startPos = overPanel.getCardLocation();
                    animationPanel.setCardBounds(startPos.x, startPos.y, startWidth, startHeight);
                }
                // clientFrame.clearArrows();
                animationPanel.setTappedAngle(overPanel.getTappedAngle());
                try {
                    Util.invokeAndWait(new Runnable() {
                        public void run() {
                            layeredPane.add(animationPanel);
                            layeredPane.setLayer(animationPanel, JLayeredPane.DRAG_LAYER);
                        }
                    });
                } catch (Exception e) {
                }
            }

            protected void update(final float percentage) {
                synchronized (enlargeLock) {
                    if (enlargedAnimationPanel != animationPanel) {
                        cancel();
                        return;
                    }
                }
                int currentWidth = startWidth + Math.round((endWidth - startWidth) * percentage);
                int currentHeight = startHeight + Math.round((endHeight - startHeight) * percentage);
                Point startPos = SwingUtilities.convertPoint(overPanel.getParent(), overPanel.getCardLocation(),
                        layeredPane);
                int centerX = startPos.x + Math.round(startWidth / 2f);
                int centerY = startPos.y + Math.round(startHeight / 2f);
                int currentX = Math.max(0, centerX - Math.round(currentWidth / 2f));
                currentX = Math.min(currentX, layeredPane.getWidth() - currentWidth);
                int currentY = Math.max(0, centerY - Math.round(currentHeight / 2f));
                currentY = Math.min(currentY, layeredPane.getHeight() - currentHeight);
                animationPanel.setTappedAngle(overPanel.getTappedAngle() * (1 - percentage));
                animationPanel.setCardBounds(currentX, currentY, currentWidth, currentHeight);
            }
        };
    }

    /**
     * <p>
     * isShowingEnlargedCard.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean isShowingEnlargedCard() {
        synchronized (enlargeLock) {
            return enlargedAnimationPanel != null;
        }
    }
}
