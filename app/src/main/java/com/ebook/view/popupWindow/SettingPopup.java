package com.ebook.view.popupWindow;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.ebook.R;
import com.ebook.ReadingFragment;
import com.ebook.util.ScreenBrightnessHelper;
import com.ebook.util.bookPageUtil.PaintInfo;
import com.ebook.util.SaveHelper;
import com.ebook.view.SwitchView;


/**
 * Created by Administrator on 2017/1/7.
 */

public class SettingPopup extends BasePopupWindow implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    public static final int THEME_EYE = 2;
    public static final int THEME_NIGHT = 3;

    public static final int FLIP_PAGE_LIKE = 0;
    public static final int FLIP_COVER = 1;
    public static final int FLIP_NO_EFFECT = 2;

    private int[] mPopupColors;
    private int[] mStrokeColors;

    private CardView mCardView;
    private SwitchView mSwitchView;

    private Button[] mThemeBtns;
    private Button[] mFlipStyleBtns;
    private SeekBar[] mSeekBars;

    private int mTheme; //当前主题
    private int mFlipStyle; //当前阅读方式


    private OnSettingChangedListener mListener;
    private Resources mResources;


    public interface OnSettingChangedListener {
        void onSizeChanged(int progress);

        void onThemeChanged(int theme);

        void onFlipStyleChanged(int style);
    }

    public void setOnSettingChangedListener(OnSettingChangedListener settingListener) {
        mListener = settingListener;
    }

    @Override
    protected View createConvertView() {
        return LayoutInflater.from(mContext)
                .inflate(R.layout.popup_setting_layout, null);
    }

    @Override
    protected void setSize(int width, int height) {
        setWidth((int) (width * 0.95));
        setHeight((int) (height * 0.45));
    }

    public SettingPopup(Context context) {
        super(context);
        mResources = mContext.getResources();
        initDatas();
        initViews();
        initEvents();

    }


    private void initEvents() {

        PaintInfo paintInfo = SaveHelper.getObject(mContext, SaveHelper.PAINT_INFO);
        if (paintInfo != null) {
            mSeekBars[1].setProgress(paintInfo.textSize - ReadingFragment.TEXT_SIZE_DELTA);
        }

        mTheme = SaveHelper.getInt(mContext, SaveHelper.THEME);
        mFlipStyle = SaveHelper.getInt(mContext, SaveHelper.FLIP_STYLE);

        setTheme(mTheme);

        for (Button button : mThemeBtns) {
            button.setOnClickListener(this);

        }


        for (Button button : mFlipStyleBtns) {
            button.setOnClickListener(this);
        }

        for (SeekBar seekBar : mSeekBars) {
            seekBar.setOnSeekBarChangeListener(this);

        }


        mSwitchView.setOnCheckedChangeListener(new SwitchView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChange(boolean isChecked, View view) {

                if (isChecked)
                    ScreenBrightnessHelper.openAutoBrightness(mContext);
                else
                    ScreenBrightnessHelper.closeAutoBrightness(mContext);

            }
        });

    }

    @Override
    public void onClick(View v) {

        int theme = mTheme;
        int flipStyle = mFlipStyle;

        for (int i = 0; i < mThemeBtns.length; i++) {
            if (v.getId() == mThemeBtns[i].getId()) {
                theme = i;
                break;
            }

        }
        for (int i = 0; i < mFlipStyleBtns.length; i++) {
            if (v.getId() == mFlipStyleBtns[i].getId()) {
                flipStyle = i;
                break;
            }

        }

        if (theme != mTheme) {
            setTheme(theme);

        }

        if (flipStyle != mFlipStyle) {
            setFlipStyle(flipStyle);

        }


    }

    private void setTheme(int theme) {
        ObjectAnimator animator = ObjectAnimator
                .ofInt(mCardView, "cardBackgroundColor", mPopupColors[mTheme], mPopupColors[theme])
                .setDuration(500);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();

        mTheme = theme;
        setCurThemeBtn();
        setCurSeekBarStyle();

        setCurFlipStyleBtn();

        mSwitchView.setMaskColor(mStrokeColors[mTheme]);

        if (mListener != null)
            mListener.onThemeChanged(mTheme);
    }

    private void setFlipStyle(int flipStyle) {
        mFlipStyle = flipStyle;
        setCurFlipStyleBtn();

        if (mListener != null)
            mListener.onFlipStyleChanged(mFlipStyle);

    }


    private void setCurThemeBtn() {

        Button usedButton = mThemeBtns[mTheme];

        for (int i = 0; i < mThemeBtns.length; i++) {

            // 设置背景填充颜色
            GradientDrawable drawable = (GradientDrawable) mThemeBtns[i].getBackground();
            drawable.setColor(mPopupColors[i]);

            //设置边框颜色
            if (mThemeBtns[i].getId() == usedButton.getId()) {
                int strokeColor = mStrokeColors[i];
                drawable.setStroke(5, strokeColor);
            } else {
                drawable.setStroke(5, mPopupColors[i]);    //未选择button的边框颜色和填充颜色一致

            }

        }
    }


    private void setCurSeekBarStyle() {

        for (SeekBar seekBar : mSeekBars) {

            //获取seekBar的layer-list drawable对象
            LayerDrawable layerDrawable = (LayerDrawable) seekBar.getProgressDrawable();

            //层次包括背景图和进度,所以进度直接设为1,获取并设置进度条背景
            Drawable drawable = layerDrawable.getDrawable(1);
            drawable.setColorFilter(mStrokeColors[mTheme], PorterDuff.Mode.SRC);

            //获取thumb背景
            Drawable thumb = seekBar.getThumb();
            thumb.setColorFilter(mStrokeColors[mTheme], PorterDuff.Mode.SRC);

        }


    }

    private void setCurFlipStyleBtn() {

        Button usedButton = mFlipStyleBtns[mFlipStyle];

        for (Button button : mFlipStyleBtns) {

            GradientDrawable drawable = (GradientDrawable) button.getBackground();

            //设置边框颜色
            if (button.getId() == usedButton.getId()) {
                int strokeColor = mStrokeColors[mTheme]; //被选择button边框颜色由当前theme决定
                drawable.setStroke(5, strokeColor);
            } else {
                drawable.setStroke(5, 0xffc1c0c0);

            }

        }

    }

    private void initViews() {
        mCardView = (CardView) mConvertView.findViewById(R.id.setting_pop_card_view);
        mSwitchView = (SwitchView) mConvertView.findViewById(R.id.switch_view);

        mThemeBtns = new Button[]{
                (Button) mConvertView.findViewById(R.id.old_time_btn),
                (Button) mConvertView.findViewById(R.id.usual_btn),
                (Button) mConvertView.findViewById(R.id.eye_btn),
                (Button) mConvertView.findViewById(R.id.night_btn)
        };

        mFlipStyleBtns = new Button[]{
                (Button) mConvertView.findViewById(R.id.flip_page_like_btn),
                (Button) mConvertView.findViewById(R.id.flip_cover_btn),
                (Button) mConvertView.findViewById(R.id.flip_no_effect_btn)
        };

        mSeekBars = new SeekBar[]{
                (SeekBar) mConvertView.findViewById(R.id.brightness_seek_bar),
                (SeekBar) mConvertView.findViewById(R.id.text_size_seek_bar)
        };

        //初始化亮度进度条
        int brightness= ScreenBrightnessHelper.getBrightness(mContext);
        mSeekBars[0].setProgress(brightness);

    }

    private void initDatas() {

        mPopupColors = new int[]{
                0xffece5d3,  //复古
                0xfff2f1f1,  //常规
                0xffe1f4e7,  //护眼
                0xfd464546  //夜间
        };

        mStrokeColors = new int[]{
                0xffd49762,  //复古
                0xff20a9c7,  //常规
                0xff34a98a,  //护眼
                0xff2b80cf   //夜间
        };
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //在拖动时要随之改变屏幕亮度

        if (seekBar.getId() == R.id.brightness_seek_bar) {
            mSwitchView.setChecked(false);
            ScreenBrightnessHelper.setBrightness(mContext, progress);

        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.brightness_seek_bar) {
            //开始拖动亮度进度条，关闭自动调整亮度
            mSwitchView.setChecked(false);

        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //停止拖动时才改变字体大小

        if (seekBar.getId() == R.id.text_size_seek_bar) {
            if (mListener != null)
                mListener.onSizeChanged(seekBar.getProgress());

        }

    }


    public void setBrightness(int progress) {
        mSeekBars[0].setProgress(progress);
    }

    public int getTheme() {
        return mTheme;
    }

    public int getFlipStyle() {
        return mFlipStyle;
    }

}
