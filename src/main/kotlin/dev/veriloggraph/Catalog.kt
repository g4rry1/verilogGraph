package dev.veriloggraph

import java.nio.charset.StandardCharsets

data class TokenKind(val name: String, val spelling: String?) {
    val label: String get() = spelling ?: name
}

class TokenCatalog private constructor(val kinds: List<TokenKind>) {
    val byName = kinds.associateBy { it.name }
    val bySpelling = kinds.filter { it.spelling != null }.associateBy { it.spelling!! }

    companion object {
        fun bundled(): TokenCatalog {
            fun resource(name: String): List<String> =
                requireNotNull(TokenCatalog::class.java.getResourceAsStream("/$name")) {
                    "Missing resource: $name"
                }.bufferedReader(StandardCharsets.UTF_8).use { it.readLines() }

            val spellings = resource("slang-token-text.tsv").associate { line ->
                val index = line.indexOf('\t')
                require(index > 0) { "Invalid token spelling line: $line" }
                line.substring(0, index) to line.substring(index + 1)
            }
            val kinds = resource("slang-tokenkinds.txt")
                .filter { it.isNotBlank() }
                .map { TokenKind(it.trim(), spellings[it.trim()]) }
            require(kinds.map { it.name }.toSet().size == kinds.size)
            require(spellings.keys.all { name -> kinds.any { it.name == name } })
            return TokenCatalog(kinds)
        }
    }
}

