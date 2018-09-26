package com.ebook;

import android.support.v4.app.Fragment;

public class ShelfActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new ShelfFragment();
    }

}
