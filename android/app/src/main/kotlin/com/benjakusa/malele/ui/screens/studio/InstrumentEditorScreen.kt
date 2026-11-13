package com.benjakusa.malele.ui.screens.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjakusa.malele.domain.model.InstrumentParams
import com.benjakusa.malele.ui.components.MaleleCard
import com.benjakusa.malele.ui.theme.PureRed

@Composable
fun InstrumentEditorScreen(
    trackName: String,
    params: InstrumentParams,
    onParamsChange: (InstrumentParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaleleCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DSP Parameter Editor: $trackName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))

            // 1. Sound Section
            SectionHeader("Sound & Envelope")
            ParamSlider("Pitch Offset", params.pitch, -12f..12f) { onParamsChange(params.copy(pitch = it)) }
            ParamSlider("Attack", params.attack, 0.01f..1.0f) { onParamsChange(params.copy(attack = it)) }
            ParamSlider("Decay", params.decay, 0.05f..2.0f) { onParamsChange(params.copy(decay = it)) }

            Spacer(Modifier.height(12.dp))

            // 2. Tone & Filter Section
            SectionHeader("Tone & Filter")
            ParamSlider("Low-Pass Cutoff", params.lowPassCutoff, 200f..20000f) { onParamsChange(params.copy(lowPassCutoff = it)) }
            ParamSlider("Filter Resonance", params.resonance, 0.5f..5.0f) { onParamsChange(params.copy(resonance = it)) }

            Spacer(Modifier.height(12.dp))

            // 3. FX Section
            SectionHeader("Effects (Delay & Drive)")
            ParamSlider("Delay Mix", params.delayMix, 0f..1f) { onParamsChange(params.copy(delayMix = it)) }
            ParamSlider("Saturation Drive", params.saturationAmount, 0f..1f) { onParamsChange(params.copy(saturationAmount = it)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = PureRed,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(120.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PureRed,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.width(36.dp),
        )
    }
}
