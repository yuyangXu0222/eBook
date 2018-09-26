package com.ebook.view.popupWindow;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.ebook.R;
import com.ebook.util.bookPageUtil.PaintInfo;
import com.ebook.util.SaveHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2017/1/7.
 */

public class FontPopup extends BasePopupWindow implements View.OnClickListener {
    public static final String FONTS = "fonts";
    private int mTypeIndex;
    private int[] mTextColors;

    private List<Typeface> mTypefaceList = new ArrayList<>();
    private TextView[] mTexts;
    private Button[] mButtons;
    private FloatingActionButton[] mFabs;

    private OnFontSelectedListener mListener;

    public interface OnFontSelectedListener {
        void onTypefaceSelected(int typeIndex);

        void onColorSelected(int color);
    }

    public void setOnFontSelectedListener(OnFontSelectedListener listener) {
        mListener = listener;
    }

    @Override
    protected View createConvertView() {
        return LayoutInflater.from(mContext)
                .inflate(R.layout.popup_font_layout, null);
    }


    public FontPopup(Context context) {
        super(context);

        getFontFromAssets();       //从Assets中获取字体

        initViews();

        initEvents();

    }

    private void initEvents() {

        //设置初始状态
        PaintInfo paintInfo = SaveHelper.getObject(mContext, SaveHelper.PAINT_INFO);
        if (paintInfo != null) {
            mTypeIndex = paintInfo.typeIndex;
        }
        setUsedButton();

        for (int i = 0; i < mTexts.length; i++) {
            mTexts[i].setTypeface(mTypefaceList.get(i));
        }

        for (Button button : mButtons) {
            button.setOnClickListener(this);

        }

        for (FloatingActionButton fab : mFabs) {
            fab.setOnClickListener(this);

        }

    }


    @Override
    public void onClick(View v) {
        int typeIndex = mTypeIndex;
        int color = 0;

        for (int i = 0; i < mButtons.length; i++) {
            if (v.getId() == mButtons[i].getId()) {
                typeIndex = i;
                break;
            }
        }

        for (int i = 0; i < mFabs.length; i++) {
            if (v.getId() == mFabs[i].getId()) {
                color = mTextColors[i];
                break;
            }
        }

        //改变字体
        if (typeIndex != mTypeIndex) {
            mTypeIndex = typeIndex;
            setUsedButton();
            if (mListener != null)
                mListener.onTypefaceSelected(mTypeIndex);
        }

        //改变颜色
        if (color != 0) {
            if (mListener != null)
                mListener.onColorSelected(color);
        }


    }


    private void setUsedButton() {
        int unUsedColor = 0xffc1c0c0;
        int usedColor = 0xFF5FE677;
        Button usedButton = mButtons[mTypeIndex];

        for (Button button : mButtons) {

            if (button.getId() == usedButton.getId()) {

                button.setText("正在使用");
                button.setTextColor(usedColor);
                GradientDrawable drawable = (GradientDrawable) button.getBackground();
                drawable.setStroke(5, usedColor);   // 设置边框颜色


            } else {

                button.setText("点击使用");
                button.setTextColor(unUsedColor);
                GradientDrawable drawable = (GradientDrawable) button.getBackground();
                drawable.setStroke(5, unUsedColor);   // 设置边框颜色

            }

        }

    }

    private void getFontFromAssets() {
        mTypefaceList.add(Typeface.DEFAULT);

        String[] fontNameList = null;
        AssetManager assetManager = mContext.getAssets();
        try {
            fontNameList = assetManager.list(FONTS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < fontNameList.length; i++) {

            String fontPath = FONTS + "/" + fontNameList[i];
            Typeface typeface = Typeface.createFromAsset(assetManager, fontPath);//根据路径得到Typeface
            mTypefaceList.add(typeface);
        }

    }

    private void initViews() {
        mTextColors = new int[]{
                0xff121111,  //黑
                0x8A000000,  //常规
                0xffa9a8a8,  //夜间
                0xfbe6e3e3,  //白
                0xff486c94,  //蓝
        };


        mTexts = new TextView[]{
                (TextView) mConvertView.findViewById(R.id.text_system),
                (TextView) mConvertView.findViewById(R.id.text_hksn),
                (TextView) mConvertView.findViewById(R.id.text_hwzs),
                (TextView) mConvertView.findViewById(R.id.text_kai),
                (TextView) mConvertView.findViewById(R.id.text_yy)
        };

        mButtons = new Button[]{
                (Button) mConvertView.findViewById(R.id.btn_system),
                (Button) mConvertView.findViewById(R.id.btn_hksn),
                (Button) mConvertView.findViewById(R.id.btn_hwzs),
                (Button) mConvertView.findViewById(R.id.btn_kai),
                (Button) mConvertView.findViewById(R.id.btn_yy)
        };

        mFabs = new FloatingActionButton[]{
                (FloatingActionButton) mConvertView.findViewById(R.id.fab_black),
                (FloatingActionButton) mConvertView.findViewById(R.id.fab_normal),
                (FloatingActionButton) mConvertView.findViewById(R.id.fab_night),
                (FloatingActionButton) mConvertView.findViewById(R.id.fab_white),
                (FloatingActionButton) mConvertView.findViewById(R.id.fab_blue)
        };


    }


}
