package tech.jiangtao.support.ui.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import io.realm.Realm;
import io.realm.RealmResults;
import java.util.UUID;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import tech.jiangtao.support.kit.archive.MessageBody;
import tech.jiangtao.support.kit.archive.type.MessageAuthor;
import tech.jiangtao.support.kit.archive.type.MessageExtensionType;
import tech.jiangtao.support.kit.eventbus.RecieveLastMessage;
import tech.jiangtao.support.kit.eventbus.RecieveMessage;
import tech.jiangtao.support.kit.realm.MessageRealm;
import tech.jiangtao.support.kit.realm.SessionRealm;
import tech.jiangtao.support.kit.realm.VCardRealm;
import tech.jiangtao.support.kit.util.PinYinUtils;
import tech.jiangtao.support.ui.R;
import tech.jiangtao.support.ui.fragment.ChatFragment;
import uk.co.senab.photoview.log.LoggerDefault;
import xiaofei.library.hermeseventbus.HermesEventBus;

/**
 * Class: XMPPService </br>
 * Description: 进行数据库操作和通知的服务 </br>
 * 简单的进行双进程守护
 * Creator: kevin </br>
 * Email: jiangtao103cp@gmail.com </br>
 * Date: 31/12/2016 1:54 AM</br>
 * Update: 31/12/2016 1:54 AM </br>
 **/

public class XMPPService extends Service {

  public static final String TAG = XMPPService.class.getSimpleName();
  private Realm mRealm;
  private int i = 0;

  @Override public void onCreate() {
    super.onCreate();
    if (!HermesEventBus.getDefault().isRegistered(this)) {
      HermesEventBus.getDefault().register(this);
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (mRealm==null||mRealm.isClosed()){
      mRealm = Realm.getDefaultInstance();
    }
    return START_STICKY;
  }

  @Subscribe(threadMode = ThreadMode.MAIN) public void onRecieveMessage(RecieveMessage message) {
    //先保存会话表，然后保存到消息记录表
    mRealm.executeTransactionAsync(realm -> {
      RealmResults<SessionRealm> result = realm.where(SessionRealm.class)
          .equalTo("user_from", message.userJID)
          .equalTo("user_to", message.ownJid)
          .findAll();
      SessionRealm sessionRealm;
      if (result.size() != 0) {
        sessionRealm = result.first();
        sessionRealm.setMessage_id(message.id);
        sessionRealm.setUnReadCount(sessionRealm.getUnReadCount() + 1);
      } else {
        sessionRealm = new SessionRealm();
        sessionRealm.setSession_id(UUID.randomUUID().toString());
        sessionRealm.setUser_from(message.userJID);
        sessionRealm.setUser_to(message.ownJid);
        sessionRealm.setVcard_id(message.userJID);
        sessionRealm.setMessage_id(message.id);
        sessionRealm.setUnReadCount(1);
      }
      MessageRealm messageRealm = new MessageRealm();
      messageRealm.setId(message.id);
      messageRealm.setMainJID(message.userJID);
      messageRealm.setWithJID(message.ownJid);
      messageRealm.setTextMessage(message.message);
      messageRealm.setTime(null);
      messageRealm.setThread(message.thread);
      messageRealm.setType(message.type.toString());
      messageRealm.setMessageType(message.messageType.toString());
      messageRealm.setMessageStatus(false);
      realm.copyToRealmOrUpdate(sessionRealm);
      realm.copyToRealm(messageRealm);
    }, () -> {
      Log.d(TAG, "onSuccess: 保存消息成功");
      HermesEventBus.getDefault()
          .post(new RecieveLastMessage(message.id,message.type,message.userJID,message.ownJid,
              message.thread,message.message,message.messageType,false,
              message.messageAuthor));
      if (message.messageType== MessageExtensionType.TEXT) {
        showOnesNotification(message.userJID, message.message, new Intent(XMPPService.this, ChatFragment.class));
        //保存到本地数据库
      }
      if (message.messageType== MessageExtensionType.IMAGE) {
        showOnesNotification(message.userJID, "图片", new Intent(XMPPService.this, ChatFragment.class));
        //保存到本地数据库
      }
      if (message.messageType== MessageExtensionType.AUDIO) {
        showOnesNotification(message.userJID, "音频", new Intent(XMPPService.this, ChatFragment.class));
        //保存到本地数据库
      }
      if (message.messageType== MessageExtensionType.VIDEO) {
        showOnesNotification(message.userJID, "视频", new Intent(XMPPService.this, ChatFragment.class));
        //保存到本地数据库
      }
    }, new Realm.Transaction.OnError() {
      @Override public void onError(Throwable error) {
        Log.d(TAG, "onError: 保存消息失败"+error.getMessage());
      }
    });
  }

  @Subscribe(threadMode = ThreadMode.MAIN) public void onVCardRealmMessage(VCardRealm realmObject) {
    mRealm.executeTransactionAsync(realm -> {
      RealmResults<VCardRealm> result =
          realm.where(VCardRealm.class).equalTo("jid", realmObject.getJid()).findAll();
      if (result.size() != 0) {
        VCardRealm realmUpdate = result.first();
        realmUpdate.setNickName(realmObject.getNickName());
        realmUpdate.setSex(realmObject.getSex());
        realmUpdate.setSubject(realmObject.getSubject());
        realmUpdate.setOffice(realmObject.getOffice());
        realmUpdate.setEmail(realmObject.getEmail());
        realmUpdate.setPhoneNumber(realmObject.getPhoneNumber());
        realmUpdate.setSignature(realmObject.getSignature());
        realmUpdate.setAvatar(realmObject.getAvatar());
        if (realmUpdate.getNickName() != null) {
          realmUpdate.setAllPinYin(PinYinUtils.ccs2Pinyin(realmObject.getNickName()));
          realmUpdate.setFirstLetter(PinYinUtils.getPinyinFirstLetter(realmObject.getNickName()));
        }
        realmUpdate.setFriend(true);
      } else {
        realm.copyToRealm(realmObject);
      }
    }, () -> {
      Log.d(TAG, "onSuccess: 执行成功");
      //发送消息更新，应该也可以不用发送消息
    }, error -> Log.d(TAG, "onError: 通讯录后台执行错误，错误信息" + error.getMessage()));
  }

  /**
   * 显示通知
   */
  public void showNotification(String name, String info) {
    Notification.Builder builder = new Notification.Builder(this);
    Intent intent = new Intent(this, ChatFragment.class);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      builder.setContentIntent(
          PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT))
          .setContentTitle(name)
          .setContentText(info)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setWhen(System.currentTimeMillis())
          .setPriority(Notification.PRIORITY_HIGH)
          .setDefaults(Notification.DEFAULT_VIBRATE);
      Notification notification = builder.build();
      notification.flags = Notification.FLAG_ONGOING_EVENT;
      notification.defaults = Notification.DEFAULT_SOUND;
      startForeground(110, notification);
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mRealm.close();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  public void showOnesNotification(String name, String info, Intent intent) {
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    Notification.Builder builder = new Notification.Builder(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      builder.setContentIntent(
          PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT))
          .setContentTitle(name)
          .setContentText(info)
          .setSmallIcon(tech.jiangtao.support.kit.R.mipmap.ic_launcher)
          .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
              tech.jiangtao.support.kit.R.mipmap.ic_launcher))
          .setWhen(System.currentTimeMillis())
          .setPriority(Notification.PRIORITY_HIGH)
          .setDefaults(Notification.DEFAULT_VIBRATE);
      Notification notification = builder.build();
      notification.flags = Notification.FLAG_AUTO_CANCEL;
      notification.defaults = Notification.DEFAULT_SOUND;
      mNotificationManager.notify(0, notification);
    }
  }

  public void saveOfflineMessage(){
    //MessageRealm messageRealm = new MessageRealm();
    //messageRealm.setId(message.id);
    //messageRealm.setMainJID(message.userJID);
    //messageRealm.setWithJID(message.ownJid);
    //messageRealm.setTextMessage(message.message);
    //messageRealm.setThread(message.thread);
    //messageRealm.setType(message.type.toString());
    //messageRealm.setMessageType(message.messageType.toString());
    //messageRealm.setMessageStatus(message.readStatus);
    //realm.copyToRealm(messageRealm);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void messageAchieve(MessageBody messagebody){
    Log.d(TAG, "messageAchieve: "+messagebody.toString()+"总数为:"+i);
    i++;
  }
}