package com.veit.alex.est;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.veit.alex.est.internetLibs.MyBounceInterpolator;
import com.veit.alex.est.internetLibs.SpeechRecognitionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by alex on 7/23/2017.
 */

public class SpeakListAdapter extends BaseAdapter {

    private static final int SPEAK = 555;

    private static class ViewHolder {
        private TextView mSentence;
        private ImageView mCheck;
        private ImageView mEx;
        private View mResRow;
        private TextView mResult;
        private TextView mSenNumTextView;
        private ImageButton mMic;
        private ImageButton mPlay;
        private View mSeekLayout;
        private TextView mProgessText;
        private SeekBar mSenSeek;

        public ViewHolder(View convertView) {
            mResRow = convertView.findViewById(R.id.resRow);
            mResult = (TextView) convertView.findViewById(R.id.textViewResult);
            mEx = (ImageView) convertView.findViewById(R.id.imageViewEx);
            mCheck = (ImageView) convertView.findViewById(R.id.imageViewCheck);
            mSentence = (TextView) convertView.findViewById(R.id.textViewSpeak);
            mSenNumTextView = (TextView) convertView.findViewById(R.id.textViewSenID);
            mMic = (ImageButton) convertView.findViewById(R.id.imageButtonSpeak);
            mPlay = (ImageButton) convertView.findViewById(R.id.imageButtonSenPlay);
            mSeekLayout = convertView.findViewById(R.id.seekLayout);
            mProgessText = (TextView) convertView.findViewById(R.id.textViewSenProgress);
            mSenSeek = (SeekBar) convertView.findViewById(R.id.seekBarSen);
        }
    }

    private class SenAudio {
        private MediaPlayer mMP;
        private boolean mIsPrepared;
        private int mDuration;

        public SenAudio() {
            mMP = new MediaPlayer();
            mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mIsPrepared = true;
                    mDuration = mMP.getDuration() / 100;
                    notifyDataSetChanged();
                }
            });
            mIsPrepared = false;
        }

        public void destroy() {
            mMP.stop();
            mMP.release();
            mMP = null;
        }
    }

    private LayoutInflater mInflater = null;
    private SpeakListActivity mSpeakListActivity = null;
    private ArrayList<String> mSentences = null;
    private boolean[] mCheckHasFadedIn = null;
    private SenAudio[] mSenAudios = null;
    private int mBookNum = -1;
    private int mStoryNum = -1;
    private Animation mAnimCheckFadein = null;
    private Animation mAnimPlayBounce = null;
    private Animation mAnimExFadein = null;
    private Animation mAnimSpeakBounce = null;
    private Animation mAnimCheckFade = null;
    private int mLastSelectionNum = -1;
    private int mLastSelectionNumForPlayer = -1;
    private ViewHolder mHolder = null;
    private ViewHolder mHolderForPlayer = null;
    private boolean mIsPlaying;

    public SpeakListAdapter(SpeakListActivity a, int bookNum, int storyNum) {

        mSpeakListActivity = a;

        mInflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mBookNum = bookNum;
        mStoryNum = storyNum;
        mSentences = Utils.getStorySentences(bookNum, storyNum);

        mAnimCheckFadein = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in);
        mAnimExFadein = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in);

        mAnimSpeakBounce = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in_slow);
        mAnimSpeakBounce.setInterpolator(new MyBounceInterpolator(0.2, 20));

        mAnimPlayBounce = AnimationUtils.loadAnimation(mSpeakListActivity, R.anim.fade_in_slow);
        mAnimPlayBounce.setInterpolator(new MyBounceInterpolator(0.2, 20));

        mCheckHasFadedIn = new boolean[mSentences.size()];

        mIsPlaying = false;

        mSenAudios = new SenAudio[mSentences.size()];
        for(int senNum = 0; senNum < mSenAudios.length; senNum++) {
            mSenAudios[senNum] = new SenAudio();

            try {
                mSenAudios[senNum].mMP.setDataSource(Utils.getSentenceMp3URL(bookNum,storyNum,senNum));
                mSenAudios[senNum].mMP.prepareAsync();
            } catch (IOException e) {
                Toast.makeText(mSpeakListActivity, "mp3 for sentence " + (senNum+1)
                        + " not found", Toast.LENGTH_SHORT).show();
            }
        }

        final Handler handler = new Handler();

        mSpeakListActivity.runOnUiThread(new Runnable() {

            final String mFormat = "%02d:%02d/%02d:%02d";
            final long mDelayMillis = 300;
            final Locale mLocale = Locale.getDefault();
            final int SECOND = 1000;
            final int MINUTE = 60 * SECOND;
            final int HOUR = 60 * MINUTE;

            @Override
            public void run() {
                if (mIsPlaying && mSenAudios[mLastSelectionNumForPlayer].mMP != null && mSenAudios[mLastSelectionNumForPlayer].mDuration != 0) {
                    mHolderForPlayer.mProgessText.setText(String.format(mLocale, mFormat,
                            (mSenAudios[mLastSelectionNumForPlayer].mMP.getCurrentPosition() % HOUR) / MINUTE,
                            (mSenAudios[mLastSelectionNumForPlayer].mMP.getCurrentPosition() % MINUTE) / SECOND,
                            (mSenAudios[mLastSelectionNumForPlayer].mMP.getDuration() % HOUR) / MINUTE,
                            (mSenAudios[mLastSelectionNumForPlayer].mMP.getDuration() % MINUTE) / SECOND));
                    mHolderForPlayer.mSenSeek.setProgress(mSenAudios[mLastSelectionNumForPlayer].mMP.getCurrentPosition() / mSenAudios[mLastSelectionNumForPlayer].mDuration);
                }
                handler.postDelayed(this, mDelayMillis);
            }
        });

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
    public View getView(final int senNum, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_speak, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.mSenNumTextView.setText("#" + (senNum+1));

        setupAudioUI(holder,senNum);

        holder.mMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseLastSelectedAudio();
                mHolder = holder;
                mLastSelectionNum = senNum;
                holder.mMic.startAnimation(mAnimSpeakBounce);
                SpeechRecognitionHelper.run(mSpeakListActivity,
                        SPEAK, mSentences.get(senNum));
            }
        });

        holder.mSentence.setText(mSentences.get(senNum));

        if(Utils.isPassed(mBookNum, mStoryNum,senNum)) {
            holder.mResRow.setVisibility(View.GONE);
            holder.mCheck.setVisibility(View.VISIBLE);
            if(!mCheckHasFadedIn[senNum] || senNum == mLastSelectionNum) {
                holder.mCheck.startAnimation(mAnimCheckFadein);
                mCheckHasFadedIn[senNum] = true;
            }
        } else {
            if (mLastSelectionNum == senNum) {
                holder.mResRow.setVisibility(View.VISIBLE);
                holder.mEx.startAnimation(mAnimExFadein);
            } else {
                holder.mResRow.setVisibility(View.GONE);
            }
            if(mCheckHasFadedIn[senNum]) {
                mHolder = holder;
                holder.mCheck.startAnimation(mAnimCheckFade);
                mCheckHasFadedIn[senNum] = false;
            } else {
                holder.mCheck.setVisibility(View.INVISIBLE);
            }
        }

        return convertView;
    }

    private void setupAudioUI(final ViewHolder holder, final int senNum) {

        if(mSenAudios[senNum].mIsPrepared) {
            if(mSenAudios[senNum].mMP.isPlaying()) {
                holder.mPlay.setImageResource(R.mipmap.btn_pause_sml);
                holder.mSeekLayout.setVisibility(View.VISIBLE);
                mHolderForPlayer = holder;
            }
            else {
                holder.mPlay.setImageResource(R.mipmap.btn_play_sml);
                holder.mSeekLayout.setVisibility(View.GONE);
            }
            holder.mPlay.setEnabled(true);
            holder.mPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mSenAudios[senNum].mMP.isPlaying()) {
                        mIsPlaying = false;
                        mSenAudios[senNum].mMP.pause();
                        holder.mPlay.setImageResource(R.mipmap.btn_play_sml);
                        holder.mSeekLayout.setVisibility(View.GONE);
                    }
                    else {
                        pauseLastSelectedAudio();
                        mLastSelectionNumForPlayer = senNum;
                        mIsPlaying = true;
                        mSenAudios[senNum].mMP.start();
                        holder.mPlay.setImageResource(R.mipmap.btn_pause_sml);
                        holder.mSeekLayout.setVisibility(View.VISIBLE);
                        mHolderForPlayer = holder;
                    }
                    holder.mPlay.startAnimation(mAnimPlayBounce);
                }
            });
            mSenAudios[senNum].mMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mIsPlaying = false;
                    holder.mPlay.setImageResource(R.mipmap.btn_play_sml);
                    holder.mSeekLayout.setVisibility(View.GONE);
                }
            });
            holder.mSenSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { mSenAudios[senNum].mMP.start(); }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { mSenAudios[senNum].mMP.pause(); }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mSenAudios[senNum].mMP != null && fromUser) {
                        mSenAudios[senNum].mMP.seekTo(progress * mSenAudios[senNum].mDuration);
                    }
                }
            });

        }
        else
            holder.mPlay.setEnabled(false);

    }

    private void pauseLastSelectedAudio() {
        if(mLastSelectionNumForPlayer >= 0) {
            mSenAudios[mLastSelectionNumForPlayer].mMP.pause();
            mHolderForPlayer.mPlay.setImageResource(R.mipmap.btn_play_sml);
            mHolderForPlayer.mSeekLayout.setVisibility(View.GONE);
        }
    }

    public boolean processActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEAK && resultCode == Activity.RESULT_OK) {
            ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String fromGoogle, fromMemory;
            fromMemory = cleanUpText(mSentences.get(mLastSelectionNum));
            Utils.setPassed(mBookNum,mStoryNum,mLastSelectionNum,false);
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
                    Utils.setPassed(mBookNum,mStoryNum,mLastSelectionNum,true);
                    matched = true;
                    break;
                }
            }
            mSpeakListActivity.needsToWriteFile();
            if(!matched)
                mHolder.mResult.setText(matches.get(highestConfidenceIndex).toString());
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    public void destroy() {
        mIsPlaying = false;
        for(int senNum = 0; senNum < mSenAudios.length; senNum++) {
            mSenAudios[senNum].destroy();
        }
        mSenAudios = null;
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