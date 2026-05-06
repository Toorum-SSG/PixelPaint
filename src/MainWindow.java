import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame{
    private CanvasPanel canvas;

    public MainWindow() {
        setTitle("PixelPaint");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        canvas = new CanvasPanel(800,600);
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.getViewport().setBackground(new Color(180, 180, 180));
        add(scrollPane, BorderLayout.CENTER);
    }

}
