import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private boolean confirmed = false;
    private final JTextField widthField;
    private final JTextField heightField;
    private final JSpinner   undoSpinner;
    private final JSpinner   brushSpinner;
    private final JComboBox<String> themeCombo;

    public SettingsDialog(JFrame parent, CanvasPanel canvas) {
        super(parent, "Settings", true);
        setResizable(false);
        widthField   = new JTextField(String.valueOf(canvas.getImage().getWidth()), 6);
        heightField  = new JTextField(String.valueOf(canvas.getImage().getHeight()), 6);
        undoSpinner  = new JSpinner(new SpinnerNumberModel(30, 1, 200, 1));
        brushSpinner = new JSpinner(new SpinnerNumberModel(canvas.getBrushSize(), 1, 60, 1));
        themeCombo   = new JComboBox<>(new String[]{"Light", "Dark"});
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        int row = 0;
        form.add(new JLabel("Canvas size"), lc(lc, row));
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        sizePanel.add(widthField);
        sizePanel.add(new JLabel("×"));
        sizePanel.add(heightField);
        form.add(sizePanel, lc(fc, row++));
        form.add(new JLabel("Max undo steps"),     lc(lc, row));
        form.add(undoSpinner,                       lc(fc, row++));
        form.add(new JLabel("Default brush size"), lc(lc, row));
        form.add(brushSpinner,                      lc(fc, row++));
        form.add(new JLabel("Theme"),               lc(lc, row));
        form.add(themeCombo,                         lc(fc, row));

        JButton ok     = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e     -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        add(form,    BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isConfirmed()     {
        return confirmed;
    }

    public int getCanvasWidth(){
        return parsePositiveInt(widthField.getText(),800);
    }

    public int getCanvasHeight(){
        return parsePositiveInt(heightField.getText(),600);
    }

    public int getMaxUndoSteps(){
        return (Integer) undoSpinner.getValue();
    }

    public int getDefaultBrushSize(){
        return (Integer) brushSpinner.getValue();
    }

    public String getTheme(){
        return (String) themeCombo.getSelectedItem();
    }

    private int parsePositiveInt(String s, int fallback) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 0, 12);
        c.gridx  = 0;
        return c;
    }

    private GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 0, 0);
        c.gridx  = 1;
        c.weightx = 1.0;
        return c;
    }

    private GridBagConstraints lc(GridBagConstraints base, int row) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row;
        return c;
    }
}