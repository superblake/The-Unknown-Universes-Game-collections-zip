package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TierBoostCodesDialog(
  onDismiss: () -> Unit,
  onApplyCode: (String) -> Unit,
  onBoostAllTiers: (Int) -> Unit,
  onMaxAllTiers: () -> Unit,
  onMaxAllStars: () -> Unit,
  onGrantEssence: () -> Unit,
  onImportBo3Pack: () -> Unit,
) {
  var codeText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("tier_boost_codes_dialog"),
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "TIER PROTOCOLS & INSTANT CODES",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF00E5FF),
        )
        Text(
          text = "Enter codes or use 1-tap buttons to immediately elevate tiers of all mods, weapons, gear, character, and equipment.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        // Direct Code Input Field
        item {
          OutlinedTextField(
            value = codeText,
            onValueChange = { codeText = it.take(30) },
            label = { Text("Protocol Code (e.g. TIER25, BOOST5, STARS10)") },
            placeholder = { Text("TIER25") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("tier_code_input"),
          )
          Spacer(Modifier.height(6.dp))
          Button(
            onClick = {
              if (codeText.isNotBlank()) {
                onApplyCode(codeText.trim())
                codeText = ""
              }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("tier_code_submit_btn"),
          ) {
            Text("Activate Code", fontWeight = FontWeight.Bold)
          }
        }

        // 1-Tap Quick Action Buttons (Zero tapping or holding friction)
        item {
          Text(
            text = "1-TAP INSTANT TIER BOOSTERS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        item {
          Button(
            onClick = { onMaxAllTiers() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick_tier_max_all_btn"),
          ) {
            Text("MAX ALL TIERS TO T25 (MULTIVERSAL)", fontWeight = FontWeight.Bold, color = Color.Black)
          }
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onBoostAllTiers(5) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("quick_tier_plus_5_btn"),
            ) {
              Text("+5 All Tiers", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onBoostAllTiers(1) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("quick_tier_plus_1_btn"),
            ) {
              Text("+1 All Tiers", fontWeight = FontWeight.Bold)
            }
          }
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onMaxAllStars() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("quick_star_max_all_btn"),
            ) {
              Text("Max 10★ Stars", fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
            }
            OutlinedButton(
              onClick = { onGrantEssence() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("quick_grant_essence_btn"),
            ) {
              Text("+50k Essence", fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
            }
          }
        }

        item {
          Button(
            onClick = { onImportBo3Pack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick_import_bo3_preset_btn"),
          ) {
            Text("IMPORT BO3 MOD TOOLS PERKS & WEAPONS", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        // Code Reference Cheat Sheet
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("KNOWN PROTOCOL CODES:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
              Text("• TIER25 / MAXTIER — Sets character, weapons, wonder weapons, gear, equipment to T25 Multiversal", style = MaterialTheme.typography.bodySmall)
              Text("• BOOST5 — Increases all equipment tiers by +5 immediately", style = MaterialTheme.typography.bodySmall)
              Text("• STARS10 — Unlocks 10★ Omniverse Nova star ranks", style = MaterialTheme.typography.bodySmall)
              Text("• ESSENCE999 — Grants +100k Multi-Color Essence & 500 Blast Shards", style = MaterialTheme.typography.bodySmall)
              Text("• UNLOCKALL — Unlocks T25, 10★, all skill nodes & NG+5", style = MaterialTheme.typography.bodySmall)
              Text("• BO3PERKS — Activates Treyarch BO3 Mod Tools overdrive", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, modifier = Modifier.testTag("tier_codes_dialog_close")) {
        Text("Done")
      }
    },
  )
}
