import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class CanvasPanel extends JPanel{
    private BufferedImage image;
    private Graphics2D g2d;

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
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(lastMousePos[0], lastMousePos[1], e.getX(), e.getY());
                lastMousePos[0] = e.getX();
                lastMousePos[1] = e.getY();
                repaint();
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


}
