package com.ebook.util.bookPageUtil;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.ebook.model.Book;
import com.ebook.model.BookLab;
import com.ebook.util.SaveHelper;
import com.ebook.view.popupWindow.FontPopup;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mum on 2017/2/6.
 */

public class BookPageFactory {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private int marginWidth;
    private int marginHeight;
    private int mBookId;

    //绘制正文区域
    private float mVisibleWidth;
    private float mVisibleHeight;

    private float mLineHeight; //行高
    private int mLineCount; //一页能容纳的行数

    private List<String> mParaList; //文本段落集合
    private List<String> mContents;    //目录集合(卷/章/回/集等)
    private List<Integer> mContentParaIndex;   //目录对应的在段落集合中的索引
    private int mParaListSize;

    private List<String> mPageLines = new ArrayList<>();
    private String mCurContent;//当前page对应的目录
    private Paint mPaint;

    private int[] mBgColors;
    private int[] mTextColors;

    private List<Typeface> mTypefaceList = new ArrayList<>();

    private PaintInfo mPaintInfo;
    private ReadInfo mReadInfo;
    private String percentStr;


    public BookPageFactory(Context context, int bookId) {
        mContext = context;
        mBookId = bookId;
        calWidthAndHeight();
        getFontFromAssets();

        initDatas();
    }

    private void initDatas() {
        Book book = BookLab.newInstance(mContext).getBookList().get(mBookId);
        mParaList = book.getParagraphList();
        mParaListSize = mParaList.size();
        mContents = book.getBookContents();
        mContentParaIndex = book.getContentParaIndexs();

        marginWidth = (int) (mWidth / 30f);
        marginHeight = (int) (mHeight / 60f);
        mVisibleWidth = mWidth - marginWidth * 2;
        mVisibleHeight = mHeight - marginHeight * 2;

        mBgColors = new int[]{
                0xffe7dcbe,  //复古
                0xffffffff,  // 常规
                0xffcbe1cf,  //护眼
                0xff333232  //夜间
        };

        mTextColors = new int[]{
                0x8A000000,
                0x8A000000,
                0x8A000000,
                0xffa9a8a8   //夜间
        };


        PaintInfo paintInfo = SaveHelper.getObject(mContext, SaveHelper.PAINT_INFO);
        if (paintInfo != null)
            mPaintInfo = paintInfo;
        else
            mPaintInfo = new PaintInfo();

        mLineHeight = mPaintInfo.textSize * 1.5f;
        mLineCount = (int) (mVisibleHeight / mLineHeight) - 1;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.LEFT);

        mPaint.setColor(mPaintInfo.textColor);
        mPaint.setTextSize(mPaintInfo.textSize);
        mPaint.setTypeface(mTypefaceList.get(mPaintInfo.typeIndex));

        ReadInfo info = SaveHelper.getObject(mContext, mBookId + SaveHelper.DRAW_INFO);
        if (info != null)
            mReadInfo = info;
        else
            mReadInfo = new ReadInfo();
    }

    public Bitmap drawNextPage(float powerPercent) {
        if (!mReadInfo.isLastNext) {
            pageDown();
            mReadInfo.isLastNext = true;

        }

        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(mPaintInfo.bgColor);

        //下一页
        mPageLines = getNextPageLines();
        //已经到最后一页了
        if (mPageLines.size() == 0 || mPageLines == null) {
            return null;
        }

        float y = mPaintInfo.textSize;

        for (String strLine : mPageLines) {
            y += mLineHeight;
            canvas.drawText(strLine, marginWidth, y, mPaint);
        }

        //绘制显示在底部的信息
        drawInfo(canvas, powerPercent);

        return bitmap;

    }


    public Bitmap drawPrePage(float powerPercent) {
        if (mReadInfo.isLastNext) {
            pageUp();

            mReadInfo.isLastNext = false;
        }

        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(mPaintInfo.bgColor);

        //下一页
        mPageLines = getPrePageLines();
        //已经到第一页了
        if (mPageLines.size() == 0 || mPageLines == null) {
            return null;
        }

        float y = mPaintInfo.textSize;

        for (String strLine : mPageLines) {
            y += mLineHeight;
            canvas.drawText(strLine, marginWidth, y, mPaint);
        }

        //绘制显示的信息
        drawInfo(canvas, powerPercent);

        return bitmap;

    }


    public List<Bitmap> updatePagesByContent(int nextParaIndex, float powerPercent) {
        mReadInfo.nextParaIndex = nextParaIndex;

        if (mReadInfo.nextParaIndex == 1)    //第一章和卷名一起处理
            mReadInfo.nextParaIndex = 0;
        reset();

        mReadInfo.isLastNext = true;//设置为直接往后读
        List<Bitmap> bitmaps = new ArrayList<>();
        bitmaps.add(drawNextPage(powerPercent));
        bitmaps.add(drawNextPage(powerPercent));

        return bitmaps;

    }


    public List<Bitmap> updateTheme(int theme, float powerPercent) {
        mPaintInfo.bgColor = mBgColors[theme];
        mPaintInfo.textColor = mTextColors[theme];
        return drawCurTwoPages(powerPercent);
    }

    public List<Bitmap> updateTypeface(int typeIndex, float powerPercent) {
        mPaintInfo.typeIndex = typeIndex;
        return drawCurTwoPages(powerPercent);
    }

    public List<Bitmap> updateTextSize(int textSize, float powerPercent) {
        mPaintInfo.textSize = textSize;
        mLineHeight = textSize * 1.5f;
        mLineCount = (int) (mVisibleHeight / mLineHeight) - 1;
        return drawCurTwoPages(powerPercent);
    }

    public List<Bitmap> updateTextColor(int textColor, float powerPercent) {
        mPaintInfo.textColor = textColor;
        return drawCurTwoPages(powerPercent);
    }

    public List<Bitmap> drawCurTwoPages(float powerPercent) {

        setIndexToCurStart();

        mPaint.setColor(mPaintInfo.textColor);
        mPaint.setTextSize(mPaintInfo.textSize);
        mPaint.setTypeface(mTypefaceList.get(mPaintInfo.typeIndex));

        List<Bitmap> bitmaps = new ArrayList<>();
        if (mReadInfo.isLastNext) {
            bitmaps.add(drawNextPage(powerPercent));
            bitmaps.add(drawNextPage(powerPercent));
        } else {
            bitmaps.add(drawPrePage(powerPercent));
            bitmaps.add(0, drawPrePage(powerPercent));
        }

        return bitmaps;
    }

    private void setIndexToCurStart() {

        if (mReadInfo.isLastNext) {
            pageUp();
            mReadInfo.nextParaIndex += 1;

            if (!mReadInfo.isPreRes)
                return;

            String string = mParaList.get(mReadInfo.nextParaIndex);

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                mReadInfo.nextResLines.add(string.substring(0, size));

                string = string.substring(size);

            }

            mReadInfo.nextResLines.clear();
            mReadInfo.isNextRes = true;
            mReadInfo.nextParaIndex += 1;

            mReadInfo.preResLines.clear();
            mReadInfo.isPreRes = false;


        } else {
            pageDown();
            mReadInfo.nextParaIndex -= 1;

            if (!mReadInfo.isNextRes)
                return;

            String string = mParaList.get(mReadInfo.nextParaIndex);

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                mReadInfo.preResLines.add(string.substring(0, size));

                string = string.substring(size);

            }

            mReadInfo.preResLines.removeAll(mReadInfo.nextResLines);
            mReadInfo.isPreRes = true;
            mReadInfo.nextParaIndex -= 1;

            mReadInfo.nextResLines.removeAll(mReadInfo.preResLines);
            mReadInfo.isNextRes = false;


        }


    }

    private String findContent(int paraIndex) {    //找到当前page对应的目录
        for (int i = 0; i < mContentParaIndex.size() - 1; i++) {
            if (paraIndex >= mContentParaIndex.get(i) && paraIndex < mContentParaIndex.get(i + 1)) {
                if (i == 0)
                    i = 1;   //合并卷名和第一章

                return mContents.get(i);
            }

        }

        return mContents.get(mContentParaIndex.size() - 1);

    }

    private void drawInfo(Canvas canvas, float powerPercent) {

        Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoPaint.setTextAlign(Paint.Align.LEFT);
        infoPaint.setTextSize(32);
        infoPaint.setColor(0xff5c5c5c);

        float offsetY = mHeight - marginHeight;

        //当前page对应的目录
        canvas.drawText(mCurContent, marginWidth, marginHeight, infoPaint);

        //阅读进度
        float percent = mReadInfo.nextParaIndex * 1.0f / mParaListSize;
        DecimalFormat df = new DecimalFormat("#0.00");
        percentStr = df.format(percent * 100) + "%";
        canvas.drawText(percentStr, marginWidth, offsetY, infoPaint);

        //当前系统时间
        Time time = new Time();
        time.setToNow(); // 取得系统时间。
        int hour = time.hour;    // 0-23
        int minute = time.minute;
        String timeStr = "";
        if (minute < 10) {
            timeStr = hour + ":0" + minute;
        } else {
            timeStr = hour + ":" + minute;
        }
        canvas.drawText(timeStr, mWidth - 7f * marginWidth, offsetY, infoPaint);

        //电池电量
        infoPaint.reset();
        infoPaint.setStyle(Paint.Style.STROKE);
        infoPaint.setStrokeWidth(1);
        infoPaint.setColor(0xff5c5c5c);

        float left = mWidth - 3.8f * marginWidth;
        float right = mWidth - 2.2f * marginWidth;
        float height = 0.8f * marginHeight;

        //电池左边部分外框
        RectF rectF = new RectF(left, offsetY - height, right, offsetY);
        canvas.drawRect(rectF, infoPaint);

        //电池左边部分内部电量区域
        infoPaint.setStyle(Paint.Style.FILL);

        float width = (right - left) * powerPercent;
        rectF = new RectF(left + 1.5f, offsetY - height + 1.5f, left + width - 1.5f, offsetY - 1.5f);
        canvas.drawRect(rectF, infoPaint);

        //电池右边小矩形
        rectF = new RectF(right, offsetY - 0.7f * height, right + 0.2f * marginWidth, offsetY - 0.3f * height);
        canvas.drawRect(rectF, infoPaint);
    }

    private List<String> getNextPageLines() {

        String string = "";

        List<String> lines = new ArrayList<>();

        if (mReadInfo.isNextRes) {
            lines.addAll(mReadInfo.nextResLines);

            mReadInfo.nextResLines.clear();

            mReadInfo.isNextRes = false;
        }

        if (mReadInfo.nextParaIndex >= mParaListSize) {
            return lines;
        }

        mCurContent = findContent(mReadInfo.nextParaIndex);

        while (lines.size() < mLineCount && mReadInfo.nextParaIndex < mParaListSize) {

            string = mParaList.get(mReadInfo.nextParaIndex);

            mReadInfo.nextParaIndex++;

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                lines.add(string.substring(0, size));

                string = string.substring(size);

            }

        }

        while (lines.size() > mLineCount) {
            mReadInfo.isNextRes = true;

            int end = lines.size() - 1;

            mReadInfo.nextResLines.add(0, lines.get(end));

            lines.remove(end);
        }

        return lines;
    }


    private List<String> getPrePageLines() {

        String string = "";
        List<String> lines = new ArrayList<>();

        if (mReadInfo.isPreRes) {

            lines.addAll(mReadInfo.preResLines);

            mReadInfo.preResLines.clear();

            mReadInfo.isPreRes = false;
        }

        if (mReadInfo.nextParaIndex < 0) {

            return lines;
        }

        mCurContent = findContent(mReadInfo.nextParaIndex);

        while (lines.size() < mLineCount && mReadInfo.nextParaIndex >= 0) {

            List<String> paraLines = new ArrayList<>();

            string = mParaList.get(mReadInfo.nextParaIndex);

            mReadInfo.nextParaIndex--;

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                paraLines.add(string.substring(0, size));

                string = string.substring(size);

            }

            lines.addAll(0, paraLines);

        }

        while (lines.size() > mLineCount) {
            mReadInfo.isPreRes = true;

            mReadInfo.preResLines.add(lines.get(0));

            lines.remove(0);
        }

        return lines;

    }

    //向后移动两页的距离
    private void pageDown() {
        mReadInfo.nextParaIndex += 1;//移动到最后已读的段落

        String string = "";

        List<String> lines = new ArrayList<>();

        int totalLines = 2 * mLineCount + mReadInfo.preResLines.size();

        reset();

        while (lines.size() < totalLines && mReadInfo.nextParaIndex < mParaListSize) {

            string = mParaList.get(mReadInfo.nextParaIndex);

            mReadInfo.nextParaIndex++;

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                lines.add(string.substring(0, size));

                string = string.substring(size);

            }

        }

        while (lines.size() > totalLines) {
            mReadInfo.isNextRes = true;

            int end = lines.size() - 1;

            mReadInfo.nextResLines.add(0, lines.get(end));

            lines.remove(end);
        }


    }

    //向前移动两页的距离
    private void pageUp() {
        mReadInfo.nextParaIndex -= 1; //移动到最后已读的段落

        String string = "";

        List<String> lines = new ArrayList<>();

        int totalLines = 2 * mLineCount + mReadInfo.nextResLines.size();

        reset();

        while (lines.size() < totalLines && mReadInfo.nextParaIndex >= 0) {

            List<String> paraLines = new ArrayList<>();

            string = mParaList.get(mReadInfo.nextParaIndex);

            mReadInfo.nextParaIndex--;

            while (string.length() > 0) {
                //检测一行能够显示多少字
                int size = mPaint.breakText(string, true, mVisibleWidth, null);

                paraLines.add(string.substring(0, size));

                string = string.substring(size);

            }

            lines.addAll(0, paraLines);

        }

        while (lines.size() > totalLines) {
            mReadInfo.isPreRes = true;

            mReadInfo.preResLines.add(lines.get(0));

            lines.remove(0);
        }

    }

    private void reset() {
        mReadInfo.preResLines.clear();
        mReadInfo.isPreRes = false;

        mReadInfo.nextResLines.clear();
        mReadInfo.isNextRes = false;
    }

    //获取屏幕的宽高
    private void calWidthAndHeight() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;

    }

    private void getFontFromAssets() {
        mTypefaceList.add(Typeface.DEFAULT);

        String[] fontNameList = null;
        AssetManager assetManager = mContext.getAssets();
        try {
            fontNameList = assetManager.list(FontPopup.FONTS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < fontNameList.length; i++) {

            String fontPath = FontPopup.FONTS + "/" + fontNameList[i];
            Typeface typeface = Typeface.createFromAsset(assetManager, fontPath);//根据路径得到Typeface
            mTypefaceList.add(typeface);
        }

    }

    public ReadInfo getReadInfo() {
        return mReadInfo;
    }


    public PaintInfo getPaintInfo() {
        return mPaintInfo;
    }

    public void setReadInfo(ReadInfo readInfo) {
        mReadInfo = readInfo;
    }

    public String getCurContent() {
        return mCurContent;
    }

    public String getPercentStr() {
        return percentStr;
    }
}
