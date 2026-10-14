package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // KMK -->
        Image(
            painter = painterResource(R.drawable.ic_komiyomi_logo),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 32.dp, bottom = 12.dp)
                .size(88.dp),
        )
        Text(stringResource(KMR.strings.komiyomi_app_name), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(KMR.strings.komiyomi_hiragana),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )
        // KMK <--

        HorizontalDivider()
    }
}
