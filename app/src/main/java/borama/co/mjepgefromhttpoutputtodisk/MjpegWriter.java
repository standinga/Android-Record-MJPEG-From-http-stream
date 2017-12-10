package borama.co.mjepgefromhttpoutputtodisk;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MjpegWriter {

    private static final String TAG = "MjpegWriter";

    private static final String BOUNDARY_PART = "\r\n\r\n--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ";
    private static final String BOUNDARY_DELTA_TIME = "\r\nDelta-time: 110";
    private static final String BOUNDARY_END = "\r\n\r\n";

    private File mjpegFile;
    private FileOutputStream fos;
    private BufferedOutputStream bos;

    private boolean mRecording;

    public void saveByteArrayToFile(byte[] b) {

        if (mRecording) {
            try {
                byte[] boundaryBytes = (BOUNDARY_PART + b.length + BOUNDARY_DELTA_TIME + BOUNDARY_END).getBytes();
                bos.write(boundaryBytes);
                bos.write(b);
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveBitmapToFile(Bitmap bmp, int w, int h) {

        if (mRecording) {
            try {
                ByteArrayOutputStream jpegByteArrayOutputStream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, jpegByteArrayOutputStream);
                byte[] jpegByteArray = jpegByteArrayOutputStream.toByteArray();
                byte[] boundaryBytes = (BOUNDARY_PART + jpegByteArray.length + BOUNDARY_DELTA_TIME + BOUNDARY_END).getBytes();
                bos.write(boundaryBytes);
                bos.write(jpegByteArray);
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void recordMjpeg () {

        Date T = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String szFileName = "vid-"+sdf.format(T)+"-";
        try {
            mjpegFile = File.createTempFile(szFileName, ".mjpeg", Environment.getExternalStorageDirectory());
            startRecording();
            Log.d(TAG, "adb pull " + mjpegFile.getAbsolutePath());
        } catch (Exception e) {
            Log.v(TAG,e.getMessage());
        }
    }

    private void startRecording () {
        try {
            fos = new FileOutputStream(mjpegFile);
            bos = new BufferedOutputStream(fos);
            mRecording = true;
            Log.v(TAG, "Recording Started");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording () {
        mRecording = false;
        try {
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "Recording Stopped");
    }
}
