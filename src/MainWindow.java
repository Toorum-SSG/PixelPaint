import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame{
    private CanvasPanel canvas;

    public MainWindow() {
        setTitle("PixelPaint");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setJMenuBar(buildMenuBar());
        canvas = new CanvasPanel(800,600);
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.getViewport().setBackground(new Color(180, 180, 180));
        add(scrollPane, BorderLayout.CENTER);
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

    private void saveFile(){
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Ensure .png extension
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getPath() + ".png");
            }

            try {
                ImageIO.write(canvas.getImage(), "PNG", file);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this)
                == JFileChooser.APPROVE_OPTION) {
            try {
                java.awt.image.BufferedImage img =
                        ImageIO.read(chooser.getSelectedFile());
                canvas.getImage().createGraphics()
                        .drawImage(img, 0, 0, null);
                canvas.repaint();
            } catch (IOException ex) { /* show error */ }
        }
    }


}
