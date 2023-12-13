package org.golfcoder.database

import com.moshbit.katerbase.inArray
import kotlinx.coroutines.flow.toList
import org.golfcoder.mainDatabase


suspend fun getUserProfiles(userIds: Set<String>): Map<String, User> {
    val userIdsToUsers = mainDatabase.getSuspendingCollection<User>()
        .find(User::_id inArray userIds)
        .selectedFields(
            User::_id,
            User::name,
            User::nameIsPublic,
            User::profilePictureIsPublic,
            User::publicProfilePictureUrl,
            User::adventOfCodeRepositoryInfo,
        )
        .toList()
        .associateBy { it._id }
    return userIdsToUsers
}