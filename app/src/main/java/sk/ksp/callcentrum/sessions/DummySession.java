package sk.ksp.callcentrum.sessions;

import android.os.Handler;
import android.util.Log;

import sk.ksp.callcentrum.CallSessionManager;

public class DummySession extends CallSessionManager {

    public DummySession(Handler uiHandler) {
        super(uiHandler);
    }

    @Override
    public void onButtonPressed(int button) {
        Log.d("DummySession", "onButtonPressed " + button);
    }

    @Override
    public void onButtonReleased(int button) {
        Log.d("DummySession", "onButtonReleased" + button);
    }
}
