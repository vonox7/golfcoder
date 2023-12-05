package golf.adventofcode.endpoints.web

import io.ktor.server.application.*
import kotlinx.html.*

object AboutView {
    suspend fun getHtml(call: ApplicationCall) {
        call.respondHtmlView("About Advent of Code Golf") {
            h1 { +"About Advent of Code Golf" }

            p {
                +"A global leaderboard for "
                a("https://adventofcode.com") { +"adventofcode.com" }
                +", with a focus on code size."
            }

            p {
                +"Code golf is a type of recreational computer programming competition in which participants strive to achieve the shortest possible source code that solves a certain problem. "
                a("https://en.wikipedia.org/wiki/Code_golf") { +"[wikipedia.com]" }
                br()
                +" In this challenge, "
                em { +"short" }
                +" is considered has having the least amount of code tokens."
            }

            p {
                +"Inspired by "
                a("https://github.com/SebLague/Chess-Challenge") { +"SebLague's chess coding challenge" }
                +"."
            }
            p {
                +"This project is not affiliated with "
                a("https://adventofcode.com") { +"adventofcode.com" }
                +"."
            }

            h2 { +"Credits" }
            "Favicon: " + a("https://www.flaticon.com/free-icons/three") { +"Three icons created by Uniconlabs - Flaticon" }
        }
    }
}