package se.bergqvist.bergqvistmediaplayer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
// import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Scrollable;
// import javax.swing.JViewport;
// import javax.swing.event.ListSelectionEvent;
// import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

/**
 * Main frame.
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class MainFrame extends javax.swing.JFrame {

    private MediaPlayerWindow mediaPlayerWindow;

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
//    private final Collator swedishCollator = Collator.getInstance(Locale.of("sv","SE"));
//    private final Collator swedishCollator = Collator.getInstance();
    private final Comparator stringComparator = (a,b) -> {
        return ((String)a).toLowerCase().compareTo(((String)b).toLowerCase());
    };
    Comparator pathComparator = (Comparator<Path>) (Path o1, Path o2) -> {
        return stringComparator.compare(o1.toFile().getName().toLowerCase(), o2.toFile().getName().toLowerCase());
    };
    private final MyTreeModel folderTreeModel = new MyTreeModel();
    private final DefaultListModel folderModel = new DefaultListModel();
    private final List<FolderOrMovieItem> currentFolder_foldersAndMoviesList = new ArrayList<>();
    private final SortedMap<Path, SortedSet<Path>> foldersAndSubfoldersMap = new TreeMap<>();
    private final SortedMap<Path, List<Path>> foldersAndMoviesMap = new TreeMap<>(pathComparator);

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        initComponents();
/*
        movieList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = movieList.locationToIndex(e.getPoint());

                if (index != -1) {
                    Path movie = movieList.getSelectedValue().getMovie();
                    System.out.format("Movie: %s%n", movie);
                    mediaPlayerWindow = new MediaPlayerWindow(MainFrame.this, movie.toFile());
                }
            }
        });
*/
        folderTree.setFont(folderTree.getFont().deriveFont(22f));
//        movieList.setFont(movieList.getFont().deriveFont(22f));
//        jSplitPane1.setDividerLocation(0.4);
        jSplitPane1.setDividerLocation(0.3);
//        jSplitPane1.setResizeWeight(0.4);
        jSplitPane1.setResizeWeight(0.3);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        loadMovies();
//        MoviePanel moviePanel = new MoviePanel();
//        moviePanel.init();
//        jScrollPaneMovies.add(moviePanel);
        ((MoviePanel)moviePanel).init();
        pack();

//        folderTree.setSelectionRow(0);  // AAAAAAAAA
//        if (1==0)
        java.awt.EventQueue.invokeLater(() -> {
            folderTree.expandRow(0);        // AAAAAAAAA
//            folderTree.setSelectionRow(2);  // AAAAAAAAA
            folderTree.setSelectionRow(1);  // AAAAAAAAA
        });
//        folderTree.setSelectionRow(2);  // AAAAAAAAA
    }

    public void exitProgram() {
        // Release mediaPlayerComponent.
        mediaPlayerComponent.release();
        System.exit(0);
    }

    public void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private void selectFolderInTree(Path p) {

        Path pTemp = p;
        List<TreeItem> list = new ArrayList<>();
        while (pTemp != null) {
            TreeItem ti = folderTreeModel.pathTreeItemMap.get(pTemp);
            System.out.format("TreeItem: %s%n", ti);
            if (ti != null) {
                // Insert first in the list
                list.add(0, ti);
            }
            pTemp = pTemp.getParent();
        }
        // Add root node to the beginning of the list
        list.add(0, folderTreeModel.root);

        TreePath path = new TreePath(list.toArray(TreeItem[]::new));
        folderTree.expandPath(path.getParentPath());
        int row = folderTree.getRowForPath(path);
        folderTree.setSelectionRow(row);
    }

    private void loadMovies() {
        var mainFolders = SystemProperties.get().getMainFolders();

        try {
            Set<String> validExtensions = new HashSet<>();
            Set<String> invalidExtensions = new HashSet<>();
            Set<String> unknownExtensions = new HashSet<>();

            validExtensions.add("avi");
            validExtensions.add("mp4");
            validExtensions.add("m4v");
            validExtensions.add("mkv");
            validExtensions.add("mov");
            validExtensions.add("webm");
            validExtensions.add("webp");

            validExtensions.add("mp3");
            validExtensions.add("m4a");

            invalidExtensions.add("bergqvist");
            invalidExtensions.add("bup");
            invalidExtensions.add("ifo");
            invalidExtensions.add("nfo");
            invalidExtensions.add("part");
            invalidExtensions.add("sh");
            invalidExtensions.add("sh_");
            invalidExtensions.add("sh__");
            invalidExtensions.add("srt");
            invalidExtensions.add("vob");
            invalidExtensions.add("jpg");
            invalidExtensions.add("jpeg");
            invalidExtensions.add("png");
            invalidExtensions.add("gif");
            invalidExtensions.add("svg");
            invalidExtensions.add("bmp");
            invalidExtensions.add("ico");
            invalidExtensions.add("txt");
            invalidExtensions.add("xcf");
            invalidExtensions.add("pdf");



            for (String folder : mainFolders) {
                try (Stream<Path> paths = Files.walk(Paths.get(folder))) {
                    paths
                        .filter(Files::isRegularFile)
                        .forEach(path -> {

                            String filename = path.getFileName().toString();
                            Path filenameFolder = path.getParent();
                            Path parentFolder = path.getParent().getParent();

                            String extension = "";
                            int extensionPos = filename.lastIndexOf('.');
                            if (extensionPos != -1) {
                                extension = filename.substring(extensionPos+1);
                            }

                            extension = extension.toLowerCase();

                            if (validExtensions.contains(extension)) {
//                                currentFolder_foldersAndMoviesList.add(new FolderOrMovieItem(path, false));

                                foldersAndSubfoldersMap.computeIfAbsent(parentFolder, f -> new TreeSet<>(pathComparator));

                                foldersAndSubfoldersMap.get(parentFolder).add(filenameFolder);

                                foldersAndMoviesMap.computeIfAbsent(
                                        filenameFolder, f -> new ArrayList<>());

                                foldersAndMoviesMap.get(filenameFolder).add(path);

                            } else if (invalidExtensions.contains(extension)) {
                                // Do nothing
                            } else {
                                unknownExtensions.add(extension);
                                System.out.format("Folder: %s, File: %s, Extension: %s%n", filenameFolder, filename, extension);
                            }
                        });
                }
            }

            for (String ext : unknownExtensions) {
                System.out.format("Unknown extension: %s%n", ext);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        this.pack();

        folderTree.addTreeSelectionListener((e) -> {
            var folder = ((TreeItem)e.getPath().getLastPathComponent()).getFolder();
            currentFolder_foldersAndMoviesList.clear();

            Set<Path> tempFolderList = foldersAndSubfoldersMap.get(folder);
            if (tempFolderList != null) {
                List<Path> folders = new ArrayList<>(tempFolderList);
                for (Path p : folders) {
                    currentFolder_foldersAndMoviesList.add(new FolderOrMovieItem(p, true));
                }
            }

            List<Path> tempList = foldersAndMoviesMap.get(folder);
            if (tempList != null) {
                List<Path> movies = new ArrayList<>(tempList);
                Collections.sort(movies, pathComparator);
                for (Path p : movies) {
                    currentFolder_foldersAndMoviesList.add(new FolderOrMovieItem(p, false));
                }
            }
            ((MoviePanel)moviePanel).showMovies();
        });

        currentFolder_foldersAndMoviesList.clear();
        folderModel.clear();
        folderModel.addAll(foldersAndMoviesMap.keySet());

        for (Path p : foldersAndMoviesMap.keySet()) {
            folderTreeModel.getFolderNode(p);
        }
        folderTreeModel.notifyTreeChanged();
//        expandAll(folderTree);

//        ((MoviePanel)moviePanel).showMovies();

//        exitProgram();
    }

//    private static class MoviePanel extends JPanel {
    private class MoviePanel extends JPanel implements Scrollable {

        private final ImageIcon folderIcon;
        private final ImageIcon movieIcon;
        private final Font font;

        private MoviePanel() {
            try {
//                var folderImage = this.getClass().getResource("/resources/folder-1485_128.png");
                var folderImage = this.getClass().getResource("/resources/folder-1485_64.png");
                BufferedImage folderBufferedImage = ImageIO.read(folderImage);
                folderIcon = new ImageIcon(folderBufferedImage);

                var movieImage = this.getClass().getResource("/resources/video-camera-2806_64.png");
                BufferedImage movieBufferedImage = ImageIO.read(movieImage);
                movieIcon = new ImageIcon(movieBufferedImage);

            } catch (IOException e) {
//                e.printStackTrace();
                throw new RuntimeException(e);
            }

            JLabel testLabel = new JLabel();
            font = testLabel.getFont().deriveFont(22f);
        }

        private void init() {
            this.setBackground(Color.WHITE);

//            var size = this.getPreferredSize();
//            System.out.format("Size: %d, %d%n", size.width, size.height);

//            showMovies();
        }

        private void showMovies() {

            this.removeAll();

            // https://docs.oracle.com/javase/tutorial/uiswing/layout/gridbag.html
            // https://docs.oracle.com/javase/8/docs/api/java/awt/GridBagLayout.html
            // https://docs.oracle.com/javase/8/docs/api/java/awt/GridBagConstraints.html#gridx
            this.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
//                c.weightx = 0.5;
//                c.weighty = 0.5;
            c.weightx = 0;
            c.weighty = 0;
            c.anchor = GridBagConstraints.CENTER;
            c.anchor = GridBagConstraints.NORTH;
//                c.fill = GridBagConstraints.BOTH;
            c.fill = GridBagConstraints.NONE;
//            int insets = 10;
//            c.insets = new Insets(insets, insets, insets, insets);


            final Insets InsetsIcon = new Insets(15, 5, 5, 5);
            final Insets InsetsFilename = new Insets(5, 10, 15, 10);


//            c.insets = new Insets(top, left, bottom, right);

            final int labelWidth = 230;
            final int totWidth = labelWidth + InsetsFilename.left + InsetsFilename.right;

            int x = 0;
            int y = 0;
            int width = totWidth;

            for (var movie : currentFolder_foldersAndMoviesList) {
                c.gridx = x;
                c.gridy = y;
                c.insets = InsetsIcon;
                JLabel iconLabel;
                if (movie.isFolder) {
                    iconLabel = new JLabel(folderIcon);
                    iconLabel.addMouseListener(new MouseClickListener(()-> {
                        selectFolderInTree(movie.path);
                    }));
                } else {
                    iconLabel = new JLabel(movieIcon);
                    iconLabel.addMouseListener(new MouseClickListener(()-> {
                        mediaPlayerWindow = new MediaPlayerWindow(MainFrame.this, movie.path.toFile());
                    }));
                }
                add(iconLabel, c);
                c.gridy = y + 1;

                c.insets = InsetsFilename;
                MyLabel textLabel;
                if (movie.isFolder) {
                    textLabel = new MyLabel(labelWidth, movie.toString(), font, ()-> {
                        selectFolderInTree(movie.path);
                    });
                } else {
                    textLabel = new MyLabel(labelWidth, movie.toString(), font, ()-> {
                        mediaPlayerWindow = new MediaPlayerWindow(MainFrame.this, movie.path.toFile());
                    });
                }
                add(textLabel, c);
                x++;

                c.gridx = x;
//                add(Box.createHorizontalStrut(5), c);
                x++;

                width += labelWidth + 5 + 10;
                if (width >= this.getWidth()) {
                    x = 0;
                    y += 2;
                    width = totWidth;
                }
            }


            c.gridx = 99;
            c.gridy = 99;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            add(new JLabel(""), c);


            Container parent = this.getParent();
            parent.validate();
            parent.repaint();
//            this.invalidate();
//            MainFrame.this.pack();

//            java.awt.EventQueue.invokeLater(() -> jScrollPaneMovies.getHorizontalScrollBar().setValue(0));
        }

        // https://docs.oracle.com/javame/config/cdc/opt-pkgs/api/agui/jsr209/javax/swing/Scrollable.html

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return this.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
//            return scrollY *= 2;
            return 50;
//            return (int) (100 * Math.random());
//            throw new UnsupportedOperationException("Not supported");
        }

        private int scrollY = 1;
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
//            return scrollY *= 2;
            return 50;
//            return (int) (100 * Math.random());
//            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Don't allow horizontal scrolling
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Allow vertical scrolling
            return false;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        folderTree = new javax.swing.JTree();
        jScrollPaneMovies = new javax.swing.JScrollPane();
        moviePanel = new MoviePanel();
        mainMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        menuItemQuit = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        menuItemPreferences = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(300);

        folderTree.setModel(folderTreeModel);
        folderTree.setRootVisible(false);
        jScrollPane1.setViewportView(folderTree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        javax.swing.GroupLayout moviePanelLayout = new javax.swing.GroupLayout(moviePanel);
        moviePanel.setLayout(moviePanelLayout);
        moviePanelLayout.setHorizontalGroup(
            moviePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 731, Short.MAX_VALUE)
        );
        moviePanelLayout.setVerticalGroup(
            moviePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 380, Short.MAX_VALUE)
        );

        jScrollPaneMovies.setViewportView(moviePanel);

        jSplitPane1.setRightComponent(jScrollPaneMovies);

        fileMenu.setText("File");

        menuItemQuit.setText("Quit");
        menuItemQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemQuitActionPerformed(evt);
            }
        });
        fileMenu.add(menuItemQuit);

        mainMenuBar.add(fileMenu);

        editMenu.setText("Edit");

        menuItemPreferences.setText("Preferences");
        editMenu.add(menuItemPreferences);

        mainMenuBar.add(editMenu);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemQuitActionPerformed
        exitProgram();
        System.exit(0);
    }//GEN-LAST:event_menuItemQuitActionPerformed


    public static class MouseClickListener implements MouseListener {

        private final Runnable action;

        public MouseClickListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            action.run();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Do nothing
        }
    }

    private static class FolderOrMovieItem {

        private final Path path;
        private final boolean isFolder;

        private FolderOrMovieItem(Path m, boolean isFolder) {
            this.path = m;
            this.isFolder = isFolder;
        }

        @Override
        public String toString() {
            return path.getFileName().toString();
        }
    }


    private static class TreeItem {
        private final Path folder;
        private final List<TreeItem> children = new ArrayList<>();

        public TreeItem(Path p) {
            this.folder = p;
        }

        public Path getFolder() {
            return folder;
        }

        public List<TreeItem> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            if (folder != null) {
                return folder.getFileName().toString();
            } else {
                return "";
            }
        }
    }


    private static class MyTreeModel implements TreeModel {

        private final TreeItem root = new TreeItem(null);
        private final Map<Path, TreeItem> pathTreeItemMap = new HashMap<>();
        private final List<TreeModelListener> listeners = new ArrayList<>();


        public MyTreeModel() {
        }

        @Override
        public Object getRoot() {
            return root;
        }

        public TreeItem getFolderNode(Path folder) {
            int index = folder.getNameCount();
            TreeItem item = null;
            Path tempPath = folder;
            while (!tempPath.toString().equals("/") && (item = pathTreeItemMap.get(tempPath)) == null) {
                tempPath = tempPath.getParent();
                index--;
            }
            if (item == null) {
                item = root;
            }
            Path rootPath = Path.of("/");
            if (folder != tempPath) {
                for (int i=index; i < folder.getNameCount(); i++) {
                    Path thisPath = rootPath.resolve(folder.subpath(0, i+1));
                    TreeItem newItem = new TreeItem(thisPath);
                    item.children.add(newItem);
                    pathTreeItemMap.put(thisPath, newItem);
                    item = newItem;
                }
            }
            return item;
        }

        @Override
        public Object getChild(Object parent, int index) {
            return ((TreeItem)parent).children.get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            return ((TreeItem)parent).children.size();
        }

        @Override
        public boolean isLeaf(Object node) {
            return ((TreeItem)node).children.isEmpty();
        }

        // This method is invoked by the JTree only for editable trees.
        // This TreeModel does not allow editing, so we do not implement
        // this method.  The JTree editable property is false by default.
        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return ((TreeItem)parent).children.indexOf(child);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
//            // This model doesn't allow editing so listeners will never be called.
            listeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
//            // This model doesn't allow editing so listeners will never be called.
            listeners.remove(l);
        }

        public void notifyTreeChanged() {
            for (TreeModelListener l : listeners) {
                l.treeStructureChanged(new TreeModelEvent(this, new Object[]{root}));
            }
        }

    }



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTree folderTree;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneMovies;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenuItem menuItemPreferences;
    private javax.swing.JMenuItem menuItemQuit;
    private javax.swing.JPanel moviePanel;
    // End of variables declaration//GEN-END:variables

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());

}
