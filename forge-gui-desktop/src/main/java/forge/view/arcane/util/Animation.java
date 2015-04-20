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
package forge.view.arcane.util;

import java.awt.Container;
import java.awt.EventQueue;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLayeredPane;

import forge.view.arcane.CardPanel;

/**
 * <p>
 * Animation class. Provides useful static methods for animating movement of
 * cards.
 * </p>
 * 
 * @author Forge
 * @version $Id: Animation.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public abstract class Animation {
    /** Constant <code>TARGET_MILLIS_PER_FRAME=30</code>. */
    private static final long TARGET_MILLIS_PER_FRAME = 30;

    /** Constant <code>timer</code>. */
    private static final Timer timer = new Timer("Animation", true);

    private final long delay;

    private final TimerTask timerTask;
    private FrameTimer frameTimer;
    private long elapsed;

    /**
     * Constructor for Animation, with a default delay of zero.
     * 
     * @param duration
     *            the duration, in milliseconds, for which the animation will
     *            run.
     */
    private Animation(final long duration) {
        this(duration, 0);
    }

    /**
     * Constructor for Animation.
     * 
     * @param duration
     *            the duration, in milliseconds, for which the animation will
     *            run.
     * @param delay
     *            the delay, in milliseconds, between subsequent
     *            {@link #update(float)} calls while the animation is running.
     */
    private Animation(final long duration, final long delay) {
        this.delay = delay;
        timerTask = new TimerTask() {
            @Override public final void run() {
                if (frameTimer == null) {
                    onStart();
                    frameTimer = new FrameTimer();
                }
                elapsed += frameTimer.getTimeSinceLastFrame();
                if (elapsed >= duration) {
                    cancel();
                    elapsed = duration;
                }
                update(elapsed / (float) duration);
                if (elapsed == duration) {
                    onEnd();
                }
            }
        };
    }

    /**
     * Starts the animation.
     */
    protected final void run() {
        timer.scheduleAtFixedRate(timerTask, delay, TARGET_MILLIS_PER_FRAME);
    }

    /**
     * Called every {@link #delay} ms while the animation is running.
     * 
     * @param percentage
     *            a float.
     */
    protected abstract void update(float percentage);

    /**
     * Cancel the animation.
     */
    protected final void cancel() {
        timerTask.cancel();
        onEnd();
    }

    /**
     * Executed when the animation starts.
     */
    protected void onStart() {
    }

    /**
     * Executed when the animation ends.
     */
    protected void onEnd() {
    }

    /**
     * <p>
     * invokeLater.
     * </p>
     * 
     * @param runnable
     *            a {@link java.lang.Runnable} object.
     */
    private static void invokeLater(final Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    /**
     * Uses averaging of the time between the past few frames to provide smooth
     * animation.
     */
    private static final class FrameTimer {
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
     *            a {@link forge.view.arcane.CardPanel} object.
     */
    public static void tapCardToggle(final CardPanel panel) {
        new Animation(200) {
            @Override
            protected void onStart() {
                panel.setTapped(!panel.isTapped());
            }

            @Override
            protected void update(final float percentage) {
                panel.setTappedAngle(CardPanel.TAPPED_ANGLE * percentage);
                if (!panel.isTapped()) {
                    panel.setTappedAngle(CardPanel.TAPPED_ANGLE - panel.getTappedAngle());
                }
                panel.repaint();
            }

            @Override
            protected void onEnd() {
                panel.setTappedAngle(panel.isTapped() ? CardPanel.TAPPED_ANGLE : 0);
            }
        }.run();
    }

    /**
     * Animate a {@link CardPanel} moving.
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
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param placeholder
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param layeredPane
     *            a {@link javax.swing.JLayeredPane} object.
     * @param speed
     *            a int.
     */
    public static void moveCardToField(final int startX, final int startY, final int startWidth, final int endX,
            final int endY, final int endWidth, final CardPanel animationPanel, final CardPanel placeholder,
            final JLayeredPane layeredPane, final int speed) {
        Animation.invokeLater(new Runnable() {
            @Override
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
                    layeredPane.setLayer(animationPanel, JLayeredPane.MODAL_LAYER.intValue());
                }

                new Animation(700) {
                    @Override
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

                    @Override
                    protected void onEnd() {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
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
                }.run();
            }
        });
    }

    /**
     * Animate a {@link CardPanel} moving.
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
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param placeholder
     *            a {@link forge.view.arcane.CardPanel} object.
     * @param layeredPane
     *            a {@link javax.swing.JLayeredPane} object.
     * @param speed
     *            a int.
     */
    public static void moveCard(final int startX, final int startY, final int startWidth, final int endX,
            final int endY, final int endWidth, final CardPanel animationPanel, final CardPanel placeholder,
            final JLayeredPane layeredPane, final int speed) {
        Animation.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int startHeight = Math.round(startWidth * CardPanel.ASPECT_RATIO);
                final int endHeight = Math.round(endWidth * CardPanel.ASPECT_RATIO);

                animationPanel.setCardBounds(startX, startY, startWidth, startHeight);
                animationPanel.setAnimationPanel(true);
                Container parent = animationPanel.getParent();
                if (parent != layeredPane) {
                    layeredPane.add(animationPanel);
                    layeredPane.setLayer(animationPanel, JLayeredPane.MODAL_LAYER.intValue());
                }

                new Animation(speed) {
                    @Override
                    protected void update(final float percentage) {
                        int currentX = startX + Math.round((endX - startX) * percentage);
                        int currentY = startY + Math.round((endY - startY) * percentage);
                        int currentWidth = startWidth + Math.round((endWidth - startWidth) * percentage);
                        int currentHeight = startHeight + Math.round((endHeight - startHeight) * percentage);
                        animationPanel.setCardBounds(currentX, currentY, currentWidth, currentHeight);
                    }

                    @Override
                    protected void onEnd() {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (placeholder != null) {
                                    placeholder.setDisplayEnabled(true);
                                    // placeholder.setImage(animationPanel);
                                    placeholder.setCard(placeholder.getCard());
                                }
                                animationPanel.setVisible(false);
                                animationPanel.repaint();
                                layeredPane.remove(animationPanel);
                                if (animationPanel != CardPanel.getDragAnimationPanel()) {
                                    animationPanel.dispose();
                                }
                            }
                        });
                    }
                }.run();
            }
        });
    }

    /**
     * Animate a {@link CardPanel} moving.
     * 
     * @param placeholder
     *            a {@link forge.view.arcane.CardPanel} object.
     */
    public static void moveCard(final CardPanel placeholder) {
        Animation.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (placeholder != null) {
                    placeholder.setDisplayEnabled(true);
                    // placeholder.setImage(imagePanel);
                    placeholder.setCard(placeholder.getCard());
                }
            }
        });
    }

}
