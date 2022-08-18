//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package forge.adventure.editor;


import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableModel;

public class ParticleEditor extends JFrame {
    static class Slider extends JPanel {
    private JSpinner spinner;

    public Slider(float initialValue, float min, float max, float stepSize, float sliderMin, float sliderMax) {
        this.spinner = new JSpinner(new SpinnerNumberModel((double)initialValue, (double)min, (double)max, (double)stepSize));
        this.setLayout(new BorderLayout());
        this.add(this.spinner);
    }

    public void setValue(float value) {
        this.spinner.setValue((double)value);
    }

    public float getValue() {
        return ((Double)this.spinner.getValue()).floatValue();
    }

    public void addChangeListener(ChangeListener listener) {
        this.spinner.addChangeListener(listener);
    }

    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = 75;
        size.height = 26;
        return size;
    }
}

    public class CustomShading {
        private ShaderProgram shader = SpriteBatch.createDefaultShader();
        Array<String> extraTexturePaths = new Array();
        Array<Texture> extraTextures = new Array();
        String defaultVertexShaderCode;
        String defaultFragmentShaderCode;
        String vertexShaderCode;
        String fragmentShaderCode;
        FileHandle lastVertexShaderFile;
        FileHandle lastFragmentShaderFile;
        boolean hasShaderErrors;
        String shaderErrorMessage;
        boolean hasMissingSamplers;
        String missingSamplerMessage;

        public CustomShading() {
            this.vertexShaderCode = this.defaultVertexShaderCode = this.shader.getVertexShaderSource();
            this.fragmentShaderCode = this.defaultFragmentShaderCode = this.shader.getFragmentShaderSource();
        }

        public void begin(SpriteBatch spriteBatch) {
            spriteBatch.setShader(this.shader);

            for(int i = 0; i < this.extraTextures.size; ++i) {
                ((Texture)this.extraTextures.get(i)).bind(i + 1);
            }

            Gdx.gl.glActiveTexture(33984);
        }

        public void end(SpriteBatch spriteBatch) {
            spriteBatch.setShader((ShaderProgram)null);

            for(int i = 0; i < this.extraTextures.size; ++i) {
                Gdx.gl.glActiveTexture('蓁' + i);
                Gdx.gl.glBindTexture(((Texture)this.extraTextures.get(i)).glTarget, 0);
            }

            Gdx.gl.glActiveTexture(33984);
        }

        public void setVertexShaderFile(String absolutePath) {
            if (absolutePath == null) {
                this.lastVertexShaderFile = null;
                this.vertexShaderCode = this.defaultVertexShaderCode;
            } else {
                this.lastVertexShaderFile = Gdx.files.absolute(absolutePath);
                this.vertexShaderCode = this.lastVertexShaderFile.readString();
            }

            this.updateShader();
        }

        public void setFragmentShaderFile(String absolutePath) {
            if (absolutePath == null) {
                this.lastFragmentShaderFile = null;
                this.fragmentShaderCode = this.defaultFragmentShaderCode;
            } else {
                this.lastFragmentShaderFile = Gdx.files.absolute(absolutePath);
                this.fragmentShaderCode = this.lastFragmentShaderFile.readString();
            }

            this.updateShader();
        }

        public void reloadVertexShader() {
            if (this.lastVertexShaderFile != null) {
                this.vertexShaderCode = this.lastVertexShaderFile.readString();
            }

            this.updateShader();
        }

        public void reloadFragmentShader() {
            if (this.lastFragmentShaderFile != null) {
                this.fragmentShaderCode = this.lastFragmentShaderFile.readString();
            }

            this.updateShader();
        }

        private void updateShader() {
            ShaderProgram shader = new ShaderProgram(this.vertexShaderCode, this.fragmentShaderCode);
            if (shader.isCompiled()) {
                this.hasShaderErrors = false;
                this.shaderErrorMessage = null;
                if (this.shader != null) {
                    this.shader.dispose();
                }

                this.shader = shader;
                this.updateSamplers();
            } else {
                this.hasShaderErrors = true;
                this.shaderErrorMessage = shader.getLog();
                shader.dispose();
            }

        }

        public void addTexture(String absolutePath) {
            this.extraTexturePaths.add(absolutePath);
            this.extraTextures.add(new Texture(Gdx.files.absolute(absolutePath)));
            this.updateSamplers();
        }

        public void swapTexture(int indexA, int indexB) {
            this.extraTexturePaths.swap(indexA, indexB);
            this.extraTextures.swap(indexA, indexB);
            this.updateSamplers();
        }

        public void removeTexture(int index) {
            this.extraTexturePaths.removeIndex(index);
            ((Texture)this.extraTextures.removeIndex(index)).dispose();
            this.updateSamplers();
        }

        public void reloadTexture(int index) {
            Texture previousTexture = (Texture)this.extraTextures.get(index);
            String path = (String)this.extraTexturePaths.get(index);
            Texture texture = new Texture(Gdx.files.absolute(path));
            previousTexture.dispose();
            this.extraTextures.set(index, texture);
        }

        private void updateSamplers() {
            this.hasMissingSamplers = false;
            this.missingSamplerMessage = "";
            this.shader.bind();

            for(int i = 0; i < this.extraTextures.size; ++i) {
                int unit = i + 1;
                int location = this.shader.fetchUniformLocation("u_texture" + unit, false);
                if (location >= 0) {
                    this.shader.setUniformi(location, unit);
                } else {
                    this.hasMissingSamplers = true;
                    this.missingSamplerMessage = this.missingSamplerMessage + "uniform sampler2D u_texture" + unit + " missing in shader program.\n";
                }
            }

        }
    }

    static class EditorPanel extends JPanel {
        private final String name;
        private final String description;
        private final ParticleEmitter.ParticleValue value;
        private JPanel titlePanel;
        JToggleButton activeButton;
        private JPanel contentPanel;
        JToggleButton advancedButton;
        JPanel advancedPanel;
        private boolean hasAdvanced;
        JLabel descriptionLabel;

        public EditorPanel(ParticleEmitter.ParticleValue value, String name, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
            this.initializeComponents();
            this.titlePanel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if (EditorPanel.this.activeButton.isVisible()) {
                        EditorPanel.this.activeButton.setSelected(!EditorPanel.this.activeButton.isSelected());
                        EditorPanel.this.updateActive();
                    }
                }
            });
            this.activeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EditorPanel.this.updateActive();
                }
            });
            this.advancedButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EditorPanel.this.advancedPanel.setVisible(EditorPanel.this.advancedButton.isSelected());
                }
            });
            if (value != null) {
                this.activeButton.setSelected(value.isActive());
                this.updateActive();
            }

            boolean alwaysActive = value == null ? true : value.isAlwaysActive();
            this.activeButton.setVisible(!alwaysActive);
            if (alwaysActive) {
                this.contentPanel.setVisible(true);
            }

            if (alwaysActive) {
                this.titlePanel.setCursor((Cursor)null);
            }

        }

        void updateActive() {
            this.contentPanel.setVisible(this.activeButton.isSelected());
            this.advancedPanel.setVisible(this.activeButton.isSelected() && this.advancedButton.isSelected());
            this.advancedButton.setVisible(this.activeButton.isSelected() && this.hasAdvanced);
            this.descriptionLabel.setText(this.activeButton.isSelected() ? this.description : "");
            if (this.value != null) {
                this.value.setActive(this.activeButton.isSelected());
            }

        }

        public void update(ParticleEditor editor) {
        }

        public void setHasAdvanced(boolean hasAdvanced) {
            this.hasAdvanced = hasAdvanced;
            this.advancedButton.setVisible(hasAdvanced && (this.value.isActive() || this.value.isAlwaysActive()));
        }

        public JPanel getContentPanel() {
            return this.contentPanel;
        }

        public JPanel getAdvancedPanel() {
            return this.advancedPanel;
        }

        public String getName() {
            return this.name;
        }

        public void setEmbedded() {
            GridBagLayout layout = (GridBagLayout)this.getLayout();
            GridBagConstraints constraints = layout.getConstraints(this.contentPanel);
            constraints.insets = new Insets(0, 0, 0, 0);
            layout.setConstraints(this.contentPanel, constraints);
            this.titlePanel.setVisible(false);
        }

        private void initializeComponents() {
            this.setLayout(new GridBagLayout());
            this.titlePanel = new JPanel(new GridBagLayout());
            this.add(this.titlePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 2, new Insets(3, 0, 3, 0), 0, 0));
            this.titlePanel.setCursor(Cursor.getPredefinedCursor(12));
            JLabel label = new JLabel(this.name);
            this.titlePanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
            label.setFont(label.getFont().deriveFont(1));
            this.descriptionLabel = new JLabel(this.description);
            this.titlePanel.add(this.descriptionLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
            this.advancedButton = new JToggleButton("Advanced");
            this.titlePanel.add(this.advancedButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.advancedButton.setVisible(false);
            this.activeButton = new JToggleButton("Active");
            this.titlePanel.add(this.activeButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.contentPanel = new JPanel(new GridBagLayout());
            this.add(this.contentPanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
            this.contentPanel.setVisible(false);
            this.advancedPanel = new JPanel(new GridBagLayout());
            this.add(this.advancedPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
            this.advancedPanel.setVisible(false);
        }
    }
    class PreviewImagePanel extends ParticleEditor.EditorPanel {
        ParticleEditor editor;
        DefaultListModel<String> imageListModel;
        String lastDir;
        JPanel previewContainer;
        ParticleEditor.Slider valueX;
        ParticleEditor.Slider valueY;
        ParticleEditor.Slider valueWidth;
        ParticleEditor.Slider valueHeight;

        public PreviewImagePanel(final ParticleEditor editor, String name, String description) {
            super((ParticleEmitter.ParticleValue) null, name, description);
            this.editor = editor;
            JButton addButton = new JButton("Select preview");
            JButton removeButton = new JButton("Remove preview");
            this.previewContainer = new JPanel(new GridLayout(1, 1));
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileDialog dialog = new FileDialog(editor, "Select Image", 0);
                    if (PreviewImagePanel.this.lastDir != null) {
                        dialog.setDirectory(PreviewImagePanel.this.lastDir);
                    }

                    dialog.setVisible(true);
                    String file = dialog.getFile();
                    String dir = dialog.getDirectory();
                    if (dir != null && file != null && file.trim().length() != 0) {
                        PreviewImagePanel.this.lastDir = dir;

                        try {
                            FileHandle absolute = Gdx.files.absolute(dir + file);
                            BufferedImage read = ImageIO.read(absolute.read());
                            Image scaledInstance = read.getScaledInstance(100, -1, 4);
                            ImageIcon image = new ImageIcon(scaledInstance);
                            JLabel previewImage = new JLabel(image);
                            previewImage.setOpaque(true);
                            previewImage.setBackground(Color.MAGENTA);
                            this.buildImagePanel(previewImage, absolute.file());
                        } catch (IOException var10) {
                            var10.printStackTrace();
                        }

                    }
                }

                private void buildImagePanel(JLabel previewImage, File file) {
                    PreviewImagePanel.this.previewContainer.removeAll();
                    PreviewImagePanel.this.previewContainer.add(previewImage, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
                    PreviewImagePanel.this.previewContainer.updateUI();
                    PreviewImagePanel.this.editor.renderer.setImageBackground(file);
                }
            });
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    this.clearImagePanel();
                }

                private void clearImagePanel() {
                    PreviewImagePanel.this.previewContainer.removeAll();
                    PreviewImagePanel.this.previewContainer.updateUI();
                    PreviewImagePanel.this.editor.renderer.setImageBackground((File) null);
                }
            });
            JPanel buttonPanel = new JPanel(new GridLayout());
            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            this.getContentPanel().add(buttonPanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, 17, 0, new Insets(5, 5, 5, 5), 0, 0));
            this.initializeComponents();
            this.getContentPanel().add(this.previewContainer, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, 17, 0, new Insets(10, 10, 10, 10), 0, 0));
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            JLabel label = new JLabel("X:");
            contentPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.valueX = new ParticleEditor.Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.valueX, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            label = new JLabel("Y:");
            contentPanel.add(label, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
            this.valueY = new ParticleEditor.Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.valueY, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            label = new JLabel("Width:");
            contentPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.valueWidth = new ParticleEditor.Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.valueWidth, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            label = new JLabel("Height:");
            contentPanel.add(label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
            this.valueHeight = new ParticleEditor.Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.valueHeight, new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            float x = 0.0F;
            float y = 0.0F;
            float w = 64.0F;
            float h = 64.0F;
            if (this.editor.renderer.bgImage != null) {
                x = this.editor.renderer.bgImage.getX();
                y = this.editor.renderer.bgImage.getY();
                w = this.editor.renderer.bgImage.getWidth();
                h = this.editor.renderer.bgImage.getHeight();
            }

            this.valueX.setValue(x);
            this.valueY.setValue(y);
            this.valueWidth.setValue(w);
            this.valueHeight.setValue(h);
        }

        public void updateSpritePosition() {
            this.editor.renderer.updateImageBackgroundPosSize(this.valueX.getValue(), this.valueY.getValue(), this.valueWidth.getValue(), this.valueHeight.getValue());
        }
    }
    class NumericPanel extends ParticleEditor.EditorPanel {
        private final ParticleEmitter.NumericValue value;
        JSpinner valueSpinner;

        public NumericPanel(final ParticleEmitter.NumericValue value, String name, String description) {
            super(value, name, description);
            this.value = value;
            this.initializeComponents();
            this.valueSpinner.setValue(value.getValue());
            this.valueSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setValue((Float) NumericPanel.this.valueSpinner.getValue());
                }
            });
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            JLabel label = new JLabel("Value:");
            contentPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.valueSpinner = new JSpinner(new SpinnerNumberModel(new Float(0.0F), new Float(-99999.0F), new Float(99999.0F), new Float(0.1F)));
            contentPanel.add(this.valueSpinner, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        }
    }
    class EffectPanel extends JPanel {
        ParticleEditor editor;
        JTable emitterTable;
        DefaultTableModel emitterTableModel;
        int editIndex;
        String lastDir;

        public EffectPanel(ParticleEditor editor) {
            this.editor = editor;
            this.initializeComponents();
        }

        public ParticleEmitter newEmitter(String name, boolean select) {
            ParticleEmitter emitter = new ParticleEmitter();
            emitter.getDuration().setLow(1000.0F);
            emitter.getEmission().setHigh(50.0F);
            emitter.getLife().setHigh(500.0F);
            emitter.getXScale().setHigh(32.0F, 32.0F);
            emitter.getTint().setColors(new float[]{1.0F, 0.12156863F, 0.047058824F});
            emitter.getTransparency().setHigh(1.0F);
            emitter.setMaxParticleCount(25);
            emitter.setImagePaths(new Array(new String[]{"particle.png"}));
            this.addEmitter(name, select, emitter);
            return emitter;
        }

        public ParticleEmitter newExampleEmitter(String name, boolean select) {
            ParticleEmitter emitter = new ParticleEmitter();
            emitter.getDuration().setLow(3000.0F);
            emitter.getEmission().setHigh(250.0F);
            emitter.getLife().setHigh(500.0F, 1000.0F);
            emitter.getLife().setTimeline(new float[]{0.0F, 0.66F, 1.0F});
            emitter.getLife().setScaling(new float[]{1.0F, 1.0F, 0.3F});
            emitter.getXScale().setHigh(32.0F, 32.0F);
            emitter.getRotation().setLow(1.0F, 360.0F);
            emitter.getRotation().setHigh(180.0F, 180.0F);
            emitter.getRotation().setTimeline(new float[]{0.0F, 1.0F});
            emitter.getRotation().setScaling(new float[]{0.0F, 1.0F});
            emitter.getRotation().setRelative(true);
            emitter.getAngle().setHigh(45.0F, 135.0F);
            emitter.getAngle().setLow(90.0F);
            emitter.getAngle().setTimeline(new float[]{0.0F, 0.5F, 1.0F});
            emitter.getAngle().setScaling(new float[]{1.0F, 0.0F, 0.0F});
            emitter.getAngle().setActive(true);
            emitter.getVelocity().setHigh(30.0F, 300.0F);
            emitter.getVelocity().setActive(true);
            emitter.getTint().setColors(new float[]{1.0F, 0.12156863F, 0.047058824F});
            emitter.getTransparency().setHigh(1.0F, 1.0F);
            emitter.getTransparency().setTimeline(new float[]{0.0F, 0.2F, 0.8F, 1.0F});
            emitter.getTransparency().setScaling(new float[]{0.0F, 1.0F, 0.75F, 0.0F});
            emitter.setMaxParticleCount(200);
            emitter.setImagePaths(new Array(new String[]{"particle.png"}));
            this.addEmitter(name, select, emitter);
            return emitter;
        }

        private void addEmitter(String name, boolean select, ParticleEmitter emitter) {
            Array<ParticleEmitter> emitters = this.editor.effect.getEmitters();
            if (emitters.size == 0) {
                emitter.setPosition(this.editor.worldCamera.viewportWidth / 2.0F, this.editor.worldCamera.viewportHeight / 2.0F);
            } else {
                ParticleEmitter p = (ParticleEmitter)emitters.get(0);
                emitter.setPosition(p.getX(), p.getY());
            }

            emitters.add(emitter);
            this.emitterTableModel.addRow(new Object[]{name, true});
            if (select) {
                this.editor.reloadRows();
                int row = this.emitterTableModel.getRowCount() - 1;
                this.emitterTable.getSelectionModel().setSelectionInterval(row, row);
            }

        }

        void emitterSelected() {
            int row = this.emitterTable.getSelectedRow();
            if (row > -1 && row < this.emitterTableModel.getRowCount()) {
                if (row != this.editIndex) {
                    this.editIndex = row;
                    this.editor.reloadRows();
                }
            }
        }

        void openEffect(boolean mergeIntoCurrent) {
            FileDialog dialog = new FileDialog(this.editor, "Open Effect", 0);
            if (this.lastDir != null) {
                dialog.setDirectory(this.lastDir);
            }

            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            if (dir != null && file != null && file.trim().length() != 0) {
                this.lastDir = dir;
                ParticleEffect effect = new ParticleEffect();

                try {
                    File effectFile = new File(dir, file);
                    effect.loadEmitters(Gdx.files.absolute(effectFile.getAbsolutePath()));
                    if (mergeIntoCurrent) {
                        Array.ArrayIterator var7 = effect.getEmitters().iterator();

                        while(var7.hasNext()) {
                            ParticleEmitter emitter = (ParticleEmitter)var7.next();
                            this.addEmitter(emitter.getName(), false, emitter);
                        }
                    } else {
                        this.editor.effect = effect;
                        this.editor.effectFile = effectFile;
                    }

                    this.emitterTableModel.getDataVector().removeAllElements();
                    this.editor.particleData.clear();
                } catch (Exception var9) {
                    System.out.println("Error loading effect: " + (new File(dir, file)).getAbsolutePath());
                    var9.printStackTrace();
                    JOptionPane.showMessageDialog(this.editor, "Error opening effect.");
                    return;
                }

                Array.ArrayIterator var10 = (new Array.ArrayIterator(this.editor.effect.getEmitters())).iterator();

                while(var10.hasNext()) {
                    ParticleEmitter emitter = (ParticleEmitter)var10.next();
                    emitter.setPosition(this.editor.worldCamera.viewportWidth / 2.0F, this.editor.worldCamera.viewportHeight / 2.0F);
                    this.emitterTableModel.addRow(new Object[]{emitter.getName(), true});
                }

                this.editIndex = 0;
                this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
                this.editor.reloadRows();
            }
        }

        void saveEffect() {
            FileDialog dialog = new FileDialog(this.editor, "Save Effect", 1);
            if (this.lastDir != null) {
                dialog.setDirectory(this.lastDir);
            }

            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            if (dir != null && file != null && file.trim().length() != 0) {
                this.lastDir = dir;
                int index = 0;
                File effectFile = new File(dir, file);
                URI effectDirUri = effectFile.getParentFile().toURI();
                Array.ArrayIterator var7 = this.editor.effect.getEmitters().iterator();

                while(var7.hasNext()) {
                    ParticleEmitter emitter = (ParticleEmitter)var7.next();
                    emitter.setName((String)this.emitterTableModel.getValueAt(index++, 0));
                    Array<String> imagePaths = emitter.getImagePaths();

                    for(int i = 0; i < imagePaths.size; ++i) {
                        String imagePath = (String)imagePaths.get(i);
                        if ((imagePath.contains("/") || imagePath.contains("\\")) && !imagePath.contains("..")) {
                            URI imageUri = (new File(imagePath)).toURI();
                            imagePaths.set(i, effectDirUri.relativize(imageUri).getPath());
                        }
                    }
                }

                File outputFile = new File(dir, file);
                Writer fileWriter = null;

                try {
                    fileWriter = new FileWriter(outputFile);
                    this.editor.effect.save(fileWriter);
                } catch (Exception var16) {
                    System.out.println("Error saving effect: " + outputFile.getAbsolutePath());
                    var16.printStackTrace();
                    JOptionPane.showMessageDialog(this.editor, "Error saving effect.");
                } finally {
                    StreamUtils.closeQuietly(fileWriter);
                }

            }
        }

        void duplicateEmitter() {
            int row = this.emitterTable.getSelectedRow();
            if (row != -1) {
                String name = (String)this.emitterTableModel.getValueAt(row, 0);
                this.addEmitter(name, true, new ParticleEmitter((ParticleEmitter)this.editor.effect.getEmitters().get(row)));
            }
        }

        void deleteEmitter() {
            if (this.editor.effect.getEmitters().size != 1) {
                int row = this.emitterTable.getSelectedRow();
                if (row != -1) {
                    if (row <= this.editIndex) {
                        int oldEditIndex = this.editIndex;
                        this.editIndex = Math.max(0, this.editIndex - 1);
                        if (oldEditIndex == row) {
                            this.editor.reloadRows();
                        }
                    }

                    this.editor.effect.getEmitters().removeIndex(row);
                    this.emitterTableModel.removeRow(row);
                    this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
                }
            }
        }

        void move(int direction) {
            if (direction >= 0 || this.editIndex > 0) {
                Array<ParticleEmitter> emitters = this.editor.effect.getEmitters();
                if (direction <= 0 || this.editIndex < emitters.size - 1) {
                    int insertIndex = this.editIndex + direction;
                    Object name = this.emitterTableModel.getValueAt(this.editIndex, 0);
                    Boolean active = (Boolean)this.emitterTableModel.getValueAt(this.editIndex, 1);
                    this.emitterTableModel.removeRow(this.editIndex);
                    ParticleEmitter emitter = (ParticleEmitter)emitters.removeIndex(this.editIndex);
                    this.emitterTableModel.insertRow(insertIndex, new Object[]{name, active});
                    emitters.insert(insertIndex, emitter);
                    this.editIndex = insertIndex;
                    this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
                }
            }
        }

        void emitterChecked(int index, boolean checked) {
            this.editor.setEnabled((ParticleEmitter)this.editor.effect.getEmitters().get(index), checked);
            this.editor.effect.start();
        }

        private void initializeComponents() {
            this.setLayout(new GridBagLayout());
            JPanel sideButtons = new JPanel(new GridBagLayout());
            this.add(sideButtons, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
            JButton downButton = new JButton("New");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.newEmitter("Untitled", true);
                }
            });
            downButton = new JButton("Duplicate");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.duplicateEmitter();
                }
            });
            downButton = new JButton("Delete");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.deleteEmitter();
                }
            });
            sideButtons.add(new JSeparator(0), new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton = new JButton("Save");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.saveEffect();
                }
            });
            downButton = new JButton("Open");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.openEffect(false);
                }
            });
            downButton = new JButton("Merge");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.openEffect(true);
                }
            });
            downButton = new JButton("Up");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 1.0, 15, 2, new Insets(0, 0, 6, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.move(-1);
                }
            });
            downButton = new JButton("Down");
            sideButtons.add(downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    EffectPanel.this.move(1);
                }
            });
            JScrollPane scroll = new JScrollPane();
            this.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
            this.emitterTable = new JTable() {
                public Class getColumnClass(int column) {
                    return column == 1 ? Boolean.class : super.getColumnClass(column);
                }
            };
            this.emitterTable.getTableHeader().setReorderingAllowed(false);
            this.emitterTable.setSelectionMode(0);
            scroll.setViewportView(this.emitterTable);
            this.emitterTableModel = new DefaultTableModel(new String[0][0], new String[]{"Emitter", "Active"});
            this.emitterTable.setModel(this.emitterTableModel);
            this.emitterTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (!event.getValueIsAdjusting()) {
                        EffectPanel.this.emitterSelected();
                    }
                }
            });
            this.emitterTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent event) {
                    if (event.getColumn() == 1) {
                        EffectPanel.this.emitterChecked(event.getFirstRow(), (Boolean) EffectPanel.this.emitterTable.getValueAt(event.getFirstRow(), 1));
                    }
                }
            });
        }
    }
    class GradientPanel extends EditorPanel {
        private final ParticleEmitter.GradientColorValue value;
        private GradientPanel.GradientEditor gradientEditor;
        GradientPanel.ColorSlider saturationSlider;
        GradientPanel.ColorSlider lightnessSlider;
        JPanel colorPanel;
        private GradientPanel.ColorSlider hueSlider;

        public GradientPanel(ParticleEmitter.GradientColorValue value, String name, String description, boolean hideGradientEditor) {
            super(value, name, description);
            this.value = value;
            this.initializeComponents();
            if (hideGradientEditor) {
                this.gradientEditor.setVisible(false);
            }

            this.gradientEditor.percentages.clear();
            float[] colors = value.getTimeline();
            int i = colors.length;

            float g;
            for(int var7 = 0; var7 < i; ++var7) {
                g = colors[var7];
                this.gradientEditor.percentages.add(g);
            }

            this.gradientEditor.colors.clear();
            colors = value.getColors();
            i = 0;

            while(i < colors.length) {
                float r = colors[i++];
                g = colors[i++];
                float b = colors[i++];
                this.gradientEditor.colors.add(new Color(r, g, b));
            }

            if (this.gradientEditor.colors.isEmpty() || this.gradientEditor.percentages.isEmpty()) {
                this.gradientEditor.percentages.clear();
                this.gradientEditor.percentages.add(0.0F);
                this.gradientEditor.percentages.add(1.0F);
                this.gradientEditor.colors.clear();
                this.gradientEditor.colors.add(Color.white);
            }

            this.setColor((Color)this.gradientEditor.colors.get(0));
        }

        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width = 10;
            return size;
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            this.gradientEditor = new GradientPanel.GradientEditor() {
                public void handleSelected(Color color) {
                    GradientPanel.this.setColor(color);
                }
            };
            contentPanel.add(this.gradientEditor, new GridBagConstraints(0, 1, 3, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 10));
            this.hueSlider = new GradientPanel.ColorSlider(new Color[]{Color.red, Color.yellow, Color.green, Color.cyan, Color.blue, Color.magenta, Color.red}) {
                protected void colorPicked() {
                    GradientPanel.this.saturationSlider.setColors(new Color[]{new Color(Color.HSBtoRGB(this.getPercentage(), 1.0F, 1.0F)), Color.white});
                    GradientPanel.this.updateColor();
                }
            };
            contentPanel.add(this.hueSlider, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
            this.saturationSlider = new GradientPanel.ColorSlider(new Color[]{Color.red, Color.white}) {
                protected void colorPicked() {
                    GradientPanel.this.updateColor();
                }
            };
            contentPanel.add(this.saturationSlider, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 6), 0, 0));
            this.lightnessSlider = new GradientPanel.ColorSlider(new Color[0]) {
                protected void colorPicked() {
                    GradientPanel.this.updateColor();
                }
            };
            contentPanel.add(this.lightnessSlider, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
            this.colorPanel = new JPanel() {
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    size.width = 52;
                    return size;
                }
            };
            contentPanel.add(this.colorPanel, new GridBagConstraints(0, 2, 1, 2, 0.0, 0.0, 10, 1, new Insets(3, 0, 0, 6), 0, 0));
            this.colorPanel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Color color = JColorChooser.showDialog(GradientPanel.this.colorPanel, "Set Color", GradientPanel.this.colorPanel.getBackground());
                    if (color != null) {
                        GradientPanel.this.setColor(color);
                    }

                }
            });
            this.colorPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
        }

        public void setColor(Color color) {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
            this.hueSlider.setPercentage(hsb[0]);
            this.saturationSlider.setPercentage(1.0F - hsb[1]);
            this.lightnessSlider.setPercentage(1.0F - hsb[2]);
            this.colorPanel.setBackground(color);
        }

        void updateColor() {
            Color color = new Color(Color.HSBtoRGB(this.hueSlider.getPercentage(), 1.0F - this.saturationSlider.getPercentage(), 1.0F));
            this.lightnessSlider.setColors(new Color[]{color, Color.black});
            color = new Color(Color.HSBtoRGB(this.hueSlider.getPercentage(), 1.0F - this.saturationSlider.getPercentage(), 1.0F - this.lightnessSlider.getPercentage()));
            this.colorPanel.setBackground(color);
            this.gradientEditor.setColor(color);
            float[] colors = new float[this.gradientEditor.colors.size() * 3];
            int i = 0;

            Color c;
            for(Iterator var4 = this.gradientEditor.colors.iterator(); var4.hasNext(); colors[i++] = (float)c.getBlue() / 255.0F) {
                c = (Color)var4.next();
                colors[i++] = (float)c.getRed() / 255.0F;
                colors[i++] = (float)c.getGreen() / 255.0F;
            }

            float[] percentages = new float[this.gradientEditor.percentages.size()];
            i = 0;

            Float percent;
            for(Iterator var8 = this.gradientEditor.percentages.iterator(); var8.hasNext(); percentages[i++] = percent) {
                percent = (Float)var8.next();
            }

            this.value.setColors(colors);
            this.value.setTimeline(percentages);
        }

        public  class ColorSlider extends JPanel {
            Color[] paletteColors;
            JSlider slider;
            private GradientPanel.ColorSlider.ColorPicker colorPicker;

            public ColorSlider(Color[] paletteColors) {
                this.paletteColors = paletteColors;
                this.setLayout(new GridBagLayout());
                this.slider = new JSlider(0, 1000, 0);
                this.slider.setPaintTrack(false);
                this.add(this.slider, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 6, 0, 6), 0, 0));
                this.colorPicker = new GradientPanel.ColorSlider.ColorPicker();
                this.add(this.colorPicker, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 6, 0, 6), 0, 0));
                this.slider.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        GradientPanel.ColorSlider.this.colorPicked();
                    }
                });
            }

            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = 10;
                return size;
            }

            public void setPercentage(float percent) {
                this.slider.setValue((int)(1000.0F * percent));
            }

            public float getPercentage() {
                return (float)this.slider.getValue() / 1000.0F;
            }

            protected void colorPicked() {
            }

            public void setColors(Color[] colors) {
                this.paletteColors = colors;
                this.repaint();
            }

            public class ColorPicker extends JPanel {
                public ColorPicker() {
                    this.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent event) {
                            GradientPanel.ColorSlider.this.slider.setValue((int)((float)event.getX() / (float) GradientPanel.ColorSlider.ColorPicker.this.getWidth() * 1000.0F));
                        }
                    });
                }

                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D)graphics;
                    int width = this.getWidth() - 1;
                    int height = this.getHeight() - 1;
                    int i = 0;

                    for(int n = GradientPanel.ColorSlider.this.paletteColors.length - 1; i < n; ++i) {
                        Color color1 = GradientPanel.ColorSlider.this.paletteColors[i];
                        Color color2 = GradientPanel.ColorSlider.this.paletteColors[i + 1];
                        float point1 = (float)i / (float)n * (float)width;
                        float point2 = (float)(i + 1) / (float)n * (float)width;
                        g.setPaint(new GradientPaint(point1, 0.0F, color1, point2, 0.0F, color2, false));
                        g.fillRect((int)point1, 0, (int)Math.ceil((double)(point2 - point1)), height);
                    }

                    g.setPaint((Paint)null);
                    g.setColor(Color.black);
                    g.drawRect(0, 0, width, height);
                }
            }
        }

        public class GradientEditor extends JPanel {
            ArrayList<Color> colors = new ArrayList();
            ArrayList<Float> percentages = new ArrayList();
            int handleWidth = 12;
            int handleHeight = 12;
            int gradientX;
            int gradientY;
            int gradientWidth;
            int gradientHeight;
            int dragIndex;
            int selectedIndex;

            public GradientEditor() {
                this.gradientX = this.handleWidth / 2;
                this.gradientY = 0;
                this.dragIndex = -1;
                this.setPreferredSize(new Dimension(100, 30));
                this.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent event) {
                        GradientPanel.GradientEditor.this.dragIndex = -1;
                        int mouseX = event.getX();
                        int mouseY = event.getY();
                        int y = GradientPanel.GradientEditor.this.gradientY + GradientPanel.GradientEditor.this.gradientHeight;
                        int i = 0;

                        for(int n = GradientPanel.GradientEditor.this.colors.size(); i < n; ++i) {
                            int x = GradientPanel.GradientEditor.this.gradientX + (int)((Float) GradientPanel.GradientEditor.this.percentages.get(i) * (float) GradientPanel.GradientEditor.this.gradientWidth) - GradientPanel.GradientEditor.this.handleWidth / 2;
                            if (mouseX >= x && mouseX <= x + GradientPanel.GradientEditor.this.handleWidth && mouseY >= GradientPanel.GradientEditor.this.gradientY && mouseY <= y + GradientPanel.GradientEditor.this.handleHeight) {
                                GradientPanel.GradientEditor.this.dragIndex = GradientPanel.GradientEditor.this.selectedIndex = i;
                                GradientPanel.GradientEditor.this.handleSelected((Color) GradientPanel.GradientEditor.this.colors.get(GradientPanel.GradientEditor.this.selectedIndex));
                                GradientPanel.GradientEditor.this.repaint();
                                break;
                            }
                        }

                    }

                    public void mouseReleased(MouseEvent event) {
                        if (GradientPanel.GradientEditor.this.dragIndex != -1) {
                            GradientPanel.GradientEditor.this.dragIndex = -1;
                            GradientPanel.GradientEditor.this.repaint();
                        }

                    }

                    public void mouseClicked(MouseEvent event) {
                        int mouseX = event.getX();
                        int mouseY = event.getY();
                        int i;
                        if (event.getClickCount() == 2) {
                            if (GradientPanel.GradientEditor.this.percentages.size() > 1) {
                                if (GradientPanel.GradientEditor.this.selectedIndex != -1 && GradientPanel.GradientEditor.this.selectedIndex != 0) {
                                    int y = GradientPanel.GradientEditor.this.gradientY + GradientPanel.GradientEditor.this.gradientHeight;
                                    i = GradientPanel.GradientEditor.this.gradientX + (int)((Float) GradientPanel.GradientEditor.this.percentages.get(GradientPanel.GradientEditor.this.selectedIndex) * (float) GradientPanel.GradientEditor.this.gradientWidth) - GradientPanel.GradientEditor.this.handleWidth / 2;
                                    if (mouseX >= i && mouseX <= i + GradientPanel.GradientEditor.this.handleWidth && mouseY >= GradientPanel.GradientEditor.this.gradientY && mouseY <= y + GradientPanel.GradientEditor.this.handleHeight) {
                                        GradientPanel.GradientEditor.this.percentages.remove(GradientPanel.GradientEditor.this.selectedIndex);
                                        GradientPanel.GradientEditor.this.colors.remove(GradientPanel.GradientEditor.this.selectedIndex);
                                        --GradientPanel.GradientEditor.this.selectedIndex;
                                        GradientPanel.GradientEditor.this.dragIndex = GradientPanel.GradientEditor.this.selectedIndex;
                                        if (GradientPanel.GradientEditor.this.percentages.size() == 2) {
                                            GradientPanel.GradientEditor.this.percentages.set(1, 1.0F);
                                        }

                                        GradientPanel.GradientEditor.this.handleSelected((Color) GradientPanel.GradientEditor.this.colors.get(GradientPanel.GradientEditor.this.selectedIndex));
                                        GradientPanel.GradientEditor.this.repaint();
                                    }

                                }
                            }
                        } else if (mouseX >= GradientPanel.GradientEditor.this.gradientX && mouseX <= GradientPanel.GradientEditor.this.gradientX + GradientPanel.GradientEditor.this.gradientWidth) {
                            if (mouseY >= GradientPanel.GradientEditor.this.gradientY && mouseY <= GradientPanel.GradientEditor.this.gradientY + GradientPanel.GradientEditor.this.gradientHeight) {
                                float percent = (float)(event.getX() - GradientPanel.GradientEditor.this.gradientX) / (float) GradientPanel.GradientEditor.this.gradientWidth;
                                if (GradientPanel.GradientEditor.this.percentages.size() == 1) {
                                    percent = 1.0F;
                                }

                                i = 0;

                                for(int n = GradientPanel.GradientEditor.this.percentages.size(); i <= n; ++i) {
                                    if (i == n || percent < (Float) GradientPanel.GradientEditor.this.percentages.get(i)) {
                                        GradientPanel.GradientEditor.this.percentages.add(i, percent);
                                        GradientPanel.GradientEditor.this.colors.add(i, GradientPanel.GradientEditor.this.colors.get(i - 1));
                                        GradientPanel.GradientEditor.this.dragIndex = GradientPanel.GradientEditor.this.selectedIndex = i;
                                        GradientPanel.GradientEditor.this.handleSelected((Color) GradientPanel.GradientEditor.this.colors.get(GradientPanel.GradientEditor.this.selectedIndex));
                                        GradientPanel.this.updateColor();
                                        GradientPanel.GradientEditor.this.repaint();
                                        break;
                                    }
                                }

                            }
                        }
                    }
                });
                this.addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent event) {
                        if (GradientPanel.GradientEditor.this.dragIndex != -1 && GradientPanel.GradientEditor.this.dragIndex != 0 && GradientPanel.GradientEditor.this.dragIndex != GradientPanel.GradientEditor.this.percentages.size() - 1) {
                            float percent = (float)(event.getX() - GradientPanel.GradientEditor.this.gradientX) / (float) GradientPanel.GradientEditor.this.gradientWidth;
                            percent = Math.max(percent, (Float) GradientPanel.GradientEditor.this.percentages.get(GradientPanel.GradientEditor.this.dragIndex - 1) + 0.01F);
                            percent = Math.min(percent, (Float) GradientPanel.GradientEditor.this.percentages.get(GradientPanel.GradientEditor.this.dragIndex + 1) - 0.01F);
                            GradientPanel.GradientEditor.this.percentages.set(GradientPanel.GradientEditor.this.dragIndex, percent);
                            GradientPanel.this.updateColor();
                            GradientPanel.GradientEditor.this.repaint();
                        }
                    }
                });
            }

            public void setColor(Color color) {
                if (this.selectedIndex != -1) {
                    this.colors.set(this.selectedIndex, color);
                    this.repaint();
                }
            }

            public void handleSelected(Color color) {
            }

            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D)graphics;
                int width = this.getWidth() - 1;
                int height = this.getHeight();
                this.gradientWidth = width - this.handleWidth;
                this.gradientHeight = height - 16;
                g.translate(this.gradientX, this.gradientY);
                int y = 0;

                for(int nx = this.colors.size() == 1 ? 1 : this.colors.size() - 1; y < nx; ++y) {
                    Color color1 = (Color)this.colors.get(y);
                    Color color2 = this.colors.size() == 1 ? color1 : (Color)this.colors.get(y + 1);
                    float percent1 = (Float)this.percentages.get(y);
                    float percent2 = this.colors.size() == 1 ? 1.0F : (Float)this.percentages.get(y + 1);
                    int point1 = (int)(percent1 * (float)this.gradientWidth);
                    int point2 = (int)Math.ceil((double)(percent2 * (float)this.gradientWidth));
                    g.setPaint(new GradientPaint((float)point1, 0.0F, color1, (float)point2, 0.0F, color2, false));
                    g.fillRect(point1, 0, point2 - point1, this.gradientHeight);
                }

                g.setPaint((Paint)null);
                g.setColor(Color.black);
                g.drawRect(0, 0, this.gradientWidth, this.gradientHeight);
                y = this.gradientHeight;
                int[] yPoints = new int[]{y, y + this.handleHeight, y + this.handleHeight};
                int[] xPoints = new int[3];
                int i = 0;

                for(int n = this.colors.size(); i < n; ++i) {
                    int x = (int)((Float)this.percentages.get(i) * (float)this.gradientWidth);
                    xPoints[0] = x;
                    xPoints[1] = x - this.handleWidth / 2;
                    xPoints[2] = x + this.handleWidth / 2;
                    if (i == this.selectedIndex) {
                        g.setColor((Color)this.colors.get(i));
                        g.fillPolygon(xPoints, yPoints, 3);
                        g.fillRect(xPoints[1], yPoints[1] + 2, this.handleWidth + 1, 2);
                        g.setColor(Color.black);
                    }

                    g.drawPolygon(xPoints, yPoints, 3);
                }

                g.translate(-this.gradientX, -this.gradientY);
            }
        }
    }

    class RangedNumericPanel extends EditorPanel {
        private final ParticleEmitter.RangedNumericValue value;
        Slider minSlider;
        Slider maxSlider;
        JButton rangeButton;
        JLabel label;

        public RangedNumericPanel(final ParticleEmitter.RangedNumericValue value, String name, String description) {
            super(value, name, description);
            this.value = value;
            this.initializeComponents();
            this.minSlider.setValue(value.getLowMin());
            this.maxSlider.setValue(value.getLowMax());
            this.minSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setLowMin(Float.valueOf(RangedNumericPanel.this.minSlider.getValue()));
                    if (!RangedNumericPanel.this.maxSlider.isVisible()) {
                        value.setLowMax(Float.valueOf(RangedNumericPanel.this.minSlider.getValue()));
                    }

                }
            });
            this.maxSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setLowMax(Float.valueOf(RangedNumericPanel.this.maxSlider.getValue()));
                }
            });
            this.rangeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    boolean visible = !RangedNumericPanel.this.maxSlider.isVisible();
                    RangedNumericPanel.this.maxSlider.setVisible(visible);
                    RangedNumericPanel.this.rangeButton.setText(visible ? "<" : ">");
                    Slider slider = visible ? RangedNumericPanel.this.maxSlider : RangedNumericPanel.this.minSlider;
                    value.setLowMax(Float.valueOf(slider.getValue()));
                }
            });
            if (value.getLowMin() == value.getLowMax()) {
                this.rangeButton.doClick(0);
            }

        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            this.label = new JLabel("Value:");
            contentPanel.add(this.label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.minSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            contentPanel.add(this.minSlider, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.maxSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            contentPanel.add(this.maxSlider, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
            this.rangeButton = new JButton("<");
            this.rangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            contentPanel.add(this.rangeButton, new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
        }
    }

    class SpawnPanel extends EditorPanel {
        JComboBox shapeCombo;
        JCheckBox edgesCheckbox;
        JLabel edgesLabel;
        JComboBox sideCombo;
        JLabel sideLabel;

        public SpawnPanel(final ParticleEditor editor, final ParticleEmitter.SpawnShapeValue spawnShapeValue, String name, String description) {
            super((ParticleEmitter.ParticleValue)null, name, description);
            this.initializeComponents();
            this.edgesCheckbox.setSelected(spawnShapeValue.isEdges());
            this.sideCombo.setSelectedItem(spawnShapeValue.getShape());
            this.shapeCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ParticleEmitter.SpawnShape shape = (ParticleEmitter.SpawnShape) SpawnPanel.this.shapeCombo.getSelectedItem();
                    spawnShapeValue.setShape(shape);
                    switch (shape) {
                        case line:
                        case square:
                            SpawnPanel.this.setEdgesVisible(false);
                            editor.setVisible("Spawn Width", true);
                            editor.setVisible("Spawn Height", true);
                            break;
                        case ellipse:
                            SpawnPanel.this.setEdgesVisible(true);
                            editor.setVisible("Spawn Width", true);
                            editor.setVisible("Spawn Height", true);
                            break;
                        case point:
                            SpawnPanel.this.setEdgesVisible(false);
                            editor.setVisible("Spawn Width", false);
                            editor.setVisible("Spawn Height", false);
                    }

                }
            });
            this.edgesCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    spawnShapeValue.setEdges(SpawnPanel.this.edgesCheckbox.isSelected());
                    SpawnPanel.this.setEdgesVisible(true);
                }
            });
            this.sideCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ParticleEmitter.SpawnEllipseSide side = (ParticleEmitter.SpawnEllipseSide) SpawnPanel.this.sideCombo.getSelectedItem();
                    spawnShapeValue.setSide(side);
                }
            });
            this.shapeCombo.setSelectedItem(spawnShapeValue.getShape());
        }

        public void update(ParticleEditor editor) {
            this.shapeCombo.setSelectedItem(editor.getEmitter().getSpawnShape().getShape());
        }

        void setEdgesVisible(boolean visible) {
            this.edgesCheckbox.setVisible(visible);
            this.edgesLabel.setVisible(visible);
            visible = visible && this.edgesCheckbox.isSelected();
            this.sideCombo.setVisible(visible);
            this.sideLabel.setVisible(visible);
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            JLabel label = new JLabel("Shape:");
            contentPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.shapeCombo = new JComboBox();
            this.shapeCombo.setModel(new DefaultComboBoxModel(ParticleEmitter.SpawnShape.values()));
            contentPanel.add(this.shapeCombo, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.edgesLabel = new JLabel("Edges:");
            contentPanel.add(this.edgesLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
            this.edgesCheckbox = new JCheckBox();
            contentPanel.add(this.edgesCheckbox, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.sideLabel = new JLabel("Side:");
            contentPanel.add(this.sideLabel, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
            this.sideCombo = new JComboBox();
            this.sideCombo.setModel(new DefaultComboBoxModel(ParticleEmitter.SpawnEllipseSide.values()));
            contentPanel.add(this.sideCombo, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension());
            contentPanel.add(spacer, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        }
    }
    public class Chart extends JPanel {
        private static final int POINT_SIZE = 6;
        private static final int POINT_SIZE_EXPANDED = 10;
        ArrayList<Chart.Point> points = new ArrayList();
        private int numberHeight;
        int chartX;
        int chartY;
        int chartWidth;
        int chartHeight;
        int maxX;
        int maxY;
        int overIndex = -1;
        int movingIndex = -1;
        boolean isExpanded;
        String title;
        boolean moveAll = false;
        boolean moveAllProportionally = false;
        int moveAllPrevY;

        public Chart(String title) {
            this.title = title;
            this.setLayout(new GridBagLayout());
            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    Chart.this.movingIndex = Chart.this.overIndex;
                    Chart.this.moveAll = event.isControlDown();
                    if (Chart.this.moveAll) {
                        Chart.this.moveAllProportionally = event.isShiftDown();
                        Chart.this.moveAllPrevY = event.getY();
                    }

                }

                public void mouseReleased(MouseEvent event) {
                    Chart.this.movingIndex = -1;
                    Chart.this.moveAll = false;
                }

                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        if (Chart.this.overIndex > 0 && Chart.this.overIndex < Chart.this.points.size()) {
                            Chart.this.points.remove(Chart.this.overIndex);
                            Chart.this.pointsChanged();
                            Chart.this.repaint();
                        }
                    } else if (Chart.this.movingIndex == -1) {
                        if (Chart.this.overIndex == -1) {
                            int mouseX = event.getX();
                            int mouseY = event.getY();
                            if (mouseX >= Chart.this.chartX && mouseX <= Chart.this.chartX + Chart.this.chartWidth) {
                                if (mouseY >= Chart.this.chartY && mouseY <= Chart.this.chartY + Chart.this.chartHeight) {
                                    Chart.Point newPoint = Chart.this.pixelToPoint((float)mouseX, (float)mouseY);
                                    int i = 0;
                                    Chart.Point lastPoint = null;

                                    for(Iterator var7 = Chart.this.points.iterator(); var7.hasNext(); ++i) {
                                        Chart.Point point = (Chart.Point)var7.next();
                                        if (point.x > newPoint.x) {
                                            if (Math.abs(point.x - newPoint.x) < 0.001F) {
                                                return;
                                            }

                                            if (lastPoint != null && Math.abs(lastPoint.x - newPoint.x) < 0.001F) {
                                                return;
                                            }

                                            Chart.this.points.add(i, newPoint);
                                            Chart.this.overIndex = i;
                                            Chart.this.pointsChanged();
                                            Chart.this.repaint();
                                            return;
                                        }

                                        lastPoint = point;
                                    }

                                    Chart.this.overIndex = Chart.this.points.size();
                                    Chart.this.points.add(newPoint);
                                    Chart.this.pointsChanged();
                                    Chart.this.repaint();
                                }
                            }
                        }
                    }
                }
            });
            this.addMouseMotionListener(new MouseMotionListener() {
                public void mouseDragged(MouseEvent event) {
                    if (Chart.this.movingIndex != -1 && Chart.this.movingIndex < Chart.this.points.size()) {
                        float deltaY;
                        if (Chart.this.moveAll) {
                            int newY = event.getY();
                            deltaY = (float)(Chart.this.moveAllPrevY - newY) / (float) Chart.this.chartHeight * (float) Chart.this.maxY;

                            Chart.Point pointx;
                            for(Iterator var4 = Chart.this.points.iterator(); var4.hasNext(); pointx.y = Math.min((float) Chart.this.maxY, Math.max(0.0F, pointx.y + (Chart.this.moveAllProportionally ? deltaY * pointx.y : deltaY)))) {
                                pointx = (Chart.Point)var4.next();
                            }

                            Chart.this.moveAllPrevY = newY;
                        } else {
                            float nextX = Chart.this.movingIndex == Chart.this.points.size() - 1 ? (float) Chart.this.maxX : ((Chart.Point) Chart.this.points.get(Chart.this.movingIndex + 1)).x - 0.001F;
                            if (Chart.this.movingIndex == 0) {
                                nextX = 0.0F;
                            }

                            deltaY = Chart.this.movingIndex == 0 ? 0.0F : ((Chart.Point) Chart.this.points.get(Chart.this.movingIndex - 1)).x + 0.001F;
                            Chart.Point point = (Chart.Point) Chart.this.points.get(Chart.this.movingIndex);
                            point.x = Math.min(nextX, Math.max(deltaY, (float)(event.getX() - Chart.this.chartX) / (float) Chart.this.chartWidth * (float) Chart.this.maxX));
                            point.y = Math.min((float) Chart.this.maxY, (float)Math.max(0, Chart.this.chartHeight - (event.getY() - Chart.this.chartY)) / (float) Chart.this.chartHeight * (float) Chart.this.maxY);
                        }

                        Chart.this.pointsChanged();
                        Chart.this.repaint();
                    }
                }

                public void mouseMoved(MouseEvent event) {
                    int mouseX = event.getX();
                    int mouseY = event.getY();
                    int oldIndex = Chart.this.overIndex;
                    Chart.this.overIndex = -1;
                    int pointSize = Chart.this.isExpanded ? 10 : 6;
                    int i = 0;

                    for(Iterator var7 = Chart.this.points.iterator(); var7.hasNext(); ++i) {
                        Chart.Point point = (Chart.Point)var7.next();
                        int x = Chart.this.chartX + (int)((float) Chart.this.chartWidth * (point.x / (float) Chart.this.maxX));
                        int y = Chart.this.chartY + Chart.this.chartHeight - (int)((float) Chart.this.chartHeight * (point.y / (float) Chart.this.maxY));
                        if (Math.abs(x - mouseX) <= pointSize && Math.abs(y - mouseY) <= pointSize) {
                            Chart.this.overIndex = i;
                            break;
                        }
                    }

                    if (Chart.this.overIndex != oldIndex) {
                        Chart.this.repaint();
                    }

                }
            });
        }

        public void addPoint(float x, float y) {
            this.points.add(new Chart.Point(x, y));
        }

        public void pointsChanged() {
        }

        public float[] getValuesX() {
            float[] values = new float[this.points.size()];
            int i = 0;

            Chart.Point point;
            for(Iterator var3 = this.points.iterator(); var3.hasNext(); values[i++] = point.x) {
                point = (Chart.Point)var3.next();
            }

            return values;
        }

        public float[] getValuesY() {
            float[] values = new float[this.points.size()];
            int i = 0;

            Chart.Point point;
            for(Iterator var3 = this.points.iterator(); var3.hasNext(); values[i++] = point.y) {
                point = (Chart.Point)var3.next();
            }

            return values;
        }

        public void setValues(float[] x, float[] y) {
            this.points.clear();

            for(int i = 0; i < x.length; ++i) {
                this.points.add(new Chart.Point(x[i], y[i]));
            }

        }

        Chart.Point pixelToPoint(float x, float y) {
            Chart.Point point = new Chart.Point();
            point.x = Math.min((float)this.maxX, Math.max(0.0F, x - (float)this.chartX) / (float)this.chartWidth * (float)this.maxX);
            point.y = Math.min((float)this.maxY, Math.max(0.0F, (float)this.chartHeight - (y - (float)this.chartY)) / (float)this.chartHeight * (float)this.maxY);
            return point;
        }

        Chart.Point pointToPixel(Chart.Point point) {
            Chart.Point pixel = new Chart.Point();
            pixel.x = (float)(this.chartX + (int)((float)this.chartWidth * (point.x / (float)this.maxX)));
            pixel.y = (float)(this.chartY + this.chartHeight - (int)((float)this.chartHeight * (point.y / (float)this.maxY)));
            return pixel;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D)graphics;
            FontMetrics metrics = g.getFontMetrics();
            if (this.numberHeight == 0) {
                this.numberHeight = this.getFont().layoutGlyphVector(g.getFontRenderContext(), new char[]{'0'}, 0, 1, 0).getGlyphPixelBounds(0, g.getFontRenderContext(), 0.0F, 0.0F).height;
            }

            int width = this.getWidth();
            if (!this.isExpanded) {
                width = Math.min(150, width);
            }

            width = Math.max(100, width);
            int height = this.getHeight();
            int maxAxisLabelWidth;
            int yAxisWidth;
            if (this.isExpanded) {
                maxAxisLabelWidth = metrics.stringWidth("100%");
                yAxisWidth = maxAxisLabelWidth + 8;
                this.chartX = yAxisWidth;
                this.chartY = this.numberHeight / 2 + 1;
                this.chartWidth = width - yAxisWidth - 2;
                this.chartHeight = height - this.chartY - this.numberHeight - 8;
            } else {
                maxAxisLabelWidth = 0;
                yAxisWidth = 2;
                this.chartX = yAxisWidth;
                this.chartY = 2;
                this.chartWidth = width - yAxisWidth - 2;
                this.chartHeight = height - this.chartY - 3;
            }

            g.setColor(Color.white);
            g.fillRect(this.chartX, this.chartY, this.chartWidth, this.chartHeight);
            g.setColor(Color.black);
            g.drawRect(this.chartX, this.chartY, this.chartWidth, this.chartHeight);
            this.maxX = 1;
            int lastX;
            if (this.isExpanded) {
                lastX = height - this.numberHeight;
            } else {
                lastX = height + 5;
            }

            int lastY = (int)Math.min(10.0F, (float)this.chartWidth / ((float)maxAxisLabelWidth * 1.5F));

            int i;
            int y;
            for(i = 0; i <= lastY; ++i) {
                float percent = (float)i / (float)lastY;
                String label = this.axisLabel((float)this.maxX * percent);
                y = metrics.stringWidth(label);
                int x = (int)((float)yAxisWidth + (float)this.chartWidth * percent);
                if (i != 0 && i != lastY) {
                    g.setColor(Color.lightGray);
                    g.drawLine(x, this.chartY + 1, x, this.chartY + this.chartHeight);
                    g.setColor(Color.black);
                }

                g.drawLine(x, lastX - 4, x, lastX - 8);
                if (this.isExpanded) {
                    x -= y / 2;
                    if (i == lastY) {
                        x = Math.min(x, width - y);
                    }

                    g.drawString(label, x, lastX + this.numberHeight);
                }
            }

            this.maxY = 1;
            lastX = this.isExpanded ? Math.min(10, this.chartHeight / (this.numberHeight * 3)) : 4;

            for(lastY = 0; lastY <= lastX; ++lastY) {
                float percent = (float)lastY / (float)lastX;
                String label = this.axisLabel((float)this.maxY * percent);
                int labelWidth = metrics.stringWidth(label);
                y = (int)((float)(this.chartY + this.chartHeight) - (float)this.chartHeight * percent);
                if (this.isExpanded) {
                    g.drawString(label, yAxisWidth - 6 - labelWidth, y + this.numberHeight / 2);
                }

                if (lastY != 0 && lastY != lastX) {
                    g.setColor(Color.lightGray);
                    g.drawLine(this.chartX, y, this.chartX + this.chartWidth - 1, y);
                    g.setColor(Color.black);
                }

                g.drawLine(yAxisWidth - 4, y, yAxisWidth, y);
            }

            lastX = metrics.stringWidth(this.title);
            lastY = yAxisWidth + this.chartWidth / 2 - lastX / 2;
            i = this.chartY + this.chartHeight / 2 - this.numberHeight / 2;
            g.setColor(Color.white);
            g.fillRect(lastY - 2, i - 2, lastX + 4, this.numberHeight + 4);
            g.setColor(Color.lightGray);
            g.drawString(this.title, lastY, i + this.numberHeight);
            g.setColor(Color.blue);
            g.setStroke(new BasicStroke(this.isExpanded ? 3.0F : 2.0F));
            lastX = -1;
            lastY = -1;

            Chart.Point point;
            for(Iterator var21 = this.points.iterator(); var21.hasNext(); lastY = (int)point.y) {
                 point = (Chart.Point)var21.next();
                point = this.pointToPixel(point);
                if (lastX != -1) {
                    g.drawLine(lastX, lastY, (int)point.x, (int)point.y);
                }

                lastX = (int)point.x;
            }

            g.drawLine(lastX, lastY, this.chartX + this.chartWidth - 1, lastY);
            i = 0;

            for(int n = this.points.size(); i < n; ++i) {
                point = (Chart.Point)this.points.get(i);
                Chart.Point pixel = this.pointToPixel(point);
                if (this.overIndex == i) {
                    g.setColor(Color.red);
                } else {
                    g.setColor(Color.black);
                }

                String label = this.valueLabel(point.y);
                int labelWidth = metrics.stringWidth(label);
                int pointSize = this.isExpanded ? 10 : 6;
                int x = (int)pixel.x - pointSize / 2;
                y = (int)pixel.y - pointSize / 2;
                g.fillOval(x, y, pointSize, pointSize);
                if (this.isExpanded) {
                    g.setColor(Color.black);
                    x = Math.max(this.chartX + 2, Math.min(this.chartX + this.chartWidth - labelWidth, x));
                    y -= 3;
                    if (y < this.chartY + this.numberHeight + 3) {
                        y += 27;
                    } else if (n > 1) {
                        Chart.Point comparePoint = i == n - 1 ? (Chart.Point)this.points.get(i - 1) : (Chart.Point)this.points.get(i + 1);
                        if (y < this.chartY + this.chartHeight - 27 && comparePoint.y > point.y) {
                            y += 27;
                        }
                    }

                    g.drawString(label, x, y);
                }
            }

        }

        private String valueLabel(float value) {
            value = (float)((int)(value * 1000.0F)) / 10.0F;
            return value % 1.0F == 0.0F ? String.valueOf((int)value) + '%' : String.valueOf(value) + '%';
        }

        private String axisLabel(float value) {
            value = (float)((int)(value * 100.0F));
            return value % 1.0F == 0.0F ? String.valueOf((int)value) + '%' : String.valueOf(value) + '%';
        }

        public boolean isExpanded() {
            return this.isExpanded;
        }

        public void setExpanded(boolean isExpanded) {
            this.isExpanded = isExpanded;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public class Point {
            public float x;
            public float y;

            public Point() {
            }

            public Point(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    public class CustomShadingPanel extends EditorPanel {
        JPanel imagesPanel;
        JList imageList;
        DefaultListModel<String> imageListModel;
        String lastDir;
        final ParticleEditor editor;
        final CustomShading shading;

        public CustomShadingPanel(final ParticleEditor editor, String name, String description) {
            super((ParticleEmitter.ParticleValue)null, name, description);
            this.editor = editor;
            this.shading = editor.renderer.customShading;
            JPanel contentPanel = this.getContentPanel();
            JPanel shaderStagePanel = this.createShaderStagePanel(editor, contentPanel, true);
            contentPanel.add(shaderStagePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            shaderStagePanel = this.createShaderStagePanel(editor, contentPanel, false);
            contentPanel.add(shaderStagePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.imagesPanel = new JPanel(new GridBagLayout());
            contentPanel.add(this.imagesPanel, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.imagesPanel.add(new JLabel("Extra Texture Units"));
            this.imageListModel = new DefaultListModel();

            for(int i = 0; i < this.shading.extraTexturePaths.size; ++i) {
                String path = (String)this.shading.extraTexturePaths.get(i);
                this.imageListModel.addElement(this.textureDisplayName(i, path));
            }

            this.imageList = new JList(this.imageListModel);
            this.imageList.setFixedCellWidth(200);
            this.imageList.setSelectionMode(0);
            this.imagesPanel.add(this.imageList, new GridBagConstraints(0, 1, 1, 4, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            JButton addButton = new JButton("Add");
            this.imagesPanel.add(addButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileDialog dialog = new FileDialog(editor, "Open Image", 0);
                    if (CustomShadingPanel.this.lastDir != null) {
                        dialog.setDirectory(CustomShadingPanel.this.lastDir);
                    }

                    dialog.setVisible(true);
                    String file = dialog.getFile();
                    String dir = dialog.getDirectory();
                    if (dir != null && file != null && file.trim().length() != 0) {
                        CustomShadingPanel.this.lastDir = dir;
                        CustomShadingPanel.this.addTexture((new File(dir, file)).getAbsolutePath());
                    }
                }
            });
            JButton upButton = new JButton("↑");
            this.imagesPanel.add(upButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            upButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = CustomShadingPanel.this.imageList.getSelectedIndex();
                    if (index > 0) {
                        CustomShadingPanel.this.swapTexture(index, index - 1);
                        CustomShadingPanel.this.imageList.setSelectedIndex(index - 1);
                    }
                }
            });
            JButton downButton = new JButton("↓");
            this.imagesPanel.add(downButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = CustomShadingPanel.this.imageList.getSelectedIndex();
                    if (index >= 0 && index < CustomShadingPanel.this.imageList.getModel().getSize() - 1) {
                        ParticleEmitter emitter = editor.getEmitter();
                        CustomShadingPanel.this.swapTexture(index, index + 1);
                        CustomShadingPanel.this.imageList.setSelectedIndex(index + 1);
                    }
                }
            });
            JButton removeButton = new JButton("X");
            this.imagesPanel.add(removeButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = CustomShadingPanel.this.imageList.getSelectedIndex();
                    if (index >= 0) {
                        CustomShadingPanel.this.removeTexture(index);
                    }
                }
            });
            JButton reloadButton = new JButton("Reload");
            this.imagesPanel.add(reloadButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            reloadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = CustomShadingPanel.this.imageList.getSelectedIndex();
                    if (index >= 0) {
                        CustomShadingPanel.this.reloadTexture(index);
                    }
                }
            });
        }

        private JPanel createShaderStagePanel(final ParticleEditor editor, JPanel contentPanel, final boolean isVertexShader) {
            JPanel buttonsPanel = new JPanel(new GridLayout(5, 1));
            JLabel label = new JLabel(isVertexShader ? "Vertex Shader" : "Frag. Shader");
            buttonsPanel.add(label);
            JButton defaultButton = new JButton("Default");
            buttonsPanel.add(defaultButton);
            defaultButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (isVertexShader) {
                        CustomShadingPanel.this.shading.setVertexShaderFile((String)null);
                    } else {
                        CustomShadingPanel.this.shading.setFragmentShaderFile((String)null);
                    }

                    CustomShadingPanel.this.displayErrors();
                }
            });
            JButton setButton = new JButton("Set");
            buttonsPanel.add(setButton);
            setButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileDialog dialog = new FileDialog(editor, isVertexShader ? "Open Vertex Shader File" : "Open Fragment Shader File", 0);
                    if (CustomShadingPanel.this.lastDir != null) {
                        dialog.setDirectory(CustomShadingPanel.this.lastDir);
                    }

                    dialog.setVisible(true);
                    String file = dialog.getFile();
                    String dir = dialog.getDirectory();
                    if (dir != null && file != null && file.trim().length() != 0) {
                        CustomShadingPanel.this.lastDir = dir;
                        String path = (new File(dir, file)).getAbsolutePath();
                        if (isVertexShader) {
                            CustomShadingPanel.this.shading.setVertexShaderFile(path);
                        } else {
                            CustomShadingPanel.this.shading.setFragmentShaderFile(path);
                        }

                        CustomShadingPanel.this.displayErrors();
                    }
                }
            });
            JButton reloadButton = new JButton("Reload");
            buttonsPanel.add(reloadButton);
            reloadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (isVertexShader) {
                        CustomShadingPanel.this.shading.reloadVertexShader();
                    } else {
                        CustomShadingPanel.this.shading.reloadFragmentShader();
                    }

                    CustomShadingPanel.this.displayErrors();
                }
            });
            JButton showButton = new JButton("Show");
            buttonsPanel.add(showButton);
            showButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    JTextArea text = new JTextArea(isVertexShader ? CustomShadingPanel.this.shading.vertexShaderCode : CustomShadingPanel.this.shading.fragmentShaderCode);
                    text.setEditable(false);
                    JOptionPane.showMessageDialog(editor, text, isVertexShader ? "Current vertex shader code" : "Current fragment shader code", 1);
                }
            });
            return buttonsPanel;
        }

        protected void displayErrors() {
            if (this.shading.hasShaderErrors) {
                JOptionPane.showMessageDialog(this.editor, this.shading.shaderErrorMessage, "Shader Error", 0);
            } else if (this.shading.hasMissingSamplers) {
                JOptionPane.showMessageDialog(this.editor, this.shading.missingSamplerMessage, "Missing texture sampler", 2);
            }

        }

        private String textureDisplayName(int index, String path) {
            int unit = index + 1;
            return "u_texture" + unit + ": " + (new File(path)).getName();
        }

        protected void removeTexture(int index) {
            this.imageListModel.remove(index);
            this.shading.removeTexture(index);
            this.revalidate();
            this.displayErrors();
        }

        protected void swapTexture(int indexA, int indexB) {
            this.shading.swapTexture(indexA, indexB);
            String pathA = (String)this.shading.extraTexturePaths.get(indexA);
            String pathB = (String)this.shading.extraTexturePaths.get(indexB);
            this.imageListModel.set(indexA, this.textureDisplayName(indexA, pathA));
            this.imageListModel.set(indexB, this.textureDisplayName(indexB, pathB));
            this.revalidate();
            this.displayErrors();
        }

        protected void addTexture(String absolutePath) {
            this.imageListModel.addElement(this.textureDisplayName(this.imageListModel.getSize(), absolutePath));
            this.shading.addTexture(absolutePath);
            this.revalidate();
            this.displayErrors();
        }

        protected void reloadTexture(int index) {
            this.shading.reloadTexture(index);
            this.displayErrors();
        }
    }

    class ImagePanel extends EditorPanel {
        JPanel imagesPanel;
        JList imageList;
        DefaultListModel<String> imageListModel;
        String lastDir;

        public ImagePanel(final ParticleEditor editor, String name, String description) {
            super((ParticleEmitter.ParticleValue)null, name, description);
            JPanel contentPanel = this.getContentPanel();
            JPanel modesPanel = new JPanel(new GridLayout(3, 1));
            contentPanel.add(modesPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            JButton downButton = new JButton("Add");
            modesPanel.add(downButton);
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileDialog dialog = new FileDialog(editor, "Open Image", 0);
                    if (ImagePanel.this.lastDir != null) {
                        dialog.setDirectory(ImagePanel.this.lastDir);
                    }

                    dialog.setMultipleMode(true);
                    dialog.setVisible(true);
                    File[] files = dialog.getFiles();
                    String dir = dialog.getDirectory();
                    if (dir != null && files != null) {
                        ImagePanel.this.lastDir = dir;
                        ParticleEmitter emitter = editor.getEmitter();
                        File[] var6 = files;
                        int var7 = files.length;

                        for(int var8 = 0; var8 < var7; ++var8) {
                            File file = var6[var8];
                            emitter.getImagePaths().add(file.getAbsolutePath());
                        }

                        emitter.getSprites().clear();
                        ImagePanel.this.updateImageList(emitter.getImagePaths());
                    }
                }
            });
            JButton removeButton = new JButton("Default");
            modesPanel.add(removeButton);
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ParticleEmitter emitter = editor.getEmitter();
                    emitter.setImagePaths(new Array(new String[]{"particle.png"}));
                    emitter.getSprites().clear();
                    ImagePanel.this.updateImageList(emitter.getImagePaths());
                }
            });
            JButton defaultPremultButton = new JButton("Default (Premultiplied Alpha)");
            modesPanel.add(defaultPremultButton);
            defaultPremultButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ParticleEmitter emitter = editor.getEmitter();
                    emitter.setImagePaths(new Array(new String[]{"pre_particle.png"}));
                    emitter.getSprites().clear();
                    ImagePanel.this.updateImageList(emitter.getImagePaths());
                }
            });
            modesPanel = new JPanel(new GridLayout(4, 1));
            contentPanel.add(modesPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            JLabel label = new JLabel("Sprite mode:");
            modesPanel.add(label);
            ButtonGroup checkboxGroup = new ButtonGroup();
            JRadioButton singleCheckbox = new JRadioButton("Single", editor.getEmitter().getSpriteMode() == ParticleEmitter.SpriteMode.single);
            modesPanel.add(singleCheckbox);
            checkboxGroup.add(singleCheckbox);
            singleCheckbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == 1) {
                        editor.getEmitter().setSpriteMode(ParticleEmitter.SpriteMode.single);
                    }

                }
            });
            JRadioButton randomCheckbox = new JRadioButton("Random", editor.getEmitter().getSpriteMode() == ParticleEmitter.SpriteMode.random);
            modesPanel.add(randomCheckbox);
            checkboxGroup.add(randomCheckbox);
            randomCheckbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == 1) {
                        editor.getEmitter().setSpriteMode(ParticleEmitter.SpriteMode.random);
                    }

                }
            });
            JRadioButton animatedCheckbox = new JRadioButton("Animated", editor.getEmitter().getSpriteMode() == ParticleEmitter.SpriteMode.animated);
            modesPanel.add(animatedCheckbox);
            checkboxGroup.add(animatedCheckbox);
            animatedCheckbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == 1) {
                        editor.getEmitter().setSpriteMode(ParticleEmitter.SpriteMode.animated);
                    }

                }
            });
            this.imagesPanel = new JPanel(new GridBagLayout());
            contentPanel.add(this.imagesPanel, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.imageListModel = new DefaultListModel();
            this.imageList = new JList(this.imageListModel);
            this.imageList.setFixedCellWidth(250);
            this.imageList.setSelectionMode(0);
            this.imagesPanel.add(this.imageList, new GridBagConstraints(0, 0, 1, 3, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            JButton upButton = new JButton("↑");
            this.imagesPanel.add(upButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            upButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = ImagePanel.this.imageList.getSelectedIndex();
                    if (index > 0) {
                        ParticleEmitter emitter = editor.getEmitter();
                        String imagePath = (String)emitter.getImagePaths().removeIndex(index);
                        emitter.getImagePaths().insert(index - 1, imagePath);
                        emitter.getSprites().clear();
                        ImagePanel.this.updateImageList(emitter.getImagePaths());
                        ImagePanel.this.imageList.setSelectedIndex(index - 1);
                    }
                }
            });
            downButton = new JButton("↓");
            this.imagesPanel.add(downButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = ImagePanel.this.imageList.getSelectedIndex();
                    if (index >= 0 && index < ImagePanel.this.imageList.getModel().getSize() - 1) {
                        ParticleEmitter emitter = editor.getEmitter();
                        String imagePath = (String)emitter.getImagePaths().removeIndex(index);
                        emitter.getImagePaths().insert(index + 1, imagePath);
                        emitter.getSprites().clear();
                        ImagePanel.this.updateImageList(emitter.getImagePaths());
                        ImagePanel.this.imageList.setSelectedIndex(index + 1);
                    }
                }
            });
            removeButton = new JButton("X");
            this.imagesPanel.add(removeButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = ImagePanel.this.imageList.getSelectedIndex();
                    if (index >= 0) {
                        ParticleEmitter emitter = editor.getEmitter();
                        Array<String> imagePaths = emitter.getImagePaths();
                        imagePaths.removeIndex(index);
                        if (imagePaths.size == 0) {
                            imagePaths.add("particle.png");
                        }

                        emitter.getSprites().clear();
                        ImagePanel.this.updateImageList(imagePaths);
                    }
                }
            });
            this.updateImageList(editor.getEmitter().getImagePaths());
        }

        public void updateImageList(Array<String> imagePaths) {
            if (imagePaths != null && imagePaths.size > 0) {
                this.imagesPanel.setVisible(true);
                this.imageListModel.removeAllElements();
                Array.ArrayIterator var2 = imagePaths.iterator();

                while(var2.hasNext()) {
                    String imagePath = (String)var2.next();
                    this.imageListModel.addElement((new File(imagePath)).getName());
                }
            } else {
                this.imagesPanel.setVisible(false);
            }

            this.revalidate();
        }
    }

    class CountPanel extends EditorPanel {
        Slider maxSlider;
        Slider minSlider;

        public CountPanel(final ParticleEditor editor, String name, String description) {
            super((ParticleEmitter.ParticleValue)null, name, description);
            this.initializeComponents();
            this.maxSlider.setValue((float)editor.getEmitter().getMaxParticleCount());
            this.maxSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    editor.getEmitter().setMaxParticleCount((int) CountPanel.this.maxSlider.getValue());
                }
            });
            this.minSlider.setValue((float)editor.getEmitter().getMinParticleCount());
            this.minSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    editor.getEmitter().setMinParticleCount((int) CountPanel.this.minSlider.getValue());
                }
            });
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            JLabel label = new JLabel("Min:");
            contentPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.minSlider = new Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.minSlider, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            label = new JLabel("Max:");
            contentPanel.add(label, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
            this.maxSlider = new Slider(0.0F, 0.0F, 99999.0F, 1.0F, 0.0F, 500.0F);
            contentPanel.add(this.maxSlider, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        }
    }

    class PercentagePanel extends EditorPanel {
        final ParticleEmitter.ScaledNumericValue value;
        JButton expandButton;
        Chart chart;

        public PercentagePanel(ParticleEmitter.ScaledNumericValue value, String chartTitle, String name, String description) {
            super(value, name, description);
            this.value = value;
            this.initializeComponents(chartTitle);
            this.chart.setValues(value.getTimeline(), value.getScaling());
            this.expandButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    PercentagePanel.this.chart.setExpanded(!PercentagePanel.this.chart.isExpanded());
                    boolean expanded = PercentagePanel.this.chart.isExpanded();
                    GridBagLayout layout = (GridBagLayout) PercentagePanel.this.getContentPanel().getLayout();
                    GridBagConstraints chartConstraints = layout.getConstraints(PercentagePanel.this.chart);
                    GridBagConstraints expandButtonConstraints = layout.getConstraints(PercentagePanel.this.expandButton);
                    if (expanded) {
                        PercentagePanel.this.chart.setPreferredSize(new Dimension(150, 200));
                        PercentagePanel.this.expandButton.setText("-");
                        chartConstraints.weightx = 1.0;
                        expandButtonConstraints.weightx = 0.0;
                    } else {
                        PercentagePanel.this.chart.setPreferredSize(new Dimension(150, 62));
                        PercentagePanel.this.expandButton.setText("+");
                        chartConstraints.weightx = 0.0;
                        expandButtonConstraints.weightx = 1.0;
                    }

                    layout.setConstraints(PercentagePanel.this.chart, chartConstraints);
                    layout.setConstraints(PercentagePanel.this.expandButton, expandButtonConstraints);
                    PercentagePanel.this.chart.revalidate();
                }
            });
        }

        private void initializeComponents(String chartTitle) {
            JPanel contentPanel = this.getContentPanel();
            this.chart = new Chart(chartTitle) {
                public void pointsChanged() {
                    PercentagePanel.this.value.setTimeline(PercentagePanel.this.chart.getValuesX());
                    PercentagePanel.this.value.setScaling(PercentagePanel.this.chart.getValuesY());
                }
            };
            this.chart.setPreferredSize(new Dimension(150, 62));
            contentPanel.add(this.chart, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 1, new Insets(0, 0, 0, 0), 0, 0));
            this.expandButton = new JButton("+");
            this.expandButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            contentPanel.add(this.expandButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
        }
    }

    class OptionsPanel extends EditorPanel {
        JCheckBox attachedCheckBox;
        JCheckBox continuousCheckbox;
        JCheckBox alignedCheckbox;
        JCheckBox additiveCheckbox;
        JCheckBox behindCheckbox;
        JCheckBox premultipliedAlphaCheckbox;

        public OptionsPanel(final ParticleEditor editor, String name, String description) {
            super((ParticleEmitter.ParticleValue)null, name, description);
            this.initializeComponents();
            this.attachedCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setAttached(OptionsPanel.this.attachedCheckBox.isSelected());
                }
            });
            this.continuousCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setContinuous(OptionsPanel.this.continuousCheckbox.isSelected());
                }
            });
            this.alignedCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setAligned(OptionsPanel.this.alignedCheckbox.isSelected());
                }
            });
            this.additiveCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setAdditive(OptionsPanel.this.additiveCheckbox.isSelected());
                }
            });
            this.behindCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setBehind(OptionsPanel.this.behindCheckbox.isSelected());
                }
            });
            this.premultipliedAlphaCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    editor.getEmitter().setPremultipliedAlpha(OptionsPanel.this.premultipliedAlphaCheckbox.isSelected());
                }
            });
            ParticleEmitter emitter = editor.getEmitter();
            this.attachedCheckBox.setSelected(emitter.isAttached());
            this.continuousCheckbox.setSelected(emitter.isContinuous());
            this.alignedCheckbox.setSelected(emitter.isAligned());
            this.additiveCheckbox.setSelected(emitter.isAdditive());
            this.behindCheckbox.setSelected(emitter.isBehind());
            this.premultipliedAlphaCheckbox.setSelected(emitter.isPremultipliedAlpha());
        }

        private void initializeComponents() {
            JPanel contentPanel = this.getContentPanel();
            JLabel label = new JLabel("Additive:");
            contentPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.additiveCheckbox = new JCheckBox();
            contentPanel.add(this.additiveCheckbox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
            label = new JLabel("Attached:");
            contentPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.attachedCheckBox = new JCheckBox();
            contentPanel.add(this.attachedCheckBox, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
            label = new JLabel("Continuous:");
            contentPanel.add(label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.continuousCheckbox = new JCheckBox();
            contentPanel.add(this.continuousCheckbox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
            label = new JLabel("Aligned:");
            contentPanel.add(label, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.alignedCheckbox = new JCheckBox();
            contentPanel.add(this.alignedCheckbox, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
            label = new JLabel("Behind:");
            contentPanel.add(label, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.behindCheckbox = new JCheckBox();
            contentPanel.add(this.behindCheckbox, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
            label = new JLabel("Premultiplied Alpha:");
            contentPanel.add(label, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
            this.premultipliedAlphaCheckbox = new JCheckBox();
            contentPanel.add(this.premultipliedAlphaCheckbox, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        }
    }

    class ScaledNumericPanel extends EditorPanel {
        final ParticleEmitter.ScaledNumericValue value;
        Slider lowMinSlider;
        Slider lowMaxSlider;
        Slider highMinSlider;
        Slider highMaxSlider;
        JCheckBox relativeCheckBox;
        JCheckBox independentCheckbox;
        Chart chart;
        JPanel formPanel;
        JButton expandButton;
        JButton lowRangeButton;
        JButton highRangeButton;

        public ScaledNumericPanel(final ParticleEmitter.ScaledNumericValue value, String chartTitle, String name, String description) {
            super(value, name, description);
            this.value = value;
            final boolean hasIndependent = value instanceof ParticleEmitter.IndependentScaledNumericValue;
            this.initializeComponents(chartTitle, hasIndependent);
            this.lowMinSlider.setValue(value.getLowMin());
            this.lowMaxSlider.setValue(value.getLowMax());
            this.highMinSlider.setValue(value.getHighMin());
            this.highMaxSlider.setValue(value.getHighMax());
            this.chart.setValues(value.getTimeline(), value.getScaling());
            this.relativeCheckBox.setSelected(value.isRelative());
            if (hasIndependent) {
                this.independentCheckbox.setSelected(((ParticleEmitter.IndependentScaledNumericValue)value).isIndependent());
            }

            this.lowMinSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setLowMin(Float.valueOf(ScaledNumericPanel.this.lowMinSlider.getValue()));
                    if (!ScaledNumericPanel.this.lowMaxSlider.isVisible()) {
                        value.setLowMax(Float.valueOf(ScaledNumericPanel.this.lowMinSlider.getValue()));
                    }

                }
            });
            this.lowMaxSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setLowMax(Float.valueOf(ScaledNumericPanel.this.lowMaxSlider.getValue()));
                }
            });
            this.highMinSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setHighMin(Float.valueOf(ScaledNumericPanel.this.highMinSlider.getValue()));
                    if (!ScaledNumericPanel.this.highMaxSlider.isVisible()) {
                        value.setHighMax(Float.valueOf(ScaledNumericPanel.this.highMinSlider.getValue()));
                    }

                }
            });
            this.highMaxSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    value.setHighMax(Float.valueOf(ScaledNumericPanel.this.highMaxSlider.getValue()));
                }
            });
            this.relativeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    value.setRelative(ScaledNumericPanel.this.relativeCheckBox.isSelected());
                }
            });
            if (hasIndependent) {
                this.independentCheckbox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        ((ParticleEmitter.IndependentScaledNumericValue)value).setIndependent(ScaledNumericPanel.this.independentCheckbox.isSelected());
                    }
                });
            }

            this.lowRangeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    boolean visible = !ScaledNumericPanel.this.lowMaxSlider.isVisible();
                    ScaledNumericPanel.this.lowMaxSlider.setVisible(visible);
                    ScaledNumericPanel.this.lowRangeButton.setText(visible ? "<" : ">");
                    GridBagLayout layout = (GridBagLayout) ScaledNumericPanel.this.formPanel.getLayout();
                    GridBagConstraints constraints = layout.getConstraints(ScaledNumericPanel.this.lowRangeButton);
                    constraints.gridx = visible ? 5 : 4;
                    layout.setConstraints(ScaledNumericPanel.this.lowRangeButton, constraints);
                    Slider slider = visible ? ScaledNumericPanel.this.lowMaxSlider : ScaledNumericPanel.this.lowMinSlider;
                    value.setLowMax(Float.valueOf(slider.getValue()));
                }
            });
            this.highRangeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    boolean visible = !ScaledNumericPanel.this.highMaxSlider.isVisible();
                    ScaledNumericPanel.this.highMaxSlider.setVisible(visible);
                    ScaledNumericPanel.this.highRangeButton.setText(visible ? "<" : ">");
                    GridBagLayout layout = (GridBagLayout) ScaledNumericPanel.this.formPanel.getLayout();
                    GridBagConstraints constraints = layout.getConstraints(ScaledNumericPanel.this.highRangeButton);
                    constraints.gridx = visible ? 5 : 4;
                    layout.setConstraints(ScaledNumericPanel.this.highRangeButton, constraints);
                    Slider slider = visible ? ScaledNumericPanel.this.highMaxSlider : ScaledNumericPanel.this.highMinSlider;
                    value.setHighMax(Float.valueOf(slider.getValue()));
                }
            });
            this.expandButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ScaledNumericPanel.this.chart.setExpanded(!ScaledNumericPanel.this.chart.isExpanded());
                    boolean expanded = ScaledNumericPanel.this.chart.isExpanded();
                    GridBagLayout layout = (GridBagLayout) ScaledNumericPanel.this.getContentPanel().getLayout();
                    GridBagConstraints chartConstraints = layout.getConstraints(ScaledNumericPanel.this.chart);
                    GridBagConstraints expandButtonConstraints = layout.getConstraints(ScaledNumericPanel.this.expandButton);
                    if (expanded) {
                        ScaledNumericPanel.this.chart.setPreferredSize(new Dimension(150, 200));
                        ScaledNumericPanel.this.expandButton.setText("-");
                        chartConstraints.weightx = 1.0;
                        expandButtonConstraints.weightx = 0.0;
                    } else {
                        ScaledNumericPanel.this.chart.setPreferredSize(new Dimension(150, 30));
                        ScaledNumericPanel.this.expandButton.setText("+");
                        chartConstraints.weightx = 0.0;
                        expandButtonConstraints.weightx = 1.0;
                    }

                    layout.setConstraints(ScaledNumericPanel.this.chart, chartConstraints);
                    layout.setConstraints(ScaledNumericPanel.this.expandButton, expandButtonConstraints);
                    ScaledNumericPanel.this.relativeCheckBox.setVisible(!expanded);
                    if (hasIndependent) {
                        ScaledNumericPanel.this.independentCheckbox.setVisible(!expanded);
                    }

                    ScaledNumericPanel.this.formPanel.setVisible(!expanded);
                    ScaledNumericPanel.this.chart.revalidate();
                }
            });
            if (value.getLowMin() == value.getLowMax()) {
                this.lowRangeButton.doClick(0);
            }

            if (value.getHighMin() == value.getHighMax()) {
                this.highRangeButton.doClick(0);
            }

        }

        public JPanel getFormPanel() {
            return this.formPanel;
        }

        private void initializeComponents(String chartTitle, boolean hasIndependent) {
            JPanel contentPanel = this.getContentPanel();
            this.formPanel = new JPanel(new GridBagLayout());
            contentPanel.add(this.formPanel, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            JLabel label = new JLabel("High:");
            this.formPanel.add(label, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.highMinSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            this.formPanel.add(this.highMinSlider, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.highMaxSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            this.formPanel.add(this.highMaxSlider, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
            this.highRangeButton = new JButton("<");
            this.highRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            this.formPanel.add(this.highRangeButton, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
            label = new JLabel("Low:");
            this.formPanel.add(label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
            this.lowMinSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            this.formPanel.add(this.lowMinSlider, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
            this.lowMaxSlider = new Slider(0.0F, -99999.0F, 99999.0F, 1.0F, -400.0F, 400.0F);
            this.formPanel.add(this.lowMaxSlider, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
            this.lowRangeButton = new JButton("<");
            this.lowRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            this.formPanel.add(this.lowRangeButton, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
            this.chart = new Chart(chartTitle) {
                public void pointsChanged() {
                    ScaledNumericPanel.this.value.setTimeline(ScaledNumericPanel.this.chart.getValuesX());
                    ScaledNumericPanel.this.value.setScaling(ScaledNumericPanel.this.chart.getValuesY());
                }
            };
            contentPanel.add(this.chart, new GridBagConstraints(6, 5, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
            this.chart.setPreferredSize(new Dimension(150, 30));
            this.expandButton = new JButton("+");
            contentPanel.add(this.expandButton, new GridBagConstraints(7, 5, 1, 1, 0.0, 0.0, 16, 0, new Insets(0, 5, 0, 0), 0, 0));
            this.expandButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            this.relativeCheckBox = new JCheckBox("Relative");
            contentPanel.add(this.relativeCheckBox, new GridBagConstraints(8, 5, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
            if (hasIndependent) {
                this.independentCheckbox = new JCheckBox("Independent");
                contentPanel.add(this.independentCheckbox, new GridBagConstraints(8, 5, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
            }

        }
    }
    public static final String DEFAULT_PARTICLE = "particle.png";
        public static final String DEFAULT_PREMULT_PARTICLE = "pre_particle.png";
        public Renderer renderer = new Renderer();
        Canvas lwjglCanvas;
        JPanel rowsPanel;
        JPanel editRowsPanel;
        EffectPanel effectPanel;
        PreviewImagePanel previewImagePanel;
        private JSplitPane splitPane;
        OrthographicCamera worldCamera;
        OrthographicCamera textCamera;
        ParticleEmitter.NumericValue pixelsPerMeter;
        ParticleEmitter.NumericValue zoomLevel;
        ParticleEmitter.NumericValue deltaMultiplier;
        ParticleEmitter.GradientColorValue backgroundColor;
        float pixelsPerMeterPrev;
        float zoomLevelPrev;
        ParticleEffect effect = new ParticleEffect();
        File effectFile;
        final HashMap<ParticleEmitter, ParticleData> particleData = new HashMap();
        JCheckBox renderGridCheckBox;

        public ParticleEditor() {
            super("Particle Editor");
            this.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent event) {
                    System.exit(0);
                }
            });
            this.initializeComponents();
            this.setSize(1000, 950);
            this.setLocationRelativeTo((Component)null);
            this.setDefaultCloseOperation(2);
            this.setVisible(true);
        }

        private void createCanvas() {
            this.lwjglCanvas = new Canvas() {
                private final Dimension minSize = new Dimension(1, 1);
                private float scaleX;
                private float scaleY;

                public final void addNotify() {
                    super.addNotify();
                    AffineTransform transform = this.getGraphicsConfiguration().getDefaultTransform();
                    this.scaleX = (float)transform.getScaleX();
                    this.scaleY = (float)transform.getScaleY();
                }

                public Dimension getMinimumSize() {
                    return this.minSize;
                }

                public int getWidth() {
                    return Math.round((float)super.getWidth() * this.scaleX);
                }

                public int getHeight() {
                    return Math.round((float)super.getHeight() * this.scaleY);
                }
            };
            this.lwjglCanvas.setSize(1, 1);
            this.lwjglCanvas.setIgnoreRepaint(true);
            new LwjglApplication(this.renderer, this.lwjglCanvas);
        }

        void reloadRows() {
            this.editRowsPanel.removeAll();
            this.addEditorRow(new NumericPanel(this.pixelsPerMeter, "Pixels per meter", ""));
            this.addEditorRow(new NumericPanel(this.zoomLevel, "Zoom level", ""));
            this.addEditorRow(new NumericPanel(this.deltaMultiplier, "Delta multiplier", ""));
            this.addEditorRow(new GradientPanel(this.backgroundColor, "Background color", "", true));
            this.previewImagePanel = new PreviewImagePanel(this, "Preview Image", "");
            this.addEditorRow(this.previewImagePanel);
            JPanel gridPanel = new JPanel(new GridLayout());
            boolean previousSelected = this.renderGridCheckBox != null && this.renderGridCheckBox.isSelected();
            this.renderGridCheckBox = new JCheckBox("Render Grid", previousSelected);
            gridPanel.add(this.renderGridCheckBox, new GridBagConstraints());
            this.addEditorRow(gridPanel);
            this.addEditorRow(new CustomShadingPanel(this, "Shading", "Custom shader and multi-texture preview."));
            this.rowsPanel.removeAll();
            ParticleEmitter emitter = this.getEmitter();
            this.addRow(new ImagePanel(this, "Images", ""));
            this.addRow(new CountPanel(this, "Count", "Min number of particles at all times, max number of particles allowed."));
            this.addRow(new RangedNumericPanel(emitter.getDelay(), "Delay", "Time from beginning of effect to emission start, in milliseconds."));
            this.addRow(new RangedNumericPanel(emitter.getDuration(), "Duration", "Time particles will be emitted, in milliseconds."));
            this.addRow(new ScaledNumericPanel(emitter.getEmission(), "Duration", "Emission", "Number of particles emitted per second."));
            this.addRow(new ScaledNumericPanel(emitter.getLife(), "Duration", "Life", "Time particles will live, in milliseconds."));
            this.addRow(new ScaledNumericPanel(emitter.getLifeOffset(), "Duration", "Life Offset", "Particle starting life consumed, in milliseconds."));
            this.addRow(new RangedNumericPanel(emitter.getXOffsetValue(), "X Offset", "Amount to offset a particle's starting X location, in world units."));
            this.addRow(new RangedNumericPanel(emitter.getYOffsetValue(), "Y Offset", "Amount to offset a particle's starting Y location, in world units."));
            this.addRow(new SpawnPanel(this, emitter.getSpawnShape(), "Spawn", "Shape used to spawn particles."));
            this.addRow(new ScaledNumericPanel(emitter.getSpawnWidth(), "Duration", "Spawn Width", "Width of the spawn shape, in world units."));
            this.addRow(new ScaledNumericPanel(emitter.getSpawnHeight(), "Duration", "Spawn Height", "Height of the spawn shape, in world units."));
            this.addRow(new ScaledNumericPanel(emitter.getXScale(), "Life", "X Size", "Particle x size, in world units. If Y Size is not active, this also controls the y size"));
            this.addRow(new ScaledNumericPanel(emitter.getYScale(), "Life", "Y Size", "Particle y size, in world units."));
            this.addRow(new ScaledNumericPanel(emitter.getVelocity(), "Life", "Velocity", "Particle speed, in world units per second."));
            this.addRow(new ScaledNumericPanel(emitter.getAngle(), "Life", "Angle", "Particle emission angle, in degrees."));
            this.addRow(new ScaledNumericPanel(emitter.getRotation(), "Life", "Rotation", "Particle rotation, in degrees."));
            this.addRow(new ScaledNumericPanel(emitter.getWind(), "Life", "Wind", "Wind strength, in world units per second."));
            this.addRow(new ScaledNumericPanel(emitter.getGravity(), "Life", "Gravity", "Gravity strength, in world units per second."));
            this.addRow(new GradientPanel(emitter.getTint(), "Tint", "", false));
            this.addRow(new PercentagePanel(emitter.getTransparency(), "Life", "Transparency", ""));
            this.addRow(new OptionsPanel(this, "Options", ""));
            Component[] var4 = this.rowsPanel.getComponents();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Component component = var4[var6];
                if (component instanceof EditorPanel) {
                    ((EditorPanel)component).update(this);
                }
            }

            this.rowsPanel.repaint();
        }

        void addEditorRow(JPanel row) {
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
            this.editRowsPanel.add(row, new GridBagConstraints(0, -1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        }

        void addRow(JPanel row) {
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
            this.rowsPanel.add(row, new GridBagConstraints(0, -1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        }

        public void setVisible(String name, boolean visible) {
            Component[] var3 = this.rowsPanel.getComponents();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Component component = var3[var5];
                if (component instanceof EditorPanel && ((EditorPanel)component).getName().equals(name)) {
                    component.setVisible(visible);
                }
            }

        }

        public ParticleEmitter getEmitter() {
            Array<ParticleEmitter> emitters = this.effect.getEmitters();
            return this.effectPanel.editIndex < emitters.size ? (ParticleEmitter)emitters.get(this.effectPanel.editIndex) : (ParticleEmitter)emitters.get(0);
        }

        public void setEnabled(ParticleEmitter emitter, boolean enabled) {
            ParticleData data = (ParticleData)this.particleData.get(emitter);
            if (data == null) {
                this.particleData.put(emitter, data = new ParticleData());
            }

            data.enabled = enabled;
            emitter.reset();
        }

        public boolean isEnabled(ParticleEmitter emitter) {
            ParticleData data = (ParticleData)this.particleData.get(emitter);
            return data == null ? true : data.enabled;
        }

        private void initializeComponents() {
            this.createCanvas();
            this.splitPane = new JSplitPane();
            this.splitPane.setUI(new BasicSplitPaneUI() {
                public void paint(Graphics g, JComponent jc) {
                }
            });
            this.splitPane.setDividerSize(4);
            this.getContentPane().add(this.splitPane, "Center");
            JSplitPane leftSplit = new JSplitPane(0);
            leftSplit.setUI(new BasicSplitPaneUI() {
                public void paint(Graphics g, JComponent jc) {
                }
            });
            leftSplit.setDividerSize(4);
            this.splitPane.add(leftSplit, "right");
            JPanel emittersPanel = new JPanel(new GridBagLayout());
            leftSplit.add(emittersPanel, "top");
            emittersPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Editor Properties")));
            JScrollPane scroll = new JScrollPane();
            emittersPanel.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
            scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            this.editRowsPanel = new JPanel(new GridBagLayout());
            scroll.setViewportView(this.editRowsPanel);
            scroll.getVerticalScrollBar().setUnitIncrement(70);
            emittersPanel = new JPanel(new GridBagLayout());
            leftSplit.add(emittersPanel, "bottom");
            emittersPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Emitter Properties")));
            scroll = new JScrollPane();
            emittersPanel.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
            scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            this.rowsPanel = new JPanel(new GridBagLayout());
            scroll.setViewportView(this.rowsPanel);
            scroll.getVerticalScrollBar().setUnitIncrement(70);
            leftSplit.setDividerLocation(200);
            leftSplit = new JSplitPane(0);
            leftSplit.setUI(new BasicSplitPaneUI() {
                public void paint(Graphics g, JComponent jc) {
                }
            });
            leftSplit.setDividerSize(4);
            this.splitPane.add(leftSplit, "left");
            emittersPanel = new JPanel(new BorderLayout());
            leftSplit.add(emittersPanel, "bottom");
            emittersPanel.add(this.lwjglCanvas);
            emittersPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            emittersPanel = new JPanel(new BorderLayout());
            leftSplit.add(emittersPanel, "top");
            emittersPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(0, 6, 6, 0), BorderFactory.createTitledBorder("Effect Emitters")));
            this.effectPanel = new EffectPanel(this);
            emittersPanel.add(this.effectPanel);
            leftSplit.setDividerLocation(575);
            this.splitPane.setDividerLocation(325);
        }

        public static void main(String[] args) {
            UIManager.LookAndFeelInfo[] var1 = UIManager.getInstalledLookAndFeels();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                UIManager.LookAndFeelInfo info = var1[var3];
                if ("Nimbus".equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (Throwable var6) {
                    }
                    break;
                }
            }

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new ParticleEditor();
                }
            });
        }

        static class ParticleData {
            public boolean enabled = true;

            ParticleData() {
            }
        }

        class Renderer implements ApplicationListener, InputProcessor {
            private float maxActiveTimer;
            private int maxActive;
            private int lastMaxActive;
            private boolean mouseDown;
            private int activeCount;
            private int mouseX;
            private int mouseY;
            private BitmapFont font;
            private SpriteBatch spriteBatch;
            private ShapeRenderer shapeRenderer;
            private com.badlogic.gdx.graphics.Color lineColor;
            public Sprite bgImage;
            public CustomShading customShading;

            Renderer() {
            }

            public void create() {
                if (this.spriteBatch == null) {
                    this.customShading = new CustomShading();
                    this.spriteBatch = new SpriteBatch();
                    this.shapeRenderer = new ShapeRenderer();
                    this.lineColor = com.badlogic.gdx.graphics.Color.valueOf("636363");
                    ParticleEditor.this.worldCamera = new OrthographicCamera();
                    ParticleEditor.this.textCamera = new OrthographicCamera();
                    ParticleEditor.this.pixelsPerMeter = new ParticleEmitter.NumericValue();
                    ParticleEditor.this.pixelsPerMeter.setValue(1.0F);
                    ParticleEditor.this.pixelsPerMeter.setAlwaysActive(true);
                    ParticleEditor.this.zoomLevel = new ParticleEmitter.NumericValue();
                    ParticleEditor.this.zoomLevel.setValue(1.0F);
                    ParticleEditor.this.zoomLevel.setAlwaysActive(true);
                    ParticleEditor.this.deltaMultiplier = new ParticleEmitter.NumericValue();
                    ParticleEditor.this.deltaMultiplier.setValue(1.0F);
                    ParticleEditor.this.deltaMultiplier.setAlwaysActive(true);
                    ParticleEditor.this.backgroundColor = new ParticleEmitter.GradientColorValue();
                    ParticleEditor.this.backgroundColor.setColors(new float[]{0.0F, 0.0F, 0.0F});
                    this.font = new BitmapFont(Gdx.files.getFileHandle("default.fnt", FileType.Internal), Gdx.files.getFileHandle("default.png", FileType.Internal), true);
                    ParticleEditor.this.effectPanel.newExampleEmitter("Untitled", true);
                    OrthoCamController orthoCamController = new OrthoCamController(ParticleEditor.this.worldCamera);
                    Gdx.input.setInputProcessor(new InputMultiplexer(new InputProcessor[]{orthoCamController, this}));
                    this.resize(ParticleEditor.this.lwjglCanvas.getWidth(), ParticleEditor.this.lwjglCanvas.getHeight());
                }
            }

            public void resize(int width, int height) {
                Gdx.gl.glViewport(0, 0, width, height);
                if (ParticleEditor.this.pixelsPerMeter.getValue() <= 0.0F) {
                    ParticleEditor.this.pixelsPerMeter.setValue(1.0F);
                }

                ParticleEditor.this.worldCamera.setToOrtho(false, (float)width / ParticleEditor.this.pixelsPerMeter.getValue(), (float)height / ParticleEditor.this.pixelsPerMeter.getValue());
                ParticleEditor.this.worldCamera.update();
                ParticleEditor.this.textCamera.setToOrtho(true, (float)width, (float)height);
                ParticleEditor.this.textCamera.update();
                ParticleEditor.this.effect.setPosition(ParticleEditor.this.worldCamera.viewportWidth / 2.0F, ParticleEditor.this.worldCamera.viewportHeight / 2.0F);
            }

            private void renderGrid(ShapeRenderer shapeRenderer, int minX, int maxX, int minY, int maxY) {
                shapeRenderer.begin(ShapeType.Line);
                shapeRenderer.setColor(this.lineColor);

                int i;
                for(i = minX; i <= maxX; ++i) {
                    shapeRenderer.line((float)i, (float)minY, (float)i, (float)maxY);
                }

                for(i = minY; i <= maxY; ++i) {
                    shapeRenderer.line((float)minX, (float)i, (float)maxX, (float)i);
                }

                shapeRenderer.end();
            }

            public void render() {
                int viewWidth = Gdx.graphics.getWidth();
                int viewHeight = Gdx.graphics.getHeight();
                float delta = Math.max(0.0F, Gdx.graphics.getDeltaTime() * ParticleEditor.this.deltaMultiplier.getValue());
                float[] colors = ParticleEditor.this.backgroundColor.getColors();
                Gdx.gl.glClearColor(colors[0], colors[1], colors[2], 1.0F);
                Gdx.gl.glClear(16384);
                ParticleEditor.this.previewImagePanel.updateSpritePosition();
                if (ParticleEditor.this.pixelsPerMeter.getValue() != ParticleEditor.this.pixelsPerMeterPrev || ParticleEditor.this.zoomLevel.getValue() != ParticleEditor.this.zoomLevelPrev) {
                    if (ParticleEditor.this.pixelsPerMeter.getValue() <= 0.0F) {
                        ParticleEditor.this.pixelsPerMeter.setValue(1.0F);
                    }

                    ParticleEditor.this.worldCamera.setToOrtho(false, (float)viewWidth / ParticleEditor.this.pixelsPerMeter.getValue(), (float)viewHeight / ParticleEditor.this.pixelsPerMeter.getValue());
                    ParticleEditor.this.worldCamera.zoom = ParticleEditor.this.zoomLevel.getValue();
                    ParticleEditor.this.worldCamera.update();
                    ParticleEditor.this.effect.setPosition(ParticleEditor.this.worldCamera.viewportWidth / 2.0F, ParticleEditor.this.worldCamera.viewportHeight / 2.0F);
                    ParticleEditor.this.zoomLevelPrev = ParticleEditor.this.zoomLevel.getValue();
                    ParticleEditor.this.pixelsPerMeterPrev = ParticleEditor.this.pixelsPerMeter.getValue();
                }

                this.spriteBatch.setProjectionMatrix(ParticleEditor.this.worldCamera.combined);
                this.shapeRenderer.setProjectionMatrix(ParticleEditor.this.worldCamera.combined);
                if (ParticleEditor.this.renderGridCheckBox.isSelected()) {
                    this.renderGrid(this.shapeRenderer, -40, 40, -40, 40);
                }

                this.shapeRenderer.begin(ShapeType.Line);
                this.shapeRenderer.line(-1000.0F, 0.0F, 1000.0F, 0.0F, com.badlogic.gdx.graphics.Color.GREEN, com.badlogic.gdx.graphics.Color.GREEN);
                this.shapeRenderer.line(0.0F, -1000.0F, 0.0F, 1000.0F, com.badlogic.gdx.graphics.Color.RED, com.badlogic.gdx.graphics.Color.RED);
                this.shapeRenderer.end();
                this.spriteBatch.begin();
                this.spriteBatch.enableBlending();
                this.spriteBatch.setBlendFunction(770, 771);
                if (this.bgImage != null) {
                    this.bgImage.draw(this.spriteBatch);
                }

                this.activeCount = 0;
                boolean complete = true;
                this.customShading.begin(this.spriteBatch);
                Array.ArrayIterator var6 = ParticleEditor.this.effect.getEmitters().iterator();

                while(var6.hasNext()) {
                    ParticleEmitter emitter = (ParticleEmitter)var6.next();
                    if (emitter.getSprites().size == 0 && emitter.getImagePaths().size > 0) {
                        this.loadImages(emitter);
                    }

                    boolean enabled = ParticleEditor.this.isEnabled(emitter);
                    if (enabled) {
                        if (emitter.getSprites().size > 0) {
                            emitter.draw(this.spriteBatch, delta);
                        }

                        this.activeCount += emitter.getActiveCount();
                        if (!emitter.isComplete()) {
                            complete = false;
                        }
                    }
                }

                this.customShading.end(this.spriteBatch);
                if (complete) {
                    ParticleEditor.this.effect.start();
                }

                this.maxActive = Math.max(this.maxActive, this.activeCount);
                this.maxActiveTimer += delta;
                if (this.maxActiveTimer > 3.0F) {
                    this.maxActiveTimer = 0.0F;
                    this.lastMaxActive = this.maxActive;
                    this.maxActive = 0;
                }

                if (this.mouseDown) {
                }

                this.spriteBatch.setProjectionMatrix(ParticleEditor.this.textCamera.combined);
                this.font.draw(this.spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 5.0F, 15.0F);
                this.font.draw(this.spriteBatch, "Count: " + this.activeCount, 5.0F, 35.0F);
                this.font.draw(this.spriteBatch, "Max: " + this.lastMaxActive, 5.0F, 55.0F);
                this.font.draw(this.spriteBatch, (int)(ParticleEditor.this.getEmitter().getPercentComplete() * 100.0F) + "%", 5.0F, 75.0F);
                this.spriteBatch.end();
            }

            private void loadImages(ParticleEmitter emitter) {
                String imagePath = null;

                try {
                    Array<Sprite> sprites = new Array();
                    Array<String> imagePaths = emitter.getImagePaths();

                    for(int i = 0; i < imagePaths.size; ++i) {
                        imagePath = (String)imagePaths.get(i);
                        String imageName = (new File(imagePath.replace('\\', '/'))).getName();
                        FileHandle file;
                        if (!imagePath.equals("particle.png") && !imagePath.equals("pre_particle.png")) {
                            if ((imagePath.contains("/") || imagePath.contains("\\")) && !imageName.contains("..")) {
                                file = Gdx.files.absolute(imagePath);
                                if (!file.exists()) {
                                    file = Gdx.files.absolute((new File(ParticleEditor.this.effectFile.getParentFile(), imageName)).getAbsolutePath());
                                }
                            } else {
                                file = Gdx.files.absolute((new File(ParticleEditor.this.effectFile.getParentFile(), imagePath)).getAbsolutePath());
                            }
                        } else {
                            file = Gdx.files.classpath(imagePath);
                        }

                        sprites.add(new Sprite(new Texture(file)));
                    }

                    emitter.setSprites(sprites);
                } catch (GdxRuntimeException var8) {
                    var8.printStackTrace();
                    final String imageRef = imagePath;
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(ParticleEditor.this, "Error loading image:\n" + imageRef);
                        }
                    });
                    emitter.getImagePaths().clear();
                }

            }

            public boolean keyDown(int keycode) {
                if (keycode == 62) {
                    ParticleEditor.this.effect.setPosition(ParticleEditor.this.previewImagePanel.valueX.getValue() + ParticleEditor.this.previewImagePanel.valueWidth.getValue() / 2.0F, ParticleEditor.this.previewImagePanel.valueY.getValue() + ParticleEditor.this.previewImagePanel.valueHeight.getValue() / 2.0F);
                }

                return false;
            }

            public boolean keyUp(int keycode) {
                return false;
            }

            public boolean keyTyped(char character) {
                return false;
            }

            public boolean touchDown(int x, int y, int pointer, int newParam) {
                if (Gdx.input.isButtonPressed(1)) {
                    Vector3 touchPoint = new Vector3((float)x, (float)y, 0.0F);
                    ParticleEditor.this.worldCamera.unproject(touchPoint);
                    ParticleEditor.this.effect.setPosition(touchPoint.x, touchPoint.y);
                }

                return false;
            }

            public boolean touchUp(int x, int y, int pointer, int button) {
                ParticleEditor.this.dispatchEvent(new WindowEvent(ParticleEditor.this, 208));
                ParticleEditor.this.dispatchEvent(new WindowEvent(ParticleEditor.this, 207));
                ParticleEditor.this.requestFocusInWindow();
                return false;
            }

            public boolean touchDragged(int x, int y, int pointer) {
                if (Gdx.input.isButtonPressed(1)) {
                    Vector3 touchPoint = new Vector3((float)x, (float)y, 0.0F);
                    ParticleEditor.this.worldCamera.unproject(touchPoint);
                    ParticleEditor.this.effect.setPosition(touchPoint.x, touchPoint.y);
                }

                return false;
            }

            public void dispose() {
            }

            public void pause() {
            }

            public void resume() {
            }

            public boolean mouseMoved(int x, int y) {
                return false;
            }

            public boolean scrolled(float amountX, float amountY) {
                return false;
            }

            public void setImageBackground(File file) {
                if (this.bgImage != null) {
                    this.bgImage.getTexture().dispose();
                    this.bgImage = null;
                }

                if (file != null) {
                    this.bgImage = new Sprite(new Texture(Gdx.files.absolute(file.getAbsolutePath())));
                }

            }

            public void updateImageBackgroundPosSize(float x, float y, float width, float height) {
                if (this.bgImage != null) {
                    this.bgImage.setPosition(x, y);
                    this.bgImage.setSize(width, height);
                }

            }

            private class OrthoCamController extends InputAdapter {
                final OrthographicCamera camera;
                final Vector3 curr = new Vector3();
                final Vector3 last = new Vector3(-1.0F, -1.0F, -1.0F);
                final Vector3 delta = new Vector3();
                boolean canDrag = false;

                public OrthoCamController(OrthographicCamera camera) {
                    this.camera = camera;
                }

                public boolean scrolled(float amountX, float amountY) {
                    OrthographicCamera var10000 = ParticleEditor.this.worldCamera;
                    var10000.zoom += amountY * 0.01F;
                    ParticleEditor.this.worldCamera.zoom = MathUtils.clamp(ParticleEditor.this.worldCamera.zoom, 0.01F, 5000.0F);
                    ParticleEditor.this.worldCamera.update();
                    return super.scrolled(amountX, amountY);
                }

                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    if (button == 0) {
                        this.canDrag = true;
                    } else {
                        this.canDrag = false;
                    }

                    return super.touchDown(screenX, screenY, pointer, button);
                }

                public boolean touchDragged(int x, int y, int pointer) {
                    if (!this.canDrag) {
                        return false;
                    } else {
                        this.camera.unproject(this.curr.set((float)x, (float)y, 0.0F));
                        if (this.last.x != -1.0F || this.last.y != -1.0F || this.last.z != -1.0F) {
                            this.camera.unproject(this.delta.set(this.last.x, this.last.y, 0.0F));
                            this.delta.sub(this.curr);
                            this.camera.position.add(this.delta.x, this.delta.y, 0.0F);
                        }

                        this.last.set((float)x, (float)y, 0.0F);
                        this.camera.update();
                        return false;
                    }
                }

                public boolean touchUp(int x, int y, int pointer, int button) {
                    this.last.set(-1.0F, -1.0F, -1.0F);
                    this.canDrag = false;
                    return false;
                }
            }
        }
    }


