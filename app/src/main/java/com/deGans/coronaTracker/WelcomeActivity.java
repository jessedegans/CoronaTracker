package com.deGans.coronaTracker;

import androidx.annotation.FontRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;
import com.github.paolorotolo.appintro.model.SliderPagerBuilder;

public class WelcomeActivity extends AppIntro {
    @FontRes int titleTypeface = R.font.rubik_bold;
    @FontRes int descriptionTypeface = R.font.rubik_medium;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showIntroSlides();
    }
    private void showIntroSlides() {
        SliderPagerBuilder pageBuilder = new SliderPagerBuilder();

        SliderPage pageone = pageBuilder.title("How it works")
                .description("This app will monitor your location and let you know if you had contact with a corona patient.")
                .imageDrawable(R.drawable.ic_track)
                .titleTypefaceRes(descriptionTypeface)
                .descTypefaceRes(descriptionTypeface)
                .bgColor(Color.parseColor("#187795"))
                .build();


        SliderPage pagetwo = pageBuilder.title("What happend's if I do?")
                .description("If it turns out that you indeed have corona (after medical attention) then your location history will be anonymously shared with other devices upon confirmation.")
                .imageDrawable(R.drawable.ic_share)
                .titleTypefaceRes(descriptionTypeface)
                .descTypefaceRes(descriptionTypeface)
                .bgColor(Color.parseColor("#1F487E"))
                .build();


        SliderPage pagethree = pageBuilder.title("Privacy #1")
                .description("I find privacy very important and that's why your location history will only be shared (anonymously) if you confirm that you're infected with the corona virus.")
                .imageDrawable(R.drawable.ic_privacy)
                .descTypefaceRes(R.font.rubik_medium)
                .titleTypefaceRes(descriptionTypeface)
                .descTypefaceRes(descriptionTypeface)
                .bgColor(Color.parseColor("#345995"))
                .build();


        addSlide(AppIntroFragment.newInstance(pageone));
        addSlide(AppIntroFragment.newInstance(pagetwo));
        addSlide(AppIntroFragment.newInstance(pagethree));
        showStatusBar(false);
        setSkipTextTypeface(R.font.rubik);
        setDoneTextTypeface(R.font.rubik);
        setFadeAnimation();
    }
    private void goToMain() {
        finish();
    }
    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        goToMain();
    }
    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        goToMain();
    }
    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);

    }
}
