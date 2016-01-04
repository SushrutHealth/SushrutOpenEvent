/* 
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

package org.andstatus.app.data;

import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import android.text.TextUtils;

import org.andstatus.app.ClassInApplicationPackage;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.data.MyDatabase.Msg;
import org.andstatus.app.data.MyDatabase.Origin;
import org.andstatus.app.data.MyDatabase.User;

/**
 * Classifier of Uri-s, passed to our content provider
 * @author yvolk@yurivolkov.com
 */
public enum MatchedUri {
    /**
     * This Uri is for some Timeline
     */
    TIMELINE(1),
    TIMELINE_SEARCH(3),
    /**
     * The Timeline URI contains Message id 
     */
    TIMELINE_ITEM(4),
    /**
     * Operations on {@link MyDatabase.Msg} itself
     */
    MSG(7),
    MSG_ITEM(10),
    MSG_COUNT(2),
    ORIGIN(8),
    ORIGIN_ITEM(11),
    /**
     * List of Users
     */
    USERLIST(5),
    /**
     * Operations on {@link MyDatabase.User} itself
     */
    USER(6),
    USER_ITEM(9),
    
    UNKNOWN(0);
    
    /**
     * "Authority", represented by this Content Provider and declared in the application's manifest.
     * (see <a href="http://developer.android.com/guide/topics/manifest/provider-element.html">&lt;provider&gt;</a>)
     * 
     * Note: This is historical constant, remained to preserve compatibility without reinstallation
     */
    public static final String AUTHORITY = ClassInApplicationPackage.PACKAGE_NAME + ".data.MyProvider";

    private static final String COMBINED_SEGMENT = "combined";
    private static final String SEARCH_SEGMENT = "search";
    private static final String LISTTYPE_SEGMENT = "lt";
    private static final String COUNT_SEGMENT = "count";
    private static final String CONTENT_SEGMENT = "content";
    private static final String CONTENT_ITEM_SEGMENT = "item";
    private static final String USER_SEGMENT = "user";

    private static final String CONTENT_URI_PREFIX = "content://" + AUTHORITY + "/";
    public static final Uri MSG_CONTENT_URI = Uri.parse(CONTENT_URI_PREFIX + Msg.TABLE_NAME + "/" + CONTENT_SEGMENT);
    public static final Uri MSG_CONTENT_COUNT_URI = Uri.parse(CONTENT_URI_PREFIX + Msg.TABLE_NAME + "/" + COUNT_SEGMENT);

    private final int code;
    
    private MatchedUri(int codeIn) {
        code = codeIn;
    }
    
    public static MatchedUri fromUri(Uri uri) {
        int codeIn = URI_MATCHER.match(uri);
        for (MatchedUri matched : values()) {
            if (matched.code == codeIn) {
                return matched;
            }
        }
        return UNKNOWN;
    }
    
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        /** 
         * The order of PathSegments (parameters of timelines) in the URI
         * 1. MyAccount USER_ID is the first parameter (this is his timeline of the type specified below!)
         * 2 - 3. LISTTYPE_SEGMENT + actual type
         * 4 - 5. COMBINED_SEGMENT +  0 or 1  (1 for combined timeline) 
         * 6 - 7. MyDatabase.MSG_TABLE_NAME + "/" + MSG_ID  (optional, used to access specific Message)
         */
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/#/" + LISTTYPE_SEGMENT + "/*/" + COMBINED_SEGMENT + "/#/" + USER_SEGMENT + "/#/" + CONTENT_ITEM_SEGMENT + "/#", TIMELINE_ITEM.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/#/" + LISTTYPE_SEGMENT + "/*/" + COMBINED_SEGMENT + "/#/" + USER_SEGMENT + "/#/" + SEARCH_SEGMENT + "/*", TIMELINE_SEARCH.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/#/" + LISTTYPE_SEGMENT + "/*/" + COMBINED_SEGMENT + "/#/" + USER_SEGMENT + "/#/rnd/#", TIMELINE.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/#/" + LISTTYPE_SEGMENT + "/*/" + COMBINED_SEGMENT + "/#/" + USER_SEGMENT + "/#", TIMELINE.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/#/" + CONTENT_ITEM_SEGMENT + "/#", MSG_ITEM.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/" + CONTENT_SEGMENT, MSG.code);
        URI_MATCHER.addURI(AUTHORITY, Msg.TABLE_NAME + "/" + COUNT_SEGMENT, MSG_COUNT.code);

        URI_MATCHER.addURI(AUTHORITY, Origin.TABLE_NAME + "/#/" + CONTENT_ITEM_SEGMENT + "/#", ORIGIN_ITEM.code);
        URI_MATCHER.addURI(AUTHORITY, Origin.TABLE_NAME + "/" + CONTENT_SEGMENT, ORIGIN.code);
        
        URI_MATCHER.addURI(AUTHORITY, User.TABLE_NAME + "/#/" + LISTTYPE_SEGMENT + "/*/" + COMBINED_SEGMENT + "/#", USERLIST.code);
        URI_MATCHER.addURI(AUTHORITY, User.TABLE_NAME + "/#/" + CONTENT_ITEM_SEGMENT + "/#", USER_ITEM.code);
        URI_MATCHER.addURI(AUTHORITY, User.TABLE_NAME + "/#/" + CONTENT_SEGMENT, USER.code);
    }
    
    /**
     *  MIME types should be like in android:mimeType in AndroidManifest.xml 
     */
    private static final String CONTENT_TYPE_PREFIX = "vnd.android.cursor.dir/"
            + ClassInApplicationPackage.PACKAGE_NAME + ".provider.";
    private static final String CONTENT_ITEM_TYPE_PREFIX = "vnd.android.cursor.item/"
            + ClassInApplicationPackage.PACKAGE_NAME + ".provider.";
    /**
     * Implements {@link android.content.ContentProvider#getType(Uri)}
     */
    public String getMimeType() {
        String type = null;
        switch (this) {
            case MSG:
            case TIMELINE:
            case TIMELINE_SEARCH:
            case MSG_COUNT:
                type = CONTENT_TYPE_PREFIX + Msg.TABLE_NAME;
                break;
            case TIMELINE_ITEM:
            case MSG_ITEM:
                type = CONTENT_ITEM_TYPE_PREFIX + Msg.TABLE_NAME;
                break;
            case ORIGIN_ITEM:
                type = CONTENT_ITEM_TYPE_PREFIX + Origin.TABLE_NAME;
                break;
            case USER:
            case USERLIST:
                type = CONTENT_TYPE_PREFIX + User.TABLE_NAME;
                break;
            case USER_ITEM:
                type = CONTENT_ITEM_TYPE_PREFIX + User.TABLE_NAME;
                break;
            default:
                break;
        }
        return type;
    }

    public static Uri getTimelineSearchUri(long accountUserId, TimelineType timelineType, boolean isCombined, long selectedUserId, String searchQuery) {
        Uri uri = getTimelineUri(accountUserId, timelineType, isCombined, selectedUserId);
        if (!TextUtils.isEmpty(searchQuery)) {
            uri = Uri.withAppendedPath(uri, SEARCH_SEGMENT);
            uri = Uri.withAppendedPath(uri, Uri.encode(searchQuery));
        }
        return uri;
    }
    
    /**
     * Uri for the message in the account's timeline
     */
    public static Uri getTimelineItemUri(long accountUserId, TimelineType timelineType, boolean isCombined, long selectedUserId, long msgId) {
        Uri uri = getTimelineUri(accountUserId, timelineType, isCombined, selectedUserId);
        uri = Uri.withAppendedPath(uri,  CONTENT_ITEM_SEGMENT);
        uri = ContentUris.withAppendedId(uri, msgId);
        return uri;
    }

    /**
     * Build a Timeline Uri for this User / {@link MyAccount}
     * @param accountUserId {@link MyDatabase.User#USER_ID}. This user <i>may</i> be an account: {@link MyAccount#getUserId()} 
     * @return
     */
    public static Uri getTimelineUri(long accountUserId, TimelineType timelineType, boolean isTimelineCombined, long selectedUserId) {
        Uri uri = getBaseAccountUri(accountUserId, Msg.TABLE_NAME); 
        uri = Uri.withAppendedPath(uri, LISTTYPE_SEGMENT + "/" + timelineType.save());
        uri = Uri.withAppendedPath(uri, COMBINED_SEGMENT + "/" + (isTimelineCombined ? "1" : "0"));
        uri = Uri.withAppendedPath(uri,  USER_SEGMENT);
        uri = ContentUris.withAppendedId(uri, selectedUserId);
        return uri;
    }

    public static Uri getMsgUri(long accountUserId, long msgId) {
        return getContentItemUri(accountUserId, MyDatabase.Msg.TABLE_NAME, msgId);
    }
    
    public static Uri getUserUri(long accountUserId, long userId) {
        return getContentItemUri(accountUserId, MyDatabase.User.TABLE_NAME, userId);
    }

    public static Uri getOriginUri(long originId) {
        return getContentItemUri(0, MyDatabase.Origin.TABLE_NAME, originId);
    }

    /**
     * @param accountUserId userId of MyAccount or 0 if not needed
     * @param tableName 
     * @param itemId ID or 0 - if the Item doesn't exist
     */
    private static Uri getContentItemUri(long accountUserId, String tableName, long itemId) {
        Uri uri = getBaseAccountUri(accountUserId, tableName); 
        uri = Uri.withAppendedPath(uri, CONTENT_ITEM_SEGMENT);
        uri = ContentUris.withAppendedId(uri, itemId);
        return uri;
    }
    
    private static Uri getBaseAccountUri(long accountUserId, String tableName) {
        return ContentUris.withAppendedId(Uri.parse(CONTENT_URI_PREFIX + tableName), 
                accountUserId);
    }
}