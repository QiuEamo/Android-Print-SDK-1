package ly.kite.payment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.paypal.android.sdk.payments.PayPalPayment;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ly.kite.KiteSDK;
import ly.kite.KiteSDKException;
import ly.kite.address.Address;
import ly.kite.address.Country;

/**
 * Created by deonbotha on 16/02/2014.
 */
public class PayPalCard implements Serializable {

    private static final String LOG_TAG = "PayPalCard";

    private static final String PERSISTED_LUC_FILENAME = "luc";

    public static enum CardType {
        VISA("visa"),
        MASTERCARD("mastercard"),
        DISCOVER("discover"),
        AMEX("amex"),
        UNSUPPORTED("unsupported");

        private final String mPaypalIdentifier;

        CardType(String paypalIdentifier) {

            this.mPaypalIdentifier = paypalIdentifier;
        }

        public static CardType getCardType(io.card.payment.CardType type) {

            switch (type) {
                case AMEX:
                    return AMEX;
                case MASTERCARD:
                    return MASTERCARD;
                case DISCOVER:
                    return DISCOVER;
                case VISA:
                    return VISA;
                default:
                    return UNSUPPORTED;
            }
        }
    }

    private static final long serialVersionUID = 0L;
    private String mNumber;
    private String mNumberMasked;
    private CardType mCardType;
    private int mExpireMonth;
    private int mExpireYear;
    private String mCvv2;
    private String mFirstName;
    private String mLastName;
    private String mVaultId;
    private Date mVaultExpireDate;

    public PayPalCard() {

    }

    public PayPalCard(CardType type, String number, int expireMonth, int expireYear, String cvv2) {

        this.mCardType = type;
        this.mNumber = number;
        this.mExpireMonth = expireMonth;
        setExpireYear(expireYear);
        this.mCvv2 = cvv2;
    }

    public String getNumber() {

        return mNumber;
    }

    public String getNumberMasked() {

        return mNumberMasked;
    }

    public String getLastFour() {

        if (mNumber != null && mNumber.length() == 16) {
            return mNumber.substring(mNumber.length() - 4);
        } else if (mNumberMasked != null) {
            return mNumberMasked.substring(mNumberMasked.length() - Math.min(4, mNumberMasked.length()));
        }

        return null;
    }

    public CardType getCardType() {

        return mCardType;
    }

    public int getExpireMonth() {

        return mExpireMonth;
    }

    public int getExpireYear() {

        return mExpireYear;
    }

    public String getCvv2() {

        return mCvv2;
    }

    public void setNumber(String number) {

        this.mNumber = number;
    }

    public void setCardType(CardType cardType) {

        this.mCardType = cardType;
    }

    public void setExpireMonth(int expireMonth) {

        if (expireMonth < 1 || expireMonth > 12) {
            throw new IllegalArgumentException("Expire month must be in range of 1-12 incusive");
        }
        this.mExpireMonth = expireMonth;
    }

    public void setExpireYear(int expireYear) {

        if (expireYear <= 99) {
            expireYear += 2000;
        }

        this.mExpireYear = expireYear;
    }

    public void setCvv2(String cvv2) {

        this.mCvv2 = cvv2;
    }

    private void getAccessToken(final KiteSDK kiteSDK, final AccessTokenListener listener) {

        final AsyncTask<Void, Void, Object> requestTask = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... voids) {

                final HttpClient httpclient = new DefaultHttpClient();
                final HttpPost req = new HttpPost(String.format("https://%s/v1/oauth2/token", kiteSDK.getPayPalAPIHost()));
                req.setHeader("Content-Type", "application/x-www-form-urlencoded");
                try {
                    req.setEntity(new StringEntity("grant_type=client_credentials"));
                } catch (UnsupportedEncodingException e) {
                    return e;
                }

                req.setHeader("Authorization", "Basic " + kiteSDK.getPayPalAuthToken());

                try {
                    final HttpResponse response = httpclient.execute(req);
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    final StringBuilder builder = new StringBuilder();
                    for (String line = null; (line = reader.readLine()) != null; ) {
                        builder.append(line).append("\n");
                    }

                    final JSONTokener t = new JSONTokener(builder.toString());
                    final JSONObject json = new JSONObject(t);
                    final String accessToken = json.getString("access_token");
                    return accessToken;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object response) {

                if (response instanceof String) {
                    listener.onAccessToken((String) response);
                } else {
                    listener.onError((Exception) response);
                }
            }
        };

        requestTask.execute();
    }

    public void storeCard(final KiteSDK kiteSDK, final PayPalCardVaultStorageListener listener) {

        getAccessToken(kiteSDK, new AccessTokenListener() {
            @Override
            public void onAccessToken(final String accessToken) {

                final JSONObject storeJSON = new JSONObject();

                try {
                    storeJSON.put("number", mNumber);
                    storeJSON.put("type", mCardType.mPaypalIdentifier);
                    storeJSON.put("expire_month", "" + mExpireMonth);
                    storeJSON.put("expire_year", "" + mExpireYear);
                    storeJSON.put("cvv2", mCvv2);
                } catch (JSONException ex) {
                    listener.onError(PayPalCard.this, ex);
                    return;
                }

                final AsyncTask<Void, Void, Object> requestTask = new AsyncTask<Void, Void, Object>() {
                    @Override
                    protected Object doInBackground(Void... voids) {

                        final HttpClient httpclient = new DefaultHttpClient();
                        final HttpPost req = new HttpPost(String.format("https://%s/v1/vault/credit-card", kiteSDK.getPayPalAPIHost()));
                        req.setHeader("Content-Type", "application/json");
                        req.setHeader("Accept-Language", "en");
                        try {
                            req.setEntity(new StringEntity(storeJSON.toString()));
                        } catch (UnsupportedEncodingException e) {
                            return e;
                        }

                        req.setHeader("Authorization", "Bearer " + accessToken);

                        try {
                            final HttpResponse response = httpclient.execute(req);
                            final BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                            final StringBuilder builder = new StringBuilder();
                            for (String line = null; (line = reader.readLine()) != null; ) {
                                builder.append(line).append("\n");
                            }

                            final JSONTokener t = new JSONTokener(builder.toString());
                            final JSONObject json = new JSONObject(t);
                            final int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode <= 299) {
                                final VaultStoreResponse storageResponse = new VaultStoreResponse();
                                storageResponse.mNumber = json.getString("number");
                                storageResponse.mVaultId = json.getString("id");
                                final String vaultExpireDateStr = json.getString("valid_until");
                                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.UK);

                                storageResponse.mVaultExpireDate = dateFormat.parse(vaultExpireDateStr.replaceAll("Z$", "+0000"));
                                return storageResponse;
                            } else {
                                String errorMessage = json.optString("message");
                                if (errorMessage == null) {
                                    errorMessage = "Failed to make the payment. Please check your internet connectivity and try again.";
                                }

                                return new KiteSDKException(errorMessage);
                            }
                        } catch (Exception e) {
                            return e;
                        }
                    }

                    @Override
                    protected void onPostExecute(Object response) {

                        if (response instanceof VaultStoreResponse) {
                            final VaultStoreResponse storageResponse = (VaultStoreResponse) response;
                            PayPalCard.this.mVaultId = storageResponse.mVaultId;
                            PayPalCard.this.mVaultExpireDate = storageResponse.mVaultExpireDate;
                            PayPalCard.this.mNumberMasked = storageResponse.mNumber;
                            listener.onStoreSuccess(PayPalCard.this);
                        } else {
                            listener.onError(PayPalCard.this, (Exception) response);
                        }
                    }
                };

                requestTask.execute();
            }

            @Override
            public void onError(Exception error) {

                listener.onError(PayPalCard.this, error);
            }
        });
    }

    private JSONObject createAuthorisationJSON(BigDecimal amount, String currencyCode, String description, Address shippingAddress)
            throws JSONException {

        final JSONObject fundingInstrument = new JSONObject();
        if (mNumber != null) {
            // take payment directly using full card number
            final JSONObject cc = new JSONObject();
            fundingInstrument.put("credit_card", cc);
            cc.put("number", mNumber);
            cc.put("type", mCardType.mPaypalIdentifier);
            cc.put("expire_month", "" + mExpireMonth);
            cc.put("expire_year", "" + mExpireYear);
            cc.put("cvv2", mCvv2);
        } else {
            final JSONObject token = new JSONObject();
            fundingInstrument.put("credit_card_token", token);
            token.put("credit_card_id", mVaultId);
        }

        final JSONObject payment = new JSONObject();

        // The intent is authorise; the payment is actually made by the server
        payment.put("intent", PayPalPayment.PAYMENT_INTENT_AUTHORIZE);

        final JSONObject payer = new JSONObject();
        payment.put("payer", payer);
        payer.put("payment_method", "credit_card");
        final JSONArray fundingInstruments = new JSONArray();
        payer.put("funding_instruments", fundingInstruments);
        fundingInstruments.put(fundingInstrument);

        final JSONObject transaction = new JSONObject();

        transaction.put("description", description);

        final JSONObject jsonObjectAmount = new JSONObject();

        // Local.ENGLISH to force . separator instead
        jsonObjectAmount.put("total", String.format(Locale.ENGLISH, "%.2f", amount.floatValue()));
        // of comma
        jsonObjectAmount.put("currency", currencyCode);

        transaction.put("amount", jsonObjectAmount);

        // Create an item list that contains the shipping address
        if (shippingAddress != null) {
            final JSONObject shippingAddressJSONObject = new JSONObject();

            final String recipientName = shippingAddress.getRecipientName();
            final String line1 = shippingAddress.getLine1();
            final String line2 = shippingAddress.getLine2();
            final String city = shippingAddress.getCity();
            final String stateOrCounty = shippingAddress.getStateOrCounty();
            final String zipOrPostalCode = shippingAddress.getZipOrPostalCode();
            final Country country = shippingAddress.getCountry();

            if (recipientName != null) {
                shippingAddressJSONObject.put("recipient_name", recipientName);
            }
            if (line1 != null) {
                shippingAddressJSONObject.put("line1", line1);
            }
            if (line2 != null) {
                shippingAddressJSONObject.put("line2", line2);
            }
            if (city != null) {
                shippingAddressJSONObject.put("city", city);
            }
            if (stateOrCounty != null) {
                shippingAddressJSONObject.put("state", stateOrCounty);
            }
            if (zipOrPostalCode != null) {
                shippingAddressJSONObject.put("postal_code", zipOrPostalCode);
            }
            if (country != null) {
                shippingAddressJSONObject.put("country_code", country.iso2Code().toUpperCase());
            }

            final JSONObject itemListJSONObject = new JSONObject();

            itemListJSONObject.put("shipping_address", shippingAddressJSONObject);

            transaction.put("item_list", itemListJSONObject);
        }

        final JSONArray transactions = new JSONArray();
        payment.put("transactions", transactions);
        transactions.put(transaction);

        return payment;
    }

    public void authoriseCard(final KiteSDK kiteSDK, final BigDecimal amount, final String currencyCode, final String description, final
        Address shippingAddress, final PayPalCardChargeListener listener) {

        getAccessToken(kiteSDK, new AccessTokenListener() {
            @Override
            public void onAccessToken(final String accessToken) {

                JSONObject paymentJSON = null;
                try {
                    paymentJSON = createAuthorisationJSON(amount, currencyCode, description, shippingAddress);
                } catch (JSONException ex) {
                    listener.onError(PayPalCard.this, ex);
                    return;
                }

                final AsyncTask<JSONObject, Void, Object> requestTask = new AsyncTask<JSONObject, Void, Object>() {
                    @Override
                    protected Object doInBackground(JSONObject... jsons) {

                        final JSONObject paymentJSON = jsons[0];

                        final HttpClient httpclient = new DefaultHttpClient();
                        final HttpPost req = new HttpPost(String.format("https://%s/v1/payments/payment", kiteSDK.getPayPalAPIHost()));
                        req.setHeader("Content-Type", "application/json");
                        req.setHeader("Accept-Language", "en");
                        try {
                            req.setEntity(new StringEntity(paymentJSON.toString()));
                        } catch (UnsupportedEncodingException e) {
                            return e;
                        }

                        req.setHeader("Authorization", "Bearer " + accessToken);

                        try {
                            final HttpResponse response = httpclient.execute(req);
                            final BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                            final StringBuilder builder = new StringBuilder();
                            for (String line = null; (line = reader.readLine()) != null; ) {
                                builder.append(line).append("\n");
                            }

                            final JSONTokener t = new JSONTokener(builder.toString());
                            final JSONObject json = new JSONObject(t);
                            final int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode <= 299) {
                                final String paymentId = json.getString("id");
                                final String paymentState = json.getString("state");
                                if (!paymentState.equalsIgnoreCase("approved")) {
                                    return new KiteSDKException("Your payment was not approved. Please try again.");
                                }

                                return paymentId;
                            } else {
                                Log.e(LOG_TAG, "Invalid status code for response: " + json.toString());

                                String errorMessage = json.optString("message");
                                if (errorMessage == null) {
                                    errorMessage = "Failed to make the payment. Please check your internet connectivity and try again.";
                                }

                                return new KiteSDKException(errorMessage);
                            }
                        } catch (Exception e) {
                            return e;
                        }
                    }

                    @Override
                    protected void onPostExecute(Object response) {

                        if (response instanceof String) {
                            listener.onChargeSuccess(PayPalCard.this, (String) response);
                        } else {
                            listener.onError(PayPalCard.this, (Exception) response);
                        }
                    }
                };

                requestTask.execute(paymentJSON);
            }

            @Override
            public void onError(Exception error) {

                listener.onError(PayPalCard.this, error);
            }
        });
    }

    public void authoriseCard(final KiteSDK kiteSDK, final BigDecimal amount, final String currencyCode, final String description, final
        PayPalCardChargeListener listener) {

        authoriseCard(kiteSDK, amount, currencyCode, description, null, listener);
    }

    public boolean isStoredInVault() {

        return mVaultId != null;
    }

    public boolean hasVaultStorageExpired() {

        if (mVaultExpireDate == null) {
            return true;
        }

        return mVaultExpireDate.before(new Date());
    }

    private static interface AccessTokenListener {
        void onAccessToken(String accessToken);

        void onError(Exception error);
    }

    private static class VaultStoreResponse {
        String mNumber;
        String mVaultId;
        Date mVaultExpireDate;
    }

    /*
     * Last used card persistence
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        out.writeObject(mNumberMasked);
        out.writeInt(mCardType.ordinal());
        out.writeInt(mExpireMonth);
        out.writeInt(mExpireYear);
        out.writeObject(mFirstName);
        out.writeObject(mLastName);
        out.writeObject(mVaultId);
        out.writeObject(mVaultExpireDate);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        mNumberMasked = (String) in.readObject();
        mCardType = CardType.values()[in.readInt()];
        mExpireMonth = in.readInt();
        mExpireYear = in.readInt();
        mFirstName = (String) in.readObject();
        mLastName = (String) in.readObject();
        mVaultId = (String) in.readObject();
        mVaultExpireDate = (Date) in.readObject();
    }

    public static PayPalCard getLastUsedCard(Context c) {

        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new BufferedInputStream(c.openFileInput(PERSISTED_LUC_FILENAME)));
            final PayPalCard luc = (PayPalCard) is.readObject();
            return luc;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (InvalidClassException ice) {
            // There is likely to have been some sort of change to the class, so reading a previously
            // serialised class hasn't worked. Serialisation is not such a good idea for stuff like this.

            // Ignore the error (and the previous card)
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
                Log.e(LOG_TAG, "Error closing the last payPal info" , ex);
            }
        }
    }

    public static void clearLastUsedCard(Context c) {

        persistLastUsedCardToDisk(c, null);
    }

    private static void persistLastUsedCardToDisk(Context c, PayPalCard card) {

        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new BufferedOutputStream(c.openFileOutput(PERSISTED_LUC_FILENAME, Context.MODE_PRIVATE)));
            os.writeObject(card);

        } catch (Exception ex) {
            // Ignore , we'll just lose this last used card
        } finally {
            try {
                os.close();
            } catch (Exception ex) {
                Log.e(LOG_TAG, "Error closing the last payment card info" , ex);
            }
        }
    }

    public void saveAsLastUsedCard(Context c) {

        persistLastUsedCardToDisk(c, this);
    }
}
