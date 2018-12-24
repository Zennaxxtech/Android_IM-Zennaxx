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

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.irmsimapp.BuildConfig;
import com.irmsimapp.R;
import com.irmsimapp.components.CustomCircularProgressView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.annotations.NonNull;

import static android.content.Context.ACTIVITY_SERVICE;

public class Utils {

    public static final String TAG = "Utils";
    private static Dialog dialog;
    private static CustomCircularProgressView ivProgressBar;


    public Utils() {
    }

    public static boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) MyApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static void showToast(String message) {
        Toast.makeText(MyApp.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static void showCustomProgressDialog(Context context, boolean isCancel) {
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        if (isInternetConnected()) {
            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.circuler_progerss_bar_two);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            ivProgressBar = (CustomCircularProgressView) dialog.findViewById(R.id.ivProgressBarTwo);
            ivProgressBar.startAnimation();
            dialog.setCancelable(isCancel);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setDimAmount(0);
            if (!((AppCompatActivity) context).isFinishing()) {
                dialog.show();
            }
        }
    }

    public static void hideCustomProgressDialog() {
        try {
            if (dialog != null && ivProgressBar != null) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isNeedPermission() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    public static File createImageFile() {
        SimpleDateFormat sdfSaveImage = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timeStamp = sdfSaveImage.format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File albumF = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MyApp.getContext().getPackageName());
        albumF.mkdirs();
        return new File(albumF, imageFileName);
    }

    public static File createAudioFile() {
        SimpleDateFormat sdfSaveImage = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timeStamp = sdfSaveImage.format(new Date());
        String imageFileName = "Audio_" + timeStamp + ".mp3";
        File albumF = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MyApp.getContext().getPackageName());
        albumF.mkdirs();
        return new File(albumF, imageFileName);
    }

    public static String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(BuildConfig.AES_API_KEY.getBytes(), "AES");
            byte[] finalIvs = new byte[16];
            int len = BuildConfig.AES_API_IV.getBytes().length > 16 ? 16 : BuildConfig.AES_API_IV.getBytes().length;
            System.arraycopy(BuildConfig.AES_API_IV.getBytes(), 0, finalIvs, 0, len);
            IvParameterSpec ivps = new IvParameterSpec(finalIvs);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivps);
            return Base64.encodeToString(cipher.doFinal(data.getBytes()),
                    Base64.DEFAULT).trim().replaceAll("\n", "").replaceAll("\r", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String decrypt(@NonNull String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(BuildConfig.AES_API_KEY.getBytes(), "AES");
            byte[] finalIvs = new byte[16];
            int len = BuildConfig.AES_API_IV.getBytes().length > 16 ? 16 : BuildConfig.AES_API_IV.getBytes().length;
            System.arraycopy(BuildConfig.AES_API_IV.getBytes(), 0, finalIvs, 0, len);
            IvParameterSpec ivps = new IvParameterSpec(finalIvs);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivps);
            return new String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null)
        {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isForeground(Context context, String myPackage) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }

    public static Uri getPhotosUri(String ImageFilePath,Context context) {
        Bitmap photoBitmap;
        int rotationAngle = 0;
        if (ImageFilePath != null && ImageFilePath.length() > 0) {

            try {
                int mobile_width = 480;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(ImageFilePath, options);
                int outWidth = options.outWidth;
                int ratio = (int) ((((float) outWidth) / mobile_width) + 0.5f);

                if (ratio == 0) {
                    ratio = 1;
                }
                ExifInterface exif = new ExifInterface(ImageFilePath);

                String orientString = exif
                        .getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation = orientString != null ? Integer
                        .parseInt(orientString)
                        : ExifInterface.ORIENTATION_NORMAL;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotationAngle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotationAngle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotationAngle = 270;
                        break;
                    case ExifInterface.ORIENTATION_NORMAL:
                        rotationAngle = 0;
                        break;
                    default:
                        // do with default
                        break;
                }
                AppLog.Log("", "Rotation : " + rotationAngle);

                options.inJustDecodeBounds = false;
                options.inSampleSize = ratio;

                photoBitmap = BitmapFactory.decodeFile(ImageFilePath, options);
                if (photoBitmap != null) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotationAngle,
                            (float) photoBitmap.getWidth() / 2,
                            (float) photoBitmap.getHeight() / 2);
                    photoBitmap = Bitmap.createBitmap(photoBitmap, 0, 0,
                            photoBitmap.getWidth(),
                            photoBitmap.getHeight(), matrix, true);

                    AppLog.Log("RegisterFragment",
                            "Take photo on activity result");
                    String path = MediaStore.Images.Media.insertImage(
                            context.getContentResolver(), photoBitmap,
                            Calendar.getInstance().getTimeInMillis()
                                    + ".jpg", null);

                    return Uri.parse(path);
                }
            } catch (OutOfMemoryError e) {
                AppLog.Log("", "out of Memory");
            } catch (IOException e) {
                AppLog.handleException("", e);
            }
        } else {
            Toast.makeText(
                    context,
                    "Error in photo capturing",
                    Toast.LENGTH_LONG).show();
        }
        return null;
    }

}

