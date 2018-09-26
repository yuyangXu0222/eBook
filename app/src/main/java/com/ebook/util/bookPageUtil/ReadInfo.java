package com.ebook.util.bookPageUtil;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/23.
 */

public class ReadInfo implements Serializable {
    public int nextParaIndex;            //即将读取的段落的索引
    public boolean isLastNext = true;           //上一次阅读是向后阅读还是向前阅读
    public boolean isNextRes;              //往后读是否剩余字符串
    public boolean isPreRes;                       //往前读是否剩余字符串
    public List<String> preResLines = new ArrayList<>();        //上一次向前读剩余的line
    public List<String> nextResLines = new ArrayList<>();       //上一次向后读剩余的line

}
