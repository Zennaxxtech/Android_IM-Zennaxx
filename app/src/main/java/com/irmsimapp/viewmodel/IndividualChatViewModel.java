/*
 * Copyright 2017, The Android Open Source Project
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

package com.irmsimapp.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.text.TextUtils;

import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.utils.MyApp;

public class IndividualChatViewModel extends AndroidViewModel {
    private String from, to;
    private final MediatorLiveData<PagedList<ChatMessagesEntity>> mObservableProducts;

    public IndividualChatViewModel(Application application) {
        super(application);
        mObservableProducts = new MediatorLiveData<>();
        mObservableProducts.setValue(null);
    }

    public void setFromAndTo(String from, String to) {
        if (TextUtils.isEmpty(this.from) && TextUtils.isEmpty(this.to)) {
            this.from = from;
            this.to = to;
            getIndividualChatMessages();
        }
    }

    private void getIndividualChatMessages() {
        LiveData<PagedList<ChatMessagesEntity>> messageList = new LivePagedListBuilder<>(((MyApp) getApplication()).getRepository().getMoreIndividualChatMessages(from, to), 20).build();
        mObservableProducts.addSource(messageList, mObservableProducts::setValue);
    }

    public LiveData<PagedList<ChatMessagesEntity>> getMessageList() {
        return mObservableProducts;
    }


}
