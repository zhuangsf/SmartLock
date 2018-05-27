package com.oulu.lock.beans;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by liao on 2017/12/6.
 */
@Entity
public class LockBean {
    @Id(autoincrement = true)
    private Long  id;
    private String lockName;
    @Unique
    private String address;
    private String customName;
    private int isable;
    @Generated(hash = 757315465)
    public LockBean(Long id, String lockName, String address, String customName,
            int isable) {
        this.id = id;
        this.lockName = lockName;
        this.address = address;
        this.customName = customName;
        this.isable = isable;
    }
    @Generated(hash = 1993894450)
    public LockBean() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getLockName() {
        return this.lockName;
    }
    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getCustomName() {
        return this.customName;
    }
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    public int getIsable() {
        return this.isable;
    }
    public void setIsable(int isable) {
        this.isable = isable;
    }



}
