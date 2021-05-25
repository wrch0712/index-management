/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.indexmanagement.common.model.dimension

import org.opensearch.indexmanagement.util.IndexUtils.Companion.getFieldFromMappings
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParser.Token
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import org.opensearch.search.aggregations.AggregatorFactories
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder
import java.io.IOException
import org.opensearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder
import org.opensearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder

data class Terms(
    override val sourceField: String,
    override val targetField: String
) : Dimension(Type.TERMS, sourceField, targetField) {

    init {
        require(sourceField.isNotEmpty() && targetField.isNotEmpty()) { "Source and target field must not be empty" }
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sourceField = sin.readString(),
        targetField = sin.readString()
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .startObject(type.type)
            .field(DIMENSION_SOURCE_FIELD_FIELD, sourceField)
            .field(DIMENSION_TARGET_FIELD_FIELD, targetField)
            .endObject()
            .endObject()
    }

    override fun writeTo(out: StreamOutput) {
        out.writeString(sourceField)
        out.writeString(targetField)
    }

    override fun toSourceBuilder(appendType: Boolean): CompositeValuesSourceBuilder<*> {
        val name = if (appendType) "${this.targetField}.${Type.TERMS.type}" else this.targetField
        return TermsValuesSourceBuilder(name)
            .missingBucket(true)
            .field(this.sourceField)
    }

    override fun canBeRealizedInMappings(mappings: Map<String, Any>): Boolean {
        val fieldType = getFieldFromMappings(sourceField, mappings)?.get("type") ?: return false

        return "keyword" == fieldType
    }

    // TODO missing terms field
    fun getRewrittenAggregation(
        aggregationBuilder: TermsAggregationBuilder,
        subAggregations: AggregatorFactories.Builder
    ): TermsAggregationBuilder =
        TermsAggregationBuilder(aggregationBuilder.name)
            .also { aggregationBuilder.collectMode()?.apply { it.collectMode(this) } }
            .executionHint(aggregationBuilder.executionHint())
            .includeExclude(aggregationBuilder.includeExclude())
            .also {
                if (aggregationBuilder.minDocCount() >= 0) {
                    it.minDocCount(aggregationBuilder.minDocCount())
                }
            }
            .also { aggregationBuilder.order()?.apply { it.order(this) } }
            .also {
                if (aggregationBuilder.shardMinDocCount() >= 0) {
                    it.shardMinDocCount(aggregationBuilder.shardMinDocCount())
                }
            }
            .also {
                if (aggregationBuilder.shardSize() > 0) {
                    it.shardSize(aggregationBuilder.shardSize())
                }
            }
            .showTermDocCountError(aggregationBuilder.showTermDocCountError())
            .also {
                if (aggregationBuilder.size() > 0) {
                    it.size(aggregationBuilder.size())
                }
            }
            .field(this.targetField + ".terms")
            .subAggregations(subAggregations)

    companion object {
        @Suppress("ComplexMethod", "LongMethod")
        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): Terms {
            var sourceField: String? = null
            var targetField: String? = null

            ensureExpectedToken(
                Token.START_OBJECT,
                xcp.currentToken(),
                xcp
            )
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    DIMENSION_SOURCE_FIELD_FIELD -> sourceField = xcp.text()
                    DIMENSION_TARGET_FIELD_FIELD -> targetField = xcp.text()
                    else -> throw IllegalArgumentException("Invalid field [$fieldName] found in terms dimension.")
                }
            }
            if (targetField == null) targetField = sourceField
            return Terms(
                requireNotNull(sourceField) { "Source field cannot be null" },
                requireNotNull(targetField) { "Target field cannot be null" }
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput) = Terms(sin)
    }
}