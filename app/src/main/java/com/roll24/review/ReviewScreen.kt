package com.roll24.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.roll24.R
import com.roll24.film.FilmProfile
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.ui.components.Roll24TactilePanel
import com.roll24.ui.theme.*

@Composable
fun ReviewScreen(
    bitmap: Bitmap,
    profile: FilmProfile,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val haptics = rememberRoll24Haptics()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Roll24Colors.InkBlack)
            .systemBarsPadding()
    ) {
        // Image display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Roll24Radius.Lg))
            )
        }
        
        // Profile info and actions panel
        Roll24TactilePanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile name
                Text(
                    text = profile.name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = Roll24Colors.WarmGold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Profile description
                Text(
                    text = profile.description,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Roll24Colors.MutedText
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Discard button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Roll24Radius.Md))
                            .background(Roll24Colors.Raised)
                            .clickable {
                                haptics.discard()
                                onDiscard()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = stringResource(R.string.discard),
                            tint = Roll24Colors.Danger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.discard),
                            color = Roll24Colors.Paper
                        )
                    }
                    
                    // Save button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Roll24Radius.Md))
                            .background(Roll24Colors.WarmGold)
                            .clickable {
                                haptics.saveSuccess()
                                onSave()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_save),
                            contentDescription = stringResource(R.string.save),
                            tint = Roll24Colors.InkBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save),
                            color = Roll24Colors.InkBlack
                        )
                    }
                }
            }
        }
    }
}
