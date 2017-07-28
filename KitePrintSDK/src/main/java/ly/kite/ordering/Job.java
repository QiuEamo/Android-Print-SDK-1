package ly.kite.ordering;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ly.kite.address.Address;
import ly.kite.catalogue.Product;
import ly.kite.util.Asset;
import ly.kite.util.AssetFragment;
import ly.kite.util.UploadableImage;

/**
 * Created by deonbotha on 09/02/2014.
 */

/*****************************************************
 *
 * This class represents a job: a request for a single
 * product. Orders may contain any number of jobs.
 *
 * Note that the naming isn't consistent with other abstract
 * classes used in the SDK (i.e. it is not called "AJob");
 * this is intentional. Since it is developer-facing, they are
 * probably more comfortable with this naming.
 *
 *****************************************************/
public abstract class Job implements Parcelable {
    protected static final String JSON_NAME_POLAROID_TEXT = "polaroid_text";

    private static final String JSON_NAME_OPTIONS = "options";

    private long mId;  // The id from the basket database
    private transient Product mProduct;  // Stop the product being serialised
    private int mOrderQuantity;
    private final HashMap<String, String> mOptionsMap;
    private int mShippingClass;

    ////////// Constructor(s) //////////

    protected Job(long id, Product product, int orderQuantity, HashMap<String, String> optionsMap, int shippingClass) {

        mId = id;
        mProduct = product;
        mOrderQuantity = orderQuantity;
        mOptionsMap = optionsMap != null ? optionsMap : new HashMap<String, String>(0);
        mShippingClass = shippingClass;
    }

    protected Job(Parcel sourceParcel) {

        mId = sourceParcel.readLong();
        mProduct = Product.CREATOR.createFromParcel(sourceParcel);
        mOrderQuantity = sourceParcel.readInt();
        mOptionsMap = sourceParcel.readHashMap(HashMap.class.getClassLoader());
        mShippingClass = sourceParcel.readInt();
    }

    ////////// Abstract Method(s) //////////

    public abstract BigDecimal getCost(String currencyCode);

    public abstract Set<String> getCurrenciesSupported();

    public abstract int getQuantity();

    abstract List<UploadableImage> getImagesForUploading();

    abstract JSONObject getJSONRepresentation();

    ////////// Static Method(s) //////////

    /*****************************************************
     *
     * Creates a print job.
     *
     *****************************************************/

    public static Job createPrintJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, List<?> imageList, boolean
            nullImagesAreBlank, int shippingClass) {

        return new ImagesJob(product, orderQuantity, optionsMap, imageList, nullImagesAreBlank, shippingClass);
    }

    public static Job createPrintJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, List<?> imageList, int
            shippingClass) {

        return new ImagesJob(product, orderQuantity, optionsMap, imageList, false, shippingClass);
    }

    public static Job createPrintJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, List<?> imageList, int
            offset, int length, boolean nullImagesAreBlank, int shippingClass) {

        return new ImagesJob(product, orderQuantity, optionsMap, imageList, offset, length, nullImagesAreBlank, shippingClass);
    }

    public static Job createPrintJob(Product product, HashMap<String, String> optionsMap, List<?> imageList, int shippingClass) {

        return new ImagesJob(product, 1, optionsMap, imageList, shippingClass);
    }

    public static Job createPrintJob(Product product, List<?> imageList, int shippingClass) {

        return new ImagesJob(product, 1, null, imageList, shippingClass);
    }

    public static Job createPrintJob(Product product, HashMap<String, String> optionsMap, Object image, int shippingClass) {

        final List<UploadableImage> singleImageSpecList = new ArrayList<>(1);

        singleImageSpecList.add(singleUploadableImageFrom(image));

        return new ImagesJob(product, 1, optionsMap, singleImageSpecList, shippingClass);
    }

    public static Job createPrintJob(Product product, Object image, int shippingClass) {

        return createPrintJob(product, null, image, shippingClass);
    }

    /*****************************************************
     *
     * Creates a photobook job.
     *
     *****************************************************/

    public static PhotobookJob createPhotobookJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, Object
            frontCoverImage, List<?> contentImageList, int shippingClass) {

        return new PhotobookJob(product, orderQuantity, optionsMap, frontCoverImage, contentImageList, shippingClass);
    }

    public static PhotobookJob createPhotobookJob(Product product, HashMap<String, String> optionsMap, Object frontCoverImage, List<?>
            contentImageList, int shippingClass) {

        return createPhotobookJob(product, 1, optionsMap, frontCoverImage, contentImageList, shippingClass);
    }

    public static PhotobookJob createPhotobookJob(Product product, Object frontCoverImage, List<?> contentImageList, int shippingClass) {

        return createPhotobookJob(product, 1, null, frontCoverImage, contentImageList, shippingClass);
    }

    /*****************************************************
     *
     * Creates a greeting card job.
     *
     *****************************************************/

    public static GreetingCardJob createGreetingCardJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, Object
            frontImage, Object backImage, Object insideLeftImage, Object insideRightImage, int shippingClass) {

        return new GreetingCardJob(product, orderQuantity, optionsMap, frontImage, backImage, insideLeftImage, insideRightImage,
                shippingClass);
    }

    public static GreetingCardJob createGreetingCardJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, Object
            frontImage, int shippingClass) {

        return new GreetingCardJob(product, orderQuantity, optionsMap, frontImage, null, null, null, shippingClass);
    }

    public static GreetingCardJob createGreetingCardJob(Product product, Object frontImage, Object backImage, Object insideLeftImage,
                                                        Object insideRightImage, int shippingClass) {

        return createGreetingCardJob(product, 1, null, frontImage, backImage, insideLeftImage, insideRightImage, shippingClass);
    }

    public static GreetingCardJob createGreetingCardJob(Product product, Object frontImage, int shippingClass) {

        return createGreetingCardJob(product, 1, null, frontImage, null, null, null, shippingClass);
    }

    /*****************************************************
     *
     * Creates a postcard job.
     *
     *****************************************************/

    public static Job createPostcardJob(Product product, int orderQuantity, HashMap<String, String> optionsMap, Asset frontImageAsset,
                                        Asset backImageAsset, String message, Address address, int shippingClass) {

        return new PostcardJob(product, orderQuantity, optionsMap, frontImageAsset, backImageAsset, message, address, shippingClass);
    }

    public static Job createPostcardJob(Product product, HashMap<String, String> optionMap, Asset frontImageAsset, String message,
                                        Address address, int shippingClass) {

        return createPostcardJob(product, 1, optionMap, frontImageAsset, null, message, address, shippingClass);
    }

    public static Job createPostcardJob(Product product, Asset frontImageAsset, String message, Address address, int shippingClass) {

        return createPostcardJob(product, 1, null, frontImageAsset, null, message, address, shippingClass);
    }

    public static Job createPostcardJob(Product product, Asset frontImageAsset, Asset backImageAsset, int shippingClass) {

        return createPostcardJob(product, 1, null, frontImageAsset, backImageAsset, null, null, shippingClass);
    }

    public static Job createPostcardJob(Product product, Asset frontImageAsset, Asset backImageAsset, String message, Address address,
                                        int shippingClass) {

        return createPostcardJob(product, 1, null, frontImageAsset, backImageAsset, message, address, shippingClass);
    }

    /*****************************************************
     *
     * Adds uploadable images and any border text to two lists.
     *
     *****************************************************/
    protected static void addUploadableImages(Object object, List<UploadableImage> uploadableImageList, List<String> borderTextList,
                                              boolean nullObjectsAreBlankPages) {

        if (object == null && nullObjectsAreBlankPages) {
            uploadableImageList.add(null);

            if (borderTextList != null) {
                borderTextList.add(null);
            }
        }

        // For ImageSpecs, we need to add as many images as the quantity, and keep
        // track of any border text.

        else if (object instanceof ImageSpec) {
            final ImageSpec imageSpec = (ImageSpec) object;

            final AssetFragment assetFragment = imageSpec.getAssetFragment();
            final String borderText = imageSpec.getBorderText();
            final int quantity = imageSpec.getQuantity();

            for (int index = 0; index < quantity; index++) {
                uploadableImageList.add(new UploadableImage(assetFragment));

                if (borderTextList != null) {
                    borderTextList.add(borderText);
                }
            }
        }

        // Anything else is just one image

        else {
            final UploadableImage uploadableImage = singleUploadableImageFrom(object);

            if (uploadableImage != null) {
                uploadableImageList.add(uploadableImage);

                if (borderTextList != null) {
                    borderTextList.add(null);
                }
            }
        }
    }

    /*****************************************************
     *
     * Returns an UploadableImage from an unknown image object.
     *
     *****************************************************/
    protected static UploadableImage singleUploadableImageFrom(Object object) {

        if (object == null) {
            return null;
        }

        if (object instanceof UploadableImage) {
            return (UploadableImage) object;
        }
        if (object instanceof ImageSpec) {
            return new UploadableImage(((ImageSpec) object).getAssetFragment());
        }
        if (object instanceof AssetFragment) {
            return new UploadableImage((AssetFragment) object);
        }
        if (object instanceof Asset) {
            return new UploadableImage((Asset) object);
        }

        throw new IllegalArgumentException("Unable to convert " + object + " into UploadableImage");
    }

    public long getId() {

        return mId;
    }

    public Product getProduct() {

        return mProduct;
    }

    public String getProductId() {

        return mProduct.getId();
    }

    public int getShippingClass() {

        return mShippingClass;
    }

    public void setOrderQuantity(int orderQuantity) {

        mOrderQuantity = orderQuantity;
    }

    public void setShippingClass(int shippingClass) {

        mShippingClass = shippingClass;
    }

    public int getOrderQuantity() {

        return mOrderQuantity;
    }

    /*****************************************************
     *
     * Adds the product option choices to the JSON.
     *
     * @return The options JSON object, so that the caller
     *         may add further options if desired.
     *
     *****************************************************/
    protected JSONObject addProductOptions(JSONObject jobJSONObject) throws JSONException {

        final JSONObject optionsJSONObject = new JSONObject();

        if (mOptionsMap != null) {
            for (String optionCode : mOptionsMap.keySet()) {
                optionsJSONObject.put(optionCode, mOptionsMap.get(optionCode));
            }
        }

        jobJSONObject.put(JSON_NAME_OPTIONS, optionsJSONObject);

        return optionsJSONObject;
    }

    /*****************************************************
     *
     * Returns the chosen options for the product.
     *
     *****************************************************/
    public HashMap<String, String> getProductOptions() {

        return mOptionsMap;
    }

    /*****************************************************
     *
     * Returns a product option.
     *
     *****************************************************/
    public String getProductOption(String parameter) {

        if (mOptionsMap != null) {
            return mOptionsMap.get(parameter);
        }

        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        parcel.writeLong(mId);

        mProduct.writeToParcel(parcel, flags);

        parcel.writeInt(mOrderQuantity);

        parcel.writeMap(mOptionsMap);

        parcel.writeInt(mShippingClass);
    }

    /*****************************************************
     *
     * Returns true if this job equals the supplied job.
     *
     *****************************************************/
    @Override
    public boolean equals(Object otherJobObject) {

        if (otherJobObject == null || (!(otherJobObject instanceof Job))) {
            return false;
        }

        final Job otherJob = (Job) otherJobObject;
        final Product otherProduct = otherJob.getProduct();
        final HashMap<String, String> otherOptionMap = otherJob.getProductOptions();

        if (!mProduct.getId().equals(otherProduct.getId())) {
            return false;
        }

        if ((mOptionsMap == null && otherOptionMap != null) ||
                (mOptionsMap != null && (otherOptionMap == null ||
                        mOptionsMap.size() != otherOptionMap.size()))) {
            return false;
        }

        for (String name : mOptionsMap.keySet()) {
            final String value = mOptionsMap.get(name);
            final String otherValue = otherOptionMap.get(name);

            if ((value == null && otherValue != null) ||
                    (value != null && !value.equals(otherValue))) {
                return false;
            }
        }

        return true;
    }

}
