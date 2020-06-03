package ru.utkacraft.notchloader.demo;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.utkacraft.notchloader.NotchLoaderView;

public class MainActivity extends AppCompatActivity {
    private NotchLoaderView l;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_start).setOnClickListener(v-> l.tryAppear());
        findViewById(R.id.tv_end).setOnClickListener(v-> l.tryEnd());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        l = NotchLoaderView.install(this);
        l.setTextColor(Color.WHITE);
        l.setText("Demo title");
    }
}