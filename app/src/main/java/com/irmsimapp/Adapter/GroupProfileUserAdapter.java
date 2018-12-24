package com.irmsimapp.Adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.R;
import com.irmsimapp.activity.IndividualChatActivity;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.PreferenceHelper;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;


public class GroupProfileUserAdapter extends RecyclerView.Adapter<GroupProfileUserAdapter.ViewHolder> {
    private ArrayList<UserListItem> groupmembers;



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircularImageView iv_group_member;
        TextView tv_group_member_name;
        LinearLayout layout_group;

        private ViewHolder(View view) {
            super(view);
            layout_group = view.findViewById(R.id.layout_group);
            tv_group_member_name = view.findViewById(R.id.tv_group_member_name);
            iv_group_member = view.findViewById(R.id.iv_group_member);
            layout_group.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.layout_group:
                    int position = getLayoutPosition();
                    Intent intent = new Intent(view.getContext(), IndividualChatActivity.class);
                    if (groupmembers.get(position).getLoginName().equalsIgnoreCase(PreferenceHelper.getInstance().getLoginName())) {
                        return;
                    }
                    intent.putExtra(Const.intentKey.USER_DATA, groupmembers.get(position));
                    view.getContext().startActivity(intent);
                    break;
            }
        }
    }

    public GroupProfileUserAdapter( ArrayList<UserListItem> groupmembers) {
        this.groupmembers = groupmembers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_users, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String fullName;
        if (groupmembers.get(position).getFullName().length() > 8) {
            fullName = groupmembers.get(position).getFullName().substring(0, 6) + "..";
        } else {
            fullName = groupmembers.get(position).getFullName();
        }
        holder.tv_group_member_name.setText(fullName);
        String photoUrl = groupmembers.get(position).getPhotoUrl().trim();
        if (StringUtils.isNotEmpty(photoUrl)) {
            if (!photoUrl.startsWith("http")) {
                photoUrl = "http://" + photoUrl;
            }
            Picasso.with(holder.iv_group_member.getContext()).load(photoUrl).placeholder(holder.iv_group_member.getContext().getResources().getDrawable(R.drawable.default_user_icon)).into(holder.iv_group_member);
        }

    }


    @Override
    public int getItemCount() {
        return groupmembers.size();
    }
}
