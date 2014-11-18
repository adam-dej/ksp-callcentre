package sk.ksp.callcentrum.sessions;

import android.os.Handler;
import android.util.Log;

import sk.ksp.callcentrum.CallSessionManager;
import sk.ksp.callcentrum.DataStorage;

public class PlayQueueSession extends CallSessionManager {

    private String phoneNumber;

    public PlayQueueSession(Handler uiHandler, String phoneNumber) {
        super(uiHandler);
        if (phoneNumber == null) {
            Log.wtf("PlayQueueSession", "phoneNumber is null!");
            displayInternalError("1");
        }
        this.phoneNumber = phoneNumber;
        if (DataStorage.getStorage().getServerAddress() == null) {
            Log.wtf("PlayQueueSession", "getServerAddress() is null!");
            displayInternalError("2");
        } else {
            displayInternalError("47");
        }
    }

    private void displayInternalError(final String error) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uiHandler.obtainMessage(MESSAGE_SHOW_PROVIDER_INFO, "KSP Network").sendToTarget();
                    uiHandler.obtainMessage(MESSAGE_SHOW_MESSAGEBAR, "Phone error " +
                            error + "! Kontaktujte vedúcich!").sendToTarget();
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

    @Override
    public void onButtonClick(int button) {

    }

    @Override
    public void onDialerClick(char dialerButton) {

    }
}
