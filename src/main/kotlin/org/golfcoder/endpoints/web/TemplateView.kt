package org.golfcoder.endpoints.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.golfcoder.database.Solution
import org.intellij.lang.annotations.Language

object TemplateView {
    suspend fun download(call: ApplicationCall) {
        val templateFileName = call.parameters["templateFileName"] ?: throw NotFoundException("No language specified")
        val (languageName, fileEnding) = Regex("golfcoder-([a-z]+)-template.([a-z]+)")
            .matchEntire(templateFileName)
            ?.destructured
            ?: throw NotFoundException("Invalid template file name")
        val language = Solution.Language.entries.find { it.name.lowercase() == languageName }
            ?: throw NotFoundException("Invalid language")
        if (fileEnding != language.fileEnding) throw NotFoundException("Invalid file ending")
        val template = language.template ?: throw NotFoundException("No template available")

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, templateFileName)
                .toString()
        )
        call.respondText(contentType = ContentType.Text.Any, text = template)
    }

    val Solution.Language.template: String?
        get() = when (this) {
            Solution.Language.PYTHON -> {
                @Language("Python")
                val code = """
                |# Template for reading all lines from stdin and printing the line count to stdout.
                |# Copy this code to your IDE to get started.
                |
                |lines = []
                |while True:
                |    try:
                |        lines.append(input())
                |    except EOFError:
                |        break
                |print(len(lines))""".trimMargin()
                code
            }

            Solution.Language.RUST -> {
                @Language("TEXT")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |use std::io::BufRead;
                |
                |fn main() {
                |    let stdin = std::io::stdin();
                |    let lines = stdin.lock().lines();
                |    println!("{}", lines.count());
                |}""".trimMargin()
                code
            }

            Solution.Language.GO -> {
                @Language("Go")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |package main
                |
                |import (
                |    "bufio"
                |    "fmt"
                |    "os"
                |)
                |
                |func main() {
                |    scanner := bufio.NewScanner(os.Stdin)
                |    lines := 0
                |    for scanner.Scan() {
                |        lines++
                |    }
                |    fmt.Println(lines)
                |}""".trimMargin()
                code
            }

            Solution.Language.KOTLIN -> {
                @Language("kotlin")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |fun main() {
                |    // If you want to iterate over `lines` multiple times, write `generateSequence(::readLine).toList()`
                |    val lines = generateSequence(::readLine)
                |    println(lines.count())
                |}""".trimMargin()
                code
            }

            Solution.Language.JAVASCRIPT -> {
                @Language("JavaScript")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |let lines = [];
                |
                |require('readline')
                |    .createInterface({input: process.stdin})
                |    .on('line', (line) => {
                |        lines.push(line);
                |    })
                |    .on('close', () => {
                |        console.log(lines.length);
                |    });""".trimMargin()
                code
            }

            Solution.Language.CSHARP -> {
                @Language("TEXT")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |using System;
                |using System.Collections.Generic;
                |using System.Linq;
                |
                |public class Program
                |{
                |    public static void Main()
                |    {
                |        var lines = new List<string>();
                |        string line;
                |        while ((line = Console.ReadLine()) != null)
                |        {
                |            lines.Add(line);
                |        }
                |        Console.WriteLine(lines.Count);
                |    }
                |}""".trimMargin()
                code
            }

            Solution.Language.TYPESCRIPT -> {
                @Language("TypeScript")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |declare var require: any
                |declare var process: any
                |
                |let lines: string[] = [];
                |
                |require('readline')
                |    .createInterface({input: process.stdin})
                |    .on('line', (line) => {
                |        lines.push(line);
                |    })
                |    .on('close', () => {
                |        console.log(lines.length);
                |    });""".trimMargin()
                code
            }

            Solution.Language.CPLUSPLUS -> {
                @Language("C++")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |#include <iostream>
                |
                |int main() {
                |    std::string line;
                |    int lines = 0;
                |    while (std::getline(std::cin, line)) {
                |        lines++;
                |    }
                |    std::cout << lines << std::endl;
                |}""".trimMargin()
                code
            }

            Solution.Language.JAVA -> {
                @Language("Java")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |import java.util.ArrayList;
                |import java.util.Scanner;
                |
                |public class Main {
                |    public static void main(String[] args) {
                |        Scanner scanner = new Scanner(System.in);
                |        var lines = new ArrayList<String>();
                |        while (scanner.hasNextLine()) {
                |            lines.add(scanner.nextLine());
                |        }
                |        System.out.println(lines.size());
                |    }
                |}""".trimMargin()
                code
            }

            Solution.Language.C -> {
                @Language("TEXT")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |#include <stdio.h>
                |
                |int main() {
                |    char line[1000];
                |    int lines = 0;
                |    while (fgets(line, sizeof(line), stdin)) {
                |        lines++;
                |    }
                |    printf("%d", lines);
                |}
""".trimMargin()
                code
            }

            Solution.Language.SWIFT -> {
                @Language("Swift")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |import Foundation
                |
                |var lines = [String]()
                |while let line = readLine() {
                |    lines.append(line)
                |}
                |print(lines.count)
                |""".trimMargin()
                code
            }

            Solution.Language.SCALA -> {
                @Language("TEXT")
                val code = """
                |// Template for reading lines from stdin and printing the line count to stdout.
                |// Copy this code to your IDE to get started.
                |
                |import scala.io.StdIn
                |
                |object Main extends App {
                |    val lines = Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toList
                |    println(lines.length)
                |}""".trimMargin()
                code
            }

            Solution.Language.RUBY -> {
                @Language("TEXT") val code = """
                |# Template for reading lines from stdin and printing the line count to stdout.
                |# Copy this code to your IDE to get started.
                |
                |lines = []
                |while line = gets
                |    lines << line
                |end
                |puts lines.length
                |""".trimMargin()
                code
            }
        }
}
