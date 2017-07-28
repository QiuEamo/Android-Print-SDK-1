package ly.kite.api;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ly.kite.KiteSDK;
import ly.kite.KiteSDKException;
import ly.kite.ordering.Order;
import ly.kite.util.HTTPJSONRequest;

/**
 * Created by deonbotha on 09/02/2014.
 */
public class SubmitOrderRequest {
    private static final String LOG_TAG = "SubmitOrderRequest";

    private static final boolean DISPLAY_PRINT_ORDER_JSON = false;

    private final Order mPrintOrder;
    private KiteAPIRequest mReq;

    public SubmitOrderRequest(Order printOrder) {

        this.mPrintOrder = printOrder;
    }

    public void submitForPrinting(Context context, final IProgressListener listener) {

        assert mReq == null : "you can only submit a request once";

        final JSONObject json = mPrintOrder.getJSONRepresentation(context);

        if (DISPLAY_PRINT_ORDER_JSON) {
            Log.d(LOG_TAG, "Print Order JSON:\n" + json.toString());
        }

        final String url = String.format("%s/print", KiteSDK.getInstance(context).getAPIEndpoint());
        mReq = new KiteAPIRequest(context, KiteAPIRequest.HttpMethod.POST, url, null, json.toString());
        mReq.start(new HTTPJSONRequest.IJSONResponseListener() {
            @Override
            public void onSuccess(int httpStatusCode, JSONObject json) {

                if (DISPLAY_PRINT_ORDER_JSON) {
                    Log.d(LOG_TAG, "Print Order response JSON:\n" + json.toString());
                }

                try {
                    if (httpStatusCode >= 200 && httpStatusCode <= 299) {
                        final String orderId = json.getString("print_order_id");
                        listener.onSubmissionComplete(SubmitOrderRequest.this, orderId);
                    } else {
                        final JSONObject error = json.getJSONObject("error");
                        final String message = error.getString("message");
                        final String errorCode = error.getString("code");
                        if (errorCode.equalsIgnoreCase("20")) {
                            // this error code indicates an original success response for the request. It's handy to report a success in
                            // this
                            // case as it may be that the client never received the original success response.
                            final String orderId = json.getString("print_order_id");
                            listener.onSubmissionComplete(SubmitOrderRequest.this, orderId);
                        } else {
                            listener.onError(SubmitOrderRequest.this, new KiteSDKException(message));
                        }
                    }
                } catch (JSONException ex) {
                    listener.onError(SubmitOrderRequest.this, ex);
                }
            }

            @Override
            public void onError(Exception exception) {

                listener.onError(SubmitOrderRequest.this, exception);
            }
        });
    }

    public void cancelSubmissionForPrinting() {

        if (mReq != null) {
            mReq.cancel();
            mReq = null;
        }
    }

    public interface IProgressListener {
        public void onSubmissionComplete(SubmitOrderRequest req, String orderId);

        public void onError(SubmitOrderRequest req, Exception error);
    }

}
