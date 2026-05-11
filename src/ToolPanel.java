import javax.swing.*;
import java.awt.*;

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

    private JPanel buildToolButtons(){
        JPanel p = new JPanel(new GridLayout(3, 2, 2, 2));
        p.add(toolBtn("Brush", Tool.BRUSH));
        p.add(toolBtn("Eraser", Tool.ERASER));
        p.add(toolBtn("Line", Tool.LINE));
        return p;
    }
}
