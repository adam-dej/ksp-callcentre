package sk.ksp.callcentrum.sessions;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import sk.ksp.callcentrum.CallSessionManager;
import sk.ksp.callcentrum.DataStorage;
import sk.ksp.callcentrum.R;

public class PlayQueueSession extends CallSessionManager {

    private String phoneNumber;

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

    private class ServerCommThread implements Runnable {

        private Socket commSocket;

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
            } catch (UnknownHostException e) {
                Log.e("PlayQueueSession", e.toString());
                killCallWithMessage(resources.getString(R.string.ksp_no_signal));
            } catch (IOException e) {
                Log.e("PlayQueueSession", e.toString());
                killCallWithMessage(resources.getString(R.string.ksp_no_signal));
            }
        }

    }

    private ServerCommThread serverCommThread;

    public PlayQueueSession(final Handler uiHandler, final Resources resources, String phoneNumber) {
        super(uiHandler, resources);
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

    }

    @Override
    public void onDialerClick(char dialerButton) {

    }
}
