package com.arduino.sensormonitoringapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeatMapView extends View {
//    private List<Float> temperatureValues;
    private Paint paint;
    private Map<Long, List<Float>> heatmapData;

    private Paint axisPaint;
    private int rows = 10;
    private int cols = 10;


    public HeatMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setTextSize(24);
        heatmapData = new HashMap<>();
    }

    public void setHeatmapData(Map<Long, List<Float>> heatmapData) {
        this.heatmapData = heatmapData;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (heatmapData.isEmpty()) return;

        int cellWidth = getWidth() / cols;
        int cellHeight = getHeight() / rows;

        int colIndex = 0;
        for (Map.Entry<Long, List<Float>> entry : heatmapData.entrySet()) {
            long timestamp = entry.getKey();
            List<Float> temperatures = entry.getValue();
            for (int rowIndex = 0; rowIndex < temperatures.size(); rowIndex++) {
                float temperature = temperatures.get(rowIndex);
                paint.setColor(getColorForTemperature(temperature));

                int left = colIndex * cellWidth;
                int top = rowIndex * cellHeight;
                int right = left + cellWidth;
                int bottom = top + cellHeight;

                canvas.drawRect(left, top, right, bottom, paint);
            }
            colIndex++;
        }

        drawXAxis(canvas, cellWidth);
        drawYAxis(canvas, cellHeight);
    }

    private void drawYAxis(Canvas canvas, int cellHeight) {
        int yAxisX = 50; // Position the Y-axis at the left
        for (int i = 0; i < rows; i++) {
            String tempLabel = String.valueOf(i * 10); // Example: 0, 10, 20, ...
            int y = i * cellHeight + (cellHeight / 2);
            canvas.drawText(tempLabel, yAxisX, y, axisPaint);
        }
    }
    private void drawXAxis(Canvas canvas, int cellWidth) {
        int xAxisY = getHeight() - 50; // Position the X-axis at the bottom
        int colIndex = 0;

        for (Long timestamp : heatmapData.keySet()) {
            String timeLabel = formatTimestamp(timestamp);
            int x = colIndex * cellWidth + (cellWidth / 2);
            canvas.drawText(timeLabel, x, xAxisY, axisPaint);
            colIndex++;
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }



//    private int getColorForTemperature(float temperature) {
//        // Define a color gradient based on temperature
//        if (temperature < 15) {
//            return Color.BLUE;
//        } else if (temperature < 25) {
//            return Color.GREEN;
//        } else if (temperature < 35) {
//            return Color.YELLOW;
//        } else {
//            return Color.RED;
//        }
//    }

    private int getColorForTemperature(float temperature) {
        // Define the minimum and maximum temperature range for your data
        float minTemp = 10; // Adjust this based on your dataset
        float maxTemp = 40; // Adjust this based on your dataset

        // Normalize the temperature to a value between 0 and 1
        float normalized = (temperature - minTemp) / (maxTemp - minTemp);
        normalized = Math.max(0, Math.min(1, normalized)); // Clamp between 0 and 1

        // Define the start and end colors for the gradient
        int startColor = Color.BLUE; // Cooler temperatures
        int endColor = Color.RED;    // Hotter temperatures

        // Interpolate between the start and end colors based on the normalized temperature
        return interpolateColor(startColor, endColor, normalized);
    }

    // Helper method to interpolate between two colors
    private int interpolateColor(int startColor, int endColor, float fraction) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        int interA = (int) (startA + (endA - startA) * fraction);
        int interR = (int) (startR + (endR - startR) * fraction);
        int interG = (int) (startG + (endG - startG) * fraction);
        int interB = (int) (startB + (endB - startB) * fraction);

        return (interA << 24) | (interR << 16) | (interG << 8) | interB;
    }
}
