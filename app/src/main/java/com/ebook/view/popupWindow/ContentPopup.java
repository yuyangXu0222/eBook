package com.ebook.view.popupWindow;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ebook.R;
import com.ebook.model.Book;

import java.util.List;


/**
 * Created by Administrator on 2017/1/7.
 */

public class ContentPopup extends BasePopupWindow {
    private RecyclerView mRecyclerView;
    private LinearLayout mLinearLayout;
    private Book mBook;

    private OnContentSelectedListener mListener;

    @Override
    protected View createConvertView() {
        return LayoutInflater.from(mContext)
                .inflate(R.layout.popup_content_layout, null);
    }


    public interface OnContentSelectedListener {
        void OnContentClicked(int paraIndex);
    }

    public void setOnContentClicked(OnContentSelectedListener listener) {
        mListener = listener;
    }

    public ContentPopup(Context context, Book book) {
        super(context);
        mBook = book;
        mLinearLayout = (LinearLayout) mConvertView.findViewById(R.id.pop_content_linear_layout);
        mRecyclerView = (RecyclerView) mConvertView.findViewById(R.id.pop_contents_recycle_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(new ContentsAdapter(mBook.getBookContents()));

    }


    private class ContentsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTextView;
        private int mPosition;

        public ContentsHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
            itemView.setOnClickListener(this);
        }

        public void bind(String content, int position) {
            mPosition = position;
            mTextView.setText(content);
        }

        @Override
        public void onClick(View v) {

            if (mListener != null)
                mListener.OnContentClicked(mBook.getContentParaIndexs().get(mPosition));

        }

    }

    private class ContentsAdapter extends RecyclerView.Adapter<ContentsHolder> {
        private List<String> mBookContents;

        public ContentsAdapter(List<String> bookContents) {
            mBookContents = bookContents;
        }


        @Override
        public ContentsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ContentsHolder(view);
        }

        @Override
        public void onBindViewHolder(ContentsHolder holder, int position) {
            holder.bind(mBookContents.get(position), position);

        }

        @Override
        public int getItemCount() {
            return mBookContents.size();
        }
    }


    public void setBackgroundColor(int color) {
        mLinearLayout.setBackgroundColor(color);

    }


}
