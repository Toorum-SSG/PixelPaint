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
        canvas = new CanvasPanel(800, 600);
        toolPanel = new ToolPanel(canvas);
        statusBar = new StatusBar();
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.getViewport().setBackground(new Color(180, 180, 180));
        canvas.setScrollPane(scrollPane);
        add(toolPanel,  BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar,  BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());
        statusBar.attachMouseTracking(canvas);
        statusBar.setCanvasSize(800, 600);
        registerKeyboardShortcuts();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
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

    private void openFile(){
        if (!confirmDiscardChanges()) return;
        JFileChooser chooser = imageChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage loaded = ImageIO.read(file);
                if (loaded == null) throw new IOException("Unsupported format");
                canvas.resizeCanvas(loaded.getWidth(), loaded.getHeight());
                Graphics2D g = canvas.getImage().createGraphics();
                g.drawImage(loaded, 0, 0, null);
                g.dispose();
                canvas.repaint();
                currentFile = file;
                modified    = false;
                statusBar.setCanvasSize(loaded.getWidth(), loaded.getHeight());
                updateTitle();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not open file:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JFileChooser imageChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp"));
        return chooser;
    }

    private void openSettings() {
        SettingsDialog dlg = new SettingsDialog(this, canvas);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            int w = dlg.getCanvasWidth();
            int h = dlg.getCanvasHeight();
            if (w != canvas.getImage().getWidth() || h != canvas.getImage().getHeight()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Resizing the canvas will clear all artwork. Continue?",
                        "Resize Canvas", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    canvas.resizeCanvas(w, h);
                    statusBar.setCanvasSize(w, h);
                }
            }
            canvas.setMaxUndoSteps(dlg.getMaxUndoSteps());
            canvas.setBrushSize(dlg.getDefaultBrushSize());
            String theme = dlg.getTheme();
            applyTheme(theme);
        }
    }

    private void applyTheme(String theme) {
        try {
            if ("Dark".equals(theme)) {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                UIManager.put("control", new Color(60,  63,  65));
                UIManager.put("text", Color.WHITE);
                UIManager.put("nimbusBase", new Color(43,  43,  43));
                UIManager.put("nimbusBlueGrey", new Color(60,  63,  65));
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
    }

    private void registerKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) return false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_B: canvas.setCurrentTool(Tool.BRUSH);     return true;
                case KeyEvent.VK_E: canvas.setCurrentTool(Tool.ERASER);    return true;
                case KeyEvent.VK_L: canvas.setCurrentTool(Tool.LINE);      return true;
                case KeyEvent.VK_C: canvas.setCurrentTool(Tool.CIRCLE);    return true;
                case KeyEvent.VK_R: canvas.setCurrentTool(Tool.RECTANGLE); return true;
            }
            return false;
        });
    }

    private boolean confirmDiscardChanges() {
        if (!modified) return true;
        int choice = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Discard them?", "Unsaved Changes", JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    private void confirmAndExit() {
        if (confirmDiscardChanges()) System.exit(0);
    }

    private void updateTitle() {
        String name = currentFile == null ? "Untitled" : currentFile.getName();
        setTitle("PixelPaint – " + name + (modified ? " *" : ""));
    }

}
