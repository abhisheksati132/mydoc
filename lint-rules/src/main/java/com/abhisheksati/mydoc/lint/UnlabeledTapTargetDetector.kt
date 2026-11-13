package com.abhisheksati.mydoc.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * Flags icon-only tap targets that expose no accessible name, so a control that a
 * sighted user reads from its glyph becomes silence for TalkBack. Covers both
 * icon-button composables and raw Box/Icon/Image surfaces made tappable through
 * Modifier.clickable / combinedClickable.
 */
class UnlabeledTapTargetDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve() ?: return
                val methodName = method.name
                val owner = method.containingClass?.qualifiedName.orEmpty()
                if (!owner.startsWith("androidx.compose.") && !owner.startsWith("coil.compose.")) return
                if (methodName !in TAP_TARGETS && methodName !in CLICKABLE_GLYPH_SURFACES) return

                var hasGlyph = false
                var hasClick = false
                var hasName = false
                node.accept(object : AbstractUastVisitor() {
                    override fun visitCallExpression(call: UCallExpression): Boolean {
                        val called = call.resolve() ?: return false
                        val name = called.name
                        if (name in GLYPHS) hasGlyph = true
                        if (name == "clickable" || name == "combinedClickable") hasClick = true
                        if (name == "Text") hasName = true
                        called.parameterList.parameters.forEachIndexed { index, parameter ->
                            if (parameter.name == "contentDescription" || parameter.name == "onClickLabel") {
                                val argument = call.getArgumentForParameter(index)
                                if (argument != null && isNonEmptyLabel(argument)) hasName = true
                            }
                        }
                        return false
                    }
                })
                if (!hasGlyph || hasName || (methodName !in TAP_TARGETS && !hasClick)) return

                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "`$methodName` is a tap target with no accessible name. " +
                        "Give the icon a real `contentDescription`, label the click with " +
                        "`onClickLabel`, or add visible text."
                )
            }
        }

    private fun isNonEmptyLabel(expression: UExpression): Boolean {
        if (expression is UParenthesizedExpression) return isNonEmptyLabel(expression.expression)
        if (expression is ULiteralExpression) return expression.value?.toString()?.isNotBlank() == true
        val evaluated = expression.evaluate()
        return evaluated !is String || evaluated.isNotBlank()
    }

    companion object {
        private val TAP_TARGETS = setOf(
            "IconButton",
            "FloatingActionButton",
            "ExtendedFloatingActionButton",
            "IconButtonRow"
        )
        private val CLICKABLE_GLYPH_SURFACES = setOf(
            "Box",
            "Icon",
            "Image",
            "AsyncImage"
        )
        private val GLYPHS = setOf("Icon", "Image", "AsyncImage", "IconImage")

        val ISSUE: Issue = Issue.create(
            id = "UnlabeledTapTarget",
            briefDescription = "Icon-only tap target has no accessible label",
            explanation = "A tappable control whose only content is an icon marked " +
                "`contentDescription = null` announces nothing to screen readers. " +
                "Declare the action the control performs.",
            category = Category.A11Y,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnlabeledTapTargetDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}