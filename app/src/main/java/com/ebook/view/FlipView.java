package com.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.ebook.util.SaveHelper;
import com.ebook.view.popupWindow.SettingPopup;

import java.util.List;

/**
 * Created by Mum on 2017/2/5.
 */

public class FlipView extends View {
    private static final String TAG = "FoldView";
    private static final float CURVATURE = 1 / 4f;//假定贝塞尔曲线从原这折线的1/4f处开始
    private Context mContext;
    private List<Bitmap> mBitmapList;

    private int mViewWidth, mViewHeight;// 控件宽高
    float mDiagonalLength;//对角线长度

    //折叠三角形的底部直角边、左侧直角边
    private float mFoldEdgeBtm;
    private float mFoldEdgeLeft;

    private float mFoldBuffArea; //翻页时的缓冲，防止折叠区域无法封闭

    //触摸点
    private PointF mTouch;
    //底部贝塞尔曲线起始点、控制点、终点、顶点
    private PointF mBezierStart1;
    private PointF mBezierControl1;
    private PointF mBezierEnd1;
    private PointF mBezierVertex1;
    //右侧贝塞尔曲线起始点、控制点、终点、顶点
    private PointF mBezierStart2;
    private PointF mBezierControl2;
    private PointF mBezierEnd2;
    private PointF mBezierVertex2;
    //自滑直线起点
    private PointF mAutoSlideStart;
    //当前翻页模式对应的边角（右下角，右上角）
    private PointF mCorner;
    //手指落下时的触摸点
    private PointF mLastDownPoint;


    private Path mFoldPath;// 折叠区域path
    private Path mFoldAndNextPath;// 包含折叠和下一页区域的Path
    private Paint mFoldPaint; //折叠区域的画笔

    //渐变阴影
    private GradientDrawable mNextShadowRL;
    private GradientDrawable mNextShadowLR;

    private GradientDrawable mFoldShadowRL;
    private GradientDrawable mFoldShadowLR;

    private GradientDrawable mCurShadowRL;
    private GradientDrawable mCurShadowBT;
    private GradientDrawable mCurShadowTB;


    private float mSlideSpeedLeft;// 滑动速度
    private float mSlideSpeedRight;

    private float mAutoAreaBound;// 自滑区域分界

    private static final int SUB_WIDTH = 19, SUB_HEIGHT = 19;// 细分值横竖各19个网格
    private final float[] mCoordinates;//扭曲之后的坐标点的数组集合

    private boolean isSlide;// 是否滑动
    private boolean isPrePageOver;//前一页是否已经翻完
    private boolean isDrawOnMove; //移动的时候是否已经开始重绘了
    private boolean isFlipNext;//翻页时翻前一页还是后一页

    private int mPrePage = 0; //前一页索引
    private int mNextPage = 1;//后一页索引

    private Slide mSlide;
    private FlipMode mFlipMode;
    private FlipStyle mFlipStyle;

    private OnPageFlippedListener mListener;

    private SlideHandler mSlideHandler; // 滑动处理Handler

    // 滑动方式：往左滑，往右滑
    private enum Slide {
        LEFT, RIGHT
    }

    //翻页方式：右上角，右边中部，右下角
    private enum FlipMode {
        RIGHT_TOP, RIGHT_MIDDLE, RIGHT_BOTTOM
    }

    //翻页风格：仿真、覆盖、无效果
    public enum FlipStyle {
        STYLE_PAGE_LIKE, STYLE_COVER, STYLE_NO_EFFECT
    }


    public void setOnPageFlippedListener(OnPageFlippedListener listener) {
        mListener = listener;
    }

    public interface OnPageFlippedListener {

        List<Bitmap> onNextPageFlipped();

        List<Bitmap> onPrePageFlipped();

        void onFlipStarted();

        void onFoldViewClicked();
    }


    //处理滑动的Handler
    private class SlideHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage: 7");
            // 重绘视图
            FlipView.this.invalidate();
            // 循环调用滑动计算
            FlipView.this.slide();
        }

        //延迟向Handler发送消息实现时间间隔
        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }


    public FlipView(Context context) {
        this(context, null);
    }

    public FlipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mCoordinates = new float[(SUB_WIDTH + 1) * (SUB_HEIGHT + 1) * 2];
        initObjects();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mViewWidth = w;
        mViewHeight = h;
        initDatas();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        if (mBitmapList == null || mBitmapList.size() == 0) {
            return;
        }

        // 重绘时清除上次路径
        mFoldPath.reset();
        mFoldAndNextPath.reset();

        //首次进入绘制前一页
        if (!isPrePageOver && mTouch.x == 0 && mTouch.y == 0) {
            canvas.drawBitmap(mBitmapList.get(mPrePage), 0, 0, null);
            return;
        }


        if (!isPrePageOver) {

            //仿真翻页
            if (mFlipStyle == FlipStyle.STYLE_PAGE_LIKE) {

                if (mFlipMode == FlipMode.RIGHT_MIDDLE)

                    flipPageFromMiddle(canvas); //右侧中部翻页
                else
                    flipPageFromCorner(canvas); //右上角和右下角翻页

            }

            //覆盖翻页
            if (mFlipStyle == FlipStyle.STYLE_COVER)
                flipCover(canvas);


            //无效果翻页
            if (mFlipStyle == FlipStyle.STYLE_NO_EFFECT)
                flipNoEffect(canvas);


        } else {
            //前一页已经完全翻完，直接绘制下一页区域
            canvas.save();
            canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);
            canvas.restore();

        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isSlide)
            return true;   //自动滑动过程中不响应touch事件

        mTouch.x = event.getX();
        mTouch.y = event.getY();

        float width = mDiagonalLength / 100f; //判断是翻页还是点击事件的距离

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastDownPoint.x = mTouch.x;//保存下手指落下时刻的触摸点
                mLastDownPoint.y = mTouch.y;

                isDrawOnMove = false;

                //仿真翻页风格确定翻页方式
                if (mFlipStyle == FlipStyle.STYLE_PAGE_LIKE)
                    getFlipPageMode();

                break;

            case MotionEvent.ACTION_MOVE:

                if (!isDrawOnMove) {

                    //翻前一页
                    if (mTouch.x - mLastDownPoint.x > width) {

                        isFlipNext = false;

                        if (mFlipStyle == FlipStyle.STYLE_PAGE_LIKE)
                            mFlipMode = FlipMode.RIGHT_MIDDLE;

                        if (!isPrePageOver) {
                            //回调获得前一页page

                            List<Bitmap> temp = null;

                            if (mListener != null)
                                temp = mListener.onPrePageFlipped();

                            if (temp == null) {
                                Toast.makeText(mContext, "已经是第一页了", Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            mBitmapList = temp;
                            isDrawOnMove = true;


                        } else {

                            isPrePageOver = false;
                            isDrawOnMove = true;
                        }

                    }


                    //翻下一页
                    if (mTouch.x - mLastDownPoint.x < -width) {
                        isFlipNext = true;

                        if (isPrePageOver) {
                            //回调获得后一页page

                            List<Bitmap> temp = null;

                            if (mListener != null)
                                temp = mListener.onNextPageFlipped();

                            if (temp == null) {
                                Toast.makeText(mContext, "已经是最后一页了", Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            mBitmapList = temp;
                            isPrePageOver = false;
                            isDrawOnMove = true;

                        } else {

                            isDrawOnMove = true;
                        }
                    }


                } else {

                    if (mListener != null)
                        mListener.onFlipStarted();

                    invalidate();
                }

                break;


            case MotionEvent.ACTION_UP:

                float dis = (float) Math.hypot(mTouch.x - mLastDownPoint.x, mTouch.y - mLastDownPoint.y);
                if (dis < width) {   //没有触发翻页效果，认为是点击事件

                    mTouch.x = mCorner.x;  //强制设置touch点坐标，防止因设置visibility而重绘导致的画面突变
                    mTouch.y = mCorner.y;

                    if (mListener != null)
                        mListener.onFoldViewClicked();

                    return true;
                }

                if (!isDrawOnMove) {
                    return true;
                }

                if (mFlipStyle == FlipStyle.STYLE_PAGE_LIKE) {

                    //抬起的时候强制限制触摸点，防止滑动时因为取消限制产生的突变
                    if (mFlipMode == FlipMode.RIGHT_BOTTOM || mFlipMode == FlipMode.RIGHT_TOP) {
                        PointF pointF = limitTouchPoints(mTouch.x, mTouch.y);
                        mTouch.x = pointF.x;
                        mTouch.y = pointF.y;
                    }

                }


                if (mFlipStyle != FlipStyle.STYLE_NO_EFFECT) {

                    if (mTouch.x < mAutoAreaBound) {

                        mSlide = Slide.LEFT;// 当前为往左滑
                        startSlide(mTouch.x, mTouch.y);// 开始滑动
                    } else {

                        mSlide = Slide.RIGHT;
                        startSlide(mTouch.x, mTouch.y);

                    }
                }


                break;

        }

        return true;
    }

    private void getFlipPageMode() {
        float height = mViewHeight * 3 / 10f;
        if (mTouch.y > mViewHeight - height) {

            mFlipMode = FlipMode.RIGHT_BOTTOM;

            mCorner.x = mViewWidth;
            mCorner.y = mViewHeight;

        } else if (mTouch.y > height) {

            mFlipMode = FlipMode.RIGHT_MIDDLE;

            mCorner.x = mViewWidth;
            mCorner.y = mViewHeight / 2f;

        } else {

            mFlipMode = FlipMode.RIGHT_TOP;

            mCorner.x = mViewWidth;
            mCorner.y = 0;
        }
    }

    private void flipCover(Canvas canvas) {
        //绘制下层page
        canvas.save();
        canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);
        canvas.restore();

        //绘制上层page
        if (isFlipNext) {   //向后翻
            float moveDis = mLastDownPoint.x - mTouch.x;
            if (moveDis < 0) moveDis = 0;
            canvas.save();
            canvas.drawBitmap(mBitmapList.get(mPrePage), -moveDis, 0, null);
            canvas.restore();

            //阴影
            int left = (int) (mViewWidth - moveDis) - 1;
            int right = (int) (left + mDiagonalLength / 30f);

            canvas.save();
            mFoldShadowRL.setBounds(left, 0, right, mViewHeight);
            mFoldShadowRL.draw(canvas);
            canvas.restore();

        } else {     //向前翻

            float moveDis = mTouch.x;
            canvas.save();
            canvas.drawBitmap(mBitmapList.get(mPrePage), -(mViewWidth - moveDis), 0, null);
            canvas.restore();

            //阴影
            int left = (int) mTouch.x - 1;
            int right = (int) (left + mDiagonalLength / 30f);
            canvas.save();
            mFoldShadowRL.setBounds(left, 0, right, mViewHeight);
            mFoldShadowRL.draw(canvas);
            canvas.restore();

        }
    }

    private void flipNoEffect(Canvas canvas) {
        if (isFlipNext) {
            canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);
            isPrePageOver = true;

        } else {
            canvas.drawBitmap(mBitmapList.get(mPrePage), 0, 0, null);
        }
    }

    private void flipPageFromCorner(Canvas canvas) {
        //手动拖拽时限制触摸点范围，自动滑动时放开限制
        if (!isSlide) {
            mTouch = limitTouchPoints(mTouch.x, mTouch.y);
        }

        calPoints();    //计算坐标点

        calPaths();    //计算路径

        drawCurrentAreaAndShadow(canvas);   //当前页区域填充及阴影绘制

        drawFoldAreaAndShadow(canvas);     //折叠区域填充及阴影绘制

        drawNextAreaAndShadow(canvas);     //下一页区域填充及阴影绘制
    }

    private void flipPageFromMiddle(Canvas canvas) {
        if (!isPrePageOver) {
            //前一页还没完全翻完，要绘制三个区域

            float foldWidth = (mViewWidth - mTouch.x) * 4 / 10f; //折叠区域宽度

            //当前页区域
            canvas.save();
            canvas.clipRect(new RectF(0, 0, mTouch.x, mViewHeight));
            canvas.drawBitmap(mBitmapList.get(mPrePage), 0, 0, null);

            mCurShadowRL.setBounds((int) (mTouch.x - foldWidth  / 10f), 0, (int) (mTouch.x + 1), mViewHeight);
            mCurShadowRL.draw(canvas);
            canvas.restore();

            //折叠区域
            canvas.save();
            canvas.clipRect(new RectF(mTouch.x, 0, mTouch.x + foldWidth, mViewHeight));
            canvas.scale(-1, 1);//x方向镜像
            canvas.translate(-(mViewWidth + mTouch.x), 0);
            canvas.drawBitmap(mBitmapList.get(mPrePage), 0, 0, mFoldPaint);
            canvas.restore();

            canvas.save();//还原坐标系,绘制折叠区域阴影
            mFoldShadowLR.setBounds((int) (mTouch.x + foldWidth * 7 / 10f), 0, (int) (mTouch.x + foldWidth + 1), mViewHeight);
            mFoldShadowLR.draw(canvas);
            canvas.restore();


            //下一页区域
            canvas.save();
            canvas.clipRect(new RectF(mTouch.x + foldWidth, 0, mViewWidth, mViewHeight));
            canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);
            mNextShadowLR.setBounds((int) (mTouch.x + foldWidth * 7 / 10), 0, (int) (mTouch.x + foldWidth + foldWidth * 1 / 2), mViewHeight);
            mNextShadowLR.draw(canvas);
            canvas.restore();

        } else {

            //前一页已经完全翻完，直接绘制下一页区域
            canvas.save();
            canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);
            canvas.restore();
        }

    }

    private void startSlide(float x, float y) {
        // 获取并设置直线方程的起点
        mAutoSlideStart.x = x;
        mAutoSlideStart.y = y;
        // 开始滑动
        isSlide = true;

        // 滑动
        slide();
    }

    private void slide() {
        if (mFlipStyle == FlipStyle.STYLE_PAGE_LIKE) {

            //前一页已经向左完全翻完
            if (mSlide == Slide.LEFT && mTouch.x <= -mViewWidth + mFoldBuffArea) {
                isPrePageOver = true;
                isSlide=false;
                invalidate();
            }

            //往右边滑结束
            if (mSlide == Slide.RIGHT && mTouch.x >= mViewWidth) {
                isSlide=false;
            }



            //往左边滑
            if (mSlide == Slide.LEFT && mTouch.x > -mViewWidth + mFoldBuffArea) {
                // 则让x坐标自减，右侧侧边翻页不用设定y值
                mTouch.x -= mSlideSpeedLeft;

                // 角翻页需要设定y值
                if (mFlipMode == FlipMode.RIGHT_BOTTOM || mFlipMode == FlipMode.RIGHT_TOP) {
                    mTouch.y = mAutoSlideStart.y + ((mTouch.x - mAutoSlideStart.x) * (mCorner.y - mAutoSlideStart.y)) / (-mCorner.x - mAutoSlideStart.x);
                }

                // 让SlideHandler处理重绘
                mSlideHandler.sleep(25);
            }

            //往右边滑
            if (mSlide == Slide.RIGHT && mTouch.x < mViewWidth) {

                // 则让x坐标自加，右侧侧边翻页不用设定y值
                mTouch.x += mSlideSpeedRight;

                // 角翻页需要设定y值
                if (mFlipMode == FlipMode.RIGHT_BOTTOM || mFlipMode == FlipMode.RIGHT_TOP) {
                    mTouch.y = mAutoSlideStart.y + ((mTouch.x - mAutoSlideStart.x) * (mCorner.y - mAutoSlideStart.y)) / (mCorner.x - mAutoSlideStart.x);
                }

                // 让SlideHandler处理重绘
                mSlideHandler.sleep(25);

            }

        }

        if (mFlipStyle == FlipStyle.STYLE_COVER) {

            //前一页已经向左完全翻完
            if (mSlide == Slide.LEFT && mTouch.x <= -(mViewWidth - mLastDownPoint.x)) {
                isPrePageOver = true;
                isSlide=false;
                invalidate();

            }

            //往右边滑
            if (mSlide == Slide.RIGHT && mTouch.x >= mViewWidth) {
               isSlide=false;

            }

            //往左边滑
            if (mSlide == Slide.LEFT && mTouch.x > -(mViewWidth - mLastDownPoint.x)) {
                mTouch.x -= mSlideSpeedLeft;
                mSlideHandler.sleep(25);
            }

            //往右边滑
            if (mSlide == Slide.RIGHT && mTouch.x < mViewWidth) {
                mTouch.x += mSlideSpeedRight;
                mSlideHandler.sleep(25);

            }


        }

    }

    //更新当前page
    public void updateBitmapList(List<Bitmap> bitmapList) {

        mBitmapList = bitmapList;

        invalidate();
    }


    public void setPageByContent(List<Bitmap> bitmapList) {

        mBitmapList = bitmapList;

        isPrePageOver = false;

        mTouch.x = 0;
        mTouch.y = 0;

        invalidate();
    }


    private PointF limitTouchPoints(float touchX, float touchY) {
        PointF effectPoint = new PointF(mViewWidth * 2 / 10f, mCorner.y);
        float effectR = mCorner.x - effectPoint.x;
        float distance = (float) Math.hypot(touchX - effectPoint.x, touchY - effectPoint.y);
        if (distance > effectR) {
            float radio = (distance - effectR) / distance;
            touchX = touchX - (touchX - effectPoint.x) * radio;
            touchY = touchY + (mCorner.y - touchY) * radio;
        }

        return new PointF(touchX, touchY);

    }


    //根据图来计算坐标点
    private void calPoints() {

        //为了兼容右上角翻页和右下角翻页增加的变量
        int operator = 1;
        if (mFlipMode == FlipMode.RIGHT_TOP) {
            operator = -1;
        }

        float toCornerX = mCorner.x - mTouch.x;
        float toCornerY = Math.abs(mCorner.y - mTouch.y);
        float temp = (float) (Math.pow(toCornerY, 2) + Math.pow(toCornerX, 2));

        //折叠三角形的底部直角边和左侧直角边
        mFoldEdgeBtm = temp / (2F * toCornerX);
        mFoldEdgeLeft = temp / (2F * toCornerY);


        //底部贝塞尔曲线
        mBezierControl1.x = mCorner.x - mFoldEdgeBtm;
        mBezierControl1.y = mCorner.y;

        mBezierStart1.x = mBezierControl1.x - CURVATURE * mFoldEdgeBtm;
        mBezierStart1.y = mCorner.y;

        mBezierEnd1.x = mTouch.x + (1 - CURVATURE) * (toCornerX - mFoldEdgeBtm);
        mBezierEnd1.y = mTouch.y + operator * (1 - CURVATURE) * toCornerY;

        mBezierVertex1.x = 0.25F * mBezierStart1.x + 0.5F * mBezierControl1.x + 0.25F * mBezierEnd1.x;
        mBezierVertex1.y = 0.25F * mBezierStart1.y + 0.5F * mBezierControl1.y + 0.25F * mBezierEnd1.y;

        //右侧贝塞尔曲线
        mBezierControl2.x = mCorner.x;
        mBezierControl2.y = mCorner.y - operator * mFoldEdgeLeft;

        mBezierStart2.x = mCorner.x;
        mBezierStart2.y = mCorner.y - operator * mFoldEdgeLeft - operator * CURVATURE * mFoldEdgeLeft;//

        mBezierEnd2.x = mTouch.x + (1 - CURVATURE) * toCornerX;
        mBezierEnd2.y = mTouch.y + operator * (1 - CURVATURE) * (toCornerY - mFoldEdgeLeft);//


        mBezierVertex2.x = 0.25F * mBezierStart2.x + 0.5F * mBezierControl2.x + 0.25F * mBezierEnd2.x;
        mBezierVertex2.y = 0.25F * mBezierStart2.y + 0.5F * mBezierControl2.y + 0.25F * mBezierEnd2.y;

    }


    private void calPaths() {

        //折叠区域path
        mFoldPath.moveTo(mBezierStart1.x, mBezierStart1.y);
        mFoldPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y);
        mFoldPath.lineTo(mTouch.x, mTouch.y);
        mFoldPath.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mFoldPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y);
        mFoldPath.lineTo(mBezierVertex2.x, mBezierVertex2.y);
        mFoldPath.lineTo(mBezierVertex1.x, mBezierVertex1.y);

        //包含折叠区域和下一页的path
        mFoldAndNextPath.moveTo(mBezierStart1.x, mBezierStart1.y);
        mFoldAndNextPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y);
        mFoldAndNextPath.lineTo(mTouch.x, mTouch.y);
        mFoldAndNextPath.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mFoldAndNextPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y);
        mFoldAndNextPath.lineTo(mCorner.x, mCorner.y);
        mFoldAndNextPath.close();


    }


    private void drawCurrentAreaAndShadow(Canvas canvas) {
        //当前页bitmap填充
        canvas.save();
        canvas.clipRect(new RectF(0, 0, mViewWidth, mViewHeight));
        canvas.clipPath(mFoldAndNextPath, Region.Op.DIFFERENCE);

        canvas.drawBitmap(mBitmapList.get(mPrePage), 0, 0, null);
        canvas.restore();

        if (mFlipMode == FlipMode.RIGHT_BOTTOM) {

            //折叠三角形底部直角边阴影
            Path tempPath = new Path();
            double degree = Math.PI / 4
                    + Math.atan2(mTouch.x - mBezierControl1.x, mBezierControl1.y - mTouch.y);

            float rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
                    - mBezierControl1.x, mBezierControl1.y - mTouch.y));

            double d1 = (float) 25 * 1.414 * Math.cos(degree);
            double d2 = (float) 25 * 1.414 * Math.sin(degree);

            //阴影的起点
            PointF pointF = new PointF((float) (mTouch.x - d1), (float) (mTouch.y - d2));
            float width = (float) Math.hypot((mTouch.x - mCorner.x), (mTouch.y - mCorner.y))/40f;
            int left = (int) (mBezierControl1.x - width);
            int right = (int) mBezierControl1.x + 1;

            tempPath.reset();
            tempPath.moveTo(pointF.x, pointF.y);
            tempPath.lineTo(mTouch.x, mTouch.y);
            tempPath.lineTo(mBezierControl1.x, mBezierControl1.y);
            tempPath.lineTo(mBezierStart1.x, mBezierStart1.y);
            tempPath.close();

            canvas.save();
            canvas.clipRect(new RectF(0, 0, mViewWidth, mViewHeight));
            canvas.clipPath(mFoldAndNextPath, Region.Op.DIFFERENCE);
            canvas.clipPath(tempPath);

            mCurShadowRL.setBounds(left, (int) (mBezierControl1.y - mDiagonalLength),
                    right, (int) (mBezierControl1.y));
            canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
            mCurShadowRL.draw(canvas);
            canvas.restore();


            //还原坐标系，绘制折叠三角形左侧直角边阴影

            int btmY = (int) (mTouch.y + 1);
            int topY = (int) (mTouch.y - width);
            rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y - mTouch.y,
                    mBezierControl2.x - mTouch.x));

            tempPath.reset();
            tempPath.moveTo(pointF.x, pointF.y);
            tempPath.lineTo(mTouch.x, mTouch.y);
            tempPath.lineTo(mBezierControl2.x, mBezierControl2.y);
            tempPath.lineTo(mBezierStart2.x, mBezierStart2.y);
            tempPath.close();

            canvas.save();
            canvas.clipRect(new RectF(0, 0, mViewWidth, mViewHeight));
            canvas.clipPath(mFoldAndNextPath, Region.Op.DIFFERENCE);
            canvas.clipPath(tempPath);

            mCurShadowBT.setBounds((int) (mTouch.x - 2f*width), topY,
                    (int) (mTouch.x + mDiagonalLength), btmY);
            canvas.rotate(rotateDegrees, mTouch.x, mTouch.y);
            mCurShadowBT.draw(canvas);
            canvas.restore();


        } else if (mFlipMode == FlipMode.RIGHT_TOP) {


            //折叠三角形底部直角边阴影
            Path tempPath = new Path();
            double degree = Math.PI / 4
                    + Math.atan2(mBezierControl1.x - mTouch.x, mTouch.y - mBezierControl1.y);

            float rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x - mTouch.x,
                    mTouch.y - mBezierControl1.y));

            double d1 = (float) 25 * 1.414 * Math.sin(degree);
            double d2 = (float) 25 * 1.414 * Math.cos(degree);

            //阴影的起点
            PointF pointF = new PointF((float) (mTouch.x - d1), (float) (mTouch.y + d2));

            float width = (float) Math.hypot((mTouch.x - mCorner.x), (mTouch.y - mCorner.y))/40f;
            int left = (int) (mBezierControl1.x - width);
            int right = (int) mBezierControl1.x + 1;

            tempPath.reset();
            tempPath.moveTo(pointF.x, pointF.y);
            tempPath.lineTo(mTouch.x, mTouch.y);
            tempPath.lineTo(mBezierControl1.x, mBezierControl1.y);
            tempPath.lineTo(mBezierStart1.x, mBezierStart1.y);
            tempPath.close();

            canvas.save();
            canvas.clipRect(new RectF(0, 0, mViewWidth, mViewHeight));
            canvas.clipPath(mFoldAndNextPath, Region.Op.DIFFERENCE);
            canvas.clipPath(tempPath);

            mCurShadowRL.setBounds(left, (int) mBezierControl1.y,
                    right, (int) (mBezierControl1.y + mDiagonalLength));
            canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
            mCurShadowRL.draw(canvas);
            canvas.restore();


            //还原坐标系，绘制折叠三角形左侧直角边阴影

            int topY = (int) (mTouch.y - 1);
            int btmY = (int) (mTouch.y + width);
            rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y - mTouch.y,
                    mBezierControl2.x - mTouch.x));

            tempPath.reset();
            tempPath.moveTo(pointF.x, pointF.y);
            tempPath.lineTo(mTouch.x, mTouch.y);
            tempPath.lineTo(mBezierControl2.x, mBezierControl2.y);
            tempPath.lineTo(mBezierStart2.x, mBezierStart2.y);
            tempPath.close();

            canvas.save();
            canvas.clipRect(new RectF(0, 0, mViewWidth, mViewHeight));
            canvas.clipPath(mFoldAndNextPath, Region.Op.DIFFERENCE);
            canvas.clipPath(tempPath);

            mCurShadowTB.setBounds((int) (mTouch.x - 2f*width), topY,
                    (int) (mTouch.x + mDiagonalLength), btmY);
            canvas.rotate(rotateDegrees, mTouch.x, mTouch.y);
            mCurShadowTB.draw(canvas);
            canvas.restore();

        }

    }


    private void drawFoldAreaAndShadow(Canvas canvas) {

        if (mFlipMode == FlipMode.RIGHT_BOTTOM) {

            //计算填充折叠区域时需要的画布坐标旋转角度
            float rotate = (float) Math.toDegrees(Math.PI / 2f
                    - Math.atan2(mTouch.y - mBezierControl2.y, mViewWidth - mTouch.x));

            canvas.save();
            canvas.clipPath(mFoldAndNextPath);
            canvas.clipPath(mFoldPath, Region.Op.INTERSECT);
            canvas.scale(-1, 1);//x方向镜像
            canvas.translate(-(mViewWidth + mTouch.x), -(mViewHeight - mTouch.y));

            //mTouch在新坐标系下的坐标
            float x = -mTouch.x + mViewWidth + mTouch.x;
            float y = mTouch.y + mViewHeight - mTouch.y;

            canvas.rotate(-rotate, x, y);

            //计算bitmap扭曲后的坐标
            wrapCoordinates();
            //位图的扭曲填充
            canvas.drawBitmapMesh(mBitmapList.get(mPrePage), SUB_WIDTH, SUB_HEIGHT, mCoordinates, 0, null, 0, mFoldPaint);

            canvas.restore();


            //绘制阴影
            float width = mFoldEdgeBtm / 8f;
            int left = (int) (mBezierStart1.x - width - 1);
            int right = (int) (mBezierStart1.x + 1);
            float rotateDegrees = (float) Math.toDegrees(Math.PI / 2f
                    + Math.atan2(mViewHeight - mBezierStart2.y, mViewWidth - mBezierStart1.x));

            //还原坐标轴，绘制阴影
            canvas.save();
            canvas.clipPath(mFoldAndNextPath);
            canvas.clipPath(mFoldPath, Region.Op.INTERSECT);
            canvas.rotate(-rotateDegrees, mBezierStart1.x, mBezierStart1.y);
            mFoldShadowRL.setBounds(left, (int) mBezierStart1.y, right,
                    (int) (mBezierStart1.y + mDiagonalLength));
            mFoldShadowRL.draw(canvas);
            canvas.restore();


        } else if (mFlipMode == FlipMode.RIGHT_TOP) {


            //计算填充折叠区域时需要的画布坐标旋转角度
            float rotate = (float) Math.toDegrees(Math.PI / 2f
                    - Math.atan2(mBezierControl2.y - mTouch.y, mBezierControl2.x - mTouch.x));

            canvas.save();
            canvas.clipPath(mFoldAndNextPath);
            canvas.clipPath(mFoldPath, Region.Op.INTERSECT);
            canvas.scale(-1, 1);//x方向镜像
            canvas.translate(-(mViewWidth + mTouch.x), -(0 - mTouch.y));

            //mTouch在新坐标系下的坐标
            float x = -mTouch.x + mViewWidth + mTouch.x;
            float y = mTouch.y + (0 - mTouch.y);

            canvas.rotate(rotate, x, y);

            //计算bitmap扭曲后的坐标
            wrapCoordinates();

            //位图的扭曲填充
            canvas.drawBitmapMesh(mBitmapList.get(mPrePage), SUB_WIDTH, SUB_HEIGHT, mCoordinates, 0, null, 0, mFoldPaint);

            canvas.restore();


            //绘制阴影
            float width = mFoldEdgeBtm / 8f;
            int left = (int) (mBezierStart1.x - width - 1);
            int right = (int) (mBezierStart1.x + 1);
            float rotateDegrees = (float) Math.toDegrees(Math.PI / 2f
                    + Math.atan2(mBezierStart2.y, mViewWidth - mBezierStart1.x));

            //还原坐标轴，绘制阴影
            canvas.save();
            canvas.clipPath(mFoldAndNextPath);
            canvas.clipPath(mFoldPath, Region.Op.INTERSECT);
            canvas.rotate(rotateDegrees, mBezierStart1.x, mBezierStart1.y);
            mFoldShadowRL.setBounds(left, (int) (mBezierStart1.y - mDiagonalLength), right,
                    (int) mBezierStart1.y);
            mFoldShadowRL.draw(canvas);
            canvas.restore();

        }

    }


    private void drawNextAreaAndShadow(Canvas canvas) {

        // 下一页区域bitmap填充
        canvas.save();
        canvas.clipPath(mFoldAndNextPath);
        canvas.clipPath(mFoldPath, Region.Op.DIFFERENCE);
        canvas.drawBitmap(mBitmapList.get(mNextPage), 0, 0, null);

        //阴影绘制
        float touchToCornerDis = (float) Math.hypot((mTouch.x - mCorner.x), (mTouch.y - mCorner.y));
        int left = (int) (mBezierStart1.x - touchToCornerDis / 5);
        int right = (int) mBezierStart1.x;

        if (mFlipMode == FlipMode.RIGHT_BOTTOM) {

            float rotateDegrees = (float) Math.toDegrees(Math.PI / 2f
                    + Math.atan2(mCorner.y - mBezierStart2.y, mCorner.x - mBezierStart1.x));

            canvas.rotate(-rotateDegrees, mBezierStart1.x, mBezierStart1.y);

            mNextShadowRL.setBounds(left, (int) mBezierStart1.y, right,
                    (int) (mDiagonalLength + mBezierStart1.y));

        } else if (mFlipMode == FlipMode.RIGHT_TOP) {

            float rotateDegrees = (float) Math.toDegrees(Math.PI / 2f
                    + Math.atan2(mCorner.y + mBezierStart2.y, mCorner.x - mBezierStart1.x));

            canvas.rotate(rotateDegrees, mBezierStart1.x, mBezierStart1.y);

            mNextShadowRL.setBounds(left, (int) (mBezierStart1.y - mDiagonalLength), right,
                    (int) mBezierStart1.y);

        }

        mNextShadowRL.draw(canvas);
        canvas.restore();


    }


    public void setFlipStyle(int style) {

        switch (style) {
            case SettingPopup.FLIP_PAGE_LIKE:
                mFlipStyle = FlipStyle.STYLE_PAGE_LIKE;
                break;
            case SettingPopup.FLIP_COVER:
                mFlipStyle = FlipStyle.STYLE_COVER;
                break;
            case SettingPopup.FLIP_NO_EFFECT:
                mFlipStyle = FlipStyle.STYLE_NO_EFFECT;
                break;

        }


    }

    //生成折叠区域的扭曲坐标
    private void wrapCoordinates() {
        //每一个网格的宽高
        float subMinWidth = mViewWidth / (SUB_WIDTH + 1);
        float subMinHeight = mViewHeight / (SUB_HEIGHT + 1);

        int index = 0;

        // 长边偏移
        float offsetLong = CURVATURE / 2F * mFoldEdgeLeft;

        // 长边偏移倍增
        float mulOffsetLong = 1.0F;

        // 短边偏移
        float offsetShort = CURVATURE / 2F * mFoldEdgeBtm;

        // 短边偏移倍增
        float mulOffsetShort = 1.0F;


        // 计算底部扭曲的起始细分下标
        float subWidthStart = Math.round((mBezierControl1.x / subMinWidth)) - 1;
        float subWidthEnd = Math.round(((mBezierControl1.x + CURVATURE * mFoldEdgeBtm) / subMinWidth)) + 1;


        if (mFlipMode == FlipMode.RIGHT_BOTTOM) {

            // 计算右侧扭曲的起始细分下标
            float subHeightStart = Math.round((mBezierControl2.y / subMinHeight)) - 1;
            float subHeightEnd = Math.round(((mBezierControl2.y + CURVATURE * mFoldEdgeLeft) / subMinHeight)) + 1;

            for (int y = 0; y <= SUB_HEIGHT; y++) {
                float fy = mViewHeight * y / SUB_HEIGHT;
                for (int x = 0; x <= SUB_WIDTH; x++) {

                    float fx = mViewWidth * x / SUB_WIDTH;
                    //右侧扭曲
                    if (x == SUB_WIDTH) {
                        if (y >= subHeightStart && y <= subHeightEnd) {
                            fx = mViewWidth * x / SUB_WIDTH + offsetLong * mulOffsetLong;
                            mulOffsetLong = mulOffsetLong / 1.5F;

                        }
                    }

                    //底部扭曲
                    if (y == SUB_HEIGHT) {
                        if (x >= subWidthStart && x <= subWidthEnd) {
                            fy = mViewHeight * y / SUB_HEIGHT + offsetShort * mulOffsetShort;
                            mulOffsetShort = mulOffsetShort / 1.5F;
                        }
                    }

                    mCoordinates[index * 2 + 0] = fx;
                    mCoordinates[index * 2 + 1] = fy;

                    index += 1;
                }
            }
        } else if (mFlipMode == FlipMode.RIGHT_TOP) {

            // 计算右侧扭曲的起始细分下标
            float subHeightStart = Math.round(((mBezierControl2.y - CURVATURE * mFoldEdgeLeft) / subMinHeight)) - 1;
            float subHeightEnd = Math.round((mBezierControl2.y / subMinHeight)) + 1;


            for (int y = 0; y <= SUB_HEIGHT; y++) {
                float fy = mViewHeight * y / SUB_HEIGHT;
                for (int x = 0; x <= SUB_WIDTH; x++) {

                    float fx = mViewWidth * x / SUB_WIDTH;
                    //右侧扭曲
                    if (x == SUB_WIDTH) {
                        if (y >= subHeightStart && y <= subHeightEnd) {
                            fx = mViewWidth * x / SUB_WIDTH + offsetLong * mulOffsetLong;
                            mulOffsetLong = mulOffsetLong * 1.5F;

                        }
                    }

                    //底部扭曲
                    if (y == 0) {
                        if (x >= subWidthStart && x <= subWidthEnd) {
                            fy = mViewHeight * y / SUB_HEIGHT - offsetShort * mulOffsetShort;
                            mulOffsetShort = mulOffsetShort / 1.5F;
                        }
                    }

                    mCoordinates[index * 2 + 0] = fx;
                    mCoordinates[index * 2 + 1] = fy;

                    index += 1;
                }
            }

        }

    }


    private void initDatas() {
        //控件对角线长度
        mDiagonalLength = (float) Math.hypot(mViewWidth, mViewHeight);


        //翻折时x的缓冲，防止完全翻页时折叠区域的曲线无法封闭
        mFoldBuffArea = mViewWidth / 20f;

        //计算自滑界限位置
        mAutoAreaBound = mViewWidth * 6 / 10f;

        //滑动速度
        mSlideSpeedLeft = mViewWidth / 25f;
        mSlideSpeedRight = mViewWidth  / 80f;

        isPrePageOver = SaveHelper.getBoolean(mContext, SaveHelper.IS_PRE_PAGE_OVER);
        int style = SaveHelper.getInt(mContext, SaveHelper.FLIP_STYLE);
        setFlipStyle(style);

    }


    private void initObjects() {

        mTouch = new PointF();
        mCorner = new PointF();
        mLastDownPoint = new PointF();

        mBezierStart1 = new PointF();
        mBezierControl1 = new PointF();
        mBezierEnd1 = new PointF();
        mBezierVertex1 = new PointF();

        mBezierStart2 = new PointF();
        mBezierControl2 = new PointF();
        mBezierEnd2 = new PointF();
        mBezierVertex2 = new PointF();

        mAutoSlideStart = new PointF();

        mFoldPaint = new Paint();
        mFoldPaint.setAntiAlias(true);
        mFoldPaint.setAlpha(0x70);

        mFoldPath = new Path();
        mFoldAndNextPath = new Path();

        mSlideHandler = new SlideHandler();

        //初始化阴影GradientDrawable
        int[] frontShadowColors = new int[]{0x80111111, 0x00111111};//从深到浅
        mCurShadowRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, frontShadowColors);
        mCurShadowRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mCurShadowBT = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, frontShadowColors);
        mCurShadowBT.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mCurShadowTB = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, frontShadowColors);
        mCurShadowTB.setGradientType(GradientDrawable.LINEAR_GRADIENT);


        int[] color = {0x00333333, 0xb0333333}; //从浅到深
        mFoldShadowRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, color);
        mFoldShadowRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFoldShadowLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, color);
        mFoldShadowLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);


        int[] nextShadowColors = new int[]{0xff111111, 0x00111111}; //从深到浅
        mNextShadowRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, nextShadowColors);
        mNextShadowRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);


        mNextShadowLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, nextShadowColors);
        mNextShadowLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }

    public void setPrePageOver(boolean prePageOver) {
        isPrePageOver = prePageOver;
    }

    public boolean isPrePageOver() {
        return isPrePageOver;
    }

}
