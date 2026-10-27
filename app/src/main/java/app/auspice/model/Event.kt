package app.auspice.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.time.LocalDate

@Entity(
    indices = [Index(
        value = arrayOf("name", "surname", "originalDate"),
        unique = true
    )]
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @Expose
    val type: String? = EventCode.BIRTHDAY.name,
    @Expose
    val name: String,
    @Expose
    val surname: String? = "",
    val favorite: Boolean? = false,
    @Expose
    val yearMatter: Boolean? = true,
    @Expose
    val originalDate: LocalDate,
    @Expose
    val notes: String? = "",
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val image: ByteArray? = null,
    // Optional, only used by traditions that differ by sex (yakudoshi). Values: MALE, FEMALE or null
    @Expose
    val sex: String? = null,
    // Optional birth time ("HH:mm"), place (label, coordinates, IANA zone) and personality type ("INTJ-A")
    @Expose
    val birthTime: String? = null,
    @Expose
    val birthPlace: String? = null,
    @Expose
    val birthLatitude: Double? = null,
    @Expose
    val birthLongitude: Double? = null,
    @Expose
    val birthZone: String? = null,
    @Expose
    val personality: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (id != other.id) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (surname != other.surname) return false
        if (originalDate != other.originalDate) return false
        if (!image.contentEquals(other.image)) return false
        if (sex != other.sex) return false
        if (birthTime != other.birthTime) return false
        if (birthPlace != other.birthPlace) return false
        if (birthLatitude != other.birthLatitude) return false
        if (birthLongitude != other.birthLongitude) return false
        if (birthZone != other.birthZone) return false
        if (personality != other.personality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (surname?.hashCode() ?: 0)
        result = 31 * result + (favorite?.hashCode() ?: 0)
        result = 31 * result + (yearMatter?.hashCode() ?: 0)
        result = 31 * result + originalDate.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (sex?.hashCode() ?: 0)
        result = 31 * result + (birthTime?.hashCode() ?: 0)
        result = 31 * result + (birthPlace?.hashCode() ?: 0)
        result = 31 * result + (birthLatitude?.hashCode() ?: 0)
        result = 31 * result + (birthLongitude?.hashCode() ?: 0)
        result = 31 * result + (birthZone?.hashCode() ?: 0)
        result = 31 * result + (personality?.hashCode() ?: 0)
        return result
    }
}