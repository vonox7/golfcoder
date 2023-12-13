const Parser = require('tree-sitter');
const Python = require('tree-sitter-python');
const JavaScript = require('tree-sitter-javascript');
const Kotlin = require('tree-sitter-kotlin');

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
    } else if (request.body.language === "javascript") {
        parser.setLanguage(JavaScript);
    } else if (request.body.language === "kotlin") {
        parser.setLanguage(Kotlin);
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

        if (String(node.type) === "character_literal") {
            // tree-sitter-kotlin has as character_literal text "'a'", but has only 2 children (the 2 brackets).
            // So manually add the middle character.
            // Not sure yet if this needs to be done for other languages - at least Python & JS don't have this bug.
            addNode(node.children[0], "character_literal")
            tokens.push({
                startRow: node.children[0].endPosition.row + 1,
                startColumn: node.children[0].endPosition.column,
                endRow: node.children[1].startPosition.row + 1,
                endColumn: node.children[1].startPosition.column,
                type: "character_literal",
                text: String(node.text).replaceAll("'", ""),
            })
            addNode(node.children[1], "character_literal")
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