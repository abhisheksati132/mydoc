package com.abhisheksati.mydoc.domain.model

/**
 * The type chips above a folder listing. Lives in the domain layer because the choice is held by
 * the view model, not the pane: the Library screen leaves composition whenever a document is
 * opened, and a filter kept in `remember` came back as ALL after pressing back.
 */
enum class DocumentTypeFilter(val label: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PPT"),
    TEXT("Text")
}
