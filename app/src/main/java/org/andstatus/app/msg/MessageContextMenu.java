/*
 * Copyright (C) 2013-2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.msg;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.TextView;

import org.andstatus.app.IntentExtra;
import org.andstatus.app.R;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.data.MatchedUri;
import org.andstatus.app.data.MessageForAccount;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.util.MyLog;

/**
 * Context menu and corresponding actions on messages from the list 
 * @author yvolk@yurivolkov.com
 */
public class MessageContextMenu implements OnCreateContextMenuListener {

    public final ActionableMessageList messageList;
    
    private View viewOfTheContext = null;
    /**
     * Id of the Message that was selected (clicked, or whose context menu item
     * was selected) TODO: clicked, restore position...
     */
    private long mMsgId = 0;
    /**
     *  Corresponding account information ( "Reply As..." ... ) 
     *  oh whose behalf we are going to execute an action on this line in the list (message...) 
     */
    private long mActorUserIdForCurrentMessage = 0;
    public String imageFilename = null;

    private long mAccountUserIdToActAs;

    public MessageContextMenu(ActionableMessageList actionableMessageList) {
        messageList = actionableMessageList;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        final String method = "onCreateContextMenu";
        long userIdForThisMessage = mAccountUserIdToActAs;
        viewOfTheContext = v;
        String logMsg = method;
        if (menuInfo != null) {
            AdapterView.AdapterContextMenuInfo info;
            try {
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                MyLog.e(this, method + "; bad menuInfo", e);
                return;
            }

            mMsgId = info.id;
            logMsg += "; info.id=" + mMsgId + "; position=" + info.position;
            messageList.getActivity().setPositionOfContextMenu(info.position);
            if (userIdForThisMessage == 0) {
                userIdForThisMessage = messageList.getLinkedUserIdFromCursor(info.position);
            }

        } else {
            messageList.getActivity().setPositionOfContextMenu(-1);
            TextView id = (TextView) v.findViewById(R.id.id);
            mMsgId = Long.parseLong(id.getText().toString());
            logMsg += "; idView.text=" + mMsgId;
            if (userIdForThisMessage == 0) {
                TextView linkedUserId = (TextView) v.findViewById(R.id.linked_user_id);
                String strUserId = linkedUserId.getText().toString();
                if (!TextUtils.isEmpty(strUserId)) {
                    userIdForThisMessage = Long.parseLong(strUserId);
                }
            }
        }
        mActorUserIdForCurrentMessage = 0;
        if (mMsgId <= 0) {
            mMsgId = 0;
            return;
        }
        MyLog.v(this, logMsg);
        MessageForAccount msg = getMessageForAccount(userIdForThisMessage, getCurrentMyAccountUserId());
        if (!msg.myAccount().isValid()) {
            return;
        }
        mActorUserIdForCurrentMessage = msg.myAccount().getUserId();
        mAccountUserIdToActAs = 0;

        int order = 0;
        // Create the Context menu
        try {
            menu.setHeaderTitle((MyContextHolder.get().persistentAccounts().size() > 1
                    ? msg.myAccount().shortestUniqueAccountName() + ": " : "")
                    + msg.bodyTrimmed);

            if (msg.status != DownloadStatus.LOADED) {
                ContextMenuItem.EDIT.addTo(menu, order++, R.string.menu_item_edit);
            }
            if (msg.status.mayBeSent()) {
                ContextMenuItem.RESEND.addTo(menu, order++, R.string.menu_item_resend);
            }
            if (isEditorVisible()) {
                ContextMenuItem.COPY_TEXT.addTo(menu, order++, R.string.menu_item_copy_text);
                ContextMenuItem.COPY_AUTHOR.addTo(menu, order++, R.string.menu_item_copy_author);
            }
            if (msg.isLoaded() && !msg.isDirect() && !isEditorVisible()) {
                ContextMenuItem.REPLY.addTo(menu, order++, R.string.menu_item_reply);
                ContextMenuItem.REPLY_ALL.addTo(menu, order++, R.string.menu_item_reply_all);
            }
            ContextMenuItem.SHARE.addTo(menu, order++, R.string.menu_item_share);
            if (!TextUtils.isEmpty(msg.imageFilename)) {
                imageFilename = msg.imageFilename;
                ContextMenuItem.VIEW_IMAGE.addTo(menu, order++, R.string.menu_item_view_image);
            }

            if (!isEditorVisible()) {
                // TODO: Only if he follows me?
                ContextMenuItem.DIRECT_MESSAGE.addTo(menu, order++,
                        R.string.menu_item_direct_message);
            }

            if (msg.isLoaded() && !msg.isDirect()) {
                if (msg.favorited) {
                    ContextMenuItem.DESTROY_FAVORITE.addTo(menu, order++,
                            R.string.menu_item_destroy_favorite);
                } else {
                    ContextMenuItem.FAVORITE.addTo(menu, order++,
                            R.string.menu_item_favorite);
                }
                if (msg.reblogged) {
                    ContextMenuItem.DESTROY_REBLOG.addTo(menu, order++,
                            msg.myAccount().alternativeTermForResourceId(R.string.menu_item_destroy_reblog));
                } else {
                    // Don't allow a User to reblog himself
                    if (mActorUserIdForCurrentMessage != msg.senderId) {
                        ContextMenuItem.REBLOG.addTo(menu, order++,
                                msg.myAccount().alternativeTermForResourceId(R.string.menu_item_reblog));
                    }
                }
            }

            if (messageList.getSelectedUserId() != msg.senderId) {
                /*
                 * Messages by the Sender of this message ("User timeline" of
                 * that user)
                 */
                ContextMenuItem.SENDER_MESSAGES.addTo(menu, order++,
                        String.format(
                                getContext().getText(R.string.menu_item_user_messages).toString(),
                                MyQuery.userIdToWebfingerId(msg.senderId)));
            }

            if (messageList.getSelectedUserId() != msg.authorId && msg.senderId != msg.authorId) {
                /*
                 * Messages by the Author of this message ("User timeline" of
                 * that user)
                 */
                ContextMenuItem.AUTHOR_MESSAGES.addTo(menu, order++,
                        String.format(
                                getContext().getText(R.string.menu_item_user_messages).toString(),
                                MyQuery.userIdToWebfingerId(msg.authorId)));
            }

            if (msg.isLoaded()) {
                ContextMenuItem.OPEN_MESSAGE_PERMALINK.addTo(menu, order++, R.string.menu_item_open_message_permalink);
                ContextMenuItem.OPEN_CONVERSATION.addTo(menu, order++, R.string.menu_item_open_conversation);
            }

            if (msg.isSender) {
                // This message is by current User, hence we may delete it.
                if (msg.isDirect()) {
                    // This is a Direct Message
                    // TODO: Delete Direct message
                } else if (!msg.reblogged) {
                    ContextMenuItem.DESTROY_STATUS.addTo(menu, order++,
                            R.string.menu_item_destroy_status);
                }
            }

            if (!msg.isSender) {
                if (msg.senderFollowed) {
                    ContextMenuItem.STOP_FOLLOWING_SENDER.addTo(menu, order++,
                            String.format(
                                    getContext().getText(R.string.menu_item_stop_following_user).toString(),
                                    MyQuery.userIdToWebfingerId(msg.senderId)));
                } else {
                    ContextMenuItem.FOLLOW_SENDER.addTo(menu, order++,
                            String.format(
                                    getContext().getText(R.string.menu_item_follow_user).toString(),
                                    MyQuery.userIdToWebfingerId(msg.senderId)));
                }
            }
            if (!msg.isAuthor && (msg.authorId != msg.senderId)) {
                if (msg.authorFollowed) {
                    ContextMenuItem.STOP_FOLLOWING_AUTHOR.addTo(menu, order++,
                            String.format(
                                    getContext().getText(R.string.menu_item_stop_following_user).toString(),
                                    MyQuery.userIdToWebfingerId(msg.authorId)));
                } else {
                    ContextMenuItem.FOLLOW_AUTHOR.addTo(menu, order++,
                            String.format(
                                    getContext().getText(R.string.menu_item_follow_user).toString(),
                                    MyQuery.userIdToWebfingerId(msg.authorId)));
                }
            }
            if (msg.isLoaded()) {
                switch (msg.myAccount().numberOfAccountsOfThisOrigin()) {
                    case 1:
                        break;
                    case 2:
                        ContextMenuItem.ACT_AS_USER.addTo(menu, order++,
                                String.format(
                                        getContext().getText(R.string.menu_item_act_as_user).toString(),
                                        msg.myAccount().firstOtherAccountOfThisOrigin().shortestUniqueAccountName()));
                        break;
                    default:
                        ContextMenuItem.ACT_AS.addTo(menu, order++, R.string.menu_item_act_as);
                        break;
                }
            }
        } catch (Exception e) {
            MyLog.e(this, "onCreateContextMenu", e);
        }
    }

    private MessageForAccount getMessageForAccount(long userIdForThisMessage, long preferredUserId) {
        MyAccount ma1 = MyContextHolder.get().persistentAccounts()
                .getAccountForThisMessage(mMsgId, userIdForThisMessage,
                        preferredUserId,
                        false);
        MessageForAccount msg = new MessageForAccount(mMsgId, ma1);
        boolean forceFirstUser = mAccountUserIdToActAs !=0;
        if (ma1.isValid() && !forceFirstUser
                && !msg.isTiedToThisAccount()
                && ma1.getUserId() != preferredUserId
                && messageList.getTimelineType() != TimelineType.FOLLOWING_USER) {
            MyAccount ma2 = MyContextHolder.get().persistentAccounts().fromUserId(preferredUserId);
            if (ma2.isValid() && ma1.getOriginId() == ma2.getOriginId()) {
                msg = new MessageForAccount(mMsgId, ma2);
            }
        }
        return msg;
    }

    private boolean isEditorVisible() {
        return messageList.getMessageEditor().isVisible();
    }

    protected long getCurrentMyAccountUserId() {
        return messageList.getCurrentMyAccountUserId();
    }

    protected Context getContext() {
        return messageList.getActivity();
    }
    
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        String msgInfo = "";
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            if (info != null) {
                mMsgId = info.id;
            } else {
                msgInfo = "; info==null";
            }
        } catch (ClassCastException e) {
            MyLog.e(this, "bad menuInfo", e);
            return false;
        }
        if (mMsgId == 0) {
            MyLog.e(this, "message id == 0");
            return false;
        }

        MyAccount ma = MyContextHolder.get().persistentAccounts().fromUserId(mActorUserIdForCurrentMessage);
        if (ma.isValid()) {
            ContextMenuItem contextMenuItem = ContextMenuItem.fromId(item.getItemId());
            MyLog.v(this, "onContextItemSelected: " + contextMenuItem + "; actor=" + ma.getAccountName() + "; msgId=" + mMsgId + msgInfo);
            return contextMenuItem.execute(this, ma);
        } else {
            return false;
        }
    }

    public void switchTimelineActivity(TimelineType timelineType, boolean isTimelineCombined, long selectedUserId) {
        Intent intent;
        if (MyLog.isVerboseEnabled()) {
            MyLog.v(this, "switchTimelineActivity; " + timelineType 
                    + "; isCombined=" + (isTimelineCombined ? "yes" : "no")
                    + "; userId=" + selectedUserId);
        }
        
        // Actually we use one Activity for all timelines...
        intent = new Intent(getContext(), TimelineActivity.class);
        intent.setData(MatchedUri.getTimelineUri(MyContextHolder.get().persistentAccounts()
                .getCurrentAccountUserId(), TimelineTypeSelector.selectableType(timelineType),
                isTimelineCombined, selectedUserId));
        // We don't use Intent.ACTION_SEARCH action anywhere, so there is no need it setting it.
        // - we're analyzing query instead!
        messageList.getActivity().startActivity(intent);
    }

    public void showContextMenu() {
        if (viewOfTheContext != null) {
            viewOfTheContext.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        viewOfTheContext.showContextMenu();
                    } catch (NullPointerException e) {
                        MyLog.d(this, "on showContextMenu; " + (viewOfTheContext != null ? "viewOfTheContext is not null" : ""), e);
                    }
                }
            });                    
        }
    }
    
    public void loadState(SharedPreferences savedInstanceState) {
        if (savedInstanceState != null 
                && savedInstanceState.contains(IntentExtra.ITEMID.key)) {
            mMsgId = savedInstanceState.getLong(IntentExtra.ITEMID.key, 0);
        }
    }

    public void saveState(Editor outState) {
        if (outState != null) {
            outState.putLong(IntentExtra.ITEMID.key, mMsgId);
        }
    }

    public long getMsgId() {
        return mMsgId;
    }

    public void setAccountUserIdToActAs(long accountUserIdToActAs) {
        this.mAccountUserIdToActAs = accountUserIdToActAs;
    }

    public long getActorUserIdForCurrentMessage() {
        return mActorUserIdForCurrentMessage;
    }

}
