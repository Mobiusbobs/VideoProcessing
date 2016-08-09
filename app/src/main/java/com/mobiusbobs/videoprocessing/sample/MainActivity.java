package com.mobiusbobs.videoprocessing.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
  public static String TAG = "MainActivity";

  private static final int REQUEST_PERMISSION_CODE = 1;
  private static String[] PERMISSIONS = {
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA
  };

  private boolean permissionGranted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // toolbar
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    setupListView();

    permissionGranted = verifyPermissions(this);
  }

  private void setupListView() {
    String[] list = {
      "Test Record from SurfaceView",
      "Test Video Processing",
      "Copy Video",
    };

    ListView listView = (ListView)findViewById(R.id.list_view);
    ArrayAdapter<String> listAdapter = new ArrayAdapter<>(
      this,android.R.layout.simple_list_item_1,
      list
    );

    listView.setAdapter(listAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
          case 0: {
            startActivity(VideoRecorderActivity.class);
            break;
          }
          case 1: {
            startActivity(VideoProcessingTestActivity.class);
            break;
          }
          case 2:
            startActivity(CopyVideoActivity.class);
            break;
        }
      }
    });
  }

  private void startActivity(Class<?> klass) {
    if (permissionGranted) {
      Intent intent = new Intent(MainActivity.this, klass);
      startActivity(intent);
    } else {
      printFunctionDisable();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * Checks if the app has permission
   *
   * If the app does not has permission then the user will be prompted to grant permissions
   *
   * @param activity current activity
   */
  private boolean verifyPermissions(Activity activity) {
    // Check if we have write permission
    int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (permission != PackageManager.PERMISSION_GRANTED) {
      // We don't have permission so prompt the user
      ActivityCompat.requestPermissions(
        activity,
        PERMISSIONS,
        REQUEST_PERMISSION_CODE
      );
      return false;
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_PERMISSION_CODE) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];
        Log.d(TAG, "permission = " + permission + ", grantResults = " + grantResult);

        // permission denied
        if (grantResult != PackageManager.PERMISSION_GRANTED) {
          permissionGranted = false;
          return;
        }
      }
      permissionGranted = true;
    }
  }

  private void printFunctionDisable() {
    String msg = "permission is not granted. Function is disabled";
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }
}
