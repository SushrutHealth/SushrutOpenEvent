/* 
 * Copyright (c) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.service;

import org.andstatus.app.data.DataInserter;
import org.andstatus.app.net.http.ConnectionException;

/**
 * Downloads ("loads") different types of Timelines 
 *  (i.e. Tweets and Messages) from the Internet (e.g. from twitter.com server).
 * Then Store them into database using {@link DataInserter}
 * 
 * @author yvolk@yurivolkov.com
 */
abstract class TimelineDownloader {
    private static final String TAG = TimelineDownloader.class.getSimpleName();

    protected CommandExecutionContext execContext;
    
    protected static TimelineDownloader getStrategy(CommandExecutionContext execContext) {
        TimelineDownloader td;
        switch (execContext.getTimelineType()) {
            case FOLLOWING_USER:
                td = new TimelineDownloaderFollowing();
                break;
            case ALL:
            case EVERYTHING:
                throw new IllegalArgumentException(TAG + ": Invalid TimelineType for loadTimeline: " + execContext.getTimelineType());
            default:
                td = new TimelineDownloaderOther();
                break;
        }
        td.execContext = execContext;
        return td;
    }
    
    public abstract void download() throws ConnectionException;
}
