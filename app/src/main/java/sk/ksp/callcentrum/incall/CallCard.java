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

package sk.ksp.callcentrum.incall;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import sk.ksp.callcentrum.InCallActivity;
import sk.ksp.callcentrum.R;


/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class CallCard extends LinearLayout {
    private static final String LOG_TAG = "CallCard";
    private static final boolean DBG = true;

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    private InCallActivity mInCallScreen;

    // Top-level subviews of the CallCard
    /** Container for info about the current call(s) */
    private ViewGroup mCallInfoContainer;
    /** Primary "call info" block (the foreground or ringing call) */
    private ViewGroup mPrimaryCallInfo;
    /** "Call banner" for the primary call */
    private ViewGroup mPrimaryCallBanner;
    /** Secondary "call info" block (the background "on hold" call) */
    private ViewStub mSecondaryCallInfo;

    /**
     * Container for both provider info and call state. This will take care of showing/hiding
     * animation for those views.
     */
    private ViewGroup mSecondaryInfoContainer;
    private ViewGroup mProviderInfo;
    private TextView mProviderLabel;
    private TextView mProviderAddress;

    // "Call state" widgets
    private TextView mCallStateLabel;
    private TextView mElapsedTime;

    // Text colors, used for various labels / titles
    private int mTextColorCallTypeSip;

    // The main block of info about the "primary" or "active" call,
    // including photo / name / phone number / etc.
    private ImageView mPhoto;
    private View mPhotoDimEffect;

    private TextView mName;
    private TextView mPhoneNumber;
    private TextView mLabel;
    private TextView mCallTypeLabel;
    // private TextView mSocialStatus;

    /**
     * Uri being used to load contact photo for mPhoto. Will be null when nothing is being loaded,
     * or a photo is already loaded.
     */
    private Uri mLoadingPersonUri;

    // Info about the "secondary" call, which is the "call on hold" when
    // two lines are in use.
    private TextView mSecondaryCallName;
    private ImageView mSecondaryCallPhoto;
    private View mSecondaryCallPhotoDimEffect;

    // Onscreen hint for the incoming call RotarySelector widget.
    private int mIncomingCallWidgetHintTextResId;
    private int mIncomingCallWidgetHintColorResId;

//    private CallTime mCallTime;

    // Track the state for the photo.
//    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

    // Cached DisplayMetrics density.
    private float mDensity;

    private Context mContext;

    public CallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        if (DBG) log("CallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);


        mDensity = getResources().getDisplayMetrics().density;
        if (DBG) log("- Density: " + mDensity);
    }

    public void setInCallScreenInstance(InCallActivity inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");

        mCallInfoContainer = (ViewGroup) findViewById(R.id.call_info_container);
        mPrimaryCallInfo = (ViewGroup) findViewById(R.id.primary_call_info);
        mPrimaryCallBanner = (ViewGroup) findViewById(R.id.primary_call_banner);

        mSecondaryInfoContainer = (ViewGroup) findViewById(R.id.secondary_info_container);
        mProviderInfo = (ViewGroup) findViewById(R.id.providerInfo);
        mProviderLabel = (TextView) findViewById(R.id.providerLabel);
        mProviderAddress = (TextView) findViewById(R.id.providerAddress);
        mCallStateLabel = (TextView) findViewById(R.id.callStateLabel);
        mElapsedTime = (TextView) findViewById(R.id.elapsedTime);

        // Text colors
        mTextColorCallTypeSip = getResources().getColor(R.color.incall_callTypeSip);

        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView) findViewById(R.id.photo);
        mPhotoDimEffect = findViewById(R.id.dim_effect_for_primary_photo);

        mName = (TextView) findViewById(R.id.name);
        mPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
        mLabel = (TextView) findViewById(R.id.label);
        mCallTypeLabel = (TextView) findViewById(R.id.callTypeLabel);
        // mSocialStatus = (TextView) findViewById(R.id.socialStatus);

        // Secondary info area, for the background ("on hold") call
        mSecondaryCallInfo = (ViewStub) findViewById(R.id.secondary_call_info);
    }

    /**
     * Updates the state of all UI elements on the CallCard, based on the
     * current state of the phone.
     */
    // TODO update state
    public void updateState() {
        if (DBG) log("updateState()...");

        // TODO temporary hardwired state
        updateForegroundCall(0);

//
//        // Update the onscreen UI based on the current state of the phone.
//
//        PhoneConstants.State state = cm.getState();  // IDLE, RINGING, or OFFHOOK
//        Call ringingCall = cm.getFirstActiveRingingCall();
//        Call fgCall = cm.getActiveFgCall();
//        Call bgCall = cm.getFirstActiveBgCall();
//
//        // Update the overall layout of the onscreen elements, if in PORTRAIT.
//        // Portrait uses a programatically altered layout, whereas landscape uses layout xml's.
//        // Landscape view has the views side by side, so no shifting of the picture is needed
//        if (!PhoneUtils.isLandscape(this.getContext())) {
//            updateCallInfoLayout(state);
//        }
//
//        // If the FG call is dialing/alerting, we should display for that call
//        // and ignore the ringing call. This case happens when the telephony
//        // layer rejects the ringing call while the FG call is dialing/alerting,
//        // but the incoming call *does* briefly exist in the DISCONNECTING or
//        // DISCONNECTED state.
//        if ((ringingCall.getState() != Call.State.IDLE)
//                && !fgCall.getState().isDialing()) {
//            // A phone call is ringing, call waiting *or* being rejected
//            // (ie. another call may also be active as well.)
//            updateRingingCall(cm);
//        } else if ((fgCall.getState() != Call.State.IDLE)
//                || (bgCall.getState() != Call.State.IDLE)) {
//            // We are here because either:
//            // (1) the phone is off hook. At least one call exists that is
//            // dialing, active, or holding, and no calls are ringing or waiting,
//            // or:
//            // (2) the phone is IDLE but a call just ended and it's still in
//            // the DISCONNECTING or DISCONNECTED state. In this case, we want
//            // the main CallCard to display "Hanging up" or "Call ended".
//            // The normal "foreground call" code path handles both cases.
//            updateForegroundCall(cm);
//        } else {
//            // We don't have any DISCONNECTED calls, which means that the phone
//            // is *truly* idle.
//            if (mApplication.inCallUiState.showAlreadyDisconnectedState) {
//                // showAlreadyDisconnectedState implies the phone call is disconnected
//                // and we want to show the disconnected phone call for a moment.
//                //
//                // This happens when a phone call ends while the screen is off,
//                // which means the user had no chance to see the last status of
//                // the call. We'll turn off showAlreadyDisconnectedState flag
//                // and bail out of the in-call screen soon.
//                updateAlreadyDisconnected(cm);
//            } else {
//                // It's very rare to be on the InCallScreen at all in this
//                // state, but it can happen in some cases:
//                // - A stray onPhoneStateChanged() event came in to the
//                //   InCallScreen *after* it was dismissed.
//                // - We're allowed to be on the InCallScreen because
//                //   an MMI or USSD is running, but there's no actual "call"
//                //   to display.
//                // - We're displaying an error dialog to the user
//                //   (explaining why the call failed), so we need to stay on
//                //   the InCallScreen so that the dialog will be visible.
//                //
//                // In these cases, put the callcard into a sane but "blank" state:
//                updateNoCall(cm);
//            }
//        }
    }

    /**
     * Updates the overall size and positioning of mCallInfoContainer and
     * the "Call info" blocks, based on the phone state.
     */
    private void updateCallInfoLayout(int state) {
//        boolean ringing = (state == PhoneConstants.State.RINGING);
//        if (DBG) log("updateCallInfoLayout()...  ringing = " + ringing);
//
//        // Based on the current state, update the overall
//        // CallCard layout:
//
//        // - Update the bottom margin of mCallInfoContainer to make sure
//        //   the call info area won't overlap with the touchable
//        //   controls on the bottom part of the screen.
//
//        int reservedVerticalSpace = mInCallScreen.getInCallTouchUi().getTouchUiHeight();
//        ViewGroup.MarginLayoutParams callInfoLp =
//                (ViewGroup.MarginLayoutParams) mCallInfoContainer.getLayoutParams();
//        callInfoLp.bottomMargin = reservedVerticalSpace;  // Equivalent to setting
//                                                          // android:layout_marginBottom in XML
//        if (DBG) log("  ==> callInfoLp.bottomMargin: " + reservedVerticalSpace);
//        mCallInfoContainer.setLayoutParams(callInfoLp);
    }

    /**
     * Updates the UI for the state where the phone is in use, but not ringing.
     */
    private void updateForegroundCall(int state) {
        if (DBG) log("updateForegroundCall()...");

        // TODO temporary hardwired
        displayMainCallStatus();

//        // if (DBG) PhoneUtils.dumpCallManager();
//
//        Call fgCall = cm.getActiveFgCall();
//        Call bgCall = cm.getFirstActiveBgCall();
//
//        if (fgCall.getState() == Call.State.IDLE) {
//            if (DBG) log("updateForegroundCall: no active call, show holding call");
//            // TODO: make sure this case agrees with the latest UI spec.
//
//            // Display the background call in the main info area of the
//            // CallCard, since there is no foreground call.  Note that
//            // displayMainCallStatus() will notice if the call we passed in is on
//            // hold, and display the "on hold" indication.
//            fgCall = bgCall;
//
//            // And be sure to not display anything in the "on hold" box.
//            bgCall = null;
//        }
//
//        displayMainCallStatus(cm, fgCall);
//
//        Phone phone = fgCall.getPhone();
//
//        int phoneType = phone.getPhoneType();
//        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
//            if ((mApplication.cdmaPhoneCallState.getCurrentCallState()
//                    == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
//                    && mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
//                displaySecondaryCallStatus(cm, fgCall);
//            } else {
//                //This is required so that even if a background call is not present
//                // we need to clean up the background call area.
//                displaySecondaryCallStatus(cm, bgCall);
//            }
//        } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
//                || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
//            displaySecondaryCallStatus(cm, bgCall);
//        }
    }

    /**
     * Updates the UI for the state where an incoming call is just disconnected while we want to
     * show the screen for a moment.
     *
     * This case happens when the whole in-call screen is in background when phone calls are hanged
     * up, which means there's no way to determine which call was the last call finished. Right now
     * this method simply shows the previous primary call status with a photo, closing the
     * secondary call status. In most cases (including conference call or misc call happening in
     * CDMA) this behaves right.
     *
     * If there were two phone calls both of which were hung up but the primary call was the
     * first, this would behave a bit odd (since the first one still appears as the
     * "last disconnected").
     */
    private void updateAlreadyDisconnected(int state) {
        // For the foreground call, we manually set up every component based on previous state.
        mPrimaryCallInfo.setVisibility(View.VISIBLE);
        mSecondaryInfoContainer.setLayoutTransition(null);
        mProviderInfo.setVisibility(View.GONE);
        mCallStateLabel.setVisibility(View.VISIBLE);
//        mCallStateLabel.setText(mContext.getString(R.string.card_title_call_ended));
        mElapsedTime.setVisibility(View.VISIBLE);
//        mCallTime.cancelTimer();

        // Just hide it.
        displaySecondaryCallStatus();
    }

    /**
     * Updates the main block of caller info on the CallCard
     * (ie. the stuff in the primaryCallInfo block) based on the specified Call.
     */
    private void displayMainCallStatus() {

        if (DBG) log("displayMainCallStatus()...");

        mPrimaryCallInfo.setVisibility(View.VISIBLE);
        updateCallStateWidgets();
        updateInfoUi("KSP CallCentrum", "+427 947 427 472", "");
        showImage(mPhoto, R.drawable.picture_unknown);

//
//        if (call == null) {
//            // There's no call to display, presumably because the phone is idle.
//            mPrimaryCallInfo.setVisibility(View.GONE);
//            return;
//        }
//
//        Call.State state = call.getState();
//        if (DBG) log("  - call.state: " + call.getState());
//
//        switch (state) {
//            case ACTIVE:
//            case DISCONNECTING:
//                // update timer field
//                if (DBG) log("displayMainCallStatus: start periodicUpdateTimer");
//                mCallTime.setActiveCallMode(call);
//                mCallTime.reset();
//                mCallTime.periodicUpdateTimer();
//
//                break;
//
//            case HOLDING:
//                // update timer field
//                mCallTime.cancelTimer();
//
//                break;
//
//            case DISCONNECTED:
//                // Stop getting timer ticks from this call
//                mCallTime.cancelTimer();
//
//                break;
//
//            case DIALING:
//            case ALERTING:
//                // Stop getting timer ticks from a previous call
//                mCallTime.cancelTimer();
//
//                break;
//
//            case INCOMING:
//            case WAITING:
//                // Stop getting timer ticks from a previous call
//                mCallTime.cancelTimer();
//
//                break;
//
//            case IDLE:
//                // The "main CallCard" should never be trying to display
//                // an idle call!  In updateState(), if the phone is idle,
//                // we call updateNoCall(), which means that we shouldn't
//                // have passed a call into this method at all.
//                Log.w(LOG_TAG, "displayMainCallStatus: IDLE call in the main call card!");
//
//                // (It is possible, though, that we had a valid call which
//                // became idle *after* the check in updateState() but
//                // before we get here...  So continue the best we can,
//                // with whatever (stale) info we can get from the
//                // passed-in Call object.)
//
//                break;
//
//            default:
//                Log.w(LOG_TAG, "displayMainCallStatus: unexpected call state: " + state);
//                break;
//        }
//
//
//        if (PhoneUtils.isConferenceCall(call)) {
//            // Update onscreen info for a conference call.
//            updateDisplayForConference(call);
//        } else {
//            // Update onscreen info for a regular call (which presumably
//            // has only one connection.)
//            Connection conn = null;
//            int phoneType = call.getPhone().getPhoneType();
//            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
//                conn = call.getLatestConnection();
//            } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
//                  || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
//                conn = call.getEarliestConnection();
//            } else {
//                throw new IllegalStateException("Unexpected phone type: " + phoneType);
//            }
//
//            if (conn == null) {
//                if (DBG) log("displayMainCallStatus: connection is null, using default values.");
//                // if the connection is null, we run through the behaviour
//                // we had in the past, which breaks down into trivial steps
//                // with the current implementation of getCallerInfo and
//                // updateDisplayForPerson.
//                CallerInfo info = PhoneUtils.getCallerInfo(getContext(), null /* conn */);
//                updateDisplayForPerson(info, PhoneConstants.PRESENTATION_ALLOWED, false, call,
//                        conn);
//            } else {
//                if (DBG) log("  - CONN: " + conn + ", state = " + conn.getState());
//                int presentation = conn.getNumberPresentation();
//
//                // make sure that we only make a new query when the current
//                // callerinfo differs from what we've been requested to display.
//                boolean runQuery = true;
//                Object o = conn.getUserData();
//                if (o instanceof PhoneUtils.CallerInfoToken) {
//                    runQuery = mPhotoTracker.isDifferentImageRequest(
//                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
//                } else {
//                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
//                }
//
//                // Adding a check to see if the update was caused due to a Phone number update
//                // or CNAP update. If so then we need to start a new query
//                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
//                    Object obj = conn.getUserData();
//                    String updatedNumber = conn.getAddress();
//                    String updatedCnapName = conn.getCnapName();
//                    CallerInfo info = null;
//                    if (obj instanceof PhoneUtils.CallerInfoToken) {
//                        info = ((PhoneUtils.CallerInfoToken) o).currentInfo;
//                    } else if (o instanceof CallerInfo) {
//                        info = (CallerInfo) o;
//                    }
//
//                    if (info != null) {
//                        if (updatedNumber != null && !updatedNumber.equals(info.phoneNumber)) {
//                            if (DBG) log("- displayMainCallStatus: updatedNumber = "
//                                    + updatedNumber);
//                            runQuery = true;
//                        }
//                        if (updatedCnapName != null && !updatedCnapName.equals(info.cnapName)) {
//                            if (DBG) log("- displayMainCallStatus: updatedCnapName = "
//                                    + updatedCnapName);
//                            runQuery = true;
//                        }
//                    }
//                }
//
//                if (runQuery) {
//                    if (DBG) log("- displayMainCallStatus: starting CallerInfo query...");
//                    PhoneUtils.CallerInfoToken info =
//                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, call);
//                    updateDisplayForPerson(info.currentInfo, presentation, !info.isFinal,
//                                           call, conn);
//                } else {
//                    // No need to fire off a new query.  We do still need
//                    // to update the display, though (since we might have
//                    // previously been in the "conference call" state.)
//                    if (DBG) log("- displayMainCallStatus: using data we already have...");
//                    if (o instanceof CallerInfo) {
//                        CallerInfo ci = (CallerInfo) o;
//                        // Update CNAP information if Phone state change occurred
//                        ci.cnapName = conn.getCnapName();
//                        ci.numberPresentation = conn.getNumberPresentation();
//                        ci.namePresentation = conn.getCnapNamePresentation();
//                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
//                                + "CNAP name=" + ci.cnapName
//                                + ", Number/Name Presentation=" + ci.numberPresentation);
//                        if (DBG) log("   ==> Got CallerInfo; updating display: ci = " + ci);
//                        updateDisplayForPerson(ci, presentation, false, call, conn);
//                    } else if (o instanceof PhoneUtils.CallerInfoToken){
//                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
//                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
//                                + "CNAP name=" + ci.cnapName
//                                + ", Number/Name Presentation=" + ci.numberPresentation);
//                        if (DBG) log("   ==> Got CallerInfoToken; updating display: ci = " + ci);
//                        updateDisplayForPerson(ci, presentation, true, call, conn);
//                    } else {
//                        Log.w(LOG_TAG, "displayMainCallStatus: runQuery was false, "
//                              + "but we didn't have a cached CallerInfo object!  o = " + o);
//                        // TODO: any easy way to recover here (given that
//                        // the CallCard is probably displaying stale info
//                        // right now?)  Maybe force the CallCard into the
//                        // "Unknown" state?
//                    }
//                }
//            }
//        }
//
//        // In some states we override the "photo" ImageView to be an
//        // indication of the current state, rather than displaying the
//        // regular photo as set above.
//        updatePhotoForCallState(call);
//
//        // One special feature of the "number" text field: For incoming
//        // calls, while the user is dragging the RotarySelector widget, we
//        // use mPhoneNumber to display a hint like "Rotate to answer".
//        if (mIncomingCallWidgetHintTextResId != 0) {
//            // Display the hint!
//            mPhoneNumber.setText(mIncomingCallWidgetHintTextResId);
//            mPhoneNumber.setTextColor(getResources().getColor(mIncomingCallWidgetHintColorResId));
//            mPhoneNumber.setVisibility(View.VISIBLE);
//            mLabel.setVisibility(View.GONE);
//        }
//        // If we don't have a hint to display, just don't touch
//        // mPhoneNumber and mLabel. (Their text / color / visibility have
//        // already been set correctly, by either updateDisplayForPerson()
//        // or updateDisplayForConference().)
    }

    /**
     * Updates the "call state label" and the elapsed time widget based on the
     * current state of the call.
     */
    private void updateCallStateWidgets() {
        if (DBG) log("updateCallStateWidgets()...");

        // TODO hardwired state

        String callStateLabel = null;
        callStateLabel = mContext.getString(R.string.card_title_dialing);

        mProviderInfo.setVisibility(View.VISIBLE);

        // TODO hardcoded string?!
        mProviderLabel.setText(mContext.getString(R.string.calling_via_template, "KSP Network"));
//        mProviderAddress.setText(inCallUiState.providerAddress);

        mCallStateLabel.setVisibility(View.VISIBLE);
        mCallStateLabel.setText(callStateLabel);

//        final Call.State state = call.getState();
//        final Context context = getContext();
//        final Phone phone = call.getPhone();
//        final int phoneType = phone.getPhoneType();
//
//        String callStateLabel = null;  // Label to display as part of the call banner
//        int bluetoothIconId = 0;  // Icon to display alongside the call state label
//
//        switch (state) {
//            case IDLE:
//                // "Call state" is meaningless in this state.
//                break;
//
//            case ACTIVE:
//                // We normally don't show a "call state label" at all in
//                // this state (but see below for some special cases).
//                break;
//
//            case HOLDING:
//                callStateLabel = context.getString(R.string.card_title_on_hold);
//                break;
//
//            case DIALING:
//            case ALERTING:
//                callStateLabel = context.getString(R.string.card_title_dialing);
//                break;
//
//            case INCOMING:
//            case WAITING:
//                callStateLabel = context.getString(R.string.card_title_incoming_call);
//
//                // Also, display a special icon (alongside the "Incoming call"
//                // label) if there's an incoming call and audio will be routed
//                // to bluetooth when you answer it.
////                if (mApplication.showBluetoothIndication()) {
////                    bluetoothIconId = R.drawable.ic_incoming_call_bluetooth;
////                }
////                break;
//
//            case DISCONNECTING:
//                // While in the DISCONNECTING state we display a "Hanging up"
//                // message in order to make the UI feel more responsive.  (In
//                // GSM it's normal to see a delay of a couple of seconds while
//                // negotiating the disconnect with the network, so the "Hanging
//                // up" state at least lets the user know that we're doing
//                // something.  This state is currently not used with CDMA.)
//                callStateLabel = context.getString(R.string.card_title_hanging_up);
//                break;
//
//            case DISCONNECTED:
//                callStateLabel = getCallFailedString(call);
//                break;
//
//            default:
//                Log.wtf(LOG_TAG, "updateCallStateWidgets: unexpected call state: " + state);
//                break;
//        }
//
//        // Check a couple of other special cases (these are all CDMA-specific).
//
//        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
//            if ((state == Call.State.ACTIVE)
//                && mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
//                // Display "Dialing" while dialing a 3Way call, even
//                // though the foreground call state is actually ACTIVE.
//                callStateLabel = context.getString(R.string.card_title_dialing);
//            } else if (PhoneGlobals.getInstance().notifier.getIsCdmaRedialCall()) {
//                callStateLabel = context.getString(R.string.card_title_redialing);
//            }
//        }
//        if (PhoneUtils.isPhoneInEcm(phone)) {
//            // In emergency callback mode (ECM), use a special label
//            // that shows your own phone number.
//            callStateLabel = getECMCardTitle(context, phone);
//        }
//
//        final InCallUiState inCallUiState = mApplication.inCallUiState;
//        if (DBG) {
//            log("==> callStateLabel: '" + callStateLabel
//                    + "', bluetoothIconId = " + bluetoothIconId
//                    + ", providerInfoVisible = " + inCallUiState.providerInfoVisible);
//        }
//
//        // Animation will be done by mCallerDetail's LayoutTransition, but in some cases, we don't
//        // want that.
//        // - DIALING: This is at the beginning of the phone call.
//        // - DISCONNECTING, DISCONNECTED: Screen will disappear soon; we have no time for animation.
//        final boolean skipAnimation = (state == Call.State.DIALING
//                || state == Call.State.DISCONNECTING
//                || state == Call.State.DISCONNECTED);
//        LayoutTransition layoutTransition = null;
//        if (skipAnimation) {
//            // Evict LayoutTransition object to skip animation.
//            layoutTransition = mSecondaryInfoContainer.getLayoutTransition();
//            mSecondaryInfoContainer.setLayoutTransition(null);
//        }
//
//        if (inCallUiState.providerInfoVisible) {
//            mProviderInfo.setVisibility(View.VISIBLE);
//            mProviderLabel.setText(context.getString(R.string.calling_via_template,
//                    inCallUiState.providerLabel));
//            mProviderAddress.setText(inCallUiState.providerAddress);
//
//            mInCallScreen.requestRemoveProviderInfoWithDelay();
//        } else {
//            mProviderInfo.setVisibility(View.GONE);
//        }
//
//        if (!TextUtils.isEmpty(callStateLabel)) {
//            mCallStateLabel.setVisibility(View.VISIBLE);
//            mCallStateLabel.setText(callStateLabel);
//
//            // ...and display the icon too if necessary.
//            if (bluetoothIconId != 0) {
//                mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(bluetoothIconId, 0, 0, 0);
//                mCallStateLabel.setCompoundDrawablePadding((int) (mDensity * 5));
//            } else {
//                // Clear out any icons
//                mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//            }
//        } else {
//            mCallStateLabel.setVisibility(View.GONE);
//            // Gravity is aligned left when receiving an incoming call in landscape.
//            // In that rare case, the gravity needs to be reset to the right.
//            // Also, setText("") is used since there is a delay in making the view GONE,
//            // so the user will otherwise see the text jump to the right side before disappearing.
//            if(mCallStateLabel.getGravity() != Gravity.END) {
//                mCallStateLabel.setText("");
//                mCallStateLabel.setGravity(Gravity.END);
//            }
//        }
//        if (skipAnimation) {
//            // Restore LayoutTransition object to recover animation.
//            mSecondaryInfoContainer.setLayoutTransition(layoutTransition);
//        }
//
//        // ...and update the elapsed time widget too.
//        switch (state) {
//            case ACTIVE:
//            case DISCONNECTING:
//                // Show the time with fade-in animation.
//                AnimationUtils.Fade.show(mElapsedTime);
//                updateElapsedTimeWidget(call);
//                break;
//
//            case DISCONNECTED:
//                // In the "Call ended" state, leave the mElapsedTime widget
//                // visible, but don't touch it (so we continue to see the
//                // elapsed time of the call that just ended.)
//                // Check visibility to keep possible fade-in animation.
//                if (mElapsedTime.getVisibility() != View.VISIBLE) {
//                    mElapsedTime.setVisibility(View.VISIBLE);
//                }
//                break;
//
//            default:
//                // Call state here is IDLE, ACTIVE, HOLDING, DIALING, ALERTING,
//                // INCOMING, or WAITING.
//                // In all of these states, the "elapsed time" is meaningless, so
//                // don't show it.
//                AnimationUtils.Fade.hide(mElapsedTime, View.INVISIBLE);
//
//                // Additionally, in call states that can only occur at the start
//                // of a call, reset the elapsed time to be sure we won't display
//                // stale info later (like if we somehow go straight from DIALING
//                // or ALERTING to DISCONNECTED, which can actually happen in
//                // some failure cases like "line busy").
//                if ((state ==  Call.State.DIALING) || (state == Call.State.ALERTING)) {
//                    updateElapsedTimeWidget(0);
//                }
//
//                break;
//        }
    }

    /**
     * Updates the "on hold" box in the "other call" info area
     * (ie. the stuff in the secondaryCallInfo block)
     * based on the specified Call.
     * Or, clear out the "on hold" box if the specified call
     * is null or idle.
     */
    private void displaySecondaryCallStatus() {
        if (DBG) log("displayOnHoldCallStatus()...");
//
//        if ((call == null) || (PhoneGlobals.getInstance().isOtaCallInActiveState())) {
//            mSecondaryCallInfo.setVisibility(View.GONE);
//            return;
//        }
//
//        Call.State state = call.getState();
//        switch (state) {
//            case HOLDING:
//                // Ok, there actually is a background call on hold.
//                // Display the "on hold" box.
//
//                // Note this case occurs only on GSM devices.  (On CDMA,
//                // the "call on hold" is actually the 2nd connection of
//                // that ACTIVE call; see the ACTIVE case below.)
//                showSecondaryCallInfo();
//
//                if (PhoneUtils.isConferenceCall(call)) {
//                    if (DBG) log("==> conference call.");
//                    mSecondaryCallName.setText(getContext().getString(R.string.confCall));
//                    showImage(mSecondaryCallPhoto, R.drawable.picture_conference);
//                } else {
//                    // perform query and update the name temporarily
//                    // make sure we hand the textview we want updated to the
//                    // callback function.
//                    if (DBG) log("==> NOT a conf call; call startGetCallerInfo...");
//                    PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
//                            getContext(), call, this, mSecondaryCallName);
//                    mSecondaryCallName.setText(
//                            PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
//                                                                    getContext()));
//
//                    // Also pull the photo out of the current CallerInfo.
//                    // (Note we assume we already have a valid photo at
//                    // this point, since *presumably* the caller-id query
//                    // was already run at some point *before* this call
//                    // got put on hold.  If there's no cached photo, just
//                    // fall back to the default "unknown" image.)
//                    if (infoToken.isFinal) {
//                        showCachedImage(mSecondaryCallPhoto, infoToken.currentInfo);
//                    } else {
//                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                    }
//                }
//
//                AnimationUtils.Fade.show(mSecondaryCallPhotoDimEffect);
//                break;
//
//            case ACTIVE:
//                // CDMA: This is because in CDMA when the user originates the second call,
//                // although the Foreground call state is still ACTIVE in reality the network
//                // put the first call on hold.
//                if (mApplication.phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
//                    showSecondaryCallInfo();
//
//                    List<Connection> connections = call.getConnections();
//                    if (connections.size() > 2) {
//                        // This means that current Mobile Originated call is the not the first 3-Way
//                        // call the user is making, which in turn tells the PhoneGlobals that we no
//                        // longer know which previous caller/party had dropped out before the user
//                        // made this call.
//                        mSecondaryCallName.setText(
//                                getContext().getString(R.string.card_title_in_call));
//                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                    } else {
//                        // This means that the current Mobile Originated call IS the first 3-Way
//                        // and hence we display the first callers/party's info here.
//                        Connection conn = call.getEarliestConnection();
//                        PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
//                                getContext(), conn, this, mSecondaryCallName);
//
//                        // Get the compactName to be displayed, but then check that against
//                        // the number presentation value for the call. If it's not an allowed
//                        // presentation, then display the appropriate presentation string instead.
//                        CallerInfo info = infoToken.currentInfo;
//
//                        String name = PhoneUtils.getCompactNameFromCallerInfo(info, getContext());
//                        boolean forceGenericPhoto = false;
//                        if (info != null && info.numberPresentation !=
//                                PhoneConstants.PRESENTATION_ALLOWED) {
//                            name = PhoneUtils.getPresentationString(
//                                    getContext(), info.numberPresentation);
//                            forceGenericPhoto = true;
//                        }
//                        mSecondaryCallName.setText(name);
//
//                        // Also pull the photo out of the current CallerInfo.
//                        // (Note we assume we already have a valid photo at
//                        // this point, since *presumably* the caller-id query
//                        // was already run at some point *before* this call
//                        // got put on hold.  If there's no cached photo, just
//                        // fall back to the default "unknown" image.)
//                        if (!forceGenericPhoto && infoToken.isFinal) {
//                            showCachedImage(mSecondaryCallPhoto, info);
//                        } else {
//                            showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                        }
//                    }
//                } else {
//                    // We shouldn't ever get here at all for non-CDMA devices.
//                    Log.w(LOG_TAG, "displayOnHoldCallStatus: ACTIVE state on non-CDMA device");
//                    mSecondaryCallInfo.setVisibility(View.GONE);
//                }
//
//                AnimationUtils.Fade.hide(mSecondaryCallPhotoDimEffect, View.GONE);
//                break;
//
//            default:
//                // There's actually no call on hold.  (Presumably this call's
//                // state is IDLE, since any other state is meaningless for the
//                // background call.)
//                mSecondaryCallInfo.setVisibility(View.GONE);
//                break;
//        }
    }

    private void showSecondaryCallInfo() {
        mSecondaryCallInfo.setVisibility(View.VISIBLE);
        if (mSecondaryCallName == null) {
            mSecondaryCallName = (TextView) findViewById(R.id.secondaryCallName);
        }
        if (mSecondaryCallPhoto == null) {
            mSecondaryCallPhoto = (ImageView) findViewById(R.id.secondaryCallPhoto);
        }
        if (mSecondaryCallPhotoDimEffect == null) {
            mSecondaryCallPhotoDimEffect = findViewById(R.id.dim_effect_for_secondary_photo);
            mSecondaryCallPhotoDimEffect.setOnClickListener(mInCallScreen);
        }
    }

    /**
     * Updates the info portion of the UI to be generic.  Used for CDMA 3-way calls.
     */
    private void updateGenericInfoUi() {
        mName.setText(R.string.card_title_in_call);
        mPhoneNumber.setVisibility(View.GONE);
        mLabel.setVisibility(View.GONE);
    }

    /**
     * Updates the info portion of the call card with passed in values.
     */
    private void updateInfoUi(String displayName, String displayNumber, String label) {
        mName.setText(displayName);
        mName.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(displayNumber)) {
            mPhoneNumber.setVisibility(View.GONE);
            // We have a real phone number as "mName" so make it always LTR
//            mName.setTextDirection(View.TEXT_DIRECTION_LTR);
        } else {
            mPhoneNumber.setText(displayNumber);
            mPhoneNumber.setVisibility(View.VISIBLE);
            // We have a real phone number as "mPhoneNumber" so make it always LTR
//            mPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }

        if (TextUtils.isEmpty(label)) {
            mLabel.setVisibility(View.GONE);
        } else {
            mLabel.setText(label);
            mLabel.setVisibility(View.VISIBLE);
        }
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static void showImage(ImageView view, int resource) {
        showImage(view, view.getContext().getResources().getDrawable(resource));
    }

    private static void showImage(ImageView view, Bitmap bitmap) {
        showImage(view, new BitmapDrawable(view.getContext().getResources(), bitmap));
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static void showImage(ImageView view, Drawable drawable) {
        Resources res = view.getContext().getResources();
        Drawable current = (Drawable) view.getTag();

        if (current == null) {
            if (DBG) log("Start fade-in animation for " + view);
            view.setImageDrawable(drawable);
            AnimationUtils.Fade.show(view);
            view.setTag(drawable);
        } else {
            AnimationUtils.startCrossFade(view, current, drawable);
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides the top-level UI elements of the call card:  The "main
     * call card" element representing the current active or ringing call,
     * and also the info areas for "ongoing" or "on hold" calls in some
     * states.
     *
     * This is intended to be used in special states where the normal
     * in-call UI is totally replaced by some other UI, like OTA mode on a
     * CDMA device.
     *
     * To bring back the regular CallCard UI, just re-run the normal
     * updateState() call sequence.
     */
    public void hideCallCardElements() {
        mPrimaryCallInfo.setVisibility(View.GONE);
        mSecondaryCallInfo.setVisibility(View.GONE);
    }

    /*
     * Updates the hint (like "Rotate to answer") that we display while
     * the user is dragging the incoming call RotarySelector widget.
     */
    /* package */ void setIncomingCallWidgetHint(int hintTextResId, int hintColorResId) {
        mIncomingCallWidgetHintTextResId = hintTextResId;
        mIncomingCallWidgetHintColorResId = hintColorResId;
    }

    // Accessibility event support.
    // Since none of the CallCard elements are focusable, we need to manually
    // fill in the AccessibilityEvent here (so that the name / number / etc will
    // get pronounced by a screen reader, for example.)
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchPopulateAccessibilityEvent(event, mName);
            dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
            return true;
        }

        dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
        dispatchPopulateAccessibilityEvent(event, mPhoto);
        dispatchPopulateAccessibilityEvent(event, mName);
        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
        dispatchPopulateAccessibilityEvent(event, mLabel);
        // dispatchPopulateAccessibilityEvent(event, mSocialStatus);
        if (mSecondaryCallName != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryCallName);
        }
        if (mSecondaryCallPhoto != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryCallPhoto);
        }
        return true;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }

    public void clear() {

        // Other elements can also be cleared here.  Starting with elapsed time to fix a bug.
        mElapsedTime.setVisibility(View.GONE);
        mElapsedTime.setText(null);
    }
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
