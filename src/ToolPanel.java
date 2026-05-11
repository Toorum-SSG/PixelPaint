import javax.swing.*;

public class ToolPanel extends JPanel {
    private CanvasPanel canvas;

    public ToolPanel(CanvasPanel canvas) {
        this.canvas = canvas;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(160, 0));
    }

    private JButton toolBtn(String label, Tool tool) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> canvas.setCurrentTool(tool));
        return btn;
    }
}
