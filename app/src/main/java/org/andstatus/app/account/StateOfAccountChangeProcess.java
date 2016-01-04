/* 
 * Copyright (C) 2010-2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.account;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.andstatus.app.IntentExtra;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.origin.OriginType;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.TriState;

/** State of the Account add/change process that we store between activity execution steps
*   It's not proper to persist a Bundle, 
*   see: <a href="http://groups.google.com/group/android-developers/browse_thread/thread/6526fe81d2d56a98">http://groups.google.com/group/android-developers/browse_thread/thread/6526fe81d2d56a98</a>.
*   
*   This class will be close to com.android.email.activity.setup.SetupData
**/
class StateOfAccountChangeProcess {
    private static final String TAG = StateOfAccountChangeProcess.class.getSimpleName();

    /** Stored state of the single object of this class
     * It's static so it generally stays intact between the {@link AccountSettingsActivity}'s instantiations 
     * */
    private static final Bundle STORED_STATE = new Bundle();
    
    private static final String ACCOUNT_ACTION_KEY = "account_action";
    private static final String ACCOUNT_AUTHENTICATOR_RESPONSE_KEY = "account_authenticator_response";
    private static final String ACCOUNT_KEY = "account";
    private static final String ACTION_COMPLETED_KEY = "action_completed";
    private static final String ACTION_SUCCEEDED_KEY = "action_succeeded";
    private static final String REQUEST_TOKEN_KEY = "request_token";
    private static final String REQUEST_SECRET_KEY = "request_secret";
    
    private String accountAction = Intent.ACTION_DEFAULT;
    boolean actionCompleted = true;
    boolean actionSucceeded = true;
    AccountAuthenticatorResponse authenticatorResponse = null;
    MyAccount.Builder builder = null;

    boolean useThisState = false;
    
    /**
     * The state was restored
     */
    boolean restored = false;

    boolean accountShouldBeSelected = false;
    boolean originShouldBeSelected = false;

    private String requestToken = null;
    private String requestSecret = null;
    
    private StateOfAccountChangeProcess() {
    }

    /**
     * Restore state if it was stored earlier or create default new one
     */
    static StateOfAccountChangeProcess fromStoredState() {
        StateOfAccountChangeProcess state = new StateOfAccountChangeProcess();
        state.restored = state.restore();
        return state;
    }

    /**
     * Don't restore previously stored state 
     */
    static StateOfAccountChangeProcess fromIntent(Intent intent) {
        StateOfAccountChangeProcess state = new StateOfAccountChangeProcess();
        state.setAccountAction(intent.getAction());   
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
            // For a usage example see also com.android.email.activity.setup.AccountSettings.onCreate(Bundle)

            // Unparcel Extras!
            state.authenticatorResponse = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (state.authenticatorResponse != null) {
                state.useThisState = true;
            }
            
            // Maybe we received MyAccount name as a parameter?!
            String accountName = extras.getString(IntentExtra.ACCOUNT_NAME.key);
            if (!TextUtils.isEmpty(accountName)) {
                state.builder = MyAccount.Builder.newOrExistingFromAccountName(
                        MyContextHolder.get(),
                        accountName, 
                        TriState.UNKNOWN);
                state.useThisState = state.builder.isPersistent();
            }
        }

        if (state.builder == null && !state.getAccountAction().equals(Intent.ACTION_INSERT)) {
            switch (MyContextHolder.get().persistentAccounts().size()) {
                case 0:
                    state.setAccountAction(Intent.ACTION_INSERT);
                    break;
                case 1:
                    state.builder = MyAccount.Builder.fromMyAccount(
                            MyContextHolder.get(),
                            MyContextHolder.get()
                            .persistentAccounts().getCurrentAccount(), "fromIntent", false);
                    break;
                default:
                    state.accountShouldBeSelected = true;
                    break;
            }
        }
        
        if (state.builder == null) {
            if (state.getAccountAction().equals(Intent.ACTION_INSERT)) {
                Origin origin = MyContextHolder
                        .get()
                        .persistentOrigins()
                        .firstOfType(OriginType.UNKNOWN);
                state.builder = MyAccount.Builder.newOrExistingFromAccountName(
                        MyContextHolder.get(),
                        AccountName.ORIGIN_SEPARATOR + origin.getName(), TriState.UNKNOWN);
                state.originShouldBeSelected = true;
            } else {
                state.builder = MyAccount.Builder.newOrExistingFromAccountName(
                        MyContextHolder.get(),
                        MyContextHolder.get().persistentAccounts().getCurrentAccountName(), TriState.UNKNOWN);
            }
            if (!state.builder.isPersistent()) {
                state.setAccountAction(Intent.ACTION_INSERT);
            } else {
                state.setAccountAction(Intent.ACTION_VIEW);
            }
        } else {
            if (state.builder.isPersistent()) {
                state.setAccountAction(Intent.ACTION_EDIT);
            } else {
                state.setAccountAction(Intent.ACTION_INSERT);
            }
        }
        
        return state;
    }
    
    private void save(Bundle bundle) {
        if (bundle != null) {
            bundle.putString(ACCOUNT_ACTION_KEY, getAccountAction());
            bundle.putBoolean(ACTION_COMPLETED_KEY, actionCompleted);
            bundle.putBoolean(ACTION_SUCCEEDED_KEY, actionSucceeded);
            bundle.putParcelable(ACCOUNT_KEY, builder);
            bundle.putParcelable(ACCOUNT_AUTHENTICATOR_RESPONSE_KEY, authenticatorResponse);
            bundle.putString(REQUEST_TOKEN_KEY, requestToken);
            bundle.putString(REQUEST_SECRET_KEY, requestSecret);
            
            MyLog.v(this, "State saved to Bundle");
        }
    }

    private boolean restore(Bundle bundle) {
        boolean restoredNow = false;
        if (bundle != null
                && bundle.containsKey(ACTION_COMPLETED_KEY)) {
            setAccountAction(bundle.getString(ACCOUNT_ACTION_KEY));
            actionCompleted = bundle.getBoolean(ACTION_COMPLETED_KEY, true);
            actionSucceeded = bundle.getBoolean(ACTION_SUCCEEDED_KEY);
            builder = bundle.getParcelable(ACCOUNT_KEY);
            authenticatorResponse = bundle.getParcelable(ACCOUNT_AUTHENTICATOR_RESPONSE_KEY);
            setRequestTokenWithSecret(bundle.getString(REQUEST_TOKEN_KEY), bundle.getString(REQUEST_SECRET_KEY));
            restoredNow = true;
        }
        this.restored = restoredNow;
        return restoredNow;
    }

    public String getRequestToken() {
        return requestToken;
    }

    public String getRequestSecret() {
        return requestSecret;
    }
    
    /** null means to clear the old values */
    public void setRequestTokenWithSecret(String token,
            String secret) {
        if (TextUtils.isEmpty(token)) {
            requestToken = null;
        } else {
            requestToken = token;
        }
        MyLog.d(TAG, TextUtils.isEmpty(token) ? "Clearing Request Token" : "Saving Request Token: " + token);
        if (TextUtils.isEmpty(secret)) {
            requestSecret = null;
        } else {
            requestSecret = secret;
        }
        MyLog.d(TAG, TextUtils.isEmpty(secret) ? "Clearing Request Secret" : "Saving Request Secret: " + secret);
    }
    
    /**
     * Store the state of the not completed actions in the global static object
     * or forget old state of completed action
     */
    void save() {
        if (actionCompleted) {
            forget();
        } else {
            StateOfAccountChangeProcess.STORED_STATE.clear();
            save(StateOfAccountChangeProcess.STORED_STATE);
        }
    }
    
    boolean restore() {
        return restore(StateOfAccountChangeProcess.STORED_STATE);
    }

    /**
     * Forget stored state
     */
    void forget() {
        authenticatorResponse = null;
        StateOfAccountChangeProcess.STORED_STATE.clear();
    }

    String getAccountAction() {
        return accountAction;
    }

    MyAccount getAccount() {
        return builder.getAccount();
    }
    
    void setAccountAction(String accountAction) {
        if (TextUtils.isEmpty(accountAction)) {
            this.accountAction = Intent.ACTION_DEFAULT;
        } else {
            this.accountAction = accountAction;
        }
    }    
}