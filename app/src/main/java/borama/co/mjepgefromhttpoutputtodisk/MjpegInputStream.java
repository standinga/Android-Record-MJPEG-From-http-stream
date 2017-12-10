package borama.co.mjepgefromhttpoutputtodisk;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class MjpegInputStream extends DataInputStream {

    interface MJpegListener {
        void onStream(MjpegInputStream stream);
    }
    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final String CONTENT_LENGTH = "Content-Length";
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
    private int mContentLength = -1;

    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    }

    public static MjpegInputStream read(final String urlString, final Activity act, final MJpegListener listener) {

            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL(urlString);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestProperty("User-Agent", "");
                            connection.setRequestMethod("GET");
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream inputStream = connection.getInputStream();
                            act.runOnUiThread(new MyRunnable(inputStream, listener));
                    }  catch (IOException e) {
                            e.printStackTrace();
                    }
                }
            }).start();

            return null;
    }

    private static class MyRunnable implements Runnable {

        InputStream inputStream;
        MJpegListener sListener;
        MyRunnable (InputStream input, MJpegListener list) {
            inputStream = input;
            sListener = list;
        }
        @Override
        public void run() {
            sListener.onStream(new MjpegInputStream(inputStream));
        }
    }

    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {

        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length) return i + 1;
            } else seqIndex = 0;
        }
        return -1;
    }

    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {

        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int parseContentLength(byte[] headerBytes) throws IOException, NumberFormatException {

        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }

    public MjpegContainer readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();
        byte[] header = new byte[headerLen];
        readFully(header);
        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException nfe) {
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER);
        }
        reset();
        byte[] frameData = new byte[mContentLength];

        skipBytes(headerLen);
        readFully(frameData);

        Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
        return new MjpegContainer(bm, frameData);
    }
}