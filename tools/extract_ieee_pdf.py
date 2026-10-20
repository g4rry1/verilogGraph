#!/usr/bin/env python3
"""Extract Annex A BNF from IEEE 1800-2023, preserving bold-red terminals.

Usage: python3 tools/extract_ieee_pdf.py /path/to/ieee1800-2023.pdf
Requires Poppler's pdftohtml. The PDF itself is not committed.
"""

import pathlib
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET


def main() -> None:
    pdf = pathlib.Path(sys.argv[1]).resolve()
    destination = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/ieee1800-2023-annex-a-styled.txt"
    with tempfile.TemporaryDirectory() as temporary:
        xml = pathlib.Path(temporary) / "annex.xml"
        subprocess.run(
            ["pdftohtml", "-f", "1173", "-l", "1217", "-xml", "-hidden", "-i", "-q", str(pdf), str(xml)],
            check=True,
        )
        root = ET.parse(xml).getroot()
        colors = {font.get("id"): font.get("color") for font in root.iter("fontspec")}
        lines = []
        for page in root.iter("page"):
            spans = []
            for node in page.iter("text"):
                top = int(node.get("top"))
                if not 90 < top < 1090 or colors[node.get("font")] == "#0000ff":
                    continue
                value = "".join(node.itertext())
                if value.strip():
                    spans.append((top, int(node.get("left")), int(node.get("width")), value, colors[node.get("font")]))
            groups = []
            for span in sorted(spans):
                if not groups or span[0] - groups[-1][0][0] > 4:
                    groups.append([span])
                else:
                    groups[-1].append(span)
            for group in groups:
                pieces = []
                previous_end = None
                previous_value = ""
                for _, left, width, value, color in sorted(group, key=lambda item: item[1]):
                    if previous_end is not None and left - previous_end >= 4 and \
                            not previous_value.endswith(" ") and not value.startswith(" "):
                        pieces.append(" ")
                    pieces.append(f"⟦{value}⟧" if color == "#ff0000" else value)
                    previous_end = left + width
                    previous_value = value
                line = "".join(pieces).strip()
                lines.append(line)

    start = lines.index("A.1 Source text")
    end = lines.index("A.10 BNF clarifications")
    grammar = "\n".join(lines[start:end]).rstrip() + "\n"
    # The superscript footnotes 26,27 are blue; dropping them leaves a comma
    # attached to this one heading. It is punctuation from the footnote, not BNF.
    grammar = re.sub(r"dpi_function_proto\s*,\s*::=", "dpi_function_proto ::=", grammar)
    if len(re.findall(r"^[A-Za-z_][A-Za-z_0-9]*\s*::=", grammar, re.MULTILINE)) != 736 or "⟦{⟧" not in grammar:
        raise RuntimeError("Extracted Annex A appears incomplete")
    destination.write_text(grammar, encoding="utf-8")
    print(f"Wrote {grammar.count('::=')} rule headings to {destination}")


if __name__ == "__main__":
    main()
