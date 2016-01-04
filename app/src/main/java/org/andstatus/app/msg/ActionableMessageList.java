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

package org.andstatus.app.msg;

import android.app.Activity;

import org.andstatus.app.MyBaseListActivity;
import org.andstatus.app.MyListActivity;
import org.andstatus.app.data.TimelineType;

/**
 * Activity should implement this interface in order to use {@link MessageContextMenu} 
 * @author yvolk@yurivolkov.com
 */
public interface ActionableMessageList {
    MyBaseListActivity getActivity();
    MessageEditor getMessageEditor();
    void onMessageEditorVisibilityChange(boolean isVisible);
    long getLinkedUserIdFromCursor(int position);
    long getCurrentMyAccountUserId();
    long getSelectedUserId();
    TimelineType getTimelineType();
    boolean isTimelineCombined();
}
