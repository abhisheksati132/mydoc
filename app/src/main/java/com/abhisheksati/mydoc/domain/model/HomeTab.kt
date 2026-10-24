package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    HOME("Home"),
    FILES("Files"),
    TOOLS("Tools");

    companion object {
        val DOCUMENTS = HOME
    }
}
