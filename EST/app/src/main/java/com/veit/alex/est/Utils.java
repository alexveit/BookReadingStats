package com.veit.alex.est;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.veit.alex.est.Book;
import com.veit.alex.est.MainActivity;
import com.veit.alex.est.R;
import com.veit.alex.est.SpeakListActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

import static java.nio.charset.Charset.defaultCharset;

/**
 * Created by alex on 7/15/2017.
 *
 * this class does static handling of books array
 *
 * it also handles IO functionality
 */

public class Utils {

    public static final String BOOK_NUM = "BOOK_NUM";
    public static final String STORY_NUM = "STORY_NUM";
    public static final String BOOK_PRICE = "BOOK_PRICE";
    public static final String STORY_LIST = "STORY_LIST";

    private static class LoadResourcesTask extends AsyncTask<Void, Integer, Void> {

        private int mBookCount = -1;
        private MainActivity mMainActivity = null;
        private ProgressBar mProgressBar;
        private boolean mScanOK;
        private boolean mReadOK;

        private double mProgress;

        private LoadResourcesTask(MainActivity mainActivity, int bookCount) {
            mMainActivity = mainActivity;
            mProgressBar = (ProgressBar)mainActivity.findViewById(R.id.resProgressBar);
            mBookCount = bookCount;
            mProgressBar.setMax(bookCount);
            mProgress = 0;

        }

        @Override
        protected Void doInBackground(Void... params) {

            URL url;
            Scanner scanner;
            String bookNumForURLString;
            ArrayList<String> titles = null;
            ArrayList<ArrayList<String>> sentences = null;
            mBooks = new ArrayList<>();

            for(int bookNum = 0; bookNum < mBookCount; bookNum++) {

                bookNumForURLString = ((bookNum + 1) < 10 ? "0" : "") + (bookNum + 1);

                try {
                    url = new URL(mMainActivity.getResources()
                            .getString(R.string.site)
                            + bookNumForURLString
                            + "/b" + bookNumForURLString + ".txt");

                    scanner = new Scanner(url.openStream());
                    scanner.useDelimiter("\n");
                    String text;
                    titles = new ArrayList<>();
                    sentences = new ArrayList<>();
                    int pos = -1;

                    do {

                        text = scanner.next();
                        if (text.contains("#")) {
                            titles.add(text);
                            sentences.add(new ArrayList<String>());
                            pos++;
                        } else {
                            sentences.get(pos).add(text);
                        }
                    } while (scanner.hasNext());
                    scanner.close();
                    mScanOK = true;
                } catch (IOException e) {
                    mScanOK = false;
                }

                if(mScanOK) {
                    String[] urlsThumb = new String[titles.size()];
                    String[] urlsImage = new String[titles.size()];
                    String[] urlsMp3 = new String[titles.size()];
                    String[][] urlsSenMp3 = new String[titles.size()][];

                    String tempUrl = mMainActivity.getResources()
                            .getString(R.string.site) + bookNumForURLString + "/b"
                            + bookNumForURLString + "s";

                    String finalUrl;

                    for (int storyNum = 0; storyNum < titles.size(); storyNum++) {

                        finalUrl = tempUrl + ((storyNum + 1) < 10 ? "0" : "") + (storyNum + 1);

                        urlsThumb[storyNum] = finalUrl + "thumb.jpg";
                        urlsImage[storyNum] = finalUrl + ".jpg";
                        urlsMp3[storyNum] = finalUrl + ".mp3";

                        int senCount = sentences.get(storyNum).size();
                        urlsSenMp3[storyNum] = new String[senCount];
                        String finalUrlSen = finalUrl +"sentences/sen";

                        for(int senNum = 0; senNum < senCount; senNum++) {
                            urlsSenMp3[storyNum][senNum] =
                                    finalUrlSen + ((senNum + 1) < 10 ? "0" : "")
                                            + (senNum + 1) + ".mp3";
                        }

                    }

                    mBooks.add(new Book(titles, sentences, urlsThumb, urlsImage, urlsMp3, urlsSenMp3));
                    mProgress =  mBooks.size() * 0.9;
                    publishProgress((int)mProgress);
                } else {
                    mBooksInitialized = false;
                    break;
                }
            }

            if(mScanOK) {
                mBooksInitialized = true;
                readFile();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
            mProgressBar.setVisibility(View.GONE);
            if(mScanOK && mReadOK) {
                mMainActivity.enableBookButtons();
                mMainActivity.updateUI();
            }
            else {
                mMainActivity.notifyUserAboutLoadError();
            }
        }

        private void readFile() {

            try {
                FileInputStream fis = mMainActivity.openFileInput(mMainActivity.getString(R.string.file_name));
                BufferedReader br = new BufferedReader(new InputStreamReader(fis, defaultCharset()));

                int comma, sentenceCount, bookNum, storyNum;

                String sentencesPassed, input = br.readLine();

                int lastBookNum = 0;

                double progressIncrement = (mBooks.size() - mProgress) / mBooks.size();


                while(input.length() > 0) {

                    //find the next instance of a comma ','
                    //and store it in the comma variable
                    comma = input.indexOf(',');

                    //get the string that precedes the comma
                    //it will look something like this: "0611:0101011"
                    //the first two characters represent the book number "06"
                    //the next two characters represents the story number "11"
                    //everything after the ":" represents the sentences "0101011"
                    //in the previouse case there are seven sentences
                    //where "1" is passed and "0" is not passed
                    //and the user has passed the second, fourth, sixth, and seventh sentence
                    String var = input.substring(0, comma);

                    //since we have stored some data in the var string
                    //now we can remove it from the input string
                    //this eventualy will get us out of this read loop routine
                    input = input.replace(var + ",", "");

                    //the first two characters of var represent the book number
                    bookNum = Integer.parseInt(var.substring(0,2));

                    //the next two characters of var represents the story number
                    storyNum = Integer.parseInt(var.substring(2,4));

                    //the rest are the sentence states of passed or not represented by 1 or 0
                    //note that the 4 character is a ":"
                    //so we get the substring starting from index 5 instead
                    sentencesPassed = var.substring(5,var.length());

                    //get the amount of sentences from this story
                    //that is simply determinet by the string's length
                    sentenceCount = sentencesPassed.length();

                    //initiate a loop to store the state of each sentence of
                    //the particular book and particular story
                    for(int sentenceNum = 0; sentenceNum < sentenceCount; sentenceNum++) {
                        setPassed(bookNum,storyNum,sentenceNum,(sentencesPassed.charAt(sentenceNum) == '1'));
                    }

                    if(lastBookNum != bookNum) {
                        lastBookNum = bookNum;
                        mProgress += progressIncrement;
                        publishProgress((int) mProgress);
                    }
                }

                mProgress += progressIncrement;
                publishProgress((int) mProgress);
                fis.close();
                mReadOK = true;

            } catch (FileNotFoundException e) {
                mReadOK = true;

            } catch (IOException e) {
                mReadOK = false;
            }
        }
    }

    private static class WriteToFile extends AsyncTask<Void, Void, Void> {

        private SpeakListActivity mSpeakListActivity = null;

        private WriteToFile(SpeakListActivity mainActivity) { mSpeakListActivity = mainActivity; }

        @Override
        protected Void doInBackground(Void... params) {

            StringBuilder fileString = new StringBuilder();

            //variable to hold book count
            int bookCount = mBooks.size();

            //start the write loop
            //iterate for all books
            for(int bookNum = 0; bookNum < bookCount; bookNum++) {

                //get story count for this book
                int storyCount = mBooks.get(bookNum).getStoryCount();

                //iterate for every story
                for(int storyNum = 0; storyNum < storyCount; storyNum++) {

                    //get sentence count for this story
                    int sentenceCount = mBooks.get(bookNum).getStorySentenceCount(storyNum);

                    //appent bookNumber storyNumber and a ":"
                    fileString
                            .append(getZerofiedNum(bookNum))
                            .append(getZerofiedNum(storyNum))
                            .append(":");

                    //iterate for every sentence
                    for(int sentenceNum = 0; sentenceNum < sentenceCount; sentenceNum++) {

                        //append 1 or 0 depending on sentence pass state
                        fileString.append(getTrueFalseInt(bookNum,storyNum,sentenceNum));
                    }

                    //append a comma
                    fileString.append(",");
                }
            }
            try {

                //String allPassed = "0000:11111111111,0001:1111111111,0002:111111111,0003:1111111111,0004:1111111,0005:1111111,0006:11111,0007:1111111,0008:11111111,0009:111111,0010:111111111,0011:11111,0012:111111111,0013:111111111,0014:1111111111,0100:1111,0101:111111111,0102:1111111,0103:111111111,0104:11111111,0105:11111,0106:11111111,0107:1111111,0108:11111111,0109:11111111,0110:111111,0111:1111111,0112:11111111,0113:11111111,0114:1111111,0200:111111111,0201:1111111,0202:11111111,0203:111111,0204:1111111,0205:111111111,0206:1111,0207:1111111,0208:111111,0209:1111,0210:111111,0211:11111111,0212:111111,0213:1111111,0214:111111,0300:111111,0301:111111,0302:1111111,0303:11111,0304:11111111,0305:11111,0306:11111,0307:111111,0308:111111,0309:11111111,0310:11111111,0311:111111,0312:111111,0313:111111,0314:11111111,0400:11111,0401:1111111,0402:11111,0403:1111111,0404:11111111,0405:111111,0406:1111111,0407:111111,0408:1111111,0409:111111,0410:1111111,0411:111111,0412:11111,0413:1111111,0414:11111111,0500:1111111,0501:111111,0502:11111,0503:111111,0504:1111,0505:1111,0506:1111111,0507:11111111,0508:1111111,0509:11111111,0510:1111,0511:11111,0512:111111,0600:111111,0601:1111,0602:1111111,0603:11111,0604:111111,0605:1111111,0606:111111,0607:111111,0608:11111111,0609:11111,0610:111111,0611:1111111,0612:111111,0613:111111,0614:11111111,0700:1111111,0701:11111,0702:1111,0703:11111,0704:11111111,0705:1111,0706:111111,0707:1111111,0708:111111,0709:111111,0710:111111,0711:1111111,0712:1111111,0713:11111,0714:11111,0800:11111111,0801:11111111,0802:111111,0803:11111,0804:111111,0805:11111111,0806:1111,0807:111111,0808:11111111,0809:1111111,0810:1111111,0811:11111111,0812:11111,0813:11111111,0900:1111,0901:111111,0902:11111111,0903:11111,0904:1111,0905:1111,0906:111111,0907:1111111,0908:111111,0909:111111,0910:111111,0911:111111,0912:111111111,0913:111111,0914:1111,0915:111111,0916:1111111,0917:11111,1000:1111111,1001:11111,1002:11111,1003:1111111,1004:11111,1005:11111,1006:11111,1007:1111111,1008:111111,1009:11111,1010:111111,1011:1111111,1012:1111,1013:1111111,1014:111111,1015:111111,1016:111111,1017:111111,1100:1111111,1101:111111,1102:1111,1103:111111,1104:111111,1105:111111,1106:11111,1107:11111,1108:111111,1109:11111,1111:11111,1111:111111,1112:11111,1113:1111111,1114:11111,1115:11111111,1116:1111111,1117:1111,1200:11111,1201:11111111,1202:111111,1203:111111,1204:111111,1205:111111,1206:111111,1207:1111111,1208:1111111,1209:1111111,1210:111111,1211:11111,1212:111111,1213:111111,1214:111111111,1215:1111111,1216:111111,1217:11111,1300:11111,1301:11111,1302:11111,1303:11111111,1304:111111111,1305:111111111,1306:1111111,1307:111111,1308:11111,1309:1111111,1310:11111,1311:111111,1312:111111,1313:1111111,1314:111111,1315:1111,1316:11111111,1317:111111,1400:1111,1401:11111,1402:111111,1403:11111111,1404:111111,1405:11111111,1406:11111111,1407:111111,1408:1111111,1409:111111,1410:1111111,1411:11111,1412:1111111,1413:11111,1414:111111,1415:111111,1416:111111,1417:111111,1500:111111,1501:11111,1502:111111,1503:111111,1504:111111,1505:11111,1506:11111,1507:111111,1508:11111,1509:1111111,1510:11111,1511:111111,1512:1111111,1513:111111,1514:11111111,1515:111111,1516:1111,1517:1111111,1600:11111,1601:1111111,1602:111111,1603:11111111,1604:111111,1605:111111,1606:11111,1607:1111,1608:1111111,1609:1111111,1610:11111,1611:111111,1612:11111,1613:11111,1614:1111111,1615:11111,1616:111111,1617:11111,1700:11111,1701:111111111,1702:111111,1703:11111111,1704:1111,1705:11111,1706:1111111,1707:111111,1708:1111111,1709:11111,1710:111111,1711:111111,1712:111111,1713:1111111,1714:1111111,1715:11111,1716:111111,1717:11111111,";

                FileOutputStream fos = mSpeakListActivity.openFileOutput(
                        mSpeakListActivity.getString(R.string.file_name), Context.MODE_PRIVATE);


                fos.write(fileString.toString().getBytes());


                //fos.write(allPassed.getBytes());

                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private static int getTrueFalseInt(int bookNum, int storyNum, int sentenceNum) {
            return (mBooks.get(bookNum).isPassed(storyNum,sentenceNum) ? 1 : 0);
        }

    }

    private static ArrayList<Book> mBooks = null;
    private static boolean mBooksInitialized = false;

    public static String getZerofiedNum(int num) {
        return (num < 10 ? "0" : "") + num;
    }

    public static void addStoryProgressTime(int bookNum, int storyNum, Calendar start) {
        mBooks.get(bookNum).addStoryProgressTime(storyNum,(int)
                (Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis()) / 1000);
    }

    public static String getStoryProgressTimeString(int bookNum, int storyNum) {
        return mBooks.get(bookNum).getStoryProgressTimeString(storyNum);
    }

    public static boolean isBooksInitialized() { return mBooksInitialized; }

    public static boolean hasPassedAllStories(int bookNum) {
        int storyCount = mBooks.get(bookNum).getStoryCount();
        for(int storyNum = 0; storyNum < storyCount; storyNum++) {
            if(!hasPassedAllStorySentences(bookNum,storyNum))
                return false;
        }
        return true;
    }

    public static int getBooksCount() { return mBooks.size(); }

    public static ArrayList<String> getTitles(int bookNum) {
        return mBooks.get(bookNum).getTitles();
    }

    public static String[] getUrlsThumb(int bookNum) {
        return mBooks.get(bookNum).getUrlsThumb();
    }

    public static String getStoryTitleString(int bookNum, int storyNum) {
        String title = "B" + (bookNum + 1) +
                mBooks.get(bookNum).getStoryTile(storyNum).replace('#','S');
        return title;
    }

    public static String getUrlImageString(int bookNum, int storyNum) {
        return mBooks.get(bookNum).getUrlImageString(storyNum);
    }

    public static String getUrlMp3String(int bookNum, int storyNum) {
        return mBooks.get(bookNum).getUrlMp3String(storyNum);
    }

    public static ArrayList<String> getStorySentences(int bookNum, int storyNum) {
        return mBooks.get(bookNum).getStorySentences(storyNum);
    }

    public static String getSentenceMp3URL(int bookNum, int storyNum, int senNum) {
        return mBooks.get(bookNum).getSentenceMp3URL(storyNum,senNum);
    }

    public static void LoadResources(MainActivity mainActivity, int bookCount) {
        new LoadResourcesTask(mainActivity,bookCount).execute();
    }

    public static void writeToFile(SpeakListActivity mSpeakListActivity) {
        new WriteToFile(mSpeakListActivity).execute();
    }

    protected static boolean isPassed(int bookNum, int storyNum, int sentenceNum) {
        return mBooks.get(bookNum).isPassed(storyNum,sentenceNum);
    }

    protected static void setPassed(int bookNum, int storyNum, int sentenceNum, boolean passed) {
        mBooks.get(bookNum).setPassed(storyNum,sentenceNum,passed);
    }

    protected static boolean hasPassedAllStorySentences(int bookNum, int storyNum) {
        int sentenceCount = mBooks.get(bookNum).getStorySentenceCount(storyNum);
        for(int sentenceNum = 0; sentenceNum < sentenceCount; sentenceNum++) {
            if(!mBooks.get(bookNum).isPassed(storyNum,sentenceNum))
                return false;
        }
        return true;
    }

}