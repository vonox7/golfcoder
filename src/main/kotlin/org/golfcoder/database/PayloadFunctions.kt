package org.golfcoder.database

import com.moshbit.katerbase.inArray
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.golfcoder.database.pgpayloads.UserTable
import org.golfcoder.database.pgpayloads.toUser
import org.golfcoder.mainDatabase
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.selectAll


suspend fun getUserProfiles(userIds: Set<String>): Map<String, User> {
    val userIdsToUsers = (UserTable.selectAll().where(UserTable.id inList userIds).map { it.toUser() }.toList() +
            mainDatabase.getSuspendingCollection<User>()
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
            )
        .associateBy { it._id }
    return userIdsToUsers
}