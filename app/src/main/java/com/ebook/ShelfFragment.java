package com.ebook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ebook.model.Book;
import com.ebook.model.BookLab;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2016/12/14.
 */

public class ShelfFragment extends Fragment {
    private Context mContext;
    private List<Book> mBookList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shelf_layout, container, false);
        initEvents(v);
        return v;

    }

    private void initEvents(View v) {
        mContext = getActivity();
        mBookList = BookLab.newInstance(mContext).getBookList();

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.fragment_book_shelf_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        recyclerView.setAdapter(new BookAdapter(mBookList));

    }


    private class BookHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mBookCover;
        private Book mBook;

        public BookHolder(View itemView) {
            super(itemView);
            mBookCover = (ImageView) itemView.findViewById(R.id.item_recycler_view_image_view);
            itemView.setOnClickListener(this);
        }

        public void bind(Book book) {
            mBook = book;
            mBookCover.setImageBitmap(mBook.getBookCover());
        }

        @Override
        public void onClick(View v) {
            Intent intent = ReadingActivity.newIntent(mContext, mBookList.indexOf(mBook));
            startActivity(intent);
        }


    }

    private class BookAdapter extends RecyclerView.Adapter<BookHolder> {
        private List<Book> bookList = new ArrayList<>();

        public BookAdapter(List<Book> bookList) {
            this.bookList = bookList;
        }

        @Override
        public BookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_recycler_view_shelf, parent, false);

            return new BookHolder(view);
        }

        @Override
        public void onBindViewHolder(BookHolder holder, int position) {
            holder.bind(bookList.get(position));
        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }
    }

}
