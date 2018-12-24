package com.irmsimapp.ApiClient;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;

import com.irmsimapp.BuildConfig;
import com.irmsimapp.interfaces.AsyncTaskCompleteListener;
import com.irmsimapp.utils.AppLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class DownloadFileTask extends AsyncTask<String, Integer, String> {
    private PowerManager.WakeLock mWakeLock;
    private final ThreadLocal<Context> context = new ThreadLocal<>();
    private AsyncTaskCompleteListener completeListener;
    private String TAG = "DownloadFileTask ";

    public DownloadFileTask(AsyncTaskCompleteListener completeListener, Context context) {
        this.completeListener = completeListener;
        this.context.set(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        PowerManager pm = (PowerManager) context.get().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        }
        mWakeLock.acquire(2 * 60 * 1000L /*2 minutes*/);

    }

    @Override
    protected String doInBackground(String... sUrl) {
        String fileURL = sUrl[0];
        String fileName = "";
        HttpURLConnection httpConn;
        try {
            URL url = new URL(fileURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setReadTimeout(90 * 1000);
            httpConn.setConnectTimeout(100 * 1000);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String disposition = httpConn.getHeaderField("Content-Disposition");
                if (disposition != null) {
                    String[] strings = disposition.split("=");
                    fileName = strings[1];
                } else {
                    fileName = fileURL.split("\\?")[1].split("&")[0].split("=")[1];
                    AppLog.Log(TAG + "FileName ", fileName);
                }
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + BuildConfig.FLAVOR);
                dir.mkdirs();
                File file = new File(dir, fileName);
                if (file.exists()) {
                    return file.getAbsolutePath();
                }
                InputStream inputStream = httpConn.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(file);

                int bytesRead = -1;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                httpConn.disconnect();
                return file.getAbsolutePath();
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        completeListener.onTaskCompleted(result);
    }
}
