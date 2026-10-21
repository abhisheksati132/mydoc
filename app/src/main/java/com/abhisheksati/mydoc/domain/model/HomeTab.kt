package com.abhisheksati.mydoc.domain.model

enum class HomeTab(val label: String) {
    NOW("Now"),
    LIBRARY("Library"),
    TOOLS("Tools");

    companion object {
        val FILES = NOW
        val DOCUMENTS = LIBRARY
        val MEDIA = LIBRARY
    }
}
