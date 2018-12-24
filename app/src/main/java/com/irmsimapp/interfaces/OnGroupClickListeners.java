package com.irmsimapp.interfaces;

import android.view.View;



public interface OnGroupClickListeners {

    void onGroupParentClick(View view, int groupPosition);

    void onGroupChildClick(View view, int groupPosition, int childPosition);
}
