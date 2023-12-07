const Parser = require('tree-sitter');
const JavaScript = require('tree-sitter-javascript');
const Python = require('tree-sitter-python');

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
    } else {
        throw new Error("Language not supported");
    }

    const tree = parser.parse(request.body.code);
    const tokens = [];

    const traverseTree = (node) => {
        const children = node.children;
        if (children.length === 0) {
            tokens.push({
                startRow: node.startPosition.row + 1,
                startColumn: node.startPosition.column,
                endRow: node.endPosition.row + 1,
                endColumn: node.endPosition.column,
                type: String(node.type),
                text: String(node.text),
            })
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