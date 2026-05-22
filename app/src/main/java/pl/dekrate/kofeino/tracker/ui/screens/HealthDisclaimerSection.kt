package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.dekrate.kofeino.tracker.R

@Composable
fun HealthDisclaimerSection() {
    var isDisclaimerExpanded by remember { mutableStateOf(false) }
    var isSourcesExpanded by remember { mutableStateOf(false) }

    val disclaimerDescription = if (isDisclaimerExpanded) {
        stringResource(R.string.health_disclaimer_collapse)
    } else {
        stringResource(R.string.health_disclaimer_expand)
    }

    val sourcesDescription = if (isSourcesExpanded) {
        stringResource(R.string.health_references_collapse)
    } else {
        stringResource(R.string.health_references_expand)
    }

    Column {
        // ── Disclaimer section ──
        SectionHeader(
            text = stringResource(R.string.health_disclaimer_title),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        ExpandableCard(
            title = stringResource(R.string.health_disclaimer_title),
            isExpanded = isDisclaimerExpanded,
            description = disclaimerDescription,
            onToggle = { isDisclaimerExpanded = !isDisclaimerExpanded }
        ) {
            Text(
                text = stringResource(R.string.health_disclaimer_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Sources section ──
        SectionHeader(
            text = stringResource(R.string.health_references_title),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        ExpandableCard(
            title = stringResource(R.string.health_references_title),
            isExpanded = isSourcesExpanded,
            description = sourcesDescription,
            onToggle = { isSourcesExpanded = !isSourcesExpanded }
        ) {
            Text(
                text = stringResource(R.string.health_references_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    isExpanded: Boolean,
    description: String,
    onToggle: () -> Unit,
    body: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp)
                    .semantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                body()
            }
        }
    }
}
