package com.nomade.android.nomadeapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.setupClasses.Snap;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.Setup;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import eltos.simpledialogfragment.SimpleDialog;

/**
 * BodyDragAndDropActivity
 *
 * Activity to configure a Setup using a drag and drop system
 */
public class BodyDragAndDropActivity extends AppCompatActivity implements View.OnDragListener, View.OnLongClickListener, SimpleDialog.OnDialogResultListener {

    private static final String TAG = "BodyDragAndDropActivity";
    private ProgressDialog pDialog;
    private Context context = this;

    private SharedPreferences.Editor setupEditor;

    private ViewOutlineProvider mOutlineProviderCircle;

    private LinearLayout gridParent;
    private LinearLayout dragViewParent;
    private LinearLayout dragViewContainer;
    private LinearLayout floatingParent;
    private LinearLayout floatingContainer;
    private LinearLayout deleteContainer;

    private int childSize;
    private int parentSize;

    private LinearLayout.LayoutParams bigDragViewParams;
    private LinearLayout.LayoutParams smallDragViewParams;

    private TextView dragViewContainerTextView;

    private Snap[] targetSnaps;

    private String jsonSetup;
    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private Setup setup;
    private Setup oldSetup;

    private ArrayList<Integer> saveIdArrayList = new ArrayList<>();
    private ArrayList<String> saveJsonArrayList = new ArrayList<>();
    private int saveIndex = 0;
    private ArrayList<String> deleteUrlArrayList = new ArrayList<>();
    private int deleteIndex = 0;

    private ArrayList<ImageView> imageViewArrayList = new ArrayList<>();

    private final int ADD_INSTRUMENT = 0;
    private final int EDIT_INSTRUMENT = 1;

    private static final String COULD_NOT_SAVE_DIALOG = "dialogTagCouldNotSave";
    private static final String SAVE_CHANGES_DIALOG = "dialogTagSaveChanges";
    private static final String QUIT_DIALOG = "dialogTagQuit";
    private static final String LOCK_SETUP_DIALOG = "dialogTagLockSetup";

    private PDDocument document;
    private PDPage page;
    private PDPageContentStream contentStream;
    private float width;
    private float startY;
    private float endY;
    private float heightCounter;
    private float currentXPosition;
    private float wrapOffsetY;

    private boolean downloadingAllPdfs = false;
    private ArrayList<Integer> setupIdArrayList;
    private int downloadPdfIndex;
    private int requestErrors = 0;
    private List<Integer> requestErrorIds = new ArrayList<>();
    private int pdfErrors = 0;
    private List<Integer> pdfErrorIds = new ArrayList<>();
    private int setupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_drag_and_drop);

        SharedPreferences setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        mOutlineProviderCircle = new CircleOutlineProvider();

        gridParent = findViewById(R.id.grid_parent);
        dragViewParent = findViewById(R.id.drag_view_parent);
        floatingParent = findViewById(R.id.floating_parent);
        deleteContainer = findViewById(R.id.delete_container);
        deleteContainer.setOnDragListener(this);

        dragViewContainerTextView = findViewById(R.id.drag_view_container_text_view);

//        addRelativeLayout();

        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        if (savedInstanceState != null){
            setup = savedInstanceState.getParcelable("STATE_SETUP");
            oldSetup = savedInstanceState.getParcelable("STATE_OLD_SETUP");
            jsonSetup = savedInstanceState.getString("STATE_JSON_SETUP");
            addRelativeLayout();
            addDragViewsFromSetup();
        }
        else if (getIntent().hasExtra("DOWNLOAD_PDF_IDS")) {
            downloadingAllPdfs = true;
            setupIdArrayList = getIntent().getIntegerArrayListExtra("DOWNLOAD_PDF_IDS");
            if (setupIdArrayList != null) {
                downloadPdfIndex = 0;
                pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + setupIdArrayList.size() + ")");
                showDialog();
                downloadAllPdfFiles();
            }
        }
        else {
            int setupId = getIntent().getIntExtra("SETUP_ID", 0);
            if (setupId > 0){
                getSetupById(setupId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (setup != null){
            setTitle(setup.getName());
            if (setup.isLocked() || setup.isObsolete()){
                ActionBar actionBar = getActionBar();
                if (actionBar != null){
                    actionBar.setDisplayShowHomeEnabled(true);
                    actionBar.setIcon(R.drawable.ic_lock_light);
                }

                deleteContainer.setVisibility(View.GONE);
                dragViewParent.setVisibility(View.GONE);
                dragViewContainerTextView.setVisibility(View.GONE);

                invalidateOptionsMenu();
            }
            else {
                deleteContainer.setVisibility(View.VISIBLE);
                dragViewParent.setVisibility(View.VISIBLE);
                dragViewContainerTextView.setVisibility(View.VISIBLE);

                invalidateOptionsMenu();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("STATE_SETUP", setup);
        outState.putParcelable("STATE_OLD_SETUP", oldSetup);
        outState.putString("STATE_JSON_SETUP", jsonSetup);
        super.onSaveInstanceState(outState);
    }

    /**
     * Prepares the grid and containers for usage.
     */
    private void addRelativeLayout() {
        MyLog.d(TAG, "addRelativeLayout");

        int height = getResources().getDisplayMetrics().heightPixels;
        int width = getResources().getDisplayMetrics().widthPixels;

        int gridSize = 15;
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//            // Landscape
//            childSize = (height - getHeightStatusActionSoft() - 100) / gridSize;
//
//            if (childSize * gridSize + 100 > width / 2){
//                childSize = (width - 200) / 2 / gridSize;
//            }
//
//        }
//        else {
//            // Portrait
//            childSize = (height - getHeightStatusActionSoft()) / (gridSize + 3);
//
//            if (childSize * gridSize + 300 > width){
//                childSize = (width - 300) / gridSize;
//            }
//        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            // Landscape
            childSize = (height - getHeightStatusActionSoft()) / (gridSize + 2);
            MyLog.d("Landscape", "");
        }
        else {
            // Portrait
            childSize = width / (gridSize + 2);
            MyLog.d("Portrait", "");
        }

        MyLog.d("height", "" + height);
        MyLog.d("width", "" + width);
        MyLog.d("getHeightStatusActionSoft", "" + getHeightStatusActionSoft());
        MyLog.d("gridSize", "" + gridSize);
        MyLog.d("childSize", "" + childSize);

        parentSize = childSize * gridSize;

        int dragViewSize = childSize * 8 / 10;

        MyLog.d(TAG,"addRelativeLayout | Parent Size: " + parentSize + ", Child Size: " + childSize + ", Grid Size: " + gridSize + ", DragView Size: " + dragViewSize);

        bigDragViewParams = new LinearLayout.LayoutParams( dragViewSize * 3 / 2, dragViewSize * 3 / 2);
        bigDragViewParams.setMargins(10,10,10,10);

        smallDragViewParams = new LinearLayout.LayoutParams(dragViewSize, dragViewSize);
        smallDragViewParams.setMargins(10,10,10,10);

        RelativeLayout parent = new RelativeLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(parentSize, parentSize);
        parent.setLayoutParams(layoutParams);
        gridParent.addView(parent);

//        String jsonString = "{\"coords\":[{\"x\":0,\"y\":0,\"r\":[]},{\"x\":-1,\"y\":0,\"r\":[]},{\"x\":1,\"y\":0,\"r\":[]},{\"x\":-4,\"y\":3,\"r\":[270]},{\"x\":-3,\"y\":4,\"r\":[0]},{\"x\":-4,\"y\":4,\"r\":[315]},{\"x\":4,\"y\":4,\"r\":[45]},{\"x\":4,\"y\":3,\"r\":[90]},{\"x\":3,\"y\":4,\"r\":[0]},{\"x\":-3,\"y\":-6,\"r\":[180]},{\"x\":-4,\"y\":-6,\"r\":[225]},{\"x\":-4,\"y\":-5,\"r\":[270]},{\"x\":3,\"y\":-6,\"r\":[180]},{\"x\":4,\"y\":-6,\"r\":[135]},{\"x\":4,\"y\":-5,\"r\":[90]},{\"x\":4,\"y\":-1,\"r\":[90]},{\"x\":-4,\"y\":-1,\"r\":[270]},{\"x\":0,\"y\":-6,\"r\":[180]}]}";
//        String jsonString = "{\"coords\":[{\"x\":-4,\"y\":3},{\"x\":-3,\"y\":4},{\"x\":-4,\"y\":4},{\"x\":4,\"y\":4},{\"x\":4,\"y\":3},{\"x\":3,\"y\":4},{\"x\":-3,\"y\":-6},{\"x\":-4,\"y\":-6},{\"x\":-4,\"y\":-5},{\"x\":3,\"y\":-6},{\"x\":4,\"y\":-6},{\"x\":4,\"y\":-5},{\"x\":4,\"y\":-1},{\"x\":-4,\"y\":-1},{\"x\":0,\"y\":-6}]}";
//        String jsonString = "{\"coords\":[{\"x\":-4,\"y\":4,\"r\":-45},{\"x\":-4,\"y\":3,\"r\":-90},{\"x\":-4,\"y\":2,\"r\":-90},{\"x\":-4,\"y\":1,\"r\":-90},{\"x\":-4,\"y\":0,\"r\":-90},{\"x\":-4,\"y\":-1,\"r\":-90},{\"x\":-4,\"y\":-2,\"r\":-90},{\"x\":-4,\"y\":-3,\"r\":-90},{\"x\":-4,\"y\":-4,\"r\":-90},{\"x\":-4,\"y\":-5,\"r\":-90},{\"x\":-4,\"y\":-6,\"r\":-135},{\"x\":-3,\"y\":4,\"r\":0},{\"x\":-3,\"y\":-6,\"r\":180},{\"x\":-2,\"y\":4,\"r\":0},{\"x\":-2,\"y\":-6,\"r\":180},{\"x\":-1,\"y\":4,\"r\":0},{\"x\":-1,\"y\":-6,\"r\":180},{\"x\":0,\"y\":4,\"r\":0},{\"x\":0,\"y\":-6,\"r\":180},{\"x\":1,\"y\":4,\"r\":0},{\"x\":1,\"y\":-6,\"r\":180},{\"x\":2,\"y\":4,\"r\":0},{\"x\":2,\"y\":-6,\"r\":180},{\"x\":3,\"y\":4,\"r\":0},{\"x\":3,\"y\":-6,\"r\":180},{\"x\":4,\"y\":4,\"r\":45},{\"x\":4,\"y\":3,\"r\":90},{\"x\":4,\"y\":2,\"r\":90},{\"x\":4,\"y\":1,\"r\":90},{\"x\":4,\"y\":0,\"r\":90},{\"x\":4,\"y\":-1,\"r\":90},{\"x\":4,\"y\":-2,\"r\":90},{\"x\":4,\"y\":-3,\"r\":90},{\"x\":4,\"y\":-4,\"r\":90},{\"x\":4,\"y\":-5,\"r\":90},{\"x\":4,\"y\":-6,\"r\":135}]}";
//        String jsonString = "{\"coords\":[{\"x\":-50,\"y\":50,\"r\":0},{\"x\":-40,\"y\":50,\"r\":0},{\"x\":-30,\"y\":50,\"r\":0},{\"x\":-20,\"y\":50,\"r\":0},{\"x\":-10,\"y\":50,\"r\":0},{\"x\":0,\"y\":50,\"r\":0},{\"x\":10,\"y\":50,\"r\":0},{\"x\":20,\"y\":50,\"r\":0},{\"x\":30,\"y\":50,\"r\":0},{\"x\":40,\"y\":50,\"r\":0},{\"x\":50,\"y\":50,\"r\":0},{\"x\":-50,\"y\":40,\"r\":0},{\"x\":-40,\"y\":40,\"r\":0},{\"x\":-30,\"y\":40,\"r\":0},{\"x\":-20,\"y\":40,\"r\":0},{\"x\":-10,\"y\":40,\"r\":0},{\"x\":0,\"y\":40,\"r\":0},{\"x\":10,\"y\":40,\"r\":0},{\"x\":20,\"y\":40,\"r\":0},{\"x\":30,\"y\":40,\"r\":0},{\"x\":40,\"y\":40,\"r\":0},{\"x\":50,\"y\":40,\"r\":0},{\"x\":-50,\"y\":30,\"r\":0},{\"x\":-40,\"y\":30,\"r\":0},{\"x\":-30,\"y\":30,\"r\":0},{\"x\":-20,\"y\":30,\"r\":0},{\"x\":-10,\"y\":30,\"r\":0},{\"x\":0,\"y\":30,\"r\":0},{\"x\":10,\"y\":30,\"r\":0},{\"x\":20,\"y\":30,\"r\":0},{\"x\":30,\"y\":30,\"r\":0},{\"x\":40,\"y\":30,\"r\":0},{\"x\":50,\"y\":30,\"r\":0},{\"x\":-50,\"y\":20,\"r\":0},{\"x\":-40,\"y\":20,\"r\":0},{\"x\":-30,\"y\":20,\"r\":0},{\"x\":-20,\"y\":20,\"r\":0},{\"x\":-10,\"y\":20,\"r\":0},{\"x\":0,\"y\":20,\"r\":0},{\"x\":10,\"y\":20,\"r\":0},{\"x\":20,\"y\":20,\"r\":0},{\"x\":30,\"y\":20,\"r\":0},{\"x\":40,\"y\":20,\"r\":0},{\"x\":50,\"y\":20,\"r\":0},{\"x\":-50,\"y\":10,\"r\":0},{\"x\":-40,\"y\":10,\"r\":0},{\"x\":-30,\"y\":10,\"r\":0},{\"x\":-20,\"y\":10,\"r\":0},{\"x\":-10,\"y\":10,\"r\":0},{\"x\":0,\"y\":10,\"r\":0},{\"x\":10,\"y\":10,\"r\":0},{\"x\":20,\"y\":10,\"r\":0},{\"x\":30,\"y\":10,\"r\":0},{\"x\":40,\"y\":10,\"r\":0},{\"x\":50,\"y\":10,\"r\":0},{\"x\":-50,\"y\":0,\"r\":0},{\"x\":-40,\"y\":0,\"r\":0},{\"x\":-30,\"y\":0,\"r\":0},{\"x\":-20,\"y\":0,\"r\":0},{\"x\":-10,\"y\":0,\"r\":0},{\"x\":0,\"y\":0,\"r\":0},{\"x\":10,\"y\":0,\"r\":0},{\"x\":20,\"y\":0,\"r\":0},{\"x\":30,\"y\":0,\"r\":0},{\"x\":40,\"y\":0,\"r\":0},{\"x\":50,\"y\":0,\"r\":0},{\"x\":-50,\"y\":-10,\"r\":0},{\"x\":-40,\"y\":-10,\"r\":0},{\"x\":-30,\"y\":-10,\"r\":0},{\"x\":-20,\"y\":-10,\"r\":0},{\"x\":-10,\"y\":-10,\"r\":0},{\"x\":0,\"y\":-10,\"r\":0},{\"x\":10,\"y\":-10,\"r\":0},{\"x\":20,\"y\":-10,\"r\":0},{\"x\":30,\"y\":-10,\"r\":0},{\"x\":40,\"y\":-10,\"r\":0},{\"x\":50,\"y\":-10,\"r\":0},{\"x\":-50,\"y\":-20,\"r\":0},{\"x\":-40,\"y\":-20,\"r\":0},{\"x\":-30,\"y\":-20,\"r\":0},{\"x\":-20,\"y\":-20,\"r\":0},{\"x\":-10,\"y\":-20,\"r\":0},{\"x\":0,\"y\":-20,\"r\":0},{\"x\":10,\"y\":-20,\"r\":0},{\"x\":20,\"y\":-20,\"r\":0},{\"x\":30,\"y\":-20,\"r\":0},{\"x\":40,\"y\":-20,\"r\":0},{\"x\":50,\"y\":-20,\"r\":0},{\"x\":-50,\"y\":-30,\"r\":0},{\"x\":-40,\"y\":-30,\"r\":0},{\"x\":-30,\"y\":-30,\"r\":0},{\"x\":-20,\"y\":-30,\"r\":0},{\"x\":-10,\"y\":-30,\"r\":0},{\"x\":0,\"y\":-30,\"r\":0},{\"x\":10,\"y\":-30,\"r\":0},{\"x\":20,\"y\":-30,\"r\":0},{\"x\":30,\"y\":-30,\"r\":0},{\"x\":40,\"y\":-30,\"r\":0},{\"x\":50,\"y\":-30,\"r\":0},{\"x\":-50,\"y\":-40,\"r\":0},{\"x\":-40,\"y\":-40,\"r\":0},{\"x\":-30,\"y\":-40,\"r\":0},{\"x\":-20,\"y\":-40,\"r\":0},{\"x\":-10,\"y\":-40,\"r\":0},{\"x\":0,\"y\":-40,\"r\":0},{\"x\":10,\"y\":-40,\"r\":0},{\"x\":20,\"y\":-40,\"r\":0},{\"x\":30,\"y\":-40,\"r\":0},{\"x\":40,\"y\":-40,\"r\":0},{\"x\":50,\"y\":-40,\"r\":0},{\"x\":-50,\"y\":-50,\"r\":0},{\"x\":-40,\"y\":-50,\"r\":0},{\"x\":-30,\"y\":-50,\"r\":0},{\"x\":-20,\"y\":-50,\"r\":0},{\"x\":-10,\"y\":-50,\"r\":0},{\"x\":0,\"y\":-50,\"r\":0},{\"x\":10,\"y\":-50,\"r\":0},{\"x\":20,\"y\":-50,\"r\":0},{\"x\":30,\"y\":-50,\"r\":0},{\"x\":40,\"y\":-50,\"r\":0},{\"x\":50,\"y\":-50,\"r\":0}]}";
        String oldLocations = "{\"coords\":[{\"x\":-27,\"y\":55,\"r\":0,\"d\":\"front head\"},{\"x\":26,\"y\":54,\"r\":0,\"d\":\"back head\"},{\"x\":-39,\"y\":25,\"r\":0,\"d\":\"front right upper arm\"},{\"x\":-15,\"y\":25,\"r\":0,\"d\":\"front left upper arm\"},{\"x\":14,\"y\":25,\"r\":0,\"d\":\"back left upper arm\"},{\"x\":38,\"y\":25,\"r\":0,\"d\":\"back right upper arm\"},{\"x\":-41,\"y\":10,\"r\":0,\"d\":\"front right lower arm\"},{\"x\":-12,\"y\":10,\"r\":0,\"d\":\"front left lower arm\"},{\"x\":11,\"y\":10,\"r\":0,\"d\":\"back left lower arm\"},{\"x\":40,\"y\":10,\"r\":0,\"d\":\"back right lower arm\"},{\"x\":-32,\"y\":-8,\"r\":0,\"d\":\"front right upper leg\"},{\"x\":-22,\"y\":-8,\"r\":0,\"d\":\"front left upper leg\"},{\"x\":21,\"y\":-8,\"r\":0,\"d\":\"back left upper leg\"},{\"x\":31,\"y\":-8,\"r\":0,\"d\":\"back right upper leg\"},{\"x\":-31,\"y\":-35,\"r\":0,\"d\":\"front right lower leg\"},{\"x\":-22,\"y\":-35,\"r\":0,\"d\":\"front left lower leg\"},{\"x\":22,\"y\":-35,\"r\":0,\"d\":\"back left lower leg\"},{\"x\":30,\"y\":-35,\"r\":0,\"d\":\"back right lower leg\"},{\"x\":-27,\"y\":30,\"r\":0,\"d\":\"front upper body\"},{\"x\":26,\"y\":30,\"r\":0,\"d\":\"back upper body\"},{\"x\":-27,\"y\":18,\"r\":0,\"d\":\"front lower body\"},{\"x\":26,\"y\":18,\"r\":0,\"d\":\"back lower body\"}]}";
        String newLocations = "{\"coords\":[{\"n\":1,\"x\":-27,\"y\":60,\"d\":\"Top of the head\"},{\"n\":2,\"x\":26,\"y\":48,\"d\":\"Back of the head\"},{\"n\":3,\"x\":-27,\"y\":51,\"d\":\"Front of the head\"},{\"n\":4,\"x\":-20,\"y\":51,\"d\":\"Left Temple\"},{\"n\":4,\"x\":-34,\"y\":51,\"d\":\"Right Temple\"},{\"n\":8,\"x\":-27,\"y\":43,\"d\":\"Chin\"},{\"n\":9,\"x\":26,\"y\":41,\"d\":\"Vertebrae name (C1 to C7)\"},{\"n\":10,\"x\":26,\"y\":29,\"d\":\"Vertebrae name (T1 to T12)\"},{\"n\":11,\"x\":26,\"y\":13,\"d\":\"Vertebrae name (L1 to L5)\"},{\"n\":12,\"x\":26,\"y\":6,\"d\":\"Pelvis S2\"},{\"n\":13,\"x\":32,\"y\":6,\"d\":\"Right Pelvis SIPS\"},{\"n\":14,\"x\":20,\"y\":6,\"d\":\"Left Pelvis SIPS\"},{\"n\":15,\"x\":-34,\"y\":10,\"d\":\"Right Pelvis SIAS\"},{\"n\":16,\"x\":-20,\"y\":10,\"d\":\"Left Pelvis SIAS\"},{\"n\":17,\"x\":-27,\"y\":27,\"d\":\"Body of Sternum\"},{\"n\":18,\"x\":-34,\"y\":22,\"d\":\"Right Rib (T5 to T10)\"},{\"n\":19,\"x\":-20,\"y\":22,\"d\":\"Left Rib (T5 to T10)\"},{\"n\":20,\"x\":-16,\"y\":37,\"d\":\"Left Acromion(superior face)\"},{\"n\":20,\"x\":-38,\"y\":37,\"d\":\"Right Acromion (superior face)\"},{\"n\":21,\"x\":-12,\"y\":24,\"d\":\"Left Humerus (lateraly)\"},{\"n\":21,\"x\":-42,\"y\":24,\"d\":\"Right Humerus (lateraly)\"},{\"n\":23,\"x\":2,\"y\":2,\"d\":\"Left Radius (distally and laterally)\"},{\"n\":23,\"x\":49,\"y\":2,\"d\":\"Right Radius (distally and laterally)\"},{\"n\":24,\"x\":13,\"y\":14,\"d\":\"Left Ulna (olecranon posteriorly)\"},{\"n\":24,\"x\":39,\"y\":15,\"d\":\"Right Ulna (olecranon posteriorly)\"},{\"n\":25,\"x\":14,\"y\":2,\"d\":\"Left Ulna (distally and laterally)\"},{\"n\":25,\"x\":37,\"y\":2,\"d\":\"Right Ulna (distally and laterally)\"},{\"n\":26,\"x\":8,\"y\":2,\"d\":\"Left Distal radioulnar joint (dorsally)\"},{\"n\":26,\"x\":43,\"y\":2,\"d\":\"Right Distal radioulnar joint (dorsally)\"},{\"n\":27,\"x\":7,\"y\":-6,\"d\":\"Left Third metacarpal bone (dorsal face)\"},{\"n\":27,\"x\":45,\"y\":-6,\"d\":\"Right Third metacarpal bone (dorsal face)\"},{\"n\":28,\"x\":-16,\"y\":3,\"d\":\"Left Great trochanter\"},{\"n\":28,\"x\":-37,\"y\":3,\"d\":\"Right Great trochanter\"},{\"n\":29,\"x\":16,\"y\":-10,\"d\":\"Left Lateral face of the thigh\"},{\"n\":29,\"x\":36,\"y\":-10,\"d\":\"Right Lateral face of the thigh\"},{\"n\":30,\"x\":-23,\"y\":-23,\"d\":\"Left Anterior face of the patella\"},{\"n\":30,\"x\":-30,\"y\":-23,\"d\":\"Right Anterior face of the patella\"},{\"n\":31,\"x\":-22,\"y\":-32,\"d\":\"Left Anterior border of the tibia under the tibial tuberosity\"},{\"n\":31,\"x\":-31,\"y\":-32,\"d\":\"Right Anterior border of the tibia under the tibial tuberosity\"},{\"n\":32,\"x\":-22,\"y\":-48,\"d\":\"Left Lateral malleolus\"},{\"n\":32,\"x\":-31,\"y\":-48,\"d\":\"Right Lateral malleolus\"},{\"n\":34,\"x\":-22,\"y\":-60,\"d\":\"Left Third metatarsal bone (dorsal face)\"},{\"n\":34,\"x\":-31,\"y\":-60,\"d\":\"Right Third metatarsal bone (dorsal face)\"}]}";

        String jsonString;
        if (setup == null || setup.getVersion() > 1) {
            jsonString = newLocations;
        }
        else {
            jsonString = oldLocations;
        }

        MyLog.d(TAG, "jsonString: " + jsonString);

        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONArray jCoords = jsonObject.getJSONArray("coords");

            targetSnaps = new Snap[jCoords.length()];

            for (int i = 0; i < jCoords.length(); i++){

                JSONObject jCurrentCoord = jCoords.getJSONObject(i);

                Float x;
                try {
                    x = BigDecimal.valueOf(jCurrentCoord.getDouble("x")).floatValue();
                }
                catch (JSONException e){
                    x = null;
                }
//                MyLog.d("JSON", "x: " + x);

                Float y;
                try {
                    y = BigDecimal.valueOf(jCurrentCoord.getDouble("y")).floatValue();
                }
                catch (JSONException e){
                    y = null;
                }
//                MyLog.d("JSON", "y: " + y);

                Float r = 0F;
//                Float r;
//                try {
//                    r = BigDecimal.valueOf(jCurrentCoord.getDouble("r")).floatValue();
//                }
//                catch (JSONException e){
//                    r = null;
//                }
//                MyLog.d("JSON", "r: " + r);

                String description;
                try {
                    description = jCurrentCoord.getString("d");
                }
                catch (JSONException e) {
                    description = "";
                }

//                MyLog.d("JSON", "Add Snap " + i + ": x = " + x + " | y = " + y + " | r = " + r);

                targetSnaps[i] = new Snap(context, x, y, r, description);
                Float[] coords = calculateTargetBounds(x, y, parentSize, childSize, 1);
                RelativeLayout.LayoutParams snapParams = new RelativeLayout.LayoutParams(childSize, childSize);
                snapParams.setMargins(coords[0].intValue(), coords[1].intValue(), coords[2].intValue(), coords[3].intValue());
                targetSnaps[i].setLayoutParams(snapParams);
                targetSnaps[i].setGravity(Gravity.CENTER);
                targetSnaps[i].setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                targetSnaps[i].setId(i);

                parent.addView(targetSnaps[i]);

                targetSnaps[i].setOnDragListener(this);
                targetSnaps[i].setOutlineProvider(mOutlineProviderCircle);
                targetSnaps[i].setClipToOutline(true);
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "addRelativeLayout | JSONException: %s", e);
        }

        dragViewContainer = new LinearLayout(this);
        LinearLayout.LayoutParams dragViewContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, childSize * 3 / 2);
        dragViewContainerParams.setMargins(16,0,16,16);
        dragViewContainer.setLayoutParams(dragViewContainerParams);
        dragViewContainer.setOrientation(LinearLayout.HORIZONTAL);
        dragViewContainer.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
        dragViewContainer.setGravity(Gravity.CENTER);
        dragViewParent.addView(dragViewContainer);
        dragViewContainer.setOnDragListener(this);

        floatingContainer = new LinearLayout(this);
        LinearLayout.LayoutParams floatingContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, childSize * 3 / 2);
        floatingContainerParams.setMargins(16,0,16,16);
        floatingContainer.setLayoutParams(floatingContainerParams);
        floatingContainer.setOrientation(LinearLayout.HORIZONTAL);
        floatingContainer.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
        floatingContainer.setGravity(Gravity.CENTER);
        floatingParent.addView(floatingContainer);
        floatingContainer.setOnDragListener(this);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, childSize * 3 / 2);
        deleteParams.setMargins(16,16,16,16);
        deleteContainer.setLayoutParams(deleteParams);
    }

    /**
     * Creates and executes a request to get a setup with the provided setup ID.
     *
     * @param setupId ID of the requested setup
     */
    private void getSetupById(final int setupId) {
        String tag_string_req = "get_setup";

        if (!downloadingAllPdfs) {
            pDialog.setMessage(getString(R.string.getting_selected_setup_ellipsis));
            showDialog();
        }

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/");

        final StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    jsonSetup = response;
                    setup = Utilities.parseJsonSetup(context, jsonSetup, jsonTypeInfoList, jsonParameterInfoList);
                    oldSetup = Utilities.parseJsonSetup(context, jsonSetup, jsonTypeInfoList, jsonParameterInfoList);

                    if (setup != null){
                        setupEditor.putString(Constants.API_SETUPS_ + setup.getId(), response).apply();
                    }

                    addRelativeLayout();

                    prepareSetup();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

            if (!downloadingAllPdfs) {
                hideDialog();
                Utilities.displayVolleyError(context, e);
            }
            else {
                requestErrors++;
                requestErrorIds.add(setupId);

                downloadPdfIndex++;
                downloadAllPdfFiles();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    /**
     * Processes the setup and prepares to display it.
     */
    private void prepareSetup() {
        if (setup != null){
            setTitle(setup.getName());
            if (setup.isLocked() || setup.isObsolete()){
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setIcon(R.drawable.ic_lock_light);

                deleteContainer.setVisibility(View.GONE);
                dragViewParent.setVisibility(View.GONE);
                dragViewContainerTextView.setVisibility(View.GONE);

                invalidateOptionsMenu();
            }
            else {
                deleteContainer.setVisibility(View.VISIBLE);
                dragViewParent.setVisibility(View.VISIBLE);
                dragViewContainerTextView.setVisibility(View.VISIBLE);

                invalidateOptionsMenu();
            }

            int dragViewId = 0;

            for (Instrument instrument : setup.getInstrumentArrayList()){
                instrument.setDragViewId(dragViewId);
                dragViewId++;

                boolean boolX = false;
                boolean boolY = false;

                for (Parameter parameter : instrument.getParameterArrayList()){
                    switch (parameter.getId()){
                        case 1:
                            instrument.setLocationX(parameter.getValue());
                            boolX = true;
//                            MyLog.d(TAG, "addDragViewFromInstrument | Instrument ID: " + instrument.getId() + ", X location set: " + instrument.getLocationX());
                            break;

                        case 2:
                            instrument.setLocationY(parameter.getValue());
                            boolY = true;
//                            MyLog.d(TAG, "addDragViewFromInstrument | Instrument ID: " + instrument.getId() + ", Y location set: " + instrument.getLocationY());
                            break;

                        case 3:
                            instrument.setRotation(parameter.getValue());
                            break;
                    }
                }

                instrument.setLocatable(boolX && boolY);
                instrument.setPlaced(true);
            }

            addDragViewsFromSetup();
        }

        if (!downloadingAllPdfs) {
            hideDialog();
        }
        else {
            new Handler(Looper.getMainLooper()).postDelayed(this::createPdfFile, 100);
        }
    }

    /**
     * Loops through all instruments in the setup to add them as a drag view to the grid.
     */
    private void addDragViewsFromSetup() {
        for (Instrument instrument : setup.getInstrumentArrayList()){
            addDragViewFromInstrument(instrument);
        }
    }

    /**
     * Adds an instrument as a drag view to the grid.
     *
     * @param instrument to be added to the grid
     */
    private void addDragViewFromInstrument(Instrument instrument) {
//        MyLog.d(TAG, "addDragViewFromInstrument | Instrument ID: " + instrument.getId());

        imageViewArrayList.add(new ImageView(context));
        imageViewArrayList.get(imageViewArrayList.size()-1).setId(instrument.getDragViewId());
        imageViewArrayList.get(imageViewArrayList.size()-1).setBackground(getInstrumentIcon(instrument));

        if (instrument.isLocatable()){
            boolean matched = false;

            for (Snap target : targetSnaps){
                if (instrument.getLocationX() != null && instrument.getLocationY() != null)
                {
//                    MyLog.d(TAG, "Instrument: X = " + instrument.getLocationX() + ", Y = " + instrument.getLocationY());

                    if (instrument.getLocationX().equals(target.getLocationX()) && instrument.getLocationY().equals(target.getLocationY()))
                    {
//                        MyLog.d(TAG, "Target: X = " + target.getLocationX() + ", Y = " + target.getLocationY());

                        imageViewArrayList.get(imageViewArrayList.size()-1).setLayoutParams(smallDragViewParams);
                        if (instrument.getRotation() != null) {
                            imageViewArrayList.get(imageViewArrayList.size()-1).setRotation(instrument.getRotation());
                        }
                        target.addView(imageViewArrayList.get(imageViewArrayList.size()-1));
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched)
            {
                imageViewArrayList.get(imageViewArrayList.size()-1).setLayoutParams(bigDragViewParams);
                instrument.setLocationX(null);
                instrument.setLocationY(null);
                instrument.setRotation(0f);
                instrument.setPlaced(false);
                imageViewArrayList.get(imageViewArrayList.size()-1).setRotation(0);
                dragViewContainer.addView(imageViewArrayList.get(imageViewArrayList.size()-1));
            }
        }
        else {
            if (instrument.isPlaced() || !instrument.isLocatable())
            {
                imageViewArrayList.get(imageViewArrayList.size()-1).setLayoutParams(bigDragViewParams);
                instrument.setLocationX(null);
                instrument.setLocationY(null);
                instrument.setRotation(0f);
                instrument.setPlaced(true);
                imageViewArrayList.get(imageViewArrayList.size()-1).setRotation(0);
                floatingContainer.addView(imageViewArrayList.get(imageViewArrayList.size()-1));
            }
            else {
                imageViewArrayList.get(imageViewArrayList.size()-1).setLayoutParams(bigDragViewParams);
                instrument.setLocationX(null);
                instrument.setLocationY(null);
                instrument.setRotation(0f);
                imageViewArrayList.get(imageViewArrayList.size()-1).setRotation(0);
                dragViewContainer.addView(imageViewArrayList.get(imageViewArrayList.size()-1));
            }
        }

        setClickListeners(imageViewArrayList.get(imageViewArrayList.size()-1));
    }

    @Override
    public boolean onLongClick(View v) {

        if (setup.isLocked() || setup.isObsolete()){
            return false;
        }

        // the instrument has been touched
        // create clip data holding data of the type MIMETYPE_TEXT_PLAIN
        ClipData clipData = ClipData.newPlainText("", "");

        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            /*start the drag - contains the data to be dragged,
            metadata for this data and callback for drawing shadow*/
        v.startDrag(clipData, shadowBuilder, v, 0);
        // we're dragging the shadow so make the view invisible
        v.setVisibility(View.INVISIBLE);
        return true;
    }

    @Override
    public boolean onDrag(View receivingLayoutView, DragEvent dragEvent) {

        if (setup.isLocked() || setup.isObsolete()){
            return false;
        }

        View draggedImageView = (View) dragEvent.getLocalState();

        ViewGroup draggedImageViewParentLayout = (ViewGroup) draggedImageView.getParent();
        LinearLayout receivingLinearLayout = (LinearLayout) receivingLayoutView;

        // Handles each of the expected events
        switch (dragEvent.getAction()) {

            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                // returns true to indicate that the View can accept the dragged data.
                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return dragEvent.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

            case DragEvent.ACTION_DRAG_ENTERED:
                // the drag shadow has entered the bounding box

                if (receivingLayoutView == deleteContainer){
                    // the receiving layout is the delete container

                    return true;
                }
                else if (receivingLayoutView == floatingContainer){
                    // the receiving layout is the floating container

                    Instrument instrument = setup.getInstrumentArrayList().get(imageViewArrayList.indexOf(draggedImageView));
                    if (instrument.isLocatable()){
                        // the instrument is locatable

                        receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_red));
                        return true;
                    }
                    return true;
                }
                else if (receivingLayoutView == dragViewContainer){
                    // the receiving layout is the drag view container

                    Instrument instrument = setup.getInstrumentArrayList().get(imageViewArrayList.indexOf(draggedImageView));
                    if (!instrument.isLocatable()){
                        // the instrument is not locatable

                        receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_red));
                        return true;
                    }
                    return true;
                }
                else {
                    // the receiving layout is a snap target

                    Instrument instrument = setup.getInstrumentArrayList().get(imageViewArrayList.indexOf(draggedImageView));

//                    MyLog.d(TAG, "DROP_DEBUG | Receiving childs: " + receivingLinearLayout.getChildCount() + ", Locatable: " + instrument.isLocatable());

                    if (receivingLinearLayout.getChildCount() > 0 || !instrument.isLocatable()) {
                        receivingLayoutView.setBackgroundColor(getResources().getColor(R.color.transparant_red));
                        Float[] coords = calculateTargetBounds(((Snap) receivingLayoutView).getLocationX(), ((Snap) receivingLayoutView).getLocationY(), parentSize, childSize, 3);
                        RelativeLayout.LayoutParams snapParams = new RelativeLayout.LayoutParams(childSize * 3, childSize * 3);
                        snapParams.setMargins(coords[0].intValue(), coords[1].intValue(), coords[2].intValue(), coords[3].intValue());
                        receivingLayoutView.setLayoutParams(snapParams);
                    }
                    else {
                        receivingLayoutView.setBackgroundColor(getResources().getColor(R.color.transparant_green));
                        Float[] coords = calculateTargetBounds(((Snap) receivingLayoutView).getLocationX(), ((Snap) receivingLayoutView).getLocationY(), parentSize, childSize, 3);
                        RelativeLayout.LayoutParams snapParams = new RelativeLayout.LayoutParams(childSize * 3, childSize * 3);
                        snapParams.setMargins(coords[0].intValue(), coords[1].intValue(), coords[2].intValue(), coords[3].intValue());
                        receivingLayoutView.setLayoutParams(snapParams);
                    }
                }
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                /*triggered after ACTION_DRAG_ENTERED
                stops after ACTION_DRAG_EXITED*/
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                // the drag shadow has left the bounding box

                if (receivingLayoutView == deleteContainer){
                    // the receiving layout is the delete container

                    return true;
                }
                else if (receivingLayoutView == floatingContainer){
                    // the receiving layout is the floating container

                    receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    return true;
                }
                else if (receivingLayoutView == dragViewContainer){
                    // the receiving layout is the drag view container

                    receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    return true;
                }
                else {
                    // the receiving layout is a snap target

                    receivingLayoutView.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    Float[] coords = calculateTargetBounds(((Snap) receivingLayoutView).getLocationX(), ((Snap) receivingLayoutView).getLocationY(), parentSize, childSize, 1);
                    RelativeLayout.LayoutParams snapParams = new RelativeLayout.LayoutParams(childSize, childSize);
                    snapParams.setMargins(coords[0].intValue(), coords[1].intValue(), coords[2].intValue(), coords[3].intValue());
                    receivingLayoutView.setLayoutParams(snapParams);
                }
                return true;

            case DragEvent.ACTION_DROP:
                /* the listener receives this action type when
                drag shadow released over the target view
                the action only sent here if ACTION_DRAG_STARTED returned true
                return true if successfully handled the drop else false*/
//                MyLog.d(TAG, "onDrag | Receiving ID: " + receivingLayoutView.getId());

                Instrument instrument = setup.getInstrumentArrayList().get(imageViewArrayList.indexOf(draggedImageView));

                if (receivingLayoutView == deleteContainer){
                    // the instrument is placed in the delete container, the instrument is removed

                    draggedImageViewParentLayout.removeView(draggedImageView);
                    setup.getInstrumentArrayList().remove(instrument);
                    imageViewArrayList.remove(draggedImageView);
                    return true;
                }

                if (receivingLayoutView != dragViewContainer && receivingLayoutView != floatingContainer){
                    // the instrument is placed in a snap, remove the green or red marker indicating a (in)valid drop

                    receivingLayoutView.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    Float[] coords = calculateTargetBounds(((Snap) receivingLayoutView).getLocationX(), ((Snap) receivingLayoutView).getLocationY(), parentSize, childSize, 1);
                    RelativeLayout.LayoutParams snapParams = new RelativeLayout.LayoutParams(childSize, childSize);
                    snapParams.setMargins(coords[0].intValue(), coords[1].intValue(), coords[2].intValue(), coords[3].intValue());
                    receivingLayoutView.setLayoutParams(snapParams);
                }

                if (((LinearLayout) receivingLayoutView).getChildCount() > 0 && receivingLinearLayout != dragViewContainer && receivingLayoutView != floatingContainer){
                    // the instrument is placed in a snap target, but the snap target already has a child, so the drop is invalid and false is returned

                    return false;
                }

                if (instrument.isLocatable() && receivingLayoutView == floatingContainer){
                    // the instrument is placed in the floating container, but it is locatable, so the drop is invalid and false is returned

                    receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    return false;
                }

                if (!instrument.isLocatable() && receivingLayoutView == dragViewContainer){
                    // the instrument is placed in the drag view container, but it is not locatable, so the drop is invalid and false is returned

                    receivingLinearLayout.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
                    return false;
                }

                if (((LinearLayout) receivingLayoutView).getChildCount() == 0 && receivingLinearLayout != dragViewContainer && receivingLayoutView != floatingContainer && instrument.isLocatable()){
                    // the instrument is placed in a snap target without children, so the drop is valid, it is removed from the source container/snap and placed in the new snap

                    draggedImageViewParentLayout.removeView(draggedImageView);
                    receivingLinearLayout.addView(draggedImageView);
                    draggedImageView.setVisibility(View.VISIBLE);

                    draggedImageView.setLayoutParams(smallDragViewParams);
                    Snap snap = (Snap) receivingLinearLayout;
                    instrument.setLocationX(snap.getLocationX());
                    instrument.setLocationY(snap.getLocationY());
                    instrument.setRotation(snap.getDefaultRotation());
                    draggedImageView.setRotation(snap.getDefaultRotation());

                    instrument.setPlaced(true);

                    for (Parameter parameter : instrument.getParameterArrayList())
                    {
                        switch (parameter.getId()){
                            case 1:
                                parameter.setValue(instrument.getLocationX());
                                break;
                            case 2:
                                parameter.setValue(instrument.getLocationY());
                                break;
                            case 3:
                                parameter.setValue(instrument.getRotation());
                                break;
                        }
                    }

                    instrument.setName(snap.getDescription());
                    Utilities.displayToast(context, snap.getDescription());

                    return true;
                }

                if (!instrument.isLocatable() && receivingLayoutView == floatingContainer){
                    // the instrument is placed in the floating container and it is not locatable, so the drop is valid, it is removed from the source container and placed in the floating container

                    draggedImageViewParentLayout.removeView(draggedImageView);
                    receivingLinearLayout.addView(draggedImageView);
                    draggedImageView.setVisibility(View.VISIBLE);

                    draggedImageView.setLayoutParams(bigDragViewParams);
                    instrument.setLocationX(null);
                    instrument.setLocationY(null);
                    instrument.setRotation(null);
                    draggedImageView.setRotation(0);

                    instrument.setPlaced(true);

                    for (Parameter parameter : instrument.getParameterArrayList())
                    {
                        switch (parameter.getId()){
                            case 1:
                                parameter.setValue(instrument.getLocationX());
                                break;
                            case 2:
                                parameter.setValue(instrument.getLocationY());
                                break;
                            case 3:
                                parameter.setValue(instrument.getRotation());
                                break;
                        }
                    }
                    return true;
                }

                if (receivingLayoutView == dragViewContainer){
                    // the instrument is placed in the drag view container, so the drop is valid, it is removed from the source container/snap and placed in the drag view container

                    draggedImageViewParentLayout.removeView(draggedImageView);
                    receivingLinearLayout.addView(draggedImageView);
                    draggedImageView.setVisibility(View.VISIBLE);

                    draggedImageView.setLayoutParams(bigDragViewParams);
                    instrument.setLocationX(null);
                    instrument.setLocationY(null);
                    instrument.setRotation(null);
                    draggedImageView.setRotation(0);

                    instrument.setPlaced(false);

                    for (Parameter parameter : instrument.getParameterArrayList())
                    {
                        switch (parameter.getId()){
                            case 1:
                                parameter.setValue(instrument.getLocationX());
                                break;
                            case 2:
                                parameter.setValue(instrument.getLocationY());
                                break;
                            case 3:
                                parameter.setValue(instrument.getRotation());
                                break;
                        }
                    }

                    instrument.setName(instrument.getDescription());

                    return true;
                }

                return false;

            case DragEvent.ACTION_DRAG_ENDED:
                if (!dragEvent.getResult()) {
                    // if the drop was not successful, set the instrument to visible
                    draggedImageView.setVisibility(View.VISIBLE);
                }
                return true;

            // An unknown action type was received.
            default:

                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {

        if (setup != null) {
            if (setup.isLocked() || setup.isObsolete()){
                finish();
            }
            else if (createListOfChangedInstruments()){
                SimpleDialog.build()
                        .title(R.string.save_changes)
                        .msg(R.string.quit_save_changes)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .neut(R.string.cancel)
                        .cancelable(false)
                        .show(this, SAVE_CHANGES_DIALOG);
            } else if (createListOfDeletedInstruments()){
                SimpleDialog.build()
                        .title(R.string.save_changes)
                        .msg(R.string.quit_save_changes)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .neut(R.string.cancel)
                        .cancelable(false)
                        .show(this, SAVE_CHANGES_DIALOG);
            }
            else {
                SimpleDialog.build()
                        .title(R.string.quit)
                        .msg(R.string.quit_setup_screen)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .cancelable(false)
                        .show(this, QUIT_DIALOG);
            }
        }
        else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_drag_and_drop, menu);

        if (setup != null){
            if (setup.isLocked() || setup.isObsolete()){
                menu.findItem(R.id.action_save_setup).setVisible(false);
                menu.findItem(R.id.action_add_instrument).setVisible(false);
                menu.findItem(R.id.action_lock_setup).setVisible(false);
            }
            else {
                menu.findItem(R.id.action_save_setup).setVisible(true);
                menu.findItem(R.id.action_add_instrument).setVisible(true);
                menu.findItem(R.id.action_lock_setup).setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null){
            int itemId = item.getItemId();
            if (itemId == R.id.action_setup_info) {
                Intent intent1 = new Intent(context, SetupInfoActivity.class);
                if (setup != null) {
                    if (setup.getId() < 0) {
                        Utilities.displayToast(context, "Can't open a setup that doesn't exist (yet).");
                    } else {
                        intent1.putExtra("SETUP_ID", setup.getId());
                        startActivity(intent1);
                    }
                }
            } else if (itemId == R.id.action_save_setup) {
                if (createListOfChangedInstruments()) {
                    uploadInstrumentChanges(false);
                } else if (createListOfDeletedInstruments()) {
                    deleteInstruments(false);
                }
            } else if (itemId == R.id.action_add_instrument) {
                Intent intent2 = new Intent(context, SetupAddInstrumentActivity.class);
                if (setup != null) {
                    if (setup.getId() < 0) {
                        Utilities.displayToast(context, getString(R.string.cant_add_instrument_without_setup_id));
                    } else {
                        intent2.putExtra("SETUP_ID", setup.getId());
                        startActivityForResult(intent2, ADD_INSTRUMENT);
                    }
                }
            } else if (itemId == R.id.action_lock_setup) {
                SimpleDialog.build()
                        .title("Lock setup")
                        .msg("Do you want to lock this setup? This operation can't be undone!")
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .cancelable(true)
                        .show(this, LOCK_SETUP_DIALOG);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case ADD_INSTRUMENT:
                if (resultCode == RESULT_OK){
                    Instrument newInstrument = data.getParcelableExtra("NEW_INSTRUMENT");

                    if (newInstrument != null){

                        for (Parameter parameter : newInstrument.getParameterArrayList())
                        {
                            switch (parameter.getId())
                            {
                                case 1:
                                    newInstrument.setLocationX(parameter.getValue());
                                    break;
                                case 2:
                                    newInstrument.setLocationY(parameter.getValue());
                                    break;
                                case 3:
                                    newInstrument.setRotation(parameter.getValue());
                                    break;
                            }
                        }

                        setup.getInstrumentArrayList().add(newInstrument);

                        recreate();
                    }
                }
                break;

            case EDIT_INSTRUMENT:
                if (resultCode == RESULT_OK){
                    Instrument editedInstrument = data.getParcelableExtra("EDITED_INSTRUMENT");

                    if (editedInstrument != null){
                        int index = -1;

                        for (Parameter parameter : editedInstrument.getParameterArrayList()){

                            switch (parameter.getId()){
                                case 1:
                                    editedInstrument.setLocationX(parameter.getValue());
                                    break;
                                case 2:
                                    editedInstrument.setLocationY(parameter.getValue());
                                    break;
                                case 3:
                                    editedInstrument.setRotation(parameter.getValue());
                                    break;
                            }
                        }

                        for (Instrument instrument : setup.getInstrumentArrayList()){
                            if (instrument.getId() == editedInstrument.getId()){
                                index = setup.getInstrumentArrayList().indexOf(instrument);
                            }
                        }

                        if (index != -1){
                            setup.getInstrumentArrayList().remove(index);
                            setup.getInstrumentArrayList().add(index, editedInstrument);
                        }

                        recreate();
                    }
                }
                break;
        }
    }

    /**
     * The view outline provider to display the linear layout drag targets as circles
     * instead of squares.
     */
    private class CircleOutlineProvider extends ViewOutlineProvider {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    }

    /**
     * Calculates the height of the available screen space.
     *
     * @return height of the available screen space
     */
    private int getHeightStatusActionSoft() {
        int status = 0;
        int action = 0;
        int soft = 0;

        // Status Bar
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            status = getResources().getDimensionPixelSize(resourceId);
        }

        // Action Bar
        final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        action = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        // Soft Keys
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            DisplayMetrics metrics = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(metrics);
//            int usableHeight = metrics.heightPixels;
//            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
//            int realHeight = metrics.heightPixels;
//            if (realHeight > usableHeight){
//                soft = realHeight - usableHeight;
//            }
//        }

        int value = status + action + soft;

        MyLog.d(TAG, "Height Status: " + status + ", Height Action: " + action + ", Height Soft: " + soft + ", Height Total: " + value);

        return value;
    }

    /**
     * Calculates the bounds for the snap target views.
     *
     * @param x horizontal coordinate for the center of the view
     * @param y vertical coordinate for the center of the view
     * @param parent size of the side of the total grid
     * @param child size of side of one cell of the grid
     * @param magnify factor to change the size of the view
     * @return an array of floats representing the bounds of the target
     */
    private Float[] calculateTargetBounds(Float x, Float y, int parent, int child, int magnify) {
//        MyLog.d("calculateTargetBounds", "X: " + x + " | Y: " + y + " | Parent: " + parent + " | Child: " + child);

        x /= 8;
        y /= 8;

        Float left, top, right, bottom;

        left = (parent / 2F) + (x * child);
        top = (parent / 2F) - (y * child);

        left = left  - (child / 2);
        top = top - (child / 2);

        right = parent - left - child;
        bottom = parent - top - child;

//        MyLog.d("calculateTargetBounds", "NORMAL: Left: " + left + " | Top: " + top + " | Right: " + right + " | Bottom: " + bottom);

        left = left - child * (magnify * 5 - 5) / 10;
        top = top - child * (magnify * 5 - 5) / 10;
        right = right - child * (magnify * 5 - 5) / 10;
        bottom = bottom - child * (magnify * 5 - 5) / 10;

//        MyLog.d("calculateTargetBounds", "ZOOMED: Left: " + left + " | Top: " + top + " | Right: " + right + " | Bottom: " + bottom);

        if (left < 0){
            right = right + left;
            left = 0f;
        }
        if (top < 0){
            bottom = bottom + top;
            top = 0f;
        }
        if (right < 0){
            left = left + right;
            right  = 0f;
        }
        if (bottom < 0){
            top = top + bottom;
            bottom  = 0f;
        }

//        MyLog.d("calculateTargetBounds", "CORRECTED: Left: " + left + " | Top: " + top + " | Right: " + right + " | Bottom: " + bottom);

        return new Float[] {left, top, right, bottom};
    }

    /**
     * Gets the appropriate icon for the given instrument.
     *
     * @param instrument for which the icon is requested
     * @return the drawable of the icon
     */
    private Drawable getInstrumentIcon(Instrument instrument) {
        for (Parameter parameter : instrument.getParameterArrayList())
        {
            if (parameter.getId() == Constants.SETUP_PRM_APP_INSTRUMENT_ICON)
            {
                if (parameter.getValue() == null){
                    MyLog.d(TAG, "NO_ICON_PARAMETER");
                    return ContextCompat.getDrawable(context, R.drawable.icon_default);
                }

//                MyLog.d(TAG, "Icon parameter value: " + Float.floatToIntBits(parameter.getValue()));
                switch (Float.floatToIntBits(parameter.getValue()))
                {
                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_IMU_ICON:
//                        MyLog.d(TAG, "IMU_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_IMU_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_imu);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_GPS_ICON:
//                        MyLog.d(TAG, "GPS_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_GPS_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_gps);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_DISTANCE_SENSOR_ICON:
//                        MyLog.d(TAG, "DISTANCE_SENSOR_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_DISTANCE_SENSOR_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_distance);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_HEART_RATE_ICON:
//                        MyLog.d(TAG, "HEART_RATE_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_HEART_RATE_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_heart);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_ICON:
//                        MyLog.d(TAG, "JOYSTICK_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_joystick);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_OAS_ICON:
//                        MyLog.d(TAG, "OAS_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_OAS_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_oas);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_RTC_ICON:
//                        MyLog.d(TAG, "RTC_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_RTC_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_rtc);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_ANDROID_DEVICE_ICON:
//                        MyLog.d(TAG, "ANDROID_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_ANDROID_DEVICE_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_android);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_PROFILE_ICON:
//                        MyLog.d(TAG, "JOYSTICK_PROFILE_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_PROFILE_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_joystick_profile);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_AAMS_ICON:
//                        MyLog.d(TAG, "AAMS_ICON: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_AAMS_ICON);
                        return ContextCompat.getDrawable(context, R.drawable.icon_aams);

                    case (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_NONE:
                    default:
                        MyLog.d(TAG, "ICON_NONE: " + (int) Constants.SETUP_PRM_APP_INSTRUMENT_ICON_option_NONE);
                        return ContextCompat.getDrawable(context, R.drawable.icon_default);

                }
            }
        }
        MyLog.d(TAG, "NO_MATCH_ICON");
        return ContextCompat.getDrawable(context, R.drawable.icon_default);
    }

    /**
     * Sets the on click and on long click listeners for the drag views.
     *
     * @param v drag view
     */
    private void setClickListeners(View v) {
        v.setOnLongClickListener(this);

        v.setOnClickListener(v1 -> {

            final Instrument instrument = setup.getInstrumentArrayList().get(imageViewArrayList.indexOf(v1));

            Intent intent = new Intent(context, SetupEditInstrumentActivity.class);
            if (instrument != null){
                intent.putExtra("INSTRUMENT", instrument);
                intent.putExtra("SETUP", setup);
                startActivityForResult(intent, EDIT_INSTRUMENT);
            }
        });
    }

    /**
     * Creates a list of instruments that are changed in the new setup.
     *
     * @return true if there are one or more changed instruments
     *         false if there are no changed instruments
     */
    private boolean createListOfChangedInstruments(){
        saveJsonArrayList.clear();
        if (oldSetup != null){
            // Iterate through all instruments in the new setup
            for (Instrument newInstrument : setup.getInstrumentArrayList()){

                MyLog.d(TAG, "createListOfChangedInstruments: new instrument " + newInstrument.getId());

                if (compareNewWithOldInstruments(newInstrument, oldSetup)){

                    MyLog.d(TAG, "createListOfChangedInstruments: instrument " + newInstrument.getId() + " changed, adding json to queue");

                    saveIdArrayList.add(newInstrument.getId());
                    saveJsonArrayList.add(Utilities.serializeInstrument(context, newInstrument));
                }
            }
        }
        else {
            // oldSetup == null
            // TODO: add exception when old setup == null
            MyLog.e(TAG, "Old setup is null");
        }

        return saveJsonArrayList.size() > 0;
    }

    /**
     * Compares an instrument to all instruments in the old setup to find a match and check for
     * changes to the instrument.
     *
     * @param newInstrument instrument to check for changes
     * @param oldSetup setup to compare the instrument to
     * @return true if there are changes to the instrument or if the instrument is newly added
     *         false if there are no changes to the instrument and the instrument is not newly added
     */
    private boolean compareNewWithOldInstruments(Instrument newInstrument, Setup oldSetup){

        boolean matchForNewInstrument = false;

        // Iterate through all instruments in the old setup
        for (Instrument oldInstrument : oldSetup.getInstrumentArrayList()){

//            MyLog.d(TAG, "compareNewWithOldInstruments: old instrument " + oldInstrument.getMeasurementId());

            // Check if the new and the old instrument have the same id
            if (newInstrument.getId() == oldInstrument.getId()){

//                MyLog.d(TAG, "checkSetupForChanges: instrument id's are equal");

                // check if the name of the instrument has changed
                if (!newInstrument.getName().equals(oldInstrument.getName())){
//                    MyLog.d(TAG, "compareNewWithOldInstruments: name different");
                    MyLog.d(TAG, "NEW: " + Utilities.serializeInstrument(context, newInstrument));
                    MyLog.d(TAG, "OLD: " + Utilities.serializeInstrument(context, oldInstrument));
                    return true;
                }

                // check if the name of the instrument has changed
                if (!newInstrument.getDescription().equals(oldInstrument.getDescription())){
//                    MyLog.d(TAG, "compareNewWithOldInstruments: description different");
                    MyLog.d(TAG, "NEW: " + Utilities.serializeInstrument(context, newInstrument));
                    MyLog.d(TAG, "OLD: " + Utilities.serializeInstrument(context, oldInstrument));
                    return true;
                }

                matchForNewInstrument = true;

                // Iterate through all parameters of the new instrument
                for (Parameter newParameter : newInstrument.getParameterArrayList()){

//                    MyLog.d(TAG, "compareNewWithOldInstruments: new instrument " + newInstrument.getMeasurementId() + " | new parameter " + newParameter.getMeasurementId());

                    // Iterate through all parameters of the old instrument
                    for (Parameter oldParameter : oldInstrument.getParameterArrayList()){

//                        MyLog.d(TAG, "compareNewWithOldInstruments: old instrument " + oldInstrument.getMeasurementId() + " | old parameter " + oldParameter.getMeasurementId());

                        // Check if the new and the old parameter have the same id
                        if (newParameter.getId() == oldParameter.getId()){

//                            MyLog.d(TAG, "compareNewWithOldInstruments: parameter id's are equal");

                            // Both parameters are not null
                            if (newParameter.getValue() != null && oldParameter.getValue() != null)
                            {
                                // If the values are not equal, update the parameter
                                if (!newParameter.getValue().equals(oldParameter.getValue())){
//                                    MyLog.d(TAG, "compareNewWithOldInstruments: values different");
                                    MyLog.d("NEW", Utilities.serializeInstrument(context, newInstrument));
                                    MyLog.d("OLD", Utilities.serializeInstrument(context, oldInstrument));
                                    return true;
                                }
                            }
                            // One or both parameters are null
                            else {
                                // Only one parameter is null
                                if (!(newParameter.getValue() == null && oldParameter.getValue() == null)){
//                                    MyLog.d(TAG, "compareNewWithOldInstruments: one parameter is null");
                                    MyLog.d("NEW", Utilities.serializeInstrument(context, newInstrument));
                                    MyLog.d("OLD", Utilities.serializeInstrument(context, oldInstrument));
                                    return true;
                                }
                            }

                            // break out the for loop because the equal parameter has been found
                            break;
                        }
                    }
                }
                // break out the for loop because the equal instrument has been found
                break;
            }

        }

        // New instrument was added, save parameters
        if (!matchForNewInstrument)
        {
//            MyLog.d(TAG, "compareNewWithOldInstruments: new instrument, no match found");
            return true;
        }

        MyLog.d(TAG, "compareNewWithOldInstruments: no changes found");
        return false;
    }

    /**
     * Uploads the changes to the instruments to the server, this method calls itself to iterate
     * through the list of changed instruments.
     *
     * @param finishActivity indicates whether or not to finish the activity after uploading
     *                       the changes
     */
    private void uploadInstrumentChanges(boolean finishActivity) {

        Bundle bundle = new Bundle();
        bundle.putBoolean("FINISH_ACTIVITY", finishActivity);

        if (saveIndex == saveIdArrayList.size()){

            hideDialog();

            if (saveIdArrayList.size() == 0){

                if (createListOfDeletedInstruments()){
                    deleteInstruments(finishActivity);
                }
                else {
                    oldSetup = Utilities.deepCopy(setup);

                    if (finishActivity){
                        finish();
                    }
                }
            }
            else {
                SimpleDialog.build()
                        .title(R.string.saving_failed)
                        .msg(R.string.couldnt_save_changes)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .extra(bundle)
                        .show(this, COULD_NOT_SAVE_DIALOG);
            }
        }
        else {
            String tag_string_req = "upload_para_value";

            pDialog.setMessage(getString(R.string.saving_changes_ellipsis));
            showDialog();

            MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/" + saveIdArrayList.get(saveIndex) + "/");
            MyLog.d("StringRequest", "Put body: " + (saveJsonArrayList.get(saveIndex) == null ? null : saveJsonArrayList.get(saveIndex).getBytes(StandardCharsets.UTF_8)));

            StringRequest strReq = new StringRequest(
                    Request.Method.PUT,
                    PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/" + saveIdArrayList.get(saveIndex) + "/",
                    response -> {
                        LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);
                        saveIdArrayList.remove(saveIndex);
                        saveJsonArrayList.remove(saveIndex);
                        uploadInstrumentChanges(finishActivity);
                    }, e -> {
                MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
                Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
                hideDialog();

                saveIndex++;
                uploadInstrumentChanges(finishActivity);
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return saveJsonArrayList.get(saveIndex) == null ? null : saveJsonArrayList.get(saveIndex).getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                    return headers;
                }
            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }

    /**
     * Creates a list of urls to delete the instruments that were removed from the current setup.
     *
     * @return true if there is at least one instrument that needs to be deleted
     *         false if there are no instruments to be deleted
     */
    private boolean createListOfDeletedInstruments(){
        deleteUrlArrayList.clear();
        if (oldSetup != null){
            for (Instrument oldInstrument : oldSetup.getInstrumentArrayList()){
                if (!searchInstrument(oldInstrument, setup)){
                    deleteUrlArrayList.add(PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/" + oldInstrument.getId() + "/");
                }
            }
        }
        return deleteUrlArrayList.size() > 0;
    }

    /**
     * Searches a specific instrument in a setup.
     *
     * @param searchedInstrument instrument to be searched
     * @param setup in which the instrument needs to be found
     * @return true if a match is found
     *         false if no match was found
     */
    private boolean searchInstrument(Instrument searchedInstrument, Setup setup){
        for (Instrument instrument : setup.getInstrumentArrayList()){
            if (searchedInstrument.getId() == instrument.getId()){
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the instruments on the server, this method calls itself to iterate through the list
     * of deleted instruments.
     *
     * @param finishActivity indicates whether or not to finish the activity after deleting
     *                       the instrument(s)
     */
    private void deleteInstruments(boolean finishActivity){

        Bundle bundle = new Bundle();
        bundle.putBoolean("FINISH_ACTIVITY", finishActivity);

        if (deleteIndex == deleteUrlArrayList.size()){

            hideDialog();

            if (deleteUrlArrayList.size() == 0){

                oldSetup = Utilities.deepCopy(setup);

                if (finishActivity){
                    finish();
                }

            }
            else {
                SimpleDialog.build()
                        .title(R.string.saving_failed)
                        .msg(R.string.couldnt_save_changes)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .extra(bundle)
                        .show(this, COULD_NOT_SAVE_DIALOG);
            }
        }
        else {
            String tag_string_req = "delete_instruments";

            pDialog.setMessage(getString(R.string.saving_changes_ellipsis));
            showDialog();

            MyLog.d("StringRequest", deleteUrlArrayList.get(deleteIndex));

            StringRequest strReq = new StringRequest(
                    Request.Method.DELETE,
                    deleteUrlArrayList.get(deleteIndex),
                    response -> {
                        LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);
                        deleteUrlArrayList.remove(deleteIndex);
                        deleteInstruments(finishActivity);
                    }, e -> {
                MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

                deleteIndex++;
                deleteInstruments(finishActivity);
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                    return headers;
                }
            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }

    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        switch (dialogTag){

            case COULD_NOT_SAVE_DIALOG:
                switch (which){
                    case BUTTON_POSITIVE:
                        saveIndex = 0;
                        deleteIndex = 0;
                        uploadInstrumentChanges(extras.getBoolean("FINISH_ACTIVITY"));
                        return true;
                    case BUTTON_NEGATIVE:
                        saveIndex = 0;
                        deleteIndex = 0;
                        saveIdArrayList.clear();
                        saveJsonArrayList.clear();
                        if (extras.getBoolean("FINISH_ACTIVITY")){
                            finish();
                        }
                        return true;
                }
                break;

            case SAVE_CHANGES_DIALOG:
                switch (which){
                    case BUTTON_POSITIVE:
                        uploadInstrumentChanges(true);
                        return true;
                    case BUTTON_NEGATIVE:
                        finish();
                        return true;
                }
                break;

            case QUIT_DIALOG:
                if (which == BUTTON_POSITIVE) {
                    finish();
                    return true;
                }
                break;

            case LOCK_SETUP_DIALOG:
                if (which == BUTTON_POSITIVE){
                    prepareLockedSetup();
                }
                break;

        }

        return false;
    }

    private void prepareLockedSetup(){
        try {

            JSONObject jData = new JSONObject();
            jData.put("name_en", setup.getName());
            jData.put("hw_identifier", setup.getHardwareIdentifier());
            jData.put("version", setup.getVersion());
            jData.put("locked", true);

            // TODO change later
            jData.put("setup_group_id", 1);

            // TODO change later (1 Uncategorized / 2 Development / 3 Production)
            jData.put("setup_category_id", 2);

            JSONObject jObj = new JSONObject();
            jObj.put("data", jData);

            putLockedSetup(jObj.toString(), setup.getId());
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    private void putLockedSetup(String submitString, int setupId){
        String tag_string_req = "lock_setup";

        pDialog.setMessage("Locking setup...");
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.PUT,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    hideDialog();
                    Utilities.displayToast(context, "Setup locked successfully");
                    finish();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }) {

            // replaces getParams with getBody
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            // replaces getParams with getBodyContentType
            @Override
            public byte[] getBody() {
                return submitString == null ? null : submitString.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void downloadAllPdfFiles() {
        clearSetup();
        if (downloadPdfIndex < setupIdArrayList.size()) {
            pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + setupIdArrayList.size() + ")");
            MyLog.d(TAG, "Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + setupIdArrayList.size() + ")");
            setupId = setupIdArrayList.get(downloadPdfIndex);
            if (setupId > 0) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getSetupById(setupId);
                    }
                }, 100);
            }
            else {
                downloadPdfIndex++;
                downloadAllPdfFiles();
            }
        }
        else {
            hideDialog();

            MyLog.d(TAG, "requestErrors: " + requestErrors);
            for (int i = 0; i < requestErrorIds.size(); i++) {
                MyLog.d(TAG, "requestError ID " + (i+1) + ": " + requestErrorIds.get(i));
            }
            MyLog.d(TAG, "pdfErrors: " + pdfErrors);
            for (int i = 0; i < pdfErrorIds.size(); i++) {
                MyLog.d(TAG, "pdfError ID " + (i+1) + ": " + pdfErrorIds.get(i));
            }

            runOnUiThread(() -> Utilities.displayToast(context, "Downloading PDF files completed (Request errors: " + requestErrors + " | PDF errors: " + pdfErrors + ")"));
        }
    }

    /**
     * Creates and outputs the PDF file.
     */
    private void createPdfFile() {

        // Enable Android asset loading
        PDFBoxResourceLoader.init(getApplicationContext());

        document = new PDDocument();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDRectangle mediaBox = page.getMediaBox();
        float marginY = 80;
        float marginX = 60;
        width = mediaBox.getWidth() - 2 * marginX;
        float startX = mediaBox.getLowerLeftX() + marginX;
        float endX = mediaBox.getUpperRightX() - marginX;
        startY = mediaBox.getUpperRightY() - marginY;
        endY = mediaBox.getLowerLeftY() + marginY;
        heightCounter = startY;
        currentXPosition = 0;

        float smallOffsetY = 8;
        float normalOffsetY = 20;
        wrapOffsetY = 2;

        Paint titleTextPaint = new TextPaint();
        float titleFontSize = 20;
        titleTextPaint.setTextSize(titleFontSize);
        titleTextPaint.setTypeface(Typeface.create("Helvetica", Typeface.BOLD));
        Paint.FontMetrics titleFontMetrics = titleTextPaint.getFontMetrics();
        float titleFontHeight = titleFontMetrics.descent - titleFontMetrics.ascent;

        Paint defaultTextPaint = new TextPaint();
        float defaultFontSize = 12;
        defaultTextPaint.setTextSize(defaultFontSize);
        defaultTextPaint.setTypeface(Typeface.create("Helvetica", Typeface.NORMAL));
        Paint.FontMetrics defaultFontMetrics = defaultTextPaint.getFontMetrics();
        float defaultFontHeight = defaultFontMetrics.descent - defaultFontMetrics.ascent;

        Paint smallTextPaint = new TextPaint();
        float smallFontSize = 10;
        smallTextPaint.setTextSize(smallFontSize);
        smallTextPaint.setTypeface(Typeface.create("Helvetica", Typeface.NORMAL));
        Paint.FontMetrics smallFontMetrics = smallTextPaint.getFontMetrics();
        float smallFontHeight = smallFontMetrics.descent - defaultFontMetrics.ascent;

        // Create font objects
        PDFont titleFont = PDType1Font.HELVETICA_BOLD;
        PDFont defaultFont = PDType1Font.HELVETICA;


        try {
            // Define a content stream for adding to the PDF
            contentStream = new PDPageContentStream(document, page);

            // Load in logo
            InputStream in = getAssets().open("nomade_logo_small.png");

            // Draw the logo
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            PDImageXObject xImage = LosslessFactory.createFromImage(document, bitmap);
            contentStream.drawImage(xImage, endX - 148, startY - 63, 148, 63);

            // Write title
            contentStream.beginText();

            addParagraph(startX, 0, true, titleFont, titleFontSize, titleFontHeight, setup.getName(), width - 148);

            // Write setup details
            addParagraph(startX + 5, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.id_colon) + setup.getId());
            addParagraph(startX + 5, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.hardware_identifier_colon) + setup.getHardwareIdentifier());
            addParagraph(startX + 5, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.version_colon) + setup.getVersion());
            addParagraph(startX + 5, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.locked_colon) + setup.isLocked());

            // Print setup image
            addParagraph(startX, normalOffsetY + 300 - titleFontHeight, titleFont, titleFontSize, titleFontHeight, " ");
            contentStream.endText();

            ImageView wheelchairImageView = findViewById(R.id.imageView);

            Bitmap wheelchairBitmap = getBitmapFromView(wheelchairImageView);
            PDImageXObject xImageWheelchair = LosslessFactory.createFromImage(document, wheelchairBitmap);
            contentStream.drawImage(xImageWheelchair, startX, heightCounter, 300, 300);

            Bitmap gridParentBitmap = getBitmapFromView(gridParent);
            PDImageXObject xImageGridParent = LosslessFactory.createFromImage(document, gridParentBitmap);
            contentStream.drawImage(xImageGridParent, startX, heightCounter, 300, 300);

            if (dragViewContainer.getChildCount() > 0) {
                Bitmap dragViewParentBitmap = getBitmapFromView(dragViewParent);
                PDImageXObject xImageDragViewParent = LosslessFactory.createFromImage(document, dragViewParentBitmap);
                contentStream.drawImage(xImageDragViewParent, startX, heightCounter - 20, 300, 30);
            }

            if (floatingContainer.getChildCount() > 0) {
                Bitmap floatingParentBitmap = getBitmapFromView(floatingParent);
                PDImageXObject xImageFloatingParent = LosslessFactory.createFromImage(document, floatingParentBitmap);
                contentStream.drawImage(xImageFloatingParent, startX, heightCounter + 270, 300, 30);
            }

            contentStream.beginText();
            addParagraph(startX, -defaultFontHeight, true, titleFont, titleFontSize, titleFontHeight, " ");

            // Write instruments with type and parameters
            Instrument instrument;
            Parameter parameter;

            for (int i = 0; i < setup.getInstrumentArrayList().size(); i++) {

                instrument = setup.getInstrumentArrayList().get(i);

                addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.instrument));
                addParagraph(startX + 5, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.name_colon) + instrument.getName());
                addParagraph(startX + 5, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.id_colon) + instrument.getId());
                if (!instrument.getDescription().equals("")) {
                    addParagraph(startX + 5, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.description_colon) + instrument.getDescription());
                }

                addParagraph(startX + 20, smallOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.type));
                addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.name_colon) + instrument.getType().getName());
                addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.id_colon) + instrument.getType().getId());
                addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.category_colon) + instrument.getType().getCategory());
                if (!instrument.getType().getDescription().equals("")) {
                    addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.description_colon) + instrument.getType().getDescription());
                }

                for (int j = 0; j < instrument.getParameterArrayList().size(); j++) {

                    parameter = instrument.getParameterArrayList().get(j);

                    addParagraph(startX + 20, smallOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.parameter));
                    addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.name_colon) + parameter.getName());
                    addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.id_colon) + parameter.getId());
                    addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.value_colon) + parameter.getValue());
                    if (!parameter.getValueDescription().equals("")) {
                        addParagraph(startX + 25, wrapOffsetY, defaultFont, smallFontSize, smallFontHeight, getString(R.string.value_description_colon) + parameter.getValueDescription());
                    }
                }
            }

            contentStream.endText();

            contentStream.close();

            // Adding page numbers to the whole document
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                String pageNumberString = (i + 1) + " / " + pageCount;
                float size = defaultFontSize * defaultFont.getStringWidth(pageNumberString) / 1000;
                page = document.getPage(i);
                contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                contentStream.beginText();
                contentStream.setFont(defaultFont, defaultFontSize);
                contentStream.newLineAtOffset(endX + marginX - endY + defaultFontHeight - size, endY - defaultFontHeight);
                contentStream.showText(pageNumberString);
                contentStream.endText();
                contentStream.close();
            }

            // Make sure that the content stream is closed:
            contentStream.close();

            OutputStream outputStream;

            String name = setup.getId() + "_" + setup.getName() + ".pdf";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MyLog.d(TAG, "MediaStore used");
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        document.save(outputStream);
                        outputStream.close();
                        document.close();
                    }
                }
            }

            downloadPdfIndex++;
            downloadAllPdfFiles();
        } catch (IOException e) {
            e.printStackTrace();

            pdfErrors++;
            pdfErrorIds.add(setupId);

            downloadPdfIndex++;
            downloadAllPdfFiles();
        }
    }

    private void clearSetup() {
        if (dragViewContainer != null && floatingContainer != null) {
            dragViewContainer.removeAllViews();
            floatingContainer.removeAllViews();
            for (Snap s : targetSnaps) {
                s.removeAllViews();
            }
        }
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     */
    private void addParagraph(float positionX, float offsetY, PDFont font, float fontSize, float fontHeight, String text) throws IOException {
        addParagraph(positionX, offsetY, false, font, fontSize, fontHeight, text, width);
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param setYToHeightCounter set Y location to height counter
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     */
    private void addParagraph(float positionX, float offsetY, boolean setYToHeightCounter, PDFont font, float fontSize, float fontHeight, String text) throws IOException {
        addParagraph(positionX, offsetY, setYToHeightCounter, font, fontSize, fontHeight, text, width);
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param setYToHeightCounter set Y location to height counter
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     * @param width available width
     */
    private void addParagraph(float positionX, float offsetY, boolean setYToHeightCounter, PDFont font, float fontSize, float fontHeight, String text, float width) throws IOException {
        List<String> lines = parseLines(text.replaceAll("\\p{Cntrl}", ""), width, font, fontSize);
        contentStream.setFont(font, fontSize);

        float neededHeight = lines.size() * (wrapOffsetY + fontHeight) + offsetY - wrapOffsetY;

        if (heightCounter - neededHeight < endY) {
            // Create new page
            contentStream.endText();
            contentStream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);

            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX, startY - fontHeight);
                    heightCounter = startY - fontHeight;
                    currentXPosition = positionX;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
        else if (setYToHeightCounter) {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX, heightCounter - offsetY - fontHeight);
                    heightCounter -= offsetY + fontHeight;
                    currentXPosition = positionX;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
        else {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX - currentXPosition, - offsetY - fontHeight);
                    heightCounter -= offsetY + fontHeight;
                    currentXPosition += positionX - currentXPosition;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
    }

    /**
     * Splits up the text depending on the available width, the font and the font size
     *
     * @param text string to split
     * @param width available width
     * @param font font for the text
     * @param fontSize font size for the text
     * @return a list of split strings
     */
    private static List<String> parseLines(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<String>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = fontSize * font.getStringWidth(subString) / 1000;
            if (size > width) {
                if (lastSpace < 0){
                    lastSpace = spaceIndex;
                }
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    /**
     * Shows the dialog when it's not already showing.
     */
    private void showDialog() {
        if (!isFinishing() && pDialog != null && !pDialog.isShowing())
            pDialog.show();
    }

    /**
     * Hides the dialog when it's showing.
     */
    private void hideDialog() {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        hideDialog();
        super.onDestroy();
    }
}
