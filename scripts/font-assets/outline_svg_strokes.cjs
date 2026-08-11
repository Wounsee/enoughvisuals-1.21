#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

async function main() {
  const [source, destination, toolsDirectory] = process.argv.slice(2);
  if (!source || !destination || !toolsDirectory) {
    throw new Error("usage: outline_svg_strokes.cjs <source> <destination> <tools-directory>");
  }

  const outlineStroke = require(path.join(toolsDirectory, "node_modules", "svg-outline-stroke"));
  const svg = fs.readFileSync(source);
  const outlined = await outlineStroke(svg, { color: "#000000" });
  fs.writeFileSync(destination, outlined);
}

main().catch(error => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
