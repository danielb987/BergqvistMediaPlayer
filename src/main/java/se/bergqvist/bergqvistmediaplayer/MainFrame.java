package se.bergqvist.bergqvistmediaplayer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final MyTreeModel folderTreeModel = new MyTreeModel();
    private final DefaultListModel folderModel = new DefaultListModel();
    private final DefaultListModel<MovieItem> movieModel = new DefaultListModel<>();
    private final SortedMap<Path, List<Path>> foldersAndMovies = new TreeMap<>();

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        initComponents();

        movieList.addMouseListener(new MouseAdapter() {
            int lastSelectedIndex;
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = movieList.locationToIndex(e.getPoint());
                if (index != -1 && index == lastSelectedIndex) {
                    movieList.clearSelection();
                    movieList.setSelectedIndex(index);
                }
                lastSelectedIndex = movieList.getSelectedIndex();
            }
        });

        folderTree.setFont(folderTree.getFont().deriveFont(22f));
        movieList.setFont(movieList.getFont().deriveFont(22f));
        jSplitPane1.setDividerLocation(0.4);
        jSplitPane1.setResizeWeight(0.4);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        loadMovies();
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

    private void loadMovies() {
        var mainFolders = SystemProperties.get().getMainFolders();

        ListSelectionListener movieSelectionListener = (ListSelectionEvent evt) -> {
            if(!evt.getValueIsAdjusting() && movieList.getSelectedValue() != null) {
                Path movie = movieList.getSelectedValue().getMovie();
                System.out.format("Movie: %s%n", movie);
                MediaPlayerWindow mediaPlayerWindow = new MediaPlayerWindow(movie.toFile());
            }
        };
        movieList.addListSelectionListener(movieSelectionListener);

        try {
            Set<String> validExtensions = new HashSet<>();
            Set<String> invalidExtensions = new HashSet<>();
            Set<String> unknownExtensions = new HashSet<>();

            validExtensions.add("mp4");
            validExtensions.add("mkv");
            validExtensions.add("webm");

            invalidExtensions.add("bergqvist");
            invalidExtensions.add("BUP");
            invalidExtensions.add("IFO");
            invalidExtensions.add("part");
            invalidExtensions.add("sh");
            invalidExtensions.add("srt");
            invalidExtensions.add("VOB");


            for (String folder : mainFolders) {
                try (Stream<Path> paths = Files.walk(Paths.get(folder))) {
                    paths
                        .filter(Files::isRegularFile)
                        .forEach(path -> {

                            String filename = path.getFileName().toString();
                            Path filenameFolder = path.getParent();

                            String extension = "";
                            int extensionPos = filename.lastIndexOf('.');
                            if (extensionPos != -1) {
                                extension = filename.substring(extensionPos+1);
                            }

                            if (validExtensions.contains(extension)) {
                                movieModel.addElement(new MovieItem(path));

                                foldersAndMovies.computeIfAbsent(
                                        filenameFolder, f -> new ArrayList<>());

                                foldersAndMovies.get(filenameFolder).add(path);

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

        folderTree.addTreeSelectionListener((e) -> {
            var folder = ((TreeItem)e.getPath().getLastPathComponent()).getFolder();
            movieModel.clear();
            List<Path> tempList = foldersAndMovies.get(folder);
            if (tempList != null) {
                List<Path> movies = new ArrayList<>(tempList);
                Collections.sort(movies, (Path a, Path b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
                for (Path p : movies) {
                    movieModel.addElement(new MovieItem(p));
                }
            }
        });

        movieModel.clear();
        folderModel.clear();
        folderModel.addAll(foldersAndMovies.keySet());

        for (Path p : foldersAndMovies.keySet()) {
            folderTreeModel.getFolderNode(p);
        }
        folderTreeModel.notifyTreeChanged();
        expandAll(folderTree);

//        exitProgram();
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
        movieListScrollPane = new javax.swing.JScrollPane();
        movieList = new JList(movieModel);
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

        movieListScrollPane.setViewportView(movieList);

        jSplitPane1.setRightComponent(movieListScrollPane);

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
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 799, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemQuitActionPerformed
        exitProgram();
        System.exit(0);
    }//GEN-LAST:event_menuItemQuitActionPerformed


    private static class MovieItem {

        private final Path movie;

        private MovieItem(Path m) {
            this.movie = m;
        }

        private Path getMovie() {
            return movie;
        }

        @Override
        public String toString() {
            return movie.getFileName().toString();
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
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenuItem menuItemPreferences;
    private javax.swing.JMenuItem menuItemQuit;
    private javax.swing.JList<MovieItem> movieList;
    private javax.swing.JScrollPane movieListScrollPane;
    // End of variables declaration//GEN-END:variables

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());

}
