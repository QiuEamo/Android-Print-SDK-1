/*****************************************************
 *
 * Asset.java
 *
 *
 * Modified MIT License
 *
 * Copyright (c) 2010-2015 Kite Tech Ltd. https://www.kite.ly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The software MAY ONLY be used with the Kite Tech Ltd platform and MAY NOT be modified
 * to be used with any competitor platforms. This means the software MAY NOT be modified 
 * to place orders with any competitors to Kite Tech Ltd, all orders MUST go through the
 * Kite Tech Ltd platform servers. 
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 *****************************************************/

///// Package Declaration /////

package ly.kite.util;

///// Import(s) /////

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

///// Class Declaration /////

/*****************************************************
 *
 * This class represents an image asset that may be supplied
 * to the SDK for eventual printing.
 *
 * Image assets can be supplied from various sources, such
 * as bitmaps, resources, or files.
 *
 *****************************************************/
public class Asset implements Parcelable {
    ////////// Static Variable(s) //////////

    public static final Parcelable.Creator<Asset> CREATOR = new Parcelable.Creator<Asset>() {
        public Asset createFromParcel(Parcel in) {

            return new Asset(in);
        }

        public Asset[] newArray(int size) {

            return new Asset[size];
        }
    };

    ////////// Static Constant(s) //////////

    public static final int BITMAP_TO_JPEG_QUALITY = 80;

    public static final String JPEG_FILE_SUFFIX_PRIMARY = ".jpg";
    public static final String JPEG_FILE_SUFFIX_SECONDARY = ".jpeg";
    public static final String PNG_FILE_SUFFIX = ".png";

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "Asset";

    private static final String SOURCE_CONTENT = "content";
    private static final String SOURCE_FILE = "file";
    private static final String SOURCE_HTTP = "http";
    private static final String SOURCE_HTTPS = "https";

    private static final String SOURCE_PREFIX_CONTENT = SOURCE_CONTENT + "://";
    private static final String SOURCE_PREFIX_FILE = SOURCE_FILE + "://";
    private static final String SOURCE_PREFIX_HTTP = SOURCE_HTTP + "://";
    private static final String SOURCE_PREFIX_HTTPS = SOURCE_HTTPS + "://";

    ////////// Member Variable(s) //////////

    private Type mType;
    private Uri mImageURI;
    private URL mRemoteURL;
    private int mBitmapResourceId;
    private Bitmap mBitmap;
    private String mImageFilePath;
    private byte[] mImageBytes;
    private MIMEType mMIMEType;

    ////////// Static Initialiser(s) //////////

    ////////// Constructor(s) //////////

    /*****************************************************
     *
     * Constructs an asset from a content URI.
     *
     *****************************************************/
    public Asset(Uri uri) {
        // Check that we support the scheme

        final String scheme = uri.getScheme();

        if (scheme == null) {
            throw new IllegalArgumentException("The URI scheme is null");
        }

        if (!scheme.equalsIgnoreCase("content")) {
            throw new IllegalArgumentException("Only URIs with content schemes are currently supported, your scheme " + uri.getScheme() +
                    " is not");
        }

        mType = Type.IMAGE_URI;
        mImageURI = uri;
    }

    /*****************************************************
     *
     * Constructs an asset from a remote URL.
     *
     *****************************************************/
    public Asset(URL url, MIMEType mimeType) {
        // Check that we support the protocol

        if (!url.getProtocol().equalsIgnoreCase("http") && !url.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URL schemes are supported");
        }

        // If the MIME type is not provided - determine it from the extension of the file in the URL

        if (mimeType != null) {
            mMIMEType = mimeType;
        } else {
            // Check that we support the file type. Use the path, since the file will have the query string
            // appended to it.

            final String path = url.getPath().toLowerCase(Locale.UK);

            if (path.endsWith(JPEG_FILE_SUFFIX_PRIMARY) || path.endsWith(JPEG_FILE_SUFFIX_SECONDARY)) {
                mMIMEType = MIMEType.JPEG;
            } else if (path.endsWith(PNG_FILE_SUFFIX)) {
                mMIMEType = MIMEType.PNG;
            } else {
                throw new IllegalArgumentException("If the MIME type is not supplied, the URL must identify the MIME type by ending with " +
                        "a supported file extension i.e. '.jpeg', '.jpg' or '.png' thus '" + path + "' is not valid.");
            }
        }

        mType = Type.REMOTE_URL;
        mRemoteURL = url;
    }

    /*****************************************************
     *
     * Constructs an asset from a remote URL where the MIME
     * type is not known.
     *
     *****************************************************/
    public Asset(URL url) {

        this(url, null);
    }

    /*****************************************************
     *
     * Constructs an asset from an image file.
     *
     *****************************************************/
    public Asset(String imageFilePath) {
        // Check that we support the file type

        final String path = imageFilePath.toLowerCase(Locale.UK);

        if (!path.endsWith(JPEG_FILE_SUFFIX_PRIMARY) &&
                !path.endsWith(JPEG_FILE_SUFFIX_SECONDARY) &&
                !path.endsWith(PNG_FILE_SUFFIX)) {
            throw new IllegalArgumentException("Currently only JPEG & PNG assets are supported");
        }

        mType = Type.IMAGE_FILE;
        mImageFilePath = imageFilePath;
    }

    /*****************************************************
     *
     * Constructs an asset from an image file.
     *
     *****************************************************/
    public Asset(File imageFile) {

        this(imageFile.getAbsolutePath());
    }

    /*****************************************************
     *
     * Constructs an asset from a resource.
     *
     *****************************************************/
    public Asset(int bitmapResourceId) {

        mType = Type.BITMAP_RESOURCE_ID;
        mBitmapResourceId = bitmapResourceId;
    }

    /*****************************************************
     *
     * Constructs an asset from a bitmap.
     *
     *****************************************************/
    public Asset(Bitmap bitmap) {

        mType = Type.BITMAP;
        mBitmap = bitmap;
    }

    /*****************************************************
     *
     * Constructs an asset from a image data.
     *
     *****************************************************/
    public Asset(byte[] imageBytes, MIMEType mimeType) {

        mType = Type.IMAGE_BYTES;
        mImageBytes = imageBytes;
        mMIMEType = mimeType;
    }

    /*****************************************************
     *
     * Constructs an asset from a parcel.
     *
     *****************************************************/
    Asset(Parcel sourceParcel) {

        final String typeName = sourceParcel.readString();

        mType = Type.valueOf(typeName);

        mImageURI = (Uri) sourceParcel.readValue(Uri.class.getClassLoader());
        mRemoteURL = (URL) sourceParcel.readSerializable();
        mBitmapResourceId = sourceParcel.readInt();
        mImageFilePath = sourceParcel.readString();
        mMIMEType = MIMEType.fromString(sourceParcel.readString());
    }

    ////////// Static Method(s) //////////

    /*****************************************************
     *
     * Creates and returns a new asset corresponding to the
     * supplied URL. If the protocol is "file", then a file-backed
     * asset will be returned instead.
     *
     *****************************************************/
    public static Asset create(Object object) {

        if (object != null) {

            if (object instanceof String) {
                ///// String /////

                final String string = (String) object;

                if (string.toLowerCase().startsWith(SOURCE_PREFIX_HTTP) || string.toLowerCase().startsWith(SOURCE_PREFIX_HTTPS)) {
                    try {
                        return new Asset(new URL(string));
                    } catch (MalformedURLException mue) {
                        Log.e(LOG_TAG, "Unable to parse URL: " + string, mue);

                        // Fall through
                    }
                } else if (string.toLowerCase().startsWith(SOURCE_PREFIX_FILE)) {
                    return new Asset(string.substring(SOURCE_PREFIX_FILE.length()));
                } else if (string.toLowerCase().startsWith(SOURCE_PREFIX_CONTENT)) {
                    return new Asset(Uri.parse(string));
                } else if (string.startsWith("/")) {
                    return new Asset(string);
                }
            } else if (object instanceof URL) {
                ///// URL /////

                final URL url = (URL) object;

                final String protocol = url.getProtocol();

                if (protocol != null) {
                    if (protocol.equalsIgnoreCase(SOURCE_FILE)) {
                        return new Asset(url.getPath());
                    }
                }

                return new Asset(url);
            } else if (object instanceof Uri) {
                ///// Uri /////

                final Uri uri = (Uri) object;

                final String scheme = uri.getScheme();

                if (scheme != null) {
                    try {
                        if (scheme.equalsIgnoreCase(SOURCE_FILE)) {
                            return new Asset(uri.getPath());
                        } else if (scheme.equalsIgnoreCase(SOURCE_HTTP)) {
                            return new Asset(new URL(uri.toString()));
                        } else if (scheme.equalsIgnoreCase(SOURCE_HTTPS)) {
                            return new Asset(new URL(uri.toString()));
                        }
                    } catch (MalformedURLException mue) {
                        Log.e(LOG_TAG, "Unable to end get uri scheme imfo: " + uri, mue);
                    }
                }

                return new Asset(uri);
            } else if (object instanceof File) {
                ///// File /////

                final File file = (File) object;

                return new Asset(file);
            } else if (object instanceof Integer) {
                ///// Bitmap resource id /////

                final int resourceId = ((Integer) object).intValue();

                return new Asset(resourceId);
            } else if (object instanceof Bitmap) {
                ///// Bitmap /////

                final Bitmap bitmap = (Bitmap) object;

                return new Asset(bitmap);
            }
        }

        return null;
    }

    /*****************************************************
     *
     * Finds the first available asset in a list.
     *
     *****************************************************/
    public static Asset findFirst(List<Asset> assetList) {

        if (assetList != null) {
            for (Asset asset : assetList) {
                if (asset != null) {
                    return asset;
                }
            }
        }

        return null;
    }

    /*****************************************************
     *
     * Returns true if the asset is in the list.
     *
     *****************************************************/
    public static boolean isInList(List<Asset> assetList, Asset soughtAsset) {

        for (Asset candidateAsset : assetList) {
            if (candidateAsset.equals(soughtAsset)) {
                return true;
            }
        }

        return false;
    }

    /*****************************************************
     *
     * Returns true if both the assets are null, or equal.
     *
     *****************************************************/
    public static boolean areBothNullOrEqual(Asset asset1, Asset asset2) {

        if (asset1 == null && asset2 == null) {
            return true;
        }
        if (asset1 == null || asset2 == null) {
            return false;
        }

        return asset1.equals(asset2);
    }

    /*****************************************************
     *
     * Returns true if both the asset lists are null, or equal.
     *
     *****************************************************/
    public static boolean areBothNullOrEqual(List<Asset> assetList1, List<Asset> assetList2) {

        if (assetList1 == null && assetList2 == null) {
            return true;
        }
        if (assetList1 == null || assetList2 == null) {
            return false;
        }

        if (assetList1.size() != assetList2.size()) {
            return false;
        }

        int assetIndex = 0;

        for (Asset asset1 : assetList1) {
            if (!Asset.areBothNullOrEqual(asset1, assetList2.get(assetIndex))) {
                return false;
            }

            assetIndex++;
        }

        return true;
    }

    ////////// Parcelable Method(s) //////////

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel targetParcel, int flags) {
        // Make sure we can be parcelled
        if (!mType.isParcelable()) {
            throw new IllegalStateException(mType.name() + " asset cannot be parcelled");
        }

        targetParcel.writeString(mType.name());
        targetParcel.writeValue(mImageURI);
        targetParcel.writeSerializable(mRemoteURL);
        targetParcel.writeInt(mBitmapResourceId);
        targetParcel.writeString(mImageFilePath);
        targetParcel.writeString(mMIMEType != null ? mMIMEType.mimeTypeString() : null);
    }

    ////////// Method(s) //////////

    /*****************************************************
     *
     * Returns the asset type.
     *
     *****************************************************/
    public Type getType() {

        return mType;
    }

    /*****************************************************
     *
     * Returns the URI.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from a URI.
     *
     *****************************************************/
    public Uri getImageURI() {

        if (mType != Type.IMAGE_URI) {
            throw new IllegalStateException("The URI has been requested, but the asset type is: " + mType);
        }

        return mImageURI;
    }

    /*****************************************************
     *
     * Returns the bitmap resource id.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from a bitmap resource.
     *
     *****************************************************/
    public int getBitmapResourceId() {

        if (mType != Type.BITMAP_RESOURCE_ID) {
            throw new IllegalStateException("The bitmap resource id has been requested, but the asset type is: " + mType);
        }

        return mBitmapResourceId;
    }

    /*****************************************************
     *
     * Returns the bitmap.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from a bitmap.
     *
     *****************************************************/
    public Bitmap getBitmap() {

        if (mType != Type.BITMAP) {
            throw new IllegalStateException("The bitmap has been requested, but the asset type is: " + mType);
        }

        return mBitmap;
    }

    /*****************************************************
     *
     * Returns the remote URL.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from a remote URL.
     *
     *****************************************************/
    public URL getRemoteURL() {

        if (mType != Type.REMOTE_URL) {
            throw new IllegalStateException("The remote URL has been requested, but the asset type is: " + mType);
        }

        return mRemoteURL;
    }

    /*****************************************************
     *
     * Returns the image file path.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from an image file.
     *
     *****************************************************/
    public String getImageFilePath() {

        if (mType != Type.IMAGE_FILE) {
            throw new IllegalStateException("The image file path has been requested, but the asset type is: " + mType);
        }

        return mImageFilePath;
    }

    /*****************************************************
     *
     * Returns the image file path.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from an image file.
     *
     *****************************************************/
    public File getImageFile() {

        return new File(getImageFilePath());
    }

    /*****************************************************
     *
     * Returns the image file name.
     *
     * @throw IllegalStateException if the asset was not
     *        constructed from an image file.
     *
     *****************************************************/
    public String getImageFileName() {

        final File imageFile = getImageFile();

        return imageFile.getName();
    }

    /*****************************************************
     *
     * Returns the stored image bytes. Note that this is only
     * for use by the {@link AssetHelper}. The proper way to
     * get the MIME type is to use
     * {@link AssetHelper#requestImageBytes}.
     *
     *****************************************************/
    byte[] getImageBytes() {

        if (mImageBytes == null) {
            throw new IllegalStateException("No image bytes were supplied when the asset was created. Did you mean to use AssetHelper" +
                    ".requestImageBytes?");
        }

        return mImageBytes;
    }

    /*****************************************************
     *
     * Returns the stored MIME type. Note that this is only
     * for use by the {@link AssetHelper}. The proper way to
     * get the MIME type is to use
     * {@link AssetHelper#getMimeType(Context, Asset)}.
     *
     *****************************************************/
    MIMEType getMIMEType() {

        if (mMIMEType == null) {
            throw new IllegalStateException("No MIME type was supplied when the asset was created. Did you mean to use AssetHelper" +
                    ".getMIMEType?");
        }

        return mMIMEType;
    }

    /*****************************************************
     *
     * Returns a string representation of this asset.
     *
     *****************************************************/
    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{ ");

        if (mType != null) {
            stringBuilder.append("type = ").append(mType).append(", ");
            stringBuilder.append(mType.describeSource(this));
        } else {
            stringBuilder.append("type = null");
        }

        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    /*****************************************************
     *
     * Returns true, if the supplied object is an asset and
     * is the same as this one, false otherwise.
     *
     *****************************************************/
    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == null || !(otherObject instanceof Asset)) {
            return false;
        }

        final Asset otherAsset = (Asset) otherObject;

        if (otherAsset == this) {
            return true;
        }

        if (mType != otherAsset.mType || mMIMEType != otherAsset.mMIMEType) {
            return false;
        }

        switch (this.mType) {
            case IMAGE_URI:
                return mImageURI.equals(otherAsset.mImageURI);

            case BITMAP_RESOURCE_ID:
                return mBitmapResourceId == otherAsset.mBitmapResourceId;

            case BITMAP:
                return mBitmap.sameAs(otherAsset.mBitmap);

            case REMOTE_URL:
                // Note that we are matching the entire URL; this will fail if the protocol
                // is different: http vs https, even though that should point to the same
                // resource.
                return mRemoteURL.equals(otherAsset.mRemoteURL);

            case IMAGE_FILE:
                return mImageFilePath.equals(otherAsset.mImageFilePath);

            case IMAGE_BYTES:
                return Arrays.equals(mImageBytes, otherAsset.mImageBytes);

            default:
                throw new IllegalStateException("Invalid asset type: " + mType);
        }

    }

    /*****************************************************
     *
     * Returns a hash code for the asset, based on the underlying
     * source.
     *
     *****************************************************/
    @Override
    public int hashCode() {

        switch (mType) {
            case IMAGE_URI:
                return mImageURI.hashCode();

            case BITMAP_RESOURCE_ID:
                return mBitmapResourceId;

            case BITMAP:
                return mBitmap.hashCode();

            case REMOTE_URL:
                return mRemoteURL.hashCode();

            case IMAGE_FILE:
                return mImageFilePath.hashCode();

            case IMAGE_BYTES:
                return Arrays.hashCode(this.mImageBytes);
            default:
                throw new IllegalStateException("Invalid asset type: " + mType);
        }

    }

    /*****************************************************
     *
     * Returns a URI representing this asset.
     *
     *****************************************************/
    public Uri toURI(Context context) {

        switch (mType) {
            case IMAGE_URI:
                return mImageURI;

            case BITMAP_RESOURCE_ID:

                final Resources resources = context.getResources();

                return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                        resources.getResourcePackageName(mBitmapResourceId) + "/" +
                        resources.getResourceTypeName(mBitmapResourceId) + "/" +
                        resources.getResourceEntryName(mBitmapResourceId));

            case REMOTE_URL:
                return Uri.parse(mRemoteURL.toString());

            case IMAGE_FILE:
                return Uri.parse(ContentResolver.SCHEME_FILE + "://" + mImageFilePath);

            default:
                // Fall through
        }

        throw new IllegalStateException("Unable to create URI for asset type: " + mType);
    }

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * An MIME type.
     *
     *****************************************************/
    public enum MIMEType {
        JPEG("image/jpeg", JPEG_FILE_SUFFIX_PRIMARY),
        PNG("image/png", PNG_FILE_SUFFIX);

        ////////// Member Variable(s) //////////

        private final String mMIMETypeString;
        private final String mPrimaryFileSuffix;

        ////////// Constructor(s) //////////

        MIMEType(String mimeTypeString, String primaryFileSuffix) {

            mMIMETypeString = mimeTypeString;
            mPrimaryFileSuffix = primaryFileSuffix;
        }

        ////////// Static Method(s) //////////

        public static MIMEType fromString(String mimeType) {

            if (mimeType == null) {
                return null;
            }

            if (mimeType.equalsIgnoreCase(JPEG.mMIMETypeString)) {
                return JPEG;
            } else if (mimeType.equalsIgnoreCase(PNG.mMIMETypeString)) {
                return PNG;
            }

            throw new UnsupportedOperationException("Requested mimetype " + mimeType + " is not supported");
        }

        ////////// Method(s) //////////

        public String mimeTypeString() {

            return mMIMETypeString;
        }

        public String primaryFileSuffix() {

            return mPrimaryFileSuffix;
        }
    }

    /*****************************************************
     *
     * The type of asset.
     *
     *****************************************************/
    public enum Type {
        IMAGE_URI(true) {
            @Override
            public String describeSource(Asset asset) {

                return "URI = " + asset.getImageURI();
            }
        },
        BITMAP_RESOURCE_ID(true) {
            @Override
            public String describeSource(Asset asset) {

                return "bitmap resource id = " + asset.getBitmapResourceId();
            }
        },
        BITMAP(false) {
            @Override
            public String describeSource(Asset asset) {

                return "[bitmap]";
            }
        },
        IMAGE_BYTES(false) {
            @Override
            public String describeSource(Asset asset) {

                return "[image bytes]";
            }
        },
        IMAGE_FILE(true) {
            @Override
            public String describeSource(Asset asset) {

                return "image file = " + asset.getImageFile().getAbsolutePath();
            }
        },
        REMOTE_URL(true) {
            @Override
            public String describeSource(Asset asset) {

                return "remote URL = " + asset.getRemoteURL();
            }
        };

        private boolean mIsParcelable;

        private Type(boolean isParcelable) {

            mIsParcelable = isParcelable;
        }

        /*****************************************************
         *
         * Returns true if the asset type is safe to be parcelled.
         *
         *****************************************************/
        public boolean isParcelable() {

            return mIsParcelable;
        }

        /*****************************************************
         *
         * Returns a string description of the source of the asset.
         *
         *****************************************************/
        public abstract String describeSource(Asset asset);

    }

}
