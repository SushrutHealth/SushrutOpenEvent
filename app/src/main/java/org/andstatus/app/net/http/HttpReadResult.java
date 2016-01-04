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

package org.andstatus.app.net.http;

import android.text.TextUtils;

import org.andstatus.app.net.http.ConnectionException.StatusCode;
import org.andstatus.app.util.I18n;
import org.andstatus.app.util.MyLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class HttpReadResult {
    private final String urlInitial;
    private String urlString = "";
    private URL url;
    boolean authenticate = true;
    private boolean mIsLegacyHttpProtocol = false;
    
    JSONObject formParams = new JSONObject();
    StringBuilder logBuilder =  new StringBuilder();
    Exception e1 = null;
    String strResponse = "";
    final File fileResult;
    String statusLine = "";
    private int intStatusCode = 0;
    private StatusCode statusCode = StatusCode.UNKNOWN;

    boolean redirected = false;

    public HttpReadResult(String urlIn) throws ConnectionException {
        this (urlIn, (File) null);
    }

    public HttpReadResult(String urlIn, File file) throws ConnectionException {
        urlInitial = urlIn;
        fileResult = file;
        setUrl(urlIn);
    }

    public HttpReadResult(String urlIn, JSONObject formParamsIn) throws ConnectionException {
        this(urlIn);
        if (formParamsIn != null) {
            formParams = formParamsIn;
        }
    }

    public final void setUrl(String urlIn) throws ConnectionException {
        if (!TextUtils.isEmpty(urlIn) && !urlString.contentEquals(urlIn)) {
            redirected = !TextUtils.isEmpty(urlString);
            urlString = urlIn;
            try {
                url = new URL(urlIn);
            } catch (MalformedURLException e) {
                throw new ConnectionException("Malformed URL; " + toString(), e);
            }
        }
    }
    
    void setStatusCode(int intStatusCodeIn) {
        intStatusCode = intStatusCodeIn;
        statusCode = ConnectionException.StatusCode.fromResponseCode(intStatusCodeIn);
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }
    
    public String getUrl() {
        return urlString;
    }

    public URL getUrlObj() {
        return url;
    }
    
    void appendToLog(CharSequence chars) {
        if (TextUtils.isEmpty(chars)) {
            return;
        }
        if (logBuilder.length() > 0) {
            logBuilder.append("; ");
        }
        logBuilder.append(chars);
    }
    
    public String logMsg() {
        return logBuilder.toString();
    }
    
    @Override
    public String toString() {
        return logMsg()
                + ((statusCode == StatusCode.OK) || TextUtils.isEmpty(statusLine) 
                        ? "" : "; statusLine:'" + statusLine + "'")
                + (intStatusCode == 0 ? "" : "; statusCode:" + statusCode + " (" + intStatusCode + ")") 
                + "; url:'" + urlString + "'"
                + (isLegacyHttpProtocol() ? "; legacy HTTP" : "")
                + (authenticate ? "; authenticated" : "")
                + (redirected ? "; redirected from:'" + urlInitial + "'" : "")
                + ( hasFormParams() ? "; posted:'" + formParams.toString() + "'" : "")
                + (TextUtils.isEmpty(strResponse) ? "" : "; response:'" + I18n.trimTextAt(strResponse, 40) + "'")
                + (fileResult == null ? "" : "; saved to file");
    }
    
    JSONObject getJsonObject() throws ConnectionException {
        return innerGetJsonObject(strResponse);
    }

    private JSONObject innerGetJsonObject(String strJson) throws ConnectionException {
        String method = "getJsonObject; ";
        JSONObject jso = null;
        try {
            if (TextUtils.isEmpty(strJson)) {
                jso = new JSONObject();
            } else {
                jso = new JSONObject(strJson);
                String error = jso.optString("error");
                if ("Could not authenticate you.".equals(error)) {
                    appendToLog("error:" + error);
                    throw new ConnectionException(toString());
                }
                
            }
        } catch (JSONException e) {
            throw ConnectionException.loggedJsonException(this, method + toString(), e, strJson);
        }
        return jso;
    }

    JSONArray getJsonArray() throws ConnectionException {
        String method = "getJsonArray; ";
        if (TextUtils.isEmpty(strResponse)) {
            MyLog.v(this, method + "; response is empty");
            return new JSONArray();
        }
        JSONTokener jst = new JSONTokener(strResponse);
        JSONArray jsa = null;
        try {
            Object obj = jst.nextValue();
            if (JSONObject.class.isInstance(obj)) {
                JSONObject jso = (JSONObject) obj;
                if (jso.has("items")) {
                    try {
                        obj = jso.getJSONArray("items");
                    } catch (JSONException e) {
                        throw ConnectionException.loggedJsonException(this, "'items' is not an array?!" 
                                + method + toString(), e, jso);
                    }
                } else {
                    Iterator<String> iterator =  jso.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        Object obj2 = jso.get(key);                    
                        if (JSONArray.class.isInstance(obj2)) {
                            MyLog.v(this, method + "; found array inside '" + key + "' object");
                            obj = obj2;
                            break;
                        }
                    }
                }
            }
            jsa = (JSONArray) obj;
        } catch (JSONException e) {
            throw ConnectionException.loggedJsonException(this, method + toString(), e, strResponse);
        } catch (ClassCastException e) {
            throw ConnectionException.loggedHardJsonException(this, method + toString(), e, strResponse);
        }
        return jsa;
    }
 
    public ConnectionException getExceptionFromJsonErrorResponse() {
        StatusCode statusCode = this.statusCode;
        ConnectionException ce = null;
        String error = "?";
        try {
            JSONObject jsonError = new JSONObject(strResponse);
            error = jsonError.optString("error", error);
            if (statusCode == StatusCode.UNKNOWN) {
                statusCode = (error.indexOf("not found") < 0 ? StatusCode.UNKNOWN
                        : StatusCode.NOT_FOUND);
            }
            ce = new ConnectionException(statusCode, toString() + "; error='" + error + "'");
        } catch (JSONException e) {
            ce = ConnectionException.fromStatusCodeAndThrowable(statusCode, toString() + "; error='" + error + "'", e);
        }
        return ce;
    }

    public void setException(Exception e) {
        e1 = e;
    }

    public void parseAndThrow() throws ConnectionException {
        if ( isStatusOk()) {
            MyLog.v(this, toString());
        } else {
            if (!TextUtils.isEmpty(strResponse)) {
                throw getExceptionFromJsonErrorResponse();
            } else {
                throw ConnectionException.fromStatusCodeAndThrowable(statusCode, toString(), e1);
            }
        }
    }

    private boolean isStatusOk() {
        return e1 == null 
                && (statusCode == StatusCode.OK || statusCode == StatusCode.UNKNOWN);
    }

    public HttpReadResult setFormParams(JSONObject formParamsIn) {
        if (formParamsIn != null) {
            formParams = formParamsIn;
        }
        return this;
    }

    public boolean hasFormParams() {
        return formParams.length() > 0;
    }
    
    public JSONObject getFormParams() {
        return formParams;
    }
 
    public boolean isLegacyHttpProtocol() {
        return mIsLegacyHttpProtocol;
    }

    public HttpReadResult setLegacyHttpProtocol(boolean mIsLegacyHttpProtocol) {
        this.mIsLegacyHttpProtocol = mIsLegacyHttpProtocol;
        return this;
    }

    public void resetError() {
        e1 = null;
        statusCode = StatusCode.UNKNOWN;
    }
}