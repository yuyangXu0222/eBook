package com.ebook.view.popupWindow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.ebook.R;

/**
 * Created by xyy on 2017/3/31.
 */

public abstract class BasePopupWindow extends PopupWindow {

    protected Context mContext;
    protected View mConvertView;

    /**
     * @function 返回布局
     */
    protected abstract View createConvertView();


    public BasePopupWindow(Context context) {
        super(context);
        mContext = context;
        mConvertView = createConvertView();
        setContentView(mConvertView);

        //一些常用的基本设置

        //获取屏幕的宽高
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        setSize(metrics.widthPixels, metrics.heightPixels);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        //点击popupWindow外部消失
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

    }


    /**
     * @function 传入屏幕宽高，设置popupWindow默认宽高
     */
    protected void setSize(int width, int height) {
        setWidth(width);
        setHeight((int) (height * 0.85));

    }


}
