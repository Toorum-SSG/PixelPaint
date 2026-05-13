import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StatusBar extends JPanel {

    private final JLabel positionLabel;
    private final JLabel canvasLabel;
    private final JLabel zoomLabel;
    private final JLabel undoLabel;

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 16, 2));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        setBackground(new Color(235, 235, 235));
        setPreferredSize(new Dimension(0, 24));

        positionLabel = label("X: 0000  Y: 0000");
        canvasLabel = label("Canvas: 800×600");
        zoomLabel = label("Zoom: 100%");
        undoLabel = label("Undo: ✓/—    Redo: ✓/—");

        add(positionLabel);
        add(separator());
        add(canvasLabel);
        add(separator());
        add(zoomLabel);
        add(separator());
        add(undoLabel);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return l;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 16));
        return sep;
    }

    public void setPosition(int x, int y)     { positionLabel.setText("X: " + x + "  Y: " + y); }
    public void setCanvasSize(int w, int h)    { canvasLabel.setText("Canvas: " + w + "×" + h); }
    public void setZoom(double factor)         { zoomLabel.setText(String.format("Zoom: %.0f%%", factor * 100)); }
    public void attachMouseTracking(CanvasPanel canvas) {
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int x = (int)(e.getX() / canvas.getZoomFactor());
                int y = (int)(e.getY() / canvas.getZoomFactor());
                setPosition(x, y);
            }
            @Override public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });
    }
}