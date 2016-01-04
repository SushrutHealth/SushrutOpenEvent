/**
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.context;

import android.text.TextUtils;

import org.andstatus.app.util.MyLog;

/** How to show a User in a Timeline */
public enum UserInTimeline {
    USERNAME(1),
    AT_USERNAME(2),
    WEBFINGER_ID(3),
    REAL_NAME(4),
    REAL_NAME_AT_USERNAME(5);

    private static final String TAG = UserInTimeline.class.getSimpleName();
    
    private final long code;
    
    UserInTimeline(long code) {
        this.code = code;
    }
    
    public String save() {
        return Long.toString(code);
    }

    /**
     * Returns the enum or {@link #USERNAME}
     */
    public static UserInTimeline load(String strCode) {
        try {
            if (!TextUtils.isEmpty(strCode)) {
                return load(Long.parseLong(strCode));
            }
        } catch (NumberFormatException e) {
            MyLog.v(TAG, "Error converting '" + strCode + "'", e);
        }
        return USERNAME;
    }
    
    public static UserInTimeline load( long code) {
        for(UserInTimeline val : values()) {
            if (val.code == code) {
                return val;
            }
        }
        return USERNAME;
    }
    
}
