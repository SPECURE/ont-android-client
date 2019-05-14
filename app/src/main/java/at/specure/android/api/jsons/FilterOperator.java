/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package at.specure.android.api.jsons;

import com.google.gson.annotations.SerializedName;

import at.specure.androidX.data.operators.MapFilterOperator;

/**
 * Created by michal.cadrik on 10/24/2017.
 */

public class FilterOperator {

    /**
     * this is true if it is default checked options
     */
    @SerializedName("default")
    public Boolean isDefault = false;

    /**
     * Long name of operator
     */
    @SerializedName("detail")
    public String detail;

    /**
     * Short name of operator
     */
    @SerializedName("title")
    public String title;

    /**
     * could be null if it is all operators
     */
    @SerializedName("id_provider")
    public String id;

    public FilterOperator(MapFilterOperator operator) {
        isDefault = operator.isDefault == null ? false : operator.isDefault;
        detail = operator.detail;
        title = operator.title;
        id = operator.providerNumber == null ? "" : operator.title;
    }
}

