package org.golfcoder.endpoints.api

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object MarkSolutionAsCheatedApi {

  @Serializable
  private class MarkSolutionAsCheatedRequest(
    val solutionId: String,
  )

  suspend fun post(call: ApplicationCall) {
    val request = call.receive<MarkSolutionAsCheatedRequest>()
    val session = call.sessions.get<UserSession>()!!
    val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!
    if (!currentUser.admin) {
      call.respond(ApiCallResult(buttonText = "Not an admin"))
      return
    }

    val solution =
      mainDatabase.getSuspendingCollection<Solution>().findOne(Solution::_id equal request.solutionId) ?: run {
        call.respond(ApiCallResult(buttonText = "Solution not found"))
        return
      }

    mainDatabase.getCollection<Solution>().updateOne(Solution::_id equal solution._id) {
      Solution::markedAsCheated setTo true
    }

    // Invalidate leaderboard cache
    UploadSolutionApi.recalculateScore(year = solution.year, day = solution.day)

    call.respond(
      ApiCallResult(buttonText = "Marked as cheated", redirect = "/${solution.year}/day/${solution.day}")
    )
  }
}