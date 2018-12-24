package com.irmsimapp.Adapter;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.R;
import com.irmsimapp.database.DataRepository;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.database.entity.GroupModelEntity;
import com.irmsimapp.interfaces.OnGroupClickListeners;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.PreferenceHelper;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class GroupListAdapter extends BaseExpandableListAdapter implements Filterable {
    private List<GroupModelEntity> groupModelList;
    private List<GroupModelEntity> filteredList;

    private OnGroupClickListeners groupClickListeners;
    private CustomFilter mFilter;
    private SimpleDateFormat sDFormat;
    private String TAG = this.getClass().getSimpleName();
    private DataRepository dataRepository;


    public GroupListAdapter(DataRepository dataRepository, List<GroupModelEntity> groupModelList, OnGroupClickListeners groupClickListeners) {
        this.groupModelList = groupModelList;
        this.filteredList = new ArrayList<>();
        this.filteredList.addAll(groupModelList);
        this.groupClickListeners = groupClickListeners;
        this.mFilter = new CustomFilter();
        this.dataRepository = dataRepository;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.filteredList.get(groupPosition).getUserList().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        UserListItem usersList = (UserListItem) getChild(groupPosition, childPosition);
        String loginName = usersList.getLoginName();
        String fullName = usersList.getFullName();

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_child_item, null);
        }

        TextView txtListChild = (TextView) convertView.findViewById(R.id.tvChildUserName);
        TextView tv_individual_latest_message = (TextView) convertView.findViewById(R.id.tvIndividualLatestMessage);
        TextView tv_time = (TextView) convertView.findViewById(R.id.tvChildIndividualMsgTime);
        TextView tvPersonalMsgCounter = (TextView) convertView.findViewById(R.id.tvPersonalMsgCounter);
        ImageView iv_user = (ImageView) convertView.findViewById(R.id.ivChildUserIcon);

        txtListChild.setText(fullName);
        tv_individual_latest_message.setText("");
        tv_time.setText("");
        if (usersList.getChatMessagesEntity() != null && !TextUtils.isEmpty(usersList.getChatMessagesEntity().getMessage())) {
            long lastMsgTime = usersList.getChatMessagesEntity().getMsgTime();
            String lastMsg = usersList.getChatMessagesEntity().getMessage();

            if (lastMsgTime > 0) {
                Date date = new Date();
                date.setTime(lastMsgTime);
                if (DateUtils.isSameDay(date, new Date())) {
                    sDFormat = new SimpleDateFormat("HH:mm", Locale.US);
                } else {
                    sDFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                }

                if (lastMsg.trim().length() > 10)
                    lastMsg = lastMsg.substring(0, 10) + "...";
                tv_individual_latest_message.setText(lastMsg);
                tv_time.setText(sDFormat.format(date));
            }
        } else {
            dataRepository.getLastMessageInIndividualChat(PreferenceHelper.getInstance().getLoginName(), loginName, new SingleObserver<ChatMessagesEntity>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onSuccess(ChatMessagesEntity chatMessagesEntity) {
                    usersList.setChatMessagesEntity(chatMessagesEntity);
                    long lastMsgTime = usersList.getChatMessagesEntity().getMsgTime();
                    String lastMsg = usersList.getChatMessagesEntity().getMessage();

                    if (lastMsgTime > 0) {
                        Date date = new Date();
                        date.setTime(lastMsgTime);
                        if (DateUtils.isSameDay(date, new Date())) {
                            sDFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        } else {
                            sDFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                        }

                        if (lastMsg.trim().length() > 10)
                            lastMsg = lastMsg.substring(0, 10) + "...";
                        tv_individual_latest_message.setText(lastMsg);
                        tv_time.setText(sDFormat.format(date));


                        for (GroupModelEntity groupModelEntity : filteredList) {

                            Collections.sort(groupModelEntity.getUserList(), (t1, t2) -> Long.compare((t2.getChatMessagesEntity() == null) ? 0 : t2.getChatMessagesEntity().getMsgTime(), (t1.getChatMessagesEntity() == null) ? 0 : t1.getChatMessagesEntity().getMsgTime()));

                        }
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(Throwable e) {

                }
            });
        }


        dataRepository.getPersonalUnreadCounter(loginName.toLowerCase(), new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Integer integer) {
                if (integer != 0) {
                    tvPersonalMsgCounter.setVisibility(View.VISIBLE);
                    tvPersonalMsgCounter.setText(integer > 99 ? "99+" : String.valueOf(integer));
                } else {
                    tvPersonalMsgCounter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Throwable e) {
                AppLog.Log(TAG, "onError " + e.getMessage());
            }
        });


        String photoUrl = usersList.getPhotoUrl();
        if (StringUtils.isNotEmpty(photoUrl)) {
            if (!photoUrl.startsWith("http")) {
                photoUrl = "http://" + photoUrl;
            }
            Picasso.with(iv_user.getContext()).load(photoUrl).placeholder(convertView.getResources().getDrawable(R.drawable.default_user_icon)).into(iv_user);
        }

        convertView.setOnClickListener(view -> groupClickListeners.onGroupChildClick(view, groupPosition, childPosition));

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.filteredList.get(groupPosition).getUserList().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.filteredList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.filteredList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {

        GroupModelEntity groupEntity = (GroupModelEntity) getGroup(groupPosition);
        String groupNo = groupEntity.getGroupNo();
        String contactName = groupEntity.getContactName();
        String contactTitle = groupEntity.getContactTitle();
        String group_image = groupEntity.getPhotoUrl();
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_parent_item, null);
        }

        TextView lblListHeader = (TextView) convertView.findViewById(R.id.lblListHeader);
        TextView tv_transferee_name = (TextView) convertView.findViewById(R.id.tv_transferee_name);
        TextView tv_group_latest_message = (TextView) convertView.findViewById(R.id.tv_group_latest_message);
        TextView tv_time = (TextView) convertView.findViewById(R.id.tvChildIndividualMsgTime);
        ImageView iv_group = (ImageView) convertView.findViewById(R.id.iv_group);
        TextView tvGroupMsgCounter = (TextView) convertView.findViewById(R.id.tvGroupMsgCounter);

        if (StringUtils.isNotEmpty(group_image)) {
            if (!group_image.startsWith("http")) {
                group_image = "http://" + group_image;
            }
            Picasso.with(iv_group.getContext()).load(group_image).placeholder(R.drawable.group_icon_round).into(iv_group);
        }
        tv_group_latest_message.setText("");
        tv_time.setText("");
        tvGroupMsgCounter.setText("");
        if (groupEntity.getChatMessagesEntity() != null && !TextUtils.isEmpty(groupEntity.getChatMessagesEntity().getMessage())) {
            long lastMsgTime = groupEntity.getChatMessagesEntity().getMsgTime();
            String lastMessage = groupEntity.getChatMessagesEntity().getMessage();
            String msgFromGroupMember = groupEntity.getChatMessagesEntity().getFullName();
            if (lastMsgTime > 0) {
                Date date = new Date();
                date.setTime(lastMsgTime);
                if (DateUtils.isSameDay(date, new Date())) {
                    sDFormat = new SimpleDateFormat("HH:mm", Locale.US);
                } else {
                    sDFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                }

                if (lastMessage.trim().length() > 10)
                    lastMessage = lastMessage.substring(0, 10) + "...";
                tv_group_latest_message.setText(String.format("%s:%s", msgFromGroupMember, lastMessage));
                tv_time.setText(sDFormat.format(date));
            }
        }
        dataRepository.getGroupUnreadCounter(groupNo.toLowerCase(), new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Integer integer) {
                if (integer != 0) {
                    tvGroupMsgCounter.setVisibility(View.VISIBLE);
                    tvGroupMsgCounter.setText(integer > 99 ? "99+" : String.valueOf(integer));
                } else {
                    tvGroupMsgCounter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Throwable e) {
            }
        });


        LinearLayout layout_right = (LinearLayout) convertView.findViewById(R.id.layout_right);
        lblListHeader.setText(groupNo);
        if (groupNo.equalsIgnoreCase("Internal")) {
         /*   tv_transferee_name.setVisibility(View.GONE);
        } else {
            tv_transferee_name.setVisibility(View.VISIBLE);
            tv_transferee_name.setText(String.format("%s : %s", contactTitle, contactName));*/
        }

        ImageView iv_expand_collapse = (ImageView) convertView.findViewById(R.id.iv_expand_collapse);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(groupNo);

        if (isExpanded) {
            iv_expand_collapse.setImageResource(R.drawable.down_green_aerrow);
        } else {
            iv_expand_collapse.setImageResource(R.drawable.up_aerrow);
        }

        layout_right.setOnClickListener(view -> groupClickListeners.onGroupParentClick(view, groupPosition));
        convertView.setOnClickListener(view -> groupClickListeners.onGroupParentClick(view, groupPosition));

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public class CustomFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredList.clear();
            final FilterResults results = new FilterResults();
            if (constraint.length() == 0) {
                filteredList.addAll(groupModelList);
            } else {
                final String filterPattern = constraint.toString().toLowerCase().trim();
                for (GroupModelEntity dataItem : groupModelList) {
                    if (dataItem.getGroupNo().toLowerCase().contains(filterPattern)) {
                        filteredList.add(dataItem);
                    } else {
                        for (UserListItem userListItem : dataItem.getUserList()) {
                            if (userListItem.getLoginName().toLowerCase().contains(filterPattern)) {
                                filteredList.add(dataItem);
                                break;
                            }
                        }
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    public List<GroupModelEntity> getFilteredList() {
        return this.filteredList;
    }

    private void moveLastModifiedFirst() {

        Collections.sort(filteredList, (t1, t2) -> Long.compare(t2.getChatMessagesEntity().getMsgTime(), t1.getChatMessagesEntity().getMsgTime()));
       /* for (GroupModelEntity groupModelEntity : filteredList) {

            Collections.sort(groupModelEntity.getUserList(), (lhs, rhs) -> {

                if (lhs.getChatMessagesEntity() == null) {
                    return (rhs.getChatMessagesEntity() == null) ? 0 : 1;
                }
                if (rhs.getChatMessagesEntity() == null) {
                    return -1;
                }
                return Long.compare(rhs.getChatMessagesEntity().getMsgTime(), lhs.getChatMessagesEntity().getMsgTime());

            });
        }*/
    }

    public void setGroupModelList(List<GroupModelEntity> groupModelList) {
        this.groupModelList = groupModelList;
        filteredList.clear();
        this.filteredList.addAll(this.groupModelList);
        moveLastModifiedFirst();
        this.notifyDataSetChanged();
    }

}