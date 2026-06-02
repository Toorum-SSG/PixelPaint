import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StatusBar extends JPanel {

    private final JLabel positionLabel;
    private final JLabel canvasLabel;
    private final JLabel zoomLabel;
    private final JLabel undoLabel;
    private final JLabel redoLabel;

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 16, 2));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        setBackground(new Color(235, 235, 235));
        setPreferredSize(new Dimension(0, 24));
        positionLabel = label("X: 0    Y: 0");
        canvasLabel = label("Canvas: —");
        zoomLabel = label("Zoom: 100%");
        undoLabel = label("Undo: \u2716");   // ✖  updated live
        redoLabel = label("Redo: \u2716");
        add(positionLabel);
        add(sep());
        add(canvasLabel);
        add(sep());
        add(zoomLabel);
        add(sep());
        add(undoLabel);
        add(sep());
        add(redoLabel);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return l;
    }

    private JSeparator sep() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 14));
        return sep;
    }

    //sets current X and Y positions stored
    private void updatePosition(MouseEvent e, CanvasPanel canvas) {
        int x = (int)(e.getX() / canvas.getZoomFactor());
        int y = (int)(e.getY() / canvas.getZoomFactor());
        x = Math.max(0, Math.min(x, canvas.getImage().getWidth()  - 1));
        y = Math.max(0, Math.min(y, canvas.getImage().getHeight() - 1));
        positionLabel.setText("X: " + x + "  Y: " + y);
    }

    public void setPosition(int x, int y) {
        positionLabel.setText("X: " + x + "  Y: " + y);
    }

    public void setCanvasSize(int w, int h) {
        canvasLabel.setText("Canvas: " + w + "×" + h);
    }

    public void setZoom(double factor) {
        zoomLabel.setText(String.format("Zoom: %.0f%%", factor * 100));
    }

    //tracks mouse for its X and Y positions
    public void attachMouseTracking(CanvasPanel canvas) {

        MouseMotionAdapter adapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updatePosition(e, canvas);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePosition(e, canvas);
            }
        };
        canvas.addMouseMotionListener(adapter);
    }

    //updates the status of status bar parameters
    public void update(CanvasPanel canvas) {
        canvasLabel.setText("Canvas: " + canvas.getImage().getWidth() + "\u00d7" + canvas.getImage().getHeight());
        zoomLabel.setText(String.format("Zoom: %.0f%%", canvas.getZoomFactor() * 100));
        undoLabel.setText("Undo: " + (canvas.hasUndo() ? "\u2714" : "\u2716"));
        redoLabel.setText("Redo: " + (canvas.hasRedo() ? "\u2714" : "\u2716"));
    }
}