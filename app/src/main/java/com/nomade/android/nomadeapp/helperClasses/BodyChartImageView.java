package com.nomade.android.nomadeapp.helperClasses;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;

import com.nomade.android.nomadeapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * BodyChartImageView
 *
 * This class defines what's needed for the functioning of the BodyChart.
 */
public class BodyChartImageView extends androidx.appcompat.widget.AppCompatImageView {

    private static final String TAG = "BodyChartImageView";

    private ArrayList<Area> areas;
    private Paint textPaint;
    private int maximumAreas = 3;
    private int areaIndex = 0;

    //drawing path
    private Path drawPath;
    //drawing and canvas selector_normal
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    //brush sizes
    private float brushSize;
    //pain level
    private int painLevel;
    //pattern
    private String pattern;
    //erase flag
    private boolean erase=false;

    private NewAreaDrawnListener newAreaDrawnListener;

    public BodyChartImageView(Context context) {
        super(context);
        init();
        setupDrawing();
    }

    public BodyChartImageView(Context context, int maximumAreas) {
        super(context);
        init();
        setupDrawing();
        this.maximumAreas = maximumAreas;
    }

    public BodyChartImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setupDrawing();
    }

    public BodyChartImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setupDrawing();
    }

    //setup drawing
    private void setupDrawing(){

        //prepare for drawing and setup selector_normal stroke properties
        brushSize = 2;
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(getWidth() * 0.02f * brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setAlpha(200);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    //size assigned to view
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    //draw the view - will be called after touch event
    @Override
    protected void onDraw(Canvas canvas) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);

        for (int j = 0; j < areas.size(); j++) {
            float averageX = 0;
            float averageY = 0;
            for (Marker marker : areas.get(j).markerArrayList) {
                averageX += marker.x;
                averageY += marker.y;
            }
            averageX /= areas.get(j).markerArrayList.size();
            averageY /= areas.get(j).markerArrayList.size();

            canvas.drawCircle(averageX, averageY, getWidth() * 0.01f, textPaint);
            float verticalCorrection = 0f;
            float yScaled = averageY / (float) getHeight() * 100f;
            if (yScaled <= 25) {
                verticalCorrection = textPaint.getTextSize() * 0.7f;
            }
            else if (yScaled > 25 && yScaled < 75) {
                verticalCorrection = textPaint.getTextSize() * 0.35f;
            }
            else if (yScaled >= 75) {
                verticalCorrection = 0f;
            }
            textPaint.setTextSize(getWidth() * 0.075f);
            canvas.drawText("" + (j + 1), averageX + getWidth() * 0.02f, averageY + verticalCorrection, textPaint);
        }

        if (brushSize < 4) {
            // new width based implementation
            drawPaint.setStrokeWidth(getWidth() * 0.02f * brushSize);
        }
        else {
            // legacy implementation based on values from resources
            drawPaint.setStrokeWidth(brushSize);
        }
    }

    //register user touches as drawing action
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        if (areaIndex >= maximumAreas || !isEnabled()){
            return false;
        }

        //respond to down, move and up events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                MyLog.d(TAG, "onTouchEvent: ACTION DOWN");
                areas.add(new Area(brushSize, painLevel, pattern, new ArrayList<>()));
                areas.get(areaIndex).markerArrayList.add(new Marker(touchX, touchY));
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                MyLog.d(TAG, "onTouchEvent: ACTION MOVE");
                areas.get(areaIndex).markerArrayList.add(new Marker(touchX, touchY));
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                MyLog.d(TAG, "onTouchEvent: ACTION UP");
                areas.get(areaIndex).markerArrayList.add(new Marker(touchX, touchY));
                drawPath.lineTo(touchX, touchY);
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                areaIndex++;
                if (newAreaDrawnListener != null) {
                    newAreaDrawnListener.onNewAreaDrawn();
                }
                break;
            default:
                return false;
        }
        //redraw
        invalidate();
        return true;
    }

    //update color
    public void setPainLevel(int painLevel) {
//        invalidate();
        this.painLevel = painLevel;
        switch (painLevel) {
            case 1:
                // yellow
                paintColor = Color.parseColor("#FFFFF200");
                break;

            case 2:
                // orange
                paintColor = Color.parseColor("#FFFF7F27");
                break;

            case 3:
                // red
                paintColor = Color.parseColor("#FFED1C24");
                break;
        }
        ColorFilter filter = new PorterDuffColorFilter(paintColor, PorterDuff.Mode.SRC_IN);
        drawPaint.setColorFilter(filter);
        drawPaint.setAlpha(200);
    }

    // update pattern
    public void setPattern(String pattern) {
//        invalidate();
        this.pattern = pattern;
        //check whether filled or pattern name
        if(pattern.equals("filled")){
            drawPaint.setColor(paintColor);
            drawPaint.setShader(null);
            drawPaint.setAlpha(200);
        }
        else if (pattern.equals("pins_and_needles")) {
            //pattern
            int patternID = getResources().getIdentifier(
                    pattern, "drawable", "com.nomade.android.nomadeapp");
            //decode
            Bitmap patternBMP = BitmapFactory.decodeResource(getResources(), patternID);
            //create shader
            BitmapShader patternBMPshader = new BitmapShader(patternBMP,
                    Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            //color and shader
            drawPaint.setColor(paintColor);
            drawPaint.setShader(patternBMPshader);
            drawPaint.setAlpha(200);
        }
    }

    //set brush size
    public void setBrushSize(float newSize) {
        if (newSize < 4) {
            // new width based implementation
            brushSize = newSize;
            drawPaint.setStrokeWidth(getWidth() * 0.02f * brushSize);
        }
        else {
            // legacy implementation based on values from resources
            brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    newSize, getResources().getDisplayMetrics());
            drawPaint.setStrokeWidth(brushSize);
        }
    }

    //get and set maximum areas
    public int getMaximumAreas() {
        return maximumAreas;
    }
    public void setMaximumAreas(int maximumAreas) {
        this.maximumAreas = maximumAreas;
    }

    //set erase true or false
    public void setErase(boolean isErase){
        erase=isErase;
        if(erase) drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        else drawPaint.setXfermode(null);
    }

    //start new drawing
    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    public void reset(){
        areas.clear();
        updateDrawingBasedOnAreas();
    }

    public String[] getAreas() {
        String[] stringAreas = new String[areas.size()];
        try {
            for (int i = 0; i < areas.size(); i++) {
                Area area = areas.get(i);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("brush_size", area.brushSize);
                jsonObject.put("pain_level", area.painLevel);
                jsonObject.put("pattern", area.pattern);

                JSONArray jsonArray = new JSONArray();
                for (int j = 0; j < area.markerArrayList.size(); j++) {
                    Marker marker = area.markerArrayList.get(j);
                    float xScaled = marker.x / (float) getWidth() * 100f;
                    float yScaled = marker.y / (float) getHeight() * 100f;
                    JSONObject jsonMarker = new JSONObject();
                    jsonMarker.put("x", xScaled);
                    jsonMarker.put("y", yScaled);
                    jsonArray.put(jsonMarker);
                }

                jsonObject.put("markers", jsonArray);
                stringAreas[i] = jsonObject.toString();

                MyLog.d(TAG, "getAreas: stringAreas[" + i + "]: " + stringAreas[i]);
            }
            return stringAreas;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setAreas(String[] stringAreas) {
        areas.clear();
        for (String stringArea : stringAreas) {
            try {
                JSONObject jsonObject = new JSONObject(stringArea);
                float brushSize = (float) jsonObject.getDouble("brush_size");
                int painLevel = jsonObject.getInt("pain_level");
                String pattern = jsonObject.getString("pattern");
                ArrayList<Marker> markerArrayList = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray("markers");
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject jsonMarker = jsonArray.getJSONObject(j);
                    float xScaled = (float) jsonMarker.getDouble("x");
                    float yScaled = (float) jsonMarker.getDouble("y");
                    float x = xScaled / 100f * (float) getWidth();
                    float y = yScaled / 100f * (float) getHeight();
                    markerArrayList.add(new Marker(x, y));
                }
                areas.add(new Area(brushSize, painLevel, pattern, markerArrayList));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        areaIndex = areas.size();

        updateDrawingBasedOnAreas();
    }

    public void removeArea(int index) {
        areas.remove(index);
        areaIndex--;

        if (newAreaDrawnListener != null) {
            newAreaDrawnListener.onNewAreaDrawn();
        }

        updateDrawingBasedOnAreas();
    }

    private void updateDrawingBasedOnAreas() {

        float lastBrushSize = brushSize;
        int lastPainLevel = painLevel;
        String lastPattern = pattern;

        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        drawPath = new Path();
        for (Area area : areas) {
            brushSize = area.brushSize;
            if (brushSize < 4) {
                // new width based implementation
                drawPaint.setStrokeWidth(getWidth() * 0.02f * brushSize);
            }
            else {
                // legacy implementation based on values from resources
                drawPaint.setStrokeWidth(brushSize);
            }
            setPainLevel(area.painLevel);
            setPattern(area.pattern);

            drawPath.moveTo(area.markerArrayList.get(0).x, area.markerArrayList.get(0).y);

            for (int j = 1; j < area.markerArrayList.size(); j++) {
                drawPath.lineTo(area.markerArrayList.get(j).x, area.markerArrayList.get(j).y);
            }

            drawCanvas.drawPath(drawPath, drawPaint);
            drawPath.reset();
        }

        brushSize = lastBrushSize;
        if (brushSize < 4) {
            // new width based implementation
            drawPaint.setStrokeWidth(getWidth() * 0.02f * brushSize);
        }
        else {
            // legacy implementation based on values from resources
            drawPaint.setStrokeWidth(brushSize);
        }
        setPainLevel(lastPainLevel);
        if (lastPattern != null) {
            setPattern(lastPattern);
        }

        invalidate();
    }

    private void init(){
        newAreaDrawnListener = null;
        areas = new ArrayList<>();
        textPaint = new Paint();
        textPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.font_size_marker));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(getResources().getColor(R.color.colorPrimary));
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(10, 0, 0, Color.GRAY);
    }

    private static class Area {
        //brush sizes
        float brushSize;
        //pain level
        int painLevel;
        //pattern
        String pattern;
        //path coordinates
        ArrayList<Marker> markerArrayList;

        Area(float brushSize, int painLevel, String pattern, ArrayList<Marker> markerArrayList) {
            this.brushSize = brushSize;
            this.painLevel = painLevel;
            this.pattern = pattern;
            this.markerArrayList = markerArrayList;
        }
    }

    private static class Marker {
        float x;
        float y;

        Marker(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface NewAreaDrawnListener {
        public void onNewAreaDrawn();
    }

    public void setNewAreaDrawnListener(NewAreaDrawnListener listener) {
        this.newAreaDrawnListener = listener;
    }
}
