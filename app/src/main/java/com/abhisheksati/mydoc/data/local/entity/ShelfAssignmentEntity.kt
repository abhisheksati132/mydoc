package com.abhisheksati.mydoc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shelf_assignments",
    primaryKeys = ["documentUri", "shelfId"],
    foreignKeys = [
        ForeignKey(
            entity = ShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("shelfId"),
        Index("documentUri")
    ]
)
data class ShelfAssignmentEntity(
    val documentUri: String,
    val shelfId: Long
)
