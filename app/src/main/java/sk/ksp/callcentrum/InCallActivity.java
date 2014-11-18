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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import sk.ksp.callcentrum.incall.AnimationUtils;
import sk.ksp.callcentrum.incall.CallCard;
import sk.ksp.callcentrum.incall.DTMFTwelveKeyDialer;
import sk.ksp.callcentrum.incall.InCallTouchUi;
import sk.ksp.callcentrum.sessions.PlayQueueSession;


/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity
        implements Handler.Callback {
    private static final String LOG_TAG = "InCallActivity";

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final boolean VDBG = BuildConfig.DEBUG;

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

        callSessionManager = new PlayQueueSession(new Handler(this), getIntent().getExtras().getString("EXTRA_NUMBER"));

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
        switch (message.what) {
            case CallSessionManager.MESSAGE_SHOW_IMAGE:
                if (message.obj instanceof Integer) {
                    mCallCard.showImage((Integer) message.obj);
                } else if (message.obj instanceof Bitmap) {
                    mCallCard.showImage((Bitmap) message.obj);
                }
                break;
            case CallSessionManager.MESSAGE_SHOW_NUMBER:
                mCallCard.showNumber((String) message.obj);
                break;
            case CallSessionManager.MESSAGE_SHOW_NAME:
                mCallCard.showName((String) message.obj);
                break;
            case CallSessionManager.MESSAGE_SHOW_MESSAGEBAR:
                mCallCard.showMessagebar((String) message.obj);
                break;
            case CallSessionManager.MESSAGE_HIDE_MESSAGEBAR:
                mCallCard.hideMessagebar();
                break;
            case CallSessionManager.MESSAGE_DIE:
                finish();
                break;
            case CallSessionManager.MESSAGE_UPDATE_TIME:
                mCallCard.updateTime((String) message.obj);
                break;
            case CallSessionManager.MESSAGE_SHOW_PROVIDER_INFO:
                mCallCard.showProviderInfo((String) message.obj);
                break;
            case CallSessionManager.MESSAGE_HIDE_PROVIDER_INFO:
                mCallCard.hideProviderInfo();
                break;
            case CallSessionManager.MESSAGE_SHOW_DIALPAD:
                openDialpadInternal(true);
                break;
            case CallSessionManager.MESSAGE_HIDE_DIALPAD:
                closeDialpadInternal(true);
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

        mInCallTouchUi = (InCallTouchUi) findViewById(R.id.inCallTouchUi);
        mInCallTouchUi.setInCallScreenInstance(this);

        ViewStub stub = (ViewStub) findViewById(R.id.dtmf_twelve_key_dialer_stub);
        mDialer = new DTMFTwelveKeyDialer(this, stub);

        mCallCard.showImage(R.drawable.picture_unknown);
    }

    public void handleDialerKeyDown(char dialerKey) {
        Log.d(LOG_TAG, "handleDialerKeyDown: " + dialerKey);

        callSessionManager.onDialerClick(dialerKey);
    }

    @Override
    public void onBackPressed() {

        if (mDialer.isOpened()) {
            closeDialpadInternal(true);  // do the "closing" animation
            return;
        }

        super.onBackPressed();
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

    private void openDialpadInternal(boolean animate) {
        mDialer.openDialer(animate);
    }

    private void closeDialpadInternal(boolean animate) {
        mDialer.closeDialer(animate);
    }

    public void handleOnscreenButtonClick(int id) {
        if (DBG) log("handleOnscreenButtonClick(id " + id + ")...");

        callSessionManager.onButtonClick(id);
//
//        switch (id) {
//            // The other regular (single-tap) buttons used while in-call:
//            case R.id.holdButton:
//                break;
//            case R.id.swapButton:
//                break;
//            case R.id.endButton:
//                break;
//            case R.id.dialpadButton:
//                break;
//            case R.id.muteButton:
//                break;
//            case R.id.addButton:
//                break;
//            case R.id.mergeButton:
//                break;
//
//            default:
//                Log.w(LOG_TAG, "handleOnscreenButtonClick: unexpected ID " + id);
//                break;
//        }

//        updateInCallTouchUi();
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
     * Updates the state of the in-call touch UI.
     */
    private void updateInCallTouchUi() {
        if (mInCallTouchUi != null) {
            mInCallTouchUi.updateState();
        }
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
