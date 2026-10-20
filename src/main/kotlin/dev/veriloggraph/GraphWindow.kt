package dev.veriloggraph

import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.CubicCurve2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private val background = Color(12, 18, 31)
private val panel = Color(20, 29, 46)
private val muted = Color(142, 160, 186)
private val light = Color(232, 239, 250)
private val accent = Color(69, 203, 177)

class GraphWindow(private val catalog: TokenCatalog, private val grammar: Grammar) : JFrame("VerilogGraph — IEEE 1800-2023") {
    private val explorer = Explorer(grammar, catalog, "source_text")
    private val tokenGraph = TokenGraph(grammar, catalog)
    private val search = JTextField()
    private val tokenModel = DefaultListModel<TokenKind>()
    private val tokenList = JList(tokenModel)
    private val path = JLabel()
    private val details = JTextArea()
    private val graph = GraphCanvas { candidate ->
        if (globalToken != null) {
            globalToken = candidate.token
            refresh()
        } else if (explorer.advance(candidate.token.name)) refresh()
    }
    private var showingNext = false
    private var globalToken: TokenKind? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(930, 660)
        size = Dimension(1280, 820)
        setLocationRelativeTo(null)
        contentPane.background = background
        layout = BorderLayout()

        val header = JPanel(BorderLayout(18, 0)).apply {
            background = background
            border = BorderFactory.createEmptyBorder(22, 28, 18, 28)
            add(JLabel("VERILOGGRAPH").apply {
                foreground = accent
                font = Font("SansSerif", Font.BOLD, 22)
            }, BorderLayout.WEST)
            add(JLabel("IEEE 1800-2023  /  Annex A  /  slang TokenKind").apply {
                foreground = muted
                horizontalAlignment = SwingConstants.RIGHT
                font = Font("SansSerif", Font.PLAIN, 13)
            }, BorderLayout.EAST)
        }
        add(header, BorderLayout.NORTH)

        val left = JPanel(BorderLayout(0, 12)).apply {
            background = panel
            border = BorderFactory.createEmptyBorder(18, 18, 18, 18)
            preferredSize = Dimension(270, 500)
        }
        left.add(JLabel("ТОКЕНЫ SLANG  ·  ${catalog.kinds.size}").apply {
            foreground = muted
            font = Font("SansSerif", Font.BOLD, 12)
        }, BorderLayout.NORTH)
        val listPanel = JPanel(BorderLayout(0, 10)).apply { background = panel }
        search.background = Color(32, 43, 62)
        search.foreground = light
        search.caretColor = accent
        search.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(55, 71, 94)),
            BorderFactory.createEmptyBorder(9, 11, 9, 11)
        )
        search.toolTipText = "Поиск по написанию или TokenKind"
        listPanel.add(search, BorderLayout.NORTH)
        tokenList.background = panel
        tokenList.foreground = light
        tokenList.selectionBackground = Color(41, 82, 86)
        tokenList.selectionForeground = light
        tokenList.font = Font("Monospaced", Font.PLAIN, 13)
        tokenList.fixedCellHeight = 29
        tokenList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        tokenList.setCellRenderer { _, value, _, selected, _ ->
            JLabel("  ${value.label}   ·   ${value.name}").apply {
                foreground = if (selected) accent else light
                background = if (selected) Color(41, 82, 86) else panel
                isOpaque = true
                font = Font("SansSerif", Font.PLAIN, 12)
            }
        }
        listPanel.add(JScrollPane(tokenList).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = panel
        }, BorderLayout.CENTER)
        left.add(listPanel, BorderLayout.CENTER)

        val center = JPanel(BorderLayout(0, 12)).apply {
            background = background
            border = BorderFactory.createEmptyBorder(0, 16, 0, 16)
        }
        val controls = JPanel(BorderLayout(10, 0)).apply { background = background }
        val actions = JPanel().apply { background = background }
        fun button(label: String, action: () -> Unit): JButton = JButton(label).apply {
            background = Color(36, 52, 73)
            foreground = light
            font = Font("SansSerif", Font.BOLD, 12)
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder(9, 14, 9, 14)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { action() }
        }
        actions.add(button("← Назад") {
            if (globalToken != null) {
                openModule()
            } else if (explorer.back()) {
                showingNext = true
                refresh()
            }
        })
        actions.add(button("↺ module") { openModule() })
        actions.add(button("↺ файл") {
            globalToken = null
            explorer.reset("source_text")
            showingNext = true
            refresh()
        })
        actions.add(button("→ Дальше") {
            showingNext = true
            refresh()
        })
        controls.add(actions, BorderLayout.WEST)
        path.foreground = muted
        path.font = Font("Monospaced", Font.PLAIN, 12)
        path.horizontalAlignment = SwingConstants.RIGHT
        controls.add(path, BorderLayout.CENTER)
        center.add(controls, BorderLayout.NORTH)
        center.add(JScrollPane(graph).apply {
            border = BorderFactory.createLineBorder(Color(38, 54, 76))
            viewport.background = panel
            horizontalScrollBar.unitIncrement = 20
            verticalScrollBar.unitIncrement = 20
        }, BorderLayout.CENTER)

        val inspector = JPanel(BorderLayout(0, 10)).apply {
            background = panel
            border = BorderFactory.createEmptyBorder(16, 20, 16, 20)
            preferredSize = Dimension(200, 160)
        }
        inspector.add(JLabel("ПРАВИЛА И ИСТОЧНИКИ ПЕРЕХОДОВ").apply {
            foreground = accent
            font = Font("SansSerif", Font.BOLD, 12)
        }, BorderLayout.NORTH)
        details.background = panel
        details.foreground = light
        details.font = Font("Monospaced", Font.PLAIN, 12)
        details.isEditable = false
        details.lineWrap = true
        details.wrapStyleWord = true
        inspector.add(JScrollPane(details).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = panel
        }, BorderLayout.CENTER)
        center.add(inspector, BorderLayout.SOUTH)

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center).apply {
            dividerLocation = 270
            dividerSize = 4
            border = BorderFactory.createEmptyBorder()
            background = background
        }
        add(split, BorderLayout.CENTER)

        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = filterTokens()
            override fun removeUpdate(e: DocumentEvent) = filterTokens()
            override fun changedUpdate(e: DocumentEvent) = filterTokens()
        })
        tokenList.addListSelectionListener {
            if (!it.valueIsAdjusting) tokenList.selectedValue?.let(::selectToken)
        }
        filterTokens()
        openModule()
    }

    private fun filterTokens() {
        val query = search.text.trim()
        tokenModel.clear()
        catalog.kinds.asSequence()
            .filter { query.isEmpty() || it.label.contains(query, true) || it.name.contains(query, true) }
            .forEach(tokenModel::addElement)
    }

    private fun selectToken(token: TokenKind) {
        explorer.reset("source_text")
        globalToken = null
        showingNext = explorer.advance(token.name)
        if (!showingNext) {
            globalToken = token
            showingNext = true
            refresh()
        } else refresh()
    }

    private fun openModule() {
        globalToken = null
        explorer.reset("source_text")
        check(explorer.advance("ModuleKeyword"))
        showingNext = false
        refresh()
    }

    private fun refresh() {
        val selectedGlobal = globalToken
        if (selectedGlobal != null) {
            val candidates = if (showingNext) tokenGraph.successors(selectedGlobal.name) else emptyList()
            path.text = selectedGlobal.label
            graph.show(selectedGlobal, candidates, showingNext)
            details.text = buildString {
                append("Непосредственные продолжения токена «")
                    .append(selectedGlobal.label).append("» по Annex A.\n")
                append("Составная цепочка может смешивать разные грамматические контексты. ")
                    .append("Сверяйте правило ниже с PDF стандарта.\n")
                append("\nВершин: ").append(tokenGraph.vertexCount)
                    .append("; ребер: ").append(tokenGraph.edgeCount).append("\n")
                for (candidate in candidates) {
                    append("\n").append(candidate.token.label).append("  ←  ")
                        .append(candidate.rules.take(4).joinToString()).append("\n")
                }
            }
            details.caretPosition = 0
            return
        }
        val current = explorer.prefix.lastOrNull()
        path.text = explorer.prefix.joinToString("  →  ") { it.label }
        val candidates = if (showingNext) explorer.next() else emptyList()
        graph.show(current, candidates, showingNext, contextual = true)
        details.text = buildString {
            append("Контекст: ").append(explorer.root).append("\n")
            append("Префикс: ").append(explorer.prefix.joinToString(" ") { it.label }).append("\n")
            if (explorer.isComplete()) append("Правило может завершиться здесь.\n")
            if (!showingNext) append("Нажмите «→ Дальше», чтобы увидеть непосредственные продолжения.\n")
            if (showingNext) append("Начало отмечает именованное правило Annex A, которое может начаться на следующем токене.\n")
            for (candidate in candidates) {
                append("\n").append(candidate.token.label).append("  [").append(candidate.token.name).append("]\n")
                if (candidate.starts.isEmpty()) append("  Без начала нового именованного правила.\n")
                else append("  Могут начаться: ").append(candidate.starts.joinToString()).append("\n")
                for (name in candidate.rules.take(3)) {
                    val source = grammar.sources[name]
                    if (source != null) append("  ").append(source.section).append("  ")
                        .append(source.text.take(320)).append("\n")
                }
            }
            append("\nНеразрешённых обозначений Annex A: ")
                .append(grammar.unresolvedSymbols.size).append("; см. --audit.\n")
        }
        details.caretPosition = 0
    }
}

fun saveModulePreview(catalog: TokenCatalog, grammar: Grammar, path: Path) {
    val explorer = Explorer(grammar, catalog, "source_text")
    check(explorer.advance("ModuleKeyword"))
    val canvas = GraphCanvas { }
    canvas.show(catalog.byName["ModuleKeyword"], explorer.next(), true, contextual = true)
    canvas.setSize(900, 450)
    val image = BufferedImage(900, 450, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    canvas.paint(graphics)
    graphics.dispose()
    ImageIO.write(image, "png", path.toFile())
}

private class GraphCanvas(private val onChoose: (Candidate) -> Unit) : JPanel() {
    private var current: TokenKind? = null
    private var successors = emptyList<Candidate>()
    private var revealed = false
    private var contextual = false
    private val hitboxes = mutableListOf<Pair<java.awt.Rectangle, Candidate>>()

    init {
        background = panel
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                hitboxes.firstOrNull { it.first.contains(e.point) }?.second?.let(onChoose)
            }
        })
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                cursor = Cursor.getPredefinedCursor(
                    if (hitboxes.any { it.first.contains(e.point) }) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR
                )
            }
        })
    }

    fun show(current: TokenKind?, successors: List<Candidate>, revealed: Boolean, contextual: Boolean = false) {
        this.current = current
        this.successors = successors
        this.revealed = revealed
        this.contextual = contextual
        preferredSize = Dimension(900, maxOf(450, 100 + successors.size * 65))
        revalidate()
        repaint()
    }

    fun showStandalone(token: TokenKind) = show(token, emptyList(), false)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.color = Color(38, 54, 76, 110)
        for (x in 18 until width step 32) for (y in 18 until height step 32) g2.fillOval(x, y, 2, 2)

        val centerY = 220
        val x = 65
        val w = 225
        val h = 82
        g2.color = Color(23, 88, 83)
        g2.fill(RoundRectangle2D.Float(x.toFloat(), (centerY - 40).toFloat(), w.toFloat(), h.toFloat(), 22f, 22f))
        g2.color = accent
        g2.stroke = BasicStroke(2f)
        g2.draw(RoundRectangle2D.Float(x.toFloat(), (centerY - 40).toFloat(), w.toFloat(), h.toFloat(), 22f, 22f))
        g2.font = Font("SansSerif", Font.BOLD, 20)
        g2.drawString(current?.label?.take(19) ?: "начало", x + 17, centerY - 2)
        g2.font = Font("Monospaced", Font.PLAIN, 11)
        g2.color = muted
        g2.drawString(current?.name ?: "grammar", x + 17, centerY + 20)

        hitboxes.clear()
        if (revealed) {
            for ((index, candidate) in successors.withIndex()) {
                val y = 58 + index * 65
                val tx = 430
                val tw = 300
                val th = 47
                val begins = contextual && candidate.starts.isNotEmpty()
                g2.color = if (begins) accent else Color(58, 96, 114)
                g2.stroke = BasicStroke(1.7f)
                g2.draw(CubicCurve2D.Float((x + w).toFloat(), centerY.toFloat(), 345f, centerY.toFloat(), 375f, (y + 23).toFloat(), tx.toFloat(), (y + 23).toFloat()))
                g2.fillPolygon(intArrayOf(tx - 1, tx - 9, tx - 9), intArrayOf(y + 23, y + 18, y + 28), 3)
                g2.color = Color(34, 49, 69)
                g2.fill(RoundRectangle2D.Float(tx.toFloat(), y.toFloat(), tw.toFloat(), th.toFloat(), 15f, 15f))
                g2.color = if (begins) accent else Color(61, 87, 112)
                g2.draw(RoundRectangle2D.Float(tx.toFloat(), y.toFloat(), tw.toFloat(), th.toFloat(), 15f, 15f))
                g2.color = light
                g2.font = Font("SansSerif", Font.BOLD, 14)
                g2.drawString(candidate.token.label.take(19), tx + 14, y + 21)
                g2.font = Font("Monospaced", Font.PLAIN, 10)
                g2.color = muted
                g2.drawString(candidate.token.name.take(20), tx + 160, y + 20)
                if (contextual) {
                    g2.color = if (begins) accent else muted
                    val rule = candidate.starts.firstOrNull { it.endsWith("_declaration") || it.endsWith("_construct") }
                        ?: candidate.starts.firstOrNull()
                    val label = if (rule == null) "без начала нового правила" else "начало: $rule"
                    g2.drawString(label.take(45), tx + 14, y + 38)
                }
                hitboxes += java.awt.Rectangle(tx, y, tw, th) to candidate
            }
        } else {
            g2.color = muted
            g2.font = Font("SansSerif", Font.PLAIN, 15)
            g2.drawString("Нажмите «→ Дальше»", 430, centerY + 5)
        }
        g2.dispose()
    }
}
