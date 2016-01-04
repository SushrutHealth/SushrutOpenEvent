/*
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.andstatus.app.R;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.UserInTimeline;
import org.andstatus.app.data.MyDatabase;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.util.MyHtml;
import org.andstatus.app.util.MyLog;

public class MessageShare {
    Origin origin;
    private long messageId;
    
    public MessageShare(long messageId) {
        this.messageId = messageId;
        origin = MyContextHolder.get().persistentOrigins().fromId(MyQuery.msgIdToOriginId(messageId));
        if (origin == null) {
            MyLog.v(this, "Origin not found for messageId=" + messageId);
        }
    }

    /**
     * @return true if succeeded
     */
    public boolean share(Context context) {
        if (origin == null) {
            return false;
        }
        context.startActivity(
                Intent.createChooser(intentForShare(), context.getText(R.string.menu_item_share)));
        return true;
    }

    Intent intentForShare() {
        StringBuilder subject = new StringBuilder();
        String msgBody = MyQuery.msgIdToStringColumnValue(MyDatabase.Msg.BODY, messageId);
        String msgBodyPlainText = msgBody;
        if (origin.isHtmlContentAllowed()) {
            msgBodyPlainText = MyHtml.fromHtml(msgBody);
        }

        subject.append(MyContextHolder.get().context()
                .getText(origin.alternativeTermForResourceId(R.string.message)));
        subject.append(" - " + msgBodyPlainText);
        int maxlength = 80;
        if (subject.length() > maxlength) {
            subject.setLength(maxlength);
            // Truncate at the last space
            subject.setLength(subject.lastIndexOf(" "));
            subject.append("...");
        }

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
        intent.putExtra(Intent.EXTRA_TEXT, buildBody(origin, msgBodyPlainText, false));
        if (origin.isHtmlContentAllowed() && MyHtml.hasHtmlMarkup(msgBody) ) {
            intent.putExtra(Intent.EXTRA_HTML_TEXT, buildBody(origin, msgBody, true));
        }
        return intent;
    }

    private static String SIGNATURE_FORMAT_HTML = "<p>-- <br />\n%s<br />\nURL: %s</p>";
    private static String SIGNATURE_PLAIN_TEXT = "\n-- \n%s\n URL: %s";

    private String buildBody(Origin origin, String msgBodyText, boolean html) {
        return new StringBuilder()
                .append(msgBodyText)
                .append(
                        String.format(
                                html ? SIGNATURE_FORMAT_HTML
                                        : SIGNATURE_PLAIN_TEXT,
                                MyQuery.msgIdToUsername(
                                        MyDatabase.Msg.AUTHOR_ID,
                                        messageId,
                                        origin.isMentionAsWebFingerId() ? UserInTimeline.WEBFINGER_ID
                                                : UserInTimeline.USERNAME),
                                origin.messagePermalink(messageId)
                                )).toString();
    }

    /**
     * @return true if succeeded
     */
    public boolean openPermalink(Context context) {
        if (origin == null) {
            return false;
        }
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        String permalinkString = origin.messagePermalink(messageId);
        if (TextUtils.isEmpty(permalinkString)) {
            return false;
        } else {
            intent.setData(Uri.parse(permalinkString));
            context.startActivity(intent);
            return true;
        }
    }
    
}
