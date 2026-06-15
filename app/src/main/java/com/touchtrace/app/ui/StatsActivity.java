package com.touchtrace.app.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.touchtrace.app.R;
import com.touchtrace.app.storage.GestureFileManager;
import java.util.Locale;
import java.util.Map;

public final class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        GestureFileManager mgr = new GestureFileManager(this, getExternalFilesDir(null));

        Map<String, Integer> counts = mgr.getGestureCounts();
        int total = 0;
        for (int v : counts.values()) total += v;

        ((TextView) findViewById(R.id.tvStatsTotal))
                .setText(String.format(Locale.US, "%d TOTAL GESTURES", total));

        StatsChartView chart = findViewById(R.id.chartView);
        chart.setData(counts);

        findViewById(R.id.btnStatsBack).setOnClickListener(v -> finish());
    }
}
