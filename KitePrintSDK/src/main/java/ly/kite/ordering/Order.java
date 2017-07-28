package ly.kite.ordering;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ly.kite.KiteSDK;
import ly.kite.address.Address;
import ly.kite.api.AssetUploadRequest;
import ly.kite.api.SubmitOrderRequest;
import ly.kite.catalogue.Product;
import ly.kite.catalogue.SingleCurrencyAmounts;
import ly.kite.image.ImageAgent;
import ly.kite.image.ImageProcessingRequest;
import ly.kite.pricing.OrderPricing;
import ly.kite.util.Asset;
import ly.kite.util.AssetFragment;
import ly.kite.util.UploadableImage;

/**
 * Created by deonbotha on 09/02/2014.
 */
public class Order implements Parcelable /* , Serializable */ {

    public static final Parcelable.Creator<Order> CREATOR = new Parcelable.Creator<Order>() {
        public Order createFromParcel(Parcel in) {

            return new Order(in);
        }

        public Order[] newArray(int size) {

            return new Order[size];
        }
    };

    private static final String LOG_TAG = "Order";

    private static final int NOT_PERSISTED = -1;

    private static final int JOB_TYPE_POSTCARD = 0;
    private static final int JOB_TYPE_GREETING_CARD = 1;
    private static final int JOB_TYPE_PHOTOBOOK = 2;
    private static final int JOB_TYPE_PRINTS = 3;

    private static final String JSON_NAME_LOCALE = "locale";
    private static final String JSON_NAME_JOB_ID = "job_id";
    private static final String JSON_NAME_QUANTITY = "quantity";
    private static final String JSON_NAME_TEMPLATE_ID = "template_id";
    private static final String JSON_NAME_COUNTRY_CODE = "country_code";
    private static final String JSON_NAME_SHIPPING_CLASS = "shipping_class";

    // These values are used to build the order
    private ArrayList<Job> mJobs = new ArrayList<Job>();
    private Address mShippingAddress;
    private String mStatusNotificationEmail;
    private String mStatusNotificationPhone;
    private JSONObject mUserData;
    private HashMap<String, String> mAdditionalParametersMap;
    private String mPromoCode;
    //private String                       voucherCode;

    // These values are generated throughout the order processing, but
    // need to be persisted in some form for the order history
    private OrderPricing mOrderPricing;
    private String mProofOfPayment;
    private String mReceipt;

    // Transient values solely used during order submission
    private boolean mUserSubmittedForPrinting;
    private AssetUploadRequest mAssetUploadReq;
    private int mImagesToCropCount;
    private List<UploadableImage> mImagesToUpload;
    private boolean mAssetUploadComplete;
    private SubmitOrderRequest mPrintOrderReq;
    private Date mLastPrintSubmissionDate;
    private ISubmissionProgressListener mSubmissionListener;
    private Exception mLastPrintSubmissionError;
    private int mStorageIdentifier = NOT_PERSISTED;

    public Order() {

    }

    private Order(Parcel p) {

        this.mShippingAddress = (Address) p.readValue(Address.class.getClassLoader());
        this.mProofOfPayment = p.readString();
        //this.voucherCode = p.readString();
        final String userDataString = p.readString();
        if (userDataString != null) {
            try {
                this.mUserData = new JSONObject(userDataString);
            } catch (JSONException ex) {
                throw new RuntimeException(ex); // will never happen ;)
            }
        }

        mAdditionalParametersMap = (HashMap<String, String>) p.readSerializable();

        final int numJobs = p.readInt();

        for (int i = 0; i < numJobs; ++i) {
            final int jobType = p.readInt();

            final Job job;

            switch (jobType) {
                case JOB_TYPE_POSTCARD:
                    job = PostcardJob.CREATOR.createFromParcel(p);
                    break;

                case JOB_TYPE_GREETING_CARD:
                    job = GreetingCardJob.CREATOR.createFromParcel(p);
                    break;

                case JOB_TYPE_PHOTOBOOK:
                    job = PhotobookJob.CREATOR.createFromParcel(p);
                    break;

                default:
                    job = ImagesJob.CREATOR.createFromParcel(p);
            }

            this.mJobs.add(job);
        }

        this.mUserSubmittedForPrinting = (Boolean) p.readValue(Boolean.class.getClassLoader());
        this.mAssetUploadComplete = (Boolean) p.readValue(Boolean.class.getClassLoader());
        this.mLastPrintSubmissionDate = (Date) p.readValue(Date.class.getClassLoader());
        this.mReceipt = p.readString();
        this.mLastPrintSubmissionError = (Exception) p.readSerializable();
        this.mStorageIdentifier = p.readInt();
        this.mPromoCode = p.readString();
        mOrderPricing = (OrderPricing) p.readParcelable(OrderPricing.class.getClassLoader());
        this.mStatusNotificationEmail = p.readString();
        this.mStatusNotificationPhone = p.readString();
    }

    /*****************************************************
     *
     * Constructor used by basket activity.
     *
     *****************************************************/
    public Order(Context context,
                 List<BasketItem> basketItemList,
                 Address shippingAddress,
                 String contactEmail,
                 String contactPhone,
                 HashMap<String, String> additionalParametersMap) {
        // Convert the basket items into jobs

        if (basketItemList != null) {
            for (BasketItem basketItem : basketItemList) {
                final Product product = basketItem.getProduct();
                final int orderQuantity = basketItem.getOrderQuantity();

                product.getUserJourneyType().addJobsToOrder(context, product, orderQuantity, basketItem.getOptionsMap(), basketItem
                        .getImageSpecList(), this, basketItem.getShippingClass());
            }
        }

        setShippingAddress(shippingAddress);
        setEmail(contactEmail);
        setPhone(contactPhone);
        setAdditionalParameters(additionalParametersMap);
    }

    /*****************************************************
     *
     * Constructor used by order history fragment.
     *
     *****************************************************/
    public Order(Context context,
                 List<BasketItem> basketItemList,
                 Address shippingAddress,
                 String contactEmail,
                 String contactPhone,
                 JSONObject userDataJSONObject,
                 HashMap<String, String> additionalParametersMap,
                 String promoCode,
                 OrderPricing orderPricing,
                 String proofOfPayment,
                 String receipt) {

        this(context, basketItemList, shippingAddress, contactEmail, contactPhone, additionalParametersMap);

        setUserData(userDataJSONObject);
        setPromoCode(promoCode);
        setOrderPricing(orderPricing);
        if (proofOfPayment != null) {
            setProofOfPayment(proofOfPayment);
        }
        setReceipt(receipt);
    }

    public Order setShippingAddress(Address shippingAddress) {

        this.mShippingAddress = shippingAddress;

        return this;
    }

    public Address getShippingAddress() {

        return mShippingAddress;
    }

    public ArrayList<Job> getJobs() {

        return mJobs;
    }

    public void setProofOfPayment(String proofOfPayment) {

        if (proofOfPayment == null ||
                (!proofOfPayment.startsWith("AP-") &&
                        !proofOfPayment.startsWith("PAY-") &&
                        !proofOfPayment.startsWith("PAUTH-") &&
                        !proofOfPayment.startsWith("tok_"))) {
            throw new IllegalArgumentException("Proof of payment must start with AP-, PAY-, PAUTH-, or tok_ : " + proofOfPayment);
        }

        this.mProofOfPayment = proofOfPayment;
    }

    public String getProofOfPayment() {

        return mProofOfPayment;
    }

    //    public void setVoucherCode(String voucherCode) {
    //        this.voucherCode = voucherCode;
    //    }
    //
    //    public String getVoucherCode() {
    //        return voucherCode;
    //    }

    public void setUserData(JSONObject userData) {

        this.mUserData = userData;
    }

    public void setAdditionalParameters(HashMap<String, String> additionalParametersMap) {

        mAdditionalParametersMap = additionalParametersMap;
    }

    public HashMap<String, String> getAdditionalParameters() {

        return mAdditionalParametersMap;
    }

    public Order setAdditionalParameter(String parameterName, String parameterValue) {

        if (mAdditionalParametersMap == null) {
            mAdditionalParametersMap = new HashMap<>();
        }

        mAdditionalParametersMap.put(parameterName, parameterValue);

        return this;
    }

    public String getAdditionalParameter(String parameterName) {

        if (mAdditionalParametersMap != null) {
            return mAdditionalParametersMap.get(parameterName);
        }

        return null;
    }

    /*****************************************************
     *
     * Sets a user data parameter, creating a user data object
     * if necessary.
     *
     *****************************************************/
    public Order setUserDataParameter(String parameterName, String parameterValue) {

        if (mUserData == null) {
            mUserData = new JSONObject();
        }

        try {
            mUserData.put(parameterName, parameterValue);
        } catch (JSONException je) {
            Log.e(LOG_TAG, "Unable to set " + parameterName + " = " + parameterValue, je);
        }

        return this;
    }

    /*****************************************************
     *
     * Clears a user data parameter.
     *
     *****************************************************/
    public Order removeUserDataParameter(String parameterName) {
        // We don't need to clear anything if there is no user data
        if (mUserData != null) {
            mUserData.remove(parameterName);
        }

        return this;
    }

    /*****************************************************
     *
     * Sets both the notification and user data email.
     *
     *****************************************************/
    public Order setEmail(String email) {

        setNotificationEmail(email);

        setUserDataParameter("email", email);

        return this;
    }

    /*****************************************************
     *
     * Sets both the notification and user data phone number.
     *
     *****************************************************/
    public Order setPhone(String phone) {

        setNotificationPhoneNumber(phone);

        setUserDataParameter("phone", phone);

        return this;
    }

    public void setNotificationEmail(String receiptEmail) {

        this.mStatusNotificationEmail = receiptEmail;
    }

    public String getNotificationEmail() {

        return mStatusNotificationEmail;
    }

    public void setNotificationPhoneNumber(String statusNotificationPhone) {

        this.mStatusNotificationPhone = statusNotificationPhone;
    }

    public String getNotificationPhoneNumber() {

        return mStatusNotificationPhone;
    }

    public JSONObject getUserData() {

        return mUserData;
    }

    /*****************************************************
     *
     * Returns the JSON representation of this order for
     * submission to the print request endpoint.
     *
     *****************************************************/
    public JSONObject getJSONRepresentation(Context context) {

        try {
            final JSONObject json = new JSONObject();
            if (mProofOfPayment != null) {
                json.put("proof_of_payment", mProofOfPayment);
            } else {
                json.put("proof_of_payment", "");
            }

            json.put("receipt_email", mStatusNotificationEmail);
            if (mPromoCode != null) {
                json.put("promo_code", mPromoCode);
            }

            // Add the jobs

            final JSONArray jobs = new JSONArray();
            JSONObject tempJobs = new JSONObject();

            json.put("jobs", jobs);

            for (Job job : this.mJobs) {
                // Duplicate jobs orderQuantity times

                final int orderQuantity = job.getOrderQuantity();

                for (int index = 0; index < orderQuantity; index++) {
                    tempJobs = job.getJSONRepresentation();
                    tempJobs.put(JSON_NAME_SHIPPING_CLASS, job.getShippingClass());
                    jobs.put(tempJobs);
                }
            }

            // Make sure we always have user data, and put the default locale into it

            if (mUserData == null) {
                mUserData = new JSONObject();
            }

            mUserData.put(JSON_NAME_LOCALE, Locale.getDefault().toString());

            json.put("user_data", mUserData);

            // Add any additional parameters

            if (mAdditionalParametersMap != null) {
                for (String parameterName : mAdditionalParametersMap.keySet()) {
                    final String parameterValue = mAdditionalParametersMap.get(parameterName);

                    json.put(parameterName, parameterValue);
                }
            }

            // Add the customer payment information

            final OrderPricing orderPricing = getOrderPricing();

            if (orderPricing != null) {
                final String preferredCurrencyCode = KiteSDK.getInstance(context).getLockedCurrencyCode();

                final SingleCurrencyAmounts orderTotalCost = orderPricing.getTotalCost().getAmountsWithFallback(preferredCurrencyCode);

                // construct customer payment object in a round about manner to guarantee 2dp amount value
                final StringBuilder builder = new StringBuilder();
                builder.append("{");
                builder.append("\"currency\": \"").append(orderTotalCost.getCurrencyCode()).append("\"").append(",");
                builder.append(String.format(Locale.ENGLISH, "\"amount\": %.2f", orderTotalCost.getAmount().floatValue())); // Local
                // .ENGLISH to force . separator instead of comma
                builder.append("}");
                final JSONObject customerPayment = new JSONObject(builder.toString());
                json.put("customer_payment", customerPayment);
            }

            if (mShippingAddress != null) {
                final JSONObject sajson = new JSONObject();
                sajson.put("recipient_name", mShippingAddress.getRecipientName());
                sajson.put("address_line_1", mShippingAddress.getLine1());
                sajson.put("address_line_2", mShippingAddress.getLine2());
                sajson.put("city", mShippingAddress.getCity());
                sajson.put("county_state", mShippingAddress.getStateOrCounty());
                sajson.put("postcode", mShippingAddress.getZipOrPostalCode());
                sajson.put("country_code", mShippingAddress.getCountry().iso3Code());
                json.put("shipping_address", sajson);
            }

            if (KiteSDK.DEBUG_PAYMENT_KEYS) {
                Log.d(LOG_TAG, "Create order JSON:\n" + json.toString());
            }

            return json;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Set<String> getCurrenciesSupported() {

        Set<String> supported = null;
        for (Job job : mJobs) {
            final Set<String> supported2 = job.getCurrenciesSupported();
            if (supported == null) {
                supported = supported2;
            } else {
                supported.retainAll(supported2);
            }
        }

        return supported == null ? Collections.EMPTY_SET : supported;
    }

    /*****************************************************
     *
     * Returns a string description of the items. This is
     * primarily used as the item description on the order
     * history screen.
     *
     *****************************************************/
    public String getItemsDescription() {

        final StringBuilder descriptionBuilder = new StringBuilder();

        String separatorString = "";

        // Go through each of the jobs

        for (Job job : mJobs) {
            final Product product = job.getProduct();

            final String itemString = product.getDisplayLabel();

            descriptionBuilder
                    .append(separatorString)
                    .append(itemString);

            separatorString = ", ";
        }

        return descriptionBuilder.toString();
    }

    /*****************************************************
     *
     * Returns this order as a JSON basket.
     *
     *    [
     *     {
     *     "country_code": "GBR",
     *     "job_id": "48CD1DFA-254B-4FF9-A81C-1FB7A854C509",
     *     "quantity": 1,
     *     "template_id":"i6splus_case"
     *     }
     *   ]
     *
     *****************************************************/
    public JSONArray asBasketJSONArray(String countryCode) {

        final JSONArray jsonArray = new JSONArray();

        final String separatorString = "";

        for (Job job : mJobs) {
            // Each job is repeated orderQuantity times

            final int orderQuantity = job.getOrderQuantity();

            for (int index = 0; index < orderQuantity; index++) {
                final JSONObject itemJSONObject = new JSONObject();

                try {
                    itemJSONObject.put(JSON_NAME_JOB_ID, job.getId());
                    itemJSONObject.put(JSON_NAME_QUANTITY, job.getQuantity());
                    itemJSONObject.put(JSON_NAME_TEMPLATE_ID, job.getProductId());
                    itemJSONObject.put(JSON_NAME_COUNTRY_CODE, countryCode);
                    itemJSONObject.put(JSON_NAME_SHIPPING_CLASS, job.getShippingClass());
                } catch (JSONException je) {
                    Log.e(LOG_TAG, "Unable to create basket item JSON", je);
                }

                jsonArray.put(itemJSONObject);
            }
        }

        return jsonArray;
    }

    List<UploadableImage> getImagesToUpload() {

        final List<UploadableImage> uploadableImageList = new ArrayList<>();
        for (Job job : mJobs) {
            for (UploadableImage uploadableImage : job.getImagesForUploading()) {
                if (uploadableImage != null && !uploadableImageList.contains(uploadableImage)) {
                    uploadableImageList.add(uploadableImage);
                }
            }
        }
        return uploadableImageList;
    }

    public int getTotalAssetsToUpload() {

        return getImagesToUpload().size();
    }

    public boolean isPrinted() {

        return mReceipt != null;
    }

    public Date getLastPrintSubmissionDate() {

        return mLastPrintSubmissionDate;
    }

    public Exception getLastPrintSubmissionError() {

        return mLastPrintSubmissionError;
    }

    public void setReceipt(String receipt) {

        this.mReceipt = receipt;
    }

    // Used for testing
    public void clearReceipt() {

        this.mReceipt = null;
    }

    public String getReceipt() {

        return this.mReceipt;
    }

    public Order addJob(Job job) {

        if (!(job instanceof ImagesJob || job instanceof PostcardJob || job instanceof GreetingCardJob)) {
            throw new IllegalArgumentException("Currently only support PrintsPrintJobs & PostcardPrintJob, if any further jobs " +
                    "classes are added support for them must be added to the Parcelable interface in particular readTypedList must work ;" +
                    ")");
        }

        mJobs.add(job);

        return this;
    }

    public void removeJob(Job job) {

        mJobs.remove(job);
    }

    /*****************************************************
     *
     * Returns a sanitised version of this order.
     *
     *****************************************************/
    public Order createSanitisedCopy() {
        // Create a new order, and copy over relevant details

        final Order sanitisedOrder = new Order();

        sanitisedOrder.setShippingAddress(getShippingAddress());
        sanitisedOrder.setNotificationEmail(getNotificationEmail());
        sanitisedOrder.setNotificationPhoneNumber(getNotificationPhoneNumber());
        sanitisedOrder.setUserData(getUserData());
        sanitisedOrder.setAdditionalParameters(getAdditionalParameters());
        sanitisedOrder.setPromoCode(getPromoCode());

        sanitisedOrder.setOrderPricing(getOrderPricing());
        sanitisedOrder.setProofOfPayment(getProofOfPayment());
        sanitisedOrder.setReceipt(getReceipt());

        return sanitisedOrder;
    }

    private boolean isAssetUploadInProgress() {
        // There may be a brief window where assetUploadReq == null whilst we asynchronously collect info about the assets
        // to upload. assetsToUpload will be non nil whilst this is happening.
        return mImagesToUpload != null || mAssetUploadReq != null;
    }

    public void preemptAssetUpload(Context context) {

        if (isAssetUploadInProgress() || mAssetUploadComplete) {
            return;
        }

        startAssetUpload(context);
    }

    private void startAssetUpload(final Context context) {

        if (isAssetUploadInProgress() || mAssetUploadComplete) {
            throw new IllegalStateException("Asset upload should not have previously been started");
        }

        // Call back with progress (even though we haven't made any yet), so the user gets
        // a dialog box. Otherwise there's a delay whilst any images are cropped, and it's
        // not obvious that anything's happening.

        if (mSubmissionListener != null) {
            mSubmissionListener.onProgress(this, 0, 0);
        }

        // Get a list of all the images that need uploading. This list will exclude any
        // blank images.
        mImagesToUpload = getImagesToUpload();

        // Crop any images where the the asset fragment is a sub-section of the original
        // asset.

        mImagesToCropCount = 0;

        for (UploadableImage uploadableImage : mImagesToUpload) {
            final AssetFragment assetFragment = uploadableImage.getAssetFragment();
            final Asset asset = uploadableImage.getAsset();

            // If this asset fragment is not full size then it needs to be cropped before
            // it can be uploaded.

            if (!assetFragment.isFullSize()) {
                mImagesToCropCount++;

                ImageAgent.with(context)
                        .transform(asset)
                        .byCroppingTo(assetFragment.getProportionalRectangle())
                        .intoNewAsset()
                        .thenNotify(new ImageCroppedCallback(context, uploadableImage));
            }
        }

        // If there are no images to crop - start the upload immediately
        if (mImagesToCropCount < 1) {
            startAssetUploadRequest(context);
        }
    }

    private void startAssetUploadRequest(Context context) {

        final boolean[] previousError = {false};
        final int[] outstandingLengthCallbacks = {mImagesToUpload.size()};

        mAssetUploadReq = new AssetUploadRequest(context);
        mAssetUploadReq.uploadAssets(context, mImagesToUpload, new MyAssetUploadRequestListener(context));
    }

    public void submitForPrinting(Context context, ISubmissionProgressListener listener) {

        this.mSubmissionListener = listener;

        if (mUserSubmittedForPrinting) {
            notifyIllegalStateError("An order has already been submitted for printing. An order submission must be cancelled before it " +
                    "can be submitted again.");

            return;
        }

        if (mPrintOrderReq != null) {
            notifyIllegalStateError("A print order request is already in progress.");

            return;
        }

        mLastPrintSubmissionDate = new Date();
        mUserSubmittedForPrinting = true;

        if (mAssetUploadComplete) {
            submitForPrinting(context);
        } else if (!isAssetUploadInProgress()) {
            startAssetUpload(context);
        }
    }

    private void submitForPrinting(Context context) {

        if (!mUserSubmittedForPrinting) {
            notifyIllegalStateError("The order cannot be submitted for printing if it has not been marked as submitted");

            return;
        }

        if (!mAssetUploadComplete || isAssetUploadInProgress()) {
            notifyIllegalStateError("The order should not be submitted for priting until the asset upload has completed.");

            return;
        }

        // Step 2: Submit print order to the server. Print Job JSON can now reference real asset ids.
        mPrintOrderReq = new SubmitOrderRequest(this);
        mPrintOrderReq.submitForPrinting(context, new SubmitOrderRequest.IProgressListener() {
            @Override
            public void onSubmissionComplete(SubmitOrderRequest req, String orderId) {
                // The initial submission was successful, but note that this doesn't mean the order will
                // pass validation. It may fail subsequently when we are polling the order status. So remember
                // to clear the receipt / set the error if we find a problem later.

                setReceipt(orderId);

                mPrintOrderReq = null;

                mSubmissionListener.onSubmissionComplete(Order.this, orderId);
            }

            @Override
            public void onError(SubmitOrderRequest req, Exception error) {

                mUserSubmittedForPrinting = false;
                mLastPrintSubmissionError = error;

                mPrintOrderReq = null;

                notifyError(error);
            }
        });
    }

    public void cancelSubmissionOrPreemptedAssetUpload() {

        if (mAssetUploadReq != null) {
            mAssetUploadReq.cancelUpload();
            mAssetUploadReq = null;
        }

        if (mPrintOrderReq != null) {
            mPrintOrderReq.cancelSubmissionForPrinting();
            mPrintOrderReq = null;
        }

        mUserSubmittedForPrinting = false;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {

        p.writeValue(mShippingAddress);
        p.writeString(mProofOfPayment);
        //p.writeString( voucherCode );

        final String userDataString = mUserData == null ? null : mUserData.toString();
        p.writeString(userDataString);

        p.writeSerializable(mAdditionalParametersMap);

        p.writeInt(mJobs.size());

        for (Job job : mJobs) {
            if (job instanceof PostcardJob) {
                p.writeInt(JOB_TYPE_POSTCARD);
                job.writeToParcel(p, flags);
            } else if (job instanceof GreetingCardJob) {
                p.writeInt(JOB_TYPE_GREETING_CARD);
                job.writeToParcel(p, flags);
            } else if (job instanceof PhotobookJob) {
                p.writeInt(JOB_TYPE_PHOTOBOOK);
                job.writeToParcel(p, flags);
            } else {
                p.writeInt(JOB_TYPE_PRINTS);
                job.writeToParcel(p, flags);
            }
        }

        p.writeValue(mUserSubmittedForPrinting);
        p.writeValue(mAssetUploadComplete);
        p.writeValue(mLastPrintSubmissionDate);
        p.writeString(mReceipt);
        p.writeSerializable(mLastPrintSubmissionError);
        p.writeInt(mStorageIdentifier);
        p.writeString(mPromoCode);
        p.writeParcelable(mOrderPricing, flags);
        p.writeString(mStatusNotificationEmail);
        p.writeString(mStatusNotificationPhone);
    }

    /*
     * Promo code stuff
     */

    public void setPromoCode(String promoCode) {

        this.mPromoCode = promoCode;
    }

    public String getPromoCode() {

        return mPromoCode;
    }

    public void clearPromoCode() {

        this.mPromoCode = null;
        mOrderPricing = null;
    }

    public void setOrderPricing(OrderPricing orderPricing) {

        mOrderPricing = orderPricing;
    }

    public OrderPricing getOrderPricing() {

        return mOrderPricing;
    }

    /*****************************************************
     *
     * Notifies any listener of an error.
     *
     *****************************************************/
    private void notifyError(Exception exception) {

        if (mSubmissionListener != null) {
            mSubmissionListener.onError(this, exception);
        }
    }

    /*****************************************************
     *
     * Notifies any listener of an illegal state error.
     *
     *****************************************************/
    private void notifyIllegalStateError(String message) {

        notifyError(new IllegalStateException(message));
    }

    /*****************************************************
     *
     * Resets the order state with an error. This is likely
     * to have been returned from polling the order status.
     *
     *****************************************************/
    public void setError(Exception exception) {

        clearReceipt();

        mLastPrintSubmissionError = exception;
        mUserSubmittedForPrinting = false;
    }

    private class MyAssetUploadRequestListener implements AssetUploadRequest.IProgressListener {
        private Context mContext;

        MyAssetUploadRequestListener(Context context) {

            mContext = context;
        }

        @Override
        public void onProgress(AssetUploadRequest req, int totalAssetsUploaded, int totalAssetsToUpload, long bytesWritten, long
                totalAssetBytesWritten, long totalAssetBytesExpectedToWrite) {
            // Calculate the primary / secondary progress
            final int primaryProgressPercent = Math.round((float) totalAssetsUploaded * 100f / (float) totalAssetsToUpload);
            final int secondaryProgressPercent = Math.round((float) totalAssetBytesWritten * 100f / (float) totalAssetBytesExpectedToWrite);

            if (mUserSubmittedForPrinting) {
                mSubmissionListener.onProgress(Order.this, primaryProgressPercent, secondaryProgressPercent);
            }
        }

        @Override
        public void onUploadComplete(AssetUploadRequest req, List<UploadableImage> uploadedImages) {
            // We don't want to check that the number of upload assets matches the size
            // of the asset list, because some of them might be blank.

            // Check that all the assets scheduled to be uploaded, were

            for (UploadableImage uploadedImage : uploadedImages) {
                if (!mImagesToUpload.contains(uploadedImage)) {
                    Log.e(LOG_TAG, "Found image not in upload list: " + uploadedImage);

                    notifyIllegalStateError("An image has been uploaded that shouldn't have been");

                    return;
                }
            }

            // Make sure all job assets have asset ids & preview urls. We need to do this because
            // we optimize the asset upload to avoid uploading assets that are considered to have
            // duplicate contents.

            for (Job job : mJobs) {
                for (UploadableImage uploadedImage : uploadedImages) {
                    for (UploadableImage jobUploadableImage : job.getImagesForUploading()) {
                        if (uploadedImage != jobUploadableImage && uploadedImage.equals(jobUploadableImage)) {
                            jobUploadableImage.markAsUploaded(uploadedImage.getUploadedAssetId(), uploadedImage.getPreviewURL());
                        }
                    }
                }
            }

            // Sanity check all assets are uploaded

            for (Job job : mJobs) {
                for (UploadableImage uploadableImage : job.getImagesForUploading()) {
                    if (uploadableImage != null && !uploadableImage.hasBeenUploaded()) {
                        Log.e(LOG_TAG, "An image that should have been uploaded, hasn't been.");

                        notifyIllegalStateError("An image that should have been uploaded, hasn't been.");

                        return;
                    }
                }
            }

            mAssetUploadComplete = true;
            mImagesToUpload = null;
            mAssetUploadReq = null;
            if (mUserSubmittedForPrinting) {
                submitForPrinting(mContext);
            }
        }

        @Override
        public void onError(AssetUploadRequest req, Exception error) {

            mAssetUploadReq = null;
            mImagesToUpload = null;
            if (mUserSubmittedForPrinting) {
                mLastPrintSubmissionError = error;
                mUserSubmittedForPrinting = false; // allow the user to resubmit for printing
                mSubmissionListener.onError(Order.this, error);
            }
        }
    }

    public interface ISubmissionProgressListener {
        void onProgress(Order order, int primaryProgressPercent, int secondaryProgressPercent);

        void onSubmissionComplete(Order order, String orderId);

        void onError(Order order, Exception error);
    }

    private class ImageCroppedCallback implements ImageProcessingRequest.ICallback {
        private Context mContext;
        private UploadableImage mUploadableImage;

        ImageCroppedCallback(Context context, UploadableImage uploadableImage) {

            mContext = context;
            mUploadableImage = uploadableImage;
        }

        @Override
        public void ipcOnImageAvailable(Asset targetAsset) {
            // Replace the previous asset fragment with the entire area of the cropped asset
            mUploadableImage.setImage(targetAsset);

            mImagesToCropCount--;

            // If all the images have been cropped - start the upload
            if (mImagesToCropCount < 1) {
                startAssetUploadRequest(mContext);
            }
        }

        @Override
        public void ipcOnImageUnavailable() {

            mAssetUploadReq = null;
            mImagesToUpload = null;

            if (mUserSubmittedForPrinting) {
                mLastPrintSubmissionError = null;
                mUserSubmittedForPrinting = false; // allow the user to resubmit for printing
                mSubmissionListener.onError(Order.this, null);
            }
        }
    }
}
