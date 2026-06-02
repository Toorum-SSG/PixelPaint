import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToolPanel extends JPanel {
    private final CanvasPanel canvas;
    private static final Color[] PALETTE = {Color.BLACK,new Color(128,128,128), new Color(139,0,0), new Color(255,140,0), Color.YELLOW, new Color(0,128,0), Color.CYAN, Color.BLUE, new Color(128,0,128), new Color(255,105,180), new Color(139,69,19), new Color(192,192,192), Color.RED, Color.GREEN, new Color(0,191,255), new Color(75,0,130), new Color(154,205,50), new Color(255,228,196), new Color(216,191,216), new Color(0,100,0)};
    private JLabel  primarySwatch;
    private JLabel  secondarySwatch;
    private JLabel  sizeValueLabel;
    private JLabel  opacityValueLabel;

    public ToolPanel(CanvasPanel canvas) {
        this.canvas = canvas;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
        setBackground(new Color(240, 240, 240));
        setPreferredSize(new Dimension(160, 0));

        add(buildToolButtons());
        add(Box.createVerticalStrut(4));
        add(buildPalette());
        add(Box.createVerticalStrut(4));
        add(buildColorDisplay());
        add(Box.createVerticalStrut(8));
        add(buildSizeSlider());
        add(Box.createVerticalStrut(4));
        add(buildOpacitySlider());
        add(Box.createVerticalStrut(4));
        add(Box.createVerticalGlue());
    }


    private JButton toolButton(String tooltip, Tool tool, String label) {
        JButton btn = new JButton(label);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.addActionListener(e -> canvas.setCurrentTool(tool));
        return btn;
    }

    //creates tool chooser buttons
    private JPanel buildToolButtons() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Tools"));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,160));
        panel.add(toolButton("Brush",Tool.BRUSH,"🖌"));
        panel.add(toolButton("Eraser",Tool.ERASER,"🧽"));
        panel.add(toolButton("Circle",Tool.CIRCLE,"○"));
        panel.add(toolButton("Rect",Tool.RECTANGLE,"□"));
        panel.add(toolButton("Line",Tool.LINE,"—"));
        panel.add(toolButton("Fill", Tool.FILL,"🧺"));
        JButton clearBtn = new JButton("🗑");
        clearBtn.setToolTipText("Clear canvas");
        clearBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        clearBtn.setMargin(new Insets(2, 2, 2, 2));
        clearBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(canvas, "Clear the entire canvas?", "Clear", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) canvas.clearCanvas();
        });
        panel.add(clearBtn);
        return panel;
    }

    //creates slider for brush size
    private JPanel buildSizeSlider(){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        p.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        sizeValueLabel = new JLabel("Size: " + canvas.getBrushSize());
        sizeValueLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sizeValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        JSlider slider = new JSlider(1, 60, canvas.getBrushSize());
        slider.setOpaque(false);
        slider.setAlignmentX(LEFT_ALIGNMENT);
        slider.addChangeListener(e -> {
            canvas.setBrushSize(slider.getValue());
            sizeValueLabel.setText("Size: " + slider.getValue());
        });
        p.add(sizeValueLabel);
        p.add(slider);
        return p;
    }

    //creates quick color chooser
    private JPanel buildPalette() {
        JPanel panel = new JPanel(new GridLayout(5, 4, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Colour"));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        for (Color c : PALETTE) {
            JLabel swatch = new JLabel();
            swatch.setBackground(c);
            swatch.setOpaque(true);
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            swatch.setPreferredSize(new Dimension(24, 18));
            swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            swatch.setToolTipText(colorHex(c));
            swatch.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        canvas.setSecondaryColor(c);
                    } else {
                        canvas.setPrimaryColor(c);
                    }
                    refreshColorDisplay();
                }
            });
            panel.add(swatch);
        }
        return panel;
    }

    //shows current selected colors
    private JPanel buildColorDisplay() {
        JPanel outer = new JPanel(null);
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        outer.setPreferredSize(new Dimension(0, 64));
        secondarySwatch = colorSwatch(canvas.getSecondaryColor(), false);
        secondarySwatch.setBounds(28, 22, 32, 32);
        outer.add(secondarySwatch);
        primarySwatch = colorSwatch(canvas.getPrimaryColor(), true);
        primarySwatch.setBounds(8, 8, 32, 32);
        outer.add(primarySwatch);
        JLabel lbl = new JLabel("/* Primary & secondary colors */");
        lbl.setFont(new Font("Monospaced", Font.ITALIC, 9));
        lbl.setForeground(Color.GRAY);
        lbl.setBounds(4, 54, 150, 12);
        outer.add(lbl);
        return outer;
    }

    //renders color chooser
    private JLabel colorSwatch(Color c, boolean primary) {
        JLabel lbl = new JLabel();
        lbl.setBackground(c);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Color chosen = JColorChooser.showDialog(canvas,
                        primary ? "Choose primary colour" : "Choose secondary colour",
                        primary ? canvas.getPrimaryColor() : canvas.getSecondaryColor());
                if (chosen != null) {
                    if (primary) canvas.setPrimaryColor(chosen);
                    else         canvas.setSecondaryColor(chosen);
                    refreshColorDisplay();
                }
            }
        });
        return lbl;
    }

    public void refreshColorDisplay() {
        primarySwatch.setBackground(canvas.getPrimaryColor());
        secondarySwatch.setBackground(canvas.getSecondaryColor());
    }

    //Builds the slider for brush opacity
    private JPanel buildOpacitySlider() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        int initPct = Math.round(canvas.getOpacity() * 100);
        opacityValueLabel = new JLabel("Opacity: " + initPct);
        opacityValueLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        opacityValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        JSlider slider = new JSlider(1, 100, initPct);
        slider.setOpaque(false);
        slider.setAlignmentX(LEFT_ALIGNMENT);
        slider.addChangeListener((ChangeEvent e) -> {
            float opacity = slider.getValue() / 100.0f;
            canvas.setOpacity(opacity);
            opacityValueLabel.setText("Opacity: " + slider.getValue());
        });
        panel.add(opacityValueLabel);
        panel.add(slider);
        return panel;
    }

    private static String colorHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
