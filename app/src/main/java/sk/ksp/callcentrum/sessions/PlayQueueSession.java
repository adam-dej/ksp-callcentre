package sk.ksp.callcentrum.sessions;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sk.ksp.callcentrum.BuildConfig;
import sk.ksp.callcentrum.CallSessionManager;
import sk.ksp.callcentrum.DataStorage;
import sk.ksp.callcentrum.R;

public class PlayQueueSession extends CallSessionManager {

    private String phoneNumber;
    private boolean dialerOpen;

    private class TimerUpdateRunnable implements Runnable {

        private boolean stop = false;
        private int seconds = 0;

        @Override
        public void run() {
            while (!stop) {
                try {
                    Thread.sleep(1000);
                    uiHandler.obtainMessage(MESSAGE_UPDATE_TIME, String.format("%02d:%02d", seconds / 60, seconds % 60)).sendToTarget();
                    seconds++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopTimer() {
            stop = true;
        }
    }

    private TimerUpdateRunnable timerUpdateRunnable;

    private class ServerCommThread implements Runnable, MediaQueue.MediaQueueEmptyCallback {

        private Socket commSocket;
        private OutputStreamWriter out;
        private BufferedReader  in;
        private MediaQueue queue = new MediaQueue(this, context, this);
        private boolean properTermination;

        private int readerState = 0;

        private static final int STATE_UNINITIALIZED = 0;
        private static final int STATE_NORMAL = 1;
        private static final int STATE_DEAD = 2;

        private void serverWrite(String line) throws IOException{
            if (BuildConfig.DEBUG) {
                Log.d("PlayQueueSession", "-> " + line);
            }
            out.write(line + '\n');
            out.flush();
        }

        private String serverRead() throws IOException {
            String line = in.readLine();
            if (BuildConfig.DEBUG) {
                Log.d("PlayQueueSession", line == null ? "<- (NULL)" : "<- " + line);
            }
            return line;
        }

        private void handleCommFailure(Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e("PlayQueueSession", e.toString());
            }
            readerState = STATE_DEAD;
            killCallWithMessage(resources.getString(R.string.ksp_call_terminated));
            timerUpdateRunnable.stopTimer();
        }

        public void sendButtonPress(char button) {
            if (readerState == STATE_NORMAL) {
                try {
                    serverWrite("button " + button);
                } catch (IOException e) {
                    handleCommFailure(e);
                }
            }
        }

        public void sendShakeEvent() {
            if (readerState == STATE_NORMAL) {
                try {
                    serverWrite("shake");
                } catch (IOException e) {
                    handleCommFailure(e);
                }
            }
        }

        public void killComm() {
            try {
                properTermination = true;
                commSocket.close();
                queue.clear();
            } catch (IOException e) {
            }
        }

        @Override
        public void run() {
            uiHandler.obtainMessage(MESSAGE_SHOW_NUMBER, phoneNumber).sendToTarget();
            uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, resources.getString(R.string.ksp_dialing)).sendToTarget();
            try {
                InetAddress serverAddr = InetAddress.getByName(DataStorage.getStorage().getServerAddress());
                commSocket = new Socket(serverAddr, Integer.decode(DataStorage.getStorage().getServerPort()));
                uiHandler.obtainMessage(MESSAGE_HIDE_MESSAGEBAR).sendToTarget();
                timerUpdateRunnable = new TimerUpdateRunnable();
                new Thread(timerUpdateRunnable).start();

                out = new OutputStreamWriter(commSocket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(commSocket.getInputStream()));

                serverWrite("druzinka " + BuildConfig.druzinkaName + " " + phoneNumber);

                while (true) {
                    String line = serverRead();
                    if (line == null) {
                        readerState = STATE_DEAD;
                        killCallWithMessage(resources.getString(R.string.ksp_lost_signal));
                        queue.clear();
                        timerUpdateRunnable.stopTimer();
                        break;
                    }

                    switch (readerState) {
                        case STATE_UNINITIALIZED:
                            if ("start".equals(line)) {
                                readerState = STATE_NORMAL;
                            }
                            break;
                        case STATE_NORMAL:
                            if (line.startsWith("play")) {
                                queue.push(line.split(" ")[1]);
                            } else if ("clear".equals(line)) {
                                queue.clear();
                            } else if (line.startsWith("image")) {
                                String img = line.split(" ")[1];
                                if ("old".equals(img)) {
                                    uiHandler.obtainMessage(MESSAGE_SHOW_IMAGE, R.drawable.call_alf).sendToTarget();
                                } else if ("child".equals(img)) {
                                    uiHandler.obtainMessage(MESSAGE_SHOW_IMAGE, R.drawable.call_child).sendToTarget();
                                } else if ("alf".equals(img)) {
                                    uiHandler.obtainMessage(MESSAGE_SHOW_IMAGE, R.drawable.call_old).sendToTarget();
                                }
                            } else if (line.startsWith("name")) {
                                uiHandler.obtainMessage(MESSAGE_SHOW_NAME, line.replaceFirst("name ", "")).sendToTarget();
                            } else if ("shutdown".equals(line)) {
                                queue.push("shutdown");
                            }
                            break;
                    }

                }

            } catch (UnknownHostException e) {
                queue.clear();
                Log.e("PlayQueueSession", e.toString());
                killCallWithMessage(resources.getString(R.string.ksp_no_signal));
            } catch (IOException e) {
                queue.clear();
                if (properTermination) {
                    readerState = STATE_DEAD;
                    killCallWithMessage(resources.getString(R.string.ksp_call_terminated));
                    if (timerUpdateRunnable != null) {
                        timerUpdateRunnable.stopTimer();
                    }
                } else {
                    Log.e("PlayQueueSession", e.toString());
                    killCallWithMessage(resources.getString(R.string.ksp_no_signal));
                }
            }
        }

        @Override
        public void mediaQueueEmpty() {
            try {
                serverWrite("empty");
            } catch (IOException e) {
                handleCommFailure(e);
            }
        }
    }

    private ServerCommThread serverCommThread;

    public PlayQueueSession(final Handler uiHandler, Context context, String phoneNumber) {
        super(uiHandler, context);
        if (phoneNumber == null) {
            Log.wtf("PlayQueueSession", "phoneNumber is null!");
            displayInternalError("1");
        }
        this.phoneNumber = phoneNumber;
        if (DataStorage.getStorage().getServerAddress() == null) {
            Log.wtf("PlayQueueSession", "getServerAddress() is null!");
            displayInternalError("2");
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        uiHandler.obtainMessage(MESSAGE_SHOW_PROVIDER_INFO,
                                resources.getString(R.string.ksp_network)).sendToTarget();
                        Thread.sleep(1000);
                        uiHandler.obtainMessage(MESSAGE_HIDE_PROVIDER_INFO).sendToTarget();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            serverCommThread = new ServerCommThread();
            new Thread(serverCommThread).start();
        }
    }

    private static class MediaQueue implements MediaPlayer.OnCompletionListener {

        private MediaPlayer mediaPlayer;
        private Context context;
        private ServerCommThread parent;

        interface MediaQueueEmptyCallback {
            public void mediaQueueEmpty();
        }

        private MediaQueueEmptyCallback mqeCallback;

        private Queue<String> media;

        private void play(String str) {
            try {

                if ("shutdown".equals(str)) {
                    parent.killComm();
                }

                int soundID = context.getResources().getIdentifier(str, "raw", "sk.ksp.callcentrum");

                if (soundID == 0) {
                    if (BuildConfig.DEBUG) {
                        Log.w("MediaQueue", "Sound does not exist: " + str + "!");
                    }
                    onCompletion(null);
                    return;
                }

                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnCompletionListener(this);
                } else {
                    mediaPlayer.reset();
                }

                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundID);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                mediaPlayer.prepare();
                if (BuildConfig.DEBUG) {
                    Log.i("MediaQueue", "Playback started: " + str);
                }
                mediaPlayer.start();
                afd.close();
            } catch (IOException e) {
                Log.wtf("MediaQueue", e.toString());
            }
        }

        public MediaQueue(MediaQueueEmptyCallback mqeCallback, Context context, ServerCommThread parent) {
            media = new ConcurrentLinkedQueue<String>();
            this.mqeCallback = mqeCallback;
            this.context = context;
            this.parent = parent;
        }

        public void push(String sound) {
            media.add(sound);
            try {
                if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
                    play(sound);
                    if (!media.isEmpty()) media.remove();
                }
            } catch (IllegalStateException e) {
                play(sound);
                if (!media.isEmpty()) media.remove();
            }
        }

        public void clear() {
            media.clear();
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
            }
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (BuildConfig.DEBUG) {
                Log.i("MediaQueue", "Playback completed");
            }
            if (!media.isEmpty()) {
                play(media.remove());
            } else {
                mqeCallback.mediaQueueEmpty();
            }
        }
    }

    private void displayInternalError(final String error) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uiHandler.obtainMessage(MESSAGE_SHOW_PROVIDER_INFO,
                            resources.getString(R.string.ksp_network)).sendToTarget();
                    uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR,
                            resources.getString(R.string.ksp_internal_phone_error) + " (" + error + ")").sendToTarget();
                    Thread.sleep(6000);
                    uiHandler.obtainMessage(MESSAGE_HIDE_PROVIDER_INFO).sendToTarget();
                    Thread.sleep(600);
                    uiHandler.obtainMessage(MESSAGE_DIE).sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void killCallWithMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, message).sendToTarget();
                    Thread.sleep(3000);
                    uiHandler.obtainMessage(MESSAGE_DIE).sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onButtonClick(int button) {
        switch (button) {
            case R.id.dialpadButton:
                uiHandler.obtainMessage(dialerOpen ? MESSAGE_HIDE_DIALPAD : MESSAGE_SHOW_DIALPAD).sendToTarget();
                dialerOpen = !dialerOpen;
                break;
            case R.id.endButton:
                serverCommThread.killComm();
                break;
        }
    }

    @Override
    public void onDialerClick(char dialerButton) {
        serverCommThread.sendButtonPress(dialerButton);
    }

    @Override
    public void onPhoneShake() {
        if (serverCommThread != null) {
            serverCommThread.sendShakeEvent();
        }
    }
}
