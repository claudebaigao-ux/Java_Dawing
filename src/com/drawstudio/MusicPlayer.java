package com.drawstudio;

import javazoom.jl.player.Player;

import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 基于 JLayer 的背景音乐播放器。
 *
 * <p>支持播放、暂停、上一首、下一首和列表循环。暂停通过一个可阻塞的
 * 输入流实现，因此恢复后会从暂停的位置继续播放，而不是重新开始。</p>
 */
public final class MusicPlayer {

    public enum State {
        STOPPED, PLAYING, PAUSED, ERROR
    }

    public interface Listener {
        void onMusicStateChanged(MusicPlayer player);
    }

    private final Object lock = new Object();
    private final List<File> tracks = new ArrayList<File>();
    private final File musicDirectory;
    private volatile Listener listener;

    private int index;
    private int generation;
    private State state = State.STOPPED;
    private boolean paused;
    private boolean loop = true;
    private Player player;
    private PausableInputStream stream;
    private Thread thread;
    private String errorMessage;

    public MusicPlayer() {
        this.musicDirectory = findMusicDirectory();
        loadTracks();
        if (tracks.isEmpty()) {
            errorMessage = musicDirectory == null
                    ? "未找到 music 文件夹" : "music 文件夹中没有 MP3 文件";
        }
    }

    private void loadTracks() {
        if (musicDirectory == null || !musicDirectory.isDirectory()) {
            return;
        }
        File[] files = musicDirectory.listFiles((dir, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".mp3"));
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        tracks.addAll(Arrays.asList(files));
        if (!tracks.isEmpty()) {
            index = 0;
        }
    }

    private static File findMusicDirectory() {
        List<File> candidates = new ArrayList<File>();
        String home = System.getProperty("drawstudio.home");
        if (home != null && !home.isEmpty()) {
            candidates.add(new File(home, "music"));
        }
        try {
            File codeLocation = new File(MusicPlayer.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File current = codeLocation.isFile() ? codeLocation.getParentFile() : codeLocation;
            for (int i = 0; i < 5 && current != null; i++) {
                candidates.add(new File(current, "music"));
                current = current.getParentFile();
            }
        } catch (URISyntaxException | SecurityException ignored) {
            // 无法定位类路径时继续尝试 user.dir。
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            candidates.add(new File(userDir, "music"));
            candidates.add(new File(userDir, "作品一/music"));
        }

        for (File candidate : candidates) {
            try {
                if (candidate.isDirectory()) {
                    return candidate.getCanonicalFile();
                }
            } catch (IOException ignored) {
                // 继续尝试下一个候选目录。
            }
        }
        return null;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        publishState();
    }

    public File getMusicDirectory() {
        return musicDirectory;
    }

    public int getTrackCount() {
        synchronized (lock) {
            return tracks.size();
        }
    }

    public int getTrackIndex() {
        synchronized (lock) {
            return index;
        }
    }

    public String getTrackName() {
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return "无背景音乐";
            }
            String name = tracks.get(index).getName();
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        }
    }

    public State getState() {
        synchronized (lock) {
            return state;
        }
    }

    public String getErrorMessage() {
        synchronized (lock) {
            return errorMessage;
        }
    }

    public boolean isLoop() {
        synchronized (lock) {
            return loop;
        }
    }

    public void setLoop(boolean loop) {
        synchronized (lock) {
            this.loop = loop;
        }
    }

    public String getStatusText() {
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return errorMessage != null ? errorMessage : "无背景音乐";
            }
            switch (state) {
                case PLAYING:
                    return loop ? "正在播放 · 循环" : "正在播放";
                case PAUSED:
                    return "已暂停";
                case ERROR:
                    return errorMessage != null ? errorMessage : "播放失败";
                default:
                    return "已停止";
            }
        }
    }

    /** 开始播放；若当前已暂停则从暂停位置恢复。 */
    public void play() {
        boolean resume = false;
        boolean start = false;
        int target;
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return;
            }
            if (state == State.PAUSED && stream != null) {
                paused = false;
                stream.setPaused(false);
                state = State.PLAYING;
                resume = true;
                target = index;
            } else if (state == State.PLAYING) {
                return;
            } else {
                start = true;
                target = index;
            }
        }
        if (resume) {
            publishState();
        } else if (start) {
            startTrack(target);
        }
    }

    public void togglePause() {
        boolean start = false;
        int target;
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return;
            }
            if (state == State.PLAYING && stream != null) {
                paused = true;
                stream.setPaused(true);
                state = State.PAUSED;
                target = index;
            } else if (state == State.PAUSED && stream != null) {
                paused = false;
                stream.setPaused(false);
                state = State.PLAYING;
                target = index;
            } else {
                start = true;
                target = index;
            }
        }
        if (start) {
            startTrack(target);
        } else {
            publishState();
        }
    }

    public void next() {
        int target;
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return;
            }
            target = (index + 1) % tracks.size();
        }
        startTrack(target);
    }

    public void previous() {
        int target;
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return;
            }
            target = (index - 1 + tracks.size()) % tracks.size();
        }
        startTrack(target);
    }

    public void stop() {
        synchronized (lock) {
            generation++;
            state = State.STOPPED;
            paused = false;
            closeCurrentQuietly();
            thread = null;
        }
        publishState();
    }

    private void startTrack(int requestedIndex) {
        File file;
        synchronized (lock) {
            if (tracks.isEmpty()) {
                return;
            }
            generation++;
            index = ((requestedIndex % tracks.size()) + tracks.size()) % tracks.size();
            state = State.PLAYING;
            paused = false;
            errorMessage = null;
            closeCurrentQuietly();
            file = tracks.get(index);
            final int myGeneration = generation;
            thread = new Thread(() -> runTrack(file, myGeneration), "drawstudio-music");
            thread.setDaemon(true);
            thread.start();
        }
        publishState();
    }

    private void runTrack(File file, int myGeneration) {
        PausableInputStream input = null;
        Player current = null;
        String failure = null;
        try {
            input = new PausableInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
            current = new Player(input);
            synchronized (lock) {
                if (myGeneration != generation) {
                    return;
                }
                stream = input;
                player = current;
                if (paused) {
                    input.setPaused(true);
                }
            }
            current.play();
        } catch (Exception ex) {
            failure = ex.getMessage();
            if (failure == null || failure.trim().isEmpty()) {
                failure = "无法播放 " + file.getName();
            }
        } finally {
            if (current != null) {
                try {
                    current.close();
                } catch (Exception ignored) {
                    // 关闭失败不影响界面。
                }
            }
            if (input != null) {
                input.closeQuietly();
            }

            boolean autoNext = false;
            boolean report = false;
            synchronized (lock) {
                if (myGeneration != generation) {
                    return;
                }
                player = null;
                stream = null;
                if (failure != null) {
                    if (state != State.STOPPED) {
                        state = State.ERROR;
                        errorMessage = failure;
                        report = true;
                    }
                } else if (state == State.PLAYING && !paused) {
                    boolean hasNext = loop || index < tracks.size() - 1;
                    if (hasNext) {
                        index = (index + 1) % tracks.size();
                        autoNext = true;
                    } else {
                        state = State.STOPPED;
                        report = true;
                    }
                }
            }
            if (autoNext) {
                int target;
                synchronized (lock) {
                    target = index;
                }
                startTrack(target);
            } else if (report) {
                publishState();
            }
        }
    }

    private void closeCurrentQuietly() {
        if (stream != null) {
            stream.closeQuietly();
            stream = null;
        }
        if (player != null) {
            try {
                player.close();
            } catch (Exception ignored) {
                // 忽略关闭异常。
            }
            player = null;
        }
    }

    private void publishState() {
        final Listener target = listener;
        if (target != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    target.onMusicStateChanged(MusicPlayer.this);
                } catch (RuntimeException ignored) {
                    // 界面更新失败不应影响音乐线程。
                }
            });
        }
    }

    /** 可暂停的输入流：播放线程在读取下一帧时会等待，从而实现精确暂停。 */
    private static final class PausableInputStream extends InputStream {
        private final InputStream delegate;
        private boolean paused;
        private boolean closed;

        PausableInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        synchronized void setPaused(boolean paused) {
            this.paused = paused;
            if (!paused) {
                notifyAll();
            }
        }

        private synchronized boolean awaitReadable() {
            while (paused && !closed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return !closed;
        }

        @Override
        public int read() throws IOException {
            if (!awaitReadable()) {
                return -1;
            }
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (!awaitReadable()) {
                return -1;
            }
            return delegate.read(buffer, offset, length);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            synchronized (this) {
                closed = true;
                paused = false;
                notifyAll();
            }
            delegate.close();
        }

        void closeQuietly() {
            try {
                close();
            } catch (IOException ignored) {
                // 忽略。
            }
        }
    }
}
