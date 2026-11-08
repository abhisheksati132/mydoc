package com.abhisheksati.mydoc.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FirstRunGuide(onExplore: ()->Unit) {
    val prefs=LocalContext.current.getSharedPreferences("mydoc_welcome",0)
    var visible by remember { mutableStateOf(!prefs.getBoolean("guide_197_seen",false)) }
    if(visible) OnboardingGuide(onDone={explore->
        prefs.edit().putBoolean("guide_197_seen",true).apply();visible=false
        if(explore)onExplore()
    })
}

/** No permission is requested until the user chooses a file/folder in the actual workflow. */
@Composable
fun OnboardingGuide(onDone: (Boolean)->Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles=listOf("Your files. Your choice.","Find what is inside.","Organise without losing control.")
    val bodies=listOf(
        "Open a document with Android’s file picker, or grant access to a folder you choose. You can browse selected files without granting access to all storage. Storage and media permissions can be managed in Android Settings.",
        "Open Search, then Contents, tags & smart collections. Add files or a folder to index text locally. Results include a snippet and open at the matching page. Refresh after files change. Locked files and scanned pages are not text-indexed; OCR is available separately in the reader.",
        "Add tags to files from different folders and save smart collections using words, tags and file types. In Storage Cleaner, preview duplicate copies and choose which one to keep. Removed copies go to the Recycle Bin. Your original files are never changed by indexing or tagging."
    )
    Dialog(onDismissRequest={onDone(false)},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth().padding(20.dp),shape=MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text("MYDOC 1.9.7  •  ${page+1} / 3",style=MaterialTheme.typography.labelLarge)
                Text(titles[page],style=MaterialTheme.typography.headlineMedium)
                Text(bodies[page],style=MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={if(page>0)page-- else onDone(false)}) { Text(if(page>0)"Back" else "Skip") }
                    Button(onClick={if(page<2)page++ else onDone(true)}) { Text(if(page<2)"Next" else "Explore collections") }
                }
                if(page==2) TextButton(onClick={onDone(false)}) { Text("Start browsing") }
                Text("Replay this guide from Settings → About.",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
