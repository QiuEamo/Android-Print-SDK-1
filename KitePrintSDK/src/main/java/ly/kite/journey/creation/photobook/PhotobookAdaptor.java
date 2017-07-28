/*****************************************************
 *
 * PhotobookAdaptor.java
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

package ly.kite.journey.creation.photobook;

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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import ly.kite.KiteSDK;
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
public class PhotobookAdaptor extends RecyclerView.Adapter {
    ////////// Static Constant(s) //////////

    public static final int FRONT_COVER_POSITION = 0;
    public static final int INSTRUCTIONS_POSITION = 1;
    public static final int CONTENT_START_POSITION = 2;

    public static final int FRONT_COVER_VIEW_TYPE = 0;
    public static final int INSTRUCTIONS_VIEW_TYPE = 1;
    public static final int CONTENT_VIEW_TYPE = 2;

    static final int COVER_SUMMARY_IMAGE_COUNT = 9;

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "PhotobookAdaptor";

    private static final int FRONT_COVER_ASSET_INDEX = 0;

    ////////// Static Variable(s) //////////

    ////////// Member Variable(s) //////////

    private Activity mActivity;
    private Product mProduct;
    private ArrayList<ImageSpec> mImageSpecArrayList;
    private IListener mListener;

    private LayoutInflater mLayoutInflator;

    private HashSet<CheckableImageContainerFrame> mVisibleCheckableImageSet;
    private SparseArray<CheckableImageContainerFrame> mVisibleCheckableImageArray;

    private boolean mInSelectionMode;
    private HashSet<Integer> mSelectedAssetIndexHashSet;

    private boolean mFrontCoverIsSummary;
    private int mFrontCoverPlaceableImageCount;

    private int mCurrentlyHighlightedAssetIndex;

    ////////// Static Initialiser(s) //////////

    ////////// Static Method(s) //////////

    ////////// Constructor(s) //////////

    PhotobookAdaptor(Activity activity, Product product, ArrayList<ImageSpec> imageSpecArrayList, IListener listener) {

        mActivity = activity;
        mProduct = product;
        mImageSpecArrayList = imageSpecArrayList;
        mListener = listener;

        mLayoutInflator = activity.getLayoutInflater();

        mVisibleCheckableImageSet = new HashSet<>();
        mVisibleCheckableImageArray = new SparseArray<>();

        mSelectedAssetIndexHashSet = new HashSet<>();

        if (mFrontCoverIsSummary = KiteSDK.getInstance(activity).getCustomiser().photobookFrontCoverIsSummary()) {
            mFrontCoverPlaceableImageCount = 0;
        } else {
            mFrontCoverPlaceableImageCount = 1;
        }
    }

    ////////// RecyclerView.Adapter Method(s) //////////

    /*****************************************************
     *
     * Returns the number of items.
     *
     *****************************************************/
    @Override
    public int getItemCount() {
        // The number of rows is the sum of the following:
        //   - Front cover
        //   - Instructions
        //   - Images per page / 2, rounded up

        return 2 + ((mProduct.getQuantityPerSheet() + 1) / 2);
    }

    /*****************************************************
     *
     * Returns the view type for the position.
     *
     *****************************************************/
    @Override
    public int getItemViewType(int position) {

        if (position == FRONT_COVER_POSITION) {
            return FRONT_COVER_VIEW_TYPE;
        } else if (position == INSTRUCTIONS_POSITION) {
            return INSTRUCTIONS_VIEW_TYPE;
        }

        return CONTENT_VIEW_TYPE;
    }

    /*****************************************************
     *
     * Creates a view holder for the supplied view type.
     *
     *****************************************************/
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == FRONT_COVER_VIEW_TYPE) {
            return new FrontCoverViewHolder(mLayoutInflator.inflate(R.layout.list_item_photobook_front_cover, parent, false));
        } else if (viewType == INSTRUCTIONS_VIEW_TYPE) {
            return new InstructionsViewHolder(mLayoutInflator.inflate(R.layout.list_item_photobook_instructions, parent, false));
        }

        return new ContentViewHolder(mLayoutInflator.inflate(R.layout.list_item_photobook_content, parent, false));
    }

    /*****************************************************
     *
     * Populates a view.
     *
     *****************************************************/
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof FrontCoverViewHolder) {
            bindFrontCover((FrontCoverViewHolder) viewHolder);
        } else if (viewHolder instanceof InstructionsViewHolder) {
            // Do nothing - the inflated view already has the correct text
        } else {
            bindContent((ContentViewHolder) viewHolder, position);
        }
    }

    ////////// Method(s) //////////

    /*****************************************************
     *
     * Binds the front cover view holder.
     *
     *****************************************************/
    private void bindFrontCover(FrontCoverViewHolder viewHolder) {

        if (mFrontCoverIsSummary) {
            ///// Summary front cover /////

            viewHolder.mImageIndex = -1;

            viewHolder.mCheckableImageContainerFrame.setVisibility(View.INVISIBLE);
            viewHolder.mAddImageView.setVisibility(View.INVISIBLE);
            viewHolder.mImageGridView.setVisibility(View.VISIBLE);

            // Populate the summary image grid
            for (int imageIndex = 0; imageIndex < COVER_SUMMARY_IMAGE_COUNT; imageIndex++) {
                final ImageView imageView = viewHolder.mImageGridViewHolder.mGridImageViewArray[imageIndex];

                imageView.setImageDrawable(null);

                final ImageSpec imageSpec = mImageSpecArrayList.get(imageIndex);

                if (imageSpec != null) {
                    final AssetFragment imageAssetFragment = imageSpec.getAssetFragment();

                    if (imageAssetFragment != null) {
                        ImageAgent.with(mActivity)
                                .load(imageAssetFragment)
                                .resizeForDimen(imageView, R.dimen.image_default_resize_size, R.dimen.image_default_resize_size)
                                .onlyScaleDown()
                                .reduceColourSpace()
                                .into(imageView, imageAssetFragment);
                    }
                }

            }
        } else {
            ///// Standard front cover /////

            viewHolder.mCheckableImageContainerFrame.setVisibility(View.VISIBLE);
            // The add image view visibility is set below
            viewHolder.mImageGridView.setVisibility(View.INVISIBLE);

            // If the holder is already bound - remove its reference
            if (viewHolder.mImageIndex >= 0) {
                mVisibleCheckableImageSet.remove(viewHolder.mCheckableImageContainerFrame);
                mVisibleCheckableImageArray.remove(viewHolder.mImageIndex);
            }

            viewHolder.mImageIndex = FRONT_COVER_ASSET_INDEX;

            mVisibleCheckableImageSet.add(viewHolder.mCheckableImageContainerFrame);
            mVisibleCheckableImageArray.put(viewHolder.mImageIndex, viewHolder.mCheckableImageContainerFrame);

            // We only display the add image icon if there is no assets and quantity for that position,
            // not just if there is no edited asset yet.

            final ImageSpec imageSpec = mImageSpecArrayList.get(FRONT_COVER_ASSET_INDEX);

            if (imageSpec != null) {
                viewHolder.mAddImageView.setVisibility(View.INVISIBLE);

                final AssetFragment assetFragment = imageSpec.getAssetFragment();

                if (mInSelectionMode) {
                    if (mSelectedAssetIndexHashSet.contains(FRONT_COVER_ASSET_INDEX)) {
                        viewHolder.mCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.CHECKED);
                    } else {
                        viewHolder.mCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_VISIBLE);
                    }
                } else {
                    viewHolder.mCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
                }

                if (assetFragment != null) {
                    viewHolder.mCheckableImageContainerFrame.clearForNewImage(assetFragment);

                    ImageAgent.with(mActivity)
                            .load(assetFragment)
                            .resizeForDimen(viewHolder.mCheckableImageContainerFrame, R.dimen.image_default_resize_size, R.dimen
                                    .image_default_resize_size)
                            .onlyScaleDown()
                            .reduceColourSpace()
                            .into(viewHolder.mCheckableImageContainerFrame, assetFragment);
                }

            } else {
                viewHolder.mAddImageView.setVisibility(View.VISIBLE);
                viewHolder.mCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
                viewHolder.mCheckableImageContainerFrame.clear();
            }
        }
    }

    /*****************************************************
     *
     * Binds the content view holder.
     *
     *****************************************************/
    private void bindContent(ContentViewHolder viewHolder, int position) {

        if (viewHolder.mLeftAssetIndex >= 0) {
            mVisibleCheckableImageSet.remove(viewHolder.mLeftCheckableImageContainerFrame);
            mVisibleCheckableImageArray.remove(viewHolder.mLeftAssetIndex);
        }

        if (viewHolder.mRightAssetIndex >= 0) {
            mVisibleCheckableImageSet.remove(viewHolder.mRightCheckableImageContainerFrame);
            mVisibleCheckableImageArray.remove(viewHolder.mRightAssetIndex);
        }

        // Calculate the indexes for the list view position
        final int leftIndex = mFrontCoverPlaceableImageCount + ((position - CONTENT_START_POSITION) * 2);
        final int rightIndex = leftIndex + 1;

        viewHolder.mLeftAssetIndex = leftIndex;

        mVisibleCheckableImageSet.add(viewHolder.mLeftCheckableImageContainerFrame);
        mVisibleCheckableImageArray.put(viewHolder.mLeftAssetIndex, viewHolder.mLeftCheckableImageContainerFrame);

        viewHolder.mLeftTextView.setText(String.format("%02d", leftIndex));

        final ImageSpec leftImageSpec = getImageSpecAt(leftIndex);

        if (leftImageSpec != null) {
            viewHolder.mLeftAddImageView.setVisibility(View.INVISIBLE);

            final AssetFragment leftAssetFragment = leftImageSpec.getAssetFragment();

            if (mInSelectionMode) {
                if (mSelectedAssetIndexHashSet.contains(viewHolder.mLeftAssetIndex)) {
                    viewHolder.mLeftCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.CHECKED);
                } else {
                    viewHolder.mLeftCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_VISIBLE);
                }
            } else {
                viewHolder.mLeftCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
            }

            if (leftAssetFragment != null) {
                viewHolder.mLeftCheckableImageContainerFrame.clearForNewImage(leftAssetFragment);

                //AssetHelper.requestImage( mActivity, leftEditedAsset, viewHolder.mLeftCheckableImageContainerFrame );
                ImageAgent.with(mActivity)
                        .load(leftAssetFragment)
                        .resizeForDimen(viewHolder.mLeftCheckableImageContainerFrame, R.dimen.image_default_resize_size, R.dimen
                                .image_default_resize_size)
                        .onlyScaleDown()
                        .reduceColourSpace()
                        .into(viewHolder.mLeftCheckableImageContainerFrame, leftAssetFragment);
            }
        } else {
            viewHolder.mLeftAddImageView.setVisibility(View.VISIBLE);
            viewHolder.mLeftCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
            viewHolder.mLeftCheckableImageContainerFrame.clear();
        }

        viewHolder.mRightAssetIndex = rightIndex;

        mVisibleCheckableImageSet.add(viewHolder.mRightCheckableImageContainerFrame);
        mVisibleCheckableImageArray.put(viewHolder.mRightAssetIndex, viewHolder.mRightCheckableImageContainerFrame);

        viewHolder.mRightTextView.setText(String.format("%02d", rightIndex));

        final ImageSpec rightImageSpec = getImageSpecAt(rightIndex);

        if (rightImageSpec != null) {
            viewHolder.mRightAddImageView.setVisibility(View.INVISIBLE);

            final AssetFragment rightAssetFragment = rightImageSpec.getAssetFragment();

            if (mInSelectionMode) {
                if (mSelectedAssetIndexHashSet.contains(viewHolder.mRightAssetIndex)) {
                    viewHolder.mRightCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.CHECKED);
                } else {
                    viewHolder.mRightCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_VISIBLE);
                }
            } else {
                viewHolder.mRightCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
            }

            if (rightAssetFragment != null) {
                viewHolder.mRightCheckableImageContainerFrame.clearForNewImage(rightAssetFragment);

                ImageAgent.with(mActivity)
                        .load(rightAssetFragment)
                        .resizeForDimen(viewHolder.mRightCheckableImageContainerFrame, R.dimen.image_default_resize_size, R.dimen
                                .image_default_resize_size)
                        .onlyScaleDown()
                        .reduceColourSpace()
                        .into(viewHolder.mRightCheckableImageContainerFrame, rightAssetFragment);
            }
        } else {
            viewHolder.mRightAddImageView.setVisibility(View.VISIBLE);
            viewHolder.mRightCheckableImageContainerFrame.setState(CheckableImageContainerFrame.State.UNCHECKED_INVISIBLE);
            viewHolder.mRightCheckableImageContainerFrame.clear();
        }
    }

    /*****************************************************
     *
     * Returns the asset for the asset index, or null
     * if it doesn't exist.
     *
     *****************************************************/
    private ImageSpec getImageSpecAt(int index) {

        if (index < 0 || index >= mImageSpecArrayList.size()) {
            return null;
        }

        return mImageSpecArrayList.get(index);
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
            final CheckableImageContainerFrame currentlyHighlightedCheckableImage =
                    mVisibleCheckableImageArray.get(mCurrentlyHighlightedAssetIndex, null);

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
     * Front cover view holder.
     *
     *****************************************************/
    private class FrontCoverViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        int mImageIndex;

        CheckableImageContainerFrame mCheckableImageContainerFrame;
        ImageView mAddImageView;
        View mImageGridView;
        ImageGridViewHolder mImageGridViewHolder;

        FrontCoverViewHolder(View view) {

            super(view);

            this.mImageIndex = -1;

            this.mCheckableImageContainerFrame = (CheckableImageContainerFrame) view.findViewById(R.id.checkable_image_container_frame);
            this.mAddImageView = (ImageView) view.findViewById(R.id.add_image_view);
            this.mImageGridView = view.findViewById(R.id.image_grid);

            this.mImageGridViewHolder = new ImageGridViewHolder(this.mImageGridView);

            this.mCheckableImageContainerFrame.setOnClickListener(this);
            this.mCheckableImageContainerFrame.setOnLongClickListener(this);
        }

        ////////// View.OnClickListener Method(s) //////////

        @Override
        public void onClick(View view) {

            if (!mFrontCoverIsSummary) {
                if (view == this.mCheckableImageContainerFrame) {
                    if (mInSelectionMode) {
                        final ImageSpec imageSpec = getImageSpecAt(this.mImageIndex);

                        if (imageSpec != null) {
                            if (!mSelectedAssetIndexHashSet.contains(this.mImageIndex)) {
                                mSelectedAssetIndexHashSet.add(this.mImageIndex);

                                this.mCheckableImageContainerFrame.setChecked(true);
                            } else {
                                mSelectedAssetIndexHashSet.remove(this.mImageIndex);

                                this.mCheckableImageContainerFrame.setChecked(false);
                            }

                            onSelectedImagesChanged();
                        } else {
                            rejectAddImage(this.mAddImageView);
                        }
                    } else {
                        mListener.onClickImage(this.mImageIndex, view);
                    }
                }
            }
        }

        ////////// View.OnLongClickListener Method(s) //////////

        @Override
        public boolean onLongClick(View view) {

            if (!mFrontCoverIsSummary) {
                if (!mInSelectionMode) {
                    if (view == this.mCheckableImageContainerFrame && getImageSpecAt(FRONT_COVER_ASSET_INDEX) != null) {
                        mListener.onLongClickImage(this.mImageIndex, this.mCheckableImageContainerFrame);

                        return true;
                    }
                }
            }

            return false;
        }

    }

    /*****************************************************
     *
     * Summary image grid view holder.
     *
     *****************************************************/
    private class ImageGridViewHolder {
        ImageView[] mGridImageViewArray;

        ImageGridViewHolder(View view) {

            this.mGridImageViewArray = new ImageView[COVER_SUMMARY_IMAGE_COUNT];

            if (view != null) {
                this.mGridImageViewArray[0] = (ImageView) view.findViewById(R.id.grid_image_1);
                this.mGridImageViewArray[1] = (ImageView) view.findViewById(R.id.grid_image_2);
                this.mGridImageViewArray[2] = (ImageView) view.findViewById(R.id.grid_image_3);
                this.mGridImageViewArray[3] = (ImageView) view.findViewById(R.id.grid_image_4);
                this.mGridImageViewArray[4] = (ImageView) view.findViewById(R.id.grid_image_5);
                this.mGridImageViewArray[5] = (ImageView) view.findViewById(R.id.grid_image_6);
                this.mGridImageViewArray[6] = (ImageView) view.findViewById(R.id.grid_image_7);
                this.mGridImageViewArray[7] = (ImageView) view.findViewById(R.id.grid_image_8);
                this.mGridImageViewArray[8] = (ImageView) view.findViewById(R.id.grid_image_9);
            }
        }
    }

    /*****************************************************
     *
     * Instructions view holder.
     *
     *****************************************************/
    private class InstructionsViewHolder extends RecyclerView.ViewHolder {
        InstructionsViewHolder(View view) {

            super(view);
        }
    }

    /*****************************************************
     *
     * Content view holder.
     *
     *****************************************************/
    private class ContentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        int mLeftAssetIndex;
        int mRightAssetIndex;

        CheckableImageContainerFrame mLeftCheckableImageContainerFrame;
        CheckableImageContainerFrame mRightCheckableImageContainerFrame;

        ImageView mLeftAddImageView;
        ImageView mRightAddImageView;

        TextView mLeftTextView;
        TextView mRightTextView;

        ContentViewHolder(View view) {

            super(view);

            this.mLeftAssetIndex = -1;
            this.mRightAssetIndex = -1;

            this.mLeftCheckableImageContainerFrame = (CheckableImageContainerFrame) view.findViewById(R.id
                    .left_checkable_image_container_frame);
            this.mRightCheckableImageContainerFrame = (CheckableImageContainerFrame) view.findViewById(R.id
                    .right_checkable_image_container_frame);

            this.mLeftAddImageView = (ImageView) view.findViewById(R.id.left_add_image_view);
            this.mRightAddImageView = (ImageView) view.findViewById(R.id.right_add_image_view);

            this.mLeftTextView = (TextView) view.findViewById(R.id.left_text_view);
            this.mRightTextView = (TextView) view.findViewById(R.id.right_text_view);

            mLeftCheckableImageContainerFrame.setOnClickListener(this);
            mLeftCheckableImageContainerFrame.setOnLongClickListener(this);

            mRightCheckableImageContainerFrame.setOnClickListener(this);
            mRightCheckableImageContainerFrame.setOnLongClickListener(this);
        }

        ////////// View.OnClickListener Method(s) //////////

        @Override
        public void onClick(View view) {

            if (view == this.mLeftCheckableImageContainerFrame) {
                if (mInSelectionMode) {
                    final ImageSpec leftImageSpec = getImageSpecAt(this.mLeftAssetIndex);

                    if (leftImageSpec != null) {
                        if (!mSelectedAssetIndexHashSet.contains(this.mLeftAssetIndex)) {
                            mSelectedAssetIndexHashSet.add(this.mLeftAssetIndex);

                            this.mLeftCheckableImageContainerFrame.setChecked(true);
                        } else {
                            mSelectedAssetIndexHashSet.remove(this.mLeftAssetIndex);

                            this.mLeftCheckableImageContainerFrame.setChecked(false);
                        }

                        onSelectedImagesChanged();
                    } else {
                        rejectAddImage(this.mLeftAddImageView);
                    }
                } else {
                    mListener.onClickImage(this.mLeftAssetIndex, view);
                }

                return;
            }

            if (view == this.mRightCheckableImageContainerFrame) {
                if (mInSelectionMode) {
                    final ImageSpec rightImageSpec = getImageSpecAt(this.mRightAssetIndex);

                    if (rightImageSpec != null) {
                        if (!mSelectedAssetIndexHashSet.contains(this.mRightAssetIndex)) {
                            mSelectedAssetIndexHashSet.add(this.mRightAssetIndex);

                            this.mRightCheckableImageContainerFrame.setChecked(true);
                        } else {
                            mSelectedAssetIndexHashSet.remove(this.mRightAssetIndex);

                            this.mRightCheckableImageContainerFrame.setChecked(false);
                        }

                        onSelectedImagesChanged();
                    } else {
                        rejectAddImage(this.mRightAddImageView);
                    }
                } else {
                    mListener.onClickImage(this.mRightAssetIndex, view);
                }

                return;
            }

        }

        ////////// View.OnLongClickListener Method(s) //////////

        @Override
        public boolean onLongClick(View view) {

            if (!mInSelectionMode) {
                if (view == this.mLeftCheckableImageContainerFrame) {
                    if (getImageSpecAt(this.mLeftAssetIndex) != null) {
                        mListener.onLongClickImage(this.mLeftAssetIndex, this.mLeftCheckableImageContainerFrame);

                        return true;
                    }
                } else if (view == this.mRightCheckableImageContainerFrame) {
                    if (getImageSpecAt(this.mRightAssetIndex) != null) {
                        mListener.onLongClickImage(this.mRightAssetIndex, this.mRightCheckableImageContainerFrame);

                        return true;
                    }
                }
            }

            return false;
        }

    }

}

