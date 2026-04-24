package pastimegames.mykidsjukebox.features.shared.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.R

@Composable
fun LargeCloseButton(
    onClick: () -> Unit,
    height: Dp = 100.dp,
    iconSize: Dp = 70.dp,
    cornerRadius: Dp = 50.dp,
    modifier: Modifier = Modifier
) {
    val uiFeedback = rememberUiFeedback()
    Button(
        onClick = {
            uiFeedback.performPrimaryActionFeedback()
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.close),
            modifier = Modifier.size(iconSize)
        )
    }
}
