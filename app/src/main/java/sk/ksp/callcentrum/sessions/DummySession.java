package sk.ksp.callcentrum.sessions;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import sk.ksp.callcentrum.CallSessionManager;
import sk.ksp.callcentrum.R;

public class DummySession extends CallSessionManager {

    // TODO do not hardcode strings!

    // UI status
    private boolean dialerOpen = false;

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

    public DummySession(final Handler uiHandler, Resources resources) {
        super(uiHandler, resources);
        timerUpdateRunnable = new TimerUpdateRunnable();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uiHandler.obtainMessage(MESSAGE_SHOW_NUMBER, "+427 942 427 472").sendToTarget();
                    uiHandler.obtainMessage(MESSAGE_SHOW_NAME, "KSP Call Centrum").sendToTarget();
                    uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, "Dialing").sendToTarget();
                    uiHandler.obtainMessage(MESSAGE_SHOW_PROVIDER_INFO, "KSP Network").sendToTarget();
                    Thread.sleep(1000);
                    uiHandler.obtainMessage(MESSAGE_HIDE_PROVIDER_INFO).sendToTarget();
                    Thread.sleep(1500);
                    uiHandler.obtainMessage(MESSAGE_HIDE_MESSAGEBAR).sendToTarget();
                    new Thread(timerUpdateRunnable).start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onButtonClick(int button) {
        Log.d("DummySession", "onButtonClick " + button);
        switch (button) {
            case R.id.dialpadButton:
                uiHandler.obtainMessage(dialerOpen ? MESSAGE_HIDE_DIALPAD : MESSAGE_SHOW_DIALPAD).sendToTarget();
                dialerOpen = !dialerOpen;
                break;
            case R.id.endButton:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, "Hanging up").sendToTarget();
                            timerUpdateRunnable.stopTimer();
                            Thread.sleep(2000);
                            uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, "Call ended").sendToTarget();
                            Thread.sleep(750);
                            uiHandler.obtainMessage(MESSAGE_HIDE_MESSAGEBAR).sendToTarget();
                            Thread.sleep(500);
                            uiHandler.obtainMessage(MESSAGE_DIE).sendToTarget();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
        }
    }

    @Override
    public void onDialerClick(char dialerButton) {
        Log.d("DummySession", "onDialerClick" + dialerButton);
    }
}
