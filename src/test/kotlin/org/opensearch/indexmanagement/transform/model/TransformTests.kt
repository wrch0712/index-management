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

package org.opensearch.indexmanagement.transform.model

import org.opensearch.indexmanagement.transform.randomTransform
import kotlin.test.assertFailsWith
import org.opensearch.test.OpenSearchTestCase

class TransformTests : OpenSearchTestCase() {

    fun `test transform same indices`() {
        assertFailsWith(IllegalArgumentException::class, "Source and target index cannot be the same") {
            randomTransform().copy(sourceIndex = "dummy-index", targetIndex = "dummy-index")
        }
    }

    fun `test transform requires at least one grouping`() {
        assertFailsWith(IllegalArgumentException::class, "Must specify at least one grouping") {
            randomTransform().copy(groups = listOf())
        }
    }

    fun `test transform requires page size to be between 1 and 10K`() {
        assertFailsWith(IllegalArgumentException::class, "Page size was less than 1") {
            randomTransform().copy(pageSize = -1)
        }

        assertFailsWith(IllegalArgumentException::class, "Page size was greater than 10K") {
            randomTransform().copy(pageSize = 10001)
        }

        randomTransform().copy(pageSize = 1)
        randomTransform().copy(pageSize = 10000)
        randomTransform().copy(pageSize = 500)
    }
}