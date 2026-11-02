package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun HomeCategoriesGrid(
    categories: List<CategoryItem>,
    onCategoryClick: (CategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureWhite)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section Header: "Quick Categories" on the left, "See All >" on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        categories.find { it.id == "cat_all" }?.let { onCategoryClick(it) }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "See All >",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // 2 Rows of 4 items each = 8 Total Categories
        val row1 = categories.take(4)
        val row2 = categories.drop(4).take(4)

        CategoryRow(items = row1, onCategoryClick = onCategoryClick)
        Spacer(modifier = Modifier.height(12.dp))
        CategoryRow(items = row2, onCategoryClick = onCategoryClick)
    }
}

@Composable
private fun CategoryRow(
    items: List<CategoryItem>,
    onCategoryClick: (CategoryItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { category ->
            CategoryGridItem(
                category = category,
                onClick = { onCategoryClick(category) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, bgColor, iconTint) = getCategoryVisuals(category.id)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("category_btn_${category.id}")
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Icon container with soft colored background and subtle border
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(54.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(1.dp, iconTint.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.title,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Optional mini badge
            if (category.badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(if (category.badge == "AI" || category.badge == "HOT") Color(0xFFEF4444) else AccentBlue)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = category.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Exactly matched icons:
// 1. Smart Scanner -> DocumentScanner (Scanner/Document frame icon)
// 2. PDF Tools -> PictureAsPdf (PDF file icon)
// 3. Import Image -> Image (Gallery/Image icon)
// 4. Import File -> Folder (Folder/File icon)
// 5. ID Card Scanner -> Badge (ID card / Credit card icon)
// 6. Extract Text / OCR -> TextFields (Text / OCR scanning icon)
// 7. Image Tools -> AutoFixHigh (Edit/Crop/Wand icon)
// 8. All Categories -> GridView (Grid/Menu icon)
private fun getCategoryVisuals(id: String): Triple<ImageVector, Color, Color> {
    return when (id) {
        "cat_smart_scan" -> Triple(Icons.Outlined.DocumentScanner, Color(0xFFEFF6FF), AccentBlue)
        "cat_pdf_tools" -> Triple(Icons.Filled.PictureAsPdf, Color(0xFFFEF2F2), Color(0xFFDC2626))
        "cat_import_img" -> Triple(Icons.Filled.Image, Color(0xFFF0FDF4), Color(0xFF10B981))
        "cat_import_file" -> Triple(Icons.Filled.Folder, Color(0xFFF5F3FF), Color(0xFF7C3AED))
        "cat_id_card" -> Triple(Icons.Filled.Badge, Color(0xFFFFFBEB), Color(0xFFD97706))
        "cat_ocr" -> Triple(Icons.Filled.TextFields, Color(0xFFECFEFF), Color(0xFF0891B2))
        "cat_image_tools" -> Triple(Icons.Filled.AutoFixHigh, Color(0xFFFDF2F8), Color(0xFFDB2777))
        else -> Triple(Icons.Filled.GridView, Color(0xFFF3F4F6), Color(0xFF4B5563))
    }
}
