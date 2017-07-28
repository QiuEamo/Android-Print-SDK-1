package ly.kite.journey.shipping;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import ly.kite.R;
import ly.kite.address.Country;
import ly.kite.catalogue.MultipleCurrencyAmounts;
import ly.kite.catalogue.Product;
import ly.kite.journey.basket.BasketActivity;

/**
 * Created by andrei on 03/07/2017.
 */

public class ShippingMethod extends BasketActivity {

    ArrayList<ShippingMethodItem> mList = new ArrayList<>();

    private ListView mListView;
    private ShippingAdapter mShippingAdapter;
    private ArrayList<Integer> mShippingClasses = new ArrayList<Integer>();
    private ArrayList<Integer> mShippingClassIndex = new ArrayList<Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_shipping_method);

        Product product;
        JSONObject shippingInfo = new JSONObject();

        int noOfItems = 0;
        int selectedShippingClass = 123;// value for N/A
        final Country shippingCountry;
        String jsonCountryCode;
        JSONArray shippingCosts = new JSONArray();
        JSONObject reggionMapping;

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            product = null;
        } else {
            shippingCountry = Country.getInstance(extras.getString("shippingCountry"));
            // get number of items from basket
            noOfItems = extras.getInt("noOfItems");

            // current index number for mShippingClassIndex
            int k = 0;
            for (int i = 0; i < noOfItems; i++) {
                product = (Product) extras.get("product" + i);

                try {

                    shippingInfo = new JSONObject(product.getShippingMethods());
                    reggionMapping = new JSONObject(product.getRegionMapping());
                    jsonCountryCode = reggionMapping.getString(shippingCountry.iso3Code());
                    shippingCosts = shippingInfo.getJSONObject(jsonCountryCode).getJSONArray("shipping_classes");
                    selectedShippingClass = extras.getInt("selectedShippingClass" + i);

                    if (selectedShippingClass == 123) {
                        selectedShippingClass = shippingCosts.getJSONObject(0).getInt("id");//get the first information from shipping
                        // json object
                    }

                    for (int j = -1; j < shippingCosts.length(); j++) {
                        k++;
                        mShippingClassIndex.add(j);
                    }

                    //select default shipping method (first/only method present) for each object
                    final JSONObject temp = shippingCosts.getJSONObject(0);

                    //set our default shippingClasses methods
                    mShippingClasses.add(i, temp.getInt("id"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mList.add(new ShippingMethodItem(product, extras.getInt("orderQuantity" + i), i, shippingCosts, shippingCountry
                        .iso3CurrencyCode(), selectedShippingClass));
            }
        }

        mListView = (ListView) findViewById(R.id.shipping_method_listview);

        mShippingAdapter = new ShippingAdapter(this, mList);

        mShippingAdapter.setShippingClasses(mShippingClasses);//get current shipping classes in case basket items have been removed

        mListView.setAdapter(mShippingAdapter);
    }

    @Override
    public void onBackPressed() {
        //when either back button is pressed , finish current activity , send selected shipping classes back to basket and open it
        finish();
        mShippingClasses = ShippingAdapter.getShippingClasses();

        final Intent intent = new Intent(this, BasketActivity.class);
        intent.putExtra("shippingClass", mShippingClasses);

        startActivity(intent);
    }

    public class ShippingMethodItem {

        Product mProduct;

        private int mItemQuantity;
        private int mNoOfOptions;//number of shipping classes for each element
        private JSONArray mShippingCosts;
        private String mCurrencyCode;
        private int mElementIndex;
        private int mSelectedShippingClass;

        public ShippingMethodItem(Product product, int quantity, int elementIndex, JSONArray shippingCosts, String currencyCode, int
                selectedShippingClass) {

            this.mProduct = product;
            this.mItemQuantity = quantity;
            this.mShippingCosts = shippingCosts;
            this.mCurrencyCode = currencyCode;

            this.mNoOfOptions = shippingCosts.length();
            this.mElementIndex = elementIndex;
            this.mSelectedShippingClass = selectedShippingClass;

        }

        public int getElementIndex() {

            return this.mElementIndex;
        }

        public Product getProduct() {

            return this.mProduct;
        }

        public int getItemQuantity() {

            return mItemQuantity;
        }

        public JSONArray getShippingCosts() {

            return this.mShippingCosts;
        }

        public int getNoOfOptions() {

            return this.mNoOfOptions;
        }

        public int getSelectedShippingClass() {

            return this.mSelectedShippingClass;
        }

        public void setSelectedShippingClass(int shippingClass) {

            this.mSelectedShippingClass = shippingClass;
        }

        public int getCurrentShippingMethods(int position) {

            return mShippingClassIndex.get(position);
        }

        public String getCurrencyCode() {

            return mCurrencyCode;
        }
    }

    public static class ShippingAdapter extends BaseAdapter {
        private static ArrayList<Integer> mShippingClasses = new ArrayList<Integer>();
        private MultipleCurrencyAmounts mShipCost;
        private Context mContext;
        private ArrayList<ShippingMethodItem> mItems;

        public ShippingAdapter(Context context, ArrayList<ShippingMethodItem> items) {

            this.mContext = context;
            this.mItems = items;
        }

        @Override
        public int getCount() {

            int count = 0;

            for (int i = 0; i < mItems.size(); i++) {
                count += mItems.get(i).getNoOfOptions() + 1;//+1 for including the title

            }
            return count;
        }

        @Override
        public Object getItem(int position) {

            for (int i = 0; i < mItems.size(); ++i) {
                final ShippingMethodItem item = mItems.get(i);
                if (position < item.getNoOfOptions() + 1) {
                    return item;
                } else {
                    position -= item.getNoOfOptions() + 1;
                }
            }

            throw new IllegalStateException("Will never get here");

        }

        @Override
        public long getItemId(int position) {

            return 0L;
        }

        @Override
        public int getViewTypeCount() {

            return 2;
        }

        @Override
        public int getItemViewType(int position) {

            for (int i = 0; i < mItems.size(); ++i) {
                final ShippingMethodItem item = mItems.get(i);
                if (position == 0) {
                    return 0;
                } else if (position < item.getNoOfOptions() + 1) {
                    return 1;
                } else {
                    position -= item.getNoOfOptions() + 1;
                }
            }
            throw new IllegalStateException("Will never get here");
        }

        public static ArrayList<Integer> getShippingClasses() {

            return mShippingClasses;
        }

        public void setShippingClasses(ArrayList<Integer> shippingClasses) {

            mShippingClasses = shippingClasses;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final ShippingMethodItem currentItem = (ShippingMethodItem) getItem(position);
            final int viewType = this.getItemViewType(position);
            final LayoutInflater vi;

            //if title element
            if (viewType == 0) {
                vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ViewHolder1 title;
                if (convertView == null) {
                    convertView = vi.inflate(R.layout.list_item_shipping_method_title, null);
                    title = new ViewHolder1(convertView);
                } else {
                    title = (ViewHolder1) convertView.getTag();
                }

                title.mItemName.setText(currentItem.getItemQuantity() + " x " + currentItem.getProduct().getDisplayLabel() + " (" +
                        currentItem.getProduct().getCategory() + ")");

                convertView.setTag(title);
                return convertView;

                //if shipping method element
            } else if (viewType == 1) {
                vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ViewHolder2 info;

                if (convertView == null) {
                    convertView = vi.inflate(R.layout.list_item_shipping_methods, null);
                    info = new ViewHolder2(convertView);
                } else {
                    info = (ViewHolder2) convertView.getTag();
                }

                try {
                    //get the information from the json object storred in shippingCost string
                    final JSONObject temp = currentItem.getShippingCosts().getJSONObject(currentItem.getCurrentShippingMethods(position));

                    final int id = temp.getInt("id");
                    final int index = currentItem.getElementIndex();

                    info.mShippingClassName.setText(temp.getString("mobile_shipping_name"));
                    info.mShippingTime.setText(temp.getString("min_delivery_time") + " - " + temp.getString("max_delivery_time") + " days");
                    final JSONArray costArray = temp.getJSONArray("costs");

                    //if current item is the default/selected shipping class
                    if (id == currentItem.getSelectedShippingClass()) {
                        //place icon to show that it is selected
                        info.mOptionSelected.setVisibility(View.VISIBLE);

                        //place the shipping class information into the dedicated list
                        mShippingClasses.set(index, id);

                    } else {
                        info.mOptionSelected.setVisibility(View.INVISIBLE);
                    }

                    mShipCost = new MultipleCurrencyAmounts(costArray);
                    final String shippingCost = mShipCost.getDisplayAmountWithFallbackMultipliedBy(Country.getInstance().iso3CurrencyCode
                            (), currentItem.getItemQuantity(), Locale.getDefault());

                    info.mShippingPrice.setText(shippingCost);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //when a shipping class is selected it becomes the default one for that item , and is storred in the
                            // dedicated list
                            info.mOptionSelected.setVisibility(View.VISIBLE);
                            currentItem.setSelectedShippingClass(id);
                            notifyDataSetChanged();
                            mShippingClasses.set(currentItem.getElementIndex(), id);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                convertView.setTag(info);
                return convertView;
            } else {
                return null;
            }

        }

        //ViewHolder1 inner class
        private class ViewHolder1 {
            TextView mItemName;

            public ViewHolder1(View view) {

                mItemName = (TextView) view.findViewById(R.id.nameText0);
            }
        }

        //ViewHolder2 inner class
        private class ViewHolder2 {
            TextView mShippingClassName;
            TextView mShippingTime;
            TextView mShippingPrice;
            ImageView mOptionSelected;

            public ViewHolder2(View view) {

                mShippingClassName = (TextView) view.findViewById(R.id.shipping_method_class);
                mShippingTime = (TextView) view.findViewById(R.id.shipping_method_time);
                mShippingPrice = (TextView) view.findViewById(R.id.shipping_method_price);
                mOptionSelected = (ImageView) view.findViewById(R.id.shipping_method_selected);
                mOptionSelected.setImageResource(R.drawable.tick);
            }
        }

    }

}
