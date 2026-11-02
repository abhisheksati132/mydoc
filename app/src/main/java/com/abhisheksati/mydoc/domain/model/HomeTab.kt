package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    FILES("Files"),
    TOOLS("Tools");

    companion object {
        val DOCUMENTS = FILES
    }
}
