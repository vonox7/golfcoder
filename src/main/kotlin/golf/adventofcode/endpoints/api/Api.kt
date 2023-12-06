package golf.adventofcode.endpoints.api

import kotlinx.serialization.Serializable

@Serializable
class ApiCallResult(
    val buttonText: String? = null,
    val reloadSite: Boolean = false
)