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

package com.irmsimapp.Adapter;

import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.drawee.view.SimpleDraweeView;
import com.irmsimapp.R;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.interfaces.AudioChangeListener;
import com.irmsimapp.interfaces.OnRecyclerViewClickListener;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class ChatAdapter extends PagedListAdapter<ChatMessagesEntity, RecyclerView.ViewHolder> {
    private static final int CHAT_MSG = 0;
    private static final int CHAT_IMAGE = 1;
    private static final int CHAT_AUDIO = 2;
    private static final int CHAT_VIDEO = 3;
    private String TAG = "ChatAdapter";
    private SimpleDateFormat sdfHours;
    private OnRecyclerViewClickListener recyclerViewClickListener;
    private MediaPlayer mPlayer = null;
    private AnimationDrawable frameAnimation;
    private ImageView ivPlaying;
    private Handler handler;
    private boolean isLastPlayRight;
    private Runnable runnable;
    private AudioChangeListener audioChangeListener;
    private AudioManager audioManager;
    private String audioFilePath = null;
    private boolean isGroupChat;
    private String loginName;

    public ChatAdapter(boolean isGroupChat, @NonNull DiffUtil.ItemCallback<ChatMessagesEntity> diffCallback, OnRecyclerViewClickListener recyclerViewClickListener) {
        super(diffCallback);
        this.recyclerViewClickListener = recyclerViewClickListener;
        this.isGroupChat = isGroupChat;
        sdfHours = new SimpleDateFormat("hh:mm a", Locale.US);
        this.audioChangeListener = isSpeaker -> {
            if (mPlayer != null && audioManager != null && !TextUtils.isEmpty(audioFilePath)) {
                if (isSpeaker) {
                    changeAudioPlayerMode(true);
                } else {
                    changeAudioPlayerMode(false);
                }
            }
        };
        this.loginName = PreferenceHelper.getInstance().getLoginName();
        handler = new Handler();
        this.audioManager = (AudioManager) MyApp.getContext().getSystemService(Context.AUDIO_SERVICE);

    }

    private class ViewHolderChatMsg extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SimpleDraweeView ivChatUser;
        private TextView tvChatUserName, tvChatMsg, tvChatMsgTime;
        private LinearLayout llBgChatMessage;
        private RelativeLayout llChatMain;

        ViewHolderChatMsg(View view) {
            super(view);
            ivChatUser = view.findViewById(R.id.ivChatUser);
            tvChatUserName = view.findViewById(R.id.tvChatUserName);
            tvChatMsg = view.findViewById(R.id.tvChatMsg);
            tvChatMsgTime = view.findViewById(R.id.tvChatMsgTime);
            llBgChatMessage = view.findViewById(R.id.llBgChatMessage);
            llChatMain = view.findViewById(R.id.llChatMain);
            ivChatUser.setOnClickListener(this);
            tvChatMsg.setMaxWidth(Utils.getScreenWidth() / 2);
            tvChatUserName.setMaxWidth(Utils.getScreenWidth() / 2);
        }

        private void bindData(ChatMessagesEntity chatMessagesEntity) {

            boolean isRight = loginName.equalsIgnoreCase(isGroupChat ? chatMessagesEntity.getMsgFromGroupMember() : chatMessagesEntity.getMsgFrom());
            llChatMain.setLayoutDirection(isRight ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            llBgChatMessage.setBackground(llBgChatMessage.getContext().getResources().getDrawable(isRight ? R.drawable.voice_green_bg : R.drawable.voice_white_bg));
            tvChatUserName.setVisibility(isRight ? View.GONE : View.VISIBLE);
            Calendar calendar0 = Calendar.getInstance();
            calendar0.setTimeInMillis(chatMessagesEntity.getMsgTime());
            tvChatMsgTime.setText(sdfHours.format(calendar0.getTimeInMillis()));
            tvChatMsg.setText(chatMessagesEntity.getMessage());
            tvChatUserName.setText(chatMessagesEntity.getFullName());
            String userUrl;
            if (isRight) {
                userUrl = PreferenceHelper.getInstance().getProfilePicture();
            } else {
                userUrl = chatMessagesEntity.getSenderProfileUrl();
            }

            if (!TextUtils.isEmpty(userUrl)) {
                final Uri imageUri = Uri.parse(userUrl);
                ivChatUser.setImageURI(imageUri);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ivChatUser:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
            }
        }
    }

    private class ViewHolderChatImage extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SimpleDraweeView ivChatUser;
        private TextView tvChatUserName, tvChatMsgTime;
        private FrameLayout ivChatContentImg;
        private ImageView ivChatMsgImg;
        private LinearLayout llBgChatMessage;
        private RelativeLayout llChatMain;
        private ProgressBar ivProgressBar;

        ViewHolderChatImage(View view) {
            super(view);
            ivChatUser = view.findViewById(R.id.ivChatUser);
            ivChatUser.setOnClickListener(this);
            tvChatUserName = view.findViewById(R.id.tvChatUserName);
            ivChatContentImg = view.findViewById(R.id.flChatContentImg);
            ivChatMsgImg = view.findViewById(R.id.ivChatMsgImg);
            tvChatMsgTime = view.findViewById(R.id.tvChatMsgTime);
            ivProgressBar = view.findViewById(R.id.ivProgressBar);
            llBgChatMessage = view.findViewById(R.id.llBgChatMessage);
            llChatMain = view.findViewById(R.id.llChatMain);
            ivChatMsgImg.setMaxHeight(Utils.getScreenHeight() / 4);
            ivChatMsgImg.setMaxWidth(Utils.getScreenWidth() / 2);
        }

        private void bindData(ChatMessagesEntity chatMessagesEntity) {
            boolean isRight = loginName.equalsIgnoreCase(isGroupChat ? chatMessagesEntity.getMsgFromGroupMember() : chatMessagesEntity.getMsgFrom());
            llChatMain.setLayoutDirection(isRight ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            llBgChatMessage.setBackground(llBgChatMessage.getContext().getResources().getDrawable(isRight ? R.drawable.voice_green_bg : R.drawable.voice_white_bg));
            Calendar calendar0 = Calendar.getInstance();
            calendar0.setTimeInMillis(chatMessagesEntity.getMsgTime());
            tvChatMsgTime.setText(sdfHours.format(calendar0.getTimeInMillis()));
            tvChatUserName.setText(chatMessagesEntity.getFullName());
            if (chatMessagesEntity.isUploadingFile()) {
                ivProgressBar.setVisibility(View.VISIBLE);
            } else {
                ivProgressBar.setVisibility(View.GONE);
                ivChatContentImg.setOnClickListener(this);
            }
            tvChatUserName.setVisibility(isRight ? View.GONE : View.VISIBLE);
            String userUrl;
            if (isRight) {
                userUrl = PreferenceHelper.getInstance().getProfilePicture();
            } else {
                userUrl = chatMessagesEntity.getSenderProfileUrl();
            }
            if (!TextUtils.isEmpty(userUrl)) {
                final Uri imageUri = Uri.parse(userUrl);
                ivChatUser.setImageURI(imageUri);
            }

            String contentFilePath = chatMessagesEntity.getMediaPath();

            String thumbnailUrl = null;
            URL url = null;
            URI uri = null;
            try {
                url = new URL(chatMessagesEntity.getThumbnailUrl());
                uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                thumbnailUrl = uri.toURL().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (StringUtils.isNotEmpty(contentFilePath)) {
                Glide
                        .with(ivChatContentImg.getContext())
                        .asBitmap()
                        .load(Uri.fromFile(new File(contentFilePath)))
                        .apply(new RequestOptions().override(600, 400))
                        .apply(new RequestOptions().placeholder(ivChatContentImg.getContext().getResources().getDrawable(R.drawable.default_user_icon)))
                        .into(ivChatMsgImg);
            } else {
                Glide
                        .with(ivChatContentImg.getContext())
                        .asBitmap()
                        .load(thumbnailUrl)
                        .apply(new RequestOptions().override(600, 400))
                        .apply(new RequestOptions().placeholder(ivChatContentImg.getContext().getResources().getDrawable(R.drawable.default_user_icon)))
                        .into(ivChatMsgImg);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.flChatContentImg:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
                case R.id.ivChatUser:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
            }
        }
    }

    private class ViewHolderChatVideo extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SimpleDraweeView ivChatUser;
        private TextView tvChatUserName, tvChatMsgTime;
        private FrameLayout flChatContentVideo;
        private ImageView ivChatMsgVideo;
        private LinearLayout llBgChatMessage;
        private RelativeLayout llChatMain;
        private ProgressBar ivProgressBar;

        ViewHolderChatVideo(View view) {
            super(view);
            ivChatUser = view.findViewById(R.id.ivChatUser);
            ivChatUser.setOnClickListener(this);
            tvChatUserName = view.findViewById(R.id.tvChatUserName);
            flChatContentVideo = view.findViewById(R.id.flChatContentVideo);
            ivChatMsgVideo = view.findViewById(R.id.ivChatMsgVideo);
            tvChatMsgTime = view.findViewById(R.id.tvChatMsgTime);
            ivProgressBar = view.findViewById(R.id.ivProgressBar);
            llBgChatMessage = view.findViewById(R.id.llBgChatMessage);
            llChatMain = view.findViewById(R.id.llChatMain);
            ivChatMsgVideo.setMaxHeight(Utils.getScreenHeight() / 4);
            ivChatMsgVideo.setMaxWidth(Utils.getScreenWidth() / 2);
        }

        private void bindData(ChatMessagesEntity chatMessagesEntity) {
            boolean isRight = loginName.equalsIgnoreCase(isGroupChat ? chatMessagesEntity.getMsgFromGroupMember() : chatMessagesEntity.getMsgFrom());
            llChatMain.setLayoutDirection(isRight ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            llBgChatMessage.setBackground(llBgChatMessage.getContext().getResources().getDrawable(isRight ? R.drawable.voice_green_bg : R.drawable.voice_white_bg));
            Calendar calendar0 = Calendar.getInstance();
            calendar0.setTimeInMillis(chatMessagesEntity.getMsgTime());
            tvChatMsgTime.setText(sdfHours.format(calendar0.getTimeInMillis()));
            tvChatUserName.setText(chatMessagesEntity.getFullName());
            if (chatMessagesEntity.isUploadingFile()) {
                ivProgressBar.setVisibility(View.VISIBLE);
            } else {
                ivProgressBar.setVisibility(View.GONE);
                flChatContentVideo.setOnClickListener(this);
            }
            tvChatUserName.setVisibility(isRight ? View.GONE : View.VISIBLE);
            String userUrl;
            if (isRight) {
                userUrl = PreferenceHelper.getInstance().getProfilePicture();
            } else {
                userUrl = chatMessagesEntity.getSenderProfileUrl();
            }
            if (!TextUtils.isEmpty(userUrl)) {
                final Uri imageUri = Uri.parse(userUrl);
                ivChatUser.setImageURI(imageUri);
            }

            String contentFilePath = chatMessagesEntity.getMediaPath();

            String thumbnailUrl = null;
            URL url = null;
            URI uri = null;
            try {
                url = new URL(chatMessagesEntity.getThumbnailUrl());
                uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                thumbnailUrl = uri.toURL().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (StringUtils.isNotEmpty(contentFilePath)) {

                Glide
                        .with(ivChatMsgVideo.getContext())
                        .asBitmap()
                        .load(Uri.fromFile(new File(contentFilePath)))
                        .apply(new RequestOptions().override(600, 400))
                        .into(ivChatMsgVideo);
            } else {
                Glide
                        .with(ivChatMsgVideo.getContext())
                        .asBitmap()
                        .load(thumbnailUrl)
                        .apply(new RequestOptions().override(600, 400))
                        .into(ivChatMsgVideo);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.flChatContentVideo:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
                case R.id.ivChatUser:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
            }
        }
    }

    private class ViewHolderChatAudio extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SimpleDraweeView ivChatUser;
        private TextView tvChatUserName, tvChatMsgTime, tvVoiceLength;
        private ImageView ivChatAudioStartStop;
        private LinearLayout llBgChatMessage;
        private RelativeLayout llChatMain;
        private ProgressBar ivProgressBar;

        ViewHolderChatAudio(View view) {
            super(view);
            ivChatUser = view.findViewById(R.id.ivChatUser);
            tvChatUserName = view.findViewById(R.id.tvChatUserName);
            ivChatAudioStartStop = view.findViewById(R.id.ivChatAudioStartStop);
            tvVoiceLength = view.findViewById(R.id.tvVoiceLength);
            tvChatMsgTime = view.findViewById(R.id.tvChatMsgTime);
            llBgChatMessage = view.findViewById(R.id.llBgChatMessage);
            ivProgressBar = view.findViewById(R.id.ivProgressBar);
            llChatMain = view.findViewById(R.id.llChatMain);
            ivChatUser.setOnClickListener(this);
        }

        private void bindData(ChatMessagesEntity chatMessagesEntity) {
            boolean isRight = loginName.equalsIgnoreCase(isGroupChat ? chatMessagesEntity.getMsgFromGroupMember() : chatMessagesEntity.getMsgFrom());
            llChatMain.setLayoutDirection(isRight ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            llBgChatMessage.setBackground(llBgChatMessage.getContext().getResources().getDrawable(isRight ? R.drawable.voice_green_bg : R.drawable.voice_white_bg));
            Calendar calendar0 = Calendar.getInstance();
            calendar0.setTimeInMillis(chatMessagesEntity.getMsgTime());
            tvChatMsgTime.setText(sdfHours.format(calendar0.getTimeInMillis()));
            tvChatUserName.setText(chatMessagesEntity.getFullName());
            tvChatUserName.setVisibility(isRight ? View.GONE : View.VISIBLE);
            ivChatAudioStartStop.setBackgroundResource(isRight ? R.drawable.green_voice : R.drawable.green_voice_left);
            String userUrl;
            if (isRight) {
                userUrl = PreferenceHelper.getInstance().getProfilePicture();
            } else {
                userUrl = chatMessagesEntity.getSenderProfileUrl();
            }
            if (!TextUtils.isEmpty(userUrl)) {
                final Uri imageUri = Uri.parse(userUrl);
                ivChatUser.setImageURI(imageUri);
            }

            if (chatMessagesEntity.isUploadingFile()) {
                ivProgressBar.setVisibility(View.VISIBLE);
            } else {
                ivProgressBar.setVisibility(View.GONE);
            }
            try {
                Uri uri = Uri.parse(chatMessagesEntity.getMediaPath());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(llBgChatMessage.getContext(), uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int millSecond = Integer.parseInt(durationStr);
                tvVoiceLength.setText(millSecond / 1000 + "'s");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            ivChatAudioStartStop.setOnClickListener(v -> {
                String filePath = chatMessagesEntity.getMediaPath();
                playAudio(ivChatAudioStartStop, filePath, isRight);
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ivChatUser:
                    recyclerViewClickListener.onRecyclerViewClick(v, getLayoutPosition());
                    break;
            }
        }
    }


    @Override
    public int getItemViewType(int position) {
        ChatMessagesEntity chatMessage = getItem(position);
        if (chatMessage == null) {
            return CHAT_MSG;
        }
        if (chatMessage.getMsgType().equalsIgnoreCase("chat")) {
            return CHAT_MSG;
        } else if (chatMessage.getMsgType().equalsIgnoreCase("audio")) {
            return CHAT_AUDIO;
        } else if (chatMessage.getMsgType().equalsIgnoreCase("image")) {
            return CHAT_IMAGE;
        } else if (chatMessage.getMsgType().equalsIgnoreCase("video")) {
            return CHAT_VIDEO;
        } else {
            return CHAT_MSG;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case CHAT_MSG:
                return new ViewHolderChatMsg(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_msg, parent, false));
            case CHAT_IMAGE:
                return new ViewHolderChatImage(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_image, parent, false));
            case CHAT_AUDIO:
                return new ViewHolderChatAudio(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_audio, parent, false));
            case CHAT_VIDEO:
                return new ViewHolderChatVideo(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_video, parent, false));
        }
        return new ViewHolderChatMsg(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_msg, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        ChatMessagesEntity chatMessagesEntity = getItem(position);
        if (chatMessagesEntity != null) {
            switch (viewHolder.getItemViewType()) {

                case CHAT_MSG:
                    final ViewHolderChatMsg viewHolderChatLeftMsg = (ViewHolderChatMsg) viewHolder;
                    viewHolderChatLeftMsg.bindData(chatMessagesEntity);
                    break;
                case CHAT_IMAGE:
                    final ViewHolderChatImage viewHolderChatLeftImage = (ViewHolderChatImage) viewHolder;
                    viewHolderChatLeftImage.bindData(chatMessagesEntity);
                    break;
                case CHAT_AUDIO:
                    final ViewHolderChatAudio viewHolderChatLeftAudio = (ViewHolderChatAudio) viewHolder;
                    viewHolderChatLeftAudio.bindData(chatMessagesEntity);
                    break;
                case CHAT_VIDEO:
                    final ViewHolderChatVideo viewHolderChatLeftVideo = (ViewHolderChatVideo) viewHolder;
                    viewHolderChatLeftVideo.bindData(chatMessagesEntity);
                    break;

            }
        } else {
            AppLog.Log(TAG, " chat message null in bindHolder");
        }
    }


    private void playAudio(final View view, String AudioFilePath, final boolean isRight) {
        File file = new File(AudioFilePath);
        if (!file.exists()) {
            Utils.showToast(view.getContext().getString(R.string.msg_audio_file_does_not_exist));
            return;
        }
        audioFilePath = AudioFilePath;
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }
        mPlayer = null;
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(file.getAbsolutePath());
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            mPlayer.setVolume(1.0f, 1.0f);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            AppLog.Log(TAG, e.getMessage());
            AppLog.Log(TAG, e.getCause() + "");
            Utils.showToast("Not Playing Audio,try again.");
            return;
        }

        if (frameAnimation != null) {
            frameAnimation.stop();
            frameAnimation = null;
            ivPlaying.setBackground(AppCompatResources.getDrawable(view.getContext(), isLastPlayRight ? R.drawable.green_voice : R.drawable.green_voice_left));
        }


        ivPlaying = (ImageView) view;
        isLastPlayRight = isRight;
        ivPlaying.setBackgroundResource(isRight ? R.drawable.play_audio_right : R.drawable.play_audio_left);
        frameAnimation = (AnimationDrawable) ivPlaying.getBackground();
        ivPlaying.post(new Runnable() {
            @Override
            public void run() {
                frameAnimation.start();
                frameAnimation.setOneShot(false);
            }
        });
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                if (frameAnimation != null) {
                    ivPlaying.setBackground(AppCompatResources.getDrawable(ivPlaying.getContext(), isRight ? R.drawable.green_voice : R.drawable.green_voice_left));
                    frameAnimation.stop();
                    frameAnimation = null;
                }
            }
        };
        handler.postDelayed(runnable, mPlayer.getDuration());
    }

    public void stopPlaying() {
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }
        mPlayer = null;
        audioFilePath = null;
    }

    public AudioChangeListener getAudioChangeListener() {
        return this.audioChangeListener;
    }

    private void changeAudioPlayerMode(boolean isSpeaker) {
        final int lastPosition = mPlayer.getCurrentPosition();
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFilePath);
            if (isSpeaker) {
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(true);
            } else {
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(false);
            }
            mPlayer.setVolume(1.0f, 1.0f);
            mPlayer.setOnPreparedListener(mp -> {
                mp.start();
                mp.seekTo(lastPosition);
            });
            mPlayer.prepareAsync();
            mPlayer.setOnCompletionListener(mp -> {
                if (frameAnimation != null) {
                    frameAnimation.stop();
                    ivPlaying.setBackground(AppCompatResources.getDrawable(ivPlaying.getContext(), isLastPlayRight ? R.drawable.green_voice : R.drawable.green_voice_left));
                }
            });
        } catch (IOException e) {
            AppLog.Log(TAG, e.getMessage());
        }
    }


}