/**
 * Copyright (C) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

import android.app.Notification;
import android.content.Context;

import org.andstatus.app.account.PersistentAccounts;
import org.andstatus.app.data.AssertionData;
import org.andstatus.app.data.MyDatabase;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.net.http.HttpConnection;
import org.andstatus.app.origin.PersistentOrigins;
import org.andstatus.app.service.ConnectionRequired;

import java.util.Locale;

public interface MyContext {
    MyContext newInitialized(Context context, String initializerName);
    MyContext newCreator(Context context, String initializerName);
    boolean initialized();
    boolean isReady();
    Locale getLocale();
    MyContextState state();
    Context context();
    String initializedBy();
    long preferencesChangeTime();
    MyDatabase getDatabase();
    PersistentAccounts persistentAccounts();
    PersistentOrigins persistentOrigins();
    void put(AssertionData data);
    void release();
    boolean isExpired();
    void setExpired();
    boolean isOnline(ConnectionRequired connectionRequired);
    /** Is our application in Foreground now? **/
    boolean isInForeground();
    void setInForeground(boolean inForeground);
    void notify(TimelineType id, Notification notification);
    void clearNotification(TimelineType id);
    
    // For testing
    boolean isTestRun();
    HttpConnection getHttpConnectionMock();
}
