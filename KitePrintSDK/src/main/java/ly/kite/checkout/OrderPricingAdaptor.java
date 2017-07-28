/*****************************************************
 *
 * OrderPricingAdaptor.java
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

package ly.kite.checkout;

///// Import(s) /////

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ly.kite.KiteSDK;
import ly.kite.R;
import ly.kite.catalogue.MultipleCurrencyAmounts;
import ly.kite.catalogue.SingleCurrencyAmounts;
import ly.kite.pricing.OrderPricing;

///// Class Declaration /////

/*****************************************************
 *
 * An adaptor for the image sources.
 *
 *****************************************************/
public class OrderPricingAdaptor extends BaseAdapter {
    ////////// Static Constant(s) //////////

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "OrderPricingAdaptor";

    ////////// Static Variable(s) //////////

    ////////// Member Variable(s) //////////

    private Context mContext;
    private OrderPricing mPricing;

    private LayoutInflater mLayoutInflator;

    private ArrayList<Item> mItemList;

    ////////// Static Initialiser(s) //////////

    ////////// Constructor(s) //////////

    public OrderPricingAdaptor(Context context, OrderPricing pricing) {

        mContext = context;
        mPricing = pricing;

        mLayoutInflator = LayoutInflater.from(context);

        // Create the item list

        mItemList = new ArrayList<>();

        if (pricing == null) {
            return;
        }

        final Locale defaultLocale = Locale.getDefault();
        final String preferredCurrencyCode = KiteSDK.getInstance(mContext).getLockedCurrencyCode();

        ///// Line items

        final List<OrderPricing.LineItem> lineItemList = pricing.getLineItems();

        for (OrderPricing.LineItem lineItem : lineItemList) {
            final String description = lineItem.getDescription();

            final MultipleCurrencyAmounts itemCost = lineItem.getProductCost();

            final String itemCostString = itemCost.getDefaultDisplayAmountWithFallback();

            mItemList.add(new Item(description, itemCostString, false));
        }

        ///// Shipping

        final MultipleCurrencyAmounts shippingCost = pricing.getTotalShippingCost();
        final SingleCurrencyAmounts shippingCostInSingleCurrency;
        final String shippingCostString;

        if (shippingCost != null &&
                (shippingCostInSingleCurrency = shippingCost.getAmountsWithFallback(preferredCurrencyCode)) != null &&
                shippingCostInSingleCurrency.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            shippingCostString = shippingCostInSingleCurrency.getDisplayAmountForLocale(defaultLocale);
        } else {
            shippingCostString = mContext.getString(R.string.Free);
        }

        mItemList.add(new Item(mContext.getString(R.string.Shipping), shippingCostString, false));

        ///// Promo code

        final MultipleCurrencyAmounts promoDiscount = pricing.getPromoCodeDiscount();

        if (promoDiscount != null) {
            final SingleCurrencyAmounts promoDiscountInSingleCurrency = promoDiscount.getAmountsWithFallback(preferredCurrencyCode);

            if (promoDiscountInSingleCurrency != null &&
                    promoDiscountInSingleCurrency.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                mItemList.add(new Item(mContext.getString(R.string.Promotional_Discount), promoDiscountInSingleCurrency
                        .getDisplayAmountForLocale(defaultLocale), false));
            }
        }

        ///// Total

        final MultipleCurrencyAmounts totalCost = pricing.getTotalCost();

        if (totalCost != null) {
            final SingleCurrencyAmounts totalCostInSingleCurrency = totalCost.getAmountsWithFallback(preferredCurrencyCode);

            mItemList.add(new Item(mContext.getString(R.string.Total), totalCostInSingleCurrency.getDisplayAmountForLocale(defaultLocale)
                    , true));
        }

    }

    ////////// Static Method(s) //////////

    /*****************************************************
     *
     * Adds the pricing items as individual views to a linear
     * layout rather than a list view.
     *
     *****************************************************/
    public static void addItems(Context context, OrderPricing pricing, ViewGroup viewGroup) {

        new OrderPricingAdaptor(context, pricing).addItems(viewGroup);
    }

    ////////// BaseAdapter Method(s) //////////

    /*****************************************************
     *
     * Returns the number of product items.
     *
     *****************************************************/
    @Override
    public int getCount() {

        return mItemList.size();
    }

    /*****************************************************
     *
     * Returns the product item at the requested position.
     *
     *****************************************************/
    @Override
    public Object getItem(int position) {

        return mItemList.get(position);
    }

    /*****************************************************
     *
     * Returns an id for the product item at the requested
     * position.
     *
     *****************************************************/
    @Override
    public long getItemId(int position) {

        return 0;
    }

    /*****************************************************
     *
     * Returns the view for the product item at the requested
     * position.
     *
     *****************************************************/
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Either re-use the convert view, or create a new one.

        final Object tag;
        final View view;
        final ViewHolder viewHolder;

        if (convertView != null &&
                (tag = convertView.getTag()) != null &&
                (tag instanceof ViewHolder)) {
            view = convertView;
            viewHolder = (ViewHolder) tag;
        } else {
            view = mLayoutInflator.inflate(R.layout.list_item_order_pricing, parent, false);
            viewHolder = new ViewHolder(view);

            view.setTag(viewHolder);
        }

        // Set up the view

        final Item item = (Item) getItem(position);

        viewHolder.bind(item);

        return view;
    }

    ////////// BaseAdapter Method(s) //////////

    /*****************************************************
     *
     * Adds the pricing items to a view group, as child views.
     *
     *****************************************************/
    private void addItems(ViewGroup viewGroup) {

        for (int position = 0; position < getCount(); position++) {
            final View view = mLayoutInflator.inflate(R.layout.list_item_order_pricing, viewGroup, false);
            final ViewHolder viewHolder = new ViewHolder(view);

            final Item item = (Item) getItem(position);

            viewHolder.bind(item);

            viewGroup.addView(view);
        }
    }

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * A row item.
     *
     *****************************************************/
    private class Item {
        String mDescription;
        String mAmount;
        boolean mIsBold;

        Item(String description, String amount, boolean isBold) {

            this.mDescription = description;
            this.mAmount = amount;
            this.mIsBold = isBold;
        }
    }

    /*****************************************************
     *
     * References to views within the layout.
     *
     *****************************************************/
    private class ViewHolder {
        TextView mDescriptionTextView;
        TextView mAmountTextView;

        ViewHolder(View view) {

            this.mDescriptionTextView = (TextView) view.findViewById(R.id.description_text_view);
            this.mAmountTextView = (TextView) view.findViewById(R.id.amount_text_view);
        }

        void bind(Item item) {
            // Set the text
            this.mDescriptionTextView.setText(item.mDescription);
            this.mAmountTextView.setText(item.mAmount);

            // Change the style appropriately

            final int style = item.mIsBold ? Typeface.BOLD : Typeface.NORMAL;

            this.mDescriptionTextView.setTypeface(Typeface.create(this.mDescriptionTextView.getTypeface(), style));
            this.mAmountTextView.setTypeface(Typeface.create(this.mAmountTextView.getTypeface(), style));
        }
    }

}

