package com.irmsimapp.activity;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.irmsimapp.Adapter.GroupProfileUserAdapter;
import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.R;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.utils.Const;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class GroupProfileActivity extends BaseActivity {
    private String groupName, contactName, contactTitle, groupImageUrl;

    private ArrayList<UserListItem> groupmembers;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_profile);
        groupName = getIntent().getStringExtra(Const.intentKey.GROUP_NAME);
        contactName = getIntent().getStringExtra(Const.intentKey.CONTACT_NAME);
        contactTitle = getIntent().getStringExtra(Const.intentKey.CONTACT_TITLE);
        groupImageUrl = getIntent().getStringExtra(Const.intentKey.GROUP_IMAGE_URL);
        groupmembers = (ArrayList<UserListItem>) getIntent().getSerializableExtra(Const.intentKey.GROUP_MEMBERS);
        setUpToolbar();
        setUpViewAndClickAction();
    }

    @Override
    void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbarTitle.setText(R.string.members);
        CircularImageView toolbarIconLeft = toolbar.findViewById(R.id.ivToolbarIconLeft);
        CircularImageView toolbarIconRight = toolbar.findViewById(R.id.ivToolbarIconRight);
        toolbarIconLeft.setVisibility(View.GONE);
        toolbarIconRight.setVisibility(View.GONE);
        toolbar.setNavigationIcon(R.drawable.left_aerrow);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

    }

    @Override
    void setUpViewAndClickAction() {
        CircularImageView ivGroupIcon = findViewById(R.id.ivGroupIcon);
        TextView tvGroupName = findViewById(R.id.tvGroupName);
        TextView tvGroupContactName = findViewById(R.id.tvGroupContactName);
        TextView tvGroupMembersCount = findViewById(R.id.tvGroupMembersCount);
        RecyclerView rvGroupUser = findViewById(R.id.rvGroupUser);
        GroupProfileUserAdapter groupProfileUserAdapter = new GroupProfileUserAdapter(groupmembers);
        GridLayoutManager layoutManager = new GridLayoutManager(GroupProfileActivity.this, 3);
        rvGroupUser.setLayoutManager(layoutManager);
        rvGroupUser.setHasFixedSize(true);
        rvGroupUser.setAdapter(groupProfileUserAdapter);
        tvGroupName.setText(groupName);
        tvGroupContactName.setText(String.format("%s %s", contactTitle, contactName));
        tvGroupMembersCount.setText(String.format(Locale.US, "%d", groupmembers.size()));
        if (StringUtils.isNotEmpty(groupImageUrl)) {
            if (!groupImageUrl.startsWith("http")) {
                groupImageUrl = "http://" + groupImageUrl;
            }
            Picasso.with(ivGroupIcon.getContext()).load(groupImageUrl).placeholder(R.drawable.group_icon_round).resize(1200, 800).onlyScaleDown().into(ivGroupIcon);
        }
    }

    @Override
    public void closedOnError() {
        super.closedOnError();
        logoutToServer(GroupProfileActivity.this);
    }

    @Override
    public void closedOnConflict() {
        super.closedOnConflict();
        runOnUiThread(() -> openLogoutConflictDialog(GroupProfileActivity.this));
    }
}
