const Parser = require('tree-sitter');
const Python = require('tree-sitter-python');
const Rust = require('tree-sitter-rust');
const Go = require('tree-sitter-go');
const Kotlin = require('tree-sitter-kotlin');
const JavaScript = require('tree-sitter-javascript');

const express = require("express");
const app = express();
app.use(express.json());

app.get('/', (request, response) => {
    response.send("tree-sitter server is running");
});
app.post('/tokenize', (request, response) => {
    const parser = new Parser();

    if (request.body.language === "python") {
        parser.setLanguage(Python);
    } else if (request.body.language === "rust") {
        parser.setLanguage(Rust);
    } else if (request.body.language === "go") {
        parser.setLanguage(Go);
    } else if (request.body.language === "kotlin") {
        parser.setLanguage(Kotlin);
    } else if (request.body.language === "javascript") {
        parser.setLanguage(JavaScript);
    } else {
        throw new Error("Language not supported");
    }

    const tree = parser.parse(request.body.code);
    const tokens = [];

    let addNode = (node, type) => {
        tokens.push({
            startRow: node.startPosition.row + 1,
            startColumn: node.startPosition.column,
            endRow: node.endPosition.row + 1,
            endColumn: node.endPosition.column,
            type: type ?? String(node.type),
            text: String(node.text),
        })
    }

    const traverseTree = (node) => {
        const children = node.children;

        if (String(node.type) === "string_literal") {
            // tree-sitter-rust has as string_literal text ""hello"", but has only 2 children (the 2 brackets).
            // So manually add the literal text
            addNode(node)
        } else if (String(node.type) === "interpreted_string_literal") {
            // tree-sitter-go has as interpreted_string_literal text ""hello"", but has only 2 children (the 2 brackets).
            // So manually add the literal text
            addNode(node)
        } else if (String(node.type) === "character_literal") {
            // tree-sitter-kotlin has as character_literal text "'a'", but has only 2 children (the 2 brackets).
            // So manually add the literal text
            addNode(node)
        } else if (children.length === 0) {
            addNode(node)
        } else {
            for (const child of node.children) {
                traverseTree(child);
            }
        }
    };

    traverseTree(tree.rootNode);

    response.send({tokens: tokens});
});


app.listen(8031);
console.log("Listening on port 8031");