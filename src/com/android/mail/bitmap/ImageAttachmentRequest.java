package com.android.mail.bitmap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.android.bitmap.DecodeTask;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * A request object for image attachment previews.
 * <p>
 * All requests are the same size to promote bitmap reuse.
 */
public class ImageAttachmentRequest implements DecodeTask.Request {
    private final Context mContext;
    private final String mLookupUri;
    private final int mRendition;
    public final int mDestW;

    private Uri mCachedUri;
    private String mCachedMimeType;

    public ImageAttachmentRequest(final Context context, final String lookupUri,
            final int rendition, final int destW) {
        mContext = context;
        mLookupUri = lookupUri;
        mRendition = rendition;
        mDestW = destW;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ImageAttachmentRequest)) {
            return false;
        }
        final ImageAttachmentRequest other = (ImageAttachmentRequest) o;
        return TextUtils.equals(mLookupUri, other.mLookupUri) && mRendition == other.mRendition
                && mDestW == other.mDestW;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash += 31 * hash + mLookupUri.hashCode();
        hash += 31 * hash + mRendition;
        hash += 31 * hash + mDestW;
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[");
        sb.append(super.toString());
        sb.append(" uri=");
        sb.append(mLookupUri);
        sb.append(" rendition=");
        sb.append(mRendition);
        sb.append(" w=");
        sb.append(mDestW);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns true iff the other request is for the same attachment, and differs only in which
     * rendition is being requested.
     *
     */
    public boolean matches(ImageAttachmentRequest other) {
        return other != null && TextUtils.equals(mLookupUri, other.mLookupUri)
                && mDestW == other.mDestW;
    }

    @Override
    public AssetFileDescriptor createFd() throws IOException {
        if (mCachedUri == null) {
            cacheValues();
        }
        return mContext.getContentResolver().openAssetFileDescriptor(mCachedUri, "r");
    }

    private void cacheValues() throws IOException {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Uri.parse(mLookupUri),
                    UIProvider.ATTACHMENT_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final Attachment a = new Attachment(cursor);
                mCachedUri = a.getUriForRendition(mRendition);
                final String mimeType = a.getContentType();
                mCachedMimeType = mimeType != null ? mimeType.toLowerCase() : null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return null;
    }

    @Override
    public boolean hasOrientationExif() throws IOException {
        if (mCachedUri == null) {
            cacheValues();
        }
        return mCachedMimeType == null || mCachedMimeType.equals("image/jpeg");
    }
}
