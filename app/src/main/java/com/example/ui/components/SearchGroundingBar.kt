package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GroundingSourceItem
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface

private val ANCHOR_LINK_REGEX = Regex(
    """<a\b[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val HTML_TAG_REGEX = Regex("<[^>]+>")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchGroundingBar(
    webSources: List<GroundingSourceItem>,
    searchEntryPointHtml: String?,
    modifier: Modifier = Modifier
) {
    val parsedEntryLinks = remember(searchEntryPointHtml) {
        if (searchEntryPointHtml.isNullOrBlank()) {
            emptyList()
        } else {
            ANCHOR_LINK_REGEX.findAll(searchEntryPointHtml).mapNotNull { match ->
                val url = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val label = match.groupValues.getOrNull(2)
                    ?.replace(HTML_TAG_REGEX, "")
                    ?.trim()
                    .orEmpty()
                if (url.startsWith("http") && label.isNotBlank()) {
                    GroundingSourceItem(title = label, uri = url)
                } else {
                    null
                }
            }.toList()
        }
    }

    val allSources = remember(webSources, parsedEntryLinks) {
        (webSources + parsedEntryLinks).distinctBy { it.uri }
    }

    if (allSources.isEmpty() && searchEntryPointHtml.isNullOrBlank()) return

    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = WhereDeepGreen,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (allSources.isNotEmpty()) "Google Search Sources" else "Grounded with Google Search",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (allSources.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSources.forEach { source ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = WhereMintSurface,
                        border = BorderStroke(1.dp, WhereMintBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.uri)))
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open source",
                                tint = WhereDeepGreen,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
