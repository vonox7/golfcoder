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
                br()
                +"The "
                a(href = "https://adventofcode.com/leaderboard") { +"official leaderboard" }
                +" calculates the score based on completion time. "
                +"Depending on your personal timezone and your personal/work schedule, this can be a significant disadvantage. "
                br()
                +"I have, therefore, created this unofficial leaderboard to compare code size optimization, instead of completion time."
                br()
                +"This leaderboard is not meant to replace the official leaderboard but to complement it with a different perspective."
                br()
                +"I want to express my gratitude to Eric Wastl and his supporters for creating the Advent of Code challenges each year with such great detail and care!"
            }

            h2 { +"Credits" }
            ul {
                li {
                    +"Favicon: "
                    a("https://www.flaticon.com/free-icons/three") { +"Three icons created by Uniconlabs - Flaticon" }
                }
                li {
                    +"HTML Checkbox: "
                    a("https://www.w3schools.com/howto/howto_css_custom_checkbox.asp") { +"w3schools" }
                }
            }
        }
    }
}