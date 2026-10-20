#!/usr/bin/env python3
"""Snapshot the slang lexer inventory and its Annex A grammar transcription.

Usage: python3 tools/import_slang.py /path/to/slang
"""

import ast
import pathlib
import re
import subprocess
import sys


def main() -> None:
    source = pathlib.Path(sys.argv[1]).resolve()
    target = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources"
    target.mkdir(parents=True, exist_ok=True)

    kinds = (source / "scripts/tokenkinds.txt").read_text()
    grammar = (source / "scripts/grammar.txt").read_text()
    lexer = (source / "source/parsing/LexerFacts.cpp").read_text()
    method = lexer.split("std::string_view LexerFacts::getTokenKindText(TokenKind kind)", 1)[1]
    method = method.split("// keywords", 1)[0] + "// keywords" + method.split("// keywords", 1)[1]
    method = method.split("\nstd::", 1)[0]
    matches = re.findall(r'case TokenKind::(\w+): return ("(?:\\.|[^"\\])*");', method)
    labels = {kind: ast.literal_eval(quoted) for kind, quoted in matches}
    inventory = [line.strip() for line in kinds.splitlines() if line.strip()]
    unknown = sorted(set(labels) - set(inventory))
    if unknown or len(labels) < 200:
        raise RuntimeError(f"Lexer map looks incomplete: {len(labels)} entries; unknown={unknown}")

    (target / "slang-tokenkinds.txt").write_text(kinds)
    (target / "slang-token-text.tsv").write_text(
        "".join(f"{kind}\t{labels[kind]}\n" for kind in inventory if kind in labels)
    )
    (target / "ieee1800-2023-annex-a.txt").write_text(grammar)
    revision = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
    (target / "SOURCE.txt").write_text(
        "IEEE Std 1800-2023, Annex A (formal syntax)\n"
        "https://ieeexplore.ieee.org/document/10458102\n"
        "Grammar transcription and lexer inventory: MikePopoloski/slang (MIT license)\n"
        f"https://github.com/MikePopoloski/slang/tree/{revision}/scripts\n"
        f"slang revision: {revision}\n"
        "Input: scripts/grammar.txt, scripts/tokenkinds.txt, "
        "source/parsing/LexerFacts.cpp\n"
    )
    print(f"Imported {len(inventory)} token kinds, {len(labels)} fixed spellings, "
          f"{len(grammar.splitlines())} grammar lines from {revision[:12]}")


if __name__ == "__main__":
    main()
