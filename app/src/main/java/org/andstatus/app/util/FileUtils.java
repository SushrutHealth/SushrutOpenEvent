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

package org.andstatus.app.util;

import android.text.TextUtils;

import org.andstatus.app.data.DbUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    private static final int BUFFER_LENGTH = 4 * 1024;

    private FileUtils() {
        // Empty
    }
    
    public static JSONArray getJSONArray(File file) throws IOException {
        JSONArray jso = null;
        String fileString = utf8File2String(file);
        if (!TextUtils.isEmpty(fileString)) {
            try {
                jso = new JSONArray(fileString);
            } catch (JSONException e) {
                MyLog.v(TAG, e);
                jso = null;
            }
        }
        if (jso == null) {
            jso = new JSONArray();
        }
        return jso;
    }
    
    public static JSONObject getJSONObject(File file) throws IOException {
        JSONObject jso = null;
        String fileString = utf8File2String(file);
        if (!TextUtils.isEmpty(fileString)) {
            try {
                jso = new JSONObject(fileString);
            } catch (JSONException e) {
                MyLog.v(TAG, e);
                jso = null;
            }
        }
        if (jso == null) {
            jso = new JSONObject();
        }
        return jso;
    }

    private static String utf8File2String(File file) throws IOException {
        return new String(getBytes(file), Charset.forName("UTF-8"));
    }

    /** Reads the whole file */
    public static byte[] getBytes(File file) throws IOException {
        if (file != null) {
            return getBytes(new FileInputStream(file));
        }
        return new byte[0];
    }

    /** Read the stream into an array and close the stream **/
    public static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (is != null) {
            byte[] readBuffer = new byte[BUFFER_LENGTH];
            try {
                int read;
                do {
                    read = is.read(readBuffer, 0, readBuffer.length);
                    if(read == -1) {
                        break;
                    }
                    bout.write(readBuffer, 0, read);
                } while(true);
                return bout.toByteArray();
            } finally {
                DbUtils.closeSilently(is);
            }
        }
        return new byte[0];
    }
    
    /** Reads up to 'size' bytes, starting from 'offset' */
    public static byte[] getBytes(File file, int offset, int size) throws IOException {
        if (file != null) {
            InputStream is = new FileInputStream(file);
            byte[] readBuffer = new byte[size];
            try {
                long bytesSkipped = is.skip(offset);
                if (bytesSkipped < offset) {
                    throw new FileNotFoundException("Skipped only " + bytesSkipped 
                            + " of " + offset + " bytes in file='" + file.getAbsolutePath() + "'");
                }
                int bytesRead = is.read(readBuffer, 0, size);
                if (bytesRead == readBuffer.length) {
                    return readBuffer;
                } else if (bytesRead > 0) {
                    return Arrays.copyOf(readBuffer, bytesRead);
                }
            } finally {
                DbUtils.closeSilently(is);
            }
        }
        return new byte[0];
    }

    public static void deleteFilesRecursively(File rootDirectory) {
        if (rootDirectory == null) {
            return;
        }
        MyLog.i(TAG, "On delete all files inside '" + rootDirectory.getAbsolutePath() +"'");
        MyLog.i(TAG, "Deleted files and dirs: " + deleteFilesRecursively(rootDirectory, 1));
    }

    private static long deleteFilesRecursively(File rootDirectory, long level) {
        if (rootDirectory == null) {
            return 0;
        }
        File[] files = rootDirectory.listFiles();
        if (files == null) {
            MyLog.v(TAG, "No files inside " + rootDirectory.getAbsolutePath());
            return 0;
        }
        long nDeleted = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                nDeleted += deleteFilesRecursively(file, level + 1);
                if (level > 1) {
                    nDeleted += deleteAndCountFile(file);
                }
            } else {
                nDeleted += deleteAndCountFile(file);
            }
        }
        return nDeleted;
    }

    private static long deleteAndCountFile(File file) {
        long nDeleted = 0;
        if (file.delete()) {
            nDeleted++;
        } else {
            MyLog.w(TAG, "Couldn't delete " + file.getAbsolutePath());
        }
        return nDeleted;
    }

    /**
     * Accepts null argument
     */
    public static boolean exists(File file) {
        if (file == null) {
            return false;
        }
        return file.exists();
    }

    public static void readStreamToFile(InputStream in, File file) throws IOException {
        if (in == null || file == null) {
            return;
        }
        byte[] buffer = new byte[BUFFER_LENGTH];
        int count;
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            try {
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            } finally {
                DbUtils.closeSilently(out);
            }
        } finally {
            DbUtils.closeSilently(in);
        }
    }

}
