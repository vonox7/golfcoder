package org.golfcoder.database

import com.moshbit.katerbase.MongoDatabase
import com.moshbit.katerbase.child
import org.golfcoder.Sysinfo

class MainDatabase(uri: String) : MongoDatabase(
    uri,
    allowReadFromSecondaries = Sysinfo.isOneOff,
    useMajorityWrite = Sysinfo.isWorker,
    supportChangeStreams = false,
    autoCreateCollections = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    autoCreateIndexes = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    autoDeleteIndexes = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    collections = {
        collection<User>("users") {
            index(
                User::oAuthDetails.child(User.OAuthDetails::provider).ascending(),
                User::oAuthDetails.child(User.OAuthDetails::providerUserId).ascending()
            )
        }
        collection<Solution>("solutions") {
            index(
                Solution::year.ascending(),
                Solution::day.ascending(),
                Solution::part.ascending(),
                Solution::markedAsCheated.descending(),
                Solution::tokenCount.descending()
            )
            index(
                Solution::language.descending(),
                Solution::tokenizerVersion.descending(),
            )
            index(
                Solution::userId.descending(),
                Solution::uploadDate.descending(),
            )
        }
    }
)