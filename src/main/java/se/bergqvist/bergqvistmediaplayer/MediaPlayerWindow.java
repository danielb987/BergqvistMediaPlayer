package se.bergqvist.bergqvistmediaplayer;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
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

    private final JFrame frame;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final SortedMap<Integer, String> bookmarkLabels = new TreeMap<>();
    private final Map<Integer, Long> bookmarkTimes = new HashMap<>();
    private final Properties movieProperties = new Properties();


    private void load(File f) {
        File propFile = new File(f.getAbsoluteFile()+".bergqvist");
        if (propFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(propFile, StandardCharsets.UTF_8))) {
                movieProperties.load(reader);
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
            movieProperties.store(writer, f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MediaPlayerWindow(File f) {
        AtomicReference<File> fileRef = new AtomicReference<>(f);

        JSlider slider = new JSlider(0,1000);
        slider.setEnabled(false);

        load(fileRef.get());

        frame = new JFrame("My First Media Player");

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                store(fileRef.get());
                // Release mediaPlayerComponent.
                mediaPlayerComponent.release();
                System.out.println("Exit BergqvistMediaPlayer");
                frame.dispose();
//                System.exit(0);
            }
        });


//        // https://stackoverflow.com/questions/45722445/how-to-set-jframe-to-full-screen
//        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.setUndecorated(true);
//        frame.setBounds(100, 100, 600, 400);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        System.out.format("Load MediaPlayerComponent%n");
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

        mediaPlayer.events().addMediaPlayerEventListener(
                new MediaPlayerEventAdapter() {

                    @Override
                    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
                        System.out.format(":: New media: %s%n", media.duplicateMedia().info().mrl());
                    }

                    @Override
                    public void opening(MediaPlayer mediaPlayer) {
                        System.out.format(":: Opening%n");
                    }

                    @Override
                    public void buffering(MediaPlayer mediaPlayer, float newCache) {
//                        System.out.format(":: Buffering. New cache: %s%n", newCache);
                    }

                    @Override
                    public void playing(MediaPlayer mediaPlayer) {
                        System.out.format(":: Playing%n");
                    }

                    @Override
                    public void paused(MediaPlayer mediaPlayer) {
                        System.out.format(":: Paused%n");
                        store(fileRef.get());
                    }

                    @Override
                    public void stopped(MediaPlayer mediaPlayer) {
                        System.out.format(":: Stopped%n");
                        store(fileRef.get());
                    }

                    @Override
                    public void forward(MediaPlayer mediaPlayer) {
                        System.out.format(":: Forward%n");
                    }

                    @Override
                    public void backward(MediaPlayer mediaPlayer) {
                        System.out.format(":: Backward%n");
                    }

                    @Override
                    public void finished(MediaPlayer mediaPlayer) {
                        System.out.format(":: Finished%n");
                    }

                    @Override
                    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                        System.out.format(":: New time: %s%n", newTime);
//                        System.out.format(":: New time: %s%n", mediaPlayer.status().time());
                        slider.setEnabled(false);
                        System.out.format("Set slider%n");
                        slider.setValue((int) (newTime/1000));
                        System.out.format("Set slider done%n");
                        slider.setEnabled(true);
                    }

                    @Override
                    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                        System.out.format(":: New position: %s%n", newPosition);
                    }

                    @Override
                    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
                        System.out.format(":: Seekable changed: %s%n", newSeekable);
                    }

                    @Override
                    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
                        System.out.format(":: Pausable changed: %s%n", newPausable);
                    }

                    @Override
                    public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
                        System.out.format(":: Title changed: %s%n", newTitle);
                    }

                    @Override
                    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                        System.out.format(":: Length changed: %s%n", newLength);
                        slider.setEnabled(false);
                        slider.setMaximum((int) (newLength/1000));
                        slider.setEnabled(true);
                    }

                    @Override
                    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {
                        System.out.format(":: Chapter changed: %s%n", newChapter);
                    }

                    @Override
                    public void error(MediaPlayer mediaPlayer) {
                        System.out.format(":: Error: %s%n", mediaPlayer);
                    }

                    @Override
                    public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                        System.out.format(":: MediaPlayerReady%n");

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
                    }
                });

        JPanel controlsPane = new JPanel();
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
        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> {
            mediaPlayer.controls().pause();
        });
        controlsPane.add(pauseButton);

        JButton rewindButton = new JButton("Rewind");
        rewindButton.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(-10000);
            mediaPlayer.controls().skipTime(-1000);
        });
        controlsPane.add(rewindButton);

        JButton skipButton = new JButton("Skip");
        skipButton.addActionListener(e -> {
//            mediaPlayer.controls().skipTime(10000);
            mediaPlayer.controls().skipTime(1000);
        });
        controlsPane.add(skipButton);


        for (var entry : bookmarkLabels.entrySet()) {
            JButton button = new JButton(entry.getValue());
            button.addActionListener(e -> {
                mediaPlayer.controls().setTime((long)bookmarkTimes.get(entry.getKey()));
            });
            controlsPane.add(button);
        }



        slider.addChangeListener(e -> {
            if (slider.isEnabled()) {
                System.out.format("Set slider listener%n");
                mediaPlayer.controls().setTime(slider.getValue()*1000);
//                System.exit(0);
            }
        });
        controlsPane.add(slider);

        contentPane.add(controlsPane, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);
        frame.setVisible(true);

        // https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java
        device.setFullScreenWindow(frame);
//        // https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java
//        device.setFullScreenWindow(null);

        System.out.format("Start paused: %s%n", fileRef.get().getAbsolutePath());
//        mediaPlayer.media().startPaused(f.getAbsolutePath());
        mediaPlayer.media().play(fileRef.get().getAbsolutePath());

//        mediaPlayer.controls().setPosition(10f);
        System.out.format("Skip time: 5000%n");
//        mediaPlayer.controls().skipTime(5000);
//        mediaPlayer.controls().setTime(300*1000);
//        mediaPlayer.controls().setTime(311090);
        long time = Long.parseLong(movieProperties.getProperty("Time"));
        mediaPlayer.controls().setTime(time);
        System.out.format("Load completed%n");
    }

}
