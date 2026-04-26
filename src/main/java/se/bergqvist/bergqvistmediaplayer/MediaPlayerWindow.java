package se.bergqvist.bergqvistmediaplayer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

/**
 * MediaPlayerWindow
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class MediaPlayerWindow {

    private static final GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices()[0];

    private final Object mouseCursorTimerLock = new Object();
    private final JFrame frame;
    private final Cursor emptyCursor;
    private final Timer mouseCursorTimer = new Timer("Mouse cursor timer");
    private TimerTask mouseCursorTimerTask = null;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final SortedMap<Integer, String> bookmarkLabels = new TreeMap<>();
    private final Map<Integer, Long> bookmarkTimes = new HashMap<>();
    private final Properties movieProperties = new Properties();

    private int selectedAudio = Integer.MIN_VALUE;

    private boolean showSubtitles = true;
    private int selectedSubtitle = Integer.MIN_VALUE;


    private void load(File f) {
        File propFile = new File(f.getAbsoluteFile()+".bergqvist");
        if (propFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(propFile, StandardCharsets.UTF_8))) {
                movieProperties.load(reader);
                String selectedAudioStr = movieProperties.getProperty("SelectedAudio");
                if (selectedAudioStr != null) {
                    try {
                        selectedAudio = Integer.parseInt(selectedAudioStr);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                String selectedSubtitleStr = movieProperties.getProperty("SelectedSubtitle");
                if (selectedSubtitleStr != null) {
                    try {
                        selectedSubtitle = Integer.parseInt(selectedSubtitleStr);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                loadBookmarks();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadBookmarks() {
        for (Object k : movieProperties.keySet()) {
            String key = k.toString();
            if (key.startsWith("Bookmark_Label_")) {
                String idStr = key.substring("Bookmark_Label_".length());
                int id = Integer.parseInt(idStr);
                String label = movieProperties.getProperty(key);
                long time = Long.parseLong(movieProperties.getProperty("Bookmark_Time_"+idStr));
                bookmarkLabels.put(id, label);
                bookmarkTimes.put(id, time);
            }
        }
    }

    private void store(File f) {
        File propFile = new File(f.getAbsoluteFile()+".bergqvist");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(propFile, StandardCharsets.UTF_8))) {
            long time = mediaPlayerComponent.mediaPlayer().status().time();
            movieProperties.setProperty("Time", Long.toString(time));
            if (selectedAudio != Integer.MIN_VALUE) {
                movieProperties.setProperty("SelectedAudio", Integer.toString(selectedAudio));
            }
            if (selectedSubtitle != Integer.MIN_VALUE) {
                movieProperties.setProperty("SelectedSubtitle", Integer.toString(selectedSubtitle));
            }
            movieProperties.store(writer, f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeWindow(File f) {
        store(f);
        // Release mediaPlayerComponent.
        if (mediaPlayerComponent.mediaPlayer().media().info().state() == State.PLAYING) {
            mediaPlayerComponent.mediaPlayer().controls().pause();
        }
        mediaPlayerComponent.release();
        System.out.println("Exit BergqvistMediaPlayer");
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
        });
    }

    private void showOrHideSubtitles() {
        showOrHideSubtitles(!showSubtitles);
    }

    private void showOrHideSubtitles(boolean show) {
        MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();

        showSubtitles = show;

        if (showSubtitles) {
            // Enable subtitles
            if (selectedSubtitle != Integer.MIN_VALUE) {
                mediaPlayer.subpictures().setTrack(selectedSubtitle);
            } else {
                List<TrackDescription> tracks = mediaPlayer.subpictures().trackDescriptions();
                for (var track : tracks) {
                    System.out.format("track %d: %s%n", track.id(), track.description());
                    if (track.id() != -1) {
                        selectedSubtitle = track.id();
                        mediaPlayer.subpictures().setTrack(track.id());
                    }
                }
            }
        } else {
            // Disable subtitles
            mediaPlayer.subpictures().setTrack(-1);
        }
    }

    public static void runOnGUI(Runnable ta) {
        if (SwingUtilities.isEventDispatchThread()) {
            // run now
            ta.run();
        } else {
            // dispatch to Swing
            try {
                SwingUtilities.invokeAndWait(ta);
            } catch (InterruptedException e) {
                System.out.println("Interrupted while running on GUI thread");
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                System.out.format("Error while on GUI thread: %s", e.getCause());
                System.out.format("   Came from call to runOnGUI: %s", e);
                e.printStackTrace();
                // should have been handled inside the ThreadAction
            }
        }
    }

    public static void runOnGUIEventually(Runnable ta) {
        // dispatch to Swing
        SwingUtilities.invokeLater(ta);
    }

    private void startMouseCursorTimerTask() {
        synchronized(mouseCursorTimerLock) {
            if (mouseCursorTimerTask != null) {
                mouseCursorTimerTask.cancel();
            }
            mouseCursorTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mediaPlayerComponent.setCursor(emptyCursor);
                }
            };
            mouseCursorTimer.schedule(mouseCursorTimerTask, 2000);
        }
    }

    public MediaPlayerWindow(File f) {
        AtomicReference<File> fileRef = new AtomicReference<>(f);
        JPanel audioPane = new JPanel();
        JPanel subtitlesPane = new JPanel();

        JSlider slider = new JSlider(0,1000);
        slider.setEnabled(false);

        load(fileRef.get());

        frame = new JFrame("My First Media Player");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow(fileRef.get());
            }
        });

//        // https://stackoverflow.com/questions/45722445/how-to-set-jframe-to-full-screen
//        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.setUndecorated(true);
//        frame.setBounds(100, 100, 600, 400);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());


        // Hide mouse cursor
        emptyCursor = frame.getToolkit().createCustomCursor(
                new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ),
                new Point(),
                null );

//        System.out.format("Load MediaPlayerComponent%n");
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mediaPlayerComponent.setCursor(Cursor.getDefaultCursor());
                startMouseCursorTimerTask();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mediaPlayerComponent.setCursor(Cursor.getDefaultCursor());
                startMouseCursorTimerTask();
            }
        };

        mediaPlayerComponent.setCursor(emptyCursor);

        MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

        mediaPlayer.events().addMediaPlayerEventListener(
                new MediaPlayerEventAdapter() {

                    @Override
                    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
//                        System.out.format(":: New media: %s%n", media.duplicateMedia().info().mrl());
                    }

                    @Override
                    public void opening(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Opening%n");
                    }

                    @Override
                    public void buffering(MediaPlayer mediaPlayer, float newCache) {
//                        System.out.format(":: Buffering. New cache: %s%n", newCache);
                    }

                    @Override
                    public void playing(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Playing%n");
                    }

                    @Override
                    public void paused(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Paused%n");
                        store(fileRef.get());
                    }

                    @Override
                    public void stopped(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Stopped%n");
                        store(fileRef.get());
                    }

                    @Override
                    public void forward(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Forward%n");
                    }

                    @Override
                    public void backward(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Backward%n");
                    }

                    @Override
                    public void finished(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Finished%n");
                        runOnGUIEventually(() -> {
                            frame.dispose();
                        });
                    }

                    @Override
                    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
//                        System.out.format(":: New time: %s%n", newTime);
//                        System.out.format(":: New time: %s%n", mediaPlayer.status().time());
                        runOnGUIEventually(() -> {
                            slider.setEnabled(false);
                            slider.setValue((int) (newTime/1000));
                            slider.setEnabled(true);
                        });
                    }

                    @Override
                    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
//                        System.out.format(":: New position: %s%n", newPosition);
                    }

                    @Override
                    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
//                        System.out.format(":: Seekable changed: %s%n", newSeekable);
                    }

                    @Override
                    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
//                        System.out.format(":: Pausable changed: %s%n", newPausable);
                    }

                    @Override
                    public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
//                        System.out.format(":: Title changed: %s%n", newTitle);

                        runOnGUIEventually(() -> {
                            audioPane.removeAll();

                            List<TrackDescription> tracks = mediaPlayer.audio().trackDescriptions();
                            for (var track : tracks) {
                                JButton audioButton = new JButton(track.description());
                                audioButton.addActionListener(e -> {
                                    mediaPlayerComponent.mediaPlayer().submit(() -> {
                                        selectedAudio = track.id();
                                        mediaPlayer.audio().setTrack(track.id());
                                    });
                                });
                                audioButton.setFocusable(false);
                                audioPane.add(audioButton);
                                frame.pack();
                            }

                            // Ensure the desired audio is selected
                            if (selectedAudio != Integer.MIN_VALUE) {
                                mediaPlayer.audio().setTrack(selectedAudio);
                            }


                            subtitlesPane.removeAll();

                            tracks = mediaPlayer.subpictures().trackDescriptions();
                            for (var track : tracks) {
                                JButton subtitleButton = new JButton(track.description());
                                subtitleButton.addActionListener(e -> {
                                    mediaPlayerComponent.mediaPlayer().submit(() -> {
                                        selectedSubtitle = track.id();
                                        mediaPlayer.subpictures().setTrack(track.id());
                                        showOrHideSubtitles(true);
                                    });
                                });
                                subtitleButton.setFocusable(false);
                                subtitlesPane.add(subtitleButton);
                                frame.pack();
                            }

                            // Ensure the desired subtitle is selected
                            if (selectedSubtitle != Integer.MIN_VALUE) {
                                mediaPlayer.subpictures().setTrack(selectedSubtitle);
                            }
                        });
                    }

                    @Override
                    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
//                        System.out.format(":: Length changed: %s%n", newLength);
                        runOnGUIEventually(() -> {
                            slider.setEnabled(false);
                            slider.setMaximum((int) (newLength/1000));
                            slider.setEnabled(true);
                        });
                    }

                    @Override
                    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {
//                        System.out.format(":: Chapter changed: %s%n", newChapter);
                    }

                    @Override
                    public void error(MediaPlayer mediaPlayer) {
//                        System.out.format(":: Error: %s%n", mediaPlayer);
                    }

                    @Override
                    public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                        System.out.format(":: MediaPlayerReady%n");
                        List<TrackDescription> tracks = mediaPlayer.subpictures().trackDescriptions();
                        for (var track : tracks) {
                            System.out.format("track %d: %s%n", track.id(), track.description());
                        }
/*
                        // Hide subtitles
                        List<TrackDescription> tracks = mediaPlayer.subpictures().trackDescriptions();
                        System.out.format("Num tracks: %d%n", tracks.size());
                        for (var track : tracks) {
                            System.out.format("Track: %s%n", track);
                        }
//                        System.exit(0);
                        // Disable subtitles
                        mediaPlayer.subpictures().setTrack(-1);
                        // Enable subtitles
                        for (var track : tracks) {
                            if (track.id() != -1) {
                                mediaPlayer.subpictures().setTrack(track.id());
                            }
                        }
//                        mediaPlayer.subpictures().setSpu(trackId);
*/
                    }
                });

        JPanel southPanels = new JPanel();
        southPanels.setLayout(new BoxLayout(southPanels, BoxLayout.Y_AXIS));

        audioPane.setVisible(false);
        southPanels.add(audioPane);

        subtitlesPane.setVisible(false);
        southPanels.add(subtitlesPane);

        JPanel controlsPane = new JPanel();
        controlsPane.setVisible(false);
/*
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            mediaPlayer.controls().pause();
            fileRef.set(null);
            mediaPlayer.submit(() -> {
                movieProperties.clear();
                fileRef.set(new File("/daniel_data/film/Full_resolution/brollop.i.italien-682cb35-svtplay.mp4"));
                load(fileRef.get());
                mediaPlayer.media().play(fileRef.get().getAbsolutePath());
                long time = Long.parseLong(movieProperties.getProperty("Time"));
                mediaPlayer.controls().setTime(time);
            });
        });
        controlsPane.add(loadButton);
*/
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> {
            mediaPlayerComponent.mediaPlayer().submit(() -> {
                store(f);
                // Release mediaPlayerComponent.
                mediaPlayerComponent.mediaPlayer().controls().pause();
                mediaPlayerComponent.release();
                System.out.println("Exit BergqvistMediaPlayer");
                SwingUtilities.invokeLater(frame::dispose);
            });
        });
        stopButton.setFocusable(false);
        controlsPane.add(stopButton);

        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> {
            mediaPlayer.controls().pause();
        });
        pauseButton.setFocusable(false);
        controlsPane.add(pauseButton);

        JButton rewindAllButton = new JButton("Rewind all");
        rewindAllButton.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(-10000);
            mediaPlayer.controls().setTime(0);
        });
        rewindAllButton.setFocusable(false);
        controlsPane.add(rewindAllButton);

        JButton rewind10Button = new JButton("Rewind x10");
        rewind10Button.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(-10000);
            mediaPlayer.controls().skipTime(-1000*10);
        });
        rewind10Button.setFocusable(false);
        controlsPane.add(rewind10Button);

        JButton rewindButton = new JButton("Rewind");
        rewindButton.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(-10000);
            mediaPlayer.controls().skipTime(-1000);
        });
        rewindButton.setFocusable(false);
        controlsPane.add(rewindButton);

        JButton skipButton = new JButton("Skip");
        skipButton.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(10000);
            mediaPlayer.controls().skipTime(1000);
        });
        skipButton.setFocusable(false);
        controlsPane.add(skipButton);

        JButton skip10Button = new JButton("Skip x10");
        skip10Button.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(10000);
            mediaPlayer.controls().skipTime(1000*10);
        });
        skip10Button.setFocusable(false);
        controlsPane.add(skip10Button);


        for (var entry : bookmarkLabels.entrySet()) {
            JButton button = new JButton(entry.getValue());
            button.addActionListener(e -> {
                mediaPlayer.controls().setTime((long)bookmarkTimes.get(entry.getKey()));
            });
            controlsPane.add(button);
        }



        slider.addChangeListener(e -> {
            if (slider.isEnabled()) {
                mediaPlayer.controls().setTime(slider.getValue()*1000);
            }
        });
        controlsPane.add(slider);

        southPanels.add(controlsPane);

        contentPane.add(southPanels, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);
        frame.setVisible(true);

        // https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java
        device.setFullScreenWindow(frame);
//        // https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java
//        device.setFullScreenWindow(null);

//        System.out.format("Start paused: %s%n", fileRef.get().getAbsolutePath());
//        mediaPlayer.media().startPaused(f.getAbsolutePath());
        mediaPlayer.media().play(fileRef.get().getAbsolutePath());

//        mediaPlayer.controls().setPosition(10f);
//        System.out.format("Skip time: 5000%n");
//        mediaPlayer.controls().skipTime(5000);
//        mediaPlayer.controls().setTime(300*1000);
//        mediaPlayer.controls().setTime(311090);
        if (movieProperties.containsKey("Time")) {
            long time = Long.parseLong(movieProperties.getProperty("Time"));
            mediaPlayer.controls().setTime(time);
        }
//        System.out.format("Load completed%n");

//        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke("ENTER");

        KeyStroke leftKeyStroke = KeyStroke.getKeyStroke("LEFT");
        Action leftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(-1000*1);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(leftKeyStroke, "LEFT");
        frame.getRootPane().getActionMap().put("LEFT", leftAction);

        KeyStroke shiftLeftKeyStroke = KeyStroke.getKeyStroke("shift LEFT");
        Action shiftLeftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(-1000*10);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shiftLeftKeyStroke, "shift LEFT");
        frame.getRootPane().getActionMap().put("shift LEFT", shiftLeftAction);

        KeyStroke ctrlLeftKeyStroke = KeyStroke.getKeyStroke("control LEFT");
        Action ctrlLeftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(-1000*100);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlLeftKeyStroke, "control LEFT");
        frame.getRootPane().getActionMap().put("control LEFT", ctrlLeftAction);

        KeyStroke rightKeyStroke = KeyStroke.getKeyStroke("RIGHT");
        Action rightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(1000*1);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(rightKeyStroke, "RIGHT");
        frame.getRootPane().getActionMap().put("RIGHT", rightAction);

        KeyStroke shiftRightKeyStroke = KeyStroke.getKeyStroke("shift RIGHT");
        Action shiftRightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(1000*10);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shiftRightKeyStroke, "shift RIGHT");
        frame.getRootPane().getActionMap().put("shift RIGHT", shiftRightAction);

        KeyStroke ctrlRightKeyStroke = KeyStroke.getKeyStroke("control RIGHT");
        Action ctrlRightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.controls().skipTime(1000*100);
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlRightKeyStroke, "control RIGHT");
        frame.getRootPane().getActionMap().put("control RIGHT", ctrlRightAction);

        KeyStroke f1KeyStroke = KeyStroke.getKeyStroke("F1");
        Action f1Action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                controlsPane.setVisible(!controlsPane.isVisible());
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f1KeyStroke, "F1");
        frame.getRootPane().getActionMap().put("F1", f1Action);

        KeyStroke shiftF2KeyStroke = KeyStroke.getKeyStroke("shift F2");
        Action shiftF2Action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.submit(() -> {
                    showOrHideSubtitles();
                });
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shiftF2KeyStroke, "shift F2");
        frame.getRootPane().getActionMap().put("shift F2", shiftF2Action);

        KeyStroke f2KeyStroke = KeyStroke.getKeyStroke("F2");
        Action f2Action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                subtitlesPane.setVisible(!subtitlesPane.isVisible());
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f2KeyStroke, "F2");
        frame.getRootPane().getActionMap().put("F2", f2Action);

        KeyStroke f3KeyStroke = KeyStroke.getKeyStroke("F3");
        Action f3Action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                audioPane.setVisible(!audioPane.isVisible());
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f3KeyStroke, "F3");
        frame.getRootPane().getActionMap().put("F3", f3Action);

        KeyStroke spaceKeyStroke = KeyStroke.getKeyStroke("SPACE");
        Action spaceAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mediaPlayer.submit(() -> {
                    mediaPlayerComponent.mediaPlayer().controls().pause();
                });
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(spaceKeyStroke, "SPACE");
        frame.getRootPane().getActionMap().put("SPACE", spaceAction);
/*
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        Action escapeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                closeWindow(fileRef.get());
            }
        };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        frame.getRootPane().getActionMap().put("ESCAPE", escapeAction);
*/
    }

}
