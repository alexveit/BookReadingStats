package com.veit.alex.est;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.veit.alex.est.internetLibs.MyBounceInterpolator;
import com.veit.alex.est.util.Utils;

import java.util.Currency;
import java.util.Locale;

public class InfoActivity extends AppCompatActivity {

    private static final int[] INFO_VIEW_ID = {
            R.id.textViewDescription, R.id.textViewInfo1, R.id.textViewInfo2,R.id.textViewInfo3,R.id.textViewInfo4,
            R.id.textViewInfo5,R.id.textViewInfo6,R.id.textViewInfo7,R.id.textViewInfo8, R.id.textViewHowTo,
            R.id.textViewHowTo1, R.id.textViewHowTo2, R.id.textViewHowTo3
    };

    private static final int[] ENG_INFO = {
            R.string.description, R.string.info_p1,R.string.info_p2,R.string.info_p3,R.string.info_p4,
            R.string.info_p5,R.string.info_p6,R.string.info_p7,R.string.info_p8,
            R.string.how_to, R.string.how_to1,R.string.how_to2,R.string.how_to3
    };

    private static final int[] POR_INFO = {
            R.string.descriptionp, R.string.info_p1p,R.string.info_p2p,R.string.info_p3p,R.string.info_p4p,
            R.string.info_p5p,R.string.info_p6p,R.string.info_p7p,R.string.info_p8p,
            R.string.how_top, R.string.how_to1p,R.string.how_to2p,R.string.how_to3p
    };

    private static TextView[] infoViews = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        final TextView eng = (TextView) findViewById(R.id.textViewEnglish);
        final TextView br = (TextView) findViewById(R.id.textViewPortuguese);
        final TextView priceText = (TextView) findViewById(R.id.textViewPrice);
        final TextView pricePacageText = (TextView) findViewById(R.id.textViewPacagePrice);
        TextView priceAmount = (TextView) findViewById(R.id.textViewPriceAmount);
        TextView pricePacageAmount = (TextView) findViewById(R.id.textViewPacagePriceAmount);

        final Animation bounceEng = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow);
        final Animation bouncePor = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow);
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);

        bounceEng.setInterpolator(interpolator);
        bouncePor.setInterpolator(interpolator);

        br.setTypeface(null, Typeface.ITALIC);

        String bookPrice = getIntent().getStringExtra(Utils.BOOK_PRICE);
        if(bookPrice != null) {
            priceAmount.setText(bookPrice);
            double pacagePrice = Double.parseDouble(bookPrice.replaceAll("[^\\d.]", ""));
            Currency.getInstance(Locale.getDefault()).getSymbol();
            pacagePrice *= (Utils.getBooksCount() - 1);
            pricePacageAmount.setText(Currency.getInstance(Locale.getDefault()).getSymbol()
                    + Double.toString(pacagePrice));
        }
        else {
            findViewById(R.id.priceLayout).setVisibility(View.GONE);
        }

        infoViews = new TextView[INFO_VIEW_ID.length];
        for(int i = 0; i < INFO_VIEW_ID.length; i++) {
            infoViews[i] = (TextView) findViewById(INFO_VIEW_ID[i]);
        }

        eng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                eng.startAnimation(bounceEng);

                eng.setTypeface(null, Typeface.BOLD);
                br.setTypeface(null, Typeface.ITALIC);
                priceText.setText(getText(R.string.price_per_book));

                for(int i = 0; i < INFO_VIEW_ID.length; i++) {
                    infoViews[i].setText(getText(ENG_INFO[i]));
                    infoViews[i].setTypeface(null, Typeface.NORMAL);
                }

            }
        });


        br.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                br.startAnimation(bouncePor);

                br.setTypeface(null, Typeface.BOLD_ITALIC);
                eng.setTypeface(null, Typeface.NORMAL);
                priceText.setText(getText(R.string.preco_livro));

                for(int i = 0; i < INFO_VIEW_ID.length; i++) {
                    infoViews[i].setText(getText(POR_INFO[i]));
                    infoViews[i].setTypeface(null, Typeface.ITALIC);
                }

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: onBackPressed(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
