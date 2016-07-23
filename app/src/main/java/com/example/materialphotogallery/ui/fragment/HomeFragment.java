package com.example.materialphotogallery.ui.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.materialphotogallery.R;
import com.example.materialphotogallery.common.Constants;
import com.example.materialphotogallery.common.ContractFragment;
import com.example.materialphotogallery.custom.CustomImageView;
import com.example.materialphotogallery.custom.CustomItemDecoration;
import com.example.materialphotogallery.custom.CustomMultiChoiceCursorRecyclerViewAdapter;
import com.example.materialphotogallery.custom.CustomRecyclerView;
import com.example.materialphotogallery.event.ModelLoadedEvent;
import com.squareup.picasso.Picasso;

import java.io.File;

import de.greenrobot.event.EventBus;


public class HomeFragment extends ContractFragment<HomeFragment.Contract>{

    private CustomRecyclerView mRecyclerView;

    public interface Contract {
        // TODO handle ViewHolder.OnClick()
    }

    public void onEnterAnimationComplete() {
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.scheduleLayoutAnimation();
    }

    private Cursor mCursor;
    private CustomRecyclerViewAdapter mAdapter;
    private TextView mEmptyView;

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_main, container, false);

        // hide the toolbar shadow on devices API 21+
        View toolbarShadow = view.findViewById(R.id.toolbar_shadow);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbarShadow.setVisibility(View.GONE);
        }
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);
        mRecyclerView = (CustomRecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a 3-col grid on screens >= 540dp
        GridLayoutManager layoutManager = null;
        Configuration config = getResources().getConfiguration();
        if (config.screenWidthDp >= 540) {
            layoutManager = new GridLayoutManager(getActivity(), 3);
        } else {
            layoutManager = new GridLayoutManager(getActivity(), 2);
        }
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new CustomItemDecoration(
                getResources().getDimensionPixelSize(R.dimen.grid_item_space),
                getResources().getDimensionPixelSize(R.dimen.grid_item_space)));
        mAdapter = new CustomRecyclerViewAdapter(getActivity(), mCursor);
        //mAdapter.setMultiChoiceModeListener((AppCompatActivity)getActivity(), this); // FIXME
        if (isAdded()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.setAdapter(mAdapter);
            } else {
                onEnterAnimationComplete();
            }
        }
        if (savedInstanceState != null) {
            mAdapter.restoreInstanceState(savedInstanceState);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        showHideEmpty();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ModelLoadedEvent event) {
        // DEBUG
//        Cursor cursor = event.getModel();
//        if (cursor != null) {
//            while(cursor.moveToNext()) {
//                long id = cursor.getLong(cursor.getColumnIndex(Constants.PHOTO_ID));
//                String filePath = cursor.getString(cursor.getColumnIndex(Constants.PHOTO_FILE_PATH));
//                Timber.i("%s: id: %d, filePath: %s", Constants.LOG_TAG, id, filePath);
//            }
//        }

        // pass the retrieved cursor to the adapter
        mAdapter.changeCursor(event.getModel());
        showHideEmpty();
    }

    private void showHideEmpty() {
        if (mAdapter.getItemCount() > 0) {
            // hide recycler view when empty
            mEmptyView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public class CustomRecyclerViewAdapter extends CustomMultiChoiceCursorRecyclerViewAdapter<CustomViewHolder> {

        public CustomRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.recycler_item_view, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder viewHolder, Cursor cursor) {
            if (cursor != null) {
                viewHolder.bindViewHolder(cursor);
                // highlight any selected notes
                int position = cursor.getPosition();
                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(
                        getActivity(), isSelected(position) ? R.color.colorAccent : R.color.colorSecondaryBackground));
            }
        }

    }


    public class CustomViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener{

        CustomImageView mImageView;
        String mThumbnailPath;

        public CustomViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mImageView = (CustomImageView) itemView.findViewById(R.id.image_view);
        }

        public void bindViewHolder(Cursor cursor) {
            mThumbnailPath = cursor.getString(cursor.getColumnIndex(Constants.PHOTO_THUMBNAIL_PATH));
            Picasso.with(getActivity())
                    .load(new File(mThumbnailPath))
                    .fit()
                    .centerCrop()
                    .into(mImageView);
        }

        @Override
        public void onClick(View view) {
            // TODO propagate to hosting activity
        }

        @Override
        public boolean onLongClick(View view) {
            // TODO
            return false;
        }
    }

}
