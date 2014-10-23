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
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.ServiceState;
import android.text.method.DialerKeyListener;
import android.util.EventLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import sk.ksp.callcentrum.incall.AnimationUtils;
import sk.ksp.callcentrum.incall.CallCard;
import sk.ksp.callcentrum.incall.DTMFTwelveKeyDialer;
import sk.ksp.callcentrum.incall.InCallTouchUi;


/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity
        implements View.OnClickListener {
    private static final String LOG_TAG = "InCallActivity";

    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /** Main in-call UI elements. */
    private CallCard mCallCard;

    private InCallTouchUi mInCallTouchUi;
    private DTMFTwelveKeyDialer mDialer;


    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate()...  this = " + this);
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);

        initInCallScreen();
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

    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallActivity, since we don't want to
     * get destroyed and then have to be re-created from scratch for the
     * next call.  Instead, we just move ourselves to the back of the
     * activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK
     * button (since moveTaskToBack() puts us behind the Home app, but the
     * home app doesn't allow the BACK key to move you any farther down in
     * the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means
     * that we'll keep a single InCallActivity instance around for the
     * entire uptime of the device.  This noticeably improves the UI
     * responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        if (DBG) log("finish()...");
        moveTaskToBack(true);
    }

    private void initInCallScreen() {
        if (VDBG) log("initInCallScreen()...");

        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        // Initialize the CallCard.
        mCallCard = (CallCard) findViewById(R.id.callCard);
        mCallCard.setInCallScreenInstance(this);

        initInCallTouchUi();

        ViewStub stub = (ViewStub) findViewById(R.id.dtmf_twelve_key_dialer_stub);
        mDialer = new DTMFTwelveKeyDialer(this, stub);
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {

        // TODO Really handle dialer key down

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
        // if (DBG) log("onKeyUp(keycode " + keyCode + ")...");

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

    private void updateScreen() {
        if (DBG) log("updateScreen()...");
        updateInCallTouchUi();
        mCallCard.updateState();

        // Now that we're sure DTMF dialpad is in an appropriate state, reflect
        // the dialpad state into CallCard
        updateCallCardVisibilityPerDialerState(false);
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

    private void onOpenCloseDialpad() {
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
     * @see #updateScreen()
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
