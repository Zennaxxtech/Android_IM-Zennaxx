package com.irmsimapp.datamodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.BadKeyWord.BadKeyWord;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BadKeyWordsModel extends ViewModel {
    private static MutableLiveData<ArrayList<String>> badKeyWords;

    public LiveData<ArrayList<String>> getBadKeyWords() {
        if (badKeyWords == null) {
            badKeyWords = new MutableLiveData<>();
        }
        return badKeyWords;
    }

    public static void getBadWordsFromServer() {
        Map<String, String> group_bad_keyword_param = new HashMap<>();
        group_bad_keyword_param.put(Const.intentKey.USER_TYPE, Utils.encrypt(PreferenceHelper.getInstance().getUserType()));
        Call<BadKeyWord> badKeyWordCall;
        if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
            badKeyWordCall = ApiHandler.getCommonApiService().checkBadKeyWordsAspx(group_bad_keyword_param);
        } else {
            badKeyWordCall = ApiHandler.getCommonApiService().checkBadKeyWords(group_bad_keyword_param);
        }
        badKeyWordCall.enqueue(new Callback<BadKeyWord>() {
            @Override
            public void onResponse(Call<BadKeyWord> call, Response<BadKeyWord> response) {
                BadKeyWord badKeyWord = response.body();
                ArrayList<String> badWords = new ArrayList<>();

                if (badKeyWord != null && badKeyWord.getStatus().equalsIgnoreCase("1")) {
                    List<BadKeyWord.Datum> datumList = badKeyWord.getData();
                    if (datumList != null) {
                        for (int i = 0; i < datumList.size(); i++) {
                            badWords.add(datumList.get(i).getItemName().toLowerCase());
                        }
                    } else {
                        ArrayList<String> strings = new ArrayList<>();
                        strings.add("fuck");
                        strings.add("fuck off");
                        strings.add("bitch");
                        strings.add("SOB");
                        strings.add("Pervert");
                        strings.add("stupid jerk");
                        strings.add("bastard");
                        badWords.addAll(strings);
                    }
                }
                badKeyWords.setValue(badWords);
            }

            @Override
            public void onFailure(Call<BadKeyWord> call, Throwable t) {
                Utils.showToast(t.getMessage());
            }
        });
    }
}
