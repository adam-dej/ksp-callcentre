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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
     * Container for both provider info and call state. This will take care of showing/hiding
     * animation for those views.
     */
    private ViewGroup mProviderInfo;
    private TextView mProviderLabel;

    // "Call state" widgets
    private TextView mCallStateLabel;
    private TextView mElapsedTime;

    // The main block of info about the "primary" or "active" call,
    // including photo / name / phone number / etc.
    private ImageView mPhoto;

    private TextView mName;
    private TextView mPhoneNumber;
    private TextView mLabel;

    private Context mContext;

    public CallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        if (DBG) log("CallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);


        float mDensity = getResources().getDisplayMetrics().density;
        if (DBG) log("- Density: " + mDensity);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");

        mProviderInfo = (ViewGroup) findViewById(R.id.providerInfo);
        mProviderLabel = (TextView) findViewById(R.id.providerLabel);
        mCallStateLabel = (TextView) findViewById(R.id.callStateLabel);
        mElapsedTime = (TextView) findViewById(R.id.elapsedTime);

        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView) findViewById(R.id.photo);

        mName = (TextView) findViewById(R.id.name);
        mPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
        mLabel = (TextView) findViewById(R.id.label);

    }

    public void showImage(int imageId) {
        showImage(mPhoto, imageId);
    }

    public void showImage(Bitmap bitmap) {
        showImage(mPhoto, bitmap);
    }

    public void showNumber(String number) {
        updateInfoUi(null, number, null);
    }

    public void showName(String name) {
        updateInfoUi(name, null, null);
    }

    public void showMessagebar(String message) {
        mCallStateLabel.setVisibility(View.VISIBLE);
        mCallStateLabel.setText(message);
    }

    public void hideMessagebar() {
        mCallStateLabel.setVisibility(View.GONE);
    }

    public void updateTime(String time) {
        mElapsedTime.setVisibility(View.VISIBLE);
        mElapsedTime.setText(time);
    }

    public void showProviderInfo(String provider) {
        mProviderInfo.setVisibility(View.VISIBLE);
        mProviderLabel.setText(mContext.getString(R.string.calling_via_template, provider));
    }

    public void hideProviderInfo() {
        mProviderInfo.setVisibility(View.INVISIBLE);
    }

    /**
     * Updates the info portion of the call card with passed in values.
     */
    private void updateInfoUi(String displayName, String displayNumber, String label) {
        if (displayName != null) {
            mName.setText(displayName);
            mName.setVisibility(View.VISIBLE);
        }
        if (displayNumber != null) {
            if (TextUtils.isEmpty(displayNumber)) {
                mPhoneNumber.setVisibility(View.GONE);
            } else {
                mPhoneNumber.setText(displayNumber);
                mPhoneNumber.setVisibility(View.VISIBLE);
            }
        }

        if (label != null) {
            if (TextUtils.isEmpty(label)) {
                mLabel.setVisibility(View.GONE);
            } else {
                mLabel.setText(label);
                mLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    private static void showImage(ImageView view, int resource) {
        showImage(view, view.getContext().getResources().getDrawable(resource));
    }

    private static void showImage(ImageView view, Bitmap bitmap) {
        showImage(view, new BitmapDrawable(view.getContext().getResources(), bitmap));
    }

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

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
