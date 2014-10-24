/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sk.ksp.callcentrum.dialpad.DialpadFragment;

/**
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 */
public class CallCentrumActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CallCentrumActivity";

    private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
    private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
    private static final String KEY_SEARCH_QUERY = "search_query";

    private static final String TAG_DIALPAD_FRAGMENT = "dialpad";

    private static final int ACTIVITY_REQUEST_CODE_VOICE_SEARCH = 1;

    /**
     * Fragment containing the dialpad that slides into view
     */
    private DialpadFragment mDialpadFragment;

    private View mFakeActionBar;
    private View mCallHistoryButton;
    private View mDialpadButton;
    private View mDialButton;

    // Padding view used to shift the fragment frame up when the dialpad is shown so that
    // the contents of the fragment frame continue to exist in a layout of the same height
    private View mFragmentsSpacer;
    private View mFragmentsFrame;

    private boolean mInDialpadSearch;
    private boolean mInRegularSearch;
    private boolean mClearSearchOnPause;

    /**
     * True if the dialpad is only temporarily showing due to being in call
     */
    private boolean mInCallDialpadUp;

    private View mSearchViewContainer;

    // This view points to the Framelayout that houses both the search view and remove view
    // containers.
    private View mSearchAndRemoveViewContainer;
    private View mSearchViewCloseButton;
    private View mVoiceSearchButton;
    private EditText mSearchView;

    private String mSearchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        fixIntent(intent);

        setContentView(R.layout.dialtacts_activity);

        getActionBar().hide();

        // Add the favorites fragment, and the dialpad fragment, but only if savedInstanceState
        // is null. Otherwise the fragment manager takes care of recreating these fragments.
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.dialtacts_container, new DialpadFragment(), TAG_DIALPAD_FRAGMENT)
                    .commit();
        } else {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            mInRegularSearch = savedInstanceState.getBoolean(KEY_IN_REGULAR_SEARCH_UI);
            mInDialpadSearch = savedInstanceState.getBoolean(KEY_IN_DIALPAD_SEARCH_UI);
        }

        mFragmentsFrame = findViewById(R.id.dialtacts_frame);
        mFragmentsSpacer = findViewById(R.id.contact_tile_frame_spacer);

        mSearchAndRemoveViewContainer = findViewById(R.id.search_and_remove_view_container);

        // When the first global layout pass is completed (and mSearchAndRemoveViewContainer has
        // been assigned a valid height), assign that height to mFragmentsSpacer as well.
        mSearchAndRemoveViewContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mSearchAndRemoveViewContainer.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        mFragmentsSpacer.setLayoutParams(
                                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                                        mSearchAndRemoveViewContainer.getHeight()));
                    }
                });

        setupFakeActionBarItems();
        prepareSearchView();

        hideDialpadFragment(false, false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mInCallDialpadUp) {
            hideDialpadFragment(false, true);
            mInCallDialpadUp = false;
        }
        prepareVoiceSearchButton();
    }

    @Override
    protected void onPause() {
        if (mClearSearchOnPause) {
            hideDialpadAndSearchUi();
            mClearSearchOnPause = false;
        }
        super.onPause();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof DialpadFragment) {
            mDialpadFragment = (DialpadFragment) fragment;
            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.hide(mDialpadFragment);
            transaction.commit();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialpad_button:
                // Reset the boolean flag that tracks whether the dialpad was up because
                // we were in call. Regardless of whether it was true before, we want to
                // show the dialpad because the user has explicitly clicked the dialpad
                // button.
                mInCallDialpadUp = false;
                showDialpadFragment(true);
                break;
            case R.id.dial_button:
                Log.d(TAG, "Dial button pressed!");
                if (!mDialpadFragment.isDigitsEmpty()) {
                    Intent intent = new Intent(this, InCallActivity.class);
                    intent.putExtra("EXTRA_NUMBER", mDialpadFragment.getDigits());
                    startActivity(intent);
                }
                break;
            case R.id.call_history_button:
                // possibility for action
                break;
            case R.id.search_close_button:
                // Clear the search field
                if (!TextUtils.isEmpty(mSearchView.getText())) {
                    mDialpadFragment.clearDialpad();
                    mSearchView.setText("");
                }
                break;
            case R.id.voice_search_button:
                try {
                    startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                            ACTIVITY_REQUEST_CODE_VOICE_SEARCH);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(CallCentrumActivity.this, R.string.voice_search_not_available,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default: {
                Log.wtf(TAG, "Unexpected onClick event from " + view);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE_VOICE_SEARCH) {
            if (resultCode == RESULT_OK) {
                final ArrayList<String> matches = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() > 0) {
                    final String match = matches.get(0);
                    mSearchView.setText(match);
                } else {
                    Log.e(TAG, "Voice search - nothing heard");
                }
            } else {
                Log.e(TAG, "Voice search failed");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDialpadFragment(boolean animate) {
        mDialpadFragment.setAdjustTranslationForAnimation(animate);
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(R.anim.slide_in, 0);
        } else {
            mDialpadFragment.setYFraction(0);
        }
        ft.show(mDialpadFragment);
        ft.commit();
        mDialButton.setVisibility(shouldShowOnscreenDialButton() ? View.VISIBLE : View.GONE);
        mDialpadButton.setVisibility(View.GONE);

        setDialButtonEnabled(true);
    }

    public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
        if (mDialpadFragment == null) return;
        if (clearDialpad) {
            mDialpadFragment.clearDialpad();
        }
        if (!mDialpadFragment.isVisible()) return;
        mDialpadFragment.setAdjustTranslationForAnimation(animate);
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(0, R.anim.slide_out);
        }
        ft.hide(mDialpadFragment);
        ft.commit();
        mDialButton.setVisibility(View.GONE);
        mDialpadButton.setVisibility(View.VISIBLE);
        setDialButtonEnabled(false);
    }

    private void prepareSearchView() {
        mSearchViewContainer = findViewById(R.id.search_view_container);
        mSearchViewCloseButton = findViewById(R.id.search_close_button);
        mSearchViewCloseButton.setOnClickListener(this);

        mSearchView = (EditText) findViewById(R.id.search_view);
        mSearchView.setHint(getString(R.string.dialer_hint_find_contact));

        prepareVoiceSearchButton();
    }

    private void prepareVoiceSearchButton() {
        mVoiceSearchButton = findViewById(R.id.voice_search_button);
        final Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (canIntentBeHandled(voiceIntent)) {
            mVoiceSearchButton.setVisibility(View.VISIBLE);
            mVoiceSearchButton.setOnClickListener(this);
        } else {
            mVoiceSearchButton.setVisibility(View.GONE);
        }
    }

    private boolean getInSearchUi() {
        return mInDialpadSearch || mInRegularSearch;
    }

    private void setNotInSearchUi() {
        mInDialpadSearch = false;
        mInRegularSearch = false;
    }

    private void hideDialpadAndSearchUi() {
        mSearchView.setText(null);
        hideDialpadFragment(false, true);
    }

    private void setupFakeActionBarItems() {
        mFakeActionBar = findViewById(R.id.fake_action_bar);

        mCallHistoryButton = findViewById(R.id.call_history_button);
        mCallHistoryButton.setOnClickListener(this);

        mDialButton = findViewById(R.id.dial_button);
        mDialButton.setOnClickListener(this);
        mDialButton.setEnabled(true);

        mDialpadButton = findViewById(R.id.dialpad_button);
        mDialpadButton.setOnClickListener(this);
    }

    private void fixIntent(Intent intent) {
        // This should be cleaned up: the call key used to send an Intent
        // that just said to go to the recent calls list.  It now sends this
        // abstract action, but this class hasn't been rewritten to deal with it.
        if (Intent.ACTION_CALL_BUTTON.equals(intent.getAction())) {
            intent.setDataAndType(Calls.CONTENT_URI, Calls.CONTENT_TYPE);
            intent.putExtra("call_key", true);
            setIntent(intent);
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        setIntent(newIntent);
        fixIntent(newIntent);

        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            hideDialpadFragment(true, false);
        } else if (getInSearchUi()) {
            mSearchView.setText(null);
            mDialpadFragment.clearDialpad();
        } else {
            super.onBackPressed();
        }
    }

    public void setDialButtonEnabled(boolean enabled) {
        if (mDialButton != null) {
            mDialButton.setEnabled(enabled);
        }
    }

    private boolean canIntentBeHandled(Intent intent) {
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.size() > 0;
    }

    private boolean shouldShowOnscreenDialButton() {
        return true;
    }
}
