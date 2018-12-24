package com.irmsimapp.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.R;
import com.irmsimapp.components.zoomimage.ZoomImageView;
import com.irmsimapp.database.DataRepository;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.datamodel.BadKeyWordsModel;
import com.irmsimapp.interfaces.XMPPErrorListener;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.xmpp.XMPPConfiguration;
import com.irmsimapp.xmpp.XMPPService;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_SERVICE;

public abstract class BaseActivity extends AppCompatActivity {
    public PreferenceHelper preferenceHelper;
    public String TAG = this.getClass().getSimpleName();
    public SimpleDateFormat sdfSendServer;
    public XMPPErrorListener xmppErrorListener;
    public BadKeyWordsModel model;
    public ArrayList<String> badWords;
    public DataRepository dataRepository;
    private static int sessionDepth = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceHelper = PreferenceHelper.getInstance();
        dataRepository = ((MyApp) getApplication()).getRepository();
        model = ViewModelProviders.of(BaseActivity.this).get(BadKeyWordsModel.class);
        badWords = model.getBadKeyWords().getValue();
        sdfSendServer = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        xmppErrorListener = new XMPPErrorListener() {
            @Override
            public void connectionClosedOnError() {
                closedOnError();
            }

            @Override
            public void connectionClosedOnConflict() {
                closedOnConflict();
            }
        };
        if (XMPPService.XMPPConnection != null) {
            XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
        }
    }

    public void closedOnError() {
    }

    public void closedOnConflict() {
    }

    abstract void setUpToolbar();

    abstract void setUpViewAndClickAction();

    @Override
    protected void onStart() {
        super.onStart();
        sessionDepth++;
    }

    public String filterBadWords(String textMessage) {
        for (String badWord : badWords) {
            textMessage = textMessage.replaceAll("(?i)" + badWord, "*****");
        }
        return textMessage;
    }

    public void openChatImageView(Activity activity, ChatMessagesEntity chatMessagesEntity) {
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_open_image);
        dialog.setCancelable(true);
        Toolbar toolbar = dialog.findViewById(R.id.dialogToolbar);
        TextView tvFilename = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbar.setNavigationOnClickListener(view -> dialog.dismiss());
        ZoomImageView ivFile = dialog.findViewById(R.id.ivDialogOpenImage);
        ProgressBar progressBar = dialog.findViewById(R.id.ivProgressBar);
        if (!TextUtils.isEmpty(chatMessagesEntity.getMediaPath())) {
            File file = new File(chatMessagesEntity.getMediaPath());
            tvFilename.setText(file.getName());
            Picasso.with(ivFile.getContext()).load(file).placeholder(activity.getResources().getDrawable(R.drawable.default_user_icon)).into(ivFile, new Callback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    progressBar.setVisibility(View.GONE);
                }


            });
        } else {
            String[] downLoadUrls = chatMessagesEntity.getMediaUrl().split(",");
            String downLoadUrl = "";
            if (downLoadUrls.length > 1) {
                downLoadUrl = downLoadUrls[1];
            } else {
                downLoadUrl = downLoadUrls[0];
            }
            String fileName = downLoadUrl.split("\\?")[1].replace("FileName=", "");
            tvFilename.setText(fileName);

            URL url = null;
            URI uri = null;
            try {
                url = new URL(downLoadUrl);
                uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

                downLoadUrl = uri.toURL().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    AppLog.Log(TAG + "fileName", fileName);
                    ivFile.setImageBitmap(bitmap);
                    progressBar.setVisibility(View.GONE);
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + BuildConfig.FLAVOR);
                    dir.mkdirs();
                    File file = new File(dir, fileName);
                    try {
                        FileOutputStream ostream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, ostream);
                        ostream.flush();
                        ostream.close();
                        chatMessagesEntity.setMediaPath(file.getAbsolutePath());
                        dataRepository.updateChatMessagesEntity(chatMessagesEntity);
                    } catch (IOException e) {
                        Log.e("IOException", e.getLocalizedMessage());
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Log.e("onBitmapFailed", "onBitmapFailed");
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    progressBar.setVisibility(View.VISIBLE);
                    AppLog.Log(TAG, "onPrepareLoad");
                }
            };
            ivFile.setTag(target);
            Picasso.with(ivFile.getContext()).setLoggingEnabled(true);
            Picasso.with(ivFile.getContext()).load(downLoadUrl).placeholder(activity.getResources().getDrawable(R.drawable.default_user_icon)).into(target);

        }

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

    public void openChatVideoView(Activity activity, ChatMessagesEntity chatMessagesEntity) {
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_open_video);
        dialog.setCancelable(true);
        Toolbar toolbar = dialog.findViewById(R.id.dialogToolbar);
        TextView tvFilename = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbar.setNavigationOnClickListener(view -> dialog.dismiss());
        PlayerView ivDialogOpenVideo = dialog.findViewById(R.id.ivDialogOpenVideo);
        SimpleExoPlayer player;

        File file = new File(chatMessagesEntity.getMediaPath());
      /*  if (!TextUtils.isEmpty(chatMessagesEntity.getMediaPath())) {

            tvFilename.setText(file.getName());*/

            /*ivDialogOpenVideo.setVideoURI(Uri.fromFile(file));

            ivDialogOpenVideo.setOnPreparedListener(mp -> {
                ViewGroup.LayoutParams lp = ivDialogOpenVideo.getLayoutParams();
                float videoWidth = mp.getVideoWidth();
                float videoHeight = mp.getVideoHeight();
                float viewWidth = ivDialogOpenVideo.getWidth();
                lp.height = (int) (viewWidth * (videoHeight / videoWidth));
                ivDialogOpenVideo.setLayoutParams(lp);

                if(mp.isPlaying())
                    mp.start();
                progressBar.setVisibility(View.GONE);
            });*/

            player = ExoPlayerFactory.newSimpleInstance(this,
                    new DefaultRenderersFactory(this),
                    new DefaultTrackSelector(), new DefaultLoadControl());

            ivDialogOpenVideo.setPlayer(player);

            player.setPlayWhenReady(true);
            player.seekTo(0, 0);

            MediaSource mediaSource = buildMediaSource(Uri.fromFile(file));
            player.prepare(mediaSource, true, false);

       /* } else {
            String[] downLoadUrls = chatMessagesEntity.getMediaUrl().split(",");
            String downLoadUrl = "";
            if (downLoadUrls.length > 1) {
                downLoadUrl = downLoadUrls[1];
            } else {
                downLoadUrl = downLoadUrls[0];
            }
            String fileName = downLoadUrl.split("\\?")[1].replace("FileName=", "");
            tvFilename.setText(fileName);

            URL url = null;
            URI uri = null;
            try {
                url = new URL(downLoadUrl);
                uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

                downLoadUrl = uri.toURL().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            MediaController controller = new MediaController(this);
            controller.setAnchorView(ivDialogOpenVideo);
            controller.setMediaPlayer(ivDialogOpenVideo);
            ivDialogOpenVideo.setVisibility(View.VISIBLE);
            ivDialogOpenVideo.setMediaController(controller);
            ivDialogOpenVideo.setVideoURI(Uri.parse(downLoadUrl));
            ivDialogOpenVideo.setZOrderOnTop(true);
            ivDialogOpenVideo.setBackgroundColor(Color.TRANSPARENT);

            ivDialogOpenVideo.setOnPreparedListener(mp -> {
                progressBar.setVisibility(View.GONE);
                ViewGroup.LayoutParams lp = ivDialogOpenVideo.getLayoutParams();
                float videoWidth = mp.getVideoWidth();
                float videoHeight = mp.getVideoHeight();
                float viewWidth = ivDialogOpenVideo.getWidth();
                lp.height = (int) (viewWidth * (videoHeight / videoWidth));
                ivDialogOpenVideo.setLayoutParams(lp);
                ivDialogOpenVideo.start();
            });

        }
*/
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

   /* private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("asdfasf") ).
                createMediaSource(uri);
    }*/

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new FileDataSourceFactory();
        return new ExtractorMediaSource(uri, dataSourceFactory,
                new DefaultExtractorsFactory(), null, null);
    }
    public void logoutToServer(Activity activity) {

        Utils.showCustomProgressDialog(activity, false);
        Map<String, String> map = new HashMap<>();
        map.put("apiType", Utils.encrypt("2"));
        map.put("Type", Utils.encrypt("Quit"));
        map.put("Jid", Utils.encrypt(preferenceHelper.getOpenfireSenderId() + "@" + OPENFIRE_HOST_SERVER_SERVICE));


        Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().callChatLogsApi(Utils.encrypt("2"), Utils.encrypt(preferenceHelper.getOpenfireSenderId() + "@" + OPENFIRE_HOST_SERVER_SERVICE), Utils.encrypt("Login"));

        responseBodyCall.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Utils.hideCustomProgressDialog();
                logoutUser(activity);

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Utils.hideCustomProgressDialog();
                logoutUser(activity);
            }
        });
    }

    public void openLogoutConflictDialog(final Activity activity) {
        final Dialog dialog = new Dialog(activity, R.style.Theme_AppCompat_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout);
        dialog.setCancelable(false);
        TextView tvDialogOk = dialog.findViewById(R.id.tvDialogOk);
        tvDialogOk.setOnClickListener(view -> {
            dialog.dismiss();
            logoutToServer(activity);
        });
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }


    private void logoutUser(Activity activity) {
        ((MyApp) MyApp.getContext()).getRepository().deleteAllGroupModels();
        preferenceHelper.logoutUser();
        activity.stopService(new Intent(activity, XMPPService.class));
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(new Intent(activity, LoginActivity.class));
        finishAffinity();
    }


    /*public void sendUnReadMessageCountToService() {
        if (!preferenceHelper.isLogin()) {
            return;
        }
        ((MyApp) MyApp.getContext()).getRepository().getTotalUnreadCounter(new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Integer integer) {
               *//* Map<String, String> map = new HashMap<>();
                map.put("apiType", Utils.encrypt("2"));
                map.put("type", Utils.encrypt("Update"));
                map.put("badge", Utils.encrypt(String.valueOf(integer)));
                map.put("Jid", preferenceHelper.getOpenfireSenderId() + "@" + OPENFIRE_HOST_SERVER_SERVICE);*//*

                String formData=BASE_URL_OPENFIRE+"plugins/chatlogs?apiType="+Utils.encrypt("2")
                        +"&Jid="+Utils.encrypt(preferenceHelper.getOpenfireSenderId() + "@" + OPENFIRE_HOST_SERVER_SERVICE)
                       +"&type="+Utils.encrypt("Login")+"&badge="+Utils.encrypt(String.valueOf(integer));

                Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().callChatLogsApi(formData);
                responseBodyCall.enqueue(new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }*/

    @Override
    protected void onStop() {
        super.onStop();
        if (sessionDepth > 0)
            sessionDepth--;
        if (sessionDepth == 0) {
            // app went to background
            //sendUnReadMessageCountToService();
        }
    }

    public static String getDuration(File file) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(file.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return String.valueOf(Integer.parseInt(durationStr) / 1000);
    }

}
