package com.ebook.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mum on 2017/1/24.
 */

public class BookLab {
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    private static BookLab sBookLab;

    private AssetManager mAssetManager;
    private List<Book> mBookList;
    //assets中的文件名清单
    private String[] mAssetsImageList;
    private String[] mAssetsTextList;


    private BookLab(Context context) {
        mAssetManager = context.getAssets();
        loadAssetsFiles();
    }


    public static BookLab newInstance(Context context) {
        if (sBookLab == null) {
            sBookLab = new BookLab(context);
        }
        return sBookLab;
    }

    //加载assets中的文件
    private void loadAssetsFiles() {
        mBookList = new ArrayList<>();
        //获取image、text中的文件名清单
        try {
            mAssetsImageList = mAssetManager.list(IMAGE);
            mAssetsTextList = mAssetManager.list(TEXT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < mAssetsTextList.length; i++) {
            //获取书名
            String[] nameSplit = mAssetsTextList[i].split("_");
            String nameSecond = nameSplit[nameSplit.length - 1];
            String bookTitle = nameSecond.replace(".txt", "");

            //获取封面
            String imagePath = IMAGE + "/" + mAssetsImageList[i];
            Bitmap bookCover = loadImage(imagePath);

            //获取文本
            String textPath = TEXT + "/" + mAssetsTextList[i];
            String bodyText = loadText(textPath);


            Book book = new Book(bookTitle, bookCover, bodyText);
            mBookList.add(book);

        }

    }


    //从assets中读取文本
    private String loadText(String path) {
        InputStream in = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            in = mAssetManager.open(path);
            reader = new BufferedReader(new InputStreamReader(in));

            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return stringBuilder.toString();

    }

    //从assets中读取图片
    private Bitmap loadImage(String path) {
        Bitmap image = null;
        InputStream in = null;
        try {
            in = mAssetManager.open(path);
            image = BitmapFactory.decodeStream(in);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return image;
    }

    public List<Book> getBookList() {
        return mBookList;
    }
}
