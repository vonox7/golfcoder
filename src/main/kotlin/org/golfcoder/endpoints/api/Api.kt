package org.golfcoder.endpoints.api

import kotlinx.serialization.Serializable

@Serializable
class ApiCallResult(
    val buttonText: String? = null,
    val resetButtonTextSeconds: Int? = 2,
    val alertText: String? = null,
    val reloadSite: Boolean = false,
    val changeInput: Map<String, String> = emptyMap(), // name to value
    val setInnerHtml: Map<String, String> = emptyMap(), // id to html
)