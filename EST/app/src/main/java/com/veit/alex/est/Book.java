package com.veit.alex.est;

import com.veit.alex.est.util.Utils;

import java.util.ArrayList;

/**
 * Created by alex on 8/6/2017.
 *
 * this is the Book class
 *
 * it handles all the sentences from all the stories of a particular book
 *
 */

public class Book {

    private ArrayList<String> mTitles = null;
    private ArrayList<ArrayList<String>> mSentences = null;
    private ArrayList<ArrayList<Boolean>> mPassed = null;
    private String[] mUrlsThumb = null;
    private String[] mUrlsImage = null;
    private String[] mUrlsMp3 = null;

    private int[] mStoryProgressTime = null;

    public Book(ArrayList<String> titles, ArrayList<ArrayList<String>> sentences,
            String[] urlsThumb, String[] urlsImage, String[] urlsMp3) {

        mTitles = titles;
        mSentences = sentences;
        mUrlsImage = urlsImage;
        mUrlsThumb = urlsThumb;
        mUrlsMp3 = urlsMp3;

        mStoryProgressTime = new int[titles.size()];
        mPassed = new ArrayList<>();

        for(int titleNum = 0; titleNum < titles.size(); titleNum++) {

            mPassed.add(new ArrayList<Boolean>());
            mStoryProgressTime[titleNum] = 0;

            for(int sentenceNum = 0; sentenceNum < sentences.get(titleNum).size(); sentenceNum++) {
                mPassed.get(titleNum).add(false);
            }
        }
    }

    public void addStoryProgressTime(int storyNum, int seconds) {
        mStoryProgressTime[storyNum] += seconds;
    }

    public String getStoryProgressTimeString(int storyNum) {

        int min = 0;
        int sec = 0;
        int prog = mStoryProgressTime[storyNum];

        do {
            if (prog >= 60) {
                min++;
                prog -= 60;
            }
        }
        while (prog >= 60);

        sec = prog;

        return Utils.getZerofiedNum(min) + ":" + Utils.getZerofiedNum(sec);
    }

    public void setPassed(int storyNum, int sentenceNum, boolean passed) {
        mPassed.get(storyNum).set(sentenceNum, passed);
    }

    public boolean isPassed(int storyNum, int sentenceNum) {
        return mPassed.get(storyNum).get(sentenceNum);
    }

    public ArrayList<String> getTitles() { return mTitles; }

    public String[] getUrlsThumb() { return mUrlsThumb; }

    public String getUrlImageString(int storyNum) { return mUrlsImage[storyNum]; }

    public String getUrlMp3String(int storyNum) { return mUrlsMp3[storyNum]; }

    public int getStorySentenceCount(int storyNum) { return mSentences.get(storyNum).size(); }

    public String getStoryTile(int storyNum) { return mTitles.get(storyNum); }

    public ArrayList<String> getStorySentences(int storyNum) { return mSentences.get(storyNum); }

    public int getStoryCount() { return mTitles.size(); }
}
