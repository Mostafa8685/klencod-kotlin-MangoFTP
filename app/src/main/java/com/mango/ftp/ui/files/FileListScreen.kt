package com.mango.ftp.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mango.ftp.data.model.FileCategory
import com.mango.ftp.data.model.FtpFileItem
import com.mango.ftp.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel,
    onUploadClick: () -> Unit,
    onFileDownloaded: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredFiles by viewModel.filteredFiles.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showCreateDirDialog by remember { mutableStateOf(false) }
    var newDirName by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<FtpFileItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Search Bar & Breadcrumb Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث في الملفات أو اسم الرافع...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = MangoOrange) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Path Breadcrumbs & Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (currentPath != "/" && currentPath.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.navigateUp() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "الرجوع للمجلد السابق",
                                        tint = MangoOrange
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "المسار الحالي",
                                tint = MangoOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Create Directory button
                        IconButton(
                            onClick = { showCreateDirDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "مجلد جديد",
                                tint = MangoOrange
                            )
                        }
                    }
                }
            }

            // Categories horizontal filter bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    FileCategory.ALL,
                    FileCategory.VIDEO,
                    FileCategory.IMAGE,
                    FileCategory.AUDIO,
                    FileCategory.DOCUMENT,
                    FileCategory.ARCHIVE,
                    FileCategory.FOLDER
                )
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.titleArabic) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color.White else getCategoryColor(category)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MangoOrange,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "خطأ", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // File items list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MangoOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري قراءة محتويات فلاشة الراوتر...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MangoTextMedium
                        )
                    }
                }
            } else if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📂", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المجلد فارغ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MangoTextDark
                        )
                        Text(
                            text = "اضغط على زر (+) في الأسفل لرفع ملفاتك إلى الراوتر",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MangoTextMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.fullPath }) { item ->
                        FileItemCard(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.navigateInto(item)
                                }
                            },
                            onDownload = {
                                viewModel.downloadFile(item, it, onFileDownloaded)
                            },
                            onDelete = {
                                fileToDelete = item
                            }
                        )
                    }
                    // Bottom spacing for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Upload FAB
        FloatingActionButton(
            onClick = onUploadClick,
            containerColor = MangoOrange,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "رفع ملف")
                Spacer(modifier = Modifier.width(8.dp))
                Text("رفع ملف", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Create Directory Dialog
    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text("إنشاء مجلد جديد") },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    label = { Text("اسم المجلد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDirName.isNotBlank()) {
                            viewModel.createDirectory(newDirName)
                            newDirName = ""
                            showCreateDirDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
                ) {
                    Text("إنشاء", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من رغبتك في حذف '${item.cleanName}' من فلاشة الراوتر؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(item)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun FileItemCard(
    item: FtpFileItem,
    onClick: () -> Unit,
    onDownload: (android.content.Context) -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDirectory) MangoCardBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = null,
                    tint = getCategoryColor(item.category),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.cleanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Publisher Badge (Smart metadata parsed)
                if (item.isSmartNamed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MangoOrange.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "👤 رُفع بواسطة: ${item.uploaderName ?: "مجهول"} (${item.deviceModel ?: "هاتف"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MangoOrangeDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Size & Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MangoTextMedium
                    )

                    if (item.modifiedTimeMillis > 0) {
                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(item.modifiedTimeMillis))
                        Text(
                            text = "•  $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MangoTextLight
                        )
                    }
                }
            }

            // Actions
            if (item.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "دخول المجلد",
                    tint = MangoOrange
                )
            } else {
                IconButton(onClick = { onDownload(context) }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "تنزيل",
                        tint = MangoOrange
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = MangoTextMedium
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تنزيل الملف") },
                            onClick = {
                                showMenu = false
                                onDownload(context)
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف من الراوتر", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.ALL -> Icons.Default.Apps
        FileCategory.VIDEO -> Icons.Default.Movie
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.AUDIO -> Icons.Default.Audiotrack
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.ARCHIVE -> Icons.Default.Archive
        FileCategory.FOLDER -> Icons.Default.Folder
        FileCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

fun getCategoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.ALL -> MangoOrange
        FileCategory.VIDEO -> CatVideoColor
        FileCategory.IMAGE -> CatImageColor
        FileCategory.AUDIO -> CatAudioColor
        FileCategory.DOCUMENT -> CatDocColor
        FileCategory.ARCHIVE -> CatArchiveColor
        FileCategory.FOLDER -> CatFolderColor
        FileCategory.OTHER -> CatOtherColor
    }
}
