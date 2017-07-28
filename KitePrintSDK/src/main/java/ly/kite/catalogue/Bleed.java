/*****************************************************
 *
 * Bleed.java
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

package ly.kite.catalogue;

///// Import(s) /////

import android.os.Parcel;
import android.os.Parcelable;

///// Class Declaration /////

/*****************************************************
 *
 * This class represents a bleed.
 *
 *****************************************************/
public class Bleed implements Parcelable {

    ////////// Static Variable(s) //////////

    public static final Parcelable.Creator<Bleed> CREATOR = new Parcelable.Creator<Bleed>() {
        public Bleed createFromParcel(Parcel sourceParcel) {

            return new Bleed(sourceParcel);
        }

        public Bleed[] newArray(int size) {

            return new Bleed[size];
        }
    };

    ////////// Static Constant(s) //////////

    @SuppressWarnings("unused")
    private static final String LOG_TAG = "Bleed";

    ////////// Member Variable(s) //////////

    public final int mTopPixels;
    public final int mLeftPixels;
    public final int mRightPixels;
    public final int mBottomPixels;

    ////////// Static Initialiser(s) //////////

    ////////// Static Method(s) //////////

    ////////// Constructor(s) //////////

    public Bleed(int topPixels, int rightPixels, int bottomPixels, int leftPixels) {

        this.mTopPixels = topPixels;
        this.mLeftPixels = leftPixels;
        this.mRightPixels = rightPixels;
        this.mBottomPixels = bottomPixels;
    }

    // Constructor used by parcelable interface
    private Bleed(Parcel sourceParcel) {

        this.mTopPixels = sourceParcel.readInt();
        this.mLeftPixels = sourceParcel.readInt();
        this.mRightPixels = sourceParcel.readInt();
        this.mBottomPixels = sourceParcel.readInt();
    }

    ////////// Parcelable Method(s) //////////

    /*****************************************************
     *
     * Describes the contents of this parcelable.
     *
     *****************************************************/
    @Override
    public int describeContents() {

        return 0;
    }

    /*****************************************************
     *
     * Write the contents of this product to a parcel.
     *
     *****************************************************/
    @Override
    public void writeToParcel(Parcel targetParcel, int flags) {

        targetParcel.writeInt(this.mTopPixels);
        targetParcel.writeInt(this.mLeftPixels);
        targetParcel.writeInt(this.mRightPixels);
        targetParcel.writeInt(this.mBottomPixels);
    }

    ////////// Method(s) //////////

    /*****************************************************
     *
     * Creates a string representation of this bleed.
     *
     *****************************************************/
    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("{ topPixels = ").append(this.mTopPixels)
                .append(", leftPixels = ").append(this.mLeftPixels)
                .append(", rightPixels = ").append(this.mRightPixels)
                .append(", bottomPixels = ").append(this.mBottomPixels).append(" }");

        return stringBuilder.toString();
    }

    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * ...
     *
     *****************************************************/

}

