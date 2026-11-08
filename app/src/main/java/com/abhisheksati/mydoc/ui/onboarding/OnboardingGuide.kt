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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

@Composable
fun FirstRunGuide(onExplore: ()->Unit) {
    val prefs=LocalContext.current.getSharedPreferences("mydoc_welcome",0)
    var visible by rememberSaveable { mutableStateOf(!prefs.getBoolean("welcome_v2_seen",false)) }
    if(visible) OnboardingGuide(onDone={explore->
        prefs.edit().putBoolean("welcome_v2_seen",true).apply();visible=false
        if(explore)onExplore()
    })
}

/** No permission is requested until the user chooses a file/folder in the actual workflow. */
@Composable
fun OnboardingGuide(onDone: (Boolean)->Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    LaunchedEffect(page) { scrollState.scrollTo(0) }
    val titles=listOf("Your files. Your choice.","Find what is inside.","Organise without losing control.")
    val bodies=listOf(
        "Read, browse and organise in one place. Start with a single file, or choose the folders you want MyDoc to access.",
        "Find a passage, not just a filename. Add chosen files to your private, on-device search library.",
        "Bring files together with tags and smart collections. Review duplicate copies before choosing what to remove."
    )
    val details=listOf(
        listOf("Choose your access" to "Opening a file does not require access to all storage.",
            "Stay in control" to "MyDoc asks for permissions when a workflow needs them. Change access in Android Settings."),
        listOf("Go straight to the match" to "Search results include a snippet and can open the matching page.",
            "Choose what to index" to "Refresh after edits. Locked files and scanned pages are not indexed; OCR is a separate reader tool."),
        listOf("Organise across folders" to "Tags and collection rules leave your original files in place.",
            "Keep the right copy" to "Preview duplicates, choose a keeper and move unwanted copies to the Recycle Bin.")
    )
    val icons=listOf(Icons.Outlined.FolderOpen,Icons.Outlined.Search,Icons.Outlined.CollectionsBookmark)
    Dialog(onDismissRequest={onDone(false)},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=28.dp,vertical=16.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("MYDOC",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                    TextButton(onClick={onDone(false)}) { Text("Skip") }
                }
                Column(Modifier.weight(1f).verticalScroll(scrollState),verticalArrangement=Arrangement.spacedBy(22.dp)) {
                Spacer(Modifier.height(20.dp))
                Box(Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                    Icon(icons[page],contentDescription=null,modifier=Modifier.size(40.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(titles[page],style=MaterialTheme.typography.headlineMedium)
                Text(bodies[page],style=MaterialTheme.typography.bodyLarge)
                details[page].forEach { (title,description) ->
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(title,style=MaterialTheme.typography.titleSmall)
                        Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index -> Box(Modifier.weight(1f).height(4.dp).background(
                        if(index==page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,CircleShape)) }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={if(page>0)page-- else onDone(false)}) { Text(if(page>0)"Back" else "Start browsing") }
                    Button(onClick={if(page<2)page++ else onDone(true)}) { Text(if(page<2)"Next" else "Explore collections") }
                }
                if(page==2) TextButton(onClick={onDone(false)}) { Text("Start browsing") }
                Text("${page+1} of 3 · Replay from Settings → About",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
