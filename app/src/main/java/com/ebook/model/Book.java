package com.ebook.model;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mum on 2017/1/24.
 */
public class Book {
    private static final String TAG = "Book";
    private String mBookTitle;
    private Bitmap mBookCover;
    //格式化文本，将文本以段落为单位保存
    private List<String> mParagraphList;

    //目录集合(卷/章/回/集等)
    private List<String> mBookContents;
    //目录对应的在段落集合中的索引
    private List<Integer> mContentParaIndexs;

    //空两格
    private String mSpace = "\t\t\t\t\t\t";

    public Book(String bookTitle, Bitmap bookCover, String fullText) {
        mParagraphList = new ArrayList<>();
        mBookContents = new ArrayList<>();
        mContentParaIndexs=new ArrayList<>();

        mBookTitle = bookTitle;
        mBookCover = bookCover;

        formatText(fullText);

        findContents(mParagraphList);
    }

    //格式化文本，将文本以段落为单位保存
    private void formatText(String text) {
        boolean isFirstParas = true;
        String paragraph = "";

        //按段落切分文本
        String[] paragraphs = text.split("\\s{2,}");
        //格式化段落
        for (int i = 0; i < paragraphs.length; i++) {
            if (paragraphs[i].isEmpty()) {
                continue;
            }
            if (isFirstParas) {
                paragraph = mSpace + paragraphs[i];
                isFirstParas = false;
            } else {
                paragraph = "\n" + mSpace + paragraphs[i];

            }

            mParagraphList.add(paragraph);

        }

    }



    private void findContents(List<String> paraList) {
        //字符串匹配模式
        String patternString = "第\\S{2,4}\\s\\S{2,}";
        Pattern pattern = Pattern.compile(patternString);

        for (String para:paraList) {

            Matcher matcher = pattern.matcher(para);

           if (matcher.find()){

               //除去段首多余空格
                int start = matcher.start();
                int end = matcher.end();
                String subString = para.substring(start, end);

                mBookContents.add(subString);   //目录
                mContentParaIndexs.add(paraList.indexOf(para)); //目录对应的在段落集合中的索引

            }

        }

    }


    public String getBookTitle() {
        return mBookTitle;
    }

    public Bitmap getBookCover() {
        return mBookCover;
    }

    public List<String> getParagraphList() {
        return mParagraphList;
    }

    public List<String> getBookContents() {
        return mBookContents;
    }

    public List<Integer> getContentParaIndexs() {
        return mContentParaIndexs;
    }
}
