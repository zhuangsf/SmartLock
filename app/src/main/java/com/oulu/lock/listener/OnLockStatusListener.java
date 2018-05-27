package com.oulu.lock.listener;

/**
 * Created by liao on 2018/5/19.
 */

public interface OnLockStatusListener {
    public void getLogin(boolean flag);
    public void getToken(String  token);
    public void getBar(String  power);
    public void getUnlock(boolean flag);
    public void getLock(boolean flag);
    public void getModifyName(boolean flag);
    public void getPassword(boolean flag);
    public void getLockstatus(boolean flag);

}
