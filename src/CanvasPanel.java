import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

public class CanvasPanel extends JPanel{
    private BufferedImage image;
    private BufferedImage scratch;
    private Graphics2D g2d;
    private Tool currentTool = Tool.BRUSH;
    private Color primaryColor = Color.BLACK;
    private Color secondaryColor = Color.WHITE;
    private int brushSize = 10;
    private float opacity = 1.0f;
    private int startX;
    private int startY;
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();

    private void restoreImage(BufferedImage saved) {
        g2d.drawImage(saved, 0, 0, null);
    }

    public void setCurrentTool(Tool t) {
        currentTool = t;
    }

    public void setPrimaryColor(Color c) {
        primaryColor = c;
    }

    public void setBrushSize(int s) {
        brushSize = s;
    }

    public void setOpacity(float o) {
        opacity = o;
    }

    private void applySettings(){
        g2d.setColor(primaryColor);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    public CanvasPanel(int width, int height){
        setPreferredSize(new Dimension(width, height));
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        int[]lastMousePos = {0,0};
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e){
                lastMousePos[0] = e.getX();
                lastMousePos[1] = e.getY();
                g2d.setColor(Color.BLACK);
                g2d.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);
                repaint();
                startX = e.getX();
                startY = e.getY();
                if (currentTool == Tool.LINE   || currentTool == Tool.RECTANGLE || currentTool == Tool.CIRCLE) {
                    scratch = copyImage(image);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(lastMousePos[0], lastMousePos[1], e.getX(), e.getY());
                lastMousePos[0] = e.getX();
                lastMousePos[1] = e.getY();
                repaint();
                switch (currentTool) {
                    case LINE:
                    case RECTANGLE:
                    case CIRCLE:
                        restoreImage(scratch);
                        drawShape(startX, startY, e.getX(), e.getY());
                        break;
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        graphics.drawImage(image, 0, 0, null);
    }

    private void drawShape(int x1, int y1, int x2, int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        switch (currentTool) {
            case LINE:
                g2d.drawLine(x1,y1,x2,y2);
                break;
            case RECTANGLE:
                g2d.drawRect(x,y,w,h);
                break;
            case CIRCLE:
                g2d.drawOval(x,y,w,h);
                break;
        }
    }

    private void saveSnapshot() {
        undoStack.push(copyImage(image));
        redoStack.clear();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.push(copyImage(image));
        restoreImage(redoStack.pop());
        repaint();
    }

    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        copy.createGraphics().drawImage(src, 0, 0, null);
        return copy;
    }
}
