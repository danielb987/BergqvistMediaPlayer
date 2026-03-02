package se.bergqvist.bergqvistmediaplayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

/**
 * Main frame.
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class MainFrame extends javax.swing.JFrame {

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final DefaultListModel folderModel = new DefaultListModel();
    private final DefaultListModel<MovieItem> movieModel = new DefaultListModel<>();
    private final SortedMap<String, List<Path>> foldersAndMovies = new TreeMap<>();

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        loadMovies();
    }

    public void exitProgram() {
        // Release mediaPlayerComponent.
        mediaPlayerComponent.release();
        System.exit(0);
    }

    private void loadMovies() {
        ListSelectionListener folderSelectionListener = (ListSelectionEvent evt) -> {
            if(!evt.getValueIsAdjusting()) {
                System.out.format("Folder: %s%n", folderList.getSelectedValue());
                movieModel.clear();
                List<Path> movies = new ArrayList<>(foldersAndMovies.get(folderList.getSelectedValue()));
                Collections.sort(movies, (Path a, Path b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
                for (Path p : movies) {
                    movieModel.addElement(new MovieItem(p));
                }
            }
        };
        folderList.addListSelectionListener(folderSelectionListener);
        var mainFolders = SystemProperties.get().getMainFolders();
        folderModel.addAll(mainFolders);

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
                            String filenameFolder = path.getParent().toString();

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
        movieModel.clear();
        folderModel.clear();
        folderModel.addAll(foldersAndMovies.keySet());
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

        jScrollPane2 = new javax.swing.JScrollPane();
        folderList = new JList(folderModel);
        jScrollPane1 = new javax.swing.JScrollPane();
        movieList = new JList(movieModel);
        mainMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        menuItemQuit = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        menuItemPreferences = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane2.setViewportView(folderList);

        jScrollPane1.setViewportView(movieList);

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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 389, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JList<String> folderList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenuItem menuItemPreferences;
    private javax.swing.JMenuItem menuItemQuit;
    private javax.swing.JList<MovieItem> movieList;
    // End of variables declaration//GEN-END:variables

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());

}
