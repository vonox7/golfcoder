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
                +" and it's community leaderboard."
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

            h2 { +"FAQ" }
            h3 { +"What is a token?" }
            +"A token represents one token in the "
            a(href = "https://en.wikipedia.org/wiki/Abstract_syntax_tree") { +"abstract syntax tree" }
            +". "
            +"Every name, including variables and functions, is considered as a single token, irrespective of its length. "
            +"Therefore, both lines of code - "
            code { +"bool a = true;" }
            +" and "
            code { +"bool aVeryDetailedVariableName = true;" }
            +" - contribute equally to the token count."
            br()
            +"Whitespaces (spaces, newlines, tabs...), statement delimiters (eg. semikolons in C++) and comments are ignored."
            +"However, each single character in a string counts as one token, so solving the challenge with one big Regex might not be the optimal solution."
            br()
            br()
            +"This means that the following code has a score of 13 tokens: "
            br()
            br()
            code {
                +"fun main() {"
                br()
                +"    println(\"hi\")"
                br()
                +"}"
            }
            ol {
                li { code { +"fun" } }
                li { code { +"main" } }
                li { code { +"(" } }
                li { code { +")" } }
                li { code { +"{" } }
                li { code { +"println" } }
                li { code { +"(" } }
                li { code { +"\"" } }
                li { code { +"h" } }
                li { code { +"i" } }
                li { code { +"\"" } }
                li { code { +")" } }
                li { code { +"}" } }
            }
            +""

            h3 { +"How is the leaderboard sorted?" }
            +"Each user has for each language one leaderboard entry. The best score from each part will be used. "
            +"If a user has not submitted a solution for a part, that part will be ranked with 10_000 tokens."

            h3 { +"Why is my language not yet supported?" }
            +"Writing a tokenizer for a language is not trivial. "
            +"I therefore look for a tokenizer that is already implemented and can easily be integrated. "
            /* TODO uncomment when public
            +"Join the discussion on "
            a(href = "https://github.com/vonox7/advent-of-code-golf/labels/language-support") { +"GitHub" }
            +" and help to add support for your favorite language, or just upvote the language you want to promote! "
             */
            +"Please be patient, I will add more popular languages as soon as possible. "

            h2 { +"Credits" }
            ul {
                li {
                    +"Advent of Code: "
                    a("https://adventofcode.com", "_blank") { +"Eric Wastl + team" }
                }
                li {
                    +"Favicon: "
                    a("https://www.flaticon.com/free-icons/three", "_blank") {
                        +"Three icons created by Uniconlabs - Flaticon"
                    }
                }
                li {
                    +"HTML Checkbox: "
                    a("https://www.w3schools.com/howto/howto_css_custom_checkbox.asp", "_blank") { +"w3schools" }
                }
                li {
                    +"Wording help: "
                    a("https://chat.openai.com/", "_blank") { +"ChatGPT 3.5" }
                }
                li {
                    +"This site: "
                    a("https://github.com/vonox7", "_blank") { +"Valentin Slawicek" }
                }
            }
        }
    }
}