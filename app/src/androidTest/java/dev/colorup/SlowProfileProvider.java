package dev.colorup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Standalone test APK process: use only platform/Java classes. */
public class SlowProfileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/json"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            new Thread(() -> {
                try {
                    Thread.sleep(1800);
                    try (ParcelFileDescriptor.AutoCloseOutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                        output.write("{\"schemaVersion\":2,\"animations\":[{\"id\":\"unlock_to_app\",\"durationMs\":730}]}".getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception error) { throw new RuntimeException(error); }
            }).start();
            return pipe[0];
        } catch (IOException error) { throw new java.io.FileNotFoundException(error.toString()); }
    }
}
