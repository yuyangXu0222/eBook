package com.ebook.util.bookPageUtil;


import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2017/2/27.
 */

public class Label extends DataSupport{
    private int mBookId;
    private String mDetails;
    private String mProgress;
    private String mTime;
    private boolean isPrePageOver;
    private String readInfoStr; //readInfo对象序列化编码后的String


    public int getBookId() {
        return mBookId;
    }


    public boolean isPrePageOver() {
        return isPrePageOver;
    }

    public String getTime() {
        return mTime;
    }

    public String getProgress() {
        return mProgress;
    }

    public String getDetails() {
        return mDetails;
    }

    public String getReadInfoStr() {
        return readInfoStr;
    }


    public void setBookId(int bookId) {
        mBookId = bookId;
    }

    public void setDetails(String details) {
        mDetails = details;
    }

    public void setProgress(String progress) {
        mProgress = progress;
    }

    public void setTime(String time) {
        mTime = time;
    }

    public void setPrePageOver(boolean prePageOver) {
        isPrePageOver = prePageOver;
    }


    public void setReadInfoStr(String readInfoStr) {
        this.readInfoStr = readInfoStr;
    }
}
