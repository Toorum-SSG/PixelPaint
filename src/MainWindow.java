import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame{
    private CanvasPanel canvas;
    private ToolPanel toolPanel;
    private StatusBar statusBar;
    private File currentFile = null;
    private boolean modified = false;


    public MainWindow() {
        setTitle("PixelPaint");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setJMenuBar(buildMenuBar());

        canvas = new CanvasPanel(800,600);
        toolPanel = new ToolPanel(canvas);
        statusBar = new StatusBar();

        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.getViewport().setBackground(new Color(180, 180, 180));
        statusBar.attachMouseTracking(canvas);
        statusBar.setCanvasSize(800, 600);
        registerKeyboardShortcuts();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });

        add(toolPanel,  BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar,  BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(buildFileMenu());
        bar.add(buildEditMenu());
        bar.add(buildViewMenu());
        return bar;
    }

    private JMenuItem item(String text, int mnemonic, KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(text, mnemonic);
        if (accelerator != null) item.setAccelerator(accelerator);
        return item;
    }

    private JMenu buildFileMenu() {
            JMenu menu = new JMenu("File");
            menu.setMnemonic(KeyEvent.VK_F);

            JMenuItem newCanvas = item("New Canvas", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
            newCanvas.addActionListener(e -> newCanvas());
            JMenuItem open = item("Open...", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
            open.addActionListener(e -> openFile());
            JMenuItem save = item("Save", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
            save.addActionListener(e -> saveFile(false));
            JMenuItem saveAs = item("Save as...", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            saveAs.addActionListener(e -> saveFile(true));
            JMenuItem exit = item("Exit", KeyEvent.VK_X, null);
            exit.addActionListener(e -> confirmAndExit());

            menu.add(newCanvas);
            menu.add(open);
            menu.add(save);
            menu.add(saveAs);
            menu.addSeparator();
            menu.add(exit);
            return menu;
    }

    private JMenu buildEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        JMenuItem undo = item("Undo", KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> canvas.undo());
        JMenuItem redo = item("Redo", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redo.addActionListener(e -> canvas.redo());
        JMenuItem clear = item("Clear Canvas", KeyEvent.VK_C, null);
        clear.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Clear the entire canvas?", "Clear", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) canvas.clearCanvas();
        });
        JMenuItem settings = item("Settings...", KeyEvent.VK_S, null);
        settings.addActionListener(e -> openSettings());

        menu.add(undo);
        menu.add(redo);
        menu.addSeparator();
        menu.add(clear);
        menu.addSeparator();
        menu.add(settings);
        return menu;
    }

    public JMenu buildViewMenu(){
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);

        JMenuItem zoomIn = item("Zoom in", KeyEvent.VK_I, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        zoomIn.addActionListener(e -> {
            canvas.zoomIn();
            statusBar.setZoom(canvas.getZoomFactor());
        });

        JMenuItem resetZoom = item("Reset Zoom", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        resetZoom.addActionListener(e -> {
            canvas.resetZoom();
            statusBar.setZoom(canvas.getZoomFactor());
        });

        menu.add(zoomIn);
        menu.add(resetZoom);
        return menu;
    }

    private void newCanvas() {
        if (!confirmDiscardChanges()) return;
        canvas.clearCanvas();
        currentFile = null;
        modified    = false;
        updateTitle();
    }
    private void saveFile(boolean forceChooser){
        if (currentFile == null || forceChooser) {
            JFileChooser chooser = imageChooser();
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            currentFile = file;
        }
        try {
            ImageIO.write(canvas.getImage(), "PNG", currentFile);
            modified = false;
            updateTitle();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save file:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
