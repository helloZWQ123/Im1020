package com.im1020.controller.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.exceptions.HyphenateException;
import com.im1020.ImApplication;
import com.im1020.R;
import com.im1020.controller.adapter.GroupDetailAdapter;
import com.im1020.modle.Modle;
import com.im1020.modle.bean.UserInfo;
import com.im1020.utils.Constant;
import com.im1020.utils.ShowToast;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatDetailsActivity extends AppCompatActivity {

    @Bind(R.id.gv_group_detail)
    GridView gvGroupDetail;
    @Bind(R.id.bt_group_detail)
    Button btGroupDetail;
    private String groupid;
    private String owner;
    private EMGroup group;
    private GroupDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_details);
        ButterKnife.bind(this);


        getData();
        //initView();
        initData();

        //获取群成员
        getGroupMembers();
        //监听事件
        initListener();

    }

    private void initListener() {
        gvGroupDetail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()){

                    case MotionEvent.ACTION_DOWN:

                        //获取当前gridview适配器中的deleteModle
                        boolean deleteModle = adapter.getDeleteModle();
                        //只有删除模式下才管用
                        if (deleteModle){
                            adapter.setDeleteModle(false);
                        }

                        break;
                }
                return false;
            }
        });
    }

    private void getData() {
        //获取群id
        groupid = getIntent().getStringExtra("groupid");

        if (TextUtils.isEmpty(groupid)){

            return;
        }
    }

    private void getGroupMembers() {

        Modle.getInstance().getGlobalThread().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //获取群组
                    EMGroup emGroup = EMClient.getInstance().groupManager()
                            .getGroupFromServer(groupid);

                    //获取群成员
                    List<String> members = emGroup.getMembers();

                    //转类型
                    final List<UserInfo> userinfos = new ArrayList<UserInfo>();

                    for (String hxid:members
                         ) {
                        userinfos.add(new UserInfo(hxid));
                    }
                    //内存和网页
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.refresh(userinfos);
                        }
                    });

                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private void initView() {

    }

    private void initData() {

        //获取当前的群组
        group = EMClient.getInstance().groupManager().getGroup(groupid);
        //获取群主
        owner = group.getOwner();

        if (EMClient.getInstance().getCurrentUser().equals(owner)){
            //是群主
            btGroupDetail.setText("解散群");

            btGroupDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Modle.getInstance().getGlobalThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //去环信服务器解散群
                                EMClient.getInstance().groupManager()
                                        .destroyGroup(groupid);
                                //退群
                                exitGroup();
                                //结束当前页面
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                        ShowToast.show(ChatDetailsActivity.this,"解散群成功");
                                    }
                                });
                            } catch (HyphenateException e) {
                                e.printStackTrace();
                                ShowToast.showUI(ChatDetailsActivity.this,"解散群失败"+e.getMessage());
                            }
                        }
                    });
                }
            });
        }else{
            //是成员
            btGroupDetail.setText("退群");
            btGroupDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Modle.getInstance().getGlobalThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //告诉环信退群
                                EMClient.getInstance().groupManager()
                                        .leaveGroup(groupid);
                                exitGroup();
                                //结束当前页面
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                        ShowToast.show(ChatDetailsActivity.this,"退群成功");
                                    }
                                });
                            } catch (HyphenateException e) {
                                e.printStackTrace();
                                ShowToast.showUI(ChatDetailsActivity.this,"退群失败"+e.getMessage());
                            }
                        }
                    });
                }
            });
        }

        //判断是否有邀请的权限
        boolean isModify = EMClient.getInstance()
                .getCurrentUser().equals(owner) || group.isPublic();
        adapter = new GroupDetailAdapter(this, isModify, new GroupDetailAdapter.OnMembersChangeListener() {
            @Override
            public void onRemoveGroupMember(final UserInfo userInfo) {


                Modle.getInstance().getGlobalThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        /*
                        * 第一个参数是我们的群ID
                        * 第二个参数是我们的用户ID
                        *
                        * */
                        try {
                            //网络删除群成员
                            EMClient.getInstance().groupManager()
                                    .removeUserFromGroup(group.getGroupId(),userInfo.getHxid());
                            //网络获取群成员
                            getGroupMembers();

                            ShowToast.showUI(ChatDetailsActivity.this,"删除成功");
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                            ShowToast.showUI(ChatDetailsActivity.this,"删除失败"+e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onAddGroupMember(UserInfo userInfo) {
                ShowToast.show(ChatDetailsActivity.this,"添加成功");

                //跳转到选择好友界面
                Intent intent = new Intent(ChatDetailsActivity.this,PickContactActivity.class);
                intent.putExtra("groupid",group.getGroupId());
                startActivityForResult(intent,2);
            }
        });

        gvGroupDetail.setAdapter(adapter);
    }

    private void exitGroup() {

        //注意上下文
        LocalBroadcastManager manager = LocalBroadcastManager
                .getInstance(ImApplication.getContext());
        Intent intent = new Intent(Constant.DESTORY_GROUP);
        intent.putExtra("groupid",groupid);

        manager.sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2){

            //添加选中的群成员
            addMembers(data);
        }
    }

    private void addMembers(Intent data) {

        final String[] memberses = data.getStringArrayExtra("members");

        if (memberses == null || memberses.length ==0){
            return;
        }

        Modle.getInstance().getGlobalThread().execute(new Runnable() {
            @Override
            public void run() {

                try {
                    EMClient.getInstance().groupManager()
                            .addUsersToGroup(group.getGroupId(),memberses);

                    ShowToast.showUI(ChatDetailsActivity.this,"添加群成员成功");
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    ShowToast.showUI(ChatDetailsActivity.this,"添加群成员失败"+e.getMessage());
                }
            }
        });

    }
}
