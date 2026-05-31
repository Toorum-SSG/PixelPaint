import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class CanvasPanel extends JPanel {
    private BufferedImage image;
    private Graphics2D g2d;
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private int maxUndoSteps = 30;
    private Tool  currentTool = Tool.BRUSH;
    private Color primaryColor = Color.BLACK;
    private Color secondaryColor = Color.WHITE;
    private int   brushSize = 10;
    private float opacity = 1.0f;
    private double zoomFactor = 1.0;
    private static final double ZOOM_MIN  = 0.1;
    private static final double ZOOM_MAX  = 16.0;
    private static final double ZOOM_STEP = 0.12;
    private JScrollPane scrollPane;
    private StatusBar statusBar;
    private int lastX, lastY;
    private int startX, startY;
    private BufferedImage shapeScratch;
    private boolean panActive = false;
    private int panAnchorX, panAnchorY;
    private int panScrollX, panScrollY;
    public CanvasPanel(int width, int height) {
        setBackground(Color.LIGHT_GRAY);
        setPreferredSize(new Dimension(width, height));
        initImage(width, height);
        addDrawingListeners();
        addZoomListener();
    }

    public void setScrollPane(JScrollPane sp) {
        this.scrollPane = sp;
    }

    public void setStatusBar(StatusBar sb) {
        this.statusBar = sb;
    }

    private void notifyStatusBar() {
        if (statusBar != null)
            statusBar.update(this);
    }

    private void initImage(int w, int h) {
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        g2d = makeG2D(image);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
    }

    private Graphics2D makeG2D(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        return g;
    }

    private void addZoomListener() {
        addMouseWheelListener(e -> {
            if (!e.isControlDown())
                return;
            int mouseX = e.getX();
            int mouseY = e.getY();
            double imgX = mouseX / zoomFactor;
            double imgY = mouseY / zoomFactor;
            double delta = -e.getPreciseWheelRotation() * ZOOM_STEP;
            zoomFactor = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoomFactor + delta));

            int newW = (int)(image.getWidth()  * zoomFactor);
            int newH = (int)(image.getHeight() * zoomFactor);
            setPreferredSize(new Dimension(newW, newH));
            revalidate();
            if (scrollPane != null) {
                int scrollX = (int)(imgX * zoomFactor) - mouseX;
                int scrollY = (int)(imgY * zoomFactor) - mouseY;
                scrollX = Math.max(0, Math.min(newW, scrollX));
                scrollY = Math.max(0, Math.min(newH, scrollY));
                scrollPane.getHorizontalScrollBar().setValue(scrollX);
                scrollPane.getVerticalScrollBar().setValue(scrollY);
            }
            repaint();
            notifyStatusBar();
        });
    }

    private void addDrawingListeners() {
        MouseAdapter adapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    panActive  = true;
                    panAnchorX = e.getXOnScreen();
                    panAnchorY = e.getYOnScreen();
                    if (scrollPane != null) {
                        panScrollX = scrollPane.getHorizontalScrollBar().getValue();
                        panScrollY = scrollPane.getVerticalScrollBar().getValue();
                    }
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                int x = toCanvas(e.getX());
                int y = toCanvas(e.getY());
                lastX = x;
                lastY = y;
                startX = x;
                startY = y;
                if (currentTool == Tool.LINE || currentTool == Tool.CIRCLE || currentTool == Tool.RECTANGLE) {
                    shapeScratch = copyImage(image);
                } else if (currentTool == Tool.FILL) {
                    Color fc = SwingUtilities.isRightMouseButton(e) ? secondaryColor : primaryColor;
                    saveSnapshot();
                    floodFill(x, y, fc);
                } else {
                    saveSnapshot();
                    drawDot(x, y, e);
                    paintImmediately(0, 0, getWidth(), getHeight());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panActive) {
                    if (scrollPane != null) {
                        int dx = e.getXOnScreen() - panAnchorX;
                        int dy = e.getYOnScreen() - panAnchorY;
                        scrollPane.getHorizontalScrollBar().setValue(panScrollX - dx);
                        scrollPane.getVerticalScrollBar().setValue(panScrollY - dy);
                    }
                    return;
                }
                int x = toCanvas(e.getX());
                int y = toCanvas(e.getY());
                switch (currentTool) {
                    case BRUSH:
                    case ERASER:
                        drawStroke(lastX, lastY, x, y, e);
                        lastX = x;
                        lastY = y;
                        paintImmediately(0, 0, getWidth(), getHeight());
                        break;
                    case LINE:
                    case RECTANGLE:
                    case CIRCLE:
                        restoreImage(shapeScratch);
                        drawShape(startX, startY, x, y, e);
                        repaint();
                        break;
                    case FILL:
                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panActive && SwingUtilities.isMiddleMouseButton(e)) {
                    panActive = false;
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }

                if (currentTool == Tool.LINE || currentTool == Tool.CIRCLE || currentTool == Tool.RECTANGLE) {
                    int x = toCanvas(e.getX());
                    int y = toCanvas(e.getY());
                    saveSnapshot();
                    restoreImage(shapeScratch);
                    drawShape(startX, startY, x, y, e);
                    shapeScratch = null;
                    repaint();
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void drawDot(int x, int y, MouseEvent e) {
        configureG2D(e);
        g2d.fillOval(x - brushSize / 2, y - brushSize / 2, brushSize, brushSize);
    }

    private void drawStroke(int x1, int y1, int x2, int y2, MouseEvent e) {
        configureG2D(e);
        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    private void drawShape(int x1, int y1, int x2, int y2, MouseEvent e) {
        configureG2D(e);
        g2d.setStroke(new BasicStroke(brushSize));
        int x = Math.min(x1, x2),  y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1), h = Math.abs(y2 - y1);
        switch (currentTool) {
            case LINE:
                g2d.drawLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                g2d.drawRect(x, y, w, h);
                break;
            case CIRCLE:
                g2d.drawOval(x, y, w, h);
                break;
        }
    }

    private void configureG2D(MouseEvent e) {
        if (currentTool == Tool.ERASER) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setColor(SwingUtilities.isRightMouseButton(e) ? secondaryColor : primaryColor);
        }
    }

    private void floodFill(int sx, int sy, Color fillColor) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (sx < 0 || sy < 0 || sx >= w || sy >= h) return;
        int targetRGB = image.getRGB(sx, sy);
        int fillRGB   = fillColor.getRGB();
        if (targetRGB == fillRGB) return;
        boolean[][] visited = new boolean[h][w];
        Queue<Integer> queue = new LinkedList<>();
        visited[sy][sx] = true;
        queue.add((sy << 16) | sx);
        int[] dx = { 1, -1, 0,  0 };
        int[] dy = { 0,  0, 1, -1 };
        while (!queue.isEmpty()) {
            int packed = queue.poll();
            int cy = packed >> 16;
            int cx = packed & 0xFFFF;
            image.setRGB(cx, cy, fillRGB);
            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (visited[ny][nx]) continue;
                if (image.getRGB(nx, ny) != targetRGB) continue;
                visited[ny][nx] = true;
                queue.add((ny << 16) | nx);
            }
        }
        repaint();
    }

    private void saveSnapshot() {
        undoStack.push(copyImage(image));
        redoStack.clear();
        while (undoStack.size() > maxUndoSteps)
            ((ArrayDeque<BufferedImage>) undoStack).removeLast();
        notifyStatusBar();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.push(copyImage(image));
        restoreImage(undoStack.pop());
        repaint();
        notifyStatusBar();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.push(copyImage(image));
        restoreImage(redoStack.pop());
        repaint();
        notifyStatusBar();
    }

    private void restoreImage(BufferedImage saved) {
        Composite prev = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1f));
        g2d.drawImage(saved, 0, 0, null);
        g2d.setComposite(prev);
    }

    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D cg = copy.createGraphics();
        cg.drawImage(src, 0, 0, null);
        cg.dispose();
        return copy;
    }

    public void clearCanvas() {
        saveSnapshot();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
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
        notifyStatusBar();
    }

    public void zoomIn() {
        zoomFactor = Math.min(zoomFactor + 0.25, ZOOM_MAX);
        applyZoom();
    }

    public void resetZoom() {
        zoomFactor = 1.0;
        applyZoom();
    }

    private void applyZoom() {
        setPreferredSize(new Dimension((int)(image.getWidth()  * zoomFactor), (int)(image.getHeight() * zoomFactor)));
        revalidate();
        repaint();
        notifyStatusBar();
    }

    private int toCanvas(int screenCoord) {
        return (int) (screenCoord / zoomFactor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(image, 0, 0, (int)(image.getWidth()  * zoomFactor), (int)(image.getHeight() * zoomFactor),null);
    }

    public boolean hasUndo() {
        return !undoStack.isEmpty();
    }

    public boolean hasRedo() {
        return !redoStack.isEmpty();
    }

    public BufferedImage getImage() {
        return image;
    }

    public double  getZoomFactor() {
        return zoomFactor;
    }

    public Color   getPrimaryColor() {
        return primaryColor;
    }

    public Color   getSecondaryColor() {
        return secondaryColor;
    }

    public void    setPrimaryColor(Color c) {
        primaryColor = c;
    }

    public void setSecondaryColor(Color c) {
        secondaryColor = c;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int s) {
        brushSize = s;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float o) {
        opacity = o;
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(Tool t) {
        currentTool = t;
    }
    public void setMaxUndoSteps(int s) {
        maxUndoSteps = s;
    }
}