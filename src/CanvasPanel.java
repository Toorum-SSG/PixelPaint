import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

public class CanvasPanel extends JPanel {
    private BufferedImage image;          // current drawing surface
    private Graphics2D    g2d;            // graphics context for the image
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private int maxUndoSteps = 30;
    private Tool    currentTool     = Tool.BRUSH;
    private Color   primaryColor    = Color.BLACK;
    private Color   secondaryColor  = Color.WHITE;
    private int     brushSize       = 10;
    private float   opacity         = 1.0f;
    private boolean fill            = false;
    private double zoomFactor = 1.0;
    private int lastX, lastY;
    private int startX, startY;
    private BufferedImage shapeScratch;

    public CanvasPanel(int width, int height) {
        setBackground(Color.LIGHT_GRAY);
        setPreferredSize(new Dimension(width, height));
        initImage(width, height);
        addMouseListeners();
    }

    private void initImage(int w, int h) {
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2d   = createG2D(image);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
    }

    private Graphics2D createG2D(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    private void addMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int x = screenToCanvas(e.getX());
                int y = screenToCanvas(e.getY());
                lastX = x; lastY = y;
                startX = x; startY = y;

                // For shapes/line: save snapshot so we can redraw cleanly while dragging
                if (currentTool == Tool.LINE || currentTool == Tool.CIRCLE || currentTool == Tool.RECTANGLE) {
                    shapeScratch = copyImage(image);
                } else {
                    saveUndoSnapshot();
                    drawPoint(x, y, e);
                }
                repaint();
            }

            @Override public void mouseDragged(MouseEvent e) {
                int x = screenToCanvas(e.getX());
                int y = screenToCanvas(e.getY());

                switch (currentTool) {
                    case BRUSH:
                    case ERASER:
                        drawLine(lastX, lastY, x, y, e);
                        lastX = x; lastY = y;
                        break;
                    case LINE:
                    case CIRCLE:
                    case RECTANGLE:
                        // Restore pre-drag snapshot, then draw preview
                        restoreImage(shapeScratch);
                        drawShape(startX, startY, x, y, e);
                        break;
                }
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                int x = screenToCanvas(e.getX());
                int y = screenToCanvas(e.getY());

                if (currentTool == Tool.LINE || currentTool == Tool.CIRCLE || currentTool == Tool.RECTANGLE) {
                    saveUndoSnapshot();           // save BEFORE final draw
                    restoreImage(shapeScratch);   // roll back preview
                    drawShape(startX, startY, x, y, e);
                    repaint();
                }
                shapeScratch = null;
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void drawPoint(int x, int y, MouseEvent e) {
        setupG2D(e);
        g2d.fillOval(x - brushSize / 2, y - brushSize / 2, brushSize, brushSize);
    }

    private void drawLine(int x1, int y1, int x2, int y2, MouseEvent e) {
        setupG2D(e);
        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    private void drawShape(int x1, int y1, int x2, int y2, MouseEvent e) {
        setupG2D(e);
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        g2d.setStroke(new BasicStroke(brushSize));

        switch (currentTool) {
            case LINE:
                g2d.drawLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                if (fill) g2d.fillRect(x, y, w, h);
                else      g2d.drawRect(x, y, w, h);
                break;
            case CIRCLE:
                if (fill) g2d.fillOval(x, y, w, h);
                else      g2d.drawOval(x, y, w, h);
                break;
        }
    }

    private void setupG2D(MouseEvent e) {
        boolean rightButton = SwingUtilities.isRightMouseButton(e);
        Color color;

        if (currentTool == Tool.ERASER) {
            color = Color.WHITE;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            color = rightButton ? secondaryColor : primaryColor;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }
        g2d.setColor(color);
    }

    private void saveUndoSnapshot() {
        undoStack.push(copyImage(image));
        redoStack.clear();
        // Trim history
        while (undoStack.size() > maxUndoSteps) {
            ((ArrayDeque<BufferedImage>) undoStack).removeLast();
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(copyImage(image));
            restoreImage(undoStack.pop());
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(copyImage(image));
            restoreImage(redoStack.pop());
            repaint();
        }
    }

    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D cg = copy.createGraphics();
        cg.drawImage(src, 0, 0, null);
        cg.dispose();
        return copy;
    }

    private void restoreImage(BufferedImage saved) {
        g2d.drawImage(saved, 0, 0, null);
    }

    public void clearCanvas() {
        saveUndoSnapshot();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        repaint();
    }

    public void resizeCanvas(int w, int h) {
        undoStack.clear();
        redoStack.clear();
        initImage(w, h);
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }

    public void zoomIn()    {
        zoomFactor = Math.min(zoomFactor + 0.25, 8.0); updateZoom();
    }

    public void resetZoom() {
        zoomFactor = 1.0; updateZoom();
    }

    private void updateZoom() {
        int w = (int)(image.getWidth()  * zoomFactor);
        int h = (int)(image.getHeight() * zoomFactor);
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }

    private int screenToCanvas(int screenCoord) {
        return (int)(screenCoord / zoomFactor);
    }

    public double getZoomFactor() { return zoomFactor; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = (int)(image.getWidth()  * zoomFactor);
        int h = (int)(image.getHeight() * zoomFactor);
        g.drawImage(image, 0, 0, w, h, null);
    }

    public BufferedImage getImage(){
        return image;
    }
    public Color getPrimaryColor(){
        return primaryColor;
    }

    public Color getSecondaryColor(){
        return secondaryColor;
    }

    public void setPrimaryColor(Color c){
        primaryColor = c;
    }

    public void setSecondaryColor(Color c){
        secondaryColor = c;
    }

    public int getBrushSize(){
        return brushSize;
    }

    public void setBrushSize(int s){
        brushSize = s;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float o){
        opacity = o;
    }

    public boolean isFill(){
        return fill;
    }

    public void setFill(boolean f){
        fill = f;
    }

    public Tool getCurrentTool(){
        return currentTool;
    }

    public void setCurrentTool(Tool t) {
        currentTool = t;
    }

    public void setMaxUndoSteps(int s){
        maxUndoSteps = s;
    }

}