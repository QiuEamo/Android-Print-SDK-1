/*****************************************************
 *
 * AShippingActivity.java
 *
 *
 * Modified MIT License
 *
 * Copyright (c) 2010-2016 Kite Tech Ltd. https://www.kite.ly
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

package ly.kite.checkout;

///// Import(s) /////

import android.content.Intent;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.HashMap;

import ly.kite.address.Address;
import ly.kite.journey.AKiteActivity;
import ly.kite.ordering.Order;

///// Class Declaration /////

/*****************************************************
 *
 * This class is the parent of shipping activities.
 *
 *****************************************************/
abstract public class AShippingActivity extends AKiteActivity {
    ////////// Static Constant(s) //////////

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "AShippingActivity";

    public static final String KEY_ORDER = "ly.kite.order";
    public static final String KEY_SHIPPING_ADDRESS = "ly.kite.shippingaddress";
    public static final String KEY_EMAIL = "ly.kite.email";
    public static final String KEY_PHONE = "ly.kite.phone";
    public static final String KEY_ADDITIONAL_PARAMETERS = "ly.kite.additionalparameters";

    ////////// Static Variable(s) //////////

    ////////// Member Variable(s) //////////

    ////////// Static Initialiser(s) //////////

    ////////// Static Method(s) //////////

    /*****************************************************
     *
     * Adds a shipping address as an extra to the intent.
     *
     *****************************************************/
    public static void addShippingAddress(Address shippingAddress, Intent intent) {

        if (shippingAddress != null) {
            intent.putExtra(KEY_SHIPPING_ADDRESS, (Parcelable) shippingAddress);
        }
    }

    /*****************************************************
     *
     * Adds an email address as an extra to the intent.
     *
     *****************************************************/
    public static void addEmail(String email, Intent intent) {

        if (email != null) {
            intent.putExtra(KEY_EMAIL, email);
        }
    }

    /*****************************************************
     *
     * Adds the order and contact details as extras to the
     * intent.
     *
     *****************************************************/
    public static void addExtras(Order order, Intent intent) {

        if (order != null) {
            // We need to pass the order to the activity for analytics
            intent.putExtra(KEY_ORDER, order);

            // Put any shipping address, email, and phone number from the order into the intent.

            addShippingAddress(order.getShippingAddress(), intent);

            JSONObject userData = order.getUserData();

            if (userData != null) {
                addEmail(userData.optString("email"), intent);
                intent.putExtra(KEY_PHONE, userData.optString("phone"));
            }

            // Add any additional parameters

            HashMap<String, String> additionalParametersMap = order.getAdditionalParameters();

            if (additionalParametersMap != null) {
                intent.putExtra(KEY_ADDITIONAL_PARAMETERS, additionalParametersMap);
            }
        }
    }

    /*****************************************************
     *
     * Returns an order from the intent.
     *
     *****************************************************/
    public static Order getOrder(Intent intent) {

        if (intent == null) {
            return null;
        }

        return intent.getParcelableExtra(KEY_ORDER);
    }

    /*****************************************************
     *
     * Returns the shipping address from an intent.
     *
     *****************************************************/
    public static Address getShippingAddress(Intent data) {

        return data.getParcelableExtra(KEY_SHIPPING_ADDRESS);
    }

    /*****************************************************
     *
     * Returns the email from an intent.
     *
     *****************************************************/
    public static String getEmail(Intent data) {

        return data.getStringExtra(KEY_EMAIL);
    }

    /*****************************************************
     *
     * Returns the phone number from an intent.
     *
     *****************************************************/
    public static String getPhone(Intent data) {

        return data.getStringExtra(KEY_PHONE);
    }

    /*****************************************************
     *
     * Returns the additional parameters from an intent.
     *
     *****************************************************/
    public static HashMap<String, String> getAdditionalParameters(Intent data) {

        return (HashMap<String, String>) data.getSerializableExtra(KEY_ADDITIONAL_PARAMETERS);
    }

    /*****************************************************
     *
     * Sets an additional parameter in an intent.
     *
     *****************************************************/
    public static void setAdditionalParameter(String name, String value, Intent data) {

        HashMap<String, String> additionalParameterMap = getAdditionalParameters(data);

        if (additionalParameterMap == null) {
            additionalParameterMap = new HashMap<>();

            data.putExtra(KEY_ADDITIONAL_PARAMETERS, additionalParameterMap);
        }

        additionalParameterMap.put(name, value);
    }

    ////////// Constructor(s) //////////

    ////////// Method(s) //////////

    /*****************************************************
     *
     * ...
     *
     *****************************************************/

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * ...
     *
     *****************************************************/

}

