package com.veit.alex.est.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veit.alex.est.R;
import com.veit.alex.est.SpeakListActivity;
import com.veit.alex.est.internetLibs.MyBounceInterpolator;
import com.veit.alex.est.internetLibs.SpeechRecognitionHelper;

import java.util.ArrayList;

/**
 * Created by alex on 7/23/2017.
 */

public class MySpeakListAdapter extends BaseAdapter {

    private static final int SPEAK = 555;

    private static class ViewHolder {
        private TextView mText;
        private TextView mSenID;
        private ImageView mCheck;
        private ImageView mEx;
        private LinearLayout mResRow;
        private TextView mResult;
        private ImageButton mMicrophone;
    }

    private LayoutInflater mInflater = null;
    private SpeakListActivity mSpeakListActivity = null;
    private ArrayList<String> mSentences = null;
    private boolean mCheckHasFadedIn[] = null;
    private int mLastSelection= -1;
    private int mBookNum = -1;
    private int mStoryNum = -1;
    private Animation mAnimCheckBounce = null;
    private Animation mAnimExBounce = null;
    private Animation mAnimSpeakBounce = null;
    private Animation mAnimCheckFade = null;
    private ViewHolder mHolder = null;

    /*
    private TextView mStoryProgressTime = null;
    private Calendar mStart = null;
    //private int mSecondsProg = 0;*/

    public MySpeakListAdapter(SpeakListActivity a, TextView elapsed, int bookNum, int storyNum) {

        mSpeakListActivity = a;
        mLastSelection= -1;

        /*
        mStoryProgressTime = elapsed;
        mStoryProgressTime.setText(Utils.getStoryProgressTimeString(bookNum,storyNum));*/

        mInflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mBookNum = bookNum;
        mStoryNum = storyNum;
        mSentences = Utils.getStorySentences(bookNum, storyNum);

        mAnimCheckBounce = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in);
        mAnimExBounce = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in);

        mAnimSpeakBounce = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in_slow);
        mAnimSpeakBounce.setInterpolator(new MyBounceInterpolator(0.2, 20));

        mCheckHasFadedIn = new boolean[mSentences.size()];

        mAnimCheckFade = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_out);
        mAnimCheckFade.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mHolder.mCheck.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    @Override
    public int getCount() { return mSentences.size(); }

    @Override
    public Object getItem(final int position) { return position; }

    @Override
    public long getItemId(final int position) { return position; }

    @Override
    public View getView(final int sentenceNum, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_speak, null);
            holder = new ViewHolder();
            holder.mText = (TextView) convertView.findViewById(R.id.textViewSpeak);
            holder.mSenID = (TextView) convertView.findViewById(R.id.textViewSenID);
            holder.mResRow = (LinearLayout) convertView.findViewById(R.id.resRow);
            holder.mResult = (TextView) convertView.findViewById(R.id.textViewResult);
            holder.mEx = (ImageView) convertView.findViewById(R.id.imageViewEx);
            holder.mCheck = (ImageView) convertView.findViewById(R.id.imageViewCheck);
            holder.mMicrophone = (ImageButton) convertView.findViewById(R.id.imageButtonSpeak);

            holder.mMicrophone.setOnClickListener(new View.OnClickListener() {

                //handle click
                @Override
                public void onClick(View v) {

                    /*mStart = Calendar.getInstance();*/

                    mLastSelection = sentenceNum;
                    mHolder = holder;
                    mHolder.mMicrophone.startAnimation(mAnimSpeakBounce);

                    SpeechRecognitionHelper.run(mSpeakListActivity,
                            SPEAK, mSentences.get(sentenceNum));



                }
            });

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.mText.setText(mSentences.get(sentenceNum));
        holder.mSenID.setText("#" + (sentenceNum+1));

        if(Utils.isPassed(mBookNum, mStoryNum,sentenceNum)) {
            holder.mResRow.setVisibility(View.GONE);
            holder.mCheck.setVisibility(View.VISIBLE);
            if(!mCheckHasFadedIn[sentenceNum] || sentenceNum == mLastSelection) {
                holder.mCheck.startAnimation(mAnimCheckBounce);
                mCheckHasFadedIn[sentenceNum] = true;
            }
        } else {
            if(mLastSelection == sentenceNum) {
                holder.mResRow.setVisibility(View.VISIBLE);
                holder.mEx.startAnimation(mAnimExBounce);
            } else {
                holder.mResRow.setVisibility(View.GONE);
            }
            if(mCheckHasFadedIn[sentenceNum]) {
                mHolder = holder;
                holder.mCheck.startAnimation(mAnimCheckFade);
                mCheckHasFadedIn[sentenceNum] = false;
            } else {
                holder.mCheck.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }

    public boolean processActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SPEAK && resultCode == Activity.RESULT_OK) {

            ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String fromGoogle, fromMemory;
            fromMemory = cleanUpText(mSentences.get(mLastSelection));
            Utils.setPassed(mBookNum,mStoryNum,mLastSelection,false);

            boolean matched = false;
            float highestConfidence = 0;
            int highestConfidenceIndex = 0;
            float[] confidenceScores = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);

            for(int i = 0; i < matches.size(); i++) {

                fromGoogle = cleanUpText(matches.get(i).toString());

                if(confidenceScores[i] > highestConfidence) {
                    highestConfidence = confidenceScores[i];
                    highestConfidenceIndex = i;
                }

                if(fromGoogle.equals(fromMemory)) {
                    Utils.setPassed(mBookNum,mStoryNum,mLastSelection,true);
                    matched = true;
                    break;
                }
            }

            mSpeakListActivity.needsToWriteFile();

            /*
            Utils.addStoryProgressTime(mBookNum,mStoryNum, mStart);
            mStoryProgressTime.setText(Utils.getStoryProgressTimeString(mBookNum,mStoryNum));*/

            if(!matched)
                mHolder.mResult.setText(matches.get(highestConfidenceIndex).toString());

            super.notifyDataSetChanged();

            return true;
        }

        return false;
    }

    private String cleanUpText(String sentence) {
        return sentence
                .toLowerCase()
                .replace(",", "")
                .replace("\"", "")
                .replace(".", "")
                .replace(":", "")
                .replace(";", "")
                .replace("(", "")
                .replace(")", "")
                .replace("!", "")
                .replace("?", "")
                .replaceAll("\\s+","");

    }
}