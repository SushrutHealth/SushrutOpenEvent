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

package org.andstatus.app.net.social;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;

import org.andstatus.app.account.AccountDataReaderEmpty;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.MatchedUri;
import org.andstatus.app.data.MyDatabase;
import org.andstatus.app.data.MyDatabase.Msg;
import org.andstatus.app.data.MyDatabase.OidEnum;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.net.http.HttpConnectionMock;
import org.andstatus.app.net.http.OAuthClientKeys;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.origin.OriginConnectionData;
import org.andstatus.app.origin.OriginType;
import org.andstatus.app.util.RawResourceUtils;
import org.andstatus.app.util.SelectionAndArgs;
import org.andstatus.app.util.TriState;
import org.andstatus.app.util.UriUtils;
import org.andstatus.app.util.UrlUtils;

import java.io.IOException;

public class VerifyCredentialsTest extends InstrumentationTestCase {
    Context context;
    Connection connection;
    HttpConnectionMock httpConnection;
    OriginConnectionData connectionData;

    String keyStored;
    String secretStored;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = TestSuite.initialize(this);

        Origin origin = MyContextHolder.get().persistentOrigins().firstOfType(OriginType.TWITTER);
        connectionData = origin.getConnectionData(TriState.UNKNOWN);
        connectionData.setDataReader(new AccountDataReaderEmpty());
        connection = connectionData.getConnectionClass().newInstance();
        connection.enrichConnectionData(connectionData);
        connectionData.setHttpConnectionClass(HttpConnectionMock.class);
        connection.setAccountData(connectionData);
        httpConnection = (HttpConnectionMock) connection.http;

        httpConnection.data.originUrl = UrlUtils.fromString("https://twitter.com");
        httpConnection.data.oauthClientKeys = OAuthClientKeys.fromConnectionData(httpConnection.data);
        keyStored = httpConnection.data.oauthClientKeys.getConsumerKey();
        secretStored = httpConnection.data.oauthClientKeys.getConsumerSecret();

        if (!httpConnection.data.oauthClientKeys.areKeysPresent()) {
            httpConnection.data.oauthClientKeys.setConsumerKeyAndSecret("keyForGetTimelineForTw", "thisIsASecret341232");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (!TextUtils.isEmpty(keyStored)) {
            httpConnection.data.oauthClientKeys.setConsumerKeyAndSecret(keyStored, secretStored);        
        }
    }

    public void testVerifyCredentials() throws IOException {
        String jso = RawResourceUtils.getString(this.getInstrumentation().getContext(), 
                org.andstatus.app.tests.R.raw.verify_credentials_twitter);
        httpConnection.setResponse(jso);
        
        MbUser mbUser = connection.verifyCredentials();
        assertEquals("User's oid is user oid of this account", TestSuite.TWITTER_TEST_ACCOUNT_USER_OID, mbUser.oid);
        
        Origin origin = MyContextHolder.get().persistentOrigins().firstOfType(OriginType.TWITTER);
        MyAccount.Builder builder = MyAccount.Builder.newOrExistingFromAccountName(
                MyContextHolder.get(),
                "/" + origin.getName(), TriState.TRUE);
        builder.onCredentialsVerified(mbUser, null);
        assertTrue("Account is persistent", builder.isPersistent());
        long userId = builder.getAccount().getUserId();
        assertTrue("Account " + mbUser.getUserName() + " has UserId", userId != 0);
        assertEquals("Account UserOid", builder.getAccount().getUserOid(), mbUser.oid);
        assertEquals("User in the database for id=" + userId, 
                mbUser.oid,
                MyQuery.idToOid(OidEnum.USER_OID, userId, 0));

        String msgOid = "383296535213002752";
        Uri contentUri = MatchedUri.MSG_CONTENT_URI;
        SelectionAndArgs sa = new SelectionAndArgs();
        String sortOrder = MyDatabase.Msg.DEFAULT_SORT_ORDER;
        sa.addSelection(MyDatabase.Msg.SENDER_ID + " = ? AND " 
        + MyDatabase.Msg.MSG_OID + " = ?",
                new String[] {
                        Long.toString(userId), msgOid 
                });
        String[] PROJECTION = new String[] {
            Msg.MSG_ID,
            };
        Cursor cursor = context.getContentResolver().query(contentUri, PROJECTION, sa.selection, sa.selectionArgs, sortOrder);
        assertTrue("Cursor returned", cursor != null);
        assertTrue("Message by " + mbUser.getUserName() + " found", cursor.getCount() > 0);
        cursor.moveToFirst();
        long messageId = cursor.getLong(0);
        cursor.close();

        assertEquals("Message permalink at twitter",
                "https://" + origin.fixUriforPermalink(UriUtils.fromUrl(origin.getUrl())).getHost()
                        + "/"
                        + builder.getAccount().getUsername() + "/status/" + msgOid,
                origin.messagePermalink(messageId));
    }
}
