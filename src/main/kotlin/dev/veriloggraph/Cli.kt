package dev.veriloggraph

import java.io.Reader
import java.io.Writer
import java.io.PrintWriter

/** Interactive, context-aware grammar browser with injectable streams for tests. */
fun runCli(
    catalog: TokenCatalog,
    grammar: Grammar,
    input: Reader,
    output: Writer,
    initialRule: String = "module_declaration",
) {
    val out = PrintWriter(output)
    val reader = input.buffered()
    fun resolvedRule(name: String): String? = when {
        name in grammar.byLeft -> name
        "@lexer_$name" in grammar.byLeft -> "@lexer_$name"
        else -> null
    }
    val root = resolvedRule(initialRule)
    if (root == null) {
        out.println("Правило недоступно для пошагового просмотра: $initialRule")
        out.flush()
        return
    }
    val graph = TokenGraph(grammar, catalog)
    val explorer = Explorer(grammar, catalog, root)

    fun token(value: String): TokenKind? = catalog.byName[value] ?: catalog.bySpelling[value]
    fun visibleRule(name: String) = name.removePrefix("@lexer_")
    fun path() {
        out.println("Правило: ${visibleRule(explorer.root)}")
        out.println("Префикс: ${explorer.prefix.joinToString(" ") { it.tokenText() }.ifEmpty { "(пусто)" }}")
        out.println(if (explorer.isComplete()) "Конструкция может завершиться здесь." else "Конструкция пока не завершена.")
    }
    fun candidates(items: List<Candidate>, limit: Int = 40, contextual: Boolean = true) {
        if (items.isEmpty()) {
            out.println("Следующих токенов нет.")
            return
        }
        out.println("Продолжений: ${items.size}")
        for (item in items.take(limit)) {
            val sources = item.rules.take(3).joinToString()
            val more = if (item.rules.size > 3) " (+${item.rules.size - 3})" else ""
            val boundary = when {
                !contextual -> ""
                item.starts.isEmpty() -> "  [без начала нового правила]"
                else -> "  [начало: ${item.starts.joinToString()}]"
            }
            out.println("  ${item.token.tokenText()}  ←  $sources$more$boundary")
        }
        if (items.size > limit) out.println("Показаны первые $limit; используйте фильтр команды.")
    }
    fun next(query: String = "") {
        path()
        val items = explorer.next().filter {
            query.isEmpty() || query == "*" || it.token.name.contains(query, true) || it.token.label.contains(query, true)
        }
        candidates(items, if (query == "*") Int.MAX_VALUE else 40)
    }
    fun lookup(kind: String, query: String) {
        val found = if (kind == "tokens") {
            catalog.kinds.asSequence()
                .filter { query.isEmpty() || it.name.contains(query, true) || it.label.contains(query, true) }
                .map { it.tokenText() }.sorted().toList()
        } else {
            grammar.sources.keys.asSequence()
                .filter { resolvedRule(it) != null }
                .filter { query.isEmpty() || it.contains(query, true) }
                .sorted().toList()
        }
        out.println("Найдено: ${found.size}")
        found.take(30).forEach { out.println("  $it") }
        if (found.size > 30) out.println("Показаны первые 30; уточните запрос.")
    }

    out.println("VerilogGraph CLI — IEEE 1800-2023 Annex A / slang TokenKind")
    out.println("Введите help для списка команд.")
    next()
    while (true) {
        out.print("vg> ")
        out.flush()
        val line = reader.readLine()?.trim() ?: break
        if (line.isEmpty()) continue
        val command = line.substringBefore(' ').lowercase()
        val arg = line.substringAfter(' ', "").trim()
        when (command) {
            "help", "?" -> out.println(
                "Команды: next [поиск|*] | step <TokenKind/написание> | back | path | use <правило> | " +
                    "rules [поиск] | tokens [поиск] | rule <правило> | global <токен> | " +
                    "edge <от> <к> | quit"
            )
            "next" -> next(arg)
            "path" -> path()
            "step" -> {
                val chosen = token(arg)
                when {
                    arg.isEmpty() -> out.println("Использование: step <TokenKind/написание>")
                    chosen == null -> out.println("Неизвестный токен: $arg")
                    !explorer.advance(chosen.name) -> out.println("Недопустимое продолжение: ${chosen.tokenText()}")
                    else -> next()
                }
            }
            "back" -> if (explorer.back()) next() else out.println("Префикс уже пуст.")
            "use" -> {
                val rule = resolvedRule(arg)
                when {
                    arg.isEmpty() -> out.println("Использование: use <правило>")
                    rule == null -> out.println("Правило недоступно для пошагового просмотра: $arg")
                    else -> { explorer.reset(rule); next() }
                }
            }
            "rules", "tokens" -> lookup(command, arg)
            "rule" -> {
                val source = grammar.sources[arg]
                if (source == null) out.println("Неизвестное правило: $arg")
                else out.println("${source.section}\n${source.text}")
            }
            "global" -> {
                val parts = arg.split(Regex("\\s+")).filter(String::isNotEmpty)
                val from = token(parts.firstOrNull().orEmpty())
                if (from == null) out.println("Неизвестный токен: $arg")
                else {
                    out.println("Глобальные продолжения ${from.tokenText()}:")
                    val query = parts.drop(1).joinToString(" ")
                    val items = graph.successors(from.name).filter {
                        query.isEmpty() || query == "*" ||
                            it.token.name.contains(query, true) || it.token.label.contains(query, true)
                    }
                    candidates(items, if (query == "*") Int.MAX_VALUE else 40, contextual = false)
                }
            }
            "edge" -> {
                val parts = arg.split(Regex("\\s+")).filter(String::isNotEmpty)
                if (parts.size != 2) out.println("Использование: edge <от> <к>")
                else {
                    val from = token(parts[0])
                    val to = token(parts[1])
                    if (from == null || to == null) out.println("Неизвестный токен в запросе.")
                    else {
                        val sources = graph.evidence(from.name, to.name)
                        if (sources.isEmpty()) out.println("Глобального ребра нет: ${from.tokenText()} → ${to.tokenText()}")
                        else {
                            out.println("${from.tokenText()} → ${to.tokenText()}")
                            for (source in sources) {
                                val section = grammar.sources[source]?.section ?: "Annex A"
                                out.println("  $source — $section")
                            }
                        }
                    }
                }
            }
            "quit", "exit" -> { out.println("Выход."); out.flush(); return }
            else -> out.println("Неизвестная команда: $command (help — список команд)")
        }
        out.flush()
    }
    out.flush()
}

private fun TokenKind.tokenText(): String =
    if (spelling == null) name else "$name ($spelling)"
