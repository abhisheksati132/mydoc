package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    DOCUMENTS("Documents"),
    TOOLS("Tools");

    companion object {
        val NOW = DOCUMENTS
        val LIBRARY = DOCUMENTS
        val FILES = DOCUMENTS
        val MEDIA = DOCUMENTS
    }
}
