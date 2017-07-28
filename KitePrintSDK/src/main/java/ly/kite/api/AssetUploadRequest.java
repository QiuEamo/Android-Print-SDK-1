package ly.kite.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ly.kite.KiteSDK;
import ly.kite.util.Asset;
import ly.kite.util.AssetHelper;
import ly.kite.util.HTTPJSONRequest;
import ly.kite.util.UploadableImage;

/**
 * Created by deonbotha on 07/02/2014.
 */
public class AssetUploadRequest {

    private Context mContext;

    private boolean mCancelled;
    private KiteAPIRequest mRegisterImageURLAssetsReq;
    private KiteAPIRequest mSignReq;
    private int mNumOutstandingAsyncOpertions = 0;
    private boolean mNotifiedUploadListenerOfOutcome = false;

    public AssetUploadRequest(Context context) {

        mContext = context;
    }

    public void cancelUpload() {

        mCancelled = true;
        mNotifiedUploadListenerOfOutcome = false;
        mNumOutstandingAsyncOpertions = 0;
        if (mRegisterImageURLAssetsReq != null) {
            mRegisterImageURLAssetsReq.cancel();
            mRegisterImageURLAssetsReq = null;
        }

        if (mSignReq != null) {
            mSignReq.cancel();
            mSignReq = null;
        }
    }

    public void uploadAsset(UploadableImage uploadableImage, Context context, IProgressListener uploadListener) {

        final ArrayList<UploadableImage> list = new ArrayList<>();
        list.add(uploadableImage);
        uploadAssets(context, list, uploadListener);
    }

    public void uploadAssets(Context context, final List<UploadableImage> uploadableImages, final IProgressListener uploadListener) {

        final ArrayList<UploadableImage> urlsToRegister = new ArrayList<>();
        final ArrayList<UploadableImage> assetsToUpload = new ArrayList<>();

        for (UploadableImage uploadableImage : uploadableImages) {
            if (uploadableImage.getType() == Asset.Type.REMOTE_URL) {
                urlsToRegister.add(uploadableImage);
            } else {
                assetsToUpload.add(uploadableImage);
            }
        }

        final AssetUploadOrRegisterListener listener = new AssetUploadOrRegisterListener() {
            @Override
            public void onProgress(int totalAssetsUploaded, int totalAssetsToUpload, long bytesWritten, long totalAssetBytesWritten, long
                    totalAssetBytesExpectedToWrite) {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }
                uploadListener.onProgress(AssetUploadRequest.this, totalAssetsUploaded, totalAssetsToUpload, bytesWritten,
                        totalAssetBytesWritten, totalAssetBytesExpectedToWrite);
            }

            @Override
            public void onSuccess() {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }
                completedOutstandingAsyncOperation(uploadableImages, null, uploadListener);
            }

            @Override
            public void onError(Exception ex) {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }
                completedOutstandingAsyncOperation(uploadableImages, ex, uploadListener);
            }
        };

        if (assetsToUpload.size() > 0) {
            ++mNumOutstandingAsyncOpertions;
            uploadAssets(context, assetsToUpload, listener);
        }

        if (urlsToRegister.size() > 0) {
            ++mNumOutstandingAsyncOpertions;
            registerImageURLs(context, urlsToRegister, listener);
        }
    }

    private void completedOutstandingAsyncOperation(List<UploadableImage> uploadableImages, Exception ex, IProgressListener listener) {

        if (mCancelled || mNotifiedUploadListenerOfOutcome) {
            return;
        }

        if (ex != null) {
            mNotifiedUploadListenerOfOutcome = true;
            listener.onError(this, ex);
            return;
        }

        if (--mNumOutstandingAsyncOpertions == 0) {
            mNotifiedUploadListenerOfOutcome = true;
            assert ex == null : "errors should be covered above";
            listener.onUploadComplete(this, uploadableImages);
        }
    }

    private void getSignedS3UploadRequestURLs(Context context, final List<UploadableImage> uploadableImages, final
        SignS3UploadsRequestListener listener) {

        final StringBuilder mimeTypes = new StringBuilder();
        for (UploadableImage uploadableImage : uploadableImages) {
            if (mimeTypes.length() > 0) {
                mimeTypes.append(",");
            }

            mimeTypes.append(AssetHelper.getMimeType(context, uploadableImage.getAsset()).mimeTypeString());
        }

        final String url = String.format("%s/asset/sign/?mime_types=%s&client_asset=true", KiteSDK.getInstance(context).getAPIEndpoint(),
                mimeTypes.toString());
        mRegisterImageURLAssetsReq = new KiteAPIRequest(context, KiteAPIRequest.HttpMethod.GET, url, null, (String) null);
        mRegisterImageURLAssetsReq.start(new HTTPJSONRequest.IJSONResponseListener() {
            @Override
            public void onSuccess(int httpStatusCode, JSONObject json) {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }

                try {
                    if (httpStatusCode != 200) {
                        final JSONObject error = json.getJSONObject("error");
                        final String message = error.getString("message");
                        listener.onError(new IllegalStateException(message));
                        return;
                    }

                    final JSONArray signedUploadReqURLs = json.getJSONArray("signed_requests");
                    final JSONArray assetS3PreviewURLs = json.getJSONArray("urls");
                    final JSONArray assetIds = json.getJSONArray("asset_ids");

                    if (uploadableImages.size() != signedUploadReqURLs.length() || signedUploadReqURLs.length() != assetS3PreviewURLs
                            .length() || assetS3PreviewURLs.length() != assetIds.length()) {
                        listener.onError(new IllegalStateException(String.format("Only got sign %d/%d sign requests", signedUploadReqURLs
                                .length(), uploadableImages.size())));
                        return;
                    }

                    final ArrayList<SignedS3RequestUploadDetails> details = new ArrayList<SignedS3RequestUploadDetails>();
                    for (int i = 0; i < signedUploadReqURLs.length(); ++i) {
                        final SignedS3RequestUploadDetails d = new SignedS3RequestUploadDetails();
                        d.mSignedS3UploadReqURL = new URL(signedUploadReqURLs.getString(i));
                        d.mS3AssetPreviewURL = new URL(assetS3PreviewURLs.getString(i));
                        d.mAssetId = assetIds.getLong(i);
                        d.mUploadableImage = uploadableImages.get(i);
                        details.add(d);
                    }

                    listener.onSuccess(details);
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }

            @Override
            public void onError(Exception exception) {

                listener.onError(exception);
            }
        });
    }

    private void uploadAssetToS3(Context context, final SignedS3RequestUploadDetails details, final byte[] bytes, final
        UploadToS3Listener listener) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Attempting to kick off asset upload on a thread that is not the main thread");
        }

        final String mimeType = AssetHelper.getMimeType(context, details.mUploadableImage.getAsset()).mimeTypeString();
        final AsyncTask<Void, Void, Exception> uploadTask = new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... voids) {

                final HttpClient httpclient = new DefaultHttpClient();
                final HttpPut request = new HttpPut(details.mSignedS3UploadReqURL.toString());
                request.setHeader("Content-Type", mimeType);
                request.setHeader("x-amz-acl", "private");
                request.setEntity(new ByteArrayEntity(bytes));

                try {
                    final HttpResponse response = httpclient.execute(request);
                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode <= 299) {
                        return null;
                    } else {
                        return new IllegalStateException("Failed to upload asset to amazon s3 with status code: " + statusCode);
                    }
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception ex) {

                if (ex != null) {
                    listener.onError(ex);
                } else {
                    listener.onProgress(bytes.length, bytes.length, bytes.length); // TODO: do proper upload progress tracking rather
                    // than one off callback on completion
                    listener.onUploadComplete();
                }
            }
        };

        uploadTask.execute();
    }

    private void uploadAssetsToS3(final Context context, final List<SignedS3RequestUploadDetails> remainingAssetsToUpload, final
            List<UploadableImage> uploadedAssetAccumulator, final AssetUploadOrRegisterListener listener) {

        if (mCancelled || mNotifiedUploadListenerOfOutcome) {
            return;
        }

        final SignedS3RequestUploadDetails assetToUploadDetails = remainingAssetsToUpload.remove(0);
        final int totalAssetsToUpload = uploadedAssetAccumulator.size() + remainingAssetsToUpload.size() + 1;
        AssetHelper.requestImageBytes(context, assetToUploadDetails.mUploadableImage.getAsset(), new AssetHelper.IImageBytesConsumer() {
            @Override
            public void onAssetBytes(Asset asset, byte[] bytes) {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }

                listener.onProgress(uploadedAssetAccumulator.size(), totalAssetsToUpload, 0, 0, bytes.length);
                uploadAssetToS3(context, assetToUploadDetails, bytes, new UploadToS3Listener() {
                    @Override
                    public void onProgress(long bytesWritten, long totalAssetBytesWritten, long totalAssetBytesExpectedToWrite) {

                        if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                            return;
                        }
                        listener.onProgress(uploadedAssetAccumulator.size(), totalAssetsToUpload, bytesWritten, totalAssetBytesWritten,
                                totalAssetBytesExpectedToWrite);
                    }

                    @Override
                    public void onUploadComplete() {

                        if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                            return;
                        }

                        final UploadableImage uploadableImage = assetToUploadDetails.mUploadableImage;
                        uploadableImage.markAsUploaded(assetToUploadDetails.mAssetId, assetToUploadDetails.mS3AssetPreviewURL);
                        uploadedAssetAccumulator.add(uploadableImage);
                        if (remainingAssetsToUpload.size() == 0) {
                            listener.onSuccess();
                        } else {
                            uploadAssetsToS3(context, remainingAssetsToUpload, uploadedAssetAccumulator, listener);
                        }
                    }

                    @Override
                    public void onError(Exception ex) {

                        if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                            return;
                        }
                        listener.onError(ex);
                    }
                });
            }

            @Override
            public void onAssetError(Asset asset, Exception ex) {

                if (mCancelled || mNotifiedUploadListenerOfOutcome) {
                    return;
                }
                listener.onError(ex);
            }
        });
    }

    private void uploadAssets(final Context context, final List<UploadableImage> uploadableImages, final AssetUploadOrRegisterListener
            listener) {

        getSignedS3UploadRequestURLs(context, uploadableImages, new SignS3UploadsRequestListener() {

            @Override
            public void onSuccess(ArrayList<SignedS3RequestUploadDetails> details) {

                uploadAssetsToS3(context, details, new ArrayList<UploadableImage>(), listener);
            }

            @Override
            public void onError(Exception ex) {

                listener.onError(ex);
            }
        });
    }

    private void registerImageURLs(Context context, final List<UploadableImage> uploadableImages, final AssetUploadOrRegisterListener
            listener) {

        int c = 0;
        final JSONObject jsonBody = new JSONObject();
        final JSONArray objects = new JSONArray();
        try {
            jsonBody.put("objects", objects);
            for (UploadableImage uploadableImage : uploadableImages) {
                if (uploadableImage.getType() == Asset.Type.REMOTE_URL) {
                    ++c;
                    final JSONObject o = new JSONObject();
                    o.put("url", uploadableImage.getRemoteURL().toString());
                    o.put("client_asset", true);
                    o.put("mime_type", AssetHelper.getMimeType(context, uploadableImage.getAsset()).mimeTypeString());
                    objects.put(o);
                }
            }
        } catch (JSONException ex) {
            listener.onError(ex);
            return;
        }

        final int expectedRegisteredAssetCount = c;

        final String url = String.format("%s/asset/", KiteSDK.getInstance(context).getAPIEndpoint());
        mRegisterImageURLAssetsReq = new KiteAPIRequest(context, KiteAPIRequest.HttpMethod.PATCH, url, null, jsonBody.toString());
        mRegisterImageURLAssetsReq.start(new HTTPJSONRequest.IJSONResponseListener() {
            @Override
            public void onSuccess(int httpStatusCode, JSONObject json) {

                int registeredAssetCount = 0;
                try {
                    if (httpStatusCode >= 200 && httpStatusCode <= 299) {
                        final JSONArray objects = json.getJSONArray("objects");
                        for (int i = 0; i < objects.length(); ++i) {
                            final JSONObject o = objects.getJSONObject(i);
                            final long assetId = o.getLong("asset_id");
                            final URL previewURL = new URL(o.getString("url"));

                            for (UploadableImage uploadableImage : uploadableImages) {
                                if (uploadableImage.getType() == Asset.Type.REMOTE_URL && uploadableImage.getRemoteURL().equals
                                        (previewURL)) {
                                    uploadableImage.markAsUploaded(assetId, previewURL);
                                    ++registeredAssetCount;
                                }
                            }
                        }

                        if (registeredAssetCount == expectedRegisteredAssetCount) {
                            listener.onSuccess();
                        } else {
                            listener.onError(new IllegalStateException(String.format("Only registered %d/%d image URLs with the asset " +
                                    "endpoint", registeredAssetCount, expectedRegisteredAssetCount)));
                        }
                    } else {
                        final JSONObject error = json.getJSONObject("error");
                        final String message = error.getString("message");
                        listener.onError(new IllegalStateException(message));
                    }
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }

            @Override
            public void onError(Exception exception) {

                listener.onError(exception);
            }
        });
    }

    private static class SignedS3RequestUploadDetails {
        URL mSignedS3UploadReqURL;
        URL mS3AssetPreviewURL;
        long mAssetId;
        UploadableImage mUploadableImage;
    }

    private static interface AssetUploadOrRegisterListener {
        void onProgress(int totalAssetsUploaded, int totalAssetsToUpload, long bytesWritten, long totalAssetBytesWritten, long
                totalAssetBytesExpectedToWrite);

        void onSuccess();

        void onError(Exception ex);
    }

    private static interface SignS3UploadsRequestListener {
        void onSuccess(ArrayList<SignedS3RequestUploadDetails> details);

        void onError(Exception ex);
    }

    private static interface UploadToS3Listener {
        void onProgress(long bytesWritten, long totalAssetBytesWritten, long totalAssetBytesExpectedToWrite);

        void onUploadComplete();

        void onError(Exception ex);
    }

    public interface IProgressListener {
        void onProgress(AssetUploadRequest req, int totalAssetsUploaded, int totalAssetsToUpload, long bytesWritten, long
                totalAssetBytesWritten, long totalAssetBytesExpectedToWrite);

        void onUploadComplete(AssetUploadRequest req, List<UploadableImage> uploadableImages);

        void onError(AssetUploadRequest req, Exception error);
    }

}
