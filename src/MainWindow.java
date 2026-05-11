import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
        setJMenuBar(buildMenuBar());
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(buildFileMenu());
        bar.add(buildEditMenu());
        bar.add(buildViewMenu());
        return bar;
    }
    private JMenu buildFileMenu() {
        JMenu menu = new JMenu("File");
        JMenuItem save = new JMenuItem("Save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        save.addActionListener(e -> saveFile());
        menu.add(save);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        menu.add(exit);
        return menu;
    }

    private JMenu buildEditMenu() {
        JMenu menu = new JMenu("Edit");
        JMenuItem undo = new JMenuItem("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> canvas.undo());
        menu.add(undo);
        return menu;
    }

    public JMenu buildViewMenu(){
        JMenu menu = new JMenu("View");
        return menu;
    }

    private void saveFile(){}

    private void openFile(){}

}
