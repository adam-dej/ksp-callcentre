package sk.ksp.callcentrum;

import android.os.Handler;

public abstract class CallSessionManager {

    public static final int MESSAGE_SHOW_IMAGE = 0;
    public static final int MESSAGE_SHOW_NUMBER = 1;
    public static final int MESSAGE_SHOW_NAME = 2;
    public static final int MESSAGE_SHOW_MESSAGEBAR = 3;
    public static final int MESSAGE_HIDE_MESSAGEBAR = 4;
    public static final int MESSAGE_DIE = 5;
    public static final int MESSAGE_UPDATE_TIME = 6;
    public static final int MESSAGE_SHOW_PROVIDER_INFO = 7;
    public static final int MESSAGE_HIDE_PROVIDER_INFO = 8;
    public static final int MESSAGE_SHOW_DIALPAD = 9;
    public static final int MESSAGE_HIDE_DIALPAD = 10;
    // To annoy the user, codes > 50
    public static final int MESSAGE_SET_RTL = 51;
    public static final int MESSAGE_SET_LTR = 52;

    protected Handler uiHandler;

    public CallSessionManager(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public abstract void onButtonClick(int button);

}
