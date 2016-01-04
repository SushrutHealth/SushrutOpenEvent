/**
 * Copyright (C) 2015 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app;

import android.content.Intent;
import android.net.Uri;

import org.andstatus.app.service.MyService;
import org.andstatus.app.service.MyServiceManager;

public enum MyAction {
    /**
     * This action is used in intent sent to {@link MyService}. 
     * This intent will be received by MyService only if it's initialized
     * (and corresponding broadcast receiver registered). 
     */
    EXECUTE_COMMAND("EXECUTE_COMMAND"),
    /**
     * Broadcast with this action is being sent by {@link MyService} to notify of its state.
     *  Actually {@link MyServiceManager} receives it.
     */
    SERVICE_STATE("SERVICE_STATE"),
    VIEW_CONVERSATION("VIEW_CONVERSATION"),
    VIEW_USERS("VIEW_USERS"),
    UNKNOWN("UNKNOWN");
    
    private final String action;
    
    private MyAction(String actionSuffix) {
        this.action = ClassInApplicationPackage.PACKAGE_NAME + ".action." + actionSuffix;
    }

    public Intent getIntent() {
        return new Intent(getAction());
    }

    public Intent getIntent(Uri uri) {
        return new Intent(getAction(), uri);
    }
    
    public static MyAction fromAction(String action) {
        for (MyAction value : MyAction.values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        return UNKNOWN;
    }
    
    public String getAction() {
        return action;
    }
}
