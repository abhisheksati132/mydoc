package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    TOOLS("Tools");

    companion object {
        val FILES = LIBRARY
        val DOCUMENTS = LIBRARY
    }
}
