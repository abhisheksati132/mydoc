package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    FILES("Files"),
    TOOLS("Tools");

    companion object {
        val DOCUMENTS = HOME
    }
}
