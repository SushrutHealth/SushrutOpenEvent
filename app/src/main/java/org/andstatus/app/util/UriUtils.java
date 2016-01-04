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

package org.andstatus.app.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import java.net.URL;

public class UriUtils {
    
    private UriUtils() {
        // Empty
    }

    /**
     * @return true for null also
     */
    public static boolean isEmpty(Uri uri) {
        return Uri.EMPTY.equals(notNull(uri));
    }

    /** returns not null */
    public static Uri fromString(String strUri) {
        return TextUtils.isEmpty(strUri) ? Uri.EMPTY : Uri.parse(strUri.trim());
    }

    public static Uri notNull(Uri uri) {
        return uri == null ? Uri.EMPTY : uri;
    }

    /** returns not null */
    public static Uri fromUrl(URL url) {
        if (url == null) {
            return Uri.EMPTY;
        } else {
            return fromString(url.toExternalForm());
        }
    }
    
    /** See http://developer.android.com/guide/topics/providers/document-provider.html */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int flagsToTakePersistableUriPermission() {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        }
        return flags;
    }
    
    /** See http://stackoverflow.com/questions/25999886/android-content-provider-uri-doesnt-work-after-reboot */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void takePersistableUriPermission(Context context, Uri uri, int takeFlagsIn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if ((takeFlagsIn & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
            final int takeFlags = takeFlagsIn & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException e) {
                MyLog.i(context,"Exception while taking persistable URI permission for '" + uri + "'", e);
            }
        } else {
            MyLog.i(context,"No persistable URI permission for '" + uri + "'");
        }
    }

    /**
     * @return true for null and EMPTY also
     */
    public static boolean isLocal(Uri uri) {
        switch (notNull(uri).getScheme()) {
            case "http":
            case "https":
                return false;
            default:
                break;
        }
        return true;
    }
}
