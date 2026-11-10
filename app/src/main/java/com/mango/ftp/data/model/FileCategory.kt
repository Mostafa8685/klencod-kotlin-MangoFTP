package com.mango.ftp.data.model

enum class FileCategory(val titleArabic: String) {
    ALL("الكل"),
    VIDEO("فيديوهات"),
    IMAGE("صور"),
    AUDIO("صوتيات"),
    DOCUMENT("مستندات"),
    ARCHIVE("مضغوطة"),
    FOLDER("مجلدات"),
    OTHER("أخرى")
}
