/*****************************************************
 *
 * ImageAgent.java
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

package ly.kite.image;

///// Import(s) /////

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import ly.kite.KiteSDK;
import ly.kite.util.Asset;
import ly.kite.util.AssetFragment;
import ly.kite.util.FileDownloader;

///// Class Declaration /////

/*****************************************************
 *
 * This singleton class manages (downloads, saves, and caches)
 * images.
 *
 * Images originate from a network server, and are specified
 * using a URL. Images may be stored in the cache directory on
 * the devices. This allows them to be cleared by clearing
 * the cache in the app manager.
 *
 * In-memory image caching has been removed completely. Due
 * to the stringent requirements of low-end devices, any caching
 * is best left to the caller.
 *
 *****************************************************/
public class ImageAgent {
    ////////// Static Constant(s) //////////

    public static final RectF FULL_PROPORTIONAL_RECTANGLE = new RectF(0.0f, 0.0f, 1.0f, 1.0f);

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "ImageAgent";

    private static final int CROPPED_IMAGE_FILLER_COLOUR = 0xffffffff;

    private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.RGB_565;

    private static final int MAX_FILE_NAME_LENGTH = 200;

    ////////// Static Variable(s) //////////

    private static ImageAgent sImageManager;

    ////////// Member Variable(s) //////////

    private Context mApplicationContext;
    private File mCacheDirectory;

    private HashMap<String, Integer> mURLResourceIdTable;

    private FileDownloader mFileDownloader;
    private ImageRequestProcessor mImageRequestProcessor;

    ////////// Static Initialiser(s) //////////

    ////////// Constructor(s) //////////

    private ImageAgent(Context context) {

        final Context applicationContext = context.getApplicationContext();

        mApplicationContext = applicationContext;
        mCacheDirectory = applicationContext.getCacheDir();
        mURLResourceIdTable = new HashMap<>();

        mFileDownloader = FileDownloader.getInstance(applicationContext);
        mImageRequestProcessor = ImageRequestProcessor.getInstance(applicationContext);
    }

    ////////// Static Method(s) //////////

    /*****************************************************
     *
     * Returns an instance of the image manager.
     *
     *****************************************************/
    public static ImageAgent getInstance(Context context) {

        if (sImageManager == null) {
            sImageManager = new ImageAgent(context);
        }

        return sImageManager;
    }

    /*****************************************************
     *
     * Creates a new image request builder.
     *
     *****************************************************/
    public static ImageAgent with(Context context) {

        return getInstance(context);
    }

    /*****************************************************
     *
     * Returns true if the extension of the supplied file name
     * corresponds to an image type.
     *
     *****************************************************/
    public static boolean hasImageFileExtension(String filePath) {

        if (filePath == null) {
            return false;
        }

        filePath = filePath.toLowerCase();

        if (filePath.endsWith(".jpg") ||
                filePath.endsWith(".jpeg") ||
                filePath.endsWith(".png")) {
            return true;
        }

        return false;
    }

    /*****************************************************
     *
     * Converts the supplied string to a 'safe' string for
     * use in file / directory names.
     *
     *****************************************************/
    public static String toSafeString(String sourceString) {

        if (sourceString == null) {
            return "";
        }

        final int length = sourceString.length();

        final char[] targetCharArray = new char[length];

        for (int index = 0; index < length; index++) {
            final char sourceChar = sourceString.charAt(index);

            if ((sourceChar >= '0' && sourceChar <= '9') ||
                    (sourceChar >= 'A' && sourceChar <= 'Z') ||
                    (sourceChar >= 'a' && sourceChar <= 'z')) {
                // Digits 0-9 and letters A-Z / a-z stay the same
                targetCharArray[index] = sourceChar;
            } else {
                // Everything else gets converted to underscore
                targetCharArray[index] = '_';
            }
        }

        return new String(targetCharArray);
    }

    /*****************************************************
     *
     * Returns a cropped bitmap image.
     *
     *****************************************************/
    public static Bitmap crop(Bitmap originalBitmap, float croppedAspectRatio) {
        // Get the bitmap dimensions
        final int originalWidth = originalBitmap.getWidth();
        final int originalHeight = originalBitmap.getHeight();

        // Avoid divide by zero
        if (originalHeight < KiteSDK.FLOAT_ZERO_THRESHOLD) {
            return originalBitmap;
        }

        final float originalAspectRatio = (float) originalWidth / (float) originalHeight;

        // Crop the bitmap

        final Bitmap croppedBitmap;

        if (croppedAspectRatio <= originalAspectRatio) {
            final float croppedWidth = originalWidth * croppedAspectRatio / originalAspectRatio;
            final float croppedHeight = originalHeight;

            croppedBitmap = originalBitmap.createBitmap(originalBitmap, (int) ((originalWidth - croppedWidth) * 0.5f), 0, (int)
                    croppedWidth, (int) croppedHeight);
        } else {
            final float croppedHeight = originalHeight * originalAspectRatio / croppedAspectRatio;
            final float croppedWidth = originalWidth;

            croppedBitmap = originalBitmap.createBitmap(originalBitmap, 0, (int) ((originalHeight - croppedHeight) * 0.5f), (int)
                    croppedWidth, (int) croppedHeight);
        }

        return croppedBitmap;
    }

    /*****************************************************
     *
     * Returns a proportional crop rectangle using the aspect
     * ratio.
     *
     *****************************************************/
    public static RectF getProportionalCropRectangle(int originalWidth, int originalHeight, float croppedAspectRatio) {
        // Avoid divide by zero
        if (originalHeight < KiteSDK.FLOAT_ZERO_THRESHOLD) {
            return FULL_PROPORTIONAL_RECTANGLE;
        }

        final float originalAspectRatio = (float) originalWidth / (float) originalHeight;

        // Calculate the crop rectangle

        final RectF proportionalCropRectangle;

        if (croppedAspectRatio <= originalAspectRatio) {
            final float croppedHalfWidthProportion = (croppedAspectRatio / originalAspectRatio) * 0.5f;

            proportionalCropRectangle = new RectF(0.5f - croppedHalfWidthProportion, 0.0f, 0.5f + croppedHalfWidthProportion, 1.0f);
        } else {
            final float croppedHalfHeightProportion = (originalAspectRatio / croppedAspectRatio) * 0.5f;

            proportionalCropRectangle = new RectF(0.0f, 0.5f - croppedHalfHeightProportion, 1.0f, 0.5f + croppedHalfHeightProportion);
        }

        return proportionalCropRectangle;
    }

    /*****************************************************
     *
     * Returns a crop rectangle using the aspect ratio.
     *
     *****************************************************/
    public static Rect getCropRectangle(int originalWidth, int originalHeight, float croppedAspectRatio) {

        final RectF proportionalCropRectangle = getProportionalCropRectangle(originalWidth, originalHeight, croppedAspectRatio);

        return new Rect(
                (int) (originalWidth * proportionalCropRectangle.left),
                (int) (originalHeight * proportionalCropRectangle.top),
                (int) (originalWidth * proportionalCropRectangle.right),
                (int) (originalHeight * proportionalCropRectangle.bottom));
    }

    /*****************************************************
     *
     * Returns a cropped bitmap image.
     *
     *****************************************************/
    public static Bitmap crop(Bitmap originalBitmap, RectF proportionalCropRectangle) {
        // Get the bitmap dimensions
        final int originalWidth = originalBitmap.getWidth();
        final int originalHeight = originalBitmap.getHeight();

        // Get the actual bounds
        final int left = (int) (proportionalCropRectangle.left * originalWidth);
        final int top = (int) (proportionalCropRectangle.top * originalHeight);
        final int right = (int) (proportionalCropRectangle.right * originalWidth);
        final int bottom = (int) (proportionalCropRectangle.bottom * originalHeight);

        // If the bounds are completely within the image, we can simply create a new bitmap from the sub area
        if (left >= 0 && top >= 0 && right < originalWidth && bottom < originalHeight) {
            return Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);
        }

        // The bounds are outside the image, so we want to create a white canvas, draw the bitmap into it,
        // and then return it.

        final int croppedWidth = right - left;
        final int croppedHeight = bottom - top;

        // The bitmap config can sometimes be null (if the actual config doesn't match one of
        // the standard types), so use a default config if this happens.

        Bitmap.Config bitmapConfig = originalBitmap.getConfig();

        if (bitmapConfig == null) {
            bitmapConfig = DEFAULT_BITMAP_CONFIG;
        }

        final Bitmap croppedBitmap = Bitmap.createBitmap(croppedWidth, croppedHeight, bitmapConfig);

        final Canvas croppedBitmapCanvas = new Canvas(croppedBitmap);

        croppedBitmapCanvas.drawColor(CROPPED_IMAGE_FILLER_COLOUR);

        final Rect sourceRect = new Rect(0, 0, originalWidth, originalHeight);
        final Rect targetRect = new Rect(-left, -top, originalWidth - left, originalHeight - top);

        croppedBitmapCanvas.drawBitmap(originalBitmap, sourceRect, targetRect, null);

        return croppedBitmap;
    }

    /*****************************************************
     *
     * Returns a downscaled bitmap.
     *
     * If no scaling is required, because the scaled width is
     * < 1, or the source bitmap is smaller than the scaled
     * width, then the original bitmap is returned without
     * alteration.
     *
     *****************************************************/
    public static Bitmap downscaleBitmap(Bitmap sourceBitmap, int scaledWidth) {

        if (scaledWidth < 1 || sourceBitmap.getWidth() <= scaledWidth) {
            return sourceBitmap;
        }

        // Calculate the height so as to maintain the aspect ratio

        final int scaledHeight = (int) ((float) sourceBitmap.getHeight() * (float) scaledWidth / (float) sourceBitmap.getWidth());

        return sourceBitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true);
    }

    /*****************************************************
     *
     * Returns a scaled bitmap.
     *
     * If no scaling is required then the original bitmap
     * is returned without alteration.
     *
     *****************************************************/
    public static Bitmap scaleBitmap(Bitmap sourceBitmap, int scaledWidth) {

        if (scaledWidth < 1 || sourceBitmap.getWidth() < 1) {
            return sourceBitmap;
        }

        // Calculate the height so as to maintain the aspect ratio

        final int scaledHeight = (int) ((float) sourceBitmap.getHeight() * (float) scaledWidth / (float) sourceBitmap.getWidth());

        return sourceBitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true);
    }

    /*****************************************************
     *
     * Returns a scaled bitmap. If no scaling is performed
     * then the original bitmap is returned.
     *
     * @param sourceBitmap The bitmap to be scaled.
     * @param scaledWidth  The width that the bitmap should
     *                     fit inside.
     * @param scaledHeight The height that the bitmap should
     *                     fit inside.
     * @param onlyScaleDown It set to true, the bitmap is only
     *                      scaled down.
     *
     *****************************************************/
    public static Bitmap scaleBitmap(Bitmap sourceBitmap, int scaledWidth, int scaledHeight, boolean onlyScaleDown) {
        // Check the dimensions

        final int originalWidth = sourceBitmap.getWidth();
        final int originalHeight = sourceBitmap.getHeight();

        if (scaledWidth < 1 || scaledHeight < 1 || originalWidth < 1 || originalHeight < 1) {
            return sourceBitmap;
        }

        // Use the smaller of the two scalings
        final float scaleFactor = Math.min((float) scaledWidth / (float) originalWidth, (float) scaledHeight / (float) originalHeight);

        // Only scale up if allowed
        if (scaleFactor > 1.0f && onlyScaleDown) {
            return sourceBitmap;
        }

        // Calculate the new width and height
        final int newWidth = (int) (originalWidth * scaleFactor);
        final int newHeight = (int) (originalHeight * scaleFactor);

        // Resize the bitmap
        return sourceBitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true);
    }

    /*****************************************************
     *
     * Vertically flips the supplied bitmap. It is
     * flipped in place, so the bitmap must be mutable.
     *
     *****************************************************/
    public static void verticallyFlipBitmap(Bitmap bitmap) {

        if (bitmap == null) {
            return;
        }

        final int imageWidth = bitmap.getWidth();
        final int imageHeight = bitmap.getHeight();
        final int imageHalfHeight = imageHeight >>> 1;

        final int[] topRow = new int[imageWidth];
        final int[] bottomRow = new int[imageWidth];

        for (int y = 0; y < imageHalfHeight; y++) {
            bitmap.getPixels(topRow, 0, imageWidth, 0, y, imageWidth, 1);
            bitmap.getPixels(bottomRow, 0, imageWidth, 0, imageHeight - y - 1, imageWidth, 1);

            bitmap.setPixels(bottomRow, 0, imageWidth, 0, y, imageWidth, 1);
            bitmap.setPixels(topRow, 0, imageWidth, 0, imageHeight - y - 1, imageWidth, 1);
        }
    }

    /*****************************************************
     *
     * Horizontally flips the supplied bitmap. It is
     * flipped in place, so the bitmap must be mutable.
     *
     *****************************************************/
    public static void horizontallyFlipBitmap(Bitmap bitmap) {

        if (bitmap == null) {
            return;
        }

        final int imageWidth = bitmap.getWidth();
        final int imageHeight = bitmap.getHeight();
        final int imageHalfWidth = imageWidth >>> 1;

        final int[] leftColumn = new int[imageHeight];
        final int[] rightColumn = new int[imageHeight];

        for (int x = 0; x < imageHalfWidth; x++) {
            bitmap.getPixels(leftColumn, 0, 1, x, 0, 1, imageHeight);
            bitmap.getPixels(rightColumn, 0, 1, imageWidth - x - 1, 0, 1, imageHeight);

            bitmap.setPixels(rightColumn, 0, 1, x, 0, 1, imageHeight);
            bitmap.setPixels(leftColumn, 0, 1, imageWidth - x - 1, 0, 1, imageHeight);
        }
    }

    /*****************************************************
     *
     * Rotates the supplied bitmap anticlockwise.
     *
     *****************************************************/
    public static Bitmap rotateAnticlockwiseBitmap(Bitmap sourceBitmap) {

        if (sourceBitmap == null) {
            return null;
        }

        final int width = sourceBitmap.getWidth();
        final int height = sourceBitmap.getHeight();

        // Create a new Bitmap for the rotated image

        Bitmap targetBitmap = null;

        final Bitmap.Config bitmapConfig = sourceBitmap.getConfig();

        try {
            targetBitmap = Bitmap.createBitmap(height, width, bitmapConfig);
        } catch (OutOfMemoryError oome) {
            // If we ran out of memory trying to create a bitmap with full colour space, try
            // again using a reduced colour space.

            if (bitmapConfig == Bitmap.Config.ARGB_8888) {
                try {
                    targetBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.RGB_565);
                } catch (OutOfMemoryError oome2) {
                    //Ignore . Give up
                }
            }
        }

        if (targetBitmap == null) {
            return sourceBitmap;
        }

        // Scan the source bitmap in columns

        final int[] column = new int[height];

        for (int x = 0; x < width; x++) {
            // Convert the column from the source to a row in the target
            sourceBitmap.getPixels(column, 0, 1, x, 0, 1, height);
            targetBitmap.setPixels(column, 0, height, 0, width - x - 1, height, 1);
        }

        return targetBitmap;
    }

    ////////// Method(s) //////////

    /*****************************************************
     *
     * Adds a single mapping from a URL to a resource id. This
     * is useful if we want to pre-cache any images.
     *
     *****************************************************/
    public ImageAgent addResourceMapping(String urlString, int resourceId) {

        mURLResourceIdTable.put(urlString, resourceId);

        return this;
    }

    /*****************************************************
     *
     * Adds a set of mappings from URLs to resource ids. This
     * is useful if we want to pre-cache any images.
     *
     *****************************************************/
    public ImageAgent addResourceMappings(Pair<String, Integer>... resourceMappings) {

        for (Pair<String, Integer> resourceMapping : resourceMappings) {
            mURLResourceIdTable.put(resourceMapping.first, resourceMapping.second);
        }

        return this;
    }

    /*****************************************************
     *
     * Returns a mapped resource for the supplied URL, or
     * null if there is no mapping.
     *
     *****************************************************/
    public Integer getMappedResource(URL url) {

        return mURLResourceIdTable.get(url.toString());
    }

    /*****************************************************
     *
     * Returns a mapped resource for the supplied URI, or
     * null if there is no mapping.
     *
     *****************************************************/
    public Integer getMappedResource(Uri uri) {

        return mURLResourceIdTable.get(uri.toString());
    }

    /*****************************************************
     *
     * Clears any outstanding load / download requests.
     *
     * Must be called on the UI thread.
     *
     *****************************************************/
    public void clearPendingRequests() {

        mFileDownloader.clearPendingRequests();
        mImageRequestProcessor.clearPendingRequests();
    }

    /*****************************************************
     *
     * Returns an image directory path.
     *
     *****************************************************/
    public String getImageCacheDirectoryForCategory(String imageCategory) {

        return mCacheDirectory.getPath() + File.separator + toSafeString(imageCategory);
    }

    /*****************************************************
     *
     * Returns an image directory path and file path.
     *
     *****************************************************/
    public Pair<String, String> getImageCacheDirectoryAndFilePath(String imageCategory, String imageIdentifier) {
        // Construct the directory and file paths. The file path is: "<cache-directory>/<image-class-string>/<image-url-string>"
        // The image class string and image URL string are first converted into 'safe' strings.
        final String imageDirectoryPath = getImageCacheDirectoryForCategory(imageCategory);
        final String imageFilePath = imageDirectoryPath + File.separator + getImageCacheFileName(imageIdentifier);

        return new Pair<String, String>(imageDirectoryPath, imageFilePath);
    }

    /*****************************************************
     *
     * Returns the directory and file path for an image file
     * used to download an image from a URL.
     *
     *****************************************************/
    public Pair<String, String> getImageCacheDirectoryAndFilePath(String imageCategory, URL imageURL) {
        // Some customers are hitting the file name length limit, and getting an ENAMETOOLONG
        // error. This can happen when the query string is long.
        // If the URL path is the exact file (i.e. ends in an image file extension such as JPG, JPEG,
        // PNG etc.) then we can miss off the query string because that won't change the file that
        // is downloaded. Otherwise we use the whole URL file (including query), truncate the length,
        // and then hope for the best.

        String imageIdentifier;

        final String urlPath = imageURL.getPath();

        if (urlPath != null && hasImageFileExtension(urlPath)) {
            imageIdentifier = imageURL.getProtocol() + "://" + imageURL.getAuthority() + imageURL.getPath();
        } else {
            imageIdentifier = imageURL.toString();
        }

        // Truncate the identifier length if it is too long
        if (imageIdentifier.length() > MAX_FILE_NAME_LENGTH) {
            imageIdentifier = imageIdentifier.substring(0, MAX_FILE_NAME_LENGTH);
        }

        return getImageCacheDirectoryAndFilePath(imageCategory, imageIdentifier);
    }

    /*****************************************************
     *
     * Returns the file name for an image URL.
     *
     *****************************************************/
    public String getImageCacheFileName(String imageIdentifier) {

        return toSafeString(imageIdentifier);
    }

    /*****************************************************
     *
     * Returns the file name for an image URL.
     *
     *****************************************************/
    public String getImageCacheFileName(URL imageURL) {

        return getImageCacheFileName(imageURL.toString());
    }

    /*****************************************************
     *
     * Returns a new image request builder.
     *
     *****************************************************/
    private ImageLoadRequest.Builder getImageRequestBuilder() {

        return new ImageLoadRequest(mApplicationContext).new Builder();
    }

    /*****************************************************
     *
     * Creates an image request builder for bitmap data.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(byte[] bitmapBytes) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(bitmapBytes);
    }

    /*****************************************************
     *
     * Creates an image request builder for a bitmap.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(Bitmap bitmap) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(bitmap);
    }

    /*****************************************************
     *
     * Creates an image request builder for a file.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(File file) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(file);
    }

    /*****************************************************
     *
     * Creates an image request builder for a URL.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(URL url, String imageCategory) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(url, imageCategory);
    }

    /*****************************************************
     *
     * Creates an image request builder for a URL.
     *
     *****************************************************/
    public ImageLoadRequest.Builder loadURL(String urlString, String imageCategory) throws MalformedURLException {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.loadURL(urlString, imageCategory);
    }

    /*****************************************************
     *
     * Creates an image request builder for a URL.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(Uri uri) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(uri);
    }

    /*****************************************************
     *
     * Creates an image request builder for a bitmap resource.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(int bitmapResourceId) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(bitmapResourceId);
    }

    /*****************************************************
     *
     * Sets the source of the image as an asset.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(Asset asset) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(asset);
    }

    /*****************************************************
     *
     * Sets the source of the image as an asset fragment.
     *
     *****************************************************/
    public ImageLoadRequest.Builder load(AssetFragment assetFragment) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.load(assetFragment);
    }

    /*****************************************************
     *
     * Sets the source of the image as an asset.
     *
     *****************************************************/
    public ImageLoadRequest.Builder loadSizeOf(Asset asset) {

        final ImageLoadRequest.Builder builder = getImageRequestBuilder();

        return builder.loadSizeOf(asset);
    }

    /*****************************************************
     *
     * Starts an image processing request.
     *
     *****************************************************/
    public ImageProcessingRequest.Builder transform(Asset asset) {

        final ImageProcessingRequest.Builder builder = new ImageProcessingRequest(mApplicationContext).new Builder();

        builder.transform(asset);

        return builder;
    }

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * ...
     *
     *****************************************************/

}
