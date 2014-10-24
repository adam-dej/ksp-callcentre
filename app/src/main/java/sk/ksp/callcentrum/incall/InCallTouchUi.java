/*
 * Copyright (C) 2009 The Android Open Source Project
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

package sk.ksp.callcentrum.incall;

import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import sk.ksp.callcentrum.InCallActivity;
import sk.ksp.callcentrum.R;

/**
 * In-call onscreen touch UI elements, used on some platforms.
 *
 * This widget is a fullscreen overlay, drawn on top of the
 * non-touch-sensitive parts of the in-call UI (i.e. the call card).
 */
public class InCallTouchUi extends FrameLayout
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String LOG_TAG = "InCallTouchUi";
    private static final boolean DBG = true;

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    private InCallActivity mInCallScreen;

    /** UI elements while on a regular call (bottom buttons, DTMF dialpad) */
    private View mInCallControls;

    private ImageButton mAddButton;
    private ImageButton mMergeButton;
    private ImageButton mEndButton;
    private CompoundButton mDialpadButton;
    private CompoundButton mMuteButton;
    private CompoundButton mAudioButton;
    private CompoundButton mHoldButton;
    private ImageButton mSwapButton;
    private View mHoldSwapSpacer;

    public InCallTouchUi(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG) log("InCallTouchUi constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);
    }

    public void setInCallScreenInstance(InCallActivity inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (DBG) log("InCallTouchUi onFinishInflate(this = " + this + ")...");

        // Look up the various UI elements.

        // Container for the UI elements shown while on a regular call.
        mInCallControls = findViewById(R.id.inCallControls);

        // Regular (single-tap) buttons, where we listen for click events:
        // Main cluster of buttons:
        mAddButton = (ImageButton) mInCallControls.findViewById(R.id.addButton);
        mAddButton.setOnClickListener(this);
        mAddButton.setOnLongClickListener(this);
        mMergeButton = (ImageButton) mInCallControls.findViewById(R.id.mergeButton);
        mMergeButton.setOnClickListener(this);
        mMergeButton.setOnLongClickListener(this);
        mEndButton = (ImageButton) mInCallControls.findViewById(R.id.endButton);
        mEndButton.setOnClickListener(this);
        mDialpadButton = (CompoundButton) mInCallControls.findViewById(R.id.dialpadButton);
        mDialpadButton.setOnClickListener(this);
        mDialpadButton.setOnLongClickListener(this);
        mMuteButton = (CompoundButton) mInCallControls.findViewById(R.id.muteButton);
        mMuteButton.setOnClickListener(this);
        mMuteButton.setOnLongClickListener(this);
        mAudioButton = (CompoundButton) mInCallControls.findViewById(R.id.audioButton);
        mAudioButton.setOnClickListener(this);
        mAudioButton.setOnLongClickListener(this);
        mHoldButton = (CompoundButton) mInCallControls.findViewById(R.id.holdButton);
        mHoldButton.setOnClickListener(this);
        mHoldButton.setOnLongClickListener(this);
        mSwapButton = (ImageButton) mInCallControls.findViewById(R.id.swapButton);
        mSwapButton.setOnClickListener(this);
        mSwapButton.setOnLongClickListener(this);
        mHoldSwapSpacer = mInCallControls.findViewById(R.id.holdSwapSpacer);

        // TODO temporary hack, this should not be called from here
        updateState();

    }

    // TODO wire up custom call manager
    public void updateState() {

        updateInCallControls();
        mInCallControls.setVisibility(View.VISIBLE);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (DBG) log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.addButton:
            case R.id.mergeButton:
            case R.id.endButton:
                // TODO temporary
                mInCallScreen.finish();
                break;
            case R.id.dialpadButton:
                // TODO temporary
                mInCallScreen.onOpenCloseDialpad();
                break;
            case R.id.muteButton:
            case R.id.holdButton:
            case R.id.swapButton:
                mInCallScreen.handleOnscreenButtonClick(id);
                break;

            case R.id.audioButton:
                break;

            default:
                Log.w(LOG_TAG, "onClick: unexpected click: View " + view + ", id " + id);
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final int id = view.getId();
        if (DBG) log("onLongClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.addButton:
            case R.id.mergeButton:
            case R.id.dialpadButton:
            case R.id.muteButton:
            case R.id.holdButton:
            case R.id.swapButton:
            case R.id.audioButton: {
                final CharSequence description = view.getContentDescription();
                if (!TextUtils.isEmpty(description)) {
                    // Show description as ActionBar's menu buttons do.
                    // See also ActionMenuItemView#onLongClick() for the original implementation.
                    final Toast cheatSheet =
                            Toast.makeText(view.getContext(), description, Toast.LENGTH_SHORT);
                    cheatSheet.setGravity(
                            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, view.getHeight());
                    cheatSheet.show();
                }
                return true;
            }
            default:
                Log.w(LOG_TAG, "onLongClick() with unexpected View " + view + ". Ignoring it.");
                break;
        }
        return false;
    }

    /**
     * Updates the enabledness and "checked" state of the buttons on the
     * "inCallControls" panel, based on the current telephony state.
     */
    private void updateInCallControls() {

        Log.d(LOG_TAG, "updateInCallControls()");

        // "End call"
        mEndButton.setEnabled(true);

        // "Dialpad": Enabled only when it's OK to use the dialpad in the
        // first place.
        mDialpadButton.setEnabled(true);
        mDialpadButton.setChecked(false); // TODO

        // "Mute"
        mMuteButton.setEnabled(true);
        mMuteButton.setChecked(false); // TODO

        // "Audio"
        mAudioButton.setEnabled(true);
        mAudioButton.setChecked(true); // They cannot change this
        updateAudioButton(); // TODO state

        mHoldButton.setVisibility(View.VISIBLE);
        mHoldButton.setEnabled(true);
        mHoldButton.setChecked(false); // TODO
        mSwapButton.setVisibility(View.GONE);
        mHoldSwapSpacer.setVisibility(View.VISIBLE);

        mMergeButton.setVisibility(GONE);
    }

    /**
     * Updates the onscreen "Audio mode" button based on the current state.
     *
     * - If bluetooth is available, this button's function is to bring up the
     *   "Audio mode" popup (which provides a 3-way choice between earpiece /
     *   speaker / bluetooth).  So it should look like a regular action button,
     *   but should also have the small "more_indicator" triangle that indicates
     *   that a menu will pop up.
     *
     * - If speaker (but not bluetooth) is available, this button should look like
     *   a regular toggle button (and indicate the current speaker state.)
     *
     * - If even speaker isn't available, disable the button entirely.
     */
    private void updateAudioButton() {

        LayerDrawable layers = (LayerDrawable) mAudioButton.getBackground();

        layers.findDrawableByLayerId(R.id.compoundBackgroundItem).setAlpha(0);

        layers.findDrawableByLayerId(R.id.moreIndicatorItem).setAlpha(0);

        layers.findDrawableByLayerId(R.id.bluetoothItem).setAlpha(0);

        layers.findDrawableByLayerId(R.id.handsetItem).setAlpha(0);

        layers.findDrawableByLayerId(R.id.speakerphoneOnItem).setAlpha(255);

        layers.findDrawableByLayerId(R.id.speakerphoneOffItem).setAlpha(0);
    }

    /**
     * @return the amount of vertical space (in pixels) that needs to be
     * reserved for the button cluster at the bottom of the screen.
     * (The CallCard uses this measurement to determine how big
     * the main "contact photo" area can be.)
     *
     * NOTE that this returns the "canonical height" of the main in-call
     * button cluster, which may not match the amount of vertical space
     * actually used.  Specifically:
     *
     *   - If an incoming call is ringing, the button cluster isn't
     *     visible at all.  (And the GlowPadView widget is actually
     *     much taller than the button cluster.)
     *
     *   - If the InCallTouchUi widget's "extra button row" is visible
     *     (in some rare phone states) the button cluster will actually
     *     be slightly taller than the "canonical height".
     *
     * In either of these cases, we allow the bottom edge of the contact
     * photo to be covered up by whatever UI is actually onscreen.
     */
    public int getTouchUiHeight() {
        // Add up the vertical space consumed by the various rows of buttons.
        int height = 0;

        // - The main row of buttons:
        height += (int) getResources().getDimension(R.dimen.in_call_button_height);

        // - The End button:
        height += (int) getResources().getDimension(R.dimen.in_call_end_button_height);

        // - Note we *don't* consider the InCallTouchUi widget's "extra
        //   button row" here.

        //- And an extra bit of margin:
        height += (int) getResources().getDimension(R.dimen.in_call_touch_ui_upper_margin);

        return height;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
