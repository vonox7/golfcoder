const Parser = require('tree-sitter');
const Python = require('tree-sitter-python');
const Rust = require('tree-sitter-rust');
const Go = require('tree-sitter-go');
const Kotlin = require('tree-sitter-kotlin');
const JavaScript = require('tree-sitter-javascript');
const CSharp = require('tree-sitter-c-sharp');
const TypeScript = require('tree-sitter-typescript').typescript;
const CPlusPlus = require('tree-sitter-cpp');
const Java = require('tree-sitter-java');
const C = require('tree-sitter-c');
const Swift = require('tree-sitter-swift');
const Scala = require('tree-sitter-scala');

const express = require("express");
const app = express();
app.use(express.json());

app.get('/', (request, response) => {
    response.send("tree-sitter server is running");
});
app.post('/tokenize', (request, response) => {
    const parser = new Parser();

    const languages = {
        "python": Python,
        "rust": Rust,
        "go": Go,
        "kotlin": Kotlin,
        "javascript": JavaScript,
        "csharp": CSharp,
        "typescript": TypeScript,
        "cpp": CPlusPlus,
        "java": Java,
        "c": C,
        "swift": Swift,
        "scala": Scala,
    };
    const language = languages[request.body.language];
    if (language === undefined) {
        throw new Error("Language not supported: " + request.body.language + ". Supported languages: " + request.body.language);
    }
    parser.setLanguage(language);

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

        if (String(node.type) === "preproc_arg") {
            // tree-sitter-cpp/c doesn't evaluate defines, so manually parse the content
            const prefix = "\n".repeat(node.startPosition.row) + " ".repeat(node.startPosition.column);
            const defineTree = parser.parse(prefix + node.text);
            traverseTree(defineTree.rootNode);
        } else if (String(node.type) === "string_literal") {
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
        } else if (String(node.type) === "predefined_type") {
            // tree-sitter-typescript returns for e.g. `let line: string` the `string` as predefined type with 1 child (type=string, text=string).
            // So manually add the predefined type, as `string` is a token, not a literal string.
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