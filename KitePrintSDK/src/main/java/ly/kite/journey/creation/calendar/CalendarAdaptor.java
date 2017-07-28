/*****************************************************
 *
 * CalendarAdaptor.java
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

package ly.kite.journey.creation.calendar;

///// Import(s) /////

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import ly.kite.R;
import ly.kite.catalogue.Product;
import ly.kite.image.ImageAgent;
import ly.kite.ordering.ImageSpec;
import ly.kite.util.AssetFragment;
import ly.kite.widget.CheckableImageContainerFrame;

///// Class Declaration /////

/*****************************************************
 *
 * This is the adaptor for the photobook list view.
 *
 *****************************************************/
public class CalendarAdaptor extends RecyclerView.Adapter {
    ////////// Static Constant(s) //////////

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "CalendarAdaptor";

    ////////// Static Variable(s) //////////

    ////////// Member Variable(s) //////////

    private Activity mActivity;
    private Product mProduct;
    private ArrayList<ImageSpec> mImageSpecArrayList;
    private IListener mListener;

    private int mImagesPerMonth;
    private int mGridCountX;
    private int mGridCountY;

    private LayoutInflater mLayoutInflator;

    private HashSet<CheckableImageContainerFrame> mVisibleCheckableImageSet;
    private SparseArray<CheckableImageContainerFrame> mVisibleCheckableImageArray;

    private boolean mInSelectionMode;
    private HashSet<Integer> mSelectedAssetIndexHashSet;

    private int mCurrentlyHighlightedAssetIndex;

    ////////// Static Initialiser(s) //////////

    ////////// Static Method(s) //////////

    ////////// Constructor(s) //////////

    CalendarAdaptor(Activity activity, Product product, ArrayList<ImageSpec> imageSpecArrayList, IListener listener) {

        mActivity = activity;
        mProduct = product;
        mImageSpecArrayList = imageSpecArrayList;
        mListener = listener;

        mGridCountX = mProduct.getGridCountX();
        mGridCountY = mProduct.getGridCountY();
        mImagesPerMonth = mGridCountX * mGridCountY;

        mLayoutInflator = activity.getLayoutInflater();

        mVisibleCheckableImageSet = new HashSet<>();
        mVisibleCheckableImageArray = new SparseArray<>();

        mSelectedAssetIndexHashSet = new HashSet<>();
    }

    ////////// RecyclerView.Adapter Method(s) //////////

    /*****************************************************
     *
     * Returns the number of items.
     *
     *****************************************************/
    @Override
    public int getItemCount() {

        return CalendarFragment.MONTHS_PER_YEAR;
    }

    /*****************************************************
     *
     * Creates a view holder for the supplied view type.
     *
     *****************************************************/
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new PageViewHolder(mLayoutInflator.inflate(R.layout.item_calendar_page, parent, false));
    }

    /*****************************************************
     *
     * Populates a view.
     *
     *****************************************************/
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        final PageViewHolder pageViewHolder = (PageViewHolder) viewHolder;

        // Remove any previously visible items

        final int previousFirstImageSpecIndex = pageViewHolder.mFirstImageSpecIndex;

        if (previousFirstImageSpecIndex >= 0) {
            for (int imageIndex = 0; imageIndex < mImagesPerMonth; imageIndex++) {
                // We don't need to remove the checkable image container frame from the set because it's recycled and so always visible
                mVisibleCheckableImageArray.remove(previousFirstImageSpecIndex + imageIndex);
            }
        }

        pageViewHolder.mMonthIndex = position;
        pageViewHolder.mFirstImageSpecIndex = position * mImagesPerMonth;

        // Process each of the images

        for (int imageIndex = 0; imageIndex < mImagesPerMonth; imageIndex++) {
            final CheckableImageContainerFrame checkableImageContainerFrame
                    = pageViewHolder.mImageViewHolderArray[imageIndex].mCheckableImageContainerFrame;
            final ImageView addImageView = pageViewHolder.mImageViewHolderArray[imageIndex].mAddImageView;

            mVisibleCheckableImageSet.add(checkableImageContainerFrame);
            mVisibleCheckableImageArray.put(pageViewHolder.mFirstImageSpecIndex + imageIndex, checkableImageContainerFrame);

            // Get the matching image spec
            final ImageSpec imageSpec = getImageSpecAt(position, imageIndex);

            if (imageSpec != null) {
                addImageView.setVisibility(View.INVISIBLE);

                if (mInSelectionMode) {
                    if (mSelectedAssetIndexHashSet.contains(pageViewHolder.mFirstImageSpecIndex + imageIndex)) {
                        checkableImageContainerFrame.setState(CheckableImageContainerFrame.State.CHECKED);
                    } else {
                        checkableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_VISIBLE);
                    }
                } else {
                    checkableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
                }

                final AssetFragment assetFragment = imageSpec.getAssetFragment();

                if (assetFragment != null) {
                    checkableImageContainerFrame.clearForNewImage(assetFragment);

                    ImageAgent.with(mActivity)
                            .load(assetFragment)
                            .resizeForDimen(checkableImageContainerFrame, R.dimen.image_default_resize_size, R.dimen
                                    .image_default_resize_size)
                            .onlyScaleDown()
                            .reduceColourSpace()
                            .into(checkableImageContainerFrame, assetFragment);
                }
            } else {
                addImageView.setVisibility(View.VISIBLE);
                checkableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
                checkableImageContainerFrame.clear();
            }

        }

        // Set up the calendar asset

        pageViewHolder.mAssetImageView.setImageDrawable(null);

        final ArrayList<String> calendarImageURLString = mProduct.getCalendarImages();

        if (calendarImageURLString != null && calendarImageURLString.size() >= CalendarFragment.MONTHS_PER_YEAR) {
            Picasso.with(mActivity)
                    .load(calendarImageURLString.get(position))
                    .into(pageViewHolder.mAssetImageView);
        }
    }

    ////////// Method(s) //////////

    /*****************************************************
     *
     * Returns the asset for the asset index, or null
     * if it doesn't exist.
     *
     *****************************************************/
    private ImageSpec getImageSpecAt(int monthIndex, int imageIndex) {

        if (monthIndex < 0 || monthIndex >= CalendarFragment.MONTHS_PER_YEAR) {
            return null;
        }

        final int imageSpecIndex = (monthIndex * mImagesPerMonth) + imageIndex;

        if (imageSpecIndex >= 0 && imageSpecIndex < mImageSpecArrayList.size()) {
            return mImageSpecArrayList.get(imageSpecIndex);
        }

        return null;
    }

    /*****************************************************
     *
     * Sets the selection mode.
     *
     *****************************************************/
    public void setSelectionMode(boolean inSelectionMode) {

        if (inSelectionMode != mInSelectionMode) {
            mInSelectionMode = inSelectionMode;

            final CheckableImageContainerFrame.State newState;

            if (inSelectionMode) {
                mSelectedAssetIndexHashSet.clear();

                newState = CheckableImageContainerFrame.State.UNCHECKED_VISIBLE;
            } else {
                newState = CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE;
            }

            // Check all the visible check image containers to show their check circle

            final Iterator<CheckableImageContainerFrame> visibleCheckableImageIterator = mVisibleCheckableImageSet.iterator();

            while (visibleCheckableImageIterator.hasNext()) {
                final CheckableImageContainerFrame checkableImage = visibleCheckableImageIterator.next();

                checkableImage.setState(newState);
            }
        }
    }

    /*****************************************************
     *
     * Selects an image.
     *
     *****************************************************/
    public void selectImage(int imageIndex) {

        final ImageSpec imageSpec = mImageSpecArrayList.get(imageIndex);

        if (imageSpec != null) {
            mSelectedAssetIndexHashSet.add(imageIndex);

            // If the image for this asset is visible, set its state

            final CheckableImageContainerFrame visibleCheckableImage = mVisibleCheckableImageArray.get(imageIndex);

            if (visibleCheckableImage != null) {
                visibleCheckableImage.setState(CheckableImageContainerFrame.State.CHECKED);
            }

            onSelectedImagesChanged();
        }
    }

    /*****************************************************
     *
     * Called when the set of selected assets has changed.
     *
     *****************************************************/
    private void onSelectedImagesChanged() {

        mListener.onSelectedImagesChanged(mSelectedAssetIndexHashSet.size());
    }

    /*****************************************************
     *
     * Returns the selected edited assets.
     *
     *****************************************************/
    public HashSet<Integer> getSelectedAssets() {

        return mSelectedAssetIndexHashSet;
    }

    /*****************************************************
     *
     * Sets the currently highlighted asset image.
     *
     *****************************************************/
    public void setHighlightedAsset(int assetIndex) {

        if (assetIndex != mCurrentlyHighlightedAssetIndex) {
            clearHighlightedAsset();

            final CheckableImageContainerFrame newHighlightedCheckableImage = mVisibleCheckableImageArray.get(assetIndex);

            if (newHighlightedCheckableImage != null) {
                final Resources resources = mActivity.getResources();

                newHighlightedCheckableImage.setHighlightBorderSizePixels(resources.getDimensionPixelSize(R.dimen
                        .checkable_image_highlight_border_size));
                newHighlightedCheckableImage.setHighlightBorderColour(resources.getColor(R.color.photobook_target_image_highlight));
                newHighlightedCheckableImage.setHighlightBorderShowing(true);

                mCurrentlyHighlightedAssetIndex = assetIndex;
            }
        }
    }

    /*****************************************************
     *
     * Clears the currently highlighted asset image.
     *
     *****************************************************/
    public void clearHighlightedAsset() {

        if (mCurrentlyHighlightedAssetIndex >= 0) {
            final CheckableImageContainerFrame currentlyHighlightedCheckableImage = mVisibleCheckableImageArray.get
                    (mCurrentlyHighlightedAssetIndex, null);

            if (currentlyHighlightedCheckableImage != null) {
                currentlyHighlightedCheckableImage.setHighlightBorderShowing(false);
            }

            mCurrentlyHighlightedAssetIndex = -1;
        }
    }

    /*****************************************************
     *
     * Called when add image is clicked whilst in selection
     * mode. The action is rejected by animating the icon.
     *
     *****************************************************/
    void rejectAddImage(ImageView imageView) {
        // Get the animation set and start it
        final Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.reject_add_image);

        imageView.startAnimation(animation);
    }

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * An event listener.
     *
     *****************************************************/
    interface IListener {
        void onClickImage(int assetIndex, View view);

        void onLongClickImage(int assetIndex, View view);

        void onSelectedImagesChanged(int selectedImageCount);
    }

    /*****************************************************
     *
     * Content view holder.
     *
     *****************************************************/
    private class PageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        int mMonthIndex;
        int mFirstImageSpecIndex;

        LinearLayout mImageLayout;
        ImageView mAssetImageView;

        ImageViewHolder[] mImageViewHolderArray;

        PageViewHolder(View view) {

            super(view);

            this.mMonthIndex = -1;
            this.mFirstImageSpecIndex = -1;

            this.mImageLayout = (LinearLayout) view.findViewById(R.id.image_layout);
            this.mAssetImageView = (ImageView) view.findViewById(R.id.asset_image_view);

            // Set up the image layout

            this.mImageViewHolderArray = new ImageViewHolder[mImagesPerMonth];

            for (int y = 0; y < mGridCountY; y++) {
                final LinearLayout rowLayout = new LinearLayout(mActivity);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);

                for (int x = 0; x < mGridCountX; x++) {
                    final View imageView = mLayoutInflator.inflate(R.layout.item_calendar_image, rowLayout, false);

                    final ImageViewHolder imageViewHolder = new ImageViewHolder(imageView);

                    this.mImageViewHolderArray[(y * mGridCountX) + x] = imageViewHolder;

                    // Add the image to the current row
                    rowLayout.addView(imageView);

                    imageViewHolder.mCheckableImageContainerFrame.setOnClickListener(this);
                    imageViewHolder.mCheckableImageContainerFrame.setOnLongClickListener(this);
                }

                // Add the row to the image layout

                final LinearLayout.LayoutParams rowLayoutParams =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                this.mImageLayout.addView(rowLayout, rowLayoutParams);
            }

        }

        ////////// View.OnClickListener Method(s) //////////

        @Override
        public void onClick(View view) {
            // Work out which view was clicked

            for (int imageIndex = 0; imageIndex < mImagesPerMonth; imageIndex++) {
                final int assetIndex = this.mFirstImageSpecIndex + imageIndex;

                final CheckableImageContainerFrame checkableImageContainerFrame =
                        this.mImageViewHolderArray[imageIndex].mCheckableImageContainerFrame;
                final ImageView addImageView = this.mImageViewHolderArray[imageIndex].mAddImageView;

                if (view == checkableImageContainerFrame) {
                    if (mInSelectionMode) {
                        final ImageSpec imageSpec = getImageSpecAt(this.mMonthIndex, imageIndex);

                        if (imageSpec != null) {
                            if (!mSelectedAssetIndexHashSet.contains(assetIndex)) {
                                mSelectedAssetIndexHashSet.add(assetIndex);

                                checkableImageContainerFrame.setChecked(true);
                            } else {
                                mSelectedAssetIndexHashSet.remove(assetIndex);

                                checkableImageContainerFrame.setChecked(false);
                            }

                            onSelectedImagesChanged();
                        } else {
                            rejectAddImage(addImageView);
                        }
                    } else {
                        mListener.onClickImage(assetIndex, view);
                    }

                    return;
                }

            }

        }

        ////////// View.OnLongClickListener Method(s) //////////

        @Override
        public boolean onLongClick(View view) {
            // Work out which view was clicked

            for (int imageIndex = 0; imageIndex < mImagesPerMonth; imageIndex++) {
                final int assetIndex = this.mFirstImageSpecIndex + imageIndex;

                final CheckableImageContainerFrame checkableImageContainerFrame =
                        this.mImageViewHolderArray[imageIndex].mCheckableImageContainerFrame;

                if (!mInSelectionMode) {
                    if (view == checkableImageContainerFrame) {
                        if (getImageSpecAt(this.mMonthIndex, imageIndex) != null) {
                            mListener.onLongClickImage(assetIndex, checkableImageContainerFrame);

                            return true;
                        }
                    }
                }
            }

            return false;
        }

    }

    /*****************************************************
     *
     * Image view holder.
     *
     *****************************************************/
    private class ImageViewHolder {
        View mView;
        CheckableImageContainerFrame mCheckableImageContainerFrame;
        ImageView mAddImageView;

        ImageViewHolder(View view) {

            this.mView = view;
            this.mCheckableImageContainerFrame = (CheckableImageContainerFrame) view.findViewById(R.id.checkable_image_container_frame);
            this.mAddImageView = (ImageView) view.findViewById(R.id.add_image_view);
        }
    }

}

