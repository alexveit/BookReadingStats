package com.veit.alex.est.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.veit.alex.est.R;
import com.veit.alex.est.StoryListActivity;

import java.util.ArrayList;

public class MyStoryListAdapter extends BaseAdapter {

    private static class ViewHolder {
        private TextView mText;
        private ImageView mPassed;
        private ImageView mThumb;
    }

    private LayoutInflater mInflater = null;
    private StoryListActivity mStoryListActivity = null;
    private ArrayList<String> mTitles = null;
    private String[] mUrlsThumb = null;
    private int mBookNum = 0;
    private double mWidth = 0;
    private double mHeight = 0;
    private boolean mCheckHasFadedIn[] = null;
    private Animation mAnimBounce = null;
    private Animation mAnimFade = null;
    private Animation.AnimationListener mFadeAnimListener = null;
    private ViewHolder mHolder = null;

    public MyStoryListAdapter(StoryListActivity a, int bookNum) {

        mStoryListActivity = a;
        mBookNum = bookNum;
        mInflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTitles = Utils.getTitles(bookNum);
        mUrlsThumb = Utils.getUrlsThumb(bookNum);
        mCheckHasFadedIn = new boolean[mTitles.size()];
        mWidth = mStoryListActivity.getResources().getDisplayMetrics().widthPixels * 0.1868;
        mHeight = mWidth * 0.7137;
        mAnimBounce = AnimationUtils.loadAnimation(mStoryListActivity, R.anim.fade_in);

        mFadeAnimListener = new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mHolder.mPassed.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };

        mAnimFade = AnimationUtils.loadAnimation(mStoryListActivity, R.anim.fade_out);
        mAnimFade.setAnimationListener(mFadeAnimListener);
    }

    @Override
    public int getCount() { return mTitles.size(); }

    @Override
    public Object getItem(final int position) { return position; }

    @Override
    public long getItemId(final int position) { return position; }

    @Override
    public View getView(int storyNum, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if(convertView == null) {

            convertView = mInflater.inflate(R.layout.item_story, null);

            holder = new ViewHolder();
            holder.mText = (TextView) convertView.findViewById(R.id.textViewStory);
            holder.mPassed = (ImageView) convertView.findViewById(R.id.imageViewPassed);
            holder.mThumb = (ImageView) convertView.findViewById(R.id.imageViewThumb);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Glide.with(mStoryListActivity)
                .load(mUrlsThumb[storyNum])
                .override((int)mWidth,(int)mHeight)
                .into(holder.mThumb);

        holder.mText.setText(mTitles.get(storyNum));

        if(Utils.hasPassedAllStorySentences(mBookNum, storyNum)) {
            holder.mPassed.setVisibility(View.VISIBLE);
            if(!mCheckHasFadedIn[storyNum]) {
                holder.mPassed.startAnimation(mAnimBounce);
                mCheckHasFadedIn[storyNum] = true;
            }
        } else {
            if(mCheckHasFadedIn[storyNum]) {
                mHolder = holder;
                holder.mPassed.startAnimation(mAnimFade);
                mCheckHasFadedIn[storyNum] = false;
            } else {
                holder.mPassed.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }
}