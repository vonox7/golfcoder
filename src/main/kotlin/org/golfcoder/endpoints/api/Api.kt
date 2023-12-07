package org.golfcoder.endpoints.api

import kotlinx.serialization.Serializable

@Serializable
class ApiCallResult(
    val buttonText: String? = null,
    val alertText: String? = null,
    val reloadSite: Boolean = false,
)