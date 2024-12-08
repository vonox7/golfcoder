package org.golfcoder.database

import com.moshbit.katerbase.MongoDatabase
import com.moshbit.katerbase.child
import com.moshbit.katerbase.equal
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
        collection<LeaderboardPosition>("leaderboards") {
            index(
                LeaderboardPosition::year.ascending(),
                LeaderboardPosition::day.ascending(),
                LeaderboardPosition::tokenSum.ascending(),
            )
            index(
                LeaderboardPosition::userId.ascending(),
            )
            index(
                LeaderboardPosition::year.ascending(),
                LeaderboardPosition::position.ascending(),
                partialIndex = arrayOf(LeaderboardPosition::position equal 1)
            )
        }
        collection<ExpectedOutput>("expectedOutputs") {
            index(
                ExpectedOutput::year.ascending(),
                ExpectedOutput::day.ascending(),
                ExpectedOutput::part.ascending(),
                ExpectedOutput::source.ascending(),
            )
        }
    }
)