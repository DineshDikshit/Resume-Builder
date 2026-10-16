package com.example.resumebuilder.pdf;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

/** Open and share actions for an exported PDF, using a temporary read grant on its URI. */
public final class PdfActions {

    private static final String MIME_PDF = "application/pdf";

    private PdfActions() {
    }

    /** Returns false when no installed app can show PDF files. */
    public static boolean open(Activity activity, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MIME_PDF);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /** Returns false when no installed app can receive the file. */
    public static boolean share(Activity activity, Uri uri, String subject) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(MIME_PDF);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.setClipData(ClipData.newRawUri("Resume PDF", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(Intent.createChooser(send, "Share resume"));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}