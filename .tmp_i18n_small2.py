import io

sp = 'app/src/main/res/values/strings.xml'
s = io.open(sp, encoding='utf-8').read()
add = '''    <string name="fidelity_full_layout">Full layout</string>
    <string name="fidelity_relaid">Re-laid out</string>
    <string name="fidelity_text_only">Text only</string>
    <string name="fidelity_recovered">Recovered text</string>
    <string name="fidelity_not_rendered">Not rendered</string>
    <string name="notes_title">Speaker notes</string>
    <string name="notes_slide">Slide %1$d</string>
    <string name="notes_empty">No speaker notes for this slide.</string>
    <string name="deck_fail_title">Can\u2019t render this deck</string>
    <string name="deck_fail_message">This presentation uses proprietary slides or formatting that could not be parsed. Open it in another presentation app for full fidelity.</string>
    <string name="deck_open_other">Open in another app</string>
    <string name="ctx_copy">Copy</string>
    <string name="ctx_copy_page">Copy page</string>
    <string name="ctx_search">Search</string>
    <string name="cd_close_actions">Close actions</string>
</resources>'''
s = s.rstrip()[:-len('</resources>')] + add
io.open(sp, 'w', encoding='utf-8', newline='').write(s)


def fix_imports(t):
    if 'import androidx.compose.ui.res.stringResource' not in t and 'stringResource(' in t:
        t = t.replace('import androidx.compose.ui.Modifier',
                      'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.res.stringResource', 1)
    if 'import com.abhisheksati.mydoc.R' not in t and 'R.string.' in t:
        lines = t.split('\n')
        idx = next(i for i, l in enumerate(lines) if l.startswith('import '))
        lines.insert(idx, 'import com.abhisheksati.mydoc.R')
        t = '\n'.join(lines)
    return t


def apply(path, reps, composable_label=False):
    t = io.open(path, encoding='utf-8').read()
    for old, new in reps:
        if old not in t:
            raise SystemExit(path + ' missing: ' + old[:60])
        t = t.replace(old, new)
    t = fix_imports(t)
    io.open(path, 'w', encoding='utf-8', newline='').write(t)


base = 'app/src/main/java/com/abhisheksati/mydoc/ui/'
apply(base + 'viewer/FidelityNotice.kt', [
    ('fun RenderFidelity.label(): String = when (this) {',
     '@Composable\nfun RenderFidelity.label(): String = when (this) {'),
    ('RenderFidelity.FIXED_LAYOUT -> "Full layout"', 'RenderFidelity.FIXED_LAYOUT -> stringResource(R.string.fidelity_full_layout)'),
    ('RenderFidelity.STRUCTURED_REFLOW -> "Re-laid out"', 'RenderFidelity.STRUCTURED_REFLOW -> stringResource(R.string.fidelity_relaid)'),
    ('RenderFidelity.TEXT_ONLY -> "Text only"', 'RenderFidelity.TEXT_ONLY -> stringResource(R.string.fidelity_text_only)'),
    ('RenderFidelity.RECOVERED_SCRAPE -> "Recovered text"', 'RenderFidelity.RECOVERED_SCRAPE -> stringResource(R.string.fidelity_recovered)'),
    ('RenderFidelity.NONE -> "Not rendered"', 'RenderFidelity.NONE -> stringResource(R.string.fidelity_not_rendered)'),
])

apply(base + 'viewer/presentation/PresentationSpeakerNotesSheet.kt', [
    ('text = "Speaker notes",', 'text = stringResource(R.string.notes_title),'),
    ('text = "Slide ${currentSlideIndex + 1}",', 'text = stringResource(R.string.notes_slide, currentSlideIndex + 1),'),
    ('text = notes.ifBlank { "No speaker notes for this slide." },',
     'text = notes.ifBlank { stringResource(R.string.notes_empty) },'),
])

apply(base + 'viewer/presentation/PresentationEmptyRecoveryCard.kt', [
    ('text = "Can\u2019t render this deck",', 'text = stringResource(R.string.deck_fail_title),'),
    ('text = "Can\'t render this deck",', 'text = stringResource(R.string.deck_fail_title),'),
    ('text = "This presentation uses proprietary slides or formatting that could not be parsed. Open it in another presentation app for full fidelity.",',
     'text = stringResource(R.string.deck_fail_message),'),
    ('Text("Open in another app")', 'Text(stringResource(R.string.deck_open_other))'),
])

apply(base + 'viewer/pdf/PdfFloatingContextualBar.kt', [
    ('label = if (activeSelectionText != null) "Copy" else "Copy page",',
     'label = stringResource(if (activeSelectionText != null) R.string.ctx_copy else R.string.ctx_copy_page),'),
    ('label = "Search",', 'label = stringResource(R.string.ctx_search),'),
    ('contentDescription = "Close actions",', 'contentDescription = stringResource(R.string.cd_close_actions),'),
])
print('ok')
