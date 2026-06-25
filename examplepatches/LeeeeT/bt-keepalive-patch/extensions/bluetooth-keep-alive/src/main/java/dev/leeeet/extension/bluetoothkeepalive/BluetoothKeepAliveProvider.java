package dev.leeeet.extension.bluetoothkeepalive;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;

/**
 * Plays an inaudible looping PCM stream for the lifetime of the host app's process so connected
 * Bluetooth headphones stay in active audio mode and don't cut off the first 100-300 ms of any
 * later audio playback.
 *
 * <p>Registered as a {@code <provider>} in the host application's manifest, which causes Android
 * to instantiate this class and call {@link #onCreate()} during process startup -- before
 * {@code Application.onCreate()} -- on the main thread. The {@link AudioTrack} reference is held
 * in a static field so it lives for the entire process lifetime.
 */
public final class BluetoothKeepAliveProvider extends ContentProvider {
    private static final int SAMPLE_RATE_HZ = 44100;
    private static final int FRAME_COUNT = SAMPLE_RATE_HZ; // 1 second of mono 16-bit PCM
    private static final int BUFFER_BYTES = FRAME_COUNT * 2;

    @SuppressWarnings("unused")
    private static AudioTrack track;

    @Override
    public boolean onCreate() {
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();

            AudioTrack t = new AudioTrack(
                    attributes,
                    format,
                    BUFFER_BYTES,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);

            short[] silence = new short[FRAME_COUNT];
            t.write(silence, 0, FRAME_COUNT);
            t.setLoopPoints(0, FRAME_COUNT, -1);
            t.play();

            track = t;
        } catch (Throwable ignored) {
            // Best effort. If the device cannot create an AudioTrack at this point, the patch is
            // a no-op and the host app behaves as if it weren't applied.
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
