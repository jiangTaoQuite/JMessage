package tech.jiangtao.support.ui.pattern;

import android.support.annotation.DrawableRes;

import java.util.List;

import tech.jiangtao.support.kit.realm.ContactRealm;
import tech.jiangtao.support.kit.model.type.ContactType;

/**
 * Class: ConstrutContact </br>
 * Description: 构造通讯录界面 </br>
 * Creator: kevin </br>
 * Email: jiangtao103cp@gmail.com </br>
 * Date: 04/12/2016 9:40 PM</br>
 * Update: 04/12/2016 9:40 PM </br>
 **/

public class ConstrutContact {

  public ContactType mType;
  public @DrawableRes int mId;
  public String mTitle;
  public Object mObject;
  public List<?> mDatas;
  public String mSubtitle;
  public ContactRealm mContactRealm;
  public String mImg;
  public boolean mIsChecked;

  public static class Builder {
    public ContactType type;
    public @DrawableRes int id;
    public String title;
    public Object object;
    public List<?> datas;
    public String subtitle;
    public ContactRealm contactRealm;
    public String img;
    public boolean isChecked;

    public ConstrutContact build() {
      ConstrutContact contact = new ConstrutContact();
      contact.mType = type;
      contact.mId = id;
      contact.mTitle = title;
      contact.mObject = object;
      contact.mDatas = datas;
      contact.mSubtitle = subtitle;
      contact.mContactRealm = contactRealm;
      contact.mImg = img;
      contact.mIsChecked = isChecked;
      return contact;
    }

    public Builder type(ContactType type1) {
      this.type = type1;
      return this;
    }

    public Builder id(int id1) {
      this.id = id1;
      return this;
    }

    public Builder title(String text) {
      this.title = text;
      return this;
    }

    public Builder object(Object object) {
      this.object = object;
      return this;
    }

    public Builder datas(List<?> datas) {
      this.datas = datas;
      return this;
    }

    public Builder subtitle(String title) {
      this.subtitle = title;
      return this;
    }

    public Builder contactRealm(ContactRealm contactRealm) {
      this.contactRealm = contactRealm;
      return this;
    }

    public Builder img(String img) {
      this.img = img;
      return this;
    }

    public Builder isChecked(boolean isChecked) {
      this.isChecked = isChecked;
      return this;
    }
  }
}
