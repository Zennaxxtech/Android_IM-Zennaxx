/*
 * Copyright 2017 ElluminatiInc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irmsimapp.utils;


import android.util.Log;

public class AppLog {

    private static final boolean isDebug = true;

    public static void Log(String tag, String message) {
        if (isDebug) {
            Log.d(tag, message + "");
        }
    }

    public static void Error(String tag, String message) {
        if (isDebug) {
            Log.e(tag, message + "");
        }
    }

    public static void handleException(String tag, Exception e) {
        if (isDebug) {
            if (e != null) {
                Log.e(tag, e.getMessage());
            }
        }
    }
}
