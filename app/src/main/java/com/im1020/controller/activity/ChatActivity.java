package com.im1020.controller.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.im1020.R;
import com.im1020.utils.Constant;
import com.im1020.utils.ShowToast;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity {

    @Bind(R.id.chat_fl)
    FrameLayout chatFl;
    private EaseChatFragment chatFragment;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String id = intent.getStringExtra("groupid");

            if (groupid.equals(id)){
                finish();
            }
        }
    };
    private String groupid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);


        initData();
        initListener();
    }

    private void initListener() {


        chatFragment.setChatFragmentListener(new EaseChatFragment.EaseChatFragmentHelper() {
            @Override
            public void onSetMessageAttributes(EMMessage message) {


            }

            @Override
            public void onEnterToChatDetails() {


                Intent intent = new Intent(ChatActivity.this,ChatDetailsActivity.class);
                //判断是否是群组
               /* if (getIntent().getExtras().get(EaseConstant.EXTRA_CHAT_TYPE)
                        ==EaseConstant.CHATTYPE_GROUP){

                }*/
                intent.putExtra("groupid",getIntent().getExtras()
                        .getString(EaseConstant.EXTRA_USER_ID));
                startActivity(intent);

            }

            @Override
            public void onAvatarClick(String username) {

            }

            @Override
            public void onAvatarLongClick(String username) {

            }

            @Override
            public boolean onMessageBubbleClick(EMMessage message) {
                return false;
            }

            @Override
            public void onMessageBubbleLongClick(EMMessage message) {

            }

            @Override
            public boolean onExtendMenuItemClick(int itemId, View view) {
                return false;
            }

            @Override
            public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
                return null;
            }
        });
    }

    private void initData() {

        //聊天的fragment
        chatFragment = new EaseChatFragment();

        chatFragment.setArguments(getIntent().getExtras());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.chat_fl, chatFragment).commit();

        //判断类型
        int type = getIntent().getExtras().getInt(EaseConstant.EXTRA_CHAT_TYPE);
        //获取用户或者群ID
        groupid = getIntent().getExtras().getString(EaseConstant.EXTRA_USER_ID);
        //在群页面才进行退群注册
        if (type == EaseConstant.CHATTYPE_GROUP){
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
            manager.registerReceiver(receiver,new IntentFilter(Constant.DESTORY_GROUP));
        }
    }
}
