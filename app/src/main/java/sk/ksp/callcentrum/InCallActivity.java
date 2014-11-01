/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.ksp.callcentrum;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import sk.ksp.callcentrum.incall.AnimationUtils;
import sk.ksp.callcentrum.incall.CallCard;
import sk.ksp.callcentrum.incall.DTMFTwelveKeyDialer;
import sk.ksp.callcentrum.incall.InCallTouchUi;
import sk.ksp.callcentrum.sessions.DummySession;


/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity
        implements View.OnClickListener, Handler.Callback {
    private static final String LOG_TAG = "InCallActivity";

    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /** Main in-call UI elements. */
    private CallCard mCallCard;

    private InCallTouchUi mInCallTouchUi;
    private DTMFTwelveKeyDialer mDialer;

    private CallSessionManager callSessionManager;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate()...  this = " + this);
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);

        initInCallScreen();

        // TODO get number from extra and create appropriade call session

        // TODO this does not look right...
        callSessionManager = new DummySession(new Handler(this));

    }

    @Override
    protected void onResume() {
        if (DBG) log("onResume()...");
        super.onResume();

        takeKeyEvents(true);

        if (VDBG) log("onResume() done.");
    }

    @Override
    protected void onPause() {
        if (DBG) log("onPause()...");
        super.onPause();

        mDialer.onDialerKeyUp(null);

        mDialer.stopDialerSession();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.arg1) {
            case CallSessionManager.MESSAGE_SHOW_IMAGE:
                break;
            case CallSessionManager.MESSAGE_SHOW_NUMBER:
                break;
            case CallSessionManager.MESSAGE_SHOW_NAME:
                break;
            case CallSessionManager.MESSAGE_SHOW_MESSAGEBAR:
                break;
            case CallSessionManager.MESSAGE_HIDE_MESSAGEBAR:
                break;
            case CallSessionManager.MESSAGE_DIE:
                finish();
                break;
            case CallSessionManager.MESSAGE_UPDATE_TIME:
                break;
            case CallSessionManager.MESSAGE_SHOW_PROVIDER_INFO:
                break;
            case CallSessionManager.MESSAGE_HIDE_PROVIDER_INFO:
                break;
        }
        return true;
    }

    private void initInCallScreen() {
        if (VDBG) log("initInCallScreen()...");

        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        // Initialize the CallCard.
        mCallCard = (CallCard) findViewById(R.id.callCard);

        initInCallTouchUi();

        ViewStub stub = (ViewStub) findViewById(R.id.dtmf_twelve_key_dialer_stub);
        mDialer = new DTMFTwelveKeyDialer(this, stub);

        // TODO temporary hack
        mCallCard.showImage(R.drawable.picture_unknown);
        mCallCard.showName("KSP CallCentrum");
        mCallCard.showNumber("+427 942 427 472");
        mCallCard.showProviderInfo("KSP Network");
//        mCallCard.hideProviderInfo();
        mCallCard.showMessagebar("Dialing");
        mCallCard.updateTime("00:47");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallCard.hideProviderInfo();
                        }
                    });
                    Thread.sleep(2000);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallCard.hideMessagebar();
                        }
                    });
                    Thread.sleep(3000);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallCard.showMessagebar("Hanging up...");
                        }
                    });
                    Thread.sleep(1000);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallCard.showMessagebar("Call ended...");
                        }
                    });
                    Thread.sleep(1500);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallCard.hideMessagebar();
                        }
                    });
                    Thread.sleep(500);
                    InCallActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {

        // TODO Really handle dialer key dow

        Log.d(LOG_TAG, "handleDialerKeyDown: " + keyCode + "; " + event.toString());

        if (isKeyEventAcceptableDTMF(event)) {
            return mDialer.onDialerKeyDown(event);
        }

        return false;
    }

    @Override
    public void onBackPressed() {

        if (mDialer.isOpened()) {
            closeDialpadInternal(true);  // do the "closing" animation
            return;
        }

        super.onBackPressed();
    }

    boolean isKeyEventAcceptableDTMF (KeyEvent event) {
        return (mDialer != null && mDialer.isKeyEventAcceptable(event));
    }

    /**
     * Overriden to track relevant focus changes.
     *
     * If a key is down and some time later the focus changes, we may
     * NOT recieve the keyup event; logically the keyup event has not
     * occured in this window.  This issue is fixed by treating a focus
     * changed event as an interruption to the keydown, making sure
     * that any code that needs to be run in onKeyUp is ALSO run here.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // the dtmf tones should no longer be played
        if (VDBG) log("onWindowFocusChanged(" + hasFocus + ")...");
        if (!hasFocus && mDialer != null) {
            if (VDBG) log("- onWindowFocusChanged: faking onDialerKeyUp()...");
            mDialer.onDialerKeyUp(null);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (DBG) log("onKeyUp(keycode " + keyCode + "; event " + event.toString() + ")...");

        // push input to the dialer.
        if ((mDialer != null) && (mDialer.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (DBG) log("onKeyDown(keycode " + keyCode + "; event " + event.toString() + ")...");

        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_MUTE:
                // TODO do something else
//                onMuteClick();
                return true;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * View.OnClickListener implementation.
     *
     * This method handles clicks from UI elements that use the
     * InCallActivity itself as their OnClickListener.
     *
     * Note: Currently this method is used only for a few special buttons:
     * - the mButtonManageConferenceDone "Back to call" button
     * - the "dim" effect for the secondary call photo in CallCard as the second "swap" button
     * - other OTASP-specific buttons managed by OtaUtils.java.
     *
     * *Most* in-call controls are handled by the handleOnscreenButtonClick() method, via the
     * InCallTouchUi widget.
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (VDBG) log("onClick(View " + view + ", id " + id + ")...");
    }

    private void onHoldClick() {
    }

    private void onMuteClick() {
    }

    public void onOpenCloseDialpad() {
        if (VDBG) log("onOpenCloseDialpad()...");
        if (mDialer.isOpened()) {
            closeDialpadInternal(true);  // do the "closing" animation
        } else {
            openDialpadInternal(true);  // do the "opening" animation
        }
    }

    private void openDialpadInternal(boolean animate) {
        mDialer.openDialer(animate);
    }

    private void closeDialpadInternal(boolean animate) {
        mDialer.closeDialer(animate);
    }

    /**
     * Handles button clicks from the InCallTouchUi widget.
     */
    // TODO too many layers
    public void handleOnscreenButtonClick(int id) {
        if (DBG) log("handleOnscreenButtonClick(id " + id + ")...");

        switch (id) {
            // The other regular (single-tap) buttons used while in-call:
            case R.id.holdButton:
                // TODO do something else
                onHoldClick();
                break;
            case R.id.swapButton:
                break;
            case R.id.endButton:
                break;
            case R.id.dialpadButton:
                // TODO do something else
//                onOpenCloseDialpad();
                break;
            case R.id.muteButton:
                // TODO do something else
//                onMuteClick();
                break;
            case R.id.addButton:
                break;
            case R.id.mergeButton:
                break;

            default:
                Log.w(LOG_TAG, "handleOnscreenButtonClick: unexpected ID " + id);
                break;
        }

        updateInCallTouchUi();
    }

    /**
     * Updates {@link #mCallCard}'s visibility state per DTMF dialpad visibility. They
     * cannot be shown simultaneously and thus we should reflect DTMF dialpad visibility into
     * another.
     *
     * Note: During OTA calls or users' managing conference calls, we should *not* call this method
     * but manually manage both visibility.
     *
     */
    private void updateCallCardVisibilityPerDialerState(boolean animate) {
        // We need to hide the CallCard while the dialpad is visible.
        if (isDialerOpened()) {
            if (VDBG) {
                log("- updateCallCardVisibilityPerDialerState(animate="
                        + animate + "): dialpad open, hide mCallCard...");
            }
            if (animate) {
                AnimationUtils.Fade.hide(mCallCard, View.GONE);
            } else {
                mCallCard.setVisibility(View.GONE);
            }
        } else {
            // Dialpad is dismissed; bring back the CallCard if it's supposed to be visible.
            if (animate) {
                AnimationUtils.Fade.show(mCallCard);
            }
            mCallCard.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @see DTMFTwelveKeyDialer#isOpened()
     */
    public boolean isDialerOpened() {
        return (mDialer != null && mDialer.isOpened());
    }

    /**
     * Called any time the DTMF dialpad is opened.
     * @see DTMFTwelveKeyDialer#openDialer(boolean)
     */
    public void onDialerOpen(boolean animate) {
        if (DBG) log("onDialerOpen()...");

        // Update the in-call touch UI.
        updateInCallTouchUi();

        // Update CallCard UI, which depends on the dialpad.
        updateCallCardVisibilityPerDialerState(animate);
    }

    /**
     * Called any time the DTMF dialpad is closed.
     * @see DTMFTwelveKeyDialer#closeDialer(boolean)
     */
    public void onDialerClose(boolean animate) {
        if (DBG) log("onDialerClose()...");
        updateInCallTouchUi();
        updateCallCardVisibilityPerDialerState(animate);
    }

    /**
     * Initializes the in-call touch UI on devices that need it.
     */
    private void initInCallTouchUi() {
        if (DBG) log("initInCallTouchUi()...");
        mInCallTouchUi = (InCallTouchUi) findViewById(R.id.inCallTouchUi);
        mInCallTouchUi.setInCallScreenInstance(this);

    }

    /**
     * Updates the state of the in-call touch UI.
     */
    private void updateInCallTouchUi() {
        if (mInCallTouchUi != null) {
            mInCallTouchUi.updateState();
        }
    }

    /**
     * @return the InCallTouchUi widget
     */
    public InCallTouchUi getInCallTouchUi() {
        return mInCallTouchUi;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.dispatchPopulateAccessibilityEvent(event);
        mCallCard.dispatchPopulateAccessibilityEvent(event);
        return true;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
