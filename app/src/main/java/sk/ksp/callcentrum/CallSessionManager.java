package sk.ksp.callcentrum;

import android.os.Handler;

public abstract class CallSessionManager {

    public static final int BUTTON_HANGUP = 0;
    public static final int BUTTON_DIALPAD = 1;
    public static final int BUTTON_SPEAKER = 2;
    public static final int BUTTON_MUTE = 3;
    public static final int BUTTON_HOLD = 4;
    public static final int BUTTON_CONFERENCE_ADD = 5;
    public static final int BUTTON_DIALPAD_0 = 10;
    public static final int BUTTON_DIALPAD_1 = 11;
    public static final int BUTTON_DIALPAD_2 = 12;
    public static final int BUTTON_DIALPAD_3 = 13;
    public static final int BUTTON_DIALPAD_4 = 14;
    public static final int BUTTON_DIALPAD_5 = 15;
    public static final int BUTTON_DIALPAD_6 = 16;
    public static final int BUTTON_DIALPAD_7 = 17;
    public static final int BUTTON_DIALPAD_8 = 18;
    public static final int BUTTON_DIALPAD_9 = 19;
    public static final int BUTTON_DIALPAD_STAR = 20;
    public static final int BUTTON_DIALPAD_SHARP = 21;

    public static final int MESSAGE_SHOW_IMAGE = 0;
    public static final int MESSAGE_SHOW_NUMBER = 1;
    public static final int MESSAGE_SHOW_NAME = 2;
    public static final int MESSAGE_SHOW_MESSAGEBAR = 3;
    public static final int MESSAGE_HIDE_MESSAGEBAR = 4;
    public static final int MESSAGE_DIE = 5;
    public static final int MESSAGE_UPDATE_TIME = 6;
    public static final int MESSAGE_SHOW_PROVIDER_INFO = 7;
    public static final int MESSAGE_HIDE_PROVIDER_INFO = 8;
    // To annoy the user, codes > 50
    public static final int MESSAGE_SET_RTL = 51;
    public static final int MESSAGE_SET_LTR = 52;

    protected Handler uiHandler;

    public CallSessionManager(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public abstract void onButtonPressed(int button);
    public abstract void onButtonReleased(int button);

}
