package borama.co.mjepgefromhttpoutputtodisk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

//    private MjpegView mv;
    private static final String TAG = "MainActivity";

    // request permissions to write
    private static int REQUEST_WRITE_PERMISSIONS = 1;
    private static final String[] WRITE_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    String URL = "http://1.2.3.4:5/file.mjpeg";

    private ImageView mImageView;
    private MjpegWriter mMjpegWriter;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.outputImageView); // output frames to imageview
        mMjpegWriter = new MjpegWriter();

        ActivityCompat.requestPermissions(this, WRITE_PERMISSIONS, REQUEST_WRITE_PERMISSIONS);
    }

    private void startSession () {
        MjpegInputStream.read(URL, this, new MjpegInputStream.MJpegListener() {
            @Override
            public void onStream(MjpegInputStream stream) {
                final MjpegInputStream mIS = stream;
                Thread mjpegThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (running) {
                            try {
                                final MjpegContainer mjpegContainer = mIS.readMjpegFrame();
                                if (mMjpegWriter != null) mMjpegWriter.saveByteArrayToFile(mjpegContainer.data);

                                // this is mainly for testing if we are really capturing anything
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Bitmap bitmap = mjpegContainer.bitmap;
                                        mImageView.setImageBitmap(bitmap);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                mjpegThread.start();
            }
        });

        // Test recording videos:
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMjpegWriter.recordMjpeg();
                        }
                    });
                    Thread.sleep(8000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMjpegWriter.stopRecording();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSIONS) {
            if (grantResults.length == WRITE_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "ERROR GETTING PERMISSIONS!", Toast.LENGTH_LONG).show();
                        break;
                    } else {
                        startSession();
                    }
                }
            } else {
                Toast.makeText(this, "ERROR GETTING PERMISSIONS!", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
