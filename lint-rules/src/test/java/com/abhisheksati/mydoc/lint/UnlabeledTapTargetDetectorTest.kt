package com.abhisheksati.mydoc.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class UnlabeledTapTargetDetectorTest {

    private fun check(body: String) = lint()
        .files(
            kotlin("""
                package androidx.compose.runtime
                @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
                annotation class Composable
            """),
            kotlin("""
                package androidx.compose.ui
                interface Modifier { companion object : Modifier }
            """),
            kotlin("""
                package androidx.compose.material.icons
                object Icons { object Outlined }
            """),
            kotlin("""
                package androidx.compose.material.icons.outlined
                import androidx.compose.material.icons.Icons
                val Icons.Outlined.Close: Any get() = Any()
            """),
            kotlin("""
                package androidx.compose.foundation
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                fun Modifier.clickable(onClickLabel: String? = null, onClick: () -> Unit): Modifier = this
                fun Modifier.combinedClickable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = this
                @Composable fun Image(imageVector: Any, contentDescription: String?, modifier: Modifier = Modifier) {}
            """),
            kotlin("""
                package androidx.compose.foundation.layout
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                @Composable fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit) {}
            """),
            kotlin("""
                package androidx.compose.material3
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                @Composable fun Icon(imageVector: Any, contentDescription: String?, modifier: Modifier = Modifier) {}
                @Composable fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {}
                @Composable fun Text(text: String) {}
            """),
            kotlin("""
                package coil.compose
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                @Composable fun AsyncImage(model: Any?, contentDescription: String?, modifier: Modifier = Modifier) {}
            """),
            kotlin(
                """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.Image
                import androidx.compose.foundation.clickable
                import androidx.compose.foundation.combinedClickable
                import androidx.compose.foundation.layout.Box
                import androidx.compose.material.icons.Icons
                import androidx.compose.material.icons.outlined.Close
                import androidx.compose.material3.Icon
                import androidx.compose.material3.IconButton
                import androidx.compose.material3.Text
                import androidx.compose.ui.Modifier
                import coil.compose.AsyncImage

                $body
                """.trimIndent()
            )
        )
        .issues(UnlabeledTapTargetDetector.ISSUE)
        .run()

    @Test
    fun iconButton_withNullDescription_isFlagged() {
        check(
            """
            @Composable fun Sample() {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun iconButton_withContentDescription_isClean() {
        check(
            """
            @Composable fun Sample() {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun iconButton_withOnClickLabel_isClean() {
        check(
            """
            @Composable fun Sample() {
                IconButton(onClick = {}, modifier = Modifier.clickable(onClickLabel = "Close") {}) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun iconButton_withTextChild_isClean() {
        check(
            """
            @Composable fun Sample() {
                IconButton(onClick = {}) {
                    Text("Close")
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun clickableBox_withUnlabeledIcon_isFlagged() {
        check(
            """
            @Composable fun Sample() {
                Box(Modifier.clickable { }) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun combinedClickableBox_withUnlabeledIcon_isFlagged() {
        check(
            """
            @Composable fun Sample() {
                Box(Modifier.combinedClickable(onClick = {}, onLongClick = {})) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun clickableIcon_withNullDescription_isFlagged() {
        check(
            """
            @Composable fun Sample() {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.clickable { }
                )
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun clickableImage_withNullDescription_isFlagged() {
        check(
            """
            @Composable fun Sample() {
                Image(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.clickable { }
                )
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun clickableAsyncImage_withNullDescription_isFlagged() {
        check(
            """
            @Composable fun Sample(model: Any?) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.clickable { }
                )
            }
            """
        ).expectErrorCount(1)
    }

    @Test
    fun clickableBox_withLabeledIcon_isClean() {
        check(
            """
            @Composable fun Sample() {
                Box(Modifier.clickable { }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun clickableBox_withOnClickLabel_isClean() {
        check(
            """
            @Composable fun Sample() {
                Box(Modifier.clickable(onClickLabel = "Close") { }) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun clickableBox_withTextChild_isClean() {
        check(
            """
            @Composable fun Sample() {
                Box(Modifier.clickable { }) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                    Text("Close")
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun plainBox_withoutClick_isClean() {
        check(
            """
            @Composable fun Sample() {
                Box {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            """
        ).expectClean()
    }

    @Test
    fun nonClickableIcon_isClean() {
        check(
            """
            @Composable fun Sample() {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
            """
        ).expectClean()
    }
}