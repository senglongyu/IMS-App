package jp.kshoji.stlviewer.activity;

import java.io.File;

import jp.kshoji.stlviewer.R;
import jp.kshoji.stlviewer.renderer.STLRenderer;
import jp.kshoji.stlviewer.util.Log;
import jp.kshoji.stlviewer.view.STLView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import java.util.Arrays;

/**
 * TODO "Reset view" → Button
 *
 * @author K.Shoji
 */
public class STLViewActivity extends Activity implements FileListDialog.OnFileListDialogListener {
	private STLView stlView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		final Context context = this;

		super.onCreate(savedInstanceState);

		PackageManager manager = getPackageManager();
		ApplicationInfo appInfo = null;
		try {
			appInfo = manager.getApplicationInfo(getPackageName(), 0);
			Log.setDebug((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE);
		} catch (NameNotFoundException e) {
			Log.d(e);
		}

		Intent intent = getIntent();
		Uri uri = null;
		if (intent.getData() != null) {
			uri = getIntent().getData();
			Log.i("Uri:" + uri);
		}
		setUpViews(uri);
	}

	public void updateTextView(String toThis) {

		TextView textView = (TextView) findViewById(R.id.textView);
		textView.setText(toThis);

		return;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (stlView != null) {
			Log.i("onResume");
			STLRenderer.requestRedraw();
			stlView.onResume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (stlView != null) {
			Log.i("onPause");
			stlView.onPause();
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.i("onRestoreInstanceState");
		Parcelable stlFileName = savedInstanceState.getParcelable("STLFileName");
		if (stlFileName != null) {
			setUpViews((Uri) stlFileName);
		}
		boolean isRotate = savedInstanceState.getBoolean("isRotate");
		ToggleButton toggleButton = (ToggleButton) findViewById(R.id.rotateOrMoveToggleButton);
		toggleButton.setChecked(isRotate);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (stlView != null) {
			Log.i("onSaveInstanceState");
			outState.putParcelable("STLFileName", stlView.getUri());
			outState.putBoolean("isRotate", stlView.isRotate());
		}
	}

	@Override
	public void onClickFileList(File file) {
		if (file == null) {
			return;
		}

		SharedPreferences config = getSharedPreferences("PathSetting", Activity.MODE_PRIVATE);
		SharedPreferences.Editor configEditor = config.edit();
		configEditor.putString("lastPath", file.getParent());
		configEditor.commit();

		setUpViews(Uri.fromFile(file));
	}

	private void setUpViews(Uri uri) {
		setContentView(R.layout.stl);
		final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.rotateOrMoveToggleButton);
		toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (stlView != null) {
                    stlView.setRotate(isChecked);
                }
            }
        });

		final ImageButton loadButton = (ImageButton) findViewById(R.id.loadButton);
		loadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FileListDialog fileListDialog = new FileListDialog(STLViewActivity.this, false, "Choose STL file...", ".stl");
				fileListDialog.setOnFileListDialogListener(STLViewActivity.this);

				SharedPreferences config = getSharedPreferences("PathSetting", Activity.MODE_PRIVATE);
				fileListDialog.show(config.getString("lastPath", "/mnt/sdcard/"));
			}
		});

		final ImageButton preferencesButton = (ImageButton) findViewById(R.id.preferncesButton);
		preferencesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(STLViewActivity.this, PreferencesActivity.class);
				startActivity(intent);
			}
		});

		final TextView textView = (TextView) findViewById(R.id.textView);
		textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (stlView != null) {
					updateTextView("φ: " + Float.toString(stlView.getAngleX()) + "°\nθ: " + Float.toString(stlView.getAngleY()) + "°");
                    //updateTextView(stlView.getUri().getPath().substring(stlView.getUri().getPath().lastIndexOf("/") + 1));
					updatePlot();
				}
			}
		});

		final Button theoryButton = (Button) findViewById(R.id.theoryButton);
		theoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(STLViewActivity.this, WebViewActivity.class);
                startActivity(intent);
            }
        });

		setUpGraph();

		if (uri != null) {
			setTitle(uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));

			FrameLayout relativeLayout = (FrameLayout) findViewById(R.id.stlFrameLayout);
			stlView = new STLView(this, uri);
			relativeLayout.addView(stlView);

			toggleButton.setVisibility(View.VISIBLE);

			stlView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (preferencesButton.getVisibility() == View.INVISIBLE) {
						;
					}
				}
			});
		}
	}

	private XYPlot plot;

	public void setUpGraph(){
		plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

		// same as above
		//XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

		// Create a formatter to use for drawing a series using LineAndPointRenderer
		// and configure it from xml:
		LineAndPointFormatter series1Format = new LineAndPointFormatter();
		//series1Format.setPointLabelFormatter(new PointLabelFormatter());
		//series1Format.setPointLabelFormatter(null);
		//series1Format.configure(getApplicationContext(),
		//        R.xml.line_point_formatter_with_plf1);

		// add a new series' to the xyplot:
		plot.addSeries(series1, new LineAndPointFormatter(Color.RED, Color.GREEN, null, null));

		// same as above:
		//LineAndPointFormatter series2Format = new LineAndPointFormatter();
		//series2Format.setPointLabelFormatter(new PointLabelFormatter());
		//series2Format.configure(getApplicationContext(),
		//       R.xml.line_point_formatter_with_plf2);
		//plot.addSeries(series2, series2Format);

		// reduce the number of range labels
		plot.setTicksPerRangeLabel(3);
		plot.getGraphWidget().setDomainLabelOrientation(-45);
	}

    private XYSeries series1 = new SimpleXYSeries(
            Arrays.asList(graphData.CessnaTheta0),          // SimpleXYSeries takes a List so turn our array into a List
            SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, // Y_VALS_ONLY means use the element index as the x value
            "RCS");

	public void updatePlot() {
        Number[] newData;

        String temp = stlView.getUri().getPath().substring(stlView.getUri().getPath().lastIndexOf("/") + 1);

        float angleby5 = stlView.getAngleY()/5.0f;
        if (temp.equals("Cessna.stl")) {


            if (angleby5 <= 0.0f && angleby5 < 1.0f) {
                if (angleby5 < 0.5f)
                    newData = graphData.CessnaTheta0;
                else
                    newData = graphData.CessnaTheta5;
            } else if (angleby5 <= 1.0f && angleby5 < 2.0f) {
                if (angleby5 < 1.5f)
                    newData = graphData.CessnaTheta5;
                else
                    newData = graphData.CessnaTheta10;
            } else if (angleby5 <= 2.0f && angleby5 < 3.0f) {
                if (angleby5 < 2.5f)
                    newData = graphData.CessnaTheta10;
                else
                    newData = graphData.CessnaTheta15;
            } else if (angleby5 <= 3.0f && angleby5 < 4.0f) {
                if (angleby5 < 3.5f)
                    newData = graphData.CessnaTheta15;
                else
                    newData = graphData.CessnaTheta20;
            } else if (angleby5 <= 4.0f && angleby5 < 5.0f) {
                if (angleby5 < 4.5f)
                    newData = graphData.CessnaTheta20;
                else
                    newData = graphData.CessnaTheta25;
            } else if (angleby5 <= 5.0f && angleby5 < 6.0f) {
                if (angleby5 < 5.5f)
                    newData = graphData.CessnaTheta25;
                else
                    newData = graphData.CessnaTheta30;
            } else if (angleby5 <= 6.0f && angleby5 < 7.0f) {
                if (angleby5 < 6.5f)
                    newData = graphData.CessnaTheta30;
                else
                    newData = graphData.CessnaTheta35;
            } else if (angleby5 <= 7.0f && angleby5 < 8.0f) {
                if (angleby5 < 7.5f)
                    newData = graphData.CessnaTheta35;
                else
                    newData = graphData.CessnaTheta40;
            } else if (angleby5 <= 8.0f && angleby5 < 9.0f) {
                if (angleby5 < 8.5f)
                    newData = graphData.CessnaTheta40;
                else
                    newData = graphData.CessnaTheta45;
            } else if (angleby5 <= 9.0f && angleby5 < 10.0f) {
                if (angleby5 < 9.5f)
                    newData = graphData.CessnaTheta45;
                else
                    newData = graphData.CessnaTheta50;
            } else if (angleby5 <= 10.0f && angleby5 < 11.0f) {
                if (angleby5 < 10.5f)
                    newData = graphData.CessnaTheta50;
                else
                    newData = graphData.CessnaTheta55;
            } else if (angleby5 <= 11.0f && angleby5 < 12.0f) {
                if (angleby5 < 11.5f)
                    newData = graphData.CessnaTheta55;
                else
                    newData = graphData.CessnaTheta60;
            } else if (angleby5 <= 12.0f && angleby5 < 13.0f) {
                if (angleby5 < 12.5f)
                    newData = graphData.CessnaTheta60;
                else
                    newData = graphData.CessnaTheta65;
            } else if (angleby5 <= 13.0f && angleby5 < 14.0f) {
                if (angleby5 < 13.5f)
                    newData = graphData.CessnaTheta65;
                else
                    newData = graphData.CessnaTheta70;
            } else if (angleby5 <= 14.0f && angleby5 < 15.0f) {
                if (angleby5 < 14.5f)
                    newData = graphData.CessnaTheta70;
                else
                    newData = graphData.CessnaTheta75;
            } else if (angleby5 <= 15.0f && angleby5 < 16.0f) {
                if (angleby5 < 15.5f)
                    newData = graphData.CessnaTheta75;
                else
                    newData = graphData.CessnaTheta80;
            } else if (angleby5 <= 16.0f && angleby5 < 17.0f) {
                if (angleby5 < 16.5f)
                    newData = graphData.CessnaTheta80;
                else
                    newData = graphData.CessnaTheta85;
            } else if (angleby5 <= 17.0f && angleby5 < 18.0f) {
                if (angleby5 < 17.5f)
                    newData = graphData.CessnaTheta85;
                else
                    newData = graphData.CessnaTheta90;
            } else if (angleby5 <= 18.0f && angleby5 < 19.0f) {
                if (angleby5 < 18.5f)
                    newData = graphData.CessnaTheta90;
                else
                    newData = graphData.CessnaTheta95;
            } else if (angleby5 <= 19.0f && angleby5 < 20.0f) {
                if (angleby5 < 19.5f)
                    newData = graphData.CessnaTheta95;
                else
                    newData = graphData.CessnaTheta100;
            } else if (angleby5 <= 20.0f && angleby5 < 21.0f) {
                if (angleby5 < 20.5f)
                    newData = graphData.CessnaTheta100;
                else
                    newData = graphData.CessnaTheta105;
            } else if (angleby5 <= 21.0f && angleby5 < 22.0f) {
                if (angleby5 < 21.5f)
                    newData = graphData.CessnaTheta105;
                else
                    newData = graphData.CessnaTheta110;
            } else if (angleby5 <= 22.0f && angleby5 < 23.0f) {
                if (angleby5 < 22.5f)
                    newData = graphData.CessnaTheta110;
                else
                    newData = graphData.CessnaTheta115;
            } else if (angleby5 <= 23.0f && angleby5 < 24.0f) {
                if (angleby5 < 23.5f)
                    newData = graphData.CessnaTheta115;
                else
                    newData = graphData.CessnaTheta120;
            } else if (angleby5 <= 24.0f && angleby5 < 25.0f) {
                if (angleby5 < 24.5f)
                    newData = graphData.CessnaTheta120;
                else
                    newData = graphData.CessnaTheta125;
            } else if (angleby5 <= 25.0f && angleby5 < 26.0f) {
                if (angleby5 < 25.5f)
                    newData = graphData.CessnaTheta125;
                else
                    newData = graphData.CessnaTheta130;
            } else if (angleby5 <= 26.0f && angleby5 < 27.0f) {
                if (angleby5 < 26.5f)
                    newData = graphData.CessnaTheta130;
                else
                    newData = graphData.CessnaTheta135;
            } else if (angleby5 <= 27.0f && angleby5 < 28.0f) {
                if (angleby5 < 27.5f)
                    newData = graphData.CessnaTheta135;
                else
                    newData = graphData.CessnaTheta140;
            } else if (angleby5 <= 28.0f && angleby5 < 29.0f) {
                if (angleby5 < 28.5f)
                    newData = graphData.CessnaTheta140;
                else
                    newData = graphData.CessnaTheta145;
            } else if (angleby5 <= 29.0f && angleby5 < 30.0f) {
                if (angleby5 < 29.5f)
                    newData = graphData.CessnaTheta145;
                else
                    newData = graphData.CessnaTheta150;
            } else if (angleby5 <= 30.0f && angleby5 < 31.0f) {
                if (angleby5 < 30.5f)
                    newData = graphData.CessnaTheta150;
                else
                    newData = graphData.CessnaTheta155;
            } else if (angleby5 <= 31.0f && angleby5 < 32.0f) {
                if (angleby5 < 31.5f)
                    newData = graphData.CessnaTheta155;
                else
                    newData = graphData.CessnaTheta160;
            } else if (angleby5 <= 32.0f && angleby5 < 33.0f) {
                if (angleby5 < 32.5f)
                    newData = graphData.CessnaTheta160;
                else
                    newData = graphData.CessnaTheta165;
            } else if (angleby5 <= 33.0f && angleby5 < 34.0f) {
                if (angleby5 < 33.5f)
                    newData = graphData.CessnaTheta165;
                else
                    newData = graphData.CessnaTheta170;
            } else if (angleby5 <= 34.0f && angleby5 < 35.0f) {
                if (angleby5 < 34.5f)
                    newData = graphData.CessnaTheta170;
                else
                    newData = graphData.CessnaTheta175;
            } else if (angleby5 <= 35.0f && angleby5 < 36.0f) {
                if (angleby5 < 35.5f)
                    newData = graphData.CessnaTheta175;
                else
                    newData = graphData.CessnaTheta180;
            } else {
                if (angleby5 < 36.5)
                    newData = graphData.CessnaTheta180;
                else
                    newData = graphData.CessnaTheta0;
            }
        }
        else
            newData = graphData.BattleShip;
        plot.clear();
        series1 = new SimpleXYSeries(Arrays.asList(newData),
                SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                "RCS");
		plot.addSeries(series1, new LineAndPointFormatter(Color.RED, Color.GREEN, null, null));
		plot.redraw();
	}
}