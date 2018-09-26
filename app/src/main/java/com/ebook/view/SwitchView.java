package com.ebook.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ebook.R;


/**
 * Created by Administrator on 2017/1/16.
 * Custom SwitchView
 */

public class SwitchView extends View {

    private int mWidth, mHeight;
    private float mLeft, mTop, mRight, mBottom;
    private float mButtonRadius;
    private float mButtonWidth;
    private float mButtonLeft;
    private float mButtonRight;
    private float mButtonX;
    private float mButtonY;
    private float mStrokeWidth;
    private float mSlidePercent;

    private int mStrokeColor;
    private int mMaskColor;
    private int mButtonColor;

    private Paint mStrokePaint;
    private Paint mMaskPaint;
    private Paint mButtonPaint;

    private Path mPath;

    private boolean isChecked;
    private SlideHandler mSlideHandler;

    private OnCheckedChangeListener mListener;


    public interface OnCheckedChangeListener {
        void onCheckedChange(boolean isChecked, View view);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public SwitchView(Context context) {
        this(context, null);
    }

    public SwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取自定义属性
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MySwitchView);
        mButtonColor = ta.getColor(R.styleable.MySwitchView_buttonColor, 0xffbab9b9);
        mStrokeColor = ta.getColor(R.styleable.MySwitchView_strokeColor, 0xffbab9b9);
        mMaskColor = ta.getColor(R.styleable.MySwitchView_maskColor, 0xff20a9c7);
        isChecked = ta.getBoolean(R.styleable.MySwitchView_state, false);

        ta.recycle();

        initObjects();

    }

    private void initObjects() {
        mPath = new Path();
        mSlideHandler = new SlideHandler();

        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setStyle(Style.STROKE);
        mStrokePaint.setColor(mStrokeColor);

        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setStyle(Style.FILL);
        mMaskPaint.setColor(mMaskColor);

        mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setStyle(Style.FILL);
        mButtonPaint.setColor(mButtonColor);


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = (int) (widthSize * 0.55f);
        setMeasuredDimension(widthSize, heightSize);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;

        //背景path的外部矩形，预留一定空间给边框
        mStrokeWidth = mHeight / 20f;
        mLeft = mStrokeWidth * 2;
        mTop = mStrokeWidth * 2;
        mRight = mWidth - mStrokeWidth * 2;
        mBottom = mHeight - mStrokeWidth * 2;

        RectF rectF = new RectF(mLeft, mTop, mBottom, mBottom);
        mPath.arcTo(rectF, 90, 180);
        rectF.left = mRight - mBottom;
        rectF.right = mRight;
        mPath.arcTo(rectF, 270, 180);

        mPath.close();

        //滑动button外部正方形范围
        mButtonLeft = mLeft;
        mButtonRight = mBottom;
        mButtonWidth = mButtonRight - mButtonLeft;
        mButtonRadius = mButtonWidth / 6f;
        mButtonX = mButtonWidth / 2 + mStrokeWidth * 2;
        mButtonY = mButtonWidth / 2 + mStrokeWidth * 2;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mSlidePercent > 0)
            mSlidePercent -= 0.1f;
        else
            mSlidePercent = 0;


        //绘制背景边框
        canvas.save();
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        canvas.drawPath(mPath, mStrokePaint);
        canvas.restore();


        //绘制遮盖层
        float scale = (mStrokeWidth * 2 + mWidth) / mWidth * (isChecked ? 1 - mSlidePercent : mSlidePercent);
        canvas.save();
        canvas.scale(scale, scale, mButtonWidth / 2, (mTop + mBottom) / 2);
        canvas.drawPath(mPath, mMaskPaint);
        canvas.restore();

        //绘制滑动button
        final float translate = (mWidth - mStrokeWidth * 4 - mButtonWidth) * (isChecked ? 1 - mSlidePercent : mSlidePercent); // 平移距离参数随sAnim变化而变化
        canvas.save();
        canvas.translate(translate, 0);

        //在滑动过程中根据状态改变button半径
        if (mSlidePercent > 0 && isChecked) {
            mButtonRadius += mButtonWidth / 50f;
        } else if (mSlidePercent > 0 && !isChecked) {
            mButtonRadius -= mButtonWidth / 50f;
        }

        // 根据状态改变paint颜色
        if (isChecked)
            mButtonPaint.setColor(Color.WHITE);
        else
            mButtonPaint.setColor(mButtonColor);

        canvas.drawCircle(mButtonX, mButtonY, mButtonRadius, mButtonPaint);
        canvas.restore();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP) {
            //状态切换
            isChecked = !isChecked;

            mSlidePercent = 1;
            startSlide();

            if (mListener != null)
                mListener.onCheckedChange(isChecked, this);
        }

        return true;
    }


    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        if (isChecked != checked) {
            isChecked = checked;
            //开启滑动，重新绘制
            mSlidePercent = 1;
            startSlide();

            if (mListener != null)
                mListener.onCheckedChange(isChecked, this);

        }
    }

    public void setMaskColor(int maskColor) {
        mMaskColor = maskColor;
        mMaskPaint.setColor(mMaskColor);
        invalidate();
    }


    private void startSlide() {
        if (mSlidePercent > 0)
            mSlideHandler.sleep(5);


    }


    //处理滑动的Handler
    private class SlideHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            SwitchView.this.invalidate();

            // 循环调用
            SwitchView.this.startSlide();
        }

        //延迟向Handler发送消息实现时间间隔
        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }


}
