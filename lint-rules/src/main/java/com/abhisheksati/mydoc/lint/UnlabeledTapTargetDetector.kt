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
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * Flags icon-only tap targets that expose no accessible name, so a control that a
 * sighted user reads from its glyph becomes silence for TalkBack.
 */
class UnlabeledTapTargetDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val methodName = node.methodName ?: return
                if (methodName !in TAP_TARGETS) return
                if (!isComposeUi(context)) return
                if (node.valueArguments.none { it is ULambdaExpression }) return

                val callText = node.sourcePsi?.text ?: return
                if (!GLYPH.containsMatchIn(callText)) return
                if (LABELLED_DESC.containsMatchIn(callText)) return
                if (TEXT_CHILD.containsMatchIn(callText)) return

                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "`$methodName` draws an icon with no accessible name. " +
                        "Give the icon a real `contentDescription`, or label it with the " +
                        "action it performs."
                )
            }
        }

    private fun isComposeUi(context: JavaContext): Boolean {
        val text = context.getContents() ?: return false
        return text.contains("androidx.compose.material")
    }

    companion object {
        private val TAP_TARGETS = setOf(
            "IconButton",
            "FloatingActionButton",
            "ExtendedFloatingActionButton",
            "IconButtonRow"
        )
        private val GLYPH = Regex("""\b(Icon|Image|AsyncImage|IconImage)\s*\(""")
        private val LABELLED_DESC = Regex("""contentDescription\s*=\s*(?!null)["a-zA-Z_$(]""")
        private val TEXT_CHILD = Regex("""(^|[^A-Za-z])Text\s*\(""")

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
